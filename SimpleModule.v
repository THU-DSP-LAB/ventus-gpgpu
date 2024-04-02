module SimpleModule(
  input        clock,
  input        reset,
  input        io_current_mask_0,
  input        io_current_mask_1,
  input        io_current_mask_2,
  input        io_current_mask_3,
  output [1:0] io_current_mask_index_0,
  output [1:0] io_current_mask_index_1,
  output [1:0] io_current_mask_index_2,
  output [1:0] io_current_mask_index_3
);
  assign io_current_mask_index_0 = 2'h0;
  assign io_current_mask_index_1 = {{1'd0}, io_current_mask_1};
  assign io_current_mask_index_2 = io_current_mask_2 ? 2'h2 : 2'h0; // @[masktest.scala 24:30 18:35 25:39]
  assign io_current_mask_index_3 = io_current_mask_3 ? 2'h3 : 2'h0; // @[masktest.scala 24:30 18:35 25:39]
endmodule
