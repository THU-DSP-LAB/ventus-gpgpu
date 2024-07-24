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

package L2cache

import chisel3._
import chisel3.util._
import freechips.rocketchip.tilelink._
import freechips.rocketchip.util._
import TLMessages._
import TLPermissions._


class SourceDRequest_lite(params: InclusiveCacheParameters_lite) extends DirectoryResult_lite(params){

}
class TLBundleD_lite(params: InclusiveCacheParameters_lite) extends Bundle
{
  val opcode=UInt(params.op_bits.W)
  val size=UInt(params.size_bits.W)
  val source=UInt(params.source_bits.W)
  val data  = UInt(params.data_bits.W)
  val param =UInt(3.W)
}
class TLBundleD_lite_plus(params: InclusiveCacheParameters_lite)extends TLBundleD_lite(params)
{
  val address=UInt(params.addressBits.W)
}
class TLBundleD_lite_withid(params: InclusiveCacheParameters_lite)extends DirectoryResult_lite(params)
{
  val from_mem = Bool()
}

class SourceD(params: InclusiveCacheParameters_lite) extends Module
{
  val io = IO(new Bundle {

    val req = Flipped(Decoupled(new TLBundleD_lite_withid(params))) 
    val d = Decoupled(new TLBundleD_lite_plus(params))

    val pb_pop = Decoupled(new PutBufferPop(params))
    val pb_beat = Input(new PutBufferAEntry(params)) 
 
    val bs_radr = Decoupled(new BankedStoreInnerAddress(params))
    val bs_rdat = Input(  new BankedStoreInnerDecoded(params))
    val bs_wadr = Decoupled(new BankedStoreInnerAddress(params))
    val bs_wdat = Output(new BankedStoreInnerPoison(params))
    val a       = Decoupled(new FullRequest(params))
    val mshr_wait = Output(Bool())

    val finish_issue = Output(Bool())
  })


  io.pb_pop.valid:=  io.req.fire&& (io.req.bits.opcode===PutFullData|| io.req.bits.opcode===PutPartialData)  //&& !io.req.bits.from_mem && io.req.bits.hit  //all write acknowledgement response are from source D
  io.pb_pop.bits.index:=io.req.bits.put //sink D also support pop,source D only considers write hit pop
  val pb_beat_reg_init=WireInit(0.U.asTypeOf(new PutBufferAEntry(params)))
  val pb_beat_reg=RegInit(pb_beat_reg_init)
  //stage
  val stage_1 :: stage_2 :: stage_3 ::stage_4::stage_5::stage_6::stage_7::stage_8::Nil =Enum(8)
  val stateReg= RegInit(stage_1)
  val s1_req_reg_init=WireInit(0.U.asTypeOf(new TLBundleD_lite_withid(params)))
  s1_req_reg_init.opcode:=5.U

  val s1_req_reg = RegInit(s1_req_reg_init)
  when(io.req.fire){
    s1_req_reg:=io.req.bits
    pb_beat_reg:=io.pb_beat
  }
  val busy = RegInit(false.B)

  val pb_beat =Mux(io.req.fire, io.pb_beat, pb_beat_reg)
  val s1_req =Mux(io.req.fire, io.req.bits, s1_req_reg)  //stall if busy
  val s_final_req=RegNext(s1_req)  ///
  val s1_need_w =(s1_req.opcode===PutFullData || s1_req.opcode===PutPartialData) && !s1_req.from_mem &&s1_req.hit

  val s1_need_r =((s1_req.opcode===Get) && s1_req.hit) || ((!s1_req.hit ||s1_req.opcode===Hint) && s1_req.dirty)//&& s1_req.hit  //read hit or miss dirty


  val s1_valid_r = s1_need_r


  val read_sent_reg=RegInit(false.B)
  when((s1_valid_r&&io.bs_radr.ready)){
    read_sent_reg:=true.B
  }.otherwise{
    read_sent_reg:=false.B
  }
  val read_sent=Mux(io.req.fire,false.B,read_sent_reg)
  io.bs_radr.valid     :=s1_valid_r &&(!read_sent)   //第一个周期就送过去了
  io.bs_radr.bits.way  := s1_req.way
  io.bs_radr.bits.set  := s1_req.set
  io.bs_radr.bits.mask := s1_req.mask

  val about_to_not_busy = ((stateReg === stage_3) && io.a.fire) || ((stateReg === stage_4) && Mux((!s_final_req.hit && (s_final_req.opcode === PutFullData || s_final_req.opcode === PutPartialData)), io.a.fire && io.d.fire, io.d.fire)) || ((stateReg === stage_8) && io.d.fire) ||
    ((stateReg === stage_7) && io.a.fire)
  io.req.ready  := !busy// || about_to_not_busy

  val s1_w_valid=s1_need_w
  val sourceA_sent_reg=RegInit(false.B)
  when ((s1_req.opcode===PutFullData ||s1_req.opcode===PutPartialData) &&io.a.ready){
    sourceA_sent_reg:=true.B
  }.otherwise{
    sourceA_sent_reg:=false.B
  }
  val sourceA_sent=Mux(io.req.fire,false.B,sourceA_sent_reg)


  val write_sent_reg=RegInit(false.B)
  when((s1_w_valid&&io.bs_wadr.ready)){
    write_sent_reg:=true.B
  }.otherwise{
    write_sent_reg:=false.B
  }
  val write_sent=Mux(io.req.fire,false.B,write_sent_reg)



val tobedone=RegInit(false.B) //all resources not ready
val mshr_wait_reg =RegInit(false.B)
  switch(stateReg){
    is(stage_1){
      busy:=false.B
      mshr_wait_reg :=false.B
      when (io.req.fire || tobedone) {
        mshr_wait_reg := false.B
        when(!s1_req.hit) {
          when(s1_req.dirty) {
            mshr_wait_reg := true.B //used for kicking out victim way, to block premature potential miss request of victim way
          }.otherwise { //miss but not dirty?? from mshr
            stateReg := stage_4
            busy := true.B
            tobedone := false.B
            when(s1_req.opcode === PutFullData && s1_req.opcode === PutPartialData) { //wait write miss no allocate
              mshr_wait_reg := true.B
            }
          }
        }
        //        when(!s1_req.hit && s1_req.opcode===Get ){
        //          stateReg := stage_4
        //          busy := true.B
        //          tobedone:=false.B
        //        }.else
        when(s1_valid_r && io.bs_radr.ready) { //for read hit or miss dirty
          when((!s1_req.hit || (s1_req.opcode === Hint && !s1_req.last_flush)) && s1_req.dirty) { //miss dirty should read bankstore, then to source A
            stateReg := stage_3
            busy := true.B
            tobedone := false.B

          }.otherwise { // read hit
            stateReg := stage_4
            busy := true.B
            tobedone := false.B
          }
        }.elsewhen((s1_req.opcode === PutFullData || s1_req.opcode === PutPartialData) && !s1_req.from_mem && s1_req.hit) { // for write hit,should write bankstore
          when(io.bs_wadr.ready) {
            stateReg := stage_4
            busy := true.B
            tobedone := false.B
          }.otherwise {
            stateReg := stage_2 //need to wait to write
            busy := true.B
            tobedone := false.B
          }
          //            when(io.a.ready) {
          //              when(io.bs_wadr.ready ||  !s1_need_w) {
          //                stateReg := stage_4
          //                busy := true.B
          //                tobedone:=false.B
          //              }.otherwise {
          //                stateReg := stage_2
          //                busy := true.B
          //                tobedone:=false.B
          //              }
          //            }.otherwise {
          //              when(io.bs_wadr.ready||  !s1_need_w) {
          //                stateReg := stage_3
          //                busy := true.B
          //                tobedone:=false.B
          //              }
          //            }


        }.elsewhen(s1_req.opcode===Hint && s1_req.last_flush){ //last flush but not dirty.
          stateReg:= stage_8
          busy := false.B
          tobedone := false.B
          mshr_wait_reg := false.B
        }.otherwise {
          busy := true.B
          tobedone := true.B

        }
      }
    }
    is(stage_2){
      when(io.bs_wadr.ready){
        stateReg:=stage_4
      }
    }
    is(stage_3) { //writeback dirty cache line
      when(io.a.fire) {
        stateReg := stage_1
        busy := false.B
        tobedone := false.B
        mshr_wait_reg := false.B
      }
    }
    is(stage_4) { //ack for miss and hit
      when((!s_final_req.hit && (s_final_req.opcode===PutFullData ||s_final_req.opcode===PutPartialData)) || s_final_req.opcode===Hint) { //ack for write miss no allocate
        when(io.d.fire && io.a.fire) {
          busy := false.B
          stateReg := stage_1
          tobedone := false.B //todo may cause fault
          mshr_wait_reg := false.B
        }.elsewhen(io.d.fire) {
          stateReg := stage_7
        }.elsewhen(io.a.fire) {
          stateReg := stage_8
        }
      }.otherwise{ //ack for read miss from mshr or read hit
        when(io.d.fire) {
          stateReg := stage_1
          busy := false.B
          tobedone := false.B
          mshr_wait_reg := false.B
        }
      }
    }

    is(stage_7){
      when(io.a.fire){
        stateReg := stage_1
        busy := false.B
        tobedone := false.B
        mshr_wait_reg := false.B
      }
    }
    is(stage_8){ //wait for d ready
      when(io.d.fire){
        stateReg := stage_1
        busy := false.B
        tobedone := false.B
        mshr_wait_reg := false.B
      }
    }
  }
  io.mshr_wait      :=mshr_wait_reg
  io.bs_wadr.valid   :=    s1_w_valid &&(!write_sent)
  io.bs_wadr.bits.set:=    s1_req.set
  io.bs_wadr.bits.way:=    s1_req.way
  io.bs_wdat.data    :=    pb_beat.data
  io.bs_wadr.bits.mask:=   pb_beat.mask

  ///将读取数据输出d
  io.d.valid        :=(stateReg===stage_4 || stateReg===stage_8)&& !(s_final_req.opcode===Hint && !s_final_req.last_flush) //&& s1_req.opcode===Get)//数据读出来之后准备输出
  io.d.bits.source  :=s_final_req.source
  io.d.bits.opcode  :=Mux(s_final_req.opcode===Get,AccessAckData,Mux(s_final_req.last_flush,HintAck,AccessAck))
  io.d.bits.size    := s_final_req.size
  io.d.bits.data    :=Mux(s_final_req.opcode===Get,Mux(s_final_req.hit, io.bs_rdat.data,s_final_req.data),0.U.asTypeOf(io.bs_rdat.data)) //Mux(s_final_req.opcode===Get,io.bs_rdat.data,0.U.asTypeOf(io.bs_rdat.data)) //要求应该是读的情况，写的情况直接返回0
  io.d.bits.address      := params.expandAddress(s_final_req.tag,s_final_req.l2cidx, s_final_req.set,s_final_req.offset)
////将读出的数据返回给sourceA
  io.d.bits.param := 0.U


  io.a.valid      :=(stateReg===stage_4 || stateReg===stage_7 || stateReg===stage_3) && ((!s_final_req.hit ||s_final_req.opcode===Hint) && (s_final_req.dirty || s_final_req.opcode===PutFullData ||s_final_req.opcode===PutPartialData))// !s_final_req.hit && (!sourceA_sent) && s_final_req.dirty//(s1_req.opcode===PutFullData ||s1_req.opcode=== PutPartialData)  &&(!sourceA_sent)  //todo for miss kickout dirty cacheline no writethrough , write/read miss kickout dirty ,write allocate
  io.a.bits       := s_final_req

  io.a.bits.data  := Mux((s_final_req.opcode===PutFullData ||s_final_req.opcode=== PutPartialData),s_final_req.data,io.bs_rdat.data) // should be victim data 写miss的数据也经过这个地方转给sourceA
  io.a.bits.opcode:= PutFullData

  io.finish_issue := io.d.valid && s_final_req.last_flush
}
