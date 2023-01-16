#ifndef _PARAMETERS_H
#define _PARAMETERS_H

#include "systemc.h"
#include "tlm.h"
#include "tlm_core/tlm_1/tlm_req_rsp/tlm_channels/tlm_fifo/tlm_fifo.h"
#include <set>
#include <queue>
#include <math.h>

inline constexpr int num_warp = 4;
inline constexpr int depth_warp = 2; //
inline constexpr int xLen = 32;
inline constexpr long unsigned int num_thread = 8;
inline constexpr int ireg_bitsize = 9;
inline constexpr int ireg_size = 1 << ireg_bitsize;
inline constexpr int INS_LENGTH = 32; // the length of per instruction
inline constexpr int PERIOD = 10;

using reg_t = sc_int<32>;

enum OP_TYPE
{
    lw_,
    add_,
    vload_,
    vaddvv_,
    vaddvx_,
    vfadd_,
    beq_
};
class I_TYPE // type of per instruction
{
public:
    int s1; // load指令为存储器addr
    int s2; // load指令为offset
    int d;  // beq指令为imm
    OP_TYPE op;
    I_TYPE(){};
    I_TYPE(OP_TYPE _op, int _s1, int _s2, int _d) : op(_op), s1(_s1), s2(_s2), d(_d){};
    bool operator==(const I_TYPE &rhs) const
    {
        return rhs.op == op && rhs.s1 == s1 && rhs.s2 == s2 && rhs.d == d;
    }
    I_TYPE &operator=(const I_TYPE &rhs)
    {
        op = rhs.op;
        s1 = rhs.s1;
        s2 = rhs.s2;
        d = rhs.d;
        return *this;
    }
    friend ostream &operator<<(ostream &os, I_TYPE const &v)
    {
        os << "(" << v.op << "," << v.s1 << "," << v.s2 << "," << v.d << ")";
        return os;
    }
    friend void sc_trace(sc_trace_file *tf, const I_TYPE &v, const std::string &NAME)
    {
        sc_trace(tf, v.op, NAME + ".op");
        sc_trace(tf, v.s1, NAME + ".s1");
        sc_trace(tf, v.s2, NAME + ".s2");
        sc_trace(tf, v.d, NAME + ".d");
    }
};
// typename I_TYPE sc_uint<INS_LENGTH>;

class event_if : virtual public sc_interface // "if" means interface
{
public:
    virtual const sc_event &obtain_event() const = 0;
    virtual void notify() = 0;
};
class event : public sc_module, public event_if
{
public:
    event(sc_module_name _name) : sc_module(_name) {}
    const sc_event &obtain_event() const { return self_event; }
    void notify() { self_event.notify(); }

private:
    sc_event self_event;
};

enum REG_TYPE
{
    s,
    v,
    f
};
class SCORE_TYPE
{
public:
    enum REG_TYPE regtype; // record to write scalar reg or vector reg
    int addr;
    bool operator<(const SCORE_TYPE &t_) const
    {
        if (regtype == t_.regtype)
            return addr < t_.addr;
        else
            return regtype < t_.regtype;
    }
    SCORE_TYPE(REG_TYPE regtype_, int addr_) : regtype(regtype_), addr(addr_){};
};

struct salu_out_t{
    I_TYPE ins;
    reg_t data;
        bool operator==(const salu_out_t &rhs) const
    {
        return rhs.ins == ins && rhs.data == data;
    }
    salu_out_t &operator=(const salu_out_t &rhs)
    {
        ins = rhs.ins;
        data = rhs.data;
        return *this;
    }
    friend ostream &operator<<(ostream &os, salu_out_t const &v)
    {
        os << "(" << v.ins << "," << v.data << ")";
        return os;
    }
    friend void sc_trace(sc_trace_file *tf, const salu_out_t &v, const std::string &NAME)
    {
        sc_trace(tf, v.ins, NAME + ".ins");
        sc_trace(tf, v.data, NAME + ".data");
    }
};

#endif