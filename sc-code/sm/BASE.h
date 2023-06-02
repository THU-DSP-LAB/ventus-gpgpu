#ifndef BASE_H_
#define BASE_H_

#define SC_INCLUDE_DYNAMIC_PROCESSES
#include "../parameters.h"

class BASE : public sc_core::sc_module
{
public:
    sc_in_clk clk{"clk"};
    sc_in<bool> rst_n{"rst_n"};

    void debug_sti();
    void debug_display();
    void debug_display1();
    void debug_display2();
    void debug_display3();
    void INIT_EXTMEM();
    void INIT_INSMEM();
    uint32_t getBufferData(const std::vector<std::vector<uint8_t>> &buffers, int virtualAddress, int num_buffer, uint64_t *buffer_base, uint64_t *buffer_size);
    void readTextFile(const std::string &filename, std::vector<std::vector<uint8_t>> &buffers, uint64_t *buffer_size);
    void writeBufferData(int writevalue, std::vector<std::vector<uint8_t>> &buffers, int virtualAddress, int num_buffer, uint64_t *buffer_base, uint64_t *buffer_size);

    // fetch
    void INIT_INS();
    void INIT_DECODETABLE();
    void INIT_INSTABLE();
    void PROGRAM_COUNTER(int warp_id);
    // void FETCH_2();
    void INSTRUCTION_REG(int warp_id);
    void DECODE(int warp_id);
    // ibuffer
    void IBUF_ACTION(int warp_id);
    void IBUF_PARAM(int warp_id);
    // scoreboard
    void JUDGE_DISPATCH(int warp_id);
    void UPDATE_SCORE(int warp_id);
    // issue
    void ISSUE_ACTION();
    // opc
    void OPC_FIFO();
    void OPC_FETCH();
    void OPC_EMIT();
    bank_t bank_decode(int warp_id, int srcaddr);
    warpaddr_t bank_undecode(int bank_id, int addr);

    // regfile
    void INIT_REG(int warp_id);
    std::pair<int, int> reg_arbiter(const std::array<std::array<bank_t, 3>, OPCFIFO_SIZE> &addr_arr, // opc_srcaddr
                                    const std::array<std::array<bool, 3>, OPCFIFO_SIZE> &valid_arr,  // opc_valid
                                    std::array<std::array<bool, 3>, OPCFIFO_SIZE> &ready_arr,        // opc_ready
                                    int bank_id,
                                    std::array<int, BANK_NUM> &REGcurrentIdx,
                                    std::array<int, BANK_NUM> &read_bank_addr);
    void READ_REG();
    void WRITE_REG(int warp_id);
    // exec
    void SALU_IN();
    void SALU_CALC();
    void SALU_CTRL();
    void VALU_IN();
    void VALU_CALC();
    void VALU_CTRL();
    void VFPU_IN();
    void VFPU_CALC();
    void VFPU_CTRL();
    void LSU_IN();
    void LSU_CALC();
    void LSU_CTRL();
    void SIMT_STACK(int warp_id);
    void CSR_IN();
    void CSR_CALC();
    void CSR_CTRL();
    void MUL_IN();
    void MUL_CALC();
    void MUL_CTRL();
        void SFU_IN();
    void SFU_CALC();
    void SFU_CTRL();
    // writeback
    void WRITE_BACK();

    // initialize
    void start_of_simulation()
    {
        for (auto &warp_ : WARPS)
        {
            warp_.pc = -1;
            warp_.ibuftop_ins = I_TYPE(INVALID_, 0, 0, 0);
        }
        issue_ins = I_TYPE(INVALID_, 0, 0, 0);
    }

    BASE(sc_core::sc_module_name name, std::string _inssrc)
        : sc_module(name), inssrc(_inssrc)
    {
        SC_HAS_PROCESS(BASE);

        SC_THREAD(debug_sti);
        // SC_THREAD(debug_display);
        // SC_THREAD(debug_display1);
        // SC_THREAD(debug_display2);
        // SC_THREAD(debug_display3);
        SC_THREAD(INIT_EXTMEM);
        SC_THREAD(INIT_INSMEM);
        SC_THREAD(INIT_INSTABLE);
        SC_THREAD(INIT_DECODETABLE);

        for (int i = 0; i < num_warp; i++)
        {
            sc_spawn(sc_bind(&BASE::PROGRAM_COUNTER, this, i), ("warp" + std::to_string(i) + "_PROGRAM_COUNTER").c_str());
            sc_spawn(sc_bind(&BASE::INSTRUCTION_REG, this, i), ("warp" + std::to_string(i) + "_INSTRUCTION_REG").c_str());
            sc_spawn(sc_bind(&BASE::DECODE, this, i), ("warp" + std::to_string(i) + "_DECODE").c_str());
            sc_spawn(sc_bind(&BASE::IBUF_ACTION, this, i), ("warp" + std::to_string(i) + "_IBUF_ACTION").c_str());
            sc_spawn(sc_bind(&BASE::JUDGE_DISPATCH, this, i), ("warp" + std::to_string(i) + "_JUDGE_DISPATCH").c_str());
            sc_spawn(sc_bind(&BASE::UPDATE_SCORE, this, i), ("warp" + std::to_string(i) + "_UPDATE_SCORE").c_str());
            sc_spawn(sc_bind(&BASE::INIT_REG, this, i), ("warp" + std::to_string(i) + "_INIT_REG").c_str());
            sc_spawn(sc_bind(&BASE::SIMT_STACK, this, i), ("warp" + std::to_string(i) + "_SIMT_STACK").c_str());
            sc_spawn(sc_bind(&BASE::WRITE_REG, this, i), ("warp" + std::to_string(i) + "_WRITE_REG").c_str());
        }

        SC_THREAD(INIT_INS);
        // issue
        SC_THREAD(ISSUE_ACTION);
        sensitive << clk.pos();
        // opc
        SC_THREAD(OPC_FIFO);
        sensitive << clk.pos();
        SC_THREAD(OPC_FETCH);
        sensitive << clk.pos();
        SC_THREAD(OPC_EMIT);
        sensitive << clk.pos();
        // regfile
        SC_THREAD(READ_REG);
        // exec
        SC_THREAD(SALU_IN);
        sensitive << clk.pos();
        SC_THREAD(SALU_CALC);
        sensitive << clk.pos();
        SC_THREAD(SALU_CTRL);

        SC_THREAD(VALU_IN);
        sensitive << clk.pos();
        SC_THREAD(VALU_CALC);
        sensitive << clk.pos();
        SC_THREAD(VALU_CTRL);

        SC_THREAD(VFPU_IN);
        sensitive << clk.pos();
        SC_THREAD(VFPU_CALC);
        sensitive << clk.pos();
        SC_THREAD(VFPU_CTRL);

        SC_THREAD(LSU_IN);
        sensitive << clk.pos();
        SC_THREAD(LSU_CALC);
        sensitive << clk.pos();
        SC_THREAD(LSU_CTRL);

        SC_THREAD(CSR_IN);
        sensitive << clk.pos();
        SC_THREAD(CSR_CALC);
        sensitive << clk.pos();
        SC_THREAD(CSR_CTRL);

        SC_THREAD(MUL_IN);
        sensitive << clk.pos();
        SC_THREAD(MUL_CALC);
        sensitive << clk.pos();
        SC_THREAD(MUL_CTRL);

        SC_THREAD(SFU_IN);
        sensitive << clk.pos();
        SC_THREAD(SFU_CALC);
        sensitive << clk.pos();
        SC_THREAD(SFU_CTRL);

        // writeback
        SC_THREAD(WRITE_BACK);
        sensitive << clk.pos();
    }

public:
    std::map<OP_TYPE, decodedat> decode_table;
    std::vector<instable_t> instable_vec;
    /*** SIMT frontend ***/
    std::array<WARP_BONE, num_warp> WARPS;

    /*** SIMD backend ***/
    // issue
    sc_event_and_list ev_issue_list;
    sc_signal<I_TYPE> issue_ins{"issue_ins"};
    sc_signal<int> issueins_warpid;
    sc_signal<int> last_dispatch_warpid{"last_dispatch_warpid"}; // 需要设为sc_signal，否则ISSUE_ACTION对i的循环边界【i < last_dispatch_warpid + num_warp】会变化
    sc_signal<bool> dispatch_valid{"dispatch_valid"};
    // opc
    sc_signal<int> last_emit_entryid{"last_emit_entryid"};
    sc_event ev_opc_pop, ev_regfile_readdata,
        ev_opc_judge_emit, ev_opc_store, ev_opc_collect;
    StaticEntry<opcfifo_t, OPCFIFO_SIZE> opcfifo; // tlm::tlm_fifo<I_TYPE> opcfifo;
    std::array<std::array<bool, 3>, OPCFIFO_SIZE> opc_valid;
    std::array<std::array<bool, 3>, OPCFIFO_SIZE> opc_ready;
    std::array<std::array<bank_t, 3>, OPCFIFO_SIZE> opc_srcaddr;
    std::array<std::array<bool, 3>, OPCFIFO_SIZE> opc_banktype; // 0-s, 1-v
    std::array<int, BANK_NUM> read_bank_addr;                   // regfile arbiter给出
    std::array<int, BANK_NUM> REGcurrentIdx;                    // OPC轮询到哪了
    std::array<std::array<reg_t, num_thread>, BANK_NUM> read_data;
    std::array<std::pair<int, int>, BANK_NUM> REGselectIdx; // 轮询选出哪个了（索引，有这个数据，ready其实没有用了）
    sc_signal<int> emit_idx{"emit_idx"};                    // 上一周期emit的ins在opc中的索引，最大是BANK_NUM
    sc_signal<bool> opc_full{"opc_full"};
    bool opc_empty;
    sc_signal<I_TYPE> emit_ins;
    sc_signal<int> emitins_warpid;
    sc_signal<int> opcfifo_elem_num;
    bool findemit; // 轮询时，找到了全ready且执行单元也ready的entry
    sc_signal<bool> doemit;
    // regfile
    sc_signal<int> rds1_addr{"rds1_addr"}, rdv1_addr{"rdv1_addr"};
    sc_signal<reg_t> rds1_data{"rds1_data"};
    sc_vector<sc_signal<reg_t>> rdv1_data{"rdv1_data", num_thread};

    // exec
    sc_signal<bool> emito_salu{"emito_salu"};
    sc_signal<reg_t> tosalu_data1{"tosalu_data1"}, tosalu_data2{"tosalu_data2"}, tosalu_data3{"tosalu_data3"}; // OPC TO SALU
    bool salu_ready;
    sc_signal<bool> salu_ready_old{"salu_ready_old"};
    sc_event_queue salu_eqa, salu_eqb; // 分别负责a time和b time，最后一个是SALU_IN的，优先级比eqb低
    sc_event salu_eva, salu_evb, salu_unready, salu_nothinghappen,
        ev_salufifo_pushed, ev_saluready_updated;
    std::queue<salu_in_t> salu_dq;
    StaticQueue<salu_out_t, 3> salufifo;
    salu_out_t salutop_dat;
    bool salufifo_empty, salufifo_push;
    int salufifo_elem_num;
    salu_in_t salutmp1;
    salu_out_t salutmp2;
    sc_signal<bool> salueqa_triggered, salueqb_triggered; // 例如eqa_triggered，仅在eqa被触发时，delta 0变为1，delta 1给SALU_IN看，同时又变回0

    sc_signal<bool> emito_valu{"emito_valu"};
    sc_vector<sc_signal<reg_t>> tovalu_data1{"tovalu_data1", num_thread}, // OPC TO VALU
        tovalu_data2{"tovalu_data2", num_thread};
    bool valu_ready;
    sc_signal<bool> valu_ready_old{"Valu_ready_old"};
    sc_event_queue valu_eqa, valu_eqb;
    sc_event valu_eva, valu_evb, valu_unready, valu_nothinghappen,
        ev_valufifo_pushed, ev_valuready_updated;
    std::queue<valu_in_t> valu_dq;
    StaticQueue<valu_out_t, 3> valufifo;
    valu_out_t valutop_dat;
    bool valufifo_empty, valufifo_push;
    int valufifo_elem_num;
    sc_signal<bool> valueqa_triggered, valueqb_triggered;

    sc_signal<bool> emito_vfpu{"emito_vfpu"};
    sc_vector<sc_signal<int32_t>> tovfpu_data1{"tovfpu_data1", num_thread}, // OPC TO VFPU
        tovfpu_data2{"tovfpu_data2", num_thread}, tovfpu_data3{"tovfpu_data3", num_thread};
    bool vfpu_ready;
    sc_signal<bool> vfpu_ready_old{"vfpu_ready_old"};
    sc_event_queue vfpu_eqa, vfpu_eqb;
    sc_event vfpu_eva, vfpu_evb, vfpu_unready, vfpu_nothinghappen,
        ev_vfpufifo_pushed, ev_vfpuready_updated;
    std::queue<vfpu_in_t> vfpu_dq;
    StaticQueue<vfpu_out_t, 3> vfpufifo;
    vfpu_out_t vfputop_dat;
    bool vfpufifo_empty, vfpufifo_push;
    int vfpufifo_elem_num;
    sc_signal<bool> vfpueqa_triggered, vfpueqb_triggered;

    sc_signal<bool> emito_lsu{"emito_lsu"};
    sc_vector<sc_signal<int32_t>> tolsu_data1{"emitolsu_data1", num_thread}, // OPC TO LSU
        tolsu_data2{"emitolsu_data2", num_thread};
    bool lsu_ready;
    sc_signal<bool> lsu_ready_old{"lsu_ready_old"};
    sc_event_queue lsu_eqa, lsu_eqb;
    sc_event lsu_eva, lsu_evb, lsu_unready, lsu_nothinghappen,
        ev_lsufifo_pushed, ev_lsuready_updated;
    std::queue<lsu_in_t> lsu_dq;
    StaticQueue<lsu_out_t, 10> lsufifo;
    lsu_out_t lsutop_dat;
    bool lsufifo_empty;
    int lsufifo_elem_num;
    sc_signal<bool> lsueqa_triggered, lsueqb_triggered;

    sc_signal<bool> emito_simtstk{"emito_simtstk"};                    // 对应join，由于wait_bran的存在，这两个不会同时为1
    sc_signal<bool, SC_MANY_WRITERS> valuto_simtstk{"valuto_simtstk"}; // 对应beq类，由于wait_bran的存在，这两个不会同时为1
    simtstack_t simtstk_newelem;                                       // from VALU to SIMT-stack
    sc_signal<int> simtstk_new_warpid{"simtstk_new_warpid"};           // newelem对应的warp
    sc_signal<sc_bv<num_thread>> branch_elsemask{"branch_elsemask"};   // VALU计算出的elsemask，将发给SIMT-stack
    sc_signal<sc_bv<num_thread>> branch_ifmask{"branch_ifmask"};       // 与elsemask相反
    sc_signal<int> branch_elsepc{"branch_elsepc"};                     // VALU处理分支跳转的else分支pc
    sc_signal<I_TYPE> vbranch_ins{"vbranch_ins"};
    sc_signal<int> vbranchins_warpid{"vbranchins_warpid"};

    sc_signal<bool> emito_csr{"emito_csr"};
    sc_signal<int32_t> tocsr_data1{"tocsr_data1"}, tocsr_data2{"tocsr_data2"}; // OPC TO CSR
    bool csr_ready;
    sc_signal<bool> csr_ready_old{"csr_ready_old"};
    sc_event_queue csr_eqa, csr_eqb;
    sc_event csr_eva, csr_evb, csr_unready, csr_nothinghappen,
        ev_csrfifo_pushed, ev_csrready_updated;
    std::queue<csr_in_t> csr_dq;
    StaticQueue<csr_out_t, 10> csrfifo;
    csr_out_t csrtop_dat;
    bool csrfifo_empty;
    int csrfifo_elem_num;
    sc_signal<bool> csreqa_triggered, csreqb_triggered;

    sc_signal<bool> emito_mul{"emito_mul"};
    sc_vector<sc_signal<reg_t>> tomul_data1{"tomul_data1", num_thread},
        tomul_data2{"tomul_data2", num_thread};
    bool mul_ready;
    sc_signal<bool> mul_ready_old{"mul_ready_old"};
    sc_event_queue mul_eqa, mul_eqb;
    sc_event mul_eva, mul_evb, mul_unready, mul_nothinghappen,
        ev_mulfifo_pushed, ev_mulready_updated;
    std::queue<mul_in_t> mul_dq;
    StaticQueue<mul_out_t, 3> mulfifo;
    mul_out_t multop_dat;
    bool mulfifo_empty, mulfifo_push;
    int mulfifo_elem_num;
    sc_signal<bool> muleqa_triggered, muleqb_triggered;

    sc_signal<bool> emito_sfu{"emito_sfu"};
    sc_vector<sc_signal<reg_t>> tosfu_data1{"tosfu_data1", num_thread},
        tosfu_data2{"tosfu_data2", num_thread};
    bool sfu_ready;
    sc_signal<bool> sfu_ready_old{"sfu_ready_old"};
    sc_event_queue sfu_eqa, sfu_eqb;
    sc_event sfu_eva, sfu_evb, sfu_unready, sfu_nothinghappen,
        ev_sfufifo_pushed, ev_sfuready_updated;
    std::queue<sfu_in_t> sfu_dq;
    StaticQueue<sfu_out_t, 3> sfufifo;
    sfu_out_t sfutop_dat;
    bool sfufifo_empty, sfufifo_push;
    int sfufifo_elem_num;
    sc_signal<bool> sfueqa_triggered, sfueqb_triggered;
    
    // writeback
    sc_signal<bool> write_s, write_v, write_f;
    sc_signal<bool> execpop_salu, execpop_valu, execpop_vfpu, execpop_lsu;
    sc_signal<I_TYPE> wb_ins{"wb_ins"};
    sc_signal<int> wb_warpid{"wb_warpid"};
    sc_signal<bool> wb_ena{"wb_ena"};

    // debug，没实际用处
    sc_signal<bool> dispatch_ready;

    // 外部存储，暂时在BASE中实现
    std::array<reg_t, 81920> external_mem;
    std::array<I_TYPE, ireg_size> ireg;
    std::array<int, 8192> ins_mem;
    std::vector<std::vector<uint8_t>> *buffer_data;

    std::array<int, 32> testCSR;
    meta_data mtd;

    // 命令行参数
    std::string inssrc; // 指令的来源，ireg为I_TYPE，imem为32bit二进制
    std::string metafile;
    std::string datafile;

    int num_warp_activated;
};

#endif
