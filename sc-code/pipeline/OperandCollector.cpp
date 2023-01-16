#include "OperandCollector.h"

void OperandCollector::fifoin()
{
    while (true)
    {
        wait(dispatch->obtain_event());
        opcfifo.put(ibuf_ins.read());
    }
}

void OperandCollector::fifoout()
{
    while (true)
    {
        I_TYPE tmpins = opcfifo.peek();
        switch (tmpins.op)
        {
        case add_:
            if (salu_ready)
            {
                rss1_addr.write(tmpins.s1);
                rss2_addr.write(tmpins.s2);
                wait(SC_ZERO_TIME);
                opcins_salu.write(tmpins);
                salu_data1.write(rss1_data.read());
                salu_data2.write(rss2_data.read());
                emito_salu->notify();
                opcfifo.get();
            }
            else
                wait(salu_ready.posedge_event());
            break;
        case vaddvv_:
            if (valu_ready)
            {
                rsv1_addr.write(tmpins.s1);
                rsv2_addr.write(tmpins.s2);
                wait(SC_ZERO_TIME);
                opcins_valu.write(tmpins);
                for (int i = 0; i < num_thread; i++)
                    valu_data1[i] = rsv1_data[i];
                for (int i = 0; i < num_thread; i++)
                    valu_data2[i] = rsv2_data[i];
                emito_valu->notify();
                opcfifo.get();
            }
            else
                wait(valu_ready.posedge_event());
            break;

        case vaddvx_:
            if (valu_ready)
            {
                rsv1_addr.write(tmpins.s1);
                rss2_addr.write(tmpins.s2);
                wait(SC_ZERO_TIME);
                opcins_valu.write(tmpins);
                for (int i = 0; i < num_thread; i++)
                    valu_data1[i] = rsv1_data[i];
                valu_data2[0] = rss2_data;
                emito_valu->notify();
                opcfifo.get();
            }
            else
                wait(valu_ready.posedge_event());
            break;

        case vfadd_:
            if (vfpu_ready)
            {
                rsf1_addr.write(tmpins.s1);
                rsf2_addr.write(tmpins.s2);
                wait(SC_ZERO_TIME);
                opcins_vfpu.write(tmpins);
                for (int i = 0; i < num_thread; i++)
                    vfpu_data1[i] = rsf1_data[i];
                for (int i = 0; i < num_thread; i++)
                    vfpu_data2[i] = rsf2_data[i];
                emito_vfpu->notify();
                opcfifo.get();
            }
            else
                wait(vfpu_ready.posedge_event());
            break;

        case lw_ | vload_:
            if (lsu_ready)
            {
                opcins_lsu.write(tmpins);
                emito_lsu->notify();
                opcfifo.get();
            }
            else
                wait(lsu_ready.posedge_event());
            break;
        case beq_:
            rss1_addr.write(tmpins.s1);
            rss2_addr.write(tmpins.s2);
            wait(SC_ZERO_TIME);
            if (rss1_data.read() == rss2_data.read())
            {
                jump_addr = tmpins.d;
                jump = true;
                flush->notify();
            }
            opcfifo.get();
        }
    }
}

void OperandCollector::saluController()
{
    salu_ready = true;
    while (true)
    {
        wait(emito_salu->obtain_event());
        salu_ready = false;
        wait(2 * PERIOD, SC_NS);
        salu_ready = true;
    }
}

void OperandCollector::valuController()
{
    valu_ready = true;
    while (true)
    {
        wait(emito_valu->obtain_event());
        valu_ready = false;
        wait(3 * PERIOD, SC_NS);
        valu_ready = true;
    }
}

void OperandCollector::vfpuController()
{
    vfpu_ready = true;
    while (true)
    {
        wait(emito_vfpu->obtain_event());
        vfpu_ready = false;
        wait(4 * PERIOD, SC_NS);
        vfpu_ready = true;
    }
}

void OperandCollector::lsuController()
{
    lsu_ready = true;
    while (true)
    {
        wait(emito_lsu->obtain_event());
        lsu_ready = false;
        wait(10 * PERIOD, SC_NS);
        lsu_ready = true;
    }
}