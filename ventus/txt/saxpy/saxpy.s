.equ CSR_NUMW,  0x801
.equ CSR_NUMT,  0x802
.equ CSR_TID,   0x800
.equ CSR_WID,   0x805
.equ CSR_GDS,   0x807
.equ CSR_LDS,   0x806

# data
#   vector X                x10
#   vector Y                x11
# value
#   size                    x7
#   fp scalar A             x20

csrr        x5, CSR_GDS
csrr        x6, CSR_TID
vid.v       v7
vadd.vx     v8, v7, x6
lw          x7, 0(x5)               # size
vmv.v.x     v9, x7
vbge        v8, v9, L_END
lw          x20, 4(x5)              # A
addi        x7, x7, 31
srli        x7, x7, 5
slli        x7, x7, 7
slli        x6, x6, 2
addi        x10, x5, 128            
add         x10, x10, x6            # X
vle32.v     v10, (x10)
add         x11, x10, x7            # Y
vle32.v     v11, (x11)
vfmacc.vf   v11, f20, v10
vse32.v     v11, (x11)

L_END:
join        v0, v0, L_ENDPRG
L_ENDPRG:
barrier     x0, x0, 0
endprg      x0, x0, x0
endprg      x0, x0, x0
endprg      x0, x0, x0
