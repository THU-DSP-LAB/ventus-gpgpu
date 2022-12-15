package pipeline


import chisel3._
import chisel3.util._
import top._
import L2cache._
import parameters._
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
  val addrLen = 64
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
  val ptw_source=4
  val mem_source=4
  val depth_ptw_source = log2Ceil(ptw_source)
  val depth_mem_source = log2Ceil(mem_source)
}

import SV39._
import MMUParameters._

class PTW_Req extends Bundle{
  val addr = UInt(addrLen.W)
  val PTBR = UInt(addrLen.W)
  val source = UInt(depth_ptw_source.W)
}

class PTW_Rsp extends Bundle{
  val addr = UInt(addrLen.W)
  val source = UInt(depth_ptw_source.W)
}

class LL_Req extends PTW_Rsp

class Cache_Req extends PTW_Rsp

class Cache_Rsp extends PTW_Rsp

class MEM_Req extends Bundle{
  val addr = UInt(addrLen.W)
  val source = UInt(depth_mem_source.W)
}

class MEM_Rsp extends Bundle{
  val data = UInt(addrLen.W)
  val source = UInt(depth_mem_source.W)
}

class FistLevelPTW(ways: Int = 1) extends Module{
  val io = IO(new Bundle{
    val ptw_req = Flipped(DecoupledIO(new PTW_Req))
    val mem_req = DecoupledIO(new Cache_Req)
    val mem_rsp = Flipped(DecoupledIO(new Cache_Rsp))
    val ll_req = DecoupledIO(new LL_Req)
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

  val avail = is_idle.reduce(_ || _)
  val enq_ptr = PriorityEncoder(is_idle)

  val is_memreq = state.map(_ === s_memreq)
  val is_memwait = state.map(_ === s_memwait)
  val is_llreq = state.map(_ === s_llreq)

  io.ptw_req.ready := avail
  when(io.ptw_req.fire){
    state(enq_ptr) := s_memreq
    entries(enq_ptr).cur_level := levels.U - 1.U
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
      when(is_memwait(i) && entries(i).source === io.mem_rsp.bits.source){
        state(i) := Mux(entries(i).cur_level > 1.U, s_memreq, s_llreq)
        entries(i).cur_level := entries(i).cur_level - 1.U
        entries(i).ppn := PTE2PPN(io.mem_rsp.bits.addr)
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
  io.ll_req.bits.source := llreq_arb.io.out.bits.source
  io.ll_req.bits.addr := llreq_arb.io.out.bits.makePA

}

class LastLevelPTW(ways: Int) extends Module{
  val io = IO(new Bundle{
    val ll_req = Flipped(DecoupledIO(new LL_Req))
    val mem_req = DecoupledIO(new Cache_Req)
    val mem_rsp = Flipped(DecoupledIO(new Cache_Rsp))
    val ptw_rsp = DecoupledIO(new PTW_Rsp)
  })
  val s_idle :: s_memreq :: s_memwait :: s_ptwrsp :: Nil = Enum(4)

  class LLPTWEntry extends LL_Req

  val entries = RegInit(VecInit.fill(ways)(0.U.asTypeOf(new LLPTWEntry)))

  val state = RegInit(VecInit.fill(ways)(s_idle))
  val is_idle = state.map(_ === s_idle)

  val avail = is_idle.reduce(_ || _)
  val enq_ptr = PriorityEncoder(is_idle)

  val is_memreq = state.map(_ === s_memreq)
  val is_memwait = state.map(_ === s_memwait)
  val is_ptwrsp = state.map(_ === s_ptwrsp)

  io.ll_req.ready := avail
  when(io.ll_req.fire){
    state(enq_ptr) := s_memreq
    entries(enq_ptr) := io.ll_req.bits
  }

  val memreq_arb = Module(new RRArbiter(new LLPTWEntry, ways))
  (0 until ways).foreach { i =>
    memreq_arb.io.in(i).bits := entries(i)
    memreq_arb.io.in(i).valid := is_memreq(i)
  }
  io.mem_req.bits := memreq_arb.io.out.bits
  io.mem_req.valid := memreq_arb.io.out.valid
  memreq_arb.io.out.ready := io.mem_req.ready

  when(io.mem_req.fire){
    state(memreq_arb.io.chosen) := s_memwait
  }

  io.mem_rsp.ready := is_memwait.reduce(_ || _)
  when(io.mem_rsp.fire){
    (0 until ways).foreach{ i =>
      when(is_memwait(i) && entries(i).source === io.mem_rsp.bits.source){
        state(i) := s_ptwrsp
        entries(i).addr := io.mem_rsp.bits.addr // raw pte here
      }
    }
  }

  val deq_ptr = PriorityEncoder(is_ptwrsp)
  when(io.ptw_rsp.fire){
    state(deq_ptr) := s_idle
    entries(deq_ptr) := 0.U.asTypeOf(new LLPTWEntry)
  }
  val ptwrsp_arb = Module(new RRArbiter(new LLPTWEntry, ways))
  (0 until ways).foreach{ i =>
    ptwrsp_arb.io.in(i).bits := entries(i)
    ptwrsp_arb.io.in(i).valid := is_ptwrsp(i)
  }
  io.ptw_rsp <> ptwrsp_arb.io.out
}

class TLB_Req extends PTW_Req
class TLB_Rsp extends PTW_Rsp
class L2TLB_Req extends PTW_Req
class L2TLB_Rsp extends PTW_Rsp

class TLB extends Module{
  val io = IO(new Bundle{
    val tlb_req = Flipped(DecoupledIO(new TLB_Req))
    val tlb_rsp = DecoupledIO(new TLB_Rsp)
    val l2tlb_rsp = Flipped(DecoupledIO(new L2TLB_Rsp))
    val l2tlb_req = DecoupledIO(new L2TLB_Req)
  })
  val l2tlb_req_fifo=Module(new Queue(new L2TLB_Req,1))
  val tlb_rsp_fifo=Module(new Queue(new TLB_Rsp,1))
  io.tlb_req<>l2tlb_req_fifo.io.enq
  io.l2tlb_req<>l2tlb_req_fifo.io.deq
  io.tlb_rsp<>tlb_rsp_fifo.io.deq
  io.l2tlb_rsp<>tlb_rsp_fifo.io.enq
}
class L2TLB extends Module{
  val io = IO(new Bundle{
    val l2tlb_req = Flipped(DecoupledIO(new L2TLB_Req))
    val l2tlb_rsp = DecoupledIO(new L2TLB_Rsp)
    val ptw_rsp = Flipped(DecoupledIO(new PTW_Rsp))
    val ptw_req = DecoupledIO(new PTW_Req)
  })
  val ptw_req_fifo=Module(new Queue(new PTW_Req,1))
  val l2tlb_rsp_fifo=Module(new Queue(new L2TLB_Rsp,1))
  io.l2tlb_req<>ptw_req_fifo.io.enq
  io.ptw_req<>ptw_req_fifo.io.deq
  io.ptw_rsp<>l2tlb_rsp_fifo.io.enq
  io.l2tlb_rsp<>l2tlb_rsp_fifo.io.deq
}

class PageCache extends Module{
  val io = IO(new Bundle{
    val in_req = Flipped(DecoupledIO(new Cache_Req))
    val in_rsp = DecoupledIO(new Cache_Req)
    val out_req = DecoupledIO(new MEM_Req)
    val out_rsp = Flipped(DecoupledIO(new MEM_Rsp))
  })
  val mem_req_fifo=Module(new Queue(new MEM_Req,1))
  val mem_rsp_fifo=Module(new Queue(new Cache_Rsp,1))
  io.in_req.valid<>mem_req_fifo.io.enq.valid
  io.in_req.ready<>mem_req_fifo.io.enq.ready
  io.in_req.bits.source<>mem_req_fifo.io.enq.bits.source
  io.in_req.bits.addr<>mem_req_fifo.io.enq.bits.addr
  io.out_req<>mem_req_fifo.io.deq
  io.in_rsp<>mem_rsp_fifo.io.deq
  io.out_rsp.bits.source<>mem_rsp_fifo.io.enq.bits.source
  io.out_rsp.bits.data<>mem_rsp_fifo.io.enq.bits.addr
  io.out_rsp.valid<>mem_rsp_fifo.io.enq.valid
  io.out_rsp.ready<>mem_rsp_fifo.io.enq.ready
}


class Arb_MEM_Req(val num_source:Int) extends MEM_Req {
  override val source = UInt((log2Up(num_source)+depth_mem_source).W)
}

class Arb_MEM_Rsp(val num_source:Int) extends MEM_Rsp{
  override val source = UInt((log2Up(num_source)+depth_mem_source).W)
}


class MMUMemArbiter(val num_source : Int = 2) extends Module {
  val io = IO(new Bundle{
    val memReqVecIn = Flipped(Vec(num_source, Decoupled(new MEM_Req())))
    val memReqOut = Decoupled(new Arb_MEM_Req(num_source))
    val memRspIn = Flipped(Decoupled(new Arb_MEM_Rsp(num_source)))
    val memRspVecOut = Vec(num_source, Decoupled(new MEM_Rsp()))
  })
  // **** memReq ****
  val memReqArb = Module(new Arbiter(new Arb_MEM_Req(num_source),num_source))
  memReqArb.io.in <> io.memReqVecIn
  for(i <- 0 until num_source) {
    memReqArb.io.in(i).bits.source := Cat(i.asUInt,io.memReqVecIn(i).bits.source)
  }
  io.memReqOut <> memReqArb.io.out
  // ****************

  // **** memRsp ****
  for(i <- 0 until num_source) {
    io.memRspVecOut(i).bits <> io.memRspIn.bits
    io.memRspVecOut(i).valid :=
      io.memRspIn.bits.source(depth_mem_source+log2Up(num_source)-1,depth_mem_source)===i.asUInt && io.memRspIn.valid
  }
  io.memRspIn.ready := Mux1H(UIntToOH(io.memRspIn.bits.source(depth_mem_source+log2Up(num_source)-1,depth_mem_source)),
    Reverse(Cat(io.memRspVecOut.map(_.ready))))
  // ****************
}


class MMU extends Module{
  val io = IO(new Bundle{
    val mmu_req=Flipped(DecoupledIO(new TLB_Req))
    val mmu_rsp=DecoupledIO(new TLB_Rsp)
    val mem_req=DecoupledIO(new Arb_MEM_Req(2))
    val mem_rsp=Flipped(DecoupledIO(new Arb_MEM_Rsp(2)))
  })
  val tlb=Module(new TLB)
  val l2tlb=Module(new L2TLB)
  val ptw=Module(new FistLevelPTW(ways = ptw_source))
  val llptw=Module(new LastLevelPTW(ways = ptw_source))
  val page_cache=Module(new PageCache)
  val ll_page_cache=Module(new PageCache)
  val mem_arb=Module(new MMUMemArbiter(2))

  tlb.io.tlb_req<>io.mmu_req
  tlb.io.tlb_rsp<>io.mmu_rsp
  tlb.io.l2tlb_req<>l2tlb.io.l2tlb_req
  l2tlb.io.l2tlb_rsp<>tlb.io.l2tlb_rsp
  l2tlb.io.ptw_req<>ptw.io.ptw_req
  llptw.io.ll_req<>ptw.io.ll_req
  llptw.io.ptw_rsp<>l2tlb.io.ptw_rsp
  llptw.io.mem_req<>ll_page_cache.io.in_req
  llptw.io.mem_rsp<>ll_page_cache.io.in_rsp
  ptw.io.mem_req<>page_cache.io.in_req
  ptw.io.mem_rsp<>page_cache.io.in_rsp
  ll_page_cache.io.out_req<>mem_arb.io.memReqVecIn(0)
  page_cache.io.out_req<>mem_arb.io.memReqVecIn(1)
  ll_page_cache.io.out_rsp<>mem_arb.io.memRspVecOut(0)
  page_cache.io.out_rsp<>mem_arb.io.memRspVecOut(1)
  mem_arb.io.memReqOut<>io.mem_req
  mem_arb.io.memRspIn<>io.mem_rsp

}

class MMUtest extends Module{
  val l2cache_cache=CacheParameters(2,l2cache_NWays,l2cache_NSets,8,8)
  val l2cache_micro=InclusiveCacheMicroParameters(l2cache_writeBytes,l2cache_memCycles,l2cache_portFactor,mem_source,2)
  val l2cache_params=InclusiveCacheParameters_lite(l2cache_cache,l2cache_micro,false)
  val l2cache_params_temp=InclusiveCacheParameters_lite(l2cache_cache,l2cache_micro,false)
  val io = IO(new Bundle{
    val host_req=Flipped(DecoupledIO(new host2CTA_data))
    val host_rsp=DecoupledIO(new CTA2host_data)
    val out_a =Decoupled(new TLBundleA_lite(l2cache_params_temp))
    val out_d=Flipped(Decoupled(new TLBundleD_lite(l2cache_params_temp)))
  })
  val mmu=Module(new MMU)
  io.host_req.ready<>mmu.io.mmu_req.ready
  io.host_req.valid<>mmu.io.mmu_req.valid
  io.host_req.bits.host_start_pc<>mmu.io.mmu_req.bits.addr
  io.host_req.bits.host_gds_baseaddr<>mmu.io.mmu_req.bits.PTBR
  io.host_req.bits.host_wg_id<>mmu.io.mmu_req.bits.source

  io.host_rsp.bits.inflight_wg_buffer_host_wf_done_wg_id:= mmu.io.mmu_rsp.bits.source
  io.host_rsp.valid:=mmu.io.mmu_rsp.valid
  io.host_rsp.ready<>mmu.io.mmu_rsp.ready
  when(io.host_rsp.fire){
    printf(p"addr translate complete, target is 0x${Hexadecimal(mmu.io.mmu_rsp.bits.addr)} for 0x${mmu.io.mmu_rsp.bits.source}")
  }
  io.out_a.valid<>mmu.io.mem_req.valid
  io.out_a.ready<>mmu.io.mem_req.ready
  io.out_a.bits.source<>mmu.io.mem_req.bits.source
  io.out_a.bits.mask:=1.U
  io.out_a.bits.address:=mmu.io.mem_req.bits.addr(31,0)
  io.out_a.bits.data:=0.U
  io.out_a.bits.size:=0.U
  io.out_a.bits.opcode:=4.U

  io.out_d.bits.data<>mmu.io.mem_rsp.bits.data
  io.out_d.bits.source<>mmu.io.mem_rsp.bits.source
  io.out_d.valid<>mmu.io.mem_rsp.valid
  io.out_d.ready<>mmu.io.mem_rsp.ready

}

object GPGPU_gen2 extends App{
  (new chisel3.stage.ChiselStage).emitVerilog(new MMUtest())
}