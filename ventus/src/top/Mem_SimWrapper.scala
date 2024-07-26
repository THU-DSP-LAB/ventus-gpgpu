package top

import L2cache.{TLBundleA_lite, TLBundleD_lite}
import chisel3._
import chisel3.util._
import top.parameters.{INST_CNT_2, l2cache_params, num_sm}

class Mem_SimIO(DATA_BYTE_LEN: Int, ADDR_WIDTH: Int) extends Bundle {
  val wr = new Bundle {
    val en = Output(Bool())
    val mask = Output(UInt(DATA_BYTE_LEN.W))
    val addr = Output(UInt(ADDR_WIDTH.W))
    val data = Output(UInt((8*DATA_BYTE_LEN).W))
  }
  val rd = new Bundle {
    val en = Output(Bool())
    val addr = Output(UInt(ADDR_WIDTH.W))
    val data = Input(UInt((8*DATA_BYTE_LEN).W))
  }
}

class Mem_SimWrapper(val genA: TLBundleA_lite, genD: TLBundleD_lite, DELAY_LDS: Int = 0, val DELAY_DDR: Int = 2) extends Module {
  val DATA_BYTE_LEN = genA.data.getWidth / 8
  val DEPTH = 5
  val io = IO(new Bundle {
    val req = Flipped(DecoupledIO(genA.cloneType))
    val rsp = DecoupledIO(genD.cloneType)
    val mem = new Mem_SimIO(DATA_BYTE_LEN, ADDR_WIDTH = parameters.MEM_ADDR_WIDTH)
  })

  val REQ_OPCODE_READ = 4.U
  val REQ_OPCODE_WRITE_PARTIAL = 1.U
  val REQ_OPCODE_WRITE_FULL = 0.U
  val LDS_ADDRESS_START = 0x70000000.U
  val LDS_ADDRESS_END = BigInt("80000000", 16).U

  val rsp_valid = RegInit(VecInit.fill(DEPTH)(false.B))
  val rsp_data = Reg(Vec(DEPTH, new Bundle {
    val delay = UInt(32.W)
    val data = genD.cloneType
  }))

  io.req.ready := !rsp_valid.asUInt.andR
  io.mem.wr.en := io.req.fire && (io.req.bits.opcode === REQ_OPCODE_WRITE_PARTIAL || io.req.bits.opcode === REQ_OPCODE_WRITE_FULL)
  io.mem.wr.addr := io.req.bits.address
  io.mem.wr.data := io.req.bits.data
  io.mem.wr.mask := io.req.bits.mask
  io.mem.rd.en   := io.req.fire && (io.req.bits.opcode === REQ_OPCODE_READ)
  io.mem.rd.addr := io.req.bits.address

  val rsp_gen = Wire(genD.cloneType)
  rsp_gen.opcode := (io.req.bits.opcode === REQ_OPCODE_READ)
  rsp_gen.data := io.mem.rd.data
  rsp_gen.source := io.req.bits.source
  rsp_gen.size := io.req.bits.size
  rsp_gen.param := io.req.bits.param

  val rsp_idle_ptr = PriorityEncoder(~rsp_valid.asUInt)
  when(io.req.fire) {
    rsp_data(rsp_idle_ptr).data := rsp_gen
    rsp_data(rsp_idle_ptr).delay := Mux(io.req.bits.address >= LDS_ADDRESS_START && io.req.bits.address < LDS_ADDRESS_END, DELAY_LDS.U, DELAY_DDR.U)
    rsp_valid(rsp_idle_ptr) := true.B
  }

  val rsp_delay_finish = WireInit(VecInit.fill(DEPTH)(false.B))
  for(i <- 0 until DEPTH) {
    when(rsp_valid(i)) {
      rsp_data(i).delay := Mux(rsp_data(i).delay > 0.U, rsp_data(i).delay - 1.U, 0.U)
      rsp_delay_finish(i) := rsp_data(i).delay === 0.U
    }
  }

  val rsp_delay_finish_idx = PriorityEncoder(rsp_delay_finish)
  io.rsp.valid := rsp_delay_finish.asUInt.orR
  io.rsp.bits := rsp_data(rsp_delay_finish_idx).data

  when(io.rsp.fire) {
    rsp_valid(rsp_delay_finish_idx) := false.B
  }
}

class GPGPU_SimTop extends Module {
  val DATA_BYTE_LEN = (new TLBundleA_lite(l2cache_params)).data.getWidth / 8
  val io = IO(new Bundle {
    val host_req = Flipped(DecoupledIO(new host2CTA_data))
    val host_rsp = DecoupledIO(new CTA2host_data)
    val mem = new Mem_SimIO(DATA_BYTE_LEN, ADDR_WIDTH = parameters.MEM_ADDR_WIDTH)
    val cnt = Output(UInt(32.W))
    //val inst_cnt = if(INST_CNT_2) Output(Vec(num_sm, Vec(2, UInt(32.W)))) else Output(Vec(num_sm, UInt(32.W)))
  })

  val gpgpu = Module{ new GPGPU_SimWrapper(FakeCache = false) }
  val mem = Module { new Mem_SimWrapper(gpgpu.io.out_a.bits, gpgpu.io.out_d.bits) }

  io.host_req <> gpgpu.io.host_req
  io.host_rsp <> gpgpu.io.host_rsp
  io.cnt <> gpgpu.io.cnt
  //io.inst_cnt <> gpgpu.io.inst_cnt

  gpgpu.io.out_a <> mem.io.req
  gpgpu.io.out_d <> mem.io.rsp
  io.mem <> mem.io.mem
}

object emitVerilog extends App {
  chisel3.emitVerilog(
    //new GPGPU_SimWrapper(FakeCache = false),
    new GPGPU_SimTop,
    Array("--target-dir", "generated/", "--target", "verilog")
  )
}

