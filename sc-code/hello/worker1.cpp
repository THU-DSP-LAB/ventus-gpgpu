#include "hello.h"

void worker1::worker_action()
{
    wait(20, SC_NS);
    cout << "waited for 20ns and notifying event1" << endl;
    acase->notify();
    wait(40, SC_NS);
    cout << "waited for another 40ns and notifying event1" << endl;
    acase->notify();
}