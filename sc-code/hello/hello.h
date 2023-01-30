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
  sc_out<int> value;

  SC_CTOR(worker2)
  {
    SC_THREAD(worker_action);
  }

  void worker_action()
  {
    wait(40, SC_NS);
    cout << "waited for 40ns and notifying event2, then set value=20" << endl;
    acase->notify();
    value = 20;
    wait(20, SC_NS);
    cout << "waited for another 20ns and set value=40, then notify event2" << endl;
    value = 40;
    acase->notify();
  }
};

SC_MODULE(receiver)
{
  sc_port<event_if> incase1;
  sc_port<event_if> incase2;
  sc_in<int> value;

  SC_CTOR(receiver)
  {
    // SC_THREAD(receiver_action);
    SC_METHOD(receiver_action);
    sensitive << incase1->obtain_event() << incase2->obtain_event();
  }

  void method_action()
  {
    if (incase1->obtain_event().triggered())
      cout << "receiver has received the event1 triggered" << endl;
    if (incase2->obtain_event().triggered())
      cout << "receiver has received the event2 triggered" << endl;
    cout << "value = " << value.read() << " at time " << sc_time_stamp() << endl;
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
      cout << "value = " << value.read() << " at time " << sc_time_stamp() << endl;
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

SC_MODULE(delaytest)
{
  sc_in_clk clk;
  sc_event_queue eq;
  sc_int<32> value = 0;
  sc_signal<int> sigvalue;
  tlm::tlm_fifo<int> fifo, bibo;
  void eventnot()
  {
    while (true)
    {
      wait();
      eq.notify(sc_time(PERIOD, SC_NS));
      cout << "notified by clk.pos, will notify eq in 10ns, now is " << sc_time_stamp() << "\n";
    }
  }
  void updatevalue()
  {
    while (true)
    {
      wait(eq.default_event());
      cout << "notified by eq at time " << sc_time_stamp() << ", old value is " << value << ", old sigvalue is " << sigvalue << "\n";
      cout << "notified by eq, old fifo has " << fifo.used() << " elems in it at time " << sc_time_stamp() << "\n";

      // value = 100;
      sigvalue = sigvalue + 1;
      // sigvalue = 100;
      fifo.put(value);
      cout << "notified by eq, after update, value is " << value << ", sigvalue is " << sigvalue << " at time " << sc_time_stamp() << "\n";
      cout << "notified by eq, after put, fifo has " << fifo.used() << " elems in it at time " << sc_time_stamp() << "\n";
      wait(SC_ZERO_TIME);
      value = value + 1;
      cout << "notified by eq, after SC_ZERO_TIME, value is " << value << ", sigvalue is " << sigvalue << " at time " << sc_time_stamp() << "\n";
      cout << "notified by eq, after SC_ZERO_TIME, fifo has " << fifo.used() << " elems in it at time " << sc_time_stamp() << "\n";
      cout << "notified by eq, after SC_ZERO_TIME, bibo has " << bibo.used() << " elems in it at time " << sc_time_stamp() << "\n";
    }
  }
  void display()
  {
    while (true)
    {
      wait(clk.posedge_event());
      cout<<"ffffffffffffffffff\n";
      cout << "notified by clk.pos, value=" << value << ", sigvalue=" << sigvalue << ", fifo has " << fifo.used() << " elems in it, bibo has " << bibo.used() << " elems at time " << sc_time_stamp() << "\n";
      bibo.put(value);
      cout << "notified by clk.pos, after put, bibo has " << bibo.used() << " elems in it at time " << sc_time_stamp() << "\n";
      wait(SC_ZERO_TIME);
      cout << "notified by clk.pos, after SC_ZERO_TIME, value=" << value << ", sigvalue=" << sigvalue << ", bibo has " << bibo.used() << " elems in it at time " << sc_time_stamp() << "\n";
    }
  }

  delaytest(sc_module_name name_) : sc_module(name_), fifo(10), bibo(10)
  {
    SC_HAS_PROCESS(delaytest);
    SC_THREAD(eventnot);
    sensitive << clk.pos();
    SC_THREAD(updatevalue);
    SC_THREAD(display);
  }
};

#endif
