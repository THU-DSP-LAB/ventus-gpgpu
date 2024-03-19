package pipeline

import top.parameters._
import chisel3._
import chisel3.util.{log2Up, _}
import IDecode._
import L2cache.{TLBundleA_lite, TLBundleD_lite_plus}
import org.scalactic.TypeCheckedTripleEquals.convertToCheckingEqualizer
import chisel3.util._
import org.scalactic.NumericEqualityConstraints.numericEqualityConstraint

import scala.math.Ordered.orderingToOrdered

//parameters
var tma_aligned = 8 //bytes
var maxcopysize = 128 //bytes
var l2cachetagbits = 16
var l2cacheline = 128
var sharedcacheline = 64
def numgroupshared = sharedcacheline / tma_aligned  // num group in sharedcacheline: one tma_aligned is a group
def numgroupl2cache = l2cacheline / tma_aligned  // num group in sharedcacheline: one tma_aligned is a group
def numgroupentry = maxcopysize / tma_aligned // max num group per instruction
//Bundles
class TMATag extends Bundle{
  val copysize = UInt(log2Up(maxcopysize).W)
  val l2cachetag = Vec((maxcopysize / tma_aligned), UInt(l2cachetagbits.W))
  val addrinit = Vec((maxcopysize / tma_aligned), UInt((xLen - l2cachetagbits).W))
  val addrlast = Vec((maxcopysize / tma_aligned), UInt((xLen - l2cachetagbits).W))
}
class TempOutput extends Bundle{
  val tag = new MshrTag
  val data = Vec(num_thread, UInt(xLen.W))
}
//Modules
class Temp_mem extends Module {
  val io = IO(new Bundle {
    val from_addr = Flipped(DecoupledIO(new Bundle {
      val tag = Input(new TMATag)
    }))
    val idx_entry = Output(UInt(log2Up(tma_nMshrEntry).W))
    val from_l2cache = Flipped(DecoupledIO(new TLBundleD_lite_plus(l2cache_params)))
    val to_shared = DecoupledIO(new TempOutput)
  })
  val reg_req = RegInit(VecInit(Seq.fill(tma_nMshrEntry)(new TMATag)))
  val valid = RegInit(VecInit(Seq.fill(tma_nMshrEntry)(0.U((numgroupentry).W))))
  val shared = RegInit(VecInit(Seq.fill(tma_nMshrEntry)(0.U((numgroupentry).W))))
  val data = Mem(tma_nMshrEntry, Vec(numgroupentry, UInt((tma_aligned * 8).W)))
//  val tag = Mem(tma_nMshrEntry, UInt((io.from_addr.bits.tag.getWidth).W))
  val used = RegInit(0.U(tma_nMshrEntry.W))
  val valid_entry = Mux(used.andR, 0.U, PriorityEncoder(~used))
  val complete = VecInit(shared.map {
    _ === 1.U
  }).asUInt & used
  val output_entry = Mux(complete.orR, PriorityEncoder(complete), 0.U)
  // logic for sharedmem
  val shared_valid_index = Wire(VecInit(Seq.fill(tma_nMshrEntry)(UInt((log2Up(numgroupentry)).W)))) // find if there is data in mem enough to send to sharedmem
  val found_shared_valid = Wire(VecInit(Seq.fill(tma_nMshrEntry)(Bool())))
  for (i <- 0 until (tma_nMshrEntry)) {
    when(PopCount(valid(i)) === (reg_req(i).copysize / tma_aligned.U) && PopCount(shared(i)) =/= (reg_req(i).copysize / tma_aligned.U)) {
      shared_valid_index(i) := PriorityEncoder(valid(i) ^ shared(i))
      found_shared_valid(i) := true.B
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
//  val current_entry_index = io.from_l2cache.bits.source(log2Up(tma_nMshrEntry)-1,0).asUInt
  val current_tag = io.from_l2cache.bits.source(source_width -1, source_width -1 -l2cachetagbits + 1)
//  val current_tag_index = PriorityEncoder(reg_req(current_entry_index).l2cachetag.map(_===current_tag))


  //combinational logic to generate mask for l2cacheline
  val current_mask_l2cache = Wire(Vec(numgroupl2cache, Bool()))
  when(io.from_l2cache.fire){
    (0 until( numgroupl2cache)).foreach(x =>{
      current_mask_l2cache(x) := false.B
      when(reg_req(instruId).addrinit.asUInt >= x.U * tma_aligned){
        when(reg_req(instruId).addrlast.asUInt < (x.U + 1.U) * tma_aligned){
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
  when(io.from_l2cache.fire){
    (0 until(numgroupl2cache)).foreach( x=> {
      current_data_l2cache(x) := io.from_l2cache.bits.data((x + 1) * (tma_aligned * 8) - 1, x * (tma_aligned * 8))
    })
  }.otherwise{
    (0 until(numgroupl2cache)).foreach( x=> {
      current_data_l2cache(x) := 0.U
    })
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
    }
  }
  is(s_add){
    used := used.bitSet(valid_entry, true.B)
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
  }
}
//  val output_tag = tag.read(output_entry).asTypeOf(new MshrTag)
  val current_valid_index = shared_valid_index(PriorityEncoder(found_shared_valid.asUInt))
  val raw_data = data.read(PriorityEncoder(found_shared_valid.asUInt))
  val output_data = Wire(Vec(numgroupshared, UInt((tma_aligned * 8).W)))
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
}