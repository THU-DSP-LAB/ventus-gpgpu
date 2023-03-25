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
package L1Cache.DCache

//import L1Cache.{L1TagAccess, MSHR}
import SRAMTemplate.SRAMTemplate
import chisel3._
import chisel3.util._
import config.config.Parameters

class DataAccessReadPort(implicit p: Parameters) extends DCacheBundle{
  val setIdx = Input(UInt(SetIdxBits.W))
  val wayIdx = Input(UInt(WayIdxBits.W))
  val bankEnable = Input(Vec(NBanks, Bool()))
  val bankOffset = Input(Vec(NBanks, UInt(BankOffsetBits.W)))

  val readRspData = Output(Vec(NBanks, UInt(WordLength.W)))
}

class DataAccessWritePort(implicit p: Parameters) extends DCacheBundle{
  val setIdx = Input(UInt(SetIdxBits.W))
  val wayIdx = Input(UInt(WayIdxBits.W))
  val bankEnable = Input(Vec(NBanks, Bool()))
  val bankOffset = Input(Vec(NBanks, UInt(BankOffsetBits.W)))
  val intraWordByteMask = Input(Vec(NBanks, Vec(BytesOfWord, Bool())))
  val missRsp = Input(Bool())//This bit open every bank and every word without offset and enable

  val writeData = Input(Vec(NBanks, Vec(BankWords, UInt(WordLength.W))))
}
/*
* WRITE port serves 2 scenario
* 1. coreReq WRITE
* BW = NBanks*WordLength
* potential byte enable
* 2. missRsp WRITE
* BW = BlockWords*WordLength
* no byte enable
* so intraBankWordMask BW align to 1.
* */

class DataAccess(implicit p: Parameters) extends DCacheModule {
  val io = IO(new Bundle{
    val readPort = new DataAccessReadPort
    val writePort = new DataAccessWritePort
  })

  val w = io.writePort
  val r = io.readPort
  val readBankOffset_r = RegNext(r.bankOffset)

/*
* whether placed in "for" or addr, is determined by its parallel WRITE requirement
* if it need to be accessed in parallel, then "for"
* otherwise just placed in addr
* */
  for (iofB <- 0 until NBanks){
    val BankRam = Wire(Vec(BankWords, UInt(WordLength.W)))
    for (iinB <- 0 until BankWords){
      val WordRam = Wire(Vec(BytesOfWord, UInt(8.W)))
      for (iinW <- 0 until  BytesOfWord){
        val ByteRam = SyncReadMem(NSets*NWays, UInt(8.W))
        when(w.missRsp){
          ByteRam(Cat(w.setIdx,w.wayIdx)) := w.writeData(iofB)(iinB)(8*(iinW+1)-1,8*iinW)
        }.elsewhen(io.writePort.bankEnable(iofB) && w.intraWordByteMask(iofB)(iinW)
          && iinB.asUInt() ===w.bankOffset(iofB)){
          ByteRam(Cat(w.setIdx,w.wayIdx)) := w.writeData(iofB)(0)(8*(iinW+1)-1,8*iinW)
        }
        WordRam(iinW) := ByteRam(Cat(r.setIdx,r.wayIdx))
      }
      BankRam(iinB) := Cat(WordRam.reverse)//TODO check endian
    }
    r.readRspData(iofB) := Mux1H(UIntToOH(readBankOffset_r(iofB)),BankRam)
  }
}
