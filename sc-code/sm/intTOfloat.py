import struct

# 将十进制整数转换为32bit float
i = 608
b = struct.pack('i', i)
f = struct.unpack('f', b)[0]
print(f)

# 将十六进制整数转换为32bit float
h = 0x348d3ac0
b = struct.pack('I', h)
f = struct.unpack('f', b)[0]
print(f)
