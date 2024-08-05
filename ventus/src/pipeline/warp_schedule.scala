/*
 * Copyright (c) 2021-2022 International Innovation Center of Tsinghua University, Shanghai
 * Ventus is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 *          http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND,
 * EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT,
 * MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details. */
package pipeline

import chisel3._
import chisel3.util._
import top.parameters._

class warp_scheduler extends Module{
  val io = IO(new Bundle{
    val pc_reset = Input(Bool())
    val warpReq=Flipped(Decoupled(new warpReqData)) //new warp
    val warpRsp=Decoupled(new warpRspData) //endprg
    val wg_id_lookup=Output(UInt(depth_warp.W)) //lookup CTA
    val wg_id_tag=Input(UInt(TAG_WIDTH.W))  //barrier related
    val pc_req=Decoupled(new ICachePipeReq_np) //should flush icache
    val pc_rsp=Flipped(Valid(new ICachePipeRsp_np)) //icache miss state
    val branch = Flipped(DecoupledIO(new BranchCtrl)) //branch, flush pipeline
    val warp_control=Flipped(DecoupledIO(new warpSchedulerExeData)) //engprg and barrier
    val issued_warp=Flipped(Valid(UInt(depth_warp.W))) //not use
    val scoreboard_busy=Input(UInt(num_warp.W)) //scoreboard race
    val exe_busy=Input(UInt(num_warp.W)) //exe race
    //val pc_icache_ready=Input(Vec(num_warp,Bool()))
    val pc_ibuffer_ready=Input(Vec(num_warp,UInt(depth_ibuffer.W))) //ibuffer ready
    val warp_ready=Output(UInt(num_warp.W)) //to issue
    val flush=(ValidIO(UInt(depth_warp.W)))
    val flushCache=(ValidIO(UInt(depth_warp.W)))
    val CTA2csr=ValidIO(new warpReqData) //redirect warpreq
    //val ldst = Input(new warp_schedule_ldst_io()) // assume finish l2cache request
    //val switch = Input(Bool()) // assume coming from LDST unit (or other unit)
    val flushDCache = Decoupled(Bool())
    // val inquire_csr_wid = Output(UInt(depth_warp.W))
    // val inquire_csr_addr = Output(UInt(12.W))
    // val inquire_csr_data = Input(UInt(xLen.W))
  })

  val warp_end=io.warp_control.fire&io.warp_control.bits.ctrl.simt_stack_op
  val warp_end_id=io.warp_control.bits.ctrl.wid

  io.branch.ready:= !io.flushCache.valid
  io.warp_control.ready:= !io.branch.fire & !io.flushCache.valid

  io.warpReq.ready:=true.B
  io.warpRsp.valid:=warp_end // always ready.
  io.warpRsp.bits.wid:=warp_end_id

  io.CTA2csr.bits:=io.warpReq.bits
  io.CTA2csr.valid:=io.warpReq.valid


  io.flush.valid:=(io.branch.fire&io.branch.bits.jump) | warp_end//(暂定barrier不flush)
  io.flush.bits:=Mux((io.branch.fire&io.branch.bits.jump),io.branch.bits.wid,warp_end_id)
  io.flushCache.valid:=io.pc_rsp.valid&io.pc_rsp.bits.status(0)
  io.flushCache.bits:=io.pc_rsp.bits.warpid

  val pcControl=VecInit(Seq.fill(num_warp)(Module(new PCcontrol()).io))
  //val pcReplay=VecInit(pcControl.map(x=>RegEnable(x.PC_next,(x.PC_src===2.U)&(!x.PC_replay))))
  //val warp_memory_idle=Reg(Vec(num_warp,Bool()))
  //val warp_barrier_array=RegInit(0.U(num_warp.W))
  //val block_warp_waiting=RegInit(VecInit(Seq.fill(num_block)(0.U(num_warp.W)))) // if meet barrier, switch and set 1. all 1 -> all 0.
  val warp_init_addr=(VecInit(Seq.fill(num_warp)(0.U(32.W))))//,172.U,176.U) //初值怎么传进去，这是个问题？建议走CSR，并且是vec version的
  pcControl.foreach{
    x=>{
      x.New_PC:=io.branch.bits.new_pc
      x.PC_replay:=true.B
      x.PC_src:=0.U
      x.mask_i:=0.U
    }
  }
  val pc_ready=Wire(Vec(num_warp,Bool()))

  val current_warp=RegInit(0.U(depth_warp.W))
  val next_warp=WireInit(current_warp)
  current_warp:=next_warp
  pcControl(next_warp).PC_replay:= (!io.pc_req.ready)|(!pc_ready(next_warp))
  pcControl(next_warp).PC_src:=2.U
  io.pc_req.bits.addr := pcControl(next_warp).PC_next
  io.pc_req.bits.warpid := next_warp
  io.pc_req.bits.mask := pcControl(next_warp).mask_o

  io.wg_id_lookup:=Mux(!io.warp_control.bits.ctrl.simt_stack_op,warp_end_id,io.warpRsp.bits.wid) //barrier的时候没有warp_end，只是叫这个名字

  val warp_bar_cur=RegInit(VecInit(Seq.fill(num_block)(0.U(num_warp_in_a_block.W))))
  val warp_bar_exp=RegInit(VecInit(Seq.fill(num_block)(0.U(num_warp_in_a_block.W))))
  val warp_endprg_cnt = RegInit(VecInit(Seq.fill(num_block)(0.U(num_warp_in_a_block.W))))
  val warp_wg_valid = RegInit(VecInit(Seq.fill(num_block)(false.B)))
  val warp_endprg_mask_0 = WireInit(VecInit(Seq.fill(num_block)(false.B)))
  //val warp_bar_cur_next=warp_bar_cur
  //val warp_bar_exp_next=warp_bar_exp
  val warp_bar_lock=WireInit(VecInit(Seq.fill(num_block)(false.B))) //equals to "active block"
  val new_wg_id=io.warpReq.bits.CTAdata.dispatch2cu_wf_tag_dispatch(TAG_WIDTH-1,WF_COUNT_WIDTH_PER_WG)
  val new_wf_id=io.warpReq.bits.CTAdata.dispatch2cu_wf_tag_dispatch(WF_COUNT_WIDTH_PER_WG-1,0)
  val new_wg_wf_count=io.warpReq.bits.CTAdata.dispatch2cu_wg_wf_count
  val end_wg_id=io.wg_id_tag(TAG_WIDTH-1,WF_COUNT_WIDTH_PER_WG)
  val end_wf_id=io.wg_id_tag(WF_COUNT_WIDTH_PER_WG-1,0)
  val warp_bar_data=RegInit(0.U(num_warp.W))  // 0 means not locked by barrier
  val warp_bar_belong=RegInit(VecInit(Seq.fill(num_block)(0.U(num_warp.W))))

  when(io.warpReq.fire){
    warp_bar_belong(new_wg_id):=warp_bar_belong(new_wg_id) | (1.U<<io.warpReq.bits.wid).asUInt  //显示warp中有哪些属于wg
//    warp_bar_exp(new_wg_id):= warp_bar_exp(new_wg_id) | (1.U<<io.warpReq.bits.wid).asUInt
    when(!warp_bar_lock(new_wg_id)) {
      warp_bar_cur(new_wg_id) := 0.U
      warp_bar_exp(new_wg_id) := (1.U << new_wg_wf_count).asUInt - 1.U  // init to 1 for all future wfs in wg
    }
  }
  when(io.warpRsp.fire){
//    warp_bar_exp(end_wg_id):=warp_bar_exp(end_wg_id) & (~(1.U<<io.warpRsp.bits.wid)).asUInt
    warp_bar_belong(end_wg_id):=warp_bar_belong(end_wg_id) & (~(1.U<<io.warpRsp.bits.wid)).asUInt
  }
  warp_bar_lock:=warp_bar_belong.map(x=>x.orR)
  when(io.warp_control.fire&(!io.warp_control.bits.ctrl.simt_stack_op)){ //means barrrier
    warp_bar_cur(end_wg_id):=warp_bar_cur(end_wg_id) | (1.U<<end_wf_id).asUInt
    warp_bar_data:=warp_bar_data | (1.U<<io.warp_control.bits.ctrl.wid).asUInt
    when((warp_bar_cur(end_wg_id) | (1.U<<end_wf_id).asUInt) === warp_bar_exp(end_wg_id)){
      warp_bar_cur(end_wg_id):=0.U
      warp_bar_data:=warp_bar_data & (~warp_bar_belong(end_wg_id)).asUInt
    }
  }
  // collect endprg in one wg and issue flush request
  when(io.warpReq.fire){
    warp_endprg_cnt(new_wg_id):=warp_endprg_cnt(new_wg_id) | (1.U<<io.warpReq.bits.wid).asUInt
    warp_wg_valid(new_wg_id):=true.B
  }
  when(io.warpRsp.fire){
    warp_endprg_cnt(new_wg_id) := warp_endprg_cnt(end_wg_id) & (~(1.U<<io.warpRsp.bits.wid)).asUInt
  }
  for(i<-0 until num_block){
    warp_endprg_mask_0(i) := (warp_endprg_cnt(i).orR === false.B) && warp_wg_valid(i)
  }
  val need_flush = warp_endprg_mask_0.asUInt.orR
  val flush_entry = OHToUInt(warp_endprg_mask_0.asUInt)
  when(warp_endprg_mask_0(flush_entry) && io.flushDCache.ready){
    warp_wg_valid(flush_entry) := false.B
  }
  io.flushDCache.valid := need_flush
  io.flushDCache.bits := need_flush


  val warp_active=RegInit(0.U(num_warp.W))



  warp_active:=(warp_active | ((1.U<<io.warpReq.bits.wid).asUInt&Fill(num_warp,io.warpReq.fire))) & (~( Fill(num_warp,warp_end)&(1.U<<warp_end_id).asUInt )).asUInt
  val warp_ready=(~(warp_bar_data | io.scoreboard_busy | io.exe_busy | (~warp_active).asUInt)).asUInt
  io.warp_ready:=warp_ready
  for (i<- num_warp-1 to 0 by -1){
    pc_ready(i):= io.pc_ibuffer_ready(i) & warp_active(i) 
    when(pc_ready(i)){next_warp:=i.asUInt}
  }
  io.pc_req.valid:=pc_ready(next_warp)
  //lock one warp to execute
  //next_warp:=0.U
  if(SINGLE_INST) next_warp:=0.U



  when(io.pc_rsp.valid&io.pc_rsp.bits.status(0)){//miss acknowledgement
    pcControl(io.pc_rsp.bits.warpid).PC_replay:=false.B
    pcControl(io.pc_rsp.bits.warpid).PC_src:=3.U
    pcControl(io.pc_rsp.bits.warpid).New_PC:=io.pc_rsp.bits.addr//pcReplay(io.pc_rsp.bits.warpid)
    pcControl(io.pc_rsp.bits.warpid).mask_i:=io.pc_rsp.bits.mask
  }

  when(io.branch.fire&io.branch.bits.jump){
    pcControl(io.branch.bits.wid).PC_replay:=false.B
    pcControl(io.branch.bits.wid).PC_src:=1.U
    pcControl(io.branch.bits.wid).New_PC:=io.branch.bits.new_pc
    //PC:=io.branch.bits.new_pc
    when(io.branch.bits.wid===next_warp){
    io.pc_req.valid:=false.B}
  }


  when(io.warpReq.fire){
    pcControl(io.warpReq.bits.wid).PC_replay:=false.B
    pcControl(io.warpReq.bits.wid).PC_src:=1.U
    pcControl(io.warpReq.bits.wid).New_PC:=io.warpReq.bits.CTAdata.dispatch2cu_start_pc_dispatch
  }


  when(io.pc_reset){
    pcControl.zipWithIndex.foreach{case(x,b)=>{x.PC_src:=1.U;x.New_PC:=warp_init_addr(b);x.PC_replay:=false.B} }
    io.pc_req.valid:=false.B
  }
}