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
    WARPS[warp_id].pc = 0x80000000;
    while (true)
    {
        wait(clk.posedge_event());
        // cout << "PC warp" << warp_id << " start at " << sc_time_stamp() << "," << sc_delta_count_at_current_time() << "\n";
        // wait(WARPS[warp_id].ev_ibuf_inout); // ibuf判断swallow后，fetch新指令
        // cout << "PC start, ibuf_swallow=" << ibuf_swallow << " at " << sc_time_stamp() << "," << sc_delta_count_at_current_time() << "\n";

        if (WARPS[warp_id].is_warp_activated)
        {
            if (rst_n == 0)
            {
                WARPS[warp_id].pc = 0x80000000 - 4;
                WARPS[warp_id].fetch_valid = false;
            }
            else if (WARPS[warp_id].jump == 1)
            {
                WARPS[warp_id].pc = WARPS[warp_id].jump_addr;
                WARPS[warp_id].fetch_valid = true;
                // cout << "pc jumps to addr " << jump_addr.read() << " at " << sc_time_stamp() <<","<< sc_delta_count_at_current_time() << "\n";
            }
            else if (WARPS[warp_id].simtstk_jump == 1)
            {
                WARPS[warp_id].pc = WARPS[warp_id].simtstk_jumpaddr;
                WARPS[warp_id].fetch_valid = true;
            }
            else if (WARPS[warp_id].ibuf_empty |
                     (!WARPS[warp_id].ibuf_full |
                      (WARPS[warp_id].dispatch_warp_valid && (!opc_full | doemit))))
            {
                // cout << "pc will +1 at " << sc_time_stamp() << "," << sc_delta_count_at_current_time() << "\n";
                WARPS[warp_id].pc = WARPS[warp_id].pc.read() + 4;
                WARPS[warp_id].fetch_valid = true;
            }
        }
        WARPS[warp_id].ev_fetchpc.notify();
    }
}

void BASE::INSTRUCTION_REG(int warp_id)
{
    // initialize
    // ireg[0] = I_TYPE(ADD_, 0, 1, 2);
    // ireg[1] = I_TYPE(ADD_, 1, 3, 3);
    // ireg[2] = I_TYPE(ADD_, 0, 4, 5);
    // ireg[3] = I_TYPE(BEQ_, 0, 7, 2);
    // ireg[4] = I_TYPE(VADD_VX_, 0, 1, 4);
    // ireg[5] = I_TYPE(VFADD_VV_, 3, 4, 2);
    // ireg[6] = I_TYPE(BEQ_, 0, 7, 5);

    while (true)
    {
        wait(clk.posedge_event());
        if (WARPS[warp_id].is_warp_activated)
        {
            if (WARPS[warp_id].jump == 1 |
                WARPS[warp_id].simtstk_jump == 1 |
                WARPS[warp_id].ibuf_empty |
                (!WARPS[warp_id].ibuf_full |
                 (WARPS[warp_id].dispatch_warp_valid && (!opc_full | doemit))))
            {
                WARPS[warp_id].fetch_valid2 = WARPS[warp_id].fetch_valid;
                if (inssrc == "ireg")
                    WARPS[warp_id].fetch_ins =
                        (WARPS[warp_id].pc.read() >= 0)
                            ? ireg[WARPS[warp_id].pc.read() / 4]
                            : I_TYPE(INVALID_, 0, 0, 0);
                else if (inssrc == "imem")
                {
                    WARPS[warp_id].fetch_ins =
                        (WARPS[warp_id].pc.read() >= 0x80000000)
                            ? I_TYPE(ins_mem[(WARPS[warp_id].pc.read() - 0x80000000) / 4])
                            : I_TYPE(INVALID_, 0, 0, 0);
                    // cout << "ICACHE: ins_mem[" << std::dec << WARPS[warp_id].pc.read() / 4 << "]=" << std::hex << ins_mem[WARPS[warp_id].pc.read() / 4] << ", fetch_ins.bit=" << WARPS[warp_id].fetch_ins.origin32bit << std::dec << "\n";
                }

                else
                    cout << "ICACHE error: unrecognized param inssrc=" << inssrc << "\n";
                WARPS[warp_id].ev_decode.notify();
            }

            // cout << "pc=" << pc << ", fetch_ins is " << fetch_ins << " at " << sc_time_stamp()
            //      << ", it will be " << ireg[pc.read()] << " at the next timestamp"
            //      << "\n";

            // wait(WARPS[warp_id].pc.value_changed_event());
            // cout << "pc.value_changed_event() triggered\n";
        }
    }
}

void BASE::DECODE(int warp_id)
{
    I_TYPE tmpins;
    sc_bv<32> scinsbit;
    while (true)
    {
        wait(WARPS[warp_id].ev_decode);
        tmpins = I_TYPE(WARPS[warp_id].fetch_ins, WARPS[warp_id].pc.read());
        if (inssrc == "imem")
        {
            bool foundBitIns = 0;
            for (const auto &instable_item : instable_vec)
            {
                std::bitset<32> masked_ins = std::bitset<32>(tmpins.origin32bit) & instable_item.mask;
                // cout << "warp" << warp_id << " DECODE: mask=" << instable_item.mask << ", masked_ins=" << masked_ins << " at " << sc_time_stamp() << "," << sc_delta_count_at_current_time() << "\n";
                auto it = instable_item.itable.find(masked_ins);
                if (it != instable_item.itable.end())
                {
                    tmpins.op = it->second;
                    foundBitIns = true;
                    break;
                }
            }
            if (!foundBitIns)
            {
                tmpins.op = INVALID_;
                cout << "warp" << warp_id << " DECODE error: invalid bit ins " << std::bitset<32>(tmpins.origin32bit) << " at " << sc_time_stamp() << "," << sc_delta_count_at_current_time() << "\n";
            }
            else
            {
                // cout << "warp" << warp_id << " DECODE: match ins bit=" << std::bitset<32>(tmpins.origin32bit) << " with " << magic_enum::enum_name((OP_TYPE)tmpins.op) << " at " << sc_time_stamp() << "," << sc_delta_count_at_current_time() << "\n";
            }
        }
        tmpins.ddd = decode_table[(OP_TYPE)tmpins.op];
        // tmpins.ddd.mem = (tmpins.ddd.mem_cmd & 1) | ((tmpins.ddd.mem_cmd) >> 1 & 1);
        if (WARPS[warp_id].fetch_ins.ddd.tc)
            tmpins.ddd.sel_execunit = DecodeParams::TC;
        else if (WARPS[warp_id].fetch_ins.ddd.sfu)
            tmpins.ddd.sel_execunit = DecodeParams::SFU;
        else if (WARPS[warp_id].fetch_ins.ddd.fp)
            tmpins.ddd.sel_execunit = DecodeParams::VFPU;
        else if (WARPS[warp_id].fetch_ins.ddd.csr != 0)
            tmpins.ddd.sel_execunit = DecodeParams::CSR;
        else if (WARPS[warp_id].fetch_ins.ddd.mul)
            tmpins.ddd.sel_execunit = DecodeParams::MUL;
        else if (WARPS[warp_id].fetch_ins.ddd.mem_cmd != 0)
            tmpins.ddd.sel_execunit = DecodeParams::LSU;
        else if (WARPS[warp_id].fetch_ins.ddd.isvec)
        {
            if (WARPS[warp_id].fetch_ins.op == JOIN_)
                tmpins.ddd.sel_execunit = DecodeParams::SIMTSTK;
            else
                tmpins.ddd.sel_execunit = DecodeParams::VALU;
        }
        else if (WARPS[warp_id].fetch_ins.ddd.barrier)
            tmpins.ddd.sel_execunit = DecodeParams::WPSCHEDLER;
        else
            tmpins.ddd.sel_execunit = DecodeParams::SALU;

        tmpins.s1 = extractBits32(tmpins.origin32bit, 19, 15);
        tmpins.s2 = extractBits32(tmpins.origin32bit, 24, 20);
        tmpins.s3 = (tmpins.ddd.fp & !tmpins.ddd.isvec)
                        ? extractBits32(tmpins.origin32bit, 31, 27)
                        : extractBits32(tmpins.origin32bit, 11, 7);
        tmpins.d = extractBits32(tmpins.origin32bit, 11, 7);

        scinsbit = tmpins.origin32bit;
        switch (tmpins.ddd.sel_imm)
        {
        case DecodeParams::IMM_I:
            tmpins.imm = scinsbit.range(31, 20).to_int(); // to_int()会自动补符号位，to_uint()补0
            break;
        case DecodeParams::IMM_S:
            tmpins.imm = (scinsbit.range(31, 25), scinsbit.range(11, 7)).to_int();
            break;
        case DecodeParams::IMM_B:
            tmpins.imm = (scinsbit.range(31, 31), scinsbit.range(7, 7), scinsbit.range(30, 25), scinsbit.range(11, 8)).to_int() << 1;
            break;
        case DecodeParams::IMM_U:
            tmpins.imm = (scinsbit.range(31, 12)).to_int() << 12;
            break;
        case DecodeParams::IMM_J:
            tmpins.imm = (scinsbit.range(31, 31), scinsbit.range(19, 12), scinsbit.range(20, 20), scinsbit.range(30, 21)).to_int() << 1;
            break;
        case DecodeParams::IMM_Z:
            tmpins.imm = (scinsbit.range(19, 15)).to_uint();
            break;
        case DecodeParams::IMM_2:
            tmpins.imm = (scinsbit.range(24, 20)).to_int();
            break;
        case DecodeParams::IMM_V: // 和scala不一样，需要修改，加位拓展
            tmpins.imm = (scinsbit.range(19, 15)).to_int();
            break;
        case DecodeParams::IMM_L11:
            tmpins.imm = (scinsbit.range(30, 20)).to_int();
            break;
        case DecodeParams::IMM_S11:
            tmpins.imm = (scinsbit.range(30, 25), scinsbit.range(11, 7)).to_int();
            break;
        default:
            break;
        }

        WARPS[warp_id].decode_ins = tmpins;
    }
}

void BASE::IBUF_ACTION(int warp_id)
{
    I_TYPE dispatch_ins_;
    I_TYPE _readdata3;
    while (true)
    {
        wait(clk.posedge_event());
        // cout << "IBUF_ACTION warp" << warp_id << ": start at " << sc_time_stamp() << "," << sc_delta_count_at_current_time() << "\n";
        if (WARPS[warp_id].is_warp_activated)
        {
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
                if (WARPS[warp_id].fetch_valid2 && WARPS[warp_id].jump == false)
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
                else if (WARPS[warp_id].jump || WARPS[warp_id].simtstk_jump)
                {
                    // cout << "ibuf detected jump at " << sc_time_stamp() <<","<< sc_delta_count_at_current_time() << "\n";
                    WARPS[warp_id].ififo.clear();
                }
            }
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
}

void BASE::UPDATE_SCORE(int warp_id)
{
    I_TYPE tmpins;
    std::set<SCORE_TYPE>::iterator it;
    REG_TYPE regtype_;
    bool insertscore = false;
    while (true)
    {
        wait(clk.posedge_event());
        if (WARPS[warp_id].is_warp_activated)
        {
            if (wb_ena && wb_warpid == warp_id)
            { // 写回阶段，删除score
                tmpins = wb_ins;
                // cout << "scoreboard: wb_ins is " << tmpins << " at " << sc_time_stamp() <<","<< sc_delta_count_at_current_time() << "\n";
                if (tmpins.ddd.wvd)
                {
                    if (tmpins.ddd.wxd)
                        cout << "Scoreboard warp" << warp_id << " error: wb_ins wvd=wxd=1 at the same time at " << sc_time_stamp() << "," << sc_delta_count_at_current_time() << "\n";
                    regtype_ = v;
                }
                else if (tmpins.ddd.wxd)
                    regtype_ = s;
                else
                    cout << "Scoreboard warp" << warp_id << " error: wb_ins wvd=wxd=0 at the same time at " << sc_time_stamp() << "," << sc_delta_count_at_current_time() << "\n";
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
            // dispatch阶段，写入score
            tmpins = WARPS[warp_id].ibuftop_ins; // this ibuftop_ins is the old data
            if (WARPS[warp_id].branch_sig || WARPS[warp_id].vbran_sig)
            {
                if (WARPS[warp_id].wait_bran == 0)
                    cout << "warp" << warp_id << "_scoreboard error: detect (v)branch_sig=1(from salu) while wait_bran=0 at " << sc_time_stamp() << "," << sc_delta_count_at_current_time() << "\n";
                else if (WARPS[warp_id].dispatch_warp_valid && (!opc_full | doemit))
                    cout << "warp" << warp_id << "_scoreboard error: detect (v)branch_sig=1(from salu) while dispatch=1 at " << sc_time_stamp() << "," << sc_delta_count_at_current_time() << "\n";
                WARPS[warp_id].wait_bran = 0;
            }
            // else if ((tmpins.op == BNE_ ||
            //           tmpins.op == BEQ_ ||
            //           tmpins.op == BLT_ ||
            //           tmpins.op == BLTU_ ||
            //           tmpins.op == BGE_ ||
            //           tmpins.op == BGEU_ ||
            //           tmpins.op == JAL_ ||
            //           tmpins.op == JALR_ ||
            //           tmpins.op == VBEQ_ ||
            //           tmpins.op == VBNE_ ||
            //           tmpins.op == VBLT_ ||
            //           tmpins.op == VBGE_ ||
            //           tmpins.op == VBLTU_ ||
            //           tmpins.op == VBGEU_ ||
            //           tmpins.op == JOIN_) &&
            //          WARPS[warp_id].dispatch_warp_valid && (!opc_full | doemit))
            else if ((tmpins.ddd.branch != 0) &&
                     WARPS[warp_id].dispatch_warp_valid && (!opc_full | doemit))
            {
                // cout << "ibuf let wait_bran=1 at " << sc_time_stamp() <<","<< sc_delta_count_at_current_time() << "\n";
                WARPS[warp_id].wait_bran = 1;
            }
            if (WARPS[warp_id].dispatch_warp_valid && (!opc_full | doemit))
            { // 加入 score
                insertscore = true;
                if (tmpins.ddd.wvd)
                {
                    if (tmpins.ddd.wxd)
                        cout << "Scoreboard warp" << warp_id << " error: dispatch_ins wvd=wxd=1 at the same time at " << sc_time_stamp() << "," << sc_delta_count_at_current_time() << "\n";
                    regtype_ = v;
                }
                else if (tmpins.ddd.wxd)
                    regtype_ = s;
                else
                    insertscore = false;
                if (insertscore)
                    WARPS[warp_id].score.insert(SCORE_TYPE(regtype_, tmpins.d));
                // cout << "warp" << warp_id << "_scoreboard: insert " << SCORE_TYPE(regtype_, tmpins.d)
                //      << " because of dispatch " << tmpins << " at " << sc_time_stamp() << "," << sc_delta_count_at_current_time() << "\n";
            }
            WARPS[warp_id].ev_judge_dispatch.notify();
        }
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
            WARPS[warp_id].can_dispatch = true;

            if (_readibuf.op == INVALID_)
                WARPS[warp_id].can_dispatch = false;

            if (_readibuf.ddd.wxd && WARPS[warp_id].score.find(SCORE_TYPE(s, _readibuf.d)) != WARPS[warp_id].score.end())
                WARPS[warp_id].can_dispatch = false;
            else if (_readibuf.ddd.wvd && WARPS[warp_id].score.find(SCORE_TYPE(v, _readibuf.d)) != WARPS[warp_id].score.end())
                WARPS[warp_id].can_dispatch = false;
            else if (_readibuf.ddd.sel_alu1 == DecodeParams::A1_RS1 && WARPS[warp_id].score.find(SCORE_TYPE(s, _readibuf.s1)) != WARPS[warp_id].score.end())
                WARPS[warp_id].can_dispatch = false;
            else if (_readibuf.ddd.sel_alu1 == DecodeParams::A1_VRS1 && WARPS[warp_id].score.find(SCORE_TYPE(v, _readibuf.s1)) != WARPS[warp_id].score.end())
                WARPS[warp_id].can_dispatch = false;
            else if (_readibuf.ddd.sel_alu2 == DecodeParams::A2_RS2 && WARPS[warp_id].score.find(SCORE_TYPE(s, _readibuf.s2)) != WARPS[warp_id].score.end())
                WARPS[warp_id].can_dispatch = false;
            else if (_readibuf.ddd.sel_alu2 == DecodeParams::A2_VRS2 && WARPS[warp_id].score.find(SCORE_TYPE(v, _readibuf.s2)) != WARPS[warp_id].score.end())
                WARPS[warp_id].can_dispatch = false;
            else if (_readibuf.ddd.sel_alu3 == DecodeParams::A3_FRS3 && WARPS[warp_id].score.find(SCORE_TYPE(s, _readibuf.s3)) != WARPS[warp_id].score.end())
                WARPS[warp_id].can_dispatch = false;
            else if (_readibuf.ddd.sel_alu3 == DecodeParams::A3_VRS3 && WARPS[warp_id].score.find(SCORE_TYPE(v, _readibuf.d)) != WARPS[warp_id].score.end())
                WARPS[warp_id].can_dispatch = false;
            // switch (_readibuf.op)
            // {
            // case JAL_:
            // case AUIPC_:
            // case LUI_:
            //     if (WARPS[warp_id].score.find(SCORE_TYPE(s, _readibuf.d)) == WARPS[warp_id].score.end())
            //         WARPS[warp_id].can_dispatch = true;
            //     else
            //         WARPS[warp_id].can_dispatch = false;
            //     break;
            // case JALR_:
            // case LW_:
            // case ADDI_:
            // case SLTI_:
            // case SLTIU_:
            // case ANDI_:
            // case ORI_:
            // case XORI_:
            // case SLLI_:
            // case SRLI_:
            // case SRAI_:
            //     if (WARPS[warp_id].score.find(SCORE_TYPE(s, _readibuf.s1)) == WARPS[warp_id].score.end() &&
            //         WARPS[warp_id].score.find(SCORE_TYPE(s, _readibuf.d)) == WARPS[warp_id].score.end())
            //         WARPS[warp_id].can_dispatch = true;
            //     else
            //         WARPS[warp_id].can_dispatch = false;
            //     break;
            // case ADD_:
            // case SLT_:
            // case SLTU_:
            // case AND_:
            // case OR_:
            // case XOR_:
            // case SUB_:
            // case SLL_:
            // case SRL_:
            // case SRA_:
            // case MUL_:
            // case MULH_:
            // case MULHSU_:
            // case MULHU_:
            // case DIV_:
            // case DIVU_:
            // case REM_:
            // case REMU_:
            //     // cout << "JUDGE_DISPATCH switch to ADD_ case at " << sc_time_stamp() <<","<< sc_delta_count_at_current_time() << "\n";
            //     if (WARPS[warp_id].score.find(SCORE_TYPE(s, _readibuf.s1)) == WARPS[warp_id].score.end() &&
            //         WARPS[warp_id].score.find(SCORE_TYPE(s, _readibuf.s2)) == WARPS[warp_id].score.end() &&
            //         WARPS[warp_id].score.find(SCORE_TYPE(s, _readibuf.d)) == WARPS[warp_id].score.end())
            //         WARPS[warp_id].can_dispatch = true;
            //     else
            //         WARPS[warp_id].can_dispatch = false;
            //     break;
            // case BNE_:
            // case BEQ_:
            // case BLT_:
            // case BLTU_:
            // case BGE_:
            // case BGEU_:
            // case SW_:
            //     if (WARPS[warp_id].score.find(SCORE_TYPE(s, _readibuf.s1)) == WARPS[warp_id].score.end() &&
            //         WARPS[warp_id].score.find(SCORE_TYPE(s, _readibuf.s2)) == WARPS[warp_id].score.end())
            //         WARPS[warp_id].can_dispatch = true;
            //     else
            //         WARPS[warp_id].can_dispatch = false;
            //     break;
            // case VBEQ_:
            // case VBNE_:
            // case VBLT_:
            // case VBGE_:
            // case VBLTU_:
            // case VBGEU_:
            //     if (WARPS[warp_id].score.find(SCORE_TYPE(v, _readibuf.s1)) == WARPS[warp_id].score.end() &&
            //         WARPS[warp_id].score.find(SCORE_TYPE(v, _readibuf.s2)) == WARPS[warp_id].score.end())
            //         WARPS[warp_id].can_dispatch = true;
            //     else
            //         WARPS[warp_id].can_dispatch = false;
            //     break;
            // case JOIN_:
            //     WARPS[warp_id].can_dispatch = true;
            //     break;
            // case FSQRT_S_:
            // case VLE32_V_:
            //     if (WARPS[warp_id].score.find(SCORE_TYPE(v, _readibuf.s1)) == WARPS[warp_id].score.end() &&
            //         WARPS[warp_id].score.find(SCORE_TYPE(v, _readibuf.d)) == WARPS[warp_id].score.end())
            //         WARPS[warp_id].can_dispatch = true;
            //     else
            //         WARPS[warp_id].can_dispatch = false;
            //     break;
            // case FCVT_W_S_:
            // case FCVT_WU_S_:
            // case FCLASS_S_:
            //     if (WARPS[warp_id].score.find(SCORE_TYPE(v, _readibuf.s1)) == WARPS[warp_id].score.end() &&
            //         WARPS[warp_id].score.find(SCORE_TYPE(s, _readibuf.d)) == WARPS[warp_id].score.end())
            //         WARPS[warp_id].can_dispatch = true;
            //     else
            //         WARPS[warp_id].can_dispatch = false;
            //     break;
            // case FCVT_S_W_:
            // case FCVT_S_WU_:
            //     if (WARPS[warp_id].score.find(SCORE_TYPE(s, _readibuf.s1)) == WARPS[warp_id].score.end() &&
            //         WARPS[warp_id].score.find(SCORE_TYPE(v, _readibuf.d)) == WARPS[warp_id].score.end())
            //         WARPS[warp_id].can_dispatch = true;
            //     else
            //         WARPS[warp_id].can_dispatch = false;
            //     break;
            // case FMADD_S_:
            // case FMSUB_S_:
            // case FNMSUB_S_:
            // case FNMADD_S_:
            //     if (WARPS[warp_id].score.find(SCORE_TYPE(v, _readibuf.s1)) == WARPS[warp_id].score.end() &&
            //         WARPS[warp_id].score.find(SCORE_TYPE(v, _readibuf.s2)) == WARPS[warp_id].score.end() &&
            //         WARPS[warp_id].score.find(SCORE_TYPE(v, _readibuf.s3)) == WARPS[warp_id].score.end() &&
            //         WARPS[warp_id].score.find(SCORE_TYPE(v, _readibuf.d)) == WARPS[warp_id].score.end())
            //         WARPS[warp_id].can_dispatch = true;
            //     else
            //         WARPS[warp_id].can_dispatch = false;
            //     break;
            // case FADD_S_:
            // case FSUB_S_:
            // case FMUL_S_:
            // case FDIV_S_:
            // case FSGNJ_S_:
            // case FSGNJN_S_:
            // case FSGNJX_S_:
            // case FMIN_S_:
            // case FMAX_S_:

            // case VFMUL_VV_:

            // case VADD_VV_:
            // case VFADD_VV_:
            //     // cout << "JUDGE_DISPATCH switch to VADD_VV_ case at " << sc_time_stamp() <<","<< sc_delta_count_at_current_time() << "\n";
            //     if (WARPS[warp_id].score.find(SCORE_TYPE(v, _readibuf.s1)) == WARPS[warp_id].score.end() &&
            //         WARPS[warp_id].score.find(SCORE_TYPE(v, _readibuf.s2)) == WARPS[warp_id].score.end() &&
            //         WARPS[warp_id].score.find(SCORE_TYPE(v, _readibuf.d)) == WARPS[warp_id].score.end())
            //         WARPS[warp_id].can_dispatch = true;
            //     else
            //         WARPS[warp_id].can_dispatch = false;
            //     break;

            // case FEQ_S_:
            // case FLT_S_:
            // case FLE_S_:
            //     if (WARPS[warp_id].score.find(SCORE_TYPE(v, _readibuf.s1)) == WARPS[warp_id].score.end() &&
            //         WARPS[warp_id].score.find(SCORE_TYPE(v, _readibuf.s2)) == WARPS[warp_id].score.end() &&
            //         WARPS[warp_id].score.find(SCORE_TYPE(s, _readibuf.d)) == WARPS[warp_id].score.end())
            //         WARPS[warp_id].can_dispatch = true;
            //     else
            //         WARPS[warp_id].can_dispatch = false;
            //     break;
            // case VADD_VX_:
            //     // cout << "JUDGE_DISPATCH switch to VADD_VX_ case at " << sc_time_stamp() <<","<< sc_delta_count_at_current_time() << "\n";
            //     if (WARPS[warp_id].score.find(SCORE_TYPE(s, _readibuf.s1)) == WARPS[warp_id].score.end() &&
            //         WARPS[warp_id].score.find(SCORE_TYPE(v, _readibuf.s2)) == WARPS[warp_id].score.end() &&
            //         WARPS[warp_id].score.find(SCORE_TYPE(v, _readibuf.d)) == WARPS[warp_id].score.end())
            //         WARPS[warp_id].can_dispatch = true;
            //     else
            //     {
            //         WARPS[warp_id].can_dispatch = false;
            //         // cout << "warp" << warp_id << ": JUDGE_DISPATCH don't dispatch VADD_VX_, ins is " << _readibuf << ", at " << sc_time_stamp() << "," << sc_delta_count_at_current_time() << "\n";
            //     }
            //     break;
            // default:
            //     WARPS[warp_id].can_dispatch = false;
            //     // cout << "JUDGE_DISPATCH switch to default case at " << sc_time_stamp() <<","<< sc_delta_count_at_current_time() << "\n";
            //     break;
            // }
        }
        WARPS[warp_id].ev_issue.notify();
    }
}

void BASE::ISSUE_ACTION()
{
    bool find_dispatchwarp = 0;
    last_dispatch_warpid = 0;
    I_TYPE _newissueins;
    while (true)
    {
        wait(ev_issue_list);
        // cout << "ISSUE start at " << sc_time_stamp() << "," << sc_delta_count_at_current_time() << "\n";
        if (!opc_full | doemit) // 这是dispatch_ready (ready-valid机制)
        {
            find_dispatchwarp = 0;
            for (int i = last_dispatch_warpid; i < last_dispatch_warpid + num_warp_activated; i++)
            {
                if (!find_dispatchwarp && WARPS[i % num_warp_activated].can_dispatch)
                {
                    // cout << "ISSUE: opc_full=" << opc_full << " at " << sc_time_stamp() << "," << sc_delta_count_at_current_time() << "\n";
                    WARPS[i % num_warp_activated].dispatch_warp_valid = true;
                    dispatch_valid = true;
                    _newissueins = WARPS[i % num_warp_activated].ififo.front();
                    _newissueins.mask = WARPS[i % num_warp_activated].current_mask;
                    // cout << "let issue_ins mask=" << _newissueins.mask << " at " << sc_time_stamp() << "," << sc_delta_count_at_current_time() << "\n";
                    issue_ins = _newissueins;
                    issueins_warpid = i % num_warp_activated;
                    find_dispatchwarp = true;
                    last_dispatch_warpid = i % num_warp_activated + 1;
                }
                else
                {
                    WARPS[i % num_warp_activated].dispatch_warp_valid = false;
                    // cout << "ISSUE: let warp" << i % num_warp_activated << " dispatch_warp_valid=false at " << sc_time_stamp() << "," << sc_delta_count_at_current_time() << "\n";
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
    std::array<bool, 3> in_ready;
    std::array<bool, 3> in_valid;
    std::array<bank_t, 3> in_srcaddr;
    std::array<bool, 3> in_banktype;
    opcfifo_t newopcdat;
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
                in_ready = {1, 1, 1};
                in_valid = {0, 0, 0}; // 要取操作数，则ready=0，valid=1

                if (_readdata4.ddd.sel_alu1 == DecodeParams::A1_RS1)
                {
                    in_ready[0] = 0;
                    in_valid[0] = 1;
                    in_srcaddr[0] = bank_decode(_readwarpid, _readdata4.s1);
                    in_banktype[0] = 0; // 0为scalar，1为vector
                }
                else if (_readdata4.ddd.sel_alu1 == DecodeParams::A1_VRS1)
                {
                    in_ready[0] = 0;
                    in_valid[0] = 1;
                    in_srcaddr[0] = bank_decode(_readwarpid, _readdata4.s1);
                    in_banktype[0] = 1;
                }
                else if (_readdata4.ddd.sel_alu1 == DecodeParams::A1_IMM)
                {
                    newopcdat.data[0].fill(_readdata4.imm);
                }
                else if (_readdata4.ddd.sel_alu1 == DecodeParams::A1_PC)
                {
                    newopcdat.data[0].fill(_readdata4.currentpc);
                }

                if (_readdata4.ddd.sel_alu2 == DecodeParams::A2_RS2)
                {
                    in_ready[1] = 0;
                    in_valid[1] = 1;
                    in_srcaddr[1] = bank_decode(_readwarpid, _readdata4.s2);
                    in_banktype[1] = 0;
                }
                else if (_readdata4.ddd.sel_alu2 == DecodeParams::A2_VRS2)
                {
                    in_ready[1] = 0;
                    in_valid[1] = 1;
                    in_srcaddr[1] = bank_decode(_readwarpid, _readdata4.s2);
                    in_banktype[1] = 1;
                }
                else if (_readdata4.ddd.sel_alu2 == DecodeParams::A2_IMM)
                {
                    newopcdat.data[1].fill(_readdata4.imm);
                }
                else if (_readdata4.ddd.sel_alu2 == DecodeParams::A2_SIZE)
                {
                    newopcdat.data[1].fill(4);
                }

                if (_readdata4.ddd.sel_alu3 == DecodeParams::A3_FRS3)
                {
                    in_ready[2] = 0;
                    in_valid[2] = 1;
                    in_srcaddr[2] = bank_decode(_readwarpid, _readdata4.s3);
                    in_banktype[2] = 0;
                }
                else if (_readdata4.ddd.sel_alu3 == DecodeParams::A3_VRS3)
                {
                    in_ready[2] = 0;
                    in_valid[2] = 1;
                    in_srcaddr[2] = bank_decode(_readwarpid, _readdata4.s3);
                    in_banktype[2] = 1;
                }
                else if (_readdata4.ddd.sel_alu3 == DecodeParams::A3_PC && _readdata4.ddd.branch != DecodeParams::B_R)
                {
                    newopcdat.data[2].fill(_readdata4.imm + _readdata4.currentpc);
                }
                else if (_readdata4.ddd.sel_alu3 == DecodeParams::A3_PC)
                {
                    in_ready[2] = 0;
                    in_valid[2] = 1;
                    in_srcaddr[2] = bank_decode(_readwarpid, _readdata4.s1);
                    in_banktype[2] = 0;
                }
                else if (_readdata4.ddd.sel_alu3 == DecodeParams::A3_SD)
                {
                    in_ready[2] = 0;
                    in_valid[2] = 1;
                    in_srcaddr[2] = bank_decode(_readwarpid, (_readdata4.ddd.isvec & (!_readdata4.ddd.readmask)) ? _readdata4.s3 : _readdata4.s2);
                    in_banktype[2] = _readdata4.ddd.isvec ? 1 : 0;
                }

                // cout << "opcfifo push issue_ins " << _readdata4 << "warp" << _readwarpid
                //      << ", srcaddr=(" << in_srcaddr[0] << ";" << in_srcaddr[1] << ")"
                //      << " at " << sc_time_stamp() << "," << sc_delta_count_at_current_time() << "\n";

                newopcdat.ins = _readdata4;
                newopcdat.warp_id = _readwarpid;
                newopcdat.ready = in_ready;
                newopcdat.valid = in_valid;
                newopcdat.srcaddr = in_srcaddr;
                newopcdat.banktype = in_banktype;

                // opcfifo.push(opcfifo_t(_readdata4, _readwarpid,
                //                        in_ready, in_valid, in_srcaddr, in_banktype));
                opcfifo.push(newopcdat);
            }
        }
        opcfifo_elem_num = opcfifo.get_size();
        opc_full = opcfifo.get_size() == OPCFIFO_SIZE;
        opc_empty = opcfifo.get_size() == 0;
        // cout << "OPC_FIFO waiting ev_opc_judge_emit at " << sc_time_stamp() << "," << sc_delta_count_at_current_time() << "\n";
        wait(ev_opc_judge_emit & ev_regfile_readdata);
        // cout << "OPC_FIFO get ev_opc_judge_emit at " << sc_time_stamp() << "," << sc_delta_count_at_current_time() << "\n";

        //  由ready写入entry，不能影响当前cycle判断emit
        for (int i = 0; i < OPCFIFO_SIZE; i++)
        {
            for (int j = 0; j < 3; j++)
                if (opc_ready[i][j] == true)
                {
                    if (opcfifo[i].valid[j] == false)
                        cout << "opc collect error[" << i << "," << j << "]: ins " << magic_enum::enum_name((OP_TYPE)opcfifo[i].ins.op) << " ready=1 but valid=0 at " << sc_time_stamp() << "," << sc_delta_count_at_current_time() << "\n";
                    opcfifo[i].ready[j] = true;
                    opcfifo[i].valid[j] = false;
                    opcfifo[i].data[j] = read_data[opcfifo[i].srcaddr[j].bank_id];
                    printdata_ = read_data[opcfifo[i].srcaddr[j].bank_id];
                    // cout << "OPC_FIFO: store_in[" << i << "," << j << "], ins=" << magic_enum::enum_name((OP_TYPE)opcfifo[i].ins.op) << "warp" << opcfifo[i].warp_id
                    //      << ", data[" << j << "]=" << printdata_ << ", srcaddr=" << opcfifo[i].srcaddr[j] << " at " << sc_time_stamp() << "," << sc_delta_count_at_current_time() << "\n";
                }
        }
        ev_opc_store.notify();
        // cout << "OPC: entry[0]: ins=" << magic_enum::enum_name((OP_TYPE)opcfifo[0].ins.op) << ", tag_valid="<<opcfifo.tag_valid(0)<<", valid={" << opcfifo[0].valid[0] << "," << opcfifo[0].valid[1] << "," << opcfifo[0].valid[2] << "}, ready={" << opcfifo[0].ready[0] << "," << opcfifo[0].ready[1] << "," << opcfifo[0].ready[2] << "}\n";
        // cout << "     entry[1]: ins=" << magic_enum::enum_name((OP_TYPE)opcfifo[1].ins.op) << ", tag_valid="<<opcfifo.tag_valid(1)<<", valid={" << opcfifo[1].valid[0] << "," << opcfifo[1].valid[1] << "," << opcfifo[1].valid[2] << "}, ready={" << opcfifo[1].ready[0] << "," << opcfifo[1].ready[1] << "," << opcfifo[1].ready[2] << "}\n";
        // cout << "     entry[2]: ins=" << magic_enum::enum_name((OP_TYPE)opcfifo[2].ins.op) << ", tag_valid="<<opcfifo.tag_valid(2)<<", valid={" << opcfifo[2].valid[0] << "," << opcfifo[2].valid[1] << "," << opcfifo[2].valid[2] << "}, ready={" << opcfifo[2].ready[0] << "," << opcfifo[2].ready[1] << "," << opcfifo[2].ready[2] << "}\n";
        // cout << "     entry[3]: ins=" << magic_enum::enum_name((OP_TYPE)opcfifo[3].ins.op) << ", tag_valid="<<opcfifo.tag_valid(3)<<", valid={" << opcfifo[3].valid[0] << "," << opcfifo[3].valid[1] << "," << opcfifo[3].valid[2] << "}, ready={" << opcfifo[3].ready[0] << "," << opcfifo[3].ready[1] << "," << opcfifo[3].ready[2] << "} at " << sc_time_stamp() << "," << sc_delta_count_at_current_time() << "\n";
    }
}

void BASE::OPC_EMIT()
{
    reg_t pa1;
    reg_t *pa2; // 用于int转float
    float *pf1, *pf2;
    // FloatAndInt pr1, pr2;
    int p20;
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
        emito_simtstk = false;
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
                switch (opcfifo[entryidx].ins.ddd.sel_execunit)
                {
                case DecodeParams::SALU:
                    if (salu_ready)
                    {
                        emit_idx = entryidx;
                        last_emit_entryid = entryidx + 1;
                        findemit = 1;
                        doemit = true;
                        // cout << "OPC: salu is ready at " << sc_time_stamp() <<","<< sc_delta_count_at_current_time() << "\n";
                        emito_salu = true;
                        tosalu_data1 = opcfifo[entryidx].data[0][0];
                        tosalu_data2 = opcfifo[entryidx].data[1][0];
                        tosalu_data3 = opcfifo[entryidx].data[2][0];
                    }
                    else
                    {
                        // cout << "OPC_EMIT: find all_ready ins " << opcfifo[entryidx].ins << "warp" << opcfifo[entryidx].warp_id << " but salu not ready at " << sc_time_stamp() << "," << sc_delta_count_at_current_time() << "\n";
                    }
                    break;

                case DecodeParams::VALU:
                    if (valu_ready)
                    {
                        emit_idx = entryidx;
                        last_emit_entryid = entryidx + 1;
                        findemit = 1;
                        doemit = true;
                        emito_valu = true;

                        for (int j = 0; j < num_thread; j++)
                        {
                            tovalu_data1[j] = opcfifo[entryidx].data[0][j];
                            tovalu_data2[j] = opcfifo[entryidx].data[1][j];
                        }

                        // if (opcfifo[entryidx].ins.ddd.sel_alu2 == DecodeParams::A2_VRS2)
                        //     for (int j = 0; j < num_thread; j++)
                        //         tovalu_data2[j] = opcfifo[entryidx].data[1][j];
                        // else if (opcfifo[entryidx].ins.ddd.sel_alu2 == DecodeParams::A2_RS2 |
                        //          opcfifo[entryidx].ins.ddd.sel_alu2 == DecodeParams::A2_IMM)
                        //     tovalu_data2[0] = opcfifo[entryidx].data[1][0];
                    }
                    break;

                case DecodeParams::SIMTSTK:
                    emit_idx = entryidx;
                    last_emit_entryid = entryidx + 1;
                    findemit = 1;
                    doemit = true;
                    emito_simtstk = true;
                    break;

                case DecodeParams::VFPU:
                    if (vfpu_ready)
                    {
                        emit_idx = entryidx;
                        last_emit_entryid = entryidx + 1;
                        findemit = 1;
                        doemit = true;
                        emito_vfpu = true;
                        if (opcfifo[entryidx].ins.ddd.sel_alu1 == DecodeParams::A1_VRS1)
                            for (int j = 0; j < num_thread; j++)
                                tovfpu_data1[j] = opcfifo[entryidx].data[0][j];
                        else if (opcfifo[entryidx].ins.ddd.sel_alu1 == DecodeParams::A1_RS1)
                            tovfpu_data1[0] = opcfifo[entryidx].data[0][0];
                        if (opcfifo[entryidx].ins.ddd.sel_alu2 == DecodeParams::A2_VRS2)
                            for (int j = 0; j < num_thread; j++)
                                tovfpu_data2[j] = opcfifo[entryidx].data[1][j];
                        else if (opcfifo[entryidx].ins.ddd.sel_alu2 == DecodeParams::A2_RS2)
                            tovfpu_data2[0] = opcfifo[entryidx].data[1][0];
                        if (opcfifo[entryidx].ins.ddd.sel_alu3 == DecodeParams::A3_VRS3)
                            for (int j = 0; j < num_thread; j++)
                                tovfpu_data3[j] = opcfifo[entryidx].data[2][j];
                        else if (opcfifo[entryidx].ins.ddd.sel_alu3 == DecodeParams::A3_FRS3)
                            tovfpu_data3[0] = opcfifo[entryidx].data[2][0];
                    }
                    break;

                case DecodeParams::LSU:
                    if (lsu_ready)
                    {
                        emit_idx = entryidx;
                        last_emit_entryid = entryidx + 1;
                        findemit = 1;
                        doemit = true;
                        emito_lsu = true;
                        if (opcfifo[entryidx].ins.ddd.sel_alu1 == DecodeParams::A1_VRS1)
                            for (int j = 0; j < num_thread; j++)
                                tolsu_data1[j] = opcfifo[entryidx].data[0][j];
                        else if (opcfifo[entryidx].ins.ddd.sel_alu1 == DecodeParams::A1_RS1)
                            tolsu_data1[0] = opcfifo[entryidx].data[0][0];
                        if (opcfifo[entryidx].ins.ddd.sel_alu2 == DecodeParams::A2_VRS2)
                            for (int j = 0; j < num_thread; j++)
                                tolsu_data2[j] = opcfifo[entryidx].data[1][j];
                        else if (opcfifo[entryidx].ins.ddd.sel_alu2 == DecodeParams::A2_RS2)
                            tolsu_data2[0] = opcfifo[entryidx].data[1][0];
                    }
                    break;
                case DecodeParams::INVALID_EXECUNIT:
                    cout << "OPC_EMIT error: ins=" << opcfifo[entryidx].ins << "but INVALID EXECUNIT at " << sc_time_stamp() << "," << sc_delta_count_at_current_time() << "\n";
                    break;
                default:
                    cout << "OPC_EMIT warning: ins=" << opcfifo[entryidx].ins << "but undefined EXECUNIT at " << sc_time_stamp() << "," << sc_delta_count_at_current_time() << "\n";
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
    const std::array<std::array<bank_t, 3>, OPCFIFO_SIZE> &addr_arr, // opc_srcaddr
    const std::array<std::array<bool, 3>, OPCFIFO_SIZE> &valid_arr,  // opc_valid
    std::array<std::array<bool, 3>, OPCFIFO_SIZE> &ready_arr,        // opc_ready
    int bank_id,
    std::array<int, BANK_NUM> &REGcurrentIdx,
    std::array<int, BANK_NUM> &read_bank_addr)
{
    const int rows = OPCFIFO_SIZE; // = addr_arr.size()
    const int cols = 3;            // = addr_arr[0].size(), 每个opc_fifo_t四个待取元素
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
        ev_regfile_readdata.notify();
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
    // FloatAndInt newFI;

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
            // write_f = false;
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
            // write_f = false;
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
            // write_f = true;
            wb_ena = true;
            execpop_vfpu = true;
            execpop_salu = false;
            execpop_valu = false;
            execpop_lsu = false;
            wb_ins = vfputop_dat.ins;
            if (vfputop_dat.ins.ddd.wxd) // FEQ_S_等指令
            {
                write_v = false;
                write_s = true;
                rds1_addr = vfputop_dat.ins.d;
                rds1_data = vfputop_dat.rds1_data;
            }
            else
            {
                write_v = true;
                write_s = false;
                rdv1_addr = vfputop_dat.ins.d;
                // cout << "WB: let wb_ins=" << vfputop_dat.ins << "warp" << vfputop_dat.warp_id << ", rdf1_data={";
                for (int i = 0; i < num_thread; i++)
                {
                    // newFI.f = vfputop_dat.rdf1_data[i];
                    // cout << std::hex << newFI.i << std::dec << ",";
                    rdv1_data[i].write(vfputop_dat.rdf1_data[i]);
                }
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
            case LW_:
                write_s = true;
                write_v = false;
                write_f = false;
                wb_ena = true;
                wb_ins = lsutop_dat.ins;
                rds1_addr = lsutop_dat.ins.d;
                rds1_data = lsutop_dat.rds1_data;
                // cout << "WB: arbit LW_ writeback, ins=" << lsutop_dat.ins << "warp" << lsutop_dat.warp_id << " at " << sc_time_stamp() << "," << sc_delta_count_at_current_time() << "\n";
                break;
            case VLE32_V_:
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
                cout << "SM" << sm_id << " WRITE_REG ins" << wb_ins << "warp" << warp_id << ": scalar, s_regfile[" << rds1_addr.read() << "]="
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
                     << "}, mask=" << wb_ins.read().mask << " at " << sc_time_stamp() << "," << sc_delta_count_at_current_time() << "\n";
                for (int i = 0; i < num_thread; i++)
                    if (wb_ins.read().mask[i] == 1)
                        WARPS[warp_id].v_regfile[rdv1_addr.read()][i] = rdv1_data[i];
            }
            // if (write_f)
            // {
            //     cout << "WRITE_REG ins" << wb_ins << "warp" << warp_id << ": float, v_regfile[" << rdf1_addr.read() << "]={"
            //          << std::hex
            //          << rdf1_data[0].read() << "," << rdf1_data[1].read() << "," << rdf1_data[2].read() << "," << rdf1_data[3].read() << ","
            //          << rdf1_data[4].read() << "," << rdf1_data[5].read() << "," << rdf1_data[6].read() << "," << rdf1_data[7].read()
            //          << std::dec
            //          << "}, mask=" << wb_ins.read().mask << " at " << sc_time_stamp() << "," << sc_delta_count_at_current_time() << "\n";
            //     for (int i = 0; i < num_thread; i++)
            //     {
            //         // f1 = rdf1_data[i];
            //         // pa1 = &f1;
            //         // WARPS[warp_id].v_regfile[rdf1_addr.read()][i] = *(reg_t *)(pa1);
            //         if (wb_ins.read().mask[i] == 1)
            //             WARPS[warp_id].v_regfile[rdf1_addr.read()][i] = rdf1_data[i].read();
            //     }
            // }
        }
    }
}
