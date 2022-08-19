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
//C通道的主动写回功能由A通道替代
package L2cache
//import Chisel._
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
  //override def cloneType: TLBundle_AD.this.type = new TLBundle_AD(params).asInstanceOf[this.type]
}

class Scheduler(params: InclusiveCacheParameters_lite) extends Module
{
  val io = IO(new Bundle {
    //    val in = new TLBundle_AD(params)
    val in_a =Flipped(Decoupled( new TLBundleA_lite(params))) //引用了TL数据线
    val in_d =Decoupled(new TLBundleD_lite_plus(params))
    val out_a =Decoupled(new TLBundleA_lite(params))
    val out_d=Flipped(Decoupled(new TLBundleD_lite(params)))

    // Control port
    //    val req = Decoupled(new SinkXRequest(params)).flip
    //    val resp = Decoupled(new SourceXRequest(params)) //sink X用于flush
  })


  //sink是接收器，source是源？
  val sourceA = Module(new SourceA(params))

  val sourceD = Module(new SourceD(params))
  //  val sourceX = Module(new SourceX(params))
  val sinkA = Module(new SinkA(params))
  val sinkD = Module(new SinkD(params))
  io.out_a.valid := sourceA.io.a.valid //传给main memory
  io.out_a.bits:=sourceA.io.a.bits
  sourceA.io.a.ready:=io.out_a.ready


  sinkA.io.pb_pop2<>sinkD.io.pb_pop
  sinkD.io.pb_beat<>sinkA.io.pb_beat2
  sourceD.io.pb_pop<>sinkA.io.pb_pop
  sourceD.io.pb_beat<>sinkA.io.pb_beat

  sinkD.io.d.bits:=io.out_d.bits
  sinkD.io.d.valid:=io.out_d.valid
  io.out_d.ready:=sinkD.io.d.ready
  //  io.resp <> sourceX.io.x



  //  val sinkX = Module(new SinkX(params))

  sinkA.io.a.bits:= io.in_a.bits
  sinkA.io.a.valid:=io.in_a.valid



  io.in_a.ready:=sinkA.io.a.ready

  io.in_d.valid := sourceD.io.d.valid //传给main memory
  io.in_d.bits  := sourceD.io.d.bits
  sourceD.io.d.ready:=io.in_d.ready

  //  sinkX.io.x <> io.req



  val directory = Module(new Directory_test(params))

  val bankedStore = Module(new BankedStore(params))

  //实际操作的bank
  val requests = Module(new ListBuffer(ListBufferParameters(UInt(params.source_bits.W), params.mshrs, params.secondary, false,true)))

  val mshrs = Seq.fill(params.mshrs) { Module(new MSHR(params)) }
  //生成若干个mshrs


  sinkD.io.way   := VecInit(mshrs.map(_.io.status.way))(sinkD.io.source)
  sinkD.io.set   := VecInit(mshrs.map(_.io.status.set))(sinkD.io.source)
  sinkD.io.opcode:= VecInit(mshrs.map(_.io.status.opcode))(sinkD.io.source)
  sinkD.io.put   := VecInit(mshrs.map(_.io.status.put))(sinkD.io.source)
  // Consider scheduling an MSHR only if all the resources it requires are available
  val mshr_request = Cat(mshrs.map {  m =>
    ((sourceA.io.req.ready  &&m.io.schedule.a.valid) ||
      (sourceD.io.req.ready &&m.io.schedule.d.valid) ||
      //     (sourceX.io.req.ready && m.io.schedule.bits.x.valid)||
      (m.io.schedule.dir.valid&&directory.io.write.ready)) //如果不握手可能会出现多次写tag
  }.reverse)//数据被轮流发出去？


  val robin_filter = RegInit(0.U(params.mshrs.W))  ///宽度为mshrs的寄存器,revised
  val robin_request = Cat(mshr_request, mshr_request & robin_filter)
  val mshr_selectOH2 = (~(leftOR(robin_request) << 1)).asUInt() & robin_request
  val mshr_selectOH = mshr_selectOH2(2*params.mshrs-1, params.mshrs) | mshr_selectOH2(params.mshrs-1, 0)
  val mshr_select = OHToUInt(mshr_selectOH)//正在处理的mshr，这里面的mshr都是被allocate过的

  //选择对应的MSHR
  val schedule    = Mux1H (mshr_selectOH, mshrs.map(_.io.schedule))   //对应的MSHR输出


  // When an MSHR wins the schedule, it has lowest priority next time
  when (mshr_request.orR()) { robin_filter := ~rightOR(mshr_selectOH) }

  // Fill in which MSHR sends the request
  schedule.a.bits.source := mshr_select      //source对应bits数值，这其实是MSHR的标号

  mshrs.zipWithIndex.foreach { case (m, i) =>
    m.io.sinkd.valid := sinkD.io.resp.valid && (sinkD.io.resp.bits.source === i.asUInt())&&(sinkD.io.resp.bits.opcode===AccessAckData)
    m.io.sinkd.bits  := sinkD.io.resp.bits
    m.io.schedule.a.ready  := sourceA.io.req.ready&&(mshr_select===i.asUInt())
    m.io.schedule.d.ready  := sourceD.io.req.ready&&(mshr_select===i.asUInt())&& requests.io.valid(i)
    m.io.schedule.dir.ready:= directory.io.write.ready&&(mshr_select===i.asUInt())
    m.io.valid      := requests.io.valid(i)
    //   m.io.schedule.ready:=true.B
  }

  //  sourceX.io.req := schedule.x
  val write_buffer =Module(new Queue(new FullRequest(params),4,false,false))
  write_buffer.io.enq.valid:=sourceD.io.a.valid
  write_buffer.io.enq.bits:=sourceD.io.a.bits
  write_buffer.io.deq.ready:= sourceA.io.req.ready&&(!schedule.a.valid)
  sourceA.io.req.bits:=Mux(schedule.a.valid,schedule.a.bits,write_buffer.io.deq.bits)

  sourceA.io.req.valid:=Mux(schedule.a.valid,schedule.a.valid,write_buffer.io.deq.valid)
  sourceD.io.a.ready:= write_buffer.io.enq.ready


  // Pick highest priority request完成修改这才是真正的数据输入
  val request = Wire(Decoupled(new FullRequest(params)))
  request.valid :=(sinkA.io.req.valid)// || sinkX.io.req.valid
  request.bits := sinkA.io.req.bits  //Mux(sinkX.io.req.valid, sinkX.io.req.bits, )//X的优先级更高
  //什么时候给ready?怎么调度？

  //优先级选择
  //  sinkX.io.req.ready := request.ready
  sinkA.io.req.ready := request.ready //&& !sinkX.io.req.valid//用来接收到信号ready后读L2或者写回数据


  //L1需要读取数据,需要知道来自sinkA,从opcode看出来 //todo
  //directory连着sink A以及write连着MSHR和自己result
  directory.io.read.valid:=request.valid
  directory.io.read.bits:=request.bits

  directory.io.write.valid:=schedule.dir.valid
  directory.io.write.bits.way:=schedule.dir.bits.way
  directory.io.write.bits.set:=schedule.dir.bits.set
  directory.io.write.bits.data.tag:=schedule.dir.bits.data.tag
  directory.io.write.bits.data.valid:=schedule.dir.bits.data.valid
  //  directory.io.write.bits.data.dirty:=false.B



  //合并或者新alloc
  val tagMatches = Cat(mshrs.zipWithIndex.map { case(m,i) =>   requests.io.valid(i)&&(m.io.status.tag === directory.io.result.bits.tag)&& (!directory.io.result.bits.hit)}.reverse) //查看是否可以合并
  val alloc = !tagMatches.orR() //如果没有可以合并的，则准备alloc
  // Is there an MSHR free for this request?
  val mshr_validOH = requests.io.valid //todo 这个时候对应排空了应该访问一下mshr清除数据
  val mshr_free = (~mshr_validOH).asUInt.orR()



  val mshr_insertOH_init=( (~(leftOR((~mshr_validOH).asUInt())<< 1)).asUInt() & (~mshr_validOH ).asUInt())
  val mshr_insertOH =mshr_insertOH_init//这个才是打算allocate的
  (mshr_insertOH.asBools zip mshrs) map { case (s, m) =>{
    m.io.allocate.valid:=false.B
    m.io.allocate.bits:=0.U.asTypeOf(new DirectoryResult_lite(params))
    when (directory.io.result.valid && alloc && s && !directory.io.result.bits.hit){// && directory.io.result.bits.opcode===Get) {
      m.io.allocate.valid := true.B
      m.io.allocate.bits := directory.io.result.bits
    }}
  }

  requests.io.push.valid      := directory.io.result.valid && (!directory.io.result.bits.hit)  //&&directory.io.result.bits.opcode===Get
  requests.io.push.bits.data  := directory.io.result.bits.source
  requests.io.push.bits.index := OHToUInt(Mux(alloc,mshr_insertOH,tagMatches))



  //如果准备弹回数据，应该把所有的都弹出来
  requests.io.pop.valid := requests.io.valid(mshr_select)&&schedule.d.valid&&sourceD.io.req.ready//时序是对的
  requests.io.pop.bits  := mshr_select


  request.ready := mshr_free && requests.io.push.ready &&(directory.io.read.ready || directory.io.write.ready)//&& (~requests.used)//目前仅考虑mshr有资源的情况ready



  // pop出来source,这个地方不太对劲
  val dir_result_buffer=Module(new Queue(new DirectoryResult_lite(params),4))

  dir_result_buffer.io.enq.valid:= directory.io.result.valid && (directory.io.result.bits.hit)
  dir_result_buffer.io.enq.bits:=directory.io.result.bits
  dir_result_buffer.io.deq.ready:= !schedule.d.valid && sourceD.io.req.ready


  directory.io.result.ready:= Mux(directory.io.result.bits.hit,dir_result_buffer.io.enq.ready,mshr_free)
  sourceD.io.req.bits.way:=Mux(!schedule.d.valid ,dir_result_buffer.io.deq.bits.way,schedule.d.bits.way)
  sourceD.io.req.bits.data:=Mux(!schedule.d.valid ,dir_result_buffer.io.deq.bits.data,schedule.d.bits.data)
  sourceD.io.req.bits.from_mem:=Mux(!schedule.d.valid ,false.B,true.B)
  sourceD.io.req.bits.hit:=Mux(!schedule.d.valid ,dir_result_buffer.io.deq.bits.hit,schedule.d.bits.hit)
  sourceD.io.req.bits.set:=Mux(!schedule.d.valid ,dir_result_buffer.io.deq.bits.set,schedule.d.bits.set)
  sourceD.io.req.bits.tag:=Mux(!schedule.d.valid ,dir_result_buffer.io.deq.bits.tag,schedule.d.bits.tag)
  sourceD.io.req.bits.mask:=Mux(!schedule.d.valid ,dir_result_buffer.io.deq.bits.mask,schedule.d.bits.mask)
  sourceD.io.req.bits.offset:=Mux(!schedule.d.valid ,dir_result_buffer.io.deq.bits.offset,schedule.d.bits.offset)
  sourceD.io.req.bits.opcode:=Mux(!schedule.d.valid ,dir_result_buffer.io.deq.bits.opcode,schedule.d.bits.opcode)
  sourceD.io.req.bits.put:=Mux(!schedule.d.valid ,dir_result_buffer.io.deq.bits.put,schedule.d.bits.put)
  sourceD.io.req.bits.size:=Mux(!schedule.d.valid ,dir_result_buffer.io.deq.bits.size,schedule.d.bits.size)
  sourceD.io.req.valid:=Mux(!schedule.d.valid ,dir_result_buffer.io.deq.valid,schedule.d.valid)
  sourceD.io.req.bits.source:=Mux(!schedule.d.valid,dir_result_buffer.io.deq.bits.source,requests.io.data) //todo

  // BankedStore ports

  bankedStore.io.sinkD_adr <> sinkD.io.bs_adr         //返回的数据用来写入
 // bankedStore.io.sinkD_dat :=( sinkD.io.bs_dat.asUInt() & (~FillInterleaved(params.micro.writeBytes,sinkA.putbuffer.io.data.mask)).asUInt() ) |( sinkA. & FillInterleaved(params.micro.writeBytes,sinkA.putbuffer.io.data.mask))  //主存返回的数据用来写入
  bankedStore.io.sinkD_dat :=sinkD.io.bs_dat
  bankedStore.io.sourceD_radr <> sourceD.io.bs_radr   //读写的l2cache地址
  bankedStore.io.sourceD_wadr <> sourceD.io.bs_wadr
  bankedStore.io.sourceD_wdat := sourceD.io.bs_wdat   //
  sourceD.io.bs_rdat := bankedStore.io.sourceD_rdat   //output


}

//object hello_gen extends App{
//  val cache = CacheParameters(2,4,2,8,8)
//  val micro = InclusiveCacheMicroParameters(1,4,2)
//  val params = InclusiveCacheParameters_lite(cache,micro,false)
//  (new chisel3.stage.ChiselStage).emitVerilog(new Scheduler(params))
//}
