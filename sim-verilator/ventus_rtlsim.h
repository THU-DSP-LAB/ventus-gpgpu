#pragma once

#ifdef __cplusplus
extern "C" {
#endif

#if __GNUC__ >= 4
#define DLL_PUBLIC __attribute__((visibility("default")))
#define DLL_LOCAL __attribute__((visibility("hidden")))
#else
#define DLL_PUBLIC
#define DLL_LOCAL
#endif

#include <stdbool.h>
#include <stdint.h>

typedef struct ventus_rtlsim_t ventus_rtlsim_t;
typedef uint64_t paddr_t;

typedef struct ventus_kernel_metadata_t {
    // Additional data
    const char* name; // kernel name
    void* data;       // use this as you like, such as callback function argument

    // Raw metadata
    uint64_t startaddr;
    uint64_t kernel_id;        // Is this useful??? Maybe this should be moved to additional data
    uint64_t kernel_size[3];   // 每个kernel的workgroup三维数目
    uint64_t wf_size;          // 每个warp的thread数目
    uint64_t wg_size;          // 每个workgroup的warp数目
    uint64_t metaDataBaseAddr; // CSR_KNL的值，
    uint64_t ldsSize;          // 每个workgroup使用的local memory的大小
    uint64_t pdsSize;          // 每个thread用到的private memory大小
    uint64_t sgprUsage;        // 每个wavefront(warp)使用的标量寄存器数目
    uint64_t vgprUsage;        // 每个wavefront(warp)(also thread)使用的向量寄存器数目
    uint64_t pdsBaseAddr; // resident private-memory pool基址；每个驻留workgroup槽位步长为wf_size*wg_size*pdsSize
    uint64_t num_thread_global[3]; // 全局三维thread数目
    uint64_t num_thread_local[3];  // 线程块内三维thread数目
    uint64_t threadIdxOffset[3];   // global threadIdx偏移量
    uint64_t num_buffer;           // buffer的数目，包括pc
    uint64_t* buffer_base;         // 各buffer的基址。第一块buffer是给硬件用的metadata
    uint64_t* buffer_size;      // 各buffer的size，以Bytes为单位。实际使用的大小，用于初始化.data
    uint64_t* buffer_allocsize; // 各buffer的size，以Bytes为单位。分配的大小
} ventus_kernel_metadata_t;

typedef struct {
    uint64_t sim_time_max; // 最大仿真时间限制
    struct {               // These log sinks can be enabled simultaneously
        struct {           // Write log to a file (append to its tail)
            bool enable;
            const char* level; // "trace", "debug", "info", "warn", "error", "critical"
            const char* filename;
        } file;
        struct { // console log
            bool enable;
            const char* level;
        } console;
        const char* level;
    } log;
    struct {
        uint64_t pagesize; // 物理内存页大小
        uint64_t auto_alloc; // 若访存到未分配的物理页，自动分配（如此则与实际硬件内存行为相同）
        // 注意，自动分配的物理内存是不会释放的，除非整个仿真结束
    } pmem;
    struct { // 波形输出功能，这里只设置正常仿真流程，对仿真快照回溯后的波形输出无影响
        bool enable;         // 是否启用？仿真快照回溯后将自动启用
        uint64_t time_begin; // 输出波形的起始时刻
        uint64_t time_end;   // 输出波形的结束时刻，end > begin才有波形输出
        int levels;          // 波形输出的层级
        const char* filename;
    } waveform;
    struct { // 仿真快照，当仿真出错时可回溯仿真进度到最旧快照，开启波形记录重新仿真
        bool enable;
        uint64_t time_interval; // 快照时间间隔
        int num_max;            // 最大快照数量，超限时新快照将顶替最旧快照
        const char* filename;   // 快照输出的FST波形文件名
    } snapshot;
    struct {               // verilator运行时命令行参数，以argc,argv形式传入
        int argc;          // 注意argc可以为0
        const char** argv; // 共有argc个char*字符串，[0]成员不是程序名，而是首个verilator参数
    } verilator;
    uint64_t hang_timeout; // 0 disables PMU progress watchdog; otherwise timeout in simulation time units.
} ventus_rtlsim_config_t;

typedef struct {
    bool error;       // Simulation got fatal error, or RTL $finish()
    bool time_exceed; // Simulation time exceeds limit
    bool idle;        // All given kernels has finished
    bool hang;        // PMU progress watchdog detected no issue/memory progress for too long.
} ventus_rtlsim_step_result_t;

typedef struct {
    uint64_t active_cycles;
    uint64_t total_scalar_issued;
    uint64_t total_vector_issued;
    uint64_t exec_structural_hazard_cycles_x;
    uint64_t exec_structural_hazard_cycles_v;
    uint64_t data_dep_stall_cycles;
    uint64_t barrier_stall_cycles;
    uint64_t control_hazard_flush_count;
    uint64_t frontend_stall_cycles;
    uint64_t lsu_backpressure_cycles;
    uint64_t ibuffer_full_cycles;
} ventus_rtlsim_pipeline_pmu_t;

typedef struct {
    uint64_t compute_issued;
    uint64_t mem_issued;
    uint64_t ctrl_issued;
} ventus_rtlsim_inst_class_pmu_t;

typedef struct {
    uint64_t total_req;
    uint64_t read_req;
    uint64_t write_req;
    uint64_t read_miss;
    uint64_t write_miss;
    uint64_t read_primary_miss;
    uint64_t read_secondary_miss;
    uint64_t read_primary_full_miss;
    uint64_t read_secondary_full_miss;
    uint64_t write_fresh_miss;
    uint64_t write_inflight_miss;
    uint64_t replacements;
    uint64_t dirty_writebacks;
    uint64_t mshr_full_cycles;
    uint64_t rtab_replays;
    uint64_t bank_conflict_cycles;
    uint64_t core_req_pipe_pipelined_cycles;
    uint64_t mem_rsp_pipe_decoupled_cycles;
} ventus_rtlsim_dcache_pmu_t;

typedef struct {
    uint32_t num_sm;
    bool has_dcache;
    const ventus_rtlsim_pipeline_pmu_t *pipeline;
    const ventus_rtlsim_inst_class_pmu_t *inst_class;
    const ventus_rtlsim_dcache_pmu_t *dcache;
} ventus_rtlsim_pmu_t;

typedef enum {
    VENTUS_PMEM_REGION_BUFFER = 0,
    VENTUS_PMEM_REGION_PDS = 1,
} ventus_pmem_region_kind_t;

typedef struct {
    uint64_t cold_page;
    uint64_t pds_cold_page;
    uint64_t allocation_padding;
    uint64_t out_of_bounds;
} ventus_pmem_missing_read_stats_t;

// =
// API functions:
// =

//
// Helper functions
//

// Give you a recommended default config.
DLL_PUBLIC void ventus_rtlsim_get_default_config(ventus_rtlsim_config_t* config);
// Get current simulation time.
DLL_PUBLIC uint64_t ventus_rtlsim_get_time(const ventus_rtlsim_t* sim);
// Check if the simulated GPU is idle (no kernel is running).
DLL_PUBLIC bool ventus_rtlsim_is_idle(const ventus_rtlsim_t* sim);
// Get RTL parameters (output from *out_value, return 0 on success)
DLL_PUBLIC int ventus_rtlsim_get_parameter(const char* name, uint32_t* out_value);

//
// Init, calculate, and finish
//

// Init the simulation.
DLL_PUBLIC ventus_rtlsim_t* ventus_rtlsim_init(const ventus_rtlsim_config_t* config);

// Finish the simulation.
// If error occurred in the simulation, and snapshot feature enabled,
//   it will rollback to the oldest snapshot to find out what happened.
// You can force the rollback by passing `snapshot_rollback_forcing = true`
DLL_PUBLIC void ventus_rtlsim_finish(ventus_rtlsim_t* sim, bool snapshot_rollback_forcing);
// Status-returning variant. Returns zero only when cleanup and any requested replay succeed.
DLL_PUBLIC int ventus_rtlsim_finish_checked(ventus_rtlsim_t* sim, bool snapshot_rollback_forcing);
// Persist or cold-restore a complete idle RTL simulator state. Only available in SAVABLE=1 builds.
DLL_PUBLIC int ventus_rtlsim_save_state(ventus_rtlsim_t* sim, const char* directory);
DLL_PUBLIC ventus_rtlsim_t* ventus_rtlsim_restore_state(
    const ventus_rtlsim_config_t* config, const char* directory);
// Return the supported persistent-state ABI version, or zero when this build is not savable.
DLL_PUBLIC uint32_t ventus_rtlsim_persistent_state_version(void);
// Ask RTL to print the accumulated testcase PMU summary once.
DLL_PUBLIC void ventus_rtlsim_dump_testcase_pmu(ventus_rtlsim_t* sim);
// Return a read-only view of the latest PMU counters sampled after the most recent step().
// The pointed-to storage is owned by sim and may change after the next ventus_rtlsim_step().
DLL_PUBLIC ventus_rtlsim_pmu_t ventus_rtlsim_get_pmu(const ventus_rtlsim_t* sim);

// Calculate 1 unit-time of simulation.
// Return the result of this step: ok, error, time_exceed, or idle.
// If error occurred, calling this function has no effect, you should consider finish the simulation.
DLL_PUBLIC const ventus_rtlsim_step_result_t* ventus_rtlsim_step(ventus_rtlsim_t* sim);

// Host request GPGPU device to invalidate its Icache
// (for example, after loading new kernel code to device memory)
// This will take effect in the next simulation step()
DLL_PUBLIC void ventus_rtlsim_icache_invalidate(ventus_rtlsim_t* sim);

//
// Push new kernels to gpu for execution.
//

// After a kernel finishing its execution, the finish_callback will be called, with metadata passed,
//   aka. `finish_callback(metadata)` will be called.

// It's allowed to delay data-loading until the kernel is actually activated on GPU,
// by using data_load_callback
// **Temporary api**, May be removed in the future
DLL_PUBLIC void ventus_rtlsim_add_kernel__delay_data_loading(
    ventus_rtlsim_t* sim, const ventus_kernel_metadata_t* metadata,
    void (*load_data_callback)(const ventus_kernel_metadata_t*),
    void (*finish_callback)(const ventus_kernel_metadata_t*)
);

// It's recommended to use this ↓. Remember to load data to GPU before calling this.
DLL_PUBLIC void ventus_rtlsim_add_kernel(
    ventus_rtlsim_t* sim, const ventus_kernel_metadata_t* metadata,
    void (*finish_callback)(const ventus_kernel_metadata_t*)
);

//
// Physical memory interface
//

// Physical page alloc & free
// These functions are not needed by actual hardware memory, only for reducing simulation memory usage.
// If config.pmem.auto_alloc is set, you don't need to call these functions.
DLL_PUBLIC bool ventus_rtlsim_pmem_page_alloc(ventus_rtlsim_t* sim, paddr_t base);
DLL_PUBLIC bool ventus_rtlsim_pmem_page_free(ventus_rtlsim_t* sim, paddr_t base);

// Register driver-owned physical ranges for classifying reads from sparse, unmaterialized pages.
// Region metadata is diagnostic state and is intentionally not part of persistent RTL snapshots.
DLL_PUBLIC bool ventus_rtlsim_pmem_region_register(
    ventus_rtlsim_t* sim, paddr_t base, uint64_t requested_size,
    uint64_t allocated_size, ventus_pmem_region_kind_t kind,
    uint64_t allocation_id);
DLL_PUBLIC bool ventus_rtlsim_pmem_region_unregister(
    ventus_rtlsim_t* sim, paddr_t base, uint64_t allocation_id);
DLL_PUBLIC ventus_pmem_missing_read_stats_t ventus_rtlsim_pmem_missing_read_stats(
    const ventus_rtlsim_t* sim);

// Physical memory read & write
// copy data from host to device
DLL_PUBLIC bool ventus_rtlsim_pmemcpy_h2d(ventus_rtlsim_t* sim, paddr_t dst, const void* src, uint64_t size);
// copy data from device to host
DLL_PUBLIC bool ventus_rtlsim_pmemcpy_d2h(ventus_rtlsim_t* sim, void* dst, paddr_t src, uint64_t size);

#ifdef ENABLE_GVM
DLL_PUBLIC int fw_vt_dev_open();
DLL_PUBLIC int fw_vt_dev_close();
DLL_PUBLIC int fw_vt_buf_alloc(uint64_t size, uint64_t *vaddr, int BUF_TYPE, uint64_t taskID, uint64_t kernelID);
DLL_PUBLIC int fw_vt_buf_alloc_fixed(uint64_t size, uint64_t fixed_vaddr, int BUF_TYPE, uint64_t taskID, uint64_t kernelID);
DLL_PUBLIC int fw_vt_buf_free(uint64_t size, uint64_t *vaddr, uint64_t taskID, uint64_t kernelID);
DLL_PUBLIC int fw_vt_one_buf_free(uint64_t size, uint64_t *vaddr, uint64_t taskID, uint64_t kernelID);
DLL_PUBLIC int fw_vt_copy_to_dev(uint64_t dev_vaddr,const void *src_addr, uint64_t size, uint64_t taskID, uint64_t kernelID);
DLL_PUBLIC int fw_vt_start(void* metaData, uint64_t taskID);
DLL_PUBLIC int fw_vt_kernel_finish();
DLL_PUBLIC int fw_vt_upload_kernel_file(const char* filename, int taskID);
#endif // ENABLE_GVM

#undef DLL_PUBLIC
#undef DLL_LOCAL

#ifdef __cplusplus
} // extern "C"
#endif
