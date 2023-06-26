#ifndef CTA_SCHEDULER_H_
#define CTA_SCHEDULER_H_

#include <iostream>
#include <fstream>
#include <vector>
#include <queue>
#include "BASE.h"

class CTA_Scheduler : public sc_core::sc_module
{
public:

    SC_HAS_PROCESS(CTA_Scheduler);

    CTA_Scheduler();

    CTA_Scheduler(sc_core::sc_module_name name, std::string _filename) : sc_module(name), metafilename(_filename)
    {      
        SC_THREAD(host_send_CTA);
        SC_THREAD(CTA_queue_recv_CTA);
        SC_THREAD(allocator);
        SC_THREAD(CTA_queue_send_CTA);
    }

    void CTA_INIT();

    void host_send_CTA();
    void CTA_queue_recv_CTA();
    void allocator();
    void CTA_queue_send_CTA();

    int has_free_sm(int sm_group_working[], int NUM_SM);
    
public:
    struct CTA_INFO  //还需要添加更多信息
    {
        int id;
        int num_warp;
        int lds_size;
        int sgpr_size;
        int vgpr_size;
    };

    const int queue_size_total = 16;  //cta queue的最大容量
    int queue_num_cta = 0;  //当前queue中cta的数量
    int sm_group_working[NUM_SM] = {0}; //如果sm在工作中，标记为1，否则为0

    sc_event ev_queue_has_cta; //queue中有cta
    sc_event ev_sm_available; //有cta在空闲状态
    sc_event ev_alloc_finish; //allocator分配结束
    sc_event ev_queue_ready; //queue准备好接收来自host的cta
    sc_event ev_host_has_cta; //host有cta待发送到queue
    sc_event ev_send_finish; //queue中发送cta到sm
    sc_event ev_enq_finish; //queue接收cta完成
    sc_event ev_activate_warp; //激活warp

    sc_signal<int> sig_cta_id;
    sc_signal<int> sig_cta_num_warp;
    sc_signal<int> sig_cta_lds_size;
    sc_signal<int> sig_cta_sgpr_size;
    sc_signal<int> sig_cta_vgpr_size;
    sc_signal<int> sig_alloc_smid;

    std::queue<CTA_INFO> cta_queue; //保存来自host的cta

public:
    void readHexFile(const std::string &filename, int itemSize, std::vector<uint64_t> &items);
    void assignMetadata(const std::vector<uint64_t> &metadata, meta_data &mtd);
    void activate_warp();

    BASE **sm_group;
    meta_data mtd;

private:
    bool isHexCharacter(char c);
    int charToHex(char c);
    void freeMetadata(meta_data &mtd);
    std::vector<uint64_t> metadata;
    std::string metafilename;
    
};

#endif