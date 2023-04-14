#include "BASE.h"

void BASE::SALU_IN()
{
    I_TYPE new_ins;
    salu_in_t new_data;
    int a_delay, b_delay;
    while (true)
    {
        wait();
        for (auto &warp_ : WARPS)
        {
            warp_.jump = false;
            warp_.branch_sig = false;
        }
        if (emito_salu)
        {
            // cout << "SALU_IN: receive ins=" << emit_ins << "warp" << emitins_warpid << " at " << sc_time_stamp() << "," << sc_delta_count_at_current_time() << "\n";
            if (salu_ready_old == false)
            {
                cout << "salu error: not ready at " << sc_time_stamp() << "," << sc_delta_count_at_current_time() << "\n";
            }
            salu_unready.notify();
            switch (emit_ins.read().op)
            {
            case beq_:
                WARPS[emitins_warpid].branch_sig = true;
                b_delay = 0;
                if (b_delay == 0)
                    salu_evb.notify();
                else
                {
                    salu_eqb.notify(sc_time((b_delay)*PERIOD, SC_NS));
                    ev_saluready_updated.notify();
                }
                if (rss1_data == rss2_data)
                {
                    WARPS[emitins_warpid].jump = 1;
                    WARPS[emitins_warpid].jump_addr = emit_ins.read().jump_addr;
                    cout << "warp" << emitins_warpid << ": jump is updated to 1 at " << sc_time_stamp() << "," << sc_delta_count_at_current_time() << "\n";
                }
                if (!salueqa_triggered)
                    ev_salufifo_pushed.notify();
                break;
            case add_:
            case addi_:
                new_data.ins = emit_ins;
                new_data.warp_id = emitins_warpid;
                new_data.rss1_data = rss1_data;
                new_data.rss2_data = rss2_data;
                salu_dq.push(new_data);
                // cout << "salu_dq has just pushed 1 elem at " << sc_time_stamp() <<","<< sc_delta_count_at_current_time() << "\n";
                a_delay = 1;
                b_delay = 1;
                if (a_delay == 0)
                    salu_eva.notify();
                else if (salueqa_triggered)
                    salu_eqa.notify(sc_time((a_delay)*PERIOD, SC_NS));
                else
                {
                    salu_eqa.notify(sc_time((a_delay)*PERIOD, SC_NS));
                    ev_salufifo_pushed.notify();
                }
                if (b_delay == 0)
                    salu_evb.notify();
                else
                { // 这都是emit的情况，所以这个cycle eqb不可能被触发
                    salu_eqb.notify(sc_time((b_delay)*PERIOD, SC_NS));
                    ev_saluready_updated.notify();
                }
                // cout << "SALU_IN switch to add_ (from opc input) at " << sc_time_stamp() <<","<< sc_delta_count_at_current_time() << "\n";
                break;
            default:
                cout << "salu error: receive wrong ins " << emit_ins << " at " << sc_time_stamp() << "," << sc_delta_count_at_current_time() << "\n";
                break;
            }
        }
        else
        {
            if (!salueqa_triggered)
                ev_salufifo_pushed.notify();
            if (!salueqb_triggered)
                ev_saluready_updated.notify();
        }
    }
}

void BASE::SALU_CALC()
{
    salufifo_elem_num = 0;
    salufifo_empty = 1;
    salueqa_triggered = false;
    bool succeed;
    while (true)
    {
        wait(salu_eva | salu_eqa.default_event());
        salutmp1 = salu_dq.front();
        // cout << "salu_dq.front's ins is " << salutmp1.ins << ", data is " << salutmp1.rss1_data << "," << salutmp1.rss2_data << "\n";
        salu_dq.pop();
        // cout << "salu_dq has poped, now its elem_num is " << salu_dq.size() << " at " << sc_time_stamp() <<","<< sc_delta_count_at_current_time() << "\n";
        switch (salutmp1.ins.op)
        {
        case add_:
            salutmp2.ins = salutmp1.ins;
            salutmp2.warp_id = salutmp1.warp_id;
            salutmp2.data = salutmp1.rss1_data + salutmp1.rss2_data;
            // cout << "SALU_OUT: do add at " << sc_time_stamp() <<","<< sc_delta_count_at_current_time() << "\n";
            salufifo.push(salutmp2);
            break;
        case addi_:
            salutmp2.ins = salutmp1.ins;
            salutmp2.warp_id = salutmp1.warp_id;
            salutmp2.data = salutmp1.rss1_data + salutmp1.ins.s2;
            // cout << "SALU_OUT: do add at " << sc_time_stamp() <<","<< sc_delta_count_at_current_time() << "\n";
            salufifo.push(salutmp2);
            break;
        }
        if (salu_eqa.default_event().triggered())
        {
            salueqa_triggered = true;
            wait(SC_ZERO_TIME);
            salueqa_triggered = false;
            ev_salufifo_pushed.notify();
        }
        else
            ev_salufifo_pushed.notify();
    }
}

void BASE::SALU_OUT()
{
    while (true)
    {
        wait();
    }
}

void BASE::SALU_CTRL()
{
    salu_ready = true;
    salu_ready_old = true;
    salueqb_triggered = false;
    while (true)
    {
        // unready和eqb不可能在同一个cycle被触发
        // 因unready表示接收了新指令，而eqb触发意味着SALU此时才从busy变ready
        // 同理，evb和eqb也不可能在同一个cycle
        wait(salu_eqb.default_event() | salu_unready | salu_evb);
        if (salu_eqb.default_event().triggered())
        {
            // eq的触发发生在delta 0
            salu_ready = true;
            salu_ready_old = salu_ready;
            salueqb_triggered = true;
            wait(SC_ZERO_TIME);
            salueqb_triggered = false;
            ev_saluready_updated.notify();
        }
        else if (salu_evb.triggered())
        {
            salu_ready = true;
            salu_ready_old = salu_ready;
            ev_saluready_updated.notify();
        }
        else if (salu_unready.triggered())
        { // else if很重要，对于b_delay=0的情况，salu_ready不会变0
            salu_ready = false;
            salu_ready_old = salu_ready;
            ev_saluready_updated.notify();
        }
    }
}

void BASE::VALU_IN()
{
    I_TYPE new_ins;
    valu_in_t new_data;
    int a_delay, b_delay;
    while (true)
    {
        wait();
        if (emito_valu)
        {
            if (valu_ready_old == false)
            {
                cout << "valu error: not ready at " << sc_time_stamp() << "," << sc_delta_count_at_current_time() << "\n";
            }
            valu_unready.notify();
            switch (emit_ins.read().op)
            {
            case vaddvv_:
                new_data.ins = emit_ins;
                new_data.warp_id = emitins_warpid;
                for (int i = 0; i < num_thread; i++)
                {
                    new_data.rsv1_data[i] = rsv1_data[i];
                    new_data.rsv2_data[i] = rsv2_data[i];
                }
                valu_dq.push(new_data);
                a_delay = 5;
                b_delay = 1;
                // cout << "valu: receive vaddvv_, will notify eq, at " << sc_time_stamp() <<","<< sc_delta_count_at_current_time() << "\n";
                if (a_delay == 0)
                    valu_eva.notify();
                else if (valueqa_triggered)
                    valu_eqa.notify(sc_time((a_delay)*PERIOD, SC_NS));
                else
                {
                    valu_eqa.notify(sc_time((a_delay)*PERIOD, SC_NS));
                    ev_valufifo_pushed.notify();
                }
                if (b_delay == 0)
                    valu_evb.notify();
                else
                {
                    valu_eqb.notify(sc_time((b_delay)*PERIOD, SC_NS));
                    ev_valuready_updated.notify();
                }
                break;
            case vaddvx_:
                new_data.ins = emit_ins;
                new_data.warp_id = emitins_warpid;
                for (int i = 0; i < num_thread; i++)
                {
                    new_data.rsv1_data[i] = rsv1_data[i];
                }
                new_data.rss2_data = rss2_data;
                valu_dq.push(new_data);
                a_delay = 5;
                b_delay = 1;
                // cout << "valu: receive vaddvx_, will notify eq, at " << sc_time_stamp() <<","<< sc_delta_count_at_current_time() << "\n";
                if (a_delay == 0)
                    valu_eva.notify();
                else if (valueqa_triggered)
                    valu_eqa.notify(sc_time((a_delay)*PERIOD, SC_NS));
                else
                {
                    valu_eqa.notify(sc_time((a_delay)*PERIOD, SC_NS));
                    ev_valufifo_pushed.notify();
                }
                if (b_delay == 0)
                    valu_evb.notify();
                else
                {
                    valu_eqb.notify(sc_time((b_delay)*PERIOD, SC_NS));
                    ev_valuready_updated.notify();
                }
                break;
            default:
                cout << "valu error: receive wrong ins " << emit_ins << " at " << sc_time_stamp() << "," << sc_delta_count_at_current_time() << "\n";
                break;
            }
        }
        else
        {
            if (!valueqa_triggered)
                ev_valufifo_pushed.notify();
            if (!valueqb_triggered)
                ev_valuready_updated.notify();
        }
    }
}

void BASE::VALU_CALC()
{
    valufifo_elem_num = 0;
    valufifo_empty = 1;
    valueqa_triggered = false;
    valu_in_t valutmp1;
    valu_out_t valutmp2;
    bool succeed;
    while (true)
    {
        wait(valu_eva | valu_eqa.default_event());
        // cout << "valu_eqa.default_event triggered at " << sc_time_stamp() << "," << sc_delta_count_at_current_time() << "\n";
        valutmp1 = valu_dq.front();
        valu_dq.pop();
        switch (valutmp1.ins.op)
        {
        case vaddvv_:
            valutmp2.ins = valutmp1.ins;
            valutmp2.warp_id = valutmp1.warp_id;
            for (int i = 0; i < num_thread; i++)
            {
                valutmp2.rdv1_data[i] = valutmp1.rsv1_data[i] + valutmp1.rsv2_data[i];
            }
            valufifo.push(valutmp2);
            break;
        case vaddvx_:
            valutmp2.ins = valutmp1.ins;
            valutmp2.warp_id = valutmp1.warp_id;
            for (int i = 0; i < num_thread; i++)
            {
                valutmp2.rdv1_data[i] = valutmp1.rsv1_data[i] + valutmp1.rss2_data;
            }
            valufifo.push(valutmp2);
            break;
        }
        if (valu_eqa.default_event().triggered())
        {
            valueqa_triggered = true;
            wait(SC_ZERO_TIME);
            valueqa_triggered = false;
            ev_valufifo_pushed.notify();
        }
        else
            ev_valufifo_pushed.notify();
    }
}

void BASE::VALU_OUT()
{
    while (true)
    {
        wait();
    }
}

void BASE::VALU_CTRL()
{
    valu_ready = true;
    valu_ready_old = true;
    valueqb_triggered = false;
    while (true)
    {
        wait(valu_eqb.default_event() | valu_unready | valu_evb);
        if (valu_eqb.default_event().triggered())
        {
            valu_ready = true;
            valu_ready_old = valu_ready;
            valueqb_triggered = true;
            wait(SC_ZERO_TIME);
            valueqb_triggered = false;
            ev_valuready_updated.notify();
        }
        else if (valu_evb.triggered())
        {
            valu_ready = true;
            valu_ready_old = valu_ready;
            ev_valuready_updated.notify();
        }
        else if (valu_unready.triggered())
        {
            valu_ready = false;
            valu_ready_old = valu_ready;
            ev_valuready_updated.notify();
        }
    }
}

void BASE::VFPU_IN()
{
    I_TYPE new_ins;
    vfpu_in_t new_data;
    int a_delay, b_delay;
    FloatAndInt fi1_;
    while (true)
    {
        wait();
        if (emito_vfpu)
        {
            if (vfpu_ready_old == false)
            {
                cout << "vfpu error: not ready at " << sc_time_stamp() << "," << sc_delta_count_at_current_time() << "\n";
            }
            vfpu_unready.notify();
            switch (emit_ins.read().op)
            {
            case vfaddvv_:
                new_data.ins = emit_ins;
                new_data.warp_id = emitins_warpid;
                // cout << "VFPU_IN: receive ins" << emit_ins << "warp" << emitins_warpid << ", rs1_data={";
                for (int i = 0; i < num_thread; i++)
                {
                    new_data.rsf1_data[i] = rsf1_data[i].read();
                    // fi1_.f = rsf1_data[i];
                    // cout << std::hex << fi1_.i << std::dec << ",";
                }
                // cout << "}, rs2_data={";
                for (int i = 0; i < num_thread; i++)
                {
                    new_data.rsf2_data[i] = rsf2_data[i].read();
                    // fi1_.f = rsf2_data[i];
                    // cout << std::hex << fi1_.i << std::dec << ",";
                }
                // cout << "}, at " << sc_time_stamp() << "," << sc_delta_count_at_current_time() << "\n";
                vfpu_dq.push(new_data);
                a_delay = 5;
                b_delay = 1;
                if (a_delay == 0)
                    vfpu_eva.notify();
                else if (vfpueqa_triggered)
                    vfpu_eqa.notify(sc_time((a_delay)*PERIOD, SC_NS));
                else
                {
                    vfpu_eqa.notify(sc_time((a_delay)*PERIOD, SC_NS));
                    ev_vfpufifo_pushed.notify();
                }
                if (b_delay == 0)
                    vfpu_evb.notify();
                else
                {
                    vfpu_eqb.notify(sc_time((b_delay)*PERIOD, SC_NS));
                    ev_vfpuready_updated.notify();
                }
                break;
            default:
                cout << "vfpu error: receive wrong ins " << emit_ins << " at " << sc_time_stamp() << "," << sc_delta_count_at_current_time() << "\n";
            }
        }
        else
        {
            if (!vfpueqa_triggered)
                ev_vfpufifo_pushed.notify();
            if (!vfpueqb_triggered)
                ev_vfpuready_updated.notify();
        }
    }
}

void BASE::VFPU_CALC()
{
    vfpufifo_elem_num = 0;
    vfpufifo_empty = true;
    vfpueqa_triggered = false;
    vfpu_in_t vfputmp1;
    vfpu_out_t vfputmp2;
    bool succeed;
    while (true)
    {
        wait(vfpu_eva | vfpu_eqa.default_event());

        vfputmp1 = vfpu_dq.front();
        vfpu_dq.pop();
        switch (vfputmp1.ins.op)
        {
        case vfaddvv_:
            vfputmp2.ins = vfputmp1.ins;
            vfputmp2.warp_id = vfputmp1.warp_id;
            for (int i = 0; i < num_thread; i++)
            {
                vfputmp2.rdf1_data[i] = vfputmp1.rsf1_data[i] + vfputmp1.rsf2_data[i];
            }
            vfpufifo.push(vfputmp2);
            break;
        }
        if (vfpu_eqa.default_event().triggered())
        {
            vfpueqa_triggered = true;
            wait(SC_ZERO_TIME);
            vfpueqa_triggered = false;
            ev_vfpufifo_pushed.notify();
        }
        else
            ev_vfpufifo_pushed.notify();
    }
}

void BASE::VFPU_OUT()
{
    while (true)
    {
        wait();
    }
}

void BASE::VFPU_CTRL()
{
    vfpu_ready = true;
    vfpu_ready_old = true;
    vfpueqb_triggered = false;
    while (true)
    {
        wait(vfpu_eqb.default_event() | vfpu_unready | vfpu_evb);
        if (vfpu_eqb.default_event().triggered())
        {
            vfpu_ready = true;
            vfpu_ready_old = vfpu_ready;
            vfpueqb_triggered = true;
            wait(SC_ZERO_TIME);
            vfpueqb_triggered = false;
            ev_vfpuready_updated.notify();
        }
        else if (vfpu_evb.triggered())
        {
            vfpu_ready = true;
            vfpu_ready_old = vfpu_ready;
            ev_vfpuready_updated.notify();
        }
        else if (vfpu_unready.triggered())
        {
            vfpu_ready = false;
            vfpu_ready_old = vfpu_ready;
            ev_vfpuready_updated.notify();
        }
    }
}

void BASE::LSU_IN()
{
    I_TYPE new_ins;
    lsu_in_t new_data;
    int a_delay, b_delay;
    while (true)
    {
        wait();
        if (emito_lsu)
        {
            if (lsu_ready_old == false)
            {
                cout << "lsu error: not ready at " << sc_time_stamp() << "," << sc_delta_count_at_current_time() << "\n";
            }
            lsu_unready.notify();
            switch (emit_ins.read().op)
            {
            case lw_:
                // cout << "LSU receive lw ins=" << emit_ins << "warp" << emitins_warpid << " at " << sc_time_stamp() << "," << sc_delta_count_at_current_time() << "\n";
                new_data.ins = emit_ins;
                new_data.warp_id = emitins_warpid;
                new_data.rss1_data = rss1_data;
                lsu_dq.push(new_data);
                a_delay = 5;
                b_delay = 3;
                if (a_delay == 0)
                    lsu_eva.notify();
                else if (lsueqa_triggered)
                    lsu_eqa.notify(sc_time((a_delay)*PERIOD, SC_NS));
                else
                {
                    lsu_eqa.notify(sc_time((a_delay)*PERIOD, SC_NS));
                    ev_lsufifo_pushed.notify();
                }
                if (b_delay == 0)
                    lsu_evb.notify();
                else
                {
                    lsu_eqb.notify(sc_time((b_delay)*PERIOD, SC_NS));
                    ev_lsuready_updated.notify();
                }
                break;
            case vle32v_:
                // cout << "LSU: receive vle32v at " << sc_time_stamp() << "," << sc_delta_count_at_current_time() << "\n";
                new_data.ins = emit_ins;
                new_data.warp_id = emitins_warpid;
                new_data.rss1_data = rss1_data;
                lsu_dq.push(new_data);
                a_delay = 6;
                b_delay = 3;
                if (a_delay == 0)
                    lsu_eva.notify();
                else if (lsueqa_triggered)
                    lsu_eqa.notify(sc_time((a_delay)*PERIOD, SC_NS));
                else
                {
                    lsu_eqa.notify(sc_time((a_delay)*PERIOD, SC_NS));
                    ev_lsufifo_pushed.notify();
                }
                if (b_delay == 0)
                    lsu_evb.notify();
                else
                {
                    lsu_eqb.notify(sc_time((b_delay)*PERIOD, SC_NS));
                    ev_lsuready_updated.notify();
                }
                break;
            default:
                cout << "lsu error: receive wrong ins " << emit_ins << " at " << sc_time_stamp() << "," << sc_delta_count_at_current_time() << "\n";
                break;
            }
        }
        else
        {
            if (!lsueqa_triggered)
                ev_lsufifo_pushed.notify();
            if (!lsueqb_triggered)
                ev_lsuready_updated.notify();
        }
    }
}

void BASE::LSU_CALC()
{
    lsufifo_elem_num = 0;
    lsufifo_empty = 1;
    lsueqa_triggered = false;
    lsu_in_t lsutmp1;
    lsu_out_t lsutmp2;
    bool succeed;
    int external_addr;
    while (true)
    {
        wait(lsu_eva | lsu_eqa.default_event());
        // cout << "LSU_OUT: triggered by eva/eqa at " << sc_time_stamp() << "," << sc_delta_count_at_current_time() << "\n";
        lsutmp1 = lsu_dq.front();
        lsu_dq.pop();
        switch (lsutmp1.ins.op)
        {
        case lw_:
            // cout << "LSU_CALC: calc lw, rss1=" << lsutmp1.rss1_data
            //      << ", ins=" << lsutmp1.ins << "warp" << lsutmp1.warp_id << " at " << sc_time_stamp() << "," << sc_delta_count_at_current_time() << "\n";
            lsutmp2.ins = lsutmp1.ins;
            lsutmp2.warp_id = lsutmp1.warp_id;
            external_addr = (lsutmp1.rss1_data + lsutmp1.ins.s2) / 4 - 128 * num_thread;
            // cout << "LSU_CALC: read lw rss1_data=" << lsutmp1.rss1_data
            //      << ", external addr=" << external_addr
            //      << ", ins=" << lsutmp1.ins
            //      << ", external_mem[]=" << external_mem[external_addr]
            //      << " at " << sc_time_stamp() << "," << sc_delta_count_at_current_time() << "\n";
            lsutmp2.rds1_data = external_mem[external_addr];
            lsufifo.push(lsutmp2);
            // cout << "LSU_CALC: lw, pushed " << lsutmp2.rds1_data << " to s_regfile rd=" << lsutmp1.ins.d << " at " << sc_time_stamp() << "," << sc_delta_count_at_current_time() << "\n";
            break;
        case vle32v_:
            lsutmp2.ins = lsutmp1.ins;
            lsutmp2.warp_id = lsutmp1.warp_id;
            // cout << "LSU_CALC: calc vle32v, rss1=" << lsutmp1.rss1_data << " at " << sc_time_stamp() << "," << sc_delta_count_at_current_time() << "\n";
            for (int i = 0; i < num_thread; i++)
                lsutmp2.rdv1_data[i] = external_mem[lsutmp1.rss1_data / 4 + i - 128 * num_thread];
            lsufifo.push(lsutmp2);
            // cout << "LSU_CALC: pushed vle32v output at " << sc_time_stamp() << "," << sc_delta_count_at_current_time() << "\n";
            break;
        }
        if (lsu_eqa.default_event().triggered())
        {
            lsueqa_triggered = true;
            wait(SC_ZERO_TIME);
            lsueqa_triggered = false;
            ev_lsufifo_pushed.notify();
        }
        else
            ev_lsufifo_pushed.notify();
    }
}

void BASE::LSU_OUT()
{
    while (true)
    {
        wait();
    }
}

void BASE::LSU_CTRL()
{
    lsu_ready = true;
    lsu_ready_old = true;
    lsueqb_triggered = false;
    while (true)
    {
        wait(lsu_eqb.default_event() | lsu_unready | lsu_evb);
        if (lsu_eqb.default_event().triggered())
        {
            lsu_ready = true;
            lsu_ready_old = lsu_ready;
            lsueqb_triggered = true;
            wait(SC_ZERO_TIME);
            lsueqb_triggered = false;
            ev_lsuready_updated.notify();
        }
        else if (lsu_evb.triggered())
        {
            lsu_ready = true;
            lsu_ready_old = lsu_ready;
            ev_lsuready_updated.notify();
        }
        else if (lsu_unready.triggered())
        {
            lsu_ready = false;
            lsu_ready_old = lsu_ready;
            ev_lsuready_updated.notify();
        }
    }
}
