package top

import chisel3._
import chisel3.util._
import mmu.SVParam
import top.parameters._

class cache_spike_info(SV: SVParam) extends Bundle{
  val pc = UInt(xLen.W)
  val vaddr = UInt(SV.vaLen.W)
}
