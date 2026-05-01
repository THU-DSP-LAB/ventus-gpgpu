// DPI-C 函数头文件

#pragma once

#include <vector>
#include <cstdint>
#include "gvm_global_var.hpp"

extern "C" {
// CTA -> Warp 分配
void c_GvmDutCta2Warp(int software_wg_id,
                       int software_warp_id,
                       int sm_id,
                       int hardware_warp_id,
                       int sgpr_base,
                       int vgpr_base,
                       int wg_slot_id_in_warp_sche,
                       int lds_base,
                       int rtl_num_thread);
// Insn Dispatch
void c_GvmDutInsnDispatch(int sm_id,
                            int hardware_warp_id,
                            int pc,
                            int instr,
                            int dispatch_id,
                            bool is_extended);
// XReg Writeback
void c_GvmDutXRegWriteback(int sm_id,
                            int rd,
                            bool is_scalar_wb,
                            int reg_idx,
                            int hardware_warp_id,
                            int pc,
                            int inst,
                            int dispatch_id);
// New warp XReg snapshot
void c_GvmDutWarpXRegInit(int sm_id,
                           int hardware_warp_id,
                           int xreg_word,
                           int xreg_word_idx);
// New warp VReg snapshot
void c_GvmDutWarpVRegInit(int sm_id,
                           int hardware_warp_id,
                           int vreg_word,
                           int vreg_word_idx,
                           int thread_idx);
// VReg Writeback  
void c_GvmDutVRegWriteback(int sm_id,
                            int rd_data,     // 单个线程的向量数据
                            bool is_vector_wb,
                            int reg_idx,
                            int hardware_warp_id,
                            int pc,
                            int inst,
                            int dispatch_id,
                            bool wvd_mask,   // 单个线程的写回掩码
                            int thread_idx); // 线程索引
// Barrier done
void c_GvmDutBarrierDone(int sm_id,
                          int hardware_warp_id,
                          int pc,
                          int inst,
                          int dispatch_id);
} // extern "C"
