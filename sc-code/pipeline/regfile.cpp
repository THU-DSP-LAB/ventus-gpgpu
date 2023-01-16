#include "regfile.h"

void regfile::read_scalar1()
{
    rss1_data = s_regfile[rss1_addr.read()];
    while (true)
    {
        wait(rss1_addr.value_changed_event() |
             write_s->obtain_event());
        rss1_data = s_regfile[rss1_addr.read()];
    }
}

void regfile::read_scalar2()
{
    rss2_data = s_regfile[rss2_addr.read()];
    while (true)
    {
        wait(rss2_addr.value_changed_event() |
             write_s->obtain_event());
        rss2_data = s_regfile[rss2_addr.read()];
    }
}

void regfile::read_vector1()
{
    auto &elem1 = v_regfile[rsv1_addr.read()];
    for (int i = 0; i < num_thread; i++)
        rsv1_data[i] = elem1[i];
    while ((true))
    {
        wait(rsv1_addr.value_changed_event() |
             write_v->obtain_event());
        auto &elem2 = v_regfile[rsv1_addr.read()];
        for (int i = 0; i < num_thread; i++)
            rsv1_data[i] = elem2[i];
    }
}

void regfile::read_vector2()
{
    auto &elem1 = v_regfile[rsv2_addr.read()];
    for (int i = 0; i < num_thread; i++)
        rsv2_data[i] = elem1[i];
    while ((true))
    {
        wait(rsv2_addr.value_changed_event() |
             write_v->obtain_event());
        auto &elem2 = v_regfile[rsv2_addr.read()];
        for (int i = 0; i < num_thread; i++)
            rsv2_data[i] = elem2[i];
    }
}

void regfile::read_float1()
{
    auto &elem1 = f_regfile[rsf1_addr.read()];
    for (int i = 0; i < num_thread; i++)
        rsf1_data[i] = elem1[i];
    while ((true))
    {
        wait(rsf1_addr.value_changed_event() |
             write_f->obtain_event());
        auto &elem2 = f_regfile[rsf1_addr.read()];
        for (int i = 0; i < num_thread; i++)
            rsf1_data[i] = elem2[i];
    }
}

void regfile::read_float2()
{
    auto &elem1 = f_regfile[rsf2_addr.read()];
    for (int i = 0; i < num_thread; i++)
        rsf2_data[i] = elem1[i];
    while ((true))
    {
        wait(rsf2_addr.value_changed_event() |
             write_f->obtain_event());
        auto &elem2 = f_regfile[rsf2_addr.read()];
        for (int i = 0; i < num_thread; i++)
            rsf2_data[i] = elem2[i];
    }
}

void regfile::write_scalar()
{
    while (true)
    {
        wait(write_s->obtain_event());
        s_regfile[rds1_addr.read()] = rds1_data;
    }
}

void regfile::write_vector()
{
    while (true)
    {
        wait(write_v->obtain_event());
        for (int i = 0; i < num_thread; i++)
            v_regfile[rdv1_addr.read()][i] = rdv1_data[i];
    }
}

void regfile::write_float()
{
    while (true)
    {
        wait(write_f->obtain_event());
        for (int i = 0; i < num_thread; i++)
            f_regfile[rdf1_addr.read()][i] = rdf1_data[i];
    }
}

void regfile::init_sreg()
{
    s_regfile[0] = 22;
    s_regfile[1] = 6;
}
