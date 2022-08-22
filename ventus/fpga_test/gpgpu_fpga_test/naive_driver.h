#include "xil_types.h"
#include "xil_io.h"
#include "xparameters.h"
#include "xstatus.h"

#define GPU_BASEADDR		0x00000000
#define GPU_HIGHADDR		0x0000ffff
/*************PARAM OFFSETS**********************/
#define GPU_VALID_OFFSET 	            0x00        // host -> gpu
#define GPU_WG_ID_OFFSET    		    0x04
#define GPU_NUM_WF_OFFSET               0x08
#define GPU_WF_SIZE_OFFSET              0x0c
#define GPU_START_PC_OFFSET	            0x10
#define GPU_VGPR_SIZE_T_OFFSET          0x14
#define GPU_SGPR_SIZE_T_OFFSET          0x18
#define GPU_LDS_SIZE_OFFSET	            0x1c
//#define GPU_GDS_SIZE_OFFSET             0x20
#define GPU_DATA_BASEADDR_OFFSET        0x20        // dcache
#define GPU_VGPR_SIZE_WF_OFFSET         0x24
#define GPU_SGPR_SIZE_WF_OFFSET         0x28
#define GPU_PC_BASEADDR_OFFSET          0x2c        // icache

#define GPU_WG_ID_DONE_OFFSET           0x38
#define GPU_WG_VALID_OFFSET             0x3c
/*************CONSTANTS**************************/
#define GPU_SEND_TIMEOUT                8


#define Gpu_ReadReg(BaseAddress, RegOffset)             \
        Xil_In32((BaseAddress) + (u32)(RegOffset))
#define Gpu_WriteReg(BaseAddress, RegOffset, Data)      \
        Xil_Out32((BaseAddress) + (u32)(RegOffset), (u32)(Data))

#define GPU_ADDR_WIDTH              32

// typedef struct{
//     u32 DeviceId;
//     UINTPTR BaseAddress;    // physical
//     int AddrWidth;
//     Memory Mem;
// }Gpu_Config;

typedef struct{
    u32* Instr;
    u32* Data;
    int ISize;              // word
    int DSize;              // word
}TaskMemory;

typedef struct{
    UINTPTR BaseAddr;       // GPU Base Address
    int Initialized;
    int AddrWidth;
}Gpu;

typedef struct{
    u32 WgId;
    u32 NumWf;
    u32 WfSize;
    u32 StartPC;
    u32 VgprSizeTotal;
    u32 SgprSizeTotal;
    u32 LdsSize;
    u32 VgprSizePerWf;
    u32 SgprSizePerWf;

    TaskMemory Mem;
}TaskConfig;

u32 GpuInit(Gpu* GpuInstance, UINTPTR BaseAddr){
    GpuInstance->AddrWidth = 32;
    GpuInstance->BaseAddr = BaseAddr;
    GpuInstance->Initialized = 1;
    return XST_SUCCESS;
}

// init and load memory for task
// ISize DSize = IMem DMem size (in words)
u32 GpuTaskMemoryInit(TaskMemory* Mem, int ISize, int DSize){
    u32* Instr = (u32*)malloc(ISize * sizeof(u32));
    u32* Data = (u32*)malloc(DSize * sizeof(u32));
    if(Instr == NULL || Data == NULL){
        xil_printf("Failed to allocate memory.\r\n");
        return XST_FAILURE;
    }
    xil_printf("Instr: %08x[%d], Data: %08x[%d].\r\n", Instr, ISize, Data, DSize);
    memcpy(Instr, ProgramInstr, ISize * sizeof(u32));
    memcpy(Data, ProgramData, DSize * sizeof(u32));
    Mem->Instr = Instr;
    Mem->Data = Data;
    Mem->ISize = ISize;
    Mem->DSize = DSize;
    return XST_SUCCESS;
}

u32 GpuSendTask(Gpu* GpuInstance, TaskConfig* TaskCfg){
    int wait;

    Gpu_WriteReg(GpuInstance->BaseAddr, GPU_WG_ID_OFFSET, TaskCfg->WgId);
    Gpu_WriteReg(GpuInstance->BaseAddr, GPU_NUM_WF_OFFSET, TaskCfg->NumWf);
    Gpu_WriteReg(GpuInstance->BaseAddr, GPU_WF_SIZE_OFFSET, TaskCfg->WfSize);
    Gpu_WriteReg(GpuInstance->BaseAddr, GPU_START_PC_OFFSET, TaskCfg->StartPC);
    Gpu_WriteReg(GpuInstance->BaseAddr, GPU_VGPR_SIZE_T_OFFSET, TaskCfg->VgprSizeTotal);
    Gpu_WriteReg(GpuInstance->BaseAddr, GPU_SGPR_SIZE_T_OFFSET, TaskCfg->SgprSizeTotal);
    Gpu_WriteReg(GpuInstance->BaseAddr, GPU_LDS_SIZE_OFFSET, TaskCfg->LdsSize);
    Gpu_WriteReg(GpuInstance->BaseAddr, GPU_VGPR_SIZE_WF_OFFSET, TaskCfg->VgprSizePerWf);
    Gpu_WriteReg(GpuInstance->BaseAddr, GPU_SGPR_SIZE_WF_OFFSET, TaskCfg->SgprSizePerWf);

    Gpu_WriteReg(GpuInstance->BaseAddr, GPU_PC_BASEADDR_OFFSET, TaskCfg->Mem.Instr);
    Gpu_WriteReg(GpuInstance->BaseAddr, GPU_DATA_BASEADDR_OFFSET, TaskCfg->Mem.Data);

    Gpu_WriteReg(GpuInstance->BaseAddr, GPU_VALID_OFFSET, (u32)1);
    wait = GPU_SEND_TIMEOUT;
    while(wait){
        if(Gpu_ReadReg(GpuInstance->BaseAddr, GPU_VALID_OFFSET) == 0)
            break;
        wait--;
    }
    if(wait == 0){
        xil_printf("Sending WG#%d failed.\r\n", TaskCfg->WgId);
        return XST_FAILURE;
    }
    xil_printf("Sending WG#%d successfully.\r\n", TaskCfg->WgId);
    return XST_SUCCESS;
}

u32 GpuWatchTask(Gpu* GpuInstance, u32* WgIdReturn, int wait){
    while(wait){
        if(Gpu_ReadReg(GpuInstance->BaseAddr, GPU_WG_VALID_OFFSET) == 1)
            break;
        wait--;
    }
    if(wait == 0){
        xil_printf("Waiting time limit exceeded.\r\n");
        return XST_FAILURE;
    }
    *WgIdReturn = Gpu_ReadReg(GpuInstance->BaseAddr, GPU_WG_ID_DONE_OFFSET);
    xil_printf("Get response from WG#%d.\r\n", *WgIdReturn);
    Gpu_WriteReg(GpuInstance->BaseAddr, GPU_WG_VALID_OFFSET, (u32)0);
    return XST_SUCCESS;
}

// clear the memory
void GpuDeleteTask(TaskConfig* TaskCfg){
    TaskCfg->Mem.Instr = 0;
    TaskCfg->Mem.Data = 0;
    free(TaskCfg->Mem.Instr);
    free(TaskCfg->Mem.Data);
    return;
}

// All data here
static u32 ProgramInstr[1024] = {
    0
};
static u32 ProgramData[32768] = {
    0
};