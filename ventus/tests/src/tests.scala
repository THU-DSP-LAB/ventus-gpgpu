package play

import L1Cache.MyConfig
import chisel3._
import chiseltest._
import org.scalatest.freespec.AnyFreeSpec
import chiseltest.simulator.WriteVcdAnnotation
import top._

// add new testcases here!
object TestCaseList{
  val L: Map[String, TestCase#Props] = Array[TestCase](
    new TestCase("gaussian", "gaussian_.vmem", "gaussian8.data", 8, 8, 0, 5000),
    new TestCase("saxpy", "saxpy_.vmem", "saxpy.data", 8, 8, 0, 400),
    new TestCase("gemm", "gemm_.vmem", "gemm4x8x4.data", 1, 8, 0, 2400),
    //new TestCase("gemm", "gemm_.vmem", "gemm8x16x12.data", 2, 8, 0, 300),
    new TestCase("saxpy2", "saxpy2_.vmem", "saxpy.data", 1, 8, 0, 800)
  ).map{x => (x.name, x.props)}.toMap

  def apply(s: String) = TestCaseList.L(s)
}

class hello_test2 extends AnyFreeSpec with ChiselScalatestTester{
  "first_test" in {
    val caseName = "saxpy2"
    test(new GPGPU_ExtMemWrapper(TestCaseList(caseName))).withAnnotations(Seq(WriteVcdAnnotation)) { c =>
      c.clock.setTimeout(0)
      c.clock.step(TestCaseList(caseName).cycles)
    }
  }
}
