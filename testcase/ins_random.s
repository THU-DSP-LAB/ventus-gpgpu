.equ CSR_NUMW,  0x801
.equ CSR_NUMT,  0x802
.equ CSR_TID,   0x800
.equ CSR_WID,   0x805
.equ CSR_GDS,   0x807
.equ CSR_LDS,   0x806
addi x1, x0, 915
addi x2, x0, 713
addi x3, x0, 919
addi x4, x0, 533
addi x5, x0, 706
addi x6, x0, 738
addi x7, x0, 661
addi x8, x0, 715
addi x9, x0, 980
addi x10, x0, 540
addi x11, x0, 691
addi x12, x0, 608
addi x13, x0, 954
addi x14, x0, 575
addi x15, x0, 614
addi x16, x0, 652
addi x17, x0, 778
addi x18, x0, 735
addi x19, x0, 989
addi x20, x0, 964
addi x21, x0, 800
addi x22, x0, 626
addi x23, x0, 871
addi x24, x0, 862
addi x25, x0, 837
addi x26, x0, 992
addi x27, x0, 912
addi x28, x0, 870
addi x29, x0, 954
addi x30, x0, 787
addi x31, x0, 633
vadd.vx v0, v31, x0
vadd.vx v1, v31, x1
vadd.vx v2, v31, x2
vadd.vx v3, v31, x3
vadd.vx v4, v31, x4
vadd.vx v5, v31, x5
vadd.vx v6, v31, x6
vadd.vx v7, v31, x7
vadd.vx v8, v31, x8
vadd.vx v9, v31, x9
vadd.vx v10, v31, x10
vadd.vx v11, v31, x11
vadd.vx v12, v31, x12
vadd.vx v13, v31, x13
vadd.vx v14, v31, x14
vadd.vx v15, v31, x15
vadd.vx v16, v31, x16
vadd.vx v17, v31, x17
vadd.vx v18, v31, x18
vadd.vx v19, v31, x19
vadd.vx v20, v31, x20
vadd.vx v21, v31, x21
vadd.vx v22, v31, x22
vadd.vx v23, v31, x23
vadd.vx v24, v31, x24
vadd.vx v25, v31, x25
vadd.vx v26, v31, x26
vadd.vx v27, v31, x27
vadd.vx v28, v31, x28
vadd.vx v29, v31, x29
vadd.vx v30, v31, x30
vadd.vx v31, v31, x31
add x2, x5, x5
lw x4, 31(x1)
vadd.vx v4, v1, x1
lw x1, -50(x2)
vle32.v v2, (x1)
add x1, x3, x5
vfadd.vv v1, v4, v2
add x4, x3, x1
lw x5, 15(x1)
lw x1, -13(x3)
vfadd.vv v5, v1, v2
vfadd.vv v4, v5, v3
vadd.vx v5, v4, x2
vfadd.vv v2, v2, v3
vfadd.vv v3, v4, v3
vfadd.vv v1, v1, v3
vle32.v v1, (x4)
add x2, x3, x5
add x2, x4, x3
addi x1, x4, 8
lw x4, 44(x2)
vadd.vx v5, v2, x2
vadd.vx v5, v2, x4
addi x4, x3, 7
vfadd.vv v2, v3, v4
addi x2, x3, 4
add x5, x1, x1
vadd.vx v4, v2, x5
vadd.vx v3, v4, x4
vfadd.vv v2, v4, v5
vle32.v v2, (x3)
lw x2, 27(x1)
vadd.vx v1, v5, x3
vadd.vv v3, v3, v5
add x3, x1, x1
vadd.vv v5, v1, v5
vfadd.vv v1, v4, v2
vfadd.vv v2, v2, v2
beq x3, x2, label39
label39:
vle32.v v5, (x1)
vadd.vv v3, v4, v5
vle32.v v2, (x4)
add x1, x3, x5
add x4, x1, x3
lw x5, -7(x1)
vadd.vv v2, v4, v2
lw x3, 44(x3)
addi x4, x3, 39
vadd.vx v5, v5, x3
vfadd.vv v5, v1, v4
