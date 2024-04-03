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
//import chiseltest.simulator.
import pipeline.pipe
import top._

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

class AdvancedTest extends AnyFreeSpec with ChiselScalatestTester{ // Working in progress
  import top.helper._

  case class AdvTest(name: String, meta: Seq[String], data: Seq[String], var warp: Int, var cycles: Int)

  "adv_test" in {
    // TODO: rename

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

    val metas = metaFileDir.map(MetaData(_))

    parameters.num_warp = (metas.map(_.wg_size.toInt) :+ testbench.warp).max
    assert(metas.map(_.wf_size.toInt == metas.head.wf_size.toInt).reduceLeft(_ && _))
    parameters.num_thread = metas.head.wf_size.toInt

    print(s"Hardware: num_warp = ${parameters.num_warp}, num_thread = ${parameters.num_thread}\n")

    val mem = new MemBox

    test(new GPGPU_SimWrapper(FakeCache = false)).withAnnotations(Seq(WriteVcdAnnotation, VerilatorBackendAnnotation)){ c =>

      def waitForValid[T <: Data](x: ReadyValidIO[T], maxCycle: BigInt): Boolean = {
        while (x.valid.peek().litToBoolean == false) {
          if(c.io.cnt.peek().litValue > maxCycle)
            return false
          c.clock.step(1)
        }
        true
      }
      def memLatency = 5

      c.io.host_req.initSource()
      c.io.host_req.setSourceClock(c.clock)
      c.io.out_d.initSource()
      c.io.out_d.setSourceClock(c.clock)
      c.io.host_rsp.initSink()
      c.io.host_rsp.setSinkClock(c.clock)
      c.io.out_a.initSink()
      c.io.out_a.setSinkClock(c.clock)
      c.clock.setTimeout(200)
      c.clock.step(5)

      var size3d = Array.fill(3)(0)
      var wg_list = Array.fill(metaFileDir.length)(Array.fill(1)(false))

      val DelayMem = new DelayFIFO[DelayFIFOEntry](memLatency, memLatency + 5)
      val data_byte_count = c.io.out_a.bits.data.getWidth/8 // bits count -> bytes count
      fork{
        while(c.io.cnt.peek().litValue <= maxCycle){
          c.io.out_a.ready.poke((!DelayMem.isFull).B)
          c.clock.step(1)
          DelayMem.step()
        }
        c.clock.step(2)
      }.fork{ // HOST <-> GPU
        def enq(knl: Int) = fork{
          for (i <- 0 until size3d(0);
               j <- 0 until size3d(1);
               k <- 0 until size3d(2)
               ) {
            c.io.host_req.enqueue(metas(knl).generateHostReq(i, j, k))
          }
        }
        def deq(knl: Int) = fork {
          timescope {
            c.io.host_rsp.ready.poke(true.B)
            if (waitForValid(c.io.host_rsp, maxCycle)) {
              val rsp = c.io.host_rsp.bits.peek().litValue
              val extract_rsp = (rsp >> parameters.CU_ID_WIDTH).toInt // See Also: MemBox.scala/MetaData.generateHostReq()
              wg_list(knl)(extract_rsp) = true
            }
            c.clock.step(1)
          }
        }
        metaFileDir.indices.foreach { i =>
          mem.loadfile(metas(i), dataFileDir(i))
          size3d = metas(i).kernel_size.map(_.toInt)
          wg_list(i) = Array.fill(size3d(0) * size3d(1) * size3d(2))(false)
          if(c.io.cnt.peek().litValue <= maxCycle){
            print(s"kernel $i \n")
            enq(i).join()
            while (!wg_list(i).reduce(_ && _) && c.io.cnt.peek().litValue <= maxCycle) {
              deq(i).join()
            }
          }
          c.clock.step(2)
        }
      }.fork{ // GPU <-> MEM
        fork{
          while (!wg_list.flatten.reduce(_ && _) && c.io.cnt.peek().litValue.toInt <= maxCycle) {
            if(!wg_list.flatten.reduce(_ && _)){
              //c.io.out_a.ready.poke((!DelayMem.isFull).B)
              if (DelayMem.canPop) {
                val out = DelayMem.pop
                c.io.out_d.enqueue(new TLBundleD_lite(parameters.l2cache_params).Lit(
                  _.opcode -> out.opcode.U, // w:0 r:1
                  _.data -> out.data.U,
                  _.source -> out.source.U,
                  _.size -> out.size.U, // TODO: Unused
                  _.param -> out.param.U
                ))
              }
            }
            //c.io.out_a.ready.poke((!DelayMem.isFull).B)
            c.clock.step(1)
          }
        }.fork{
          while (!wg_list.flatten.reduce(_ && _) && c.io.cnt.peek().litValue.toInt <= maxCycle) {
            if (!wg_list.flatten.reduce(_ && _)) {
              if (waitForValid(c.io.out_a, maxCycle)) {
                if(!DelayMem.isFull) {
                  val addr = c.io.out_a.bits.address.peek().litValue
                  var opcode_rsp = 0
                  val source = c.io.out_a.bits.source.peek().litValue
                  var data = new Array[Byte](data_byte_count)
                  if (c.io.out_a.bits.opcode.peek().litValue == 4) { // read
                    data = mem.readMem(addr, data_byte_count) // read operation
                    opcode_rsp = 1
                  }
                  else if (c.io.out_a.bits.opcode.peek().litValue == 1) { // write partial
                    data = BigInt2ByteArray(c.io.out_a.bits.data.peek().litValue, data_byte_count)
                    val mask = c.io.out_a.bits.mask.peek().litValue.toString(2).reverse.padTo(c.io.out_a.bits.mask.getWidth, '0').map {
                      case '1' => true
                      case _ => false
                    }.flatMap(x => Seq.fill(4)(x)) // word mask -> byte mask, no byte/halfword support yet
                    mem.writeMem(addr, data_byte_count, data, mask) // write operation
                    data = Array.fill(data_byte_count)(0.toByte) // response = 0
                    opcode_rsp = 0
                  }
                  else if (c.io.out_a.bits.opcode.peek().litValue == 0) { // write full
                    data = BigInt2ByteArray(c.io.out_a.bits.data.peek().litValue, data_byte_count)
                    val mask = IndexedSeq.fill(4 * c.io.out_a.bits.mask.getWidth)(true)
                    mem.writeMem(addr, data_byte_count, data, mask) // write operation
                    data = Array.fill(data_byte_count)(0.toByte) // response = 0
                    opcode_rsp = 0
                  }
                  else {
                    data = Array.fill(data_byte_count)(0.toByte)
                  }
                  DelayMem.push(DelayFIFOEntry(opcode_rsp, ByteArray2BigInt(data), source, 0, 0))
                }
                c.clock.step(1)
              }
            }
          }
        }.join
      }.join
    }
  }
}