#include "sm.h"

SC_MODULE(regfile)
{
    sc_in<sc_uint<5>> rsv1_addr;

    sc_out<sc_int<32>> rsv1_data[8];

    void read_vector();

    SC_CTOR(regfile)
    {
        SC_THREAD(read_vector);
    }

private:
    sc_int<32> v_regfile[32][8];    // suppose data stored in it
};

void regfile::read_vector()
{
    for (int i = 0; i < 8; i++)
        rsv1_data[i] = v_regfile[rsv1_addr.read()][i];
    while ((true))
    {
        wait(rsv1_addr.value_changed_event());
        for (int i = 0; i < num_thread; i++)
            rsv1_data[i] = v_regfile[rsv1_addr.read()][i];
    }
}