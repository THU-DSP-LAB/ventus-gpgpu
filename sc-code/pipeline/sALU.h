#ifndef _SALU_H
#define _SALU_H

#include "../parameters.h"

struct datapack
{
    I_TYPE ins;
    reg_t rss1_data;
    reg_t rss2_data;
};

// template <typename T>
class sALU : public sc_module
{
public:
    sc_in<I_TYPE> opcins{"opcins"};
    sc_in<reg_t> rss1_data{"rss1_data"};
    sc_in<reg_t> rss2_data{"rss2_data"};
    sc_port<event_if> emito_salu{"emito_salu"};

    sc_port<event_if> finish{"finish"};
    sc_out<salu_out_t> out{"out"};

    SC_HAS_PROCESS(sALU);
    sALU(sc_module_name name_, sc_time tdelay_) : sc_module(name_),
                                                  tdelay(tdelay_),
                                                  // in_pack("in"),
                                                  out("out")
    {
        SC_THREAD(mi);
        SC_METHOD(mo);
        sensitive << eq;
    }

    reg_t sALU_calc(datapack val);
    void mi();
    void mo();

    sc_time tdelay;
    sc_event_queue eq;
    std::queue<datapack> vq;
    datapack val;
};

#endif
