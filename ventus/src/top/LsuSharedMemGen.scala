package top

import L1Cache.MyConfig
import L1Cache.ShareMem.SharedMemory
import chisel3._
import chisel3.util._
import config.config.Parameters
import mainargs.{ParserForMethods, arg, main}
import pipeline.{DCacheCoreRsp_np, ShareMemCoreReq_np}
import _root_.circt.stage.ChiselStage

/** A synthesis-friendly wrapper that keeps only the LSU-side shared-memory
  * request/response interface plus the real SharedMemory datapath underneath.
  *
  * This isolates the LSU -> crossbar -> shared-memory subsystem so area/timing
  * studies are not polluted by the rest of the SM frontend/backend.
  */
class LsuSharedMemTop(implicit p: Parameters) extends Module {
  val io = IO(new Bundle {
    val req = Flipped(DecoupledIO(new ShareMemCoreReq_np))
    val rsp = DecoupledIO(new DCacheCoreRsp_np)
  })

  val sharedmem = Module(new SharedMemory)

  sharedmem.io.coreReq.bits.data := io.req.bits.data
  sharedmem.io.coreReq.bits.instrId := io.req.bits.instrId
  sharedmem.io.coreReq.bits.isWrite := io.req.bits.isWrite
  sharedmem.io.coreReq.bits.setIdx := io.req.bits.setIdx
  sharedmem.io.coreReq.bits.perLaneAddr := io.req.bits.perLaneAddr
  sharedmem.io.coreReq.valid := io.req.valid
  io.req.ready := sharedmem.io.coreReq.ready

  sharedmem.io.coreRsp.ready := io.rsp.ready
  io.rsp.valid := sharedmem.io.coreRsp.valid
  io.rsp.bits.data := sharedmem.io.coreRsp.bits.data
  io.rsp.bits.instrId := sharedmem.io.coreRsp.bits.instrId
  io.rsp.bits.activeMask := sharedmem.io.coreRsp.bits.activeMask

  // Keep the req/rsp boundary visible through elaboration so the shared-memory
  // datapath is preserved as an externally observable block in synthesis.
  dontTouch(sharedmem.io.coreReq.ready)
  dontTouch(sharedmem.io.coreRsp.valid)
  dontTouch(sharedmem.io.coreRsp.bits.data)
}

object LsuSharedMemGen {
  private val TopModuleName = "LsuSharedMemTop"

  private def autoDirName(
    dirPrefix: String,
    numWarp: Int,
    numThread: Int,
    sharedmemCapacityBytes: Int,
    sharedmemNBanks: Int
  ): String = {
    val cfg = HardwareConfig.defaults.copy(
      numSm = 1,
      numWarp = numWarp,
      numThread = numThread,
      sharedmemNBanks = sharedmemNBanks,
      sharedmemCapacityBytes = sharedmemCapacityBytes
    )
    val sharedmemBandwidthBits = HardwareConfig.derivedSharedmemBandwidthBits(cfg)
    val extraPrefix = if (dirPrefix.nonEmpty) s"_${dirPrefix}" else ""
    s"${TopModuleName}${extraPrefix}_warp${numWarp}_thread${numThread}_smem${sharedmemCapacityBytes}B_smbank${sharedmemNBanks}_smbw${sharedmemBandwidthBits}"
  }

  private def resolveTargetDir(
    targetDir: String,
    outputRoot: String,
    dirPrefix: String,
    numWarp: Int,
    numThread: Int,
    sharedmemCapacityBytes: Int,
    sharedmemNBanks: Int
  ): String = {
    if (targetDir.nonEmpty) targetDir
    else s"$outputRoot/${autoDirName(dirPrefix, numWarp, numThread, sharedmemCapacityBytes, sharedmemNBanks)}"
  }

  @main
  def elaborate(
    @arg(name = "target-dir", doc = "exact output directory, overrides auto naming") targetDir: String = "",
    @arg(name = "output-root", doc = "base directory for auto-named outputs") outputRoot: String = "sim-verilator-nocache/lsuSharedMem",
    @arg(name = "dir-prefix", doc = "extra prefix appended after the top-module name in the auto-named output folder") dirPrefix: String = "",
    @arg(name = "num-warp", doc = "warps used by LSU/shared-memory metadata") numWarp: Int = HardwareConfig.defaults.numWarp,
    @arg(name = "num-thread", doc = "threads per warp") numThread: Int = HardwareConfig.defaults.numThread,
    @arg(name = "sharedmem-nbanks", doc = "shared memory bank count; defaults to num-thread when omitted") sharedmemNBanks: Int = -1,
    @arg(name = "sharedmem-capacity-bytes", doc = "shared memory capacity in bytes") sharedmemCapacityBytes: Int = HardwareConfig.defaults.sharedmemCapacityBytes,
    @arg(name = "dcache-block-words", doc = "kept only because shared-memory line decomposition reuses this lower bound") dcacheBlockWords: Int = HardwareConfig.defaults.dcacheBlockWords,
    @arg(name = "dcache-nsets", doc = "kept only because LSU/shared-memory request metadata reuses these widths") dcacheNSets: Int = HardwareConfig.defaults.dcacheNSets,
    @arg(name = "lsu-num-entry-each-warp", doc = "LSU entry count used by LSU-side req/rsp bundles") lsuNumEntryEachWarp: Int = HardwareConfig.defaults.lsuNumEntryEachWarp
  ): Unit = {
    val resolvedSharedmemNBanks = if (sharedmemNBanks > 0) sharedmemNBanks else numThread
    val resolvedTargetDir = resolveTargetDir(
      targetDir,
      outputRoot,
      dirPrefix,
      numWarp,
      numThread,
      sharedmemCapacityBytes,
      resolvedSharedmemNBanks
    )

    HardwareConfig.configure(
      HardwareConfig.defaults.copy(
        numSm = 1,
        numWarp = numWarp,
        numThread = numThread,
        numBlock = numWarp,
        dcacheNSets = dcacheNSets,
        dcacheBlockWords = dcacheBlockWords,
        sharedmemNBanks = resolvedSharedmemNBanks,
        sharedmemCapacityBytes = sharedmemCapacityBytes,
        lsuNumEntryEachWarp = lsuNumEntryEachWarp
      )
    )

    val param = (new MyConfig).toInstance
    ChiselStage.emitSystemVerilogFile(
      new LsuSharedMemTop()(param),
      args = Array("--target-dir", resolvedTargetDir),
      firtoolOpts = Array(
        "--disable-mem-randomization",
        "--disable-reg-randomization",
        "-lowering-options=disallowLocalVariables"
      )
    )

    ParametersToJson.saveToJson(s"$resolvedTargetDir/parameters.json")
    println(s"lsu-sharedmem output dir: $resolvedTargetDir")
  }

  def main(args: Array[String]): Unit = ParserForMethods(this).runOrExit(args.toIndexedSeq)
}
