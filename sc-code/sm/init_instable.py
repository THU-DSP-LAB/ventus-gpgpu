import re
import os

data_structure = []

# 获取当前文件所在目录和父目录
current_dir = os.path.dirname(__file__)
parent_dir = os.path.abspath(os.path.join(current_dir, ".."))

# 拼接生成文件的完整路径
file1_path = os.path.join(current_dir, "Instructions.scala")

# 打开文本文件
with open(file1_path, 'r') as file:
    # 逐行读取文件内容
    for line in file:
        # 使用正则表达式匹配行中的op和字符串
        match = re.match(r'\s*def\s+(\w+)\s+=\s+BitPat\("b([01?]+)"\)', line)
        if match:
            op = match.group(1)
            binary_string = match.group(2)

            masked_pattern = binary_string.replace('?', '0')
            mask = binary_string.replace('0', '1').replace('?', '0')

            data_structure.append((op + "_", masked_pattern, mask))

# # 输出数据结构
# for item in data_structure:
#     print(item)

# 生成所有指令用于定义enum OP_TYPE
allins_path = os.path.join(current_dir, "all_instructions.txt")

with open(allins_path, 'w') as allins_file:
    for item in data_structure:
        allins_file.write(item[0] + ",\n")


# 生成C++文件
cpp_path = os.path.join(current_dir, "init_instable.cpp")

with open(cpp_path, 'w') as cpp_file:
    cpp_file.write('#include <vector>\n')
    cpp_file.write('#include <unordered_map>\n')
    cpp_file.write('#include "BASE.h"\n\n')
    cpp_file.write('void BASE::INIT_INSTABLE()\n')
    cpp_file.write('{\n')

    # 根据 mask 进行分组
    groups = {}
    for item in data_structure:
        op = item[0]
        masked_pattern = item[1]
        mask = item[2]

        if mask in groups:
            groups[mask].append((masked_pattern, op))
        else:
            groups[mask] = [(masked_pattern, op)]

    # 生成初始化代码
    for key, values in groups.items():
        cpp_file.write(f'    std::unordered_map<std::bitset<32>, OP_TYPE> map_{key};\n')
        for value in values:
            masked_pattern = value[0]
            op = value[1]
            cpp_file.write(f'    map_{key}.insert(std::make_pair(std::bitset<32>("{masked_pattern}"), {op}));\n')
        cpp_file.write(f'    instable_vec.push_back(instable_t{{std::bitset<32>("{key}"), map_{key}}});\n')

    cpp_file.write('};\n')