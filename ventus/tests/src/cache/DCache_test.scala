package play.cache

import chisel3._
import chisel3.util._
import chiseltest._
import org.scalatest.freespec.AnyFreeSpec
import L2cache.{TLBundleA_lite_custom, TLBundleD_lite_plus_custom, InclusiveCacheParameters_lite, TLBundleA_lite, TLBundleD_lite_plus, CacheParameters, InclusiveCacheMicroParameters}
import L1Cache.AtomicUnit.AtomicUnit
import L1Cache.DCache._
import L1Cache._
import L1Cache.{ MyConfig}
import top.parameters._
import config.config.Parameters
import mmu.SVParam

class DCacheWrapperTest extends AnyFreeSpec with ChiselScalatestTester {
  implicit val p: Parameters = (new MyConfig).toInstance
  
  "DCacheWrapper_test" in {
    test(new DCacheWrapperWithAtomic(
      params = InclusiveCacheParameters_lite(
        cache = CacheParameters(
          level = 2,
          ways = 8,
          sets = 32,
          l2cs = 1,
          blockBytes = 64,
          beatBytes = dcache_BlockWords << 2
        ),
        micro = InclusiveCacheMicroParameters(
          writeBytes = 1,
          memCycles = 40,
          portFactor = 4,
          num_warp = 32,
          num_sm = 4,
          num_sm_in_cluster = 2,
          num_cluster = 2,
          NMshrEntry = 16,
          NSets = 32,
          NInfWriteEntry = 16
        ),
        control = false,
        mmu = false
      )
    )).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
      // 初始化信号
      dut.io.coreReq.initSource()
      dut.io.coreRsp.initSink()
      dut.io.coreRsp.ready.poke(true.B)
      dut.clock.setTimeout(60)
      //dut.io.pipe_req.bits.addr := Cat(blockAddr, blockAddrOffset)

      dut.io.coreReq.valid.poke(false.B)
      dut.io.coreRsp.ready.poke(true.B)

      dut.reset.poke(true.B)
      dut.clock.step(1)
      dut.reset.poke(false.B)
      dut.clock.step(5)

      
      val filename = s"ventus/txt/DCache/RAMOR_sameadd.txt"
      val file = scala.io.Source.fromFile(filename)
      val fileLines = file.getLines()

      for (line <- fileLines) {
        if (line.trim.nonEmpty) { // 检测line变量是否为空行
          val fields = line.split(",")// 字段中不允许存在空格
          dut.io.coreReq.valid.poke(true.B)
          dut.io.coreReq.bits.opcode.poke(fields(4).U)
          dut.io.coreReq.bits.param.poke(fields(5).U)
          dut.io.coreReq.bits.instrId.poke(fields(0).U)
          val blockIdxFromTxt = fields(1).U
          val tagFromTxt = blockIdxFromTxt(dut.dcache.SetIdxBits+dut.dcache.TagBits-1,dut.dcache.SetIdxBits)
          val setIdxFromTxt = blockIdxFromTxt(dut.dcache.SetIdxBits-1,0)
          dut.io.coreReq.bits.tag.poke(tagFromTxt)
          dut.io.coreReq.bits.setIdx.poke(setIdxFromTxt)
          // 目前只支持测试标量
          dut.io.coreReq.bits.perLaneAddr(0).blockOffset.poke(fields(2).U)
          if(fields(6) == "d0"){
            dut.io.coreReq.bits.perLaneAddr(0).activeMask.poke(true.B)
            dut.io.coreReq.bits.perLaneAddr(0).wordOffset1H.poke("b1111".U)
            (1 until dut.dcache.NLanes).foreach { iofL =>
              dut.io.coreReq.bits.perLaneAddr(iofL).activeMask.poke(false.B)
              dut.io.coreReq.bits.perLaneAddr(iofL).wordOffset1H.poke("b1111".U)
            }
          } else if (fields(6) == "d1"){
            (1 until dut.dcache.NLanes).foreach { iofL =>
              dut.io.coreReq.bits.perLaneAddr(iofL).activeMask.poke(true.B)
              dut.io.coreReq.bits.perLaneAddr(iofL).wordOffset1H.poke("b1111".U)
            }
          } else {
            println("错误：vector or scalar栏格式错误！")
            sys.exit(1)
          }
          dut.io.coreReq.bits.data(0).poke(fields(3).U)
         // println("op="+fields(0),", blockaddr="+fields(4))
          fork
            .withRegion(Monitor) {
              while (dut.io.coreReq.ready.peek().litToBoolean == false) {
                dut.clock.step(1)
              }
            }
            .joinAndStep(dut.clock)

        } else {
          dut.io.coreReq.valid.poke(false.B)
          dut.clock.step(1)

        }
      }
      //dut.io.coreReq.enqueueSeq()
      dut.io.coreReq.valid.poke(false.B)

      dut.clock.step(30)
      file.close()
      // 测试写操作
      //dut.io.coreReq.valid.poke(true.B)
      //dut.io.coreReq.bits.instrId.poke(0.U)
      //dut.io.coreReq.bits.opcode.poke(1.U)  // 写操作
      //dut.io.coreReq.bits.param.poke(0.U)
      //dut.io.coreReq.bits.tag.poke("h123".U)
      //dut.io.coreReq.bits.setIdx.poke("h2".U)
      //dut.io.coreReq.bits.perLaneAddr(0).activeMask.poke(true.B)
      //dut.io.coreReq.bits.perLaneAddr(0).blockOffset.poke(0.U)
      //dut.io.coreReq.bits.perLaneAddr(0).wordOffset1H.poke("b1111".U)
      //dut.io.coreReq.bits.data(0).poke("h12345678".U)
      //dut.clock.step(1)
      //dut.io.coreReq.valid.poke(false.B)
      //dut.io.coreReq.enqueue((new DCacheCoreReq(None)(p)).Lit(
      //  _.instrId     -> 0.U,
      //  _.opcode      -> 1.U,  // 写操作
      //  _.param       -> 0.U,
      //  _.tag         -> "h123".U,
      //  _.setIdx                     ->"h45".U,
      //  _.perLaneAddr(0).activeMask   ->true.B,
      //  _.perLaneAddr(0).blockOffset  ->0.U,
      //  _.perLaneAddr(0).wordOffset1H ->"b1111".U,
      //  _.data(0)      -> "h12345678".U
      //))
      
      //dut.clock.step(5)

      //// 测试读操作
      //val readReq = new DCacheCoreReq(None)(p)
      //readReq.instrId.poke(1.U)
      //readReq.opcode.poke(0.U)  // 读操作
      //readReq.param.poke(0.U)
      //readReq.tag.poke("h123".U)
      //readReq.setIdx.poke("h45".U)
      //readReq.perLaneAddr(0).activeMask.poke(true.B)
      //readReq.perLaneAddr(0).blockOffset.poke(0.U)
      //readReq.perLaneAddr(0).wordOffset1H.poke("b1111".U)
      //dut.io.coreReq.enqueue(readReq)
//
      //// 等待响应
      //dut.clock.step(10)
    }
  }
} 