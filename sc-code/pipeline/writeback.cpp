#include "writeback.h"

void writeback::write_scalar(){
    while (true)
    {
        wait(salu_finish->obtain_event());
        auto s_data = salu_out.read();
        rds1_data.write(s_data.data);
        rds1_addr.write(s_data.ins.d);
        wb_ins.write(s_data.ins);
        write_s->notify();
        wb_event->notify();
    }
    
}