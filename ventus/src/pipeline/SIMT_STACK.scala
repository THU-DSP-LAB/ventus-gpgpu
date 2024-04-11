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

class hash(val width:Int) extends Module{
  val io = IO(new Bundle() {
    val in1 = Input(UInt(width.W))
    val in2 = Input(UInt(width.W))
    val out = Output(UInt(width.W))
  })
  io.out := io.in1 ^ io.in2
  //change to other hash function if needed
}
//STACK used in SIMT-STACK
class ipdom_stack(val width:Int,val depth:Int) extends Module{//width = wtm+wpc  depth = num of warp
  val io = IO(new Bundle {
    val push  = Input(Bool())
    val pop   = Input(Bool())
    val pair  = Input(Bool())//whether there're q_end&q_else in one entry(diverge or not) ~no else
    val branchImm = Input(Bool())//no if
    val q1    = Input(UInt(width.W))
    val q2    = Input(UInt(width.W))
    val d     = Output(UInt(width.W))
    val index = Output(Bool())
    val empty = Output(Bool())
    val pairo = Output(Bool())
    val CurSig = Output(UInt(sig_length.W))
    val prevSig = Output(UInt(sig_length.W))
    // val dout  = Output(UInt((width*2).W))

    //    val elseo = Output(Bool())
  })
  val is_part = RegInit(VecInit(Seq.fill(depth)(0.U(1.W))))
  val rd_ptr  = RegInit(0.U(log2Ceil(depth+1).W))
  val wr_ptr  = RegInit(0.U(log2Ceil(depth+1).W))
  val dout    = WireInit(0.U((width*2).W))
  val stack_mem = Mem(depth, UInt((width*2).W))
  val diverge    = Wire(Bool())
  val pair_mem = RegInit(VecInit(Seq.fill(depth)(true.B)))
  //  val BranchImm_mem = RegInit(VecInit(Seq.fill(depth)(false.B)))
  val PCbranch = Wire(UInt(32.W))
  val PrevSig = RegInit(0.U(sig_length.W))
  val CurSig = RegInit(0.U(sig_length.W))
  val newSig = Wire(UInt(sig_length.W))
  val hashHis = Module(new hash(sig_length))

  diverge := !io.pair | (io.branchImm)

  when(io.push ){
    rd_ptr  := wr_ptr
    wr_ptr  := wr_ptr + 1.U
    is_part(wr_ptr) := diverge
    pair_mem(wr_ptr) := io.pair
    //  BranchImm_mem(wr_ptr) := io.branchImm

    stack_mem(wr_ptr) := Cat(io.q1,io.q2)
  } .elsewhen(io.pop) {
    wr_ptr  := wr_ptr - is_part(rd_ptr)
    rd_ptr  := rd_ptr - is_part(rd_ptr)
    is_part(rd_ptr) := 1.U(1.W)

  }

  dout    := stack_mem(rd_ptr)
  io.index := is_part(rd_ptr).asBool()
  io.d     := Mux(io.index,dout(width*2-1,width),dout(width,0))//index=0, diverge path
  io.empty := wr_ptr===0.U
  io.pairo := pair_mem(rd_ptr)
  // io.elseo := BranchImm_mem(rd_ptr)
  //io.dout := dout
  //generate signature
  PCbranch := io.q2(width-1,width-32)
  newSig := Cat(dout(width,width-31),io.pairo & io.index)
  hashHis.io.in1 := CurSig
  hashHis.io.in2 := newSig
  val hashout = Wire(UInt(33.W))
  hashout := hashHis.io.out
  //CurSig := hashout
  val trigger = RegInit(false.B)
  trigger := io.push | io.pop
  when(trigger){
    CurSig := hashout
    PrevSig := CurSig
  }
  io.CurSig := CurSig
  io.prevSig := PrevSig
}

class simtExeData extends Bundle{
  val opcode  = UInt(1.W)
  val wid     = UInt(depth_warp.W)
  val PC_branch = UInt(32.W)
  //val PC_reconv = UInt(32.W)
  val PC_execute = UInt(32.W)
  val mask_init = UInt(num_thread.W)
  val spike_info = if (SPIKE_OUTPUT) Some(new InstWriteBack) else None
}
//TODO: what's the difference between simtExeData and BranchCtrl
//BranchCtrl is just for branch, doesn't need PC reconvergence

class vec_alu_bus() extends Bundle{
  val if_mask =  UInt(num_thread.W)
  val wid     =  UInt(depth_warp.W)
}
/*
class BranchCtrl extends Bundle{
  val wid=UInt(depth_warp.W)
  val jump=Bool()
  val new_pc=UInt(32.W)
}*/

//this is the REAL SIMT-STACK for divergence management
class SIMT_STACK(val depth_stack : Int) extends Module{
  val io = IO( new Bundle() {
    val  branch_ctl  =  Flipped(Decoupled(new simtExeData()))
    val  if_mask     =  Flipped(Decoupled(new vec_alu_bus()))
    val  input_wid   =  Input(UInt(depth_warp.W))
    val  out_mask    =  Output(UInt(num_thread.W))
    val  complete   =  Valid(UInt(depth_warp.W))
    val  fetch_ctl   =  Decoupled(new BranchCtrl())
    val  CurSig = Output(UInt(sig_length.W))
    val PrevSig = Output(UInt(sig_length.W))
    val missTableTrigger = Output(Bool())
  })
  val opcode       = Wire(UInt(1.W))
  val warp_id      = Wire(UInt(depth_warp.W))
  val PC_branch    = Wire(UInt(32.W))
  val join_index   = Wire(UInt(1.W))
  val join_pc      = Wire(UInt(32.W))
  val join_tm      = Wire(UInt(num_thread.W))
  val join_pair    = Wire(Bool())
  // val join_else   = Wire(Bool())
  val branch_ctl_buf=Queue(io.branch_ctl,1,flow=true)
  val if_mask_buf=Queue(io.if_mask,0)
  val fetch_ctl_buf = Module(new Queue(new BranchCtrl,1,flow=true))
  //val fetch_ctl_buf=Module(new Queue(new BranchCtrl,1,pipe=true))


  val thread_masks = RegInit(VecInit(Seq.fill(num_warp)(~0.U(num_thread.W))))  //thread masks for warps
  //val issue_stall  = RegInit(VecInit(Seq.fill(num_warp)(0.U(1.W))))
  //val issue_stall  =  Wire(UInt(num_warp.W))
  val if_mask      = WireInit(~0.U(num_thread.W))
  val else_mask    = WireInit(0.U(num_thread.W))
  //signals connected with ipdom stack
  val push         = WireInit(VecInit(Seq.fill(num_warp)(false.B)))//Wire(Vec(num_warp,Bool()))
  val pop          = WireInit(VecInit(Seq.fill(num_warp)(false.B)))//Wire(Vec(num_warp,Bool()))
  val diverge      = Wire(UInt(1.W))
  val q_end        = WireInit(VecInit(Seq.fill(num_warp)(0.U((32+num_thread).W))))//Wire(Vec(num_warp,UInt((32+num_thread).W)))
  val q_else       = WireInit(VecInit(Seq.fill(num_warp)(0.U((32+num_thread).W))))//Wire(Vec(num_warp,UInt((32+num_thread).W)))
  val ipdom_data   = WireInit(VecInit(Seq.fill(num_warp)(0.U((32+num_thread).W))))//Wire(Vec(num_warp,UInt((num_thread+32).W)))
  val ipdom_index  = WireInit(VecInit(Seq.fill(num_warp)(false.B)))//Wire(Vec(num_warp,Bool()))
  val ipdom_empty  = WireInit(VecInit(Seq.fill(num_warp)(true.B)))
  val ipdom_pair   = WireInit(VecInit(Seq.fill(num_warp)(true.B)))
  val ipdom_dout   = WireInit(VecInit(Seq.fill(num_warp)(0.U(((32+num_thread)*2).W))))
  val ipdom_curSig  = WireInit(VecInit(Seq.fill(num_warp)(0.U(33.W))))
  val ipdom_prevSig  = WireInit(VecInit(Seq.fill(num_warp)(0.U(33.W))))

  //  val ipdom_else   = WireInit(VecInit(Seq.fill(num_warp)(false.B)))

  var x = 0

  val ipdom_stack  = VecInit(Seq.fill(num_warp)(Module(new ipdom_stack(num_thread+32,depth_stack)).io))
  //branch_ctl signals, from ISSUE
  opcode := branch_ctl_buf.bits.opcode
  PC_branch := branch_ctl_buf.bits.PC_branch
  warp_id  := branch_ctl_buf.bits.wid
  when(fetch_ctl_buf.io.enq.ready){
  when(branch_ctl_buf.valid & (opcode === 0.U) & (branch_ctl_buf.bits.wid === if_mask_buf.bits.wid)) {
    branch_ctl_buf.ready := if_mask_buf.fire()
    //when(opcode === 0.U) {
     // when(branch_ctl_buf.bits.wid === if_mask_buf.bits.wid) {
    //    branch_ctl_buf.ready := if_mask_buf.fire()
     // }.otherwise {
     //   branch_ctl_buf.ready := false.B
      //}
    //}.elsewhen(opcode === 1.U) {
    //  branch_ctl_buf.ready := io.fetch_ctl.fire()
   // }.otherwise {
   //   branch_ctl_buf.ready := true.B//branch_ctl_buf.valid
   // }
  }.elsewhen(branch_ctl_buf.valid & (opcode === 1.U)){
    branch_ctl_buf.ready := true.B
  }.otherwise{branch_ctl_buf.ready:=false.B}}.otherwise{
    branch_ctl_buf.ready := false.B
  }

  val elseOnly = Wire(Bool())
  if_mask  := if_mask_buf.bits.if_mask &branch_ctl_buf.bits.mask_init
  else_mask:= (~if_mask_buf.bits.if_mask).asUInt()&branch_ctl_buf.bits.mask_init
  diverge  := true.B //~no else & thread_masks(warp_id).asUInt()
  elseOnly := false.B
  when(if_mask_buf.fire() ){
    elseOnly:= else_mask === thread_masks(warp_id).asUInt()
    diverge := ((else_mask& thread_masks(warp_id).asUInt())  =/= 0.U)//& thread_masks(warp_id).asUInt()
  }
  // elseOnly := if_mask.asUInt() === 0.U //no if
  if_mask_buf.ready := fetch_ctl_buf.io.enq.ready//true.B//if_mask_buf.valid
  io.complete.valid:=(if_mask_buf.fire()&opcode===0.U&branch_ctl_buf.valid&(!elseOnly))
  io.complete.bits:=branch_ctl_buf.bits.wid

  for(x <- 0 until num_warp ){
    push(x) := (opcode === 0.U) && (branch_ctl_buf.fire()) && (x.asUInt() === warp_id)
    pop(x)  := (opcode === 1.U) && (branch_ctl_buf.fire()) && (x.asUInt() === warp_id)
    // when(x.asUInt() === warp_id) {
    //   //split or join will cause issue stall
    //   when(opcode === 1.U) {
    //     when(if_mask_buf.ready) {
    //       issue_stall(x) := 0.U
    //     }.otherwise {
    //       issue_stall(x) := 1.U
    //     }
    //   }.elsewhen(opcode === 2.U) {
    //     when(io.fetch_ctl.fire()) {
    //       issue_stall(x) := 0.U
    //     }.otherwise {
    //       issue_stall(x) := 1.U
    //     }
    //   }
    // }
    q_end(x)   :=  Cat(0.U(32.W),thread_masks(warp_id))
    q_else(x)  :=  Cat(PC_branch,else_mask & thread_masks(warp_id).asUInt())

    //ipdom stack connection
    ipdom_stack(x).push := push(x)
    ipdom_stack(x).pop  := pop(x)
    ipdom_stack(x).pair := diverge
    ipdom_stack(x).branchImm := elseOnly
    ipdom_stack(x).q1   := q_end(x)
    ipdom_stack(x).q2   := q_else(x)
    ipdom_data(x)       := ipdom_stack(x).d
    ipdom_index(x)      := ~ipdom_stack(x).index
    ipdom_empty(x)      := ipdom_stack(x).empty
    ipdom_pair(x)       := ipdom_stack(x).pairo
    ipdom_curSig(x)     := ipdom_stack(x).CurSig
    ipdom_prevSig(x)    := ipdom_stack(x).prevSig
    //ipdom_dout(x)       := ipdom_stack(x).dout

    // ipdom_else(x)  := ipdom_stack(x).elseo
  }
  // val Sig = WireInit(0.U(33.W))
  // val CPC = WireInit(0.U(32.W))
  // val mark = WireInit(0.U(1.W))
  //CPC := ipdom_dout(warp_id)(32+num_thread-1,num_thread)
  join_pc   :=  ipdom_data(warp_id)(32+num_thread-1,num_thread)
  join_tm   :=  ipdom_data(warp_id)(num_thread-1,0)
  join_index:=  ipdom_index(warp_id)
  join_pair :=  ipdom_pair(warp_id)
  //mark := join_pair & (~join_index)
  //Sig := Cat(CPC,mark) //PC branch | join_pair & (~join_index)

  //join_else := ipdom_else(warp_id)
  val fetch_ctl = Wire(new BranchCtrl)
  val fetch_ctl_valid = Wire(Bool())
  fetch_ctl.wid := 0.U
  fetch_ctl.new_pc := 0.U
  fetch_ctl.jump := false.B
  fetch_ctl_valid := false.B
  when(opcode === 1.U && branch_ctl_buf.valid){
    when(join_index === 1.U){
      fetch_ctl.new_pc := join_pc
      fetch_ctl.jump := 1.U
    }.elsewhen(!join_pair){
      fetch_ctl.jump := 1.U
      fetch_ctl.new_pc := branch_ctl_buf.bits.PC_branch
    }
    fetch_ctl_valid := branch_ctl_buf.valid
    fetch_ctl.wid := warp_id
  }.elsewhen(opcode === 0.U && branch_ctl_buf.valid){
    when(elseOnly){
      fetch_ctl.jump := 1.U
      fetch_ctl_valid := branch_ctl_buf.valid
      fetch_ctl.wid := warp_id
      fetch_ctl.new_pc := branch_ctl_buf.bits.PC_branch
    }
  }
  if(SPIKE_OUTPUT){
    fetch_ctl.spike_info.get:=branch_ctl_buf.bits.spike_info.get
    when(io.complete.valid/*&&io.complete.bits===wid_to_check.U*/&& !io.branch_ctl.fire){
      printf(p"sm ${branch_ctl_buf.bits.spike_info.get.sm_id} warp ${Decimal(io.complete.bits)} ${Hexadecimal(branch_ctl_buf.bits.spike_info.get.pc)} 0x${Hexadecimal(branch_ctl_buf.bits.spike_info.get.inst)}")
      printf(p" simt ")
      if_mask.asTypeOf(Vec(num_thread,Bool())).reverse.foreach(x=>printf(p"${Hexadecimal(x.asUInt)}"))
      printf(p"\n")
    }
  }
  fetch_ctl_buf.io.enq.bits := fetch_ctl
  fetch_ctl_buf.io.enq.valid := fetch_ctl_valid
  io.fetch_ctl <> fetch_ctl_buf.io.deq

  when(if_mask_buf.fire()){
    when(!elseOnly) {
      thread_masks(warp_id) := if_mask
    }.otherwise{
      thread_masks(warp_id) := else_mask & thread_masks(warp_id).asUInt()
    }
  }.elsewhen(opcode === 1.U & branch_ctl_buf.valid){
    thread_masks(warp_id) := join_tm
  }

  io.out_mask  := thread_masks(io.input_wid)
  io.CurSig := ipdom_curSig(warp_id)
  io.PrevSig := ipdom_prevSig(warp_id)
  //TODO:太凑了
  val CurSig_R = RegNext(ipdom_curSig(warp_id))
 // val missTableTrigger1 = RegNext(branch_ctl_buf.valid)
  //val missTableTrigger = RegNext(missTableTrigger1)

  io.missTableTrigger := ipdom_curSig(warp_id) =/= CurSig_R
  //io.OutSig := Sig
}
/*
//STACK used in SIMT-STACK
class ipdom_stack(val width:Int,val depth:Int) extends Module{//width = wtm+wpc  depth = num of warp
  val io = IO(new Bundle {
    val push  = Input(Bool())
    val pop   = Input(Bool())
    val pair  = Input(Bool())//whether there're q_end&q_else in one entry(diverge or not) ~no else
    val branchImm = Input(Bool())//no if
    val q1    = Input(UInt(width.W))
    val q2    = Input(UInt(width.W))
    val d     = Output(UInt(width.W))
    val index = Output(Bool())
    val empty = Output(Bool())
    val pairo = Output(Bool())
//    val elseo = Output(Bool())
  })
  val is_part = RegInit(VecInit(Seq.fill(depth)(0.U(1.W))))
  val rd_ptr  = RegInit(0.U(log2Ceil(depth+1).W))
  val wr_ptr  = RegInit(0.U(log2Ceil(depth+1).W))
  val dout    = WireInit(0.U((width*2).W))
  val stack_mem = Mem(depth, UInt((width*2).W))
  val diverge    = Wire(Bool())
  val pair_mem = RegInit(VecInit(Seq.fill(depth)(true.B)))
//  val BranchImm_mem = RegInit(VecInit(Seq.fill(depth)(false.B)))

  diverge := !io.pair | (io.branchImm)

  when(io.push ){
    rd_ptr  := wr_ptr
    wr_ptr  := wr_ptr + 1.U
    is_part(wr_ptr) := diverge
    pair_mem(wr_ptr) := io.pair
  //  BranchImm_mem(wr_ptr) := io.branchImm

    stack_mem(wr_ptr) := Cat(io.q1,io.q2)
  } .elsewhen(io.pop) {
    wr_ptr  := wr_ptr - is_part(rd_ptr)
    rd_ptr  := rd_ptr - is_part(rd_ptr)
    is_part(rd_ptr) := 1.U(1.W)

  }


  dout    := stack_mem(rd_ptr)
  io.index := is_part(rd_ptr).asBool()
  io.d     := Mux(io.index,dout(width*2-1,width),dout(width,0))//index=0, diverge path
  io.empty := wr_ptr===0.U
  io.pairo := pair_mem(rd_ptr)
 // io.elseo := BranchImm_mem(rd_ptr)
}

class simtExeData extends Bundle{
  val opcode  = UInt(1.W)
  val wid     = UInt(depth_warp.W)
  val PC_branch = UInt(32.W)
  val mask_init = UInt(num_thread.W)
}

class vec_alu_bus() extends Bundle{
  val if_mask =  UInt(num_thread.W)
  val wid     =  UInt(depth_warp.W)
}


//this is the REAL SIMT-STACK for divergence management
class SIMT_STACK(val depth_stack : Int) extends Module{
  val io = IO( new Bundle() {
    val  branch_ctl  =  Flipped(Decoupled(new simtExeData()))
    val  if_mask     =  Flipped(Decoupled(new vec_alu_bus()))
    val  input_wid   =  Input(UInt(depth_warp.W))
    val  out_mask    =  Output(UInt(num_thread.W))
    val  complete   =  Valid(UInt(depth_warp.W))
    val  fetch_ctl   =  Decoupled(new BranchCtrl())
  })
  val opcode       = Wire(UInt(1.W))
  val warp_id      = Wire(UInt(depth_warp.W))
  val PC_branch    = Wire(UInt(32.W))
  val join_index   = Wire(UInt(1.W))
  val join_pc      = Wire(UInt(32.W))
  val join_tm      = Wire(UInt(num_thread.W))
  val join_pair    = Wire(Bool())
 // val join_else   = Wire(Bool())
  val branch_ctl_buf=Queue(io.branch_ctl,1,flow=true)
  val if_mask_buf=Queue(io.if_mask,0)
  //val fetch_ctl_buf=Module(new Queue(new BranchCtrl,1,pipe=true))


  val thread_masks = RegInit(VecInit(Seq.fill(num_warp)(~0.U(num_thread.W))))  //thread masks for warps
  //val issue_stall  = RegInit(VecInit(Seq.fill(num_warp)(0.U(1.W))))
  //val issue_stall  =  Wire(UInt(num_warp.W))
  val if_mask      = WireInit(~0.U(num_thread.W))
  val else_mask    = WireInit(0.U(num_thread.W))
//signals connected with ipdom stack
  val push         = WireInit(VecInit(Seq.fill(num_warp)(false.B)))//Wire(Vec(num_warp,Bool()))
  val pop          = WireInit(VecInit(Seq.fill(num_warp)(false.B)))//Wire(Vec(num_warp,Bool()))
  val diverge      = Wire(UInt(1.W))
  val q_end        = WireInit(VecInit(Seq.fill(num_warp)(0.U((32+num_thread).W))))//Wire(Vec(num_warp,UInt((32+num_thread).W)))
  val q_else       = WireInit(VecInit(Seq.fill(num_warp)(0.U((32+num_thread).W))))//Wire(Vec(num_warp,UInt((32+num_thread).W)))
  val ipdom_data   = WireInit(VecInit(Seq.fill(num_warp)(0.U((32+num_thread).W))))//Wire(Vec(num_warp,UInt((num_thread+32).W)))
  val ipdom_index  = WireInit(VecInit(Seq.fill(num_warp)(false.B)))//Wire(Vec(num_warp,Bool()))
  val ipdom_empty  = WireInit(VecInit(Seq.fill(num_warp)(true.B)))
  val ipdom_pair   = WireInit(VecInit(Seq.fill(num_warp)(true.B)))
//  val ipdom_else   = WireInit(VecInit(Seq.fill(num_warp)(false.B)))

  var x = 0

  val ipdom_stack  = VecInit(Seq.fill(num_warp)(Module(new ipdom_stack(num_thread+32,depth_stack)).io))
  //branch_ctl signals, from ISSUE
  opcode := branch_ctl_buf.bits.opcode
  PC_branch := branch_ctl_buf.bits.PC_branch
  warp_id  := branch_ctl_buf.bits.wid
  when(branch_ctl_buf.valid) {
    when(opcode === 0.U) {
      when(branch_ctl_buf.bits.wid === if_mask_buf.bits.wid) {
        branch_ctl_buf.ready := if_mask_buf.fire()
      }.otherwise {
        branch_ctl_buf.ready := false.B
      }
    }.elsewhen(opcode === 1.U) {
      branch_ctl_buf.ready := io.fetch_ctl.fire()
    }.otherwise {
      branch_ctl_buf.ready := true.B//branch_ctl_buf.valid
    }
  }.otherwise{branch_ctl_buf.ready:=true.B}

  val elseOnly = Wire(Bool())
  if_mask  := if_mask_buf.bits.if_mask //&branch_ctl_buf.bits.mask_init
  else_mask:= (~if_mask).asUInt()//&branch_ctl_buf.bits.mask_init
  diverge  := true.B //~no else & thread_masks(warp_id).asUInt()
  elseOnly := false.B
  when(if_mask_buf.valid ){
    elseOnly:= else_mask === thread_masks(warp_id).asUInt()
    diverge := ((else_mask& thread_masks(warp_id).asUInt())  =/= 0.U)//& thread_masks(warp_id).asUInt()
  }
 // elseOnly := if_mask.asUInt() === 0.U //no if
  if_mask_buf.ready := true.B//if_mask_buf.valid
  io.complete.valid:=(if_mask_buf.fire()&opcode===0.U&branch_ctl_buf.valid)
  io.complete.bits:=branch_ctl_buf.bits.wid

  for(x <- 0 until num_warp ){
    push(x) := (opcode === 0.U) && (branch_ctl_buf.fire()) && (x.asUInt() === warp_id)
    pop(x)  := (opcode === 1.U) && (branch_ctl_buf.fire()) && (x.asUInt() === warp_id)
    // when(x.asUInt() === warp_id) {
    //   //split or join will cause issue stall
    //   when(opcode === 1.U) {
    //     when(if_mask_buf.ready) {
    //       issue_stall(x) := 0.U
    //     }.otherwise {
    //       issue_stall(x) := 1.U
    //     }
    //   }.elsewhen(opcode === 2.U) {
    //     when(io.fetch_ctl.fire()) {
    //       issue_stall(x) := 0.U
    //     }.otherwise {
    //       issue_stall(x) := 1.U
    //     }
    //   }
    // }
    q_end(x)   :=  Cat(0.U(32.W),thread_masks(warp_id))
    q_else(x)  :=  Cat(PC_branch,else_mask & thread_masks(warp_id).asUInt())


    //ipdom stack connection
    ipdom_stack(x).push := push(x)
    ipdom_stack(x).pop  := pop(x)
    ipdom_stack(x).pair := diverge
    ipdom_stack(x).branchImm := elseOnly
    ipdom_stack(x).q1   := q_end(x)
    ipdom_stack(x).q2   := q_else(x)
    ipdom_data(x)       := ipdom_stack(x).d
    ipdom_index(x)      := ~ipdom_stack(x).index
    ipdom_empty(x)      := ipdom_stack(x).empty
    ipdom_pair(x)       := ipdom_stack(x).pairo
   // ipdom_else(x)  := ipdom_stack(x).elseo
  }
  join_pc   :=  ipdom_data(warp_id)(32+num_thread-1,num_thread)
  join_tm   :=  ipdom_data(warp_id)(num_thread-1,0)
  join_index:=  ipdom_index(warp_id)
  join_pair :=  ipdom_pair(warp_id)
  //join_else := ipdom_else(warp_id)
  io.fetch_ctl.bits.wid := 0.U
  io.fetch_ctl.bits.new_pc := 0.U
  io.fetch_ctl.bits.jump := false.B
  io.fetch_ctl.valid := false.B
  when(opcode === 1.U && branch_ctl_buf.valid){
    when(join_index === 1.U){
      io.fetch_ctl.bits.new_pc := join_pc
      io.fetch_ctl.bits.jump := 1.U
    }.elsewhen(!join_pair){
      io.fetch_ctl.bits.jump := 1.U
      io.fetch_ctl.bits.new_pc := branch_ctl_buf.bits.PC_branch
    }
    io.fetch_ctl.valid := branch_ctl_buf.valid
    io.fetch_ctl.bits.wid := warp_id
  }.elsewhen(opcode === 0.U && branch_ctl_buf.valid){
    when(elseOnly){
      io.fetch_ctl.bits.jump := 1.U
      io.fetch_ctl.valid := branch_ctl_buf.valid
      io.fetch_ctl.bits.wid := warp_id
      io.fetch_ctl.bits.new_pc := branch_ctl_buf.bits.PC_branch
    }
  }

  when(if_mask_buf.fire()){
    when(!elseOnly) {
      thread_masks(warp_id) := if_mask
    }.otherwise{
      thread_masks(warp_id) := else_mask & thread_masks(warp_id).asUInt()
    }
  }.elsewhen(opcode === 1.U & branch_ctl_buf.valid){
    thread_masks(warp_id) := join_tm
  }

  io.out_mask  := thread_masks(io.input_wid)

}*/

