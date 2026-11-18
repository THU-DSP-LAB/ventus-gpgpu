package play.cache

import chisel3._
import chisel3.experimental.BundleLiterals.AddBundleLiteralConstructor
import chisel3.util._
import chiseltest._
import org.scalatest.freespec.AnyFreeSpec
import top.parameters._
import L1Cache.DCache._
import L1Cache.AtomicUnit.AtomicUnit
import L1Cache.{ MyConfig,  RVGParameters }
import L2cache._
import chiseltest.{ChiselScalatestTester, WriteVcdAnnotation}
import config.config.Parameters

class AtomicUnitTest extends AnyFreeSpec with ChiselScalatestTester {
  implicit val p = Parameters.empty
  val params = InclusiveCacheParameters_lite(CacheParameters(2,l2cache_NSets,l2cache_NWays,num_l2cache,blockBytes=(l2cache_BlockWords<<2),beatBytes=(l2cache_BlockWords<<2)),InclusiveCacheMicroParameters(l2cache_writeBytes,l2cache_memCycles,l2cache_portFactor,num_warp,num_sm,num_sm_in_cluster,num_cluster,dcache_MshrEntry,dcache_NSets,atuns_NInfWriteEntry),false, MMU_ENABLED)
  val param = (new MyConfig).toInstance
  
  "AtomicUnit atomic Operations" in {
    test(new AtomicWrapper(params)(param)).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
      // Initialize signals
    dut.io.L12ATUmemReq.initSource().setSourceClock(dut.clock)
    //dut.io.L22ATUmemRsp.initSource().setSourceClock(dut.clock)
    //dut.io.ATU2L2memReq.initSink().setSinkClock(dut.clock)
    //dut.io.ATU2L1memRsp.initSink().setSinkClock(dut.clock)
    dut.io.ATU2L1memRsp.ready.poke(true.B)
      // Create two threads
        // Test LR operation
        dut.io.L12ATUmemReq.enqueue(new TLBundleA_lite(params).Lit(
          _.opcode -> 2.U,
          _.param -> 4.U, // arith add
          _.size -> 0.U,
          _.address -> "h10".U,
          _.data -> 4.U,
          _.mask -> "b0000_0000_0000_1111".U,
          _.source -> 1.U
        ))
        dut.clock.step(1)
        dut.io.ATU2L1memRsp.ready.poke(false.B)
        dut.io.L12ATUmemReq.enqueue(new TLBundleA_lite(params).Lit(
          _.opcode -> 1.U,
          _.param -> 0.U, // put
          _.size -> 0.U,
          _.address -> "h10".U,
          _.data -> 8.U,
          _.mask -> "b0000_0000_0000_1111".U,
          _.source -> 6.U
        ))
        dut.clock.step(1)
        dut.io.ATU2L1memRsp.ready.poke(true.B)
        dut.clock.step(5)
        
      
        
        // Send response
        
        dut.clock.step(6)
    }
  }
} 