import struct
import numpy as np

dec_float = 5.9


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
    print(float)  # 5.9
    return float


def MatFP16ToHexString(a):
    a_str = []
    for i in a:
        for j in i:
            a_str.append(getFP16Str(j))
    return a_str


def MatABCD2Register(a, b, c, d, thread: int = 32):
    Register_A = ['h' for i in range(thread)]
    Register_B = ['h' for i in range(thread)]
    Register_C = ['h' for i in range(thread)]
    Register_D = ['h' for i in range(thread)]
    for i in range(thread):
        Register_A[i] += (a[2 * i + 1 + 64] + a[2 * i + 64] + a[2 * i + 1] + a[2 * i])
        Register_B[i] += ('0000' + '0000' + b[2 * i + 1] + b[2 * i])
        Register_C[i] += (c[2 * i + 1 + 64] + c[2 * i + 64] + c[2 * i + 1] + c[2 * i])
        Register_D[i] += (d[2 * i + 1 + 64] + d[2 * i + 64] + d[2 * i + 1] + d[2 * i])

    return Register_A, Register_B, Register_C, Register_D


def getMatABCD(m: int, n: int, k: int):
    a = np.random.randn(m, k).astype(np.float16)
    b = np.random.randn(k, n).astype(np.float16)
    c = np.random.randn(m, n).astype(np.float16)

    d = np.dot(a, b) + c
    print(d.dtype, d)
    np.savetxt('testData/a.txt', a)
    np.savetxt('testData/b.txt', b)
    np.savetxt('testData/c.txt', c)
    np.savetxt('testData/d.txt', d)

    print('Save Done!')
    return a, b, c, d


def strArr_Save(str_array, fname: str):
    # 使用空格将数组中的字符串连接起来
    space_separated_string = ' '.join(str_array)
    # 将连接后的字符串写入到TXT文件中
    with open(fname, 'w', encoding='utf-8') as file:
        file.write(space_separated_string)


if __name__ == '__main__':
    a, b, c, d = getMatABCD(16, 8, 8)
    print(getFP16Str(0))
    a_str = MatFP16ToHexString(a)
    b_str = MatFP16ToHexString(b.T)
    c_str = MatFP16ToHexString(c)
    d_str = MatFP16ToHexString(d)
    # print(a_str)
    print(len(a_str))
    RA, RB, RC, RD = MatABCD2Register(a_str, b_str, c_str, d_str)
    strArr_Save(RA, 'testData/RA.txt')
    strArr_Save(RB, 'testData/RB.txt')
    strArr_Save(RC, 'testData/RC.txt')
    strArr_Save(RD, 'testData/RD.txt')
