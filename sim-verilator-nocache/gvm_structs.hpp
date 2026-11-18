// GVM 核心类用到的结构体

#pragma once

#include <cstdint>
#include <map>
#include <vector>
#include <unordered_set>
#include <memory>
#include <spdlog/logger.h>

#include "../../spike/gvmref/gvmref_interface.h"
#include "gvm_global_var.hpp"

struct care_insn_t {
  uint32_t mask;
  uint32_t value;
  const char* name;
};

enum class InsnType {
  DONT_CARE, // 其他指令
  XREG, // 写回标量寄存器的指令
  VREG // 写回向量寄存器的指令
};

struct xreg_result_t {
  uint32_t rd; // 写回数据
  uint32_t reg_idx; // 写回地址
};
struct vreg_result_t {
  std::array<uint32_t, 32> rd; // 写回数据
  uint32_t reg_idx; // 写回地址
  std::array<bool, 32> mask;
  // TBD
};

struct insn_result_t {
  InsnType insn_type;
  xreg_result_t xreg_result;
  vreg_result_t vreg_result;
};

struct single_insn_cmp_t {
  bool care; // 是否支持单指令比对功能。以 gvm_care_insns.cpp 中的列表为准
  bool dut_done;
  bool ref_done;
  insn_result_t dut_result; // dut_done 为 true 时，其中的内容才有效
  insn_result_t ref_result; // ref_done 为 true 时，其中的内容才有效
  int cmp_pass; // 0 for not compared yet, 1 for cmp pass, -1 for cmp fail, -2 for unknown insn type
};

struct insn_t {
  // dut_active_warp_t 中的 insns 中的条目
  uint32_t pc;
  uint32_t insn;
  bool extended;
  bool care; // 是否影响 retire
  bool done;
  bool retired;
  single_insn_cmp_t single_insn_cmp; // 单指令比对所需的数据
  uint32_t dispatch_id; // 需要注意该 id 在 SM 完成旧 warp 获得新 warp 时不会重置
};

struct dut_active_warp_t {
  uint32_t sm_id;
  uint32_t hardware_warp_id;
  uint32_t software_wg_id;
  uint32_t software_warp_id;
  uint32_t xreg_base;
  uint32_t xreg_usage;
  uint32_t vreg_base;
  uint32_t vreg_usage;
  uint32_t wg_slot_id_in_warp_sche;
  std::vector<uint32_t> curr_xreg; // xreg
  std::vector<std::vector<uint32_t>> curr_vreg; // vreg
  std::map<uint32_t, insn_t> insns; // dispatch_id -> insn
  std::unordered_set<uint32_t> pending_single_insn_cmp_dispatch_ids;
  uint32_t base_dispatch_id; // 本 warp 的首条指令的 dispatch_id
  uint32_t next_retire_dispatch_id; // 下一条应当被 retire 的指令的 id
  bool base_dispatch_id_set; // 是否设置了 base_dispatch_id，初始为 0
  uint32_t num_thread;
};

using warp_key_t = std::pair<uint32_t, uint32_t>; // software_wg_id, software_warp_id

struct retireInfo_t {
  struct retire_cnt_item_t {
    uint32_t sm_id;
    uint32_t hardware_warp_id;
    uint32_t software_wg_id;
    uint32_t software_warp_id;
    uint32_t retire_cnt;
    bool barrier_included;
    bool barrier_retry;
  };
  std::vector<retire_cnt_item_t> warp_retire_cnt;
};
