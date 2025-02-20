package TensorCore
import pipeline.TCCtrl_mulslot_v2
import top.parameters._

object emitVerilog extends App {
  chisel3.emitVerilog(
    new TensorCore_MixedPrecision_multslot_simple(DimM=8, DimN=8, DimK=8, slot_num = num_warp, xDatalen=16, new TCCtrl_mulslot_v2(xLen, depth_warp)),
    Array("--target-dir", "generated/")
  )
}
