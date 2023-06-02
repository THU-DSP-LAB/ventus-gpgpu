#ifndef CTA_SCHEDULER_H_
#define CTA_SCHEDULER_H_

#include <iostream>
#include <fstream>
#include <vector>
#include "BASE.h"

class CTA_Scheduler : public sc_core::sc_module
{
public:
    CTA_Scheduler();
    CTA_Scheduler(sc_core::sc_module_name name, std::string _filename) : sc_module(name), metafilename(_filename) {}
    void CTA_INIT();

public:
    sc_event ev_activate_warp;

    void readHexFile(const std::string &filename, int itemSize, std::vector<uint64_t> &items);
    void assignMetadata(const std::vector<uint64_t> &metadata, meta_data &mtd);
    void activate_warp();

    BASE *sm_group;
    meta_data mtd;

private:
    bool isHexCharacter(char c);
    int charToHex(char c);
    void freeMetadata(meta_data &mtd);
    std::vector<uint64_t> metadata;
    std::string metafilename;
    
};

#endif