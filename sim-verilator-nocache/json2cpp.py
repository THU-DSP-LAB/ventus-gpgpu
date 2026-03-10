import json
import sys

# 函数：将不同类型的值转换为 int 类型
def to_int(value):
    if isinstance(value, bool):
        return 1 if value else 0
    if isinstance(value, int):
        return value
    if isinstance(value, str):
        try:
            return int(value)
        except ValueError:
            return None  # 如果字符串不能转换为数字，返回None
    return None  # 默认返回None

# 函数：将 JSON 数据转换为 C++ 代码
def json_to_cpp(json_data, skip_keys=set()):
    cpp_code = """
#include <cstdint>
#include <string>
#include <unordered_map>

extern const std::unordered_map<std::string, uint32_t> rtl_parameters;
const std::unordered_map<std::string, uint32_t> rtl_parameters = {
"""
    for key, value in json_data.items():
        if isinstance(value, (bool, int, str)):  # 只处理 bool, int, str 类型
            cpp_value = to_int(value)
            if cpp_value is None:
                if key not in skip_keys:
                    print(f"Warning: parameters.json value for key '{key}' cannot be converted to int. Skipping.")
                continue
            cpp_code += f'    {{ "{key}", {cpp_value} }},\n'

    cpp_code += """
};
"""
    return cpp_code

if __name__ == "__main__":
    skip_keys = set("l2cache_cache l2cache_micro l2cache_micro_l l2cache_params l2cache_params_l".split())

    json_file = sys.argv[1] if len(sys.argv) > 1 else 'parameters.json'
    cpp_file = sys.argv[2] if len(sys.argv) > 2 else 'rtl_parameters.cpp'

    with open(json_file, 'r') as f:
        data = json.load(f)

    cpp_code = json_to_cpp(data, skip_keys=skip_keys)

    with open(cpp_file, 'w') as f:
        f.write(cpp_code)
