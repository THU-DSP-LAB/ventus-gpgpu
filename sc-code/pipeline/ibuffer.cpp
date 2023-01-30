#include "ibuffer.h"

void ibuffer::IBUF_ACTION()
{
    I_TYPE read_data;

    while (true)
    {
        ibuf_full = ififo.nb_can_put();
        top_ins = ififo.peek();
        wait(rst_n.negedge_event() |
             fetch_out->obtain_event() |
             dispatch->obtain_event());
        if (rst_n.read() == 0)
            while (ififo.nb_get(read_data))
            {
            }
        else
        {
            if (dispatch->obtain_event().triggered())
            {
                read_data = ififo.get();        // scoreboard can ensure there is data in ibuffer
                ibuf_ins.write(read_data);
            }

            if (fetch_out->obtain_event().triggered())
                ififo.put(fetch_ins);
        }
    }
}