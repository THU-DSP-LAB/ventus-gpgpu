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
package CTA

import chisel3.util._

class parameter{
    var NUMBER_CU = 4
    var NUMBER_RES_TABLE = 2
    var NUMBER_VGPR_SLOTS = 4096
    var NUMBER_SGPR_SLOTS = 4096
    var NUMBER_LDS_SLOTS = 4096
    var WG_ID_WIDTH = 15 //Format: wg id + prefer scheduler (if multi-schedulers) + prefer cu
    var NUMBER_WF_SLOTS = 8
    var WF_COUNT_MAX = 256
    var WF_COUNT_PER_WG_MAX = 32
    var GDS_SIZE = 1024
    var NUMBER_ENTRIES = 2 //This parameter should be a power of 2
    var WAVE_ITEM_WIDTH = 10
    var MEM_ADDR_WIDTH = 32
    var NUM_SCHEDULER = 2
    var CU_ID_WIDTH = Math.max(log2Ceil(NUMBER_CU), 1)
    var RES_TABLE_ADDR_WIDTH = Math.max(log2Ceil(NUMBER_RES_TABLE), 1)
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
    var NUM_SCHEDULER_WIDTH = Math.max(log2Ceil(NUM_SCHEDULER), 1)
}

