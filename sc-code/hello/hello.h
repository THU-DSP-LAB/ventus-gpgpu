#ifndef _HELLO_H
#define _HELLO_H

#include "systemc.h"
#include "../parameters.h"

SC_MODULE(worker1)
{
  sc_port<event_if> acase;

  SC_CTOR(worker1)
  {
    SC_THREAD(worker_action);
  }

  void worker_action();
};

SC_MODULE(worker2)
{
  sc_port<event_if> acase;

  SC_CTOR(worker2)
  {
    SC_THREAD(worker_action);
  }

  void worker_action()
  {
    wait(40, SC_NS);
    cout << "waited for 40ns and notifying event2" << endl;
    acase->notify();
    wait(20, SC_NS);
    cout << "waited for another 20ns and notifying event2" << endl;
    acase->notify();
  }
};

SC_MODULE(receiver)
{
  sc_port<event_if> incase1;
  sc_port<event_if> incase2;

  SC_CTOR(receiver)
  {
    SC_THREAD(receiver_action);
  }

  void receiver_action()
  {
    while (true)
    {
      wait(incase1->obtain_event() | incase2->obtain_event());
      if (incase1->obtain_event().triggered())
        cout << "receiver has received the event1 triggered" << endl;
      if (incase2->obtain_event().triggered())
        cout << "receiver has received the event2 triggered" << endl;
    }
  }
};

SC_MODULE(itype_test)
{
  sc_in<sc_uint<8>> addr;
  sc_out<I_TYPE> ins;

  I_TYPE *mem = new I_TYPE[2];

  void test_action()
  {
    // initialize
    mem[0] = I_TYPE(add_, 2, 3, 4);
    mem[1] = I_TYPE(lw_, 6, 7, 8);
    while (true)
    {

      ins = mem[addr.read()];
      // cout << "read ins" << ins << "from mem[" << addr << "]" << endl;
      // 若按照上面的语句，输出的ins不是更新后的ins。
      // 因为SC_THREAD、SC_METHOD每次被激活运行，都是运行结束后间隔一个SC_ZERO_TIME，
      // 再将新的值更新到所有成员变量。
      cout << "read ins" << mem[addr.read()] << "from mem[" << addr << "]" << endl;
      wait();
    }
  }

  SC_CTOR(itype_test)
  {
    SC_THREAD(test_action);
    sensitive << addr;
  }
};

SC_MODULE(itype_testbench)
{
  sc_out<sc_uint<8>> addr;
  sc_in<I_TYPE> ins;

  void gen_addr()
  {
    cout << "addr is initialized as " << addr << endl;
    wait(20, SC_NS);
    cout << "let addr = 1" << endl;
    addr = 1;
  }
  void display_ins()
  {
    while (true)
    {
      wait();
      cout << ins << endl;
    }
    wait(SC_ZERO_TIME);
  }

  SC_CTOR(itype_testbench)
  {
    SC_THREAD(gen_addr);
    SC_THREAD(display_ins);
    sensitive << ins;
  }
};

#endif
