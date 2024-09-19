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

package axi

import chisel3._
import chisel3.util._
import top.parameters._
import top.host2CTA_data
import top.CTA2host_data

class AXI4Lite2CTA(val addrWidth:Int, val busWidth:Int) extends Module{
  val io = IO(new Bundle{
    val ctl = Flipped(new AXI4Lite(addrWidth, busWidth))
    //val bus = new CSRBusBundle(addrWidth, busWidth)
    val data = DecoupledIO(new host2CTA_data)
    val rsp = Flipped(Decoupled(new CTA2host_data))
  })



  val regs = RegInit(VecInit(Seq.fill(18)(0.U(busWidth.W))))

  io.rsp.ready:=false.B
  when(io.rsp.valid& !regs(17)(0)){
    io.rsp.ready:=true.B
    regs(17):=1.U
    regs(16):=io.rsp.bits.inflight_wg_buffer_host_wf_done_wg_id
  }

  val sIdle :: sReadAddr :: sReadData :: sWriteAddr :: sWriteData :: sWriteResp :: Nil = Enum(6)
  val state = RegInit(sIdle)

  val awready = RegInit(false.B)
  val wready = RegInit(false.B)
  val bvalid = RegInit(false.B)
  val bresp = WireInit(0.U(AXI4Lite.respWidth.W))

  val arready = RegInit(false.B)
  val rvalid = RegInit(false.B)
  val rresp = WireInit(0.U(AXI4Lite.respWidth.W))

  val addr = RegInit(0.U(addrWidth.W))

  val read = RegInit(false.B)
  val write = RegInit(false.B)
  val dataOut = RegInit(0.U(busWidth.W))

  val transaction_id = RegInit(0.U(AXI4Lite.idWidth.W))

  val rdata = WireInit(0.U(busWidth.W))
  val rdata_reg = RegInit(0.U(busWidth.W))
  rdata_reg := rdata
  rdata := rdata_reg
  when(RegNext(io.ctl.r.rready) || !RegNext(rvalid)){
    rdata := regs(addr)
  }
  //  rdata := RegNext(rdata)
  io.ctl.r.rdata := rdata
  //  io.ctl.r.rdata := regs(addr)
  io.ctl.r.rid := transaction_id


  io.ctl.aw.awready := awready
  io.ctl.w.wready := wready
  io.ctl.b.bvalid := bvalid
  io.ctl.b.bresp := bresp
  io.ctl.b.bid := transaction_id

  io.ctl.ar.arready := arready
  io.ctl.r.rvalid := rvalid
  io.ctl.r.rresp := rresp


  val out_sIdle::out_sOutput::Nil=Enum(2)
  val out_state=RegInit(out_sIdle)
  val input_valid=regs(0)(0)
  io.data.valid:=input_valid & out_state===out_sOutput // TODO: New AXI
  io.data.bits.host_wg_id:=regs(1)
  io.data.bits.host_num_wf:=regs(2)
  io.data.bits.host_wf_size:=regs(3)
  io.data.bits.host_start_pc:=regs(4)
  io.data.bits.host_vgpr_size_total:=regs(5)
  io.data.bits.host_sgpr_size_total:=regs(6)
  io.data.bits.host_lds_size_total:=regs(7)
  io.data.bits.host_gds_size_total:=0.U
  io.data.bits.host_vgpr_size_per_wf:=regs(8)
  io.data.bits.host_sgpr_size_per_wf:=regs(9)
  io.data.bits.host_gds_baseaddr:=regs(10)
  io.data.bits.host_pds_baseaddr:=regs(11)
  io.data.bits.host_csr_knl:=regs(12)
  io.data.bits.host_kernel_size_3d(0):=regs(13)
  io.data.bits.host_kernel_size_3d(1):=regs(14)
  io.data.bits.host_kernel_size_3d(2):=regs(15)

  switch(out_state) {
    is(out_sIdle) {
      when(input_valid) {
        out_state := out_sOutput
      }
    }
    is(out_sOutput) {
      when(io.data.fire) {
        out_state := out_sIdle
        regs(0):=0.U
      }
    }
  }
  when(write) {
    regs(addr) := dataOut
  }

  switch(state){
    is(sIdle){
      rvalid := false.B
      bvalid := false.B
      read := false.B
      write := false.B
      transaction_id := 0.U
      when(io.ctl.aw.awvalid&out_state===out_sIdle){
        state := sWriteAddr
        transaction_id := io.ctl.aw.awid
      }.elsewhen(io.ctl.ar.arvalid){
        state := sReadAddr
        transaction_id := io.ctl.ar.arid
      }

    }
    is(sReadAddr){
      arready := true.B
      when(io.ctl.ar.arvalid && arready){
        state := sReadData
        addr := io.ctl.ar.araddr(addrWidth - 1, 2)
        read := true.B
        arready := false.B
      }
    }
    is(sReadData){
      rvalid := true.B
      when(io.ctl.r.rready && rvalid){
        state := sIdle
        rvalid := false.B
      }
    }
    is(sWriteAddr){
      awready := true.B
      when(io.ctl.aw.awvalid && awready){
        addr := io.ctl.aw.awaddr(addrWidth - 1, 2)
        state := sWriteData
        awready := false.B
      }
    }
    is(sWriteData){
      wready := true.B
      when(io.ctl.w.wvalid && wready){
        state := sWriteResp
        dataOut := io.ctl.w.wdata
        write := true.B
        wready := false.B
      }
    }
    is(sWriteResp){
      write := false.B
      wready := false.B
      bvalid := true.B
      when(io.ctl.b.bready && bvalid){
        state := sIdle
        bvalid := false.B
      }
    }
  }

}


trait HasPipelineReg{ this: CTA_IO =>
  def latency: Int

  //val ready = Wire(Bool())
  //val cnt = RegInit(0.U((log2Up(latency)+1).W))

  //ready := (cnt < latency.U) || (cnt === latency.U && io.out.ready)
  //cnt := cnt + io.in.fire - io.out.fire

  val valids = io.in.valid +: Array.fill(latency)(RegInit(false.B))
  for(i <- 1 to latency){
    when(!(!io.out.ready && valids.drop(i).reduce(_&&_) )){ valids(i) := valids(i-1) }
  }

  def PipelineReg[T<:Data](i: Int)(next: T) = RegEnable(next,valids(i-1) && !(!io.out.ready && valids.drop(i).reduce(_&&_) ))
  def S1Reg[T<:Data](next: T):T = PipelineReg[T](1)(next)
  def S2Reg[T<:Data](next: T):T = PipelineReg[T](2)(next)
  def S3Reg[T<:Data](next: T):T = PipelineReg[T](3)(next)
  def S4Reg[T<:Data](next: T):T = PipelineReg[T](4)(next)
  def S5Reg[T<:Data](next: T):T = PipelineReg[T](5)(next)

  io.in.ready := !(!io.out.ready && valids.drop(1).reduce(_&&_))
  io.out.valid := valids.last
}


abstract class CTA_IO extends Module {
  val io=IO(new Bundle{
    val in = Flipped(DecoupledIO(new host2CTA_data))
    val out = (Decoupled(new CTA2host_data))
  })
}

class CTA_module extends CTA_IO with HasPipelineReg{
  def latency = 5
  io.out.bits.inflight_wg_buffer_host_wf_done_wg_id:=S5Reg(S4Reg(S3Reg(S2Reg(S1Reg(io.in.bits.host_wg_id)))))
}


class AXIwrapper_test(val addrWidth:Int, val busWidth:Int) extends Module{
  val io=IO(new Bundle{
    val ctl = Flipped(new AXI4Lite(addrWidth, busWidth))
  })
  val axiAdapter=Module(new AXI4Lite2CTA(addrWidth,busWidth))
  val cta_module=Module(new CTA_module)
  axiAdapter.io.ctl<>io.ctl
  axiAdapter.io.data<>cta_module.io.in
  axiAdapter.io.rsp<>cta_module.io.out
}

