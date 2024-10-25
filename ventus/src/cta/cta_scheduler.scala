package cta

import chisel3._
import chisel3.util._
import top.parameters.{CTA_SCHE_CONFIG => CONFIG}

/** IO Bundle: CTA information
 *  Data producer: host
 *  Data consumer: allocator
 *
 *  Information which is related to resource occupancy
 *  should be passed to the allocator.
 */
trait ctainfo_host_to_alloc extends Bundle {
  // num_wg_slot = 1 constant, one wg always occupies one slot
  val num_sgpr = UInt(log2Ceil(CONFIG.WG.NUM_SGPR_MAX+1).W)   // Number of sgpr used by this cta
  val num_vgpr = UInt(log2Ceil(CONFIG.WG.NUM_VGPR_MAX+1).W)   // Number of vgpr used by this cta
  val num_lds = UInt(log2Ceil(CONFIG.WG.NUM_LDS_MAX+1).W)     // Number of Local Data Share used by this cta
}

/** IO Bundle: CTA information
 *  Data producer: host
 *  Data consumer: CU interface
 *
 *  Information which is related to splitting wg into wf
 */
trait ctainfo_host_to_cuinterface extends Bundle {
  val num_sgpr_per_wf = UInt(log2Ceil(CONFIG.WG.NUM_SGPR_MAX+1).W)      // Number of sgpr used by each wf in this wg
  val num_vgpr_per_wf = UInt(log2Ceil(CONFIG.WG.NUM_VGPR_MAX+1).W)      // Number of vgpr used by each wf in this wg
  val num_pds_per_wf = UInt(log2Ceil(CONFIG.WG.NUM_PDS_MAX+1).W)        // Number of pds  used by each wf in this wg
  val threadIdx_in_grid_offset_x = UInt(log2Ceil(CONFIG.KERNEL.NUM_THREAD_PER_KNL_MAX).W) // thread-Index-Global offset x
  val threadIdx_in_grid_offset_y = UInt(log2Ceil(CONFIG.KERNEL.NUM_THREAD_PER_KNL_MAX).W) // thread-Index-Global offset y
  val threadIdx_in_grid_offset_z = UInt(log2Ceil(CONFIG.KERNEL.NUM_THREAD_PER_KNL_MAX).W) // thread-Index-Global offset z
}

/** IO Bundle: CTA information
 *  Data producer: allocator
 *  Data consumer: CU interface
 *
 *  Information which is related to splitting wg into wf
 */
trait ctainfo_alloc_to_cuinterface_to_rt extends Bundle {
  val cu_id = UInt(log2Ceil(CONFIG.GPU.NUM_CU).W)
  val wg_slot_id = UInt(log2Ceil(CONFIG.GPU.NUM_WG_SLOT).W)
  val lds_dealloc_en = Bool()   // if LDS needs dealloc. When num_lds==0, lds do not need dealloc
  val sgpr_dealloc_en = Bool()
  val vgpr_dealloc_en = Bool()
}
trait ctainfo_alloc_to_cuinterface extends ctainfo_alloc_to_cuinterface_to_rt {
  // Global_Index_{x,y,z} of the first thread of this WG
  val threadIdx_in_grid_base_x = UInt(log2Ceil(CONFIG.KERNEL.NUM_THREAD_PER_KNL_MAX).W)
  val threadIdx_in_grid_base_y = UInt(log2Ceil(CONFIG.KERNEL.NUM_THREAD_PER_KNL_MAX).W)
  val threadIdx_in_grid_base_z = UInt(log2Ceil(CONFIG.KERNEL.NUM_THREAD_PER_KNL_MAX).W)
  // num_thread_per_grid_x,    num_thread_per_grid_x * num_thread_per_grid_y
  val num_thread_per_grid_x = UInt(log2Ceil(CONFIG.WG.NUM_THREAD_PER_WG_MAX+1).W)
  val num_thread_per_grid_xy = UInt(log2Ceil(CONFIG.WG.NUM_THREAD_PER_WG_MAX+1).W)
}

/** IO Bundle: CTA information
 *  Data producer: allocator
 *  Data consumer: CU
 *
 *  Information which is related to code execution
 *  These are **WG** sgpr/vgpr/lds base address
 *  CU-interface may update them into **WF** sgpr/vgpr/lds base address
 */
trait ctainfo_alloc_to_cu extends Bundle {
  val sgpr_base = UInt(log2Ceil(CONFIG.WG.NUM_SGPR_MAX).W)            // sgpr base address (initial WG, later WF)
  val vgpr_base = UInt(log2Ceil(CONFIG.WG.NUM_VGPR_MAX).W)            // vgpr base address (initial WG, later WF)
  val lds_base = UInt(log2Ceil(CONFIG.WG.NUM_LDS_MAX).W)              // lds  base address (initial WG, later WF)
}

/** IO Bundle: CTA information
 *  Data producer: host
 *  Data consumer: allocator & CU interface & CU
 *
 */
trait ctainfo_host_to_alloc_to_cu extends Bundle {
  val num_wf = UInt(log2Ceil(CONFIG.WG.NUM_WF_MAX+1).W)       // Number of wavefront in this cta
  val wgIdx_x = UInt(log2Ceil(CONFIG.KERNEL.NUM_WG_MAX).W)    // thread-block index in grid - x dimension
  val wgIdx_y = UInt(log2Ceil(CONFIG.KERNEL.NUM_WG_MAX).W)    // thread-block index in grid - y dimension
  val wgIdx_z = UInt(log2Ceil(CONFIG.KERNEL.NUM_WG_MAX).W)    // thread-block index in grid - z dimension
  val num_wg_x = UInt(log2Ceil(CONFIG.KERNEL.NUM_WG_MAX+1).W) // Number of wg in x-dimension in this kernel
  val num_wg_y = UInt(log2Ceil(CONFIG.KERNEL.NUM_WG_MAX+1).W) // Number of wg in y-dimension in this kernel
  val num_wg_z = UInt(log2Ceil(CONFIG.KERNEL.NUM_WG_MAX+1).W) // Number of wg in z-dimension in this kernel
  val num_thread_per_wg_x = UInt(log2Ceil(CONFIG.WG.NUM_THREAD_PER_WG_MAX+1).W)
  val num_thread_per_wg_y = UInt(log2Ceil(CONFIG.WG.NUM_THREAD_PER_WG_MAX+1).W)
  val num_thread_per_wg_z = UInt(log2Ceil(CONFIG.WG.NUM_THREAD_PER_WG_MAX+1).W)
}

/** IO Bundle: CTA information
 *  Data producer: host
 *  Data consumer: CU
 *
 *  Information which is related code execution
 *  It will be passed on to CU later
 *  Some Information may be updated by CU-interface during splitting wg into wf
 */
trait ctainfo_host_to_cu extends Bundle {
  val num_thread_per_wf = UInt(log2Ceil(CONFIG.GPU.NUM_THREAD+1).W)   // Number of thread in each wf
  //val num_gds = UInt(log2Ceil(CONFIG.WG.NUM_GDS_MAX+1).W)           // Number of Global Data Share used by this WG
  val gds_base = UInt(CONFIG.GPU.MEM_ADDR_WIDTH)                      // GDS base address of this WG
  val pds_base = UInt(CONFIG.GPU.MEM_ADDR_WIDTH)                      // PDS base addr of this WG, convert to WF base addr in CUinterface
  val start_pc = UInt(CONFIG.GPU.MEM_ADDR_WIDTH)                      // Program start pc address
  val csr_kernel = UInt(CONFIG.GPU.MEM_ADDR_WIDTH)                    // Meta-data base address
  val asid_kernel = if(CONFIG.GPU.MMU_ENABLE) Some(UInt(CONFIG.GPU.ASID_WIDTH)) else None // Virtual memory space ID
}

/** IO between CU-interface and CU
 */
class io_cuinterface2cu extends Bundle with ctainfo_host_to_cu with ctainfo_alloc_to_cu with ctainfo_host_to_alloc_to_cu {
  val wg_id = UInt(CONFIG.WG.WG_ID_WIDTH)
  val wf_tag = UInt(CONFIG.WG.WF_TAG_WIDTH)
  val threadIdx_in_wg_x = Vec(CONFIG.GPU.NUM_THREAD, UInt(log2Ceil(CONFIG.WG.NUM_THREAD_PER_WG_MAX).W))
  val threadIdx_in_wg_y = Vec(CONFIG.GPU.NUM_THREAD, UInt(log2Ceil(CONFIG.WG.NUM_THREAD_PER_WG_MAX).W))
  val threadIdx_in_wg_z = Vec(CONFIG.GPU.NUM_THREAD, UInt(log2Ceil(CONFIG.WG.NUM_THREAD_PER_WG_MAX).W))
  val threadIdx_in_wg   = Vec(CONFIG.GPU.NUM_THREAD, UInt(log2Ceil(CONFIG.WG.NUM_THREAD_PER_WG_MAX).W))
  val threadIdx_in_grid_x = Vec(CONFIG.GPU.NUM_THREAD, UInt(log2Ceil(CONFIG.KERNEL.NUM_THREAD_PER_KNL_MAX).W))
  val threadIdx_in_grid_y = Vec(CONFIG.GPU.NUM_THREAD, UInt(log2Ceil(CONFIG.KERNEL.NUM_THREAD_PER_KNL_MAX).W))
  val threadIdx_in_grid_z = Vec(CONFIG.GPU.NUM_THREAD, UInt(log2Ceil(CONFIG.KERNEL.NUM_THREAD_PER_KNL_MAX).W))
  val threadIdx_in_grid   = Vec(CONFIG.GPU.NUM_THREAD, UInt(log2Ceil(CONFIG.KERNEL.NUM_THREAD_PER_KNL_MAX).W))
}
class io_cu2cuinterface extends Bundle {
  val wf_tag = UInt(CONFIG.WG.WF_TAG_WIDTH)
  //val wg_id = if(CONFIG.DEBUG) Some(UInt(CONFIG.WG.WG_ID_WIDTH)) else None
}

/** IO between host and wg-buffer
 */
class io_host2cta extends Bundle with ctainfo_host_to_alloc with ctainfo_host_to_cuinterface with ctainfo_host_to_cu with ctainfo_host_to_alloc_to_cu {
  val wg_id = UInt(CONFIG.WG.WG_ID_WIDTH)
}
class io_cta2host extends Bundle {
  val wg_id = UInt(CONFIG.WG.WG_ID_WIDTH)
  val cu_id = UInt(log2Ceil(CONFIG.GPU.NUM_CU).W)   // For CTA schedule strategy research
}

class cta_scheduler_top(val NUM_CU: Int = CONFIG.GPU.NUM_CU) extends Module {
  val io = IO(new Bundle{
    val host_wg_new = Flipped(DecoupledIO(new io_host2cta))     // From Host, New wg info
    val host_wg_done = DecoupledIO(new io_cta2host)             // To host, ID of wg which finished its execution

    // From CU(i), tag of wf which finished its execution
    val cu_wf_done = Vec(NUM_CU, Flipped(DecoupledIO(new io_cu2cuinterface)))
    // To CU(i), new wf info
    val cu_wf_new = Vec(NUM_CU, DecoupledIO(new io_cuinterface2cu))
  })

  val wg_buffer_inst = Module(new wg_buffer)
  val allocator_inst = Module(new allocator)
  val resource_table_inst = Module(new resource_table_top)
  val cu_interface_inst = Module(new cu_interface)

  val init_ok = cu_interface_inst.io.init_ok
  wg_buffer_inst.io.host_wg_new.valid := io.host_wg_new.valid && init_ok
  io.host_wg_new.ready := wg_buffer_inst.io.host_wg_new.ready && init_ok
  io.host_wg_new.bits <> wg_buffer_inst.io.host_wg_new.bits

  wg_buffer_inst.io.alloc_wg_new <> allocator_inst.io.wgbuffer_wg_new
  allocator_inst.io.wgbuffer_result <> wg_buffer_inst.io.alloc_result
  wg_buffer_inst.io.cuinterface_wg_new <> cu_interface_inst.io.wgbuffer_wg_new
  allocator_inst.io.cuinterface_wg_new <> cu_interface_inst.io.alloc_wg_new
  cu_interface_inst.io.host_wg_done <> io.host_wg_done

  allocator_inst.io.rt_alloc <> resource_table_inst.io.alloc
  allocator_inst.io.rt_result_lds <> resource_table_inst.io.rtcache_lds
  allocator_inst.io.rt_result_sgpr <> resource_table_inst.io.rtcache_sgpr
  allocator_inst.io.rt_result_vgpr <> resource_table_inst.io.rtcache_vgpr
  resource_table_inst.io.dealloc <> cu_interface_inst.io.rt_dealloc
  resource_table_inst.io.cuinterface_wg_new <> cu_interface_inst.io.rt_wg_new
  allocator_inst.io.rt_dealloc <> resource_table_inst.io.slot_dealloc

  for(i <- 0 until NUM_CU) {
    io.cu_wf_new(i) <> cu_interface_inst.io.cu_wf_new(i)
    io.cu_wf_done(i) <> cu_interface_inst.io.cu_wf_done(i)
  }
}

object emitVerilog extends App {
  chisel3.emitVerilog(
    //new cta_scheduler_top(CONFIG.GPU.NUM_CU),
    new allocator(),
    //new cu_interface(),
    Array("--target-dir", "generated/")
  )
}
