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
//source from https://github.com/lingscale/cc01/blob/master/src/main/scala/core/CSR.scala
import chisel3._
import chisel3.util._
import pipeline.parameters._

object CSR{
  val N = 0.U(2.W)  // not CSR operation
  val W = 1.U(2.W)  // write
  val S = 2.U(2.W)  // set
  val C = 3.U(2.W)  // clear

  // float csr address
  val fflags = 0x001.U(12.W)
  val frm = 0x002.U(12.W)
  val fcsr = 0x003.U(12.W)

  // thread csr address
  val threadid = 0x800.U(12.W)// base thread id. e.g. 0 32 64
  //val threadmask = 0x803.U(12.W)//软件完成
  val wg_wf_count =        0x801.U(12.W) //sum of warp for this cta.
  val wf_size_dispatch =   0x802.U(12.W) //default = num_thread
  val sgpr_base_dispatch = 0x803.U(12.W)
  val vgpr_base_dispatch = 0x804.U(12.W)
  val wf_tag_dispatch =    0x805.U(12.W) //warp id(
  val lds_base_dispatch =  0x806.U(12.W) //lds_baseaddr
  val gds_baseaddr = 0x807.U(12.W) //gds_baseaddr

  // Vector csr address
  val vstart = 0x008.U(12.W)
  val vxsat = 0x009.U(12.W)
  val vxrm = 0x00A.U(12.W)
  val vcsr = 0x00F.U(12.W)
  val vl = 0xC20.U(12.W)
  val vtype = 0xC21.U(12.W)
  val vlenb = 0xC22.U(12.W)



  val PRV_M = 0x3.U(2.W)
  // Machine-level CSR addrs
  // Machine Information Registers
  val mvendorid  = 0xF11.U(12.W)  // Vendor ID
  val marchid    = 0xF12.U(12.W)  // Architecture ID
  val mimpid     = 0xF13.U(12.W)  // Implementation ID
  val mhartid    = 0xF14.U(12.W)  // Hardware thread ID
  // Machine Trap Setup
  val mstatus    = 0x300.U(12.W)  // Machine status register
  val misa       = 0x301.U(12.W)  // ISA and extensions
  //val medeleg    = 0x302.U(12.W)  // Machine exception delegation register (only M-mode,should not exist)
  //val mideleg    = 0x303.U(12.W)  // Machine interrupt delegation register (only M-mode,should not exist)
  val mie        = 0x304.U(12.W)  // Machine interrupt-enable register
  val mtvec      = 0x305.U(12.W)  // Machine trap-handler base address
  val mcounteren = 0x306.U(12.W)  // Machine counter enable
  // Machine Trap Handling
  val mscratch   = 0x340.U(12.W)  // Scratch register for machine trap handlers
  val mepc       = 0x341.U(12.W)  // Machine exception program counter
  val mcause     = 0x342.U(12.W)  // Machine trap cause
  val mtval      = 0x343.U(12.W)  // Machine bad address or instruction
  val mip        = 0x344.U(12.W)  // Machine interrupt pending
  // Machine Counter/Timers
  val mcycle     = 0xB00.U(12.W)  // Machine cycle counter
  val minstret   = 0xB02.U(12.W)  // Machine intruction-retired counter
  val mcycleh    = 0xB80.U(12.W)  // Upper 32 bits of mcycle, RV32I only
  val minstreth  = 0xB82.U(12.W)  // Upper 32 bits of minstret, RV32I only
  // Machine Counter Setup
  val mcountinhibit = 0x320.U(12.W) // Machine counter-inhibit register



}
class CSRFile extends Module {
  val io=IO(new Bundle{
    val ctrl = Input(new CtrlSigs)
    val in1 = Input(UInt(xLen.W))
    val write = Input(Bool())
    val wb_wxd_rd  = Output(UInt(xLen.W))
    val frm = Output(UInt(3.W))
    val CTA2csr = Flipped(ValidIO(new warpReqData))
  })

  // Machine Trap-Vector Base-Address Register (mtvec)
  val mtvec = 0.U(xLen.W)  // do not support trap vector

  // Machine Interrupt Registers (mip and mie)
  val MTIP = RegInit(false.B)  // timer interrupt-pending bits
  val STIP = false.B
  val UTIP = false.B
  val MTIE = RegInit(false.B)  // timer interrupt-enable bits
  val STIE = false.B
  val UTIE = false.B
  val MSIP = RegInit(false.B)  // software interrupt-pending bits
  val SSIP = false.B
  val USIP = false.B
  val MSIE = RegInit(false.B)  // software interrupt-enable bits
  val SSIE = false.B
  val USIE = false.B
  val MEIP = RegInit(false.B)  // external interrupt-pending bits
  val SEIP = false.B
  val UEIP = false.B
  val MEIE = RegInit(false.B)  // external interrupt-enable bits
  val SEIE = false.B
  val UEIE = false.B
  val mip = Cat(0.U((xLen - 12).W), MEIP, 0.U(1.W), SEIP, UEIP, MTIP, 0.U(1.W), STIP, UTIP, MSIP, 0.U(1.W), SSIP, USIP)
  val mie = Cat(0.U((xLen - 12).W), MEIE, 0.U(1.W), SEIE, UEIE, MTIE, 0.U(1.W), STIE, UTIE, MSIE, 0.U(1.W), SSIE, USIE)

  val MIE  = RegInit(false.B)  // interrupt-enable bit
  val SIE  = false.B
  val UIE  = false.B
  val MPIE = RegInit(false.B)  // to support nested tarps
  val SPIE = false.B
  val UPIE = false.B
  val MPP  = 3.U(2.W)
  val SPP  = 0.U(1.W)
  val MPRV = false.B           // memory privilege
  val MXR  = false.B
  val SUM  = false.B
  val TVM  = false.B           // virtualization support
  val TW   = false.B
  val TSR  = false.B
  val FS   = 0.U(2.W)          // extension context status
  val XS   = 0.U(2.W)
  val SD   = false.B           //   state dirty
  val mstatus = Cat(SD, 0.U(8.W), TSR, TW, TVM, MXR, SUM, MPRV, XS, FS, MPP, 0.U(2.W), SPP, MPIE, 0.U(1.W), SPIE, UPIE, MIE, 0.U(1.W), SIE, UIE)

  // Machine Scratch Register (mscratch)
  val mscratch = RegInit(0.U(xLen.W))

  // Machine Exception Program Counter (mepc)
  val mepc = RegInit(0.U(xLen.W))

  // Machine Cause Register (mcause)
  val mcause = RegInit(0.U(xLen.W))

  // Machine Trap Value Register (mtval)
  val mtval = RegInit(0.U(xLen.W))

  // thread message register
  val threadid = RegInit(0.U(xLen.W))
  val lds_baseaddr = RegInit(0.U(xLen.W))
  val gds_baseaddr = RegInit(sharemem_size.U(xLen.W))
  //val threadmask = RegInit(0.U(xLen.W))
  val wg_wf_count = Reg(UInt(WF_COUNT_WIDTH.W)) // sum of wf in a wg
  val wf_size_dispatch = Reg(UInt(WAVE_ITEM_WIDTH.W)) // 32 thread
  val sgpr_base_dispatch = Reg(UInt((SGPR_ID_WIDTH + 1).W))
  val vgpr_base_dispatch = Reg(UInt((VGPR_ID_WIDTH + 1).W))
  val wf_tag_dispatch = Reg(UInt(TAG_WIDTH.W))
  val lds_base_dispatch = Reg(UInt((LDS_ID_WIDTH + 1).W))
  //val start_pc_dispatch = Reg(UInt(MEM_ADDR_WIDTH.W))

  // float csr address
  val NV = RegInit(false.B)
  val DZ = RegInit(false.B)
  val OF = RegInit(false.B)
  val UF = RegInit(false.B)
  val NX = RegInit(false.B)
  val fflags = Cat(NV,DZ,OF,UF,NX)
  val frm = RegInit(0.U(3.W))
  val fcsr = Cat(0.U(24.W),frm,fflags)
  io.frm:=frm

  // Vector csr address
  val VILL = RegInit(false.B)
  val VMA = RegInit(false.B)
  val VTA = RegInit(false.B)//undisturbed
  val VSEW = RegInit(2.U(3.W))//only support e32
  val VLMUL = RegInit(0.U(3.W))//only support LMUL=1
  val VLMAX = RegInit(num_thread.U(xLen.W))
  val vstart = RegInit(0.U(xLen.W))
  val vxsat = RegInit(false.B)//fix-point accrued saturation flag
  val vxrm = RegInit(0.U(2.W))//fix point rounding mode
  val vcsr = RegInit(0.U(xLen.W))
  val vl = RegInit(0.U(xLen.W))
  val vtype = Cat(VILL,0.U(23.W),VMA,VTA,VSEW,VLMUL)
  val vlenb = RegInit(0.U(xLen.W))

  val csr_addr = io.ctrl.inst(31,20)
  val csr_input = io.in1
  val csr_rdata = Wire(UInt(xLen.W))

  val wdata=Wire(UInt(xLen.W))
  wdata:=csr_rdata
  io.wb_wxd_rd:=wdata

  val wen=io.ctrl.csr.orR()&io.write
  val csr_wdata = MuxLookup(io.ctrl.csr, 0.U, Seq( CSR.W -> csr_input, CSR.S -> (csr_rdata | csr_input),  CSR.C -> (csr_rdata & (~csr_input).asUInt)))

  val csrFile = Seq(
    BitPat(CSR.mstatus)         -> mstatus,
    BitPat(CSR.mie)             -> mie,
    BitPat(CSR.mtvec)           -> mtvec,
    BitPat(CSR.mscratch)        -> mscratch,
    BitPat(CSR.mepc)            -> mepc,
    BitPat(CSR.mcause)          -> mcause,
    BitPat(CSR.mtval)           -> mtval,
    BitPat(CSR.mip)             -> mip,
    BitPat(CSR.frm)->frm,
    BitPat(CSR.fcsr)->fcsr,
    BitPat(CSR.fflags)->fflags,
    BitPat(CSR.threadid)->threadid,
    //BitPat(CSR.threadmask)->threadmask,
    BitPat(CSR.vtype)->vtype ,
    BitPat(CSR.wg_wf_count)->wg_wf_count,
    BitPat(CSR.wf_size_dispatch)->wf_size_dispatch,
    BitPat(CSR.sgpr_base_dispatch)->sgpr_base_dispatch,
    BitPat(CSR.vgpr_base_dispatch)->vgpr_base_dispatch,
    BitPat(CSR.wf_tag_dispatch)->    wf_tag_dispatch,
    BitPat(CSR.lds_base_dispatch)-> lds_base_dispatch,
    BitPat(CSR.gds_baseaddr)-> gds_baseaddr
  )

  csr_rdata := Lookup(csr_addr, 0.U(xLen.W), csrFile).asUInt
  val AVL=csr_input

    when(wen){
      when(io.ctrl.isvec){
        wdata:=Mux(AVL<VLMAX,AVL,VLMAX)
//               Mux(AVL>(VLMAX<<1.U).asUInt(),VLMAX,AVL>>1.U))
      } .elsewhen(csr_addr === CSR.fflags){
        NX:=csr_wdata(0)
        UF:=csr_wdata(1)
        OF:=csr_wdata(2)
        DZ:=csr_wdata(3)
        NV:=csr_wdata(4)
      } .elsewhen (csr_addr === CSR.frm){
        frm:=csr_wdata(2,0)
      } .elsewhen (csr_addr === CSR.mstatus) {
        MIE := csr_wdata(3)
        MPIE := csr_wdata(7)
      } .elsewhen (csr_addr === CSR.mip) {
        MTIP := csr_wdata(7)
        MSIP := csr_wdata(3)
        MEIP := csr_wdata(11)
      } .elsewhen (csr_addr === CSR.mie) {
        MTIE := csr_wdata(7)
        MSIE := csr_wdata(3)
        MEIE := csr_wdata(11)
      } .elsewhen(csr_addr === CSR.mscratch) {
        mscratch := csr_wdata
      } .elsewhen(csr_addr === CSR.mepc) {
        mepc := csr_wdata
      } .elsewhen(csr_addr === CSR.mcause) {
        mcause := csr_wdata
      } .elsewhen(csr_addr === CSR.mtval) {
        mtval := csr_wdata
    }
  }
  when(io.CTA2csr.valid){
    //是否应该清除原有的CSR配置？
    wg_wf_count :=io.CTA2csr.bits.CTAdata.dispatch2cu_wg_wf_count
    wf_size_dispatch :=io.CTA2csr.bits.CTAdata.dispatch2cu_wf_size_dispatch
    sgpr_base_dispatch:=io.CTA2csr.bits.CTAdata.dispatch2cu_sgpr_base_dispatch
    vgpr_base_dispatch:=io.CTA2csr.bits.CTAdata.dispatch2cu_vgpr_base_dispatch
    wf_tag_dispatch :=io.CTA2csr.bits.CTAdata.dispatch2cu_wf_tag_dispatch
    lds_base_dispatch:=io.CTA2csr.bits.CTAdata.dispatch2cu_lds_base_dispatch
    gds_baseaddr:=io.CTA2csr.bits.CTAdata.dispatch2cu_gds_base_dispatch

    threadid:=io.CTA2csr.bits.CTAdata.dispatch2cu_wf_tag_dispatch(depth_thread-1,0)<<depth_thread
  }
}

class CSRexe extends Module {
  val io = IO(new Bundle {
    val in = Flipped(Decoupled(new csrExeData))
    val out = Decoupled(new WriteScalarCtrl())
    val frm_wid = Input(UInt(depth_warp.W))
    val frm = Output(UInt(3.W))
    val CTA2csr = Flipped(ValidIO(new warpReqData))
    //val warpsetting = Input()
  })
  val vCSR=VecInit(Seq.fill(num_warp)(Module(new CSRFile).io))
  vCSR.foreach(x=>{
    x.ctrl:=io.in.bits.ctrl
    x.write:=false.B
    x.in1:=io.in.bits.in1
    x.CTA2csr.valid:=false.B
    x.CTA2csr.bits:=io.CTA2csr.bits
  })
  vCSR(io.in.bits.ctrl.wid).write:=io.in.fire
  vCSR(io.CTA2csr.bits.wid).CTA2csr.valid:=io.CTA2csr.valid
  val result=Module(new Queue(new WriteScalarCtrl,1,pipe=true))
  result.io.deq<>io.out

  io.in.ready:=result.io.enq.ready & !io.CTA2csr.valid
  result.io.enq.valid:=io.in.valid
  result.io.enq.bits:=0.U.asTypeOf(new WriteScalarCtrl)
  result.io.enq.bits.reg_idxw:=io.in.bits.ctrl.reg_idxw
  result.io.enq.bits.wxd:=io.in.bits.ctrl.wxd
  result.io.enq.bits.wb_wxd_rd:=vCSR(io.in.bits.ctrl.wid).wb_wxd_rd
  result.io.enq.bits.warp_id:=io.in.bits.ctrl.wid


  io.frm:=vCSR(io.frm_wid).frm

}
