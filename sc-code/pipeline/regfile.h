#ifndef _REGFILE_H
#define _REGFILE_H

#include "../parameters.h"
#include <array>

SC_MODULE(regfile)
{
    sc_in<sc_uint<5>> rss1_addr{"rss1_addr"}; // rs-source, s-scalar
    sc_in<sc_uint<5>> rss2_addr{"rss2_addr"};
    // sc_in<sc_uint<5>> rss3_addr;
    sc_in<sc_uint<5>> rsv1_addr{"rsv1_addr"};
    sc_in<sc_uint<5>> rsv2_addr{"rsv2_addr"};
    // sc_in<sc_uint<5>> rsv3_addr;
    sc_in<sc_uint<5>> rsf1_addr{"rsf1_addr"};
    sc_in<sc_uint<5>> rsf2_addr{"rsf2_addr"};
    sc_in<sc_uint<5>> rds1_addr{"rds1_addr"};
    sc_in<sc_uint<5>> rdv1_addr{"rdv1_addr"};
    sc_in<sc_uint<5>> rdf1_addr{"rdf1_addr"};
    sc_in<reg_t> rds1_data{"rds1_data"};
    sc_vector<sc_in<reg_t>> rdv1_data{"rdv1_data", num_thread};
    sc_vector<sc_in<float>> rdf1_data{"rdf1_data", num_thread};
    sc_port<event_if> write_s;
    sc_port<event_if> write_v;
    sc_port<event_if> write_f;

    // CSR还没加
    sc_out<reg_t> rss1_data;
    sc_out<reg_t> rss2_data;
    // sc_out<sc_int<32>> rss3_data;
    sc_vector<sc_out<reg_t>> rsv1_data{"rsv1_data", num_thread};
    sc_vector<sc_out<reg_t>> rsv2_data{"rsv2_data", num_thread};
    // sc_out<sc_int<32>> rsv3_data[num_thread];
    sc_vector<sc_out<float>> rsf1_data{"rsf1_data", num_thread};
    sc_vector<sc_out<float>> rsf2_data{"rsf2_data", num_thread};

    void read_scalar1();
    void read_scalar2();
    void read_vector1();
    void read_vector2();
    void read_float1();
    void read_float2();
    void write_scalar();
    void write_vector();
    void write_float();
    void init_sreg();

    SC_CTOR(regfile)
    {
        SC_THREAD(read_scalar1);
        SC_THREAD(read_scalar2);
        SC_THREAD(read_vector1);
        SC_THREAD(read_vector2);
        SC_THREAD(read_float1);
        SC_THREAD(read_float2);
        SC_THREAD(write_scalar);
        SC_THREAD(write_vector);
        SC_THREAD(write_float);
        SC_THREAD(init_sreg);
    }

public:
    std::array<reg_t, 32> s_regfile;
    using v_regfile_t = std::array<reg_t, num_thread>;
    std::array<v_regfile_t, 32> v_regfile;
    using f_regfile_t = std::array<float, num_thread>;
    std::array<f_regfile_t, 32> f_regfile;
};

#endif