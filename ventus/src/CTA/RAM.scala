/*
 * Copyright (c) 2021-2022 International Innovation Center of Tsinghua University, Shanghai
 * Ventus is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 *          http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND,
 * EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT,
 * MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details. */
package CTA

import chisel3._

class RAM(val WORD_SIZE: Int, val ADDR_SIZE: Int, val NUM_WORDS: Int) extends Module {
  val io = IO(new Bundle {
    val rd_addr = Input(UInt(ADDR_SIZE.W))
    val wr_addr = Input(UInt(ADDR_SIZE.W))
    val wr_word = Input(UInt(WORD_SIZE.W))
    val rd_word = Output(UInt(WORD_SIZE.W))
    val wr_en = Input(Bool())
    val rd_en = Input(Bool())
    })
    val mem = Mem(NUM_WORDS, UInt(WORD_SIZE.W))
    when(io.wr_en) {
      mem(io.wr_addr) := io.wr_word
      //printf(p"RAM: The writing address is: ${io.wr_addr}\n")
    }
    val rd_word_reg = RegInit(0.U(WORD_SIZE.W))
    when(io.rd_en) {
      rd_word_reg := mem(io.rd_addr)
    }
    io.rd_word := rd_word_reg
}