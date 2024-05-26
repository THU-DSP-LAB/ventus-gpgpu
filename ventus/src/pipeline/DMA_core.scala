package pipeline

import top.parameters._
import chisel3._
import chisel3.util.{log2Ceil, _}
import mmu.SV32.asidLen
import mmu.SV32.paLen
//import L2cache.{InclusiveCacheParameters_lite, TLBundleA_lite, TLBundleD_lite, TLBundleD_lite_plus}
import top.parameters._
import chisel3._
import chisel3.util._
import IDecode._
import L1Cache.{DCacheMemReq_p, DCacheMemRsp}
import config.config.Parameters
import mmu.SV32.asidLen
//def log2Floor_dma(num: UInt): UInt = {
////  require(num > 0.U, "Input to log2Floor must be greater than 0")
//
//  // Count the number of leading zeros
//  val leadingZeros = PriorityEncoder(Reverse(num))
//
//  // Subtract leadingZeros from the bit width of the number
//  (num.getWidth - 1).U - leadingZeros
//}

class regSave extends Bundle{
  val in1 = Vec(num_thread, UInt(xLen.W))
  val in2 = Vec(num_thread, UInt(xLen.W))
  val in3 = Vec(num_thread, UInt(xLen.W))
  val address=UInt(xLen.W)
  val ctrl=new CtrlSigs()
}
class vExeDataDMA extends Bundle {
  //  val mode = Bool()
  //  val used = Bool()
  //  val complete = Bool()
  val src = UInt(xLen.W)  // src
  val dst = UInt(xLen.W)  //dst
  val funct = UInt(4.W)
  val copysize = UInt(xLen.W)  // only 4 8 16 , may add 32
  val srcsize = UInt(xLen.W)  // only 4 8 16 , may add 32
  val wid = UInt(depth_warp.W)
  val tensorvars = new TensorVars
  //  val ctrl = new CtrlSigs()
}
class cacheline_info extends Bundle{
  val tag = UInt(xLen.W)
  val tensor_dim_step = Vec(5, UInt(xLen.W))
  val box_dim0_start = UInt(xLen.W)
  val tensor_dim0_start = UInt(xLen.W)
}


//class l2cache_transform(params: InclusiveCacheParameters_lite) extends Bundle
//{
//  val opcode=UInt(params.op_bits.W)
//  val size=UInt(params.size_bits.W)
//  val source=UInt(params.source_bits.W)
//  val data  = Vec(numgroupl2cache, UInt((dma_aligned_bulk * 8).W))
//  val param =UInt(3.W)
//}
class l2cacheline_info(implicit p: Parameters) extends Bundle
{
  val base = new DCacheMemRsp
  val cacheline_info = new cacheline_info
}
class TempOutput extends Bundle{
  val entry_index = UInt(log2Ceil(max_dma_inst).W)
  val mask = Vec(numgroupshared, Bool())
  val data = Vec(numgroupshared, UInt((dma_aligned_bulk * BitsOfByte).W))
  val cacheline_info = new cacheline_info
  val instinfo = new vExeDataDMA()
}

//class l2cache2temp extends Module {
//  val io = IO(new Bundle{
//    val from_l2cache = Flipped(DecoupledIO(new TLBundleD_lite(l2cache_params)))
//    val l22temp = DecoupledIO(new l2cache_transform(l2cache_params))
//  })
//  io.l22temp.valid := io.from_l2cache.valid
//  io.from_l2cache.ready := io.l22temp.ready
//  (0 until(numgroupl2cache)).foreach( x=> {
//    io.l22temp.bits.data(x) := io.from_l2cache.bits.data((x + 1) * (dma_aligned_bulk * 8) - 1, x * (dma_aligned_bulk * 8))
//  })
//  io.l22temp.bits.source := io.from_l2cache.bits.source
//  io.l22temp.bits.param := io.from_l2cache.bits.param
//  io.l22temp.bits.size := io.from_l2cache.bits.size
//  io.l22temp.bits.opcode := io.from_l2cache.bits.opcode)
//}
class TensorVars extends Bundle{
  // from vrs2 boxsize
  val BoxAddress = UInt(xLen.W)
  val boxDim = Vec(5, UInt(xLen.W))
  //  val boxDim1 = UInt(xLen.W)
  //  val boxDim2 = UInt(xLen.W)
  //  val boxDim3 = UInt(xLen.W)
  //  val boxDim4 = UInt(xLen.W)
  //  val boxDim5 = UInt(xLen.W)
  val elementStrides  = Vec(5,UInt(xLen.W))
  //  val elementStrides1 = UInt(xLen.W)
  //  val elementStrides2 = UInt(xLen.W)
  //  val elementStrides3 = UInt(xLen.W)
  //  val elementStrides4 = UInt(xLen.W)
  //  val elementStrides5 = UInt(xLen.W)
  val interleaveMode = UInt(log2Ceil(3).W)
  val swizzleMode = UInt(log2Ceil(4).W)
  val L2promotion = UInt(log2Ceil(4).W) // no use now
  val oobfill = UInt(log2Ceil(2).W)    // 1 fill nan, 0 fill zero

  //from vrs1 l2cache tensor
  val datawidth = UInt(3.W)
  val dataType = UInt(log2Ceil(13).W)
  val tensorRank = UInt(log2Ceil(5).W)
  val globalAddress = UInt(xLen.W)
  val globalDim = Vec(5, UInt(xLen.W))
  val globalStrides = Vec(5, UInt(xLen.W))
  //  val globalDim1 = UInt(xLen.W)
  //  val globalDim2 = UInt(xLen.W)
  //  val globalDim3 = UInt(xLen.W)
  //  val globalDim4 = UInt(xLen.W)
  //  val globalDim5 = UInt(xLen.W)
  //  val globalStrides1 = UInt(xLen.W)
  //  val globalStrides2 = UInt(xLen.W)
  //  val globalStrides3 = UInt(xLen.W)
  //  val globalStrides4 = UInt(xLen.W)
  //  val globalStrides5 = UInt(xLen.W)

  //from rs3 sharedmem
  //  val sharedAddress = UInt(xLen.W)

  // generated
  //  val copysize = UInt(xLen.W)
}
//class sharedRspRouter extends Module {
//  val io = IO(new Bundle {
//    val in = Flipped(DecoupledIO(new DCacheCoreRsp_np))
//    val outDMA = DecoupledIO(new DCacheCoreRsp_np)
//    val outLSU = DecoupledIO(new DCacheCoreRsp_np)
//  })
//  val is_dma = RegInit(false.B)
//  val RSPFIFO = Module(new Queue(new DCacheCoreRsp_np, entries=1, pipe=true))
//  RSPFIFO.io.enq <> io.in
//  io.outDMA.valid := RSPFIFO.io.deq.valid //&& is_dma
//  io.outLSU.valid := RSPFIFO.io.deq.valid //&& !is_dma
////  RSPFIFO.io.deq.ready := Mux(is_dma, io.outDMA.ready, io.outLSU.ready)
//  RSPFIFO.io.deq.ready := io.outDMA.ready
//  io.outDMA.bits := io.in.bits
//  io.outLSU.bits := io.in.bits
//  when(io.in.fire){
//    is_dma := io.in.bits.dma
//  }.otherwise{
//    is_dma := is_dma
//  }
//}
class AddrCalc_l2cache() extends Module{
  val io = IO(new Bundle{
    val from_fifo = Flipped(DecoupledIO(new vExeData))
    val to_tempmem_inst = DecoupledIO(
      //      new Bundle{
      //      val tag = new vExeDataDMA
      new vExeDataDMA
    )
    //    val to_tempmem_tag = DecoupledIO(UInt(addr_tag_bits.W))
    val to_tempmem_tag = DecoupledIO(new cacheline_info)
    val inst_mem_index = Input(UInt(log2Ceil(max_dma_inst).W))
    val tag_mem_index = Input(UInt(log2Ceil(max_dma_tag).W))
    val to_l2cache = DecoupledIO(new DCacheMemReq_p)

    //tlb
    val to_l2TLB = Decoupled(new DmaTLBReq)
    val from_l2TLB = Flipped(Decoupled(UInt(paLen.W)))
  })
  val s_idle :: s_save :: s_l2cache_tag :: s_tlb_req :: s_tlb_rsp :: s_l2cache :: Nil = Enum(6)
  val state = RegInit(init = s_idle)
  val reg_save = Reg(new regSave)
  //  val current_numgroup = io.from_fifo.bits.in2(0).asUInt /dma_aligned_bulk.asUInt
  //  val cnt = new Counter(n = numgroupinsdmax)
  val inst_mem_index_reg = RegInit(0.U(log2Ceil(max_dma_inst).W))
  val tag_mem_index_reg  = RegInit(0.U(log2Ceil(max_dma_tag).W))

  //tlb
  val p_addr_reg = RegInit(0.U(paLen.W))


//  val copysize = Wire(UInt(log2Ceil(16).W))
//  copysize := 4.U
//  switch(reg_save.ctrl.copysize){
//    //    copysize := 4.U
//    is(0.U){
//      copysize := 4.U
//    }
//    is(1.U){
//      copysize := 8.U
//    }
//    is(2.U){
//      copysize := 16.U
//    }
//    is(3.U){
//      copysize := 32.U //todo may not good
//    }
//  }
  val TensorVars = Wire(new TensorVars)
  TensorVars.BoxAddress := reg_save.in2(0)
  (0 until (5)).foreach(x => {
    TensorVars.boxDim(x) := reg_save.in2(1 + x)
    TensorVars.elementStrides(x) := reg_save.in2(6 + x)
  })
  TensorVars.interleaveMode := reg_save.in2(11)(log2Ceil(3) - 1, 0)
  TensorVars.swizzleMode := reg_save.in2(12)(log2Ceil(4) - 1, 0)
  TensorVars.L2promotion := reg_save.in2(13)(log2Ceil(4) - 1, 0)
  TensorVars.oobfill := reg_save.in2(14)(log2Ceil(2) - 1, 0)
  TensorVars.dataType := reg_save.in1(0)(log2Ceil(13) - 1, 0)
  TensorVars.tensorRank := reg_save.in1(1)(log2Ceil(5) - 1, 0)
  TensorVars.globalAddress := reg_save.in1(2)
  (0 until (5)).foreach(x => {
    TensorVars.globalDim(x) := reg_save.in1(3 + x)
    TensorVars.globalStrides(x) := reg_save.in1(8 + x)
  })
  TensorVars.datawidth := Mux((reg_save.in1(0).asUInt === UINT8.asUInt), 1.U,
    Mux((reg_save.in1(0).asUInt === UINT16.asUInt) || (reg_save.in1(0).asUInt === FLOAT16.asUInt) || (reg_save.in1(0).asUInt === BFLOAT16.asUInt), 2.U,
      Mux((reg_save.in1(0).asUInt === UINT32.asUInt) || (reg_save.in1(0).asUInt === INT32.asUInt) || (reg_save.in1(0).asUInt === FLOAT32.asUInt) || (reg_save.in1(0).asUInt === FLOAT32_FTZ.asUInt) || (reg_save.in1(0).asUInt === TFLOAT32.asUInt) || (reg_save.in1(0).asUInt === TFLOAT32_FTZ.asUInt), 4.U, 8.U)))
  val box_elements_num = Wire(Vec(5, UInt(xLen.W)))
  (0 until(5)).foreach( x=> {
    box_elements_num(x) := ((TensorVars.boxDim(x) + TensorVars.elementStrides(x) - 1.U) / TensorVars.elementStrides(x)).asUInt
  })
  val Tensorcopysize = Wire(UInt(xLen.W))
  Tensorcopysize := 0.U
  switch(TensorVars.tensorRank) {
    is(1.U) {
      Tensorcopysize := TensorVars.boxDim(0) * TensorVars.datawidth
    }
    is(2.U) {
      Tensorcopysize := TensorVars.boxDim(0) * TensorVars.datawidth *
        box_elements_num(1).asUInt
    }
    is(3.U) {
      Tensorcopysize := TensorVars.boxDim(0) * TensorVars.datawidth *
        box_elements_num(1).asUInt *
        box_elements_num(2).asUInt
    }
    is(4.U) {
      Tensorcopysize := TensorVars.boxDim(0) * TensorVars.datawidth *
        box_elements_num(1).asUInt *
        box_elements_num(2).asUInt *
        box_elements_num(3).asUInt
    }
    is(5.U) {
      Tensorcopysize := TensorVars.boxDim(0) * TensorVars.datawidth *
        box_elements_num(1).asUInt *
        box_elements_num(2).asUInt *
        box_elements_num(3).asUInt *
        box_elements_num(4).asUInt
    }
  }
  //  val output_data.cacheline_info.tensor_dim0_start = RegInit(VecInit(Seq.fill(5)(0.U((xLen).W)))) // save the dim start, in order to add the strides in tensor
  val tensor_dim_step_reg = RegInit(VecInit(Seq.fill(5)(0.U(xLen.W)))) // index 0 mean how many boxline has been covered, in a array
  val tensor_dim_step_next = Wire(Vec(5, UInt(xLen.W))) // index 0 mean how many boxline has been covered, in a array
  (0 until(5)).foreach( x=> {
    tensor_dim_step_next(x) := tensor_dim_step_reg(x)
  })
  val tensor_dim0_start = Wire(UInt(xLen.W))
  tensor_dim0_start := 0.U
  switch(TensorVars.tensorRank) {
    is(1.U) {
      tensor_dim0_start := TensorVars.globalAddress
    }
    is(2.U) {
      tensor_dim0_start := TensorVars.globalAddress +
        (tensor_dim_step_reg(1).asUInt * TensorVars.globalStrides(0).asUInt * TensorVars.elementStrides(1).asUInt)
    }
    is(3.U) {
      tensor_dim0_start := TensorVars.globalAddress +
        (tensor_dim_step_reg(1).asUInt * TensorVars.globalStrides(0).asUInt * TensorVars.elementStrides(1).asUInt +
          tensor_dim_step_reg(2).asUInt * TensorVars.globalStrides(1).asUInt * TensorVars.elementStrides(2).asUInt)
    }
    is(4.U) {
      tensor_dim0_start := TensorVars.globalAddress + (tensor_dim_step_reg(1).asUInt * TensorVars.globalStrides(0).asUInt * TensorVars.elementStrides(1).asUInt +
        tensor_dim_step_reg(2).asUInt * TensorVars.globalStrides(1).asUInt * TensorVars.elementStrides(2).asUInt +
        tensor_dim_step_reg(3).asUInt * TensorVars.globalStrides(2).asUInt * TensorVars.elementStrides(3).asUInt)

    }
    is(5.U) {
      tensor_dim0_start := TensorVars.globalAddress +
        (tensor_dim_step_reg(1).asUInt * TensorVars.globalStrides(0).asUInt * TensorVars.elementStrides(1).asUInt +
          tensor_dim_step_reg(2).asUInt * TensorVars.globalStrides(1).asUInt * TensorVars.elementStrides(2).asUInt +
          tensor_dim_step_reg(3).asUInt * TensorVars.globalStrides(2).asUInt * TensorVars.elementStrides(3).asUInt +
          tensor_dim_step_reg(4).asUInt * TensorVars.globalStrides(3).asUInt * TensorVars.elementStrides(4).asUInt)

    }
  }
  //  val box_dim0_start_reg = RegInit(TensorVars.BoxAddress.asUInt)
  val box_dim0_start = Wire(UInt(xLen.W))
  box_dim0_start := 0.U
  switch(TensorVars.tensorRank) {
    is(1.U) {
      box_dim0_start := TensorVars.BoxAddress
    }
    is(2.U) {
      box_dim0_start := TensorVars.BoxAddress +
        (tensor_dim_step_reg(1).asUInt * TensorVars.globalStrides(0).asUInt * TensorVars.elementStrides(1).asUInt)
    }
    is(3.U) {
      box_dim0_start := TensorVars.BoxAddress +
        (tensor_dim_step_reg(1).asUInt * TensorVars.globalStrides(0).asUInt * TensorVars.elementStrides(1).asUInt+
          tensor_dim_step_reg(2).asUInt * TensorVars.globalStrides(1).asUInt * TensorVars.elementStrides(2).asUInt)
    }
    is(4.U) {
      box_dim0_start := TensorVars.BoxAddress +
        (tensor_dim_step_reg(1).asUInt * TensorVars.globalStrides(0).asUInt * TensorVars.elementStrides(1).asUInt +
          tensor_dim_step_reg(2).asUInt * TensorVars.globalStrides(1).asUInt * TensorVars.elementStrides(2).asUInt +
          tensor_dim_step_reg(3).asUInt * TensorVars.globalStrides(2).asUInt * TensorVars.elementStrides(3).asUInt )
    }
    is(5.U) {
      box_dim0_start := TensorVars.BoxAddress +
        (tensor_dim_step_reg(1).asUInt * TensorVars.globalStrides(0).asUInt * TensorVars.elementStrides(1).asUInt +
          tensor_dim_step_reg(2).asUInt * TensorVars.globalStrides(1).asUInt * TensorVars.elementStrides(2).asUInt +
          tensor_dim_step_reg(3).asUInt * TensorVars.globalStrides(2).asUInt * TensorVars.elementStrides(3).asUInt +
          tensor_dim_step_reg(4).asUInt * TensorVars.globalStrides(3).asUInt * TensorVars.elementStrides(4).asUInt )
    }
  }
  //  val tensor_boxdim_length = Wire(Vec(5, UInt(xLen.W)))
  //  (0 until (5)).foreach(x => {
  //    tensor_boxdim_length(x) := TensorVars.boxDim(x) * TensorVars.datawidth
  //  })
  //  val dim0_num_cacheline = Wire(UInt(xLen.W))
  //  dim0_num_cacheline := Mux(tensor_boxdim_length(0)(log2Ceil(l2cacheline) - 1, 0).asUInt === 0.U,
  //    (tensor_boxdim_length(0) >> log2Ceil(l2cacheline)).asUInt,
  //    (tensor_boxdim_length(0) >> log2Ceil(l2cacheline)).asUInt + 1.U)

  val address_next = Wire(UInt(xLen.W))
  address_next := reg_save.address.asUInt + l2cacheline.asUInt
//  printf(p"reg_save.address : 0x${Hexadecimal(reg_save.address.asUInt)} \n")
  switch(reg_save.ctrl.funct) {
    is(0.U) {
      address_next := reg_save.address.asUInt + l2cacheline.asUInt
    }
    is(2.U) {
      address_next := reg_save.address.asUInt + l2cacheline.asUInt
    }
    is(3.U) {
      when((reg_save.address + l2cacheline.U).asUInt <= box_dim0_start.asUInt + TensorVars.boxDim(0).asUInt * TensorVars.datawidth.asUInt) {
        address_next := reg_save.address.asUInt + l2cacheline.asUInt
      }.otherwise {
        switch(TensorVars.tensorRank) {
          is(1.U) {
            address_next := reg_save.address.asUInt + l2cacheline.asUInt
          }
          is(2.U) {
            address_next := Cat((TensorVars.BoxAddress +  (tensor_dim_step_reg(1) + 1.U) * TensorVars.globalStrides(0) * TensorVars.elementStrides(1))(xLen-1, xLen-1-addr_tag_bits +1),0.U((xLen - addr_tag_bits).W))
            tensor_dim_step_next(1) := tensor_dim_step_reg(1) + 1.U
          }
          is(3.U) {
            when(tensor_dim_step_reg(1).asUInt === box_elements_num(1).asUInt - 1.U) {
              address_next := Cat((TensorVars.BoxAddress +
                (tensor_dim_step_reg(2) + 1.U) * TensorVars.globalStrides(1) * TensorVars.elementStrides(2))(xLen - 1, xLen - 1 - addr_tag_bits + 1),
                0.U((xLen - addr_tag_bits).W))
              tensor_dim_step_next(1) := 0.U
              tensor_dim_step_next(2) := tensor_dim_step_reg(2) + 1.U
            }.otherwise {
              address_next := Cat((TensorVars.BoxAddress +
                tensor_dim_step_reg(2) * TensorVars.globalStrides(1) * TensorVars.elementStrides(2) +
                (tensor_dim_step_reg(1) + 1.U) * TensorVars.globalStrides(0) * TensorVars.elementStrides(1))(xLen - 1, xLen - 1 - addr_tag_bits + 1),
                0.U((xLen - addr_tag_bits).W))
              tensor_dim_step_next(1) := tensor_dim_step_reg(1) + 1.U
            }

          }
          is(4.U) {
            var dim1_full = tensor_dim_step_reg(1).asUInt === box_elements_num(1).asUInt - 1.U
            var dim2_full = tensor_dim_step_reg(2).asUInt === box_elements_num(2).asUInt - 1.U
            when(dim1_full && dim2_full) {
              address_next := Cat((TensorVars.BoxAddress +
                (tensor_dim_step_reg(3) + 1.U) * TensorVars.globalStrides(2) * TensorVars.elementStrides(3))(xLen-1, xLen-1-addr_tag_bits +1),
                0.U((xLen - addr_tag_bits).W))
              tensor_dim_step_next(1) := 0.U
              tensor_dim_step_next(2) := 0.U
              tensor_dim_step_next(3) := tensor_dim_step_reg(3) + 1.U
            }.elsewhen(dim1_full) {
              address_next := Cat((TensorVars.BoxAddress +
                tensor_dim_step_reg(3) * TensorVars.globalStrides(2) * TensorVars.elementStrides(3) +
                (tensor_dim_step_reg(2) + 1.U) * TensorVars.globalStrides(1) * TensorVars.elementStrides(2))(xLen-1, xLen-1-addr_tag_bits +1),
                0.U((xLen - addr_tag_bits).W))
              tensor_dim_step_next(1) := 0.U
              tensor_dim_step_next(2) := tensor_dim_step_reg(2) + 1.U
            }.otherwise {
              address_next := Cat((TensorVars.BoxAddress +
                tensor_dim_step_reg(3) * TensorVars.globalStrides(2) * TensorVars.elementStrides(3) +
                tensor_dim_step_reg(2) * TensorVars.globalStrides(1) * TensorVars.elementStrides(2) +
                (tensor_dim_step_reg(1) + 1.U) * TensorVars.globalStrides(0) * TensorVars.elementStrides(1))(xLen - 1, xLen - 1 - addr_tag_bits + 1),
                0.U((xLen - addr_tag_bits).W))
              tensor_dim_step_next(1) := tensor_dim_step_reg(1) + 1.U
            }
          }
          is(5.U) {
            var dim1_full = tensor_dim_step_reg(1).asUInt === box_elements_num(1).asUInt - 1.U
            var dim2_full = tensor_dim_step_reg(2).asUInt === box_elements_num(2).asUInt - 1.U
            var dim3_full = tensor_dim_step_reg(3).asUInt === box_elements_num(3).asUInt - 1.U
            //            var dim4_full = tensor_dim_step_reg(3).asUInt === box_elements_num(4).asUInt - 1.U
            when(dim1_full && dim2_full && dim3_full) {
              address_next := Cat((TensorVars.BoxAddress
                +  (tensor_dim_step_reg(4) + 1.U) * TensorVars.globalStrides(3) * TensorVars.elementStrides(4))(xLen-1, xLen-1-addr_tag_bits +1),
                0.U((xLen - addr_tag_bits).W))
              tensor_dim_step_next(1) := 0.U
              tensor_dim_step_next(2) := 0.U
              tensor_dim_step_next(3) := 0.U
              tensor_dim_step_next(4) := tensor_dim_step_reg(4) + 1.U
            }.elsewhen(dim1_full && dim2_full) {
              address_next := Cat((TensorVars.BoxAddress +
                tensor_dim_step_reg(4) * TensorVars.globalStrides(3) * TensorVars.elementStrides(4) +
                (tensor_dim_step_reg(3) + 1.U) * TensorVars.globalStrides(2) * TensorVars.elementStrides(3))(xLen-1, xLen-1-addr_tag_bits +1),
                0.U((xLen - addr_tag_bits).W))
              tensor_dim_step_next(1) := 0.U
              tensor_dim_step_next(2) := 0.U
              tensor_dim_step_next(3) := tensor_dim_step_reg(3) + 1.U
            }.elsewhen(dim1_full) {
              address_next := Cat((TensorVars.BoxAddress +
                tensor_dim_step_reg(4) * TensorVars.globalStrides(3) * TensorVars.elementStrides(4) +
                tensor_dim_step_reg(3) * TensorVars.globalStrides(2) * TensorVars.elementStrides(3) +
                (tensor_dim_step_reg(2) + 1.U) * TensorVars.globalStrides(1) * TensorVars.elementStrides(2))(xLen-1, xLen-1-addr_tag_bits +1),
                0.U((xLen - addr_tag_bits).W))
              tensor_dim_step_next(1) := 0.U
              tensor_dim_step_next(2) := tensor_dim_step_reg(2) + 1.U
            }.otherwise {
              address_next := Cat((TensorVars.BoxAddress +
                tensor_dim_step_reg(4) * TensorVars.globalStrides(3) * TensorVars.elementStrides(4) +
                tensor_dim_step_reg(3) * TensorVars.globalStrides(2) * TensorVars.elementStrides(3) +
                tensor_dim_step_reg(2) * TensorVars.globalStrides(1) * TensorVars.elementStrides(2) +
                (tensor_dim_step_reg(1) + 1.U) * TensorVars.globalStrides(0) * TensorVars.elementStrides(1))(xLen - 1, xLen - 1 - addr_tag_bits + 1),
                0.U((xLen - addr_tag_bits).W))
              tensor_dim_step_next(1) := tensor_dim_step_reg(1) + 1.U
            }
          }
        }
      }
    }
  }
  val next_cacheline = Wire(UInt(xLen.W))
  next_cacheline := Cat(reg_save.address(xLen - 1, xLen - 1 - addr_tag_bits + 1), 0.U((xLen - addr_tag_bits).W)) + l2cacheline.asUInt
  val complete_address = Wire(Bool())
  //  complete_address := address_next  >= Cat((reg_save.in1(0).asUInt + reg_save.in2(0).asUInt)(xLen - 1, xLen - 1 - addr_tag_bits + 1) + 1.U, 0.U((xLen - addr_tag_bits).W))
  complete_address := Mux(reg_save.ctrl.funct === 3.U,
    tensor_dim_step_next(TensorVars.tensorRank - 1.U) === box_elements_num(TensorVars.tensorRank - 1.U)
    ,next_cacheline  >= (reg_save.in1(0).asUInt + reg_save.in2(0).asUInt))
//    ,address_next  >= (reg_save.in1(0).asUInt + reg_save.in2(0).asUInt))
  io.to_tempmem_inst.valid := state===s_save// & (reg_save.ctrl.mem_cmd.orR)
  io.to_tempmem_tag.valid := (state === s_l2cache_tag)// ||(state === s_save)
  io.to_tempmem_inst.bits.src := Mux(reg_save.ctrl.funct === 3.U, TensorVars.globalAddress, reg_save.in1(0))
  io.to_tempmem_inst.bits.srcsize := Mux(reg_save.ctrl.funct === 3.U,TensorVars.datawidth,reg_save.in2(0))
  io.to_tempmem_inst.bits.copysize := 0.U
  switch(reg_save.ctrl.funct){
    is(0.U){
      io.to_tempmem_inst.bits.copysize := 4.U << reg_save.ctrl.copysize
    }
    is(1.U){
      io.to_tempmem_inst.bits.copysize := reg_save.in2(0)
    }
    is(3.U){
      io.to_tempmem_inst.bits.copysize := Tensorcopysize
    }
  }
  io.to_tempmem_tag.bits.tag := Cat(reg_save.address(xLen - 1, xLen - 1 - addr_tag_bits + 1),0.U((xLen - addr_tag_bits).W))
  io.to_tempmem_tag.bits.box_dim0_start := box_dim0_start
  io.to_tempmem_tag.bits.tensor_dim0_start := tensor_dim0_start
  (0 until(5)).foreach( x => {
    io.to_tempmem_tag.bits.tensor_dim_step(x) := tensor_dim_step_reg(x)
  })
  io.to_tempmem_inst.bits.dst := reg_save.in3(0)
  io.to_tempmem_inst.bits.wid := reg_save.ctrl.wid
  io.to_tempmem_inst.bits.funct := reg_save.ctrl.funct
  io.to_tempmem_inst.bits.tensorvars := TensorVars
  io.to_l2cache.bits.a_opcode := 4.U
  //  io.to_l2cache.bits.a_size   := 0.U
  io.to_l2cache.bits.a_source :=  Cat(tag_mem_index_reg,inst_mem_index_reg,0.U((l1cache_sourceBits - log2Ceil(max_dma_tag) - log2Ceil(max_dma_inst)).W))
  //  io.to_l2cache.bits.source(l1cache_sourceBits - 1 - log2Ceil(max_dma_tag) - log2Ceil(max_dma_inst),0) := 0.U
  //  io.to_l2cache.bits.source(0) := 0.U // use crossbar of l1cache
  // temporarily set it as inst_mem_index_reg, for mshr; cat with wid, for shiftboard; jingguo ceshi hui fangzai hou 6 bit
  io.to_l2cache.bits.a_addr := p_addr_reg

  io.to_l2cache.bits.a_mask   := VecInit(Seq.fill(dcache_BlockWords)(Fill(BytesOfWord,1.U)))
  io.to_l2cache.bits.a_data   :=  VecInit(Seq.fill(dcache_BlockWords)(0.U(xLen.W)))
  io.to_l2cache.bits.a_param  :=  0.U
  io.to_l2cache.valid := (state===s_l2cache)// && !complete_address

  io.from_fifo.ready := state === s_idle

  //tlb
  io.to_l2TLB.valid := state === s_tlb_req
  io.to_l2TLB.bits.vaddr := Cat(reg_save.address(xLen - 1, xLen - 1 - addr_tag_bits + 1), 0.U((xLen - addr_tag_bits).W))
  io.to_l2TLB.bits.asid := reg_save.ctrl.asid
  io.from_l2TLB.ready := state === s_tlb_rsp

  // FSM State Transfer
  switch(state){
    is (s_idle){
      when(io.from_fifo.fire){ state := s_save }.otherwise{ state := state }
      //      cnt.reset()
    }
    is (s_save){
      //      when(reg_save.ctrl.mem_cmd.orR){ // read or write
      when(io.to_tempmem_inst.fire){state := s_l2cache_tag}.otherwise{state := s_save}
      //      }.otherwise{ state := s_idle }
    }
    is(s_l2cache_tag){
      when(io.to_tempmem_tag.fire){
        state := s_tlb_req
      }.otherwise{
        state := state
      }
    }
    is(s_tlb_req){
      when(io.to_l2TLB.fire){
        state := s_tlb_rsp
      }.otherwise{
        state := state
      }
    }
    is(s_tlb_rsp) {
      when(io.from_l2TLB.fire) {
        state := s_l2cache
      }.otherwise {
        state := state
      }
    }
    is (s_l2cache){
      when(io.to_l2cache.fire){
        when(complete_address) {
          state := s_idle
        }.otherwise {
          state := s_l2cache_tag
        }
      }.otherwise {
        state := state
      }
    }
  }
  // FSM Operation
  switch(state){
    is (s_idle){
      when(io.from_fifo.fire){ // Next: s_save
        reg_save.in1 := io.from_fifo.bits.in1
        reg_save.in3 := io.from_fifo.bits.in3
        reg_save.in2 := io.from_fifo.bits.in2
        reg_save.address := Mux(io.from_fifo.bits.ctrl.funct === 3.U,Cat(io.from_fifo.bits.in2(0)(xLen-1, xLen-1-addr_tag_bits +1),0.U((xLen - addr_tag_bits).W)),
          Cat(io.from_fifo.bits.in1(0)(xLen-1, xLen-1-addr_tag_bits +1),0.U((xLen - addr_tag_bits).W)))
        reg_save.ctrl := io.from_fifo.bits.ctrl
      }.otherwise{reg_save := RegInit(0.U.asTypeOf(new regSave))}
    }
    is (s_save){
      when(io.to_tempmem_inst.fire){
        inst_mem_index_reg := io.inst_mem_index
      }  // get entryID from MSHR
    }
    is(s_tlb_req){

    }
    is(s_tlb_rsp){
      when(io.from_l2TLB.fire){
        p_addr_reg := io.from_l2TLB.bits
      }
    }
    is(s_l2cache_tag){
      when(io.to_tempmem_tag.fire){
        tag_mem_index_reg  := io.tag_mem_index
      }
    }
    is (s_l2cache){
      when(io.to_l2cache.fire){                                      // request is sent
        reg_save.address := address_next
        (0 until(5)).foreach(x =>{
          tensor_dim_step_reg(x) := tensor_dim_step_next(x)
        })
        //        box_dim0_start_reg := box_dim0_start
      }.otherwise{
        reg_save.address := reg_save.address
      }

    }
  }
  //  printf("state: %b\n",state.asUInt)
  //    printf("copysize: %b\n",io.to_tempmem_inst.bits.copysize.asUInt)
  //    printf("in2: %d\n",io.from_fifo.bits.in2(0).asUInt)
  //    printf("reg_save_in2: %d\n",reg_save.in2.asUInt)
  //  printf("l2req:address: %b\n",io.to_l2cache.bits.address.asUInt)
}


class Temp_mem(implicit p: Parameters) extends Module { //2024.5.9 start here! sth wrong with the l2cachemask
  val io = IO(new Bundle {
    val from_addr = Flipped(DecoupledIO(//new Bundle {
      //      val tag = Input(new vExeDataDMA)
      new vExeDataDMA
    ))
    //    val from_addr_tag = Flipped(DecoupledIO(UInt(addr_tag_bits.W)))
    val from_addr_tag = Flipped(DecoupledIO(new cacheline_info))

    val inst_mem_index = Output(UInt(log2Ceil(max_dma_inst).W))
    val tag_mem_index = Output(UInt(log2Ceil(max_dma_tag).W))

    val from_l2cache = Flipped(DecoupledIO(new DCacheMemRsp))

    val from_shared = Flipped(DecoupledIO(new DCacheCoreRsp_np))

    val to_shared = DecoupledIO(new TempOutput)
    val inst_complete = DecoupledIO(UInt(32.W))
  })
  //  val datamem =  SyncReadMem(max_l2cacheline, UInt(io.from_l2cache.bits.getWidth.W))
  val datamem =  Mem(max_l2cacheline, new l2cacheline_info)
  val instmem =   Mem(max_dma_inst, new vExeDataDMA)
  val tagmem  =   Mem(max_dma_tag, new cacheline_info)
  //  val stepMem =   Mem(max_dma_tag, Vec(5, UInt(xLen.W)))
  val from_l2cache_all = Wire(new l2cacheline_info)
  from_l2cache_all.base := io.from_l2cache.bits
  from_l2cache_all.cacheline_info.tag  := tagmem.read(io.from_l2cache.bits.d_source(l1cache_sourceBits - 1, l1cache_sourceBits - 1 - log2Ceil(max_dma_tag) + 1)).tag
  from_l2cache_all.cacheline_info.tensor_dim0_start := tagmem.read(io.from_l2cache.bits.d_source(l1cache_sourceBits - 1, l1cache_sourceBits - 1 - log2Ceil(max_dma_tag) + 1)).tensor_dim0_start
  from_l2cache_all.cacheline_info.box_dim0_start    := tagmem.read(io.from_l2cache.bits.d_source(l1cache_sourceBits - 1, l1cache_sourceBits - 1 - log2Ceil(max_dma_tag) + 1)).box_dim0_start
  (0 until(5)).foreach( x=> {
    from_l2cache_all.cacheline_info.tensor_dim_step(x)          := tagmem.read(io.from_l2cache.bits.d_source(l1cache_sourceBits - 1, l1cache_sourceBits - 1 - log2Ceil(max_dma_tag) + 1)).tensor_dim_step(x)
  })

  //  val tensor_dim_step = Wire(Vec(5, UInt(xLen.W)))
  //  tensor_dim_step := stepMem.read(io.from_l2cache.bits.source(l1cache_sourceBits - 1, l1cache_sourceBits - 1 - log2Ceil(max_dma_tag) + 1))
  //  val shared_cnt = RegInit(VecInit(Seq.fill(max_dma_inst)((numgroupinsdmax-1).U((log2Ceil(numgroupinsdmax)).W))))
  //  val finish_cnt = RegInit(VecInit(Seq.fill(max_dma_inst)((numgroupinsdmax-1).U((log2Ceil(numgroupinsdmax)).W))))
  //val shared_cnt = RegInit(VecInit(Seq.fill(max_dma_inst)((numgroupinsdmax-1).U((xLen).W))))
  val finish_cnt = RegInit(VecInit(Seq.fill(max_dma_inst)(1.U((xLen).W))))
  val used_inst = RegInit(0.U(max_dma_inst.W))
  val used_tag = RegInit(0.U(max_dma_tag.W))
  val used_cache = RegInit(0.U(max_l2cacheline.W))
  val complete = Wire(Vec(max_dma_inst,Bool()))
  (0 until (max_dma_inst)).foreach(x => {
    complete(x) := finish_cnt(x) === 0.U
  })
  val entry_index_reg = RegInit(VecInit(Seq.fill(max_l2cacheline)(0.U(log2Ceil(max_dma_inst).W))))
  val valid_inst_entry = Mux(used_inst.andR, 0.U, PriorityEncoder(~used_inst))
  val valid_data_entry = Mux(used_cache.andR, 0.U, PriorityEncoder(~used_cache))
  val valid_tag_entry  = Mux(used_tag.andR, 0.U, PriorityEncoder(~used_tag))
  val complete_inst_entry = Mux(complete.asUInt.orR, PriorityEncoder(complete), 0.U)
  val output_data_entry = Reg(UInt(log2Ceil(max_l2cacheline).W))
  val output_tag_entry = Reg(UInt(log2Ceil(max_dma_tag).W))
  val output_inst_entry = Reg(UInt(log2Ceil(max_dma_inst).W))
  //l2cache logic
  val current_inst_entry_index = io.from_l2cache.bits.d_source(l1cache_sourceBits -1 -log2Ceil(max_dma_tag), l1cache_sourceBits -1 -log2Ceil(max_dma_tag) - log2Ceil(max_dma_inst) + 1).asUInt

  //  val current_inst_entry_index = io.from_l2cache.bits.source(log2Ceil(max_dma_inst) - 1, 0).asUInt

  val current_inst_entry_index_reg = RegInit(0.U(log2Ceil(max_dma_inst).W))
  //  val current_tag_index = PriorityEncoder(output_inst(current_inst_entry_index).cacheline_info.tag.map(_===current_tag))
  val mask_l2cache = RegInit(VecInit(Seq.fill(numgroupl2cache)(false.B)))
//  val mask_l2cache_cur = Wire(Vec(numgroupl2cache,Bool()))
//  (0 until(numgroupl2cache)).foreach(x => {
//      when(x.asUInt >= PriorityEncoder(mask_l2cache).asUInt && x.asUInt < PriorityEncoder(mask_l2cache).asUInt + numgroupshared.asUInt && mask_l2cache(x)){
//        mask_l2cache_cur(x) := true.B
//      }.otherwise{
//        mask_l2cache_cur(x) := false.B
//      }
//  })
  val mask_index = Wire(UInt((log2Ceil(xLen)+1).W))
  mask_index := PriorityEncoder(mask_l2cache)
  val mask_l2cache_next = Wire(Vec(numgroupl2cache, Bool()))
  (0 until (numgroupl2cache)).foreach(x => {
//    when(mask_l2cache(x)){
//      when(x.asUInt >= PriorityEncoder(mask_l2cache) && x.asUInt < PriorityEncoder(mask_l2cache) + numgroupshared.asUInt){
//        mask_l2cache_next(x) := false.B
//      }.otherwise{
//        mask_l2cache_next(x) := true.B
//      }
//    }.otherwise{
//      mask_l2cache_next(x) := false.B
//    }
//    printf("x: %d  prior: %d  prior+shared: %d \n",x.asUInt,mask_index.asUInt,mask_index + numgroupshared.asUInt)
    mask_l2cache_next(x) := mask_l2cache(x) && !(x.asUInt >= mask_index && x.asUInt < mask_index + numgroupshared.asUInt)
  })
  val mask_shared = Wire(Vec(numgroupshared,Bool()))
  (0 until(numgroupshared)).foreach( x => {
    when(x.asUInt + mask_index.asUInt < numgroupl2cache.asUInt){
      mask_shared(x) := mask_l2cache(x.asUInt + mask_index)
    }.otherwise{
      mask_shared(x) := false.B
    }
  })
//  val mask_l2cache_obb = RegInit(VecInit(Seq.fill(numgroupl2cache* dma_aligned_bulk)(false.B)))
  val output_inst = Reg(new vExeDataDMA)
  val output_data = Reg(new l2cacheline_info)
  val output_data_4byte = Wire(Vec(numgroupl2cache, UInt((dma_aligned_bulk * BitsOfByte).W)))
  //  printf("output_data.base.d_data: %d",output_data.base.d_data.getWidth.asUInt)
  (0 until (numgroupl2cache)).foreach(x => {
    output_data_4byte(x) := output_data.base.d_data.asUInt((x + 1) * (dma_aligned_bulk * BitsOfByte) - 1, x * (dma_aligned_bulk * BitsOfByte))
  })
  // slice the l2cacheline to num_thread
//  val shared_mask_reg = Reg(Vec(shared_group_num_0, Bool()))
//  val shared_mask_next = Wire(Vec(shared_group_num_0, Bool()))
//  val shared_mask_current = Wire(Vec(shared_group_num_0,Bool()))
//  (0 until(shared_group_num_0)).foreach( x => {
//    when(x.asUInt >= PriorityEncoder(shared_mask_reg) && x.asUInt < PriorityEncoder(shared_mask_reg) + shared_group_num_1.asUInt){
//      shared_mask_current(x) := true.B
//    }.otherwise{
//      shared_mask_current(x) := false.B
//    }
//  })
//  (0 until(shared_group_num_0)).foreach( x => {
//    shared_mask_next(x) := shared_mask_reg(x) && !shared_mask_current(x)
//  })


  //  val source_width = output_data.source.getWidth
  //  val tag_reg = RegInit(0.U(addr_tag_bits.W))
  val tag_wire = Wire(UInt(xLen.W))
  tag_wire := output_data.cacheline_info.tag
  //  tag_wire := Cat(tagmem.read(output_data.source(xLen - 1, xLen - 1 - log2Ceil(max_dma_tag) + 1)),0.U((xLen - addr_tag_bits).W))

  val tmp_1 = Wire(Vec(l2cacheline, UInt((1 * BitsOfByte).W)))
  val tmp_2 = Wire(Vec(l2cacheline/2, UInt((2 * BitsOfByte).W)))
  val tmp_4 = Wire(Vec(l2cacheline/4, UInt((4 * BitsOfByte).W)))
  val tmp_8 = Wire(Vec(l2cacheline/8, UInt((8 * BitsOfByte).W)))
  tmp_1.zipWithIndex.foreach { case (x, i) => x := output_data.base.d_data.asUInt(i * BitsOfByte + 1 * BitsOfByte - 1, i * BitsOfByte) }
  tmp_2.zipWithIndex.foreach { case (x, i) => x := output_data.base.d_data.asUInt(i * 2 * BitsOfByte + 2 * BitsOfByte - 1, i * 2 * BitsOfByte) }
  tmp_4.zipWithIndex.foreach { case (x, i) => x := output_data.base.d_data.asUInt(i * 4 * BitsOfByte + 4 * BitsOfByte - 1, i * 4 * BitsOfByte) }
  tmp_8.zipWithIndex.foreach { case (x, i) => x := output_data.base.d_data.asUInt(i * 8 * BitsOfByte + 8 * BitsOfByte - 1, i * 8 * BitsOfByte) }
  switch(output_inst.tensorvars.datawidth) {
    is(1.U) {
      (0 until l2cacheline).foreach(x => {
        var line_in_tensor = (tag_wire.asUInt + x.asUInt >= output_data.cacheline_info.tensor_dim0_start.asUInt) && (tag_wire.asUInt + x.asUInt < (output_data.cacheline_info.tensor_dim0_start + output_inst.tensorvars.globalDim(0) * output_inst.tensorvars.datawidth).asUInt)
        when(!line_in_tensor){
          tmp_1(x) := 0.U
        }
      })
    }
    is(2.U) {
      (0 until l2cacheline  by 2).foreach(x => {
        var line_in_tensor = (tag_wire.asUInt + x.asUInt >= output_data.cacheline_info.tensor_dim0_start) && (tag_wire.asUInt + x.asUInt < output_data.cacheline_info.tensor_dim0_start + output_inst.tensorvars.globalDim(0) * output_inst.tensorvars.datawidth)
        when(!line_in_tensor.asBool){
          switch(output_inst.tensorvars.dataType) {
            is(UINT16.asUInt) {
              tmp_2(x/2) :=  0.U
            }
            is(FLOAT16.asUInt) {
              tmp_2(x/2) :=  Mux(output_inst.tensorvars.oobfill.asBool,VecInit(Seq.fill(2 * BitsOfByte)(1.U)).asUInt,0.U)
              //              tmp_2(x/2) :=  Mux(output_inst.tensorvars.oobfill.asBool,"b1111111111111111".U,0.U)
              //                        output_data.base.d_data(x + 2 * BitsOfByte - 1, x) := Mux(output_inst.tensorvars.oobfill.asBool,
              //                          Cat(output_data.base.d_data(x + 2 * BitsOfByte - 1),(BigInt(1) << (2 * BitsOfByte - 1) - 1).asUInt),0.U)
            }
            is(BFLOAT16.asUInt) {
              tmp_2(x/2) :=  Mux(output_inst.tensorvars.oobfill.asBool,VecInit(Seq.fill(2 * BitsOfByte)(1.U)).asUInt,0.U)
              //                        output_data.base.d_data(x + 2 * BitsOfByte - 1, x) := Mux(output_inst.tensorvars.oobfill.asBool,
              //                          Cat(output_data.base.d_data(x + 2 * BitsOfByte - 1), (BigInt(1) << (2 * BitsOfByte - 1) - 1).asUInt), 0.U)
            }
          }
        }
      })
    }
    is(4.U) {
      (0 until l2cacheline by 4).foreach(x => {
        var line_in_tensor = (tag_wire.asUInt + (x.asUInt >> log2Ceil(BitsOfByte).asUInt).asUInt >= output_data.cacheline_info.tensor_dim0_start) && (tag_wire.asUInt + (x.asUInt >> log2Ceil(BitsOfByte).asUInt).asUInt < output_data.cacheline_info.tensor_dim0_start + output_inst.tensorvars.globalDim(0) * output_inst.tensorvars.datawidth)
        when(!line_in_tensor.asBool) {
          switch(output_inst.tensorvars.dataType) {
            is(UINT32.asUInt) {
              tmp_4( x / 4) := 0.U
            }
            is(INT32.asUInt) {
              tmp_4( x / 4) := 0.U
            }
            is(FLOAT32.asUInt) {
              tmp_4( x / 4) := Mux(output_inst.tensorvars.oobfill.asBool,VecInit(Seq.fill(4 * BitsOfByte)(1.U)).asUInt,0.U)
            }
            is(FLOAT32_FTZ.asUInt) {
              tmp_4( x / 4) := Mux(output_inst.tensorvars.oobfill.asBool,VecInit(Seq.fill(4 * BitsOfByte)(1.U)).asUInt,0.U)
            }
            is(TFLOAT32.asUInt) {
              tmp_4( x / 4) := Mux(output_inst.tensorvars.oobfill.asBool,VecInit(Seq.fill(4 * BitsOfByte)(1.U)).asUInt,0.U)
            }
            is(TFLOAT32_FTZ.asUInt) {
              tmp_4( x / 4) := Mux(output_inst.tensorvars.oobfill.asBool,VecInit(Seq.fill(4 * BitsOfByte)(1.U)).asUInt,0.U)
            }
          }
        }
      })
    }
    is(8.U) {
      (0 until l2cacheline  by 8).foreach(x => {
        var line_in_tensor = (tag_wire.asUInt + x.asUInt >= output_data.cacheline_info.tensor_dim0_start) && (tag_wire.asUInt + x.asUInt < output_data.cacheline_info.tensor_dim0_start + output_inst.tensorvars.globalDim(0) * output_inst.tensorvars.datawidth)
        when(!line_in_tensor.asBool) {
          switch(output_inst.tensorvars.dataType) {
            is(UINT64.asUInt) {
              tmp_8( x / 8) := 0.U
            }
            is(INT64.asUInt) {
              tmp_8( x / 8) := 0.U
            }
            is(FLOAT64.asUInt) {
              tmp_8( x / 8) := Mux(output_inst.tensorvars.oobfill.asBool,VecInit(Seq.fill(8 * BitsOfByte)(1.U)).asUInt,0.U)
            }
          }
        }
      })
    }
  }



  val s_idle ::s_getdata :: s_shared :: s_shared1:: s_reset :: Nil = Enum(5)
  val state = RegInit(s_idle)
//  io.from_l2cache.ready := !(used_cache.andR) && !(io.to_shared.ready && used_cache.orR) && !(state === s_shared)&& !(state === s_shared1)
  io.from_l2cache.ready := !(used_cache.andR) && (state === s_idle || state === s_getdata)
  io.from_shared.ready := !io.from_addr.fire && !(state === s_reset) //&& used_cache.orR
  io.from_addr.ready := state === s_idle && !(used_inst.andR)
  io.from_addr_tag.ready := !(used_tag.andR) && !io.from_l2cache.fire
  io.to_shared.valid := state === s_idle && used_cache.orR
  io.inst_complete.valid := state === s_reset
  io.inst_mem_index := Mux(io.from_addr.fire, valid_inst_entry, 0.U)
  io.tag_mem_index  := Mux(io.from_addr_tag.fire, valid_tag_entry, 0.U)

  //finsih_cnt part
  when(io.from_addr.fire) {
    switch(io.from_addr.bits.funct) {
      is(0.U) {
        var group_start = io.from_addr.bits.dst(xLen - 1, 2)
        var group_end = (io.from_addr.bits.dst + io.from_addr.bits.copysize)(xLen - 1, 2)

        finish_cnt(valid_inst_entry) := 1.U + (group_end.asUInt - group_start.asUInt)
        //          printf("group_start : %d\n",group_start.asUInt)
        //          printf("group_end : %d\n",group_end.asUInt)
        //          printf("finish_cnt : %d\n",finish_cnt(valid_inst_entry).asUInt)
      }
      is(1.U) {
        finish_cnt(valid_inst_entry) := io.from_addr.bits.copysize >> log2Ceil(dma_aligned_bulk)
      }
      is(3.U) {
        finish_cnt(valid_inst_entry) := io.from_addr.bits.copysize >> log2Ceil(dma_aligned_bulk)
      }
      //shared_cnt(valid_inst_entry) := io.from_addr.bits.copysize >> log2Ceil(dma_aligned_bulk)
    }
  }

  when(io.from_shared.fire) {
    finish_cnt(io.from_shared.bits.instrId) := finish_cnt(io.from_shared.bits.instrId) - (PopCount(io.from_shared.bits.activeMask) >> log2Ceil(dma_aligned_bulk / 4)).asUInt
  }

  //state machine
  switch(state){
    is(s_idle){
      when(io.to_shared.ready && used_cache.orR && !io.from_l2cache.fire){
        state := s_getdata
      }.elsewhen(complete.asUInt.orR){
        state := s_reset
      }.otherwise{
        state := s_idle
      }
    }
    is(s_getdata){
      //    when(io.to_shared.ready){state:=s_shared}.otherwise{state:=s_getdata}
      state:=s_shared
    }
    is(s_shared){
//      when(complete.asUInt.orR){state:=s_reset}.otherwise{
      when(io.to_shared.fire) {
        state := s_shared1
      }.otherwise{
        state := state
      }
//      }
    }
    is(s_shared1){
      when(mask_l2cache.asUInt === 0.U) {
        state := s_idle
      }.otherwise {
        state := s_shared
      }
    }
    is(s_reset){
      // todo here, can not use the correct code here, for IO
        when(io.inst_complete.fire){
          state:=s_idle
        }.otherwise{
          state := s_reset
        }
    }
  }

  switch(state){
    is(s_idle) {
      mask_l2cache := VecInit(Seq.fill(numgroupl2cache)(false.B))
      output_data_entry := 0.U
      output_inst := 0.U.asTypeOf(new vExeDataDMA)
      output_data := 0.U.asTypeOf(new l2cacheline_info)

      when(io.from_addr.fire) {
        used_inst := used_inst.bitSet(valid_inst_entry, true.B)
        instmem.write(valid_inst_entry, io.from_addr.bits)
      }
      when(io.from_l2cache.fire) {
        used_cache := used_cache.bitSet(valid_data_entry, true.B)
        used_tag := used_tag.bitSet(io.from_l2cache.bits.d_source(l1cache_sourceBits - 1, l1cache_sourceBits - 1 - log2Ceil(max_dma_tag) + 1), false.B)
        tagmem.write(io.from_l2cache.bits.d_source(l1cache_sourceBits - 1, l1cache_sourceBits - 1 - log2Ceil(max_dma_tag) + 1), 0.U.asTypeOf(new cacheline_info))
        datamem.write(valid_data_entry, from_l2cache_all)
        entry_index_reg(valid_data_entry) := current_inst_entry_index
        //    tagmem.write(io.from_l2cache.bits.source(l1cache_sourceBits - 1, l1cache_sourceBits - 1 - log2Ceil(max_dma_tag) + 1),0.U(addr_tag_bits.W))
      }
      when(io.from_addr_tag.fire) {
        used_tag := used_tag.bitSet(valid_tag_entry, true.B)
        //      tagmem.write(valid_tag_entry, io.from_addr_tag.bits)
        tagmem.write(valid_tag_entry, io.from_addr_tag.bits)
        //    stepMem.write(valid_tag_entry,io.from_addr_tag.bits.tensor_dim0_start)
      }
      when(io.to_shared.ready && used_cache.orR && !io.from_l2cache.fire){
        output_data := datamem.read(PriorityEncoder(used_cache).asUInt).asTypeOf(new l2cacheline_info)
        output_inst := instmem.read(entry_index_reg(PriorityEncoder(used_cache)))
        current_inst_entry_index_reg := entry_index_reg(PriorityEncoder(used_cache))
        output_data_entry := PriorityEncoder(used_cache)
      }
      when(complete.asUInt.orR){
        output_inst := instmem(PriorityEncoder(complete.asUInt))
      }
    }
    is(s_getdata){
      switch(output_inst.funct){
        is(0.U) {
          (0 until (numgroupl2cache)).foreach(x => {
            var condition1 = (tag_wire.asUInt + (x.asUInt * dma_aligned_bulk.asUInt)) <= output_inst.src && (tag_wire.asUInt + ((x + 1).asUInt * dma_aligned_bulk.asUInt - 1.U)) >= output_inst.src
            var condition2 = (tag_wire.asUInt + (x.asUInt * dma_aligned_bulk.asUInt) >= output_inst.src) && (tag_wire.asUInt + (x.asUInt * dma_aligned_bulk.asUInt) < output_inst.src + output_inst.copysize)
            var condition3 = (tag_wire.asUInt + (x.asUInt * dma_aligned_bulk.asUInt)) < output_inst.src + output_inst.copysize && (tag_wire.asUInt + ((x.asUInt + 1.U) * dma_aligned_bulk.asUInt) - 1.U) >= output_inst.src + output_inst.copysize
            when(condition1 || condition2 || condition3) {
              printf(p"addr_true : 0x${Hexadecimal(tag_wire.asUInt + (x.asUInt * dma_aligned_bulk.asUInt))} ")
              mask_l2cache(x) := true.B
            }.otherwise {
              printf(p"addr_false : 0x${Hexadecimal(tag_wire.asUInt + (x.asUInt * dma_aligned_bulk.asUInt))} ")
              mask_l2cache(x) := false.B
            }
          })
        }
        is(1.U){
          //            when((tag_wire.asUInt + (x.asUInt * dma_aligned_bulk.asUInt) >= output_inst.src) && (tag_wire.asUInt + (x.asUInt * dma_aligned_bulk.asUInt) < output_inst.src + output_inst.copysize)) {
          (0 until (numgroupl2cache)).foreach(x => {
            var condition2 = (tag_wire.asUInt + (x.asUInt * dma_aligned_bulk.asUInt) >= output_inst.src) && (tag_wire.asUInt + (x.asUInt * dma_aligned_bulk.asUInt) < output_inst.src + output_inst.copysize)
            when(condition2) {
              printf(p"addr_true : 0x${Hexadecimal(tag_wire.asUInt + (x.asUInt * dma_aligned_bulk.asUInt))} ")
              mask_l2cache(x) := true.B
            }.otherwise {
              printf(p"addr_false : 0x${Hexadecimal(tag_wire.asUInt + (x.asUInt * dma_aligned_bulk.asUInt))} ")
              mask_l2cache(x) := false.B
            }
          })
        }
        is(3.U){
          var box_dim_start = output_data.cacheline_info.box_dim0_start
          //            var output_data.cacheline_info.tensor_dim0_start = output_data.cacheline_info.tensor_dim0_start
          var datawidth = output_inst.tensorvars.datawidth
          (0 until(numgroupl2cache)).foreach( x=> {
            //              var element_group_width = datawidth * output_inst.tensorvars.elementStrides(0)
            var cacheline_in_box = (tag_wire.asUInt + x.asUInt * dma_aligned_bulk.asUInt >= box_dim_start) && (tag_wire.asUInt + x.asUInt * dma_aligned_bulk.asUInt < box_dim_start + output_inst.tensorvars.boxDim(0) * output_inst.tensorvars.datawidth)
            //              var index_of_the_start = (tag_wire.asUInt - box_dim_start.asUInt) % (from_l2cache_all.asUInt)
            when(cacheline_in_box){
              mask_l2cache(x) := true.B
            }.otherwise{
              mask_l2cache(x) := false.B
            }
          })
          switch(datawidth){
            is(1.U){
              output_data.base.d_data := tmp_1.asTypeOf(output_data.base.d_data)
            }
            is(2.U) {
              output_data.base.d_data := tmp_2.asTypeOf(output_data.base.d_data)
            }
            is(4.U) {
              output_data.base.d_data := tmp_4.asTypeOf(output_data.base.d_data)
            }
            is(8.U) {
              output_data.base.d_data := tmp_8.asTypeOf(output_data.base.d_data)
            }
          }
        }
      }
      when(io.from_addr_tag.fire) {
        used_tag := used_tag.bitSet(valid_tag_entry, true.B)
        //      tagmem.write(valid_tag_entry, io.from_addr_tag.bits)
        tagmem.write(valid_tag_entry, io.from_addr_tag.bits)
        //    stepMem.write(valid_tag_entry,io.from_addr_tag.bits.tensor_dim0_start)
      }
      when(io.from_l2cache.fire) {
        used_cache := used_cache.bitSet(valid_data_entry, true.B)
        used_tag := used_tag.bitSet(io.from_l2cache.bits.d_source(l1cache_sourceBits - 1, l1cache_sourceBits - 1 - log2Ceil(max_dma_tag) + 1), false.B)
        tagmem.write(io.from_l2cache.bits.d_source(l1cache_sourceBits - 1, l1cache_sourceBits - 1 - log2Ceil(max_dma_tag) + 1), 0.U.asTypeOf(new cacheline_info))
        datamem.write(valid_data_entry, from_l2cache_all)
        entry_index_reg(valid_data_entry) := current_inst_entry_index
        //    tagmem.write(io.from_l2cache.bits.source(l1cache_sourceBits - 1, l1cache_sourceBits - 1 - log2Ceil(max_dma_tag) + 1),0.U(addr_tag_bits.W))
      }
    }
    is(s_shared){
      when(io.to_shared.fire){
        mask_l2cache := mask_l2cache_next
      }
      when(io.from_addr_tag.fire) {
        used_tag := used_tag.bitSet(valid_tag_entry, true.B)
        //      tagmem.write(valid_tag_entry, io.from_addr_tag.bits)
        tagmem.write(valid_tag_entry, io.from_addr_tag.bits)
        //    stepMem.write(valid_tag_entry,io.from_addr_tag.bits.tensor_dim0_start)
      }
    }
    is(s_shared1){
      when(mask_l2cache.asUInt === 0.U){
        datamem.write(output_data_entry, 0.U.asTypeOf(new l2cacheline_info))
        used_cache := used_cache.bitSet(output_data_entry, false.B)
//        tagmem.write(output_data.base.d_source(l1cache_sourceBits - 1, l1cache_sourceBits - 1 - log2Ceil(max_dma_tag) + 1),0.U.asTypeOf(new cacheline_info()))
//        used_tag := used_tag.bitSet(output_data.base.d_source(l1cache_sourceBits - 1, l1cache_sourceBits - 1 - log2Ceil(max_dma_tag) + 1), false.B)
        output_data_entry := 0.U
        //shared_cnt(current_inst_entry_index_reg) := shared_cnt(current_inst_entry_index_reg) - PopCount(mask_l2cache.asUInt)
        //        datamem.write(0.U,0.U)
        mask_l2cache := VecInit(Seq.fill(numgroupl2cache)(false.B))
        output_inst := 0.U.asTypeOf(new vExeDataDMA)
        //        tag_reg :=  0.U(addr_tag_bits.W)
        current_inst_entry_index_reg := 0.U(log2Ceil(max_dma_inst).W)
      }
      when(io.from_addr_tag.fire) {
        used_tag := used_tag.bitSet(valid_tag_entry, true.B)
        //      tagmem.write(valid_tag_entry, io.from_addr_tag.bits)
        tagmem.write(valid_tag_entry, io.from_addr_tag.bits)
        //    stepMem.write(valid_tag_entry,io.from_addr_tag.bits.tensor_dim0_start)
      }
    }
    is(s_reset){
      used_inst := used_inst.bitSet(complete_inst_entry, false.B)
      finish_cnt(PriorityEncoder(complete)) := 1.U
      //      instmem.write(complete_inst_entry,0.U)
      when(io.from_addr_tag.fire) {
        used_tag := used_tag.bitSet(valid_tag_entry, true.B)
        //      tagmem.write(valid_tag_entry, io.from_addr_tag.bits)
        tagmem.write(valid_tag_entry, io.from_addr_tag.bits)
        //    stepMem.write(valid_tag_entry,io.from_addr_tag.bits.tensor_dim0_start)
      }
    }
  }
  io.to_shared.valid := state === s_shared
  io.to_shared.bits.entry_index := current_inst_entry_index_reg
  io.to_shared.bits.mask := mask_shared
  (0 until(numgroupshared)).foreach( x=> {
    when(x.asUInt + mask_index < numgroupl2cache.asUInt){
      io.to_shared.bits.data(x) := output_data_4byte(x.asUInt + mask_index)
    }.otherwise{
      io.to_shared.bits.data(x) := 0.U
    }
  })
  io.to_shared.bits.instinfo := output_inst
  io.to_shared.bits.cacheline_info.tag := tag_wire + mask_index * dma_aligned_bulk.asUInt
  io.to_shared.bits.cacheline_info.tensor_dim0_start := output_data.cacheline_info.tensor_dim0_start
  io.to_shared.bits.cacheline_info.tensor_dim_step := output_data.cacheline_info.tensor_dim_step
  io.to_shared.bits.cacheline_info.box_dim0_start := output_data.cacheline_info.box_dim0_start
  io.inst_complete.bits := output_inst.wid


  //  printf("state: %d \n",state.asUInt)
  //  printf("state: %d \n",state.asUInt)
  //  printf("usedmem: %b \n",used_cache.asUInt)
  //  printf("to_shared: %b \n",io.to_shared.ready.asUInt)
  //  printf("l22temp data: %d \n",io.from_l2cache.bits.data(0).asUInt)
  //  printf("l22temp data: %d \n",io.from_l2cache.bits.data(1).asUInt)
  //  printf("l22temp data: %d \n",io.from_l2cache.bits.data(2).asUInt)
  //  printf("l22temp data: %d \n",io.from_l2cache.bits.data(3).asUInt)

}

class Addrcalc_shared() extends Module {
  val io = IO(new Bundle {
    val from_temp = Flipped(DecoupledIO(new TempOutput))
    val shared_req = DecoupledIO(new ShareMemCoreReq_np)
  })
  //  val s_idle :: s_save :: s_shared :: Nil = Enum(3)
  //  val s_idle :: s_shared1 ::s_shared2:: s_shared3::Nil = Enum(4)
  val s_idle :: s_shared1 ::s_shared2::s_shift::Nil = Enum(4)
  val state = RegInit(init = s_idle)
  val reg_save = Reg(new TempOutput)
  val current_numgroup = Reg(UInt(log2Ceil(numgroupshared).W))
  val output_reg = Reg(new ShareMemCoreReq_np)
  val cnt = new Counter(n = numgroupshared)
  //  val inst_mem_index_reg = RegInit(0.U(log2Ceil(max_dma_inst).W))

  val box_elements_num = Wire(Vec(5, UInt(xLen.W)))
  (0 until (5)).foreach(x => {
    box_elements_num(x) := ((reg_save.instinfo.tensorvars.boxDim(x) + reg_save.instinfo.tensorvars.elementStrides(x) - 1.U) / reg_save.instinfo.tensorvars.elementStrides(x)).asUInt
  })
  val l2cacheTag_shift = Wire(UInt(xLen.W))
  l2cacheTag_shift := reg_save.cacheline_info.tag
  val addr = Wire(Vec(numgroupshared,UInt(xLen.W))) // changed according to l2cachetag
  addr := VecInit(Seq.fill(numgroupshared)(0.U(xLen.W)))
  (0 until(numgroupshared)).foreach(x => { // assume the data stored in memory continuously
    switch(reg_save.instinfo.funct){
      is(0.U){
        addr(x) := l2cacheTag_shift - reg_save.instinfo.src + reg_save.instinfo.dst + x.asUInt * dma_aligned_bulk.U
      }
      is(1.U){
        addr(x) := reg_save.cacheline_info.tag - reg_save.instinfo.src + reg_save.instinfo.dst + x.asUInt * dma_aligned_bulk.U
      }
      is(3.U){
        var shared_box_dim_start = reg_save.instinfo.dst +
          reg_save.cacheline_info.tensor_dim_step(1) * reg_save.instinfo.tensorvars.boxDim(0) +
          reg_save.cacheline_info.tensor_dim_step(2) * box_elements_num(1) * reg_save.instinfo.tensorvars.boxDim(0) +
          reg_save.cacheline_info.tensor_dim_step(3) * box_elements_num(2) * box_elements_num(1) * reg_save.instinfo.tensorvars.boxDim(0) +
          reg_save.cacheline_info.tensor_dim_step(4) * box_elements_num(3) * box_elements_num(2) * box_elements_num(1) * reg_save.instinfo.tensorvars.boxDim(0)
        //        printf(p"addr_shift : ${Hexadecimal(reg_save.cacheline_info.tensor_dim_step(1) * reg_save.instinfo.tensorvars.boxDim(0) +
        //          reg_save.cacheline_info.tensor_dim_step(2) * box_elements_num(1) * reg_save.instinfo.tensorvars.boxDim(0) +
        //          reg_save.cacheline_info.tensor_dim_step(3) * box_elements_num(2) * box_elements_num(1) * reg_save.instinfo.tensorvars.boxDim(0) +
        //          reg_save.cacheline_info.tensor_dim_step(4) * box_elements_num(3) * box_elements_num(2) * box_elements_num(1) * reg_save.instinfo.tensorvars.boxDim(0))} \n")
        //        printf(p"addr(x) : ${Hexadecimal(reg_save.cacheline_info.tensor_dim_step(1) * reg_save.instinfo.tensorvars.boxDim(0) +
        addr(x) := reg_save.cacheline_info.tag - reg_save.cacheline_info.box_dim0_start + shared_box_dim_start + x.asUInt * dma_aligned_bulk.U
      }
    }
  })
  val mask_bits = Wire(UInt((numgroupshared).W))
  val addr_wire=Wire(UInt(xLen.W))
  addr_wire:=addr(PriorityEncoder(mask_bits.asUInt))
  val current_tag = Mux(reg_save.mask.asUInt =/=0.U, addr_wire(xLen-1, xLen-1-dcache_TagBits+1), 0.U(dcache_TagBits.W))
  val setIdx = Mux(reg_save.mask.asUInt=/=0.U, addr_wire(xLen-1-dcache_TagBits, xLen-1-dcache_TagBits-dcache_SetIdxBits+1), 0.U(dcache_SetIdxBits.W))
  val blockOffset = Wire(Vec(numgroupshared, UInt(dcache_BlockOffsetBits.W)))
  (0 until numgroupshared).foreach( x => blockOffset(x) := addr(x)(10, 2) )


  val current_mask = Wire(Vec(numgroupshared, Bool()))
  (0 until numgroupshared).foreach(x => {
    current_mask(x) := reg_save.mask(x) && (addr(x)(xLen-1, xLen-1-dcache_TagBits+1)===current_tag && addr(x)(xLen-1-dcache_TagBits, xLen-1-dcache_TagBits-dcache_SetIdxBits+1)===setIdx)
  })
  val mask_next = Wire(Vec(numgroupshared, Bool()))
  (0 until numgroupshared).foreach( x => {
    mask_next(x) := reg_save.mask(x) && !(addr(x)(xLen-1, xLen-1-dcache_TagBits+1)===current_tag && addr(x)(xLen-1-dcache_TagBits, xLen-1-dcache_TagBits-dcache_SetIdxBits+1)===setIdx)
  })
  //  val current_mask_index = Reg(Vec(numgroupshared, UInt(log2Ceil(numgroupshared).W)))
  // wordoffset part
  val wordOffset1H_bytes = Wire(Vec(numgroupshared * dma_aligned_bulk, Bool())) // will changed according to addr
  (0 until numgroupshared * dma_aligned_bulk).foreach(x => {
    // todo
    wordOffset1H_bytes(x) := true.B
    switch(reg_save.instinfo.funct) {
      is(0.U) {
        var shared_tag = reg_save.cacheline_info.tag - reg_save.instinfo.src + reg_save.instinfo.dst
        var shared_last_cp = reg_save.instinfo.dst + reg_save.instinfo.copysize
        //        var shared_last_src = reg_save.instinfo.dst + reg_save.instinfo.srcsize
        var current_addr = x.asUInt + shared_tag.asUInt
        //wordOffset1H_bytes(x) := 15.U(4.W)
        when(current_addr >= reg_save.instinfo.dst && current_addr < shared_last_cp) {
          wordOffset1H_bytes(x) := true.B
        }.otherwise {
          wordOffset1H_bytes(x) := false.B
        }
      }
      is(1.U) {
        wordOffset1H_bytes(x) := true.B
      }
      is(3.U) {
        wordOffset1H_bytes(x) := true.B
      }
    }
  })
  //set zero part
  val data_bytes = Wire(Vec(numgroupshared * dma_aligned_bulk, UInt(BitsOfByte.W)))
  data_bytes := reg_save.data.asTypeOf(data_bytes)
  val data_bytes_z = Wire(Vec(numgroupshared * dma_aligned_bulk, UInt(BitsOfByte.W)))
  (0 until (numgroupshared * dma_aligned_bulk)).foreach(x => {
    var shared_tag = reg_save.cacheline_info.tag - reg_save.instinfo.src.asUInt + reg_save.instinfo.dst.asUInt
    var shared_last_cp = reg_save.cacheline_info.tag - reg_save.instinfo.src.asUInt + reg_save.instinfo.dst.asUInt + reg_save.instinfo.copysize - 1.U
    var shared_last_src = reg_save.cacheline_info.tag - reg_save.instinfo.src.asUInt + reg_save.instinfo.dst.asUInt + reg_save.instinfo.srcsize - 1.U
    var current_addr = x.asUInt + shared_tag.asUInt
    //    val groupIndex = x / dma_aligned_bulk
    //    val byteIndex = x % dma_aligned_bulk
    //        when(current_addr > shared_last_src.asUInt && current_addr < shared_last_cp.asUInt) {
    when(current_addr > shared_last_src.asUInt && current_addr <= shared_last_cp.asUInt) {
      data_bytes_z(x) := 0.U(BitsOfByte.W)
      //      reg_save.data(groupIndex).asTypeOf(Vec(dma_aligned_bulk, UInt(BitsOfByte.W)))(byteIndex) := 0.U(BitsOfByte.W)
    }.otherwise {
      data_bytes_z(x) := data_bytes(x)
      //      reg_save.data(groupIndex).asTypeOf(Vec(dma_aligned_bulk, UInt(BitsOfByte.W)))(byteIndex) := data_bytes_z_s(x)
    }
  })
  //shift part
  val src_1 = Mux(reg_save.instinfo.src < reg_save.cacheline_info.tag, reg_save.cacheline_info.tag, reg_save.instinfo.src)
  val dst_1 = src_1 - reg_save.instinfo.src + reg_save.instinfo.dst
  val shiftbytes = Wire(UInt(2.W))
  shiftbytes := 0.U
  val high_margin = PriorityEncoder(reg_save.mask.reverse)
  val low_margin = PriorityEncoder(reg_save.mask)
  val data_bytes_z_s = Wire(Vec(numgroupshared * dma_aligned_bulk, UInt(BitsOfByte.W)))
  val wordOffset1H_bytes_s = Wire(Vec(numgroupshared * dma_aligned_bulk, Bool())) // will changed according to addr
  wordOffset1H_bytes_s := wordOffset1H_bytes
  data_bytes_z_s := reg_save.data.asTypeOf(Vec(numgroupshared * dma_aligned_bulk, UInt(BitsOfByte.W)))
  mask_bits := reg_save.mask.asUInt
  when(src_1(1,0).asUInt === dst_1(1,0).asUInt){
    // do not need to shift
//    data_bytes_z_s := reg_save.data.asTypeOf(Vec(numgroupshared * dma_aligned_bulk, UInt(BitsOfByte.W)))
    data_bytes_z_s := data_bytes_z
    mask_bits := reg_save.mask.asUInt
    l2cacheTag_shift := reg_save.cacheline_info.tag
    wordOffset1H_bytes_s := wordOffset1H_bytes
    //    wordoffset_bits := Cat(wordOffset1H)
  }.otherwise{

    when(src_1(1,0).asUInt > dst_1(1,0).asUInt){

      when(high_margin <= low_margin){
        // shift right
        shiftbytes := src_1(1,0).asUInt - dst_1(1,0).asUInt
        (0 until(dma_aligned_bulk * numgroupshared)).foreach( x => {
          when( x.asUInt + shiftbytes.asUInt < dma_aligned_bulk.asUInt * numgroupshared.asUInt){
            data_bytes_z_s(x) := data_bytes_z(x.asUInt + shiftbytes.asUInt)
          }.otherwise{
            data_bytes_z_s(x) := 0.U(BitsOfByte.W)
          }
        })
        wordOffset1H_bytes_s := (wordOffset1H_bytes.asUInt >> shiftbytes.asUInt).asTypeOf(wordOffset1H_bytes)
        mask_bits := reg_save.mask.asUInt
        l2cacheTag_shift := reg_save.cacheline_info.tag + (src_1(1,0).asUInt - dst_1(1,0).asUInt)
        //        data_bytes_z_s := Cat(reg_save.data) >> (src_1(1,0).asUInt - dst_1(1,0).asUInt) * BitsOfByte.asUInt
        //        wordoffset_bits := Cat(wordOffset1H)
      }.otherwise{
        //shift left
        shiftbytes := dma_aligned_bulk.asUInt - (src_1(1,0).asUInt - dst_1(1,0).asUInt)
        (0 until(dma_aligned_bulk * numgroupshared)).foreach( x => {
          when(x.asUInt - shiftbytes.asUInt > 0.U){
            data_bytes_z_s(x) := data_bytes_z(x.asUInt - shiftbytes.asUInt)
          }.otherwise{
            data_bytes_z_s(x) := 0.U(BitsOfByte.W)
          }
        })
        wordOffset1H_bytes_s := (wordOffset1H_bytes.asUInt << shiftbytes.asUInt).asTypeOf(wordOffset1H_bytes)
        mask_bits := reg_save.mask.asUInt << 1
        l2cacheTag_shift := reg_save.cacheline_info.tag - shiftbytes
        //        data_bytes_z_s := Cat(reg_save.data) << (dma_aligned_bulk.asUInt - dst_1(1,0).asUInt + src_1(1,0).asUInt) * BitsOfByte.asUInt
        //        mask_bits := mask_bits << 1
        //        wordoffset_bits := Cat(wordOffset1H)  <<  4.U
      }
    }.elsewhen(src_1(1,0).asUInt < dst_1(1,0).asUInt){
      when(high_margin <= low_margin){
        shiftbytes := dma_aligned_bulk.asUInt - (dst_1(1,0).asUInt - src_1(1,0).asUInt)
        (0 until(dma_aligned_bulk * numgroupshared)).foreach( x => {
          when(x.asUInt + shiftbytes < dma_aligned_bulk.asUInt * numgroupshared.asUInt){
            data_bytes_z_s(x) :=  data_bytes_z(x.asUInt + shiftbytes.asUInt)
          }.otherwise{
            data_bytes_z_s(x) := 0.U
          }
        })
        wordOffset1H_bytes_s := (wordOffset1H_bytes.asUInt >> shiftbytes.asUInt).asTypeOf(wordOffset1H_bytes)
        mask_bits := reg_save.mask.asUInt >> 1
        l2cacheTag_shift := reg_save.cacheline_info.tag + shiftbytes//(dma_aligned_bulk.asUInt-src_1(1,0).asUInt + dst_1(1,0).asUInt)
        //        wordoffset_bits := Cat(wordOffset1H)  >>  4.U
        //        data_bytes_z_s := Cat(reg_save.data) >> (dma_aligned_bulk.asUInt-src_1(1,0).asUInt + dst_1(1,0).asUInt) * BitsOfByte.asUInt
        //        mask_bits := mask_bits >> 1
      }.otherwise{
        shiftbytes := dst_1.asUInt - src_1.asUInt
        (0 until(dma_aligned_bulk * numgroupshared)).foreach( x => {
          when(x.asUInt - shiftbytes < 0.U){
            data_bytes_z_s(x) :=  data_bytes_z(x.asUInt - shiftbytes.asUInt)
          }.otherwise{
            data_bytes_z_s(x) := 0.U
          }
        })
        wordOffset1H_bytes_s := (wordOffset1H_bytes.asUInt << shiftbytes.asUInt).asTypeOf(wordOffset1H_bytes)
        mask_bits := reg_save.mask.asUInt
        l2cacheTag_shift := reg_save.cacheline_info.tag - shiftbytes
        //        data_bytes_z_s := Cat(reg_save.data) << (dst_1(1,0).asUInt - src_1(1,0).asUInt) * BitsOfByte.asUInt
        //        wordoffset_bits := Cat(wordOffset1H)
      }
    }
  }




  io.shared_req.bits := output_reg
  io.shared_req.valid := state===s_shared2

  io.from_temp.ready := state === s_idle
  var cnt_mask = 0
  // FSM State Transfer
  switch(state){
    is(s_idle){
      when(io.from_temp.fire){
        switch(io.from_temp.bits.instinfo.funct){
          is(0.U){
            state := s_shift
          }
          is(1.U){
            state := s_shared1
          }
          is(3.U){
            state := s_shared1
          }
        }
//        state := s_shared1
      }.otherwise{
        state := state
      }
      cnt.reset()
    }
    is(s_shift){
      state := s_shared1
    }
    is(s_shared1){
      when(io.shared_req.ready){
        state := s_shared2
      }.otherwise{
        state := s_shared1
      }
    }
    is(s_shared2){
      when(io.shared_req.fire){
        //        when(cnt.value >= current_numgroup || mask_next.asUInt === 0.U){
        when(mask_next.asUInt === 0.U){
          cnt.reset()
          state := s_idle
        }.otherwise{
          cnt.inc();state := s_shared1
        }
      }.otherwise{state := s_shared2}
    }
  }
  switch(state){
    is(s_idle){
      when(io.from_temp.fire){
        reg_save := io.from_temp.bits
        output_reg := RegInit(0.U.asTypeOf((new ShareMemCoreReq_np)))
//        current_numgroup := PopCount(io.from_temp.bits.mask)
      }.otherwise{
//        current_numgroup := 0.U(log2Ceil(numgroupshared).W)
        reg_save := RegInit(0.U.asTypeOf(new TempOutput))
        output_reg := RegInit(0.U.asTypeOf(new ShareMemCoreReq_np))
      }
    }
    is(s_shift){
      reg_save.data := data_bytes_z_s.asTypeOf(reg_save.data)
      reg_save.cacheline_info.tag := l2cacheTag_shift
      (0 until(numgroupshared)).foreach( x => {
        reg_save.mask(x) := mask_bits(x)
      })
    }
    is(s_shared1) {
      output_reg.instrId := reg_save.entry_index
      output_reg.isWrite := true.B
      output_reg.setIdx := setIdx
//      output_reg.dma := true.B
      switch(reg_save.instinfo.funct) {
        is(0.U) {
          (0 until (numgroupshared)).foreach(x => {
            output_reg.perLaneAddr(x).blockOffset := blockOffset(x)
            //            output_reg.perLaneAddr(x).wordOffset1H := wordOffset1H(x)
            output_reg.perLaneAddr(x).wordOffset1H := Cat(wordOffset1H_bytes_s(x * dma_aligned_bulk),
              wordOffset1H_bytes_s(x * dma_aligned_bulk + 1),
              wordOffset1H_bytes_s(x * dma_aligned_bulk + 2),
              wordOffset1H_bytes_s(x * dma_aligned_bulk + 3))
            output_reg.perLaneAddr(x).activeMask := mask_bits(x)
            output_reg.data(x) := Cat(data_bytes_z_s(x * dma_aligned_bulk),
              data_bytes_z_s(x * dma_aligned_bulk + 1),
              data_bytes_z_s(x * dma_aligned_bulk + 2),
              data_bytes_z_s(x * dma_aligned_bulk + 3))
          })
        }
        is(1.U) {
          (0 until (numgroupshared)).foreach(x => {
            output_reg.perLaneAddr(x).blockOffset := blockOffset(x)
            output_reg.perLaneAddr(x).wordOffset1H := Cat(wordOffset1H_bytes(x * dma_aligned_bulk),
              wordOffset1H_bytes(x * dma_aligned_bulk + 1),
              wordOffset1H_bytes(x * dma_aligned_bulk + 2),
              wordOffset1H_bytes(x * dma_aligned_bulk + 3))
            output_reg.perLaneAddr(x).activeMask := current_mask(x)
            output_reg.data(x) := reg_save.data(x)
          })
        }
        is(3.U) {
          (0 until (numgroupshared)).foreach(x => {
            output_reg.perLaneAddr(x).blockOffset := blockOffset(x)
            output_reg.perLaneAddr(x).wordOffset1H := Cat(wordOffset1H_bytes(x * dma_aligned_bulk),
              wordOffset1H_bytes(x * dma_aligned_bulk + 1),
              wordOffset1H_bytes(x * dma_aligned_bulk + 2),
              wordOffset1H_bytes(x * dma_aligned_bulk + 3))
            output_reg.perLaneAddr(x).activeMask := current_mask(x)
            output_reg.data(x) := reg_save.data(x)
          })
        }
      }
    }
    is(s_shared2){
      when(io.shared_req.fire){
        reg_save.mask := mask_next
        //        current_mask_index := RegInit(VecInit(Seq.fill(numgroupshared)(0.U(log2Ceil(numgroupshared).W))))
        output_reg := RegInit(0.U.asTypeOf((new ShareMemCoreReq_np)))

      }.otherwise{
        reg_save.mask := reg_save.mask
      }
    }
  }
}
class DMA_core(SV: Option[mmu.SVParam] = None)(implicit p: Parameters) extends Module{
  val io = IO(new Bundle{
    val dma_req = Flipped(DecoupledIO(new vExeData()))
    val dma_cache_rsp = Flipped(DecoupledIO(new DCacheMemRsp))
    val shared_rsp = Flipped(DecoupledIO(new DCacheCoreRsp_np))
    val dma_cache_req = DecoupledIO(new DCacheMemReq_p)
    val shared_req = DecoupledIO(new ShareMemCoreReq_np)
    val fence_end_dma = DecoupledIO(UInt(depth_warp.W))

    val TLBReq = Decoupled(new DmaTLBReq)
    val TLBRsp = Flipped(Decoupled(UInt(paLen.W)))
  })



  val InputFIFO = Module(new Queue(new vExeData, entries=1, pipe=true))
  InputFIFO.io.enq <> io.dma_req
  val addrCalc_l2cache = Module(new AddrCalc_l2cache)
  addrCalc_l2cache.io.from_fifo <> InputFIFO.io.deq
  io.dma_cache_req <> addrCalc_l2cache.io.to_l2cache
  io.TLBReq <> addrCalc_l2cache.io.to_l2TLB
  addrCalc_l2cache.io.from_l2TLB <> io.TLBRsp



  val tempmem = Module(new Temp_mem)
  addrCalc_l2cache.io.to_tempmem_tag <> tempmem.io.from_addr_tag
  addrCalc_l2cache.io.inst_mem_index := tempmem.io.inst_mem_index
  addrCalc_l2cache.io.tag_mem_index := tempmem.io.tag_mem_index
  tempmem.io.from_addr <> addrCalc_l2cache.io.to_tempmem_inst
  tempmem.io.from_addr_tag <> addrCalc_l2cache.io.to_tempmem_tag
  tempmem.io.from_l2cache <> io.dma_cache_rsp
  tempmem.io.from_shared <> io.shared_rsp
  io.fence_end_dma <> tempmem.io.inst_complete
  val addrCalc_shared = Module(new Addrcalc_shared)
  addrCalc_shared.io.from_temp <> tempmem.io.to_shared
  io.shared_req <> addrCalc_shared.io.shared_req
}