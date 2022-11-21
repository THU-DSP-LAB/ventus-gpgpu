#!/bin/bash

if [ "$#" -lt 2 ]; then
    echo "Usage:"
    echo "chisel-to-systemc-tb.sh verilog_module target_dir [-t]"
    echo "If -t is specified, a testbench skeleton will also be generated."
    exit
fi

get_abs_filename() {
  # $1 : relative filename
  echo "$(cd "$(dirname "$1")" && pwd)/$(basename "$1")"
}

WORK_DIR="/tmp/verilog-to-sc"
MODULE_NAME=$1
TARGET_DIR=$(get_abs_filename $2)
VERILATOR_DIR="$VERILATOR_ROOT/include"
TOOL_DIR=$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )
SRC_DIR=$(pwd)

cd $SRC_DIR
mkdir -p $WORK_DIR
mkdir -p $TARGET_DIR
echo "Invoking Chisel to generate Verilog..."
sbt "run --targetDir $WORK_DIR --backend v"

echo "Converting Verilog module $1 into SystemC at $2..."
cd $SRC_DIR
verilator --sc $MODULE_NAME.v +define+SYNTHESIS+1 -Wno-lint
cp -f obj_dir/*.cpp $TARGET_DIR/
cp -f obj_dir/*.h $TARGET_DIR/
cp -f $VERILATOR_DIR/verilated.cpp $TARGET_DIR

if [ "$3" = "-t" ]; then
  echo "Creating SystemC testbench skeleton for module V$MODULE_NAME..."
  cd $TOOL_DIR
  cp -f testbench-template/Makefile $TARGET_DIR
  cp -f testbench-template/*.h $TARGET_DIR
  ./build-sc-tb.py $SRC_DIR/obj_dir/V$MODULE_NAME.h > $TARGET_DIR/main.cpp
fi

cd $SRC_DIR
rm -rf $WORK_DIR

echo "Done!"

