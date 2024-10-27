vecadd.metadata内容如下：

```c++
struct meta_data{  // 这个metadata是供驱动使用的，而不是给硬件的
    uint64_t start_addr;    // 指令起始地址
    uint64_t kernel_id;
    uint64_t kernel_size[3];///> 每个kernel的workgroup三维数目
    uint64_t wf_size; ///> 每个warp的thread数目
    uint64_t wg_size; ///> 每个workgroup的warp数目
    uint64_t metaDataBaseAddr;///> CSR_KNL的值，
    uint64_t ldsSize;///> 每个workgroup使用的local memory(shared memory)的大小
    uint64_t pdsSize;///> 每个thread用到的private memory大小
    uint64_t sgprUsage;///> 每个warp使用的标量寄存器数目
    uint64_t vgprUsage;///> 每个warp使用的向量寄存器数目
    uint64_t pdsBaseAddr;///> private memory的基址，要转成每个workgroup的基地址， wf_size*wg_size*pdsSize
    uint64_t num_buffer; ///> buffer的数目，包括指令buffer、privatemem
    uint64_t buffer_base[num_buffer];//各buffer的基址。第一块buffer是给硬件用的metadata
    uint64_t buffer_size[num_buffer];//各buffer的size，以Bytes为单位
    uint64_t buffer_allocsize[num_buffer];//各buffer的size，以Bytes为单位
};

// CSR是每个workgroup一个，所以pdsBaseAddr转换成CSR_PDS，每个workgroup有自己的pdsbaseaddr（分配block时计算偏移）。
// 硬件保证每个线程访问privatemem映射到相应地址。用专用指令访问。
// allocSize是整个kernel的privatemem的大小。matadd例子因为只有一个workgroup所以其大小等于wf_size*wg_size*pdsSize
// privatemem和globalmem是一个层级的，需要用L2cache访问

// localmem是SM内共享的空间，每个workgroup的CSR的CSR_LDS在block分配时加上偏移量。编译器保证访存时加上偏移。

// SM内寄存器总数固定，每次分配block占用一定的寄存器

```

在spike启动任务时，会同时写入一个"vecadd.data"文件，格式如下：

```txt
1233fedc   //按照每行4Bytes的16进制方式，按顺序放入所有buffer的内容
c3434cca
```

对CPUtest.scala这个模块，控制信号按下面的顺序产生：

```scala
  io.host2cta.bits.host_wg_id:=   Cat( i ,0.U(CU_ID_WIDTH.W)), for i in 0 until kernel_size[0]*[1]*[2]
  io.host2cta.bits.host_num_wf:= wg_size
  io.host2cta.bits.host_wf_size:=  wf_size
  io.host2cta.bits.host_start_pc:=   0.U // 
  io.host2cta.bits.host_vgpr_size_total:= wg_size * vgprUsage
  io.host2cta.bits.host_sgpr_size_total:= wg_size * sgprUsage
  io.host2cta.bits.host_lds_size_total:= ldsSize
  io.host2cta.bits.host_gds_size_total:= 0.U //这个信号存疑。pdsSize * wf_size * wg_size
  io.host2cta.bits.host_vgpr_size_per_wf:= vgprUsage
  io.host2cta.bits.host_sgpr_size_per_wf:= sgprUsage
  io.host2cta.bits.host_gds_baseaddr := 0.U
  io.host2cta.bits.host_pds_baseaddr := i * pdsSize  * wf_size * wg_size， for i in 0 until kernel_size[0]*[1]*[2]
  io.host2cta.bits.host_csr_knl:= metaDataBaseAddr
  io.host2cta.bits.host_kernel_size_3d:= kernel_size[i][j][k]  //遍历即可
```
