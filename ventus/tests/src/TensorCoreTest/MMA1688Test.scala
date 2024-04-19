package TensorCoreTest

import FPUv2.utils.{FPUInput, RoundingModes, TestFPUCtrl}
import FPUv2.{TCDotProduct,TCComputationInput}
import chisel3._
import chisel3.experimental.BundleLiterals._
import chisel3.experimental.VecLiterals.AddVecLiteralConstructor
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec

import FPUv2.vExeData

class MMA1688Test extends AnyFlatSpec with ChiselScalatestTester {
  import TestArgs._
  object TCMMA1688Input {
    var count = 0
    def reset = { count = 0 }
    def apply(DimM:Int, DimN:Int, DimK:Int) = {
      count = (count + 1) % 32
      new TC_MMA1688Input(new TCCtrl(32, 1)).Lit(
        _.data_in -> new vExeData().Lit(
          _.in1 -> Vec(32, UInt(64.W)).Lit(
            (0 until 32).map{ i => i -> "0x3C003C003C003C00".U}:_*
          ),
          _.in2 -> Vec(32, UInt(64.W)).Lit(
            (0 until 32).map{ i => i -> "0x3C003C003C003C00".U}:_*
          ),
          _.in3 -> Vec(32, UInt(64.W)).Lit(
            (0 until 32).map{ i => i -> "0x3C003C003C003C00".U}:_*
          ),
          _.mask -> Vec(32,Bool()).Lit(
            (0 until 32).map{ i => i -> false.B}:_*
          )
        ),
        _.rm -> RoundingModes.RNE,
        _.ctrl -> new TCCtrl(32, 1).Lit(
          _.reg_idxw -> count.U,
          _.warpID -> 0.U
        )
      )
    }

  }


  behavior of "Tensor Core Computation Array"
  it should "TCComputationArray 848 FP16" in {
    test(new TC_MMA1688(16,8,8,16,new TCCtrl(32, 1))).withAnnotations(Seq(WriteVcdAnnotation)) { d =>
      TCMMA1688Input.reset
      d.io.in.initSource()
      d.io.in.setSourceClock(d.clock)
      d.io.out.initSink()
      d.io.out.setSinkClock(d.clock)
      d.clock.setTimeout(80)
      d.io.out.ready.poke(true.B)
      d.io.in.valid.poke(true.B)
      fork{
        d.io.in.enqueueSeq(Seq(
          TCMMA1688Input(8,4,8)
        ))
      }.fork {
        d.io.out.ready.poke(true.B)
      }.join()
      d.clock.step(40)
    }
  }



}

