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
    //CTA_Scheduler::activate_warp();

    ev_queue_ready.notify(10,SC_NS);
    ev_sm_available.notify(10,SC_NS);
    ev_enq_finish.notify(10,SC_NS);
    ev_send_finish.notify(10,SC_NS);
}

int CTA_Scheduler::has_free_sm(int sm_group_working[], int NUM_SM)
{
    for(int i = 0; i < NUM_SM; i++)
        {
        if(sm_group_working[i] == 0)
        {
            return 1;
        }
    }
    return 0;
}

//allocator根据SM的当前可用资源信息为CTA分配SM
//CTA queue接收来自host的CTA
//并根据allocator的结果发送CTA给SM（表现的行为是唤醒SM的warp）

//另外需要SM返回cta执行完的信息，然后再给它分配cta


//行为是：有kernel启动时，读入metadate，逐个发送CTA给CTA queue
void CTA_Scheduler::host_send_CTA()
{
    //kernel启动
    //kernel信息读入metadata
    //获得cta数
    //逐个发送cta给cta queue

    int num_CTA = mtd.kernel_size[0] * mtd.kernel_size[1] * mtd.kernel_size[2];
    int cta_cnt = 0;
    std::cout << "host发送CTA给CTA_Scheduler...  CTA的数量 = " << num_CTA << std::endl;
    while(true)
    {
        wait(1,SC_NS);
        wait(ev_queue_ready & ev_enq_finish);

        if(num_CTA-- > 0)
        {
            //发送一个cta的信息给cta queue
            std::cout<<"=======  host准备好了一个cta  cta id="<< cta_cnt <<std::endl;
            sig_cta_id.write(cta_cnt);
            cta_cnt++;
            sig_cta_num_warp.write(mtd.wg_size);
            sig_cta_lds_size.write(mtd.ldsSize);
            sig_cta_sgpr_size.write(mtd.sgprUsage);
            sig_cta_vgpr_size.write(mtd.vgprUsage);
          
            ev_host_has_cta.notify(1,SC_NS);
            std::cout<<"当前共有"<<num_CTA+1<<"个cta等待发送"<<std::endl;
        }
        else
        {
            ev_host_has_cta.cancel();
            std::cout<<"host将cta发送完毕"<<std::endl;
        }
    }
}

void CTA_Scheduler::CTA_queue_recv_CTA()
{
    while(true)
    {
        wait(1,SC_NS);
        wait(ev_host_has_cta);

        CTA_INFO cta;
        cta.id = sig_cta_id.read();
        std::cout<<"queue收到cta,id="<< cta.id << std::endl;
        cta.num_warp = sig_cta_num_warp.read();
        cta.lds_size = sig_cta_lds_size.read();
        cta.sgpr_size = sig_cta_sgpr_size.read();
        cta.vgpr_size = sig_cta_vgpr_size.read();

        cta_queue.push(cta);
        ev_enq_finish.notify(1,SC_NS);

        queue_num_cta += 1;
        std::cout <<"queue中存入CTA,queue的长度="<< cta_queue.size() << std::endl;
        if(cta_queue.size() == queue_size_total)
        {
            ev_queue_ready.cancel();
            ev_queue_has_cta.notify(1,SC_NS);
            std::cout<<"queue已满"<<std::endl;
        }
        else if(cta_queue.size() == 0)
        {
            ev_queue_ready.notify(1,SC_NS);
            ev_queue_has_cta.cancel();
            std::cout<<"queue空了"<<std::endl;
        }
        else
        {
            ev_queue_ready.notify(1,SC_NS);
            ev_queue_has_cta.notify(1,SC_NS);
            std::cout<<"queue没满"<<std::endl;
        }
        
    }
    
}

void CTA_Scheduler::allocator() 
{
    int alloc_result = 0;
    while(true)
    {
        wait(1,SC_NS);
        wait(ev_queue_has_cta & ev_sm_available & ev_send_finish);
        CTA_INFO cta = cta_queue.front();
        for(int i = 0; i < NUM_SM; i++)
        {
            if(sm_group_working[i]==0 && cta.lds_size <= sm_group[i]->lds_available && cta.sgpr_size <= sm_group[i]->sgpr_available && cta.vgpr_size <= sm_group[i]->vgpr_available && cta.num_warp <= sm_group[i]->warp_available)
            { 
                sig_alloc_smid.write(i);
                ev_alloc_finish.notify(1,SC_NS);
                std::cout << "allocator为cta "<<cta.id<<" 分配的SM ID = " << i << std::endl;
                alloc_result = 1;
                break;
            }
        }
        if(alloc_result == 0)
        {
            sig_alloc_smid.write(-1);
            ev_alloc_finish.notify(1,SC_NS);
            std::cout << "allocator分配失败    未找到满足资源要求的SM" << std::endl;
        }
    }
}

//现在是一次性把全部warp给激活。待完善...逐个发送warp
void CTA_Scheduler::CTA_queue_send_CTA() {
    int smid = -1;
    while(true)
    {
        wait(1,SC_NS);
        wait(ev_alloc_finish);
        smid = sig_alloc_smid.read();

        CTA_INFO cta = cta_queue.front();
        if(smid != -1)
        {
            sm_group_working[smid] = 1;
            for(int i=0;i<NUM_SM;i++)
            {
              std::cout<<"sm"<<i<<"使用情况="<<sm_group_working[i]<<std::endl;
            }
            
            if(has_free_sm(sm_group_working,NUM_SM)){
                ev_sm_available.notify(1,SC_NS);
                std::cout << "存在空闲SM" << std::endl;
            }
            else{
                ev_sm_available.cancel();
                std::cout << "SM全部在工作中" << std::endl;
            }
            
            cta_queue.pop();
            queue_num_cta -= 1;
            std::cout<<"queue中还有"<<queue_num_cta<<"个cta"<<std::endl;
            if(queue_num_cta == 0){
                ev_queue_has_cta.cancel();
            }
            sm_group[smid]->lds_available -= cta.lds_size; 
            sm_group[smid]->warp_available -= cta.num_warp;
            sm_group[smid]->sgpr_available -= cta.sgpr_size;
            sm_group[smid]->vgpr_available -= cta.vgpr_size;

            // 激活smid的对应数量的warp
            // 这里你可以完善一下...
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
        else
        {
            std::cout << "不执行发送CTA" << std::endl;
        }
        ev_alloc_finish.cancel();
        ev_send_finish.notify(1,SC_NS);

        std::cout << "cta" <<cta.id<<"被发送到sm"<<smid<< std::endl;
    }
}

//增加SM执行完warp的逻辑.....
