package pipeline.mmu

import L2cache.{SRAMTemplate, TLBundleA_lite, TLBundleD_lite}
import chisel3._
import chisel3.util._
import freechips.rocketchip.util.SetAssocLRU
import chisel3.experimental.dataview._

trait L2TlbParam{
  val nSets = 16
  val nWays = 4
  val nSectors = 2

  def vpnL2TlbBundle(SV: SVParam) = new Bundle {
    val tag = UInt((SV.vpnLen - log2Up(nSets) - log2Up(nSectors)).W)
    val setIndex = UInt(log2Up(nSets).W)
    val sectorIndex = UInt(log2Up(nSectors).W)
  }
}

class L2TlbEntry(SV: SVParam) extends Bundle with L2TlbParam {
  val vpn = UInt(SV.vpnLen.W)
  val level = UInt(log2Up(SV.levels).W)
  val ppns = Vec(nSectors, UInt(SV.ppnLen.W))
  val flags = Vec(nSectors, UInt(8.W))
}

class L2TlbEntryA(SV: SVParam) extends L2TlbEntry(SV){
  val asid = UInt(SV.asidLen.W)

  def toBase: (UInt, L2TlbEntry) = {
    val asid = this.asid
    val entry = this
    (asid, entry)
  }
}

class L2TlbWriteBundle(SV: SVParam) extends Bundle with L2TlbParam {
  val windex = UInt(log2Up(nSets).W)
  val waymask = UInt(nWays.W)
  val wdata = new L2TlbEntryA(SV)
}

class L2TlbStorage(SV: SVParam) extends Module with L2TlbParam {
  val io = IO(new Bundle{
    val rindex = Input(UInt(log2Up(nSets).W))
    val tlbOut = Output(Vec(nWays, new L2TlbEntryA(SV)))
    val write = Flipped(ValidIO(new L2TlbWriteBundle(SV)))
    val wAvail = Output(UInt(nWays.W))
    val ready = Output(Bool())
    val invalidate = Flipped(ValidIO(UInt(SV.asidLen.W)))
  })

  val Entries = Mem(nSets, Vec(nWays, new L2TlbEntry(SV))) // Storage1

  class AsidVBundle extends Bundle{ val asid = UInt(SV.asidLen.W); val v = Vec(nSectors, Bool()); }
  // Storage2, {nSets * nWays * {asid, nSectors * valid}}
  val AsidV = RegInit(
    VecInit(Seq.fill(nSets)(
      VecInit(Seq.fill(nWays)(0.U.asTypeOf(new AsidVBundle)))
    ))
  )
  io.wAvail := Mux(io.write.valid, VecInit(AsidV(io.write.bits.windex).map(_.v)).asUInt, 0.U)

  val s_idle :: s_reset :: Nil = Enum(2)
  val nState = WireInit(s_idle)
  val cState = RegNext(nState)
  val (resetState, resetFin) = Counter(cState === s_reset, nSets)
  val resetAsid = RegInit(0.U(SV.asidLen.W))

  val wen = Mux(cState === s_reset, true.B, io.write.valid && io.ready)
  val windex = Mux(cState === s_reset, resetState, io.write.bits.windex)

  val wdata = Wire(Vec(nWays, new L2TlbEntryA(SV)))
  when(cState === s_reset){
    wdata := 0.U.asTypeOf(Vec(nWays, new L2TlbEntryA(SV)))
  }.otherwise{ wdata.foreach{ _ := io.write.bits.wdata }}
  val waymask = Mux(cState === s_reset, 0.U, io.write.bits.waymask)

  when(cState === s_idle){
    when(io.invalidate.valid){
      nState := s_reset
      resetAsid := io.invalidate.bits
    }.elsewhen(io.write.valid){
      Entries.write(windex, VecInit(wdata.map(_.toBase._2)), waymask.asBools)
      for (((d, m), i) <- (wdata zip waymask.asBools).zipWithIndex) {
        when(m) {
          (AsidV(windex)(i).v zip d.flags).foreach{ case (v, f) => v := f(0) } // for each sector
          AsidV(windex)(i).asid := d.asid
        }
      }
    }
  }.elsewhen(cState === s_reset){ // Reset valid signal depends on ASID match (set by set
    AsidV(resetState).foreach{ av =>
      when(av.asid === resetAsid){
        av.v.foreach{ _ := false.B }
      }
    }
    when(resetFin){
      nState := s_idle
      resetAsid := 0.U
    }.otherwise{
      nState := s_reset
    }
  }
  // change the valid bit with Storage2
  val readTlbOut= {
    val raw = Entries.read(io.rindex)
    val out = Wire(Vec(nWays, new L2TlbEntryA(SV)))
    // for each way:
    ((raw zip out) zip AsidV(io.rindex)).foreach{ case ((r, o), av) =>
      o.viewAsSupertype(new L2TlbEntry(SV)) := r
      val x = Wire(Vec(nSectors, Vec(8, Bool())))
      x := VecInit(r.flags.map(x => VecInit(x.asBools)))
      (0 until nSectors).foreach{ i => x(i)(0) := r.flags(i)(0) & av.v(i) }
      o.flags := VecInit(x.map(_.asUInt))
      o.asid := av.asid
    }
    out
  }
  // bypass write to read
  val tlbOut = WireInit(readTlbOut)
  when(io.write.valid && windex === io.rindex){
    for(i <- 0 until nWays){
      tlbOut(i) := Mux(waymask(i), wdata(i), readTlbOut(i))
    }
  }
  io.ready := cState === s_idle
  io.tlbOut := tlbOut
}

class L2TlbMemReq_Test(SV: SVParam, sectors: Int) extends Bundle{
  val addr = UInt(SV.xLen.W)
  val data = UInt((SV.xLen * sectors).W)
  val op = UInt(2.W) // 10: read, 01: write
}
class L2TlbMemRsp_Test(SV: SVParam, sectors: Int) extends Bundle{
  val addr = UInt(SV.xLen.W)
  val data = UInt((SV.xLen * sectors).W)
  val op = UInt(2.W)
}

class L2Tlb(SV: SVParam/*, L2C: L2cache.InclusiveCacheParameters_lite*/) extends Module with L2TlbParam {
  //override val nSectors = L2C.beatBytes / (SV.xLen/8)
  //verride val nSectors = sectors

  val io = IO(new Bundle{
    val in = Flipped(DecoupledIO(new Bundle{
      val asid = UInt(SV.asidLen.W)
      val ptbr = UInt(SV.xLen.W)
      val vpn = UInt(SV.vpnLen.W)
      val id = UInt(8.W) // L1's id
    }))
    val invalidate = Flipped(ValidIO(new Bundle{
      val asid = UInt(SV.asidLen.W)
    }))
    val out = DecoupledIO(new Bundle{
      val id = UInt(8.W)
      val ppn = UInt(SV.ppnLen.W)
      val flag = UInt(8.W)
    })
    // Request Always Read!
    val mem_req = DecoupledIO(new Cache_Req(SV))
    val mem_rsp = Flipped(DecoupledIO(new Cache_Rsp(SV)))
  })

  val storage = Module(new L2TlbStorage(SV))
  val walker = Module(new PTW(SV, 1))

  val replace = new SetAssocLRU(nSets, nWays, "lru")
  val refillWay = Mux(storage.io.wAvail.orR, PriorityEncoder(storage.io.wAvail), replace.way(storage.io.write.bits.windex))

  val refillData = RegInit(0.U.asTypeOf(new L2TlbEntryA(SV)))

  val s_idle :: s_check :: s_ptw_req :: s_ptw_rsp :: s_reply :: Nil = Enum(5)
  val nState = WireInit(s_idle)
  val cState = RegNext(nState)

  io.in.ready := cState === s_idle && !io.invalidate.valid

  val tlb_req = RegInit(0.U.asTypeOf(io.in.bits))
  storage.io.rindex := tlb_req.asTypeOf(vpnL2TlbBundle(SV)).setIndex
  val storage_rsp = storage.io.tlbOut // Bundle{vpn, ppn: Vec(nSectors, UInt), flags: Vec(nSectors, UInt)}
  val tlb_rsp = RegInit(0.U.asTypeOf(io.out.bits))

  val hitVec = VecInit(storage_rsp.map(m =>
    m.flags(tlb_req.vpn.asTypeOf(vpnL2TlbBundle(SV)).sectorIndex)(0)
    && (m.vpn.asTypeOf(vpnL2TlbBundle(SV)).tag === tlb_req.vpn.asTypeOf(vpnL2TlbBundle(SV)).tag)
    && (m.asid === tlb_req.asid)
  )).asUInt
  val hit = cState === s_check && hitVec.orR
  val miss = cState === s_check && !hitVec.orR

  walker.io.ptw_req.bits.source := tlb_req.id
  walker.io.ptw_req.bits.vpn := tlb_req.vpn
  walker.io.ptw_req.bits.ptbr := tlb_req.ptbr
  walker.io.ptw_req.valid := cState === s_ptw_req

  walker.io.ptw_rsp.ready := storage.io.ready && cState === s_ptw_rsp
  storage.io.write.valid := RegNext(cState === s_ptw_rsp && walker.io.ptw_rsp.fire) // s_ptw_rsp -> [s_reply]
  storage.io.write.bits.waymask := UIntToOH(refillWay)
  storage.io.write.bits.wdata := VecInit(Seq.fill(nWays)(refillData))

  io.mem_req <> walker.io.mem_req
  walker.io.mem_rsp <> io.mem_rsp

  io.out.bits := tlb_rsp
  io.out.valid := cState === s_reply

  storage.io.invalidate.valid := io.invalidate.valid
  storage.io.invalidate.bits := io.invalidate.bits.asid

  switch(cState){
    is(s_idle){
      when(io.in.fire){
        tlb_req := io.in.bits
        when(storage.io.ready && !io.invalidate.valid) {
          nState := s_check
        }.otherwise{
          nState := s_ptw_req
          refillData.asid := io.in.bits.asid
          refillData.vpn := io.in.bits.vpn
        }
      }
    }
    is(s_check){
      when(hit){
        replace.access(storage.io.rindex, hitVec)
        tlb_rsp.id := tlb_req.id
        tlb_rsp.ppn := storage_rsp(OHToUInt(hitVec)).ppns(tlb_req.vpn.asTypeOf(vpnL2TlbBundle(SV)).sectorIndex)
        nState := s_reply
      }.otherwise{
        nState := s_ptw_req
        refillData.asid := tlb_req.asid
        refillData.vpn := tlb_req.vpn
        refillData.level := SV.levels.U
      }
    }
    is(s_ptw_req){
      when(walker.io.ptw_req.fire){
        nState := s_ptw_rsp
      }
    }
    is(s_ptw_rsp){
      when(walker.io.ptw_rsp.fire){
        storage.io.write.bits.windex := tlb_req.vpn.asTypeOf(vpnL2TlbBundle(SV)).setIndex
        refillData.ppns := walker.io.ptw_rsp.bits.ppns
        refillData.flags := walker.io.ptw_rsp.bits.flags
        replace.access(tlb_req.vpn.asTypeOf(vpnL2TlbBundle(SV)).setIndex, refillWay)

        tlb_rsp.id := tlb_req.id
        tlb_rsp.ppn := walker.io.ptw_rsp.bits.ppns
        tlb_rsp.flag := walker.io.ptw_rsp.bits.flags
        nState := s_reply
      }
    }
    is(s_reply){
      refillData := 0.U.asTypeOf(refillData)
      when(io.out.fire){ nState := s_idle }
    }
  }
}