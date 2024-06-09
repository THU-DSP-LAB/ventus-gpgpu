package mmu

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

import MMUParam._

class PTW_Req(SV: SVParam) extends Bundle{
  val vpn = UInt(SV.vpnLen.W)
  val paddr = UInt(SV.xLen.W)
  val curlevel = UInt(log2Ceil(SV.levels + 1).W)
  val source = UInt(depth_ptw_source.W)
}

class PTW_Rsp(SV: SVParam) extends Bundle with L2TlbParam {
  val ppns = Vec(nSectors, UInt(SV.ppnLen.W))
  val flags = Vec(nSectors, UInt(8.W))
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

class PTW(
   SV: SVParam,
   Banks: Int = 1,
   debug: Boolean = false,
   L2C: Option[L2cache.InclusiveCacheParameters_lite] = None
) extends Module with L2TlbParam {
  def memreq_gen: Bundle = L2C match {
    case Some(l2c) => new TLBundleA_lite(l2c)
    case None => new Cache_Req(SV)
  }
  def memrsp_gen: Bundle = L2C match {
    case Some(l2c) => new TLBundleD_lite(l2c)
    case None => new Cache_Rsp(SV)
  }

  val io = IO(new Bundle{
    val ptw_req = Vec(Banks, Flipped(DecoupledIO(new PTW_Req(SV))))
    val ptw_rsp = Vec(Banks, DecoupledIO(new PTW_Rsp(SV)))
    val mem_req: Vec[DecoupledIO[Bundle]] = Vec(Banks, DecoupledIO(memreq_gen))
    val mem_rsp: Vec[DecoupledIO[Bundle]] = Vec(Banks, Flipped(DecoupledIO(memrsp_gen)))
    val accel_fill = Vec(Banks, ValidIO(new Bundle{
      val ppns = Vec(nSectors, UInt(SV.ppnLen.W))
      val flags = Vec(nSectors, UInt(8.W))
      val cur_level = UInt(log2Ceil(SV.levels).W)
    }))
  })

  val s_idle :: s_memreq :: s_memwait :: s_rsp :: s_fault :: Nil = Enum(5)

  class PTWEntry extends Bundle with L2TlbParam {
    val ppns = Vec(nSectors, UInt(SV.ppnLen.W))
    val sectorIdx = UInt(log2Up(nSectors).W)
    val flags = Vec(nSectors, UInt(8.W))
    val vpn = UInt(SV.vpnLen.W)
    val cur_level = UInt(log2Up(SV.levels + 1).W)
    val source = L2C match {
      case Some(l2c) => UInt(l2c.source_bits.W)
      case None => UInt(depth_ptw_source.W)
    }
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

  val entries = RegInit(VecInit(Seq.fill(Banks)(0.U.asTypeOf(new PTWEntry))))

  val state = RegInit(VecInit(Seq.fill(Banks)(s_idle)))
  val is_idle = VecInit(state.map(_ === s_idle))
  val is_memwait = VecInit(state.map(_ === s_memwait))
  val is_rsp = VecInit(state.map{x => x === s_rsp || x === s_fault})

  //val avail = is_idle.reduce(_ || _)
  //val enq_ptr = PriorityEncoder(is_idle)

  (io.ptw_req zip is_idle).foreach{ case(req, idle) => req.ready := idle }
  (0 until Banks).foreach{ i =>
    val ptw_req = io.ptw_req(i)
    when(ptw_req.fire){ // idle -> mem req
      state(i) := s_memreq
      entries(i).cur_level := ptw_req.bits.curlevel
      entries(i).vpn := ptw_req.bits.vpn
      entries(i).ppns(0) := ptw_req.bits.paddr >> SV.offsetLen
      entries(i).sectorIdx := 0.U // root of a page directory is always sector 0
      entries(i).source := ptw_req.bits.source
      entries(i).fault := false.B
      //printf(p"PTW#${enq_ptr} REQ | vpn: ${Hexadecimal(io.ptw_req.bits.vpn)} ptbr: ${io.ptw_req.bits.ptbr}\n")
    }
    val ptw_rsp = io.ptw_rsp(i)
    when(ptw_rsp.fire){ // mem rsp -> idle, page fault -> idle
      entries(i) := 0.U.asTypeOf(new PTWEntry)
      state(i) := s_idle
    }
  }

//  val memreq_arb = L2C match {
//    case Some(l2c) => Module(new RRArbiter(new TLBundleA_lite(l2c), Banks))
//    case None => Module(new RRArbiter(new Cache_Req(SV), Banks))
//  }
  (0 until Banks).foreach{ i =>
    val req = Wire(memreq_gen)
    req match {
      case r1: Cache_Req => {
        r1.addr := alignedPA(makePA(entries(i)))._1 // makePA: select correct sector of PPN, append VPN index by cur_level
        r1.source := entries(i).source
      }
      case r1: TLBundleA_lite => {
        r1.address := alignedPA(makePA(entries(i)))._1
        r1.source := entries(i).source
        r1.data := 0.U.asTypeOf(r1.data)
        r1.mask := Fill(r1.mask.getWidth, true.B)
        r1.opcode := 4.U // READ
        r1.size := 0.U // TODO: correct the value
        r1.param := 0.U // TODO: correct the value
        r1.spike_info.foreach( _ := DontCare )
      }
    }
    io.mem_req(i).bits := req
    io.mem_req(i).valid := state(i) === s_memreq
  }

  (0 until Banks).foreach{ i =>
    when(io.mem_req(i).fire){ // mem req -> mem_wait
      state(i) := s_memwait
      // update sector Index when send, so it can be read when mem_rsp
      // e.g. after sending mem_req(PTBR), sectorIdx will update to VPN(2)
      entries(i).sectorIdx :=
        SV.getVPNIdx(entries(i).vpn, entries(i).cur_level)(log2Up(nSectors)-1, 0)
    }
    val mem_rsp_data = io.mem_rsp(i).bits match {
      case x: TLBundleD_lite => VecInit((0 until nSectors).map{ i => (x.data >> (i * SV.xLen))(SV.xLen-1, 0) })
      case y: Cache_Rsp => y.data
    }
    io.mem_rsp(i).ready := is_memwait(i)

    val pte_rsp = mem_rsp_data(entries(i).sectorIdx).asTypeOf(new PTE)
    io.accel_fill(i).bits.ppns := VecInit(mem_rsp_data.map(SV.PTE2PPN))
    io.accel_fill(i).bits.flags := VecInit(mem_rsp_data.map(_(7, 0)))
    io.accel_fill(i).bits.cur_level := entries(i).cur_level - 1.U
    io.accel_fill(i).valid := false.B

    when(io.mem_rsp(i).fire){
      when(is_memwait(i)){
        // non-leaf node
        when(pte_rsp.isPDE && entries(i).cur_level > 0.U){ // mem wait -> mem req
          state(i) := s_memreq
          entries(i).cur_level := entries(i).cur_level - 1.U
          entries(i).ppns := VecInit(mem_rsp_data.map(SV.PTE2PPN))
          io.accel_fill(i).valid := true.B
          // leaf node
        }.elsewhen(pte_rsp.isLeaf){ // mem wait -> mem rsp
          entries(i).cur_level := 0.U
          entries(i).ppns := VecInit(mem_rsp_data.map(SV.PTE2PPN))
          entries(i).flags := VecInit(mem_rsp_data.map(_(7, 0)))
          state(i) := s_rsp
        }.otherwise{ // mem wait -> page fault
          entries(i).cur_level := 0.U
          entries(i).ppns.foreach{ _ := 0.U }
          entries(i).flags := VecInit(mem_rsp_data.map(_(7, 0)))
          entries(i).fault := true.B
          state(i) := s_fault
        }
      }
    }
    io.ptw_rsp(i).bits := entries(i)
    io.ptw_rsp(i).valid := is_rsp(i) || state(i) === s_fault

    if(debug){
      when(io.mem_req(i).fire){
        val cur_entry = entries(i)
        val cur_sector = cur_entry.sectorIdx
        printf(p"PTW#${i} MEM | ppn: 0x${Hexadecimal(SV.PPN2PtePA(cur_entry.ppns(cur_sector), 0.U))}[" +
          p"idx: 0x${Hexadecimal(SV.getVPNIdx(cur_entry.vpn, cur_entry.cur_level))}] " +
          p"pa: 0x${Hexadecimal(alignedPA(makePA(cur_entry))._1)}+${Hexadecimal(alignedPA(makePA(cur_entry))._2)}\n")
      }
      when(io.mem_rsp(i).fire){
        printf(p"PTW#${i} MEM | ->")
        (0 until nSectors).foreach{ j =>
          when(j.U === entries(i).sectorIdx){
            printf(p" [0x${Hexadecimal(SV.PTE2PPN(mem_rsp_data(j)))}+${Hexadecimal(mem_rsp_data(j)(7, 0))}]")
          }.otherwise {
            printf(p" 0x${Hexadecimal(SV.PTE2PPN(mem_rsp_data(j)))}+${Hexadecimal(mem_rsp_data(j)(7, 0))}")
          }
        }
        printf(p"\n")
      }
    }
  }
}

