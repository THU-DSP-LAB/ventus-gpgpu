package pipeline.mmu

import chisel3._
import chisel3.util._

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
  val level = UInt(log2Up(SV.levels).W)
  val ppn = UInt(SV.ppnLen.W)
  val flag = UInt(8.W)
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
    val l2_rsp = DecoupledIO(new Bundle{
      val ppn = UInt(SV.ppnLen.W)
    })
  })
  val storage = Reg(Vec(nWays, new L1TlbEntry(SV)))

  val s_idle :: s_check :: s_l2tlb_req :: s_l2tlb_rsp :: s_reply :: Nil = Enum(5)
  val nState = WireInit(s_idle)
  val cState = RegNext(nState)

  io.in.ready := cState === s_idle && !io.invalidate.valid

  val tlb_req = RegInit(0.U.asTypeOf(io.in.bits))

  val hitVec = VecInit(storage.map{ x =>
    (x.asid === tlb_req.asid) && (x.vpn === SV.getVPN(tlb_req.vaddr)) && x.flag(0)
  }).asUInt
  val hit = hitVec.orR

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
      }.otherwise{
        nState := s_l2tlb_req
      }
    }
    is(s_l2tlb_req){
      when(io.l2_req.fire){
        nState := s_l2tlb_rsp
      }
    }
  }
}