package MmuTest

import chisel3._
import chisel3.util._
import chisel3.experimental.BundleLiterals._
import chisel3.experimental.VecLiterals._
import chiseltest._
import pipeline.mmu
import pipeline.mmu._
import MemboxS.Memory


object MmuTestUtils {
  case class SVPair[A <: MemboxS.BaseSV, B <: mmu.SVParam](
    val host : A,
    val device : B
  )

  object IOHelpers {
    def checkForValid[T <: Data](port: DecoupledIO[T]): Boolean = port.valid.peek().litToBoolean
    def checkForReady[T <: Data](port: DecoupledIO[T]): Boolean = port.ready.peek().litToBoolean
    val SV32 = SVPair(host = MemboxS.SV32, device = mmu.SV32)
    val SV39 = SVPair(host = MemboxS.SV39, device = mmu.SV39)
  }

  class RequestSender[A <: Data, B <: Data](
    val reqPort: DecoupledIO[A],
    val rspPort: DecoupledIO[B]
  ){
    import IOHelpers._
    var send_list: Seq[A] = Nil
    val Idle = 0; val SendingReq = 1; val WaitingRsp = 2
    var state = Idle; var next_state = Idle

    def add(req: A): Unit = send_list = send_list :+ req
    def add(req: Seq[A]): Unit = send_list = send_list ++ req

    def eval(): Unit = {
      state = next_state
      state match {
        case Idle =>
          send_list match {
            case Nil =>
              reqPort.valid.poke(false.B)
              next_state = Idle
            case _ =>
              reqPort.valid.poke(true.B)
              reqPort.bits.poke(send_list.head)
              next_state = SendingReq
          }
        case SendingReq =>
          if (checkForReady(reqPort)){
            reqPort.valid.poke(false.B)
            rspPort.ready.poke(true.B)
            next_state = WaitingRsp
          }
        case WaitingRsp =>
          if (checkForValid(rspPort)){
            rspPort.ready.poke(false.B)
            send_list = send_list.drop(1)
            next_state = Idle
          }
      }
    }
  }

  class MemPortDriver[A <: MemboxS.BaseSV, B <: mmu.SVParam](SV: SVPair[A, B])(
    val reqPort: DecoupledIO[Cache_Req],
    val rspPort: DecoupledIO[Cache_Rsp],
    val memBox: Memory[A]
  ){
    import IOHelpers._

    val WaitingReq = 1
    val SendingRsp = 2
    var state = WaitingReq
    var addr: BigInt = 0; var source: BigInt = 0; var data: Seq[BigInt] = Nil
    val data_num = rspPort.bits.data.size

    def eval(): Unit = {
      var next_state = state
      state match{
        case WaitingReq => {
          // reqPort fires
          if(checkForValid(reqPort) && checkForReady(reqPort)){
            next_state = SendingRsp
            addr = reqPort.bits.addr.peek().litValue
            source = reqPort.bits.source.peek().litValue
            data = memBox.readWordsPhysical(addr, data_num, Array.fill(data_num)(true))._2.map(_.toBigInt)
          }
        }
        case SendingRsp => {
          if(checkForValid(reqPort) && checkForReady(rspPort)){
            next_state = WaitingReq
            addr = 0; source = 0; data = Nil
          }
        }
        case _ => {}
      }
      next_state match{
        case WaitingReq => {
          reqPort.ready.poke(true.B)
          rspPort.valid.poke(false.B)
        }
        case SendingRsp => {
          reqPort.ready.poke(false.B)
          rspPort.valid.poke(true.B)
          rspPort.bits.poke(new Cache_Rsp(SV.device).Lit(
            _.source -> source.U,
            _.data -> Vec(data_num, UInt(SV.device.xLen.W)).Lit(
              (0 until data_num).map{i => i -> data(i).U}: _*
            )
          ))
        }
      }
      state = next_state
    }
  }
}
