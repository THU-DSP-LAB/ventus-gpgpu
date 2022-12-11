#include "regfile.h"

void regfile::read_scalar()
{
    r_s_data = s_regfile[r_s_addr.read()];
    while (true)
    {
        wait(r_s_addr.value_changed_event() |
             write_s->obtain_event());
        wait(SC_ZERO_TIME);
        r_s_data = s_regfile[r_s_addr.read()];
    }
}

void regfile::read_vector()
{
    for (int i = 0; i < num_thread; i++)
        r_v_data[i] = v_regfile[r_v_addr.read()][i];
    while ((true))
    {
        wait(r_v_addr.value_changed_event() |
             write_v->obtain_event());
        wait(SC_ZERO_TIME);
        for (int i = 0; i < num_thread; i++)
            r_v_data[i] = v_regfile[r_v_addr.read()][i];
    }
}

void regfile::write_scalar()
{
    while (true)
    {
        wait(write_s->obtain_event());
        s_regfile[w_s_addr.read()] = w_s_data;
    }
}

void regfile::write_vector()
{
    while (true)
    {
        wait(write_v->obtain_event());
        for (int i = 0; i < num_thread; i++)
            v_regfile[w_v_addr.read()][i] = w_v_data[i];
    }
}
