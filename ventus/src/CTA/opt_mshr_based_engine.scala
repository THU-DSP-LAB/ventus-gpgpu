package CTA

import chisel3._
import chisel3.util._

class opt_mshr_based_engine(val CYCLE_WIDTH: Int, val SAMPLE_WIDTH: Int, val METRIC_WIDTH: Int, val GEARS: Array[Int], val THRES: Array[Int], val initial_max: Int, val max_max: Int, val WG_SLOT_ID_WIDTH: Int) extends Module{
    val io = IO(new Bundle{
        val c_mshr_full = Input(UInt(CYCLE_WIDTH.W))
        val c_valid = Input(Bool())
        val monitor_start = Output(Bool())
        val wg_max_update = Output(UInt((WG_SLOT_ID_WIDTH + 1).W))
        val wg_max_update_valid = Output(Bool())
        val init = Input(Bool())
        val start = Input(Bool())
    })
    val STATE_OPT_IDLE = 1.U(3.W)
    val STATE_OPT_LAUNCH = 2.U(3.W)
    val STATE_OPT_WAIT = 4.U(3.W)
    val state_opt = RegInit(STATE_OPT_IDLE)
    val max_cta = RegInit(initial_max.U((WG_SLOT_ID_WIDTH + 1).W))
    val max_cta_valid = RegInit(false.B)
    val counter = RegInit(0.U(SAMPLE_WIDTH.W))
    val start_i = RegInit(false.B)
    val monitor_start_i = RegInit(false.B)
    val result = Wire(UInt((METRIC_WIDTH + 1).W))
    when(io.c_mshr_full === 0.U){
        result := (1 << METRIC_WIDTH).U
    }
    .otherwise{
        result := counter / io.c_mshr_full
    }
    when(io.start){
        start_i := true.B
    }
    io.wg_max_update := max_cta
    io.wg_max_update_valid := max_cta_valid
    io.monitor_start := monitor_start_i

    monitor_start_i := false.B
    max_cta_valid := false.B
    switch(state_opt){
        is(STATE_OPT_IDLE){
            when(io.init){
                state_opt := STATE_OPT_IDLE
                max_cta := initial_max.U
                start_i := false.B
            }.elsewhen(start_i){
                state_opt := STATE_OPT_LAUNCH
            }
        }
        is(STATE_OPT_LAUNCH){
            when(io.init){
                state_opt := STATE_OPT_IDLE
                max_cta := initial_max.U
                start_i := false.B
            }
            .otherwise{
                monitor_start_i := true.B
                counter := 0.U
                state_opt := STATE_OPT_WAIT
            }
        }
        is(STATE_OPT_WAIT){
            when(io.init){
                state_opt := STATE_OPT_IDLE
                max_cta := initial_max.U
                start_i := false.B
            }
            .elsewhen(io.c_valid){
                for(i <- 0 until THRES.length){
                    when(result > (THRES(i)).U){
                        max_cta := GEARS(i).U
                    }
                }
                max_cta_valid := true.B
                start_i := false.B
                state_opt := STATE_OPT_IDLE
            }
            .otherwise{
                counter := counter + 1.U
            }
        }
    }
}