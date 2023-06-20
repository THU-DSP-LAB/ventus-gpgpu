#include "CTA_Scheduler.hpp"

bool CTA_Scheduler::isHexCharacter(char c)
{
    return (c >= '0' && c <= '9') || (c >= 'A' && c <= 'F') || (c >= 'a' && c <= 'f');
}

int CTA_Scheduler::charToHex(char c)
{
    if (c >= '0' && c <= '9')
        return c - '0';
    else if (c >= 'A' && c <= 'F')
        return c - 'A' + 10;
    else if (c >= 'a' && c <= 'f')
        return c - 'a' + 10;
    else
        return -1; // Invalid character
}

void CTA_Scheduler::readHexFile(const std::string &filename, int itemSize, std::vector<uint64_t> &items)
{ // itemSize为每个数据的比特数，这里为64
    ifstream file(filename);

    if (!file)
    {
        cout << "Error opening file: " << filename << endl;
        return;
    }

    char c;
    int bits = 0;
    unsigned long long value = 0;
    bool leftside = false;

    while (file.get(c))
    {
        if (c == '\n')
        {
            if (bits != 0)
                leftside = true;
            continue;
        }

        if (!isHexCharacter(c))
        {
            cout << "Invalid character found: " << c << endl;
            continue;
        }

        int hexValue = charToHex(c);
        if (leftside)
            value = value | (hexValue << bits);
        else
            value = (value << 4) | hexValue;
        bits += 4;

        if (bits >= itemSize)
        {
            items.push_back(value);
            value = 0;
            bits = 0;
            leftside = false;
        }
    }

    if (bits > 0)
    {
        cout << "Warning: Incomplete item found at the end of the file!" << endl;
    }

    file.close();
}

void CTA_Scheduler::assignMetadata(const std::vector<uint64_t> &metadata, meta_data &mtd)
{
    int index = 0;

    mtd.kernel_id = metadata[index++];

    for (int i = 0; i < 3; i++)
    {
        mtd.kernel_size[i] = metadata[index++];
    }

    mtd.wf_size = metadata[index++];
    mtd.wg_size = metadata[index++];
    mtd.metaDataBaseAddr = metadata[index++];
    mtd.ldsSize = metadata[index++];
    mtd.pdsSize = metadata[index++];
    mtd.sgprUsage = metadata[index++];
    mtd.vgprUsage = metadata[index++];
    mtd.pdsBaseAddr = metadata[index++];

    mtd.num_buffer = metadata[index++];
    // cout << "CTA: num_buffer=" << std::hex << mtd.num_buffer << std::dec << "\n";

    mtd.buffer_base = new uint64_t[mtd.num_buffer];

    for (int i = 0; i < mtd.num_buffer; i++)
    {
        mtd.buffer_base[i] = metadata[index++];
    }

    mtd.buffer_size = new uint64_t[mtd.num_buffer];
    for (int i = 0; i < mtd.num_buffer; i++)
    {
        mtd.buffer_size[i] = metadata[index++];
    }
}

void CTA_Scheduler::freeMetadata(meta_data &mtd)
{
    delete[] mtd.buffer_base;
    delete[] mtd.buffer_size;
}

void CTA_Scheduler::activate_warp()
{

    SC_REPORT_INFO("CTA_Scheduler", "Activating warps...");

    // 处理metadata数据
    uint64_t knum_workgroup = mtd.kernel_size[0] * mtd.kernel_size[1] * mtd.kernel_size[2]; // k means kernel
    cout << "CTA: knum_workgroup=" << knum_workgroup << "\n";
    if (knum_workgroup > 2)
        cout << "CTA warning: currently not support so many workgroups\n";
    int warp_limit = num_warp;
    if (mtd.wg_size > warp_limit)
        cout << "CTA error: wg_size > warp_limit per SM\n";
    for (int i = 0; i < knum_workgroup; i++)
    {
        int warp_counter = 0;
        while (warp_counter < mtd.wg_size)
        {
            cout << "CTA: SM" << i << " warp" << warp_counter << " is activated\n";
            sm_group[i]->WARPS[warp_counter].is_warp_activated = true;
            sm_group[i]->WARPS[warp_counter].CSR_reg[1] = mtd.wg_size;
            sm_group[i]->WARPS[warp_counter].CSR_reg[2] = num_thread;

            sm_group[i]->WARPS[warp_counter].CSR_reg[4] = 0;
            sm_group[i]->WARPS[warp_counter].CSR_reg[5] = warp_counter;

            sm_group[i]->WARPS[warp_counter].CSR_reg[8] = 0;
            sm_group[i]->WARPS[warp_counter].CSR_reg[9] = 0;
            sm_group[i]->WARPS[warp_counter].CSR_reg[10] = 0;
            ++warp_counter;
        }
        sm_group[i]->num_warp_activated = warp_counter;
    }
}

void CTA_Scheduler::CTA_INIT()
{
    CTA_Scheduler::readHexFile(metafilename, 64, metadata);
    CTA_Scheduler::assignMetadata(metadata, mtd);
    CTA_Scheduler::activate_warp();
}
