#ifndef POINTER_H_
#define POINTER_H_

#include "systemc.h"
#include "../parameters.h"

SC_MODULE(POINTER)
{
public:
    sc_in_clk clk{"clk"};
    using v_regfile_t = std::array<int, num_thread>;
    std::array<v_regfile_t, 32> v_regfile;
    float floatmem;
    float *floatptr;
    sc_signal<float> rsf1_data{"rsf1_data"};
    int i;

    void INIT()
    {
        floatmem = 3.2;
        floatptr = &floatmem;
        v_regfile[0].fill(1);
        v_regfile[1].fill(5);
        v_regfile[2].fill(785);
        v_regfile[3].fill(1543641023);
        v_regfile[4].fill(55666);
        i = 0;
    }
    void ACT()
    {
        while (true)
        {
            wait();
            int *pa = &v_regfile[i][0];
            rsf1_data = *((float *)pa);
            i = i + 1;
        }
    }
    void DISPLAY()
    {
        float *p1;
        while (true)
        {
            wait();
            
            cout << "rsf1data=" << rsf1_data << " at " << sc_time_stamp() << endl;
        }
    }

    SC_CTOR(POINTER)
    {
        SC_METHOD(INIT);
        SC_THREAD(ACT);
        sensitive << clk.pos();
        SC_THREAD(DISPLAY);
        sensitive << clk.pos();
    }
};

#endif