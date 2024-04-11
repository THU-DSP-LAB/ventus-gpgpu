package top
import chisel3._
import chisel3.util._
import chisel3.util.experimental.loadMemoryFromFile
import L1Cache.ICache._
import L1Cache.MyConfig
import L1Cache.DCache._
import pipeline._
import parameters._
import L2cache._
import config.config._
import parameters.num_warp

class DecoupledPipe[T <: Data](dat: T, latency: Int = 1) extends Module {
  val io = IO(new Bundle {
    val enq = Flipped(DecoupledIO(dat))
    val deq = DecoupledIO(dat)
  })
  val valids = io.enq.valid +: Seq.fill(latency)(RegInit(false.B))
  io.enq.ready := RegNext(io.deq.ready)
  for (i <- 1 to latency) {
    when(!(!io.deq.ready && valids.drop(i).reduce(_ && _))) {
      valids(i) := valids(i - 1)
    }
  }

  def generate: Seq[T] = {
    var regs = Seq(RegEnable(io.enq.bits, valids(0) && !(!io.deq.ready && valids.drop(1).reduce(_ && _))))
    for (i <- 2 to latency) {
      regs = regs :+ RegEnable(regs.last, valids(i - 1) && !(!io.deq.ready && valids.drop(i).reduce(_ && _)))
    }
    regs
  }

  val regs = generate
  io.enq.ready := !(!io.deq.ready && valids.drop(1).reduce(_ && _))
  io.deq.valid := valids.last
  io.deq.bits := regs.last
}

class GPGPU_SimWrapper(FakeCache: Boolean = false) extends Module{
  val L1param = (new MyConfig).toInstance
  val L2param = InclusiveCacheParameters_lite(
    CacheParameters(
      2,
      l2cache_NSets,
      l2cache_NWays,
      blockBytes = (l2cache_BlockWords << 2),
      beatBytes = (l2cache_BlockWords << 2),
      l2cs = num_l2cache
    ),
    InclusiveCacheMicroParameters(l2cache_writeBytes, l2cache_memCycles, l2cache_portFactor, num_warp, num_sm,num_sm_in_cluster,num_cluster,dcache_MshrEntry,dcache_NSets),
    false
  )

  val io = IO(new Bundle {
    val host_req = Flipped(DecoupledIO(new host2CTA_data))
    val host_rsp = DecoupledIO(new CTA2host_data)
    val out_a = Decoupled(new TLBundleA_lite(l2cache_params))
    val out_d = Flipped(Decoupled(new TLBundleD_lite(l2cache_params)))
    val cnt = Output(UInt(32.W))
    val inst_cnt = if(INST_CNT_2) Output(Vec(num_sm, Vec(2, UInt(32.W)))) else Output(Vec(num_sm, UInt(32.W)))
  })

  val counter = new Counter(200000)
  counter.reset()
  when(true.B){
    counter.inc()
  }
  io.cnt := counter.value

  val GPU = Module(new GPGPU_top()(L1param, FakeCache))
  GPU.io.cycle_cnt := counter.value

  val pipe_a = Module(new DecoupledPipe(new TLBundleA_lite(l2cache_params), 2))
  val pipe_d = Module(new DecoupledPipe(new TLBundleD_lite(l2cache_params), 2))
  io.out_a <> pipe_a.io.deq
  pipe_a.io.enq <> GPU.io.out_a(0)
  GPU.io.out_d(0) <> pipe_d.io.deq
  pipe_d.io.enq <> io.out_d

  GPU.io.host_req <> io.host_req
  io.host_rsp <> GPU.io.host_rsp

  io.inst_cnt := GPU.io.inst_cnt.getOrElse(0.U.asTypeOf(io.inst_cnt))
}
