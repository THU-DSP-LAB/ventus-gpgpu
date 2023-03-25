###### File hierarchy

```shell
|-- imports
    `-- GPGPU_axi_adapter.v
|-- project_gpgpu.tcl
|-- project_gpgpu_def_val.txt
|-- project_gpgpu_dump.txt
|-- readme.md
`-- scrs
|   |-- bd
|   |   `-- config_mb_wrapper.v
|   |-- constrs
|   |   `-- toplevel.xdc
|   |-- driver
|       |-- expample_vid_sum.vmem
|       |-- naive_driver.c
|       |-- naiver_driver.h
|       `-- single_read.data  
|   `-- gpgpu_fpga_test
|       `-- GPGPU_axi_adapter.v
```

`project_gpgpu_dump.txt`is a file containing a dump of the current values of all objects in the design, and `project_ gpgpu_def_val.txt`is a file indicating what the default values are for all objects in the design. These files can be used to aide in debugging issues with the recreated project.

Folder `scrs` includes block design warpper code `config_mb_wrapper.v`,  constraint file of vivado project `toplevel.xdc` and driver code written with C++  running in SDK, which are in folder `driver`. The files `example_vid_sum.vmem` and `single_read.data` are instruction file and data file of C++ program, respectively.

###### How to establish vivado project

1. Copy file `GPGPU_axi_top.v` that you generated from chisel code to folder `scrs\gpgpg_fpga_test` and `imports`

2. Open vivado

3. Switch to the folder above

4. In vivado Tcl command line, excute:

```
source project_gpgpu.tcl
```

This will take a long time to generate the project and bitstream.

4. Program deivce

5. Export hardware to SDK and launch SDK

6. Create a project and import files in folder driver

7. Run program on FPGA board
