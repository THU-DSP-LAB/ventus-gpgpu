package top

import chisel3.stage.ChiselGeneratorAnnotation
import circt.stage.ChiselStage
import mainargs.{ParserForMethods, arg, main}

object SingleSMGen {
  private val TopModuleName = "SM"

  private def pipeTags(
    lsuSharedmemPipeCut: Boolean,
    lsuDcachePipeCut: Boolean,
    lsuAddrCalcPipeCut: Boolean,
    operandCollectorRfPipeCut: Boolean,
    operandCrossbarPipeCut: Boolean,
    operandIssuePipeCut: Boolean,
    ibufferIssuePipeCut: Boolean,
    csrResultPipeCut: Boolean,
    csrIssuePipeCut: Boolean,
    simtPipeCut: Boolean,
    fpuInputPipeCut: Boolean,
    tensorCoreInputPipeCut: Boolean,
    tensorCoreWbPipeCut: Boolean,
    writebackOutPipeCut: Boolean
  ): String = {
    val sharedTag = if (lsuSharedmemPipeCut) "_pipe1" else ""
    val dcacheTag = if (lsuDcachePipeCut) "_dcachepipe" else ""
    val addrpipeTag = if (lsuAddrCalcPipeCut) "_addrpipe" else ""
    val ocpipeTag = if (operandCollectorRfPipeCut) "_ocpipe" else ""
    val ocxbarpipeTag = if (operandCrossbarPipeCut) "_ocxbarpipe" else ""
    val opissueTag = if (operandIssuePipeCut) "_opissuepipe" else ""
    val ibufpipeTag = if (ibufferIssuePipeCut) "_ibufpipe" else ""
    val csrpipeTag = if (csrResultPipeCut) "_csrpipe" else ""
    val csrissueTag = if (csrIssuePipeCut) "_csrissuepipe" else ""
    val simtpipeTag = if (simtPipeCut) "_simtpipe" else ""
    val fpuinpipeTag = if (fpuInputPipeCut) "_fpuinpipe" else ""
    val tcinpipeTag = if (tensorCoreInputPipeCut) "_tcinpipe" else ""
    val tcwbpipeTag = if (tensorCoreWbPipeCut) "_tcwbpipe" else ""
    val wboutpipeTag = if (writebackOutPipeCut) "_wboutpipe" else ""
    s"$sharedTag$dcacheTag$addrpipeTag$ocpipeTag$ocxbarpipeTag$opissueTag$ibufpipeTag$csrpipeTag$csrissueTag$simtpipeTag$fpuinpipeTag$tcinpipeTag$tcwbpipeTag$wboutpipeTag"
  }

  private def autoDirName(
    dirPrefix: String,
    numWarp: Int,
    numThread: Int,
    sharedmemCapacityBytes: Int,
    sharedmemNBanks: Int,
    lsuSharedmemPipeCut: Boolean,
    lsuDcachePipeCut: Boolean,
    lsuAddrCalcPipeCut: Boolean,
    operandCollectorRfPipeCut: Boolean,
    operandCrossbarPipeCut: Boolean,
    operandIssuePipeCut: Boolean,
    ibufferIssuePipeCut: Boolean,
    csrResultPipeCut: Boolean,
    csrIssuePipeCut: Boolean,
    simtPipeCut: Boolean,
    fpuInputPipeCut: Boolean,
    tensorCoreInputPipeCut: Boolean,
    tensorCoreWbPipeCut: Boolean,
    writebackOutPipeCut: Boolean
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
    val pipeTag = pipeTags(lsuSharedmemPipeCut, lsuDcachePipeCut, lsuAddrCalcPipeCut, operandCollectorRfPipeCut, operandCrossbarPipeCut, operandIssuePipeCut, ibufferIssuePipeCut, csrResultPipeCut, csrIssuePipeCut, simtPipeCut, fpuInputPipeCut, tensorCoreInputPipeCut, tensorCoreWbPipeCut, writebackOutPipeCut)
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
    lsuSharedmemPipeCut: Boolean,
    lsuDcachePipeCut: Boolean,
    lsuAddrCalcPipeCut: Boolean,
    operandCollectorRfPipeCut: Boolean,
    operandCrossbarPipeCut: Boolean,
    operandIssuePipeCut: Boolean,
    ibufferIssuePipeCut: Boolean,
    csrResultPipeCut: Boolean,
    csrIssuePipeCut: Boolean,
    simtPipeCut: Boolean,
    fpuInputPipeCut: Boolean,
    tensorCoreInputPipeCut: Boolean,
    tensorCoreWbPipeCut: Boolean,
    writebackOutPipeCut: Boolean
  ): String = {
    if (targetDir.nonEmpty) targetDir
    else s"$outputRoot/${autoDirName(dirPrefix, numWarp, numThread, sharedmemCapacityBytes, sharedmemNBanks, lsuSharedmemPipeCut, lsuDcachePipeCut, lsuAddrCalcPipeCut, operandCollectorRfPipeCut, operandCrossbarPipeCut, operandIssuePipeCut, ibufferIssuePipeCut, csrResultPipeCut, csrIssuePipeCut, simtPipeCut, fpuInputPipeCut, tensorCoreInputPipeCut, tensorCoreWbPipeCut, writebackOutPipeCut)}"
  }

  @main
  def elaborate(
    @arg(name = "target-dir", doc = "exact output directory, overrides auto naming") targetDir: String = "",
    @arg(name = "output-root", doc = "base directory for auto-named outputs") outputRoot: String = "gen_sm_verilog",
    @arg(name = "dir-prefix", doc = "extra prefix appended after the top-module name in the auto-named output folder") dirPrefix: String = "",
    @arg(name = "num-warp", doc = "warps in the single SM") numWarp: Int = HardwareConfig.defaults.numWarp,
    @arg(name = "num-thread", doc = "threads per warp") numThread: Int = HardwareConfig.defaults.numThread,
    @arg(name = "num-bank", doc = "register-file banks") numBank: Int = HardwareConfig.defaults.numBank,
    @arg(name = "num-fetch", doc = "fetch width in instructions, power of 2") numFetch: Int = HardwareConfig.defaults.numFetch,
    @arg(name = "size-ibuffer", doc = "instruction buffer depth") sizeIbuffer: Int = HardwareConfig.defaults.sizeIbuffer,
    @arg(name = "num-block", doc = "max resident blocks per SM") numBlock: Int = HardwareConfig.defaults.numBlock,
    @arg(name = "dcache-nsets", doc = "L1D set count") dcacheNSets: Int = HardwareConfig.defaults.dcacheNSets,
    @arg(name = "dcache-nways", doc = "L1D way count") dcacheNWays: Int = HardwareConfig.defaults.dcacheNWays,
    @arg(name = "dcache-block-words", doc = "L1D block words") dcacheBlockWords: Int = HardwareConfig.defaults.dcacheBlockWords,
    @arg(name = "dcache-mshr-entry", doc = "L1D MSHR entries") dcacheMshrEntry: Int = HardwareConfig.defaults.dcacheMshrEntry,
    @arg(name = "dcache-mshr-sub-entry", doc = "L1D MSHR sub entries") dcacheMshrSubEntry: Int = HardwareConfig.defaults.dcacheMshrSubEntry,
    @arg(name = "dcache-wshr-entry", doc = "L1D writeback queue entries") dcacheWshrEntry: Int = HardwareConfig.defaults.dcacheWshrEntry,
    @arg(name = "sharedmem-nbanks", doc = "shared memory bank count; defaults to num-thread when omitted") sharedmemNBanks: Int = -1,
    @arg(name = "sharedmem-capacity-bytes", doc = "shared memory capacity per SM in bytes") sharedmemCapacityBytes: Int = HardwareConfig.defaults.sharedmemCapacityBytes,
    @arg(name = "lsu-num-entry-each-warp", doc = "LSU queue depth per warp") lsuNumEntryEachWarp: Int = HardwareConfig.defaults.lsuNumEntryEachWarp,
    @arg(name = "lsu-sharedmem-pipe-cut", doc = "insert one pipeline stage between LSU shared request and SharedMemory") lsuSharedmemPipeCut: Boolean = false,
    @arg(name = "lsu-dcache-pipe-cut", doc = "insert one pipeline stage between LSU dcache request and DataCache") lsuDcachePipeCut: Boolean = false,
    @arg(name = "lsu-addr-calc-pipe-cut", doc = "insert one pipeline stage inside AddrCalculate between CSR read and address computation") lsuAddrCalcPipeCut: Boolean = false,
    @arg(name = "operand-rf-pipe-cut", doc = "insert one pipeline stage between operand-collector arbiters and regfile reads") operandCollectorRfPipeCut: Boolean = false,
    @arg(name = "operand-crossbar-pipe-cut", doc = "insert one pipeline stage between operand-collector crossbar outputs and collectorUnit bank inputs") operandCrossbarPipeCut: Boolean = false,
    @arg(name = "operand-issue-pipe-cut", doc = "insert one pipeline stage between operand-collector outputs and issue") operandIssuePipeCut: Boolean = false,
    @arg(name = "ibuffer-issue-pipe-cut", doc = "insert one pipeline stage between ibuffer outputs and issue/scoreboard consumers") ibufferIssuePipeCut: Boolean = false,
    @arg(name = "csr-result-pipe-cut", doc = "insert one pipeline stage between CSR decode/read and CSR writeback result queues") csrResultPipeCut: Boolean = false,
    @arg(name = "csr-issue-pipe-cut", doc = "insert one pipeline stage between Issue.out_CSR and CSRexe.io.in") csrIssuePipeCut: Boolean = false,
    @arg(name = "simt-pipe-cut", doc = "insert one aligned pipeline stage for SIMT branch control and reconvergence PC before branch_join") simtPipeCut: Boolean = false,
    @arg(name = "fpu-input-pipe-cut", doc = "insert one pipeline stage between Issue.out_vFPU and FPUexe.io.in") fpuInputPipeCut: Boolean = false,
    @arg(name = "tensorcore-input-pipe-cut", doc = "insert one pipeline stage between Issue.out_TC and vTCexe.io.in") tensorCoreInputPipeCut: Boolean = false,
    @arg(name = "tensorcore-wb-pipe-cut", doc = "insert one pipeline stage between tensorcore.io.out_v and writeback input") tensorCoreWbPipeCut: Boolean = false,
    @arg(name = "writeback-out-pipe-cut", doc = "insert one pipeline stage on writeback outputs before scoreboard and regfile-bypass consumers") writebackOutPipeCut: Boolean = false
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
      lsuSharedmemPipeCut,
      lsuDcachePipeCut,
      lsuAddrCalcPipeCut,
      operandCollectorRfPipeCut,
      operandCrossbarPipeCut,
      operandIssuePipeCut,
      ibufferIssuePipeCut,
      csrResultPipeCut,
      csrIssuePipeCut,
      simtPipeCut,
      fpuInputPipeCut,
      tensorCoreInputPipeCut,
      tensorCoreWbPipeCut,
      writebackOutPipeCut
    )

    HardwareConfig.configure(
      HardwareConfig.defaults.copy(
        numSm = 1,
        numWarp = numWarp,
        numThread = numThread,
        numBank = numBank,
        numFetch = numFetch,
        sizeIbuffer = sizeIbuffer,
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

    (new ChiselStage).execute(
      Array("--target", "chirrtl", "--target-dir", resolvedTargetDir),
      Seq(ChiselGeneratorAnnotation(() => new SM(
        lsuSharedmemPipeCut = lsuSharedmemPipeCut,
        lsuDcachePipeCut = lsuDcachePipeCut,
        lsuAddrCalcPipeCut = lsuAddrCalcPipeCut,
        operandCollectorRfPipeCut = operandCollectorRfPipeCut,
        operandCrossbarPipeCut = operandCrossbarPipeCut,
        operandIssuePipeCut = operandIssuePipeCut,
        ibufferIssuePipeCut = ibufferIssuePipeCut,
        csrResultPipeCut = csrResultPipeCut,
        csrIssuePipeCut = csrIssuePipeCut,
        simtPipeCut = simtPipeCut,
        fpuInputPipeCut = fpuInputPipeCut,
        tensorCoreInputPipeCut = tensorCoreInputPipeCut,
        tensorCoreWbPipeCut = tensorCoreWbPipeCut,
        writebackOutPipeCut = writebackOutPipeCut
      )))
    )

    ParametersToJson.saveToJson(s"$resolvedTargetDir/parameters.json")
    println(s"single-sm output dir: $resolvedTargetDir")
  }

  def main(args: Array[String]): Unit = ParserForMethods(this).runOrExit(args.toIndexedSeq)
}
