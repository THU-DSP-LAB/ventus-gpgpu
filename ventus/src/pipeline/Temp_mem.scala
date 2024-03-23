package pipeline

import top.parameters._
import chisel3._
import chisel3.util.{log2Up, _}
import IDecode._
import L2cache.{InclusiveCacheParameters_lite, TLBundleA_lite, TLBundleD_lite_plus}
import org.scalactic.TypeCheckedTripleEquals.convertToCheckingEqualizer
import chisel3.util._
import org.scalactic.NumericEqualityConstraints.numericEqualityConstraint

import scala.math.Ordered.orderingToOrdered

//parameters
var cacheline = 128 //bytes
var tma_aligned = 1/2 //bytes
var maxcopysize = 1024 //bytes  all 5D multiply together
var mincopysize = 16 //bytes
var totaltempmem = 1024 // bytes
var l2cacheline = cacheline
var sharedcacheline = cacheline
var l2cachetagbits = 16
var l2cachesetbits = 4
var num_bank = 2
def numgroupshared = sharedcacheline / tma_aligned  // num group in sharedcacheline: one tma_aligned is a group
def numgroupl2cache = l2cacheline / tma_aligned  // num group in sharedcacheline: one tma_aligned is a group
def numgroupentry = l2cacheline / num_bank / tma_aligned // max num group per instruction

def addr_tag_bits = l2cachetagbits + l2cachesetbits

def num_tag_max = maxcopysize / cacheline / 2
def tma_nMshrEntry = 8// todo just set it to 8, can take 8 inst in a time
//def num_in_group = tma_aligned / (xLen / 8)
//Bundles
class l2cache_transform(params: InclusiveCacheParameters_lite) extends Bundle
{
  val opcode=UInt(params.op_bits.W)
  val size=UInt(params.size_bits.W)
  val source=UInt(params.source_bits.W)
  val data  = Vec(numgroupl2cache, UInt((tma_aligned * 8).W))
  val param =UInt(3.W)
}

class ShareMemCoreRsp_np_transform extends Bundle{
  val instrId = UInt(log2Up(tma_nMshrEntry).W)
  //  val data = Vec(num_thread, UInt(xLen.W))
  val activeMask = Vec(numgroupshared, Bool())//UInt(NLanes.W)
}

class TMATag extends Bundle{
  val copysize = UInt(log2Up(maxcopysize).W)
  val l2cachetag = Vec((num_tag_max), UInt(addr_tag_bits.W))
  val addrinit = Vec((num_tag_max), UInt((xLen - addr_tag_bits).W))
  val addrlast = Vec((num_tag_max), UInt((xLen - addr_tag_bits).W))
  val instruinfo = new vExeDataTMA()
}
class TempOutput extends Bundle{
  val entry_index = UInt(log2Up(tma_nMshrEntry).W)
  val inner_index = UInt(log2Up(numgroupentry).W)
  val data = Vec(numgroupl2cache, UInt((tma_aligned * 8).W))
  val instinfo = new vExeDataTMA()
}
class SharedInput extends Bundle{
  val entry_index = UInt(log2Up(tma_nMshrEntry).W)
  val inner_index = UInt(log2Up(numgroupentry).W)
  val data = Vec(num_thread, UInt((xLen).W))
  val instinfo = new vExeDataTMA()
}
//Modules
class l2cache2temp extends Module {
  val io = IO(new Bundle{
    val from_l2cache = Flipped(DecoupledIO(new TLBundleD_lite_plus(l2cache_params)))
    val l22temp = DecoupledIO(new l2cache_transform(l2cache_params))
  })
  io.l22temp.valid := io.from_l2cache.valid
  io.from_l2cache.ready := io.l22temp.ready
  (0 until(numgroupl2cache)).foreach( x=> {
    io.l22temp.bits.data(x) := io.from_l2cache.bits.data((x + 1) * (tma_aligned * 8) - 1, x * (tma_aligned * 8))
  })
  io.l22temp.bits.source := io.from_l2cache.bits.source
}
class shared2temp extends Module {
  val io = IO(new Bundle{
    val from_shared = Flipped(DecoupledIO(new ShareMemCoreRsp_np))
    val sm2temp = DecoupledIO(new ShareMemCoreRsp_np_transform)
  })
  io.sm2temp.valid := io.from_shared.valid
  io.from_shared.ready := io.sm2temp.ready
//  io.sm2temp.bits.data := io.from_shared.bits.data
  (0 until(numgroupentry) by (xLen / 8 / tma_aligned
    )).foreach( x=> {
    when((x+1) * tma_aligned - 1 <= num_thread.asUInt){
      io.sm2temp.bits.activeMask(x) := io.from_shared.bits.activeMask.asUInt((x+1) * tma_aligned - 1, x * tma_aligned).orR
    }.otherwise{
      io.sm2temp.bits.activeMask(x) := false.B
    }
  })
class temp2shared extends Module {
  val io = IO(new Bundle{
    val to_shared = Flipped(DecoupledIO(new TempOutput))
    val temp2shared = DecoupledIO(new SharedInput)
  })
  io.to_shared.valid := io.temp2shared.valid
  io.temp2shared.ready := io.to_shared.ready
  io.temp2shared.bits.entry_index := io.to_shared.bits.entry_index
  io.temp2shared.bits.inner_index := io.to_shared.bits.inner_index
  io.temp2shared.bits.instinfo    := io.to_shared.bits.instinfo
  val totalbits = Wire(UInt((tma_aligned * 8 * numgroupl2cache).W))
  (0 until(numgroupl2cache)).foreach( x=>{
    totalbits(tma_aligned * 8 * (x+1) - 1,tma_aligned * 8 * x) := io.to_shared.bits.data(x)
  })
  (0 until(num_thread)).foreach( x =>{
    when((num_thread * xLen < tma_aligned * 8 * numgroupl2cache).asBool){
      io.temp2shared.bits.data(x) := totalbits(xLen * (x+1) - 1,xLen* x)
    }.otherwise{
      io.temp2shared.bits.data(x) := 0.U
    }
  })
}
class Temp_mem extends Module {
  val io = IO(new Bundle {
    val from_addr = Flipped(DecoupledIO(new Bundle {
      val tag = Input(new TMATag)
    }))
    val idx_entry = Output(UInt(log2Up(tma_nMshrEntry).W))
    val from_l2cache = Flipped(DecoupledIO(new l2cache_transform(l2cache_params)))
    val from_shared = Flipped(DecoupledIO(new ShareMemCoreRsp_np))
    val to_shared = DecoupledIO(new TempOutput)
  })
  val reg_req = RegInit(VecInit(Seq.fill(tma_nMshrEntry)(new TMATag)))
  val valid = RegInit(VecInit(Seq.fill(tma_nMshrEntry)(0.U((numgroupentry).W))))
  val shared = RegInit(VecInit(Seq.fill(tma_nMshrEntry)(0.U((numgroupentry).W))))
  val finished = RegInit(VecInit(Seq.fill(tma_nMshrEntry)(0.U((numgroupentry).W))))
  val data = Mem(tma_nMshrEntry, Vec(numgroupentry, UInt((tma_aligned * 8).W)))
  val instmem = Mem(tma_nMshrEntry, UInt((io.from_addr.bits.tag.instruinfo.getWidth).W))
  val used = RegInit(0.U(tma_nMshrEntry.W))
  val valid_entry = Mux(used.andR, 0.U, PriorityEncoder(~used))

  val output_entry = Mux(complete.orR, PriorityEncoder(complete), 0.U)
  //logic for complete
//  val complete = VecInit(shared.map {_ === 1.U}).asUInt & used
  val complete = Wire(UInt(tma_nMshrEntry.W))
  (0 until(tma_nMshrEntry)).foreach( x=>{
    complete(x) := PopCount(finished(x)) === reg_req(x).copysize
  })
  // logic for sharedmem
  val shared_valid_index = Wire(VecInit(Seq.fill(tma_nMshrEntry)(UInt((log2Up(numgroupentry)).W)))) // find if there is data in mem enough to send to sharedmem
  val found_shared_valid = Wire(VecInit(Seq.fill(tma_nMshrEntry)(Bool())))
  val found_shared_full = Wire(VecInit(Seq.fill(tma_nMshrEntry)(Bool())))
  for (i <- 0 until (tma_nMshrEntry)) {
    when(PopCount(valid(i)) === (reg_req(i).copysize / tma_aligned.U) && PopCount(shared(i)) =/= (reg_req(i).copysize / tma_aligned.U)) {
      shared_valid_index(i) := PriorityEncoder(valid(i) ^ shared(i))
      found_shared_valid(i) := true.B
      found_shared_full(i)  :=  false.B
    }.otherwise {
      found_shared_valid(i) := false.B
      shared_valid_index(i) := 0.U
      for (j <- 0 until (numgroupentry) by numgroupshared) {
        when(j < reg_req(i).copysize - numgroupshared.U) {
          when((valid(i)(j + numgroupshared - 1,j) ^ shared(i)(j + numgroupshared - 1,j)).andR) {
            found_shared_valid(i) := true.B
            shared_valid_index(i) := j.U
          }
        }
      }
    }
  }
  val instruId = io.from_l2cache.bits.source(log2Up(tma_nMshrEntry)-1,0).asUInt
  val source_width = io.from_l2cache.bits.source.getWidth
  val current_entry_index = io.from_l2cache.bits.source(log2Up(tma_nMshrEntry)-1,0).asUInt
  val current_tag = io.from_l2cache.bits.source(source_width -1, source_width -1 -addr_tag_bits + 1)
  val current_tag_index = PriorityEncoder(reg_req(current_entry_index).l2cachetag.map(_===current_tag))


  //combinational logic to generate mask for l2cacheline
  val current_mask_l2cache = Wire(Vec(numgroupl2cache, Bool()))
  when(io.from_l2cache.fire){
    (0 until( numgroupl2cache)).foreach(x =>{
      current_mask_l2cache(x) := false.B
      when(reg_req(instruId).addrinit(current_tag_index).asUInt >= x.U * tma_aligned){
        when(reg_req(instruId).addrlast(current_tag_index).asUInt < (x.U + 1.U) * tma_aligned){
          current_mask_l2cache(x) := true.B
        }
      }
    })
  }.otherwise{
    (0 until( numgroupl2cache)).foreach(x =>{
      current_mask_l2cache(x) := false.B
    })
  }
  //combinational logic to generate mask for data tag, 1 for same tag
  val current_mask_tag = Wire(Vec(numgroupentry, Bool()))
  when(io.from_l2cache.fire){
    (0 until(numgroupentry)).foreach( x=> {
      current_mask_tag(x) := false.B
      when(x < reg_req(instruId).copysize){
        current_mask_tag(x) := current_tag === reg_req(instruId).l2cachetag(x)
      }
    })
  }.otherwise{
    (0 until(numgroupentry)).foreach( x=> {
      current_mask_tag(x) := false.B
    })
  }
  //combinational logic to generate mask for data entry, 1 just for valid data, not tag
  val priorityIdx = PriorityEncoder(current_mask_tag.asUInt)
  val current_mask_entry = Wire(Vec(numgroupentry, Bool()))
  val entry_tag_num = current_mask_tag.count(_ === true.B)
  (0 until( numgroupentry)).foreach( x=> {
    current_mask_entry(x) := false.B
    when(x >= priorityIdx){
      when((x < (priorityIdx + numgroupl2cache)).asBool && x < reg_req(instruId).copysize){
        when(entry_tag_num === numgroupl2cache.asUInt){
          current_mask_entry(x) := current_mask_l2cache(x.asUInt - priorityIdx.asUInt )
        }.otherwise{
          when(current_mask_tag(0) === true.B){
            current_mask_entry(x) := current_mask_l2cache(x.asUInt - priorityIdx.asUInt + (numgroupl2cache - entry_tag_num))
          }.otherwise{
            current_mask_entry(x) := current_mask_l2cache(x.asUInt - priorityIdx.asUInt )
          }
        }
      }
    }

  })

  //  when(io.from_l2cache.fire){ why it is wrong
//    current_mask_entry(priorityIdx + 1.U, priorityIdx) := 0.U
//
//  }

  // 3.19 1:20 here!
  //combinational logic to transfer cacline data into vec, granularity is tma_aligned
  //then use 2 masks, transfer the cacheline data into the num_entry data, cat zero or truncate zero from cacheline
  val current_data_l2cache = Wire(Vec(numgroupl2cache, UInt((tma_aligned * 8).W)))
//  when(io.from_l2cache.fire){
//    (0 until(numgroupl2cache)).foreach( x=> {
//      current_data_l2cache(x) := io.from_l2cache.bits.data((x + 1) * (tma_aligned * 8) - 1, x * (tma_aligned * 8))
//    })
//  }.otherwise{
//    (0 until(numgroupl2cache)).foreach( x=> {
//      current_data_l2cache(x) := 0.U
//    })
//  }
  when(io.from_l2cache.fire){
      current_data_l2cache := io.from_l2cache.bits.data
  }.otherwise{
      current_data_l2cache := 0.U
  }
  val current_data_entry = Wire(Vec(numgroupentry, UInt((tma_aligned * 8).W)))
  val indices = Wire(Vec(numgroupl2cache, UInt(log2Ceil(numgroupl2cache).W)))
  var count = 0.U
   (0 until numgroupl2cache).foreach( i => {
     indices(count) := Mux(current_mask_l2cache(i), i.U, 0.U)
     count := count + Mux(current_mask_l2cache(i), 1.U, 0.U)
   })
  indices.zipWithIndex.foreach { case (index, count) =>
    current_data_entry(count.asUInt + PriorityEncoder(current_mask_entry).asUInt) := current_data_l2cache(index)
  }

  val s_idle :: s_add :: s_shared ::s_reset:: Nil = Enum(3)
  val state = RegInit(s_idle)
  io.from_l2cache.ready := state === s_idle // && used.orR
  io.from_addr.ready := state === s_idle && !(used.andR)
  io.idx_entry := Mux(io.from_addr.fire, valid_entry, 0.U) // return the MSHR entrys

  when(state === s_idle) {
    when(io.from_l2cache.fire) {
      when(io.from_addr.fire) {
        state := s_add
      }.elsewhen(io.to_shared.ready && found_shared_valid.asUInt.orR) {
        state := s_shared
      }.elsewhen(complete.orR){
        state := s_reset
      }.otherwise {
        state := s_idle
      }
    }.elsewhen(found_shared_valid.asUInt.orR && io.to_shared.ready) {
      state := s_shared
    }.otherwise {
      state := s_idle
    }
  }.elsewhen(state === s_shared) {
    when(io.to_shared.ready && found_shared_valid.asUInt.orR) {
      state := s_shared
    }.elsewhen(complete.orR){
      state := s_reset
    }.otherwise {
      state := s_idle
    }
  }.elsewhen(state === s_add) {
    when(io.to_shared.ready && found_shared_valid.asUInt.orR) {
      state := s_shared
    }.elsewhen(complete.orR){
      state := s_reset
    }.otherwise {
      state := s_idle
    }
  }.elsewhen(state === s_reset){
    state:=s_idle
  }.otherwise {
    state := s_idle
  }
switch(state){
  is(s_idle){
    when(io.from_l2cache.fire){
      data.write(instruId, current_data_entry,current_mask_entry)
      valid(instruId) := valid(instruId) | current_mask_entry.asUInt
      when(io.from_addr.fire){reg_req(valid_entry) := io.from_addr.bits.tag}
    }.elsewhen(io.from_addr.fire){
      used := used.bitSet(valid_entry, true.B)
      data.write(valid_entry, VecInit(Seq.fill(numgroupentry)(0.U((tma_aligned * 8).W))))
      instmem.write(valid_entry, reg_req.asUInt)
//      instinfo.write(valid_entry, io.from_addr.bits.tag.instruinfo.asUInt)
    }
    when(io.from_shared.fire){
      finished(io.from_shared.bits.instrId) := finished (io.from_shared.bits.instrId) |  io.from_shared.bits.activeMask
  }
  is(s_add){
    used := used.bitSet(valid_entry, true.B)
//    instinfo.write(valid_entry, io.from_addr.bits.tag.instruinfo.asUInt)
    instmem.write(valid_entry, io.from_addr.bits.tag.instruinfo.asUInt)
    data.write(valid_entry, VecInit(Seq.fill(numgroupentry)(0.U((tma_aligned * 8).W))))
    valid(instruId) := valid(instruId) | current_mask_entry.asUInt
  }
  is(s_shared){
    when(io.to_shared.fire){
      (0 until(numgroupentry)).foreach( x=> {
        when(x >= shared_valid_index(PriorityEncoder(found_shared_valid.asUInt)).asUInt){
          when(x < shared_valid_index(PriorityEncoder(found_shared_valid.asUInt)) + numgroupshared.asUInt - 1.U){
            shared(PriorityEncoder(found_shared_valid.asUInt))(x) := 1.U
          }
        }

      })

    }
  }
  is(s_reset){
    used := used.bitSet(output_entry,false.B)
    valid(output_entry) := 0.U(numgroupentry.W)
    shared(output_entry)  := 0.U(numgroupentry.W)
    instmem.write(output_entry, 0.U(io.from_addr.bits.tag.instruinfo.getWidth.W))
  }
}
//  val output_tag = instinfo.read(output_entry).asTypeOf(new MshrTag)
  val current_valid_index = shared_valid_index(PriorityEncoder(found_shared_valid.asUInt))
  val raw_data = data.read(PriorityEncoder(found_shared_valid.asUInt))
  val output_data = Wire(Vec(numgroupl2cache, UInt((tma_aligned * 8).W)))
  var count_output = 0.U
  (0 until(numgroupentry)).foreach( x=> {
    when(x >= current_valid_index.asUInt){
      when(x < current_valid_index.asUInt + numgroupshared.asUInt - 1.U){
        output_data(count_output) := raw_data(x)
        count_output := count_output + 1.U
      }
    }
  })
//  output_data := raw_data(current_valid_index.asUInt + numgroupshared.asUInt - 1.U)
  io.to_shared.valid := state === s_shared
  io.to_shared.bits.data := output_data
  io.to_shared.bits.entry_index := PriorityEncoder(found_shared_valid.asUInt).asUInt
  io.to_shared.bits.inner_index := current_valid_index
  io.to_shared.bits.instinfo    := instmem.read(PriorityEncoder(found_shared_valid.asUInt))
//  io.to_shared.bits.instruinfo := instinfo.read(PriorityEncoder(found_shared_valid.asUInt))
}