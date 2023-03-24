#ifndef _PARAMETERS_H
#define _PARAMETERS_H

#include "systemc.h"
#include "tlm.h"
#include "tlm_core/tlm_1/tlm_req_rsp/tlm_channels/tlm_fifo/tlm_fifo.h"
#include <set>
#include <queue>
#include <math.h>
#include <iostream>

inline constexpr int num_warp = 4;
inline constexpr int depth_warp = 2; //
inline constexpr int xLen = 32;
inline constexpr long unsigned int num_thread = 8;
inline constexpr int ireg_bitsize = 9;
inline constexpr int ireg_size = 1 << ireg_bitsize;
inline constexpr int INS_LENGTH = 32; // the length of per instruction
inline constexpr double PERIOD = 10;
inline constexpr int IFIFO_SIZE = 10;
inline constexpr int OPCFIFO_SIZE = 4;
inline constexpr int BANK_NUM = 4;

using reg_t = sc_int<32>;

enum OP_TYPE
{ // start at 1, so 0 is invalid op
    INVALID_ = 0,
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
    int s1 = -1;        // load指令为寄存器addr
    int s2 = -1;        // load指令为offset
    int d = -1;         // beq指令为imm
    int op;             // trace不支持enum，只能定义op为int型
    int jump_addr = -1; // 分支指令才有用
    bool willwb;        // 是否写回寄存器
    I_TYPE(){};
    I_TYPE(OP_TYPE _op, int _s1, int _s2, int _d) : op(_op), s1(_s1), s2(_s2), d(_d){};
    I_TYPE(I_TYPE _ins, int _jump_addr) : op(_ins.op), s1(_ins.s1), s2(_ins.s2), d(_ins.d), jump_addr(_jump_addr){};
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
        jump_addr = rhs.jump_addr;
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
        sc_trace(tf, v.jump_addr, NAME + ".jump_addr");
    }
};
// typename I_TYPE sc_uint<INS_LENGTH>;

class event_if : virtual public sc_interface // "if" means interface
{
public:
    virtual const sc_event &obtain_event() const = 0;
    virtual void notify() = 0;
    virtual void notify(const double time_) = 0;
};
class event : public sc_module, public event_if
{
public:
    event(sc_module_name _name) : sc_module(_name) {}
    const sc_event &obtain_event() const { return self_event; }
    void notify() { self_event.notify(); }
    void notify(const double time_) { self_event.notify(time_, SC_NS); }

private:
    sc_event self_event;
};

enum REG_TYPE
{
    s = 1,
    v,
    f
};
class SCORE_TYPE // every score in scoreboard
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
    friend ostream &operator<<(ostream &os, SCORE_TYPE const &v)
    {
        os << "(regtype:" << v.regtype << ",addr:" << v.addr << ")";
        return os;
    }
    SCORE_TYPE(REG_TYPE regtype_, int addr_) : regtype(regtype_), addr(addr_){};
};
struct bank_t
{
    int bank_id;
    int addr;
};
struct opcfifo_t
{
    // 进入opcfifo时，若要取操作数，令valid=1，等待regfile返回ready=1
    // 若是立即数，不用取操作数，令valid=0且直接令ready=1
    // 只要ready=1，就可以发射
    I_TYPE ins;
    std::array<bool, 4> ready = {0};
    std::array<bool, 4> valid = {0};
    std::array<bank_t, 4> srcaddr;
    std::array<bool, 4> banktype = {0};
    // int mask;
    std::array<std::array<reg_t, num_thread>, 4> data;
    bool all_ready()
    {
        return ready[0] && ready[1] && ready[2] && ready[3];
    }
    opcfifo_t(){};
    opcfifo_t(I_TYPE ins_) : ins(ins_){};
    opcfifo_t(I_TYPE ins_, const std::array<bool, 4> &ready_arr,
              const std::array<bool, 4> &valid_arr,
              const std::array<bank_t, 4> &srcaddr_arr,
              const std::array<bool, 4> &banktype_arr)
        : ins(ins_), ready(ready_arr), valid(valid_arr),
          srcaddr(srcaddr_arr), banktype(banktype_arr){};
};

template <typename T, size_t N>
class StaticEntry
{ // for OPC entry
public:
    StaticEntry() : size_(0)
    {
        tag_.fill(false);
    }
    void clear()
    {
        tag_.fill(false);
    }
    void push(const T &value)
    {
        if (size_ < N)
        {
            for (size_t i = 0; i < N; ++i)
            {
                if (tag_[i] == false)
                {
                    data_[i] = value;
                    tag_[i] = true;
                    ++size_;
                    break;
                }
            }
        }
    }

    void pop(size_t index)
    {
        if (index >= N || !tag_[index])
        {
            throw std::out_of_range("Invalid index");
            return;
        }

        tag_[index] = false;
        --size_;
    }

    T &operator[](size_t index)
    {
        return data_[index];
    }

    const T &operator[](size_t index) const
    {
        return data_[index];
    }
    bool tag_valid(size_t index) const
    {
        return tag_[index];
    }
    size_t get_size() const
    {
        return size_;
    }

    // 以下函数是为了使用范围-based for循环
    T *begin() { return data_.begin(); }
    T *end() { return data_.end(); }
    const T *begin() const { return data_.begin(); }
    const T *end() const { return data_.end(); }

private:
    std::array<T, N> data_;
    std::array<bool, N> tag_; // 标志位置是否有效
    size_t size_;
};

template <typename T, std::size_t capacity>
class StaticQueue
{
private:
    std::array<T, capacity> data;
    std::size_t size;
    std::size_t front_index;

public:
    StaticQueue() : size(0), front_index(0) {}
    void push(const T &value)
    {
        if (size == capacity)
        {
            throw std::out_of_range("Queue is full");
        }
        data[(front_index + size) % capacity] = value;
        ++size;
    }
    void pop()
    {
        if (size == 0)
        {
            throw std::out_of_range("Queue is empty");
        }
        front_index = (front_index + 1) % capacity;
        --size;
    }
    void clear()
    {
        while (!isempty())
            pop();
    }
    T get()
    { // return front and pop
        if (size == 0)
        {
            throw std::out_of_range("Queue is empty");
        }
        T re = data[front_index];
        front_index = (front_index + 1) % capacity;
        --size;
        return re;
    }
    T &front()
    {
        if (size == 0)
        {
            throw std::out_of_range("Queue is empty");
        }
        return data[front_index];
    }
    const T &front() const
    {
        if (size == 0)
        {
            throw std::out_of_range("Queue is empty");
        }
        return data[front_index];
    }
    T &operator[](std::size_t index)
    { // front对应索引0
        return data[(front_index + index) % capacity];
    }
    const T &operator[](std::size_t index) const
    {
        return data[(front_index + index) % capacity];
    }
    T &at(size_t index) // 与[]不同，at()包含边界检查
    {
        if (index >= size)
        {
            throw std::out_of_range("Index out of range");
        }
        return data[(front_index + index) % capacity];
    }
    const T &at(size_t index) const
    {
        if (index >= size)
        {
            throw std::out_of_range("Index out of range");
        }
        return data[(front_index + index) % capacity];
    }
    bool isempty() const
    {
        return size == 0;
    }
    bool isfull() const
    {
        return size == capacity;
    }
    size_t used() const
    {
        return size;
    }
    size_t get_capacity() const
    {
        return capacity;
    }
};

struct salu_in_t
{
    I_TYPE ins;
    reg_t rss1_data;
    reg_t rss2_data;
};
struct salu_out_t
{
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
struct valu_in_t
{
    I_TYPE ins;
    std::array<reg_t, num_thread> rsv1_data, rsv2_data;
    reg_t rss2_data;
};
struct valu_out_t
{
    I_TYPE ins;
    std::array<reg_t, num_thread> rdv1_data;
    bool operator==(const valu_out_t &rhs) const
    {
        return rhs.ins == ins && rhs.rdv1_data == rdv1_data;
    }
    valu_out_t &operator=(const valu_out_t &rhs)
    {
        ins = rhs.ins;
        rdv1_data = rhs.rdv1_data;
        return *this;
    }
    friend ostream &operator<<(ostream &os, valu_out_t const &v)
    {
        os << "{" << v.ins << ";";
        auto it = v.rdv1_data.begin();
        while (it != v.rdv1_data.end())
        {
            os << *it << " ";
            it = std::next(it);
        }
        os << "}";
        return os;
    }
    friend void sc_trace(sc_trace_file *tf, const valu_out_t &v, const std::string &NAME)
    {
        sc_trace(tf, v.ins, NAME + ".ins");
        sc_trace(tf, v.rdv1_data[0], NAME + ".rdv1_data(0)");
        sc_trace(tf, v.rdv1_data[1], NAME + ".rdv1_data(1)");
        sc_trace(tf, v.rdv1_data[2], NAME + ".rdv1_data(2)");
        sc_trace(tf, v.rdv1_data[3], NAME + ".rdv1_data(3)");
        sc_trace(tf, v.rdv1_data[4], NAME + ".rdv1_data(4)");
        sc_trace(tf, v.rdv1_data[5], NAME + ".rdv1_data(5)");
        sc_trace(tf, v.rdv1_data[6], NAME + ".rdv1_data(6)");
        sc_trace(tf, v.rdv1_data[7], NAME + ".rdv1_data(7)");
    }
};
struct vfpu_in_t
{
    I_TYPE ins;
    std::array<float, num_thread> rsf1_data, rsf2_data;
};
struct vfpu_out_t
{
    I_TYPE ins;
    std::array<float, num_thread> rdf1_data;
    bool operator==(const vfpu_out_t &rhs) const
    {
        return rhs.ins == ins && rhs.rdf1_data == rdf1_data;
    }
    vfpu_out_t &operator=(const vfpu_out_t &rhs)
    {
        ins = rhs.ins;
        rdf1_data = rhs.rdf1_data;
        return *this;
    }
    friend ostream &operator<<(ostream &os, vfpu_out_t const &v)
    {
        os << "{" << v.ins << ";";
        auto it = v.rdf1_data.begin();
        while (it != v.rdf1_data.end())
        {
            os << *it << " ";
            it = std::next(it);
        }
        os << "}";
        return os;
    }
    friend void sc_trace(sc_trace_file *tf, const vfpu_out_t &v, const std::string &NAME)
    {
        sc_trace(tf, v.ins, NAME + ".ins");
        sc_trace(tf, v.rdf1_data[0], NAME + ".rdf1_data(0)");
        sc_trace(tf, v.rdf1_data[1], NAME + ".rdf1_data(1)");
        sc_trace(tf, v.rdf1_data[2], NAME + ".rdf1_data(2)");
        sc_trace(tf, v.rdf1_data[3], NAME + ".rdf1_data(3)");
        sc_trace(tf, v.rdf1_data[4], NAME + ".rdf1_data(4)");
        sc_trace(tf, v.rdf1_data[5], NAME + ".rdf1_data(5)");
        sc_trace(tf, v.rdf1_data[6], NAME + ".rdf1_data(6)");
        sc_trace(tf, v.rdf1_data[7], NAME + ".rdf1_data(7)");
    }
};
struct lsu_in_t
{
    I_TYPE ins;
    reg_t rss1_data; // 从regfile取出的数据作为momory地址
    // below 3 data is to store
    reg_t rds1_data;
    std::array<reg_t, num_thread> rdv1_data;
    std::array<float, num_thread> rdf1_data;
};
struct lsu_out_t
{
    I_TYPE ins;
    reg_t rds1_data;
    std::array<reg_t, num_thread> rdv1_data;
    std::array<float, num_thread> rdf1_data;
    bool operator==(const lsu_out_t &rhs) const
    {
        return rhs.ins == ins && rhs.rds1_data == rds1_data &&
               rhs.rdv1_data == rdv1_data && rhs.rdf1_data == rdf1_data;
    }
    lsu_out_t &operator=(const lsu_out_t &rhs)
    {
        ins = rhs.ins;
        rds1_data = rhs.rds1_data;
        rdv1_data = rhs.rdv1_data;
        rdf1_data = rhs.rdf1_data;
        return *this;
    }
    friend ostream &operator<<(ostream &os, lsu_out_t const &v)
    {
        os << "{" << v.ins << ";" << v.rds1_data << ";";
        auto it = v.rdv1_data.begin();
        while (it != v.rdv1_data.end())
        {
            os << *it << " ";
            it = std::next(it);
        }
        os << ";";
        auto itf = v.rdf1_data.begin();
        while (itf != v.rdf1_data.end())
        {
            os << *itf << " ";
            itf = std::next(itf);
        }
        os << "}";
        return os;
    }
    friend void sc_trace(sc_trace_file *tf, const lsu_out_t &v, const std::string &NAME)
    {
        sc_trace(tf, v.ins, NAME + ".ins");
        sc_trace(tf, v.rds1_data, NAME + ".rds1_data");
        sc_trace(tf, v.rdv1_data[0], NAME + ".rdv1_data(0)");
        sc_trace(tf, v.rdv1_data[1], NAME + ".rdv1_data(1)");
        sc_trace(tf, v.rdv1_data[2], NAME + ".rdv1_data(2)");
        sc_trace(tf, v.rdv1_data[3], NAME + ".rdv1_data(3)");
        sc_trace(tf, v.rdv1_data[4], NAME + ".rdv1_data(4)");
        sc_trace(tf, v.rdv1_data[5], NAME + ".rdv1_data(5)");
        sc_trace(tf, v.rdv1_data[6], NAME + ".rdv1_data(6)");
        sc_trace(tf, v.rdv1_data[7], NAME + ".rdv1_data(7)");
        sc_trace(tf, v.rdf1_data[0], NAME + ".rdf1_data(0)");
        sc_trace(tf, v.rdf1_data[1], NAME + ".rdf1_data(1)");
        sc_trace(tf, v.rdf1_data[2], NAME + ".rdf1_data(2)");
        sc_trace(tf, v.rdf1_data[3], NAME + ".rdf1_data(3)");
        sc_trace(tf, v.rdf1_data[4], NAME + ".rdf1_data(4)");
        sc_trace(tf, v.rdf1_data[5], NAME + ".rdf1_data(5)");
        sc_trace(tf, v.rdf1_data[6], NAME + ".rdf1_data(6)");
        sc_trace(tf, v.rdf1_data[7], NAME + ".rdf1_data(7)");
    }
};

#endif