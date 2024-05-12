package MmuTest

import chisel3._
import chisel3.util._
import chisel3.experimental.BundleLiterals._
import chisel3.experimental.VecLiterals._
import chiseltest._
import mmu._
import play.TestUtils.{IOTestDriver, IOTransform, checkForReady, checkForValid}
import MemboxS.Memory


object MmuTestUtils {
  case class SVPair[A <: MemboxS.BaseSV, B <: mmu.SVParam](
    val host : A,
    val device : B
  )

  trait MMUHelpers {
    val SV32 = SVPair(host = MemboxS.SV32, device = mmu.SV32)
    val SV39 = SVPair(host = MemboxS.SV39, device = mmu.SV39)
  }

//  class MemPortDriver[A <: MemboxS.BaseSV, B <: mmu.SVParam](SV: SVPair[A, B])(
//    val reqPort: DecoupledIO[Cache_Req],
//    val rspPort: DecoupledIO[Cache_Rsp],
//    val memBox: Memory[A]
//  ) extends IOTestDriver[Cache_Req, Cache_Rsp] with MMUHelpers {
//
//    val WaitingReq = 1
//    val SendingRsp = 2
//    var state = WaitingReq
//    var addr: BigInt = 0; var source: BigInt = 0; var data: Seq[BigInt] = Nil
//    val data_num = rspPort.bits.data.size
//
//    def eval(): Unit = {
//      var next_state = state
//      state match{
//        case WaitingReq => {
//          // reqPort fires
//          if(checkForValid(reqPort) && checkForReady(reqPort)){
//            next_state = SendingRsp
//            addr = reqPort.bits.addr.peek().litValue
//            source = reqPort.bits.source.peek().litValue
//            data = memBox.readWordsPhysical(addr, data_num, Array.fill(data_num)(true))._2.map(_.toBigInt)
//          }
//        }
//        case SendingRsp => {
//          if(checkForValid(reqPort) && checkForReady(rspPort)){
//            next_state = WaitingReq
//            addr = 0; source = 0; data = Nil
//          }
//        }
//        case _ => {}
//      }
//      next_state match{
//        case WaitingReq => {
//          reqPort.ready.poke(true.B)
//          rspPort.valid.poke(false.B)
//        }
//        case SendingRsp => {
//          reqPort.ready.poke(false.B)
//          rspPort.valid.poke(true.B)
//          rspPort.bits.poke(new Cache_Rsp(SV.device).Lit(
//            _.source -> source.U,
//            _.data -> Vec(data_num, UInt(SV.device.xLen.W)).Lit(
//              (0 until data_num).map{i => i -> data(i).U}: _*
//            )
//          ))
//        }
//      }
//      state = next_state
//    }
//  }

  class MMUMemPortDriverDelay[A <: MemboxS.BaseSV, B <: mmu.SVParam](SV: SVPair[A, B])(
    val reqPort: DecoupledIO[Cache_Req],
    val rspPort: DecoupledIO[Cache_Rsp],
    val memBox: Memory[A],
    val latency: Int,
    val depth: Int
  ) extends IOTestDriver[Cache_Req, Cache_Rsp]
    with IOTransform[Cache_Req, Cache_Rsp]{

    var addr: BigInt = 0; var source: BigInt = 0; var data: Seq[BigInt] = Nil
    val data_num = rspPort.bits.data.size
    var rsp_queue: Seq[(Int, Cache_Rsp)] = Seq.empty

    override def transform(in: Cache_Req): Cache_Rsp = {
      addr = in.addr.peek().litValue
      source = in.source.peek().litValue
      data = memBox.readWordsPhysical(addr, data_num, Array.fill(data_num)(true))._2.map(_.toBigInt)
      (new Cache_Rsp(SV.device)).Lit(
        _.source -> source.U,
        _.data -> Vec(data_num, UInt(SV.device.xLen.W)).Lit(
          (0 until data_num).map{i => i -> data(i).U}: _*
        )
      )
    }

    override def eval(): Unit = {
      if(checkForValid(reqPort) && checkForReady(reqPort)){
        rsp_queue :+= (latency, transform(reqPort.bits))
      }

      if(rsp_queue.nonEmpty && rsp_queue.head._1 == 0){
        if(checkForValid(rspPort) && checkForReady(rspPort)){
          rspPort.valid.poke(false.B)
          rsp_queue = rsp_queue.drop(1)
        }
        else{
          rspPort.valid.poke(true.B)
          rspPort.bits.poke(rsp_queue.head._2)
        }
      }
      else{
        rspPort.valid.poke(false.B)
      }

      rsp_queue = rsp_queue.zipWithIndex.map{ case (e, i) =>
        if (e._1 > i) (e._1 - 1, e._2) else (i, e._2)
      }
      if(rsp_queue.nonEmpty && rsp_queue.head._1 == 0){
        rspPort.valid.poke(true.B)
        rspPort.bits.poke(rsp_queue.head._2)
      }
      reqPort.ready.poke((rsp_queue.size < depth).B)
    }
  }
}
