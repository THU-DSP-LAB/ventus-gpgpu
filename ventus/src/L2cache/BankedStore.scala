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
import freechips.rocketchip.config._
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
  val rowBytes = params.micro.portFactor * max(innerBytes, outerBytes)
  require (rowBytes < params.cache.sizeBytes)
  val rowEntries = params.cache.sizeBytes / rowBytes *params.micro.portFactor
  val rowBits = log2Ceil(rowEntries)
  val numBanks = rowBytes / params.micro.writeBytes
  val codeBits = 8*params.micro.writeBytes
  val singlePort= false
  val cc_banks = Seq.tabulate(numBanks) {
    i =>
      Module(new SRAMTemplate(UInt(codeBits.W), set=rowEntries, way=1,
        shouldReset=true, holdRead=false, singlePort=singlePort,bypassWrite = false))
  }




  // These constraints apply on the port priorities:
  //  sourceC > sinkD     outgoing Release > incoming Grant      (we start eviction+refill concurrently)
  //  sinkC > sourceC     incoming ProbeAck > outgoing ProbeAck  (we delay probeack writeback by 1 cycle for QoR)
  //  sinkC > sourceDr    incoming ProbeAck > SourceD read       (we delay probeack writeback by 1 cycle for QoR)
  //  sourceDw > sourceDr modified data visible on next cycle    (needed to ensure SourceD forward progress)
  //  sinkC > sourceC     inner ProbeAck > outer ProbeAck        (make wormhole routing possible [not yet implemented])
  //  sinkC&D > sourceD*  beat arrival > beat read|update        (make wormhole routing possible [not yet implemented])

  // Combining these restrictions yields a priority scheme of:
  //  sinkC > sourceC > sinkD > sourceDw > sourceDr
  //          ^^^^^^^^^^^^^^^ outer interface

  // Requests have different port widths, but we don't want to allow cutting in line.
  // Suppose we have requests A > B > C requesting ports --A-, --BB, ---C.
  // The correct arbitration is to allow --A- only, not --AC.
  // Obviously --A-, BB--, ---C should still be resolved to BBAC.

  class Request extends Bundle {
    val wen      = Bool()
    val index    = UInt(rowBits .W)
    val bankSel  = UInt(numBanks.W)
    val bankSum  = UInt(numBanks.W) // OR of all higher priority bankSels
    val bankEn   = UInt(numBanks.W) // ports actually activated by request
    val data     = Vec(numBanks, UInt(codeBits.W))
  }

  def req[T <: BankedStoreAddress](b: DecoupledIO[T], write: Bool, d: UInt): Request = {
    val beatBytes = if (b.bits.inner) innerBytes else outerBytes
    val ports = beatBytes /params.micro.writeBytes  //4
    val bankBits = log2Ceil(numBanks / ports) //2 port factor==4
    val words = Seq.tabulate(ports) { i =>
      val data = d((i + 1) * 8 * params.micro.writeBytes - 1, i * 8 *params.micro.writeBytes)
      data
    }
    val a = Cat(b.bits.way, b.bits.set)
    val m = b.bits.mask
    val out = Wire(new Request)

    val select = UIntToOH(a(bankBits-1, 0), numBanks/ports)
    val ready  = Cat(Seq.tabulate(numBanks/ports) { i => !(out.bankSum((i+1)*ports-1, i*ports) &m).orR } .reverse)
    b.ready := ready(a(bankBits-1,0))

    out.wen      := write
    out.index    := a //>> bankBits   //width=rowbits
    out.bankSel  := Mux(b.valid, FillInterleaved(ports, select)&Fill(numBanks/ports,m) , 0.U) //ports =write ports
    out.bankEn   :=  out.bankSel & FillInterleaved(ports, ready)
    out.data     := VecInit(Seq.fill(numBanks/ports) { words }.flatten) //需要将尺寸填成跟rowbytes数据一样

    out
  }

  val innerData = 0.U((innerBytes*8).W)
  val outerData = 0.U((outerBytes*8).W)
  val W = true.B
  val R = false.B

  val sinkD_req    = req(io.sinkD_adr,    W, io.sinkD_dat.data)
  val sourceD_rreq = req(io.sourceD_radr, R, innerData)
  val sourceD_wreq = req(io.sourceD_wadr, W, io.sourceD_wdat.data)

  // See the comments above for why this prioritization is used
  val reqs = Seq(sinkD_req, sourceD_wreq, sourceD_rreq) //有优先级区别

  // Connect priorities; note that even if a request does not go through due to failing
  // to obtain a needed subbank, it still blocks overlapping lower priority requests.
  reqs.foldLeft(0.U) { case (sum, req) =>
    req.bankSum := sum
    req.bankSel | sum
  }
  // Access the banks
  val regout = VecInit(cc_banks.zipWithIndex.map { case (b, i) =>
    val en  = reqs.map(_.bankEn(i)).reduce(_||_)
    val sel = reqs.map(_.bankSel(i))
    val wen = PriorityMux(sel, reqs.map(_.wen))
    val idx = PriorityMux(sel, reqs.map(_.index))
    val data= PriorityMux(sel, reqs.map(_.data(i)))

    b.io.w.req.valid := wen && en
    b.io.w.req.bits.apply(
      setIdx=idx,
      data=data,
      waymask=1.U)

    b.io.r.req.valid := !wen && en
    b.io.r.req.bits.apply(setIdx=idx)
    RegEnable(b.io.r.resp.data(0), RegNext(!wen && en))
  })

  val regsel_sourceD = RegNext(RegNext(sourceD_rreq.bankEn))



  val decodeDX = regout.zipWithIndex.map {
    // Intentionally not Mux1H and/or an indexed-mux b/c we want it 0 when !sel to save decode power
    case (r, i) => Mux(regsel_sourceD(i), r, 0.U)
  }.grouped(innerBytes/params.micro.writeBytes).toList.transpose.map(s => s.reduce(_|_))

  io.sourceD_rdat.data := Cat(decodeDX.reverse)
}
