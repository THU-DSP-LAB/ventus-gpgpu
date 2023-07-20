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

import freechips.rocketchip.diplomacy.AddressSet
import freechips.rocketchip.tilelink._
import freechips.rocketchip.util._
import TLPermissions._
import TLMessages._
import chisel3._
import chisel3.util._
class TLBundle_AD (params: InclusiveCacheParameters_lite)extends Bundle{
  val a =new  TLBundleA_lite(params)
  val d = new  TLBundleD_lite(params)

}
abstract class L2CacheIO(params: InclusiveCacheParameters_lite) extends Module{
  val io = IO(new Bundle {
    val in_a = Flipped(Decoupled(new TLBundleA_lite(params)))
    val in_d = Decoupled(new TLBundleD_lite_plus(params))
    val out_a = Decoupled(new TLBundleA_lite(params))
    val out_d = Flipped(Decoupled(new TLBundleD_lite(params)))
    val flush = Flipped(Decoupled(Bool()))
    val invalidate = Flipped(Decoupled(Bool()))
  })
}
class Scheduler(params: InclusiveCacheParameters_lite) extends L2CacheIO(params)
{
  val sourceA = Module(new SourceA(params))

  val sourceD = Module(new SourceD(params))

  val sinkA = Module(new SinkA(params))
  val sinkD = Module(new SinkD(params))
  io.out_a.valid := sourceA.io.a.valid 
  io.out_a.bits:=sourceA.io.a.bits
  sourceA.io.a.ready:=io.out_a.ready


  sinkA.io.pb_pop2<>sinkD.io.pb_pop
  sinkD.io.pb_beat<>sinkA.io.pb_beat2
  sourceD.io.pb_pop<>sinkA.io.pb_pop
  sourceD.io.pb_beat<>sinkA.io.pb_beat

  sinkD.io.d.bits:=io.out_d.bits
  sinkD.io.d.valid:=io.out_d.valid
  io.out_d.ready:=sinkD.io.d.ready



  val issue_flush_invalidate= RegInit(false.B)
  when(io.flush.fire || io.invalidate.fire){
    issue_flush_invalidate :=true.B
  }.elsewhen(sourceD.io.finish_issue){
    issue_flush_invalidate :=false.B
  }

  io.flush.ready := !issue_flush_invalidate
  io.invalidate.ready  := !issue_flush_invalidate

  sinkA.io.a.bits:= io.in_a.bits
  sinkA.io.a.valid:=io.in_a.valid



  io.in_a.ready:=sinkA.io.a.ready

  io.in_d.valid := sourceD.io.d.valid 
  io.in_d.bits  := sourceD.io.d.bits
  sourceD.io.d.ready:=io.in_d.ready





  val directory = Module(new Directory_test(params))

  val bankedStore = Module(new BankedStore(params))


  val requests = Module(new ListBuffer(ListBufferParameters(UInt(params.source_bits.W), params.mshrs, params.secondary, false,true)))

  val mshrs = Seq.fill(params.mshrs) { Module(new MSHR(params)) }



  sinkD.io.way   := VecInit(mshrs.map(_.io.status.way))(sinkD.io.source)
  sinkD.io.set   := VecInit(mshrs.map(_.io.status.set))(sinkD.io.source)
  sinkD.io.opcode:= VecInit(mshrs.map(_.io.status.opcode))(sinkD.io.source)
  sinkD.io.put   := VecInit(mshrs.map(_.io.status.put))(sinkD.io.source)
 
  val mshr_request = Cat(mshrs.map {  m =>
    ((sourceA.io.req.ready  &&m.io.schedule.a.valid) ||
      (sourceD.io.req.ready &&m.io.schedule.d.valid) ||

      (m.io.schedule.dir.valid&&directory.io.write.ready)) 
  }.reverse)

  val robin_filter = RegInit(0.U(params.mshrs.W))  
  val robin_request = Cat(mshr_request, mshr_request & robin_filter)
  val mshr_selectOH2 = (~(leftOR(robin_request) << 1)).asUInt() & robin_request
  val mshr_selectOH = mshr_selectOH2(2*params.mshrs-1, params.mshrs) | mshr_selectOH2(params.mshrs-1, 0)
  val mshr_select = OHToUInt(mshr_selectOH)

 
  val schedule    = Mux1H (mshr_selectOH, mshrs.map(_.io.schedule))  



  when (mshr_request.orR()) { robin_filter := ~rightOR(mshr_selectOH) }

  
  schedule.a.bits.source := mshr_select      

  mshrs.zipWithIndex.foreach { case (m, i) =>
    m.io.sinkd.valid := sinkD.io.resp.valid && (sinkD.io.resp.bits.source === i.asUInt())&&(sinkD.io.resp.bits.opcode===AccessAckData)
    m.io.sinkd.bits  := sinkD.io.resp.bits
    m.io.schedule.a.ready  := sourceA.io.req.ready&&(mshr_select===i.asUInt())
    m.io.schedule.d.ready  := sourceD.io.req.ready&&(mshr_select===i.asUInt())&& requests.io.valid(i)
    m.io.schedule.dir.ready:= directory.io.write.ready&&(mshr_select===i.asUInt())
    m.io.valid      := requests.io.valid(i)
    m.io.mshr_wait  := sourceD.io.mshr_wait

  }

 
  val write_buffer =Module(new Queue(new FullRequest(params),4,false,false))
  write_buffer.io.enq.valid:=sourceD.io.a.valid
  write_buffer.io.enq.bits:=sourceD.io.a.bits
  write_buffer.io.deq.ready:= sourceA.io.req.ready&&(!schedule.a.valid)
  sourceA.io.req.bits:=Mux(schedule.a.valid,schedule.a.bits,write_buffer.io.deq.bits)

  sourceA.io.req.valid:=Mux(schedule.a.valid,schedule.a.valid,write_buffer.io.deq.valid)
  sourceD.io.a.ready:= write_buffer.io.enq.ready

  val mshr_validOH = requests.io.valid

  val mshr_free = (~mshr_validOH).asUInt.orR()
  val mshr_empty = (~mshr_validOH).asUInt.andR.asBool

  val request = Wire(Decoupled(new FullRequest(params)))
  request.valid :=(sinkA.io.req.valid)
  request.bits := sinkA.io.req.bits  
 
  sinkA.io.req.ready := request.ready   //if mshr still have entries and if dir ready


  val putbuffer_empty= sinkA.io.empty
  directory.io.read.valid:=request.valid
  directory.io.read.bits:=request.bits
  directory.io.flush.valid:= mshr_empty && putbuffer_empty
  directory.io.write.valid:=schedule.dir.valid
  directory.io.write.bits.is_writemiss:= schedule.dir.bits.is_writemiss
  directory.io.write.bits.way:=schedule.dir.bits.way
  directory.io.write.bits.set:=schedule.dir.bits.set
  directory.io.write.bits.data.tag:=schedule.dir.bits.data.tag
  directory.io.invalidate.valid:= io.invalidate.valid
  directory.io.invalidate.bits:= io.invalidate.bits
  directory.io.flush.valid :=io.flush.valid
  directory.io.flush.bits :=io.flush.bits

  //directory.io.write.bits.data.valid:=schedule.dir.bits.data.valid





  val tagMatches = Cat(mshrs.zipWithIndex.map { case(m,i) =>   requests.io.valid(i)&&(m.io.status.tag === directory.io.result.bits.tag)&&(m.io.status.set ===directory.io.result.bits.set)&& (!directory.io.result.bits.hit)}.reverse)
  val alloc = !tagMatches.orR() 




  val mshr_insertOH_init=( (~(leftOR((~mshr_validOH).asUInt())<< 1)).asUInt() & (~mshr_validOH ).asUInt())
  val mshr_insertOH =mshr_insertOH_init
  (mshr_insertOH.asBools zip mshrs) map { case (s, m) =>{
    m.io.allocate.valid:=false.B
    m.io.allocate.bits:=0.U.asTypeOf(new DirectoryResult_lite(params))
    when (directory.io.result.valid && alloc && s && !directory.io.result.bits.hit && !directory.io.result.bits.flush){
      m.io.allocate.valid := true.B
      m.io.allocate.bits.set := directory.io.result.bits.set
      m.io.allocate.bits.tag := directory.io.result.bits.tag
      m.io.allocate.bits.way := directory.io.result.bits.way
      m.io.allocate.bits.opcode := directory.io.result.bits.opcode
      m.io.allocate.bits.data := directory.io.result.bits.data
      m.io.allocate.bits.dirty := directory.io.result.bits.dirty
      m.io.allocate.bits.mask := directory.io.result.bits.mask
      m.io.allocate.bits.hit := directory.io.result.bits.hit
      m.io.allocate.bits.size := directory.io.result.bits.size
      m.io.allocate.bits.put := directory.io.result.bits.put
      m.io.allocate.bits.last_flush := directory.io.result.bits.last_flush
      m.io.allocate.bits.offset := directory.io.result.bits.offset
      m.io.allocate.bits.source:= directory.io.result.bits.source
      m.io.allocate.bits.flush := directory.io.result.bits.flush
      m.io.allocate.bits.l2cidx := directory.io.result.bits.l2cidx
    }}
  }

  requests.io.push.valid      := directory.io.result.valid && (!directory.io.result.bits.hit)  
  requests.io.push.bits.data  := directory.io.result.bits.source
  requests.io.push.bits.index := OHToUInt(Mux(alloc,mshr_insertOH,tagMatches))




  requests.io.pop.valid := requests.io.valid(mshr_select)&&schedule.d.valid&&sourceD.io.req.ready
  requests.io.pop.bits  := mshr_select


  request.ready := mshr_free && requests.io.push.ready &&(Mux(request.bits.opcode===Get,directory.io.read.ready , directory.io.write.ready)) && directory.io.ready && !issue_flush_invalidate




  val dir_result_buffer=Module(new Queue(new DirectoryResult_lite_victim(params),4))

  dir_result_buffer.io.enq.valid:= directory.io.result.valid && (directory.io.result.bits.hit||directory.io.result.bits.dirty) //hit or miss dirty, sourceD don't care if dirty when hit
  dir_result_buffer.io.enq.bits:=directory.io.result.bits

  dir_result_buffer.io.deq.ready:= !schedule.d.valid && sourceD.io.req.ready


  directory.io.result.ready:= Mux(directory.io.result.bits.hit,dir_result_buffer.io.enq.ready,mshr_free)

  sourceD.io.req.bits.way:=Mux(!schedule.d.valid ,dir_result_buffer.io.deq.bits.way,schedule.d.bits.way)
  sourceD.io.req.bits.data:=Mux(!schedule.d.valid ,dir_result_buffer.io.deq.bits.data,schedule.d.bits.data)
  sourceD.io.req.bits.from_mem:=Mux(!schedule.d.valid ,false.B,true.B)
  sourceD.io.req.bits.hit:=Mux(!schedule.d.valid ,dir_result_buffer.io.deq.bits.hit,schedule.d.bits.hit)
  sourceD.io.req.bits.set:=Mux(!schedule.d.valid ,dir_result_buffer.io.deq.bits.set,schedule.d.bits.set)
  sourceD.io.req.bits.l2cidx :=Mux(!schedule.d.valid ,dir_result_buffer.io.deq.bits.l2cidx,schedule.d.bits.l2cidx)
  sourceD.io.req.bits.tag:=Mux(!schedule.d.valid ,Mux(!dir_result_buffer.io.deq.bits.hit,dir_result_buffer.io.deq.bits.victim_tag,dir_result_buffer.io.deq.bits.tag),schedule.d.bits.tag)
  sourceD.io.req.bits.mask:=Mux(!schedule.d.valid ,dir_result_buffer.io.deq.bits.mask,schedule.d.bits.mask)
  sourceD.io.req.bits.offset:=Mux(!schedule.d.valid ,Mux(!dir_result_buffer.io.deq.bits.hit,0.U,dir_result_buffer.io.deq.bits.offset),schedule.d.bits.offset)
  sourceD.io.req.bits.opcode:=Mux(!schedule.d.valid ,dir_result_buffer.io.deq.bits.opcode,schedule.d.bits.opcode)
  sourceD.io.req.bits.put:=Mux(!schedule.d.valid ,dir_result_buffer.io.deq.bits.put,schedule.d.bits.put)
  sourceD.io.req.bits.size:=Mux(!schedule.d.valid ,dir_result_buffer.io.deq.bits.size,schedule.d.bits.size)
  sourceD.io.req.valid:=Mux(!schedule.d.valid ,dir_result_buffer.io.deq.valid,schedule.d.valid)
  sourceD.io.req.bits.source:=Mux(!schedule.d.valid,dir_result_buffer.io.deq.bits.source,requests.io.data) 
  sourceD.io.req.bits.last_flush:= Mux(!schedule.d.valid ,dir_result_buffer.io.deq.bits.last_flush,schedule.d.bits.last_flush)
  sourceD.io.req.bits.flush:= Mux(!schedule.d.valid ,dir_result_buffer.io.deq.bits.flush,schedule.d.bits.flush)
  sourceD.io.req.bits.dirty :=Mux(!schedule.d.valid ,dir_result_buffer.io.deq.bits.dirty,schedule.d.bits.dirty)

  bankedStore.io.sinkD_adr <> sinkD.io.bs_adr         

  bankedStore.io.sinkD_dat :=sinkD.io.bs_dat
  bankedStore.io.sourceD_radr <> sourceD.io.bs_radr   
  bankedStore.io.sourceD_wadr <> sourceD.io.bs_wadr
  bankedStore.io.sourceD_wdat := sourceD.io.bs_wdat   
  sourceD.io.bs_rdat := bankedStore.io.sourceD_rdat   


}


