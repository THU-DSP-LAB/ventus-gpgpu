package top
import L1Cache.MyConfig
import L2cache.{CacheParameters, InclusiveCacheMicroParameters, InclusiveCacheParameters_lite}
import pipeline.parameters.num_warp

object GPGPU_gen extends App{
  (new chisel3.stage.ChiselStage).emitVerilog(new GPGPU_axi_top())
}

/*object GPGPU_gen2 extends App{
  (new chisel3.stage.ChiselStage).emitVerilog(new GPGPU_ExtMemWrapper())
}
*/