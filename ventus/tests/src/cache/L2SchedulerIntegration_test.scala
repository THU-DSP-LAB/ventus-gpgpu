package play.cache

import L2cache._
import chisel3._
import chisel3.util._
import chiseltest._
import chiseltest.experimental.expose
import freechips.rocketchip.tilelink.TLMessages.{AccessAckData, Get, Hint}
import org.scalatest.freespec.AnyFreeSpec

class SchedulerIntegrationHarness(params: InclusiveCacheParameters_lite) extends Module {
  val io = IO(new Bundle {
    val in_a = Flipped(Decoupled(new TLBundleA_lite(params)))
    val in_d = Decoupled(new TLBundleD_lite_plus(params))
    val out_a = Decoupled(new TLBundleA_lite(params))
    val out_d = Flipped(Decoupled(new TLBundleD_lite(params)))
    val directoryReservation = Output(UInt((params.cache.sets * params.cache.ways).W))
  })

  val scheduler = Module(new Scheduler(params))

  scheduler.io.in_a.bits := io.in_a.bits
  scheduler.io.in_a.valid := io.in_a.valid
  io.in_a.ready := scheduler.io.in_a.ready

  io.in_d.bits := scheduler.io.in_d.bits
  io.in_d.valid := scheduler.io.in_d.valid
  scheduler.io.in_d.ready := io.in_d.ready

  io.out_a.bits := scheduler.io.out_a.bits
  io.out_a.valid := scheduler.io.out_a.valid
  scheduler.io.out_a.ready := io.out_a.ready

  scheduler.io.out_d.bits := io.out_d.bits
  scheduler.io.out_d.valid := io.out_d.valid
  io.out_d.ready := scheduler.io.out_d.ready

  val directoryReady = expose(scheduler.directory.io.ready)
  val directoryResultValid = expose(scheduler.directory.io.result.valid)
  val directoryResultReady = expose(scheduler.directory.io.result.ready)
  val directoryResultHit = expose(scheduler.directory.io.result.bits.hit)
  val directoryResultWay = expose(scheduler.directory.io.result.bits.way)
  val directoryWriteValid = expose(scheduler.directory.io.write.valid)
  val directoryWriteReady = expose(scheduler.directory.io.write.ready)
  val directoryWriteSet = expose(scheduler.directory.io.write.bits.set)
  val directoryWriteWay = expose(scheduler.directory.io.write.bits.way)
  val directoryWriteHitHazard = expose(scheduler.directory.io.write_hit_hazard)
  val bankedStoreWriteValid = expose(scheduler.bankedStore.io.sinkD_adr.valid)
  val bankedStoreWriteSet = expose(scheduler.bankedStore.io.sinkD_adr.bits.set)
  val bankedStoreWriteWay = expose(scheduler.bankedStore.io.sinkD_adr.bits.way)
  val invalidate = expose(scheduler.directory.io.invalidate)
  val mshrRequest = expose(scheduler.mshr_request)
  val mshrOwned = expose(scheduler.mshr_ownedOH)
  val mshrWait = expose(scheduler.sourceD.io.mshr_wait)
  val maintenanceDrained = expose(scheduler.maintenanceDrained)
  private val exposedReservation = (0 until params.cache.sets).map { set =>
    expose(scheduler.directory.reservation(set))
  }
  io.directoryReservation := VecInit(exposedReservation).asUInt
}

class L2SchedulerIntegrationTest extends AnyFreeSpec with ChiselScalatestTester {
  private val params = InclusiveCacheParameters_lite(
    cache = CacheParameters(
      level = 2,
      ways = 2,
      sets = 2,
      l2cs = 1,
      blockBytes = 16,
      beatBytes = 16
    ),
    micro = InclusiveCacheMicroParameters(
      writeBytes = 1,
      memCycles = 2,
      portFactor = 2,
      num_warp = 2,
      num_sm = 1,
      num_sm_in_cluster = 1,
      num_cluster = 1,
      NMshrEntry = 2,
      NSets = 2,
      NInfWriteEntry = 2
    ),
    control = false,
    mmu = false
  )
  private val livenessParams = params.copy(
    cache = params.cache.copy(ways = 4),
    micro = params.micro.copy(memCycles = 3)
  )
  private val admissionParams = params.copy(
    cache = params.cache.copy(ways = 4, blockBytes = 64),
    micro = params.micro.copy(memCycles = 6)
  )

  private class Observer(dut: SchedulerIntegrationHarness) {
    var fillCommits = 0

    def step(cycles: Int = 1): Unit = {
      for (_ <- 0 until cycles) {
        val directoryFire = dut.directoryWriteValid.peek().litToBoolean &&
          dut.directoryWriteReady.peek().litToBoolean
        val storeValid = dut.bankedStoreWriteValid.peek().litToBoolean

        assert(directoryFire == storeValid,
          "Directory and BankedStore must commit a refill on the same cycle")
        if (directoryFire) {
          dut.bankedStoreWriteSet.expect(dut.directoryWriteSet.peek())
          dut.bankedStoreWriteWay.expect(dut.directoryWriteWay.peek())
          fillCommits += 1
        }
        dut.clock.step()
      }
    }
  }

  private def initialize(dut: SchedulerIntegrationHarness): Unit = {
    dut.io.in_a.valid.poke(false.B)
    dut.io.in_a.bits.opcode.poke(0.U)
    dut.io.in_a.bits.size.poke(0.U)
    dut.io.in_a.bits.source.poke(0.U)
    dut.io.in_a.bits.address.poke(0.U)
    dut.io.in_a.bits.mask.poke(0.U)
    dut.io.in_a.bits.data.poke(0.U)
    dut.io.in_a.bits.param.poke(0.U)

    dut.io.in_d.ready.poke(true.B)
    dut.io.out_a.ready.poke(true.B)

    dut.io.out_d.valid.poke(false.B)
    dut.io.out_d.bits.opcode.poke(0.U)
    dut.io.out_d.bits.size.poke(0.U)
    dut.io.out_d.bits.source.poke(0.U)
    dut.io.out_d.bits.data.poke(0.U)
    dut.io.out_d.bits.param.poke(0.U)
  }

  private def reset(dut: SchedulerIntegrationHarness, observer: Observer): Unit = {
    dut.reset.poke(true.B)
    observer.step(2)
    dut.reset.poke(false.B)

    var cycles = 0
    while (!dut.directoryReady.peek().litToBoolean && cycles < 16) {
      observer.step()
      cycles += 1
    }
    dut.directoryReady.expect(true.B)
  }

  private def driveA(
    dut: SchedulerIntegrationHarness,
    observer: Observer,
    opcode: UInt,
    address: BigInt,
    source: BigInt,
    param: BigInt = 0
  ): Unit = {
    dut.io.in_a.bits.opcode.poke(opcode)
    dut.io.in_a.bits.size.poke(4.U)
    dut.io.in_a.bits.source.poke(source.U)
    dut.io.in_a.bits.address.poke(address.U)
    dut.io.in_a.bits.mask.poke(BigInt("ffff", 16).U)
    dut.io.in_a.bits.data.poke(0.U)
    dut.io.in_a.bits.param.poke(param.U)
    dut.io.in_a.valid.poke(true.B)

    var cycles = 0
    while (!dut.io.in_a.ready.peek().litToBoolean && cycles < 64) {
      observer.step()
      cycles += 1
    }
    dut.io.in_a.ready.expect(true.B)
    observer.step()
    dut.io.in_a.valid.poke(false.B)
  }

  private def waitForMemoryGet(
    dut: SchedulerIntegrationHarness,
    observer: Observer,
    address: BigInt,
    captureVictim: Boolean = false
  ): (BigInt, Option[BigInt]) = {
    var cycles = 0
    var victimWay: Option[BigInt] = None
    while (!dut.io.out_a.valid.peek().litToBoolean && cycles < 64) {
      if (captureVictim && dut.directoryResultValid.peek().litToBoolean &&
          !dut.directoryResultHit.peek().litToBoolean) {
        victimWay = Some(dut.directoryResultWay.peek().litValue)
      }
      observer.step()
      cycles += 1
    }

    dut.io.out_a.valid.expect(true.B)
    dut.io.out_a.bits.opcode.expect(Get)
    dut.io.out_a.bits.address.expect(address.U)
    val memorySource = dut.io.out_a.bits.source.peek().litValue
    if (captureVictim && dut.directoryResultValid.peek().litToBoolean &&
        !dut.directoryResultHit.peek().litToBoolean) {
      victimWay = Some(dut.directoryResultWay.peek().litValue)
    }
    observer.step()
    (memorySource, victimWay)
  }

  private def sendRefill(
    dut: SchedulerIntegrationHarness,
    observer: Observer,
    memorySource: BigInt,
    data: BigInt
  ): Unit = {
    dut.io.out_d.bits.opcode.poke(AccessAckData)
    dut.io.out_d.bits.size.poke(4.U)
    dut.io.out_d.bits.source.poke(memorySource.U)
    dut.io.out_d.bits.data.poke(data.U)
    dut.io.out_d.bits.param.poke(0.U)
    dut.io.out_d.valid.poke(true.B)

    var cycles = 0
    while (!dut.io.out_d.ready.peek().litToBoolean && cycles < 64) {
      observer.step()
      cycles += 1
    }
    dut.io.out_d.ready.expect(true.B)
    observer.step()
    dut.io.out_d.valid.poke(false.B)
  }

  private def waitForFillCommit(dut: SchedulerIntegrationHarness, observer: Observer): BigInt = {
    var cycles = 0
    while (!(dut.directoryWriteValid.peek().litToBoolean &&
             dut.directoryWriteReady.peek().litToBoolean) && cycles < 64) {
      observer.step()
      cycles += 1
    }
    dut.directoryWriteValid.expect(true.B)
    dut.directoryWriteReady.expect(true.B)
    val way = dut.directoryWriteWay.peek().litValue
    observer.step()
    way
  }

  private def waitForResponse(
    dut: SchedulerIntegrationHarness,
    observer: Observer,
    source: BigInt,
    expectedData: Option[BigInt] = None,
    maxCycles: Int = 128
  ): Unit = {
    var cycles = 0
    while (!dut.io.in_d.valid.peek().litToBoolean && cycles < maxCycles) {
      observer.step()
      cycles += 1
    }
    dut.io.in_d.valid.expect(true.B)
    dut.io.in_d.bits.source.expect(source.U)
    expectedData.foreach(data => dut.io.in_d.bits.data.expect(data.U))
    observer.step()
  }

  private def fillLine(
    dut: SchedulerIntegrationHarness,
    observer: Observer,
    address: BigInt,
    source: BigInt,
    data: BigInt
  ): BigInt = {
    driveA(dut, observer, Get, address, source)
    val (memorySource, _) = waitForMemoryGet(dut, observer, address)
    sendRefill(dut, observer, memorySource, data)
    val way = waitForFillCommit(dut, observer)
    waitForResponse(dut, observer, source, Some(data))
    way
  }

  "hit, refill, and maintenance traffic remain ordered through Scheduler" in {
    test(new SchedulerIntegrationHarness(params)) { dut =>
      initialize(dut)
      val observer = new Observer(dut)
      reset(dut, observer)

      val line0 = BigInt("00000000000000000000000011111111", 16)
      val line1 = BigInt("22222222222222222222222233333333", 16)
      val line2 = BigInt("44444444444444444444444455555555", 16)
      val address0 = BigInt("00000000", 16)
      val address1 = BigInt("00000020", 16)
      val address2 = BigInt("00000040", 16)

      val way0 = fillLine(dut, observer, address0, source = 1, line0)
      val way1 = fillLine(dut, observer, address1, source = 2, line1)
      assert(way0 != way1, "same-set cold fills must occupy different ways")

      // Hold a hit response in SourceD so its directory way remains reserved.
      dut.io.in_d.ready.poke(false.B)
      driveA(dut, observer, Get, address0, source = 3)

      var hitResultCycles = 0
      while (!(dut.directoryResultValid.peek().litToBoolean &&
               dut.directoryResultHit.peek().litToBoolean) && hitResultCycles < 64) {
        dut.io.out_a.valid.expect(false.B)
        observer.step()
        hitResultCycles += 1
      }
      dut.directoryResultValid.expect(true.B)
      dut.directoryResultHit.expect(true.B)
      val heldWay = dut.directoryResultWay.peek().litValue
      observer.step()

      var hitResponseCycles = 0
      while (!dut.io.in_d.valid.peek().litToBoolean && hitResponseCycles < 128) {
        observer.step()
        hitResponseCycles += 1
      }
      dut.io.in_d.valid.expect(true.B)
      dut.io.in_d.bits.source.expect(3.U)
      dut.io.in_d.bits.data.expect(line0.U)

      // A new same-set miss must avoid the way still referenced by the hit.
      driveA(dut, observer, Get, address2, source = 4)
      val (memorySource, victimWay) = waitForMemoryGet(
        dut, observer, address2, captureVictim = true)
      assert(victimWay.nonEmpty, "the third miss must produce a directory victim")
      assert(victimWay.get != heldWay,
        "a miss must not select the way held by an outstanding hit")

      sendRefill(dut, observer, memorySource, line2)
      val commitsBeforeThirdFill = observer.fillCommits
      val thirdFillWay = waitForFillCommit(dut, observer)
      assert(thirdFillWay == victimWay.get)
      assert(observer.fillCommits == commitsBeforeThirdFill + 1)

      // Invalidate must remain blocked while the hit and refill responses are pending.
      dut.io.in_a.bits.opcode.poke(Hint)
      dut.io.in_a.bits.size.poke(4.U)
      dut.io.in_a.bits.source.poke(5.U)
      dut.io.in_a.bits.address.poke(0.U)
      dut.io.in_a.bits.mask.poke(0.U)
      dut.io.in_a.bits.data.poke(0.U)
      dut.io.in_a.bits.param.poke(1.U)
      dut.io.in_a.valid.poke(true.B)

      for (_ <- 0 until 4) {
        dut.io.in_a.ready.expect(false.B)
        dut.invalidate.expect(false.B)
        observer.step()
      }

      // Releasing D-channel backpressure drains the hit first, then the refill.
      dut.io.in_d.ready.poke(true.B)
      waitForResponse(dut, observer, source = 3, Some(line0))
      waitForResponse(dut, observer, source = 4, Some(line2))

      var hintAcceptCycles = 0
      while (!dut.io.in_a.ready.peek().litToBoolean && hintAcceptCycles < 128) {
        dut.invalidate.expect(false.B)
        observer.step()
        hintAcceptCycles += 1
      }
      dut.io.in_a.ready.expect(true.B)
      dut.invalidate.expect(true.B)
      observer.step()
      dut.io.in_a.valid.poke(false.B)

      waitForResponse(dut, observer, source = 5, maxCycles = 128)
      assert(observer.fillCommits == commitsBeforeThirdFill + 1,
        "the third refill must commit exactly once")
    }
  }

  "a blocked fill candidate does not starve another ready MSHR" in {
    test(new SchedulerIntegrationHarness(livenessParams)) { dut =>
      initialize(dut)
      val observer = new Observer(dut)
      reset(dut, observer)

      val address0 = BigInt("00000000", 16)
      val address1 = BigInt("00000020", 16)
      val address2 = BigInt("00000040", 16)
      val blockerAddress = BigInt("00000010", 16)
      val pendingAddress = BigInt("00000030", 16)
      val line0 = BigInt("10101010101010101010101010101010", 16)
      val line1 = BigInt("21212121212121212121212121212121", 16)
      val line2 = BigInt("32323232323232323232323232323232", 16)
      val blockerLine = BigInt("43434343434343434343434343434343", 16)
      val otherLine = BigInt("43434343434343434343434343434343", 16)

      fillLine(dut, observer, address0, source = 1, line0)
      fillLine(dut, observer, address1, source = 2, line1)
      fillLine(dut, observer, blockerAddress, source = 3, blockerLine)

      // Occupy SourceD from the other set, leaving the target set free of hit
      // reservations while address2 selects its refill way.
      dut.io.in_d.ready.poke(false.B)
      driveA(dut, observer, Get, blockerAddress, source = 4)
      var cycles = 0
      while (!dut.io.in_d.valid.peek().litToBoolean && cycles < 128) {
        observer.step()
        cycles += 1
      }
      dut.io.in_d.valid.expect(true.B)

      // Allocate the fill which will later be blocked by a held same-set result.
      driveA(dut, observer, Get, address2, source = 5)
      val (blockedFillSource, _) = waitForMemoryGet(dut, observer, address2)

      // Allocate another MSHR, but hold its memory A request at SourceA.
      dut.io.out_a.ready.poke(false.B)
      driveA(dut, observer, Get, pendingAddress, source = 6)
      cycles = 0
      while (dut.mshrOwned.peek().litValue.bitCount < 2 && cycles < 64) {
        observer.step()
        cycles += 1
      }
      assert(dut.mshrOwned.peek().litValue.bitCount == 2,
        "the test must own blocked-fill and pending-A MSHRs before filling the result queue")
      dut.io.out_a.valid.expect(false.B)

      // Keep three hit results queued behind the blocked SourceD response.
      for (source <- 7 until 10) {
        driveA(dut, observer, Get, blockerAddress, source)
      }

      // Accept the fourth queued hit and the refill on the same edge. SinkD's
      // registered response and the fourth Directory result are then visible
      // together on the next cycle.
      dut.io.in_a.bits.opcode.poke(Get)
      dut.io.in_a.bits.size.poke(4.U)
      dut.io.in_a.bits.source.poke(10.U)
      dut.io.in_a.bits.address.poke(blockerAddress.U)
      dut.io.in_a.bits.mask.poke(BigInt("ffff", 16).U)
      dut.io.in_a.bits.data.poke(0.U)
      dut.io.in_a.bits.param.poke(0.U)
      dut.io.in_a.valid.poke(true.B)
      dut.io.out_d.bits.opcode.poke(AccessAckData)
      dut.io.out_d.bits.size.poke(4.U)
      dut.io.out_d.bits.source.poke(blockedFillSource.U)
      dut.io.out_d.bits.data.poke(line2.U)
      dut.io.out_d.bits.param.poke(0.U)
      dut.io.out_d.valid.poke(true.B)
      dut.io.in_a.ready.expect(true.B)
      dut.io.out_d.ready.expect(true.B)
      observer.step()
      dut.io.in_a.valid.poke(false.B)
      dut.io.out_d.valid.poke(false.B)

      // While that fourth result fills the queue, pipeline a target-set hit.
      // Its result remains held while the refill's schedule.dir becomes valid.
      driveA(dut, observer, Get, address0, source = 11)
      dut.directoryResultValid.expect(true.B)
      dut.directoryResultHit.expect(true.B)
      dut.directoryResultReady.expect(false.B)
      dut.directoryWriteHitHazard.expect(true.B)
      dut.directoryWriteValid.expect(false.B)
      dut.io.out_a.ready.poke(true.B)
      assert(dut.mshrRequest.peek().litValue.bitCount >= 2,
        s"the test did not create both candidates: request=0x${dut.mshrRequest.peek().litValue.toString(16)} " +
          s"owned=0x${dut.mshrOwned.peek().litValue.toString(16)} " +
          s"outA=${dut.io.out_a.valid.peek().litToBoolean} " +
          s"wait=${dut.mshrWait.peek().litToBoolean}")

      var readyMshrSource: Option[BigInt] = None
      cycles = 0
      while (readyMshrSource.isEmpty && cycles <= livenessParams.mshrs) {
        if (dut.io.out_a.valid.peek().litToBoolean) {
          dut.io.out_a.bits.address.expect(pendingAddress.U)
          readyMshrSource = Some(dut.io.out_a.bits.source.peek().litValue)
        }
        observer.step()
        cycles += 1
      }
      assert(readyMshrSource.nonEmpty,
        "a blocked fill candidate starved another ready MSHR")

      sendRefill(dut, observer, readyMshrSource.get, otherLine)
      dut.io.in_d.ready.poke(true.B)

      cycles = 0
      while (!dut.maintenanceDrained.peek().litToBoolean && cycles < 1024) {
        observer.step()
        cycles += 1
      }
      dut.maintenanceDrained.expect(true.B)
      dut.mshrOwned.expect(0.U)
      assert(cycles < 1024, "Scheduler did not drain after all backpressure was released")
      assert(observer.fillCommits == 5,
        "both additional refills must commit exactly once")
    }
  }

  "a pending primary miss reserves the final free MSHR at admission" in {
    test(new SchedulerIntegrationHarness(admissionParams)) { dut =>
      initialize(dut)
      val observer = new Observer(dut)
      reset(dut, observer)

      // Keep allocated misses resident in their MSHRs. With two MSHRs, the
      // first request leaves exactly one free entry.
      dut.io.out_a.ready.poke(false.B)
      driveA(dut, observer, Get, address = 0x00, source = 1)
      var cycles = 0
      while (dut.mshrOwned.peek().litValue.bitCount != 1 && cycles < 32) {
        observer.step()
        cycles += 1
      }
      assert(dut.mshrOwned.peek().litValue.bitCount == 1,
        "the setup request must own exactly one MSHR")

      // This miss enters the Directory while one MSHR is still free.
      dut.io.in_a.bits.opcode.poke(Get)
      dut.io.in_a.bits.size.poke(4.U)
      dut.io.in_a.bits.source.poke(2.U)
      dut.io.in_a.bits.address.poke(0x80.U)
      dut.io.in_a.bits.mask.poke(BigInt("ffff", 16).U)
      dut.io.in_a.bits.data.poke(0.U)
      dut.io.in_a.bits.param.poke(0.U)
      dut.io.in_a.valid.poke(true.B)
      dut.io.in_a.ready.expect(true.B)
      observer.step()

      // Its pending result consumes the final free MSHR on this cycle. Do not
      // admit another lookup using the same apparent free entry: if that
      // lookup misses, it could reserve a victim without an MSHR owner.
      dut.io.in_a.bits.source.poke(3.U)
      dut.io.in_a.bits.address.poke(0x100.U)
      dut.io.in_a.ready.expect(false.B)
      observer.step()
      dut.io.in_a.valid.poke(false.B)
      dut.mshrOwned.expect(3.U)

      // Let the two owned misses reach memory, then complete one of them.
      // The held result must subsequently allocate the released MSHR and
      // issue its own memory request.
      dut.io.out_a.ready.poke(true.B)
      val (memorySource0, _) = waitForMemoryGet(dut, observer, 0x00)
      val (memorySource1, _) = waitForMemoryGet(dut, observer, 0x80)
      val line0 = BigInt("10101010101010101010101010101010", 16)
      val line1 = BigInt("21212121212121212121212121212121", 16)
      val line2 = BigInt("32323232323232323232323232323232", 16)

      sendRefill(dut, observer, memorySource0, line0)
      waitForFillCommit(dut, observer)
      waitForResponse(dut, observer, source = 1, Some(line0))

      driveA(dut, observer, Get, address = 0x100, source = 3)
      val (memorySource2, _) = waitForMemoryGet(dut, observer, 0x100)
      sendRefill(dut, observer, memorySource1, line1)
      waitForFillCommit(dut, observer)
      waitForResponse(dut, observer, source = 2, Some(line1))
      sendRefill(dut, observer, memorySource2, line2)
      waitForFillCommit(dut, observer)
      waitForResponse(dut, observer, source = 3, Some(line2))

      cycles = 0
      while (!dut.maintenanceDrained.peek().litToBoolean && cycles < 128) {
        observer.step()
        cycles += 1
      }
      dut.maintenanceDrained.expect(true.B)
      dut.mshrOwned.expect(0.U)
      dut.io.directoryReservation.expect(0.U)
    }
  }
}
