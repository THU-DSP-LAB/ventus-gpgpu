/*
 * Copyright 2019 SiFive, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You should have received a copy of LICENSE.Apache2 along with
 * this software. If not, you may obtain a copy at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
//todo MSHR从何处拿到way的数据？？之前是在dir.result里面拿到的
package L2cache

import chisel3._
import chisel3.util._
import chisel3.internal.sourceinfo.SourceInfo
import freechips.rocketchip.tilelink._
import TLPermissions._
import TLMessages._
import MetaData._

class ScheduleRequest(params:InclusiveCacheParameters_lite) extends Bundle //request mean what??
{
  val a = Decoupled(new FullRequest(params)) //主要为了返回去主存拿数据的valid
  val d = Decoupled(new DirectoryResult_lite(params))
//  val x = Decoupled(new SourceXRequest_lite)//目前x不知道有啥用
  val dir = Decoupled(new DirectoryWrite_lite(params))
  //override def cloneType: ScheduleRequest.this.type = new ScheduleRequest(params).asInstanceOf[this.type]
//  val reload = Bool()
}

class MSHR (params:InclusiveCacheParameters_lite)extends Module
{
  val io = IO(new Bundle {
    val allocate  = Flipped(Valid(new DirectoryResult_lite(params)))//
    //用于查看状态，是输出？用于合并应该是
    val status    = Output( new DirectoryResult_lite(params)) //如果不valid正好说明里面一开始是空的
    val valid     = Input(Bool())
    //对外发送的口,用于entry的pop
    val schedule  = new ScheduleRequest(params)
    //对内接收的
    val sinkd     = Flipped(Valid(new SinkDResponse(params))) //确实应该是valid，scheduler里面做了判断
  })

  val request = RegInit(0.U.asTypeOf(new DirectoryResult_lite(params)))


//现在在处理的MSHR是哪一个？,由scheduler决定，sinkD怎么知道对哪个mSHR清空呢？用valid体现，只对当前在处理的mshr体现为valid

  // Scheduler status用于查询miss是否已经存在于MSHR之中,这个交给scheduler处理
  io.status    := request
//  io.schedule.valid     :=io.schedule.bits.a.valid || io.schedule.bits.d.valid||io.schedule.bits.dir.valid


  //怎么让source a只发出来一次？？
  val sche_a_valid=RegInit(false.B)  //只能在scheduler里面操作了
  val sche_dir_valid=RegInit(false.B)
  val sink_d_reg=RegInit(false.B)

  when (io.allocate.valid) {
    request := io.allocate.bits //取决于scheduler
    sink_d_reg:=false.B
  }


  io.schedule.d.valid:= io.valid && sink_d_reg
  io.schedule.d.bits:=request //todo 这里面感觉sinkd没有source信息 这里面非常不对 感觉应该跟listbuffer联动
  io.schedule.d.bits.hit:=false.B//Mux(request.opcode===Get,true.B,false.B)
  io.schedule.d.bits.data :=Mux(sink_d_reg, io.sinkd.bits.data, request.data)

  //todo source在listbuffer里面取
  io.schedule.a.valid:=sche_a_valid
  io.schedule.a.bits.set:=request.set
  io.schedule.a.bits.opcode:=Get
  io.schedule.a.bits.tag:=request.tag
  io.schedule.a.bits.put:=request.put
  io.schedule.a.bits.offset:=request.offset
  io.schedule.a.bits.source:=request.source
  io.schedule.a.bits.data:=request.data
  io.schedule.a.bits.size:=request.size
  io.schedule.a.bits.mask:= ~(0.U(params.mask_bits.W))
  when(io.schedule.a.fire()){sche_a_valid:=false.B}.elsewhen(io.allocate.valid){
    sche_a_valid:=true.B
  }.otherwise{
    sche_a_valid:=sche_a_valid
  }

  when(io.schedule.dir.fire()){sche_dir_valid:=false.B}


  io.schedule.dir.valid:=sche_dir_valid
  io.schedule.dir.bits.set:=request.set
  io.schedule.dir.bits.data.tag:=request.tag
  io.schedule.dir.bits.data.valid:=true.B
  io.schedule.dir.bits.way:=request.way




//处理d的相关数据
  when (io.sinkd.valid) {
    sche_dir_valid:=true.B
    sink_d_reg :=true.B

  }


}
