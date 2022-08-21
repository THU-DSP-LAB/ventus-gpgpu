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

package L2cache

import chisel3._
import chisel3.util._
import chisel3.internal.sourceinfo.SourceInfo
import freechips.rocketchip.tilelink._
import TLPermissions._
import TLMessages._
import MetaData._

class ScheduleRequest(params:InclusiveCacheParameters_lite) extends Bundle 
{
  val a = Decoupled(new FullRequest(params)) 
  val d = Decoupled(new DirectoryResult_lite(params))

  val dir = Decoupled(new DirectoryWrite_lite(params))

}

class MSHR (params:InclusiveCacheParameters_lite)extends Module
{
  val io = IO(new Bundle {
    val allocate  = Flipped(Valid(new DirectoryResult_lite(params)))

    val status    = Output( new DirectoryResult_lite(params)) 
    val valid     = Input(Bool())
 
    val schedule  = new ScheduleRequest(params)

    val sinkd     = Flipped(Valid(new SinkDResponse(params))) 
  })

  val request = RegInit(0.U.asTypeOf(new DirectoryResult_lite(params)))



  io.status    := request

  val sche_a_valid=RegInit(false.B)  
  val sche_dir_valid=RegInit(false.B)
  val sink_d_reg=RegInit(false.B)

  when (io.allocate.valid) {
    request := io.allocate.bits 
    sink_d_reg:=false.B
  }


  io.schedule.d.valid:= io.valid && sink_d_reg
  io.schedule.d.bits:=request 
  io.schedule.d.bits.hit:=false.B
  io.schedule.d.bits.data :=Mux(sink_d_reg, io.sinkd.bits.data, request.data)


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





  when (io.sinkd.valid) {
    sche_dir_valid:=true.B
    sink_d_reg :=true.B

  }


}
