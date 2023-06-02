#ifndef _HELLO_H
#define _HELLO_H

#include "systemc.h"
#include "../parameters.h"
#include "tlm.h"
#include "tlm_core/tlm_1/tlm_req_rsp/tlm_channels/tlm_fifo/tlm_fifo.h"

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
    mem[0] = I_TYPE((OP_TYPE)1, 2, 3, 4);
    mem[1] = I_TYPE((OP_TYPE)1, 6, 7, 8);
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

// SC_MODULE(delaytest)
// {
//   sc_in_clk clk;
//   sc_event_queue eq;
//   sc_int<32> value = 0;
//   sc_signal<int> sigvalue, sigvalue2;
//   tlm::tlm_fifo<int> fifo, bibo;
//   void eventnot()
//   {
//     while (true)
//     {
//       wait();
//       eq.notify(sc_time(PERIOD, SC_NS));
//       cout << "notified by clk.pos, will notify eq in 10ns, now is " << sc_time_stamp() << "\n";
//     }
//   }
//   void updatevalue()
//   {
//     while (true)
//     {
//       wait(eq.default_event());
//       cout << "notified by eq at time " << sc_time_stamp() << ", old value is " << value << ", old sigvalue is " << sigvalue << ", old sigvalue2=" << sigvalue2 << "\n";
//       cout << "notified by eq, old fifo has " << fifo.used() << " elems in it at time " << sc_time_stamp() << "\n";

//       // value = 100;
//       sigvalue = sigvalue + 1;
//       value = value + 1;
//       // sigvalue = 100;
//       fifo.put(value);
//       cout << "notified by eq, after update, value is " << value << ", sigvalue is " << sigvalue << " at time " << sc_time_stamp() << "\n";
//       cout << "notified by eq, after fifo-put, fifo has " << fifo.used() << " elems in it at time " << sc_time_stamp() << "\n";
//       wait(SC_ZERO_TIME);

//       cout << "notified by eq, after SC_ZERO_TIME, value is " << value << ", sigvalue is " << sigvalue << ", sigvalue2=" << sigvalue2 << " at time " << sc_time_stamp() << "\n";
//       cout << "notified by eq, after SC_ZERO_TIME, fifo has " << fifo.used() << " elems in it at time " << sc_time_stamp() << "\n";
//       cout << "notified by eq, after SC_ZERO_TIME, bibo has " << bibo.used() << " elems in it at time " << sc_time_stamp() << "\n";
//     }
//   }
//   void display()
//   {
//     while (true)
//     {
//       wait(clk.posedge_event());
//       cout << "notified by clk.pos, value=" << value << ", sigvalue=" << sigvalue << ", sigvalue2=" << sigvalue2 << ", fifo has " << fifo.used() << " elems in it, bibo has " << bibo.used() << " elems at time " << sc_time_stamp() << "\n";
//       bibo.put(value);
//       sigvalue2 = sigvalue2 + 1;
//       cout << "notified by clk.pos, after bibo-put and sigvalue2-update, bibo has " << bibo.used() << " elems in it, sigvalue2=" << sigvalue2 << " at time " << sc_time_stamp() << "\n";
//       if (bibo.used() == 3)
//       {
//         int uuuuu;
//         // while (bibo.nb_get(uuuuu))
//         // {
//         // }
//         // cout << "detect bibo.used==3, clear bibo, now bibo.used()=" << bibo.used() << " at " << sc_time_stamp() << endl;
//         uuuuu = bibo.get();
//         uuuuu = bibo.get();
//         cout << "detect bibo.used==3, fetch two elems from bibo, now bibo.used()=" << bibo.used() << " at " << sc_time_stamp() << endl;
//       }
//       wait(SC_ZERO_TIME);
//       cout << "notified by clk.pos, after SC_ZERO_TIME, value=" << value << ", sigvalue=" << sigvalue << ", sigvalue2=" << sigvalue2 << ", bibo has " << bibo.used() << " elems in it at time " << sc_time_stamp() << "\n";
//     }
//   }
//
// delaytest(sc_module_name name_) : sc_module(name_), fifo(10), bibo(10)
// {
//   SC_HAS_PROCESS(delaytest);
//   SC_THREAD(eventnot);
//   sensitive << clk.pos();
//   SC_THREAD(updatevalue);
//   SC_THREAD(display);
// }
// };

SC_MODULE(delaytest)
{
  sc_in_clk clk;
  sc_event_queue eq;
  sc_int<32> value = 0;
  sc_signal<int> sigvalue;
  void eventnot()
  {
    while (true)
    {
      wait();
      eq.notify(sc_time(PERIOD, SC_NS));
      cout << "notified by clk.pos, will notify eq in 10ns, now is " << sc_time_stamp() << ", delta_cycle=" << sc_delta_count_at_current_time() << ".\n";
    }
  }
  void updatevalue()
  {
    while (true)
    {
      wait(eq.default_event());
      cout << "notified by eq at " << sc_time_stamp() << ", old sigvalue is " << sigvalue << ", delta_cycle=" << sc_delta_count_at_current_time() << ".\n";
      sigvalue = sigvalue + 1;
      cout << "notified by eq, after update, sigvalue is " << sigvalue << " at " << sc_time_stamp() << ", delta_cycle=" << sc_delta_count_at_current_time() << ".\n";
      wait(SC_ZERO_TIME);
      cout << "notified by eq, after SC_ZERO_TIME, sigvalue is " << sigvalue << " at " << sc_time_stamp() << ", delta_cycle=" << sc_delta_count_at_current_time() << ".\n";
    }
  }
  void display()
  {
    while (true)
    {
      wait();
      cout << "notified by clk.pos, sigvalue=" << sigvalue << " at " << sc_time_stamp() << ", delta_cycle=" << sc_delta_count_at_current_time() << ".\n";
      wait(SC_ZERO_TIME);
      cout << "notified by clk.pos, after SC_ZERO_TIME, sigvalue=" << sigvalue << " at " << sc_time_stamp() << ", delta_cycle=" << sc_delta_count_at_current_time() << ".\n";
    }
  }

  delaytest(sc_module_name name_) : sc_module(name_)
  {
    SC_HAS_PROCESS(delaytest);
    SC_THREAD(eventnot);
    sensitive << clk.pos();
    SC_THREAD(updatevalue);
    SC_THREAD(display);
    sensitive << clk.pos();
  }
};

struct my_struct
{
  bool ready;
  std::array<int, 4> data;
  my_struct(){};
  my_struct(bool ready_, std::array<int, 4> &data_) : ready(ready_), data(data_){};
  friend ostream &operator<<(ostream &os, my_struct const &v)
  {
    os << "(" << v.ready << ";" << v.data[0] << ","
       << v.data[1] << "," << v.data[2] << "," << v.data[3] << ")";
    return os;
  }
  bool operator==(const my_struct &rhs) const
  {
    return rhs.ready == ready && rhs.data == data;
  }
};

SC_MODULE(timing)
{
  sc_in_clk clk;
  std::array<my_struct, 10> my_array;
  void write()
  {
    int index = 0;
    std::array<int, 4> write_data;
    while (true)
    {
      wait(clk.posedge_event());
      wait(SC_ZERO_TIME); // wait for read_oldvalue() to execute
      write_data.fill(index);
      my_array[index % 10] = my_struct(index % 2, write_data);
      index++;
    }
  }
  void read_oldvalue()
  {
    while (true)
    {
      wait(clk.posedge_event());
      for (int idx = 0; idx < 10; idx++)
      {
        cout << my_array[idx];
      }
      cout << endl;
    }
  }
  void read_newvalue()
  {
    while (true)
    {
      wait(clk.posedge_event());
      wait(SC_ZERO_TIME); // wait for read_oldvalue() to execute
      wait(SC_ZERO_TIME); // wait for write() to execute
      for (int idx = 0; idx < 10; idx++)
      {
        cout << my_array[idx];
      }
      cout << endl;
    }
  }
  SC_CTOR(timing)
  {
    SC_THREAD(write);
    SC_THREAD(read_oldvalue);
    SC_THREAD(read_newvalue);
  }
};

SC_MODULE(timing2)
{
  sc_in_clk clk;
  sc_core::sc_vector<sc_core::sc_signal<my_struct>> my_array{"my_array", 10};
  int index = 0;
  sc_event my_event;
  sc_signal<int> value;
  void write()
  {
    std::array<int, 4> write_data;
    write_data.fill(index);
    my_array[index % 10].write(my_struct(index % 2, write_data));
    index++;
    value = index;
    my_event.notify();
  }
  void read_oldvalue()
  {
    while (true)
    {
      /* code */
      wait();
      cout << "old value=" << value << " at " << sc_time_stamp() << endl;
    }
  }
  void read_newvalue()
  {
    while (true)
    {
      /* code */
      wait(my_event);
      cout << "new value=" << value << " at " << sc_time_stamp() << endl;
    }
  }
  SC_CTOR(timing2)
  {
    SC_METHOD(write);
    dont_initialize();
    sensitive << clk.pos();
    SC_THREAD(read_oldvalue);
    sensitive << clk.pos();
    SC_THREAD(read_newvalue);
    // for (auto &s : my_array)
    //   sensitive << s;
  }
};

SC_MODULE(fifotest)
{
  sc_in_clk clk;
  tlm::tlm_fifo<int> fifo;
  sc_event ev_display;
  sc_signal<int> i;
  void updatefifo()
  {
    i = 0;
    while (true)
    {
      /* code */
      wait(clk.posedge_event());
      i = i + 1;
      fifo.put(i);
      // wait(SC_ZERO_TIME);
      ev_display.notify();
    }
  }

  void displayfifo()
  {
    while (true)
    {
      /* code */
      wait(ev_display);
      cout << "fifo.nb_can_put()=" << fifo.nb_can_put() << ", fifo.elem_num=" << fifo.used() << ", i=" << i << "\n";
    }
  }

  fifotest(sc_module_name name_) : sc_module(name_), fifo(10)
  {
    SC_HAS_PROCESS(fifotest);
    SC_THREAD(updatefifo);
    SC_THREAD(displayfifo);
  }
};

SC_MODULE(eventqueue_test)
{
  sc_in_clk clk;
  sc_event_queue eq;
  void gen()
  {
    while (true)
    {
      /* code */
      wait();
      int a = 0;
      eq.notify(a, SC_NS);
    }
  }
  void display()
  {
    while (true)
    {
      /* code */
      wait(eq.default_event());
      cout << "eq triggered at " << sc_time_stamp() << "," << sc_delta_count_at_current_time() << endl;
    }
  }
  SC_CTOR(eventqueue_test)
  {
    SC_THREAD(gen);
    sensitive << clk.pos();
    SC_THREAD(display);
  }
};

SC_MODULE(eq_nothing_test)
{
  sc_in_clk clk;
  sc_event_queue eq;
  sc_event nothing, nothing2;
  void gen()
  {
    while (true)
    {
      /* code */
      wait();
      int a = 10;
      eq.notify(a, SC_NS);
      cout << "gen notifying nothing at " << sc_time_stamp() << "," << sc_delta_count_at_current_time() << endl;
      nothing.notify();
    }
  }
  void gen2()
  {
    while (true)
    {
      /* code */
      wait(nothing);
      int a = 10;
      eq.notify(a, SC_NS);
      cout << "gen2 notifying nothing2 at " << sc_time_stamp() << "," << sc_delta_count_at_current_time() << endl;
      nothing2.notify();
    }
  }
  void OUT()
  {
    while (true)
    {
      wait(eq.default_event() | nothing2);
      if (eq.default_event().triggered())
      {
        wait(SC_ZERO_TIME);
        cout << "triggered by eq at " << sc_time_stamp() << "," << sc_delta_count_at_current_time() << endl;
      }
      else if (nothing2.triggered())
        cout << "triggered by nothing2 at " << sc_time_stamp() << "," << sc_delta_count_at_current_time() << endl;
    }
  }
  SC_CTOR(eq_nothing_test)
  {
    SC_THREAD(gen);
    sensitive << clk.pos();
    SC_THREAD(gen2);
    sensitive << clk.pos();
    SC_THREAD(OUT);
  }
};

SC_MODULE(triggered_test)
{
  sc_in_clk clk;
  sc_event_queue eq;
  sc_event ev;

  void genev()
  {
    while (true)
    {
      wait(clk.posedge_event());
      eq.notify(10, SC_NS);
      ev.notify();
    }
  }
  void display()
  {
    while (true)
    {
      // wait(clk.posedge_event());
      wait(eq.default_event() & ev);
      if (eq.default_event().triggered())
        cout << "eq at " << sc_time_stamp() << ", delta_cycle=" << sc_delta_count_at_current_time() << ".\n";
      if (ev.triggered())
        cout << "ev at " << sc_time_stamp() << ", delta_cycle=" << sc_delta_count_at_current_time() << ".\n";
    }
  }
  SC_CTOR(triggered_test)
  {
    SC_THREAD(genev);
    SC_THREAD(display);
  }
};

SC_MODULE(example)
{
  sc_in_clk clk;
  void func1()
  {
    while (true)
    {
      wait(clk.posedge_event());
      a = a + 1;
      cout << "b=" << b << " at " << sc_time_stamp() << "\n";
    }
  };
  void func2()
  {
    while (true)
    {
      wait(clk.posedge_event());
      b = b + 1;
      cout << "a=" << a << " at " << sc_time_stamp() << "\n";
    }
  };
  example(sc_module_name name) : sc_module(name)
  {
    SC_HAS_PROCESS(example);
    SC_THREAD(func1);
    SC_THREAD(func2);
  }
  sc_signal<int> a, b;
};

SC_MODULE(STAGE)
{
  SC_CTOR(STAGE)
  { // elaboration
    std::cout << sc_time_stamp() << ": Elaboration: constructor" << std::endl;
    SC_THREAD(thread); // initialization + simulation
  };
  ~STAGE()
  { // cleanup
    std::cout << sc_time_stamp() << ": Cleanup: desctructor" << std::endl;
  }
  void thread()
  {
    std::cout << sc_time_stamp() << ": Execution.initialization" << std::endl;
    int i = 0;
    while (true)
    {
      wait(1, SC_SEC);                                                       // advance-time
      std::cout << sc_time_stamp() << ": Execution.simulation" << std::endl; // evaluation
      if (++i >= 2)
      {
        sc_stop(); // stop simulation after 2 iterations
      }
    }
  }
  void before_end_of_elaboration()
  {
    std::cout << "before end of elaboration" << std::endl;
  }
  void end_of_elaboration()
  {
    std::cout << "end of elaboration" << std::endl;
  }
  void start_of_simulation()
  {
    std::cout << "start of simulation" << std::endl;
  }
  void end_of_simulation()
  {
    std::cout << "end of simulation" << std::endl;
  }
  void printinmain()
  {
    std::cout << "in sc_main call" << std::endl;
  }
};

#endif
