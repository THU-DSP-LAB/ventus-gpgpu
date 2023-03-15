.text
# data
#   matrix A                x10, input
#   matrix M                x9, init with all zero
#   vector B                x11, input
# value
# - size                    x8,
# - tid.x                   v3
# - tid.y                   v4
# - t                       x7
## !! size should be multiplicand of 8 !!
#.equ        size,       0x10000000  # 'size', or the startAddr of all data
csrrs       x9, 0x807, x0
lw          x5, 0(x9)               # szN
addi        x8, x0, 1
sll         x8, x8, x5              # size = 2 ** szN
addi        x9, x9, 128             # baseAddr of matM
mul         x11, x8, x8
slli        x11, x11, 2             # sizeof(matrix)
add         x10, x9, x11            # baseAddr of matA
#li          x10, matA
add         x11, x10, x11           # baseAddr of vecB

vid.v       v7
csrrs       x7, 0x800, x0           # get warpbase
#csrrs       x6, 0x802, x0           # get num_thread
#vmv.v.x     v2, x6
vadd.vx     v2, v7, x7              # 1D tid
#vmadd.vx    v2, x7, v7
vsrl.vx     v3, v2, x5
addi        x8, x8, -1
vand.vx     v4, v2, x8
addi        x8, x8, 1
addi        x7, x0, 0               # t
vmv.v.x     v31, x0

Kernel1:
addi        x20, x7, 1              # t+1
vadd.vx     v20, v4, x20            # tid.y + (t+1)
vmv.v.x     v21, x8                 # size    
vbge        v20, v21, K1_A
vbne        v3, v31, K1_END         # | only tid.x==0 exec kernel1
vmul.vx     v20, v20, x8            # | i.e. the first warp(s)
slli        x17, x7, 2              # [][t]
mul         x20, x17, x8            # [t][0]
add         x20, x20, x17           # [t][t]
slli        x16, x8, 2              # [1][0] i.e. stride
add         x15, x20, x16           # [t+1][t]
add         x15, x15, x10           # A[t+1][t]

vlse32.v    v10, (x15), x16         # A[(t+1)+tid.y][t]
add         x15, x20, x10
lw          x15, 0(x15)             # A[t][t]
vmv.v.x     v11, x15
vfdiv.vv    v10, v10, v11           # FP DIV

add         x15, x20, x16
add         x15, x15, x9
vsse32.v    v10, (x15), x16         # save to M

K1_END:
join        v0,v0, K1_A
K1_A:
join        v0,v0, K1_B
K1_B: 
barrier     x0, x0, 0               # syncthread

Kernel2:
addi        x20, x7, 1
vadd.vx     v20, v3, x20            # tid.x + (t+1)
vadd.vx     v21, v4, x7             # tid.y + t
vmv.v.x     v22, x8
vbge        v20, v22, K2_J2
vbge        v21, v22, K2_J1

vmul.vx     v18, v20, x8
vadd.vx     v19, v18, x7
vsll.vi     v19, v19, 2             # [(t+1)+tid.x][t]
vloxei32.v  v9, (x9), v19           # M[(t+1)+tid.x][t]
#vadd.vv     v18, v18, v21
#vsll.vi     v18, v18, 2             # [(t+1)+tid.x][t+tid.y]
#vloxei32.v  v10, (x10), v18         # A[(t+1)+tid.x][t+tid.y]
vmv.x.s     x15, v19
# 此处的vle32尽量WarpSize整除MatrixSize或反过来, 不满足的话也许会出问题, 我不好说
add         x15, x15, x10           # A[(t+1)+tid.x.base][t+0]
vle32.v     v10, (x15)              # |
mul         x20, x7, x8
vadd.vx     v22, v21, x20
vsll.vi     v22, v22, 2             # [t][t+tid.y]
# 在WarpSize > MatrixSize的情况下, 以下指令并不能使用vle32.v或vlse32.v代替.
vloxei32.v  v12, (x10), v22         # A[t][t+tid.y]

vfnmsac.vv  v10, v9, v12
#vsoxei32.v  v10, (x10), v18
vse32.v     v10, (x15)

vbne        v3, v31, K2_END         # only tid.x==0 exec:
#vsll.vi     v19, v20, 2             # [(t+1)+tid.x]
#vloxei32.v  v11, (x11), v19         # B[(t+1)+tid.x]
vadd.vi     v21, v21, 1
vmv.v.x     v22, x8
vbge        v21, v22, K2_J0

slli        x6, x7, 2               # [t]
add         x6, x6, x11
lw          x14, 0(x6)              # B[t]
addi        x6, x6, 4              # B[t+1]
vle32.v     v11, (x6)              # B[(t+1)+tid.y]
vfnmsac.vf  v11, f14, v9
vse32.v     v11, (x6)
K2_J0:
join        v0, v0, K2_END
K2_END:
join        v0, v0, K2_J1
K2_J1:
join        v0, v0, K2_J2
K2_J2:
join        v0, v0, K2_B
K2_B:
barrier     x0, x0, 0
addi        x7, x7, 1               # t+1
blt         x7, x8, Kernel1
endprg      x0, x0, x0
ret
