#include "ibuffer.h"

void ibuffer::IBUF_ACTION()
{
    I_TYPE read_data;

    while (true)
    {
        wait(rst_n.negedge_event() |
             fetch_out->obtain_event() |
             read_ins->obtain_event());
        if (rst_n.read() == 0)
            while (ififo.nb_read(read_data))
            {
            }
        else
        {
            if (read_ins->obtain_event().triggered())
            {
                read_data = ififo.read();
                ibuf_instruction.write(read_data);
                emit_ins->notify();baidu
            }

            if (fetch_out->obtain_event().triggered())
                ififo.write(fetch_instruction);
        }
    }
}