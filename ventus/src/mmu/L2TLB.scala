package mmu

import L2cache.{SRAMTemplate, TLBundleA_lite, TLBundleD_lite, TLBundleD_lite_plus}
import chisel3._
import chisel3.util._
import freechips.rocketchip.util.{RandomReplacement, SetAssocLRU}
import chisel3.experimental.dataview._

trait L2TlbParam{
  def nSets = 16 // total Sets of all banks
  def nWays = 4
  def nSectors = 2
  def nBanks = 1

  def vpnL2TlbBundle(SV: SVParam) = new Bundle {
    val tag = UInt((SV.vpnLen - log2Up(nSets) - log2Up(nSectors)).W)
    val setIndex = UInt((log2Up(nSets)-log2Ceil(nBanks)).W)
    val bankIndex = UInt(log2Ceil(nBanks).W)
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

  val Entries = Mem(nSets/nBanks, Vec(nWays, new L2TlbEntry(SV))) // Storage1

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

class L2TlbAccelStorage(SV: SVParam, _nWays: Int, level: Int) extends Module with L2TlbParam {
  def vpnSplit(vpn: UInt): (UInt, UInt) = {
    val sectorIdx: UInt = vpn(level * SV.idxLen + log2Up(nSectors) - 1, level * SV.idxLen)
    val vpnBase: UInt = Cat(
      vpn(vpn.getWidth-1, level * SV.idxLen + log2Up(nSectors)),
      0.U((level * SV.idxLen + log2Up(nSectors)).W)
    )
    (vpnBase, sectorIdx)
  }
  val io = IO(new Bundle{
    val accelReq = Vec(nBanks, Flipped(ValidIO(new Bundle{
      val asid = UInt(SV.asidLen.W)
      val vpn = UInt(SV.vpnLen.W)
    })))
    val accelOut = Vec(nBanks, ValidIO(UInt(SV.ppnLen.W)))
    val refill = Flipped(ValidIO(new L2TlbEntryA(SV)))
    val invalidate = Flipped(ValidIO(UInt(SV.asidLen.W)))
  })
  val Entries = Reg(Vec(_nWays, new L2TlbEntryA(SV)))
  val Valids = Reg(Vec(_nWays, Bool()))
  val replace = new RandomReplacement(_nWays)
  val replaceWay = Mux(Valids.reduce(_ && _), PriorityEncoder(Valids), replace.get_replace_way(0.U))
  (0 until nBanks).foreach{ b =>
    val accelReq_split = vpnSplit(io.accelReq(b).bits.vpn)
    val hitVec = VecInit((Entries zip Valids).map{ case(m, v) => // asid match + vpn match + sector valid
      m.asid === io.accelReq(b).bits.asid && vpnSplit(m.vpn)._1 === accelReq_split._1 && m.flags(accelReq_split._2)(0) && v
    })
    val hitWay = PriorityEncoder(hitVec)
    val hit = hitVec.asUInt =/= 0.U

    // --- Output
    io.accelOut(b).valid := io.accelReq(b).valid && hit
    io.accelOut(b).bits := Mux(io.accelReq(b).valid && hit, Entries(hitWay).ppns(accelReq_split._2), 0.U(SV.ppnLen.W))
    // Output ---
  }
  when(io.invalidate.valid){
    (Entries zip Valids).foreach{ case(m, v) =>
      when(m.asid === io.invalidate.bits){
        m := 0.U.asTypeOf(m)
        v := false.B
      }
    }
  }.elsewhen(io.refill.valid){
    Entries(replaceWay) := io.refill.bits
    Valids(replaceWay) := true.B
  }
}

object TlbID extends L2TlbParam{
  def apply(implicit param: Option[L1Cache.HasRVGParameters]): UInt = {
    return param match {
      // MSB -> {L2Tlb Bank, cache id, isTLB} -> LSB
      case Some(param) => UInt((log2Ceil(nBanks) + log2Ceil(param.NSms * param.NCacheInSM) + 1).W)
      case None => UInt(8.W)
    }
  }
}

class L2TlbReq(SV: SVParam)(implicit param: Option[L1Cache.HasRVGParameters]) extends Bundle{
  val asid = UInt(SV.asidLen.W)
  //val ptbr = UInt(SV.xLen.W)
  val vpn = UInt(SV.vpnLen.W)
  val id: UInt = TlbID.apply
}
class L2TlbRsp(SV: SVParam)(implicit param: Option[L1Cache.HasRVGParameters]) extends Bundle{
  val id = TlbID.apply
  val ppn = UInt(SV.ppnLen.W)
  val flag = UInt(8.W)
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

class L2Tlb(
  SV: SVParam,
  debug: Boolean = false,
  L2C: Option[L2cache.InclusiveCacheParameters_lite] = None,
  accelSize: Int = 8
)(
  implicit val L1C: Option[L1Cache.HasRVGParameters]
)
  extends Module with L2TlbParam {
  assert(log2Ceil(nBanks) == log2Floor(nBanks))
  override def nSectors: Int = L2C match {
    case Some(l2c) => l2c.beatBytes / (SV.xLen / 8)
    case None => super.nSectors
  }
  val io = IO(new Bundle{
    val in = Vec(nBanks, Flipped(DecoupledIO(new L2TlbReq(SV))))
    val invalidate = Flipped(ValidIO(new Bundle{
      val asid = UInt(SV.asidLen.W)
    }))
    val out = Vec(nBanks, DecoupledIO(new L2TlbRsp(SV)))
    // Request Always Read!
    val mem_req = L2C match {
      case Some(l2c) => Vec(nBanks, DecoupledIO(new TLBundleA_lite(l2c))) // interconnect with xbar
      case None => Vec(nBanks, DecoupledIO(new Cache_Req(SV)))
    }
    val mem_rsp = Flipped(L2C match {
      case Some(l2c) => Vec(nBanks, DecoupledIO(new TLBundleD_lite(l2c))) // interconnect with xbar
      case None => Vec(nBanks, DecoupledIO(new Cache_Rsp(SV)))
    })
    val asid_req = Output(Vec(nBanks, UInt(SV.asidLen.W)))
    val ptbr_rsp = Flipped(Vec(nBanks, Valid(UInt(SV.xLen.W))))
  })

  val storageArray = Seq.fill(nBanks)(Module(new L2TlbStorage(SV)))
  val accelStorageArray = (1 until SV.levels).map{ i => Module(new L2TlbAccelStorage(SV, accelSize, i))}
  val accelOut_delay = accelStorageArray.map{ a => RegNext(a.io.accelOut) }
  val accelRefillArb = (1 until SV.levels).map{i => Module(new RRArbiter(new L2TlbEntryA(SV), nBanks)) }
  accelRefillArb.foreach{ a => a.io.out.ready := true.B }

  val walker = Module(new PTW(SV, nBanks, debug))

  val replace = Seq.fill(nBanks)(new SetAssocLRU(nSets, nWays, "lru"))
  val refillIndex = RegInit(VecInit(Seq.fill(nBanks)(0.U(log2Up(nSets).W))))
  val refillWay = VecInit((storageArray zip replace).map{ case(s, r) => Mux(s.io.wAvail.orR, PriorityEncoder(s.io.wAvail), r.way(s.io.write.bits.windex))})

  val refillData = Seq.fill(nBanks)(RegInit(0.U.asTypeOf(new L2TlbEntryA(SV))))

  val s_idle :: s_check :: s_ptw_req :: s_ptw_rsp :: s_reply :: Nil = Enum(5)
  val nextState = WireInit(VecInit(Seq.fill(nBanks)(s_idle)))
  val curState = nextState.map(RegNext(_))

  val cnt = new Counter(65535)

  (0 until nBanks).foreach{ i =>
    val in = io.in(i)
    val out = io.out(i)
    val cState = curState(i)
    val nState = nextState(i)
    val storage = storageArray(i)
    val ptw_req = walker.io.ptw_req(i)
    val ptw_rsp = walker.io.ptw_rsp(i)
    val asid_req = io.asid_req(i)
    val ptbr_rsp = io.ptbr_rsp(i)

    in.ready := cState === s_idle && !io.invalidate.valid

    val tlb_req = RegInit(0.U.asTypeOf(in.bits))
    storage.io.rindex := tlb_req.asTypeOf(vpnL2TlbBundle(SV)).setIndex
    val storage_rsp = storage.io.tlbOut // Bundle{vpn, ppn: Vec(nSectors, UInt), flags: Vec(nSectors, UInt)}
    val tlb_rsp = RegInit(0.U.asTypeOf(out.bits))

    val hitVec = VecInit(storage_rsp.map(m =>
      m.flags(tlb_req.vpn.asTypeOf(vpnL2TlbBundle(SV)).sectorIndex)(0)
        && (m.vpn.asTypeOf(vpnL2TlbBundle(SV)).tag === tlb_req.vpn.asTypeOf(vpnL2TlbBundle(SV)).tag)
        && (m.asid === tlb_req.asid)
    )).asUInt
    assert(PopCount(hitVec) <= 1.U)
    val hit = cState === s_check && hitVec.orR
    val miss = cState === s_check && !hitVec.orR

    // for SV39 VA {VPN2, VPN1, VPN0}:
    //   accelStorage(1) checks VPN2 and gives out level = 2 when match (2 walks to go)
    //   accelStorage(0) checks {VPN2, VPN1} and gives out level = 1 when match (1 walk to go)
    //   hit checks whole vpn and gives out level = 0 when match
    //   nothing match will gives out level = 3
    val accelLevel_pre = PriorityEncoder((hit +: accelStorageArray.map(_.io.accelOut(i).valid)) :+ true.B)
    val accelLevel = RegNext(accelLevel_pre)
    (0 until SV.levels-1).foreach{ j => // j for selecting level, i for selecting walker ways / tlb banks
      accelStorageArray(j).io.accelReq(i).valid := curState(i) === s_check
      accelStorageArray(j).io.accelReq(i).bits.asid := tlb_req.asid
      accelStorageArray(j).io.accelReq(i).bits.vpn := tlb_req.vpn

      val refill_level = walker.io.accel_fill(i).bits.cur_level
      accelRefillArb(j).io.in(i).valid := walker.io.accel_fill(i).valid && refill_level === j.U
      accelRefillArb(j).io.in(i).bits.asid := tlb_req.asid
      accelRefillArb(j).io.in(i).bits.vpn := accelStorageArray(j).vpnSplit(tlb_req.vpn)._1
      accelRefillArb(j).io.in(i).bits.level := i.U // likely unused at all
      accelRefillArb(j).io.in(i).bits.ppns := Mux(accelRefillArb(j).io.in(i).valid, walker.io.accel_fill(i).bits.ppns, 0.U.asTypeOf(accelRefillArb(j).io.in(i).bits.ppns))
      accelRefillArb(j).io.in(i).bits.flags := Mux(accelRefillArb(j).io.in(i).valid, walker.io.accel_fill(i).bits.flags, 0.U.asTypeOf(accelRefillArb(j).io.in(i).bits.flags))

      accelStorageArray(j).io.refill := accelRefillArb(j).io.out
      accelStorageArray(j).io.invalidate.valid := io.invalidate.valid
      accelStorageArray(j).io.invalidate.bits := io.invalidate.bits.asid
    }
    val accelPA = MuxLookup(accelLevel, 0.U,
      (1 to SV.levels).map(_.U) zip
        (accelOut_delay.map{a => Cat(a(i).bits, 0.U(SV.offsetLen.W))} :+ ptbr_rsp.bits)
    )

    ptw_req.bits.source := tlb_req.id
    ptw_req.bits.vpn := tlb_req.vpn
    ptw_req.bits.paddr := accelPA
    ptw_req.bits.curlevel := accelLevel - 1.U
    ptw_req.valid := cState === s_ptw_req

    ptw_rsp.ready := storage.io.ready && cState === s_ptw_rsp
    storage.io.write.valid := RegNext(cState === s_ptw_rsp && ptw_rsp.fire) // s_ptw_rsp -> [s_reply]
    storage.io.write.bits.waymask := UIntToOH(refillWay(i))
    storage.io.write.bits.wdata := refillData(i)
    storage.io.write.bits.windex := refillIndex(i)

    out.bits := tlb_rsp
    out.valid := cState === s_reply

    storage.io.invalidate.valid := io.invalidate.valid
    storage.io.invalidate.bits := io.invalidate.bits.asid

    asid_req := tlb_req.asid

    switch(cState){
      is(s_idle){
        when(in.fire){
          tlb_req := in.bits
          when(storage.io.ready && !io.invalidate.valid) {
            nState := s_check
          }.otherwise{
            nState := s_ptw_req
            refillData(i).asid := in.bits.asid
            refillData(i).vpn := in.bits.vpn
          }
        }
      }
      is(s_check){
        (0 until SV.levels - 1).foreach{ j =>
          accelStorageArray(j).io.accelReq(i).bits.asid := tlb_req.asid
          accelStorageArray(j).io.accelReq(i).bits.vpn := tlb_req.vpn
          accelStorageArray(j).io.accelReq(i).valid := true.B
        }
        when(hit){
          replace(i).access(storage.io.rindex, hitVec)
          tlb_rsp.id := tlb_req.id
          tlb_rsp.ppn := storage_rsp(OHToUInt(hitVec)).ppns(tlb_req.vpn.asTypeOf(vpnL2TlbBundle(SV)).sectorIndex)
          nState := s_reply
        }.otherwise{
          nState := s_ptw_req
          refillData(i).asid := tlb_req.asid
          refillData(i).vpn := tlb_req.vpn
          refillData(i).level := SV.levels.U
        }
      }
      is(s_ptw_req){
        when(ptw_req.fire){
          nState := s_ptw_rsp
        }.otherwise{ nState := s_ptw_req }
      }
      is(s_ptw_rsp){
        when(ptw_rsp.fire){
          refillIndex(i) := tlb_req.vpn.asTypeOf(vpnL2TlbBundle(SV)).setIndex
          refillData(i).ppns := ptw_rsp.bits.ppns
          refillData(i).flags := ptw_rsp.bits.flags
          replace(i).access(tlb_req.vpn.asTypeOf(vpnL2TlbBundle(SV)).setIndex, refillWay(i))

          tlb_rsp.id := tlb_req.id
          tlb_rsp.ppn := ptw_rsp.bits.ppns(tlb_req.vpn.asTypeOf(vpnL2TlbBundle(SV)).sectorIndex)
          tlb_rsp.flag := ptw_rsp.bits.flags(tlb_req.vpn.asTypeOf(vpnL2TlbBundle(SV)).sectorIndex)
          nState := s_reply
        }.otherwise{ nState := s_ptw_rsp }
      }
      is(s_reply){
        refillData(i) := 0.U.asTypeOf(refillData(i))
        when(out.fire){ nState := s_idle }.otherwise{ nState := s_reply }
      }
    }

    if (debug){
      when(in.fire){
        printf(p"[TLB${i} ${cnt.value}] ")
        printf(p"REQ | asid: 0x${Hexadecimal(in.bits.asid)} vpn: 0x${Hexadecimal(in.bits.vpn)}\n")
      }
      when(cState === s_check){
        printf(p"[TLB${i} ${cnt.value}] ")
        when(hit){ printf(p"-| HIT  | ") }
          .elsewhen(accelLevel_pre =/= SV.levels.U){ printf(p"-| AC ${accelLevel_pre} |") }
          .otherwise{ printf(p"-| MISS | ") }
        printf(p"line: ${storage.io.rindex} way: ${Decimal(OHToUInt(hitVec))} sector: ${tlb_req.vpn.asTypeOf(vpnL2TlbBundle(SV)).sectorIndex}\n")
      }
      when(ptw_req.fire){
        printf(p"[TLB${i} ${cnt.value}] ")
        printf(p"- -| >>PTW | ptbr: 0x${Hexadecimal(ptw_req.bits.paddr)} vpn: 0x${Hexadecimal(ptw_req.bits.vpn)}\n")
      }
      when(ptw_rsp.fire){
        printf(p"[TLB${i} ${cnt.value}] ")
        printf(p"- -| PTW>> | ppn+flag:")
        (0 until nSectors).foreach{ i =>
          printf(p" 0x${Hexadecimal(ptw_rsp.bits.ppns(i))}+${Hexadecimal(ptw_rsp.bits.flags(i))}")
        }
        printf("\n")
      }
      when(storage.io.write.valid){
        printf(p"[TLB${i} ${cnt.value}] ")
        printf(p"- -|REFILL | line: ${storage.io.write.bits.windex} way: ${Decimal(refillWay(i))}\n")
        printf(p"[TLB${i} ${cnt.value}]            | asid: 0x${Hexadecimal(storage.io.write.bits.wdata.asid)} vpn: 0x${Hexadecimal(storage.io.write.bits.wdata.vpn)} ppn+flag:")
        (0 until nSectors).foreach{ i =>
          printf(p" 0x${Hexadecimal(storage.io.write.bits.wdata.ppns(i))}+${Hexadecimal(storage.io.write.bits.wdata.flags(i))}")
        }
        printf("\n")
      }
      when(out.fire){
        printf(p"[TLB${i} ${cnt.value}] ")
        printf(p"RSP | ppn+flag: 0x${Hexadecimal(out.bits.ppn)}+${Hexadecimal(out.bits.flag)}\n")
      }
//      when(io.mem_req(i).fire || io.mem_rsp(i).fire){
//        printf(p"[MEM${i} ${cnt.value}] ")
//      }
    }
  }
  if(debug){
    cnt.inc()
  }
  io.mem_req <> walker.io.mem_req
  walker.io.mem_rsp <> io.mem_rsp
}

class L1ToL2TlbXBar(SV: SVParam, n_l1: Int = 1)(implicit val L1C: Option[L1Cache.HasRVGParameters])
  extends Module with L2TlbParam {
  val num_l1 = L1C match {
    case Some(x) => x.NSms * x.NCacheInSM
    case None => n_l1
  }
  val num_l2 = nBanks
  val io = IO(new Bundle{
    val req_l1 = Flipped(Vec(num_l1, DecoupledIO(new L2TlbReq(SV))))
    val req_l2 = Vec(num_l2, DecoupledIO(new L2TlbReq(SV)))
    val rsp_l1 = Vec(num_l1, DecoupledIO(new L2TlbReq(SV)))
    val rsp_l2 = Flipped(Vec(num_l2, DecoupledIO(new L2TlbRsp(SV))))
  })
  if(num_l1 == 1){
    for(j <- 0 until num_l2){
      val req_bank_idx = io.req_l1(0).bits.vpn.asTypeOf(vpnL2TlbBundle(SV)).bankIndex
      io.req_l2(j).valid := io.req_l1(0).valid && req_bank_idx === j.U
      io.req_l2(j).bits := io.req_l1(0).bits
      io.req_l2(j).bits.id := Cat(req_bank_idx(log2Ceil(nBanks)-1, 0), 1.U(1.W))
      io.req_l1(0).ready := (VecInit(io.req_l2.map(_.ready)).asUInt)(req_bank_idx)
    }
  }
  else {
    val arb_req = Seq.fill(num_l2)(Module(new RRArbiter(new L2TlbReq(SV), num_l1)))
    for (i <- 0 until num_l1;
         j <- 0 until num_l2) {
      val req_bank_idx = io.req_l1(i).bits.vpn.asTypeOf(vpnL2TlbBundle(SV)).bankIndex

      arb_req(j).io.in(i).valid := io.req_l1(i).valid && req_bank_idx === j.U
      arb_req(j).io.in(i).bits := Mux(io.req_l1(i).valid && req_bank_idx === j.U, io.req_l1(i).bits, 0.U.asTypeOf(new L2TlbReq(SV)))
      arb_req(j).io.in(i).bits.id := Cat(req_bank_idx(log2Ceil(nBanks)-1, 0), i.U(log2Ceil(num_l1).W), 1.U(1.W))
      io.req_l1(i).ready := (VecInit(arb_req.map {
        _.io.in(i).ready
      }).asUInt)(req_bank_idx)

      io.req_l2(j) <> arb_req(j).io.out
    }
  }

  if(num_l2 == 1){
    for(i <- 0 until num_l1){
      val reply_id = (io.rsp_l2(0).bits.id >> 1)(log2Ceil(num_l1) - 1, 0)
      io.rsp_l1(i).valid := io.rsp_l2(0).valid
      io.rsp_l1(i).bits := io.rsp_l2(0).bits
      io.rsp_l2(0).ready := (VecInit(io.rsp_l1.map(_.ready)).asUInt)(reply_id)
    }
  }
  else {
    val arb_rsp = Seq.fill(num_l1)(Module(new RRArbiter(new L2TlbRsp(SV), num_l2)))
    for (i <- 0 until num_l1;
         j <- 0 until num_l2) {

      val reply_id = (io.rsp_l2(j).bits.id >> 1)(log2Ceil(num_l1) - 1, 0)

      arb_rsp(i).io.in(j).valid := io.rsp_l2(j).valid && reply_id === i.U
      arb_rsp(i).io.in(j).bits := Mux(io.rsp_l2(j).valid && reply_id === i.U, io.rsp_l2(j).bits, 0.U.asTypeOf(new L2TlbRsp(SV)))
      io.rsp_l2(j).ready := (VecInit(arb_rsp.map{
        _.io.in(j).ready}
      ).asUInt)(reply_id)

      io.rsp_l1(i) <> arb_rsp(i).io.out
    }
  }
}
// send to L2C port by parseAddress()
class L2TlbToL2CacheXBar(
  SV: SVParam,
  nL2CReqPort: Int,
  L2C: L2cache.InclusiveCacheParameters_lite)
(
  implicit val L1C: L1Cache.HasRVGParameters
)extends Module
  with L2TlbParam{
  val nTlbMemPort = nBanks
  val io = IO(new Bundle{
    val req_tlb = Vec(nTlbMemPort, Flipped(DecoupledIO(new TLBundleA_lite(L2C))))
    val rsp_tlb = Vec(nTlbMemPort, DecoupledIO(new TLBundleD_lite(L2C)))
    val req_cache = Vec(nL2CReqPort, DecoupledIO(new TLBundleA_lite(L2C)))
    val rsp_cache = Vec(nL2CReqPort, Flipped(DecoupledIO(new TLBundleD_lite_plus(L2C))))
  })
  if(nTlbMemPort == 1){
    for(j <- 0 until nL2CReqPort){
      val req_idx = L2C.parseAddress(io.req_tlb(0).bits.address)._2
      io.req_cache(j).valid := io.req_tlb(0).valid & req_idx === j.U
      io.req_cache(j).bits := Mux(io.req_tlb(0).valid & req_idx === j.U, io.req_tlb(0).bits, 0.U.asTypeOf(new TLBundleA_lite(L2C)))
      io.req_tlb(0).ready := (VecInit(io.req_cache.map(_.ready)).asUInt)(req_idx)
    }
  }
  else{
    val arb_req = Seq.fill(nL2CReqPort)(Module(new RRArbiter(new TLBundleA_lite(L2C), nTlbMemPort)))
    for (i <- 0 until nTlbMemPort;
         j <- 0 until nL2CReqPort){
      val req_idx = L2C.parseAddress(io.req_tlb(i).bits.address)._2
      arb_req(j).io.in(i).valid := io.req_tlb(i).valid & req_idx === j.U
      arb_req(j).io.in(i).bits := Mux(io.req_tlb(i).valid & req_idx === j.U, io.req_tlb(i).bits, 0.U.asTypeOf(new TLBundleA_lite(L2C)))
      io.req_tlb(i).ready := (VecInit(arb_req.map{
        _.io.in(i).ready
      }).asUInt)(req_idx)
      io.req_cache(j) <> arb_req(j).io.out
    }
  }
  if(nL2CReqPort == 1){
    val reply_id = (io.rsp_cache(0).bits.source >> (1 + log2Ceil(L1C.NSms)))(log2Ceil(nTlbMemPort) - 1, 0)
    for(i <- 0 until nTlbMemPort) {
      io.rsp_tlb(i).valid := io.rsp_cache(0).valid & reply_id === i.U
      io.rsp_tlb(i).bits := Mux(io.rsp_cache(0).valid & reply_id === i.U, io.rsp_cache(0).bits, 0.U.asTypeOf(new TLBundleD_lite(L2C)))
      io.rsp_cache(0).ready := (VecInit(io.rsp_tlb.map(_.ready)).asUInt)(reply_id)
    }
  }
  else{
    val arb_rsp = Seq.fill(nTlbMemPort)(Module(new RRArbiter(new TLBundleD_lite(L2C), nL2CReqPort)))
    for (i <- 0 until nTlbMemPort;
         j <- 0 until nL2CReqPort) {
      val reply_id = (io.rsp_cache(j).bits.source >> (1 + log2Ceil(L1C.NSms)))(log2Ceil(nTlbMemPort)-1, 0)
      arb_rsp(i).io.in(j).valid := io.rsp_cache(j).valid & reply_id === i.U
      arb_rsp(i).io.in(j).bits := Mux(io.rsp_cache(j).valid & reply_id === i.U, io.rsp_cache(j).bits, 0.U.asTypeOf(new TLBundleD_lite(L2C)))
      io.rsp_cache(j).ready := (VecInit(arb_rsp.map{
        _.io.in(j).ready
      }).asUInt)(reply_id)
      io.rsp_tlb(i) <> arb_rsp(i).io.out
    }
  }
}