
#include "ventus_rtlsim_impl.hpp"

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
    std::shared_ptr<Kernel> kernel = std::make_shared<Kernel>(metadata, load_data_callback, finish_callback);
    sim->cta->kernel_add(kernel);
}
extern "C" void ventus_rtlsim_add_kernel(
    ventus_rtlsim_t* sim, const ventus_kernel_metadata_t* metadata,
    void (*finish_callback)(const ventus_kernel_metadata_t*)
) {
    ventus_rtlsim_add_kernel__delay_data_loading(sim, metadata, nullptr, finish_callback);
}

extern "C" bool ventus_rtlsim_pmem_page_alloc(ventus_rtlsim_t* sim, paddr_t base) { return sim->pmem_page_alloc(base); }
extern "C" bool ventus_rtlsim_pmem_page_free(ventus_rtlsim_t* sim, paddr_t base) { return sim->pmem_page_free(base); }
extern "C" bool ventus_rtlsim_pmemcpy_h2d(ventus_rtlsim_t* sim, paddr_t dst, const void* src, uint64_t size) {
    return sim->pmem_write(dst, src, size);
}
extern "C" bool ventus_rtlsim_pmemcpy_d2h(ventus_rtlsim_t* sim, void* dst, paddr_t src, uint64_t size) {
    return sim->pmem_read(src, dst, size);
}