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

import L1Cache.MyConfig
import L2cache.TLBundleD_lite
import chisel3._
import chisel3.util._
import chisel3.experimental.BundleLiterals.AddBundleLiteralConstructor
import chisel3.experimental.VecLiterals.AddVecLiteralConstructor
import chiseltest._
import org.scalatest.freespec
import org.scalatest.freespec.AnyFreeSpec
import chiseltest.simulator.WriteFstAnnotation
import mmu.AsidLookupEntry
import play.TestUtils._

import scala.collection.mutable.ArrayBuffer
//import chiseltest.simulator.
import pipeline.pipe
import top._
import config.config.Parameters

// add new testcases here!
object TestCaseList{
  // IMPORTANT:
  // `parameters.num_warp` should >= `warp` parameter in TestCase() below
  // the simulation may be slow if `parameters.num_warp` is large
  val L: Map[String, TestCase#Props] = Array[TestCase](
    // TODO: Refresh file
    new TestCase("gaussian", "gaussian_.vmem", "gaussian8.data", 8, 8, 0, 5000),
    new TestCase("saxpy", "saxpy_.vmem", "saxpy.data", 4, 4, 0, 500),
    new TestCase("gemm", "gemm_.vmem", "gemm4x8x4.data", 1, 8, 0, 2400),
    //new TestCase("gemm", "gemm_.vmem", "gemm8x16x12.data", 2, 8, 0, 300),
    new TestCase("saxpy2", "saxpy2_.vmem", "saxpy.data", 1, 8, 0, 800)
  ).map{x => (x.name, x.props)}.toMap

  def apply(s: String) = TestCaseList.L(s)
}

class hello_test2 extends AnyFreeSpec with ChiselScalatestTester{
  "first_test" in {
    val caseName = "saxpy"
    test(new GPGPU_ExtMemWrapper(TestCaseList(caseName))).withAnnotations(Seq(WriteVcdAnnotation)){ c =>
      c.clock.setTimeout(0)
      c.clock.step(TestCaseList(caseName).cycles)
    }
  }
}

class single extends AnyFreeSpec with ChiselScalatestTester{
  val param = (new MyConfig).toInstance
  "first_test" in {
    test(new pipe(sm_id = 0, SV = Some(mmu.SV32))(param)).withAnnotations(Seq(WriteVcdAnnotation)) { div =>
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

case class AdvTest(name: String, meta: Seq[String], data: Seq[String], var warp: Int, var cycles: Int)

class AdvancedTest extends AnyFreeSpec with ChiselScalatestTester{ // Working in progress
  import top.helper._



  "adv_test_dma" in {
    import TestUtils._
    val iniFile = new IniFile("./ventus/txt/_cases.ini")
    val defaultCaseName: String = iniFile.sections("")("Default").head
    val section = iniFile.sections(defaultCaseName)

    val testbench = AdvTest(
      defaultCaseName,
      section("Files").map(_ + ".metadata"),
      section("Files").map(_ + ".data"),
      section("nWarps").head.toInt,
      section("SimCycles").head.toInt
    )

    val metaFileDir = testbench.meta.map("./ventus/txt/" + testbench.name + "/" + _)
    val dataFileDir = testbench.data.map("./ventus/txt/" + testbench.name + "/" + _)
    val maxCycle = testbench.cycles
//    val maxCycle = 1

    val metas = metaFileDir.map(MetaData(_))

    parameters.num_warp = (metas.map(_.wg_size.toInt) :+ testbench.warp).max
    //assert(metas.map(_.wf_size.toInt == metas.head.wf_size.toInt).reduceLeft(_ && _))
    parameters.num_thread = metas.head.wf_size.toInt

    print(s"Hardware: num_warp = ${parameters.num_warp}, num_thread = ${parameters.num_thread}\n")

    val mem = new MemBox(MemboxS.SV32)
    //mem.loadfile(0, metas.head, dataFileDir.head)
    test(new GPGPU_SimWrapper(FakeCache = false, Some(mmu.SV32))).withAnnotations(Seq(VerilatorBackendAnnotation, WriteVcdAnnotation)){ c =>
      c.io.host_req.initSource()
      c.io.host_req.setSourceClock(c.clock)
      c.io.out_d.initSource()
      c.io.out_d.setSourceClock(c.clock)
      c.io.host_rsp.initSink()
      c.io.host_rsp.setSinkClock(c.clock)
      c.io.out_a.initSink()
      c.io.out_a.setSinkClock(c.clock)
      c.clock.setTimeout(23500)
      c.clock.step(5)

      var meta = new MetaData
      var size3d = Array.fill(3)(0)
      var wg_list = Array.fill(metaFileDir.length)(Array.fill(1)(false))
      val ptbr_table = ArrayBuffer.fill(256)(BigInt(-1))
      var ptbr_pos = 1

      var current_kernel = 0
      var clock_cnt = 0
      var timestamp = 0
      object AsidException extends Exception{
        val message = "Run out of ASID"
        val cause: Throwable = null
      }
      class RequestSenderGPU(gap: Int = 5) extends RequestSender(c.io.host_req, c.io.host_rsp){
        override def finishWait(): Boolean = {
          clock_cnt - timestamp > gap
        }
        def senderEval(): Unit = {
          if(checkForValid(reqPort) && checkForReady(reqPort)){
            send_list = send_list.tail
          }
          if(send_list.nonEmpty && finishWait()){
            reqPort.valid.poke(true.B)
            reqPort.bits.poke(send_list.head)
          }
          else{
            reqPort.valid.poke(false.B)
          }
        }
        def receiverEval(): Unit = {
          if(checkForValid(rspPort) && checkForReady(rspPort)){
            val rsp = c.io.host_rsp.bits.peek().litValue
            val extract_rsp = (rsp >> parameters.CU_ID_WIDTH).toInt // See Also: MemBox.scala/MetaData.generateHostReq()
            wg_list(current_kernel)(extract_rsp) = true
          }
          rspPort.ready.poke(true.B)
        }
        override def eval() = {
          senderEval()
          receiverEval()
        }
      }

      val host_driver = new RequestSenderGPU(5)
      val mem_driver = new MemPortDriverDelay(c.io.out_a, c.io.out_d, mem, 20, 3)

      while(clock_cnt <= maxCycle && !wg_list.flatten.reduce(_ && _)){
        if(clock_cnt - timestamp == 0){
          print(s"kernel ${current_kernel} ${dataFileDir(current_kernel)}\n")

          if(ptbr_table(ptbr_pos) == -1){
            ptbr_table(ptbr_pos) = mem.createRootPageTable()
            c.io.asid_fill.valid.poke(true.B)
            c.io.asid_fill.bits.poke((new AsidLookupEntry(mmu.SV32)).Lit(
              _.ptbr -> ptbr_table(ptbr_pos).U,
              _.asid -> ptbr_pos.U,
              _.valid -> true.B
            ))
          }
          else{ c.io.asid_fill.valid.poke(false.B) }

          meta = mem.loadfile(ptbr_table(ptbr_pos), metas(current_kernel), dataFileDir(current_kernel))
          meta.asid = ptbr_pos
          size3d = meta.kernel_size.map(_.toInt)
          wg_list(current_kernel) = Array.fill(size3d(0) * size3d(1) * size3d(2))(false)

          host_driver.add(
            for {
              i <- 0 until size3d(0)
              j <- 0 until size3d(1)
              k <- 0 until size3d(2)
            } yield meta.generateHostReq(i, j, k)
          )
          ptbr_pos += 1
        }
        else c.io.asid_fill.valid.poke(false.B)

        if(ptbr_pos >= 255) throw AsidException

        host_driver.eval()
        mem_driver.eval()

        c.clock.step(1)
        clock_cnt += 1

        if (wg_list(current_kernel).reduce(_ && _) && host_driver.send_list.isEmpty) {
          timestamp = clock_cnt
          current_kernel += 1
        }
      }
      print(s"FIN ${clock_cnt} |")
      if(top.parameters.INST_CNT){
        c.io.inst_cnt.zipWithIndex.foreach{ case(x, i) =>
          print(s" [${i}: ${x.peek.litValue.toInt}]")
        }
      }
      else if(top.parameters.INST_CNT_2){
        c.io.inst_cnt.zipWithIndex.foreach{ case(x, i) =>
          print(s" [${i}: X: ${x.peek.litValue & ((BigInt(1) << 32) - 1)} V: ${x.peek.litValue >> 32}]")
        }
      }
      print("\n")
      Seq.fill(3000){
        mem_driver.eval()
        c.clock.step(1)
        clock_cnt +=1
      }
    }
  }
}

class PageTableTest extends AnyFreeSpec{
  "Page Table builder" in {
    val iniFile = new IniFile("./ventus/txt/_cases.ini")
    val defaultCaseName: String = iniFile.sections("")("Default").head
    val section = iniFile.sections(defaultCaseName)

    val testbench = AdvTest(
      defaultCaseName,
      section("Files").map(_ + ".metadata"),
      section("Files").map(_ + ".data"),
      section("nWarps").head.toInt,
      section("SimCycles").head.toInt
    )
    val metaFileDir = testbench.meta.map("./ventus/txt/" + testbench.name + "/" + _)
    val dataFileDir = testbench.data.map("./ventus/txt/" + testbench.name + "/" + _)

    var meta = new MetaData
    var size3d = Array.fill(3)(0)
    var wg_list = Array.fill(metaFileDir.length)(Array.fill(1)(false))
    val ptbr_table = ArrayBuffer.fill(256)(BigInt(-1))
    var ptbr_pos = 1

    val metas = metaFileDir.map(MetaData(_))

    val mem = new MemBox(MemboxS.SV32)
    ptbr_table(ptbr_pos) = mem.createRootPageTable()

    var current_kernel = 0
    meta = mem.loadfile(ptbr_table(ptbr_pos), metas(current_kernel), dataFileDir(current_kernel))
    meta.asid = ptbr_pos
  }
}