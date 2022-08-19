package pipeline

import chisel3._
import parameters._
import chisel3.util._
import chisel3.util.experimental.loadMemoryFromFile
import utils.QueueWithDataFlush

class PCfifo extends Module{
  val io=IO(new Bundle{
    val icachereq=Flipped(ValidIO(new ICachePipeReq_np()))
    val icachersp_wid=Input(UInt(depth_warp.W))
    val icachersp_valid=Input(Bool())
    val icachersp_ibuffer_ready=Input(Bool())
    //val pcreq=Input(UInt(32.W))
    //val pc_rsp=Output(UInt(32.W))
    val flush=Flipped(ValidIO(UInt(depth_warp.W)))
    val flush_icache=Output(Bool())
    val icachebuf_ready=Output(Vec(num_warp,Bool()))
  })
  //io.pc_rsp:=0.U
  io.flush_icache:=true.B
  //if I want to use flush data, I need to update Queue IP
  //val fifo=VecInit(Seq.fill(num_warp)(Module(new Queue(gen=UInt(32.W),entries=num_icachebuf)).io))
  val fifo_flush=VecInit(Seq.fill(num_warp)(Module(new QueueWithDataFlush(gen=Bool(),pipe = false,entries=num_icachebuf)).io))
  for (i <- 0 until num_warp) {
    io.icachebuf_ready(i):=fifo_flush(i).enq.ready
    //fifo(i).enq.bits:=io.icachereq.bits.addr
    //fifo(i).enq.valid:=false.B
    //fifo(i).deq.ready:=false.B
    fifo_flush(i).enq.bits:=true.B
    fifo_flush(i).enq.valid:=false.B
    fifo_flush(i).deq.ready:=false.B
    fifo_flush(i).flushdata:=io.flush.valid&(i.asUInt()===io.flush.bits)
  }
  //fifo(io.icachereq.bits.warpid).enq.valid:=io.icachereq.valid
  fifo_flush(io.icachereq.bits.warpid).enq.valid:=io.icachereq.valid
  val select=fifo_flush(io.icachersp_wid).deq.bits
  when(select){
    //io.flush_icache:=select
    //fifo(io.icachersp_wid).deq.ready:=io.icachersp_valid&io.icachersp_ibuffer_ready
    fifo_flush(io.icachersp_wid).deq.ready:=io.icachersp_valid&io.icachersp_ibuffer_ready
  }.otherwise(
    {
      //io.flush_icache:=false.B
      //fifo(io.icachersp_wid).deq.ready:=io.icachersp_valid
      fifo_flush(io.icachersp_wid).deq.ready:=io.icachersp_valid
    }
  )
  io.flush_icache:=select
  //io.pc_rsp:=fifo(io.icachersp_wid).deq.bits
}
class IcacheSimpleVersion extends Module{
  val io = IO(new Bundle {
    val data_in = Flipped(DecoupledIO(new ICachePipeReq_np))
    val data_out = DecoupledIO(new ICachePipeRsp_np)
  })
  val q=Module(new Queue((new ICachePipeRsp_np),entries = 1,pipe=true))
  val memory=Mem(100,UInt(32.W))
  val baseaddr=0.U(32.W)
  val addrbias=0.U(32.W)//Mux(io.data_in.bits.warpid.orR,baseaddr,0.U(32.W))
  q.io.enq.bits.data:=memory.read(io.data_in.bits.addr(31,2)+addrbias)
  q.io.enq.bits.warpid:=io.data_in.bits.warpid
  q.io.enq.valid:=io.data_in.valid
  io.data_in.ready:=q.io.enq.ready


  io.data_out<>q.io.deq

  //io.data_out:=Cat(memory.read(io.address),memory.read(io.address+1.U),memory.read(io.address+2.U),memory.read(io.address+3.U))
  //io.data_out:=memory.read(io.address)
  loadMemoryFromFile(memory,"C:/Users/xiang/IdeaProjects/GPGPU/txt/PCrom.txt")
}