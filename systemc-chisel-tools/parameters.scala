/*
 * Copyright (c) 2021-2022 International Innovation Center of Tsinghua University, Shanghai
 * Ventus is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 *          http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND,
 * EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT,
 * MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details. */
package pipeline
// import L2cache.{CacheParameters, InclusiveCacheMicroParameters, InclusiveCacheParameters_lite}
import chisel3._
import chisel3.util._
object parameters{//notice log2Ceil(4) returns 2.that is ,n is the total num, not the last idx.
  def num_sm=2

  val SINGLE_INST:Boolean=false

  def num_warp=4
  def depth_warp=log2Ceil(num_warp)
  def num_thread=8
  def depth_thread=log2Ceil(num_thread)
  def xLen = 32 // data length 32-bit
  def num_block=num_warp // not bigger than num_warp
  def num_warp_in_a_block=num_warp

  def num_icachebuf = 1 //blocking for each warp
  def depth_icachebuf = log2Ceil(num_icachebuf)
  def num_ibuffer=2
  def depth_ibuffer=log2Ceil(num_ibuffer)
  def lsu_num_entry_each_warp=4//blocking for each warp
  def lsu_nMshrEntry = num_warp // less than num_warp
  def dcache_NSets: Int = 32
  def dcache_NWays: Int = 2
  def dcache_BlockWords: Int = num_thread
  def dcache_SetIdxBits: Int = log2Ceil(dcache_NSets)
  def BytesOfWord = 32/8
  def dcache_WordOffsetBits = log2Ceil(BytesOfWord)//a Word has 4 Bytes
  def dcache_BlockOffsetBits = log2Ceil(dcache_BlockWords)// select word in block
  def dcache_TagBits = xLen - (dcache_SetIdxBits + dcache_BlockOffsetBits + dcache_WordOffsetBits)

  def num_sfu = (num_thread >> 2).max(1)

  def sharedmem_depth = 128
  def sharedmem_BlockWords = dcache_BlockWords
  def sharemem_size = sharedmem_depth * sharedmem_BlockWords * 4 //bytes

  // def l2cache_NSets: Int = 2
  // def l2cache_NWays: Int = 4
  // def l2cache_BlockWords: Int = dcache_BlockWords
  // def l2cache_writeBytes: Int = 4
  // def l2cache_memCycles: Int = 4
  // def l2cache_portFactor: Int = 2
  // val l2cache_cache=CacheParameters(2,l2cache_NWays,l2cache_NSets,l2cache_BlockWords<<2,l2cache_BlockWords<<2)
  // val l2cache_micro=InclusiveCacheMicroParameters(l2cache_writeBytes,l2cache_memCycles,l2cache_portFactor,num_warp,num_sm)
  // val l2cache_params=InclusiveCacheParameters_lite(l2cache_cache,l2cache_micro,false)

  def sig_length = 33

  def num_cache_in_sm = 2

  var NUMBER_CU = num_sm
  var NUMBER_RES_TABLE = 1 // <NUMBER_CU
  var NUMBER_VGPR_SLOTS = 4096
  var NUMBER_SGPR_SLOTS = 4096
  var NUMBER_LDS_SLOTS = 4096
  var NUMBER_WF_SLOTS = num_block // max num of wg in a CU
  var WG_ID_WIDTH = 2+log2Ceil(NUMBER_WF_SLOTS)+log2Ceil(NUMBER_CU) //Format: wg id + prefer scheduler (if multi-schedulers) + prefer cu
  var WG_NUM_MAX = NUMBER_WF_SLOTS * NUMBER_CU
  var WF_COUNT_MAX = num_warp // max num of wf in a cu
  var WF_COUNT_PER_WG_MAX = num_warp_in_a_block // max num of wf in a wg
  var GDS_SIZE = 1024 //unused.
  var NUMBER_ENTRIES = 2 //This parameter should be a power of 2
  var WAVE_ITEM_WIDTH = 10
  var MEM_ADDR_WIDTH = 32
  var NUM_SCHEDULER = 1 // only used for multi-cta-scheduler
  var RES_TABLE_ADDR_WIDTH = log2Ceil(NUMBER_RES_TABLE).max(1)
  var CU_ID_WIDTH = log2Ceil(NUMBER_CU).max(RES_TABLE_ADDR_WIDTH+1)
  var VGPR_ID_WIDTH = log2Ceil(NUMBER_VGPR_SLOTS)
  var SGPR_ID_WIDTH = log2Ceil(NUMBER_SGPR_SLOTS)
  var LDS_ID_WIDTH = log2Ceil(NUMBER_LDS_SLOTS)
  var WG_SLOT_ID_WIDTH = log2Ceil(NUMBER_WF_SLOTS)
  var WF_COUNT_WIDTH = log2Ceil(WF_COUNT_MAX) + 1
  var WF_COUNT_WIDTH_PER_WG = log2Ceil(WF_COUNT_PER_WG_MAX) + 1
  var GDS_ID_WIDTH = log2Ceil(GDS_SIZE)
  var ENTRY_ADDR_WIDTH = log2Ceil(NUMBER_ENTRIES)
  var TAG_WIDTH = WG_SLOT_ID_WIDTH + WF_COUNT_WIDTH_PER_WG
  var INIT_MAX_WG_COUNT = NUMBER_WF_SLOTS
  var NUM_SCHEDULER_WIDTH = log2Ceil(NUM_SCHEDULER)
}
