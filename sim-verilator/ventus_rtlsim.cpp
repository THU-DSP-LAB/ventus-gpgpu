#include "ventus_rtlsim_impl.hpp"
#include "../../spike/gvmref/gvmref_interface.h"
#include <ctime>
#include <cstdlib>  // bfs4096-008 Phase 0.2: getenv/strtol for VENTUS_VERILATOR_SEED
#include <cstdio>   // bfs4096-008 Phase 0.2: printf actual seed

static char verilator_rand_seed_setting[128] = "+verilator+seed+10086";
static char* verilator_runtime_args_default[] = { verilator_rand_seed_setting };
extern "C" void ventus_rtlsim_get_default_config(ventus_rtlsim_config_t* config) {
    if (config == nullptr)
        return;

    config->sim_time_max = 1000000;
    config->log.console.enable = true;
    config->log.console.level = "info";
    config->log.file.enable = true;
    config->log.file.level = "trace";
    config->log.file.filename = "logs/ventus_rtlsim.log";
    config->log.level = "trace";
    config->pmem.pagesize = 4096;
    config->pmem.auto_alloc = 0;
    config->waveform.enable = true;
    config->waveform.time_begin = 0;
    config->waveform.time_end = -1;
    config->waveform.levels = 99;
    config->waveform.filename = "logs/ventus_rtlsim.fst";
    config->snapshot.enable = true;
    config->snapshot.time_interval = 100000;
    config->snapshot.num_max = 2;
    config->snapshot.filename = "logs/ventus_rtlsim.snapshot.fst";
    config->verilator.argc = 0;
    config->verilator.argv = nullptr;
    config->hang_timeout = 0;

    // bfs4096-008 Phase 0.2: seed control —— 读 ENV VAR VENTUS_VERILATOR_SEED.
    //   设了非空 → 用固定 seed (dive 可复现);  未设 → 保持原 nsec 随机 (default 行为不变).
    //   总是打印实际 seed, 便于后续按 seed 复现 mismatch.
    long verilator_seed_value;
    const char* env_seed = getenv("VENTUS_VERILATOR_SEED");
    if (env_seed != nullptr && env_seed[0] != '\0') {
        verilator_seed_value = strtol(env_seed, nullptr, 0);
        printf("[INFO] Verilator seed: %ld (from VENTUS_VERILATOR_SEED)\n", verilator_seed_value);
    } else {
        timespec ts;
        clock_gettime(CLOCK_MONOTONIC, &ts);
        verilator_seed_value = ts.tv_nsec;
        printf("[INFO] Verilator seed: %ld (nsec random)\n", verilator_seed_value);
    }
    fflush(stdout);
    snprintf(verilator_rand_seed_setting, sizeof(verilator_rand_seed_setting), "+verilator+seed+%ld", verilator_seed_value);
    config->verilator.argc = sizeof(verilator_runtime_args_default) / sizeof(verilator_runtime_args_default[0]);
    config->verilator.argv = (const char**)(verilator_runtime_args_default);
}

extern "C" ventus_rtlsim_t* ventus_rtlsim_init(const ventus_rtlsim_config_t* config) {
    ventus_rtlsim_t* sim = new ventus_rtlsim_t();
    sim->constructor(config);
    return sim;
}
extern "C" void ventus_rtlsim_finish(ventus_rtlsim_t* sim, bool snapshot_rollback_forcing) {
    sim->destructor(snapshot_rollback_forcing);
    delete sim;
}
extern "C" void ventus_rtlsim_dump_testcase_pmu(ventus_rtlsim_t* sim) { sim->dump_testcase_pmu_summary(); }
extern "C" ventus_rtlsim_pmu_t ventus_rtlsim_get_pmu(const ventus_rtlsim_t* sim) {
    if (sim == nullptr) {
        return {};
    }
    return sim->pmu_view();
}
extern "C" const ventus_rtlsim_step_result_t* ventus_rtlsim_step(ventus_rtlsim_t* sim) { return sim->step(); }
extern "C" void ventus_rtlsim_icache_invalidate(ventus_rtlsim_t* sim) { sim->need_icache_invalidate = true; }
extern "C" uint64_t ventus_rtlsim_get_time(const ventus_rtlsim_t* sim) { return sim->contextp->time(); }
extern "C" bool ventus_rtlsim_is_idle(const ventus_rtlsim_t* sim) { return sim->cta->is_idle(); }

extern "C" void ventus_rtlsim_add_kernel__delay_data_loading(
    ventus_rtlsim_t* sim, const ventus_kernel_metadata_t* metadata,
    void (*load_data_callback)(const ventus_kernel_metadata_t*),
    void (*finish_callback)(const ventus_kernel_metadata_t*)
) {
    std::shared_ptr<Kernel> kernel
        = std::make_shared<Kernel>(metadata, load_data_callback, finish_callback, sim->logger);
    sim->cta->kernel_add(kernel);
}
extern "C" void ventus_rtlsim_add_kernel(
    ventus_rtlsim_t* sim, const ventus_kernel_metadata_t* metadata,
    void (*finish_callback)(const ventus_kernel_metadata_t*)
) {
    ventus_rtlsim_add_kernel__delay_data_loading(sim, metadata, nullptr, finish_callback);
}

extern "C" bool ventus_rtlsim_pmem_page_alloc(ventus_rtlsim_t* sim, paddr_t base) {
    return sim->pmem->page_alloc(base);
}
extern "C" bool ventus_rtlsim_pmem_page_free(ventus_rtlsim_t* sim, paddr_t base) { return sim->pmem->page_free(base); }
extern "C" bool ventus_rtlsim_pmemcpy_h2d(ventus_rtlsim_t* sim, paddr_t dst, const void* src, uint64_t size) {
    return sim->pmem->write(dst, src, size);
}
extern "C" bool ventus_rtlsim_pmemcpy_d2h(ventus_rtlsim_t* sim, void* dst, paddr_t src, uint64_t size) {
    return sim->pmem->read(src, dst, size);
}

extern "C" int ventus_rtlsim_get_parameter(const char* name, uint32_t* out_value) {
    if (name == nullptr || out_value == nullptr)
        return -1;
    auto it = rtl_parameters.find(name);
    if (it == rtl_parameters.end())
        return -2;
    *out_value = it->second;
    return 0;
}
#ifdef ENABLE_GVM
extern "C" int fw_vt_dev_open() {
    return gvmref_vt_dev_open();
}
extern "C" int fw_vt_dev_close() {
    return gvmref_vt_dev_close();
}
extern "C" int fw_vt_buf_alloc(uint64_t size, uint64_t *vaddr, int BUF_TYPE, uint64_t taskID, uint64_t kernelID) {
    return gvmref_vt_buf_alloc(size, vaddr, BUF_TYPE, taskID, kernelID);
}
extern "C" int fw_vt_buf_alloc_fixed(uint64_t size, uint64_t fixed_vaddr, int BUF_TYPE, uint64_t taskID, uint64_t kernelID) {
    return gvmref_vt_buf_alloc_fixed(size, fixed_vaddr, BUF_TYPE, taskID, kernelID);
}
extern "C" int fw_vt_buf_free(uint64_t size, uint64_t *vaddr, uint64_t taskID, uint64_t kernelID) {
    return gvmref_vt_buf_free(size, vaddr, taskID, kernelID);
}
extern "C" int fw_vt_one_buf_free(uint64_t size, uint64_t *vaddr, uint64_t taskID, uint64_t kernelID) {
    return gvmref_vt_one_buf_free(size, vaddr, taskID, kernelID);
}
extern "C" int fw_vt_copy_to_dev(uint64_t dev_vaddr,const void *src_addr, uint64_t size, uint64_t taskID, uint64_t kernelID) {
    return gvmref_vt_copy_to_dev(dev_vaddr, src_addr, size, taskID, kernelID);
}
extern "C" int fw_vt_start(void* metaData, uint64_t taskID) {
    return gvmref_vt_start(metaData, taskID);
}
extern "C" int fw_vt_kernel_finish() {
    return gvmref_vt_kernel_finish();
}
extern "C" int fw_vt_upload_kernel_file(const char* filename, int taskID) {
    return gvmref_vt_upload_kernel_file(filename, taskID);
}
#endif // ENABLE_GVM
