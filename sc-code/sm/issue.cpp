#include "sm.h"

void sm::issue()
{
    if (can_dispatch && opc_full == false)
    {
        issue_ins = ibuf_ins;
        dispatch.notify();
    }
}