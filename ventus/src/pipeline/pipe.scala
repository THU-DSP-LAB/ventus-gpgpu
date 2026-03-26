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

import L1Cache.ICache._
import chisel3._
import chisel3.util._
import top.parameters._
import gvm._

class ICachePipeReq_np extends Bundle {
  val addr = UInt(32.W)
  val mask = UInt(num_fetch.W)
  val warpid = UInt(depth_warp.W)
  val asid = if(MMU_ENABLED) Some(UInt(KNL_ASID_WIDTH.W)) else None
}
class ICachePipeRsp_np extends Bundle{
  val addr = UInt(32.W)
  val data = UInt((num_fetch*32).W)
  val mask = UInt(num_fetch.W)
  val warpid = UInt(depth_warp.W)
  val status = UInt(2.W)
}

class pipe(
  val lsuAddrCalcPipeCut: Boolean = false,
  val operandCollectorRfPipeCut: Boolean = false,
  val operandCrossbarPipeCut: Boolean = false,
  val operandIssuePipeCut: Boolean = false,
  val ibufferIssuePipeCut: Boolean = false,
  val csrResultPipeCut: Boolean = false,
  val csrIssuePipeCut: Boolean = false,
  val simtPipeCut: Boolean = false,
  val fpuInputPipeCut: Boolean = false,
  val tensorCoreInputPipeCut: Boolean = false,
  val tensorCoreWbPipeCut: Boolean = false,
  val writebackOutPipeCut: Boolean = false
) extends Module{
  val sm_id = IO(Input(UInt(8.W)))
  val io = IO(new Bundle{
    val icache_req = (DecoupledIO(new ICachePipeReq_np))
    val icache_rsp = Flipped(DecoupledIO(new ICachePipeRsp_np))
    val externalFlushPipe = ValidIO(UInt(depth_warp.W))
    val dcache_req = DecoupledIO(new DCacheCoreReq_np)
    val dcache_rsp = Flipped(DecoupledIO(new DCacheCoreRsp_np))
    val shared_req = DecoupledIO(new ShareMemCoreReq_np)
    val shared_rsp = Flipped(DecoupledIO(new DCacheCoreRsp_np))
    val pc_reset = Input(Bool())
    val warpReq=Flipped(Decoupled(new warpReqData))
    val warpRsp=(Decoupled(new warpRspData))
    val wg_id_lookup=Output(UInt(depth_warp.W))
    val wg_id_tag=Input(UInt(TAG_WIDTH.W))
    val inst = if (SINGLE_INST) Some(Flipped(DecoupledIO(UInt(32.W)))) else None
    val inst_cnt = if(INST_CNT) Some(Output(UInt(32.W))) else if(INST_CNT_2) Some(Output(Vec(2, UInt(32.W)))) else None
    val inst_cnt2 = if(INST_CNT_2) Some(Output(Vec(2, UInt(32.W)))) else None
  })
  val issue_stall=Wire(Bool())
  val flush=Wire(Bool())


  val warp_sche=Module(new warp_scheduler)
  //val pcfifo=Module(new PCfifo)
  val control=Module(new InstrDecodeV2)
  control.io.sm_id := sm_id
  val operand_collector=Module(new operandCollector(operandCollectorRfPipeCut, operandCrossbarPipeCut))
  if (GVM_ENABLED) {
    val gvm_xreg = Module(new GvmDutXReg)
    gvm_xreg.io.clock := clock
    gvm_xreg.io.sm_id := sm_id
    // gvm_xreg.io.num_bank := num_bank.U(32.W)
    // gvm_xreg.io.num_sgpr_slots := NUMBER_SGPR_SLOTS.U(32.W)
    val scalar_flatten_banks = Wire(Vec(num_bank, UInt((NUMBER_SGPR_SLOTS / num_bank * xLen).W)))
    for (i <- 0 until num_bank) {
      scalar_flatten_banks(i) := operand_collector.io.scalarBanks.get(i).asUInt
    }
    // println(s"checkwidth${scalar_flatten_banks.asUInt.getWidth}")
    gvm_xreg.io.xbanks := scalar_flatten_banks.asUInt

    val gvm_vreg = Module(new GvmDutVReg)
    gvm_vreg.io.clock := clock
    gvm_vreg.io.sm_id := sm_id
    val vector_flatten_banks = Wire(Vec(num_bank, UInt((NUMBER_VGPR_SLOTS / num_bank * num_thread * xLen).W)))
    for (i <- 0 until num_bank) {
      vector_flatten_banks(i) := operand_collector.io.vectorBanks.get(i).asUInt
    }
    gvm_vreg.io.vbanks := vector_flatten_banks.asUInt
  }
  //val issue=Module(new Issue)
  val issueX = Module(new Issue)
  val issueV = Module(new Issue)
  val alu=Module(new ALUexe)
  val valu=Module(new vALUv2(num_thread, num_lane))
  val fpu=Module(new FPUexe(num_thread,num_lane))
  val lsu=Module(new LSUexe(lsuAddrCalcPipeCut))
  val sfu=Module(new SFUexe)
  val mul=Module(new vMULv2(num_thread,num_lane))
  val tensorcore=Module(new vTCexe)
  val lsu2wb=Module(new LSU2WB)
  val wb=Module(new Writeback(6,7))
  val wbOutXStage = Queue(wb.io.out_x, if (writebackOutPipeCut) 1 else 0, pipe = false, flow = false)
  val wbOutVStage = Queue(wb.io.out_v, if (writebackOutPipeCut) 1 else 0, pipe = false, flow = false)

  val inst_cnt_xv = RegInit(VecInit(0.U(32.W), 0.U(32.W)))
  if(INST_CNT_2){
    when(issueX.io.in.fire){
      when(issueV.io.in.fire && !issueV.io.in.bits.ctrl.isvec){
        inst_cnt_xv(0) := inst_cnt_xv(0) + 2.U
      }.otherwise{
        inst_cnt_xv(0) := inst_cnt_xv(0) + 1.U
      }
    }
    when(issueV.io.in.fire){
      when(issueV.io.in.bits.ctrl.isvec){
        inst_cnt_xv(1) := inst_cnt_xv(1) + PopCount(issueV.io.in.bits.mask)
      }
    }
  }

  val scoreb=VecInit(Seq.fill(num_warp)(Module(new Scoreboard).io))
  val ibuffer=Module(new InstrBufferV2)
  val ibuffer2issue=Module(new ibuffer2issue)
  val ibufferIssueStages = if (ibufferIssuePipeCut) {
    Some(Seq.fill(num_warp)(Module(new Queue(new CtrlSigs, 1, pipe = false, flow = false, hasFlush = true))))
  } else {
    None
  }
  if(INST_CNT) {
    io.inst_cnt.foreach(_ := ibuffer2issue.io.cnt.getOrElse(0.U))
  }
  if(INST_CNT_2){
    io.inst_cnt2.foreach( _ := inst_cnt_xv)
  }
  else{
    io.inst_cnt.foreach( _ := 0.U)
  }
  //  val exe_acq_reg=Module(new Queue(new CtrlSigs,1,pipe=true))
  val exe_dataX_src = Wire(DecoupledIO(new vExeData))
  val exe_dataV_src = Wire(DecoupledIO(new vExeData))
  val exe_dataX = Queue(exe_dataX_src, if (operandIssuePipeCut) 1 else 0, pipe = false, flow = false)
  val exe_dataV = Queue(exe_dataV_src, if (operandIssuePipeCut) 1 else 0, pipe = false, flow = false)
  class SimtStageData extends Bundle {
    val branch = new simtExeData
    val rpc = UInt(xLen.W)
  }
  val simt_stack=Module(new branch_join(num_thread))
  val branch_back=Module(new Branch_back)
  val csrfile=Module(new CSRexe(csrResultPipeCut))
  val csrIssueStage = Queue(issueX.io.out_CSR, if (csrIssuePipeCut) 1 else 0, pipe = false, flow = false)
  val simtStageSrc = Wire(DecoupledIO(new SimtStageData))
  val simtStage = Queue(simtStageSrc, if (simtPipeCut) 1 else 0, pipe = false, flow = false)
  val fpuInputStage = Queue(issueV.io.out_vFPU, if (fpuInputPipeCut) 1 else 0, pipe = false, flow = false)
  val tensorCoreInputStage = Queue(issueV.io.out_TC, if (tensorCoreInputPipeCut) 1 else 0, pipe = false, flow = false)
  val tensorCoreWbStage = Queue(tensorcore.io.out_v, if (tensorCoreWbPipeCut) 1 else 0, pipe = false, flow = false)

  io.externalFlushPipe.valid:=warp_sche.io.flush.valid|warp_sche.io.flushCache.valid
  io.externalFlushPipe.bits:=Mux(warp_sche.io.flush.valid,warp_sche.io.flush.bits,warp_sche.io.flushCache.bits)

  if (lsuAddrCalcPipeCut) {
    // CSR pre-fetch: drive csrfile with the incoming instruction's wid (from the pipe-level
    // dispatch bus, physically co-located with csrfile), then register the CSR outputs here
    // before forwarding to LSUexe.  This eliminates the long-wire round trip:
    //   AddrCalc_reg_save_ctrl_wid_reg (deep inside pipe_lsu)
    //   -> csrfile (pipe_csrfile) -> csr_pds_used_reg (back in pipe_lsu)
    // which was 1.83 ns at 1 GHz.  With the register placed here the two paths become:
    //   issueV_wid_reg -> csrfile mux -> csr_lsu_*_prereg  (all local to pipe)
    //   csr_lsu_*_prereg -> wire -> AddrCalc.csr_*_used_reg  (no mux, just wire)
    csrfile.io.lsu_wid := lsu.io.lsu_req.bits.ctrl.wid
    val csr_lsu_numw_prereg = RegEnable(csrfile.io.lsu_numw, lsu.io.lsu_req.fire)
    val csr_lsu_pds_prereg  = RegEnable(csrfile.io.lsu_pds,  lsu.io.lsu_req.fire)
    val csr_lsu_tid_prereg  = RegEnable(csrfile.io.lsu_tid,  lsu.io.lsu_req.fire)
    lsu.io.csr_numw := csr_lsu_numw_prereg
    lsu.io.csr_pds  := csr_lsu_pds_prereg
    lsu.io.csr_tid  := csr_lsu_tid_prereg
  } else {
    csrfile.io.lsu_wid := lsu.io.csr_wid
    lsu.io.csr_pds  := csrfile.io.lsu_pds
    lsu.io.csr_tid  := csrfile.io.lsu_tid
    lsu.io.csr_numw := csrfile.io.lsu_numw
  }
  if (SPIKE_OUTPUT) {
    when(csrfile.io.in.valid && csrfile.io.in.bits.ctrl.custom_signal_0){
      printf(p"sm ${sm_id} warp ${Decimal(csrfile.io.in.bits.ctrl.wid)} " +
             p"0x${Hexadecimal(csrfile.io.in.bits.ctrl.pc)} 0x${Hexadecimal(csrfile.io.in.bits.ctrl.inst)}  setrpc 0x${Hexadecimal(csrfile.io.in.bits.in1)} \n")
    }
  }

  warp_sche.io.pc_reset:=io.pc_reset
  warp_sche.io.branch<>branch_back.io.out
  //warp_sche.io.pc_icache_ready:=pcfifo.io.icachebuf_ready
  warp_sche.io.pc_ibuffer_ready:=ibuffer.io.ibuffer_ready
  warp_sche.io.wg_id_tag:=io.wg_id_tag
  io.wg_id_lookup:=warp_sche.io.wg_id_lookup
  warp_sche.io.pc_rsp.valid:=io.icache_rsp.valid
  warp_sche.io.pc_rsp.bits:=io.icache_rsp.bits
  warp_sche.io.pc_rsp.bits.status:=Mux(ibuffer.io.in.ready,io.icache_rsp.bits.status,1.U(2.W))

  warp_sche.io.pc_req<>io.icache_req
  warp_sche.io.warp_control<>issueX.io.out_warpscheduler
  warp_sche.io.issued_warp.bits:=exe_dataX_src.bits.ctrl.wid // not used
  warp_sche.io.issued_warp.valid:=exe_dataX_src.fire // not used
  warp_sche.io.scoreboard_busy:=(VecInit(scoreb.map(_.delay))).asUInt

  csrfile.io.CTA2csr:=warp_sche.io.CTA2csr
  val init_thread_mask = (1.U(num_thread.W) << warp_sche.io.CTA2csr.bits.CTAdata.dispatch2cu_wf_size_dispatch).asUInt - 1.U
  simt_stack.io.initMask.valid := warp_sche.io.CTA2csr.valid
  simt_stack.io.initMask.bits.warp_id := warp_sche.io.CTA2csr.bits.wid
  simt_stack.io.initMask.bits.thread_mask := init_thread_mask
  when(warp_sche.io.CTA2csr.fire){
    printf(p"sm ${sm_id} warp ${Decimal(warp_sche.io.CTA2csr.bits.wid)} init thread mask 0x${Hexadecimal(init_thread_mask)}\n")
  }
  operand_collector.io.sgpr_base:=csrfile.io.sgpr_base
  operand_collector.io.vgpr_base:=csrfile.io.vgpr_base
  warp_sche.io.warpReq<>io.warpReq
  warp_sche.io.warpRsp<>io.warpRsp
  //warp_sche.io.flushDCache<>lsu.io.flush_dcache
  lsu.io.flush_dcache.valid := warp_sche.io.flushDCache.valid
   lsu.io.flush_dcache.bits := warp_sche.io.flushDCache.bits
   warp_sche.io.flushDCache.ready := lsu.io.flush_dcache.ready

  //flush:=(warp_sche.io.branch.fire&warp_sche.io.branch.bits.jump) | ()
  flush:=warp_sche.io.flush.valid

  control.io.sm_id := sm_id
  control.io.pc:=io.icache_rsp.bits.addr
  control.io.inst.zipWithIndex.foreach{ case (ins, i) =>
    ins := (io.icache_rsp.bits.data >> (xLen * i))(xLen - 1, 0)
  }
  control.io.wid:=io.icache_rsp.bits.warpid
  control.io.inst_mask:=Mux(io.icache_rsp.valid& !io.icache_rsp.bits.status(0),io.icache_rsp.bits.mask.asTypeOf(control.io.inst_mask),0.U.asTypeOf(control.io.inst_mask))
  control.io.flush_wid := warp_sche.io.flush
  control.io.ibuffer_ready := ibuffer.io.ibuffer_ready
  ibuffer.io.in.bits.control := control.io.control
  ibuffer.io.in.bits.control_mask := control.io.control_mask
  ibuffer.io.in.valid:=io.icache_rsp.valid& !io.icache_rsp.bits.status(0)
  if(MMU_ENABLED) {
    for (i <- 0 until num_fetch) {
      ibuffer.io.in.bits.control(i).asid.get := warp_sche.io.asid.get
    }
  }
  ibuffer.io.flush_wid:=warp_sche.io.flush

  // 进入ibuffer的指令不一定会被实际发射执行，这里不应检测undefined instruction
  // 例如，指令段之后的一些无用数据也可能进入ibuffer，但实际上因跳转/endprg而不会发射
  // (control.io.control zip control.io.control_mask).foreach{ case (ctrl, mask) =>
  //   when(ctrl.alu_fn === 63.U & ibuffer.io.in.valid & mask) {
  //     printf(p"sm ${sm_id} warp ${Decimal(ctrl.wid)} ")
  //     printf(p"undefined @ 0x${Hexadecimal(ctrl.pc)}: 0x${Hexadecimal(ctrl.inst)}\n")
  //   }
  //   assert (!(ctrl.alu_fn === 63.U & ibuffer.io.in.valid & mask), s"undefined instruction")
  // }



  if(SINGLE_INST){
    control.io.inst(0) := VecInit(io.inst.get.bits, 0.U(xLen.W))
    control.io.inst_mask := VecInit(1.U(1.W), 0.U(1.W))
    control.io.wid:=0.U
    io.inst.map(_.ready:=ibuffer.io.in.ready)
    ibuffer.io.in.valid:=io.inst.get.valid
    when(io.inst.get.fire) {printf(p"${Hexadecimal(control.io.inst(0))}\n")}
  }

  io.icache_rsp.ready:=ibuffer.io.in.ready

  //val ibuffer_ready=Wire(Vec(num_warp,Bool()))
  warp_sche.io.exe_busy:= VecInit(Seq.fill(num_warp)(false.B)).asUInt //~ibuffer_ready.asUInt

  for (i <- 0 until num_warp) {
    val issueCtrlBits = Wire(new CtrlSigs)
    val issueCtrlValid = Wire(Bool())
    issueCtrlBits := ibuffer.io.out(i).bits
    issueCtrlValid := ibuffer.io.out(i).valid
    ibuffer.io.out(i).ready := false.B

    if (ibufferIssuePipeCut) {
      val stage = ibufferIssueStages.get(i)
      stage.io.enq.bits := ibuffer.io.out(i).bits
      stage.io.enq.valid := ibuffer.io.out(i).valid
      stage.flush := warp_sche.io.flush.valid && (warp_sche.io.flush.bits === i.U)
      ibuffer.io.out(i).ready := stage.io.enq.ready
      issueCtrlBits := stage.io.deq.bits
      issueCtrlValid := stage.io.deq.valid
      stage.io.deq.ready := false.B
    }

    ibuffer2issue.io.in(i).bits:=issueCtrlBits
    ibuffer2issue.io.in(i).valid:=issueCtrlValid & warp_sche.io.warp_ready(i)
    if (ibufferIssuePipeCut) {
      ibufferIssueStages.get(i).io.deq.ready := ibuffer2issue.io.in(i).ready & warp_sche.io.warp_ready(i)
    } else {
      ibuffer.io.out(i).ready:=ibuffer2issue.io.in(i).ready & warp_sche.io.warp_ready(i)
    }
    if(SINGLE_INST) {
      ibuffer2issue.io.in(i).valid:=issueCtrlValid & !scoreb(i).delay
      if (ibufferIssuePipeCut) {
        ibufferIssueStages.get(i).io.deq.ready:=ibuffer2issue.io.in(i).ready & !scoreb(i).delay
      } else {
        ibuffer.io.out(i).ready:=ibuffer2issue.io.in(i).ready & !scoreb(i).delay
      }
    }
    val ctrl=issueCtrlBits
    /*ibuffer_ready(i):=Mux(ctrl.sfu,sfu.io.in.ready,
      Mux(ctrl.fp,fpu.io.in.ready,
        Mux(ctrl.csr.orR,csrfile.io.in.ready,
          Mux(ctrl.mem,lsu.io.lsu_req.ready,
            Mux(ctrl.isvec&ctrl.simt_stack&ctrl.simt_stack_op===0.U,valu.io.in.ready&simt_stack.io.branch_ctl.ready,
              Mux(ctrl.isvec&ctrl.simt_stack,simt_stack.io.branch_ctl.ready,
                Mux(ctrl.isvec,valu.io.in.ready,
                  Mux(ctrl.barrier,warp_sche.io.warp_control.ready,alu.io.in.ready))))))))*/
    //when(!ibuffer.io.out(i).valid){ibuffer_ready(i):=false.B}
    scoreb(i).ibuffer_if_ctrl:=issueCtrlBits
    scoreb(i).if_ctrl:= Mux((i.asUInt === ibuffer2issue.io.out_x.bits.wid) && ibuffer2issue.io.out_x.fire, ibuffer2issue.io.out_x.bits,ibuffer2issue.io.out_v.bits)
    scoreb(i).wb_v_ctrl:=wbOutVStage.bits
    scoreb(i).wb_x_ctrl:=wbOutXStage.bits
    scoreb(i).fence_end:=lsu.io.fence_end(i)
    scoreb(i).if_fire:=Mux(((i.asUInt===ibuffer2issue.io.out_x.bits.wid)&&ibuffer2issue.io.out_x.fire) ||
      ((i.asUInt===ibuffer2issue.io.out_v.bits.wid)&&ibuffer2issue.io.out_v.fire), true.B,false.B)
    scoreb(i).wb_v_fire:=false.B
    scoreb(i).wb_x_fire:=false.B
    scoreb(i).br_ctrl:=false.B
    scoreb(i).op_colX_in_fire:=false.B
    scoreb(i).op_colX_out_fire:=false.B
    scoreb(i).op_colV_in_fire := false.B
    scoreb(i).op_colV_out_fire := false.B

    when(warp_sche.io.branch.fire&(warp_sche.io.branch.bits.wid===i.asUInt)){scoreb(i).br_ctrl:=true.B}.
      elsewhen(warp_sche.io.warp_control.fire&(warp_sche.io.warp_control.bits.ctrl.wid===i.asUInt)){scoreb(i).br_ctrl:=true.B}.
      elsewhen(simt_stack.io.complete.valid&(simt_stack.io.complete.bits===i.asUInt)){scoreb(i).br_ctrl:=true.B}
  }
  val op_colV_in_wid = Wire(UInt(depth_warp.W))
  val op_colV_out_wid = Wire(UInt(depth_warp.W))
  val op_colX_in_wid = Wire(UInt(depth_warp.W))
  val op_colX_out_wid = Wire(UInt(depth_warp.W))
  op_colV_in_wid := operand_collector.io.controlV.bits.wid
  op_colV_out_wid := operand_collector.io.out(0).bits.control.wid
  scoreb(op_colV_in_wid).op_colV_in_fire:=operand_collector.io.controlV.fire
  scoreb(op_colV_out_wid).op_colV_out_fire:=operand_collector.io.out(0).fire

  op_colX_in_wid := operand_collector.io.controlX.bits.wid
  op_colX_out_wid := operand_collector.io.out(1).bits.control.wid
  scoreb(op_colX_in_wid).op_colX_in_fire := operand_collector.io.controlX.fire
  scoreb(op_colX_out_wid).op_colX_out_fire := operand_collector.io.out(1).fire


  scoreb(wbOutXStage.bits.warp_id).wb_x_fire:=wbOutXStage.fire
  scoreb(wbOutVStage.bits.warp_id).wb_v_fire:=wbOutVStage.fire

  // ibuffer2issue模块的IO都是实际发射但尚未执行的指令，在这检查undefined instruction
  when(ibuffer2issue.io.out_x.fire){
    val ctrl = ibuffer2issue.io.out_x.bits
    assert(ctrl.alu_fn =/= 63.U, 
           p"UNDEFINED INSTRUCTION @ SM ${sm_id} warp ${Decimal(ctrl.wid)} " + 
           p"PC 0x${Hexadecimal(ctrl.pc)}: 0x${Hexadecimal(ctrl.inst)}")
  }
  when(ibuffer2issue.io.out_v.fire){
    val ctrl = ibuffer2issue.io.out_v.bits
    assert(ctrl.alu_fn =/= 63.U, 
           p"UNDEFINED INSTRUCTION @ SM ${sm_id} warp ${Decimal(ctrl.wid)} " + 
           p"PC 0x${Hexadecimal(ctrl.pc)}: 0x${Hexadecimal(ctrl.inst)}")
  }

  operand_collector.io.controlV<>ibuffer2issue.io.out_v//ibuffer2issue.io.out.bits
  operand_collector.io.controlX<>ibuffer2issue.io.out_x//ibuffer2issue.io.out.bits
  operand_collector.io.writeVecCtrl<>wbOutVStage
  operand_collector.io.writeScalarCtrl<>wbOutXStage

  simt_stack.io.input_wid:=operand_collector.io.out(0).bits.control.wid//ibuffer2issue.io.out.bits.wid
  csrfile.io.simt_wid := issueV.io.out_SIMT.bits.wid

  when(io.icache_req.fire&(io.icache_req.bits.warpid===2.U)){
    //printf(p"wid=${io.icache_req.bits.warpid},pc=0x${Hexadecimal(io.icache_req.bits.addr)}\n")
  }
  when(io.icache_rsp.fire&(io.icache_rsp.bits.warpid===2.U)){
    //printf(p"wid=${io.icache_rsp.bits.warpid},pc=0x${Hexadecimal(io.icache_rsp.bits.addr)},inst=0x${Hexadecimal(io.icache_rsp.bits.data)}\n")
  }
  when(exe_dataX.fire&(exe_dataX.bits.ctrl.wid===2.U)){
    //printf(p"wid=${exe_dataX.bits.ctrl.wid},pc=0x${Hexadecimal(exe_dataX.bits.ctrl.pc)},inst=0x${Hexadecimal(exe_dataX.bits.ctrl.inst)}\n")
  }
  when(exe_dataV.fire&(exe_dataV.bits.ctrl.wid===2.U)) {
    //printf(p"wid=${exe_dataV.bits.ctrl.wid},pc=0x${Hexadecimal(exe_dataV.bits.ctrl.pc)},inst=0x${Hexadecimal(exe_dataV.bits.ctrl.inst)}\n")
  }


  //输出所有write mem的操作
  //val wid_to_check = 2.U //exe_data.io.deq.bits.ctrl.wid===wid_to_check&
  //  when( exe_data.io.deq.fire&exe_data.io.deq.bits.ctrl.mem_cmd===2.U){
  //    when(exe_data.io.deq.bits.ctrl.isvec){
  //      printf(p"warp${exe_data.io.deq.bits.ctrl.wid} 0x${Hexadecimal(exe_data.io.deq.bits.ctrl.pc)} 0x${Hexadecimal(exe_data.io.deq.bits.ctrl.inst)} w v${exe_data.io.deq.bits.ctrl.reg_idx3} ")
  //      exe_data.io.deq.bits.in3.reverse.foreach(x => printf(p"${Hexadecimal(x.asUInt)} "))
  //      printf(p"mask ${Binary(exe_data.io.deq.bits.mask.asUInt)} @")
  //      (exe_data.io.deq.bits.in1 zip exe_data.io.deq.bits.in2).reverse.foreach(x => printf(p" ${Hexadecimal(x._1)}+${Hexadecimal(x._2)}"))
  //      printf("\n")
  //    }.otherwise{
  //      printf(p"warp${exe_data.io.deq.bits.ctrl.wid} 0x${Hexadecimal(exe_data.io.deq.bits.ctrl.pc)} 0x${Hexadecimal(exe_data.io.deq.bits.ctrl.inst)} w x${exe_data.io.deq.bits.ctrl.reg_idxw} ")
  //      printf(p"${Hexadecimal(exe_data.io.deq.bits.in3(0))} ")
  //      printf(p"@ ${Hexadecimal(exe_data.io.deq.bits.in1(0))}+${Hexadecimal(exe_data.io.deq.bits.in2(0))}\n")
  //    }
  //  }
  //输出所有发射的指令
  //when( exe_data.io.deq.fire){
  //  printf(p"${exe_data.io.deq.bits.ctrl.wid},0x${Hexadecimal(exe_data.io.deq.bits.ctrl.pc)},writedata=")
  //  //exe_data.io.deq.bits.in3.foreach(x=>{printf(p"${Hexadecimal(x.asUInt)} ")})
  //  printf(p"mask ${exe_data.io.deq.bits.mask} with${Hexadecimal(exe_data.io.deq.bits.in1(0))},${Hexadecimal(exe_data.io.deq.bits.in2(0))}")
  //  printf(p"\n")
  //}

  //输出特定指令的操作数
  //when((exe_data.io.deq.bits.ctrl.wid===wid_to_check)& exe_data.io.deq.fire ){
  //    printf(p"0x${Hexadecimal(exe_data.io.deq.bits.ctrl.pc+0x348.U-0xc.U) },${exe_data.io.deq.bits.ctrl.wid} operand is =")
  //  exe_data.io.deq.bits.in2.foreach(x=>{printf(p"${Hexadecimal(x.asUInt)} ")})
  //  exe_data.io.deq.bits.in1.foreach(x=>{printf(p"${Hexadecimal(x.asUInt)} ")})
  //  exe_data.io.deq.bits.in3.foreach(x=>{printf(p"${Hexadecimal(x.asUInt)} ")})
  //  exe_data.io.deq.bits.mask.foreach(x=>{printf(p"${Hexadecimal(x.asUInt)} ")})
  //  printf(p"\n")
  //}
  //输出写入向量寄存器的
  //  when(wb.io.out_v.fire&wb.io.out_v.bits.warp_id===wid_to_check){
  //    printf(p"write v${wb.io.out_v.bits.reg_idxw} ")
  //    wb.io.out_v.bits.wb_wvd_rd.foreach(x=>printf(p"${Hexadecimal(x.asUInt)} "))
  //    printf(p"mask ${wb.io.out_v.bits.wvd_mask}\n")
  //  }
  //  ////输出写入标量寄存器的
  //  when(wb.io.out_x.fire&wb.io.out_x.bits.warp_id===wid_to_check){
  //    printf(p"write x${wb.io.out_x.bits.reg_idxw} 0x${Hexadecimal(wb.io.out_x.bits.wb_wxd_rd)}\n")
  //  }

  {
    exe_dataX_src.bits.ctrl := operand_collector.io.out(1).bits.control
    exe_dataX_src.bits.in1 := operand_collector.io.out(1).bits.alu_src1
    exe_dataX_src.bits.in2 := operand_collector.io.out(1).bits.alu_src2
    exe_dataX_src.bits.in3 := operand_collector.io.out(1).bits.alu_src3
    exe_dataX_src.bits.mask.foreach(_ := true.B)
    exe_dataX_src.valid:=operand_collector.io.out(1).valid
    operand_collector.io.out(1).ready := exe_dataX_src.ready

    exe_dataV_src.bits.ctrl := operand_collector.io.out(0).bits.control
    exe_dataV_src.bits.in1 := operand_collector.io.out(0).bits.alu_src1
    exe_dataV_src.bits.in2 := operand_collector.io.out(0).bits.alu_src2
    exe_dataV_src.bits.in3 := operand_collector.io.out(0).bits.alu_src3
    exe_dataV_src.bits.mask := (operand_collector.io.out(0).bits.mask.zipWithIndex.map { case (x, y) => x & simt_stack.io.out_mask(y) })
    exe_dataV_src.valid := operand_collector.io.out(0).valid
    operand_collector.io.out(0).ready := exe_dataV_src.ready
  }
  //  exe_acq_reg.io.deq.ready:=exe_data.io.enq.ready//ibuffer2issue.io.out.ready:=exe_data.io.enq.ready
  issueV.io.in<>exe_dataV
  issueX.io.in<>exe_dataX

  issueV.io.out_vALU<>valu.io.in
  issueX.io.out_vALU.ready := false.B
  issueV.io.out_LSU<>lsu.io.lsu_req
  issueX.io.out_LSU.ready := false.B
  issueX.io.out_sALU<>alu.io.in
  issueV.io.out_sALU.ready := false.B
  csrfile.io.in <> csrIssueStage
  issueV.io.out_CSR.ready := false.B
  simtStageSrc.valid := issueV.io.out_SIMT.valid
  simtStageSrc.bits.branch := issueV.io.out_SIMT.bits
  simtStageSrc.bits.rpc := csrfile.io.simt_rpc
  issueV.io.out_SIMT.ready := simtStageSrc.ready
  simt_stack.io.branch_ctl.valid := simtStage.valid
  simt_stack.io.branch_ctl.bits := simtStage.bits.branch
  simt_stack.io.pc_reconv.valid := simtStage.valid
  simt_stack.io.pc_reconv.bits := simtStage.bits.rpc
  simtStage.ready := simt_stack.io.branch_ctl.ready && simt_stack.io.pc_reconv.ready
  issueX.io.out_SIMT.ready := false.B
  issueV.io.out_SFU<>sfu.io.in
  issueX.io.out_SFU.ready := false.B
  //simt_stack.io.branch_ctl<>Queue(issue.io.out_SIMT,1,flow = true)
  simt_stack.io.if_mask<>valu.io.out2simt_stack
  simt_stack.io.fetch_ctl<>branch_back.io.in1

  alu.io.out2br<>branch_back.io.in0

  issueV.io.out_MUL<>mul.io.in
  issueX.io.out_MUL.ready := false.B
  tensorcore.io.in <> tensorCoreInputStage
  issueX.io.out_TC.ready := false.B
  fpu.io.in <> fpuInputStage
  issueX.io.out_vFPU.ready := false.B
  issueX.io.out_warpscheduler <> warp_sche.io.warp_control
  issueV.io.out_warpscheduler.ready := false.B

  fpu.io.rm := Mux(fpu.io.in.bits.ctrl.force_rm_rtz, RoundingMode.RTZ, csrfile.io.rm(0))
  csrfile.io.rm_wid(0):=fpu.io.in.bits.ctrl.wid
  sfu.io.rm := csrfile.io.rm(1)
  csrfile.io.rm_wid(1):=sfu.io.in.bits.ctrl.wid
  tensorcore.io.rm := csrfile.io.rm(2)
  csrfile.io.rm_wid(2):=tensorcore.io.in.bits.ctrl.wid

  lsu.io.dcache_rsp<>io.dcache_rsp
  lsu.io.dcache_req<>io.dcache_req
  lsu.io.lsu_rsp<>lsu2wb.io.lsu_rsp
  lsu.io.shared_rsp<>io.shared_rsp
  lsu.io.shared_req<>io.shared_req

  wb.io.in_x(0)<>alu.io.out
  wb.io.in_x(1)<>fpu.io.out_x
  wb.io.in_x(2)<>lsu2wb.io.out_x
  wb.io.in_x(3)<>csrfile.io.out
  wb.io.in_x(4)<>sfu.io.out_x
  wb.io.in_x(5)<>mul.io.out_x
  wb.io.in_v(0)<>valu.io.out
  wb.io.in_v(1)<>fpu.io.out_v
  wb.io.in_v(2)<>lsu2wb.io.out_v
  wb.io.in_v(3)<>sfu.io.out_v
  wb.io.in_v(4)<>mul.io.out_v
  wb.io.in_v(5)<>tensorCoreWbStage
  wb.io.in_v(6)<>csrfile.io.out_v

  issue_stall:=(~issueX.io.in.ready).asBool | (~issueV.io.in.ready).asBool//scoreb.io.delay | issue.io.in.ready
}
