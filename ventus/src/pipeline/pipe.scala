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
import rtcore._

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

class pipe() extends Module{
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
    val perfEnable = Input(Bool())
    val perfReset = Input(Bool())
    val perf_pipeline = if(PMU_PIPELINE) Some(Output(new PipelinePerfCounters)) else None
    val perf_inst_class = if(PMU_INST_CLASS) Some(Output(new InstClassPerfCounters)) else None
    val perf_rtcore = Output(new RtCorePerfCounters)
  })
  val issue_stall=Wire(Bool())
  val flush=Wire(Bool())


  val warp_sche=Module(new warp_scheduler)
  //val pcfifo=Module(new PCfifo)
  val control=Module(new InstrDecodeV2)
  control.io.sm_id := sm_id
  val operand_collector=Module(new operandCollector)
  if (GVM_ENABLED) {
    val gvm_xreg_init = Module(new GvmDutWarpXRegInit)
    val gvm_vreg_init = Module(new GvmDutWarpVRegInit)
    gvm_xreg_init.io.clock := clock
    gvm_xreg_init.io.reset := reset.asBool
    gvm_xreg_init.io.fire := io.warpReq.fire
    gvm_xreg_init.io.sm_id := sm_id
    gvm_xreg_init.io.hardware_warp_id := io.warpReq.bits.wid.pad(32)
    gvm_xreg_init.io.xregs := operand_collector.io.gvmWarpXRegs.get.asUInt
    gvm_vreg_init.io.clock := clock
    gvm_vreg_init.io.reset := reset.asBool
    gvm_vreg_init.io.fire := io.warpReq.fire
    gvm_vreg_init.io.sm_id := sm_id
    gvm_vreg_init.io.hardware_warp_id := io.warpReq.bits.wid.pad(32)
    gvm_vreg_init.io.vregs := operand_collector.io.gvmWarpVRegs.get.asUInt

    operand_collector.io.gvmWarpHwId.get := io.warpReq.bits.wid
    operand_collector.io.gvmWarpSgprBase.get := io.warpReq.bits.CTAdata.dispatch2cu_sgpr_base_dispatch
    operand_collector.io.gvmWarpVgprBase.get := io.warpReq.bits.CTAdata.dispatch2cu_vgpr_base_dispatch
  }
  //val issue=Module(new Issue)
  val issueX = Module(new Issue)
  val issueV = Module(new Issue)
  val alu=Module(new ALUexe)
  val valu=Module(new vALUv2(num_thread, num_lane))
  val fpu=Module(new FPUexe(num_thread,num_lane))
  val lsu=Module(new LSUexe)
  val sfu=Module(new SFUexe)
  val mul=Module(new vMULv2(num_thread,num_lane))
  val tensorcore=Module(new vTCexe)
  val lsu2wb=Module(new LSU2WB)
  val wb=Module(new Writeback(6,9))
  val rtcore = Module(new RtCore)
  val rtLocalSram = Module(new RtLocalSram)
  val rtWriteback = Module(new Queue(new WriteVecCtrl, 1, pipe = true))
  val rtLocalWriteback = Module(new Queue(new WriteVecCtrl, 1, pipe = true))
  val rtInFlight = RegInit(VecInit(Seq.fill(num_warp)(false.B)))
  val rtIssueId = RegInit(VecInit(Seq.fill(num_warp)(0.U(RtCorePortParameters.issueIdWidth.W))))
  val rtCompletionAwaitingWb = RegInit(false.B)
  val rtCompletionWarp = Reg(UInt(depth_warp.W))
  val rtCompletionDestination = Reg(UInt(RtCorePortParameters.regidx_totalwidth.W))
  val rtCompletionContextReleased = RegInit(false.B)
  val rtReleaseSent = RegInit(false.B)
  // Reserve at operand-collector admission. A foreign traversal admitted
  // before this reservation is visible can later block the owner's RT path.
  val rtReservation = Module(new RtSessionReservation)

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
  val exe_dataX=Module(new Module{
    val io = IO(new Bundle{
      val enq = Flipped(DecoupledIO(new vExeData))
      val deq = DecoupledIO(Output(new vExeData))
    })
    io.deq <> io.enq
  })
  val exe_dataV = Module(new Module {
    val io = IO(new Bundle {
      val enq = Flipped(DecoupledIO(new vExeData))
      val deq = DecoupledIO(Output(new vExeData))
    })
    io.deq <> io.enq
  })
  val simt_stack=Module(new branch_join(num_thread))
  val branch_back=Module(new Branch_back)
  val csrfile=Module(new CSRexe())

  io.externalFlushPipe.valid:=warp_sche.io.flush.valid|warp_sche.io.flushCache.valid
  io.externalFlushPipe.bits:=Mux(warp_sche.io.flush.valid,warp_sche.io.flush.bits,warp_sche.io.flushCache.bits)

  csrfile.io.lsu_wid:=lsu.io.csr_wid
  lsu.io.csr_pds:=csrfile.io.lsu_pds
  lsu.io.csr_tid:=csrfile.io.lsu_tid
  lsu.io.csr_numw:=csrfile.io.lsu_numw
  lsu.io.csr_numt:=csrfile.io.lsu_numt
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
  warp_sche.io.issued_warp.bits:=exe_dataX.io.enq.bits.ctrl.wid // not used
  warp_sche.io.issued_warp.valid:=exe_dataX.io.enq.fire // not used
  val scoreboardBusy = (VecInit(scoreb.map(_.delay))).asUInt
  warp_sche.io.scoreboard_busy:=scoreboardBusy

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
  warp_sche.io.flushDCache <> lsu.io.flush_dcache

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
    val rtAdmissionAllowed = ibuffer.io.out(i).bits.rtOp =/= RtOp.traverse ||
      rtReservation.io.allowed(i)
    ibuffer2issue.io.in(i).bits:=ibuffer.io.out(i).bits
    ibuffer2issue.io.in(i).valid:=ibuffer.io.out(i).valid & warp_sche.io.warp_ready(i) & rtAdmissionAllowed
    ibuffer.io.out(i).ready:=ibuffer2issue.io.in(i).ready & warp_sche.io.warp_ready(i) & rtAdmissionAllowed
    if(SINGLE_INST) {ibuffer2issue.io.in(i).valid:=ibuffer.io.out(i).valid & !scoreb(i).delay & rtAdmissionAllowed
      ibuffer.io.out(i).ready:=ibuffer2issue.io.in(i).ready & !scoreb(i).delay & rtAdmissionAllowed}
    val ctrl=ibuffer.io.out(i).bits
    /*ibuffer_ready(i):=Mux(ctrl.sfu,sfu.io.in.ready,
      Mux(ctrl.fp,fpu.io.in.ready,
        Mux(ctrl.csr.orR,csrfile.io.in.ready,
          Mux(ctrl.mem,lsu.io.lsu_req.ready,
            Mux(ctrl.isvec&ctrl.simt_stack&ctrl.simt_stack_op===0.U,valu.io.in.ready&simt_stack.io.branch_ctl.ready,
              Mux(ctrl.isvec&ctrl.simt_stack,simt_stack.io.branch_ctl.ready,
                Mux(ctrl.isvec,valu.io.in.ready,
                  Mux(ctrl.barrier,warp_sche.io.warp_control.ready,alu.io.in.ready))))))))*/
    //when(!ibuffer.io.out(i).valid){ibuffer_ready(i):=false.B}
    scoreb(i).ibuffer_if_ctrl:=ibuffer.io.out(i).bits
    scoreb(i).if_ctrl:= Mux((i.asUInt === ibuffer2issue.io.out_x.bits.wid) && ibuffer2issue.io.out_x.fire, ibuffer2issue.io.out_x.bits,ibuffer2issue.io.out_v.bits)
    scoreb(i).wb_v_ctrl:=wb.io.out_v.bits
    scoreb(i).wb_x_ctrl:=wb.io.out_x.bits
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
  // This boundary is intentionally before operand collection: issuing into
  // Issue is too late, because a foreign traversal can already be buffered
  // upstream. The lane mask is resolved at RT issue below.
  rtReservation.io.issued.valid := ibuffer2issue.io.out_v.fire &&
    ibuffer2issue.io.out_v.bits.rtOp === RtOp.traverse
  rtReservation.io.issued.bits := ibuffer2issue.io.out_v.bits.wid
  rtReservation.io.releaseIssued.valid := rtcore.io.release.fire
  rtReservation.io.releaseIssued.bits := rtcore.io.release.bits.warpId
  rtReservation.io.released.valid := rtcore.io.releaseAck.fire
  rtReservation.io.released.bits := rtcore.io.releaseAck.bits.warpId
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


  scoreb(wb.io.out_x.bits.warp_id).wb_x_fire:=wb.io.out_x.fire
  scoreb(wb.io.out_v.bits.warp_id).wb_v_fire:=wb.io.out_v.fire

  // ibuffer2issue模块的IO都是实际发射但尚未执行的指令，在这检查undefined instruction
  when(ibuffer2issue.io.out_x.fire){
    val ctrl = ibuffer2issue.io.out_x.bits
    assert(ctrl.alu_fn =/= 63.U || ctrl.rtOp.orR,
           p"UNDEFINED INSTRUCTION @ SM ${sm_id} warp ${Decimal(ctrl.wid)} " + 
           p"PC 0x${Hexadecimal(ctrl.pc)}: 0x${Hexadecimal(ctrl.inst)}")
  }
  when(ibuffer2issue.io.out_v.fire){
    val ctrl = ibuffer2issue.io.out_v.bits
    assert(ctrl.alu_fn =/= 63.U || ctrl.rtOp.orR,
           p"UNDEFINED INSTRUCTION @ SM ${sm_id} warp ${Decimal(ctrl.wid)} " + 
           p"PC 0x${Hexadecimal(ctrl.pc)}: 0x${Hexadecimal(ctrl.inst)}")
  }
  operand_collector.io.controlV<>ibuffer2issue.io.out_v//ibuffer2issue.io.out.bits
  operand_collector.io.controlX<>ibuffer2issue.io.out_x//ibuffer2issue.io.out.bits
  operand_collector.io.writeVecCtrl<>wb.io.out_v
  operand_collector.io.writeScalarCtrl<>wb.io.out_x

  simt_stack.io.input_wid:=operand_collector.io.out(0).bits.control.wid//ibuffer2issue.io.out.bits.wid
  csrfile.io.simt_wid := operand_collector.io.out(0).bits.control.wid // todo check this

  when(io.icache_req.fire&(io.icache_req.bits.warpid===2.U)){
    //printf(p"wid=${io.icache_req.bits.warpid},pc=0x${Hexadecimal(io.icache_req.bits.addr)}\n")
  }
  when(io.icache_rsp.fire&(io.icache_rsp.bits.warpid===2.U)){
    //printf(p"wid=${io.icache_rsp.bits.warpid},pc=0x${Hexadecimal(io.icache_rsp.bits.addr)},inst=0x${Hexadecimal(io.icache_rsp.bits.data)}\n")
  }
  when(exe_dataX.io.deq.fire&(exe_dataX.io.deq.bits.ctrl.wid===2.U)){
    //printf(p"wid=${exe_dataX.io.deq.bits.ctrl.wid},pc=0x${Hexadecimal(exe_dataX.io.deq.bits.ctrl.pc)},inst=0x${Hexadecimal(exe_dataX.io.deq.bits.ctrl.inst)}\n")
  }
  when(exe_dataV.io.deq.fire&(exe_dataV.io.deq.bits.ctrl.wid===2.U)) {
    //printf(p"wid=${exe_dataV.io.deq.bits.ctrl.wid},pc=0x${Hexadecimal(exe_dataV.io.deq.bits.ctrl.pc)},inst=0x${Hexadecimal(exe_dataV.io.deq.bits.ctrl.inst)}\n")
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
    exe_dataX.io.enq.bits.ctrl := operand_collector.io.out(1).bits.control
    exe_dataX.io.enq.bits.in1 := operand_collector.io.out(1).bits.alu_src1
    exe_dataX.io.enq.bits.in2 := operand_collector.io.out(1).bits.alu_src2
    exe_dataX.io.enq.bits.in3 := operand_collector.io.out(1).bits.alu_src3
    exe_dataX.io.enq.bits.mask.foreach(_ := true.B)
    exe_dataX.io.enq.valid:=operand_collector.io.out(1).valid
    operand_collector.io.out(1).ready := exe_dataX.io.enq.ready

    exe_dataV.io.enq.bits.ctrl := operand_collector.io.out(0).bits.control
    exe_dataV.io.enq.bits.in1 := operand_collector.io.out(0).bits.alu_src1
    exe_dataV.io.enq.bits.in2 := operand_collector.io.out(0).bits.alu_src2
    exe_dataV.io.enq.bits.in3 := operand_collector.io.out(0).bits.alu_src3
    exe_dataV.io.enq.bits.mask := (operand_collector.io.out(0).bits.mask.zipWithIndex.map { case (x, y) => x & simt_stack.io.out_mask(y) })
    exe_dataV.io.enq.valid := operand_collector.io.out(0).valid
    operand_collector.io.out(0).ready := exe_dataV.io.enq.ready
  }
  //  exe_acq_reg.io.deq.ready:=exe_data.io.enq.ready//ibuffer2issue.io.out.ready:=exe_data.io.enq.ready
  issueV.io.in<>exe_dataV.io.deq
  issueX.io.in<>exe_dataX.io.deq

  issueV.io.out_vALU<>valu.io.in
  issueX.io.out_vALU.ready := false.B
  issueV.io.out_LSU<>lsu.io.lsu_req
  issueX.io.out_LSU.ready := false.B
  issueX.io.out_sALU<>alu.io.in
  issueV.io.out_sALU.ready := false.B
  issueX.io.out_CSR<>csrfile.io.in
  issueV.io.out_CSR.ready := false.B
  issueV.io.out_SIMT<>simt_stack.io.branch_ctl
  issueX.io.out_SIMT.ready := false.B
  issueV.io.out_SFU<>sfu.io.in
  issueX.io.out_SFU.ready := false.B
  //simt_stack.io.branch_ctl<>Queue(issue.io.out_SIMT,1,flow = true)
  simt_stack.io.if_mask<>valu.io.out2simt_stack
  simt_stack.io.fetch_ctl<>branch_back.io.in1
  simt_stack.io.pc_reconv.bits := csrfile.io.simt_rpc
  simt_stack.io.pc_reconv.valid := issueV.io.out_SIMT.valid//true.B //todo check this

  alu.io.out2br<>branch_back.io.in0

  issueV.io.out_MUL<>mul.io.in
  issueX.io.out_MUL.ready := false.B
  issueV.io.out_TC<>tensorcore.io.in
  issueX.io.out_TC.ready := false.B
  issueV.io.out_vFPU<>fpu.io.in
  issueX.io.out_vFPU.ready := false.B
  issueX.io.out_RT.ready := false.B
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
  // RTcore is the generic cacheline client; its private MSHR remains behind
  // this single LSU boundary.
  lsu.io.cachelineClientReq <> rtcore.io.lsuRequest
  rtcore.io.lsuResponse <> lsu.io.cachelineClientRsp
  val rtLocalResponseToCore = RegInit(false.B)
  val rtLocalReadWarp = Reg(UInt(depth_warp.W))
  val rtLocalReadDestination = Reg(UInt(RtCorePortParameters.regidx_totalwidth.W))
  val rtLocalReadMask = Reg(UInt(num_thread.W))

  val rtIssue = issueV.io.out_RT.bits
  val rtIsTraverse = rtIssue.ctrl.rtOp === RtOp.traverse
  val rtIsRelease = rtIssue.ctrl.rtOp === RtOp.release
  val rtIsLocalLoad = rtIssue.ctrl.rtOp === RtOp.localLoad
  val rtIsLocalStore = rtIssue.ctrl.rtOp === RtOp.localStore
  // RT instructions remain in the scalar instruction stream even when SIMT
  // control has disabled every lane.  They have no RT-local side effect in
  // that case: in particular, do not create (or try to retire) a session.
  val rtIssueMask = rtIssue.mask.asUInt
  val rtTraverseEmpty = rtIsTraverse && !rtIssueMask.orR
  val rtReleaseEmpty = rtIsRelease && !rtIssueMask.orR
  // A reconverged release can have live lanes even though its matching
  // traverse was empty and therefore deliberately never opened a session.
  // Releasing that non-existent session would hold the RT issue entry forever:
  // RTcore correctly refuses an acknowledgement without an owner.
  // A software session can outlive its physical RT context. Its final release
  // must not touch another warp's newly reserved/executing context.
  val rtReleaseNoSession = rtIsRelease &&
    (!rtReservation.io.reserved || rtReservation.io.owner =/= rtIssue.ctrl.wid)
  val rtReleaseSkipped = rtReleaseEmpty || rtReleaseNoSession
  rtReservation.io.traverseResolved.valid := issueV.io.out_RT.fire && rtIsTraverse
  rtReservation.io.traverseResolved.bits.warpId := rtIssue.ctrl.wid
  rtReservation.io.traverseResolved.bits.wasEmpty := rtTraverseEmpty
  val softwareLocalRequest = Wire(Decoupled(new RtLocalSramRequest))
  softwareLocalRequest.valid := issueV.io.out_RT.valid && (rtIsLocalLoad || rtIsLocalStore) &&
    !rtInFlight(rtIssue.ctrl.wid)
  softwareLocalRequest.bits := 0.U.asTypeOf(new RtLocalSramRequest)
  softwareLocalRequest.bits.warpId := rtIssue.ctrl.wid
  softwareLocalRequest.bits.fieldId := rtIssue.ctrl.inst(31, 26)
  softwareLocalRequest.bits.write := rtIsLocalStore
  softwareLocalRequest.bits.laneMask := rtIssue.mask.asUInt
  softwareLocalRequest.bits.writeData := rtIssue.in2

  rtLocalSram.io.request.valid := rtcore.io.localRequest.valid || softwareLocalRequest.valid
  rtLocalSram.io.request.bits := Mux(rtcore.io.localRequest.valid,
    rtcore.io.localRequest.bits, softwareLocalRequest.bits)
  rtcore.io.localRequest.ready := rtLocalSram.io.request.ready
  softwareLocalRequest.ready := !rtcore.io.localRequest.valid && rtLocalSram.io.request.ready
  when(rtLocalSram.io.request.fire && !rtLocalSram.io.request.bits.write) {
    rtLocalResponseToCore := rtcore.io.localRequest.valid
    when(!rtcore.io.localRequest.valid) {
      rtLocalReadWarp := rtIssue.ctrl.wid
      rtLocalReadDestination := rtIssue.ctrl.reg_idxw
      rtLocalReadMask := rtIssue.mask.asUInt
    }
  }
  rtcore.io.localResponse.valid := rtLocalSram.io.response.valid && rtLocalResponseToCore
  rtcore.io.localResponse.bits := rtLocalSram.io.response.bits
  rtLocalWriteback.io.enq.valid := rtLocalSram.io.response.valid && !rtLocalResponseToCore
  rtLocalWriteback.io.enq.bits := 0.U.asTypeOf(new WriteVecCtrl)
  rtLocalWriteback.io.enq.bits.warp_id := rtLocalReadWarp
  rtLocalWriteback.io.enq.bits.reg_idxw := rtLocalReadDestination
  rtLocalWriteback.io.enq.bits.wvd := true.B
  rtLocalWriteback.io.enq.bits.wvd_mask := rtLocalReadMask.asBools
  rtLocalWriteback.io.enq.bits.wb_wvd_rd := rtLocalSram.io.response.bits.readData
  if (SPIKE_OUTPUT) { rtLocalWriteback.io.enq.bits.spike_info.get := 0.U.asTypeOf(new InstWriteBack) }
  rtLocalSram.io.response.ready := Mux(rtLocalResponseToCore,
    rtcore.io.localResponse.ready, rtLocalWriteback.io.enq.ready)
  if (sys.env.get("VENTUS_RTLOCAL_TRACE").contains("1")) {
    when(rtLocalSram.io.request.fire) {
      printf(p"[RTLOCAL trace] request core=${Decimal(rtcore.io.localRequest.valid)}" +
        p" warp=${Decimal(rtLocalSram.io.request.bits.warpId)}" +
        p" field=${Decimal(rtLocalSram.io.request.bits.fieldId)}" +
        p" write=${Decimal(rtLocalSram.io.request.bits.write)}" +
        p" mask=0x${Hexadecimal(rtLocalSram.io.request.bits.laneMask)}" +
        p" lane0=0x${Hexadecimal(rtLocalSram.io.request.bits.writeData(0))}\n")
      // Reflections diverges only after a sparse recursive ray rewrites its
      // ray-state.  Keep this diagnostic narrow: record the actual values of
      // active image lanes, not every 32-lane transaction or RTcore flush.
      when(!rtcore.io.localRequest.valid && rtLocalSram.io.request.bits.write &&
        rtLocalSram.io.request.bits.fieldId >= 5.U && rtLocalSram.io.request.bits.fieldId <= 15.U) {
        for (lane <- 1 until 16) {
          when(rtLocalSram.io.request.bits.laneMask(lane)) {
            printf(p"[RTLOCAL ray-write] warp=${Decimal(rtLocalSram.io.request.bits.warpId)}" +
              p" field=${Decimal(rtLocalSram.io.request.bits.fieldId)} lane=${Decimal(lane.U)}" +
              p" value=0x${Hexadecimal(rtLocalSram.io.request.bits.writeData(lane))}\n")
          }
        }
      }
    }
    when(rtLocalSram.io.response.fire) {
      printf(p"[RTLOCAL trace] response core=${Decimal(rtLocalResponseToCore)}" +
        p" warp=${Decimal(rtLocalReadWarp)}" +
        p" lane0=0x${Hexadecimal(rtLocalSram.io.response.bits.readData(0))}\n")
    }
  }
  // release is a lifecycle barrier: software TERMINATED stores for this warp
  // must complete through the legacy LSU before RTcore may reclaim its state.
  val rtReleaseStoreDrained = lsu.io.fence_end(rtIssue.ctrl.wid)
  rtcore.io.command.valid := issueV.io.out_RT.valid && rtIsTraverse &&
    !rtTraverseEmpty && !rtInFlight(rtIssue.ctrl.wid)
  rtcore.io.command.bits.warpId := rtIssue.ctrl.wid
  rtcore.io.command.bits.issueId := rtIssueId(rtIssue.ctrl.wid)
  rtcore.io.command.bits.destinationVd := rtIssue.ctrl.reg_idxw
  rtcore.io.command.bits.activeMask := rtIssue.mask.asUInt

  // A release must not retire before the RTcore has actually reclaimed its
  // retained context.  Hold the issue entry after its one-time submission.
  rtcore.io.release.valid := issueV.io.out_RT.valid && rtIsRelease &&
    !rtReleaseSkipped && !rtReleaseSent && !rtInFlight(rtIssue.ctrl.wid) && rtReleaseStoreDrained
  rtcore.io.release.bits.warpId := rtIssue.ctrl.wid
  rtcore.io.release.bits.activeMask := rtIssue.mask.asUInt
  rtcore.io.releaseAck.ready := rtReleaseSent && issueV.io.out_RT.valid && rtIsRelease
  issueV.io.out_RT.ready := Mux(rtIsTraverse,
    rtTraverseEmpty || (!rtInFlight(rtIssue.ctrl.wid) && rtcore.io.command.ready),
    Mux(rtIsRelease, Mux(rtReleaseSkipped,
      rtReleaseEmpty || (!rtInFlight(rtIssue.ctrl.wid) && rtReleaseStoreDrained),
      rtReleaseSent && rtcore.io.releaseAck.valid),
      Mux(rtIsLocalLoad || rtIsLocalStore, softwareLocalRequest.ready, false.B)))
  when(rtcore.io.command.fire) {
    val wid = rtcore.io.command.bits.warpId
    if (sys.env.get("VENTUS_RTCORE_TRACE").contains("1")) {
      printf(p"[RTCORE trace] command sm=${Decimal(sm_id)} warp=${Decimal(wid)}" +
        p" issue=${Decimal(rtcore.io.command.bits.issueId)}" +
        p" vd=${Decimal(rtcore.io.command.bits.destinationVd)}" +
        p" mask=0x${Hexadecimal(rtcore.io.command.bits.activeMask)}\n")
    }
    rtInFlight(wid) := true.B
    rtIssueId(wid) := rtIssueId(wid) + 1.U
  }
  when(rtcore.io.release.fire) {
    rtReleaseSent := true.B
  }
  when(rtcore.io.releaseAck.fire) {
    assert(rtcore.io.releaseAck.bits.warpId === rtIssue.ctrl.wid,
      "RT release acknowledgement must retire the reserved issue warp")
    rtReleaseSent := false.B
  }

  rtWriteback.io.enq.valid := rtcore.io.completion.valid && !rtCompletionAwaitingWb
  rtWriteback.io.enq.bits := 0.U.asTypeOf(new WriteVecCtrl)
  rtWriteback.io.enq.bits.warp_id := rtcore.io.completion.bits.warpId
  rtWriteback.io.enq.bits.reg_idxw := rtcore.io.completion.bits.destinationVd
  rtWriteback.io.enq.bits.wvd := true.B
  rtWriteback.io.enq.bits.wvd_mask := rtcore.io.completion.bits.completionMask.asBools
  rtWriteback.io.enq.bits.wb_wvd_rd := rtcore.io.completion.bits.laneStatus
  if (SPIKE_OUTPUT) { rtWriteback.io.enq.bits.spike_info.get := 0.U.asTypeOf(new InstWriteBack) }
  rtcore.io.completion.ready := rtWriteback.io.enq.ready && !rtCompletionAwaitingWb
  when(rtcore.io.completion.fire) {
    if (sys.env.get("VENTUS_RTCORE_TRACE").contains("1")) {
      val hitMask = VecInit((0 until num_thread).map(lane =>
        rtcore.io.completion.bits.laneStatus(lane) === RtAbiLayout.TraversalCompleteHit.U)).asUInt
      val nonOpaqueMask = VecInit((0 until num_thread).map(lane =>
        rtcore.io.completion.bits.laneStatus(lane) === RtAbiLayout.TraversalCandidateNonOpaqueTriangle.U)).asUInt
      val proceduralMask = VecInit((0 until num_thread).map(lane =>
        rtcore.io.completion.bits.laneStatus(lane) === RtAbiLayout.TraversalCandidateProceduralAabb.U)).asUInt
      val terminatedMask = VecInit((0 until num_thread).map(lane =>
        rtcore.io.completion.bits.laneStatus(lane) === RtAbiLayout.TraversalTerminated.U)).asUInt
      printf(p"[RTCORE trace] completion sm=${Decimal(sm_id)}" +
        p" warp=${Decimal(rtcore.io.completion.bits.warpId)}" +
        p" issue=${Decimal(rtcore.io.completion.bits.issueId)}" +
        p" vd=${Decimal(rtcore.io.completion.bits.destinationVd)}" +
        p" contextReleased=${Decimal(rtcore.io.completion.bits.contextReleased)}" +
        p" mask=0x${Hexadecimal(rtcore.io.completion.bits.completionMask)}" +
        p" hit=0x${Hexadecimal(hitMask)} nonopaque=0x${Hexadecimal(nonOpaqueMask)}" +
        p" procedural=0x${Hexadecimal(proceduralMask)} terminated=0x${Hexadecimal(terminatedMask)}\n")
    }
    if (sys.env.get("VENTUS_RTLOCAL_TRACE").contains("1")) {
      printf(p"[RTLOCAL trace] completion warp=${Decimal(rtcore.io.completion.bits.warpId)}" +
        p" vd=${Decimal(rtcore.io.completion.bits.destinationVd)}" +
        p" mask0=${Decimal(rtcore.io.completion.bits.completionMask(0))}" +
        p" status0=0x${Hexadecimal(rtcore.io.completion.bits.laneStatus(0))}\n")
    }
    rtCompletionAwaitingWb := true.B
    rtCompletionWarp := rtcore.io.completion.bits.warpId
    rtCompletionDestination := rtcore.io.completion.bits.destinationVd
    rtCompletionContextReleased := rtcore.io.completion.bits.contextReleased
  }

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
  wb.io.in_v(5)<>tensorcore.io.out_v
  wb.io.in_v(6)<>csrfile.io.out_v
  wb.io.in_v(7)<>rtWriteback.io.deq
  wb.io.in_v(8)<>rtLocalWriteback.io.deq
  // Observe the actual RT writeback input, not a coincidentally matching
  // destination on another vector writer. No new owner enters before this.
  val rtCompletionWriteback = rtWriteback.io.deq.fire && rtCompletionAwaitingWb
  rtReservation.io.completed.valid := rtCompletionWriteback
  rtReservation.io.completed.bits.warpId := rtCompletionWarp
  rtReservation.io.completed.bits.contextReleased := rtCompletionContextReleased
  when(rtCompletionWriteback) {
    assert(rtWriteback.io.deq.bits.warp_id === rtCompletionWarp &&
      rtWriteback.io.deq.bits.reg_idxw === rtCompletionDestination)
    if (sys.env.get("VENTUS_RTLOCAL_TRACE").contains("1")) {
      printf(p"[RTLOCAL trace] completion-wb warp=${Decimal(wb.io.out_v.bits.warp_id)}" +
        p" vd=${Decimal(wb.io.out_v.bits.reg_idxw)}" +
        p" status0=0x${Hexadecimal(wb.io.out_v.bits.wb_wvd_rd(0))}\n")
    }
    rtInFlight(rtCompletionWarp) := false.B
    rtCompletionAwaitingWb := false.B
  }

  // PMU is an elaboration-time option.  Keep this collector in one structural
  // branch so an `off` build has neither its counters nor their increment
  // cones; the always-present ABI output is driven to zero in that mode.
  if (PMU_ENABLED) {
  val activeCycles = RegInit(0.U(64.W))
  val totalScalarIssued = RegInit(0.U(64.W))
  val totalVectorIssued = RegInit(0.U(64.W))
  val execStructuralHazardCyclesX = RegInit(0.U(64.W))
  val execStructuralHazardCyclesV = RegInit(0.U(64.W))
  val dataDepStallCycles = RegInit(0.U(64.W))
  val barrierStallCycles = RegInit(0.U(64.W))
  val controlHazardFlushCount = RegInit(0.U(64.W))
  val frontendStallCycles = RegInit(0.U(64.W))
  val lsuBackpressureCycles = RegInit(0.U(64.W))
  val ibufferFullCycles = RegInit(0.U(64.W))

  val computeIssued = RegInit(0.U(64.W))
  val memIssued = RegInit(0.U(64.W))
  val ctrlIssued = RegInit(0.U(64.W))

  val rtTraverseIssued = RegInit(0.U(64.W))
  val rtTraverseActiveLanes = RegInit(0.U(64.W))
  val rtCompletionCount = RegInit(0.U(64.W))
  val rtCompletionLanes = RegInit(0.U(64.W))
  val rtReleaseIssued = RegInit(0.U(64.W))
  val rtActiveSessionCycles = RegInit(0.U(64.W))
  val rtCommandBackpressureCycles = RegInit(0.U(64.W))
  val rtCompletionBackpressureCycles = RegInit(0.U(64.W))
  val rtReleaseBackpressureCycles = RegInit(0.U(64.W))
  val rtLocalSramReadRequests = RegInit(0.U(64.W))
  val rtLocalSramWriteRequests = RegInit(0.U(64.W))
  val rtLocalSramReadResponses = RegInit(0.U(64.W))
  val rtLocalSramBackpressureCycles = RegInit(0.U(64.W))
  val rtRaySetupOutputs = RegInit(0.U(64.W))
  val rtAsLineAcquires = RegInit(0.U(64.W))
  val rtAsLineReady = RegInit(0.U(64.W))
  val rtAsAcquireBackpressureCycles = RegInit(0.U(64.W))
  val rtAsCacheResidentHits = RegInit(0.U(64.W))
  val rtAsCachePendingMerges = RegInit(0.U(64.W))
  val rtAsCacheMisses = RegInit(0.U(64.W))
  val rtAsCacheRefills = RegInit(0.U(64.W))
  val rtAsCacheEvictions = RegInit(0.U(64.W))
  val rtBox4Issued = RegInit(0.U(64.W))
  val rtBox4Results = RegInit(0.U(64.W))
  val rtTriangleIssued = RegInit(0.U(64.W))
  val rtTriangleResults = RegInit(0.U(64.W))
  val rtTriangleResolveIssued = RegInit(0.U(64.W))
  val rtTriangleResolveResults = RegInit(0.U(64.W))
  val rtStackPushes = RegInit(0.U(64.W))
  val rtStackPops = RegInit(0.U(64.W))

  def isCtrlInst(ctrl: CtrlSigs): Bool = {
    ctrl.csr.orR || ctrl.barrier || ctrl.simt_stack || ctrl.branch.orR
  }

  val anyBufferedInst = VecInit(ibuffer.io.out.map(_.valid)).asUInt.orR
  val anySchedulableInst = VecInit((0 until num_warp).map(i => ibuffer.io.out(i).valid && warp_sche.io.warp_ready(i))).asUInt.orR
  val anyScoreExeBlockedInst = VecInit((0 until num_warp).map(i => ibuffer.io.out(i).valid && (scoreboardBusy(i) || warp_sche.io.exe_busy(i)))).asUInt.orR
  val anyBarrierBlockedInst = VecInit((0 until num_warp).map(i => ibuffer.io.out(i).valid && warp_sche.io.barrier_busy(i))).asUInt.orR
  val noIssueFire = !issueX.io.in.fire && !issueV.io.in.fire
  val noIssueInput = !issueX.io.in.valid && !issueV.io.in.valid
  val dataDepStall = noIssueFire && noIssueInput && anyBufferedInst && !anySchedulableInst && anyScoreExeBlockedInst
  val barrierStall = noIssueFire && noIssueInput && anyBufferedInst && !anySchedulableInst &&
    !anyScoreExeBlockedInst && anyBarrierBlockedInst
  val frontendStall = noIssueFire && noIssueInput && !dataDepStall && !barrierStall

  // Elaborate-only deadlock aid.  A long no-issue interval used to require
  // guessing whether the pending warp was blocked by a register, the operand
  // collector, or a control dependency.  Keep it out of normal RTL builds.
  if (sys.env.get("VENTUS_SCOREBOARD_TRACE").contains("1")) {
    val noIssueCycles = RegInit(0.U(16.W))
    when(noIssueFire) { noIssueCycles := noIssueCycles + 1.U }
      .otherwise { noIssueCycles := 0.U }
    when(noIssueCycles === 1024.U) {
      for (i <- 0 until num_warp) {
        when(ibuffer.io.out(i).valid) {
          printf(p"[SCOREBOARD trace] warp=${Decimal(i.U)} pc=0x${Hexadecimal(ibuffer.io.out(i).bits.pc)} inst=0x${Hexadecimal(ibuffer.io.out(i).bits.inst)} ready=${warp_sche.io.warp_ready(i)} exe=${warp_sche.io.exe_busy(i)} barrier=${warp_sche.io.barrier_busy(i)} delay=${scoreb(i).delay} r1=${scoreb(i).stall_read1} r2=${scoreb(i).stall_read2} r3=${scoreb(i).stall_read3} mask=${scoreb(i).stall_mask} wr=${scoreb(i).stall_write} br=${scoreb(i).stall_branch} fence=${scoreb(i).stall_fence} opv=${scoreb(i).stall_opcol_v} opx=${scoreb(i).stall_opcol_x}\n")
        }
      }
    }
  }

  val flushEvent = warp_sche.io.flush.valid && !RegNext(warp_sche.io.flush.valid, false.B)

  when(io.perfReset){
    activeCycles := 0.U
    totalScalarIssued := 0.U
    totalVectorIssued := 0.U
    execStructuralHazardCyclesX := 0.U
    execStructuralHazardCyclesV := 0.U
    dataDepStallCycles := 0.U
    barrierStallCycles := 0.U
    controlHazardFlushCount := 0.U
    frontendStallCycles := 0.U
    lsuBackpressureCycles := 0.U
    ibufferFullCycles := 0.U
    computeIssued := 0.U
    memIssued := 0.U
    ctrlIssued := 0.U
    rtTraverseIssued := 0.U
    rtTraverseActiveLanes := 0.U
    rtCompletionCount := 0.U
    rtCompletionLanes := 0.U
    rtReleaseIssued := 0.U
    rtActiveSessionCycles := 0.U
    rtCommandBackpressureCycles := 0.U
    rtCompletionBackpressureCycles := 0.U
    rtReleaseBackpressureCycles := 0.U
    rtLocalSramReadRequests := 0.U
    rtLocalSramWriteRequests := 0.U
    rtLocalSramReadResponses := 0.U
    rtLocalSramBackpressureCycles := 0.U
    rtRaySetupOutputs := 0.U
    rtAsLineAcquires := 0.U
    rtAsLineReady := 0.U
    rtAsAcquireBackpressureCycles := 0.U
    rtAsCacheResidentHits := 0.U
    rtAsCachePendingMerges := 0.U
    rtAsCacheMisses := 0.U
    rtAsCacheRefills := 0.U
    rtAsCacheEvictions := 0.U
    rtBox4Issued := 0.U
    rtBox4Results := 0.U
    rtTriangleIssued := 0.U
    rtTriangleResults := 0.U
    rtTriangleResolveIssued := 0.U
    rtTriangleResolveResults := 0.U
    rtStackPushes := 0.U
    rtStackPops := 0.U
  }.elsewhen(io.perfEnable){
    activeCycles := activeCycles + 1.U

    when(issueX.io.in.fire){
      totalScalarIssued := totalScalarIssued + 1.U
      when(issueX.io.in.bits.ctrl.mem){
        memIssued := memIssued + 1.U
      }.elsewhen(isCtrlInst(issueX.io.in.bits.ctrl)){
        ctrlIssued := ctrlIssued + 1.U
      }.otherwise{
        computeIssued := computeIssued + 1.U
      }
    }

    when(issueV.io.in.fire){
      totalVectorIssued := totalVectorIssued + 1.U
      when(issueV.io.in.bits.ctrl.mem){
        memIssued := memIssued + 1.U
      }.elsewhen(isCtrlInst(issueV.io.in.bits.ctrl)){
        ctrlIssued := ctrlIssued + 1.U
      }.otherwise{
        computeIssued := computeIssued + 1.U
      }
    }

    when(issueX.io.in.valid && !issueX.io.in.ready){
      execStructuralHazardCyclesX := execStructuralHazardCyclesX + 1.U
    }
    when(issueV.io.in.valid && !issueV.io.in.ready){
      execStructuralHazardCyclesV := execStructuralHazardCyclesV + 1.U
    }
    when(dataDepStall){
      dataDepStallCycles := dataDepStallCycles + 1.U
    }
    when(barrierStall){
      barrierStallCycles := barrierStallCycles + 1.U
    }
    when(frontendStall){
      frontendStallCycles := frontendStallCycles + 1.U
    }
    when(flushEvent){
      controlHazardFlushCount := controlHazardFlushCount + 1.U
    }
    when(issueV.io.in.valid && issueV.io.in.bits.ctrl.mem && !issueV.io.in.ready){
      lsuBackpressureCycles := lsuBackpressureCycles + 1.U
    }
    when(ibuffer.io.in.valid && !ibuffer.io.in.ready){
      ibufferFullCycles := ibufferFullCycles + 1.U
    }

    when(rtcore.io.command.fire) {
      rtTraverseIssued := rtTraverseIssued + 1.U
      rtTraverseActiveLanes := rtTraverseActiveLanes + PopCount(rtcore.io.command.bits.activeMask)
    }
    when(rtcore.io.completion.fire) {
      rtCompletionCount := rtCompletionCount + 1.U
      rtCompletionLanes := rtCompletionLanes + PopCount(rtcore.io.completion.bits.completionMask)
    }
    // Count logical software-session releases even when physical context
    // reclamation already happened at a terminal traversal completion.
    when(issueV.io.out_RT.fire && rtIsRelease && !rtReleaseEmpty) {
      rtReleaseIssued := rtReleaseIssued + 1.U
    }
    when(rtInFlight.asUInt.orR || rtCompletionAwaitingWb || rtReleaseSent) {
      rtActiveSessionCycles := rtActiveSessionCycles + 1.U
    }
    when(rtcore.io.command.valid && !rtcore.io.command.ready) {
      rtCommandBackpressureCycles := rtCommandBackpressureCycles + 1.U
    }
    when(rtcore.io.completion.valid && !rtcore.io.completion.ready) {
      rtCompletionBackpressureCycles := rtCompletionBackpressureCycles + 1.U
    }
    when(rtcore.io.release.valid && !rtcore.io.release.ready) {
      rtReleaseBackpressureCycles := rtReleaseBackpressureCycles + 1.U
    }
    when(rtcore.io.perf.localSramReadRequest) { rtLocalSramReadRequests := rtLocalSramReadRequests + 1.U }
    when(rtcore.io.perf.localSramWriteRequest) { rtLocalSramWriteRequests := rtLocalSramWriteRequests + 1.U }
    when(rtcore.io.perf.localSramReadResponse) { rtLocalSramReadResponses := rtLocalSramReadResponses + 1.U }
    when(rtcore.io.perf.localSramBackpressure) { rtLocalSramBackpressureCycles := rtLocalSramBackpressureCycles + 1.U }
    when(rtcore.io.perf.raySetupOutput) { rtRaySetupOutputs := rtRaySetupOutputs + 1.U }
    when(rtcore.io.perf.asLineAcquire) { rtAsLineAcquires := rtAsLineAcquires + 1.U }
    rtAsLineReady := rtAsLineReady + rtcore.io.perf.asLineReady
    when(rtcore.io.perf.asAcquireBackpressure) { rtAsAcquireBackpressureCycles := rtAsAcquireBackpressureCycles + 1.U }
    when(rtcore.io.perf.asCacheResidentHit) { rtAsCacheResidentHits := rtAsCacheResidentHits + 1.U }
    when(rtcore.io.perf.asCachePendingMerge) { rtAsCachePendingMerges := rtAsCachePendingMerges + 1.U }
    when(rtcore.io.perf.asCacheMiss) { rtAsCacheMisses := rtAsCacheMisses + 1.U }
    when(rtcore.io.perf.asCacheRefill) { rtAsCacheRefills := rtAsCacheRefills + 1.U }
    when(rtcore.io.perf.asCacheEviction) { rtAsCacheEvictions := rtAsCacheEvictions + 1.U }
    rtBox4Issued := rtBox4Issued + rtcore.io.perf.box4Issue
    rtBox4Results := rtBox4Results + rtcore.io.perf.box4Result
    rtTriangleIssued := rtTriangleIssued + rtcore.io.perf.triangleIssue
    rtTriangleResults := rtTriangleResults + rtcore.io.perf.triangleResult
    when(rtcore.io.perf.triangleResolveIssue) { rtTriangleResolveIssued := rtTriangleResolveIssued + 1.U }
    when(rtcore.io.perf.triangleResolveResult) { rtTriangleResolveResults := rtTriangleResolveResults + 1.U }
    rtStackPushes := rtStackPushes + rtcore.io.perf.stackPush
    rtStackPops := rtStackPops + rtcore.io.perf.stackPop
  }

  io.perf_rtcore.traverseIssued := rtTraverseIssued
  io.perf_rtcore.traverseActiveLanes := rtTraverseActiveLanes
  io.perf_rtcore.completionCount := rtCompletionCount
  io.perf_rtcore.completionLanes := rtCompletionLanes
  io.perf_rtcore.releaseIssued := rtReleaseIssued
  io.perf_rtcore.activeSessionCycles := rtActiveSessionCycles
  io.perf_rtcore.commandBackpressureCycles := rtCommandBackpressureCycles
  io.perf_rtcore.completionBackpressureCycles := rtCompletionBackpressureCycles
  io.perf_rtcore.releaseBackpressureCycles := rtReleaseBackpressureCycles
  io.perf_rtcore.localSramReadRequests := rtLocalSramReadRequests
  io.perf_rtcore.localSramWriteRequests := rtLocalSramWriteRequests
  io.perf_rtcore.localSramReadResponses := rtLocalSramReadResponses
  io.perf_rtcore.localSramBackpressureCycles := rtLocalSramBackpressureCycles
  io.perf_rtcore.raySetupOutputs := rtRaySetupOutputs
  io.perf_rtcore.asLineAcquires := rtAsLineAcquires
  io.perf_rtcore.asLineReady := rtAsLineReady
  io.perf_rtcore.asAcquireBackpressureCycles := rtAsAcquireBackpressureCycles
  io.perf_rtcore.asCacheResidentHits := rtAsCacheResidentHits
  io.perf_rtcore.asCachePendingMerges := rtAsCachePendingMerges
  io.perf_rtcore.asCacheMisses := rtAsCacheMisses
  io.perf_rtcore.asCacheRefills := rtAsCacheRefills
  io.perf_rtcore.asCacheEvictions := rtAsCacheEvictions
  io.perf_rtcore.box4Issued := rtBox4Issued
  io.perf_rtcore.box4Results := rtBox4Results
  io.perf_rtcore.triangleIssued := rtTriangleIssued
  io.perf_rtcore.triangleResults := rtTriangleResults
  io.perf_rtcore.triangleResolveIssued := rtTriangleResolveIssued
  io.perf_rtcore.triangleResolveResults := rtTriangleResolveResults
  io.perf_rtcore.stackPushes := rtStackPushes
  io.perf_rtcore.stackPops := rtStackPops
  if (PMU_DETAILED) {
    io.perf_rtcore.detailed.get := rtcore.io.perf.detailed.get
    io.perf_rtcore.detailed.get.enabled := io.perfEnable
    io.perf_rtcore.detailed.get.clear := io.perfReset || reset.asBool
  }

  io.perf_pipeline.foreach { perf =>
    perf.activeCycles := activeCycles
    perf.totalScalarIssued := totalScalarIssued
    perf.totalVectorIssued := totalVectorIssued
    perf.execStructuralHazardCyclesX := execStructuralHazardCyclesX
    perf.execStructuralHazardCyclesV := execStructuralHazardCyclesV
    perf.dataDepStallCycles := dataDepStallCycles
    perf.barrierStallCycles := barrierStallCycles
    perf.controlHazardFlushCount := controlHazardFlushCount
    perf.frontendStallCycles := frontendStallCycles
    perf.lsuBackpressureCycles := lsuBackpressureCycles
    perf.ibufferFullCycles := ibufferFullCycles
  }

  io.perf_inst_class.foreach { perf =>
    perf.computeIssued := computeIssued
    perf.memIssued := memIssued
    perf.ctrlIssued := ctrlIssued
  }
  } else {
    io.perf_rtcore := 0.U.asTypeOf(new RtCorePerfCounters)
  }

  issue_stall:=(~issueX.io.in.ready).asBool | (~issueV.io.in.ready).asBool//scoreb.io.delay | issue.io.in.ready
}
