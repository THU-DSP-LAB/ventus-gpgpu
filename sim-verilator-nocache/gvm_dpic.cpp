// DPI-C 函数实现

#include <vector>
#include <cstdint>
#include <array>
#include "gvm_global_var.hpp"
#include "gvm_dpic.hpp"
#include <cassert>

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
                       int rtl_num_thread) {
  Cta2WarpData d;
  d.software_wg_id     = software_wg_id;
  d.software_warp_id   = software_warp_id;
  d.sm_id              = sm_id;
  d.hardware_warp_id   = hardware_warp_id;
  d.sgpr_base          = sgpr_base;
  d.vgpr_base          = vgpr_base;
  d.wg_slot_id_in_warp_sche = wg_slot_id_in_warp_sche;
  d.lds_base           = static_cast<uint32_t>(lds_base);
  d.num_thread_in_warp = rtl_num_thread;
  g_cta2warp_data.push_back(d);
}

// Insn Dispatch
void c_GvmDutInsnDispatch(int sm_id,
                            int hardware_warp_id,
                            int pc,
                            int instr,
                            int dispatch_id,
                            bool is_extended) {
  InsnDispatchData d;
  d.sm_id             = sm_id;
  d.hardware_warp_id  = hardware_warp_id;
  d.pc                = pc;
  d.insn             = instr;
  d.dispatch_id       = dispatch_id;
  d.is_extended      = is_extended;
  g_insn_dispatch_data.push_back(d);
}

// XReg Writeback
void c_GvmDutXRegWriteback(int sm_id,
                            int rd,
                            bool is_scalar_wb,
                            int reg_idx,
                            int hardware_warp_id,
                            int pc,
                            int inst,
                            int dispatch_id) {
  XRegWritebackData d;
  d.sm_id             = sm_id;
  d.rd                = rd;
  d.is_scalar_wb      = is_scalar_wb;
  d.reg_idx           = reg_idx;
  d.hardware_warp_id  = hardware_warp_id;
  d.pc                = pc;
  d.insn              = inst;
  d.dispatch_id       = dispatch_id;
  g_xreg_wb_data.push_back(d);
}

// New warp XRegs
void c_GvmDutWarpXRegInit(int sm_id,
                           int hardware_warp_id,
                           int xreg_word,
                           int xreg_word_idx) {
  WarpXRegInitData* target = nullptr;
  for (auto& item : g_warp_xreg_init_data) {
    if (item.sm_id == static_cast<uint32_t>(sm_id)
        && item.hardware_warp_id == static_cast<uint32_t>(hardware_warp_id)) {
      target = &item;
      break;
    }
  }
  if (target == nullptr) {
    g_warp_xreg_init_data.push_back({static_cast<uint32_t>(sm_id), static_cast<uint32_t>(hardware_warp_id), {}});
    target = &g_warp_xreg_init_data.back();
  }
  if (target->xreg_data.size() <= static_cast<size_t>(xreg_word_idx)) {
    target->xreg_data.resize(static_cast<size_t>(xreg_word_idx) + 1);
  }
  target->xreg_data[static_cast<size_t>(xreg_word_idx)] = static_cast<uint32_t>(xreg_word);
}

// New warp VRegs
void c_GvmDutWarpVRegInit(int sm_id,
                           int hardware_warp_id,
                           int vreg_word,
                           int vreg_word_idx,
                           int thread_idx) {
  WarpVRegInitData* target = nullptr;
  for (auto& item : g_warp_vreg_init_data) {
    if (item.sm_id == static_cast<uint32_t>(sm_id)
        && item.hardware_warp_id == static_cast<uint32_t>(hardware_warp_id)) {
      target = &item;
      break;
    }
  }
  if (target == nullptr) {
    g_warp_vreg_init_data.push_back({static_cast<uint32_t>(sm_id), static_cast<uint32_t>(hardware_warp_id), {}});
    target = &g_warp_vreg_init_data.back();
  }
  if (target->vreg_data.size() <= static_cast<size_t>(vreg_word_idx)) {
    target->vreg_data.resize(static_cast<size_t>(vreg_word_idx) + 1);
  }
  if (target->vreg_data[static_cast<size_t>(vreg_word_idx)].size() <= static_cast<size_t>(thread_idx)) {
    target->vreg_data[static_cast<size_t>(vreg_word_idx)].resize(static_cast<size_t>(thread_idx) + 1);
  }
  target->vreg_data[static_cast<size_t>(vreg_word_idx)][static_cast<size_t>(thread_idx)] = static_cast<uint32_t>(vreg_word);
}

// VReg Writeback
void c_GvmDutVRegWriteback(int sm_id,
                            int rd_data,    
                            bool is_vector_wb,
                            int reg_idx,
                            int hardware_warp_id,
                            int pc,
                            int inst,
                            int dispatch_id,
                            bool wvd_mask,   
                            int thread_idx) { 
  g_vreg_wb_data[{sm_id, hardware_warp_id, dispatch_id}].sm_id = sm_id;
  g_vreg_wb_data[{sm_id, hardware_warp_id, dispatch_id}].rd_data[thread_idx] = rd_data;
  g_vreg_wb_data[{sm_id, hardware_warp_id, dispatch_id}].is_vector_wb = is_vector_wb;
  g_vreg_wb_data[{sm_id, hardware_warp_id, dispatch_id}].reg_idx = reg_idx;
  g_vreg_wb_data[{sm_id, hardware_warp_id, dispatch_id}].hardware_warp_id = hardware_warp_id;
  g_vreg_wb_data[{sm_id, hardware_warp_id, dispatch_id}].pc = pc;
  g_vreg_wb_data[{sm_id, hardware_warp_id, dispatch_id}].insn = inst;
  g_vreg_wb_data[{sm_id, hardware_warp_id, dispatch_id}].dispatch_id = dispatch_id;
  g_vreg_wb_data[{sm_id, hardware_warp_id, dispatch_id}].wvd_mask[thread_idx] = wvd_mask;
}

// Barrier done
void c_GvmDutBarrierDone(int sm_id,
                          int hardware_warp_id,
                          int pc,
                          int inst,
                          int dispatch_id) {
  BarDoneData d;
  d.sm_id             = sm_id;
  d.wg_slot_id        = hardware_warp_id;
  d.pc                = pc;
  d.insn              = inst;
  d.dispatch_id       = dispatch_id;
  g_bar_done_data.push_back(d);
}

} // extern "C"
