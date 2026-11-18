// GVM 核心类的实现

#include <cstdint>
#include <map>
#include <vector>
#include <memory>
#include <cmath>
#include <cstring>
#include <spdlog/logger.h>

#include "../../spike/gvmref/gvmref_interface.h"
#include "gvm_global_var.hpp"
#include "gvm.hpp"
#include "gvm_structs.hpp"
#include "ventus_rtlsim.h"
#include <bitset>
#include <array>
#include "gvm_macro.h"
#include <string>
#include <cassert>
#include <cstdlib>

//
// ------------------------- insn related ----------------------------------------
//

uint64_t gvm_t::makeHwWarpKey(uint32_t sm_id, uint32_t hardware_warp_id) {
  return (static_cast<uint64_t>(sm_id) << 32) | hardware_warp_id;
}

uint64_t gvm_t::makeSmWgslotKey(uint32_t sm_id, uint32_t wg_slot_id) {
  return (static_cast<uint64_t>(sm_id) << 32) | wg_slot_id;
}

uint32_t gvm_t::getNumWgSlotPerSm() {
  static uint32_t num_wg_slot_per_sm = 0;
  if (num_wg_slot_per_sm != 0) {
    return num_wg_slot_per_sm;
  }

  if (ventus_rtlsim_get_parameter("num_block", &num_wg_slot_per_sm) != 0 || num_wg_slot_per_sm == 0) {
    logger->error("GVM INTERNAL error: failed to query RTL parameter num_block.");
    assert(0);
  }
  return num_wg_slot_per_sm;
}

void gvm_t::bindWorkgroupSlotOrDie(uint32_t software_wg_id, uint32_t slot_linear) {
  auto& binding = workgroup_runtime_bindings[software_wg_id];
  if (binding.pds_slot_valid) {
    if (binding.pds_slot_linear != slot_linear) {
      setFatalMismatch(fmt::format(
          "workgroup {} observed inconsistent PDS slot_linear: old={}, new={}",
          software_wg_id,
          binding.pds_slot_linear,
          slot_linear
      ));
    }
    return;
  }

  binding.pds_slot_valid = true;
  binding.pds_slot_linear = slot_linear;
  if (gvmref_bind_workgroup_slot(software_wg_id, slot_linear) != 0) {
    setFatalMismatch(fmt::format(
        "gvmref_bind_workgroup_slot failed for workgroup {}, slot_linear={}",
        software_wg_id,
        slot_linear
    ));
  }
}

void gvm_t::bindWorkgroupLdsBaseOrDie(uint32_t software_wg_id, uint32_t lds_base) {
  auto& binding = workgroup_runtime_bindings[software_wg_id];
  if (binding.lds_base_valid) {
    if (binding.lds_base != lds_base) {
      setFatalMismatch(fmt::format(
          "workgroup {} observed inconsistent LDS base: old=0x{:08x}, new=0x{:08x}",
          software_wg_id,
          binding.lds_base,
          lds_base
      ));
    }
    return;
  }

  binding.lds_base_valid = true;
  binding.lds_base = lds_base;
  if (gvmref_bind_workgroup_lds_base(software_wg_id, lds_base) != 0) {
    setFatalMismatch(fmt::format(
        "gvmref_bind_workgroup_lds_base failed for workgroup {}, lds_base=0x{:08x}",
        software_wg_id,
        lds_base
    ));
  }
}

dut_active_warp_t* gvm_t::findWarpByHw(uint32_t sm_id, uint32_t hardware_warp_id) {
  auto it = hw_warp_to_sw_warp.find(makeHwWarpKey(sm_id, hardware_warp_id));
  if (it == hw_warp_to_sw_warp.end()) {
    return nullptr;
  }
  auto warp_it = dut_active_warps.find(it->second);
  if (warp_it == dut_active_warps.end()) {
    return nullptr;
  }
  return &warp_it->second;
}

const dut_active_warp_t* gvm_t::findWarpByHw(uint32_t sm_id, uint32_t hardware_warp_id) const {
  auto it = hw_warp_to_sw_warp.find(makeHwWarpKey(sm_id, hardware_warp_id));
  if (it == hw_warp_to_sw_warp.end()) {
    return nullptr;
  }
  auto warp_it = dut_active_warps.find(it->second);
  if (warp_it == dut_active_warps.end()) {
    return nullptr;
  }
  return &warp_it->second;
}

void gvm_t::setFatalMismatch(const std::string& msg) {
  fatal_mismatch = true;
  fatal_mismatch_msg = msg;
  logger->error("GVM ERROR[FATAL]: {}", msg);
  std::abort();
}

bool gvm_t::isInsnCareCached(
    uint32_t insn, const std::vector<care_insn_t>& care_insns, std::unordered_map<uint32_t, bool>& cache
) {
  auto it = cache.find(insn);
  if (it != cache.end()) {
    return it->second;
  }
  bool hit = false;
  for (const auto& pattern : care_insns) {
    if ((insn & pattern.mask) == pattern.value) {
      hit = true;
      break;
    }
  }
  cache.emplace(insn, hit);
  return hit;
}

bool gvm_t::isInsnCare(uint32_t insn, const std::vector<care_insn_t>& care_insns) {
  // 判断指令是否 care
  for (const auto& pattern : care_insns) {
    if ((insn & pattern.mask) == pattern.value) {
      return true;
    }
  }
  return false;
}

uint32_t gvm_t::getInsnRd(uint32_t insn) {
  return (insn >> 7) & 0x1f;
}

void gvm_t::disasm(uint32_t insn, char* insn_name) {
  std::vector<std::string> matched_names;
  for (const auto& pattern : disasm_table) {
    if ((insn & pattern.mask) == pattern.value) {
      matched_names.push_back(pattern.name);
    }
  }
  if (matched_names.empty()) {
    strcpy(insn_name, " ");
    return;
  }
  if (matched_names.size() > 1) {
    logger->error("GVM INTERNAL error: multiple disasm matches for insn 0x{:08x}: {}"
      , insn, fmt::join(matched_names, " "));
    assert(0);
  } else {
    strcpy(insn_name, matched_names[0].c_str());
  }
  return;
}

//
// ------------------------- gvm_t::getDut() ----------------------------------------------
//

static std::array<bool, 32> unpack_to_array(uint32_t v, size_t n) {
    std::bitset<32> bs(v);
    std::array<bool, 32> a{};
    for (size_t i = 0; i < n; ++i) {
        a[i] = bs[i]; // 保持一致：a[0] 对应最低位
    }
    return a;
}

static std::string mask_to_string(const std::array<bool, 32>& mask, size_t n) {
    std::string s;
    for (size_t i = 0; i < n; ++i) {
        s.push_back(mask[i] ? '1' : '0');
    }
    return s;
}

void gvm_t::getDut() {
  getDutWarpNew(); // 添加新 warp 条目
  getDutWarpFinish(); // 删除已完成 warp 条目
  getDutInsnDispatch(); // 添加新指令条目，其中不关心的指令直接置为 single_insn_cmp.cmp_pass = 1
  getDutInsnFinish(); // 标记指令条目为已完成，维护 dut_done 与 dut_result
  // 为了降低开销，不再每周期读取全量标量/向量寄存器堆
  // 仅在新warp分配时，读取并同步标量/向量寄存器堆到REF
  getDutXReg();
  getDutVReg();
  getDutWarpNewSetRefXReg();
  getDutWarpNewSetRefVReg();
  clearGlobal(); // 清空全局变量
}

void gvm_t::getDutWarpNew() {
  for (const auto& item : g_cta2warp_data) {
    dut_active_warp_t d;
    d.sm_id = item.sm_id;
    d.hardware_warp_id = item.hardware_warp_id;
    d.software_wg_id = item.software_wg_id;
    d.software_warp_id = item.software_warp_id;
    d.xreg_base = item.sgpr_base;
    d.xreg_usage = g_sgprUsage; // 临时特殊处理
    d.vreg_base = item.vgpr_base;
    d.vreg_usage = g_vgprUsage; // 临时特殊处理
    d.base_dispatch_id_set = 0;
    d.wg_slot_id_in_warp_sche = item.wg_slot_id_in_warp_sche;
    d.num_thread = item.num_thread_in_warp;

    // check if the warp is already in the list
    bool found_sw = false;
    bool found_hw = false;
    for (const auto& warp : dut_active_warps) {
      if (warp.second.software_wg_id == d.software_wg_id && warp.second.software_warp_id == d.software_warp_id) {
        found_sw = true;
      }
      if (warp.second.sm_id == d.sm_id && warp.second.hardware_warp_id == d.hardware_warp_id) {
        found_hw = true;
      }
    }
    if (found_sw) {
      logger->error("GVM INTERNAL error: repeated cta2warp dispatch with same software_wg_id {} & software_warp_id {}.",
        d.software_wg_id, d.software_warp_id);
      assert(0);
    }
    if (found_hw) {
      logger->error("GVM INTERNAL error: repeated cta2warp dispatch with same sm_id {} & hardware_warp_id {}.",
        d.sm_id, d.hardware_warp_id);
      assert(0);
    }
    warp_key_t key { d.software_wg_id, d.software_warp_id };
    dut_active_warps[key] = d;
    hw_warp_to_sw_warp[makeHwWarpKey(d.sm_id, d.hardware_warp_id)] = key;
    sm_wgslot_to_sw_wg[makeSmWgslotKey(d.sm_id, d.wg_slot_id_in_warp_sche)] = d.software_wg_id;
    const uint32_t slot_linear = d.sm_id * getNumWgSlotPerSm() + d.wg_slot_id_in_warp_sche;
    bindWorkgroupSlotOrDie(d.software_wg_id, slot_linear);
    bindWorkgroupLdsBaseOrDie(d.software_wg_id, item.lds_base);
  }
}

void gvm_t::getDutWarpFinish() {
  for (const auto& item : g_insn_dispatch_data) {
    if (item.insn == 0x0000400B) {
      // 0x0000400B 是 endprg 指令
      // delete dut_active_warp
      auto* warp = findWarpByHw(item.sm_id, item.hardware_warp_id);
      if (warp == nullptr) {
        logger->debug(
            "GVM info: endprg dispatched but no active warp found for sm_id {}, hardware_warp_id {}",
            item.sm_id, item.hardware_warp_id
        );
      } else {
        logger->debug(
            "GVM info: endprg dispatched, deleting warp sm_id {}, hardware_warp_id {}",
            warp->sm_id, warp->hardware_warp_id
        );
        hw_warp_to_sw_warp.erase(makeHwWarpKey(warp->sm_id, warp->hardware_warp_id));
        bool has_same_wgslot = false;
        for (const auto& active_warp : dut_active_warps) {
          if (active_warp.second.sm_id == warp->sm_id
              && active_warp.second.wg_slot_id_in_warp_sche == warp->wg_slot_id_in_warp_sche
              && !(active_warp.second.software_wg_id == warp->software_wg_id
                   && active_warp.second.software_warp_id == warp->software_warp_id)) {
            has_same_wgslot = true;
            break;
          }
        }
        if (!has_same_wgslot) {
          sm_wgslot_to_sw_wg.erase(makeSmWgslotKey(warp->sm_id, warp->wg_slot_id_in_warp_sche));
        }
        dut_active_warps.erase({ warp->software_wg_id, warp->software_warp_id });
      }
      // 这里最好把对应的 ref warp 跑到头
    }
  }
}

void gvm_t::getDutInsnDispatch() {
  for (const auto& item : g_insn_dispatch_data) {
    insn_t d;
    d.pc = item.pc;
    d.insn = item.insn;
    d.extended = item.is_extended;
    d.care = isInsnCareCached(item.insn, retire_care_insns, retire_care_cache);
    d.done = 0;
    d.retired = 0;
    d.single_insn_cmp.care = isInsnCareCached(item.insn, single_insn_cmp_care_insns, single_cmp_care_cache);
    const bool scalar_single_cmp_care =
        isInsnCareCached(item.insn, scalar_single_insn_cmp_care_insns, scalar_single_cmp_care_cache);
    if (scalar_single_cmp_care && getInsnRd(item.insn) == 0) {
      d.single_insn_cmp.care = false;
    }
    d.single_insn_cmp.dut_done = 0;
    d.single_insn_cmp.ref_done = 0;
    d.single_insn_cmp.cmp_pass = 0;
    if (!d.single_insn_cmp.care) {
      d.single_insn_cmp.cmp_pass = 1; // 不关心的指令直接置为比对通过
    }
    d.dispatch_id = item.dispatch_id;

    auto* warp = findWarpByHw(item.sm_id, item.hardware_warp_id);
    if (warp != nullptr) {
      if (warp->insns.find(d.dispatch_id) != warp->insns.end()) {
        logger->error(
            "GVM INTERNAL error: duplicate dispatch_id {} in warp sm_id {}, hardware_warp_id {}",
            d.dispatch_id, item.sm_id, item.hardware_warp_id
        );
        assert(0);
      } else {
        warp->insns[d.dispatch_id] = d;
        if (d.single_insn_cmp.care) {
          warp->pending_single_insn_cmp_dispatch_ids.insert(d.dispatch_id);
        }
        if (!warp->base_dispatch_id_set) {
          // 设置本 warp 的首条指令的 dispatch_id
          warp->base_dispatch_id = d.dispatch_id;
          warp->base_dispatch_id_set = 1;
          warp->next_retire_dispatch_id = d.dispatch_id;
        }
      }
    }
    // 目前获取 warp 结束的标志是 endprg dispatch，
    // 但这距离 endprg 真正执行完成还有一段时间，
    // 在此期间仍有其他指令会 dispatch，但 dut_active_warps 中的 warp 已经被删除了
  }
}

void gvm_t::getDutInsnFinish() {
  // 将 insns 中 care 的指令标记为已完成
  // 并记录 insn_t.single_insn_cmp 中 care 的指令的完成信息
  getDutXRegWbFinish();
  getDutVRegWbFinish();
  getDutBarDone();
}
void gvm_t::getDutXRegWbFinish() {
  for (const auto& item : g_xreg_wb_data) {
    const bool in_retire_care = isInsnCareCached(item.insn, retire_care_insns, retire_care_cache);
    const bool in_barrier = isInsnCareCached(item.insn, barrier_insns, barrier_care_cache);
    if (!in_retire_care || in_barrier) {
      logger->error(
          "GVM ERROR[XREG_WB_TYPE]: unexpected XReg writeback type sm_id={}, hw_warp_id={}, dispatch_id={}, pc=0x{:08x}, insn=0x{:08x}",
          item.sm_id, item.hardware_warp_id, item.dispatch_id, item.pc, item.insn
      );
      continue;
    }
    auto* warp = findWarpByHw(item.sm_id, item.hardware_warp_id);
    if (warp == nullptr) {
      logger->debug(
          "GVM info: xreg wb cannot find active warp sm_id={}, hw_warp_id={}, dispatch_id={}",
          item.sm_id, item.hardware_warp_id, item.dispatch_id
      );
      continue;
    }
    auto insn_it = warp->insns.find(item.dispatch_id);
    if ((insn_it != warp->insns.end()) && (insn_it->second.done != 1)) {
      if (insn_it->second.pc != item.pc || insn_it->second.insn != item.insn || !insn_it->second.care) {
        logger->error(
            "GVM ERROR[XREG_WB_META]: xreg wb metadata mismatch sm_id={}, hw_warp_id={}, dispatch_id={}, dut_pc=0x{:08x}, dut_insn=0x{:08x}, wb_pc=0x{:08x}, wb_insn=0x{:08x}",
            item.sm_id, item.hardware_warp_id, item.dispatch_id, insn_it->second.pc, insn_it->second.insn, item.pc,
            item.insn
        );
      }
      // 维护 retire 相关变量
      insn_it->second.done = true;
      // 标量也纳入单指令比对
      if (insn_it->second.single_insn_cmp.care == true) {
        insn_it->second.single_insn_cmp.dut_done = 1;
        insn_it->second.single_insn_cmp.dut_result.insn_type = InsnType::XREG;
        insn_it->second.single_insn_cmp.dut_result.xreg_result.rd = item.rd;
        insn_it->second.single_insn_cmp.dut_result.xreg_result.reg_idx = item.reg_idx;
      }
    } else {
      logger->error(
          "GVM ERROR[XREG]: xreg wb cannot match unfinished dispatch_id, sm_id={}, hw_warp_id={}, dispatch_id={}, pc=0x{:08x}, insn=0x{:08x}",
          item.sm_id, item.hardware_warp_id, item.dispatch_id, item.pc, item.insn
      );
    }
  }
}

void gvm_t::getDutVRegWbFinish() {
  for (const auto& item : g_vreg_wb_data) {
    const bool in_barrier = isInsnCareCached(item.second.insn, barrier_insns, barrier_care_cache);
    const bool in_retire = isInsnCareCached(item.second.insn, retire_care_insns, retire_care_cache);
    if (in_barrier || in_retire) {
      logger->error(
          "GVM ERROR[VREG_WB_TYPE]: unexpected VReg writeback type sm_id={}, hw_warp_id={}, dispatch_id={}, pc=0x{:08x}, insn=0x{:08x}",
          item.second.sm_id, item.second.hardware_warp_id, item.second.dispatch_id, item.second.pc, item.second.insn
      );
      continue;
    }
    if (isInsnCareCached(item.second.insn, single_insn_cmp_care_insns, single_cmp_care_cache)) {
      auto* warp = findWarpByHw(item.second.sm_id, item.second.hardware_warp_id);
      if (warp == nullptr) {
        logger->debug(
            "GVM info: vreg wb cannot find active warp sm_id={}, hw_warp_id={}, dispatch_id={}",
            item.second.sm_id, item.second.hardware_warp_id, item.second.dispatch_id
        );
        continue;
      }
      auto insn_it = warp->insns.find(item.second.dispatch_id);
      if (insn_it != warp->insns.end() && (insn_it->second.single_insn_cmp.dut_done != 1)) {
        if (insn_it->second.pc != item.second.pc || insn_it->second.insn != item.second.insn || insn_it->second.care) {
          logger->error(
              "GVM ERROR[VREG_WB_META]: vreg wb metadata mismatch sm_id={}, hw_warp_id={}, dispatch_id={}, dut_pc=0x{:08x}, dut_insn=0x{:08x}, wb_pc=0x{:08x}, wb_insn=0x{:08x}",
              item.second.sm_id, item.second.hardware_warp_id, item.second.dispatch_id, insn_it->second.pc,
              insn_it->second.insn, item.second.pc, item.second.insn
          );
        }
        // 维护 single insn cmp 相关变量
        if (insn_it->second.single_insn_cmp.care == true) {
          insn_it->second.single_insn_cmp.dut_done = 1;
          insn_it->second.single_insn_cmp.dut_result.insn_type = InsnType::VREG;
          insn_it->second.single_insn_cmp.dut_result.vreg_result.rd = item.second.rd_data;
          insn_it->second.single_insn_cmp.dut_result.vreg_result.reg_idx = item.second.reg_idx;
          insn_it->second.single_insn_cmp.dut_result.vreg_result.mask = item.second.wvd_mask;
        }
      } else {
        logger->debug(
            "GVM info: vreg wb cannot match unfinished dispatch_id, sm_id={}, hw_warp_id={}, dispatch_id={}, pc=0x{:08x}, insn=0x{:08x}",
            item.second.sm_id, item.second.hardware_warp_id, item.second.dispatch_id, item.second.pc, item.second.insn
        );
      }
    } else {
      logger->warn("GVM WARN[VREG_IGNORED]: ignoring VReg writeback from pc 0x{:08x}, insn 0x{:08x}",
        item.second.pc, item.second.insn);
    }
  }
}

void gvm_t::getDutBarDone() {
  for (const auto& item : g_bar_done_data) {
    if (!isInsnCareCached(item.insn, barrier_insns, barrier_care_cache)
        || !isInsnCareCached(item.insn, retire_care_insns, retire_care_cache)) {
      logger->error(
          "GVM ERROR[BARRIER_DONE_TYPE]: unexpected barrier done event sm_id={}, wg_slot_id={}, pc=0x{:08x}, insn=0x{:08x}",
          item.sm_id, item.wg_slot_id, item.pc, item.insn
      );
      continue;
    }
    if (isInsnCareCached(item.insn, single_insn_cmp_care_insns, single_cmp_care_cache)) {
      logger->error(
          "GVM INTERNAL error: barrier insn should not be in single-insn-cmp list, insn=0x{:08x}",
          item.insn
      );
      assert(0);
      continue;
    } // barrier 指令不参与单指令比对
    bool found = false;
    auto sw_it = sm_wgslot_to_sw_wg.find(makeSmWgslotKey(item.sm_id, item.wg_slot_id));
    if (sw_it != sm_wgslot_to_sw_wg.end()) {
      for (auto& warp_it : dut_active_warps) {
        if (warp_it.second.software_wg_id != sw_it->second) {
          continue;
        }
        for (auto& insn: warp_it.second.insns) {
          if (insn.second.pc == item.pc) {
            if(insn.second.done == false) {
              found = true;
            }
            // 为什么这里不用 dispatch_id 来识别指令？因为各个 warp 的分支行为可能不同
            // 进而导致对同一条 barrier 指令的 dispatch_id 不同
            // 而这里拿到的 dispatch_id 只是最后一个完成的 warp 的 dispatch_id
            // 因此使用 pc 识别 barrier 指令
            if (!insn.second.care) {
              logger->error(
                  "GVM ERROR[BARRIER_DONE_MATCH]: barrier done matched a non-retire-care insn, sm_id={}, wg_slot_id={}, pc=0x{:08x}",
                  item.sm_id, item.wg_slot_id, item.pc
              );
            }
            insn.second.done = true; // 标记该指令已完成
          }
        }
      }
    }
    if(found == false) {
      logger->debug("GVM info: barrier done cannot find unfinished barrier insn, sm_id: {}, wg_slot_id: {}, pc: 0x{:08x}, insn: 0x{:08x}",
        item.sm_id, item.wg_slot_id, item.pc, item.insn);
    }
  }
}

void gvm_t::getDutXReg() {
  if (g_cta2warp_data.empty()) {
    return;
  }
  if (g_warp_xreg_init_data.empty()) {
    logger->warn("GVM WARN[XREG_SYNC]: no xreg snapshot this cycle when new warp exists, skip xreg sync");
    return;
  }
  for (const auto& item : g_cta2warp_data) {
    auto warp_it = dut_active_warps.find({ item.software_wg_id, item.software_warp_id });
    if (warp_it == dut_active_warps.end()) {
      continue;
    }
    auto& warp = warp_it->second;
    const WarpXRegInitData* snapshot = nullptr;
    for (const auto& xreg_item : g_warp_xreg_init_data) {
      if (xreg_item.sm_id == warp.sm_id && xreg_item.hardware_warp_id == warp.hardware_warp_id) {
        snapshot = &xreg_item;
        break;
      }
    }
    if (snapshot == nullptr) {
      logger->warn(
          "GVM WARN[XREG_SYNC]: no xreg snapshot for sm_id {}, hardware_warp_id {}, skip xreg sync",
          warp.sm_id, warp.hardware_warp_id
      );
      continue;
    }
    warp.curr_xreg.resize(warp.xreg_usage);
    if (snapshot->xreg_data.size() < warp.xreg_usage) {
      logger->error(
          "GVM INTERNAL error: xreg snapshot too small for warp sw_wg={}, sw_warp={}, snapshot_size={}, xreg_usage={}",
          warp.software_wg_id, warp.software_warp_id, snapshot->xreg_data.size(), warp.xreg_usage
      );
      assert(0);
      continue;
    }
    for (uint32_t i = 0; i < warp.xreg_usage; ++i) {
      warp.curr_xreg[i] = snapshot->xreg_data[i];
    }
    warp.curr_xreg[0] = 0; // 强制 x0 为 0，认为 DUT 已经正确地对 x0 做了特殊处理
  }
}

void gvm_t::getDutVReg() {
  if (g_cta2warp_data.empty()) {
    return;
  }
  if (g_warp_vreg_init_data.empty()) {
    logger->warn("GVM WARN[VREG_SYNC]: no vreg snapshot this cycle when new warp exists, skip vreg sync");
    return;
  }
  for (const auto& item : g_cta2warp_data) {
    auto warp_it = dut_active_warps.find({ item.software_wg_id, item.software_warp_id });
    if (warp_it == dut_active_warps.end()) {
      continue;
    }
    auto& warp = warp_it->second;
    const WarpVRegInitData* snapshot = nullptr;
    for (const auto& vreg_item : g_warp_vreg_init_data) {
      if (vreg_item.sm_id == warp.sm_id && vreg_item.hardware_warp_id == warp.hardware_warp_id) {
        snapshot = &vreg_item;
        break;
      }
    }
    if (snapshot == nullptr) {
      logger->warn(
          "GVM WARN[VREG_SYNC]: no vreg snapshot for sm_id {}, hardware_warp_id {}, skip vreg sync",
          warp.sm_id, warp.hardware_warp_id
      );
      continue;
    }
    warp.curr_vreg.resize(warp.vreg_usage);
    if (snapshot->vreg_data.size() < warp.vreg_usage) {
      logger->error(
          "GVM INTERNAL error: vreg snapshot too small for warp sw_wg={}, sw_warp={}, snapshot_size={}, vreg_usage={}",
          warp.software_wg_id, warp.software_warp_id, snapshot->vreg_data.size(), warp.vreg_usage
      );
      assert(0);
      continue;
    }
    for (uint32_t i = 0; i < warp.vreg_usage; ++i) {
      warp.curr_vreg[i] = snapshot->vreg_data[i];
    }
  }
}

void gvm_t::getDutWarpNewSetRefXReg() {
  // 这个函数是为了解决以下问题：
  // REF 对每一个 warp 的标量寄存器都是零初始化的；
  // 但 RTL DUT 的 CTA 调度器在向 SM 分派新 warp 时，只会在寄存器堆中分配一块空间，但不会零初始化；
  // 导致大量寄存器不匹配。
  // 因此这里在 DUT 的 CTA 调度器向 SM 分派新 warp 时，将 DUT 的该 warp 的寄存器数据同步到 REF 的对应 warp。
  for (const auto& item : g_cta2warp_data) {
    auto warp_it = dut_active_warps.find({ item.software_wg_id, item.software_warp_id });
    if (warp_it == dut_active_warps.end()) {
      logger->error(
          "GVM INTERNAL error: cannot find new warp for xreg sync, software_wg_id={}, software_warp_id={}",
          item.software_wg_id, item.software_warp_id
      );
      assert(0);
      continue;
    }
    auto &warp = warp_it->second;

    gvmref_warp_xreg_t xreg_data;
    xreg_data.xreg.resize(warp.xreg_usage);
    for (uint32_t i = 0; i < warp.xreg_usage; ++i) {
      xreg_data.xreg[i] = warp.curr_xreg[i];
    }
    gvmref_set_warp_xreg(item.software_wg_id, item.software_warp_id, warp.xreg_usage, xreg_data);
  }
}

void gvm_t::getDutWarpNewSetRefVReg() {
  for (const auto& item : g_cta2warp_data) {
    auto warp_it = dut_active_warps.find({ item.software_wg_id, item.software_warp_id });
    if (warp_it == dut_active_warps.end()) {
      logger->error(
          "GVM INTERNAL error: cannot find new warp for vreg sync, software_wg_id={}, software_warp_id={}",
          item.software_wg_id, item.software_warp_id
      );
      assert(0);
      continue;
    }
    auto &warp = warp_it->second;

    gvmref_warp_vreg_t vreg_data;
    vreg_data.vreg.resize(warp.vreg_usage);
    for (uint32_t i = 0; i < warp.vreg_usage; ++i) {
      vreg_data.vreg[i] = warp.curr_vreg[i];
    }
    gvmref_set_warp_vreg(item.software_wg_id, item.software_warp_id, warp.vreg_usage, vreg_data);
  }
}

void gvm_t::clearGlobal() {
  // 清空全局变量
  g_cta2warp_data.clear();
  g_insn_dispatch_data.clear();
  // g_sgprUsage.clear();
  g_xreg_wb_data.clear();
  g_warp_xreg_init_data.clear();
  g_warp_vreg_init_data.clear();
  g_vreg_wb_data.clear();
  g_bar_done_data.clear();
}

//
// ------------------------- gvm_t::gvmStep() ----------------------------------------------
//

int gvm_t::gvmStep() {
  if (fatal_mismatch) {
    return -1;
  }
  checkRetire(); // 判断是否有 retire 指令，并将相关信息写入 retire_info
  stepRef(); // 根据 retire_info 步进 REF，每次步进前确认 PC，维护 ref_done 与 ref_result
  if (fatal_mismatch) {
    return -1;
  }
  doSingleInsnCmp();
  clearInsnItem(); // 删除已 retire 且已 single_insn_cmp 通过的指令条目
  resetRetireInfo(); // 重置 retire_info
  return fatal_mismatch ? -1 : 0;
}


void gvm_t::checkRetire() {
  for (auto& warp: dut_active_warps) {
    auto it = warp.second.insns.find(warp.second.next_retire_dispatch_id);
    if (!(it == warp.second.insns.end() || it->second.retired == false)) {
      logger->error(
          "GVM INTERNAL error: next_retire_dispatch_id already retired, sw_wg={}, sw_warp={}, dispatch_id={}",
          warp.second.software_wg_id, warp.second.software_warp_id, warp.second.next_retire_dispatch_id
      );
      assert(0);
      continue;
    }

    if (it == warp.second.insns.end()) {
      continue;
    }

    auto insn_it_begin = it;
    uint32_t final_cnt = 0;
    uint32_t temp_retire_cnt = 0;
    bool barriered = false;
    for (; it != warp.second.insns.end(); ++it) {
      if (it->second.care == false) {
        if (isInsnCareCached(it->second.insn, barrier_insns, barrier_care_cache)) {
          logger->error("GVM INTERNAL error: barrier insn cannot be non-retire-care");
          assert(0);
        }
        temp_retire_cnt++;
      } else if (it->second.done == true) {
        final_cnt += temp_retire_cnt;
        temp_retire_cnt = 0;
        final_cnt++;
        if(isInsnCareCached(it->second.insn, barrier_insns, barrier_care_cache)) {
          barriered = true;
          break;
        }
      } else {
        break;
      }
    }
    ++it;
    bool retiring = true;
    for (; it != warp.second.insns.end(); ++it) {
      if (barriered) {
        if (!(it->second.care == false || it->second.done == false)) {
          logger->error(
              "GVM ERROR[BARRIER_CROSS]: insn crossed barrier and completed, sw_wg={}, sw_warp={}, dispatch_id={}",
              warp.second.software_wg_id, warp.second.software_warp_id, it->second.dispatch_id
          );
        }
      }
      else if (it->second.care == true && it->second.done == true) {
        retiring = false;
        break;
      }
    }
    if (final_cnt == 0 || retiring == false) {
      continue;
    }
    retireInfo_t::retire_cnt_item_t r;
    r.sm_id = warp.second.sm_id;
    r.hardware_warp_id = warp.second.hardware_warp_id;
    r.software_wg_id = warp.second.software_wg_id;
    r.software_warp_id = warp.second.software_warp_id;
    r.retire_cnt = final_cnt;
    r.barrier_included = barriered;
    r.barrier_retry = false;
    retire_info.warp_retire_cnt.push_back(r);

    logger->debug(fmt::format("GVM retire message from gvm_t::checkRetire()"));

    // 打印 retire log（遍历最终 retire 的那一段）
    auto print_it = insn_it_begin;
    for (uint32_t i = 0; i < final_cnt && print_it != warp.second.insns.end(); ++i, ++print_it) {
      char insn_name[64];
      disasm(print_it->second.insn, insn_name);
      logger->debug(fmt::format(
        "GVM retire: sm_id: {}, hardware_warp_id: {}, software_wg_id: {}, software_warp_id: {}, dispatch_id: {}, pc: 0x{:08x}, insn: 0x{:08x} {}",
        warp.second.sm_id, warp.second.hardware_warp_id, warp.second.software_wg_id,
        warp.second.software_warp_id, print_it->second.dispatch_id, print_it->second.pc, print_it->second.insn, insn_name
      ));
    }
  }
}


void gvm_t::stepRef() {
  auto step_ref_until_insn = [&](uint32_t software_wg_id, uint32_t software_warp_id,
                                 gvmref_step_return_info_t* ret) -> bool {
    do {
      gvmref_step(software_wg_id, software_warp_id, ret);
      if (ret->insn_executed) {
        return true;
      }
    } while (!ret->wg_done);
    return false;
  };

  for (auto& item : retire_info.warp_retire_cnt) {
    // 分别步进 REF 的每个 warp
    auto warp_it = dut_active_warps.find({ item.software_wg_id, item.software_warp_id });
    if (warp_it == dut_active_warps.end() || item.retire_cnt == 0) {
      logger->error(
          "GVM INTERNAL error: invalid retire item sw_wg={}, sw_warp={}, retire_cnt={}",
          item.software_wg_id, item.software_warp_id, item.retire_cnt
      );
      assert(0);
      continue;
    }

    for (int i=0; i<item.retire_cnt; i++) {
      gvmref_step_return_info_t gvmref_step_return_info;

      auto& cur_insn = warp_it->second.insns[warp_it->second.next_retire_dispatch_id];

      if (cur_insn.extended) {
        if (!step_ref_until_insn(item.software_wg_id, item.software_warp_id, &gvmref_step_return_info)) { // 跳过 regext
          setFatalMismatch("REF reached done while skipping extended instruction");
          return;
        }
      }

      // 确认 DUT 与 REF 的 PC 一致
      uint32_t next_gvmref_pc;
      next_gvmref_pc = gvmref_get_next_pc(item.software_wg_id, item.software_warp_id);
      uint32_t next_dut_pc;
      next_dut_pc = cur_insn.pc;
      if (next_dut_pc != next_gvmref_pc) {
        setFatalMismatch(
            fmt::format(
                "PC mismatch sm_id={}, hw_warp_id={}, sw_wg={}, sw_warp={}, dut_next_pc=0x{:08x}, ref_next_pc=0x{:08x}",
                item.sm_id, item.hardware_warp_id, item.software_wg_id, item.software_warp_id, next_dut_pc,
                next_gvmref_pc
            )
        );
        return;
      }

      // 步进 REF 并维护 insn_t.single_insn_cmp
      if (!step_ref_until_insn(item.software_wg_id, item.software_warp_id, &gvmref_step_return_info)) {
        setFatalMismatch("REF reached done before stepping retired instruction");
        return;
      }
      uint32_t next2_gvmref_pc = gvmref_get_next_pc(item.software_wg_id, item.software_warp_id);
      if (next2_gvmref_pc == next_gvmref_pc) {
        logger->debug(fmt::format("GVM info: REF PC not advanced after step on sm_id: {}, hardware_warp_id: {}, software_wg_id: {}, software_warp_id: {}. REF next PC before step: 0x{:08x}, after step: 0x{:08x}",
          item.sm_id, item.hardware_warp_id, item.software_wg_id, item.software_warp_id, next_gvmref_pc, next2_gvmref_pc));
        if (isInsnCareCached(gvmref_step_return_info.insn, barrier_insns, barrier_care_cache)) {
          if (!isInsnCareCached(cur_insn.insn, barrier_insns, barrier_care_cache)) {
            setFatalMismatch("REF barrier retry matched non-barrier DUT instruction");
            return;
          }
          if (item.barrier_retry) {
            logger->error(
                "GVM INTERNAL error: barrier_retry already set before, sw_wg={}, sw_warp={}",
                item.software_wg_id, item.software_warp_id
            );
            assert(0);
          }
          item.barrier_retry = true; // 该 warp 包含 barrier 指令，且 REF PC 未前进，标记 barrier_retry
        }
      }
      // assert(next2_gvmref_pc != next_gvmref_pc); // REF 的 PC 应当已经更新

      if (cur_insn.single_insn_cmp.care) {
        // 若为 single_insn_cmp.care 的指令，则从 spike 返回值中提取指令执行结果
        switch (gvmref_step_return_info.insn_result.insn_type) {
          // 对于相同的指令类型，gvmref 认为的指令集合可为 RTL 认为的指令集合的子集
          case XREG:
            if (cur_insn.single_insn_cmp.cmp_pass != 0) {
              logger->error(
                  "GVM INTERNAL error: duplicate REF XREG result on dispatch_id={}", cur_insn.dispatch_id
              );
            }
            cur_insn.single_insn_cmp.ref_done = 1;
            cur_insn.single_insn_cmp.ref_result.insn_type = InsnType::XREG;
            cur_insn.single_insn_cmp.ref_result.xreg_result.rd =
              gvmref_step_return_info.insn_result.xreg_result.rd;
            cur_insn.single_insn_cmp.ref_result.xreg_result.reg_idx =
              gvmref_step_return_info.insn_result.xreg_result.reg_idx;
            break;
          case VREG:
            if (cur_insn.single_insn_cmp.cmp_pass != 0) {
              logger->error(
                  "GVM INTERNAL error: duplicate REF VREG result on dispatch_id={}", cur_insn.dispatch_id
              );
            }
            cur_insn.single_insn_cmp.ref_done = 1;
            cur_insn.single_insn_cmp.ref_result.insn_type = InsnType::VREG;
            cur_insn.single_insn_cmp.ref_result.vreg_result.rd =
              gvmref_step_return_info.insn_result.vreg_result.rd;
            cur_insn.single_insn_cmp.ref_result.vreg_result.reg_idx =
              gvmref_step_return_info.insn_result.vreg_result.reg_idx;
            cur_insn.single_insn_cmp.ref_result.vreg_result.mask =
              unpack_to_array(gvmref_step_return_info.insn_result.vreg_result.mask, warp_it->second.num_thread);

            // 浮点指令结果即时修正：若 DUT/REF 有 bit 差异但误差在容差内，将 DUT 的值覆写到 REF
            // 这样后续的分支指令看到相同的寄存器值，避免因微小浮点差异导致分支方向不同
            if (cur_insn.single_insn_cmp.dut_done
                && isInsnCareCached(cur_insn.insn, fp32_vreg_insns, fp32_vreg_cache)) {
              bool need_fix = false;
              bool has_real_error = false;
              for (uint32_t i = 0; i < warp_it->second.num_thread; i++) {
                if (cur_insn.single_insn_cmp.dut_result.vreg_result.mask[i]) {
                  uint32_t dut_rd = cur_insn.single_insn_cmp.dut_result.vreg_result.rd[i];
                  uint32_t ref_rd = cur_insn.single_insn_cmp.ref_result.vreg_result.rd[i];
                  if (dut_rd != ref_rd) {
                    float dut_val = *reinterpret_cast<float*>(&dut_rd);
                    float ref_val = *reinterpret_cast<float*>(&ref_rd);
                    if (std::abs(dut_val - ref_val) <= fp32_atol + fp32_rtol * std::abs(ref_val)) {
                      need_fix = true;
                    } else {
                      has_real_error = true;
                      break;
                    }
                  }
                }
              }
              if (need_fix && !has_real_error) {
                gvmref_warp_single_vreg_t fix_data;
                fix_data.reg_idx = cur_insn.single_insn_cmp.dut_result.vreg_result.reg_idx;
                fix_data.data.assign(
                    cur_insn.single_insn_cmp.dut_result.vreg_result.rd.begin(),
                    cur_insn.single_insn_cmp.dut_result.vreg_result.rd.begin() + warp_it->second.num_thread);
                gvmref_set_warp_single_vreg(item.software_wg_id, item.software_warp_id, fix_data);
                // 同步修正 ref_result，使 doSingleInsnCmp 不报错
                for (uint32_t i = 0; i < warp_it->second.num_thread; i++) {
                  if (cur_insn.single_insn_cmp.dut_result.vreg_result.mask[i]) {
                    cur_insn.single_insn_cmp.ref_result.vreg_result.rd[i] =
                        cur_insn.single_insn_cmp.dut_result.vreg_result.rd[i];
                  }
                }
              }
            }
            break;
        }
        if (cur_insn.single_insn_cmp.ref_done == 0) {
          logger->debug(fmt::format("GVM WARN[INSN_TYPE]: suspected dut/ref insn-type mismatch on pc: 0x{:08x}, insn: 0x{:08x}.",
            cur_insn.pc, cur_insn.insn)); // cmp care 但 ref 返回的 insn type 非 care
          volatile bool temp_flag = cur_insn.single_insn_cmp.ref_done;
        }
        cur_insn.single_insn_cmp.ref_done = 1;
        // 即便 gvmref_step_return_info.insn_result.insn_type 不在上面的 switch-case 中，也置为 ref_done = 1
        // 避免因 care == 1 但 ref_done 恒为 0 导致该条目永远无法删除
        // 在下面的 doSingleInsnCmp() 中会将其忽略而不进行比较（insn type switch-case 的 default）
      }
      if (!item.barrier_retry) {
        cur_insn.retired = true;
        warp_it->second.next_retire_dispatch_id++;
      }
    }
  }
  // 步进末尾的 barrier
  for (const auto& item : retire_info.warp_retire_cnt) {
    // 分别步进 REF 的每个 warp
    auto warp_it = dut_active_warps.find({ item.software_wg_id, item.software_warp_id });
    if (warp_it == dut_active_warps.end()) {
      continue;
    }

    if (item.barrier_included && item.barrier_retry) {
      gvmref_step_return_info_t gvmref_step_return_info;
      auto& cur_insn = warp_it->second.insns[warp_it->second.next_retire_dispatch_id];
      if (cur_insn.extended) {
        logger->error("GVM INTERNAL error: barrier insn should not be extended");
        assert(0);
      }

      // 确认 DUT 与 REF 的 PC 一致
      uint32_t next_gvmref_pc;
      next_gvmref_pc = gvmref_get_next_pc(item.software_wg_id, item.software_warp_id);
      uint32_t next_dut_pc;
      next_dut_pc = cur_insn.pc;
      if (next_dut_pc != next_gvmref_pc) {
        setFatalMismatch(
            fmt::format(
                "PC mismatch while retry barrier sm_id={}, hw_warp_id={}, sw_wg={}, sw_warp={}, dut_next_pc=0x{:08x}, ref_next_pc=0x{:08x}",
                item.sm_id, item.hardware_warp_id, item.software_wg_id, item.software_warp_id, next_dut_pc,
                next_gvmref_pc
            )
        );
        return;
      }

      // 步进 REF 并维护 insn_t.single_insn_cmp
      if (!step_ref_until_insn(item.software_wg_id, item.software_warp_id, &gvmref_step_return_info)) {
        setFatalMismatch("REF reached done before retrying barrier instruction");
        return;
      }
      uint32_t next2_gvmref_pc = gvmref_get_next_pc(item.software_wg_id, item.software_warp_id);
      if (next2_gvmref_pc == next_gvmref_pc) {
        logger->debug(fmt::format("GVM info: REF PC not advanced after step on sm_id: {}, hardware_warp_id: {}, software_wg_id: {}, software_warp_id: {}. REF next PC before step: 0x{:08x}, after step: 0x{:08x}",
          item.sm_id, item.hardware_warp_id, item.software_wg_id, item.software_warp_id, next_gvmref_pc, next2_gvmref_pc));
        logger->debug(
            "GVM info: REF barrier is still waiting for the workgroup; keep barrier unretired and retry later"
        );
        continue;
      }
      // assert(next2_gvmref_pc != next_gvmref_pc); // REF 的 PC 应当已经更新

      if (cur_insn.single_insn_cmp.care) {
        logger->error("GVM INTERNAL error: barrier insn should not enter single-insn-cmp");
        assert(0);
      }
      cur_insn.retired = true;
      warp_it->second.next_retire_dispatch_id++;
    }
  }
}

int gvm_t::doSingleInsnCmp() {
  for (auto& warpIt : dut_active_warps) {
    auto& warp = warpIt.second;
    for (auto it = warp.pending_single_insn_cmp_dispatch_ids.begin(); it != warp.pending_single_insn_cmp_dispatch_ids.end();) {
      auto insnIt = warp.insns.find(*it);
      if (insnIt == warp.insns.end()) {
        it = warp.pending_single_insn_cmp_dispatch_ids.erase(it);
        continue;
      }
      if (insnIt->second.single_insn_cmp.dut_done && insnIt->second.single_insn_cmp.ref_done) {
        switch (insnIt->second.single_insn_cmp.dut_result.insn_type) {
            case InsnType::XREG: {
              if (insnIt->second.single_insn_cmp.ref_result.insn_type != InsnType::XREG) {
                logger->error(
                    "GVM ERROR[XREG]: insn type mismatch at sm_id {}, hw_warp_id {}, sw_wg {}, sw_warp {}, dispatch_id {}, pc 0x{:08x}, insn 0x{:08x}, dut_type=XREG",
                    warp.sm_id, warp.hardware_warp_id, warp.software_wg_id, warp.software_warp_id,
                    insnIt->second.dispatch_id, insnIt->second.pc, insnIt->second.insn
                );
                insnIt->second.single_insn_cmp.cmp_pass = -1;
                break;
              }
              if ((insnIt->second.single_insn_cmp.dut_result.xreg_result.rd
                != insnIt->second.single_insn_cmp.ref_result.xreg_result.rd)
                || (insnIt->second.single_insn_cmp.dut_result.xreg_result.reg_idx
                != insnIt->second.single_insn_cmp.ref_result.xreg_result.reg_idx)) {
                logger->error(fmt::format(
                  "GVM ERROR[XREG]: insn result mismatch sm_id {}, hardware_warp_id {}, software_wg_id {}, software_warp_id {}, dispatch_id {}, pc 0x{:08x}, insn 0x{:08x}, insn_type XREG, DUT reg_idx: {}, REF reg_idx: {}, DUT rd: 0x{:08x}, REF rd: 0x{:08x}",
                  warp.sm_id, warp.hardware_warp_id, warp.software_wg_id,
                  warp.software_warp_id, insnIt->second.dispatch_id, insnIt->second.pc, insnIt->second.insn,
                  insnIt->second.single_insn_cmp.dut_result.xreg_result.reg_idx,
                  insnIt->second.single_insn_cmp.ref_result.xreg_result.reg_idx,
                  insnIt->second.single_insn_cmp.dut_result.xreg_result.rd,
                  insnIt->second.single_insn_cmp.ref_result.xreg_result.rd
                ));
                insnIt->second.single_insn_cmp.cmp_pass = -1;
              } else {
                insnIt->second.single_insn_cmp.cmp_pass = 1;
              }
              break;
            }
            case InsnType::VREG: {
              if (insnIt->second.single_insn_cmp.ref_result.insn_type != InsnType::VREG) {
                logger->error(
                    "GVM ERROR[VREG]: insn type mismatch at sm_id {}, hw_warp_id {}, sw_wg {}, sw_warp {}, dispatch_id {}, pc 0x{:08x}, insn 0x{:08x}, dut_type=VREG",
                    warp.sm_id, warp.hardware_warp_id, warp.software_wg_id, warp.software_warp_id,
                    insnIt->second.dispatch_id, insnIt->second.pc, insnIt->second.insn
                );
                insnIt->second.single_insn_cmp.cmp_pass = -1;
                break;
              }
              bool mask_same = std::equal(
                insnIt->second.single_insn_cmp.dut_result.vreg_result.mask.begin(),
                insnIt->second.single_insn_cmp.dut_result.vreg_result.mask.begin() + warp.num_thread,
                insnIt->second.single_insn_cmp.ref_result.vreg_result.mask.begin()
              );
              if ((insnIt->second.single_insn_cmp.dut_result.vreg_result.mask
                != insnIt->second.single_insn_cmp.ref_result.vreg_result.mask)
                || (!mask_same))
              {
                logger->error(fmt::format(
                  "GVM ERROR[VREG]: DUT and REF vreg insn result writeback mask or reg_idx mismatch at sm_id {}, hardware_warp_id {}, software_wg_id {}, software_warp_id {}, dispatch_id {}, pc 0x{:08x}, insn 0x{:08x}, "
                  "insn_type VREG, DUT reg_idx: {}, REF reg_idx: {}, DUT mask: {}, REF mask: {}, DUT reg_idx: {}, REF reg_idx: {}",
                  warp.sm_id, warp.hardware_warp_id, warp.software_wg_id,
                  warp.software_warp_id, insnIt->second.dispatch_id, insnIt->second.pc, insnIt->second.insn,
                  insnIt->second.single_insn_cmp.dut_result.vreg_result.reg_idx,
                  insnIt->second.single_insn_cmp.ref_result.vreg_result.reg_idx,
                  mask_to_string(insnIt->second.single_insn_cmp.dut_result.vreg_result.mask, warp.num_thread),
                  mask_to_string(insnIt->second.single_insn_cmp.ref_result.vreg_result.mask, warp.num_thread),
                  insnIt->second.single_insn_cmp.dut_result.vreg_result.reg_idx,
                  insnIt->second.single_insn_cmp.ref_result.vreg_result.reg_idx
                ));
                insnIt->second.single_insn_cmp.cmp_pass = -1;
              } else {
                bool is_fp32 = isInsnCareCached(insnIt->second.insn, fp32_vreg_insns, fp32_vreg_cache);
                for (uint32_t i = 0; i < warp.num_thread; i++) {
                  if (insnIt->second.single_insn_cmp.dut_result.vreg_result.mask[i]) {
                    if (is_fp32) {
                      float dut_value = *reinterpret_cast<float*>(&insnIt->second.single_insn_cmp.dut_result.vreg_result.rd[i]);
                      float ref_value = *reinterpret_cast<float*>(&insnIt->second.single_insn_cmp.ref_result.vreg_result.rd[i]);
                      if (std::abs(dut_value - ref_value) > fp32_atol + fp32_rtol * std::abs(ref_value)) {
                        logger->error(fmt::format(
                          "GVM ERROR[VREG_FP32]: DUT and REF vreg-float mismatch at sm_id {}, hardware_warp_id {}, software_wg_id {}, software_warp_id {}, dispatch_id {}, pc 0x{:08x}, insn 0x{:08x}, "
                          "vreg_idx {}, vec_element_idx {}, DUT value: {}, REF value: {}",
                          warp.sm_id, warp.hardware_warp_id, warp.software_wg_id, warp.software_warp_id,
                          insnIt->second.dispatch_id, insnIt->second.pc, insnIt->second.insn,
                          insnIt->second.single_insn_cmp.dut_result.vreg_result.reg_idx,
                          i, dut_value, ref_value
                        ));
                        insnIt->second.single_insn_cmp.cmp_pass = -1;
                      }
                    } else {
                      if (insnIt->second.single_insn_cmp.dut_result.vreg_result.rd[i]
                        != insnIt->second.single_insn_cmp.ref_result.vreg_result.rd[i]) {
                        logger->error(fmt::format(
                          "GVM ERROR[VREG]: DUT and REF vreg mismatch at sm_id {}, hardware_warp_id {}, software_wg_id {}, software_warp_id {}, dispatch_id {}, pc 0x{:08x}, insn 0x{:08x}, "
                          "vreg_idx {}, vec_element_idx {}, DUT value: 0x{:08x}, REF value: 0x{:08x}",
                          warp.sm_id, warp.hardware_warp_id, warp.software_wg_id, warp.software_warp_id,
                          insnIt->second.dispatch_id, insnIt->second.pc, insnIt->second.insn,
                          insnIt->second.single_insn_cmp.dut_result.vreg_result.reg_idx,
                          i,
                          insnIt->second.single_insn_cmp.dut_result.vreg_result.rd[i],
                          insnIt->second.single_insn_cmp.ref_result.vreg_result.rd[i]
                        ));
                        insnIt->second.single_insn_cmp.cmp_pass = -1;
                      }
                    }
                  }
                }
                if (insnIt->second.single_insn_cmp.cmp_pass == 0) {
                  insnIt->second.single_insn_cmp.cmp_pass = 1; // 比对通过
                }
              }
              break;
            }
            default: {
              insnIt->second.single_insn_cmp.cmp_pass = -2; // unknown insn type
              break;
            }
          }
          if (insnIt->second.single_insn_cmp.cmp_pass != 0) {
            it = warp.pending_single_insn_cmp_dispatch_ids.erase(it);
            continue;
          }
        }
      ++it;
      }
  }
  return 0;
}

int gvm_t::doRetireCmp() {
  // 旧版“retire时整堆xreg比对”已停用，标量结果改为单指令比对
  return 0;
}

void gvm_t::clearInsnItem() {
  for (auto& warp: dut_active_warps) {
    for (auto insn_it = warp.second.insns.begin(); insn_it != warp.second.insns.end(); ) {
      if ((insn_it->second.single_insn_cmp.cmp_pass != 0)
        && (insn_it->second.retired == true)) {
        warp.second.pending_single_insn_cmp_dispatch_ids.erase(insn_it->second.dispatch_id);
        insn_it = warp.second.insns.erase(insn_it);
      }
      else {
        break;
      }
    }
  }
}

void gvm_t::resetRetireInfo() {
  retire_info.warp_retire_cnt.clear();
}
