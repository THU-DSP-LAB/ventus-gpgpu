#pragma once

#include "ventus_rtlsim.h"

int ventus_persistent_state_save(ventus_rtlsim_t* sim, const char* directory);
ventus_rtlsim_t* ventus_persistent_state_restore(
    const ventus_rtlsim_config_t* config, const char* directory);
