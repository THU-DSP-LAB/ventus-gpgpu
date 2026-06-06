# Program-level stressor for the L2 last_flush handshake bug.
#
# Intent:
#   1. Workgroup 0 dirties a modest number of lines, then exits quickly.
#   2. Workgroup 1 keeps generating L2 miss/writeback pressure.
#   3. Workgroup 0 completion triggers dcache flush while workgroup 1 traffic
#      can still occupy dir_result_buffer/SourceD.
#
# This file intentionally uses only standard RISC-V integer instructions plus
# raw Ventus custom words for barrier/endprg, so it can be assembled by the
# current Ventus LLVM toolchain.

csrr        x5, 0x807              # CSR_GDS
csrr        x6, 0x800              # CSR_TID
csrr        x7, 0x804              # CSR_WGID
andi        x7, x7, 1
bnez        x7, L_LONG_BLOCK

# Start after the first cache line in the GDS buffer. Each thread/lane writes a
# different word so the workload remains active across lanes.
addi        x10, x5, 256
slli        x6, x6, 2
add         x10, x10, x6

# Short group: dirty enough lines to make completion flush non-trivial, then
# end while the other group is still stressing L2.
li          x11, 512
li          x12, 128
li          x13, 0x5a5a0000
add         x13, x13, x6

L_SHORT_DIRTY:
sw          x13, 0(x10)
add         x10, x10, x12
addi        x13, x13, 1
addi        x11, x11, -1
bnez        x11, L_SHORT_DIRTY
j           L_ENDPRG

L_LONG_BLOCK:
addi        x10, x5, 256
slli        x6, x6, 2
add         x10, x10, x6

# Long group: keep SourceD/MSHR traffic active after group 0 begins flushing.
li          x11, 8192
li          x12, 128
li          x13, 0x6b6b0000
add         x13, x13, x6

L_LONG_PRESSURE:
sw          x13, 0(x10)
lw          x14, 0(x10)
add         x13, x13, x14
add         x10, x10, x12
addi        x13, x13, 1
addi        x11, x11, -1
bnez        x11, L_LONG_PRESSURE

L_ENDPRG:
.word       0x0000400b             # endprg x0, x0, x0
.word       0x0000400b
.word       0x0000400b
