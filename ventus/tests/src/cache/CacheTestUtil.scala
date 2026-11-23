package play.cache

import chisel3._
import chisel3.util._
import L2cache.{TLBundleA_lite_custom, TLBundleD_lite_plus_custom, InclusiveCacheParameters_lite, TLBundleA_lite, TLBundleD_lite_plus}
import L1Cache.AtomicUnit.AtomicUnit
import L1Cache.DCache._
import L1Cache._
import L1Cache.{ MyConfig}
import top.parameters._
import config.config.Parameters
import freechips.rocketchip.util.SeqToAugmentedSeq

class L2CacheUtil(params: InclusiveCacheParameters_lite) extends Module {
  val io = IO(new Bundle {
    val memReq = Flipped(Decoupled(new TLBundleA_lite_custom(params)))
    val memRsp = Decoupled(new TLBundleD_lite_plus_custom(params))
  })

  // ROM storage
  val romSize = 256  // 1KB ROM
  val rom = Mem(romSize, UInt((dcache_BlockWords * xLen).W))
  val init = RegInit(true.B)
  // Initialize ROM with some test data
  when(!init){
  for (i <- 0 until romSize) {
    rom.write(i.U, (i * 2).U)
  }
  init := false.B
}

  // Request queue
  val reqQueue = Module(new Queue(new Bundle {
    val addr = UInt(params.addressBits.W)
    val source = UInt(params.source_bits_custom.W)
    val opcode = UInt(3.W)
    val param = UInt(3.W)
    val size = UInt(3.W)
    val data = UInt((dcache_BlockWords*xLen).W)
    val mask = UInt((dcache_BlockWords*4).W)
  }, 4))

  // Default outputs
  io.memReq.ready := reqQueue.io.enq.ready
  io.memRsp.valid := false.B
  io.memRsp.bits := DontCare

  // Handle incoming requests
  reqQueue.io.enq.valid := io.memReq.valid
  reqQueue.io.enq.bits.addr := io.memReq.bits.address
  reqQueue.io.enq.bits.source := io.memReq.bits.source
  reqQueue.io.enq.bits.opcode := io.memReq.bits.opcode
  reqQueue.io.enq.bits.param := io.memReq.bits.param
  reqQueue.io.enq.bits.size := io.memReq.bits.size
  reqQueue.io.enq.bits.data := io.memReq.bits.data
  reqQueue.io.enq.bits.mask := io.memReq.bits.mask

  // Handle write operations immediately
  when(io.memReq.valid && (io.memReq.bits.opcode === 1.U || io.memReq.bits.opcode === 0.U)) {
    val writeAddr = io.memReq.bits.address >> (2+dcache_BlockOffsetBits)
    val writeData = io.memReq.bits.data
    val writeMask = io.memReq.bits.mask
    
    // Read current data
    val currentData = rom.read(writeAddr)
    
    // Apply mask and write new data
    // 将16位掩码扩展到128位，每个掩码位控制8位数据
    val expandedMask = Wire(Vec(16, UInt(8.W)))
    for (i <- 0 until 16) {
      expandedMask(i) := Mux(writeMask(i).asBool, 0xFF.U(8.W), 0.U(8.W))
    }
    val finalMask = expandedMask.asUInt
    val newData = (currentData & ~finalMask) | (writeData & finalMask)
    rom.write(writeAddr, newData)
  }

  // Handle responses
  when(reqQueue.io.deq.valid) {
    io.memRsp.valid := true.B
    io.memRsp.bits.opcode := Mux(reqQueue.io.deq.bits.opcode === 4.U, 1.U, 0.U)
    io.memRsp.bits.param := reqQueue.io.deq.bits.param
    io.memRsp.bits.size := reqQueue.io.deq.bits.size
    io.memRsp.bits.source := reqQueue.io.deq.bits.source
    io.memRsp.bits.data := rom.read(reqQueue.io.deq.bits.addr >> (2+dcache_BlockOffsetBits))
    io.memRsp.bits.address := reqQueue.io.deq.bits.addr

    when(io.memRsp.ready) {
      reqQueue.io.deq.ready := true.B
    }.otherwise {
      reqQueue.io.deq.ready := false.B
    }
  }.otherwise {
    reqQueue.io.deq.ready := false.B
  }
}

class L2CacheUtillite(params: InclusiveCacheParameters_lite)(implicit p: Parameters) extends Module {
  val io = IO(new Bundle {
    val memReq = Flipped(Decoupled(new DCacheMemReq))
    val memRsp = Decoupled(new DCacheMemRsp)
  })

  // ROM storage
  val romSize = 256  // 1KB ROM
  val rom = Mem(romSize, UInt((dcache_BlockWords * xLen).W))
  
  // Initialize ROM with some test data
  for (i <- 0 until romSize) {
    rom.write(i.U, (i * 2).U)
  }

  // Request queue
  val reqQueue = Module(new Queue(new Bundle {
    val addr = UInt(params.addressBits.W)
    val source = UInt(params.source_bits.W)
    val opcode = UInt(3.W)
    val param = UInt(3.W)
    val size = UInt(3.W)
    val data = UInt((dcache_BlockWords*xLen).W)
    val mask = UInt((dcache_BlockWords*4).W)
  }, 4))

  // Default outputs
  io.memReq.ready := reqQueue.io.enq.ready
  io.memRsp.valid := false.B
  io.memRsp.bits := DontCare

  // Handle incoming requests
  reqQueue.io.enq.valid := io.memReq.valid
  reqQueue.io.enq.bits.addr := io.memReq.bits.a_addr.get
  reqQueue.io.enq.bits.source := io.memReq.bits.a_source
  reqQueue.io.enq.bits.opcode := io.memReq.bits.a_opcode
  reqQueue.io.enq.bits.param := io.memReq.bits.a_param
  reqQueue.io.enq.bits.size := DontCare
  reqQueue.io.enq.bits.data := io.memReq.bits.a_data.asUInt
  reqQueue.io.enq.bits.mask := io.memReq.bits.a_mask.asUInt

  // Handle write operations immediately
  when(io.memReq.valid && (io.memReq.bits.a_opcode === 1.U || io.memReq.bits.a_opcode === 0.U)) {
    val writeAddr = io.memReq.bits.a_addr.get >> (2+dcache_BlockOffsetBits)
    val writeData = io.memReq.bits.a_data
    val writeMask = io.memReq.bits.a_mask
    
    // Read current data
    val currentData = rom.read(writeAddr)
    
    // Apply mask and write new data
    val newData = (currentData & ~writeMask.asUInt) | (writeData.asUInt & writeMask.asUInt)
    rom.write(writeAddr, newData)
  }

  // Handle responses
  when(reqQueue.io.deq.valid) {
    io.memRsp.valid := true.B
    io.memRsp.bits.d_opcode := Mux(reqQueue.io.deq.bits.opcode === 4.U, 1.U, 0.U)
    io.memRsp.bits.d_param := reqQueue.io.deq.bits.param
  //  io.memRsp.bits.d_size := reqQueue.io.deq.bits.size
    io.memRsp.bits.d_source := reqQueue.io.deq.bits.source
    io.memRsp.bits.d_data := rom.read(reqQueue.io.deq.bits.addr >> (2+dcache_BlockOffsetBits)).asTypeOf(io.memRsp.bits.d_data)
    io.memRsp.bits.d_addr := reqQueue.io.deq.bits.addr

    when(io.memRsp.ready) {
      reqQueue.io.deq.ready := true.B
    }.otherwise {
      reqQueue.io.deq.ready := false.B
    }
  }.otherwise {
    reqQueue.io.deq.ready := false.B
  }
}

class AtomicWrapper(params: InclusiveCacheParameters_lite)(implicit p: Parameters) extends Module {
  val io = IO(new Bundle {
    val L12ATUmemReq = Flipped(Decoupled(new TLBundleA_lite(params)))
    val ATU2L1memRsp = Decoupled(new TLBundleD_lite_plus(params))
  })

  // Instantiate AtomicUnit
  val atomicUnit = Module(new AtomicUnit(params))
  
  // Instantiate L2ROM
  val l2 = Module(new L2CacheUtil(params))

  // Connect AtomicUnit to L2ROM
  atomicUnit.io.ATU2L2memReq <> l2.io.memReq
  atomicUnit.io.L22ATUmemRsp <> l2.io.memRsp

  // Connect external ports
  io.L12ATUmemReq <> atomicUnit.io.L12ATUmemReq
  io.ATU2L1memRsp <> atomicUnit.io.ATU2L1memRsp
} 

class DCacheWrapper(params: InclusiveCacheParameters_lite, SV: Option[mmu.SVParam] = None)(implicit p: Parameters) extends Module {
  val io = IO(new Bundle {
    val coreReq = Flipped(DecoupledIO(new DCacheCoreReq(SV)))
    val coreRsp = DecoupledIO(new DCacheCoreRsp)
  })
  val buffer = Module(new Queue(new DCacheCoreReq(SV),1))
  buffer.io.enq <> io.coreReq

  // Instantiate AtomicUnit
   val dcache = Module(new DataCachev2(SV)(p))
  
  // Instantiate L2ROM
  val l2 = Module(new L2CacheUtillite(params))

  dcache.io.coreReq <> buffer.io.deq
  dcache.io.coreRsp <> io.coreRsp
  dcache.io.memReq.get <> l2.io.memReq
  dcache.io.memRsp <> l2.io.memRsp
} 



class DCacheWrapperWithAtomic(params: InclusiveCacheParameters_lite, SV: Option[mmu.SVParam] = None)(implicit p: Parameters) extends Module {
  val io = IO(new Bundle {
    val coreReq = Flipped(DecoupledIO(new DCacheCoreReq(SV)))
    val coreRsp = DecoupledIO(new DCacheCoreRsp)
  })
  val buffer = Module(new Queue(new DCacheCoreReq(SV),1))
  buffer.io.enq <> io.coreReq

  // Instantiate AtomicUnit
   val dcache = Module(new DataCachev2(SV)(p))
   val ATUN =  Module(new AtomicUnit(params))
  
  // Instantiate L2ROM
  val l2 = Module(new L2CacheUtil(params))

  // Add converter for memReq

  
  dcache.io.coreReq <> buffer.io.deq
  dcache.io.coreRsp <> io.coreRsp

  ATUN.io.L12ATUmemReq.valid := dcache.io.memReq.get.valid
  ATUN.io.L12ATUmemReq.bits.opcode    := dcache.io.memReq.get.bits.a_opcode
  ATUN.io.L12ATUmemReq.bits.size      := DontCare
  ATUN.io.L12ATUmemReq.bits.source    := dcache.io.memReq.get.bits.a_source
  ATUN.io.L12ATUmemReq.bits.address   := dcache.io.memReq.get.bits.a_addr.get
  ATUN.io.L12ATUmemReq.bits.mask      := dcache.io.memReq.get.bits.a_mask.asTypeOf(ATUN.io.L12ATUmemReq.bits.mask)
  ATUN.io.L12ATUmemReq.bits.data      := dcache.io.memReq.get.bits.a_data.asTypeOf(ATUN.io.L12ATUmemReq.bits.data)
  ATUN.io.L12ATUmemReq.bits.param     := dcache.io.memReq.get.bits.a_param
  dcache.io.memReq.get.ready := ATUN.io.L12ATUmemReq.ready

  dcache.io.memRsp.valid  := ATUN.io.ATU2L1memRsp.valid
  dcache.io.memRsp.bits.d_opcode    :=   ATUN.io.ATU2L1memRsp.bits.opcode
  dcache.io.memRsp.bits.d_param     :=   ATUN.io.ATU2L1memRsp.bits.param
  dcache.io.memRsp.bits.d_addr      :=   ATUN.io.ATU2L1memRsp.bits.address
  dcache.io.memRsp.bits.d_data      :=   ATUN.io.ATU2L1memRsp.bits.data.asTypeOf(dcache.io.memRsp.bits.d_data)
  dcache.io.memRsp.bits.d_source    :=   ATUN.io.ATU2L1memRsp.bits.source
  ATUN.io.ATU2L1memRsp.ready := dcache.io.memRsp.ready

  ATUN.io.ATU2L2memReq <> l2.io.memReq
  ATUN.io.L22ATUmemRsp <> l2.io.memRsp
} 