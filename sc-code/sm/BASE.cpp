#include "BASE.h"

void BASE::debug_sti()
{
    // wait(30, SC_NS);
    // wait(clk.posedge_event());
    // jump = 1;
    // jump_addr = 2;
    // wait(SC_ZERO_TIME);
    // wait(clk.posedge_event());
    // jump = 0;
}

void BASE::debug_display()
{
}

void BASE::PROGRAM_COUNTER()
{
    while (true)
    {
        wait();
        wait(SC_ZERO_TIME); // wait for jump to update
        if (rst_n.read() == 0)
        {
            pc = -1;
            fetch_valid = false;
        }
        else if (jump.read() == 1)
        {
            pc = jump_addr.read();
            // cout << "pc jumps to addr " << jump_addr.read() << " at time " << sc_time_stamp() << "\n";
        }
        else if (dispatch == true | ibuf_full == false)
        {
            // later work: should ensure pc valid
            pc = pc.read() + 1;
            fetch_valid = true;
        }
    }
}

void BASE::INSTRUCTION_REG()
{
    // initialize
    // ireg[0] = I_TYPE(add_, 0, 1, 2);
    // ireg[1] = I_TYPE(add_, 1, 3, 3);
    // ireg[2] = I_TYPE(add_, 0, 4, 5);
    // ireg[3] = I_TYPE(beq_, 0, 7, 2);
    // ireg[4] = I_TYPE(vaddvx_, 0, 1, 4);
    // ireg[5] = I_TYPE(vfadd_, 3, 4, 2);
    // ireg[6] = I_TYPE(beq_, 0, 7, 5);

    ireg[0] = I_TYPE(vaddvv_, 0, 1, 2);
    ireg[1] = I_TYPE(add_, 1, 3, 3);
    ireg[2] = I_TYPE(lw_, 0, 3, 9);
    ireg[3] = I_TYPE(vload_, 3, 9, 4);
    ireg[4] = I_TYPE(vaddvx_, 0, 4, 5);
    ireg[5] = I_TYPE(vaddvx_, 0, 1, 4);
    ireg[6] = I_TYPE(vfadd_, 3, 4, 2);
    ireg[7] = I_TYPE(beq_, 0, 7, -4);

    while (true)
    {
        fetch_ins = (pc.read() >= 0) ? ireg[pc.read()] : I_TYPE(INVALID_, 0, 0, 0);
        // cout << "pc=" << pc << ", fetch_ins is " << fetch_ins << " at time " << sc_time_stamp()
        //      << ", it will be " << ireg[pc.read()] << " at the next timestamp"
        //      << "\n";
        wait(pc.value_changed_event());
        // cout << "pc.value_changed_event() triggered\n";
    }
}

void BASE::DECODE()
{
    while (true)
    {
        wait();
        wait(SC_ZERO_TIME);
        wait(SC_ZERO_TIME); // wait for fetch data to update
        if (fetch_valid)
        {
            switch (fetch_ins.op)
            {
            case beq_:
                fetch_ins.jump_addr = pc.read() + 1 + fetch_ins.d;
                // cout << "decoding beq ins at time " << sc_time_stamp() << "\n";
                break;
            default:
                fetch_ins.jump_addr = -1;
                break;
            }
        }
    }
}

void BASE::IBUF_ACTION()
{
    I_TYPE dispatch_ins_;
    while (true)
    {
        wait();
        if (rst_n.read() == 0)
        {
            while (ififo.nb_get(_readdata3))
            {
            }
        }
        else
        {
            if (dispatch)
            {
                // cout << "before dispatch, ififo has " << ififo.used() << " elems at time " << sc_time_stamp() << "\n";
                dispatch_ins_ = ififo.get();
                // cout << "after dispatch, ififo has " << ififo.used() << " elems at time " << sc_time_stamp() << "\n";
            }
            if (fetch_valid)
            {
                // cout << "before put, ififo has " << ififo.used() << " elems at time " << sc_time_stamp() << "\n";
                ififo.put(fetch_ins);
                // cout << "after put, ififo has " << ififo.used() << " elems at time " << sc_time_stamp() << "\n";
                // cout << "ififo has put ins " << fetch_ins << ", whose jump_addr is " << fetch_ins.jump_addr << " at time " << sc_time_stamp() << "\n";
            }
        }
        wait(SC_ZERO_TIME);
        if (jump)
        {
            while (ififo.nb_get(_readdata3))
            {
            }
            ibuf_full = false;
            ibuf_empty = true;
            ififo_elem_num = 0;
            ibuftop_ins = I_TYPE(INVALID_, 0, 0, 0);
        }
        else
        {
            ibuf_full = !ififo.nb_can_put();
            // cout << "ififo.nb_can_put()=" << ififo.nb_can_put()
            //      << ", ibuf_full=" << ibuf_full << " at time " << sc_time_stamp() << "\n";
            ibuf_empty = !ififo.nb_peek(ibuftop_ins);
            ififo_elem_num = ififo.used();
            // cout << "ififo has " << ififo.used() << " elems in it at time " << sc_time_stamp() << "\n";
        }
    }
}

void BASE::JUDGE_DISPATCH()
{
    while (true)
    {
        wait();
        wait(SC_ZERO_TIME); // wait for UPDATE_SCORE
        wait(SC_ZERO_TIME); // wait for ibuffer top to update
        // cout << "scoreboard: ibuftop_ins=" << ibuftop_ins << " at time " << sc_time_stamp() << "\n";
        switch (ibuftop_ins.op)
        {
        case lw_:
            // cout << "JUDGE_DISPATCH switch to lw_ case at time " << sc_time_stamp() << "\n";
            if (score.find(SCORE_TYPE(s, ibuftop_ins.s1)) == score.end() &&
                score.find(SCORE_TYPE(s, ibuftop_ins.d)) == score.end())
                can_dispatch = true;
            else
                can_dispatch = false;
            break;
        case add_:

            // cout << "JUDGE_DISPATCH switch to add_ case at time " << sc_time_stamp() << "\n";
            if (score.find(SCORE_TYPE(s, ibuftop_ins.s1)) == score.end() &&
                score.find(SCORE_TYPE(s, ibuftop_ins.s2)) == score.end() &&
                score.find(SCORE_TYPE(s, ibuftop_ins.d)) == score.end())
                can_dispatch = true;
            else
                can_dispatch = false;
            break;
        case beq_:
            if (score.find(SCORE_TYPE(s, ibuftop_ins.s1)) == score.end() &&
                score.find(SCORE_TYPE(s, ibuftop_ins.s2)) == score.end())
                can_dispatch = true;
            else
                can_dispatch = false;
            break;
        case vload_:
            if (score.find(SCORE_TYPE(v, ibuftop_ins.s1)) == score.end() &&
                score.find(SCORE_TYPE(v, ibuftop_ins.d)) == score.end())
                can_dispatch = true;
            else
                can_dispatch = false;
            break;
        case vaddvv_:
            if (score.find(SCORE_TYPE(v, ibuftop_ins.s1)) == score.end() &&
                score.find(SCORE_TYPE(v, ibuftop_ins.s2)) == score.end() &&
                score.find(SCORE_TYPE(v, ibuftop_ins.d)) == score.end())
                can_dispatch = true;
            else
                can_dispatch = false;
            break;
        case vaddvx_:
            if (score.find(SCORE_TYPE(v, ibuftop_ins.s1)) == score.end() &&
                score.find(SCORE_TYPE(s, ibuftop_ins.s2)) == score.end() &&
                score.find(SCORE_TYPE(v, ibuftop_ins.d)) == score.end())
                can_dispatch = true;
            else
                can_dispatch = false;
            break;
        case vfadd_:
            if (score.find(SCORE_TYPE(f, ibuftop_ins.s1)) == score.end() &&
                score.find(SCORE_TYPE(f, ibuftop_ins.s2)) == score.end() &&
                score.find(SCORE_TYPE(f, ibuftop_ins.d)) == score.end())
                can_dispatch = true;
            else
                can_dispatch = false;
            break;
        default:
            can_dispatch = false;
            // cout << "JUDGE_DISPATCH switch to default case at time " << sc_time_stamp() << "\n";
            break;
        }
    }
}

void BASE::UPDATE_SCORE()
{
    I_TYPE tmpins;
    std::set<SCORE_TYPE>::iterator it;
    REG_TYPE regtype_;
    while (true)
    {
        wait();
        if (dispatch)
        {
            tmpins = ibuftop_ins; // this ibuftop_ins is the old data
            switch (tmpins.op)
            {
            case lw_:
            case add_:
                regtype_ = s;
                break;
            case vaddvv_:
            case vaddvx_:
            case vload_:
                regtype_ = v;
                break;
            case vfadd_:
                regtype_ = f;
                break;
            }
            score.insert(SCORE_TYPE(regtype_, tmpins.d));
            // cout << "scoreboard insert " << SCORE_TYPE(regtype_, tmpins.d)
            //      << " because of dispatch at time " << sc_time_stamp() << " (using old ibuftop_ins)\n";
        }
        wait(SC_ZERO_TIME);
        wait(SC_ZERO_TIME);
        if (wb_ena)
        {
            tmpins = wb_ins;
            // cout << "scoreboard: wb_ins is " << tmpins << " at " << sc_time_stamp() << "\n";
            switch (tmpins.op)
            {
            case lw_:
            case add_:
                regtype_ = s;
                break;
            case vaddvv_:
            case vaddvx_:
            case vload_:
                regtype_ = v;
                break;
            case vfadd_:
                regtype_ = f;
                break;
            }
            it = score.find(SCORE_TYPE(regtype_, tmpins.d));
            cout << "scoreboard: finding SCORE " << SCORE_TYPE(regtype_, tmpins.d) << " at " << sc_time_stamp() << "\n";
            if (it == score.end())
            {
                cout << "wb_ena error: scoreboard can't find rd in score set, wb_ins=" << wb_ins << " at " << sc_time_stamp() << "\n";
                break;
            }
            score.erase(it);
            cout << "scoreboard: succesfully erased SCORE " << SCORE_TYPE(regtype_, tmpins.d) << ", wb_ins=" << wb_ins << " at " << sc_time_stamp() << "\n";
        }
    }
}

void BASE::ISSUE_ACTION()
{
    while (true)
    {
        wait();
        wait(SC_ZERO_TIME);
        wait(SC_ZERO_TIME);
        wait(SC_ZERO_TIME);
        if (can_dispatch && (emit == true | opc_full == false))
        {
            dispatch = true;
            issue_ins = ibuftop_ins;
        }
        else
            dispatch = false;
    }
}

void BASE::OPC_FIFO()
{
    I_TYPE _readdata4;
    while (true)
    {
        wait();
        if (emit)
        {
            opcfifo.get(); // last cycle emit
        }
        if (dispatch)
        {
            opcfifo.put(issue_ins);
            // cout << "opcfifo has put issue_ins " << issue_ins << " at time " << sc_time_stamp() << "\n";
        }
        wait(SC_ZERO_TIME); // wait for opc_fifo to update
        if (jump)
        {
            while (opcfifo.nb_get(_readdata4))
            {
            }
            opc_full = false;
            opc_empty = true;
            opctop_ins = I_TYPE(INVALID_, 0, 0, 0);
            opcfifo_elem_num = 0;
        }
        else
        {
            opc_full = !opcfifo.nb_can_put();
            opc_empty = !opcfifo.nb_peek(opctop_ins);
            opcfifo_elem_num = opcfifo.used();
        }
    }
}

void BASE::OPC_EMIT()
{
    while (true)
    {
        wait();
        wait(SC_ZERO_TIME); // wait for opcfifo to update
        wait(SC_ZERO_TIME); // wait for salu_ready etc. to update
        // cout << "OPC: salu_ready=" << salu_ready << " at time " << sc_time_stamp() << "\n";
        switch (opctop_ins.op)
        {
        case add_:
        case beq_:
            if (salu_ready)
            {
                // cout << "OPC: salu is ready at time " << sc_time_stamp() << "\n";
                rss1_addr = opctop_ins.s1;
                rss2_addr = opctop_ins.s2;
                emito_salu = true;
                emito_valu = emito_vfpu = emito_lsu = false;
            }
            else
            {
                emito_salu = emito_valu = emito_vfpu = emito_lsu = false;
                // wait(salu_ready.posedge_event());
                // cout << "OPC has received salu_ready.pos at time " << sc_time_stamp() << "\n";
            }
            break;
        case vaddvv_:
            if (valu_ready)
            {
                rsv1_addr.write(opctop_ins.s1);
                rsv2_addr.write(opctop_ins.s2);
                emito_valu = true;
                emito_salu = emito_vfpu = emito_lsu = false;
            }
            else
                emito_salu = emito_valu = emito_vfpu = emito_lsu = false;
            break;
        case vaddvx_:
            if (valu_ready)
            {
                rsv1_addr.write(opctop_ins.s1);
                rss2_addr.write(opctop_ins.s2);
                emito_valu = true;
                emito_salu = emito_vfpu = emito_lsu = false;
            }
            else
                emito_salu = emito_valu = emito_vfpu = emito_lsu = false;
            break;
        case vfadd_:
            if (vfpu_ready)
            {
                rsf1_addr.write(opctop_ins.s1);
                rsf2_addr.write(opctop_ins.s2);
                emito_vfpu = true;
                emito_salu = emito_valu = emito_lsu = false;
            }
            else
                emito_salu = emito_valu = emito_vfpu = emito_lsu = false;
            break;
        case lw_:
        case vload_:
            if (lsu_ready)
            {
                rss1_addr.write(opctop_ins.s1);
                emito_lsu = true;
                emito_salu = emito_valu = emito_vfpu = false;
            }
            else
                emito_salu = emito_valu = emito_vfpu = emito_lsu = false;
            break;
        case INVALID_:
            emito_salu = emito_valu = emito_vfpu = emito_lsu = false;
            break;
        }
        emit = emito_salu | emito_valu | emito_vfpu | emito_lsu;
    }
}

void BASE::INIT_REG()
{
    s_regfile[0] = 22;
    s_regfile[1] = -10;
    s_regfile[2] = 666;
    s_regfile[3] = 11;
    s_regfile[4] = 10;
    s_regfile[5] = 888;
    s_regfile[6] = 6;
    s_regfile[7] = 22;
    v_regfile[0].fill(1);
    v_regfile[1].fill(3);
    v_regfile[2].fill(-10);
    v_regfile[3].fill(7);
    v_regfile[4].fill(-1);
    v_regfile[5].fill(10);
    v_regfile[6].fill(8);
    v_regfile[7].fill(1);
    f_regfile[0].fill(1);
    f_regfile[1].fill(5.20);
    f_regfile[2].fill(-0.3);
    f_regfile[3].fill(3.14);
    f_regfile[4].fill(18.99);
}

void BASE::READ_REG()
{
    while (true)
    {
        wait();
        wait(SC_ZERO_TIME);
        wait(SC_ZERO_TIME);
        wait(SC_ZERO_TIME); // wait for opc to update rs_addr
        rss1_data = s_regfile[rss1_addr.read()];
        rss2_data = s_regfile[rss2_addr.read()];
        for (int i = 0; i < num_thread; i++)
        {
            rsv1_data[i] = v_regfile[rsv1_addr.read()][i];
            rsv2_data[i] = v_regfile[rsv2_addr.read()][i];
            rsf1_data[i] = f_regfile[rsf1_addr.read()][i];
            rsf2_data[i] = f_regfile[rsf2_addr.read()][i];
        }
    }
}

void BASE::WRITE_REG()
{
    while (true)
    {
        wait();
        wait(SC_ZERO_TIME);
        wait(SC_ZERO_TIME);
        // 后续regfile要一次只能写一个，否则报错
        if (write_s)
        {
            s_regfile[rds1_addr.read()] = rds1_data;
        }
        if (write_v)
        {
            for (int i = 0; i < num_thread; i++)
                v_regfile[rdv1_addr.read()][i] = rdv1_data[i];
        }
        if (write_f)
        {
            for (int i = 0; i < num_thread; i++)
                f_regfile[rdf1_addr.read()][i] = rdf1_data[i];
        }
    }
}

void BASE::SALU_IN()
{
    I_TYPE new_ins;
    salu_in_t new_data;
    int a_delay, b_delay;
    while (true)
    {
        wait();
        jump = 0;
        if (emito_salu)
        {
            // cout << "SALU_IN: emito_salu is " << emito_salu << " at time " << sc_time_stamp() << "\n";
            if (salu_ready == false)
            {
                cout << "salu error: not ready at time " << sc_time_stamp() << "\n";
            }
            salu_unready.notify();
            switch (opctop_ins.op)
            {
            case beq_:
                b_delay = 1;
                salu_eqb.notify(sc_time((b_delay - 1) * PERIOD, SC_NS));
                if (rss1_data == rss2_data)
                    jump = 1;
                jump_addr = opctop_ins.jump_addr;
                // cout << "jump is updated to 1 at time " << sc_time_stamp() << "\n";
                break;
            case add_:
                new_data.ins = opctop_ins;
                new_data.rss1_data = rss1_data;
                new_data.rss2_data = rss2_data;
                salu_dq.push(new_data);
                // cout << "salu_dq has just pushed 1 elem at time " << sc_time_stamp() << "\n";
                a_delay = 3;
                b_delay = 2;
                salu_eqa.notify(sc_time((a_delay)*PERIOD, SC_NS));
                salu_eqb.notify(sc_time((b_delay - 1) * PERIOD, SC_NS));
                // cout << "SALU_IN switch to add_ (from opc input) at time " << sc_time_stamp() << "\n";
                break;
            default:
                cout << "salu error: receive wrong ins " << opctop_ins << " at time " << sc_time_stamp() << "\n";
            }
        }
    }
}

void BASE::SALU_OUT()
{
    salufifo_elem_num = salufifo.used();
    salufifo_empty = !salufifo.nb_peek(salutop_dat);
    salu_in_t salutmp1;
    salu_out_t salutmp2;
    bool succeed;
    while (true)
    {
        wait(salu_eqa.default_event());
        if (salu_eqa.default_event().triggered())
        {
            // cout << "SALU_OUT: eqa triggered at time " << sc_time_stamp() << "\n";
            salutmp1 = salu_dq.front();
            // cout << "salu_dq.front's ins is " << salutmp1.ins << ", data is " << salutmp1.rss1_data << "," << salutmp1.rss2_data << "\n";
            salu_dq.pop();
            // cout << "salu_dq has poped, now its elem_num is " << salu_dq.size() << " at time " << sc_time_stamp() << "\n";
            switch (salutmp1.ins.op)
            {
            case add_:
                salutmp2.ins = salutmp1.ins;
                salutmp2.data = salutmp1.rss1_data + salutmp1.rss2_data;
                // cout << "SALU_OUT: do add at time " << sc_time_stamp() << "\n";
                succeed = salufifo.nb_put(salutmp2);
                if (succeed == false)
                {
                    cout << "salu error: output failed to put in fifo at time " << sc_time_stamp() << "\n";
                }
                break;
            }
            wait(SC_ZERO_TIME);
            salufifo_empty = !salufifo.nb_peek(salutop_dat);
            salufifo_elem_num = salufifo.used();
            wait(SC_ZERO_TIME);
            wait(SC_ZERO_TIME);
            if (write_s)
            {
                salufifo.get();
                // cout << "salufifo has poped out at time " << sc_time_stamp() << "\n";
                wait(SC_ZERO_TIME);
                salufifo_empty = !salufifo.nb_peek(salutop_dat);
                salufifo_elem_num = salufifo.used();
            }
        }
    }
}

void BASE::SALU_CTRL()
{
    salu_ready = true;
    while (true)
    {
        wait(salu_eqb.default_event() | salu_unready);
        if (salu_eqb.default_event().triggered())
        {
            // cout << "salu notified by b, previous salu_ready=" << salu_ready << ", update it to true at time " << sc_time_stamp() << "\n";
            salu_ready = true;
        }
        else if (salu_unready.triggered())
        {
            // cout << "salu notified by unready, previous salu_ready=" << salu_ready << ", update it to false at time " << sc_time_stamp() << "\n";
            salu_ready = false;
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
            if (valu_ready == false)
            {
                cout << "valu error: not ready at time " << sc_time_stamp() << "\n";
            }
            valu_unready.notify();
            switch (opctop_ins.op)
            {
            case vaddvv_:
                new_data.ins = opctop_ins;
                for (int i = 0; i < num_thread; i++)
                {
                    new_data.rsv1_data[i] = rsv1_data[i];
                    new_data.rsv2_data[i] = rsv2_data[i];
                }
                valu_dq.push(new_data);
                a_delay = 5;
                b_delay = 2;
                valu_eqa.notify(sc_time((a_delay)*PERIOD, SC_NS));
                valu_eqb.notify(sc_time((b_delay - 1) * PERIOD, SC_NS));
                break;
            case vaddvx_:
                new_data.ins = opctop_ins;
                for (int i = 0; i < num_thread; i++)
                {
                    new_data.rsv1_data[i] = rsv1_data[i];
                }
                new_data.rss2_data = rss2_data;
                valu_dq.push(new_data);
                a_delay = 5;
                b_delay = 2;
                valu_eqa.notify(sc_time((a_delay)*PERIOD, SC_NS));
                valu_eqb.notify(sc_time((b_delay - 1) * PERIOD, SC_NS));
                break;
            default:
                cout << "valu error: receive wrong ins " << opctop_ins << " at time " << sc_time_stamp() << "\n";
            }
        }
    }
}

void BASE::VALU_OUT()
{
    valufifo_elem_num = valufifo.used();
    valufifo_empty = !valufifo.nb_peek(valutop_dat);
    valu_in_t valutmp1;
    valu_out_t valutmp2;
    bool succeed;
    while (true)
    {
        wait(valu_eqa.default_event());
        if (valu_eqa.default_event().triggered())
        {
            valutmp1 = valu_dq.front();
            valu_dq.pop();
            switch (valutmp1.ins.op)
            {
            case vaddvv_:
                valutmp2.ins = valutmp1.ins;
                for (int i = 0; i < num_thread; i++)
                {
                    valutmp2.rdv1_data[i] = valutmp1.rsv1_data[i] + valutmp1.rsv2_data[i];
                }
                succeed = valufifo.nb_put(valutmp2);
                break;
            case vaddvx_:
                valutmp2.ins = valutmp1.ins;
                for (int i = 0; i < num_thread; i++)
                {
                    valutmp2.rdv1_data[i] = valutmp1.rsv1_data[i] + valutmp1.rss2_data;
                }
                succeed = valufifo.nb_put(valutmp2);
                break;
            }
            if (succeed == false)
                cout << "valu error: output failed to put in fifo at time " << sc_time_stamp() << "\n";
            wait(SC_ZERO_TIME);
            valufifo_empty = !valufifo.nb_peek(valutop_dat);
            valufifo_elem_num = valufifo.used();
            wait(SC_ZERO_TIME);
            wait(SC_ZERO_TIME);
            if (write_v)
            {
                valufifo.get();
                wait(SC_ZERO_TIME);
                valufifo_empty = !valufifo.nb_peek(valutop_dat);
                valufifo_elem_num = valufifo.used();
            }
        }
    }
}

void BASE::VALU_CTRL()
{
    valu_ready = true;
    while (true)
    {
        wait(valu_eqb.default_event() | valu_unready);
        if (valu_eqb.default_event().triggered())
        {
            valu_ready = true;
        }
        else if (valu_unready.triggered())
        {
            valu_ready = false;
        }
    }
}

void BASE::VFPU_IN()
{
    I_TYPE new_ins;
    vfpu_in_t new_data;
    int a_delay, b_delay;
    while (true)
    {
        wait();
        if (emito_vfpu)
        {
            if (vfpu_ready == false)
            {
                cout << "vfpu error: not ready at time " << sc_time_stamp() << "\n";
            }
            vfpu_unready.notify();
            switch (opctop_ins.op)
            {
            case vfadd_:
                new_data.ins = opctop_ins;
                for (int i = 0; i < num_thread; i++)
                {
                    new_data.rsf1_data[i] = rsf1_data[i];
                    new_data.rsf2_data[i] = rsf2_data[i];
                }
                vfpu_dq.push(new_data);
                a_delay = 5;
                b_delay = 2;
                vfpu_eqa.notify(sc_time((a_delay)*PERIOD, SC_NS));
                vfpu_eqb.notify(sc_time((b_delay - 1) * PERIOD, SC_NS));
                break;
            default:
                cout << "vfpu error: receive wrong ins " << opctop_ins << " at time " << sc_time_stamp() << "\n";
            }
        }
    }
}

void BASE::VFPU_OUT()
{
    vfpufifo_elem_num = vfpufifo.used();
    vfpufifo_empty = !vfpufifo.nb_peek(vfputop_dat);
    vfpu_in_t vfputmp1;
    vfpu_out_t vfputmp2;
    bool succeed;
    while (true)
    {
        wait(vfpu_eqa.default_event());
        if (vfpu_eqa.default_event().triggered())
        {
            vfputmp1 = vfpu_dq.front();
            vfpu_dq.pop();
            switch (vfputmp1.ins.op)
            {
            case vfadd_:
                vfputmp2.ins = vfputmp1.ins;
                for (int i = 0; i < num_thread; i++)
                {
                    vfputmp2.rdf1_data[i] = vfputmp1.rsf1_data[i] + vfputmp1.rsf2_data[i];
                }
                succeed = vfpufifo.nb_put(vfputmp2);
                break;
            }
            if (succeed == false)
                cout << "vfpu error: output failed to put in fifo at time " << sc_time_stamp() << "\n";
            wait(SC_ZERO_TIME);
            vfpufifo_empty = !vfpufifo.nb_peek(vfputop_dat);
            vfpufifo_elem_num = vfpufifo.used();
            wait(SC_ZERO_TIME);
            wait(SC_ZERO_TIME);
            if (write_f)
            {
                vfpufifo.get();
                wait(SC_ZERO_TIME);
                vfpufifo_empty = !vfpufifo.nb_peek(vfputop_dat);
                vfpufifo_elem_num = vfpufifo.used();
            }
        }
    }
}

void BASE::VFPU_CTRL()
{
    vfpu_ready = true;
    while (true)
    {
        wait(vfpu_eqb.default_event() | vfpu_unready);
        if (vfpu_eqb.default_event().triggered())
        {
            vfpu_ready = true;
        }
        else if (vfpu_unready.triggered())
        {
            vfpu_ready = false;
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
            if (lsu_ready == false)
            {
                cout << "lsu error: not ready at time " << sc_time_stamp() << "\n";
            }
            lsu_unready.notify();
            switch (opctop_ins.op)
            {
            case lw_:
                new_data.ins = opctop_ins;
                new_data.rss1_data = rss1_data;
                lsu_dq.push(new_data);
                a_delay = 10;
                b_delay = 4;
                lsu_eqa.notify(sc_time((a_delay)*PERIOD, SC_NS));
                lsu_eqb.notify(sc_time((b_delay - 1) * PERIOD, SC_NS));
                break;
            case vload_:
                new_data.ins = opctop_ins;
                new_data.rss1_data = rss1_data;
                lsu_dq.push(new_data);
                a_delay = 15;
                b_delay = 4;
                lsu_eqa.notify(sc_time((a_delay)*PERIOD, SC_NS));
                lsu_eqb.notify(sc_time((b_delay - 1) * PERIOD, SC_NS));
                break;
            default:
                cout << "lsu error: receive wrong ins " << opctop_ins << " at time " << sc_time_stamp() << "\n";
            }
        }
    }
}

void BASE::LSU_OUT()
{
    lsufifo_elem_num = lsufifo.used();
    lsufifo_empty = !lsufifo.nb_peek(lsutop_dat);
    lsu_in_t lsutmp1;
    lsu_out_t lsutmp2;
    bool succeed;
    while (true)
    {
        wait(lsu_eqa.default_event());
        if (lsu_eqa.default_event().triggered())
        {
            lsutmp1 = lsu_dq.front();
            lsu_dq.pop();
            switch (lsutmp1.ins.op)
            {
            case lw_:
                lsutmp2.ins = lsutmp1.ins;
                lsutmp2.rds1_data = s_memory[lsutmp1.rss1_data + lsutmp1.ins.s2];
                succeed = lsufifo.nb_put(lsutmp2);
                break;
            case vload_:
                lsutmp2.ins = lsutmp1.ins;
                for (int i = 0; i < num_thread; i++)
                {
                    lsutmp2.rdv1_data[i] = v_memory[lsutmp1.rss1_data + lsutmp1.ins.s2][i];
                }
                succeed = lsufifo.nb_put(lsutmp2);
                break;
            }
            if (succeed == false)
                cout << "lsu error: output failed to put in fifo at time " << sc_time_stamp() << "\n";
            wait(SC_ZERO_TIME);
            lsufifo_empty = !lsufifo.nb_peek(lsutop_dat);
            lsufifo_elem_num = lsufifo.used();
            wait(SC_ZERO_TIME);
            wait(SC_ZERO_TIME);
            if (write_lsu)
            {
                lsufifo.get();
                wait(SC_ZERO_TIME);
                lsufifo_empty = !lsufifo.nb_peek(lsutop_dat);
                lsufifo_elem_num = lsufifo.used();
            }
        }
    }
}

void BASE::LSU_CTRL()
{
    lsu_ready = true;
    while (true)
    {
        wait(lsu_eqb.default_event() | lsu_unready);
        if (lsu_eqb.default_event().triggered())
        {
            lsu_ready = true;
        }
        else if (lsu_unready.triggered())
        {
            lsu_ready = false;
        }
    }
}

void BASE::WRITE_BACK()
{
    write_s = false;
    while (true)
    {
        wait();
        wait(SC_ZERO_TIME);
        if (salufifo_empty == false)
        {
            write_s = true;
            write_v = write_f = false;
            // cout << "do write_s=true at " << sc_time_stamp() << "\n";
            wb_ins = salutop_dat.ins;
            rds1_addr = salutop_dat.ins.d;
            rds1_data = salutop_dat.data;
        }
        else if (valufifo_empty == false)
        {
            write_v = true;
            write_s = write_f = false;
            wb_ins = valutop_dat.ins;
            rdv1_addr = valutop_dat.ins.d;
            for (int i = 0; i < num_thread; i++)
            {
                rdv1_data[i] = valutop_dat.rdv1_data[i];
            }
        }
        else if (vfpufifo_empty == false)
        {
            write_f = true;
            write_s = write_v = false;
            wb_ins = vfputop_dat.ins;
            rdf1_addr = vfputop_dat.ins.d;
            for (int i = 0; i < num_thread; i++)
            {
                rdf1_data[i] = vfputop_dat.rdf1_data[i];
            }
        }
        else if (lsufifo_empty == false)
        {
            write_lsu = true;
            switch (lsutop_dat.ins.op)
            {
            case lw_:
                write_s = true;
                write_v = write_f = false;
                wb_ins = lsutop_dat.ins;
                rds1_addr = lsutop_dat.ins.d;
                rds1_data = lsutop_dat.rds1_data;
                break;
            case vload_:
                write_v = true;
                write_s = write_f = false;
                wb_ins = lsutop_dat.ins;
                rds1_addr = lsutop_dat.ins.d;
                for (int i = 0; i < num_thread; i++)
                {
                    rdv1_data[i] = lsutop_dat.rdv1_data[i];
                }
                break;
            default:
                cout << "wb error: lsu unrecognized ins\n";
                break;
            }
        }
        else
        {
            write_s = write_v = write_f = false;
        }
        wb_ena = write_s | write_v | write_f;
    }
}
