import os
import re

# 定义指令对应的操作码
OP_CODES = {
    "add": 0,
    "lw": 1,
    "vadd.vv": 2,
    "beq": 3,
    "vadd.vx": 4,
    "vle32.v": 5,
    "vfadd.vv": 6,
    "addi" : 7,
}

# 获取当前文件所在目录
current_dir = os.path.dirname(__file__)

# 拼接生成文件的完整路径
insfile_path = os.path.join(current_dir, "ins.txt")

# 打开指令文件
with open(insfile_path, "r") as f:
    instructions = f.readlines()

parent_dir = os.path.abspath(os.path.join(current_dir, ".."))
cpp_path = os.path.join(parent_dir, "sc-code", "sm", "init_ins.cpp")

# 打开C++函数文件
with open(cpp_path, "w") as f:
    # 写入函数头
    f.write('#include "BASE.h"\nvoid BASE::INIT_INS(int warp_id)\n{\n')
    index = 0
    # 写入指令解析和存储的代码
    for ins in instructions:
        parts = ins.strip().split()  # 分离指令的各个部分
        # print(ins, ":", parts[0])
        op = OP_CODES[parts[0]]  # 获取操作码
        op_str = parts[0]
        i_type = "I_TYPE("  # 初始化I_TYPE变量的代码
        if op == 0:  # add指令
            i_type += (
                "add_"
                + f", {int(parts[1][1:-1])}, {int(parts[2][1:-1])}, {int(parts[3][1:])}"
            )
        elif op == 1:  # lw指令
            m = re.search(r"(-?\d+)\((\w+)\)", parts[2])
            reg = m.group(2)
            # print(f"m offset: {offset}, reg: {reg}")
            offset = 0 if m is None else int(m.group(1))
            i_type += "lw_" + f", {int(parts[1][1:-1])}, {int(reg[1:])}, {offset}"
        elif op == 2:  # vadd.vv指令
            i_type += (
                "vaddvv_"
                + f", {int(parts[1][1:-1])}, {int(parts[2][1:-1])}, {int(parts[3][1:])}"
            )
        elif op == 3:  # beq指令
            i_type += (
                "beq_"
                + f", {int(parts[3])}, {int(parts[1][1:-1])}, {int(parts[2][1:-1])}"
            )
        elif op == 4:  # vadd.vx指令
            i_type += (
                "vaddvx_"
                + f", {int(parts[1][1:-1])}, {int(parts[2][1:-1])}, {int(parts[3][1:])}"
            )
        elif op == 5:  # vle32.v指令
            m = re.search(r"\((\D*)(\d*)\)", parts[2])
            rs1reg = m.group(2)
            i_type += "vle32v_" + f", {int(parts[1][1:-1])}, {int(rs1reg)}, -1"
        elif op == 6:  # vfadd.vv指令
            i_type += (
                "vfaddvv_"
                + f", {int(parts[1][1:-1])}, {int(parts[2][1:-1])}, {int(parts[3][1:])}"
            )
        i_type += ")"
        f.write(f"    WARPS[warp_id].ireg[{index}] = {i_type};\n")  # 将解析好的指令存入i_reg数组
        index = index + 1

    # 写入函数尾
    f.write("}\n")

# 输出成功信息
print("init.cpp文件已生成。")
