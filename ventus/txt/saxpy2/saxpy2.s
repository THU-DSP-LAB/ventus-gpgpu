.equ CSR_NUMW,  0x801
.equ CSR_NUMT,  0x802
.equ CSR_TID,   0x800
.equ CSR_WID,   0x805
.equ CSR_GDS,   0x807
.equ CSR_LDS,   0x806
# register arguments:
#     a0      n
#     a3      a
#     a1      x
#     a2      y

csrr            t1, CSR_GDS
lw              a0, 0(t1)
lw              a3, 4(t1)
addi            t2, a0, 31
srli            t2, t2, 5
slli            t2, t2, 7
addi            a1, t1, 128
add             a2, a1, t2

saxpy:
vsetvli         a4, a0, e32, m8, ta, ma
vle32.v         v0, (a1)
sub             a0, a0, a4
slli            a4, a4, 2
add             a1, a1, a4
vle32.v         v8, (a2)
vfmacc.vf       v8, fa3, v0
vse32.v         v8, (a2)
add             a2, a2, a4
bnez            a0, saxpy
endprg          x0, x0, x0
endprg          x0, x0, x0
endprg          x0, x0, x0