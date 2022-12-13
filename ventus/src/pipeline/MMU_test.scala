package pipeline


import chisel3._
import chisel3.util._
import top._
import L2cache._

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
object SV39{
  val xLen = 64
  val ppnLen = 44
  val offsetLen = 12
  val idxLen = 9
  val levels = 3
  val vpnLen = idxLen * levels

  def getVPN(va: UInt): UInt = va(38, 12)
  def getVPNIdx(vpn: UInt, level: UInt): UInt = (vpn >> (level * idxLen.U))(idxLen-1, 0)
  def PTE2PPN(pte: UInt): UInt = pte(53, 10)
  def PPN2PA(ppn: UInt, idx: UInt = 0.U(idxLen.W)): UInt =
    (ppn << offsetLen.U).asUInt + (idx << 3.U).asUInt & "h00ffffffffffffff".U
  def PTBR2PPN(ptbr: UInt): UInt = ptbr(ppnLen-1, 0)
}

object MMUParameters {
  val depth_ptw_source = 0
  val depth_mem_source = 0
}

import SV39._
import MMUParameters._

class PTW_Req extends Bundle{
  val addr = UInt(xLen.W)
  val PTBR = UInt(xLen.W)
  val source = UInt(depth_ptw_source.W)
}

class PTW_Rsp extends Bundle{
  val addr = UInt(xLen.W)
  val source = UInt(depth_ptw_source.W)
}

class LL_Req extends PTW_Rsp{
  val vpn = UInt(idxLen.W)
}

class MEM_Req extends Bundle{
  val addr = UInt(xLen.W)
  val source = UInt(depth_mem_source.W)
}

class MEM_Rsp extends Bundle{
  val data = UInt(xLen.W)
  val source = UInt(depth_mem_source.W)
}

class PTWIO() extends Bundle{
  val ptw_req = Flipped(DecoupledIO(new PTW_Req))
  val ptw_rsp = DecoupledIO(new PTW_Rsp)
  val mem_req = DecoupledIO(new MEM_Req)
  val mem_rsp = Flipped(DecoupledIO(new MEM_Rsp))
}
class FistLevelPTW(ways: Int = 1) extends Module{
  val io = IO(new Bundle{
    val ptw_req = Flipped(DecoupledIO(new PTW_Req))
    //val ptw_rsp = DecoupledIO(new PTW_Rsp)
    val mem_req = DecoupledIO(new MEM_Req)
    val mem_rsp = Flipped(DecoupledIO(new MEM_Rsp))
    val ll_req = DecoupledIO(new LL_Req)
    //val ll_rsp = Flipped(DecoupledIO(new PTW_Rsp))
  })

  val s_idle :: s_memreq :: s_memwait :: s_llreq :: Nil = Enum(4)

  class PTWEntry extends Bundle{
    val ppn = UInt(ppnLen.W)
    val vpn = UInt(vpnLen.W)
    val cur_level = UInt(log2Up(levels).W)
    val source = UInt(depth_ptw_source.W)

    def makePA: UInt = {
      val vpnidx = getVPNIdx(vpn, cur_level)
      PPN2PA(ppn, vpnidx)
    }
  }
  val entries = RegInit(VecInit(Seq.fill(ways)(0.U.asTypeOf(new PTWEntry))))

  val state = RegInit(VecInit(Seq.fill(ways)(s_idle)))
  val is_idle = state.map(_ === s_idle)

  val full = !is_idle.reduce(_ || _)
  val enq_ptr = PriorityEncoder(is_idle)

  val is_memreq = state.map(_ === s_memreq)
  val is_memwait = state.map(_ === s_memwait)
  val is_llreq = state.map(_ === s_llreq)

  io.ptw_req.ready := !full
  when(io.ptw_req.fire){
    state(enq_ptr) := s_memreq
    entries(enq_ptr).cur_level := levels - 1.U
    entries(enq_ptr).vpn := getVPN(io.ptw_req.bits.addr)
    entries(enq_ptr).ppn := PTBR2PPN(io.ptw_req.bits.PTBR)
    entries(enq_ptr).source := io.ptw_req.bits.source
  }
  val memreq_arb = Module(new RRArbiter(new PTWEntry, ways))
  (0 until ways).foreach{ i =>
    memreq_arb.io.in(i).bits := entries(i)
    memreq_arb.io.in(i).valid := is_memreq(i)
  }
  io.mem_req.bits.addr := memreq_arb.io.out.bits.makePA
  io.mem_req.bits.source := memreq_arb.io.out.bits.source
  io.mem_req.valid := memreq_arb.io.out.valid
  memreq_arb.io.out.ready := io.mem_req.ready

  when(io.mem_req.fire){
    state(memreq_arb.io.chosen) := s_memwait
  }

  io.mem_rsp.ready := is_memwait.reduce(_ || _)
  when(io.mem_rsp.fire){
    (0 until ways).foreach{ i =>
      when(state(i) === s_memwait && entries(i).source === io.mem_rsp.bits.source){
        state(i) := Mux(entries(i).cur_level > 1.U, s_memreq, s_llreq)
        entries(i).cur_level := entries(i).cur_level - 1.U
        entries(i).ppn := PTE2PPN(io.mem_rsp.bits.data)
      }
    }
  }

  val deq_ptr = PriorityEncoder(is_llreq)
  when(io.ll_req.fire){
    state(deq_ptr) := s_idle
    entries(deq_ptr) := 0.U.asTypeOf(new PTWEntry)
  }
  val llreq_arb = Module(new RRArbiter(new PTWEntry, ways))
  (0 until ways).foreach { i =>
    llreq_arb.io.in(i).bits := entries(i)
    llreq_arb.io.in(i).valid := is_llreq(i)
  }
  io.ll_req.valid := llreq_arb.io.out.valid
  llreq_arb.io.out.ready := io.ll_req.ready
  io.ll_req.bits.vpn := getVPNIdx(llreq_arb.io.out.bits.vpn, 0.U)
  io.ll_req.bits.source := llreq_arb.io.out.bits.source
  io.ll_req.bits.addr := llreq_arb.io.out.bits.makePA

}

class LastLevelPTW(ways: Int) extends Module{

}
