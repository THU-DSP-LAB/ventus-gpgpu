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
import chisel3.util._
import chisel3.experimental.BundleLiterals.AddBundleLiteralConstructor
import chisel3.experimental.VecLiterals.AddVecLiteralConstructor
import chiseltest._
import org.scalatest.freespec.AnyFreeSpec
import chiseltest.simulator.WriteVcdAnnotation
import pipeline.IDecode._
import testbox.{vALUv2TestInput, vALUv2TestWrapper}
import top._

// add new testcases here!
object TestCaseList{
  val L: Map[String, TestCase#Props] = Array[TestCase](
    new TestCase("gaussian", "gaussian_.vmem", "gaussian8.data", 8, 8, 0, 5000),
    new TestCase("saxpy", "saxpy_.vmem", "saxpy.data", 8, 8, 0, 400),
    new TestCase("gemm", "gemm_.vmem", "gemm4x8x4.data", 1, 8, 0, 2400),
    //new TestCase("gemm", "gemm_.vmem", "gemm8x16x12.data", 2, 8, 0, 300),
    new TestCase("saxpy2", "saxpy2_.vmem", "saxpy.data", 1, 8, 0, 800)
  ).map{x => (x.name, x.props)}.toMap

  def apply(s: String) = TestCaseList.L(s)
}

class hello_test2 extends AnyFreeSpec with ChiselScalatestTester{
  "first_test" in {
    val caseName = "saxpy2"
    test(new GPGPU_ExtMemWrapper(TestCaseList(caseName)))/*.withAnnotations(Seq(WriteVcdAnnotation))*/ { c =>
      c.clock.setTimeout(0)
      c.clock.step(TestCaseList(caseName).cycles)
    }
  }
}


class vALUv2_test() extends AnyFreeSpec with ChiselScalatestTester{
  val softThread = 12
  val hardThread = 4
  object vALUInput { d =>
    var count = 0
    def reset = { count = 0 }
    def apply(a: Int, b: Int, c: Int, op: UInt) = {
      count = (count + 1) % 32
      (new vALUv2TestInput(softThread)).Lit(
        _.in1 -> Vec(softThread, UInt(32.W)).Lit((0 until softThread).map{ i => i -> (a+i).U }:_*),
        _.in2 -> Vec(softThread, UInt(32.W)).Lit((0 until softThread).map{ i => i -> b.U }:_*),
        _.in3 -> Vec(softThread, UInt(32.W)).Lit((0 until softThread).map{ i => i -> c.U }:_*),
        _.op -> op,
        _.count -> count.U
      )
    }
  }
  "vALU_Test" in {
    test(new vALUv2TestWrapper(12, 4)).withAnnotations(Seq(WriteVcdAnnotation)){ c =>
      c.io.in.initSource()
      c.io.in.setSourceClock(c.clock)
      c.io.out.initSink()
      c.io.out2simt_stack.initSink()
      c.io.out.setSinkClock(c.clock)
      c.io.out.setSinkClock(c.clock)
      c.io.out.ready.poke(true.B)
      c.io.out2simt_stack.ready.poke(true.B)
      fork{
        c.io.in.enqueueSeq(Seq(
          vALUInput(0, 100, 0, FN_ADD),
          vALUInput(1000, 100, 0, FN_ADD)
        ))
      }.fork{
        c.io.out.ready.poke(true.B)
      }.fork{
        c.io.out2simt_stack.ready.poke(true.B)
      }.fork{
        c.clock.step(4)
        c.io.out.ready.poke(false.B)
        c.clock.step(5)
        c.io.out.ready.poke(true.B)
      }.join()
      c.clock.step(8)
    }
  }
}
