/***************************************************************************************
  * Copyright (c) 2020-2021 Institute of Computing Technology, Chinese Academy of Sciences
  * Copyright (c) 2020-2021 Peng Cheng Laboratory
  *
  * XiangShan is licensed under Mulan PSL v2.
  * You can use this software according to the terms and conditions of the Mulan PSL v2.
  * You may obtain a copy of Mulan PSL v2 at:
  *          http://license.coscl.org.cn/MulanPSL2
  *
  * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND,
  * EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT,
  * MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
  *
  * See the Mulan PSL v2 for more details.
  ***************************************************************************************/

package pipeline
import L2cache._
import parameters._
import chisel3._
import chisel3.util._

trait HasVTParameter{
  val XLEN=xLen
  val vlen=vLen
  val PAddrBits=paddr
  val VAddrBits=vaddr
  val asidLen=num_thread
  val L2param =InclusiveCacheParameters_lite(CacheParameters(2,l2cache_NSets,l2cache_NWays,blockBytes=(l2cache_BlockWords<<2),beatBytes=(l2cache_BlockWords<<2)),InclusiveCacheMicroParameters(l2cache_writeBytes,l2cache_memCycles,l2cache_portFactor,num_warp,num_sm),false)

}
case class TLBParameters
(
  name: String = "none",
  fetchi: Boolean = false, // TODO: remove it
  fenceDelay: Int = 2,
  useDmode: Boolean = true,
  normalNSets: Int = 1, // when da or sa
  normalNWays: Int = 8, // when fa or sa
  superNSets: Int = 1,
  superNWays: Int = 2,
  normalReplacer: Option[String] = Some("random"),
  superReplacer: Option[String] = Some("plru"),
  normalAssociative: String = "fa", // "fa", "sa", "da", "sa" is not supported
  superAssociative: String = "fa", // must be fa
  normalAsVictim: Boolean = false, // when get replace from fa, store it into sram
  outReplace: Boolean = false,
  partialStaticPMP: Boolean = false, // partial static pmp result stored in entries
  outsideRecvFlush: Boolean = false, // if outside moudle waiting for tlb recv flush pipe
  saveLevel: Boolean = false
)

case class L2TLBParameters
(
  name: String = "l2tlb",
  // l1
  l1Size: Int = 16,
  l1Associative: String = "fa",
  l1Replacer: Option[String] = Some("plru"),
  // l2
  l2nSets: Int = 32,
  l2nWays: Int = 2,
  l2Replacer: Option[String] = Some("setplru"),
  // l3
  l3nSets: Int = 128,
  l3nWays: Int = 4,
  l3Replacer: Option[String] = Some("setplru"),
  // sp
  spSize: Int = 16,
  spReplacer: Option[String] = Some("plru"),
  // filter
  ifilterSize: Int = 4,
  dfilterSize: Int = 8,
  // miss queue, add more entries than 'must require'
  // 0 for easier bug trigger, please set as big as u can, 8 maybe
  missqueueExtendSize: Int = 0,
  // llptw
  llptwsize: Int = 6,
  // way size
  blockBytes: Int = 64,
  // prefetch
  enablePrefetch: Boolean = true,
  // ecc
  ecc: Option[String] = Some("secded")
)

trait HasTlbConst extends HasVTParameter {
  val Level = 3

  val offLen  = 12
  val ppnLen  = PAddrBits - offLen
  val vpnnLen = 9
  val vpnLen  = VAddrBits - offLen
  val flagLen = 8
  val pteResLen = XLEN - ppnLen - 2 - flagLen

  val sramSinglePort = true

  val timeOutThreshold = 5000

  def get_pn(addr: UInt) = {
    require(addr.getWidth > offLen)
    addr(addr.getWidth-1, offLen)
  }
  def get_off(addr: UInt) = {
    require(addr.getWidth > offLen)
    addr(offLen-1, 0)
  }

  def get_set_idx(vpn: UInt, nSets: Int): UInt = {
    require(nSets >= 1)
    vpn(log2Up(nSets)-1, 0)
  }

  def drop_set_idx(vpn: UInt, nSets: Int): UInt = {
    require(nSets >= 1)
    require(vpn.getWidth > log2Ceil(nSets))
    vpn(vpn.getWidth-1, log2Ceil(nSets))
  }

  def drop_set_equal(vpn1: UInt, vpn2: UInt, nSets: Int): Bool = {
    require(nSets >= 1)
    require(vpn1.getWidth == vpn2.getWidth)
    if (vpn1.getWidth <= log2Ceil(nSets)) {
      true.B
    } else {
      drop_set_idx(vpn1, nSets) === drop_set_idx(vpn2, nSets)
    }
  }

  def replaceWrapper(v: UInt, lruIdx: UInt): UInt = {
    val width = v.getWidth
    val emptyIdx = ParallelPriorityMux((0 until width).map( i => (!v(i), i.U(log2Up(width).W))))
    val full = Cat(v).andR
    Mux(full, lruIdx, emptyIdx)
  }

  def replaceWrapper(v: Seq[Bool], lruIdx: UInt): UInt = {
    replaceWrapper(VecInit(v).asUInt, lruIdx)
  }

  implicit def ptwresp_to_tlbperm(ptwResp: PtwResp): TlbPermBundle = {
    val tp = Wire(new TlbPermBundle)
    val ptePerm = ptwResp.entry.perm.get.asTypeOf(new PtePermBundle().cloneType)
    tp.pf := ptwResp.pf
    tp.af := ptwResp.af
    tp.d := ptePerm.d
    tp.a := ptePerm.a
    tp.g := ptePerm.g
    tp.u := ptePerm.u
    tp.x := ptePerm.x
    tp.w := ptePerm.w
    tp.r := ptePerm.r
    tp.pm := DontCare
    tp
  }
}

trait MemoryOpConstants {
  val NUM_XA_OPS = 9
  val M_SZ      = 5
  def M_X       = BitPat("b?????")
  def M_XRD     = "b00000".U // int load
  def M_XWR     = "b00001".U // int store
  def M_PFR     = "b00010".U // prefetch with intent to read
  def M_PFW     = "b00011".U // prefetch with intent to write
  def M_XA_SWAP = "b00100".U
  def M_FLUSH_ALL = "b00101".U  // flush all lines
  def M_XLR     = "b00110".U
  def M_XSC     = "b00111".U
  def M_XA_ADD  = "b01000".U
  def M_XA_XOR  = "b01001".U
  def M_XA_OR   = "b01010".U
  def M_XA_AND  = "b01011".U
  def M_XA_MIN  = "b01100".U
  def M_XA_MAX  = "b01101".U
  def M_XA_MINU = "b01110".U
  def M_XA_MAXU = "b01111".U
  def M_FLUSH   = "b10000".U // write back dirty data and cede R/W permissions
  def M_PWR     = "b10001".U // partial (masked.U store
  def M_PRODUCE = "b10010".U // write back dirty data and cede W permissions
  def M_CLEAN   = "b10011".U // write back dirty data and retain R/W permissions
  def M_SFENCE  = "b10100".U // flush TLB
  def M_WOK     = "b10111".U // check write permissions but don't perform a write

  def isAMOLogical(cmd: UInt) = cmd === M_XA_SWAP || cmd === M_XA_XOR || cmd === M_XA_OR || cmd === M_XA_AND
  def isAMOArithmetic(cmd: UInt) = cmd === M_XA_ADD || cmd === M_XA_MIN || cmd === M_XA_MAX || cmd === M_XA_MINU || cmd === M_XA_MAXU
  def isAMO(cmd: UInt) = isAMOLogical(cmd) || isAMOArithmetic(cmd)
  def isPrefetch(cmd: UInt) = cmd === M_PFR || cmd === M_PFW
  def isRead(cmd: UInt) = cmd === M_XRD || cmd === M_XLR || cmd === M_XSC || isAMO(cmd)
  def isWrite(cmd: UInt) = cmd === M_XWR || cmd === M_PWR || cmd === M_XSC || isAMO(cmd)
  def isWriteIntent(cmd: UInt) = isWrite(cmd) || cmd === M_PFW || cmd === M_XLR
}

object MemoryOpConstants extends MemoryOpConstants {
  def getMemoryOpName(cmd: UInt): String = {
    val opNames = Map(
      M_XRD -> "M_XRD",
      M_XWR -> "M_XWR",
      M_PFR -> "M_PFR",
      M_PFW -> "M_PFW",
      M_XA_SWAP -> "M_XA_SWAP",
      M_FLUSH_ALL -> "M_FLUSH_ALL",
      M_XLR -> "M_XLR",
      M_XSC -> "M_XSC",
      M_XA_ADD -> "M_XA_ADD",
      M_XA_XOR -> "M_XA_XOR",
      M_XA_OR -> "M_XA_OR",
      M_XA_AND -> "M_XA_AND",
      M_XA_MIN -> "M_XA_MIN",
      M_XA_MAX -> "M_XA_MAX",
      M_XA_MINU -> "M_XA_MINU",
      M_XA_MAXU -> "M_XA_MAXU",
      M_FLUSH -> "M_FLUSH",
      M_PWR -> "M_PWR",
      M_PRODUCE -> "M_PRODUCE",
      M_CLEAN -> "M_CLEAN",
      M_SFENCE -> "M_SFENCE",
      M_WOK -> "M_WOK"
    )
    val opLitNames = opNames map {case (k, v) => (k.litValue.longValue, v)}
    return opLitNames(cmd.litValue.longValue)
  }
}

trait HasPtwConst extends HasTlbConst with MemoryOpConstants{
  val PtwWidth = num_warp
  val l2tlbParams = new L2TLBParameters
  val sourceWidth = { if (l2tlbParams.enablePrefetch) PtwWidth + 1 else PtwWidth}
  val prefetchID = PtwWidth

  val blockBits = l2tlbParams.blockBytes * 8

  val bPtwWidth = log2Up(PtwWidth)
  val bSourceWidth = log2Up(sourceWidth)
  // ptwl1: fully-associated
  val PtwL1TagLen = vpnnLen

  /* +-------+----------+-------------+
   * |  Tag  |  SetIdx  |  SectorIdx  |
   * +-------+----------+-------------+
   */
  // ptwl2: 8-way group-associated
  val l2tlbParams.l2nWays = l2tlbParams.l2nWays
  val PtwL2SetNum = l2tlbParams.l2nSets
  val PtwL2SectorSize = blockBits /XLEN
  val PtwL2IdxLen = log2Up(PtwL2SetNum * PtwL2SectorSize)
  val PtwL2SectorIdxLen = log2Up(PtwL2SectorSize)
  val PtwL2SetIdxLen = log2Up(PtwL2SetNum)
  val PtwL2TagLen = vpnnLen * 2 - PtwL2IdxLen

  // ptwl3: 16-way group-associated
  val l2tlbParams.l3nWays = l2tlbParams.l3nWays
  val PtwL3SetNum = l2tlbParams.l3nSets
  val PtwL3SectorSize =  blockBits / XLEN
  val PtwL3IdxLen = log2Up(PtwL3SetNum * PtwL3SectorSize)
  val PtwL3SectorIdxLen = log2Up(PtwL3SectorSize)
  val PtwL3SetIdxLen = log2Up(PtwL3SetNum)
  val PtwL3TagLen = vpnnLen * 3 - PtwL3IdxLen

  // super page, including 1GB and 2MB page
  val SPTagLen = vpnnLen * 2

  // miss queue
  val MissQueueSize = l2tlbParams.ifilterSize + l2tlbParams.dfilterSize
  val MemReqWidth = l2tlbParams.llptwsize + 1
  val FsmReqID = l2tlbParams.llptwsize
  val bMemID = log2Up(MemReqWidth)

  def genPtwL2Idx(vpn: UInt) = {
    (vpn(vpnLen - 1, vpnnLen))(PtwL2IdxLen - 1, 0)
  }

  def genPtwL2SectorIdx(vpn: UInt) = {
    genPtwL2Idx(vpn)(PtwL2SectorIdxLen - 1, 0)
  }

  def genPtwL2SetIdx(vpn: UInt) = {
    genPtwL2Idx(vpn)(PtwL2SetIdxLen + PtwL2SectorIdxLen - 1, PtwL2SectorIdxLen)
  }

  def genPtwL3Idx(vpn: UInt) = {
    vpn(PtwL3IdxLen - 1, 0)
  }

  def genPtwL3SectorIdx(vpn: UInt) = {
    genPtwL3Idx(vpn)(PtwL3SectorIdxLen - 1, 0)
  }

  def dropL3SectorBits(vpn: UInt) = {
    vpn(vpn.getWidth-1, PtwL3SectorIdxLen)
  }

  def genPtwL3SetIdx(vpn: UInt) = {
    genPtwL3Idx(vpn)(PtwL3SetIdxLen + PtwL3SectorIdxLen - 1, PtwL3SectorIdxLen)
  }

  def MakeAddr(ppn: UInt, off: UInt) = {
    require(off.getWidth == 9)
    Cat(ppn, off, 0.U(log2Up(XLEN/8).W))(PAddrBits-1, 0)
  }

  def getVpnn(vpn: UInt, idx: Int): UInt = {
    vpn(vpnnLen*(idx+1)-1, vpnnLen*idx)
  }

  def getVpnClip(vpn: UInt, level: Int) = {
    // level 0  /* vpnn2 */
    // level 1  /* vpnn2 * vpnn1 */
    // level 2  /* vpnn2 * vpnn1 * vpnn0*/
    vpn(vpnLen - 1, (2 - level) * vpnnLen)
  }

  def get_next_line(vpn: UInt) = {
    Cat(dropL3SectorBits(vpn) + 1.U, 0.U(PtwL3SectorIdxLen.W))
  }

  def same_l2entry(vpn1: UInt, vpn2: UInt) = {
    vpn1(vpnLen-1, vpnnLen) === vpn2(vpnLen-1, vpnnLen)
  }

  def from_pre(source: UInt) = {
    (source === prefetchID.U)
  }

  def sel_data(data: UInt, index: UInt): UInt = {
    val inner_data = data.asTypeOf(Vec(data.getWidth / XLEN, UInt(XLEN.W)))
    inner_data(index)
  }

  // vpn1 and vpn2 is at same cacheline
  def dup(vpn1: UInt, vpn2: UInt): Bool = {
    dropL3SectorBits(vpn1) === dropL3SectorBits(vpn2)
  }


  def printVec[T <: Data](x: Seq[T]): Printable = {
    (0 until x.length).map(i => p"(${i.U})${x(i)} ").reduce(_+_)
  }
}
