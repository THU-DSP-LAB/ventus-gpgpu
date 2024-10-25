package play.cta

import chisel3._
import chisel3.experimental.BundleLiterals.AddBundleLiteralConstructor
import chisel3.util._
import chiseltest._
import org.scalatest.freespec.AnyFreeSpec
import cta._
import top.parameters.{CTA_SCHE_CONFIG => CONFIG}

import scala.util.Random

class MyException(msg: String) extends Exception(msg)
class ResourceConflictException(msg: String) extends MyException(msg) {
  def this(resource: String, wg1: Int, wftag1: Int, wg2: Int, wftag2: Int) = {
    this(s"ERROR: ${resource} conflicts between WG.WF ${wg1}.${wftag1} and ${wg2}.${wftag2}")
  }
}

object TESTCONFIG {
  val TESTLEN = 50
}
import TESTCONFIG.TESTLEN

class Test(val len: Int) {
  class TestIn(testlen: Int) {
    val random = Random
    val csr = Seq.tabulate(testlen){i => random.nextInt().abs}
    val num_thread_per_wg_x = Seq.tabulate(testlen){i => random.nextInt(4) + 1}
    val num_thread_per_wg_y = Seq.tabulate(testlen){i => random.nextInt(4) + 1}
    val num_thread_per_wg_z = Seq.tabulate(testlen){i => random.nextInt(4) + 1}
    val num_wg_x = Seq.tabulate(testlen){i => random.nextInt(4) + 1}
    val num_wg_y = Seq.tabulate(testlen){i => random.nextInt(4) + 1}
    val num_wg_z = Seq.tabulate(testlen){i => random.nextInt(4) + 1}
    val wgIdx_x = Seq.tabulate(testlen){i => random.nextInt(num_wg_x(i))}
    val wgIdx_y = Seq.tabulate(testlen){i => random.nextInt(num_wg_y(i))}
    val wgIdx_z = Seq.tabulate(testlen){i => random.nextInt(num_wg_z(i))}
    val threadIdx_offset_x = Seq.tabulate(testlen){i => 0}
    val threadIdx_offset_y = Seq.tabulate(testlen){i => 0}
    val threadIdx_offset_z = Seq.tabulate(testlen){i => 0}
    val wf_max = CONFIG.WG.NUM_WF_MAX                                           * 3 /  8
    val wf = Seq.tabulate(testlen){i => (num_thread_per_wg_x(i) * num_thread_per_wg_y(i) * num_thread_per_wg_z(i) + CONFIG.GPU.NUM_THREAD - 1) / CONFIG.GPU.NUM_THREAD}
    val lds = Seq.tabulate(testlen){i =>  random.nextInt(CONFIG.WG.NUM_LDS_MAX  * 3 / (8*wf(i))) * wf(i)}
    val sgpr = Seq.tabulate(testlen){i => random.nextInt(CONFIG.WG.NUM_SGPR_MAX * 3 / (8*wf(i)))}
    val vgpr = Seq.tabulate(testlen){i => random.nextInt(CONFIG.WG.NUM_VGPR_MAX * 3 / (8*wf(i)))}
    val wf_time = Seq.tabulate(testlen){i => Seq.tabulate(wf(i)){j => 100 + random.nextInt(200)}}
  }
  class TestExec(testlen: Int) {
    val valid = Array.fill(testlen)(false)
    val cu = new Array[Int](testlen)
    val wf_cnt = Array.fill(testlen)(0)
    val lds = new Array[(Int,Int)](testlen)
    val sgpr = new Array[(Int,Int)](testlen)
    val vgpr = new Array[(Int,Int)](testlen)
    val csr = new Array[Int](testlen)
  }
  val in = new TestIn(len)
  val wg_exec = new TestExec(len)
}

class Wf_slot {
  var valid: Boolean = false
  val threadIdxL_3d = new Array[(Int,Int,Int)](CONFIG.GPU.NUM_THREAD)
  val threadIdxL_1d = new Array[Int](CONFIG.GPU.NUM_THREAD)
  val threadIdxG_3d = new Array[(Int,Int,Int)](CONFIG.GPU.NUM_THREAD)
  val threadIdxG_1d = new Array[Int](CONFIG.GPU.NUM_THREAD)
  var wg_id: Int = _
  var wf_tag: Int = _
  var lds: (Int, Int) = _
  var sgpr: (Int, Int) = _
  var vgpr: (Int, Int) = _
  var csr: Int = _
  var time: Int = _
  assert(CONFIG.WG.NUM_WF_MAX == 8 && CONFIG.GPU.NUM_WG_SLOT == 8)
  def wf_id: Int = (wf_tag & 0x7)
  def wg_slot: Int = (wf_tag >> 3) & 0x7
  def :=(that: Wf_slot): Unit = {
    valid = that.valid
    wg_id = that.wg_id
    wf_tag = that.wf_tag
    lds = that.lds
    sgpr = that.sgpr
    vgpr = that.vgpr
    csr = that.csr
    time = that.time
    for(i <- 0 until CONFIG.GPU.NUM_THREAD) {
      threadIdxL_3d(i) = that.threadIdxL_3d(i)
      threadIdxL_1d(i) = that.threadIdxL_1d(i)
      threadIdxG_3d(i) = that.threadIdxG_3d(i)
      threadIdxG_1d(i) = that.threadIdxG_1d(i)
    }
  }
  def check(): Unit = {
  }
}

class Cu(val cu_id: Int, test: Test, NUM_WF_SLOT: Int = CONFIG.GPU.NUM_WF_SLOT) {

  val wf_slot = Seq.fill(NUM_WF_SLOT)(new Wf_slot)

  var wf_new_last_valid = false
  var wf_new_last_wgid: Int = _
  var wf_new_last_wfid: Int = _
  var wf_new_last_csr: Int = _
  var wf_new_last_lds: (Int,Int) = _
  var wf_new_last_sgpr: (Int,Int) = _
  var wf_new_last_vgpr: (Int,Int) = _

  def update(): (Boolean, Int, Int) = { // 仿真步进，判断哪些WF执行完毕，并返回其中一个
    var wf_finish = false
    var wf_tag = 0
    var wg_id = 0
    for(i <- 0 until NUM_WF_SLOT) {
      if(wf_slot(i).valid) {
        if(wf_slot(i).time > 0) {
          wf_slot(i).time -= 1    // WF执行剩余时间--
        } else { // 在执行完毕的WF中选择序号最靠前的一个返回给CTA_sche
          wf_tag = if(wf_finish) wf_tag else wf_slot(i).wf_tag
          wg_id = if(wf_finish) wg_id else wf_slot(i).wg_id
          wf_finish = true
        }
      }
    }
    (wf_finish, wg_id, wf_tag)
  }
  def wf_check(wf2: Wf_slot): Unit = {
    val wg_id = wf2.wg_id
    for(i <- 0 until CONFIG.GPU.NUM_THREAD - 1) {
      if(wf2.threadIdxL_1d(i) + 1 != wf2.threadIdxL_1d(i+1)) { // threadIdxL_linear应当序号连续
        new MyException(s"WF threadIdx-local-linear incorrect: ${wf2.threadIdxL_1d}")
      }
      if(wf2.wf_id != wf2.threadIdxL_1d(0) / CONFIG.GPU.NUM_THREAD) { // 首个threadIdxL应当与wf_id匹配
        new MyException(s"WF ID ${wf2.wf_id} doesn't match local threadIdx-linear ${wf2.threadIdxL_1d(0)}~${wf2.threadIdxL_1d.last}")
      }
      // threadIdxL_{x,y,z}正确
      if(wf2.threadIdxL_3d(i)._1 != (wf2.wf_id * CONFIG.GPU.NUM_THREAD + i) % (test.in.num_thread_per_wg_x(wf2.wg_id)) ||
         wf2.threadIdxL_3d(i)._2 != (wf2.wf_id * CONFIG.GPU.NUM_THREAD + i) % (test.in.num_thread_per_wg_x(wf2.wg_id) * test.in.num_thread_per_wg_y(wf2.wg_id)) / test.in.num_thread_per_wg_x(wf2.wg_id) ||
         wf2.threadIdxL_3d(i)._3 != (wf2.wf_id * CONFIG.GPU.NUM_THREAD + i) / (test.in.num_thread_per_wg_x(wf2.wg_id) * test.in.num_thread_per_wg_y(wf2.wg_id))
      ) { new MyException(s"local threadIdx-3d incorrect or don't match WF_ID ${wf2.wf_id}: ${wf2.threadIdxL_3d}") }
      // threadIdxG_{x,y,z}正确
      if(wf2.threadIdxG_3d(i)._1 != (test.in.wgIdx_x(wg_id) * test.in.num_thread_per_wg_x(wg_id) + wf2.threadIdxL_3d(i)._1 + test.in.threadIdx_offset_x(wg_id)) ||
         wf2.threadIdxG_3d(i)._2 != (test.in.wgIdx_y(wg_id) * test.in.num_thread_per_wg_y(wg_id) + wf2.threadIdxL_3d(i)._2 + test.in.threadIdx_offset_y(wg_id)) ||
         wf2.threadIdxG_3d(i)._3 != (test.in.wgIdx_z(wg_id) * test.in.num_thread_per_wg_z(wg_id) + wf2.threadIdxL_3d(i)._3 + test.in.threadIdx_offset_z(wg_id))
      ) { new MyException(s"global threadIdx-3d incorrect or don't match blockIdx ${(test.in.wgIdx_x(wg_id),test.in.wgIdx_y(wg_id),test.in.wgIdx_z(wg_id))} or offset: ${wf2.threadIdxG_3d}") }
      if(wf2.threadIdxG_1d(i) != (wf2.threadIdxG_3d(i)._1 - test.in.threadIdx_offset_x(wg_id)) +
         (wf2.threadIdxG_3d(i)._2 - test.in.threadIdx_offset_y(wg_id))  * test.in.num_thread_per_wg_x(wg_id) * test.in.num_wg_x(wg_id) +
         (wf2.threadIdxG_3d(i)._3 - test.in.threadIdx_offset_z(wg_id))  * test.in.num_thread_per_wg_x(wg_id) * test.in.num_wg_x(wg_id) * test.in.num_thread_per_wg_y(wg_id) * test.in.num_wg_y(wg_id)
      ) {
        new MyException(s"global threadIdx-1d ${wf2.threadIdxG_1d} not match threadIdxG_3d ${wf2.threadIdxG_3d} - offset ${(test.in.threadIdx_offset_x,test.in.threadIdx_offset_y,test.in.threadIdx_offset_z)}")
      }
    }
    new MyException(s"Try exception")

    // 检验新来的WF的LDS/sGPR/vGPR资源占用是否与其它WF有重叠
    wf_slot.foreach { wf1 =>
      if(wf1.valid) {
        // |--------------------检验资源占用是否重叠--------------------|    |--旧WF对此资源的耗用为0时跳过检查--|   |-同WG的LDS共享，跳过检查-|
        if((wf1.lds._1  >= wf2.lds._1  && wf1.lds._1  <= wf2.lds._2 ) && (wf1.lds._1  != wf1.lds._2  + 1) && wf1.wg_id != wf2.wg_id) { throw new ResourceConflictException("LDS", wf1.wg_id, wf1.wf_tag, wf2.wg_id, wf2.wf_tag) }
        if((wf1.sgpr._1 >= wf2.sgpr._1 && wf1.sgpr._1 <= wf2.sgpr._2) && (wf1.sgpr._1 != wf1.sgpr._2 + 1)) { throw new ResourceConflictException("sGPR", wf1.wg_id, wf1.wf_tag, wf2.wg_id, wf2.wf_tag) }
        if((wf1.vgpr._1 >= wf2.vgpr._1 && wf1.vgpr._1 <= wf2.vgpr._2) && (wf1.vgpr._1 != wf1.vgpr._2 + 1)) { throw new ResourceConflictException("vGPR", wf1.wg_id, wf1.wf_tag, wf2.wg_id, wf2.wf_tag) }
        if((wf2.lds._1  >= wf1.lds._1  && wf2.lds._1  <= wf1.lds._2 ) && (wf2.lds._1  != wf2.lds._2  + 1) && wf1.wg_id != wf2.wg_id) { throw new ResourceConflictException("LDS", wf1.wg_id, wf1.wf_tag, wf2.wg_id, wf2.wf_tag) }
        if((wf2.sgpr._1 >= wf1.sgpr._1 && wf2.sgpr._1 <= wf1.sgpr._2) && (wf2.sgpr._1 != wf2.sgpr._2 + 1)) { throw new ResourceConflictException("sGPR", wf1.wg_id, wf1.wf_tag, wf2.wg_id, wf2.wf_tag) }
        if((wf2.vgpr._1 >= wf1.vgpr._1 && wf2.vgpr._1 <= wf1.vgpr._2) && (wf2.vgpr._1 != wf2.vgpr._2 + 1)) { throw new ResourceConflictException("vGPR", wf1.wg_id, wf1.wf_tag, wf2.wg_id, wf2.wf_tag) }
      }
    }
  }
  def wf_new(wf: Wf_slot): Unit = { // 向CU中新增WF，WF信息已被从硬件信号转化为软件class
    if(wf.wg_id < 0 || wf.wg_id >= test.len) {throw new MyException(s"WG ID ERROR: ${wf.wg_id}, expect WG ID Max = ${test.len - 1}")}
    //wf_check(wf) // 检测新WF的LDS/sGPR/vGPR资源分配无误
    wf.valid = true

    val wf_cnt = test.wg_exec.wf_cnt(wf.wg_id) + 1

    if(wf.wf_id != test.wg_exec.wf_cnt(wf.wg_id)) {
      throw new MyException(s"Warning: WG ${wf.wg_id} WF not dispatched in order, expect WF_ID=${test.wg_exec.wf_cnt(wf.wg_id)}, received WF_ID=${wf.wf_id}")
    } else test.wg_exec.wf_cnt(wf.wg_id) += 1
    if(wf.csr != test.in.csr(wf.wg_id)) {
      throw new MyException(s"CU ${cu_id} WG ${wf.wg_id} WF ${wf.wf_id} CSR error: expect csr=${test.in.csr(wf.wg_id)}, got ${wf.csr}")
    } else test.wg_exec.csr(wf.wg_id) = wf.csr
    if(wf.lds._2 - wf.lds._1 + 1 != test.in.lds(wf.wg_id)) {
      throw new MyException(s"CU ${cu_id} WG ${wf.wg_id} WF ${wf.wf_id} resource LDS allocation error: expect num_lds=${test.in.lds(wf.wg_id)}, got ${wf.lds}")
    }
    if(wf.sgpr._2 - wf.sgpr._1 + 1 != test.in.sgpr(wf.wg_id)) {
      throw new MyException(s"CU ${cu_id} WG ${wf.wg_id} resource SGPR allocation error: expect num_sgpr_per_wf=${test.in.sgpr(wf.wg_id)}, got ${wf.sgpr}")
    }
    if(wf.vgpr._2 - wf.vgpr._1 + 1 != test.in.vgpr(wf.wg_id)) {
      throw new MyException(s"CU ${cu_id} WG ${wf.wg_id} resource VGPR allocation error: expect num_vgpr_per_wf=${test.in.vgpr(wf.wg_id)}, got ${wf.vgpr}")
    }
    if(test.wg_exec.valid(wf.wg_id)) {  // For wf_id != 0
      if(test.wg_exec.cu(wf.wg_id) != cu_id){
        throw new MyException(s"WG ${wf.wg_id} dispatched to more than 1 CU: ${test.wg_exec.cu(wf.wg_id)} and ${cu_id}")
      }
      if(test.wg_exec.lds(wf.wg_id) != wf.lds) {
        throw new MyException(s"CU ${cu_id} WG ${wf.wg_id} resource LDS allocation error: WF ID ${wf_cnt-1} and ${wf.wf_id}, LDS ${test.wg_exec.lds(wf.wg_id)} VS ${wf.lds}")
      }
      if(test.wg_exec.sgpr(wf.wg_id)._2 + 1 != wf.sgpr._1) {
        throw new MyException(s"CU ${cu_id} WG ${wf.wg_id} resource SGPR allocation error: WF ID ${wf_cnt-1} and ${wf.wf_id}")
      } else test.wg_exec.sgpr(wf.wg_id) = (test.wg_exec.sgpr(wf.wg_id)._1, wf.sgpr._2)
      if(test.wg_exec.vgpr(wf.wg_id)._2 + 1 != wf.vgpr._1) {
        throw new MyException(s"CU ${cu_id} WG ${wf.wg_id} resource VGPR allocation error: WF ID ${wf_cnt-1} and ${wf.wf_id}")
      } else test.wg_exec.vgpr(wf.wg_id) = (test.wg_exec.vgpr(wf.wg_id)._1, wf.vgpr._2)
  } else {
      assert(wf.wf_id == 0)
      test.wg_exec.cu(wf.wg_id) = cu_id
      test.wg_exec.lds(wf.wg_id) = wf.lds
      test.wg_exec.sgpr(wf.wg_id) = wf.sgpr
      test.wg_exec.vgpr(wf.wg_id) = wf.vgpr
    }

    if(wf_cnt == test.in.wf(wf.wg_id)) {  // This WG has finished its dispatch
      println(s"CU ${cu_id} receive WG ${wf.wg_id}: LDS=${wf.lds}, SGPR=${test.wg_exec.sgpr(wf.wg_id)}, VGPR=${test.wg_exec.vgpr(wf.wg_id)}")
    } else if(wf_cnt > test.in.wf(wf.wg_id)) {
      throw new MyException(s"CU ${cu_id} WG ${wf.wg_id} got too much WF: expect num_wf=${test.in.wf(wf.wg_id)}, got WF_ID=${wf.wf_id}")
    }

    if(wf_new_last_valid && wf.wf_id == 0) {
      if(wf_new_last_wfid + 1 != test.in.wf(wf_new_last_wgid)) {
        throw new MyException(s"CU ${cu_id} WG ${wf_new_last_wgid} not complete: expect num_wf=${test.in.wf(wf_new_last_wgid)}, received ${wf_new_last_wfid+1}")
      }
    }
    wf_new_last_valid = true
    wf_new_last_wgid = wf.wg_id
    wf_new_last_wfid = wf.wf_id
    for(i <- 0 until NUM_WF_SLOT) {
      if(!wf_slot(i).valid) {
        test.wg_exec.valid(wf.wg_id) = true
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

class Gpu(val test: Test) {
  val NUM_CU = CONFIG.GPU.NUM_CU
  val wg_wf_cnt = Array.fill(test.len)(0)
  val cu = Seq.tabulate(NUM_CU)(i => new Cu(cu_id = i, test = test))
  var gpu_wf_cnt = 0
  def update(): Seq[(Boolean, Int, Int)] = {
    Seq.tabulate(NUM_CU)(i => cu(i).update())
  }
  def wf_new(cu_id: Int, wg: Wf_slot): Unit = {
    cu(cu_id).wf_new(wg)
    gpu_wf_cnt += 1
  }
  def wf_done(cu_id: Int, wf_tag: Int): Unit = {
    val wg_id = cu(cu_id).wf_done(wf_tag = wf_tag)
    gpu_wf_cnt -= 1
  }
  def final_check(): Unit = {
    assert(gpu_wf_cnt == 0)
    assert(!test.wg_exec.valid.contains(false))
    assert(test.wg_exec.wf_cnt.zipWithIndex.forall{ case (wf_cnt, wg_id) => (wf_cnt == test.in.wf(wg_id)) })
    assert(test.wg_exec.lds.zipWithIndex.forall{ case (lds, wg_id) => (lds._2 - lds._1 + 1 == test.in.lds(wg_id)) })
    assert(test.wg_exec.sgpr.zipWithIndex.forall{ case (sgpr, wg_id) => (sgpr._2 - sgpr._1 + 1 == test.in.sgpr(wg_id) * test.in.wf(wg_id)) })
    assert(test.wg_exec.vgpr.zipWithIndex.forall{ case (vgpr, wg_id) => (vgpr._2 - vgpr._1 + 1 == test.in.vgpr(wg_id) * test.in.wf(wg_id)) })
  }
}

class RunCtaTests extends AnyFreeSpec with ChiselScalatestTester {
  "Test: CTA_scheduler" in {
    test(new cta_scheduler_top()).withAnnotations(Seq(WriteVcdAnnotation, VerilatorBackendAnnotation)) { dut =>
    //test(new cta_scheduler_top()).withAnnotations(Seq(VerilatorBackendAnnotation)) { dut =>
    //test(new cta_scheduler_top()).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
    //test(new cta_scheduler_top()).withAnnotations(Seq()) { dut =>
      dut.io.host_wg_new.initSource().setSourceClock(dut.clock)
      dut.io.host_wg_done.initSink().setSinkClock(dut.clock)
      dut.io.cu_wf_new.map(i => i.initSink().setSinkClock(dut.clock))
      dut.io.cu_wf_done.map(i => i.initSource().setSinkClock(dut.clock))

      val testlen = TESTLEN
      val test = new Test(testlen)
      val testOut_wg = new Array[Boolean](testlen)

      val NUM_CU = CONFIG.GPU.NUM_CU
      val gpu = new Gpu(test)

      val testSeqIn = Seq.tabulate(testlen){i => (new io_host2cta).Lit(
        _.wg_id -> i.U,
        _.csr_kernel-> test.in.csr(i).U(CONFIG.GPU.MEM_ADDR_WIDTH),
        _.num_sgpr_per_wf -> test.in.sgpr(i).U,
        _.num_vgpr_per_wf -> test.in.vgpr(i).U,
        _.num_pds_per_wf -> i.U,
        _.num_sgpr -> (test.in.sgpr(i) * test.in.wf(i)).U,
        _.num_vgpr -> (test.in.vgpr(i) * test.in.wf(i)).U,
        _.num_lds -> test.in.lds(i).U,
        _.num_wf -> test.in.wf(i).U,
        _.gds_base -> 0.U,
        _.num_thread_per_wf -> 0.U,
        _.pds_base -> 0.U,
        _.start_pc -> 0.U,
        _.num_wg_x -> (test.in.num_wg_x(i)).U,
        _.num_wg_y -> (test.in.num_wg_y(i)).U,
        _.num_wg_z -> (test.in.num_wg_z(i)).U,
        //(_.asid_kernel.get) -> 0.U,
        _.num_thread_per_wg_x -> (test.in.num_thread_per_wg_x(i)).U,
        _.num_thread_per_wg_y -> (test.in.num_thread_per_wg_y(i)).U,
        _.num_thread_per_wg_z -> (test.in.num_thread_per_wg_z(i)).U,
        _.wgIdx_x -> (test.in.wgIdx_x(i)).U,
        _.wgIdx_y -> (test.in.wgIdx_y(i)).U,
        _.wgIdx_z -> (test.in.wgIdx_z(i)).U,
        _.threadIdx_in_grid_offset_x -> (test.in.threadIdx_offset_x(i)).U,
        _.threadIdx_in_grid_offset_y -> (test.in.threadIdx_offset_y(i)).U,
        _.threadIdx_in_grid_offset_z -> (test.in.threadIdx_offset_z(i)).U,
      ) }

      println("CTA Scheduler Simulation begin")

      dut.io.host_wg_done.ready.poke(false.B)
      dut.io.host_wg_new.valid.poke(false.B)
      dut.clock.step(5)

      var cnt = 0
      var clk_cnt = 0
      fork{       // Host_wg_new
        dut.io.host_wg_new.enqueueSeq(testSeqIn)
      } .fork {   // CU interface
        while(cnt < testlen) {
          val wf_done_seq = gpu.update()
          for(i <- 0 until NUM_CU) {
            dut.io.cu_wf_done(i).valid.poke(wf_done_seq(i)._1.asBool)
            dut.io.cu_wf_done(i).bits.wf_tag.poke(wf_done_seq(i)._3.asUInt)
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
            wf_new.ready.poke(Random.nextBoolean().B)
            if(wf_new.valid.peek.litToBoolean && wf_new.ready.peek.litToBoolean) {
              val wf = new Wf_slot
              wf.wg_id = wf_new.bits.wg_id.peek.litValue.toInt
              wf.lds =  (wf_new.bits.lds_base.peek.litValue.toInt , wf_new.bits.lds_base.peek.litValue.toInt  + test.in.lds(wf.wg_id)  - 1)
              wf.sgpr = (wf_new.bits.sgpr_base.peek.litValue.toInt, wf_new.bits.sgpr_base.peek.litValue.toInt + test.in.sgpr(wf.wg_id) - 1)
              wf.vgpr = (wf_new.bits.vgpr_base.peek.litValue.toInt, wf_new.bits.vgpr_base.peek.litValue.toInt + test.in.vgpr(wf.wg_id) - 1)
              wf.wf_tag = wf_new.bits.wf_tag.peek.litValue.toInt
              wf.csr = wf_new.bits.csr_kernel.peek.litValue.toInt
              for(t <- 0 until CONFIG.GPU.NUM_THREAD) {
                wf.threadIdxG_3d(t) = (wf_new.bits.threadIdx_in_grid_x(t).peek.litValue.toInt, wf_new.bits.threadIdx_in_grid_y(t).peek.litValue.toInt, wf_new.bits.threadIdx_in_grid_z(t).peek.litValue.toInt)
                wf.threadIdxL_3d(t) = (wf_new.bits.threadIdx_in_wg_x(t).peek.litValue.toInt, wf_new.bits.threadIdx_in_wg_y(t).peek.litValue.toInt, wf_new.bits.threadIdx_in_wg_z(t).peek.litValue.toInt)
                wf.threadIdxG_1d(t) = wf_new.bits.threadIdx_in_grid(t).peek.litValue.toInt
                wf.threadIdxL_1d(t) = wf_new.bits.threadIdx_in_wg(t).peek.litValue.toInt
              }
              wf.time = test.in.wf_time(wf.wg_id)(wf.wf_id)
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
      } .fork {
        while(cnt < testlen) {
          clk_cnt += 1
          if(clk_cnt % 10000 == 0) { println(s"clock step ${clk_cnt} cycles") }
          dut.clock.step()
        }
      }.join

      gpu.final_check()
      dut.clock.step(100)
      println(s"===== CTA Simulation passed in ${clk_cnt} cycles =====")
    }
  }
}
