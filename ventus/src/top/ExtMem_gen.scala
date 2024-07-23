/*
* Copyright (c) 2021-2022 International Innovation Center of Tsinghua University, Shanghai
* Ventus is licensed under Mulan PSL v2.
* You can use this software according to the terms and conditions of the Mulan PSL v2.
* You may obtain a copy of Mulan PSL v2 at:
* http://license.coscl.org.cn/MulanPSL2
* THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND,
* EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT,
* MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
* See the Mulan PSL v2 for more details. */
package top
import L1Cache.MyConfig
import L2cache.{CacheParameters, InclusiveCacheMicroParameters, InclusiveCacheParameters_lite}
import parameters.{dcache_MshrEntry, dcache_NSets, l2cache_BlockWords, l2cache_NSets, l2cache_NWays, l2cache_memCycles, l2cache_portFactor, l2cache_writeBytes, num_cluster, num_l2cache, num_sm, num_sm_in_cluster, num_warp}
import pipeline.Scoreboard


//object GPGPU_gen extends App{
//  (new chisel3.stage.ChiselStage).emitVerilog(new GPGPU_axi_top())
//}
object GPGPU_gen extends App{
  val L1param = (new MyConfig).toInstance
  val L2param = InclusiveCacheParameters_lite(CacheParameters(2, l2cache_NSets, l2cache_NWays, blockBytes = (l2cache_BlockWords << 2), beatBytes = (l2cache_BlockWords << 2),l2cs = num_l2cache), InclusiveCacheMicroParameters(l2cache_writeBytes, l2cache_memCycles, l2cache_portFactor, num_warp, num_sm, num_sm_in_cluster,num_cluster,dcache_MshrEntry,dcache_NSets), false)
  chisel3.emitVerilog(new GPGPU_top()(L1param),Array("--emission-options=disableMemRandomization,disableRegisterRandomization"))
}


/*object GPGPU_gen2 extends App{
(new chisel3.stage.ChiselStage).emitVerilog(new GPGPU_ExtMemWrapper())
}
*/

//object vALUv2_gen extends App{
// (new chisel3.stage.ChiselStage).emitVerilog(new vALUv2TestWrapper(12,4))
//}