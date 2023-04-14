import random
import os

scalar_regs = ["x{}".format(i) for i in range(1, 31)]
vector_regs = ["v{}".format(i) for i in range(1, 31)]

insts = {
    "lw": {"rd": scalar_regs, "rs1": scalar_regs},
    "vle32.v": {"rd": vector_regs, "rs1": scalar_regs},  # vload
    "add": {"rd": scalar_regs, "rs1": scalar_regs, "rs2": scalar_regs},
    "vadd.vv": {"rd": vector_regs, "rs1": vector_regs, "rs2": vector_regs},
    "vadd.vx": {"rd": vector_regs, "rs1": vector_regs, "rs2": scalar_regs},
    "vfadd.vv": {"rd": vector_regs, "rs1": vector_regs, "rs2": vector_regs},
    "beq": {"rd": None, "rs1": scalar_regs, "rs2": scalar_regs},
    "addi": {"rd": scalar_regs, "rs1": scalar_regs},
}

# 获取当前文件所在目录和父目录
current_dir = os.path.dirname(__file__)
parent_dir = os.path.abspath(os.path.join(current_dir, ".."))

# 拼接生成文件的完整路径
file1_path = os.path.join(current_dir, "ins_random.s")
cpp_path = os.path.join(parent_dir, "sc-code", "sm", "init_ins.cpp")

# 打开文件并写入内容
with open(file1_path, "w") as f1, open(cpp_path, "w") as f3:
    f1.write(".equ CSR_NUMW,  0x801\n")
    f1.write(".equ CSR_NUMT,  0x802\n")
    f1.write(".equ CSR_TID,   0x800\n")
    f1.write(".equ CSR_WID,   0x805\n")
    f1.write(".equ CSR_GDS,   0x807\n")
    f1.write(".equ CSR_LDS,   0x806\n")
    f3.write('#include "BASE.h"\nvoid BASE::INIT_INS(int warp_id)\n{\n')

    f3_idx = 0
    for i in range(1, 32):
        inst_name = "addi"
        inst = insts[inst_name]
        rd = "x{}".format(i)
        rs1 = "x0"
        imm = random.randint(100, 200) + 8 * 128
        f1.write("{} {}, {}, {}\n".format(inst_name, rd, rs1, imm))
        i_type = "I_TYPE("
        i_type += "addi_" + f", {i}, 0, {imm}"
        i_type += ")"
        f3.write(f"    WARPS[warp_id].ireg[{f3_idx}] = {i_type};\n")
        f3_idx = f3_idx + 1
        f1.write("{} {}, {}, {}\n".format("add", rd, rd, rd))
        f1.write("{} {}, {}, {}\n".format("add", rd, rd, rd))
        i_type = "I_TYPE("
        i_type += "add_" + f", {i}, {i}, {i}"
        i_type += ")"
        f3.write(f"    WARPS[warp_id].ireg[{f3_idx}] = {i_type};\n")
        f3_idx = f3_idx + 1
        f3.write(f"    WARPS[warp_id].ireg[{f3_idx}] = {i_type};\n")
        f3_idx = f3_idx + 1

    for i in range(0, 32):
        inst_name = "vadd.vx"
        inst = insts[inst_name]
        rd = "v{}".format(i)
        rs1 = "v31"
        rs2 = "x{}".format(i)
        f1.write("{} {}, {}, {}\n".format(inst_name, rd, rs1, rs2))
        i_type = "I_TYPE("
        i_type += "vaddvx_" + f", {i}, 31, {i}"
        i_type += ")"
        f3.write(f"    WARPS[warp_id].ireg[{f3_idx}] = {i_type};\n")
        f3_idx = f3_idx + 1

    for i in range(200):
        inst_name = random.choice(list(insts.keys()))
        inst = insts[inst_name]

        # 检查是否有目标寄存器(rd)，如果没有则将其设为None
        if inst["rd"] is None:
            rd = None
        else:
            rd = random.choice(inst["rd"])
        if inst["rs1"] is None:
            rs1 = None
        else:
            rs1 = random.choice(inst["rs1"])
        if inst.get("rs2") is None:
            rs2 = None
        else:
            rs2 = random.choice(inst["rs2"])

        i_type = "I_TYPE("

        if inst_name == "lw":
            offset = 4 * random.randint(-10, 10)
            f1.write("{} {}, {}({})\n".format(inst_name, rd, offset, rs1))
            i_type += "lw_" + f", {int(rd[1:])}, {int(rs1[1:])}, {offset}"

        elif inst_name == "vle32.v":
            f1.write("{} {}, ({})\n".format(inst_name, rd, rs1))
            i_type += "vle32v_" + f", {int(rd[1:])}, {int(rs1[1:])}, -1"

        elif inst_name == "add":
            f1.write("{} {}, {}, {}\n".format(inst_name, rd, rs1, rs2))
            i_type += "add_" + f", {int(rd[1:])}, {int(rs1[1:])}, {int(rs2[1:])}"

        elif inst_name == "vadd.vv":
            f1.write("{} {}, {}, {}\n".format(inst_name, rd, rs1, rs2))
            i_type += "vaddvv_" + f", {int(rd[1:])}, {int(rs1[1:])}, {int(rs2[1:])}"

        elif inst_name == "vadd.vx":
            f1.write("{} {}, {}, {}\n".format(inst_name, rd, rs1, rs2))
            i_type += "vaddvx_" + f", {int(rd[1:])}, {int(rs1[1:])}, {int(rs2[1:])}"

        elif inst_name == "vfadd.vv":
            f1.write("{} {}, {}, {}\n".format(inst_name, rd, rs1, rs2))
            i_type += "vfaddvv_" + f", {int(rd[1:])}, {int(rs1[1:])}, {int(rs2[1:])}"

        elif inst_name == "beq":
            label = "label{}".format(i + 1)
            # jump = random.randint(-5, 5)
            f1.write("{} {}, {}, {}\n".format(inst_name, rs1, rs2, label))
            # # 随机生成一个跳转标签
            f1.write("{}:\n".format(label))
            i_type += "beq_" + f", 0, {int(rs1[1:])}, {int(rs2[1:])}"

        elif inst_name == "addi":
            imm = 4 * random.randint(-2, 2)
            f1.write("{} {}, {}, {}\n".format(inst_name, rd, rs1, imm))
            i_type += "addi_" + f", {int(rd[1:])}, {int(rs1[1:])}, {imm}"

        else:
            raise ValueError("Invalid instruction name")

        i_type += ")"
        f3.write(f"    WARPS[warp_id].ireg[{f3_idx}] = {i_type};\n")
        f3_idx = f3_idx + 1

    f3.write("}\n")
