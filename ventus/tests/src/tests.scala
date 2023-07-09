/*
 * Copyright (c) 2021-2022 International Innovation Center of Tsinghua University, Shanghai
 * Ventus is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 *          http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND,
 * EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT,
 * MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details. */
package play

import L1Cache.DCache.DataCache
import L1Cache._
import chisel3._
import chisel3.util._
import chisel3.experimental.BundleLiterals.AddBundleLiteralConstructor
import chisel3.experimental.VecLiterals.AddVecLiteralConstructor
import chiseltest._
import org.scalatest.freespec
import org.scalatest.freespec.AnyFreeSpec
import chiseltest.simulator.WriteVcdAnnotation

import scala.util.Random
//import chiseltest.simulator.
import pipeline.pipe
import top._
import scala.io.Source

// add new testcases here!
object TestCaseList{
  // IMPORTANT:
  // `parameters.num_warp` should >= `warp` parameter in TestCase() below
  // the simulation may be slow if `parameters.num_warp` is large
  val L: Map[String, TestCase#Props] = Array[TestCase](
    new TestCase("gaussian", "gaussian_.vmem", "gaussian8.data", 8, 8, 0, 5000),
    new TestCase("saxpy", "saxpy_.vmem", "saxpy.data", 8, 8, 0, 500),
    new TestCase("gemm", "gemm_.vmem", "gemm4x8x4.data", 1, 8, 0, 2400),
    //new TestCase("gemm", "gemm_.vmem", "gemm8x16x12.data", 2, 8, 0, 300),
    new TestCase("saxpy2", "saxpy2_.vmem", "saxpy.data", 1, 8, 0, 800)
  ).map{x => (x.name, x.props)}.toMap

  def apply(s: String) = TestCaseList.L(s)
}
object test_Gen extends App {
  val param = (new MyConfig).toInstance
  (new chisel3.stage.ChiselStage).emitVerilog(new DataCache( )(param))
}

object L1MSHRGen extends App {
  (new chisel3.stage.ChiselStage).emitVerilog(new MSHR(bABits=20,tIWidth=50,WIdBits=5,NMshrEntry = 4, NMshrSubEntry = 4))
}
object L1TagAccessGen extends App {
  (new chisel3.stage.ChiselStage).emitVerilog(new L1TagAccess(set=32,way=2,tagBits=20,true))
}
class test_tb extends AnyFreeSpec with ChiselScalatestTester{
  "tag_test" in {
    val timeLength = 5
    val way = 4
    test(new ReplacementUnit(timeLength,way,true)).withAnnotations(Seq(WriteVcdAnnotation)) { DUT =>
      println("****** ReplacementUnit ******")
      DUT.clock.step(5)
      DUT.io.validOfSet.poke("b1110".U)
      for (i <- 0 until way){
        val temp = (1+i).asUInt
        DUT.io.timeOfSet_st1(i).poke(temp)
      }
      DUT.clock.step(1)
      DUT.io.validOfSet.poke("b1111".U)
      for (i <- 0 until way) {
        val temp = (way-i).asUInt
        DUT.io.timeOfSet_st1(i).poke(temp)
      }
      DUT.clock.step(1)
    }
  }
}

class DCache_RRRRRmiss_diff extends AnyFreeSpec with ChiselScalatestTester{
  //implicit val p = (new MyConfig).toInstance
  "DCache_RRRRRmiss_diff" in {
    test(new DCacheWraper).withAnnotations(Seq(WriteVcdAnnotation)){ DUT =>
      println("****** L1Cache.DCache RRRRRmiss_diff ******")
      //println("wordOffsetBits = ",DUT.DCache.WordOffsetBits)
      println("blockOffsetBits = "+DUT.DCache.BlockOffsetBits)
      println("SetIdxBits = "+DUT.DCache.SetIdxBits)
      println("tagIdxBits = "+DUT.DCache.TagBits)
      println("MSHR depth = "+DUT.DCache.NMshrEntry+". Subentries = "+DUT.DCache.NMshrSubEntry)

      DUT.io.memReq_ready.poke(true.B)
      //DUT.io.pipe_req.bits.addr := Cat(blockAddr, blockAddrOffset)

      DUT.io.coreReq.valid.poke(false.B)
      DUT.io.coreRsp.ready.poke(true.B)

      DUT.reset.poke(true.B)
      DUT.clock.step(1)
      DUT.reset.poke(false.B)
      DUT.clock.step(5)

      val filename = "ventus/txt/DCache/RRRRRmiss_diff.txt"
      val file = Source.fromFile(filename)
      val fileLines = file.getLines()

      for (line <- fileLines) {
        if (line.trim.nonEmpty) { // 检测line变量是否为空行
          val fields = line.split(",")// 字段中不允许存在空格
          // fields格式：
          // 0: op,
          // 1: param,
          // 2: warp id,
          // 3: reg idx,
          // 4: block idx,
          // 5: block offset(0),
          // 6: vector or scalar,
          // 7: data(0)
          if (fields.length != 8) {
            println("错误：元素个数不为8！")
            sys.exit(1)
          }
          DUT.io.coreReq.valid.poke(true.B)
          DUT.io.coreReq.bits.opcode.poke(fields(0).U)
          DUT.io.coreReq.bits.param.poke(fields(1).U)
          DUT.io.coreReq.bits.instrId.poke(fields(3).U)
          val blockIdxFromTxt = fields(4).U
          val tagFromTxt = blockIdxFromTxt(DUT.DCache.SetIdxBits+DUT.DCache.TagBits-1,DUT.DCache.SetIdxBits)
          val setIdxFromTxt = blockIdxFromTxt(DUT.DCache.SetIdxBits-1,0)
          DUT.io.coreReq.bits.tag.poke(tagFromTxt)
          DUT.io.coreReq.bits.setIdx.poke(setIdxFromTxt)
          // 目前只支持测试标量
          DUT.io.coreReq.bits.perLaneAddr(0).blockOffset.poke(fields(5).U)
          if(fields(6) == "d0"){
            DUT.io.coreReq.bits.perLaneAddr(0).activeMask.poke(true.B)
            DUT.io.coreReq.bits.perLaneAddr(0).wordOffset1H.poke("b1111".U)
            (1 until DUT.DCache.NLanes).foreach { iofL =>
              DUT.io.coreReq.bits.perLaneAddr(iofL).activeMask.poke(false.B)
              DUT.io.coreReq.bits.perLaneAddr(iofL).wordOffset1H.poke("b1111".U)
            }
          } else if (fields(6) == "d1"){
            (1 until DUT.DCache.NLanes).foreach { iofL =>
              DUT.io.coreReq.bits.perLaneAddr(iofL).activeMask.poke(true.B)
              DUT.io.coreReq.bits.perLaneAddr(iofL).wordOffset1H.poke("b1111".U)
            }
          } else {
            println("错误：vector or scalar栏格式错误！")
            sys.exit(1)
          }
          DUT.io.coreReq.bits.data(0).poke(fields(7).U)
          fork
            .withRegion(Monitor) {
              while (DUT.io.coreReq.ready.peek().litToBoolean == false) {
                DUT.clock.step(1)
              }
            }
            .joinAndStep(DUT.clock)
        } else {
          DUT.io.coreReq.valid.poke(false.B)
          DUT.clock.step(1)
        }
      }
      //DUT.io.coreReq.enqueueSeq()
      DUT.io.coreReq.valid.poke(false.B)

      DUT.clock.step(30)
      file.close()
    }
  }
}
class hello_test2 extends AnyFreeSpec with ChiselScalatestTester{
  "first_test" in {
    val caseName = "gaussian"
    test(new GPGPU_ExtMemWrapper(TestCaseList(caseName))).withAnnotations(Seq(WriteVcdAnnotation)){ c =>
      c.clock.setTimeout(0)
      c.clock.step(TestCaseList(caseName).cycles)
    }
  }
}

class single extends AnyFreeSpec with ChiselScalatestTester{
  "first_test" in {
    test(new pipe()).withAnnotations(Seq(WriteVcdAnnotation)) { div =>
      //c.io.in1.poke(2.U)
      //def input(a: Int) = chiselTypeOf(div.io.inst.get.bits.Lit(_.bits -> a.U))
      div.io.inst.get.initSource().setSourceClock(div.clock)
      fork{
        div.io.inst.get.enqueueSeq(Seq(
          //0:   5e02b0d7                vmv.v.i v1,5
          //4:   4a111157                vfcvt.f.xu.v    v2,v1
          //8:   5e0331d7                vmv.v.i v3,6
          //c:   4a311257                vfcvt.f.xu.v    v4,v3
          //10:  922212d7                vfmul.vv        v5,v2,v4
          //14:  00008067                jalr    x0,0(x1)


          //0:   5e02b0d7                vmv.v.i v1,5
          //4:   4a111157                vfcvt.f.xu.v    v2,v1
          //8:   5e0331d7                vmv.v.i v3,6
          //c:   4a311257                vfcvt.f.xu.v    v4,v3
          //10:   822212d7                vfdiv.vv        v5,v2,v4
          //14:   4e201357                vfsqrt.v        v6,v2
          //18:   00008067                jalr    x0,0(x1)


          (0x5e02b0d7L).U(32.W),
          (0x4a111157L).U(32.W),
          (0x5e0331d7L).U(32.W),
          (0x4a311257L).U(32.W),
          (0x822212d7L).U(32.W),
          (0x4e201357L).U(32.W),
        )++Seq.fill(10)((0x00000513L).U(32.W)))
      }.fork{

      }.join()

      div.clock.step(500)
      //c.io.in2.poke(3.U)
    }
  }
}