# 导入必要的库
import re
import os
from tabulate import tabulate

# 定义解析规则的正则表达式
pattern = r"^(\w+)\s*->\s*List\(([\w\s\.\,]+)\)"

current_dir = os.path.dirname(__file__)
txtfile = os.path.join(current_dir, "init_decodetable.txt")

# 打开要读取的文件
with open(txtfile, "r") as f:
    # 消除每行开头空行
    lines = [line.strip() for line in f if line.strip()]
    # 创建输出文件

cppfile = os.path.join(current_dir, "init_decodetable.cpp")
supportfile = os.path.join(current_dir, "supported_instructions.txt")

opcodes = []
data = []

with open(cppfile, "w") as out_file:
    # 逐行读取输入文件内容
    out_file.write(
        '#include "BASE.h"\nvoid BASE::INIT_DECODETABLE(){\ndecode_table = {\n'
    )
    for line in lines:
        # 使用正则表达式匹配当前行的内容
        match = re.match(pattern, line)
        if match:
            # 获取指令名称
            opcode = match.group(1)
            # print("opcode =", opcode)
            # 将指令名称中的“->”替换为下划线
            opcode = opcode.replace("->", "_")
            opcodes.append(opcode)
            # 获取指令参数列表
            params = match.group(2).split(",")
            # 删除每一项的所有空格
            for i in range(len(params)):
                params[i] = params[i].strip().replace(" ", "")
            # 对每个参数进行处理
            processed_params = []
            reduced_params = []
            for param in params:
                # 如果参数为N或Y，则将其转换为0或1
                if param == "N":
                    processed_params.append("0")
                    reduced_params.append("0")
                elif param == "Y":
                    processed_params.append("1")
                    reduced_params.append("1")
                else:
                    # 如果参数包含“.”，则将“.”转换为下划线
                    if "." in param:
                        param = param.replace(".", "_")
                    reduced_params.append(param)
                    # 在参数前添加“DecodeParams::”
                    param = "DecodeParams::" + param
                    processed_params.append(param)
            data.append({"opcode": opcode, "params": reduced_params})
            # 将处理后的参数列表转换为C++代码形式
            code_params = ", ".join(processed_params)
            # 将结果写入输出文件
            out_file.write("{{{0}_, {{{1}}}}}, \n".format(opcode, code_params))
    out_file.write("};\n}")

opcodes.sort()
with open(supportfile, "w") as supportinsf:
    supportinsf.write(",\n".join(opcodes))

headers = [
    "EXEC",
    "op",
    "isvec",
    "fp",
    "barrier",
    "bran",
    "simtSTK",
    "simtop",
    "csr",
    "rever",
    "selalu3",
    "selalu2",
    "selalu1",
    "selimm",
    "memwhb",
    "alufn",
    "mul",
    "memcmd",
    "mem_un",
    "fence",
    "sfu",
    "wvd",
    "rdmask",
    "wrtmask",
    "wxd",
    "tc",
    "dismask",
    "X",
    "X",
]
# 构建表格数据
table_data = []


class decodedat:
    def __init__(
        self,
        isvec,
        fp,
        barrier,
        branch,
        simt_stack,
        simt_stack_op,
        csr,
        reverse,
        sel_alu3,
        sel_alu2,
        sel_alu1,
        sel_imm,
        mem_whb,
        alu_fn,
        mul,
        mem_cmd,
        mem_unsigned,
        fence,
        sfu,
        wvd,
        readmask,
        writemask,
        wxd,
        tc,
        disable_mask,
        undefined1,
        undefined2,
    ):
        self.isvec = isvec
        self.fp = fp
        self.barrier = barrier
        self.branch = branch
        self.simt_stack = simt_stack
        self.simt_stack_op = simt_stack_op
        self.csr = csr
        self.reverse = reverse
        self.sel_alu3 = sel_alu3
        self.sel_alu2 = sel_alu2
        self.sel_alu1 = sel_alu1
        self.sel_imm = sel_imm
        self.mem_whb = mem_whb
        self.alu_fn = alu_fn
        self.mul = mul
        self.mem_cmd = mem_cmd
        self.mem_unsigned = mem_unsigned
        self.fence = fence
        self.sfu = sfu
        self.wvd = wvd
        self.readmask = readmask
        self.writemask = writemask
        self.wxd = wxd
        self.tc = tc
        self.disable_mask = disable_mask
        self.undefined1 = undefined1
        self.undefined2 = undefined2


for entry in data:
    # print(entry)
    old_params = entry["params"]
    myclass = decodedat(*old_params)
    if myclass.tc != "0":
        execunit = "TC"
    elif myclass.sfu != "0":
        execunit = "SFU"
    elif myclass.fp != "0":
        execunit = "VFPU"
    elif myclass.csr != "CSR_N":
        execunit = "CSR"
    elif myclass.mul != "0":
        execunit = "MUL"
    elif myclass.mem_cmd != "M_X":
        execunit = "LSU"
    elif myclass.isvec != "0":
        if entry["opcode"] == "JOIN":
            execunit = "SIMTSTK"
        else:
            execunit = "VALU"
    elif myclass.barrier != "0":
        execunit = "WPSCHEDLER"
    else:
        execunit = "SALU"
    entry["params"] = entry["params"] + [execunit]


def sort_key(element):
    params = element["params"]
    last_param = params[-1]  # 获取最后一个元素
    return (last_param, element["opcode"])  # 返回一个元组，用于比较排序


data = sorted(data, key=sort_key)
for entry in data:
    row = [entry["params"][-1]] + [entry["opcode"]] + entry["params"][0:-1]
    table_data.append(row)

# 使用tabulate生成Markdown表格
markdown_table = tabulate(table_data, headers, tablefmt="pipe")
# 保存Markdown表格到文件
with open(os.path.join(current_dir, "decodetable.md"), "w") as f:
    f.write(markdown_table)
