#ifndef CTA_SCHEDULER_H_
#define CTA_SCHEDULER_H_

#include <iostream>
#include <fstream>
#include <vector>
#include "BASE.h"

class CTA_Scheduler : public sc_core::sc_module
{
public:

    SC_HAS_PROCESS(CTA_Scheduler);

    CTA_Scheduler();
    CTA_Scheduler(sc_core::sc_module_name name, std::string _filename) : sc_module(name), metafilename(_filename)
    {   
        
        SC_THREAD(CTA_buffer_recv_CTA);
        SC_THREAD(allocator);
        SC_THREAD(CTA_buffer_send_CTA);
        
    }
    void CTA_INIT();

    //new
    void CTA_buffer_recv_CTA();
    void CTA_buffer_send_CTA();
    void allocator();

public:
    const int cta_buffer_size_total = 32;  //cta buffer的最大容量
    int cta_buffer_cta_available = 0;  //host存到buffer里的cta数量
    int sm_groups_working[NUM_SM] = {0};

public:
    sc_signal<int> alloc_smid;


public:
    sc_event ev_activate_warp;

    //new
    sc_event ev_alloc_finish;
    sc_event ev_buffer_not_empty;
    sc_event ev_sm_available; //存在空闲状态的sm

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