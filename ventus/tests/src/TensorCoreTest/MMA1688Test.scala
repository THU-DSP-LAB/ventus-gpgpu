package TensorCoreTest

import FPUv2.utils.{FPUInput, RoundingModes, TestFPUCtrl}
import TensorCore.{TCComputationInput, TCCtrl, TCDotProduct, TC_MMA1688, TC_MMA1688Input, TC_MMA1688Output}
import chisel3._
import chisel3.experimental.BundleLiterals._
import chisel3.experimental.VecLiterals.AddVecLiteralConstructor
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec
import pipeline.{CtrlSigs, InstWriteBack, vExeData}
import play.TestUtils.RequestSender

class MMA1688Test extends AnyFlatSpec with ChiselScalatestTester {
//  import FPUv2.TestArgs._
  object TCMMA1688Input {
    var count = 0
    def reset = { count = 0 }
    def apply(DimM:Int, DimN:Int, DimK:Int) = {
      count = (count + 1) % 32
      new TC_MMA1688Input(new TCCtrl(32, 1)).Lit(
        _.data_in -> new vExeData().Lit(
          _.in1 -> Vec(32, UInt(64.W)).Lit(
            (0 until 32).map{ i => i -> "h3C003C003C003C00".U}:_*
          ),
          _.in2 -> Vec(32, UInt(64.W)).Lit(
            (0 until 32).map{ i => i -> "h3C003C003C003C00".U}:_*
          ),
          _.in3 -> Vec(32, UInt(64.W)).Lit(
            (0 until 32).map{ i => i -> "h3C003C003C003C00".U}:_*
          ),
          _.mask -> Vec(32,Bool()).Lit(
            (0 until 32).map{ i => i -> false.B}:_*
          ),
          _.ctrl -> genBundle_bulk()
        ),
        _.rm -> RoundingModes.RNE,
        _.ctrl -> new TCCtrl(32, 1).Lit(
          _.reg_idxw -> count.U,
          _.warpID -> 0.U
        )
      )
    }
  def genBundle_bulk(): CtrlSigs = {
    val ctrlsigs = (new CtrlSigs).Lit(
      _.inst -> 0.U,
      _.wid -> 0.U,
      _.fp -> false.B,
      _.branch -> 0.U,
      _.simt_stack -> false.B,
      _.simt_stack_op -> false.B,
      _.barrier -> false.B,
      _.csr -> 0.U,
      _.reverse -> false.B,
      _.sel_alu2 -> 0.U,
      _.sel_alu1 -> 0.U,
      _.isvec -> false.B,
      _.sel_alu3 -> 0.U,
      _.mask -> false.B,
      _.sel_imm -> 0.U,
      _.mem_whb -> 0.U,
      _.mem_unsigned -> false.B,
      _.alu_fn -> 0.U,
      _.force_rm_rtz -> false.B,
      _.is_vls12 -> false.B,
      _.mem -> false.B,
      _.mul -> false.B,
      _.tc -> true.B,
      _.disable_mask -> false.B,
      _.custom_signal_0 -> false.B,
      _.mem_cmd -> 0.U,
      _.mop -> 0.U,
      _.reg_idx1 -> 0.U,
      _.reg_idx2 -> 0.U,
      _.reg_idx3 -> 0.U,
      _.reg_idxw -> 0.U,
      _.wvd -> false.B,
      _.fence -> false.B,
      _.sfu -> false.B,
      _.readmask -> false.B,
      _.writemask -> false.B,
      _.wxd -> false.B,
      _.pc -> 4096.U,
      _.imm_ext -> 0.U,
      _.spike_info.get -> (new InstWriteBack).Lit(_.sm_id -> 0.U,_.pc -> 0.U, _.inst -> 0.U),
      _.atomic -> false.B,
      _.aq -> false.B,
      _.rl -> false.B,
    )
    ctrlsigs
  }
  }


  behavior of "Tensor Core"
  it should "TCComputationArray 1688 FP16" in {
    test(new TC_MMA1688(16,8,8,16,new TCCtrl(32, 1))).withAnnotations(Seq(WriteVcdAnnotation)) { d =>
      TCMMA1688Input.reset
      d.io.in.initSource()
      d.io.in.setSourceClock(d.clock)
      d.io.out.initSink()
      d.io.out.setSinkClock(d.clock)
      d.clock.setTimeout(80)

      val input_list = Seq(TCMMA1688Input(8,4,8))

      val TC_input_sender = new RequestSender[TC_MMA1688Input, TC_MMA1688Output](d.io.in, d.io.out)

      TC_input_sender.add(input_list)

      d.clock.setTimeout(0)
      d.clock.step(4)
      var clock_cnt = 0

      while(clock_cnt <= 100){
        TC_input_sender.eval()
        d.io.out.ready.poke(true.B)
        d.clock.step()
        clock_cnt += 1
      }
      d.clock.step(30)
    }
  }
}

