#include "sALU.h"

// template <typename T>
void sALU::mi()
{
    while (true)
    {
        wait(emito_salu->obtain_event());
        val.ins = opcins.read();
        val.rss1_data = rss1_data.read();
        val.rss2_data = rss2_data.read();
        vq.push(val);
        eq.notify(tdelay);
    }
}

void sALU::mo()
{
    val = vq.front();
    auto out_data = sALU_calc(val);
    I_TYPE out_ins = val.ins;
    salu_out_t tmp = {ins : out_ins, data : out_data};
    out.write(tmp);
    vq.pop();
    finish->notify();
}

reg_t sALU::sALU_calc(datapack val)
{
    I_TYPE ins = val.ins;
    reg_t rs1 = val.rss1_data;
    reg_t rs2 = val.rss2_data;
    if (ins.op == add_)
        return rs1 + rs2;
    else
        return 0;
}
