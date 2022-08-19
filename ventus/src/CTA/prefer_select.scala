package CTA

import chisel3._

class prefer_select(val RANGE: Int, val ID_WIDTH: Int) extends RawModule{
    val io = IO(new Bundle{
        val signal = Vec(RANGE, Input(Bool()))
        val prefer = Input(UInt(ID_WIDTH.W))
        val valid = Output(Bool())
        val id = Output(UInt(ID_WIDTH.W))
    })
    val found = Wire(Bool())
    val found_id = Wire(UInt((ID_WIDTH + 1).W))
    found := false.B
    found_id := 0.U
    for(i <- 1 until RANGE + 1){
        when(i.U((ID_WIDTH + 1).W) + io.prefer >= RANGE.U((ID_WIDTH + 1).W)){
            when(io.signal(i.U((ID_WIDTH + 1).W) + io.prefer - RANGE.U((ID_WIDTH + 1).W))){
                found := true.B
                found_id := i.U((ID_WIDTH + 1).W) + io.prefer - RANGE.U((ID_WIDTH + 1).W)
            }
        }
        .otherwise{
            when(io.signal(i.U + io.prefer)){
                found := true.B
                found_id := i.U + io.prefer
            }
        }
    }
    io.valid := found
    io.id := found_id(ID_WIDTH - 1, 0)
}