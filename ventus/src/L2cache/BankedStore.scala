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

package L2cache

//import Chisel._
import chisel3.dontTouch
import org.chipsalliance.cde.config._
import freechips.rocketchip.diplomacy._
import freechips.rocketchip.tilelink._
import freechips.rocketchip.util.DescribedSRAM
import chisel3._
import chisel3.util._
import scala.math.{max, min}

abstract class BankedStoreAddress(val inner: Boolean, params: InclusiveCacheParameters_lite) extends Bundle
{
 // val noop = Bool() // do not actually use the SRAMs, just block their use
  val way  = UInt(params.wayBits.W)
  val set  = UInt(params.setBits.W)
  val mask = UInt((if (inner) params.innerMaskBits else params.outerMaskBits).W)
}

trait BankedStoreRW
{
  val write = Bool()
}

class BankedStoreOuterAddress (params: InclusiveCacheParameters_lite)extends BankedStoreAddress(false,params)
{
  //override def cloneType: BankedStoreOuterAddress.this.type = new BankedStoreOuterAddress(params).asInstanceOf[this.type]
}
class BankedStoreInnerAddress (params: InclusiveCacheParameters_lite)extends BankedStoreAddress(true,params)
{
  //override def cloneType: BankedStoreInnerAddress.this.type = new BankedStoreInnerAddress(params).asInstanceOf[this.type]
}
class BankedStoreInnerAddressRW(params: InclusiveCacheParameters_lite) extends BankedStoreInnerAddress (params) with BankedStoreRW

abstract class BankedStoreData(val inner: Boolean, params: InclusiveCacheParameters_lite) extends Bundle
{
  val data = UInt(((if (inner) params.beatBytes else params.beatBytes)*8).W)  //inner may different from outer todo
}

class BankedStoreOuterData    (params: InclusiveCacheParameters_lite)extends BankedStoreData(false,params)
class BankedStoreInnerData    (params: InclusiveCacheParameters_lite)extends BankedStoreData(true,params)
class BankedStoreInnerPoison  (params: InclusiveCacheParameters_lite)extends BankedStoreInnerData(params)
{
  //override def cloneType: BankedStoreInnerPoison.this.type = new BankedStoreInnerPoison(params).asInstanceOf[this.type]
}
class BankedStoreOuterPoison  (params: InclusiveCacheParameters_lite)extends BankedStoreOuterData(params)
{
  //override def cloneType: BankedStoreOuterPoison.this.type = new BankedStoreOuterPoison(params).asInstanceOf[this.type]
}
class BankedStoreInnerDecoded (params: InclusiveCacheParameters_lite)extends BankedStoreInnerData(params)
{
  //override def cloneType: BankedStoreInnerDecoded.this.type = new BankedStoreInnerDecoded(params).asInstanceOf[this.type]
}
class BankedStoreOuterDecoded (params: InclusiveCacheParameters_lite)extends BankedStoreOuterData(params)
{
  //override def cloneType: BankedStoreOuterDecoded.this.type = new BankedStoreOuterDecoded(params).asInstanceOf[this.type]
}

class BankedStore(params:InclusiveCacheParameters_lite) extends Module
{
  val io = IO(new Bundle {

    val sinkD_adr = Flipped(Decoupled(new BankedStoreOuterAddress(params)))
    val sinkD_dat = Input(new  BankedStoreOuterPoison(params))

    val sourceD_radr = Flipped(Decoupled(new BankedStoreInnerAddress(params)))
    val sourceD_rdat = Output(new BankedStoreInnerDecoded(params))
    val sourceD_wadr = Flipped(Decoupled(new BankedStoreInnerAddress(params)))
    val sourceD_wdat = Input(new BankedStoreInnerPoison(params))
  })

  val innerBytes= params.beatBytes
  val outerBytes=params.beatBytes
  val rowBytes = max(innerBytes, outerBytes)
  require (rowBytes < params.cache.sizeBytes)
  val rowEntries = params.cache.sizeBytes / rowBytes
  val rowBits = log2Ceil(rowEntries)
  val numBanks = rowBytes / params.micro.writeBytes
  val codeBits = 8*params.micro.writeBytes
  val singlePort= false
  val cc_banks = Module(new SRAMTemplate(UInt(codeBits.W), set=rowEntries, way=numBanks,
        shouldReset=false, holdRead=false, singlePort=singlePort,bypassWrite = false))  //single bank so far






  val set_index =(io.sourceD_radr.bits.set)*(params.cache.ways.asUInt) +io.sourceD_radr.bits.way
  val data_sel =Mux(io.sinkD_adr.valid,io.sinkD_dat.data,io.sourceD_wdat.data)
  val mask_sel =Mux(io.sinkD_adr.valid,io.sinkD_adr.bits.mask,io.sourceD_wadr.bits.mask)
  val set_idx_sel=Mux(io.sinkD_adr.valid,(io.sinkD_adr.bits.set)*(params.cache.ways.asUInt) +io.sinkD_adr.bits.way,(io.sourceD_wadr.bits.set)*(params.cache.ways.asUInt) +io.sourceD_wadr.bits.way)
  cc_banks.io.r.req.valid:= io.sourceD_radr.valid
  cc_banks.io.r.req.bits.apply(setIdx = set_index)
  cc_banks.io.w.req.valid:= io.sourceD_wadr.valid||io.sinkD_adr.valid
  cc_banks.io.w.req.bits.apply(data_sel.asTypeOf(Vec(numBanks,UInt(codeBits.W))),set_idx_sel,mask_sel)
  io.sourceD_wadr.ready:= !io.sinkD_adr.valid
  io.sinkD_adr.ready:= true.B
  io.sourceD_radr.ready:=true.B
  io.sourceD_rdat.data:= (cc_banks.io.r.resp.data).asUInt //read mask no longer useful





}
