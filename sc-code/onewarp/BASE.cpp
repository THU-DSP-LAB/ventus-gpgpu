#include "BASE.h"

void BASE::debug_sti()
{
}

void BASE::debug_display()
{
    while (true)
    {
        wait(ev_salufifo_updated);
        cout << "ev_salufifo_updated triggered at " << sc_time_stamp() << "," << sc_delta_count_at_current_time() << "\n";
    }
}
void BASE::debug_display1()
{
    while (true)
    {
        wait(ev_valufifo_updated);
        cout << "ev_valufifo_updated triggered at " << sc_time_stamp() << "," << sc_delta_count_at_current_time() << "\n";
    }
}
void BASE::debug_display2()
{
    while (true)
    {
        wait(ev_vfpufifo_updated);
        cout << "ev_vfpufifo_updated triggered at " << sc_time_stamp() << "," << sc_delta_count_at_current_time() << "\n";
    }
}
void BASE::debug_display3()
{
    while (true)
    {
        wait(ev_lsufifo_updated);
        cout << "ev_lsufifo_updated triggered at " << sc_time_stamp() << "," << sc_delta_count_at_current_time() << "\n";
    }
}

void BASE::PROGRAM_COUNTER()
{
    while (true)
    {
        // wait();
        // cout << "PC start by clk at " << sc_time_stamp() << "," << sc_delta_count_at_current_time() << "\n";
        wait(ev_ibuf_inout);
        // cout << "PC start, ibuf_swallow=" << ibuf_swallow << " at " << sc_time_stamp() << "," << sc_delta_count_at_current_time() << "\n";
        if (rst_n == 0)
        {
            pc = -1;
            fetch_valid = false;
        }
        else if (jump == 1)
        {
            pc = jump_addr;
            fetch_valid = true;
            // cout << "pc jumps to addr " << jump_addr.read() << " at " << sc_time_stamp() <<","<< sc_delta_count_at_current_time() << "\n";
        }
        else if (ibuf_empty | ibuf_swallow)
        {
            // cout << "pc will +1 at " << sc_time_stamp() << "," << sc_delta_count_at_current_time() << "\n";
            pc = pc.read() + 1;
            fetch_valid = true;
        }
        else
        {
            fetch_valid = false;
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

    while (true)
    {
        fetch_ins = (pc.read() >= 0) ? ireg[pc.read()] : I_TYPE(INVALID_, 0, 0, 0);
        // cout << "pc=" << pc << ", fetch_ins is " << fetch_ins << " at " << sc_time_stamp()
        //      << ", it will be " << ireg[pc.read()] << " at the next timestamp"
        //      << "\n";
        ev_decode.notify();
        wait(pc.value_changed_event());
        // cout << "pc.value_changed_event() triggered\n";
    }
}

void BASE::DECODE()
{
    while (true)
    {
        wait(ev_decode);
        switch (fetch_ins.op)
        {
        case beq_:
            decode_ins = I_TYPE(fetch_ins, pc.read() + 1 + fetch_ins.d);
            // cout << "decoding beq ins at " << sc_time_stamp() <<","<< sc_delta_count_at_current_time() << "\n";
            break;
        default:
            decode_ins = I_TYPE(fetch_ins, -1);
            break;
        }
    }
}

void BASE::IBUF_ACTION()
{
    I_TYPE dispatch_ins_;
    I_TYPE _readdata3;
    while (true)
    {
        wait();
        ibuf_swallow = false;
        // cout << "IBUF: entering ibuf at " << sc_time_stamp() <<","<< sc_delta_count_at_current_time() << "\n";
        if (rst_n.read() == 0)
            ififo.clear();
        else
        {
            if (dispatch)
            {
                // cout << "before dispatch, ififo has " << ififo.used() << " elems at " << sc_time_stamp() <<","<< sc_delta_count_at_current_time() << "\n";
                dispatch_ins_ = ififo.get();
                // cout << "IBUF: after dispatch, ififo has " << ififo.used() << " elems at " << sc_time_stamp() <<","<< sc_delta_count_at_current_time() << "\n";
            }
            else
            {
                // cout << "IBUF: dispatch == false at " << sc_time_stamp() <<","<< sc_delta_count_at_current_time() << "\n";
            }
            if (fetch_valid && jump == false)
            {
                if (ififo.isfull())
                {
                    // cout << "IBUF ERROR: ibuf is full but is sent an ins from FETCH at " << sc_time_stamp() <<","<< sc_delta_count_at_current_time() << "\n";
                }
                else
                {
                    ififo.push(decode_ins.read());
                    ibuf_swallow = true;
                }
                // cout << "before put, ififo has " << ififo.used() << " elems at " << sc_time_stamp() <<","<< sc_delta_count_at_current_time() << "\n";

                // cout << "after put, ififo has " << ififo.used() << " elems at " << sc_time_stamp() <<","<< sc_delta_count_at_current_time() << "\n";
                // cout << "ififo has put ins " << fetch_ins << ", whose jump_addr is " << fetch_ins.jump_addr << " at " << sc_time_stamp() <<","<< sc_delta_count_at_current_time() << "\n";
            }
            else if (jump)
            {
                // cout << "ibuf detected jump at " << sc_time_stamp() <<","<< sc_delta_count_at_current_time() << "\n";
                ififo.clear();
            }
        }
        ev_ibuf_inout.notify();
    }
}

void BASE::IBUF_PARAM()
{
    while (true)
    {
        wait(ev_ibuf_inout);
        ibuf_empty = ififo.isempty();
        if (ififo.isempty())
        {
            ififo_elem_num = 0;
            ibuftop_ins = I_TYPE(INVALID_, -1, 0, 0);
        }
        else
        {
            ibuftop_ins.write(ififo.front());
            ififo_elem_num = ififo.used();
            // cout << "ififo has " << ififo.used() << " elems in it at " << sc_time_stamp() <<","<< sc_delta_count_at_current_time() << "\n";
        }
        ev_ibuf_updated.notify();
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
        if (wb_ena)
        {
            tmpins = wb_ins;
            // cout << "scoreboard: wb_ins is " << tmpins << " at " << sc_time_stamp() <<","<< sc_delta_count_at_current_time() << "\n";
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
            // cout << "scoreboard写回: 正在寻找 SCORE " << SCORE_TYPE(regtype_, tmpins.d) << " at " << sc_time_stamp() <<","<< sc_delta_count_at_current_time() << "\n";
            if (it == score.end())
            {
                cout << "wb_ena error: scoreboard can't find rd in score set, wb_ins=" << wb_ins << " at " << sc_time_stamp() << "," << sc_delta_count_at_current_time() << "\n";
                break;
            }
            score.erase(it);
            cout << "scoreboard: succesfully erased SCORE " << SCORE_TYPE(regtype_, tmpins.d) << ", wb_ins=" << wb_ins << " at " << sc_time_stamp() << "," << sc_delta_count_at_current_time() << "\n";
        }
        if (branch_sig)
        {
            if (wait_bran == 0)
            {
                cout << "scoreboard error: detect branch_sig=1(from salu) while wait_bran=0 at " << sc_time_stamp() << "," << sc_delta_count_at_current_time() << "\n";
            }
            else if (dispatch)
            {
                cout << "scoreboard error: detect branch_sig=1(from salu) while dispatch=1 at " << sc_time_stamp() << "," << sc_delta_count_at_current_time() << "\n";
            }
            wait_bran = 0;
        }
        else if (ibuftop_ins.read().op == beq_ && dispatch)
        {
            // cout << "ibuf let wait_bran=1 at " << sc_time_stamp() <<","<< sc_delta_count_at_current_time() << "\n";
            wait_bran = 1;
        }
        if (dispatch)
        {
            tmpins = ibuftop_ins; // this ibuftop_ins is the old data
            if (tmpins.op != beq_)
            {
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
                cout << "scoreboard insert " << SCORE_TYPE(regtype_, tmpins.d)
                     << " because of dispatch " << tmpins << " at " << sc_time_stamp() << "," << sc_delta_count_at_current_time() << "\n";
            }
        }
        ev_judge_dispatch.notify();
    }
}

void BASE::JUDGE_DISPATCH()
{
    I_TYPE _readibuf;
    while (true)
    {
        wait(ev_judge_dispatch & ev_ibuf_updated);
        // cout << "scoreboard: ibuftop_ins=" << ibuftop_ins << " at " << sc_time_stamp() <<","<< sc_delta_count_at_current_time() << "\n";
        if (wait_bran | jump)
        {
            can_dispatch = false;
        }
        else if (!ififo.isempty())
        {
            _readibuf = ififo.front();
            switch (_readibuf.op)
            {
            case lw_:
                // cout << "JUDGE_DISPATCH switch to lw_ case at " << sc_time_stamp() <<","<< sc_delta_count_at_current_time() << "\n";
                if (score.find(SCORE_TYPE(s, _readibuf.s1)) == score.end() &&
                    score.find(SCORE_TYPE(s, _readibuf.d)) == score.end())
                    can_dispatch = true;
                else
                    can_dispatch = false;
                break;
            case add_:
                // cout << "JUDGE_DISPATCH switch to add_ case at " << sc_time_stamp() <<","<< sc_delta_count_at_current_time() << "\n";
                if (score.find(SCORE_TYPE(s, _readibuf.s1)) == score.end() &&
                    score.find(SCORE_TYPE(s, _readibuf.s2)) == score.end() &&
                    score.find(SCORE_TYPE(s, _readibuf.d)) == score.end())
                    can_dispatch = true;
                else
                    can_dispatch = false;
                break;
            case beq_:
                if (score.find(SCORE_TYPE(s, _readibuf.s1)) == score.end() &&
                    score.find(SCORE_TYPE(s, _readibuf.s2)) == score.end())
                    can_dispatch = true;
                else
                    can_dispatch = false;
                break;
            case vload_:
                if (score.find(SCORE_TYPE(v, _readibuf.s1)) == score.end() &&
                    score.find(SCORE_TYPE(v, _readibuf.d)) == score.end())
                    can_dispatch = true;
                else
                    can_dispatch = false;
                break;
            case vaddvv_:
                // cout << "JUDGE_DISPATCH switch to vaddvv_ case at " << sc_time_stamp() <<","<< sc_delta_count_at_current_time() << "\n";
                if (score.find(SCORE_TYPE(v, _readibuf.s1)) == score.end() &&
                    score.find(SCORE_TYPE(v, _readibuf.s2)) == score.end() &&
                    score.find(SCORE_TYPE(v, _readibuf.d)) == score.end())
                    can_dispatch = true;
                else
                    can_dispatch = false;
                break;
            case vaddvx_:
                // cout << "JUDGE_DISPATCH switch to vaddvx_ case at " << sc_time_stamp() <<","<< sc_delta_count_at_current_time() << "\n";
                if (score.find(SCORE_TYPE(v, _readibuf.s1)) == score.end() &&
                    score.find(SCORE_TYPE(s, _readibuf.s2)) == score.end() &&
                    score.find(SCORE_TYPE(v, _readibuf.d)) == score.end())
                    can_dispatch = true;
                else
                {
                    can_dispatch = false;
                    // cout << "JUDGE_DISPATCH don't dispatch vaddvx_, ins is " << ibuftop_ins << ", at " << sc_time_stamp() <<","<< sc_delta_count_at_current_time() << "\n";
                }
                break;
            case vfadd_:
                // cout << "JUDGE_DISPATCH switch to vfadd_ case at " << sc_time_stamp() <<","<< sc_delta_count_at_current_time() << "\n";
                if (score.find(SCORE_TYPE(f, _readibuf.s1)) == score.end() &&
                    score.find(SCORE_TYPE(f, _readibuf.s2)) == score.end() &&
                    score.find(SCORE_TYPE(f, _readibuf.d)) == score.end())
                    can_dispatch = true;
                else
                    can_dispatch = false;
                break;
            default:
                can_dispatch = false;
                // cout << "JUDGE_DISPATCH switch to default case at " << sc_time_stamp() <<","<< sc_delta_count_at_current_time() << "\n";
                break;
            }
        }
        ev_issue.notify();
    }
}

void BASE::ISSUE_ACTION()
{
    while (true)
    {
        wait(ev_issue);
        if (can_dispatch && (emit == true | opc_full == false))
        {
            dispatch = true;
            issue_ins = ififo.front();
        }
        else
            dispatch = false;
    }
}

bank_t BASE::bank_decode(int warp_id, int srcaddr)
{
    bank_t tmp;
    tmp.bank_id = srcaddr % BANK_NUM;
    tmp.addr = warp_id * 32 / BANK_NUM + srcaddr / BANK_NUM;
    return tmp;
}

void BASE::OPC_FIFO()
{
    I_TYPE _readdata4;
    std::array<bool, 4> in_ready;
    std::array<bool, 4> in_valid;
    std::array<bank_t, 4> in_srcaddr;
    std::array<bool, 4> in_banktype;
    while (true)
    {
        wait();
        // cout << "OPC_FIFO start at " << sc_time_stamp() << "," << sc_delta_count_at_current_time() << "\n";
        if (jump)
        {
            opcfifo.clear();
            opc_full = false;
            opc_empty = true;
            opctop_ins = I_TYPE(INVALID_, 0, 0, 0);
            opcfifo_elem_num = 0;
            ev_opc_pop.notify();
        }
        else
        {
            if (emit)
            {
                // cout << "opcfifo is popping index " << emit_idx << " at " << sc_time_stamp() <<","<< sc_delta_count_at_current_time() << "\n";
                opcfifo.pop(emit_idx); // last cycle emit
            }
            ev_opc_pop.notify();
            if (dispatch && jump == false)
            {
                // cout << "opc begin to put at " << sc_time_stamp() << "," << sc_delta_count_at_current_time() << "\n";
                if (opc_full)
                {
                    cout << "OPC ERROR: is full but receive ins from issue at " << sc_time_stamp() << "," << sc_delta_count_at_current_time() << "\n";
                }
                else
                {
                    _readdata4 = issue_ins.read();
                    switch (_readdata4.op)
                    {
                    case lw_:
                    case vload_:
                        in_ready = {0, 1, 1, 1};
                        in_valid = {1, 0, 0, 0};
                        in_srcaddr[0] = bank_decode(0, _readdata4.s1); // 暂时warp_id=0
                        in_banktype = {0, 0, 0, 0};
                        break;
                    case add_:
                    case beq_:
                        in_ready = {0, 0, 1, 1};
                        in_valid = {1, 1, 0, 0};
                        in_srcaddr[0] = bank_decode(0, _readdata4.s1);
                        in_srcaddr[1] = bank_decode(0, _readdata4.s2);
                        in_banktype = {0, 0, 0, 0};
                        break;
                    case vaddvv_:
                    case vfadd_:
                        in_ready = {0, 0, 1, 1};
                        in_valid = {1, 1, 0, 0};
                        in_srcaddr[0] = bank_decode(0, _readdata4.s1);
                        in_srcaddr[1] = bank_decode(0, _readdata4.s2);
                        in_banktype = {1, 1, 0, 0};
                        break;
                    case vaddvx_:
                        in_ready = {0, 0, 1, 1};
                        in_valid = {1, 1, 0, 0};
                        in_srcaddr[0] = bank_decode(0, _readdata4.s1);
                        in_srcaddr[1] = bank_decode(0, _readdata4.s2);
                        in_banktype = {1, 0, 0, 0};
                        break;
                    case INVALID_:
                        cout << "OPC error: issue_ins INVALID_ at " << sc_time_stamp() << "," << sc_delta_count_at_current_time() << "\n";
                        break;
                    default:
                        cout << "OPC warning: OPC_FIFO switch to unrecognized branch\n";
                    }
                }
                opcfifo.push(opcfifo_t(_readdata4, in_ready, in_valid, in_srcaddr, in_banktype));
                // cout << "opcfifo has put issue_ins " << issue_ins << " at " << sc_time_stamp() <<","<< sc_delta_count_at_current_time() << "\n";
            }
        }
        opcfifo_elem_num = opcfifo.get_size();
        opc_full = opcfifo_elem_num == OPCFIFO_SIZE;
        opc_empty = opcfifo_elem_num == 0;
        //  由ready写入entry
        // cout << "OPC_FIFO waiting ev_opc_judge_emit at " << sc_time_stamp() << "," << sc_delta_count_at_current_time() << "\n";
        wait(ev_opc_judge_emit);
        // cout << "OPC_FIFO get ev_opc_judge_emit at " << sc_time_stamp() << "," << sc_delta_count_at_current_time() << "\n";
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
                }
        }
        ev_opc_store.notify();
    }
}

void BASE::OPC_EMIT()
{
    reg_t *pa1, *pa2; // 用于int转float
    while (true)
    {
        wait(ev_opc_pop);
        emit = 0;
        findemit = 0;
        emito_salu = emito_valu = emito_vfpu = emito_lsu = false;
        for (int i = 0; i < OPCFIFO_SIZE; i++)
        {
            if (findemit)
                break;
            if (opcfifo.tag_valid(i) && opcfifo[i].all_ready())
            {
                opctop_ins = opcfifo[i].ins;
                // cout << "opcfifo[" << i << "]-" << opctop_ins << "is all ready, at " << sc_time_stamp() << "," << sc_delta_count_at_current_time() << "\n";
                switch (opctop_ins.op)
                {
                case add_:
                case beq_:
                    if (salu_ready)
                    {
                        emit_idx = i;
                        findemit = 1;
                        // cout << "OPC: salu is ready at " << sc_time_stamp() <<","<< sc_delta_count_at_current_time() << "\n";
                        emito_salu = true;
                        emito_valu = emito_vfpu = emito_lsu = false;
                        rss1_data = opcfifo[i].data[0][0];
                        rss2_data = opcfifo[i].data[1][0];
                    }
                    else
                    {
                        emito_salu = emito_valu = emito_vfpu = emito_lsu = false;
                    }
                    break;
                case vaddvv_:
                    if (valu_ready)
                    {
                        emit_idx = i;
                        findemit = 1;
                        emito_valu = true;
                        emito_salu = emito_vfpu = emito_lsu = false;
                        for (int j = 0; j < num_thread; j++)
                        {
                            rsv1_data[j] = opcfifo[i].data[0][j];
                            rsv2_data[j] = opcfifo[i].data[1][j];
                        }
                    }
                    else
                        emito_salu = emito_valu = emito_vfpu = emito_lsu = false;
                    break;
                case vaddvx_:
                    if (valu_ready)
                    {
                        emit_idx = i;
                        findemit = 1;
                        emito_valu = true;
                        emito_salu = emito_vfpu = emito_lsu = false;
                        for (int j = 0; j < num_thread; j++)
                        {
                            rsv1_data[j] = opcfifo[i].data[0][j];
                        }
                        rss2_data = opcfifo[i].data[1][0];
                    }
                    else
                        emito_salu = emito_valu = emito_vfpu = emito_lsu = false;
                    break;
                case vfadd_:
                    if (vfpu_ready)
                    {
                        emit_idx = i;
                        findemit = 1;
                        emito_vfpu = true;
                        emito_salu = emito_valu = emito_lsu = false;
                        for (int j = 0; j < num_thread; j++)
                        {
                            pa1 = &(opcfifo[i].data[0][j]);
                            rsf1_data[j] = *((float *)pa1);
                            pa2 = &(opcfifo[i].data[1][j]);
                            rsf2_data[j] = *((float *)pa2);
                        }
                    }
                    else
                        emito_salu = emito_valu = emito_vfpu = emito_lsu = false;
                    break;
                case lw_:
                case vload_:
                    if (lsu_ready)
                    {
                        emit_idx = i;
                        findemit = 1;
                        emito_lsu = true;
                        emito_salu = emito_valu = emito_vfpu = false;
                        rss1_data = opcfifo[i].data[0][0];
                    }
                    else
                    {
                        // 注意，一旦写连等式，就不能用sc_signal，否则左侧变量会被右侧变量的旧值赋值
                        emito_salu = emito_valu = emito_vfpu = emito_lsu = false;
                    }
                    break;
                case INVALID_:
                    emito_salu = emito_valu = emito_vfpu = emito_lsu = false;
                    break;
                }
            }
        }
        emit = emito_salu | emito_valu | emito_vfpu | emito_lsu;
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

std::pair<int, int> BASE::reg_arbiter(const std::array<std::array<bank_t, 4>, OPCFIFO_SIZE> &addr_arr, // opc_srcaddr
                                      const std::array<std::array<bool, 4>, OPCFIFO_SIZE> &valid_arr,  // opc_valid
                                      std::array<std::array<bool, 4>, OPCFIFO_SIZE> &ready_arr,        // opc_ready
                                      int bank_id,
                                      std::array<int, BANK_NUM> &REGcurrentIdx)
{
    const int rows = OPCFIFO_SIZE; // = addr_arr.size()
    const int cols = 4;            // = addr_arr[0].size()
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
                read_bank_addr[bank_id] == addr_arr[i][j].addr;
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
    while (true)
    {
        wait();
        for (int i = 0; i < BANK_NUM; i++)
        {
            row = REGselectIdx[i].first;
            col = REGselectIdx[i].second;
            if (REGselectIdx[i] != temp_pair)
            {
                // cout << "从regfile读出: REGselectIdx[" << i << "] to opc(" << REGselectIdx[i].first << "," << REGselectIdx[i].second << ") at " << sc_time_stamp() <<","<< sc_delta_count_at_current_time() << "\n";
                if (opc_banktype[row][col] == 0)
                {
                    read_data[i][0] = s_regfile[read_bank_addr[i]];
                }
                else
                {
                    read_data[i] = v_regfile[read_bank_addr[i]];
                }
            }
        }
        wait(ev_opc_collect);

        for (auto &elem : opc_ready)
            elem.fill(0);
        // cout << "opc_ready is set to { ";
        // for (int i = 0; i < OPCFIFO_SIZE; i++)
        // {

        //     for (int j = 0; j < 4; j++)
        //     {
        //         cout << opc_ready[i][j];
        //     }
        //     cout << " ";
        // }
        // cout << "} at " << sc_time_stamp() <<","<< sc_delta_count_at_current_time() << "\n";
        for (int i = 0; i < BANK_NUM; i++)
        {
            REGselectIdx[i] = reg_arbiter(opc_srcaddr, opc_valid, opc_ready, i, REGcurrentIdx);
        }
    }
}

void BASE::WRITE_REG()
{
    float f1;
    float *pa1;
    while (true)
    {
        wait();
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
            {
                f1 = rdf1_data[i];
                pa1 = &f1;
                v_regfile[rdf1_addr.read()][i] = *(reg_t *)(pa1);
            }
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
        branch_sig = 0;
        if (emito_salu && jump == false)
        {
            // cout << "SALU_IN: emito_salu is " << emito_salu << " at " << sc_time_stamp() <<","<< sc_delta_count_at_current_time() << "\n";
            if (salu_ready_old == false)
            {
                cout << "salu error: not ready at " << sc_time_stamp() << "," << sc_delta_count_at_current_time() << "\n";
            }
            salu_unready.notify();
            switch (opctop_ins.op)
            {
            case beq_:
                branch_sig = 1;
                b_delay = 0;
                salu_eqb.notify(sc_time((b_delay)*PERIOD, SC_NS));
                if (rss1_data == rss2_data)
                {
                    jump = 1;
                    jump_addr = opctop_ins.jump_addr;
                    cout << "jump is updated to 1 at " << sc_time_stamp() << "," << sc_delta_count_at_current_time() << "\n";
                }
                break;
            case add_:
                new_data.ins = opctop_ins;
                new_data.rss1_data = rss1_data;
                new_data.rss2_data = rss2_data;
                salu_dq.push(new_data);
                // cout << "salu_dq has just pushed 1 elem at " << sc_time_stamp() <<","<< sc_delta_count_at_current_time() << "\n";
                a_delay = 1;
                b_delay = 1;
                if (a_delay == 0)
                    salu_eva.notify();
                else if (salu_eqa.default_event().triggered())
                    salu_eqa.notify(sc_time((a_delay)*PERIOD, SC_NS));
                else
                {
                    salu_eqa.notify(sc_time((a_delay)*PERIOD, SC_NS));
                    ev_salufifo_updated.notify();
                }
                cout << "SALU triggered eva/eqa at " << sc_time_stamp() << "," << sc_delta_count_at_current_time() << "\n";
                salu_eqb.notify(sc_time((b_delay)*PERIOD, SC_NS));
                // cout << "SALU_IN switch to add_ (from opc input) at " << sc_time_stamp() <<","<< sc_delta_count_at_current_time() << "\n";
                break;
            default:
                cout << "salu error: receive wrong ins " << opctop_ins << " at " << sc_time_stamp() << "," << sc_delta_count_at_current_time() << "\n";
                break;
            }
        }
        else if (!salu_eqa.default_event().triggered())
            ev_salufifo_updated.notify();
    }
}

void BASE::SALU_CALC()
{
    salufifo_elem_num = 0;
    salufifo_empty = 1;
    salu_in_t salutmp1;
    salu_out_t salutmp2;
    bool succeed;
    while (true)
    {
        wait(salu_eva | salu_eqa.default_event());
        cout << "SALU_OUT: triggered by eva/eqa at " << sc_time_stamp() << "," << sc_delta_count_at_current_time() << "\n";
        salutmp1 = salu_dq.front();
        // cout << "salu_dq.front's ins is " << salutmp1.ins << ", data is " << salutmp1.rss1_data << "," << salutmp1.rss2_data << "\n";
        salu_dq.pop();
        // cout << "salu_dq has poped, now its elem_num is " << salu_dq.size() << " at " << sc_time_stamp() <<","<< sc_delta_count_at_current_time() << "\n";
        switch (salutmp1.ins.op)
        {
        case add_:
            salutmp2.ins = salutmp1.ins;
            salutmp2.data = salutmp1.rss1_data + salutmp1.rss2_data;
            // cout << "SALU_OUT: do add at " << sc_time_stamp() <<","<< sc_delta_count_at_current_time() << "\n";
            salufifo.push(salutmp2);
            break;
        }
        ev_salufifo_updated.notify();
    }
}

void BASE::SALU_OUT()
{
    while (true)
    {
        wait();
        if (execpop_salu)
        {
            cout << "SALU_OUT: detected execpop at " << sc_time_stamp() << "," << sc_delta_count_at_current_time() << "\n";
            salufifo.pop();
        }
    }
}

void BASE::SALU_CTRL()
{
    salu_ready = true;
    salu_ready_old = true;
    while (true)
    {
        wait(salu_eqb.default_event() | salu_unready);
        if (salu_eqb.default_event().triggered())
        {
            // cout << "salu notified by b, previous salu_ready=" << salu_ready << ", update it to true at " << sc_time_stamp() <<","<< sc_delta_count_at_current_time() << "\n";
            salu_ready = true;
        }
        else if (salu_unready.triggered())
        { // else if很重要，对于b_delay=0的情况，salu_ready不会变0
            // cout << "salu notified by unready, previous salu_ready=" << salu_ready << ", update it to false at " << sc_time_stamp() <<","<< sc_delta_count_at_current_time() << "\n";
            salu_ready = false;
        }
        salu_ready_old = salu_ready;
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
        if (emito_valu && jump == false)
        {
            if (valu_ready == false)
            {
                cout << "valu error: not ready at " << sc_time_stamp() << "," << sc_delta_count_at_current_time() << "\n";
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
                b_delay = 1;
                // cout << "valu: receive vaddvv_, will notify eq, at " << sc_time_stamp() <<","<< sc_delta_count_at_current_time() << "\n";
                if (a_delay == 0)
                    valu_eva.notify();
                else if (valu_eqa.default_event().triggered())
                    valu_eqa.notify(sc_time((a_delay)*PERIOD, SC_NS));
                else
                {
                    valu_eqa.notify(sc_time((a_delay)*PERIOD, SC_NS));
                    ev_valufifo_updated.notify();
                }
                valu_eqb.notify(sc_time((b_delay)*PERIOD, SC_NS));
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
                b_delay = 1;
                // cout << "valu: receive vaddvx_, will notify eq, at " << sc_time_stamp() <<","<< sc_delta_count_at_current_time() << "\n";
                if (a_delay == 0)
                    valu_eva.notify();
                else if (valu_eqa.default_event().triggered())
                    valu_eqa.notify(sc_time((a_delay)*PERIOD, SC_NS));
                else
                {
                    valu_eqa.notify(sc_time((a_delay)*PERIOD, SC_NS));
                    ev_valufifo_updated.notify();
                }
                valu_eqb.notify(sc_time((b_delay)*PERIOD, SC_NS));
                break;
            default:
                cout << "valu error: receive wrong ins " << opctop_ins << " at " << sc_time_stamp() << "," << sc_delta_count_at_current_time() << "\n";
                break;
            }
        }
        else if (!valu_eqa.default_event().triggered())
            ev_valufifo_updated.notify();
    }
}

void BASE::VALU_CALC()
{
    valufifo_elem_num = 0;
    valufifo_empty = 1;
    valu_in_t valutmp1;
    valu_out_t valutmp2;
    bool succeed;
    while (true)
    {
        wait(valu_eva | valu_eqa.default_event());
        // cout << "valu_eqa.default_event triggered at " << sc_time_stamp() <<","<< sc_delta_count_at_current_time() << "\n";
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
            valufifo.push(valutmp2);
            break;
        case vaddvx_:
            valutmp2.ins = valutmp1.ins;
            for (int i = 0; i < num_thread; i++)
            {
                valutmp2.rdv1_data[i] = valutmp1.rsv1_data[i] + valutmp1.rss2_data;
            }
            valufifo.push(valutmp2);
            break;
        }
        ev_valufifo_updated.notify();
    }
}

void BASE::VALU_OUT()
{
    while (true)
    {
        wait();
        if (execpop_valu)
            valufifo.pop();
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
        if (emito_vfpu && jump == false)
        {
            if (vfpu_ready == false)
            {
                cout << "vfpu error: not ready at " << sc_time_stamp() << "," << sc_delta_count_at_current_time() << "\n";
            }
            vfpu_unready.notify();
            switch (opctop_ins.op)
            {
            case vfadd_:
                new_data.ins = opctop_ins;
                for (int i = 0; i < num_thread; i++)
                {
                    new_data.rsf1_data[i] = rsf1_data[i].read();
                    new_data.rsf2_data[i] = rsf2_data[i].read();
                }
                vfpu_dq.push(new_data);
                a_delay = 5;
                b_delay = 1;
                if (a_delay == 0)
                    vfpu_eva.notify();
                else if (vfpu_eqa.default_event().triggered())
                    vfpu_eqa.notify(sc_time((a_delay)*PERIOD, SC_NS));
                else
                {
                    vfpu_eqa.notify(sc_time((a_delay)*PERIOD, SC_NS));
                    ev_vfpufifo_updated.notify();
                }
                vfpu_eqb.notify(sc_time((b_delay)*PERIOD, SC_NS));
                break;
            default:
                cout << "vfpu error: receive wrong ins " << opctop_ins << " at " << sc_time_stamp() << "," << sc_delta_count_at_current_time() << "\n";
            }
        }
        else if (!vfpu_eqa.default_event().triggered())
            ev_vfpufifo_updated.notify();
    }
}

void BASE::VFPU_CALC()
{
    vfpufifo_elem_num = 0;
    vfpufifo_empty = true;
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
        case vfadd_:
            vfputmp2.ins = vfputmp1.ins;
            for (int i = 0; i < num_thread; i++)
            {
                vfputmp2.rdf1_data[i] = vfputmp1.rsf1_data[i] + vfputmp1.rsf2_data[i];
            }
            vfpufifo.push(vfputmp2);
            break;
        }

        ev_vfpufifo_updated.notify();
    }
}

void BASE::VFPU_OUT()
{
    while (true)
    {
        wait();
        if (execpop_vfpu)
            vfpufifo.pop();
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
        if (emito_lsu && jump == false)
        {
            if (lsu_ready == false)
            {
                cout << "lsu error: not ready at " << sc_time_stamp() << "," << sc_delta_count_at_current_time() << "\n";
            }
            lsu_unready.notify();
            switch (opctop_ins.op)
            {
            case lw_:
                new_data.ins = opctop_ins;
                new_data.rss1_data = rss1_data;
                lsu_dq.push(new_data);
                a_delay = 5;
                b_delay = 3;
                if (a_delay == 0)
                    lsu_eva.notify();
                else if (lsu_eqa.default_event().triggered())
                    lsu_eqa.notify(sc_time((a_delay)*PERIOD, SC_NS));
                else
                {
                    lsu_eqa.notify(sc_time((a_delay)*PERIOD, SC_NS));
                    ev_lsufifo_updated.notify();
                }
                lsu_eqb.notify(sc_time((b_delay)*PERIOD, SC_NS));
                break;
            case vload_:
                new_data.ins = opctop_ins;
                new_data.rss1_data = rss1_data;
                lsu_dq.push(new_data);
                a_delay = 6;
                b_delay = 3;
                if (a_delay == 0)
                    lsu_eva.notify();
                else if (lsu_eqa.default_event().triggered())
                    lsu_eqa.notify(sc_time((a_delay)*PERIOD, SC_NS));
                else
                {
                    lsu_eqa.notify(sc_time((a_delay)*PERIOD, SC_NS));
                    ev_lsufifo_updated.notify();
                }
                lsu_eqb.notify(sc_time((b_delay)*PERIOD, SC_NS));
                break;
            default:
                cout << "lsu error: receive wrong ins " << opctop_ins << " at " << sc_time_stamp() << "," << sc_delta_count_at_current_time() << "\n";
                break;
            }
        }
        else if (!lsu_eqa.default_event().triggered())
            ev_lsufifo_updated.notify();
    }
}

void BASE::LSU_CALC()
{
    lsufifo_elem_num = 0;
    lsufifo_empty = 1;
    lsu_in_t lsutmp1;
    lsu_out_t lsutmp2;
    bool succeed;
    while (true)
    {
        wait(lsu_eva | lsu_eqa.default_event());
        cout << "LSU_OUT: triggered by eva/eqa at " << sc_time_stamp() << "," << sc_delta_count_at_current_time() << "\n";
        lsutmp1 = lsu_dq.front();
        lsu_dq.pop();
        switch (lsutmp1.ins.op)
        {
        case lw_:
            lsutmp2.ins = lsutmp1.ins;
            lsutmp2.rds1_data = s_memory[lsutmp1.rss1_data + lsutmp1.ins.s2];
            lsufifo.push(lsutmp2);
            break;
        case vload_:
            lsutmp2.ins = lsutmp1.ins;
            for (int i = 0; i < num_thread; i++)
                lsutmp2.rdv1_data[i] = v_memory[lsutmp1.rss1_data + lsutmp1.ins.s2][i];
            lsufifo.push(lsutmp2);
            break;
        }
        ev_lsufifo_updated.notify();
    }
}

void BASE::LSU_OUT()
{
    while (true)
    {
        wait();
        if (execpop_lsu)
            lsufifo.pop();
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
    write_s = write_v = write_f = false;
    execpop_salu = execpop_valu = execpop_vfpu = execpop_lsu = false;

    while (true)
    {
        wait(ev_salufifo_updated & ev_valufifo_updated & ev_vfpufifo_updated &
             ev_lsufifo_updated);

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

        cout << "WriteBack start at " << sc_time_stamp() << "," << sc_delta_count_at_current_time() << "\n";
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
            for (int i = 0; i < num_thread; i++)
            {
                rdf1_data[i] = vfputop_dat.rdf1_data[i];
            }
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
                break;
            case vload_:
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
