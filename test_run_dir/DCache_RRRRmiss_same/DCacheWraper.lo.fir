circuit DCacheWraper :
  module getEntryStatusReq :
    input clock : Clock
    input reset : UInt<1>
    input io_valid_list : UInt<2>
    output io_alm_full : UInt<1>
    output io_full : UInt<1>
    output io_next : UInt<1>

    node _used_T = bits(io_valid_list, 0, 0) @[Bitwise.scala 50:65]
    node _used_T_1 = bits(io_valid_list, 1, 1) @[Bitwise.scala 50:65]
    node _used_T_2 = add(_used_T, _used_T_1) @[Bitwise.scala 48:55]
    node used = bits(_used_T_2, 1, 0) @[Bitwise.scala 48:55]
    node _io_alm_full_T = sub(UInt<2>("h2"), UInt<1>("h1")) @[L1MSHR.scala 53:36]
    node _io_alm_full_T_1 = tail(_io_alm_full_T, 1) @[L1MSHR.scala 53:36]
    node _io_alm_full_T_2 = eq(used, _io_alm_full_T_1) @[L1MSHR.scala 53:23]
    node _io_full_T = andr(io_valid_list) @[L1MSHR.scala 54:28]
    node _io_next_T = bits(io_valid_list, 0, 0) @[L1MSHR.scala 55:36]
    node _io_next_T_1 = bits(io_valid_list, 1, 1) @[L1MSHR.scala 55:36]
    node _io_next_WIRE_0 = _io_next_T @[L1MSHR.scala 55:{21,21}]
    node _io_next_T_2 = eq(_io_next_WIRE_0, UInt<1>("h0")) @[L1MSHR.scala 55:58]
    node _io_next_WIRE_1 = _io_next_T_1 @[L1MSHR.scala 55:{21,21}]
    node _io_next_T_3 = eq(_io_next_WIRE_1, UInt<1>("h0")) @[L1MSHR.scala 55:58]
    node _io_next_T_4 = mux(_io_next_T_2, UInt<1>("h0"), UInt<1>("h1")) @[L1MSHR.scala 55:55]
    io_alm_full <= _io_alm_full_T_2 @[L1MSHR.scala 53:15]
    io_full <= _io_full_T @[L1MSHR.scala 54:11]
    io_next <= _io_next_T_4 @[L1MSHR.scala 55:11]

  module getEntryStatusRsp :
    input clock : Clock
    input reset : UInt<1>
    input io_valid_list : UInt<2>
    output io_next2cancel : UInt<1>
    output io_used : UInt<2>

    node _io_next2cancel_T = bits(io_valid_list, 0, 0) @[L1MSHR.scala 64:43]
    node _io_next2cancel_T_1 = bits(io_valid_list, 1, 1) @[L1MSHR.scala 64:43]
    node _io_next2cancel_WIRE_0 = _io_next2cancel_T @[L1MSHR.scala 64:{28,28}]
    node _io_next2cancel_T_2 = eq(_io_next2cancel_WIRE_0, UInt<1>("h1")) @[L1MSHR.scala 64:65]
    node _io_next2cancel_WIRE_1 = _io_next2cancel_T_1 @[L1MSHR.scala 64:{28,28}]
    node _io_next2cancel_T_3 = eq(_io_next2cancel_WIRE_1, UInt<1>("h1")) @[L1MSHR.scala 64:65]
    node _io_next2cancel_T_4 = mux(_io_next2cancel_T_2, UInt<1>("h0"), UInt<1>("h1")) @[L1MSHR.scala 64:62]
    node _io_used_T = bits(io_valid_list, 0, 0) @[Bitwise.scala 50:65]
    node _io_used_T_1 = bits(io_valid_list, 1, 1) @[Bitwise.scala 50:65]
    node _io_used_T_2 = add(_io_used_T, _io_used_T_1) @[Bitwise.scala 48:55]
    node _io_used_T_3 = bits(_io_used_T_2, 1, 0) @[Bitwise.scala 48:55]
    io_next2cancel <= _io_next2cancel_T_4 @[L1MSHR.scala 64:18]
    io_used <= _io_used_T_3 @[L1MSHR.scala 65:11]

  module getEntryStatusReq_1 :
    input clock : Clock
    input reset : UInt<1>
    input io_valid_list : UInt<4>
    output io_alm_full : UInt<1>
    output io_full : UInt<1>
    output io_next : UInt<2>

    node _used_T = bits(io_valid_list, 0, 0) @[Bitwise.scala 50:65]
    node _used_T_1 = bits(io_valid_list, 1, 1) @[Bitwise.scala 50:65]
    node _used_T_2 = bits(io_valid_list, 2, 2) @[Bitwise.scala 50:65]
    node _used_T_3 = bits(io_valid_list, 3, 3) @[Bitwise.scala 50:65]
    node _used_T_4 = add(_used_T, _used_T_1) @[Bitwise.scala 48:55]
    node _used_T_5 = bits(_used_T_4, 1, 0) @[Bitwise.scala 48:55]
    node _used_T_6 = add(_used_T_2, _used_T_3) @[Bitwise.scala 48:55]
    node _used_T_7 = bits(_used_T_6, 1, 0) @[Bitwise.scala 48:55]
    node _used_T_8 = add(_used_T_5, _used_T_7) @[Bitwise.scala 48:55]
    node used = bits(_used_T_8, 2, 0) @[Bitwise.scala 48:55]
    node _io_alm_full_T = sub(UInt<3>("h4"), UInt<1>("h1")) @[L1MSHR.scala 53:36]
    node _io_alm_full_T_1 = tail(_io_alm_full_T, 1) @[L1MSHR.scala 53:36]
    node _io_alm_full_T_2 = eq(used, _io_alm_full_T_1) @[L1MSHR.scala 53:23]
    node _io_full_T = andr(io_valid_list) @[L1MSHR.scala 54:28]
    node _io_next_T = bits(io_valid_list, 0, 0) @[L1MSHR.scala 55:36]
    node _io_next_T_1 = bits(io_valid_list, 1, 1) @[L1MSHR.scala 55:36]
    node _io_next_T_2 = bits(io_valid_list, 2, 2) @[L1MSHR.scala 55:36]
    node _io_next_T_3 = bits(io_valid_list, 3, 3) @[L1MSHR.scala 55:36]
    node _io_next_WIRE_0 = _io_next_T @[L1MSHR.scala 55:{21,21}]
    node _io_next_T_4 = eq(_io_next_WIRE_0, UInt<1>("h0")) @[L1MSHR.scala 55:58]
    node _io_next_WIRE_1 = _io_next_T_1 @[L1MSHR.scala 55:{21,21}]
    node _io_next_T_5 = eq(_io_next_WIRE_1, UInt<1>("h0")) @[L1MSHR.scala 55:58]
    node _io_next_WIRE_2 = _io_next_T_2 @[L1MSHR.scala 55:{21,21}]
    node _io_next_T_6 = eq(_io_next_WIRE_2, UInt<1>("h0")) @[L1MSHR.scala 55:58]
    node _io_next_WIRE_3 = _io_next_T_3 @[L1MSHR.scala 55:{21,21}]
    node _io_next_T_7 = eq(_io_next_WIRE_3, UInt<1>("h0")) @[L1MSHR.scala 55:58]
    node _io_next_T_8 = mux(_io_next_T_6, UInt<2>("h2"), UInt<2>("h3")) @[L1MSHR.scala 55:55]
    node _io_next_T_9 = mux(_io_next_T_5, UInt<1>("h1"), _io_next_T_8) @[L1MSHR.scala 55:55]
    node _io_next_T_10 = mux(_io_next_T_4, UInt<1>("h0"), _io_next_T_9) @[L1MSHR.scala 55:55]
    io_alm_full <= _io_alm_full_T_2 @[L1MSHR.scala 53:15]
    io_full <= _io_full_T @[L1MSHR.scala 54:11]
    io_next <= _io_next_T_10 @[L1MSHR.scala 55:11]

  module MSHR :
    input clock : Clock
    input reset : UInt<1>
    input io_probe_valid : UInt<1>
    input io_probe_bits_blockAddr : UInt<28>
    output io_probeOut_st1_probeStatus : UInt<3>
    output io_probeOut_st1_a_source : UInt<2>
    output io_missReq_ready : UInt<1>
    input io_missReq_valid : UInt<1>
    input io_missReq_bits_blockAddr : UInt<28>
    input io_missReq_bits_instrId : UInt<3>
    input io_missReq_bits_targetInfo : UInt<31>
    output io_missRspIn_ready : UInt<1>
    input io_missRspIn_valid : UInt<1>
    input io_missRspIn_bits_instrId : UInt<2>
    output io_missRspOut_valid : UInt<1>
    output io_missRspOut_bits_targetInfo : UInt<31>
    output io_missRspOut_bits_blockAddr : UInt<28>
    output io_missRspOut_bits_instrId : UInt<3>
    output io_empty : UInt<1>

    inst subentryStatus of getEntryStatusReq @[L1MSHR.scala 113:30]
    inst subentryStatusForRsp of getEntryStatusRsp @[L1MSHR.scala 117:36]
    inst entryStatus of getEntryStatusReq_1 @[L1MSHR.scala 120:27]
    reg blockAddr_Access_0 : UInt<28>, clock with :
      reset => (UInt<1>("h0"), blockAddr_Access_0) @[L1MSHR.scala 81:33]
    reg blockAddr_Access_1 : UInt<28>, clock with :
      reset => (UInt<1>("h0"), blockAddr_Access_1) @[L1MSHR.scala 81:33]
    reg blockAddr_Access_2 : UInt<28>, clock with :
      reset => (UInt<1>("h0"), blockAddr_Access_2) @[L1MSHR.scala 81:33]
    reg blockAddr_Access_3 : UInt<28>, clock with :
      reset => (UInt<1>("h0"), blockAddr_Access_3) @[L1MSHR.scala 81:33]
    reg instrId_Access_0 : UInt<3>, clock with :
      reset => (UInt<1>("h0"), instrId_Access_0) @[L1MSHR.scala 82:31]
    reg instrId_Access_1 : UInt<3>, clock with :
      reset => (UInt<1>("h0"), instrId_Access_1) @[L1MSHR.scala 82:31]
    reg instrId_Access_2 : UInt<3>, clock with :
      reset => (UInt<1>("h0"), instrId_Access_2) @[L1MSHR.scala 82:31]
    reg instrId_Access_3 : UInt<3>, clock with :
      reset => (UInt<1>("h0"), instrId_Access_3) @[L1MSHR.scala 82:31]
    reg targetInfo_Accesss_0_0 : UInt<31>, clock with :
      reset => (UInt<1>("h0"), targetInfo_Accesss_0_0) @[L1MSHR.scala 83:35]
    reg targetInfo_Accesss_0_1 : UInt<31>, clock with :
      reset => (UInt<1>("h0"), targetInfo_Accesss_0_1) @[L1MSHR.scala 83:35]
    reg targetInfo_Accesss_1_0 : UInt<31>, clock with :
      reset => (UInt<1>("h0"), targetInfo_Accesss_1_0) @[L1MSHR.scala 83:35]
    reg targetInfo_Accesss_1_1 : UInt<31>, clock with :
      reset => (UInt<1>("h0"), targetInfo_Accesss_1_1) @[L1MSHR.scala 83:35]
    reg targetInfo_Accesss_2_0 : UInt<31>, clock with :
      reset => (UInt<1>("h0"), targetInfo_Accesss_2_0) @[L1MSHR.scala 83:35]
    reg targetInfo_Accesss_2_1 : UInt<31>, clock with :
      reset => (UInt<1>("h0"), targetInfo_Accesss_2_1) @[L1MSHR.scala 83:35]
    reg targetInfo_Accesss_3_0 : UInt<31>, clock with :
      reset => (UInt<1>("h0"), targetInfo_Accesss_3_0) @[L1MSHR.scala 83:35]
    reg targetInfo_Accesss_3_1 : UInt<31>, clock with :
      reset => (UInt<1>("h0"), targetInfo_Accesss_3_1) @[L1MSHR.scala 83:35]
    reg subentry_valid_0_0 : UInt<1>, clock with :
      reset => (UInt<1>("h0"), subentry_valid_0_0) @[L1MSHR.scala 85:31]
    reg subentry_valid_0_1 : UInt<1>, clock with :
      reset => (UInt<1>("h0"), subentry_valid_0_1) @[L1MSHR.scala 85:31]
    reg subentry_valid_1_0 : UInt<1>, clock with :
      reset => (UInt<1>("h0"), subentry_valid_1_0) @[L1MSHR.scala 85:31]
    reg subentry_valid_1_1 : UInt<1>, clock with :
      reset => (UInt<1>("h0"), subentry_valid_1_1) @[L1MSHR.scala 85:31]
    reg subentry_valid_2_0 : UInt<1>, clock with :
      reset => (UInt<1>("h0"), subentry_valid_2_0) @[L1MSHR.scala 85:31]
    reg subentry_valid_2_1 : UInt<1>, clock with :
      reset => (UInt<1>("h0"), subentry_valid_2_1) @[L1MSHR.scala 85:31]
    reg subentry_valid_3_0 : UInt<1>, clock with :
      reset => (UInt<1>("h0"), subentry_valid_3_0) @[L1MSHR.scala 85:31]
    reg subentry_valid_3_1 : UInt<1>, clock with :
      reset => (UInt<1>("h0"), subentry_valid_3_1) @[L1MSHR.scala 85:31]
    node _entry_valid_T = cat(subentry_valid_0_0, subentry_valid_0_1) @[Cat.scala 31:58]
    node _entry_valid_T_1 = orr(_entry_valid_T) @[L1MSHR.scala 86:59]
    node _entry_valid_T_2 = cat(subentry_valid_1_0, subentry_valid_1_1) @[Cat.scala 31:58]
    node _entry_valid_T_3 = orr(_entry_valid_T_2) @[L1MSHR.scala 86:59]
    node _entry_valid_T_4 = cat(subentry_valid_2_0, subentry_valid_2_1) @[Cat.scala 31:58]
    node _entry_valid_T_5 = orr(_entry_valid_T_4) @[L1MSHR.scala 86:59]
    node _entry_valid_T_6 = cat(subentry_valid_3_0, subentry_valid_3_1) @[Cat.scala 31:58]
    node _entry_valid_T_7 = orr(_entry_valid_T_6) @[L1MSHR.scala 86:59]
    node entry_valid_lo = cat(_entry_valid_T_5, _entry_valid_T_7) @[Cat.scala 31:58]
    node entry_valid_hi = cat(_entry_valid_T_1, _entry_valid_T_3) @[Cat.scala 31:58]
    node _entry_valid_T_8 = cat(entry_valid_hi, entry_valid_lo) @[Cat.scala 31:58]
    node _entry_valid_T_9 = bits(_entry_valid_T_8, 1, 0) @[Bitwise.scala 111:18]
    node _entry_valid_T_10 = bits(_entry_valid_T_9, 0, 0) @[Bitwise.scala 111:18]
    node _entry_valid_T_11 = bits(_entry_valid_T_9, 1, 1) @[Bitwise.scala 111:47]
    node _entry_valid_T_12 = cat(_entry_valid_T_10, _entry_valid_T_11) @[Cat.scala 31:58]
    node _entry_valid_T_13 = bits(_entry_valid_T_8, 3, 2) @[Bitwise.scala 111:47]
    node _entry_valid_T_14 = bits(_entry_valid_T_13, 0, 0) @[Bitwise.scala 111:18]
    node _entry_valid_T_15 = bits(_entry_valid_T_13, 1, 1) @[Bitwise.scala 111:47]
    node _entry_valid_T_16 = cat(_entry_valid_T_14, _entry_valid_T_15) @[Cat.scala 31:58]
    node entry_valid = cat(_entry_valid_T_12, _entry_valid_T_16) @[Cat.scala 31:58]
    node _io_empty_T = orr(entry_valid) @[L1MSHR.scala 87:28]
    node _io_empty_T_1 = eq(_io_empty_T, UInt<1>("h0")) @[L1MSHR.scala 87:15]
    node _entryMatchProbe_T = eq(blockAddr_Access_0, io_probe_bits_blockAddr) @[L1MSHR.scala 136:57]
    node _entryMatchProbe_T_1 = eq(blockAddr_Access_1, io_probe_bits_blockAddr) @[L1MSHR.scala 136:57]
    node entryMatchProbe_hi = cat(_entryMatchProbe_T, _entryMatchProbe_T_1) @[Cat.scala 31:58]
    node _entryMatchProbe_T_2 = eq(blockAddr_Access_2, io_probe_bits_blockAddr) @[L1MSHR.scala 136:57]
    node _entryMatchProbe_T_3 = eq(blockAddr_Access_3, io_probe_bits_blockAddr) @[L1MSHR.scala 136:57]
    node entryMatchProbe_lo = cat(_entryMatchProbe_T_2, _entryMatchProbe_T_3) @[Cat.scala 31:58]
    node _entryMatchProbe_T_4 = cat(entryMatchProbe_hi, entryMatchProbe_lo) @[Cat.scala 31:58]
    node _entryMatchProbe_T_5 = bits(_entryMatchProbe_T_4, 1, 0) @[Bitwise.scala 111:18]
    node _entryMatchProbe_T_6 = bits(_entryMatchProbe_T_5, 0, 0) @[Bitwise.scala 111:18]
    node _entryMatchProbe_T_7 = bits(_entryMatchProbe_T_5, 1, 1) @[Bitwise.scala 111:47]
    node _entryMatchProbe_T_8 = cat(_entryMatchProbe_T_6, _entryMatchProbe_T_7) @[Cat.scala 31:58]
    node _entryMatchProbe_T_9 = bits(_entryMatchProbe_T_4, 3, 2) @[Bitwise.scala 111:47]
    node _entryMatchProbe_T_10 = bits(_entryMatchProbe_T_9, 0, 0) @[Bitwise.scala 111:18]
    node _entryMatchProbe_T_11 = bits(_entryMatchProbe_T_9, 1, 1) @[Bitwise.scala 111:47]
    node _entryMatchProbe_T_12 = cat(_entryMatchProbe_T_10, _entryMatchProbe_T_11) @[Cat.scala 31:58]
    node _entryMatchProbe_T_13 = cat(_entryMatchProbe_T_8, _entryMatchProbe_T_12) @[Cat.scala 31:58]
    node _entryMatchProbe_T_14 = and(_entryMatchProbe_T_13, entry_valid) @[L1MSHR.scala 136:88]
    node entryMatchProbe = _entryMatchProbe_T_14 @[L1MSHR.scala 111:29 136:19]
    node subentrySelectedForReq_hi = bits(entryMatchProbe, 3, 2) @[OneHot.scala 30:18]
    node subentrySelectedForReq_lo = bits(entryMatchProbe, 1, 0) @[OneHot.scala 31:18]
    node _subentrySelectedForReq_T = orr(subentrySelectedForReq_hi) @[OneHot.scala 32:14]
    node _subentrySelectedForReq_T_1 = or(subentrySelectedForReq_hi, subentrySelectedForReq_lo) @[OneHot.scala 32:28]
    node _subentrySelectedForReq_T_2 = bits(_subentrySelectedForReq_T_1, 1, 1) @[CircuitMath.scala 30:8]
    node _subentrySelectedForReq_T_3 = cat(_subentrySelectedForReq_T, _subentrySelectedForReq_T_2) @[Cat.scala 31:58]
    node _GEN_0 = validif(eq(UInt<1>("h0"), _subentrySelectedForReq_T_3), subentry_valid_0_0) @[Cat.scala 31:{58,58}]
    node _GEN_1 = mux(eq(UInt<1>("h1"), _subentrySelectedForReq_T_3), subentry_valid_1_0, _GEN_0) @[Cat.scala 31:{58,58}]
    node _GEN_2 = mux(eq(UInt<2>("h2"), _subentrySelectedForReq_T_3), subentry_valid_2_0, _GEN_1) @[Cat.scala 31:{58,58}]
    node _GEN_3 = mux(eq(UInt<2>("h3"), _subentrySelectedForReq_T_3), subentry_valid_3_0, _GEN_2) @[Cat.scala 31:{58,58}]
    node _GEN_4 = validif(eq(UInt<1>("h0"), _subentrySelectedForReq_T_3), subentry_valid_0_1) @[Cat.scala 31:{58,58}]
    node _GEN_5 = mux(eq(UInt<1>("h1"), _subentrySelectedForReq_T_3), subentry_valid_1_1, _GEN_4) @[Cat.scala 31:{58,58}]
    node _GEN_6 = mux(eq(UInt<2>("h2"), _subentrySelectedForReq_T_3), subentry_valid_2_1, _GEN_5) @[Cat.scala 31:{58,58}]
    node _GEN_7 = mux(eq(UInt<2>("h3"), _subentrySelectedForReq_T_3), subentry_valid_3_1, _GEN_6) @[Cat.scala 31:{58,58}]
    node _subentry_valid_subentrySelectedForReq_T_3_0 = _GEN_3 @[Cat.scala 31:58]
    node _subentry_valid_subentrySelectedForReq_T_3_1 = _GEN_7 @[Cat.scala 31:58]
    node _subentryStatus_io_valid_list_T = cat(_subentry_valid_subentrySelectedForReq_T_3_0, _subentry_valid_subentrySelectedForReq_T_3_1) @[Cat.scala 31:58]
    node _subentryStatus_io_valid_list_T_1 = bits(_subentryStatus_io_valid_list_T, 0, 0) @[Bitwise.scala 111:18]
    node _subentryStatus_io_valid_list_T_2 = bits(_subentryStatus_io_valid_list_T, 1, 1) @[Bitwise.scala 111:47]
    node _subentryStatus_io_valid_list_T_3 = cat(_subentryStatus_io_valid_list_T_1, _subentryStatus_io_valid_list_T_2) @[Cat.scala 31:58]
    reg mshrStatus_st1_r : UInt<3>, clock with :
      reset => (UInt<1>("h0"), mshrStatus_st1_r) @[L1MSHR.scala 124:33]
    node _T = bits(entryMatchProbe, 0, 0) @[Bitwise.scala 50:65]
    node _T_1 = bits(entryMatchProbe, 1, 1) @[Bitwise.scala 50:65]
    node _T_2 = bits(entryMatchProbe, 2, 2) @[Bitwise.scala 50:65]
    node _T_3 = bits(entryMatchProbe, 3, 3) @[Bitwise.scala 50:65]
    node _T_4 = add(_T, _T_1) @[Bitwise.scala 48:55]
    node _T_5 = bits(_T_4, 1, 0) @[Bitwise.scala 48:55]
    node _T_6 = add(_T_2, _T_3) @[Bitwise.scala 48:55]
    node _T_7 = bits(_T_6, 1, 0) @[Bitwise.scala 48:55]
    node _T_8 = add(_T_5, _T_7) @[Bitwise.scala 48:55]
    node _T_9 = bits(_T_8, 2, 0) @[Bitwise.scala 48:55]
    node _T_10 = leq(_T_9, UInt<1>("h1")) @[L1MSHR.scala 137:36]
    node _T_11 = asUInt(reset) @[L1MSHR.scala 137:9]
    node _T_12 = eq(_T_11, UInt<1>("h0")) @[L1MSHR.scala 137:9]
    node _T_13 = eq(_T_10, UInt<1>("h0")) @[L1MSHR.scala 137:9]
    node secondaryMiss = orr(entryMatchProbe) @[L1MSHR.scala 138:39]
    node primaryMiss = eq(secondaryMiss, UInt<1>("h0")) @[L1MSHR.scala 139:21]
    node _T_14 = and(io_missReq_ready, io_missReq_valid) @[Decoupled.scala 50:35]
    node _T_15 = and(primaryMiss, entryStatus.io_alm_full) @[L1MSHR.scala 145:22]
    node _T_16 = and(secondaryMiss, subentryStatus.io_alm_full) @[L1MSHR.scala 147:30]
    node _GEN_8 = mux(_T_16, UInt<2>("h3"), mshrStatus_st1_r) @[L1MSHR.scala 147:50 148:24 124:33]
    node _GEN_9 = mux(_T_15, UInt<1>("h1"), _GEN_8) @[L1MSHR.scala 145:43 146:24]
    node _GEN_10 = mux(entryStatus.io_full, UInt<1>("h1"), UInt<1>("h0")) @[L1MSHR.scala 152:27 153:26 157:26]
    node _GEN_11 = mux(subentryStatus.io_full, UInt<2>("h3"), UInt<2>("h2")) @[L1MSHR.scala 160:26 161:26 165:26]
    node _GEN_12 = mux(primaryMiss, _GEN_10, _GEN_11) @[L1MSHR.scala 151:23]
    node _T_17 = eq(mshrStatus_st1_r, UInt<1>("h1")) @[L1MSHR.scala 170:27]
    node _T_18 = eq(mshrStatus_st1_r, UInt<2>("h3")) @[L1MSHR.scala 172:33]
    node _T_19 = eq(subentryStatusForRsp.io_used, UInt<1>("h1")) @[L1MSHR.scala 172:73]
    node _T_20 = and(_T_18, _T_19) @[L1MSHR.scala 172:41]
    node _T_21 = eq(mshrStatus_st1_r, UInt<3>("h4")) @[L1MSHR.scala 174:33]
    node _T_22 = eq(subentryStatusForRsp.io_used, UInt<1>("h0")) @[L1MSHR.scala 174:73]
    node _T_23 = and(_T_21, _T_22) @[L1MSHR.scala 174:41]
    node _GEN_13 = mux(_T_23, UInt<2>("h2"), mshrStatus_st1_r) @[L1MSHR.scala 174:82 175:24 124:33]
    node _GEN_14 = mux(_T_20, UInt<3>("h4"), _GEN_13) @[L1MSHR.scala 172:81 173:24]
    node _GEN_15 = mux(_T_17, UInt<1>("h0"), _GEN_14) @[L1MSHR.scala 170:35 171:24]
    node _GEN_16 = mux(io_missRspIn_valid, _GEN_15, mshrStatus_st1_r) @[L1MSHR.scala 124:33 168:33]
    node _GEN_17 = mux(io_probe_valid, _GEN_12, _GEN_16) @[L1MSHR.scala 150:29]
    node _GEN_18 = mux(_T_14, _GEN_9, _GEN_17) @[L1MSHR.scala 144:24]
    reg entryMatchProbe_st1 : UInt<4>, clock with :
      reset => (UInt<1>("h0"), entryMatchProbe_st1) @[Reg.scala 16:16]
    node _GEN_19 = mux(io_probe_valid, entryMatchProbe, entryMatchProbe_st1) @[Reg.scala 16:16 17:{18,22}]
    node _T_24 = eq(mshrStatus_st1_r, UInt<1>("h0")) @[L1MSHR.scala 182:43]
    node _T_25 = eq(mshrStatus_st1_r, UInt<1>("h1")) @[L1MSHR.scala 182:71]
    node _T_26 = or(_T_24, _T_25) @[L1MSHR.scala 182:51]
    node _T_27 = and(secondaryMiss, _T_26) @[L1MSHR.scala 182:22]
    node _GEN_20 = mux(_T_27, _GEN_11, mshrStatus_st1_r) @[L1MSHR.scala 182:80 189:22]
    node mshrStatus_st1_w = _GEN_20 @[L1MSHR.scala 125:30]
    node _io_missReq_ready_T = eq(mshrStatus_st1_w, UInt<1>("h1")) @[L1MSHR.scala 195:42]
    node _io_missReq_ready_T_1 = eq(mshrStatus_st1_w, UInt<2>("h3")) @[L1MSHR.scala 195:70]
    node _io_missReq_ready_T_2 = or(_io_missReq_ready_T, _io_missReq_ready_T_1) @[L1MSHR.scala 195:50]
    node _io_missReq_ready_T_3 = eq(_io_missReq_ready_T_2, UInt<1>("h0")) @[L1MSHR.scala 195:23]
    node _T_28 = and(io_missReq_ready, io_missReq_valid) @[Decoupled.scala 50:35]
    node _T_29 = eq(_T_28, UInt<1>("h0")) @[L1MSHR.scala 196:10]
    node _T_30 = and(io_missReq_ready, io_missReq_valid) @[Decoupled.scala 50:35]
    node _T_31 = and(io_missRspIn_ready, io_missRspIn_valid) @[Decoupled.scala 50:35]
    node _T_32 = eq(_T_31, UInt<1>("h0")) @[L1MSHR.scala 196:50]
    node _T_33 = and(_T_30, _T_32) @[L1MSHR.scala 196:47]
    node _T_34 = or(_T_29, _T_33) @[L1MSHR.scala 196:27]
    node _T_35 = asUInt(reset) @[L1MSHR.scala 196:9]
    node _T_36 = eq(_T_35, UInt<1>("h0")) @[L1MSHR.scala 196:9]
    node _T_37 = eq(_T_34, UInt<1>("h0")) @[L1MSHR.scala 196:9]
    node real_SRAMAddrUp_hi = bits(entryMatchProbe_st1, 3, 2) @[OneHot.scala 30:18]
    node real_SRAMAddrUp_lo = bits(entryMatchProbe_st1, 1, 0) @[OneHot.scala 31:18]
    node _real_SRAMAddrUp_T = orr(real_SRAMAddrUp_hi) @[OneHot.scala 32:14]
    node _real_SRAMAddrUp_T_1 = or(real_SRAMAddrUp_hi, real_SRAMAddrUp_lo) @[OneHot.scala 32:28]
    node _real_SRAMAddrUp_T_2 = bits(_real_SRAMAddrUp_T_1, 1, 1) @[CircuitMath.scala 30:8]
    node _real_SRAMAddrUp_T_3 = cat(_real_SRAMAddrUp_T, _real_SRAMAddrUp_T_2) @[Cat.scala 31:58]
    node real_SRAMAddrUp = mux(secondaryMiss, _real_SRAMAddrUp_T_3, entryStatus.io_next) @[L1MSHR.scala 197:28]
    node real_SRAMAddrDown = mux(secondaryMiss, subentryStatus.io_next, UInt<1>("h0")) @[L1MSHR.scala 198:30]
    node _T_38 = and(io_missReq_ready, io_missReq_valid) @[Decoupled.scala 50:35]
    node _targetInfo_Accesss_real_SRAMAddrUp_real_SRAMAddrDown = io_missReq_bits_targetInfo @[L1MSHR.scala 200:{60,60}]
    node _GEN_21 = mux(and(eq(UInt<1>("h0"), real_SRAMAddrUp), eq(UInt<1>("h0"), real_SRAMAddrDown)), _targetInfo_Accesss_real_SRAMAddrUp_real_SRAMAddrDown, targetInfo_Accesss_0_0) @[L1MSHR.scala 200:{60,60} 83:35]
    node _GEN_22 = mux(and(eq(UInt<1>("h0"), real_SRAMAddrUp), eq(UInt<1>("h1"), real_SRAMAddrDown)), _targetInfo_Accesss_real_SRAMAddrUp_real_SRAMAddrDown, targetInfo_Accesss_0_1) @[L1MSHR.scala 200:{60,60} 83:35]
    node _GEN_23 = mux(and(eq(UInt<1>("h1"), real_SRAMAddrUp), eq(UInt<1>("h0"), real_SRAMAddrDown)), _targetInfo_Accesss_real_SRAMAddrUp_real_SRAMAddrDown, targetInfo_Accesss_1_0) @[L1MSHR.scala 200:{60,60} 83:35]
    node _GEN_24 = mux(and(eq(UInt<1>("h1"), real_SRAMAddrUp), eq(UInt<1>("h1"), real_SRAMAddrDown)), _targetInfo_Accesss_real_SRAMAddrUp_real_SRAMAddrDown, targetInfo_Accesss_1_1) @[L1MSHR.scala 200:{60,60} 83:35]
    node _GEN_25 = mux(and(eq(UInt<2>("h2"), real_SRAMAddrUp), eq(UInt<1>("h0"), real_SRAMAddrDown)), _targetInfo_Accesss_real_SRAMAddrUp_real_SRAMAddrDown, targetInfo_Accesss_2_0) @[L1MSHR.scala 200:{60,60} 83:35]
    node _GEN_26 = mux(and(eq(UInt<2>("h2"), real_SRAMAddrUp), eq(UInt<1>("h1"), real_SRAMAddrDown)), _targetInfo_Accesss_real_SRAMAddrUp_real_SRAMAddrDown, targetInfo_Accesss_2_1) @[L1MSHR.scala 200:{60,60} 83:35]
    node _GEN_27 = mux(and(eq(UInt<2>("h3"), real_SRAMAddrUp), eq(UInt<1>("h0"), real_SRAMAddrDown)), _targetInfo_Accesss_real_SRAMAddrUp_real_SRAMAddrDown, targetInfo_Accesss_3_0) @[L1MSHR.scala 200:{60,60} 83:35]
    node _GEN_28 = mux(and(eq(UInt<2>("h3"), real_SRAMAddrUp), eq(UInt<1>("h1"), real_SRAMAddrDown)), _targetInfo_Accesss_real_SRAMAddrUp_real_SRAMAddrDown, targetInfo_Accesss_3_1) @[L1MSHR.scala 200:{60,60} 83:35]
    node _GEN_29 = mux(_T_38, _GEN_21, targetInfo_Accesss_0_0) @[L1MSHR.scala 199:25 83:35]
    node _GEN_30 = mux(_T_38, _GEN_22, targetInfo_Accesss_0_1) @[L1MSHR.scala 199:25 83:35]
    node _GEN_31 = mux(_T_38, _GEN_23, targetInfo_Accesss_1_0) @[L1MSHR.scala 199:25 83:35]
    node _GEN_32 = mux(_T_38, _GEN_24, targetInfo_Accesss_1_1) @[L1MSHR.scala 199:25 83:35]
    node _GEN_33 = mux(_T_38, _GEN_25, targetInfo_Accesss_2_0) @[L1MSHR.scala 199:25 83:35]
    node _GEN_34 = mux(_T_38, _GEN_26, targetInfo_Accesss_2_1) @[L1MSHR.scala 199:25 83:35]
    node _GEN_35 = mux(_T_38, _GEN_27, targetInfo_Accesss_3_0) @[L1MSHR.scala 199:25 83:35]
    node _GEN_36 = mux(_T_38, _GEN_28, targetInfo_Accesss_3_1) @[L1MSHR.scala 199:25 83:35]
    node _T_39 = and(io_missReq_ready, io_missReq_valid) @[Decoupled.scala 50:35]
    node _T_40 = eq(mshrStatus_st1_w, UInt<1>("h0")) @[L1MSHR.scala 203:44]
    node _T_41 = and(_T_39, _T_40) @[L1MSHR.scala 203:24]
    node _blockAddr_Access_entryStatus_io_next = io_missReq_bits_blockAddr @[L1MSHR.scala 204:{43,43}]
    node _GEN_37 = mux(eq(UInt<1>("h0"), entryStatus.io_next), _blockAddr_Access_entryStatus_io_next, blockAddr_Access_0) @[L1MSHR.scala 204:{43,43} 81:33]
    node _GEN_38 = mux(eq(UInt<1>("h1"), entryStatus.io_next), _blockAddr_Access_entryStatus_io_next, blockAddr_Access_1) @[L1MSHR.scala 204:{43,43} 81:33]
    node _GEN_39 = mux(eq(UInt<2>("h2"), entryStatus.io_next), _blockAddr_Access_entryStatus_io_next, blockAddr_Access_2) @[L1MSHR.scala 204:{43,43} 81:33]
    node _GEN_40 = mux(eq(UInt<2>("h3"), entryStatus.io_next), _blockAddr_Access_entryStatus_io_next, blockAddr_Access_3) @[L1MSHR.scala 204:{43,43} 81:33]
    node _instrId_Access_entryStatus_io_next = io_missReq_bits_instrId @[L1MSHR.scala 205:{41,41}]
    node _GEN_41 = mux(eq(UInt<1>("h0"), entryStatus.io_next), _instrId_Access_entryStatus_io_next, instrId_Access_0) @[L1MSHR.scala 205:{41,41} 82:31]
    node _GEN_42 = mux(eq(UInt<1>("h1"), entryStatus.io_next), _instrId_Access_entryStatus_io_next, instrId_Access_1) @[L1MSHR.scala 205:{41,41} 82:31]
    node _GEN_43 = mux(eq(UInt<2>("h2"), entryStatus.io_next), _instrId_Access_entryStatus_io_next, instrId_Access_2) @[L1MSHR.scala 205:{41,41} 82:31]
    node _GEN_44 = mux(eq(UInt<2>("h3"), entryStatus.io_next), _instrId_Access_entryStatus_io_next, instrId_Access_3) @[L1MSHR.scala 205:{41,41} 82:31]
    node _GEN_45 = mux(_T_41, _GEN_37, blockAddr_Access_0) @[L1MSHR.scala 203:53 81:33]
    node _GEN_46 = mux(_T_41, _GEN_38, blockAddr_Access_1) @[L1MSHR.scala 203:53 81:33]
    node _GEN_47 = mux(_T_41, _GEN_39, blockAddr_Access_2) @[L1MSHR.scala 203:53 81:33]
    node _GEN_48 = mux(_T_41, _GEN_40, blockAddr_Access_3) @[L1MSHR.scala 203:53 81:33]
    node _GEN_49 = mux(_T_41, _GEN_41, instrId_Access_0) @[L1MSHR.scala 203:53 82:31]
    node _GEN_50 = mux(_T_41, _GEN_42, instrId_Access_1) @[L1MSHR.scala 203:53 82:31]
    node _GEN_51 = mux(_T_41, _GEN_43, instrId_Access_2) @[L1MSHR.scala 203:53 82:31]
    node _GEN_52 = mux(_T_41, _GEN_44, instrId_Access_3) @[L1MSHR.scala 203:53 82:31]
    node entryMatchMissRsp = io_missRspIn_bits_instrId @[L1MSHR.scala 110:31 221:21]
    node _GEN_53 = validif(eq(UInt<1>("h0"), entryMatchMissRsp), subentry_valid_0_0) @[Cat.scala 31:{58,58}]
    node _GEN_54 = mux(eq(UInt<1>("h1"), entryMatchMissRsp), subentry_valid_1_0, _GEN_53) @[Cat.scala 31:{58,58}]
    node _GEN_55 = mux(eq(UInt<2>("h2"), entryMatchMissRsp), subentry_valid_2_0, _GEN_54) @[Cat.scala 31:{58,58}]
    node _GEN_56 = mux(eq(UInt<2>("h3"), entryMatchMissRsp), subentry_valid_3_0, _GEN_55) @[Cat.scala 31:{58,58}]
    node _GEN_57 = validif(eq(UInt<1>("h0"), entryMatchMissRsp), subentry_valid_0_1) @[Cat.scala 31:{58,58}]
    node _GEN_58 = mux(eq(UInt<1>("h1"), entryMatchMissRsp), subentry_valid_1_1, _GEN_57) @[Cat.scala 31:{58,58}]
    node _GEN_59 = mux(eq(UInt<2>("h2"), entryMatchMissRsp), subentry_valid_2_1, _GEN_58) @[Cat.scala 31:{58,58}]
    node _GEN_60 = mux(eq(UInt<2>("h3"), entryMatchMissRsp), subentry_valid_3_1, _GEN_59) @[Cat.scala 31:{58,58}]
    node _subentry_valid_entryMatchMissRsp_0 = _GEN_56 @[Cat.scala 31:58]
    node _subentry_valid_entryMatchMissRsp_1 = _GEN_60 @[Cat.scala 31:58]
    node _subentryStatusForRsp_io_valid_list_T = cat(_subentry_valid_entryMatchMissRsp_0, _subentry_valid_entryMatchMissRsp_1) @[Cat.scala 31:58]
    node _subentryStatusForRsp_io_valid_list_T_1 = bits(_subentryStatusForRsp_io_valid_list_T, 0, 0) @[Bitwise.scala 111:18]
    node _subentryStatusForRsp_io_valid_list_T_2 = bits(_subentryStatusForRsp_io_valid_list_T, 1, 1) @[Bitwise.scala 111:47]
    node _subentryStatusForRsp_io_valid_list_T_3 = cat(_subentryStatusForRsp_io_valid_list_T_1, _subentryStatusForRsp_io_valid_list_T_2) @[Cat.scala 31:58]
    node _io_missRspIn_ready_T = geq(subentryStatusForRsp.io_used, UInt<2>("h2")) @[L1MSHR.scala 218:57]
    node _io_missRspIn_ready_T_1 = eq(mshrStatus_st1_w, UInt<3>("h4")) @[L1MSHR.scala 219:24]
    node _io_missRspIn_ready_T_2 = eq(mshrStatus_st1_w, UInt<2>("h3")) @[L1MSHR.scala 219:52]
    node _io_missRspIn_ready_T_3 = or(_io_missRspIn_ready_T_1, _io_missRspIn_ready_T_2) @[L1MSHR.scala 219:32]
    node _io_missRspIn_ready_T_4 = eq(subentryStatusForRsp.io_used, UInt<1>("h1")) @[L1MSHR.scala 219:93]
    node _io_missRspIn_ready_T_5 = and(_io_missRspIn_ready_T_3, _io_missRspIn_ready_T_4) @[L1MSHR.scala 219:61]
    node _io_missRspIn_ready_T_6 = or(_io_missRspIn_ready_T, _io_missRspIn_ready_T_5) @[L1MSHR.scala 218:65]
    node _io_missRspIn_ready_T_7 = eq(_io_missRspIn_ready_T_6, UInt<1>("h0")) @[L1MSHR.scala 218:25]
    reg io_missRspOut_bits_targetInfo_REG : UInt<31>, clock with :
      reset => (UInt<1>("h0"), io_missRspOut_bits_targetInfo_REG) @[L1MSHR.scala 230:43]
    node subentry_next2cancel = subentryStatusForRsp.io_next2cancel @[L1MSHR.scala 224:34 225:24]
    node _GEN_61 = validif(and(eq(UInt<1>("h0"), entryMatchMissRsp), eq(UInt<1>("h0"), subentry_next2cancel)), targetInfo_Accesss_0_0) @[L1MSHR.scala 230:{43,43}]
    node _GEN_62 = mux(and(eq(UInt<1>("h0"), entryMatchMissRsp), eq(UInt<1>("h1"), subentry_next2cancel)), targetInfo_Accesss_0_1, _GEN_61) @[L1MSHR.scala 230:{43,43}]
    node _GEN_63 = mux(and(eq(UInt<1>("h1"), entryMatchMissRsp), eq(UInt<1>("h0"), subentry_next2cancel)), targetInfo_Accesss_1_0, _GEN_62) @[L1MSHR.scala 230:{43,43}]
    node _GEN_64 = mux(and(eq(UInt<1>("h1"), entryMatchMissRsp), eq(UInt<1>("h1"), subentry_next2cancel)), targetInfo_Accesss_1_1, _GEN_63) @[L1MSHR.scala 230:{43,43}]
    node _GEN_65 = mux(and(eq(UInt<2>("h2"), entryMatchMissRsp), eq(UInt<1>("h0"), subentry_next2cancel)), targetInfo_Accesss_2_0, _GEN_64) @[L1MSHR.scala 230:{43,43}]
    node _GEN_66 = mux(and(eq(UInt<2>("h2"), entryMatchMissRsp), eq(UInt<1>("h1"), subentry_next2cancel)), targetInfo_Accesss_2_1, _GEN_65) @[L1MSHR.scala 230:{43,43}]
    node _GEN_67 = mux(and(eq(UInt<2>("h3"), entryMatchMissRsp), eq(UInt<1>("h0"), subentry_next2cancel)), targetInfo_Accesss_3_0, _GEN_66) @[L1MSHR.scala 230:{43,43}]
    node _GEN_68 = mux(and(eq(UInt<2>("h3"), entryMatchMissRsp), eq(UInt<1>("h1"), subentry_next2cancel)), targetInfo_Accesss_3_1, _GEN_67) @[L1MSHR.scala 230:{43,43}]
    reg io_missRspOut_bits_blockAddr_REG : UInt<28>, clock with :
      reset => (UInt<1>("h0"), io_missRspOut_bits_blockAddr_REG) @[L1MSHR.scala 231:42]
    node _GEN_69 = validif(eq(UInt<1>("h0"), entryMatchMissRsp), blockAddr_Access_0) @[L1MSHR.scala 231:{42,42}]
    node _GEN_70 = mux(eq(UInt<1>("h1"), entryMatchMissRsp), blockAddr_Access_1, _GEN_69) @[L1MSHR.scala 231:{42,42}]
    node _GEN_71 = mux(eq(UInt<2>("h2"), entryMatchMissRsp), blockAddr_Access_2, _GEN_70) @[L1MSHR.scala 231:{42,42}]
    node _GEN_72 = mux(eq(UInt<2>("h3"), entryMatchMissRsp), blockAddr_Access_3, _GEN_71) @[L1MSHR.scala 231:{42,42}]
    reg io_missRspOut_valid_REG : UInt<1>, clock with :
      reset => (UInt<1>("h0"), io_missRspOut_valid_REG) @[L1MSHR.scala 233:33]
    node _T_42 = eq(UInt<1>("h0"), entryStatus.io_next) @[L1MSHR.scala 241:24]
    node _T_43 = eq(UInt<1>("h0"), UInt<1>("h0")) @[L1MSHR.scala 242:24]
    node _T_44 = and(_T_42, _T_43) @[L1MSHR.scala 241:47]
    node _T_45 = and(io_missReq_ready, io_missReq_valid) @[Decoupled.scala 50:35]
    node _T_46 = and(_T_44, _T_45) @[L1MSHR.scala 242:31]
    node _T_47 = and(_T_46, primaryMiss) @[L1MSHR.scala 242:50]
    node _T_48 = eq(UInt<1>("h0"), entryMatchMissRsp) @[L1MSHR.scala 244:30]
    node _T_49 = eq(UInt<1>("h0"), subentry_next2cancel) @[L1MSHR.scala 245:29]
    node _T_50 = and(_T_49, io_missRspIn_valid) @[L1MSHR.scala 245:53]
    node _T_51 = eq(UInt<1>("h0"), subentryStatus.io_next) @[L1MSHR.scala 248:35]
    node _T_52 = and(io_missReq_ready, io_missReq_valid) @[Decoupled.scala 50:35]
    node _T_53 = and(_T_51, _T_52) @[L1MSHR.scala 248:61]
    node _T_54 = and(_T_53, secondaryMiss) @[L1MSHR.scala 249:27]
    node _GEN_73 = mux(_T_54, UInt<1>("h1"), subentry_valid_0_0) @[L1MSHR.scala 249:44 250:43 85:31]
    node _GEN_74 = mux(_T_50, UInt<1>("h0"), _GEN_73) @[L1MSHR.scala 246:30 247:43]
    node _GEN_75 = mux(_T_48, _GEN_74, subentry_valid_0_0) @[L1MSHR.scala 244:51 85:31]
    node _GEN_76 = mux(_T_47, UInt<1>("h1"), _GEN_75) @[L1MSHR.scala 242:65 243:41]
    node _T_55 = eq(UInt<1>("h0"), entryStatus.io_next) @[L1MSHR.scala 241:24]
    node _T_56 = eq(UInt<1>("h1"), UInt<1>("h0")) @[L1MSHR.scala 242:24]
    node _T_57 = and(_T_55, _T_56) @[L1MSHR.scala 241:47]
    node _T_58 = and(io_missReq_ready, io_missReq_valid) @[Decoupled.scala 50:35]
    node _T_59 = and(_T_57, _T_58) @[L1MSHR.scala 242:31]
    node _T_60 = and(_T_59, primaryMiss) @[L1MSHR.scala 242:50]
    node _T_61 = eq(UInt<1>("h0"), entryMatchMissRsp) @[L1MSHR.scala 244:30]
    node _T_62 = eq(UInt<1>("h1"), subentry_next2cancel) @[L1MSHR.scala 245:29]
    node _T_63 = and(_T_62, io_missRspIn_valid) @[L1MSHR.scala 245:53]
    node _T_64 = eq(UInt<1>("h1"), subentryStatus.io_next) @[L1MSHR.scala 248:35]
    node _T_65 = and(io_missReq_ready, io_missReq_valid) @[Decoupled.scala 50:35]
    node _T_66 = and(_T_64, _T_65) @[L1MSHR.scala 248:61]
    node _T_67 = and(_T_66, secondaryMiss) @[L1MSHR.scala 249:27]
    node _GEN_77 = mux(_T_67, UInt<1>("h1"), subentry_valid_0_1) @[L1MSHR.scala 249:44 250:43 85:31]
    node _GEN_78 = mux(_T_63, UInt<1>("h0"), _GEN_77) @[L1MSHR.scala 246:30 247:43]
    node _GEN_79 = mux(_T_61, _GEN_78, subentry_valid_0_1) @[L1MSHR.scala 244:51 85:31]
    node _GEN_80 = mux(_T_60, UInt<1>("h1"), _GEN_79) @[L1MSHR.scala 242:65 243:41]
    node _T_68 = eq(UInt<1>("h1"), entryStatus.io_next) @[L1MSHR.scala 241:24]
    node _T_69 = eq(UInt<1>("h0"), UInt<1>("h0")) @[L1MSHR.scala 242:24]
    node _T_70 = and(_T_68, _T_69) @[L1MSHR.scala 241:47]
    node _T_71 = and(io_missReq_ready, io_missReq_valid) @[Decoupled.scala 50:35]
    node _T_72 = and(_T_70, _T_71) @[L1MSHR.scala 242:31]
    node _T_73 = and(_T_72, primaryMiss) @[L1MSHR.scala 242:50]
    node _T_74 = eq(UInt<1>("h1"), entryMatchMissRsp) @[L1MSHR.scala 244:30]
    node _T_75 = eq(UInt<1>("h0"), subentry_next2cancel) @[L1MSHR.scala 245:29]
    node _T_76 = and(_T_75, io_missRspIn_valid) @[L1MSHR.scala 245:53]
    node _T_77 = eq(UInt<1>("h0"), subentryStatus.io_next) @[L1MSHR.scala 248:35]
    node _T_78 = and(io_missReq_ready, io_missReq_valid) @[Decoupled.scala 50:35]
    node _T_79 = and(_T_77, _T_78) @[L1MSHR.scala 248:61]
    node _T_80 = and(_T_79, secondaryMiss) @[L1MSHR.scala 249:27]
    node _GEN_81 = mux(_T_80, UInt<1>("h1"), subentry_valid_1_0) @[L1MSHR.scala 249:44 250:43 85:31]
    node _GEN_82 = mux(_T_76, UInt<1>("h0"), _GEN_81) @[L1MSHR.scala 246:30 247:43]
    node _GEN_83 = mux(_T_74, _GEN_82, subentry_valid_1_0) @[L1MSHR.scala 244:51 85:31]
    node _GEN_84 = mux(_T_73, UInt<1>("h1"), _GEN_83) @[L1MSHR.scala 242:65 243:41]
    node _T_81 = eq(UInt<1>("h1"), entryStatus.io_next) @[L1MSHR.scala 241:24]
    node _T_82 = eq(UInt<1>("h1"), UInt<1>("h0")) @[L1MSHR.scala 242:24]
    node _T_83 = and(_T_81, _T_82) @[L1MSHR.scala 241:47]
    node _T_84 = and(io_missReq_ready, io_missReq_valid) @[Decoupled.scala 50:35]
    node _T_85 = and(_T_83, _T_84) @[L1MSHR.scala 242:31]
    node _T_86 = and(_T_85, primaryMiss) @[L1MSHR.scala 242:50]
    node _T_87 = eq(UInt<1>("h1"), entryMatchMissRsp) @[L1MSHR.scala 244:30]
    node _T_88 = eq(UInt<1>("h1"), subentry_next2cancel) @[L1MSHR.scala 245:29]
    node _T_89 = and(_T_88, io_missRspIn_valid) @[L1MSHR.scala 245:53]
    node _T_90 = eq(UInt<1>("h1"), subentryStatus.io_next) @[L1MSHR.scala 248:35]
    node _T_91 = and(io_missReq_ready, io_missReq_valid) @[Decoupled.scala 50:35]
    node _T_92 = and(_T_90, _T_91) @[L1MSHR.scala 248:61]
    node _T_93 = and(_T_92, secondaryMiss) @[L1MSHR.scala 249:27]
    node _GEN_85 = mux(_T_93, UInt<1>("h1"), subentry_valid_1_1) @[L1MSHR.scala 249:44 250:43 85:31]
    node _GEN_86 = mux(_T_89, UInt<1>("h0"), _GEN_85) @[L1MSHR.scala 246:30 247:43]
    node _GEN_87 = mux(_T_87, _GEN_86, subentry_valid_1_1) @[L1MSHR.scala 244:51 85:31]
    node _GEN_88 = mux(_T_86, UInt<1>("h1"), _GEN_87) @[L1MSHR.scala 242:65 243:41]
    node _T_94 = eq(UInt<2>("h2"), entryStatus.io_next) @[L1MSHR.scala 241:24]
    node _T_95 = eq(UInt<1>("h0"), UInt<1>("h0")) @[L1MSHR.scala 242:24]
    node _T_96 = and(_T_94, _T_95) @[L1MSHR.scala 241:47]
    node _T_97 = and(io_missReq_ready, io_missReq_valid) @[Decoupled.scala 50:35]
    node _T_98 = and(_T_96, _T_97) @[L1MSHR.scala 242:31]
    node _T_99 = and(_T_98, primaryMiss) @[L1MSHR.scala 242:50]
    node _T_100 = eq(UInt<2>("h2"), entryMatchMissRsp) @[L1MSHR.scala 244:30]
    node _T_101 = eq(UInt<1>("h0"), subentry_next2cancel) @[L1MSHR.scala 245:29]
    node _T_102 = and(_T_101, io_missRspIn_valid) @[L1MSHR.scala 245:53]
    node _T_103 = eq(UInt<1>("h0"), subentryStatus.io_next) @[L1MSHR.scala 248:35]
    node _T_104 = and(io_missReq_ready, io_missReq_valid) @[Decoupled.scala 50:35]
    node _T_105 = and(_T_103, _T_104) @[L1MSHR.scala 248:61]
    node _T_106 = and(_T_105, secondaryMiss) @[L1MSHR.scala 249:27]
    node _GEN_89 = mux(_T_106, UInt<1>("h1"), subentry_valid_2_0) @[L1MSHR.scala 249:44 250:43 85:31]
    node _GEN_90 = mux(_T_102, UInt<1>("h0"), _GEN_89) @[L1MSHR.scala 246:30 247:43]
    node _GEN_91 = mux(_T_100, _GEN_90, subentry_valid_2_0) @[L1MSHR.scala 244:51 85:31]
    node _GEN_92 = mux(_T_99, UInt<1>("h1"), _GEN_91) @[L1MSHR.scala 242:65 243:41]
    node _T_107 = eq(UInt<2>("h2"), entryStatus.io_next) @[L1MSHR.scala 241:24]
    node _T_108 = eq(UInt<1>("h1"), UInt<1>("h0")) @[L1MSHR.scala 242:24]
    node _T_109 = and(_T_107, _T_108) @[L1MSHR.scala 241:47]
    node _T_110 = and(io_missReq_ready, io_missReq_valid) @[Decoupled.scala 50:35]
    node _T_111 = and(_T_109, _T_110) @[L1MSHR.scala 242:31]
    node _T_112 = and(_T_111, primaryMiss) @[L1MSHR.scala 242:50]
    node _T_113 = eq(UInt<2>("h2"), entryMatchMissRsp) @[L1MSHR.scala 244:30]
    node _T_114 = eq(UInt<1>("h1"), subentry_next2cancel) @[L1MSHR.scala 245:29]
    node _T_115 = and(_T_114, io_missRspIn_valid) @[L1MSHR.scala 245:53]
    node _T_116 = eq(UInt<1>("h1"), subentryStatus.io_next) @[L1MSHR.scala 248:35]
    node _T_117 = and(io_missReq_ready, io_missReq_valid) @[Decoupled.scala 50:35]
    node _T_118 = and(_T_116, _T_117) @[L1MSHR.scala 248:61]
    node _T_119 = and(_T_118, secondaryMiss) @[L1MSHR.scala 249:27]
    node _GEN_93 = mux(_T_119, UInt<1>("h1"), subentry_valid_2_1) @[L1MSHR.scala 249:44 250:43 85:31]
    node _GEN_94 = mux(_T_115, UInt<1>("h0"), _GEN_93) @[L1MSHR.scala 246:30 247:43]
    node _GEN_95 = mux(_T_113, _GEN_94, subentry_valid_2_1) @[L1MSHR.scala 244:51 85:31]
    node _GEN_96 = mux(_T_112, UInt<1>("h1"), _GEN_95) @[L1MSHR.scala 242:65 243:41]
    node _T_120 = eq(UInt<2>("h3"), entryStatus.io_next) @[L1MSHR.scala 241:24]
    node _T_121 = eq(UInt<1>("h0"), UInt<1>("h0")) @[L1MSHR.scala 242:24]
    node _T_122 = and(_T_120, _T_121) @[L1MSHR.scala 241:47]
    node _T_123 = and(io_missReq_ready, io_missReq_valid) @[Decoupled.scala 50:35]
    node _T_124 = and(_T_122, _T_123) @[L1MSHR.scala 242:31]
    node _T_125 = and(_T_124, primaryMiss) @[L1MSHR.scala 242:50]
    node _T_126 = eq(UInt<2>("h3"), entryMatchMissRsp) @[L1MSHR.scala 244:30]
    node _T_127 = eq(UInt<1>("h0"), subentry_next2cancel) @[L1MSHR.scala 245:29]
    node _T_128 = and(_T_127, io_missRspIn_valid) @[L1MSHR.scala 245:53]
    node _T_129 = eq(UInt<1>("h0"), subentryStatus.io_next) @[L1MSHR.scala 248:35]
    node _T_130 = and(io_missReq_ready, io_missReq_valid) @[Decoupled.scala 50:35]
    node _T_131 = and(_T_129, _T_130) @[L1MSHR.scala 248:61]
    node _T_132 = and(_T_131, secondaryMiss) @[L1MSHR.scala 249:27]
    node _GEN_97 = mux(_T_132, UInt<1>("h1"), subentry_valid_3_0) @[L1MSHR.scala 249:44 250:43 85:31]
    node _GEN_98 = mux(_T_128, UInt<1>("h0"), _GEN_97) @[L1MSHR.scala 246:30 247:43]
    node _GEN_99 = mux(_T_126, _GEN_98, subentry_valid_3_0) @[L1MSHR.scala 244:51 85:31]
    node _GEN_100 = mux(_T_125, UInt<1>("h1"), _GEN_99) @[L1MSHR.scala 242:65 243:41]
    node _T_133 = eq(UInt<2>("h3"), entryStatus.io_next) @[L1MSHR.scala 241:24]
    node _T_134 = eq(UInt<1>("h1"), UInt<1>("h0")) @[L1MSHR.scala 242:24]
    node _T_135 = and(_T_133, _T_134) @[L1MSHR.scala 241:47]
    node _T_136 = and(io_missReq_ready, io_missReq_valid) @[Decoupled.scala 50:35]
    node _T_137 = and(_T_135, _T_136) @[L1MSHR.scala 242:31]
    node _T_138 = and(_T_137, primaryMiss) @[L1MSHR.scala 242:50]
    node _T_139 = eq(UInt<2>("h3"), entryMatchMissRsp) @[L1MSHR.scala 244:30]
    node _T_140 = eq(UInt<1>("h1"), subentry_next2cancel) @[L1MSHR.scala 245:29]
    node _T_141 = and(_T_140, io_missRspIn_valid) @[L1MSHR.scala 245:53]
    node _T_142 = eq(UInt<1>("h1"), subentryStatus.io_next) @[L1MSHR.scala 248:35]
    node _T_143 = and(io_missReq_ready, io_missReq_valid) @[Decoupled.scala 50:35]
    node _T_144 = and(_T_142, _T_143) @[L1MSHR.scala 248:61]
    node _T_145 = and(_T_144, secondaryMiss) @[L1MSHR.scala 249:27]
    node _GEN_101 = mux(_T_145, UInt<1>("h1"), subentry_valid_3_1) @[L1MSHR.scala 249:44 250:43 85:31]
    node _GEN_102 = mux(_T_141, UInt<1>("h0"), _GEN_101) @[L1MSHR.scala 246:30 247:43]
    node _GEN_103 = mux(_T_139, _GEN_102, subentry_valid_3_1) @[L1MSHR.scala 244:51 85:31]
    node _GEN_104 = mux(_T_138, UInt<1>("h1"), _GEN_103) @[L1MSHR.scala 242:65 243:41]
    node _blockAddr_Access_WIRE_0 = UInt<28>("h0") @[L1MSHR.scala 81:{41,41}]
    node _blockAddr_Access_WIRE_1 = UInt<28>("h0") @[L1MSHR.scala 81:{41,41}]
    node _blockAddr_Access_WIRE_2 = UInt<28>("h0") @[L1MSHR.scala 81:{41,41}]
    node _blockAddr_Access_WIRE_3 = UInt<28>("h0") @[L1MSHR.scala 81:{41,41}]
    node _instrId_Access_WIRE_0 = UInt<3>("h0") @[L1MSHR.scala 82:{39,39}]
    node _instrId_Access_WIRE_1 = UInt<3>("h0") @[L1MSHR.scala 82:{39,39}]
    node _instrId_Access_WIRE_2 = UInt<3>("h0") @[L1MSHR.scala 82:{39,39}]
    node _instrId_Access_WIRE_3 = UInt<3>("h0") @[L1MSHR.scala 82:{39,39}]
    node _targetInfo_Accesss_WIRE__0 = UInt<31>("h0") @[L1MSHR.scala 83:{72,72}]
    node _targetInfo_Accesss_WIRE__1 = UInt<31>("h0") @[L1MSHR.scala 83:{72,72}]
    node _targetInfo_Accesss_WIRE_1_0 = UInt<31>("h0") @[L1MSHR.scala 83:{72,72}]
    node _targetInfo_Accesss_WIRE_1_1 = UInt<31>("h0") @[L1MSHR.scala 83:{72,72}]
    node _targetInfo_Accesss_WIRE_2_0 = UInt<31>("h0") @[L1MSHR.scala 83:{72,72}]
    node _targetInfo_Accesss_WIRE_2_1 = UInt<31>("h0") @[L1MSHR.scala 83:{72,72}]
    node _targetInfo_Accesss_WIRE_3_0 = UInt<31>("h0") @[L1MSHR.scala 83:{72,72}]
    node _targetInfo_Accesss_WIRE_3_1 = UInt<31>("h0") @[L1MSHR.scala 83:{72,72}]
    node _targetInfo_Accesss_WIRE_4_0_0 = _targetInfo_Accesss_WIRE__0 @[L1MSHR.scala 83:{43,43}]
    node _targetInfo_Accesss_WIRE_4_0_1 = _targetInfo_Accesss_WIRE__1 @[L1MSHR.scala 83:{43,43}]
    node _targetInfo_Accesss_WIRE_4_1_0 = _targetInfo_Accesss_WIRE_1_0 @[L1MSHR.scala 83:{43,43}]
    node _targetInfo_Accesss_WIRE_4_1_1 = _targetInfo_Accesss_WIRE_1_1 @[L1MSHR.scala 83:{43,43}]
    node _targetInfo_Accesss_WIRE_4_2_0 = _targetInfo_Accesss_WIRE_2_0 @[L1MSHR.scala 83:{43,43}]
    node _targetInfo_Accesss_WIRE_4_2_1 = _targetInfo_Accesss_WIRE_2_1 @[L1MSHR.scala 83:{43,43}]
    node _targetInfo_Accesss_WIRE_4_3_0 = _targetInfo_Accesss_WIRE_3_0 @[L1MSHR.scala 83:{43,43}]
    node _targetInfo_Accesss_WIRE_4_3_1 = _targetInfo_Accesss_WIRE_3_1 @[L1MSHR.scala 83:{43,43}]
    node _subentry_valid_WIRE__0 = UInt<1>("h0") @[L1MSHR.scala 85:{68,68}]
    node _subentry_valid_WIRE__1 = UInt<1>("h0") @[L1MSHR.scala 85:{68,68}]
    node _subentry_valid_WIRE_1_0 = UInt<1>("h0") @[L1MSHR.scala 85:{68,68}]
    node _subentry_valid_WIRE_1_1 = UInt<1>("h0") @[L1MSHR.scala 85:{68,68}]
    node _subentry_valid_WIRE_2_0 = UInt<1>("h0") @[L1MSHR.scala 85:{68,68}]
    node _subentry_valid_WIRE_2_1 = UInt<1>("h0") @[L1MSHR.scala 85:{68,68}]
    node _subentry_valid_WIRE_3_0 = UInt<1>("h0") @[L1MSHR.scala 85:{68,68}]
    node _subentry_valid_WIRE_3_1 = UInt<1>("h0") @[L1MSHR.scala 85:{68,68}]
    node _subentry_valid_WIRE_4_0_0 = _subentry_valid_WIRE__0 @[L1MSHR.scala 85:{39,39}]
    node _subentry_valid_WIRE_4_0_1 = _subentry_valid_WIRE__1 @[L1MSHR.scala 85:{39,39}]
    node _subentry_valid_WIRE_4_1_0 = _subentry_valid_WIRE_1_0 @[L1MSHR.scala 85:{39,39}]
    node _subentry_valid_WIRE_4_1_1 = _subentry_valid_WIRE_1_1 @[L1MSHR.scala 85:{39,39}]
    node _subentry_valid_WIRE_4_2_0 = _subentry_valid_WIRE_2_0 @[L1MSHR.scala 85:{39,39}]
    node _subentry_valid_WIRE_4_2_1 = _subentry_valid_WIRE_2_1 @[L1MSHR.scala 85:{39,39}]
    node _subentry_valid_WIRE_4_3_0 = _subentry_valid_WIRE_3_0 @[L1MSHR.scala 85:{39,39}]
    node _subentry_valid_WIRE_4_3_1 = _subentry_valid_WIRE_3_1 @[L1MSHR.scala 85:{39,39}]
    node _targetInfo_Accesss_entryMatchMissRsp_subentry_next2cancel = _GEN_68 @[L1MSHR.scala 230:43]
    node _blockAddr_Access_entryMatchMissRsp = _GEN_72 @[L1MSHR.scala 231:42]
    io_probeOut_st1_probeStatus <= mshrStatus_st1_w @[L1MSHR.scala 191:31]
    io_probeOut_st1_a_source <= real_SRAMAddrUp @[L1MSHR.scala 208:28]
    io_missReq_ready <= _io_missReq_ready_T_3 @[L1MSHR.scala 195:20]
    io_missRspIn_ready <= _io_missRspIn_ready_T_7 @[L1MSHR.scala 218:22]
    io_missRspOut_valid <= io_missRspOut_valid_REG @[L1MSHR.scala 233:23]
    io_missRspOut_bits_targetInfo <= io_missRspOut_bits_targetInfo_REG @[L1MSHR.scala 230:33]
    io_missRspOut_bits_blockAddr <= io_missRspOut_bits_blockAddr_REG @[L1MSHR.scala 231:32]
    io_missRspOut_bits_instrId <= pad(io_missRspIn_bits_instrId, 3) @[L1MSHR.scala 232:30]
    io_empty <= _io_empty_T_1 @[L1MSHR.scala 87:12]
    blockAddr_Access_0 <= mux(reset, _blockAddr_Access_WIRE_0, _GEN_45) @[L1MSHR.scala 81:{33,33}]
    blockAddr_Access_1 <= mux(reset, _blockAddr_Access_WIRE_1, _GEN_46) @[L1MSHR.scala 81:{33,33}]
    blockAddr_Access_2 <= mux(reset, _blockAddr_Access_WIRE_2, _GEN_47) @[L1MSHR.scala 81:{33,33}]
    blockAddr_Access_3 <= mux(reset, _blockAddr_Access_WIRE_3, _GEN_48) @[L1MSHR.scala 81:{33,33}]
    instrId_Access_0 <= mux(reset, _instrId_Access_WIRE_0, _GEN_49) @[L1MSHR.scala 82:{31,31}]
    instrId_Access_1 <= mux(reset, _instrId_Access_WIRE_1, _GEN_50) @[L1MSHR.scala 82:{31,31}]
    instrId_Access_2 <= mux(reset, _instrId_Access_WIRE_2, _GEN_51) @[L1MSHR.scala 82:{31,31}]
    instrId_Access_3 <= mux(reset, _instrId_Access_WIRE_3, _GEN_52) @[L1MSHR.scala 82:{31,31}]
    targetInfo_Accesss_0_0 <= mux(reset, _targetInfo_Accesss_WIRE_4_0_0, _GEN_29) @[L1MSHR.scala 83:{35,35}]
    targetInfo_Accesss_0_1 <= mux(reset, _targetInfo_Accesss_WIRE_4_0_1, _GEN_30) @[L1MSHR.scala 83:{35,35}]
    targetInfo_Accesss_1_0 <= mux(reset, _targetInfo_Accesss_WIRE_4_1_0, _GEN_31) @[L1MSHR.scala 83:{35,35}]
    targetInfo_Accesss_1_1 <= mux(reset, _targetInfo_Accesss_WIRE_4_1_1, _GEN_32) @[L1MSHR.scala 83:{35,35}]
    targetInfo_Accesss_2_0 <= mux(reset, _targetInfo_Accesss_WIRE_4_2_0, _GEN_33) @[L1MSHR.scala 83:{35,35}]
    targetInfo_Accesss_2_1 <= mux(reset, _targetInfo_Accesss_WIRE_4_2_1, _GEN_34) @[L1MSHR.scala 83:{35,35}]
    targetInfo_Accesss_3_0 <= mux(reset, _targetInfo_Accesss_WIRE_4_3_0, _GEN_35) @[L1MSHR.scala 83:{35,35}]
    targetInfo_Accesss_3_1 <= mux(reset, _targetInfo_Accesss_WIRE_4_3_1, _GEN_36) @[L1MSHR.scala 83:{35,35}]
    subentry_valid_0_0 <= mux(reset, _subentry_valid_WIRE_4_0_0, _GEN_76) @[L1MSHR.scala 85:{31,31}]
    subentry_valid_0_1 <= mux(reset, _subentry_valid_WIRE_4_0_1, _GEN_80) @[L1MSHR.scala 85:{31,31}]
    subentry_valid_1_0 <= mux(reset, _subentry_valid_WIRE_4_1_0, _GEN_84) @[L1MSHR.scala 85:{31,31}]
    subentry_valid_1_1 <= mux(reset, _subentry_valid_WIRE_4_1_1, _GEN_88) @[L1MSHR.scala 85:{31,31}]
    subentry_valid_2_0 <= mux(reset, _subentry_valid_WIRE_4_2_0, _GEN_92) @[L1MSHR.scala 85:{31,31}]
    subentry_valid_2_1 <= mux(reset, _subentry_valid_WIRE_4_2_1, _GEN_96) @[L1MSHR.scala 85:{31,31}]
    subentry_valid_3_0 <= mux(reset, _subentry_valid_WIRE_4_3_0, _GEN_100) @[L1MSHR.scala 85:{31,31}]
    subentry_valid_3_1 <= mux(reset, _subentry_valid_WIRE_4_3_1, _GEN_104) @[L1MSHR.scala 85:{31,31}]
    subentryStatus.clock <= clock
    subentryStatus.reset <= reset
    subentryStatus.io_valid_list <= _subentryStatus_io_valid_list_T_3 @[L1MSHR.scala 114:32]
    subentryStatusForRsp.clock <= clock
    subentryStatusForRsp.reset <= reset
    subentryStatusForRsp.io_valid_list <= _subentryStatusForRsp_io_valid_list_T_3 @[L1MSHR.scala 211:38]
    entryStatus.clock <= clock
    entryStatus.reset <= reset
    entryStatus.io_valid_list <= entry_valid @[L1MSHR.scala 121:29]
    mshrStatus_st1_r <= mux(reset, UInt<3>("h0"), _GEN_18) @[L1MSHR.scala 124:{33,33}]
    entryMatchProbe_st1 <= _GEN_19
    io_missRspOut_bits_targetInfo_REG <= _targetInfo_Accesss_entryMatchMissRsp_subentry_next2cancel @[L1MSHR.scala 230:43]
    io_missRspOut_bits_blockAddr_REG <= _blockAddr_Access_entryMatchMissRsp @[L1MSHR.scala 231:42]
    io_missRspOut_valid_REG <= io_missRspIn_valid @[L1MSHR.scala 233:33]
    printf(clock, and(and(and(UInt<1>("h1"), _T_12), _T_13), UInt<1>("h1")), "Assertion failed\n    at L1MSHR.scala:137 assert(PopCount(entryMatchProbe) <= 1.U)\n") : printf @[L1MSHR.scala 137:9]
    assert(clock, _T_10, and(and(UInt<1>("h1"), _T_12), UInt<1>("h1")), "") : assert @[L1MSHR.scala 137:9]
    printf(clock, and(and(and(UInt<1>("h1"), _T_36), _T_37), UInt<1>("h1")), "Assertion failed: MSHR cant have Req & Rsp valid in same cycle, later the prior\n    at L1MSHR.scala:196 assert(!io.missReq.fire || (io.missReq.fire && !io.missRspIn.fire),\"MSHR cant have Req & Rsp valid in same cycle, later the prior\")\n") : printf_1 @[L1MSHR.scala 196:9]
    assert(clock, _T_34, and(and(UInt<1>("h1"), _T_36), UInt<1>("h1")), "") : assert_1 @[L1MSHR.scala 196:9]

  module SRAMTemplate :
    input clock : Clock
    input reset : UInt<1>
    output io_r_req_ready : UInt<1>
    input io_r_req_valid : UInt<1>
    input io_r_req_bits_setIdx : UInt<2>
    output io_r_resp_data_0 : UInt<26>
    output io_r_resp_data_1 : UInt<26>
    output io_w_req_ready : UInt<1>
    input io_w_req_valid : UInt<1>
    input io_w_req_bits_setIdx : UInt<2>
    input io_w_req_bits_data_0 : UInt<26>
    input io_w_req_bits_data_1 : UInt<26>
    input io_w_req_bits_waymask : UInt<2>

    mem array_0 : @[SRAMTemplate.scala 96:26]
      data-type => UInt<26>
      depth => 4
      read-latency => 1
      write-latency => 1
      reader => raw_rdata
      writer => MPORT
      read-under-write => undefined
    mem array_1 : @[SRAMTemplate.scala 96:26]
      data-type => UInt<26>
      depth => 4
      read-latency => 1
      write-latency => 1
      reader => raw_rdata
      writer => MPORT
      read-under-write => undefined
    node resetState = UInt<1>("h0")
    node wen = or(io_w_req_valid, resetState) @[SRAMTemplate.scala 108:52]
    node resetSet = UInt<1>("h0")
    node setIdx = mux(resetState, resetSet, io_w_req_bits_setIdx) @[SRAMTemplate.scala 111:19]
    node _wdata_WIRE__0 = UInt<26>("h0") @[SRAMTemplate.scala 112:{51,51}]
    node _wdata_T_0 = mux(resetState, _wdata_WIRE__0, io_w_req_bits_data_0) @[SRAMTemplate.scala 112:26]
    node _wdata_WIRE__1 = UInt<26>("h0") @[SRAMTemplate.scala 112:{51,51}]
    node _wdata_T_1 = mux(resetState, _wdata_WIRE__1, io_w_req_bits_data_1) @[SRAMTemplate.scala 112:26]
    node _waymask_T = mux(UInt<1>("h1"), UInt<2>("h3"), UInt<2>("h0")) @[Bitwise.scala 74:12]
    node waymask = mux(resetState, _waymask_T, io_w_req_bits_waymask) @[SRAMTemplate.scala 113:20]
    node _T = bits(waymask, 0, 0) @[SRAMTemplate.scala 114:51]
    node _T_1 = bits(waymask, 1, 1) @[SRAMTemplate.scala 114:51]
    node _T_2 = or(setIdx, UInt<2>("h0"))
    node _T_3 = bits(_T_2, 1, 0)
    node _wdata_WIRE_1 = _wdata_T_0 @[SRAMTemplate.scala 112:{102,102}]
    node wdata_0 = _wdata_WIRE_1 @[SRAMTemplate.scala 112:{22,22}]
    node _GEN_0 = validif(_T, wdata_0)
    node _GEN_1 = mux(_T, UInt<1>("h1"), UInt<1>("h0"))
    node _wdata_WIRE_2 = _wdata_T_1 @[SRAMTemplate.scala 112:{102,102}]
    node wdata_1 = _wdata_WIRE_2 @[SRAMTemplate.scala 112:{22,22}]
    node _GEN_2 = validif(_T_1, wdata_1)
    node _GEN_3 = mux(_T_1, UInt<1>("h1"), UInt<1>("h0"))
    node _GEN_4 = validif(wen, _T_3) @[SRAMTemplate.scala 114:14]
    node _GEN_5 = validif(wen, clock) @[SRAMTemplate.scala 114:14]
    node _GEN_6 = mux(wen, UInt<1>("h1"), UInt<1>("h0")) @[SRAMTemplate.scala 114:14 96:26]
    node _GEN_7 = validif(wen, _GEN_1) @[SRAMTemplate.scala 114:14]
    node _GEN_8 = validif(wen, _GEN_3) @[SRAMTemplate.scala 114:14]
    node _GEN_9 = validif(wen, _GEN_0) @[SRAMTemplate.scala 114:14]
    node _GEN_10 = validif(wen, _GEN_2) @[SRAMTemplate.scala 114:14]
    node _GEN_11 = validif(io_r_req_valid, io_r_req_bits_setIdx) @[SRAMTemplate.scala 116:{29,29}]
    node _raw_rdata_WIRE = _GEN_11 @[SRAMTemplate.scala 116:29]
    node _raw_rdata_T = or(_raw_rdata_WIRE, UInt<2>("h0")) @[SRAMTemplate.scala 116:29]
    node _raw_rdata_T_1 = bits(_raw_rdata_T, 1, 0) @[SRAMTemplate.scala 116:29]
    node _GEN_12 = mux(io_r_req_valid, UInt<1>("h1"), UInt<1>("h0")) @[SRAMTemplate.scala 116:{29,29} 96:26]
    node _GEN_13 = validif(io_r_req_valid, _raw_rdata_T_1) @[SRAMTemplate.scala 116:{29,29}]
    node _GEN_14 = validif(io_r_req_valid, clock) @[SRAMTemplate.scala 116:{29,29}]
    reg bypass_wdata_lfsr : UInt<64>, clock with :
      reset => (UInt<1>("h0"), bypass_wdata_lfsr) @[LFSR64.scala 20:23]
    node _bypass_wdata_xor_T = bits(bypass_wdata_lfsr, 0, 0) @[LFSR64.scala 21:19]
    node _bypass_wdata_xor_T_1 = bits(bypass_wdata_lfsr, 1, 1) @[LFSR64.scala 21:29]
    node _bypass_wdata_xor_T_2 = xor(_bypass_wdata_xor_T, _bypass_wdata_xor_T_1) @[LFSR64.scala 21:23]
    node _bypass_wdata_xor_T_3 = bits(bypass_wdata_lfsr, 3, 3) @[LFSR64.scala 21:39]
    node _bypass_wdata_xor_T_4 = xor(_bypass_wdata_xor_T_2, _bypass_wdata_xor_T_3) @[LFSR64.scala 21:33]
    node _bypass_wdata_xor_T_5 = bits(bypass_wdata_lfsr, 4, 4) @[LFSR64.scala 21:49]
    node bypass_wdata_xor = xor(_bypass_wdata_xor_T_4, _bypass_wdata_xor_T_5) @[LFSR64.scala 21:43]
    node _bypass_wdata_lfsr_T = eq(bypass_wdata_lfsr, UInt<1>("h0")) @[LFSR64.scala 23:24]
    node _bypass_wdata_lfsr_T_1 = bits(bypass_wdata_lfsr, 63, 1) @[LFSR64.scala 23:51]
    node _bypass_wdata_lfsr_T_2 = cat(bypass_wdata_xor, _bypass_wdata_lfsr_T_1) @[Cat.scala 31:58]
    node _bypass_wdata_lfsr_T_3 = mux(_bypass_wdata_lfsr_T, UInt<1>("h1"), _bypass_wdata_lfsr_T_2) @[LFSR64.scala 23:18]
    node _GEN_15 = mux(UInt<1>("h1"), _bypass_wdata_lfsr_T_3, bypass_wdata_lfsr) @[LFSR64.scala 22:22 23:12 20:23]
    reg bypass_wdata_lfsr_1 : UInt<64>, clock with :
      reset => (UInt<1>("h0"), bypass_wdata_lfsr_1) @[LFSR64.scala 20:23]
    node _bypass_wdata_xor_T_6 = bits(bypass_wdata_lfsr_1, 0, 0) @[LFSR64.scala 21:19]
    node _bypass_wdata_xor_T_7 = bits(bypass_wdata_lfsr_1, 1, 1) @[LFSR64.scala 21:29]
    node _bypass_wdata_xor_T_8 = xor(_bypass_wdata_xor_T_6, _bypass_wdata_xor_T_7) @[LFSR64.scala 21:23]
    node _bypass_wdata_xor_T_9 = bits(bypass_wdata_lfsr_1, 3, 3) @[LFSR64.scala 21:39]
    node _bypass_wdata_xor_T_10 = xor(_bypass_wdata_xor_T_8, _bypass_wdata_xor_T_9) @[LFSR64.scala 21:33]
    node _bypass_wdata_xor_T_11 = bits(bypass_wdata_lfsr_1, 4, 4) @[LFSR64.scala 21:49]
    node bypass_wdata_xor_1 = xor(_bypass_wdata_xor_T_10, _bypass_wdata_xor_T_11) @[LFSR64.scala 21:43]
    node _bypass_wdata_lfsr_T_4 = eq(bypass_wdata_lfsr_1, UInt<1>("h0")) @[LFSR64.scala 23:24]
    node _bypass_wdata_lfsr_T_5 = bits(bypass_wdata_lfsr_1, 63, 1) @[LFSR64.scala 23:51]
    node _bypass_wdata_lfsr_T_6 = cat(bypass_wdata_xor_1, _bypass_wdata_lfsr_T_5) @[Cat.scala 31:58]
    node _bypass_wdata_lfsr_T_7 = mux(_bypass_wdata_lfsr_T_4, UInt<1>("h1"), _bypass_wdata_lfsr_T_6) @[LFSR64.scala 23:18]
    node _GEN_16 = mux(UInt<1>("h1"), _bypass_wdata_lfsr_T_7, bypass_wdata_lfsr_1) @[LFSR64.scala 22:22 23:12 20:23]
    node _bypass_mask_need_check_T = and(io_r_req_valid, io_w_req_valid) @[SRAMTemplate.scala 121:34]
    reg bypass_mask_need_check : UInt<1>, clock with :
      reset => (UInt<1>("h0"), bypass_mask_need_check) @[SRAMTemplate.scala 121:29]
    reg bypass_mask_waddr_reg : UInt<2>, clock with :
      reset => (UInt<1>("h0"), bypass_mask_waddr_reg) @[SRAMTemplate.scala 122:28]
    reg bypass_mask_raddr_reg : UInt<2>, clock with :
      reset => (UInt<1>("h0"), bypass_mask_raddr_reg) @[SRAMTemplate.scala 123:28]
    node _bypass_mask_bypass_T = eq(bypass_mask_waddr_reg, bypass_mask_raddr_reg) @[SRAMTemplate.scala 125:52]
    node _bypass_mask_bypass_T_1 = and(bypass_mask_need_check, _bypass_mask_bypass_T) @[SRAMTemplate.scala 125:39]
    node _bypass_mask_bypass_T_2 = bits(_bypass_mask_bypass_T_1, 0, 0) @[Bitwise.scala 74:15]
    node _bypass_mask_bypass_T_3 = mux(_bypass_mask_bypass_T_2, UInt<2>("h3"), UInt<2>("h0")) @[Bitwise.scala 74:12]
    reg bypass_mask_bypass_REG : UInt<2>, clock with :
      reset => (UInt<1>("h0"), bypass_mask_bypass_REG) @[SRAMTemplate.scala 125:76]
    node bypass_mask_bypass = and(_bypass_mask_bypass_T_3, bypass_mask_bypass_REG) @[SRAMTemplate.scala 125:67]
    node bypass_mask = bypass_mask_bypass @[SRAMTemplate.scala 126:{20,20}]
    node _mem_rdata_T = bits(bypass_mask, 0, 0) @[SRAMTemplate.scala 133:30]
    node _mem_rdata_T_1 = bits(bypass_mask, 1, 1) @[SRAMTemplate.scala 133:30]
    node _bypass_wdata_WIRE = bits(bypass_wdata_lfsr, 25, 0) @[SRAMTemplate.scala 129:{58,58}]
    node bypass_wdata_0 = _bypass_wdata_WIRE @[SRAMTemplate.scala 129:{17,17}]
    node _mem_rdata_T_2 = mux(_mem_rdata_T, bypass_wdata_0, array_0.raw_rdata.data) @[SRAMTemplate.scala 134:30]
    node _bypass_wdata_WIRE_1 = bits(bypass_wdata_lfsr_1, 25, 0) @[SRAMTemplate.scala 129:{58,58}]
    node bypass_wdata_1 = _bypass_wdata_WIRE_1 @[SRAMTemplate.scala 129:{17,17}]
    node _mem_rdata_T_3 = mux(_mem_rdata_T_1, bypass_wdata_1, array_1.raw_rdata.data) @[SRAMTemplate.scala 134:30]
    reg rdata_REG : UInt<1>, clock with :
      reset => (UInt<1>("h0"), rdata_REG) @[SRAMTemplate.scala 139:59]
    reg rdata_r_0 : UInt<26>, clock with :
      reset => (UInt<1>("h0"), rdata_r_0) @[Reg.scala 28:20]
    reg rdata_r_1 : UInt<26>, clock with :
      reset => (UInt<1>("h0"), rdata_r_1) @[Reg.scala 28:20]
    node mem_rdata_0 = _mem_rdata_T_2 @[SRAMTemplate.scala 133:{17,17}]
    node _GEN_17 = mux(rdata_REG, mem_rdata_0, rdata_r_0) @[Reg.scala 29:18 28:20 29:22]
    node mem_rdata_1 = _mem_rdata_T_3 @[SRAMTemplate.scala 133:{17,17}]
    node _GEN_18 = mux(rdata_REG, mem_rdata_1, rdata_r_1) @[Reg.scala 29:18 28:20 29:22]
    node _rdata_T_0 = mux(rdata_REG, mem_rdata_0, rdata_r_0) @[Hold.scala 18:48]
    node _rdata_T_1 = mux(rdata_REG, mem_rdata_1, rdata_r_1) @[Hold.scala 18:48]
    node _io_r_req_ready_T = eq(resetState, UInt<1>("h0")) @[SRAMTemplate.scala 143:21]
    node _io_r_req_ready_T_1 = and(_io_r_req_ready_T, UInt<1>("h1")) @[SRAMTemplate.scala 143:33]
    node _rdata_WIRE_0 = UInt<26>("h0") @[Hold.scala 18:{81,81}]
    node _rdata_WIRE_1 = UInt<26>("h0") @[Hold.scala 18:{81,81}]
    node rdata_0 = _rdata_T_0 @[SRAMTemplate.scala 140:{45,45}]
    node rdata_1 = _rdata_T_1 @[SRAMTemplate.scala 140:{45,45}]
    node _WIRE_0 = rdata_0 @[SRAMTemplate.scala 142:{28,28}]
    node _WIRE_1 = rdata_1 @[SRAMTemplate.scala 142:{28,28}]
    io_r_req_ready <= _io_r_req_ready_T_1 @[SRAMTemplate.scala 143:18]
    io_r_resp_data_0 <= _WIRE_0 @[SRAMTemplate.scala 142:18]
    io_r_resp_data_1 <= _WIRE_1 @[SRAMTemplate.scala 142:18]
    io_w_req_ready <= UInt<1>("h1") @[SRAMTemplate.scala 144:18]
    array_0.raw_rdata.addr <= _GEN_13
    array_1.raw_rdata.addr <= _GEN_13
    array_0.raw_rdata.en <= _GEN_12
    array_1.raw_rdata.en <= _GEN_12
    array_0.raw_rdata.clk <= _GEN_14
    array_1.raw_rdata.clk <= _GEN_14
    array_0.MPORT.addr <= _GEN_4
    array_1.MPORT.addr <= _GEN_4
    array_0.MPORT.en <= _GEN_6
    array_1.MPORT.en <= _GEN_6
    array_0.MPORT.clk <= _GEN_5
    array_1.MPORT.clk <= _GEN_5
    array_0.MPORT.data <= _GEN_9
    array_1.MPORT.data <= _GEN_10
    array_0.MPORT.mask <= _GEN_7
    array_1.MPORT.mask <= _GEN_8
    bypass_wdata_lfsr <= mux(reset, UInt<64>("h1234567887654321"), _GEN_15) @[LFSR64.scala 20:{23,23}]
    bypass_wdata_lfsr_1 <= mux(reset, UInt<64>("h1234567887654321"), _GEN_16) @[LFSR64.scala 20:{23,23}]
    bypass_mask_need_check <= _bypass_mask_need_check_T @[SRAMTemplate.scala 121:29]
    bypass_mask_waddr_reg <= io_w_req_bits_setIdx @[SRAMTemplate.scala 122:28]
    bypass_mask_raddr_reg <= io_r_req_bits_setIdx @[SRAMTemplate.scala 123:28]
    bypass_mask_bypass_REG <= io_w_req_bits_waymask @[SRAMTemplate.scala 125:76]
    rdata_REG <= io_r_req_valid @[SRAMTemplate.scala 139:59]
    rdata_r_0 <= mux(reset, _rdata_WIRE_0, _GEN_17) @[Reg.scala 28:{20,20}]
    rdata_r_1 <= mux(reset, _rdata_WIRE_1, _GEN_18) @[Reg.scala 28:{20,20}]

  module Arbiter :
    input clock : Clock
    input reset : UInt<1>
    output io_in_0_ready : UInt<1>
    input io_in_0_valid : UInt<1>
    input io_in_0_bits_setIdx : UInt<2>
    output io_in_1_ready : UInt<1>
    input io_in_1_valid : UInt<1>
    input io_in_1_bits_setIdx : UInt<2>
    output io_in_2_ready : UInt<1>
    input io_in_2_valid : UInt<1>
    input io_in_2_bits_setIdx : UInt<2>
    input io_out_ready : UInt<1>
    output io_out_valid : UInt<1>
    output io_out_bits_setIdx : UInt<2>
    output io_chosen : UInt<2>

    node _GEN_0 = mux(io_in_1_valid, UInt<1>("h1"), UInt<2>("h2")) @[Arbiter.scala 138:13 141:26 142:17]
    node _GEN_1 = mux(io_in_1_valid, io_in_1_bits_setIdx, io_in_2_bits_setIdx) @[Arbiter.scala 139:15 141:26 143:19]
    node _GEN_2 = mux(io_in_0_valid, UInt<1>("h0"), _GEN_0) @[Arbiter.scala 141:26 142:17]
    node _GEN_3 = mux(io_in_0_valid, io_in_0_bits_setIdx, _GEN_1) @[Arbiter.scala 141:26 143:19]
    node _grant_T = or(io_in_0_valid, io_in_1_valid) @[Arbiter.scala 46:68]
    node grant_1 = eq(io_in_0_valid, UInt<1>("h0")) @[Arbiter.scala 46:78]
    node grant_2 = eq(_grant_T, UInt<1>("h0")) @[Arbiter.scala 46:78]
    node _io_in_0_ready_T = and(UInt<1>("h1"), io_out_ready) @[Arbiter.scala 149:19]
    node _io_in_1_ready_T = and(grant_1, io_out_ready) @[Arbiter.scala 149:19]
    node _io_in_2_ready_T = and(grant_2, io_out_ready) @[Arbiter.scala 149:19]
    node _io_out_valid_T = eq(grant_2, UInt<1>("h0")) @[Arbiter.scala 150:19]
    node _io_out_valid_T_1 = or(_io_out_valid_T, io_in_2_valid) @[Arbiter.scala 150:31]
    io_in_0_ready <= _io_in_0_ready_T @[Arbiter.scala 149:14]
    io_in_1_ready <= _io_in_1_ready_T @[Arbiter.scala 149:14]
    io_in_2_ready <= _io_in_2_ready_T @[Arbiter.scala 149:14]
    io_out_valid <= _io_out_valid_T_1 @[Arbiter.scala 150:16]
    io_out_bits_setIdx <= _GEN_3
    io_chosen <= _GEN_2

  module SRAMTemplate_1 :
    input clock : Clock
    input reset : UInt<1>
    output io_r_req_ready : UInt<1>
    input io_r_req_valid : UInt<1>
    input io_r_req_bits_setIdx : UInt<2>
    output io_r_resp_data_0 : UInt<10>
    output io_r_resp_data_1 : UInt<10>
    output io_w_req_ready : UInt<1>
    input io_w_req_valid : UInt<1>
    input io_w_req_bits_setIdx : UInt<2>
    input io_w_req_bits_data_0 : UInt<10>
    input io_w_req_bits_data_1 : UInt<10>
    input io_w_req_bits_waymask : UInt<2>

    mem array_0 : @[SRAMTemplate.scala 96:26]
      data-type => UInt<10>
      depth => 4
      read-latency => 1
      write-latency => 1
      reader => raw_rdata
      writer => MPORT
      read-under-write => undefined
    mem array_1 : @[SRAMTemplate.scala 96:26]
      data-type => UInt<10>
      depth => 4
      read-latency => 1
      write-latency => 1
      reader => raw_rdata
      writer => MPORT
      read-under-write => undefined
    node resetState = UInt<1>("h0")
    node wen = or(io_w_req_valid, resetState) @[SRAMTemplate.scala 108:52]
    node resetSet = UInt<1>("h0")
    node setIdx = mux(resetState, resetSet, io_w_req_bits_setIdx) @[SRAMTemplate.scala 111:19]
    node _wdata_WIRE__0 = UInt<10>("h0") @[SRAMTemplate.scala 112:{51,51}]
    node _wdata_T_0 = mux(resetState, _wdata_WIRE__0, io_w_req_bits_data_0) @[SRAMTemplate.scala 112:26]
    node _wdata_WIRE__1 = UInt<10>("h0") @[SRAMTemplate.scala 112:{51,51}]
    node _wdata_T_1 = mux(resetState, _wdata_WIRE__1, io_w_req_bits_data_1) @[SRAMTemplate.scala 112:26]
    node _waymask_T = mux(UInt<1>("h1"), UInt<2>("h3"), UInt<2>("h0")) @[Bitwise.scala 74:12]
    node waymask = mux(resetState, _waymask_T, io_w_req_bits_waymask) @[SRAMTemplate.scala 113:20]
    node _T = bits(waymask, 0, 0) @[SRAMTemplate.scala 114:51]
    node _T_1 = bits(waymask, 1, 1) @[SRAMTemplate.scala 114:51]
    node _T_2 = or(setIdx, UInt<2>("h0"))
    node _T_3 = bits(_T_2, 1, 0)
    node _wdata_WIRE_1 = _wdata_T_0 @[SRAMTemplate.scala 112:{102,102}]
    node wdata_0 = _wdata_WIRE_1 @[SRAMTemplate.scala 112:{22,22}]
    node _GEN_0 = validif(_T, wdata_0)
    node _GEN_1 = mux(_T, UInt<1>("h1"), UInt<1>("h0"))
    node _wdata_WIRE_2 = _wdata_T_1 @[SRAMTemplate.scala 112:{102,102}]
    node wdata_1 = _wdata_WIRE_2 @[SRAMTemplate.scala 112:{22,22}]
    node _GEN_2 = validif(_T_1, wdata_1)
    node _GEN_3 = mux(_T_1, UInt<1>("h1"), UInt<1>("h0"))
    node _GEN_4 = validif(wen, _T_3) @[SRAMTemplate.scala 114:14]
    node _GEN_5 = validif(wen, clock) @[SRAMTemplate.scala 114:14]
    node _GEN_6 = mux(wen, UInt<1>("h1"), UInt<1>("h0")) @[SRAMTemplate.scala 114:14 96:26]
    node _GEN_7 = validif(wen, _GEN_1) @[SRAMTemplate.scala 114:14]
    node _GEN_8 = validif(wen, _GEN_3) @[SRAMTemplate.scala 114:14]
    node _GEN_9 = validif(wen, _GEN_0) @[SRAMTemplate.scala 114:14]
    node _GEN_10 = validif(wen, _GEN_2) @[SRAMTemplate.scala 114:14]
    node _GEN_11 = validif(io_r_req_valid, io_r_req_bits_setIdx) @[SRAMTemplate.scala 116:{29,29}]
    node _raw_rdata_WIRE = _GEN_11 @[SRAMTemplate.scala 116:29]
    node _raw_rdata_T = or(_raw_rdata_WIRE, UInt<2>("h0")) @[SRAMTemplate.scala 116:29]
    node _raw_rdata_T_1 = bits(_raw_rdata_T, 1, 0) @[SRAMTemplate.scala 116:29]
    node _GEN_12 = mux(io_r_req_valid, UInt<1>("h1"), UInt<1>("h0")) @[SRAMTemplate.scala 116:{29,29} 96:26]
    node _GEN_13 = validif(io_r_req_valid, _raw_rdata_T_1) @[SRAMTemplate.scala 116:{29,29}]
    node _GEN_14 = validif(io_r_req_valid, clock) @[SRAMTemplate.scala 116:{29,29}]
    reg bypass_wdata_lfsr : UInt<64>, clock with :
      reset => (UInt<1>("h0"), bypass_wdata_lfsr) @[LFSR64.scala 20:23]
    node _bypass_wdata_xor_T = bits(bypass_wdata_lfsr, 0, 0) @[LFSR64.scala 21:19]
    node _bypass_wdata_xor_T_1 = bits(bypass_wdata_lfsr, 1, 1) @[LFSR64.scala 21:29]
    node _bypass_wdata_xor_T_2 = xor(_bypass_wdata_xor_T, _bypass_wdata_xor_T_1) @[LFSR64.scala 21:23]
    node _bypass_wdata_xor_T_3 = bits(bypass_wdata_lfsr, 3, 3) @[LFSR64.scala 21:39]
    node _bypass_wdata_xor_T_4 = xor(_bypass_wdata_xor_T_2, _bypass_wdata_xor_T_3) @[LFSR64.scala 21:33]
    node _bypass_wdata_xor_T_5 = bits(bypass_wdata_lfsr, 4, 4) @[LFSR64.scala 21:49]
    node bypass_wdata_xor = xor(_bypass_wdata_xor_T_4, _bypass_wdata_xor_T_5) @[LFSR64.scala 21:43]
    node _bypass_wdata_lfsr_T = eq(bypass_wdata_lfsr, UInt<1>("h0")) @[LFSR64.scala 23:24]
    node _bypass_wdata_lfsr_T_1 = bits(bypass_wdata_lfsr, 63, 1) @[LFSR64.scala 23:51]
    node _bypass_wdata_lfsr_T_2 = cat(bypass_wdata_xor, _bypass_wdata_lfsr_T_1) @[Cat.scala 31:58]
    node _bypass_wdata_lfsr_T_3 = mux(_bypass_wdata_lfsr_T, UInt<1>("h1"), _bypass_wdata_lfsr_T_2) @[LFSR64.scala 23:18]
    node _GEN_15 = mux(UInt<1>("h1"), _bypass_wdata_lfsr_T_3, bypass_wdata_lfsr) @[LFSR64.scala 22:22 23:12 20:23]
    reg bypass_wdata_lfsr_1 : UInt<64>, clock with :
      reset => (UInt<1>("h0"), bypass_wdata_lfsr_1) @[LFSR64.scala 20:23]
    node _bypass_wdata_xor_T_6 = bits(bypass_wdata_lfsr_1, 0, 0) @[LFSR64.scala 21:19]
    node _bypass_wdata_xor_T_7 = bits(bypass_wdata_lfsr_1, 1, 1) @[LFSR64.scala 21:29]
    node _bypass_wdata_xor_T_8 = xor(_bypass_wdata_xor_T_6, _bypass_wdata_xor_T_7) @[LFSR64.scala 21:23]
    node _bypass_wdata_xor_T_9 = bits(bypass_wdata_lfsr_1, 3, 3) @[LFSR64.scala 21:39]
    node _bypass_wdata_xor_T_10 = xor(_bypass_wdata_xor_T_8, _bypass_wdata_xor_T_9) @[LFSR64.scala 21:33]
    node _bypass_wdata_xor_T_11 = bits(bypass_wdata_lfsr_1, 4, 4) @[LFSR64.scala 21:49]
    node bypass_wdata_xor_1 = xor(_bypass_wdata_xor_T_10, _bypass_wdata_xor_T_11) @[LFSR64.scala 21:43]
    node _bypass_wdata_lfsr_T_4 = eq(bypass_wdata_lfsr_1, UInt<1>("h0")) @[LFSR64.scala 23:24]
    node _bypass_wdata_lfsr_T_5 = bits(bypass_wdata_lfsr_1, 63, 1) @[LFSR64.scala 23:51]
    node _bypass_wdata_lfsr_T_6 = cat(bypass_wdata_xor_1, _bypass_wdata_lfsr_T_5) @[Cat.scala 31:58]
    node _bypass_wdata_lfsr_T_7 = mux(_bypass_wdata_lfsr_T_4, UInt<1>("h1"), _bypass_wdata_lfsr_T_6) @[LFSR64.scala 23:18]
    node _GEN_16 = mux(UInt<1>("h1"), _bypass_wdata_lfsr_T_7, bypass_wdata_lfsr_1) @[LFSR64.scala 22:22 23:12 20:23]
    node _bypass_mask_need_check_T = and(io_r_req_valid, io_w_req_valid) @[SRAMTemplate.scala 121:34]
    reg bypass_mask_need_check : UInt<1>, clock with :
      reset => (UInt<1>("h0"), bypass_mask_need_check) @[SRAMTemplate.scala 121:29]
    reg bypass_mask_waddr_reg : UInt<2>, clock with :
      reset => (UInt<1>("h0"), bypass_mask_waddr_reg) @[SRAMTemplate.scala 122:28]
    reg bypass_mask_raddr_reg : UInt<2>, clock with :
      reset => (UInt<1>("h0"), bypass_mask_raddr_reg) @[SRAMTemplate.scala 123:28]
    node _bypass_mask_bypass_T = eq(bypass_mask_waddr_reg, bypass_mask_raddr_reg) @[SRAMTemplate.scala 125:52]
    node _bypass_mask_bypass_T_1 = and(bypass_mask_need_check, _bypass_mask_bypass_T) @[SRAMTemplate.scala 125:39]
    node _bypass_mask_bypass_T_2 = bits(_bypass_mask_bypass_T_1, 0, 0) @[Bitwise.scala 74:15]
    node _bypass_mask_bypass_T_3 = mux(_bypass_mask_bypass_T_2, UInt<2>("h3"), UInt<2>("h0")) @[Bitwise.scala 74:12]
    reg bypass_mask_bypass_REG : UInt<2>, clock with :
      reset => (UInt<1>("h0"), bypass_mask_bypass_REG) @[SRAMTemplate.scala 125:76]
    node bypass_mask_bypass = and(_bypass_mask_bypass_T_3, bypass_mask_bypass_REG) @[SRAMTemplate.scala 125:67]
    node bypass_mask = bypass_mask_bypass @[SRAMTemplate.scala 126:{20,20}]
    node _mem_rdata_T = bits(bypass_mask, 0, 0) @[SRAMTemplate.scala 133:30]
    node _mem_rdata_T_1 = bits(bypass_mask, 1, 1) @[SRAMTemplate.scala 133:30]
    node _bypass_wdata_WIRE = bits(bypass_wdata_lfsr, 9, 0) @[SRAMTemplate.scala 129:{58,58}]
    node bypass_wdata_0 = _bypass_wdata_WIRE @[SRAMTemplate.scala 129:{17,17}]
    node _mem_rdata_T_2 = mux(_mem_rdata_T, bypass_wdata_0, array_0.raw_rdata.data) @[SRAMTemplate.scala 134:30]
    node _bypass_wdata_WIRE_1 = bits(bypass_wdata_lfsr_1, 9, 0) @[SRAMTemplate.scala 129:{58,58}]
    node bypass_wdata_1 = _bypass_wdata_WIRE_1 @[SRAMTemplate.scala 129:{17,17}]
    node _mem_rdata_T_3 = mux(_mem_rdata_T_1, bypass_wdata_1, array_1.raw_rdata.data) @[SRAMTemplate.scala 134:30]
    reg rdata_REG : UInt<1>, clock with :
      reset => (UInt<1>("h0"), rdata_REG) @[SRAMTemplate.scala 139:59]
    reg rdata_r_0 : UInt<10>, clock with :
      reset => (UInt<1>("h0"), rdata_r_0) @[Reg.scala 28:20]
    reg rdata_r_1 : UInt<10>, clock with :
      reset => (UInt<1>("h0"), rdata_r_1) @[Reg.scala 28:20]
    node mem_rdata_0 = _mem_rdata_T_2 @[SRAMTemplate.scala 133:{17,17}]
    node _GEN_17 = mux(rdata_REG, mem_rdata_0, rdata_r_0) @[Reg.scala 29:18 28:20 29:22]
    node mem_rdata_1 = _mem_rdata_T_3 @[SRAMTemplate.scala 133:{17,17}]
    node _GEN_18 = mux(rdata_REG, mem_rdata_1, rdata_r_1) @[Reg.scala 29:18 28:20 29:22]
    node _rdata_T_0 = mux(rdata_REG, mem_rdata_0, rdata_r_0) @[Hold.scala 18:48]
    node _rdata_T_1 = mux(rdata_REG, mem_rdata_1, rdata_r_1) @[Hold.scala 18:48]
    node _io_r_req_ready_T = eq(resetState, UInt<1>("h0")) @[SRAMTemplate.scala 143:21]
    node _io_r_req_ready_T_1 = and(_io_r_req_ready_T, UInt<1>("h1")) @[SRAMTemplate.scala 143:33]
    node _rdata_WIRE_0 = UInt<10>("h0") @[Hold.scala 18:{81,81}]
    node _rdata_WIRE_1 = UInt<10>("h0") @[Hold.scala 18:{81,81}]
    node rdata_0 = _rdata_T_0 @[SRAMTemplate.scala 140:{45,45}]
    node rdata_1 = _rdata_T_1 @[SRAMTemplate.scala 140:{45,45}]
    node _WIRE_0 = rdata_0 @[SRAMTemplate.scala 142:{28,28}]
    node _WIRE_1 = rdata_1 @[SRAMTemplate.scala 142:{28,28}]
    io_r_req_ready <= _io_r_req_ready_T_1 @[SRAMTemplate.scala 143:18]
    io_r_resp_data_0 <= _WIRE_0 @[SRAMTemplate.scala 142:18]
    io_r_resp_data_1 <= _WIRE_1 @[SRAMTemplate.scala 142:18]
    io_w_req_ready <= UInt<1>("h1") @[SRAMTemplate.scala 144:18]
    array_0.raw_rdata.addr <= _GEN_13
    array_1.raw_rdata.addr <= _GEN_13
    array_0.raw_rdata.en <= _GEN_12
    array_1.raw_rdata.en <= _GEN_12
    array_0.raw_rdata.clk <= _GEN_14
    array_1.raw_rdata.clk <= _GEN_14
    array_0.MPORT.addr <= _GEN_4
    array_1.MPORT.addr <= _GEN_4
    array_0.MPORT.en <= _GEN_6
    array_1.MPORT.en <= _GEN_6
    array_0.MPORT.clk <= _GEN_5
    array_1.MPORT.clk <= _GEN_5
    array_0.MPORT.data <= _GEN_9
    array_1.MPORT.data <= _GEN_10
    array_0.MPORT.mask <= _GEN_7
    array_1.MPORT.mask <= _GEN_8
    bypass_wdata_lfsr <= mux(reset, UInt<64>("h1234567887654321"), _GEN_15) @[LFSR64.scala 20:{23,23}]
    bypass_wdata_lfsr_1 <= mux(reset, UInt<64>("h1234567887654321"), _GEN_16) @[LFSR64.scala 20:{23,23}]
    bypass_mask_need_check <= _bypass_mask_need_check_T @[SRAMTemplate.scala 121:29]
    bypass_mask_waddr_reg <= io_w_req_bits_setIdx @[SRAMTemplate.scala 122:28]
    bypass_mask_raddr_reg <= io_r_req_bits_setIdx @[SRAMTemplate.scala 123:28]
    bypass_mask_bypass_REG <= io_w_req_bits_waymask @[SRAMTemplate.scala 125:76]
    rdata_REG <= io_r_req_valid @[SRAMTemplate.scala 139:59]
    rdata_r_0 <= mux(reset, _rdata_WIRE_0, _GEN_17) @[Reg.scala 28:{20,20}]
    rdata_r_1 <= mux(reset, _rdata_WIRE_1, _GEN_18) @[Reg.scala 28:{20,20}]

  module Arbiter_1 :
    input clock : Clock
    input reset : UInt<1>
    output io_in_0_ready : UInt<1>
    input io_in_0_valid : UInt<1>
    input io_in_0_bits_setIdx : UInt<2>
    input io_in_0_bits_data_0 : UInt<10>
    input io_in_0_bits_data_1 : UInt<10>
    input io_in_0_bits_waymask : UInt<2>
    output io_in_1_ready : UInt<1>
    input io_in_1_valid : UInt<1>
    input io_in_1_bits_setIdx : UInt<2>
    input io_in_1_bits_data_0 : UInt<10>
    input io_in_1_bits_data_1 : UInt<10>
    input io_in_1_bits_waymask : UInt<2>
    input io_out_ready : UInt<1>
    output io_out_valid : UInt<1>
    output io_out_bits_setIdx : UInt<2>
    output io_out_bits_data_0 : UInt<10>
    output io_out_bits_data_1 : UInt<10>
    output io_out_bits_waymask : UInt<2>
    output io_chosen : UInt<1>

    node _GEN_0 = mux(io_in_0_valid, UInt<1>("h0"), UInt<1>("h1")) @[Arbiter.scala 138:13 141:26 142:17]
    node _GEN_1 = mux(io_in_0_valid, io_in_0_bits_setIdx, io_in_1_bits_setIdx) @[Arbiter.scala 139:15 141:26 143:19]
    node _GEN_2 = mux(io_in_0_valid, io_in_0_bits_data_0, io_in_1_bits_data_0) @[Arbiter.scala 139:15 141:26 143:19]
    node _GEN_3 = mux(io_in_0_valid, io_in_0_bits_data_1, io_in_1_bits_data_1) @[Arbiter.scala 139:15 141:26 143:19]
    node _GEN_4 = mux(io_in_0_valid, io_in_0_bits_waymask, io_in_1_bits_waymask) @[Arbiter.scala 139:15 141:26 143:19]
    node grant_1 = eq(io_in_0_valid, UInt<1>("h0")) @[Arbiter.scala 46:78]
    node _io_in_0_ready_T = and(UInt<1>("h1"), io_out_ready) @[Arbiter.scala 149:19]
    node _io_in_1_ready_T = and(grant_1, io_out_ready) @[Arbiter.scala 149:19]
    node _io_out_valid_T = eq(grant_1, UInt<1>("h0")) @[Arbiter.scala 150:19]
    node _io_out_valid_T_1 = or(_io_out_valid_T, io_in_1_valid) @[Arbiter.scala 150:31]
    io_in_0_ready <= _io_in_0_ready_T @[Arbiter.scala 149:14]
    io_in_1_ready <= _io_in_1_ready_T @[Arbiter.scala 149:14]
    io_out_valid <= _io_out_valid_T_1 @[Arbiter.scala 150:16]
    io_out_bits_setIdx <= _GEN_1
    io_out_bits_data_0 <= _GEN_2
    io_out_bits_data_1 <= _GEN_3
    io_out_bits_waymask <= _GEN_4
    io_chosen <= _GEN_0

  module tagChecker :
    input clock : Clock
    input reset : UInt<1>
    input io_tag_of_set_0 : UInt<26>
    input io_tag_of_set_1 : UInt<26>
    input io_tag_from_pipe : UInt<26>
    input io_way_valid_0 : UInt<1>
    input io_way_valid_1 : UInt<1>
    output io_waymask : UInt<2>
    output io_cache_hit : UInt<1>

    node _io_waymask_T = eq(io_tag_of_set_0, io_tag_from_pipe) @[L1TagAccess.scala 248:90]
    node _io_waymask_T_1 = and(_io_waymask_T, io_way_valid_0) @[L1TagAccess.scala 248:112]
    node _io_waymask_T_2 = eq(io_tag_of_set_1, io_tag_from_pipe) @[L1TagAccess.scala 248:90]
    node _io_waymask_T_3 = and(_io_waymask_T_2, io_way_valid_1) @[L1TagAccess.scala 248:112]
    node _io_waymask_T_4 = cat(_io_waymask_T_1, _io_waymask_T_3) @[Cat.scala 31:58]
    node _io_waymask_T_5 = bits(_io_waymask_T_4, 0, 0) @[Bitwise.scala 111:18]
    node _io_waymask_T_6 = bits(_io_waymask_T_4, 1, 1) @[Bitwise.scala 111:47]
    node _io_waymask_T_7 = cat(_io_waymask_T_5, _io_waymask_T_6) @[Cat.scala 31:58]
    node _T = bits(io_waymask, 0, 0) @[Bitwise.scala 50:65]
    node _T_1 = bits(io_waymask, 1, 1) @[Bitwise.scala 50:65]
    node _T_2 = add(_T, _T_1) @[Bitwise.scala 48:55]
    node _T_3 = bits(_T_2, 1, 0) @[Bitwise.scala 48:55]
    node _T_4 = leq(_T_3, UInt<1>("h1")) @[L1TagAccess.scala 250:31]
    node _T_5 = asUInt(reset) @[L1TagAccess.scala 250:9]
    node _T_6 = eq(_T_5, UInt<1>("h0")) @[L1TagAccess.scala 250:9]
    node _T_7 = eq(_T_4, UInt<1>("h0")) @[L1TagAccess.scala 250:9]
    node _io_cache_hit_T = orr(io_waymask) @[L1TagAccess.scala 251:30]
    io_waymask <= _io_waymask_T_7 @[L1TagAccess.scala 248:14]
    io_cache_hit <= _io_cache_hit_T @[L1TagAccess.scala 251:16]
    printf(clock, and(and(and(UInt<1>("h1"), _T_6), _T_7), UInt<1>("h1")), "Assertion failed\n    at L1TagAccess.scala:250 assert(PopCount(io.waymask) <= 1.U)//if waymask not one-hot, duplicate tags in one set, error\n") : printf @[L1TagAccess.scala 250:9]
    assert(clock, _T_4, and(and(UInt<1>("h1"), _T_6), UInt<1>("h1")), "") : assert @[L1TagAccess.scala 250:9]

  module minIdxTree :
    input clock : Clock
    input reset : UInt<1>
    input io_candidateIn_0 : UInt<10>
    input io_candidateIn_1 : UInt<10>
    output io_idxOfMin : UInt<1>

    node candVec_0_candidate = io_candidateIn_1 @[L1TagAccess.scala 266:21 268:26]
    node candVec_1_candidate = io_candidateIn_0 @[L1TagAccess.scala 266:21 268:26]
    node _io_idxOfMin_T = lt(candVec_0_candidate, candVec_1_candidate) @[L1TagAccess.scala 264:79]
    node _io_idxOfMin_T_1_candidate = mux(_io_idxOfMin_T, candVec_0_candidate, candVec_1_candidate) @[L1TagAccess.scala 264:66]
    node candVec_0_index = UInt<1>("h0") @[L1TagAccess.scala 266:21 269:22]
    node candVec_1_index = UInt<1>("h1") @[L1TagAccess.scala 266:21 269:22]
    node _io_idxOfMin_T_1_index = mux(_io_idxOfMin_T, candVec_0_index, candVec_1_index) @[L1TagAccess.scala 264:66]
    io_idxOfMin <= _io_idxOfMin_T_1_index @[L1TagAccess.scala 272:15]

  module ReplacementUnit :
    input clock : Clock
    input reset : UInt<1>
    input io_validOfSet : UInt<2>
    input io_timeOfSet_st1_0 : UInt<10>
    input io_timeOfSet_st1_1 : UInt<10>
    output io_waymask_st1 : UInt<2>
    output io_Set_is_full : UInt<1>

    inst minTimeChooser of minIdxTree @[L1TagAccess.scala 216:32]
    node _io_Set_is_full_T = mux(UInt<1>("h1"), UInt<2>("h3"), UInt<2>("h0")) @[Bitwise.scala 74:12]
    node _io_Set_is_full_T_1 = eq(io_validOfSet, _io_Set_is_full_T) @[L1TagAccess.scala 210:35]
    node _timeOfSetAfterValid_0_T = bits(io_validOfSet, 0, 0) @[L1TagAccess.scala 215:50]
    node _timeOfSetAfterValid_0_T_1 = mux(_timeOfSetAfterValid_0_T, io_timeOfSet_st1_0, UInt<1>("h0")) @[L1TagAccess.scala 215:36]
    node _timeOfSetAfterValid_1_T = bits(io_validOfSet, 1, 1) @[L1TagAccess.scala 215:50]
    node _timeOfSetAfterValid_1_T_1 = mux(_timeOfSetAfterValid_1_T, io_timeOfSet_st1_1, UInt<1>("h0")) @[L1TagAccess.scala 215:36]
    node _io_waymask_st1_T = not(io_validOfSet) @[L1TagAccess.scala 221:77]
    node _io_waymask_st1_T_1 = bits(_io_waymask_st1_T, 0, 0) @[OneHot.scala 47:45]
    node _io_waymask_st1_T_2 = bits(_io_waymask_st1_T, 1, 1) @[OneHot.scala 47:45]
    node _io_waymask_st1_T_3 = mux(_io_waymask_st1_T_1, UInt<1>("h0"), UInt<1>("h1")) @[Mux.scala 47:70]
    node victimIdx = minTimeChooser.io_idxOfMin @[L1TagAccess.scala 209:34 218:15]
    node _io_waymask_st1_T_4 = mux(io_Set_is_full, victimIdx, _io_waymask_st1_T_3) @[L1TagAccess.scala 221:33]
    node _io_waymask_st1_T_5 = dshl(UInt<1>("h1"), _io_waymask_st1_T_4) @[OneHot.scala 57:35]
    node timeOfSetAfterValid_0 = _timeOfSetAfterValid_0_T_1 @[L1TagAccess.scala 213:35 215:30]
    node timeOfSetAfterValid_1 = _timeOfSetAfterValid_1_T_1 @[L1TagAccess.scala 213:35 215:30]
    io_waymask_st1 <= _io_waymask_st1_T_5 @[L1TagAccess.scala 221:18]
    io_Set_is_full <= _io_Set_is_full_T_1 @[L1TagAccess.scala 210:18]
    minTimeChooser.clock <= clock
    minTimeChooser.reset <= reset
    minTimeChooser.io_candidateIn_0 <= timeOfSetAfterValid_0 @[L1TagAccess.scala 217:35]
    minTimeChooser.io_candidateIn_1 <= timeOfSetAfterValid_1 @[L1TagAccess.scala 217:35]

  module L1TagAccess :
    input clock : Clock
    input reset : UInt<1>
    output io_probeRead_ready : UInt<1>
    input io_probeRead_valid : UInt<1>
    input io_probeRead_bits_setIdx : UInt<2>
    input io_tagFromCore_st1 : UInt<26>
    input io_probeIsWrite_st1 : UInt<1>
    output io_hit_st1 : UInt<1>
    output io_waymaskHit_st1 : UInt<2>
    input io_allocateWrite_valid : UInt<1>
    input io_allocateWrite_bits_setIdx : UInt<2>
    input io_allocateWriteData_st1 : UInt<26>
    input io_allocateWriteTagSRAMWValid_st1 : UInt<1>
    output io_needReplace : UInt<1>
    output io_waymaskReplacement_st1 : UInt<2>
    output io_a_addrReplacement_st1 : UInt<26>
    output io_hasDirty_st0 : UInt<1>
    output io_dirtySetIdx_st0 : UInt<2>
    output io_dirtyWayMask_st0 : UInt<2>
    output io_dirtyTag_st1 : UInt<26>
    input io_flushChoosen_valid : UInt<1>
    input io_flushChoosen_bits : UInt<4>
    input io_invalidateAll : UInt<1>

    inst tagBodyAccess of SRAMTemplate @[L1TagAccess.scala 65:29]
    inst tagAccessRArb of Arbiter @[L1TagAccess.scala 77:31]
    inst timeAccess of SRAMTemplate_1 @[L1TagAccess.scala 92:26]
    inst timeAccessWArb of Arbiter_1 @[L1TagAccess.scala 105:30]
    inst iTagChecker of tagChecker @[L1TagAccess.scala 130:27]
    inst Replacement of ReplacementUnit @[L1TagAccess.scala 146:27]
    node _T = and(io_probeRead_ready, io_probeRead_valid) @[Decoupled.scala 50:35]
    node _T_1 = and(_T, io_allocateWrite_valid) @[L1TagAccess.scala 54:30]
    node _T_2 = eq(_T_1, UInt<1>("h0")) @[L1TagAccess.scala 54:10]
    node _T_3 = asUInt(reset) @[L1TagAccess.scala 54:9]
    node _T_4 = eq(_T_3, UInt<1>("h0")) @[L1TagAccess.scala 54:9]
    node _T_5 = eq(_T_2, UInt<1>("h0")) @[L1TagAccess.scala 54:9]
    node _accessFire_T = and(io_probeRead_ready, io_probeRead_valid) @[Decoupled.scala 50:35]
    node accessFire = or(_accessFire_T, io_allocateWrite_valid) @[L1TagAccess.scala 57:38]
    reg accessCount : UInt<10>, clock with :
      reset => (UInt<1>("h0"), accessCount) @[Counter.scala 62:40]
    node wrap_wrap = eq(accessCount, UInt<10>("h3e7")) @[Counter.scala 74:24]
    node _wrap_value_T = add(accessCount, UInt<1>("h1")) @[Counter.scala 78:24]
    node _wrap_value_T_1 = tail(_wrap_value_T, 1) @[Counter.scala 78:24]
    node _GEN_0 = mux(wrap_wrap, UInt<1>("h0"), _wrap_value_T_1) @[Counter.scala 78:15 88:{20,28}]
    node _GEN_1 = mux(accessFire, _GEN_0, accessCount) @[Counter.scala 120:16 62:40]
    node _GEN_2 = mux(accessFire, wrap_wrap, UInt<1>("h0")) @[Counter.scala 120:{16,23}]
    node _tagAccessRArb_io_in_2_valid_T = eq(io_probeRead_valid, UInt<1>("h0")) @[L1TagAccess.scala 85:37]
    node _tagAccessRArb_io_in_2_valid_T_1 = eq(io_allocateWrite_valid, UInt<1>("h0")) @[L1TagAccess.scala 85:60]
    node _tagAccessRArb_io_in_2_valid_T_2 = and(_tagAccessRArb_io_in_2_valid_T, _tagAccessRArb_io_in_2_valid_T_1) @[L1TagAccess.scala 85:57]
    node _T_6 = and(timeAccessWArb.io_in_0_valid, timeAccessWArb.io_in_1_valid) @[L1TagAccess.scala 106:42]
    node _T_7 = eq(_T_6, UInt<1>("h0")) @[L1TagAccess.scala 106:10]
    node _T_8 = asUInt(reset) @[L1TagAccess.scala 106:9]
    node _T_9 = eq(_T_8, UInt<1>("h0")) @[L1TagAccess.scala 106:9]
    node _T_10 = eq(_T_7, UInt<1>("h0")) @[L1TagAccess.scala 106:9]
    reg REG : UInt<2>, clock with :
      reset => (UInt<1>("h0"), REG) @[L1TagAccess.scala 112:21]
    reg timeAccessWArb_io_in_1_valid_REG : UInt<1>, clock with :
      reset => (UInt<1>("h0"), timeAccessWArb_io_in_1_valid_REG) @[L1TagAccess.scala 116:43]
    reg REG_1 : UInt<2>, clock with :
      reset => (UInt<1>("h0"), REG_1) @[L1TagAccess.scala 119:21]
    reg way_valid_0_0 : UInt<1>, clock with :
      reset => (UInt<1>("h0"), way_valid_0_0) @[L1TagAccess.scala 124:26]
    reg way_valid_0_1 : UInt<1>, clock with :
      reset => (UInt<1>("h0"), way_valid_0_1) @[L1TagAccess.scala 124:26]
    reg way_valid_1_0 : UInt<1>, clock with :
      reset => (UInt<1>("h0"), way_valid_1_0) @[L1TagAccess.scala 124:26]
    reg way_valid_1_1 : UInt<1>, clock with :
      reset => (UInt<1>("h0"), way_valid_1_1) @[L1TagAccess.scala 124:26]
    reg way_valid_2_0 : UInt<1>, clock with :
      reset => (UInt<1>("h0"), way_valid_2_0) @[L1TagAccess.scala 124:26]
    reg way_valid_2_1 : UInt<1>, clock with :
      reset => (UInt<1>("h0"), way_valid_2_1) @[L1TagAccess.scala 124:26]
    reg way_valid_3_0 : UInt<1>, clock with :
      reset => (UInt<1>("h0"), way_valid_3_0) @[L1TagAccess.scala 124:26]
    reg way_valid_3_1 : UInt<1>, clock with :
      reset => (UInt<1>("h0"), way_valid_3_1) @[L1TagAccess.scala 124:26]
    reg way_dirty_0_0 : UInt<1>, clock with :
      reset => (UInt<1>("h0"), way_dirty_0_0) @[L1TagAccess.scala 126:26]
    reg way_dirty_0_1 : UInt<1>, clock with :
      reset => (UInt<1>("h0"), way_dirty_0_1) @[L1TagAccess.scala 126:26]
    reg way_dirty_1_0 : UInt<1>, clock with :
      reset => (UInt<1>("h0"), way_dirty_1_0) @[L1TagAccess.scala 126:26]
    reg way_dirty_1_1 : UInt<1>, clock with :
      reset => (UInt<1>("h0"), way_dirty_1_1) @[L1TagAccess.scala 126:26]
    reg way_dirty_2_0 : UInt<1>, clock with :
      reset => (UInt<1>("h0"), way_dirty_2_0) @[L1TagAccess.scala 126:26]
    reg way_dirty_2_1 : UInt<1>, clock with :
      reset => (UInt<1>("h0"), way_dirty_2_1) @[L1TagAccess.scala 126:26]
    reg way_dirty_3_0 : UInt<1>, clock with :
      reset => (UInt<1>("h0"), way_dirty_3_0) @[L1TagAccess.scala 126:26]
    reg way_dirty_3_1 : UInt<1>, clock with :
      reset => (UInt<1>("h0"), way_dirty_3_1) @[L1TagAccess.scala 126:26]
    node _iTagChecker_io_way_valid_T = and(io_probeRead_ready, io_probeRead_valid) @[Decoupled.scala 50:35]
    reg iTagChecker_io_way_valid_r : UInt<2>, clock with :
      reset => (UInt<1>("h0"), iTagChecker_io_way_valid_r) @[Reg.scala 16:16]
    node _GEN_3 = mux(_iTagChecker_io_way_valid_T, io_probeRead_bits_setIdx, iTagChecker_io_way_valid_r) @[Reg.scala 16:16 17:{18,22}]
    node _GEN_4 = validif(eq(UInt<1>("h0"), iTagChecker_io_way_valid_r), way_valid_0_0) @[L1TagAccess.scala 133:{28,28}]
    node _GEN_5 = mux(eq(UInt<1>("h1"), iTagChecker_io_way_valid_r), way_valid_1_0, _GEN_4) @[L1TagAccess.scala 133:{28,28}]
    node _GEN_6 = mux(eq(UInt<2>("h2"), iTagChecker_io_way_valid_r), way_valid_2_0, _GEN_5) @[L1TagAccess.scala 133:{28,28}]
    node _GEN_7 = mux(eq(UInt<2>("h3"), iTagChecker_io_way_valid_r), way_valid_3_0, _GEN_6) @[L1TagAccess.scala 133:{28,28}]
    node _GEN_8 = validif(eq(UInt<1>("h0"), iTagChecker_io_way_valid_r), way_valid_0_1) @[L1TagAccess.scala 133:{28,28}]
    node _GEN_9 = mux(eq(UInt<1>("h1"), iTagChecker_io_way_valid_r), way_valid_1_1, _GEN_8) @[L1TagAccess.scala 133:{28,28}]
    node _GEN_10 = mux(eq(UInt<2>("h2"), iTagChecker_io_way_valid_r), way_valid_2_1, _GEN_9) @[L1TagAccess.scala 133:{28,28}]
    node _GEN_11 = mux(eq(UInt<2>("h3"), iTagChecker_io_way_valid_r), way_valid_3_1, _GEN_10) @[L1TagAccess.scala 133:{28,28}]
    node _T_11 = and(iTagChecker.io_cache_hit, io_probeIsWrite_st1) @[L1TagAccess.scala 137:39]
    node _T_12 = and(_T_11, io_flushChoosen_valid) @[L1TagAccess.scala 137:66]
    node _T_13 = eq(_T_12, UInt<1>("h0")) @[L1TagAccess.scala 137:12]
    node _T_14 = asUInt(reset) @[L1TagAccess.scala 137:11]
    node _T_15 = eq(_T_14, UInt<1>("h0")) @[L1TagAccess.scala 137:11]
    node _T_16 = eq(_T_13, UInt<1>("h0")) @[L1TagAccess.scala 137:11]
    node _T_17 = and(iTagChecker.io_cache_hit, io_probeIsWrite_st1) @[L1TagAccess.scala 138:35]
    reg REG_2 : UInt<2>, clock with :
      reset => (UInt<1>("h0"), REG_2) @[L1TagAccess.scala 139:24]
    node _T_18 = or(REG_2, UInt<2>("h0"))
    node _T_19 = bits(_T_18, 1, 0)
    node _T_20 = bits(iTagChecker.io_waymask, 0, 0)
    node _way_dirty_T_19_T_20 = UInt<1>("h1") @[L1TagAccess.scala 139:{76,76}]
    node _GEN_12 = mux(and(eq(UInt<1>("h0"), _T_19), eq(UInt<1>("h0"), _T_20)), _way_dirty_T_19_T_20, way_dirty_0_0) @[L1TagAccess.scala 126:26 139:{76,76}]
    node _GEN_13 = mux(and(eq(UInt<1>("h0"), _T_19), eq(UInt<1>("h1"), _T_20)), _way_dirty_T_19_T_20, way_dirty_0_1) @[L1TagAccess.scala 126:26 139:{76,76}]
    node _GEN_14 = mux(and(eq(UInt<1>("h1"), _T_19), eq(UInt<1>("h0"), _T_20)), _way_dirty_T_19_T_20, way_dirty_1_0) @[L1TagAccess.scala 126:26 139:{76,76}]
    node _GEN_15 = mux(and(eq(UInt<1>("h1"), _T_19), eq(UInt<1>("h1"), _T_20)), _way_dirty_T_19_T_20, way_dirty_1_1) @[L1TagAccess.scala 126:26 139:{76,76}]
    node _GEN_16 = mux(and(eq(UInt<2>("h2"), _T_19), eq(UInt<1>("h0"), _T_20)), _way_dirty_T_19_T_20, way_dirty_2_0) @[L1TagAccess.scala 126:26 139:{76,76}]
    node _GEN_17 = mux(and(eq(UInt<2>("h2"), _T_19), eq(UInt<1>("h1"), _T_20)), _way_dirty_T_19_T_20, way_dirty_2_1) @[L1TagAccess.scala 126:26 139:{76,76}]
    node _GEN_18 = mux(and(eq(UInt<2>("h3"), _T_19), eq(UInt<1>("h0"), _T_20)), _way_dirty_T_19_T_20, way_dirty_3_0) @[L1TagAccess.scala 126:26 139:{76,76}]
    node _GEN_19 = mux(and(eq(UInt<2>("h3"), _T_19), eq(UInt<1>("h1"), _T_20)), _way_dirty_T_19_T_20, way_dirty_3_1) @[L1TagAccess.scala 126:26 139:{76,76}]
    node _T_21 = bits(io_flushChoosen_bits, 3, 2) @[L1TagAccess.scala 141:41]
    node _T_22 = bits(io_flushChoosen_bits, 1, 0) @[L1TagAccess.scala 141:92]
    node _T_23 = bits(_T_22, 0, 0)
    node _way_dirty_T_21_T_23 = UInt<1>("h0") @[L1TagAccess.scala 141:{103,103}]
    node _GEN_20 = mux(and(eq(UInt<1>("h0"), _T_21), eq(UInt<1>("h0"), _T_23)), _way_dirty_T_21_T_23, way_dirty_0_0) @[L1TagAccess.scala 141:{103,103} 126:26]
    node _GEN_21 = mux(and(eq(UInt<1>("h0"), _T_21), eq(UInt<1>("h1"), _T_23)), _way_dirty_T_21_T_23, way_dirty_0_1) @[L1TagAccess.scala 141:{103,103} 126:26]
    node _GEN_22 = mux(and(eq(UInt<1>("h1"), _T_21), eq(UInt<1>("h0"), _T_23)), _way_dirty_T_21_T_23, way_dirty_1_0) @[L1TagAccess.scala 141:{103,103} 126:26]
    node _GEN_23 = mux(and(eq(UInt<1>("h1"), _T_21), eq(UInt<1>("h1"), _T_23)), _way_dirty_T_21_T_23, way_dirty_1_1) @[L1TagAccess.scala 141:{103,103} 126:26]
    node _GEN_24 = mux(and(eq(UInt<2>("h2"), _T_21), eq(UInt<1>("h0"), _T_23)), _way_dirty_T_21_T_23, way_dirty_2_0) @[L1TagAccess.scala 141:{103,103} 126:26]
    node _GEN_25 = mux(and(eq(UInt<2>("h2"), _T_21), eq(UInt<1>("h1"), _T_23)), _way_dirty_T_21_T_23, way_dirty_2_1) @[L1TagAccess.scala 141:{103,103} 126:26]
    node _GEN_26 = mux(and(eq(UInt<2>("h3"), _T_21), eq(UInt<1>("h0"), _T_23)), _way_dirty_T_21_T_23, way_dirty_3_0) @[L1TagAccess.scala 141:{103,103} 126:26]
    node _GEN_27 = mux(and(eq(UInt<2>("h3"), _T_21), eq(UInt<1>("h1"), _T_23)), _way_dirty_T_21_T_23, way_dirty_3_1) @[L1TagAccess.scala 141:{103,103} 126:26]
    node _GEN_28 = mux(io_flushChoosen_valid, _GEN_20, way_dirty_0_0) @[L1TagAccess.scala 126:26 140:42]
    node _GEN_29 = mux(io_flushChoosen_valid, _GEN_21, way_dirty_0_1) @[L1TagAccess.scala 126:26 140:42]
    node _GEN_30 = mux(io_flushChoosen_valid, _GEN_22, way_dirty_1_0) @[L1TagAccess.scala 126:26 140:42]
    node _GEN_31 = mux(io_flushChoosen_valid, _GEN_23, way_dirty_1_1) @[L1TagAccess.scala 126:26 140:42]
    node _GEN_32 = mux(io_flushChoosen_valid, _GEN_24, way_dirty_2_0) @[L1TagAccess.scala 126:26 140:42]
    node _GEN_33 = mux(io_flushChoosen_valid, _GEN_25, way_dirty_2_1) @[L1TagAccess.scala 126:26 140:42]
    node _GEN_34 = mux(io_flushChoosen_valid, _GEN_26, way_dirty_3_0) @[L1TagAccess.scala 126:26 140:42]
    node _GEN_35 = mux(io_flushChoosen_valid, _GEN_27, way_dirty_3_1) @[L1TagAccess.scala 126:26 140:42]
    node _GEN_36 = mux(_T_17, _GEN_12, _GEN_28) @[L1TagAccess.scala 138:62]
    node _GEN_37 = mux(_T_17, _GEN_13, _GEN_29) @[L1TagAccess.scala 138:62]
    node _GEN_38 = mux(_T_17, _GEN_14, _GEN_30) @[L1TagAccess.scala 138:62]
    node _GEN_39 = mux(_T_17, _GEN_15, _GEN_31) @[L1TagAccess.scala 138:62]
    node _GEN_40 = mux(_T_17, _GEN_16, _GEN_32) @[L1TagAccess.scala 138:62]
    node _GEN_41 = mux(_T_17, _GEN_17, _GEN_33) @[L1TagAccess.scala 138:62]
    node _GEN_42 = mux(_T_17, _GEN_18, _GEN_34) @[L1TagAccess.scala 138:62]
    node _GEN_43 = mux(_T_17, _GEN_19, _GEN_35) @[L1TagAccess.scala 138:62]
    reg allocateWrite_st1_setIdx : UInt<2>, clock with :
      reset => (UInt<1>("h0"), allocateWrite_st1_setIdx) @[Reg.scala 16:16]
    node _GEN_44 = mux(io_allocateWrite_valid, io_allocateWrite_bits_setIdx, allocateWrite_st1_setIdx) @[Reg.scala 16:16 17:{18,22}]
    node _io_needReplace_T = bits(Replacement.io_waymask_st1, 1, 1) @[CircuitMath.scala 30:8]
    node _GEN_45 = validif(and(eq(UInt<1>("h0"), allocateWrite_st1_setIdx), eq(UInt<1>("h0"), _io_needReplace_T)), way_dirty_0_0) @[L1TagAccess.scala 151:{101,101}]
    node _GEN_46 = mux(and(eq(UInt<1>("h0"), allocateWrite_st1_setIdx), eq(UInt<1>("h1"), _io_needReplace_T)), way_dirty_0_1, _GEN_45) @[L1TagAccess.scala 151:{101,101}]
    node _GEN_47 = mux(and(eq(UInt<1>("h1"), allocateWrite_st1_setIdx), eq(UInt<1>("h0"), _io_needReplace_T)), way_dirty_1_0, _GEN_46) @[L1TagAccess.scala 151:{101,101}]
    node _GEN_48 = mux(and(eq(UInt<1>("h1"), allocateWrite_st1_setIdx), eq(UInt<1>("h1"), _io_needReplace_T)), way_dirty_1_1, _GEN_47) @[L1TagAccess.scala 151:{101,101}]
    node _GEN_49 = mux(and(eq(UInt<2>("h2"), allocateWrite_st1_setIdx), eq(UInt<1>("h0"), _io_needReplace_T)), way_dirty_2_0, _GEN_48) @[L1TagAccess.scala 151:{101,101}]
    node _GEN_50 = mux(and(eq(UInt<2>("h2"), allocateWrite_st1_setIdx), eq(UInt<1>("h1"), _io_needReplace_T)), way_dirty_2_1, _GEN_49) @[L1TagAccess.scala 151:{101,101}]
    node _GEN_51 = mux(and(eq(UInt<2>("h3"), allocateWrite_st1_setIdx), eq(UInt<1>("h0"), _io_needReplace_T)), way_dirty_3_0, _GEN_50) @[L1TagAccess.scala 151:{101,101}]
    node _GEN_52 = mux(and(eq(UInt<2>("h3"), allocateWrite_st1_setIdx), eq(UInt<1>("h1"), _io_needReplace_T)), way_dirty_3_1, _GEN_51) @[L1TagAccess.scala 151:{101,101}]
    node _way_dirty_allocateWrite_st1_setIdx_io_needReplace_T = _GEN_52 @[L1TagAccess.scala 151:101]
    node _io_needReplace_T_1 = bits(_way_dirty_allocateWrite_st1_setIdx_io_needReplace_T, 0, 0) @[L1TagAccess.scala 151:101]
    node _GEN_53 = validif(eq(UInt<1>("h0"), io_allocateWrite_bits_setIdx), way_valid_0_0) @[Cat.scala 31:{58,58}]
    node _GEN_54 = mux(eq(UInt<1>("h1"), io_allocateWrite_bits_setIdx), way_valid_1_0, _GEN_53) @[Cat.scala 31:{58,58}]
    node _GEN_55 = mux(eq(UInt<2>("h2"), io_allocateWrite_bits_setIdx), way_valid_2_0, _GEN_54) @[Cat.scala 31:{58,58}]
    node _GEN_56 = mux(eq(UInt<2>("h3"), io_allocateWrite_bits_setIdx), way_valid_3_0, _GEN_55) @[Cat.scala 31:{58,58}]
    node _GEN_57 = validif(eq(UInt<1>("h0"), io_allocateWrite_bits_setIdx), way_valid_0_1) @[Cat.scala 31:{58,58}]
    node _GEN_58 = mux(eq(UInt<1>("h1"), io_allocateWrite_bits_setIdx), way_valid_1_1, _GEN_57) @[Cat.scala 31:{58,58}]
    node _GEN_59 = mux(eq(UInt<2>("h2"), io_allocateWrite_bits_setIdx), way_valid_2_1, _GEN_58) @[Cat.scala 31:{58,58}]
    node _GEN_60 = mux(eq(UInt<2>("h3"), io_allocateWrite_bits_setIdx), way_valid_3_1, _GEN_59) @[Cat.scala 31:{58,58}]
    node _way_valid_io_allocateWrite_bits_setIdx_0 = _GEN_56 @[Cat.scala 31:58]
    node _way_valid_io_allocateWrite_bits_setIdx_1 = _GEN_60 @[Cat.scala 31:58]
    node _Replacement_io_validOfSet_T = cat(_way_valid_io_allocateWrite_bits_setIdx_0, _way_valid_io_allocateWrite_bits_setIdx_1) @[Cat.scala 31:58]
    node _io_a_addrReplacement_st1_T = bits(Replacement.io_waymask_st1, 0, 0)
    node _GEN_61 = validif(eq(UInt<1>("h0"), _io_a_addrReplacement_st1_T), tagBodyAccess.io_r_resp_data_0) @[Cat.scala 31:{58,58}]
    node _GEN_62 = mux(eq(UInt<1>("h1"), _io_a_addrReplacement_st1_T), tagBodyAccess.io_r_resp_data_1, _GEN_61) @[Cat.scala 31:{58,58}]
    node _tagBodyAccess_io_r_resp_data_io_a_addrReplacement_st1_T = _GEN_62 @[Cat.scala 31:58]
    node _io_a_addrReplacement_st1_T_1 = cat(_tagBodyAccess_io_r_resp_data_io_a_addrReplacement_st1_T, allocateWrite_st1_setIdx) @[Cat.scala 31:58]
    node _io_a_addrReplacement_st1_T_2 = cat(_io_a_addrReplacement_st1_T_1, UInt<4>("h0")) @[Cat.scala 31:58]
    reg REG_3 : UInt<1>, clock with :
      reset => (UInt<1>("h0"), REG_3) @[L1TagAccess.scala 164:15]
    node _T_24 = eq(Replacement.io_Set_is_full, UInt<1>("h0")) @[L1TagAccess.scala 164:42]
    node _T_25 = and(REG_3, _T_24) @[L1TagAccess.scala 164:39]
    node _T_26 = bits(Replacement.io_waymask_st1, 1, 1) @[CircuitMath.scala 30:8]
    node _way_valid_allocateWrite_st1_setIdx_T_26 = UInt<1>("h1") @[L1TagAccess.scala 165:{79,79}]
    node _GEN_63 = mux(and(eq(UInt<1>("h0"), allocateWrite_st1_setIdx), eq(UInt<1>("h0"), _T_26)), _way_valid_allocateWrite_st1_setIdx_T_26, way_valid_0_0) @[L1TagAccess.scala 124:26 165:{79,79}]
    node _GEN_64 = mux(and(eq(UInt<1>("h0"), allocateWrite_st1_setIdx), eq(UInt<1>("h1"), _T_26)), _way_valid_allocateWrite_st1_setIdx_T_26, way_valid_0_1) @[L1TagAccess.scala 124:26 165:{79,79}]
    node _GEN_65 = mux(and(eq(UInt<1>("h1"), allocateWrite_st1_setIdx), eq(UInt<1>("h0"), _T_26)), _way_valid_allocateWrite_st1_setIdx_T_26, way_valid_1_0) @[L1TagAccess.scala 124:26 165:{79,79}]
    node _GEN_66 = mux(and(eq(UInt<1>("h1"), allocateWrite_st1_setIdx), eq(UInt<1>("h1"), _T_26)), _way_valid_allocateWrite_st1_setIdx_T_26, way_valid_1_1) @[L1TagAccess.scala 124:26 165:{79,79}]
    node _GEN_67 = mux(and(eq(UInt<2>("h2"), allocateWrite_st1_setIdx), eq(UInt<1>("h0"), _T_26)), _way_valid_allocateWrite_st1_setIdx_T_26, way_valid_2_0) @[L1TagAccess.scala 124:26 165:{79,79}]
    node _GEN_68 = mux(and(eq(UInt<2>("h2"), allocateWrite_st1_setIdx), eq(UInt<1>("h1"), _T_26)), _way_valid_allocateWrite_st1_setIdx_T_26, way_valid_2_1) @[L1TagAccess.scala 124:26 165:{79,79}]
    node _GEN_69 = mux(and(eq(UInt<2>("h3"), allocateWrite_st1_setIdx), eq(UInt<1>("h0"), _T_26)), _way_valid_allocateWrite_st1_setIdx_T_26, way_valid_3_0) @[L1TagAccess.scala 124:26 165:{79,79}]
    node _GEN_70 = mux(and(eq(UInt<2>("h3"), allocateWrite_st1_setIdx), eq(UInt<1>("h1"), _T_26)), _way_valid_allocateWrite_st1_setIdx_T_26, way_valid_3_1) @[L1TagAccess.scala 124:26 165:{79,79}]
    node _way_valid_WIRE_5_0 = UInt<1>("h0") @[L1TagAccess.scala 167:{47,47}]
    node _way_valid_WIRE_9_0_0 = _way_valid_WIRE_5_0 @[L1TagAccess.scala 167:{25,25}]
    node _GEN_71 = mux(io_invalidateAll, _way_valid_WIRE_9_0_0, way_valid_0_0) @[L1TagAccess.scala 166:31 167:15 124:26]
    node _way_valid_WIRE_5_1 = UInt<1>("h0") @[L1TagAccess.scala 167:{47,47}]
    node _way_valid_WIRE_9_0_1 = _way_valid_WIRE_5_1 @[L1TagAccess.scala 167:{25,25}]
    node _GEN_72 = mux(io_invalidateAll, _way_valid_WIRE_9_0_1, way_valid_0_1) @[L1TagAccess.scala 166:31 167:15 124:26]
    node _way_valid_WIRE_6_0 = UInt<1>("h0") @[L1TagAccess.scala 167:{47,47}]
    node _way_valid_WIRE_9_1_0 = _way_valid_WIRE_6_0 @[L1TagAccess.scala 167:{25,25}]
    node _GEN_73 = mux(io_invalidateAll, _way_valid_WIRE_9_1_0, way_valid_1_0) @[L1TagAccess.scala 166:31 167:15 124:26]
    node _way_valid_WIRE_6_1 = UInt<1>("h0") @[L1TagAccess.scala 167:{47,47}]
    node _way_valid_WIRE_9_1_1 = _way_valid_WIRE_6_1 @[L1TagAccess.scala 167:{25,25}]
    node _GEN_74 = mux(io_invalidateAll, _way_valid_WIRE_9_1_1, way_valid_1_1) @[L1TagAccess.scala 166:31 167:15 124:26]
    node _way_valid_WIRE_7_0 = UInt<1>("h0") @[L1TagAccess.scala 167:{47,47}]
    node _way_valid_WIRE_9_2_0 = _way_valid_WIRE_7_0 @[L1TagAccess.scala 167:{25,25}]
    node _GEN_75 = mux(io_invalidateAll, _way_valid_WIRE_9_2_0, way_valid_2_0) @[L1TagAccess.scala 166:31 167:15 124:26]
    node _way_valid_WIRE_7_1 = UInt<1>("h0") @[L1TagAccess.scala 167:{47,47}]
    node _way_valid_WIRE_9_2_1 = _way_valid_WIRE_7_1 @[L1TagAccess.scala 167:{25,25}]
    node _GEN_76 = mux(io_invalidateAll, _way_valid_WIRE_9_2_1, way_valid_2_1) @[L1TagAccess.scala 166:31 167:15 124:26]
    node _way_valid_WIRE_8_0 = UInt<1>("h0") @[L1TagAccess.scala 167:{47,47}]
    node _way_valid_WIRE_9_3_0 = _way_valid_WIRE_8_0 @[L1TagAccess.scala 167:{25,25}]
    node _GEN_77 = mux(io_invalidateAll, _way_valid_WIRE_9_3_0, way_valid_3_0) @[L1TagAccess.scala 166:31 167:15 124:26]
    node _way_valid_WIRE_8_1 = UInt<1>("h0") @[L1TagAccess.scala 167:{47,47}]
    node _way_valid_WIRE_9_3_1 = _way_valid_WIRE_8_1 @[L1TagAccess.scala 167:{25,25}]
    node _GEN_78 = mux(io_invalidateAll, _way_valid_WIRE_9_3_1, way_valid_3_1) @[L1TagAccess.scala 166:31 167:15 124:26]
    node _GEN_79 = mux(_T_25, _GEN_63, _GEN_71) @[L1TagAccess.scala 164:70]
    node _GEN_80 = mux(_T_25, _GEN_64, _GEN_72) @[L1TagAccess.scala 164:70]
    node _GEN_81 = mux(_T_25, _GEN_65, _GEN_73) @[L1TagAccess.scala 164:70]
    node _GEN_82 = mux(_T_25, _GEN_66, _GEN_74) @[L1TagAccess.scala 164:70]
    node _GEN_83 = mux(_T_25, _GEN_67, _GEN_75) @[L1TagAccess.scala 164:70]
    node _GEN_84 = mux(_T_25, _GEN_68, _GEN_76) @[L1TagAccess.scala 164:70]
    node _GEN_85 = mux(_T_25, _GEN_69, _GEN_77) @[L1TagAccess.scala 164:70]
    node _GEN_86 = mux(_T_25, _GEN_70, _GEN_78) @[L1TagAccess.scala 164:70]
    node _T_27 = and(io_allocateWrite_valid, io_invalidateAll) @[L1TagAccess.scala 169:35]
    node _T_28 = eq(_T_27, UInt<1>("h0")) @[L1TagAccess.scala 169:10]
    node _T_29 = asUInt(reset) @[L1TagAccess.scala 169:9]
    node _T_30 = eq(_T_29, UInt<1>("h0")) @[L1TagAccess.scala 169:9]
    node _T_31 = eq(_T_28, UInt<1>("h0")) @[L1TagAccess.scala 169:9]
    node _way_dirtyAfterValid_0_T = and(way_dirty_0_0, way_valid_0_0) @[L1TagAccess.scala 184:95]
    node _way_dirtyAfterValid_0_T_1 = and(way_dirty_0_1, way_valid_0_1) @[L1TagAccess.scala 184:95]
    node _way_dirtyAfterValid_0_WIRE_1 = _way_dirtyAfterValid_0_T_1 @[L1TagAccess.scala 184:{40,40}]
    node way_dirtyAfterValid_0_1 = _way_dirtyAfterValid_0_WIRE_1 @[L1TagAccess.scala 175:35 184:30]
    node _way_dirtyAfterValid_0_WIRE_0 = _way_dirtyAfterValid_0_T @[L1TagAccess.scala 184:{40,40}]
    node way_dirtyAfterValid_0_0 = _way_dirtyAfterValid_0_WIRE_0 @[L1TagAccess.scala 175:35 184:30]
    node _setDirty_0_T = cat(way_dirtyAfterValid_0_1, way_dirtyAfterValid_0_0) @[L1TagAccess.scala 185:45]
    node _setDirty_0_T_1 = orr(_setDirty_0_T) @[L1TagAccess.scala 185:52]
    node _way_dirtyAfterValid_1_T = and(way_dirty_1_0, way_valid_1_0) @[L1TagAccess.scala 184:95]
    node _way_dirtyAfterValid_1_T_1 = and(way_dirty_1_1, way_valid_1_1) @[L1TagAccess.scala 184:95]
    node _way_dirtyAfterValid_1_WIRE_1 = _way_dirtyAfterValid_1_T_1 @[L1TagAccess.scala 184:{40,40}]
    node way_dirtyAfterValid_1_1 = _way_dirtyAfterValid_1_WIRE_1 @[L1TagAccess.scala 175:35 184:30]
    node _way_dirtyAfterValid_1_WIRE_0 = _way_dirtyAfterValid_1_T @[L1TagAccess.scala 184:{40,40}]
    node way_dirtyAfterValid_1_0 = _way_dirtyAfterValid_1_WIRE_0 @[L1TagAccess.scala 175:35 184:30]
    node _setDirty_1_T = cat(way_dirtyAfterValid_1_1, way_dirtyAfterValid_1_0) @[L1TagAccess.scala 185:45]
    node _setDirty_1_T_1 = orr(_setDirty_1_T) @[L1TagAccess.scala 185:52]
    node _way_dirtyAfterValid_2_T = and(way_dirty_2_0, way_valid_2_0) @[L1TagAccess.scala 184:95]
    node _way_dirtyAfterValid_2_T_1 = and(way_dirty_2_1, way_valid_2_1) @[L1TagAccess.scala 184:95]
    node _way_dirtyAfterValid_2_WIRE_1 = _way_dirtyAfterValid_2_T_1 @[L1TagAccess.scala 184:{40,40}]
    node way_dirtyAfterValid_2_1 = _way_dirtyAfterValid_2_WIRE_1 @[L1TagAccess.scala 175:35 184:30]
    node _way_dirtyAfterValid_2_WIRE_0 = _way_dirtyAfterValid_2_T @[L1TagAccess.scala 184:{40,40}]
    node way_dirtyAfterValid_2_0 = _way_dirtyAfterValid_2_WIRE_0 @[L1TagAccess.scala 175:35 184:30]
    node _setDirty_2_T = cat(way_dirtyAfterValid_2_1, way_dirtyAfterValid_2_0) @[L1TagAccess.scala 185:45]
    node _setDirty_2_T_1 = orr(_setDirty_2_T) @[L1TagAccess.scala 185:52]
    node _way_dirtyAfterValid_3_T = and(way_dirty_3_0, way_valid_3_0) @[L1TagAccess.scala 184:95]
    node _way_dirtyAfterValid_3_T_1 = and(way_dirty_3_1, way_valid_3_1) @[L1TagAccess.scala 184:95]
    node _way_dirtyAfterValid_3_WIRE_1 = _way_dirtyAfterValid_3_T_1 @[L1TagAccess.scala 184:{40,40}]
    node way_dirtyAfterValid_3_1 = _way_dirtyAfterValid_3_WIRE_1 @[L1TagAccess.scala 175:35 184:30]
    node _way_dirtyAfterValid_3_WIRE_0 = _way_dirtyAfterValid_3_T @[L1TagAccess.scala 184:{40,40}]
    node way_dirtyAfterValid_3_0 = _way_dirtyAfterValid_3_WIRE_0 @[L1TagAccess.scala 175:35 184:30]
    node _setDirty_3_T = cat(way_dirtyAfterValid_3_1, way_dirtyAfterValid_3_0) @[L1TagAccess.scala 185:45]
    node _setDirty_3_T_1 = orr(_setDirty_3_T) @[L1TagAccess.scala 185:52]
    node setDirty_1 = _setDirty_1_T_1 @[L1TagAccess.scala 174:24 185:19]
    node setDirty_0 = _setDirty_0_T_1 @[L1TagAccess.scala 174:24 185:19]
    node hasDirty_st0_lo = cat(setDirty_1, setDirty_0) @[L1TagAccess.scala 187:30]
    node setDirty_3 = _setDirty_3_T_1 @[L1TagAccess.scala 174:24 185:19]
    node setDirty_2 = _setDirty_2_T_1 @[L1TagAccess.scala 174:24 185:19]
    node hasDirty_st0_hi = cat(setDirty_3, setDirty_2) @[L1TagAccess.scala 187:30]
    node _hasDirty_st0_T = cat(hasDirty_st0_hi, hasDirty_st0_lo) @[L1TagAccess.scala 187:30]
    node _hasDirty_st0_T_1 = orr(_hasDirty_st0_T) @[L1TagAccess.scala 187:37]
    node _choosenDirtySetIdx_st0_T = mux(setDirty_2, UInt<2>("h2"), UInt<2>("h3")) @[Mux.scala 47:70]
    node _choosenDirtySetIdx_st0_T_1 = mux(setDirty_1, UInt<1>("h1"), _choosenDirtySetIdx_st0_T) @[Mux.scala 47:70]
    node _choosenDirtySetIdx_st0_T_2 = mux(setDirty_0, UInt<1>("h0"), _choosenDirtySetIdx_st0_T_1) @[Mux.scala 47:70]
    node choosenDirtySetIdx_st0 = _choosenDirtySetIdx_st0_T_2 @[L1TagAccess.scala 188:28 62:36]
    node _GEN_87 = validif(eq(UInt<1>("h0"), choosenDirtySetIdx_st0), way_dirtyAfterValid_0_0) @[L1TagAccess.scala 189:{26,26}]
    node _GEN_88 = mux(eq(UInt<1>("h1"), choosenDirtySetIdx_st0), way_dirtyAfterValid_1_0, _GEN_87) @[L1TagAccess.scala 189:{26,26}]
    node _GEN_89 = mux(eq(UInt<2>("h2"), choosenDirtySetIdx_st0), way_dirtyAfterValid_2_0, _GEN_88) @[L1TagAccess.scala 189:{26,26}]
    node _GEN_90 = mux(eq(UInt<2>("h3"), choosenDirtySetIdx_st0), way_dirtyAfterValid_3_0, _GEN_89) @[L1TagAccess.scala 189:{26,26}]
    node _GEN_91 = validif(eq(UInt<1>("h0"), choosenDirtySetIdx_st0), way_dirtyAfterValid_0_1) @[L1TagAccess.scala 189:{26,26}]
    node _GEN_92 = mux(eq(UInt<1>("h1"), choosenDirtySetIdx_st0), way_dirtyAfterValid_1_1, _GEN_91) @[L1TagAccess.scala 189:{26,26}]
    node _GEN_93 = mux(eq(UInt<2>("h2"), choosenDirtySetIdx_st0), way_dirtyAfterValid_2_1, _GEN_92) @[L1TagAccess.scala 189:{26,26}]
    node _GEN_94 = mux(eq(UInt<2>("h3"), choosenDirtySetIdx_st0), way_dirtyAfterValid_3_1, _GEN_93) @[L1TagAccess.scala 189:{26,26}]
    node _way_dirtyAfterValid_choosenDirtySetIdx_st0_1 = _GEN_94 @[L1TagAccess.scala 189:26]
    node choosenDirtySetValid_1 = _way_dirtyAfterValid_choosenDirtySetIdx_st0_1 @[L1TagAccess.scala 177:36 189:26]
    node _choosenDirtyWayMask_st0_enc_T = mux(choosenDirtySetValid_1, UInt<2>("h2"), UInt<2>("h0")) @[Mux.scala 47:70]
    node _way_dirtyAfterValid_choosenDirtySetIdx_st0_0 = _GEN_90 @[L1TagAccess.scala 189:26]
    node choosenDirtySetValid_0 = _way_dirtyAfterValid_choosenDirtySetIdx_st0_0 @[L1TagAccess.scala 177:36 189:26]
    node choosenDirtyWayMask_st0_enc = mux(choosenDirtySetValid_0, UInt<2>("h1"), _choosenDirtyWayMask_st0_enc_T) @[Mux.scala 47:70]
    node _choosenDirtyWayMask_st0_T = bits(choosenDirtyWayMask_st0_enc, 0, 0) @[OneHot.scala 82:30]
    node _choosenDirtyWayMask_st0_T_1 = bits(choosenDirtyWayMask_st0_enc, 1, 1) @[OneHot.scala 82:30]
    node _choosenDirtyWayMask_st0_WIRE_1 = _choosenDirtyWayMask_st0_T_1 @[L1TagAccess.scala 190:{39,39}]
    node _choosenDirtyWayMask_st0_WIRE_0 = _choosenDirtyWayMask_st0_T @[L1TagAccess.scala 190:{39,39}]
    node _choosenDirtyWayMask_st0_T_2 = cat(_choosenDirtyWayMask_st0_WIRE_1, _choosenDirtyWayMask_st0_WIRE_0) @[L1TagAccess.scala 190:81]
    node choosenDirtyWayMask_st0 = _choosenDirtyWayMask_st0_T_2 @[L1TagAccess.scala 178:39 190:29]
    node _choosenDirtyTag_st1_T = bits(choosenDirtyWayMask_st0, 0, 0)
    node _GEN_95 = validif(eq(UInt<1>("h0"), _choosenDirtyTag_st1_T), tagBodyAccess.io_r_resp_data_0) @[L1TagAccess.scala 191:{25,25}]
    node _GEN_96 = mux(eq(UInt<1>("h1"), _choosenDirtyTag_st1_T), tagBodyAccess.io_r_resp_data_1, _GEN_95) @[L1TagAccess.scala 191:{25,25}]
    node accessCounterFull = _GEN_2
    node hasDirty_st0 = _hasDirty_st0_T_1 @[L1TagAccess.scala 187:18 61:26]
    node _WIRE__0 = accessCount @[SRAMTemplate.scala 54:{18,18}]
    node _WIRE__1 = accessCount @[SRAMTemplate.scala 54:{18,18}]
    node _WIRE_1_0 = accessCount @[SRAMTemplate.scala 54:{18,18}]
    node _WIRE_1_1 = accessCount @[SRAMTemplate.scala 54:{18,18}]
    node _way_valid_WIRE__0 = UInt<1>("h0") @[L1TagAccess.scala 124:{56,56}]
    node _way_valid_WIRE__1 = UInt<1>("h0") @[L1TagAccess.scala 124:{56,56}]
    node _way_valid_WIRE_1_0 = UInt<1>("h0") @[L1TagAccess.scala 124:{56,56}]
    node _way_valid_WIRE_1_1 = UInt<1>("h0") @[L1TagAccess.scala 124:{56,56}]
    node _way_valid_WIRE_2_0 = UInt<1>("h0") @[L1TagAccess.scala 124:{56,56}]
    node _way_valid_WIRE_2_1 = UInt<1>("h0") @[L1TagAccess.scala 124:{56,56}]
    node _way_valid_WIRE_3_0 = UInt<1>("h0") @[L1TagAccess.scala 124:{56,56}]
    node _way_valid_WIRE_3_1 = UInt<1>("h0") @[L1TagAccess.scala 124:{56,56}]
    node _way_valid_WIRE_4_0_0 = _way_valid_WIRE__0 @[L1TagAccess.scala 124:{34,34}]
    node _way_valid_WIRE_4_0_1 = _way_valid_WIRE__1 @[L1TagAccess.scala 124:{34,34}]
    node _way_valid_WIRE_4_1_0 = _way_valid_WIRE_1_0 @[L1TagAccess.scala 124:{34,34}]
    node _way_valid_WIRE_4_1_1 = _way_valid_WIRE_1_1 @[L1TagAccess.scala 124:{34,34}]
    node _way_valid_WIRE_4_2_0 = _way_valid_WIRE_2_0 @[L1TagAccess.scala 124:{34,34}]
    node _way_valid_WIRE_4_2_1 = _way_valid_WIRE_2_1 @[L1TagAccess.scala 124:{34,34}]
    node _way_valid_WIRE_4_3_0 = _way_valid_WIRE_3_0 @[L1TagAccess.scala 124:{34,34}]
    node _way_valid_WIRE_4_3_1 = _way_valid_WIRE_3_1 @[L1TagAccess.scala 124:{34,34}]
    node _way_dirty_WIRE__0 = UInt<1>("h0") @[L1TagAccess.scala 126:{56,56}]
    node _way_dirty_WIRE__1 = UInt<1>("h0") @[L1TagAccess.scala 126:{56,56}]
    node _way_dirty_WIRE_1_0 = UInt<1>("h0") @[L1TagAccess.scala 126:{56,56}]
    node _way_dirty_WIRE_1_1 = UInt<1>("h0") @[L1TagAccess.scala 126:{56,56}]
    node _way_dirty_WIRE_2_0 = UInt<1>("h0") @[L1TagAccess.scala 126:{56,56}]
    node _way_dirty_WIRE_2_1 = UInt<1>("h0") @[L1TagAccess.scala 126:{56,56}]
    node _way_dirty_WIRE_3_0 = UInt<1>("h0") @[L1TagAccess.scala 126:{56,56}]
    node _way_dirty_WIRE_3_1 = UInt<1>("h0") @[L1TagAccess.scala 126:{56,56}]
    node _way_dirty_WIRE_4_0_0 = _way_dirty_WIRE__0 @[L1TagAccess.scala 126:{34,34}]
    node _way_dirty_WIRE_4_0_1 = _way_dirty_WIRE__1 @[L1TagAccess.scala 126:{34,34}]
    node _way_dirty_WIRE_4_1_0 = _way_dirty_WIRE_1_0 @[L1TagAccess.scala 126:{34,34}]
    node _way_dirty_WIRE_4_1_1 = _way_dirty_WIRE_1_1 @[L1TagAccess.scala 126:{34,34}]
    node _way_dirty_WIRE_4_2_0 = _way_dirty_WIRE_2_0 @[L1TagAccess.scala 126:{34,34}]
    node _way_dirty_WIRE_4_2_1 = _way_dirty_WIRE_2_1 @[L1TagAccess.scala 126:{34,34}]
    node _way_dirty_WIRE_4_3_0 = _way_dirty_WIRE_3_0 @[L1TagAccess.scala 126:{34,34}]
    node _way_dirty_WIRE_4_3_1 = _way_dirty_WIRE_3_1 @[L1TagAccess.scala 126:{34,34}]
    node _way_valid_iTagChecker_io_way_valid_r_0 = _GEN_7 @[L1TagAccess.scala 133:28]
    node _way_valid_iTagChecker_io_way_valid_r_1 = _GEN_11 @[L1TagAccess.scala 133:28]
    node _WIRE_2_0 = io_allocateWriteData_st1 @[SRAMTemplate.scala 54:{18,18}]
    node _WIRE_2_1 = io_allocateWriteData_st1 @[SRAMTemplate.scala 54:{18,18}]
    node _tagBodyAccess_io_r_resp_data_choosenDirtyTag_st1_T = _GEN_96 @[L1TagAccess.scala 191:25]
    node choosenDirtyTag_st1 = _tagBodyAccess_io_r_resp_data_choosenDirtyTag_st1_T @[L1TagAccess.scala 179:35 191:25]
    io_probeRead_ready <= tagAccessRArb.io_in_1_ready @[L1TagAccess.scala 80:28]
    io_hit_st1 <= iTagChecker.io_cache_hit @[L1TagAccess.scala 135:14]
    io_waymaskHit_st1 <= iTagChecker.io_waymask @[L1TagAccess.scala 134:21]
    io_needReplace <= _io_needReplace_T_1 @[L1TagAccess.scala 151:24]
    io_waymaskReplacement_st1 <= Replacement.io_waymask_st1 @[L1TagAccess.scala 156:29]
    io_a_addrReplacement_st1 <= bits(_io_a_addrReplacement_st1_T_2, 25, 0) @[L1TagAccess.scala 158:34]
    io_hasDirty_st0 <= hasDirty_st0 @[L1TagAccess.scala 197:25]
    io_dirtySetIdx_st0 <= choosenDirtySetIdx_st0 @[L1TagAccess.scala 195:28]
    io_dirtyWayMask_st0 <= choosenDirtyWayMask_st0 @[L1TagAccess.scala 196:29]
    io_dirtyTag_st1 <= choosenDirtyTag_st1 @[L1TagAccess.scala 194:25]
    accessCount <= mux(reset, UInt<10>("h0"), _GEN_1) @[Counter.scala 62:{40,40}]
    tagBodyAccess.clock <= clock
    tagBodyAccess.reset <= reset
    tagBodyAccess.io_r_req_valid <= tagAccessRArb.io_out_valid @[L1TagAccess.scala 78:28]
    tagBodyAccess.io_r_req_bits_setIdx <= tagAccessRArb.io_out_bits_setIdx @[L1TagAccess.scala 78:28]
    tagBodyAccess.io_w_req_valid <= io_allocateWriteTagSRAMWValid_st1 @[L1TagAccess.scala 162:32]
    tagBodyAccess.io_w_req_bits_setIdx <= allocateWrite_st1_setIdx @[SRAMTemplate.scala 37:17]
    tagBodyAccess.io_w_req_bits_data_0 <= _WIRE_2_0 @[SRAMTemplate.scala 48:15]
    tagBodyAccess.io_w_req_bits_data_1 <= _WIRE_2_1 @[SRAMTemplate.scala 48:15]
    tagBodyAccess.io_w_req_bits_waymask <= Replacement.io_waymask_st1 @[SRAMTemplate.scala 49:24]
    tagAccessRArb.clock <= clock
    tagAccessRArb.reset <= reset
    tagAccessRArb.io_in_0_valid <= io_allocateWrite_valid @[L1TagAccess.scala 82:34]
    tagAccessRArb.io_in_0_bits_setIdx <= io_allocateWrite_bits_setIdx @[L1TagAccess.scala 83:40]
    tagAccessRArb.io_in_1_valid <= io_probeRead_valid @[L1TagAccess.scala 80:28]
    tagAccessRArb.io_in_1_bits_setIdx <= io_probeRead_bits_setIdx @[L1TagAccess.scala 80:28]
    tagAccessRArb.io_in_2_valid <= _tagAccessRArb_io_in_2_valid_T_2 @[L1TagAccess.scala 85:34]
    tagAccessRArb.io_in_2_bits_setIdx <= choosenDirtySetIdx_st0 @[L1TagAccess.scala 86:40]
    tagAccessRArb.io_out_ready <= tagBodyAccess.io_r_req_ready @[L1TagAccess.scala 78:28]
    timeAccess.clock <= clock
    timeAccess.reset <= reset
    timeAccess.io_r_req_valid <= io_allocateWrite_valid @[L1TagAccess.scala 101:29]
    timeAccess.io_r_req_bits_setIdx <= io_allocateWrite_bits_setIdx @[L1TagAccess.scala 102:35]
    timeAccess.io_w_req_valid <= timeAccessWArb.io_out_valid @[L1TagAccess.scala 122:23]
    timeAccess.io_w_req_bits_setIdx <= timeAccessWArb.io_out_bits_setIdx @[L1TagAccess.scala 122:23]
    timeAccess.io_w_req_bits_data_0 <= timeAccessWArb.io_out_bits_data_0 @[L1TagAccess.scala 122:23]
    timeAccess.io_w_req_bits_data_1 <= timeAccessWArb.io_out_bits_data_1 @[L1TagAccess.scala 122:23]
    timeAccess.io_w_req_bits_waymask <= timeAccessWArb.io_out_bits_waymask @[L1TagAccess.scala 122:23]
    timeAccessWArb.clock <= clock
    timeAccessWArb.reset <= reset
    timeAccessWArb.io_in_0_valid <= io_hit_st1 @[L1TagAccess.scala 109:33]
    timeAccessWArb.io_in_0_bits_setIdx <= REG @[SRAMTemplate.scala 37:17]
    timeAccessWArb.io_in_0_bits_data_0 <= _WIRE__0 @[SRAMTemplate.scala 48:15]
    timeAccessWArb.io_in_0_bits_data_1 <= _WIRE__1 @[SRAMTemplate.scala 48:15]
    timeAccessWArb.io_in_0_bits_waymask <= io_waymaskHit_st1 @[SRAMTemplate.scala 49:24]
    timeAccessWArb.io_in_1_valid <= timeAccessWArb_io_in_1_valid_REG @[L1TagAccess.scala 116:33]
    timeAccessWArb.io_in_1_bits_setIdx <= REG_1 @[SRAMTemplate.scala 37:17]
    timeAccessWArb.io_in_1_bits_data_0 <= _WIRE_1_0 @[SRAMTemplate.scala 48:15]
    timeAccessWArb.io_in_1_bits_data_1 <= _WIRE_1_1 @[SRAMTemplate.scala 48:15]
    timeAccessWArb.io_in_1_bits_waymask <= io_waymaskReplacement_st1 @[SRAMTemplate.scala 49:24]
    timeAccessWArb.io_out_ready <= timeAccess.io_w_req_ready @[L1TagAccess.scala 122:23]
    REG <= io_probeRead_bits_setIdx @[L1TagAccess.scala 112:21]
    timeAccessWArb_io_in_1_valid_REG <= io_allocateWrite_valid @[L1TagAccess.scala 116:43]
    REG_1 <= io_allocateWrite_bits_setIdx @[L1TagAccess.scala 119:21]
    way_valid_0_0 <= mux(reset, _way_valid_WIRE_4_0_0, _GEN_79) @[L1TagAccess.scala 124:{26,26}]
    way_valid_0_1 <= mux(reset, _way_valid_WIRE_4_0_1, _GEN_80) @[L1TagAccess.scala 124:{26,26}]
    way_valid_1_0 <= mux(reset, _way_valid_WIRE_4_1_0, _GEN_81) @[L1TagAccess.scala 124:{26,26}]
    way_valid_1_1 <= mux(reset, _way_valid_WIRE_4_1_1, _GEN_82) @[L1TagAccess.scala 124:{26,26}]
    way_valid_2_0 <= mux(reset, _way_valid_WIRE_4_2_0, _GEN_83) @[L1TagAccess.scala 124:{26,26}]
    way_valid_2_1 <= mux(reset, _way_valid_WIRE_4_2_1, _GEN_84) @[L1TagAccess.scala 124:{26,26}]
    way_valid_3_0 <= mux(reset, _way_valid_WIRE_4_3_0, _GEN_85) @[L1TagAccess.scala 124:{26,26}]
    way_valid_3_1 <= mux(reset, _way_valid_WIRE_4_3_1, _GEN_86) @[L1TagAccess.scala 124:{26,26}]
    way_dirty_0_0 <= mux(reset, _way_dirty_WIRE_4_0_0, _GEN_36) @[L1TagAccess.scala 126:{26,26}]
    way_dirty_0_1 <= mux(reset, _way_dirty_WIRE_4_0_1, _GEN_37) @[L1TagAccess.scala 126:{26,26}]
    way_dirty_1_0 <= mux(reset, _way_dirty_WIRE_4_1_0, _GEN_38) @[L1TagAccess.scala 126:{26,26}]
    way_dirty_1_1 <= mux(reset, _way_dirty_WIRE_4_1_1, _GEN_39) @[L1TagAccess.scala 126:{26,26}]
    way_dirty_2_0 <= mux(reset, _way_dirty_WIRE_4_2_0, _GEN_40) @[L1TagAccess.scala 126:{26,26}]
    way_dirty_2_1 <= mux(reset, _way_dirty_WIRE_4_2_1, _GEN_41) @[L1TagAccess.scala 126:{26,26}]
    way_dirty_3_0 <= mux(reset, _way_dirty_WIRE_4_3_0, _GEN_42) @[L1TagAccess.scala 126:{26,26}]
    way_dirty_3_1 <= mux(reset, _way_dirty_WIRE_4_3_1, _GEN_43) @[L1TagAccess.scala 126:{26,26}]
    iTagChecker.clock <= clock
    iTagChecker.reset <= reset
    iTagChecker.io_tag_of_set_0 <= tagBodyAccess.io_r_resp_data_0 @[L1TagAccess.scala 131:29]
    iTagChecker.io_tag_of_set_1 <= tagBodyAccess.io_r_resp_data_1 @[L1TagAccess.scala 131:29]
    iTagChecker.io_tag_from_pipe <= io_tagFromCore_st1 @[L1TagAccess.scala 132:32]
    iTagChecker.io_way_valid_0 <= _way_valid_iTagChecker_io_way_valid_r_0 @[L1TagAccess.scala 133:28]
    iTagChecker.io_way_valid_1 <= _way_valid_iTagChecker_io_way_valid_r_1 @[L1TagAccess.scala 133:28]
    iTagChecker_io_way_valid_r <= _GEN_3
    REG_2 <= io_probeRead_bits_setIdx @[L1TagAccess.scala 139:24]
    Replacement.clock <= clock
    Replacement.reset <= reset
    Replacement.io_validOfSet <= _Replacement_io_validOfSet_T @[L1TagAccess.scala 154:29]
    Replacement.io_timeOfSet_st1_0 <= timeAccess.io_r_resp_data_0 @[L1TagAccess.scala 155:32]
    Replacement.io_timeOfSet_st1_1 <= timeAccess.io_r_resp_data_1 @[L1TagAccess.scala 155:32]
    allocateWrite_st1_setIdx <= _GEN_44
    REG_3 <= io_allocateWrite_valid @[L1TagAccess.scala 164:15]
    printf(clock, and(and(and(UInt<1>("h1"), _T_4), _T_5), UInt<1>("h1")), "Assertion failed: tag probe and allocate in same cycle\n    at L1TagAccess.scala:54 assert(!(io.probeRead.fire && io.allocateWrite.fire), s\"tag probe and allocate in same cycle\")\n") : printf @[L1TagAccess.scala 54:9]
    assert(clock, _T_2, and(and(UInt<1>("h1"), _T_4), UInt<1>("h1")), "") : assert @[L1TagAccess.scala 54:9]
    printf(clock, and(and(and(UInt<1>("h1"), _T_9), _T_10), UInt<1>("h1")), "Assertion failed: tag probe and allocate in same cycle\n    at L1TagAccess.scala:106 assert(!(timeAccessWArb.io.in(0).valid && timeAccessWArb.io.in(1).valid), s\"tag probe and allocate in same cycle\")\n") : printf_1 @[L1TagAccess.scala 106:9]
    assert(clock, _T_7, and(and(UInt<1>("h1"), _T_9), UInt<1>("h1")), "") : assert_1 @[L1TagAccess.scala 106:9]
    printf(clock, and(and(and(UInt<1>("h1"), _T_15), _T_16), UInt<1>("h1")), "Assertion failed: way_dirty write-in conflict!\n    at L1TagAccess.scala:137 assert(!(iTagChecker.io.cache_hit && io.probeIsWrite_st1.get && io.flushChoosen.get.valid),\"way_dirty write-in conflict!\")\n") : printf_2 @[L1TagAccess.scala 137:11]
    assert(clock, _T_13, and(and(UInt<1>("h1"), _T_15), UInt<1>("h1")), "") : assert_2 @[L1TagAccess.scala 137:11]
    printf(clock, and(and(and(UInt<1>("h1"), _T_30), _T_31), UInt<1>("h1")), "Assertion failed\n    at L1TagAccess.scala:169 assert(!(io.allocateWrite.valid && io.invalidateAll))\n") : printf_3 @[L1TagAccess.scala 169:9]
    assert(clock, _T_28, and(and(UInt<1>("h1"), _T_30), UInt<1>("h1")), "") : assert_3 @[L1TagAccess.scala 169:9]

  module DCacheWSHR :
    input clock : Clock
    input reset : UInt<1>
    output io_pushReq_ready : UInt<1>
    input io_pushReq_valid : UInt<1>
    input io_pushReq_bits_blockAddr : UInt<28>
    output io_conflict : UInt<1>
    output io_pushedIdx : UInt<2>
    output io_empty : UInt<1>
    input io_popReq_valid : UInt<1>
    input io_popReq_bits : UInt<2>

    node _T = and(io_pushReq_valid, io_popReq_valid) @[DCacheWSHR.scala 22:29]
    node _T_1 = eq(_T, UInt<1>("h0")) @[DCacheWSHR.scala 22:10]
    node _T_2 = asUInt(reset) @[DCacheWSHR.scala 22:9]
    node _T_3 = eq(_T_2, UInt<1>("h0")) @[DCacheWSHR.scala 22:9]
    node _T_4 = eq(_T_1, UInt<1>("h0")) @[DCacheWSHR.scala 22:9]
    reg blockAddrEntries_0 : UInt<28>, clock with :
      reset => (UInt<1>("h0"), blockAddrEntries_0) @[DCacheWSHR.scala 24:33]
    reg blockAddrEntries_1 : UInt<28>, clock with :
      reset => (UInt<1>("h0"), blockAddrEntries_1) @[DCacheWSHR.scala 24:33]
    reg blockAddrEntries_2 : UInt<28>, clock with :
      reset => (UInt<1>("h0"), blockAddrEntries_2) @[DCacheWSHR.scala 24:33]
    reg blockAddrEntries_3 : UInt<28>, clock with :
      reset => (UInt<1>("h0"), blockAddrEntries_3) @[DCacheWSHR.scala 24:33]
    reg valid_0 : UInt<1>, clock with :
      reset => (UInt<1>("h0"), valid_0) @[DCacheWSHR.scala 25:33]
    reg valid_1 : UInt<1>, clock with :
      reset => (UInt<1>("h0"), valid_1) @[DCacheWSHR.scala 25:33]
    reg valid_2 : UInt<1>, clock with :
      reset => (UInt<1>("h0"), valid_2) @[DCacheWSHR.scala 25:33]
    reg valid_3 : UInt<1>, clock with :
      reset => (UInt<1>("h0"), valid_3) @[DCacheWSHR.scala 25:33]
    node _io_empty_T = or(valid_0, valid_1) @[DCacheWSHR.scala 26:34]
    node _io_empty_T_1 = or(valid_2, valid_3) @[DCacheWSHR.scala 26:34]
    node _io_empty_T_2 = or(_io_empty_T, _io_empty_T_1) @[DCacheWSHR.scala 26:34]
    node _io_empty_T_3 = eq(_io_empty_T_2, UInt<1>("h0")) @[DCacheWSHR.scala 26:15]
    node _pushMatchMask_T = eq(blockAddrEntries_0, io_pushReq_bits_blockAddr) @[DCacheWSHR.scala 31:27]
    node _pushMatchMask_T_1 = and(_pushMatchMask_T, valid_0) @[DCacheWSHR.scala 31:58]
    node _pushMatchMask_T_2 = eq(blockAddrEntries_1, io_pushReq_bits_blockAddr) @[DCacheWSHR.scala 31:27]
    node _pushMatchMask_T_3 = and(_pushMatchMask_T_2, valid_1) @[DCacheWSHR.scala 31:58]
    node _pushMatchMask_T_4 = eq(blockAddrEntries_2, io_pushReq_bits_blockAddr) @[DCacheWSHR.scala 31:27]
    node _pushMatchMask_T_5 = and(_pushMatchMask_T_4, valid_2) @[DCacheWSHR.scala 31:58]
    node _pushMatchMask_T_6 = eq(blockAddrEntries_3, io_pushReq_bits_blockAddr) @[DCacheWSHR.scala 31:27]
    node _pushMatchMask_T_7 = and(_pushMatchMask_T_6, valid_3) @[DCacheWSHR.scala 31:58]
    node pushMatchMask_lo = cat(_pushMatchMask_T_5, _pushMatchMask_T_7) @[Cat.scala 31:58]
    node pushMatchMask_hi = cat(_pushMatchMask_T_1, _pushMatchMask_T_3) @[Cat.scala 31:58]
    node _pushMatchMask_T_8 = cat(pushMatchMask_hi, pushMatchMask_lo) @[Cat.scala 31:58]
    node _pushMatchMask_T_9 = bits(_pushMatchMask_T_8, 1, 0) @[Bitwise.scala 111:18]
    node _pushMatchMask_T_10 = bits(_pushMatchMask_T_9, 0, 0) @[Bitwise.scala 111:18]
    node _pushMatchMask_T_11 = bits(_pushMatchMask_T_9, 1, 1) @[Bitwise.scala 111:47]
    node _pushMatchMask_T_12 = cat(_pushMatchMask_T_10, _pushMatchMask_T_11) @[Cat.scala 31:58]
    node _pushMatchMask_T_13 = bits(_pushMatchMask_T_8, 3, 2) @[Bitwise.scala 111:47]
    node _pushMatchMask_T_14 = bits(_pushMatchMask_T_13, 0, 0) @[Bitwise.scala 111:18]
    node _pushMatchMask_T_15 = bits(_pushMatchMask_T_13, 1, 1) @[Bitwise.scala 111:47]
    node _pushMatchMask_T_16 = cat(_pushMatchMask_T_14, _pushMatchMask_T_15) @[Cat.scala 31:58]
    node _pushMatchMask_T_17 = cat(_pushMatchMask_T_12, _pushMatchMask_T_16) @[Cat.scala 31:58]
    node pushMatchMask = _pushMatchMask_T_17 @[DCacheWSHR.scala 29:27 30:17]
    node _T_5 = bits(pushMatchMask, 0, 0) @[Bitwise.scala 50:65]
    node _T_6 = bits(pushMatchMask, 1, 1) @[Bitwise.scala 50:65]
    node _T_7 = bits(pushMatchMask, 2, 2) @[Bitwise.scala 50:65]
    node _T_8 = bits(pushMatchMask, 3, 3) @[Bitwise.scala 50:65]
    node _T_9 = add(_T_5, _T_6) @[Bitwise.scala 48:55]
    node _T_10 = bits(_T_9, 1, 0) @[Bitwise.scala 48:55]
    node _T_11 = add(_T_7, _T_8) @[Bitwise.scala 48:55]
    node _T_12 = bits(_T_11, 1, 0) @[Bitwise.scala 48:55]
    node _T_13 = add(_T_10, _T_12) @[Bitwise.scala 48:55]
    node _T_14 = bits(_T_13, 2, 0) @[Bitwise.scala 48:55]
    node _T_15 = leq(_T_14, UInt<1>("h1")) @[DCacheWSHR.scala 32:34]
    node _T_16 = asUInt(reset) @[DCacheWSHR.scala 32:9]
    node _T_17 = eq(_T_16, UInt<1>("h0")) @[DCacheWSHR.scala 32:9]
    node _T_18 = eq(_T_15, UInt<1>("h0")) @[DCacheWSHR.scala 32:9]
    node _io_conflict_T = orr(pushMatchMask) @[DCacheWSHR.scala 33:32]
    node _io_pushReq_ready_T = and(valid_0, valid_1) @[DCacheWSHR.scala 36:43]
    node _io_pushReq_ready_T_1 = and(valid_2, valid_3) @[DCacheWSHR.scala 36:43]
    node _io_pushReq_ready_T_2 = and(_io_pushReq_ready_T, _io_pushReq_ready_T_1) @[DCacheWSHR.scala 36:43]
    node _io_pushReq_ready_T_3 = eq(_io_pushReq_ready_T_2, UInt<1>("h0")) @[DCacheWSHR.scala 36:23]
    node _nextEntryIdx_T = eq(valid_0, UInt<1>("h0")) @[DCacheWSHR.scala 38:54]
    node _nextEntryIdx_T_1 = eq(valid_1, UInt<1>("h0")) @[DCacheWSHR.scala 38:54]
    node _nextEntryIdx_T_2 = eq(valid_2, UInt<1>("h0")) @[DCacheWSHR.scala 38:54]
    node _nextEntryIdx_T_3 = eq(valid_3, UInt<1>("h0")) @[DCacheWSHR.scala 38:54]
    node _nextEntryIdx_T_4 = mux(_nextEntryIdx_T_2, UInt<2>("h2"), UInt<2>("h3")) @[DCacheWSHR.scala 38:38]
    node _nextEntryIdx_T_5 = mux(_nextEntryIdx_T_1, UInt<1>("h1"), _nextEntryIdx_T_4) @[DCacheWSHR.scala 38:38]
    node nextEntryIdx = mux(_nextEntryIdx_T, UInt<1>("h0"), _nextEntryIdx_T_5) @[DCacheWSHR.scala 38:38]
    node _T_19 = and(io_pushReq_ready, io_pushReq_valid) @[Decoupled.scala 50:35]
    node _blockAddrEntries_nextEntryIdx = io_pushReq_bits_blockAddr @[DCacheWSHR.scala 41:{36,36}]
    node _GEN_0 = mux(eq(UInt<1>("h0"), nextEntryIdx), _blockAddrEntries_nextEntryIdx, blockAddrEntries_0) @[DCacheWSHR.scala 24:33 41:{36,36}]
    node _GEN_1 = mux(eq(UInt<1>("h1"), nextEntryIdx), _blockAddrEntries_nextEntryIdx, blockAddrEntries_1) @[DCacheWSHR.scala 24:33 41:{36,36}]
    node _GEN_2 = mux(eq(UInt<2>("h2"), nextEntryIdx), _blockAddrEntries_nextEntryIdx, blockAddrEntries_2) @[DCacheWSHR.scala 24:33 41:{36,36}]
    node _GEN_3 = mux(eq(UInt<2>("h3"), nextEntryIdx), _blockAddrEntries_nextEntryIdx, blockAddrEntries_3) @[DCacheWSHR.scala 24:33 41:{36,36}]
    node _valid_nextEntryIdx = UInt<1>("h1") @[DCacheWSHR.scala 42:{25,25}]
    node _GEN_4 = mux(eq(UInt<1>("h0"), nextEntryIdx), _valid_nextEntryIdx, valid_0) @[DCacheWSHR.scala 42:{25,25} 25:33]
    node _GEN_5 = mux(eq(UInt<1>("h1"), nextEntryIdx), _valid_nextEntryIdx, valid_1) @[DCacheWSHR.scala 42:{25,25} 25:33]
    node _GEN_6 = mux(eq(UInt<2>("h2"), nextEntryIdx), _valid_nextEntryIdx, valid_2) @[DCacheWSHR.scala 42:{25,25} 25:33]
    node _GEN_7 = mux(eq(UInt<2>("h3"), nextEntryIdx), _valid_nextEntryIdx, valid_3) @[DCacheWSHR.scala 42:{25,25} 25:33]
    node _valid_io_popReq_bits = UInt<1>("h0") @[DCacheWSHR.scala 44:{27,27}]
    node _GEN_8 = mux(eq(UInt<1>("h0"), io_popReq_bits), _valid_io_popReq_bits, valid_0) @[DCacheWSHR.scala 44:{27,27} 25:33]
    node _GEN_9 = mux(eq(UInt<1>("h1"), io_popReq_bits), _valid_io_popReq_bits, valid_1) @[DCacheWSHR.scala 44:{27,27} 25:33]
    node _GEN_10 = mux(eq(UInt<2>("h2"), io_popReq_bits), _valid_io_popReq_bits, valid_2) @[DCacheWSHR.scala 44:{27,27} 25:33]
    node _GEN_11 = mux(eq(UInt<2>("h3"), io_popReq_bits), _valid_io_popReq_bits, valid_3) @[DCacheWSHR.scala 44:{27,27} 25:33]
    node _GEN_12 = mux(io_popReq_valid, _GEN_8, valid_0) @[DCacheWSHR.scala 43:30 25:33]
    node _GEN_13 = mux(io_popReq_valid, _GEN_9, valid_1) @[DCacheWSHR.scala 43:30 25:33]
    node _GEN_14 = mux(io_popReq_valid, _GEN_10, valid_2) @[DCacheWSHR.scala 43:30 25:33]
    node _GEN_15 = mux(io_popReq_valid, _GEN_11, valid_3) @[DCacheWSHR.scala 43:30 25:33]
    node _GEN_16 = mux(_T_19, _GEN_0, blockAddrEntries_0) @[DCacheWSHR.scala 40:24 24:33]
    node _GEN_17 = mux(_T_19, _GEN_1, blockAddrEntries_1) @[DCacheWSHR.scala 40:24 24:33]
    node _GEN_18 = mux(_T_19, _GEN_2, blockAddrEntries_2) @[DCacheWSHR.scala 40:24 24:33]
    node _GEN_19 = mux(_T_19, _GEN_3, blockAddrEntries_3) @[DCacheWSHR.scala 40:24 24:33]
    node _GEN_20 = mux(_T_19, _GEN_4, _GEN_12) @[DCacheWSHR.scala 40:24]
    node _GEN_21 = mux(_T_19, _GEN_5, _GEN_13) @[DCacheWSHR.scala 40:24]
    node _GEN_22 = mux(_T_19, _GEN_6, _GEN_14) @[DCacheWSHR.scala 40:24]
    node _GEN_23 = mux(_T_19, _GEN_7, _GEN_15) @[DCacheWSHR.scala 40:24]
    node _blockAddrEntries_WIRE_0 = UInt<28>("h0") @[DCacheWSHR.scala 24:{41,41}]
    node _blockAddrEntries_WIRE_1 = UInt<28>("h0") @[DCacheWSHR.scala 24:{41,41}]
    node _blockAddrEntries_WIRE_2 = UInt<28>("h0") @[DCacheWSHR.scala 24:{41,41}]
    node _blockAddrEntries_WIRE_3 = UInt<28>("h0") @[DCacheWSHR.scala 24:{41,41}]
    node _valid_WIRE_0 = UInt<1>("h0") @[DCacheWSHR.scala 25:{41,41}]
    node _valid_WIRE_1 = UInt<1>("h0") @[DCacheWSHR.scala 25:{41,41}]
    node _valid_WIRE_2 = UInt<1>("h0") @[DCacheWSHR.scala 25:{41,41}]
    node _valid_WIRE_3 = UInt<1>("h0") @[DCacheWSHR.scala 25:{41,41}]
    io_pushReq_ready <= _io_pushReq_ready_T_3 @[DCacheWSHR.scala 36:20]
    io_conflict <= _io_conflict_T @[DCacheWSHR.scala 33:15]
    io_pushedIdx <= nextEntryIdx @[DCacheWSHR.scala 39:16]
    io_empty <= _io_empty_T_3 @[DCacheWSHR.scala 26:12]
    blockAddrEntries_0 <= mux(reset, _blockAddrEntries_WIRE_0, _GEN_16) @[DCacheWSHR.scala 24:{33,33}]
    blockAddrEntries_1 <= mux(reset, _blockAddrEntries_WIRE_1, _GEN_17) @[DCacheWSHR.scala 24:{33,33}]
    blockAddrEntries_2 <= mux(reset, _blockAddrEntries_WIRE_2, _GEN_18) @[DCacheWSHR.scala 24:{33,33}]
    blockAddrEntries_3 <= mux(reset, _blockAddrEntries_WIRE_3, _GEN_19) @[DCacheWSHR.scala 24:{33,33}]
    valid_0 <= mux(reset, _valid_WIRE_0, _GEN_20) @[DCacheWSHR.scala 25:{33,33}]
    valid_1 <= mux(reset, _valid_WIRE_1, _GEN_21) @[DCacheWSHR.scala 25:{33,33}]
    valid_2 <= mux(reset, _valid_WIRE_2, _GEN_22) @[DCacheWSHR.scala 25:{33,33}]
    valid_3 <= mux(reset, _valid_WIRE_3, _GEN_23) @[DCacheWSHR.scala 25:{33,33}]
    printf(clock, and(and(and(UInt<1>("h1"), _T_3), _T_4), UInt<1>("h1")), "Assertion failed: WSHR cant pop and push in same cycle\n    at DCacheWSHR.scala:22 assert(!(io.pushReq.valid && io.popReq.valid),\"WSHR cant pop and push in same cycle\")\n") : printf @[DCacheWSHR.scala 22:9]
    assert(clock, _T_1, and(and(UInt<1>("h1"), _T_3), UInt<1>("h1")), "") : assert @[DCacheWSHR.scala 22:9]
    printf(clock, and(and(and(UInt<1>("h1"), _T_17), _T_18), UInt<1>("h1")), "Assertion failed: WSHR dont store same bA in 2 entries\n    at DCacheWSHR.scala:32 assert(PopCount(pushMatchMask) <= 1.U,\"WSHR dont store same bA in 2 entries\")\n") : printf_1 @[DCacheWSHR.scala 32:9]
    assert(clock, _T_15, and(and(UInt<1>("h1"), _T_17), UInt<1>("h1")), "") : assert_1 @[DCacheWSHR.scala 32:9]

  module Queue :
    input clock : Clock
    input reset : UInt<1>
    output io_enq_ready : UInt<1>
    input io_enq_valid : UInt<1>
    input io_enq_bits_instrId : UInt<3>
    input io_enq_bits_opcode : UInt<3>
    input io_enq_bits_param : UInt<4>
    input io_enq_bits_tag : UInt<26>
    input io_enq_bits_setIdx : UInt<2>
    input io_enq_bits_perLaneAddr_0_activeMask : UInt<1>
    input io_enq_bits_perLaneAddr_0_blockOffset : UInt<2>
    input io_enq_bits_perLaneAddr_0_wordOffset1H : UInt<4>
    input io_enq_bits_perLaneAddr_1_activeMask : UInt<1>
    input io_enq_bits_perLaneAddr_1_blockOffset : UInt<2>
    input io_enq_bits_perLaneAddr_1_wordOffset1H : UInt<4>
    input io_enq_bits_perLaneAddr_2_activeMask : UInt<1>
    input io_enq_bits_perLaneAddr_2_blockOffset : UInt<2>
    input io_enq_bits_perLaneAddr_2_wordOffset1H : UInt<4>
    input io_enq_bits_perLaneAddr_3_activeMask : UInt<1>
    input io_enq_bits_perLaneAddr_3_blockOffset : UInt<2>
    input io_enq_bits_perLaneAddr_3_wordOffset1H : UInt<4>
    input io_enq_bits_data_0 : UInt<32>
    input io_enq_bits_data_1 : UInt<32>
    input io_enq_bits_data_2 : UInt<32>
    input io_enq_bits_data_3 : UInt<32>
    input io_deq_ready : UInt<1>
    output io_deq_valid : UInt<1>
    output io_deq_bits_instrId : UInt<3>
    output io_deq_bits_opcode : UInt<3>
    output io_deq_bits_param : UInt<4>
    output io_deq_bits_tag : UInt<26>
    output io_deq_bits_setIdx : UInt<2>
    output io_deq_bits_perLaneAddr_0_activeMask : UInt<1>
    output io_deq_bits_perLaneAddr_0_blockOffset : UInt<2>
    output io_deq_bits_perLaneAddr_0_wordOffset1H : UInt<4>
    output io_deq_bits_perLaneAddr_1_activeMask : UInt<1>
    output io_deq_bits_perLaneAddr_1_blockOffset : UInt<2>
    output io_deq_bits_perLaneAddr_1_wordOffset1H : UInt<4>
    output io_deq_bits_perLaneAddr_2_activeMask : UInt<1>
    output io_deq_bits_perLaneAddr_2_blockOffset : UInt<2>
    output io_deq_bits_perLaneAddr_2_wordOffset1H : UInt<4>
    output io_deq_bits_perLaneAddr_3_activeMask : UInt<1>
    output io_deq_bits_perLaneAddr_3_blockOffset : UInt<2>
    output io_deq_bits_perLaneAddr_3_wordOffset1H : UInt<4>
    output io_deq_bits_data_0 : UInt<32>
    output io_deq_bits_data_1 : UInt<32>
    output io_deq_bits_data_2 : UInt<32>
    output io_deq_bits_data_3 : UInt<32>
    output io_count : UInt<1>

    mem ram_instrId : @[Decoupled.scala 259:95]
      data-type => UInt<3>
      depth => 1
      read-latency => 0
      write-latency => 1
      reader => io_deq_bits_MPORT
      writer => MPORT
      read-under-write => undefined
    mem ram_opcode : @[Decoupled.scala 259:95]
      data-type => UInt<3>
      depth => 1
      read-latency => 0
      write-latency => 1
      reader => io_deq_bits_MPORT
      writer => MPORT
      read-under-write => undefined
    mem ram_param : @[Decoupled.scala 259:95]
      data-type => UInt<4>
      depth => 1
      read-latency => 0
      write-latency => 1
      reader => io_deq_bits_MPORT
      writer => MPORT
      read-under-write => undefined
    mem ram_tag : @[Decoupled.scala 259:95]
      data-type => UInt<26>
      depth => 1
      read-latency => 0
      write-latency => 1
      reader => io_deq_bits_MPORT
      writer => MPORT
      read-under-write => undefined
    mem ram_setIdx : @[Decoupled.scala 259:95]
      data-type => UInt<2>
      depth => 1
      read-latency => 0
      write-latency => 1
      reader => io_deq_bits_MPORT
      writer => MPORT
      read-under-write => undefined
    mem ram_perLaneAddr_0_activeMask : @[Decoupled.scala 259:95]
      data-type => UInt<1>
      depth => 1
      read-latency => 0
      write-latency => 1
      reader => io_deq_bits_MPORT
      writer => MPORT
      read-under-write => undefined
    mem ram_perLaneAddr_0_blockOffset : @[Decoupled.scala 259:95]
      data-type => UInt<2>
      depth => 1
      read-latency => 0
      write-latency => 1
      reader => io_deq_bits_MPORT
      writer => MPORT
      read-under-write => undefined
    mem ram_perLaneAddr_0_wordOffset1H : @[Decoupled.scala 259:95]
      data-type => UInt<4>
      depth => 1
      read-latency => 0
      write-latency => 1
      reader => io_deq_bits_MPORT
      writer => MPORT
      read-under-write => undefined
    mem ram_perLaneAddr_1_activeMask : @[Decoupled.scala 259:95]
      data-type => UInt<1>
      depth => 1
      read-latency => 0
      write-latency => 1
      reader => io_deq_bits_MPORT
      writer => MPORT
      read-under-write => undefined
    mem ram_perLaneAddr_1_blockOffset : @[Decoupled.scala 259:95]
      data-type => UInt<2>
      depth => 1
      read-latency => 0
      write-latency => 1
      reader => io_deq_bits_MPORT
      writer => MPORT
      read-under-write => undefined
    mem ram_perLaneAddr_1_wordOffset1H : @[Decoupled.scala 259:95]
      data-type => UInt<4>
      depth => 1
      read-latency => 0
      write-latency => 1
      reader => io_deq_bits_MPORT
      writer => MPORT
      read-under-write => undefined
    mem ram_perLaneAddr_2_activeMask : @[Decoupled.scala 259:95]
      data-type => UInt<1>
      depth => 1
      read-latency => 0
      write-latency => 1
      reader => io_deq_bits_MPORT
      writer => MPORT
      read-under-write => undefined
    mem ram_perLaneAddr_2_blockOffset : @[Decoupled.scala 259:95]
      data-type => UInt<2>
      depth => 1
      read-latency => 0
      write-latency => 1
      reader => io_deq_bits_MPORT
      writer => MPORT
      read-under-write => undefined
    mem ram_perLaneAddr_2_wordOffset1H : @[Decoupled.scala 259:95]
      data-type => UInt<4>
      depth => 1
      read-latency => 0
      write-latency => 1
      reader => io_deq_bits_MPORT
      writer => MPORT
      read-under-write => undefined
    mem ram_perLaneAddr_3_activeMask : @[Decoupled.scala 259:95]
      data-type => UInt<1>
      depth => 1
      read-latency => 0
      write-latency => 1
      reader => io_deq_bits_MPORT
      writer => MPORT
      read-under-write => undefined
    mem ram_perLaneAddr_3_blockOffset : @[Decoupled.scala 259:95]
      data-type => UInt<2>
      depth => 1
      read-latency => 0
      write-latency => 1
      reader => io_deq_bits_MPORT
      writer => MPORT
      read-under-write => undefined
    mem ram_perLaneAddr_3_wordOffset1H : @[Decoupled.scala 259:95]
      data-type => UInt<4>
      depth => 1
      read-latency => 0
      write-latency => 1
      reader => io_deq_bits_MPORT
      writer => MPORT
      read-under-write => undefined
    mem ram_data_0 : @[Decoupled.scala 259:95]
      data-type => UInt<32>
      depth => 1
      read-latency => 0
      write-latency => 1
      reader => io_deq_bits_MPORT
      writer => MPORT
      read-under-write => undefined
    mem ram_data_1 : @[Decoupled.scala 259:95]
      data-type => UInt<32>
      depth => 1
      read-latency => 0
      write-latency => 1
      reader => io_deq_bits_MPORT
      writer => MPORT
      read-under-write => undefined
    mem ram_data_2 : @[Decoupled.scala 259:95]
      data-type => UInt<32>
      depth => 1
      read-latency => 0
      write-latency => 1
      reader => io_deq_bits_MPORT
      writer => MPORT
      read-under-write => undefined
    mem ram_data_3 : @[Decoupled.scala 259:95]
      data-type => UInt<32>
      depth => 1
      read-latency => 0
      write-latency => 1
      reader => io_deq_bits_MPORT
      writer => MPORT
      read-under-write => undefined
    reg maybe_full : UInt<1>, clock with :
      reset => (UInt<1>("h0"), maybe_full) @[Decoupled.scala 262:27]
    node _GEN_26 = mux(UInt<1>("h0"), UInt<1>("h0"), UInt<1>("h0")) @[Decoupled.scala 282:15 Counter.scala 99:11]
    node enq_ptr_value = _GEN_26
    node deq_ptr_value = _GEN_26
    node ptr_match = eq(enq_ptr_value, deq_ptr_value) @[Decoupled.scala 263:33]
    node _empty_T = eq(maybe_full, UInt<1>("h0")) @[Decoupled.scala 264:28]
    node empty = and(ptr_match, _empty_T) @[Decoupled.scala 264:25]
    node full = and(ptr_match, maybe_full) @[Decoupled.scala 265:24]
    node _do_enq_T = and(io_enq_ready, io_enq_valid) @[Decoupled.scala 50:35]
    node _do_deq_T = and(io_deq_ready, io_deq_valid) @[Decoupled.scala 50:35]
    node do_enq = _do_enq_T
    node _GEN_0 = validif(do_enq, UInt<1>("h0")) @[Decoupled.scala 272:16 273:8]
    node _GEN_1 = validif(do_enq, clock) @[Decoupled.scala 272:16 273:8]
    node _GEN_2 = mux(do_enq, UInt<1>("h1"), UInt<1>("h0")) @[Decoupled.scala 272:16 273:8 259:95]
    node _GEN_3 = validif(do_enq, UInt<1>("h1")) @[Decoupled.scala 272:16 273:24]
    node _GEN_4 = validif(do_enq, io_enq_bits_instrId) @[Decoupled.scala 272:16 273:24]
    node _GEN_5 = validif(do_enq, io_enq_bits_opcode) @[Decoupled.scala 272:16 273:24]
    node _GEN_6 = validif(do_enq, io_enq_bits_param) @[Decoupled.scala 272:16 273:24]
    node _GEN_7 = validif(do_enq, io_enq_bits_tag) @[Decoupled.scala 272:16 273:24]
    node _GEN_8 = validif(do_enq, io_enq_bits_setIdx) @[Decoupled.scala 272:16 273:24]
    node _GEN_9 = validif(do_enq, io_enq_bits_perLaneAddr_0_activeMask) @[Decoupled.scala 272:16 273:24]
    node _GEN_10 = validif(do_enq, io_enq_bits_perLaneAddr_0_blockOffset) @[Decoupled.scala 272:16 273:24]
    node _GEN_11 = validif(do_enq, io_enq_bits_perLaneAddr_0_wordOffset1H) @[Decoupled.scala 272:16 273:24]
    node _GEN_12 = validif(do_enq, io_enq_bits_perLaneAddr_1_activeMask) @[Decoupled.scala 272:16 273:24]
    node _GEN_13 = validif(do_enq, io_enq_bits_perLaneAddr_1_blockOffset) @[Decoupled.scala 272:16 273:24]
    node _GEN_14 = validif(do_enq, io_enq_bits_perLaneAddr_1_wordOffset1H) @[Decoupled.scala 272:16 273:24]
    node _GEN_15 = validif(do_enq, io_enq_bits_perLaneAddr_2_activeMask) @[Decoupled.scala 272:16 273:24]
    node _GEN_16 = validif(do_enq, io_enq_bits_perLaneAddr_2_blockOffset) @[Decoupled.scala 272:16 273:24]
    node _GEN_17 = validif(do_enq, io_enq_bits_perLaneAddr_2_wordOffset1H) @[Decoupled.scala 272:16 273:24]
    node _GEN_18 = validif(do_enq, io_enq_bits_perLaneAddr_3_activeMask) @[Decoupled.scala 272:16 273:24]
    node _GEN_19 = validif(do_enq, io_enq_bits_perLaneAddr_3_blockOffset) @[Decoupled.scala 272:16 273:24]
    node _GEN_20 = validif(do_enq, io_enq_bits_perLaneAddr_3_wordOffset1H) @[Decoupled.scala 272:16 273:24]
    node _GEN_21 = validif(do_enq, io_enq_bits_data_0) @[Decoupled.scala 272:16 273:24]
    node _GEN_22 = validif(do_enq, io_enq_bits_data_1) @[Decoupled.scala 272:16 273:24]
    node _GEN_23 = validif(do_enq, io_enq_bits_data_2) @[Decoupled.scala 272:16 273:24]
    node _GEN_24 = validif(do_enq, io_enq_bits_data_3) @[Decoupled.scala 272:16 273:24]
    node do_deq = _do_deq_T
    node _T = neq(do_enq, do_deq) @[Decoupled.scala 279:15]
    node _GEN_25 = mux(_T, do_enq, maybe_full) @[Decoupled.scala 279:27 280:16 262:27]
    node _GEN_27 = mux(UInt<1>("h0"), UInt<1>("h0"), _GEN_25) @[Decoupled.scala 282:15 285:16]
    node _io_deq_valid_T = eq(empty, UInt<1>("h0")) @[Decoupled.scala 288:19]
    node _io_enq_ready_T = eq(full, UInt<1>("h0")) @[Decoupled.scala 289:19]
    node _GEN_28 = mux(io_deq_ready, UInt<1>("h1"), _io_enq_ready_T) @[Decoupled.scala 289:16 309:{24,39}]
    node _ptr_diff_T = sub(enq_ptr_value, deq_ptr_value) @[Decoupled.scala 312:32]
    node ptr_diff = tail(_ptr_diff_T, 1) @[Decoupled.scala 312:32]
    node _io_count_T = and(maybe_full, ptr_match) @[Decoupled.scala 315:32]
    node _io_count_T_1 = mux(_io_count_T, UInt<1>("h1"), UInt<1>("h0")) @[Decoupled.scala 315:20]
    node _io_count_T_2 = or(_io_count_T_1, ptr_diff) @[Decoupled.scala 315:62]
    io_enq_ready <= _GEN_28
    io_deq_valid <= _io_deq_valid_T @[Decoupled.scala 288:16]
    io_deq_bits_instrId <= ram_instrId.io_deq_bits_MPORT.data @[Decoupled.scala 296:17]
    io_deq_bits_opcode <= ram_opcode.io_deq_bits_MPORT.data @[Decoupled.scala 296:17]
    io_deq_bits_param <= ram_param.io_deq_bits_MPORT.data @[Decoupled.scala 296:17]
    io_deq_bits_tag <= ram_tag.io_deq_bits_MPORT.data @[Decoupled.scala 296:17]
    io_deq_bits_setIdx <= ram_setIdx.io_deq_bits_MPORT.data @[Decoupled.scala 296:17]
    io_deq_bits_perLaneAddr_0_activeMask <= ram_perLaneAddr_0_activeMask.io_deq_bits_MPORT.data @[Decoupled.scala 296:17]
    io_deq_bits_perLaneAddr_0_blockOffset <= ram_perLaneAddr_0_blockOffset.io_deq_bits_MPORT.data @[Decoupled.scala 296:17]
    io_deq_bits_perLaneAddr_0_wordOffset1H <= ram_perLaneAddr_0_wordOffset1H.io_deq_bits_MPORT.data @[Decoupled.scala 296:17]
    io_deq_bits_perLaneAddr_1_activeMask <= ram_perLaneAddr_1_activeMask.io_deq_bits_MPORT.data @[Decoupled.scala 296:17]
    io_deq_bits_perLaneAddr_1_blockOffset <= ram_perLaneAddr_1_blockOffset.io_deq_bits_MPORT.data @[Decoupled.scala 296:17]
    io_deq_bits_perLaneAddr_1_wordOffset1H <= ram_perLaneAddr_1_wordOffset1H.io_deq_bits_MPORT.data @[Decoupled.scala 296:17]
    io_deq_bits_perLaneAddr_2_activeMask <= ram_perLaneAddr_2_activeMask.io_deq_bits_MPORT.data @[Decoupled.scala 296:17]
    io_deq_bits_perLaneAddr_2_blockOffset <= ram_perLaneAddr_2_blockOffset.io_deq_bits_MPORT.data @[Decoupled.scala 296:17]
    io_deq_bits_perLaneAddr_2_wordOffset1H <= ram_perLaneAddr_2_wordOffset1H.io_deq_bits_MPORT.data @[Decoupled.scala 296:17]
    io_deq_bits_perLaneAddr_3_activeMask <= ram_perLaneAddr_3_activeMask.io_deq_bits_MPORT.data @[Decoupled.scala 296:17]
    io_deq_bits_perLaneAddr_3_blockOffset <= ram_perLaneAddr_3_blockOffset.io_deq_bits_MPORT.data @[Decoupled.scala 296:17]
    io_deq_bits_perLaneAddr_3_wordOffset1H <= ram_perLaneAddr_3_wordOffset1H.io_deq_bits_MPORT.data @[Decoupled.scala 296:17]
    io_deq_bits_data_0 <= ram_data_0.io_deq_bits_MPORT.data @[Decoupled.scala 296:17]
    io_deq_bits_data_1 <= ram_data_1.io_deq_bits_MPORT.data @[Decoupled.scala 296:17]
    io_deq_bits_data_2 <= ram_data_2.io_deq_bits_MPORT.data @[Decoupled.scala 296:17]
    io_deq_bits_data_3 <= ram_data_3.io_deq_bits_MPORT.data @[Decoupled.scala 296:17]
    io_count <= _io_count_T_2 @[Decoupled.scala 315:14]
    ram_instrId.io_deq_bits_MPORT.addr <= UInt<1>("h0") @[Decoupled.scala 296:23]
    ram_opcode.io_deq_bits_MPORT.addr <= UInt<1>("h0") @[Decoupled.scala 296:23]
    ram_param.io_deq_bits_MPORT.addr <= UInt<1>("h0") @[Decoupled.scala 296:23]
    ram_tag.io_deq_bits_MPORT.addr <= UInt<1>("h0") @[Decoupled.scala 296:23]
    ram_setIdx.io_deq_bits_MPORT.addr <= UInt<1>("h0") @[Decoupled.scala 296:23]
    ram_perLaneAddr_0_activeMask.io_deq_bits_MPORT.addr <= UInt<1>("h0") @[Decoupled.scala 296:23]
    ram_perLaneAddr_0_blockOffset.io_deq_bits_MPORT.addr <= UInt<1>("h0") @[Decoupled.scala 296:23]
    ram_perLaneAddr_0_wordOffset1H.io_deq_bits_MPORT.addr <= UInt<1>("h0") @[Decoupled.scala 296:23]
    ram_perLaneAddr_1_activeMask.io_deq_bits_MPORT.addr <= UInt<1>("h0") @[Decoupled.scala 296:23]
    ram_perLaneAddr_1_blockOffset.io_deq_bits_MPORT.addr <= UInt<1>("h0") @[Decoupled.scala 296:23]
    ram_perLaneAddr_1_wordOffset1H.io_deq_bits_MPORT.addr <= UInt<1>("h0") @[Decoupled.scala 296:23]
    ram_perLaneAddr_2_activeMask.io_deq_bits_MPORT.addr <= UInt<1>("h0") @[Decoupled.scala 296:23]
    ram_perLaneAddr_2_blockOffset.io_deq_bits_MPORT.addr <= UInt<1>("h0") @[Decoupled.scala 296:23]
    ram_perLaneAddr_2_wordOffset1H.io_deq_bits_MPORT.addr <= UInt<1>("h0") @[Decoupled.scala 296:23]
    ram_perLaneAddr_3_activeMask.io_deq_bits_MPORT.addr <= UInt<1>("h0") @[Decoupled.scala 296:23]
    ram_perLaneAddr_3_blockOffset.io_deq_bits_MPORT.addr <= UInt<1>("h0") @[Decoupled.scala 296:23]
    ram_perLaneAddr_3_wordOffset1H.io_deq_bits_MPORT.addr <= UInt<1>("h0") @[Decoupled.scala 296:23]
    ram_data_0.io_deq_bits_MPORT.addr <= UInt<1>("h0") @[Decoupled.scala 296:23]
    ram_data_1.io_deq_bits_MPORT.addr <= UInt<1>("h0") @[Decoupled.scala 296:23]
    ram_data_2.io_deq_bits_MPORT.addr <= UInt<1>("h0") @[Decoupled.scala 296:23]
    ram_data_3.io_deq_bits_MPORT.addr <= UInt<1>("h0") @[Decoupled.scala 296:23]
    ram_instrId.io_deq_bits_MPORT.en <= UInt<1>("h1") @[Decoupled.scala 296:23]
    ram_opcode.io_deq_bits_MPORT.en <= UInt<1>("h1") @[Decoupled.scala 296:23]
    ram_param.io_deq_bits_MPORT.en <= UInt<1>("h1") @[Decoupled.scala 296:23]
    ram_tag.io_deq_bits_MPORT.en <= UInt<1>("h1") @[Decoupled.scala 296:23]
    ram_setIdx.io_deq_bits_MPORT.en <= UInt<1>("h1") @[Decoupled.scala 296:23]
    ram_perLaneAddr_0_activeMask.io_deq_bits_MPORT.en <= UInt<1>("h1") @[Decoupled.scala 296:23]
    ram_perLaneAddr_0_blockOffset.io_deq_bits_MPORT.en <= UInt<1>("h1") @[Decoupled.scala 296:23]
    ram_perLaneAddr_0_wordOffset1H.io_deq_bits_MPORT.en <= UInt<1>("h1") @[Decoupled.scala 296:23]
    ram_perLaneAddr_1_activeMask.io_deq_bits_MPORT.en <= UInt<1>("h1") @[Decoupled.scala 296:23]
    ram_perLaneAddr_1_blockOffset.io_deq_bits_MPORT.en <= UInt<1>("h1") @[Decoupled.scala 296:23]
    ram_perLaneAddr_1_wordOffset1H.io_deq_bits_MPORT.en <= UInt<1>("h1") @[Decoupled.scala 296:23]
    ram_perLaneAddr_2_activeMask.io_deq_bits_MPORT.en <= UInt<1>("h1") @[Decoupled.scala 296:23]
    ram_perLaneAddr_2_blockOffset.io_deq_bits_MPORT.en <= UInt<1>("h1") @[Decoupled.scala 296:23]
    ram_perLaneAddr_2_wordOffset1H.io_deq_bits_MPORT.en <= UInt<1>("h1") @[Decoupled.scala 296:23]
    ram_perLaneAddr_3_activeMask.io_deq_bits_MPORT.en <= UInt<1>("h1") @[Decoupled.scala 296:23]
    ram_perLaneAddr_3_blockOffset.io_deq_bits_MPORT.en <= UInt<1>("h1") @[Decoupled.scala 296:23]
    ram_perLaneAddr_3_wordOffset1H.io_deq_bits_MPORT.en <= UInt<1>("h1") @[Decoupled.scala 296:23]
    ram_data_0.io_deq_bits_MPORT.en <= UInt<1>("h1") @[Decoupled.scala 296:23]
    ram_data_1.io_deq_bits_MPORT.en <= UInt<1>("h1") @[Decoupled.scala 296:23]
    ram_data_2.io_deq_bits_MPORT.en <= UInt<1>("h1") @[Decoupled.scala 296:23]
    ram_data_3.io_deq_bits_MPORT.en <= UInt<1>("h1") @[Decoupled.scala 296:23]
    ram_instrId.io_deq_bits_MPORT.clk <= clock @[Decoupled.scala 296:23]
    ram_opcode.io_deq_bits_MPORT.clk <= clock @[Decoupled.scala 296:23]
    ram_param.io_deq_bits_MPORT.clk <= clock @[Decoupled.scala 296:23]
    ram_tag.io_deq_bits_MPORT.clk <= clock @[Decoupled.scala 296:23]
    ram_setIdx.io_deq_bits_MPORT.clk <= clock @[Decoupled.scala 296:23]
    ram_perLaneAddr_0_activeMask.io_deq_bits_MPORT.clk <= clock @[Decoupled.scala 296:23]
    ram_perLaneAddr_0_blockOffset.io_deq_bits_MPORT.clk <= clock @[Decoupled.scala 296:23]
    ram_perLaneAddr_0_wordOffset1H.io_deq_bits_MPORT.clk <= clock @[Decoupled.scala 296:23]
    ram_perLaneAddr_1_activeMask.io_deq_bits_MPORT.clk <= clock @[Decoupled.scala 296:23]
    ram_perLaneAddr_1_blockOffset.io_deq_bits_MPORT.clk <= clock @[Decoupled.scala 296:23]
    ram_perLaneAddr_1_wordOffset1H.io_deq_bits_MPORT.clk <= clock @[Decoupled.scala 296:23]
    ram_perLaneAddr_2_activeMask.io_deq_bits_MPORT.clk <= clock @[Decoupled.scala 296:23]
    ram_perLaneAddr_2_blockOffset.io_deq_bits_MPORT.clk <= clock @[Decoupled.scala 296:23]
    ram_perLaneAddr_2_wordOffset1H.io_deq_bits_MPORT.clk <= clock @[Decoupled.scala 296:23]
    ram_perLaneAddr_3_activeMask.io_deq_bits_MPORT.clk <= clock @[Decoupled.scala 296:23]
    ram_perLaneAddr_3_blockOffset.io_deq_bits_MPORT.clk <= clock @[Decoupled.scala 296:23]
    ram_perLaneAddr_3_wordOffset1H.io_deq_bits_MPORT.clk <= clock @[Decoupled.scala 296:23]
    ram_data_0.io_deq_bits_MPORT.clk <= clock @[Decoupled.scala 296:23]
    ram_data_1.io_deq_bits_MPORT.clk <= clock @[Decoupled.scala 296:23]
    ram_data_2.io_deq_bits_MPORT.clk <= clock @[Decoupled.scala 296:23]
    ram_data_3.io_deq_bits_MPORT.clk <= clock @[Decoupled.scala 296:23]
    ram_instrId.MPORT.addr <= _GEN_0
    ram_opcode.MPORT.addr <= _GEN_0
    ram_param.MPORT.addr <= _GEN_0
    ram_tag.MPORT.addr <= _GEN_0
    ram_setIdx.MPORT.addr <= _GEN_0
    ram_perLaneAddr_0_activeMask.MPORT.addr <= _GEN_0
    ram_perLaneAddr_0_blockOffset.MPORT.addr <= _GEN_0
    ram_perLaneAddr_0_wordOffset1H.MPORT.addr <= _GEN_0
    ram_perLaneAddr_1_activeMask.MPORT.addr <= _GEN_0
    ram_perLaneAddr_1_blockOffset.MPORT.addr <= _GEN_0
    ram_perLaneAddr_1_wordOffset1H.MPORT.addr <= _GEN_0
    ram_perLaneAddr_2_activeMask.MPORT.addr <= _GEN_0
    ram_perLaneAddr_2_blockOffset.MPORT.addr <= _GEN_0
    ram_perLaneAddr_2_wordOffset1H.MPORT.addr <= _GEN_0
    ram_perLaneAddr_3_activeMask.MPORT.addr <= _GEN_0
    ram_perLaneAddr_3_blockOffset.MPORT.addr <= _GEN_0
    ram_perLaneAddr_3_wordOffset1H.MPORT.addr <= _GEN_0
    ram_data_0.MPORT.addr <= _GEN_0
    ram_data_1.MPORT.addr <= _GEN_0
    ram_data_2.MPORT.addr <= _GEN_0
    ram_data_3.MPORT.addr <= _GEN_0
    ram_instrId.MPORT.en <= _GEN_2
    ram_opcode.MPORT.en <= _GEN_2
    ram_param.MPORT.en <= _GEN_2
    ram_tag.MPORT.en <= _GEN_2
    ram_setIdx.MPORT.en <= _GEN_2
    ram_perLaneAddr_0_activeMask.MPORT.en <= _GEN_2
    ram_perLaneAddr_0_blockOffset.MPORT.en <= _GEN_2
    ram_perLaneAddr_0_wordOffset1H.MPORT.en <= _GEN_2
    ram_perLaneAddr_1_activeMask.MPORT.en <= _GEN_2
    ram_perLaneAddr_1_blockOffset.MPORT.en <= _GEN_2
    ram_perLaneAddr_1_wordOffset1H.MPORT.en <= _GEN_2
    ram_perLaneAddr_2_activeMask.MPORT.en <= _GEN_2
    ram_perLaneAddr_2_blockOffset.MPORT.en <= _GEN_2
    ram_perLaneAddr_2_wordOffset1H.MPORT.en <= _GEN_2
    ram_perLaneAddr_3_activeMask.MPORT.en <= _GEN_2
    ram_perLaneAddr_3_blockOffset.MPORT.en <= _GEN_2
    ram_perLaneAddr_3_wordOffset1H.MPORT.en <= _GEN_2
    ram_data_0.MPORT.en <= _GEN_2
    ram_data_1.MPORT.en <= _GEN_2
    ram_data_2.MPORT.en <= _GEN_2
    ram_data_3.MPORT.en <= _GEN_2
    ram_instrId.MPORT.clk <= _GEN_1
    ram_opcode.MPORT.clk <= _GEN_1
    ram_param.MPORT.clk <= _GEN_1
    ram_tag.MPORT.clk <= _GEN_1
    ram_setIdx.MPORT.clk <= _GEN_1
    ram_perLaneAddr_0_activeMask.MPORT.clk <= _GEN_1
    ram_perLaneAddr_0_blockOffset.MPORT.clk <= _GEN_1
    ram_perLaneAddr_0_wordOffset1H.MPORT.clk <= _GEN_1
    ram_perLaneAddr_1_activeMask.MPORT.clk <= _GEN_1
    ram_perLaneAddr_1_blockOffset.MPORT.clk <= _GEN_1
    ram_perLaneAddr_1_wordOffset1H.MPORT.clk <= _GEN_1
    ram_perLaneAddr_2_activeMask.MPORT.clk <= _GEN_1
    ram_perLaneAddr_2_blockOffset.MPORT.clk <= _GEN_1
    ram_perLaneAddr_2_wordOffset1H.MPORT.clk <= _GEN_1
    ram_perLaneAddr_3_activeMask.MPORT.clk <= _GEN_1
    ram_perLaneAddr_3_blockOffset.MPORT.clk <= _GEN_1
    ram_perLaneAddr_3_wordOffset1H.MPORT.clk <= _GEN_1
    ram_data_0.MPORT.clk <= _GEN_1
    ram_data_1.MPORT.clk <= _GEN_1
    ram_data_2.MPORT.clk <= _GEN_1
    ram_data_3.MPORT.clk <= _GEN_1
    ram_instrId.MPORT.data <= _GEN_4
    ram_opcode.MPORT.data <= _GEN_5
    ram_param.MPORT.data <= _GEN_6
    ram_tag.MPORT.data <= _GEN_7
    ram_setIdx.MPORT.data <= _GEN_8
    ram_perLaneAddr_0_activeMask.MPORT.data <= _GEN_9
    ram_perLaneAddr_0_blockOffset.MPORT.data <= _GEN_10
    ram_perLaneAddr_0_wordOffset1H.MPORT.data <= _GEN_11
    ram_perLaneAddr_1_activeMask.MPORT.data <= _GEN_12
    ram_perLaneAddr_1_blockOffset.MPORT.data <= _GEN_13
    ram_perLaneAddr_1_wordOffset1H.MPORT.data <= _GEN_14
    ram_perLaneAddr_2_activeMask.MPORT.data <= _GEN_15
    ram_perLaneAddr_2_blockOffset.MPORT.data <= _GEN_16
    ram_perLaneAddr_2_wordOffset1H.MPORT.data <= _GEN_17
    ram_perLaneAddr_3_activeMask.MPORT.data <= _GEN_18
    ram_perLaneAddr_3_blockOffset.MPORT.data <= _GEN_19
    ram_perLaneAddr_3_wordOffset1H.MPORT.data <= _GEN_20
    ram_data_0.MPORT.data <= _GEN_21
    ram_data_1.MPORT.data <= _GEN_22
    ram_data_2.MPORT.data <= _GEN_23
    ram_data_3.MPORT.data <= _GEN_24
    ram_instrId.MPORT.mask <= _GEN_3
    ram_opcode.MPORT.mask <= _GEN_3
    ram_param.MPORT.mask <= _GEN_3
    ram_tag.MPORT.mask <= _GEN_3
    ram_setIdx.MPORT.mask <= _GEN_3
    ram_perLaneAddr_0_activeMask.MPORT.mask <= _GEN_3
    ram_perLaneAddr_0_blockOffset.MPORT.mask <= _GEN_3
    ram_perLaneAddr_0_wordOffset1H.MPORT.mask <= _GEN_3
    ram_perLaneAddr_1_activeMask.MPORT.mask <= _GEN_3
    ram_perLaneAddr_1_blockOffset.MPORT.mask <= _GEN_3
    ram_perLaneAddr_1_wordOffset1H.MPORT.mask <= _GEN_3
    ram_perLaneAddr_2_activeMask.MPORT.mask <= _GEN_3
    ram_perLaneAddr_2_blockOffset.MPORT.mask <= _GEN_3
    ram_perLaneAddr_2_wordOffset1H.MPORT.mask <= _GEN_3
    ram_perLaneAddr_3_activeMask.MPORT.mask <= _GEN_3
    ram_perLaneAddr_3_blockOffset.MPORT.mask <= _GEN_3
    ram_perLaneAddr_3_wordOffset1H.MPORT.mask <= _GEN_3
    ram_data_0.MPORT.mask <= _GEN_3
    ram_data_1.MPORT.mask <= _GEN_3
    ram_data_2.MPORT.mask <= _GEN_3
    ram_data_3.MPORT.mask <= _GEN_3
    maybe_full <= mux(reset, UInt<1>("h0"), _GEN_27) @[Decoupled.scala 262:{27,27}]

  module Queue_1 :
    input clock : Clock
    input reset : UInt<1>
    output io_enq_ready : UInt<1>
    input io_enq_valid : UInt<1>
    input io_enq_bits_instrId : UInt<3>
    input io_enq_bits_isWrite : UInt<1>
    input io_enq_bits_data_0 : UInt<32>
    input io_enq_bits_data_1 : UInt<32>
    input io_enq_bits_data_2 : UInt<32>
    input io_enq_bits_data_3 : UInt<32>
    input io_enq_bits_activeMask_0 : UInt<1>
    input io_enq_bits_activeMask_1 : UInt<1>
    input io_enq_bits_activeMask_2 : UInt<1>
    input io_enq_bits_activeMask_3 : UInt<1>
    input io_deq_ready : UInt<1>
    output io_deq_valid : UInt<1>
    output io_deq_bits_instrId : UInt<3>
    output io_deq_bits_isWrite : UInt<1>
    output io_deq_bits_data_0 : UInt<32>
    output io_deq_bits_data_1 : UInt<32>
    output io_deq_bits_data_2 : UInt<32>
    output io_deq_bits_data_3 : UInt<32>
    output io_deq_bits_activeMask_0 : UInt<1>
    output io_deq_bits_activeMask_1 : UInt<1>
    output io_deq_bits_activeMask_2 : UInt<1>
    output io_deq_bits_activeMask_3 : UInt<1>
    output io_count : UInt<3>

    mem ram_instrId : @[Decoupled.scala 259:95]
      data-type => UInt<3>
      depth => 4
      read-latency => 0
      write-latency => 1
      reader => io_deq_bits_MPORT
      writer => MPORT
      read-under-write => undefined
    mem ram_isWrite : @[Decoupled.scala 259:95]
      data-type => UInt<1>
      depth => 4
      read-latency => 0
      write-latency => 1
      reader => io_deq_bits_MPORT
      writer => MPORT
      read-under-write => undefined
    mem ram_data_0 : @[Decoupled.scala 259:95]
      data-type => UInt<32>
      depth => 4
      read-latency => 0
      write-latency => 1
      reader => io_deq_bits_MPORT
      writer => MPORT
      read-under-write => undefined
    mem ram_data_1 : @[Decoupled.scala 259:95]
      data-type => UInt<32>
      depth => 4
      read-latency => 0
      write-latency => 1
      reader => io_deq_bits_MPORT
      writer => MPORT
      read-under-write => undefined
    mem ram_data_2 : @[Decoupled.scala 259:95]
      data-type => UInt<32>
      depth => 4
      read-latency => 0
      write-latency => 1
      reader => io_deq_bits_MPORT
      writer => MPORT
      read-under-write => undefined
    mem ram_data_3 : @[Decoupled.scala 259:95]
      data-type => UInt<32>
      depth => 4
      read-latency => 0
      write-latency => 1
      reader => io_deq_bits_MPORT
      writer => MPORT
      read-under-write => undefined
    mem ram_activeMask_0 : @[Decoupled.scala 259:95]
      data-type => UInt<1>
      depth => 4
      read-latency => 0
      write-latency => 1
      reader => io_deq_bits_MPORT
      writer => MPORT
      read-under-write => undefined
    mem ram_activeMask_1 : @[Decoupled.scala 259:95]
      data-type => UInt<1>
      depth => 4
      read-latency => 0
      write-latency => 1
      reader => io_deq_bits_MPORT
      writer => MPORT
      read-under-write => undefined
    mem ram_activeMask_2 : @[Decoupled.scala 259:95]
      data-type => UInt<1>
      depth => 4
      read-latency => 0
      write-latency => 1
      reader => io_deq_bits_MPORT
      writer => MPORT
      read-under-write => undefined
    mem ram_activeMask_3 : @[Decoupled.scala 259:95]
      data-type => UInt<1>
      depth => 4
      read-latency => 0
      write-latency => 1
      reader => io_deq_bits_MPORT
      writer => MPORT
      read-under-write => undefined
    reg enq_ptr_value : UInt<2>, clock with :
      reset => (UInt<1>("h0"), enq_ptr_value) @[Counter.scala 62:40]
    reg deq_ptr_value : UInt<2>, clock with :
      reset => (UInt<1>("h0"), deq_ptr_value) @[Counter.scala 62:40]
    reg maybe_full : UInt<1>, clock with :
      reset => (UInt<1>("h0"), maybe_full) @[Decoupled.scala 262:27]
    node ptr_match = eq(enq_ptr_value, deq_ptr_value) @[Decoupled.scala 263:33]
    node _empty_T = eq(maybe_full, UInt<1>("h0")) @[Decoupled.scala 264:28]
    node empty = and(ptr_match, _empty_T) @[Decoupled.scala 264:25]
    node full = and(ptr_match, maybe_full) @[Decoupled.scala 265:24]
    node _do_enq_T = and(io_enq_ready, io_enq_valid) @[Decoupled.scala 50:35]
    node _do_deq_T = and(io_deq_ready, io_deq_valid) @[Decoupled.scala 50:35]
    node wrap = eq(enq_ptr_value, UInt<2>("h3")) @[Counter.scala 74:24]
    node _value_T = add(enq_ptr_value, UInt<1>("h1")) @[Counter.scala 78:24]
    node _value_T_1 = tail(_value_T, 1) @[Counter.scala 78:24]
    node do_enq = _do_enq_T
    node _GEN_0 = validif(do_enq, enq_ptr_value) @[Decoupled.scala 272:16 273:8]
    node _GEN_1 = validif(do_enq, clock) @[Decoupled.scala 272:16 273:8]
    node _GEN_2 = mux(do_enq, UInt<1>("h1"), UInt<1>("h0")) @[Decoupled.scala 272:16 273:8 259:95]
    node _GEN_3 = validif(do_enq, UInt<1>("h1")) @[Decoupled.scala 272:16 273:24]
    node _GEN_4 = validif(do_enq, io_enq_bits_instrId) @[Decoupled.scala 272:16 273:24]
    node _GEN_5 = validif(do_enq, io_enq_bits_isWrite) @[Decoupled.scala 272:16 273:24]
    node _GEN_6 = validif(do_enq, io_enq_bits_data_0) @[Decoupled.scala 272:16 273:24]
    node _GEN_7 = validif(do_enq, io_enq_bits_data_1) @[Decoupled.scala 272:16 273:24]
    node _GEN_8 = validif(do_enq, io_enq_bits_data_2) @[Decoupled.scala 272:16 273:24]
    node _GEN_9 = validif(do_enq, io_enq_bits_data_3) @[Decoupled.scala 272:16 273:24]
    node _GEN_10 = validif(do_enq, io_enq_bits_activeMask_0) @[Decoupled.scala 272:16 273:24]
    node _GEN_11 = validif(do_enq, io_enq_bits_activeMask_1) @[Decoupled.scala 272:16 273:24]
    node _GEN_12 = validif(do_enq, io_enq_bits_activeMask_2) @[Decoupled.scala 272:16 273:24]
    node _GEN_13 = validif(do_enq, io_enq_bits_activeMask_3) @[Decoupled.scala 272:16 273:24]
    node _GEN_14 = mux(do_enq, _value_T_1, enq_ptr_value) @[Decoupled.scala 272:16 Counter.scala 78:15 62:40]
    node wrap_1 = eq(deq_ptr_value, UInt<2>("h3")) @[Counter.scala 74:24]
    node _value_T_2 = add(deq_ptr_value, UInt<1>("h1")) @[Counter.scala 78:24]
    node _value_T_3 = tail(_value_T_2, 1) @[Counter.scala 78:24]
    node do_deq = _do_deq_T
    node _GEN_15 = mux(do_deq, _value_T_3, deq_ptr_value) @[Decoupled.scala 276:16 Counter.scala 78:15 62:40]
    node _T = neq(do_enq, do_deq) @[Decoupled.scala 279:15]
    node _GEN_16 = mux(_T, do_enq, maybe_full) @[Decoupled.scala 279:27 280:16 262:27]
    node _GEN_17 = mux(UInt<1>("h0"), UInt<1>("h0"), _GEN_14) @[Decoupled.scala 282:15 Counter.scala 99:11]
    node _GEN_18 = mux(UInt<1>("h0"), UInt<1>("h0"), _GEN_15) @[Decoupled.scala 282:15 Counter.scala 99:11]
    node _GEN_19 = mux(UInt<1>("h0"), UInt<1>("h0"), _GEN_16) @[Decoupled.scala 282:15 285:16]
    node _io_deq_valid_T = eq(empty, UInt<1>("h0")) @[Decoupled.scala 288:19]
    node _io_enq_ready_T = eq(full, UInt<1>("h0")) @[Decoupled.scala 289:19]
    node _ptr_diff_T = sub(enq_ptr_value, deq_ptr_value) @[Decoupled.scala 312:32]
    node ptr_diff = tail(_ptr_diff_T, 1) @[Decoupled.scala 312:32]
    node _io_count_T = and(maybe_full, ptr_match) @[Decoupled.scala 315:32]
    node _io_count_T_1 = mux(_io_count_T, UInt<3>("h4"), UInt<1>("h0")) @[Decoupled.scala 315:20]
    node _io_count_T_2 = or(_io_count_T_1, ptr_diff) @[Decoupled.scala 315:62]
    io_enq_ready <= _io_enq_ready_T @[Decoupled.scala 289:16]
    io_deq_valid <= _io_deq_valid_T @[Decoupled.scala 288:16]
    io_deq_bits_instrId <= ram_instrId.io_deq_bits_MPORT.data @[Decoupled.scala 296:17]
    io_deq_bits_isWrite <= ram_isWrite.io_deq_bits_MPORT.data @[Decoupled.scala 296:17]
    io_deq_bits_data_0 <= ram_data_0.io_deq_bits_MPORT.data @[Decoupled.scala 296:17]
    io_deq_bits_data_1 <= ram_data_1.io_deq_bits_MPORT.data @[Decoupled.scala 296:17]
    io_deq_bits_data_2 <= ram_data_2.io_deq_bits_MPORT.data @[Decoupled.scala 296:17]
    io_deq_bits_data_3 <= ram_data_3.io_deq_bits_MPORT.data @[Decoupled.scala 296:17]
    io_deq_bits_activeMask_0 <= ram_activeMask_0.io_deq_bits_MPORT.data @[Decoupled.scala 296:17]
    io_deq_bits_activeMask_1 <= ram_activeMask_1.io_deq_bits_MPORT.data @[Decoupled.scala 296:17]
    io_deq_bits_activeMask_2 <= ram_activeMask_2.io_deq_bits_MPORT.data @[Decoupled.scala 296:17]
    io_deq_bits_activeMask_3 <= ram_activeMask_3.io_deq_bits_MPORT.data @[Decoupled.scala 296:17]
    io_count <= _io_count_T_2 @[Decoupled.scala 315:14]
    ram_instrId.io_deq_bits_MPORT.addr <= deq_ptr_value @[Decoupled.scala 296:23]
    ram_isWrite.io_deq_bits_MPORT.addr <= deq_ptr_value @[Decoupled.scala 296:23]
    ram_data_0.io_deq_bits_MPORT.addr <= deq_ptr_value @[Decoupled.scala 296:23]
    ram_data_1.io_deq_bits_MPORT.addr <= deq_ptr_value @[Decoupled.scala 296:23]
    ram_data_2.io_deq_bits_MPORT.addr <= deq_ptr_value @[Decoupled.scala 296:23]
    ram_data_3.io_deq_bits_MPORT.addr <= deq_ptr_value @[Decoupled.scala 296:23]
    ram_activeMask_0.io_deq_bits_MPORT.addr <= deq_ptr_value @[Decoupled.scala 296:23]
    ram_activeMask_1.io_deq_bits_MPORT.addr <= deq_ptr_value @[Decoupled.scala 296:23]
    ram_activeMask_2.io_deq_bits_MPORT.addr <= deq_ptr_value @[Decoupled.scala 296:23]
    ram_activeMask_3.io_deq_bits_MPORT.addr <= deq_ptr_value @[Decoupled.scala 296:23]
    ram_instrId.io_deq_bits_MPORT.en <= UInt<1>("h1") @[Decoupled.scala 296:23]
    ram_isWrite.io_deq_bits_MPORT.en <= UInt<1>("h1") @[Decoupled.scala 296:23]
    ram_data_0.io_deq_bits_MPORT.en <= UInt<1>("h1") @[Decoupled.scala 296:23]
    ram_data_1.io_deq_bits_MPORT.en <= UInt<1>("h1") @[Decoupled.scala 296:23]
    ram_data_2.io_deq_bits_MPORT.en <= UInt<1>("h1") @[Decoupled.scala 296:23]
    ram_data_3.io_deq_bits_MPORT.en <= UInt<1>("h1") @[Decoupled.scala 296:23]
    ram_activeMask_0.io_deq_bits_MPORT.en <= UInt<1>("h1") @[Decoupled.scala 296:23]
    ram_activeMask_1.io_deq_bits_MPORT.en <= UInt<1>("h1") @[Decoupled.scala 296:23]
    ram_activeMask_2.io_deq_bits_MPORT.en <= UInt<1>("h1") @[Decoupled.scala 296:23]
    ram_activeMask_3.io_deq_bits_MPORT.en <= UInt<1>("h1") @[Decoupled.scala 296:23]
    ram_instrId.io_deq_bits_MPORT.clk <= clock @[Decoupled.scala 296:23]
    ram_isWrite.io_deq_bits_MPORT.clk <= clock @[Decoupled.scala 296:23]
    ram_data_0.io_deq_bits_MPORT.clk <= clock @[Decoupled.scala 296:23]
    ram_data_1.io_deq_bits_MPORT.clk <= clock @[Decoupled.scala 296:23]
    ram_data_2.io_deq_bits_MPORT.clk <= clock @[Decoupled.scala 296:23]
    ram_data_3.io_deq_bits_MPORT.clk <= clock @[Decoupled.scala 296:23]
    ram_activeMask_0.io_deq_bits_MPORT.clk <= clock @[Decoupled.scala 296:23]
    ram_activeMask_1.io_deq_bits_MPORT.clk <= clock @[Decoupled.scala 296:23]
    ram_activeMask_2.io_deq_bits_MPORT.clk <= clock @[Decoupled.scala 296:23]
    ram_activeMask_3.io_deq_bits_MPORT.clk <= clock @[Decoupled.scala 296:23]
    ram_instrId.MPORT.addr <= _GEN_0
    ram_isWrite.MPORT.addr <= _GEN_0
    ram_data_0.MPORT.addr <= _GEN_0
    ram_data_1.MPORT.addr <= _GEN_0
    ram_data_2.MPORT.addr <= _GEN_0
    ram_data_3.MPORT.addr <= _GEN_0
    ram_activeMask_0.MPORT.addr <= _GEN_0
    ram_activeMask_1.MPORT.addr <= _GEN_0
    ram_activeMask_2.MPORT.addr <= _GEN_0
    ram_activeMask_3.MPORT.addr <= _GEN_0
    ram_instrId.MPORT.en <= _GEN_2
    ram_isWrite.MPORT.en <= _GEN_2
    ram_data_0.MPORT.en <= _GEN_2
    ram_data_1.MPORT.en <= _GEN_2
    ram_data_2.MPORT.en <= _GEN_2
    ram_data_3.MPORT.en <= _GEN_2
    ram_activeMask_0.MPORT.en <= _GEN_2
    ram_activeMask_1.MPORT.en <= _GEN_2
    ram_activeMask_2.MPORT.en <= _GEN_2
    ram_activeMask_3.MPORT.en <= _GEN_2
    ram_instrId.MPORT.clk <= _GEN_1
    ram_isWrite.MPORT.clk <= _GEN_1
    ram_data_0.MPORT.clk <= _GEN_1
    ram_data_1.MPORT.clk <= _GEN_1
    ram_data_2.MPORT.clk <= _GEN_1
    ram_data_3.MPORT.clk <= _GEN_1
    ram_activeMask_0.MPORT.clk <= _GEN_1
    ram_activeMask_1.MPORT.clk <= _GEN_1
    ram_activeMask_2.MPORT.clk <= _GEN_1
    ram_activeMask_3.MPORT.clk <= _GEN_1
    ram_instrId.MPORT.data <= _GEN_4
    ram_isWrite.MPORT.data <= _GEN_5
    ram_data_0.MPORT.data <= _GEN_6
    ram_data_1.MPORT.data <= _GEN_7
    ram_data_2.MPORT.data <= _GEN_8
    ram_data_3.MPORT.data <= _GEN_9
    ram_activeMask_0.MPORT.data <= _GEN_10
    ram_activeMask_1.MPORT.data <= _GEN_11
    ram_activeMask_2.MPORT.data <= _GEN_12
    ram_activeMask_3.MPORT.data <= _GEN_13
    ram_instrId.MPORT.mask <= _GEN_3
    ram_isWrite.MPORT.mask <= _GEN_3
    ram_data_0.MPORT.mask <= _GEN_3
    ram_data_1.MPORT.mask <= _GEN_3
    ram_data_2.MPORT.mask <= _GEN_3
    ram_data_3.MPORT.mask <= _GEN_3
    ram_activeMask_0.MPORT.mask <= _GEN_3
    ram_activeMask_1.MPORT.mask <= _GEN_3
    ram_activeMask_2.MPORT.mask <= _GEN_3
    ram_activeMask_3.MPORT.mask <= _GEN_3
    enq_ptr_value <= mux(reset, UInt<2>("h0"), _GEN_17) @[Counter.scala 62:{40,40}]
    deq_ptr_value <= mux(reset, UInt<2>("h0"), _GEN_18) @[Counter.scala 62:{40,40}]
    maybe_full <= mux(reset, UInt<1>("h0"), _GEN_19) @[Decoupled.scala 262:{27,27}]

  module Queue_2 :
    input clock : Clock
    input reset : UInt<1>
    output io_enq_ready : UInt<1>
    input io_enq_valid : UInt<1>
    input io_enq_bits_d_opcode : UInt<3>
    input io_enq_bits_d_source : UInt<7>
    input io_enq_bits_d_addr : UInt<32>
    input io_enq_bits_d_data_0 : UInt<32>
    input io_enq_bits_d_data_1 : UInt<32>
    input io_enq_bits_d_data_2 : UInt<32>
    input io_enq_bits_d_data_3 : UInt<32>
    input io_deq_ready : UInt<1>
    output io_deq_valid : UInt<1>
    output io_deq_bits_d_opcode : UInt<3>
    output io_deq_bits_d_source : UInt<7>
    output io_deq_bits_d_addr : UInt<32>
    output io_deq_bits_d_data_0 : UInt<32>
    output io_deq_bits_d_data_1 : UInt<32>
    output io_deq_bits_d_data_2 : UInt<32>
    output io_deq_bits_d_data_3 : UInt<32>
    output io_count : UInt<2>

    mem ram_d_opcode : @[Decoupled.scala 259:95]
      data-type => UInt<3>
      depth => 2
      read-latency => 0
      write-latency => 1
      reader => io_deq_bits_MPORT
      writer => MPORT
      read-under-write => undefined
    mem ram_d_source : @[Decoupled.scala 259:95]
      data-type => UInt<7>
      depth => 2
      read-latency => 0
      write-latency => 1
      reader => io_deq_bits_MPORT
      writer => MPORT
      read-under-write => undefined
    mem ram_d_addr : @[Decoupled.scala 259:95]
      data-type => UInt<32>
      depth => 2
      read-latency => 0
      write-latency => 1
      reader => io_deq_bits_MPORT
      writer => MPORT
      read-under-write => undefined
    mem ram_d_data_0 : @[Decoupled.scala 259:95]
      data-type => UInt<32>
      depth => 2
      read-latency => 0
      write-latency => 1
      reader => io_deq_bits_MPORT
      writer => MPORT
      read-under-write => undefined
    mem ram_d_data_1 : @[Decoupled.scala 259:95]
      data-type => UInt<32>
      depth => 2
      read-latency => 0
      write-latency => 1
      reader => io_deq_bits_MPORT
      writer => MPORT
      read-under-write => undefined
    mem ram_d_data_2 : @[Decoupled.scala 259:95]
      data-type => UInt<32>
      depth => 2
      read-latency => 0
      write-latency => 1
      reader => io_deq_bits_MPORT
      writer => MPORT
      read-under-write => undefined
    mem ram_d_data_3 : @[Decoupled.scala 259:95]
      data-type => UInt<32>
      depth => 2
      read-latency => 0
      write-latency => 1
      reader => io_deq_bits_MPORT
      writer => MPORT
      read-under-write => undefined
    reg enq_ptr_value : UInt<1>, clock with :
      reset => (UInt<1>("h0"), enq_ptr_value) @[Counter.scala 62:40]
    reg deq_ptr_value : UInt<1>, clock with :
      reset => (UInt<1>("h0"), deq_ptr_value) @[Counter.scala 62:40]
    reg maybe_full : UInt<1>, clock with :
      reset => (UInt<1>("h0"), maybe_full) @[Decoupled.scala 262:27]
    node ptr_match = eq(enq_ptr_value, deq_ptr_value) @[Decoupled.scala 263:33]
    node _empty_T = eq(maybe_full, UInt<1>("h0")) @[Decoupled.scala 264:28]
    node empty = and(ptr_match, _empty_T) @[Decoupled.scala 264:25]
    node full = and(ptr_match, maybe_full) @[Decoupled.scala 265:24]
    node _do_enq_T = and(io_enq_ready, io_enq_valid) @[Decoupled.scala 50:35]
    node _do_deq_T = and(io_deq_ready, io_deq_valid) @[Decoupled.scala 50:35]
    node wrap = eq(enq_ptr_value, UInt<1>("h1")) @[Counter.scala 74:24]
    node _value_T = add(enq_ptr_value, UInt<1>("h1")) @[Counter.scala 78:24]
    node _value_T_1 = tail(_value_T, 1) @[Counter.scala 78:24]
    node do_enq = _do_enq_T
    node _GEN_0 = validif(do_enq, enq_ptr_value) @[Decoupled.scala 272:16 273:8]
    node _GEN_1 = validif(do_enq, clock) @[Decoupled.scala 272:16 273:8]
    node _GEN_2 = mux(do_enq, UInt<1>("h1"), UInt<1>("h0")) @[Decoupled.scala 272:16 273:8 259:95]
    node _GEN_3 = validif(do_enq, UInt<1>("h1")) @[Decoupled.scala 272:16 273:24]
    node _GEN_4 = validif(do_enq, io_enq_bits_d_opcode) @[Decoupled.scala 272:16 273:24]
    node _GEN_5 = validif(do_enq, io_enq_bits_d_source) @[Decoupled.scala 272:16 273:24]
    node _GEN_6 = validif(do_enq, io_enq_bits_d_addr) @[Decoupled.scala 272:16 273:24]
    node _GEN_7 = validif(do_enq, io_enq_bits_d_data_0) @[Decoupled.scala 272:16 273:24]
    node _GEN_8 = validif(do_enq, io_enq_bits_d_data_1) @[Decoupled.scala 272:16 273:24]
    node _GEN_9 = validif(do_enq, io_enq_bits_d_data_2) @[Decoupled.scala 272:16 273:24]
    node _GEN_10 = validif(do_enq, io_enq_bits_d_data_3) @[Decoupled.scala 272:16 273:24]
    node _GEN_11 = mux(do_enq, _value_T_1, enq_ptr_value) @[Decoupled.scala 272:16 Counter.scala 78:15 62:40]
    node wrap_1 = eq(deq_ptr_value, UInt<1>("h1")) @[Counter.scala 74:24]
    node _value_T_2 = add(deq_ptr_value, UInt<1>("h1")) @[Counter.scala 78:24]
    node _value_T_3 = tail(_value_T_2, 1) @[Counter.scala 78:24]
    node do_deq = _do_deq_T
    node _GEN_12 = mux(do_deq, _value_T_3, deq_ptr_value) @[Decoupled.scala 276:16 Counter.scala 78:15 62:40]
    node _T = neq(do_enq, do_deq) @[Decoupled.scala 279:15]
    node _GEN_13 = mux(_T, do_enq, maybe_full) @[Decoupled.scala 279:27 280:16 262:27]
    node _GEN_14 = mux(UInt<1>("h0"), UInt<1>("h0"), _GEN_11) @[Decoupled.scala 282:15 Counter.scala 99:11]
    node _GEN_15 = mux(UInt<1>("h0"), UInt<1>("h0"), _GEN_12) @[Decoupled.scala 282:15 Counter.scala 99:11]
    node _GEN_16 = mux(UInt<1>("h0"), UInt<1>("h0"), _GEN_13) @[Decoupled.scala 282:15 285:16]
    node _io_deq_valid_T = eq(empty, UInt<1>("h0")) @[Decoupled.scala 288:19]
    node _io_enq_ready_T = eq(full, UInt<1>("h0")) @[Decoupled.scala 289:19]
    node _ptr_diff_T = sub(enq_ptr_value, deq_ptr_value) @[Decoupled.scala 312:32]
    node ptr_diff = tail(_ptr_diff_T, 1) @[Decoupled.scala 312:32]
    node _io_count_T = and(maybe_full, ptr_match) @[Decoupled.scala 315:32]
    node _io_count_T_1 = mux(_io_count_T, UInt<2>("h2"), UInt<1>("h0")) @[Decoupled.scala 315:20]
    node _io_count_T_2 = or(_io_count_T_1, ptr_diff) @[Decoupled.scala 315:62]
    io_enq_ready <= _io_enq_ready_T @[Decoupled.scala 289:16]
    io_deq_valid <= _io_deq_valid_T @[Decoupled.scala 288:16]
    io_deq_bits_d_opcode <= ram_d_opcode.io_deq_bits_MPORT.data @[Decoupled.scala 296:17]
    io_deq_bits_d_source <= ram_d_source.io_deq_bits_MPORT.data @[Decoupled.scala 296:17]
    io_deq_bits_d_addr <= ram_d_addr.io_deq_bits_MPORT.data @[Decoupled.scala 296:17]
    io_deq_bits_d_data_0 <= ram_d_data_0.io_deq_bits_MPORT.data @[Decoupled.scala 296:17]
    io_deq_bits_d_data_1 <= ram_d_data_1.io_deq_bits_MPORT.data @[Decoupled.scala 296:17]
    io_deq_bits_d_data_2 <= ram_d_data_2.io_deq_bits_MPORT.data @[Decoupled.scala 296:17]
    io_deq_bits_d_data_3 <= ram_d_data_3.io_deq_bits_MPORT.data @[Decoupled.scala 296:17]
    io_count <= _io_count_T_2 @[Decoupled.scala 315:14]
    ram_d_opcode.io_deq_bits_MPORT.addr <= deq_ptr_value @[Decoupled.scala 296:23]
    ram_d_source.io_deq_bits_MPORT.addr <= deq_ptr_value @[Decoupled.scala 296:23]
    ram_d_addr.io_deq_bits_MPORT.addr <= deq_ptr_value @[Decoupled.scala 296:23]
    ram_d_data_0.io_deq_bits_MPORT.addr <= deq_ptr_value @[Decoupled.scala 296:23]
    ram_d_data_1.io_deq_bits_MPORT.addr <= deq_ptr_value @[Decoupled.scala 296:23]
    ram_d_data_2.io_deq_bits_MPORT.addr <= deq_ptr_value @[Decoupled.scala 296:23]
    ram_d_data_3.io_deq_bits_MPORT.addr <= deq_ptr_value @[Decoupled.scala 296:23]
    ram_d_opcode.io_deq_bits_MPORT.en <= UInt<1>("h1") @[Decoupled.scala 296:23]
    ram_d_source.io_deq_bits_MPORT.en <= UInt<1>("h1") @[Decoupled.scala 296:23]
    ram_d_addr.io_deq_bits_MPORT.en <= UInt<1>("h1") @[Decoupled.scala 296:23]
    ram_d_data_0.io_deq_bits_MPORT.en <= UInt<1>("h1") @[Decoupled.scala 296:23]
    ram_d_data_1.io_deq_bits_MPORT.en <= UInt<1>("h1") @[Decoupled.scala 296:23]
    ram_d_data_2.io_deq_bits_MPORT.en <= UInt<1>("h1") @[Decoupled.scala 296:23]
    ram_d_data_3.io_deq_bits_MPORT.en <= UInt<1>("h1") @[Decoupled.scala 296:23]
    ram_d_opcode.io_deq_bits_MPORT.clk <= clock @[Decoupled.scala 296:23]
    ram_d_source.io_deq_bits_MPORT.clk <= clock @[Decoupled.scala 296:23]
    ram_d_addr.io_deq_bits_MPORT.clk <= clock @[Decoupled.scala 296:23]
    ram_d_data_0.io_deq_bits_MPORT.clk <= clock @[Decoupled.scala 296:23]
    ram_d_data_1.io_deq_bits_MPORT.clk <= clock @[Decoupled.scala 296:23]
    ram_d_data_2.io_deq_bits_MPORT.clk <= clock @[Decoupled.scala 296:23]
    ram_d_data_3.io_deq_bits_MPORT.clk <= clock @[Decoupled.scala 296:23]
    ram_d_opcode.MPORT.addr <= _GEN_0
    ram_d_source.MPORT.addr <= _GEN_0
    ram_d_addr.MPORT.addr <= _GEN_0
    ram_d_data_0.MPORT.addr <= _GEN_0
    ram_d_data_1.MPORT.addr <= _GEN_0
    ram_d_data_2.MPORT.addr <= _GEN_0
    ram_d_data_3.MPORT.addr <= _GEN_0
    ram_d_opcode.MPORT.en <= _GEN_2
    ram_d_source.MPORT.en <= _GEN_2
    ram_d_addr.MPORT.en <= _GEN_2
    ram_d_data_0.MPORT.en <= _GEN_2
    ram_d_data_1.MPORT.en <= _GEN_2
    ram_d_data_2.MPORT.en <= _GEN_2
    ram_d_data_3.MPORT.en <= _GEN_2
    ram_d_opcode.MPORT.clk <= _GEN_1
    ram_d_source.MPORT.clk <= _GEN_1
    ram_d_addr.MPORT.clk <= _GEN_1
    ram_d_data_0.MPORT.clk <= _GEN_1
    ram_d_data_1.MPORT.clk <= _GEN_1
    ram_d_data_2.MPORT.clk <= _GEN_1
    ram_d_data_3.MPORT.clk <= _GEN_1
    ram_d_opcode.MPORT.data <= _GEN_4
    ram_d_source.MPORT.data <= _GEN_5
    ram_d_addr.MPORT.data <= _GEN_6
    ram_d_data_0.MPORT.data <= _GEN_7
    ram_d_data_1.MPORT.data <= _GEN_8
    ram_d_data_2.MPORT.data <= _GEN_9
    ram_d_data_3.MPORT.data <= _GEN_10
    ram_d_opcode.MPORT.mask <= _GEN_3
    ram_d_source.MPORT.mask <= _GEN_3
    ram_d_addr.MPORT.mask <= _GEN_3
    ram_d_data_0.MPORT.mask <= _GEN_3
    ram_d_data_1.MPORT.mask <= _GEN_3
    ram_d_data_2.MPORT.mask <= _GEN_3
    ram_d_data_3.MPORT.mask <= _GEN_3
    enq_ptr_value <= mux(reset, UInt<1>("h0"), _GEN_14) @[Counter.scala 62:{40,40}]
    deq_ptr_value <= mux(reset, UInt<1>("h0"), _GEN_15) @[Counter.scala 62:{40,40}]
    maybe_full <= mux(reset, UInt<1>("h0"), _GEN_16) @[Decoupled.scala 262:{27,27}]

  module Queue_3 :
    input clock : Clock
    input reset : UInt<1>
    output io_enq_ready : UInt<1>
    input io_enq_valid : UInt<1>
    input io_enq_bits_a_opcode : UInt<3>
    input io_enq_bits_a_param : UInt<3>
    input io_enq_bits_a_addr : UInt<32>
    input io_enq_bits_a_data_0 : UInt<32>
    input io_enq_bits_a_data_1 : UInt<32>
    input io_enq_bits_a_data_2 : UInt<32>
    input io_enq_bits_a_data_3 : UInt<32>
    input io_enq_bits_a_mask_0 : UInt<1>
    input io_enq_bits_a_mask_1 : UInt<1>
    input io_enq_bits_a_mask_2 : UInt<1>
    input io_enq_bits_a_mask_3 : UInt<1>
    input io_enq_bits_a_source : UInt<7>
    input io_enq_bits_hasCoreRsp : UInt<1>
    input io_enq_bits_coreRspInstrId : UInt<32>
    input io_deq_ready : UInt<1>
    output io_deq_valid : UInt<1>
    output io_deq_bits_a_opcode : UInt<3>
    output io_deq_bits_a_param : UInt<3>
    output io_deq_bits_a_addr : UInt<32>
    output io_deq_bits_a_data_0 : UInt<32>
    output io_deq_bits_a_data_1 : UInt<32>
    output io_deq_bits_a_data_2 : UInt<32>
    output io_deq_bits_a_data_3 : UInt<32>
    output io_deq_bits_a_mask_0 : UInt<1>
    output io_deq_bits_a_mask_1 : UInt<1>
    output io_deq_bits_a_mask_2 : UInt<1>
    output io_deq_bits_a_mask_3 : UInt<1>
    output io_deq_bits_a_source : UInt<7>
    output io_deq_bits_hasCoreRsp : UInt<1>
    output io_deq_bits_coreRspInstrId : UInt<32>
    output io_count : UInt<4>

    mem ram_a_opcode : @[Decoupled.scala 259:95]
      data-type => UInt<3>
      depth => 8
      read-latency => 0
      write-latency => 1
      reader => io_deq_bits_MPORT
      writer => MPORT
      read-under-write => undefined
    mem ram_a_param : @[Decoupled.scala 259:95]
      data-type => UInt<3>
      depth => 8
      read-latency => 0
      write-latency => 1
      reader => io_deq_bits_MPORT
      writer => MPORT
      read-under-write => undefined
    mem ram_a_addr : @[Decoupled.scala 259:95]
      data-type => UInt<32>
      depth => 8
      read-latency => 0
      write-latency => 1
      reader => io_deq_bits_MPORT
      writer => MPORT
      read-under-write => undefined
    mem ram_a_data_0 : @[Decoupled.scala 259:95]
      data-type => UInt<32>
      depth => 8
      read-latency => 0
      write-latency => 1
      reader => io_deq_bits_MPORT
      writer => MPORT
      read-under-write => undefined
    mem ram_a_data_1 : @[Decoupled.scala 259:95]
      data-type => UInt<32>
      depth => 8
      read-latency => 0
      write-latency => 1
      reader => io_deq_bits_MPORT
      writer => MPORT
      read-under-write => undefined
    mem ram_a_data_2 : @[Decoupled.scala 259:95]
      data-type => UInt<32>
      depth => 8
      read-latency => 0
      write-latency => 1
      reader => io_deq_bits_MPORT
      writer => MPORT
      read-under-write => undefined
    mem ram_a_data_3 : @[Decoupled.scala 259:95]
      data-type => UInt<32>
      depth => 8
      read-latency => 0
      write-latency => 1
      reader => io_deq_bits_MPORT
      writer => MPORT
      read-under-write => undefined
    mem ram_a_mask_0 : @[Decoupled.scala 259:95]
      data-type => UInt<1>
      depth => 8
      read-latency => 0
      write-latency => 1
      reader => io_deq_bits_MPORT
      writer => MPORT
      read-under-write => undefined
    mem ram_a_mask_1 : @[Decoupled.scala 259:95]
      data-type => UInt<1>
      depth => 8
      read-latency => 0
      write-latency => 1
      reader => io_deq_bits_MPORT
      writer => MPORT
      read-under-write => undefined
    mem ram_a_mask_2 : @[Decoupled.scala 259:95]
      data-type => UInt<1>
      depth => 8
      read-latency => 0
      write-latency => 1
      reader => io_deq_bits_MPORT
      writer => MPORT
      read-under-write => undefined
    mem ram_a_mask_3 : @[Decoupled.scala 259:95]
      data-type => UInt<1>
      depth => 8
      read-latency => 0
      write-latency => 1
      reader => io_deq_bits_MPORT
      writer => MPORT
      read-under-write => undefined
    mem ram_a_source : @[Decoupled.scala 259:95]
      data-type => UInt<7>
      depth => 8
      read-latency => 0
      write-latency => 1
      reader => io_deq_bits_MPORT
      writer => MPORT
      read-under-write => undefined
    mem ram_hasCoreRsp : @[Decoupled.scala 259:95]
      data-type => UInt<1>
      depth => 8
      read-latency => 0
      write-latency => 1
      reader => io_deq_bits_MPORT
      writer => MPORT
      read-under-write => undefined
    mem ram_coreRspInstrId : @[Decoupled.scala 259:95]
      data-type => UInt<32>
      depth => 8
      read-latency => 0
      write-latency => 1
      reader => io_deq_bits_MPORT
      writer => MPORT
      read-under-write => undefined
    reg enq_ptr_value : UInt<3>, clock with :
      reset => (UInt<1>("h0"), enq_ptr_value) @[Counter.scala 62:40]
    reg deq_ptr_value : UInt<3>, clock with :
      reset => (UInt<1>("h0"), deq_ptr_value) @[Counter.scala 62:40]
    reg maybe_full : UInt<1>, clock with :
      reset => (UInt<1>("h0"), maybe_full) @[Decoupled.scala 262:27]
    node ptr_match = eq(enq_ptr_value, deq_ptr_value) @[Decoupled.scala 263:33]
    node _empty_T = eq(maybe_full, UInt<1>("h0")) @[Decoupled.scala 264:28]
    node empty = and(ptr_match, _empty_T) @[Decoupled.scala 264:25]
    node full = and(ptr_match, maybe_full) @[Decoupled.scala 265:24]
    node _do_enq_T = and(io_enq_ready, io_enq_valid) @[Decoupled.scala 50:35]
    node _do_deq_T = and(io_deq_ready, io_deq_valid) @[Decoupled.scala 50:35]
    node wrap = eq(enq_ptr_value, UInt<3>("h7")) @[Counter.scala 74:24]
    node _value_T = add(enq_ptr_value, UInt<1>("h1")) @[Counter.scala 78:24]
    node _value_T_1 = tail(_value_T, 1) @[Counter.scala 78:24]
    node do_enq = _do_enq_T
    node _GEN_0 = validif(do_enq, enq_ptr_value) @[Decoupled.scala 272:16 273:8]
    node _GEN_1 = validif(do_enq, clock) @[Decoupled.scala 272:16 273:8]
    node _GEN_2 = mux(do_enq, UInt<1>("h1"), UInt<1>("h0")) @[Decoupled.scala 272:16 273:8 259:95]
    node _GEN_3 = validif(do_enq, UInt<1>("h1")) @[Decoupled.scala 272:16 273:24]
    node _GEN_4 = validif(do_enq, io_enq_bits_a_opcode) @[Decoupled.scala 272:16 273:24]
    node _GEN_5 = validif(do_enq, io_enq_bits_a_param) @[Decoupled.scala 272:16 273:24]
    node _GEN_6 = validif(do_enq, io_enq_bits_a_addr) @[Decoupled.scala 272:16 273:24]
    node _GEN_7 = validif(do_enq, io_enq_bits_a_data_0) @[Decoupled.scala 272:16 273:24]
    node _GEN_8 = validif(do_enq, io_enq_bits_a_data_1) @[Decoupled.scala 272:16 273:24]
    node _GEN_9 = validif(do_enq, io_enq_bits_a_data_2) @[Decoupled.scala 272:16 273:24]
    node _GEN_10 = validif(do_enq, io_enq_bits_a_data_3) @[Decoupled.scala 272:16 273:24]
    node _GEN_11 = validif(do_enq, io_enq_bits_a_mask_0) @[Decoupled.scala 272:16 273:24]
    node _GEN_12 = validif(do_enq, io_enq_bits_a_mask_1) @[Decoupled.scala 272:16 273:24]
    node _GEN_13 = validif(do_enq, io_enq_bits_a_mask_2) @[Decoupled.scala 272:16 273:24]
    node _GEN_14 = validif(do_enq, io_enq_bits_a_mask_3) @[Decoupled.scala 272:16 273:24]
    node _GEN_15 = validif(do_enq, io_enq_bits_a_source) @[Decoupled.scala 272:16 273:24]
    node _GEN_16 = validif(do_enq, io_enq_bits_hasCoreRsp) @[Decoupled.scala 272:16 273:24]
    node _GEN_17 = validif(do_enq, io_enq_bits_coreRspInstrId) @[Decoupled.scala 272:16 273:24]
    node _GEN_18 = mux(do_enq, _value_T_1, enq_ptr_value) @[Decoupled.scala 272:16 Counter.scala 78:15 62:40]
    node wrap_1 = eq(deq_ptr_value, UInt<3>("h7")) @[Counter.scala 74:24]
    node _value_T_2 = add(deq_ptr_value, UInt<1>("h1")) @[Counter.scala 78:24]
    node _value_T_3 = tail(_value_T_2, 1) @[Counter.scala 78:24]
    node do_deq = _do_deq_T
    node _GEN_19 = mux(do_deq, _value_T_3, deq_ptr_value) @[Decoupled.scala 276:16 Counter.scala 78:15 62:40]
    node _T = neq(do_enq, do_deq) @[Decoupled.scala 279:15]
    node _GEN_20 = mux(_T, do_enq, maybe_full) @[Decoupled.scala 279:27 280:16 262:27]
    node _GEN_21 = mux(UInt<1>("h0"), UInt<1>("h0"), _GEN_18) @[Decoupled.scala 282:15 Counter.scala 99:11]
    node _GEN_22 = mux(UInt<1>("h0"), UInt<1>("h0"), _GEN_19) @[Decoupled.scala 282:15 Counter.scala 99:11]
    node _GEN_23 = mux(UInt<1>("h0"), UInt<1>("h0"), _GEN_20) @[Decoupled.scala 282:15 285:16]
    node _io_deq_valid_T = eq(empty, UInt<1>("h0")) @[Decoupled.scala 288:19]
    node _io_enq_ready_T = eq(full, UInt<1>("h0")) @[Decoupled.scala 289:19]
    node _ptr_diff_T = sub(enq_ptr_value, deq_ptr_value) @[Decoupled.scala 312:32]
    node ptr_diff = tail(_ptr_diff_T, 1) @[Decoupled.scala 312:32]
    node _io_count_T = and(maybe_full, ptr_match) @[Decoupled.scala 315:32]
    node _io_count_T_1 = mux(_io_count_T, UInt<4>("h8"), UInt<1>("h0")) @[Decoupled.scala 315:20]
    node _io_count_T_2 = or(_io_count_T_1, ptr_diff) @[Decoupled.scala 315:62]
    io_enq_ready <= _io_enq_ready_T @[Decoupled.scala 289:16]
    io_deq_valid <= _io_deq_valid_T @[Decoupled.scala 288:16]
    io_deq_bits_a_opcode <= ram_a_opcode.io_deq_bits_MPORT.data @[Decoupled.scala 296:17]
    io_deq_bits_a_param <= ram_a_param.io_deq_bits_MPORT.data @[Decoupled.scala 296:17]
    io_deq_bits_a_addr <= ram_a_addr.io_deq_bits_MPORT.data @[Decoupled.scala 296:17]
    io_deq_bits_a_data_0 <= ram_a_data_0.io_deq_bits_MPORT.data @[Decoupled.scala 296:17]
    io_deq_bits_a_data_1 <= ram_a_data_1.io_deq_bits_MPORT.data @[Decoupled.scala 296:17]
    io_deq_bits_a_data_2 <= ram_a_data_2.io_deq_bits_MPORT.data @[Decoupled.scala 296:17]
    io_deq_bits_a_data_3 <= ram_a_data_3.io_deq_bits_MPORT.data @[Decoupled.scala 296:17]
    io_deq_bits_a_mask_0 <= ram_a_mask_0.io_deq_bits_MPORT.data @[Decoupled.scala 296:17]
    io_deq_bits_a_mask_1 <= ram_a_mask_1.io_deq_bits_MPORT.data @[Decoupled.scala 296:17]
    io_deq_bits_a_mask_2 <= ram_a_mask_2.io_deq_bits_MPORT.data @[Decoupled.scala 296:17]
    io_deq_bits_a_mask_3 <= ram_a_mask_3.io_deq_bits_MPORT.data @[Decoupled.scala 296:17]
    io_deq_bits_a_source <= ram_a_source.io_deq_bits_MPORT.data @[Decoupled.scala 296:17]
    io_deq_bits_hasCoreRsp <= ram_hasCoreRsp.io_deq_bits_MPORT.data @[Decoupled.scala 296:17]
    io_deq_bits_coreRspInstrId <= ram_coreRspInstrId.io_deq_bits_MPORT.data @[Decoupled.scala 296:17]
    io_count <= _io_count_T_2 @[Decoupled.scala 315:14]
    ram_a_opcode.io_deq_bits_MPORT.addr <= deq_ptr_value @[Decoupled.scala 296:23]
    ram_a_param.io_deq_bits_MPORT.addr <= deq_ptr_value @[Decoupled.scala 296:23]
    ram_a_addr.io_deq_bits_MPORT.addr <= deq_ptr_value @[Decoupled.scala 296:23]
    ram_a_data_0.io_deq_bits_MPORT.addr <= deq_ptr_value @[Decoupled.scala 296:23]
    ram_a_data_1.io_deq_bits_MPORT.addr <= deq_ptr_value @[Decoupled.scala 296:23]
    ram_a_data_2.io_deq_bits_MPORT.addr <= deq_ptr_value @[Decoupled.scala 296:23]
    ram_a_data_3.io_deq_bits_MPORT.addr <= deq_ptr_value @[Decoupled.scala 296:23]
    ram_a_mask_0.io_deq_bits_MPORT.addr <= deq_ptr_value @[Decoupled.scala 296:23]
    ram_a_mask_1.io_deq_bits_MPORT.addr <= deq_ptr_value @[Decoupled.scala 296:23]
    ram_a_mask_2.io_deq_bits_MPORT.addr <= deq_ptr_value @[Decoupled.scala 296:23]
    ram_a_mask_3.io_deq_bits_MPORT.addr <= deq_ptr_value @[Decoupled.scala 296:23]
    ram_a_source.io_deq_bits_MPORT.addr <= deq_ptr_value @[Decoupled.scala 296:23]
    ram_hasCoreRsp.io_deq_bits_MPORT.addr <= deq_ptr_value @[Decoupled.scala 296:23]
    ram_coreRspInstrId.io_deq_bits_MPORT.addr <= deq_ptr_value @[Decoupled.scala 296:23]
    ram_a_opcode.io_deq_bits_MPORT.en <= UInt<1>("h1") @[Decoupled.scala 296:23]
    ram_a_param.io_deq_bits_MPORT.en <= UInt<1>("h1") @[Decoupled.scala 296:23]
    ram_a_addr.io_deq_bits_MPORT.en <= UInt<1>("h1") @[Decoupled.scala 296:23]
    ram_a_data_0.io_deq_bits_MPORT.en <= UInt<1>("h1") @[Decoupled.scala 296:23]
    ram_a_data_1.io_deq_bits_MPORT.en <= UInt<1>("h1") @[Decoupled.scala 296:23]
    ram_a_data_2.io_deq_bits_MPORT.en <= UInt<1>("h1") @[Decoupled.scala 296:23]
    ram_a_data_3.io_deq_bits_MPORT.en <= UInt<1>("h1") @[Decoupled.scala 296:23]
    ram_a_mask_0.io_deq_bits_MPORT.en <= UInt<1>("h1") @[Decoupled.scala 296:23]
    ram_a_mask_1.io_deq_bits_MPORT.en <= UInt<1>("h1") @[Decoupled.scala 296:23]
    ram_a_mask_2.io_deq_bits_MPORT.en <= UInt<1>("h1") @[Decoupled.scala 296:23]
    ram_a_mask_3.io_deq_bits_MPORT.en <= UInt<1>("h1") @[Decoupled.scala 296:23]
    ram_a_source.io_deq_bits_MPORT.en <= UInt<1>("h1") @[Decoupled.scala 296:23]
    ram_hasCoreRsp.io_deq_bits_MPORT.en <= UInt<1>("h1") @[Decoupled.scala 296:23]
    ram_coreRspInstrId.io_deq_bits_MPORT.en <= UInt<1>("h1") @[Decoupled.scala 296:23]
    ram_a_opcode.io_deq_bits_MPORT.clk <= clock @[Decoupled.scala 296:23]
    ram_a_param.io_deq_bits_MPORT.clk <= clock @[Decoupled.scala 296:23]
    ram_a_addr.io_deq_bits_MPORT.clk <= clock @[Decoupled.scala 296:23]
    ram_a_data_0.io_deq_bits_MPORT.clk <= clock @[Decoupled.scala 296:23]
    ram_a_data_1.io_deq_bits_MPORT.clk <= clock @[Decoupled.scala 296:23]
    ram_a_data_2.io_deq_bits_MPORT.clk <= clock @[Decoupled.scala 296:23]
    ram_a_data_3.io_deq_bits_MPORT.clk <= clock @[Decoupled.scala 296:23]
    ram_a_mask_0.io_deq_bits_MPORT.clk <= clock @[Decoupled.scala 296:23]
    ram_a_mask_1.io_deq_bits_MPORT.clk <= clock @[Decoupled.scala 296:23]
    ram_a_mask_2.io_deq_bits_MPORT.clk <= clock @[Decoupled.scala 296:23]
    ram_a_mask_3.io_deq_bits_MPORT.clk <= clock @[Decoupled.scala 296:23]
    ram_a_source.io_deq_bits_MPORT.clk <= clock @[Decoupled.scala 296:23]
    ram_hasCoreRsp.io_deq_bits_MPORT.clk <= clock @[Decoupled.scala 296:23]
    ram_coreRspInstrId.io_deq_bits_MPORT.clk <= clock @[Decoupled.scala 296:23]
    ram_a_opcode.MPORT.addr <= _GEN_0
    ram_a_param.MPORT.addr <= _GEN_0
    ram_a_addr.MPORT.addr <= _GEN_0
    ram_a_data_0.MPORT.addr <= _GEN_0
    ram_a_data_1.MPORT.addr <= _GEN_0
    ram_a_data_2.MPORT.addr <= _GEN_0
    ram_a_data_3.MPORT.addr <= _GEN_0
    ram_a_mask_0.MPORT.addr <= _GEN_0
    ram_a_mask_1.MPORT.addr <= _GEN_0
    ram_a_mask_2.MPORT.addr <= _GEN_0
    ram_a_mask_3.MPORT.addr <= _GEN_0
    ram_a_source.MPORT.addr <= _GEN_0
    ram_hasCoreRsp.MPORT.addr <= _GEN_0
    ram_coreRspInstrId.MPORT.addr <= _GEN_0
    ram_a_opcode.MPORT.en <= _GEN_2
    ram_a_param.MPORT.en <= _GEN_2
    ram_a_addr.MPORT.en <= _GEN_2
    ram_a_data_0.MPORT.en <= _GEN_2
    ram_a_data_1.MPORT.en <= _GEN_2
    ram_a_data_2.MPORT.en <= _GEN_2
    ram_a_data_3.MPORT.en <= _GEN_2
    ram_a_mask_0.MPORT.en <= _GEN_2
    ram_a_mask_1.MPORT.en <= _GEN_2
    ram_a_mask_2.MPORT.en <= _GEN_2
    ram_a_mask_3.MPORT.en <= _GEN_2
    ram_a_source.MPORT.en <= _GEN_2
    ram_hasCoreRsp.MPORT.en <= _GEN_2
    ram_coreRspInstrId.MPORT.en <= _GEN_2
    ram_a_opcode.MPORT.clk <= _GEN_1
    ram_a_param.MPORT.clk <= _GEN_1
    ram_a_addr.MPORT.clk <= _GEN_1
    ram_a_data_0.MPORT.clk <= _GEN_1
    ram_a_data_1.MPORT.clk <= _GEN_1
    ram_a_data_2.MPORT.clk <= _GEN_1
    ram_a_data_3.MPORT.clk <= _GEN_1
    ram_a_mask_0.MPORT.clk <= _GEN_1
    ram_a_mask_1.MPORT.clk <= _GEN_1
    ram_a_mask_2.MPORT.clk <= _GEN_1
    ram_a_mask_3.MPORT.clk <= _GEN_1
    ram_a_source.MPORT.clk <= _GEN_1
    ram_hasCoreRsp.MPORT.clk <= _GEN_1
    ram_coreRspInstrId.MPORT.clk <= _GEN_1
    ram_a_opcode.MPORT.data <= _GEN_4
    ram_a_param.MPORT.data <= _GEN_5
    ram_a_addr.MPORT.data <= _GEN_6
    ram_a_data_0.MPORT.data <= _GEN_7
    ram_a_data_1.MPORT.data <= _GEN_8
    ram_a_data_2.MPORT.data <= _GEN_9
    ram_a_data_3.MPORT.data <= _GEN_10
    ram_a_mask_0.MPORT.data <= _GEN_11
    ram_a_mask_1.MPORT.data <= _GEN_12
    ram_a_mask_2.MPORT.data <= _GEN_13
    ram_a_mask_3.MPORT.data <= _GEN_14
    ram_a_source.MPORT.data <= _GEN_15
    ram_hasCoreRsp.MPORT.data <= _GEN_16
    ram_coreRspInstrId.MPORT.data <= _GEN_17
    ram_a_opcode.MPORT.mask <= _GEN_3
    ram_a_param.MPORT.mask <= _GEN_3
    ram_a_addr.MPORT.mask <= _GEN_3
    ram_a_data_0.MPORT.mask <= _GEN_3
    ram_a_data_1.MPORT.mask <= _GEN_3
    ram_a_data_2.MPORT.mask <= _GEN_3
    ram_a_data_3.MPORT.mask <= _GEN_3
    ram_a_mask_0.MPORT.mask <= _GEN_3
    ram_a_mask_1.MPORT.mask <= _GEN_3
    ram_a_mask_2.MPORT.mask <= _GEN_3
    ram_a_mask_3.MPORT.mask <= _GEN_3
    ram_a_source.MPORT.mask <= _GEN_3
    ram_hasCoreRsp.MPORT.mask <= _GEN_3
    ram_coreRspInstrId.MPORT.mask <= _GEN_3
    enq_ptr_value <= mux(reset, UInt<3>("h0"), _GEN_21) @[Counter.scala 62:{40,40}]
    deq_ptr_value <= mux(reset, UInt<3>("h0"), _GEN_22) @[Counter.scala 62:{40,40}]
    maybe_full <= mux(reset, UInt<1>("h0"), _GEN_23) @[Decoupled.scala 262:{27,27}]

  module Arbiter_2 :
    input clock : Clock
    input reset : UInt<1>
    output io_in_0_ready : UInt<1>
    input io_in_0_valid : UInt<1>
    input io_in_0_bits_a_opcode : UInt<3>
    input io_in_0_bits_a_param : UInt<3>
    input io_in_0_bits_a_addr : UInt<32>
    input io_in_0_bits_a_data_0 : UInt<32>
    input io_in_0_bits_a_data_1 : UInt<32>
    input io_in_0_bits_a_data_2 : UInt<32>
    input io_in_0_bits_a_data_3 : UInt<32>
    input io_in_0_bits_a_mask_0 : UInt<1>
    input io_in_0_bits_a_mask_1 : UInt<1>
    input io_in_0_bits_a_mask_2 : UInt<1>
    input io_in_0_bits_a_mask_3 : UInt<1>
    input io_in_0_bits_a_source : UInt<7>
    input io_in_0_bits_hasCoreRsp : UInt<1>
    input io_in_0_bits_coreRspInstrId : UInt<32>
    output io_in_1_ready : UInt<1>
    input io_in_1_valid : UInt<1>
    input io_in_1_bits_a_opcode : UInt<3>
    input io_in_1_bits_a_param : UInt<3>
    input io_in_1_bits_a_addr : UInt<32>
    input io_in_1_bits_a_data_0 : UInt<32>
    input io_in_1_bits_a_data_1 : UInt<32>
    input io_in_1_bits_a_data_2 : UInt<32>
    input io_in_1_bits_a_data_3 : UInt<32>
    input io_in_1_bits_a_mask_0 : UInt<1>
    input io_in_1_bits_a_mask_1 : UInt<1>
    input io_in_1_bits_a_mask_2 : UInt<1>
    input io_in_1_bits_a_mask_3 : UInt<1>
    input io_in_1_bits_a_source : UInt<7>
    input io_in_1_bits_hasCoreRsp : UInt<1>
    input io_in_1_bits_coreRspInstrId : UInt<32>
    output io_in_2_ready : UInt<1>
    input io_in_2_valid : UInt<1>
    input io_in_2_bits_a_opcode : UInt<3>
    input io_in_2_bits_a_param : UInt<3>
    input io_in_2_bits_a_addr : UInt<32>
    input io_in_2_bits_a_data_0 : UInt<32>
    input io_in_2_bits_a_data_1 : UInt<32>
    input io_in_2_bits_a_data_2 : UInt<32>
    input io_in_2_bits_a_data_3 : UInt<32>
    input io_in_2_bits_a_mask_0 : UInt<1>
    input io_in_2_bits_a_mask_1 : UInt<1>
    input io_in_2_bits_a_mask_2 : UInt<1>
    input io_in_2_bits_a_mask_3 : UInt<1>
    input io_in_2_bits_a_source : UInt<7>
    input io_in_2_bits_hasCoreRsp : UInt<1>
    input io_in_2_bits_coreRspInstrId : UInt<32>
    input io_out_ready : UInt<1>
    output io_out_valid : UInt<1>
    output io_out_bits_a_opcode : UInt<3>
    output io_out_bits_a_param : UInt<3>
    output io_out_bits_a_addr : UInt<32>
    output io_out_bits_a_data_0 : UInt<32>
    output io_out_bits_a_data_1 : UInt<32>
    output io_out_bits_a_data_2 : UInt<32>
    output io_out_bits_a_data_3 : UInt<32>
    output io_out_bits_a_mask_0 : UInt<1>
    output io_out_bits_a_mask_1 : UInt<1>
    output io_out_bits_a_mask_2 : UInt<1>
    output io_out_bits_a_mask_3 : UInt<1>
    output io_out_bits_a_source : UInt<7>
    output io_out_bits_hasCoreRsp : UInt<1>
    output io_out_bits_coreRspInstrId : UInt<32>
    output io_chosen : UInt<2>

    node _GEN_0 = mux(io_in_1_valid, UInt<1>("h1"), UInt<2>("h2")) @[Arbiter.scala 138:13 141:26 142:17]
    node _GEN_1 = mux(io_in_1_valid, io_in_1_bits_a_opcode, io_in_2_bits_a_opcode) @[Arbiter.scala 139:15 141:26 143:19]
    node _GEN_2 = mux(io_in_1_valid, io_in_1_bits_a_param, io_in_2_bits_a_param) @[Arbiter.scala 139:15 141:26 143:19]
    node _GEN_3 = mux(io_in_1_valid, io_in_1_bits_a_addr, io_in_2_bits_a_addr) @[Arbiter.scala 139:15 141:26 143:19]
    node _GEN_4 = mux(io_in_1_valid, io_in_1_bits_a_data_0, io_in_2_bits_a_data_0) @[Arbiter.scala 139:15 141:26 143:19]
    node _GEN_5 = mux(io_in_1_valid, io_in_1_bits_a_data_1, io_in_2_bits_a_data_1) @[Arbiter.scala 139:15 141:26 143:19]
    node _GEN_6 = mux(io_in_1_valid, io_in_1_bits_a_data_2, io_in_2_bits_a_data_2) @[Arbiter.scala 139:15 141:26 143:19]
    node _GEN_7 = mux(io_in_1_valid, io_in_1_bits_a_data_3, io_in_2_bits_a_data_3) @[Arbiter.scala 139:15 141:26 143:19]
    node _GEN_8 = mux(io_in_1_valid, io_in_1_bits_a_mask_0, io_in_2_bits_a_mask_0) @[Arbiter.scala 139:15 141:26 143:19]
    node _GEN_9 = mux(io_in_1_valid, io_in_1_bits_a_mask_1, io_in_2_bits_a_mask_1) @[Arbiter.scala 139:15 141:26 143:19]
    node _GEN_10 = mux(io_in_1_valid, io_in_1_bits_a_mask_2, io_in_2_bits_a_mask_2) @[Arbiter.scala 139:15 141:26 143:19]
    node _GEN_11 = mux(io_in_1_valid, io_in_1_bits_a_mask_3, io_in_2_bits_a_mask_3) @[Arbiter.scala 139:15 141:26 143:19]
    node _GEN_12 = mux(io_in_1_valid, io_in_1_bits_a_source, io_in_2_bits_a_source) @[Arbiter.scala 139:15 141:26 143:19]
    node _GEN_13 = mux(io_in_1_valid, io_in_1_bits_hasCoreRsp, io_in_2_bits_hasCoreRsp) @[Arbiter.scala 139:15 141:26 143:19]
    node _GEN_14 = mux(io_in_1_valid, io_in_1_bits_coreRspInstrId, io_in_2_bits_coreRspInstrId) @[Arbiter.scala 139:15 141:26 143:19]
    node _GEN_15 = mux(io_in_0_valid, UInt<1>("h0"), _GEN_0) @[Arbiter.scala 141:26 142:17]
    node _GEN_16 = mux(io_in_0_valid, io_in_0_bits_a_opcode, _GEN_1) @[Arbiter.scala 141:26 143:19]
    node _GEN_17 = mux(io_in_0_valid, io_in_0_bits_a_param, _GEN_2) @[Arbiter.scala 141:26 143:19]
    node _GEN_18 = mux(io_in_0_valid, io_in_0_bits_a_addr, _GEN_3) @[Arbiter.scala 141:26 143:19]
    node _GEN_19 = mux(io_in_0_valid, io_in_0_bits_a_data_0, _GEN_4) @[Arbiter.scala 141:26 143:19]
    node _GEN_20 = mux(io_in_0_valid, io_in_0_bits_a_data_1, _GEN_5) @[Arbiter.scala 141:26 143:19]
    node _GEN_21 = mux(io_in_0_valid, io_in_0_bits_a_data_2, _GEN_6) @[Arbiter.scala 141:26 143:19]
    node _GEN_22 = mux(io_in_0_valid, io_in_0_bits_a_data_3, _GEN_7) @[Arbiter.scala 141:26 143:19]
    node _GEN_23 = mux(io_in_0_valid, io_in_0_bits_a_mask_0, _GEN_8) @[Arbiter.scala 141:26 143:19]
    node _GEN_24 = mux(io_in_0_valid, io_in_0_bits_a_mask_1, _GEN_9) @[Arbiter.scala 141:26 143:19]
    node _GEN_25 = mux(io_in_0_valid, io_in_0_bits_a_mask_2, _GEN_10) @[Arbiter.scala 141:26 143:19]
    node _GEN_26 = mux(io_in_0_valid, io_in_0_bits_a_mask_3, _GEN_11) @[Arbiter.scala 141:26 143:19]
    node _GEN_27 = mux(io_in_0_valid, io_in_0_bits_a_source, _GEN_12) @[Arbiter.scala 141:26 143:19]
    node _GEN_28 = mux(io_in_0_valid, io_in_0_bits_hasCoreRsp, _GEN_13) @[Arbiter.scala 141:26 143:19]
    node _GEN_29 = mux(io_in_0_valid, io_in_0_bits_coreRspInstrId, _GEN_14) @[Arbiter.scala 141:26 143:19]
    node _grant_T = or(io_in_0_valid, io_in_1_valid) @[Arbiter.scala 46:68]
    node grant_1 = eq(io_in_0_valid, UInt<1>("h0")) @[Arbiter.scala 46:78]
    node grant_2 = eq(_grant_T, UInt<1>("h0")) @[Arbiter.scala 46:78]
    node _io_in_0_ready_T = and(UInt<1>("h1"), io_out_ready) @[Arbiter.scala 149:19]
    node _io_in_1_ready_T = and(grant_1, io_out_ready) @[Arbiter.scala 149:19]
    node _io_in_2_ready_T = and(grant_2, io_out_ready) @[Arbiter.scala 149:19]
    node _io_out_valid_T = eq(grant_2, UInt<1>("h0")) @[Arbiter.scala 150:19]
    node _io_out_valid_T_1 = or(_io_out_valid_T, io_in_2_valid) @[Arbiter.scala 150:31]
    io_in_0_ready <= _io_in_0_ready_T @[Arbiter.scala 149:14]
    io_in_1_ready <= _io_in_1_ready_T @[Arbiter.scala 149:14]
    io_in_2_ready <= _io_in_2_ready_T @[Arbiter.scala 149:14]
    io_out_valid <= _io_out_valid_T_1 @[Arbiter.scala 150:16]
    io_out_bits_a_opcode <= _GEN_16
    io_out_bits_a_param <= _GEN_17
    io_out_bits_a_addr <= _GEN_18
    io_out_bits_a_data_0 <= _GEN_19
    io_out_bits_a_data_1 <= _GEN_20
    io_out_bits_a_data_2 <= _GEN_21
    io_out_bits_a_data_3 <= _GEN_22
    io_out_bits_a_mask_0 <= _GEN_23
    io_out_bits_a_mask_1 <= _GEN_24
    io_out_bits_a_mask_2 <= _GEN_25
    io_out_bits_a_mask_3 <= _GEN_26
    io_out_bits_a_source <= _GEN_27
    io_out_bits_hasCoreRsp <= _GEN_28
    io_out_bits_coreRspInstrId <= _GEN_29
    io_chosen <= _GEN_15

  module genControl :
    input clock : Clock
    input reset : UInt<1>
    input io_opcode : UInt<3>
    input io_param : UInt<4>
    output io_control_isRead : UInt<1>
    output io_control_isWrite : UInt<1>
    output io_control_isLR : UInt<1>
    output io_control_isSC : UInt<1>
    output io_control_isAMO : UInt<1>
    output io_control_isFlush : UInt<1>
    output io_control_isInvalidate : UInt<1>
    output io_control_isWaitMSHR : UInt<1>

    node _T = eq(UInt<1>("h0"), io_opcode) @[DCache.scala 53:20]
    node _T_1 = eq(io_param, UInt<1>("h0")) @[DCache.scala 55:21]
    node _T_2 = eq(io_param, UInt<1>("h1")) @[DCache.scala 57:27]
    node _GEN_0 = mux(_T_2, UInt<1>("h1"), UInt<1>("h0")) @[DCache.scala 46:19 57:36 58:25]
    node _GEN_1 = mux(_T_1, UInt<1>("h1"), UInt<1>("h0")) @[DCache.scala 44:21 55:29 56:27]
    node _GEN_2 = mux(_T_1, UInt<1>("h0"), _GEN_0) @[DCache.scala 46:19 55:29]
    node _T_3 = eq(UInt<1>("h1"), io_opcode) @[DCache.scala 53:20]
    node _T_4 = eq(io_param, UInt<1>("h0")) @[DCache.scala 62:21]
    node _T_5 = eq(io_param, UInt<1>("h1")) @[DCache.scala 64:27]
    node _GEN_3 = mux(_T_5, UInt<1>("h1"), UInt<1>("h0")) @[DCache.scala 47:19 64:36 65:25]
    node _GEN_4 = mux(_T_4, UInt<1>("h1"), UInt<1>("h0")) @[DCache.scala 45:22 62:30 63:28]
    node _GEN_5 = mux(_T_4, UInt<1>("h0"), _GEN_3) @[DCache.scala 47:19 62:30]
    node _T_6 = eq(UInt<2>("h2"), io_opcode) @[DCache.scala 53:20]
    node _T_7 = eq(UInt<2>("h3"), io_opcode) @[DCache.scala 53:20]
    node _T_8 = eq(io_param, UInt<1>("h0")) @[DCache.scala 72:21]
    node _T_9 = eq(io_param, UInt<1>("h1")) @[DCache.scala 74:27]
    node _T_10 = eq(io_param, UInt<2>("h2")) @[DCache.scala 76:27]
    node _GEN_6 = mux(_T_10, UInt<1>("h1"), UInt<1>("h0")) @[DCache.scala 51:25 76:35 77:31]
    node _GEN_7 = mux(_T_9, UInt<1>("h1"), UInt<1>("h0")) @[DCache.scala 49:22 74:36 75:28]
    node _GEN_8 = mux(_T_9, UInt<1>("h0"), _GEN_6) @[DCache.scala 51:25 74:36]
    node _GEN_9 = mux(_T_8, UInt<1>("h1"), UInt<1>("h0")) @[DCache.scala 50:27 72:30 73:33]
    node _GEN_10 = mux(_T_8, UInt<1>("h0"), _GEN_7) @[DCache.scala 49:22 72:30]
    node _GEN_11 = mux(_T_8, UInt<1>("h0"), _GEN_8) @[DCache.scala 51:25 72:30]
    node _GEN_12 = mux(_T_7, _GEN_9, UInt<1>("h0")) @[DCache.scala 53:20 50:27]
    node _GEN_13 = mux(_T_7, _GEN_10, UInt<1>("h0")) @[DCache.scala 53:20 49:22]
    node _GEN_14 = mux(_T_7, _GEN_11, UInt<1>("h0")) @[DCache.scala 53:20 51:25]
    node _GEN_15 = mux(_T_6, UInt<1>("h1"), UInt<1>("h0")) @[DCache.scala 48:20 53:20 69:24]
    node _GEN_16 = mux(_T_6, UInt<1>("h0"), _GEN_12) @[DCache.scala 53:20 50:27]
    node _GEN_17 = mux(_T_6, UInt<1>("h0"), _GEN_13) @[DCache.scala 53:20 49:22]
    node _GEN_18 = mux(_T_6, UInt<1>("h0"), _GEN_14) @[DCache.scala 53:20 51:25]
    node _GEN_19 = mux(_T_3, _GEN_4, UInt<1>("h0")) @[DCache.scala 53:20 45:22]
    node _GEN_20 = mux(_T_3, _GEN_5, UInt<1>("h0")) @[DCache.scala 47:19 53:20]
    node _GEN_21 = mux(_T_3, UInt<1>("h0"), _GEN_15) @[DCache.scala 48:20 53:20]
    node _GEN_22 = mux(_T_3, UInt<1>("h0"), _GEN_16) @[DCache.scala 53:20 50:27]
    node _GEN_23 = mux(_T_3, UInt<1>("h0"), _GEN_17) @[DCache.scala 53:20 49:22]
    node _GEN_24 = mux(_T_3, UInt<1>("h0"), _GEN_18) @[DCache.scala 53:20 51:25]
    node _GEN_25 = mux(_T, _GEN_1, UInt<1>("h0")) @[DCache.scala 53:20 44:21]
    node _GEN_26 = mux(_T, _GEN_2, UInt<1>("h0")) @[DCache.scala 46:19 53:20]
    node _GEN_27 = mux(_T, UInt<1>("h0"), _GEN_19) @[DCache.scala 53:20 45:22]
    node _GEN_28 = mux(_T, UInt<1>("h0"), _GEN_20) @[DCache.scala 47:19 53:20]
    node _GEN_29 = mux(_T, UInt<1>("h0"), _GEN_21) @[DCache.scala 48:20 53:20]
    node _GEN_30 = mux(_T, UInt<1>("h0"), _GEN_22) @[DCache.scala 53:20 50:27]
    node _GEN_31 = mux(_T, UInt<1>("h0"), _GEN_23) @[DCache.scala 53:20 49:22]
    node _GEN_32 = mux(_T, UInt<1>("h0"), _GEN_24) @[DCache.scala 53:20 51:25]
    io_control_isRead <= _GEN_25
    io_control_isWrite <= _GEN_27
    io_control_isLR <= _GEN_26
    io_control_isSC <= _GEN_28
    io_control_isAMO <= _GEN_29
    io_control_isFlush <= _GEN_31
    io_control_isInvalidate <= _GEN_30
    io_control_isWaitMSHR <= _GEN_32

  module getDataAccessBankEn :
    input clock : Clock
    input reset : UInt<1>
    input io_perLaneBlockIdx_0 : UInt<2>
    input io_perLaneBlockIdx_1 : UInt<2>
    input io_perLaneBlockIdx_2 : UInt<2>
    input io_perLaneBlockIdx_3 : UInt<2>
    input io_perLaneValid_0 : UInt<1>
    input io_perLaneValid_1 : UInt<1>
    input io_perLaneValid_2 : UInt<1>
    input io_perLaneValid_3 : UInt<1>
    output io_perBankValid_0 : UInt<1>
    output io_perBankValid_1 : UInt<1>
    output io_perBankValid_2 : UInt<1>
    output io_perBankValid_3 : UInt<1>
    output io_perBankBlockIdx_0 : UInt<2>
    output io_perBankBlockIdx_1 : UInt<2>
    output io_perBankBlockIdx_2 : UInt<2>
    output io_perBankBlockIdx_3 : UInt<2>

    node _blockIdx1H_0_T = dshl(UInt<1>("h1"), io_perLaneBlockIdx_0) @[OneHot.scala 57:35]
    node _blockIdxMasked_0_T = bits(io_perLaneValid_0, 0, 0) @[Bitwise.scala 74:15]
    node _blockIdxMasked_0_T_1 = mux(_blockIdxMasked_0_T, UInt<4>("hf"), UInt<4>("h0")) @[Bitwise.scala 74:12]
    node blockIdx1H_0 = _blockIdx1H_0_T @[DCache.scala 547:24 550:19]
    node _blockIdxMasked_0_T_2 = and(blockIdx1H_0, _blockIdxMasked_0_T_1) @[DCache.scala 551:40]
    node _blockIdx1H_1_T = dshl(UInt<1>("h1"), io_perLaneBlockIdx_1) @[OneHot.scala 57:35]
    node _blockIdxMasked_1_T = bits(io_perLaneValid_1, 0, 0) @[Bitwise.scala 74:15]
    node _blockIdxMasked_1_T_1 = mux(_blockIdxMasked_1_T, UInt<4>("hf"), UInt<4>("h0")) @[Bitwise.scala 74:12]
    node blockIdx1H_1 = _blockIdx1H_1_T @[DCache.scala 547:24 550:19]
    node _blockIdxMasked_1_T_2 = and(blockIdx1H_1, _blockIdxMasked_1_T_1) @[DCache.scala 551:40]
    node _blockIdx1H_2_T = dshl(UInt<1>("h1"), io_perLaneBlockIdx_2) @[OneHot.scala 57:35]
    node _blockIdxMasked_2_T = bits(io_perLaneValid_2, 0, 0) @[Bitwise.scala 74:15]
    node _blockIdxMasked_2_T_1 = mux(_blockIdxMasked_2_T, UInt<4>("hf"), UInt<4>("h0")) @[Bitwise.scala 74:12]
    node blockIdx1H_2 = _blockIdx1H_2_T @[DCache.scala 547:24 550:19]
    node _blockIdxMasked_2_T_2 = and(blockIdx1H_2, _blockIdxMasked_2_T_1) @[DCache.scala 551:40]
    node _blockIdx1H_3_T = dshl(UInt<1>("h1"), io_perLaneBlockIdx_3) @[OneHot.scala 57:35]
    node _blockIdxMasked_3_T = bits(io_perLaneValid_3, 0, 0) @[Bitwise.scala 74:15]
    node _blockIdxMasked_3_T_1 = mux(_blockIdxMasked_3_T, UInt<4>("hf"), UInt<4>("h0")) @[Bitwise.scala 74:12]
    node blockIdx1H_3 = _blockIdx1H_3_T @[DCache.scala 547:24 550:19]
    node _blockIdxMasked_3_T_2 = and(blockIdx1H_3, _blockIdxMasked_3_T_1) @[DCache.scala 551:40]
    node blockIdxMasked_0 = _blockIdxMasked_0_T_2 @[DCache.scala 548:28 551:23]
    node _perBankReq_Bin_0_T = bits(blockIdxMasked_0, 0, 0) @[DCache.scala 555:58]
    node blockIdxMasked_1 = _blockIdxMasked_1_T_2 @[DCache.scala 548:28 551:23]
    node _perBankReq_Bin_0_T_1 = bits(blockIdxMasked_1, 0, 0) @[DCache.scala 555:58]
    node blockIdxMasked_2 = _blockIdxMasked_2_T_2 @[DCache.scala 548:28 551:23]
    node _perBankReq_Bin_0_T_2 = bits(blockIdxMasked_2, 0, 0) @[DCache.scala 555:58]
    node blockIdxMasked_3 = _blockIdxMasked_3_T_2 @[DCache.scala 548:28 551:23]
    node _perBankReq_Bin_0_T_3 = bits(blockIdxMasked_3, 0, 0) @[DCache.scala 555:58]
    node perBankReq_Bin_0_lo = cat(_perBankReq_Bin_0_T_2, _perBankReq_Bin_0_T_3) @[Cat.scala 31:58]
    node perBankReq_Bin_0_hi = cat(_perBankReq_Bin_0_T, _perBankReq_Bin_0_T_1) @[Cat.scala 31:58]
    node _perBankReq_Bin_0_T_4 = cat(perBankReq_Bin_0_hi, perBankReq_Bin_0_lo) @[Cat.scala 31:58]
    node _perBankReq_Bin_0_T_5 = bits(_perBankReq_Bin_0_T_4, 1, 0) @[Bitwise.scala 111:18]
    node _perBankReq_Bin_0_T_6 = bits(_perBankReq_Bin_0_T_5, 0, 0) @[Bitwise.scala 111:18]
    node _perBankReq_Bin_0_T_7 = bits(_perBankReq_Bin_0_T_5, 1, 1) @[Bitwise.scala 111:47]
    node _perBankReq_Bin_0_T_8 = cat(_perBankReq_Bin_0_T_6, _perBankReq_Bin_0_T_7) @[Cat.scala 31:58]
    node _perBankReq_Bin_0_T_9 = bits(_perBankReq_Bin_0_T_4, 3, 2) @[Bitwise.scala 111:47]
    node _perBankReq_Bin_0_T_10 = bits(_perBankReq_Bin_0_T_9, 0, 0) @[Bitwise.scala 111:18]
    node _perBankReq_Bin_0_T_11 = bits(_perBankReq_Bin_0_T_9, 1, 1) @[Bitwise.scala 111:47]
    node _perBankReq_Bin_0_T_12 = cat(_perBankReq_Bin_0_T_10, _perBankReq_Bin_0_T_11) @[Cat.scala 31:58]
    node _perBankReq_Bin_0_T_13 = cat(_perBankReq_Bin_0_T_8, _perBankReq_Bin_0_T_12) @[Cat.scala 31:58]
    node _perBankReq_Bin_1_T = bits(blockIdxMasked_0, 1, 1) @[DCache.scala 555:58]
    node _perBankReq_Bin_1_T_1 = bits(blockIdxMasked_1, 1, 1) @[DCache.scala 555:58]
    node _perBankReq_Bin_1_T_2 = bits(blockIdxMasked_2, 1, 1) @[DCache.scala 555:58]
    node _perBankReq_Bin_1_T_3 = bits(blockIdxMasked_3, 1, 1) @[DCache.scala 555:58]
    node perBankReq_Bin_1_lo = cat(_perBankReq_Bin_1_T_2, _perBankReq_Bin_1_T_3) @[Cat.scala 31:58]
    node perBankReq_Bin_1_hi = cat(_perBankReq_Bin_1_T, _perBankReq_Bin_1_T_1) @[Cat.scala 31:58]
    node _perBankReq_Bin_1_T_4 = cat(perBankReq_Bin_1_hi, perBankReq_Bin_1_lo) @[Cat.scala 31:58]
    node _perBankReq_Bin_1_T_5 = bits(_perBankReq_Bin_1_T_4, 1, 0) @[Bitwise.scala 111:18]
    node _perBankReq_Bin_1_T_6 = bits(_perBankReq_Bin_1_T_5, 0, 0) @[Bitwise.scala 111:18]
    node _perBankReq_Bin_1_T_7 = bits(_perBankReq_Bin_1_T_5, 1, 1) @[Bitwise.scala 111:47]
    node _perBankReq_Bin_1_T_8 = cat(_perBankReq_Bin_1_T_6, _perBankReq_Bin_1_T_7) @[Cat.scala 31:58]
    node _perBankReq_Bin_1_T_9 = bits(_perBankReq_Bin_1_T_4, 3, 2) @[Bitwise.scala 111:47]
    node _perBankReq_Bin_1_T_10 = bits(_perBankReq_Bin_1_T_9, 0, 0) @[Bitwise.scala 111:18]
    node _perBankReq_Bin_1_T_11 = bits(_perBankReq_Bin_1_T_9, 1, 1) @[Bitwise.scala 111:47]
    node _perBankReq_Bin_1_T_12 = cat(_perBankReq_Bin_1_T_10, _perBankReq_Bin_1_T_11) @[Cat.scala 31:58]
    node _perBankReq_Bin_1_T_13 = cat(_perBankReq_Bin_1_T_8, _perBankReq_Bin_1_T_12) @[Cat.scala 31:58]
    node _perBankReq_Bin_2_T = bits(blockIdxMasked_0, 2, 2) @[DCache.scala 555:58]
    node _perBankReq_Bin_2_T_1 = bits(blockIdxMasked_1, 2, 2) @[DCache.scala 555:58]
    node _perBankReq_Bin_2_T_2 = bits(blockIdxMasked_2, 2, 2) @[DCache.scala 555:58]
    node _perBankReq_Bin_2_T_3 = bits(blockIdxMasked_3, 2, 2) @[DCache.scala 555:58]
    node perBankReq_Bin_2_lo = cat(_perBankReq_Bin_2_T_2, _perBankReq_Bin_2_T_3) @[Cat.scala 31:58]
    node perBankReq_Bin_2_hi = cat(_perBankReq_Bin_2_T, _perBankReq_Bin_2_T_1) @[Cat.scala 31:58]
    node _perBankReq_Bin_2_T_4 = cat(perBankReq_Bin_2_hi, perBankReq_Bin_2_lo) @[Cat.scala 31:58]
    node _perBankReq_Bin_2_T_5 = bits(_perBankReq_Bin_2_T_4, 1, 0) @[Bitwise.scala 111:18]
    node _perBankReq_Bin_2_T_6 = bits(_perBankReq_Bin_2_T_5, 0, 0) @[Bitwise.scala 111:18]
    node _perBankReq_Bin_2_T_7 = bits(_perBankReq_Bin_2_T_5, 1, 1) @[Bitwise.scala 111:47]
    node _perBankReq_Bin_2_T_8 = cat(_perBankReq_Bin_2_T_6, _perBankReq_Bin_2_T_7) @[Cat.scala 31:58]
    node _perBankReq_Bin_2_T_9 = bits(_perBankReq_Bin_2_T_4, 3, 2) @[Bitwise.scala 111:47]
    node _perBankReq_Bin_2_T_10 = bits(_perBankReq_Bin_2_T_9, 0, 0) @[Bitwise.scala 111:18]
    node _perBankReq_Bin_2_T_11 = bits(_perBankReq_Bin_2_T_9, 1, 1) @[Bitwise.scala 111:47]
    node _perBankReq_Bin_2_T_12 = cat(_perBankReq_Bin_2_T_10, _perBankReq_Bin_2_T_11) @[Cat.scala 31:58]
    node _perBankReq_Bin_2_T_13 = cat(_perBankReq_Bin_2_T_8, _perBankReq_Bin_2_T_12) @[Cat.scala 31:58]
    node _perBankReq_Bin_3_T = bits(blockIdxMasked_0, 3, 3) @[DCache.scala 555:58]
    node _perBankReq_Bin_3_T_1 = bits(blockIdxMasked_1, 3, 3) @[DCache.scala 555:58]
    node _perBankReq_Bin_3_T_2 = bits(blockIdxMasked_2, 3, 3) @[DCache.scala 555:58]
    node _perBankReq_Bin_3_T_3 = bits(blockIdxMasked_3, 3, 3) @[DCache.scala 555:58]
    node perBankReq_Bin_3_lo = cat(_perBankReq_Bin_3_T_2, _perBankReq_Bin_3_T_3) @[Cat.scala 31:58]
    node perBankReq_Bin_3_hi = cat(_perBankReq_Bin_3_T, _perBankReq_Bin_3_T_1) @[Cat.scala 31:58]
    node _perBankReq_Bin_3_T_4 = cat(perBankReq_Bin_3_hi, perBankReq_Bin_3_lo) @[Cat.scala 31:58]
    node _perBankReq_Bin_3_T_5 = bits(_perBankReq_Bin_3_T_4, 1, 0) @[Bitwise.scala 111:18]
    node _perBankReq_Bin_3_T_6 = bits(_perBankReq_Bin_3_T_5, 0, 0) @[Bitwise.scala 111:18]
    node _perBankReq_Bin_3_T_7 = bits(_perBankReq_Bin_3_T_5, 1, 1) @[Bitwise.scala 111:47]
    node _perBankReq_Bin_3_T_8 = cat(_perBankReq_Bin_3_T_6, _perBankReq_Bin_3_T_7) @[Cat.scala 31:58]
    node _perBankReq_Bin_3_T_9 = bits(_perBankReq_Bin_3_T_4, 3, 2) @[Bitwise.scala 111:47]
    node _perBankReq_Bin_3_T_10 = bits(_perBankReq_Bin_3_T_9, 0, 0) @[Bitwise.scala 111:18]
    node _perBankReq_Bin_3_T_11 = bits(_perBankReq_Bin_3_T_9, 1, 1) @[Bitwise.scala 111:47]
    node _perBankReq_Bin_3_T_12 = cat(_perBankReq_Bin_3_T_10, _perBankReq_Bin_3_T_11) @[Cat.scala 31:58]
    node _perBankReq_Bin_3_T_13 = cat(_perBankReq_Bin_3_T_8, _perBankReq_Bin_3_T_12) @[Cat.scala 31:58]
    node perBankReq_Bin_0 = _perBankReq_Bin_0_T_13 @[DCache.scala 553:39 555:23]
    node _T = orr(perBankReq_Bin_0) @[DCache.scala 557:43]
    node perBankReq_Bin_1 = _perBankReq_Bin_1_T_13 @[DCache.scala 553:39 555:23]
    node _T_1 = orr(perBankReq_Bin_1) @[DCache.scala 557:43]
    node perBankReq_Bin_2 = _perBankReq_Bin_2_T_13 @[DCache.scala 553:39 555:23]
    node _T_2 = orr(perBankReq_Bin_2) @[DCache.scala 557:43]
    node perBankReq_Bin_3 = _perBankReq_Bin_3_T_13 @[DCache.scala 553:39 555:23]
    node _T_3 = orr(perBankReq_Bin_3) @[DCache.scala 557:43]
    node _T_4 = bits(perBankReq_Bin_0, 0, 0) @[OneHot.scala 47:45]
    node _T_5 = bits(perBankReq_Bin_0, 1, 1) @[OneHot.scala 47:45]
    node _T_6 = bits(perBankReq_Bin_0, 2, 2) @[OneHot.scala 47:45]
    node _T_7 = bits(perBankReq_Bin_0, 3, 3) @[OneHot.scala 47:45]
    node _T_8 = mux(_T_6, UInt<2>("h2"), UInt<2>("h3")) @[Mux.scala 47:70]
    node _T_9 = mux(_T_5, UInt<1>("h1"), _T_8) @[Mux.scala 47:70]
    node _T_10 = mux(_T_4, UInt<1>("h0"), _T_9) @[Mux.scala 47:70]
    node _T_11 = bits(perBankReq_Bin_1, 0, 0) @[OneHot.scala 47:45]
    node _T_12 = bits(perBankReq_Bin_1, 1, 1) @[OneHot.scala 47:45]
    node _T_13 = bits(perBankReq_Bin_1, 2, 2) @[OneHot.scala 47:45]
    node _T_14 = bits(perBankReq_Bin_1, 3, 3) @[OneHot.scala 47:45]
    node _T_15 = mux(_T_13, UInt<2>("h2"), UInt<2>("h3")) @[Mux.scala 47:70]
    node _T_16 = mux(_T_12, UInt<1>("h1"), _T_15) @[Mux.scala 47:70]
    node _T_17 = mux(_T_11, UInt<1>("h0"), _T_16) @[Mux.scala 47:70]
    node _T_18 = bits(perBankReq_Bin_2, 0, 0) @[OneHot.scala 47:45]
    node _T_19 = bits(perBankReq_Bin_2, 1, 1) @[OneHot.scala 47:45]
    node _T_20 = bits(perBankReq_Bin_2, 2, 2) @[OneHot.scala 47:45]
    node _T_21 = bits(perBankReq_Bin_2, 3, 3) @[OneHot.scala 47:45]
    node _T_22 = mux(_T_20, UInt<2>("h2"), UInt<2>("h3")) @[Mux.scala 47:70]
    node _T_23 = mux(_T_19, UInt<1>("h1"), _T_22) @[Mux.scala 47:70]
    node _T_24 = mux(_T_18, UInt<1>("h0"), _T_23) @[Mux.scala 47:70]
    node _T_25 = bits(perBankReq_Bin_3, 0, 0) @[OneHot.scala 47:45]
    node _T_26 = bits(perBankReq_Bin_3, 1, 1) @[OneHot.scala 47:45]
    node _T_27 = bits(perBankReq_Bin_3, 2, 2) @[OneHot.scala 47:45]
    node _T_28 = bits(perBankReq_Bin_3, 3, 3) @[OneHot.scala 47:45]
    node _T_29 = mux(_T_27, UInt<2>("h2"), UInt<2>("h3")) @[Mux.scala 47:70]
    node _T_30 = mux(_T_26, UInt<1>("h1"), _T_29) @[Mux.scala 47:70]
    node _T_31 = mux(_T_25, UInt<1>("h0"), _T_30) @[Mux.scala 47:70]
    io_perBankValid_0 <= _T @[DCache.scala 557:19]
    io_perBankValid_1 <= _T_1 @[DCache.scala 557:19]
    io_perBankValid_2 <= _T_2 @[DCache.scala 557:19]
    io_perBankValid_3 <= _T_3 @[DCache.scala 557:19]
    io_perBankBlockIdx_0 <= _T_10 @[DCache.scala 558:22]
    io_perBankBlockIdx_1 <= _T_17 @[DCache.scala 558:22]
    io_perBankBlockIdx_2 <= _T_24 @[DCache.scala 558:22]
    io_perBankBlockIdx_3 <= _T_31 @[DCache.scala 558:22]

  module SRAMTemplate_2 :
    input clock : Clock
    input reset : UInt<1>
    output io_r_req_ready : UInt<1>
    input io_r_req_valid : UInt<1>
    input io_r_req_bits_setIdx : UInt<3>
    output io_r_resp_data_0 : UInt<8>
    output io_r_resp_data_1 : UInt<8>
    output io_r_resp_data_2 : UInt<8>
    output io_r_resp_data_3 : UInt<8>
    output io_w_req_ready : UInt<1>
    input io_w_req_valid : UInt<1>
    input io_w_req_bits_setIdx : UInt<3>
    input io_w_req_bits_data_0 : UInt<8>
    input io_w_req_bits_data_1 : UInt<8>
    input io_w_req_bits_data_2 : UInt<8>
    input io_w_req_bits_data_3 : UInt<8>
    input io_w_req_bits_waymask : UInt<4>

    mem array_0 : @[SRAMTemplate.scala 96:26]
      data-type => UInt<8>
      depth => 8
      read-latency => 1
      write-latency => 1
      reader => raw_rdata
      writer => MPORT
      read-under-write => undefined
    mem array_1 : @[SRAMTemplate.scala 96:26]
      data-type => UInt<8>
      depth => 8
      read-latency => 1
      write-latency => 1
      reader => raw_rdata
      writer => MPORT
      read-under-write => undefined
    mem array_2 : @[SRAMTemplate.scala 96:26]
      data-type => UInt<8>
      depth => 8
      read-latency => 1
      write-latency => 1
      reader => raw_rdata
      writer => MPORT
      read-under-write => undefined
    mem array_3 : @[SRAMTemplate.scala 96:26]
      data-type => UInt<8>
      depth => 8
      read-latency => 1
      write-latency => 1
      reader => raw_rdata
      writer => MPORT
      read-under-write => undefined
    node resetState = UInt<1>("h0")
    node wen = or(io_w_req_valid, resetState) @[SRAMTemplate.scala 108:52]
    node resetSet = UInt<1>("h0")
    node setIdx = mux(resetState, resetSet, io_w_req_bits_setIdx) @[SRAMTemplate.scala 111:19]
    node _wdata_WIRE__0 = UInt<8>("h0") @[SRAMTemplate.scala 112:{51,51}]
    node _wdata_T_0 = mux(resetState, _wdata_WIRE__0, io_w_req_bits_data_0) @[SRAMTemplate.scala 112:26]
    node _wdata_WIRE__1 = UInt<8>("h0") @[SRAMTemplate.scala 112:{51,51}]
    node _wdata_T_1 = mux(resetState, _wdata_WIRE__1, io_w_req_bits_data_1) @[SRAMTemplate.scala 112:26]
    node _wdata_WIRE__2 = UInt<8>("h0") @[SRAMTemplate.scala 112:{51,51}]
    node _wdata_T_2 = mux(resetState, _wdata_WIRE__2, io_w_req_bits_data_2) @[SRAMTemplate.scala 112:26]
    node _wdata_WIRE__3 = UInt<8>("h0") @[SRAMTemplate.scala 112:{51,51}]
    node _wdata_T_3 = mux(resetState, _wdata_WIRE__3, io_w_req_bits_data_3) @[SRAMTemplate.scala 112:26]
    node _waymask_T = mux(UInt<1>("h1"), UInt<4>("hf"), UInt<4>("h0")) @[Bitwise.scala 74:12]
    node waymask = mux(resetState, _waymask_T, io_w_req_bits_waymask) @[SRAMTemplate.scala 113:20]
    node _T = bits(waymask, 0, 0) @[SRAMTemplate.scala 114:51]
    node _T_1 = bits(waymask, 1, 1) @[SRAMTemplate.scala 114:51]
    node _T_2 = bits(waymask, 2, 2) @[SRAMTemplate.scala 114:51]
    node _T_3 = bits(waymask, 3, 3) @[SRAMTemplate.scala 114:51]
    node _T_4 = or(setIdx, UInt<3>("h0"))
    node _T_5 = bits(_T_4, 2, 0)
    node _wdata_WIRE_1 = _wdata_T_0 @[SRAMTemplate.scala 112:{102,102}]
    node wdata_0 = _wdata_WIRE_1 @[SRAMTemplate.scala 112:{22,22}]
    node _GEN_0 = validif(_T, wdata_0)
    node _GEN_1 = mux(_T, UInt<1>("h1"), UInt<1>("h0"))
    node _wdata_WIRE_2 = _wdata_T_1 @[SRAMTemplate.scala 112:{102,102}]
    node wdata_1 = _wdata_WIRE_2 @[SRAMTemplate.scala 112:{22,22}]
    node _GEN_2 = validif(_T_1, wdata_1)
    node _GEN_3 = mux(_T_1, UInt<1>("h1"), UInt<1>("h0"))
    node _wdata_WIRE_3 = _wdata_T_2 @[SRAMTemplate.scala 112:{102,102}]
    node wdata_2 = _wdata_WIRE_3 @[SRAMTemplate.scala 112:{22,22}]
    node _GEN_4 = validif(_T_2, wdata_2)
    node _GEN_5 = mux(_T_2, UInt<1>("h1"), UInt<1>("h0"))
    node _wdata_WIRE_4 = _wdata_T_3 @[SRAMTemplate.scala 112:{102,102}]
    node wdata_3 = _wdata_WIRE_4 @[SRAMTemplate.scala 112:{22,22}]
    node _GEN_6 = validif(_T_3, wdata_3)
    node _GEN_7 = mux(_T_3, UInt<1>("h1"), UInt<1>("h0"))
    node _GEN_8 = validif(wen, _T_5) @[SRAMTemplate.scala 114:14]
    node _GEN_9 = validif(wen, clock) @[SRAMTemplate.scala 114:14]
    node _GEN_10 = mux(wen, UInt<1>("h1"), UInt<1>("h0")) @[SRAMTemplate.scala 114:14 96:26]
    node _GEN_11 = validif(wen, _GEN_1) @[SRAMTemplate.scala 114:14]
    node _GEN_12 = validif(wen, _GEN_3) @[SRAMTemplate.scala 114:14]
    node _GEN_13 = validif(wen, _GEN_5) @[SRAMTemplate.scala 114:14]
    node _GEN_14 = validif(wen, _GEN_7) @[SRAMTemplate.scala 114:14]
    node _GEN_15 = validif(wen, _GEN_0) @[SRAMTemplate.scala 114:14]
    node _GEN_16 = validif(wen, _GEN_2) @[SRAMTemplate.scala 114:14]
    node _GEN_17 = validif(wen, _GEN_4) @[SRAMTemplate.scala 114:14]
    node _GEN_18 = validif(wen, _GEN_6) @[SRAMTemplate.scala 114:14]
    node _GEN_19 = validif(io_r_req_valid, io_r_req_bits_setIdx) @[SRAMTemplate.scala 116:{29,29}]
    node _raw_rdata_WIRE = _GEN_19 @[SRAMTemplate.scala 116:29]
    node _raw_rdata_T = or(_raw_rdata_WIRE, UInt<3>("h0")) @[SRAMTemplate.scala 116:29]
    node _raw_rdata_T_1 = bits(_raw_rdata_T, 2, 0) @[SRAMTemplate.scala 116:29]
    node _GEN_20 = mux(io_r_req_valid, UInt<1>("h1"), UInt<1>("h0")) @[SRAMTemplate.scala 116:{29,29} 96:26]
    node _GEN_21 = validif(io_r_req_valid, _raw_rdata_T_1) @[SRAMTemplate.scala 116:{29,29}]
    node _GEN_22 = validif(io_r_req_valid, clock) @[SRAMTemplate.scala 116:{29,29}]
    reg bypass_wdata_lfsr : UInt<64>, clock with :
      reset => (UInt<1>("h0"), bypass_wdata_lfsr) @[LFSR64.scala 20:23]
    node _bypass_wdata_xor_T = bits(bypass_wdata_lfsr, 0, 0) @[LFSR64.scala 21:19]
    node _bypass_wdata_xor_T_1 = bits(bypass_wdata_lfsr, 1, 1) @[LFSR64.scala 21:29]
    node _bypass_wdata_xor_T_2 = xor(_bypass_wdata_xor_T, _bypass_wdata_xor_T_1) @[LFSR64.scala 21:23]
    node _bypass_wdata_xor_T_3 = bits(bypass_wdata_lfsr, 3, 3) @[LFSR64.scala 21:39]
    node _bypass_wdata_xor_T_4 = xor(_bypass_wdata_xor_T_2, _bypass_wdata_xor_T_3) @[LFSR64.scala 21:33]
    node _bypass_wdata_xor_T_5 = bits(bypass_wdata_lfsr, 4, 4) @[LFSR64.scala 21:49]
    node bypass_wdata_xor = xor(_bypass_wdata_xor_T_4, _bypass_wdata_xor_T_5) @[LFSR64.scala 21:43]
    node _bypass_wdata_lfsr_T = eq(bypass_wdata_lfsr, UInt<1>("h0")) @[LFSR64.scala 23:24]
    node _bypass_wdata_lfsr_T_1 = bits(bypass_wdata_lfsr, 63, 1) @[LFSR64.scala 23:51]
    node _bypass_wdata_lfsr_T_2 = cat(bypass_wdata_xor, _bypass_wdata_lfsr_T_1) @[Cat.scala 31:58]
    node _bypass_wdata_lfsr_T_3 = mux(_bypass_wdata_lfsr_T, UInt<1>("h1"), _bypass_wdata_lfsr_T_2) @[LFSR64.scala 23:18]
    node _GEN_23 = mux(UInt<1>("h1"), _bypass_wdata_lfsr_T_3, bypass_wdata_lfsr) @[LFSR64.scala 22:22 23:12 20:23]
    reg bypass_wdata_lfsr_1 : UInt<64>, clock with :
      reset => (UInt<1>("h0"), bypass_wdata_lfsr_1) @[LFSR64.scala 20:23]
    node _bypass_wdata_xor_T_6 = bits(bypass_wdata_lfsr_1, 0, 0) @[LFSR64.scala 21:19]
    node _bypass_wdata_xor_T_7 = bits(bypass_wdata_lfsr_1, 1, 1) @[LFSR64.scala 21:29]
    node _bypass_wdata_xor_T_8 = xor(_bypass_wdata_xor_T_6, _bypass_wdata_xor_T_7) @[LFSR64.scala 21:23]
    node _bypass_wdata_xor_T_9 = bits(bypass_wdata_lfsr_1, 3, 3) @[LFSR64.scala 21:39]
    node _bypass_wdata_xor_T_10 = xor(_bypass_wdata_xor_T_8, _bypass_wdata_xor_T_9) @[LFSR64.scala 21:33]
    node _bypass_wdata_xor_T_11 = bits(bypass_wdata_lfsr_1, 4, 4) @[LFSR64.scala 21:49]
    node bypass_wdata_xor_1 = xor(_bypass_wdata_xor_T_10, _bypass_wdata_xor_T_11) @[LFSR64.scala 21:43]
    node _bypass_wdata_lfsr_T_4 = eq(bypass_wdata_lfsr_1, UInt<1>("h0")) @[LFSR64.scala 23:24]
    node _bypass_wdata_lfsr_T_5 = bits(bypass_wdata_lfsr_1, 63, 1) @[LFSR64.scala 23:51]
    node _bypass_wdata_lfsr_T_6 = cat(bypass_wdata_xor_1, _bypass_wdata_lfsr_T_5) @[Cat.scala 31:58]
    node _bypass_wdata_lfsr_T_7 = mux(_bypass_wdata_lfsr_T_4, UInt<1>("h1"), _bypass_wdata_lfsr_T_6) @[LFSR64.scala 23:18]
    node _GEN_24 = mux(UInt<1>("h1"), _bypass_wdata_lfsr_T_7, bypass_wdata_lfsr_1) @[LFSR64.scala 22:22 23:12 20:23]
    reg bypass_wdata_lfsr_2 : UInt<64>, clock with :
      reset => (UInt<1>("h0"), bypass_wdata_lfsr_2) @[LFSR64.scala 20:23]
    node _bypass_wdata_xor_T_12 = bits(bypass_wdata_lfsr_2, 0, 0) @[LFSR64.scala 21:19]
    node _bypass_wdata_xor_T_13 = bits(bypass_wdata_lfsr_2, 1, 1) @[LFSR64.scala 21:29]
    node _bypass_wdata_xor_T_14 = xor(_bypass_wdata_xor_T_12, _bypass_wdata_xor_T_13) @[LFSR64.scala 21:23]
    node _bypass_wdata_xor_T_15 = bits(bypass_wdata_lfsr_2, 3, 3) @[LFSR64.scala 21:39]
    node _bypass_wdata_xor_T_16 = xor(_bypass_wdata_xor_T_14, _bypass_wdata_xor_T_15) @[LFSR64.scala 21:33]
    node _bypass_wdata_xor_T_17 = bits(bypass_wdata_lfsr_2, 4, 4) @[LFSR64.scala 21:49]
    node bypass_wdata_xor_2 = xor(_bypass_wdata_xor_T_16, _bypass_wdata_xor_T_17) @[LFSR64.scala 21:43]
    node _bypass_wdata_lfsr_T_8 = eq(bypass_wdata_lfsr_2, UInt<1>("h0")) @[LFSR64.scala 23:24]
    node _bypass_wdata_lfsr_T_9 = bits(bypass_wdata_lfsr_2, 63, 1) @[LFSR64.scala 23:51]
    node _bypass_wdata_lfsr_T_10 = cat(bypass_wdata_xor_2, _bypass_wdata_lfsr_T_9) @[Cat.scala 31:58]
    node _bypass_wdata_lfsr_T_11 = mux(_bypass_wdata_lfsr_T_8, UInt<1>("h1"), _bypass_wdata_lfsr_T_10) @[LFSR64.scala 23:18]
    node _GEN_25 = mux(UInt<1>("h1"), _bypass_wdata_lfsr_T_11, bypass_wdata_lfsr_2) @[LFSR64.scala 22:22 23:12 20:23]
    reg bypass_wdata_lfsr_3 : UInt<64>, clock with :
      reset => (UInt<1>("h0"), bypass_wdata_lfsr_3) @[LFSR64.scala 20:23]
    node _bypass_wdata_xor_T_18 = bits(bypass_wdata_lfsr_3, 0, 0) @[LFSR64.scala 21:19]
    node _bypass_wdata_xor_T_19 = bits(bypass_wdata_lfsr_3, 1, 1) @[LFSR64.scala 21:29]
    node _bypass_wdata_xor_T_20 = xor(_bypass_wdata_xor_T_18, _bypass_wdata_xor_T_19) @[LFSR64.scala 21:23]
    node _bypass_wdata_xor_T_21 = bits(bypass_wdata_lfsr_3, 3, 3) @[LFSR64.scala 21:39]
    node _bypass_wdata_xor_T_22 = xor(_bypass_wdata_xor_T_20, _bypass_wdata_xor_T_21) @[LFSR64.scala 21:33]
    node _bypass_wdata_xor_T_23 = bits(bypass_wdata_lfsr_3, 4, 4) @[LFSR64.scala 21:49]
    node bypass_wdata_xor_3 = xor(_bypass_wdata_xor_T_22, _bypass_wdata_xor_T_23) @[LFSR64.scala 21:43]
    node _bypass_wdata_lfsr_T_12 = eq(bypass_wdata_lfsr_3, UInt<1>("h0")) @[LFSR64.scala 23:24]
    node _bypass_wdata_lfsr_T_13 = bits(bypass_wdata_lfsr_3, 63, 1) @[LFSR64.scala 23:51]
    node _bypass_wdata_lfsr_T_14 = cat(bypass_wdata_xor_3, _bypass_wdata_lfsr_T_13) @[Cat.scala 31:58]
    node _bypass_wdata_lfsr_T_15 = mux(_bypass_wdata_lfsr_T_12, UInt<1>("h1"), _bypass_wdata_lfsr_T_14) @[LFSR64.scala 23:18]
    node _GEN_26 = mux(UInt<1>("h1"), _bypass_wdata_lfsr_T_15, bypass_wdata_lfsr_3) @[LFSR64.scala 22:22 23:12 20:23]
    node _bypass_mask_need_check_T = and(io_r_req_valid, io_w_req_valid) @[SRAMTemplate.scala 121:34]
    reg bypass_mask_need_check : UInt<1>, clock with :
      reset => (UInt<1>("h0"), bypass_mask_need_check) @[SRAMTemplate.scala 121:29]
    reg bypass_mask_waddr_reg : UInt<3>, clock with :
      reset => (UInt<1>("h0"), bypass_mask_waddr_reg) @[SRAMTemplate.scala 122:28]
    reg bypass_mask_raddr_reg : UInt<3>, clock with :
      reset => (UInt<1>("h0"), bypass_mask_raddr_reg) @[SRAMTemplate.scala 123:28]
    node _bypass_mask_bypass_T = eq(bypass_mask_waddr_reg, bypass_mask_raddr_reg) @[SRAMTemplate.scala 125:52]
    node _bypass_mask_bypass_T_1 = and(bypass_mask_need_check, _bypass_mask_bypass_T) @[SRAMTemplate.scala 125:39]
    node _bypass_mask_bypass_T_2 = bits(_bypass_mask_bypass_T_1, 0, 0) @[Bitwise.scala 74:15]
    node _bypass_mask_bypass_T_3 = mux(_bypass_mask_bypass_T_2, UInt<4>("hf"), UInt<4>("h0")) @[Bitwise.scala 74:12]
    reg bypass_mask_bypass_REG : UInt<4>, clock with :
      reset => (UInt<1>("h0"), bypass_mask_bypass_REG) @[SRAMTemplate.scala 125:76]
    node bypass_mask_bypass = and(_bypass_mask_bypass_T_3, bypass_mask_bypass_REG) @[SRAMTemplate.scala 125:67]
    node bypass_mask = bypass_mask_bypass @[SRAMTemplate.scala 126:{20,20}]
    node _mem_rdata_T = bits(bypass_mask, 0, 0) @[SRAMTemplate.scala 133:30]
    node _mem_rdata_T_1 = bits(bypass_mask, 1, 1) @[SRAMTemplate.scala 133:30]
    node _mem_rdata_T_2 = bits(bypass_mask, 2, 2) @[SRAMTemplate.scala 133:30]
    node _mem_rdata_T_3 = bits(bypass_mask, 3, 3) @[SRAMTemplate.scala 133:30]
    node _bypass_wdata_WIRE = bits(bypass_wdata_lfsr, 7, 0) @[SRAMTemplate.scala 129:{58,58}]
    node bypass_wdata_0 = _bypass_wdata_WIRE @[SRAMTemplate.scala 129:{17,17}]
    node _mem_rdata_T_4 = mux(_mem_rdata_T, bypass_wdata_0, array_0.raw_rdata.data) @[SRAMTemplate.scala 134:30]
    node _bypass_wdata_WIRE_1 = bits(bypass_wdata_lfsr_1, 7, 0) @[SRAMTemplate.scala 129:{58,58}]
    node bypass_wdata_1 = _bypass_wdata_WIRE_1 @[SRAMTemplate.scala 129:{17,17}]
    node _mem_rdata_T_5 = mux(_mem_rdata_T_1, bypass_wdata_1, array_1.raw_rdata.data) @[SRAMTemplate.scala 134:30]
    node _bypass_wdata_WIRE_2 = bits(bypass_wdata_lfsr_2, 7, 0) @[SRAMTemplate.scala 129:{58,58}]
    node bypass_wdata_2 = _bypass_wdata_WIRE_2 @[SRAMTemplate.scala 129:{17,17}]
    node _mem_rdata_T_6 = mux(_mem_rdata_T_2, bypass_wdata_2, array_2.raw_rdata.data) @[SRAMTemplate.scala 134:30]
    node _bypass_wdata_WIRE_3 = bits(bypass_wdata_lfsr_3, 7, 0) @[SRAMTemplate.scala 129:{58,58}]
    node bypass_wdata_3 = _bypass_wdata_WIRE_3 @[SRAMTemplate.scala 129:{17,17}]
    node _mem_rdata_T_7 = mux(_mem_rdata_T_3, bypass_wdata_3, array_3.raw_rdata.data) @[SRAMTemplate.scala 134:30]
    node _io_r_req_ready_T = eq(resetState, UInt<1>("h0")) @[SRAMTemplate.scala 143:21]
    node _io_r_req_ready_T_1 = and(_io_r_req_ready_T, UInt<1>("h1")) @[SRAMTemplate.scala 143:33]
    node mem_rdata_0 = _mem_rdata_T_4 @[SRAMTemplate.scala 133:{17,17}]
    node mem_rdata_1 = _mem_rdata_T_5 @[SRAMTemplate.scala 133:{17,17}]
    node mem_rdata_2 = _mem_rdata_T_6 @[SRAMTemplate.scala 133:{17,17}]
    node mem_rdata_3 = _mem_rdata_T_7 @[SRAMTemplate.scala 133:{17,17}]
    node rdata_0 = mem_rdata_0 @[SRAMTemplate.scala 140:{45,45}]
    node rdata_1 = mem_rdata_1 @[SRAMTemplate.scala 140:{45,45}]
    node rdata_2 = mem_rdata_2 @[SRAMTemplate.scala 140:{45,45}]
    node rdata_3 = mem_rdata_3 @[SRAMTemplate.scala 140:{45,45}]
    node _WIRE_0 = rdata_0 @[SRAMTemplate.scala 142:{28,28}]
    node _WIRE_1 = rdata_1 @[SRAMTemplate.scala 142:{28,28}]
    node _WIRE_2 = rdata_2 @[SRAMTemplate.scala 142:{28,28}]
    node _WIRE_3 = rdata_3 @[SRAMTemplate.scala 142:{28,28}]
    io_r_req_ready <= _io_r_req_ready_T_1 @[SRAMTemplate.scala 143:18]
    io_r_resp_data_0 <= _WIRE_0 @[SRAMTemplate.scala 142:18]
    io_r_resp_data_1 <= _WIRE_1 @[SRAMTemplate.scala 142:18]
    io_r_resp_data_2 <= _WIRE_2 @[SRAMTemplate.scala 142:18]
    io_r_resp_data_3 <= _WIRE_3 @[SRAMTemplate.scala 142:18]
    io_w_req_ready <= UInt<1>("h1") @[SRAMTemplate.scala 144:18]
    array_0.raw_rdata.addr <= _GEN_21
    array_1.raw_rdata.addr <= _GEN_21
    array_2.raw_rdata.addr <= _GEN_21
    array_3.raw_rdata.addr <= _GEN_21
    array_0.raw_rdata.en <= _GEN_20
    array_1.raw_rdata.en <= _GEN_20
    array_2.raw_rdata.en <= _GEN_20
    array_3.raw_rdata.en <= _GEN_20
    array_0.raw_rdata.clk <= _GEN_22
    array_1.raw_rdata.clk <= _GEN_22
    array_2.raw_rdata.clk <= _GEN_22
    array_3.raw_rdata.clk <= _GEN_22
    array_0.MPORT.addr <= _GEN_8
    array_1.MPORT.addr <= _GEN_8
    array_2.MPORT.addr <= _GEN_8
    array_3.MPORT.addr <= _GEN_8
    array_0.MPORT.en <= _GEN_10
    array_1.MPORT.en <= _GEN_10
    array_2.MPORT.en <= _GEN_10
    array_3.MPORT.en <= _GEN_10
    array_0.MPORT.clk <= _GEN_9
    array_1.MPORT.clk <= _GEN_9
    array_2.MPORT.clk <= _GEN_9
    array_3.MPORT.clk <= _GEN_9
    array_0.MPORT.data <= _GEN_15
    array_1.MPORT.data <= _GEN_16
    array_2.MPORT.data <= _GEN_17
    array_3.MPORT.data <= _GEN_18
    array_0.MPORT.mask <= _GEN_11
    array_1.MPORT.mask <= _GEN_12
    array_2.MPORT.mask <= _GEN_13
    array_3.MPORT.mask <= _GEN_14
    bypass_wdata_lfsr <= mux(reset, UInt<64>("h1234567887654321"), _GEN_23) @[LFSR64.scala 20:{23,23}]
    bypass_wdata_lfsr_1 <= mux(reset, UInt<64>("h1234567887654321"), _GEN_24) @[LFSR64.scala 20:{23,23}]
    bypass_wdata_lfsr_2 <= mux(reset, UInt<64>("h1234567887654321"), _GEN_25) @[LFSR64.scala 20:{23,23}]
    bypass_wdata_lfsr_3 <= mux(reset, UInt<64>("h1234567887654321"), _GEN_26) @[LFSR64.scala 20:{23,23}]
    bypass_mask_need_check <= _bypass_mask_need_check_T @[SRAMTemplate.scala 121:29]
    bypass_mask_waddr_reg <= io_w_req_bits_setIdx @[SRAMTemplate.scala 122:28]
    bypass_mask_raddr_reg <= io_r_req_bits_setIdx @[SRAMTemplate.scala 123:28]
    bypass_mask_bypass_REG <= io_w_req_bits_waymask @[SRAMTemplate.scala 125:76]

  module DataCache :
    input clock : Clock
    input reset : UInt<1>
    output io_coreReq_ready : UInt<1>
    input io_coreReq_valid : UInt<1>
    input io_coreReq_bits_instrId : UInt<3>
    input io_coreReq_bits_opcode : UInt<3>
    input io_coreReq_bits_param : UInt<4>
    input io_coreReq_bits_tag : UInt<26>
    input io_coreReq_bits_setIdx : UInt<2>
    input io_coreReq_bits_perLaneAddr_0_activeMask : UInt<1>
    input io_coreReq_bits_perLaneAddr_0_blockOffset : UInt<2>
    input io_coreReq_bits_perLaneAddr_0_wordOffset1H : UInt<4>
    input io_coreReq_bits_perLaneAddr_1_activeMask : UInt<1>
    input io_coreReq_bits_perLaneAddr_1_blockOffset : UInt<2>
    input io_coreReq_bits_perLaneAddr_1_wordOffset1H : UInt<4>
    input io_coreReq_bits_perLaneAddr_2_activeMask : UInt<1>
    input io_coreReq_bits_perLaneAddr_2_blockOffset : UInt<2>
    input io_coreReq_bits_perLaneAddr_2_wordOffset1H : UInt<4>
    input io_coreReq_bits_perLaneAddr_3_activeMask : UInt<1>
    input io_coreReq_bits_perLaneAddr_3_blockOffset : UInt<2>
    input io_coreReq_bits_perLaneAddr_3_wordOffset1H : UInt<4>
    input io_coreReq_bits_data_0 : UInt<32>
    input io_coreReq_bits_data_1 : UInt<32>
    input io_coreReq_bits_data_2 : UInt<32>
    input io_coreReq_bits_data_3 : UInt<32>
    input io_coreRsp_ready : UInt<1>
    output io_coreRsp_valid : UInt<1>
    output io_coreRsp_bits_instrId : UInt<3>
    output io_coreRsp_bits_isWrite : UInt<1>
    output io_coreRsp_bits_data_0 : UInt<32>
    output io_coreRsp_bits_data_1 : UInt<32>
    output io_coreRsp_bits_data_2 : UInt<32>
    output io_coreRsp_bits_data_3 : UInt<32>
    output io_coreRsp_bits_activeMask_0 : UInt<1>
    output io_coreRsp_bits_activeMask_1 : UInt<1>
    output io_coreRsp_bits_activeMask_2 : UInt<1>
    output io_coreRsp_bits_activeMask_3 : UInt<1>
    output io_memRsp_ready : UInt<1>
    input io_memRsp_valid : UInt<1>
    input io_memRsp_bits_d_opcode : UInt<3>
    input io_memRsp_bits_d_source : UInt<7>
    input io_memRsp_bits_d_addr : UInt<32>
    input io_memRsp_bits_d_data_0 : UInt<32>
    input io_memRsp_bits_d_data_1 : UInt<32>
    input io_memRsp_bits_d_data_2 : UInt<32>
    input io_memRsp_bits_d_data_3 : UInt<32>
    input io_memReq_ready : UInt<1>
    output io_memReq_valid : UInt<1>
    output io_memReq_bits_a_opcode : UInt<3>
    output io_memReq_bits_a_param : UInt<3>
    output io_memReq_bits_a_addr : UInt<32>
    output io_memReq_bits_a_data_0 : UInt<32>
    output io_memReq_bits_a_data_1 : UInt<32>
    output io_memReq_bits_a_data_2 : UInt<32>
    output io_memReq_bits_a_data_3 : UInt<32>
    output io_memReq_bits_a_mask_0 : UInt<1>
    output io_memReq_bits_a_mask_1 : UInt<1>
    output io_memReq_bits_a_mask_2 : UInt<1>
    output io_memReq_bits_a_mask_3 : UInt<1>
    output io_memReq_bits_a_source : UInt<7>

    inst MshrAccess of MSHR @[DCache.scala 96:26]
    inst TagAccess of L1TagAccess @[DCache.scala 99:25]
    inst WshrAccess of DCacheWSHR @[DCache.scala 100:26]
    inst coreReq_Q of Queue @[DCache.scala 103:25]
    inst coreRsp_Q of Queue_1 @[DCache.scala 106:25]
    inst memRsp_Q of Queue_2 @[DCache.scala 109:24]
    inst memReq_Q of Queue_3 @[DCache.scala 113:24]
    inst MemReqArb of Arbiter_2 @[DCache.scala 114:25]
    inst genCtrl of genControl @[DCache.scala 157:23]
    inst getBankEn of getDataAccessBankEn @[DCache.scala 221:25]
    inst DataAccessesRRsp_DataAccess of SRAMTemplate_2 @[DCache.scala 402:28]
    inst DataAccessesRRsp_DataAccess_1 of SRAMTemplate_2 @[DCache.scala 402:28]
    inst DataAccessesRRsp_DataAccess_2 of SRAMTemplate_2 @[DCache.scala 402:28]
    inst DataAccessesRRsp_DataAccess_3 of SRAMTemplate_2 @[DCache.scala 402:28]
    node _secondaryFullReturn_T = eq(MshrAccess.io_probeOut_st1_probeStatus, UInt<3>("h4")) @[DCache.scala 127:76]
    reg secondaryFullReturn : UInt<1>, clock with :
      reset => (UInt<1>("h0"), secondaryFullReturn) @[DCache.scala 127:36]
    node _coreReqControl_st1_T = and(io_coreReq_ready, io_coreReq_valid) @[Decoupled.scala 50:35]
    reg coreReqControl_st1_isRead : UInt<1>, clock with :
      reset => (UInt<1>("h0"), coreReqControl_st1_isRead) @[Reg.scala 16:16]
    reg coreReqControl_st1_isWrite : UInt<1>, clock with :
      reset => (UInt<1>("h0"), coreReqControl_st1_isWrite) @[Reg.scala 16:16]
    reg coreReqControl_st1_isLR : UInt<1>, clock with :
      reset => (UInt<1>("h0"), coreReqControl_st1_isLR) @[Reg.scala 16:16]
    reg coreReqControl_st1_isSC : UInt<1>, clock with :
      reset => (UInt<1>("h0"), coreReqControl_st1_isSC) @[Reg.scala 16:16]
    reg coreReqControl_st1_isAMO : UInt<1>, clock with :
      reset => (UInt<1>("h0"), coreReqControl_st1_isAMO) @[Reg.scala 16:16]
    reg coreReqControl_st1_isFlush : UInt<1>, clock with :
      reset => (UInt<1>("h0"), coreReqControl_st1_isFlush) @[Reg.scala 16:16]
    reg coreReqControl_st1_isInvalidate : UInt<1>, clock with :
      reset => (UInt<1>("h0"), coreReqControl_st1_isInvalidate) @[Reg.scala 16:16]
    reg coreReqControl_st1_isWaitMSHR : UInt<1>, clock with :
      reset => (UInt<1>("h0"), coreReqControl_st1_isWaitMSHR) @[Reg.scala 16:16]
    node coreReqControl_st0_isRead = genCtrl.io_control_isRead @[DCache.scala 128:32 160:22]
    node _GEN_0 = mux(_coreReqControl_st1_T, coreReqControl_st0_isRead, coreReqControl_st1_isRead) @[Reg.scala 16:16 17:{18,22}]
    node coreReqControl_st0_isWrite = genCtrl.io_control_isWrite @[DCache.scala 128:32 160:22]
    node _GEN_1 = mux(_coreReqControl_st1_T, coreReqControl_st0_isWrite, coreReqControl_st1_isWrite) @[Reg.scala 16:16 17:{18,22}]
    node coreReqControl_st0_isLR = genCtrl.io_control_isLR @[DCache.scala 128:32 160:22]
    node _GEN_2 = mux(_coreReqControl_st1_T, coreReqControl_st0_isLR, coreReqControl_st1_isLR) @[Reg.scala 16:16 17:{18,22}]
    node coreReqControl_st0_isSC = genCtrl.io_control_isSC @[DCache.scala 128:32 160:22]
    node _GEN_3 = mux(_coreReqControl_st1_T, coreReqControl_st0_isSC, coreReqControl_st1_isSC) @[Reg.scala 16:16 17:{18,22}]
    node coreReqControl_st0_isAMO = genCtrl.io_control_isAMO @[DCache.scala 128:32 160:22]
    node _GEN_4 = mux(_coreReqControl_st1_T, coreReqControl_st0_isAMO, coreReqControl_st1_isAMO) @[Reg.scala 16:16 17:{18,22}]
    node coreReqControl_st0_isFlush = genCtrl.io_control_isFlush @[DCache.scala 128:32 160:22]
    node _GEN_5 = mux(_coreReqControl_st1_T, coreReqControl_st0_isFlush, coreReqControl_st1_isFlush) @[Reg.scala 16:16 17:{18,22}]
    node coreReqControl_st0_isInvalidate = genCtrl.io_control_isInvalidate @[DCache.scala 128:32 160:22]
    node _GEN_6 = mux(_coreReqControl_st1_T, coreReqControl_st0_isInvalidate, coreReqControl_st1_isInvalidate) @[Reg.scala 16:16 17:{18,22}]
    node coreReqControl_st0_isWaitMSHR = genCtrl.io_control_isWaitMSHR @[DCache.scala 128:32 160:22]
    node _GEN_7 = mux(_coreReqControl_st1_T, coreReqControl_st0_isWaitMSHR, coreReqControl_st1_isWaitMSHR) @[Reg.scala 16:16 17:{18,22}]
    node cacheMiss_st1 = eq(TagAccess.io_hit_st1, UInt<1>("h0")) @[DCache.scala 132:23]
    node cacheHit_st1 = TagAccess.io_hit_st1 @[DCache.scala 130:26 131:16]
    node readHit_st1 = and(cacheHit_st1, coreReqControl_st1_isRead) @[DCache.scala 134:34]
    node readMiss_st1 = and(cacheMiss_st1, coreReqControl_st1_isRead) @[DCache.scala 135:36]
    node writeHit_st1 = and(cacheHit_st1, coreReqControl_st1_isWrite) @[DCache.scala 136:35]
    node writeMiss_st1 = and(cacheMiss_st1, coreReqControl_st1_isWrite) @[DCache.scala 137:37]
    reg coreRsp_st2_instrId : UInt<3>, clock with :
      reset => (UInt<1>("h0"), coreRsp_st2_instrId) @[DCache.scala 139:23]
    reg coreRsp_st2_isWrite : UInt<1>, clock with :
      reset => (UInt<1>("h0"), coreRsp_st2_isWrite) @[DCache.scala 139:23]
    reg coreRsp_st2_data_0 : UInt<32>, clock with :
      reset => (UInt<1>("h0"), coreRsp_st2_data_0) @[DCache.scala 139:23]
    reg coreRsp_st2_data_1 : UInt<32>, clock with :
      reset => (UInt<1>("h0"), coreRsp_st2_data_1) @[DCache.scala 139:23]
    reg coreRsp_st2_data_2 : UInt<32>, clock with :
      reset => (UInt<1>("h0"), coreRsp_st2_data_2) @[DCache.scala 139:23]
    reg coreRsp_st2_data_3 : UInt<32>, clock with :
      reset => (UInt<1>("h0"), coreRsp_st2_data_3) @[DCache.scala 139:23]
    reg coreRsp_st2_activeMask_0 : UInt<1>, clock with :
      reset => (UInt<1>("h0"), coreRsp_st2_activeMask_0) @[DCache.scala 139:23]
    reg coreRsp_st2_activeMask_1 : UInt<1>, clock with :
      reset => (UInt<1>("h0"), coreRsp_st2_activeMask_1) @[DCache.scala 139:23]
    reg coreRsp_st2_activeMask_2 : UInt<1>, clock with :
      reset => (UInt<1>("h0"), coreRsp_st2_activeMask_2) @[DCache.scala 139:23]
    reg coreRsp_st2_activeMask_3 : UInt<1>, clock with :
      reset => (UInt<1>("h0"), coreRsp_st2_activeMask_3) @[DCache.scala 139:23]
    reg coreRsp_st2_perLaneAddr_0_activeMask : UInt<1>, clock with :
      reset => (UInt<1>("h0"), coreRsp_st2_perLaneAddr_0_activeMask) @[DCache.scala 141:36]
    reg coreRsp_st2_perLaneAddr_0_blockOffset : UInt<2>, clock with :
      reset => (UInt<1>("h0"), coreRsp_st2_perLaneAddr_0_blockOffset) @[DCache.scala 141:36]
    reg coreRsp_st2_perLaneAddr_0_wordOffset1H : UInt<4>, clock with :
      reset => (UInt<1>("h0"), coreRsp_st2_perLaneAddr_0_wordOffset1H) @[DCache.scala 141:36]
    reg coreRsp_st2_perLaneAddr_1_activeMask : UInt<1>, clock with :
      reset => (UInt<1>("h0"), coreRsp_st2_perLaneAddr_1_activeMask) @[DCache.scala 141:36]
    reg coreRsp_st2_perLaneAddr_1_blockOffset : UInt<2>, clock with :
      reset => (UInt<1>("h0"), coreRsp_st2_perLaneAddr_1_blockOffset) @[DCache.scala 141:36]
    reg coreRsp_st2_perLaneAddr_1_wordOffset1H : UInt<4>, clock with :
      reset => (UInt<1>("h0"), coreRsp_st2_perLaneAddr_1_wordOffset1H) @[DCache.scala 141:36]
    reg coreRsp_st2_perLaneAddr_2_activeMask : UInt<1>, clock with :
      reset => (UInt<1>("h0"), coreRsp_st2_perLaneAddr_2_activeMask) @[DCache.scala 141:36]
    reg coreRsp_st2_perLaneAddr_2_blockOffset : UInt<2>, clock with :
      reset => (UInt<1>("h0"), coreRsp_st2_perLaneAddr_2_blockOffset) @[DCache.scala 141:36]
    reg coreRsp_st2_perLaneAddr_2_wordOffset1H : UInt<4>, clock with :
      reset => (UInt<1>("h0"), coreRsp_st2_perLaneAddr_2_wordOffset1H) @[DCache.scala 141:36]
    reg coreRsp_st2_perLaneAddr_3_activeMask : UInt<1>, clock with :
      reset => (UInt<1>("h0"), coreRsp_st2_perLaneAddr_3_activeMask) @[DCache.scala 141:36]
    reg coreRsp_st2_perLaneAddr_3_blockOffset : UInt<2>, clock with :
      reset => (UInt<1>("h0"), coreRsp_st2_perLaneAddr_3_blockOffset) @[DCache.scala 141:36]
    reg coreRsp_st2_perLaneAddr_3_wordOffset1H : UInt<4>, clock with :
      reset => (UInt<1>("h0"), coreRsp_st2_perLaneAddr_3_wordOffset1H) @[DCache.scala 141:36]
    reg readHit_st2 : UInt<1>, clock with :
      reset => (UInt<1>("h0"), readHit_st2) @[DCache.scala 143:28]
    node _coreReq_Q_io_deq_ready_T = eq(coreReq_Q.io_deq_bits_opcode, UInt<2>("h3")) @[DCache.scala 146:36]
    node _coreReq_Q_io_deq_ready_T_1 = and(_coreReq_Q_io_deq_ready_T, readHit_st1) @[DCache.scala 146:44]
    node _coreReq_st1_valid_T = eq(secondaryFullReturn, UInt<1>("h0")) @[DCache.scala 182:86]
    node _coreReq_st1_valid_T_1 = and(MshrAccess.io_missRspOut_valid, _coreReq_st1_valid_T) @[DCache.scala 182:83]
    node _coreReq_st1_valid_T_2 = eq(_coreReq_st1_valid_T_1, UInt<1>("h0")) @[DCache.scala 182:50]
    node _coreReq_st1_valid_T_3 = and(coreReq_Q.io_deq_valid, _coreReq_st1_valid_T_2) @[DCache.scala 182:47]
    node coreReq_st1_valid = _coreReq_st1_valid_T_3 @[DCache.scala 124:31 182:21]
    node _coreReq_Q_io_deq_ready_T_2 = and(_coreReq_Q_io_deq_ready_T_1, coreReq_st1_valid) @[DCache.scala 146:59]
    node _coreReq_Q_io_deq_ready_T_3 = eq(_coreReq_Q_io_deq_ready_T_2, UInt<1>("h0")) @[DCache.scala 146:5]
    node _T_16 = or(coreReqControl_st1_isRead, coreReqControl_st1_isWrite) @[DCache.scala 239:34]
    node _GEN_40 = mux(coreRsp_Q.io_enq_ready, UInt<1>("h1"), UInt<1>("h0")) @[DCache.scala 238:21 241:36 242:27]
    node _T_17 = and(MshrAccess.io_missReq_ready, MemReqArb.io_in_1_ready) @[DCache.scala 246:42]
    node _T_18 = eq(secondaryFullReturn, UInt<1>("h0")) @[DCache.scala 247:50]
    node _T_19 = and(MshrAccess.io_missRspOut_valid, _T_18) @[DCache.scala 247:47]
    node _T_20 = eq(_T_19, UInt<1>("h0")) @[DCache.scala 247:14]
    node _T_21 = and(_T_17, _T_20) @[DCache.scala 247:11]
    node _GEN_41 = mux(_T_21, UInt<1>("h1"), UInt<1>("h0")) @[DCache.scala 238:21 247:72 248:29]
    node _T_22 = and(coreRsp_Q.io_enq_ready, MemReqArb.io_in_1_ready) @[DCache.scala 252:37]
    node _GEN_42 = mux(_T_22, UInt<1>("h1"), UInt<1>("h0")) @[DCache.scala 238:21 252:65 253:29]
    node _GEN_43 = mux(coreReqControl_st1_isRead, _GEN_41, _GEN_42) @[DCache.scala 245:38]
    node _GEN_44 = mux(TagAccess.io_hit_st1, _GEN_40, _GEN_43) @[DCache.scala 240:32]
    reg coreReqTagHasDirty_st1 : UInt<1>, clock with :
      reset => (UInt<1>("h0"), coreReqTagHasDirty_st1) @[DCache.scala 163:39]
    node _T_23 = eq(coreReqTagHasDirty_st1, UInt<1>("h0")) @[DCache.scala 260:10]
    node _T_24 = and(_T_23, MshrAccess.io_empty) @[DCache.scala 260:34]
    node _T_25 = and(_T_24, WshrAccess.io_empty) @[DCache.scala 260:57]
    node _GEN_45 = mux(_T_25, UInt<1>("h1"), UInt<1>("h0")) @[DCache.scala 238:21 260:80 261:25]
    node _T_26 = eq(coreReqTagHasDirty_st1, UInt<1>("h0")) @[DCache.scala 264:10]
    node _T_27 = and(_T_26, WshrAccess.io_empty) @[DCache.scala 264:34]
    node _GEN_46 = mux(_T_27, UInt<1>("h1"), UInt<1>("h0")) @[DCache.scala 238:21 264:57 265:25]
    node _GEN_47 = mux(MshrAccess.io_empty, UInt<1>("h1"), UInt<1>("h0")) @[DCache.scala 238:21 268:30 269:25]
    node _GEN_48 = mux(coreReqControl_st1_isWaitMSHR, _GEN_47, UInt<1>("h1")) @[DCache.scala 267:44 272:23]
    node _GEN_49 = mux(coreReqControl_st1_isFlush, _GEN_46, _GEN_48) @[DCache.scala 263:41]
    node _GEN_50 = mux(coreReqControl_st1_isInvalidate, _GEN_45, _GEN_49) @[DCache.scala 259:46]
    node _GEN_51 = mux(_T_16, _GEN_44, _GEN_50) @[DCache.scala 239:64]
    node coreReq_st1_ready = _GEN_51 @[DCache.scala 122:31]
    node _coreReq_Q_io_deq_ready_T_4 = and(coreReq_st1_ready, _coreReq_Q_io_deq_ready_T_3) @[DCache.scala 145:47]
    node _TagAccess_io_probeRead_valid_T = and(io_coreReq_ready, io_coreReq_valid) @[Decoupled.scala 50:35]
    node _MshrAccess_io_probe_valid_T = and(io_coreReq_ready, io_coreReq_valid) @[Decoupled.scala 50:35]
    node _MshrAccess_io_probe_bits_blockAddr_T = cat(io_coreReq_bits_tag, io_coreReq_bits_setIdx) @[Cat.scala 31:58]
    node _coreReqInvOrFluValid_st0_T = eq(coreReq_Q.io_deq_bits_opcode, UInt<2>("h3")) @[DCache.scala 168:34]
    node _coreReqInvOrFluValid_st0_T_1 = and(coreReq_Q.io_deq_valid, _coreReqInvOrFluValid_st0_T) @[DCache.scala 167:54]
    node _coreReqInvOrFluValid_st0_T_2 = neq(coreReq_Q.io_deq_bits_param, UInt<2>("h2")) @[DCache.scala 168:73]
    node _coreReqInvOrFluValid_st0_T_3 = and(_coreReqInvOrFluValid_st0_T_1, _coreReqInvOrFluValid_st0_T_2) @[DCache.scala 168:42]
    node _coreReqInvOrFluValid_st1_T = or(coreReqControl_st1_isInvalidate, coreReqControl_st1_isFlush) @[DCache.scala 170:38]
    node _coreReqInvOrFluValid_st1_T_1 = and(coreReq_st1_valid, _coreReqInvOrFluValid_st1_T) @[DCache.scala 169:49]
    node coreReqInvOrFluValid_st0 = _coreReqInvOrFluValid_st0_T_3 @[DCache.scala 165:38 167:28]
    node coreReqInvOrFluValid_st1 = _coreReqInvOrFluValid_st1_T_1 @[DCache.scala 166:38 169:28]
    node _TagAccess_io_flushChoosen_valid_T = or(coreReqInvOrFluValid_st0, coreReqInvOrFluValid_st1) @[DCache.scala 171:68]
    node _TagAccess_io_flushChoosen_valid_T_1 = and(_TagAccess_io_flushChoosen_valid_T, TagAccess.io_hasDirty_st0) @[DCache.scala 171:97]
    node _TagAccess_io_flushChoosen_bits_T = cat(TagAccess.io_dirtySetIdx_st0, TagAccess.io_dirtyWayMask_st0) @[Cat.scala 31:58]
    node _dataAccessInvOrFluRValid_T = or(coreReqInvOrFluValid_st0, coreReqInvOrFluValid_st1) @[DCache.scala 176:60]
    node dataAccessInvOrFluRValid = and(_dataAccessInvOrFluRValid_T, TagAccess.io_hasDirty_st0) @[DCache.scala 176:89]
    node _DataAccessInvOrFluSRAMRReq_0_setIdx_T = cat(TagAccess.io_dirtySetIdx_st0, TagAccess.io_dirtyWayMask_st0) @[Cat.scala 31:58]
    node _DataAccessInvOrFluSRAMRReq_1_setIdx_T = cat(TagAccess.io_dirtySetIdx_st0, TagAccess.io_dirtyWayMask_st0) @[Cat.scala 31:58]
    node _DataAccessInvOrFluSRAMRReq_2_setIdx_T = cat(TagAccess.io_dirtySetIdx_st0, TagAccess.io_dirtyWayMask_st0) @[Cat.scala 31:58]
    node _DataAccessInvOrFluSRAMRReq_3_setIdx_T = cat(TagAccess.io_dirtySetIdx_st0, TagAccess.io_dirtyWayMask_st0) @[Cat.scala 31:58]
    node _MshrAccess_io_missReq_valid_T = eq(MshrAccess.io_missRspOut_valid, UInt<1>("h0")) @[DCache.scala 187:50]
    node _MshrAccess_io_missReq_valid_T_1 = and(readMiss_st1, _MshrAccess_io_missReq_valid_T) @[DCache.scala 187:47]
    node _MshrAccess_io_missReq_valid_T_2 = and(_MshrAccess_io_missReq_valid_T_1, coreReq_st1_valid) @[DCache.scala 187:82]
    reg MshrAccess_io_missReq_valid_REG : UInt<1>, clock with :
      reset => (UInt<1>("h0"), MshrAccess_io_missReq_valid_REG) @[DCache.scala 187:114]
    node _MshrAccess_io_missReq_valid_T_3 = eq(MshrAccess_io_missReq_valid_REG, UInt<1>("h0")) @[DCache.scala 187:106]
    node _MshrAccess_io_missReq_valid_T_4 = and(_MshrAccess_io_missReq_valid_T_2, _MshrAccess_io_missReq_valid_T_3) @[DCache.scala 187:103]
    node _MshrAccess_io_missReq_bits_blockAddr_T = cat(coreReq_Q.io_deq_bits_tag, coreReq_Q.io_deq_bits_setIdx) @[Cat.scala 31:58]
    node mshrMissReqTI_perLaneAddr_0_activeMask = coreReq_Q.io_deq_bits_perLaneAddr_0_activeMask @[DCache.scala 188:27 191:29]
    node mshrMissReqTI_perLaneAddr_0_blockOffset = coreReq_Q.io_deq_bits_perLaneAddr_0_blockOffset @[DCache.scala 188:27 191:29]
    node MshrAccess_io_missReq_bits_targetInfo_lo_lo_hi = cat(mshrMissReqTI_perLaneAddr_0_activeMask, mshrMissReqTI_perLaneAddr_0_blockOffset) @[DCache.scala 194:58]
    node mshrMissReqTI_perLaneAddr_0_wordOffset1H = coreReq_Q.io_deq_bits_perLaneAddr_0_wordOffset1H @[DCache.scala 188:27 191:29]
    node MshrAccess_io_missReq_bits_targetInfo_lo_lo = cat(MshrAccess_io_missReq_bits_targetInfo_lo_lo_hi, mshrMissReqTI_perLaneAddr_0_wordOffset1H) @[DCache.scala 194:58]
    node mshrMissReqTI_perLaneAddr_1_activeMask = coreReq_Q.io_deq_bits_perLaneAddr_1_activeMask @[DCache.scala 188:27 191:29]
    node mshrMissReqTI_perLaneAddr_1_blockOffset = coreReq_Q.io_deq_bits_perLaneAddr_1_blockOffset @[DCache.scala 188:27 191:29]
    node MshrAccess_io_missReq_bits_targetInfo_lo_hi_hi = cat(mshrMissReqTI_perLaneAddr_1_activeMask, mshrMissReqTI_perLaneAddr_1_blockOffset) @[DCache.scala 194:58]
    node mshrMissReqTI_perLaneAddr_1_wordOffset1H = coreReq_Q.io_deq_bits_perLaneAddr_1_wordOffset1H @[DCache.scala 188:27 191:29]
    node MshrAccess_io_missReq_bits_targetInfo_lo_hi = cat(MshrAccess_io_missReq_bits_targetInfo_lo_hi_hi, mshrMissReqTI_perLaneAddr_1_wordOffset1H) @[DCache.scala 194:58]
    node MshrAccess_io_missReq_bits_targetInfo_lo = cat(MshrAccess_io_missReq_bits_targetInfo_lo_hi, MshrAccess_io_missReq_bits_targetInfo_lo_lo) @[DCache.scala 194:58]
    node mshrMissReqTI_perLaneAddr_2_activeMask = coreReq_Q.io_deq_bits_perLaneAddr_2_activeMask @[DCache.scala 188:27 191:29]
    node mshrMissReqTI_perLaneAddr_2_blockOffset = coreReq_Q.io_deq_bits_perLaneAddr_2_blockOffset @[DCache.scala 188:27 191:29]
    node MshrAccess_io_missReq_bits_targetInfo_hi_lo_hi = cat(mshrMissReqTI_perLaneAddr_2_activeMask, mshrMissReqTI_perLaneAddr_2_blockOffset) @[DCache.scala 194:58]
    node mshrMissReqTI_perLaneAddr_2_wordOffset1H = coreReq_Q.io_deq_bits_perLaneAddr_2_wordOffset1H @[DCache.scala 188:27 191:29]
    node MshrAccess_io_missReq_bits_targetInfo_hi_lo = cat(MshrAccess_io_missReq_bits_targetInfo_hi_lo_hi, mshrMissReqTI_perLaneAddr_2_wordOffset1H) @[DCache.scala 194:58]
    node mshrMissReqTI_perLaneAddr_3_blockOffset = coreReq_Q.io_deq_bits_perLaneAddr_3_blockOffset @[DCache.scala 188:27 191:29]
    node mshrMissReqTI_perLaneAddr_3_wordOffset1H = coreReq_Q.io_deq_bits_perLaneAddr_3_wordOffset1H @[DCache.scala 188:27 191:29]
    node MshrAccess_io_missReq_bits_targetInfo_hi_hi_lo = cat(mshrMissReqTI_perLaneAddr_3_blockOffset, mshrMissReqTI_perLaneAddr_3_wordOffset1H) @[DCache.scala 194:58]
    node mshrMissReqTI_instrId = coreReq_Q.io_deq_bits_instrId @[DCache.scala 188:27 190:25]
    node mshrMissReqTI_perLaneAddr_3_activeMask = coreReq_Q.io_deq_bits_perLaneAddr_3_activeMask @[DCache.scala 188:27 191:29]
    node MshrAccess_io_missReq_bits_targetInfo_hi_hi_hi = cat(mshrMissReqTI_instrId, mshrMissReqTI_perLaneAddr_3_activeMask) @[DCache.scala 194:58]
    node MshrAccess_io_missReq_bits_targetInfo_hi_hi = cat(MshrAccess_io_missReq_bits_targetInfo_hi_hi_hi, MshrAccess_io_missReq_bits_targetInfo_hi_hi_lo) @[DCache.scala 194:58]
    node MshrAccess_io_missReq_bits_targetInfo_hi = cat(MshrAccess_io_missReq_bits_targetInfo_hi_hi, MshrAccess_io_missReq_bits_targetInfo_hi_lo) @[DCache.scala 194:58]
    node _MshrAccess_io_missReq_bits_targetInfo_T = cat(MshrAccess_io_missReq_bits_targetInfo_hi, MshrAccess_io_missReq_bits_targetInfo_lo) @[DCache.scala 194:58]
    node writeMissReq_a_addr_hi = cat(coreReq_Q.io_deq_bits_tag, coreReq_Q.io_deq_bits_setIdx) @[Cat.scala 31:58]
    node _writeMissReq_a_addr_T = cat(writeMissReq_a_addr_hi, UInt<4>("h0")) @[Cat.scala 31:58]
    node readMissReq_a_source_hi = cat(UInt<1>("h1"), MshrAccess.io_probeOut_st1_a_source) @[Cat.scala 31:58]
    node _readMissReq_a_source_T = cat(readMissReq_a_source_hi, coreReq_Q.io_deq_bits_setIdx) @[Cat.scala 31:58]
    node readMissReq_a_addr_hi = cat(coreReq_Q.io_deq_bits_tag, coreReq_Q.io_deq_bits_setIdx) @[Cat.scala 31:58]
    node _readMissReq_a_addr_T = cat(readMissReq_a_addr_hi, UInt<4>("h0")) @[Cat.scala 31:58]
    node writeMissReq_a_opcode = UInt<3>("h1") @[DCache.scala 198:26 200:25]
    node readMissReq_a_opcode = UInt<3>("h4") @[DCache.scala 199:25 209:24]
    node _missMemReq_T_a_opcode = mux(writeMiss_st1, writeMissReq_a_opcode, readMissReq_a_opcode) @[DCache.scala 218:20]
    node writeMissReq_a_param = UInt<3>("h0") @[DCache.scala 198:26 201:24]
    node readMissReq_a_param = UInt<3>("h0") @[DCache.scala 199:25 210:23]
    node _missMemReq_T_a_param = mux(writeMiss_st1, writeMissReq_a_param, readMissReq_a_param) @[DCache.scala 218:20]
    node writeMissReq_a_addr = _writeMissReq_a_addr_T @[DCache.scala 198:26 203:23]
    node readMissReq_a_addr = _readMissReq_a_addr_T @[DCache.scala 199:25 212:22]
    node _missMemReq_T_a_addr = mux(writeMiss_st1, writeMissReq_a_addr, readMissReq_a_addr) @[DCache.scala 218:20]
    node writeMissReq_a_data_0 = coreReq_Q.io_deq_bits_data_0 @[DCache.scala 198:26 205:23]
    node readMissReq_a_data_0 = validif(UInt<1>("h0"), UInt<32>("h0"))
    node _missMemReq_T_a_data_0 = mux(writeMiss_st1, writeMissReq_a_data_0, readMissReq_a_data_0) @[DCache.scala 218:20]
    node writeMissReq_a_data_1 = coreReq_Q.io_deq_bits_data_1 @[DCache.scala 198:26 205:23]
    node readMissReq_a_data_1 = validif(UInt<1>("h0"), UInt<32>("h0"))
    node _missMemReq_T_a_data_1 = mux(writeMiss_st1, writeMissReq_a_data_1, readMissReq_a_data_1) @[DCache.scala 218:20]
    node writeMissReq_a_data_2 = coreReq_Q.io_deq_bits_data_2 @[DCache.scala 198:26 205:23]
    node readMissReq_a_data_2 = validif(UInt<1>("h0"), UInt<32>("h0"))
    node _missMemReq_T_a_data_2 = mux(writeMiss_st1, writeMissReq_a_data_2, readMissReq_a_data_2) @[DCache.scala 218:20]
    node writeMissReq_a_data_3 = coreReq_Q.io_deq_bits_data_3 @[DCache.scala 198:26 205:23]
    node readMissReq_a_data_3 = validif(UInt<1>("h0"), UInt<32>("h0"))
    node _missMemReq_T_a_data_3 = mux(writeMiss_st1, writeMissReq_a_data_3, readMissReq_a_data_3) @[DCache.scala 218:20]
    node writeMissReq_a_mask_0 = coreReq_Q.io_deq_bits_perLaneAddr_0_activeMask @[DCache.scala 198:26 204:23]
    node readMissReq_a_mask_0 = coreReq_Q.io_deq_bits_perLaneAddr_0_activeMask @[DCache.scala 199:25 213:22]
    node _missMemReq_T_a_mask_0 = mux(writeMiss_st1, writeMissReq_a_mask_0, readMissReq_a_mask_0) @[DCache.scala 218:20]
    node writeMissReq_a_mask_1 = coreReq_Q.io_deq_bits_perLaneAddr_1_activeMask @[DCache.scala 198:26 204:23]
    node readMissReq_a_mask_1 = coreReq_Q.io_deq_bits_perLaneAddr_1_activeMask @[DCache.scala 199:25 213:22]
    node _missMemReq_T_a_mask_1 = mux(writeMiss_st1, writeMissReq_a_mask_1, readMissReq_a_mask_1) @[DCache.scala 218:20]
    node writeMissReq_a_mask_2 = coreReq_Q.io_deq_bits_perLaneAddr_2_activeMask @[DCache.scala 198:26 204:23]
    node readMissReq_a_mask_2 = coreReq_Q.io_deq_bits_perLaneAddr_2_activeMask @[DCache.scala 199:25 213:22]
    node _missMemReq_T_a_mask_2 = mux(writeMiss_st1, writeMissReq_a_mask_2, readMissReq_a_mask_2) @[DCache.scala 218:20]
    node writeMissReq_a_mask_3 = coreReq_Q.io_deq_bits_perLaneAddr_3_activeMask @[DCache.scala 198:26 204:23]
    node readMissReq_a_mask_3 = coreReq_Q.io_deq_bits_perLaneAddr_3_activeMask @[DCache.scala 199:25 213:22]
    node _missMemReq_T_a_mask_3 = mux(writeMiss_st1, writeMissReq_a_mask_3, readMissReq_a_mask_3) @[DCache.scala 218:20]
    node writeMissReq_a_source = validif(UInt<1>("h0"), UInt<7>("h0"))
    node readMissReq_a_source = pad(_readMissReq_a_source_T, 7) @[DCache.scala 199:25 211:24]
    node _missMemReq_T_a_source = mux(writeMiss_st1, writeMissReq_a_source, readMissReq_a_source) @[DCache.scala 218:20]
    node writeMissReq_hasCoreRsp = UInt<1>("h1") @[DCache.scala 198:26 206:27]
    node readMissReq_hasCoreRsp = UInt<1>("h0") @[DCache.scala 199:25 215:26]
    node _missMemReq_T_hasCoreRsp = mux(writeMiss_st1, writeMissReq_hasCoreRsp, readMissReq_hasCoreRsp) @[DCache.scala 218:20]
    node writeMissReq_coreRspInstrId = pad(coreReq_Q.io_deq_bits_instrId, 32) @[DCache.scala 198:26 207:31]
    node readMissReq_coreRspInstrId = validif(UInt<1>("h0"), UInt<32>("h0"))
    node _missMemReq_T_coreRspInstrId = mux(writeMiss_st1, writeMissReq_coreRspInstrId, readMissReq_coreRspInstrId) @[DCache.scala 218:20]
    node _DataAccessWriteHitSRAMWReq_0_setIdx_T = cat(coreReq_Q.io_deq_bits_setIdx, TagAccess.io_waymaskHit_st1) @[Cat.scala 31:58]
    node _DataAccessWriteHitSRAMWReq_1_setIdx_T = cat(coreReq_Q.io_deq_bits_setIdx, TagAccess.io_waymaskHit_st1) @[Cat.scala 31:58]
    node _DataAccessWriteHitSRAMWReq_2_setIdx_T = cat(coreReq_Q.io_deq_bits_setIdx, TagAccess.io_waymaskHit_st1) @[Cat.scala 31:58]
    node _DataAccessWriteHitSRAMWReq_3_setIdx_T = cat(coreReq_Q.io_deq_bits_setIdx, TagAccess.io_waymaskHit_st1) @[Cat.scala 31:58]
    node _GEN_8 = validif(eq(UInt<1>("h0"), getBankEn.io_perBankBlockIdx_0), coreReq_Q.io_deq_bits_perLaneAddr_0_wordOffset1H) @[DCache.scala 230:{47,47}]
    node _GEN_9 = mux(eq(UInt<1>("h1"), getBankEn.io_perBankBlockIdx_0), coreReq_Q.io_deq_bits_perLaneAddr_1_wordOffset1H, _GEN_8) @[DCache.scala 230:{47,47}]
    node _GEN_10 = mux(eq(UInt<2>("h2"), getBankEn.io_perBankBlockIdx_0), coreReq_Q.io_deq_bits_perLaneAddr_2_wordOffset1H, _GEN_9) @[DCache.scala 230:{47,47}]
    node _GEN_11 = mux(eq(UInt<2>("h3"), getBankEn.io_perBankBlockIdx_0), coreReq_Q.io_deq_bits_perLaneAddr_3_wordOffset1H, _GEN_10) @[DCache.scala 230:{47,47}]
    node _GEN_12 = validif(eq(UInt<1>("h0"), getBankEn.io_perBankBlockIdx_0), coreReq_Q.io_deq_bits_data_0)
    node _GEN_13 = mux(eq(UInt<1>("h1"), getBankEn.io_perBankBlockIdx_0), coreReq_Q.io_deq_bits_data_1, _GEN_12)
    node _GEN_14 = mux(eq(UInt<2>("h2"), getBankEn.io_perBankBlockIdx_0), coreReq_Q.io_deq_bits_data_2, _GEN_13)
    node _GEN_15 = mux(eq(UInt<2>("h3"), getBankEn.io_perBankBlockIdx_0), coreReq_Q.io_deq_bits_data_3, _GEN_14)
    node _coreReq_Q_io_deq_bits_data_getBankEn_io_perBankBlockIdx_0 = _GEN_15
    node _WIRE_1 = _coreReq_Q_io_deq_bits_data_getBankEn_io_perBankBlockIdx_0
    node _T = bits(_WIRE_1, 7, 0) @[DCache.scala 231:101]
    node _T_1 = bits(_WIRE_1, 15, 8) @[DCache.scala 231:101]
    node _T_2 = bits(_WIRE_1, 23, 16) @[DCache.scala 231:101]
    node _T_3 = bits(_WIRE_1, 31, 24) @[DCache.scala 231:101]
    node _GEN_16 = validif(eq(UInt<1>("h0"), getBankEn.io_perBankBlockIdx_1), coreReq_Q.io_deq_bits_perLaneAddr_0_wordOffset1H) @[DCache.scala 230:{47,47}]
    node _GEN_17 = mux(eq(UInt<1>("h1"), getBankEn.io_perBankBlockIdx_1), coreReq_Q.io_deq_bits_perLaneAddr_1_wordOffset1H, _GEN_16) @[DCache.scala 230:{47,47}]
    node _GEN_18 = mux(eq(UInt<2>("h2"), getBankEn.io_perBankBlockIdx_1), coreReq_Q.io_deq_bits_perLaneAddr_2_wordOffset1H, _GEN_17) @[DCache.scala 230:{47,47}]
    node _GEN_19 = mux(eq(UInt<2>("h3"), getBankEn.io_perBankBlockIdx_1), coreReq_Q.io_deq_bits_perLaneAddr_3_wordOffset1H, _GEN_18) @[DCache.scala 230:{47,47}]
    node _GEN_20 = validif(eq(UInt<1>("h0"), getBankEn.io_perBankBlockIdx_1), coreReq_Q.io_deq_bits_data_0)
    node _GEN_21 = mux(eq(UInt<1>("h1"), getBankEn.io_perBankBlockIdx_1), coreReq_Q.io_deq_bits_data_1, _GEN_20)
    node _GEN_22 = mux(eq(UInt<2>("h2"), getBankEn.io_perBankBlockIdx_1), coreReq_Q.io_deq_bits_data_2, _GEN_21)
    node _GEN_23 = mux(eq(UInt<2>("h3"), getBankEn.io_perBankBlockIdx_1), coreReq_Q.io_deq_bits_data_3, _GEN_22)
    node _coreReq_Q_io_deq_bits_data_getBankEn_io_perBankBlockIdx_1 = _GEN_23
    node _WIRE_3 = _coreReq_Q_io_deq_bits_data_getBankEn_io_perBankBlockIdx_1
    node _T_4 = bits(_WIRE_3, 7, 0) @[DCache.scala 231:101]
    node _T_5 = bits(_WIRE_3, 15, 8) @[DCache.scala 231:101]
    node _T_6 = bits(_WIRE_3, 23, 16) @[DCache.scala 231:101]
    node _T_7 = bits(_WIRE_3, 31, 24) @[DCache.scala 231:101]
    node _GEN_24 = validif(eq(UInt<1>("h0"), getBankEn.io_perBankBlockIdx_2), coreReq_Q.io_deq_bits_perLaneAddr_0_wordOffset1H) @[DCache.scala 230:{47,47}]
    node _GEN_25 = mux(eq(UInt<1>("h1"), getBankEn.io_perBankBlockIdx_2), coreReq_Q.io_deq_bits_perLaneAddr_1_wordOffset1H, _GEN_24) @[DCache.scala 230:{47,47}]
    node _GEN_26 = mux(eq(UInt<2>("h2"), getBankEn.io_perBankBlockIdx_2), coreReq_Q.io_deq_bits_perLaneAddr_2_wordOffset1H, _GEN_25) @[DCache.scala 230:{47,47}]
    node _GEN_27 = mux(eq(UInt<2>("h3"), getBankEn.io_perBankBlockIdx_2), coreReq_Q.io_deq_bits_perLaneAddr_3_wordOffset1H, _GEN_26) @[DCache.scala 230:{47,47}]
    node _GEN_28 = validif(eq(UInt<1>("h0"), getBankEn.io_perBankBlockIdx_2), coreReq_Q.io_deq_bits_data_0)
    node _GEN_29 = mux(eq(UInt<1>("h1"), getBankEn.io_perBankBlockIdx_2), coreReq_Q.io_deq_bits_data_1, _GEN_28)
    node _GEN_30 = mux(eq(UInt<2>("h2"), getBankEn.io_perBankBlockIdx_2), coreReq_Q.io_deq_bits_data_2, _GEN_29)
    node _GEN_31 = mux(eq(UInt<2>("h3"), getBankEn.io_perBankBlockIdx_2), coreReq_Q.io_deq_bits_data_3, _GEN_30)
    node _coreReq_Q_io_deq_bits_data_getBankEn_io_perBankBlockIdx_2 = _GEN_31
    node _WIRE_5 = _coreReq_Q_io_deq_bits_data_getBankEn_io_perBankBlockIdx_2
    node _T_8 = bits(_WIRE_5, 7, 0) @[DCache.scala 231:101]
    node _T_9 = bits(_WIRE_5, 15, 8) @[DCache.scala 231:101]
    node _T_10 = bits(_WIRE_5, 23, 16) @[DCache.scala 231:101]
    node _T_11 = bits(_WIRE_5, 31, 24) @[DCache.scala 231:101]
    node _GEN_32 = validif(eq(UInt<1>("h0"), getBankEn.io_perBankBlockIdx_3), coreReq_Q.io_deq_bits_perLaneAddr_0_wordOffset1H) @[DCache.scala 230:{47,47}]
    node _GEN_33 = mux(eq(UInt<1>("h1"), getBankEn.io_perBankBlockIdx_3), coreReq_Q.io_deq_bits_perLaneAddr_1_wordOffset1H, _GEN_32) @[DCache.scala 230:{47,47}]
    node _GEN_34 = mux(eq(UInt<2>("h2"), getBankEn.io_perBankBlockIdx_3), coreReq_Q.io_deq_bits_perLaneAddr_2_wordOffset1H, _GEN_33) @[DCache.scala 230:{47,47}]
    node _GEN_35 = mux(eq(UInt<2>("h3"), getBankEn.io_perBankBlockIdx_3), coreReq_Q.io_deq_bits_perLaneAddr_3_wordOffset1H, _GEN_34) @[DCache.scala 230:{47,47}]
    node _GEN_36 = validif(eq(UInt<1>("h0"), getBankEn.io_perBankBlockIdx_3), coreReq_Q.io_deq_bits_data_0)
    node _GEN_37 = mux(eq(UInt<1>("h1"), getBankEn.io_perBankBlockIdx_3), coreReq_Q.io_deq_bits_data_1, _GEN_36)
    node _GEN_38 = mux(eq(UInt<2>("h2"), getBankEn.io_perBankBlockIdx_3), coreReq_Q.io_deq_bits_data_2, _GEN_37)
    node _GEN_39 = mux(eq(UInt<2>("h3"), getBankEn.io_perBankBlockIdx_3), coreReq_Q.io_deq_bits_data_3, _GEN_38)
    node _coreReq_Q_io_deq_bits_data_getBankEn_io_perBankBlockIdx_3 = _GEN_39
    node _WIRE_7 = _coreReq_Q_io_deq_bits_data_getBankEn_io_perBankBlockIdx_3
    node _T_12 = bits(_WIRE_7, 7, 0) @[DCache.scala 231:101]
    node _T_13 = bits(_WIRE_7, 15, 8) @[DCache.scala 231:101]
    node _T_14 = bits(_WIRE_7, 23, 16) @[DCache.scala 231:101]
    node _T_15 = bits(_WIRE_7, 31, 24) @[DCache.scala 231:101]
    node _DataAccessReadHitSRAMRReq_0_setIdx_T = cat(coreReq_Q.io_deq_bits_setIdx, TagAccess.io_waymaskHit_st1) @[Cat.scala 31:58]
    node _DataAccessReadHitSRAMRReq_1_setIdx_T = cat(coreReq_Q.io_deq_bits_setIdx, TagAccess.io_waymaskHit_st1) @[Cat.scala 31:58]
    node _DataAccessReadHitSRAMRReq_2_setIdx_T = cat(coreReq_Q.io_deq_bits_setIdx, TagAccess.io_waymaskHit_st1) @[Cat.scala 31:58]
    node _DataAccessReadHitSRAMRReq_3_setIdx_T = cat(coreReq_Q.io_deq_bits_setIdx, TagAccess.io_waymaskHit_st1) @[Cat.scala 31:58]
    node _waitMSHRCoreRsp_st1_T = and(coreReq_st1_valid, coreReqControl_st1_isWaitMSHR) @[DCache.scala 276:47]
    node waitMSHRCoreRsp_st1 = and(_waitMSHRCoreRsp_st1_T, MshrAccess.io_empty) @[DCache.scala 276:80]
    node _fluCoreRsp_st1_T = and(coreReq_st1_valid, coreReqControl_st1_isFlush) @[DCache.scala 277:42]
    node _fluCoreRsp_st1_T_1 = eq(coreReqTagHasDirty_st1, UInt<1>("h0")) @[DCache.scala 278:5]
    node _fluCoreRsp_st1_T_2 = and(_fluCoreRsp_st1_T, _fluCoreRsp_st1_T_1) @[DCache.scala 277:72]
    node fluCoreRsp_st1 = and(_fluCoreRsp_st1_T_2, WshrAccess.io_empty) @[DCache.scala 278:29]
    node _invCOreRsp_st1_T = and(coreReq_st1_valid, coreReqControl_st1_isInvalidate) @[DCache.scala 279:42]
    node _invCOreRsp_st1_T_1 = eq(coreReqTagHasDirty_st1, UInt<1>("h0")) @[DCache.scala 280:5]
    node _invCOreRsp_st1_T_2 = and(_invCOreRsp_st1_T, _invCOreRsp_st1_T_1) @[DCache.scala 279:77]
    node _invCOreRsp_st1_T_3 = and(_invCOreRsp_st1_T_2, MshrAccess.io_empty) @[DCache.scala 280:29]
    node invCOreRsp_st1 = and(_invCOreRsp_st1_T_3, WshrAccess.io_empty) @[DCache.scala 280:52]
    node _InvOrFluMemReqValid_st1_T = or(coreReqControl_st1_isInvalidate, coreReqControl_st1_isFlush) @[DCache.scala 282:87]
    node _InvOrFluMemReqValid_st1_T_1 = and(coreReq_st1_valid, _InvOrFluMemReqValid_st1_T) @[DCache.scala 282:51]
    node InvOrFluMemReqValid_st1 = and(_InvOrFluMemReqValid_st1_T_1, coreReqTagHasDirty_st1) @[DCache.scala 282:118]
    reg InvOrFluMemReq_a_addr_REG : UInt<2>, clock with :
      reset => (UInt<1>("h0"), InvOrFluMemReq_a_addr_REG) @[DCache.scala 289:12]
    node InvOrFluMemReq_a_addr_hi = cat(TagAccess.io_dirtyTag_st1, InvOrFluMemReq_a_addr_REG) @[Cat.scala 31:58]
    node _InvOrFluMemReq_a_addr_T = cat(InvOrFluMemReq_a_addr_hi, UInt<4>("h0")) @[Cat.scala 31:58]
    node _TagAccess_io_invalidateAll_T = and(coreReq_st1_valid, coreReqControl_st1_isInvalidate) @[DCache.scala 295:51]
    node _TagAccess_io_invalidateAll_T_1 = eq(coreReqTagHasDirty_st1, UInt<1>("h0")) @[DCache.scala 295:89]
    node _TagAccess_io_invalidateAll_T_2 = and(_TagAccess_io_invalidateAll_T, _TagAccess_io_invalidateAll_T_1) @[DCache.scala 295:86]
    reg tagReqValidCtrl : UInt<1>, clock with :
      reset => (UInt<1>("h0"), tagReqValidCtrl) @[DCache.scala 300:32]
    reg tagReqReadyCtrl : UInt<1>, clock with :
      reset => (UInt<1>("h0"), tagReqReadyCtrl) @[DCache.scala 301:32]
    reg tagReplaceStatus : UInt<1>, clock with :
      reset => (UInt<1>("h0"), tagReplaceStatus) @[DCache.scala 333:33]
    node _T_40 = eq(tagReplaceStatus, UInt<1>("h0")) @[DCache.scala 344:25]
    node _tagAllocateWriteReady_T = eq(TagAccess.io_needReplace, UInt<1>("h0")) @[DCache.scala 345:30]
    node _GEN_59 = mux(_T_40, _tagAllocateWriteReady_T, MemReqArb.io_in_0_ready) @[DCache.scala 344:37 345:27 347:27]
    node tagAllocateWriteReady = _GEN_59 @[DCache.scala 302:35]
    node tagAllocateWriteReady_mod = mux(tagReqReadyCtrl, UInt<1>("h1"), tagAllocateWriteReady) @[DCache.scala 303:38]
    node tagAllocateWriteFire = and(TagAccess.io_allocateWrite_valid, tagAllocateWriteReady_mod) @[DCache.scala 304:69]
    node _T_28 = and(memRsp_Q.io_deq_ready, memRsp_Q.io_deq_valid) @[Decoupled.scala 50:35]
    node _T_29 = eq(_T_28, UInt<1>("h0")) @[DCache.scala 305:32]
    node _T_30 = and(tagAllocateWriteFire, _T_29) @[DCache.scala 305:29]
    node _T_31 = eq(tagReqValidCtrl, UInt<1>("h0")) @[DCache.scala 307:14]
    node _T_32 = and(memRsp_Q.io_deq_ready, memRsp_Q.io_deq_valid) @[Decoupled.scala 50:35]
    node _T_33 = and(_T_31, _T_32) @[DCache.scala 307:31]
    node _GEN_52 = mux(_T_33, UInt<1>("h1"), tagReqValidCtrl) @[DCache.scala 307:56 308:21 300:32]
    node _GEN_53 = mux(_T_30, UInt<1>("h0"), _GEN_52) @[DCache.scala 305:55 306:21]
    node _T_34 = and(memRsp_Q.io_deq_ready, memRsp_Q.io_deq_valid) @[Decoupled.scala 50:35]
    node _T_35 = eq(_T_34, UInt<1>("h0")) @[DCache.scala 310:32]
    node _T_36 = and(tagAllocateWriteFire, _T_35) @[DCache.scala 310:29]
    node _T_37 = and(memRsp_Q.io_deq_ready, memRsp_Q.io_deq_valid) @[Decoupled.scala 50:35]
    node _T_38 = and(tagReqReadyCtrl, _T_37) @[DCache.scala 312:30]
    node _GEN_54 = mux(_T_38, UInt<1>("h0"), tagReqReadyCtrl) @[DCache.scala 312:55 313:21 301:32]
    node _GEN_55 = mux(_T_36, UInt<1>("h1"), _GEN_54) @[DCache.scala 310:55 311:21]
    node memRspIsWrite = eq(memRsp_Q.io_deq_bits_d_opcode, UInt<1>("h0")) @[DCache.scala 316:59]
    node memRspIsRead = eq(memRsp_Q.io_deq_bits_d_opcode, UInt<1>("h1")) @[DCache.scala 317:58]
    node _memRsp_Q_io_deq_ready_T = and(memRspIsRead, tagAllocateWriteReady_mod) @[DCache.scala 319:19]
    node _memRsp_Q_io_deq_ready_T_1 = and(_memRsp_Q_io_deq_ready_T, MshrAccess.io_missRspIn_ready) @[DCache.scala 319:48]
    node _memRsp_Q_io_deq_ready_T_2 = and(_memRsp_Q_io_deq_ready_T_1, coreRsp_Q.io_enq_ready) @[DCache.scala 319:81]
    node _memRsp_Q_io_deq_ready_T_3 = or(memRspIsWrite, _memRsp_Q_io_deq_ready_T_2) @[DCache.scala 318:42]
    node _WshrAccess_io_popReq_valid_T = and(memRsp_Q.io_deq_valid, memRspIsWrite) @[DCache.scala 321:55]
    node _WshrAccess_io_popReq_bits_T = bits(memRsp_Q.io_deq_bits_d_source, 3, 2) @[DCache.scala 322:61]
    reg memRsp_st1_d_opcode : UInt<3>, clock with :
      reset => (UInt<1>("h0"), memRsp_st1_d_opcode) @[DCache.scala 325:23]
    reg memRsp_st1_d_source : UInt<7>, clock with :
      reset => (UInt<1>("h0"), memRsp_st1_d_source) @[DCache.scala 325:23]
    reg memRsp_st1_d_addr : UInt<32>, clock with :
      reset => (UInt<1>("h0"), memRsp_st1_d_addr) @[DCache.scala 325:23]
    reg memRsp_st1_d_data_0 : UInt<32>, clock with :
      reset => (UInt<1>("h0"), memRsp_st1_d_data_0) @[DCache.scala 325:23]
    reg memRsp_st1_d_data_1 : UInt<32>, clock with :
      reset => (UInt<1>("h0"), memRsp_st1_d_data_1) @[DCache.scala 325:23]
    reg memRsp_st1_d_data_2 : UInt<32>, clock with :
      reset => (UInt<1>("h0"), memRsp_st1_d_data_2) @[DCache.scala 325:23]
    reg memRsp_st1_d_data_3 : UInt<32>, clock with :
      reset => (UInt<1>("h0"), memRsp_st1_d_data_3) @[DCache.scala 325:23]
    node _T_39 = eq(tagReplaceStatus, UInt<1>("h0")) @[DCache.scala 334:25]
    node _GEN_56 = mux(TagAccess.io_needReplace, UInt<1>("h1"), tagReplaceStatus) @[DCache.scala 335:39 336:24 333:33]
    node _GEN_57 = mux(MemReqArb.io_in_0_ready, UInt<1>("h0"), tagReplaceStatus) @[DCache.scala 339:36 340:24 333:33]
    node _GEN_58 = mux(_T_39, _GEN_56, _GEN_57) @[DCache.scala 334:37]
    node missRspSetIdx_st1 = bits(memRsp_st1_d_source, 1, 0) @[DCache.scala 350:46]
    reg dataReplaceReadValid_REG : UInt<1>, clock with :
      reset => (UInt<1>("h0"), dataReplaceReadValid_REG) @[DCache.scala 351:37]
    node _dataReplaceReadValid_T = eq(tagReplaceStatus, UInt<1>("h0")) @[DCache.scala 352:22]
    node _dataReplaceReadValid_T_1 = and(dataReplaceReadValid_REG, _dataReplaceReadValid_T) @[DCache.scala 351:72]
    node dataReplaceReadValid = and(_dataReplaceReadValid_T_1, TagAccess.io_needReplace) @[DCache.scala 352:34]
    node _DataAccessReplaceReadSRAMRReq_0_setIdx_T = cat(missRspSetIdx_st1, TagAccess.io_waymaskReplacement_st1) @[Cat.scala 31:58]
    node _DataAccessReplaceReadSRAMRReq_1_setIdx_T = cat(missRspSetIdx_st1, TagAccess.io_waymaskReplacement_st1) @[Cat.scala 31:58]
    node _DataAccessReplaceReadSRAMRReq_2_setIdx_T = cat(missRspSetIdx_st1, TagAccess.io_waymaskReplacement_st1) @[Cat.scala 31:58]
    node _DataAccessReplaceReadSRAMRReq_3_setIdx_T = cat(missRspSetIdx_st1, TagAccess.io_waymaskReplacement_st1) @[Cat.scala 31:58]
    reg dataFillVaild_REG : UInt<1>, clock with :
      reset => (UInt<1>("h0"), dataFillVaild_REG) @[DCache.scala 357:30]
    node _dataFillVaild_T = eq(tagReplaceStatus, UInt<1>("h0")) @[DCache.scala 358:22]
    node _dataFillVaild_T_1 = and(dataFillVaild_REG, _dataFillVaild_T) @[DCache.scala 357:65]
    node _dataFillVaild_T_2 = eq(TagAccess.io_needReplace, UInt<1>("h0")) @[DCache.scala 359:5]
    node dataFillVaild = and(_dataFillVaild_T_1, _dataFillVaild_T_2) @[DCache.scala 358:34]
    node _DataAccessMissRspSRAMWReq_0_setIdx_T = cat(missRspSetIdx_st1, TagAccess.io_waymaskReplacement_st1) @[Cat.scala 31:58]
    node _DataAccessMissRspSRAMWReq_1_setIdx_T = cat(missRspSetIdx_st1, TagAccess.io_waymaskReplacement_st1) @[Cat.scala 31:58]
    node _DataAccessMissRspSRAMWReq_2_setIdx_T = cat(missRspSetIdx_st1, TagAccess.io_waymaskReplacement_st1) @[Cat.scala 31:58]
    node _DataAccessMissRspSRAMWReq_3_setIdx_T = cat(missRspSetIdx_st1, TagAccess.io_waymaskReplacement_st1) @[Cat.scala 31:58]
    node _DataAccessMissRspSRAMWReq_0_waymask_T = mux(UInt<1>("h1"), UInt<4>("hf"), UInt<4>("h0")) @[Bitwise.scala 74:12]
    node _DataAccessMissRspSRAMWReq_1_waymask_T = mux(UInt<1>("h1"), UInt<4>("hf"), UInt<4>("h0")) @[Bitwise.scala 74:12]
    node _DataAccessMissRspSRAMWReq_2_waymask_T = mux(UInt<1>("h1"), UInt<4>("hf"), UInt<4>("h0")) @[Bitwise.scala 74:12]
    node _DataAccessMissRspSRAMWReq_3_waymask_T = mux(UInt<1>("h1"), UInt<4>("hf"), UInt<4>("h0")) @[Bitwise.scala 74:12]
    node _WIRE_10 = memRsp_st1_d_data_0
    node _T_41 = bits(_WIRE_10, 7, 0) @[DCache.scala 364:71]
    node _T_42 = bits(_WIRE_10, 15, 8) @[DCache.scala 364:71]
    node _T_43 = bits(_WIRE_10, 23, 16) @[DCache.scala 364:71]
    node _T_44 = bits(_WIRE_10, 31, 24) @[DCache.scala 364:71]
    node _WIRE_12 = memRsp_st1_d_data_1
    node _T_45 = bits(_WIRE_12, 7, 0) @[DCache.scala 364:71]
    node _T_46 = bits(_WIRE_12, 15, 8) @[DCache.scala 364:71]
    node _T_47 = bits(_WIRE_12, 23, 16) @[DCache.scala 364:71]
    node _T_48 = bits(_WIRE_12, 31, 24) @[DCache.scala 364:71]
    node _WIRE_14 = memRsp_st1_d_data_2
    node _T_49 = bits(_WIRE_14, 7, 0) @[DCache.scala 364:71]
    node _T_50 = bits(_WIRE_14, 15, 8) @[DCache.scala 364:71]
    node _T_51 = bits(_WIRE_14, 23, 16) @[DCache.scala 364:71]
    node _T_52 = bits(_WIRE_14, 31, 24) @[DCache.scala 364:71]
    node _WIRE_16 = memRsp_st1_d_data_3
    node _T_53 = bits(_WIRE_16, 7, 0) @[DCache.scala 364:71]
    node _T_54 = bits(_WIRE_16, 15, 8) @[DCache.scala 364:71]
    node _T_55 = bits(_WIRE_16, 23, 16) @[DCache.scala 364:71]
    node _T_56 = bits(_WIRE_16, 31, 24) @[DCache.scala 364:71]
    node _T_57 = and(memRsp_Q.io_deq_valid, memRspIsRead) @[DCache.scala 378:30]
    node _GEN_60 = mux(_T_57, memRsp_Q.io_deq_bits_d_opcode, memRsp_st1_d_opcode) @[DCache.scala 378:46 379:16 325:23]
    node _GEN_61 = mux(_T_57, memRsp_Q.io_deq_bits_d_source, memRsp_st1_d_source) @[DCache.scala 378:46 379:16 325:23]
    node _GEN_62 = mux(_T_57, memRsp_Q.io_deq_bits_d_addr, memRsp_st1_d_addr) @[DCache.scala 378:46 379:16 325:23]
    node _GEN_63 = mux(_T_57, memRsp_Q.io_deq_bits_d_data_0, memRsp_st1_d_data_0) @[DCache.scala 378:46 379:16 325:23]
    node _GEN_64 = mux(_T_57, memRsp_Q.io_deq_bits_d_data_1, memRsp_st1_d_data_1) @[DCache.scala 378:46 379:16 325:23]
    node _GEN_65 = mux(_T_57, memRsp_Q.io_deq_bits_d_data_2, memRsp_st1_d_data_2) @[DCache.scala 378:46 379:16 325:23]
    node _GEN_66 = mux(_T_57, memRsp_Q.io_deq_bits_d_data_3, memRsp_st1_d_data_3) @[DCache.scala 378:46 379:16 325:23]
    node _MshrAccess_io_missRspIn_valid_T = and(memRsp_Q.io_deq_valid, memRspIsRead) @[DCache.scala 383:58]
    node _MshrAccess_io_missRspIn_valid_T_1 = and(_MshrAccess_io_missRspIn_valid_T, coreRsp_Q.io_enq_ready) @[DCache.scala 383:74]
    node _MshrAccess_io_missRspIn_bits_instrId_T = bits(memRsp_Q.io_deq_bits_d_source, 3, 2) @[DCache.scala 384:72]
    node _TagAccess_io_allocateWrite_valid_T = and(memRsp_Q.io_deq_valid, memRspIsRead) @[DCache.scala 387:81]
    node _TagAccess_io_allocateWrite_valid_T_1 = mux(tagReqValidCtrl, _TagAccess_io_allocateWrite_valid_T, UInt<1>("h0")) @[DCache.scala 387:42]
    node _TagAccess_io_allocateWrite_bits_setIdx_T = bits(memRsp_Q.io_deq_bits_d_source, 1, 0) @[DCache.scala 388:74]
    node _missRspTI_st1_WIRE_1 = MshrAccess.io_missRspOut_bits_targetInfo
    node _missRspTI_st1_T = bits(_missRspTI_st1_WIRE_1, 3, 0) @[DCache.scala 393:69]
    node _missRspTI_st1_T_1 = bits(_missRspTI_st1_WIRE_1, 5, 4) @[DCache.scala 393:69]
    node _missRspTI_st1_T_2 = bits(_missRspTI_st1_WIRE_1, 6, 6) @[DCache.scala 393:69]
    node _missRspTI_st1_T_3 = bits(_missRspTI_st1_WIRE_1, 10, 7) @[DCache.scala 393:69]
    node _missRspTI_st1_T_4 = bits(_missRspTI_st1_WIRE_1, 12, 11) @[DCache.scala 393:69]
    node _missRspTI_st1_T_5 = bits(_missRspTI_st1_WIRE_1, 13, 13) @[DCache.scala 393:69]
    node _missRspTI_st1_T_6 = bits(_missRspTI_st1_WIRE_1, 17, 14) @[DCache.scala 393:69]
    node _missRspTI_st1_T_7 = bits(_missRspTI_st1_WIRE_1, 19, 18) @[DCache.scala 393:69]
    node _missRspTI_st1_T_8 = bits(_missRspTI_st1_WIRE_1, 20, 20) @[DCache.scala 393:69]
    node _missRspTI_st1_T_9 = bits(_missRspTI_st1_WIRE_1, 24, 21) @[DCache.scala 393:69]
    node _missRspTI_st1_T_10 = bits(_missRspTI_st1_WIRE_1, 26, 25) @[DCache.scala 393:69]
    node _missRspTI_st1_T_11 = bits(_missRspTI_st1_WIRE_1, 27, 27) @[DCache.scala 393:69]
    node _missRspTI_st1_T_12 = bits(_missRspTI_st1_WIRE_1, 30, 28) @[DCache.scala 393:69]
    node _TagAccess_io_allocateWriteData_st1_T = shr(MshrAccess.io_missRspOut_bits_blockAddr, 2) @[L1CacheParameters.scala 54:35]
    reg TagAccess_io_allocateWriteTagSRAMWValid_st1_REG : UInt<1>, clock with :
      reset => (UInt<1>("h0"), TagAccess_io_allocateWriteTagSRAMWValid_st1_REG) @[DCache.scala 397:57]
    node _TagAccess_io_allocateWriteTagSRAMWValid_st1_T = and(TagAccess_io_allocateWriteTagSRAMWValid_st1_REG, tagAllocateWriteReady) @[DCache.scala 397:92]
    node _DataAccessesRRsp_DataAccess_io_w_req_valid_T = and(writeHit_st1, getBankEn.io_perBankValid_0) @[DCache.scala 412:20]
    node _DataAccessesRRsp_DataAccess_io_w_req_valid_T_1 = mux(dataFillVaild, UInt<1>("h1"), _DataAccessesRRsp_DataAccess_io_w_req_valid_T) @[DCache.scala 410:37]
    node _DataAccessesRRsp_DataAccess_io_w_req_bits_T = and(memRsp_Q.io_deq_valid, memRspIsRead) @[DCache.scala 413:59]
    node DataAccessMissRspSRAMWReq_0_setIdx = bits(_DataAccessMissRspSRAMWReq_0_setIdx_T, 2, 0) @[DCache.scala 360:64 361:46]
    node DataAccessWriteHitSRAMWReq_0_setIdx = bits(_DataAccessWriteHitSRAMWReq_0_setIdx_T, 2, 0) @[DCache.scala 226:65 228:47]
    node _DataAccessesRRsp_DataAccess_io_w_req_bits_T_1_setIdx = mux(_DataAccessesRRsp_DataAccess_io_w_req_bits_T, DataAccessMissRspSRAMWReq_0_setIdx, DataAccessWriteHitSRAMWReq_0_setIdx) @[DCache.scala 413:36]
    node _WIRE_9_0 = _T_41 @[DCache.scala 364:{71,71}]
    node DataAccessMissRspSRAMWReq_0_data_0 = _WIRE_9_0 @[DCache.scala 360:64 364:39]
    node _WIRE__0 = _T @[DCache.scala 231:{101,101}]
    node DataAccessWriteHitSRAMWReq_0_data_0 = _WIRE__0 @[DCache.scala 226:65 231:40]
    node _DataAccessesRRsp_DataAccess_io_w_req_bits_T_1_data_0 = mux(_DataAccessesRRsp_DataAccess_io_w_req_bits_T, DataAccessMissRspSRAMWReq_0_data_0, DataAccessWriteHitSRAMWReq_0_data_0) @[DCache.scala 413:36]
    node _WIRE_9_1 = _T_42 @[DCache.scala 364:{71,71}]
    node DataAccessMissRspSRAMWReq_0_data_1 = _WIRE_9_1 @[DCache.scala 360:64 364:39]
    node _WIRE__1 = _T_1 @[DCache.scala 231:{101,101}]
    node DataAccessWriteHitSRAMWReq_0_data_1 = _WIRE__1 @[DCache.scala 226:65 231:40]
    node _DataAccessesRRsp_DataAccess_io_w_req_bits_T_1_data_1 = mux(_DataAccessesRRsp_DataAccess_io_w_req_bits_T, DataAccessMissRspSRAMWReq_0_data_1, DataAccessWriteHitSRAMWReq_0_data_1) @[DCache.scala 413:36]
    node _WIRE_9_2 = _T_43 @[DCache.scala 364:{71,71}]
    node DataAccessMissRspSRAMWReq_0_data_2 = _WIRE_9_2 @[DCache.scala 360:64 364:39]
    node _WIRE__2 = _T_2 @[DCache.scala 231:{101,101}]
    node DataAccessWriteHitSRAMWReq_0_data_2 = _WIRE__2 @[DCache.scala 226:65 231:40]
    node _DataAccessesRRsp_DataAccess_io_w_req_bits_T_1_data_2 = mux(_DataAccessesRRsp_DataAccess_io_w_req_bits_T, DataAccessMissRspSRAMWReq_0_data_2, DataAccessWriteHitSRAMWReq_0_data_2) @[DCache.scala 413:36]
    node _WIRE_9_3 = _T_44 @[DCache.scala 364:{71,71}]
    node DataAccessMissRspSRAMWReq_0_data_3 = _WIRE_9_3 @[DCache.scala 360:64 364:39]
    node _WIRE__3 = _T_3 @[DCache.scala 231:{101,101}]
    node DataAccessWriteHitSRAMWReq_0_data_3 = _WIRE__3 @[DCache.scala 226:65 231:40]
    node _DataAccessesRRsp_DataAccess_io_w_req_bits_T_1_data_3 = mux(_DataAccessesRRsp_DataAccess_io_w_req_bits_T, DataAccessMissRspSRAMWReq_0_data_3, DataAccessWriteHitSRAMWReq_0_data_3) @[DCache.scala 413:36]
    node DataAccessMissRspSRAMWReq_0_waymask = _DataAccessMissRspSRAMWReq_0_waymask_T @[DCache.scala 360:64 362:51]
    node _coreReq_Q_io_deq_bits_perLaneAddr_getBankEn_io_perBankBlockIdx_0_wordOffset1H = _GEN_11 @[DCache.scala 230:47]
    node DataAccessWriteHitSRAMWReq_0_waymask = _coreReq_Q_io_deq_bits_perLaneAddr_getBankEn_io_perBankBlockIdx_0_wordOffset1H @[DCache.scala 226:65 230:47]
    node _DataAccessesRRsp_DataAccess_io_w_req_bits_T_1_waymask = mux(_DataAccessesRRsp_DataAccess_io_w_req_bits_T, DataAccessMissRspSRAMWReq_0_waymask, DataAccessWriteHitSRAMWReq_0_waymask) @[DCache.scala 413:36]
    node _DataAccessesRRsp_DataAccess_io_r_req_valid_T = or(readHit_st1, dataReplaceReadValid) @[DCache.scala 414:46]
    node _DataAccessesRRsp_DataAccess_io_r_req_valid_T_1 = or(_DataAccessesRRsp_DataAccess_io_r_req_valid_T, dataAccessInvOrFluRValid) @[DCache.scala 414:70]
    node DataAccessInvOrFluSRAMRReq_0_setIdx = bits(_DataAccessInvOrFluSRAMRReq_0_setIdx_T, 2, 0) @[DCache.scala 175:40 178:47]
    node DataAccessReadHitSRAMRReq_0_setIdx = bits(_DataAccessReadHitSRAMRReq_0_setIdx_T, 2, 0) @[DCache.scala 235:39 236:46]
    node _GEN_67 = mux(dataAccessInvOrFluRValid, DataAccessInvOrFluSRAMRReq_0_setIdx, DataAccessReadHitSRAMRReq_0_setIdx) @[DCache.scala 417:41 418:32 420:32]
    node DataAccessReplaceReadSRAMRReq_0_setIdx = bits(_DataAccessReplaceReadSRAMRReq_0_setIdx_T, 2, 0) @[DCache.scala 354:43 355:50]
    node _GEN_68 = mux(dataReplaceReadValid, DataAccessReplaceReadSRAMRReq_0_setIdx, _GEN_67) @[DCache.scala 415:31 416:32]
    node DataAccessesRRsp_lo = cat(DataAccessesRRsp_DataAccess.io_r_resp_data_1, DataAccessesRRsp_DataAccess.io_r_resp_data_0) @[Cat.scala 31:58]
    node DataAccessesRRsp_hi = cat(DataAccessesRRsp_DataAccess.io_r_resp_data_3, DataAccessesRRsp_DataAccess.io_r_resp_data_2) @[Cat.scala 31:58]
    node DataAccessesRRsp_0 = cat(DataAccessesRRsp_hi, DataAccessesRRsp_lo) @[Cat.scala 31:58]
    node _DataAccessesRRsp_DataAccess_io_w_req_valid_T_2 = and(writeHit_st1, getBankEn.io_perBankValid_1) @[DCache.scala 412:20]
    node _DataAccessesRRsp_DataAccess_io_w_req_valid_T_3 = mux(dataFillVaild, UInt<1>("h1"), _DataAccessesRRsp_DataAccess_io_w_req_valid_T_2) @[DCache.scala 410:37]
    node _DataAccessesRRsp_DataAccess_io_w_req_bits_T_2 = and(memRsp_Q.io_deq_valid, memRspIsRead) @[DCache.scala 413:59]
    node DataAccessMissRspSRAMWReq_1_setIdx = bits(_DataAccessMissRspSRAMWReq_1_setIdx_T, 2, 0) @[DCache.scala 360:64 361:46]
    node DataAccessWriteHitSRAMWReq_1_setIdx = bits(_DataAccessWriteHitSRAMWReq_1_setIdx_T, 2, 0) @[DCache.scala 226:65 228:47]
    node _DataAccessesRRsp_DataAccess_io_w_req_bits_T_3_setIdx = mux(_DataAccessesRRsp_DataAccess_io_w_req_bits_T_2, DataAccessMissRspSRAMWReq_1_setIdx, DataAccessWriteHitSRAMWReq_1_setIdx) @[DCache.scala 413:36]
    node _WIRE_11_0 = _T_45 @[DCache.scala 364:{71,71}]
    node DataAccessMissRspSRAMWReq_1_data_0 = _WIRE_11_0 @[DCache.scala 360:64 364:39]
    node _WIRE_2_0 = _T_4 @[DCache.scala 231:{101,101}]
    node DataAccessWriteHitSRAMWReq_1_data_0 = _WIRE_2_0 @[DCache.scala 226:65 231:40]
    node _DataAccessesRRsp_DataAccess_io_w_req_bits_T_3_data_0 = mux(_DataAccessesRRsp_DataAccess_io_w_req_bits_T_2, DataAccessMissRspSRAMWReq_1_data_0, DataAccessWriteHitSRAMWReq_1_data_0) @[DCache.scala 413:36]
    node _WIRE_11_1 = _T_46 @[DCache.scala 364:{71,71}]
    node DataAccessMissRspSRAMWReq_1_data_1 = _WIRE_11_1 @[DCache.scala 360:64 364:39]
    node _WIRE_2_1 = _T_5 @[DCache.scala 231:{101,101}]
    node DataAccessWriteHitSRAMWReq_1_data_1 = _WIRE_2_1 @[DCache.scala 226:65 231:40]
    node _DataAccessesRRsp_DataAccess_io_w_req_bits_T_3_data_1 = mux(_DataAccessesRRsp_DataAccess_io_w_req_bits_T_2, DataAccessMissRspSRAMWReq_1_data_1, DataAccessWriteHitSRAMWReq_1_data_1) @[DCache.scala 413:36]
    node _WIRE_11_2 = _T_47 @[DCache.scala 364:{71,71}]
    node DataAccessMissRspSRAMWReq_1_data_2 = _WIRE_11_2 @[DCache.scala 360:64 364:39]
    node _WIRE_2_2 = _T_6 @[DCache.scala 231:{101,101}]
    node DataAccessWriteHitSRAMWReq_1_data_2 = _WIRE_2_2 @[DCache.scala 226:65 231:40]
    node _DataAccessesRRsp_DataAccess_io_w_req_bits_T_3_data_2 = mux(_DataAccessesRRsp_DataAccess_io_w_req_bits_T_2, DataAccessMissRspSRAMWReq_1_data_2, DataAccessWriteHitSRAMWReq_1_data_2) @[DCache.scala 413:36]
    node _WIRE_11_3 = _T_48 @[DCache.scala 364:{71,71}]
    node DataAccessMissRspSRAMWReq_1_data_3 = _WIRE_11_3 @[DCache.scala 360:64 364:39]
    node _WIRE_2_3 = _T_7 @[DCache.scala 231:{101,101}]
    node DataAccessWriteHitSRAMWReq_1_data_3 = _WIRE_2_3 @[DCache.scala 226:65 231:40]
    node _DataAccessesRRsp_DataAccess_io_w_req_bits_T_3_data_3 = mux(_DataAccessesRRsp_DataAccess_io_w_req_bits_T_2, DataAccessMissRspSRAMWReq_1_data_3, DataAccessWriteHitSRAMWReq_1_data_3) @[DCache.scala 413:36]
    node DataAccessMissRspSRAMWReq_1_waymask = _DataAccessMissRspSRAMWReq_1_waymask_T @[DCache.scala 360:64 362:51]
    node _coreReq_Q_io_deq_bits_perLaneAddr_getBankEn_io_perBankBlockIdx_1_wordOffset1H = _GEN_19 @[DCache.scala 230:47]
    node DataAccessWriteHitSRAMWReq_1_waymask = _coreReq_Q_io_deq_bits_perLaneAddr_getBankEn_io_perBankBlockIdx_1_wordOffset1H @[DCache.scala 226:65 230:47]
    node _DataAccessesRRsp_DataAccess_io_w_req_bits_T_3_waymask = mux(_DataAccessesRRsp_DataAccess_io_w_req_bits_T_2, DataAccessMissRspSRAMWReq_1_waymask, DataAccessWriteHitSRAMWReq_1_waymask) @[DCache.scala 413:36]
    node _DataAccessesRRsp_DataAccess_io_r_req_valid_T_2 = or(readHit_st1, dataReplaceReadValid) @[DCache.scala 414:46]
    node _DataAccessesRRsp_DataAccess_io_r_req_valid_T_3 = or(_DataAccessesRRsp_DataAccess_io_r_req_valid_T_2, dataAccessInvOrFluRValid) @[DCache.scala 414:70]
    node DataAccessInvOrFluSRAMRReq_1_setIdx = bits(_DataAccessInvOrFluSRAMRReq_1_setIdx_T, 2, 0) @[DCache.scala 175:40 178:47]
    node DataAccessReadHitSRAMRReq_1_setIdx = bits(_DataAccessReadHitSRAMRReq_1_setIdx_T, 2, 0) @[DCache.scala 235:39 236:46]
    node _GEN_69 = mux(dataAccessInvOrFluRValid, DataAccessInvOrFluSRAMRReq_1_setIdx, DataAccessReadHitSRAMRReq_1_setIdx) @[DCache.scala 417:41 418:32 420:32]
    node DataAccessReplaceReadSRAMRReq_1_setIdx = bits(_DataAccessReplaceReadSRAMRReq_1_setIdx_T, 2, 0) @[DCache.scala 354:43 355:50]
    node _GEN_70 = mux(dataReplaceReadValid, DataAccessReplaceReadSRAMRReq_1_setIdx, _GEN_69) @[DCache.scala 415:31 416:32]
    node DataAccessesRRsp_lo_1 = cat(DataAccessesRRsp_DataAccess_1.io_r_resp_data_1, DataAccessesRRsp_DataAccess_1.io_r_resp_data_0) @[Cat.scala 31:58]
    node DataAccessesRRsp_hi_1 = cat(DataAccessesRRsp_DataAccess_1.io_r_resp_data_3, DataAccessesRRsp_DataAccess_1.io_r_resp_data_2) @[Cat.scala 31:58]
    node DataAccessesRRsp_1 = cat(DataAccessesRRsp_hi_1, DataAccessesRRsp_lo_1) @[Cat.scala 31:58]
    node _DataAccessesRRsp_DataAccess_io_w_req_valid_T_4 = and(writeHit_st1, getBankEn.io_perBankValid_2) @[DCache.scala 412:20]
    node _DataAccessesRRsp_DataAccess_io_w_req_valid_T_5 = mux(dataFillVaild, UInt<1>("h1"), _DataAccessesRRsp_DataAccess_io_w_req_valid_T_4) @[DCache.scala 410:37]
    node _DataAccessesRRsp_DataAccess_io_w_req_bits_T_4 = and(memRsp_Q.io_deq_valid, memRspIsRead) @[DCache.scala 413:59]
    node DataAccessMissRspSRAMWReq_2_setIdx = bits(_DataAccessMissRspSRAMWReq_2_setIdx_T, 2, 0) @[DCache.scala 360:64 361:46]
    node DataAccessWriteHitSRAMWReq_2_setIdx = bits(_DataAccessWriteHitSRAMWReq_2_setIdx_T, 2, 0) @[DCache.scala 226:65 228:47]
    node _DataAccessesRRsp_DataAccess_io_w_req_bits_T_5_setIdx = mux(_DataAccessesRRsp_DataAccess_io_w_req_bits_T_4, DataAccessMissRspSRAMWReq_2_setIdx, DataAccessWriteHitSRAMWReq_2_setIdx) @[DCache.scala 413:36]
    node _WIRE_13_0 = _T_49 @[DCache.scala 364:{71,71}]
    node DataAccessMissRspSRAMWReq_2_data_0 = _WIRE_13_0 @[DCache.scala 360:64 364:39]
    node _WIRE_4_0 = _T_8 @[DCache.scala 231:{101,101}]
    node DataAccessWriteHitSRAMWReq_2_data_0 = _WIRE_4_0 @[DCache.scala 226:65 231:40]
    node _DataAccessesRRsp_DataAccess_io_w_req_bits_T_5_data_0 = mux(_DataAccessesRRsp_DataAccess_io_w_req_bits_T_4, DataAccessMissRspSRAMWReq_2_data_0, DataAccessWriteHitSRAMWReq_2_data_0) @[DCache.scala 413:36]
    node _WIRE_13_1 = _T_50 @[DCache.scala 364:{71,71}]
    node DataAccessMissRspSRAMWReq_2_data_1 = _WIRE_13_1 @[DCache.scala 360:64 364:39]
    node _WIRE_4_1 = _T_9 @[DCache.scala 231:{101,101}]
    node DataAccessWriteHitSRAMWReq_2_data_1 = _WIRE_4_1 @[DCache.scala 226:65 231:40]
    node _DataAccessesRRsp_DataAccess_io_w_req_bits_T_5_data_1 = mux(_DataAccessesRRsp_DataAccess_io_w_req_bits_T_4, DataAccessMissRspSRAMWReq_2_data_1, DataAccessWriteHitSRAMWReq_2_data_1) @[DCache.scala 413:36]
    node _WIRE_13_2 = _T_51 @[DCache.scala 364:{71,71}]
    node DataAccessMissRspSRAMWReq_2_data_2 = _WIRE_13_2 @[DCache.scala 360:64 364:39]
    node _WIRE_4_2 = _T_10 @[DCache.scala 231:{101,101}]
    node DataAccessWriteHitSRAMWReq_2_data_2 = _WIRE_4_2 @[DCache.scala 226:65 231:40]
    node _DataAccessesRRsp_DataAccess_io_w_req_bits_T_5_data_2 = mux(_DataAccessesRRsp_DataAccess_io_w_req_bits_T_4, DataAccessMissRspSRAMWReq_2_data_2, DataAccessWriteHitSRAMWReq_2_data_2) @[DCache.scala 413:36]
    node _WIRE_13_3 = _T_52 @[DCache.scala 364:{71,71}]
    node DataAccessMissRspSRAMWReq_2_data_3 = _WIRE_13_3 @[DCache.scala 360:64 364:39]
    node _WIRE_4_3 = _T_11 @[DCache.scala 231:{101,101}]
    node DataAccessWriteHitSRAMWReq_2_data_3 = _WIRE_4_3 @[DCache.scala 226:65 231:40]
    node _DataAccessesRRsp_DataAccess_io_w_req_bits_T_5_data_3 = mux(_DataAccessesRRsp_DataAccess_io_w_req_bits_T_4, DataAccessMissRspSRAMWReq_2_data_3, DataAccessWriteHitSRAMWReq_2_data_3) @[DCache.scala 413:36]
    node DataAccessMissRspSRAMWReq_2_waymask = _DataAccessMissRspSRAMWReq_2_waymask_T @[DCache.scala 360:64 362:51]
    node _coreReq_Q_io_deq_bits_perLaneAddr_getBankEn_io_perBankBlockIdx_2_wordOffset1H = _GEN_27 @[DCache.scala 230:47]
    node DataAccessWriteHitSRAMWReq_2_waymask = _coreReq_Q_io_deq_bits_perLaneAddr_getBankEn_io_perBankBlockIdx_2_wordOffset1H @[DCache.scala 226:65 230:47]
    node _DataAccessesRRsp_DataAccess_io_w_req_bits_T_5_waymask = mux(_DataAccessesRRsp_DataAccess_io_w_req_bits_T_4, DataAccessMissRspSRAMWReq_2_waymask, DataAccessWriteHitSRAMWReq_2_waymask) @[DCache.scala 413:36]
    node _DataAccessesRRsp_DataAccess_io_r_req_valid_T_4 = or(readHit_st1, dataReplaceReadValid) @[DCache.scala 414:46]
    node _DataAccessesRRsp_DataAccess_io_r_req_valid_T_5 = or(_DataAccessesRRsp_DataAccess_io_r_req_valid_T_4, dataAccessInvOrFluRValid) @[DCache.scala 414:70]
    node DataAccessInvOrFluSRAMRReq_2_setIdx = bits(_DataAccessInvOrFluSRAMRReq_2_setIdx_T, 2, 0) @[DCache.scala 175:40 178:47]
    node DataAccessReadHitSRAMRReq_2_setIdx = bits(_DataAccessReadHitSRAMRReq_2_setIdx_T, 2, 0) @[DCache.scala 235:39 236:46]
    node _GEN_71 = mux(dataAccessInvOrFluRValid, DataAccessInvOrFluSRAMRReq_2_setIdx, DataAccessReadHitSRAMRReq_2_setIdx) @[DCache.scala 417:41 418:32 420:32]
    node DataAccessReplaceReadSRAMRReq_2_setIdx = bits(_DataAccessReplaceReadSRAMRReq_2_setIdx_T, 2, 0) @[DCache.scala 354:43 355:50]
    node _GEN_72 = mux(dataReplaceReadValid, DataAccessReplaceReadSRAMRReq_2_setIdx, _GEN_71) @[DCache.scala 415:31 416:32]
    node DataAccessesRRsp_lo_2 = cat(DataAccessesRRsp_DataAccess_2.io_r_resp_data_1, DataAccessesRRsp_DataAccess_2.io_r_resp_data_0) @[Cat.scala 31:58]
    node DataAccessesRRsp_hi_2 = cat(DataAccessesRRsp_DataAccess_2.io_r_resp_data_3, DataAccessesRRsp_DataAccess_2.io_r_resp_data_2) @[Cat.scala 31:58]
    node DataAccessesRRsp_2 = cat(DataAccessesRRsp_hi_2, DataAccessesRRsp_lo_2) @[Cat.scala 31:58]
    node _DataAccessesRRsp_DataAccess_io_w_req_valid_T_6 = and(writeHit_st1, getBankEn.io_perBankValid_3) @[DCache.scala 412:20]
    node _DataAccessesRRsp_DataAccess_io_w_req_valid_T_7 = mux(dataFillVaild, UInt<1>("h1"), _DataAccessesRRsp_DataAccess_io_w_req_valid_T_6) @[DCache.scala 410:37]
    node _DataAccessesRRsp_DataAccess_io_w_req_bits_T_6 = and(memRsp_Q.io_deq_valid, memRspIsRead) @[DCache.scala 413:59]
    node DataAccessMissRspSRAMWReq_3_setIdx = bits(_DataAccessMissRspSRAMWReq_3_setIdx_T, 2, 0) @[DCache.scala 360:64 361:46]
    node DataAccessWriteHitSRAMWReq_3_setIdx = bits(_DataAccessWriteHitSRAMWReq_3_setIdx_T, 2, 0) @[DCache.scala 226:65 228:47]
    node _DataAccessesRRsp_DataAccess_io_w_req_bits_T_7_setIdx = mux(_DataAccessesRRsp_DataAccess_io_w_req_bits_T_6, DataAccessMissRspSRAMWReq_3_setIdx, DataAccessWriteHitSRAMWReq_3_setIdx) @[DCache.scala 413:36]
    node _WIRE_15_0 = _T_53 @[DCache.scala 364:{71,71}]
    node DataAccessMissRspSRAMWReq_3_data_0 = _WIRE_15_0 @[DCache.scala 360:64 364:39]
    node _WIRE_6_0 = _T_12 @[DCache.scala 231:{101,101}]
    node DataAccessWriteHitSRAMWReq_3_data_0 = _WIRE_6_0 @[DCache.scala 226:65 231:40]
    node _DataAccessesRRsp_DataAccess_io_w_req_bits_T_7_data_0 = mux(_DataAccessesRRsp_DataAccess_io_w_req_bits_T_6, DataAccessMissRspSRAMWReq_3_data_0, DataAccessWriteHitSRAMWReq_3_data_0) @[DCache.scala 413:36]
    node _WIRE_15_1 = _T_54 @[DCache.scala 364:{71,71}]
    node DataAccessMissRspSRAMWReq_3_data_1 = _WIRE_15_1 @[DCache.scala 360:64 364:39]
    node _WIRE_6_1 = _T_13 @[DCache.scala 231:{101,101}]
    node DataAccessWriteHitSRAMWReq_3_data_1 = _WIRE_6_1 @[DCache.scala 226:65 231:40]
    node _DataAccessesRRsp_DataAccess_io_w_req_bits_T_7_data_1 = mux(_DataAccessesRRsp_DataAccess_io_w_req_bits_T_6, DataAccessMissRspSRAMWReq_3_data_1, DataAccessWriteHitSRAMWReq_3_data_1) @[DCache.scala 413:36]
    node _WIRE_15_2 = _T_55 @[DCache.scala 364:{71,71}]
    node DataAccessMissRspSRAMWReq_3_data_2 = _WIRE_15_2 @[DCache.scala 360:64 364:39]
    node _WIRE_6_2 = _T_14 @[DCache.scala 231:{101,101}]
    node DataAccessWriteHitSRAMWReq_3_data_2 = _WIRE_6_2 @[DCache.scala 226:65 231:40]
    node _DataAccessesRRsp_DataAccess_io_w_req_bits_T_7_data_2 = mux(_DataAccessesRRsp_DataAccess_io_w_req_bits_T_6, DataAccessMissRspSRAMWReq_3_data_2, DataAccessWriteHitSRAMWReq_3_data_2) @[DCache.scala 413:36]
    node _WIRE_15_3 = _T_56 @[DCache.scala 364:{71,71}]
    node DataAccessMissRspSRAMWReq_3_data_3 = _WIRE_15_3 @[DCache.scala 360:64 364:39]
    node _WIRE_6_3 = _T_15 @[DCache.scala 231:{101,101}]
    node DataAccessWriteHitSRAMWReq_3_data_3 = _WIRE_6_3 @[DCache.scala 226:65 231:40]
    node _DataAccessesRRsp_DataAccess_io_w_req_bits_T_7_data_3 = mux(_DataAccessesRRsp_DataAccess_io_w_req_bits_T_6, DataAccessMissRspSRAMWReq_3_data_3, DataAccessWriteHitSRAMWReq_3_data_3) @[DCache.scala 413:36]
    node DataAccessMissRspSRAMWReq_3_waymask = _DataAccessMissRspSRAMWReq_3_waymask_T @[DCache.scala 360:64 362:51]
    node _coreReq_Q_io_deq_bits_perLaneAddr_getBankEn_io_perBankBlockIdx_3_wordOffset1H = _GEN_35 @[DCache.scala 230:47]
    node DataAccessWriteHitSRAMWReq_3_waymask = _coreReq_Q_io_deq_bits_perLaneAddr_getBankEn_io_perBankBlockIdx_3_wordOffset1H @[DCache.scala 226:65 230:47]
    node _DataAccessesRRsp_DataAccess_io_w_req_bits_T_7_waymask = mux(_DataAccessesRRsp_DataAccess_io_w_req_bits_T_6, DataAccessMissRspSRAMWReq_3_waymask, DataAccessWriteHitSRAMWReq_3_waymask) @[DCache.scala 413:36]
    node _DataAccessesRRsp_DataAccess_io_r_req_valid_T_6 = or(readHit_st1, dataReplaceReadValid) @[DCache.scala 414:46]
    node _DataAccessesRRsp_DataAccess_io_r_req_valid_T_7 = or(_DataAccessesRRsp_DataAccess_io_r_req_valid_T_6, dataAccessInvOrFluRValid) @[DCache.scala 414:70]
    node DataAccessInvOrFluSRAMRReq_3_setIdx = bits(_DataAccessInvOrFluSRAMRReq_3_setIdx_T, 2, 0) @[DCache.scala 175:40 178:47]
    node DataAccessReadHitSRAMRReq_3_setIdx = bits(_DataAccessReadHitSRAMRReq_3_setIdx_T, 2, 0) @[DCache.scala 235:39 236:46]
    node _GEN_73 = mux(dataAccessInvOrFluRValid, DataAccessInvOrFluSRAMRReq_3_setIdx, DataAccessReadHitSRAMRReq_3_setIdx) @[DCache.scala 417:41 418:32 420:32]
    node DataAccessReplaceReadSRAMRReq_3_setIdx = bits(_DataAccessReplaceReadSRAMRReq_3_setIdx_T, 2, 0) @[DCache.scala 354:43 355:50]
    node _GEN_74 = mux(dataReplaceReadValid, DataAccessReplaceReadSRAMRReq_3_setIdx, _GEN_73) @[DCache.scala 415:31 416:32]
    node DataAccessesRRsp_lo_3 = cat(DataAccessesRRsp_DataAccess_3.io_r_resp_data_1, DataAccessesRRsp_DataAccess_3.io_r_resp_data_0) @[Cat.scala 31:58]
    node DataAccessesRRsp_hi_3 = cat(DataAccessesRRsp_DataAccess_3.io_r_resp_data_3, DataAccessesRRsp_DataAccess_3.io_r_resp_data_2) @[Cat.scala 31:58]
    node DataAccessesRRsp_3 = cat(DataAccessesRRsp_hi_3, DataAccessesRRsp_lo_3) @[Cat.scala 31:58]
    node _T_58 = and(io_coreReq_ready, io_coreReq_valid) @[Decoupled.scala 50:35]
    reg REG : UInt<1>, clock with :
      reset => (UInt<1>("h0"), REG) @[DCache.scala 427:31]
    node _T_59 = and(cacheHit_st1, REG) @[DCache.scala 427:21]
    node _T_60 = or(waitMSHRCoreRsp_st1, fluCoreRsp_st1) @[DCache.scala 435:34]
    node _T_61 = or(_T_60, invCOreRsp_st1) @[DCache.scala 435:52]
    node _GEN_75 = validif(eq(_T_61, UInt<1>("h0")), coreRsp_st2_data_0) @[DCache.scala 139:23 435:70]
    node _GEN_76 = validif(eq(_T_61, UInt<1>("h0")), coreRsp_st2_data_1) @[DCache.scala 139:23 435:70]
    node _GEN_77 = validif(eq(_T_61, UInt<1>("h0")), coreRsp_st2_data_2) @[DCache.scala 139:23 435:70]
    node _GEN_78 = validif(eq(_T_61, UInt<1>("h0")), coreRsp_st2_data_3) @[DCache.scala 139:23 435:70]
    node _GEN_79 = mux(_T_61, UInt<1>("h0"), coreRsp_st2_isWrite) @[DCache.scala 139:23 435:70 437:25]
    node _GEN_80 = mux(MshrAccess.io_missRspOut_valid, memRsp_st1_d_data_0, _GEN_75) @[DCache.scala 431:45 433:22]
    node _GEN_81 = mux(MshrAccess.io_missRspOut_valid, memRsp_st1_d_data_1, _GEN_76) @[DCache.scala 431:45 433:22]
    node _GEN_82 = mux(MshrAccess.io_missRspOut_valid, memRsp_st1_d_data_2, _GEN_77) @[DCache.scala 431:45 433:22]
    node _GEN_83 = mux(MshrAccess.io_missRspOut_valid, memRsp_st1_d_data_3, _GEN_78) @[DCache.scala 431:45 433:22]
    node _GEN_84 = mux(MshrAccess.io_missRspOut_valid, UInt<1>("h0"), _GEN_79) @[DCache.scala 431:45 434:25]
    node _GEN_85 = validif(eq(_T_59, UInt<1>("h0")), _GEN_80) @[DCache.scala 427:50]
    node _GEN_86 = validif(eq(_T_59, UInt<1>("h0")), _GEN_81) @[DCache.scala 427:50]
    node _GEN_87 = validif(eq(_T_59, UInt<1>("h0")), _GEN_82) @[DCache.scala 427:50]
    node _GEN_88 = validif(eq(_T_59, UInt<1>("h0")), _GEN_83) @[DCache.scala 427:50]
    node _GEN_89 = mux(_T_59, coreReqControl_st1_isWrite, _GEN_84) @[DCache.scala 427:50 430:25]
    node _T_62 = and(io_coreReq_ready, io_coreReq_valid) @[Decoupled.scala 50:35]
    reg REG_1 : UInt<1>, clock with :
      reset => (UInt<1>("h0"), REG_1) @[DCache.scala 439:32]
    node _T_63 = and(cacheHit_st1, REG_1) @[DCache.scala 439:22]
    node _T_64 = and(secondaryFullReturn, MshrAccess.io_missRspOut_valid) @[DCache.scala 439:75]
    node _T_65 = or(_T_63, _T_64) @[DCache.scala 439:51]
    node _T_66 = or(_T_65, waitMSHRCoreRsp_st1) @[DCache.scala 439:110]
    node _T_67 = or(_T_66, fluCoreRsp_st1) @[DCache.scala 440:25]
    node _T_68 = or(_T_67, invCOreRsp_st1) @[DCache.scala 440:43]
    node _missRspTI_st1_WIRE_instrId = _missRspTI_st1_T_12 @[DCache.scala 393:{69,69}]
    node missRspTI_st1_instrId = _missRspTI_st1_WIRE_instrId @[DCache.scala 393:17 98:27]
    node _GEN_90 = mux(MshrAccess.io_missRspOut_valid, missRspTI_st1_instrId, coreRsp_st2_instrId) @[DCache.scala 139:23 444:45 445:25]
    node _missRspTI_st1_WIRE_perLaneAddr_0_activeMask = _missRspTI_st1_T_2 @[DCache.scala 393:{69,69}]
    node missRspTI_st1_perLaneAddr_0_activeMask = _missRspTI_st1_WIRE_perLaneAddr_0_activeMask @[DCache.scala 393:17 98:27]
    node _GEN_91 = mux(MshrAccess.io_missRspOut_valid, missRspTI_st1_perLaneAddr_0_activeMask, coreRsp_st2_activeMask_0) @[DCache.scala 139:23 444:45 446:28]
    node _missRspTI_st1_WIRE_perLaneAddr_1_activeMask = _missRspTI_st1_T_5 @[DCache.scala 393:{69,69}]
    node missRspTI_st1_perLaneAddr_1_activeMask = _missRspTI_st1_WIRE_perLaneAddr_1_activeMask @[DCache.scala 393:17 98:27]
    node _GEN_92 = mux(MshrAccess.io_missRspOut_valid, missRspTI_st1_perLaneAddr_1_activeMask, coreRsp_st2_activeMask_1) @[DCache.scala 139:23 444:45 446:28]
    node _missRspTI_st1_WIRE_perLaneAddr_2_activeMask = _missRspTI_st1_T_8 @[DCache.scala 393:{69,69}]
    node missRspTI_st1_perLaneAddr_2_activeMask = _missRspTI_st1_WIRE_perLaneAddr_2_activeMask @[DCache.scala 393:17 98:27]
    node _GEN_93 = mux(MshrAccess.io_missRspOut_valid, missRspTI_st1_perLaneAddr_2_activeMask, coreRsp_st2_activeMask_2) @[DCache.scala 139:23 444:45 446:28]
    node _missRspTI_st1_WIRE_perLaneAddr_3_activeMask = _missRspTI_st1_T_11 @[DCache.scala 393:{69,69}]
    node missRspTI_st1_perLaneAddr_3_activeMask = _missRspTI_st1_WIRE_perLaneAddr_3_activeMask @[DCache.scala 393:17 98:27]
    node _GEN_94 = mux(MshrAccess.io_missRspOut_valid, missRspTI_st1_perLaneAddr_3_activeMask, coreRsp_st2_activeMask_3) @[DCache.scala 139:23 444:45 446:28]
    node _GEN_95 = mux(MshrAccess.io_missRspOut_valid, missRspTI_st1_perLaneAddr_0_activeMask, coreRsp_st2_perLaneAddr_0_activeMask) @[DCache.scala 444:45 447:29 141:36]
    node _missRspTI_st1_WIRE_perLaneAddr_0_blockOffset = _missRspTI_st1_T_1 @[DCache.scala 393:{69,69}]
    node missRspTI_st1_perLaneAddr_0_blockOffset = _missRspTI_st1_WIRE_perLaneAddr_0_blockOffset @[DCache.scala 393:17 98:27]
    node _GEN_96 = mux(MshrAccess.io_missRspOut_valid, missRspTI_st1_perLaneAddr_0_blockOffset, coreRsp_st2_perLaneAddr_0_blockOffset) @[DCache.scala 444:45 447:29 141:36]
    node _missRspTI_st1_WIRE_perLaneAddr_0_wordOffset1H = _missRspTI_st1_T @[DCache.scala 393:{69,69}]
    node missRspTI_st1_perLaneAddr_0_wordOffset1H = _missRspTI_st1_WIRE_perLaneAddr_0_wordOffset1H @[DCache.scala 393:17 98:27]
    node _GEN_97 = mux(MshrAccess.io_missRspOut_valid, missRspTI_st1_perLaneAddr_0_wordOffset1H, coreRsp_st2_perLaneAddr_0_wordOffset1H) @[DCache.scala 444:45 447:29 141:36]
    node _GEN_98 = mux(MshrAccess.io_missRspOut_valid, missRspTI_st1_perLaneAddr_1_activeMask, coreRsp_st2_perLaneAddr_1_activeMask) @[DCache.scala 444:45 447:29 141:36]
    node _missRspTI_st1_WIRE_perLaneAddr_1_blockOffset = _missRspTI_st1_T_4 @[DCache.scala 393:{69,69}]
    node missRspTI_st1_perLaneAddr_1_blockOffset = _missRspTI_st1_WIRE_perLaneAddr_1_blockOffset @[DCache.scala 393:17 98:27]
    node _GEN_99 = mux(MshrAccess.io_missRspOut_valid, missRspTI_st1_perLaneAddr_1_blockOffset, coreRsp_st2_perLaneAddr_1_blockOffset) @[DCache.scala 444:45 447:29 141:36]
    node _missRspTI_st1_WIRE_perLaneAddr_1_wordOffset1H = _missRspTI_st1_T_3 @[DCache.scala 393:{69,69}]
    node missRspTI_st1_perLaneAddr_1_wordOffset1H = _missRspTI_st1_WIRE_perLaneAddr_1_wordOffset1H @[DCache.scala 393:17 98:27]
    node _GEN_100 = mux(MshrAccess.io_missRspOut_valid, missRspTI_st1_perLaneAddr_1_wordOffset1H, coreRsp_st2_perLaneAddr_1_wordOffset1H) @[DCache.scala 444:45 447:29 141:36]
    node _GEN_101 = mux(MshrAccess.io_missRspOut_valid, missRspTI_st1_perLaneAddr_2_activeMask, coreRsp_st2_perLaneAddr_2_activeMask) @[DCache.scala 444:45 447:29 141:36]
    node _missRspTI_st1_WIRE_perLaneAddr_2_blockOffset = _missRspTI_st1_T_7 @[DCache.scala 393:{69,69}]
    node missRspTI_st1_perLaneAddr_2_blockOffset = _missRspTI_st1_WIRE_perLaneAddr_2_blockOffset @[DCache.scala 393:17 98:27]
    node _GEN_102 = mux(MshrAccess.io_missRspOut_valid, missRspTI_st1_perLaneAddr_2_blockOffset, coreRsp_st2_perLaneAddr_2_blockOffset) @[DCache.scala 444:45 447:29 141:36]
    node _missRspTI_st1_WIRE_perLaneAddr_2_wordOffset1H = _missRspTI_st1_T_6 @[DCache.scala 393:{69,69}]
    node missRspTI_st1_perLaneAddr_2_wordOffset1H = _missRspTI_st1_WIRE_perLaneAddr_2_wordOffset1H @[DCache.scala 393:17 98:27]
    node _GEN_103 = mux(MshrAccess.io_missRspOut_valid, missRspTI_st1_perLaneAddr_2_wordOffset1H, coreRsp_st2_perLaneAddr_2_wordOffset1H) @[DCache.scala 444:45 447:29 141:36]
    node _GEN_104 = mux(MshrAccess.io_missRspOut_valid, missRspTI_st1_perLaneAddr_3_activeMask, coreRsp_st2_perLaneAddr_3_activeMask) @[DCache.scala 444:45 447:29 141:36]
    node _missRspTI_st1_WIRE_perLaneAddr_3_blockOffset = _missRspTI_st1_T_10 @[DCache.scala 393:{69,69}]
    node missRspTI_st1_perLaneAddr_3_blockOffset = _missRspTI_st1_WIRE_perLaneAddr_3_blockOffset @[DCache.scala 393:17 98:27]
    node _GEN_105 = mux(MshrAccess.io_missRspOut_valid, missRspTI_st1_perLaneAddr_3_blockOffset, coreRsp_st2_perLaneAddr_3_blockOffset) @[DCache.scala 444:45 447:29 141:36]
    node _missRspTI_st1_WIRE_perLaneAddr_3_wordOffset1H = _missRspTI_st1_T_9 @[DCache.scala 393:{69,69}]
    node missRspTI_st1_perLaneAddr_3_wordOffset1H = _missRspTI_st1_WIRE_perLaneAddr_3_wordOffset1H @[DCache.scala 393:17 98:27]
    node _GEN_106 = mux(MshrAccess.io_missRspOut_valid, missRspTI_st1_perLaneAddr_3_wordOffset1H, coreRsp_st2_perLaneAddr_3_wordOffset1H) @[DCache.scala 444:45 447:29 141:36]
    node _GEN_107 = mux(_T_68, coreReq_Q.io_deq_bits_instrId, _GEN_90) @[DCache.scala 440:61 441:25]
    node _GEN_108 = mux(_T_68, coreReq_Q.io_deq_bits_perLaneAddr_0_activeMask, _GEN_91) @[DCache.scala 440:61 442:28]
    node _GEN_109 = mux(_T_68, coreReq_Q.io_deq_bits_perLaneAddr_1_activeMask, _GEN_92) @[DCache.scala 440:61 442:28]
    node _GEN_110 = mux(_T_68, coreReq_Q.io_deq_bits_perLaneAddr_2_activeMask, _GEN_93) @[DCache.scala 440:61 442:28]
    node _GEN_111 = mux(_T_68, coreReq_Q.io_deq_bits_perLaneAddr_3_activeMask, _GEN_94) @[DCache.scala 440:61 442:28]
    node _GEN_112 = mux(_T_68, coreReq_Q.io_deq_bits_perLaneAddr_0_activeMask, _GEN_95) @[DCache.scala 440:61 443:29]
    node _GEN_113 = mux(_T_68, coreReq_Q.io_deq_bits_perLaneAddr_0_blockOffset, _GEN_96) @[DCache.scala 440:61 443:29]
    node _GEN_114 = mux(_T_68, coreReq_Q.io_deq_bits_perLaneAddr_0_wordOffset1H, _GEN_97) @[DCache.scala 440:61 443:29]
    node _GEN_115 = mux(_T_68, coreReq_Q.io_deq_bits_perLaneAddr_1_activeMask, _GEN_98) @[DCache.scala 440:61 443:29]
    node _GEN_116 = mux(_T_68, coreReq_Q.io_deq_bits_perLaneAddr_1_blockOffset, _GEN_99) @[DCache.scala 440:61 443:29]
    node _GEN_117 = mux(_T_68, coreReq_Q.io_deq_bits_perLaneAddr_1_wordOffset1H, _GEN_100) @[DCache.scala 440:61 443:29]
    node _GEN_118 = mux(_T_68, coreReq_Q.io_deq_bits_perLaneAddr_2_activeMask, _GEN_101) @[DCache.scala 440:61 443:29]
    node _GEN_119 = mux(_T_68, coreReq_Q.io_deq_bits_perLaneAddr_2_blockOffset, _GEN_102) @[DCache.scala 440:61 443:29]
    node _GEN_120 = mux(_T_68, coreReq_Q.io_deq_bits_perLaneAddr_2_wordOffset1H, _GEN_103) @[DCache.scala 440:61 443:29]
    node _GEN_121 = mux(_T_68, coreReq_Q.io_deq_bits_perLaneAddr_3_activeMask, _GEN_104) @[DCache.scala 440:61 443:29]
    node _GEN_122 = mux(_T_68, coreReq_Q.io_deq_bits_perLaneAddr_3_blockOffset, _GEN_105) @[DCache.scala 440:61 443:29]
    node _GEN_123 = mux(_T_68, coreReq_Q.io_deq_bits_perLaneAddr_3_wordOffset1H, _GEN_106) @[DCache.scala 440:61 443:29]
    node _coreRsp_st2_valid_from_coreReq_T = or(readHit_st1, writeHit_st1) @[DCache.scala 458:18]
    node _coreRsp_st2_valid_from_coreReq_T_1 = and(coreReq_st1_valid, _coreRsp_st2_valid_from_coreReq_T) @[DCache.scala 457:63]
    reg coreRsp_st2_valid_from_coreReq_REG : UInt<1>, clock with :
      reset => (UInt<1>("h0"), coreRsp_st2_valid_from_coreReq_REG) @[DCache.scala 457:44]
    reg coreRsp_st2_valid_from_memRsp_REG : UInt<1>, clock with :
      reset => (UInt<1>("h0"), coreRsp_st2_valid_from_memRsp_REG) @[DCache.scala 459:43]
    node coreRsp_st2_valid_from_coreReq = coreRsp_st2_valid_from_coreReq_REG @[DCache.scala 451:44 457:34]
    node coreRsp_st2_valid_from_memRsp = coreRsp_st2_valid_from_memRsp_REG @[DCache.scala 452:43 459:33]
    node _T_69 = and(coreRsp_st2_valid_from_coreReq, coreRsp_st2_valid_from_memRsp) @[DCache.scala 460:44]
    node _T_70 = eq(_T_69, UInt<1>("h0")) @[DCache.scala 460:11]
    node _T_71 = asUInt(reset) @[DCache.scala 460:10]
    node _T_72 = eq(_T_71, UInt<1>("h0")) @[DCache.scala 460:10]
    node _T_73 = eq(_T_70, UInt<1>("h0")) @[DCache.scala 460:10]
    node _coreRsp_st2_valid_from_memReq_T = and(WshrAccess.io_pushReq_valid, memReq_Q.io_deq_bits_hasCoreRsp) @[DCache.scala 508:64]
    node coreRsp_st2_valid_from_memReq = _coreRsp_st2_valid_from_memReq_T @[DCache.scala 453:43 508:33]
    node _T_74 = and(coreRsp_st2_valid_from_coreReq, coreRsp_st2_valid_from_memReq) @[DCache.scala 461:44]
    node _T_75 = eq(_T_74, UInt<1>("h0")) @[DCache.scala 461:11]
    node _T_76 = asUInt(reset) @[DCache.scala 461:10]
    node _T_77 = eq(_T_76, UInt<1>("h0")) @[DCache.scala 461:10]
    node _T_78 = eq(_T_75, UInt<1>("h0")) @[DCache.scala 461:10]
    node _T_79 = and(coreRsp_st2_valid_from_memReq, coreRsp_st2_valid_from_memRsp) @[DCache.scala 462:43]
    node _T_80 = eq(_T_79, UInt<1>("h0")) @[DCache.scala 462:11]
    node _T_81 = asUInt(reset) @[DCache.scala 462:10]
    node _T_82 = eq(_T_81, UInt<1>("h0")) @[DCache.scala 462:10]
    node _T_83 = eq(_T_80, UInt<1>("h0")) @[DCache.scala 462:10]
    node _coreRsp_st2_valid_T = or(coreRsp_st2_valid_from_coreReq, coreRsp_st2_valid_from_memRsp) @[DCache.scala 463:55]
    node _coreRsp_st2_valid_T_1 = or(_coreRsp_st2_valid_T, coreRsp_st2_valid_from_memReq) @[DCache.scala 463:88]
    node coreRspFromMemReq_instrId = bits(memReq_Q.io_deq_bits_coreRspInstrId, 2, 0) @[DCache.scala 455:31 526:29]
    node _coreRsp_Q_io_enq_bits_T_instrId = mux(coreRsp_st2_valid_from_memReq, coreRspFromMemReq_instrId, coreRsp_st2_instrId) @[DCache.scala 467:31]
    node coreRspFromMemReq_isWrite = UInt<1>("h1") @[DCache.scala 455:31 524:29]
    node _coreRsp_Q_io_enq_bits_T_isWrite = mux(coreRsp_st2_valid_from_memReq, coreRspFromMemReq_isWrite, coreRsp_st2_isWrite) @[DCache.scala 467:31]
    node coreRspFromMemReq_data_0 = validif(UInt<1>("h0"), UInt<32>("h0"))
    node _coreRsp_Q_io_enq_bits_T_data_0 = mux(coreRsp_st2_valid_from_memReq, coreRspFromMemReq_data_0, coreRsp_st2_data_0) @[DCache.scala 467:31]
    node coreRspFromMemReq_data_1 = validif(UInt<1>("h0"), UInt<32>("h0"))
    node _coreRsp_Q_io_enq_bits_T_data_1 = mux(coreRsp_st2_valid_from_memReq, coreRspFromMemReq_data_1, coreRsp_st2_data_1) @[DCache.scala 467:31]
    node coreRspFromMemReq_data_2 = validif(UInt<1>("h0"), UInt<32>("h0"))
    node _coreRsp_Q_io_enq_bits_T_data_2 = mux(coreRsp_st2_valid_from_memReq, coreRspFromMemReq_data_2, coreRsp_st2_data_2) @[DCache.scala 467:31]
    node coreRspFromMemReq_data_3 = validif(UInt<1>("h0"), UInt<32>("h0"))
    node _coreRsp_Q_io_enq_bits_T_data_3 = mux(coreRsp_st2_valid_from_memReq, coreRspFromMemReq_data_3, coreRsp_st2_data_3) @[DCache.scala 467:31]
    node _WIRE_18_0 = UInt<1>("h1") @[DCache.scala 527:{42,42}]
    node coreRspFromMemReq_activeMask_0 = _WIRE_18_0 @[DCache.scala 455:31 527:32]
    node _coreRsp_Q_io_enq_bits_T_activeMask_0 = mux(coreRsp_st2_valid_from_memReq, coreRspFromMemReq_activeMask_0, coreRsp_st2_activeMask_0) @[DCache.scala 467:31]
    node _WIRE_18_1 = UInt<1>("h1") @[DCache.scala 527:{42,42}]
    node coreRspFromMemReq_activeMask_1 = _WIRE_18_1 @[DCache.scala 455:31 527:32]
    node _coreRsp_Q_io_enq_bits_T_activeMask_1 = mux(coreRsp_st2_valid_from_memReq, coreRspFromMemReq_activeMask_1, coreRsp_st2_activeMask_1) @[DCache.scala 467:31]
    node _WIRE_18_2 = UInt<1>("h1") @[DCache.scala 527:{42,42}]
    node coreRspFromMemReq_activeMask_2 = _WIRE_18_2 @[DCache.scala 455:31 527:32]
    node _coreRsp_Q_io_enq_bits_T_activeMask_2 = mux(coreRsp_st2_valid_from_memReq, coreRspFromMemReq_activeMask_2, coreRsp_st2_activeMask_2) @[DCache.scala 467:31]
    node _WIRE_18_3 = UInt<1>("h1") @[DCache.scala 527:{42,42}]
    node coreRspFromMemReq_activeMask_3 = _WIRE_18_3 @[DCache.scala 455:31 527:32]
    node _coreRsp_Q_io_enq_bits_T_activeMask_3 = mux(coreRsp_st2_valid_from_memReq, coreRspFromMemReq_activeMask_3, coreRsp_st2_activeMask_3) @[DCache.scala 467:31]
    node DataAccessReadSRAMRRsp_0 = DataAccessesRRsp_0 @[DCache.scala 424:{50,50}]
    node _T_84_0 = mux(readHit_st2, DataAccessReadSRAMRRsp_0, coreRsp_st2_data_0) @[DCache.scala 473:34]
    node DataAccessReadSRAMRRsp_1 = DataAccessesRRsp_1 @[DCache.scala 424:{50,50}]
    node _T_84_1 = mux(readHit_st2, DataAccessReadSRAMRRsp_1, coreRsp_st2_data_1) @[DCache.scala 473:34]
    node DataAccessReadSRAMRRsp_2 = DataAccessesRRsp_2 @[DCache.scala 424:{50,50}]
    node _T_84_2 = mux(readHit_st2, DataAccessReadSRAMRRsp_2, coreRsp_st2_data_2) @[DCache.scala 473:34]
    node DataAccessReadSRAMRRsp_3 = DataAccessesRRsp_3 @[DCache.scala 424:{50,50}]
    node _T_84_3 = mux(readHit_st2, DataAccessReadSRAMRRsp_3, coreRsp_st2_data_3) @[DCache.scala 473:34]
    node coreRsp_st2_dataMemOrder_0 = _T_84_0 @[DCache.scala 470:38 473:28]
    node _GEN_124 = validif(eq(UInt<1>("h0"), coreRsp_st2_perLaneAddr_0_blockOffset), coreRsp_st2_dataMemOrder_0) @[DCache.scala 475:{34,34}]
    node coreRsp_st2_dataMemOrder_1 = _T_84_1 @[DCache.scala 470:38 473:28]
    node _GEN_125 = mux(eq(UInt<1>("h1"), coreRsp_st2_perLaneAddr_0_blockOffset), coreRsp_st2_dataMemOrder_1, _GEN_124) @[DCache.scala 475:{34,34}]
    node coreRsp_st2_dataMemOrder_2 = _T_84_2 @[DCache.scala 470:38 473:28]
    node _GEN_126 = mux(eq(UInt<2>("h2"), coreRsp_st2_perLaneAddr_0_blockOffset), coreRsp_st2_dataMemOrder_2, _GEN_125) @[DCache.scala 475:{34,34}]
    node coreRsp_st2_dataMemOrder_3 = _T_84_3 @[DCache.scala 470:38 473:28]
    node _GEN_127 = mux(eq(UInt<2>("h3"), coreRsp_st2_perLaneAddr_0_blockOffset), coreRsp_st2_dataMemOrder_3, _GEN_126) @[DCache.scala 475:{34,34}]
    node _GEN_128 = validif(eq(UInt<1>("h0"), coreRsp_st2_perLaneAddr_1_blockOffset), coreRsp_st2_dataMemOrder_0) @[DCache.scala 475:{34,34}]
    node _GEN_129 = mux(eq(UInt<1>("h1"), coreRsp_st2_perLaneAddr_1_blockOffset), coreRsp_st2_dataMemOrder_1, _GEN_128) @[DCache.scala 475:{34,34}]
    node _GEN_130 = mux(eq(UInt<2>("h2"), coreRsp_st2_perLaneAddr_1_blockOffset), coreRsp_st2_dataMemOrder_2, _GEN_129) @[DCache.scala 475:{34,34}]
    node _GEN_131 = mux(eq(UInt<2>("h3"), coreRsp_st2_perLaneAddr_1_blockOffset), coreRsp_st2_dataMemOrder_3, _GEN_130) @[DCache.scala 475:{34,34}]
    node _GEN_132 = validif(eq(UInt<1>("h0"), coreRsp_st2_perLaneAddr_2_blockOffset), coreRsp_st2_dataMemOrder_0) @[DCache.scala 475:{34,34}]
    node _GEN_133 = mux(eq(UInt<1>("h1"), coreRsp_st2_perLaneAddr_2_blockOffset), coreRsp_st2_dataMemOrder_1, _GEN_132) @[DCache.scala 475:{34,34}]
    node _GEN_134 = mux(eq(UInt<2>("h2"), coreRsp_st2_perLaneAddr_2_blockOffset), coreRsp_st2_dataMemOrder_2, _GEN_133) @[DCache.scala 475:{34,34}]
    node _GEN_135 = mux(eq(UInt<2>("h3"), coreRsp_st2_perLaneAddr_2_blockOffset), coreRsp_st2_dataMemOrder_3, _GEN_134) @[DCache.scala 475:{34,34}]
    node _GEN_136 = validif(eq(UInt<1>("h0"), coreRsp_st2_perLaneAddr_3_blockOffset), coreRsp_st2_dataMemOrder_0) @[DCache.scala 475:{34,34}]
    node _GEN_137 = mux(eq(UInt<1>("h1"), coreRsp_st2_perLaneAddr_3_blockOffset), coreRsp_st2_dataMemOrder_1, _GEN_136) @[DCache.scala 475:{34,34}]
    node _GEN_138 = mux(eq(UInt<2>("h2"), coreRsp_st2_perLaneAddr_3_blockOffset), coreRsp_st2_dataMemOrder_2, _GEN_137) @[DCache.scala 475:{34,34}]
    node _GEN_139 = mux(eq(UInt<2>("h3"), coreRsp_st2_perLaneAddr_3_blockOffset), coreRsp_st2_dataMemOrder_3, _GEN_138) @[DCache.scala 475:{34,34}]
    reg dirtyReplace_st2_a_opcode : UInt<3>, clock with :
      reset => (UInt<1>("h0"), dirtyReplace_st2_a_opcode) @[DCache.scala 481:29]
    reg dirtyReplace_st2_a_param : UInt<3>, clock with :
      reset => (UInt<1>("h0"), dirtyReplace_st2_a_param) @[DCache.scala 481:29]
    reg dirtyReplace_st2_a_addr : UInt<32>, clock with :
      reset => (UInt<1>("h0"), dirtyReplace_st2_a_addr) @[DCache.scala 481:29]
    reg dirtyReplace_st2_a_data_0 : UInt<32>, clock with :
      reset => (UInt<1>("h0"), dirtyReplace_st2_a_data_0) @[DCache.scala 481:29]
    reg dirtyReplace_st2_a_data_1 : UInt<32>, clock with :
      reset => (UInt<1>("h0"), dirtyReplace_st2_a_data_1) @[DCache.scala 481:29]
    reg dirtyReplace_st2_a_data_2 : UInt<32>, clock with :
      reset => (UInt<1>("h0"), dirtyReplace_st2_a_data_2) @[DCache.scala 481:29]
    reg dirtyReplace_st2_a_data_3 : UInt<32>, clock with :
      reset => (UInt<1>("h0"), dirtyReplace_st2_a_data_3) @[DCache.scala 481:29]
    reg dirtyReplace_st2_a_mask_0 : UInt<1>, clock with :
      reset => (UInt<1>("h0"), dirtyReplace_st2_a_mask_0) @[DCache.scala 481:29]
    reg dirtyReplace_st2_a_mask_1 : UInt<1>, clock with :
      reset => (UInt<1>("h0"), dirtyReplace_st2_a_mask_1) @[DCache.scala 481:29]
    reg dirtyReplace_st2_a_mask_2 : UInt<1>, clock with :
      reset => (UInt<1>("h0"), dirtyReplace_st2_a_mask_2) @[DCache.scala 481:29]
    reg dirtyReplace_st2_a_mask_3 : UInt<1>, clock with :
      reset => (UInt<1>("h0"), dirtyReplace_st2_a_mask_3) @[DCache.scala 481:29]
    reg dirtyReplace_st2_a_source : UInt<7>, clock with :
      reset => (UInt<1>("h0"), dirtyReplace_st2_a_source) @[DCache.scala 481:29]
    reg dirtyReplace_st2_hasCoreRsp : UInt<1>, clock with :
      reset => (UInt<1>("h0"), dirtyReplace_st2_hasCoreRsp) @[DCache.scala 481:29]
    reg dirtyReplace_st2_coreRspInstrId : UInt<32>, clock with :
      reset => (UInt<1>("h0"), dirtyReplace_st2_coreRspInstrId) @[DCache.scala 481:29]
    node _MemReqArb_io_in_1_valid_T = eq(MshrAccess.io_probeOut_st1_probeStatus, UInt<1>("h0")) @[DCache.scala 491:103]
    node _MemReqArb_io_in_1_valid_T_1 = and(readMiss_st1, _MemReqArb_io_in_1_valid_T) @[DCache.scala 491:84]
    node _MemReqArb_io_in_1_valid_T_2 = or(writeMiss_st1, _MemReqArb_io_in_1_valid_T_1) @[DCache.scala 491:67]
    node _MemReqArb_io_in_1_valid_T_3 = and(coreReq_st1_valid, _MemReqArb_io_in_1_valid_T_2) @[DCache.scala 491:49]
    reg memReq_st3_a_opcode : UInt<3>, clock with :
      reset => (UInt<1>("h0"), memReq_st3_a_opcode) @[DCache.scala 497:23]
    reg memReq_st3_a_param : UInt<3>, clock with :
      reset => (UInt<1>("h0"), memReq_st3_a_param) @[DCache.scala 497:23]
    reg memReq_st3_a_addr : UInt<32>, clock with :
      reset => (UInt<1>("h0"), memReq_st3_a_addr) @[DCache.scala 497:23]
    reg memReq_st3_a_data_0 : UInt<32>, clock with :
      reset => (UInt<1>("h0"), memReq_st3_a_data_0) @[DCache.scala 497:23]
    reg memReq_st3_a_data_1 : UInt<32>, clock with :
      reset => (UInt<1>("h0"), memReq_st3_a_data_1) @[DCache.scala 497:23]
    reg memReq_st3_a_data_2 : UInt<32>, clock with :
      reset => (UInt<1>("h0"), memReq_st3_a_data_2) @[DCache.scala 497:23]
    reg memReq_st3_a_data_3 : UInt<32>, clock with :
      reset => (UInt<1>("h0"), memReq_st3_a_data_3) @[DCache.scala 497:23]
    reg memReq_st3_a_mask_0 : UInt<1>, clock with :
      reset => (UInt<1>("h0"), memReq_st3_a_mask_0) @[DCache.scala 497:23]
    reg memReq_st3_a_mask_1 : UInt<1>, clock with :
      reset => (UInt<1>("h0"), memReq_st3_a_mask_1) @[DCache.scala 497:23]
    reg memReq_st3_a_mask_2 : UInt<1>, clock with :
      reset => (UInt<1>("h0"), memReq_st3_a_mask_2) @[DCache.scala 497:23]
    reg memReq_st3_a_mask_3 : UInt<1>, clock with :
      reset => (UInt<1>("h0"), memReq_st3_a_mask_3) @[DCache.scala 497:23]
    reg memReq_st3_a_source : UInt<7>, clock with :
      reset => (UInt<1>("h0"), memReq_st3_a_source) @[DCache.scala 497:23]
    node _memReqIsWrite_st3_T = eq(memReq_Q.io_deq_bits_a_opcode, UInt<3>("h0")) @[DCache.scala 499:37]
    node _memReqIsWrite_st3_T_1 = eq(memReq_Q.io_deq_bits_a_opcode, UInt<3>("h1")) @[DCache.scala 499:70]
    node _memReqIsWrite_st3_T_2 = eq(memReq_Q.io_deq_bits_a_param, UInt<1>("h0")) @[DCache.scala 499:121]
    node _memReqIsWrite_st3_T_3 = and(_memReqIsWrite_st3_T_1, _memReqIsWrite_st3_T_2) @[DCache.scala 499:89]
    node memReqIsWrite_st3 = or(_memReqIsWrite_st3_T, _memReqIsWrite_st3_T_3) @[DCache.scala 499:56]
    node _memReqIsRead_st3_T = eq(memReq_Q.io_deq_bits_a_opcode, UInt<3>("h4")) @[DCache.scala 500:36]
    node _memReqIsRead_st3_T_1 = eq(memReq_Q.io_deq_bits_a_param, UInt<1>("h0")) @[DCache.scala 500:83]
    node memReqIsRead_st3 = and(_memReqIsRead_st3_T, _memReqIsRead_st3_T_1) @[DCache.scala 500:51]
    node _WshrAccess_io_pushReq_bits_blockAddr_T = shr(memReq_Q.io_deq_bits_a_addr, 4) @[DCache.scala 502:71]
    node _wshrProtect_T = or(memReqIsWrite_st3, memReqIsRead_st3) @[DCache.scala 503:66]
    node wshrProtect = and(WshrAccess.io_conflict, _wshrProtect_T) @[DCache.scala 503:44]
    node _cRspBlockedOrWshrFull_T = eq(coreRsp_Q.io_enq_ready, UInt<1>("h0")) @[DCache.scala 504:33]
    node _cRspBlockedOrWshrFull_T_1 = and(_cRspBlockedOrWshrFull_T, memReq_Q.io_deq_bits_hasCoreRsp) @[DCache.scala 504:57]
    node _cRspBlockedOrWshrFull_T_2 = eq(WshrAccess.io_pushReq_ready, UInt<1>("h0")) @[DCache.scala 505:8]
    node _cRspBlockedOrWshrFull_T_3 = or(_cRspBlockedOrWshrFull_T_1, _cRspBlockedOrWshrFull_T_2) @[DCache.scala 505:5]
    node cRspBlockedOrWshrFull = and(_cRspBlockedOrWshrFull_T_3, memReqIsWrite_st3) @[DCache.scala 505:38]
    node _wshrPass_T = eq(wshrProtect, UInt<1>("h0")) @[DCache.scala 506:18]
    node _wshrPass_T_1 = eq(cRspBlockedOrWshrFull, UInt<1>("h0")) @[DCache.scala 506:34]
    node wshrPass = and(_wshrPass_T, _wshrPass_T_1) @[DCache.scala 506:31]
    node _WshrAccess_io_pushReq_valid_T = and(wshrPass, memReq_Q.io_deq_valid) @[DCache.scala 507:43]
    node _WshrAccess_io_pushReq_valid_T_1 = and(_WshrAccess_io_pushReq_valid_T, memReqIsWrite_st3) @[DCache.scala 507:68]
    node _memReq_Q_io_deq_ready_T = and(wshrPass, io_memReq_ready) @[DCache.scala 510:37]
    node _T_85 = and(wshrPass, memReq_Q.io_deq_valid) @[DCache.scala 512:17]
    node _GEN_140 = mux(_T_85, memReq_Q.io_deq_bits_a_source, memReq_st3_a_source) @[DCache.scala 512:43 513:16 497:23]
    node _GEN_141 = mux(_T_85, memReq_Q.io_deq_bits_a_mask_0, memReq_st3_a_mask_0) @[DCache.scala 512:43 513:16 497:23]
    node _GEN_142 = mux(_T_85, memReq_Q.io_deq_bits_a_mask_1, memReq_st3_a_mask_1) @[DCache.scala 512:43 513:16 497:23]
    node _GEN_143 = mux(_T_85, memReq_Q.io_deq_bits_a_mask_2, memReq_st3_a_mask_2) @[DCache.scala 512:43 513:16 497:23]
    node _GEN_144 = mux(_T_85, memReq_Q.io_deq_bits_a_mask_3, memReq_st3_a_mask_3) @[DCache.scala 512:43 513:16 497:23]
    node _GEN_145 = mux(_T_85, memReq_Q.io_deq_bits_a_data_0, memReq_st3_a_data_0) @[DCache.scala 512:43 513:16 497:23]
    node _GEN_146 = mux(_T_85, memReq_Q.io_deq_bits_a_data_1, memReq_st3_a_data_1) @[DCache.scala 512:43 513:16 497:23]
    node _GEN_147 = mux(_T_85, memReq_Q.io_deq_bits_a_data_2, memReq_st3_a_data_2) @[DCache.scala 512:43 513:16 497:23]
    node _GEN_148 = mux(_T_85, memReq_Q.io_deq_bits_a_data_3, memReq_st3_a_data_3) @[DCache.scala 512:43 513:16 497:23]
    node _GEN_149 = mux(_T_85, memReq_Q.io_deq_bits_a_addr, memReq_st3_a_addr) @[DCache.scala 512:43 513:16 497:23]
    node _GEN_150 = mux(_T_85, memReq_Q.io_deq_bits_a_param, memReq_st3_a_param) @[DCache.scala 512:43 513:16 497:23]
    node _GEN_151 = mux(_T_85, memReq_Q.io_deq_bits_a_opcode, memReq_st3_a_opcode) @[DCache.scala 512:43 513:16 497:23]
    node memReqSetIdx_st2 = bits(memReq_Q.io_deq_bits_a_addr, 5, 4) @[DCache.scala 517:53]
    node memReq_st3_a_source_hi = cat(UInt<1>("h0"), WshrAccess.io_pushedIdx) @[Cat.scala 31:58]
    node _memReq_st3_a_source_T = cat(memReq_st3_a_source_hi, memReqSetIdx_st2) @[Cat.scala 31:58]
    node _GEN_152 = mux(memReqIsWrite_st3, _memReq_st3_a_source_T, _GEN_140) @[DCache.scala 518:26 519:27]
    reg memReq_valid : UInt<1>, clock with :
      reset => (UInt<1>("h0"), memReq_valid) @[DCache.scala 531:29]
    node _T_86 = and(memReq_Q.io_deq_ready, memReq_Q.io_deq_valid) @[Decoupled.scala 50:35]
    node _T_87 = and(io_memReq_ready, io_memReq_valid) @[Decoupled.scala 50:35]
    node _T_88 = xor(_T_86, _T_87) @[DCache.scala 532:29]
    node _memReq_valid_T = and(memReq_Q.io_deq_ready, memReq_Q.io_deq_valid) @[Decoupled.scala 50:35]
    node _GEN_153 = mux(_T_88, _memReq_valid_T, memReq_valid) @[DCache.scala 532:46 533:18 531:29]
    node coreRsp_st2_valid = _coreRsp_st2_valid_T_1 @[DCache.scala 140:30 463:21]
    node missMemReq_a_opcode = _missMemReq_T_a_opcode @[DCache.scala 197:24 218:14]
    node missMemReq_a_param = _missMemReq_T_a_param @[DCache.scala 197:24 218:14]
    node missMemReq_a_addr = _missMemReq_T_a_addr @[DCache.scala 197:24 218:14]
    node missMemReq_a_data_0 = _missMemReq_T_a_data_0 @[DCache.scala 197:24 218:14]
    node missMemReq_a_data_1 = _missMemReq_T_a_data_1 @[DCache.scala 197:24 218:14]
    node missMemReq_a_data_2 = _missMemReq_T_a_data_2 @[DCache.scala 197:24 218:14]
    node missMemReq_a_data_3 = _missMemReq_T_a_data_3 @[DCache.scala 197:24 218:14]
    node missMemReq_a_mask_0 = _missMemReq_T_a_mask_0 @[DCache.scala 197:24 218:14]
    node missMemReq_a_mask_1 = _missMemReq_T_a_mask_1 @[DCache.scala 197:24 218:14]
    node missMemReq_a_mask_2 = _missMemReq_T_a_mask_2 @[DCache.scala 197:24 218:14]
    node missMemReq_a_mask_3 = _missMemReq_T_a_mask_3 @[DCache.scala 197:24 218:14]
    node missMemReq_a_source = _missMemReq_T_a_source @[DCache.scala 197:24 218:14]
    node missMemReq_hasCoreRsp = _missMemReq_T_hasCoreRsp @[DCache.scala 197:24 218:14]
    node missMemReq_coreRspInstrId = _missMemReq_T_coreRspInstrId @[DCache.scala 197:24 218:14]
    node InvOrFluMemReq_a_opcode = UInt<3>("h0") @[DCache.scala 284:28 285:27]
    node InvOrFluMemReq_a_param = UInt<3>("h0") @[DCache.scala 284:28 286:26]
    node InvOrFluMemReq_a_addr = _InvOrFluMemReq_a_addr_T @[DCache.scala 284:28 288:25]
    node InvOrFluMemReq_a_data_0 = DataAccessReadSRAMRRsp_0 @[DCache.scala 284:28 485:25]
    node InvOrFluMemReq_a_data_1 = DataAccessReadSRAMRRsp_1 @[DCache.scala 284:28 485:25]
    node InvOrFluMemReq_a_data_2 = DataAccessReadSRAMRRsp_2 @[DCache.scala 284:28 485:25]
    node InvOrFluMemReq_a_data_3 = DataAccessReadSRAMRRsp_3 @[DCache.scala 284:28 485:25]
    node _WIRE_8_0 = UInt<1>("h1") @[DCache.scala 290:{35,35}]
    node InvOrFluMemReq_a_mask_0 = _WIRE_8_0 @[DCache.scala 284:28 290:25]
    node _WIRE_8_1 = UInt<1>("h1") @[DCache.scala 290:{35,35}]
    node InvOrFluMemReq_a_mask_1 = _WIRE_8_1 @[DCache.scala 284:28 290:25]
    node _WIRE_8_2 = UInt<1>("h1") @[DCache.scala 290:{35,35}]
    node InvOrFluMemReq_a_mask_2 = _WIRE_8_2 @[DCache.scala 284:28 290:25]
    node _WIRE_8_3 = UInt<1>("h1") @[DCache.scala 290:{35,35}]
    node InvOrFluMemReq_a_mask_3 = _WIRE_8_3 @[DCache.scala 284:28 290:25]
    node InvOrFluMemReq_a_source = validif(UInt<1>("h0"), UInt<7>("h0"))
    node InvOrFluMemReq_hasCoreRsp = UInt<1>("h0") @[DCache.scala 284:28 292:29]
    node InvOrFluMemReq_coreRspInstrId = validif(UInt<1>("h0"), UInt<32>("h0"))
    node dirtyReplace_st1_a_opcode = UInt<3>("h0") @[DCache.scala 368:30 369:29]
    node dirtyReplace_st1_a_param = UInt<3>("h0") @[DCache.scala 368:30 370:28]
    node dirtyReplace_st1_a_addr = pad(TagAccess.io_a_addrReplacement_st1, 32) @[DCache.scala 368:30 372:27]
    node dirtyReplace_st1_a_data_0 = DataAccessReadSRAMRRsp_0 @[DCache.scala 368:30 483:27]
    node dirtyReplace_st1_a_data_1 = DataAccessReadSRAMRRsp_1 @[DCache.scala 368:30 483:27]
    node dirtyReplace_st1_a_data_2 = DataAccessReadSRAMRRsp_2 @[DCache.scala 368:30 483:27]
    node dirtyReplace_st1_a_data_3 = DataAccessReadSRAMRRsp_3 @[DCache.scala 368:30 483:27]
    node _WIRE_17_0 = UInt<1>("h1") @[DCache.scala 373:{37,37}]
    node dirtyReplace_st1_a_mask_0 = _WIRE_17_0 @[DCache.scala 368:30 373:27]
    node _WIRE_17_1 = UInt<1>("h1") @[DCache.scala 373:{37,37}]
    node dirtyReplace_st1_a_mask_1 = _WIRE_17_1 @[DCache.scala 368:30 373:27]
    node _WIRE_17_2 = UInt<1>("h1") @[DCache.scala 373:{37,37}]
    node dirtyReplace_st1_a_mask_2 = _WIRE_17_2 @[DCache.scala 368:30 373:27]
    node _WIRE_17_3 = UInt<1>("h1") @[DCache.scala 373:{37,37}]
    node dirtyReplace_st1_a_mask_3 = _WIRE_17_3 @[DCache.scala 368:30 373:27]
    node dirtyReplace_st1_a_source = validif(UInt<1>("h0"), UInt<7>("h0"))
    node dirtyReplace_st1_hasCoreRsp = UInt<1>("h0") @[DCache.scala 368:30 375:31]
    node dirtyReplace_st1_coreRspInstrId = validif(UInt<1>("h0"), UInt<32>("h0"))
    node _coreRsp_st2_dataMemOrder_coreRsp_st2_perLaneAddr_0_blockOffset = _GEN_127 @[DCache.scala 475:34]
    node coreRsp_st2_dataCoreOrder_0 = _coreRsp_st2_dataMemOrder_coreRsp_st2_perLaneAddr_0_blockOffset @[DCache.scala 471:39 475:34]
    node _coreRsp_st2_dataMemOrder_coreRsp_st2_perLaneAddr_1_blockOffset = _GEN_131 @[DCache.scala 475:34]
    node coreRsp_st2_dataCoreOrder_1 = _coreRsp_st2_dataMemOrder_coreRsp_st2_perLaneAddr_1_blockOffset @[DCache.scala 471:39 475:34]
    node _coreRsp_st2_dataMemOrder_coreRsp_st2_perLaneAddr_2_blockOffset = _GEN_135 @[DCache.scala 475:34]
    node coreRsp_st2_dataCoreOrder_2 = _coreRsp_st2_dataMemOrder_coreRsp_st2_perLaneAddr_2_blockOffset @[DCache.scala 471:39 475:34]
    node _coreRsp_st2_dataMemOrder_coreRsp_st2_perLaneAddr_3_blockOffset = _GEN_139 @[DCache.scala 475:34]
    node coreRsp_st2_dataCoreOrder_3 = _coreRsp_st2_dataMemOrder_coreRsp_st2_perLaneAddr_3_blockOffset @[DCache.scala 471:39 475:34]
    io_coreReq_ready <= coreReq_Q.io_enq_ready @[DCache.scala 118:20]
    io_coreRsp_valid <= coreRsp_Q.io_deq_valid @[DCache.scala 465:20]
    io_coreRsp_bits_instrId <= coreRsp_Q.io_deq_bits_instrId @[DCache.scala 465:20]
    io_coreRsp_bits_isWrite <= coreRsp_Q.io_deq_bits_isWrite @[DCache.scala 465:20]
    io_coreRsp_bits_data_0 <= coreRsp_Q.io_deq_bits_data_0 @[DCache.scala 465:20]
    io_coreRsp_bits_data_1 <= coreRsp_Q.io_deq_bits_data_1 @[DCache.scala 465:20]
    io_coreRsp_bits_data_2 <= coreRsp_Q.io_deq_bits_data_2 @[DCache.scala 465:20]
    io_coreRsp_bits_data_3 <= coreRsp_Q.io_deq_bits_data_3 @[DCache.scala 465:20]
    io_coreRsp_bits_activeMask_0 <= coreRsp_Q.io_deq_bits_activeMask_0 @[DCache.scala 465:20]
    io_coreRsp_bits_activeMask_1 <= coreRsp_Q.io_deq_bits_activeMask_1 @[DCache.scala 465:20]
    io_coreRsp_bits_activeMask_2 <= coreRsp_Q.io_deq_bits_activeMask_2 @[DCache.scala 465:20]
    io_coreRsp_bits_activeMask_3 <= coreRsp_Q.io_deq_bits_activeMask_3 @[DCache.scala 465:20]
    io_memRsp_ready <= memRsp_Q.io_enq_ready @[DCache.scala 297:19]
    io_memReq_valid <= memReq_valid @[DCache.scala 535:19]
    io_memReq_bits_a_opcode <= memReq_st3_a_opcode @[DCache.scala 529:18]
    io_memReq_bits_a_param <= memReq_st3_a_param @[DCache.scala 529:18]
    io_memReq_bits_a_addr <= memReq_st3_a_addr @[DCache.scala 529:18]
    io_memReq_bits_a_data_0 <= memReq_st3_a_data_0 @[DCache.scala 529:18]
    io_memReq_bits_a_data_1 <= memReq_st3_a_data_1 @[DCache.scala 529:18]
    io_memReq_bits_a_data_2 <= memReq_st3_a_data_2 @[DCache.scala 529:18]
    io_memReq_bits_a_data_3 <= memReq_st3_a_data_3 @[DCache.scala 529:18]
    io_memReq_bits_a_mask_0 <= memReq_st3_a_mask_0 @[DCache.scala 529:18]
    io_memReq_bits_a_mask_1 <= memReq_st3_a_mask_1 @[DCache.scala 529:18]
    io_memReq_bits_a_mask_2 <= memReq_st3_a_mask_2 @[DCache.scala 529:18]
    io_memReq_bits_a_mask_3 <= memReq_st3_a_mask_3 @[DCache.scala 529:18]
    io_memReq_bits_a_source <= memReq_st3_a_source @[DCache.scala 529:18]
    MshrAccess.clock <= clock
    MshrAccess.reset <= reset
    MshrAccess.io_probe_valid <= _MshrAccess_io_probe_valid_T @[DCache.scala 154:29]
    MshrAccess.io_probe_bits_blockAddr <= _MshrAccess_io_probe_bits_blockAddr_T @[DCache.scala 155:38]
    MshrAccess.io_missReq_valid <= _MshrAccess_io_missReq_valid_T_4 @[DCache.scala 187:31]
    MshrAccess.io_missReq_bits_blockAddr <= _MshrAccess_io_missReq_bits_blockAddr_T @[DCache.scala 193:40]
    MshrAccess.io_missReq_bits_instrId <= coreReq_Q.io_deq_bits_instrId @[DCache.scala 192:38]
    MshrAccess.io_missReq_bits_targetInfo <= _MshrAccess_io_missReq_bits_targetInfo_T @[DCache.scala 194:41]
    MshrAccess.io_missRspIn_valid <= _MshrAccess_io_missRspIn_valid_T_1 @[DCache.scala 383:33]
    MshrAccess.io_missRspIn_bits_instrId <= _MshrAccess_io_missRspIn_bits_instrId_T @[DCache.scala 384:40]
    TagAccess.clock <= clock
    TagAccess.reset <= reset
    TagAccess.io_probeRead_valid <= _TagAccess_io_probeRead_valid_T @[DCache.scala 149:32]
    TagAccess.io_probeRead_bits_setIdx <= io_coreReq_bits_setIdx @[DCache.scala 150:38]
    TagAccess.io_tagFromCore_st1 <= coreReq_Q.io_deq_bits_tag @[DCache.scala 151:32]
    TagAccess.io_probeIsWrite_st1 <= writeHit_st1 @[DCache.scala 183:37]
    TagAccess.io_allocateWrite_valid <= _TagAccess_io_allocateWrite_valid_T_1 @[DCache.scala 387:36]
    TagAccess.io_allocateWrite_bits_setIdx <= _TagAccess_io_allocateWrite_bits_setIdx_T @[DCache.scala 388:42]
    TagAccess.io_allocateWriteData_st1 <= _TagAccess_io_allocateWriteData_st1_T @[DCache.scala 396:38]
    TagAccess.io_allocateWriteTagSRAMWValid_st1 <= _TagAccess_io_allocateWriteTagSRAMWValid_st1_T @[DCache.scala 397:47]
    TagAccess.io_flushChoosen_valid <= _TagAccess_io_flushChoosen_valid_T_1 @[DCache.scala 171:39]
    TagAccess.io_flushChoosen_bits <= _TagAccess_io_flushChoosen_bits_T @[DCache.scala 173:38]
    TagAccess.io_invalidateAll <= _TagAccess_io_invalidateAll_T_2 @[DCache.scala 295:30]
    WshrAccess.clock <= clock
    WshrAccess.reset <= reset
    WshrAccess.io_pushReq_valid <= _WshrAccess_io_pushReq_valid_T_1 @[DCache.scala 507:31]
    WshrAccess.io_pushReq_bits_blockAddr <= _WshrAccess_io_pushReq_bits_blockAddr_T @[DCache.scala 502:40]
    WshrAccess.io_popReq_valid <= _WshrAccess_io_popReq_valid_T @[DCache.scala 321:30]
    WshrAccess.io_popReq_bits <= _WshrAccess_io_popReq_bits_T @[DCache.scala 322:29]
    coreReq_Q.clock <= clock
    coreReq_Q.reset <= reset
    coreReq_Q.io_enq_valid <= io_coreReq_valid @[DCache.scala 117:26]
    coreReq_Q.io_enq_bits_instrId <= io_coreReq_bits_instrId @[DCache.scala 119:25]
    coreReq_Q.io_enq_bits_opcode <= io_coreReq_bits_opcode @[DCache.scala 119:25]
    coreReq_Q.io_enq_bits_param <= io_coreReq_bits_param @[DCache.scala 119:25]
    coreReq_Q.io_enq_bits_tag <= io_coreReq_bits_tag @[DCache.scala 119:25]
    coreReq_Q.io_enq_bits_setIdx <= io_coreReq_bits_setIdx @[DCache.scala 119:25]
    coreReq_Q.io_enq_bits_perLaneAddr_0_activeMask <= io_coreReq_bits_perLaneAddr_0_activeMask @[DCache.scala 119:25]
    coreReq_Q.io_enq_bits_perLaneAddr_0_blockOffset <= io_coreReq_bits_perLaneAddr_0_blockOffset @[DCache.scala 119:25]
    coreReq_Q.io_enq_bits_perLaneAddr_0_wordOffset1H <= io_coreReq_bits_perLaneAddr_0_wordOffset1H @[DCache.scala 119:25]
    coreReq_Q.io_enq_bits_perLaneAddr_1_activeMask <= io_coreReq_bits_perLaneAddr_1_activeMask @[DCache.scala 119:25]
    coreReq_Q.io_enq_bits_perLaneAddr_1_blockOffset <= io_coreReq_bits_perLaneAddr_1_blockOffset @[DCache.scala 119:25]
    coreReq_Q.io_enq_bits_perLaneAddr_1_wordOffset1H <= io_coreReq_bits_perLaneAddr_1_wordOffset1H @[DCache.scala 119:25]
    coreReq_Q.io_enq_bits_perLaneAddr_2_activeMask <= io_coreReq_bits_perLaneAddr_2_activeMask @[DCache.scala 119:25]
    coreReq_Q.io_enq_bits_perLaneAddr_2_blockOffset <= io_coreReq_bits_perLaneAddr_2_blockOffset @[DCache.scala 119:25]
    coreReq_Q.io_enq_bits_perLaneAddr_2_wordOffset1H <= io_coreReq_bits_perLaneAddr_2_wordOffset1H @[DCache.scala 119:25]
    coreReq_Q.io_enq_bits_perLaneAddr_3_activeMask <= io_coreReq_bits_perLaneAddr_3_activeMask @[DCache.scala 119:25]
    coreReq_Q.io_enq_bits_perLaneAddr_3_blockOffset <= io_coreReq_bits_perLaneAddr_3_blockOffset @[DCache.scala 119:25]
    coreReq_Q.io_enq_bits_perLaneAddr_3_wordOffset1H <= io_coreReq_bits_perLaneAddr_3_wordOffset1H @[DCache.scala 119:25]
    coreReq_Q.io_enq_bits_data_0 <= io_coreReq_bits_data_0 @[DCache.scala 119:25]
    coreReq_Q.io_enq_bits_data_1 <= io_coreReq_bits_data_1 @[DCache.scala 119:25]
    coreReq_Q.io_enq_bits_data_2 <= io_coreReq_bits_data_2 @[DCache.scala 119:25]
    coreReq_Q.io_enq_bits_data_3 <= io_coreReq_bits_data_3 @[DCache.scala 119:25]
    coreReq_Q.io_deq_ready <= _coreReq_Q_io_deq_ready_T_4 @[DCache.scala 145:26]
    coreRsp_Q.clock <= clock
    coreRsp_Q.reset <= reset
    coreRsp_Q.io_enq_valid <= coreRsp_st2_valid @[DCache.scala 466:26]
    coreRsp_Q.io_enq_bits_instrId <= _coreRsp_Q_io_enq_bits_T_instrId @[DCache.scala 467:25]
    coreRsp_Q.io_enq_bits_isWrite <= _coreRsp_Q_io_enq_bits_T_isWrite @[DCache.scala 467:25]
    coreRsp_Q.io_enq_bits_data_0 <= coreRsp_st2_dataCoreOrder_0 @[DCache.scala 478:30]
    coreRsp_Q.io_enq_bits_data_1 <= coreRsp_st2_dataCoreOrder_1 @[DCache.scala 478:30]
    coreRsp_Q.io_enq_bits_data_2 <= coreRsp_st2_dataCoreOrder_2 @[DCache.scala 478:30]
    coreRsp_Q.io_enq_bits_data_3 <= coreRsp_st2_dataCoreOrder_3 @[DCache.scala 478:30]
    coreRsp_Q.io_enq_bits_activeMask_0 <= _coreRsp_Q_io_enq_bits_T_activeMask_0 @[DCache.scala 467:25]
    coreRsp_Q.io_enq_bits_activeMask_1 <= _coreRsp_Q_io_enq_bits_T_activeMask_1 @[DCache.scala 467:25]
    coreRsp_Q.io_enq_bits_activeMask_2 <= _coreRsp_Q_io_enq_bits_T_activeMask_2 @[DCache.scala 467:25]
    coreRsp_Q.io_enq_bits_activeMask_3 <= _coreRsp_Q_io_enq_bits_T_activeMask_3 @[DCache.scala 467:25]
    coreRsp_Q.io_deq_ready <= io_coreRsp_ready @[DCache.scala 465:20]
    memRsp_Q.clock <= clock
    memRsp_Q.reset <= reset
    memRsp_Q.io_enq_valid <= io_memRsp_valid @[DCache.scala 297:19]
    memRsp_Q.io_enq_bits_d_opcode <= io_memRsp_bits_d_opcode @[DCache.scala 297:19]
    memRsp_Q.io_enq_bits_d_source <= io_memRsp_bits_d_source @[DCache.scala 297:19]
    memRsp_Q.io_enq_bits_d_addr <= io_memRsp_bits_d_addr @[DCache.scala 297:19]
    memRsp_Q.io_enq_bits_d_data_0 <= io_memRsp_bits_d_data_0 @[DCache.scala 297:19]
    memRsp_Q.io_enq_bits_d_data_1 <= io_memRsp_bits_d_data_1 @[DCache.scala 297:19]
    memRsp_Q.io_enq_bits_d_data_2 <= io_memRsp_bits_d_data_2 @[DCache.scala 297:19]
    memRsp_Q.io_enq_bits_d_data_3 <= io_memRsp_bits_d_data_3 @[DCache.scala 297:19]
    memRsp_Q.io_deq_ready <= _memRsp_Q_io_deq_ready_T_3 @[DCache.scala 318:25]
    memReq_Q.clock <= clock
    memReq_Q.reset <= reset
    memReq_Q.io_enq_valid <= MemReqArb.io_out_valid @[DCache.scala 488:19]
    memReq_Q.io_enq_bits_a_opcode <= MemReqArb.io_out_bits_a_opcode @[DCache.scala 488:19]
    memReq_Q.io_enq_bits_a_param <= MemReqArb.io_out_bits_a_param @[DCache.scala 488:19]
    memReq_Q.io_enq_bits_a_addr <= MemReqArb.io_out_bits_a_addr @[DCache.scala 488:19]
    memReq_Q.io_enq_bits_a_data_0 <= MemReqArb.io_out_bits_a_data_0 @[DCache.scala 488:19]
    memReq_Q.io_enq_bits_a_data_1 <= MemReqArb.io_out_bits_a_data_1 @[DCache.scala 488:19]
    memReq_Q.io_enq_bits_a_data_2 <= MemReqArb.io_out_bits_a_data_2 @[DCache.scala 488:19]
    memReq_Q.io_enq_bits_a_data_3 <= MemReqArb.io_out_bits_a_data_3 @[DCache.scala 488:19]
    memReq_Q.io_enq_bits_a_mask_0 <= MemReqArb.io_out_bits_a_mask_0 @[DCache.scala 488:19]
    memReq_Q.io_enq_bits_a_mask_1 <= MemReqArb.io_out_bits_a_mask_1 @[DCache.scala 488:19]
    memReq_Q.io_enq_bits_a_mask_2 <= MemReqArb.io_out_bits_a_mask_2 @[DCache.scala 488:19]
    memReq_Q.io_enq_bits_a_mask_3 <= MemReqArb.io_out_bits_a_mask_3 @[DCache.scala 488:19]
    memReq_Q.io_enq_bits_a_source <= MemReqArb.io_out_bits_a_source @[DCache.scala 488:19]
    memReq_Q.io_enq_bits_hasCoreRsp <= MemReqArb.io_out_bits_hasCoreRsp @[DCache.scala 488:19]
    memReq_Q.io_enq_bits_coreRspInstrId <= MemReqArb.io_out_bits_coreRspInstrId @[DCache.scala 488:19]
    memReq_Q.io_deq_ready <= _memReq_Q_io_deq_ready_T @[DCache.scala 510:25]
    MemReqArb.clock <= clock
    MemReqArb.reset <= reset
    MemReqArb.io_in_0_valid <= tagReplaceStatus @[DCache.scala 489:28]
    MemReqArb.io_in_0_bits_a_opcode <= dirtyReplace_st2_a_opcode @[DCache.scala 490:27]
    MemReqArb.io_in_0_bits_a_param <= dirtyReplace_st2_a_param @[DCache.scala 490:27]
    MemReqArb.io_in_0_bits_a_addr <= dirtyReplace_st2_a_addr @[DCache.scala 490:27]
    MemReqArb.io_in_0_bits_a_data_0 <= dirtyReplace_st2_a_data_0 @[DCache.scala 490:27]
    MemReqArb.io_in_0_bits_a_data_1 <= dirtyReplace_st2_a_data_1 @[DCache.scala 490:27]
    MemReqArb.io_in_0_bits_a_data_2 <= dirtyReplace_st2_a_data_2 @[DCache.scala 490:27]
    MemReqArb.io_in_0_bits_a_data_3 <= dirtyReplace_st2_a_data_3 @[DCache.scala 490:27]
    MemReqArb.io_in_0_bits_a_mask_0 <= dirtyReplace_st2_a_mask_0 @[DCache.scala 490:27]
    MemReqArb.io_in_0_bits_a_mask_1 <= dirtyReplace_st2_a_mask_1 @[DCache.scala 490:27]
    MemReqArb.io_in_0_bits_a_mask_2 <= dirtyReplace_st2_a_mask_2 @[DCache.scala 490:27]
    MemReqArb.io_in_0_bits_a_mask_3 <= dirtyReplace_st2_a_mask_3 @[DCache.scala 490:27]
    MemReqArb.io_in_0_bits_a_source <= dirtyReplace_st2_a_source @[DCache.scala 490:27]
    MemReqArb.io_in_0_bits_hasCoreRsp <= dirtyReplace_st2_hasCoreRsp @[DCache.scala 490:27]
    MemReqArb.io_in_0_bits_coreRspInstrId <= dirtyReplace_st2_coreRspInstrId @[DCache.scala 490:27]
    MemReqArb.io_in_1_valid <= _MemReqArb_io_in_1_valid_T_3 @[DCache.scala 491:28]
    MemReqArb.io_in_1_bits_a_opcode <= missMemReq_a_opcode @[DCache.scala 492:27]
    MemReqArb.io_in_1_bits_a_param <= missMemReq_a_param @[DCache.scala 492:27]
    MemReqArb.io_in_1_bits_a_addr <= missMemReq_a_addr @[DCache.scala 492:27]
    MemReqArb.io_in_1_bits_a_data_0 <= missMemReq_a_data_0 @[DCache.scala 492:27]
    MemReqArb.io_in_1_bits_a_data_1 <= missMemReq_a_data_1 @[DCache.scala 492:27]
    MemReqArb.io_in_1_bits_a_data_2 <= missMemReq_a_data_2 @[DCache.scala 492:27]
    MemReqArb.io_in_1_bits_a_data_3 <= missMemReq_a_data_3 @[DCache.scala 492:27]
    MemReqArb.io_in_1_bits_a_mask_0 <= missMemReq_a_mask_0 @[DCache.scala 492:27]
    MemReqArb.io_in_1_bits_a_mask_1 <= missMemReq_a_mask_1 @[DCache.scala 492:27]
    MemReqArb.io_in_1_bits_a_mask_2 <= missMemReq_a_mask_2 @[DCache.scala 492:27]
    MemReqArb.io_in_1_bits_a_mask_3 <= missMemReq_a_mask_3 @[DCache.scala 492:27]
    MemReqArb.io_in_1_bits_a_source <= missMemReq_a_source @[DCache.scala 492:27]
    MemReqArb.io_in_1_bits_hasCoreRsp <= missMemReq_hasCoreRsp @[DCache.scala 492:27]
    MemReqArb.io_in_1_bits_coreRspInstrId <= missMemReq_coreRspInstrId @[DCache.scala 492:27]
    MemReqArb.io_in_2_valid <= InvOrFluMemReqValid_st1 @[DCache.scala 493:28]
    MemReqArb.io_in_2_bits_a_opcode <= InvOrFluMemReq_a_opcode @[DCache.scala 494:27]
    MemReqArb.io_in_2_bits_a_param <= InvOrFluMemReq_a_param @[DCache.scala 494:27]
    MemReqArb.io_in_2_bits_a_addr <= InvOrFluMemReq_a_addr @[DCache.scala 494:27]
    MemReqArb.io_in_2_bits_a_data_0 <= InvOrFluMemReq_a_data_0 @[DCache.scala 494:27]
    MemReqArb.io_in_2_bits_a_data_1 <= InvOrFluMemReq_a_data_1 @[DCache.scala 494:27]
    MemReqArb.io_in_2_bits_a_data_2 <= InvOrFluMemReq_a_data_2 @[DCache.scala 494:27]
    MemReqArb.io_in_2_bits_a_data_3 <= InvOrFluMemReq_a_data_3 @[DCache.scala 494:27]
    MemReqArb.io_in_2_bits_a_mask_0 <= InvOrFluMemReq_a_mask_0 @[DCache.scala 494:27]
    MemReqArb.io_in_2_bits_a_mask_1 <= InvOrFluMemReq_a_mask_1 @[DCache.scala 494:27]
    MemReqArb.io_in_2_bits_a_mask_2 <= InvOrFluMemReq_a_mask_2 @[DCache.scala 494:27]
    MemReqArb.io_in_2_bits_a_mask_3 <= InvOrFluMemReq_a_mask_3 @[DCache.scala 494:27]
    MemReqArb.io_in_2_bits_a_source <= InvOrFluMemReq_a_source @[DCache.scala 494:27]
    MemReqArb.io_in_2_bits_hasCoreRsp <= InvOrFluMemReq_hasCoreRsp @[DCache.scala 494:27]
    MemReqArb.io_in_2_bits_coreRspInstrId <= InvOrFluMemReq_coreRspInstrId @[DCache.scala 494:27]
    MemReqArb.io_out_ready <= memReq_Q.io_enq_ready @[DCache.scala 488:19]
    secondaryFullReturn <= _secondaryFullReturn_T @[DCache.scala 127:36]
    coreReqControl_st1_isRead <= _GEN_0
    coreReqControl_st1_isWrite <= _GEN_1
    coreReqControl_st1_isLR <= _GEN_2
    coreReqControl_st1_isSC <= _GEN_3
    coreReqControl_st1_isAMO <= _GEN_4
    coreReqControl_st1_isFlush <= _GEN_5
    coreReqControl_st1_isInvalidate <= _GEN_6
    coreReqControl_st1_isWaitMSHR <= _GEN_7
    coreRsp_st2_instrId <= _GEN_107
    coreRsp_st2_isWrite <= _GEN_89
    coreRsp_st2_data_0 <= _GEN_85
    coreRsp_st2_data_1 <= _GEN_86
    coreRsp_st2_data_2 <= _GEN_87
    coreRsp_st2_data_3 <= _GEN_88
    coreRsp_st2_activeMask_0 <= _GEN_108
    coreRsp_st2_activeMask_1 <= _GEN_109
    coreRsp_st2_activeMask_2 <= _GEN_110
    coreRsp_st2_activeMask_3 <= _GEN_111
    coreRsp_st2_perLaneAddr_0_activeMask <= _GEN_112
    coreRsp_st2_perLaneAddr_0_blockOffset <= _GEN_113
    coreRsp_st2_perLaneAddr_0_wordOffset1H <= _GEN_114
    coreRsp_st2_perLaneAddr_1_activeMask <= _GEN_115
    coreRsp_st2_perLaneAddr_1_blockOffset <= _GEN_116
    coreRsp_st2_perLaneAddr_1_wordOffset1H <= _GEN_117
    coreRsp_st2_perLaneAddr_2_activeMask <= _GEN_118
    coreRsp_st2_perLaneAddr_2_blockOffset <= _GEN_119
    coreRsp_st2_perLaneAddr_2_wordOffset1H <= _GEN_120
    coreRsp_st2_perLaneAddr_3_activeMask <= _GEN_121
    coreRsp_st2_perLaneAddr_3_blockOffset <= _GEN_122
    coreRsp_st2_perLaneAddr_3_wordOffset1H <= _GEN_123
    readHit_st2 <= readHit_st1 @[DCache.scala 143:28]
    genCtrl.clock <= clock
    genCtrl.reset <= reset
    genCtrl.io_opcode <= io_coreReq_bits_opcode @[DCache.scala 158:21]
    genCtrl.io_param <= io_coreReq_bits_param @[DCache.scala 159:20]
    coreReqTagHasDirty_st1 <= mux(reset, UInt<1>("h0"), TagAccess.io_hasDirty_st0) @[DCache.scala 163:{39,39} 164:26]
    MshrAccess_io_missReq_valid_REG <= secondaryFullReturn @[DCache.scala 187:114]
    getBankEn.clock <= clock
    getBankEn.reset <= reset
    getBankEn.io_perLaneBlockIdx_0 <= coreReq_Q.io_deq_bits_perLaneAddr_0_blockOffset @[DCache.scala 222:32]
    getBankEn.io_perLaneBlockIdx_1 <= coreReq_Q.io_deq_bits_perLaneAddr_1_blockOffset @[DCache.scala 222:32]
    getBankEn.io_perLaneBlockIdx_2 <= coreReq_Q.io_deq_bits_perLaneAddr_2_blockOffset @[DCache.scala 222:32]
    getBankEn.io_perLaneBlockIdx_3 <= coreReq_Q.io_deq_bits_perLaneAddr_3_blockOffset @[DCache.scala 222:32]
    getBankEn.io_perLaneValid_0 <= coreReq_Q.io_deq_bits_perLaneAddr_0_activeMask @[DCache.scala 223:29]
    getBankEn.io_perLaneValid_1 <= coreReq_Q.io_deq_bits_perLaneAddr_1_activeMask @[DCache.scala 223:29]
    getBankEn.io_perLaneValid_2 <= coreReq_Q.io_deq_bits_perLaneAddr_2_activeMask @[DCache.scala 223:29]
    getBankEn.io_perLaneValid_3 <= coreReq_Q.io_deq_bits_perLaneAddr_3_activeMask @[DCache.scala 223:29]
    InvOrFluMemReq_a_addr_REG <= TagAccess.io_dirtySetIdx_st0 @[DCache.scala 289:12]
    tagReqValidCtrl <= mux(reset, UInt<1>("h1"), _GEN_53) @[DCache.scala 300:{32,32}]
    tagReqReadyCtrl <= mux(reset, UInt<1>("h0"), _GEN_55) @[DCache.scala 301:{32,32}]
    memRsp_st1_d_opcode <= _GEN_60
    memRsp_st1_d_source <= _GEN_61
    memRsp_st1_d_addr <= _GEN_62
    memRsp_st1_d_data_0 <= _GEN_63
    memRsp_st1_d_data_1 <= _GEN_64
    memRsp_st1_d_data_2 <= _GEN_65
    memRsp_st1_d_data_3 <= _GEN_66
    tagReplaceStatus <= mux(reset, UInt<1>("h0"), _GEN_58) @[DCache.scala 333:{33,33}]
    dataReplaceReadValid_REG <= TagAccess.io_allocateWrite_valid @[DCache.scala 351:37]
    dataFillVaild_REG <= TagAccess.io_allocateWrite_valid @[DCache.scala 357:30]
    TagAccess_io_allocateWriteTagSRAMWValid_st1_REG <= TagAccess.io_allocateWrite_valid @[DCache.scala 397:57]
    DataAccessesRRsp_DataAccess.clock <= clock
    DataAccessesRRsp_DataAccess.reset <= reset
    DataAccessesRRsp_DataAccess.io_r_req_valid <= _DataAccessesRRsp_DataAccess_io_r_req_valid_T_1 @[DCache.scala 414:31]
    DataAccessesRRsp_DataAccess.io_r_req_bits_setIdx <= _GEN_68
    DataAccessesRRsp_DataAccess.io_w_req_valid <= _DataAccessesRRsp_DataAccess_io_w_req_valid_T_1 @[DCache.scala 410:31]
    DataAccessesRRsp_DataAccess.io_w_req_bits_setIdx <= _DataAccessesRRsp_DataAccess_io_w_req_bits_T_1_setIdx @[DCache.scala 413:30]
    DataAccessesRRsp_DataAccess.io_w_req_bits_data_0 <= _DataAccessesRRsp_DataAccess_io_w_req_bits_T_1_data_0 @[DCache.scala 413:30]
    DataAccessesRRsp_DataAccess.io_w_req_bits_data_1 <= _DataAccessesRRsp_DataAccess_io_w_req_bits_T_1_data_1 @[DCache.scala 413:30]
    DataAccessesRRsp_DataAccess.io_w_req_bits_data_2 <= _DataAccessesRRsp_DataAccess_io_w_req_bits_T_1_data_2 @[DCache.scala 413:30]
    DataAccessesRRsp_DataAccess.io_w_req_bits_data_3 <= _DataAccessesRRsp_DataAccess_io_w_req_bits_T_1_data_3 @[DCache.scala 413:30]
    DataAccessesRRsp_DataAccess.io_w_req_bits_waymask <= _DataAccessesRRsp_DataAccess_io_w_req_bits_T_1_waymask @[DCache.scala 413:30]
    DataAccessesRRsp_DataAccess_1.clock <= clock
    DataAccessesRRsp_DataAccess_1.reset <= reset
    DataAccessesRRsp_DataAccess_1.io_r_req_valid <= _DataAccessesRRsp_DataAccess_io_r_req_valid_T_3 @[DCache.scala 414:31]
    DataAccessesRRsp_DataAccess_1.io_r_req_bits_setIdx <= _GEN_70
    DataAccessesRRsp_DataAccess_1.io_w_req_valid <= _DataAccessesRRsp_DataAccess_io_w_req_valid_T_3 @[DCache.scala 410:31]
    DataAccessesRRsp_DataAccess_1.io_w_req_bits_setIdx <= _DataAccessesRRsp_DataAccess_io_w_req_bits_T_3_setIdx @[DCache.scala 413:30]
    DataAccessesRRsp_DataAccess_1.io_w_req_bits_data_0 <= _DataAccessesRRsp_DataAccess_io_w_req_bits_T_3_data_0 @[DCache.scala 413:30]
    DataAccessesRRsp_DataAccess_1.io_w_req_bits_data_1 <= _DataAccessesRRsp_DataAccess_io_w_req_bits_T_3_data_1 @[DCache.scala 413:30]
    DataAccessesRRsp_DataAccess_1.io_w_req_bits_data_2 <= _DataAccessesRRsp_DataAccess_io_w_req_bits_T_3_data_2 @[DCache.scala 413:30]
    DataAccessesRRsp_DataAccess_1.io_w_req_bits_data_3 <= _DataAccessesRRsp_DataAccess_io_w_req_bits_T_3_data_3 @[DCache.scala 413:30]
    DataAccessesRRsp_DataAccess_1.io_w_req_bits_waymask <= _DataAccessesRRsp_DataAccess_io_w_req_bits_T_3_waymask @[DCache.scala 413:30]
    DataAccessesRRsp_DataAccess_2.clock <= clock
    DataAccessesRRsp_DataAccess_2.reset <= reset
    DataAccessesRRsp_DataAccess_2.io_r_req_valid <= _DataAccessesRRsp_DataAccess_io_r_req_valid_T_5 @[DCache.scala 414:31]
    DataAccessesRRsp_DataAccess_2.io_r_req_bits_setIdx <= _GEN_72
    DataAccessesRRsp_DataAccess_2.io_w_req_valid <= _DataAccessesRRsp_DataAccess_io_w_req_valid_T_5 @[DCache.scala 410:31]
    DataAccessesRRsp_DataAccess_2.io_w_req_bits_setIdx <= _DataAccessesRRsp_DataAccess_io_w_req_bits_T_5_setIdx @[DCache.scala 413:30]
    DataAccessesRRsp_DataAccess_2.io_w_req_bits_data_0 <= _DataAccessesRRsp_DataAccess_io_w_req_bits_T_5_data_0 @[DCache.scala 413:30]
    DataAccessesRRsp_DataAccess_2.io_w_req_bits_data_1 <= _DataAccessesRRsp_DataAccess_io_w_req_bits_T_5_data_1 @[DCache.scala 413:30]
    DataAccessesRRsp_DataAccess_2.io_w_req_bits_data_2 <= _DataAccessesRRsp_DataAccess_io_w_req_bits_T_5_data_2 @[DCache.scala 413:30]
    DataAccessesRRsp_DataAccess_2.io_w_req_bits_data_3 <= _DataAccessesRRsp_DataAccess_io_w_req_bits_T_5_data_3 @[DCache.scala 413:30]
    DataAccessesRRsp_DataAccess_2.io_w_req_bits_waymask <= _DataAccessesRRsp_DataAccess_io_w_req_bits_T_5_waymask @[DCache.scala 413:30]
    DataAccessesRRsp_DataAccess_3.clock <= clock
    DataAccessesRRsp_DataAccess_3.reset <= reset
    DataAccessesRRsp_DataAccess_3.io_r_req_valid <= _DataAccessesRRsp_DataAccess_io_r_req_valid_T_7 @[DCache.scala 414:31]
    DataAccessesRRsp_DataAccess_3.io_r_req_bits_setIdx <= _GEN_74
    DataAccessesRRsp_DataAccess_3.io_w_req_valid <= _DataAccessesRRsp_DataAccess_io_w_req_valid_T_7 @[DCache.scala 410:31]
    DataAccessesRRsp_DataAccess_3.io_w_req_bits_setIdx <= _DataAccessesRRsp_DataAccess_io_w_req_bits_T_7_setIdx @[DCache.scala 413:30]
    DataAccessesRRsp_DataAccess_3.io_w_req_bits_data_0 <= _DataAccessesRRsp_DataAccess_io_w_req_bits_T_7_data_0 @[DCache.scala 413:30]
    DataAccessesRRsp_DataAccess_3.io_w_req_bits_data_1 <= _DataAccessesRRsp_DataAccess_io_w_req_bits_T_7_data_1 @[DCache.scala 413:30]
    DataAccessesRRsp_DataAccess_3.io_w_req_bits_data_2 <= _DataAccessesRRsp_DataAccess_io_w_req_bits_T_7_data_2 @[DCache.scala 413:30]
    DataAccessesRRsp_DataAccess_3.io_w_req_bits_data_3 <= _DataAccessesRRsp_DataAccess_io_w_req_bits_T_7_data_3 @[DCache.scala 413:30]
    DataAccessesRRsp_DataAccess_3.io_w_req_bits_waymask <= _DataAccessesRRsp_DataAccess_io_w_req_bits_T_7_waymask @[DCache.scala 413:30]
    REG <= _T_58 @[DCache.scala 427:31]
    REG_1 <= _T_62 @[DCache.scala 439:32]
    coreRsp_st2_valid_from_coreReq_REG <= _coreRsp_st2_valid_from_coreReq_T_1 @[DCache.scala 457:44]
    coreRsp_st2_valid_from_memRsp_REG <= MshrAccess.io_missRspOut_valid @[DCache.scala 459:43]
    dirtyReplace_st2_a_opcode <= dirtyReplace_st1_a_opcode @[DCache.scala 482:20]
    dirtyReplace_st2_a_param <= dirtyReplace_st1_a_param @[DCache.scala 482:20]
    dirtyReplace_st2_a_addr <= dirtyReplace_st1_a_addr @[DCache.scala 482:20]
    dirtyReplace_st2_a_data_0 <= dirtyReplace_st1_a_data_0 @[DCache.scala 482:20]
    dirtyReplace_st2_a_data_1 <= dirtyReplace_st1_a_data_1 @[DCache.scala 482:20]
    dirtyReplace_st2_a_data_2 <= dirtyReplace_st1_a_data_2 @[DCache.scala 482:20]
    dirtyReplace_st2_a_data_3 <= dirtyReplace_st1_a_data_3 @[DCache.scala 482:20]
    dirtyReplace_st2_a_mask_0 <= dirtyReplace_st1_a_mask_0 @[DCache.scala 482:20]
    dirtyReplace_st2_a_mask_1 <= dirtyReplace_st1_a_mask_1 @[DCache.scala 482:20]
    dirtyReplace_st2_a_mask_2 <= dirtyReplace_st1_a_mask_2 @[DCache.scala 482:20]
    dirtyReplace_st2_a_mask_3 <= dirtyReplace_st1_a_mask_3 @[DCache.scala 482:20]
    dirtyReplace_st2_a_source <= dirtyReplace_st1_a_source @[DCache.scala 482:20]
    dirtyReplace_st2_hasCoreRsp <= dirtyReplace_st1_hasCoreRsp @[DCache.scala 482:20]
    dirtyReplace_st2_coreRspInstrId <= dirtyReplace_st1_coreRspInstrId @[DCache.scala 482:20]
    memReq_st3_a_opcode <= _GEN_151
    memReq_st3_a_param <= _GEN_150
    memReq_st3_a_addr <= _GEN_149
    memReq_st3_a_data_0 <= _GEN_145
    memReq_st3_a_data_1 <= _GEN_146
    memReq_st3_a_data_2 <= _GEN_147
    memReq_st3_a_data_3 <= _GEN_148
    memReq_st3_a_mask_0 <= _GEN_141
    memReq_st3_a_mask_1 <= _GEN_142
    memReq_st3_a_mask_2 <= _GEN_143
    memReq_st3_a_mask_3 <= _GEN_144
    memReq_st3_a_source <= _GEN_152
    memReq_valid <= mux(reset, UInt<1>("h0"), _GEN_153) @[DCache.scala 531:{29,29}]
    printf(clock, and(and(and(UInt<1>("h1"), _T_72), _T_73), UInt<1>("h1")), "Assertion failed: cRsp from cReq and mRsp conflict\n    at DCache.scala:460 assert (!(coreRsp_st2_valid_from_coreReq && coreRsp_st2_valid_from_memRsp), s\"cRsp from cReq and mRsp conflict\")\n") : printf @[DCache.scala 460:10]
    assert(clock, _T_70, and(and(UInt<1>("h1"), _T_72), UInt<1>("h1")), "") : assert @[DCache.scala 460:10]
    printf(clock, and(and(and(UInt<1>("h1"), _T_77), _T_78), UInt<1>("h1")), "Assertion failed: cRsp from cReq and mReq conflict\n    at DCache.scala:461 assert (!(coreRsp_st2_valid_from_coreReq && coreRsp_st2_valid_from_memReq), \"cRsp from cReq and mReq conflict\")\n") : printf_1 @[DCache.scala 461:10]
    assert(clock, _T_75, and(and(UInt<1>("h1"), _T_77), UInt<1>("h1")), "") : assert_1 @[DCache.scala 461:10]
    printf(clock, and(and(and(UInt<1>("h1"), _T_82), _T_83), UInt<1>("h1")), "Assertion failed: cRsp from mRsp and mReq conflict\n    at DCache.scala:462 assert (!(coreRsp_st2_valid_from_memReq && coreRsp_st2_valid_from_memRsp), \"cRsp from mRsp and mReq conflict\")\n") : printf_2 @[DCache.scala 462:10]
    assert(clock, _T_80, and(and(UInt<1>("h1"), _T_82), UInt<1>("h1")), "") : assert_2 @[DCache.scala 462:10]

  module Queue_4 :
    input clock : Clock
    input reset : UInt<1>
    output io_enq_ready : UInt<1>
    input io_enq_valid : UInt<1>
    input io_enq_bits_d_opcode : UInt<3>
    input io_enq_bits_d_source : UInt<7>
    input io_enq_bits_d_addr : UInt<32>
    input io_enq_bits_d_data_0 : UInt<32>
    input io_enq_bits_d_data_1 : UInt<32>
    input io_enq_bits_d_data_2 : UInt<32>
    input io_enq_bits_d_data_3 : UInt<32>
    input io_deq_ready : UInt<1>
    output io_deq_valid : UInt<1>
    output io_deq_bits_d_opcode : UInt<3>
    output io_deq_bits_d_source : UInt<7>
    output io_deq_bits_d_addr : UInt<32>
    output io_deq_bits_d_data_0 : UInt<32>
    output io_deq_bits_d_data_1 : UInt<32>
    output io_deq_bits_d_data_2 : UInt<32>
    output io_deq_bits_d_data_3 : UInt<32>
    output io_count : UInt<3>

    mem ram_d_opcode : @[Decoupled.scala 259:95]
      data-type => UInt<3>
      depth => 6
      read-latency => 0
      write-latency => 1
      reader => io_deq_bits_MPORT
      writer => MPORT
      read-under-write => undefined
    mem ram_d_source : @[Decoupled.scala 259:95]
      data-type => UInt<7>
      depth => 6
      read-latency => 0
      write-latency => 1
      reader => io_deq_bits_MPORT
      writer => MPORT
      read-under-write => undefined
    mem ram_d_addr : @[Decoupled.scala 259:95]
      data-type => UInt<32>
      depth => 6
      read-latency => 0
      write-latency => 1
      reader => io_deq_bits_MPORT
      writer => MPORT
      read-under-write => undefined
    mem ram_d_data_0 : @[Decoupled.scala 259:95]
      data-type => UInt<32>
      depth => 6
      read-latency => 0
      write-latency => 1
      reader => io_deq_bits_MPORT
      writer => MPORT
      read-under-write => undefined
    mem ram_d_data_1 : @[Decoupled.scala 259:95]
      data-type => UInt<32>
      depth => 6
      read-latency => 0
      write-latency => 1
      reader => io_deq_bits_MPORT
      writer => MPORT
      read-under-write => undefined
    mem ram_d_data_2 : @[Decoupled.scala 259:95]
      data-type => UInt<32>
      depth => 6
      read-latency => 0
      write-latency => 1
      reader => io_deq_bits_MPORT
      writer => MPORT
      read-under-write => undefined
    mem ram_d_data_3 : @[Decoupled.scala 259:95]
      data-type => UInt<32>
      depth => 6
      read-latency => 0
      write-latency => 1
      reader => io_deq_bits_MPORT
      writer => MPORT
      read-under-write => undefined
    reg enq_ptr_value : UInt<3>, clock with :
      reset => (UInt<1>("h0"), enq_ptr_value) @[Counter.scala 62:40]
    reg deq_ptr_value : UInt<3>, clock with :
      reset => (UInt<1>("h0"), deq_ptr_value) @[Counter.scala 62:40]
    reg maybe_full : UInt<1>, clock with :
      reset => (UInt<1>("h0"), maybe_full) @[Decoupled.scala 262:27]
    node ptr_match = eq(enq_ptr_value, deq_ptr_value) @[Decoupled.scala 263:33]
    node _empty_T = eq(maybe_full, UInt<1>("h0")) @[Decoupled.scala 264:28]
    node empty = and(ptr_match, _empty_T) @[Decoupled.scala 264:25]
    node full = and(ptr_match, maybe_full) @[Decoupled.scala 265:24]
    node _do_enq_T = and(io_enq_ready, io_enq_valid) @[Decoupled.scala 50:35]
    node _do_deq_T = and(io_deq_ready, io_deq_valid) @[Decoupled.scala 50:35]
    node wrap = eq(enq_ptr_value, UInt<3>("h5")) @[Counter.scala 74:24]
    node _value_T = add(enq_ptr_value, UInt<1>("h1")) @[Counter.scala 78:24]
    node _value_T_1 = tail(_value_T, 1) @[Counter.scala 78:24]
    node _GEN_0 = mux(wrap, UInt<1>("h0"), _value_T_1) @[Counter.scala 78:15 88:{20,28}]
    node do_enq = _do_enq_T
    node _GEN_1 = validif(do_enq, enq_ptr_value) @[Decoupled.scala 272:16 273:8]
    node _GEN_2 = validif(do_enq, clock) @[Decoupled.scala 272:16 273:8]
    node _GEN_3 = mux(do_enq, UInt<1>("h1"), UInt<1>("h0")) @[Decoupled.scala 272:16 273:8 259:95]
    node _GEN_4 = validif(do_enq, UInt<1>("h1")) @[Decoupled.scala 272:16 273:24]
    node _GEN_5 = validif(do_enq, io_enq_bits_d_opcode) @[Decoupled.scala 272:16 273:24]
    node _GEN_6 = validif(do_enq, io_enq_bits_d_source) @[Decoupled.scala 272:16 273:24]
    node _GEN_7 = validif(do_enq, io_enq_bits_d_addr) @[Decoupled.scala 272:16 273:24]
    node _GEN_8 = validif(do_enq, io_enq_bits_d_data_0) @[Decoupled.scala 272:16 273:24]
    node _GEN_9 = validif(do_enq, io_enq_bits_d_data_1) @[Decoupled.scala 272:16 273:24]
    node _GEN_10 = validif(do_enq, io_enq_bits_d_data_2) @[Decoupled.scala 272:16 273:24]
    node _GEN_11 = validif(do_enq, io_enq_bits_d_data_3) @[Decoupled.scala 272:16 273:24]
    node _GEN_12 = mux(do_enq, _GEN_0, enq_ptr_value) @[Decoupled.scala 272:16 Counter.scala 62:40]
    node wrap_1 = eq(deq_ptr_value, UInt<3>("h5")) @[Counter.scala 74:24]
    node _value_T_2 = add(deq_ptr_value, UInt<1>("h1")) @[Counter.scala 78:24]
    node _value_T_3 = tail(_value_T_2, 1) @[Counter.scala 78:24]
    node _GEN_13 = mux(wrap_1, UInt<1>("h0"), _value_T_3) @[Counter.scala 78:15 88:{20,28}]
    node do_deq = _do_deq_T
    node _GEN_14 = mux(do_deq, _GEN_13, deq_ptr_value) @[Decoupled.scala 276:16 Counter.scala 62:40]
    node _T = neq(do_enq, do_deq) @[Decoupled.scala 279:15]
    node _GEN_15 = mux(_T, do_enq, maybe_full) @[Decoupled.scala 279:27 280:16 262:27]
    node _GEN_16 = mux(UInt<1>("h0"), UInt<1>("h0"), _GEN_12) @[Decoupled.scala 282:15 Counter.scala 99:11]
    node _GEN_17 = mux(UInt<1>("h0"), UInt<1>("h0"), _GEN_14) @[Decoupled.scala 282:15 Counter.scala 99:11]
    node _GEN_18 = mux(UInt<1>("h0"), UInt<1>("h0"), _GEN_15) @[Decoupled.scala 282:15 285:16]
    node _io_deq_valid_T = eq(empty, UInt<1>("h0")) @[Decoupled.scala 288:19]
    node _io_enq_ready_T = eq(full, UInt<1>("h0")) @[Decoupled.scala 289:19]
    node _ptr_diff_T = sub(enq_ptr_value, deq_ptr_value) @[Decoupled.scala 312:32]
    node ptr_diff = tail(_ptr_diff_T, 1) @[Decoupled.scala 312:32]
    node _io_count_T = mux(maybe_full, UInt<3>("h6"), UInt<1>("h0")) @[Decoupled.scala 319:10]
    node _io_count_T_1 = gt(deq_ptr_value, enq_ptr_value) @[Decoupled.scala 320:25]
    node _io_count_T_2 = add(UInt<3>("h6"), ptr_diff) @[Decoupled.scala 320:57]
    node _io_count_T_3 = tail(_io_count_T_2, 1) @[Decoupled.scala 320:57]
    node _io_count_T_4 = mux(_io_count_T_1, _io_count_T_3, ptr_diff) @[Decoupled.scala 320:10]
    node _io_count_T_5 = mux(ptr_match, _io_count_T, _io_count_T_4) @[Decoupled.scala 317:20]
    io_enq_ready <= _io_enq_ready_T @[Decoupled.scala 289:16]
    io_deq_valid <= _io_deq_valid_T @[Decoupled.scala 288:16]
    io_deq_bits_d_opcode <= ram_d_opcode.io_deq_bits_MPORT.data @[Decoupled.scala 296:17]
    io_deq_bits_d_source <= ram_d_source.io_deq_bits_MPORT.data @[Decoupled.scala 296:17]
    io_deq_bits_d_addr <= ram_d_addr.io_deq_bits_MPORT.data @[Decoupled.scala 296:17]
    io_deq_bits_d_data_0 <= ram_d_data_0.io_deq_bits_MPORT.data @[Decoupled.scala 296:17]
    io_deq_bits_d_data_1 <= ram_d_data_1.io_deq_bits_MPORT.data @[Decoupled.scala 296:17]
    io_deq_bits_d_data_2 <= ram_d_data_2.io_deq_bits_MPORT.data @[Decoupled.scala 296:17]
    io_deq_bits_d_data_3 <= ram_d_data_3.io_deq_bits_MPORT.data @[Decoupled.scala 296:17]
    io_count <= _io_count_T_5 @[Decoupled.scala 317:14]
    ram_d_opcode.io_deq_bits_MPORT.addr <= deq_ptr_value @[Decoupled.scala 296:23]
    ram_d_source.io_deq_bits_MPORT.addr <= deq_ptr_value @[Decoupled.scala 296:23]
    ram_d_addr.io_deq_bits_MPORT.addr <= deq_ptr_value @[Decoupled.scala 296:23]
    ram_d_data_0.io_deq_bits_MPORT.addr <= deq_ptr_value @[Decoupled.scala 296:23]
    ram_d_data_1.io_deq_bits_MPORT.addr <= deq_ptr_value @[Decoupled.scala 296:23]
    ram_d_data_2.io_deq_bits_MPORT.addr <= deq_ptr_value @[Decoupled.scala 296:23]
    ram_d_data_3.io_deq_bits_MPORT.addr <= deq_ptr_value @[Decoupled.scala 296:23]
    ram_d_opcode.io_deq_bits_MPORT.en <= UInt<1>("h1") @[Decoupled.scala 296:23]
    ram_d_source.io_deq_bits_MPORT.en <= UInt<1>("h1") @[Decoupled.scala 296:23]
    ram_d_addr.io_deq_bits_MPORT.en <= UInt<1>("h1") @[Decoupled.scala 296:23]
    ram_d_data_0.io_deq_bits_MPORT.en <= UInt<1>("h1") @[Decoupled.scala 296:23]
    ram_d_data_1.io_deq_bits_MPORT.en <= UInt<1>("h1") @[Decoupled.scala 296:23]
    ram_d_data_2.io_deq_bits_MPORT.en <= UInt<1>("h1") @[Decoupled.scala 296:23]
    ram_d_data_3.io_deq_bits_MPORT.en <= UInt<1>("h1") @[Decoupled.scala 296:23]
    ram_d_opcode.io_deq_bits_MPORT.clk <= clock @[Decoupled.scala 296:23]
    ram_d_source.io_deq_bits_MPORT.clk <= clock @[Decoupled.scala 296:23]
    ram_d_addr.io_deq_bits_MPORT.clk <= clock @[Decoupled.scala 296:23]
    ram_d_data_0.io_deq_bits_MPORT.clk <= clock @[Decoupled.scala 296:23]
    ram_d_data_1.io_deq_bits_MPORT.clk <= clock @[Decoupled.scala 296:23]
    ram_d_data_2.io_deq_bits_MPORT.clk <= clock @[Decoupled.scala 296:23]
    ram_d_data_3.io_deq_bits_MPORT.clk <= clock @[Decoupled.scala 296:23]
    ram_d_opcode.MPORT.addr <= _GEN_1
    ram_d_source.MPORT.addr <= _GEN_1
    ram_d_addr.MPORT.addr <= _GEN_1
    ram_d_data_0.MPORT.addr <= _GEN_1
    ram_d_data_1.MPORT.addr <= _GEN_1
    ram_d_data_2.MPORT.addr <= _GEN_1
    ram_d_data_3.MPORT.addr <= _GEN_1
    ram_d_opcode.MPORT.en <= _GEN_3
    ram_d_source.MPORT.en <= _GEN_3
    ram_d_addr.MPORT.en <= _GEN_3
    ram_d_data_0.MPORT.en <= _GEN_3
    ram_d_data_1.MPORT.en <= _GEN_3
    ram_d_data_2.MPORT.en <= _GEN_3
    ram_d_data_3.MPORT.en <= _GEN_3
    ram_d_opcode.MPORT.clk <= _GEN_2
    ram_d_source.MPORT.clk <= _GEN_2
    ram_d_addr.MPORT.clk <= _GEN_2
    ram_d_data_0.MPORT.clk <= _GEN_2
    ram_d_data_1.MPORT.clk <= _GEN_2
    ram_d_data_2.MPORT.clk <= _GEN_2
    ram_d_data_3.MPORT.clk <= _GEN_2
    ram_d_opcode.MPORT.data <= _GEN_5
    ram_d_source.MPORT.data <= _GEN_6
    ram_d_addr.MPORT.data <= _GEN_7
    ram_d_data_0.MPORT.data <= _GEN_8
    ram_d_data_1.MPORT.data <= _GEN_9
    ram_d_data_2.MPORT.data <= _GEN_10
    ram_d_data_3.MPORT.data <= _GEN_11
    ram_d_opcode.MPORT.mask <= _GEN_4
    ram_d_source.MPORT.mask <= _GEN_4
    ram_d_addr.MPORT.mask <= _GEN_4
    ram_d_data_0.MPORT.mask <= _GEN_4
    ram_d_data_1.MPORT.mask <= _GEN_4
    ram_d_data_2.MPORT.mask <= _GEN_4
    ram_d_data_3.MPORT.mask <= _GEN_4
    enq_ptr_value <= mux(reset, UInt<3>("h0"), _GEN_16) @[Counter.scala 62:{40,40}]
    deq_ptr_value <= mux(reset, UInt<3>("h0"), _GEN_17) @[Counter.scala 62:{40,40}]
    maybe_full <= mux(reset, UInt<1>("h0"), _GEN_18) @[Decoupled.scala 262:{27,27}]

  module L2ROM :
    input clock : Clock
    input reset : UInt<1>
    output io_memReq__ready : UInt<1>
    input io_memReq__valid : UInt<1>
    input io_memReq__bits_a_opcode : UInt<3>
    input io_memReq__bits_a_param : UInt<3>
    input io_memReq__bits_a_addr : UInt<32>
    input io_memReq__bits_a_data_0 : UInt<32>
    input io_memReq__bits_a_data_1 : UInt<32>
    input io_memReq__bits_a_data_2 : UInt<32>
    input io_memReq__bits_a_data_3 : UInt<32>
    input io_memReq__bits_a_mask_0 : UInt<1>
    input io_memReq__bits_a_mask_1 : UInt<1>
    input io_memReq__bits_a_mask_2 : UInt<1>
    input io_memReq__bits_a_mask_3 : UInt<1>
    input io_memReq__bits_a_source : UInt<7>
    input io_memRsp_ready : UInt<1>
    output io_memRsp_valid : UInt<1>
    output io_memRsp_bits_d_opcode : UInt<3>
    output io_memRsp_bits_d_source : UInt<7>
    output io_memRsp_bits_d_addr : UInt<32>
    output io_memRsp_bits_d_data_0 : UInt<32>
    output io_memRsp_bits_d_data_1 : UInt<32>
    output io_memRsp_bits_d_data_2 : UInt<32>
    output io_memRsp_bits_d_data_3 : UInt<32>
    input io_memReq_ready : UInt<1>
    output io_memReq_data_0 : UInt<32>
    output io_memReq_data_1 : UInt<32>
    output io_memReq_data_2 : UInt<32>
    output io_memReq_data_3 : UInt<32>
    output io_memReq_mask_0 : UInt<1>
    output io_memReq_mask_1 : UInt<1>
    output io_memReq_mask_2 : UInt<1>
    output io_memReq_mask_3 : UInt<1>

    mem memory : @[L1CacheTest.scala 43:19]
      data-type => UInt<32>
      depth => 256
      read-latency => 0
      write-latency => 1
      reader => raw_vec_0_MPORT
      reader => raw_vec_1_MPORT
      reader => raw_vec_2_MPORT
      reader => raw_vec_3_MPORT
      writer => MPORT
      writer => MPORT_1
      writer => MPORT_2
      writer => MPORT_3
      read-under-write => undefined
    inst mem_rsp_Q of Queue_4 @[L1CacheTest.scala 89:25]
    node _d_opcode_T = eq(io_memReq__bits_a_opcode, UInt<1>("h0")) @[L1CacheTest.scala 41:28]
    node _d_opcode_T_1 = eq(io_memReq__bits_a_opcode, UInt<1>("h1")) @[L1CacheTest.scala 41:50]
    node _d_opcode_T_2 = eq(io_memReq__bits_a_param, UInt<1>("h0")) @[L1CacheTest.scala 41:84]
    node _d_opcode_T_3 = and(_d_opcode_T_1, _d_opcode_T_2) @[L1CacheTest.scala 41:58]
    node d_opcode = or(_d_opcode_T, _d_opcode_T_3) @[L1CacheTest.scala 41:37]
    node _raw_vec_0_T = shr(io_memReq__bits_a_addr, 4) @[L1CacheParameters.scala 66:41]
    node _raw_vec_0_T_1 = cat(_raw_vec_0_T, UInt<2>("h0")) @[Cat.scala 31:58]
    node _raw_vec_0_T_2 = bits(_raw_vec_0_T_1, 7, 0) @[L1CacheTest.scala 47:30]
    node _raw_vec_1_T = shr(io_memReq__bits_a_addr, 4) @[L1CacheParameters.scala 66:41]
    node _raw_vec_1_T_1 = cat(_raw_vec_1_T, UInt<2>("h1")) @[Cat.scala 31:58]
    node _raw_vec_1_T_2 = bits(_raw_vec_1_T_1, 7, 0) @[L1CacheTest.scala 47:30]
    node _raw_vec_2_T = shr(io_memReq__bits_a_addr, 4) @[L1CacheParameters.scala 66:41]
    node _raw_vec_2_T_1 = cat(_raw_vec_2_T, UInt<2>("h2")) @[Cat.scala 31:58]
    node _raw_vec_2_T_2 = bits(_raw_vec_2_T_1, 7, 0) @[L1CacheTest.scala 47:30]
    node _raw_vec_3_T = shr(io_memReq__bits_a_addr, 4) @[L1CacheParameters.scala 66:41]
    node _raw_vec_3_T_1 = cat(_raw_vec_3_T, UInt<2>("h3")) @[Cat.scala 31:58]
    node _raw_vec_3_T_2 = bits(_raw_vec_3_T_1, 7, 0) @[L1CacheTest.scala 47:30]
    node raw_vec_0 = memory.raw_vec_0_MPORT.data @[L1CacheTest.scala 45:21 47:16]
    node _data_write_in_vec_0_T = mux(io_memReq__bits_a_mask_0, io_memReq__bits_a_data_0, raw_vec_0) @[L1CacheTest.scala 54:32]
    node _T = and(io_memReq__ready, io_memReq__valid) @[Decoupled.scala 50:35]
    node _T_1 = eq(io_memReq__bits_a_opcode, UInt<3>("h1")) @[L1CacheTest.scala 55:38]
    node _T_2 = eq(io_memReq__bits_a_opcode, UInt<3>("h0")) @[L1CacheTest.scala 55:68]
    node _T_3 = or(_T_1, _T_2) @[L1CacheTest.scala 55:56]
    node _T_4 = and(_T, _T_3) @[L1CacheTest.scala 55:25]
    node _T_5 = shr(io_memReq__bits_a_addr, 4) @[L1CacheParameters.scala 66:41]
    node _T_6 = cat(_T_5, UInt<2>("h0")) @[Cat.scala 31:58]
    node _T_7 = bits(_T_6, 7, 0)
    node _GEN_0 = validif(_T_4, _T_7) @[L1CacheTest.scala 55:88]
    node _GEN_1 = validif(_T_4, clock) @[L1CacheTest.scala 55:88]
    node _GEN_2 = mux(_T_4, UInt<1>("h1"), UInt<1>("h0")) @[L1CacheTest.scala 43:19 55:88]
    node _GEN_3 = validif(_T_4, UInt<1>("h1")) @[L1CacheTest.scala 55:88]
    node data_write_in_vec_0 = _data_write_in_vec_0_T @[L1CacheTest.scala 52:31 54:26]
    node _GEN_4 = validif(_T_4, data_write_in_vec_0) @[L1CacheTest.scala 55:88]
    node raw_vec_1 = memory.raw_vec_1_MPORT.data @[L1CacheTest.scala 45:21 47:16]
    node _data_write_in_vec_1_T = mux(io_memReq__bits_a_mask_1, io_memReq__bits_a_data_1, raw_vec_1) @[L1CacheTest.scala 54:32]
    node _T_8 = and(io_memReq__ready, io_memReq__valid) @[Decoupled.scala 50:35]
    node _T_9 = eq(io_memReq__bits_a_opcode, UInt<3>("h1")) @[L1CacheTest.scala 55:38]
    node _T_10 = eq(io_memReq__bits_a_opcode, UInt<3>("h0")) @[L1CacheTest.scala 55:68]
    node _T_11 = or(_T_9, _T_10) @[L1CacheTest.scala 55:56]
    node _T_12 = and(_T_8, _T_11) @[L1CacheTest.scala 55:25]
    node _T_13 = shr(io_memReq__bits_a_addr, 4) @[L1CacheParameters.scala 66:41]
    node _T_14 = cat(_T_13, UInt<2>("h1")) @[Cat.scala 31:58]
    node _T_15 = bits(_T_14, 7, 0)
    node _GEN_5 = validif(_T_12, _T_15) @[L1CacheTest.scala 55:88]
    node _GEN_6 = validif(_T_12, clock) @[L1CacheTest.scala 55:88]
    node _GEN_7 = mux(_T_12, UInt<1>("h1"), UInt<1>("h0")) @[L1CacheTest.scala 43:19 55:88]
    node _GEN_8 = validif(_T_12, UInt<1>("h1")) @[L1CacheTest.scala 55:88]
    node data_write_in_vec_1 = _data_write_in_vec_1_T @[L1CacheTest.scala 52:31 54:26]
    node _GEN_9 = validif(_T_12, data_write_in_vec_1) @[L1CacheTest.scala 55:88]
    node raw_vec_2 = memory.raw_vec_2_MPORT.data @[L1CacheTest.scala 45:21 47:16]
    node _data_write_in_vec_2_T = mux(io_memReq__bits_a_mask_2, io_memReq__bits_a_data_2, raw_vec_2) @[L1CacheTest.scala 54:32]
    node _T_16 = and(io_memReq__ready, io_memReq__valid) @[Decoupled.scala 50:35]
    node _T_17 = eq(io_memReq__bits_a_opcode, UInt<3>("h1")) @[L1CacheTest.scala 55:38]
    node _T_18 = eq(io_memReq__bits_a_opcode, UInt<3>("h0")) @[L1CacheTest.scala 55:68]
    node _T_19 = or(_T_17, _T_18) @[L1CacheTest.scala 55:56]
    node _T_20 = and(_T_16, _T_19) @[L1CacheTest.scala 55:25]
    node _T_21 = shr(io_memReq__bits_a_addr, 4) @[L1CacheParameters.scala 66:41]
    node _T_22 = cat(_T_21, UInt<2>("h2")) @[Cat.scala 31:58]
    node _T_23 = bits(_T_22, 7, 0)
    node _GEN_10 = validif(_T_20, _T_23) @[L1CacheTest.scala 55:88]
    node _GEN_11 = validif(_T_20, clock) @[L1CacheTest.scala 55:88]
    node _GEN_12 = mux(_T_20, UInt<1>("h1"), UInt<1>("h0")) @[L1CacheTest.scala 43:19 55:88]
    node _GEN_13 = validif(_T_20, UInt<1>("h1")) @[L1CacheTest.scala 55:88]
    node data_write_in_vec_2 = _data_write_in_vec_2_T @[L1CacheTest.scala 52:31 54:26]
    node _GEN_14 = validif(_T_20, data_write_in_vec_2) @[L1CacheTest.scala 55:88]
    node raw_vec_3 = memory.raw_vec_3_MPORT.data @[L1CacheTest.scala 45:21 47:16]
    node _data_write_in_vec_3_T = mux(io_memReq__bits_a_mask_3, io_memReq__bits_a_data_3, raw_vec_3) @[L1CacheTest.scala 54:32]
    node _T_24 = and(io_memReq__ready, io_memReq__valid) @[Decoupled.scala 50:35]
    node _T_25 = eq(io_memReq__bits_a_opcode, UInt<3>("h1")) @[L1CacheTest.scala 55:38]
    node _T_26 = eq(io_memReq__bits_a_opcode, UInt<3>("h0")) @[L1CacheTest.scala 55:68]
    node _T_27 = or(_T_25, _T_26) @[L1CacheTest.scala 55:56]
    node _T_28 = and(_T_24, _T_27) @[L1CacheTest.scala 55:25]
    node _T_29 = shr(io_memReq__bits_a_addr, 4) @[L1CacheParameters.scala 66:41]
    node _T_30 = cat(_T_29, UInt<2>("h3")) @[Cat.scala 31:58]
    node _T_31 = bits(_T_30, 7, 0)
    node _GEN_15 = validif(_T_28, _T_31) @[L1CacheTest.scala 55:88]
    node _GEN_16 = validif(_T_28, clock) @[L1CacheTest.scala 55:88]
    node _GEN_17 = mux(_T_28, UInt<1>("h1"), UInt<1>("h0")) @[L1CacheTest.scala 43:19 55:88]
    node _GEN_18 = validif(_T_28, UInt<1>("h1")) @[L1CacheTest.scala 55:88]
    node data_write_in_vec_3 = _data_write_in_vec_3_T @[L1CacheTest.scala 52:31 54:26]
    node _GEN_19 = validif(_T_28, data_write_in_vec_3) @[L1CacheTest.scala 55:88]
    node _opcode_out1_T = eq(d_opcode, UInt<1>("h0")) @[L1CacheTest.scala 64:31]
    node _opcode_out1_T_1 = and(io_memReq__ready, io_memReq__valid) @[Decoupled.scala 50:35]
    reg opcode_out1 : UInt<1>, clock with :
      reset => (UInt<1>("h0"), opcode_out1) @[Reg.scala 16:16]
    node _GEN_20 = mux(_opcode_out1_T_1, _opcode_out1_T, opcode_out1) @[Reg.scala 16:16 17:{18,22}]
    node _instrIdx_out1_T = and(io_memReq__ready, io_memReq__valid) @[Decoupled.scala 50:35]
    reg instrIdx_out1 : UInt<7>, clock with :
      reset => (UInt<1>("h0"), instrIdx_out1) @[Reg.scala 16:16]
    node _GEN_21 = mux(_instrIdx_out1_T, io_memReq__bits_a_source, instrIdx_out1) @[Reg.scala 16:16 17:{18,22}]
    node _data_out1_T = and(io_memReq__ready, io_memReq__valid) @[Decoupled.scala 50:35]
    reg data_out1_0 : UInt<32>, clock with :
      reset => (UInt<1>("h0"), data_out1_0) @[Reg.scala 16:16]
    reg data_out1_1 : UInt<32>, clock with :
      reset => (UInt<1>("h0"), data_out1_1) @[Reg.scala 16:16]
    reg data_out1_2 : UInt<32>, clock with :
      reset => (UInt<1>("h0"), data_out1_2) @[Reg.scala 16:16]
    reg data_out1_3 : UInt<32>, clock with :
      reset => (UInt<1>("h0"), data_out1_3) @[Reg.scala 16:16]
    node data_out_0 = raw_vec_0 @[L1CacheTest.scala 61:22 62:12]
    node _GEN_22 = mux(_data_out1_T, data_out_0, data_out1_0) @[Reg.scala 16:16 17:{18,22}]
    node data_out_1 = raw_vec_1 @[L1CacheTest.scala 61:22 62:12]
    node _GEN_23 = mux(_data_out1_T, data_out_1, data_out1_1) @[Reg.scala 16:16 17:{18,22}]
    node data_out_2 = raw_vec_2 @[L1CacheTest.scala 61:22 62:12]
    node _GEN_24 = mux(_data_out1_T, data_out_2, data_out1_2) @[Reg.scala 16:16 17:{18,22}]
    node data_out_3 = raw_vec_3 @[L1CacheTest.scala 61:22 62:12]
    node _GEN_25 = mux(_data_out1_T, data_out_3, data_out1_3) @[Reg.scala 16:16 17:{18,22}]
    node _addr_out1_T = shr(io_memReq__bits_a_addr, 4) @[L1CacheParameters.scala 66:41]
    node _addr_out1_T_1 = mux(UInt<1>("h0"), UInt<4>("hf"), UInt<4>("h0")) @[Bitwise.scala 74:12]
    node _addr_out1_T_2 = cat(_addr_out1_T, _addr_out1_T_1) @[Cat.scala 31:58]
    node _addr_out1_T_3 = and(io_memReq__ready, io_memReq__valid) @[Decoupled.scala 50:35]
    reg addr_out1 : UInt<32>, clock with :
      reset => (UInt<1>("h0"), addr_out1) @[Reg.scala 16:16]
    node _GEN_26 = mux(_addr_out1_T_3, _addr_out1_T_2, addr_out1) @[Reg.scala 16:16 17:{18,22}]
    node _fire_out1_T = and(io_memReq__ready, io_memReq__valid) @[Decoupled.scala 50:35]
    reg fire_out1 : UInt<1>, clock with :
      reset => (UInt<1>("h0"), fire_out1) @[L1CacheTest.scala 69:26]
    reg opcode_out2 : UInt<1>, clock with :
      reset => (UInt<1>("h0"), opcode_out2) @[L1CacheTest.scala 71:28]
    reg instrIdx_out2 : UInt<7>, clock with :
      reset => (UInt<1>("h0"), instrIdx_out2) @[L1CacheTest.scala 72:30]
    reg data_out2_0 : UInt<32>, clock with :
      reset => (UInt<1>("h0"), data_out2_0) @[L1CacheTest.scala 73:26]
    reg data_out2_1 : UInt<32>, clock with :
      reset => (UInt<1>("h0"), data_out2_1) @[L1CacheTest.scala 73:26]
    reg data_out2_2 : UInt<32>, clock with :
      reset => (UInt<1>("h0"), data_out2_2) @[L1CacheTest.scala 73:26]
    reg data_out2_3 : UInt<32>, clock with :
      reset => (UInt<1>("h0"), data_out2_3) @[L1CacheTest.scala 73:26]
    reg addr_out2 : UInt<32>, clock with :
      reset => (UInt<1>("h0"), addr_out2) @[L1CacheTest.scala 74:26]
    reg fire_out2 : UInt<1>, clock with :
      reset => (UInt<1>("h0"), fire_out2) @[L1CacheTest.scala 75:26]
    reg opcode_out3 : UInt<1>, clock with :
      reset => (UInt<1>("h0"), opcode_out3) @[L1CacheTest.scala 77:28]
    reg instrIdx_out3 : UInt<7>, clock with :
      reset => (UInt<1>("h0"), instrIdx_out3) @[L1CacheTest.scala 78:30]
    reg data_out3_0 : UInt<32>, clock with :
      reset => (UInt<1>("h0"), data_out3_0) @[L1CacheTest.scala 79:26]
    reg data_out3_1 : UInt<32>, clock with :
      reset => (UInt<1>("h0"), data_out3_1) @[L1CacheTest.scala 79:26]
    reg data_out3_2 : UInt<32>, clock with :
      reset => (UInt<1>("h0"), data_out3_2) @[L1CacheTest.scala 79:26]
    reg data_out3_3 : UInt<32>, clock with :
      reset => (UInt<1>("h0"), data_out3_3) @[L1CacheTest.scala 79:26]
    reg addr_out3 : UInt<32>, clock with :
      reset => (UInt<1>("h0"), addr_out3) @[L1CacheTest.scala 80:26]
    reg fire_out3 : UInt<1>, clock with :
      reset => (UInt<1>("h0"), fire_out3) @[L1CacheTest.scala 81:26]
    reg opcode_out4 : UInt<1>, clock with :
      reset => (UInt<1>("h0"), opcode_out4) @[L1CacheTest.scala 83:28]
    reg instrIdx_out4 : UInt<7>, clock with :
      reset => (UInt<1>("h0"), instrIdx_out4) @[L1CacheTest.scala 84:30]
    reg data_out4_0 : UInt<32>, clock with :
      reset => (UInt<1>("h0"), data_out4_0) @[L1CacheTest.scala 85:26]
    reg data_out4_1 : UInt<32>, clock with :
      reset => (UInt<1>("h0"), data_out4_1) @[L1CacheTest.scala 85:26]
    reg data_out4_2 : UInt<32>, clock with :
      reset => (UInt<1>("h0"), data_out4_2) @[L1CacheTest.scala 85:26]
    reg data_out4_3 : UInt<32>, clock with :
      reset => (UInt<1>("h0"), data_out4_3) @[L1CacheTest.scala 85:26]
    reg addr_out4 : UInt<32>, clock with :
      reset => (UInt<1>("h0"), addr_out4) @[L1CacheTest.scala 86:26]
    reg fire_out4 : UInt<1>, clock with :
      reset => (UInt<1>("h0"), fire_out4) @[L1CacheTest.scala 87:26]
    io_memReq__ready <= io_memReq_ready @[L1CacheTest.scala 49:19]
    io_memRsp_valid <= mem_rsp_Q.io_deq_valid @[L1CacheTest.scala 97:13]
    io_memRsp_bits_d_opcode <= mem_rsp_Q.io_deq_bits_d_opcode @[L1CacheTest.scala 97:13]
    io_memRsp_bits_d_source <= mem_rsp_Q.io_deq_bits_d_source @[L1CacheTest.scala 97:13]
    io_memRsp_bits_d_addr <= mem_rsp_Q.io_deq_bits_d_addr @[L1CacheTest.scala 97:13]
    io_memRsp_bits_d_data_0 <= mem_rsp_Q.io_deq_bits_d_data_0 @[L1CacheTest.scala 97:13]
    io_memRsp_bits_d_data_1 <= mem_rsp_Q.io_deq_bits_d_data_1 @[L1CacheTest.scala 97:13]
    io_memRsp_bits_d_data_2 <= mem_rsp_Q.io_deq_bits_d_data_2 @[L1CacheTest.scala 97:13]
    io_memRsp_bits_d_data_3 <= mem_rsp_Q.io_deq_bits_d_data_3 @[L1CacheTest.scala 97:13]
    io_memReq_data_0 <= io_memReq__bits_a_data_0 @[L1CacheTest.scala 99:18]
    io_memReq_data_1 <= io_memReq__bits_a_data_1 @[L1CacheTest.scala 99:18]
    io_memReq_data_2 <= io_memReq__bits_a_data_2 @[L1CacheTest.scala 99:18]
    io_memReq_data_3 <= io_memReq__bits_a_data_3 @[L1CacheTest.scala 99:18]
    io_memReq_mask_0 <= io_memReq__bits_a_mask_0 @[L1CacheTest.scala 100:18]
    io_memReq_mask_1 <= io_memReq__bits_a_mask_1 @[L1CacheTest.scala 100:18]
    io_memReq_mask_2 <= io_memReq__bits_a_mask_2 @[L1CacheTest.scala 100:18]
    io_memReq_mask_3 <= io_memReq__bits_a_mask_3 @[L1CacheTest.scala 100:18]
    memory.raw_vec_0_MPORT.addr <= _raw_vec_0_T_2 @[L1CacheTest.scala 47:30]
    memory.raw_vec_0_MPORT.en <= UInt<1>("h1") @[L1CacheTest.scala 47:30]
    memory.raw_vec_0_MPORT.clk <= clock @[L1CacheTest.scala 47:30]
    memory.raw_vec_1_MPORT.addr <= _raw_vec_1_T_2 @[L1CacheTest.scala 47:30]
    memory.raw_vec_1_MPORT.en <= UInt<1>("h1") @[L1CacheTest.scala 47:30]
    memory.raw_vec_1_MPORT.clk <= clock @[L1CacheTest.scala 47:30]
    memory.raw_vec_2_MPORT.addr <= _raw_vec_2_T_2 @[L1CacheTest.scala 47:30]
    memory.raw_vec_2_MPORT.en <= UInt<1>("h1") @[L1CacheTest.scala 47:30]
    memory.raw_vec_2_MPORT.clk <= clock @[L1CacheTest.scala 47:30]
    memory.raw_vec_3_MPORT.addr <= _raw_vec_3_T_2 @[L1CacheTest.scala 47:30]
    memory.raw_vec_3_MPORT.en <= UInt<1>("h1") @[L1CacheTest.scala 47:30]
    memory.raw_vec_3_MPORT.clk <= clock @[L1CacheTest.scala 47:30]
    memory.MPORT.addr <= _GEN_0
    memory.MPORT.en <= _GEN_2
    memory.MPORT.clk <= _GEN_1
    memory.MPORT.data <= _GEN_4
    memory.MPORT.mask <= _GEN_3
    memory.MPORT_1.addr <= _GEN_5
    memory.MPORT_1.en <= _GEN_7
    memory.MPORT_1.clk <= _GEN_6
    memory.MPORT_1.data <= _GEN_9
    memory.MPORT_1.mask <= _GEN_8
    memory.MPORT_2.addr <= _GEN_10
    memory.MPORT_2.en <= _GEN_12
    memory.MPORT_2.clk <= _GEN_11
    memory.MPORT_2.data <= _GEN_14
    memory.MPORT_2.mask <= _GEN_13
    memory.MPORT_3.addr <= _GEN_15
    memory.MPORT_3.en <= _GEN_17
    memory.MPORT_3.clk <= _GEN_16
    memory.MPORT_3.data <= _GEN_19
    memory.MPORT_3.mask <= _GEN_18
    opcode_out1 <= _GEN_20
    instrIdx_out1 <= _GEN_21
    data_out1_0 <= _GEN_22
    data_out1_1 <= _GEN_23
    data_out1_2 <= _GEN_24
    data_out1_3 <= _GEN_25
    addr_out1 <= _GEN_26
    fire_out1 <= _fire_out1_T @[L1CacheTest.scala 69:26]
    opcode_out2 <= opcode_out1 @[L1CacheTest.scala 71:28]
    instrIdx_out2 <= instrIdx_out1 @[L1CacheTest.scala 72:30]
    data_out2_0 <= data_out1_0 @[L1CacheTest.scala 73:26]
    data_out2_1 <= data_out1_1 @[L1CacheTest.scala 73:26]
    data_out2_2 <= data_out1_2 @[L1CacheTest.scala 73:26]
    data_out2_3 <= data_out1_3 @[L1CacheTest.scala 73:26]
    addr_out2 <= addr_out1 @[L1CacheTest.scala 74:26]
    fire_out2 <= fire_out1 @[L1CacheTest.scala 75:26]
    opcode_out3 <= opcode_out2 @[L1CacheTest.scala 77:28]
    instrIdx_out3 <= instrIdx_out2 @[L1CacheTest.scala 78:30]
    data_out3_0 <= data_out2_0 @[L1CacheTest.scala 79:26]
    data_out3_1 <= data_out2_1 @[L1CacheTest.scala 79:26]
    data_out3_2 <= data_out2_2 @[L1CacheTest.scala 79:26]
    data_out3_3 <= data_out2_3 @[L1CacheTest.scala 79:26]
    addr_out3 <= addr_out2 @[L1CacheTest.scala 80:26]
    fire_out3 <= fire_out2 @[L1CacheTest.scala 81:26]
    opcode_out4 <= opcode_out3 @[L1CacheTest.scala 83:28]
    instrIdx_out4 <= instrIdx_out3 @[L1CacheTest.scala 84:30]
    data_out4_0 <= data_out3_0 @[L1CacheTest.scala 85:26]
    data_out4_1 <= data_out3_1 @[L1CacheTest.scala 85:26]
    data_out4_2 <= data_out3_2 @[L1CacheTest.scala 85:26]
    data_out4_3 <= data_out3_3 @[L1CacheTest.scala 85:26]
    addr_out4 <= addr_out3 @[L1CacheTest.scala 86:26]
    fire_out4 <= fire_out3 @[L1CacheTest.scala 87:26]
    mem_rsp_Q.clock <= clock
    mem_rsp_Q.reset <= reset
    mem_rsp_Q.io_enq_valid <= fire_out4 @[L1CacheTest.scala 90:26]
    mem_rsp_Q.io_enq_bits_d_opcode <= pad(opcode_out4, 3) @[L1CacheTest.scala 91:34]
    mem_rsp_Q.io_enq_bits_d_source <= instrIdx_out4 @[L1CacheTest.scala 92:34]
    mem_rsp_Q.io_enq_bits_d_addr <= addr_out4 @[L1CacheTest.scala 93:32]
    mem_rsp_Q.io_enq_bits_d_data_0 <= data_out4_0 @[L1CacheTest.scala 94:32]
    mem_rsp_Q.io_enq_bits_d_data_1 <= data_out4_1 @[L1CacheTest.scala 94:32]
    mem_rsp_Q.io_enq_bits_d_data_2 <= data_out4_2 @[L1CacheTest.scala 94:32]
    mem_rsp_Q.io_enq_bits_d_data_3 <= data_out4_3 @[L1CacheTest.scala 94:32]
    mem_rsp_Q.io_deq_ready <= io_memRsp_ready @[L1CacheTest.scala 97:13]

  module DCacheWraper :
    input clock : Clock
    input reset : UInt<1>
    output io_coreReq_ready : UInt<1>
    input io_coreReq_valid : UInt<1>
    input io_coreReq_bits_instrId : UInt<3>
    input io_coreReq_bits_opcode : UInt<3>
    input io_coreReq_bits_param : UInt<4>
    input io_coreReq_bits_tag : UInt<26>
    input io_coreReq_bits_setIdx : UInt<2>
    input io_coreReq_bits_perLaneAddr_0_activeMask : UInt<1>
    input io_coreReq_bits_perLaneAddr_0_blockOffset : UInt<2>
    input io_coreReq_bits_perLaneAddr_0_wordOffset1H : UInt<4>
    input io_coreReq_bits_perLaneAddr_1_activeMask : UInt<1>
    input io_coreReq_bits_perLaneAddr_1_blockOffset : UInt<2>
    input io_coreReq_bits_perLaneAddr_1_wordOffset1H : UInt<4>
    input io_coreReq_bits_perLaneAddr_2_activeMask : UInt<1>
    input io_coreReq_bits_perLaneAddr_2_blockOffset : UInt<2>
    input io_coreReq_bits_perLaneAddr_2_wordOffset1H : UInt<4>
    input io_coreReq_bits_perLaneAddr_3_activeMask : UInt<1>
    input io_coreReq_bits_perLaneAddr_3_blockOffset : UInt<2>
    input io_coreReq_bits_perLaneAddr_3_wordOffset1H : UInt<4>
    input io_coreReq_bits_data_0 : UInt<32>
    input io_coreReq_bits_data_1 : UInt<32>
    input io_coreReq_bits_data_2 : UInt<32>
    input io_coreReq_bits_data_3 : UInt<32>
    input io_coreRsp_ready : UInt<1>
    output io_coreRsp_valid : UInt<1>
    output io_coreRsp_bits_instrId : UInt<3>
    output io_coreRsp_bits_isWrite : UInt<1>
    output io_coreRsp_bits_data_0 : UInt<32>
    output io_coreRsp_bits_data_1 : UInt<32>
    output io_coreRsp_bits_data_2 : UInt<32>
    output io_coreRsp_bits_data_3 : UInt<32>
    output io_coreRsp_bits_activeMask_0 : UInt<1>
    output io_coreRsp_bits_activeMask_1 : UInt<1>
    output io_coreRsp_bits_activeMask_2 : UInt<1>
    output io_coreRsp_bits_activeMask_3 : UInt<1>
    input io_memReq_ready : UInt<1>
    output io_dummy : UInt<1>

    inst DCache of DataCache @[L1CacheTest.scala 19:22]
    inst L2 of L2ROM @[L1CacheTest.scala 22:18]
    node io_dummy_lo = cat(L2.io_memReq_data_2, L2.io_memReq_data_3) @[Cat.scala 31:58]
    node io_dummy_hi = cat(L2.io_memReq_data_0, L2.io_memReq_data_1) @[Cat.scala 31:58]
    node _io_dummy_T = cat(io_dummy_hi, io_dummy_lo) @[Cat.scala 31:58]
    node _io_dummy_T_1 = orr(_io_dummy_T) @[L1CacheTest.scala 27:38]
    node io_dummy_lo_1 = cat(L2.io_memReq_mask_2, L2.io_memReq_mask_3) @[Cat.scala 31:58]
    node io_dummy_hi_1 = cat(L2.io_memReq_mask_0, L2.io_memReq_mask_1) @[Cat.scala 31:58]
    node _io_dummy_T_2 = cat(io_dummy_hi_1, io_dummy_lo_1) @[Cat.scala 31:58]
    node _io_dummy_T_3 = orr(_io_dummy_T_2) @[L1CacheTest.scala 27:67]
    node _io_dummy_T_4 = xor(_io_dummy_T_1, _io_dummy_T_3) @[L1CacheTest.scala 27:42]
    io_coreReq_ready <= DCache.io_coreReq_ready @[L1CacheTest.scala 20:14]
    io_coreRsp_valid <= DCache.io_coreRsp_valid @[L1CacheTest.scala 21:14]
    io_coreRsp_bits_instrId <= DCache.io_coreRsp_bits_instrId @[L1CacheTest.scala 21:14]
    io_coreRsp_bits_isWrite <= DCache.io_coreRsp_bits_isWrite @[L1CacheTest.scala 21:14]
    io_coreRsp_bits_data_0 <= DCache.io_coreRsp_bits_data_0 @[L1CacheTest.scala 21:14]
    io_coreRsp_bits_data_1 <= DCache.io_coreRsp_bits_data_1 @[L1CacheTest.scala 21:14]
    io_coreRsp_bits_data_2 <= DCache.io_coreRsp_bits_data_2 @[L1CacheTest.scala 21:14]
    io_coreRsp_bits_data_3 <= DCache.io_coreRsp_bits_data_3 @[L1CacheTest.scala 21:14]
    io_coreRsp_bits_activeMask_0 <= DCache.io_coreRsp_bits_activeMask_0 @[L1CacheTest.scala 21:14]
    io_coreRsp_bits_activeMask_1 <= DCache.io_coreRsp_bits_activeMask_1 @[L1CacheTest.scala 21:14]
    io_coreRsp_bits_activeMask_2 <= DCache.io_coreRsp_bits_activeMask_2 @[L1CacheTest.scala 21:14]
    io_coreRsp_bits_activeMask_3 <= DCache.io_coreRsp_bits_activeMask_3 @[L1CacheTest.scala 21:14]
    io_dummy <= _io_dummy_T_4 @[L1CacheTest.scala 27:12]
    DCache.clock <= clock
    DCache.reset <= reset
    DCache.io_coreReq_valid <= io_coreReq_valid @[L1CacheTest.scala 20:14]
    DCache.io_coreReq_bits_instrId <= io_coreReq_bits_instrId @[L1CacheTest.scala 20:14]
    DCache.io_coreReq_bits_opcode <= io_coreReq_bits_opcode @[L1CacheTest.scala 20:14]
    DCache.io_coreReq_bits_param <= io_coreReq_bits_param @[L1CacheTest.scala 20:14]
    DCache.io_coreReq_bits_tag <= io_coreReq_bits_tag @[L1CacheTest.scala 20:14]
    DCache.io_coreReq_bits_setIdx <= io_coreReq_bits_setIdx @[L1CacheTest.scala 20:14]
    DCache.io_coreReq_bits_perLaneAddr_0_activeMask <= io_coreReq_bits_perLaneAddr_0_activeMask @[L1CacheTest.scala 20:14]
    DCache.io_coreReq_bits_perLaneAddr_0_blockOffset <= io_coreReq_bits_perLaneAddr_0_blockOffset @[L1CacheTest.scala 20:14]
    DCache.io_coreReq_bits_perLaneAddr_0_wordOffset1H <= io_coreReq_bits_perLaneAddr_0_wordOffset1H @[L1CacheTest.scala 20:14]
    DCache.io_coreReq_bits_perLaneAddr_1_activeMask <= io_coreReq_bits_perLaneAddr_1_activeMask @[L1CacheTest.scala 20:14]
    DCache.io_coreReq_bits_perLaneAddr_1_blockOffset <= io_coreReq_bits_perLaneAddr_1_blockOffset @[L1CacheTest.scala 20:14]
    DCache.io_coreReq_bits_perLaneAddr_1_wordOffset1H <= io_coreReq_bits_perLaneAddr_1_wordOffset1H @[L1CacheTest.scala 20:14]
    DCache.io_coreReq_bits_perLaneAddr_2_activeMask <= io_coreReq_bits_perLaneAddr_2_activeMask @[L1CacheTest.scala 20:14]
    DCache.io_coreReq_bits_perLaneAddr_2_blockOffset <= io_coreReq_bits_perLaneAddr_2_blockOffset @[L1CacheTest.scala 20:14]
    DCache.io_coreReq_bits_perLaneAddr_2_wordOffset1H <= io_coreReq_bits_perLaneAddr_2_wordOffset1H @[L1CacheTest.scala 20:14]
    DCache.io_coreReq_bits_perLaneAddr_3_activeMask <= io_coreReq_bits_perLaneAddr_3_activeMask @[L1CacheTest.scala 20:14]
    DCache.io_coreReq_bits_perLaneAddr_3_blockOffset <= io_coreReq_bits_perLaneAddr_3_blockOffset @[L1CacheTest.scala 20:14]
    DCache.io_coreReq_bits_perLaneAddr_3_wordOffset1H <= io_coreReq_bits_perLaneAddr_3_wordOffset1H @[L1CacheTest.scala 20:14]
    DCache.io_coreReq_bits_data_0 <= io_coreReq_bits_data_0 @[L1CacheTest.scala 20:14]
    DCache.io_coreReq_bits_data_1 <= io_coreReq_bits_data_1 @[L1CacheTest.scala 20:14]
    DCache.io_coreReq_bits_data_2 <= io_coreReq_bits_data_2 @[L1CacheTest.scala 20:14]
    DCache.io_coreReq_bits_data_3 <= io_coreReq_bits_data_3 @[L1CacheTest.scala 20:14]
    DCache.io_coreRsp_ready <= io_coreRsp_ready @[L1CacheTest.scala 21:14]
    DCache.io_memRsp_valid <= L2.io_memRsp_valid @[L1CacheTest.scala 23:16]
    DCache.io_memRsp_bits_d_opcode <= L2.io_memRsp_bits_d_opcode @[L1CacheTest.scala 23:16]
    DCache.io_memRsp_bits_d_source <= L2.io_memRsp_bits_d_source @[L1CacheTest.scala 23:16]
    DCache.io_memRsp_bits_d_addr <= L2.io_memRsp_bits_d_addr @[L1CacheTest.scala 23:16]
    DCache.io_memRsp_bits_d_data_0 <= L2.io_memRsp_bits_d_data_0 @[L1CacheTest.scala 23:16]
    DCache.io_memRsp_bits_d_data_1 <= L2.io_memRsp_bits_d_data_1 @[L1CacheTest.scala 23:16]
    DCache.io_memRsp_bits_d_data_2 <= L2.io_memRsp_bits_d_data_2 @[L1CacheTest.scala 23:16]
    DCache.io_memRsp_bits_d_data_3 <= L2.io_memRsp_bits_d_data_3 @[L1CacheTest.scala 23:16]
    DCache.io_memReq_ready <= L2.io_memReq__ready @[L1CacheTest.scala 24:16]
    L2.clock <= clock
    L2.reset <= reset
    L2.io_memReq__valid <= DCache.io_memReq_valid @[L1CacheTest.scala 24:16]
    L2.io_memReq__bits_a_opcode <= DCache.io_memReq_bits_a_opcode @[L1CacheTest.scala 24:16]
    L2.io_memReq__bits_a_param <= DCache.io_memReq_bits_a_param @[L1CacheTest.scala 24:16]
    L2.io_memReq__bits_a_addr <= DCache.io_memReq_bits_a_addr @[L1CacheTest.scala 24:16]
    L2.io_memReq__bits_a_data_0 <= DCache.io_memReq_bits_a_data_0 @[L1CacheTest.scala 24:16]
    L2.io_memReq__bits_a_data_1 <= DCache.io_memReq_bits_a_data_1 @[L1CacheTest.scala 24:16]
    L2.io_memReq__bits_a_data_2 <= DCache.io_memReq_bits_a_data_2 @[L1CacheTest.scala 24:16]
    L2.io_memReq__bits_a_data_3 <= DCache.io_memReq_bits_a_data_3 @[L1CacheTest.scala 24:16]
    L2.io_memReq__bits_a_mask_0 <= DCache.io_memReq_bits_a_mask_0 @[L1CacheTest.scala 24:16]
    L2.io_memReq__bits_a_mask_1 <= DCache.io_memReq_bits_a_mask_1 @[L1CacheTest.scala 24:16]
    L2.io_memReq__bits_a_mask_2 <= DCache.io_memReq_bits_a_mask_2 @[L1CacheTest.scala 24:16]
    L2.io_memReq__bits_a_mask_3 <= DCache.io_memReq_bits_a_mask_3 @[L1CacheTest.scala 24:16]
    L2.io_memReq__bits_a_source <= DCache.io_memReq_bits_a_source @[L1CacheTest.scala 24:16]
    L2.io_memRsp_ready <= DCache.io_memRsp_ready @[L1CacheTest.scala 23:16]
    L2.io_memReq_ready <= io_memReq_ready @[L1CacheTest.scala 25:22]
