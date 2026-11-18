// RTL 信号经由 DPI-C 接口，写入 C++ 全局变量

#pragma once

#include <vector>
#include <cstdint>
#include <tuple>
#include <map>
#include <array>

// CTA -> Warp 分配
struct Cta2WarpData {
  uint32_t software_wg_id;
  uint32_t software_warp_id;
  uint32_t sm_id;
  uint32_t hardware_warp_id;
  uint32_t sgpr_base;
  uint32_t vgpr_base;
  uint32_t wg_slot_id_in_warp_sche;
  uint32_t lds_base;
  uint32_t num_thread_in_warp;
};
extern std::vector<Cta2WarpData> g_cta2warp_data;
// Instr Dispatch
// at each cycle, the dut will push the dispatched instruction to this vector
// the gvm top module will read this vector and compare with the ref model,
// and clear the vector after each cycle
struct InsnDispatchData {
  uint32_t sm_id;
  uint32_t hardware_warp_id;
  uint32_t pc;
  uint32_t insn;
  uint32_t dispatch_id;
  bool is_extended;
};
extern std::vector<InsnDispatchData> g_insn_dispatch_data;
// XReg Writeback
struct XRegWritebackData {
  uint32_t sm_id;
  uint32_t rd; // 写回数据
  bool is_scalar_wb;
  uint32_t reg_idx; // 写回地址
  uint32_t hardware_warp_id;
  uint32_t pc;
  uint32_t insn;
  uint32_t dispatch_id;
};
extern std::vector<XRegWritebackData> g_xreg_wb_data;
struct WarpXRegInitData {
  uint32_t sm_id;
  uint32_t hardware_warp_id;
  std::vector<uint32_t> xreg_data;
};
extern std::vector<WarpXRegInitData> g_warp_xreg_init_data;
struct WarpVRegInitData {
  uint32_t sm_id;
  uint32_t hardware_warp_id;
  std::vector<std::vector<uint32_t>> vreg_data;
};
extern std::vector<WarpVRegInitData> g_warp_vreg_init_data;
extern uint32_t g_sgprUsage; // num of sgpr used in one warp
extern uint32_t g_vgprUsage; // num of vgpr used in one warp

// VReg Writeback
struct VRegWritebackData {
  uint32_t sm_id;
  std::array<uint32_t, 32> rd_data; // 向量写回数据，32个线程
  bool is_vector_wb;
  uint32_t reg_idx; // 写回地址
  uint32_t hardware_warp_id;
  uint32_t pc;
  uint32_t insn;
  uint32_t dispatch_id;
  std::array<bool, 32> wvd_mask; // 向量写回掩码，32个线程
};
extern std::map<std::tuple<uint32_t,uint32_t,uint32_t>, VRegWritebackData> g_vreg_wb_data;

// Barrier
struct BarDoneData {
  uint32_t sm_id;
  uint32_t wg_slot_id;
  uint32_t pc;
  uint32_t insn;
  uint32_t dispatch_id;
};
extern std::vector<BarDoneData> g_bar_done_data;

void gvm_clear_global_trace_buffers();
