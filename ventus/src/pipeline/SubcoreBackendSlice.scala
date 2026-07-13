/*
 * Copyright (c) 2021-2022 International Innovation Center of Tsinghua University, Shanghai
 * Ventus is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 *          http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND,
 * EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package pipeline

import chisel3._
import chisel3.experimental.hierarchy.{Instantiate, instantiable, public}
import chisel3.util._
import top.parameters._

// SubcoreBackendSlice owns the subcore-local execution slices that have been
// validated so far. DCache/shared memory and LSU2WB remain SM-shared; final
// writeback arbitration is local to each subcore just outside this slice.

class SubcoreBackendSliceIO extends Bundle {
  private val localDepthWarp =
    if (num_warp_per_subcore == 1) 1 else log2Ceil(num_warp_per_subcore)
  private val subcoreIdWidth = if (num_subcore <= 1) 1 else log2Ceil(num_subcore)

  val subcore_id = Input(UInt(subcoreIdWidth.W))

  val frontend_control_v = Flipped(DecoupledIO(new CtrlSigs))
  val frontend_control_x = Flipped(DecoupledIO(new CtrlSigs))

  val write_scalar = Flipped(DecoupledIO(new WriteScalarCtrl))
  val write_vec = Flipped(DecoupledIO(new WriteVecCtrl))

  val branch_out = DecoupledIO(new BranchCtrl)
  val simt_complete = ValidIO(UInt(localDepthWarp.W))
  val warp_lifecycle = DecoupledIO(new warpSchedulerExeData)
  val warp_init = Input(ValidIO(new warpReqData))
  val csr_scalar = DecoupledIO(new WriteScalarCtrl)
  val csr_vec = DecoupledIO(new WriteVecCtrl)

  val lsu_dcache_req = DecoupledIO(new DCacheCoreReq_np)
  val lsu_dcache_rsp = Flipped(DecoupledIO(new DCacheCoreRsp_np))
  val lsu_shared_req = DecoupledIO(new ShareMemCoreReq_np)
  val lsu_shared_rsp = Flipped(DecoupledIO(new DCacheCoreRsp_np))
  val lsu_result_rsp = DecoupledIO(new MSHROutput)
  val flush_dcache = Flipped(DecoupledIO(Bool()))
  val lsu_fence_end = Output(UInt(num_warp_per_subcore.W))

  val compute_alu_scalar = DecoupledIO(new WriteScalarCtrl)
  val compute_fpu_scalar = DecoupledIO(new WriteScalarCtrl)
  val compute_fpu_vec = DecoupledIO(new WriteVecCtrl)
  val compute_sfu_scalar = DecoupledIO(new WriteScalarCtrl)
  val compute_sfu_vec = DecoupledIO(new WriteVecCtrl)
  val compute_valu_vec = DecoupledIO(new WriteVecCtrl)
  val compute_mul_scalar = DecoupledIO(new WriteScalarCtrl)
  val compute_mul_vec = DecoupledIO(new WriteVecCtrl)
  val compute_tc_vec = DecoupledIO(new WriteVecCtrl)

  val scoreboard = Output(new ScoreboardSubcoreEvents)
}

class SubcoreScalarExecSliceIO extends Bundle {
  val issue_salu = Input(ValidIO(new sExeData))
  val issue_salu_ready = Output(Bool())
  val issue_csr = Input(ValidIO(new csrExeData))
  val issue_csr_ready = Output(Bool())
  val csr_rm_wid = Input(Vec(3, UInt(depth_warp.W)))
  val csr_cta = Input(ValidIO(new warpReqData))
  val csr_lsu_wid = Input(UInt(depth_warp.W))
  val csr_simt_wid = Input(UInt(depth_warp.W))

  val alu_scalar = DecoupledIO(new WriteScalarCtrl)
  val alu_branch = DecoupledIO(new BranchCtrl)
  val csr_scalar = DecoupledIO(new WriteScalarCtrl)
  val csr_vec = DecoupledIO(new WriteVecCtrl)

  val rm = Output(Vec(3, UInt(3.W)))
  val sgpr_base = Output(Vec(num_warp_per_subcore, UInt((SGPR_ID_WIDTH + 1).W)))
  val vgpr_base = Output(Vec(num_warp_per_subcore, UInt((VGPR_ID_WIDTH + 1).W)))
  val lsu_tid = Output(UInt(xLen.W))
  val lsu_pds = Output(UInt(xLen.W))
  val lsu_numw = Output(UInt(xLen.W))
  val lsu_numt = Output(UInt(xLen.W))
  val simt_rpc = Output(UInt(xLen.W))
}

@instantiable
class SubcoreScalarExecSlice extends Module {
  @public val io = IO(new SubcoreScalarExecSliceIO)

  private val scalarAlu = Instantiate(new ALUexe)
  private val csr = Instantiate(new CSRexe(num_warp_per_subcore))

  scalarAlu.io.in.valid := io.issue_salu.valid
  scalarAlu.io.in.bits := io.issue_salu.bits
  io.issue_salu_ready := scalarAlu.io.in.ready
  io.alu_scalar.valid := scalarAlu.io.out.valid
  io.alu_scalar.bits := scalarAlu.io.out.bits
  scalarAlu.io.out.ready := io.alu_scalar.ready
  io.alu_branch.valid := scalarAlu.io.out2br.valid
  io.alu_branch.bits := scalarAlu.io.out2br.bits
  scalarAlu.io.out2br.ready := io.alu_branch.ready

  csr.io.in.valid := io.issue_csr.valid
  csr.io.in.bits := io.issue_csr.bits
  io.issue_csr_ready := csr.io.in.ready
  csr.io.rm_wid := io.csr_rm_wid
  csr.io.CTA2csr := io.csr_cta
  csr.io.lsu_wid := io.csr_lsu_wid
  csr.io.simt_wid := io.csr_simt_wid
  io.csr_scalar.valid := csr.io.out.valid
  io.csr_scalar.bits := csr.io.out.bits
  csr.io.out.ready := io.csr_scalar.ready
  io.csr_vec.valid := csr.io.out_v.valid
  io.csr_vec.bits := csr.io.out_v.bits
  csr.io.out_v.ready := io.csr_vec.ready

  io.rm := csr.io.rm
  io.sgpr_base := VecInit(csr.io.sgpr_base.take(num_warp_per_subcore))
  io.vgpr_base := VecInit(csr.io.vgpr_base.take(num_warp_per_subcore))
  io.lsu_tid := csr.io.lsu_tid
  io.lsu_pds := csr.io.lsu_pds
  io.lsu_numw := csr.io.lsu_numw
  io.lsu_numt := csr.io.lsu_numt
  io.simt_rpc := csr.io.simt_rpc
}

class SubcoreMemoryExecSliceIO extends Bundle {
  private val subcoreIdWidth = if (num_subcore <= 1) 1 else log2Ceil(num_subcore)

  val subcore_id = Input(UInt(subcoreIdWidth.W))
  val issue_lsu = Input(ValidIO(new vExeData))
  val issue_lsu_ready = Output(Bool())

  val lsu_dcache_req = DecoupledIO(new DCacheCoreReq_np)
  val lsu_dcache_rsp = Flipped(DecoupledIO(new DCacheCoreRsp_np))
  val lsu_shared_req = DecoupledIO(new ShareMemCoreReq_np)
  val lsu_shared_rsp = Flipped(DecoupledIO(new DCacheCoreRsp_np))
  val lsu_result_rsp = DecoupledIO(new MSHROutput)
  val flush_dcache = Flipped(DecoupledIO(Bool()))
  val fence_end = Output(UInt(num_warp_per_subcore.W))

  val csr_wid = Output(UInt(depth_warp.W))
  val csr_tid = Input(UInt(xLen.W))
  val csr_pds = Input(UInt(xLen.W))
  val csr_numw = Input(UInt(xLen.W))
  val csr_numt = Input(UInt(xLen.W))
}

@instantiable
class SubcoreMemoryExecSlice extends Module {
  @public val io = IO(new SubcoreMemoryExecSliceIO)

  private val lsu = Instantiate(new LSUexe(num_warp_per_subcore))
  lsu.io.subcore_id := io.subcore_id

  lsu.io.lsu_req.valid := io.issue_lsu.valid
  lsu.io.lsu_req.bits := io.issue_lsu.bits
  io.issue_lsu_ready := lsu.io.lsu_req.ready

  io.lsu_dcache_req.valid := lsu.io.dcache_req.valid
  io.lsu_dcache_req.bits := lsu.io.dcache_req.bits
  lsu.io.dcache_req.ready := io.lsu_dcache_req.ready
  lsu.io.dcache_rsp.valid := io.lsu_dcache_rsp.valid
  lsu.io.dcache_rsp.bits := io.lsu_dcache_rsp.bits
  io.lsu_dcache_rsp.ready := lsu.io.dcache_rsp.ready
  io.lsu_shared_req.valid := lsu.io.shared_req.valid
  io.lsu_shared_req.bits := lsu.io.shared_req.bits
  lsu.io.shared_req.ready := io.lsu_shared_req.ready
  lsu.io.shared_rsp.valid := io.lsu_shared_rsp.valid
  lsu.io.shared_rsp.bits := io.lsu_shared_rsp.bits
  io.lsu_shared_rsp.ready := lsu.io.shared_rsp.ready
  io.lsu_result_rsp.valid := lsu.io.lsu_rsp.valid
  io.lsu_result_rsp.bits := lsu.io.lsu_rsp.bits
  lsu.io.lsu_rsp.ready := io.lsu_result_rsp.ready
  lsu.io.flush_dcache.valid := io.flush_dcache.valid
  lsu.io.flush_dcache.bits := io.flush_dcache.bits
  io.flush_dcache.ready := lsu.io.flush_dcache.ready

  io.fence_end := lsu.io.fence_end
  io.csr_wid := lsu.io.csr_wid
  lsu.io.csr_pds := io.csr_pds
  lsu.io.csr_tid := io.csr_tid
  lsu.io.csr_numw := io.csr_numw
  lsu.io.csr_numt := io.csr_numt
}

class SubcoreVectorExecSliceIO extends Bundle {
  val issue_fpu = Input(ValidIO(new vExeData))
  val issue_fpu_ready = Output(Bool())
  val issue_sfu = Input(ValidIO(new vExeData))
  val issue_sfu_ready = Output(Bool())
  val issue_tc = Input(ValidIO(new vExeData))
  val issue_tc_ready = Output(Bool())

  val rm = Input(Vec(3, UInt(3.W)))

  val fpu_scalar = DecoupledIO(new WriteScalarCtrl)
  val fpu_vec = DecoupledIO(new WriteVecCtrl)
  val sfu_scalar = DecoupledIO(new WriteScalarCtrl)
  val sfu_vec = DecoupledIO(new WriteVecCtrl)
  val tc_vec = DecoupledIO(new WriteVecCtrl)
}

@instantiable
class SubcoreVectorExecSlice extends Module {
  @public val io = IO(new SubcoreVectorExecSliceIO)

  private val fpu = Instantiate(new FPUexe(num_thread, num_lane))
  private val sfu = Instantiate(new SFUexe)
  private val tensorcore = Instantiate(new vTCexe)

  fpu.io.in.valid := io.issue_fpu.valid
  fpu.io.in.bits := io.issue_fpu.bits
  fpu.io.rm := Mux(fpu.io.in.bits.ctrl.force_rm_rtz, RoundingMode.RTZ, io.rm(0))
  io.issue_fpu_ready := fpu.io.in.ready

  sfu.io.in.valid := io.issue_sfu.valid
  sfu.io.in.bits := io.issue_sfu.bits
  sfu.io.rm := io.rm(1)
  io.issue_sfu_ready := sfu.io.in.ready

  tensorcore.io.in.valid := io.issue_tc.valid
  tensorcore.io.in.bits := io.issue_tc.bits
  tensorcore.io.rm := io.rm(2)
  io.issue_tc_ready := tensorcore.io.in.ready

  // Slice-local execution outputs return to pipe.scala's WB fabric; this slice
  // does not own final writeback arbitration.
  io.fpu_scalar.valid := fpu.io.out_x.valid
  io.fpu_scalar.bits := fpu.io.out_x.bits
  fpu.io.out_x.ready := io.fpu_scalar.ready
  io.fpu_vec.valid := fpu.io.out_v.valid
  io.fpu_vec.bits := fpu.io.out_v.bits
  fpu.io.out_v.ready := io.fpu_vec.ready
  io.sfu_scalar.valid := sfu.io.out_x.valid
  io.sfu_scalar.bits := sfu.io.out_x.bits
  sfu.io.out_x.ready := io.sfu_scalar.ready
  io.sfu_vec.valid := sfu.io.out_v.valid
  io.sfu_vec.bits := sfu.io.out_v.bits
  sfu.io.out_v.ready := io.sfu_vec.ready
  io.tc_vec.valid := tensorcore.io.out_v.valid
  io.tc_vec.bits := tensorcore.io.out_v.bits
  tensorcore.io.out_v.ready := io.tc_vec.ready
}

class SubcoreIssueOperandIO extends Bundle {
  private val localDepthWarp = if (num_warp_per_subcore == 1) 1 else log2Ceil(num_warp_per_subcore)
  val control_x = Flipped(DecoupledIO(new CtrlSigs))
  val control_v = Flipped(DecoupledIO(new CtrlSigs))
  val write_scalar = Flipped(DecoupledIO(new WriteScalarCtrl))
  val write_vec = Flipped(DecoupledIO(new WriteVecCtrl))
  val sgpr_base = Input(Vec(num_warp_per_subcore, UInt((SGPR_ID_WIDTH + 1).W)))
  val vgpr_base = Input(Vec(num_warp_per_subcore, UInt((VGPR_ID_WIDTH + 1).W)))
  val simt_mask = Input(UInt(num_thread.W))
  val salu = DecoupledIO(new sExeData)
  val csr = DecoupledIO(new csrExeData)
  val lifecycle = DecoupledIO(new warpSchedulerExeData)
  val valu = DecoupledIO(new vExeData)
  val fpu = DecoupledIO(new vExeData)
  val sfu = DecoupledIO(new vExeData)
  val tc = DecoupledIO(new vExeData)
  val lsu = DecoupledIO(new vExeData)
  val mul = DecoupledIO(new vExeData)
  val simt = DecoupledIO(new simtExeData)
  val op_col_x_in_fire = Output(Bool())
  val op_col_x_in_wid = Output(UInt(localDepthWarp.W))
  val op_col_x_out_fire = Output(Bool())
  val op_col_x_out_wid = Output(UInt(localDepthWarp.W))
  val op_col_v_in_fire = Output(Bool())
  val op_col_v_in_wid = Output(UInt(localDepthWarp.W))
  val op_col_v_out_fire = Output(Bool())
  val op_col_v_out_wid = Output(UInt(localDepthWarp.W))
}

@instantiable
class SubcoreIssueOperand extends Module {
  @public val io = IO(new SubcoreIssueOperandIO)
  private val oc = Instantiate(new operandCollector(num_warp_per_subcore))
  private val issueX = Instantiate(new Issue)
  private val issueV = Instantiate(new Issue)

  oc.io.controlX.valid := io.control_x.valid; oc.io.controlX.bits := io.control_x.bits; io.control_x.ready := oc.io.controlX.ready
  oc.io.controlV.valid := io.control_v.valid; oc.io.controlV.bits := io.control_v.bits; io.control_v.ready := oc.io.controlV.ready
  oc.io.writeScalarCtrl.valid := io.write_scalar.valid; oc.io.writeScalarCtrl.bits := io.write_scalar.bits; io.write_scalar.ready := oc.io.writeScalarCtrl.ready
  oc.io.writeVecCtrl.valid := io.write_vec.valid; oc.io.writeVecCtrl.bits := io.write_vec.bits; io.write_vec.ready := oc.io.writeVecCtrl.ready
  oc.io.sgpr_base := io.sgpr_base
  oc.io.vgpr_base := io.vgpr_base

  issueX.io.in.valid := oc.io.out(1).valid
  issueX.io.in.bits.ctrl := oc.io.out(1).bits.control
  issueX.io.in.bits.in1 := oc.io.out(1).bits.alu_src1
  issueX.io.in.bits.in2 := oc.io.out(1).bits.alu_src2
  issueX.io.in.bits.in3 := oc.io.out(1).bits.alu_src3
  issueX.io.in.bits.mask.foreach(_ := true.B)
  oc.io.out(1).ready := issueX.io.in.ready
  issueV.io.in.valid := oc.io.out(0).valid
  issueV.io.in.bits.ctrl := oc.io.out(0).bits.control
  issueV.io.in.bits.in1 := oc.io.out(0).bits.alu_src1
  issueV.io.in.bits.in2 := oc.io.out(0).bits.alu_src2
  issueV.io.in.bits.in3 := oc.io.out(0).bits.alu_src3
  issueV.io.in.bits.mask := VecInit((0 until num_thread).map(i => oc.io.out(0).bits.mask(i) & io.simt_mask(i)))
  oc.io.out(0).ready := issueV.io.in.ready

  def forward[T <: Data](dst: DecoupledIO[T], src: DecoupledIO[T]): Unit = {
    dst.valid := src.valid; dst.bits := src.bits; src.ready := dst.ready
  }
  forward(io.salu, issueX.io.out_sALU)
  forward(io.csr, issueX.io.out_CSR)
  forward(io.lifecycle, issueX.io.out_warpscheduler)
  issueX.io.out_vALU.ready := false.B; issueX.io.out_vFPU.ready := false.B
  issueX.io.out_LSU.ready := false.B; issueX.io.out_SFU.ready := false.B
  issueX.io.out_SIMT.ready := false.B; issueX.io.out_MUL.ready := false.B; issueX.io.out_TC.ready := false.B
  forward(io.valu, issueV.io.out_vALU)
  forward(io.fpu, issueV.io.out_vFPU)
  forward(io.sfu, issueV.io.out_SFU)
  forward(io.tc, issueV.io.out_TC)
  forward(io.lsu, issueV.io.out_LSU)
  forward(io.mul, issueV.io.out_MUL)
  forward(io.simt, issueV.io.out_SIMT)
  issueV.io.out_sALU.ready := false.B; issueV.io.out_CSR.ready := false.B
  issueV.io.out_warpscheduler.ready := false.B

  io.op_col_x_in_fire := oc.io.controlX.fire; io.op_col_x_in_wid := oc.io.controlX.bits.wid
  io.op_col_x_out_fire := oc.io.out(1).fire; io.op_col_x_out_wid := oc.io.out(1).bits.control.wid
  io.op_col_v_in_fire := oc.io.controlV.fire; io.op_col_v_in_wid := oc.io.controlV.bits.wid
  io.op_col_v_out_fire := oc.io.out(0).fire; io.op_col_v_out_wid := oc.io.out(0).bits.control.wid
}

class SubcoreControlFlowIO extends Bundle {
  private val localDepthWarp =
    if (num_warp_per_subcore == 1) 1 else log2Ceil(num_warp_per_subcore)

  val simt_issue = Flipped(DecoupledIO(new simtExeData))
  val valu_mask = Flipped(DecoupledIO(new vec_alu_bus))
  val scalar_branch = Flipped(DecoupledIO(new BranchCtrl))
  val pc_reconv = Input(UInt(xLen.W))
  val warp_init = Input(Valid(new warpReqData))

  val simt_mask = Output(UInt(num_thread.W))
  val branch = DecoupledIO(new BranchCtrl)
  val complete = ValidIO(UInt(localDepthWarp.W))
}

@instantiable
class SubcoreControlFlow extends Module {
  @public val io = IO(new SubcoreControlFlowIO)
  private val localDepthWarp =
    if (num_warp_per_subcore == 1) 1 else log2Ceil(num_warp_per_subcore)

  private val simt = Instantiate(new branch_join(num_thread, num_warp_per_subcore))
  private val branchBack = Instantiate(new Branch_back)

  simt.io.branch_ctl.valid := io.simt_issue.valid
  simt.io.branch_ctl.bits := io.simt_issue.bits
  io.simt_issue.ready := simt.io.branch_ctl.ready
  simt.io.if_mask.valid := io.valu_mask.valid
  simt.io.if_mask.bits := io.valu_mask.bits
  io.valu_mask.ready := simt.io.if_mask.ready
  branchBack.io.in0.valid := io.scalar_branch.valid
  branchBack.io.in0.bits := io.scalar_branch.bits
  io.scalar_branch.ready := branchBack.io.in0.ready
  branchBack.io.in1 <> simt.io.fetch_ctl
  io.branch.valid := branchBack.io.out.valid
  io.branch.bits := branchBack.io.out.bits
  branchBack.io.out.ready := io.branch.ready

  simt.io.input_wid := io.simt_issue.bits.wid
  simt.io.pc_reconv.bits := io.pc_reconv
  simt.io.pc_reconv.valid := io.simt_issue.valid
  simt.io.initMask.valid := io.warp_init.valid
  simt.io.initMask.bits.warp_id := io.warp_init.bits.wid
  simt.io.initMask.bits.thread_mask :=
    (1.U(num_thread.W) << io.warp_init.bits.CTAdata.dispatch2cu_wf_size_dispatch).asUInt - 1.U

  io.simt_mask := simt.io.out_mask
  io.complete.valid := simt.io.complete.valid
  io.complete.bits := simt.io.complete.bits(localDepthWarp - 1, 0)
}

@instantiable
class SubcoreBackendSlice extends Module {
  @public val io = IO(new SubcoreBackendSliceIO)
  private val localDepthWarp =
    if (num_warp_per_subcore == 1) 1 else log2Ceil(num_warp_per_subcore)

  // Hierarchical subcore backend slices. Writeback/fabric remains outside this
  // slice so firtool does not see vector execution and arbitration as one block.
  val scalarExec = Instantiate(new SubcoreScalarExecSlice)
  val memoryExec = Instantiate(new SubcoreMemoryExecSlice)
  val vectorExec = Instantiate(new SubcoreVectorExecSlice)

  memoryExec.io.subcore_id := io.subcore_id

  io.lsu_fence_end := memoryExec.io.fence_end
  scalarExec.io.csr_cta := io.warp_init
  scalarExec.io.csr_lsu_wid := memoryExec.io.csr_wid
  io.csr_scalar.valid := scalarExec.io.csr_scalar.valid
  io.csr_scalar.bits := scalarExec.io.csr_scalar.bits
  scalarExec.io.csr_scalar.ready := io.csr_scalar.ready
  io.csr_vec.valid := scalarExec.io.csr_vec.valid
  io.csr_vec.bits := scalarExec.io.csr_vec.bits
  scalarExec.io.csr_vec.ready := io.csr_vec.ready

  // DCache, shared memory, and LSU2WB remain SM-level resources.
  io.lsu_dcache_req.valid := memoryExec.io.lsu_dcache_req.valid
  io.lsu_dcache_req.bits := memoryExec.io.lsu_dcache_req.bits
  memoryExec.io.lsu_dcache_req.ready := io.lsu_dcache_req.ready
  memoryExec.io.lsu_dcache_rsp.valid := io.lsu_dcache_rsp.valid
  memoryExec.io.lsu_dcache_rsp.bits := io.lsu_dcache_rsp.bits
  io.lsu_dcache_rsp.ready := memoryExec.io.lsu_dcache_rsp.ready
  io.lsu_shared_req.valid := memoryExec.io.lsu_shared_req.valid
  io.lsu_shared_req.bits := memoryExec.io.lsu_shared_req.bits
  memoryExec.io.lsu_shared_req.ready := io.lsu_shared_req.ready
  memoryExec.io.lsu_shared_rsp.valid := io.lsu_shared_rsp.valid
  memoryExec.io.lsu_shared_rsp.bits := io.lsu_shared_rsp.bits
  io.lsu_shared_rsp.ready := memoryExec.io.lsu_shared_rsp.ready
  io.lsu_result_rsp.valid := memoryExec.io.lsu_result_rsp.valid
  io.lsu_result_rsp.bits := memoryExec.io.lsu_result_rsp.bits
  memoryExec.io.lsu_result_rsp.ready := io.lsu_result_rsp.ready
  memoryExec.io.flush_dcache.valid := io.flush_dcache.valid
  memoryExec.io.flush_dcache.bits := io.flush_dcache.bits
  io.flush_dcache.ready := memoryExec.io.flush_dcache.ready
  memoryExec.io.csr_pds := scalarExec.io.lsu_pds
  memoryExec.io.csr_tid := scalarExec.io.lsu_tid
  memoryExec.io.csr_numw := scalarExec.io.lsu_numw
  memoryExec.io.csr_numt := scalarExec.io.lsu_numt

  vectorExec.io.rm := scalarExec.io.rm
  io.compute_alu_scalar.valid := scalarExec.io.alu_scalar.valid
  io.compute_alu_scalar.bits := scalarExec.io.alu_scalar.bits
  scalarExec.io.alu_scalar.ready := io.compute_alu_scalar.ready
  io.compute_fpu_scalar.valid := vectorExec.io.fpu_scalar.valid
  io.compute_fpu_scalar.bits := vectorExec.io.fpu_scalar.bits
  vectorExec.io.fpu_scalar.ready := io.compute_fpu_scalar.ready
  io.compute_fpu_vec.valid := vectorExec.io.fpu_vec.valid
  io.compute_fpu_vec.bits := vectorExec.io.fpu_vec.bits
  vectorExec.io.fpu_vec.ready := io.compute_fpu_vec.ready
  io.compute_sfu_scalar.valid := vectorExec.io.sfu_scalar.valid
  io.compute_sfu_scalar.bits := vectorExec.io.sfu_scalar.bits
  vectorExec.io.sfu_scalar.ready := io.compute_sfu_scalar.ready
  io.compute_sfu_vec.valid := vectorExec.io.sfu_vec.valid
  io.compute_sfu_vec.bits := vectorExec.io.sfu_vec.bits
  vectorExec.io.sfu_vec.ready := io.compute_sfu_vec.ready
  io.compute_tc_vec.valid := vectorExec.io.tc_vec.valid
  io.compute_tc_vec.bits := vectorExec.io.tc_vec.bits
  vectorExec.io.tc_vec.ready := io.compute_tc_vec.ready

  // Full subcore backend closure. All wid values in this block are local to
  // the owning subcore; pipe.scala restores global wid at shared boundaries.
  val issueOperand = Instantiate(new SubcoreIssueOperand)
  val localValu = Instantiate(new vALUv2(num_thread, num_lane))
  val localMul = Instantiate(new vMULv2(num_thread, num_lane))
  val controlFlow = Instantiate(new SubcoreControlFlow)

  issueOperand.io.control_x.valid := io.frontend_control_x.valid
  issueOperand.io.control_x.bits := io.frontend_control_x.bits
  io.frontend_control_x.ready := issueOperand.io.control_x.ready
  issueOperand.io.control_v.valid := io.frontend_control_v.valid
  issueOperand.io.control_v.bits := io.frontend_control_v.bits
  io.frontend_control_v.ready := issueOperand.io.control_v.ready
  issueOperand.io.write_scalar.valid := io.write_scalar.valid
  issueOperand.io.write_scalar.bits := io.write_scalar.bits
  io.write_scalar.ready := issueOperand.io.write_scalar.ready
  issueOperand.io.write_vec.valid := io.write_vec.valid
  issueOperand.io.write_vec.bits := io.write_vec.bits
  io.write_vec.ready := issueOperand.io.write_vec.ready
  issueOperand.io.sgpr_base := scalarExec.io.sgpr_base
  issueOperand.io.vgpr_base := scalarExec.io.vgpr_base
  issueOperand.io.simt_mask := controlFlow.io.simt_mask
  scalarExec.io.csr_rm_wid(0) := issueOperand.io.fpu.bits.ctrl.wid
  scalarExec.io.csr_rm_wid(1) := issueOperand.io.sfu.bits.ctrl.wid
  scalarExec.io.csr_rm_wid(2) := issueOperand.io.tc.bits.ctrl.wid
  scalarExec.io.csr_simt_wid := issueOperand.io.simt.bits.wid
  scalarExec.io.issue_salu.valid := issueOperand.io.salu.valid
  scalarExec.io.issue_salu.bits := issueOperand.io.salu.bits
  issueOperand.io.salu.ready := scalarExec.io.issue_salu_ready
  scalarExec.io.issue_csr.valid := issueOperand.io.csr.valid
  scalarExec.io.issue_csr.bits := issueOperand.io.csr.bits
  issueOperand.io.csr.ready := scalarExec.io.issue_csr_ready
  io.warp_lifecycle.valid := issueOperand.io.lifecycle.valid
  io.warp_lifecycle.bits := issueOperand.io.lifecycle.bits
  issueOperand.io.lifecycle.ready := io.warp_lifecycle.ready
  localValu.io.in <> issueOperand.io.valu
  vectorExec.io.issue_fpu.valid := issueOperand.io.fpu.valid; vectorExec.io.issue_fpu.bits := issueOperand.io.fpu.bits; issueOperand.io.fpu.ready := vectorExec.io.issue_fpu_ready
  vectorExec.io.issue_sfu.valid := issueOperand.io.sfu.valid; vectorExec.io.issue_sfu.bits := issueOperand.io.sfu.bits; issueOperand.io.sfu.ready := vectorExec.io.issue_sfu_ready
  vectorExec.io.issue_tc.valid := issueOperand.io.tc.valid; vectorExec.io.issue_tc.bits := issueOperand.io.tc.bits; issueOperand.io.tc.ready := vectorExec.io.issue_tc_ready
  memoryExec.io.issue_lsu.valid := issueOperand.io.lsu.valid; memoryExec.io.issue_lsu.bits := issueOperand.io.lsu.bits; issueOperand.io.lsu.ready := memoryExec.io.issue_lsu_ready
  localMul.io.in <> issueOperand.io.mul
  controlFlow.io.simt_issue <> issueOperand.io.simt

  controlFlow.io.valu_mask <> localValu.io.out2simt_stack
  controlFlow.io.scalar_branch.valid := scalarExec.io.alu_branch.valid
  controlFlow.io.scalar_branch.bits := scalarExec.io.alu_branch.bits
  scalarExec.io.alu_branch.ready := controlFlow.io.scalar_branch.ready
  controlFlow.io.pc_reconv := scalarExec.io.simt_rpc
  controlFlow.io.warp_init := io.warp_init
  io.branch_out.valid := controlFlow.io.branch.valid
  io.branch_out.bits := controlFlow.io.branch.bits
  controlFlow.io.branch.ready := io.branch_out.ready
  io.simt_complete.valid := controlFlow.io.complete.valid
  io.simt_complete.bits := controlFlow.io.complete.bits

  io.compute_valu_vec.valid := localValu.io.out.valid
  io.compute_valu_vec.bits := localValu.io.out.bits
  localValu.io.out.ready := io.compute_valu_vec.ready
  io.compute_mul_scalar.valid := localMul.io.out_x.valid
  io.compute_mul_scalar.bits := localMul.io.out_x.bits
  localMul.io.out_x.ready := io.compute_mul_scalar.ready
  io.compute_mul_vec.valid := localMul.io.out_v.valid
  io.compute_mul_vec.bits := localMul.io.out_v.bits
  localMul.io.out_v.ready := io.compute_mul_vec.ready

  io.scoreboard.op_col_x_in_fire := issueOperand.io.op_col_x_in_fire
  io.scoreboard.op_col_x_in_wid := issueOperand.io.op_col_x_in_wid
  io.scoreboard.op_col_x_out_fire := issueOperand.io.op_col_x_out_fire
  io.scoreboard.op_col_x_out_wid := issueOperand.io.op_col_x_out_wid
  io.scoreboard.op_col_v_in_fire := issueOperand.io.op_col_v_in_fire
  io.scoreboard.op_col_v_in_wid := issueOperand.io.op_col_v_in_wid
  io.scoreboard.op_col_v_out_fire := issueOperand.io.op_col_v_out_fire
  io.scoreboard.op_col_v_out_wid := issueOperand.io.op_col_v_out_wid
  io.scoreboard.branch_fire := io.branch_out.fire
  io.scoreboard.branch_wid := io.branch_out.bits.wid(localDepthWarp - 1, 0)
  io.scoreboard.simt_complete_fire := io.simt_complete.valid
  io.scoreboard.simt_complete_wid := io.simt_complete.bits
  io.scoreboard.endprg_fire := io.warp_lifecycle.fire && io.warp_lifecycle.bits.ctrl.simt_stack_op
  io.scoreboard.endprg_wid := io.warp_lifecycle.bits.ctrl.wid(localDepthWarp - 1, 0)
}
