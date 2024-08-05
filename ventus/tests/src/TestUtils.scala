package play

import chisel3._
import chisel3.util._
import chisel3.experimental.BundleLiterals._
import chisel3.experimental.VecLiterals._
import chiseltest._
import top._
import L2cache.{TLBundleA_lite, TLBundleD_lite}

object TestUtils {
  def checkForValid[T <: Data](port: DecoupledIO[T]): Boolean = port.valid.peek().litToBoolean
  def checkForReady[T <: Data](port: DecoupledIO[T]): Boolean = port.ready.peek().litToBoolean

  abstract class IOTestDriver[+A <: Data, +B <: Data] {
    val reqPort: DecoupledIO[A]
    val rspPort: DecoupledIO[B]
    def eval(): Unit
  }
  trait IOTransform[A <: Data, B <: Data] extends IOTestDriver[A, B] {
    def transform(in: A): B
  }

  class RequestSender[A <: Data, B <: Data](
    val reqPort: DecoupledIO[A],
    val rspPort: DecoupledIO[B]
  ) extends IOTestDriver[A, B] {
    var send_list: Seq[A] = Nil
    val Idle = 0; val SendingReq = 1; val WaitingRsp = 2
    var state = Idle; var next_state = Idle

    def add(req: A): Unit = send_list = send_list :+ req
    def add(req: Seq[A]): Unit = send_list = send_list ++ req
    var pause: Boolean = false

    def finishWait(): Boolean = {
      state == WaitingRsp && checkForValid(rspPort)
    }

    def eval(): Unit = {
      state = next_state
      if(pause){
        reqPort.valid.poke(false.B)
        rspPort.ready.poke(false.B)
        return
      }
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
          if (finishWait()){
            rspPort.ready.poke(false.B)
            send_list = send_list.drop(1)
            next_state = Idle
          }
      }
    }
  }

  class MemPortDriverDelay[A <: TLBundleA_lite, B >: TLBundleD_lite <: Data](
    val reqPort: DecoupledIO[A],
    val rspPort: DecoupledIO[B],
    val mem: MemBox,
    val latency: Int,
    val depth: Int
  ) extends IOTestDriver[A, B] with IOTransform[A, B]{
    val data_byte_count = reqPort.bits.data.getWidth / 8

    var rsp_queue: Seq[(Int, B)] = Seq.empty

    def eval(): Unit = {
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

    def transform(req: A): B = {
      val opcode_req = req.opcode.peek().litValue.toInt
      var opcode_rsp = 0
      val addr = req.address.peek().litValue
      val source = req.source.peek().litValue
      var data = new Array[Byte](data_byte_count)

      opcode_req match {
        case 4 => { // read
          data = mem.readMem(addr, data_byte_count)
          opcode_rsp = 1
        }
        case 1 => { // write partial
          data = top.helper.BigInt2ByteArray(req.data.peek().litValue, data_byte_count)
          val mask = req.mask.peek().litValue.toString(2).reverse.padTo(req.mask.getWidth, '0').map {
            case '1' => true
            case _ => false
          }.toArray
          mem.writeMem(addr, data_byte_count, data, mask)
          data = Array.fill(data_byte_count)(0.toByte) // write operation
          opcode_rsp = 0 // response = 0
        }
        case 0 => { // write full
          data = top.helper.BigInt2ByteArray(req.data.peek().litValue, data_byte_count)
          val mask = IndexedSeq.fill(req.mask.getWidth)(true)
          mem.writeMem(addr, data_byte_count, data, mask) // write operation
          data = Array.fill(data_byte_count)(0.toByte) // response = 0
          opcode_rsp = 0
        }
        case _ => {
          data = Array.fill(data_byte_count)(0.toByte)
        }

      }
      val rsp = (new TLBundleD_lite(parameters.l2cache_params).Lit(
        _.opcode -> opcode_rsp.U,
        _.data -> top.helper.ByteArray2BigInt(data).U,
        _.source -> source.U,
        _.size -> req.size.peek(),
        _.param -> req.param.peek()
      ))
      rsp
    }
  }
}
