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
package pipeline
import chisel3._
import chisel3.util._

class IntDivMod(xLen: Int) extends Module{
  val io = IO(new Bundle{
    val in = Flipped(DecoupledIO(new Bundle{
      val a, d = Input(UInt(xLen.W))
      val signed = Input(Bool())
    }))
    val out = DecoupledIO(new Bundle{
      val q, r = Output(UInt(xLen.W))
    })
  })
  val s_idle :: s_pre :: s_compute :: s_recovery :: s_finish :: Nil = Enum(5)
  val state = RegInit(s_idle)

  val aSign = io.in.bits.signed && io.in.bits.a.head(1).asBool
  val dSign = io.in.bits.signed && io.in.bits.d.head(1).asBool
  val qSignReg = RegEnable(aSign ^ dSign, io.in.fire)
  val rSignReg = RegEnable(aSign, io.in.fire)
  val unsignedA = Mux(aSign, (~io.in.bits.a).asUInt+1.U, io.in.bits.a)
  val unsignedD = Mux(dSign, (~io.in.bits.d).asUInt+1.U, io.in.bits.d)
  // when a = INT_MIN and d = -1, overflow:
  val overflow = io.in.bits.signed && io.in.bits.a.head(1).asBool && !io.in.bits.a.tail(1).orR && io.in.bits.d.andR
  val divByZero = io.in.bits.d === 0.U

  val rawAReg = RegEnable(io.in.bits.a, io.in.fire)
  val unsignedAReg = RegEnable(unsignedA, io.in.fire)
  val unsignedDReg = RegEnable(unsignedD, io.in.fire)
  val overflowReg = RegEnable(overflow, io.in.fire)
  val divByZeroReg = RegEnable(divByZero, io.in.fire)
  // ---------------- pre phase -------------------
  val aLez = PriorityEncoder(unsignedAReg.asBools.reverse)
  val dLez = PriorityEncoder(unsignedDReg.asBools.reverse)
  val iter = Mux(aLez > dLez, 0.U, dLez - aLez + 1.U)

  val aReg = RegInit(0.U((xLen+2).W))
  val dReg = RegInit(0.U((xLen+2).W))
  val aNorm = Cat(0.U(2.W), (unsignedAReg<<aLez)(xLen-1, 0))
  val dNorm = Cat(0.U(1.W), (unsignedDReg<<dLez)(xLen-1, 0), 0.U(1.W))
  val dNegNorm = Cat(1.U(1.W), (((~unsignedDReg).asUInt + 1.U)<<dLez)(xLen-1, 0), 0.U(1.W))
  val iterReg = RegEnable(iter, state===s_pre)
  val zeroQReg = RegEnable(unsignedAReg < unsignedDReg, state===s_pre)
// ---------------- compute phase ---------------
  val cnt = RegInit(0.U(log2Up(xLen).W))
  val cnt_next = Mux(cnt===0.U, 0.U, cnt - 1.U)

  val Q = RegInit(0.U(xLen.W))
  val QN = RegInit(0.U(xLen.W))

  val sel_pos = aReg.tail(1).head(2)==="b01".U
  val sel_neg = aReg.tail(1).head(2)==="b10".U

  val aShift = (aReg<<1).tail(1)
  val aNext = Mux(sel_pos, aShift + dNegNorm, Mux(sel_neg, aShift + dNorm, aShift))
// --------------- recovery phase -------------------
  val remIsNeg = aReg.head(1)
  val commonQReg = RegEnable(Mux(remIsNeg.asBool, Q + (~QN).asUInt, Q - QN), state===s_recovery)
  val recoveryR = Mux(remIsNeg.asBool, aReg.tail(1).head(xLen)+dReg.tail(1).head(xLen), aReg.tail(1).head(xLen))
  val commonRReg = RegEnable((recoveryR>>dLez)(xLen-1, 0), state===s_recovery)
// -------------- finish phase ------------------------
  val signedQ = Mux(qSignReg, (~commonQReg).asUInt + 1.U, commonQReg)
  val signedR = Mux(rSignReg, (~commonRReg).asUInt + 1.U, commonRReg)
  val specialQ = Mux(divByZeroReg, (-1).S(xLen.W).asUInt, Mux(overflowReg, Cat(1.U(1.W), 0.U((xLen-1).W)), 0.U(xLen.W)))
  // divByZero|zeroDivSth -> overflow
  val specialR = Mux(divByZeroReg||zeroQReg, rawAReg, 0.U(xLen.W))

  io.out.bits.q := Mux(divByZeroReg||zeroQReg||overflowReg, specialQ, signedQ)
  io.out.bits.r := Mux(divByZeroReg||zeroQReg||overflowReg, specialR, signedR)
  io.out.valid := state===s_finish
  io.in.ready := state===s_idle

// ------ FSM
  switch(state){
    is(s_idle){
      when(io.in.fire){
        when(overflow || divByZero){ state := s_finish
        }.otherwise{ state := s_pre }
      }
    }
    is(s_pre){
      when(unsignedAReg < unsignedDReg){ state := s_finish
      }.otherwise{ state := s_compute }
    }
    is(s_compute){
      when(cnt_next =/= 0.U){ state := s_compute
      }.otherwise{ state := s_recovery }
    }
    is(s_recovery){ state := s_finish }
    is(s_finish){
      when(io.out.fire){ state := s_idle }
    }
  }

  switch(state){
    is(s_idle){
      aReg := 0.U
      dReg := 0.U
      cnt := 0.U
    }
    is(s_pre){
      when(unsignedAReg < unsignedDReg){
        cnt := 0.U
      }.otherwise{
        aReg := aNorm
        dReg := dNorm
        cnt := iter
        Q := 0.U
        QN := 0.U
      }
    }
    is(s_compute){
      cnt := cnt_next
      aReg := aNext
      Q := Cat(Q.tail(1), sel_pos.asUInt)
      QN := Cat(QN.tail(1), sel_neg.asUInt)
    }
    is(s_recovery){
      cnt := 0.U
    }
    is(s_finish){
      cnt := 0.U
    }
  }
}