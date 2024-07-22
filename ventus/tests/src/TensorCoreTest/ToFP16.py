import struct
import numpy as np


def getFP16Str(dec_float=5.9):
    # 十进制单精度浮点转16位16进制
    hexa = struct.unpack('H', struct.pack('e', dec_float))[0]
    hexa = hex(hexa)
    hexa = hexa[2:]
    # print(hexa) # 45e6
    return hexa


def Hex2FP16(hexa: str):
    y = struct.pack("H", int(hexa, 16))
    float = np.frombuffer(y, dtype=np.float16)[0]
    # print(float)  # 5.9
    return float


if __name__ == '__main__':
    std = 'c65a3a442a70c556'
    out = "c65c3a442a60c557"
    for i in range(4):
        print(Hex2FP16(std[i*4:(i+1)*4]),Hex2FP16(out[i*4:(i+1)*4]),Hex2FP16(std[i*4:(i+1)*4])-Hex2FP16(out[i*4:(i+1)*4]))

