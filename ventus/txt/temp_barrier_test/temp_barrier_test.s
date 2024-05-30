addi        x7, x7, 31
vid.v       v7
vadd.vx     v8, v7, x6
barrier     x0, x0, 0
addi        x10, x5, 128
vadd.vx     v8, v7, x10
endprg      x0, x0, x0
endprg      x0, x0, x0
endprg      x0, x0, x0