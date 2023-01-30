#include "hello.h"
#include "tlm_utils/tlm_quantumkeeper.h"
#include "tlm_core/tlm_1/tlm_req_rsp/tlm_channels/tlm_fifo/tlm_fifo.h"

int sc_main(int argc, char *argv[])
{

  cout << "Hello World " << endl;

  // sc_uint<5> a = 12;
  // int b = 7;
  // bool c = a >= b;
  // if (c)
  //   cout << "a >= b" << endl;
  // else
  //   cout << "a < b" << endl;

  // sc_uint<5> a = 3;
  // sc_bv<4> b = (sc_uint<4>)a.range(3, 0);
  // if (strcmp(b.to_string().c_str(), "0011") == 0)
  //   cout << "b is equal to 0011" << endl;
  // else
  //   cout << "b is not equal to 0011" << endl;

  // sc_uint<5> a = 3;
  // cout << "a in binary is " << (sc_bv<5>)a << endl;
  // cout << "a.reverse in binary is " << ((sc_bv<5>)a).reverse() << endl;

  // sc_uint<5> a = 16; // a = 5'b10000
  // cout << "a >> 1 is " << (a >> 1) << endl; // 5'b01000
  // cout << "a as int >> 1 is " << ((sc_int<5>)a >> 1) << endl; // 5'b11000
  // cout << "a as int >> 1 's MSB is " << ((sc_int<5>)((sc_int<5>)a >> 1))[4] << endl;

  // sc_uint<5> a, b, c;
  // a = 16; b = 15;
  // cout << "a^b is " << (a ^ b) << endl;
  // cout << "size of a is " << sizeof(a) << ". type of a is " << typeid(a).name() << endl;
  // cout << "size of a^b is " << sizeof(a ^ b) << ". type of a^b is " << typeid(a^b).name() << endl;

  // // test for multiple events communication between modules
  // event a1("myevent1");
  // event a2("myevent2");
  // sc_signal<int> value;
  // worker1 W1("myworker1");
  // worker2 W2("myworker2");
  // receiver R("myreceiver");
  // W1.acase(a1);
  // W2.acase(a2);
  // W2.value(value);
  // R.incase1(a1);
  // R.incase2(a2);
  // R.value(value);
  // sc_start();

  // // test for I_TYPE communication
  // sc_signal<sc_uint<8>> addr;
  // sc_signal<I_TYPE> ins;
  // itype_test iii("mytest");
  // itype_testbench ttt("mytb");
  // iii.addr(addr);
  // iii.ins(ins);
  // ttt.addr(addr);
  // ttt.ins(ins);
  // sc_start();

  // 测试event_queue和clk的时间关系，测试数值何时更新
  if (sc_time(SC_ZERO_TIME) == sc_time(0, SC_NS))
    cout << "sc_time(SC_ZERO_TIME) == sc_time(0, SC_NS)" << endl;
  delaytest ddddd("DDDDD");
  sc_clock clk("clk", PERIOD, SC_NS, 0.5, 0, SC_NS, false);
  ddddd.clk(clk);
  sc_start(20, SC_NS);

  return 0; // Terminate simulation
}