//package pipeline
//
//import top.parameters._
//import chisel3._
//import chisel3.util.{log2Up, _}
//import IDecode._
//import L2cache.{TLBundleA_lite, TLBundleD_lite_plus}
//import org.scalactic.TypeCheckedTripleEquals.convertToCheckingEqualizer
//
//import top.parameters._
//import chisel3._
//import chisel3.util.{log2Up, _}
//import IDecode._
//import L2cache.{InclusiveCacheParameters_lite, TLBundleA_lite, TLBundleD_lite_plus}
//import org.scalactic.TypeCheckedTripleEquals.convertToCheckingEqualizer
//import chisel3.util._
//import scala.math.Ordered.orderingToOrdered
//
////parameters
//var cacheline = 128 //bytes
//var tma_aligned = 8 //bytes
//var maxcopysize = 128 //bytes  all 5D multiply together
//var mincopysize = 16 //bytes
//var totaltempmem = 512 // bytes
//var l2cacheline = cacheline
//var sharedcacheline = cacheline
//var l2cachetagbits = 16
//var l2cachesetbits = 4
//var sharedsetbits = 16
//var num_bank = 8
//def numgrouptotal = totaltempmem / tma_aligned
//def numgroupshared = sharedcacheline / tma_aligned  // num group in sharedcacheline: one tma_aligned is a group
//def numgroupl2cache = l2cacheline / tma_aligned  // num group in sharedcacheline: one tma_aligned is a group
//def numgroupinstmax = maxcopysize / tma_aligned
//def addr_tag_bits = l2cachetagbits + l2cachesetbits
//def num_entry = cacheline / num_bank/ tma_aligned
//
//def max_shared_in_cpysize = maxcopysize / sharedcacheline + 1
////def num_tag_max = maxcopysize / cacheline / 2
////def max_tma_inst = totaltempmem / mincopysize
////def max_tma_inst = 8// todo just set it to 8, can take 8 inst in a time
////def num_in_group = tma_aligned / (xLen / 8)
////Bundles
//class vExeDataTMA extends Bundle{
//  //  val mode = Bool()
//  //  val used = Bool()
//  //  val complete = Bool()
//  val in1 = UInt(xLen.W)
//  val in2 = UInt(xLen.W)
//  val srcsize = UInt(3.W)
//  val interleave = Bool()
//  //  val im2col = Vec(2,)
//  val ctrl=new CtrlSigs()
//}
//class l2cache_transform(params: InclusiveCacheParameters_lite) extends Bundle
//{
//  val opcode=UInt(params.op_bits.W)
//  val size=UInt(params.size_bits.W)
//  val source=UInt(params.source_bits.W)
//  val data  = Vec(numgroupl2cache, UInt((tma_aligned * 8).W))
//  val param =UInt(3.W)
//}
//
//class ShareMemCoreRsp_np_transform extends Bundle{
//  val instrId = UInt(log2Up(max_tma_inst).W)
//  //  val data = Vec(num_thread, UInt(xLen.W))
//  val activeMask = Vec(numgroupshared, Bool())//UInt(NLanes.W)
//}
//
//class TMATag extends Bundle{
//  val copysize = UInt(log2Up(maxcopysize).W)
//  val l2cachetag = Vec((numgroupinstmax), UInt(addr_tag_bits.W))
//  val addrinit = Vec((numgroupinstmax), UInt((xLen - addr_tag_bits).W))
//  val addrlast = Vec((numgroupinstmax), UInt((xLen - addr_tag_bits).W))
//  val instruinfo = new vExeDataTMA()
//}
//class TempOutput extends Bundle{
//  val entry_index = UInt(log2Up(max_tma_inst).W)
//  val inner_index = UInt(log2Up(numgroupinstmax).W)
//  val data = Vec(numgroupl2cache, UInt((tma_aligned * 8).W))
//  val instinfo = new vExeDataTMA()
//}
//class SharedInput extends Bundle{
//  val entry_index = UInt(log2Up(max_tma_inst).W)
//  val inner_index = UInt(log2Up(numgroupinstmax).W)
//  val data = Vec(num_thread, UInt((xLen).W))
//  val instinfo = new vExeDataTMA()
//}
////Modules
//class l2cache2temp extends Module {
//  val io = IO(new Bundle{
//    val from_l2cache = Flipped(DecoupledIO(new TLBundleD_lite_plus(l2cache_params)))
//    val l22temp = DecoupledIO(new l2cache_transform(l2cache_params))
//  })
//  io.l22temp.valid := io.from_l2cache.valid
//  io.from_l2cache.ready := io.l22temp.ready
//  (0 until(numgroupl2cache)).foreach( x=> {
//    io.l22temp.bits.data(x) := io.from_l2cache.bits.data((x + 1) * (tma_aligned * 8) - 1, x * (tma_aligned * 8))
//  })
//  io.l22temp.bits.source := io.from_l2cache.bits.source
//}
//class shared2temp extends Module {
//  val io = IO(new Bundle{
//    val from_shared = Flipped(DecoupledIO(new ShareMemCoreRsp_np))
//    val sm2temp = DecoupledIO(new ShareMemCoreRsp_np_transform)
//  })
//  io.sm2temp.valid := io.from_shared.valid
//  io.from_shared.ready := io.sm2temp.ready
//  //  io.sm2temp.bits.data := io.from_shared.bits.data
//  (0 until(numgroupinstmax)).foreach( x => {
//    io.sm2temp.bits.activeMask(x) := io.from_shared.bits.activeMask.asUInt((x+1) * ( tma_aligned / (xLen / 8)) - 1,x * ( tma_aligned / (xLen / 8))).andR
//  })
//  }
//  class temp2shared extends Module {
//    val io = IO(new Bundle{
//      val to_shared = Flipped(DecoupledIO(new TempOutput))
//      val temp2shared = DecoupledIO(new SharedInput)
//    })
//    io.to_shared.valid := io.temp2shared.valid
//    io.temp2shared.ready := io.to_shared.ready
//    io.temp2shared.bits.entry_index := io.to_shared.bits.entry_index
//    io.temp2shared.bits.inner_index := io.to_shared.bits.inner_index
//    io.temp2shared.bits.instinfo    := io.to_shared.bits.instinfo
//    val totalbits = Wire(UInt((tma_aligned * 8 * numgroupl2cache).W)) // get all bits
//    (0 until(numgroupl2cache)).foreach( x=>{
//      totalbits(tma_aligned * 8 * (x+1) - 1,tma_aligned * 8 * x) := io.to_shared.bits.data(x)
//    })
//    (0 until(num_thread)).foreach( x =>{
//      when((num_thread * xLen < tma_aligned * 8 * numgroupl2cache).asBool){
//        io.temp2shared.bits.data(x) := totalbits(xLen * (x+1) - 1,xLen* x)
//      }.otherwise{
//        io.temp2shared.bits.data(x) := 0.U
//      }
//    })
//  }
//
//  class Temp_mem extends Module {
//    val io = IO(new Bundle {
//      val from_addr = Flipped(DecoupledIO(new Bundle {
//        val tag = Input(new TMATag)
//      }))
//      val idx_entry = Output(UInt(log2Up(max_tma_inst).W))
//      val from_l2cache = Flipped(DecoupledIO(new l2cache_transform(l2cache_params)))
//      val from_shared = Flipped(DecoupledIO(new ShareMemCoreRsp_np))
//      val to_shared = DecoupledIO(new TempOutput)
//    })
//    val reg_req = RegInit(VecInit(Seq.fill(max_tma_inst)(new TMATag)))
//    val valid_list = RegInit(VecInit(Seq.fill(max_tma_inst)(0.U((numgroupinstmax).W))))
//    val shared = RegInit(VecInit(Seq.fill(max_tma_inst)(0.U((numgroupinstmax).W))))
//    val finished = RegInit(VecInit(Seq.fill(max_tma_inst)(0.U((numgroupinstmax).W))))
//
////    val data = Mem(max_tma_inst, Vec(numgroupinstmax, UInt((tma_aligned * 8).W)))
//    // the general datamem
//    val data = Seq.fill(num_bank)(Mem(totaltempmem / cacheline, Vec(num_entry, UInt((tma_aligned * 8).W))))
//    val indexInMem = RegInit(VecInit(Seq.fill(max_tma_inst)(VecInit(Seq.fill(2)(0.U(log2Up(numgrouptotal).W))))))
//    val instmem = Mem(max_tma_inst, UInt((io.from_addr.bits.tag.instruinfo.getWidth).W))
//    val used = RegInit(0.U(max_tma_inst.W))
//    val valid_entry = Mux(used.andR, 0.U, PriorityEncoder(~used))
//    val output_entry = Mux(complete.orR, PriorityEncoder(complete), 0.U)
//    //logic for complete
//    //  val complete = VecInit(shared.map {_ === 1.U}).asUInt & used
//    val complete = Wire(UInt(max_tma_inst.W))
//    (0 until(max_tma_inst)).foreach( x=>{
//      complete(x) := PopCount(finished(x)) === reg_req(x).copysize
//    })
//    // logic for sharedmem
//    // now the base addr of l2cache is not cacheline_aligned with sharedmem, but it is still continous in mem
////    val shared_first_group = WireInit(VecInit(Seq.fill(max_tma_inst)(UInt(log2Up(numgroupshared).W))))
////    val shared_index = WireInit(VecInit(Seq.fill(max_tma_inst)(VecInit(Seq.fill(max_shared_in_cpysize)(0.U(log2Up(numgroupinstmax).W))))))
////    val shared_index_valid = WireInit(VecInit(Seq.fill(max_tma_inst)(VecInit(Seq.fill(max_shared_in_cpysize)(Bool())))))
////    val shared_inst_mask = WireInit(VecInit(Seq.fill(max_tma_inst)(false.B)))
////    // get the index of each sharedmemcacheline, inner the inst mem range
////    (0 until(max_tma_inst)).foreach( x => {
////      var addr = reg_req(x).instruinfo.in2
////      when(addr(xLen - sharedsetbits - 1, 0).asUInt === 0.U){
////        shared_first_group(x) := numgroupshared.asUInt
////      }.otherwise{
////        shared_first_group(x) := (1.U-addr(xLen - sharedsetbits - 1, 0).asUInt / 8.U/ l2cacheline.asUInt) * numgroupshared
////      }
////    })
////    // get the valid index of sharedmemcache, inner inst entry
////    (0 until(max_tma_inst)).foreach( x => {
////      var curret_numgroupinst = reg_req(x).copysize / tma_aligned.asUInt
////      var cnt = 0
////      shared_inst_mask(x) := true.B
////      shared_index_valid(x):= 0.U
////      (0 until(numgroupinstmax) by numgroupshared).foreach( y => {
////        when(y < curret_numgroupinst) {
////          when( y.asUInt === 0.U){
////
////          }
////        }
////      })
////    })
////    (0 until(max_tma_inst)).foreach( x => {
////      var curret_numgroupinst = reg_req(x).copysize / tma_aligned.asUInt
////      var cnt = 0
////      (0 until(numgroupinstmax)).foreach( y => {
////        var current_addr = reg_req(x).instruinfo.in2 + y * tma_aligned.asUInt
////        var next_addr = reg_req(x).instruinfo.in2 + (y+1) * tma_aligned.asUInt
////        when(y.asUInt === 0.U){
////          shared_index(x)(cnt) := 0.asUInt
////          cnt = cnt + 1.U
////        }.elsewhen( y < curret_numgroupinst){
////          when(current_addr(xLen-1, xLen - l2cachetagbits) =/= next_addr(xLen-1, xLen - l2cachetagbits)){
////            shared_index(x)(cnt) := y.asUInt + 1.U
////            cnt = cnt + 1.U
////          }.otherwise{
////            cnt = cnt
////          }
////        }.otherwise{
////          shared_index(cnt) := curret_numgroupinst
////        }
////      })
////    })
//    // todo ask here: why it is wrong
////    (0 until(max_tma_inst)).foreach( x => {
////      (0 until(numgroupinstmax)).foreach( y => {
////        var first = shared_index(x)(y)
////        var last = (shared_index(x)(y + 1) - 1.U)
////        when(shared_index(x)(y) =/= 0.U && y.asUInt =/= 0.U ){
////          when(!shared_inst_mask(x) && valid_list(x)(last,first).asUInt.andR){
////            shared_index_valid
////          }
////        }
////      })
////    })
////    (0 until(max_tma_inst)).foreach( x=>{
////      var cnt = 0
////      (0 until(numgroupinstmax)).foreach( y=>{
////        when(y < reg_req(x).copysize / tma_aligned.asUInt){
////          when(y < shared_index(x)(cnt + 1))
////          {
////            when(y >= shared_index(x)(cnt)){
////              when(!(valid_list(x)(y) ^ shared(x)(y))){
////                shared_index_valid(x)(cnt) := false.B
////                cnt = cnt + 1
////              }
////            }
////          }
////        }
////      })
////    })
//
//
////    for (i <- 0 until (max_tma_inst)) {
////      when(PopCount(valid_list(i)) === (reg_req(i).copysize / tma_aligned.U) && PopCount(shared(i)) =/= (reg_req(i).copysize / tma_aligned.U)) {
////        shared_valid_index(i) := PriorityEncoder(valid_list(i) ^ shared(i))
////        found_shared_valid(i) := true.B
//////        found_shared_full(i)  :=  false.B
////      }.otherwise {
////        found_shared_valid(i) := false.B
////        shared_valid_index(i) := 0.U
////        for (j <- 0 until (numgroupinstmax) by numgroupshared) {
////          when(j < reg_req(i).copysize - numgroupshared.U) {
////            when((valid_list(i)(j + numgroupshared - 1,j) ^ shared(i)(j + numgroupshared - 1,j)).andR) {
////              found_shared_valid(i) := true.B
////              shared_valid_index(i) := j.U
////            }
////          }
////        }
////      }
////    }
//    val instruId = io.from_l2cache.bits.source(log2Up(max_tma_inst)-1,0).asUInt
//    val source_width = io.from_l2cache.bits.source.getWidth
//    val current_entry_index = io.from_l2cache.bits.source(log2Up(max_tma_inst)-1,0).asUInt
//    val current_tag = io.from_l2cache.bits.source(source_width -1, source_width -1 -addr_tag_bits + 1)
//    val current_tag_index = PriorityEncoder(reg_req(current_entry_index).l2cachetag.map(_===current_tag))
//
//
//    //combinational logic to generate mask for l2cacheline
//    val current_mask_l2cache = Wire(Vec(numgroupl2cache, Bool()))
//    when(io.from_l2cache.fire){
//      (0 until( numgroupl2cache)).foreach(x =>{
//        current_mask_l2cache(x) := false.B
//        when(reg_req(instruId).addrinit(current_tag_index).asUInt >= x.U * tma_aligned){
//          when(reg_req(instruId).addrlast(current_tag_index).asUInt < (x.U + 1.U) * tma_aligned){
//            current_mask_l2cache(x) := true.B
//          }
//        }
//      })
//    }.otherwise{
//      (0 until( numgroupl2cache)).foreach(x =>{
//        current_mask_l2cache(x) := false.B
//      })
//    }
//    //combinational logic to generate mask for data tag, 1 for same tag
//    val current_mask_tag = Wire(Vec(numgroupinstmax, Bool()))
//    when(io.from_l2cache.fire){
//      (0 until(numgroupinstmax)).foreach( x=> {
//        current_mask_tag(x) := false.B
//        when(x < reg_req(instruId).copysize){
//          current_mask_tag(x) := current_tag === reg_req(instruId).l2cachetag(x)
//        }
//      })
//    }.otherwise{
//      (0 until(numgroupinstmax)).foreach( x=> {
//        current_mask_tag(x) := false.B
//      })
//    }
//    //combinational logic to generate mask for data entry, 1 just for valid data, not tag
//    val priorityIdx = PriorityEncoder(current_mask_tag.asUInt)
//    val current_mask_entry = Wire(Vec(numgroupinstmax, Bool()))
//    val entry_tag_num = current_mask_tag.count(_ === true.B)
//    (0 until( numgroupinstmax)).foreach( x=> {
//      current_mask_entry(x) := false.B
//      when(x >= priorityIdx){
//        when((x < (priorityIdx + numgroupl2cache)).asBool && x < reg_req(instruId).copysize){
//          when(entry_tag_num === numgroupl2cache.asUInt){
//            current_mask_entry(x) := current_mask_l2cache(x.asUInt - priorityIdx.asUInt )
//          }.otherwise{
//            when(current_mask_tag(0) === true.B){
//              current_mask_entry(x) := current_mask_l2cache(x.asUInt - priorityIdx.asUInt + (numgroupl2cache - entry_tag_num))
//            }.otherwise{
//              current_mask_entry(x) := current_mask_l2cache(x.asUInt - priorityIdx.asUInt )
//            }
//          }
//        }
//      }
//
//    })
//
//    //  when(io.from_l2cache.fire){ why it is wrong
//    //    current_mask_entry(priorityIdx + 1.U, priorityIdx) := 0.U
//    //
//    //  }
//
//    // 3.19 1:20 here!
//    //combinational logic to transfer cacline data into vec, granularity is tma_aligned
//    //then use 2 masks, transfer the cacheline data into the num_entry data, cat zero or truncate zero from cacheline
//    val current_data_l2cache = Wire(Vec(numgroupl2cache, UInt((tma_aligned * 8).W)))
//    //  when(io.from_l2cache.fire){
//    //    (0 until(numgroupl2cache)).foreach( x=> {
//    //      current_data_l2cache(x) := io.from_l2cache.bits.data((x + 1) * (tma_aligned * 8) - 1, x * (tma_aligned * 8))
//    //    })
//    //  }.otherwise{
//    //    (0 until(numgroupl2cache)).foreach( x=> {
//    //      current_data_l2cache(x) := 0.U
//    //    })
//    //  }
//    when(io.from_l2cache.fire){
//      current_data_l2cache := io.from_l2cache.bits.data
//    }.otherwise{
//      current_data_l2cache := 0.U
//    }
//    val current_data_entry = Wire(Vec(numgroupinstmax, UInt((tma_aligned * 8).W)))
//    val indices = Wire(Vec(numgroupl2cache, UInt(log2Ceil(numgroupl2cache).W)))
//    var count = 0.U
//    (0 until numgroupl2cache).foreach( i => {
//      indices(count) := Mux(current_mask_l2cache(i), i.U, 0.U)
//      count := count + Mux(current_mask_l2cache(i), 1.U, 0.U)
//    })
//    indices.zipWithIndex.foreach { case (index, count) =>
//      current_data_entry(count.asUInt + PriorityEncoder(current_mask_entry).asUInt) := current_data_l2cache(index)
//    }
//
//    val s_idle :: s_add :: s_shared ::s_reset:: Nil = Enum(3)
//    val state = RegInit(s_idle)
//    io.from_l2cache.ready := state === s_idle // && used.orR
//    io.from_addr.ready := state === s_idle && !(used.andR)
//    io.idx_entry := Mux(io.from_addr.fire, valid_entry, 0.U) // return the MSHR entrys
//
//    when(state === s_idle) {
//      when(io.from_l2cache.fire) {
//        when(io.from_addr.fire) {
//          state := s_add
//        }.elsewhen(io.to_shared.ready && found_shared_valid.asUInt.orR) {
//          state := s_shared
//        }.elsewhen(complete.orR){
//          state := s_reset
//        }.otherwise {
//          state := s_idle
//        }
//      }.elsewhen(found_shared_valid.asUInt.orR && io.to_shared.ready) {
//        state := s_shared
//      }.otherwise {
//        state := s_idle
//      }
//    }.elsewhen(state === s_shared) {
//      when(io.to_shared.ready && found_shared_valid.asUInt.orR) {
//        state := s_shared
//      }.elsewhen(complete.orR){
//        state := s_reset
//      }.otherwise {
//        state := s_idle
//      }
//    }.elsewhen(state === s_add) {
//      when(io.to_shared.ready && found_shared_valid.asUInt.orR) {
//        state := s_shared
//      }.elsewhen(complete.orR){
//        state := s_reset
//      }.otherwise {
//        state := s_idle
//      }
//    }.elsewhen(state === s_reset){
//      state:=s_idle
//    }.otherwise {
//      state := s_idle
//    }
//    switch(state){
//      is(s_idle){
//        when(io.from_l2cache.fire){
//          data.write(instruId, current_data_entry,current_mask_entry)
//          valid_list(instruId) := valid_list(instruId) | current_mask_entry.asUInt
//          when(io.from_addr.fire){reg_req(valid_entry) := io.from_addr.bits.tag}
//        }.elsewhen(io.from_addr.fire){
//          used := used.bitSet(valid_entry, true.B)
//          data.write(valid_entry, VecInit(Seq.fill(numgroupinstmax)(0.U((tma_aligned * 8).W))))
//          instmem.write(valid_entry, reg_req.asUInt)
//          //      instinfo.write(valid_entry, io.from_addr.bits.tag.instruinfo.asUInt)
//        }
//        when(io.from_shared.fire){
//          finished(io.from_shared.bits.instrId) := finished (io.from_shared.bits.instrId) |  io.from_shared.bits.activeMask
//        }
//        is(s_add){
//          used := used.bitSet(valid_entry, true.B)
//          //    instinfo.write(valid_entry, io.from_addr.bits.tag.instruinfo.asUInt)
//          instmem.write(valid_entry, io.from_addr.bits.tag.instruinfo.asUInt)
//          data.write(valid_entry, VecInit(Seq.fill(numgroupinstmax)(0.U((tma_aligned * 8).W))))
//          valid_list(instruId) := valid_list(instruId) | current_mask_entry.asUInt
//        }
//        is(s_shared){
//          when(io.to_shared.fire){
//            (0 until(numgroupinstmax)).foreach( x=> {
//              when(x >= shared_valid_index(PriorityEncoder(found_shared_valid.asUInt)).asUInt){
//                when(x < shared_valid_index(PriorityEncoder(found_shared_valid.asUInt)) + numgroupshared.asUInt - 1.U){
//                  shared(PriorityEncoder(found_shared_valid.asUInt))(x) := 1.U
//                }
//              }
//
//            })
//
//          }
//        }
//        is(s_reset){
//          used := used.bitSet(output_entry,false.B)
//          valid_list(output_entry) := 0.U(numgroupinstmax.W)
//          shared(output_entry)  := 0.U(numgroupinstmax.W)
//          instmem.write(output_entry, 0.U(io.from_addr.bits.tag.instruinfo.getWidth.W))
//        }
//      }
//      //  val output_tag = instinfo.read(output_entry).asTypeOf(new MshrTag)
//      val current_valid_index = shared_valid_index(PriorityEncoder(found_shared_valid.asUInt))
//      val raw_data = data.read(PriorityEncoder(found_shared_valid.asUInt))
//      val output_data = Wire(Vec(numgroupl2cache, UInt((tma_aligned * 8).W)))
//      var count_output = 0.U
//      (0 until(numgroupinstmax)).foreach( x=> {
//        when(x >= current_valid_index.asUInt){
//          when(x < current_valid_index.asUInt + numgroupshared.asUInt - 1.U){
//            output_data(count_output) := raw_data(x)
//            count_output := count_output + 1.U
//          }
//        }
//      })
//      //  output_data := raw_data(current_valid_index.asUInt + numgroupshared.asUInt - 1.U)
//      io.to_shared.valid := state === s_shared
//      io.to_shared.bits.data := output_data
//      io.to_shared.bits.entry_index := PriorityEncoder(found_shared_valid.asUInt).asUInt
//      io.to_shared.bits.inner_index := current_valid_index
//      io.to_shared.bits.instinfo    := instmem.read(PriorityEncoder(found_shared_valid.asUInt))
//      //  io.to_shared.bits.instruinfo := instinfo.read(PriorityEncoder(found_shared_valid.asUInt))
//    }
//
//
//
//// upon this line, there is the new parameter definition
//
//class AddrCalc_l2cache() extends Module{
//  val io = IO(new Bundle{
//    val from_fifo = Flipped(DecoupledIO(new vExeData))
//    //    val csr_wid = Output(UInt(depth_warp.W))
//    //    val csr_pds = Input(UInt(xLen.W))
//    //    val csr_numw = Input(UInt(xLen.W))
//    //    val csr_tid = Input(UInt(xLen.W))
//    val to_tempmem = DecoupledIO(new Bundle{
//      val tag = new TMATag
//    })
//    val idx_entry = Input(UInt(log2Up(max_tma_inst).W))
//    val to_l2cache = DecoupledIO(new TLBundleA_lite(l2cache_params))
//    )
//    //    val to_shared = DecoupledIO(new ShareMemCoreReq_np)
//  })
//  //  vExeData.in1: Dcache base addr
//  //  vExeData.in2: Sharedmem base addr
//  //  vExeData.ctrl.imm_ext: copysize
//  //  maybe 4*4*2 byte one cp.async
//  //  vExeData.ctrl.memw/h/b: data type : determine the mask sent to L1 cache
//  val s_idle :: s_save :: s_l2cache :: Nil = Enum(3)
//  val state = RegInit(init = s_idle)
//  val reg_save = Reg(new vExeData)
//  val current_numgroup = reg_save.ctrl.imm_ext.asUInt /tma_aligned.asUInt
//  val cnt = new Counter(n = numgroupinstmax)
//  val reg_entryID = RegInit(0.U(log2Up(max_tma_inst).W))
//  val addr = Wire(Vec(numgroupinstmax,UInt(xLen.W)))
//  (0 until(numgroupinstmax)).foreach(x => { // assume the data stored in memory continuously
//    addr(x) := Mux(x < current_numgroup, reg_save.in1(0) + x * tma_aligned, 0.U(xLen.W))
//  })
//  val addr_wire=Wire(UInt(xLen.W))
//  addr_wire:=addr(PriorityEncoder(reg_save.mask.asUInt))
//  val current_tag = Mux(reg_save.mask.asUInt =/=0.U, addr_wire(xLen-1, xLen-1-l2cachetagbits+1), 0.U(l2cachetagbits.W))
//  val setIdx = Mux(reg_save.mask.asUInt=/=0.U, addr_wire(xLen-1-l2cachetagbits, xLen-1-l2cachetagbits-dcache_SetIdxBits+1), 0.U(dcache_SetIdxBits.W))
//  val blockOffset = Wire(Vec(numgroupinstmax, UInt(dcache_BlockOffsetBits.W)))
//  (0 until numgroupinstmax).foreach( x => blockOffset(x) := addr(x)(10, 2) )
//  val wordOffset1H = Wire(Vec(numgroupinstmax, UInt(BytesOfWord.W)))
//  (0 until numgroupinstmax).foreach( x => {
//    //DONE: Add Control Signals in vExeData.ctrl and define lw lh lb
//    wordOffset1H(x) := 15.U(4.W)
//    switch(reg_save.ctrl.mem_whb){
//      is(MEM_W) { wordOffset1H(x) := 15.U }
//      is(MEM_H) { wordOffset1H(x) :=
//        Mux(addr(x)(1)===0.U,
//          3.U,
//          12.U
//        )
//      }
//      is(MEM_B) { wordOffset1H(x) := 1.U << addr(x)(1,0) }
//    }
//  })
//  val l2cachetag = Wire(Vec(numgroupinstmax, UInt(l2cachetagbits.W)))
//  // assume all data in the same group has the same tag
//  l2cachetag := addr.map(_(xLen-1, xLen - l2cachetagbits))
//  val addrinit = Wire(Vec((numgroupinstmax), UInt((xLen - l2cachetagbits).W)))
//  val addrlast = Wire(Vec((numgroupinstmax), UInt((xLen - l2cachetagbits).W)))
//  var addrinit_cnt = 0.U
//  (0 until(numgroupinstmax)).foreach(x => { // assume the data stored in memory continuously
//    when(x < current_numgroup) {
//      when(x.U === 0.U) {
//        addrinit(addrinit_cnt) := addr(x)(xLen - l2cachetagbits - 1, 0)
//        addrinit_cnt := addrinit_cnt + 1.U
//      }.otherwise {
//        when(addr(x)(xLen - 1, xLen - l2cachetagbits) =/= addr(x - 1)(xLen - 1, xLen - l2cachetagbits)) {
//          addrinit(addrinit_cnt) := addr(x)(xLen - l2cachetagbits - 1, 0)
//          addrinit_cnt := addrinit_cnt + 1.U
//        }
//      }
//    }.otherwise {
//      addrinit(x) := 0.U
//    }
//  })
//  var addrlast_cnt = 0.U
//  (0 until(numgroupinstmax)).foreach(x => { // assume the data stored in memory continuously
//    when(x < current_numgroup){
//      when(x.U === current_numgroup - 1.U){
//        addrlast(addrlast_cnt) := addr(x)(xLen - l2cachetagbits -1 ,0) + tma_aligned.asUInt - 1.U // todo may be wrong
//        addrlast_cnt := addrlast_cnt + 1.U
//      }.otherwise{
//        when(addr(x)(xLen-1, xLen - l2cachetagbits) =/= addr(x + 1)(xLen-1, xLen - l2cachetagbits)){
//          addrlast(addrlast_cnt) := addr(x)(xLen - l2cachetagbits -1 ,0) + tma_aligned.asUInt - 1.U
//          addrlast_cnt := addrlast_cnt + 1.U
//        }
//      }
//    }.otherwise{
//      addrlast(x) := 0.U
//    }
//  })
////switch(reg_save.ctrl.mem_whb){
////    is(MEM_W){
////      srcsize := 4.U
////    }
////    is(MEM_H){
////      srcsize := 2.U
////    }
////    is(MEM_B){
////      srcsize := 1.U
////    }
////  }
//  io.to_tempmem.valid := state===s_save// & (reg_save.ctrl.mem_cmd.orR)
//  io.to_tempmem.bits.tag.copysize := reg_save.ctrl.imm_ext
//  io.to_tempmem.bits.tag.l2cachetag := l2cachetag
//  io.to_tempmem.bits.tag.addrinit := addrinit
//  io.to_tempmem.bits.tag.addrlast := addrlast
////  io.to_tempmem.bits.tag.instruinfo :=reg_save
//  io.to_l2cache.bits.opcode := 4.U
//  io.to_l2cache.bits.size   := 0.U
//  io.to_l2cache.bits.source :=  Cat(current_tag, reg_entryID)
//  // temporarily set it as reg_entryID, for mshr; cat with wid, for shiftboard; jingguo ceshi hui fangzai hou 6 bit
//  io.to_l2cache.bits.address := Cat(addr_wire(xLen-1, xLen-1-l2cachetagbits-dcache_SetIdxBits+1),0.U((xLen - l2cachetagbits-dcache_SetIdxBits).W))
//  io.to_l2cache.bits.mask   := 0.U
//  io.to_l2cache.bits.data   :=  0.U
//  io.to_l2cache.bits.param  :=  0.U
//  io.to_l2cache.valid := (state===s_l2cache)
//  val mask_next = Wire(Vec(numgroupinstmax, Bool()))
//  (0 until numgroupinstmax).foreach( x => {                          // update mask
//    mask_next(x) := reg_save.mask(x) && !(addr(x)(xLen-1, xLen-1-l2cachetagbits+1)===current_tag && addr(x)(xLen-1-l2cachetagbits, xLen-1-l2cachetagbits-dcache_SetIdxBits+1)===setIdx
//    )})
//  // End of Addr Logic
//
//
//  io.from_fifo.ready := state === s_idle
//
//  // FSM State Transfer
//  switch(state){
//    is (s_idle){
//      when(io.from_fifo.valid){ state := s_save }.otherwise{ state := state }
//      cnt.reset()
//    }
//    is (s_save){
////      when(reg_save.ctrl.mem_cmd.orR){ // read or write
//        when(io.to_tempmem.fire){state := s_l2cache}.otherwise{state := s_save}
////      }.otherwise{ state := s_idle }
//    }
//    is (s_l2cache){
//      when(io.to_l2cache.fire){
//        when (cnt.value >= current_numgroup || mask_next.asUInt===0.U){
//          cnt.reset(); state := s_idle
//        }.otherwise{
//          cnt.inc(); state := s_l2cache
//        }
//      }.otherwise{state := s_l2cache}
//    }
//  }
//  // FSM Operation
//  switch(state){
//    is (s_idle){
//      when(io.from_fifo.fire){ // Next: s_save
//        reg_save := io.from_fifo.bits   // save data
//        (0 until numgroupinstmax).foreach(x => {
//          reg_save.mask := Mux(x < current_numgroup, true, false)  //initialize the first copynum bit to 1
//        })
//      }.otherwise{reg_save := RegInit(0.U.asTypeOf(new vExeData))}
//    }
//    is (s_save){
////      when(reg_save.ctrl.mem_cmd.orR){//===1.U){  // read
//      when(io.to_tempmem.fire){reg_entryID := io.idx_entry}  // get entryID from MSHR
////      }
//    }
//    is (s_l2cache){
//      when(io.to_l2cache.fire){                                      // request is sent
//        reg_save.mask := mask_next
//      }.otherwise{
//        reg_save.mask := reg_save.mask
//      }
//    }
//  }
//
//}
//class AddrCalc_shared() extends Module{
//  val io = IO(new Bundle{
//    val from_fifo = Flipped(DecoupledIO(new vExeDataTMA))
//    val from_tempmem = Flipped(DecoupledIO(new TempOutput))
//    //    val csr_wid = Output(UInt(depth_warp.W))
//    //    val csr_pds = Input(UInt(xLen.W))
//    //    val csr_numw = Input(UInt(xLen.W))
//    //    val csr_tid = Input(UInt(xLen.W))
//    val to_mshr = DecoupledIO(new Bundle{
//      val tag = new MshrTag
//    })
//    val idx_entry = Input(UInt(log2Up(max_tma_inst).W))
//    //    val to_l2cache = DecoupledIO(new DCacheCoreReq_np)
//    val to_shared = DecoupledIO(new ShareMemCoreReq_np)
//  })
//  val instructmem = Mem(max_tma_inst,UInt((io.from_fifo.bits.getWidth.W)))
//  val s_idle :: s_save :: s_shared :: Nil = Enum(3)
//  val state = RegInit(init = s_idle)
//  val reg_save = Reg(new vExeData)
//  val current_numgroup = reg_save.ctrl.imm_ext.asUInt /tma_aligned.asUInt
//  val cnt = new Counter(n = (numgroupinstmax))
//  val reg_entryID = RegInit(0.U(log2Up(max_tma_inst).W))
//  val addr = Wire(Vec(numgroupinstmax,UInt(xLen.W)))
//  (0 until(numgroupinstmax)).foreach(x => { // assume the data stored in memory continuously
//    addr(x) := Mux(x < current_numgroup, reg_save.in2(0) + tma_aligned * x, 0.U(xLen.W))
//  })
//  val addr_wire=Wire(UInt(xLen.W))
//  addr_wire:=addr(PriorityEncoder(reg_save.mask.asUInt))
//  val current_tag = Mux(reg_save.mask.asUInt=/=0.U, addr_wire(xLen-1, xLen-1-l2cachetagbits+1), 0.U(l2cachetagbits.W))
//  val setIdx = Mux(reg_save.mask.asUInt=/=0.U, addr_wire(xLen-1-l2cachetagbits, xLen-1-l2cachetagbits-dcache_SetIdxBits+1), 0.U(dcache_SetIdxBits.W))
//  val blockOffset = Wire(Vec(numgroupinstmax, UInt(dcache_BlockOffsetBits.W)))
//  (0 until numgroupinstmax).foreach( x => blockOffset(x) := addr(x)(10, 2) )
//  val wordOffset1H = Wire(Vec(numgroupinstmax, UInt(BytesOfWord.W)))
////  (0 until numgroupinstmax).foreach( x => {
////    //DONE: Add Control Signals in vExeData.ctrl and define lw lh lb
////    wordOffset1H(x) := 15.U(4.W)
////    switch(reg_save.ctrl.mem_whb){
////      is(MEM_W) { wordOffset1H(x) := 15.U }
////      is(MEM_H) { wordOffset1H(x) :=
////        Mux(addr(x)(1)===0.U,
////          3.U,
////          12.U
////        )
////      }
////      is(MEM_B) { wordOffset1H(x) := 1.U << addr(x)(1,0) }
////    }
////  })
////  switch(reg_save.ctrl.mem_whb){
////    is(MEM_W){
////      srcsize :=  4.U
////    }
////    is(MEM_H){
////      srcsize :=  2.U
////    }
////    is(MEM_B){
////      srcsize := 1.U
////    }
////  }
//
//  io.to_mshr.bits.tag.mask := reg_save.mask
//  io.to_mshr.bits.tag.reg_idxw := reg_save.ctrl.reg_idxw
//  io.to_mshr.bits.tag.warp_id := reg_save.ctrl.wid
//  io.to_mshr.bits.tag.wxd:=reg_save.ctrl.wxd
//  io.to_mshr.bits.tag.wfd:=reg_save.ctrl.wvd
//  io.to_mshr.bits.tag.isvec := reg_save.ctrl.isvec
//  io.to_mshr.bits.tag.unsigned := reg_save.ctrl.mem_unsigned
//  io.to_mshr.bits.tag.wordOffset1H := wordOffset1H
//  io.to_mshr.valid := state===s_save & (reg_save.ctrl.mem_cmd.orR)
//  io.to_mshr.bits.tag.isWrite := 0.U
//  io.to_shared.bits.instrId := reg_entryID
//  // |reg_save| -> |addr & mask| -> |PriorityEncoder| -> |tag & idx| -> |io.to_dcache.bits|
//  //io.to_shared.bits.tag := tag
//  io.to_shared.bits.setIdx := setIdx
//  (0 until num_thread).foreach(x => {
//    io.to_shared.bits.perLaneAddr(x).blockOffset := blockOffset(x)
//    io.to_shared.bits.perLaneAddr(x).wordOffset1H := wordOffset1H(x)
//    io.to_shared.bits.perLaneAddr(x).activeMask := reg_save.mask(x) && (addr(x)(xLen-1, xLen-1-l2cachetagbits+1)===tag && addr(x)(xLen-1-l2cachetagbits, xLen-1-l2cachetagbits-dcache_SetIdxBits+1)===setIdx)
//  })
//  io.to_shared.bits.data := io.from_temp.bits.data
//  io.to_shared.bits.isWrite := reg_save.ctrl.mem_cmd(1)
//  io.to_shared.valid := state===s_shared
//  val mask_next = Wire(Vec(numgroupinstmax, Bool()))
//
//  (0 until numgroupinstmax).foreach( x => {                          // update mask
//    mask_next(x) := reg_save.mask(x) && !(addr(x)(xLen-1, xLen-1-l2cachetagbits+1)===tag && addr(x)(xLen-1-l2cachetagbits, xLen-1-l2cachetagbits-dcache_SetIdxBits+1)===setIdx
//      )})
//  // End of Addr Logic
//
//
//  io.from_temp.ready := state === s_idle
//
//  // FSM State Transfer
//  switch(state){
//    is (s_idle){
//      when(io.from_temp.valid){ state := s_save }.otherwise{ state := state }
//      cnt.reset()
//    }
//    is (s_save){
//      when(reg_save.ctrl.mem_cmd.orR){ // read or write
//        when(io.to_tempmem.fire){state := s_shared}.otherwise{state := s_save}
//      }.otherwise{ state := s_idle }
//    }
//    is (s_shared){
//      when(io.to_shared.fire){
//        when (cnt.value >= copynum || mask_next.asUInt===0.U){
//          cnt.reset(); state := s_idle
//        }.otherwise{
//          cnt.inc(); state := s_shared
//        }
//      }.otherwise{state := s_shared}
//    }
//  }
//  // FSM Operation
//  switch(state){
//    is (s_idle){
//      when(io.from_temp.fire){ // Next: s_save
//        reg_save := io.from_temp.bits
//        reg_save := io.from_temp.bits   // save data
//        (0 until numgroupinstmax).foreach(x => {
//          reg_save.mask := Mux(x < copynum, true, false)  //initialize the first copynum bit to 1
//        })
//      }.otherwise{reg_save := RegInit(0.U.asTypeOf(new vExeData))}
//    }
//    is (s_save){
//      when(reg_save.ctrl.mem_cmd.orR){//===1.U){  // read
//        when(io.to_tempmem.fire){reg_entryID := io.idx_entry}  // get entryID from MSHR
//      }
//    }
//    is (s_shared){
//      when(io.to_shared.fire){                                      // request is sent
//        reg_save.mask := mask_next
//      }.otherwise{
//        reg_save.mask := reg_save.mask
//      }
//    }
//  }
//
//
//
//}
//
////class MSHRv2TMA extends Module{
////  val io = IO(new Bundle{
////    val from_addr = Flipped(DecoupledIO(new Bundle{
////      val tag = Input(new MshrTag)
////    }))
////    val idx_entry = Output(UInt(log2Up(lsu_nMshrEntry).W))
////    val from_l2cache = Flipped(DecoupledIO(new TLBundleD_lite_plus(l2cache_params)))
////    val to_pipe = DecoupledIO(new MSHROutput)
////  })
////
////  val data = Mem(lsu_nMshrEntry, Vec(numgroupinstmax, UInt(xLen.W)))
////  val tag = Mem(lsu_nMshrEntry, UInt((io.from_addr.bits.tag.getWidth).W))
////  //val targetMask = RegInit(VecInit(Seq.fill(lsu_nMshrEntry)(0.U(num_thread.W))))
////  val currentMask = RegInit(VecInit(Seq.fill(lsu_nMshrEntry)(0.U(num_thread.W)))) // 0: complete
//////  val inv_activeMask = VecInit(io.from_l2cache.bits.activeMask.map(!_)).asUInt
////  val used = RegInit(0.U(lsu_nMshrEntry.W))
////  val complete = VecInit(currentMask.map{_===0.U}).asUInt & used
////  val output_entry = Mux(complete.orR, PriorityEncoder(complete), 0.U)
////  val valid_entry = Mux(used.andR, 0.U, PriorityEncoder(~used))
////  val reg_req = Reg(new MshrTag)
////
////  val s_idle :: s_add  :: s_out :: Nil = Enum(3)
////  val state = RegInit(s_idle)
////
////  io.from_l2cache.ready := state===s_idle// && used.orR
////  io.from_addr.ready := state===s_idle && !(used.andR)
////  io.idx_entry := Mux(io.from_addr.fire, valid_entry, 0.U) // return the MSHR entry
////
////  when(state===s_idle){
////    when(io.from_l2cache.fire){
////      when(io.from_addr.fire){
////        state := s_add
////      }.elsewhen(io.to_pipe.ready && currentMask(io.from_l2cache.bits.source(log2Up(max_tma_inst)-1.U,0))===io.from_l2cache.bits.activeMask.asUInt){
////        state := s_out
////      }.otherwise{state := s_idle}
////    }.elsewhen(complete.orR&&io.to_pipe.ready){
////      state := s_out
////    }.otherwise{
////      state := s_idle
////    }
////  }.elsewhen(state===s_out){
////    when(io.to_pipe.ready && complete.bitSet(valid_entry, false.B).orR){state:=s_out}.otherwise{state:=s_idle}
////  }.elsewhen(state===s_add){
////    when(io.to_pipe.ready && complete.orR){state:=s_out}.otherwise{state:=s_idle}
////  }.otherwise{state:=s_idle}
////
////  switch(state){
////    is(s_idle){
////      when(io.from_l2cache.fire){ // deal with update request immediately
////        data.write(io.from_l2cache.bits.instrId, io.from_l2cache.bits.data, io.from_l2cache.bits.activeMask) // data update
////        currentMask(io.from_l2cache.bits.instrId) := currentMask(io.from_l2cache.bits.instrId) & inv_activeMask // mask update
////        when(io.from_addr.fire){reg_req := io.from_addr.bits.tag} // both input valid: save the add request, and deal with it in the next cycle
////      }.elsewhen(io.from_addr.fire){// deal with add request immediately
////        used := used.bitSet(valid_entry, true.B) // set MSHR entry used
////        tag.write(valid_entry,
////          //Cat(io.from_addr.bits.tag.warp_id, io.from_addr.bits.tag.reg_idxw, io.from_addr.bits.tag.mask.asUInt)
////          io.from_addr.bits.tag.asUInt
////        )
////        data.write(valid_entry, VecInit(Seq.fill(num_thread)(0.U)))    // data initialize
////        currentMask(valid_entry) := io.from_addr.bits.tag.mask.asUInt()   // mask initialize
////      }
////    }
////    is(s_add){
////      used := used.bitSet(valid_entry, true.B)
////      tag.write(valid_entry,
////        //Cat(reg_req.warp_id, reg_req.reg_idxw, reg_req.mask.asUInt)
////        io.from_addr.bits.tag.asUInt
////      )
////      data.write(valid_entry, VecInit(Seq.fill(num_thread)(0.U)))
////      currentMask(valid_entry) := reg_req.mask.asUInt
////    }
////    is(s_out){ // release MSHR line
////      when(io.to_pipe.fire){used := used.bitSet(output_entry, false.B)}
////    }
////  }
////  val output_tag = tag.read(output_entry).asTypeOf(new MshrTag)
////  val raw_data = data.read(output_entry)
////  val output_data = Wire(Vec(num_thread, UInt(xLen.W)))
////  (0 until num_thread).foreach{ x =>
////    output_data(x) := Mux(output_tag.mask(x),
////      ByteExtract(output_tag.unsigned, raw_data(x), output_tag.wordOffset1H(x)),
////      0.U(xLen.W)
////    )
////  }
////  io.to_pipe.valid := complete.orR && state===s_out
////  io.to_pipe.bits.tag := output_tag.asTypeOf(new MshrTag)
////  io.to_pipe.bits.data := output_data
////}
//class AddrCalc_shared() extends Module{
//      val io = IO(new Bundle{
//        val from_fifo = Flipped(DecoupledIO(new vExeDataTMA))
//        val from_temp = Flipped(DecoupledIO(new SharedInput))
//        //    val csr_wid = Output(UInt(depth_warp.W))
//        //    val csr_pds = Input(UInt(xLen.W))
//        //    val csr_numw = Input(UInt(xLen.W))
//        //    val csr_tid = Input(UInt(xLen.W))
//        //    val to_mshr = DecoupledIO(new Bundle{
//        //      val tag = new MshrTag
//        //    })
//        val idx_entry = Input(UInt(log2Up(max_tma_inst).W))
//        //    val to_l2cache = DecoupledIO(new DCacheCoreReq_np)
//        val to_shared = DecoupledIO(new ShareMemCoreReq_np)
//      })
//      val s_idle :: s_save :: s_shared :: Nil = Enum(3)
//      val state = RegInit(init = s_idle)
//      val reg_save = Reg(new vExeDataTMA)
//      val current_numdata = reg_save.ctrl.imm_ext.asUInt /(xLen/8).U
//      //  val cnt = new Counter(n = num_thread)
//      val reg_entryID = RegInit(0.U(log2Up(max_tma_inst).W))
//      val addr = Wire(Vec(num_thread,UInt(xLen.W)))
//      (0 until(num_thread)).foreach(x => { // assume the data stored in memory continuously
//        addr(x) := Mux(x < current_numdata, reg_save.in2(0) + 4 * x, 0.U(xLen.W))
//      })
//      val addr_wire=Wire(UInt(xLen.W))
//      addr_wire:= addr(PriorityEncoder(reg_save.mask.asUInt))
//      val current_tag = Mux(reg_save.mask.asUInt=/=0.U, addr_wire(xLen-1, xLen-1-dcache_TagBits+1), 0.U(dcache_TagBits.W))
//      val current_setIdx = Mux(reg_save.mask.asUInt=/=0.U, addr_wire(xLen-1-dcache_TagBits, xLen-1-dcache_TagBits-dcache_SetIdxBits+1), 0.U(dcache_SetIdxBits.W))
//      val blockOffset = Wire(Vec(num_thread, UInt(dcache_BlockOffsetBits.W)))
//      (0 until num_thread).foreach( x => blockOffset(x) := addr(x)(10, 2) )
//      val wordOffset1H = Wire(Vec(num_thread, UInt(BytesOfWord.W)))
//
//      //  io.to_mshr.bits.tag.mask := reg_save.mask
//      //  io.to_mshr.bits.tag.reg_idxw := reg_save.ctrl.reg_idxw
//      //  io.to_mshr.bits.tag.warp_id := reg_save.ctrl.wid
//      //  io.to_mshr.bits.tag.wxd:=reg_save.ctrl.wxd
//      //  io.to_mshr.bits.tag.wfd:=reg_save.ctrl.wvd
//      //  io.to_mshr.bits.tag.isvec := reg_save.ctrl.isvec
//      //  io.to_mshr.bits.tag.unsigned := reg_save.ctrl.mem_unsigned
//      //  io.to_mshr.bits.tag.wordOffset1H := wordOffset1H
//      //  io.to_mshr.valid := state===s_save & (reg_save.ctrl.mem_cmd.orR)
//      //  io.to_mshr.bits.tag.isWrite := 0.U
//      io.to_shared.bits.instrId := reg_entryID
//      // |reg_save| -> |addr & mask| -> |PriorityEncoder| -> |tag & idx| -> |io.to_dcache.bits|
//      //io.to_shared.bits.tag := tag
//      // 128bytes
//      io.to_shared.bits.setIdx := current_setIdx
//      (0 until num_thread).foreach(x => {
//        io.to_shared.bits.perLaneAddr(x).blockOffset := blockOffset(x)
//        io.to_shared.bits.perLaneAddr(x).wordOffset1H := wordOffset1H(x)
//        io.to_shared.bits.perLaneAddr(x).activeMask := reg_save.mask(x) && (addr(x)(xLen-1, xLen-1-dcache_TagBits+1)===current_tag && addr(x)(xLen-1-dcache_TagBits, xLen-1-dcache_TagBits-dcache_SetIdxBits+1)===current_setIdx)
//      })
//      io.to_shared.bits.data := io.from_temp.bits.data
//      io.to_shared.bits.isWrite := 1.U
//      io.to_shared.valid := state===s_shared
//      val mask_next = Wire(Vec(num_thread, Bool()))
//
//      (0 until num_thread).foreach( x => {                          // update mask
//        mask_next(x) := reg_save.mask(x) && !(addr(x)(xLen-1, xLen-1-dcache_TagBits+1)===current_tag && addr(x)(xLen-1-dcache_TagBits, xLen-1-dcache_TagBits-dcache_SetIdxBits+1)===current_setIdx
//          )})
//      // End of Addr Logic
//
//
//      io.from_fifo.ready := state === s_idle
//
//      // FSM State Transfer
//      switch(state){
//        is (s_idle){
//          when(io.from_fifo.valid){ state := s_save }.otherwise{ state := state }
//          cnt.reset()
//        }
//        is (s_save){
//          //      when(reg_save.ctrl.mem_cmd.orR){ // read or write
//          when(io.to_shared.fire){state := s_shared}.otherwise{state := s_save}
//          //      }.otherwise{ state := s_idle }
//        }
//        is (s_shared){
//          when(io.to_shared.fire){
//            when (cnt.value >= current_numdata || mask_next.asUInt===0.U){
//              cnt.reset(); state := s_idle
//            }.otherwise{
//              cnt.inc(); state := s_shared
//            }
//          }.otherwise{state := s_shared}
//        }
//      }
//      // FSM Operation
//      switch(state){
//        is (s_idle){
//          when(io.from_fifo.fire){ // Next: s_save
//            //        reg_save := io.from_fifo.bits
//            instructmem.write()
//            (0 until num_thread).foreach(x => {
//              reg_save.mask := Mux(x < current_numdata, true, false)  //initialize the first copynum bit to 1
//            })
//          }.otherwise{reg_save := RegInit(0.U.asTypeOf(new vExeData))}
//        }
//        is (s_save){
//          when(reg_save.ctrl.mem_cmd.orR){//===1.U){  // read
//            when(io.from_temp.fire){reg_entryID := io.idx_entry}  // get entryID from MSHR
//          }
//        }
//        is (s_shared){
//          when(io.to_shared.fire){                                      // request is sent
//            reg_save.mask := mask_next
//          }.otherwise{
//            reg_save.mask := reg_save.mask
//          }
//        }
//      }
//
//
//
//  }
//class TMA_top() extends Module{
//  val io = IO(new Bundle{
//    //input
//    val tma_req = Flipped(DecoupledIO(new vExeData()))
//    /*usage:
//    vExeData.in1: Dcache base addr
//    vExeData.in2: Sharedmem base addr
//    vExeData.ctrl.imm_ext: copysize
//                           maybe 4*4*2 byte one cp.async
//    vExeData.ctrl.memw/h/b: data type : determine the mask sent to L1 cache
//    */
//    val l2cache_rsp = Flipped(DecoupledIO(new TLBundleD_lite_plus(l2cache_params)))
//    val shared_rsp = Flipped(DecoupledIO(new ShareMemCoreRsp_np()))
//    //output
//    val dcache_req = Decoupled( new TLBundleA_lite(l2cache_params))
//    val shared_req = DecoupledIO(new ShareMemCoreReq_np())
//    val fence_end_dcache = Output(UInt(num_warp.W)) //todo pass to barrier to fence
////    val fence_end_shared = Output(UInt(num_warp.W)) //todo numwarp pass to barrier to fence
//    // about csr, maybe no use
////    val csr_wid = Output(UInt(depth_warp.W))
////    val csr_pds = Input(UInt(xLen.W))
////    val csr_numw = Input(UInt(xLen.W))
////    val csr_tid = Input(UInt(xLen.W))
//  })
//  /*
//  * design: like lsu:
//  * 1. InputFIFO: collect outer req, and send it to AddrCalculator
//  * 2. AddrCalc_l2cache: use in1 ,copysize and srcsize to Calculate the addr in L1cache
//  * 3. AddrCalc_shared: use in2 ,copysize and srcsize to Calculate the addr in Sharedmem
//  * 4. MSHR_dcache: accept tag from AddrCalc_fetch and data from Arbiter. send req to L1cache
//  * 5. MSHR_shared: accept tag from AddrCalc_store and data from Arbiter, sent req to Sharedmem
//  * 5. ShiftBoard_dcache: shiftright or shiftleft based on dcache_req.fire and dcache_rsp.fire. if all warps' req are responsed, set the fence_end(i) 1 accordingly
//  *                define in LSU.scala
//  * 6. Temp_Data: store instr info and Dcache data accordingly
//  *
//  * */
//  val tma_req_transfer = new vExeData
//  val InputFIFO = Module(new Queue(new vExeData, entries = 1, pipe = true))
//  val AddrCalc_l2cache = Module(new AddrCalc_l2cache())
//  val AddrCalc_shared = Module(new AddrCalc_shared())
//  val MSHR_dcache = Module(new MSHRv2TMA)
//  val MSHR_shared = Module(new MSHRv2TMA)
//  val ShiftBoard_dcache = VecInit(Seq.fill(num_warp)(Module(new ShiftBoard(lsu_num_entry_each_warp)).io))
////  val ShiftBoard_shared = VecInit(Seq.fill(num_warp)(Module(new ShiftBoard(lsu_num_entry_each_warp)).io))
//  val InstStore = Mem(num_warp, new vExeData())
//  val TempData = RegInit(VecInit(Seq.fill(maxcopysize)(0.U(xLen.W))))
//
//  //  //InputFIFO's io
//  //  InputFIFO.io.enq <> io.tma_req
//  //  InputFIFO.io.enq.bits.src_global := io.tma_req.bits.in1(0)
//  //  InputFIFO.io.enq.bits.dst_shared := io.tma_req.bits.in2(0)
//  //  InputFIFO.io.enq.bits.in3 := io.tma_req.bits.in3(0) // no use
//  //  InputFIFO.io.enq.bits.copysize := io.tma_req.bits.ctrl.imm_ext(0)
//  ////  InputFIFO.io.enq.bits.srcsize := io.tma_req.bits.ctrl.sel_imm(0)
//  //  io.tma_req.ready := InputFIFO.io.enq.ready(0)
//
//  InputFIFO.io.enq.valid := Mux(ShiftBoard_dcache(io.tma_req.bits.ctrl.wid).full,false.B,io.tma_req.valid)
//
//  //AddrCalc_l2cache AddrCalc_shared
//  AddrCalc_l2cache.io.from_fifo <> InputFIFO.io.deq
//  io.dcache_req <> AddrCalc_l2cache.io.to_l2cache
//  AddrCalc_shared.io.from_temp = TempData.read()
//  io.shared_req <> AddrCalc_shared.io.to_shared
//
//  //MSHR_dcache MSHR_shared
//    MSHR_dcache.io.from_l2cache <> io.l2cache_rsp
//    MSHR_dcache.io.from_addr <> AddrCalc_l2cache.io.to_tempmem
//    AddrCalc_l2cache.io.idx_entry := MSHR_dcache.io.idx_entry
//    io.shared_req <> MSHR_dcache.io.mshr2shared
//  //
//    MSHR_shared.io.from_l2cache <> io.shared_rsp
//    MSHR_shared.io.from_addr <> AddrCalc_shared.io.to_tempmem
//    AddrCalc_shared.io.idx_entry := MSHR_shared.io.idx_entry
//    io.shared_req <> MSHR_shared.io.mshr2shared
//
//  //shiftboard
//    ShiftBoard_dcache.zipWithIndex.foreach{case(a,b)=> {
//    a.left:=io.tma_req.fire & io.tma_req.bits.ctrl.wid===b.asUInt
//    a.right:=io.l2cache_rsp.fire & io.l2cache_rsp.bits.tag.warp_id===b.asUInt
//  }}
////  ShiftBoard_shared.zipWithIndex.foreach{case(a,b)=> {
////    a.left:=io.shared_req.fire & io.shared_req.bits.ctrl.wid===b.asUInt
////    a.right:=io.shared_rsp.fire & io.shared_rsp.bits.tag.warp_id===b.asUInt
////  }}
//
//  io.fence_end_dcache:=VecInit(ShiftBoard_dcache.map(x=>x.empty)).asUInt
////  io.fence_end_shared:=VecInit(ShiftBoard_shared.map(x=>x.empty)).asUInt
//
//  io.tma_req.ready:=Mux(ShiftBoard_dcache(io.tma_req.bits.ctrl.wid).full,false.B,InputFIFO.io.enq.ready)
//
////  io.csr_wid:=AddrCalcTMA.io.csr_wid
////  AddrCalcTMA.io.csr_tid:=io.csr_tid
////  AddrCalcTMA.io.csr_pds:=io.csr_pds
////  AddrCalcTMA.io.csr_numw:=io.csr_numw
//}