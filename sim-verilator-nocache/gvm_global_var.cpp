#include "gvm_global_var.hpp"
#include <vector>
#include <cstdint>
#include <array>
#include <tuple>
#include <map>

std::vector<Cta2WarpData> g_cta2warp_data;
std::vector<InsnDispatchData> g_insn_dispatch_data;
std::vector<XRegWritebackData> g_xreg_wb_data;
std::vector<WarpXRegInitData> g_warp_xreg_init_data;
std::vector<WarpVRegInitData> g_warp_vreg_init_data;
uint32_t g_sgprUsage = 64;
uint32_t g_vgprUsage = 128;
std::map<std::tuple<uint32_t,uint32_t,uint32_t>, VRegWritebackData> g_vreg_wb_data;
std::vector<BarDoneData> g_bar_done_data;

void gvm_clear_global_trace_buffers() {
  g_cta2warp_data.clear();
  g_insn_dispatch_data.clear();
  g_xreg_wb_data.clear();
  g_warp_xreg_init_data.clear();
  g_warp_vreg_init_data.clear();
  g_vreg_wb_data.clear();
  g_bar_done_data.clear();
}
