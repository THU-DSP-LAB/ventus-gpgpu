import random
import os

num_lines = 81920  # 生成的数据行数

current_dir = os.path.dirname(__file__)
dec_file = os.path.join(current_dir, "data_dec.txt")
hex_file = os.path.join(current_dir, "data_hex.txt")


with open(dec_file, "w") as f_dec, open(hex_file, "w") as f_hex:
    for i in range(num_lines):
        # 生成一个随机的32位整数
        random_num = random.randint(5000, 2**13 - 1)

        # 将十进制数写入dec.txt
        f_dec.write(str(random_num) + "\n")

        # 将十六进制数写入hex.txt
        f_hex.write(hex(random_num)[2:] + "\n")  # [2:] 是为了去掉前面的 '0x'
