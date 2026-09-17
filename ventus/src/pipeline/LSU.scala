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

import top.parameters._
import chisel3._
import chisel3.util._
import IDecode._
import lsu.{LsuClientId, LsuClientRequest, LsuClientResponse}
import mmu.SV32.asidLen
import rtcore.RtAbiLayout
import top.cache_spike_info

class toShared extends Bundle{
  val instrId = UInt(log2Up(lsu_nMshrEntry).W)
  val addr = Vec(num_thread, UInt(xLen.W))
  val data = Vec(num_thread, UInt(xLen.W))
  val ctrl = new Bundle{
    val mem_cmd = UInt(2.W)
    val mop = UInt(2.W)
    val isvec = Bool()
  }
  val mask = Vec(num_thread, Bool())
}
class DCachePerLaneAddr extends Bundle{
  val activeMask = Bool()
  val blockOffset = UInt(dcache_BlockOffsetBits.W)
  val wordOffset1H = UInt(BytesOfWord.W)
}

class DCacheCoreReq_np extends Bundle{
  val instrId = UInt(log2Up(lsu_nMshrEntry).W)
//  val isWrite = Bool()
  val tag = UInt(dcache_TagBits.W)
  val setIdx = UInt(dcache_SetIdxBits.W)
  val asid = if(MMU_ENABLED) Some(UInt(asidLen.W)) else None
  val perLaneAddr = Vec(num_thread, new DCachePerLaneAddr)
  val data = Vec(num_thread, UInt(xLen.W))
  val opcode = UInt(3.W)
  val param= UInt(4.W)
  val spike_info=if(SPIKE_OUTPUT) Some(new cache_spike_info(mmu.SV32)) else None
}

class DCacheCoreRsp_np extends Bundle{
  val instrId = UInt(log2Up(lsu_nMshrEntry).W)
  val data = Vec(num_thread, UInt(xLen.W))
//  val ctrl = new Bundle{
//    val mem_cmd = UInt(2.W)
//    val mop = UInt(2.W)
//  }
  val activeMask = Vec(num_thread, Bool())
 // val isWrite = Bool()
}

/** Accepted cacheline-client request awaiting its LSU-private DCache tag. */
class LsuClientDcachePending extends Bundle {
  val request = new LsuClientRequest
  val instrId = UInt(log2Up(lsu_nMshrEntry).W)
}

class ShareMemPerLaneAddr_np extends Bundle{
  val activeMask = Bool()
  val blockOffset = UInt(dcache_BlockOffsetBits.W)
  val wordOffset1H = UInt(BytesOfWord.W)
}
class ShareMemCoreReq_np extends Bundle{
  //val ctrlAddr = new Bundle{
  val instrId = UInt(log2Up(lsu_nMshrEntry).W)
  val isWrite = Bool()//Vec(NLanes, Bool())
  //val tag = UInt(dcache_TagBits.W)
  val setIdx = UInt(log2Ceil(sharedmem_depth).W)
  val perLaneAddr = Vec(num_thread, new ShareMemPerLaneAddr_np)
  val data = Vec(num_thread, UInt(xLen.W))
}

class ShareMemCoreRsp_np extends Bundle{
  val instrId = UInt(log2Up(lsu_nMshrEntry).W)
  val data = Vec(num_thread, UInt(xLen.W))
  val activeMask = Vec(num_thread, Bool())//UInt(NLanes.W)
}

class MshrTag extends Bundle{  // AddrCalculate向MSHR添加记录并获取Tag的接口
  // The physical DCache instrId is allocated by MSHRv2.  client metadata is
  // retained here so completion can be routed without exposing that tag.
  val clientId = UInt(LsuClientId.width.W)
  val clientTag = UInt(lsu.LsuClientTag.width.W)
  val lineBase = UInt(memReqLen.W)
  val coverageMask = UInt(dcache_BlockWords.W)
  val warp_id = UInt(depth_warp.W)
  val wfd = Bool()
  val wxd = Bool()
  val reg_idxw = UInt((regidx_width + regext_width).W)
  val mask = Vec(num_thread, Bool())
  val unsigned = Bool()
  val wordOffset1H = Vec(num_thread, UInt(BytesOfWord.W))
  val isWrite = Bool()
  val spike_info = if (SPIKE_OUTPUT) Some(new InstWriteBack) else None
}

object ByteExtract{
  def apply(isUInt: Bool = true.B, in: UInt = 0.U(xLen.W), sel: UInt = "hf".U(4.W)): UInt = {
    val result = Wire(UInt(32.W))
    result := MuxCase(
      in, Array(
        (sel==="hf".U) -> in,
        (sel==="hc".U) -> Mux(!in(31)||isUInt, Cat(0.U(16.W),in(31,16)), Cat("hffff".U(16.W),in(31,16))),
        (sel==="h3".U) -> Mux(!in(15)||isUInt, Cat(0.U(16.W),in(15,0)), Cat("hffff".U(16.W),in(15,0))),
        (sel==="h8".U) -> Mux(!in(31)||isUInt, Cat(0.U(24.W),in(31,24)), Cat("hffffff".U(24.W),in(31,24))),
        (sel==="h4".U) -> Mux(!in(23)||isUInt, Cat(0.U(24.W),in(23,16)), Cat("hffffff".U(24.W),in(23,16))),
        (sel==="h2".U) -> Mux(!in(15)||isUInt, Cat(0.U(24.W),in(15,8)), Cat("hffffff".U(24.W),in(15,8))),
        (sel==="h1".U) -> Mux(!in(7)||isUInt, Cat(0.U(24.W),in(7,0)), Cat("hffffff".U(24.W),in(7,0)))
      )
    )
    result
  }
}

class AddrCalculate(val sharedmemory_maxsize: UInt = 4096.U(32.W)) extends Module{
  val io = IO(new Bundle{
    val from_fifo = Flipped(DecoupledIO(new vExeData))
    val csr_wid = Output(UInt(depth_warp.W))
    val csr_pds = Input(UInt(xLen.W))
    val csr_numw = Input(UInt(xLen.W))
    val csr_numt = Input(UInt(xLen.W))
    val csr_tid = Input(UInt(xLen.W))
    val to_mshr = DecoupledIO(new Bundle{
      val tag = new MshrTag
    })
    val idx_entry = Input(UInt(log2Up(lsu_nMshrEntry).W))
    val flush_dcache = Flipped(DecoupledIO(Bool()))
    val to_dcache = DecoupledIO(new DCacheCoreReq_np)
    val to_shared = DecoupledIO(new ShareMemCoreReq_np)
  })
  val s_idle :: s_save :: s_shared :: s_dcache ::s_dcache_1::s_dcache_2:: Nil = Enum(6)
  val cnt = new Counter(n = num_thread)
  val state = RegInit(init = s_idle)

  val reg_save = RegInit(0.U.asTypeOf(new vExeData))
  val is_flush = RegInit(false.B)
  io.csr_wid:=reg_save.ctrl.wid
  //val rdy_fromFIFO = Reg(Bool())
  io.from_fifo.ready := state===s_idle && !io.flush_dcache.valid
  io.flush_dcache.ready := state === s_idle
  val reg_entryID = RegInit(0.U(log2Up(lsu_nMshrEntry).W))

  val addr = Wire(Vec(num_thread, UInt(xLen.W)))
  val is_shared = Wire(Vec(num_thread, Bool()))
  val all_shared = Wire(Bool())


  // Address Calculate & Analyze, Comb Logic @reg_save
  (0 until num_thread).foreach( x => {
    val privateByteOffset = reg_save.in1(x) + reg_save.in2(x)
    val wordBaseOffset = Cat(privateByteOffset(31,2), 0.U(2.W))
    val byteLaneOffset = Mux(reg_save.ctrl.mem_whb === MEM_W, 0.U, privateByteOffset(1,0))
    val pdsThreadCount = io.csr_numw * io.csr_numt
    val pdsWordStride = wordBaseOffset * pdsThreadCount
    val pdsLaneSlot = Cat(io.csr_tid + x.asUInt, 0.U(2.W))
    addr(x) :=  Mux(reg_save.ctrl.isvec & reg_save.ctrl.disable_mask,
                  Mux(reg_save.ctrl.is_vls12,
                    reg_save.in1(x)+reg_save.in2(x),
                    byteLaneOffset + pdsLaneSlot + io.csr_pds + pdsWordStride
                  ),
                  Mux(reg_save.ctrl.isvec,
                    reg_save.in1(x) + Mux(reg_save.ctrl.mop===0.U,
                      x.asUInt<<2,
                      Mux(reg_save.ctrl.mop(0),
                        reg_save.in2(x),
                        x.asUInt*reg_save.in2(x))
                    ),
                    reg_save.in1(0) + reg_save.in2(0)
                  )
                )
    is_shared(x) := !reg_save.mask(x) || ( addr(x) >= LDS_BASE.U(32.W) && addr(x) < (LDS_BASE.U(32.W) + sharedmemory_maxsize) )
  })
  all_shared := Mux(reg_save.ctrl.isvec,
    is_shared.asUInt.andR, // "AND" Reduce
    is_shared(0)
  )
  val addr_wire=Wire(UInt(xLen.W))
  addr_wire:=addr(PriorityEncoder(reg_save.mask.asUInt))
  val tag = Mux(reg_save.mask.asUInt=/=0.U, addr_wire(xLen-1, xLen-1-dcache_TagBits+1), 0.U(dcache_TagBits.W))
  val setIdx = Mux(reg_save.mask.asUInt=/=0.U, addr_wire(xLen-1-dcache_TagBits, xLen-1-dcache_TagBits-dcache_SetIdxBits+1), 0.U(dcache_SetIdxBits.W))
  val setIdx_shared = Mux(reg_save.mask.asUInt=/=0.U, 
                        addr_wire(log2Ceil(sharedmem_depth) + dcache_BlockOffsetBits + dcache_WordOffsetBits - 1, 
                                  dcache_BlockOffsetBits + dcache_WordOffsetBits), 
                        0.U)
  val blockOffset = Wire(Vec(num_thread, UInt(dcache_BlockOffsetBits.W)))
  (0 until num_thread).foreach( x => blockOffset(x) := addr(x)(10, 2) )
  val wordOffset1H = Wire(Vec(num_thread, UInt(BytesOfWord.W)))
  (0 until num_thread).foreach( x => {
  //DONE: Add Control Signals in vExeData.ctrl and define lw lh lb
    wordOffset1H(x) := 15.U(4.W)
    switch(reg_save.ctrl.mem_whb){
      is(MEM_W) { wordOffset1H(x) := 15.U }
      is(MEM_H) { wordOffset1H(x) :=
        Mux(addr(x)(1)===0.U,
          3.U,
          12.U
        )
      }
      is(MEM_B) { wordOffset1H(x) := 1.U << addr(x)(1,0) }
    }
  })
  //val reg_toMSHR = Reg(new MshrTag)
  //val vld_toMSHR = Reg(Bool())
  io.to_mshr.bits.tag.mask := reg_save.mask
  io.to_mshr.bits.tag.clientId := LsuClientId.Pipe
  io.to_mshr.bits.tag.clientTag := 0.U
  io.to_mshr.bits.tag.lineBase := Cat(addr_wire(memReqLen - 1, 7), 0.U(7.W))
  io.to_mshr.bits.tag.coverageMask := reg_save.mask.asUInt
  io.to_mshr.bits.tag.reg_idxw := reg_save.ctrl.reg_idxw
  io.to_mshr.bits.tag.warp_id := reg_save.ctrl.wid
  io.to_mshr.bits.tag.wxd:=reg_save.ctrl.wxd
  io.to_mshr.bits.tag.wfd:=reg_save.ctrl.wvd
  io.to_mshr.bits.tag.unsigned := reg_save.ctrl.mem_unsigned
  io.to_mshr.bits.tag.wordOffset1H := wordOffset1H
  io.to_mshr.valid := state===s_save && reg_save.ctrl.mem_cmd.orR
  io.to_mshr.bits.tag.isWrite := reg_save.ctrl.mem_cmd(1)
  if(SPIKE_OUTPUT){
    io.to_mshr.bits.tag.spike_info.get:=reg_save.ctrl.spike_info.get
  }
  //val reg_toShared = Reg(new toShared)
  //val vld_toShared = Reg(Bool())
  val data_next=reg_save.in3

  io.to_shared.bits.instrId := reg_entryID
  // |reg_save| -> |addr & mask| -> |PriorityEncoder| -> |tag & idx| -> |io.to_dcache.bits|
  //io.to_shared.bits.tag := tag
  io.to_shared.bits.setIdx := setIdx_shared
  (0 until num_thread).foreach(x => {
    io.to_shared.bits.perLaneAddr(x).blockOffset := blockOffset(x)
    io.to_shared.bits.perLaneAddr(x).wordOffset1H := wordOffset1H(x)
    io.to_shared.bits.perLaneAddr(x).activeMask := reg_save.mask(x) && (addr(x)(xLen-1, xLen-1-dcache_TagBits+1)===tag && addr(x)(xLen-1-dcache_TagBits, xLen-1-dcache_TagBits-dcache_SetIdxBits+1)===setIdx)
  })
  io.to_shared.bits.data := data_next//Mux(reg_save.ctrl.mem_cmd(0).asBool, VecInit(Seq.fill(num_thread)(0.U(xLen.W))), reg_save.in3)
  io.to_shared.bits.isWrite := reg_save.ctrl.mem_cmd(1)
  io.to_shared.valid := state===s_shared

  //val vld_toDCache = Reg(Bool())
  io.to_dcache.bits.instrId := reg_entryID
  if(MMU_ENABLED) {
    io.to_dcache.bits.asid.get := reg_save.ctrl.asid.get
  }

  // |reg_save| -> |addr & mask| -> |PriorityEncoder| -> |tag & idx| -> |io.to_dcache.bits|
  io.to_dcache.bits.tag := tag
  io.to_dcache.bits.setIdx := setIdx
  io.to_dcache.bits.spike_info.foreach{ left =>
    left.pc := reg_save.ctrl.pc
    left.vaddr := addr_wire
  }
  val opcode_wire =Wire(UInt(3.W))
  val param_wire_alt =Wire(UInt(4.W))
  param_wire_alt := Mux(reg_save.ctrl.alu_fn===FN_SWAP,15.U,Mux(reg_save.ctrl.alu_fn===FN_AMOADD,0.U,Mux(reg_save.ctrl.alu_fn===FN_XOR,1.U,
    Mux(reg_save.ctrl.alu_fn===FN_AND,3.U,Mux(reg_save.ctrl.alu_fn===FN_OR,2.U,Mux(reg_save.ctrl.alu_fn===FN_MIN,4.U,
    Mux(reg_save.ctrl.alu_fn===FN_MAX,5.U,Mux(reg_save.ctrl.alu_fn===FN_MINU,6.U,Mux(reg_save.ctrl.alu_fn===FN_MAXU,7.U,1.U)))))))))
  val param_wire=Wire(UInt(4.W))
  when(reg_save.ctrl.atomic){
    when(reg_save.ctrl.aq &&reg_save.ctrl.rl){
      opcode_wire :=Mux(state===s_dcache_2,3.U,Mux(state===s_dcache_1,2.U,3.U))
      param_wire :=Mux(state===s_dcache_2,0.U,Mux(state===s_dcache_1,param_wire_alt,0.U))
    }.elsewhen(reg_save.ctrl.aq){
      opcode_wire :=Mux(state===s_dcache_1,2.U,3.U)
      param_wire :=Mux(state===s_dcache_1,param_wire_alt,0.U)
    }.elsewhen(reg_save.ctrl.rl){
      opcode_wire :=Mux(state===s_dcache_1,3.U,2.U)
      param_wire :=Mux(state===s_dcache_1,0.U,param_wire_alt)
    }.otherwise{
      opcode_wire := Mux(reg_save.ctrl.alu_fn===FN_ADD,reg_save.ctrl.mem_cmd(1),2.U) //todo should consider lr/sc
      param_wire :=param_wire_alt
    }
  }.elsewhen(reg_save.ctrl.fence){
    opcode_wire :=3.U
    param_wire :=0.U
  }.elsewhen(is_flush) { // is_flush: indicate kernel ends! will invalidate L1D instead of flush
    opcode_wire := 3.U
    param_wire := 0.U
  }.otherwise{
    opcode_wire :=reg_save.ctrl.mem_cmd(1)
    param_wire :=0.U
  }
  io.to_dcache.bits.opcode :=opcode_wire// fence=invalidate, atomic will split to maximum 3 instructions
  io.to_dcache.bits.param :=param_wire

  (0 until num_thread).foreach(x => {
    io.to_dcache.bits.perLaneAddr(x).blockOffset := blockOffset(x)
    io.to_dcache.bits.perLaneAddr(x).wordOffset1H := wordOffset1H(x)
    io.to_dcache.bits.perLaneAddr(x).activeMask := Mux(reg_save.ctrl.atomic,reg_save.mask(x)&& (x.asUInt===PriorityEncoder(reg_save.mask.asUInt)),
      reg_save.mask(x) && (addr(x)(xLen-1, xLen-1-dcache_TagBits+1)===tag && addr(x)(xLen-1-dcache_TagBits, xLen-1-dcache_TagBits-dcache_SetIdxBits+1)===setIdx)
    )

  })
  io.to_dcache.bits.data := data_next//Mux(reg_save.ctrl.mem_cmd(0).asBool, VecInit(Seq.fill(num_thread)(0.U(xLen.W))), reg_save.in3)
 // io.to_dcache.bits.isWrite := reg_save.ctrl.mem_cmd(1)
  io.to_dcache.valid := (state===s_dcache) ||(state===s_dcache_1) ||(state===s_dcache_2)
  val mask_next = Wire(Vec(num_thread, Bool()))

  (0 until num_thread).foreach( x => {                          // update mask
    mask_next(x) := Mux(reg_save.ctrl.atomic ,reg_save.mask(x)&& !(x.asUInt===PriorityEncoder(reg_save.mask.asUInt)),reg_save.mask(x) && !(addr(x)(xLen-1, xLen-1-dcache_TagBits+1)===tag && addr(x)(xLen-1-dcache_TagBits, xLen-1-dcache_TagBits-dcache_SetIdxBits+1)===setIdx)
    )})
  // End of Addr Logic

  // FSM State Transfer
  switch(state){
    is (s_idle){
      state := MuxCase(state, Seq(
        io.flush_dcache.fire -> s_dcache,
        io.from_fifo.fire -> s_save,
      ))
      cnt.reset()
    }
    is (s_save){
      when(reg_save.ctrl.mem_cmd.orR){ // read or write
        when(all_shared && !is_flush){ // shared memory
          when(io.to_mshr.fire){state := s_shared}.otherwise{state := s_save}
        }.otherwise{ // dcache
            when(io.to_mshr.fire) {
              state := s_dcache
            }.otherwise {
              state := s_save
            }
          }
      }.otherwise{ state := s_idle }
    }
    is (s_shared){
      when(io.to_shared.fire){
        when(cnt.value>=num_thread.U || mask_next.asUInt===0.U){
          cnt.reset(); state := s_idle
        }.otherwise{
          cnt.inc(); state := s_shared
        }
      }.otherwise{state := s_shared}
    }
    is (s_dcache) {
      when(is_flush) {
        when(io.to_dcache.fire) {
          state := s_idle
        }.otherwise {
          state := state
        }
      }.otherwise {
        when(io.to_dcache.fire) {
          when((reg_save.ctrl.atomic && reg_save.ctrl.aq) || (reg_save.ctrl.atomic && reg_save.ctrl.rl)) {
            state := s_dcache_1
          }.elsewhen(cnt.value >= num_thread.U || mask_next.asUInt === 0.U) {
            cnt.reset();
            state := s_idle
          }.otherwise {
            cnt.inc();
            state := s_dcache
          }
        }.otherwise {
          state := s_dcache
        }
      }
    }
    is(s_dcache_1){
      when(io.to_dcache.fire){

          when(reg_save.ctrl.aq && reg_save.ctrl.rl) {
            state := s_dcache_2
          }.elsewhen(cnt.value>=num_thread.U || mask_next.asUInt===0.U){
            cnt.reset(); state := s_idle
        }.otherwise{
            cnt.inc(); state :=s_dcache
        }
      }.otherwise{
        state :=s_dcache_1
      }
    }
    is(s_dcache_2){
      when(io.to_dcache.fire){
        when(cnt.value>=num_thread.U ||mask_next.asUInt===0.U){
          cnt.reset();state:=s_idle
        }.otherwise{
          cnt.inc(); state := s_dcache
        }
      }.otherwise{state := s_dcache_2}
    }
  }
  // FSM Operation
  switch(state){
    is (s_idle){
      when(io.flush_dcache.fire){
        reg_save := RegInit(0.U.asTypeOf(new vExeData))
        reg_save.ctrl.mem_cmd := 1.U
      }.elsewhen(io.from_fifo.fire){ // Next: s_save
        reg_save := io.from_fifo.bits   // save data
        when(io.from_fifo.bits.ctrl.atomic){
          reg_save.ctrl.aq := true.B
        }.otherwise{
          reg_save.ctrl.aq:=io.from_fifo.bits.ctrl.aq
        }
        reg_save.mask := Mux(io.from_fifo.bits.ctrl.isvec, io.from_fifo.bits.mask, VecInit((1.U(num_thread.W)).asBools))
      }.otherwise{reg_save := RegInit(0.U.asTypeOf(new vExeData))}
    }
    is (s_save){
      when(reg_save.ctrl.mem_cmd.orR){//===1.U){  // read
        when(io.to_mshr.fire){reg_entryID := io.idx_entry}  // get entryID from MSHR
      }
    }
    is (s_shared){
      // Maybe Nothing here :-)
      when(io.to_shared.fire){                                      // request is sent
        reg_save.mask := mask_next
      }.otherwise{
        reg_save.mask := reg_save.mask
      }
    }
    is (s_dcache){
      when(io.to_dcache.fire){                                      // request is sent
        reg_save.mask := mask_next
      }.otherwise{
        reg_save.mask := reg_save.mask
      }
    }
  }

  switch(state){
    is (s_idle){
      when(io.flush_dcache.fire){
        is_flush := true.B
      }.otherwise{
        is_flush := false.B
      }
    }
    is(s_dcache){
      when(io.to_dcache.fire){
        is_flush := false.B
      }.otherwise{
        is_flush := is_flush
      }
    }
  }

  if (SPIKE_OUTPUT) {
  when(state === s_save && io.to_mshr.fire && reg_save.ctrl.mem) {
    val common_prefix = p"sm ${reg_save.ctrl.spike_info.get.sm_id} warp ${Decimal(reg_save.ctrl.wid)} 0x${Hexadecimal(reg_save.ctrl.spike_info.get.pc)} 0x${Hexadecimal(reg_save.ctrl.spike_info.get.inst)} "
    when(!reg_save.ctrl.isvec) {
      when(reg_save.ctrl.mem_cmd === IDecode.M_XRD) {
        printf(common_prefix + p"lsu.r x ${reg_save.ctrl.reg_idxw} op ${reg_save.ctrl.mop} @ ${Hexadecimal(reg_save.in1(0))}+${Hexadecimal(reg_save.in2(0))}\n")
      } .elsewhen(reg_save.ctrl.mem_cmd === IDecode.M_XWR) {
        printf(common_prefix + p"lsu.w x ${reg_save.ctrl.reg_idx2} op ${reg_save.ctrl.mop} ${Hexadecimal(reg_save.in3(0))} @ ${Hexadecimal(reg_save.in1(0))}+${Hexadecimal(reg_save.in2(0))}\n")
      }
    } .otherwise {
      // 打印格式带 lane index "[lN]=ADDR"，便于 debug 时直接读 lane→paddr 对应关系
      val addr_print: Printable = addr.zipWithIndex.reverse.map { case (x, i) =>
        p" [l${i}]=${Hexadecimal(x)}"
      }.reduceOption(_ + _).getOrElse(p"")
      when(reg_save.ctrl.mem_cmd === IDecode.M_XRD) {
        printf(common_prefix + p"lsu.r v${reg_save.ctrl.reg_idx3} op ${reg_save.ctrl.mop} @" + addr_print + p"\n")
      } .elsewhen(reg_save.ctrl.mem_cmd === IDecode.M_XWR) {
        val idx = Mux(reg_save.ctrl.disable_mask, reg_save.ctrl.reg_idx2, reg_save.ctrl.reg_idxw)
        // store 把 data 与 addr 同 lane 配对："[lN]=DATA@ADDR"
        val store_print: Printable = reg_save.in3.zip(addr).zipWithIndex.reverse.map { case ((d, a), i) =>
          p" [l${i}]=${Hexadecimal(d)}@${Hexadecimal(a)}"
        }.reduceOption(_ + _).getOrElse(p"")
        printf(common_prefix + p"lsu.w v${idx} op ${reg_save.ctrl.mop} mask ${Binary(reg_save.mask.asUInt)}" + store_print + p"\n")
      }
    }
  }
  }

  // Opt-in trace for the PDS-backed private payload frames used by the RT
  // megakernel.  Unlike RT Local SRAM accesses, these are normal vector LSU
  // transactions.  Keep this elaboration-time gated so ordinary RTL builds
  // and timing are unaffected.  Lane 0 is sufficient for the 1x1 regression
  // and keeps full-app logs tractable.
  if (PDS_TRACE) {
    // A compact decoder witness.  Keep it before the VLS12-specific filters:
    // if a custom private-frame instruction is being decoded as an ordinary
    // LSU operation, the missing `is_vls12` bit is itself the diagnosis.
    when(state === s_save && io.to_mshr.fire && reg_save.mask(0)) {
      printf(p"[PDS trace] any pc=0x${Hexadecimal(reg_save.ctrl.pc)}" +
        p" vls12=${Decimal(reg_save.ctrl.is_vls12)}" +
        p" private=${Decimal(reg_save.ctrl.disable_mask)}" +
        p" vec=${Decimal(reg_save.ctrl.isvec)}" +
        p" cmd=${Decimal(reg_save.ctrl.mem_cmd)}" +
        p" base=0x${Hexadecimal(reg_save.in1(0))}" +
        p" off=0x${Hexadecimal(reg_save.in2(0))}" +
        p" data=0x${Hexadecimal(reg_save.in3(0))}" +
        p" paddr=0x${Hexadecimal(addr(0))}\n")
    }
    // VLS12 carries the RT megakernel's explicitly addressed private-frame
    // loads/stores.  It bypasses the ordinary PDS address transform, so trace
    // its resolved address directly rather than applying the offset filter
    // below.
    when(state === s_save && io.to_mshr.fire && reg_save.ctrl.isvec &&
         reg_save.ctrl.is_vls12 && reg_save.mask(0)) {
      when(reg_save.ctrl.mem_cmd === IDecode.M_XRD) {
        printf(p"[PDS trace] vls12-r pc=0x${Hexadecimal(reg_save.ctrl.pc)}" +
          p" paddr=0x${Hexadecimal(addr(0))}\n")
      }.elsewhen(reg_save.ctrl.mem_cmd === IDecode.M_XWR) {
        printf(p"[PDS trace] vls12-w pc=0x${Hexadecimal(reg_save.ctrl.pc)}" +
          p" data=0x${Hexadecimal(reg_save.in3(0))} paddr=0x${Hexadecimal(addr(0))}\n")
      }
    }
    when(state === s_save && io.to_mshr.fire && reg_save.ctrl.isvec &&
         reg_save.ctrl.disable_mask && !reg_save.ctrl.is_vls12 &&
         reg_save.mask(0)) {
      val lane0PrivateOffset = reg_save.in1(0) + reg_save.in2(0)
      when(lane0PrivateOffset <= 64.U) {
        when(reg_save.ctrl.mem_cmd === IDecode.M_XRD) {
          printf(p"[PDS trace] r pc=0x${Hexadecimal(reg_save.ctrl.pc)}" +
            p" off=${Decimal(lane0PrivateOffset)} paddr=0x${Hexadecimal(addr(0))}\n")
        }.elsewhen(reg_save.ctrl.mem_cmd === IDecode.M_XWR) {
          printf(p"[PDS trace] w pc=0x${Hexadecimal(reg_save.ctrl.pc)}" +
            p" off=${Decimal(lane0PrivateOffset)} data=0x${Hexadecimal(reg_save.in3(0))}" +
            p" paddr=0x${Hexadecimal(addr(0))}\n")
        }
      }
    }
  }

//   if (SPIKE_OUTPUT){
//     when( state===s_save && io.to_mshr.fire && reg_save.ctrl.mem/*&&reg_save.ctrl.wid===wid_to_check.U*/){
//       var log_str = p"sm ${reg_save.ctrl.spike_info.get.sm_id} warp ${Decimal(reg_save.ctrl.wid)} "
//       log_str += p"0x${Hexadecimal(reg_save.ctrl.spike_info.get.pc)} 0x${Hexadecimal(reg_save.ctrl.spike_info.get.inst)}"
//       when(reg_save.ctrl.mem_cmd === IDecode.M_XRD){
//         log_str += p" lsu.r "
//       }.elsewhen(reg_save.ctrl.mem_cmd === IDecode.M_XWR){
//         log_str += p" lsu.w "
//       }
//       when(!reg_save.ctrl.isvec){
//         when(reg_save.ctrl.mem_cmd === IDecode.M_XWR){
//           log_str += p"x ${reg_save.ctrl.reg_idx2} op ${reg_save.ctrl.mop} "
//           log_str += p"${Hexadecimal(reg_save.in3(0))} "
//         }.elsewhen(reg_save.ctrl.mem_cmd === IDecode.M_XRD){
//           log_str += p"x ${reg_save.ctrl.reg_idxw} op ${reg_save.ctrl.mop} "
//         }
//         log_str += p"@ ${Hexadecimal(reg_save.in1(0))}+${Hexadecimal(reg_save.in2(0))}\n"
//       }.otherwise{
//         when(reg_save.ctrl.mem_cmd === IDecode.M_XWR) {
//           // vsw12 uses inst[24:20] as src
//           when(reg_save.ctrl.disable_mask){
//             log_str += p"v${reg_save.ctrl.reg_idx2} op ${reg_save.ctrl.mop} "
//           }.otherwise{
//             log_str += p"v${reg_save.ctrl.reg_idxw} op ${reg_save.ctrl.mop} "
//           }
//           log_str += p"mask ${Binary(reg_save.mask.asUInt)} "
//           reg_save.in3.reverse.foreach { x => log_str += p"${Hexadecimal(x)} " }
//         }.elsewhen(reg_save.ctrl.mem_cmd === IDecode.M_XRD){
//           log_str += p"v${reg_save.ctrl.reg_idx3} op ${reg_save.ctrl.mop} "
//         }
//         log_str += p"@"
//         when(false.B){
//           // PDS: csr_pds + align4(offset) * csr_numw * csr_numt + (csr_tid + lane) * 4 + byte_lane
//         }.otherwise{
//           //(reg_save.in1 zip reg_save.in2).reverse.foreach(x => printf(p" ${Hexadecimal(x._1)}+${Hexadecimal(x._2)}"))
//           addr.reverse.foreach{ x =>
//             log_str += p" ${Hexadecimal(x)}"
//           }
//         }
//         log_str += (p"\n")
//       }
//       printf(log_str)
//     }
//   }
}
class LSU2WB extends Module{
  val io = IO(new Bundle{
    val lsu_rsp=Flipped(DecoupledIO(new MSHROutput))
    val out_x = DecoupledIO(new WriteScalarCtrl())
    val out_v = DecoupledIO(new WriteVecCtrl)
  })
  io.out_x.bits.warp_id:=io.lsu_rsp.bits.tag.warp_id
  io.out_x.bits.reg_idxw:=io.lsu_rsp.bits.tag.reg_idxw
  io.out_x.bits.wxd:=io.lsu_rsp.bits.tag.wxd
  io.out_x.bits.wb_wxd_rd:=io.lsu_rsp.bits.data(0)
  io.out_v.bits.warp_id:=io.lsu_rsp.bits.tag.warp_id
  io.out_v.bits.reg_idxw:=io.lsu_rsp.bits.tag.reg_idxw
  io.out_v.bits.wvd:=io.lsu_rsp.bits.tag.wfd
  io.out_v.bits.wvd_mask:=io.lsu_rsp.bits.tag.mask
  io.out_v.bits.wb_wvd_rd:=io.lsu_rsp.bits.data
  if(SPIKE_OUTPUT){
    io.out_x.bits.spike_info.get:=io.lsu_rsp.bits.tag.spike_info.get
    io.out_v.bits.spike_info.get:=io.lsu_rsp.bits.tag.spike_info.get
  }
  when(io.lsu_rsp.bits.tag.wxd){
    io.out_x.valid:=io.lsu_rsp.valid
    io.out_v.valid:=false.B
    io.lsu_rsp.ready:=io.out_x.ready
  }.elsewhen(io.lsu_rsp.bits.tag.wfd){
    io.out_v.valid:=io.lsu_rsp.valid
    io.out_x.valid:=false.B
    io.lsu_rsp.ready:=io.out_v.ready
  }.otherwise({
    io.out_v.valid:=false.B
    io.out_x.valid:=false.B
    io.lsu_rsp.ready:=io.lsu_rsp.bits.tag.isWrite//true.B // CONNECTION OF io.lsu_rsp.bits.tag.isWrite
    if(SPIKE_OUTPUT) {
      when(io.lsu_rsp.fire && io.lsu_rsp.bits.tag.isWrite){
        printf(p"sm ${io.lsu_rsp.bits.tag.spike_info.get.sm_id} warp ${io.lsu_rsp.bits.tag.warp_id} " + 
               p"0x${Hexadecimal(io.lsu_rsp.bits.tag.spike_info.get.pc)} 0x${Hexadecimal(io.lsu_rsp.bits.tag.spike_info.get.inst)} " +
               p"lsu.w finish\n")
      }
    }
  })
}
class LSUexe() extends Module{
// default size: 128 * (num_thread=8) * (xlen/8=4) = 4KByte
  val io = IO(new Bundle{
    val lsu_req = Flipped(DecoupledIO(new vExeData()))
    val dcache_rsp = Flipped(DecoupledIO(new DCacheCoreRsp_np()))
    //val lsu_rsp = DecoupledIO(new WriteBackControl())
    val lsu_rsp = DecoupledIO(new MSHROutput)
    // A cacheline client is independent from the legacy vector-LSU ingress.
    // clientId=PIPE remains reserved for that legacy path.
    val cachelineClientReq = Flipped(Decoupled(new LsuClientRequest))
    val cachelineClientRsp = Decoupled(new LsuClientResponse)
    val dcache_req = DecoupledIO(new DCacheCoreReq_np())
    val shared_req = DecoupledIO(new ShareMemCoreReq_np())
    val shared_rsp = Flipped(DecoupledIO(new DCacheCoreRsp_np))
    val fence_end = Output(UInt(num_warp.W))
    val flush_dcache = Flipped(DecoupledIO(Bool()))

    val csr_wid = Output(UInt(depth_warp.W))
    val csr_pds = Input(UInt(xLen.W))
    val csr_numw = Input(UInt(xLen.W))
    val csr_numt = Input(UInt(xLen.W))
    val csr_tid = Input(UInt(xLen.W))
  })
  val sharedmemory_addr_max = sharemem_size.U(32.W)
  //val sharedmemory = Module(new SharedMemoryV2(nSharedMemoryEntry, num_thread, xLen, lsu_nMshrEntry)) // default: 128

  val InputFIFO = Module(new Queue(new vExeData, entries=1, pipe=true))
  InputFIFO.io.enq <> io.lsu_req

  val AddrCalc = Module(new AddrCalculate(sharedmemory_addr_max))
  AddrCalc.io.from_fifo <> InputFIFO.io.deq
  io.shared_req <> AddrCalc.io.to_shared
  io.flush_dcache <> AddrCalc.io.flush_dcache

  val rspArbiter = Module(new Arbiter(new DCacheCoreRsp_np, n = 2))
  rspArbiter.io.in(1) <> io.shared_rsp
  rspArbiter.io.in(0) <> io.dcache_rsp

  // SOME MSHR INTERFACE
  val Coalscer = Module(new MSHRv2)
  Coalscer.io.from_dcache <> rspArbiter.io.out

  // The legacy PIPE path and cacheline clients contend only for an LSU-private
  // MSHR allocation.  DCache tags are retained in the pending queue and never
  // leave LSU.
  val addrArbiter = Module(new RRArbiter(AddrCalc.io.to_mshr.bits.cloneType, 2))
  addrArbiter.io.in(0) <> AddrCalc.io.to_mshr
  addrArbiter.io.in(1).bits := 0.U.asTypeOf(AddrCalc.io.to_mshr.bits.cloneType)
  addrArbiter.io.in(1).bits.tag.clientId := io.cachelineClientReq.bits.clientId
  addrArbiter.io.in(1).bits.tag.clientTag := io.cachelineClientReq.bits.clientTag
  addrArbiter.io.in(1).bits.tag.lineBase := io.cachelineClientReq.bits.lineBase
  addrArbiter.io.in(1).bits.tag.coverageMask := io.cachelineClientReq.bits.coverageMask
  addrArbiter.io.in(1).bits.tag.mask := VecInit(io.cachelineClientReq.bits.coverageMask.asBools)
  addrArbiter.io.in(1).bits.tag.unsigned := true.B
  addrArbiter.io.in(1).bits.tag.wordOffset1H := VecInit(Seq.fill(num_thread)(15.U(BytesOfWord.W)))
  addrArbiter.io.in(1).bits.tag.isWrite := io.cachelineClientReq.bits.write

  val clientPending = Module(new Queue(new LsuClientDcachePending, entries = lsu_nMshrEntry, pipe = true))
  val isExternalClient = io.cachelineClientReq.bits.clientId =/= LsuClientId.Pipe
  when(io.cachelineClientReq.valid) {
    assert(isExternalClient, "PIPE must use the legacy vector-LSU ingress")
    assert(io.cachelineClientReq.bits.lineBase(6, 0) === 0.U,
      "cacheline client requests must be 128-byte aligned")
  }
  addrArbiter.io.in(1).valid := io.cachelineClientReq.valid && isExternalClient && clientPending.io.enq.ready
  io.cachelineClientReq.ready := addrArbiter.io.in(1).ready && clientPending.io.enq.ready && isExternalClient
  clientPending.io.enq.valid := addrArbiter.io.in(1).fire
  clientPending.io.enq.bits.request := io.cachelineClientReq.bits
  clientPending.io.enq.bits.instrId := Coalscer.io.idx_entry

  Coalscer.io.from_addr <> addrArbiter.io.out
  AddrCalc.io.idx_entry:=Coalscer.io.idx_entry

  // Request arbitration is independent of MSHR admission.  Once accepted, a
  // client request retains its allocated physical tag until DCache accepts it.
  val dcacheArbiter = Module(new RRArbiter(new DCacheCoreReq_np, 2))
  dcacheArbiter.io.in(0) <> AddrCalc.io.to_dcache
  dcacheArbiter.io.in(1).bits := 0.U.asTypeOf(new DCacheCoreReq_np)
  dcacheArbiter.io.in(1).bits.instrId := clientPending.io.deq.bits.instrId
  dcacheArbiter.io.in(1).bits.tag := clientPending.io.deq.bits.request.lineBase(
    memReqLen - 1, memReqLen - dcache_TagBits)
  dcacheArbiter.io.in(1).bits.setIdx := clientPending.io.deq.bits.request.lineBase(
    memReqLen - dcache_TagBits - 1,
    dcache_BlockOffsetBits + dcache_WordOffsetBits)
  dcacheArbiter.io.in(1).bits.opcode := clientPending.io.deq.bits.request.write
  dcacheArbiter.io.in(1).bits.param := 0.U
  dcacheArbiter.io.in(1).bits.data := clientPending.io.deq.bits.request.writeData
  for (lane <- 0 until num_thread) {
    dcacheArbiter.io.in(1).bits.perLaneAddr(lane).activeMask :=
      clientPending.io.deq.bits.request.coverageMask(lane)
    dcacheArbiter.io.in(1).bits.perLaneAddr(lane).blockOffset := lane.U
    dcacheArbiter.io.in(1).bits.perLaneAddr(lane).wordOffset1H := 15.U
  }
  dcacheArbiter.io.in(1).bits.asid.foreach(_ := 0.U)
  dcacheArbiter.io.in(1).bits.spike_info.foreach(_ := 0.U.asTypeOf(new cache_spike_info(mmu.SV32)))
  dcacheArbiter.io.in(1).valid := clientPending.io.deq.valid
  clientPending.io.deq.ready := dcacheArbiter.io.in(1).ready
  io.dcache_req <> dcacheArbiter.io.out

  // The MSHR owns physical-tag completion.  PIPE uses the legacy writeback
  // path; every other client ID returns through the generic cacheline port.
  val complete = Coalscer.io.to_pipe
  val completeIsCachelineClient = complete.bits.tag.clientId =/= LsuClientId.Pipe
  io.lsu_rsp.valid := complete.valid && !completeIsCachelineClient
  io.lsu_rsp.bits := complete.bits
  io.cachelineClientRsp.valid := complete.valid && completeIsCachelineClient
  io.cachelineClientRsp.bits := 0.U.asTypeOf(new LsuClientResponse)
  io.cachelineClientRsp.bits.clientId := complete.bits.tag.clientId
  io.cachelineClientRsp.bits.clientTag := complete.bits.tag.clientTag
  io.cachelineClientRsp.bits.lineBase := complete.bits.tag.lineBase
  io.cachelineClientRsp.bits.coverageMask := complete.bits.tag.coverageMask
  io.cachelineClientRsp.bits.data := complete.bits.data
  io.cachelineClientRsp.bits.fault := false.B
  complete.ready := Mux(completeIsCachelineClient, io.cachelineClientRsp.ready, io.lsu_rsp.ready)

  val shiftBoard=VecInit(Seq.fill(num_warp)(Module(new ShiftBoard(lsu_num_entry_each_warp)).io))
  shiftBoard.zipWithIndex.foreach{case(a,b)=> {
    a.left:=io.lsu_req.fire & io.lsu_req.bits.ctrl.wid===b.asUInt
    a.right:=io.lsu_rsp.fire & io.lsu_rsp.bits.tag.warp_id===b.asUInt
    }}
  io.fence_end:=VecInit(shiftBoard.map(x=>x.empty)).asUInt
  io.lsu_req.ready:=Mux(shiftBoard(io.lsu_req.bits.ctrl.wid).full,false.B,InputFIFO.io.enq.ready)
  InputFIFO.io.enq.valid:=Mux(shiftBoard(io.lsu_req.bits.ctrl.wid).full,false.B,io.lsu_req.valid)

  io.csr_wid:=AddrCalc.io.csr_wid
  AddrCalc.io.csr_tid:=io.csr_tid
  AddrCalc.io.csr_pds:=io.csr_pds
  AddrCalc.io.csr_numw:=io.csr_numw
  AddrCalc.io.csr_numt:=io.csr_numt
}

class ShiftBoard(val depth:Int) extends Module{
  val io=IO(new Bundle{
    val left=Input(Bool())
    val right=Input(Bool())
    val full=Output(Bool())
    val empty=Output(Bool())
  })
  val taps=(Seq.fill(depth)(RegInit(false.B)))
  val left_move=io.left
  val right_move=io.right

  taps.zipWithIndex.foreach {case(a,b)=>
    if(b==0) a:= Mux(left_move^right_move,Mux(left_move,true.B,taps(b+1)),a)
    else if(b==depth-1) a:= Mux(left_move^right_move,Mux(left_move,taps(b-1),false.B),a)
    else a:=Mux(left_move^right_move,Mux(left_move,taps(b-1),taps(b+1)),a)
  }
  io.full:= taps(depth-1)//Mux(left_move^right_move,Mux(left_move,taps(depth-2),false.B),taps(depth-1))
  io.empty:= !Mux(left_move^right_move,Mux(left_move,true.B,taps(1)),taps(0))
}
