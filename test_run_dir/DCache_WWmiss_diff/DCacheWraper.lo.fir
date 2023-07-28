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

    inst subentryStatus of getEntryStatusReq @[L1MSHR.scala 111:30]
    inst subentryStatusForRsp of getEntryStatusRsp @[L1MSHR.scala 115:36]
    inst entryStatus of getEntryStatusReq_1 @[L1MSHR.scala 118:27]
    reg blockAddr_Access_0 : UInt<28>, clock with :
      reset => (UInt<1>("h0"), blockAddr_Access_0) @[L1MSHR.scala 80:33]
    reg blockAddr_Access_1 : UInt<28>, clock with :
      reset => (UInt<1>("h0"), blockAddr_Access_1) @[L1MSHR.scala 80:33]
    reg blockAddr_Access_2 : UInt<28>, clock with :
      reset => (UInt<1>("h0"), blockAddr_Access_2) @[L1MSHR.scala 80:33]
    reg blockAddr_Access_3 : UInt<28>, clock with :
      reset => (UInt<1>("h0"), blockAddr_Access_3) @[L1MSHR.scala 80:33]
    reg instrId_Access_0 : UInt<3>, clock with :
      reset => (UInt<1>("h0"), instrId_Access_0) @[L1MSHR.scala 81:31]
    reg instrId_Access_1 : UInt<3>, clock with :
      reset => (UInt<1>("h0"), instrId_Access_1) @[L1MSHR.scala 81:31]
    reg instrId_Access_2 : UInt<3>, clock with :
      reset => (UInt<1>("h0"), instrId_Access_2) @[L1MSHR.scala 81:31]
    reg instrId_Access_3 : UInt<3>, clock with :
      reset => (UInt<1>("h0"), instrId_Access_3) @[L1MSHR.scala 81:31]
    reg targetInfo_Accesss_0_0 : UInt<31>, clock with :
      reset => (UInt<1>("h0"), targetInfo_Accesss_0_0) @[L1MSHR.scala 82:35]
    reg targetInfo_Accesss_0_1 : UInt<31>, clock with :
      reset => (UInt<1>("h0"), targetInfo_Accesss_0_1) @[L1MSHR.scala 82:35]
    reg targetInfo_Accesss_1_0 : UInt<31>, clock with :
      reset => (UInt<1>("h0"), targetInfo_Accesss_1_0) @[L1MSHR.scala 82:35]
    reg targetInfo_Accesss_1_1 : UInt<31>, clock with :
      reset => (UInt<1>("h0"), targetInfo_Accesss_1_1) @[L1MSHR.scala 82:35]
    reg targetInfo_Accesss_2_0 : UInt<31>, clock with :
      reset => (UInt<1>("h0"), targetInfo_Accesss_2_0) @[L1MSHR.scala 82:35]
    reg targetInfo_Accesss_2_1 : UInt<31>, clock with :
      reset => (UInt<1>("h0"), targetInfo_Accesss_2_1) @[L1MSHR.scala 82:35]
    reg targetInfo_Accesss_3_0 : UInt<31>, clock with :
      reset => (UInt<1>("h0"), targetInfo_Accesss_3_0) @[L1MSHR.scala 82:35]
    reg targetInfo_Accesss_3_1 : UInt<31>, clock with :
      reset => (UInt<1>("h0"), targetInfo_Accesss_3_1) @[L1MSHR.scala 82:35]
    reg subentry_valid_0_0 : UInt<1>, clock with :
      reset => (UInt<1>("h0"), subentry_valid_0_0) @[L1MSHR.scala 84:31]
    reg subentry_valid_0_1 : UInt<1>, clock with :
      reset => (UInt<1>("h0"), subentry_valid_0_1) @[L1MSHR.scala 84:31]
    reg subentry_valid_1_0 : UInt<1>, clock with :
      reset => (UInt<1>("h0"), subentry_valid_1_0) @[L1MSHR.scala 84:31]
    reg subentry_valid_1_1 : UInt<1>, clock with :
      reset => (UInt<1>("h0"), subentry_valid_1_1) @[L1MSHR.scala 84:31]
    reg subentry_valid_2_0 : UInt<1>, clock with :
      reset => (UInt<1>("h0"), subentry_valid_2_0) @[L1MSHR.scala 84:31]
    reg subentry_valid_2_1 : UInt<1>, clock with :
      reset => (UInt<1>("h0"), subentry_valid_2_1) @[L1MSHR.scala 84:31]
    reg subentry_valid_3_0 : UInt<1>, clock with :
      reset => (UInt<1>("h0"), subentry_valid_3_0) @[L1MSHR.scala 84:31]
    reg subentry_valid_3_1 : UInt<1>, clock with :
      reset => (UInt<1>("h0"), subentry_valid_3_1) @[L1MSHR.scala 84:31]
    node _entry_valid_T = cat(subentry_valid_0_0, subentry_valid_0_1) @[Cat.scala 31:58]
    node _entry_valid_T_1 = orr(_entry_valid_T) @[L1MSHR.scala 85:59]
    node _entry_valid_T_2 = cat(subentry_valid_1_0, subentry_valid_1_1) @[Cat.scala 31:58]
    node _entry_valid_T_3 = orr(_entry_valid_T_2) @[L1MSHR.scala 85:59]
    node _entry_valid_T_4 = cat(subentry_valid_2_0, subentry_valid_2_1) @[Cat.scala 31:58]
    node _entry_valid_T_5 = orr(_entry_valid_T_4) @[L1MSHR.scala 85:59]
    node _entry_valid_T_6 = cat(subentry_valid_3_0, subentry_valid_3_1) @[Cat.scala 31:58]
    node _entry_valid_T_7 = orr(_entry_valid_T_6) @[L1MSHR.scala 85:59]
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
    node _entryMatchProbe_T = eq(blockAddr_Access_0, io_probe_bits_blockAddr) @[L1MSHR.scala 134:57]
    node _entryMatchProbe_T_1 = eq(blockAddr_Access_1, io_probe_bits_blockAddr) @[L1MSHR.scala 134:57]
    node entryMatchProbe_hi = cat(_entryMatchProbe_T, _entryMatchProbe_T_1) @[Cat.scala 31:58]
    node _entryMatchProbe_T_2 = eq(blockAddr_Access_2, io_probe_bits_blockAddr) @[L1MSHR.scala 134:57]
    node _entryMatchProbe_T_3 = eq(blockAddr_Access_3, io_probe_bits_blockAddr) @[L1MSHR.scala 134:57]
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
    node _entryMatchProbe_T_14 = and(_entryMatchProbe_T_13, entry_valid) @[L1MSHR.scala 134:88]
    node entryMatchProbe = _entryMatchProbe_T_14 @[L1MSHR.scala 109:29 134:19]
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
      reset => (UInt<1>("h0"), mshrStatus_st1_r) @[L1MSHR.scala 122:33]
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
    node _T_10 = leq(_T_9, UInt<1>("h1")) @[L1MSHR.scala 135:36]
    node _T_11 = asUInt(reset) @[L1MSHR.scala 135:9]
    node _T_12 = eq(_T_11, UInt<1>("h0")) @[L1MSHR.scala 135:9]
    node _T_13 = eq(_T_10, UInt<1>("h0")) @[L1MSHR.scala 135:9]
    node secondaryMiss = orr(entryMatchProbe) @[L1MSHR.scala 136:39]
    node primaryMiss = eq(secondaryMiss, UInt<1>("h0")) @[L1MSHR.scala 137:21]
    node _T_14 = and(io_missReq_ready, io_missReq_valid) @[Decoupled.scala 50:35]
    node _T_15 = and(primaryMiss, entryStatus.io_alm_full) @[L1MSHR.scala 143:22]
    node _T_16 = and(secondaryMiss, subentryStatus.io_alm_full) @[L1MSHR.scala 145:30]
    node _GEN_8 = mux(_T_16, UInt<2>("h3"), mshrStatus_st1_r) @[L1MSHR.scala 145:50 146:24 122:33]
    node _GEN_9 = mux(_T_15, UInt<1>("h1"), _GEN_8) @[L1MSHR.scala 143:43 144:24]
    node _GEN_10 = mux(entryStatus.io_full, UInt<1>("h1"), UInt<1>("h0")) @[L1MSHR.scala 150:27 151:26 155:26]
    node _GEN_11 = mux(subentryStatus.io_full, UInt<2>("h3"), UInt<2>("h2")) @[L1MSHR.scala 158:26 159:26 163:26]
    node _GEN_12 = mux(primaryMiss, _GEN_10, _GEN_11) @[L1MSHR.scala 149:23]
    node _T_17 = eq(mshrStatus_st1_r, UInt<1>("h1")) @[L1MSHR.scala 168:27]
    node _T_18 = eq(mshrStatus_st1_r, UInt<2>("h3")) @[L1MSHR.scala 170:33]
    node _T_19 = eq(subentryStatusForRsp.io_used, UInt<1>("h1")) @[L1MSHR.scala 170:73]
    node _T_20 = and(_T_18, _T_19) @[L1MSHR.scala 170:41]
    node _T_21 = eq(mshrStatus_st1_r, UInt<3>("h4")) @[L1MSHR.scala 172:33]
    node _T_22 = eq(subentryStatusForRsp.io_used, UInt<1>("h0")) @[L1MSHR.scala 172:73]
    node _T_23 = and(_T_21, _T_22) @[L1MSHR.scala 172:41]
    node _GEN_13 = mux(_T_23, UInt<2>("h2"), mshrStatus_st1_r) @[L1MSHR.scala 172:82 173:24 122:33]
    node _GEN_14 = mux(_T_20, UInt<3>("h4"), _GEN_13) @[L1MSHR.scala 170:81 171:24]
    node _GEN_15 = mux(_T_17, UInt<1>("h0"), _GEN_14) @[L1MSHR.scala 168:35 169:24]
    node _GEN_16 = mux(io_missRspIn_valid, _GEN_15, mshrStatus_st1_r) @[L1MSHR.scala 122:33 166:33]
    node _GEN_17 = mux(io_probe_valid, _GEN_12, _GEN_16) @[L1MSHR.scala 148:29]
    node _GEN_18 = mux(_T_14, _GEN_9, _GEN_17) @[L1MSHR.scala 142:24]
    reg entryMatchProbe_st1 : UInt<4>, clock with :
      reset => (UInt<1>("h0"), entryMatchProbe_st1) @[Reg.scala 16:16]
    node _GEN_19 = mux(io_probe_valid, entryMatchProbe, entryMatchProbe_st1) @[Reg.scala 16:16 17:{18,22}]
    node _T_24 = eq(mshrStatus_st1_r, UInt<1>("h0")) @[L1MSHR.scala 180:43]
    node _T_25 = eq(mshrStatus_st1_r, UInt<1>("h1")) @[L1MSHR.scala 180:71]
    node _T_26 = or(_T_24, _T_25) @[L1MSHR.scala 180:51]
    node _T_27 = and(secondaryMiss, _T_26) @[L1MSHR.scala 180:22]
    node _GEN_20 = mux(_T_27, _GEN_11, mshrStatus_st1_r) @[L1MSHR.scala 180:80 187:22]
    node mshrStatus_st1_w = _GEN_20 @[L1MSHR.scala 123:30]
    node _io_missReq_ready_T = eq(mshrStatus_st1_w, UInt<1>("h1")) @[L1MSHR.scala 193:42]
    node _io_missReq_ready_T_1 = eq(mshrStatus_st1_w, UInt<2>("h3")) @[L1MSHR.scala 193:70]
    node _io_missReq_ready_T_2 = or(_io_missReq_ready_T, _io_missReq_ready_T_1) @[L1MSHR.scala 193:50]
    node _io_missReq_ready_T_3 = eq(_io_missReq_ready_T_2, UInt<1>("h0")) @[L1MSHR.scala 193:23]
    node _T_28 = and(io_missReq_ready, io_missReq_valid) @[Decoupled.scala 50:35]
    node _T_29 = eq(_T_28, UInt<1>("h0")) @[L1MSHR.scala 194:10]
    node _T_30 = and(io_missReq_ready, io_missReq_valid) @[Decoupled.scala 50:35]
    node _T_31 = and(io_missRspIn_ready, io_missRspIn_valid) @[Decoupled.scala 50:35]
    node _T_32 = eq(_T_31, UInt<1>("h0")) @[L1MSHR.scala 194:50]
    node _T_33 = and(_T_30, _T_32) @[L1MSHR.scala 194:47]
    node _T_34 = or(_T_29, _T_33) @[L1MSHR.scala 194:27]
    node _T_35 = asUInt(reset) @[L1MSHR.scala 194:9]
    node _T_36 = eq(_T_35, UInt<1>("h0")) @[L1MSHR.scala 194:9]
    node _T_37 = eq(_T_34, UInt<1>("h0")) @[L1MSHR.scala 194:9]
    node real_SRAMAddrUp_hi = bits(entryMatchProbe_st1, 3, 2) @[OneHot.scala 30:18]
    node real_SRAMAddrUp_lo = bits(entryMatchProbe_st1, 1, 0) @[OneHot.scala 31:18]
    node _real_SRAMAddrUp_T = orr(real_SRAMAddrUp_hi) @[OneHot.scala 32:14]
    node _real_SRAMAddrUp_T_1 = or(real_SRAMAddrUp_hi, real_SRAMAddrUp_lo) @[OneHot.scala 32:28]
    node _real_SRAMAddrUp_T_2 = bits(_real_SRAMAddrUp_T_1, 1, 1) @[CircuitMath.scala 30:8]
    node _real_SRAMAddrUp_T_3 = cat(_real_SRAMAddrUp_T, _real_SRAMAddrUp_T_2) @[Cat.scala 31:58]
    node real_SRAMAddrUp = mux(secondaryMiss, _real_SRAMAddrUp_T_3, entryStatus.io_next) @[L1MSHR.scala 195:28]
    node real_SRAMAddrDown = mux(secondaryMiss, subentryStatus.io_next, UInt<1>("h0")) @[L1MSHR.scala 196:30]
    node _T_38 = and(io_missReq_ready, io_missReq_valid) @[Decoupled.scala 50:35]
    node _targetInfo_Accesss_real_SRAMAddrUp_real_SRAMAddrDown = io_missReq_bits_targetInfo @[L1MSHR.scala 198:{60,60}]
    node _GEN_21 = mux(and(eq(UInt<1>("h0"), real_SRAMAddrUp), eq(UInt<1>("h0"), real_SRAMAddrDown)), _targetInfo_Accesss_real_SRAMAddrUp_real_SRAMAddrDown, targetInfo_Accesss_0_0) @[L1MSHR.scala 198:{60,60} 82:35]
    node _GEN_22 = mux(and(eq(UInt<1>("h0"), real_SRAMAddrUp), eq(UInt<1>("h1"), real_SRAMAddrDown)), _targetInfo_Accesss_real_SRAMAddrUp_real_SRAMAddrDown, targetInfo_Accesss_0_1) @[L1MSHR.scala 198:{60,60} 82:35]
    node _GEN_23 = mux(and(eq(UInt<1>("h1"), real_SRAMAddrUp), eq(UInt<1>("h0"), real_SRAMAddrDown)), _targetInfo_Accesss_real_SRAMAddrUp_real_SRAMAddrDown, targetInfo_Accesss_1_0) @[L1MSHR.scala 198:{60,60} 82:35]
    node _GEN_24 = mux(and(eq(UInt<1>("h1"), real_SRAMAddrUp), eq(UInt<1>("h1"), real_SRAMAddrDown)), _targetInfo_Accesss_real_SRAMAddrUp_real_SRAMAddrDown, targetInfo_Accesss_1_1) @[L1MSHR.scala 198:{60,60} 82:35]
    node _GEN_25 = mux(and(eq(UInt<2>("h2"), real_SRAMAddrUp), eq(UInt<1>("h0"), real_SRAMAddrDown)), _targetInfo_Accesss_real_SRAMAddrUp_real_SRAMAddrDown, targetInfo_Accesss_2_0) @[L1MSHR.scala 198:{60,60} 82:35]
    node _GEN_26 = mux(and(eq(UInt<2>("h2"), real_SRAMAddrUp), eq(UInt<1>("h1"), real_SRAMAddrDown)), _targetInfo_Accesss_real_SRAMAddrUp_real_SRAMAddrDown, targetInfo_Accesss_2_1) @[L1MSHR.scala 198:{60,60} 82:35]
    node _GEN_27 = mux(and(eq(UInt<2>("h3"), real_SRAMAddrUp), eq(UInt<1>("h0"), real_SRAMAddrDown)), _targetInfo_Accesss_real_SRAMAddrUp_real_SRAMAddrDown, targetInfo_Accesss_3_0) @[L1MSHR.scala 198:{60,60} 82:35]
    node _GEN_28 = mux(and(eq(UInt<2>("h3"), real_SRAMAddrUp), eq(UInt<1>("h1"), real_SRAMAddrDown)), _targetInfo_Accesss_real_SRAMAddrUp_real_SRAMAddrDown, targetInfo_Accesss_3_1) @[L1MSHR.scala 198:{60,60} 82:35]
    node _GEN_29 = mux(_T_38, _GEN_21, targetInfo_Accesss_0_0) @[L1MSHR.scala 197:25 82:35]
    node _GEN_30 = mux(_T_38, _GEN_22, targetInfo_Accesss_0_1) @[L1MSHR.scala 197:25 82:35]
    node _GEN_31 = mux(_T_38, _GEN_23, targetInfo_Accesss_1_0) @[L1MSHR.scala 197:25 82:35]
    node _GEN_32 = mux(_T_38, _GEN_24, targetInfo_Accesss_1_1) @[L1MSHR.scala 197:25 82:35]
    node _GEN_33 = mux(_T_38, _GEN_25, targetInfo_Accesss_2_0) @[L1MSHR.scala 197:25 82:35]
    node _GEN_34 = mux(_T_38, _GEN_26, targetInfo_Accesss_2_1) @[L1MSHR.scala 197:25 82:35]
    node _GEN_35 = mux(_T_38, _GEN_27, targetInfo_Accesss_3_0) @[L1MSHR.scala 197:25 82:35]
    node _GEN_36 = mux(_T_38, _GEN_28, targetInfo_Accesss_3_1) @[L1MSHR.scala 197:25 82:35]
    node _T_39 = and(io_missReq_ready, io_missReq_valid) @[Decoupled.scala 50:35]
    node _T_40 = eq(mshrStatus_st1_w, UInt<1>("h0")) @[L1MSHR.scala 201:44]
    node _T_41 = and(_T_39, _T_40) @[L1MSHR.scala 201:24]
    node _blockAddr_Access_entryStatus_io_next = io_missReq_bits_blockAddr @[L1MSHR.scala 202:{43,43}]
    node _GEN_37 = mux(eq(UInt<1>("h0"), entryStatus.io_next), _blockAddr_Access_entryStatus_io_next, blockAddr_Access_0) @[L1MSHR.scala 202:{43,43} 80:33]
    node _GEN_38 = mux(eq(UInt<1>("h1"), entryStatus.io_next), _blockAddr_Access_entryStatus_io_next, blockAddr_Access_1) @[L1MSHR.scala 202:{43,43} 80:33]
    node _GEN_39 = mux(eq(UInt<2>("h2"), entryStatus.io_next), _blockAddr_Access_entryStatus_io_next, blockAddr_Access_2) @[L1MSHR.scala 202:{43,43} 80:33]
    node _GEN_40 = mux(eq(UInt<2>("h3"), entryStatus.io_next), _blockAddr_Access_entryStatus_io_next, blockAddr_Access_3) @[L1MSHR.scala 202:{43,43} 80:33]
    node _instrId_Access_entryStatus_io_next = io_missReq_bits_instrId @[L1MSHR.scala 203:{41,41}]
    node _GEN_41 = mux(eq(UInt<1>("h0"), entryStatus.io_next), _instrId_Access_entryStatus_io_next, instrId_Access_0) @[L1MSHR.scala 203:{41,41} 81:31]
    node _GEN_42 = mux(eq(UInt<1>("h1"), entryStatus.io_next), _instrId_Access_entryStatus_io_next, instrId_Access_1) @[L1MSHR.scala 203:{41,41} 81:31]
    node _GEN_43 = mux(eq(UInt<2>("h2"), entryStatus.io_next), _instrId_Access_entryStatus_io_next, instrId_Access_2) @[L1MSHR.scala 203:{41,41} 81:31]
    node _GEN_44 = mux(eq(UInt<2>("h3"), entryStatus.io_next), _instrId_Access_entryStatus_io_next, instrId_Access_3) @[L1MSHR.scala 203:{41,41} 81:31]
    node _GEN_45 = mux(_T_41, _GEN_37, blockAddr_Access_0) @[L1MSHR.scala 201:53 80:33]
    node _GEN_46 = mux(_T_41, _GEN_38, blockAddr_Access_1) @[L1MSHR.scala 201:53 80:33]
    node _GEN_47 = mux(_T_41, _GEN_39, blockAddr_Access_2) @[L1MSHR.scala 201:53 80:33]
    node _GEN_48 = mux(_T_41, _GEN_40, blockAddr_Access_3) @[L1MSHR.scala 201:53 80:33]
    node _GEN_49 = mux(_T_41, _GEN_41, instrId_Access_0) @[L1MSHR.scala 201:53 81:31]
    node _GEN_50 = mux(_T_41, _GEN_42, instrId_Access_1) @[L1MSHR.scala 201:53 81:31]
    node _GEN_51 = mux(_T_41, _GEN_43, instrId_Access_2) @[L1MSHR.scala 201:53 81:31]
    node _GEN_52 = mux(_T_41, _GEN_44, instrId_Access_3) @[L1MSHR.scala 201:53 81:31]
    node entryMatchMissRsp = io_missRspIn_bits_instrId @[L1MSHR.scala 108:31 219:21]
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
    node _io_missRspIn_ready_T = geq(subentryStatusForRsp.io_used, UInt<2>("h2")) @[L1MSHR.scala 216:57]
    node _io_missRspIn_ready_T_1 = eq(mshrStatus_st1_w, UInt<3>("h4")) @[L1MSHR.scala 217:24]
    node _io_missRspIn_ready_T_2 = eq(mshrStatus_st1_w, UInt<2>("h3")) @[L1MSHR.scala 217:52]
    node _io_missRspIn_ready_T_3 = or(_io_missRspIn_ready_T_1, _io_missRspIn_ready_T_2) @[L1MSHR.scala 217:32]
    node _io_missRspIn_ready_T_4 = eq(subentryStatusForRsp.io_used, UInt<1>("h1")) @[L1MSHR.scala 217:93]
    node _io_missRspIn_ready_T_5 = and(_io_missRspIn_ready_T_3, _io_missRspIn_ready_T_4) @[L1MSHR.scala 217:61]
    node _io_missRspIn_ready_T_6 = or(_io_missRspIn_ready_T, _io_missRspIn_ready_T_5) @[L1MSHR.scala 216:65]
    node _io_missRspIn_ready_T_7 = eq(_io_missRspIn_ready_T_6, UInt<1>("h0")) @[L1MSHR.scala 216:25]
    reg io_missRspOut_bits_targetInfo_REG : UInt<31>, clock with :
      reset => (UInt<1>("h0"), io_missRspOut_bits_targetInfo_REG) @[L1MSHR.scala 228:43]
    node subentry_next2cancel = subentryStatusForRsp.io_next2cancel @[L1MSHR.scala 222:34 223:24]
    node _GEN_61 = validif(and(eq(UInt<1>("h0"), entryMatchMissRsp), eq(UInt<1>("h0"), subentry_next2cancel)), targetInfo_Accesss_0_0) @[L1MSHR.scala 228:{43,43}]
    node _GEN_62 = mux(and(eq(UInt<1>("h0"), entryMatchMissRsp), eq(UInt<1>("h1"), subentry_next2cancel)), targetInfo_Accesss_0_1, _GEN_61) @[L1MSHR.scala 228:{43,43}]
    node _GEN_63 = mux(and(eq(UInt<1>("h1"), entryMatchMissRsp), eq(UInt<1>("h0"), subentry_next2cancel)), targetInfo_Accesss_1_0, _GEN_62) @[L1MSHR.scala 228:{43,43}]
    node _GEN_64 = mux(and(eq(UInt<1>("h1"), entryMatchMissRsp), eq(UInt<1>("h1"), subentry_next2cancel)), targetInfo_Accesss_1_1, _GEN_63) @[L1MSHR.scala 228:{43,43}]
    node _GEN_65 = mux(and(eq(UInt<2>("h2"), entryMatchMissRsp), eq(UInt<1>("h0"), subentry_next2cancel)), targetInfo_Accesss_2_0, _GEN_64) @[L1MSHR.scala 228:{43,43}]
    node _GEN_66 = mux(and(eq(UInt<2>("h2"), entryMatchMissRsp), eq(UInt<1>("h1"), subentry_next2cancel)), targetInfo_Accesss_2_1, _GEN_65) @[L1MSHR.scala 228:{43,43}]
    node _GEN_67 = mux(and(eq(UInt<2>("h3"), entryMatchMissRsp), eq(UInt<1>("h0"), subentry_next2cancel)), targetInfo_Accesss_3_0, _GEN_66) @[L1MSHR.scala 228:{43,43}]
    node _GEN_68 = mux(and(eq(UInt<2>("h3"), entryMatchMissRsp), eq(UInt<1>("h1"), subentry_next2cancel)), targetInfo_Accesss_3_1, _GEN_67) @[L1MSHR.scala 228:{43,43}]
    reg io_missRspOut_bits_blockAddr_REG : UInt<28>, clock with :
      reset => (UInt<1>("h0"), io_missRspOut_bits_blockAddr_REG) @[L1MSHR.scala 229:42]
    node _GEN_69 = validif(eq(UInt<1>("h0"), entryMatchMissRsp), blockAddr_Access_0) @[L1MSHR.scala 229:{42,42}]
    node _GEN_70 = mux(eq(UInt<1>("h1"), entryMatchMissRsp), blockAddr_Access_1, _GEN_69) @[L1MSHR.scala 229:{42,42}]
    node _GEN_71 = mux(eq(UInt<2>("h2"), entryMatchMissRsp), blockAddr_Access_2, _GEN_70) @[L1MSHR.scala 229:{42,42}]
    node _GEN_72 = mux(eq(UInt<2>("h3"), entryMatchMissRsp), blockAddr_Access_3, _GEN_71) @[L1MSHR.scala 229:{42,42}]
    reg io_missRspOut_valid_REG : UInt<1>, clock with :
      reset => (UInt<1>("h0"), io_missRspOut_valid_REG) @[L1MSHR.scala 231:33]
    node _T_42 = eq(UInt<1>("h0"), entryStatus.io_next) @[L1MSHR.scala 239:24]
    node _T_43 = eq(UInt<1>("h0"), UInt<1>("h0")) @[L1MSHR.scala 240:24]
    node _T_44 = and(_T_42, _T_43) @[L1MSHR.scala 239:47]
    node _T_45 = and(io_missReq_ready, io_missReq_valid) @[Decoupled.scala 50:35]
    node _T_46 = and(_T_44, _T_45) @[L1MSHR.scala 240:31]
    node _T_47 = and(_T_46, primaryMiss) @[L1MSHR.scala 240:50]
    node _T_48 = eq(UInt<1>("h0"), entryMatchMissRsp) @[L1MSHR.scala 242:30]
    node _T_49 = eq(UInt<1>("h0"), subentry_next2cancel) @[L1MSHR.scala 243:29]
    node _T_50 = and(_T_49, io_missRspIn_valid) @[L1MSHR.scala 243:53]
    node _T_51 = eq(UInt<1>("h0"), subentryStatus.io_next) @[L1MSHR.scala 246:35]
    node _T_52 = and(io_missReq_ready, io_missReq_valid) @[Decoupled.scala 50:35]
    node _T_53 = and(_T_51, _T_52) @[L1MSHR.scala 246:61]
    node _T_54 = and(_T_53, secondaryMiss) @[L1MSHR.scala 247:27]
    node _GEN_73 = mux(_T_54, UInt<1>("h1"), subentry_valid_0_0) @[L1MSHR.scala 247:44 248:43 84:31]
    node _GEN_74 = mux(_T_50, UInt<1>("h0"), _GEN_73) @[L1MSHR.scala 244:30 245:43]
    node _GEN_75 = mux(_T_48, _GEN_74, subentry_valid_0_0) @[L1MSHR.scala 242:51 84:31]
    node _GEN_76 = mux(_T_47, UInt<1>("h1"), _GEN_75) @[L1MSHR.scala 240:65 241:41]
    node _T_55 = eq(UInt<1>("h0"), entryStatus.io_next) @[L1MSHR.scala 239:24]
    node _T_56 = eq(UInt<1>("h1"), UInt<1>("h0")) @[L1MSHR.scala 240:24]
    node _T_57 = and(_T_55, _T_56) @[L1MSHR.scala 239:47]
    node _T_58 = and(io_missReq_ready, io_missReq_valid) @[Decoupled.scala 50:35]
    node _T_59 = and(_T_57, _T_58) @[L1MSHR.scala 240:31]
    node _T_60 = and(_T_59, primaryMiss) @[L1MSHR.scala 240:50]
    node _T_61 = eq(UInt<1>("h0"), entryMatchMissRsp) @[L1MSHR.scala 242:30]
    node _T_62 = eq(UInt<1>("h1"), subentry_next2cancel) @[L1MSHR.scala 243:29]
    node _T_63 = and(_T_62, io_missRspIn_valid) @[L1MSHR.scala 243:53]
    node _T_64 = eq(UInt<1>("h1"), subentryStatus.io_next) @[L1MSHR.scala 246:35]
    node _T_65 = and(io_missReq_ready, io_missReq_valid) @[Decoupled.scala 50:35]
    node _T_66 = and(_T_64, _T_65) @[L1MSHR.scala 246:61]
    node _T_67 = and(_T_66, secondaryMiss) @[L1MSHR.scala 247:27]
    node _GEN_77 = mux(_T_67, UInt<1>("h1"), subentry_valid_0_1) @[L1MSHR.scala 247:44 248:43 84:31]
    node _GEN_78 = mux(_T_63, UInt<1>("h0"), _GEN_77) @[L1MSHR.scala 244:30 245:43]
    node _GEN_79 = mux(_T_61, _GEN_78, subentry_valid_0_1) @[L1MSHR.scala 242:51 84:31]
    node _GEN_80 = mux(_T_60, UInt<1>("h1"), _GEN_79) @[L1MSHR.scala 240:65 241:41]
    node _T_68 = eq(UInt<1>("h1"), entryStatus.io_next) @[L1MSHR.scala 239:24]
    node _T_69 = eq(UInt<1>("h0"), UInt<1>("h0")) @[L1MSHR.scala 240:24]
    node _T_70 = and(_T_68, _T_69) @[L1MSHR.scala 239:47]
    node _T_71 = and(io_missReq_ready, io_missReq_valid) @[Decoupled.scala 50:35]
    node _T_72 = and(_T_70, _T_71) @[L1MSHR.scala 240:31]
    node _T_73 = and(_T_72, primaryMiss) @[L1MSHR.scala 240:50]
    node _T_74 = eq(UInt<1>("h1"), entryMatchMissRsp) @[L1MSHR.scala 242:30]
    node _T_75 = eq(UInt<1>("h0"), subentry_next2cancel) @[L1MSHR.scala 243:29]
    node _T_76 = and(_T_75, io_missRspIn_valid) @[L1MSHR.scala 243:53]
    node _T_77 = eq(UInt<1>("h0"), subentryStatus.io_next) @[L1MSHR.scala 246:35]
    node _T_78 = and(io_missReq_ready, io_missReq_valid) @[Decoupled.scala 50:35]
    node _T_79 = and(_T_77, _T_78) @[L1MSHR.scala 246:61]
    node _T_80 = and(_T_79, secondaryMiss) @[L1MSHR.scala 247:27]
    node _GEN_81 = mux(_T_80, UInt<1>("h1"), subentry_valid_1_0) @[L1MSHR.scala 247:44 248:43 84:31]
    node _GEN_82 = mux(_T_76, UInt<1>("h0"), _GEN_81) @[L1MSHR.scala 244:30 245:43]
    node _GEN_83 = mux(_T_74, _GEN_82, subentry_valid_1_0) @[L1MSHR.scala 242:51 84:31]
    node _GEN_84 = mux(_T_73, UInt<1>("h1"), _GEN_83) @[L1MSHR.scala 240:65 241:41]
    node _T_81 = eq(UInt<1>("h1"), entryStatus.io_next) @[L1MSHR.scala 239:24]
    node _T_82 = eq(UInt<1>("h1"), UInt<1>("h0")) @[L1MSHR.scala 240:24]
    node _T_83 = and(_T_81, _T_82) @[L1MSHR.scala 239:47]
    node _T_84 = and(io_missReq_ready, io_missReq_valid) @[Decoupled.scala 50:35]
    node _T_85 = and(_T_83, _T_84) @[L1MSHR.scala 240:31]
    node _T_86 = and(_T_85, primaryMiss) @[L1MSHR.scala 240:50]
    node _T_87 = eq(UInt<1>("h1"), entryMatchMissRsp) @[L1MSHR.scala 242:30]
    node _T_88 = eq(UInt<1>("h1"), subentry_next2cancel) @[L1MSHR.scala 243:29]
    node _T_89 = and(_T_88, io_missRspIn_valid) @[L1MSHR.scala 243:53]
    node _T_90 = eq(UInt<1>("h1"), subentryStatus.io_next) @[L1MSHR.scala 246:35]
    node _T_91 = and(io_missReq_ready, io_missReq_valid) @[Decoupled.scala 50:35]
    node _T_92 = and(_T_90, _T_91) @[L1MSHR.scala 246:61]
    node _T_93 = and(_T_92, secondaryMiss) @[L1MSHR.scala 247:27]
    node _GEN_85 = mux(_T_93, UInt<1>("h1"), subentry_valid_1_1) @[L1MSHR.scala 247:44 248:43 84:31]
    node _GEN_86 = mux(_T_89, UInt<1>("h0"), _GEN_85) @[L1MSHR.scala 244:30 245:43]
    node _GEN_87 = mux(_T_87, _GEN_86, subentry_valid_1_1) @[L1MSHR.scala 242:51 84:31]
    node _GEN_88 = mux(_T_86, UInt<1>("h1"), _GEN_87) @[L1MSHR.scala 240:65 241:41]
    node _T_94 = eq(UInt<2>("h2"), entryStatus.io_next) @[L1MSHR.scala 239:24]
    node _T_95 = eq(UInt<1>("h0"), UInt<1>("h0")) @[L1MSHR.scala 240:24]
    node _T_96 = and(_T_94, _T_95) @[L1MSHR.scala 239:47]
    node _T_97 = and(io_missReq_ready, io_missReq_valid) @[Decoupled.scala 50:35]
    node _T_98 = and(_T_96, _T_97) @[L1MSHR.scala 240:31]
    node _T_99 = and(_T_98, primaryMiss) @[L1MSHR.scala 240:50]
    node _T_100 = eq(UInt<2>("h2"), entryMatchMissRsp) @[L1MSHR.scala 242:30]
    node _T_101 = eq(UInt<1>("h0"), subentry_next2cancel) @[L1MSHR.scala 243:29]
    node _T_102 = and(_T_101, io_missRspIn_valid) @[L1MSHR.scala 243:53]
    node _T_103 = eq(UInt<1>("h0"), subentryStatus.io_next) @[L1MSHR.scala 246:35]
    node _T_104 = and(io_missReq_ready, io_missReq_valid) @[Decoupled.scala 50:35]
    node _T_105 = and(_T_103, _T_104) @[L1MSHR.scala 246:61]
    node _T_106 = and(_T_105, secondaryMiss) @[L1MSHR.scala 247:27]
    node _GEN_89 = mux(_T_106, UInt<1>("h1"), subentry_valid_2_0) @[L1MSHR.scala 247:44 248:43 84:31]
    node _GEN_90 = mux(_T_102, UInt<1>("h0"), _GEN_89) @[L1MSHR.scala 244:30 245:43]
    node _GEN_91 = mux(_T_100, _GEN_90, subentry_valid_2_0) @[L1MSHR.scala 242:51 84:31]
    node _GEN_92 = mux(_T_99, UInt<1>("h1"), _GEN_91) @[L1MSHR.scala 240:65 241:41]
    node _T_107 = eq(UInt<2>("h2"), entryStatus.io_next) @[L1MSHR.scala 239:24]
    node _T_108 = eq(UInt<1>("h1"), UInt<1>("h0")) @[L1MSHR.scala 240:24]
    node _T_109 = and(_T_107, _T_108) @[L1MSHR.scala 239:47]
    node _T_110 = and(io_missReq_ready, io_missReq_valid) @[Decoupled.scala 50:35]
    node _T_111 = and(_T_109, _T_110) @[L1MSHR.scala 240:31]
    node _T_112 = and(_T_111, primaryMiss) @[L1MSHR.scala 240:50]
    node _T_113 = eq(UInt<2>("h2"), entryMatchMissRsp) @[L1MSHR.scala 242:30]
    node _T_114 = eq(UInt<1>("h1"), subentry_next2cancel) @[L1MSHR.scala 243:29]
    node _T_115 = and(_T_114, io_missRspIn_valid) @[L1MSHR.scala 243:53]
    node _T_116 = eq(UInt<1>("h1"), subentryStatus.io_next) @[L1MSHR.scala 246:35]
    node _T_117 = and(io_missReq_ready, io_missReq_valid) @[Decoupled.scala 50:35]
    node _T_118 = and(_T_116, _T_117) @[L1MSHR.scala 246:61]
    node _T_119 = and(_T_118, secondaryMiss) @[L1MSHR.scala 247:27]
    node _GEN_93 = mux(_T_119, UInt<1>("h1"), subentry_valid_2_1) @[L1MSHR.scala 247:44 248:43 84:31]
    node _GEN_94 = mux(_T_115, UInt<1>("h0"), _GEN_93) @[L1MSHR.scala 244:30 245:43]
    node _GEN_95 = mux(_T_113, _GEN_94, subentry_valid_2_1) @[L1MSHR.scala 242:51 84:31]
    node _GEN_96 = mux(_T_112, UInt<1>("h1"), _GEN_95) @[L1MSHR.scala 240:65 241:41]
    node _T_120 = eq(UInt<2>("h3"), entryStatus.io_next) @[L1MSHR.scala 239:24]
    node _T_121 = eq(UInt<1>("h0"), UInt<1>("h0")) @[L1MSHR.scala 240:24]
    node _T_122 = and(_T_120, _T_121) @[L1MSHR.scala 239:47]
    node _T_123 = and(io_missReq_ready, io_missReq_valid) @[Decoupled.scala 50:35]
    node _T_124 = and(_T_122, _T_123) @[L1MSHR.scala 240:31]
    node _T_125 = and(_T_124, primaryMiss) @[L1MSHR.scala 240:50]
    node _T_126 = eq(UInt<2>("h3"), entryMatchMissRsp) @[L1MSHR.scala 242:30]
    node _T_127 = eq(UInt<1>("h0"), subentry_next2cancel) @[L1MSHR.scala 243:29]
    node _T_128 = and(_T_127, io_missRspIn_valid) @[L1MSHR.scala 243:53]
    node _T_129 = eq(UInt<1>("h0"), subentryStatus.io_next) @[L1MSHR.scala 246:35]
    node _T_130 = and(io_missReq_ready, io_missReq_valid) @[Decoupled.scala 50:35]
    node _T_131 = and(_T_129, _T_130) @[L1MSHR.scala 246:61]
    node _T_132 = and(_T_131, secondaryMiss) @[L1MSHR.scala 247:27]
    node _GEN_97 = mux(_T_132, UInt<1>("h1"), subentry_valid_3_0) @[L1MSHR.scala 247:44 248:43 84:31]
    node _GEN_98 = mux(_T_128, UInt<1>("h0"), _GEN_97) @[L1MSHR.scala 244:30 245:43]
    node _GEN_99 = mux(_T_126, _GEN_98, subentry_valid_3_0) @[L1MSHR.scala 242:51 84:31]
    node _GEN_100 = mux(_T_125, UInt<1>("h1"), _GEN_99) @[L1MSHR.scala 240:65 241:41]
    node _T_133 = eq(UInt<2>("h3"), entryStatus.io_next) @[L1MSHR.scala 239:24]
    node _T_134 = eq(UInt<1>("h1"), UInt<1>("h0")) @[L1MSHR.scala 240:24]
    node _T_135 = and(_T_133, _T_134) @[L1MSHR.scala 239:47]
    node _T_136 = and(io_missReq_ready, io_missReq_valid) @[Decoupled.scala 50:35]
    node _T_137 = and(_T_135, _T_136) @[L1MSHR.scala 240:31]
    node _T_138 = and(_T_137, primaryMiss) @[L1MSHR.scala 240:50]
    node _T_139 = eq(UInt<2>("h3"), entryMatchMissRsp) @[L1MSHR.scala 242:30]
    node _T_140 = eq(UInt<1>("h1"), subentry_next2cancel) @[L1MSHR.scala 243:29]
    node _T_141 = and(_T_140, io_missRspIn_valid) @[L1MSHR.scala 243:53]
    node _T_142 = eq(UInt<1>("h1"), subentryStatus.io_next) @[L1MSHR.scala 246:35]
    node _T_143 = and(io_missReq_ready, io_missReq_valid) @[Decoupled.scala 50:35]
    node _T_144 = and(_T_142, _T_143) @[L1MSHR.scala 246:61]
    node _T_145 = and(_T_144, secondaryMiss) @[L1MSHR.scala 247:27]
    node _GEN_101 = mux(_T_145, UInt<1>("h1"), subentry_valid_3_1) @[L1MSHR.scala 247:44 248:43 84:31]
    node _GEN_102 = mux(_T_141, UInt<1>("h0"), _GEN_101) @[L1MSHR.scala 244:30 245:43]
    node _GEN_103 = mux(_T_139, _GEN_102, subentry_valid_3_1) @[L1MSHR.scala 242:51 84:31]
    node _GEN_104 = mux(_T_138, UInt<1>("h1"), _GEN_103) @[L1MSHR.scala 240:65 241:41]
    node _blockAddr_Access_WIRE_0 = UInt<28>("h0") @[L1MSHR.scala 80:{41,41}]
    node _blockAddr_Access_WIRE_1 = UInt<28>("h0") @[L1MSHR.scala 80:{41,41}]
    node _blockAddr_Access_WIRE_2 = UInt<28>("h0") @[L1MSHR.scala 80:{41,41}]
    node _blockAddr_Access_WIRE_3 = UInt<28>("h0") @[L1MSHR.scala 80:{41,41}]
    node _instrId_Access_WIRE_0 = UInt<3>("h0") @[L1MSHR.scala 81:{39,39}]
    node _instrId_Access_WIRE_1 = UInt<3>("h0") @[L1MSHR.scala 81:{39,39}]
    node _instrId_Access_WIRE_2 = UInt<3>("h0") @[L1MSHR.scala 81:{39,39}]
    node _instrId_Access_WIRE_3 = UInt<3>("h0") @[L1MSHR.scala 81:{39,39}]
    node _targetInfo_Accesss_WIRE__0 = UInt<31>("h0") @[L1MSHR.scala 82:{72,72}]
    node _targetInfo_Accesss_WIRE__1 = UInt<31>("h0") @[L1MSHR.scala 82:{72,72}]
    node _targetInfo_Accesss_WIRE_1_0 = UInt<31>("h0") @[L1MSHR.scala 82:{72,72}]
    node _targetInfo_Accesss_WIRE_1_1 = UInt<31>("h0") @[L1MSHR.scala 82:{72,72}]
    node _targetInfo_Accesss_WIRE_2_0 = UInt<31>("h0") @[L1MSHR.scala 82:{72,72}]
    node _targetInfo_Accesss_WIRE_2_1 = UInt<31>("h0") @[L1MSHR.scala 82:{72,72}]
    node _targetInfo_Accesss_WIRE_3_0 = UInt<31>("h0") @[L1MSHR.scala 82:{72,72}]
    node _targetInfo_Accesss_WIRE_3_1 = UInt<31>("h0") @[L1MSHR.scala 82:{72,72}]
    node _targetInfo_Accesss_WIRE_4_0_0 = _targetInfo_Accesss_WIRE__0 @[L1MSHR.scala 82:{43,43}]
    node _targetInfo_Accesss_WIRE_4_0_1 = _targetInfo_Accesss_WIRE__1 @[L1MSHR.scala 82:{43,43}]
    node _targetInfo_Accesss_WIRE_4_1_0 = _targetInfo_Accesss_WIRE_1_0 @[L1MSHR.scala 82:{43,43}]
    node _targetInfo_Accesss_WIRE_4_1_1 = _targetInfo_Accesss_WIRE_1_1 @[L1MSHR.scala 82:{43,43}]
    node _targetInfo_Accesss_WIRE_4_2_0 = _targetInfo_Accesss_WIRE_2_0 @[L1MSHR.scala 82:{43,43}]
    node _targetInfo_Accesss_WIRE_4_2_1 = _targetInfo_Accesss_WIRE_2_1 @[L1MSHR.scala 82:{43,43}]
    node _targetInfo_Accesss_WIRE_4_3_0 = _targetInfo_Accesss_WIRE_3_0 @[L1MSHR.scala 82:{43,43}]
    node _targetInfo_Accesss_WIRE_4_3_1 = _targetInfo_Accesss_WIRE_3_1 @[L1MSHR.scala 82:{43,43}]
    node _subentry_valid_WIRE__0 = UInt<1>("h0") @[L1MSHR.scala 84:{68,68}]
    node _subentry_valid_WIRE__1 = UInt<1>("h0") @[L1MSHR.scala 84:{68,68}]
    node _subentry_valid_WIRE_1_0 = UInt<1>("h0") @[L1MSHR.scala 84:{68,68}]
    node _subentry_valid_WIRE_1_1 = UInt<1>("h0") @[L1MSHR.scala 84:{68,68}]
    node _subentry_valid_WIRE_2_0 = UInt<1>("h0") @[L1MSHR.scala 84:{68,68}]
    node _subentry_valid_WIRE_2_1 = UInt<1>("h0") @[L1MSHR.scala 84:{68,68}]
    node _subentry_valid_WIRE_3_0 = UInt<1>("h0") @[L1MSHR.scala 84:{68,68}]
    node _subentry_valid_WIRE_3_1 = UInt<1>("h0") @[L1MSHR.scala 84:{68,68}]
    node _subentry_valid_WIRE_4_0_0 = _subentry_valid_WIRE__0 @[L1MSHR.scala 84:{39,39}]
    node _subentry_valid_WIRE_4_0_1 = _subentry_valid_WIRE__1 @[L1MSHR.scala 84:{39,39}]
    node _subentry_valid_WIRE_4_1_0 = _subentry_valid_WIRE_1_0 @[L1MSHR.scala 84:{39,39}]
    node _subentry_valid_WIRE_4_1_1 = _subentry_valid_WIRE_1_1 @[L1MSHR.scala 84:{39,39}]
    node _subentry_valid_WIRE_4_2_0 = _subentry_valid_WIRE_2_0 @[L1MSHR.scala 84:{39,39}]
    node _subentry_valid_WIRE_4_2_1 = _subentry_valid_WIRE_2_1 @[L1MSHR.scala 84:{39,39}]
    node _subentry_valid_WIRE_4_3_0 = _subentry_valid_WIRE_3_0 @[L1MSHR.scala 84:{39,39}]
    node _subentry_valid_WIRE_4_3_1 = _subentry_valid_WIRE_3_1 @[L1MSHR.scala 84:{39,39}]
    node _targetInfo_Accesss_entryMatchMissRsp_subentry_next2cancel = _GEN_68 @[L1MSHR.scala 228:43]
    node _blockAddr_Access_entryMatchMissRsp = _GEN_72 @[L1MSHR.scala 229:42]
    io_probeOut_st1_probeStatus <= mshrStatus_st1_w @[L1MSHR.scala 189:31]
    io_probeOut_st1_a_source <= real_SRAMAddrUp @[L1MSHR.scala 206:28]
    io_missReq_ready <= _io_missReq_ready_T_3 @[L1MSHR.scala 193:20]
    io_missRspIn_ready <= _io_missRspIn_ready_T_7 @[L1MSHR.scala 216:22]
    io_missRspOut_valid <= io_missRspOut_valid_REG @[L1MSHR.scala 231:23]
    io_missRspOut_bits_targetInfo <= io_missRspOut_bits_targetInfo_REG @[L1MSHR.scala 228:33]
    io_missRspOut_bits_blockAddr <= io_missRspOut_bits_blockAddr_REG @[L1MSHR.scala 229:32]
    io_missRspOut_bits_instrId <= pad(io_missRspIn_bits_instrId, 3) @[L1MSHR.scala 230:30]
    blockAddr_Access_0 <= mux(reset, _blockAddr_Access_WIRE_0, _GEN_45) @[L1MSHR.scala 80:{33,33}]
    blockAddr_Access_1 <= mux(reset, _blockAddr_Access_WIRE_1, _GEN_46) @[L1MSHR.scala 80:{33,33}]
    blockAddr_Access_2 <= mux(reset, _blockAddr_Access_WIRE_2, _GEN_47) @[L1MSHR.scala 80:{33,33}]
    blockAddr_Access_3 <= mux(reset, _blockAddr_Access_WIRE_3, _GEN_48) @[L1MSHR.scala 80:{33,33}]
    instrId_Access_0 <= mux(reset, _instrId_Access_WIRE_0, _GEN_49) @[L1MSHR.scala 81:{31,31}]
    instrId_Access_1 <= mux(reset, _instrId_Access_WIRE_1, _GEN_50) @[L1MSHR.scala 81:{31,31}]
    instrId_Access_2 <= mux(reset, _instrId_Access_WIRE_2, _GEN_51) @[L1MSHR.scala 81:{31,31}]
    instrId_Access_3 <= mux(reset, _instrId_Access_WIRE_3, _GEN_52) @[L1MSHR.scala 81:{31,31}]
    targetInfo_Accesss_0_0 <= mux(reset, _targetInfo_Accesss_WIRE_4_0_0, _GEN_29) @[L1MSHR.scala 82:{35,35}]
    targetInfo_Accesss_0_1 <= mux(reset, _targetInfo_Accesss_WIRE_4_0_1, _GEN_30) @[L1MSHR.scala 82:{35,35}]
    targetInfo_Accesss_1_0 <= mux(reset, _targetInfo_Accesss_WIRE_4_1_0, _GEN_31) @[L1MSHR.scala 82:{35,35}]
    targetInfo_Accesss_1_1 <= mux(reset, _targetInfo_Accesss_WIRE_4_1_1, _GEN_32) @[L1MSHR.scala 82:{35,35}]
    targetInfo_Accesss_2_0 <= mux(reset, _targetInfo_Accesss_WIRE_4_2_0, _GEN_33) @[L1MSHR.scala 82:{35,35}]
    targetInfo_Accesss_2_1 <= mux(reset, _targetInfo_Accesss_WIRE_4_2_1, _GEN_34) @[L1MSHR.scala 82:{35,35}]
    targetInfo_Accesss_3_0 <= mux(reset, _targetInfo_Accesss_WIRE_4_3_0, _GEN_35) @[L1MSHR.scala 82:{35,35}]
    targetInfo_Accesss_3_1 <= mux(reset, _targetInfo_Accesss_WIRE_4_3_1, _GEN_36) @[L1MSHR.scala 82:{35,35}]
    subentry_valid_0_0 <= mux(reset, _subentry_valid_WIRE_4_0_0, _GEN_76) @[L1MSHR.scala 84:{31,31}]
    subentry_valid_0_1 <= mux(reset, _subentry_valid_WIRE_4_0_1, _GEN_80) @[L1MSHR.scala 84:{31,31}]
    subentry_valid_1_0 <= mux(reset, _subentry_valid_WIRE_4_1_0, _GEN_84) @[L1MSHR.scala 84:{31,31}]
    subentry_valid_1_1 <= mux(reset, _subentry_valid_WIRE_4_1_1, _GEN_88) @[L1MSHR.scala 84:{31,31}]
    subentry_valid_2_0 <= mux(reset, _subentry_valid_WIRE_4_2_0, _GEN_92) @[L1MSHR.scala 84:{31,31}]
    subentry_valid_2_1 <= mux(reset, _subentry_valid_WIRE_4_2_1, _GEN_96) @[L1MSHR.scala 84:{31,31}]
    subentry_valid_3_0 <= mux(reset, _subentry_valid_WIRE_4_3_0, _GEN_100) @[L1MSHR.scala 84:{31,31}]
    subentry_valid_3_1 <= mux(reset, _subentry_valid_WIRE_4_3_1, _GEN_104) @[L1MSHR.scala 84:{31,31}]
    subentryStatus.clock <= clock
    subentryStatus.reset <= reset
    subentryStatus.io_valid_list <= _subentryStatus_io_valid_list_T_3 @[L1MSHR.scala 112:32]
    subentryStatusForRsp.clock <= clock
    subentryStatusForRsp.reset <= reset
    subentryStatusForRsp.io_valid_list <= _subentryStatusForRsp_io_valid_list_T_3 @[L1MSHR.scala 209:38]
    entryStatus.clock <= clock
    entryStatus.reset <= reset
    entryStatus.io_valid_list <= entry_valid @[L1MSHR.scala 119:29]
    mshrStatus_st1_r <= mux(reset, UInt<3>("h0"), _GEN_18) @[L1MSHR.scala 122:{33,33}]
    entryMatchProbe_st1 <= _GEN_19
    io_missRspOut_bits_targetInfo_REG <= _targetInfo_Accesss_entryMatchMissRsp_subentry_next2cancel @[L1MSHR.scala 228:43]
    io_missRspOut_bits_blockAddr_REG <= _blockAddr_Access_entryMatchMissRsp @[L1MSHR.scala 229:42]
    io_missRspOut_valid_REG <= io_missRspIn_valid @[L1MSHR.scala 231:33]
    printf(clock, and(and(and(UInt<1>("h1"), _T_12), _T_13), UInt<1>("h1")), "Assertion failed\n    at L1MSHR.scala:135 assert(PopCount(entryMatchProbe) <= 1.U)\n") : printf @[L1MSHR.scala 135:9]
    assert(clock, _T_10, and(and(UInt<1>("h1"), _T_12), UInt<1>("h1")), "") : assert @[L1MSHR.scala 135:9]
    printf(clock, and(and(and(UInt<1>("h1"), _T_36), _T_37), UInt<1>("h1")), "Assertion failed: MSHR cant have Req & Rsp valid in same cycle, later the prior\n    at L1MSHR.scala:194 assert(!io.missReq.fire || (io.missReq.fire && !io.missRspIn.fire),\"MSHR cant have Req & Rsp valid in same cycle, later the prior\")\n") : printf_1 @[L1MSHR.scala 194:9]
    assert(clock, _T_34, and(and(UInt<1>("h1"), _T_36), UInt<1>("h1")), "") : assert_1 @[L1MSHR.scala 194:9]

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
    input io_out_ready : UInt<1>
    output io_out_valid : UInt<1>
    output io_out_bits_setIdx : UInt<2>
    output io_chosen : UInt<1>

    node _GEN_0 = mux(io_in_0_valid, UInt<1>("h0"), UInt<1>("h1")) @[Arbiter.scala 138:13 141:26 142:17]
    node _GEN_1 = mux(io_in_0_valid, io_in_0_bits_setIdx, io_in_1_bits_setIdx) @[Arbiter.scala 139:15 141:26 143:19]
    node grant_1 = eq(io_in_0_valid, UInt<1>("h0")) @[Arbiter.scala 46:78]
    node _io_in_0_ready_T = and(UInt<1>("h1"), io_out_ready) @[Arbiter.scala 149:19]
    node _io_in_1_ready_T = and(grant_1, io_out_ready) @[Arbiter.scala 149:19]
    node _io_out_valid_T = eq(grant_1, UInt<1>("h0")) @[Arbiter.scala 150:19]
    node _io_out_valid_T_1 = or(_io_out_valid_T, io_in_1_valid) @[Arbiter.scala 150:31]
    io_in_0_ready <= _io_in_0_ready_T @[Arbiter.scala 149:14]
    io_in_1_ready <= _io_in_1_ready_T @[Arbiter.scala 149:14]
    io_out_valid <= _io_out_valid_T_1 @[Arbiter.scala 150:16]
    io_out_bits_setIdx <= _GEN_1
    io_chosen <= _GEN_0

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

    node _io_waymask_T = eq(io_tag_of_set_0, io_tag_from_pipe) @[L1TagAccess.scala 196:90]
    node _io_waymask_T_1 = and(_io_waymask_T, io_way_valid_0) @[L1TagAccess.scala 196:112]
    node _io_waymask_T_2 = eq(io_tag_of_set_1, io_tag_from_pipe) @[L1TagAccess.scala 196:90]
    node _io_waymask_T_3 = and(_io_waymask_T_2, io_way_valid_1) @[L1TagAccess.scala 196:112]
    node _io_waymask_T_4 = cat(_io_waymask_T_1, _io_waymask_T_3) @[Cat.scala 31:58]
    node _io_waymask_T_5 = bits(_io_waymask_T_4, 0, 0) @[Bitwise.scala 111:18]
    node _io_waymask_T_6 = bits(_io_waymask_T_4, 1, 1) @[Bitwise.scala 111:47]
    node _io_waymask_T_7 = cat(_io_waymask_T_5, _io_waymask_T_6) @[Cat.scala 31:58]
    node _T = bits(io_waymask, 0, 0) @[Bitwise.scala 50:65]
    node _T_1 = bits(io_waymask, 1, 1) @[Bitwise.scala 50:65]
    node _T_2 = add(_T, _T_1) @[Bitwise.scala 48:55]
    node _T_3 = bits(_T_2, 1, 0) @[Bitwise.scala 48:55]
    node _T_4 = leq(_T_3, UInt<1>("h1")) @[L1TagAccess.scala 198:31]
    node _T_5 = asUInt(reset) @[L1TagAccess.scala 198:9]
    node _T_6 = eq(_T_5, UInt<1>("h0")) @[L1TagAccess.scala 198:9]
    node _T_7 = eq(_T_4, UInt<1>("h0")) @[L1TagAccess.scala 198:9]
    node _io_cache_hit_T = orr(io_waymask) @[L1TagAccess.scala 199:30]
    io_waymask <= _io_waymask_T_7 @[L1TagAccess.scala 196:14]
    io_cache_hit <= _io_cache_hit_T @[L1TagAccess.scala 199:16]
    printf(clock, and(and(and(UInt<1>("h1"), _T_6), _T_7), UInt<1>("h1")), "Assertion failed\n    at L1TagAccess.scala:198 assert(PopCount(io.waymask) <= 1.U)//if waymask not one-hot, duplicate tags in one set, error\n") : printf @[L1TagAccess.scala 198:9]
    assert(clock, _T_4, and(and(UInt<1>("h1"), _T_6), UInt<1>("h1")), "") : assert @[L1TagAccess.scala 198:9]

  module minIdxTree :
    input clock : Clock
    input reset : UInt<1>
    input io_candidateIn_0 : UInt<10>
    input io_candidateIn_1 : UInt<10>
    output io_idxOfMin : UInt<1>

    node candVec_0_candidate = io_candidateIn_1 @[L1TagAccess.scala 214:21 216:26]
    node candVec_1_candidate = io_candidateIn_0 @[L1TagAccess.scala 214:21 216:26]
    node _io_idxOfMin_T = lt(candVec_0_candidate, candVec_1_candidate) @[L1TagAccess.scala 212:79]
    node _io_idxOfMin_T_1_candidate = mux(_io_idxOfMin_T, candVec_0_candidate, candVec_1_candidate) @[L1TagAccess.scala 212:66]
    node candVec_0_index = UInt<1>("h0") @[L1TagAccess.scala 214:21 217:22]
    node candVec_1_index = UInt<1>("h1") @[L1TagAccess.scala 214:21 217:22]
    node _io_idxOfMin_T_1_index = mux(_io_idxOfMin_T, candVec_0_index, candVec_1_index) @[L1TagAccess.scala 212:66]
    io_idxOfMin <= _io_idxOfMin_T_1_index @[L1TagAccess.scala 220:15]

  module ReplacementUnit :
    input clock : Clock
    input reset : UInt<1>
    input io_validOfSet : UInt<2>
    input io_timeOfSet_st1_0 : UInt<10>
    input io_timeOfSet_st1_1 : UInt<10>
    output io_waymask_st1 : UInt<2>
    output io_Set_is_full : UInt<1>

    inst minTimeChooser of minIdxTree @[L1TagAccess.scala 164:32]
    node _io_Set_is_full_T = mux(UInt<1>("h1"), UInt<2>("h3"), UInt<2>("h0")) @[Bitwise.scala 74:12]
    node _io_Set_is_full_T_1 = eq(io_validOfSet, _io_Set_is_full_T) @[L1TagAccess.scala 158:35]
    node _timeOfSetAfterValid_0_T = bits(io_validOfSet, 0, 0) @[L1TagAccess.scala 163:50]
    node _timeOfSetAfterValid_0_T_1 = mux(_timeOfSetAfterValid_0_T, io_timeOfSet_st1_0, UInt<1>("h0")) @[L1TagAccess.scala 163:36]
    node _timeOfSetAfterValid_1_T = bits(io_validOfSet, 1, 1) @[L1TagAccess.scala 163:50]
    node _timeOfSetAfterValid_1_T_1 = mux(_timeOfSetAfterValid_1_T, io_timeOfSet_st1_1, UInt<1>("h0")) @[L1TagAccess.scala 163:36]
    node _io_waymask_st1_T = not(io_validOfSet) @[L1TagAccess.scala 169:77]
    node _io_waymask_st1_T_1 = bits(_io_waymask_st1_T, 0, 0) @[OneHot.scala 47:45]
    node _io_waymask_st1_T_2 = bits(_io_waymask_st1_T, 1, 1) @[OneHot.scala 47:45]
    node _io_waymask_st1_T_3 = mux(_io_waymask_st1_T_1, UInt<1>("h0"), UInt<1>("h1")) @[Mux.scala 47:70]
    node victimIdx = minTimeChooser.io_idxOfMin @[L1TagAccess.scala 157:34 166:15]
    node _io_waymask_st1_T_4 = mux(io_Set_is_full, victimIdx, _io_waymask_st1_T_3) @[L1TagAccess.scala 169:33]
    node _io_waymask_st1_T_5 = dshl(UInt<1>("h1"), _io_waymask_st1_T_4) @[OneHot.scala 57:35]
    node timeOfSetAfterValid_0 = _timeOfSetAfterValid_0_T_1 @[L1TagAccess.scala 161:35 163:30]
    node timeOfSetAfterValid_1 = _timeOfSetAfterValid_1_T_1 @[L1TagAccess.scala 161:35 163:30]
    io_waymask_st1 <= _io_waymask_st1_T_5 @[L1TagAccess.scala 169:18]
    io_Set_is_full <= _io_Set_is_full_T_1 @[L1TagAccess.scala 158:18]
    minTimeChooser.clock <= clock
    minTimeChooser.reset <= reset
    minTimeChooser.io_candidateIn_0 <= timeOfSetAfterValid_0 @[L1TagAccess.scala 165:35]
    minTimeChooser.io_candidateIn_1 <= timeOfSetAfterValid_1 @[L1TagAccess.scala 165:35]

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

    inst tagBodyAccess of SRAMTemplate @[L1TagAccess.scala 52:29]
    inst tagAccessRArb of Arbiter @[L1TagAccess.scala 64:31]
    inst timeAccess of SRAMTemplate_1 @[L1TagAccess.scala 75:26]
    inst timeAccessWArb of Arbiter_1 @[L1TagAccess.scala 88:30]
    inst iTagChecker of tagChecker @[L1TagAccess.scala 113:27]
    inst Replacement of ReplacementUnit @[L1TagAccess.scala 126:27]
    node _T = and(io_probeRead_ready, io_probeRead_valid) @[Decoupled.scala 50:35]
    node _T_1 = and(_T, io_allocateWrite_valid) @[L1TagAccess.scala 45:30]
    node _T_2 = eq(_T_1, UInt<1>("h0")) @[L1TagAccess.scala 45:10]
    node _T_3 = asUInt(reset) @[L1TagAccess.scala 45:9]
    node _T_4 = eq(_T_3, UInt<1>("h0")) @[L1TagAccess.scala 45:9]
    node _T_5 = eq(_T_2, UInt<1>("h0")) @[L1TagAccess.scala 45:9]
    node _accessFire_T = and(io_probeRead_ready, io_probeRead_valid) @[Decoupled.scala 50:35]
    node accessFire = or(_accessFire_T, io_allocateWrite_valid) @[L1TagAccess.scala 48:38]
    reg accessCount : UInt<10>, clock with :
      reset => (UInt<1>("h0"), accessCount) @[Counter.scala 62:40]
    node wrap_wrap = eq(accessCount, UInt<10>("h3e7")) @[Counter.scala 74:24]
    node _wrap_value_T = add(accessCount, UInt<1>("h1")) @[Counter.scala 78:24]
    node _wrap_value_T_1 = tail(_wrap_value_T, 1) @[Counter.scala 78:24]
    node _GEN_0 = mux(wrap_wrap, UInt<1>("h0"), _wrap_value_T_1) @[Counter.scala 78:15 88:{20,28}]
    node _GEN_1 = mux(accessFire, _GEN_0, accessCount) @[Counter.scala 120:16 62:40]
    node _GEN_2 = mux(accessFire, wrap_wrap, UInt<1>("h0")) @[Counter.scala 120:{16,23}]
    node _T_6 = and(timeAccessWArb.io_in_0_valid, timeAccessWArb.io_in_1_valid) @[L1TagAccess.scala 89:42]
    node _T_7 = eq(_T_6, UInt<1>("h0")) @[L1TagAccess.scala 89:10]
    node _T_8 = asUInt(reset) @[L1TagAccess.scala 89:9]
    node _T_9 = eq(_T_8, UInt<1>("h0")) @[L1TagAccess.scala 89:9]
    node _T_10 = eq(_T_7, UInt<1>("h0")) @[L1TagAccess.scala 89:9]
    reg REG : UInt<2>, clock with :
      reset => (UInt<1>("h0"), REG) @[L1TagAccess.scala 95:21]
    reg timeAccessWArb_io_in_1_valid_REG : UInt<1>, clock with :
      reset => (UInt<1>("h0"), timeAccessWArb_io_in_1_valid_REG) @[L1TagAccess.scala 99:43]
    reg REG_1 : UInt<2>, clock with :
      reset => (UInt<1>("h0"), REG_1) @[L1TagAccess.scala 102:21]
    reg way_valid_0_0 : UInt<1>, clock with :
      reset => (UInt<1>("h0"), way_valid_0_0) @[L1TagAccess.scala 107:26]
    reg way_valid_0_1 : UInt<1>, clock with :
      reset => (UInt<1>("h0"), way_valid_0_1) @[L1TagAccess.scala 107:26]
    reg way_valid_1_0 : UInt<1>, clock with :
      reset => (UInt<1>("h0"), way_valid_1_0) @[L1TagAccess.scala 107:26]
    reg way_valid_1_1 : UInt<1>, clock with :
      reset => (UInt<1>("h0"), way_valid_1_1) @[L1TagAccess.scala 107:26]
    reg way_valid_2_0 : UInt<1>, clock with :
      reset => (UInt<1>("h0"), way_valid_2_0) @[L1TagAccess.scala 107:26]
    reg way_valid_2_1 : UInt<1>, clock with :
      reset => (UInt<1>("h0"), way_valid_2_1) @[L1TagAccess.scala 107:26]
    reg way_valid_3_0 : UInt<1>, clock with :
      reset => (UInt<1>("h0"), way_valid_3_0) @[L1TagAccess.scala 107:26]
    reg way_valid_3_1 : UInt<1>, clock with :
      reset => (UInt<1>("h0"), way_valid_3_1) @[L1TagAccess.scala 107:26]
    reg way_dirty_0_0 : UInt<1>, clock with :
      reset => (UInt<1>("h0"), way_dirty_0_0) @[L1TagAccess.scala 109:26]
    reg way_dirty_0_1 : UInt<1>, clock with :
      reset => (UInt<1>("h0"), way_dirty_0_1) @[L1TagAccess.scala 109:26]
    reg way_dirty_1_0 : UInt<1>, clock with :
      reset => (UInt<1>("h0"), way_dirty_1_0) @[L1TagAccess.scala 109:26]
    reg way_dirty_1_1 : UInt<1>, clock with :
      reset => (UInt<1>("h0"), way_dirty_1_1) @[L1TagAccess.scala 109:26]
    reg way_dirty_2_0 : UInt<1>, clock with :
      reset => (UInt<1>("h0"), way_dirty_2_0) @[L1TagAccess.scala 109:26]
    reg way_dirty_2_1 : UInt<1>, clock with :
      reset => (UInt<1>("h0"), way_dirty_2_1) @[L1TagAccess.scala 109:26]
    reg way_dirty_3_0 : UInt<1>, clock with :
      reset => (UInt<1>("h0"), way_dirty_3_0) @[L1TagAccess.scala 109:26]
    reg way_dirty_3_1 : UInt<1>, clock with :
      reset => (UInt<1>("h0"), way_dirty_3_1) @[L1TagAccess.scala 109:26]
    node _iTagChecker_io_way_valid_T = and(io_probeRead_ready, io_probeRead_valid) @[Decoupled.scala 50:35]
    reg iTagChecker_io_way_valid_r : UInt<2>, clock with :
      reset => (UInt<1>("h0"), iTagChecker_io_way_valid_r) @[Reg.scala 16:16]
    node _GEN_3 = mux(_iTagChecker_io_way_valid_T, io_probeRead_bits_setIdx, iTagChecker_io_way_valid_r) @[Reg.scala 16:16 17:{18,22}]
    node _GEN_4 = validif(eq(UInt<1>("h0"), iTagChecker_io_way_valid_r), way_valid_0_0) @[L1TagAccess.scala 116:{28,28}]
    node _GEN_5 = mux(eq(UInt<1>("h1"), iTagChecker_io_way_valid_r), way_valid_1_0, _GEN_4) @[L1TagAccess.scala 116:{28,28}]
    node _GEN_6 = mux(eq(UInt<2>("h2"), iTagChecker_io_way_valid_r), way_valid_2_0, _GEN_5) @[L1TagAccess.scala 116:{28,28}]
    node _GEN_7 = mux(eq(UInt<2>("h3"), iTagChecker_io_way_valid_r), way_valid_3_0, _GEN_6) @[L1TagAccess.scala 116:{28,28}]
    node _GEN_8 = validif(eq(UInt<1>("h0"), iTagChecker_io_way_valid_r), way_valid_0_1) @[L1TagAccess.scala 116:{28,28}]
    node _GEN_9 = mux(eq(UInt<1>("h1"), iTagChecker_io_way_valid_r), way_valid_1_1, _GEN_8) @[L1TagAccess.scala 116:{28,28}]
    node _GEN_10 = mux(eq(UInt<2>("h2"), iTagChecker_io_way_valid_r), way_valid_2_1, _GEN_9) @[L1TagAccess.scala 116:{28,28}]
    node _GEN_11 = mux(eq(UInt<2>("h3"), iTagChecker_io_way_valid_r), way_valid_3_1, _GEN_10) @[L1TagAccess.scala 116:{28,28}]
    node _T_11 = and(iTagChecker.io_cache_hit, io_probeIsWrite_st1) @[L1TagAccess.scala 120:35]
    reg REG_2 : UInt<2>, clock with :
      reset => (UInt<1>("h0"), REG_2) @[L1TagAccess.scala 121:24]
    node _T_12 = or(REG_2, UInt<2>("h0"))
    node _T_13 = bits(_T_12, 1, 0)
    node _T_14 = bits(iTagChecker.io_waymask, 0, 0)
    node _way_dirty_T_13_T_14 = UInt<1>("h1") @[L1TagAccess.scala 121:{76,76}]
    node _GEN_12 = mux(and(eq(UInt<1>("h0"), _T_13), eq(UInt<1>("h0"), _T_14)), _way_dirty_T_13_T_14, way_dirty_0_0) @[L1TagAccess.scala 109:26 121:{76,76}]
    node _GEN_13 = mux(and(eq(UInt<1>("h0"), _T_13), eq(UInt<1>("h1"), _T_14)), _way_dirty_T_13_T_14, way_dirty_0_1) @[L1TagAccess.scala 109:26 121:{76,76}]
    node _GEN_14 = mux(and(eq(UInt<1>("h1"), _T_13), eq(UInt<1>("h0"), _T_14)), _way_dirty_T_13_T_14, way_dirty_1_0) @[L1TagAccess.scala 109:26 121:{76,76}]
    node _GEN_15 = mux(and(eq(UInt<1>("h1"), _T_13), eq(UInt<1>("h1"), _T_14)), _way_dirty_T_13_T_14, way_dirty_1_1) @[L1TagAccess.scala 109:26 121:{76,76}]
    node _GEN_16 = mux(and(eq(UInt<2>("h2"), _T_13), eq(UInt<1>("h0"), _T_14)), _way_dirty_T_13_T_14, way_dirty_2_0) @[L1TagAccess.scala 109:26 121:{76,76}]
    node _GEN_17 = mux(and(eq(UInt<2>("h2"), _T_13), eq(UInt<1>("h1"), _T_14)), _way_dirty_T_13_T_14, way_dirty_2_1) @[L1TagAccess.scala 109:26 121:{76,76}]
    node _GEN_18 = mux(and(eq(UInt<2>("h3"), _T_13), eq(UInt<1>("h0"), _T_14)), _way_dirty_T_13_T_14, way_dirty_3_0) @[L1TagAccess.scala 109:26 121:{76,76}]
    node _GEN_19 = mux(and(eq(UInt<2>("h3"), _T_13), eq(UInt<1>("h1"), _T_14)), _way_dirty_T_13_T_14, way_dirty_3_1) @[L1TagAccess.scala 109:26 121:{76,76}]
    node _GEN_20 = mux(_T_11, _GEN_12, way_dirty_0_0) @[L1TagAccess.scala 109:26 120:62]
    node _GEN_21 = mux(_T_11, _GEN_13, way_dirty_0_1) @[L1TagAccess.scala 109:26 120:62]
    node _GEN_22 = mux(_T_11, _GEN_14, way_dirty_1_0) @[L1TagAccess.scala 109:26 120:62]
    node _GEN_23 = mux(_T_11, _GEN_15, way_dirty_1_1) @[L1TagAccess.scala 109:26 120:62]
    node _GEN_24 = mux(_T_11, _GEN_16, way_dirty_2_0) @[L1TagAccess.scala 109:26 120:62]
    node _GEN_25 = mux(_T_11, _GEN_17, way_dirty_2_1) @[L1TagAccess.scala 109:26 120:62]
    node _GEN_26 = mux(_T_11, _GEN_18, way_dirty_3_0) @[L1TagAccess.scala 109:26 120:62]
    node _GEN_27 = mux(_T_11, _GEN_19, way_dirty_3_1) @[L1TagAccess.scala 109:26 120:62]
    reg allocateWrite_st1_setIdx : UInt<2>, clock with :
      reset => (UInt<1>("h0"), allocateWrite_st1_setIdx) @[Reg.scala 16:16]
    node _GEN_28 = mux(io_allocateWrite_valid, io_allocateWrite_bits_setIdx, allocateWrite_st1_setIdx) @[Reg.scala 16:16 17:{18,22}]
    node _io_needReplace_T = bits(Replacement.io_waymask_st1, 1, 1) @[CircuitMath.scala 30:8]
    node _GEN_29 = validif(and(eq(UInt<1>("h0"), allocateWrite_st1_setIdx), eq(UInt<1>("h0"), _io_needReplace_T)), way_dirty_0_0) @[L1TagAccess.scala 131:{101,101}]
    node _GEN_30 = mux(and(eq(UInt<1>("h0"), allocateWrite_st1_setIdx), eq(UInt<1>("h1"), _io_needReplace_T)), way_dirty_0_1, _GEN_29) @[L1TagAccess.scala 131:{101,101}]
    node _GEN_31 = mux(and(eq(UInt<1>("h1"), allocateWrite_st1_setIdx), eq(UInt<1>("h0"), _io_needReplace_T)), way_dirty_1_0, _GEN_30) @[L1TagAccess.scala 131:{101,101}]
    node _GEN_32 = mux(and(eq(UInt<1>("h1"), allocateWrite_st1_setIdx), eq(UInt<1>("h1"), _io_needReplace_T)), way_dirty_1_1, _GEN_31) @[L1TagAccess.scala 131:{101,101}]
    node _GEN_33 = mux(and(eq(UInt<2>("h2"), allocateWrite_st1_setIdx), eq(UInt<1>("h0"), _io_needReplace_T)), way_dirty_2_0, _GEN_32) @[L1TagAccess.scala 131:{101,101}]
    node _GEN_34 = mux(and(eq(UInt<2>("h2"), allocateWrite_st1_setIdx), eq(UInt<1>("h1"), _io_needReplace_T)), way_dirty_2_1, _GEN_33) @[L1TagAccess.scala 131:{101,101}]
    node _GEN_35 = mux(and(eq(UInt<2>("h3"), allocateWrite_st1_setIdx), eq(UInt<1>("h0"), _io_needReplace_T)), way_dirty_3_0, _GEN_34) @[L1TagAccess.scala 131:{101,101}]
    node _GEN_36 = mux(and(eq(UInt<2>("h3"), allocateWrite_st1_setIdx), eq(UInt<1>("h1"), _io_needReplace_T)), way_dirty_3_1, _GEN_35) @[L1TagAccess.scala 131:{101,101}]
    node _way_dirty_allocateWrite_st1_setIdx_io_needReplace_T = _GEN_36 @[L1TagAccess.scala 131:101]
    node _io_needReplace_T_1 = bits(_way_dirty_allocateWrite_st1_setIdx_io_needReplace_T, 0, 0) @[L1TagAccess.scala 131:101]
    node _GEN_37 = validif(eq(UInt<1>("h0"), io_allocateWrite_bits_setIdx), way_valid_0_0) @[Cat.scala 31:{58,58}]
    node _GEN_38 = mux(eq(UInt<1>("h1"), io_allocateWrite_bits_setIdx), way_valid_1_0, _GEN_37) @[Cat.scala 31:{58,58}]
    node _GEN_39 = mux(eq(UInt<2>("h2"), io_allocateWrite_bits_setIdx), way_valid_2_0, _GEN_38) @[Cat.scala 31:{58,58}]
    node _GEN_40 = mux(eq(UInt<2>("h3"), io_allocateWrite_bits_setIdx), way_valid_3_0, _GEN_39) @[Cat.scala 31:{58,58}]
    node _GEN_41 = validif(eq(UInt<1>("h0"), io_allocateWrite_bits_setIdx), way_valid_0_1) @[Cat.scala 31:{58,58}]
    node _GEN_42 = mux(eq(UInt<1>("h1"), io_allocateWrite_bits_setIdx), way_valid_1_1, _GEN_41) @[Cat.scala 31:{58,58}]
    node _GEN_43 = mux(eq(UInt<2>("h2"), io_allocateWrite_bits_setIdx), way_valid_2_1, _GEN_42) @[Cat.scala 31:{58,58}]
    node _GEN_44 = mux(eq(UInt<2>("h3"), io_allocateWrite_bits_setIdx), way_valid_3_1, _GEN_43) @[Cat.scala 31:{58,58}]
    node _way_valid_io_allocateWrite_bits_setIdx_0 = _GEN_40 @[Cat.scala 31:58]
    node _way_valid_io_allocateWrite_bits_setIdx_1 = _GEN_44 @[Cat.scala 31:58]
    node _Replacement_io_validOfSet_T = cat(_way_valid_io_allocateWrite_bits_setIdx_0, _way_valid_io_allocateWrite_bits_setIdx_1) @[Cat.scala 31:58]
    node _io_a_addrReplacement_st1_T = bits(Replacement.io_waymask_st1, 0, 0)
    node _GEN_45 = validif(eq(UInt<1>("h0"), _io_a_addrReplacement_st1_T), tagBodyAccess.io_r_resp_data_0) @[Cat.scala 31:{58,58}]
    node _GEN_46 = mux(eq(UInt<1>("h1"), _io_a_addrReplacement_st1_T), tagBodyAccess.io_r_resp_data_1, _GEN_45) @[Cat.scala 31:{58,58}]
    node _tagBodyAccess_io_r_resp_data_io_a_addrReplacement_st1_T = _GEN_46 @[Cat.scala 31:58]
    node _io_a_addrReplacement_st1_T_1 = cat(_tagBodyAccess_io_r_resp_data_io_a_addrReplacement_st1_T, allocateWrite_st1_setIdx) @[Cat.scala 31:58]
    node _io_a_addrReplacement_st1_T_2 = cat(_io_a_addrReplacement_st1_T_1, UInt<4>("h0")) @[Cat.scala 31:58]
    reg REG_3 : UInt<1>, clock with :
      reset => (UInt<1>("h0"), REG_3) @[L1TagAccess.scala 144:15]
    node _T_15 = eq(Replacement.io_Set_is_full, UInt<1>("h0")) @[L1TagAccess.scala 144:42]
    node _T_16 = and(REG_3, _T_15) @[L1TagAccess.scala 144:39]
    node _T_17 = bits(Replacement.io_waymask_st1, 1, 1) @[CircuitMath.scala 30:8]
    node _way_valid_allocateWrite_st1_setIdx_T_17 = UInt<1>("h1") @[L1TagAccess.scala 145:{79,79}]
    node _GEN_47 = mux(and(eq(UInt<1>("h0"), allocateWrite_st1_setIdx), eq(UInt<1>("h0"), _T_17)), _way_valid_allocateWrite_st1_setIdx_T_17, way_valid_0_0) @[L1TagAccess.scala 107:26 145:{79,79}]
    node _GEN_48 = mux(and(eq(UInt<1>("h0"), allocateWrite_st1_setIdx), eq(UInt<1>("h1"), _T_17)), _way_valid_allocateWrite_st1_setIdx_T_17, way_valid_0_1) @[L1TagAccess.scala 107:26 145:{79,79}]
    node _GEN_49 = mux(and(eq(UInt<1>("h1"), allocateWrite_st1_setIdx), eq(UInt<1>("h0"), _T_17)), _way_valid_allocateWrite_st1_setIdx_T_17, way_valid_1_0) @[L1TagAccess.scala 107:26 145:{79,79}]
    node _GEN_50 = mux(and(eq(UInt<1>("h1"), allocateWrite_st1_setIdx), eq(UInt<1>("h1"), _T_17)), _way_valid_allocateWrite_st1_setIdx_T_17, way_valid_1_1) @[L1TagAccess.scala 107:26 145:{79,79}]
    node _GEN_51 = mux(and(eq(UInt<2>("h2"), allocateWrite_st1_setIdx), eq(UInt<1>("h0"), _T_17)), _way_valid_allocateWrite_st1_setIdx_T_17, way_valid_2_0) @[L1TagAccess.scala 107:26 145:{79,79}]
    node _GEN_52 = mux(and(eq(UInt<2>("h2"), allocateWrite_st1_setIdx), eq(UInt<1>("h1"), _T_17)), _way_valid_allocateWrite_st1_setIdx_T_17, way_valid_2_1) @[L1TagAccess.scala 107:26 145:{79,79}]
    node _GEN_53 = mux(and(eq(UInt<2>("h3"), allocateWrite_st1_setIdx), eq(UInt<1>("h0"), _T_17)), _way_valid_allocateWrite_st1_setIdx_T_17, way_valid_3_0) @[L1TagAccess.scala 107:26 145:{79,79}]
    node _GEN_54 = mux(and(eq(UInt<2>("h3"), allocateWrite_st1_setIdx), eq(UInt<1>("h1"), _T_17)), _way_valid_allocateWrite_st1_setIdx_T_17, way_valid_3_1) @[L1TagAccess.scala 107:26 145:{79,79}]
    node _GEN_55 = mux(_T_16, _GEN_47, way_valid_0_0) @[L1TagAccess.scala 107:26 144:70]
    node _GEN_56 = mux(_T_16, _GEN_48, way_valid_0_1) @[L1TagAccess.scala 107:26 144:70]
    node _GEN_57 = mux(_T_16, _GEN_49, way_valid_1_0) @[L1TagAccess.scala 107:26 144:70]
    node _GEN_58 = mux(_T_16, _GEN_50, way_valid_1_1) @[L1TagAccess.scala 107:26 144:70]
    node _GEN_59 = mux(_T_16, _GEN_51, way_valid_2_0) @[L1TagAccess.scala 107:26 144:70]
    node _GEN_60 = mux(_T_16, _GEN_52, way_valid_2_1) @[L1TagAccess.scala 107:26 144:70]
    node _GEN_61 = mux(_T_16, _GEN_53, way_valid_3_0) @[L1TagAccess.scala 107:26 144:70]
    node _GEN_62 = mux(_T_16, _GEN_54, way_valid_3_1) @[L1TagAccess.scala 107:26 144:70]
    node accessCounterFull = _GEN_2
    node _WIRE__0 = accessCount @[SRAMTemplate.scala 54:{18,18}]
    node _WIRE__1 = accessCount @[SRAMTemplate.scala 54:{18,18}]
    node _WIRE_1_0 = accessCount @[SRAMTemplate.scala 54:{18,18}]
    node _WIRE_1_1 = accessCount @[SRAMTemplate.scala 54:{18,18}]
    node _way_valid_WIRE__0 = UInt<1>("h0") @[L1TagAccess.scala 107:{56,56}]
    node _way_valid_WIRE__1 = UInt<1>("h0") @[L1TagAccess.scala 107:{56,56}]
    node _way_valid_WIRE_1_0 = UInt<1>("h0") @[L1TagAccess.scala 107:{56,56}]
    node _way_valid_WIRE_1_1 = UInt<1>("h0") @[L1TagAccess.scala 107:{56,56}]
    node _way_valid_WIRE_2_0 = UInt<1>("h0") @[L1TagAccess.scala 107:{56,56}]
    node _way_valid_WIRE_2_1 = UInt<1>("h0") @[L1TagAccess.scala 107:{56,56}]
    node _way_valid_WIRE_3_0 = UInt<1>("h0") @[L1TagAccess.scala 107:{56,56}]
    node _way_valid_WIRE_3_1 = UInt<1>("h0") @[L1TagAccess.scala 107:{56,56}]
    node _way_valid_WIRE_4_0_0 = _way_valid_WIRE__0 @[L1TagAccess.scala 107:{34,34}]
    node _way_valid_WIRE_4_0_1 = _way_valid_WIRE__1 @[L1TagAccess.scala 107:{34,34}]
    node _way_valid_WIRE_4_1_0 = _way_valid_WIRE_1_0 @[L1TagAccess.scala 107:{34,34}]
    node _way_valid_WIRE_4_1_1 = _way_valid_WIRE_1_1 @[L1TagAccess.scala 107:{34,34}]
    node _way_valid_WIRE_4_2_0 = _way_valid_WIRE_2_0 @[L1TagAccess.scala 107:{34,34}]
    node _way_valid_WIRE_4_2_1 = _way_valid_WIRE_2_1 @[L1TagAccess.scala 107:{34,34}]
    node _way_valid_WIRE_4_3_0 = _way_valid_WIRE_3_0 @[L1TagAccess.scala 107:{34,34}]
    node _way_valid_WIRE_4_3_1 = _way_valid_WIRE_3_1 @[L1TagAccess.scala 107:{34,34}]
    node _way_dirty_WIRE__0 = UInt<1>("h0") @[L1TagAccess.scala 109:{56,56}]
    node _way_dirty_WIRE__1 = UInt<1>("h0") @[L1TagAccess.scala 109:{56,56}]
    node _way_dirty_WIRE_1_0 = UInt<1>("h0") @[L1TagAccess.scala 109:{56,56}]
    node _way_dirty_WIRE_1_1 = UInt<1>("h0") @[L1TagAccess.scala 109:{56,56}]
    node _way_dirty_WIRE_2_0 = UInt<1>("h0") @[L1TagAccess.scala 109:{56,56}]
    node _way_dirty_WIRE_2_1 = UInt<1>("h0") @[L1TagAccess.scala 109:{56,56}]
    node _way_dirty_WIRE_3_0 = UInt<1>("h0") @[L1TagAccess.scala 109:{56,56}]
    node _way_dirty_WIRE_3_1 = UInt<1>("h0") @[L1TagAccess.scala 109:{56,56}]
    node _way_dirty_WIRE_4_0_0 = _way_dirty_WIRE__0 @[L1TagAccess.scala 109:{34,34}]
    node _way_dirty_WIRE_4_0_1 = _way_dirty_WIRE__1 @[L1TagAccess.scala 109:{34,34}]
    node _way_dirty_WIRE_4_1_0 = _way_dirty_WIRE_1_0 @[L1TagAccess.scala 109:{34,34}]
    node _way_dirty_WIRE_4_1_1 = _way_dirty_WIRE_1_1 @[L1TagAccess.scala 109:{34,34}]
    node _way_dirty_WIRE_4_2_0 = _way_dirty_WIRE_2_0 @[L1TagAccess.scala 109:{34,34}]
    node _way_dirty_WIRE_4_2_1 = _way_dirty_WIRE_2_1 @[L1TagAccess.scala 109:{34,34}]
    node _way_dirty_WIRE_4_3_0 = _way_dirty_WIRE_3_0 @[L1TagAccess.scala 109:{34,34}]
    node _way_dirty_WIRE_4_3_1 = _way_dirty_WIRE_3_1 @[L1TagAccess.scala 109:{34,34}]
    node _way_valid_iTagChecker_io_way_valid_r_0 = _GEN_7 @[L1TagAccess.scala 116:28]
    node _way_valid_iTagChecker_io_way_valid_r_1 = _GEN_11 @[L1TagAccess.scala 116:28]
    node _WIRE_2_0 = io_allocateWriteData_st1 @[SRAMTemplate.scala 54:{18,18}]
    node _WIRE_2_1 = io_allocateWriteData_st1 @[SRAMTemplate.scala 54:{18,18}]
    io_probeRead_ready <= tagAccessRArb.io_in_1_ready @[L1TagAccess.scala 66:28]
    io_hit_st1 <= iTagChecker.io_cache_hit @[L1TagAccess.scala 118:14]
    io_waymaskHit_st1 <= iTagChecker.io_waymask @[L1TagAccess.scala 117:21]
    io_needReplace <= _io_needReplace_T_1 @[L1TagAccess.scala 131:24]
    io_waymaskReplacement_st1 <= Replacement.io_waymask_st1 @[L1TagAccess.scala 136:29]
    io_a_addrReplacement_st1 <= bits(_io_a_addrReplacement_st1_T_2, 25, 0) @[L1TagAccess.scala 138:34]
    accessCount <= mux(reset, UInt<10>("h0"), _GEN_1) @[Counter.scala 62:{40,40}]
    tagBodyAccess.clock <= clock
    tagBodyAccess.reset <= reset
    tagBodyAccess.io_r_req_valid <= tagAccessRArb.io_out_valid @[L1TagAccess.scala 65:28]
    tagBodyAccess.io_r_req_bits_setIdx <= tagAccessRArb.io_out_bits_setIdx @[L1TagAccess.scala 65:28]
    tagBodyAccess.io_w_req_valid <= io_allocateWriteTagSRAMWValid_st1 @[L1TagAccess.scala 142:32]
    tagBodyAccess.io_w_req_bits_setIdx <= allocateWrite_st1_setIdx @[SRAMTemplate.scala 37:17]
    tagBodyAccess.io_w_req_bits_data_0 <= _WIRE_2_0 @[SRAMTemplate.scala 48:15]
    tagBodyAccess.io_w_req_bits_data_1 <= _WIRE_2_1 @[SRAMTemplate.scala 48:15]
    tagBodyAccess.io_w_req_bits_waymask <= Replacement.io_waymask_st1 @[SRAMTemplate.scala 49:24]
    tagAccessRArb.clock <= clock
    tagAccessRArb.reset <= reset
    tagAccessRArb.io_in_0_valid <= io_allocateWrite_valid @[L1TagAccess.scala 67:34]
    tagAccessRArb.io_in_0_bits_setIdx <= io_allocateWrite_bits_setIdx @[L1TagAccess.scala 68:40]
    tagAccessRArb.io_in_1_valid <= io_probeRead_valid @[L1TagAccess.scala 66:28]
    tagAccessRArb.io_in_1_bits_setIdx <= io_probeRead_bits_setIdx @[L1TagAccess.scala 66:28]
    tagAccessRArb.io_out_ready <= tagBodyAccess.io_r_req_ready @[L1TagAccess.scala 65:28]
    timeAccess.clock <= clock
    timeAccess.reset <= reset
    timeAccess.io_r_req_valid <= io_allocateWrite_valid @[L1TagAccess.scala 84:29]
    timeAccess.io_r_req_bits_setIdx <= io_allocateWrite_bits_setIdx @[L1TagAccess.scala 85:35]
    timeAccess.io_w_req_valid <= timeAccessWArb.io_out_valid @[L1TagAccess.scala 105:23]
    timeAccess.io_w_req_bits_setIdx <= timeAccessWArb.io_out_bits_setIdx @[L1TagAccess.scala 105:23]
    timeAccess.io_w_req_bits_data_0 <= timeAccessWArb.io_out_bits_data_0 @[L1TagAccess.scala 105:23]
    timeAccess.io_w_req_bits_data_1 <= timeAccessWArb.io_out_bits_data_1 @[L1TagAccess.scala 105:23]
    timeAccess.io_w_req_bits_waymask <= timeAccessWArb.io_out_bits_waymask @[L1TagAccess.scala 105:23]
    timeAccessWArb.clock <= clock
    timeAccessWArb.reset <= reset
    timeAccessWArb.io_in_0_valid <= io_hit_st1 @[L1TagAccess.scala 92:33]
    timeAccessWArb.io_in_0_bits_setIdx <= REG @[SRAMTemplate.scala 37:17]
    timeAccessWArb.io_in_0_bits_data_0 <= _WIRE__0 @[SRAMTemplate.scala 48:15]
    timeAccessWArb.io_in_0_bits_data_1 <= _WIRE__1 @[SRAMTemplate.scala 48:15]
    timeAccessWArb.io_in_0_bits_waymask <= io_waymaskHit_st1 @[SRAMTemplate.scala 49:24]
    timeAccessWArb.io_in_1_valid <= timeAccessWArb_io_in_1_valid_REG @[L1TagAccess.scala 99:33]
    timeAccessWArb.io_in_1_bits_setIdx <= REG_1 @[SRAMTemplate.scala 37:17]
    timeAccessWArb.io_in_1_bits_data_0 <= _WIRE_1_0 @[SRAMTemplate.scala 48:15]
    timeAccessWArb.io_in_1_bits_data_1 <= _WIRE_1_1 @[SRAMTemplate.scala 48:15]
    timeAccessWArb.io_in_1_bits_waymask <= io_waymaskReplacement_st1 @[SRAMTemplate.scala 49:24]
    timeAccessWArb.io_out_ready <= timeAccess.io_w_req_ready @[L1TagAccess.scala 105:23]
    REG <= io_probeRead_bits_setIdx @[L1TagAccess.scala 95:21]
    timeAccessWArb_io_in_1_valid_REG <= io_allocateWrite_valid @[L1TagAccess.scala 99:43]
    REG_1 <= io_allocateWrite_bits_setIdx @[L1TagAccess.scala 102:21]
    way_valid_0_0 <= mux(reset, _way_valid_WIRE_4_0_0, _GEN_55) @[L1TagAccess.scala 107:{26,26}]
    way_valid_0_1 <= mux(reset, _way_valid_WIRE_4_0_1, _GEN_56) @[L1TagAccess.scala 107:{26,26}]
    way_valid_1_0 <= mux(reset, _way_valid_WIRE_4_1_0, _GEN_57) @[L1TagAccess.scala 107:{26,26}]
    way_valid_1_1 <= mux(reset, _way_valid_WIRE_4_1_1, _GEN_58) @[L1TagAccess.scala 107:{26,26}]
    way_valid_2_0 <= mux(reset, _way_valid_WIRE_4_2_0, _GEN_59) @[L1TagAccess.scala 107:{26,26}]
    way_valid_2_1 <= mux(reset, _way_valid_WIRE_4_2_1, _GEN_60) @[L1TagAccess.scala 107:{26,26}]
    way_valid_3_0 <= mux(reset, _way_valid_WIRE_4_3_0, _GEN_61) @[L1TagAccess.scala 107:{26,26}]
    way_valid_3_1 <= mux(reset, _way_valid_WIRE_4_3_1, _GEN_62) @[L1TagAccess.scala 107:{26,26}]
    way_dirty_0_0 <= mux(reset, _way_dirty_WIRE_4_0_0, _GEN_20) @[L1TagAccess.scala 109:{26,26}]
    way_dirty_0_1 <= mux(reset, _way_dirty_WIRE_4_0_1, _GEN_21) @[L1TagAccess.scala 109:{26,26}]
    way_dirty_1_0 <= mux(reset, _way_dirty_WIRE_4_1_0, _GEN_22) @[L1TagAccess.scala 109:{26,26}]
    way_dirty_1_1 <= mux(reset, _way_dirty_WIRE_4_1_1, _GEN_23) @[L1TagAccess.scala 109:{26,26}]
    way_dirty_2_0 <= mux(reset, _way_dirty_WIRE_4_2_0, _GEN_24) @[L1TagAccess.scala 109:{26,26}]
    way_dirty_2_1 <= mux(reset, _way_dirty_WIRE_4_2_1, _GEN_25) @[L1TagAccess.scala 109:{26,26}]
    way_dirty_3_0 <= mux(reset, _way_dirty_WIRE_4_3_0, _GEN_26) @[L1TagAccess.scala 109:{26,26}]
    way_dirty_3_1 <= mux(reset, _way_dirty_WIRE_4_3_1, _GEN_27) @[L1TagAccess.scala 109:{26,26}]
    iTagChecker.clock <= clock
    iTagChecker.reset <= reset
    iTagChecker.io_tag_of_set_0 <= tagBodyAccess.io_r_resp_data_0 @[L1TagAccess.scala 114:29]
    iTagChecker.io_tag_of_set_1 <= tagBodyAccess.io_r_resp_data_1 @[L1TagAccess.scala 114:29]
    iTagChecker.io_tag_from_pipe <= io_tagFromCore_st1 @[L1TagAccess.scala 115:32]
    iTagChecker.io_way_valid_0 <= _way_valid_iTagChecker_io_way_valid_r_0 @[L1TagAccess.scala 116:28]
    iTagChecker.io_way_valid_1 <= _way_valid_iTagChecker_io_way_valid_r_1 @[L1TagAccess.scala 116:28]
    iTagChecker_io_way_valid_r <= _GEN_3
    REG_2 <= io_probeRead_bits_setIdx @[L1TagAccess.scala 121:24]
    Replacement.clock <= clock
    Replacement.reset <= reset
    Replacement.io_validOfSet <= _Replacement_io_validOfSet_T @[L1TagAccess.scala 134:29]
    Replacement.io_timeOfSet_st1_0 <= timeAccess.io_r_resp_data_0 @[L1TagAccess.scala 135:32]
    Replacement.io_timeOfSet_st1_1 <= timeAccess.io_r_resp_data_1 @[L1TagAccess.scala 135:32]
    allocateWrite_st1_setIdx <= _GEN_28
    REG_3 <= io_allocateWrite_valid @[L1TagAccess.scala 144:15]
    printf(clock, and(and(and(UInt<1>("h1"), _T_4), _T_5), UInt<1>("h1")), "Assertion failed: tag probe and allocate in same cycle\n    at L1TagAccess.scala:45 assert(!(io.probeRead.fire && io.allocateWrite.fire), s\"tag probe and allocate in same cycle\")\n") : printf @[L1TagAccess.scala 45:9]
    assert(clock, _T_2, and(and(UInt<1>("h1"), _T_4), UInt<1>("h1")), "") : assert @[L1TagAccess.scala 45:9]
    printf(clock, and(and(and(UInt<1>("h1"), _T_9), _T_10), UInt<1>("h1")), "Assertion failed: tag probe and allocate in same cycle\n    at L1TagAccess.scala:89 assert(!(timeAccessWArb.io.in(0).valid && timeAccessWArb.io.in(1).valid), s\"tag probe and allocate in same cycle\")\n") : printf_1 @[L1TagAccess.scala 89:9]
    assert(clock, _T_7, and(and(UInt<1>("h1"), _T_9), UInt<1>("h1")), "") : assert_1 @[L1TagAccess.scala 89:9]

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
    output io_chosen : UInt<1>

    node _GEN_0 = mux(io_in_0_valid, UInt<1>("h0"), UInt<1>("h1")) @[Arbiter.scala 138:13 141:26 142:17]
    node _GEN_1 = mux(io_in_0_valid, io_in_0_bits_a_opcode, io_in_1_bits_a_opcode) @[Arbiter.scala 139:15 141:26 143:19]
    node _GEN_2 = mux(io_in_0_valid, io_in_0_bits_a_param, io_in_1_bits_a_param) @[Arbiter.scala 139:15 141:26 143:19]
    node _GEN_3 = mux(io_in_0_valid, io_in_0_bits_a_addr, io_in_1_bits_a_addr) @[Arbiter.scala 139:15 141:26 143:19]
    node _GEN_4 = mux(io_in_0_valid, io_in_0_bits_a_data_0, io_in_1_bits_a_data_0) @[Arbiter.scala 139:15 141:26 143:19]
    node _GEN_5 = mux(io_in_0_valid, io_in_0_bits_a_data_1, io_in_1_bits_a_data_1) @[Arbiter.scala 139:15 141:26 143:19]
    node _GEN_6 = mux(io_in_0_valid, io_in_0_bits_a_data_2, io_in_1_bits_a_data_2) @[Arbiter.scala 139:15 141:26 143:19]
    node _GEN_7 = mux(io_in_0_valid, io_in_0_bits_a_data_3, io_in_1_bits_a_data_3) @[Arbiter.scala 139:15 141:26 143:19]
    node _GEN_8 = mux(io_in_0_valid, io_in_0_bits_a_mask_0, io_in_1_bits_a_mask_0) @[Arbiter.scala 139:15 141:26 143:19]
    node _GEN_9 = mux(io_in_0_valid, io_in_0_bits_a_mask_1, io_in_1_bits_a_mask_1) @[Arbiter.scala 139:15 141:26 143:19]
    node _GEN_10 = mux(io_in_0_valid, io_in_0_bits_a_mask_2, io_in_1_bits_a_mask_2) @[Arbiter.scala 139:15 141:26 143:19]
    node _GEN_11 = mux(io_in_0_valid, io_in_0_bits_a_mask_3, io_in_1_bits_a_mask_3) @[Arbiter.scala 139:15 141:26 143:19]
    node _GEN_12 = mux(io_in_0_valid, io_in_0_bits_a_source, io_in_1_bits_a_source) @[Arbiter.scala 139:15 141:26 143:19]
    node _GEN_13 = mux(io_in_0_valid, io_in_0_bits_hasCoreRsp, io_in_1_bits_hasCoreRsp) @[Arbiter.scala 139:15 141:26 143:19]
    node _GEN_14 = mux(io_in_0_valid, io_in_0_bits_coreRspInstrId, io_in_1_bits_coreRspInstrId) @[Arbiter.scala 139:15 141:26 143:19]
    node grant_1 = eq(io_in_0_valid, UInt<1>("h0")) @[Arbiter.scala 46:78]
    node _io_in_0_ready_T = and(UInt<1>("h1"), io_out_ready) @[Arbiter.scala 149:19]
    node _io_in_1_ready_T = and(grant_1, io_out_ready) @[Arbiter.scala 149:19]
    node _io_out_valid_T = eq(grant_1, UInt<1>("h0")) @[Arbiter.scala 150:19]
    node _io_out_valid_T_1 = or(_io_out_valid_T, io_in_1_valid) @[Arbiter.scala 150:31]
    io_in_0_ready <= _io_in_0_ready_T @[Arbiter.scala 149:14]
    io_in_1_ready <= _io_in_1_ready_T @[Arbiter.scala 149:14]
    io_out_valid <= _io_out_valid_T_1 @[Arbiter.scala 150:16]
    io_out_bits_a_opcode <= _GEN_1
    io_out_bits_a_param <= _GEN_2
    io_out_bits_a_addr <= _GEN_3
    io_out_bits_a_data_0 <= _GEN_4
    io_out_bits_a_data_1 <= _GEN_5
    io_out_bits_a_data_2 <= _GEN_6
    io_out_bits_a_data_3 <= _GEN_7
    io_out_bits_a_mask_0 <= _GEN_8
    io_out_bits_a_mask_1 <= _GEN_9
    io_out_bits_a_mask_2 <= _GEN_10
    io_out_bits_a_mask_3 <= _GEN_11
    io_out_bits_a_source <= _GEN_12
    io_out_bits_hasCoreRsp <= _GEN_13
    io_out_bits_coreRspInstrId <= _GEN_14
    io_chosen <= _GEN_0

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

    node _T = eq(UInt<1>("h0"), io_opcode) @[DCache.scala 51:20]
    node _T_1 = eq(io_param, UInt<1>("h0")) @[DCache.scala 53:21]
    node _T_2 = eq(io_param, UInt<1>("h1")) @[DCache.scala 55:27]
    node _GEN_0 = mux(_T_2, UInt<1>("h1"), UInt<1>("h0")) @[DCache.scala 45:19 55:36 56:25]
    node _GEN_1 = mux(_T_1, UInt<1>("h1"), UInt<1>("h0")) @[DCache.scala 43:21 53:29 54:27]
    node _GEN_2 = mux(_T_1, UInt<1>("h0"), _GEN_0) @[DCache.scala 45:19 53:29]
    node _T_3 = eq(UInt<1>("h1"), io_opcode) @[DCache.scala 51:20]
    node _T_4 = eq(io_param, UInt<1>("h0")) @[DCache.scala 60:21]
    node _T_5 = eq(io_param, UInt<1>("h1")) @[DCache.scala 62:27]
    node _GEN_3 = mux(_T_5, UInt<1>("h1"), UInt<1>("h0")) @[DCache.scala 46:19 62:36 63:25]
    node _GEN_4 = mux(_T_4, UInt<1>("h1"), UInt<1>("h0")) @[DCache.scala 44:22 60:30 61:28]
    node _GEN_5 = mux(_T_4, UInt<1>("h0"), _GEN_3) @[DCache.scala 46:19 60:30]
    node _T_6 = eq(UInt<2>("h2"), io_opcode) @[DCache.scala 51:20]
    node _T_7 = eq(UInt<2>("h3"), io_opcode) @[DCache.scala 51:20]
    node _T_8 = eq(io_param, UInt<1>("h0")) @[DCache.scala 70:21]
    node _T_9 = eq(io_param, UInt<1>("h1")) @[DCache.scala 72:27]
    node _GEN_6 = mux(_T_9, UInt<1>("h1"), UInt<1>("h0")) @[DCache.scala 48:22 72:36 73:28]
    node _GEN_7 = mux(_T_8, UInt<1>("h1"), UInt<1>("h0")) @[DCache.scala 49:27 70:30 71:33]
    node _GEN_8 = mux(_T_8, UInt<1>("h0"), _GEN_6) @[DCache.scala 48:22 70:30]
    node _GEN_9 = mux(_T_7, _GEN_7, UInt<1>("h0")) @[DCache.scala 51:20 49:27]
    node _GEN_10 = mux(_T_7, _GEN_8, UInt<1>("h0")) @[DCache.scala 51:20 48:22]
    node _GEN_11 = mux(_T_6, UInt<1>("h1"), UInt<1>("h0")) @[DCache.scala 47:20 51:20 67:24]
    node _GEN_12 = mux(_T_6, UInt<1>("h0"), _GEN_9) @[DCache.scala 51:20 49:27]
    node _GEN_13 = mux(_T_6, UInt<1>("h0"), _GEN_10) @[DCache.scala 51:20 48:22]
    node _GEN_14 = mux(_T_3, _GEN_4, UInt<1>("h0")) @[DCache.scala 51:20 44:22]
    node _GEN_15 = mux(_T_3, _GEN_5, UInt<1>("h0")) @[DCache.scala 46:19 51:20]
    node _GEN_16 = mux(_T_3, UInt<1>("h0"), _GEN_11) @[DCache.scala 47:20 51:20]
    node _GEN_17 = mux(_T_3, UInt<1>("h0"), _GEN_12) @[DCache.scala 51:20 49:27]
    node _GEN_18 = mux(_T_3, UInt<1>("h0"), _GEN_13) @[DCache.scala 51:20 48:22]
    node _GEN_19 = mux(_T, _GEN_1, UInt<1>("h0")) @[DCache.scala 51:20 43:21]
    node _GEN_20 = mux(_T, _GEN_2, UInt<1>("h0")) @[DCache.scala 45:19 51:20]
    node _GEN_21 = mux(_T, UInt<1>("h0"), _GEN_14) @[DCache.scala 51:20 44:22]
    node _GEN_22 = mux(_T, UInt<1>("h0"), _GEN_15) @[DCache.scala 46:19 51:20]
    node _GEN_23 = mux(_T, UInt<1>("h0"), _GEN_16) @[DCache.scala 47:20 51:20]
    node _GEN_24 = mux(_T, UInt<1>("h0"), _GEN_17) @[DCache.scala 51:20 49:27]
    node _GEN_25 = mux(_T, UInt<1>("h0"), _GEN_18) @[DCache.scala 51:20 48:22]
    io_control_isRead <= _GEN_19
    io_control_isWrite <= _GEN_21
    io_control_isLR <= _GEN_20
    io_control_isSC <= _GEN_22
    io_control_isAMO <= _GEN_23
    io_control_isFlush <= _GEN_25
    io_control_isInvalidate <= _GEN_24

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
    node blockIdx1H_0 = _blockIdx1H_0_T @[DCache.scala 538:24 541:19]
    node _blockIdxMasked_0_T_2 = and(blockIdx1H_0, _blockIdxMasked_0_T_1) @[DCache.scala 542:40]
    node _blockIdx1H_1_T = dshl(UInt<1>("h1"), io_perLaneBlockIdx_1) @[OneHot.scala 57:35]
    node _blockIdxMasked_1_T = bits(io_perLaneValid_1, 0, 0) @[Bitwise.scala 74:15]
    node _blockIdxMasked_1_T_1 = mux(_blockIdxMasked_1_T, UInt<4>("hf"), UInt<4>("h0")) @[Bitwise.scala 74:12]
    node blockIdx1H_1 = _blockIdx1H_1_T @[DCache.scala 538:24 541:19]
    node _blockIdxMasked_1_T_2 = and(blockIdx1H_1, _blockIdxMasked_1_T_1) @[DCache.scala 542:40]
    node _blockIdx1H_2_T = dshl(UInt<1>("h1"), io_perLaneBlockIdx_2) @[OneHot.scala 57:35]
    node _blockIdxMasked_2_T = bits(io_perLaneValid_2, 0, 0) @[Bitwise.scala 74:15]
    node _blockIdxMasked_2_T_1 = mux(_blockIdxMasked_2_T, UInt<4>("hf"), UInt<4>("h0")) @[Bitwise.scala 74:12]
    node blockIdx1H_2 = _blockIdx1H_2_T @[DCache.scala 538:24 541:19]
    node _blockIdxMasked_2_T_2 = and(blockIdx1H_2, _blockIdxMasked_2_T_1) @[DCache.scala 542:40]
    node _blockIdx1H_3_T = dshl(UInt<1>("h1"), io_perLaneBlockIdx_3) @[OneHot.scala 57:35]
    node _blockIdxMasked_3_T = bits(io_perLaneValid_3, 0, 0) @[Bitwise.scala 74:15]
    node _blockIdxMasked_3_T_1 = mux(_blockIdxMasked_3_T, UInt<4>("hf"), UInt<4>("h0")) @[Bitwise.scala 74:12]
    node blockIdx1H_3 = _blockIdx1H_3_T @[DCache.scala 538:24 541:19]
    node _blockIdxMasked_3_T_2 = and(blockIdx1H_3, _blockIdxMasked_3_T_1) @[DCache.scala 542:40]
    node blockIdxMasked_0 = _blockIdxMasked_0_T_2 @[DCache.scala 539:28 542:23]
    node _perBankReq_Bin_0_T = bits(blockIdxMasked_0, 0, 0) @[DCache.scala 546:58]
    node blockIdxMasked_1 = _blockIdxMasked_1_T_2 @[DCache.scala 539:28 542:23]
    node _perBankReq_Bin_0_T_1 = bits(blockIdxMasked_1, 0, 0) @[DCache.scala 546:58]
    node blockIdxMasked_2 = _blockIdxMasked_2_T_2 @[DCache.scala 539:28 542:23]
    node _perBankReq_Bin_0_T_2 = bits(blockIdxMasked_2, 0, 0) @[DCache.scala 546:58]
    node blockIdxMasked_3 = _blockIdxMasked_3_T_2 @[DCache.scala 539:28 542:23]
    node _perBankReq_Bin_0_T_3 = bits(blockIdxMasked_3, 0, 0) @[DCache.scala 546:58]
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
    node _perBankReq_Bin_1_T = bits(blockIdxMasked_0, 1, 1) @[DCache.scala 546:58]
    node _perBankReq_Bin_1_T_1 = bits(blockIdxMasked_1, 1, 1) @[DCache.scala 546:58]
    node _perBankReq_Bin_1_T_2 = bits(blockIdxMasked_2, 1, 1) @[DCache.scala 546:58]
    node _perBankReq_Bin_1_T_3 = bits(blockIdxMasked_3, 1, 1) @[DCache.scala 546:58]
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
    node _perBankReq_Bin_2_T = bits(blockIdxMasked_0, 2, 2) @[DCache.scala 546:58]
    node _perBankReq_Bin_2_T_1 = bits(blockIdxMasked_1, 2, 2) @[DCache.scala 546:58]
    node _perBankReq_Bin_2_T_2 = bits(blockIdxMasked_2, 2, 2) @[DCache.scala 546:58]
    node _perBankReq_Bin_2_T_3 = bits(blockIdxMasked_3, 2, 2) @[DCache.scala 546:58]
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
    node _perBankReq_Bin_3_T = bits(blockIdxMasked_0, 3, 3) @[DCache.scala 546:58]
    node _perBankReq_Bin_3_T_1 = bits(blockIdxMasked_1, 3, 3) @[DCache.scala 546:58]
    node _perBankReq_Bin_3_T_2 = bits(blockIdxMasked_2, 3, 3) @[DCache.scala 546:58]
    node _perBankReq_Bin_3_T_3 = bits(blockIdxMasked_3, 3, 3) @[DCache.scala 546:58]
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
    node perBankReq_Bin_0 = _perBankReq_Bin_0_T_13 @[DCache.scala 544:39 546:23]
    node _T = orr(perBankReq_Bin_0) @[DCache.scala 548:43]
    node perBankReq_Bin_1 = _perBankReq_Bin_1_T_13 @[DCache.scala 544:39 546:23]
    node _T_1 = orr(perBankReq_Bin_1) @[DCache.scala 548:43]
    node perBankReq_Bin_2 = _perBankReq_Bin_2_T_13 @[DCache.scala 544:39 546:23]
    node _T_2 = orr(perBankReq_Bin_2) @[DCache.scala 548:43]
    node perBankReq_Bin_3 = _perBankReq_Bin_3_T_13 @[DCache.scala 544:39 546:23]
    node _T_3 = orr(perBankReq_Bin_3) @[DCache.scala 548:43]
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
    io_perBankValid_0 <= _T @[DCache.scala 548:19]
    io_perBankValid_1 <= _T_1 @[DCache.scala 548:19]
    io_perBankValid_2 <= _T_2 @[DCache.scala 548:19]
    io_perBankValid_3 <= _T_3 @[DCache.scala 548:19]
    io_perBankBlockIdx_0 <= _T_10 @[DCache.scala 549:22]
    io_perBankBlockIdx_1 <= _T_17 @[DCache.scala 549:22]
    io_perBankBlockIdx_2 <= _T_24 @[DCache.scala 549:22]
    io_perBankBlockIdx_3 <= _T_31 @[DCache.scala 549:22]

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

    inst MshrAccess of MSHR @[DCache.scala 92:26]
    inst TagAccess of L1TagAccess @[DCache.scala 95:25]
    inst WshrAccess of DCacheWSHR @[DCache.scala 98:26]
    inst coreReq_Q of Queue @[DCache.scala 101:25]
    inst coreRsp_Q of Queue_1 @[DCache.scala 104:25]
    inst memRsp_Q of Queue_2 @[DCache.scala 107:24]
    inst memReq_Q of Queue_3 @[DCache.scala 111:24]
    inst MemReqArb of Arbiter_2 @[DCache.scala 112:25]
    inst genCtrl of genControl @[DCache.scala 162:23]
    inst getBankEn of getDataAccessBankEn @[DCache.scala 206:25]
    inst DataAccessesRRsp_DataAccess of SRAMTemplate_2 @[DCache.scala 383:28]
    inst DataAccessesRRsp_DataAccess_1 of SRAMTemplate_2 @[DCache.scala 383:28]
    inst DataAccessesRRsp_DataAccess_2 of SRAMTemplate_2 @[DCache.scala 383:28]
    inst DataAccessesRRsp_DataAccess_3 of SRAMTemplate_2 @[DCache.scala 383:28]
    node _secondaryFullReturn_T = eq(MshrAccess.io_probeOut_st1_probeStatus, UInt<3>("h4")) @[DCache.scala 125:76]
    reg secondaryFullReturn : UInt<1>, clock with :
      reset => (UInt<1>("h0"), secondaryFullReturn) @[DCache.scala 125:36]
    node _coreReq_st1_valid_T = eq(secondaryFullReturn, UInt<1>("h0")) @[DCache.scala 126:86]
    node _coreReq_st1_valid_T_1 = and(MshrAccess.io_missRspOut_valid, _coreReq_st1_valid_T) @[DCache.scala 126:83]
    node _coreReq_st1_valid_T_2 = eq(_coreReq_st1_valid_T_1, UInt<1>("h0")) @[DCache.scala 126:50]
    node _coreReq_st1_valid_T_3 = and(coreReq_Q.io_deq_valid, _coreReq_st1_valid_T_2) @[DCache.scala 126:47]
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
    node coreReqControl_st0_isRead = genCtrl.io_control_isRead @[DCache.scala 128:32 165:22]
    node _GEN_0 = mux(_coreReqControl_st1_T, coreReqControl_st0_isRead, coreReqControl_st1_isRead) @[Reg.scala 16:16 17:{18,22}]
    node coreReqControl_st0_isWrite = genCtrl.io_control_isWrite @[DCache.scala 128:32 165:22]
    node _GEN_1 = mux(_coreReqControl_st1_T, coreReqControl_st0_isWrite, coreReqControl_st1_isWrite) @[Reg.scala 16:16 17:{18,22}]
    node coreReqControl_st0_isLR = genCtrl.io_control_isLR @[DCache.scala 128:32 165:22]
    node _GEN_2 = mux(_coreReqControl_st1_T, coreReqControl_st0_isLR, coreReqControl_st1_isLR) @[Reg.scala 16:16 17:{18,22}]
    node coreReqControl_st0_isSC = genCtrl.io_control_isSC @[DCache.scala 128:32 165:22]
    node _GEN_3 = mux(_coreReqControl_st1_T, coreReqControl_st0_isSC, coreReqControl_st1_isSC) @[Reg.scala 16:16 17:{18,22}]
    node coreReqControl_st0_isAMO = genCtrl.io_control_isAMO @[DCache.scala 128:32 165:22]
    node _GEN_4 = mux(_coreReqControl_st1_T, coreReqControl_st0_isAMO, coreReqControl_st1_isAMO) @[Reg.scala 16:16 17:{18,22}]
    node coreReqControl_st0_isFlush = genCtrl.io_control_isFlush @[DCache.scala 128:32 165:22]
    node _GEN_5 = mux(_coreReqControl_st1_T, coreReqControl_st0_isFlush, coreReqControl_st1_isFlush) @[Reg.scala 16:16 17:{18,22}]
    node coreReqControl_st0_isInvalidate = genCtrl.io_control_isInvalidate @[DCache.scala 128:32 165:22]
    node _GEN_6 = mux(_coreReqControl_st1_T, coreReqControl_st0_isInvalidate, coreReqControl_st1_isInvalidate) @[Reg.scala 16:16 17:{18,22}]
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
    reg readHit_st2 : UInt<1>, clock with :
      reset => (UInt<1>("h0"), readHit_st2) @[DCache.scala 145:28]
    node _TagAccess_io_probeRead_valid_T = and(io_coreReq_ready, io_coreReq_valid) @[Decoupled.scala 50:35]
    node _MshrAccess_io_probe_valid_T = and(io_coreReq_ready, io_coreReq_valid) @[Decoupled.scala 50:35]
    node _MshrAccess_io_probe_bits_blockAddr_T = cat(io_coreReq_bits_tag, io_coreReq_bits_setIdx) @[Cat.scala 31:58]
    node _MshrAccess_io_missReq_valid_T = eq(MshrAccess.io_missRspOut_valid, UInt<1>("h0")) @[DCache.scala 172:50]
    node _MshrAccess_io_missReq_valid_T_1 = and(readMiss_st1, _MshrAccess_io_missReq_valid_T) @[DCache.scala 172:47]
    node coreReq_st1_valid = _coreReq_st1_valid_T_3 @[DCache.scala 122:31 126:21]
    node _MshrAccess_io_missReq_valid_T_2 = and(_MshrAccess_io_missReq_valid_T_1, coreReq_st1_valid) @[DCache.scala 172:82]
    reg MshrAccess_io_missReq_valid_REG : UInt<1>, clock with :
      reset => (UInt<1>("h0"), MshrAccess_io_missReq_valid_REG) @[DCache.scala 172:114]
    node _MshrAccess_io_missReq_valid_T_3 = eq(MshrAccess_io_missReq_valid_REG, UInt<1>("h0")) @[DCache.scala 172:106]
    node _MshrAccess_io_missReq_valid_T_4 = and(_MshrAccess_io_missReq_valid_T_2, _MshrAccess_io_missReq_valid_T_3) @[DCache.scala 172:103]
    node _MshrAccess_io_missReq_bits_blockAddr_T = cat(coreReq_Q.io_deq_bits_tag, coreReq_Q.io_deq_bits_setIdx) @[Cat.scala 31:58]
    node mshrMissReqTI_perLaneAddr_0_activeMask = coreReq_Q.io_deq_bits_perLaneAddr_0_activeMask @[DCache.scala 173:27 176:29]
    node mshrMissReqTI_perLaneAddr_0_blockOffset = coreReq_Q.io_deq_bits_perLaneAddr_0_blockOffset @[DCache.scala 173:27 176:29]
    node MshrAccess_io_missReq_bits_targetInfo_lo_lo_hi = cat(mshrMissReqTI_perLaneAddr_0_activeMask, mshrMissReqTI_perLaneAddr_0_blockOffset) @[DCache.scala 179:58]
    node mshrMissReqTI_perLaneAddr_0_wordOffset1H = coreReq_Q.io_deq_bits_perLaneAddr_0_wordOffset1H @[DCache.scala 173:27 176:29]
    node MshrAccess_io_missReq_bits_targetInfo_lo_lo = cat(MshrAccess_io_missReq_bits_targetInfo_lo_lo_hi, mshrMissReqTI_perLaneAddr_0_wordOffset1H) @[DCache.scala 179:58]
    node mshrMissReqTI_perLaneAddr_1_activeMask = coreReq_Q.io_deq_bits_perLaneAddr_1_activeMask @[DCache.scala 173:27 176:29]
    node mshrMissReqTI_perLaneAddr_1_blockOffset = coreReq_Q.io_deq_bits_perLaneAddr_1_blockOffset @[DCache.scala 173:27 176:29]
    node MshrAccess_io_missReq_bits_targetInfo_lo_hi_hi = cat(mshrMissReqTI_perLaneAddr_1_activeMask, mshrMissReqTI_perLaneAddr_1_blockOffset) @[DCache.scala 179:58]
    node mshrMissReqTI_perLaneAddr_1_wordOffset1H = coreReq_Q.io_deq_bits_perLaneAddr_1_wordOffset1H @[DCache.scala 173:27 176:29]
    node MshrAccess_io_missReq_bits_targetInfo_lo_hi = cat(MshrAccess_io_missReq_bits_targetInfo_lo_hi_hi, mshrMissReqTI_perLaneAddr_1_wordOffset1H) @[DCache.scala 179:58]
    node MshrAccess_io_missReq_bits_targetInfo_lo = cat(MshrAccess_io_missReq_bits_targetInfo_lo_hi, MshrAccess_io_missReq_bits_targetInfo_lo_lo) @[DCache.scala 179:58]
    node mshrMissReqTI_perLaneAddr_2_activeMask = coreReq_Q.io_deq_bits_perLaneAddr_2_activeMask @[DCache.scala 173:27 176:29]
    node mshrMissReqTI_perLaneAddr_2_blockOffset = coreReq_Q.io_deq_bits_perLaneAddr_2_blockOffset @[DCache.scala 173:27 176:29]
    node MshrAccess_io_missReq_bits_targetInfo_hi_lo_hi = cat(mshrMissReqTI_perLaneAddr_2_activeMask, mshrMissReqTI_perLaneAddr_2_blockOffset) @[DCache.scala 179:58]
    node mshrMissReqTI_perLaneAddr_2_wordOffset1H = coreReq_Q.io_deq_bits_perLaneAddr_2_wordOffset1H @[DCache.scala 173:27 176:29]
    node MshrAccess_io_missReq_bits_targetInfo_hi_lo = cat(MshrAccess_io_missReq_bits_targetInfo_hi_lo_hi, mshrMissReqTI_perLaneAddr_2_wordOffset1H) @[DCache.scala 179:58]
    node mshrMissReqTI_perLaneAddr_3_blockOffset = coreReq_Q.io_deq_bits_perLaneAddr_3_blockOffset @[DCache.scala 173:27 176:29]
    node mshrMissReqTI_perLaneAddr_3_wordOffset1H = coreReq_Q.io_deq_bits_perLaneAddr_3_wordOffset1H @[DCache.scala 173:27 176:29]
    node MshrAccess_io_missReq_bits_targetInfo_hi_hi_lo = cat(mshrMissReqTI_perLaneAddr_3_blockOffset, mshrMissReqTI_perLaneAddr_3_wordOffset1H) @[DCache.scala 179:58]
    node mshrMissReqTI_instrId = coreReq_Q.io_deq_bits_instrId @[DCache.scala 173:27 175:25]
    node mshrMissReqTI_perLaneAddr_3_activeMask = coreReq_Q.io_deq_bits_perLaneAddr_3_activeMask @[DCache.scala 173:27 176:29]
    node MshrAccess_io_missReq_bits_targetInfo_hi_hi_hi = cat(mshrMissReqTI_instrId, mshrMissReqTI_perLaneAddr_3_activeMask) @[DCache.scala 179:58]
    node MshrAccess_io_missReq_bits_targetInfo_hi_hi = cat(MshrAccess_io_missReq_bits_targetInfo_hi_hi_hi, MshrAccess_io_missReq_bits_targetInfo_hi_hi_lo) @[DCache.scala 179:58]
    node MshrAccess_io_missReq_bits_targetInfo_hi = cat(MshrAccess_io_missReq_bits_targetInfo_hi_hi, MshrAccess_io_missReq_bits_targetInfo_hi_lo) @[DCache.scala 179:58]
    node _MshrAccess_io_missReq_bits_targetInfo_T = cat(MshrAccess_io_missReq_bits_targetInfo_hi, MshrAccess_io_missReq_bits_targetInfo_lo) @[DCache.scala 179:58]
    node writeMissReq_a_addr_hi = cat(coreReq_Q.io_deq_bits_tag, coreReq_Q.io_deq_bits_setIdx) @[Cat.scala 31:58]
    node _writeMissReq_a_addr_T = cat(writeMissReq_a_addr_hi, UInt<4>("h0")) @[Cat.scala 31:58]
    node readMissReq_a_source_hi = cat(UInt<1>("h1"), MshrAccess.io_probeOut_st1_a_source) @[Cat.scala 31:58]
    node _readMissReq_a_source_T = cat(readMissReq_a_source_hi, coreReq_Q.io_deq_bits_setIdx) @[Cat.scala 31:58]
    node readMissReq_a_addr_hi = cat(coreReq_Q.io_deq_bits_tag, coreReq_Q.io_deq_bits_setIdx) @[Cat.scala 31:58]
    node _readMissReq_a_addr_T = cat(readMissReq_a_addr_hi, UInt<4>("h0")) @[Cat.scala 31:58]
    node writeMissReq_a_opcode = UInt<3>("h1") @[DCache.scala 183:26 185:25]
    node readMissReq_a_opcode = UInt<3>("h4") @[DCache.scala 184:25 194:24]
    node _missMemReq_T_a_opcode = mux(writeMiss_st1, writeMissReq_a_opcode, readMissReq_a_opcode) @[DCache.scala 203:20]
    node writeMissReq_a_param = UInt<3>("h0") @[DCache.scala 183:26 186:24]
    node readMissReq_a_param = UInt<3>("h0") @[DCache.scala 184:25 195:23]
    node _missMemReq_T_a_param = mux(writeMiss_st1, writeMissReq_a_param, readMissReq_a_param) @[DCache.scala 203:20]
    node writeMissReq_a_addr = _writeMissReq_a_addr_T @[DCache.scala 183:26 188:23]
    node readMissReq_a_addr = _readMissReq_a_addr_T @[DCache.scala 184:25 197:22]
    node _missMemReq_T_a_addr = mux(writeMiss_st1, writeMissReq_a_addr, readMissReq_a_addr) @[DCache.scala 203:20]
    node writeMissReq_a_data_0 = coreReq_Q.io_deq_bits_data_0 @[DCache.scala 183:26 190:23]
    node readMissReq_a_data_0 = validif(UInt<1>("h0"), UInt<32>("h0"))
    node _missMemReq_T_a_data_0 = mux(writeMiss_st1, writeMissReq_a_data_0, readMissReq_a_data_0) @[DCache.scala 203:20]
    node writeMissReq_a_data_1 = coreReq_Q.io_deq_bits_data_1 @[DCache.scala 183:26 190:23]
    node readMissReq_a_data_1 = validif(UInt<1>("h0"), UInt<32>("h0"))
    node _missMemReq_T_a_data_1 = mux(writeMiss_st1, writeMissReq_a_data_1, readMissReq_a_data_1) @[DCache.scala 203:20]
    node writeMissReq_a_data_2 = coreReq_Q.io_deq_bits_data_2 @[DCache.scala 183:26 190:23]
    node readMissReq_a_data_2 = validif(UInt<1>("h0"), UInt<32>("h0"))
    node _missMemReq_T_a_data_2 = mux(writeMiss_st1, writeMissReq_a_data_2, readMissReq_a_data_2) @[DCache.scala 203:20]
    node writeMissReq_a_data_3 = coreReq_Q.io_deq_bits_data_3 @[DCache.scala 183:26 190:23]
    node readMissReq_a_data_3 = validif(UInt<1>("h0"), UInt<32>("h0"))
    node _missMemReq_T_a_data_3 = mux(writeMiss_st1, writeMissReq_a_data_3, readMissReq_a_data_3) @[DCache.scala 203:20]
    node writeMissReq_a_mask_0 = coreReq_Q.io_deq_bits_perLaneAddr_0_activeMask @[DCache.scala 183:26 189:23]
    node readMissReq_a_mask_0 = coreReq_Q.io_deq_bits_perLaneAddr_0_activeMask @[DCache.scala 184:25 198:22]
    node _missMemReq_T_a_mask_0 = mux(writeMiss_st1, writeMissReq_a_mask_0, readMissReq_a_mask_0) @[DCache.scala 203:20]
    node writeMissReq_a_mask_1 = coreReq_Q.io_deq_bits_perLaneAddr_1_activeMask @[DCache.scala 183:26 189:23]
    node readMissReq_a_mask_1 = coreReq_Q.io_deq_bits_perLaneAddr_1_activeMask @[DCache.scala 184:25 198:22]
    node _missMemReq_T_a_mask_1 = mux(writeMiss_st1, writeMissReq_a_mask_1, readMissReq_a_mask_1) @[DCache.scala 203:20]
    node writeMissReq_a_mask_2 = coreReq_Q.io_deq_bits_perLaneAddr_2_activeMask @[DCache.scala 183:26 189:23]
    node readMissReq_a_mask_2 = coreReq_Q.io_deq_bits_perLaneAddr_2_activeMask @[DCache.scala 184:25 198:22]
    node _missMemReq_T_a_mask_2 = mux(writeMiss_st1, writeMissReq_a_mask_2, readMissReq_a_mask_2) @[DCache.scala 203:20]
    node writeMissReq_a_mask_3 = coreReq_Q.io_deq_bits_perLaneAddr_3_activeMask @[DCache.scala 183:26 189:23]
    node readMissReq_a_mask_3 = coreReq_Q.io_deq_bits_perLaneAddr_3_activeMask @[DCache.scala 184:25 198:22]
    node _missMemReq_T_a_mask_3 = mux(writeMiss_st1, writeMissReq_a_mask_3, readMissReq_a_mask_3) @[DCache.scala 203:20]
    node writeMissReq_a_source = validif(UInt<1>("h0"), UInt<7>("h0"))
    node readMissReq_a_source = pad(_readMissReq_a_source_T, 7) @[DCache.scala 184:25 196:24]
    node _missMemReq_T_a_source = mux(writeMiss_st1, writeMissReq_a_source, readMissReq_a_source) @[DCache.scala 203:20]
    node writeMissReq_hasCoreRsp = UInt<1>("h1") @[DCache.scala 183:26 191:27]
    node readMissReq_hasCoreRsp = UInt<1>("h0") @[DCache.scala 184:25 200:26]
    node _missMemReq_T_hasCoreRsp = mux(writeMiss_st1, writeMissReq_hasCoreRsp, readMissReq_hasCoreRsp) @[DCache.scala 203:20]
    node writeMissReq_coreRspInstrId = pad(coreReq_Q.io_deq_bits_instrId, 32) @[DCache.scala 183:26 192:31]
    node readMissReq_coreRspInstrId = validif(UInt<1>("h0"), UInt<32>("h0"))
    node _missMemReq_T_coreRspInstrId = mux(writeMiss_st1, writeMissReq_coreRspInstrId, readMissReq_coreRspInstrId) @[DCache.scala 203:20]
    node _DataAccessWriteHitSRAMWReq_0_setIdx_T = cat(coreReq_Q.io_deq_bits_setIdx, TagAccess.io_waymaskHit_st1) @[Cat.scala 31:58]
    node _DataAccessWriteHitSRAMWReq_1_setIdx_T = cat(coreReq_Q.io_deq_bits_setIdx, TagAccess.io_waymaskHit_st1) @[Cat.scala 31:58]
    node _DataAccessWriteHitSRAMWReq_2_setIdx_T = cat(coreReq_Q.io_deq_bits_setIdx, TagAccess.io_waymaskHit_st1) @[Cat.scala 31:58]
    node _DataAccessWriteHitSRAMWReq_3_setIdx_T = cat(coreReq_Q.io_deq_bits_setIdx, TagAccess.io_waymaskHit_st1) @[Cat.scala 31:58]
    node _GEN_7 = validif(eq(UInt<1>("h0"), getBankEn.io_perBankBlockIdx_0), coreReq_Q.io_deq_bits_perLaneAddr_0_wordOffset1H) @[DCache.scala 215:{47,47}]
    node _GEN_8 = mux(eq(UInt<1>("h1"), getBankEn.io_perBankBlockIdx_0), coreReq_Q.io_deq_bits_perLaneAddr_1_wordOffset1H, _GEN_7) @[DCache.scala 215:{47,47}]
    node _GEN_9 = mux(eq(UInt<2>("h2"), getBankEn.io_perBankBlockIdx_0), coreReq_Q.io_deq_bits_perLaneAddr_2_wordOffset1H, _GEN_8) @[DCache.scala 215:{47,47}]
    node _GEN_10 = mux(eq(UInt<2>("h3"), getBankEn.io_perBankBlockIdx_0), coreReq_Q.io_deq_bits_perLaneAddr_3_wordOffset1H, _GEN_9) @[DCache.scala 215:{47,47}]
    node _GEN_11 = validif(eq(UInt<1>("h0"), getBankEn.io_perBankBlockIdx_0), coreReq_Q.io_deq_bits_data_0)
    node _GEN_12 = mux(eq(UInt<1>("h1"), getBankEn.io_perBankBlockIdx_0), coreReq_Q.io_deq_bits_data_1, _GEN_11)
    node _GEN_13 = mux(eq(UInt<2>("h2"), getBankEn.io_perBankBlockIdx_0), coreReq_Q.io_deq_bits_data_2, _GEN_12)
    node _GEN_14 = mux(eq(UInt<2>("h3"), getBankEn.io_perBankBlockIdx_0), coreReq_Q.io_deq_bits_data_3, _GEN_13)
    node _coreReq_Q_io_deq_bits_data_getBankEn_io_perBankBlockIdx_0 = _GEN_14
    node _WIRE_1 = _coreReq_Q_io_deq_bits_data_getBankEn_io_perBankBlockIdx_0
    node _T = bits(_WIRE_1, 7, 0) @[DCache.scala 216:101]
    node _T_1 = bits(_WIRE_1, 15, 8) @[DCache.scala 216:101]
    node _T_2 = bits(_WIRE_1, 23, 16) @[DCache.scala 216:101]
    node _T_3 = bits(_WIRE_1, 31, 24) @[DCache.scala 216:101]
    node _GEN_15 = validif(eq(UInt<1>("h0"), getBankEn.io_perBankBlockIdx_1), coreReq_Q.io_deq_bits_perLaneAddr_0_wordOffset1H) @[DCache.scala 215:{47,47}]
    node _GEN_16 = mux(eq(UInt<1>("h1"), getBankEn.io_perBankBlockIdx_1), coreReq_Q.io_deq_bits_perLaneAddr_1_wordOffset1H, _GEN_15) @[DCache.scala 215:{47,47}]
    node _GEN_17 = mux(eq(UInt<2>("h2"), getBankEn.io_perBankBlockIdx_1), coreReq_Q.io_deq_bits_perLaneAddr_2_wordOffset1H, _GEN_16) @[DCache.scala 215:{47,47}]
    node _GEN_18 = mux(eq(UInt<2>("h3"), getBankEn.io_perBankBlockIdx_1), coreReq_Q.io_deq_bits_perLaneAddr_3_wordOffset1H, _GEN_17) @[DCache.scala 215:{47,47}]
    node _GEN_19 = validif(eq(UInt<1>("h0"), getBankEn.io_perBankBlockIdx_1), coreReq_Q.io_deq_bits_data_0)
    node _GEN_20 = mux(eq(UInt<1>("h1"), getBankEn.io_perBankBlockIdx_1), coreReq_Q.io_deq_bits_data_1, _GEN_19)
    node _GEN_21 = mux(eq(UInt<2>("h2"), getBankEn.io_perBankBlockIdx_1), coreReq_Q.io_deq_bits_data_2, _GEN_20)
    node _GEN_22 = mux(eq(UInt<2>("h3"), getBankEn.io_perBankBlockIdx_1), coreReq_Q.io_deq_bits_data_3, _GEN_21)
    node _coreReq_Q_io_deq_bits_data_getBankEn_io_perBankBlockIdx_1 = _GEN_22
    node _WIRE_3 = _coreReq_Q_io_deq_bits_data_getBankEn_io_perBankBlockIdx_1
    node _T_4 = bits(_WIRE_3, 7, 0) @[DCache.scala 216:101]
    node _T_5 = bits(_WIRE_3, 15, 8) @[DCache.scala 216:101]
    node _T_6 = bits(_WIRE_3, 23, 16) @[DCache.scala 216:101]
    node _T_7 = bits(_WIRE_3, 31, 24) @[DCache.scala 216:101]
    node _GEN_23 = validif(eq(UInt<1>("h0"), getBankEn.io_perBankBlockIdx_2), coreReq_Q.io_deq_bits_perLaneAddr_0_wordOffset1H) @[DCache.scala 215:{47,47}]
    node _GEN_24 = mux(eq(UInt<1>("h1"), getBankEn.io_perBankBlockIdx_2), coreReq_Q.io_deq_bits_perLaneAddr_1_wordOffset1H, _GEN_23) @[DCache.scala 215:{47,47}]
    node _GEN_25 = mux(eq(UInt<2>("h2"), getBankEn.io_perBankBlockIdx_2), coreReq_Q.io_deq_bits_perLaneAddr_2_wordOffset1H, _GEN_24) @[DCache.scala 215:{47,47}]
    node _GEN_26 = mux(eq(UInt<2>("h3"), getBankEn.io_perBankBlockIdx_2), coreReq_Q.io_deq_bits_perLaneAddr_3_wordOffset1H, _GEN_25) @[DCache.scala 215:{47,47}]
    node _GEN_27 = validif(eq(UInt<1>("h0"), getBankEn.io_perBankBlockIdx_2), coreReq_Q.io_deq_bits_data_0)
    node _GEN_28 = mux(eq(UInt<1>("h1"), getBankEn.io_perBankBlockIdx_2), coreReq_Q.io_deq_bits_data_1, _GEN_27)
    node _GEN_29 = mux(eq(UInt<2>("h2"), getBankEn.io_perBankBlockIdx_2), coreReq_Q.io_deq_bits_data_2, _GEN_28)
    node _GEN_30 = mux(eq(UInt<2>("h3"), getBankEn.io_perBankBlockIdx_2), coreReq_Q.io_deq_bits_data_3, _GEN_29)
    node _coreReq_Q_io_deq_bits_data_getBankEn_io_perBankBlockIdx_2 = _GEN_30
    node _WIRE_5 = _coreReq_Q_io_deq_bits_data_getBankEn_io_perBankBlockIdx_2
    node _T_8 = bits(_WIRE_5, 7, 0) @[DCache.scala 216:101]
    node _T_9 = bits(_WIRE_5, 15, 8) @[DCache.scala 216:101]
    node _T_10 = bits(_WIRE_5, 23, 16) @[DCache.scala 216:101]
    node _T_11 = bits(_WIRE_5, 31, 24) @[DCache.scala 216:101]
    node _GEN_31 = validif(eq(UInt<1>("h0"), getBankEn.io_perBankBlockIdx_3), coreReq_Q.io_deq_bits_perLaneAddr_0_wordOffset1H) @[DCache.scala 215:{47,47}]
    node _GEN_32 = mux(eq(UInt<1>("h1"), getBankEn.io_perBankBlockIdx_3), coreReq_Q.io_deq_bits_perLaneAddr_1_wordOffset1H, _GEN_31) @[DCache.scala 215:{47,47}]
    node _GEN_33 = mux(eq(UInt<2>("h2"), getBankEn.io_perBankBlockIdx_3), coreReq_Q.io_deq_bits_perLaneAddr_2_wordOffset1H, _GEN_32) @[DCache.scala 215:{47,47}]
    node _GEN_34 = mux(eq(UInt<2>("h3"), getBankEn.io_perBankBlockIdx_3), coreReq_Q.io_deq_bits_perLaneAddr_3_wordOffset1H, _GEN_33) @[DCache.scala 215:{47,47}]
    node _GEN_35 = validif(eq(UInt<1>("h0"), getBankEn.io_perBankBlockIdx_3), coreReq_Q.io_deq_bits_data_0)
    node _GEN_36 = mux(eq(UInt<1>("h1"), getBankEn.io_perBankBlockIdx_3), coreReq_Q.io_deq_bits_data_1, _GEN_35)
    node _GEN_37 = mux(eq(UInt<2>("h2"), getBankEn.io_perBankBlockIdx_3), coreReq_Q.io_deq_bits_data_2, _GEN_36)
    node _GEN_38 = mux(eq(UInt<2>("h3"), getBankEn.io_perBankBlockIdx_3), coreReq_Q.io_deq_bits_data_3, _GEN_37)
    node _coreReq_Q_io_deq_bits_data_getBankEn_io_perBankBlockIdx_3 = _GEN_38
    node _WIRE_7 = _coreReq_Q_io_deq_bits_data_getBankEn_io_perBankBlockIdx_3
    node _T_12 = bits(_WIRE_7, 7, 0) @[DCache.scala 216:101]
    node _T_13 = bits(_WIRE_7, 15, 8) @[DCache.scala 216:101]
    node _T_14 = bits(_WIRE_7, 23, 16) @[DCache.scala 216:101]
    node _T_15 = bits(_WIRE_7, 31, 24) @[DCache.scala 216:101]
    node _DataAccessReadHitSRAMRReq_0_setIdx_T = cat(coreReq_Q.io_deq_bits_setIdx, TagAccess.io_waymaskHit_st1) @[Cat.scala 31:58]
    node _DataAccessReadHitSRAMRReq_1_setIdx_T = cat(coreReq_Q.io_deq_bits_setIdx, TagAccess.io_waymaskHit_st1) @[Cat.scala 31:58]
    node _DataAccessReadHitSRAMRReq_2_setIdx_T = cat(coreReq_Q.io_deq_bits_setIdx, TagAccess.io_waymaskHit_st1) @[Cat.scala 31:58]
    node _DataAccessReadHitSRAMRReq_3_setIdx_T = cat(coreReq_Q.io_deq_bits_setIdx, TagAccess.io_waymaskHit_st1) @[Cat.scala 31:58]
    node _T_16 = or(coreReqControl_st1_isRead, coreReqControl_st1_isWrite) @[DCache.scala 224:34]
    node _GEN_39 = mux(coreRsp_Q.io_enq_ready, UInt<1>("h1"), UInt<1>("h0")) @[DCache.scala 223:21 226:36 227:27]
    node _T_17 = and(MshrAccess.io_missReq_ready, MemReqArb.io_in_1_ready) @[DCache.scala 231:42]
    node _T_18 = eq(secondaryFullReturn, UInt<1>("h0")) @[DCache.scala 232:50]
    node _T_19 = and(MshrAccess.io_missRspOut_valid, _T_18) @[DCache.scala 232:47]
    node _T_20 = eq(_T_19, UInt<1>("h0")) @[DCache.scala 232:14]
    node _T_21 = and(_T_17, _T_20) @[DCache.scala 232:11]
    node _GEN_40 = mux(_T_21, UInt<1>("h1"), UInt<1>("h0")) @[DCache.scala 223:21 232:72 233:29]
    node _T_22 = and(coreRsp_Q.io_enq_ready, MemReqArb.io_in_1_ready) @[DCache.scala 237:37]
    node _GEN_41 = mux(_T_22, UInt<1>("h1"), UInt<1>("h0")) @[DCache.scala 223:21 237:65 238:29]
    node _GEN_42 = mux(coreReqControl_st1_isRead, _GEN_40, _GEN_41) @[DCache.scala 230:38]
    node _GEN_43 = mux(TagAccess.io_hit_st1, _GEN_39, _GEN_42) @[DCache.scala 225:32]
    node _GEN_44 = mux(_T_16, _GEN_43, UInt<1>("h1")) @[DCache.scala 224:64 245:23]
    reg tagReqValidCtrl : UInt<1>, clock with :
      reset => (UInt<1>("h0"), tagReqValidCtrl) @[DCache.scala 252:32]
    reg tagReqReadyCtrl : UInt<1>, clock with :
      reset => (UInt<1>("h0"), tagReqReadyCtrl) @[DCache.scala 253:32]
    reg tagReplaceStatus : UInt<1>, clock with :
      reset => (UInt<1>("h0"), tagReplaceStatus) @[DCache.scala 285:33]
    node _T_35 = eq(tagReplaceStatus, UInt<1>("h0")) @[DCache.scala 296:25]
    node _tagAllocateWriteReady_T = eq(TagAccess.io_needReplace, UInt<1>("h0")) @[DCache.scala 297:30]
    node _GEN_52 = mux(_T_35, _tagAllocateWriteReady_T, MemReqArb.io_in_0_ready) @[DCache.scala 296:37 297:27 299:27]
    node tagAllocateWriteReady = _GEN_52 @[DCache.scala 254:35]
    node tagAllocateWriteReady_mod = mux(tagReqReadyCtrl, UInt<1>("h1"), tagAllocateWriteReady) @[DCache.scala 255:38]
    node tagAllocateWriteFire = and(TagAccess.io_allocateWrite_valid, tagAllocateWriteReady_mod) @[DCache.scala 256:69]
    node _T_23 = and(memRsp_Q.io_deq_ready, memRsp_Q.io_deq_valid) @[Decoupled.scala 50:35]
    node _T_24 = eq(_T_23, UInt<1>("h0")) @[DCache.scala 257:32]
    node _T_25 = and(tagAllocateWriteFire, _T_24) @[DCache.scala 257:29]
    node _T_26 = eq(tagReqValidCtrl, UInt<1>("h0")) @[DCache.scala 259:14]
    node _T_27 = and(memRsp_Q.io_deq_ready, memRsp_Q.io_deq_valid) @[Decoupled.scala 50:35]
    node _T_28 = and(_T_26, _T_27) @[DCache.scala 259:31]
    node _GEN_45 = mux(_T_28, UInt<1>("h1"), tagReqValidCtrl) @[DCache.scala 259:56 260:21 252:32]
    node _GEN_46 = mux(_T_25, UInt<1>("h0"), _GEN_45) @[DCache.scala 257:55 258:21]
    node _T_29 = and(memRsp_Q.io_deq_ready, memRsp_Q.io_deq_valid) @[Decoupled.scala 50:35]
    node _T_30 = eq(_T_29, UInt<1>("h0")) @[DCache.scala 262:32]
    node _T_31 = and(tagAllocateWriteFire, _T_30) @[DCache.scala 262:29]
    node _T_32 = and(memRsp_Q.io_deq_ready, memRsp_Q.io_deq_valid) @[Decoupled.scala 50:35]
    node _T_33 = and(tagReqReadyCtrl, _T_32) @[DCache.scala 264:30]
    node _GEN_47 = mux(_T_33, UInt<1>("h0"), tagReqReadyCtrl) @[DCache.scala 264:55 265:21 253:32]
    node _GEN_48 = mux(_T_31, UInt<1>("h1"), _GEN_47) @[DCache.scala 262:55 263:21]
    node memRspIsWrite = eq(memRsp_Q.io_deq_bits_d_opcode, UInt<1>("h0")) @[DCache.scala 268:59]
    node memRspIsRead = eq(memRsp_Q.io_deq_bits_d_opcode, UInt<1>("h1")) @[DCache.scala 269:58]
    node _memRsp_Q_io_deq_ready_T = and(memRspIsRead, tagAllocateWriteReady_mod) @[DCache.scala 271:19]
    node _memRsp_Q_io_deq_ready_T_1 = and(_memRsp_Q_io_deq_ready_T, MshrAccess.io_missRspIn_ready) @[DCache.scala 271:48]
    node _memRsp_Q_io_deq_ready_T_2 = and(_memRsp_Q_io_deq_ready_T_1, coreRsp_Q.io_enq_ready) @[DCache.scala 271:81]
    node _memRsp_Q_io_deq_ready_T_3 = or(memRspIsWrite, _memRsp_Q_io_deq_ready_T_2) @[DCache.scala 270:42]
    node _WshrAccess_io_popReq_valid_T = and(memRsp_Q.io_deq_valid, memRspIsWrite) @[DCache.scala 273:55]
    node _WshrAccess_io_popReq_bits_T = bits(memRsp_Q.io_deq_bits_d_source, 3, 2) @[DCache.scala 274:61]
    reg memRsp_st1_d_opcode : UInt<3>, clock with :
      reset => (UInt<1>("h0"), memRsp_st1_d_opcode) @[DCache.scala 277:23]
    reg memRsp_st1_d_source : UInt<7>, clock with :
      reset => (UInt<1>("h0"), memRsp_st1_d_source) @[DCache.scala 277:23]
    reg memRsp_st1_d_addr : UInt<32>, clock with :
      reset => (UInt<1>("h0"), memRsp_st1_d_addr) @[DCache.scala 277:23]
    reg memRsp_st1_d_data_0 : UInt<32>, clock with :
      reset => (UInt<1>("h0"), memRsp_st1_d_data_0) @[DCache.scala 277:23]
    reg memRsp_st1_d_data_1 : UInt<32>, clock with :
      reset => (UInt<1>("h0"), memRsp_st1_d_data_1) @[DCache.scala 277:23]
    reg memRsp_st1_d_data_2 : UInt<32>, clock with :
      reset => (UInt<1>("h0"), memRsp_st1_d_data_2) @[DCache.scala 277:23]
    reg memRsp_st1_d_data_3 : UInt<32>, clock with :
      reset => (UInt<1>("h0"), memRsp_st1_d_data_3) @[DCache.scala 277:23]
    node _T_34 = eq(tagReplaceStatus, UInt<1>("h0")) @[DCache.scala 286:25]
    node _GEN_49 = mux(TagAccess.io_needReplace, UInt<1>("h1"), tagReplaceStatus) @[DCache.scala 287:39 288:24 285:33]
    node _GEN_50 = mux(MemReqArb.io_in_0_ready, UInt<1>("h0"), tagReplaceStatus) @[DCache.scala 291:36 292:24 285:33]
    node _GEN_51 = mux(_T_34, _GEN_49, _GEN_50) @[DCache.scala 286:37]
    node missRspSetIdx_st1 = bits(memRsp_st1_d_source, 1, 0) @[DCache.scala 302:46]
    reg dataReplaceReadValid_REG : UInt<1>, clock with :
      reset => (UInt<1>("h0"), dataReplaceReadValid_REG) @[DCache.scala 303:37]
    node _dataReplaceReadValid_T = eq(tagReplaceStatus, UInt<1>("h0")) @[DCache.scala 304:22]
    node _dataReplaceReadValid_T_1 = and(dataReplaceReadValid_REG, _dataReplaceReadValid_T) @[DCache.scala 303:72]
    node dataReplaceReadValid = and(_dataReplaceReadValid_T_1, TagAccess.io_needReplace) @[DCache.scala 304:34]
    node _DataAccessReplaceReadSRAMRReq_0_setIdx_T = cat(missRspSetIdx_st1, TagAccess.io_waymaskReplacement_st1) @[Cat.scala 31:58]
    node _DataAccessReplaceReadSRAMRReq_1_setIdx_T = cat(missRspSetIdx_st1, TagAccess.io_waymaskReplacement_st1) @[Cat.scala 31:58]
    node _DataAccessReplaceReadSRAMRReq_2_setIdx_T = cat(missRspSetIdx_st1, TagAccess.io_waymaskReplacement_st1) @[Cat.scala 31:58]
    node _DataAccessReplaceReadSRAMRReq_3_setIdx_T = cat(missRspSetIdx_st1, TagAccess.io_waymaskReplacement_st1) @[Cat.scala 31:58]
    reg dataFillVaild_REG : UInt<1>, clock with :
      reset => (UInt<1>("h0"), dataFillVaild_REG) @[DCache.scala 309:30]
    node _dataFillVaild_T = eq(tagReplaceStatus, UInt<1>("h0")) @[DCache.scala 310:22]
    node _dataFillVaild_T_1 = and(dataFillVaild_REG, _dataFillVaild_T) @[DCache.scala 309:65]
    node _dataFillVaild_T_2 = eq(TagAccess.io_needReplace, UInt<1>("h0")) @[DCache.scala 311:5]
    node dataFillVaild = and(_dataFillVaild_T_1, _dataFillVaild_T_2) @[DCache.scala 310:34]
    node _DataAccessMissRspSRAMWReq_0_setIdx_T = cat(missRspSetIdx_st1, TagAccess.io_waymaskReplacement_st1) @[Cat.scala 31:58]
    node _DataAccessMissRspSRAMWReq_1_setIdx_T = cat(missRspSetIdx_st1, TagAccess.io_waymaskReplacement_st1) @[Cat.scala 31:58]
    node _DataAccessMissRspSRAMWReq_2_setIdx_T = cat(missRspSetIdx_st1, TagAccess.io_waymaskReplacement_st1) @[Cat.scala 31:58]
    node _DataAccessMissRspSRAMWReq_3_setIdx_T = cat(missRspSetIdx_st1, TagAccess.io_waymaskReplacement_st1) @[Cat.scala 31:58]
    node _DataAccessMissRspSRAMWReq_0_waymask_T = mux(UInt<1>("h1"), UInt<4>("hf"), UInt<4>("h0")) @[Bitwise.scala 74:12]
    node _DataAccessMissRspSRAMWReq_1_waymask_T = mux(UInt<1>("h1"), UInt<4>("hf"), UInt<4>("h0")) @[Bitwise.scala 74:12]
    node _DataAccessMissRspSRAMWReq_2_waymask_T = mux(UInt<1>("h1"), UInt<4>("hf"), UInt<4>("h0")) @[Bitwise.scala 74:12]
    node _DataAccessMissRspSRAMWReq_3_waymask_T = mux(UInt<1>("h1"), UInt<4>("hf"), UInt<4>("h0")) @[Bitwise.scala 74:12]
    node _WIRE_9 = memRsp_st1_d_data_0
    node _T_36 = bits(_WIRE_9, 7, 0) @[DCache.scala 316:71]
    node _T_37 = bits(_WIRE_9, 15, 8) @[DCache.scala 316:71]
    node _T_38 = bits(_WIRE_9, 23, 16) @[DCache.scala 316:71]
    node _T_39 = bits(_WIRE_9, 31, 24) @[DCache.scala 316:71]
    node _WIRE_11 = memRsp_st1_d_data_1
    node _T_40 = bits(_WIRE_11, 7, 0) @[DCache.scala 316:71]
    node _T_41 = bits(_WIRE_11, 15, 8) @[DCache.scala 316:71]
    node _T_42 = bits(_WIRE_11, 23, 16) @[DCache.scala 316:71]
    node _T_43 = bits(_WIRE_11, 31, 24) @[DCache.scala 316:71]
    node _WIRE_13 = memRsp_st1_d_data_2
    node _T_44 = bits(_WIRE_13, 7, 0) @[DCache.scala 316:71]
    node _T_45 = bits(_WIRE_13, 15, 8) @[DCache.scala 316:71]
    node _T_46 = bits(_WIRE_13, 23, 16) @[DCache.scala 316:71]
    node _T_47 = bits(_WIRE_13, 31, 24) @[DCache.scala 316:71]
    node _WIRE_15 = memRsp_st1_d_data_3
    node _T_48 = bits(_WIRE_15, 7, 0) @[DCache.scala 316:71]
    node _T_49 = bits(_WIRE_15, 15, 8) @[DCache.scala 316:71]
    node _T_50 = bits(_WIRE_15, 23, 16) @[DCache.scala 316:71]
    node _T_51 = bits(_WIRE_15, 31, 24) @[DCache.scala 316:71]
    node _T_52 = and(memRsp_Q.io_deq_valid, memRspIsRead) @[DCache.scala 330:30]
    node _GEN_53 = mux(_T_52, memRsp_Q.io_deq_bits_d_opcode, memRsp_st1_d_opcode) @[DCache.scala 330:46 331:16 277:23]
    node _GEN_54 = mux(_T_52, memRsp_Q.io_deq_bits_d_source, memRsp_st1_d_source) @[DCache.scala 330:46 331:16 277:23]
    node _GEN_55 = mux(_T_52, memRsp_Q.io_deq_bits_d_addr, memRsp_st1_d_addr) @[DCache.scala 330:46 331:16 277:23]
    node _GEN_56 = mux(_T_52, memRsp_Q.io_deq_bits_d_data_0, memRsp_st1_d_data_0) @[DCache.scala 330:46 331:16 277:23]
    node _GEN_57 = mux(_T_52, memRsp_Q.io_deq_bits_d_data_1, memRsp_st1_d_data_1) @[DCache.scala 330:46 331:16 277:23]
    node _GEN_58 = mux(_T_52, memRsp_Q.io_deq_bits_d_data_2, memRsp_st1_d_data_2) @[DCache.scala 330:46 331:16 277:23]
    node _GEN_59 = mux(_T_52, memRsp_Q.io_deq_bits_d_data_3, memRsp_st1_d_data_3) @[DCache.scala 330:46 331:16 277:23]
    node _MshrAccess_io_missRspIn_valid_T = and(memRsp_Q.io_deq_valid, memRspIsRead) @[DCache.scala 343:58]
    node _MshrAccess_io_missRspIn_valid_T_1 = and(_MshrAccess_io_missRspIn_valid_T, coreRsp_Q.io_enq_ready) @[DCache.scala 343:74]
    node _MshrAccess_io_missRspIn_bits_instrId_T = bits(memRsp_Q.io_deq_bits_d_source, 3, 2) @[DCache.scala 344:72]
    node _TagAccess_io_allocateWrite_valid_T = and(memRsp_Q.io_deq_valid, memRspIsRead) @[DCache.scala 347:81]
    node _TagAccess_io_allocateWrite_valid_T_1 = mux(tagReqValidCtrl, _TagAccess_io_allocateWrite_valid_T, UInt<1>("h0")) @[DCache.scala 347:42]
    node _TagAccess_io_allocateWrite_bits_setIdx_T = bits(memRsp_Q.io_deq_bits_d_source, 1, 0) @[DCache.scala 348:74]
    node _missRspTI_st1_WIRE_1 = MshrAccess.io_missRspOut_bits_targetInfo
    node _missRspTI_st1_T = bits(_missRspTI_st1_WIRE_1, 3, 0) @[DCache.scala 353:69]
    node _missRspTI_st1_T_1 = bits(_missRspTI_st1_WIRE_1, 5, 4) @[DCache.scala 353:69]
    node _missRspTI_st1_T_2 = bits(_missRspTI_st1_WIRE_1, 6, 6) @[DCache.scala 353:69]
    node _missRspTI_st1_T_3 = bits(_missRspTI_st1_WIRE_1, 10, 7) @[DCache.scala 353:69]
    node _missRspTI_st1_T_4 = bits(_missRspTI_st1_WIRE_1, 12, 11) @[DCache.scala 353:69]
    node _missRspTI_st1_T_5 = bits(_missRspTI_st1_WIRE_1, 13, 13) @[DCache.scala 353:69]
    node _missRspTI_st1_T_6 = bits(_missRspTI_st1_WIRE_1, 17, 14) @[DCache.scala 353:69]
    node _missRspTI_st1_T_7 = bits(_missRspTI_st1_WIRE_1, 19, 18) @[DCache.scala 353:69]
    node _missRspTI_st1_T_8 = bits(_missRspTI_st1_WIRE_1, 20, 20) @[DCache.scala 353:69]
    node _missRspTI_st1_T_9 = bits(_missRspTI_st1_WIRE_1, 24, 21) @[DCache.scala 353:69]
    node _missRspTI_st1_T_10 = bits(_missRspTI_st1_WIRE_1, 26, 25) @[DCache.scala 353:69]
    node _missRspTI_st1_T_11 = bits(_missRspTI_st1_WIRE_1, 27, 27) @[DCache.scala 353:69]
    node _missRspTI_st1_T_12 = bits(_missRspTI_st1_WIRE_1, 30, 28) @[DCache.scala 353:69]
    node _TagAccess_io_allocateWriteData_st1_T = shr(MshrAccess.io_missRspOut_bits_blockAddr, 2) @[L1CacheParameters.scala 54:35]
    reg TagAccess_io_allocateWriteTagSRAMWValid_st1_REG : UInt<1>, clock with :
      reset => (UInt<1>("h0"), TagAccess_io_allocateWriteTagSRAMWValid_st1_REG) @[DCache.scala 362:57]
    node _TagAccess_io_allocateWriteTagSRAMWValid_st1_T = and(TagAccess_io_allocateWriteTagSRAMWValid_st1_REG, tagAllocateWriteReady) @[DCache.scala 362:92]
    node _DataAccessesRRsp_DataAccess_io_w_req_valid_T = and(writeHit_st1, getBankEn.io_perBankValid_0) @[DCache.scala 393:20]
    node _DataAccessesRRsp_DataAccess_io_w_req_valid_T_1 = mux(dataFillVaild, UInt<1>("h1"), _DataAccessesRRsp_DataAccess_io_w_req_valid_T) @[DCache.scala 391:37]
    node _DataAccessesRRsp_DataAccess_io_w_req_bits_T = and(memRsp_Q.io_deq_valid, memRspIsRead) @[DCache.scala 400:59]
    node DataAccessMissRspSRAMWReq_0_setIdx = bits(_DataAccessMissRspSRAMWReq_0_setIdx_T, 2, 0) @[DCache.scala 312:64 313:46]
    node DataAccessWriteHitSRAMWReq_0_setIdx = bits(_DataAccessWriteHitSRAMWReq_0_setIdx_T, 2, 0) @[DCache.scala 211:65 213:47]
    node _DataAccessesRRsp_DataAccess_io_w_req_bits_T_1_setIdx = mux(_DataAccessesRRsp_DataAccess_io_w_req_bits_T, DataAccessMissRspSRAMWReq_0_setIdx, DataAccessWriteHitSRAMWReq_0_setIdx) @[DCache.scala 400:36]
    node _WIRE_8_0 = _T_36 @[DCache.scala 316:{71,71}]
    node DataAccessMissRspSRAMWReq_0_data_0 = _WIRE_8_0 @[DCache.scala 312:64 316:39]
    node _WIRE__0 = _T @[DCache.scala 216:{101,101}]
    node DataAccessWriteHitSRAMWReq_0_data_0 = _WIRE__0 @[DCache.scala 211:65 216:40]
    node _DataAccessesRRsp_DataAccess_io_w_req_bits_T_1_data_0 = mux(_DataAccessesRRsp_DataAccess_io_w_req_bits_T, DataAccessMissRspSRAMWReq_0_data_0, DataAccessWriteHitSRAMWReq_0_data_0) @[DCache.scala 400:36]
    node _WIRE_8_1 = _T_37 @[DCache.scala 316:{71,71}]
    node DataAccessMissRspSRAMWReq_0_data_1 = _WIRE_8_1 @[DCache.scala 312:64 316:39]
    node _WIRE__1 = _T_1 @[DCache.scala 216:{101,101}]
    node DataAccessWriteHitSRAMWReq_0_data_1 = _WIRE__1 @[DCache.scala 211:65 216:40]
    node _DataAccessesRRsp_DataAccess_io_w_req_bits_T_1_data_1 = mux(_DataAccessesRRsp_DataAccess_io_w_req_bits_T, DataAccessMissRspSRAMWReq_0_data_1, DataAccessWriteHitSRAMWReq_0_data_1) @[DCache.scala 400:36]
    node _WIRE_8_2 = _T_38 @[DCache.scala 316:{71,71}]
    node DataAccessMissRspSRAMWReq_0_data_2 = _WIRE_8_2 @[DCache.scala 312:64 316:39]
    node _WIRE__2 = _T_2 @[DCache.scala 216:{101,101}]
    node DataAccessWriteHitSRAMWReq_0_data_2 = _WIRE__2 @[DCache.scala 211:65 216:40]
    node _DataAccessesRRsp_DataAccess_io_w_req_bits_T_1_data_2 = mux(_DataAccessesRRsp_DataAccess_io_w_req_bits_T, DataAccessMissRspSRAMWReq_0_data_2, DataAccessWriteHitSRAMWReq_0_data_2) @[DCache.scala 400:36]
    node _WIRE_8_3 = _T_39 @[DCache.scala 316:{71,71}]
    node DataAccessMissRspSRAMWReq_0_data_3 = _WIRE_8_3 @[DCache.scala 312:64 316:39]
    node _WIRE__3 = _T_3 @[DCache.scala 216:{101,101}]
    node DataAccessWriteHitSRAMWReq_0_data_3 = _WIRE__3 @[DCache.scala 211:65 216:40]
    node _DataAccessesRRsp_DataAccess_io_w_req_bits_T_1_data_3 = mux(_DataAccessesRRsp_DataAccess_io_w_req_bits_T, DataAccessMissRspSRAMWReq_0_data_3, DataAccessWriteHitSRAMWReq_0_data_3) @[DCache.scala 400:36]
    node DataAccessMissRspSRAMWReq_0_waymask = _DataAccessMissRspSRAMWReq_0_waymask_T @[DCache.scala 312:64 314:51]
    node _coreReq_Q_io_deq_bits_perLaneAddr_getBankEn_io_perBankBlockIdx_0_wordOffset1H = _GEN_10 @[DCache.scala 215:47]
    node DataAccessWriteHitSRAMWReq_0_waymask = _coreReq_Q_io_deq_bits_perLaneAddr_getBankEn_io_perBankBlockIdx_0_wordOffset1H @[DCache.scala 211:65 215:47]
    node _DataAccessesRRsp_DataAccess_io_w_req_bits_T_1_waymask = mux(_DataAccessesRRsp_DataAccess_io_w_req_bits_T, DataAccessMissRspSRAMWReq_0_waymask, DataAccessWriteHitSRAMWReq_0_waymask) @[DCache.scala 400:36]
    node _DataAccessesRRsp_DataAccess_io_r_req_valid_T = or(readHit_st1, dataReplaceReadValid) @[DCache.scala 420:46]
    node DataAccessReplaceReadSRAMRReq_0_setIdx = bits(_DataAccessReplaceReadSRAMRReq_0_setIdx_T, 2, 0) @[DCache.scala 306:43 307:50]
    node DataAccessReadHitSRAMRReq_0_setIdx = bits(_DataAccessReadHitSRAMRReq_0_setIdx_T, 2, 0) @[DCache.scala 220:39 221:46]
    node _DataAccessesRRsp_DataAccess_io_r_req_bits_T_setIdx = mux(dataReplaceReadValid, DataAccessReplaceReadSRAMRReq_0_setIdx, DataAccessReadHitSRAMRReq_0_setIdx) @[DCache.scala 427:36]
    node DataAccessesRRsp_lo = cat(DataAccessesRRsp_DataAccess.io_r_resp_data_1, DataAccessesRRsp_DataAccess.io_r_resp_data_0) @[Cat.scala 31:58]
    node DataAccessesRRsp_hi = cat(DataAccessesRRsp_DataAccess.io_r_resp_data_3, DataAccessesRRsp_DataAccess.io_r_resp_data_2) @[Cat.scala 31:58]
    node DataAccessesRRsp_0 = cat(DataAccessesRRsp_hi, DataAccessesRRsp_lo) @[Cat.scala 31:58]
    node _DataAccessesRRsp_DataAccess_io_w_req_valid_T_2 = and(writeHit_st1, getBankEn.io_perBankValid_1) @[DCache.scala 393:20]
    node _DataAccessesRRsp_DataAccess_io_w_req_valid_T_3 = mux(dataFillVaild, UInt<1>("h1"), _DataAccessesRRsp_DataAccess_io_w_req_valid_T_2) @[DCache.scala 391:37]
    node _DataAccessesRRsp_DataAccess_io_w_req_bits_T_2 = and(memRsp_Q.io_deq_valid, memRspIsRead) @[DCache.scala 400:59]
    node DataAccessMissRspSRAMWReq_1_setIdx = bits(_DataAccessMissRspSRAMWReq_1_setIdx_T, 2, 0) @[DCache.scala 312:64 313:46]
    node DataAccessWriteHitSRAMWReq_1_setIdx = bits(_DataAccessWriteHitSRAMWReq_1_setIdx_T, 2, 0) @[DCache.scala 211:65 213:47]
    node _DataAccessesRRsp_DataAccess_io_w_req_bits_T_3_setIdx = mux(_DataAccessesRRsp_DataAccess_io_w_req_bits_T_2, DataAccessMissRspSRAMWReq_1_setIdx, DataAccessWriteHitSRAMWReq_1_setIdx) @[DCache.scala 400:36]
    node _WIRE_10_0 = _T_40 @[DCache.scala 316:{71,71}]
    node DataAccessMissRspSRAMWReq_1_data_0 = _WIRE_10_0 @[DCache.scala 312:64 316:39]
    node _WIRE_2_0 = _T_4 @[DCache.scala 216:{101,101}]
    node DataAccessWriteHitSRAMWReq_1_data_0 = _WIRE_2_0 @[DCache.scala 211:65 216:40]
    node _DataAccessesRRsp_DataAccess_io_w_req_bits_T_3_data_0 = mux(_DataAccessesRRsp_DataAccess_io_w_req_bits_T_2, DataAccessMissRspSRAMWReq_1_data_0, DataAccessWriteHitSRAMWReq_1_data_0) @[DCache.scala 400:36]
    node _WIRE_10_1 = _T_41 @[DCache.scala 316:{71,71}]
    node DataAccessMissRspSRAMWReq_1_data_1 = _WIRE_10_1 @[DCache.scala 312:64 316:39]
    node _WIRE_2_1 = _T_5 @[DCache.scala 216:{101,101}]
    node DataAccessWriteHitSRAMWReq_1_data_1 = _WIRE_2_1 @[DCache.scala 211:65 216:40]
    node _DataAccessesRRsp_DataAccess_io_w_req_bits_T_3_data_1 = mux(_DataAccessesRRsp_DataAccess_io_w_req_bits_T_2, DataAccessMissRspSRAMWReq_1_data_1, DataAccessWriteHitSRAMWReq_1_data_1) @[DCache.scala 400:36]
    node _WIRE_10_2 = _T_42 @[DCache.scala 316:{71,71}]
    node DataAccessMissRspSRAMWReq_1_data_2 = _WIRE_10_2 @[DCache.scala 312:64 316:39]
    node _WIRE_2_2 = _T_6 @[DCache.scala 216:{101,101}]
    node DataAccessWriteHitSRAMWReq_1_data_2 = _WIRE_2_2 @[DCache.scala 211:65 216:40]
    node _DataAccessesRRsp_DataAccess_io_w_req_bits_T_3_data_2 = mux(_DataAccessesRRsp_DataAccess_io_w_req_bits_T_2, DataAccessMissRspSRAMWReq_1_data_2, DataAccessWriteHitSRAMWReq_1_data_2) @[DCache.scala 400:36]
    node _WIRE_10_3 = _T_43 @[DCache.scala 316:{71,71}]
    node DataAccessMissRspSRAMWReq_1_data_3 = _WIRE_10_3 @[DCache.scala 312:64 316:39]
    node _WIRE_2_3 = _T_7 @[DCache.scala 216:{101,101}]
    node DataAccessWriteHitSRAMWReq_1_data_3 = _WIRE_2_3 @[DCache.scala 211:65 216:40]
    node _DataAccessesRRsp_DataAccess_io_w_req_bits_T_3_data_3 = mux(_DataAccessesRRsp_DataAccess_io_w_req_bits_T_2, DataAccessMissRspSRAMWReq_1_data_3, DataAccessWriteHitSRAMWReq_1_data_3) @[DCache.scala 400:36]
    node DataAccessMissRspSRAMWReq_1_waymask = _DataAccessMissRspSRAMWReq_1_waymask_T @[DCache.scala 312:64 314:51]
    node _coreReq_Q_io_deq_bits_perLaneAddr_getBankEn_io_perBankBlockIdx_1_wordOffset1H = _GEN_18 @[DCache.scala 215:47]
    node DataAccessWriteHitSRAMWReq_1_waymask = _coreReq_Q_io_deq_bits_perLaneAddr_getBankEn_io_perBankBlockIdx_1_wordOffset1H @[DCache.scala 211:65 215:47]
    node _DataAccessesRRsp_DataAccess_io_w_req_bits_T_3_waymask = mux(_DataAccessesRRsp_DataAccess_io_w_req_bits_T_2, DataAccessMissRspSRAMWReq_1_waymask, DataAccessWriteHitSRAMWReq_1_waymask) @[DCache.scala 400:36]
    node _DataAccessesRRsp_DataAccess_io_r_req_valid_T_1 = or(readHit_st1, dataReplaceReadValid) @[DCache.scala 420:46]
    node DataAccessReplaceReadSRAMRReq_1_setIdx = bits(_DataAccessReplaceReadSRAMRReq_1_setIdx_T, 2, 0) @[DCache.scala 306:43 307:50]
    node DataAccessReadHitSRAMRReq_1_setIdx = bits(_DataAccessReadHitSRAMRReq_1_setIdx_T, 2, 0) @[DCache.scala 220:39 221:46]
    node _DataAccessesRRsp_DataAccess_io_r_req_bits_T_1_setIdx = mux(dataReplaceReadValid, DataAccessReplaceReadSRAMRReq_1_setIdx, DataAccessReadHitSRAMRReq_1_setIdx) @[DCache.scala 427:36]
    node DataAccessesRRsp_lo_1 = cat(DataAccessesRRsp_DataAccess_1.io_r_resp_data_1, DataAccessesRRsp_DataAccess_1.io_r_resp_data_0) @[Cat.scala 31:58]
    node DataAccessesRRsp_hi_1 = cat(DataAccessesRRsp_DataAccess_1.io_r_resp_data_3, DataAccessesRRsp_DataAccess_1.io_r_resp_data_2) @[Cat.scala 31:58]
    node DataAccessesRRsp_1 = cat(DataAccessesRRsp_hi_1, DataAccessesRRsp_lo_1) @[Cat.scala 31:58]
    node _DataAccessesRRsp_DataAccess_io_w_req_valid_T_4 = and(writeHit_st1, getBankEn.io_perBankValid_2) @[DCache.scala 393:20]
    node _DataAccessesRRsp_DataAccess_io_w_req_valid_T_5 = mux(dataFillVaild, UInt<1>("h1"), _DataAccessesRRsp_DataAccess_io_w_req_valid_T_4) @[DCache.scala 391:37]
    node _DataAccessesRRsp_DataAccess_io_w_req_bits_T_4 = and(memRsp_Q.io_deq_valid, memRspIsRead) @[DCache.scala 400:59]
    node DataAccessMissRspSRAMWReq_2_setIdx = bits(_DataAccessMissRspSRAMWReq_2_setIdx_T, 2, 0) @[DCache.scala 312:64 313:46]
    node DataAccessWriteHitSRAMWReq_2_setIdx = bits(_DataAccessWriteHitSRAMWReq_2_setIdx_T, 2, 0) @[DCache.scala 211:65 213:47]
    node _DataAccessesRRsp_DataAccess_io_w_req_bits_T_5_setIdx = mux(_DataAccessesRRsp_DataAccess_io_w_req_bits_T_4, DataAccessMissRspSRAMWReq_2_setIdx, DataAccessWriteHitSRAMWReq_2_setIdx) @[DCache.scala 400:36]
    node _WIRE_12_0 = _T_44 @[DCache.scala 316:{71,71}]
    node DataAccessMissRspSRAMWReq_2_data_0 = _WIRE_12_0 @[DCache.scala 312:64 316:39]
    node _WIRE_4_0 = _T_8 @[DCache.scala 216:{101,101}]
    node DataAccessWriteHitSRAMWReq_2_data_0 = _WIRE_4_0 @[DCache.scala 211:65 216:40]
    node _DataAccessesRRsp_DataAccess_io_w_req_bits_T_5_data_0 = mux(_DataAccessesRRsp_DataAccess_io_w_req_bits_T_4, DataAccessMissRspSRAMWReq_2_data_0, DataAccessWriteHitSRAMWReq_2_data_0) @[DCache.scala 400:36]
    node _WIRE_12_1 = _T_45 @[DCache.scala 316:{71,71}]
    node DataAccessMissRspSRAMWReq_2_data_1 = _WIRE_12_1 @[DCache.scala 312:64 316:39]
    node _WIRE_4_1 = _T_9 @[DCache.scala 216:{101,101}]
    node DataAccessWriteHitSRAMWReq_2_data_1 = _WIRE_4_1 @[DCache.scala 211:65 216:40]
    node _DataAccessesRRsp_DataAccess_io_w_req_bits_T_5_data_1 = mux(_DataAccessesRRsp_DataAccess_io_w_req_bits_T_4, DataAccessMissRspSRAMWReq_2_data_1, DataAccessWriteHitSRAMWReq_2_data_1) @[DCache.scala 400:36]
    node _WIRE_12_2 = _T_46 @[DCache.scala 316:{71,71}]
    node DataAccessMissRspSRAMWReq_2_data_2 = _WIRE_12_2 @[DCache.scala 312:64 316:39]
    node _WIRE_4_2 = _T_10 @[DCache.scala 216:{101,101}]
    node DataAccessWriteHitSRAMWReq_2_data_2 = _WIRE_4_2 @[DCache.scala 211:65 216:40]
    node _DataAccessesRRsp_DataAccess_io_w_req_bits_T_5_data_2 = mux(_DataAccessesRRsp_DataAccess_io_w_req_bits_T_4, DataAccessMissRspSRAMWReq_2_data_2, DataAccessWriteHitSRAMWReq_2_data_2) @[DCache.scala 400:36]
    node _WIRE_12_3 = _T_47 @[DCache.scala 316:{71,71}]
    node DataAccessMissRspSRAMWReq_2_data_3 = _WIRE_12_3 @[DCache.scala 312:64 316:39]
    node _WIRE_4_3 = _T_11 @[DCache.scala 216:{101,101}]
    node DataAccessWriteHitSRAMWReq_2_data_3 = _WIRE_4_3 @[DCache.scala 211:65 216:40]
    node _DataAccessesRRsp_DataAccess_io_w_req_bits_T_5_data_3 = mux(_DataAccessesRRsp_DataAccess_io_w_req_bits_T_4, DataAccessMissRspSRAMWReq_2_data_3, DataAccessWriteHitSRAMWReq_2_data_3) @[DCache.scala 400:36]
    node DataAccessMissRspSRAMWReq_2_waymask = _DataAccessMissRspSRAMWReq_2_waymask_T @[DCache.scala 312:64 314:51]
    node _coreReq_Q_io_deq_bits_perLaneAddr_getBankEn_io_perBankBlockIdx_2_wordOffset1H = _GEN_26 @[DCache.scala 215:47]
    node DataAccessWriteHitSRAMWReq_2_waymask = _coreReq_Q_io_deq_bits_perLaneAddr_getBankEn_io_perBankBlockIdx_2_wordOffset1H @[DCache.scala 211:65 215:47]
    node _DataAccessesRRsp_DataAccess_io_w_req_bits_T_5_waymask = mux(_DataAccessesRRsp_DataAccess_io_w_req_bits_T_4, DataAccessMissRspSRAMWReq_2_waymask, DataAccessWriteHitSRAMWReq_2_waymask) @[DCache.scala 400:36]
    node _DataAccessesRRsp_DataAccess_io_r_req_valid_T_2 = or(readHit_st1, dataReplaceReadValid) @[DCache.scala 420:46]
    node DataAccessReplaceReadSRAMRReq_2_setIdx = bits(_DataAccessReplaceReadSRAMRReq_2_setIdx_T, 2, 0) @[DCache.scala 306:43 307:50]
    node DataAccessReadHitSRAMRReq_2_setIdx = bits(_DataAccessReadHitSRAMRReq_2_setIdx_T, 2, 0) @[DCache.scala 220:39 221:46]
    node _DataAccessesRRsp_DataAccess_io_r_req_bits_T_2_setIdx = mux(dataReplaceReadValid, DataAccessReplaceReadSRAMRReq_2_setIdx, DataAccessReadHitSRAMRReq_2_setIdx) @[DCache.scala 427:36]
    node DataAccessesRRsp_lo_2 = cat(DataAccessesRRsp_DataAccess_2.io_r_resp_data_1, DataAccessesRRsp_DataAccess_2.io_r_resp_data_0) @[Cat.scala 31:58]
    node DataAccessesRRsp_hi_2 = cat(DataAccessesRRsp_DataAccess_2.io_r_resp_data_3, DataAccessesRRsp_DataAccess_2.io_r_resp_data_2) @[Cat.scala 31:58]
    node DataAccessesRRsp_2 = cat(DataAccessesRRsp_hi_2, DataAccessesRRsp_lo_2) @[Cat.scala 31:58]
    node _DataAccessesRRsp_DataAccess_io_w_req_valid_T_6 = and(writeHit_st1, getBankEn.io_perBankValid_3) @[DCache.scala 393:20]
    node _DataAccessesRRsp_DataAccess_io_w_req_valid_T_7 = mux(dataFillVaild, UInt<1>("h1"), _DataAccessesRRsp_DataAccess_io_w_req_valid_T_6) @[DCache.scala 391:37]
    node _DataAccessesRRsp_DataAccess_io_w_req_bits_T_6 = and(memRsp_Q.io_deq_valid, memRspIsRead) @[DCache.scala 400:59]
    node DataAccessMissRspSRAMWReq_3_setIdx = bits(_DataAccessMissRspSRAMWReq_3_setIdx_T, 2, 0) @[DCache.scala 312:64 313:46]
    node DataAccessWriteHitSRAMWReq_3_setIdx = bits(_DataAccessWriteHitSRAMWReq_3_setIdx_T, 2, 0) @[DCache.scala 211:65 213:47]
    node _DataAccessesRRsp_DataAccess_io_w_req_bits_T_7_setIdx = mux(_DataAccessesRRsp_DataAccess_io_w_req_bits_T_6, DataAccessMissRspSRAMWReq_3_setIdx, DataAccessWriteHitSRAMWReq_3_setIdx) @[DCache.scala 400:36]
    node _WIRE_14_0 = _T_48 @[DCache.scala 316:{71,71}]
    node DataAccessMissRspSRAMWReq_3_data_0 = _WIRE_14_0 @[DCache.scala 312:64 316:39]
    node _WIRE_6_0 = _T_12 @[DCache.scala 216:{101,101}]
    node DataAccessWriteHitSRAMWReq_3_data_0 = _WIRE_6_0 @[DCache.scala 211:65 216:40]
    node _DataAccessesRRsp_DataAccess_io_w_req_bits_T_7_data_0 = mux(_DataAccessesRRsp_DataAccess_io_w_req_bits_T_6, DataAccessMissRspSRAMWReq_3_data_0, DataAccessWriteHitSRAMWReq_3_data_0) @[DCache.scala 400:36]
    node _WIRE_14_1 = _T_49 @[DCache.scala 316:{71,71}]
    node DataAccessMissRspSRAMWReq_3_data_1 = _WIRE_14_1 @[DCache.scala 312:64 316:39]
    node _WIRE_6_1 = _T_13 @[DCache.scala 216:{101,101}]
    node DataAccessWriteHitSRAMWReq_3_data_1 = _WIRE_6_1 @[DCache.scala 211:65 216:40]
    node _DataAccessesRRsp_DataAccess_io_w_req_bits_T_7_data_1 = mux(_DataAccessesRRsp_DataAccess_io_w_req_bits_T_6, DataAccessMissRspSRAMWReq_3_data_1, DataAccessWriteHitSRAMWReq_3_data_1) @[DCache.scala 400:36]
    node _WIRE_14_2 = _T_50 @[DCache.scala 316:{71,71}]
    node DataAccessMissRspSRAMWReq_3_data_2 = _WIRE_14_2 @[DCache.scala 312:64 316:39]
    node _WIRE_6_2 = _T_14 @[DCache.scala 216:{101,101}]
    node DataAccessWriteHitSRAMWReq_3_data_2 = _WIRE_6_2 @[DCache.scala 211:65 216:40]
    node _DataAccessesRRsp_DataAccess_io_w_req_bits_T_7_data_2 = mux(_DataAccessesRRsp_DataAccess_io_w_req_bits_T_6, DataAccessMissRspSRAMWReq_3_data_2, DataAccessWriteHitSRAMWReq_3_data_2) @[DCache.scala 400:36]
    node _WIRE_14_3 = _T_51 @[DCache.scala 316:{71,71}]
    node DataAccessMissRspSRAMWReq_3_data_3 = _WIRE_14_3 @[DCache.scala 312:64 316:39]
    node _WIRE_6_3 = _T_15 @[DCache.scala 216:{101,101}]
    node DataAccessWriteHitSRAMWReq_3_data_3 = _WIRE_6_3 @[DCache.scala 211:65 216:40]
    node _DataAccessesRRsp_DataAccess_io_w_req_bits_T_7_data_3 = mux(_DataAccessesRRsp_DataAccess_io_w_req_bits_T_6, DataAccessMissRspSRAMWReq_3_data_3, DataAccessWriteHitSRAMWReq_3_data_3) @[DCache.scala 400:36]
    node DataAccessMissRspSRAMWReq_3_waymask = _DataAccessMissRspSRAMWReq_3_waymask_T @[DCache.scala 312:64 314:51]
    node _coreReq_Q_io_deq_bits_perLaneAddr_getBankEn_io_perBankBlockIdx_3_wordOffset1H = _GEN_34 @[DCache.scala 215:47]
    node DataAccessWriteHitSRAMWReq_3_waymask = _coreReq_Q_io_deq_bits_perLaneAddr_getBankEn_io_perBankBlockIdx_3_wordOffset1H @[DCache.scala 211:65 215:47]
    node _DataAccessesRRsp_DataAccess_io_w_req_bits_T_7_waymask = mux(_DataAccessesRRsp_DataAccess_io_w_req_bits_T_6, DataAccessMissRspSRAMWReq_3_waymask, DataAccessWriteHitSRAMWReq_3_waymask) @[DCache.scala 400:36]
    node _DataAccessesRRsp_DataAccess_io_r_req_valid_T_3 = or(readHit_st1, dataReplaceReadValid) @[DCache.scala 420:46]
    node DataAccessReplaceReadSRAMRReq_3_setIdx = bits(_DataAccessReplaceReadSRAMRReq_3_setIdx_T, 2, 0) @[DCache.scala 306:43 307:50]
    node DataAccessReadHitSRAMRReq_3_setIdx = bits(_DataAccessReadHitSRAMRReq_3_setIdx_T, 2, 0) @[DCache.scala 220:39 221:46]
    node _DataAccessesRRsp_DataAccess_io_r_req_bits_T_3_setIdx = mux(dataReplaceReadValid, DataAccessReplaceReadSRAMRReq_3_setIdx, DataAccessReadHitSRAMRReq_3_setIdx) @[DCache.scala 427:36]
    node DataAccessesRRsp_lo_3 = cat(DataAccessesRRsp_DataAccess_3.io_r_resp_data_1, DataAccessesRRsp_DataAccess_3.io_r_resp_data_0) @[Cat.scala 31:58]
    node DataAccessesRRsp_hi_3 = cat(DataAccessesRRsp_DataAccess_3.io_r_resp_data_3, DataAccessesRRsp_DataAccess_3.io_r_resp_data_2) @[Cat.scala 31:58]
    node DataAccessesRRsp_3 = cat(DataAccessesRRsp_hi_3, DataAccessesRRsp_lo_3) @[Cat.scala 31:58]
    node _T_53 = and(io_coreReq_ready, io_coreReq_valid) @[Decoupled.scala 50:35]
    reg REG : UInt<1>, clock with :
      reset => (UInt<1>("h0"), REG) @[DCache.scala 435:31]
    node _T_54 = and(cacheHit_st1, REG) @[DCache.scala 435:21]
    node _GEN_60 = mux(MshrAccess.io_missRspOut_valid, memRsp_st1_d_data_0, coreRsp_st2_data_0) @[DCache.scala 439:45 441:22 139:23]
    node _GEN_61 = mux(MshrAccess.io_missRspOut_valid, memRsp_st1_d_data_1, coreRsp_st2_data_1) @[DCache.scala 439:45 441:22 139:23]
    node _GEN_62 = mux(MshrAccess.io_missRspOut_valid, memRsp_st1_d_data_2, coreRsp_st2_data_2) @[DCache.scala 439:45 441:22 139:23]
    node _GEN_63 = mux(MshrAccess.io_missRspOut_valid, memRsp_st1_d_data_3, coreRsp_st2_data_3) @[DCache.scala 439:45 441:22 139:23]
    node _GEN_64 = mux(MshrAccess.io_missRspOut_valid, UInt<1>("h0"), coreRsp_st2_isWrite) @[DCache.scala 139:23 439:45 442:25]
    node _GEN_65 = validif(eq(_T_54, UInt<1>("h0")), _GEN_60) @[DCache.scala 435:50]
    node _GEN_66 = validif(eq(_T_54, UInt<1>("h0")), _GEN_61) @[DCache.scala 435:50]
    node _GEN_67 = validif(eq(_T_54, UInt<1>("h0")), _GEN_62) @[DCache.scala 435:50]
    node _GEN_68 = validif(eq(_T_54, UInt<1>("h0")), _GEN_63) @[DCache.scala 435:50]
    node _GEN_69 = mux(_T_54, coreReqControl_st1_isWrite, _GEN_64) @[DCache.scala 435:50 438:25]
    node _T_55 = and(io_coreReq_ready, io_coreReq_valid) @[Decoupled.scala 50:35]
    reg REG_1 : UInt<1>, clock with :
      reset => (UInt<1>("h0"), REG_1) @[DCache.scala 445:32]
    node _T_56 = and(cacheHit_st1, REG_1) @[DCache.scala 445:22]
    node _T_57 = and(secondaryFullReturn, MshrAccess.io_missRspOut_valid) @[DCache.scala 445:75]
    node _T_58 = or(_T_56, _T_57) @[DCache.scala 445:51]
    node _missRspTI_st1_WIRE_instrId = _missRspTI_st1_T_12 @[DCache.scala 353:{69,69}]
    node missRspTI_st1_instrId = _missRspTI_st1_WIRE_instrId @[DCache.scala 353:17 94:27]
    node _GEN_70 = mux(MshrAccess.io_missRspOut_valid, missRspTI_st1_instrId, coreRsp_st2_instrId) @[DCache.scala 139:23 448:45 449:25]
    node _missRspTI_st1_WIRE_perLaneAddr_0_activeMask = _missRspTI_st1_T_2 @[DCache.scala 353:{69,69}]
    node missRspTI_st1_perLaneAddr_0_activeMask = _missRspTI_st1_WIRE_perLaneAddr_0_activeMask @[DCache.scala 353:17 94:27]
    node _GEN_71 = mux(MshrAccess.io_missRspOut_valid, missRspTI_st1_perLaneAddr_0_activeMask, coreRsp_st2_activeMask_0) @[DCache.scala 139:23 448:45 450:28]
    node _missRspTI_st1_WIRE_perLaneAddr_1_activeMask = _missRspTI_st1_T_5 @[DCache.scala 353:{69,69}]
    node missRspTI_st1_perLaneAddr_1_activeMask = _missRspTI_st1_WIRE_perLaneAddr_1_activeMask @[DCache.scala 353:17 94:27]
    node _GEN_72 = mux(MshrAccess.io_missRspOut_valid, missRspTI_st1_perLaneAddr_1_activeMask, coreRsp_st2_activeMask_1) @[DCache.scala 139:23 448:45 450:28]
    node _missRspTI_st1_WIRE_perLaneAddr_2_activeMask = _missRspTI_st1_T_8 @[DCache.scala 353:{69,69}]
    node missRspTI_st1_perLaneAddr_2_activeMask = _missRspTI_st1_WIRE_perLaneAddr_2_activeMask @[DCache.scala 353:17 94:27]
    node _GEN_73 = mux(MshrAccess.io_missRspOut_valid, missRspTI_st1_perLaneAddr_2_activeMask, coreRsp_st2_activeMask_2) @[DCache.scala 139:23 448:45 450:28]
    node _missRspTI_st1_WIRE_perLaneAddr_3_activeMask = _missRspTI_st1_T_11 @[DCache.scala 353:{69,69}]
    node missRspTI_st1_perLaneAddr_3_activeMask = _missRspTI_st1_WIRE_perLaneAddr_3_activeMask @[DCache.scala 353:17 94:27]
    node _GEN_74 = mux(MshrAccess.io_missRspOut_valid, missRspTI_st1_perLaneAddr_3_activeMask, coreRsp_st2_activeMask_3) @[DCache.scala 139:23 448:45 450:28]
    node _GEN_75 = mux(_T_58, coreReq_Q.io_deq_bits_instrId, _GEN_70) @[DCache.scala 445:110 446:25]
    node _GEN_76 = mux(_T_58, coreReq_Q.io_deq_bits_perLaneAddr_0_activeMask, _GEN_71) @[DCache.scala 445:110 447:28]
    node _GEN_77 = mux(_T_58, coreReq_Q.io_deq_bits_perLaneAddr_1_activeMask, _GEN_72) @[DCache.scala 445:110 447:28]
    node _GEN_78 = mux(_T_58, coreReq_Q.io_deq_bits_perLaneAddr_2_activeMask, _GEN_73) @[DCache.scala 445:110 447:28]
    node _GEN_79 = mux(_T_58, coreReq_Q.io_deq_bits_perLaneAddr_3_activeMask, _GEN_74) @[DCache.scala 445:110 447:28]
    node _coreRsp_st2_valid_from_coreReq_T = or(readHit_st1, writeHit_st1) @[DCache.scala 461:18]
    node _coreRsp_st2_valid_from_coreReq_T_1 = and(coreReq_st1_valid, _coreRsp_st2_valid_from_coreReq_T) @[DCache.scala 460:63]
    reg coreRsp_st2_valid_from_coreReq_REG : UInt<1>, clock with :
      reset => (UInt<1>("h0"), coreRsp_st2_valid_from_coreReq_REG) @[DCache.scala 460:44]
    reg coreRsp_st2_valid_from_memRsp_REG : UInt<1>, clock with :
      reset => (UInt<1>("h0"), coreRsp_st2_valid_from_memRsp_REG) @[DCache.scala 462:43]
    node coreRsp_st2_valid_from_coreReq = coreRsp_st2_valid_from_coreReq_REG @[DCache.scala 454:44 460:34]
    node coreRsp_st2_valid_from_memRsp = coreRsp_st2_valid_from_memRsp_REG @[DCache.scala 455:43 462:33]
    node _T_59 = and(coreRsp_st2_valid_from_coreReq, coreRsp_st2_valid_from_memRsp) @[DCache.scala 463:44]
    node _T_60 = eq(_T_59, UInt<1>("h0")) @[DCache.scala 463:11]
    node _T_61 = asUInt(reset) @[DCache.scala 463:10]
    node _T_62 = eq(_T_61, UInt<1>("h0")) @[DCache.scala 463:10]
    node _T_63 = eq(_T_60, UInt<1>("h0")) @[DCache.scala 463:10]
    node _coreRsp_st2_valid_from_memReq_T = and(WshrAccess.io_pushReq_valid, memReq_Q.io_deq_bits_hasCoreRsp) @[DCache.scala 499:64]
    node coreRsp_st2_valid_from_memReq = _coreRsp_st2_valid_from_memReq_T @[DCache.scala 456:43 499:33]
    node _T_64 = and(coreRsp_st2_valid_from_coreReq, coreRsp_st2_valid_from_memReq) @[DCache.scala 464:44]
    node _T_65 = eq(_T_64, UInt<1>("h0")) @[DCache.scala 464:11]
    node _T_66 = asUInt(reset) @[DCache.scala 464:10]
    node _T_67 = eq(_T_66, UInt<1>("h0")) @[DCache.scala 464:10]
    node _T_68 = eq(_T_65, UInt<1>("h0")) @[DCache.scala 464:10]
    node _T_69 = and(coreRsp_st2_valid_from_memReq, coreRsp_st2_valid_from_memRsp) @[DCache.scala 465:43]
    node _T_70 = eq(_T_69, UInt<1>("h0")) @[DCache.scala 465:11]
    node _T_71 = asUInt(reset) @[DCache.scala 465:10]
    node _T_72 = eq(_T_71, UInt<1>("h0")) @[DCache.scala 465:10]
    node _T_73 = eq(_T_70, UInt<1>("h0")) @[DCache.scala 465:10]
    node _coreRsp_st2_valid_T = or(coreRsp_st2_valid_from_coreReq, coreRsp_st2_valid_from_memRsp) @[DCache.scala 466:55]
    node _coreRsp_st2_valid_T_1 = or(_coreRsp_st2_valid_T, coreRsp_st2_valid_from_memReq) @[DCache.scala 466:88]
    node coreRspFromMemReq_instrId = bits(memReq_Q.io_deq_bits_coreRspInstrId, 2, 0) @[DCache.scala 458:31 517:29]
    node _coreRsp_Q_io_enq_bits_T_instrId = mux(coreRsp_st2_valid_from_memReq, coreRspFromMemReq_instrId, coreRsp_st2_instrId) @[DCache.scala 470:31]
    node coreRspFromMemReq_isWrite = UInt<1>("h1") @[DCache.scala 458:31 515:29]
    node _coreRsp_Q_io_enq_bits_T_isWrite = mux(coreRsp_st2_valid_from_memReq, coreRspFromMemReq_isWrite, coreRsp_st2_isWrite) @[DCache.scala 470:31]
    node coreRspFromMemReq_data_0 = validif(UInt<1>("h0"), UInt<32>("h0"))
    node _coreRsp_Q_io_enq_bits_T_data_0 = mux(coreRsp_st2_valid_from_memReq, coreRspFromMemReq_data_0, coreRsp_st2_data_0) @[DCache.scala 470:31]
    node coreRspFromMemReq_data_1 = validif(UInt<1>("h0"), UInt<32>("h0"))
    node _coreRsp_Q_io_enq_bits_T_data_1 = mux(coreRsp_st2_valid_from_memReq, coreRspFromMemReq_data_1, coreRsp_st2_data_1) @[DCache.scala 470:31]
    node coreRspFromMemReq_data_2 = validif(UInt<1>("h0"), UInt<32>("h0"))
    node _coreRsp_Q_io_enq_bits_T_data_2 = mux(coreRsp_st2_valid_from_memReq, coreRspFromMemReq_data_2, coreRsp_st2_data_2) @[DCache.scala 470:31]
    node coreRspFromMemReq_data_3 = validif(UInt<1>("h0"), UInt<32>("h0"))
    node _coreRsp_Q_io_enq_bits_T_data_3 = mux(coreRsp_st2_valid_from_memReq, coreRspFromMemReq_data_3, coreRsp_st2_data_3) @[DCache.scala 470:31]
    node _WIRE_17_0 = UInt<1>("h1") @[DCache.scala 518:{42,42}]
    node coreRspFromMemReq_activeMask_0 = _WIRE_17_0 @[DCache.scala 458:31 518:32]
    node _coreRsp_Q_io_enq_bits_T_activeMask_0 = mux(coreRsp_st2_valid_from_memReq, coreRspFromMemReq_activeMask_0, coreRsp_st2_activeMask_0) @[DCache.scala 470:31]
    node _WIRE_17_1 = UInt<1>("h1") @[DCache.scala 518:{42,42}]
    node coreRspFromMemReq_activeMask_1 = _WIRE_17_1 @[DCache.scala 458:31 518:32]
    node _coreRsp_Q_io_enq_bits_T_activeMask_1 = mux(coreRsp_st2_valid_from_memReq, coreRspFromMemReq_activeMask_1, coreRsp_st2_activeMask_1) @[DCache.scala 470:31]
    node _WIRE_17_2 = UInt<1>("h1") @[DCache.scala 518:{42,42}]
    node coreRspFromMemReq_activeMask_2 = _WIRE_17_2 @[DCache.scala 458:31 518:32]
    node _coreRsp_Q_io_enq_bits_T_activeMask_2 = mux(coreRsp_st2_valid_from_memReq, coreRspFromMemReq_activeMask_2, coreRsp_st2_activeMask_2) @[DCache.scala 470:31]
    node _WIRE_17_3 = UInt<1>("h1") @[DCache.scala 518:{42,42}]
    node coreRspFromMemReq_activeMask_3 = _WIRE_17_3 @[DCache.scala 458:31 518:32]
    node _coreRsp_Q_io_enq_bits_T_activeMask_3 = mux(coreRsp_st2_valid_from_memReq, coreRspFromMemReq_activeMask_3, coreRsp_st2_activeMask_3) @[DCache.scala 470:31]
    node DataAccessReadSRAMRRsp_0 = DataAccessesRRsp_0 @[DCache.scala 430:{50,50}]
    node _GEN_80 = mux(readHit_st2, DataAccessReadSRAMRRsp_0, _coreRsp_Q_io_enq_bits_T_data_0) @[DCache.scala 471:20 470:25 472:32]
    node DataAccessReadSRAMRRsp_1 = DataAccessesRRsp_1 @[DCache.scala 430:{50,50}]
    node _GEN_81 = mux(readHit_st2, DataAccessReadSRAMRRsp_1, _coreRsp_Q_io_enq_bits_T_data_1) @[DCache.scala 471:20 470:25 472:32]
    node DataAccessReadSRAMRRsp_2 = DataAccessesRRsp_2 @[DCache.scala 430:{50,50}]
    node _GEN_82 = mux(readHit_st2, DataAccessReadSRAMRRsp_2, _coreRsp_Q_io_enq_bits_T_data_2) @[DCache.scala 471:20 470:25 472:32]
    node DataAccessReadSRAMRRsp_3 = DataAccessesRRsp_3 @[DCache.scala 430:{50,50}]
    node _GEN_83 = mux(readHit_st2, DataAccessReadSRAMRRsp_3, _coreRsp_Q_io_enq_bits_T_data_3) @[DCache.scala 471:20 470:25 472:32]
    reg dirtyReplace_st2_a_opcode : UInt<3>, clock with :
      reset => (UInt<1>("h0"), dirtyReplace_st2_a_opcode) @[DCache.scala 476:29]
    reg dirtyReplace_st2_a_param : UInt<3>, clock with :
      reset => (UInt<1>("h0"), dirtyReplace_st2_a_param) @[DCache.scala 476:29]
    reg dirtyReplace_st2_a_addr : UInt<32>, clock with :
      reset => (UInt<1>("h0"), dirtyReplace_st2_a_addr) @[DCache.scala 476:29]
    reg dirtyReplace_st2_a_data_0 : UInt<32>, clock with :
      reset => (UInt<1>("h0"), dirtyReplace_st2_a_data_0) @[DCache.scala 476:29]
    reg dirtyReplace_st2_a_data_1 : UInt<32>, clock with :
      reset => (UInt<1>("h0"), dirtyReplace_st2_a_data_1) @[DCache.scala 476:29]
    reg dirtyReplace_st2_a_data_2 : UInt<32>, clock with :
      reset => (UInt<1>("h0"), dirtyReplace_st2_a_data_2) @[DCache.scala 476:29]
    reg dirtyReplace_st2_a_data_3 : UInt<32>, clock with :
      reset => (UInt<1>("h0"), dirtyReplace_st2_a_data_3) @[DCache.scala 476:29]
    reg dirtyReplace_st2_a_mask_0 : UInt<1>, clock with :
      reset => (UInt<1>("h0"), dirtyReplace_st2_a_mask_0) @[DCache.scala 476:29]
    reg dirtyReplace_st2_a_mask_1 : UInt<1>, clock with :
      reset => (UInt<1>("h0"), dirtyReplace_st2_a_mask_1) @[DCache.scala 476:29]
    reg dirtyReplace_st2_a_mask_2 : UInt<1>, clock with :
      reset => (UInt<1>("h0"), dirtyReplace_st2_a_mask_2) @[DCache.scala 476:29]
    reg dirtyReplace_st2_a_mask_3 : UInt<1>, clock with :
      reset => (UInt<1>("h0"), dirtyReplace_st2_a_mask_3) @[DCache.scala 476:29]
    reg dirtyReplace_st2_a_source : UInt<7>, clock with :
      reset => (UInt<1>("h0"), dirtyReplace_st2_a_source) @[DCache.scala 476:29]
    reg dirtyReplace_st2_hasCoreRsp : UInt<1>, clock with :
      reset => (UInt<1>("h0"), dirtyReplace_st2_hasCoreRsp) @[DCache.scala 476:29]
    reg dirtyReplace_st2_coreRspInstrId : UInt<32>, clock with :
      reset => (UInt<1>("h0"), dirtyReplace_st2_coreRspInstrId) @[DCache.scala 476:29]
    node _MemReqArb_io_in_1_valid_T = eq(MshrAccess.io_probeOut_st1_probeStatus, UInt<1>("h0")) @[DCache.scala 484:103]
    node _MemReqArb_io_in_1_valid_T_1 = and(readMiss_st1, _MemReqArb_io_in_1_valid_T) @[DCache.scala 484:84]
    node _MemReqArb_io_in_1_valid_T_2 = or(writeMiss_st1, _MemReqArb_io_in_1_valid_T_1) @[DCache.scala 484:67]
    node _MemReqArb_io_in_1_valid_T_3 = and(coreReq_st1_valid, _MemReqArb_io_in_1_valid_T_2) @[DCache.scala 484:49]
    reg memReq_st3_a_opcode : UInt<3>, clock with :
      reset => (UInt<1>("h0"), memReq_st3_a_opcode) @[DCache.scala 488:23]
    reg memReq_st3_a_param : UInt<3>, clock with :
      reset => (UInt<1>("h0"), memReq_st3_a_param) @[DCache.scala 488:23]
    reg memReq_st3_a_addr : UInt<32>, clock with :
      reset => (UInt<1>("h0"), memReq_st3_a_addr) @[DCache.scala 488:23]
    reg memReq_st3_a_data_0 : UInt<32>, clock with :
      reset => (UInt<1>("h0"), memReq_st3_a_data_0) @[DCache.scala 488:23]
    reg memReq_st3_a_data_1 : UInt<32>, clock with :
      reset => (UInt<1>("h0"), memReq_st3_a_data_1) @[DCache.scala 488:23]
    reg memReq_st3_a_data_2 : UInt<32>, clock with :
      reset => (UInt<1>("h0"), memReq_st3_a_data_2) @[DCache.scala 488:23]
    reg memReq_st3_a_data_3 : UInt<32>, clock with :
      reset => (UInt<1>("h0"), memReq_st3_a_data_3) @[DCache.scala 488:23]
    reg memReq_st3_a_mask_0 : UInt<1>, clock with :
      reset => (UInt<1>("h0"), memReq_st3_a_mask_0) @[DCache.scala 488:23]
    reg memReq_st3_a_mask_1 : UInt<1>, clock with :
      reset => (UInt<1>("h0"), memReq_st3_a_mask_1) @[DCache.scala 488:23]
    reg memReq_st3_a_mask_2 : UInt<1>, clock with :
      reset => (UInt<1>("h0"), memReq_st3_a_mask_2) @[DCache.scala 488:23]
    reg memReq_st3_a_mask_3 : UInt<1>, clock with :
      reset => (UInt<1>("h0"), memReq_st3_a_mask_3) @[DCache.scala 488:23]
    reg memReq_st3_a_source : UInt<7>, clock with :
      reset => (UInt<1>("h0"), memReq_st3_a_source) @[DCache.scala 488:23]
    node _memReqIsWrite_st3_T = eq(memReq_Q.io_deq_bits_a_opcode, UInt<3>("h0")) @[DCache.scala 490:37]
    node _memReqIsWrite_st3_T_1 = eq(memReq_Q.io_deq_bits_a_opcode, UInt<3>("h1")) @[DCache.scala 490:70]
    node _memReqIsWrite_st3_T_2 = eq(memReq_Q.io_deq_bits_a_param, UInt<1>("h0")) @[DCache.scala 490:121]
    node _memReqIsWrite_st3_T_3 = and(_memReqIsWrite_st3_T_1, _memReqIsWrite_st3_T_2) @[DCache.scala 490:89]
    node memReqIsWrite_st3 = or(_memReqIsWrite_st3_T, _memReqIsWrite_st3_T_3) @[DCache.scala 490:56]
    node _memReqIsRead_st3_T = eq(memReq_Q.io_deq_bits_a_opcode, UInt<3>("h4")) @[DCache.scala 491:36]
    node _memReqIsRead_st3_T_1 = eq(memReq_Q.io_deq_bits_a_param, UInt<1>("h0")) @[DCache.scala 491:83]
    node memReqIsRead_st3 = and(_memReqIsRead_st3_T, _memReqIsRead_st3_T_1) @[DCache.scala 491:51]
    node _WshrAccess_io_pushReq_bits_blockAddr_T = shr(memReq_Q.io_deq_bits_a_addr, 4) @[DCache.scala 493:71]
    node _wshrProtect_T = or(memReqIsWrite_st3, memReqIsRead_st3) @[DCache.scala 494:66]
    node wshrProtect = and(WshrAccess.io_conflict, _wshrProtect_T) @[DCache.scala 494:44]
    node _cRspBlockedOrWshrFull_T = eq(coreRsp_Q.io_enq_ready, UInt<1>("h0")) @[DCache.scala 495:33]
    node _cRspBlockedOrWshrFull_T_1 = and(_cRspBlockedOrWshrFull_T, memReq_Q.io_deq_bits_hasCoreRsp) @[DCache.scala 495:57]
    node _cRspBlockedOrWshrFull_T_2 = eq(WshrAccess.io_pushReq_ready, UInt<1>("h0")) @[DCache.scala 496:8]
    node _cRspBlockedOrWshrFull_T_3 = or(_cRspBlockedOrWshrFull_T_1, _cRspBlockedOrWshrFull_T_2) @[DCache.scala 496:5]
    node cRspBlockedOrWshrFull = and(_cRspBlockedOrWshrFull_T_3, memReqIsWrite_st3) @[DCache.scala 496:38]
    node _wshrPass_T = eq(wshrProtect, UInt<1>("h0")) @[DCache.scala 497:18]
    node _wshrPass_T_1 = eq(cRspBlockedOrWshrFull, UInt<1>("h0")) @[DCache.scala 497:34]
    node wshrPass = and(_wshrPass_T, _wshrPass_T_1) @[DCache.scala 497:31]
    node _WshrAccess_io_pushReq_valid_T = and(wshrPass, memReq_Q.io_deq_valid) @[DCache.scala 498:43]
    node _WshrAccess_io_pushReq_valid_T_1 = and(_WshrAccess_io_pushReq_valid_T, memReqIsWrite_st3) @[DCache.scala 498:68]
    node _memReq_Q_io_deq_ready_T = and(wshrPass, io_memReq_ready) @[DCache.scala 501:37]
    node _T_74 = and(wshrPass, memReq_Q.io_deq_valid) @[DCache.scala 503:17]
    node _GEN_84 = mux(_T_74, memReq_Q.io_deq_bits_a_source, memReq_st3_a_source) @[DCache.scala 503:43 504:16 488:23]
    node _GEN_85 = mux(_T_74, memReq_Q.io_deq_bits_a_mask_0, memReq_st3_a_mask_0) @[DCache.scala 503:43 504:16 488:23]
    node _GEN_86 = mux(_T_74, memReq_Q.io_deq_bits_a_mask_1, memReq_st3_a_mask_1) @[DCache.scala 503:43 504:16 488:23]
    node _GEN_87 = mux(_T_74, memReq_Q.io_deq_bits_a_mask_2, memReq_st3_a_mask_2) @[DCache.scala 503:43 504:16 488:23]
    node _GEN_88 = mux(_T_74, memReq_Q.io_deq_bits_a_mask_3, memReq_st3_a_mask_3) @[DCache.scala 503:43 504:16 488:23]
    node _GEN_89 = mux(_T_74, memReq_Q.io_deq_bits_a_data_0, memReq_st3_a_data_0) @[DCache.scala 503:43 504:16 488:23]
    node _GEN_90 = mux(_T_74, memReq_Q.io_deq_bits_a_data_1, memReq_st3_a_data_1) @[DCache.scala 503:43 504:16 488:23]
    node _GEN_91 = mux(_T_74, memReq_Q.io_deq_bits_a_data_2, memReq_st3_a_data_2) @[DCache.scala 503:43 504:16 488:23]
    node _GEN_92 = mux(_T_74, memReq_Q.io_deq_bits_a_data_3, memReq_st3_a_data_3) @[DCache.scala 503:43 504:16 488:23]
    node _GEN_93 = mux(_T_74, memReq_Q.io_deq_bits_a_addr, memReq_st3_a_addr) @[DCache.scala 503:43 504:16 488:23]
    node _GEN_94 = mux(_T_74, memReq_Q.io_deq_bits_a_param, memReq_st3_a_param) @[DCache.scala 503:43 504:16 488:23]
    node _GEN_95 = mux(_T_74, memReq_Q.io_deq_bits_a_opcode, memReq_st3_a_opcode) @[DCache.scala 503:43 504:16 488:23]
    node memReqSetIdx_st2 = bits(memReq_Q.io_deq_bits_a_addr, 5, 4) @[DCache.scala 508:53]
    node memReq_st3_a_source_hi = cat(UInt<1>("h0"), WshrAccess.io_pushedIdx) @[Cat.scala 31:58]
    node _memReq_st3_a_source_T = cat(memReq_st3_a_source_hi, memReqSetIdx_st2) @[Cat.scala 31:58]
    node _GEN_96 = mux(memReqIsWrite_st3, _memReq_st3_a_source_T, _GEN_84) @[DCache.scala 509:26 510:27]
    reg memReq_valid : UInt<1>, clock with :
      reset => (UInt<1>("h0"), memReq_valid) @[DCache.scala 522:29]
    node _T_75 = and(memReq_Q.io_deq_ready, memReq_Q.io_deq_valid) @[Decoupled.scala 50:35]
    node _T_76 = and(io_memReq_ready, io_memReq_valid) @[Decoupled.scala 50:35]
    node _T_77 = xor(_T_75, _T_76) @[DCache.scala 523:29]
    node _memReq_valid_T = and(memReq_Q.io_deq_ready, memReq_Q.io_deq_valid) @[Decoupled.scala 50:35]
    node _GEN_97 = mux(_T_77, _memReq_valid_T, memReq_valid) @[DCache.scala 523:46 524:18 522:29]
    node _missRspTI_st1_WIRE_perLaneAddr_0_blockOffset = _missRspTI_st1_T_1 @[DCache.scala 353:{69,69}]
    node missRspTI_st1_perLaneAddr_0_blockOffset = _missRspTI_st1_WIRE_perLaneAddr_0_blockOffset @[DCache.scala 353:17 94:27]
    node _missRspTI_st1_WIRE_perLaneAddr_0_wordOffset1H = _missRspTI_st1_T @[DCache.scala 353:{69,69}]
    node missRspTI_st1_perLaneAddr_0_wordOffset1H = _missRspTI_st1_WIRE_perLaneAddr_0_wordOffset1H @[DCache.scala 353:17 94:27]
    node _missRspTI_st1_WIRE_perLaneAddr_1_blockOffset = _missRspTI_st1_T_4 @[DCache.scala 353:{69,69}]
    node missRspTI_st1_perLaneAddr_1_blockOffset = _missRspTI_st1_WIRE_perLaneAddr_1_blockOffset @[DCache.scala 353:17 94:27]
    node _missRspTI_st1_WIRE_perLaneAddr_1_wordOffset1H = _missRspTI_st1_T_3 @[DCache.scala 353:{69,69}]
    node missRspTI_st1_perLaneAddr_1_wordOffset1H = _missRspTI_st1_WIRE_perLaneAddr_1_wordOffset1H @[DCache.scala 353:17 94:27]
    node _missRspTI_st1_WIRE_perLaneAddr_2_blockOffset = _missRspTI_st1_T_7 @[DCache.scala 353:{69,69}]
    node missRspTI_st1_perLaneAddr_2_blockOffset = _missRspTI_st1_WIRE_perLaneAddr_2_blockOffset @[DCache.scala 353:17 94:27]
    node _missRspTI_st1_WIRE_perLaneAddr_2_wordOffset1H = _missRspTI_st1_T_6 @[DCache.scala 353:{69,69}]
    node missRspTI_st1_perLaneAddr_2_wordOffset1H = _missRspTI_st1_WIRE_perLaneAddr_2_wordOffset1H @[DCache.scala 353:17 94:27]
    node _missRspTI_st1_WIRE_perLaneAddr_3_blockOffset = _missRspTI_st1_T_10 @[DCache.scala 353:{69,69}]
    node missRspTI_st1_perLaneAddr_3_blockOffset = _missRspTI_st1_WIRE_perLaneAddr_3_blockOffset @[DCache.scala 353:17 94:27]
    node _missRspTI_st1_WIRE_perLaneAddr_3_wordOffset1H = _missRspTI_st1_T_9 @[DCache.scala 353:{69,69}]
    node missRspTI_st1_perLaneAddr_3_wordOffset1H = _missRspTI_st1_WIRE_perLaneAddr_3_wordOffset1H @[DCache.scala 353:17 94:27]
    node coreReq_st1_ready = _GEN_44 @[DCache.scala 120:31]
    node coreRsp_st2_valid = _coreRsp_st2_valid_T_1 @[DCache.scala 140:30 466:21]
    node missMemReq_a_opcode = _missMemReq_T_a_opcode @[DCache.scala 182:24 203:14]
    node missMemReq_a_param = _missMemReq_T_a_param @[DCache.scala 182:24 203:14]
    node missMemReq_a_addr = _missMemReq_T_a_addr @[DCache.scala 182:24 203:14]
    node missMemReq_a_data_0 = _missMemReq_T_a_data_0 @[DCache.scala 182:24 203:14]
    node missMemReq_a_data_1 = _missMemReq_T_a_data_1 @[DCache.scala 182:24 203:14]
    node missMemReq_a_data_2 = _missMemReq_T_a_data_2 @[DCache.scala 182:24 203:14]
    node missMemReq_a_data_3 = _missMemReq_T_a_data_3 @[DCache.scala 182:24 203:14]
    node missMemReq_a_mask_0 = _missMemReq_T_a_mask_0 @[DCache.scala 182:24 203:14]
    node missMemReq_a_mask_1 = _missMemReq_T_a_mask_1 @[DCache.scala 182:24 203:14]
    node missMemReq_a_mask_2 = _missMemReq_T_a_mask_2 @[DCache.scala 182:24 203:14]
    node missMemReq_a_mask_3 = _missMemReq_T_a_mask_3 @[DCache.scala 182:24 203:14]
    node missMemReq_a_source = _missMemReq_T_a_source @[DCache.scala 182:24 203:14]
    node missMemReq_hasCoreRsp = _missMemReq_T_hasCoreRsp @[DCache.scala 182:24 203:14]
    node missMemReq_coreRspInstrId = _missMemReq_T_coreRspInstrId @[DCache.scala 182:24 203:14]
    node dirtyReplace_st1_a_opcode = UInt<3>("h0") @[DCache.scala 320:30 321:29]
    node dirtyReplace_st1_a_param = UInt<3>("h0") @[DCache.scala 320:30 322:28]
    node dirtyReplace_st1_a_addr = pad(TagAccess.io_a_addrReplacement_st1, 32) @[DCache.scala 320:30 324:27]
    node dirtyReplace_st1_a_data_0 = DataAccessReadSRAMRRsp_0 @[DCache.scala 320:30 478:27]
    node dirtyReplace_st1_a_data_1 = DataAccessReadSRAMRRsp_1 @[DCache.scala 320:30 478:27]
    node dirtyReplace_st1_a_data_2 = DataAccessReadSRAMRRsp_2 @[DCache.scala 320:30 478:27]
    node dirtyReplace_st1_a_data_3 = DataAccessReadSRAMRRsp_3 @[DCache.scala 320:30 478:27]
    node _WIRE_16_0 = UInt<1>("h1") @[DCache.scala 325:{37,37}]
    node dirtyReplace_st1_a_mask_0 = _WIRE_16_0 @[DCache.scala 320:30 325:27]
    node _WIRE_16_1 = UInt<1>("h1") @[DCache.scala 325:{37,37}]
    node dirtyReplace_st1_a_mask_1 = _WIRE_16_1 @[DCache.scala 320:30 325:27]
    node _WIRE_16_2 = UInt<1>("h1") @[DCache.scala 325:{37,37}]
    node dirtyReplace_st1_a_mask_2 = _WIRE_16_2 @[DCache.scala 320:30 325:27]
    node _WIRE_16_3 = UInt<1>("h1") @[DCache.scala 325:{37,37}]
    node dirtyReplace_st1_a_mask_3 = _WIRE_16_3 @[DCache.scala 320:30 325:27]
    node dirtyReplace_st1_a_source = validif(UInt<1>("h0"), UInt<7>("h0"))
    node dirtyReplace_st1_hasCoreRsp = UInt<1>("h0") @[DCache.scala 320:30 327:31]
    node dirtyReplace_st1_coreRspInstrId = validif(UInt<1>("h0"), UInt<32>("h0"))
    io_coreReq_ready <= coreReq_Q.io_enq_ready @[DCache.scala 116:20]
    io_coreRsp_valid <= coreRsp_Q.io_deq_valid @[DCache.scala 468:20]
    io_coreRsp_bits_instrId <= coreRsp_Q.io_deq_bits_instrId @[DCache.scala 468:20]
    io_coreRsp_bits_isWrite <= coreRsp_Q.io_deq_bits_isWrite @[DCache.scala 468:20]
    io_coreRsp_bits_data_0 <= coreRsp_Q.io_deq_bits_data_0 @[DCache.scala 468:20]
    io_coreRsp_bits_data_1 <= coreRsp_Q.io_deq_bits_data_1 @[DCache.scala 468:20]
    io_coreRsp_bits_data_2 <= coreRsp_Q.io_deq_bits_data_2 @[DCache.scala 468:20]
    io_coreRsp_bits_data_3 <= coreRsp_Q.io_deq_bits_data_3 @[DCache.scala 468:20]
    io_coreRsp_bits_activeMask_0 <= coreRsp_Q.io_deq_bits_activeMask_0 @[DCache.scala 468:20]
    io_coreRsp_bits_activeMask_1 <= coreRsp_Q.io_deq_bits_activeMask_1 @[DCache.scala 468:20]
    io_coreRsp_bits_activeMask_2 <= coreRsp_Q.io_deq_bits_activeMask_2 @[DCache.scala 468:20]
    io_coreRsp_bits_activeMask_3 <= coreRsp_Q.io_deq_bits_activeMask_3 @[DCache.scala 468:20]
    io_memRsp_ready <= memRsp_Q.io_enq_ready @[DCache.scala 248:19]
    io_memReq_valid <= memReq_valid @[DCache.scala 526:19]
    io_memReq_bits_a_opcode <= memReq_st3_a_opcode @[DCache.scala 520:18]
    io_memReq_bits_a_param <= memReq_st3_a_param @[DCache.scala 520:18]
    io_memReq_bits_a_addr <= memReq_st3_a_addr @[DCache.scala 520:18]
    io_memReq_bits_a_data_0 <= memReq_st3_a_data_0 @[DCache.scala 520:18]
    io_memReq_bits_a_data_1 <= memReq_st3_a_data_1 @[DCache.scala 520:18]
    io_memReq_bits_a_data_2 <= memReq_st3_a_data_2 @[DCache.scala 520:18]
    io_memReq_bits_a_data_3 <= memReq_st3_a_data_3 @[DCache.scala 520:18]
    io_memReq_bits_a_mask_0 <= memReq_st3_a_mask_0 @[DCache.scala 520:18]
    io_memReq_bits_a_mask_1 <= memReq_st3_a_mask_1 @[DCache.scala 520:18]
    io_memReq_bits_a_mask_2 <= memReq_st3_a_mask_2 @[DCache.scala 520:18]
    io_memReq_bits_a_mask_3 <= memReq_st3_a_mask_3 @[DCache.scala 520:18]
    io_memReq_bits_a_source <= memReq_st3_a_source @[DCache.scala 520:18]
    MshrAccess.clock <= clock
    MshrAccess.reset <= reset
    MshrAccess.io_probe_valid <= _MshrAccess_io_probe_valid_T @[DCache.scala 159:29]
    MshrAccess.io_probe_bits_blockAddr <= _MshrAccess_io_probe_bits_blockAddr_T @[DCache.scala 160:38]
    MshrAccess.io_missReq_valid <= _MshrAccess_io_missReq_valid_T_4 @[DCache.scala 172:31]
    MshrAccess.io_missReq_bits_blockAddr <= _MshrAccess_io_missReq_bits_blockAddr_T @[DCache.scala 178:40]
    MshrAccess.io_missReq_bits_instrId <= coreReq_Q.io_deq_bits_instrId @[DCache.scala 177:38]
    MshrAccess.io_missReq_bits_targetInfo <= _MshrAccess_io_missReq_bits_targetInfo_T @[DCache.scala 179:41]
    MshrAccess.io_missRspIn_valid <= _MshrAccess_io_missRspIn_valid_T_1 @[DCache.scala 343:33]
    MshrAccess.io_missRspIn_bits_instrId <= _MshrAccess_io_missRspIn_bits_instrId_T @[DCache.scala 344:40]
    TagAccess.clock <= clock
    TagAccess.reset <= reset
    TagAccess.io_probeRead_valid <= _TagAccess_io_probeRead_valid_T @[DCache.scala 154:32]
    TagAccess.io_probeRead_bits_setIdx <= io_coreReq_bits_setIdx @[DCache.scala 155:38]
    TagAccess.io_tagFromCore_st1 <= coreReq_Q.io_deq_bits_tag @[DCache.scala 156:32]
    TagAccess.io_probeIsWrite_st1 <= writeHit_st1 @[DCache.scala 168:37]
    TagAccess.io_allocateWrite_valid <= _TagAccess_io_allocateWrite_valid_T_1 @[DCache.scala 347:36]
    TagAccess.io_allocateWrite_bits_setIdx <= _TagAccess_io_allocateWrite_bits_setIdx_T @[DCache.scala 348:42]
    TagAccess.io_allocateWriteData_st1 <= _TagAccess_io_allocateWriteData_st1_T @[DCache.scala 361:38]
    TagAccess.io_allocateWriteTagSRAMWValid_st1 <= _TagAccess_io_allocateWriteTagSRAMWValid_st1_T @[DCache.scala 362:47]
    WshrAccess.clock <= clock
    WshrAccess.reset <= reset
    WshrAccess.io_pushReq_valid <= _WshrAccess_io_pushReq_valid_T_1 @[DCache.scala 498:31]
    WshrAccess.io_pushReq_bits_blockAddr <= _WshrAccess_io_pushReq_bits_blockAddr_T @[DCache.scala 493:40]
    WshrAccess.io_popReq_valid <= _WshrAccess_io_popReq_valid_T @[DCache.scala 273:30]
    WshrAccess.io_popReq_bits <= _WshrAccess_io_popReq_bits_T @[DCache.scala 274:29]
    coreReq_Q.clock <= clock
    coreReq_Q.reset <= reset
    coreReq_Q.io_enq_valid <= io_coreReq_valid @[DCache.scala 115:26]
    coreReq_Q.io_enq_bits_instrId <= io_coreReq_bits_instrId @[DCache.scala 117:25]
    coreReq_Q.io_enq_bits_opcode <= io_coreReq_bits_opcode @[DCache.scala 117:25]
    coreReq_Q.io_enq_bits_param <= io_coreReq_bits_param @[DCache.scala 117:25]
    coreReq_Q.io_enq_bits_tag <= io_coreReq_bits_tag @[DCache.scala 117:25]
    coreReq_Q.io_enq_bits_setIdx <= io_coreReq_bits_setIdx @[DCache.scala 117:25]
    coreReq_Q.io_enq_bits_perLaneAddr_0_activeMask <= io_coreReq_bits_perLaneAddr_0_activeMask @[DCache.scala 117:25]
    coreReq_Q.io_enq_bits_perLaneAddr_0_blockOffset <= io_coreReq_bits_perLaneAddr_0_blockOffset @[DCache.scala 117:25]
    coreReq_Q.io_enq_bits_perLaneAddr_0_wordOffset1H <= io_coreReq_bits_perLaneAddr_0_wordOffset1H @[DCache.scala 117:25]
    coreReq_Q.io_enq_bits_perLaneAddr_1_activeMask <= io_coreReq_bits_perLaneAddr_1_activeMask @[DCache.scala 117:25]
    coreReq_Q.io_enq_bits_perLaneAddr_1_blockOffset <= io_coreReq_bits_perLaneAddr_1_blockOffset @[DCache.scala 117:25]
    coreReq_Q.io_enq_bits_perLaneAddr_1_wordOffset1H <= io_coreReq_bits_perLaneAddr_1_wordOffset1H @[DCache.scala 117:25]
    coreReq_Q.io_enq_bits_perLaneAddr_2_activeMask <= io_coreReq_bits_perLaneAddr_2_activeMask @[DCache.scala 117:25]
    coreReq_Q.io_enq_bits_perLaneAddr_2_blockOffset <= io_coreReq_bits_perLaneAddr_2_blockOffset @[DCache.scala 117:25]
    coreReq_Q.io_enq_bits_perLaneAddr_2_wordOffset1H <= io_coreReq_bits_perLaneAddr_2_wordOffset1H @[DCache.scala 117:25]
    coreReq_Q.io_enq_bits_perLaneAddr_3_activeMask <= io_coreReq_bits_perLaneAddr_3_activeMask @[DCache.scala 117:25]
    coreReq_Q.io_enq_bits_perLaneAddr_3_blockOffset <= io_coreReq_bits_perLaneAddr_3_blockOffset @[DCache.scala 117:25]
    coreReq_Q.io_enq_bits_perLaneAddr_3_wordOffset1H <= io_coreReq_bits_perLaneAddr_3_wordOffset1H @[DCache.scala 117:25]
    coreReq_Q.io_enq_bits_data_0 <= io_coreReq_bits_data_0 @[DCache.scala 117:25]
    coreReq_Q.io_enq_bits_data_1 <= io_coreReq_bits_data_1 @[DCache.scala 117:25]
    coreReq_Q.io_enq_bits_data_2 <= io_coreReq_bits_data_2 @[DCache.scala 117:25]
    coreReq_Q.io_enq_bits_data_3 <= io_coreReq_bits_data_3 @[DCache.scala 117:25]
    coreReq_Q.io_deq_ready <= coreReq_st1_ready @[DCache.scala 127:25]
    coreRsp_Q.clock <= clock
    coreRsp_Q.reset <= reset
    coreRsp_Q.io_enq_valid <= coreRsp_st2_valid @[DCache.scala 469:26]
    coreRsp_Q.io_enq_bits_instrId <= _coreRsp_Q_io_enq_bits_T_instrId @[DCache.scala 470:25]
    coreRsp_Q.io_enq_bits_isWrite <= _coreRsp_Q_io_enq_bits_T_isWrite @[DCache.scala 470:25]
    coreRsp_Q.io_enq_bits_data_0 <= _GEN_80
    coreRsp_Q.io_enq_bits_data_1 <= _GEN_81
    coreRsp_Q.io_enq_bits_data_2 <= _GEN_82
    coreRsp_Q.io_enq_bits_data_3 <= _GEN_83
    coreRsp_Q.io_enq_bits_activeMask_0 <= _coreRsp_Q_io_enq_bits_T_activeMask_0 @[DCache.scala 470:25]
    coreRsp_Q.io_enq_bits_activeMask_1 <= _coreRsp_Q_io_enq_bits_T_activeMask_1 @[DCache.scala 470:25]
    coreRsp_Q.io_enq_bits_activeMask_2 <= _coreRsp_Q_io_enq_bits_T_activeMask_2 @[DCache.scala 470:25]
    coreRsp_Q.io_enq_bits_activeMask_3 <= _coreRsp_Q_io_enq_bits_T_activeMask_3 @[DCache.scala 470:25]
    coreRsp_Q.io_deq_ready <= io_coreRsp_ready @[DCache.scala 468:20]
    memRsp_Q.clock <= clock
    memRsp_Q.reset <= reset
    memRsp_Q.io_enq_valid <= io_memRsp_valid @[DCache.scala 248:19]
    memRsp_Q.io_enq_bits_d_opcode <= io_memRsp_bits_d_opcode @[DCache.scala 248:19]
    memRsp_Q.io_enq_bits_d_source <= io_memRsp_bits_d_source @[DCache.scala 248:19]
    memRsp_Q.io_enq_bits_d_addr <= io_memRsp_bits_d_addr @[DCache.scala 248:19]
    memRsp_Q.io_enq_bits_d_data_0 <= io_memRsp_bits_d_data_0 @[DCache.scala 248:19]
    memRsp_Q.io_enq_bits_d_data_1 <= io_memRsp_bits_d_data_1 @[DCache.scala 248:19]
    memRsp_Q.io_enq_bits_d_data_2 <= io_memRsp_bits_d_data_2 @[DCache.scala 248:19]
    memRsp_Q.io_enq_bits_d_data_3 <= io_memRsp_bits_d_data_3 @[DCache.scala 248:19]
    memRsp_Q.io_deq_ready <= _memRsp_Q_io_deq_ready_T_3 @[DCache.scala 270:25]
    memReq_Q.clock <= clock
    memReq_Q.reset <= reset
    memReq_Q.io_enq_valid <= MemReqArb.io_out_valid @[DCache.scala 481:19]
    memReq_Q.io_enq_bits_a_opcode <= MemReqArb.io_out_bits_a_opcode @[DCache.scala 481:19]
    memReq_Q.io_enq_bits_a_param <= MemReqArb.io_out_bits_a_param @[DCache.scala 481:19]
    memReq_Q.io_enq_bits_a_addr <= MemReqArb.io_out_bits_a_addr @[DCache.scala 481:19]
    memReq_Q.io_enq_bits_a_data_0 <= MemReqArb.io_out_bits_a_data_0 @[DCache.scala 481:19]
    memReq_Q.io_enq_bits_a_data_1 <= MemReqArb.io_out_bits_a_data_1 @[DCache.scala 481:19]
    memReq_Q.io_enq_bits_a_data_2 <= MemReqArb.io_out_bits_a_data_2 @[DCache.scala 481:19]
    memReq_Q.io_enq_bits_a_data_3 <= MemReqArb.io_out_bits_a_data_3 @[DCache.scala 481:19]
    memReq_Q.io_enq_bits_a_mask_0 <= MemReqArb.io_out_bits_a_mask_0 @[DCache.scala 481:19]
    memReq_Q.io_enq_bits_a_mask_1 <= MemReqArb.io_out_bits_a_mask_1 @[DCache.scala 481:19]
    memReq_Q.io_enq_bits_a_mask_2 <= MemReqArb.io_out_bits_a_mask_2 @[DCache.scala 481:19]
    memReq_Q.io_enq_bits_a_mask_3 <= MemReqArb.io_out_bits_a_mask_3 @[DCache.scala 481:19]
    memReq_Q.io_enq_bits_a_source <= MemReqArb.io_out_bits_a_source @[DCache.scala 481:19]
    memReq_Q.io_enq_bits_hasCoreRsp <= MemReqArb.io_out_bits_hasCoreRsp @[DCache.scala 481:19]
    memReq_Q.io_enq_bits_coreRspInstrId <= MemReqArb.io_out_bits_coreRspInstrId @[DCache.scala 481:19]
    memReq_Q.io_deq_ready <= _memReq_Q_io_deq_ready_T @[DCache.scala 501:25]
    MemReqArb.clock <= clock
    MemReqArb.reset <= reset
    MemReqArb.io_in_0_valid <= tagReplaceStatus @[DCache.scala 482:28]
    MemReqArb.io_in_0_bits_a_opcode <= dirtyReplace_st2_a_opcode @[DCache.scala 483:27]
    MemReqArb.io_in_0_bits_a_param <= dirtyReplace_st2_a_param @[DCache.scala 483:27]
    MemReqArb.io_in_0_bits_a_addr <= dirtyReplace_st2_a_addr @[DCache.scala 483:27]
    MemReqArb.io_in_0_bits_a_data_0 <= dirtyReplace_st2_a_data_0 @[DCache.scala 483:27]
    MemReqArb.io_in_0_bits_a_data_1 <= dirtyReplace_st2_a_data_1 @[DCache.scala 483:27]
    MemReqArb.io_in_0_bits_a_data_2 <= dirtyReplace_st2_a_data_2 @[DCache.scala 483:27]
    MemReqArb.io_in_0_bits_a_data_3 <= dirtyReplace_st2_a_data_3 @[DCache.scala 483:27]
    MemReqArb.io_in_0_bits_a_mask_0 <= dirtyReplace_st2_a_mask_0 @[DCache.scala 483:27]
    MemReqArb.io_in_0_bits_a_mask_1 <= dirtyReplace_st2_a_mask_1 @[DCache.scala 483:27]
    MemReqArb.io_in_0_bits_a_mask_2 <= dirtyReplace_st2_a_mask_2 @[DCache.scala 483:27]
    MemReqArb.io_in_0_bits_a_mask_3 <= dirtyReplace_st2_a_mask_3 @[DCache.scala 483:27]
    MemReqArb.io_in_0_bits_a_source <= dirtyReplace_st2_a_source @[DCache.scala 483:27]
    MemReqArb.io_in_0_bits_hasCoreRsp <= dirtyReplace_st2_hasCoreRsp @[DCache.scala 483:27]
    MemReqArb.io_in_0_bits_coreRspInstrId <= dirtyReplace_st2_coreRspInstrId @[DCache.scala 483:27]
    MemReqArb.io_in_1_valid <= _MemReqArb_io_in_1_valid_T_3 @[DCache.scala 484:28]
    MemReqArb.io_in_1_bits_a_opcode <= missMemReq_a_opcode @[DCache.scala 485:27]
    MemReqArb.io_in_1_bits_a_param <= missMemReq_a_param @[DCache.scala 485:27]
    MemReqArb.io_in_1_bits_a_addr <= missMemReq_a_addr @[DCache.scala 485:27]
    MemReqArb.io_in_1_bits_a_data_0 <= missMemReq_a_data_0 @[DCache.scala 485:27]
    MemReqArb.io_in_1_bits_a_data_1 <= missMemReq_a_data_1 @[DCache.scala 485:27]
    MemReqArb.io_in_1_bits_a_data_2 <= missMemReq_a_data_2 @[DCache.scala 485:27]
    MemReqArb.io_in_1_bits_a_data_3 <= missMemReq_a_data_3 @[DCache.scala 485:27]
    MemReqArb.io_in_1_bits_a_mask_0 <= missMemReq_a_mask_0 @[DCache.scala 485:27]
    MemReqArb.io_in_1_bits_a_mask_1 <= missMemReq_a_mask_1 @[DCache.scala 485:27]
    MemReqArb.io_in_1_bits_a_mask_2 <= missMemReq_a_mask_2 @[DCache.scala 485:27]
    MemReqArb.io_in_1_bits_a_mask_3 <= missMemReq_a_mask_3 @[DCache.scala 485:27]
    MemReqArb.io_in_1_bits_a_source <= missMemReq_a_source @[DCache.scala 485:27]
    MemReqArb.io_in_1_bits_hasCoreRsp <= missMemReq_hasCoreRsp @[DCache.scala 485:27]
    MemReqArb.io_in_1_bits_coreRspInstrId <= missMemReq_coreRspInstrId @[DCache.scala 485:27]
    MemReqArb.io_out_ready <= memReq_Q.io_enq_ready @[DCache.scala 481:19]
    secondaryFullReturn <= _secondaryFullReturn_T @[DCache.scala 125:36]
    coreReqControl_st1_isRead <= _GEN_0
    coreReqControl_st1_isWrite <= _GEN_1
    coreReqControl_st1_isLR <= _GEN_2
    coreReqControl_st1_isSC <= _GEN_3
    coreReqControl_st1_isAMO <= _GEN_4
    coreReqControl_st1_isFlush <= _GEN_5
    coreReqControl_st1_isInvalidate <= _GEN_6
    coreRsp_st2_instrId <= _GEN_75
    coreRsp_st2_isWrite <= _GEN_69
    coreRsp_st2_data_0 <= _GEN_65
    coreRsp_st2_data_1 <= _GEN_66
    coreRsp_st2_data_2 <= _GEN_67
    coreRsp_st2_data_3 <= _GEN_68
    coreRsp_st2_activeMask_0 <= _GEN_76
    coreRsp_st2_activeMask_1 <= _GEN_77
    coreRsp_st2_activeMask_2 <= _GEN_78
    coreRsp_st2_activeMask_3 <= _GEN_79
    readHit_st2 <= readHit_st1 @[DCache.scala 145:28]
    genCtrl.clock <= clock
    genCtrl.reset <= reset
    genCtrl.io_opcode <= io_coreReq_bits_opcode @[DCache.scala 163:21]
    genCtrl.io_param <= io_coreReq_bits_param @[DCache.scala 164:20]
    MshrAccess_io_missReq_valid_REG <= secondaryFullReturn @[DCache.scala 172:114]
    getBankEn.clock <= clock
    getBankEn.reset <= reset
    getBankEn.io_perLaneBlockIdx_0 <= coreReq_Q.io_deq_bits_perLaneAddr_0_blockOffset @[DCache.scala 207:32]
    getBankEn.io_perLaneBlockIdx_1 <= coreReq_Q.io_deq_bits_perLaneAddr_1_blockOffset @[DCache.scala 207:32]
    getBankEn.io_perLaneBlockIdx_2 <= coreReq_Q.io_deq_bits_perLaneAddr_2_blockOffset @[DCache.scala 207:32]
    getBankEn.io_perLaneBlockIdx_3 <= coreReq_Q.io_deq_bits_perLaneAddr_3_blockOffset @[DCache.scala 207:32]
    getBankEn.io_perLaneValid_0 <= coreReq_Q.io_deq_bits_perLaneAddr_0_activeMask @[DCache.scala 208:29]
    getBankEn.io_perLaneValid_1 <= coreReq_Q.io_deq_bits_perLaneAddr_1_activeMask @[DCache.scala 208:29]
    getBankEn.io_perLaneValid_2 <= coreReq_Q.io_deq_bits_perLaneAddr_2_activeMask @[DCache.scala 208:29]
    getBankEn.io_perLaneValid_3 <= coreReq_Q.io_deq_bits_perLaneAddr_3_activeMask @[DCache.scala 208:29]
    tagReqValidCtrl <= mux(reset, UInt<1>("h1"), _GEN_46) @[DCache.scala 252:{32,32}]
    tagReqReadyCtrl <= mux(reset, UInt<1>("h0"), _GEN_48) @[DCache.scala 253:{32,32}]
    memRsp_st1_d_opcode <= _GEN_53
    memRsp_st1_d_source <= _GEN_54
    memRsp_st1_d_addr <= _GEN_55
    memRsp_st1_d_data_0 <= _GEN_56
    memRsp_st1_d_data_1 <= _GEN_57
    memRsp_st1_d_data_2 <= _GEN_58
    memRsp_st1_d_data_3 <= _GEN_59
    tagReplaceStatus <= mux(reset, UInt<1>("h0"), _GEN_51) @[DCache.scala 285:{33,33}]
    dataReplaceReadValid_REG <= TagAccess.io_allocateWrite_valid @[DCache.scala 303:37]
    dataFillVaild_REG <= TagAccess.io_allocateWrite_valid @[DCache.scala 309:30]
    TagAccess_io_allocateWriteTagSRAMWValid_st1_REG <= TagAccess.io_allocateWrite_valid @[DCache.scala 362:57]
    DataAccessesRRsp_DataAccess.clock <= clock
    DataAccessesRRsp_DataAccess.reset <= reset
    DataAccessesRRsp_DataAccess.io_r_req_valid <= _DataAccessesRRsp_DataAccess_io_r_req_valid_T @[DCache.scala 420:31]
    DataAccessesRRsp_DataAccess.io_r_req_bits_setIdx <= _DataAccessesRRsp_DataAccess_io_r_req_bits_T_setIdx @[DCache.scala 427:30]
    DataAccessesRRsp_DataAccess.io_w_req_valid <= _DataAccessesRRsp_DataAccess_io_w_req_valid_T_1 @[DCache.scala 391:31]
    DataAccessesRRsp_DataAccess.io_w_req_bits_setIdx <= _DataAccessesRRsp_DataAccess_io_w_req_bits_T_1_setIdx @[DCache.scala 400:30]
    DataAccessesRRsp_DataAccess.io_w_req_bits_data_0 <= _DataAccessesRRsp_DataAccess_io_w_req_bits_T_1_data_0 @[DCache.scala 400:30]
    DataAccessesRRsp_DataAccess.io_w_req_bits_data_1 <= _DataAccessesRRsp_DataAccess_io_w_req_bits_T_1_data_1 @[DCache.scala 400:30]
    DataAccessesRRsp_DataAccess.io_w_req_bits_data_2 <= _DataAccessesRRsp_DataAccess_io_w_req_bits_T_1_data_2 @[DCache.scala 400:30]
    DataAccessesRRsp_DataAccess.io_w_req_bits_data_3 <= _DataAccessesRRsp_DataAccess_io_w_req_bits_T_1_data_3 @[DCache.scala 400:30]
    DataAccessesRRsp_DataAccess.io_w_req_bits_waymask <= _DataAccessesRRsp_DataAccess_io_w_req_bits_T_1_waymask @[DCache.scala 400:30]
    DataAccessesRRsp_DataAccess_1.clock <= clock
    DataAccessesRRsp_DataAccess_1.reset <= reset
    DataAccessesRRsp_DataAccess_1.io_r_req_valid <= _DataAccessesRRsp_DataAccess_io_r_req_valid_T_1 @[DCache.scala 420:31]
    DataAccessesRRsp_DataAccess_1.io_r_req_bits_setIdx <= _DataAccessesRRsp_DataAccess_io_r_req_bits_T_1_setIdx @[DCache.scala 427:30]
    DataAccessesRRsp_DataAccess_1.io_w_req_valid <= _DataAccessesRRsp_DataAccess_io_w_req_valid_T_3 @[DCache.scala 391:31]
    DataAccessesRRsp_DataAccess_1.io_w_req_bits_setIdx <= _DataAccessesRRsp_DataAccess_io_w_req_bits_T_3_setIdx @[DCache.scala 400:30]
    DataAccessesRRsp_DataAccess_1.io_w_req_bits_data_0 <= _DataAccessesRRsp_DataAccess_io_w_req_bits_T_3_data_0 @[DCache.scala 400:30]
    DataAccessesRRsp_DataAccess_1.io_w_req_bits_data_1 <= _DataAccessesRRsp_DataAccess_io_w_req_bits_T_3_data_1 @[DCache.scala 400:30]
    DataAccessesRRsp_DataAccess_1.io_w_req_bits_data_2 <= _DataAccessesRRsp_DataAccess_io_w_req_bits_T_3_data_2 @[DCache.scala 400:30]
    DataAccessesRRsp_DataAccess_1.io_w_req_bits_data_3 <= _DataAccessesRRsp_DataAccess_io_w_req_bits_T_3_data_3 @[DCache.scala 400:30]
    DataAccessesRRsp_DataAccess_1.io_w_req_bits_waymask <= _DataAccessesRRsp_DataAccess_io_w_req_bits_T_3_waymask @[DCache.scala 400:30]
    DataAccessesRRsp_DataAccess_2.clock <= clock
    DataAccessesRRsp_DataAccess_2.reset <= reset
    DataAccessesRRsp_DataAccess_2.io_r_req_valid <= _DataAccessesRRsp_DataAccess_io_r_req_valid_T_2 @[DCache.scala 420:31]
    DataAccessesRRsp_DataAccess_2.io_r_req_bits_setIdx <= _DataAccessesRRsp_DataAccess_io_r_req_bits_T_2_setIdx @[DCache.scala 427:30]
    DataAccessesRRsp_DataAccess_2.io_w_req_valid <= _DataAccessesRRsp_DataAccess_io_w_req_valid_T_5 @[DCache.scala 391:31]
    DataAccessesRRsp_DataAccess_2.io_w_req_bits_setIdx <= _DataAccessesRRsp_DataAccess_io_w_req_bits_T_5_setIdx @[DCache.scala 400:30]
    DataAccessesRRsp_DataAccess_2.io_w_req_bits_data_0 <= _DataAccessesRRsp_DataAccess_io_w_req_bits_T_5_data_0 @[DCache.scala 400:30]
    DataAccessesRRsp_DataAccess_2.io_w_req_bits_data_1 <= _DataAccessesRRsp_DataAccess_io_w_req_bits_T_5_data_1 @[DCache.scala 400:30]
    DataAccessesRRsp_DataAccess_2.io_w_req_bits_data_2 <= _DataAccessesRRsp_DataAccess_io_w_req_bits_T_5_data_2 @[DCache.scala 400:30]
    DataAccessesRRsp_DataAccess_2.io_w_req_bits_data_3 <= _DataAccessesRRsp_DataAccess_io_w_req_bits_T_5_data_3 @[DCache.scala 400:30]
    DataAccessesRRsp_DataAccess_2.io_w_req_bits_waymask <= _DataAccessesRRsp_DataAccess_io_w_req_bits_T_5_waymask @[DCache.scala 400:30]
    DataAccessesRRsp_DataAccess_3.clock <= clock
    DataAccessesRRsp_DataAccess_3.reset <= reset
    DataAccessesRRsp_DataAccess_3.io_r_req_valid <= _DataAccessesRRsp_DataAccess_io_r_req_valid_T_3 @[DCache.scala 420:31]
    DataAccessesRRsp_DataAccess_3.io_r_req_bits_setIdx <= _DataAccessesRRsp_DataAccess_io_r_req_bits_T_3_setIdx @[DCache.scala 427:30]
    DataAccessesRRsp_DataAccess_3.io_w_req_valid <= _DataAccessesRRsp_DataAccess_io_w_req_valid_T_7 @[DCache.scala 391:31]
    DataAccessesRRsp_DataAccess_3.io_w_req_bits_setIdx <= _DataAccessesRRsp_DataAccess_io_w_req_bits_T_7_setIdx @[DCache.scala 400:30]
    DataAccessesRRsp_DataAccess_3.io_w_req_bits_data_0 <= _DataAccessesRRsp_DataAccess_io_w_req_bits_T_7_data_0 @[DCache.scala 400:30]
    DataAccessesRRsp_DataAccess_3.io_w_req_bits_data_1 <= _DataAccessesRRsp_DataAccess_io_w_req_bits_T_7_data_1 @[DCache.scala 400:30]
    DataAccessesRRsp_DataAccess_3.io_w_req_bits_data_2 <= _DataAccessesRRsp_DataAccess_io_w_req_bits_T_7_data_2 @[DCache.scala 400:30]
    DataAccessesRRsp_DataAccess_3.io_w_req_bits_data_3 <= _DataAccessesRRsp_DataAccess_io_w_req_bits_T_7_data_3 @[DCache.scala 400:30]
    DataAccessesRRsp_DataAccess_3.io_w_req_bits_waymask <= _DataAccessesRRsp_DataAccess_io_w_req_bits_T_7_waymask @[DCache.scala 400:30]
    REG <= _T_53 @[DCache.scala 435:31]
    REG_1 <= _T_55 @[DCache.scala 445:32]
    coreRsp_st2_valid_from_coreReq_REG <= _coreRsp_st2_valid_from_coreReq_T_1 @[DCache.scala 460:44]
    coreRsp_st2_valid_from_memRsp_REG <= MshrAccess.io_missRspOut_valid @[DCache.scala 462:43]
    dirtyReplace_st2_a_opcode <= dirtyReplace_st1_a_opcode @[DCache.scala 477:20]
    dirtyReplace_st2_a_param <= dirtyReplace_st1_a_param @[DCache.scala 477:20]
    dirtyReplace_st2_a_addr <= dirtyReplace_st1_a_addr @[DCache.scala 477:20]
    dirtyReplace_st2_a_data_0 <= dirtyReplace_st1_a_data_0 @[DCache.scala 477:20]
    dirtyReplace_st2_a_data_1 <= dirtyReplace_st1_a_data_1 @[DCache.scala 477:20]
    dirtyReplace_st2_a_data_2 <= dirtyReplace_st1_a_data_2 @[DCache.scala 477:20]
    dirtyReplace_st2_a_data_3 <= dirtyReplace_st1_a_data_3 @[DCache.scala 477:20]
    dirtyReplace_st2_a_mask_0 <= dirtyReplace_st1_a_mask_0 @[DCache.scala 477:20]
    dirtyReplace_st2_a_mask_1 <= dirtyReplace_st1_a_mask_1 @[DCache.scala 477:20]
    dirtyReplace_st2_a_mask_2 <= dirtyReplace_st1_a_mask_2 @[DCache.scala 477:20]
    dirtyReplace_st2_a_mask_3 <= dirtyReplace_st1_a_mask_3 @[DCache.scala 477:20]
    dirtyReplace_st2_a_source <= dirtyReplace_st1_a_source @[DCache.scala 477:20]
    dirtyReplace_st2_hasCoreRsp <= dirtyReplace_st1_hasCoreRsp @[DCache.scala 477:20]
    dirtyReplace_st2_coreRspInstrId <= dirtyReplace_st1_coreRspInstrId @[DCache.scala 477:20]
    memReq_st3_a_opcode <= _GEN_95
    memReq_st3_a_param <= _GEN_94
    memReq_st3_a_addr <= _GEN_93
    memReq_st3_a_data_0 <= _GEN_89
    memReq_st3_a_data_1 <= _GEN_90
    memReq_st3_a_data_2 <= _GEN_91
    memReq_st3_a_data_3 <= _GEN_92
    memReq_st3_a_mask_0 <= _GEN_85
    memReq_st3_a_mask_1 <= _GEN_86
    memReq_st3_a_mask_2 <= _GEN_87
    memReq_st3_a_mask_3 <= _GEN_88
    memReq_st3_a_source <= _GEN_96
    memReq_valid <= mux(reset, UInt<1>("h0"), _GEN_97) @[DCache.scala 522:{29,29}]
    printf(clock, and(and(and(UInt<1>("h1"), _T_62), _T_63), UInt<1>("h1")), "Assertion failed: cRsp from cReq and mRsp conflict\n    at DCache.scala:463 assert (!(coreRsp_st2_valid_from_coreReq && coreRsp_st2_valid_from_memRsp), s\"cRsp from cReq and mRsp conflict\")\n") : printf @[DCache.scala 463:10]
    assert(clock, _T_60, and(and(UInt<1>("h1"), _T_62), UInt<1>("h1")), "") : assert @[DCache.scala 463:10]
    printf(clock, and(and(and(UInt<1>("h1"), _T_67), _T_68), UInt<1>("h1")), "Assertion failed: cRsp from cReq and mReq conflict\n    at DCache.scala:464 assert (!(coreRsp_st2_valid_from_coreReq && coreRsp_st2_valid_from_memReq), \"cRsp from cReq and mReq conflict\")\n") : printf_1 @[DCache.scala 464:10]
    assert(clock, _T_65, and(and(UInt<1>("h1"), _T_67), UInt<1>("h1")), "") : assert_1 @[DCache.scala 464:10]
    printf(clock, and(and(and(UInt<1>("h1"), _T_72), _T_73), UInt<1>("h1")), "Assertion failed: cRsp from mRsp and mReq conflict\n    at DCache.scala:465 assert (!(coreRsp_st2_valid_from_memReq && coreRsp_st2_valid_from_memRsp), \"cRsp from mRsp and mReq conflict\")\n") : printf_2 @[DCache.scala 465:10]
    assert(clock, _T_70, and(and(UInt<1>("h1"), _T_72), UInt<1>("h1")), "") : assert_2 @[DCache.scala 465:10]

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
