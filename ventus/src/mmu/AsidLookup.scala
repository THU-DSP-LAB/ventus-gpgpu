package mmu

import chisel3._
import chisel3.util._

class AsidLookupEntry(SV: SVParam) extends Bundle{
  val asid = UInt(SV.asidLen.W)
  val ptbr = UInt(SV.xLen.W)
  val valid = Bool()
}

class AsidLookup(SV: SVParam, nBanks: Int, nEntries: Int) extends Module{
  val io = IO(new Bundle{
    val lookup_req = Input(Vec(nBanks, UInt(SV.asidLen.W)))
    val lookup_rsp = Output(Vec(nBanks, Valid(UInt(SV.xLen.W))))
    val fill_in = Flipped(ValidIO(new AsidLookupEntry(SV)))
    val flush_tlb = ValidIO(UInt(SV.asidLen.W))
  })
  val storage = Reg(Vec(nEntries, new AsidLookupEntry(SV)))

  val full = storage.map(_.valid).reduce(_ && _)
  val empty_vec = VecInit(storage.map(!_.valid))
  val fill_hitvec = VecInit(storage.map(e => e.asid === io.fill_in.bits.asid && e.valid))
  val flush_tlb = RegInit(0.U.asTypeOf(io.flush_tlb))
  flush_tlb := 0.U.asTypeOf(io.flush_tlb)

  when(io.fill_in.valid){
  //  printf(p"asid_lookup: recv valid input, asid = ${io.fill_in.bits.asid}\n")
    when(fill_hitvec.asUInt === 0.U && empty_vec.asUInt =/= 0.U){
      storage(PriorityEncoder(empty_vec)) := io.fill_in.bits
    }.elsewhen(fill_hitvec.asUInt =/= 0.U){ // if already exists, clear it.
      storage(PriorityEncoder(fill_hitvec)) := io.fill_in.bits
      flush_tlb.valid := true.B
      flush_tlb.bits := io.fill_in.bits.asid
    }
  }
  (0 until nBanks).foreach{ i =>
    val lookup_hitvec = VecInit(storage.map(e => e.asid === io.lookup_req(i) && e.valid))
    io.lookup_rsp(i).valid := lookup_hitvec.asUInt =/= 0.U
    io.lookup_rsp(i).bits := Mux(lookup_hitvec.asUInt =/= 0.U, storage(PriorityEncoder(lookup_hitvec)).ptbr, 0.U)
  }
  io.flush_tlb := flush_tlb
}
