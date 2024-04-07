package Memtest

import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec

class CombinedMemTestSpec extends AnyFlatSpec with ChiselScalatestTester {
  behavior of "Combined Memory Test"

  it should "test both MemExample and SeqMemExample with the same inputs" in {
    // 测试MemExample模块
    test(new MemExample(1024)).withAnnotations(Seq(WriteVcdAnnotation)) { memExample =>
      memExample.io.addr.poke(0.U)
      memExample.io.writeData.poke(123.U)
      memExample.io.writeEnable.poke(true.B)
      memExample.clock.step(1) // 执行写操作

      memExample.io.writeEnable.poke(false.B)
      memExample.clock.step(1) // 等待一个时钟周期

      memExample.io.addr.poke(0.U)
    }

    // 测试SeqMemExample模块
    test(new SeqMemExample(1024)).withAnnotations(Seq(WriteVcdAnnotation)) { seqMemExample =>
      seqMemExample.io.addr.poke(0.U)
      seqMemExample.io.writeData.poke(123.U)
      seqMemExample.io.writeEnable.poke(true.B)
      seqMemExample.clock.step(1) // 执行写操作

      seqMemExample.io.writeEnable.poke(false.B)
      seqMemExample.clock.step(1) // 等待一个时钟周期

      seqMemExample.io.addr.poke(0.U)
    }
  }
}

