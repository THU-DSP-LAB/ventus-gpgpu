package pipeline.mmu

import chisel3._
import chisel3.util._
import freechips.rocketchip.util.{PseudoLRU, SetAssocLRU}

trait L1TlbParam{
  val nSets = 1
  val nWays = 8

  def vpnTlbBundle(SV: SVParam) = new Bundle{
    val tag = UInt((SV.vpnLen - log2Up(nSets)).W)
    val index = UInt(log2Up(nSets).W)
  }
}

class L1TlbEntry(SV: SVParam) extends Bundle with L1TlbParam {
  val asid = UInt(SV.asidLen.W)
  val vpn = UInt(SV.vpnLen.W)
  //val level = UInt(log2Up(SV.levels).W)
  val ppn = UInt(SV.ppnLen.W)
  val flags = UInt(8.W)
}

class L1TLB(SV: SVParam, nWays: Int) extends Module{
  val io = IO(new Bundle {
    val in = Flipped(DecoupledIO(new Bundle {
      val asid = UInt(SV.asidLen.W)
      val ptbr = UInt(SV.xLen.W)
      val vaddr = UInt(SV.vaLen.W)
    }))
    val invalidate = Flipped(ValidIO(new Bundle {
      val asid = UInt(SV.asidLen.W)
    }))
    val out = DecoupledIO(new Bundle{
      val paddr = UInt(SV.paLen.W)
    })
    val l2_req = DecoupledIO(new Bundle{
      val asid = UInt(SV.asidLen.W)
      val ptbr = UInt(SV.xLen.W)
      val vpn = UInt(SV.vpnLen.W)
    })
    val l2_rsp = Flipped(DecoupledIO(new Bundle{
      val ppn = UInt(SV.ppnLen.W)
      val flags = UInt(8.W)
    }))
  })
  val storage = Reg(Vec(nWays, new L1TlbEntry(SV)))
  val avails = VecInit(storage.map(x => x.flags(0)))

  val s_idle :: s_check :: s_l2tlb_req :: s_l2tlb_rsp :: s_reply :: Nil = Enum(5)
  val nState = WireInit(s_idle)
  val cState = RegNext(nState)

  io.in.ready := cState === s_idle && !io.invalidate.valid

  val tlb_req = RegInit(0.U.asTypeOf(io.in.bits))

  val hitVec = VecInit(storage.map{ x =>
    (x.asid === tlb_req.asid) && (x.vpn === SV.getVPN(tlb_req.vaddr)) && x.flags(0)
  }).asUInt
  val hit = hitVec.orR

  val replace = new PseudoLRU(nWays)
  val refillWay = Mux(avails.asUInt.orR, PriorityEncoder(avails), replace.way)
  val refillData = RegInit(0.U.asTypeOf(new L1TlbEntry(SV)))

  switch(cState){
    is(s_idle){
      when(io.in.fire){
        tlb_req := io.in.bits
        nState := s_check
      }
    }
    is(s_check){
      when(hit){
        nState := s_reply
        replace.access(hitVec)
        tlb_req := Cat(storage(OHToUInt(hitVec)).ppn, tlb_req.vaddr(SV.offsetLen-1, 0))
      }.otherwise{
        nState := s_l2tlb_req
        storage(refillWay).vpn := SV.getVPN(tlb_req.vaddr)
        storage(refillWay).asid := tlb_req.asid
      }
    }
    is(s_l2tlb_req){
      when(io.l2_req.fire){
        nState := s_l2tlb_rsp
      }
    }
    is(s_l2tlb_rsp){
      when(io.l2_rsp.fire){
        storage(refillWay).ppn := io.l2_rsp.bits.ppn
        storage(refillWay).flags := io.l2_rsp.bits.flags
        replace.access(refillWay)
        nState := s_reply
      }
    }
    is(s_reply){
      when(io.out.fire){
        tlb_req := 0.U.asTypeOf(tlb_req)
        nState := s_idle
      }
    }
  }
  io.out.valid := cState === s_reply
  io.out.bits.paddr := Cat(storage(refillWay).ppn, tlb_req.vaddr(SV.offsetLen-1, 0))
}