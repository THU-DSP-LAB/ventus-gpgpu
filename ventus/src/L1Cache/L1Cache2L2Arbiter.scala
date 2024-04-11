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
package L1Cache

import L1Cache.DCache.{DCacheBundle, DCacheModule}
import L1Cache.{DCacheMemReq, DCacheMemRsp}
import SRAMTemplate.SRAMTemplate
import chisel3._
import chisel3.util._
import config.config.Parameters
import top.parameters._

class L1Cache2L2ArbiterIO(implicit p: Parameters) extends DCacheBundle{
  val memReqVecIn = Flipped(Vec(NCacheInSM, Decoupled(new DCacheMemReq)))
  val memReqOut = Decoupled(new L1CacheMemReqArb)
  val memRspIn = Flipped(Decoupled(new L1CacheMemRsp))
  val memRspVecOut = Vec(NCacheInSM, Decoupled(new DCacheMemRsp()))
}

class L1Cache2L2Arbiter(implicit p: Parameters) extends DCacheModule {
  val io = IO(new L1Cache2L2ArbiterIO)

  // **** memReq ****
  val memReqArb = Module(new Arbiter(new L1CacheMemReq,NCacheInSM))
  memReqArb.io.in <> io.memReqVecIn
  for(i <- 0 until NCacheInSM) {
    memReqArb.io.in(i).bits.a_source := Cat(i.asUInt,io.memReqVecIn(i).bits.a_source)
  }
  io.memReqOut <> memReqArb.io.out
  // ****************

  // **** memRsp ****
  for(i <- 0 until NCacheInSM) {
    io.memRspVecOut(i).bits <> io.memRspIn.bits
    io.memRspVecOut(i).valid :=
      io.memRspIn.bits.d_source(log2Up(NCacheInSM)+3+log2Up(dcache_MshrEntry)+log2Up(dcache_NSets)-1,3+log2Up(dcache_MshrEntry)+log2Up(dcache_NSets))===i.asUInt && io.memRspIn.valid
  }
  io.memRspIn.ready := Mux1H(UIntToOH(io.memRspIn.bits.d_source(log2Up(NCacheInSM)+log2Up(dcache_MshrEntry)+log2Up(dcache_NSets)+3-1,log2Up(dcache_MshrEntry)+log2Up(dcache_NSets)+3)),
    Reverse(Cat(io.memRspVecOut.map(_.ready))))//TODO check order in test
  // ****************
}
