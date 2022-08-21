package top
import chisel3._
import chisel3.util._
import chisel3.util.experimental.loadMemoryFromFile
import L1Cache.ICache._
import L1Cache.MyConfig
import L1Cache.DCache._
import pipeline._
import pipeline.parameters._
import L2cache._
import config.config._
import pipeline.parameters.num_warp

class GPGPU_ExtMemWrapper(C: TestCase#Props) extends Module{
  val L1param = (new MyConfig).toInstance
  val L2param =InclusiveCacheParameters_lite(CacheParameters(2,l2cache_NSets,l2cache_NWays,blockBytes=(l2cache_BlockWords<<2),beatBytes=(l2cache_BlockWords<<2)),InclusiveCacheMicroParameters(l2cache_writeBytes,l2cache_memCycles,l2cache_portFactor,num_warp,num_sm),false)

  val io = IO(new Bundle{})

  val GPU = Module(new GPGPU_top()(L1param))
  val ExtMem = Module(new ExternalMemModel(C,L2param)(L1param))
  val cpu_test=Module(new CPUtest(C))
  cpu_test.io.host2cta<>GPU.io.host_req
  cpu_test.io.cta2host<>GPU.io.host_rsp
  ExtMem.io.memReq <> GPU.io.out_a
  GPU.io.out_d <> ExtMem.io.memRsp
  ExtMem.io.memReq_ready := true.B
}
