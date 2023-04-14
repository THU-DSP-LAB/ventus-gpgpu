#include "BASE.h"

void BASE::debug_sti()
{
    while (true)
    {
        wait(clk.posedge_event());
        wait(SC_ZERO_TIME);
        dispatch_ready = !opc_full | doemit;
    }
}

void BASE::debug_display()
{
    while (true)
    {
        wait(ev_salufifo_pushed);
        cout << "ev_salufifo_pushed triggered at " << sc_time_stamp() << "," << sc_delta_count_at_current_time() << "\n";
    }
}
void BASE::debug_display1()
{
    while (true)
    {
        wait(ev_valufifo_pushed);
        cout << "ev_valufifo_pushed triggered at " << sc_time_stamp() << "," << sc_delta_count_at_current_time() << "\n";
    }
}
void BASE::debug_display2()
{
    while (true)
    {
        wait(ev_vfpufifo_pushed);
        cout << "ev_vfpufifo_pushed triggered at " << sc_time_stamp() << "," << sc_delta_count_at_current_time() << "\n";
    }
}
void BASE::debug_display3()
{
    while (true)
    {
        wait(ev_lsufifo_pushed);
        cout << "ev_lsufifo_pushed triggered at " << sc_time_stamp() << "," << sc_delta_count_at_current_time() << "\n";
    }
}

void BASE::PROGRAM_COUNTER(int warp_id)
{
    WARPS[warp_id].pc = 0;
    while (true)
    {
        wait(clk.posedge_event());
        // cout << "PC start by clk at " << sc_time_stamp() << "," << sc_delta_count_at_current_time() << "\n";
        // wait(WARPS[warp_id].ev_ibuf_inout); // ibuf判断swallow后，fetch新指令
        // cout << "PC start, ibuf_swallow=" << ibuf_swallow << " at " << sc_time_stamp() << "," << sc_delta_count_at_current_time() << "\n";
        if (rst_n == 0)
        {
            WARPS[warp_id].pc = -1;
            WARPS[warp_id].fetch_valid = false;
        }
        else if (WARPS[warp_id].jump == 1)
        {
            WARPS[warp_id].pc = WARPS[warp_id].jump_addr;
            WARPS[warp_id].fetch_valid = true;
            // cout << "pc jumps to addr " << jump_addr.read() << " at " << sc_time_stamp() <<","<< sc_delta_count_at_current_time() << "\n";
        }
        else if (WARPS[warp_id].ibuf_empty |
                 (!WARPS[warp_id].ibuf_full |
                  (WARPS[warp_id].dispatch_warp_valid && (!opc_full | doemit))))
        {
            // cout << "pc will +1 at " << sc_time_stamp() << "," << sc_delta_count_at_current_time() << "\n";
            WARPS[warp_id].pc = WARPS[warp_id].pc.read() + 1;
            WARPS[warp_id].fetch_valid = true;
        }
    }
}

void BASE::INSTRUCTION_REG(int warp_id)
{
    // initialize
    // ireg[0] = I_TYPE(add_, 0, 1, 2);
    // ireg[1] = I_TYPE(add_, 1, 3, 3);
    // ireg[2] = I_TYPE(add_, 0, 4, 5);
    // ireg[3] = I_TYPE(beq_, 0, 7, 2);
    // ireg[4] = I_TYPE(vaddvx_, 0, 1, 4);
    // ireg[5] = I_TYPE(vfaddvv_, 3, 4, 2);
    // ireg[6] = I_TYPE(beq_, 0, 7, 5);

    while (true)
    {
        WARPS[warp_id].fetch_ins =
            (WARPS[warp_id].pc.read() >= 0)
                ? WARPS[warp_id].ireg[WARPS[warp_id].pc.read()]
                : I_TYPE(INVALID_, 0, 0, 0);
        // cout << "pc=" << pc << ", fetch_ins is " << fetch_ins << " at " << sc_time_stamp()
        //      << ", it will be " << ireg[pc.read()] << " at the next timestamp"
        //      << "\n";
        WARPS[warp_id].ev_decode.notify();
        wait(WARPS[warp_id].pc.value_changed_event());
        // cout << "pc.value_changed_event() triggered\n";
    }
}

void BASE::DECODE(int warp_id)
{
    while (true)
    {
        wait(WARPS[warp_id].ev_decode);
        switch (WARPS[warp_id].fetch_ins.op)
        {
        case beq_:
            WARPS[warp_id].decode_ins =
                I_TYPE(WARPS[warp_id].fetch_ins,
                       WARPS[warp_id].pc.read() + 1 + WARPS[warp_id].fetch_ins.d);
            // cout << "decoding beq ins at " << sc_time_stamp() <<","<< sc_delta_count_at_current_time() << "\n";
            break;
        default:
            WARPS[warp_id].decode_ins = I_TYPE(WARPS[warp_id].fetch_ins, -1);
            break;
        }
    }
}

void BASE::IBUF_ACTION(int warp_id)
{
    I_TYPE dispatch_ins_;
    I_TYPE _readdata3;
    while (true)
    {
        wait(clk.posedge_event());
        WARPS[warp_id].ibuf_swallow = false;
        if (rst_n.read() == 0)
            WARPS[warp_id].ififo.clear();
        else
        {
            if (WARPS[warp_id].dispatch_warp_valid && (!opc_full | doemit))
            {
                // cout << "before dispatch, ififo has " << ififo.used() << " elems at " << sc_time_stamp() <<","<< sc_delta_count_at_current_time() << "\n";
                dispatch_ins_ = WARPS[warp_id].ififo.get();
                // cout << "IBUF: after dispatch, ififo has " << ififo.used() << " elems at " << sc_time_stamp() <<","<< sc_delta_count_at_current_time() << "\n";
            }
            else
            {
                // cout << "IBUF: dispatch == false at " << sc_time_stamp() <<","<< sc_delta_count_at_current_time() << "\n";
            }
            if (WARPS[warp_id].fetch_valid && WARPS[warp_id].jump == false)
            {
                if (WARPS[warp_id].ififo.isfull())
                {
                    // cout << "IBUF ERROR: ibuf is full but is sent an ins from FETCH at " << sc_time_stamp() <<","<< sc_delta_count_at_current_time() << "\n";
                }
                else
                {
                    WARPS[warp_id].ififo.push(WARPS[warp_id].decode_ins.read());
                    WARPS[warp_id].ibuf_swallow = true;
                }
                // cout << "before put, ififo has " << ififo.used() << " elems at " << sc_time_stamp() <<","<< sc_delta_count_at_current_time() << "\n";
                // cout << "after put, ififo has " << ififo.used() << " elems at " << sc_time_stamp() <<","<< sc_delta_count_at_current_time() << "\n";
            }
            else if (WARPS[warp_id].jump)
            {
                // cout << "ibuf detected jump at " << sc_time_stamp() <<","<< sc_delta_count_at_current_time() << "\n";
                WARPS[warp_id].ififo.clear();
            }
        }
        WARPS[warp_id].ev_ibuf_inout.notify();
    }
}

void BASE::IBUF_PARAM(int warp_id)
{
    while (true)
    {
        wait(WARPS[warp_id].ev_ibuf_inout);
        WARPS[warp_id].ibuf_empty = WARPS[warp_id].ififo.isempty();
        WARPS[warp_id].ibuf_full = WARPS[warp_id].ififo.isfull();
        if (WARPS[warp_id].ififo.isempty())
        {
            WARPS[warp_id].ififo_elem_num = 0;
            WARPS[warp_id].ibuftop_ins = I_TYPE(INVALID_, -1, 0, 0);
        }
        else
        {
            WARPS[warp_id].ibuftop_ins.write(WARPS[warp_id].ififo.front());
            WARPS[warp_id].ififo_elem_num = WARPS[warp_id].ififo.used();
            // cout << "ififo has " << ififo.used() << " elems in it at " << sc_time_stamp() <<","<< sc_delta_count_at_current_time() << "\n";
        }
        WARPS[warp_id].ev_ibuf_updated.notify();
    }
}

void BASE::UPDATE_SCORE(int warp_id)
{
    I_TYPE tmpins;
    std::set<SCORE_TYPE>::iterator it;
    REG_TYPE regtype_;
    while (true)
    {
        wait(clk.posedge_event());
        if (wb_ena && wb_warpid == warp_id)
        { // 删除score
            tmpins = wb_ins;
            // cout << "scoreboard: wb_ins is " << tmpins << " at " << sc_time_stamp() <<","<< sc_delta_count_at_current_time() << "\n";
            switch (tmpins.op)
            {
            case lw_:
            case add_:
            case addi_:
                regtype_ = s;
                break;
            case vaddvv_:
            case vaddvx_:
            case vle32v_:
                regtype_ = v;
                break;
            case vfaddvv_:
                regtype_ = v;
                break;
            }
            it = WARPS[warp_id].score.find(SCORE_TYPE(regtype_, tmpins.d));
            // cout << "scoreboard写回: 正在寻找 SCORE " << SCORE_TYPE(regtype_, tmpins.d) << " at " << sc_time_stamp() <<","<< sc_delta_count_at_current_time() << "\n";
            if (it == WARPS[warp_id].score.end())
            {
                cout << "warp" << warp_id << "_wb_ena error: scoreboard can't find rd in score set, wb_ins=" << wb_ins << " at " << sc_time_stamp() << "," << sc_delta_count_at_current_time() << "\n";
            }
            else
            {
                WARPS[warp_id].score.erase(it);
            }
            // cout << "warp" << warp_id << "_scoreboard: succesfully erased SCORE " << SCORE_TYPE(regtype_, tmpins.d) << ", wb_ins=" << wb_ins << " at " << sc_time_stamp() << "," << sc_delta_count_at_current_time() << "\n";
        }
        if (WARPS[warp_id].branch_sig)
        {
            if (WARPS[warp_id].wait_bran == 0)
            {
                cout << "warp" << warp_id << "_scoreboard error: detect branch_sig=1(from salu) while wait_bran=0 at " << sc_time_stamp() << "," << sc_delta_count_at_current_time() << "\n";
            }
            else if (WARPS[warp_id].dispatch_warp_valid && (!opc_full | doemit))
            {
                cout << "warp" << warp_id << "_scoreboard error: detect branch_sig=1(from salu) while dispatch=1 at " << sc_time_stamp() << "," << sc_delta_count_at_current_time() << "\n";
            }
            WARPS[warp_id].wait_bran = 0;
        }
        else if (WARPS[warp_id].ibuftop_ins.read().op == beq_ && WARPS[warp_id].dispatch_warp_valid && (!opc_full | doemit))
        {
            // cout << "ibuf let wait_bran=1 at " << sc_time_stamp() <<","<< sc_delta_count_at_current_time() << "\n";
            WARPS[warp_id].wait_bran = 1;
        }
        if (WARPS[warp_id].dispatch_warp_valid && (!opc_full | doemit))
        {                                        // 加入 score
            tmpins = WARPS[warp_id].ibuftop_ins; // this ibuftop_ins is the old data
            if (tmpins.op != beq_)
            {
                switch (tmpins.op)
                {
                case lw_:
                case add_:
                case addi_:
                    regtype_ = s;
                    break;
                case vaddvv_:
                case vaddvx_:
                case vle32v_:
                    regtype_ = v;
                    break;
                case vfaddvv_:
                    regtype_ = v;
                    break;
                }
                WARPS[warp_id].score.insert(SCORE_TYPE(regtype_, tmpins.d));
                // cout << "warp" << warp_id << "_scoreboard: insert " << SCORE_TYPE(regtype_, tmpins.d)
                //      << " because of dispatch " << tmpins << " at " << sc_time_stamp() << "," << sc_delta_count_at_current_time() << "\n";
            }
        }
        WARPS[warp_id].ev_judge_dispatch.notify();
    }
}

void BASE::JUDGE_DISPATCH(int warp_id)
{
    I_TYPE _readibuf;
    while (true)
    {
        wait(WARPS[warp_id].ev_judge_dispatch & WARPS[warp_id].ev_ibuf_updated);
        // cout << "scoreboard: ibuftop_ins=" << ibuftop_ins << " at " << sc_time_stamp() <<","<< sc_delta_count_at_current_time() << "\n";
        if (WARPS[warp_id].wait_bran | WARPS[warp_id].jump)
        {
            WARPS[warp_id].can_dispatch = false;
        }
        else if (!WARPS[warp_id].ififo.isempty())
        {
            _readibuf = WARPS[warp_id].ififo.front();
            switch (_readibuf.op)
            {
            case lw_:
                // cout << "JUDGE_DISPATCH switch to lw_ case at " << sc_time_stamp() <<","<< sc_delta_count_at_current_time() << "\n";
                if (WARPS[warp_id].score.find(SCORE_TYPE(s, _readibuf.s1)) == WARPS[warp_id].score.end() &&
                    WARPS[warp_id].score.find(SCORE_TYPE(s, _readibuf.d)) == WARPS[warp_id].score.end())
                    WARPS[warp_id].can_dispatch = true;
                else
                    WARPS[warp_id].can_dispatch = false;
                break;
            case add_:
                // cout << "JUDGE_DISPATCH switch to add_ case at " << sc_time_stamp() <<","<< sc_delta_count_at_current_time() << "\n";
                if (WARPS[warp_id].score.find(SCORE_TYPE(s, _readibuf.s1)) == WARPS[warp_id].score.end() &&
                    WARPS[warp_id].score.find(SCORE_TYPE(s, _readibuf.s2)) == WARPS[warp_id].score.end() &&
                    WARPS[warp_id].score.find(SCORE_TYPE(s, _readibuf.d)) == WARPS[warp_id].score.end())
                    WARPS[warp_id].can_dispatch = true;
                else
                    WARPS[warp_id].can_dispatch = false;
                break;
            case addi_:
                if (WARPS[warp_id].score.find(SCORE_TYPE(s, _readibuf.s1)) == WARPS[warp_id].score.end() &&
                    WARPS[warp_id].score.find(SCORE_TYPE(s, _readibuf.d)) == WARPS[warp_id].score.end())
                    WARPS[warp_id].can_dispatch = true;
                else
                    WARPS[warp_id].can_dispatch = false;
                break;
            case beq_:
                if (WARPS[warp_id].score.find(SCORE_TYPE(s, _readibuf.s1)) == WARPS[warp_id].score.end() &&
                    WARPS[warp_id].score.find(SCORE_TYPE(s, _readibuf.s2)) == WARPS[warp_id].score.end())
                    WARPS[warp_id].can_dispatch = true;
                else
                    WARPS[warp_id].can_dispatch = false;
                break;
            case vle32v_:
                if (WARPS[warp_id].score.find(SCORE_TYPE(v, _readibuf.s1)) == WARPS[warp_id].score.end() &&
                    WARPS[warp_id].score.find(SCORE_TYPE(v, _readibuf.d)) == WARPS[warp_id].score.end())
                    WARPS[warp_id].can_dispatch = true;
                else
                    WARPS[warp_id].can_dispatch = false;
                break;
            case vaddvv_:
                // cout << "JUDGE_DISPATCH switch to vaddvv_ case at " << sc_time_stamp() <<","<< sc_delta_count_at_current_time() << "\n";
                if (WARPS[warp_id].score.find(SCORE_TYPE(v, _readibuf.s1)) == WARPS[warp_id].score.end() &&
                    WARPS[warp_id].score.find(SCORE_TYPE(v, _readibuf.s2)) == WARPS[warp_id].score.end() &&
                    WARPS[warp_id].score.find(SCORE_TYPE(v, _readibuf.d)) == WARPS[warp_id].score.end())
                    WARPS[warp_id].can_dispatch = true;
                else
                    WARPS[warp_id].can_dispatch = false;
                break;
            case vaddvx_:
                // cout << "JUDGE_DISPATCH switch to vaddvx_ case at " << sc_time_stamp() <<","<< sc_delta_count_at_current_time() << "\n";
                if (WARPS[warp_id].score.find(SCORE_TYPE(v, _readibuf.s1)) == WARPS[warp_id].score.end() &&
                    WARPS[warp_id].score.find(SCORE_TYPE(s, _readibuf.s2)) == WARPS[warp_id].score.end() &&
                    WARPS[warp_id].score.find(SCORE_TYPE(v, _readibuf.d)) == WARPS[warp_id].score.end())
                    WARPS[warp_id].can_dispatch = true;
                else
                {
                    WARPS[warp_id].can_dispatch = false;
                    // cout << "warp" << warp_id << ": JUDGE_DISPATCH don't dispatch vaddvx_, ins is " << _readibuf << ", at " << sc_time_stamp() << "," << sc_delta_count_at_current_time() << "\n";
                }
                break;
            case vfaddvv_:
                // cout << "JUDGE_DISPATCH switch to vfaddvv_ case at " << sc_time_stamp() <<","<< sc_delta_count_at_current_time() << "\n";
                if (WARPS[warp_id].score.find(SCORE_TYPE(f, _readibuf.s1)) == WARPS[warp_id].score.end() &&
                    WARPS[warp_id].score.find(SCORE_TYPE(f, _readibuf.s2)) == WARPS[warp_id].score.end() &&
                    WARPS[warp_id].score.find(SCORE_TYPE(f, _readibuf.d)) == WARPS[warp_id].score.end())
                    WARPS[warp_id].can_dispatch = true;
                else
                    WARPS[warp_id].can_dispatch = false;
                break;
            default:
                WARPS[warp_id].can_dispatch = false;
                // cout << "JUDGE_DISPATCH switch to default case at " << sc_time_stamp() <<","<< sc_delta_count_at_current_time() << "\n";
                break;
            }
        }
        WARPS[warp_id].ev_issue.notify();
    }
}

void BASE::ISSUE_ACTION()
{
    bool find_dispatchwarp = 0;
    last_dispatch_warpid = 0;
    while (true)
    {
        wait(ev_issue_list);
        // cout << "ISSUE start at " << sc_time_stamp() << "," << sc_delta_count_at_current_time() << "\n";
        if (!opc_full | doemit) // 这是dispatch_ready (ready-valid机制)
        {
            find_dispatchwarp = 0;
            for (int i = last_dispatch_warpid; i < last_dispatch_warpid + num_warp; i++)
            {
                if (!find_dispatchwarp && WARPS[i % num_warp].can_dispatch)
                {
                    // cout << "ISSUE: opc_full=" << opc_full << " at " << sc_time_stamp() << "," << sc_delta_count_at_current_time() << "\n";
                    WARPS[i % num_warp].dispatch_warp_valid = true;
                    dispatch_valid = true;
                    issue_ins = WARPS[i % num_warp].ififo.front();
                    issueins_warpid = i % num_warp;
                    find_dispatchwarp = true;
                    last_dispatch_warpid = i % num_warp + 1;
                }
                else
                {
                    WARPS[i % num_warp].dispatch_warp_valid = false;
                    // cout << "ISSUE: let warp" << i % num_warp << " dispatch_warp_valid=false at " << sc_time_stamp() << "," << sc_delta_count_at_current_time() << "\n";
                }
            }
            if (!find_dispatchwarp)
                dispatch_valid = false;
        }
    }
}

bank_t BASE::bank_decode(int warp_id, int srcaddr)
{
    bank_t tmp;
    tmp.bank_id = srcaddr % BANK_NUM;
    tmp.addr = warp_id * 32 / BANK_NUM + srcaddr / BANK_NUM; // 32是每个warp寄存器有32个元素
    return tmp;
}

warpaddr_t BASE::bank_undecode(int bank_id, int bankaddr)
{
    warpaddr_t tmp;
    tmp.warp_id = bankaddr / 8;
    tmp.addr = (bankaddr % (32 / BANK_NUM)) * BANK_NUM + bank_id;
    return tmp;
}

void BASE::OPC_FIFO()
{
    vector_t printdata_;
    I_TYPE _readdata4;
    int _readwarpid;
    std::array<bool, 4> in_ready;
    std::array<bool, 4> in_valid;
    std::array<bank_t, 4> in_srcaddr;
    std::array<bool, 4> in_banktype;
    while (true)
    {
        wait();
        // cout << "OPC_FIFO start at " << sc_time_stamp() << "," << sc_delta_count_at_current_time() << "\n";
        if (doemit)
        {
            // cout << "opcfifo is popping index " << emit_idx << " at " << sc_time_stamp() <<","<< sc_delta_count_at_current_time() << "\n";
            auto popdat = opcfifo[emit_idx];
            opcfifo.pop(emit_idx); // last cycle emit
            // cout << "OPC_FIFO: poped ins " << popdat.ins << "warp" << popdat.warp_id << " at " << sc_time_stamp() << "," << sc_delta_count_at_current_time() << "\n";
        }
        ev_opc_pop.notify();
        // 按目前的事件顺序，若发生某ins进入OPC而立刻ready，则会有问题，后续要修改
        if (dispatch_valid)
        {
            // cout << "opc begin to put at " << sc_time_stamp() << "," << sc_delta_count_at_current_time() << "\n";
            if (opc_full && doemit == false) // 相当于上一cycle dispatch_ready
            {
                // if not ready, just wait, no need throw ERROR
                // cout << "OPC ERROR: is full but receive ins from issue at " << sc_time_stamp() << "," << sc_delta_count_at_current_time() << "\n";
            }
            else
            {
                _readdata4 = issue_ins.read();
                _readwarpid = issueins_warpid;
                switch (_readdata4.op)
                {
                case lw_:
                case vle32v_:
                    in_ready = {0, 1, 1, 1};
                    in_valid = {1, 0, 0, 0};
                    in_srcaddr[0] = bank_decode(_readwarpid, _readdata4.s1);
                    in_banktype = {0, 0, 0, 0};
                    break;
                case addi_:
                    in_ready = {0, 1, 1, 1};
                    in_valid = {1, 0, 0, 0};
                    in_srcaddr[0] = bank_decode(_readwarpid, _readdata4.s1);
                    in_banktype = {0, 0, 0, 0};
                    break;
                case add_:
                case beq_:
                    in_ready = {0, 0, 1, 1};
                    in_valid = {1, 1, 0, 0};
                    in_srcaddr[0] = bank_decode(_readwarpid, _readdata4.s1);
                    in_srcaddr[1] = bank_decode(_readwarpid, _readdata4.s2);
                    in_banktype = {0, 0, 0, 0};
                    break;
                case vaddvv_:
                case vfaddvv_:
                    in_ready = {0, 0, 1, 1};
                    in_valid = {1, 1, 0, 0};
                    in_srcaddr[0] = bank_decode(_readwarpid, _readdata4.s1);
                    in_srcaddr[1] = bank_decode(_readwarpid, _readdata4.s2);
                    in_banktype = {1, 1, 0, 0};
                    break;
                case vaddvx_:
                    in_ready = {0, 0, 1, 1};
                    in_valid = {1, 1, 0, 0};
                    in_srcaddr[0] = bank_decode(_readwarpid, _readdata4.s1);
                    in_srcaddr[1] = bank_decode(_readwarpid, _readdata4.s2);
                    in_banktype = {1, 0, 0, 0};
                    break;
                case INVALID_:
                    cout << "OPC error: issue_ins INVALID_ at " << sc_time_stamp() << "," << sc_delta_count_at_current_time() << "\n";
                    break;
                default:
                    cout << "OPC warning: OPC_FIFO switch to unrecognized branch\n";
                }
                // cout << "opcfifo push issue_ins " << _readdata4 << "warp" << _readwarpid
                //      << ", srcaddr=(" << in_srcaddr[0] << ";" << in_srcaddr[1] << ")"
                //      << " at " << sc_time_stamp() << "," << sc_delta_count_at_current_time() << "\n";
                opcfifo.push(opcfifo_t(_readdata4, _readwarpid,
                                       in_ready, in_valid, in_srcaddr, in_banktype));
            }
        }
        opcfifo_elem_num = opcfifo.get_size();
        opc_full = opcfifo.get_size() == OPCFIFO_SIZE;
        opc_empty = opcfifo.get_size() == 0;
        // cout << "OPC_FIFO waiting ev_opc_judge_emit at " << sc_time_stamp() << "," << sc_delta_count_at_current_time() << "\n";
        wait(ev_opc_judge_emit);
        // cout << "OPC_FIFO get ev_opc_judge_emit at " << sc_time_stamp() << "," << sc_delta_count_at_current_time() << "\n";

        //  由ready写入entry，不能影响当前cycle判断emit
        for (int i = 0; i < OPCFIFO_SIZE; i++)
        {
            for (int j = 0; j < 4; j++)
                if (opc_ready[i][j] == true)
                {
                    if (opcfifo[i].valid[j] == false)
                        cout << "opc collect error[" << i << "," << j << "]: ready=1 but valid=0 at " << sc_time_stamp() << "," << sc_delta_count_at_current_time() << "\n";
                    opcfifo[i].ready[j] = true;
                    opcfifo[i].valid[j] = false;
                    opcfifo[i].data[j] = read_data[opcfifo[i].srcaddr[j].bank_id];
                    printdata_ = read_data[opcfifo[i].srcaddr[j].bank_id];
                    // cout << "OPC_FIFO: store_in, ins=" << opcfifo[i].ins << "warp" << opcfifo[i].warp_id
                    //      << ", data[" << j << "]=" << printdata_ << ", srcaddr=" << opcfifo[i].srcaddr[j] << " at " << sc_time_stamp() << "," << sc_delta_count_at_current_time() << "\n";
                }
        }
        ev_opc_store.notify();
    }
}

void BASE::OPC_EMIT()
{
    reg_t pa1;
    reg_t *pa2; // 用于int转float
    float *pf1, *pf2;
    FloatAndInt pr1, pr2;
    last_emit_entryid = 0;
    while (true)
    {
        wait(ev_opc_pop & // 等opc当前cycle pop之后再判断下一cycle的pop
             ev_saluready_updated & ev_valuready_updated &
             ev_vfpuready_updated & ev_lsuready_updated);
        // cout << "OPC_EMIT start at " << sc_time_stamp() << "," << sc_delta_count_at_current_time() << "\n";
        doemit = false;
        findemit = 0;
        emito_salu = false;
        emito_valu = false;
        emito_vfpu = false;
        emito_lsu = false;
        for (int i = last_emit_entryid; i < last_emit_entryid + OPCFIFO_SIZE; i++)
        {
            int entryidx = i % OPCFIFO_SIZE;
            if (findemit)
                break;
            if (opcfifo.tag_valid(entryidx) && opcfifo[entryidx].all_ready())
            {
                emit_ins = opcfifo[entryidx].ins;
                emitins_warpid = opcfifo[entryidx].warp_id;
                // cout << "opcfifo[" << entryidx << "]-" << emit_ins << "is all ready, at " << sc_time_stamp() << "," << sc_delta_count_at_current_time() << "\n";
                switch (opcfifo[entryidx].ins.op)
                {
                case add_:
                case addi_:
                case beq_:
                    if (salu_ready)
                    {
                        emit_idx = entryidx;
                        last_emit_entryid = entryidx + 1;
                        findemit = 1;
                        doemit = true;
                        // cout << "OPC: salu is ready at " << sc_time_stamp() <<","<< sc_delta_count_at_current_time() << "\n";
                        emito_salu = true;
                        rss1_data = opcfifo[entryidx].data[0][0];
                        rss2_data = opcfifo[entryidx].data[1][0];
                    }
                    else
                    {
                        // cout << "OPC_EMIT: find all_ready ins " << opcfifo[entryidx].ins << "warp" << opcfifo[entryidx].warp_id << " but salu not ready at " << sc_time_stamp() << "," << sc_delta_count_at_current_time() << "\n";
                    }
                    break;
                case vaddvv_:
                    if (valu_ready)
                    {
                        emit_idx = entryidx;
                        last_emit_entryid = entryidx + 1;
                        findemit = 1;
                        doemit = true;
                        emito_valu = true;
                        for (int j = 0; j < num_thread; j++)
                        {
                            rsv1_data[j] = opcfifo[entryidx].data[0][j];
                            rsv2_data[j] = opcfifo[entryidx].data[1][j];
                        }
                    }
                    break;
                case vaddvx_:
                    if (valu_ready)
                    {
                        emit_idx = entryidx;
                        last_emit_entryid = entryidx + 1;
                        findemit = 1;
                        doemit = true;
                        emito_valu = true;
                        for (int j = 0; j < num_thread; j++)
                        {
                            rsv1_data[j] = opcfifo[entryidx].data[0][j];
                        }
                        rss2_data = opcfifo[entryidx].data[1][0];
                    }
                    break;
                case vfaddvv_:
                    if (vfpu_ready)
                    {
                        emit_idx = entryidx;
                        last_emit_entryid = entryidx + 1;
                        findemit = 1;
                        doemit = true;
                        emito_vfpu = true;
                        // cout << "OPC_EMIT: will emit ins" << opcfifo[entryidx].ins << "warp" << opcfifo[entryidx].warp_id << ", rs1_data={";
                        // for (int j = 0; j < num_thread; j++)
                        // {
                        //     cout << std::hex << opcfifo[entryidx].data[0][j] << std::dec << ",";
                        // }
                        // cout << "}, rs2_data={";
                        // for (int j = 0; j < num_thread; j++)
                        // {
                        //     cout << std::hex << opcfifo[entryidx].data[1][j] << std::dec << ",";
                        // }
                        // cout << "}, at " << sc_time_stamp() << "," << sc_delta_count_at_current_time() << "\n";
                        for (int j = 0; j < num_thread; j++)
                        {
                            pr1.i = opcfifo[entryidx].data[0][j];
                            rsf1_data[j] = pr1.f;
                            pr2.i = opcfifo[entryidx].data[1][j];
                            rsf2_data[j] = pr2.f;
                        }
                    }
                    break;
                case lw_:
                case vle32v_:
                    if (lsu_ready)
                    {
                        emit_idx = entryidx;
                        last_emit_entryid = entryidx + 1;
                        findemit = 1;
                        doemit = true;
                        emito_lsu = true;
                        rss1_data = opcfifo[entryidx].data[0][0];
                    }
                    break;
                case INVALID_:
                    break;
                }
            }
        }
        // cout << "emit_idx is set to " << emit_idx << "\n";
        ev_opc_judge_emit.notify();
    }
}

void BASE::OPC_FETCH()
{
    while (true)
    {
        wait(ev_opc_store);
        for (int i = 0; i < OPCFIFO_SIZE; i++)
        {
            if (opcfifo.tag_valid(i) == false)
            {
                opc_valid[i].fill(false);
            }
            else
            {
                opc_valid[i] = opcfifo[i].valid;
                opc_srcaddr[i] = opcfifo[i].srcaddr;
                opc_banktype[i] = opcfifo[i].banktype;
            }
        }
        ev_opc_collect.notify();
    }
}

std::pair<int, int> BASE::reg_arbiter(
    const std::array<std::array<bank_t, 4>, OPCFIFO_SIZE> &addr_arr, // opc_srcaddr
    const std::array<std::array<bool, 4>, OPCFIFO_SIZE> &valid_arr,  // opc_valid
    std::array<std::array<bool, 4>, OPCFIFO_SIZE> &ready_arr,        // opc_ready
    int bank_id,
    std::array<int, BANK_NUM> &REGcurrentIdx,
    std::array<int, BANK_NUM> &read_bank_addr)
{
    const int rows = OPCFIFO_SIZE; // = addr_arr.size()
    const int cols = 4;            // = addr_arr[0].size(), 每个opc_fifo_t四个待取元素
    const int size = rows * cols;
    std::pair<int, int> result(-1, -1); // 默认值表示没有找到有效数据
    int index, i, j;
    for (int idx = REGcurrentIdx[bank_id] % size;
         idx < size + REGcurrentIdx[bank_id] % size; idx++)
    {
        index = idx % size;
        i = index / cols;
        j = index % cols;
        if (valid_arr[i][j] == true)
        {
            if (addr_arr[i][j].bank_id == bank_id)
            {
                read_bank_addr[bank_id] = addr_arr[i][j].addr; // 下周期读数据
                // cout << "let read_bank_addr(bank" << bank_id << ")="
                //      << addr_arr[i][j].addr << " at " << sc_time_stamp() << "," << sc_delta_count_at_current_time() << "\n";
                result.first = i;
                result.second = j;
                ready_arr[i][j] = true;
                REGcurrentIdx[bank_id] = index + 1;
                break;
            }
        }
    }
    return result;
}

void BASE::READ_REG()
{
    std::pair<int, int> temp_pair(-1, -1);
    int row, col;
    REGselectIdx.fill({-1, -1});
    warpaddr_t tmp;
    while (true)
    {
        wait(clk.posedge_event());
        // 先根据上一cycle regfile arbiter的结果读数据
        for (int i = 0; i < BANK_NUM; i++)
        {
            row = REGselectIdx[i].first;
            col = REGselectIdx[i].second;
            if (REGselectIdx[i] != temp_pair)
            {
                tmp = bank_undecode(i, read_bank_addr[i]);
                // cout << opcfifo[row].ins << "warp" << opcfifo[row].warp_id;
                // cout << " decode(bank" << i << ",addr" << read_bank_addr[i]
                //      << ") undecode --> warp_id=" << tmp.warp_id << ", addr=" << tmp.addr << " at " << sc_time_stamp() << "," << sc_delta_count_at_current_time() << "\n";
                // cout << "从regfile读出: REGselectIdx[" << i << "] to opc(" << REGselectIdx[i].first << "," << REGselectIdx[i].second << ") at " << sc_time_stamp() <<","<< sc_delta_count_at_current_time() << "\n";
                if (opc_banktype[row][col] == 0)
                {
                    read_data[i][0] = WARPS[tmp.warp_id].s_regfile[tmp.addr];
                }
                else
                {
                    read_data[i] = WARPS[tmp.warp_id].v_regfile[tmp.addr];
                }
            }
        }
        wait(ev_opc_collect);
        for (auto &elem : opc_ready)
            elem.fill(0);
        for (int i = 0; i < BANK_NUM; i++)
        {
            REGselectIdx[i] = reg_arbiter(opc_srcaddr, opc_valid, opc_ready, i, REGcurrentIdx, read_bank_addr);
        }
    }
}

void BASE::WRITE_BACK()
{
    write_s = write_v = write_f = false;
    execpop_salu = execpop_valu = execpop_vfpu = execpop_lsu = false;

    FloatAndInt newFI;

    while (true)
    {
        wait(ev_salufifo_pushed & ev_valufifo_pushed & ev_vfpufifo_pushed &
             ev_lsufifo_pushed);
        // cout << "WRITEBACK: start at " << sc_time_stamp() << "," << sc_delta_count_at_current_time() << "\n";
        if (execpop_salu)
            salufifo.pop();
        if (execpop_valu)
        {
            valufifo.pop();
            // cout << "valu: execute pop at "<< sc_time_stamp() << "," << sc_delta_count_at_current_time() << "\n";
        }
        if (execpop_vfpu)
            vfpufifo.pop();
        if (execpop_lsu)
            lsufifo.pop();
        salufifo_empty = salufifo.isempty();
        if (!salufifo_empty)
            salutop_dat = salufifo.front();
        salufifo_elem_num = salufifo.used();
        valufifo_empty = valufifo.isempty();
        if (!valufifo_empty)
            valutop_dat = valufifo.front();
        valufifo_elem_num = valufifo.used();
        vfpufifo_empty = vfpufifo.isempty();
        if (!vfpufifo_empty)
            vfputop_dat = vfpufifo.front();
        vfpufifo_elem_num = vfpufifo.used();
        lsufifo_empty = lsufifo.isempty();
        if (!lsufifo_empty)
            lsutop_dat = lsufifo.front();
        lsufifo_elem_num = lsufifo.used();

        if (salufifo_empty == false)
        {
            write_s = true;
            write_v = false;
            write_f = false;
            wb_ena = true;
            execpop_salu = true;
            execpop_lsu = false;
            execpop_valu = false;
            execpop_vfpu = false;
            // cout << "do write_s=true at " << sc_time_stamp() <<","<< sc_delta_count_at_current_time() << "\n";
            wb_ins = salutop_dat.ins;
            rds1_addr = salutop_dat.ins.d;
            rds1_data = salutop_dat.data;
            wb_warpid = salutop_dat.warp_id;
        }
        else if (valufifo_empty == false)
        {
            write_v = true;
            write_s = false;
            write_f = false;
            wb_ena = true;
            execpop_valu = true;
            execpop_salu = false;
            execpop_vfpu = false;
            execpop_lsu = false;
            wb_ins = valutop_dat.ins;
            rdv1_addr = valutop_dat.ins.d;
            for (int i = 0; i < num_thread; i++)
            {
                rdv1_data[i] = valutop_dat.rdv1_data[i];
            }
            wb_warpid = valutop_dat.warp_id;
        }
        else if (vfpufifo_empty == false)
        {
            write_f = true;
            write_v = false;
            write_s = false;
            wb_ena = true;
            execpop_vfpu = true;
            execpop_salu = false;
            execpop_valu = false;
            execpop_lsu = false;
            wb_ins = vfputop_dat.ins;
            rdf1_addr = vfputop_dat.ins.d;
            // cout << "WB: let wb_ins=" << vfputop_dat.ins << "warp" << vfputop_dat.warp_id << ", rdf1_data={";
            for (int i = 0; i < num_thread; i++)
            {
                newFI.f = vfputop_dat.rdf1_data[i];
                // cout << std::hex << newFI.i << std::dec << ",";
                rdf1_data[i].write(newFI);
            }
            wb_warpid = vfputop_dat.warp_id;
            // cout << "} at " << sc_time_stamp() << "," << sc_delta_count_at_current_time() << "\n";
        }
        else if (lsufifo_empty == false)
        {
            execpop_lsu = true;
            execpop_salu = false;
            execpop_valu = false;
            execpop_vfpu = false;
            switch (lsutop_dat.ins.op)
            {
            case lw_:
                write_s = true;
                write_v = false;
                write_f = false;
                wb_ena = true;
                wb_ins = lsutop_dat.ins;
                rds1_addr = lsutop_dat.ins.d;
                rds1_data = lsutop_dat.rds1_data;
                // cout << "WB: arbit lw_ writeback, ins=" << lsutop_dat.ins << "warp" << lsutop_dat.warp_id << " at " << sc_time_stamp() << "," << sc_delta_count_at_current_time() << "\n";
                break;
            case vle32v_:
                write_v = true;
                write_s = false;
                write_f = false;
                wb_ena = true;
                wb_ins = lsutop_dat.ins;
                rdv1_addr = lsutop_dat.ins.d;
                for (int i = 0; i < num_thread; i++)
                {
                    rdv1_data[i] = lsutop_dat.rdv1_data[i];
                }
                break;
            default:
                cout << "wb error: lsu unrecognized ins\n";
                break;
            }
            wb_warpid = lsutop_dat.warp_id;
        }
        else
        {
            write_s = false;
            write_v = false;
            write_f = false;
            wb_ena = false;
            execpop_salu = false;
            execpop_valu = false;
            execpop_vfpu = false;
            execpop_lsu = false;
        }
    }
}

void BASE::WRITE_REG(int warp_id)
{
    float f1;
    float *pa1;
    while (true)
    {
        wait(clk.posedge_event());
        if (wb_warpid == warp_id)
        {
            // 后续regfile要一次只能写一个，否则报错
            if (write_s)
            {
                cout << "WRITE_REG ins" << wb_ins << "warp" << warp_id << ": scalar, s_regfile[" << rds1_addr.read() << "]="
                     << std::hex << rds1_data << std::dec
                     << " at " << sc_time_stamp() << "," << sc_delta_count_at_current_time() << "\n";
                WARPS[warp_id].s_regfile[rds1_addr.read()] = rds1_data;
            }
            if (write_v)
            {
                cout << "WRITE_REG ins" << wb_ins << "warp" << warp_id << ": vector, v_regfile[" << rdv1_addr.read() << "]={"
                     << std::hex
                     << rdv1_data[0] << "," << rdv1_data[1] << "," << rdv1_data[2] << "," << rdv1_data[3] << ","
                     << rdv1_data[4] << "," << rdv1_data[5] << "," << rdv1_data[6] << "," << rdv1_data[7]
                     << std::dec
                     << "} at " << sc_time_stamp() << "," << sc_delta_count_at_current_time() << "\n";
                for (int i = 0; i < num_thread; i++)
                    WARPS[warp_id].v_regfile[rdv1_addr.read()][i] = rdv1_data[i];
            }
            if (write_f)
            {
                cout << "WRITE_REG ins" << wb_ins << "warp" << warp_id << ": float, v_regfile[" << rdf1_addr.read() << "]={"
                     << std::hex
                     << rdf1_data[0].read().i << "," << rdf1_data[1].read().i << "," << rdf1_data[2].read().i << "," << rdf1_data[3].read().i << ","
                     << rdf1_data[4].read().i << "," << rdf1_data[5].read().i << "," << rdf1_data[6].read().i << "," << rdf1_data[7].read().i
                     << std::dec
                     << "} at " << sc_time_stamp() << "," << sc_delta_count_at_current_time() << "\n";
                for (int i = 0; i < num_thread; i++)
                {
                    // f1 = rdf1_data[i];
                    // pa1 = &f1;
                    // WARPS[warp_id].v_regfile[rdf1_addr.read()][i] = *(reg_t *)(pa1);
                    WARPS[warp_id].v_regfile[rdf1_addr.read()][i] = rdf1_data[i].read().i;
                }
            }
        }
    }
}
