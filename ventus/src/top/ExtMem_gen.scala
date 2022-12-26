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
package top
import pipeline.operandCollector
import L1Cache.MyConfig
import L2cache.{CacheParameters, InclusiveCacheMicroParameters, InclusiveCacheParameters_lite}
import pipeline.parameters.num_warp

object GPGPU_gen extends App{
  (new chisel3.stage.ChiselStage).emitVerilog(new operandCollector())
}

/*object GPGPU_gen2 extends App{
  (new chisel3.stage.ChiselStage).emitVerilog(new GPGPU_ExtMemWrapper())
}
*/