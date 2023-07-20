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

class GPGPU_ExtMemWrapper(C: TestCase#Props) extends Module{
  val L1param = (new MyConfig).toInstance
  val L2param =InclusiveCacheParameters_lite(CacheParameters(2,l2cache_NSets,l2cache_NWays,num_l2cache,blockBytes=(l2cache_BlockWords<<2),beatBytes=(l2cache_BlockWords<<2)),InclusiveCacheMicroParameters(l2cache_writeBytes,l2cache_memCycles,l2cache_portFactor,num_warp,num_sm,num_sm_in_cluster,num_cluster),false)

  val io = IO(new Bundle{})

  val GPU = Module(new GPGPU_top()(L1param))
  val ExtMem = VecInit(Seq.fill(num_l2cache)(Module(new ExternalMemModel(C,L2param)(L1param)).io))
  val cpu_test=Module(new CPUtest(C))
  //todo multiple l2c
  cpu_test.io.host2cta<>GPU.io.host_req
  cpu_test.io.cta2host<>GPU.io.host_rsp
  for(i<-0 until num_l2cache){
    ExtMem(i).memReq <> GPU.io.out_a(i)
    GPU.io.out_d(i) <> ExtMem(i).memRsp
    ExtMem(i).memReq_ready := true.B
  }

}
