package L1Cache

import L1Cache.DCache.DCacheBundle
import chisel3._
import chisel3.util.log2Up
import config.config.Parameters
import mmu.SV32.{asidLen, paLen}
import top.parameters._
import mmu._
/*Version Note
* DCacheCoreReq spec changed, shift some work to LSU
* //byteEn
//00 for byte
//01 for half word, alignment required
//11 for word, alignment required
*
* TL memReq port adapted
*
* this design havent take NBanks = BlockWords simplification in to account
* this design consider only NBanks = BlockWords case at miss rsp to data bank transition
* */

/*class DCacheMshrBlockAddr(implicit p: Parameters)extends DCacheBundle{
  val instrId = UInt(WIdBits.W)
  val blockAddr = UInt(bABits.W)
}*/
class DCachePerLaneAddr(implicit p: Parameters) extends DCacheBundle{
  val activeMask = Bool()
  val blockOffset = UInt(BlockOffsetBits.W)
  val wordOffset1H = UInt(BytesOfWord.W)
}
class DCacheCoreReq(implicit p: Parameters) extends DCacheBundle{
  //val ctrlAddr = new Bundle{
  val instrId = UInt(WIdBits.W)//TODO length unsure
  val opcode = UInt(3.W)//0-read 1-write 3- flush/invalidate
  val param = UInt(4.W)
  val tag = UInt(TagBits.W)
  val asid = UInt(asidLen.W)
  val setIdx = UInt(SetIdxBits.W)
  val perLaneAddr = Vec(NLanes, new DCachePerLaneAddr)
  val data = Vec(NLanes, UInt(WordLength.W))
}

class DCacheCoreRsp(implicit p: Parameters) extends DCacheBundle{
  val instrId = UInt(WIdBits.W)
  val isWrite = Bool()
  val data = Vec(NLanes, UInt(WordLength.W))
  val activeMask = Vec(NLanes, Bool())//UInt(NLanes.W)
}
class DCacheCoreRsp_d(implicit p: Parameters) extends DCacheBundle{
  val instrId = UInt(WIdBits.W)
  val isWrite = Bool()
  val data = Vec(NLanesd, UInt(WordLength.W))
  val activeMask = Vec(NLanes, Bool())//UInt(NLanes.W)
}

class DCacheMemRsp(implicit p: Parameters) extends DCacheBundle{
  val d_opcode = UInt(3.W)// AccessAckData only
  //val d_param
  //val d_size
  val d_source = UInt((3+log2Up(NMshrEntry)+log2Up(NSets)).W)//cut off head log2Up(NSms) bits at outside
  val d_addr = UInt(WordLength.W)
  val d_data = Vec(BlockWords, UInt(WordLength.W))//UInt((WordLength * BlockWords).W)
}

class L1CacheMemReq extends Bundle{
  val a_opcode = UInt(3.W)
  val a_param = UInt(3.W)
  //val a_size
  val a_source = UInt(xLen.W)
  val a_addr = UInt(xLen.W)
  //val isWrite = Bool()//Merged into a_opcode
  val a_data = Vec(dcache_BlockWords, UInt(xLen.W))
  //there is BW waste, only at most NLanes of a_data elements would be filled, BlockWords is usually larger than NLanes
  val a_mask = Vec(dcache_BlockWords,UInt(BytesOfWord.W))
}

class DCacheMemReq extends L1CacheMemReq{
  override val a_source = UInt((3+log2Up(dcache_MshrEntry)+log2Up(dcache_NSets)).W)
}

class DCacheMemReq_p extends DCacheMemReq{
  override val a_addr = UInt(paLen.W)
}

class L1CacheMemReqArb (implicit p: Parameters) extends DCacheBundle{
  val a_opcode = UInt(3.W)
  val a_param = UInt(3.W)
  //val a_size
  val a_source = UInt((log2Up(NCacheInSM)+3+log2Up(dcache_MshrEntry)+log2Up(dcache_NSets)).W)
  val a_addr = UInt(xLen.W)
  //val isWrite = Bool()//Merged into a_opcode
  val a_data = Vec(dcache_BlockWords, UInt(xLen.W))
  //there is BW waste, only at most NLanes of a_data elements would be filled, BlockWords is usually larger than NLanes
  val a_mask = Vec(dcache_BlockWords, UInt(BytesOfWord.W))
}

class L1CacheMemRsp(implicit p: Parameters) extends DCacheMemRsp{
  override val d_source = UInt((log2Up(NCacheInSM)+3+log2Up(dcache_MshrEntry)+log2Up(dcache_NSets)).W)
}