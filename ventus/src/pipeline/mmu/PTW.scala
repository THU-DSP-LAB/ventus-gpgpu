package pipeline.mmu
import L2cache._
import chisel3._
import chisel3.util._

/*
    |----++++----++++----++++----++++----++++----++++----++++----++++|
    |6------5555----44-------33-3-----3222-----22-1-----111---------0|
    |3------6543----87-------98-6-----0987-----10-8-----210---------0|
    |<-[38]------------------><- VPN2-><--VPN1-><--VPN0-><--offset-->| VA
    |xxxxxxxx<-------------------PPN2-><--PPN1-><--PPN0-><--offset-->| PA
    |xxxxxxxxxx<-------------------PPN2-><--PPN1-><--PPN0->xxDAGUXWRV| PTE
    valid value for PA:
    0000 0000 0000 0000 <-> 0000 003f ffff ffff 256GiB
    ffff ffc0 0000 0000 <-> ffff ffff ffff ffff 256GiB
*/

trait SVParam{
  def asidLen = 16
  def xLen = 32
  def vaLen = 32
  def paLen = 34
  def offsetLen = 12
  def ppnLen = paLen - offsetLen
  def idxLen = 10
  def levels = 2
  def vpnLen = idxLen * levels
  def getVPN(va: UInt): UInt = va(vaLen-1, offsetLen)
  def getVPNIdx(vpn: UInt, level: UInt) = (vpn >> (level * idxLen.U))(idxLen-1, 0)
  def PTE2PPN(pte: UInt): UInt = pte(ppnLen + 10 - 1, 10)
  def PPN2PtePA(ppn: UInt, idx: UInt = 0.U(idxLen.W)): UInt = {
    Cat(ppn(ppnLen-1, 0), idx(idxLen-1, 0), 0.U((offsetLen - idxLen).W))
  }
  def PPN2LeafPPN(ppn: UInt, vpn: UInt, level: UInt): UInt = {
    val mask = VecInit((0 until levels).map{ x =>
      ((-1.S(ppnLen.W) << x * idxLen).asUInt)(ppnLen-1, 0)
    })
    // Cat
    (ppn & mask(level)) | (vpn & (~mask(level)).asUInt)
  }
}

object SV32 extends SVParam

object SV39 extends SVParam{
  override def xLen = 64
  override def vaLen = 39
  override def paLen = 56
  override def idxLen = 9
  override def levels = 3
}

object MMUParam{
  def ptw_source = 4
  def mem_source = 4
  def depth_ptw_source = log2Ceil(ptw_source)
  def depth_mem_source = log2Ceil(mem_source)
}

class FlagBundle extends Bundle{
  val D = Bool()
  val A = Bool()
  val G = Bool()
  val U = Bool()
  val X = Bool()
  val W = Bool()
  val R = Bool()
  val V = Bool()
}

class PTE extends Bundle with SVParam{
  val reserved1 = UInt((xLen - ppnLen - 10).W)
  val PPN = UInt(ppnLen.W) // SV32: 22, SV39: 44
  val reserved2 = UInt(2.W)
  val flag = new FlagBundle()
  def isPDE: Bool = flag.V && !flag.R && !flag.W
  def isLeaf: Bool = flag.V && (flag.R || flag.W)
}

import pipeline.mmu.MMUParam._

class PTW_Req(SV: SVParam) extends Bundle{
  val vpn = UInt(SV.vpnLen.W)
  val ptbr = UInt(SV.xLen.W)
  val source = UInt(depth_ptw_source.W)
}

class PTW_Rsp(SV: SVParam) extends Bundle with L2TlbParam {
  val ppns = Vec(nSectors, UInt(SV.ppnLen.W))
  val flags = UInt(8.W)
  val source = UInt(depth_ptw_source.W)
  val fault = Bool()
}

class Cache_Req(SV: SVParam) extends Bundle{
  val addr = UInt(SV.xLen.W)
  val source = UInt(depth_mem_source.W)
}

class Cache_Rsp(SV: SVParam) extends Bundle with L2TlbParam{
  val data = Vec(nSectors, UInt(SV.xLen.W))
  val source = UInt(depth_mem_source.W)
}

class PTW(SV: SVParam, Ways: Int = 1) extends Module with L2TlbParam {
  val io = IO(new Bundle{
    val ptw_req = Flipped(DecoupledIO(new PTW_Req(SV)))
    val ptw_rsp = DecoupledIO(new PTW_Rsp(SV))
    val mem_req = DecoupledIO(new Cache_Req(SV))
    val mem_rsp = Flipped(DecoupledIO(new Cache_Rsp(SV)))
  })

  val s_idle :: s_memreq :: s_memwait :: s_rsp :: s_fault :: Nil = Enum(5)

  class PTWEntry extends Bundle with L2TlbParam {
    val ppns = Vec(nSectors, UInt(SV.ppnLen.W))
    val sectorIdx = UInt(log2Up(nSectors).W)
    val flags = Vec(nSectors, UInt(8.W))
    val vpn = UInt(SV.vpnLen.W)
    val cur_level = UInt(log2Up(SV.levels).W)
    val source = UInt(depth_ptw_source.W)
    val fault = Bool()
  }

  // Generate PA from selected sector of PPNs & VPN level
  def makePA(x: PTWEntry) = SV.PPN2PtePA(x.ppns(x.sectorIdx), SV.getVPNIdx(x.vpn, x.cur_level))
  // Align the address
  def alignedPA(x: UInt): (UInt, UInt) = {
    val split = log2Up(SV.xLen / 8) + log2Up(nSectors)
    val sectorIdx = x(split - 1, log2Up(SV.xLen / 8)) // sector from wide memory response
    val aligned = Cat( x(x.getWidth - 1, split), 0.U(split.W))
    (aligned, sectorIdx)
  }

  val entries = RegInit(Vec(Ways, 0.U.asTypeOf(new PTWEntry)))

  val state = RegInit(VecInit(Seq.fill(Ways)(s_idle)))
  val is_idle = state.map(_ === s_idle)
  val is_memwait = state.map(_ === s_memwait)
  val is_rsp = state.map{x => x === s_rsp || x === s_fault}

  val avail = is_idle.reduce(_ || _)
  val enq_ptr = PriorityEncoder(is_idle)

  io.ptw_req.ready := avail
  when(io.ptw_req.fire){ // idle -> mem req
    state(enq_ptr) := s_memreq
    entries(enq_ptr).cur_level := (SV.levels - 1).U
    entries(enq_ptr).ppns(0) := io.ptw_req.bits.ptbr(SV.ppnLen - 1, 0)
    entries(enq_ptr).sectorIdx := 0.U // access PTBR always sector 0
    entries(enq_ptr).source := io.ptw_req.bits.source
    entries(enq_ptr).fault := false.B
  }
  val memreq_arb = Module(new RRArbiter(new Cache_Req(SV), Ways))
  (0 until Ways).foreach{ i =>
    val req = Wire(new Cache_Req(SV))
    req.addr := alignedPA(makePA(entries(i)))._1 // makePA: select correct sector of PPN, append VPN index by cur_level
    req.source := entries(i).source
    memreq_arb.io.in(i).bits := req
    memreq_arb.io.in(i).valid := state(i) === s_memreq
  }
  io.mem_req <> memreq_arb.io.out

  when(io.mem_req.fire){ // mem req -> mem_wait
    state(memreq_arb.io.chosen) := s_memwait
    // update sector Index when send, so it can be read when mem_rsp
    // e.g. after sending mem_req(PTBR), sectorIdx will update to VPN(2)
    entries(memreq_arb.io.chosen).sectorIdx :=
      SV.getVPNIdx(entries(memreq_arb.io.chosen).vpn, entries(memreq_arb.io.chosen).cur_level)(log2Up(nSectors)-1, 0)
  }

  io.mem_rsp.ready := is_memwait.reduce(_ || _)

  val pte_rsp = io.mem_rsp.bits.data.asTypeOf(new PTE)

  when(io.mem_rsp.fire){
    (0 until Ways).foreach{ i =>
      when(is_memwait(i) && entries(i).source === io.mem_rsp.bits.source){
        // 非叶子节点
        when(pte_rsp.isPDE && entries(i).cur_level > 0.U){ // mem wait -> mem req
          state(i) := s_memreq
          entries(i).cur_level := entries(i).cur_level - 1.U
          entries(i).ppns := VecInit(io.mem_rsp.bits.data.map(SV.PTE2PPN))
        // 叶子节点
        }.elsewhen(pte_rsp.isLeaf){ // mem wait -> mem rsp
          entries(i).cur_level := 0.U
          entries(i).ppns := VecInit(io.mem_rsp.bits.data.map(SV.PTE2PPN))
          entries(i).flags := VecInit(io.mem_rsp.bits.data.map(_(7, 0)))
          state(i) := s_rsp
        }.otherwise{ // mem wait -> page fault
          entries(i).cur_level := 0.U
          entries(i).ppns := 0.U
          entries(i).flags := VecInit(io.mem_rsp.bits.data.map(_(7, 0)))
          entries(i).fault := true.B
          state(i) := s_fault
        }
      }
    }
  }

  val ptwrsp_arb = Module(new RRArbiter(new PTWEntry, Ways))
  (0 until Ways).foreach{ i =>
    ptwrsp_arb.io.in(i).bits := entries(i)
    ptwrsp_arb.io.in(i).valid := is_rsp(i) || state(i) === s_fault
  }
  ptwrsp_arb.io.out.ready := io.ptw_rsp.ready
  io.ptw_rsp.valid := ptwrsp_arb.io.out.valid
  io.ptw_rsp.bits.ppns := ptwrsp_arb.io.out.bits.ppns
  io.ptw_rsp.bits.source := ptwrsp_arb.io.out.bits.source
  io.ptw_rsp.bits.fault := ptwrsp_arb.io.out.bits.fault
  io.ptw_rsp.bits.flags := ptwrsp_arb.io.out.bits.flags

  when(io.ptw_rsp.fire){ // mem rsp -> idle, page fault -> idle
    entries(ptwrsp_arb.io.chosen) := 0.U.asTypeOf(new PTWEntry)
    state(ptwrsp_arb.io.chosen) := s_idle
  }
}

class PTW_L2Cluster_Adapter(SV: SVParam) extends Module{
  import top.parameters._

  val cache_param = (new L1Cache.MyConfig).toInstance
  val io = IO(new Bundle{
    val memreq_in = Flipped(DecoupledIO(new Cache_Req(SV)))
    val memrsp_out = DecoupledIO(new Cache_Rsp(SV))
    val memrsp_in = Seq.fill(num_cluster)(DecoupledIO(new TLBundleD_lite_plus(l2cache_params)))
    val memreq_out = Flipped(DecoupledIO(new TLBundleA_lite(l2cache_params)))
  })
  val align_mask = 1.U(SV.xLen.W) << ()
  val addr_base = io.memreq_in.bits.addr
}