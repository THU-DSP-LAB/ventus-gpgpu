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
import chisel3._
import chiseltest._
import org.scalatest.freespec
import org.scalatest.freespec.AnyFreeSpec
import chiseltest.simulator.WriteVcdAnnotation
import pipeline.pipe
import top._

// add new testcases here!
object TestCaseList{
  val L: Map[String, TestCase#Props] = Array[TestCase](
    new TestCase("gaussian", "gaussian_.vmem", "gaussian8.data", 4, 4, 0, 1000),
    new TestCase("saxpy", "saxpy_.vmem", "saxpy.data", 8, 8, 0, 400),
    new TestCase("gemm", "gemm_.vmem", "gemm4x8x4.data", 1, 8, 0, 2400),
    //new TestCase("gemm", "gemm_.vmem", "gemm8x16x12.data", 2, 8, 0, 300),
    new TestCase("saxpy2", "saxpy2_.vmem", "saxpy.data", 1, 8, 0, 800)
  ).map{x => (x.name, x.props)}.toMap

  def apply(s: String) = TestCaseList.L(s)
}

class hello_test2 extends AnyFreeSpec with ChiselScalatestTester{
  "first_test" in {
    val caseName = "gaussian"
    test(new GPGPU_ExtMemWrapper(TestCaseList(caseName)))/*.withAnnotations(Seq(WriteVcdAnnotation))*/ { c =>
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