module SimpleModule(
  input        clock,
  input        reset,
  input  [3:0] io_current_mask,
  output [1:0] io_current_mask_index_0,
  output [1:0] io_current_mask_index_1,
  output [1:0] io_current_mask_index_2,
  output [1:0] io_current_mask_index_3
);
  wire [1:0] maskIndices_newCntMask = io_current_mask[0] ? 2'h1 : 2'h0; // @[masktest.scala 29:40 31:20 32:32]
  wire [1:0] _GEN_2 = 2'h0 == maskIndices_newCntMask ? 2'h1 : 2'h0; // @[masktest.scala 30:{30,30} 26:44]
  wire [1:0] _GEN_3 = 2'h1 == maskIndices_newCntMask ? 2'h1 : 2'h0; // @[masktest.scala 30:{30,30} 26:44]
  wire [1:0] _GEN_4 = 2'h2 == maskIndices_newCntMask ? 2'h1 : 2'h0; // @[masktest.scala 30:{30,30} 26:44]
  wire [1:0] _GEN_5 = 2'h3 == maskIndices_newCntMask ? 2'h1 : 2'h0; // @[masktest.scala 30:{30,30} 26:44]
  wire [1:0] _maskIndices_newCntMask_T_3 = maskIndices_newCntMask + 2'h1; // @[masktest.scala 31:32]
  wire [1:0] maskIndices_newIndices_1_0 = io_current_mask[1] ? _GEN_2 : 2'h0; // @[masktest.scala 29:40 26:44]
  wire [1:0] maskIndices_newIndices_1_1 = io_current_mask[1] ? _GEN_3 : 2'h0; // @[masktest.scala 29:40 26:44]
  wire [1:0] maskIndices_newIndices_1_2 = io_current_mask[1] ? _GEN_4 : 2'h0; // @[masktest.scala 29:40 26:44]
  wire [1:0] maskIndices_newIndices_1_3 = io_current_mask[1] ? _GEN_5 : 2'h0; // @[masktest.scala 29:40 26:44]
  wire [1:0] maskIndices_newCntMask_1 = io_current_mask[1] ? _maskIndices_newCntMask_T_3 : maskIndices_newCntMask; // @[masktest.scala 29:40 31:20 32:32]
  wire [1:0] _GEN_11 = 2'h0 == maskIndices_newCntMask_1 ? 2'h2 : maskIndices_newIndices_1_0; // @[masktest.scala 30:{30,30} 26:44]
  wire [1:0] _GEN_12 = 2'h1 == maskIndices_newCntMask_1 ? 2'h2 : maskIndices_newIndices_1_1; // @[masktest.scala 30:{30,30} 26:44]
  wire [1:0] _GEN_13 = 2'h2 == maskIndices_newCntMask_1 ? 2'h2 : maskIndices_newIndices_1_2; // @[masktest.scala 30:{30,30} 26:44]
  wire [1:0] _GEN_14 = 2'h3 == maskIndices_newCntMask_1 ? 2'h2 : maskIndices_newIndices_1_3; // @[masktest.scala 30:{30,30} 26:44]
  wire [1:0] _maskIndices_newCntMask_T_5 = maskIndices_newCntMask_1 + 2'h1; // @[masktest.scala 31:32]
  wire [1:0] maskIndices_newIndices_2_0 = io_current_mask[2] ? _GEN_11 : maskIndices_newIndices_1_0; // @[masktest.scala 29:40 26:44]
  wire [1:0] maskIndices_newIndices_2_1 = io_current_mask[2] ? _GEN_12 : maskIndices_newIndices_1_1; // @[masktest.scala 29:40 26:44]
  wire [1:0] maskIndices_newIndices_2_2 = io_current_mask[2] ? _GEN_13 : maskIndices_newIndices_1_2; // @[masktest.scala 29:40 26:44]
  wire [1:0] maskIndices_newIndices_2_3 = io_current_mask[2] ? _GEN_14 : maskIndices_newIndices_1_3; // @[masktest.scala 29:40 26:44]
  wire [1:0] maskIndices_newCntMask_2 = io_current_mask[2] ? _maskIndices_newCntMask_T_5 : maskIndices_newCntMask_1; // @[masktest.scala 29:40 31:20 32:32]
  wire [1:0] _GEN_20 = 2'h0 == maskIndices_newCntMask_2 ? 2'h3 : maskIndices_newIndices_2_0; // @[masktest.scala 30:{30,30} 26:44]
  wire [1:0] _GEN_21 = 2'h1 == maskIndices_newCntMask_2 ? 2'h3 : maskIndices_newIndices_2_1; // @[masktest.scala 30:{30,30} 26:44]
  wire [1:0] _GEN_22 = 2'h2 == maskIndices_newCntMask_2 ? 2'h3 : maskIndices_newIndices_2_2; // @[masktest.scala 30:{30,30} 26:44]
  wire [1:0] _GEN_23 = 2'h3 == maskIndices_newCntMask_2 ? 2'h3 : maskIndices_newIndices_2_3; // @[masktest.scala 30:{30,30} 26:44]
  assign io_current_mask_index_0 = io_current_mask[3] ? _GEN_20 : maskIndices_newIndices_2_0; // @[masktest.scala 29:40 26:44]
  assign io_current_mask_index_1 = io_current_mask[3] ? _GEN_21 : maskIndices_newIndices_2_1; // @[masktest.scala 29:40 26:44]
  assign io_current_mask_index_2 = io_current_mask[3] ? _GEN_22 : maskIndices_newIndices_2_2; // @[masktest.scala 29:40 26:44]
  assign io_current_mask_index_3 = io_current_mask[3] ? _GEN_23 : maskIndices_newIndices_2_3; // @[masktest.scala 29:40 26:44]
endmodule
