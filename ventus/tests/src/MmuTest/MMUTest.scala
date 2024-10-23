package MmuTest

import circt.stage.ChiselStage
import chisel3._
import chisel3.util._
import chiseltest._
import chisel3.experimental.BundleLiterals._
import chisel3.experimental.VecLiterals._
import mmu._
import top.{DecoupledPipe, MemBox, MetaData}
import MemboxS._
import MmuTestUtils._
import org.scalatest.freespec.AnyFreeSpec
import play.IniFile
import play.TestUtils.{IOTestDriver, IOTransform, MemPortDriverDelay, RequestSender, checkForReady, checkForValid}
import scala.io.Source

class MMUSystem(NL1Cache: Int, NL1TlbWays: Int, SV: SVParam) extends Module{
  val l1tlb = Seq.fill(NL1Cache)(Module(new L1TLB(SV, NL1TlbWays)))
  val l2tlb = Module(new L2TLB(SV = SV, Debug = true, L2C = None, accelSize = 8)(L1C = None))
  val lookup = Module(new AsidLookup(SV, l2tlb.nBanks, 4))

  val mmu_req = IO(Vec(NL1Cache,
    Flipped(DecoupledIO(new L1TlbReq(mmu.SV32)))
  ))
  val mmu_rsp = IO(Vec(NL1Cache,
    DecoupledIO(new L1TlbRsp(mmu.SV32))
  ))
  val asid_fill = IO(Flipped(ValidIO(new AsidLookupEntry(SV))))
  val mem_req = IO(Vec(l2tlb.nBanks, DecoupledIO(new Cache_Req(SV))))
  val mem_rsp = IO(Vec(l2tlb.nBanks, Flipped(DecoupledIO(new Cache_Rsp(SV)))))

  val prev_hit = Wire(Vec(NL1Cache, UInt(20.W))); dontTouch(prev_hit)
  val prev_miss = Wire(Vec(NL1Cache, UInt(20.W))); dontTouch(prev_miss)
  val cycle_cnt = IO(Output(UInt(20.W)))
  l1tlb.zipWithIndex.foreach{case(x, i) => x.io.debug.foreach{ x =>
    prev_miss(i) := RegNext(x.miss_cnt)
    prev_hit(i) := RegNext(x.hit_cnt)
    when(prev_miss(i) =/= x.miss_cnt || prev_hit(i) =/= x.hit_cnt){
      printf(p"#${cycle_cnt} L1#$i MISS ${x.miss_cnt} HIT ${x.hit_cnt}\n")
    }
  }}
  val counter = new Counter(200000)
  cycle_cnt := counter.value
  when(true.B){ counter.inc()}

  val xbar = Module(new L1ToL2TlbXBar(SV, NL1Cache)(L1C = None))

  l1tlb.foreach{x => x.io.invalidate.valid := false.B; x.io.invalidate.bits := DontCare }
  l2tlb.io.invalidate.valid := false.B
  l2tlb.io.invalidate.bits := DontCare
  // lookup
  (l2tlb.io.ptbr_rsp zip lookup.io.lookup_rsp).foreach{ case(l, r) => l <> r }
  (l2tlb.io.asid_req zip lookup.io.lookup_req).foreach{ case(l, r) => r := l }
  lookup.io.fill_in <> asid_fill
  // in port <> l1tlb
  (mmu_req zip l1tlb.map(_.io.in)).foreach{ case (l, r) =>
    val pipe = Module(new DecoupledPipe(l.bits.cloneType, 1, false))
    r <> pipe.io.deq
    pipe.io.enq <> l
  }
  (mmu_rsp zip l1tlb.map(_.io.out)).foreach{ case (l, r) =>
    val pipe = Module(new DecoupledPipe(l.bits.cloneType, 1, false))
    l <> pipe.io.deq
    pipe.io.enq <> r
  }
  // l1tlb <> xbar
  (l1tlb.map(_.io.l2_req) zip xbar.io.req_l1).foreach{ case (l, r) =>
    r.valid := l.valid
    l.ready := r.ready
    r.bits.vpn := l.bits.vpn
    r.bits.asid := l.bits.asid
  }
  (0 until l1tlb.size).foreach{ i => xbar.io.req_l1(i).bits.id := (i % 2).U }
  (l1tlb.map(_.io.l2_rsp) zip xbar.io.rsp_l1).foreach{ case(l, r) =>
    l.bits.ppn := r.bits.ppn
    l.bits.flags := r.bits.flag
    l.valid := r.valid
    r.ready := l.ready
  }
  // xbar <> l2tlb
  (xbar.io.req_l2 zip l2tlb.io.in).foreach{ case (l, r) => r <> l }
  (xbar.io.rsp_l2 zip l2tlb.io.out).foreach{ case (l, r) => l <> r }
  // l2tlb <> out port
  (l2tlb.io.mem_req zip mem_req).foreach{ case (l, r) =>
    val pipe = Module(new DecoupledPipe(l.bits.cloneType, 1, true))
    r <> pipe.io.deq
    pipe.io.enq <> l
  }
  (l2tlb.io.mem_rsp zip mem_rsp).foreach{ case (l, r) =>
    val pipe = Module(new DecoupledPipe(l.bits.cloneType, 1, true))
    l <> pipe.io.deq
    pipe.io.enq <> r
  }
}

object MMUGen extends App{
  ChiselStage.emitSystemVerilogFile(new MMUSystem(2, 4, mmu.SV32),
    firtoolOpts = Array("--emission-options=disableMemRandomization, disableRegisterRandomization"))
}

case class TLBRequest(
  sm: Int,
  cache: Int,
  vaddr: BigInt,
  time: Int
)

object TLBRequestList{
  var req_list: Seq[Seq[TLBRequest]] = List.empty
  def parse(src: String) : Unit = {
    val pattern = "#\\s+([0-9]+)\\s+SM ([0-9]+) CACHE ([0-9]+) ADDR ([0-9a-f]+)".r
    val file = Source.fromFile(src)

    val lines = file.getLines().toList
    def groupPrefix(xs: Seq[String]): Seq[Seq[String]] = {
      val splits = (0 until xs.size).filter{ i =>
        xs(i).startsWith("kernel")
      } :+ xs.size
      (splits.dropRight(1) zip splits.tail).map{ case (l, r) => xs.slice(l+1, r) }.filter{ x => x.nonEmpty }
    }

    val grouped = groupPrefix(lines)
    req_list = grouped.map{ k =>
      k.map { str =>
        pattern.findAllMatchIn(str).toList
      }.collect{
        case x if !x.isEmpty => x.head
      }.map{ x =>
        TLBRequest(
          x.group(2).toInt,
          x.group(3).toInt,
          BigInt("0" ++ x.group(4), 16),
          x.group(1).toInt
        )
      }
    }
  }
//  parse("SM 0 CACHE 1 ADDR 8000abcd")

}

class MMU_test extends AnyFreeSpec
  with ChiselScalatestTester
  with MMUHelpers {
  case class AdvTest(name: String, meta: Seq[String], data: Seq[String], var warp: Int, var cycles: Int)

  def testcase = "adv_vecadd"
  val iniFile = new IniFile("./ventus/txt/_cases.ini")
  val section = iniFile.sections(testcase)
  val testbench = AdvTest(
    testcase,
    section("Files").map(_ + ".metadata"),
    section("Files").map(_ + ".data"),
    section("nWarps").head.toInt,
    section("SimCycles").head.toInt
  )
  val nSms = 1
  val nCacheInSm = 2
  "MMU Footprint" in {
    test(new MMUSystem(nSms * nCacheInSm, 4, mmu.SV32)).withAnnotations(Seq(WriteVcdAnnotation)){ c =>
      val metaFileDir = testbench.meta.map("./ventus/txt/" + testbench.name + "/" + _)
      val dataFileDir = testbench.data.map("./ventus/txt/" + testbench.name + "/" + _)
      val fpFileDir = "./ventus/txt/" + testbench.name + "/" + "footprint.log"
      val maxCycle = testbench.cycles

      val metas = metaFileDir.map(MetaData(_))
      val mem = new MemBox(MemboxS.SV32)
      val ptbr = mem.createRootPageTable()

      TLBRequestList.parse(fpFileDir)

      c.mmu_req.foreach{ x => x.initSource; x.setSourceClock(c.clock) }
      c.mmu_rsp.foreach{ x => x.initSink; x.setSinkClock(c.clock) }
      c.mem_req.foreach{ x => x.initSink; x.setSinkClock(c.clock) }
      c.mem_rsp.foreach{ x => x.initSource; x.setSourceClock(c.clock) }
      c.asid_fill.initSource; c.asid_fill.setSourceClock(c.clock)

      class MMUMemPortDriverDelay(
        val reqPort: DecoupledIO[Cache_Req],
        val rspPort: DecoupledIO[Cache_Rsp],
        val mem: MemBox[_],
        val latency: Int,
        val depth: Int
      ) extends IOTestDriver[Cache_Req, Cache_Rsp] with IOTransform[Cache_Req, Cache_Rsp]{

        var rsp_queue: Seq[(Int, Cache_Rsp)] = Seq.empty

        def eval(): Unit = {
          if(checkForValid(reqPort) && checkForReady(reqPort)){
            rsp_queue :+= (latency, transform(reqPort.bits))
          }

          if(rsp_queue.nonEmpty && rsp_queue.head._1 == 0){
            if(checkForValid(rspPort) && checkForReady(rspPort)){
              rspPort.valid.poke(false.B)
              rsp_queue = rsp_queue.drop(1)
            }
            else{
              rspPort.valid.poke(true.B)
              rspPort.bits.poke(rsp_queue.head._2)
            }
          }
          else{
            rspPort.valid.poke(false.B)
          }

          rsp_queue = rsp_queue.zipWithIndex.map{ case (e, i) =>
            if (e._1 > i) (e._1 - 1, e._2) else (i, e._2)
          }
          if(rsp_queue.nonEmpty && rsp_queue.head._1 == 0){
            rspPort.valid.poke(true.B)
            rspPort.bits.poke(rsp_queue.head._2)
          }
          reqPort.ready.poke((rsp_queue.size < depth).B)
        }

        def transform(req: Cache_Req): Cache_Rsp = {
          val addr = req.addr.peek().litValue
          val source = req.source.peek().litValue

          val data = mem.readWordsPhysical(addr, 2, Array.fill(2)(true))._2.map(_.toBigInt)

          val rsp = (new Cache_Rsp(mmu.SV32).Lit(
            _.data -> Vec(2, UInt(mmu.SV32.xLen.W)).Lit(
              (0 until 2).map{i => i -> data(i).U}: _*
            ),
            _.source -> source.U
          ))
          rsp
        }
      }

      val mem_driver = (c.mem_req zip c.mem_rsp).map{ case (req, rsp) =>
        new MMUMemPortDriverDelay(req, rsp, mem, 0, 5)
      }
      val req_sender = (c.mmu_req zip c.mmu_rsp).map{ case(req, rsp) =>
        new RequestSender[L1TlbReq, L1TlbRsp](req, rsp)
      }

      c.mmu_rsp.foreach{ x => x.ready.poke(true.B) }

      var cycle_cnt = 0; var ptr1 = 0; var ptr2 = 0;

      c.clock.step(1); cycle_cnt += 1
      timescope{
        c.asid_fill.valid.poke(true.B)
        c.asid_fill.bits.valid.poke(true.B)
        c.asid_fill.bits.asid.poke(1.U)
        c.asid_fill.bits.ptbr.poke(ptbr.U)
        c.clock.step(1); cycle_cnt += 1
      }
      c.clock.step(2); cycle_cnt += 2
      var current_cache = 0
      var kernel_cnt = 0
      var timestamp = 0

      while(kernel_cnt < TLBRequestList.req_list.size && cycle_cnt <= 70000){
        if(ptr2 == ptr1){
          if(ptr2 == TLBRequestList.req_list(kernel_cnt).size){
            kernel_cnt += 1
            ptr1 = 0; ptr2 = 0
          }
          else if(cycle_cnt >= TLBRequestList.req_list(kernel_cnt)(ptr1).time) {
            if(ptr1 == 0){
              print(s"kernel $kernel_cnt\n")
              mem.loadfile(ptbr, metas(kernel_cnt), dataFileDir(kernel_cnt))
            }
            current_cache = TLBRequestList.req_list(kernel_cnt)(ptr1).cache
            req_sender(current_cache).add(new L1TlbReq(mmu.SV32).Lit(
              _.asid -> 1.U,
              _.vaddr -> TLBRequestList.req_list(kernel_cnt)(ptr1).vaddr.U
            ))
            ptr1 += 1
          }
        }
        else{
          if(checkForValid(c.mmu_rsp(current_cache)) && checkForReady(c.mmu_rsp(current_cache))){
            ptr2 += 1
          }
        }
        req_sender(0).eval()
        req_sender(1).eval()
        mem_driver.foreach{ _.eval() }

        cycle_cnt += 1; c.clock.step(1)
      }
    }
  }
}
