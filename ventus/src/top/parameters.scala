package top

import L2cache.{CacheParameters, InclusiveCacheMicroParameters, InclusiveCacheParameters_lite}
import chisel3.util._
import mmu.SV32.{asidLen, paLen, vaLen}
// TODO: MOVE parameters to `ventus/top'

object parameters { //notice log2Ceil(4) returns 2.that is ,n is the total num, not the last idx.
  def num_sm = 2
  var num_warp = 8
  var num_thread = 32
  val SINGLE_INST: Boolean = false
  // Keep the historical trace-on default while allowing long RTL workloads
  // to elaborate without per-instruction trace state and printf traffic.
  val SPIKE_OUTPUT: Boolean =
    sys.env.getOrElse("RTL_SPIKE_OUTPUT", "true").toBoolean
  val INST_CNT: Boolean = true
  val INST_CNT_2: Boolean = false
  val PMU_PIPELINE: Boolean = true
  val PMU_INST_CLASS: Boolean = true
  val GVM_ENABLED: Boolean = sys.env.getOrElse("RTL_GVM_ENABLED", "false").toBoolean
  val MMU_ENABLED: Boolean = false
  val DCACHE_DEBUG: Boolean = false
  def MMU_ASID_WIDTH = mmu.SV32.asidLen
  val wid_to_check = 2
  def num_bank = 4                  // # of banks for register file
  def num_collectorUnit = num_warp
  def num_vgpr:Int = 128*num_warp
  def num_sgpr:Int = 256*num_warp
  def depth_regBank = log2Ceil(num_vgpr/num_bank)
  def regidx_width = 5

  def regext_width = 3

  def num_cluster = 1

  def num_sm_in_cluster = num_sm / num_cluster
  def depth_warp = if (num_warp == 1) 1 else log2Ceil(num_warp)

  // for register file bank access only, in operand collector
  // Calculate the highest slice index, ensuring it does not exceed the actual width of 'wid'
  def widSliceHigh = scala.math.min(log2Ceil(num_bank) - 1, depth_warp - 1)

  def depth_thread = log2Ceil(num_thread)

  def num_fetch = 2
  Predef.assert((num_fetch & (num_fetch - 1)) == 0, "num_fetch should be power of 2")

  def icache_align = num_fetch * 4

  def num_issue = 1

  def size_ibuffer = 2

  def xLen = 32 // data length 32-bit

  def instLen = 32

  def addrLen = 32

  def memReqLen = if(MMU_ENABLED) paLen else vaLen

  def num_block = 8// not bigger than num_warp

  def num_warp_in_a_block = num_warp

  def num_lane = num_thread // 2

  def num_icachebuf = 1 //blocking for each warp

  def depth_icachebuf = log2Ceil(num_icachebuf)

  def num_ibuffer = 2

  def depth_ibuffer = log2Ceil(num_ibuffer)

  def lsu_num_entry_each_warp = 4 //blocking for each warp

  def lsu_nMshrEntry = num_warp // less than num_warp

  def dcache_NSets: Int = 32

  def dcache_NWays: Int = 2

  def dcache_BlockWords: Int = 32  // number of words per cacheline(block)
  // WSHR tracks outstanding dirty writeback transactions awaiting AccessAck
  // from L2. Too few entries cause a three-way livelock between L1
  // WSHR / memReq_Q / memRspPipe.st1 under burst dirty evictions
  // (slots saturate -> pushReq.ready=0 -> wshrPass=0 -> memReq_Q.deq.ready=0
  // -> memRspPipe stuck on memReq state -> no WSHRPopReq -> WSHR cannot drain).
  // 100% reproducible at depth=4 on bfs_4096 baseline; partial relief at
  // depth=8 (peak hits 8/8 still triggers livelock after ~4.26 ms sim).
  // See bugs/bfs4096-003/phase_4_report.md. Monitor `wshrMaxUsed` debug
  // counter (DCacheWSHR.scala) to validate the budget under new workloads.
  // CONSTRAINT: dcache_wshr_entry <= dcache_MshrEntry (Chisel assert in
  // DCache.scala) -- WSHR pushedIdx is encoded into a_source sharing the
  // MSHR-width source field.
  def dcache_wshr_entry: Int = 16

  def dcache_SetIdxBits: Int = log2Ceil(dcache_NSets)

  def BytesOfWord = 32 / 8

  def dcache_WordOffsetBits = log2Ceil(BytesOfWord) //a Word has 4 Bytes

  def dcache_BlockOffsetBits = log2Ceil(dcache_BlockWords) // select word in block

  def dcache_TagBits = xLen - (dcache_SetIdxBits + dcache_BlockOffsetBits + dcache_WordOffsetBits)

  // MSHR (Miss Status Holding Register) tracks outstanding read misses.
  // Coupling constraint (after the V2 decoupling in this patch):
  //   dcache_MshrEntry >= dcache_wshr_entry (Chisel assert in
  //   DCache.scala:1037) -- a_source field width = 3 + log2Up(dcache_MshrEntry)
  //   + log2Up(dcache_NSets), and the WSHR pushedIdx is encoded into the
  //   same field at the MSHR-width slot (DCachev2.scala:571:
  //   Cat("d0", WshrAccess.io.pushedIdx, setIdx)).
  //
  // Historical note: the MSHRmiss{Req,RspOut}.instrId field's declared width
  // was `WIdBits = log2Up(num_warp)`, which used to alias MSHR idx with warp
  // id (the V1 cache assumed 1 outstanding miss per warp). After the V2
  // refactor the field carries the MSHR entry index, not a warp id, but the
  // stale width pinned NMshrEntry <= num_warp. This patch passes
  // `math.max(WIdBits, log2Up(NMshrEntry))` to MSHR/SpecialMSHR/MSHRmissRspOut
  // at instantiation sites (DCachev2 / MemRspPipe / CoreReqPipe) so that
  // NMshrEntry can grow past num_warp. Commit 2 renames the misnamed
  // parameter to `InstrIdBits` for clarity.
  //
  // WARNING: setting dcache_MshrEntry=4 forces dcache_wshr_entry<=4, which
  // re-triggers the WSHR-full livelock documented in
  // bugs/bfs4096-003/phase_4_report.md. Keep both at 16 unless wshrMaxUsed
  // counter shows you have headroom to shrink.
  // Monitor `mshrMaxUsed` debug counter (L1MSHR.scala) to validate.
  def dcache_MshrEntry: Int = 16

  def dcache_MshrSubEntry: Int = 2
  def dcache_MshrSet: Int = 2
  def dcache_MshrWay: Int = 4

  def atuns_NInfWriteEntry: Int = 16
  def atuns_NResTabEntry: Int = 16

  def num_sfu = (num_thread >> 2).max(1)

  def sharedmem_depth = 1024

  def sharedmem_BlockWords = dcache_BlockWords

  def sharemem_size = sharedmem_depth * sharedmem_BlockWords * 4 //bytes

  def l2cache_NSets: Int = 32

  def l2cache_NWays: Int = 16

  def l2cache_BlockWords: Int = dcache_BlockWords

  def l2cache_writeBytes: Int = 1

  def l2cache_memCycles: Int = 32

  def l2cache_portFactor: Int = 2

  def l1cache_sourceBits: Int = 3+log2Up(dcache_MshrEntry)+log2Up(dcache_NSets)

  def l2cache_cache = CacheParameters(2, l2cache_NWays, l2cache_NSets, num_l2cache, l2cache_BlockWords << 2, l2cache_BlockWords << 2)
  def l2cache_micro = InclusiveCacheMicroParameters(l2cache_writeBytes, l2cache_memCycles, l2cache_portFactor, num_warp, num_sm, num_sm_in_cluster, num_cluster,dcache_MshrEntry,dcache_NSets,atuns_NInfWriteEntry)
  def l2cache_micro_l = InclusiveCacheMicroParameters(l2cache_writeBytes, l2cache_memCycles, l2cache_portFactor, num_warp, num_sm, num_sm_in_cluster, 1,dcache_MshrEntry,dcache_NSets,atuns_NInfWriteEntry)
  def l2cache_params = InclusiveCacheParameters_lite(l2cache_cache, l2cache_micro, false, MMU_ENABLED)
  def l2cache_params_l = InclusiveCacheParameters_lite(l2cache_cache, l2cache_micro_l, false, MMU_ENABLED)

  def tc_dim: Seq[Int] = {
    var x: Seq[Int] = Seq(2, 2, 2)
    if (num_thread == 8)
      x = Seq(2, 4, 2)
    else if (num_thread == 32)
      x = Seq(4, 8, 4)
    x
  }

  def sig_length = 33

  def num_cache_in_sm = 2

  def num_l2cache = 1

  def l1tlb_ways = 8

  val LDS_BASE = 0x70000000  // LDS base address: a hyperparameter used within each SM

  def NUMBER_CU = num_sm
  def NUMBER_VGPR_SLOTS = num_vgpr
  def NUMBER_SGPR_SLOTS = num_sgpr
  def NUMBER_LDS_SLOTS = sharemem_size //TODO:check LDS max value. 128kB -> 2^17
  def WG_ID_WIDTH = 32
  def WF_COUNT_MAX = num_warp // max num of wf in a cu
  def WF_COUNT_PER_WG_MAX = num_warp_in_a_block // max num of wf in a wg
  def WAVE_ITEM_WIDTH = 10
  def MEM_ADDR_WIDTH = 32
  def CU_ID_WIDTH = log2Ceil(NUMBER_CU)
  def VGPR_ID_WIDTH = log2Ceil(NUMBER_VGPR_SLOTS)
  def SGPR_ID_WIDTH = log2Ceil(NUMBER_SGPR_SLOTS)
  def LDS_ID_WIDTH = log2Ceil(NUMBER_LDS_SLOTS)
  def WF_COUNT_WIDTH = log2Ceil(WF_COUNT_MAX + 1)
  def WF_COUNT_WIDTH_PER_WG = log2Ceil(WF_COUNT_PER_WG_MAX + 1)
  def TAG_WIDTH = CTA_SCHE_CONFIG.WG.WF_TAG_WIDTH_UINT
  def NUM_WG = (1 << 16) - 1   // max threadBlock num in each kernel(grid) (each dim)
  def NUM_WG_X = NUM_WG   // max threadBlock num in each kernel(grid) (x dim)
  def NUM_WG_Y = NUM_WG   // max threadBlock num in each kernel(grid) (y dim)
  def NUM_WG_Z = NUM_WG   // max threadBlock num in each kernel(grid) (z dim)
  def NUM_THREAD_PER_WG     = 1024             // max thread num in each threadBlock
  def NUM_THREAD_PER_KNL = NUM_WG * NUM_THREAD_PER_WG
  def WG_SIZE_X_WIDTH = log2Ceil(NUM_THREAD_PER_WG + 1)
  def WG_SIZE_Y_WIDTH = log2Ceil(NUM_THREAD_PER_WG + 1)
  def WG_SIZE_Z_WIDTH = log2Ceil(NUM_THREAD_PER_WG + 1)

  def KNL_ASID_WIDTH = MMU_ASID_WIDTH

  object CTA_SCHE_CONFIG {
    import chisel3._
    object GPU {
      val NUM_CU = num_sm
      val MEM_ADDR_WIDTH = parameters.MEM_ADDR_WIDTH.W
      val NUM_WG_SLOT = num_block                  // Number of WG slot in each CU
      val NUM_WF_SLOT = num_warp                   // Number of WF slot in each CU
      val NUM_THREAD = num_thread                  // Number of thread in each WF
      val MMU_ENABLE = MMU_ENABLED                 // if MMU will be used
      val ASID_WIDTH = MMU_ASID_WIDTH.W            // MMU ASID width
      val NUM_SGPR = parameters.num_sgpr
      val NUM_VGPR = parameters.num_vgpr
      val NUM_LDS = sharemem_size
    }
    object KERNEL {
      val NUM_WG_MAX = NUM_WG                      // Max number of wg in each kernel
      val NUM_THREAD_PER_KNL_MAX = NUM_THREAD_PER_KNL
    }
    object WG {
      val WG_ID_WIDTH = parameters.WG_ID_WIDTH.W
      val NUM_THREAD_PER_WG_MAX   = NUM_THREAD_PER_WG   // Max number of thread in each **WG**
      val NUM_WF_MAX = num_warp_in_a_block         // Max number of wavefront in each workgroup(block)
      val NUM_LDS_MAX = sharemem_size              // Max number of LDS  occupied by a workgroup
      val NUM_SGPR_MAX = 256                       // Max number of sgpr occupied by a workgroup
      val NUM_VGPR_MAX = 256                       // Max number of vgpr occupied by a workgroup
      val NUM_PDS_MAX = 4096*num_thread            // Max number of PDS  occupied by a *wavefront*
      //val NUM_GDS_MAX = 1024                     // Max number of GDS  occupied by a workgroup, useless

      // WF tag = cat(wg_slot_id_in_cu, wf_id_in_wg)
      val WF_TAG_WIDTH_UINT = log2Ceil(GPU.NUM_WG_SLOT) + log2Ceil(NUM_WF_MAX)
      val WF_TAG_WIDTH = WF_TAG_WIDTH_UINT.W
    }
    object WG_BUFFER {
      val NUM_ENTRIES = 8
    }
    object RESOURCE_TABLE {
      val NUM_RESULT = 2
    }
    val DEBUG = true
  }

}

//
// save parameters to json file
//

import io.circe._
import io.circe.syntax._
import scala.reflect.runtime.universe._
import scala.reflect.runtime.{universe => ru}
import java.io.{File, PrintWriter}
import scala.collection.immutable.TreeMap

object ParametersToJson {
  
  /**
   * 提取parameters对象的所有字段和无参函数值，并按名称排序
   */
  def extractAllParameters(): TreeMap[String, Json] = {
    val mirror = ru.runtimeMirror(getClass.getClassLoader)
    val instanceMirror = mirror.reflect(parameters)
    val typeSignature = instanceMirror.symbol.typeSignature
    val members = typeSignature.members
    
    // 使用TreeMap自动按键排序
    var result = TreeMap[String, Json]()
    
    members.foreach { member =>
      val memberName = member.name.toString.trim
      
      // 过滤系统方法和特殊成员
      if (isValidMember(memberName)) {
        try {
          val value = extractMemberValue(member, instanceMirror)
          value.foreach { v =>
            result = result + (memberName -> convertToJson(v))
          }
        } catch {
          case _: Exception => // 忽略无法访问的成员
        }
      }
    }
    
    result
  }
  
  /**
   * 判断是否为有效的成员名称
   */
  private def isValidMember(name: String): Boolean = {
    !name.startsWith("<") && 
    !name.contains("$") && 
    !Set("toString", "hashCode", "equals", "getClass", 
         "wait", "notify", "notifyAll", "asInstanceOf", 
         "isInstanceOf", "synchronized", "clone", "finalize").contains(name)
  }
  
  /**
   * 提取成员的值
   */
  private def extractMemberValue(member: Symbol, instanceMirror: InstanceMirror): Option[Any] = {
    if (member.isModule) {
      None // 跳过嵌套对象
    } else if (member.isMethod) {
      val methodSymbol = member.asMethod
      // 只处理无参方法（非getter）
      if ((methodSymbol.paramLists.isEmpty || methodSymbol.paramLists.forall(_.isEmpty)) && 
          !methodSymbol.isGetter) {
        val methodMirror = instanceMirror.reflectMethod(methodSymbol)
        Some(methodMirror.apply())
      } else None
    } else if (member.isTerm) {
      val termSymbol = member.asTerm
      if (termSymbol.isVal || termSymbol.isVar) {
        val fieldMirror = instanceMirror.reflectField(termSymbol)
        Some(fieldMirror.get)
      } else None
    } else None
  }
  
  /**
   * 将任意值转换为Circe的Json类型
   */
  private def convertToJson(value: Any): Json = value match {
    case null => Json.Null
    case b: Boolean => Json.fromBoolean(b)
    case i: Int => Json.fromInt(i)
    case l: Long => Json.fromLong(l)
    case f: Float => Json.fromFloatOrNull(f)
    case d: Double => Json.fromDoubleOrNull(d)
    case s: String => Json.fromString(s)
    case bi: BigInt => Json.fromString(bi.toString)
    case bd: BigDecimal => Json.fromBigDecimal(bd)
    case seq: Seq[_] => Json.fromValues(seq.map(convertToJson))
    case arr: Array[_] => Json.fromValues(arr.map(convertToJson))
    case map: Map[_, _] => 
      val sortedMap = TreeMap[String, Json]() ++ map.map { case (k, v) => 
        k.toString -> convertToJson(v)
      }
      Json.obj(sortedMap.toSeq: _*)
    case opt: Option[_] => opt.map(convertToJson).getOrElse(Json.Null)
    case _ => Json.fromString(value.toString)
  }
  
  /**
   * 保存所有参数到JSON文件（自动提取并排序）
   * @param filename 输出文件名
   * @param pretty 是否格式化输出
   */
  def saveToJson(filename: String = "parameters.json", pretty: Boolean = true): Unit = {
    val parameters = extractAllParameters()
    val json = Json.obj(parameters.toSeq: _*) // convert to json

    val jsonString = if (pretty) {
      json.spaces2  // 使用2空格缩进的格式化输出
    } else {
      json.noSpaces // 紧凑输出
    }

    // save to json file
    val writer = new PrintWriter(new File(filename))
    try {
      writer.write(jsonString)
      writer.flush()
    } finally {
      writer.close()
    }
  }
  
}

object ParamPrintApp extends App {
  import ParametersToJson._
  saveToJson("parameters.json", pretty = true)
}
