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
import java.io._

class stackEntry extends Bundle{
  val reconPC = UInt(32.W)
  val jumpPC = UInt(32.W)
  val newMask = UInt(num_thread.W)
}

class branch_join_stack(val depth:Int) extends Module{
  val io = IO(new Bundle {
    val push = Input(Bool())
    val pop = Input(Bool())
    val pushData = Input(new stackEntry())
    val threadMask = Input(UInt(num_thread.W))
    val PCexecute = Input(UInt(32.W))
    val jump = Output(Bool())
    val newPC = Output(UInt(32.W))
    val newMask = Output(UInt(num_thread.W))
  })
  val rd_ptr  = RegInit(0.U(log2Ceil(depth+1).W))
  val wr_ptr  = RegInit(0.U(log2Ceil(depth+1).W))
  val wr_ptr_add1 = Wire(UInt(log2Ceil(depth+1).W))
  val stack_mem = Mem(depth,new stackEntry())
  val is_pop = Wire(Bool())
  val is_pop_pc = Wire(Bool())
  val is_pop_underflow = Wire(Bool())
  is_pop_pc := stack_mem(rd_ptr).reconPC === io.PCexecute //&& (wr_ptr =/= 0.U) //(log2Ceil(depth+1).W)) // when TOS reconvergence PC = executing PC, can pop the entry
  is_pop_underflow := wr_ptr === 0.U //(log2Ceil(depth+1).W)) // when TOS reconvergence PC = executing PC, can pop the entry
  is_pop := is_pop_pc && !is_pop_underflow
  io.jump := is_pop && io.pop                          // else when they don't match, do nothing

  wr_ptr_add1 := wr_ptr + 1.U
  when(io.push){
    rd_ptr := wr_ptr + 1.U
    wr_ptr := wr_ptr + 2.U
    stack_mem(wr_ptr_add1).reconPC := io.pushData.reconPC
    stack_mem(wr_ptr_add1).jumpPC  := io.pushData.jumpPC
    stack_mem(wr_ptr_add1).newMask := io.pushData.newMask
    stack_mem(wr_ptr).reconPC      := io.pushData.reconPC
    stack_mem(wr_ptr).jumpPC       := io.pushData.reconPC
    stack_mem(wr_ptr).newMask      := io.threadMask
  }.elsewhen(io.jump){
    wr_ptr := wr_ptr - 1.U
    rd_ptr := rd_ptr - 1.U
  }
  io.newPC := stack_mem(rd_ptr).jumpPC
  io.newMask := stack_mem(rd_ptr).newMask
}

class branch_join(val depth_stack: Int) extends Module{
  val io = IO(new Bundle() {
    val branch_ctl = Flipped(Decoupled(new simtExeData()))
    val if_mask = Flipped(Decoupled(new vec_alu_bus()))
    val pc_reconv = Flipped(Decoupled(UInt(xLen.W)))
    val input_wid = Input(UInt(depth_warp.W))
    val out_mask = Output(UInt(num_thread.W))
    val complete = Valid(UInt(depth_warp.W))
    val fetch_ctl = Decoupled(new BranchCtrl())
    val CurSig = Output(UInt(sig_length.W))
    val PrevSig = Output(UInt(sig_length.W))
    val missTableTrigger = Output(Bool())
  })
  val opcode = Wire(UInt(1.W))
  val warp_id = Wire(UInt(depth_warp.W))
  val PC_branch = Wire(UInt(32.W))
  val PC_reconv = Wire(UInt(32.W))
  val PC_execute = Wire(UInt(32.W))

  val branch_ctl_buf = Queue(io.branch_ctl, 1, flow = true)
  val if_mask_buf = Queue(io.if_mask, 0)
  val PC_reconv_buf = Queue(io.pc_reconv, 1, flow = true)

  val fetch_ctl_buf = Module(new Queue(new BranchCtrl, 1, flow = true))

  val thread_masks = RegInit(VecInit(Seq.fill(num_warp)(~0.U(num_thread.W))))
  val if_mask = Wire(UInt(num_thread.W))
  // val if_mask = WireInit(~0.U(num_thread.W))
  val else_mask = WireInit(0.U(num_thread.W))
  val ifOnly = Wire(Bool())
  val elseOnly = Wire(Bool())
  val divOccur = Wire(Bool())   // indicate that true divergence occurs, that is, nether ifmask nor elsemask is all zero
  val pushentry = Wire(new stackEntry())
  val ifCnt = Wire(UInt(log2Ceil(num_thread+1).W))
  val elseCnt = Wire(UInt(log2Ceil(num_thread+1).W))
  val takeif = Wire(Bool())

  val push = WireInit(VecInit(Seq.fill(num_warp)(false.B))) //Wire(Vec(num_warp,Bool()))
  val pop = WireInit(VecInit(Seq.fill(num_warp)(false.B))) //Wire(Vec(num_warp,Bool()))
  val bjjump = WireInit(VecInit(Seq.fill(num_warp)(false.B)))
  val bjPC = WireInit(VecInit(Seq.fill(num_warp)(0.U(32.W))))
  val bjmask = WireInit(VecInit(Seq.fill(num_warp)(0.U(num_thread.W))))
  val popjump = Wire(Bool())
  val popPC = Wire(UInt(32.W))
  val popMask = Wire(UInt(num_thread.W))


  val bjstack  = VecInit(Seq.fill(num_warp)(Module(new branch_join_stack(depth_stack)).io))
  var x = 0
  opcode := branch_ctl_buf.bits.opcode
  PC_branch := branch_ctl_buf.bits.PC_branch
  PC_execute := branch_ctl_buf.bits.PC_execute
  warp_id  := branch_ctl_buf.bits.wid
  if_mask_buf.ready := fetch_ctl_buf.io.enq.ready //true.B//if_mask_buf.valid
  //PC_reconv_buf.ready := fetch_ctl_buf.io.enq.ready
  io.complete.valid := (if_mask_buf.fire() & opcode === 0.U & branch_ctl_buf.valid & takeif)

  io.complete.bits := branch_ctl_buf.bits.wid

  when(fetch_ctl_buf.io.enq.ready) {
    when(branch_ctl_buf.valid & (opcode === 0.U) & (branch_ctl_buf.bits.wid === if_mask_buf.bits.wid)) {
      branch_ctl_buf.ready := if_mask_buf.fire()
      PC_reconv_buf.ready := if_mask_buf.fire()
    }.elsewhen(branch_ctl_buf.valid & (opcode === 1.U)) {
      branch_ctl_buf.ready := true.B
      PC_reconv_buf.ready := true.B
    }.otherwise {
      branch_ctl_buf.ready := false.B
      PC_reconv_buf.ready := false.B
    }
  }.otherwise {
    branch_ctl_buf.ready := false.B
    PC_reconv_buf.ready := false.B
  }

  PC_reconv := PC_reconv_buf.bits
  if_mask := if_mask_buf.bits.if_mask & branch_ctl_buf.bits.mask_init
  else_mask := (~if_mask_buf.bits.if_mask).asUInt() & branch_ctl_buf.bits.mask_init
  ifCnt := PopCount(if_mask)
  elseCnt := PopCount(else_mask)
  takeif := ((ifCnt < elseCnt) && divOccur) || ifOnly  // take if first if thread take if path is less than thread take else
  ifOnly :=   else_mask === 0.U
  elseOnly := if_mask.asUInt() === 0.U
  divOccur := ~(ifOnly | elseOnly)
  pushentry.reconPC := PC_reconv
  when(takeif){
    pushentry.newMask := else_mask
    pushentry.jumpPC  := PC_branch
  }.otherwise{
    pushentry.newMask := if_mask
    pushentry.jumpPC  :=  PC_execute + 4.U
  }

  for(x <- 0 until num_warp){
    push(x)  :=  (opcode === 0.U) && (branch_ctl_buf.fire()) && (x.asUInt() === warp_id ) && divOccur
    pop(x) := (opcode === 1.U) && (branch_ctl_buf.fire()) && (x.asUInt() === warp_id) //just indicating this is join, maybe not pop, depends on whether TOS rPC match current executing PC
    bjstack(x).push := push(x)
    bjstack(x).pop := pop(x)
    bjstack(x).threadMask := thread_masks(x)
    bjstack(x).pushData := pushentry
    bjstack(x).PCexecute := PC_execute
    bjjump(x) := bjstack(x).jump
    bjPC(x) := bjstack(x).newPC
    bjmask(x) := bjstack(x).newMask
  }
  popjump := bjjump(warp_id)
  popPC  := bjPC(warp_id)
  popMask := bjmask(warp_id)


  //******  output fetch control  ********
  //issue fetch request when: 1/ branch happen and take else path  2/ join happen and jump indeed happen
  val fetch_ctl = Wire(new BranchCtrl)

  val fetch_ctl_valid = Wire(Bool())
  fetch_ctl.wid := 0.U
  fetch_ctl.new_pc := 0.U
  fetch_ctl.jump := false.B
  fetch_ctl_valid := false.B
  fetch_ctl.wid := warp_id
  when(opcode === 1.U && branch_ctl_buf.valid ){
    when(popjump){
      fetch_ctl.new_pc := popPC
      fetch_ctl.jump := true.B
      fetch_ctl_valid := true.B
    }.otherwise{
      fetch_ctl.new_pc := 0.U
      fetch_ctl.jump := false.B
      fetch_ctl_valid := true.B
    }

  }.elsewhen(opcode === 0.U && branch_ctl_buf.valid && if_mask_buf.valid && ((!takeif) || elseOnly) ){
    fetch_ctl.new_pc := PC_branch
    fetch_ctl.jump := true.B
    fetch_ctl_valid := true.B
  }
  fetch_ctl_buf.io.enq.bits := fetch_ctl
  fetch_ctl_buf.io.enq.valid := fetch_ctl_valid
  io.fetch_ctl <> fetch_ctl_buf.io.deq
  if (SPIKE_OUTPUT) {
    fetch_ctl.spike_info.get := branch_ctl_buf.bits.spike_info.get
    when(io.complete.valid /*&&io.complete.bits===wid_to_check.U*/ && !io.branch_ctl.fire) {
      printf(p"sm ${branch_ctl_buf.bits.spike_info.get.sm_id} warp ${Decimal(io.complete.bits)} 0x${Hexadecimal(branch_ctl_buf.bits.spike_info.get.pc)} 0x${Hexadecimal(branch_ctl_buf.bits.spike_info.get.inst)}")
      when(branch_ctl_buf.bits.opcode === 0.U){
        printf(p" vbranch     current mask and npc:   ")
        when(takeif){
          if_mask.asTypeOf(Vec(num_thread, Bool())).reverse.foreach(x => printf(p"${Hexadecimal(x.asUInt)}"))
          printf(p"    0x${Hexadecimal(branch_ctl_buf.bits.spike_info.get.pc + 4.U)}")
        }.otherwise{
          else_mask.asTypeOf(Vec(num_thread, Bool())).reverse.foreach(x => printf(p"${Hexadecimal(x.asUInt)}"))
          printf(p"    0x${Hexadecimal(PC_branch)}")
        }
      }
      printf(p"\n")
    }
    when(branch_ctl_buf.bits.opcode === 1.U && branch_ctl_buf.valid ) {
      printf(p"sm ${branch_ctl_buf.bits.spike_info.get.sm_id} warp ${Decimal(io.complete.bits)} 0x${Hexadecimal(branch_ctl_buf.bits.spike_info.get.pc)} 0x${Hexadecimal(branch_ctl_buf.bits.spike_info.get.inst)}")
      printf(p" join    mask and npc:    ")
      popMask.asTypeOf(Vec(num_thread, Bool())).reverse.foreach(x => printf(p"${Hexadecimal(x.asUInt)}"))
      printf(p" 0x${Hexadecimal(popPC)}")
      printf(p" pop stack ? ${Decimal(popjump)}")
      printf(p"\n")
    }

    //if_mask.asTypeOf(Vec(num_thread, Bool())).reverse.foreach(x => printf(p"${Hexadecimal(x.asUInt)}"))
  }

  //***** thread mask register control******
  //when branch indeed happened, put executing mask into corresponding register
  //when join indeed happened, put new mask into corresponding register
  when(if_mask_buf.fire()){
    when(divOccur){
      when(takeif){
        thread_masks(warp_id) := if_mask
      }.otherwise{
        thread_masks(warp_id) := else_mask
      }
    }
  }.elsewhen(opcode === 1.U && branch_ctl_buf.valid && popjump){
    thread_masks(warp_id) := popMask
  }
  io.out_mask := thread_masks(io.input_wid)
  // prefetch invalidate
  io.CurSig := 0.U
  io.PrevSig := 0.U
  io.missTableTrigger := false.B
}

