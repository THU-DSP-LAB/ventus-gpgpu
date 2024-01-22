package pipeline.mmu

import L2cache.{SRAMTemplate, TLBundleA_lite, TLBundleD_lite}
import chisel3._
import chisel3.util._

object L2TlbParam{
  val nSets = 64
  val nWays = 4
}

class L2TlbEntry(SV: SVParam) extends Bundle{
  val vpn = UInt(SV.vpnLen.W)
  val level = UInt(log2Up(SV.levels).W)
  val ppn = UInt(SV.ppnLen.W)
  val flags = UInt(8.W)
}

class L2TlbEntryA(SV: SVParam) extends L2TlbEntry(SV){
  val asid = UInt(SV.asidLen.W)

  def toBase: (UInt, L2TlbEntry) = {
    val asid = this.asid
    val entry = this
    (asid, entry)
  }
}

class L2TlbWriteBundle(SV: SVParam) extends Bundle{
  import L2TlbParam._
  val windex = UInt(log2Up(nSets).W)
  val waymask = UInt(nWays.W)
  val wdata = new L2TlbEntryA(SV)
}

class L2TlbStorage(SV: SVParam) extends Module{
  import L2TlbParam._
  val io = IO(new Bundle{
    val rindex = Input(UInt(log2Up(nSets).W))
    val tlbOut = Output(Vec(nWays, new L2TlbEntry(SV)))
    val write = Flipped(ValidIO(new L2TlbWriteBundle(SV)))
    val ready = Output(Bool())
    val invalidate = Flipped(ValidIO(UInt(SV.asidLen.W)))
  })

  val Entries = Mem(nSets, Vec(nWays, new L2TlbEntry(SV))) // Storage1
  val AsidV = RegInit(VecInit(Seq.fill(nSets) // Storage2, asid & valid
    (VecInit(Seq.fill(nWays)
      (new Bundle{
        val asid = UInt(SV.asidLen.W)
        val v = Bool()
      })
    ))
  ))

  val s_idle :: s_reset :: Nil = Enum(2)
  val nState = WireInit(s_idle)
  val cState = RegNext(nState)
  val (resetState, resetFin) = Counter(cState === s_reset, nSets)
  val resetAsid = RegInit(0.U(SV.asidLen.W))

  val wen = Mux(cState === s_reset, true.B, io.write.valid && io.ready)
  val windex = Mux(cState === s_reset, resetState, io.write.bits.windex)
  val wdata = Mux(cState === s_reset, 0.U.asTypeOf(Vec(nWays, io.write.bits.wdata)), VecInit(Seq.fill(nWays)(io.write.bits.wdata)))
  val waymask = Mux(cState === s_reset, false.B, io.write.bits.waymask)

  when(cState === s_idle){
    when(io.invalidate.valid){
      nState := s_reset
      resetAsid := io.invalidate.bits
    }.elsewhen(io.write.valid){
      Entries.write(windex, VecInit(wdata.map(_.toBase._2)), waymask.asBools)
      for (((d, m), i) <- (wdata zip waymask.asBools).zipWithIndex) {
        when(m) {
          AsidV(windex)(i).v := d.flags(0)
          AsidV(windex)(i).asid := d.asid
        }
      }
    }
  }.elsewhen(cState === s_reset){ // Reset valid signal depends on ASID match (set by set
    AsidV(resetState).foreach{ av =>
      when(av.asid === resetAsid){
        av.v := false.B
      }
    }
    when(resetFin){
      nState := s_idle
      resetAsid := 0.U
    }
  }
  // change the valid bit with Storage2
  val readTlbOut = {
    val raw = Entries.read(io.rindex)
    val out = WireInit(raw)
    ((raw zip out) zip AsidV(io.rindex)).foreach{ case ((r, o), av) =>
      o := r
      val x = Wire(Vec(8, Bool()))
      x := VecInit(r.flags.asBools)
      x(0) := r.flags(0) & av.v
      o.flags := x.asUInt
    }
    out
  }
  // bypass write to read
  val tlbOut = WireInit(readTlbOut)
  when(io.write.valid && windex === io.rindex){
    for(i <- 0 until nWays){
      tlbOut(i) := Mux(waymask(i), wdata(i).toBase._2, readTlbOut)
    }
  }

  io.tlbOut := tlbOut
}

class L2Tlb(SV: SVParam, L2C: L2cache.InclusiveCacheParameters_lite) extends Module{
  val io = IO(new Bundle{
    val in = Flipped(DecoupledIO(new Bundle{
      val asid = UInt(SV.asidLen.W)
      val vaddr = UInt(SV.vaLen.W)
    }))
    val out = DecoupledIO(new Bundle{

    })
    val mem = DecoupledIO(new Bundle{
      val req = new TLBundleA_lite(L2C)
      val rsp = new TLBundleD_lite(L2C)
    })
  })

  val storage = Module(new L2TlbStorage(SV))
}