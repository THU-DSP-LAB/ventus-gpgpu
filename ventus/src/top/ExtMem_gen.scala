package top
import L1Cache.MyConfig
import L2cache.{CacheParameters, InclusiveCacheMicroParameters, InclusiveCacheParameters_lite}
import pipeline.parameters.num_warp

  /*
  to generate verilog file, type this in sbt
      test:runMain ALU.ALUGen
  */

/*object ExtMem_gen extends App{
  val l1param = (new MyConfig).toInstance
  val l2param = InclusiveCacheParameters_lite(CacheParameters(2,4,2,blockBytes=128,beatBytes=128),InclusiveCacheMicroParameters(writeBytes = 4,4,2,num_warp,2),false)

  (new chisel3.stage.ChiselStage).emitVerilog(new ExternalMemModel(l2param)(l1param))
}
*/
object GPGPU_gen extends App{
  (new chisel3.stage.ChiselStage).emitVerilog(new GPGPU_axi_adapter_top())
}

/*object GPGPU_gen2 extends App{
  (new chisel3.stage.ChiselStage).emitVerilog(new GPGPU_ExtMemWrapper())
}
*/