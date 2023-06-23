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


//简化的模型：仅有CTA_buffer以及allocator
//allocator根据SM的当前可用资源信息为CTA分配SM
//CTA_buffer接收来自host的CTA
//并根据allocator的结果发送CTA给SM（表现的行为是唤醒SM的warp）

//有个问题：mtd好像是kernel的数据，单个cta的资源使用情况从哪获得？
//另外需要SM返回cta执行完的信息，然后再给它分配cta


void CTA_Scheduler::CTA_buffer_recv_CTA()
{
    //实际上host是逐个发送cta给cta buffer的，但是要关心的是cta的分配方式
    //因此假设一个简单的情况作为模拟的起点：host已经将一个grid的cta全部放入cta buffer
    int num_CTA = mtd.kernel_size[0] * mtd.kernel_size[1] * mtd.kernel_size[2];
    cout << "host发送CTA给CTA_Scheduler...  CTA的数量 = " << num_CTA << endl;
    cta_buffer_cta_available = num_CTA;
    //while(true)
    //{
        ev_buffer_not_empty.notify(); //即时通知，只通知一个sctime
        cout << "buffer_not_empty" << endl;

        //初始化,暂时放在这里
        ev_sm_available.notify();
    //}
}

void CTA_Scheduler::allocator()
{
    while(true)
    {
        next:
        wait(ev_buffer_not_empty & ev_sm_available);
        for(int i = 0; i < NUM_SM; i++)
        {
            if(mtd.sgprUsage <= sm_group[i]->sgpr_available && mtd.vgprUsage <= sm_group[i]->vgpr_available && mtd.ldsSize <= sm_group[i]->lds_available && mtd.wg_size <= sm_group[i]->warp_available)
            {
                alloc_smid.write(i);
                sm_groups_working[i] = 1;
                if(! &sm_groups_working){
                    ev_sm_available.notify();
                }
                else{
                    ev_sm_available.cancel();
                }
                ev_alloc_finish.notify();
                cout << "allocator分配成功    分配的SM ID = " << i << endl;
                goto next; //分配结束进入wait状态
            }
        }
        alloc_smid.write(-1);
        ev_alloc_finish.notify();
        cout << "allocator分配失败    未找到满足资源要求的SM" << endl;
    }
}

void CTA_Scheduler::CTA_buffer_send_CTA()
{
    int smid = -1;
    while(true)
    {
        wait(ev_alloc_finish);
        smid = alloc_smid.read();

        if(smid != -1)
        {
            cta_buffer_cta_available -= 1;
            if(cta_buffer_cta_available = 0){
                ev_buffer_not_empty.cancel();
            }
            sm_group[smid]->warp_available -= mtd.wg_size; 
            sm_group[smid]->sgpr_available -= mtd.sgprUsage;
            sm_group[smid]->vgpr_available -= mtd.vgprUsage;
            sm_group[smid]->lds_available -= mtd.ldsSize;
            
            // 激活smid的对应数量的warp
            int cnt = sm_group[smid]->num_warp_activated;
            for(int i = cnt; i < cnt + mtd.wg_size; i++)
            {
                sm_group[smid]->WARPS[i].is_warp_activated = true;
                sm_group[smid]->WARPS[i].CSR_reg[1] = mtd.wg_size;
                sm_group[smid]->WARPS[i].CSR_reg[2] = num_thread;

                sm_group[smid]->WARPS[i].CSR_reg[4] = 0;
                sm_group[smid]->WARPS[i].CSR_reg[5] = i;

                sm_group[smid]->WARPS[i].CSR_reg[8] = 0;
                sm_group[smid]->WARPS[i].CSR_reg[9] = 0;
                sm_group[smid]->WARPS[i].CSR_reg[10] = 0;
                cout << "SM( " << smid << " )的warp( " << i <<" )被激活" <<endl;
            }
            sm_group[smid]->num_warp_activated += mtd.wg_size;
        }
        else{
            cout << "不执行发送CTA的任务..." << endl;
        }
        ev_alloc_finish.cancel();
    }
}

//增加SM执行完warp的逻辑。。。。。