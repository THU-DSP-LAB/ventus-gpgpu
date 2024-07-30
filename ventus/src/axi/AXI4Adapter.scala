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
import freechips.rocketchip.tilelink._
import freechips.rocketchip.amba.axi4._
import freechips.rocketchip.util._
import TLPermissions._
import TLMessages._
//import Chisel._
import chisel3._
import chisel3.util._
import chisel3.experimental.SourceInfo
import freechips.rocketchip.diplomacy._
import freechips.rocketchip.util._
import L2cache.InclusiveCacheParameters_lite
import L2cache.TLBundleD_lite
import L2cache.TLBundleA_lite
case class InclusiveCacheParameters_lite_withAXI(cache_params: InclusiveCacheParameters_lite, AXI_params: AXI4BundleParameters)

class BufferBundle_read(params: InclusiveCacheParameters_lite_withAXI) extends Bundle{
  val id   = UInt(params.AXI_params.idBits.W)
  val data = UInt((params.AXI_params.dataBits).W)
  //override def cloneType: BufferBundle_read.this.type = new BufferBundle_read(params).asInstanceOf[this.type]
//  val resp = UInt(params.AXI_params.respBits.W)
//  val last =Bool()
}

class BufferBundle_write(params: InclusiveCacheParameters_lite_withAXI)  extends Bundle{
  //val source = UInt(params.cache_params.source_bits.W)
 // val address= UInt(params.cache_params.addressBits.W)
  val mask =   UInt((params.AXI_params.dataBits/8).W)
  val data=    UInt(params.AXI_params.dataBits.W)
//  val busy=Bool( )
//override def cloneType: BufferBundle_write.this.type = new BufferBundle_write(params).asInstanceOf[this.type]
}
class AXI4Adapter (params:  InclusiveCacheParameters_lite_withAXI) extends Module {
  val io= IO(new Bundle {
    val AXI_master_bundle=(new AXI4Bundle(params.AXI_params))
    val l2cache_outa =Flipped(Decoupled(new TLBundleA_lite(params.cache_params)))
    val l2cache_outd= Decoupled(new TLBundleD_lite(params.cache_params))

  })
  val total_times=(params.cache_params.data_bits /params.AXI_params.dataBits)
  val counter_read=RegInit(0.U(4.W))
  val counter_write=RegInit(0.U(4.W))

  val buffer_read=  Seq.fill(total_times){Reg(new BufferBundle_read(params))}//Seq.fill(total_times){Reg(new BufferBundle_read(params))}
  val buffer_read_valid=RegInit(false.B)
  val buffer_read_busy=WireInit(false.B)
  val buffer_read_ready=RegInit(true.B)
  val buffer_write= Seq.fill(total_times){Reg(new BufferBundle_write(params))}
  val buffer_write_valid=RegInit(false.B)
  val buffer_write_busy=WireInit(false.B)
  val buffer_write_ready=RegInit(true.B)
  //transfer total_times

  io.AXI_master_bundle.aw.valid := (io.l2cache_outa.bits.opcode === PutFullData) && io.l2cache_outa.valid

  io.AXI_master_bundle.ar.valid:=(io.l2cache_outa.bits.opcode === Get) && io.l2cache_outa.valid

  io.AXI_master_bundle.ar.bits.addr := io.l2cache_outa.bits.address
  io.AXI_master_bundle.ar.bits.size:= (log2Up( params.AXI_params.dataBits/8)).asUInt
  io.AXI_master_bundle.ar.bits.burst:= 1.U

  io.AXI_master_bundle.ar.bits.cache:= 1110.U
  io.AXI_master_bundle.ar.bits.id:=io.l2cache_outa.bits.source
  io.AXI_master_bundle.ar.bits.len:= (params.cache_params.data_bits/ params.AXI_params.dataBits -1).asUInt
  io.AXI_master_bundle.ar.bits.lock:= 0.U
  io.AXI_master_bundle.ar.bits.prot:=0.U
  io.AXI_master_bundle.ar.bits.qos:= 0.U
//  io.AXI_master_bundle.ar.bits.user:=()
//  io.AXI_master_bundle.ar.bits.echo:=()

  io.AXI_master_bundle.aw.bits.addr := io.l2cache_outa.bits.address
  io.AXI_master_bundle.aw.bits.size:= (log2Up( params.AXI_params.dataBits/8)).asUInt
  io.AXI_master_bundle.aw.bits.burst:= 1.U
  io.AXI_master_bundle.aw.bits.cache:=1110.U
  io.AXI_master_bundle.aw.bits.id:=io.l2cache_outa.bits.source
  io.AXI_master_bundle.aw.bits.len:=(params.cache_params.data_bits/ params.AXI_params.dataBits -1).asUInt
  io.AXI_master_bundle.aw.bits.lock:=0.U
  io.AXI_master_bundle.aw.bits.prot:=0.U
  io.AXI_master_bundle.aw.bits.qos:= 0.U
//  io.AXI_master_bundle.aw.bits.user:=()
//  io.AXI_master_bundle.aw.bits.echo:=()
//  val DataBits= params.AXI_params.dataBits
//  val sel_bits_low_read =counter_read*DataBits
//  val sel_bits_high_read=(counter_read+1)*DataBits-1
//  val sel_bits_low_write=counter_write*DataBits
//  val sel_bits_high_write=(counter_write+1)*DataBits-1
  // buffer_read
  when( io.AXI_master_bundle.r.bits.last && io.AXI_master_bundle.r.fire ) {
    buffer_read_valid:= true.B
  }.elsewhen(buffer_read_valid && buffer_read_ready){
    buffer_read_valid:= false.B
  }
  buffer_read_busy:= counter_read=/= 0.U
  buffer_read_ready:=io.l2cache_outd.ready
  when(io.AXI_master_bundle.r.fire){
    when(io.AXI_master_bundle.r.bits.last){
      counter_read :=0.U
    }.otherwise {
      counter_read := counter_read + 1.U
    }
    for(i <- 0 until  total_times) {

        when(io.AXI_master_bundle.r.fire && i.asUInt===counter_read) {
          buffer_read(i).data := io.AXI_master_bundle.r.bits.data
          buffer_read(i).id := io.AXI_master_bundle.r.bits.id
        }

    }

  }
  //buffer_write
  when(io.AXI_master_bundle.aw.fire) {
    buffer_write_valid := true.B //AXI write valid & data prepared at the same time
  }.elsewhen(counter_write===(total_times-1).asUInt&&io.AXI_master_bundle.w.fire){
    buffer_write_valid:= false.B
  }
  val write_busy_reg=RegInit(false.B)

  when(io.AXI_master_bundle.aw.fire){
    write_busy_reg:=true.B
  }.elsewhen(io.AXI_master_bundle.w.fire){
    write_busy_reg:=false.B
  }


  buffer_write_busy:= write_busy_reg|| (counter_write=/=0.U && counter_write=/=(total_times-1).asUInt)
  when(io.AXI_master_bundle.w.fire){
    when(io.AXI_master_bundle.w.bits.last){
      counter_write :=0.U
    }.otherwise {
      counter_write := counter_write + 1.U
    }
  }
  when(io.l2cache_outa.fire && (io.l2cache_outa.bits.opcode===PutFullData || io.l2cache_outa.bits.opcode===PutPartialData)){
    buffer_write.zipWithIndex.foreach{ case(buf,i) =>

      buf.data:= (io.l2cache_outa.bits.data)((i+1)*(params.AXI_params.dataBits)-1,i*(params.AXI_params.dataBits))

      //buf.mask := (io.l2cache_outa.bits.mask)((i+1)*(params.AXI_params.dataBits/8)-1,i*(params.AXI_params.dataBits/8))
  //      buf.source := io.l2cache_outa.bits.source
      buf.mask := FillInterleaved(params.cache_params.micro.writeBytes,io.l2cache_outa.bits.mask)((i+1)*(params.AXI_params.dataBits/8)-1,i*(params.AXI_params.dataBits/8))
    }
  }
  //write channel
  io.AXI_master_bundle.w.valid:=buffer_write_valid
  io.AXI_master_bundle.w.bits.data:= buffer_write(counter_write).data
  io.AXI_master_bundle.w.bits.last:= (counter_write===(total_times-1).asUInt).asBool
  io.AXI_master_bundle.w.bits.strb:= buffer_write(counter_write).mask

  //read channel ready
  io.AXI_master_bundle.r.ready:= Mux(buffer_read_valid,io.l2cache_outd.ready,true.B)

  //response channel ready
  io.AXI_master_bundle.b.ready:=true.B
  //sinkD ignore response
  io.l2cache_outd.valid:= io.AXI_master_bundle.b.valid || buffer_read_valid
  io.l2cache_outd.bits.source:= Mux(io.AXI_master_bundle.b.valid,io.AXI_master_bundle.b.bits.id,buffer_read(0).asTypeOf(new BufferBundle_read(params)).id)
  io.l2cache_outd.bits.opcode:=Mux(io.AXI_master_bundle.b.valid,AccessAck,AccessAckData)
  io.l2cache_outd.bits.data:=Mux(io.AXI_master_bundle.b.valid,0.U,buffer_read.map(_.asTypeOf(new BufferBundle_read(params)).data).asUInt)
  io.l2cache_outd.bits.size:= 0.U //todo undefined unused
  io.l2cache_outd.bits.param:= DontCare  //sourceA
  io.l2cache_outa.ready:= !buffer_write_busy && io.AXI_master_bundle.aw.ready && !buffer_read_busy &&io.AXI_master_bundle.ar.ready//Mux(io.l2cache_outa.bits.opcode===PutFullData,!buffer_write_busy && io.AXI_master_bundle.aw.ready, !buffer_read_busy &&io.AXI_master_bundle.ar.ready)




}
