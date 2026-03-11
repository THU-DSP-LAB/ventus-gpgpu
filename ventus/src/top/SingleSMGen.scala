package top

import chisel3.stage.ChiselGeneratorAnnotation
import circt.stage.ChiselStage
import mainargs.{ParserForMethods, arg, main}

object SingleSMGen {
  private val TopModuleName = "SM"

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
    @arg(name = "lsu-num-entry-each-warp", doc = "LSU queue depth per warp") lsuNumEntryEachWarp: Int = HardwareConfig.defaults.lsuNumEntryEachWarp
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
      Seq(ChiselGeneratorAnnotation(() => new SM()))
    )

    ParametersToJson.saveToJson(s"$resolvedTargetDir/parameters.json")
    println(s"single-sm output dir: $resolvedTargetDir")
  }

  def main(args: Array[String]): Unit = ParserForMethods(this).runOrExit(args.toIndexedSeq)
}
