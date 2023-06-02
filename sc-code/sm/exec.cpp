#include "BASE.h"

void BASE::SALU_IN()
{
    I_TYPE new_ins;
    salu_in_t new_data;
    int a_delay, b_delay;
    while (true)
    {
        wait();
        if (emito_salu)
        {
            // cout << "SALU_IN: receive ins=" << emit_ins << "warp" << emitins_warpid << " at " << sc_time_stamp() << "," << sc_delta_count_at_current_time() << "\n";
            if (salu_ready_old == false)
                cout << "salu error: not ready at " << sc_time_stamp() << "," << sc_delta_count_at_current_time() << "\n";
            salu_unready.notify();
            switch (emit_ins.read().op)
            {
            case BNE_:
            case BEQ_:
            case BLT_:
            case BLTU_:
            case BGE_:
            case BGEU_:
            case JAL_:
            case JALR_:
            case AUIPC_:
            case LUI_:
            case ADD_:
            case ADDI_:
            case SLT_:
            case SLTI_:
            case SLTIU_:
            case SLTU_:
            case AND_:
            case ANDI_:
            case OR_:
            case ORI_:
            case XOR_:
            case XORI_:
            case SUB_:
            case SLL_:
            case SRL_:
            case SRA_:
            case SLLI_:
            case SRLI_:
            case SRAI_:
            case MUL_:
            case MULH_:
            case MULHSU_:
            case MULHU_:
            case DIV_:
            case DIVU_:
            case REM_:
            case REMU_:
                new_data.ins = emit_ins;
                new_data.warp_id = emitins_warpid;
                new_data.rss1_data = tosalu_data1;
                new_data.rss2_data = tosalu_data2;
                new_data.rss3_data = tosalu_data3;
                salu_dq.push(new_data);
                // cout << "salu_dq has just pushed 1 elem at " << sc_time_stamp() <<","<< sc_delta_count_at_current_time() << "\n";
                a_delay = 1;
                b_delay = 1;
                // cout << "SALU_IN: see salueqa_triggered=" << salueqa_triggered << " at " << sc_time_stamp() << "," << sc_delta_count_at_current_time() << "\n";
                if (a_delay == 0)
                    salu_eva.notify();
                else if (salueqa_triggered)
                {
                    salu_eqa.notify(sc_time((a_delay)*PERIOD, SC_NS));
                    // cout << "SALU_IN detect salueqa is triggered at " << sc_time_stamp() << "," << sc_delta_count_at_current_time() << "\n";
                }
                else
                {
                    salu_eqa.notify(sc_time((a_delay)*PERIOD, SC_NS));
                    ev_salufifo_pushed.notify();
                    for (auto &warp_ : WARPS)
                    {
                        warp_.jump = false;
                        warp_.branch_sig = false;
                    }
                }
                if (b_delay == 0)
                    salu_evb.notify();
                else
                { // 这都是emit的情况，所以这个cycle eqb不可能被触发
                    salu_eqb.notify(sc_time((b_delay)*PERIOD, SC_NS));
                    ev_saluready_updated.notify();
                }
                // cout << "SALU_IN switch to ADD_ (from opc input) at " << sc_time_stamp() <<","<< sc_delta_count_at_current_time() << "\n";
                break;
            default:
                cout << "salu error: receive wrong ins " << emit_ins << " at " << sc_time_stamp() << "," << sc_delta_count_at_current_time() << "\n";
                break;
            }
        }
        else
        {
            if (!salueqa_triggered)
            {
                ev_salufifo_pushed.notify();
                for (auto &warp_ : WARPS)
                {
                    warp_.jump = false;
                    warp_.branch_sig = false;
                }
            }
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
        if (salu_eqa.default_event().triggered())
        {
            // cout << "SALU_CALC detect salueqa triggered at " << sc_time_stamp() << "," << sc_delta_count_at_current_time() << "\n";
            salueqa_triggered = true;
            wait(SC_ZERO_TIME);
            salueqa_triggered = false;
        }
        for (auto &warp_ : WARPS)
        {
            warp_.jump = false;
            warp_.branch_sig = false;
        }
        salutmp1 = salu_dq.front();
        // cout << "salu_dq.front's ins is " << salutmp1.ins << ", data is " << salutmp1.rss1_data << "," << salutmp1.rss2_data << "\n";
        salu_dq.pop();
        // cout << "salu_dq has poped, now its elem_num is " << salu_dq.size() << " at " << sc_time_stamp() <<","<< sc_delta_count_at_current_time() << "\n";
        if (salutmp1.ins.ddd.wxd)
        {
            salutmp2.ins = salutmp1.ins;
            salutmp2.warp_id = salutmp1.warp_id;
            switch (salutmp1.ins.ddd.alu_fn)
            {
                // case JAL_:
                //     WARPS[salutmp1.warp_id].branch_sig = true;
                //     WARPS[salutmp1.warp_id].jump = 1;
                //     WARPS[salutmp1.warp_id].jump_addr = salutmp1.ins.currentpc + 4 + salutmp1.ins.s2;
                //     salutmp2.data = salutmp1.ins.currentpc + 4;
                //     break;
                // case JALR_:
                //     WARPS[salutmp1.warp_id].branch_sig = true;
                //     WARPS[salutmp1.warp_id].jump = 1;
                //     WARPS[salutmp1.warp_id].jump_addr = (salutmp1.rss1_data + salutmp1.ins.s2) & (~1);
                //     salutmp2.data = salutmp1.ins.currentpc + 4;
                //     break;

                // case ADD_:
                // case ADDI_:
                // case AUIPC_:
                // case JAL_:
                // case JALR_:

            case DecodeParams::FN_ADD:
                salutmp2.data = salutmp1.rss1_data + salutmp1.rss2_data;
                if (salutmp1.ins.ddd.branch == DecodeParams::B_J) // jal
                {
                    WARPS[salutmp1.warp_id].branch_sig = true;
                    WARPS[salutmp1.warp_id].jump = 1;
                    WARPS[salutmp1.warp_id].jump_addr = salutmp1.rss3_data;
                }
                else if (salutmp1.ins.ddd.branch == DecodeParams::B_R) // jalr
                {
                    WARPS[salutmp1.warp_id].branch_sig = true;
                    WARPS[salutmp1.warp_id].jump = 1;
                    WARPS[salutmp1.warp_id].jump_addr = (salutmp1.rss3_data + salutmp1.ins.imm) & (~1);
                }
                break;

            // case AND_:
            // case ANDI_:
            case DecodeParams::FN_AND:
                salutmp2.data = salutmp1.rss1_data & salutmp1.rss2_data;
                break;

            // case LUI_:
            case DecodeParams::FN_A1ZERO:
                salutmp2.data = salutmp1.rss2_data;
                break;

            // case OR_:
            // case ORI_:
            case DecodeParams::FN_OR:
                salutmp2.data = salutmp1.rss1_data | salutmp1.rss2_data;
                break;

            // case SLL_:
            // case SLLI_:
            case DecodeParams::FN_SL:
                salutmp2.data = salutmp1.rss1_data << salutmp1.rss2_data;
                break;

            // case SLT_:
            // case SLTI_:
            case DecodeParams::FN_SLT:
                if (salutmp1.rss1_data < salutmp1.rss2_data)
                    salutmp2.data = 1;
                else
                    salutmp2.data = 0;
                break;

            // case SLTIU_:
            // case SLTU_:
            case DecodeParams::FN_SLTU:
                if (static_cast<unsigned int>(salutmp1.rss1_data) < static_cast<unsigned int>(salutmp1.rss2_data))
                    salutmp2.data = 1;
                else
                    salutmp2.data = 0;
                break;

            // case SRA_:
            // case SRAI_:
            case DecodeParams::FN_SRA:
                salutmp2.data = salutmp1.rss1_data >> salutmp1.rss2_data;
                break;

            // case SRL_:
            // case SRLI_:
            case DecodeParams::FN_SR:
                salutmp2.data = static_cast<unsigned int>(salutmp1.rss1_data) >> salutmp1.rss2_data;
                break;

            // case SUB_:
            case DecodeParams::FN_SUB:
                salutmp2.data = salutmp1.rss1_data - salutmp1.rss2_data;
                break;

            // case XOR_:
            // case XORI_:
            case DecodeParams::FN_XOR:
                salutmp2.data = salutmp1.rss1_data ^ salutmp1.rss2_data;
                break;

            case MUL_:
                salutmp2.data = salutmp1.rss1_data * salutmp1.rss2_data;
                break;
            case MULH_:
                salutmp2.data =
                    static_cast<int>((static_cast<long long>(salutmp1.rss1_data) *
                                      static_cast<long long>(salutmp1.rss2_data)) >>
                                     32);
                break;
            case MULHSU_:
                salutmp2.data =
                    static_cast<int>((static_cast<long long>(salutmp1.rss1_data) *
                                      static_cast<unsigned long long>(salutmp1.rss2_data)) >>
                                     32);
                break;
            case MULHU_:
                salutmp2.data =
                    static_cast<int>((static_cast<unsigned long long>(salutmp1.rss1_data) *
                                      static_cast<unsigned long long>(salutmp1.rss2_data)) >>
                                     32);
                break;
            case DIV_:
                if (salutmp1.rss2_data == 0)
                    cout << "SALU_CALC error: exec DIV_ but rs2=0 at " << sc_time_stamp() << "," << sc_delta_count_at_current_time() << "\n";
                salutmp2.data = salutmp1.rss1_data / salutmp1.rss2_data;
                break;
            case DIVU_:
                if (salutmp1.rss2_data == 0)
                    cout << "SALU_CALC error: exec DIVU_ but rs2=0 at " << sc_time_stamp() << "," << sc_delta_count_at_current_time() << "\n";
                salutmp2.data = static_cast<unsigned int>(salutmp1.rss1_data) / static_cast<unsigned int>(salutmp1.rss2_data);
                break;
            case REM_:
                if (salutmp1.rss2_data == 0)
                    cout << "SALU_CALC error: exec REM_ but rs2=0 at " << sc_time_stamp() << "," << sc_delta_count_at_current_time() << "\n";
                salutmp2.data = salutmp1.rss1_data % salutmp1.rss2_data;
                break;
            case REMU_:
                if (salutmp1.rss2_data == 0)
                    cout << "SALU_CALC error: exec REMU_ but rs2=0 at " << sc_time_stamp() << "," << sc_delta_count_at_current_time() << "\n";
                salutmp2.data = static_cast<unsigned int>(salutmp1.rss1_data) % static_cast<unsigned int>(salutmp1.rss2_data);
                break;
            }
            salufifo.push(salutmp2);
        }
        else
        {
            switch (salutmp1.ins.ddd.alu_fn)
            {
            // case BEQ_:
            case DecodeParams::FN_SEQ:
                WARPS[salutmp1.warp_id].branch_sig = true;
                if (salutmp1.rss1_data == salutmp1.rss2_data)
                {
                    WARPS[salutmp1.warp_id].jump = 1;
                    WARPS[salutmp1.warp_id].jump_addr = salutmp1.rss3_data;
                }
                break;

            // case BGE_:
            case DecodeParams::FN_SGE:
                WARPS[salutmp1.warp_id].branch_sig = true;
                if (salutmp1.rss1_data >= salutmp1.rss2_data)
                {
                    WARPS[salutmp1.warp_id].jump = 1;
                    WARPS[salutmp1.warp_id].jump_addr = salutmp1.rss3_data;
                }
                break;
            // case BGEU_:
            case DecodeParams::FN_SGEU:
                WARPS[salutmp1.warp_id].branch_sig = true;
                if (static_cast<unsigned int>(salutmp1.rss1_data) >= static_cast<unsigned int>(salutmp1.rss2_data))
                {
                    WARPS[salutmp1.warp_id].jump = 1;
                    WARPS[salutmp1.warp_id].jump_addr = salutmp1.rss3_data;
                }
                break;
            // case BLT_:
            case DecodeParams::FN_SLT:
                WARPS[salutmp1.warp_id].branch_sig = true;
                if (salutmp1.rss1_data < salutmp1.rss2_data)
                {
                    WARPS[salutmp1.warp_id].jump = 1;
                    WARPS[salutmp1.warp_id].jump_addr = salutmp1.rss3_data;
                }
                break;
            // case BLTU_:
            case DecodeParams::FN_SLTU:
                WARPS[salutmp1.warp_id].branch_sig = true;
                if (static_cast<unsigned int>(salutmp1.rss1_data) < static_cast<unsigned int>(salutmp1.rss2_data))
                {
                    WARPS[salutmp1.warp_id].jump = 1;
                    WARPS[salutmp1.warp_id].jump_addr = salutmp1.rss3_data;
                }
                break;

            // case BNE_:
            case DecodeParams::FN_SNE:
                WARPS[salutmp1.warp_id].branch_sig = true;
                if (salutmp1.rss1_data != salutmp1.rss2_data)
                {
                    WARPS[salutmp1.warp_id].jump = 1;
                    WARPS[salutmp1.warp_id].jump_addr = salutmp1.rss3_data;
                }
                break;
            }
        }
        ev_salufifo_pushed.notify();
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
    sc_bv<num_thread> _velsemask;
    sc_bv<num_thread> _vifmask;
    while (true)
    {
        wait();
        if (emito_valu)
        {
            if (valu_ready_old == false)
                cout << "valu error: not ready at " << sc_time_stamp() << "," << sc_delta_count_at_current_time() << "\n";
            valu_unready.notify();
            switch (emit_ins.read().op)
            {
            case VBEQ_:
                new_data.ins = emit_ins;
                new_data.warp_id = emitins_warpid;
                for (int i = 0; i < num_thread; i++)
                {
                    new_data.rsv1_data[i] = tovalu_data1[i];
                    new_data.rsv2_data[i] = tovalu_data2[i];
                }
                valu_dq.push(new_data);
                a_delay = 3;
                b_delay = 1;
                if (a_delay == 0)
                    valu_eva.notify();
                else if (valueqa_triggered)
                    valu_eqa.notify(sc_time((a_delay)*PERIOD, SC_NS));
                else
                {
                    valu_eqa.notify(sc_time((a_delay)*PERIOD, SC_NS));
                    ev_valufifo_pushed.notify();
                    valuto_simtstk = false;
                }
                if (b_delay == 0)
                    valu_evb.notify();
                else
                {
                    valu_eqb.notify(sc_time((b_delay)*PERIOD, SC_NS));
                    ev_valuready_updated.notify();
                }
                break;

            // case VADD_VV_:
            // case VADD_VX_:
            default:
                new_data.ins = emit_ins;
                new_data.warp_id = emitins_warpid;
                for (int i = 0; i < num_thread; i++)
                {
                    new_data.rsv1_data[i] = tovalu_data1[i];
                    new_data.rsv2_data[i] = tovalu_data2[i];
                }
                valu_dq.push(new_data);
                a_delay = 3;
                b_delay = 1;
                // cout << "valu: receive VADD_VV_, will notify eq, at " << sc_time_stamp() <<","<< sc_delta_count_at_current_time() << "\n";
                if (a_delay == 0)
                    valu_eva.notify();
                else if (valueqa_triggered)
                    valu_eqa.notify(sc_time((a_delay)*PERIOD, SC_NS));
                else
                {
                    valu_eqa.notify(sc_time((a_delay)*PERIOD, SC_NS));
                    ev_valufifo_pushed.notify();
                    valuto_simtstk = false;
                }
                if (b_delay == 0)
                    valu_evb.notify();
                else
                {
                    valu_eqb.notify(sc_time((b_delay)*PERIOD, SC_NS));
                    ev_valuready_updated.notify();
                }
                break;

                // default:
                //     cout << "valu error: receive wrong ins " << emit_ins << " at " << sc_time_stamp() << "," << sc_delta_count_at_current_time() << "\n";
                // break;
            }
        }
        else
        {
            if (!valueqa_triggered)
            {
                ev_valufifo_pushed.notify();
                valuto_simtstk = false;
            }
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
    sc_bv<num_thread> _velsemask;
    while (true)
    {
        wait(valu_eva | valu_eqa.default_event());
        if (valu_eqa.default_event().triggered())
        {
            valueqa_triggered = true;
            wait(SC_ZERO_TIME);
            valueqa_triggered = false;
        }
        valuto_simtstk = false;
        // cout << "valu_eqa.default_event triggered at " << sc_time_stamp() << "," << sc_delta_count_at_current_time() << "\n";
        valutmp1 = valu_dq.front();
        valu_dq.pop();
        if (valutmp1.ins.ddd.wxd | valutmp1.ins.ddd.wvd)
        {
            valutmp2.ins = valutmp1.ins;
            valutmp2.warp_id = valutmp1.warp_id;
            switch (valutmp1.ins.ddd.alu_fn)
            {

            case DecodeParams::FN_ADD:
                // VADD12.VI, VADD.VI, VADD.VV, VADD.VX
                for (int i = 0; i < num_thread; i++)
                {
                    if (valutmp2.ins.mask[i] == 1)
                        valutmp2.rdv1_data[i] = valutmp1.rsv1_data[i] + valutmp1.rsv2_data[i];
                }
                break;

            case DecodeParams::FN_AND:
                // VAND.VI, VAND.VV, VAND.VX
                for (int i = 0; i < num_thread; i++)
                {
                    if (valutmp2.ins.mask[i] == 1)
                        valutmp2.rdv1_data[i] = valutmp1.rsv1_data[i] & valutmp1.rsv2_data[i];
                }
                break;

            case DecodeParams::FN_SL:
                // VSLL.VI, VSLL.VV, VSLL.VX
                if (!valutmp1.ins.ddd.reverse)
                    for (int i = 0; i < num_thread; i++)
                    {
                        if (valutmp2.ins.mask[i] == 1)
                            valutmp2.rdv1_data[i] = valutmp1.rsv1_data[i] << valutmp1.rsv2_data[i];
                    }
                else
                    for (int i = 0; i < num_thread; i++)
                    {
                        if (valutmp2.ins.mask[i] == 1)
                            valutmp2.rdv1_data[i] = valutmp1.rsv2_data[i] << valutmp1.rsv1_data[i];
                    }
                break;

            case DecodeParams::FN_SUB:
                // VSUB12.VI, VSUB.VV, VSUB.VX
                if (!valutmp1.ins.ddd.reverse)
                    for (int i = 0; i < num_thread; i++)
                    {
                        if (valutmp2.ins.mask[i] == 1)
                            valutmp2.rdv1_data[i] = valutmp1.rsv1_data[i] - valutmp1.rsv2_data[i];
                    }
                else
                    for (int i = 0; i < num_thread; i++)
                    {
                        if (valutmp2.ins.mask[i] == 1)
                            valutmp2.rdv1_data[i] = valutmp1.rsv2_data[i] - valutmp1.rsv1_data[i];
                    }
                break;

            default:
                cout << "VALU_CALC warning: switch to unrecognized ins at " << sc_time_stamp() << "," << sc_delta_count_at_current_time() << "\n";
                break;
            }
            valufifo.push(valutmp2);
        }
        else
        {
            switch (valutmp1.ins.op)
            {
            case VBEQ_:
                for (int i = 0; i < num_thread; i++)
                {
                    if (valutmp1.rsv1_data[i] == valutmp1.rsv2_data[i])
                        _velsemask[i] = 1;
                    else
                        _velsemask[i] = 0;
                }
                branch_elsemask = _velsemask;
                branch_ifmask = ~_velsemask;
                branch_elsepc = valutmp1.ins.currentpc + 4 + valutmp1.ins.d;
                valuto_simtstk = true;
                vbranch_ins = valutmp1.ins;
                vbranchins_warpid = valutmp1.warp_id;
                break;
            default:
                cout << "VALU_CALC warning: switch to unrecognized ins at " << sc_time_stamp() << "," << sc_delta_count_at_current_time() << "\n";
                break;
            }
        }

        ev_valufifo_pushed.notify();
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
            new_data.ins = emit_ins;
            new_data.warp_id = emitins_warpid;

            if (new_data.ins.ddd.sel_alu1 == DecodeParams::A1_VRS1)
                for (int j = 0; j < num_thread; j++)
                    new_data.vfpuSdata1[j] = tovfpu_data1[j];
            else if (new_data.ins.ddd.sel_alu1 == DecodeParams::A1_RS1)
                new_data.vfpuSdata1[0] = tovfpu_data1[0];
            if (new_data.ins.ddd.sel_alu2 == DecodeParams::A2_VRS2)
                for (int j = 0; j < num_thread; j++)
                    new_data.vfpuSdata2[j] = tovfpu_data2[j];
            else if (new_data.ins.ddd.sel_alu2 == DecodeParams::A2_RS2)
                new_data.vfpuSdata2[0] = tovfpu_data2[0];
            if (new_data.ins.ddd.sel_alu3 == DecodeParams::A3_VRS3)
                for (int j = 0; j < num_thread; j++)
                    new_data.vfpuSdata3[j] = tovfpu_data3[j];
            else if (new_data.ins.ddd.sel_alu3 == DecodeParams::A3_FRS3)
                new_data.vfpuSdata3[0] = tovfpu_data3[0];

            vfpu_dq.push(new_data);
            a_delay = 5;
            b_delay = 1;

            // switch (emit_ins.read().op)
            // {

            // case FSQRT_S_:
            // case FCVT_W_S_:
            // case FCVT_WU_S_:
            // case FCLASS_S_:
            //     new_data.rsf1_data[0] = std::bit_cast<float>(tovfpu_data1[0].read());
            //     vfpu_dq.push(new_data);
            //     a_delay = 5;
            //     b_delay = 1;
            //     break;
            // case FCVT_S_W_:
            // case FCVT_S_WU_:
            //     new_data.rss1_data = tovfpu_data1[0];
            //     vfpu_dq.push(new_data);
            //     a_delay = 5;
            //     b_delay = 1;
            //     break;
            // case FMADD_S_:
            // case FMSUB_S_:
            // case FNMSUB_S_:
            // case FNMADD_S_:
            //     new_data.rsf1_data[0] = std::bit_cast<float>(tovfpu_data1[0].read());
            //     new_data.rsf2_data[0] = std::bit_cast<float>(tovfpu_data2[0].read());
            //     new_data.rsf3_data[0] = std::bit_cast<float>(tovfpu_data3[0].read());
            //     vfpu_dq.push(new_data);
            //     a_delay = 5;
            //     b_delay = 1;
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
            // case FEQ_S_:
            // case FLT_S_:
            // case FLE_S_:
            //     new_data.rsf1_data[0] = std::bit_cast<float>(tovfpu_data1[0].read());
            //     new_data.rsf2_data[0] = std::bit_cast<float>(tovfpu_data2[0].read());
            //     vfpu_dq.push(new_data);
            //     a_delay = 5;
            //     b_delay = 1;
            //     break;
            // case VFADD_VV_:
            //     // cout << "VFPU_IN: receive ins" << emit_ins << "warp" << emitins_warpid << ", rs1_data={";
            //     for (int i = 0; i < num_thread; i++)
            //         new_data.rsf1_data[i] = std::bit_cast<float>(tovfpu_data1[i].read());
            //     // cout << "}, rs2_data={";
            //     for (int i = 0; i < num_thread; i++)
            //         new_data.rsf2_data[i] = std::bit_cast<float>(tovfpu_data2[i].read());
            //     // cout << "}, at " << sc_time_stamp() << "," << sc_delta_count_at_current_time() << "\n";
            //     vfpu_dq.push(new_data);
            //     a_delay = 5;
            //     b_delay = 1;
            //     break;
            // default:
            //     cout << "vfpu error: receive wrong ins " << emit_ins << " at " << sc_time_stamp() << "," << sc_delta_count_at_current_time() << "\n";
            // }

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
    float source_f1, source_f2, source_f3;
    while (true)
    {
        wait(vfpu_eva | vfpu_eqa.default_event());
        if (vfpu_eqa.default_event().triggered())
        {
            vfpueqa_triggered = true;
            wait(SC_ZERO_TIME);
            vfpueqa_triggered = false;
        }
        vfputmp1 = vfpu_dq.front();
        vfpu_dq.pop();
        if (vfputmp1.ins.ddd.wxd | vfputmp1.ins.ddd.wvd)
        {
            vfputmp2.ins = vfputmp1.ins;
            vfputmp2.warp_id = vfputmp1.warp_id;
            switch (vfputmp1.ins.ddd.alu_fn)
            {
            case DecodeParams::FN_FADD:
                // VFADD.VF, VFADD.VV
                for (int i = 0; i < num_thread; i++)
                    vfputmp2.rdf1_data[i] = std::bit_cast<int>(std::bit_cast<float>(vfputmp1.vfpuSdata1[i]) + std::bit_cast<float>(vfputmp1.vfpuSdata2[i]));
                break;

            case FSQRT_S_:
                vfputmp2.rdf1_data[0] = std::bit_cast<int>(sqrtf32(std::bit_cast<float>(vfputmp1.vfpuSdata1[0])));
                break;
            case FCVT_W_S_:
                vfputmp2.rds1_data = (int)std::bit_cast<float>(vfputmp1.vfpuSdata1[0]);
                break;
            case FCVT_WU_S_:
                vfputmp2.rds1_data = (unsigned int)std::bit_cast<float>(vfputmp1.vfpuSdata1[0]);
                break;
            case FCLASS_S_:

                break;
            case FCVT_S_W_:
                vfputmp2.rdf1_data[0] = std::bit_cast<int>((float)vfputmp1.vfpuSdata1[0]);
                break;
            case FCVT_S_WU_:
                vfputmp2.rdf1_data[0] = std::bit_cast<int>((float)(unsigned)vfputmp1.vfpuSdata1[0]);
                break;
            case FMADD_S_:
                source_f1 = std::bit_cast<float>(vfputmp1.vfpuSdata1[0]);
                source_f2 = std::bit_cast<float>(vfputmp1.vfpuSdata2[0]);
                source_f3 = std::bit_cast<float>(vfputmp1.vfpuSdata3[0]);
                vfputmp2.rdf1_data[0] = std::bit_cast<int>(source_f1 * source_f2 + source_f3);
                break;
            case FMSUB_S_:
                source_f1 = std::bit_cast<float>(vfputmp1.vfpuSdata1[0]);
                source_f2 = std::bit_cast<float>(vfputmp1.vfpuSdata2[0]);
                source_f3 = std::bit_cast<float>(vfputmp1.vfpuSdata3[0]);
                vfputmp2.rdf1_data[0] = std::bit_cast<int>(source_f1 * source_f2 - source_f3);
                break;
            case FNMSUB_S_:
                source_f1 = std::bit_cast<float>(vfputmp1.vfpuSdata1[0]);
                source_f2 = std::bit_cast<float>(vfputmp1.vfpuSdata2[0]);
                source_f3 = std::bit_cast<float>(vfputmp1.vfpuSdata3[0]);
                vfputmp2.rdf1_data[0] = std::bit_cast<int>(-source_f1 * source_f2 + source_f3);
                break;
            case FNMADD_S_:
                source_f1 = std::bit_cast<float>(vfputmp1.vfpuSdata1[0]);
                source_f2 = std::bit_cast<float>(vfputmp1.vfpuSdata2[0]);
                source_f3 = std::bit_cast<float>(vfputmp1.vfpuSdata3[0]);
                vfputmp2.rdf1_data[0] = std::bit_cast<int>(-source_f1 * source_f2 - source_f3);
                break;
            case FADD_S_:
                source_f1 = std::bit_cast<float>(vfputmp1.vfpuSdata1[0]);
                source_f2 = std::bit_cast<float>(vfputmp1.vfpuSdata2[0]);
                vfputmp2.rdf1_data[0] = std::bit_cast<int>(source_f1 + source_f2);
                break;
            case FSUB_S_:
                source_f1 = std::bit_cast<float>(vfputmp1.vfpuSdata1[0]);
                source_f2 = std::bit_cast<float>(vfputmp1.vfpuSdata2[0]);
                vfputmp2.rdf1_data[0] = std::bit_cast<int>(source_f1 - source_f2);
                break;
            case FMUL_S_:
                source_f1 = std::bit_cast<float>(vfputmp1.vfpuSdata1[0]);
                source_f2 = std::bit_cast<float>(vfputmp1.vfpuSdata2[0]);
                vfputmp2.rdf1_data[0] = std::bit_cast<int>(source_f1 * source_f2);
                break;
            case FDIV_S_:
                source_f1 = std::bit_cast<float>(vfputmp1.vfpuSdata1[0]);
                source_f2 = std::bit_cast<float>(vfputmp1.vfpuSdata2[0]);
                vfputmp2.rdf1_data[0] = std::bit_cast<int>(source_f1 / source_f2);
                break;
            case FSGNJ_S_:
                vfputmp2.rdf1_data[0] = (vfputmp1.vfpuSdata1[0] & 0x7fffffff) | (vfputmp1.vfpuSdata2[0] & 0x80000000);
                break;
            case FSGNJN_S_:
                vfputmp2.rdf1_data[0] = (vfputmp1.vfpuSdata1[0] & 0x7fffffff) | ((vfputmp1.vfpuSdata2[0] & 0x80000000) ^ 0x80000000);
                break;
            case FSGNJX_S_:
                vfputmp2.rdf1_data[0] = (vfputmp1.vfpuSdata1[0] & 0x7fffffff) |
                                        ((vfputmp1.vfpuSdata2[0] & 0x80000000) ^ (vfputmp1.vfpuSdata2[0] & 0x80000000));
                break;
            case FMIN_S_:
                source_f1 = std::bit_cast<float>(vfputmp1.vfpuSdata1[0]);
                source_f2 = std::bit_cast<float>(vfputmp1.vfpuSdata2[0]);
                vfputmp2.rdf1_data[0] = std::bit_cast<int>(source_f1 < source_f2 ? source_f1 : source_f2);
                break;
            case FMAX_S_:
                source_f1 = std::bit_cast<float>(vfputmp1.vfpuSdata1[0]);
                source_f2 = std::bit_cast<float>(vfputmp1.vfpuSdata2[0]);
                vfputmp2.rdf1_data[0] = std::bit_cast<int>(source_f1 > source_f2 ? source_f1 : source_f2);
                break;
            case FEQ_S_:
                source_f1 = std::bit_cast<float>(vfputmp1.vfpuSdata1[0]);
                source_f2 = std::bit_cast<float>(vfputmp1.vfpuSdata2[0]);
                vfputmp2.rds1_data = source_f1 == source_f2;
                break;
            case FLT_S_:
                source_f1 = std::bit_cast<float>(vfputmp1.vfpuSdata1[0]);
                source_f2 = std::bit_cast<float>(vfputmp1.vfpuSdata2[0]);
                vfputmp2.rds1_data = source_f1 < source_f2;
                break;
            case FLE_S_:
                source_f1 = std::bit_cast<float>(vfputmp1.vfpuSdata1[0]);
                source_f2 = std::bit_cast<float>(vfputmp1.vfpuSdata2[0]);
                vfputmp2.rds1_data = source_f1 <= source_f2;
                break;
            case VFMUL_VV_:
                for (int i = 0; i < num_thread; i++)
                    vfputmp2.rdf1_data[i] = std::bit_cast<int>(std::bit_cast<float>(vfputmp1.vfpuSdata1[i]) * std::bit_cast<float>(vfputmp1.vfpuSdata2[i]));
                break;
            case VFMUL_VF_:
                for (int i = 0; i < num_thread; i++)
                    vfputmp2.rdf1_data[i] = std::bit_cast<int>(std::bit_cast<float>(vfputmp1.vfpuSdata1[0]) + std::bit_cast<float>(vfputmp1.vfpuSdata2[i]));
                break;
            case VFMADD_VV_:
                for (int i = 0; i < num_thread; i++)
                    vfputmp2.rdf1_data[i] = std::bit_cast<int>(
                        std::bit_cast<float>(vfputmp1.vfpuSdata1[i]) * std::bit_cast<float>(vfputmp1.vfpuSdata3[i]) + std::bit_cast<float>(vfputmp1.vfpuSdata2[i]));
                break;
            case VFMADD_VF_:
                for (int i = 0; i < num_thread; i++)
                    vfputmp2.rdf1_data[i] = std::bit_cast<int>(
                        std::bit_cast<float>(vfputmp1.vfpuSdata1[0]) * std::bit_cast<float>(vfputmp1.vfpuSdata3[i]) + std::bit_cast<float>(vfputmp1.vfpuSdata2[i]));
                break;
            case VFNMADD_VV_:
                for (int i = 0; i < num_thread; i++)
                    vfputmp2.rdf1_data[i] = std::bit_cast<int>(
                        -std::bit_cast<float>(vfputmp1.vfpuSdata1[i]) * std::bit_cast<float>(vfputmp1.vfpuSdata3[i]) - std::bit_cast<float>(vfputmp1.vfpuSdata2[i]));
                break;
            case VFNMADD_VF_:
                for (int i = 0; i < num_thread; i++)
                    vfputmp2.rdf1_data[i] = std::bit_cast<int>(
                        -std::bit_cast<float>(vfputmp1.vfpuSdata1[0]) * std::bit_cast<float>(vfputmp1.vfpuSdata3[i]) - std::bit_cast<float>(vfputmp1.vfpuSdata2[i]));
                break;
            case VFMSUB_VV_:
                for (int i = 0; i < num_thread; i++)
                    vfputmp2.rdf1_data[i] = std::bit_cast<int>(
                        std::bit_cast<float>(vfputmp1.vfpuSdata1[i]) * std::bit_cast<float>(vfputmp1.vfpuSdata3[i]) - std::bit_cast<float>(vfputmp1.vfpuSdata2[i]));
                break;
            case VFMSUB_VF_:
                for (int i = 0; i < num_thread; i++)
                    vfputmp2.rdf1_data[i] = std::bit_cast<int>(
                        std::bit_cast<float>(vfputmp1.vfpuSdata1[0]) * std::bit_cast<float>(vfputmp1.vfpuSdata3[i]) - std::bit_cast<float>(vfputmp1.vfpuSdata2[i]));
                break;
            case VFNMSUB_VV_:
                for (int i = 0; i < num_thread; i++)
                    vfputmp2.rdf1_data[i] = std::bit_cast<int>(
                        -std::bit_cast<float>(vfputmp1.vfpuSdata1[i]) * std::bit_cast<float>(vfputmp1.vfpuSdata3[i]) + std::bit_cast<float>(vfputmp1.vfpuSdata2[i]));
                break;
            case VFNMSUB_VF_:
                for (int i = 0; i < num_thread; i++)
                    vfputmp2.rdf1_data[i] = std::bit_cast<int>(
                        -std::bit_cast<float>(vfputmp1.vfpuSdata1[0]) * std::bit_cast<float>(vfputmp1.vfpuSdata3[i]) + std::bit_cast<float>(vfputmp1.vfpuSdata2[i]));
                break;
            case VFMACC_VV_:
                for (int i = 0; i < num_thread; i++)
                    vfputmp2.rdf1_data[i] = std::bit_cast<int>(
                        std::bit_cast<float>(vfputmp1.vfpuSdata1[i]) * std::bit_cast<float>(vfputmp1.vfpuSdata2[i]) + std::bit_cast<float>(vfputmp1.vfpuSdata3[i]));
                break;
            case VFMACC_VF_:
                for (int i = 0; i < num_thread; i++)
                    vfputmp2.rdf1_data[i] = std::bit_cast<int>(
                        std::bit_cast<float>(vfputmp1.vfpuSdata1[0]) * std::bit_cast<float>(vfputmp1.vfpuSdata2[i]) + std::bit_cast<float>(vfputmp1.vfpuSdata3[i]));
                break;
            }
            vfpufifo.push(vfputmp2);
        }
        else
        {
        }

        ev_vfpufifo_pushed.notify();
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
            case LW_:
            case SW_:
            case VLE32_V_:
                // cout << "LSU receive lw ins=" << emit_ins << "warp" << emitins_warpid << " at " << sc_time_stamp() << "," << sc_delta_count_at_current_time() << "\n";
                new_data.ins = emit_ins;
                new_data.warp_id = emitins_warpid;
                new_data.rss1_data = tolsu_data1[0];
                new_data.rds1_data = tolsu_data2[0]; // for SW, store rs2data into ext_mem
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
        if (lsu_eqa.default_event().triggered())
        {
            lsueqa_triggered = true;
            wait(SC_ZERO_TIME);
            lsueqa_triggered = false;
        }
        // cout << "LSU_OUT: triggered by eva/eqa at " << sc_time_stamp() << "," << sc_delta_count_at_current_time() << "\n";
        lsutmp1 = lsu_dq.front();
        lsu_dq.pop();
        switch (lsutmp1.ins.op)
        {
        case LW_:
            // cout << "LSU_CALC: calc lw, rss1=" << lsutmp1.rss1_data
            //      << ", ins=" << lsutmp1.ins << "warp" << lsutmp1.warp_id << " at " << sc_time_stamp() << "," << sc_delta_count_at_current_time() << "\n";
            lsutmp2.ins = lsutmp1.ins;
            lsutmp2.warp_id = lsutmp1.warp_id;

            // external_addr = (lsutmp1.rss1_data + lsutmp1.ins.s2) / 4 - 128 * num_thread; // 减去CSR_GDS
            external_addr = lsutmp1.rss1_data + lsutmp1.rss2_data;

            // cout << "LSU_CALC: read lw rss1_data=" << lsutmp1.rss1_data
            //      << ", external addr=" << external_addr
            //      << ", ins=" << lsutmp1.ins
            //      << ", external_mem[]=" << external_mem[external_addr]
            //      << " at " << sc_time_stamp() << "," << sc_delta_count_at_current_time() << "\n";

            // lsutmp2.rds1_data = external_mem[external_addr];
            lsutmp2.rds1_data = getBufferData(*buffer_data, external_addr, mtd.num_buffer, mtd.buffer_base, mtd.buffer_size);

            lsufifo.push(lsutmp2);
            // cout << "LSU_CALC: lw, pushed " << lsutmp2.rds1_data << " to s_regfile rd=" << lsutmp1.ins.d << " at " << sc_time_stamp() << "," << sc_delta_count_at_current_time() << "\n";
            break;
        case SW_:
            // external_addr = (lsutmp1.rss1_data + lsutmp1.ins.d) / 4 - 128 * num_thread;
            external_addr = lsutmp1.rss1_data + lsutmp1.rss2_data;
            // external_mem[external_addr] = lsutmp1.rds1_data;
            writeBufferData(lsutmp1.rss3_data, *buffer_data, external_addr, mtd.num_buffer, mtd.buffer_base, mtd.buffer_size);
            break;
        case VLE32_V_:
            lsutmp2.ins = lsutmp1.ins;
            lsutmp2.warp_id = lsutmp1.warp_id;
            // cout << "LSU_CALC: calc vle32v, rss1=" << lsutmp1.rss1_data << " at " << sc_time_stamp() << "," << sc_delta_count_at_current_time() << "\n";
            for (int i = 0; i < num_thread; i++)
            {
                // lsutmp2.rdv1_data[i] = external_mem[lsutmp1.rss1_data / 4 + i - 128 * num_thread];
                lsutmp2.rdv1_data[i] = getBufferData(*buffer_data, lsutmp1.rss1_data + i * 4, mtd.num_buffer, mtd.buffer_base, mtd.buffer_size);
            }
            lsufifo.push(lsutmp2);
            // cout << "LSU_CALC: pushed vle32v output at " << sc_time_stamp() << "," << sc_delta_count_at_current_time() << "\n";
            break;
        default:
            cout << "LSU error: unimplemented instruction! op=" << lsutmp1.ins.op << "\n";
            break;
        }
        ev_lsufifo_pushed.notify();
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

void BASE::SIMT_STACK(int warp_id)
{
    simtstack_t newstkelem;
    I_TYPE readins;
    while (true)
    {
        wait(clk.posedge_event());
        WARPS[warp_id].simtstk_jump = false;
        WARPS[warp_id].simtstk_flush = false;
        WARPS[warp_id].vbran_sig = false;
        if (valuto_simtstk && vbranchins_warpid == warp_id) // VALU计算的beq类指令
        {
            WARPS[warp_id].vbran_sig = true;
            if (emito_simtstk)
                cout << "SIMT-STACK error: receive join & beq at the same time at " << sc_time_stamp() << "," << sc_delta_count_at_current_time() << "\n";

            /*** 以下为stack管理 ***/
            if (std::bitset<num_thread>(branch_elsemask.read().to_string()) == 0)
            { // VALU计算出的elsemask全为0
                newstkelem.pair = 1;
                newstkelem.is_part = 1;
            }
            else if (std::bitset<num_thread>(branch_elsemask.read().to_string()) == std::bitset<num_thread>().set())
            { // VALU计算出的ifmask全为0
                newstkelem.pair = 0;
                newstkelem.is_part = 1;
            }
            else
            {
                newstkelem.pair = 0;
                newstkelem.is_part = 0;
            }
            newstkelem.rmask = vbranch_ins.read().mask;
            newstkelem.elsepc = branch_elsepc;
            newstkelem.elsemask = branch_elsemask;
            WARPS[warp_id].simt_stack.push(newstkelem);
            cout << "SIMT-stack warp" << warp_id << " pushed elem" << newstkelem << " at " << sc_time_stamp() << "," << sc_delta_count_at_current_time() << "\n";

            /*** 以下为分支控制 ***/
            if (std::bitset<num_thread>(branch_elsemask.read().to_string()) == std::bitset<num_thread>().set())
            { // ifmask全为0
                WARPS[warp_id].simtstk_jumpaddr = branch_elsepc;
                WARPS[warp_id].current_mask = branch_elsemask;
                WARPS[warp_id].simtstk_jump = true;
                WARPS[warp_id].simtstk_flush = true;
            }
            else
            { // 需要先走if path
                WARPS[warp_id].current_mask = branch_ifmask;
            }
        }
        if (emito_simtstk && emitins_warpid == warp_id) // OPC发射的join指令
        {
            WARPS[warp_id].vbran_sig = true;
            simtstack_t &tmpstkelem = WARPS[warp_id].simt_stack.top();
            readins = emit_ins;
            /*** 以下为分支控制 ***/
            if (tmpstkelem.pair == 1)
            { // 意味着elsemask全为0，此时遇到的是if path的join
                // ↓直接跳转到汇合点，跳转地址是join指令的跳转地址
                WARPS[warp_id].simtstk_jumpaddr = readins.currentpc + 4 + readins.d;
                WARPS[warp_id].current_mask = tmpstkelem.rmask;
                WARPS[warp_id].simtstk_jump = true;
                WARPS[warp_id].simtstk_flush = true;
            }
            else if (tmpstkelem.is_part == 1 && readins.d == 1)
            { // else path结束，汇合点紧跟着else path的指令
                // 所以readins.d==1，即join指令的相对跳转为1，
                // 这意味着else path紧跟着汇合点，不需要冲刷
                WARPS[warp_id].current_mask = tmpstkelem.rmask;
                WARPS[warp_id].simtstk_jump = false;
                WARPS[warp_id].simtstk_flush = false;
            }
            else if (tmpstkelem.is_part == 1)
            { // else path结束，汇合点紧跟着else path的指令
                // 所以readins.d==1，即join指令的跳转为1
                WARPS[warp_id].simtstk_jumpaddr = readins.currentpc + 4 + readins.d;
                WARPS[warp_id].current_mask = tmpstkelem.rmask;
                WARPS[warp_id].simtstk_jump = true;
                WARPS[warp_id].simtstk_flush = true;
            }
            else
            {
                WARPS[warp_id].current_mask = tmpstkelem.elsemask;
                // 不用跳转到else pc???
            }

            /*** 以下为stack管理 ***/
            if (tmpstkelem.is_part == 1)
            {
                WARPS[warp_id].simt_stack.pop();
            }
            else
            {
                tmpstkelem.is_part = 1;
            }
        }
    }
}

void BASE::CSR_IN()
{
    I_TYPE new_ins;
    csr_in_t new_data;
    int a_delay, b_delay;
    while (true)
    {
        wait();
        if (emito_csr)
        {
            if (csr_ready_old == false)
            {
                cout << "csr error: not ready at " << sc_time_stamp() << "," << sc_delta_count_at_current_time() << "\n";
            }
            csr_unready.notify();
            new_data.ins = emit_ins;
            new_data.warp_id = emitins_warpid;

            if (new_data.ins.ddd.sel_alu1 == DecodeParams::A1_VRS1)
                cout << "CSR error: receive vector inst at " << sc_time_stamp() << "," << sc_delta_count_at_current_time() << "\n";
            else if (new_data.ins.ddd.sel_alu1 == DecodeParams::A1_RS1)
                new_data.csrSdata1 = tocsr_data1;
            if (new_data.ins.ddd.sel_alu2 == DecodeParams::A2_VRS2)
                cout << "CSR error: receive vector inst at " << sc_time_stamp() << "," << sc_delta_count_at_current_time() << "\n";
            else if (new_data.ins.ddd.sel_alu2 == DecodeParams::A2_RS2)
                new_data.csrSdata2 = tocsr_data2;

            csr_dq.push(new_data);
            a_delay = 0;
            b_delay = 0;

            if (a_delay == 0)
                csr_eva.notify();
            else if (csreqa_triggered)
                csr_eqa.notify(sc_time((a_delay)*PERIOD, SC_NS));
            else
            {
                csr_eqa.notify(sc_time((a_delay)*PERIOD, SC_NS));
                ev_csrfifo_pushed.notify();
            }
            if (b_delay == 0)
                csr_evb.notify();
            else
            {
                csr_eqb.notify(sc_time((b_delay)*PERIOD, SC_NS));
                ev_csrready_updated.notify();
            }
        }
        else
        {
            if (!csreqa_triggered)
                ev_csrfifo_pushed.notify();
            if (!csreqb_triggered)
                ev_csrready_updated.notify();
        }
    }
}

void BASE::CSR_CALC()
{
    csrfifo_elem_num = 0;
    csrfifo_empty = true;
    csreqa_triggered = false;
    csr_in_t csrtmp1;
    csr_out_t csrtmp2;
    bool succeed;
    int t;
    while (true)
    {
        wait(csr_eva | csr_eqa.default_event());
        if (csr_eqa.default_event().triggered())
        {
            csreqa_triggered = true;
            wait(SC_ZERO_TIME);
            csreqa_triggered = false;
        }
        csrtmp1 = csr_dq.front();
        csr_dq.pop();
        if (csrtmp1.ins.ddd.wxd | csrtmp1.ins.ddd.wvd)
        {
            csrtmp2.ins = csrtmp1.ins;
            csrtmp2.warp_id = csrtmp1.warp_id;
            int csr_addr = extractBits32(csrtmp2.ins.origin32bit, 31, 20) - 0x800;
            switch (csrtmp1.ins.op)
            {
            case CSRRW_:
                // case DecodeParams::FN_ADD:
                t = WARPS[csrtmp1.warp_id].CSR_reg[csr_addr];
                csrtmp2.data = t;
                WARPS[csrtmp1.warp_id].CSR_reg[csr_addr] = t | csrtmp1.csrSdata1;
                break;
            case CSRRS_:
                t = WARPS[csrtmp1.warp_id].CSR_reg[csr_addr];
                csrtmp2.data = t;
                WARPS[csrtmp1.warp_id].CSR_reg[csr_addr] = csrtmp1.csrSdata1;
                break;
            case CSRRC_:
                t = WARPS[csrtmp1.warp_id].CSR_reg[csr_addr];
                csrtmp2.data = t;
                WARPS[csrtmp1.warp_id].CSR_reg[csr_addr] = t & ~csrtmp1.csrSdata1;
                break;
            case CSRRWI_:
                csrtmp2.data = WARPS[csrtmp1.warp_id].CSR_reg[csr_addr];
                WARPS[csrtmp1.warp_id].CSR_reg[csr_addr] = csrtmp1.ins.s1;
                break;
            case CSRRSI_:
                t = WARPS[csrtmp1.warp_id].CSR_reg[csr_addr];
                csrtmp2.data = t;
                WARPS[csrtmp1.warp_id].CSR_reg[csr_addr] = csrtmp1.ins.s1;
                break;
            case CSRRCI_:
                t = WARPS[csrtmp1.warp_id].CSR_reg[csr_addr];
                csrtmp2.data = t;
                WARPS[csrtmp1.warp_id].CSR_reg[csr_addr] = t & ~csrtmp1.ins.s1;
                break;
            }
            csrfifo.push(csrtmp2);
        }
        else
        {
        }

        ev_csrfifo_pushed.notify();
    }
}

void BASE::CSR_CTRL()
{
    csr_ready = true;
    csr_ready_old = true;
    csreqb_triggered = false;
    while (true)
    {
        wait(csr_eqb.default_event() | csr_unready | csr_evb);
        if (csr_eqb.default_event().triggered())
        {
            csr_ready = true;
            csr_ready_old = csr_ready;
            csreqb_triggered = true;
            wait(SC_ZERO_TIME);
            csreqb_triggered = false;
            ev_csrready_updated.notify();
        }
        else if (csr_evb.triggered())
        {
            csr_ready = true;
            csr_ready_old = csr_ready;
            ev_csrready_updated.notify();
        }
        else if (csr_unready.triggered())
        {
            csr_ready = false;
            csr_ready_old = csr_ready;
            ev_csrready_updated.notify();
        }
    }
}

void BASE::MUL_IN()
{
    I_TYPE new_ins;
    mul_in_t new_data;
    int a_delay, b_delay;
    while (true)
    {
        wait();
        if (emito_mul)
        {
            if (mul_ready_old == false)
                cout << "mul error: not ready at " << sc_time_stamp() << "," << sc_delta_count_at_current_time() << "\n";
            mul_unready.notify();
            switch (emit_ins.read().op)
            {

            default:
                new_data.ins = emit_ins;
                new_data.warp_id = emitins_warpid;
                for (int i = 0; i < num_thread; i++)
                {
                    new_data.rsv1_data[i] = tomul_data1[i];
                    new_data.rsv2_data[i] = tomul_data2[i];
                }
                mul_dq.push(new_data);
                a_delay = 3;
                b_delay = 1;
                // cout << "mul: receive VADD_VV_, will notify eq, at " << sc_time_stamp() <<","<< sc_delta_count_at_current_time() << "\n";
                if (a_delay == 0)
                    mul_eva.notify();
                else if (muleqa_triggered)
                    mul_eqa.notify(sc_time((a_delay)*PERIOD, SC_NS));
                else
                {
                    mul_eqa.notify(sc_time((a_delay)*PERIOD, SC_NS));
                    ev_mulfifo_pushed.notify();
                }
                if (b_delay == 0)
                    mul_evb.notify();
                else
                {
                    mul_eqb.notify(sc_time((b_delay)*PERIOD, SC_NS));
                    ev_mulready_updated.notify();
                }
                break;

                // default:
                //     cout << "mul error: receive wrong ins " << emit_ins << " at " << sc_time_stamp() << "," << sc_delta_count_at_current_time() << "\n";
                // break;
            }
        }
        else
        {
            if (!muleqa_triggered)
            {
                ev_mulfifo_pushed.notify();
            }
            if (!muleqb_triggered)
                ev_mulready_updated.notify();
        }
    }
}

void BASE::MUL_CALC()
{
    mulfifo_elem_num = 0;
    mulfifo_empty = 1;
    muleqa_triggered = false;
    mul_in_t multmp1;
    mul_out_t multmp2;
    bool succeed;
    while (true)
    {
        wait(mul_eva | mul_eqa.default_event());
        if (mul_eqa.default_event().triggered())
        {
            muleqa_triggered = true;
            wait(SC_ZERO_TIME);
            muleqa_triggered = false;
        }
        // cout << "mul_eqa.default_event triggered at " << sc_time_stamp() << "," << sc_delta_count_at_current_time() << "\n";
        multmp1 = mul_dq.front();
        mul_dq.pop();
        if (multmp1.ins.ddd.wxd | multmp1.ins.ddd.wvd)
        {
            multmp2.ins = multmp1.ins;
            multmp2.warp_id = multmp1.warp_id;
            switch (multmp1.ins.ddd.alu_fn)
            {

            case DecodeParams::FN_MUL:
                // VMUL.VV, VMUL.VX
                for (int i = 0; i < num_thread; i++)
                {
                    if (multmp2.ins.mask[i] == 1)
                        multmp2.rdv1_data[i] = multmp1.rsv1_data[i] * multmp1.rsv2_data[i];
                }
                break;

            default:
                cout << "MUL_CALC warning: switch to unrecognized ins at " << sc_time_stamp() << "," << sc_delta_count_at_current_time() << "\n";
                break;
            }
            mulfifo.push(multmp2);
        }
        else
        {
            switch (multmp1.ins.op)
            {

            default:
                cout << "MUL_CALC warning: switch to unrecognized ins at " << sc_time_stamp() << "," << sc_delta_count_at_current_time() << "\n";
                break;
            }
        }

        ev_mulfifo_pushed.notify();
    }
}

void BASE::MUL_CTRL()
{
    mul_ready = true;
    mul_ready_old = true;
    muleqb_triggered = false;
    while (true)
    {
        wait(mul_eqb.default_event() | mul_unready | mul_evb);
        if (mul_eqb.default_event().triggered())
        {
            mul_ready = true;
            mul_ready_old = mul_ready;
            muleqb_triggered = true;
            wait(SC_ZERO_TIME);
            muleqb_triggered = false;
            ev_mulready_updated.notify();
        }
        else if (mul_evb.triggered())
        {
            mul_ready = true;
            mul_ready_old = mul_ready;
            ev_mulready_updated.notify();
        }
        else if (mul_unready.triggered())
        {
            mul_ready = false;
            mul_ready_old = mul_ready;
            ev_mulready_updated.notify();
        }
    }
}

void BASE::SFU_IN()
{
    I_TYPE new_ins;
    sfu_in_t new_data;
    int a_delay, b_delay;
    while (true)
    {
        wait();
        if (emito_sfu)
        {
            if (sfu_ready_old == false)
                cout << "sfu error: not ready at " << sc_time_stamp() << "," << sc_delta_count_at_current_time() << "\n";
            sfu_unready.notify();
            switch (emit_ins.read().op)
            {

            default:
                new_data.ins = emit_ins;
                new_data.warp_id = emitins_warpid;
                for (int i = 0; i < num_thread; i++)
                {
                    new_data.rsv1_data[i] = tosfu_data1[i];
                    new_data.rsv2_data[i] = tosfu_data2[i];
                }
                sfu_dq.push(new_data);
                a_delay = 3;
                b_delay = 1;
                // cout << "sfu: receive VADD_VV_, will notify eq, at " << sc_time_stamp() <<","<< sc_delta_count_at_current_time() << "\n";
                if (a_delay == 0)
                    sfu_eva.notify();
                else if (sfueqa_triggered)
                    sfu_eqa.notify(sc_time((a_delay)*PERIOD, SC_NS));
                else
                {
                    sfu_eqa.notify(sc_time((a_delay)*PERIOD, SC_NS));
                    ev_sfufifo_pushed.notify();
                }
                if (b_delay == 0)
                    sfu_evb.notify();
                else
                {
                    sfu_eqb.notify(sc_time((b_delay)*PERIOD, SC_NS));
                    ev_sfuready_updated.notify();
                }
                break;

                // default:
                //     cout << "sfu error: receive wrong ins " << emit_ins << " at " << sc_time_stamp() << "," << sc_delta_count_at_current_time() << "\n";
                // break;
            }
        }
        else
        {
            if (!sfueqa_triggered)
            {
                ev_sfufifo_pushed.notify();
            }
            if (!sfueqb_triggered)
                ev_sfuready_updated.notify();
        }
    }
}

void BASE::SFU_CALC()
{
    sfufifo_elem_num = 0;
    sfufifo_empty = 1;
    sfueqa_triggered = false;
    sfu_in_t sfutmp1;
    sfu_out_t sfutmp2;
    bool succeed;
    while (true)
    {
        wait(sfu_eva | sfu_eqa.default_event());
        if (sfu_eqa.default_event().triggered())
        {
            sfueqa_triggered = true;
            wait(SC_ZERO_TIME);
            sfueqa_triggered = false;
        }
        // cout << "sfu_eqa.default_event triggered at " << sc_time_stamp() << "," << sc_delta_count_at_current_time() << "\n";
        sfutmp1 = sfu_dq.front();
        sfu_dq.pop();
        if (sfutmp1.ins.ddd.wxd | sfutmp1.ins.ddd.wvd)
        {
            sfutmp2.ins = sfutmp1.ins;
            sfutmp2.warp_id = sfutmp1.warp_id;
            switch (sfutmp1.ins.ddd.alu_fn)
            {

            case DecodeParams::FN_REMU:
                // VREMU.VV, VREMU.VX
                if (sfutmp1.ins.ddd.isvec)
                {
                    if (sfutmp1.ins.ddd.reverse)
                        for (int i = 0; i < num_thread; i++)
                        {
                            if (sfutmp2.ins.mask[i] == 1)
                                sfutmp2.rdv1_data[i] = (unsigned)sfutmp1.rsv2_data[i] % sfutmp1.rsv1_data[i];
                        }
                    else
                        for (int i = 0; i < num_thread; i++)
                        {
                            if (sfutmp2.ins.mask[i] == 1)
                                sfutmp2.rdv1_data[i] = (unsigned)sfutmp1.rsv1_data[i] % sfutmp1.rsv2_data[i];
                        }
                }
                else
                    sfutmp2.rdv1_data[0] = (unsigned)sfutmp1.rsv1_data[0] % sfutmp1.rsv2_data[0];
                break;

            case DecodeParams::FN_DIVU:
                // VDIVU.VV, VDIVU.VX
                if (sfutmp1.ins.ddd.isvec)
                {
                    if (sfutmp1.ins.ddd.reverse)
                        for (int i = 0; i < num_thread; i++)
                        {
                            if (sfutmp2.ins.mask[i] == 1)
                                sfutmp2.rdv1_data[i] = (unsigned)sfutmp1.rsv2_data[i] / (unsigned)sfutmp1.rsv1_data[i];
                        }
                    else
                        for (int i = 0; i < num_thread; i++)
                        {
                            if (sfutmp2.ins.mask[i] == 1)
                                sfutmp2.rdv1_data[i] = (unsigned)sfutmp1.rsv1_data[i] / (unsigned)sfutmp1.rsv2_data[i];
                        }
                }
                else
                    sfutmp2.rdv1_data[0] = (unsigned)sfutmp1.rsv1_data[0] / (unsigned)sfutmp1.rsv2_data[0];
                break;

            default:
                cout << "SFU_CALC warning: switch to unrecognized ins at " << sc_time_stamp() << "," << sc_delta_count_at_current_time() << "\n";
                break;
            }
            sfufifo.push(sfutmp2);
        }
        else
        {
            switch (sfutmp1.ins.op)
            {

            default:
                cout << "SFU_CALC warning: switch to unrecognized ins at " << sc_time_stamp() << "," << sc_delta_count_at_current_time() << "\n";
                break;
            }
        }

        ev_sfufifo_pushed.notify();
    }
}

void BASE::SFU_CTRL()
{
    sfu_ready = true;
    sfu_ready_old = true;
    sfueqb_triggered = false;
    while (true)
    {
        wait(sfu_eqb.default_event() | sfu_unready | sfu_evb);
        if (sfu_eqb.default_event().triggered())
        {
            sfu_ready = true;
            sfu_ready_old = sfu_ready;
            sfueqb_triggered = true;
            wait(SC_ZERO_TIME);
            sfueqb_triggered = false;
            ev_sfuready_updated.notify();
        }
        else if (sfu_evb.triggered())
        {
            sfu_ready = true;
            sfu_ready_old = sfu_ready;
            ev_sfuready_updated.notify();
        }
        else if (sfu_unready.triggered())
        {
            sfu_ready = false;
            sfu_ready_old = sfu_ready;
            ev_sfuready_updated.notify();
        }
    }
}