package cta

import chisel3._
import chisel3.util._

object CONFIG {
  import top.parameters
  object GPU {
    val NUM_CU = parameters.num_sm
    val MEM_ADDR_WIDTH = parameters.MEM_ADDR_WIDTH.W
    val NUM_WG_SLOT = parameters.num_block                  // Number of WG slot in each CU
    val NUM_WF_SLOT = parameters.num_warp                   // Number of WG slot in each CU
    val ASID_WIDTH = 32.W
  }
  object WG {
    val WG_ID_WIDTH = 32.W
    val NUM_WG_DIM_MAX = parameters.NUM_WG_X                // Max number of wg in a single dimension in each kernel
    val NUM_THREAD_MAX = 1 << parameters.WAVE_ITEM_WIDTH    // Max number of thread in each wavefront(warp)
    val NUM_WF_MAX = parameters.num_warp_in_a_block         // Max number of wavefront in each workgroup(block)
    val NUM_LDS_MAX = parameters.NUMBER_LDS_SLOTS           // Max number of LDS  occupied by a workgroup
    val NUM_SGPR_MAX = parameters.num_sgpr                  // Max number of sgpr occupied by a workgroup
    val NUM_VGPR_MAX = parameters.num_vgpr                  // Max number of vgpr occupied by a workgroup
    //val NUM_GDS_MAX = 1024                                // Max number of GDS  occupied by a workgroup, useless

    // WF tag = cat(wg_slot_id_in_cu, wf_id_in_wg)
    val WF_TAG_WIDTH = (log2Ceil(GPU.NUM_WG_SLOT) + log2Ceil(NUM_WF_MAX)).W
  }
  object WG_BUFFER {
    val NUM_ENTRIES = 16
  }
  object RESOURCE_TABLE {
    val NUM_RESULT = 2
  }
  val DEBUG = true
}

/** IO Bundle: CTA information
 *  Data producer: host
 *  Data consumer: allocator
 *
 *  Information which is related to resource occupancy
 *  should be passed to the allocator.
 */
trait ctainfo_host_to_alloc extends Bundle {
  // num_wg_slot = 1 constant, one wg always occupies one slot
  val num_wf = UInt(log2Ceil(CONFIG.WG.NUM_WF_MAX+1).W)       // Number of wavefront in this cta
  val num_sgpr = UInt(log2Ceil(CONFIG.WG.NUM_SGPR_MAX+1).W)   // Number of sgpr used by this cta
  val num_vgpr = UInt(log2Ceil(CONFIG.WG.NUM_VGPR_MAX+1).W)   // Number of vgpr used by this cta
  val num_lds = UInt(log2Ceil(CONFIG.WG.NUM_LDS_MAX+1).W)     // Number of Local  Data Share used by this cta
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
}

/** IO Bundle: CTA information
 *  Data producer: allocator
 *  Data consumer: CU interface
 *
 *  Information which is related to splitting wg into wf
 */
trait ctainfo_alloc_to_cuinterface extends Bundle {
  val cu_id = UInt(log2Ceil(CONFIG.GPU.NUM_CU).W)
  val wg_slot_id = UInt(log2Ceil(CONFIG.GPU.NUM_WG_SLOT).W)
  val num_wf = UInt(log2Ceil(CONFIG.WG.NUM_WF_MAX+1).W)                 // Number of wavefront in this cta
  val lds_dealloc_en = Bool()   // if LDS needs dealloc. When num_lds==0, lds do not need dealloc
  val sgpr_dealloc_en = Bool()
  val vgpr_dealloc_en = Bool()
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
  val lds_base = UInt(log2Ceil(CONFIG.WG.NUM_LDS_MAX).W)             // lds  base address (initial WG, later WF)
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
  val num_thread_per_wf = UInt(log2Ceil(CONFIG.WG.NUM_THREAD_MAX+1).W)// Number of thread in each wf
  //val num_gds = UInt(log2Ceil(CONFIG.WG.NUM_GDS_MAX+1).W)           // Number of Global Data Share used by this cta
  val gds_base = UInt(CONFIG.GPU.MEM_ADDR_WIDTH)                      // GDS base address of this cta
  val pds_base = UInt(CONFIG.GPU.MEM_ADDR_WIDTH)                      // PDS base address of this cta
  val start_pc = UInt(CONFIG.GPU.MEM_ADDR_WIDTH)                      // Program start pc address
  val csr_kernel = UInt(CONFIG.GPU.MEM_ADDR_WIDTH)                    // Meta-data base address
  val num_wg_x = UInt(log2Ceil(CONFIG.WG.NUM_WG_DIM_MAX+1).W)         // Number of wg in x-dimension in this kernel
  val num_wg_y = UInt(log2Ceil(CONFIG.WG.NUM_WG_DIM_MAX+1).W)         // Number of wg in y-dimension in this kernel
  val num_wg_z = UInt(log2Ceil(CONFIG.WG.NUM_WG_DIM_MAX+1).W)         // Number of wg in z-dimension in this kernel
  val asid_kernel = UInt(CONFIG.GPU.ASID_WIDTH)                       // Virtual memory space ID
}

/** IO between CU-interface and CU
 */
class io_cuinterface2cu extends Bundle with ctainfo_host_to_cu with ctainfo_alloc_to_cu {
  val wg_id = UInt(CONFIG.WG.WG_ID_WIDTH)
  val wf_tag = UInt(CONFIG.WG.WF_TAG_WIDTH)
  val num_wf = UInt(log2Ceil(CONFIG.WG.NUM_WF_MAX+1).W)                 // Number of wavefront in this cta
}
class io_cu2cuinterface extends Bundle {
  val wf_tag = UInt(CONFIG.WG.WF_TAG_WIDTH)
  //val wg_id = if(CONFIG.DEBUG) Some(UInt(CONFIG.WG.WG_ID_WIDTH)) else None
}

/** IO between host and wg-buffer
 */
class io_host2cta extends Bundle with ctainfo_host_to_alloc with ctainfo_host_to_cuinterface with ctainfo_host_to_cu {
  val wg_id = UInt(CONFIG.WG.WG_ID_WIDTH)
}
//class io_host2cta extends Bundle{
//  val data = UInt(8.W)
//}
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
    new cta_scheduler_top(CONFIG.GPU.NUM_CU),
    //new cta_util.RRPriorityEncoder(4),
    //new wg_buffer(),
    //new allocator(),
    Array("--target-dir", "generated/")
  )
}
