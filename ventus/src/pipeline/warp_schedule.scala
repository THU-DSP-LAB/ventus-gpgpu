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
    val asid =  Output(UInt(KNL_ASID_WIDTH.W)) // 2ibuffer
    val warp_ready=Output(UInt(num_warp.W)) //to issue
    val flush=(ValidIO(UInt(depth_warp.W)))
    val flushCache=(ValidIO(UInt(depth_warp.W)))
    val CTA2csr=ValidIO(new warpReqData) //redirect warpreq
    //val ldst = Input(new warp_schedule_ldst_io()) // assume finish l2cache request
    //val switch = Input(Bool()) // assume coming from LDST unit (or other unit)
    val flushDCache = Decoupled(Bool())

    //518
    val wg_id_lookup_async = Output(UInt(depth_warp.W))
    val wg_id_tag_async = Input(UInt(TAG_WIDTH.W))
    val issued_dma = Flipped(DecoupledIO(UInt(depth_warp.W))) // from  issue to dma 
    val finished_dma = Flipped(DecoupledIO(UInt(depth_warp.W)))  //from  dma module
  })


  val warp_end=io.warp_control.fire()&io.warp_control.bits.ctrl.simt_stack_op
  val warp_end_id=io.warp_control.bits.ctrl.wid

  io.branch.ready:= !io.flushCache.valid
  io.warp_control.ready:= !io.branch.fire() & !io.flushCache.valid
//518
  io.wg_id_lookup_async := Mux(io.issued_dma.fire,io.issued_dma.bits,io.finished_dma.bits)
  val dma_end_wg_id=io.wg_id_tag_async(WF_COUNT_WIDTH_PER_WG-1,0)  //WF_COUNT_WIDTH_PER_WG the width of the max num wavefront(warp) in a workgroup(block)

  io.warpReq.ready:=true.B
  io.warpRsp.valid:=warp_end // always ready.
  io.warpRsp.bits.wid:=warp_end_id

  io.CTA2csr.bits:=io.warpReq.bits
  io.CTA2csr.valid:=io.warpReq.valid
  val asidReg = Reg(Vec(num_warp,UInt(KNL_ASID_WIDTH.W)))
  val new_warpid = io.warpReq.bits.wid
  when(io.warpReq.fire){
    asidReg(new_warpid) := io.warpReq.bits.CTAdata.dispatch2cu_knl_asid_dispatch
  }
  io.asid := asidReg(io.pc_rsp.bits.warpid)


  io.flush.valid:=(io.branch.fire()&io.branch.bits.jump) | warp_end//(暂定barrier不flush)
  io.flush.bits:=Mux((io.branch.fire()&io.branch.bits.jump),io.branch.bits.wid,warp_end_id)
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
  io.pc_req.bits.asid := asidReg(next_warp)

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
  //518
  
  val warp_bar_data=RegInit(0.U(num_warp.W))  //0 means not locked by barrier
  val warp_bar_data_async=RegInit(0.U(num_warp.W)) // 518
  val warp_bar_data_async_tmp = Wire(UInt(num_warp.W)) // 518
  
  val warp_bar_cur_async_tmp=Wire(Vec(num_wgroup,UInt(warp_align_async.W)))  //4 warp a group

  val warp_dma = RegInit(VecInit(Seq.fill(num_warp)(0.U(max_dma_inst.W))))
  val warp_dmaing = Wire(Vec(num_warp,UInt(max_dma_inst.W)))
  val warp_dma_judge_reg = RegInit(VecInit(Seq.fill(num_wgroup)(0.U(2.W))))
  /* 0: all done
    * 1: barrier done, dma not done
    * 2: barrier not done, dma done
    * */
  val warp_dma_judge_wire = Wire(Vec(num_warp,Bool()))
  //  val warp_dma_not_done = Wire(Vec(num_wgroup,Bool()))
  val warp_bar_cur_async=RegInit(VecInit(Seq.fill(num_block)(0.U(num_warp_in_a_block.W))))
  val warp_bar_belong=RegInit(VecInit(Seq.fill(num_block)(0.U(num_warp.W))))
  val warp_bar_belong_async = RegInit(VecInit(Seq.fill(num_block)(VecInit(Seq.fill(num_wgroup)(0.U(num_warp.W))))))  //num_wgroup=num_warp /warp_align_async(4)
  (0 until(num_wgroup)).foreach( x=> {
    (0 until(warp_align_async)).foreach( y =>{
      warp_dma_judge_wire(x * warp_align_async + y) := warp_dma_judge_reg(x) =/= 0.U
    })
  })
  (0 until( num_warp_in_a_block)).foreach( x=> {
    warp_dmaing(x) := warp_dma(x)
    when(x.asUInt === dma_end_wg_id.asUInt && io.issued_dma.fire){
      warp_dmaing(x) := warp_dma(x) + 1.U   //recive onece add 1 to the id_warp 
    }
    when(x.asUInt === dma_end_wg_id.asUInt && io.finished_dma.fire){
      warp_dmaing(x) := warp_dma(x) - 1.U   //recive onece minus 1 to the id_warp
    }
  })
  (0 until( num_wgroup)).foreach( x=> {
    when(warp_dmaing.asUInt(x * warp_align_async* max_dma_inst + warp_align_async* max_dma_inst - 1, x * warp_align_async * max_dma_inst) === 0.U ){//&& !(io.warp_control.fire&&(!io.warp_control.bits.ctrl.simt_stack_op) && io.warp_control.bits.ctrl.dma )){
      //      warp_dma_not_done(x) := true.B
      //      when(warp_dma_judge_reg(x) === 0.U){
      //        warp_dma_judge_reg(x) := 2.U
      when(warp_dma_judge_reg(x) === 1.U){  //change the state
        warp_dma_judge_reg(x) := 0.U
      }
    }//.otherwise{
    //      warp_dma_not_done(x) := false.B
    //    }
  })
  val warp_bar_cur_async_change = Wire(Vec(num_wgroup,Bool()))
  when(io.warp_control.fire&&(!io.warp_control.bits.ctrl.simt_stack_op) && io.warp_control.bits.ctrl.dma ){
    (0 until (num_wgroup)).foreach(x => {//what is the meaning or expression (warp_bar_cur_async(end_wg_id) | (1.U << end_wf_id).asUInt))
      when(((warp_bar_cur_async(end_wg_id) | (1.U << end_wf_id).asUInt))(x * warp_align_async + warp_align_async - 1, x * warp_align_async) === warp_bar_exp(end_wg_id)(x * warp_align_async + warp_align_async - 1, x * warp_align_async)) {
        warp_bar_cur_async_tmp(x) := 0.U
        warp_bar_cur_async_change(x) := true.B
        when(warp_dma_judge_reg(x) === 0.U) {
          warp_dma_judge_reg(x) := 1.U
        }
        //          .elsewhen(warp_dma_judge_reg(x) === 2.U) {
        //          warp_dma_judge_reg(x) := 0.U
        //        }
      }.otherwise {
        warp_bar_cur_async_tmp(x) := (warp_bar_cur_async(end_wg_id) | (1.U << end_wf_id).asUInt)(x * warp_align_async + warp_align_async - 1, x * warp_align_async)
        warp_bar_cur_async_change(x) := false.B
//        warp_bar_data_async_tmp := (warp_bar_data_async | (1.U << io.warp_control.bits.ctrl.wid).asUInt)(x * warp_align_async + warp_align_async - 1, x * warp_align_async)
      }
    })
  }.otherwise {
    (0 until (num_wgroup)).foreach(x => {
      warp_bar_cur_async_change(x) := false.B
    })
    warp_bar_cur_async_tmp := (warp_bar_cur_async(end_wg_id)).asTypeOf(warp_bar_cur_async_tmp)
//    warp_bar_data_async_tmp := warp_bar_data_async(end_wg_id)
    //      warp_bar_data_async_tmp(x) := (warp_bar_data_async)(x * warp_align_async + warp_align_async - 1, x * warp_align_async)
  }
//  warp_bar_data_async_tmp := (warp_bar_data_async.asUInt & (~warp_bar_belong_async(end_wg_id)(x)).asUInt).asTypeOf(warp_bar_data_async_tmp)
  when(io.warp_control.fire&&(!io.warp_control.bits.ctrl.simt_stack_op) && io.warp_control.bits.ctrl.dma){
    when(warp_bar_cur_async_change.asUInt.orR){
      warp_bar_data_async_tmp := warp_bar_data_async.asUInt & (~warp_bar_belong_async(end_wg_id)((end_wf_id >> 2).asUInt)).asUInt
    }.otherwise{
      warp_bar_data_async_tmp := warp_bar_data_async | (1.U << io.warp_control.bits.ctrl.wid).asUInt
    }
  }.otherwise{
    warp_bar_data_async_tmp := warp_bar_data_async.asTypeOf(warp_bar_data_async_tmp)
  }


  io.issued_dma.ready := true.B
  io.finished_dma.ready := !io.issued_dma.fire || !io.warp_control.fire  // no issue_dma  or no warp_control data 
  when(io.issued_dma.fire || io.finished_dma.fire){
    //    warp_dma(io.issued_dma.bits) := warp_dma(io.issued_dma.bits) + 1.U
    warp_dma := warp_dmaing    //isuue dma indicate the whole barrier num  when finish  the num decrease
  }
  //  when(io.finished_dma.fire){
  //    warp_dma(io.finished_dma.bits) := warp_dma(io.finished_dma.bits) - 1.U
  //  }



  when(io.warpReq.fire){
    warp_bar_belong(new_wg_id):=warp_bar_belong(new_wg_id) | (1.U<<io.warpReq.bits.wid).asUInt()
    //518
    warp_bar_belong_async(new_wg_id)((io.warpReq.bits.CTAdata.dispatch2cu_wf_tag_dispatch(WF_COUNT_WIDTH_PER_WG-1,0) >> 2).asUInt) := warp_bar_belong_async(new_wg_id)((io.warpReq.bits.CTAdata.dispatch2cu_wf_tag_dispatch(WF_COUNT_WIDTH_PER_WG-1,0) >> 2).asUInt)| (1.U<<io.warpReq.bits.wid).asUInt()
    warp_bar_exp(new_wg_id):= warp_bar_exp(new_wg_id) | (1.U<<io.warpReq.bits.wid).asUInt//显示warp中有哪些属于wg
    when(!warp_bar_lock(new_wg_id)) {
      warp_bar_cur(new_wg_id) := 0.U
      warp_bar_exp(new_wg_id) := (1.U << new_wg_wf_count).asUInt - 1.U //init to 1 for all future wfs in wg
    }
  }
  when(io.warpRsp.fire){
    //    warp_bar_exp(end_wg_id):=warp_bar_exp(end_wg_id) & (~(1.U<<io.warpRsp.bits.wid)).asUInt
    warp_bar_belong(end_wg_id):=warp_bar_belong(end_wg_id) & (~(1.U<<io.warpRsp.bits.wid)).asUInt
  }
  warp_bar_lock:=warp_bar_belong.map(x=>x.orR)



  when(io.warp_control.fire&(!io.warp_control.bits.ctrl.simt_stack_op) ){ //means barrrier 518
    when(io.warp_control.bits.ctrl.dma){ // 518
      //      warp_bar_cur_async(end_wg_id) := warp_bar_cur_async(end_wg_id) | (1.U << io.warp_control.bits.ctrl.wid).asUInt
      //      warp_bar_data_async := warp_bar_data_async | (1.U << io.warp_control.bits.ctrl.wid).asUInt
      warp_bar_cur_async(end_wg_id) := warp_bar_cur_async_tmp.asUInt
      warp_bar_data_async := warp_bar_data_async_tmp.asUInt
    }.otherwise{
      warp_bar_cur(end_wg_id) := warp_bar_cur(end_wg_id) | (1.U << end_wf_id).asUInt
      warp_bar_data := warp_bar_data | (1.U << io.warp_control.bits.ctrl.wid).asUInt
      when((warp_bar_cur(end_wg_id) | (1.U << end_wf_id).asUInt()) === warp_bar_exp(end_wg_id)) {
        warp_bar_cur(end_wg_id) := 0.U
        warp_bar_data := warp_bar_data & (~warp_bar_belong(end_wg_id)).asUInt
      }
    }
  }


  // collect endprg in one wg and issue flush request
  when(io.warpReq.fire){
    warp_endprg_cnt(new_wg_id):=warp_endprg_cnt(new_wg_id) | (1.U<<io.warpReq.bits.wid).asUInt()
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



  warp_active:=(warp_active | ((1.U<<io.warpReq.bits.wid).asUInt()&Fill(num_warp,io.warpReq.fire()))) & (~( Fill(num_warp,warp_end)&(1.U<<warp_end_id).asUInt() )).asUInt
 //518
  val warp_ready=(~(warp_bar_data |warp_bar_data_async|warp_dma_judge_wire.asUInt| io.scoreboard_busy | io.exe_busy | (~warp_active).asUInt)).asUInt //518

  io.warp_ready:=warp_ready
  for (i<- num_warp-1 to 0 by -1){
    pc_ready(i):= io.pc_ibuffer_ready(i) & warp_active(i)
    when(pc_ready(i)){next_warp:=i.asUInt()}
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

  when(io.branch.fire()&io.branch.bits.jump){
    pcControl(io.branch.bits.wid).PC_replay:=false.B
    pcControl(io.branch.bits.wid).PC_src:=1.U
    pcControl(io.branch.bits.wid).New_PC:=io.branch.bits.new_pc
    //PC:=io.branch.bits.new_pc
    when(io.branch.bits.wid===next_warp){
      io.pc_req.valid:=false.B}
  }


  when(io.warpReq.fire()){
    pcControl(io.warpReq.bits.wid).PC_replay:=false.B
    pcControl(io.warpReq.bits.wid).PC_src:=1.U
    pcControl(io.warpReq.bits.wid).New_PC:=io.warpReq.bits.CTAdata.dispatch2cu_start_pc_dispatch
  }


  when(io.pc_reset){
    pcControl.zipWithIndex.foreach{case(x,b)=>{x.PC_src:=1.U;x.New_PC:=warp_init_addr(b);x.PC_replay:=false.B} }
    io.pc_req.valid:=false.B
  }
}