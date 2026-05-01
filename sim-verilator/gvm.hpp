// GVM 核心类的头文件

#pragma once

#include <cstdint>
#include <map>
#include <unordered_map>
#include <vector>
#include <memory>
#include <string>
#include <spdlog/logger.h>

#include "../../spike/gvmref/gvmref_interface.h"
#include "gvm_global_var.hpp"
#include "gvm_structs.hpp"

class gvm_t
{
public:
  gvm_t() = default;
  ~gvm_t() = default;

  void getDut(); // 更新 DUT 成员变量，清空全局变量
  int gvmStep(); // 执行 GVM 步进行为

  std::shared_ptr<spdlog::logger> logger;

private:
  struct workgroup_runtime_binding_t {
    bool pds_slot_valid = false;
    uint32_t pds_slot_linear = 0;
    bool lds_base_valid = false;
    uint32_t lds_base = 0;
  };

  std::map<warp_key_t, dut_active_warp_t> dut_active_warps;
  std::unordered_map<uint64_t, warp_key_t> hw_warp_to_sw_warp;
  std::unordered_map<uint64_t, uint32_t> sm_wgslot_to_sw_wg;
  std::unordered_map<uint32_t, workgroup_runtime_binding_t> workgroup_runtime_bindings;
  std::unordered_map<uint32_t, bool> retire_care_cache;
  std::unordered_map<uint32_t, bool> scalar_single_cmp_care_cache;
  std::unordered_map<uint32_t, bool> single_cmp_care_cache;
  std::unordered_map<uint32_t, bool> fp32_vreg_cache;
  std::unordered_map<uint32_t, bool> barrier_care_cache;
  bool fatal_mismatch = false;
  std::string fatal_mismatch_msg;

  // getDut() 相关函数
  void getDutWarpNew(); // 添加新 warp 条目
  void getDutWarpFinish(); // 删除已完成 warp 条目
  void getDutInsnDispatch(); // 添加新指令条目
  void getDutInsnFinish();
  void getDutXRegWbFinish();
  void getDutVRegWbFinish();
  void getDutBarDone();
  void getDutXReg(); // 根据新 warp 的 DPI 快照更新 XReg 条目
  void getDutVReg(); // 根据新 warp 的 DPI 快照更新 VReg 条目
  void getDutWarpNewSetRefXReg();
  void getDutWarpNewSetRefVReg();
  void clearGlobal(); // 清空全局变量

  // gvmStep() 相关函数
  void checkRetire();
  retireInfo_t retire_info;
  void stepRef();
  int doSingleInsnCmp();
  float fp32_atol = 1e-3f; // 浮点数比较的绝对误差
  float fp32_rtol = 1e-3f; // 浮点数比较的相对误差
  int doRetireCmp();
  void clearInsnItem();
  void resetRetireInfo();
  static uint64_t makeHwWarpKey(uint32_t sm_id, uint32_t hardware_warp_id);
  static uint64_t makeSmWgslotKey(uint32_t sm_id, uint32_t wg_slot_id);
  uint32_t getNumWgSlotPerSm();
  void bindWorkgroupSlotOrDie(uint32_t software_wg_id, uint32_t slot_linear);
  void bindWorkgroupLdsBaseOrDie(uint32_t software_wg_id, uint32_t lds_base);
  dut_active_warp_t* findWarpByHw(uint32_t sm_id, uint32_t hardware_warp_id);
  const dut_active_warp_t* findWarpByHw(uint32_t sm_id, uint32_t hardware_warp_id) const;
  bool isInsnCareCached(
      uint32_t insn, const std::vector<care_insn_t>& care_insns, std::unordered_map<uint32_t, bool>& cache
  );
  void setFatalMismatch(const std::string& msg);

  // 判断指令是否关心
  bool isInsnCare(uint32_t insn, const std::vector<care_insn_t>& care_insns);
  static uint32_t getInsnRd(uint32_t insn);
  static const std::vector<care_insn_t> retire_care_insns;
  static const std::vector<care_insn_t> scalar_single_insn_cmp_care_insns;
  static const std::vector<care_insn_t> single_insn_cmp_care_insns;
  static const std::vector<care_insn_t> fp32_vreg_insns;
  void disasm(uint32_t insn, char* insn_name);
  static const std::vector<care_insn_t> disasm_table;
  static const std::vector<care_insn_t> barrier_insns;
};
