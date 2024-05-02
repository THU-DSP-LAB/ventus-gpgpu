package mmu

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

class L1TlbReq(SV: SVParam) extends Bundle{
  val asid = UInt(SV.asidLen.W)
  val vaddr = UInt(SV.vaLen.W)
}
class L1TlbRsp(SV: SVParam) extends Bundle{
  val paddr = UInt(SV.paLen.W)
}

abstract class L1TlbIO(SV: SVParam, Debug: Boolean = true) extends Module{
  val io = IO(new Bundle {
    val in = Flipped(DecoupledIO(new L1TlbReq(SV)))
    val invalidate = Flipped(ValidIO(new Bundle {
      val asid = UInt(SV.asidLen.W)
    }))
    val out = DecoupledIO(new L1TlbRsp(SV))
    val l2_req = DecoupledIO(new Bundle{
      val asid = UInt(SV.asidLen.W)
      val vpn = UInt(SV.vpnLen.W)
    })
    val l2_rsp = Flipped(DecoupledIO(new Bundle{
      val ppn = UInt(SV.ppnLen.W)
      val flags = UInt(8.W)
    }))
    val debug = if(Debug) Some(Output(new Bundle{
      val hit_cnt = UInt(20.W)
      val miss_cnt = UInt(20.W)
    })) else None
  })
}

class L1TlbAutoForward(SV: SVParam) extends L1TlbIO(SV, false){
  val tlb_req = RegInit(0.U.asTypeOf(io.in.bits))
  val tlb_rsp = RegInit(0.U(SV.paLen.W))

  val s_idle :: s_check :: s_l2tlb_req :: s_l2tlb_rsp :: s_reply :: Nil = Enum(5)
  val nState = WireInit(s_idle)
  val cState = RegNext(nState)

  io.l2_req.bits.asid := tlb_req.asid
  io.l2_req.bits.vpn := tlb_req.vaddr(SV.offsetLen-1, 0)
  io.out.bits.paddr := tlb_rsp
  io.in.ready := cState === s_idle || cState === s_reply
  io.out.valid := cState === s_reply
  io.l2_req.valid := cState === s_l2tlb_req
  io.l2_rsp.ready := cState === s_l2tlb_rsp

  switch(cState){
    is(s_idle){
      when(io.in.fire){
        tlb_req := io.in.bits
        nState := s_l2tlb_req
      }.otherwise{ nState := s_idle}
    }
    is(s_l2tlb_req){
      when(io.l2_req.fire){
        nState := s_l2tlb_rsp
      }.otherwise{ nState := s_l2tlb_req }
    }
    is(s_l2tlb_rsp){
      when(io.l2_rsp.fire){
        nState := s_reply
        tlb_rsp := Cat(io.l2_rsp.bits.ppn, tlb_req.vaddr(SV.offsetLen-1, 0))
      }.otherwise{ nState := s_l2tlb_rsp }
    }
    is(s_reply){
      when(io.out.fire){
        nState := s_idle
        when(io.in.fire){
          tlb_req := io.in.bits
          nState := s_l2tlb_req
        }
      }.otherwise{ nState := s_reply }
    }
  }
}

class L1TlbAutoReflect(SV: SVParam) extends L1TlbIO(SV, false){
  io.invalidate <> DontCare
  io.l2_req <> DontCare
  io.l2_rsp <> DontCare
  val s_idle :: s_reply :: Nil = Enum(2)
  val state = RegInit(s_idle)
  val reply = RegInit(0.U.asTypeOf(new L1TlbRsp(SV)))
  when(state === s_idle && io.in.fire) {
    state := s_reply
    reply.paddr := io.in.bits.vaddr
  }
  when(state === s_reply && io.out.fire) {
    state := s_idle
    reply := 0.U.asTypeOf(reply)
  }
  io.out.valid := state === s_reply
  io.in.ready := state === s_idle
  io.out.bits := reply
}

class L1TLB(SV: SVParam, nWays: Int, Debug: Boolean = true) extends L1TlbIO(SV, Debug){

  val storage = Reg(Vec(nWays, new L1TlbEntry(SV)))
  val avails = VecInit(storage.map(x => !x.flags(0)))

  val s_idle :: s_check :: s_l2tlb_req :: s_l2tlb_rsp :: s_reply :: Nil = Enum(5)
  val nState = WireInit(s_idle)
  val cState = RegNext(nState)

  io.in.ready := (cState === s_idle || cState === s_reply) && !io.invalidate.valid

  val tlb_req = RegInit(0.U.asTypeOf(io.in.bits))
  val tlb_rsp = RegInit(0.U(SV.paLen.W))

  val hitVec = VecInit(storage.map{ x =>
    (x.asid === tlb_req.asid) && (x.vpn === SV.getVPN(tlb_req.vaddr)) && x.flags(0)
  }).asUInt
  val hit = hitVec.orR

  val replace = new PseudoLRU(nWays)
  val refillWay = Mux(avails.asUInt.orR, PriorityEncoder(avails), replace.way)
  val refillData = RegInit(0.U.asTypeOf(new L1TlbEntry(SV)))

  val hitCnt = new Counter(200000)
  val missCnt = new Counter(200000)
  io.debug.foreach{ x =>
    dontTouch(x)
    x.hit_cnt := hitCnt.value
    x.miss_cnt := missCnt.value
  }
  when(io.invalidate.valid){
    storage.foreach{ e => when(e.asid === io.invalidate.bits.asid & e.flags(0)){ e := 0.U.asTypeOf(new L1TlbEntry(SV)) } }
  }

  switch(cState){
    is(s_idle){
      when(io.in.fire){
        tlb_req := io.in.bits
        nState := s_check
      }.otherwise{ nState := s_idle }
    }
    is(s_check){
      when(hit){
        nState := s_reply
        replace.access(OHToUInt(hitVec))
        tlb_rsp := Cat(storage(OHToUInt(hitVec)).ppn, tlb_req.vaddr(SV.offsetLen-1, 0))
        hitCnt.inc
      }.otherwise{
        nState := s_l2tlb_req
        missCnt.inc
      }
    }
    is(s_l2tlb_req){
      when(io.l2_req.fire){
        nState := s_l2tlb_rsp
      }.otherwise{ nState := s_l2tlb_req }
    }
    is(s_l2tlb_rsp){
      when(io.l2_rsp.fire){
        storage(refillWay).vpn := SV.getVPN(tlb_req.vaddr)
        storage(refillWay).asid := tlb_req.asid
        tlb_rsp := Cat(io.l2_rsp.bits.ppn, tlb_req.vaddr(SV.offsetLen-1, 0))
        storage(refillWay).ppn := io.l2_rsp.bits.ppn
        storage(refillWay).flags := io.l2_rsp.bits.flags
        replace.access(refillWay)
        nState := s_reply
      }.otherwise{ nState := s_l2tlb_rsp }
    }
    is(s_reply){
      when(io.out.fire){
        tlb_req := 0.U.asTypeOf(tlb_req)
        nState := s_idle
        when(io.in.fire){
          tlb_req := io.in.bits
          nState := s_check
        }
      }.otherwise{ nState := s_reply }
    }
  }
  io.out.valid := cState === s_reply
  io.out.bits.paddr := tlb_rsp
  io.l2_req.valid := cState === s_l2tlb_req
  //io.l2_req.bits.ptbr := tlb_req.ptbr
  io.l2_req.bits.asid := tlb_req.asid
  io.l2_req.bits.vpn := SV.getVPN(tlb_req.vaddr)

  io.l2_rsp.ready := cState === s_l2tlb_rsp
}