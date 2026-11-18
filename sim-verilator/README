# Ventus GPGPU verilator simulation environment

## Prerequisites

Required:
- [Verilator](https://verilator.org/guide/latest/install.html) version 5.024 or later. (You may want to install [mold](https://github.com/rui314/mold) first)

Optional:
- [mold](https://github.com/rui314/mold) for accelerating verilator model linking. If you want to use mold, install it before compiling verilator.

## Usage

Basic usage:

```bash
make
make run
```

This will compile the verilator model as `obj_dir/Vdut` and run it.

You can change the default testcase configuration using cmdline arguments, try `./obj_dir/Vdut --help` for more information.

It is suggested to use `-f <file>` option to specify the testcase file, for example:
```bash
./obj_dir/Vdut -f ventus_cmdargs.txt
```

Some testcases and corresponding `ventus_cmdargs.txt` files are provided in the `testcase` directory.

`ventus_cmdargs.txt` and its corresponding testcase need to be placed in the same directory.     
Symbolic link (eg. `ln -s matadd/ventus_cmdargs.txt ..`) is supported.