module Scoreboard(
  input         clock,
  input         reset,
  input  [31:0] io_ibuffer_if_ctrl_inst,
  input  [2:0]  io_ibuffer_if_ctrl_wid,
  input         io_ibuffer_if_ctrl_fp,
  input  [1:0]  io_ibuffer_if_ctrl_branch,
  input         io_ibuffer_if_ctrl_simt_stack,
  input         io_ibuffer_if_ctrl_simt_stack_op,
  input         io_ibuffer_if_ctrl_barrier,
  input  [1:0]  io_ibuffer_if_ctrl_csr,
  input         io_ibuffer_if_ctrl_reverse,
  input  [1:0]  io_ibuffer_if_ctrl_sel_alu2,
  input  [1:0]  io_ibuffer_if_ctrl_sel_alu1,
  input         io_ibuffer_if_ctrl_isvec,
  input  [1:0]  io_ibuffer_if_ctrl_sel_alu3,
  input         io_ibuffer_if_ctrl_mask,
  input  [3:0]  io_ibuffer_if_ctrl_sel_imm,
  input  [1:0]  io_ibuffer_if_ctrl_mem_whb,
  input         io_ibuffer_if_ctrl_mem_unsigned,
  input  [5:0]  io_ibuffer_if_ctrl_alu_fn,
  input         io_ibuffer_if_ctrl_is_vls12,
  input         io_ibuffer_if_ctrl_mem,
  input         io_ibuffer_if_ctrl_mul,
  input         io_ibuffer_if_ctrl_tc,
  input         io_ibuffer_if_ctrl_disable_mask,
  input         io_ibuffer_if_ctrl_custom_signal_0,
  input  [1:0]  io_ibuffer_if_ctrl_mem_cmd,
  input  [1:0]  io_ibuffer_if_ctrl_mop,
  input  [7:0]  io_ibuffer_if_ctrl_reg_idx1,
  input  [7:0]  io_ibuffer_if_ctrl_reg_idx2,
  input  [7:0]  io_ibuffer_if_ctrl_reg_idx3,
  input  [7:0]  io_ibuffer_if_ctrl_reg_idxw,
  input         io_ibuffer_if_ctrl_wvd,
  input         io_ibuffer_if_ctrl_fence,
  input         io_ibuffer_if_ctrl_sfu,
  input         io_ibuffer_if_ctrl_readmask,
  input         io_ibuffer_if_ctrl_writemask,
  input         io_ibuffer_if_ctrl_wxd,
  input  [31:0] io_ibuffer_if_ctrl_pc,
  input  [5:0]  io_ibuffer_if_ctrl_imm_ext,
  input  [31:0] io_ibuffer_if_ctrl_spike_info_pc,
  input  [31:0] io_ibuffer_if_ctrl_spike_info_inst,
  input         io_ibuffer_if_ctrl_atomic,
  input         io_ibuffer_if_ctrl_aq,
  input         io_ibuffer_if_ctrl_rl,
  input  [31:0] io_if_ctrl_inst,
  input  [2:0]  io_if_ctrl_wid,
  input         io_if_ctrl_fp,
  input  [1:0]  io_if_ctrl_branch,
  input         io_if_ctrl_simt_stack,
  input         io_if_ctrl_simt_stack_op,
  input         io_if_ctrl_barrier,
  input  [1:0]  io_if_ctrl_csr,
  input         io_if_ctrl_reverse,
  input  [1:0]  io_if_ctrl_sel_alu2,
  input  [1:0]  io_if_ctrl_sel_alu1,
  input         io_if_ctrl_isvec,
  input  [1:0]  io_if_ctrl_sel_alu3,
  input         io_if_ctrl_mask,
  input  [3:0]  io_if_ctrl_sel_imm,
  input  [1:0]  io_if_ctrl_mem_whb,
  input         io_if_ctrl_mem_unsigned,
  input  [5:0]  io_if_ctrl_alu_fn,
  input         io_if_ctrl_is_vls12,
  input         io_if_ctrl_mem,
  input         io_if_ctrl_mul,
  input         io_if_ctrl_tc,
  input         io_if_ctrl_disable_mask,
  input         io_if_ctrl_custom_signal_0,
  input  [1:0]  io_if_ctrl_mem_cmd,
  input  [1:0]  io_if_ctrl_mop,
  input  [7:0]  io_if_ctrl_reg_idx1,
  input  [7:0]  io_if_ctrl_reg_idx2,
  input  [7:0]  io_if_ctrl_reg_idx3,
  input  [7:0]  io_if_ctrl_reg_idxw,
  input         io_if_ctrl_wvd,
  input         io_if_ctrl_fence,
  input         io_if_ctrl_sfu,
  input         io_if_ctrl_readmask,
  input         io_if_ctrl_writemask,
  input         io_if_ctrl_wxd,
  input  [31:0] io_if_ctrl_pc,
  input  [5:0]  io_if_ctrl_imm_ext,
  input  [31:0] io_if_ctrl_spike_info_pc,
  input  [31:0] io_if_ctrl_spike_info_inst,
  input         io_if_ctrl_atomic,
  input         io_if_ctrl_aq,
  input         io_if_ctrl_rl,
  input  [31:0] io_wb_v_ctrl_wb_wvd_rd_0,
  input  [31:0] io_wb_v_ctrl_wb_wvd_rd_1,
  input  [31:0] io_wb_v_ctrl_wb_wvd_rd_2,
  input  [31:0] io_wb_v_ctrl_wb_wvd_rd_3,
  input         io_wb_v_ctrl_wvd_mask_0,
  input         io_wb_v_ctrl_wvd_mask_1,
  input         io_wb_v_ctrl_wvd_mask_2,
  input         io_wb_v_ctrl_wvd_mask_3,
  input         io_wb_v_ctrl_wvd,
  input  [7:0]  io_wb_v_ctrl_reg_idxw,
  input  [2:0]  io_wb_v_ctrl_warp_id,
  input  [31:0] io_wb_v_ctrl_spike_info_pc,
  input  [31:0] io_wb_v_ctrl_spike_info_inst,
  input  [31:0] io_wb_x_ctrl_wb_wxd_rd,
  input         io_wb_x_ctrl_wxd,
  input  [7:0]  io_wb_x_ctrl_reg_idxw,
  input  [2:0]  io_wb_x_ctrl_warp_id,
  input  [31:0] io_wb_x_ctrl_spike_info_pc,
  input  [31:0] io_wb_x_ctrl_spike_info_inst,
  input         io_if_fire,
  input         io_br_ctrl,
  input         io_fence_end,
  input         io_wb_v_fire,
  input         io_wb_x_fire,
  output        io_delay,
  input         io_op_col_in_fire,
  input         io_op_col_out_fire
);
  reg [255:0] _r; // @[scoreboard.scala 84:27]
  reg [255:0] _r_1; // @[scoreboard.scala 84:27]
  wire [255:0] r = {_r_1[255:1], 1'h0}; // @[scoreboard.scala 85:37]
  reg  readb; // @[scoreboard.scala 84:27]
  reg  read_op_col; // @[scoreboard.scala 84:27]
  reg  _r_4; // @[scoreboard.scala 84:27]
  wire  _T = io_if_fire & io_if_ctrl_wvd; // @[scoreboard.scala 103:28]
  wire [255:0] _T_1 = 256'h1 << io_if_ctrl_reg_idxw; // @[scoreboard.scala 88:57]
  wire [255:0] _T_2 = _T ? _T_1 : 256'h0; // @[scoreboard.scala 88:47]
  wire [255:0] _T_3 = _r | _T_2; // @[scoreboard.scala 80:65]
  wire  _T_5 = io_wb_v_fire & io_wb_v_ctrl_wvd; // @[scoreboard.scala 104:32]
  wire [255:0] _T_6 = 256'h1 << io_wb_v_ctrl_reg_idxw; // @[scoreboard.scala 88:57]
  wire [255:0] _T_7 = _T_5 ? _T_6 : 256'h0; // @[scoreboard.scala 88:47]
  wire [255:0] _T_8 = ~_T_7; // @[scoreboard.scala 81:70]
  wire [255:0] _T_9 = _T_3 & _T_8; // @[scoreboard.scala 81:67]
  wire  _T_10 = _T | _T_5; // @[scoreboard.scala 91:15]
  wire  _T_11 = io_if_fire & io_if_ctrl_wxd; // @[scoreboard.scala 105:28]
  wire [255:0] _T_13 = _T_11 ? _T_1 : 256'h0; // @[scoreboard.scala 88:47]
  wire [255:0] _T_14 = r | _T_13; // @[scoreboard.scala 80:65]
  wire  _T_16 = io_wb_x_fire & io_wb_x_ctrl_wxd; // @[scoreboard.scala 106:32]
  wire [255:0] _T_17 = 256'h1 << io_wb_x_ctrl_reg_idxw; // @[scoreboard.scala 88:57]
  wire [255:0] _T_18 = _T_16 ? _T_17 : 256'h0; // @[scoreboard.scala 88:47]
  wire [255:0] _T_19 = ~_T_18; // @[scoreboard.scala 81:70]
  wire [255:0] _T_20 = _T_14 & _T_19; // @[scoreboard.scala 81:67]
  wire  _T_21 = _T_11 | _T_16; // @[scoreboard.scala 91:15]
  wire  _T_24 = io_if_fire & (io_if_ctrl_branch != 2'h0 | io_if_ctrl_barrier); // @[scoreboard.scala 107:25]
  wire [1:0] _T_26 = _T_24 ? 2'h1 : 2'h0; // @[scoreboard.scala 88:47]
  wire [1:0] _GEN_10 = {{1'd0}, readb}; // @[scoreboard.scala 80:65]
  wire [1:0] _T_27 = _GEN_10 | _T_26; // @[scoreboard.scala 80:65]
  wire [1:0] _GEN_4 = _T_24 ? _T_27 : {{1'd0}, readb}; // @[scoreboard.scala 92:{16,21} 84:27]
  wire [1:0] _T_30 = io_br_ctrl ? 2'h1 : 2'h0; // @[scoreboard.scala 88:47]
  wire [1:0] _T_31 = ~_T_30; // @[scoreboard.scala 81:70]
  wire [1:0] _T_32 = _T_27 & _T_31; // @[scoreboard.scala 81:67]
  wire  _T_33 = _T_24 | io_br_ctrl; // @[scoreboard.scala 91:15]
  wire [1:0] _GEN_5 = _T_33 ? _T_32 : _GEN_4; // @[scoreboard.scala 92:{16,21}]
  wire [1:0] _T_35 = io_op_col_in_fire ? 2'h1 : 2'h0; // @[scoreboard.scala 88:47]
  wire [1:0] _GEN_11 = {{1'd0}, read_op_col}; // @[scoreboard.scala 80:65]
  wire [1:0] _T_36 = _GEN_11 | _T_35; // @[scoreboard.scala 80:65]
  wire [1:0] _GEN_6 = io_op_col_in_fire ? _T_36 : {{1'd0}, read_op_col}; // @[scoreboard.scala 92:{16,21} 84:27]
  wire [1:0] _T_39 = io_op_col_out_fire ? 2'h1 : 2'h0; // @[scoreboard.scala 88:47]
  wire [1:0] _T_40 = ~_T_39; // @[scoreboard.scala 81:70]
  wire [1:0] _T_41 = _T_36 & _T_40; // @[scoreboard.scala 81:67]
  wire  _T_42 = io_op_col_in_fire | io_op_col_out_fire; // @[scoreboard.scala 91:15]
  wire [1:0] _GEN_7 = _T_42 ? _T_41 : _GEN_6; // @[scoreboard.scala 92:{16,21}]
  wire  _T_43 = io_if_fire & io_if_ctrl_fence; // @[scoreboard.scala 111:27]
  wire [1:0] _T_45 = _T_43 ? 2'h1 : 2'h0; // @[scoreboard.scala 88:47]
  wire [1:0] _GEN_12 = {{1'd0}, _r_4}; // @[scoreboard.scala 80:65]
  wire [1:0] _T_46 = _GEN_12 | _T_45; // @[scoreboard.scala 80:65]
  wire [1:0] _GEN_8 = _T_43 ? _T_46 : {{1'd0}, _r_4}; // @[scoreboard.scala 92:{16,21} 84:27]
  wire [1:0] _T_49 = io_fence_end ? 2'h1 : 2'h0; // @[scoreboard.scala 88:47]
  wire [1:0] _T_50 = ~_T_49; // @[scoreboard.scala 81:70]
  wire [1:0] _T_51 = _T_46 & _T_50; // @[scoreboard.scala 81:67]
  wire  _T_52 = _T_43 | io_fence_end; // @[scoreboard.scala 91:15]
  wire [1:0] _GEN_9 = _T_52 ? _T_51 : _GEN_8; // @[scoreboard.scala 92:{16,21}]
  wire [255:0] _read1_T = r >> io_ibuffer_if_ctrl_reg_idx1; // @[scoreboard.scala 82:33]
  wire [255:0] _read1_T_2 = _r >> io_ibuffer_if_ctrl_reg_idx1; // @[scoreboard.scala 82:33]
  wire  read1 = 2'h2 == io_ibuffer_if_ctrl_sel_alu1 ? _read1_T_2[0] : 2'h1 == io_ibuffer_if_ctrl_sel_alu1 & _read1_T[0]; // @[Mux.scala 81:58]
  wire [255:0] _read2_T = r >> io_ibuffer_if_ctrl_reg_idx2; // @[scoreboard.scala 82:33]
  wire [255:0] _read2_T_2 = _r >> io_ibuffer_if_ctrl_reg_idx2; // @[scoreboard.scala 82:33]
  wire  read2 = 2'h2 == io_ibuffer_if_ctrl_sel_alu2 ? _read2_T_2[0] : 2'h1 == io_ibuffer_if_ctrl_sel_alu2 & _read2_T[0]; // @[Mux.scala 81:58]
  wire [255:0] _read3_T = _r >> io_ibuffer_if_ctrl_reg_idx3; // @[scoreboard.scala 82:33]
  wire  _read3_T_10 = io_ibuffer_if_ctrl_isvec ? _read2_T_2[0] : _read2_T[0]; // @[scoreboard.scala 116:120]
  wire  _read3_T_11 = io_ibuffer_if_ctrl_isvec & ~io_ibuffer_if_ctrl_readmask ? _read3_T[0] : _read3_T_10; // @[scoreboard.scala 116:15]
  wire [255:0] _read3_T_12 = r >> io_ibuffer_if_ctrl_reg_idx3; // @[scoreboard.scala 82:33]
  wire  _read3_T_17 = io_ibuffer_if_ctrl_branch == 2'h3 & _read1_T[0]; // @[scoreboard.scala 118:16]
  wire  _read3_T_19 = 2'h3 == io_ibuffer_if_ctrl_sel_alu3 ? _read3_T_11 : _read3_T[0]; // @[Mux.scala 81:58]
  wire  _read3_T_21 = 2'h2 == io_ibuffer_if_ctrl_sel_alu3 ? _read3_T_12[0] : _read3_T_19; // @[Mux.scala 81:58]
  wire  read3 = 2'h0 == io_ibuffer_if_ctrl_sel_alu3 ? _read3_T_17 : _read3_T_21; // @[Mux.scala 81:58]
  wire  readm = io_ibuffer_if_ctrl_mask & _r[0]; // @[scoreboard.scala 120:16]
  wire [255:0] _readw_T = r >> io_ibuffer_if_ctrl_reg_idxw; // @[scoreboard.scala 82:33]
  wire [255:0] _readw_T_3 = _r >> io_ibuffer_if_ctrl_reg_idxw; // @[scoreboard.scala 82:33]
  wire  readw = io_ibuffer_if_ctrl_wxd & _readw_T[0] | io_ibuffer_if_ctrl_wvd & _readw_T_3[0]; // @[scoreboard.scala 121:92]
  wire  readf = io_ibuffer_if_ctrl_mem & _r_4; // @[scoreboard.scala 124:36]
  wire [1:0] _GEN_13 = reset ? 2'h0 : _GEN_5; // @[scoreboard.scala 84:{27,27}]
  wire [1:0] _GEN_14 = reset ? 2'h0 : _GEN_7; // @[scoreboard.scala 84:{27,27}]
  wire [1:0] _GEN_15 = reset ? 2'h0 : _GEN_9; // @[scoreboard.scala 84:{27,27}]
  assign io_delay = read1 | read2 | read3 | readm | readw | readb | readf | read_op_col; // @[scoreboard.scala 125:54]
  always @(posedge clock) begin
    if (reset) begin // @[scoreboard.scala 84:27]
      _r <= 256'h0; // @[scoreboard.scala 84:27]
    end else if (_T_10) begin // @[scoreboard.scala 92:16]
      _r <= _T_9; // @[scoreboard.scala 92:21]
    end else if (_T) begin // @[scoreboard.scala 92:16]
      _r <= _T_3; // @[scoreboard.scala 92:21]
    end
    if (reset) begin // @[scoreboard.scala 84:27]
      _r_1 <= 256'h0; // @[scoreboard.scala 84:27]
    end else if (_T_21) begin // @[scoreboard.scala 92:16]
      _r_1 <= _T_20; // @[scoreboard.scala 92:21]
    end else if (_T_11) begin // @[scoreboard.scala 92:16]
      _r_1 <= _T_14; // @[scoreboard.scala 92:21]
    end
    readb <= _GEN_13[0]; // @[scoreboard.scala 84:{27,27}]
    read_op_col <= _GEN_14[0]; // @[scoreboard.scala 84:{27,27}]
    _r_4 <= _GEN_15[0]; // @[scoreboard.scala 84:{27,27}]
  end
endmodule
