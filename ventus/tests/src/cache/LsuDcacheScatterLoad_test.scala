package play.cache

import L1Cache.DCache.DataCachev2
import L1Cache.{DCacheMemReq, DCacheMemRsp, MyConfig}
import chisel3._
import chisel3.util.Decoupled
import chiseltest._
import config.config.Parameters
import org.scalatest.freespec.AnyFreeSpec
import pipeline.{LSUexe, MSHROutput, vExeData}
import top.parameters._

/** Connects the real LSU and DCache to cover scattered texture-style loads.
  *
  * Unit tests of either side alone cannot detect a lost lane mask at their
  * bundle boundary or a replay/fill interaction.  The memory port is exposed
  * so the test can return deterministic cache-line data in any timing.
  */
class LsuDcacheScatterHarness(implicit p: Parameters) extends Module {
  val io = IO(new Bundle {
    val req = Flipped(Decoupled(new vExeData))
    val rsp = Decoupled(new MSHROutput)
    val memReq = Decoupled(new DCacheMemReq)
    val memRsp = Flipped(Decoupled(new DCacheMemRsp))
    val lsuDcacheFire = Output(Bool())
    val dcacheLsuFire = Output(Bool())
    val lsuDcacheValid = Output(Bool())
    val lsuDcacheReady = Output(Bool())
  })
  val lsu = Module(new LSUexe)
  val dcache = Module(new DataCachev2(None))

  lsu.io.lsu_req <> io.req
  io.rsp <> lsu.io.lsu_rsp
  lsu.io.cachelineClientReq.valid := false.B
  lsu.io.cachelineClientReq.bits := 0.U.asTypeOf(lsu.io.cachelineClientReq.bits)
  lsu.io.cachelineClientRsp.ready := true.B
  lsu.io.shared_req.ready := true.B
  lsu.io.shared_rsp.valid := false.B
  lsu.io.shared_rsp.bits := 0.U.asTypeOf(lsu.io.shared_rsp.bits)
  lsu.io.flush_dcache.valid := false.B
  lsu.io.flush_dcache.bits := false.B
  lsu.io.csr_pds := 0.U
  lsu.io.csr_numw := 1.U
  lsu.io.csr_numt := 1.U
  lsu.io.csr_tid := 0.U

  dcache.io.coreReq.valid := lsu.io.dcache_req.valid
  dcache.io.coreReq.bits.instrId := lsu.io.dcache_req.bits.instrId
  dcache.io.coreReq.bits.opcode := lsu.io.dcache_req.bits.opcode
  dcache.io.coreReq.bits.param := lsu.io.dcache_req.bits.param
  dcache.io.coreReq.bits.tag := lsu.io.dcache_req.bits.tag
  dcache.io.coreReq.bits.setIdx := lsu.io.dcache_req.bits.setIdx
  dcache.io.coreReq.bits.perLaneAddr := lsu.io.dcache_req.bits.perLaneAddr
  dcache.io.coreReq.bits.data := lsu.io.dcache_req.bits.data
  dcache.io.coreReq.bits.asid.foreach(_ := 0.U)
  dcache.io.coreReq.bits.spike_info.foreach(_ := 0.U.asTypeOf(dcache.io.coreReq.bits.spike_info.get))
  lsu.io.dcache_req.ready := dcache.io.coreReq.ready
  lsu.io.dcache_rsp.valid := dcache.io.coreRsp.valid
  lsu.io.dcache_rsp.bits.instrId := dcache.io.coreRsp.bits.instrId
  lsu.io.dcache_rsp.bits.data := dcache.io.coreRsp.bits.data
  lsu.io.dcache_rsp.bits.activeMask := dcache.io.coreRsp.bits.activeMask
  dcache.io.coreRsp.ready := lsu.io.dcache_rsp.ready
  io.lsuDcacheFire := lsu.io.dcache_req.fire
  io.dcacheLsuFire := lsu.io.dcache_rsp.fire
  io.lsuDcacheValid := lsu.io.dcache_req.valid
  io.lsuDcacheReady := lsu.io.dcache_req.ready
  io.memReq <> dcache.io.memReq.get
  dcache.io.memRsp <> io.memRsp
  dcache.io.perfEnable := false.B
  dcache.io.perfReset := false.B
}

class LsuDcacheScatterLoadTest extends AnyFreeSpec with ChiselScalatestTester {
  implicit private val p: Parameters = (new MyConfig).toInstance

  private def zero(data: Data): Unit = data match {
    case bool: Bool => bool.poke(false.B)
    case uint: UInt => uint.poke(0.U)
    case aggregate: Aggregate => aggregate.getElements.foreach(zero)
  }

  private def driveMemoryIdle(dut: LsuDcacheScatterHarness): Unit = {
    dut.io.memRsp.valid.poke(false.B)
    zero(dut.io.memRsp.bits)
  }

  "real LSU and DCache preserve a scattered eight-line vector load" in {
    test(new LsuDcacheScatterHarness).withAnnotations(Seq(VerilatorBackendAnnotation)) { dut =>
      val addresses = (0 until 32).map { lane =>
        val line = lane / 4
        val word = (lane * 7 + 3) % 32
        lane -> (BigInt("90008000", 16) + line * 128 + word * 4)
      }.toMap
      zero(dut.io.req.bits)
      dut.io.req.valid.poke(false.B)
      dut.io.rsp.ready.poke(true.B)
      dut.io.memReq.ready.poke(true.B)
      driveMemoryIdle(dut)
      dut.clock.setTimeout(0)
      dut.clock.step(2)

      addresses.foreach { case (lane, address) =>
        dut.io.req.bits.in1(lane).poke(address.U)
        dut.io.req.bits.mask(lane).poke(true.B)
      }
      dut.io.req.bits.ctrl.isvec.poke(true.B)
      dut.io.req.bits.ctrl.mem_cmd.poke(1.U)
      dut.io.req.bits.ctrl.mem_whb.poke(3.U)
      dut.io.req.bits.ctrl.mem_unsigned.poke(true.B)
      dut.io.req.bits.ctrl.mop.poke(1.U)
      dut.io.req.valid.poke(true.B)
      dut.io.req.ready.expect(true.B)
      dut.clock.step()
      dut.io.req.valid.poke(false.B)

      var pending: Option[(BigInt, BigInt)] = None
      var cycles = 0
      var memoryRequests = 0
      var memoryResponses = 0
      var lsuDcacheRequests = 0
      var dcacheLsuResponses = 0
      var sawLsuDcacheValid = false
      var sawLsuDcacheReady = false
      while (!dut.io.rsp.valid.peek().litToBoolean && cycles < 3000) {
        sawLsuDcacheValid ||= dut.io.lsuDcacheValid.peek().litToBoolean
        sawLsuDcacheReady ||= dut.io.lsuDcacheReady.peek().litToBoolean
        if (dut.io.lsuDcacheFire.peek().litToBoolean) lsuDcacheRequests += 1
        if (dut.io.dcacheLsuFire.peek().litToBoolean) dcacheLsuResponses += 1
        dut.io.memReq.ready.poke(pending.isEmpty.B)
        pending match {
          case Some((source, address)) if dut.io.memRsp.ready.peek().litToBoolean =>
            dut.io.memRsp.bits.d_opcode.poke(1.U)
            dut.io.memRsp.bits.d_param.poke(0.U)
            dut.io.memRsp.bits.d_source.poke(source.U)
            dut.io.memRsp.bits.d_addr.poke(address.U)
            val lineData = ((address >> 7) & 0xffff) << 8
            dut.io.memRsp.bits.d_data.zipWithIndex.foreach { case (word, index) =>
              word.poke((lineData + index).U)
            }
            dut.io.memRsp.valid.poke(true.B)
            dut.clock.step()
            driveMemoryIdle(dut)
            pending = None
            memoryResponses += 1
          case None if dut.io.memReq.valid.peek().litToBoolean =>
            val source = dut.io.memReq.bits.a_source.peek().litValue
            val address = dut.io.memReq.bits.a_addr.get.peek().litValue
            dut.clock.step()
            pending = Some((source, address))
            memoryRequests += 1
          case _ => dut.clock.step()
        }
        cycles += 1
      }
      assert(cycles < 3000,
          s"scattered LSU/DCache load did not complete after $cycles cycles " +
          s"(LSU->DCache valid=$sawLsuDcacheValid ready=$sawLsuDcacheReady; " +
          s"$lsuDcacheRequests LSU requests, $dcacheLsuResponses DCache responses, " +
          s"$memoryRequests memory requests, $memoryResponses fills, pending=$pending)")
      dut.io.rsp.bits.tag.mask.foreach(_.expect(true.B))
      addresses.foreach { case (lane, address) =>
        val expected = (((address >> 7) & 0xffff) << 8) + ((address >> 2) & 31)
        dut.io.rsp.bits.data(lane).expect(expected.U)
      }
    }
  }
}
