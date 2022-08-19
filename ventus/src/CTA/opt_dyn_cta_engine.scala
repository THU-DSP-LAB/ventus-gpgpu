package CTA

import chisel3._
import chisel3.util._

class opt_dyn_cta_engine(val CYCLE_WIDTH: Int, val SAMPLE_WIDTH: Int, val sample_period: Int, val t_mem_l: Int, val t_mem_h: Int, val t_idle: Int, val initial_max: Int, val max_max: Int, val WG_SLOT_ID_WIDTH: Int) extends Module{
    val io = IO(new Bundle{
        val c_mem = Input(UInt(CYCLE_WIDTH.W))
        val c_idle = Input(UInt(CYCLE_WIDTH.W))
        val c_valid = Input(Bool())
        val monitor_start = Output(Bool())
        val monitor_stop = Output(Bool())
        val wg_max_update = Output(UInt((WG_SLOT_ID_WIDTH + 1).W))
        val wg_max_update_valid = Output(Bool())
        val init = Input(Bool())
        val start = Input(Bool())
    })
    val STATE_OPT_IDLE = 1.U(4.W)
    val STATE_OPT_LAUNCH = 2.U(4.W)
    val STATE_OPT_WAIT = 4.U(4.W)
    val STATE_OPT_CALC = 8.U(4.W)
    val state_opt = RegInit(STATE_OPT_IDLE)
    val max_cta = RegInit(initial_max.U((WG_SLOT_ID_WIDTH + 1).W))
    val max_cta_valid = RegInit(false.B)
    val counter = RegInit(0.U(SAMPLE_WIDTH.W))
    val start_i = RegInit(false.B)
    val monitor_start_i = RegInit(false.B)
    val monitor_stop_i = RegInit(false.B)
    when(io.start){
        start_i := true.B
    }
    io.wg_max_update := max_cta
    io.wg_max_update_valid := max_cta_valid
    io.monitor_start := monitor_start_i
    io.monitor_stop := monitor_stop_i

    monitor_start_i := false.B
    monitor_stop_i := false.B
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
            .otherwise{
                when(counter < sample_period.U){
                    counter := counter + 1.U
                }
                .otherwise{
                    state_opt := STATE_OPT_CALC
                    monitor_stop_i := true.B
                }
            }
        }
        is(STATE_OPT_CALC){
            when(io.init){
                state_opt := STATE_OPT_IDLE
                max_cta := initial_max.U
                start_i := false.B
            }
            .elsewhen(io.c_valid){
                when(io.c_idle >= t_idle.U){
                    when(max_cta =/= max_max.U){
                        max_cta := max_cta + 1.U
                    }
                }
                .otherwise{
                    when(io.c_mem < t_mem_l.U){
                        when(max_cta =/= max_max.U){
                            max_cta := max_cta + 1.U
                        }
                    }
                    when(io.c_mem >= t_mem_h.U){
                        when(max_cta =/= 0.U){
                            max_cta := max_cta - 1.U
                        }
                    }
                }
                max_cta_valid := true.B
                state_opt := STATE_OPT_IDLE
            }
        }
    }
}