#ifndef _PARAMETERS_H
#define _PARAMETERS_H

#include "systemc.h"
#include <math.h>

extern const int num_warp = 4;
extern constexpr int depth_warp = 2; //
extern const int xLen = 32;
extern const int num_thread = 8;
extern const int ireg_bitsize = 9;
extern constexpr int ireg_size = 1 << ireg_bitsize;
extern const int INS_LENGTH = 32; // the length of per instruction

class I_TYPE // type of per instruction
{
public:
    int op, s1, s2, d;
    I_TYPE(){};
    I_TYPE(int _op, int _s1, int _s2, int _d) : op(_op), s1(_s1), s2(_s2), d(_d){};
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
    friend void sc_trace(sc_trace_file *tf, const I_TYPE &v, const std::string &NAME){
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

#endif