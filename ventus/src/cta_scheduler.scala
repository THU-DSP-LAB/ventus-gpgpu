package cta_scheduler

import chisel3._
import chisel3.util._

object CONFIG {
  object GPU{
    val NUM_CU = 2
    val MEM_ADDR_WIDTH = 32.W
    val NUM_WG_SLOT = 8             // Number of WG slot in each CU
  }
  object WG{
    val WG_ID_WIDTH = 32.W
    val NUM_WG_DIM_MAX = 1024       // Max number of wg in a single dimension in each kernel
    val NUM_THREAD_MAX = 32         // Max number of thread in each wavefront(warp)
    val NUM_WF_MAX = 32             // Max number of wavefront in each workgroup(block)
    val NUM_SGPR_MAX = 1024         // Max number of sgpr occupied by a workgroup
    val NUM_VGPR_MAX = 1024         // Max number of vgpr occupied by a workgroup
    val NUM_GDS_MAX = 1024          // Max number of GDS  occupied by a workgroup
    val NUM_LDS_MAX = 1024          // Max number of LDS  occupied by a workgroup

    // WF tag = cat(wg_slot_id_in_cu, wf_id_in_wg)
    val WF_TAG_WIDTH = (log2Ceil(GPU.NUM_WG_SLOT) + log2Ceil(NUM_WF_MAX)).W
  }
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
  val num_wf = UInt(log2Ceil(CONFIG.WG.NUM_WF_MAX).W)       // Number of wavefront in this cta
  val num_sgpr = UInt(log2Ceil(CONFIG.WG.NUM_SGPR_MAX).W)   // Number of sgpr used by this cta
  val num_vpgr = UInt(log2Ceil(CONFIG.WG.NUM_VGPR_MAX).W)   // Number of vpgr used by this cta
  val num_lds = UInt(log2Ceil(CONFIG.WG.NUM_LDS_MAX).W)     // Number of Local  Data Share used by this cta
}

/** IO Bundle: CTA information
 *  Data producer: host
 *  Data consumer: CU interface
 *
 *  Information which is related to splitting wg into wf
 */
trait ctainfo_host_to_cuinterface extends Bundle {
  val num_sgpr_per_wf = UInt(log2Ceil(CONFIG.WG.NUM_SGPR_MAX).W)      // Number of sgpr used by each wf in this wg
  val num_vpgr_per_wf = UInt(log2Ceil(CONFIG.WG.NUM_VGPR_MAX).W)      // Number of vpgr used by each wf in this wg
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
  val num_wf = UInt(log2Ceil(CONFIG.WG.NUM_WF_MAX).W)                 // Number of wavefront in this cta
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
  val lds_base = UInt(log2Ceil(CONFIG.WG.NUM_SGPR_MAX).W)             // lds  base address (initial WG, later WF)
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
  val num_thread_per_wf = UInt(log2Ceil(CONFIG.WG.NUM_THREAD_MAX).W)  // Number of thread in each wf
  val num_gds = UInt(log2Ceil(CONFIG.WG.NUM_GDS_MAX).W)               // Number of Global Data Share used by this cta
  val gds_base = UInt(CONFIG.GPU.MEM_ADDR_WIDTH)                      // GDS base address of this cta
  val pds_base = UInt(CONFIG.GPU.MEM_ADDR_WIDTH)                      // PDS base address of this cta
  val start_pc = UInt(CONFIG.GPU.MEM_ADDR_WIDTH)                      // Program start pc address
  val csr_kernel = UInt(CONFIG.GPU.MEM_ADDR_WIDTH)                    // Meta-data base address
  val num_wg_x = UInt(log2Ceil(CONFIG.WG.NUM_WG_DIM_MAX).W)           // Number of wg in x-dimension in this kernel
  val num_wg_y = UInt(log2Ceil(CONFIG.WG.NUM_WG_DIM_MAX).W)           // Number of wg in y-dimension in this kernel
  val num_wg_z = UInt(log2Ceil(CONFIG.WG.NUM_WG_DIM_MAX).W)           // Number of wg in z-dimension in this kernel
}

/** IO between CU-interface and CU
 */
class io_cuinterface2cu extends Bundle with ctainfo_host_to_cu with ctainfo_alloc_to_cu {
  val wg_id = UInt(CONFIG.WG.WG_ID_WIDTH)
  val wf_tag = UInt(CONFIG.WG.WF_TAG_WIDTH)
}
class io_cu2cuinterface extends Bundle {
  val wf_tag = UInt(CONFIG.WG.WF_TAG_WIDTH)
}

/** IO between host and wg-buffer
 */
class io_host2cta extends Bundle with ctainfo_host_to_alloc with ctainfo_host_to_cuinterface with ctainfo_host_to_cu
class io_cta2host extends Bundle {
  val wg_id = UInt(CONFIG.WG.WG_ID_WIDTH)
}

class cta_scheduler(val NUM_CU: Int = CONFIG.GPU.NUM_CU) extends Module {
  val io = IO(new Bundle{
    val host_wg_new = Flipped(DecoupledIO(new io_host2cta))     // From Host, New wg info
    val host_wg_done = DecoupledIO(new io_cta2host)             // To host, ID of wg which finished its execution

    // From CU(i), tag of wf which finished its execution
    val cu_wf_done = Vec(NUM_CU, Flipped(DecoupledIO(new io_cu2cuinterface)))
    // To CU(i), new wf info
    val cu_wf_new = Vec(NUM_CU, DecoupledIO(new io_cuinterface2cu))
  })

  io.host_wg_new.ready := false.B
  io.host_wg_done.valid := false.B
  io.host_wg_done.bits.wg_id := 0.U(CONFIG.WG.WG_ID_WIDTH)

  for(i <- 0 until NUM_CU){
    io.cu_wf_new(i).valid := false.B
    io.cu_wf_new(i).bits := 0.U.asTypeOf(new io_cuinterface2cu)
    io.cu_wf_done(i).ready := false.B
  }
}

object runner extends App {
  (new chisel3.stage.ChiselStage).emitVerilog(
    new cta_scheduler(CONFIG.GPU.NUM_CU),
    Array("--target-dir", "generated/")
  )
}
