package top

import circt.stage.ChiselStage

/** Emit one complete Ventus SM for equal-flow area comparison. */
object SM_gen extends App {
  val targetDir = sys.env.getOrElse("VENTUS_SM_TARGET_DIR", "generated-sm")
  ChiselStage.emitSystemVerilogFile(
    new SM_wrapper(FakeCache = false, sm_id = 0, SV = None),
    args = Array("--target-dir", targetDir),
    firtoolOpts = Array(
      "--disable-mem-randomization",
      "--disable-reg-randomization",
      "-lowering-options=disallowLocalVariables"
    )
  )
}
