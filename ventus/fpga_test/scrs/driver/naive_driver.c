#include "naive_driver.h"

int main(){
    Gpu TestGpu;
    // TODO: Set GPU_BASEADDR in .h file
    GpuInit(&TestGpu, GPU_BASEADDR);
    TaskMemory TestMem;
    GpuTaskMemoryInit(&TestMem, 128, 1024);
    TaskConfig TestTask = {
        0,   // WgId
        0,   // NumWf
        0,   // WfSize
        0,   // StartPC
        0,   // VgprSizeTotal
        0,   // SgprSizeTotal
        0,   // LdsSize
        0,   // VgprSizePerWf
        0,   // SgprSizePerWf
        TestMem // Mem
    };

    int retry = 5;
    u32 WarpGroupID;
    while(retry){
        if(GpuSendTask(&TestGpu, &TestTask) == XST_SUCCESS)
            break;
        retry--;
    }
    if(!retry){
        xil_printf("All tries failed. Stopped.\r\n");
        return XST_FAILURE;
    }
    GpuWatchTask(&TestGpu, &WarpGroupID, 1024);

    // TODO: Result Checking
    
    // End of Checking
    GpuDeleteTask(&TestTask);
    return XST_SUCCESS;
}