.equ CSR_NUMW,  0x801
.equ CSR_NUMT,  0x802
.equ CSR_TID,   0x800
.equ CSR_WID,   0x805
.equ CSR_GDS,   0x807
.equ CSR_LDS,   0x806

addi x27, x0, 1153
add x27, x27, x27
add x27, x27, x27

vle32.v v20, (x27)


lw x14, 20(x29)
vle32.v v20, (x20)
vfadd.vv v6, v7, v27
add x7, x30, x22
addi x4, x25, 4
vfadd.vv v8, v10, v18
vfadd.vv v27, v11, v25
add x3, x10, x26
beq x22, x14, label13
label13:
lw x25, 16(x5)
lw x12, 4(x2)
vadd.vv v23, v11, v27
vadd.vv v22, v23, v3
vadd.vx v24, v25, x23
addi x19, x9, -4
addi x8, x3, -4
vle32.v v15, (x23)
vle32.v v16, (x23)
vadd.vx v19, v19, x1
vfadd.vv v3, v1, v11
vfadd.vv v5, v9, v5
add x19, x7, x4
vadd.vx v11, v28, x11
add x10, x25, x1
vadd.vv v14, v11, v21
vfadd.vv v2, v10, v23
vle32.v v30, (x12)
vadd.vv v6, v9, v16
vfadd.vv v10, v13, v4
vfadd.vv v4, v7, v25
lw x27, 28(x4)
vfadd.vv v7, v4, v23
addi x15, x29, 4
vadd.vv v13, v17, v29
lw x20, 4(x19)
vfadd.vv v4, v20, v11
vadd.vv v11, v30, v25
beq x2, x30, label42
label42:
add x16, x22, x25
add x12, x8, x20
vfadd.vv v18, v16, v26
vfadd.vv v22, v5, v7
addi x9, x22, -4
vfadd.vv v29, v9, v15
beq x9, x14, label49
label49:
vadd.vx v14, v15, x10
vle32.v v16, (x16)
vadd.vx v5, v22, x21
beq x27, x26, label53
label53:
beq x3, x17, label54
label54:
vadd.vx v18, v26, x25
vle32.v v26, (x2)
add x28, x16, x25
addi x16, x15, -8
beq x22, x23, label59
label59:
vle32.v v16, (x12)
add x11, x3, x3
vadd.vv v5, v1, v7
vadd.vv v2, v19, v1
beq x6, x5, label64
label64:
vle32.v v7, (x20)
lw x12, 0(x27)
lw x7, -16(x28)
add x27, x26, x7
add x8, x9, x27
addi x25, x25, -4
beq x27, x16, label71
label71:
add x3, x1, x4
lw x3, -8(x17)
vadd.vv v26, v6, v29
vadd.vv v23, v14, v15
vfadd.vv v18, v22, v25
vle32.v v9, (x29)
vfadd.vv v28, v7, v6
vfadd.vv v13, v15, v21
lw x4, -32(x18)
vle32.v v30, (x10)
beq x22, x19, label82
label82:
vle32.v v2, (x12)
beq x21, x22, label84
label84:
lw x22, 32(x22)
vle32.v v5, (x21)
addi x6, x26, 4
addi x21, x30, 0
lw x8, -36(x1)
vfadd.vv v26, v5, v9
vfadd.vv v29, v7, v13
beq x18, x24, label92
label92:
lw x26, 16(x10)
beq x16, x19, label94
label94:
vfadd.vv v25, v2, v6
vadd.vv v29, v14, v9
lw x1, -20(x23)
vle32.v v27, (x24)
vfadd.vv v24, v23, v10
add x28, x30, x20
vadd.vv v18, v1, v22
addi x1, x19, -4
vadd.vx v29, v10, x23
vadd.vv v25, v11, v3
beq x8, x28, label105
label105:
lw x14, 40(x11)
addi x16, x28, 8
add x7, x10, x18
vle32.v v1, (x18)
vadd.vx v17, v10, x20
vadd.vv v27, v18, v25
add x15, x7, x6
vadd.vv v14, v2, v12
vadd.vx v12, v9, x29
addi x28, x11, -4
vfadd.vv v30, v29, v23
beq x15, x14, label117
label117:
vle32.v v23, (x16)
add x22, x8, x17
addi x14, x24, -4
beq x24, x8, label121
label121:
lw x27, -32(x1)
add x2, x11, x18
vle32.v v11, (x21)
vfadd.vv v4, v4, v30
vadd.vx v13, v15, x18
lw x20, 4(x2)
vadd.vv v1, v11, v24
lw x3, -28(x29)
addi x15, x7, 4
vfadd.vv v19, v25, v1
lw x30, 16(x1)
vle32.v v30, (x22)
add x16, x4, x29
add x18, x19, x17
lw x25, 24(x12)
add x16, x21, x17
beq x8, x27, label138
label138:
beq x27, x17, label139
label139:
addi x11, x25, -4
vadd.vv v25, v5, v21
vfadd.vv v10, v25, v21
vadd.vx v13, v12, x18
lw x27, 4(x17)
lw x22, -24(x29)
addi x8, x17, -8
vfadd.vv v22, v2, v2
lw x11, -4(x30)
beq x7, x2, label149
label149:
vle32.v v28, (x2)
vle32.v v14, (x26)
vfadd.vv v28, v18, v14
lw x7, 16(x24)
addi x14, x26, -4
vadd.vx v8, v9, x25
vadd.vv v15, v10, v8
vadd.vx v30, v26, x28
vadd.vv v29, v4, v4
addi x12, x22, 4
vadd.vx v12, v19, x6
add x13, x28, x17
vadd.vx v4, v17, x1
addi x13, x6, -4
beq x12, x16, label164
label164:
vadd.vx v7, v3, x5
vadd.vv v3, v7, v18
vadd.vv v27, v4, v20
vle32.v v9, (x3)
addi x8, x27, -8
add x7, x9, x9
vfadd.vv v12, v4, v27
vle32.v v11, (x17)
vfadd.vv v18, v6, v13
vfadd.vv v5, v27, v9
lw x24, 20(x19)
add x27, x3, x28
vadd.vv v27, v29, v28
vle32.v v21, (x24)
vadd.vv v2, v9, v24
lw x29, 16(x26)
beq x5, x7, label181
label181:
add x26, x15, x12
addi x2, x11, 4
add x23, x25, x8
add x1, x10, x19
vfadd.vv v1, v3, v17
vadd.vx v7, v18, x16
beq x6, x14, label188
label188:
vfadd.vv v5, v13, v26
beq x9, x28, label190
label190:
add x20, x6, x3
lw x27, 16(x18)
vadd.vv v16, v5, v16
vle32.v v2, (x5)
vadd.vv v9, v13, v23
vadd.vx v3, v11, x2
addi x27, x18, -8
addi x1, x14, 4
vadd.vv v27, v7, v21
vle32.v v4, (x4)
