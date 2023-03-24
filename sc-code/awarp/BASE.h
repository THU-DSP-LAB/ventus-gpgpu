#ifndef BASE_H_
#define BASE_H_

#include "../parameters.h"
#include "tlm.h"
#include "tlm_core/tlm_1/tlm_req_rsp/tlm_channels/tlm_fifo/tlm_fifo.h"

class BASE : public sc_core::sc_module
{
public:
    sc_in_clk clk{"clk"};
    sc_in<bool> rst_n{"rst_n"};

    void debug_sti();
    void debug_display();

    // fetch
    void INIT_INS();
    void PROGRAM_COUNTER();
    // void FETCH_2();
    void INSTRUCTION_REG();
    void DECODE();
    // ibuffer
    void IBUF_ACTION();
    void IBUF_PARAM();
    // scoreboard
    void JUDGE_DISPATCH();
    void UPDATE_SCORE();
    // issue
    void ISSUE_ACTION();
    // opc
    void OPC_FIFO();
    void OPC_FETCH();
    void OPC_EMIT();
    bank_t bank_decode(int warp_id, int srcaddr);
    // regfile
    void INIT_REG();
    std::pair<int, int> reg_arbiter(const std::array<std::array<bank_t, 4>, OPCFIFO_SIZE> &addr_arr, // opc_srcaddr
                                    const std::array<std::array<bool, 4>, OPCFIFO_SIZE> &valid_arr,  // opc_valid
                                    std::array<std::array<bool, 4>, OPCFIFO_SIZE> &ready_arr,        // opc_ready
                                    int bank_id,
                                    std::array<int, BANK_NUM> &REGcurrentIdx);
    void READ_REG();
    void WRITE_REG();
    // exec
    void SALU_IN();
    void SALU_OUT();
    void SALU_CTRL();
    void VALU_IN();
    void VALU_OUT();
    void VALU_CTRL();
    void VFPU_IN();
    void VFPU_OUT();
    void VFPU_CTRL();
    void LSU_IN();
    void LSU_OUT();
    void LSU_CTRL();
    // writeback
    void WRITE_BACK();

    // initialize
    void start_of_simulation()
    {
        pc = -1;
        ibuftop_ins = I_TYPE(INVALID_, 0, 0, 0);
        issue_ins = I_TYPE(INVALID_, 0, 0, 0);
    }

    BASE(sc_core::sc_module_name name)
        : sc_module(name),
          salufifo(3), valufifo(3), vfpufifo(3), lsufifo(3)
    // , opcfifo(10)
    {
        SC_HAS_PROCESS(BASE);

        SC_THREAD(debug_sti);
        SC_METHOD(debug_display);
        SC_METHOD(memory_init);

        // fetch
        SC_THREAD(INIT_INS);
        SC_THREAD(PROGRAM_COUNTER);
        sensitive << clk.pos() << rst_n.neg();
        SC_THREAD(INSTRUCTION_REG);
        SC_THREAD(DECODE);
        sensitive << clk.pos();
        // ibuffer
        SC_THREAD(IBUF_ACTION);
        sensitive << clk.pos() << rst_n.neg();
        SC_THREAD(IBUF_PARAM);
        // scoreboard
        SC_THREAD(JUDGE_DISPATCH);
        sensitive << clk.pos();
        SC_THREAD(UPDATE_SCORE);
        sensitive << clk.pos();
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
        SC_METHOD(INIT_REG);
        SC_THREAD(READ_REG);
        sensitive << clk.pos();
        SC_THREAD(WRITE_REG);
        sensitive << clk.pos();
        // exec
        SC_THREAD(SALU_IN);
        sensitive << clk.pos();
        SC_THREAD(SALU_OUT);
        SC_THREAD(SALU_CTRL);
        SC_THREAD(VALU_IN);
        sensitive << clk.pos();
        SC_THREAD(VALU_OUT);
        SC_THREAD(VALU_CTRL);
        SC_THREAD(VFPU_IN);
        sensitive << clk.pos();
        SC_THREAD(VFPU_OUT);
        SC_THREAD(VFPU_CTRL);
        SC_THREAD(LSU_IN);
        sensitive << clk.pos();
        SC_THREAD(LSU_OUT);
        SC_THREAD(LSU_CTRL);
        // writeback
        SC_THREAD(WRITE_BACK);
        sensitive << clk.pos();
    }

public:
    // fetch
    sc_event ev_fetchpc, ev_decode;
    sc_signal<bool> fetch_valid{"fetch_valid"}, ibuf_swallow{"ibuf_swallow"};
    sc_signal<bool> jump{"jump"}, branch_sig{"branch_sig"}; // 无论是否jump，只要发生了分支判断，将branch_sig置为1
    sc_signal<int> jump_addr{"jump_addr"}, pc{"pc"};
    I_TYPE fetch_ins;
    sc_signal<I_TYPE> decode_ins{"decode_ins"};
    std::array<I_TYPE, ireg_size> ireg;
    // ibuffer
    sc_event ev_ibuf_update, ev_ibuf_updated;
    sc_signal<bool> dispatch{"dispatch"}, ibuf_empty{"ibuf_empty"};
    sc_signal<I_TYPE> ibuftop_ins{"ibuftop_ins"};
    StaticQueue<I_TYPE, IFIFO_SIZE> ififo;
    sc_signal<int> ififo_elem_num;
    // scoreboard
    sc_event ev_judge_dispatch;
    sc_signal<bool> wb_ena{"wb_ena"}, can_dispatch{"can_dispatch"};
    sc_signal<I_TYPE> wb_ins{"wb_ins"};
    I_TYPE _scoretmpins;
    std::set<SCORE_TYPE> score; // record regfile addr that's to be written
    bool wait_bran;             // 应该使用C++类型；dispatch了分支指令，则要暂停dispatch等待分支指令被执行
    // issue
    sc_event ev_issue;
    sc_signal<I_TYPE> issue_ins{"issue_ins"};
    // opc
    StaticEntry<opcfifo_t, OPCFIFO_SIZE> opcfifo; // tlm::tlm_fifo<I_TYPE> opcfifo;
    std::array<std::array<bool, 4>, OPCFIFO_SIZE> opc_valid;
    std::array<std::array<bool, 4>, OPCFIFO_SIZE> opc_ready;
    std::array<std::array<bank_t, 4>, OPCFIFO_SIZE> opc_srcaddr;
    std::array<std::array<bool, 4>, OPCFIFO_SIZE> opc_banktype; // 0-s, 1-v
    std::array<int, BANK_NUM> read_bank_addr;                   // regfile arbiter给出
    std::array<int, BANK_NUM> REGcurrentIdx;                    // 轮询到哪了
    std::array<std::array<reg_t, num_thread>, BANK_NUM> read_data;
    std::array<std::pair<int, int>, BANK_NUM> REGselectIdx; // 轮询选出哪个了（索引，有这个数据，ready其实没有用了）
    sc_signal<int> emit_idx{"emit_idx"};                    // 上一周期emit的ins在opc中的索引，最大是BANK_NUM
    sc_signal<bool> opc_full{"opc_full"};
    bool opc_empty;
    I_TYPE opctop_ins;
    int opcfifo_elem_num;
    sc_signal<bool> salu_ready{"salu_ready"}, valu_ready{"valu_ready"}, vfpu_ready{"vfpu_ready"}, lsu_ready{"lsu_ready"};
    sc_signal<reg_t> rss1_data{"rss1_data"}, rss2_data{"rss2_data"};
    sc_vector<sc_signal<reg_t>> rsv1_data{"rsv1_data", num_thread},
        rsv2_data{"rsv2_data", num_thread};
    sc_vector<sc_signal<float>> rsf1_data{"rsf1_data", num_thread},
        rsf2_data{"rsf2_data", num_thread};
    sc_signal<sc_uint<5>> rss1_addr{"rss1_addr"}, rss2_addr{"rss2_addr"},
        rsv1_addr{"rsv1_addr"}, rsv2_addr{"rsv2_addr"},
        rsf1_addr{"rsf1_addr"}, rsf2_addr{"rsf2_addr"};
    bool findemit; // 轮询时，找到了全ready且执行单元也ready的entry
    bool emit, emito_salu, emito_valu, emito_vfpu, emito_lsu;
    // regfile
    std::array<reg_t, 32> s_regfile;
    using v_regfile_t = std::array<reg_t, num_thread>;
    std::array<v_regfile_t, 32> v_regfile;
    using f_regfile_t = std::array<float, num_thread>;
    sc_signal<sc_uint<5>> rds1_addr{"rds1_addr"}, rdv1_addr{"rdv1_addr"}, rdf1_addr{"rdf1_addr"};
    sc_signal<reg_t> rds1_data{"rds1_data"};
    sc_vector<sc_signal<reg_t>> rdv1_data{"rdv1_data", num_thread};
    sc_vector<sc_signal<float>> rdf1_data{"rdf1_data", num_thread};
    // exec
    sc_event_queue salu_eqa, salu_eqb; // 分别负责a time和b time，最后一个是SALU_IN的，优先级比eqb低
    sc_event salu_unready;
    std::queue<salu_in_t> salu_dq;
    tlm::tlm_fifo<salu_out_t> salufifo;
    salu_out_t salutop_dat;
    bool salufifo_empty;
    int salufifo_elem_num;
    sc_event_queue valu_eqa, valu_eqb;
    sc_event valu_unready;
    std::queue<valu_in_t> valu_dq;
    tlm::tlm_fifo<valu_out_t> valufifo;
    valu_out_t valutop_dat;
    bool valufifo_empty;
    int valufifo_elem_num;
    sc_event_queue vfpu_eqa, vfpu_eqb;
    sc_event vfpu_unready;
    std::queue<vfpu_in_t> vfpu_dq;
    tlm::tlm_fifo<vfpu_out_t> vfpufifo;
    vfpu_out_t vfputop_dat;
    bool vfpufifo_empty;
    int vfpufifo_elem_num;
    sc_event_queue lsu_eqa, lsu_eqb;
    sc_event lsu_unready;
    std::queue<lsu_in_t> lsu_dq;
    tlm::tlm_fifo<lsu_out_t> lsufifo;
    lsu_out_t lsutop_dat;
    bool lsufifo_empty;
    int lsufifo_elem_num;
    // writeback
    bool write_s, write_v, write_f;
    bool execpop_salu, execpop_valu, execpop_vfpu, execpop_lsu;

    // 外部存储，暂时在BASE中实现
    std::array<reg_t, 512> s_memory;
    std::array<v_regfile_t, 512> v_memory;
    std::array<f_regfile_t, 512> f_memory;
    void memory_init()
    {
        s_memory[25] = 34;
        s_memory[41] = 67;
        v_memory[20].fill(666);
        v_memory[41].fill(777);
    }
};

#endif
