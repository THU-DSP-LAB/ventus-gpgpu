package cta_test

import chisel3._
import chisel3.experimental.BundleLiterals.AddBundleLiteralConstructor
import chisel3.util._
import chiseltest._
import cta_scheduler._
import org.scalatest.freespec.AnyFreeSpec

import scala.util.Random

class MyException(msg: String) extends Exception(msg)
class ResourceConflictException(msg: String) extends Exception(msg) {
  def this(resource: String, wg1: Int, wftag1: Int, wg2: Int, wftag2: Int) = {
    this(s"ERROR: ${resource} conflicts between WG.WF ${wg1}.${wftag1} and ${wg2}.${wftag2}")
  }
}

class TestIn(testlen: Int) {
  val len = testlen
  val csr = Seq.tabulate(len){i => Random.nextInt().abs}
  val lds = Seq.tabulate(len){i =>  Random.nextInt(CONFIG.WG.NUM_LDS_MAX  / 2)}
  val sgpr = Seq.tabulate(len){i => Random.nextInt(CONFIG.WG.NUM_SGPR_MAX / (2*CONFIG.WG.NUM_WF_MAX))}
  val vgpr = Seq.tabulate(len){i => Random.nextInt(CONFIG.WG.NUM_VGPR_MAX / (2*CONFIG.WG.NUM_WF_MAX))}
  val wf = Seq.tabulate(len){i => Random.nextInt(CONFIG.WG.NUM_WF_MAX) + 1}
}

class Wf_slot {
  var valid: Boolean = false
  var wg_id: Int = _
  var wf_tag: Int = _
  var lds: (Int, Int) = _
  var sgpr: (Int, Int) = _
  var vgpr: (Int, Int) = _
  var csr: Int = _
  var time: Int = _
  assert(CONFIG.WG.NUM_WF_MAX == 32 && CONFIG.GPU.NUM_WG_SLOT == 8)
  def wf_id: Int = (wf_tag & 0x1F)
  def wg_slot: Int = (wf_tag >> 5) & 0x7
  def :=(that: Wf_slot): Unit = {
    valid = that.valid
    wg_id = that.wg_id
    wf_tag = that.wf_tag
    lds = that.lds
    sgpr = that.sgpr
    vgpr = that.vgpr
    csr = that.csr
    time = that.time
  }
}

class Cu(val cu_id: Int, testIn: TestIn, NUM_WF_SLOT: Int = CONFIG.GPU.NUM_WF_SLOT) {

  val wf_slot = Seq.fill(NUM_WF_SLOT)(new Wf_slot)

  var wf_new_last_valid = false
  var wf_new_last_wgid: Int = _
  var wf_new_last_wfid: Int = _
  var wf_new_last_csr: Int = _
  var wf_new_last_lds: (Int,Int) = _
  var wf_new_last_sgpr: (Int,Int) = _
  var wf_new_last_vgpr: (Int,Int) = _

  def update(): (Boolean, Int, Int) = {
    var wf_finish = false
    var wf_tag = 0
    var wg_id = 0
    for(i <- 0 until NUM_WF_SLOT) {
      if(wf_slot(i).valid) {
        if(wf_slot(i).time > 0) {
          wf_slot(i).time -= 1
        } else {
          wf_tag = if(wf_finish) wf_tag else wf_slot(i).wf_tag
          wg_id = if(wf_finish) wg_id else wf_slot(i).wg_id
          wf_finish = true
        }
      }
    }
    (wf_finish, wg_id, wf_tag)
  }
  def wf_check1(wf2: Wf_slot): Unit= {
    wf_slot.foreach { wf1 =>
      if(wf1.valid) {
        if((wf1.lds._1  != wf1.lds._2  + 1) && (wf1.lds._1  >= wf2.lds._1  && wf1.lds._1  <= wf2.lds._2 ) && wf1.wg_id != wf2.wg_id) { throw new ResourceConflictException("LDS", wf1.wg_id, wf1.wf_tag, wf2.wg_id, wf2.wf_tag) }
        if((wf1.sgpr._1 != wf1.sgpr._2 + 1) && (wf1.sgpr._1 >= wf2.sgpr._1 && wf1.sgpr._1 <= wf2.sgpr._2)) { throw new ResourceConflictException("sGPR", wf1.wg_id, wf1.wf_tag, wf2.wg_id, wf2.wf_tag) }
        if((wf1.vgpr._1 != wf1.vgpr._2 + 1) && (wf1.vgpr._1 >= wf2.vgpr._1 && wf1.vgpr._1 <= wf2.vgpr._2)) { throw new ResourceConflictException("vGPR", wf1.wg_id, wf1.wf_tag, wf2.wg_id, wf2.wf_tag) }
        if((wf2.lds._1  != wf2.lds._2  + 1) && (wf2.lds._1  >= wf1.lds._1  && wf2.lds._1  <= wf1.lds._2 ) && wf1.wg_id != wf2.wg_id) { throw new ResourceConflictException("LDS", wf1.wg_id, wf1.wf_tag, wf2.wg_id, wf2.wf_tag) }
        if((wf2.sgpr._1 != wf2.sgpr._2 + 1) && (wf2.sgpr._1 >= wf1.sgpr._1 && wf2.sgpr._1 <= wf1.sgpr._2)) { throw new ResourceConflictException("sGPR", wf1.wg_id, wf1.wf_tag, wf2.wg_id, wf2.wf_tag) }
        if((wf2.vgpr._1 != wf2.vgpr._2 + 1) && (wf2.vgpr._1 >= wf1.vgpr._1 && wf2.vgpr._1 <= wf1.vgpr._2)) { throw new ResourceConflictException("vGPR", wf1.wg_id, wf1.wf_tag, wf2.wg_id, wf2.wf_tag) }
      }
    }
  }
  def wf_check(wg_id: Int, wftag: Int, lds: (Int, Int), sgpr: (Int, Int), vgpr: (Int, Int)): Unit= {
    wf_slot.foreach {wf =>
      if(wf.valid) {
        if((wf.lds._1  != wf.lds._2  + 1) && (wf.lds._1  >= lds._1  && wf.lds._1  <= lds._2 ) && wf.wg_id != wg_id) { throw new ResourceConflictException("LDS", wf.wg_id, wf.wf_tag, wg_id, wftag) }
        if((wf.sgpr._1 != wf.sgpr._2 + 1) && (wf.sgpr._1 >= sgpr._1 && wf.sgpr._1 <= sgpr._2)) { throw new ResourceConflictException("sGPR", wf.wg_id, wf.wf_tag, wg_id, wftag) }
        if((wf.vgpr._1 != wf.vgpr._2 + 1) && (wf.vgpr._1 >= vgpr._1 && wf.vgpr._1 <= vgpr._2)) { throw new ResourceConflictException("vGPR", wf.wg_id, wf.wf_tag, wg_id, wftag) }
        if((lds._1  != lds._2  + 1) && (lds._1  >= wf.lds._1  && lds._1  <= wf.lds._2 ) && wf.wg_id != wg_id) { throw new ResourceConflictException("LDS", wf.wg_id, wf.wf_tag, wg_id, wftag) }
        if((sgpr._1 != sgpr._2 + 1) && (sgpr._1 >= wf.sgpr._1 && sgpr._1 <= wf.sgpr._2)) { throw new ResourceConflictException("sGPR", wf.wg_id, wf.wf_tag, wg_id, wftag) }
        if((vgpr._1 != vgpr._2 + 1) && (vgpr._1 >= wf.vgpr._1 && vgpr._1 <= wf.vgpr._2)) { throw new ResourceConflictException("vGPR", wf.wg_id, wf.wf_tag, wg_id, wftag) }
      }
    }
  }
  def wf_new(wf: Wf_slot): Unit = {
    wf_check1(wf)
    wf.valid = true
    wf.time = 200 + Random.nextInt(400)

    if(wf_new_last_valid && wf.wf_id == 0) {
      if(wf_new_last_wfid + 1 != testIn.wf(wf_new_last_wgid)) {
        throw new MyException(s"CU ${cu_id} WG ${wf_new_last_wgid} not complete: expect num_wf=${testIn.wf(wf_new_last_wgid)}, received ${wf_new_last_wfid+1}")
      }
      if(wf_new_last_lds._2 - wf_new_last_lds._1 + 1 != testIn.lds(wf_new_last_wgid)) {
        throw new MyException(s"CU ${cu_id} WG ${wf_new_last_wgid} resource LDS allocation error: expect num_lds=${testIn.lds(wf_new_last_wgid)}, got ${wf_new_last_lds}")
      }
      println(s"CU ${cu_id} receive WG ${wf_new_last_wgid}: LDS=${wf_new_last_lds}, SGPR=${wf_new_last_sgpr}, VGPR=${wf_new_last_vgpr}")
    } else if(wf_new_last_valid) {
      if(wf_new_last_wgid != wf.wg_id) {
        throw new MyException(s"CU ${cu_id} receives wrong WG: ${wf_new_last_wgid}, ${wf.wg_id}")
      }
      if(wf_new_last_sgpr._2 + 1 != wf.sgpr._1) {
        throw new MyException(s"CU ${cu_id} WG ${wf.wg_id} resource SGPR allocation error: WF ID ${wf_new_last_wfid} and ${wf.wf_id}")
      }
      if(wf_new_last_vgpr._2 + 1 != wf.vgpr._1) {
        throw new MyException(s"CU ${cu_id} WG ${wf.wg_id} resource VGPR allocation error: WF ID ${wf_new_last_wfid} and ${wf.wf_id}")
      }
    }
    wf_new_last_valid = true
    wf_new_last_wgid = wf.wg_id
    wf_new_last_wfid = wf.wf_id
    wf_new_last_csr = wf.csr
    wf_new_last_lds = (if(wf.wf_id==0) wf.lds._1 else wf_new_last_lds._1, wf.lds._2)
    wf_new_last_sgpr = (if(wf.wf_id==0) wf.sgpr._1 else wf_new_last_sgpr._1, wf.sgpr._2)
    wf_new_last_vgpr = (if(wf.wf_id==0) wf.vgpr._1 else wf_new_last_vgpr._1, wf.vgpr._2)
    for(i <- 0 until NUM_WF_SLOT) {
      if(!wf_slot(i).valid) {
        wf_slot(i) := wf
        return
      }
    }
    throw new MyException(s"ERROR: CU ${cu_id} WF slot not enough")
  }
  def wf_done(wf_tag: Int): Int = {
    for(i <- 0 until NUM_WF_SLOT) {
      val wf = wf_slot(i)
      if(wf.valid && wf.wf_tag == wf_tag && wf.time == 0) {
        wf_slot(i).valid = false
        return wf_slot(i).wg_id
      }
    }
    chisel3.assert(cond=false)
    0
  }
}

class Gpu(val testIn: TestIn) {
  val NUM_CU = CONFIG.GPU.NUM_CU
  val cu = Seq.tabulate(NUM_CU)(i => new Cu(cu_id = i, testIn))
  var wf_cnt = 0
  def wftag_decode(wftag: Int): (Int, Int) = {
    assert(CONFIG.WG.NUM_WF_MAX == 32 && CONFIG.GPU.NUM_WG_SLOT == 8)
    val wfid_bitmask = 0x1F
    val wgslot_bitmask = 0x7 << 5
    val wgslot = (wftag & wgslot_bitmask) >> 5
    val wfid = (wftag & wfid_bitmask)
    (wgslot, wfid)
  }

  def update(): Seq[(Boolean, Int, Int)] = {
    Seq.tabulate(NUM_CU)(i => cu(i).update())
  }
  def wf_new(cu_id: Int, wg: Wf_slot): Unit = {
    cu(cu_id).wf_new(wg)
    wf_cnt += 1
    //println(s"CU ${cu_id} exec WG ${wg_id} WF ${wf_id}: LDS=${lds}, SGPR=${sgpr}, VGPR=${vgpr}")
  }
  def wf_done(cu_id: Int, wf_tag: Int): Unit = {
    val wg_id = cu(cu_id).wf_done(wf_tag = wf_tag)
    wf_cnt -= 1
    //println(s"CU ${cu_id} done WG ${wg_id}")
  }
}

class test1 extends AnyFreeSpec with ChiselScalatestTester {
  "Test: CTA_scheduler" in {
    //test(new cta_scheduler_top()).withAnnotations(Seq(WriteVcdAnnotation, VerilatorBackendAnnotation)) { dut =>
    test(new cta_scheduler_top()).withAnnotations(Seq(VerilatorBackendAnnotation)) { dut =>
    //test(new cta_scheduler_top()).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
      dut.io.host_wg_new.initSource().setSourceClock(dut.clock)
      dut.io.host_wg_done.initSink().setSinkClock(dut.clock)
      dut.io.cu_wf_new.map(i => i.initSink().setSinkClock(dut.clock))
      dut.io.cu_wf_done.map(i => i.initSource().setSinkClock(dut.clock))

      val testlen = 2000
      val testIn = new TestIn(testlen)
      val testOut_wg = new Array[Boolean](testlen)

      val NUM_CU = CONFIG.GPU.NUM_CU
      val gpu = new Gpu(testIn)

      val testSeqIn = Seq.tabulate(testlen){i => (new io_host2cta).Lit(
        _.wg_id -> i.U,
        _.csr_kernel-> testIn.csr(i).U(CONFIG.GPU.MEM_ADDR_WIDTH),
        _.num_sgpr_per_wf -> testIn.sgpr(i).U,
        _.num_vgpr_per_wf -> testIn.vgpr(i).U,
        _.num_sgpr -> (testIn.sgpr(i) * testIn.wf(i)).U,
        _.num_vgpr -> (testIn.vgpr(i) * testIn.wf(i)).U,
        _.num_lds -> testIn.lds(i).U,
        _.num_wf -> testIn.wf(i).U,
        _.gds_base -> 0.U,
        _.num_gds -> 0.U,
        _.num_thread_per_wf -> 0.U,
        _.pds_base -> 0.U,
        _.start_pc -> 0.U,
        _.num_wg_x -> 0.U,
        _.num_wg_y -> 0.U,
        _.num_wg_z -> 0.U,
      ) }



      dut.io.host_wg_done.ready.poke(false.B)
      dut.io.host_wg_new.valid.poke(false.B)
      dut.clock.step(5)

      var cnt = 0
      fork{       // Host_wg_new
        dut.io.host_wg_new.enqueueSeq(testSeqIn)
      } .fork {   // CU interface
        while(cnt < testlen) {
          val wf_done_seq = gpu.update()
          for(i <- 0 until NUM_CU) {
            dut.io.cu_wf_done(i).valid.poke(wf_done_seq(i)._1.asBool)
            dut.io.cu_wf_done(i).bits.wf_tag.poke(wf_done_seq(i)._3.asUInt)
            if(CONFIG.DEBUG) { dut.io.cu_wf_done(i).bits.wg_id.get.poke(wf_done_seq(i)._2.asUInt) }
          }
          for(i <- 0 until NUM_CU) {
            // 这个循环不能与上一个融合，CU interface会从所有的wf_done(i).valid中Arbiter出一个作为处理对象
            // 必须等所有wf_done(i).valid更新完毕后才能读取wf_done(i).ready
            if(dut.io.cu_wf_done(i).valid.peek.litToBoolean && dut.io.cu_wf_done(i).ready.peek.litToBoolean) {
              gpu.wf_done(cu_id = i, wf_tag = wf_done_seq(i)._3)
            }
          }
          for(i <- 0 until NUM_CU) {
            val wf_new = dut.io.cu_wf_new(i)
            wf_new.ready.poke((Random.nextInt(10).abs < 2).B)
            val wf = new Wf_slot
            wf.wg_id = wf_new.bits.wg_id.peek.litValue.toInt
            wf.lds =  (wf_new.bits.lds_base.peek.litValue.toInt , wf_new.bits.lds_base.peek.litValue.toInt  + testIn.lds(wf.wg_id)  - 1)
            wf.sgpr = (wf_new.bits.sgpr_base.peek.litValue.toInt, wf_new.bits.sgpr_base.peek.litValue.toInt + testIn.sgpr(wf.wg_id) - 1)
            wf.vgpr = (wf_new.bits.vgpr_base.peek.litValue.toInt, wf_new.bits.vgpr_base.peek.litValue.toInt + testIn.vgpr(wf.wg_id) - 1)
            wf.wf_tag = wf_new.bits.wf_tag.peek.litValue.toInt
            wf.csr = wf_new.bits.csr_kernel.peek.litValue.toInt
            if(wf_new.valid.peek.litToBoolean && wf_new.ready.peek.litToBoolean) {
              gpu.wf_new(cu_id = i, wf)
            }
          }
          dut.clock.step()
        }
      } .fork {   // Host_wg_done
        dut.clock.step(70)
        while(cnt < testlen){
          dut.io.host_wg_done.ready.poke((scala.util.Random.nextBoolean() && scala.util.Random.nextBoolean()).B)
          if(dut.io.host_wg_done.valid.peek.litToBoolean && dut.io.host_wg_done.ready.peek.litToBoolean) {
            val wg_id = dut.io.host_wg_done.bits.wg_id.peek.litValue.toInt
            testOut_wg(wg_id) = true
            println(s"WG ${dut.io.host_wg_done.bits.wg_id.peek.litValue} finished")
            cnt = cnt + 1
          }
          dut.clock.step()
        }
      }.join

      assert(gpu.wf_cnt == 0)
      dut.clock.step(100)
    }
  }
}
