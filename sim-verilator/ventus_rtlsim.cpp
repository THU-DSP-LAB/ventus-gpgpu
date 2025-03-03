#include "ventus_rtlsim_impl.hpp"

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
extern "C" const ventus_rtlsim_step_result_t* ventus_rtlsim_step(ventus_rtlsim_t* sim) { return sim->step(); }
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

extern "C" bool ventus_rtlsim_pmem_page_alloc(ventus_rtlsim_t* sim, paddr_t base) { return sim->pmem->page_alloc(base); }
extern "C" bool ventus_rtlsim_pmem_page_free(ventus_rtlsim_t* sim, paddr_t base) { return sim->pmem->page_free(base); }
extern "C" bool ventus_rtlsim_pmemcpy_h2d(ventus_rtlsim_t* sim, paddr_t dst, const void* src, uint64_t size) {
    return sim->pmem->write(dst, src, size);
}
extern "C" bool ventus_rtlsim_pmemcpy_d2h(ventus_rtlsim_t* sim, void* dst, paddr_t src, uint64_t size) {
    return sim->pmem->read(src, dst, size);
}