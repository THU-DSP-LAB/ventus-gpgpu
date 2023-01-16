#include "sm.h"


void sm::ibuffer(){
    I_TYPE read_data;

    while (true)
    {
        wait(rst_n.negedge_event() |
             fetch_out |
             dispatch);
        if (rst_n.read() == 0)
            while (ififo.nb_get(read_data))
            {
            }
        else
        {
            if (dispatch.triggered())
            {
                ibuf_ins = ififo.get();
                // scoreboard can ensure there is data in ibuffer
            }

            if (fetch_out.triggered())
                ififo.put(fetch_ins);
        }
    }
}