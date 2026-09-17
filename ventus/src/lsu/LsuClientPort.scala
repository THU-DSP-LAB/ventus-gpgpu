/*
 * Cacheline-level client contract shared by the pipeline LSU and RTcore.
 *
 * DCache's narrow instrId remains an LSU-private implementation tag.  Clients
 * identify their endpoint with clientId and describe data in a canonical
 * 128-byte cacheline with coverageMask selecting the valid words.
 */
package lsu

import chisel3._
import top.parameters._

object LsuClientId {
  val width = 2
  val Pipe = 0.U(width.W)
  val RtCore = 1.U(width.W)
}

/** Opaque client transaction identity, independent of the physical DCache tag. */
object LsuClientTag { val width = 5 }

class LsuClientRequest extends Bundle {
  val clientId = UInt(LsuClientId.width.W)
  val clientTag = UInt(LsuClientTag.width.W)
  val lineBase = UInt(memReqLen.W)
  val coverageMask = UInt(dcache_BlockWords.W)
  val write = Bool()
  val writeData = Vec(dcache_BlockWords, UInt(xLen.W))
}

class LsuClientResponse extends Bundle {
  val clientId = UInt(LsuClientId.width.W)
  val clientTag = UInt(LsuClientTag.width.W)
  val lineBase = UInt(memReqLen.W)
  val coverageMask = UInt(dcache_BlockWords.W)
  val data = Vec(dcache_BlockWords, UInt(xLen.W))
  val fault = Bool()
}
