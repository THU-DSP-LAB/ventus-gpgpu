package top

import L1Cache.MyConfig
import L1Cache.ShareMem.SharedMemory
import _root_.circt.stage.ChiselStage
import chisel3._
import chisel3.util._
import chisel3.stage.ChiselGeneratorAnnotation
import config.config.Parameters
import mainargs.{ParserForMethods, arg, main}
import pipeline.{DCacheCoreReq_np, DCacheCoreRsp_np, LSUexe, MSHROutput, ShareMemCoreReq_np, vExeData}

/** Synthesis-oriented wrapper for the LSU -> shared-memory path.
  *
  * The real LSU (`LSUexe`) remains in the datapath, so address calculation,
  * request shaping, MSHR bookkeeping, bank-conflict arbiter, and shared-memory
  * crossbars are all preserved. DCache stays external to keep this block much
  * smaller than a full SM while still capturing the LSU-side overhead.
  */
class LsuSharedMemTop(val lsuSharedmemPipeCut: Boolean = false)(implicit p: Parameters) extends Module {
  val io = IO(new Bundle {
    val lsu_req = Flipped(DecoupledIO(new vExeData))
    val lsu_rsp = DecoupledIO(new MSHROutput)
    val dcache_req = DecoupledIO(new DCacheCoreReq_np)
    val dcache_rsp = Flipped(DecoupledIO(new DCacheCoreRsp_np))
    val flush_dcache = Flipped(DecoupledIO(Bool()))
    val fence_end = Output(UInt(parameters.num_warp.W))
    val csr_wid = Output(UInt(parameters.depth_warp.W))
    val csr_pds = Input(UInt(parameters.xLen.W))
    val csr_numw = Input(UInt(parameters.xLen.W))
    val csr_tid = Input(UInt(parameters.xLen.W))
  })

  val lsu = Module(new LSUexe)
  val sharedmem = Module(new SharedMemory)
  val sharedReqPipe = if (lsuSharedmemPipeCut) Some(Module(new Queue(new ShareMemCoreReq_np, entries = 1, pipe = true, flow = false))) else None

  lsu.io.lsu_req <> io.lsu_req
  io.lsu_rsp <> lsu.io.lsu_rsp
  io.dcache_req <> lsu.io.dcache_req
  lsu.io.dcache_rsp <> io.dcache_rsp
  lsu.io.flush_dcache <> io.flush_dcache

  io.fence_end := lsu.io.fence_end
  io.csr_wid := lsu.io.csr_wid
  lsu.io.csr_pds := io.csr_pds
  lsu.io.csr_numw := io.csr_numw
  lsu.io.csr_tid := io.csr_tid

  val sharedReqSrc = Wire(DecoupledIO(new ShareMemCoreReq_np))
  sharedReqSrc.bits := DontCare
  sharedReqSrc.valid := false.B
  sharedReqPipe match {
    case Some(q) =>
      q.io.enq <> lsu.io.shared_req
      sharedReqSrc <> q.io.deq
    case None =>
      sharedReqSrc <> lsu.io.shared_req
  }

  sharedmem.io.coreReq.bits.data := sharedReqSrc.bits.data
  sharedmem.io.coreReq.bits.instrId := sharedReqSrc.bits.instrId
  sharedmem.io.coreReq.bits.isWrite := sharedReqSrc.bits.isWrite
  sharedmem.io.coreReq.bits.setIdx := sharedReqSrc.bits.setIdx
  sharedmem.io.coreReq.bits.perLaneAddr := sharedReqSrc.bits.perLaneAddr
  sharedmem.io.coreReq.valid := sharedReqSrc.valid
  sharedReqSrc.ready := sharedmem.io.coreReq.ready

  sharedmem.io.coreRsp.ready := lsu.io.shared_rsp.ready
  lsu.io.shared_rsp.valid := sharedmem.io.coreRsp.valid
  lsu.io.shared_rsp.bits.data := sharedmem.io.coreRsp.bits.data
  lsu.io.shared_rsp.bits.instrId := sharedmem.io.coreRsp.bits.instrId
  lsu.io.shared_rsp.bits.activeMask := sharedmem.io.coreRsp.bits.activeMask

  // Keep the LSU/shared-memory boundary visible to synthesis and reports.
  dontTouch(sharedReqSrc.valid)
  dontTouch(sharedReqSrc.bits.setIdx)
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
    sharedmemNBanks: Int,
    lsuSharedmemPipeCut: Boolean
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
    val pipeTag = if (lsuSharedmemPipeCut) "_pipe1" else ""
    s"${TopModuleName}${pipeTag}${extraPrefix}_warp${numWarp}_thread${numThread}_smem${sharedmemCapacityBytes}B_smbank${sharedmemNBanks}_smbw${sharedmemBandwidthBits}"
  }

  private def resolveTargetDir(
    targetDir: String,
    outputRoot: String,
    dirPrefix: String,
    numWarp: Int,
    numThread: Int,
    sharedmemCapacityBytes: Int,
    sharedmemNBanks: Int,
    lsuSharedmemPipeCut: Boolean
  ): String = {
    if (targetDir.nonEmpty) targetDir
    else s"$outputRoot/${autoDirName(dirPrefix, numWarp, numThread, sharedmemCapacityBytes, sharedmemNBanks, lsuSharedmemPipeCut)}"
  }

  @main
  def elaborate(
    @arg(name = "target-dir", doc = "exact output directory, overrides auto naming") targetDir: String = "",
    @arg(name = "output-root", doc = "base directory for auto-named outputs") outputRoot: String = "gen_lsu_sharedmem_verilog",
    @arg(name = "dir-prefix", doc = "extra prefix appended after the top-module name in the auto-named output folder") dirPrefix: String = "",
    @arg(name = "num-warp", doc = "warps used by LSU/shared-memory metadata") numWarp: Int = HardwareConfig.defaults.numWarp,
    @arg(name = "num-thread", doc = "threads per warp") numThread: Int = HardwareConfig.defaults.numThread,
    @arg(name = "num-block", doc = "max resident blocks per SM metadata, kept to satisfy shared LSU parameter checks") numBlock: Int = HardwareConfig.defaults.numBlock,
    @arg(name = "dcache-nsets", doc = "L1D set count reused by LSU metadata") dcacheNSets: Int = HardwareConfig.defaults.dcacheNSets,
    @arg(name = "dcache-nways", doc = "L1D way count reused by LSU metadata") dcacheNWays: Int = HardwareConfig.defaults.dcacheNWays,
    @arg(name = "dcache-block-words", doc = "shared-memory line decomposition reuses this lower bound") dcacheBlockWords: Int = HardwareConfig.defaults.dcacheBlockWords,
    @arg(name = "dcache-mshr-entry", doc = "L1D MSHR entries reused by LSU metadata") dcacheMshrEntry: Int = HardwareConfig.defaults.dcacheMshrEntry,
    @arg(name = "dcache-mshr-sub-entry", doc = "L1D MSHR sub entries reused by LSU metadata") dcacheMshrSubEntry: Int = HardwareConfig.defaults.dcacheMshrSubEntry,
    @arg(name = "dcache-wshr-entry", doc = "L1D writeback queue entries reused by LSU metadata") dcacheWshrEntry: Int = HardwareConfig.defaults.dcacheWshrEntry,
    @arg(name = "sharedmem-nbanks", doc = "shared memory bank count; defaults to num-thread when omitted") sharedmemNBanks: Int = -1,
    @arg(name = "sharedmem-capacity-bytes", doc = "shared memory capacity in bytes") sharedmemCapacityBytes: Int = HardwareConfig.defaults.sharedmemCapacityBytes,
    @arg(name = "lsu-num-entry-each-warp", doc = "LSU entry count used by LSU-side req/rsp bundles") lsuNumEntryEachWarp: Int = HardwareConfig.defaults.lsuNumEntryEachWarp,
    @arg(name = "lsu-sharedmem-pipe-cut", doc = "insert one pipeline stage between AddrCalc output and SharedMemory input") lsuSharedmemPipeCut: Boolean = false
  ): Unit = {
    val resolvedSharedmemNBanks = if (sharedmemNBanks > 0) sharedmemNBanks else numThread
    val resolvedTargetDir = resolveTargetDir(
      targetDir,
      outputRoot,
      dirPrefix,
      numWarp,
      numThread,
      sharedmemCapacityBytes,
      resolvedSharedmemNBanks,
      lsuSharedmemPipeCut
    )

    HardwareConfig.configure(
      HardwareConfig.defaults.copy(
        numSm = 1,
        numWarp = numWarp,
        numThread = numThread,
        numBlock = numBlock,
        dcacheNSets = dcacheNSets,
        dcacheNWays = dcacheNWays,
        dcacheBlockWords = dcacheBlockWords,
        dcacheMshrEntry = dcacheMshrEntry,
        dcacheMshrSubEntry = dcacheMshrSubEntry,
        dcacheWshrEntry = dcacheWshrEntry,
        sharedmemNBanks = resolvedSharedmemNBanks,
        sharedmemCapacityBytes = sharedmemCapacityBytes,
        lsuNumEntryEachWarp = lsuNumEntryEachWarp
      )
    )

    val param = (new MyConfig).toInstance
    (new ChiselStage).execute(
      Array("--target", "chirrtl", "--target-dir", resolvedTargetDir),
      Seq(ChiselGeneratorAnnotation(() => new LsuSharedMemTop(lsuSharedmemPipeCut)(param)))
    )

    ParametersToJson.saveToJson(s"$resolvedTargetDir/parameters.json")
    println(s"lsu-sharedmem output dir: $resolvedTargetDir")
  }

  def main(args: Array[String]): Unit = ParserForMethods(this).runOrExit(args.toIndexedSeq)
}
