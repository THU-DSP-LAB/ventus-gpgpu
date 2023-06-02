import os


def generate_meta_file(decimals, file_path):
    with open(file_path, "w") as f:
        for decimal in decimals:
            hex_value = hex(decimal)[2:].zfill(16)  # 转换为16进制，并填充0到16个字符
            hex_parts = [hex_value[i : i + 8] for i in range(0, 16, 8)]  # 拆分为两个32位16进制值
            f.write(hex_parts[0] + "\n")  # 写入第一行
            f.write(hex_parts[1] + "\n")  # 写入第二行


# 示例数据
kernel_id = 0
kernel_size = [1, 1, 1]
wf_size = 8
wg_size = 4
metaDataBaseAddr = 0
ldsSize = 0
pdsSize = 0
sgprUsage = 0
vgprUsage = 0
pdsBaseAddr = 0
num_buffer = 8
buffer_base = [0] * num_buffer
buffer_size = [0] * num_buffer
decimals = (
    [kernel_id]
    + kernel_size
    + [
        wf_size,
        wg_size,
        metaDataBaseAddr,
        ldsSize,
        pdsSize,
        sgprUsage,
        vgprUsage,
        pdsBaseAddr,
        num_buffer,
    ]
    + buffer_base
    + buffer_size
)

current_dir = os.path.dirname(__file__)

# 拼接生成文件的完整路径
file1_path = os.path.join(current_dir, "test.meta")

# 生成meta文件
generate_meta_file(decimals, file1_path)
