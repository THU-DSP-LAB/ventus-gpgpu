module ImmGen(
  input  [31:0] io_inst,
  input  [2:0]  io_sel,
  output [31:0] io_out
);
  wire [11:0] Iimm = io_inst[31:20]; // @[regfile.scala 82:30]
  wire [11:0] Simm = {io_inst[31:25],io_inst[11:7]}; // @[regfile.scala 83:51]
  wire [12:0] Bimm = {io_inst[31],io_inst[7],io_inst[30:25],io_inst[11:8],1'h0}; // @[regfile.scala 84:86]
  wire [31:0] Uimm = {io_inst[31:12],12'h0}; // @[regfile.scala 85:46]
  wire [20:0] Jimm = {io_inst[31],io_inst[19:12],io_inst[20],io_inst[30:21],1'h0}; // @[regfile.scala 86:88]
  wire [31:0] Zimm = {27'h0,io_inst[19:15]}; // @[regfile.scala 87:45]
  wire [4:0] Imm2 = io_inst[24:20]; // @[regfile.scala 88:29]
  wire [4:0] Vimm = io_inst[19:15]; // @[regfile.scala 89:29]
  wire [20:0] _out_T_3 = 3'h5 == io_sel ? $signed(Jimm) : $signed({{9{Iimm[11]}},Iimm}); // @[Mux.scala 81:58]
  wire [20:0] _out_T_5 = 3'h1 == io_sel ? $signed({{9{Simm[11]}},Simm}) : $signed(_out_T_3); // @[Mux.scala 81:58]
  wire [20:0] _out_T_7 = 3'h2 == io_sel ? $signed({{8{Bimm[12]}},Bimm}) : $signed(_out_T_5); // @[Mux.scala 81:58]
  wire [31:0] _out_T_9 = 3'h3 == io_sel ? $signed(Uimm) : $signed({{11{_out_T_7[20]}},_out_T_7}); // @[Mux.scala 81:58]
  wire [31:0] _out_T_11 = 3'h4 == io_sel ? $signed({{27{Imm2[4]}},Imm2}) : $signed(_out_T_9); // @[Mux.scala 81:58]
  wire [31:0] _out_T_13 = 3'h7 == io_sel ? $signed(Zimm) : $signed(_out_T_11); // @[Mux.scala 81:58]
  assign io_out = 3'h6 == io_sel ? $signed({{27{Vimm[4]}},Vimm}) : $signed(_out_T_13); // @[regfile.scala 94:15]
endmodule
module collectorUnit(
  input         clock,
  input         reset,
  output        io_control_ready,
  input         io_control_valid,
  input  [31:0] io_control_bits_inst,
  input  [1:0]  io_control_bits_wid,
  input         io_control_bits_fp,
  input  [1:0]  io_control_bits_branch,
  input         io_control_bits_simt_stack,
  input         io_control_bits_simt_stack_op,
  input         io_control_bits_barrier,
  input  [1:0]  io_control_bits_csr,
  input         io_control_bits_reverse,
  input  [1:0]  io_control_bits_sel_alu2,
  input  [1:0]  io_control_bits_sel_alu1,
  input         io_control_bits_isvec,
  input  [1:0]  io_control_bits_sel_alu3,
  input         io_control_bits_mask,
  input  [2:0]  io_control_bits_sel_imm,
  input  [1:0]  io_control_bits_mem_whb,
  input         io_control_bits_mem_unsigned,
  input  [5:0]  io_control_bits_alu_fn,
  input         io_control_bits_mem,
  input         io_control_bits_mul,
  input  [1:0]  io_control_bits_mem_cmd,
  input  [1:0]  io_control_bits_mop,
  input  [4:0]  io_control_bits_reg_idx1,
  input  [4:0]  io_control_bits_reg_idx2,
  input  [4:0]  io_control_bits_reg_idx3,
  input  [4:0]  io_control_bits_reg_idxw,
  input         io_control_bits_wfd,
  input         io_control_bits_fence,
  input         io_control_bits_sfu,
  input         io_control_bits_readmask,
  input         io_control_bits_writemask,
  input         io_control_bits_wxd,
  input  [31:0] io_control_bits_pc,
  input  [31:0] io_control_bits_spike_info_pc,
  input  [31:0] io_control_bits_spike_info_inst,
  output        io_bankIn_0_ready,
  input         io_bankIn_0_valid,
  input  [1:0]  io_bankIn_0_bits_regOrder,
  input  [31:0] io_bankIn_0_bits_data_0,
  input  [31:0] io_bankIn_0_bits_data_1,
  input  [31:0] io_bankIn_0_bits_data_2,
  input  [31:0] io_bankIn_0_bits_data_3,
  input  [31:0] io_bankIn_0_bits_v0_0,
  output        io_bankIn_1_ready,
  input         io_bankIn_1_valid,
  input  [1:0]  io_bankIn_1_bits_regOrder,
  input  [31:0] io_bankIn_1_bits_data_0,
  input  [31:0] io_bankIn_1_bits_data_1,
  input  [31:0] io_bankIn_1_bits_data_2,
  input  [31:0] io_bankIn_1_bits_data_3,
  input  [31:0] io_bankIn_1_bits_v0_0,
  output        io_bankIn_2_ready,
  input         io_bankIn_2_valid,
  input  [1:0]  io_bankIn_2_bits_regOrder,
  input  [31:0] io_bankIn_2_bits_data_0,
  input  [31:0] io_bankIn_2_bits_data_1,
  input  [31:0] io_bankIn_2_bits_data_2,
  input  [31:0] io_bankIn_2_bits_data_3,
  input  [31:0] io_bankIn_2_bits_v0_0,
  output        io_bankIn_3_ready,
  input         io_bankIn_3_valid,
  input  [1:0]  io_bankIn_3_bits_regOrder,
  input  [31:0] io_bankIn_3_bits_data_0,
  input  [31:0] io_bankIn_3_bits_data_1,
  input  [31:0] io_bankIn_3_bits_data_2,
  input  [31:0] io_bankIn_3_bits_data_3,
  input  [31:0] io_bankIn_3_bits_v0_0,
  input         io_issue_ready,
  output        io_issue_valid,
  output [31:0] io_issue_bits_alu_src1_0,
  output [31:0] io_issue_bits_alu_src1_1,
  output [31:0] io_issue_bits_alu_src1_2,
  output [31:0] io_issue_bits_alu_src1_3,
  output [31:0] io_issue_bits_alu_src2_0,
  output [31:0] io_issue_bits_alu_src2_1,
  output [31:0] io_issue_bits_alu_src2_2,
  output [31:0] io_issue_bits_alu_src2_3,
  output [31:0] io_issue_bits_alu_src3_0,
  output [31:0] io_issue_bits_alu_src3_1,
  output [31:0] io_issue_bits_alu_src3_2,
  output [31:0] io_issue_bits_alu_src3_3,
  output        io_issue_bits_mask_0,
  output        io_issue_bits_mask_1,
  output        io_issue_bits_mask_2,
  output        io_issue_bits_mask_3,
  output [31:0] io_issue_bits_control_inst,
  output [1:0]  io_issue_bits_control_wid,
  output        io_issue_bits_control_fp,
  output [1:0]  io_issue_bits_control_branch,
  output        io_issue_bits_control_simt_stack,
  output        io_issue_bits_control_simt_stack_op,
  output        io_issue_bits_control_barrier,
  output [1:0]  io_issue_bits_control_csr,
  output        io_issue_bits_control_reverse,
  output [1:0]  io_issue_bits_control_sel_alu2,
  output [1:0]  io_issue_bits_control_sel_alu1,
  output        io_issue_bits_control_isvec,
  output [1:0]  io_issue_bits_control_sel_alu3,
  output        io_issue_bits_control_mask,
  output [2:0]  io_issue_bits_control_sel_imm,
  output [1:0]  io_issue_bits_control_mem_whb,
  output        io_issue_bits_control_mem_unsigned,
  output [5:0]  io_issue_bits_control_alu_fn,
  output        io_issue_bits_control_mem,
  output        io_issue_bits_control_mul,
  output [1:0]  io_issue_bits_control_mem_cmd,
  output [1:0]  io_issue_bits_control_mop,
  output [4:0]  io_issue_bits_control_reg_idx1,
  output [4:0]  io_issue_bits_control_reg_idx2,
  output [4:0]  io_issue_bits_control_reg_idx3,
  output [4:0]  io_issue_bits_control_reg_idxw,
  output        io_issue_bits_control_wfd,
  output        io_issue_bits_control_fence,
  output        io_issue_bits_control_sfu,
  output        io_issue_bits_control_readmask,
  output        io_issue_bits_control_writemask,
  output        io_issue_bits_control_wxd,
  output [31:0] io_issue_bits_control_pc,
  output [31:0] io_issue_bits_control_spike_info_pc,
  output [31:0] io_issue_bits_control_spike_info_inst,
  output        io_outArbiterIO_0_valid,
  output [4:0]  io_outArbiterIO_0_bits_rsAddr,
  output [1:0]  io_outArbiterIO_0_bits_bankID,
  output [1:0]  io_outArbiterIO_0_bits_rsType,
  output        io_outArbiterIO_1_valid,
  output [4:0]  io_outArbiterIO_1_bits_rsAddr,
  output [1:0]  io_outArbiterIO_1_bits_bankID,
  output [1:0]  io_outArbiterIO_1_bits_rsType,
  output        io_outArbiterIO_2_valid,
  output [4:0]  io_outArbiterIO_2_bits_rsAddr,
  output [1:0]  io_outArbiterIO_2_bits_bankID,
  output [1:0]  io_outArbiterIO_2_bits_rsType,
  output        io_outArbiterIO_3_valid,
  output [4:0]  io_outArbiterIO_3_bits_rsAddr,
  output [1:0]  io_outArbiterIO_3_bits_bankID
);
`ifdef RANDOMIZE_REG_INIT
  reg [31:0] _RAND_0;
  reg [31:0] _RAND_1;
  reg [31:0] _RAND_2;
  reg [31:0] _RAND_3;
  reg [31:0] _RAND_4;
  reg [31:0] _RAND_5;
  reg [31:0] _RAND_6;
  reg [31:0] _RAND_7;
  reg [31:0] _RAND_8;
  reg [31:0] _RAND_9;
  reg [31:0] _RAND_10;
  reg [31:0] _RAND_11;
  reg [31:0] _RAND_12;
  reg [31:0] _RAND_13;
  reg [31:0] _RAND_14;
  reg [31:0] _RAND_15;
  reg [31:0] _RAND_16;
  reg [31:0] _RAND_17;
  reg [31:0] _RAND_18;
  reg [31:0] _RAND_19;
  reg [31:0] _RAND_20;
  reg [31:0] _RAND_21;
  reg [31:0] _RAND_22;
  reg [31:0] _RAND_23;
  reg [31:0] _RAND_24;
  reg [31:0] _RAND_25;
  reg [31:0] _RAND_26;
  reg [31:0] _RAND_27;
  reg [31:0] _RAND_28;
  reg [31:0] _RAND_29;
  reg [31:0] _RAND_30;
  reg [31:0] _RAND_31;
  reg [31:0] _RAND_32;
  reg [31:0] _RAND_33;
  reg [31:0] _RAND_34;
  reg [31:0] _RAND_35;
  reg [31:0] _RAND_36;
  reg [31:0] _RAND_37;
  reg [31:0] _RAND_38;
  reg [31:0] _RAND_39;
  reg [31:0] _RAND_40;
  reg [31:0] _RAND_41;
  reg [31:0] _RAND_42;
  reg [31:0] _RAND_43;
  reg [31:0] _RAND_44;
  reg [31:0] _RAND_45;
  reg [31:0] _RAND_46;
  reg [31:0] _RAND_47;
  reg [31:0] _RAND_48;
  reg [31:0] _RAND_49;
  reg [31:0] _RAND_50;
  reg [31:0] _RAND_51;
  reg [31:0] _RAND_52;
  reg [31:0] _RAND_53;
  reg [31:0] _RAND_54;
  reg [31:0] _RAND_55;
  reg [31:0] _RAND_56;
  reg [31:0] _RAND_57;
  reg [31:0] _RAND_58;
  reg [31:0] _RAND_59;
  reg [31:0] _RAND_60;
  reg [31:0] _RAND_61;
  reg [31:0] _RAND_62;
  reg [31:0] _RAND_63;
  reg [31:0] _RAND_64;
  reg [31:0] _RAND_65;
`endif // RANDOMIZE_REG_INIT
  wire [31:0] imm_io_inst; // @[operandCollector.scala 76:19]
  wire [2:0] imm_io_sel; // @[operandCollector.scala 76:19]
  wire [31:0] imm_io_out; // @[operandCollector.scala 76:19]
  reg [31:0] controlReg_inst; // @[operandCollector.scala 58:23]
  reg [1:0] controlReg_wid; // @[operandCollector.scala 58:23]
  reg  controlReg_fp; // @[operandCollector.scala 58:23]
  reg [1:0] controlReg_branch; // @[operandCollector.scala 58:23]
  reg  controlReg_simt_stack; // @[operandCollector.scala 58:23]
  reg  controlReg_simt_stack_op; // @[operandCollector.scala 58:23]
  reg  controlReg_barrier; // @[operandCollector.scala 58:23]
  reg [1:0] controlReg_csr; // @[operandCollector.scala 58:23]
  reg  controlReg_reverse; // @[operandCollector.scala 58:23]
  reg [1:0] controlReg_sel_alu2; // @[operandCollector.scala 58:23]
  reg [1:0] controlReg_sel_alu1; // @[operandCollector.scala 58:23]
  reg  controlReg_isvec; // @[operandCollector.scala 58:23]
  reg [1:0] controlReg_sel_alu3; // @[operandCollector.scala 58:23]
  reg  controlReg_mask; // @[operandCollector.scala 58:23]
  reg [2:0] controlReg_sel_imm; // @[operandCollector.scala 58:23]
  reg [1:0] controlReg_mem_whb; // @[operandCollector.scala 58:23]
  reg  controlReg_mem_unsigned; // @[operandCollector.scala 58:23]
  reg [5:0] controlReg_alu_fn; // @[operandCollector.scala 58:23]
  reg  controlReg_mem; // @[operandCollector.scala 58:23]
  reg  controlReg_mul; // @[operandCollector.scala 58:23]
  reg [1:0] controlReg_mem_cmd; // @[operandCollector.scala 58:23]
  reg [1:0] controlReg_mop; // @[operandCollector.scala 58:23]
  reg [4:0] controlReg_reg_idx1; // @[operandCollector.scala 58:23]
  reg [4:0] controlReg_reg_idx2; // @[operandCollector.scala 58:23]
  reg [4:0] controlReg_reg_idx3; // @[operandCollector.scala 58:23]
  reg [4:0] controlReg_reg_idxw; // @[operandCollector.scala 58:23]
  reg  controlReg_wfd; // @[operandCollector.scala 58:23]
  reg  controlReg_fence; // @[operandCollector.scala 58:23]
  reg  controlReg_sfu; // @[operandCollector.scala 58:23]
  reg  controlReg_readmask; // @[operandCollector.scala 58:23]
  reg  controlReg_writemask; // @[operandCollector.scala 58:23]
  reg  controlReg_wxd; // @[operandCollector.scala 58:23]
  reg [31:0] controlReg_pc; // @[operandCollector.scala 58:23]
  reg [31:0] controlReg_spike_info_pc; // @[operandCollector.scala 58:23]
  reg [31:0] controlReg_spike_info_inst; // @[operandCollector.scala 58:23]
  reg [1:0] rsType_0; // @[operandCollector.scala 65:19]
  reg [1:0] rsType_1; // @[operandCollector.scala 65:19]
  reg [1:0] rsType_2; // @[operandCollector.scala 65:19]
  reg  ready_0; // @[operandCollector.scala 66:18]
  reg  ready_1; // @[operandCollector.scala 66:18]
  reg  ready_2; // @[operandCollector.scala 66:18]
  reg  ready_3; // @[operandCollector.scala 66:18]
  reg  valid_0; // @[operandCollector.scala 67:18]
  reg  valid_1; // @[operandCollector.scala 67:18]
  reg  valid_2; // @[operandCollector.scala 67:18]
  reg  valid_3; // @[operandCollector.scala 67:18]
  reg [4:0] regIdx_0; // @[operandCollector.scala 68:19]
  reg [4:0] regIdx_1; // @[operandCollector.scala 68:19]
  reg [4:0] regIdx_2; // @[operandCollector.scala 68:19]
  reg [31:0] rsReg_0_0; // @[operandCollector.scala 69:22]
  reg [31:0] rsReg_0_1; // @[operandCollector.scala 69:22]
  reg [31:0] rsReg_0_2; // @[operandCollector.scala 69:22]
  reg [31:0] rsReg_0_3; // @[operandCollector.scala 69:22]
  reg [31:0] rsReg_1_0; // @[operandCollector.scala 69:22]
  reg [31:0] rsReg_1_1; // @[operandCollector.scala 69:22]
  reg [31:0] rsReg_1_2; // @[operandCollector.scala 69:22]
  reg [31:0] rsReg_1_3; // @[operandCollector.scala 69:22]
  reg [31:0] rsReg_2_0; // @[operandCollector.scala 69:22]
  reg [31:0] rsReg_2_1; // @[operandCollector.scala 69:22]
  reg [31:0] rsReg_2_2; // @[operandCollector.scala 69:22]
  reg [31:0] rsReg_2_3; // @[operandCollector.scala 69:22]
  reg  mask_0; // @[operandCollector.scala 70:17]
  reg  mask_1; // @[operandCollector.scala 70:17]
  reg  mask_2; // @[operandCollector.scala 70:17]
  reg  mask_3; // @[operandCollector.scala 70:17]
  reg [1:0] state; // @[operandCollector.scala 79:22]
  wire  _io_outArbiterIO_0_bits_bankID_T = io_control_ready & io_control_valid; // @[Decoupled.scala 52:35]
  wire  _io_outArbiterIO_0_bits_bankID_T_1 = state == 2'h0; // @[operandCollector.scala 96:78]
  wire  _io_outArbiterIO_0_bits_bankID_T_2 = _io_outArbiterIO_0_bits_bankID_T & state == 2'h0; // @[operandCollector.scala 96:70]
  wire  _T_12 = 2'h0 == state; // @[operandCollector.scala 143:16]
  wire [4:0] _GEN_77 = _io_outArbiterIO_0_bits_bankID_T ? io_control_bits_reg_idx1 : 5'h0; // @[operandCollector.scala 132:14 147:28 150:23]
  wire [4:0] regIdxWire_0 = 2'h0 == state ? _GEN_77 : 5'h0; // @[operandCollector.scala 132:14 143:16]
  wire [4:0] _GEN_512 = {{3'd0}, io_control_bits_wid}; // @[operandCollector.scala 96:109]
  wire [4:0] _io_outArbiterIO_0_bits_bankID_T_4 = _GEN_512 + regIdxWire_0; // @[operandCollector.scala 96:109]
  wire [4:0] _GEN_513 = {{3'd0}, controlReg_wid}; // @[operandCollector.scala 96:139]
  wire [4:0] _io_outArbiterIO_0_bits_bankID_T_6 = _GEN_513 + regIdx_0; // @[operandCollector.scala 96:139]
  wire [4:0] _io_outArbiterIO_0_bits_bankID_T_7 = _io_outArbiterIO_0_bits_bankID_T & state == 2'h0 ?
    _io_outArbiterIO_0_bits_bankID_T_4 : _io_outArbiterIO_0_bits_bankID_T_6; // @[operandCollector.scala 96:54]
  wire [1:0] _io_outArbiterIO_0_bits_bankID_T_11 = 5'h2 == _io_outArbiterIO_0_bits_bankID_T_7 ? 2'h2 : {{1'd0}, 5'h1 ==
    _io_outArbiterIO_0_bits_bankID_T_7}; // @[Mux.scala 81:58]
  wire [1:0] _io_outArbiterIO_0_bits_bankID_T_13 = 5'h3 == _io_outArbiterIO_0_bits_bankID_T_7 ? 2'h3 :
    _io_outArbiterIO_0_bits_bankID_T_11; // @[Mux.scala 81:58]
  wire [1:0] _io_outArbiterIO_0_bits_bankID_T_15 = 5'h4 == _io_outArbiterIO_0_bits_bankID_T_7 ? 2'h0 :
    _io_outArbiterIO_0_bits_bankID_T_13; // @[Mux.scala 81:58]
  wire [1:0] _io_outArbiterIO_0_bits_bankID_T_17 = 5'h5 == _io_outArbiterIO_0_bits_bankID_T_7 ? 2'h1 :
    _io_outArbiterIO_0_bits_bankID_T_15; // @[Mux.scala 81:58]
  wire [1:0] _io_outArbiterIO_0_bits_bankID_T_19 = 5'h6 == _io_outArbiterIO_0_bits_bankID_T_7 ? 2'h2 :
    _io_outArbiterIO_0_bits_bankID_T_17; // @[Mux.scala 81:58]
  wire [1:0] _io_outArbiterIO_0_bits_bankID_T_21 = 5'h7 == _io_outArbiterIO_0_bits_bankID_T_7 ? 2'h3 :
    _io_outArbiterIO_0_bits_bankID_T_19; // @[Mux.scala 81:58]
  wire [1:0] _io_outArbiterIO_0_bits_bankID_T_23 = 5'h8 == _io_outArbiterIO_0_bits_bankID_T_7 ? 2'h0 :
    _io_outArbiterIO_0_bits_bankID_T_21; // @[Mux.scala 81:58]
  wire [1:0] _io_outArbiterIO_0_bits_bankID_T_25 = 5'h9 == _io_outArbiterIO_0_bits_bankID_T_7 ? 2'h1 :
    _io_outArbiterIO_0_bits_bankID_T_23; // @[Mux.scala 81:58]
  wire [1:0] _io_outArbiterIO_0_bits_bankID_T_27 = 5'ha == _io_outArbiterIO_0_bits_bankID_T_7 ? 2'h2 :
    _io_outArbiterIO_0_bits_bankID_T_25; // @[Mux.scala 81:58]
  wire [1:0] _io_outArbiterIO_0_bits_bankID_T_29 = 5'hb == _io_outArbiterIO_0_bits_bankID_T_7 ? 2'h3 :
    _io_outArbiterIO_0_bits_bankID_T_27; // @[Mux.scala 81:58]
  wire [1:0] _io_outArbiterIO_0_bits_bankID_T_31 = 5'hc == _io_outArbiterIO_0_bits_bankID_T_7 ? 2'h0 :
    _io_outArbiterIO_0_bits_bankID_T_29; // @[Mux.scala 81:58]
  wire [1:0] _io_outArbiterIO_0_bits_bankID_T_33 = 5'hd == _io_outArbiterIO_0_bits_bankID_T_7 ? 2'h1 :
    _io_outArbiterIO_0_bits_bankID_T_31; // @[Mux.scala 81:58]
  wire [1:0] _io_outArbiterIO_0_bits_bankID_T_35 = 5'he == _io_outArbiterIO_0_bits_bankID_T_7 ? 2'h2 :
    _io_outArbiterIO_0_bits_bankID_T_33; // @[Mux.scala 81:58]
  wire [1:0] _io_outArbiterIO_0_bits_bankID_T_37 = 5'hf == _io_outArbiterIO_0_bits_bankID_T_7 ? 2'h3 :
    _io_outArbiterIO_0_bits_bankID_T_35; // @[Mux.scala 81:58]
  wire [1:0] _io_outArbiterIO_0_bits_bankID_T_39 = 5'h10 == _io_outArbiterIO_0_bits_bankID_T_7 ? 2'h0 :
    _io_outArbiterIO_0_bits_bankID_T_37; // @[Mux.scala 81:58]
  wire [1:0] _io_outArbiterIO_0_bits_bankID_T_41 = 5'h11 == _io_outArbiterIO_0_bits_bankID_T_7 ? 2'h1 :
    _io_outArbiterIO_0_bits_bankID_T_39; // @[Mux.scala 81:58]
  wire [1:0] _io_outArbiterIO_0_bits_bankID_T_43 = 5'h12 == _io_outArbiterIO_0_bits_bankID_T_7 ? 2'h2 :
    _io_outArbiterIO_0_bits_bankID_T_41; // @[Mux.scala 81:58]
  wire [1:0] _io_outArbiterIO_0_bits_bankID_T_45 = 5'h13 == _io_outArbiterIO_0_bits_bankID_T_7 ? 2'h3 :
    _io_outArbiterIO_0_bits_bankID_T_43; // @[Mux.scala 81:58]
  wire [1:0] _io_outArbiterIO_0_bits_bankID_T_47 = 5'h14 == _io_outArbiterIO_0_bits_bankID_T_7 ? 2'h0 :
    _io_outArbiterIO_0_bits_bankID_T_45; // @[Mux.scala 81:58]
  wire [1:0] _io_outArbiterIO_0_bits_bankID_T_49 = 5'h15 == _io_outArbiterIO_0_bits_bankID_T_7 ? 2'h1 :
    _io_outArbiterIO_0_bits_bankID_T_47; // @[Mux.scala 81:58]
  wire [1:0] _io_outArbiterIO_0_bits_bankID_T_51 = 5'h16 == _io_outArbiterIO_0_bits_bankID_T_7 ? 2'h2 :
    _io_outArbiterIO_0_bits_bankID_T_49; // @[Mux.scala 81:58]
  wire [1:0] _io_outArbiterIO_0_bits_bankID_T_53 = 5'h17 == _io_outArbiterIO_0_bits_bankID_T_7 ? 2'h3 :
    _io_outArbiterIO_0_bits_bankID_T_51; // @[Mux.scala 81:58]
  wire [1:0] _io_outArbiterIO_0_bits_bankID_T_55 = 5'h18 == _io_outArbiterIO_0_bits_bankID_T_7 ? 2'h0 :
    _io_outArbiterIO_0_bits_bankID_T_53; // @[Mux.scala 81:58]
  wire [1:0] _io_outArbiterIO_0_bits_bankID_T_57 = 5'h19 == _io_outArbiterIO_0_bits_bankID_T_7 ? 2'h1 :
    _io_outArbiterIO_0_bits_bankID_T_55; // @[Mux.scala 81:58]
  wire [1:0] _io_outArbiterIO_0_bits_bankID_T_59 = 5'h1a == _io_outArbiterIO_0_bits_bankID_T_7 ? 2'h2 :
    _io_outArbiterIO_0_bits_bankID_T_57; // @[Mux.scala 81:58]
  wire [1:0] _io_outArbiterIO_0_bits_bankID_T_61 = 5'h1b == _io_outArbiterIO_0_bits_bankID_T_7 ? 2'h3 :
    _io_outArbiterIO_0_bits_bankID_T_59; // @[Mux.scala 81:58]
  wire [1:0] _io_outArbiterIO_0_bits_bankID_T_63 = 5'h1c == _io_outArbiterIO_0_bits_bankID_T_7 ? 2'h0 :
    _io_outArbiterIO_0_bits_bankID_T_61; // @[Mux.scala 81:58]
  wire [1:0] _io_outArbiterIO_0_bits_bankID_T_65 = 5'h1d == _io_outArbiterIO_0_bits_bankID_T_7 ? 2'h1 :
    _io_outArbiterIO_0_bits_bankID_T_63; // @[Mux.scala 81:58]
  wire [1:0] _io_outArbiterIO_0_bits_bankID_T_67 = 5'h1e == _io_outArbiterIO_0_bits_bankID_T_7 ? 2'h2 :
    _io_outArbiterIO_0_bits_bankID_T_65; // @[Mux.scala 81:58]
  wire [1:0] _io_outArbiterIO_0_bits_bankID_T_69 = 5'h1f == _io_outArbiterIO_0_bits_bankID_T_7 ? 2'h3 :
    _io_outArbiterIO_0_bits_bankID_T_67; // @[Mux.scala 81:58]
  wire [5:0] _GEN_514 = {{1'd0}, _io_outArbiterIO_0_bits_bankID_T_7}; // @[Mux.scala 81:61]
  wire [1:0] _io_outArbiterIO_0_bits_bankID_T_71 = 6'h20 == _GEN_514 ? 2'h0 : _io_outArbiterIO_0_bits_bankID_T_69; // @[Mux.scala 81:58]
  wire [1:0] _io_outArbiterIO_0_bits_bankID_T_73 = 6'h21 == _GEN_514 ? 2'h1 : _io_outArbiterIO_0_bits_bankID_T_71; // @[Mux.scala 81:58]
  wire [1:0] _io_outArbiterIO_0_bits_bankID_T_75 = 6'h22 == _GEN_514 ? 2'h2 : _io_outArbiterIO_0_bits_bankID_T_73; // @[Mux.scala 81:58]
  wire [4:0] _io_outArbiterIO_0_bits_rsAddr_T_3 = _io_outArbiterIO_0_bits_bankID_T_2 ? regIdxWire_0 : regIdx_0; // @[operandCollector.scala 97:54]
  wire  _io_outArbiterIO_0_bits_rsAddr_T_17 = 5'h7 == _io_outArbiterIO_0_bits_rsAddr_T_3 | (5'h6 ==
    _io_outArbiterIO_0_bits_rsAddr_T_3 | (5'h5 == _io_outArbiterIO_0_bits_rsAddr_T_3 | 5'h4 ==
    _io_outArbiterIO_0_bits_rsAddr_T_3)); // @[Mux.scala 81:58]
  wire [1:0] _io_outArbiterIO_0_bits_rsAddr_T_19 = 5'h8 == _io_outArbiterIO_0_bits_rsAddr_T_3 ? 2'h2 : {{1'd0},
    _io_outArbiterIO_0_bits_rsAddr_T_17}; // @[Mux.scala 81:58]
  wire [1:0] _io_outArbiterIO_0_bits_rsAddr_T_21 = 5'h9 == _io_outArbiterIO_0_bits_rsAddr_T_3 ? 2'h2 :
    _io_outArbiterIO_0_bits_rsAddr_T_19; // @[Mux.scala 81:58]
  wire [1:0] _io_outArbiterIO_0_bits_rsAddr_T_23 = 5'ha == _io_outArbiterIO_0_bits_rsAddr_T_3 ? 2'h2 :
    _io_outArbiterIO_0_bits_rsAddr_T_21; // @[Mux.scala 81:58]
  wire [1:0] _io_outArbiterIO_0_bits_rsAddr_T_25 = 5'hb == _io_outArbiterIO_0_bits_rsAddr_T_3 ? 2'h2 :
    _io_outArbiterIO_0_bits_rsAddr_T_23; // @[Mux.scala 81:58]
  wire [1:0] _io_outArbiterIO_0_bits_rsAddr_T_27 = 5'hc == _io_outArbiterIO_0_bits_rsAddr_T_3 ? 2'h3 :
    _io_outArbiterIO_0_bits_rsAddr_T_25; // @[Mux.scala 81:58]
  wire [1:0] _io_outArbiterIO_0_bits_rsAddr_T_29 = 5'hd == _io_outArbiterIO_0_bits_rsAddr_T_3 ? 2'h3 :
    _io_outArbiterIO_0_bits_rsAddr_T_27; // @[Mux.scala 81:58]
  wire [1:0] _io_outArbiterIO_0_bits_rsAddr_T_31 = 5'he == _io_outArbiterIO_0_bits_rsAddr_T_3 ? 2'h3 :
    _io_outArbiterIO_0_bits_rsAddr_T_29; // @[Mux.scala 81:58]
  wire [1:0] _io_outArbiterIO_0_bits_rsAddr_T_33 = 5'hf == _io_outArbiterIO_0_bits_rsAddr_T_3 ? 2'h3 :
    _io_outArbiterIO_0_bits_rsAddr_T_31; // @[Mux.scala 81:58]
  wire [2:0] _io_outArbiterIO_0_bits_rsAddr_T_35 = 5'h10 == _io_outArbiterIO_0_bits_rsAddr_T_3 ? 3'h4 : {{1'd0},
    _io_outArbiterIO_0_bits_rsAddr_T_33}; // @[Mux.scala 81:58]
  wire [2:0] _io_outArbiterIO_0_bits_rsAddr_T_37 = 5'h11 == _io_outArbiterIO_0_bits_rsAddr_T_3 ? 3'h4 :
    _io_outArbiterIO_0_bits_rsAddr_T_35; // @[Mux.scala 81:58]
  wire [2:0] _io_outArbiterIO_0_bits_rsAddr_T_39 = 5'h12 == _io_outArbiterIO_0_bits_rsAddr_T_3 ? 3'h4 :
    _io_outArbiterIO_0_bits_rsAddr_T_37; // @[Mux.scala 81:58]
  wire [2:0] _io_outArbiterIO_0_bits_rsAddr_T_41 = 5'h13 == _io_outArbiterIO_0_bits_rsAddr_T_3 ? 3'h4 :
    _io_outArbiterIO_0_bits_rsAddr_T_39; // @[Mux.scala 81:58]
  wire [2:0] _io_outArbiterIO_0_bits_rsAddr_T_43 = 5'h14 == _io_outArbiterIO_0_bits_rsAddr_T_3 ? 3'h5 :
    _io_outArbiterIO_0_bits_rsAddr_T_41; // @[Mux.scala 81:58]
  wire [2:0] _io_outArbiterIO_0_bits_rsAddr_T_45 = 5'h15 == _io_outArbiterIO_0_bits_rsAddr_T_3 ? 3'h5 :
    _io_outArbiterIO_0_bits_rsAddr_T_43; // @[Mux.scala 81:58]
  wire [2:0] _io_outArbiterIO_0_bits_rsAddr_T_47 = 5'h16 == _io_outArbiterIO_0_bits_rsAddr_T_3 ? 3'h5 :
    _io_outArbiterIO_0_bits_rsAddr_T_45; // @[Mux.scala 81:58]
  wire [2:0] _io_outArbiterIO_0_bits_rsAddr_T_49 = 5'h17 == _io_outArbiterIO_0_bits_rsAddr_T_3 ? 3'h5 :
    _io_outArbiterIO_0_bits_rsAddr_T_47; // @[Mux.scala 81:58]
  wire [2:0] _io_outArbiterIO_0_bits_rsAddr_T_51 = 5'h18 == _io_outArbiterIO_0_bits_rsAddr_T_3 ? 3'h6 :
    _io_outArbiterIO_0_bits_rsAddr_T_49; // @[Mux.scala 81:58]
  wire [2:0] _io_outArbiterIO_0_bits_rsAddr_T_53 = 5'h19 == _io_outArbiterIO_0_bits_rsAddr_T_3 ? 3'h6 :
    _io_outArbiterIO_0_bits_rsAddr_T_51; // @[Mux.scala 81:58]
  wire [2:0] _io_outArbiterIO_0_bits_rsAddr_T_55 = 5'h1a == _io_outArbiterIO_0_bits_rsAddr_T_3 ? 3'h6 :
    _io_outArbiterIO_0_bits_rsAddr_T_53; // @[Mux.scala 81:58]
  wire [2:0] _io_outArbiterIO_0_bits_rsAddr_T_57 = 5'h1b == _io_outArbiterIO_0_bits_rsAddr_T_3 ? 3'h6 :
    _io_outArbiterIO_0_bits_rsAddr_T_55; // @[Mux.scala 81:58]
  wire [2:0] _io_outArbiterIO_0_bits_rsAddr_T_59 = 5'h1c == _io_outArbiterIO_0_bits_rsAddr_T_3 ? 3'h7 :
    _io_outArbiterIO_0_bits_rsAddr_T_57; // @[Mux.scala 81:58]
  wire [2:0] _io_outArbiterIO_0_bits_rsAddr_T_61 = 5'h1d == _io_outArbiterIO_0_bits_rsAddr_T_3 ? 3'h7 :
    _io_outArbiterIO_0_bits_rsAddr_T_59; // @[Mux.scala 81:58]
  wire [2:0] _io_outArbiterIO_0_bits_rsAddr_T_63 = 5'h1e == _io_outArbiterIO_0_bits_rsAddr_T_3 ? 3'h7 :
    _io_outArbiterIO_0_bits_rsAddr_T_61; // @[Mux.scala 81:58]
  wire [2:0] _io_outArbiterIO_0_bits_rsAddr_T_65 = 5'h1f == _io_outArbiterIO_0_bits_rsAddr_T_3 ? 3'h7 :
    _io_outArbiterIO_0_bits_rsAddr_T_63; // @[Mux.scala 81:58]
  wire [1:0] _io_outArbiterIO_0_bits_rsAddr_T_69 = _io_outArbiterIO_0_bits_bankID_T_2 ? io_control_bits_wid :
    controlReg_wid; // @[operandCollector.scala 98:12]
  wire [5:0] _io_outArbiterIO_0_bits_rsAddr_T_70 = _io_outArbiterIO_0_bits_rsAddr_T_69 * 4'h8; // @[operandCollector.scala 98:84]
  wire [5:0] _GEN_518 = {{3'd0}, _io_outArbiterIO_0_bits_rsAddr_T_65}; // @[operandCollector.scala 97:134]
  wire [5:0] _io_outArbiterIO_0_bits_rsAddr_T_72 = _GEN_518 + _io_outArbiterIO_0_bits_rsAddr_T_70; // @[operandCollector.scala 97:134]
  wire [1:0] _GEN_93 = _io_outArbiterIO_0_bits_bankID_T ? io_control_bits_sel_alu1 : 2'h0; // @[operandCollector.scala 133:14 147:28 167:23]
  wire [1:0] rsTypeWire_0 = 2'h0 == state ? _GEN_93 : 2'h0; // @[operandCollector.scala 133:14 143:16]
  wire [4:0] _GEN_78 = _io_outArbiterIO_0_bits_bankID_T ? io_control_bits_reg_idx2 : 5'h0; // @[operandCollector.scala 132:14 147:28 151:23]
  wire [4:0] regIdxWire_1 = 2'h0 == state ? _GEN_78 : 5'h0; // @[operandCollector.scala 132:14 143:16]
  wire [4:0] _io_outArbiterIO_1_bits_bankID_T_4 = _GEN_512 + regIdxWire_1; // @[operandCollector.scala 96:109]
  wire [4:0] _io_outArbiterIO_1_bits_bankID_T_6 = _GEN_513 + regIdx_1; // @[operandCollector.scala 96:139]
  wire [4:0] _io_outArbiterIO_1_bits_bankID_T_7 = _io_outArbiterIO_0_bits_bankID_T & state == 2'h0 ?
    _io_outArbiterIO_1_bits_bankID_T_4 : _io_outArbiterIO_1_bits_bankID_T_6; // @[operandCollector.scala 96:54]
  wire [1:0] _io_outArbiterIO_1_bits_bankID_T_11 = 5'h2 == _io_outArbiterIO_1_bits_bankID_T_7 ? 2'h2 : {{1'd0}, 5'h1 ==
    _io_outArbiterIO_1_bits_bankID_T_7}; // @[Mux.scala 81:58]
  wire [1:0] _io_outArbiterIO_1_bits_bankID_T_13 = 5'h3 == _io_outArbiterIO_1_bits_bankID_T_7 ? 2'h3 :
    _io_outArbiterIO_1_bits_bankID_T_11; // @[Mux.scala 81:58]
  wire [1:0] _io_outArbiterIO_1_bits_bankID_T_15 = 5'h4 == _io_outArbiterIO_1_bits_bankID_T_7 ? 2'h0 :
    _io_outArbiterIO_1_bits_bankID_T_13; // @[Mux.scala 81:58]
  wire [1:0] _io_outArbiterIO_1_bits_bankID_T_17 = 5'h5 == _io_outArbiterIO_1_bits_bankID_T_7 ? 2'h1 :
    _io_outArbiterIO_1_bits_bankID_T_15; // @[Mux.scala 81:58]
  wire [1:0] _io_outArbiterIO_1_bits_bankID_T_19 = 5'h6 == _io_outArbiterIO_1_bits_bankID_T_7 ? 2'h2 :
    _io_outArbiterIO_1_bits_bankID_T_17; // @[Mux.scala 81:58]
  wire [1:0] _io_outArbiterIO_1_bits_bankID_T_21 = 5'h7 == _io_outArbiterIO_1_bits_bankID_T_7 ? 2'h3 :
    _io_outArbiterIO_1_bits_bankID_T_19; // @[Mux.scala 81:58]
  wire [1:0] _io_outArbiterIO_1_bits_bankID_T_23 = 5'h8 == _io_outArbiterIO_1_bits_bankID_T_7 ? 2'h0 :
    _io_outArbiterIO_1_bits_bankID_T_21; // @[Mux.scala 81:58]
  wire [1:0] _io_outArbiterIO_1_bits_bankID_T_25 = 5'h9 == _io_outArbiterIO_1_bits_bankID_T_7 ? 2'h1 :
    _io_outArbiterIO_1_bits_bankID_T_23; // @[Mux.scala 81:58]
  wire [1:0] _io_outArbiterIO_1_bits_bankID_T_27 = 5'ha == _io_outArbiterIO_1_bits_bankID_T_7 ? 2'h2 :
    _io_outArbiterIO_1_bits_bankID_T_25; // @[Mux.scala 81:58]
  wire [1:0] _io_outArbiterIO_1_bits_bankID_T_29 = 5'hb == _io_outArbiterIO_1_bits_bankID_T_7 ? 2'h3 :
    _io_outArbiterIO_1_bits_bankID_T_27; // @[Mux.scala 81:58]
  wire [1:0] _io_outArbiterIO_1_bits_bankID_T_31 = 5'hc == _io_outArbiterIO_1_bits_bankID_T_7 ? 2'h0 :
    _io_outArbiterIO_1_bits_bankID_T_29; // @[Mux.scala 81:58]
  wire [1:0] _io_outArbiterIO_1_bits_bankID_T_33 = 5'hd == _io_outArbiterIO_1_bits_bankID_T_7 ? 2'h1 :
    _io_outArbiterIO_1_bits_bankID_T_31; // @[Mux.scala 81:58]
  wire [1:0] _io_outArbiterIO_1_bits_bankID_T_35 = 5'he == _io_outArbiterIO_1_bits_bankID_T_7 ? 2'h2 :
    _io_outArbiterIO_1_bits_bankID_T_33; // @[Mux.scala 81:58]
  wire [1:0] _io_outArbiterIO_1_bits_bankID_T_37 = 5'hf == _io_outArbiterIO_1_bits_bankID_T_7 ? 2'h3 :
    _io_outArbiterIO_1_bits_bankID_T_35; // @[Mux.scala 81:58]
  wire [1:0] _io_outArbiterIO_1_bits_bankID_T_39 = 5'h10 == _io_outArbiterIO_1_bits_bankID_T_7 ? 2'h0 :
    _io_outArbiterIO_1_bits_bankID_T_37; // @[Mux.scala 81:58]
  wire [1:0] _io_outArbiterIO_1_bits_bankID_T_41 = 5'h11 == _io_outArbiterIO_1_bits_bankID_T_7 ? 2'h1 :
    _io_outArbiterIO_1_bits_bankID_T_39; // @[Mux.scala 81:58]
  wire [1:0] _io_outArbiterIO_1_bits_bankID_T_43 = 5'h12 == _io_outArbiterIO_1_bits_bankID_T_7 ? 2'h2 :
    _io_outArbiterIO_1_bits_bankID_T_41; // @[Mux.scala 81:58]
  wire [1:0] _io_outArbiterIO_1_bits_bankID_T_45 = 5'h13 == _io_outArbiterIO_1_bits_bankID_T_7 ? 2'h3 :
    _io_outArbiterIO_1_bits_bankID_T_43; // @[Mux.scala 81:58]
  wire [1:0] _io_outArbiterIO_1_bits_bankID_T_47 = 5'h14 == _io_outArbiterIO_1_bits_bankID_T_7 ? 2'h0 :
    _io_outArbiterIO_1_bits_bankID_T_45; // @[Mux.scala 81:58]
  wire [1:0] _io_outArbiterIO_1_bits_bankID_T_49 = 5'h15 == _io_outArbiterIO_1_bits_bankID_T_7 ? 2'h1 :
    _io_outArbiterIO_1_bits_bankID_T_47; // @[Mux.scala 81:58]
  wire [1:0] _io_outArbiterIO_1_bits_bankID_T_51 = 5'h16 == _io_outArbiterIO_1_bits_bankID_T_7 ? 2'h2 :
    _io_outArbiterIO_1_bits_bankID_T_49; // @[Mux.scala 81:58]
  wire [1:0] _io_outArbiterIO_1_bits_bankID_T_53 = 5'h17 == _io_outArbiterIO_1_bits_bankID_T_7 ? 2'h3 :
    _io_outArbiterIO_1_bits_bankID_T_51; // @[Mux.scala 81:58]
  wire [1:0] _io_outArbiterIO_1_bits_bankID_T_55 = 5'h18 == _io_outArbiterIO_1_bits_bankID_T_7 ? 2'h0 :
    _io_outArbiterIO_1_bits_bankID_T_53; // @[Mux.scala 81:58]
  wire [1:0] _io_outArbiterIO_1_bits_bankID_T_57 = 5'h19 == _io_outArbiterIO_1_bits_bankID_T_7 ? 2'h1 :
    _io_outArbiterIO_1_bits_bankID_T_55; // @[Mux.scala 81:58]
  wire [1:0] _io_outArbiterIO_1_bits_bankID_T_59 = 5'h1a == _io_outArbiterIO_1_bits_bankID_T_7 ? 2'h2 :
    _io_outArbiterIO_1_bits_bankID_T_57; // @[Mux.scala 81:58]
  wire [1:0] _io_outArbiterIO_1_bits_bankID_T_61 = 5'h1b == _io_outArbiterIO_1_bits_bankID_T_7 ? 2'h3 :
    _io_outArbiterIO_1_bits_bankID_T_59; // @[Mux.scala 81:58]
  wire [1:0] _io_outArbiterIO_1_bits_bankID_T_63 = 5'h1c == _io_outArbiterIO_1_bits_bankID_T_7 ? 2'h0 :
    _io_outArbiterIO_1_bits_bankID_T_61; // @[Mux.scala 81:58]
  wire [1:0] _io_outArbiterIO_1_bits_bankID_T_65 = 5'h1d == _io_outArbiterIO_1_bits_bankID_T_7 ? 2'h1 :
    _io_outArbiterIO_1_bits_bankID_T_63; // @[Mux.scala 81:58]
  wire [1:0] _io_outArbiterIO_1_bits_bankID_T_67 = 5'h1e == _io_outArbiterIO_1_bits_bankID_T_7 ? 2'h2 :
    _io_outArbiterIO_1_bits_bankID_T_65; // @[Mux.scala 81:58]
  wire [1:0] _io_outArbiterIO_1_bits_bankID_T_69 = 5'h1f == _io_outArbiterIO_1_bits_bankID_T_7 ? 2'h3 :
    _io_outArbiterIO_1_bits_bankID_T_67; // @[Mux.scala 81:58]
  wire [5:0] _GEN_521 = {{1'd0}, _io_outArbiterIO_1_bits_bankID_T_7}; // @[Mux.scala 81:61]
  wire [1:0] _io_outArbiterIO_1_bits_bankID_T_71 = 6'h20 == _GEN_521 ? 2'h0 : _io_outArbiterIO_1_bits_bankID_T_69; // @[Mux.scala 81:58]
  wire [1:0] _io_outArbiterIO_1_bits_bankID_T_73 = 6'h21 == _GEN_521 ? 2'h1 : _io_outArbiterIO_1_bits_bankID_T_71; // @[Mux.scala 81:58]
  wire [1:0] _io_outArbiterIO_1_bits_bankID_T_75 = 6'h22 == _GEN_521 ? 2'h2 : _io_outArbiterIO_1_bits_bankID_T_73; // @[Mux.scala 81:58]
  wire [4:0] _io_outArbiterIO_1_bits_rsAddr_T_3 = _io_outArbiterIO_0_bits_bankID_T_2 ? regIdxWire_1 : regIdx_1; // @[operandCollector.scala 97:54]
  wire  _io_outArbiterIO_1_bits_rsAddr_T_17 = 5'h7 == _io_outArbiterIO_1_bits_rsAddr_T_3 | (5'h6 ==
    _io_outArbiterIO_1_bits_rsAddr_T_3 | (5'h5 == _io_outArbiterIO_1_bits_rsAddr_T_3 | 5'h4 ==
    _io_outArbiterIO_1_bits_rsAddr_T_3)); // @[Mux.scala 81:58]
  wire [1:0] _io_outArbiterIO_1_bits_rsAddr_T_19 = 5'h8 == _io_outArbiterIO_1_bits_rsAddr_T_3 ? 2'h2 : {{1'd0},
    _io_outArbiterIO_1_bits_rsAddr_T_17}; // @[Mux.scala 81:58]
  wire [1:0] _io_outArbiterIO_1_bits_rsAddr_T_21 = 5'h9 == _io_outArbiterIO_1_bits_rsAddr_T_3 ? 2'h2 :
    _io_outArbiterIO_1_bits_rsAddr_T_19; // @[Mux.scala 81:58]
  wire [1:0] _io_outArbiterIO_1_bits_rsAddr_T_23 = 5'ha == _io_outArbiterIO_1_bits_rsAddr_T_3 ? 2'h2 :
    _io_outArbiterIO_1_bits_rsAddr_T_21; // @[Mux.scala 81:58]
  wire [1:0] _io_outArbiterIO_1_bits_rsAddr_T_25 = 5'hb == _io_outArbiterIO_1_bits_rsAddr_T_3 ? 2'h2 :
    _io_outArbiterIO_1_bits_rsAddr_T_23; // @[Mux.scala 81:58]
  wire [1:0] _io_outArbiterIO_1_bits_rsAddr_T_27 = 5'hc == _io_outArbiterIO_1_bits_rsAddr_T_3 ? 2'h3 :
    _io_outArbiterIO_1_bits_rsAddr_T_25; // @[Mux.scala 81:58]
  wire [1:0] _io_outArbiterIO_1_bits_rsAddr_T_29 = 5'hd == _io_outArbiterIO_1_bits_rsAddr_T_3 ? 2'h3 :
    _io_outArbiterIO_1_bits_rsAddr_T_27; // @[Mux.scala 81:58]
  wire [1:0] _io_outArbiterIO_1_bits_rsAddr_T_31 = 5'he == _io_outArbiterIO_1_bits_rsAddr_T_3 ? 2'h3 :
    _io_outArbiterIO_1_bits_rsAddr_T_29; // @[Mux.scala 81:58]
  wire [1:0] _io_outArbiterIO_1_bits_rsAddr_T_33 = 5'hf == _io_outArbiterIO_1_bits_rsAddr_T_3 ? 2'h3 :
    _io_outArbiterIO_1_bits_rsAddr_T_31; // @[Mux.scala 81:58]
  wire [2:0] _io_outArbiterIO_1_bits_rsAddr_T_35 = 5'h10 == _io_outArbiterIO_1_bits_rsAddr_T_3 ? 3'h4 : {{1'd0},
    _io_outArbiterIO_1_bits_rsAddr_T_33}; // @[Mux.scala 81:58]
  wire [2:0] _io_outArbiterIO_1_bits_rsAddr_T_37 = 5'h11 == _io_outArbiterIO_1_bits_rsAddr_T_3 ? 3'h4 :
    _io_outArbiterIO_1_bits_rsAddr_T_35; // @[Mux.scala 81:58]
  wire [2:0] _io_outArbiterIO_1_bits_rsAddr_T_39 = 5'h12 == _io_outArbiterIO_1_bits_rsAddr_T_3 ? 3'h4 :
    _io_outArbiterIO_1_bits_rsAddr_T_37; // @[Mux.scala 81:58]
  wire [2:0] _io_outArbiterIO_1_bits_rsAddr_T_41 = 5'h13 == _io_outArbiterIO_1_bits_rsAddr_T_3 ? 3'h4 :
    _io_outArbiterIO_1_bits_rsAddr_T_39; // @[Mux.scala 81:58]
  wire [2:0] _io_outArbiterIO_1_bits_rsAddr_T_43 = 5'h14 == _io_outArbiterIO_1_bits_rsAddr_T_3 ? 3'h5 :
    _io_outArbiterIO_1_bits_rsAddr_T_41; // @[Mux.scala 81:58]
  wire [2:0] _io_outArbiterIO_1_bits_rsAddr_T_45 = 5'h15 == _io_outArbiterIO_1_bits_rsAddr_T_3 ? 3'h5 :
    _io_outArbiterIO_1_bits_rsAddr_T_43; // @[Mux.scala 81:58]
  wire [2:0] _io_outArbiterIO_1_bits_rsAddr_T_47 = 5'h16 == _io_outArbiterIO_1_bits_rsAddr_T_3 ? 3'h5 :
    _io_outArbiterIO_1_bits_rsAddr_T_45; // @[Mux.scala 81:58]
  wire [2:0] _io_outArbiterIO_1_bits_rsAddr_T_49 = 5'h17 == _io_outArbiterIO_1_bits_rsAddr_T_3 ? 3'h5 :
    _io_outArbiterIO_1_bits_rsAddr_T_47; // @[Mux.scala 81:58]
  wire [2:0] _io_outArbiterIO_1_bits_rsAddr_T_51 = 5'h18 == _io_outArbiterIO_1_bits_rsAddr_T_3 ? 3'h6 :
    _io_outArbiterIO_1_bits_rsAddr_T_49; // @[Mux.scala 81:58]
  wire [2:0] _io_outArbiterIO_1_bits_rsAddr_T_53 = 5'h19 == _io_outArbiterIO_1_bits_rsAddr_T_3 ? 3'h6 :
    _io_outArbiterIO_1_bits_rsAddr_T_51; // @[Mux.scala 81:58]
  wire [2:0] _io_outArbiterIO_1_bits_rsAddr_T_55 = 5'h1a == _io_outArbiterIO_1_bits_rsAddr_T_3 ? 3'h6 :
    _io_outArbiterIO_1_bits_rsAddr_T_53; // @[Mux.scala 81:58]
  wire [2:0] _io_outArbiterIO_1_bits_rsAddr_T_57 = 5'h1b == _io_outArbiterIO_1_bits_rsAddr_T_3 ? 3'h6 :
    _io_outArbiterIO_1_bits_rsAddr_T_55; // @[Mux.scala 81:58]
  wire [2:0] _io_outArbiterIO_1_bits_rsAddr_T_59 = 5'h1c == _io_outArbiterIO_1_bits_rsAddr_T_3 ? 3'h7 :
    _io_outArbiterIO_1_bits_rsAddr_T_57; // @[Mux.scala 81:58]
  wire [2:0] _io_outArbiterIO_1_bits_rsAddr_T_61 = 5'h1d == _io_outArbiterIO_1_bits_rsAddr_T_3 ? 3'h7 :
    _io_outArbiterIO_1_bits_rsAddr_T_59; // @[Mux.scala 81:58]
  wire [2:0] _io_outArbiterIO_1_bits_rsAddr_T_63 = 5'h1e == _io_outArbiterIO_1_bits_rsAddr_T_3 ? 3'h7 :
    _io_outArbiterIO_1_bits_rsAddr_T_61; // @[Mux.scala 81:58]
  wire [2:0] _io_outArbiterIO_1_bits_rsAddr_T_65 = 5'h1f == _io_outArbiterIO_1_bits_rsAddr_T_3 ? 3'h7 :
    _io_outArbiterIO_1_bits_rsAddr_T_63; // @[Mux.scala 81:58]
  wire [5:0] _GEN_525 = {{3'd0}, _io_outArbiterIO_1_bits_rsAddr_T_65}; // @[operandCollector.scala 97:134]
  wire [5:0] _io_outArbiterIO_1_bits_rsAddr_T_72 = _GEN_525 + _io_outArbiterIO_0_bits_rsAddr_T_70; // @[operandCollector.scala 97:134]
  wire [1:0] _GEN_94 = _io_outArbiterIO_0_bits_bankID_T ? io_control_bits_sel_alu2 : 2'h0; // @[operandCollector.scala 133:14 147:28 168:23]
  wire [1:0] rsTypeWire_1 = 2'h0 == state ? _GEN_94 : 2'h0; // @[operandCollector.scala 133:14 143:16]
  wire [4:0] _regIdxWire_2_T_2 = io_control_bits_isvec ? io_control_bits_reg_idx3 : io_control_bits_reg_idx2; // @[operandCollector.scala 156:25]
  wire  _regIdxWire_2_T = io_control_bits_branch == 2'h3; // @[operandCollector.scala 154:48]
  wire [4:0] _regIdxWire_2_T_1 = io_control_bits_branch == 2'h3 ? io_control_bits_reg_idx1 : io_control_bits_reg_idx3; // @[operandCollector.scala 154:25]
  wire [4:0] _regIdxWire_2_T_4 = 2'h1 == io_control_bits_sel_alu3 ? io_control_bits_reg_idx3 : _regIdxWire_2_T_1; // @[Mux.scala 81:58]
  wire [4:0] _regIdxWire_2_T_6 = 2'h3 == io_control_bits_sel_alu3 ? _regIdxWire_2_T_2 : _regIdxWire_2_T_4; // @[Mux.scala 81:58]
  wire [4:0] _regIdxWire_2_T_8 = 2'h2 == io_control_bits_sel_alu3 ? io_control_bits_reg_idx3 : _regIdxWire_2_T_6; // @[Mux.scala 81:58]
  wire [4:0] _GEN_79 = _io_outArbiterIO_0_bits_bankID_T ? _regIdxWire_2_T_8 : 5'h0; // @[operandCollector.scala 132:14 147:28 152:23]
  wire [4:0] regIdxWire_2 = 2'h0 == state ? _GEN_79 : 5'h0; // @[operandCollector.scala 132:14 143:16]
  wire [4:0] _io_outArbiterIO_2_bits_bankID_T_4 = _GEN_512 + regIdxWire_2; // @[operandCollector.scala 96:109]
  wire [4:0] _io_outArbiterIO_2_bits_bankID_T_6 = _GEN_513 + regIdx_2; // @[operandCollector.scala 96:139]
  wire [4:0] _io_outArbiterIO_2_bits_bankID_T_7 = _io_outArbiterIO_0_bits_bankID_T & state == 2'h0 ?
    _io_outArbiterIO_2_bits_bankID_T_4 : _io_outArbiterIO_2_bits_bankID_T_6; // @[operandCollector.scala 96:54]
  wire [1:0] _io_outArbiterIO_2_bits_bankID_T_11 = 5'h2 == _io_outArbiterIO_2_bits_bankID_T_7 ? 2'h2 : {{1'd0}, 5'h1 ==
    _io_outArbiterIO_2_bits_bankID_T_7}; // @[Mux.scala 81:58]
  wire [1:0] _io_outArbiterIO_2_bits_bankID_T_13 = 5'h3 == _io_outArbiterIO_2_bits_bankID_T_7 ? 2'h3 :
    _io_outArbiterIO_2_bits_bankID_T_11; // @[Mux.scala 81:58]
  wire [1:0] _io_outArbiterIO_2_bits_bankID_T_15 = 5'h4 == _io_outArbiterIO_2_bits_bankID_T_7 ? 2'h0 :
    _io_outArbiterIO_2_bits_bankID_T_13; // @[Mux.scala 81:58]
  wire [1:0] _io_outArbiterIO_2_bits_bankID_T_17 = 5'h5 == _io_outArbiterIO_2_bits_bankID_T_7 ? 2'h1 :
    _io_outArbiterIO_2_bits_bankID_T_15; // @[Mux.scala 81:58]
  wire [1:0] _io_outArbiterIO_2_bits_bankID_T_19 = 5'h6 == _io_outArbiterIO_2_bits_bankID_T_7 ? 2'h2 :
    _io_outArbiterIO_2_bits_bankID_T_17; // @[Mux.scala 81:58]
  wire [1:0] _io_outArbiterIO_2_bits_bankID_T_21 = 5'h7 == _io_outArbiterIO_2_bits_bankID_T_7 ? 2'h3 :
    _io_outArbiterIO_2_bits_bankID_T_19; // @[Mux.scala 81:58]
  wire [1:0] _io_outArbiterIO_2_bits_bankID_T_23 = 5'h8 == _io_outArbiterIO_2_bits_bankID_T_7 ? 2'h0 :
    _io_outArbiterIO_2_bits_bankID_T_21; // @[Mux.scala 81:58]
  wire [1:0] _io_outArbiterIO_2_bits_bankID_T_25 = 5'h9 == _io_outArbiterIO_2_bits_bankID_T_7 ? 2'h1 :
    _io_outArbiterIO_2_bits_bankID_T_23; // @[Mux.scala 81:58]
  wire [1:0] _io_outArbiterIO_2_bits_bankID_T_27 = 5'ha == _io_outArbiterIO_2_bits_bankID_T_7 ? 2'h2 :
    _io_outArbiterIO_2_bits_bankID_T_25; // @[Mux.scala 81:58]
  wire [1:0] _io_outArbiterIO_2_bits_bankID_T_29 = 5'hb == _io_outArbiterIO_2_bits_bankID_T_7 ? 2'h3 :
    _io_outArbiterIO_2_bits_bankID_T_27; // @[Mux.scala 81:58]
  wire [1:0] _io_outArbiterIO_2_bits_bankID_T_31 = 5'hc == _io_outArbiterIO_2_bits_bankID_T_7 ? 2'h0 :
    _io_outArbiterIO_2_bits_bankID_T_29; // @[Mux.scala 81:58]
  wire [1:0] _io_outArbiterIO_2_bits_bankID_T_33 = 5'hd == _io_outArbiterIO_2_bits_bankID_T_7 ? 2'h1 :
    _io_outArbiterIO_2_bits_bankID_T_31; // @[Mux.scala 81:58]
  wire [1:0] _io_outArbiterIO_2_bits_bankID_T_35 = 5'he == _io_outArbiterIO_2_bits_bankID_T_7 ? 2'h2 :
    _io_outArbiterIO_2_bits_bankID_T_33; // @[Mux.scala 81:58]
  wire [1:0] _io_outArbiterIO_2_bits_bankID_T_37 = 5'hf == _io_outArbiterIO_2_bits_bankID_T_7 ? 2'h3 :
    _io_outArbiterIO_2_bits_bankID_T_35; // @[Mux.scala 81:58]
  wire [1:0] _io_outArbiterIO_2_bits_bankID_T_39 = 5'h10 == _io_outArbiterIO_2_bits_bankID_T_7 ? 2'h0 :
    _io_outArbiterIO_2_bits_bankID_T_37; // @[Mux.scala 81:58]
  wire [1:0] _io_outArbiterIO_2_bits_bankID_T_41 = 5'h11 == _io_outArbiterIO_2_bits_bankID_T_7 ? 2'h1 :
    _io_outArbiterIO_2_bits_bankID_T_39; // @[Mux.scala 81:58]
  wire [1:0] _io_outArbiterIO_2_bits_bankID_T_43 = 5'h12 == _io_outArbiterIO_2_bits_bankID_T_7 ? 2'h2 :
    _io_outArbiterIO_2_bits_bankID_T_41; // @[Mux.scala 81:58]
  wire [1:0] _io_outArbiterIO_2_bits_bankID_T_45 = 5'h13 == _io_outArbiterIO_2_bits_bankID_T_7 ? 2'h3 :
    _io_outArbiterIO_2_bits_bankID_T_43; // @[Mux.scala 81:58]
  wire [1:0] _io_outArbiterIO_2_bits_bankID_T_47 = 5'h14 == _io_outArbiterIO_2_bits_bankID_T_7 ? 2'h0 :
    _io_outArbiterIO_2_bits_bankID_T_45; // @[Mux.scala 81:58]
  wire [1:0] _io_outArbiterIO_2_bits_bankID_T_49 = 5'h15 == _io_outArbiterIO_2_bits_bankID_T_7 ? 2'h1 :
    _io_outArbiterIO_2_bits_bankID_T_47; // @[Mux.scala 81:58]
  wire [1:0] _io_outArbiterIO_2_bits_bankID_T_51 = 5'h16 == _io_outArbiterIO_2_bits_bankID_T_7 ? 2'h2 :
    _io_outArbiterIO_2_bits_bankID_T_49; // @[Mux.scala 81:58]
  wire [1:0] _io_outArbiterIO_2_bits_bankID_T_53 = 5'h17 == _io_outArbiterIO_2_bits_bankID_T_7 ? 2'h3 :
    _io_outArbiterIO_2_bits_bankID_T_51; // @[Mux.scala 81:58]
  wire [1:0] _io_outArbiterIO_2_bits_bankID_T_55 = 5'h18 == _io_outArbiterIO_2_bits_bankID_T_7 ? 2'h0 :
    _io_outArbiterIO_2_bits_bankID_T_53; // @[Mux.scala 81:58]
  wire [1:0] _io_outArbiterIO_2_bits_bankID_T_57 = 5'h19 == _io_outArbiterIO_2_bits_bankID_T_7 ? 2'h1 :
    _io_outArbiterIO_2_bits_bankID_T_55; // @[Mux.scala 81:58]
  wire [1:0] _io_outArbiterIO_2_bits_bankID_T_59 = 5'h1a == _io_outArbiterIO_2_bits_bankID_T_7 ? 2'h2 :
    _io_outArbiterIO_2_bits_bankID_T_57; // @[Mux.scala 81:58]
  wire [1:0] _io_outArbiterIO_2_bits_bankID_T_61 = 5'h1b == _io_outArbiterIO_2_bits_bankID_T_7 ? 2'h3 :
    _io_outArbiterIO_2_bits_bankID_T_59; // @[Mux.scala 81:58]
  wire [1:0] _io_outArbiterIO_2_bits_bankID_T_63 = 5'h1c == _io_outArbiterIO_2_bits_bankID_T_7 ? 2'h0 :
    _io_outArbiterIO_2_bits_bankID_T_61; // @[Mux.scala 81:58]
  wire [1:0] _io_outArbiterIO_2_bits_bankID_T_65 = 5'h1d == _io_outArbiterIO_2_bits_bankID_T_7 ? 2'h1 :
    _io_outArbiterIO_2_bits_bankID_T_63; // @[Mux.scala 81:58]
  wire [1:0] _io_outArbiterIO_2_bits_bankID_T_67 = 5'h1e == _io_outArbiterIO_2_bits_bankID_T_7 ? 2'h2 :
    _io_outArbiterIO_2_bits_bankID_T_65; // @[Mux.scala 81:58]
  wire [1:0] _io_outArbiterIO_2_bits_bankID_T_69 = 5'h1f == _io_outArbiterIO_2_bits_bankID_T_7 ? 2'h3 :
    _io_outArbiterIO_2_bits_bankID_T_67; // @[Mux.scala 81:58]
  wire [5:0] _GEN_528 = {{1'd0}, _io_outArbiterIO_2_bits_bankID_T_7}; // @[Mux.scala 81:61]
  wire [1:0] _io_outArbiterIO_2_bits_bankID_T_71 = 6'h20 == _GEN_528 ? 2'h0 : _io_outArbiterIO_2_bits_bankID_T_69; // @[Mux.scala 81:58]
  wire [1:0] _io_outArbiterIO_2_bits_bankID_T_73 = 6'h21 == _GEN_528 ? 2'h1 : _io_outArbiterIO_2_bits_bankID_T_71; // @[Mux.scala 81:58]
  wire [1:0] _io_outArbiterIO_2_bits_bankID_T_75 = 6'h22 == _GEN_528 ? 2'h2 : _io_outArbiterIO_2_bits_bankID_T_73; // @[Mux.scala 81:58]
  wire [4:0] _io_outArbiterIO_2_bits_rsAddr_T_3 = _io_outArbiterIO_0_bits_bankID_T_2 ? regIdxWire_2 : regIdx_2; // @[operandCollector.scala 97:54]
  wire  _io_outArbiterIO_2_bits_rsAddr_T_17 = 5'h7 == _io_outArbiterIO_2_bits_rsAddr_T_3 | (5'h6 ==
    _io_outArbiterIO_2_bits_rsAddr_T_3 | (5'h5 == _io_outArbiterIO_2_bits_rsAddr_T_3 | 5'h4 ==
    _io_outArbiterIO_2_bits_rsAddr_T_3)); // @[Mux.scala 81:58]
  wire [1:0] _io_outArbiterIO_2_bits_rsAddr_T_19 = 5'h8 == _io_outArbiterIO_2_bits_rsAddr_T_3 ? 2'h2 : {{1'd0},
    _io_outArbiterIO_2_bits_rsAddr_T_17}; // @[Mux.scala 81:58]
  wire [1:0] _io_outArbiterIO_2_bits_rsAddr_T_21 = 5'h9 == _io_outArbiterIO_2_bits_rsAddr_T_3 ? 2'h2 :
    _io_outArbiterIO_2_bits_rsAddr_T_19; // @[Mux.scala 81:58]
  wire [1:0] _io_outArbiterIO_2_bits_rsAddr_T_23 = 5'ha == _io_outArbiterIO_2_bits_rsAddr_T_3 ? 2'h2 :
    _io_outArbiterIO_2_bits_rsAddr_T_21; // @[Mux.scala 81:58]
  wire [1:0] _io_outArbiterIO_2_bits_rsAddr_T_25 = 5'hb == _io_outArbiterIO_2_bits_rsAddr_T_3 ? 2'h2 :
    _io_outArbiterIO_2_bits_rsAddr_T_23; // @[Mux.scala 81:58]
  wire [1:0] _io_outArbiterIO_2_bits_rsAddr_T_27 = 5'hc == _io_outArbiterIO_2_bits_rsAddr_T_3 ? 2'h3 :
    _io_outArbiterIO_2_bits_rsAddr_T_25; // @[Mux.scala 81:58]
  wire [1:0] _io_outArbiterIO_2_bits_rsAddr_T_29 = 5'hd == _io_outArbiterIO_2_bits_rsAddr_T_3 ? 2'h3 :
    _io_outArbiterIO_2_bits_rsAddr_T_27; // @[Mux.scala 81:58]
  wire [1:0] _io_outArbiterIO_2_bits_rsAddr_T_31 = 5'he == _io_outArbiterIO_2_bits_rsAddr_T_3 ? 2'h3 :
    _io_outArbiterIO_2_bits_rsAddr_T_29; // @[Mux.scala 81:58]
  wire [1:0] _io_outArbiterIO_2_bits_rsAddr_T_33 = 5'hf == _io_outArbiterIO_2_bits_rsAddr_T_3 ? 2'h3 :
    _io_outArbiterIO_2_bits_rsAddr_T_31; // @[Mux.scala 81:58]
  wire [2:0] _io_outArbiterIO_2_bits_rsAddr_T_35 = 5'h10 == _io_outArbiterIO_2_bits_rsAddr_T_3 ? 3'h4 : {{1'd0},
    _io_outArbiterIO_2_bits_rsAddr_T_33}; // @[Mux.scala 81:58]
  wire [2:0] _io_outArbiterIO_2_bits_rsAddr_T_37 = 5'h11 == _io_outArbiterIO_2_bits_rsAddr_T_3 ? 3'h4 :
    _io_outArbiterIO_2_bits_rsAddr_T_35; // @[Mux.scala 81:58]
  wire [2:0] _io_outArbiterIO_2_bits_rsAddr_T_39 = 5'h12 == _io_outArbiterIO_2_bits_rsAddr_T_3 ? 3'h4 :
    _io_outArbiterIO_2_bits_rsAddr_T_37; // @[Mux.scala 81:58]
  wire [2:0] _io_outArbiterIO_2_bits_rsAddr_T_41 = 5'h13 == _io_outArbiterIO_2_bits_rsAddr_T_3 ? 3'h4 :
    _io_outArbiterIO_2_bits_rsAddr_T_39; // @[Mux.scala 81:58]
  wire [2:0] _io_outArbiterIO_2_bits_rsAddr_T_43 = 5'h14 == _io_outArbiterIO_2_bits_rsAddr_T_3 ? 3'h5 :
    _io_outArbiterIO_2_bits_rsAddr_T_41; // @[Mux.scala 81:58]
  wire [2:0] _io_outArbiterIO_2_bits_rsAddr_T_45 = 5'h15 == _io_outArbiterIO_2_bits_rsAddr_T_3 ? 3'h5 :
    _io_outArbiterIO_2_bits_rsAddr_T_43; // @[Mux.scala 81:58]
  wire [2:0] _io_outArbiterIO_2_bits_rsAddr_T_47 = 5'h16 == _io_outArbiterIO_2_bits_rsAddr_T_3 ? 3'h5 :
    _io_outArbiterIO_2_bits_rsAddr_T_45; // @[Mux.scala 81:58]
  wire [2:0] _io_outArbiterIO_2_bits_rsAddr_T_49 = 5'h17 == _io_outArbiterIO_2_bits_rsAddr_T_3 ? 3'h5 :
    _io_outArbiterIO_2_bits_rsAddr_T_47; // @[Mux.scala 81:58]
  wire [2:0] _io_outArbiterIO_2_bits_rsAddr_T_51 = 5'h18 == _io_outArbiterIO_2_bits_rsAddr_T_3 ? 3'h6 :
    _io_outArbiterIO_2_bits_rsAddr_T_49; // @[Mux.scala 81:58]
  wire [2:0] _io_outArbiterIO_2_bits_rsAddr_T_53 = 5'h19 == _io_outArbiterIO_2_bits_rsAddr_T_3 ? 3'h6 :
    _io_outArbiterIO_2_bits_rsAddr_T_51; // @[Mux.scala 81:58]
  wire [2:0] _io_outArbiterIO_2_bits_rsAddr_T_55 = 5'h1a == _io_outArbiterIO_2_bits_rsAddr_T_3 ? 3'h6 :
    _io_outArbiterIO_2_bits_rsAddr_T_53; // @[Mux.scala 81:58]
  wire [2:0] _io_outArbiterIO_2_bits_rsAddr_T_57 = 5'h1b == _io_outArbiterIO_2_bits_rsAddr_T_3 ? 3'h6 :
    _io_outArbiterIO_2_bits_rsAddr_T_55; // @[Mux.scala 81:58]
  wire [2:0] _io_outArbiterIO_2_bits_rsAddr_T_59 = 5'h1c == _io_outArbiterIO_2_bits_rsAddr_T_3 ? 3'h7 :
    _io_outArbiterIO_2_bits_rsAddr_T_57; // @[Mux.scala 81:58]
  wire [2:0] _io_outArbiterIO_2_bits_rsAddr_T_61 = 5'h1d == _io_outArbiterIO_2_bits_rsAddr_T_3 ? 3'h7 :
    _io_outArbiterIO_2_bits_rsAddr_T_59; // @[Mux.scala 81:58]
  wire [2:0] _io_outArbiterIO_2_bits_rsAddr_T_63 = 5'h1e == _io_outArbiterIO_2_bits_rsAddr_T_3 ? 3'h7 :
    _io_outArbiterIO_2_bits_rsAddr_T_61; // @[Mux.scala 81:58]
  wire [2:0] _io_outArbiterIO_2_bits_rsAddr_T_65 = 5'h1f == _io_outArbiterIO_2_bits_rsAddr_T_3 ? 3'h7 :
    _io_outArbiterIO_2_bits_rsAddr_T_63; // @[Mux.scala 81:58]
  wire [5:0] _GEN_532 = {{3'd0}, _io_outArbiterIO_2_bits_rsAddr_T_65}; // @[operandCollector.scala 97:134]
  wire [5:0] _io_outArbiterIO_2_bits_rsAddr_T_72 = _GEN_532 + _io_outArbiterIO_0_bits_rsAddr_T_70; // @[operandCollector.scala 97:134]
  wire [1:0] _rsTypeWire_2_T_2 = io_control_bits_isvec ? 2'h2 : 2'h1; // @[operandCollector.scala 173:25]
  wire [1:0] _rsTypeWire_2_T_1 = _regIdxWire_2_T ? 2'h1 : 2'h3; // @[operandCollector.scala 171:25]
  wire [1:0] _rsTypeWire_2_T_4 = 2'h1 == io_control_bits_sel_alu3 ? 2'h2 : _rsTypeWire_2_T_1; // @[Mux.scala 81:58]
  wire [1:0] _rsTypeWire_2_T_6 = 2'h3 == io_control_bits_sel_alu3 ? _rsTypeWire_2_T_2 : _rsTypeWire_2_T_4; // @[Mux.scala 81:58]
  wire [1:0] _rsTypeWire_2_T_8 = 2'h2 == io_control_bits_sel_alu3 ? 2'h1 : _rsTypeWire_2_T_6; // @[Mux.scala 81:58]
  wire [1:0] _GEN_95 = _io_outArbiterIO_0_bits_bankID_T ? _rsTypeWire_2_T_8 : 2'h0; // @[operandCollector.scala 133:14 147:28 169:23]
  wire [1:0] rsTypeWire_2 = 2'h0 == state ? _GEN_95 : 2'h0; // @[operandCollector.scala 133:14 143:16]
  wire [5:0] _io_outArbiterIO_3_bits_bankID_T_3 = {{1'd0}, _GEN_512}; // @[operandCollector.scala 96:109]
  wire [5:0] _io_outArbiterIO_3_bits_bankID_T_5 = {{1'd0}, _GEN_513}; // @[operandCollector.scala 96:139]
  wire [4:0] _io_outArbiterIO_3_bits_bankID_T_7 = _io_outArbiterIO_0_bits_bankID_T & state == 2'h0 ?
    _io_outArbiterIO_3_bits_bankID_T_3[4:0] : _io_outArbiterIO_3_bits_bankID_T_5[4:0]; // @[operandCollector.scala 96:54]
  wire [1:0] _io_outArbiterIO_3_bits_bankID_T_11 = 5'h2 == _io_outArbiterIO_3_bits_bankID_T_7 ? 2'h2 : {{1'd0}, 5'h1 ==
    _io_outArbiterIO_3_bits_bankID_T_7}; // @[Mux.scala 81:58]
  wire [1:0] _io_outArbiterIO_3_bits_bankID_T_13 = 5'h3 == _io_outArbiterIO_3_bits_bankID_T_7 ? 2'h3 :
    _io_outArbiterIO_3_bits_bankID_T_11; // @[Mux.scala 81:58]
  wire [1:0] _io_outArbiterIO_3_bits_bankID_T_15 = 5'h4 == _io_outArbiterIO_3_bits_bankID_T_7 ? 2'h0 :
    _io_outArbiterIO_3_bits_bankID_T_13; // @[Mux.scala 81:58]
  wire [1:0] _io_outArbiterIO_3_bits_bankID_T_17 = 5'h5 == _io_outArbiterIO_3_bits_bankID_T_7 ? 2'h1 :
    _io_outArbiterIO_3_bits_bankID_T_15; // @[Mux.scala 81:58]
  wire [1:0] _io_outArbiterIO_3_bits_bankID_T_19 = 5'h6 == _io_outArbiterIO_3_bits_bankID_T_7 ? 2'h2 :
    _io_outArbiterIO_3_bits_bankID_T_17; // @[Mux.scala 81:58]
  wire [1:0] _io_outArbiterIO_3_bits_bankID_T_21 = 5'h7 == _io_outArbiterIO_3_bits_bankID_T_7 ? 2'h3 :
    _io_outArbiterIO_3_bits_bankID_T_19; // @[Mux.scala 81:58]
  wire [1:0] _io_outArbiterIO_3_bits_bankID_T_23 = 5'h8 == _io_outArbiterIO_3_bits_bankID_T_7 ? 2'h0 :
    _io_outArbiterIO_3_bits_bankID_T_21; // @[Mux.scala 81:58]
  wire [1:0] _io_outArbiterIO_3_bits_bankID_T_25 = 5'h9 == _io_outArbiterIO_3_bits_bankID_T_7 ? 2'h1 :
    _io_outArbiterIO_3_bits_bankID_T_23; // @[Mux.scala 81:58]
  wire [1:0] _io_outArbiterIO_3_bits_bankID_T_27 = 5'ha == _io_outArbiterIO_3_bits_bankID_T_7 ? 2'h2 :
    _io_outArbiterIO_3_bits_bankID_T_25; // @[Mux.scala 81:58]
  wire [1:0] _io_outArbiterIO_3_bits_bankID_T_29 = 5'hb == _io_outArbiterIO_3_bits_bankID_T_7 ? 2'h3 :
    _io_outArbiterIO_3_bits_bankID_T_27; // @[Mux.scala 81:58]
  wire [1:0] _io_outArbiterIO_3_bits_bankID_T_31 = 5'hc == _io_outArbiterIO_3_bits_bankID_T_7 ? 2'h0 :
    _io_outArbiterIO_3_bits_bankID_T_29; // @[Mux.scala 81:58]
  wire [1:0] _io_outArbiterIO_3_bits_bankID_T_33 = 5'hd == _io_outArbiterIO_3_bits_bankID_T_7 ? 2'h1 :
    _io_outArbiterIO_3_bits_bankID_T_31; // @[Mux.scala 81:58]
  wire [1:0] _io_outArbiterIO_3_bits_bankID_T_35 = 5'he == _io_outArbiterIO_3_bits_bankID_T_7 ? 2'h2 :
    _io_outArbiterIO_3_bits_bankID_T_33; // @[Mux.scala 81:58]
  wire [1:0] _io_outArbiterIO_3_bits_bankID_T_37 = 5'hf == _io_outArbiterIO_3_bits_bankID_T_7 ? 2'h3 :
    _io_outArbiterIO_3_bits_bankID_T_35; // @[Mux.scala 81:58]
  wire [1:0] _io_outArbiterIO_3_bits_bankID_T_39 = 5'h10 == _io_outArbiterIO_3_bits_bankID_T_7 ? 2'h0 :
    _io_outArbiterIO_3_bits_bankID_T_37; // @[Mux.scala 81:58]
  wire [1:0] _io_outArbiterIO_3_bits_bankID_T_41 = 5'h11 == _io_outArbiterIO_3_bits_bankID_T_7 ? 2'h1 :
    _io_outArbiterIO_3_bits_bankID_T_39; // @[Mux.scala 81:58]
  wire [1:0] _io_outArbiterIO_3_bits_bankID_T_43 = 5'h12 == _io_outArbiterIO_3_bits_bankID_T_7 ? 2'h2 :
    _io_outArbiterIO_3_bits_bankID_T_41; // @[Mux.scala 81:58]
  wire [1:0] _io_outArbiterIO_3_bits_bankID_T_45 = 5'h13 == _io_outArbiterIO_3_bits_bankID_T_7 ? 2'h3 :
    _io_outArbiterIO_3_bits_bankID_T_43; // @[Mux.scala 81:58]
  wire [1:0] _io_outArbiterIO_3_bits_bankID_T_47 = 5'h14 == _io_outArbiterIO_3_bits_bankID_T_7 ? 2'h0 :
    _io_outArbiterIO_3_bits_bankID_T_45; // @[Mux.scala 81:58]
  wire [1:0] _io_outArbiterIO_3_bits_bankID_T_49 = 5'h15 == _io_outArbiterIO_3_bits_bankID_T_7 ? 2'h1 :
    _io_outArbiterIO_3_bits_bankID_T_47; // @[Mux.scala 81:58]
  wire [1:0] _io_outArbiterIO_3_bits_bankID_T_51 = 5'h16 == _io_outArbiterIO_3_bits_bankID_T_7 ? 2'h2 :
    _io_outArbiterIO_3_bits_bankID_T_49; // @[Mux.scala 81:58]
  wire [1:0] _io_outArbiterIO_3_bits_bankID_T_53 = 5'h17 == _io_outArbiterIO_3_bits_bankID_T_7 ? 2'h3 :
    _io_outArbiterIO_3_bits_bankID_T_51; // @[Mux.scala 81:58]
  wire [1:0] _io_outArbiterIO_3_bits_bankID_T_55 = 5'h18 == _io_outArbiterIO_3_bits_bankID_T_7 ? 2'h0 :
    _io_outArbiterIO_3_bits_bankID_T_53; // @[Mux.scala 81:58]
  wire [1:0] _io_outArbiterIO_3_bits_bankID_T_57 = 5'h19 == _io_outArbiterIO_3_bits_bankID_T_7 ? 2'h1 :
    _io_outArbiterIO_3_bits_bankID_T_55; // @[Mux.scala 81:58]
  wire [1:0] _io_outArbiterIO_3_bits_bankID_T_59 = 5'h1a == _io_outArbiterIO_3_bits_bankID_T_7 ? 2'h2 :
    _io_outArbiterIO_3_bits_bankID_T_57; // @[Mux.scala 81:58]
  wire [1:0] _io_outArbiterIO_3_bits_bankID_T_61 = 5'h1b == _io_outArbiterIO_3_bits_bankID_T_7 ? 2'h3 :
    _io_outArbiterIO_3_bits_bankID_T_59; // @[Mux.scala 81:58]
  wire [1:0] _io_outArbiterIO_3_bits_bankID_T_63 = 5'h1c == _io_outArbiterIO_3_bits_bankID_T_7 ? 2'h0 :
    _io_outArbiterIO_3_bits_bankID_T_61; // @[Mux.scala 81:58]
  wire [1:0] _io_outArbiterIO_3_bits_bankID_T_65 = 5'h1d == _io_outArbiterIO_3_bits_bankID_T_7 ? 2'h1 :
    _io_outArbiterIO_3_bits_bankID_T_63; // @[Mux.scala 81:58]
  wire [1:0] _io_outArbiterIO_3_bits_bankID_T_67 = 5'h1e == _io_outArbiterIO_3_bits_bankID_T_7 ? 2'h2 :
    _io_outArbiterIO_3_bits_bankID_T_65; // @[Mux.scala 81:58]
  wire [1:0] _io_outArbiterIO_3_bits_bankID_T_69 = 5'h1f == _io_outArbiterIO_3_bits_bankID_T_7 ? 2'h3 :
    _io_outArbiterIO_3_bits_bankID_T_67; // @[Mux.scala 81:58]
  wire [5:0] _GEN_535 = {{1'd0}, _io_outArbiterIO_3_bits_bankID_T_7}; // @[Mux.scala 81:61]
  wire [1:0] _io_outArbiterIO_3_bits_bankID_T_71 = 6'h20 == _GEN_535 ? 2'h0 : _io_outArbiterIO_3_bits_bankID_T_69; // @[Mux.scala 81:58]
  wire [1:0] _io_outArbiterIO_3_bits_bankID_T_73 = 6'h21 == _GEN_535 ? 2'h1 : _io_outArbiterIO_3_bits_bankID_T_71; // @[Mux.scala 81:58]
  wire [1:0] _io_outArbiterIO_3_bits_bankID_T_75 = 6'h22 == _GEN_535 ? 2'h2 : _io_outArbiterIO_3_bits_bankID_T_73; // @[Mux.scala 81:58]
  wire [6:0] _io_outArbiterIO_3_bits_rsAddr_T_71 = {{1'd0}, _io_outArbiterIO_0_bits_rsAddr_T_70}; // @[operandCollector.scala 97:134]
  wire  _io_bankIn_0_ready_T = state == 2'h1; // @[operandCollector.scala 103:34]
  wire  _io_bankIn_0_ready_T_1 = ~ready_0; // @[operandCollector.scala 103:57]
  wire  _io_bankIn_1_ready_T_1 = ~ready_1; // @[operandCollector.scala 103:57]
  wire  _io_bankIn_2_ready_T_1 = ~ready_2; // @[operandCollector.scala 103:57]
  wire  _io_bankIn_3_ready_T_1 = ~ready_3; // @[operandCollector.scala 103:57]
  wire  _T_15 = io_control_bits_sel_alu1 == 2'h0; // @[operandCollector.scala 187:44]
  wire  _GEN_17 = io_control_bits_sel_alu1 == 2'h3 | _T_15; // @[operandCollector.scala 183:92 186:24]
  wire  _GEN_105 = _io_outArbiterIO_0_bits_bankID_T & _GEN_17; // @[operandCollector.scala 134:13 147:28]
  wire  readyWire_0 = 2'h0 == state & _GEN_105; // @[operandCollector.scala 134:13 143:16]
  wire  _io_outArbiterIO_0_valid_T_2 = _io_outArbiterIO_0_bits_bankID_T & ~readyWire_0; // @[operandCollector.scala 108:40]
  wire  _io_outArbiterIO_0_valid_T_5 = valid_0 & _io_bankIn_0_ready_T_1; // @[operandCollector.scala 109:45]
  wire  _io_outArbiterIO_0_valid_T_8 = 2'h1 == state; // @[Mux.scala 81:61]
  wire  _T_17 = io_control_bits_sel_alu2 == 2'h0; // @[operandCollector.scala 197:44]
  wire  _GEN_29 = io_control_bits_sel_alu2 == 2'h3 | _T_17; // @[operandCollector.scala 193:89 196:24]
  wire  _GEN_110 = _io_outArbiterIO_0_bits_bankID_T & _GEN_29; // @[operandCollector.scala 134:13 147:28]
  wire  readyWire_1 = 2'h0 == state & _GEN_110; // @[operandCollector.scala 134:13 143:16]
  wire  _io_outArbiterIO_1_valid_T_2 = _io_outArbiterIO_0_bits_bankID_T & ~readyWire_1; // @[operandCollector.scala 108:40]
  wire  _io_outArbiterIO_1_valid_T_5 = valid_1 & _io_bankIn_1_ready_T_1; // @[operandCollector.scala 109:45]
  wire  _T_20 = io_control_bits_sel_alu3 == 2'h0 & io_control_bits_branch != 2'h3; // @[operandCollector.scala 203:88]
  wire  _GEN_115 = _io_outArbiterIO_0_bits_bankID_T & _T_20; // @[operandCollector.scala 134:13 147:28]
  wire  readyWire_2 = 2'h0 == state & _GEN_115; // @[operandCollector.scala 134:13 143:16]
  wire  _io_outArbiterIO_2_valid_T_2 = _io_outArbiterIO_0_bits_bankID_T & ~readyWire_2; // @[operandCollector.scala 108:40]
  wire  _io_outArbiterIO_2_valid_T_5 = valid_2 & _io_bankIn_2_ready_T_1; // @[operandCollector.scala 109:45]
  wire  _T_21 = ~io_control_bits_mask; // @[operandCollector.scala 208:14]
  wire  _GEN_120 = _io_outArbiterIO_0_bits_bankID_T & _T_21; // @[operandCollector.scala 134:13 147:28]
  wire  readyWire_3 = 2'h0 == state & _GEN_120; // @[operandCollector.scala 134:13 143:16]
  wire  _io_outArbiterIO_3_valid_T_2 = _io_outArbiterIO_0_bits_bankID_T & ~readyWire_3; // @[operandCollector.scala 108:40]
  wire  _io_outArbiterIO_3_valid_T_5 = valid_3 & _io_bankIn_3_ready_T_1; // @[operandCollector.scala 109:45]
  wire  _io_issue_valid_T = state == 2'h2; // @[operandCollector.scala 113:26]
  wire [3:0] _io_control_ready_T_1 = {valid_3,valid_2,valid_1,valid_0}; // @[operandCollector.scala 114:49]
  wire  _io_control_ready_T_3 = ~(|_io_control_ready_T_1); // @[operandCollector.scala 114:42]
  wire [3:0] _T_8 = {ready_3,ready_2,ready_1,ready_0}; // @[operandCollector.scala 121:33]
  wire  _T_11 = io_issue_ready & io_issue_valid; // @[Decoupled.scala 52:35]
  wire [1:0] _GEN_2 = _T_11 ? 2'h0 : 2'h2; // @[operandCollector.scala 127:24 128:13 129:23]
  wire [31:0] _imm_io_inst_T_1 = _T_12 ? io_control_bits_inst : 32'h0; // @[Mux.scala 81:58]
  wire [2:0] _imm_io_sel_T_1 = _T_12 ? io_control_bits_sel_imm : 3'h0; // @[Mux.scala 81:58]
  wire [31:0] _GEN_6 = io_control_bits_sel_alu1 == 2'h0 ? io_control_bits_pc : rsReg_0_0; // @[operandCollector.scala 187:53 188:29 69:22]
  wire [31:0] _GEN_7 = io_control_bits_sel_alu1 == 2'h0 ? io_control_bits_pc : rsReg_0_1; // @[operandCollector.scala 187:53 188:29 69:22]
  wire [31:0] _GEN_8 = io_control_bits_sel_alu1 == 2'h0 ? io_control_bits_pc : rsReg_0_2; // @[operandCollector.scala 187:53 188:29 69:22]
  wire [31:0] _GEN_9 = io_control_bits_sel_alu1 == 2'h0 ? io_control_bits_pc : rsReg_0_3; // @[operandCollector.scala 187:53 188:29 69:22]
  wire [31:0] _GEN_18 = io_control_bits_sel_alu2 == 2'h0 ? 32'h4 : rsReg_1_0; // @[operandCollector.scala 197:55 198:30 69:22]
  wire [31:0] _GEN_19 = io_control_bits_sel_alu2 == 2'h0 ? 32'h4 : rsReg_1_1; // @[operandCollector.scala 197:55 198:30 69:22]
  wire [31:0] _GEN_20 = io_control_bits_sel_alu2 == 2'h0 ? 32'h4 : rsReg_1_2; // @[operandCollector.scala 197:55 198:30 69:22]
  wire [31:0] _GEN_21 = io_control_bits_sel_alu2 == 2'h0 ? 32'h4 : rsReg_1_3; // @[operandCollector.scala 197:55 198:30 69:22]
  wire [31:0] _rsReg_2_0_T_1 = imm_io_out + io_control_bits_pc; // @[operandCollector.scala 204:42]
  wire  _GEN_36 = ~io_control_bits_mask | mask_0; // @[operandCollector.scala 208:36 210:20 70:17]
  wire  _GEN_85 = _io_outArbiterIO_0_bits_bankID_T | valid_0; // @[operandCollector.scala 147:28 164:24 67:18]
  wire  _GEN_86 = _io_outArbiterIO_0_bits_bankID_T | valid_1; // @[operandCollector.scala 147:28 164:24 67:18]
  wire  _GEN_87 = _io_outArbiterIO_0_bits_bankID_T | valid_2; // @[operandCollector.scala 147:28 164:24 67:18]
  wire  _GEN_88 = _io_outArbiterIO_0_bits_bankID_T | valid_3; // @[operandCollector.scala 147:28 164:24 67:18]
  wire  _T_23 = io_bankIn_0_ready & io_bankIn_0_valid; // @[Decoupled.scala 52:35]
  wire [31:0] _T_26_0 = 2'h1 == rsType_0 ? io_bankIn_0_bits_data_0 : 32'h0; // @[Mux.scala 81:58]
  wire [31:0] _T_28_0 = 2'h2 == rsType_0 ? io_bankIn_0_bits_data_0 : _T_26_0; // @[Mux.scala 81:58]
  wire [31:0] _T_28_1 = 2'h2 == rsType_0 ? io_bankIn_0_bits_data_1 : _T_26_0; // @[Mux.scala 81:58]
  wire [31:0] _T_28_2 = 2'h2 == rsType_0 ? io_bankIn_0_bits_data_2 : _T_26_0; // @[Mux.scala 81:58]
  wire [31:0] _T_28_3 = 2'h2 == rsType_0 ? io_bankIn_0_bits_data_3 : _T_26_0; // @[Mux.scala 81:58]
  wire [31:0] _T_31_0 = 2'h1 == rsType_1 ? io_bankIn_0_bits_data_0 : 32'h0; // @[Mux.scala 81:58]
  wire [31:0] _T_33_0 = 2'h2 == rsType_1 ? io_bankIn_0_bits_data_0 : _T_31_0; // @[Mux.scala 81:58]
  wire [31:0] _T_33_1 = 2'h2 == rsType_1 ? io_bankIn_0_bits_data_1 : _T_31_0; // @[Mux.scala 81:58]
  wire [31:0] _T_33_2 = 2'h2 == rsType_1 ? io_bankIn_0_bits_data_2 : _T_31_0; // @[Mux.scala 81:58]
  wire [31:0] _T_33_3 = 2'h2 == rsType_1 ? io_bankIn_0_bits_data_3 : _T_31_0; // @[Mux.scala 81:58]
  wire [31:0] _T_36 = imm_io_out + io_bankIn_0_bits_data_0; // @[operandCollector.scala 241:66]
  wire [31:0] _T_43_1 = controlReg_isvec ? io_bankIn_0_bits_data_1 : io_bankIn_0_bits_data_0; // @[operandCollector.scala 243:29]
  wire [31:0] _T_43_2 = controlReg_isvec ? io_bankIn_0_bits_data_2 : io_bankIn_0_bits_data_0; // @[operandCollector.scala 243:29]
  wire [31:0] _T_43_3 = controlReg_isvec ? io_bankIn_0_bits_data_3 : io_bankIn_0_bits_data_0; // @[operandCollector.scala 243:29]
  wire [31:0] _T_45_0 = 2'h1 == controlReg_sel_alu3 ? io_bankIn_0_bits_data_0 : _T_36; // @[Mux.scala 81:58]
  wire [31:0] _T_45_1 = 2'h1 == controlReg_sel_alu3 ? io_bankIn_0_bits_data_1 : _T_36; // @[Mux.scala 81:58]
  wire [31:0] _T_45_2 = 2'h1 == controlReg_sel_alu3 ? io_bankIn_0_bits_data_2 : _T_36; // @[Mux.scala 81:58]
  wire [31:0] _T_45_3 = 2'h1 == controlReg_sel_alu3 ? io_bankIn_0_bits_data_3 : _T_36; // @[Mux.scala 81:58]
  wire [31:0] _T_47_0 = 2'h3 == controlReg_sel_alu3 ? io_bankIn_0_bits_data_0 : _T_45_0; // @[Mux.scala 81:58]
  wire [31:0] _T_47_1 = 2'h3 == controlReg_sel_alu3 ? _T_43_1 : _T_45_1; // @[Mux.scala 81:58]
  wire [31:0] _T_47_2 = 2'h3 == controlReg_sel_alu3 ? _T_43_2 : _T_45_2; // @[Mux.scala 81:58]
  wire [31:0] _T_47_3 = 2'h3 == controlReg_sel_alu3 ? _T_43_3 : _T_45_3; // @[Mux.scala 81:58]
  wire [31:0] _T_49_0 = 2'h2 == controlReg_sel_alu3 ? io_bankIn_0_bits_data_0 : _T_47_0; // @[Mux.scala 81:58]
  wire [31:0] _T_49_1 = 2'h2 == controlReg_sel_alu3 ? io_bankIn_0_bits_data_0 : _T_47_1; // @[Mux.scala 81:58]
  wire [31:0] _T_49_2 = 2'h2 == controlReg_sel_alu3 ? io_bankIn_0_bits_data_0 : _T_47_2; // @[Mux.scala 81:58]
  wire [31:0] _T_49_3 = 2'h2 == controlReg_sel_alu3 ? io_bankIn_0_bits_data_0 : _T_47_3; // @[Mux.scala 81:58]
  wire  _GEN_121 = io_bankIn_0_bits_regOrder == 2'h3 ? io_bankIn_0_bits_v0_0[0] : mask_0; // @[operandCollector.scala 248:58 250:23 70:17]
  wire  _GEN_122 = io_bankIn_0_bits_regOrder == 2'h3 ? io_bankIn_0_bits_v0_0[1] : mask_1; // @[operandCollector.scala 248:58 250:23 70:17]
  wire  _GEN_123 = io_bankIn_0_bits_regOrder == 2'h3 ? io_bankIn_0_bits_v0_0[2] : mask_2; // @[operandCollector.scala 248:58 250:23 70:17]
  wire  _GEN_124 = io_bankIn_0_bits_regOrder == 2'h3 ? io_bankIn_0_bits_v0_0[3] : mask_3; // @[operandCollector.scala 248:58 250:23 70:17]
  wire  _GEN_125 = io_bankIn_0_bits_regOrder == 2'h3 | ready_3; // @[operandCollector.scala 248:58 252:22 66:18]
  wire [31:0] _GEN_126 = io_bankIn_0_bits_regOrder == 2'h2 ? _T_49_0 : rsReg_2_0; // @[operandCollector.scala 239:58 240:22 69:22]
  wire [31:0] _GEN_127 = io_bankIn_0_bits_regOrder == 2'h2 ? _T_49_1 : rsReg_2_1; // @[operandCollector.scala 239:58 240:22 69:22]
  wire [31:0] _GEN_128 = io_bankIn_0_bits_regOrder == 2'h2 ? _T_49_2 : rsReg_2_2; // @[operandCollector.scala 239:58 240:22 69:22]
  wire [31:0] _GEN_129 = io_bankIn_0_bits_regOrder == 2'h2 ? _T_49_3 : rsReg_2_3; // @[operandCollector.scala 239:58 240:22 69:22]
  wire  _GEN_130 = io_bankIn_0_bits_regOrder == 2'h2 | ready_2; // @[operandCollector.scala 239:58 247:22 66:18]
  wire  _GEN_131 = io_bankIn_0_bits_regOrder == 2'h2 ? mask_0 : _GEN_121; // @[operandCollector.scala 239:58 70:17]
  wire  _GEN_132 = io_bankIn_0_bits_regOrder == 2'h2 ? mask_1 : _GEN_122; // @[operandCollector.scala 239:58 70:17]
  wire  _GEN_133 = io_bankIn_0_bits_regOrder == 2'h2 ? mask_2 : _GEN_123; // @[operandCollector.scala 239:58 70:17]
  wire  _GEN_134 = io_bankIn_0_bits_regOrder == 2'h2 ? mask_3 : _GEN_124; // @[operandCollector.scala 239:58 70:17]
  wire  _GEN_135 = io_bankIn_0_bits_regOrder == 2'h2 ? ready_3 : _GEN_125; // @[operandCollector.scala 239:58 66:18]
  wire [31:0] _GEN_136 = io_bankIn_0_bits_regOrder == 2'h1 ? _T_33_0 : rsReg_1_0; // @[operandCollector.scala 231:58 232:22 69:22]
  wire [31:0] _GEN_137 = io_bankIn_0_bits_regOrder == 2'h1 ? _T_33_1 : rsReg_1_1; // @[operandCollector.scala 231:58 232:22 69:22]
  wire [31:0] _GEN_138 = io_bankIn_0_bits_regOrder == 2'h1 ? _T_33_2 : rsReg_1_2; // @[operandCollector.scala 231:58 232:22 69:22]
  wire [31:0] _GEN_139 = io_bankIn_0_bits_regOrder == 2'h1 ? _T_33_3 : rsReg_1_3; // @[operandCollector.scala 231:58 232:22 69:22]
  wire  _GEN_140 = io_bankIn_0_bits_regOrder == 2'h1 | ready_1; // @[operandCollector.scala 231:58 238:22 66:18]
  wire [31:0] _GEN_141 = io_bankIn_0_bits_regOrder == 2'h1 ? rsReg_2_0 : _GEN_126; // @[operandCollector.scala 231:58 69:22]
  wire [31:0] _GEN_142 = io_bankIn_0_bits_regOrder == 2'h1 ? rsReg_2_1 : _GEN_127; // @[operandCollector.scala 231:58 69:22]
  wire [31:0] _GEN_143 = io_bankIn_0_bits_regOrder == 2'h1 ? rsReg_2_2 : _GEN_128; // @[operandCollector.scala 231:58 69:22]
  wire [31:0] _GEN_144 = io_bankIn_0_bits_regOrder == 2'h1 ? rsReg_2_3 : _GEN_129; // @[operandCollector.scala 231:58 69:22]
  wire  _GEN_145 = io_bankIn_0_bits_regOrder == 2'h1 ? ready_2 : _GEN_130; // @[operandCollector.scala 231:58 66:18]
  wire  _GEN_146 = io_bankIn_0_bits_regOrder == 2'h1 ? mask_0 : _GEN_131; // @[operandCollector.scala 231:58 70:17]
  wire  _GEN_147 = io_bankIn_0_bits_regOrder == 2'h1 ? mask_1 : _GEN_132; // @[operandCollector.scala 231:58 70:17]
  wire  _GEN_148 = io_bankIn_0_bits_regOrder == 2'h1 ? mask_2 : _GEN_133; // @[operandCollector.scala 231:58 70:17]
  wire  _GEN_149 = io_bankIn_0_bits_regOrder == 2'h1 ? mask_3 : _GEN_134; // @[operandCollector.scala 231:58 70:17]
  wire  _GEN_150 = io_bankIn_0_bits_regOrder == 2'h1 ? ready_3 : _GEN_135; // @[operandCollector.scala 231:58 66:18]
  wire [31:0] _GEN_151 = io_bankIn_0_bits_regOrder == 2'h0 ? _T_28_0 : rsReg_0_0; // @[operandCollector.scala 223:52 224:22 69:22]
  wire [31:0] _GEN_152 = io_bankIn_0_bits_regOrder == 2'h0 ? _T_28_1 : rsReg_0_1; // @[operandCollector.scala 223:52 224:22 69:22]
  wire [31:0] _GEN_153 = io_bankIn_0_bits_regOrder == 2'h0 ? _T_28_2 : rsReg_0_2; // @[operandCollector.scala 223:52 224:22 69:22]
  wire [31:0] _GEN_154 = io_bankIn_0_bits_regOrder == 2'h0 ? _T_28_3 : rsReg_0_3; // @[operandCollector.scala 223:52 224:22 69:22]
  wire  _GEN_155 = io_bankIn_0_bits_regOrder == 2'h0 | ready_0; // @[operandCollector.scala 223:52 230:22 66:18]
  wire [31:0] _GEN_156 = io_bankIn_0_bits_regOrder == 2'h0 ? rsReg_1_0 : _GEN_136; // @[operandCollector.scala 223:52 69:22]
  wire [31:0] _GEN_157 = io_bankIn_0_bits_regOrder == 2'h0 ? rsReg_1_1 : _GEN_137; // @[operandCollector.scala 223:52 69:22]
  wire [31:0] _GEN_158 = io_bankIn_0_bits_regOrder == 2'h0 ? rsReg_1_2 : _GEN_138; // @[operandCollector.scala 223:52 69:22]
  wire [31:0] _GEN_159 = io_bankIn_0_bits_regOrder == 2'h0 ? rsReg_1_3 : _GEN_139; // @[operandCollector.scala 223:52 69:22]
  wire  _GEN_160 = io_bankIn_0_bits_regOrder == 2'h0 ? ready_1 : _GEN_140; // @[operandCollector.scala 223:52 66:18]
  wire [31:0] _GEN_161 = io_bankIn_0_bits_regOrder == 2'h0 ? rsReg_2_0 : _GEN_141; // @[operandCollector.scala 223:52 69:22]
  wire [31:0] _GEN_162 = io_bankIn_0_bits_regOrder == 2'h0 ? rsReg_2_1 : _GEN_142; // @[operandCollector.scala 223:52 69:22]
  wire [31:0] _GEN_163 = io_bankIn_0_bits_regOrder == 2'h0 ? rsReg_2_2 : _GEN_143; // @[operandCollector.scala 223:52 69:22]
  wire [31:0] _GEN_164 = io_bankIn_0_bits_regOrder == 2'h0 ? rsReg_2_3 : _GEN_144; // @[operandCollector.scala 223:52 69:22]
  wire  _GEN_165 = io_bankIn_0_bits_regOrder == 2'h0 ? ready_2 : _GEN_145; // @[operandCollector.scala 223:52 66:18]
  wire  _GEN_166 = io_bankIn_0_bits_regOrder == 2'h0 ? mask_0 : _GEN_146; // @[operandCollector.scala 223:52 70:17]
  wire  _GEN_167 = io_bankIn_0_bits_regOrder == 2'h0 ? mask_1 : _GEN_147; // @[operandCollector.scala 223:52 70:17]
  wire  _GEN_168 = io_bankIn_0_bits_regOrder == 2'h0 ? mask_2 : _GEN_148; // @[operandCollector.scala 223:52 70:17]
  wire  _GEN_169 = io_bankIn_0_bits_regOrder == 2'h0 ? mask_3 : _GEN_149; // @[operandCollector.scala 223:52 70:17]
  wire  _GEN_170 = io_bankIn_0_bits_regOrder == 2'h0 ? ready_3 : _GEN_150; // @[operandCollector.scala 223:52 66:18]
  wire [31:0] _GEN_171 = _T_23 ? _GEN_151 : rsReg_0_0; // @[operandCollector.scala 222:33 69:22]
  wire [31:0] _GEN_172 = _T_23 ? _GEN_152 : rsReg_0_1; // @[operandCollector.scala 222:33 69:22]
  wire [31:0] _GEN_173 = _T_23 ? _GEN_153 : rsReg_0_2; // @[operandCollector.scala 222:33 69:22]
  wire [31:0] _GEN_174 = _T_23 ? _GEN_154 : rsReg_0_3; // @[operandCollector.scala 222:33 69:22]
  wire  _GEN_175 = _T_23 ? _GEN_155 : ready_0; // @[operandCollector.scala 222:33 66:18]
  wire [31:0] _GEN_176 = _T_23 ? _GEN_156 : rsReg_1_0; // @[operandCollector.scala 222:33 69:22]
  wire [31:0] _GEN_177 = _T_23 ? _GEN_157 : rsReg_1_1; // @[operandCollector.scala 222:33 69:22]
  wire [31:0] _GEN_178 = _T_23 ? _GEN_158 : rsReg_1_2; // @[operandCollector.scala 222:33 69:22]
  wire [31:0] _GEN_179 = _T_23 ? _GEN_159 : rsReg_1_3; // @[operandCollector.scala 222:33 69:22]
  wire  _GEN_180 = _T_23 ? _GEN_160 : ready_1; // @[operandCollector.scala 222:33 66:18]
  wire [31:0] _GEN_181 = _T_23 ? _GEN_161 : rsReg_2_0; // @[operandCollector.scala 222:33 69:22]
  wire [31:0] _GEN_182 = _T_23 ? _GEN_162 : rsReg_2_1; // @[operandCollector.scala 222:33 69:22]
  wire [31:0] _GEN_183 = _T_23 ? _GEN_163 : rsReg_2_2; // @[operandCollector.scala 222:33 69:22]
  wire [31:0] _GEN_184 = _T_23 ? _GEN_164 : rsReg_2_3; // @[operandCollector.scala 222:33 69:22]
  wire  _GEN_185 = _T_23 ? _GEN_165 : ready_2; // @[operandCollector.scala 222:33 66:18]
  wire  _GEN_186 = _T_23 ? _GEN_166 : mask_0; // @[operandCollector.scala 222:33 70:17]
  wire  _GEN_187 = _T_23 ? _GEN_167 : mask_1; // @[operandCollector.scala 222:33 70:17]
  wire  _GEN_188 = _T_23 ? _GEN_168 : mask_2; // @[operandCollector.scala 222:33 70:17]
  wire  _GEN_189 = _T_23 ? _GEN_169 : mask_3; // @[operandCollector.scala 222:33 70:17]
  wire  _GEN_190 = _T_23 ? _GEN_170 : ready_3; // @[operandCollector.scala 222:33 66:18]
  wire  _T_51 = io_bankIn_1_ready & io_bankIn_1_valid; // @[Decoupled.scala 52:35]
  wire [31:0] _T_54_0 = 2'h1 == rsType_0 ? io_bankIn_1_bits_data_0 : 32'h0; // @[Mux.scala 81:58]
  wire [31:0] _T_56_0 = 2'h2 == rsType_0 ? io_bankIn_1_bits_data_0 : _T_54_0; // @[Mux.scala 81:58]
  wire [31:0] _T_56_1 = 2'h2 == rsType_0 ? io_bankIn_1_bits_data_1 : _T_54_0; // @[Mux.scala 81:58]
  wire [31:0] _T_56_2 = 2'h2 == rsType_0 ? io_bankIn_1_bits_data_2 : _T_54_0; // @[Mux.scala 81:58]
  wire [31:0] _T_56_3 = 2'h2 == rsType_0 ? io_bankIn_1_bits_data_3 : _T_54_0; // @[Mux.scala 81:58]
  wire [31:0] _T_59_0 = 2'h1 == rsType_1 ? io_bankIn_1_bits_data_0 : 32'h0; // @[Mux.scala 81:58]
  wire [31:0] _T_61_0 = 2'h2 == rsType_1 ? io_bankIn_1_bits_data_0 : _T_59_0; // @[Mux.scala 81:58]
  wire [31:0] _T_61_1 = 2'h2 == rsType_1 ? io_bankIn_1_bits_data_1 : _T_59_0; // @[Mux.scala 81:58]
  wire [31:0] _T_61_2 = 2'h2 == rsType_1 ? io_bankIn_1_bits_data_2 : _T_59_0; // @[Mux.scala 81:58]
  wire [31:0] _T_61_3 = 2'h2 == rsType_1 ? io_bankIn_1_bits_data_3 : _T_59_0; // @[Mux.scala 81:58]
  wire [31:0] _T_64 = imm_io_out + io_bankIn_1_bits_data_0; // @[operandCollector.scala 241:66]
  wire [31:0] _T_71_1 = controlReg_isvec ? io_bankIn_1_bits_data_1 : io_bankIn_1_bits_data_0; // @[operandCollector.scala 243:29]
  wire [31:0] _T_71_2 = controlReg_isvec ? io_bankIn_1_bits_data_2 : io_bankIn_1_bits_data_0; // @[operandCollector.scala 243:29]
  wire [31:0] _T_71_3 = controlReg_isvec ? io_bankIn_1_bits_data_3 : io_bankIn_1_bits_data_0; // @[operandCollector.scala 243:29]
  wire [31:0] _T_73_0 = 2'h1 == controlReg_sel_alu3 ? io_bankIn_1_bits_data_0 : _T_64; // @[Mux.scala 81:58]
  wire [31:0] _T_73_1 = 2'h1 == controlReg_sel_alu3 ? io_bankIn_1_bits_data_1 : _T_64; // @[Mux.scala 81:58]
  wire [31:0] _T_73_2 = 2'h1 == controlReg_sel_alu3 ? io_bankIn_1_bits_data_2 : _T_64; // @[Mux.scala 81:58]
  wire [31:0] _T_73_3 = 2'h1 == controlReg_sel_alu3 ? io_bankIn_1_bits_data_3 : _T_64; // @[Mux.scala 81:58]
  wire [31:0] _T_75_0 = 2'h3 == controlReg_sel_alu3 ? io_bankIn_1_bits_data_0 : _T_73_0; // @[Mux.scala 81:58]
  wire [31:0] _T_75_1 = 2'h3 == controlReg_sel_alu3 ? _T_71_1 : _T_73_1; // @[Mux.scala 81:58]
  wire [31:0] _T_75_2 = 2'h3 == controlReg_sel_alu3 ? _T_71_2 : _T_73_2; // @[Mux.scala 81:58]
  wire [31:0] _T_75_3 = 2'h3 == controlReg_sel_alu3 ? _T_71_3 : _T_73_3; // @[Mux.scala 81:58]
  wire [31:0] _T_77_0 = 2'h2 == controlReg_sel_alu3 ? io_bankIn_1_bits_data_0 : _T_75_0; // @[Mux.scala 81:58]
  wire [31:0] _T_77_1 = 2'h2 == controlReg_sel_alu3 ? io_bankIn_1_bits_data_0 : _T_75_1; // @[Mux.scala 81:58]
  wire [31:0] _T_77_2 = 2'h2 == controlReg_sel_alu3 ? io_bankIn_1_bits_data_0 : _T_75_2; // @[Mux.scala 81:58]
  wire [31:0] _T_77_3 = 2'h2 == controlReg_sel_alu3 ? io_bankIn_1_bits_data_0 : _T_75_3; // @[Mux.scala 81:58]
  wire  _GEN_191 = io_bankIn_1_bits_regOrder == 2'h3 ? io_bankIn_1_bits_v0_0[0] : _GEN_186; // @[operandCollector.scala 248:58 250:23]
  wire  _GEN_192 = io_bankIn_1_bits_regOrder == 2'h3 ? io_bankIn_1_bits_v0_0[1] : _GEN_187; // @[operandCollector.scala 248:58 250:23]
  wire  _GEN_193 = io_bankIn_1_bits_regOrder == 2'h3 ? io_bankIn_1_bits_v0_0[2] : _GEN_188; // @[operandCollector.scala 248:58 250:23]
  wire  _GEN_194 = io_bankIn_1_bits_regOrder == 2'h3 ? io_bankIn_1_bits_v0_0[3] : _GEN_189; // @[operandCollector.scala 248:58 250:23]
  wire  _GEN_195 = io_bankIn_1_bits_regOrder == 2'h3 | _GEN_190; // @[operandCollector.scala 248:58 252:22]
  wire [31:0] _GEN_196 = io_bankIn_1_bits_regOrder == 2'h2 ? _T_77_0 : _GEN_181; // @[operandCollector.scala 239:58 240:22]
  wire [31:0] _GEN_197 = io_bankIn_1_bits_regOrder == 2'h2 ? _T_77_1 : _GEN_182; // @[operandCollector.scala 239:58 240:22]
  wire [31:0] _GEN_198 = io_bankIn_1_bits_regOrder == 2'h2 ? _T_77_2 : _GEN_183; // @[operandCollector.scala 239:58 240:22]
  wire [31:0] _GEN_199 = io_bankIn_1_bits_regOrder == 2'h2 ? _T_77_3 : _GEN_184; // @[operandCollector.scala 239:58 240:22]
  wire  _GEN_200 = io_bankIn_1_bits_regOrder == 2'h2 | _GEN_185; // @[operandCollector.scala 239:58 247:22]
  wire  _GEN_201 = io_bankIn_1_bits_regOrder == 2'h2 ? _GEN_186 : _GEN_191; // @[operandCollector.scala 239:58]
  wire  _GEN_202 = io_bankIn_1_bits_regOrder == 2'h2 ? _GEN_187 : _GEN_192; // @[operandCollector.scala 239:58]
  wire  _GEN_203 = io_bankIn_1_bits_regOrder == 2'h2 ? _GEN_188 : _GEN_193; // @[operandCollector.scala 239:58]
  wire  _GEN_204 = io_bankIn_1_bits_regOrder == 2'h2 ? _GEN_189 : _GEN_194; // @[operandCollector.scala 239:58]
  wire  _GEN_205 = io_bankIn_1_bits_regOrder == 2'h2 ? _GEN_190 : _GEN_195; // @[operandCollector.scala 239:58]
  wire [31:0] _GEN_206 = io_bankIn_1_bits_regOrder == 2'h1 ? _T_61_0 : _GEN_176; // @[operandCollector.scala 231:58 232:22]
  wire [31:0] _GEN_207 = io_bankIn_1_bits_regOrder == 2'h1 ? _T_61_1 : _GEN_177; // @[operandCollector.scala 231:58 232:22]
  wire [31:0] _GEN_208 = io_bankIn_1_bits_regOrder == 2'h1 ? _T_61_2 : _GEN_178; // @[operandCollector.scala 231:58 232:22]
  wire [31:0] _GEN_209 = io_bankIn_1_bits_regOrder == 2'h1 ? _T_61_3 : _GEN_179; // @[operandCollector.scala 231:58 232:22]
  wire  _GEN_210 = io_bankIn_1_bits_regOrder == 2'h1 | _GEN_180; // @[operandCollector.scala 231:58 238:22]
  wire [31:0] _GEN_211 = io_bankIn_1_bits_regOrder == 2'h1 ? _GEN_181 : _GEN_196; // @[operandCollector.scala 231:58]
  wire [31:0] _GEN_212 = io_bankIn_1_bits_regOrder == 2'h1 ? _GEN_182 : _GEN_197; // @[operandCollector.scala 231:58]
  wire [31:0] _GEN_213 = io_bankIn_1_bits_regOrder == 2'h1 ? _GEN_183 : _GEN_198; // @[operandCollector.scala 231:58]
  wire [31:0] _GEN_214 = io_bankIn_1_bits_regOrder == 2'h1 ? _GEN_184 : _GEN_199; // @[operandCollector.scala 231:58]
  wire  _GEN_215 = io_bankIn_1_bits_regOrder == 2'h1 ? _GEN_185 : _GEN_200; // @[operandCollector.scala 231:58]
  wire  _GEN_216 = io_bankIn_1_bits_regOrder == 2'h1 ? _GEN_186 : _GEN_201; // @[operandCollector.scala 231:58]
  wire  _GEN_217 = io_bankIn_1_bits_regOrder == 2'h1 ? _GEN_187 : _GEN_202; // @[operandCollector.scala 231:58]
  wire  _GEN_218 = io_bankIn_1_bits_regOrder == 2'h1 ? _GEN_188 : _GEN_203; // @[operandCollector.scala 231:58]
  wire  _GEN_219 = io_bankIn_1_bits_regOrder == 2'h1 ? _GEN_189 : _GEN_204; // @[operandCollector.scala 231:58]
  wire  _GEN_220 = io_bankIn_1_bits_regOrder == 2'h1 ? _GEN_190 : _GEN_205; // @[operandCollector.scala 231:58]
  wire [31:0] _GEN_221 = io_bankIn_1_bits_regOrder == 2'h0 ? _T_56_0 : _GEN_171; // @[operandCollector.scala 223:52 224:22]
  wire [31:0] _GEN_222 = io_bankIn_1_bits_regOrder == 2'h0 ? _T_56_1 : _GEN_172; // @[operandCollector.scala 223:52 224:22]
  wire [31:0] _GEN_223 = io_bankIn_1_bits_regOrder == 2'h0 ? _T_56_2 : _GEN_173; // @[operandCollector.scala 223:52 224:22]
  wire [31:0] _GEN_224 = io_bankIn_1_bits_regOrder == 2'h0 ? _T_56_3 : _GEN_174; // @[operandCollector.scala 223:52 224:22]
  wire  _GEN_225 = io_bankIn_1_bits_regOrder == 2'h0 | _GEN_175; // @[operandCollector.scala 223:52 230:22]
  wire [31:0] _GEN_226 = io_bankIn_1_bits_regOrder == 2'h0 ? _GEN_176 : _GEN_206; // @[operandCollector.scala 223:52]
  wire [31:0] _GEN_227 = io_bankIn_1_bits_regOrder == 2'h0 ? _GEN_177 : _GEN_207; // @[operandCollector.scala 223:52]
  wire [31:0] _GEN_228 = io_bankIn_1_bits_regOrder == 2'h0 ? _GEN_178 : _GEN_208; // @[operandCollector.scala 223:52]
  wire [31:0] _GEN_229 = io_bankIn_1_bits_regOrder == 2'h0 ? _GEN_179 : _GEN_209; // @[operandCollector.scala 223:52]
  wire  _GEN_230 = io_bankIn_1_bits_regOrder == 2'h0 ? _GEN_180 : _GEN_210; // @[operandCollector.scala 223:52]
  wire [31:0] _GEN_231 = io_bankIn_1_bits_regOrder == 2'h0 ? _GEN_181 : _GEN_211; // @[operandCollector.scala 223:52]
  wire [31:0] _GEN_232 = io_bankIn_1_bits_regOrder == 2'h0 ? _GEN_182 : _GEN_212; // @[operandCollector.scala 223:52]
  wire [31:0] _GEN_233 = io_bankIn_1_bits_regOrder == 2'h0 ? _GEN_183 : _GEN_213; // @[operandCollector.scala 223:52]
  wire [31:0] _GEN_234 = io_bankIn_1_bits_regOrder == 2'h0 ? _GEN_184 : _GEN_214; // @[operandCollector.scala 223:52]
  wire  _GEN_235 = io_bankIn_1_bits_regOrder == 2'h0 ? _GEN_185 : _GEN_215; // @[operandCollector.scala 223:52]
  wire  _GEN_236 = io_bankIn_1_bits_regOrder == 2'h0 ? _GEN_186 : _GEN_216; // @[operandCollector.scala 223:52]
  wire  _GEN_237 = io_bankIn_1_bits_regOrder == 2'h0 ? _GEN_187 : _GEN_217; // @[operandCollector.scala 223:52]
  wire  _GEN_238 = io_bankIn_1_bits_regOrder == 2'h0 ? _GEN_188 : _GEN_218; // @[operandCollector.scala 223:52]
  wire  _GEN_239 = io_bankIn_1_bits_regOrder == 2'h0 ? _GEN_189 : _GEN_219; // @[operandCollector.scala 223:52]
  wire  _GEN_240 = io_bankIn_1_bits_regOrder == 2'h0 ? _GEN_190 : _GEN_220; // @[operandCollector.scala 223:52]
  wire [31:0] _GEN_241 = _T_51 ? _GEN_221 : _GEN_171; // @[operandCollector.scala 222:33]
  wire [31:0] _GEN_242 = _T_51 ? _GEN_222 : _GEN_172; // @[operandCollector.scala 222:33]
  wire [31:0] _GEN_243 = _T_51 ? _GEN_223 : _GEN_173; // @[operandCollector.scala 222:33]
  wire [31:0] _GEN_244 = _T_51 ? _GEN_224 : _GEN_174; // @[operandCollector.scala 222:33]
  wire  _GEN_245 = _T_51 ? _GEN_225 : _GEN_175; // @[operandCollector.scala 222:33]
  wire [31:0] _GEN_246 = _T_51 ? _GEN_226 : _GEN_176; // @[operandCollector.scala 222:33]
  wire [31:0] _GEN_247 = _T_51 ? _GEN_227 : _GEN_177; // @[operandCollector.scala 222:33]
  wire [31:0] _GEN_248 = _T_51 ? _GEN_228 : _GEN_178; // @[operandCollector.scala 222:33]
  wire [31:0] _GEN_249 = _T_51 ? _GEN_229 : _GEN_179; // @[operandCollector.scala 222:33]
  wire  _GEN_250 = _T_51 ? _GEN_230 : _GEN_180; // @[operandCollector.scala 222:33]
  wire [31:0] _GEN_251 = _T_51 ? _GEN_231 : _GEN_181; // @[operandCollector.scala 222:33]
  wire [31:0] _GEN_252 = _T_51 ? _GEN_232 : _GEN_182; // @[operandCollector.scala 222:33]
  wire [31:0] _GEN_253 = _T_51 ? _GEN_233 : _GEN_183; // @[operandCollector.scala 222:33]
  wire [31:0] _GEN_254 = _T_51 ? _GEN_234 : _GEN_184; // @[operandCollector.scala 222:33]
  wire  _GEN_255 = _T_51 ? _GEN_235 : _GEN_185; // @[operandCollector.scala 222:33]
  wire  _GEN_256 = _T_51 ? _GEN_236 : _GEN_186; // @[operandCollector.scala 222:33]
  wire  _GEN_257 = _T_51 ? _GEN_237 : _GEN_187; // @[operandCollector.scala 222:33]
  wire  _GEN_258 = _T_51 ? _GEN_238 : _GEN_188; // @[operandCollector.scala 222:33]
  wire  _GEN_259 = _T_51 ? _GEN_239 : _GEN_189; // @[operandCollector.scala 222:33]
  wire  _GEN_260 = _T_51 ? _GEN_240 : _GEN_190; // @[operandCollector.scala 222:33]
  wire  _T_79 = io_bankIn_2_ready & io_bankIn_2_valid; // @[Decoupled.scala 52:35]
  wire [31:0] _T_82_0 = 2'h1 == rsType_0 ? io_bankIn_2_bits_data_0 : 32'h0; // @[Mux.scala 81:58]
  wire [31:0] _T_84_0 = 2'h2 == rsType_0 ? io_bankIn_2_bits_data_0 : _T_82_0; // @[Mux.scala 81:58]
  wire [31:0] _T_84_1 = 2'h2 == rsType_0 ? io_bankIn_2_bits_data_1 : _T_82_0; // @[Mux.scala 81:58]
  wire [31:0] _T_84_2 = 2'h2 == rsType_0 ? io_bankIn_2_bits_data_2 : _T_82_0; // @[Mux.scala 81:58]
  wire [31:0] _T_84_3 = 2'h2 == rsType_0 ? io_bankIn_2_bits_data_3 : _T_82_0; // @[Mux.scala 81:58]
  wire [31:0] _T_87_0 = 2'h1 == rsType_1 ? io_bankIn_2_bits_data_0 : 32'h0; // @[Mux.scala 81:58]
  wire [31:0] _T_89_0 = 2'h2 == rsType_1 ? io_bankIn_2_bits_data_0 : _T_87_0; // @[Mux.scala 81:58]
  wire [31:0] _T_89_1 = 2'h2 == rsType_1 ? io_bankIn_2_bits_data_1 : _T_87_0; // @[Mux.scala 81:58]
  wire [31:0] _T_89_2 = 2'h2 == rsType_1 ? io_bankIn_2_bits_data_2 : _T_87_0; // @[Mux.scala 81:58]
  wire [31:0] _T_89_3 = 2'h2 == rsType_1 ? io_bankIn_2_bits_data_3 : _T_87_0; // @[Mux.scala 81:58]
  wire [31:0] _T_92 = imm_io_out + io_bankIn_2_bits_data_0; // @[operandCollector.scala 241:66]
  wire [31:0] _T_99_1 = controlReg_isvec ? io_bankIn_2_bits_data_1 : io_bankIn_2_bits_data_0; // @[operandCollector.scala 243:29]
  wire [31:0] _T_99_2 = controlReg_isvec ? io_bankIn_2_bits_data_2 : io_bankIn_2_bits_data_0; // @[operandCollector.scala 243:29]
  wire [31:0] _T_99_3 = controlReg_isvec ? io_bankIn_2_bits_data_3 : io_bankIn_2_bits_data_0; // @[operandCollector.scala 243:29]
  wire [31:0] _T_101_0 = 2'h1 == controlReg_sel_alu3 ? io_bankIn_2_bits_data_0 : _T_92; // @[Mux.scala 81:58]
  wire [31:0] _T_101_1 = 2'h1 == controlReg_sel_alu3 ? io_bankIn_2_bits_data_1 : _T_92; // @[Mux.scala 81:58]
  wire [31:0] _T_101_2 = 2'h1 == controlReg_sel_alu3 ? io_bankIn_2_bits_data_2 : _T_92; // @[Mux.scala 81:58]
  wire [31:0] _T_101_3 = 2'h1 == controlReg_sel_alu3 ? io_bankIn_2_bits_data_3 : _T_92; // @[Mux.scala 81:58]
  wire [31:0] _T_103_0 = 2'h3 == controlReg_sel_alu3 ? io_bankIn_2_bits_data_0 : _T_101_0; // @[Mux.scala 81:58]
  wire [31:0] _T_103_1 = 2'h3 == controlReg_sel_alu3 ? _T_99_1 : _T_101_1; // @[Mux.scala 81:58]
  wire [31:0] _T_103_2 = 2'h3 == controlReg_sel_alu3 ? _T_99_2 : _T_101_2; // @[Mux.scala 81:58]
  wire [31:0] _T_103_3 = 2'h3 == controlReg_sel_alu3 ? _T_99_3 : _T_101_3; // @[Mux.scala 81:58]
  wire [31:0] _T_105_0 = 2'h2 == controlReg_sel_alu3 ? io_bankIn_2_bits_data_0 : _T_103_0; // @[Mux.scala 81:58]
  wire [31:0] _T_105_1 = 2'h2 == controlReg_sel_alu3 ? io_bankIn_2_bits_data_0 : _T_103_1; // @[Mux.scala 81:58]
  wire [31:0] _T_105_2 = 2'h2 == controlReg_sel_alu3 ? io_bankIn_2_bits_data_0 : _T_103_2; // @[Mux.scala 81:58]
  wire [31:0] _T_105_3 = 2'h2 == controlReg_sel_alu3 ? io_bankIn_2_bits_data_0 : _T_103_3; // @[Mux.scala 81:58]
  wire  _GEN_261 = io_bankIn_2_bits_regOrder == 2'h3 ? io_bankIn_2_bits_v0_0[0] : _GEN_256; // @[operandCollector.scala 248:58 250:23]
  wire  _GEN_262 = io_bankIn_2_bits_regOrder == 2'h3 ? io_bankIn_2_bits_v0_0[1] : _GEN_257; // @[operandCollector.scala 248:58 250:23]
  wire  _GEN_263 = io_bankIn_2_bits_regOrder == 2'h3 ? io_bankIn_2_bits_v0_0[2] : _GEN_258; // @[operandCollector.scala 248:58 250:23]
  wire  _GEN_264 = io_bankIn_2_bits_regOrder == 2'h3 ? io_bankIn_2_bits_v0_0[3] : _GEN_259; // @[operandCollector.scala 248:58 250:23]
  wire  _GEN_265 = io_bankIn_2_bits_regOrder == 2'h3 | _GEN_260; // @[operandCollector.scala 248:58 252:22]
  wire [31:0] _GEN_266 = io_bankIn_2_bits_regOrder == 2'h2 ? _T_105_0 : _GEN_251; // @[operandCollector.scala 239:58 240:22]
  wire [31:0] _GEN_267 = io_bankIn_2_bits_regOrder == 2'h2 ? _T_105_1 : _GEN_252; // @[operandCollector.scala 239:58 240:22]
  wire [31:0] _GEN_268 = io_bankIn_2_bits_regOrder == 2'h2 ? _T_105_2 : _GEN_253; // @[operandCollector.scala 239:58 240:22]
  wire [31:0] _GEN_269 = io_bankIn_2_bits_regOrder == 2'h2 ? _T_105_3 : _GEN_254; // @[operandCollector.scala 239:58 240:22]
  wire  _GEN_270 = io_bankIn_2_bits_regOrder == 2'h2 | _GEN_255; // @[operandCollector.scala 239:58 247:22]
  wire  _GEN_271 = io_bankIn_2_bits_regOrder == 2'h2 ? _GEN_256 : _GEN_261; // @[operandCollector.scala 239:58]
  wire  _GEN_272 = io_bankIn_2_bits_regOrder == 2'h2 ? _GEN_257 : _GEN_262; // @[operandCollector.scala 239:58]
  wire  _GEN_273 = io_bankIn_2_bits_regOrder == 2'h2 ? _GEN_258 : _GEN_263; // @[operandCollector.scala 239:58]
  wire  _GEN_274 = io_bankIn_2_bits_regOrder == 2'h2 ? _GEN_259 : _GEN_264; // @[operandCollector.scala 239:58]
  wire  _GEN_275 = io_bankIn_2_bits_regOrder == 2'h2 ? _GEN_260 : _GEN_265; // @[operandCollector.scala 239:58]
  wire [31:0] _GEN_276 = io_bankIn_2_bits_regOrder == 2'h1 ? _T_89_0 : _GEN_246; // @[operandCollector.scala 231:58 232:22]
  wire [31:0] _GEN_277 = io_bankIn_2_bits_regOrder == 2'h1 ? _T_89_1 : _GEN_247; // @[operandCollector.scala 231:58 232:22]
  wire [31:0] _GEN_278 = io_bankIn_2_bits_regOrder == 2'h1 ? _T_89_2 : _GEN_248; // @[operandCollector.scala 231:58 232:22]
  wire [31:0] _GEN_279 = io_bankIn_2_bits_regOrder == 2'h1 ? _T_89_3 : _GEN_249; // @[operandCollector.scala 231:58 232:22]
  wire  _GEN_280 = io_bankIn_2_bits_regOrder == 2'h1 | _GEN_250; // @[operandCollector.scala 231:58 238:22]
  wire [31:0] _GEN_281 = io_bankIn_2_bits_regOrder == 2'h1 ? _GEN_251 : _GEN_266; // @[operandCollector.scala 231:58]
  wire [31:0] _GEN_282 = io_bankIn_2_bits_regOrder == 2'h1 ? _GEN_252 : _GEN_267; // @[operandCollector.scala 231:58]
  wire [31:0] _GEN_283 = io_bankIn_2_bits_regOrder == 2'h1 ? _GEN_253 : _GEN_268; // @[operandCollector.scala 231:58]
  wire [31:0] _GEN_284 = io_bankIn_2_bits_regOrder == 2'h1 ? _GEN_254 : _GEN_269; // @[operandCollector.scala 231:58]
  wire  _GEN_285 = io_bankIn_2_bits_regOrder == 2'h1 ? _GEN_255 : _GEN_270; // @[operandCollector.scala 231:58]
  wire  _GEN_286 = io_bankIn_2_bits_regOrder == 2'h1 ? _GEN_256 : _GEN_271; // @[operandCollector.scala 231:58]
  wire  _GEN_287 = io_bankIn_2_bits_regOrder == 2'h1 ? _GEN_257 : _GEN_272; // @[operandCollector.scala 231:58]
  wire  _GEN_288 = io_bankIn_2_bits_regOrder == 2'h1 ? _GEN_258 : _GEN_273; // @[operandCollector.scala 231:58]
  wire  _GEN_289 = io_bankIn_2_bits_regOrder == 2'h1 ? _GEN_259 : _GEN_274; // @[operandCollector.scala 231:58]
  wire  _GEN_290 = io_bankIn_2_bits_regOrder == 2'h1 ? _GEN_260 : _GEN_275; // @[operandCollector.scala 231:58]
  wire [31:0] _GEN_291 = io_bankIn_2_bits_regOrder == 2'h0 ? _T_84_0 : _GEN_241; // @[operandCollector.scala 223:52 224:22]
  wire [31:0] _GEN_292 = io_bankIn_2_bits_regOrder == 2'h0 ? _T_84_1 : _GEN_242; // @[operandCollector.scala 223:52 224:22]
  wire [31:0] _GEN_293 = io_bankIn_2_bits_regOrder == 2'h0 ? _T_84_2 : _GEN_243; // @[operandCollector.scala 223:52 224:22]
  wire [31:0] _GEN_294 = io_bankIn_2_bits_regOrder == 2'h0 ? _T_84_3 : _GEN_244; // @[operandCollector.scala 223:52 224:22]
  wire  _GEN_295 = io_bankIn_2_bits_regOrder == 2'h0 | _GEN_245; // @[operandCollector.scala 223:52 230:22]
  wire [31:0] _GEN_296 = io_bankIn_2_bits_regOrder == 2'h0 ? _GEN_246 : _GEN_276; // @[operandCollector.scala 223:52]
  wire [31:0] _GEN_297 = io_bankIn_2_bits_regOrder == 2'h0 ? _GEN_247 : _GEN_277; // @[operandCollector.scala 223:52]
  wire [31:0] _GEN_298 = io_bankIn_2_bits_regOrder == 2'h0 ? _GEN_248 : _GEN_278; // @[operandCollector.scala 223:52]
  wire [31:0] _GEN_299 = io_bankIn_2_bits_regOrder == 2'h0 ? _GEN_249 : _GEN_279; // @[operandCollector.scala 223:52]
  wire  _GEN_300 = io_bankIn_2_bits_regOrder == 2'h0 ? _GEN_250 : _GEN_280; // @[operandCollector.scala 223:52]
  wire [31:0] _GEN_301 = io_bankIn_2_bits_regOrder == 2'h0 ? _GEN_251 : _GEN_281; // @[operandCollector.scala 223:52]
  wire [31:0] _GEN_302 = io_bankIn_2_bits_regOrder == 2'h0 ? _GEN_252 : _GEN_282; // @[operandCollector.scala 223:52]
  wire [31:0] _GEN_303 = io_bankIn_2_bits_regOrder == 2'h0 ? _GEN_253 : _GEN_283; // @[operandCollector.scala 223:52]
  wire [31:0] _GEN_304 = io_bankIn_2_bits_regOrder == 2'h0 ? _GEN_254 : _GEN_284; // @[operandCollector.scala 223:52]
  wire  _GEN_305 = io_bankIn_2_bits_regOrder == 2'h0 ? _GEN_255 : _GEN_285; // @[operandCollector.scala 223:52]
  wire  _GEN_306 = io_bankIn_2_bits_regOrder == 2'h0 ? _GEN_256 : _GEN_286; // @[operandCollector.scala 223:52]
  wire  _GEN_307 = io_bankIn_2_bits_regOrder == 2'h0 ? _GEN_257 : _GEN_287; // @[operandCollector.scala 223:52]
  wire  _GEN_308 = io_bankIn_2_bits_regOrder == 2'h0 ? _GEN_258 : _GEN_288; // @[operandCollector.scala 223:52]
  wire  _GEN_309 = io_bankIn_2_bits_regOrder == 2'h0 ? _GEN_259 : _GEN_289; // @[operandCollector.scala 223:52]
  wire  _GEN_310 = io_bankIn_2_bits_regOrder == 2'h0 ? _GEN_260 : _GEN_290; // @[operandCollector.scala 223:52]
  wire [31:0] _GEN_311 = _T_79 ? _GEN_291 : _GEN_241; // @[operandCollector.scala 222:33]
  wire [31:0] _GEN_312 = _T_79 ? _GEN_292 : _GEN_242; // @[operandCollector.scala 222:33]
  wire [31:0] _GEN_313 = _T_79 ? _GEN_293 : _GEN_243; // @[operandCollector.scala 222:33]
  wire [31:0] _GEN_314 = _T_79 ? _GEN_294 : _GEN_244; // @[operandCollector.scala 222:33]
  wire  _GEN_315 = _T_79 ? _GEN_295 : _GEN_245; // @[operandCollector.scala 222:33]
  wire [31:0] _GEN_316 = _T_79 ? _GEN_296 : _GEN_246; // @[operandCollector.scala 222:33]
  wire [31:0] _GEN_317 = _T_79 ? _GEN_297 : _GEN_247; // @[operandCollector.scala 222:33]
  wire [31:0] _GEN_318 = _T_79 ? _GEN_298 : _GEN_248; // @[operandCollector.scala 222:33]
  wire [31:0] _GEN_319 = _T_79 ? _GEN_299 : _GEN_249; // @[operandCollector.scala 222:33]
  wire  _GEN_320 = _T_79 ? _GEN_300 : _GEN_250; // @[operandCollector.scala 222:33]
  wire [31:0] _GEN_321 = _T_79 ? _GEN_301 : _GEN_251; // @[operandCollector.scala 222:33]
  wire [31:0] _GEN_322 = _T_79 ? _GEN_302 : _GEN_252; // @[operandCollector.scala 222:33]
  wire [31:0] _GEN_323 = _T_79 ? _GEN_303 : _GEN_253; // @[operandCollector.scala 222:33]
  wire [31:0] _GEN_324 = _T_79 ? _GEN_304 : _GEN_254; // @[operandCollector.scala 222:33]
  wire  _GEN_325 = _T_79 ? _GEN_305 : _GEN_255; // @[operandCollector.scala 222:33]
  wire  _GEN_326 = _T_79 ? _GEN_306 : _GEN_256; // @[operandCollector.scala 222:33]
  wire  _GEN_327 = _T_79 ? _GEN_307 : _GEN_257; // @[operandCollector.scala 222:33]
  wire  _GEN_328 = _T_79 ? _GEN_308 : _GEN_258; // @[operandCollector.scala 222:33]
  wire  _GEN_329 = _T_79 ? _GEN_309 : _GEN_259; // @[operandCollector.scala 222:33]
  wire  _GEN_330 = _T_79 ? _GEN_310 : _GEN_260; // @[operandCollector.scala 222:33]
  wire  _T_107 = io_bankIn_3_ready & io_bankIn_3_valid; // @[Decoupled.scala 52:35]
  wire [31:0] _T_110_0 = 2'h1 == rsType_0 ? io_bankIn_3_bits_data_0 : 32'h0; // @[Mux.scala 81:58]
  wire [31:0] _T_112_0 = 2'h2 == rsType_0 ? io_bankIn_3_bits_data_0 : _T_110_0; // @[Mux.scala 81:58]
  wire [31:0] _T_112_1 = 2'h2 == rsType_0 ? io_bankIn_3_bits_data_1 : _T_110_0; // @[Mux.scala 81:58]
  wire [31:0] _T_112_2 = 2'h2 == rsType_0 ? io_bankIn_3_bits_data_2 : _T_110_0; // @[Mux.scala 81:58]
  wire [31:0] _T_112_3 = 2'h2 == rsType_0 ? io_bankIn_3_bits_data_3 : _T_110_0; // @[Mux.scala 81:58]
  wire [31:0] _T_115_0 = 2'h1 == rsType_1 ? io_bankIn_3_bits_data_0 : 32'h0; // @[Mux.scala 81:58]
  wire [31:0] _T_117_0 = 2'h2 == rsType_1 ? io_bankIn_3_bits_data_0 : _T_115_0; // @[Mux.scala 81:58]
  wire [31:0] _T_117_1 = 2'h2 == rsType_1 ? io_bankIn_3_bits_data_1 : _T_115_0; // @[Mux.scala 81:58]
  wire [31:0] _T_117_2 = 2'h2 == rsType_1 ? io_bankIn_3_bits_data_2 : _T_115_0; // @[Mux.scala 81:58]
  wire [31:0] _T_117_3 = 2'h2 == rsType_1 ? io_bankIn_3_bits_data_3 : _T_115_0; // @[Mux.scala 81:58]
  wire [31:0] _T_120 = imm_io_out + io_bankIn_3_bits_data_0; // @[operandCollector.scala 241:66]
  wire [31:0] _T_127_1 = controlReg_isvec ? io_bankIn_3_bits_data_1 : io_bankIn_3_bits_data_0; // @[operandCollector.scala 243:29]
  wire [31:0] _T_127_2 = controlReg_isvec ? io_bankIn_3_bits_data_2 : io_bankIn_3_bits_data_0; // @[operandCollector.scala 243:29]
  wire [31:0] _T_127_3 = controlReg_isvec ? io_bankIn_3_bits_data_3 : io_bankIn_3_bits_data_0; // @[operandCollector.scala 243:29]
  wire [31:0] _T_129_0 = 2'h1 == controlReg_sel_alu3 ? io_bankIn_3_bits_data_0 : _T_120; // @[Mux.scala 81:58]
  wire [31:0] _T_129_1 = 2'h1 == controlReg_sel_alu3 ? io_bankIn_3_bits_data_1 : _T_120; // @[Mux.scala 81:58]
  wire [31:0] _T_129_2 = 2'h1 == controlReg_sel_alu3 ? io_bankIn_3_bits_data_2 : _T_120; // @[Mux.scala 81:58]
  wire [31:0] _T_129_3 = 2'h1 == controlReg_sel_alu3 ? io_bankIn_3_bits_data_3 : _T_120; // @[Mux.scala 81:58]
  wire [31:0] _T_131_0 = 2'h3 == controlReg_sel_alu3 ? io_bankIn_3_bits_data_0 : _T_129_0; // @[Mux.scala 81:58]
  wire [31:0] _T_131_1 = 2'h3 == controlReg_sel_alu3 ? _T_127_1 : _T_129_1; // @[Mux.scala 81:58]
  wire [31:0] _T_131_2 = 2'h3 == controlReg_sel_alu3 ? _T_127_2 : _T_129_2; // @[Mux.scala 81:58]
  wire [31:0] _T_131_3 = 2'h3 == controlReg_sel_alu3 ? _T_127_3 : _T_129_3; // @[Mux.scala 81:58]
  wire [31:0] _T_133_0 = 2'h2 == controlReg_sel_alu3 ? io_bankIn_3_bits_data_0 : _T_131_0; // @[Mux.scala 81:58]
  wire [31:0] _T_133_1 = 2'h2 == controlReg_sel_alu3 ? io_bankIn_3_bits_data_0 : _T_131_1; // @[Mux.scala 81:58]
  wire [31:0] _T_133_2 = 2'h2 == controlReg_sel_alu3 ? io_bankIn_3_bits_data_0 : _T_131_2; // @[Mux.scala 81:58]
  wire [31:0] _T_133_3 = 2'h2 == controlReg_sel_alu3 ? io_bankIn_3_bits_data_0 : _T_131_3; // @[Mux.scala 81:58]
  wire  _GEN_331 = io_bankIn_3_bits_regOrder == 2'h3 ? io_bankIn_3_bits_v0_0[0] : _GEN_326; // @[operandCollector.scala 248:58 250:23]
  wire  _GEN_332 = io_bankIn_3_bits_regOrder == 2'h3 ? io_bankIn_3_bits_v0_0[1] : _GEN_327; // @[operandCollector.scala 248:58 250:23]
  wire  _GEN_333 = io_bankIn_3_bits_regOrder == 2'h3 ? io_bankIn_3_bits_v0_0[2] : _GEN_328; // @[operandCollector.scala 248:58 250:23]
  wire  _GEN_334 = io_bankIn_3_bits_regOrder == 2'h3 ? io_bankIn_3_bits_v0_0[3] : _GEN_329; // @[operandCollector.scala 248:58 250:23]
  wire  _GEN_335 = io_bankIn_3_bits_regOrder == 2'h3 | _GEN_330; // @[operandCollector.scala 248:58 252:22]
  wire [31:0] _GEN_336 = io_bankIn_3_bits_regOrder == 2'h2 ? _T_133_0 : _GEN_321; // @[operandCollector.scala 239:58 240:22]
  wire [31:0] _GEN_337 = io_bankIn_3_bits_regOrder == 2'h2 ? _T_133_1 : _GEN_322; // @[operandCollector.scala 239:58 240:22]
  wire [31:0] _GEN_338 = io_bankIn_3_bits_regOrder == 2'h2 ? _T_133_2 : _GEN_323; // @[operandCollector.scala 239:58 240:22]
  wire [31:0] _GEN_339 = io_bankIn_3_bits_regOrder == 2'h2 ? _T_133_3 : _GEN_324; // @[operandCollector.scala 239:58 240:22]
  wire  _GEN_340 = io_bankIn_3_bits_regOrder == 2'h2 | _GEN_325; // @[operandCollector.scala 239:58 247:22]
  wire  _GEN_341 = io_bankIn_3_bits_regOrder == 2'h2 ? _GEN_326 : _GEN_331; // @[operandCollector.scala 239:58]
  wire  _GEN_342 = io_bankIn_3_bits_regOrder == 2'h2 ? _GEN_327 : _GEN_332; // @[operandCollector.scala 239:58]
  wire  _GEN_343 = io_bankIn_3_bits_regOrder == 2'h2 ? _GEN_328 : _GEN_333; // @[operandCollector.scala 239:58]
  wire  _GEN_344 = io_bankIn_3_bits_regOrder == 2'h2 ? _GEN_329 : _GEN_334; // @[operandCollector.scala 239:58]
  wire  _GEN_345 = io_bankIn_3_bits_regOrder == 2'h2 ? _GEN_330 : _GEN_335; // @[operandCollector.scala 239:58]
  wire [31:0] _GEN_346 = io_bankIn_3_bits_regOrder == 2'h1 ? _T_117_0 : _GEN_316; // @[operandCollector.scala 231:58 232:22]
  wire [31:0] _GEN_347 = io_bankIn_3_bits_regOrder == 2'h1 ? _T_117_1 : _GEN_317; // @[operandCollector.scala 231:58 232:22]
  wire [31:0] _GEN_348 = io_bankIn_3_bits_regOrder == 2'h1 ? _T_117_2 : _GEN_318; // @[operandCollector.scala 231:58 232:22]
  wire [31:0] _GEN_349 = io_bankIn_3_bits_regOrder == 2'h1 ? _T_117_3 : _GEN_319; // @[operandCollector.scala 231:58 232:22]
  wire  _GEN_350 = io_bankIn_3_bits_regOrder == 2'h1 | _GEN_320; // @[operandCollector.scala 231:58 238:22]
  wire [31:0] _GEN_351 = io_bankIn_3_bits_regOrder == 2'h1 ? _GEN_321 : _GEN_336; // @[operandCollector.scala 231:58]
  wire [31:0] _GEN_352 = io_bankIn_3_bits_regOrder == 2'h1 ? _GEN_322 : _GEN_337; // @[operandCollector.scala 231:58]
  wire [31:0] _GEN_353 = io_bankIn_3_bits_regOrder == 2'h1 ? _GEN_323 : _GEN_338; // @[operandCollector.scala 231:58]
  wire [31:0] _GEN_354 = io_bankIn_3_bits_regOrder == 2'h1 ? _GEN_324 : _GEN_339; // @[operandCollector.scala 231:58]
  wire  _GEN_355 = io_bankIn_3_bits_regOrder == 2'h1 ? _GEN_325 : _GEN_340; // @[operandCollector.scala 231:58]
  wire  _GEN_356 = io_bankIn_3_bits_regOrder == 2'h1 ? _GEN_326 : _GEN_341; // @[operandCollector.scala 231:58]
  wire  _GEN_357 = io_bankIn_3_bits_regOrder == 2'h1 ? _GEN_327 : _GEN_342; // @[operandCollector.scala 231:58]
  wire  _GEN_358 = io_bankIn_3_bits_regOrder == 2'h1 ? _GEN_328 : _GEN_343; // @[operandCollector.scala 231:58]
  wire  _GEN_359 = io_bankIn_3_bits_regOrder == 2'h1 ? _GEN_329 : _GEN_344; // @[operandCollector.scala 231:58]
  wire  _GEN_360 = io_bankIn_3_bits_regOrder == 2'h1 ? _GEN_330 : _GEN_345; // @[operandCollector.scala 231:58]
  wire [31:0] _GEN_361 = io_bankIn_3_bits_regOrder == 2'h0 ? _T_112_0 : _GEN_311; // @[operandCollector.scala 223:52 224:22]
  wire [31:0] _GEN_362 = io_bankIn_3_bits_regOrder == 2'h0 ? _T_112_1 : _GEN_312; // @[operandCollector.scala 223:52 224:22]
  wire [31:0] _GEN_363 = io_bankIn_3_bits_regOrder == 2'h0 ? _T_112_2 : _GEN_313; // @[operandCollector.scala 223:52 224:22]
  wire [31:0] _GEN_364 = io_bankIn_3_bits_regOrder == 2'h0 ? _T_112_3 : _GEN_314; // @[operandCollector.scala 223:52 224:22]
  wire  _GEN_365 = io_bankIn_3_bits_regOrder == 2'h0 | _GEN_315; // @[operandCollector.scala 223:52 230:22]
  wire [31:0] _GEN_366 = io_bankIn_3_bits_regOrder == 2'h0 ? _GEN_316 : _GEN_346; // @[operandCollector.scala 223:52]
  wire [31:0] _GEN_367 = io_bankIn_3_bits_regOrder == 2'h0 ? _GEN_317 : _GEN_347; // @[operandCollector.scala 223:52]
  wire [31:0] _GEN_368 = io_bankIn_3_bits_regOrder == 2'h0 ? _GEN_318 : _GEN_348; // @[operandCollector.scala 223:52]
  wire [31:0] _GEN_369 = io_bankIn_3_bits_regOrder == 2'h0 ? _GEN_319 : _GEN_349; // @[operandCollector.scala 223:52]
  wire [31:0] _GEN_371 = io_bankIn_3_bits_regOrder == 2'h0 ? _GEN_321 : _GEN_351; // @[operandCollector.scala 223:52]
  wire [31:0] _GEN_372 = io_bankIn_3_bits_regOrder == 2'h0 ? _GEN_322 : _GEN_352; // @[operandCollector.scala 223:52]
  wire [31:0] _GEN_373 = io_bankIn_3_bits_regOrder == 2'h0 ? _GEN_323 : _GEN_353; // @[operandCollector.scala 223:52]
  wire [31:0] _GEN_374 = io_bankIn_3_bits_regOrder == 2'h0 ? _GEN_324 : _GEN_354; // @[operandCollector.scala 223:52]
  ImmGen imm ( // @[operandCollector.scala 76:19]
    .io_inst(imm_io_inst),
    .io_sel(imm_io_sel),
    .io_out(imm_io_out)
  );
  assign io_control_ready = _io_outArbiterIO_0_bits_bankID_T_1 & ~(|_io_control_ready_T_1); // @[operandCollector.scala 114:39]
  assign io_bankIn_0_ready = state == 2'h1 & ~ready_0; // @[operandCollector.scala 103:45]
  assign io_bankIn_1_ready = state == 2'h1 & ~ready_1; // @[operandCollector.scala 103:45]
  assign io_bankIn_2_ready = state == 2'h1 & ~ready_2; // @[operandCollector.scala 103:45]
  assign io_bankIn_3_ready = state == 2'h1 & ~ready_3; // @[operandCollector.scala 103:45]
  assign io_issue_valid = state == 2'h2; // @[operandCollector.scala 113:26]
  assign io_issue_bits_alu_src1_0 = rsReg_0_0; // @[operandCollector.scala 262:26]
  assign io_issue_bits_alu_src1_1 = rsReg_0_1; // @[operandCollector.scala 262:26]
  assign io_issue_bits_alu_src1_2 = rsReg_0_2; // @[operandCollector.scala 262:26]
  assign io_issue_bits_alu_src1_3 = rsReg_0_3; // @[operandCollector.scala 262:26]
  assign io_issue_bits_alu_src2_0 = rsReg_1_0; // @[operandCollector.scala 263:26]
  assign io_issue_bits_alu_src2_1 = rsReg_1_1; // @[operandCollector.scala 263:26]
  assign io_issue_bits_alu_src2_2 = rsReg_1_2; // @[operandCollector.scala 263:26]
  assign io_issue_bits_alu_src2_3 = rsReg_1_3; // @[operandCollector.scala 263:26]
  assign io_issue_bits_alu_src3_0 = rsReg_2_0; // @[operandCollector.scala 264:26]
  assign io_issue_bits_alu_src3_1 = rsReg_2_1; // @[operandCollector.scala 264:26]
  assign io_issue_bits_alu_src3_2 = rsReg_2_2; // @[operandCollector.scala 264:26]
  assign io_issue_bits_alu_src3_3 = rsReg_2_3; // @[operandCollector.scala 264:26]
  assign io_issue_bits_mask_0 = mask_0; // @[operandCollector.scala 265:22]
  assign io_issue_bits_mask_1 = mask_1; // @[operandCollector.scala 265:22]
  assign io_issue_bits_mask_2 = mask_2; // @[operandCollector.scala 265:22]
  assign io_issue_bits_mask_3 = mask_3; // @[operandCollector.scala 265:22]
  assign io_issue_bits_control_inst = controlReg_inst; // @[operandCollector.scala 59:25]
  assign io_issue_bits_control_wid = controlReg_wid; // @[operandCollector.scala 59:25]
  assign io_issue_bits_control_fp = controlReg_fp; // @[operandCollector.scala 59:25]
  assign io_issue_bits_control_branch = controlReg_branch; // @[operandCollector.scala 59:25]
  assign io_issue_bits_control_simt_stack = controlReg_simt_stack; // @[operandCollector.scala 59:25]
  assign io_issue_bits_control_simt_stack_op = controlReg_simt_stack_op; // @[operandCollector.scala 59:25]
  assign io_issue_bits_control_barrier = controlReg_barrier; // @[operandCollector.scala 59:25]
  assign io_issue_bits_control_csr = controlReg_csr; // @[operandCollector.scala 59:25]
  assign io_issue_bits_control_reverse = controlReg_reverse; // @[operandCollector.scala 59:25]
  assign io_issue_bits_control_sel_alu2 = controlReg_sel_alu2; // @[operandCollector.scala 59:25]
  assign io_issue_bits_control_sel_alu1 = controlReg_sel_alu1; // @[operandCollector.scala 59:25]
  assign io_issue_bits_control_isvec = controlReg_isvec; // @[operandCollector.scala 59:25]
  assign io_issue_bits_control_sel_alu3 = controlReg_sel_alu3; // @[operandCollector.scala 59:25]
  assign io_issue_bits_control_mask = controlReg_mask; // @[operandCollector.scala 59:25]
  assign io_issue_bits_control_sel_imm = controlReg_sel_imm; // @[operandCollector.scala 59:25]
  assign io_issue_bits_control_mem_whb = controlReg_mem_whb; // @[operandCollector.scala 59:25]
  assign io_issue_bits_control_mem_unsigned = controlReg_mem_unsigned; // @[operandCollector.scala 59:25]
  assign io_issue_bits_control_alu_fn = controlReg_alu_fn; // @[operandCollector.scala 59:25]
  assign io_issue_bits_control_mem = controlReg_mem; // @[operandCollector.scala 59:25]
  assign io_issue_bits_control_mul = controlReg_mul; // @[operandCollector.scala 59:25]
  assign io_issue_bits_control_mem_cmd = controlReg_mem_cmd; // @[operandCollector.scala 59:25]
  assign io_issue_bits_control_mop = controlReg_mop; // @[operandCollector.scala 59:25]
  assign io_issue_bits_control_reg_idx1 = controlReg_reg_idx1; // @[operandCollector.scala 59:25]
  assign io_issue_bits_control_reg_idx2 = controlReg_reg_idx2; // @[operandCollector.scala 59:25]
  assign io_issue_bits_control_reg_idx3 = controlReg_reg_idx3; // @[operandCollector.scala 59:25]
  assign io_issue_bits_control_reg_idxw = controlReg_reg_idxw; // @[operandCollector.scala 59:25]
  assign io_issue_bits_control_wfd = controlReg_wfd; // @[operandCollector.scala 59:25]
  assign io_issue_bits_control_fence = controlReg_fence; // @[operandCollector.scala 59:25]
  assign io_issue_bits_control_sfu = controlReg_sfu; // @[operandCollector.scala 59:25]
  assign io_issue_bits_control_readmask = controlReg_readmask; // @[operandCollector.scala 59:25]
  assign io_issue_bits_control_writemask = controlReg_writemask; // @[operandCollector.scala 59:25]
  assign io_issue_bits_control_wxd = controlReg_wxd; // @[operandCollector.scala 59:25]
  assign io_issue_bits_control_pc = controlReg_pc; // @[operandCollector.scala 59:25]
  assign io_issue_bits_control_spike_info_pc = controlReg_spike_info_pc; // @[operandCollector.scala 59:25]
  assign io_issue_bits_control_spike_info_inst = controlReg_spike_info_inst; // @[operandCollector.scala 59:25]
  assign io_outArbiterIO_0_valid = 2'h1 == state ? _io_outArbiterIO_0_valid_T_5 : _T_12 & _io_outArbiterIO_0_valid_T_2; // @[Mux.scala 81:58]
  assign io_outArbiterIO_0_bits_rsAddr = _io_outArbiterIO_0_bits_rsAddr_T_72[4:0]; // @[operandCollector.scala 97:38]
  assign io_outArbiterIO_0_bits_bankID = 6'h23 == _GEN_514 ? 2'h3 : _io_outArbiterIO_0_bits_bankID_T_75; // @[Mux.scala 81:58]
  assign io_outArbiterIO_0_bits_rsType = _io_outArbiterIO_0_bits_bankID_T_2 ? rsTypeWire_0 : rsType_0; // @[operandCollector.scala 99:44]
  assign io_outArbiterIO_1_valid = 2'h1 == state ? _io_outArbiterIO_1_valid_T_5 : _T_12 & _io_outArbiterIO_1_valid_T_2; // @[Mux.scala 81:58]
  assign io_outArbiterIO_1_bits_rsAddr = _io_outArbiterIO_1_bits_rsAddr_T_72[4:0]; // @[operandCollector.scala 97:38]
  assign io_outArbiterIO_1_bits_bankID = 6'h23 == _GEN_521 ? 2'h3 : _io_outArbiterIO_1_bits_bankID_T_75; // @[Mux.scala 81:58]
  assign io_outArbiterIO_1_bits_rsType = _io_outArbiterIO_0_bits_bankID_T_2 ? rsTypeWire_1 : rsType_1; // @[operandCollector.scala 99:44]
  assign io_outArbiterIO_2_valid = 2'h1 == state ? _io_outArbiterIO_2_valid_T_5 : _T_12 & _io_outArbiterIO_2_valid_T_2; // @[Mux.scala 81:58]
  assign io_outArbiterIO_2_bits_rsAddr = _io_outArbiterIO_2_bits_rsAddr_T_72[4:0]; // @[operandCollector.scala 97:38]
  assign io_outArbiterIO_2_bits_bankID = 6'h23 == _GEN_528 ? 2'h3 : _io_outArbiterIO_2_bits_bankID_T_75; // @[Mux.scala 81:58]
  assign io_outArbiterIO_2_bits_rsType = _io_outArbiterIO_0_bits_bankID_T_2 ? rsTypeWire_2 : rsType_2; // @[operandCollector.scala 99:44]
  assign io_outArbiterIO_3_valid = 2'h1 == state ? _io_outArbiterIO_3_valid_T_5 : _T_12 & _io_outArbiterIO_3_valid_T_2; // @[Mux.scala 81:58]
  assign io_outArbiterIO_3_bits_rsAddr = _io_outArbiterIO_3_bits_rsAddr_T_71[4:0]; // @[operandCollector.scala 97:38]
  assign io_outArbiterIO_3_bits_bankID = 6'h23 == _GEN_535 ? 2'h3 : _io_outArbiterIO_3_bits_bankID_T_75; // @[Mux.scala 81:58]
  assign imm_io_inst = 2'h1 == state ? controlReg_inst : _imm_io_inst_T_1; // @[Mux.scala 81:58]
  assign imm_io_sel = 2'h1 == state ? controlReg_sel_imm : _imm_io_sel_T_1; // @[Mux.scala 81:58]
  always @(posedge clock) begin
    if (2'h0 == state) begin // @[operandCollector.scala 143:16]
      if (_io_outArbiterIO_0_bits_bankID_T) begin // @[operandCollector.scala 147:28]
        controlReg_inst <= io_control_bits_inst; // @[operandCollector.scala 148:20]
      end
    end
    if (2'h0 == state) begin // @[operandCollector.scala 143:16]
      if (_io_outArbiterIO_0_bits_bankID_T) begin // @[operandCollector.scala 147:28]
        controlReg_wid <= io_control_bits_wid; // @[operandCollector.scala 148:20]
      end
    end
    if (2'h0 == state) begin // @[operandCollector.scala 143:16]
      if (_io_outArbiterIO_0_bits_bankID_T) begin // @[operandCollector.scala 147:28]
        controlReg_fp <= io_control_bits_fp; // @[operandCollector.scala 148:20]
      end
    end
    if (2'h0 == state) begin // @[operandCollector.scala 143:16]
      if (_io_outArbiterIO_0_bits_bankID_T) begin // @[operandCollector.scala 147:28]
        controlReg_branch <= io_control_bits_branch; // @[operandCollector.scala 148:20]
      end
    end
    if (2'h0 == state) begin // @[operandCollector.scala 143:16]
      if (_io_outArbiterIO_0_bits_bankID_T) begin // @[operandCollector.scala 147:28]
        controlReg_simt_stack <= io_control_bits_simt_stack; // @[operandCollector.scala 148:20]
      end
    end
    if (2'h0 == state) begin // @[operandCollector.scala 143:16]
      if (_io_outArbiterIO_0_bits_bankID_T) begin // @[operandCollector.scala 147:28]
        controlReg_simt_stack_op <= io_control_bits_simt_stack_op; // @[operandCollector.scala 148:20]
      end
    end
    if (2'h0 == state) begin // @[operandCollector.scala 143:16]
      if (_io_outArbiterIO_0_bits_bankID_T) begin // @[operandCollector.scala 147:28]
        controlReg_barrier <= io_control_bits_barrier; // @[operandCollector.scala 148:20]
      end
    end
    if (2'h0 == state) begin // @[operandCollector.scala 143:16]
      if (_io_outArbiterIO_0_bits_bankID_T) begin // @[operandCollector.scala 147:28]
        controlReg_csr <= io_control_bits_csr; // @[operandCollector.scala 148:20]
      end
    end
    if (2'h0 == state) begin // @[operandCollector.scala 143:16]
      if (_io_outArbiterIO_0_bits_bankID_T) begin // @[operandCollector.scala 147:28]
        controlReg_reverse <= io_control_bits_reverse; // @[operandCollector.scala 148:20]
      end
    end
    if (2'h0 == state) begin // @[operandCollector.scala 143:16]
      if (_io_outArbiterIO_0_bits_bankID_T) begin // @[operandCollector.scala 147:28]
        controlReg_sel_alu2 <= io_control_bits_sel_alu2; // @[operandCollector.scala 148:20]
      end
    end
    if (2'h0 == state) begin // @[operandCollector.scala 143:16]
      if (_io_outArbiterIO_0_bits_bankID_T) begin // @[operandCollector.scala 147:28]
        controlReg_sel_alu1 <= io_control_bits_sel_alu1; // @[operandCollector.scala 148:20]
      end
    end
    if (2'h0 == state) begin // @[operandCollector.scala 143:16]
      if (_io_outArbiterIO_0_bits_bankID_T) begin // @[operandCollector.scala 147:28]
        controlReg_isvec <= io_control_bits_isvec; // @[operandCollector.scala 148:20]
      end
    end
    if (2'h0 == state) begin // @[operandCollector.scala 143:16]
      if (_io_outArbiterIO_0_bits_bankID_T) begin // @[operandCollector.scala 147:28]
        controlReg_sel_alu3 <= io_control_bits_sel_alu3; // @[operandCollector.scala 148:20]
      end
    end
    if (2'h0 == state) begin // @[operandCollector.scala 143:16]
      if (_io_outArbiterIO_0_bits_bankID_T) begin // @[operandCollector.scala 147:28]
        controlReg_mask <= io_control_bits_mask; // @[operandCollector.scala 148:20]
      end
    end
    if (2'h0 == state) begin // @[operandCollector.scala 143:16]
      if (_io_outArbiterIO_0_bits_bankID_T) begin // @[operandCollector.scala 147:28]
        controlReg_sel_imm <= io_control_bits_sel_imm; // @[operandCollector.scala 148:20]
      end
    end
    if (2'h0 == state) begin // @[operandCollector.scala 143:16]
      if (_io_outArbiterIO_0_bits_bankID_T) begin // @[operandCollector.scala 147:28]
        controlReg_mem_whb <= io_control_bits_mem_whb; // @[operandCollector.scala 148:20]
      end
    end
    if (2'h0 == state) begin // @[operandCollector.scala 143:16]
      if (_io_outArbiterIO_0_bits_bankID_T) begin // @[operandCollector.scala 147:28]
        controlReg_mem_unsigned <= io_control_bits_mem_unsigned; // @[operandCollector.scala 148:20]
      end
    end
    if (2'h0 == state) begin // @[operandCollector.scala 143:16]
      if (_io_outArbiterIO_0_bits_bankID_T) begin // @[operandCollector.scala 147:28]
        controlReg_alu_fn <= io_control_bits_alu_fn; // @[operandCollector.scala 148:20]
      end
    end
    if (2'h0 == state) begin // @[operandCollector.scala 143:16]
      if (_io_outArbiterIO_0_bits_bankID_T) begin // @[operandCollector.scala 147:28]
        controlReg_mem <= io_control_bits_mem; // @[operandCollector.scala 148:20]
      end
    end
    if (2'h0 == state) begin // @[operandCollector.scala 143:16]
      if (_io_outArbiterIO_0_bits_bankID_T) begin // @[operandCollector.scala 147:28]
        controlReg_mul <= io_control_bits_mul; // @[operandCollector.scala 148:20]
      end
    end
    if (2'h0 == state) begin // @[operandCollector.scala 143:16]
      if (_io_outArbiterIO_0_bits_bankID_T) begin // @[operandCollector.scala 147:28]
        controlReg_mem_cmd <= io_control_bits_mem_cmd; // @[operandCollector.scala 148:20]
      end
    end
    if (2'h0 == state) begin // @[operandCollector.scala 143:16]
      if (_io_outArbiterIO_0_bits_bankID_T) begin // @[operandCollector.scala 147:28]
        controlReg_mop <= io_control_bits_mop; // @[operandCollector.scala 148:20]
      end
    end
    if (2'h0 == state) begin // @[operandCollector.scala 143:16]
      if (_io_outArbiterIO_0_bits_bankID_T) begin // @[operandCollector.scala 147:28]
        controlReg_reg_idx1 <= io_control_bits_reg_idx1; // @[operandCollector.scala 148:20]
      end
    end
    if (2'h0 == state) begin // @[operandCollector.scala 143:16]
      if (_io_outArbiterIO_0_bits_bankID_T) begin // @[operandCollector.scala 147:28]
        controlReg_reg_idx2 <= io_control_bits_reg_idx2; // @[operandCollector.scala 148:20]
      end
    end
    if (2'h0 == state) begin // @[operandCollector.scala 143:16]
      if (_io_outArbiterIO_0_bits_bankID_T) begin // @[operandCollector.scala 147:28]
        controlReg_reg_idx3 <= io_control_bits_reg_idx3; // @[operandCollector.scala 148:20]
      end
    end
    if (2'h0 == state) begin // @[operandCollector.scala 143:16]
      if (_io_outArbiterIO_0_bits_bankID_T) begin // @[operandCollector.scala 147:28]
        controlReg_reg_idxw <= io_control_bits_reg_idxw; // @[operandCollector.scala 148:20]
      end
    end
    if (2'h0 == state) begin // @[operandCollector.scala 143:16]
      if (_io_outArbiterIO_0_bits_bankID_T) begin // @[operandCollector.scala 147:28]
        controlReg_wfd <= io_control_bits_wfd; // @[operandCollector.scala 148:20]
      end
    end
    if (2'h0 == state) begin // @[operandCollector.scala 143:16]
      if (_io_outArbiterIO_0_bits_bankID_T) begin // @[operandCollector.scala 147:28]
        controlReg_fence <= io_control_bits_fence; // @[operandCollector.scala 148:20]
      end
    end
    if (2'h0 == state) begin // @[operandCollector.scala 143:16]
      if (_io_outArbiterIO_0_bits_bankID_T) begin // @[operandCollector.scala 147:28]
        controlReg_sfu <= io_control_bits_sfu; // @[operandCollector.scala 148:20]
      end
    end
    if (2'h0 == state) begin // @[operandCollector.scala 143:16]
      if (_io_outArbiterIO_0_bits_bankID_T) begin // @[operandCollector.scala 147:28]
        controlReg_readmask <= io_control_bits_readmask; // @[operandCollector.scala 148:20]
      end
    end
    if (2'h0 == state) begin // @[operandCollector.scala 143:16]
      if (_io_outArbiterIO_0_bits_bankID_T) begin // @[operandCollector.scala 147:28]
        controlReg_writemask <= io_control_bits_writemask; // @[operandCollector.scala 148:20]
      end
    end
    if (2'h0 == state) begin // @[operandCollector.scala 143:16]
      if (_io_outArbiterIO_0_bits_bankID_T) begin // @[operandCollector.scala 147:28]
        controlReg_wxd <= io_control_bits_wxd; // @[operandCollector.scala 148:20]
      end
    end
    if (2'h0 == state) begin // @[operandCollector.scala 143:16]
      if (_io_outArbiterIO_0_bits_bankID_T) begin // @[operandCollector.scala 147:28]
        controlReg_pc <= io_control_bits_pc; // @[operandCollector.scala 148:20]
      end
    end
    if (2'h0 == state) begin // @[operandCollector.scala 143:16]
      if (_io_outArbiterIO_0_bits_bankID_T) begin // @[operandCollector.scala 147:28]
        controlReg_spike_info_pc <= io_control_bits_spike_info_pc; // @[operandCollector.scala 148:20]
      end
    end
    if (2'h0 == state) begin // @[operandCollector.scala 143:16]
      if (_io_outArbiterIO_0_bits_bankID_T) begin // @[operandCollector.scala 147:28]
        controlReg_spike_info_inst <= io_control_bits_spike_info_inst; // @[operandCollector.scala 148:20]
      end
    end
    if (2'h0 == state) begin // @[operandCollector.scala 143:16]
      if (_io_outArbiterIO_0_bits_bankID_T) begin // @[operandCollector.scala 147:28]
        if (2'h0 == state) begin // @[operandCollector.scala 143:16]
          if (_io_outArbiterIO_0_bits_bankID_T) begin // @[operandCollector.scala 147:28]
            rsType_0 <= io_control_bits_sel_alu1; // @[operandCollector.scala 167:23]
          end else begin
            rsType_0 <= 2'h0; // @[operandCollector.scala 133:14]
          end
        end else begin
          rsType_0 <= 2'h0; // @[operandCollector.scala 133:14]
        end
      end
    end
    if (2'h0 == state) begin // @[operandCollector.scala 143:16]
      if (_io_outArbiterIO_0_bits_bankID_T) begin // @[operandCollector.scala 147:28]
        if (2'h0 == state) begin // @[operandCollector.scala 143:16]
          if (_io_outArbiterIO_0_bits_bankID_T) begin // @[operandCollector.scala 147:28]
            rsType_1 <= io_control_bits_sel_alu2; // @[operandCollector.scala 168:23]
          end else begin
            rsType_1 <= 2'h0; // @[operandCollector.scala 133:14]
          end
        end else begin
          rsType_1 <= 2'h0; // @[operandCollector.scala 133:14]
        end
      end
    end
    if (2'h0 == state) begin // @[operandCollector.scala 143:16]
      if (_io_outArbiterIO_0_bits_bankID_T) begin // @[operandCollector.scala 147:28]
        if (2'h0 == state) begin // @[operandCollector.scala 143:16]
          if (_io_outArbiterIO_0_bits_bankID_T) begin // @[operandCollector.scala 147:28]
            rsType_2 <= _rsTypeWire_2_T_8; // @[operandCollector.scala 169:23]
          end else begin
            rsType_2 <= 2'h0; // @[operandCollector.scala 133:14]
          end
        end else begin
          rsType_2 <= 2'h0; // @[operandCollector.scala 133:14]
        end
      end
    end
    if (2'h0 == state) begin // @[operandCollector.scala 143:16]
      if (_io_outArbiterIO_0_bits_bankID_T) begin // @[operandCollector.scala 147:28]
        ready_0 <= _GEN_17;
      end
    end else if (_io_outArbiterIO_0_valid_T_8) begin // @[operandCollector.scala 143:16]
      if (_T_107) begin // @[operandCollector.scala 222:33]
        ready_0 <= _GEN_365;
      end else if (_T_79) begin // @[operandCollector.scala 222:33]
        ready_0 <= _GEN_295;
      end else begin
        ready_0 <= _GEN_245;
      end
    end else if (2'h2 == state) begin // @[operandCollector.scala 143:16]
      ready_0 <= 1'h0; // @[operandCollector.scala 259:23]
    end
    if (2'h0 == state) begin // @[operandCollector.scala 143:16]
      if (_io_outArbiterIO_0_bits_bankID_T) begin // @[operandCollector.scala 147:28]
        ready_1 <= _GEN_29;
      end
    end else if (_io_outArbiterIO_0_valid_T_8) begin // @[operandCollector.scala 143:16]
      if (_T_107) begin // @[operandCollector.scala 222:33]
        if (io_bankIn_3_bits_regOrder == 2'h0) begin // @[operandCollector.scala 223:52]
          ready_1 <= _GEN_320;
        end else begin
          ready_1 <= _GEN_350;
        end
      end else begin
        ready_1 <= _GEN_320;
      end
    end else if (2'h2 == state) begin // @[operandCollector.scala 143:16]
      ready_1 <= 1'h0; // @[operandCollector.scala 259:23]
    end
    if (2'h0 == state) begin // @[operandCollector.scala 143:16]
      if (_io_outArbiterIO_0_bits_bankID_T) begin // @[operandCollector.scala 147:28]
        ready_2 <= _T_20;
      end
    end else if (_io_outArbiterIO_0_valid_T_8) begin // @[operandCollector.scala 143:16]
      if (_T_107) begin // @[operandCollector.scala 222:33]
        if (io_bankIn_3_bits_regOrder == 2'h0) begin // @[operandCollector.scala 223:52]
          ready_2 <= _GEN_325;
        end else begin
          ready_2 <= _GEN_355;
        end
      end else begin
        ready_2 <= _GEN_325;
      end
    end else if (2'h2 == state) begin // @[operandCollector.scala 143:16]
      ready_2 <= 1'h0; // @[operandCollector.scala 259:23]
    end
    if (2'h0 == state) begin // @[operandCollector.scala 143:16]
      if (_io_outArbiterIO_0_bits_bankID_T) begin // @[operandCollector.scala 147:28]
        ready_3 <= _T_21;
      end
    end else if (_io_outArbiterIO_0_valid_T_8) begin // @[operandCollector.scala 143:16]
      if (_T_107) begin // @[operandCollector.scala 222:33]
        if (io_bankIn_3_bits_regOrder == 2'h0) begin // @[operandCollector.scala 223:52]
          ready_3 <= _GEN_330;
        end else begin
          ready_3 <= _GEN_360;
        end
      end else begin
        ready_3 <= _GEN_330;
      end
    end else if (2'h2 == state) begin // @[operandCollector.scala 143:16]
      ready_3 <= 1'h0; // @[operandCollector.scala 259:23]
    end
    if (2'h0 == state) begin // @[operandCollector.scala 143:16]
      valid_0 <= _GEN_85;
    end else if (!(_io_outArbiterIO_0_valid_T_8)) begin // @[operandCollector.scala 143:16]
      if (2'h2 == state) begin // @[operandCollector.scala 143:16]
        valid_0 <= 1'h0; // @[operandCollector.scala 258:23]
      end
    end
    if (2'h0 == state) begin // @[operandCollector.scala 143:16]
      valid_1 <= _GEN_86;
    end else if (!(_io_outArbiterIO_0_valid_T_8)) begin // @[operandCollector.scala 143:16]
      if (2'h2 == state) begin // @[operandCollector.scala 143:16]
        valid_1 <= 1'h0; // @[operandCollector.scala 258:23]
      end
    end
    if (2'h0 == state) begin // @[operandCollector.scala 143:16]
      valid_2 <= _GEN_87;
    end else if (!(_io_outArbiterIO_0_valid_T_8)) begin // @[operandCollector.scala 143:16]
      if (2'h2 == state) begin // @[operandCollector.scala 143:16]
        valid_2 <= 1'h0; // @[operandCollector.scala 258:23]
      end
    end
    if (2'h0 == state) begin // @[operandCollector.scala 143:16]
      valid_3 <= _GEN_88;
    end else if (!(_io_outArbiterIO_0_valid_T_8)) begin // @[operandCollector.scala 143:16]
      if (2'h2 == state) begin // @[operandCollector.scala 143:16]
        valid_3 <= 1'h0; // @[operandCollector.scala 258:23]
      end
    end
    if (2'h0 == state) begin // @[operandCollector.scala 143:16]
      if (_io_outArbiterIO_0_bits_bankID_T) begin // @[operandCollector.scala 147:28]
        if (2'h0 == state) begin // @[operandCollector.scala 143:16]
          if (_io_outArbiterIO_0_bits_bankID_T) begin // @[operandCollector.scala 147:28]
            regIdx_0 <= io_control_bits_reg_idx1; // @[operandCollector.scala 150:23]
          end else begin
            regIdx_0 <= 5'h0; // @[operandCollector.scala 132:14]
          end
        end else begin
          regIdx_0 <= 5'h0; // @[operandCollector.scala 132:14]
        end
      end
    end
    if (2'h0 == state) begin // @[operandCollector.scala 143:16]
      if (_io_outArbiterIO_0_bits_bankID_T) begin // @[operandCollector.scala 147:28]
        if (2'h0 == state) begin // @[operandCollector.scala 143:16]
          if (_io_outArbiterIO_0_bits_bankID_T) begin // @[operandCollector.scala 147:28]
            regIdx_1 <= io_control_bits_reg_idx2; // @[operandCollector.scala 151:23]
          end else begin
            regIdx_1 <= 5'h0; // @[operandCollector.scala 132:14]
          end
        end else begin
          regIdx_1 <= 5'h0; // @[operandCollector.scala 132:14]
        end
      end
    end
    if (2'h0 == state) begin // @[operandCollector.scala 143:16]
      if (_io_outArbiterIO_0_bits_bankID_T) begin // @[operandCollector.scala 147:28]
        if (2'h0 == state) begin // @[operandCollector.scala 143:16]
          if (_io_outArbiterIO_0_bits_bankID_T) begin // @[operandCollector.scala 147:28]
            regIdx_2 <= _regIdxWire_2_T_8; // @[operandCollector.scala 152:23]
          end else begin
            regIdx_2 <= 5'h0; // @[operandCollector.scala 132:14]
          end
        end else begin
          regIdx_2 <= 5'h0; // @[operandCollector.scala 132:14]
        end
      end
    end
    if (reset) begin // @[operandCollector.scala 69:22]
      rsReg_0_0 <= 32'h0; // @[operandCollector.scala 69:22]
    end else if (2'h0 == state) begin // @[operandCollector.scala 143:16]
      if (_io_outArbiterIO_0_bits_bankID_T) begin // @[operandCollector.scala 147:28]
        if (io_control_bits_sel_alu1 == 2'h3) begin // @[operandCollector.scala 183:92]
          rsReg_0_0 <= imm_io_out; // @[operandCollector.scala 184:29]
        end else begin
          rsReg_0_0 <= _GEN_6;
        end
      end
    end else if (_io_outArbiterIO_0_valid_T_8) begin // @[operandCollector.scala 143:16]
      if (_T_107) begin // @[operandCollector.scala 222:33]
        rsReg_0_0 <= _GEN_361;
      end else begin
        rsReg_0_0 <= _GEN_311;
      end
    end
    if (reset) begin // @[operandCollector.scala 69:22]
      rsReg_0_1 <= 32'h0; // @[operandCollector.scala 69:22]
    end else if (2'h0 == state) begin // @[operandCollector.scala 143:16]
      if (_io_outArbiterIO_0_bits_bankID_T) begin // @[operandCollector.scala 147:28]
        if (io_control_bits_sel_alu1 == 2'h3) begin // @[operandCollector.scala 183:92]
          rsReg_0_1 <= imm_io_out; // @[operandCollector.scala 184:29]
        end else begin
          rsReg_0_1 <= _GEN_7;
        end
      end
    end else if (_io_outArbiterIO_0_valid_T_8) begin // @[operandCollector.scala 143:16]
      if (_T_107) begin // @[operandCollector.scala 222:33]
        rsReg_0_1 <= _GEN_362;
      end else begin
        rsReg_0_1 <= _GEN_312;
      end
    end
    if (reset) begin // @[operandCollector.scala 69:22]
      rsReg_0_2 <= 32'h0; // @[operandCollector.scala 69:22]
    end else if (2'h0 == state) begin // @[operandCollector.scala 143:16]
      if (_io_outArbiterIO_0_bits_bankID_T) begin // @[operandCollector.scala 147:28]
        if (io_control_bits_sel_alu1 == 2'h3) begin // @[operandCollector.scala 183:92]
          rsReg_0_2 <= imm_io_out; // @[operandCollector.scala 184:29]
        end else begin
          rsReg_0_2 <= _GEN_8;
        end
      end
    end else if (_io_outArbiterIO_0_valid_T_8) begin // @[operandCollector.scala 143:16]
      if (_T_107) begin // @[operandCollector.scala 222:33]
        rsReg_0_2 <= _GEN_363;
      end else begin
        rsReg_0_2 <= _GEN_313;
      end
    end
    if (reset) begin // @[operandCollector.scala 69:22]
      rsReg_0_3 <= 32'h0; // @[operandCollector.scala 69:22]
    end else if (2'h0 == state) begin // @[operandCollector.scala 143:16]
      if (_io_outArbiterIO_0_bits_bankID_T) begin // @[operandCollector.scala 147:28]
        if (io_control_bits_sel_alu1 == 2'h3) begin // @[operandCollector.scala 183:92]
          rsReg_0_3 <= imm_io_out; // @[operandCollector.scala 184:29]
        end else begin
          rsReg_0_3 <= _GEN_9;
        end
      end
    end else if (_io_outArbiterIO_0_valid_T_8) begin // @[operandCollector.scala 143:16]
      if (_T_107) begin // @[operandCollector.scala 222:33]
        rsReg_0_3 <= _GEN_364;
      end else begin
        rsReg_0_3 <= _GEN_314;
      end
    end
    if (reset) begin // @[operandCollector.scala 69:22]
      rsReg_1_0 <= 32'h0; // @[operandCollector.scala 69:22]
    end else if (2'h0 == state) begin // @[operandCollector.scala 143:16]
      if (_io_outArbiterIO_0_bits_bankID_T) begin // @[operandCollector.scala 147:28]
        if (io_control_bits_sel_alu2 == 2'h3) begin // @[operandCollector.scala 193:89]
          rsReg_1_0 <= imm_io_out; // @[operandCollector.scala 194:29]
        end else begin
          rsReg_1_0 <= _GEN_18;
        end
      end
    end else if (_io_outArbiterIO_0_valid_T_8) begin // @[operandCollector.scala 143:16]
      if (_T_107) begin // @[operandCollector.scala 222:33]
        rsReg_1_0 <= _GEN_366;
      end else begin
        rsReg_1_0 <= _GEN_316;
      end
    end
    if (reset) begin // @[operandCollector.scala 69:22]
      rsReg_1_1 <= 32'h0; // @[operandCollector.scala 69:22]
    end else if (2'h0 == state) begin // @[operandCollector.scala 143:16]
      if (_io_outArbiterIO_0_bits_bankID_T) begin // @[operandCollector.scala 147:28]
        if (io_control_bits_sel_alu2 == 2'h3) begin // @[operandCollector.scala 193:89]
          rsReg_1_1 <= imm_io_out; // @[operandCollector.scala 194:29]
        end else begin
          rsReg_1_1 <= _GEN_19;
        end
      end
    end else if (_io_outArbiterIO_0_valid_T_8) begin // @[operandCollector.scala 143:16]
      if (_T_107) begin // @[operandCollector.scala 222:33]
        rsReg_1_1 <= _GEN_367;
      end else begin
        rsReg_1_1 <= _GEN_317;
      end
    end
    if (reset) begin // @[operandCollector.scala 69:22]
      rsReg_1_2 <= 32'h0; // @[operandCollector.scala 69:22]
    end else if (2'h0 == state) begin // @[operandCollector.scala 143:16]
      if (_io_outArbiterIO_0_bits_bankID_T) begin // @[operandCollector.scala 147:28]
        if (io_control_bits_sel_alu2 == 2'h3) begin // @[operandCollector.scala 193:89]
          rsReg_1_2 <= imm_io_out; // @[operandCollector.scala 194:29]
        end else begin
          rsReg_1_2 <= _GEN_20;
        end
      end
    end else if (_io_outArbiterIO_0_valid_T_8) begin // @[operandCollector.scala 143:16]
      if (_T_107) begin // @[operandCollector.scala 222:33]
        rsReg_1_2 <= _GEN_368;
      end else begin
        rsReg_1_2 <= _GEN_318;
      end
    end
    if (reset) begin // @[operandCollector.scala 69:22]
      rsReg_1_3 <= 32'h0; // @[operandCollector.scala 69:22]
    end else if (2'h0 == state) begin // @[operandCollector.scala 143:16]
      if (_io_outArbiterIO_0_bits_bankID_T) begin // @[operandCollector.scala 147:28]
        if (io_control_bits_sel_alu2 == 2'h3) begin // @[operandCollector.scala 193:89]
          rsReg_1_3 <= imm_io_out; // @[operandCollector.scala 194:29]
        end else begin
          rsReg_1_3 <= _GEN_21;
        end
      end
    end else if (_io_outArbiterIO_0_valid_T_8) begin // @[operandCollector.scala 143:16]
      if (_T_107) begin // @[operandCollector.scala 222:33]
        rsReg_1_3 <= _GEN_369;
      end else begin
        rsReg_1_3 <= _GEN_319;
      end
    end
    if (reset) begin // @[operandCollector.scala 69:22]
      rsReg_2_0 <= 32'h0; // @[operandCollector.scala 69:22]
    end else if (2'h0 == state) begin // @[operandCollector.scala 143:16]
      if (_io_outArbiterIO_0_bits_bankID_T) begin // @[operandCollector.scala 147:28]
        if (io_control_bits_sel_alu3 == 2'h0 & io_control_bits_branch != 2'h3) begin // @[operandCollector.scala 203:120]
          rsReg_2_0 <= _rsReg_2_0_T_1; // @[operandCollector.scala 204:29]
        end
      end
    end else if (_io_outArbiterIO_0_valid_T_8) begin // @[operandCollector.scala 143:16]
      if (_T_107) begin // @[operandCollector.scala 222:33]
        rsReg_2_0 <= _GEN_371;
      end else begin
        rsReg_2_0 <= _GEN_321;
      end
    end
    if (reset) begin // @[operandCollector.scala 69:22]
      rsReg_2_1 <= 32'h0; // @[operandCollector.scala 69:22]
    end else if (2'h0 == state) begin // @[operandCollector.scala 143:16]
      if (_io_outArbiterIO_0_bits_bankID_T) begin // @[operandCollector.scala 147:28]
        if (io_control_bits_sel_alu3 == 2'h0 & io_control_bits_branch != 2'h3) begin // @[operandCollector.scala 203:120]
          rsReg_2_1 <= _rsReg_2_0_T_1; // @[operandCollector.scala 204:29]
        end
      end
    end else if (_io_outArbiterIO_0_valid_T_8) begin // @[operandCollector.scala 143:16]
      if (_T_107) begin // @[operandCollector.scala 222:33]
        rsReg_2_1 <= _GEN_372;
      end else begin
        rsReg_2_1 <= _GEN_322;
      end
    end
    if (reset) begin // @[operandCollector.scala 69:22]
      rsReg_2_2 <= 32'h0; // @[operandCollector.scala 69:22]
    end else if (2'h0 == state) begin // @[operandCollector.scala 143:16]
      if (_io_outArbiterIO_0_bits_bankID_T) begin // @[operandCollector.scala 147:28]
        if (io_control_bits_sel_alu3 == 2'h0 & io_control_bits_branch != 2'h3) begin // @[operandCollector.scala 203:120]
          rsReg_2_2 <= _rsReg_2_0_T_1; // @[operandCollector.scala 204:29]
        end
      end
    end else if (_io_outArbiterIO_0_valid_T_8) begin // @[operandCollector.scala 143:16]
      if (_T_107) begin // @[operandCollector.scala 222:33]
        rsReg_2_2 <= _GEN_373;
      end else begin
        rsReg_2_2 <= _GEN_323;
      end
    end
    if (reset) begin // @[operandCollector.scala 69:22]
      rsReg_2_3 <= 32'h0; // @[operandCollector.scala 69:22]
    end else if (2'h0 == state) begin // @[operandCollector.scala 143:16]
      if (_io_outArbiterIO_0_bits_bankID_T) begin // @[operandCollector.scala 147:28]
        if (io_control_bits_sel_alu3 == 2'h0 & io_control_bits_branch != 2'h3) begin // @[operandCollector.scala 203:120]
          rsReg_2_3 <= _rsReg_2_0_T_1; // @[operandCollector.scala 204:29]
        end
      end
    end else if (_io_outArbiterIO_0_valid_T_8) begin // @[operandCollector.scala 143:16]
      if (_T_107) begin // @[operandCollector.scala 222:33]
        rsReg_2_3 <= _GEN_374;
      end else begin
        rsReg_2_3 <= _GEN_324;
      end
    end
    if (2'h0 == state) begin // @[operandCollector.scala 143:16]
      if (_io_outArbiterIO_0_bits_bankID_T) begin // @[operandCollector.scala 147:28]
        mask_0 <= _GEN_36;
      end
    end else if (_io_outArbiterIO_0_valid_T_8) begin // @[operandCollector.scala 143:16]
      if (_T_107) begin // @[operandCollector.scala 222:33]
        if (io_bankIn_3_bits_regOrder == 2'h0) begin // @[operandCollector.scala 223:52]
          mask_0 <= _GEN_326;
        end else begin
          mask_0 <= _GEN_356;
        end
      end else begin
        mask_0 <= _GEN_326;
      end
    end
    if (2'h0 == state) begin // @[operandCollector.scala 143:16]
      if (_io_outArbiterIO_0_bits_bankID_T) begin // @[operandCollector.scala 147:28]
        if (~io_control_bits_mask) begin // @[operandCollector.scala 208:36]
          mask_1 <= io_control_bits_isvec; // @[operandCollector.scala 210:20]
        end
      end
    end else if (_io_outArbiterIO_0_valid_T_8) begin // @[operandCollector.scala 143:16]
      if (_T_107) begin // @[operandCollector.scala 222:33]
        if (io_bankIn_3_bits_regOrder == 2'h0) begin // @[operandCollector.scala 223:52]
          mask_1 <= _GEN_327;
        end else begin
          mask_1 <= _GEN_357;
        end
      end else begin
        mask_1 <= _GEN_327;
      end
    end
    if (2'h0 == state) begin // @[operandCollector.scala 143:16]
      if (_io_outArbiterIO_0_bits_bankID_T) begin // @[operandCollector.scala 147:28]
        if (~io_control_bits_mask) begin // @[operandCollector.scala 208:36]
          mask_2 <= io_control_bits_isvec; // @[operandCollector.scala 210:20]
        end
      end
    end else if (_io_outArbiterIO_0_valid_T_8) begin // @[operandCollector.scala 143:16]
      if (_T_107) begin // @[operandCollector.scala 222:33]
        if (io_bankIn_3_bits_regOrder == 2'h0) begin // @[operandCollector.scala 223:52]
          mask_2 <= _GEN_328;
        end else begin
          mask_2 <= _GEN_358;
        end
      end else begin
        mask_2 <= _GEN_328;
      end
    end
    if (2'h0 == state) begin // @[operandCollector.scala 143:16]
      if (_io_outArbiterIO_0_bits_bankID_T) begin // @[operandCollector.scala 147:28]
        if (~io_control_bits_mask) begin // @[operandCollector.scala 208:36]
          mask_3 <= io_control_bits_isvec; // @[operandCollector.scala 210:20]
        end
      end
    end else if (_io_outArbiterIO_0_valid_T_8) begin // @[operandCollector.scala 143:16]
      if (_T_107) begin // @[operandCollector.scala 222:33]
        if (io_bankIn_3_bits_regOrder == 2'h0) begin // @[operandCollector.scala 223:52]
          mask_3 <= _GEN_329;
        end else begin
          mask_3 <= _GEN_359;
        end
      end else begin
        mask_3 <= _GEN_329;
      end
    end
    if (reset) begin // @[operandCollector.scala 79:22]
      state <= 2'h0; // @[operandCollector.scala 79:22]
    end else if (_io_outArbiterIO_0_bits_bankID_T_1) begin // @[operandCollector.scala 116:26]
      if (_io_outArbiterIO_0_bits_bankID_T & _io_control_ready_T_3) begin // @[operandCollector.scala 117:48]
        state <= 2'h1; // @[operandCollector.scala 118:13]
      end else begin
        state <= 2'h0; // @[operandCollector.scala 119:23]
      end
    end else if (_io_bankIn_0_ready_T) begin // @[operandCollector.scala 120:32]
      if (_io_control_ready_T_1 != _T_8) begin // @[operandCollector.scala 121:42]
        state <= 2'h1; // @[operandCollector.scala 122:13]
      end else begin
        state <= 2'h2; // @[operandCollector.scala 125:23]
      end
    end else if (_io_issue_valid_T) begin // @[operandCollector.scala 126:31]
      state <= _GEN_2;
    end else begin
      state <= 2'h0; // @[operandCollector.scala 130:21]
    end
  end
// Register and memory initialization
`ifdef RANDOMIZE_GARBAGE_ASSIGN
`define RANDOMIZE
`endif
`ifdef RANDOMIZE_INVALID_ASSIGN
`define RANDOMIZE
`endif
`ifdef RANDOMIZE_REG_INIT
`define RANDOMIZE
`endif
`ifdef RANDOMIZE_MEM_INIT
`define RANDOMIZE
`endif
`ifndef RANDOM
`define RANDOM $random
`endif
`ifdef RANDOMIZE_MEM_INIT
  integer initvar;
`endif
`ifndef SYNTHESIS
`ifdef FIRRTL_BEFORE_INITIAL
`FIRRTL_BEFORE_INITIAL
`endif
initial begin
  `ifdef RANDOMIZE
    `ifdef INIT_RANDOM
      `INIT_RANDOM
    `endif
    `ifndef VERILATOR
      `ifdef RANDOMIZE_DELAY
        #`RANDOMIZE_DELAY begin end
      `else
        #0.002 begin end
      `endif
    `endif
`ifdef RANDOMIZE_REG_INIT
  _RAND_0 = {1{`RANDOM}};
  controlReg_inst = _RAND_0[31:0];
  _RAND_1 = {1{`RANDOM}};
  controlReg_wid = _RAND_1[1:0];
  _RAND_2 = {1{`RANDOM}};
  controlReg_fp = _RAND_2[0:0];
  _RAND_3 = {1{`RANDOM}};
  controlReg_branch = _RAND_3[1:0];
  _RAND_4 = {1{`RANDOM}};
  controlReg_simt_stack = _RAND_4[0:0];
  _RAND_5 = {1{`RANDOM}};
  controlReg_simt_stack_op = _RAND_5[0:0];
  _RAND_6 = {1{`RANDOM}};
  controlReg_barrier = _RAND_6[0:0];
  _RAND_7 = {1{`RANDOM}};
  controlReg_csr = _RAND_7[1:0];
  _RAND_8 = {1{`RANDOM}};
  controlReg_reverse = _RAND_8[0:0];
  _RAND_9 = {1{`RANDOM}};
  controlReg_sel_alu2 = _RAND_9[1:0];
  _RAND_10 = {1{`RANDOM}};
  controlReg_sel_alu1 = _RAND_10[1:0];
  _RAND_11 = {1{`RANDOM}};
  controlReg_isvec = _RAND_11[0:0];
  _RAND_12 = {1{`RANDOM}};
  controlReg_sel_alu3 = _RAND_12[1:0];
  _RAND_13 = {1{`RANDOM}};
  controlReg_mask = _RAND_13[0:0];
  _RAND_14 = {1{`RANDOM}};
  controlReg_sel_imm = _RAND_14[2:0];
  _RAND_15 = {1{`RANDOM}};
  controlReg_mem_whb = _RAND_15[1:0];
  _RAND_16 = {1{`RANDOM}};
  controlReg_mem_unsigned = _RAND_16[0:0];
  _RAND_17 = {1{`RANDOM}};
  controlReg_alu_fn = _RAND_17[5:0];
  _RAND_18 = {1{`RANDOM}};
  controlReg_mem = _RAND_18[0:0];
  _RAND_19 = {1{`RANDOM}};
  controlReg_mul = _RAND_19[0:0];
  _RAND_20 = {1{`RANDOM}};
  controlReg_mem_cmd = _RAND_20[1:0];
  _RAND_21 = {1{`RANDOM}};
  controlReg_mop = _RAND_21[1:0];
  _RAND_22 = {1{`RANDOM}};
  controlReg_reg_idx1 = _RAND_22[4:0];
  _RAND_23 = {1{`RANDOM}};
  controlReg_reg_idx2 = _RAND_23[4:0];
  _RAND_24 = {1{`RANDOM}};
  controlReg_reg_idx3 = _RAND_24[4:0];
  _RAND_25 = {1{`RANDOM}};
  controlReg_reg_idxw = _RAND_25[4:0];
  _RAND_26 = {1{`RANDOM}};
  controlReg_wfd = _RAND_26[0:0];
  _RAND_27 = {1{`RANDOM}};
  controlReg_fence = _RAND_27[0:0];
  _RAND_28 = {1{`RANDOM}};
  controlReg_sfu = _RAND_28[0:0];
  _RAND_29 = {1{`RANDOM}};
  controlReg_readmask = _RAND_29[0:0];
  _RAND_30 = {1{`RANDOM}};
  controlReg_writemask = _RAND_30[0:0];
  _RAND_31 = {1{`RANDOM}};
  controlReg_wxd = _RAND_31[0:0];
  _RAND_32 = {1{`RANDOM}};
  controlReg_pc = _RAND_32[31:0];
  _RAND_33 = {1{`RANDOM}};
  controlReg_spike_info_pc = _RAND_33[31:0];
  _RAND_34 = {1{`RANDOM}};
  controlReg_spike_info_inst = _RAND_34[31:0];
  _RAND_35 = {1{`RANDOM}};
  rsType_0 = _RAND_35[1:0];
  _RAND_36 = {1{`RANDOM}};
  rsType_1 = _RAND_36[1:0];
  _RAND_37 = {1{`RANDOM}};
  rsType_2 = _RAND_37[1:0];
  _RAND_38 = {1{`RANDOM}};
  ready_0 = _RAND_38[0:0];
  _RAND_39 = {1{`RANDOM}};
  ready_1 = _RAND_39[0:0];
  _RAND_40 = {1{`RANDOM}};
  ready_2 = _RAND_40[0:0];
  _RAND_41 = {1{`RANDOM}};
  ready_3 = _RAND_41[0:0];
  _RAND_42 = {1{`RANDOM}};
  valid_0 = _RAND_42[0:0];
  _RAND_43 = {1{`RANDOM}};
  valid_1 = _RAND_43[0:0];
  _RAND_44 = {1{`RANDOM}};
  valid_2 = _RAND_44[0:0];
  _RAND_45 = {1{`RANDOM}};
  valid_3 = _RAND_45[0:0];
  _RAND_46 = {1{`RANDOM}};
  regIdx_0 = _RAND_46[4:0];
  _RAND_47 = {1{`RANDOM}};
  regIdx_1 = _RAND_47[4:0];
  _RAND_48 = {1{`RANDOM}};
  regIdx_2 = _RAND_48[4:0];
  _RAND_49 = {1{`RANDOM}};
  rsReg_0_0 = _RAND_49[31:0];
  _RAND_50 = {1{`RANDOM}};
  rsReg_0_1 = _RAND_50[31:0];
  _RAND_51 = {1{`RANDOM}};
  rsReg_0_2 = _RAND_51[31:0];
  _RAND_52 = {1{`RANDOM}};
  rsReg_0_3 = _RAND_52[31:0];
  _RAND_53 = {1{`RANDOM}};
  rsReg_1_0 = _RAND_53[31:0];
  _RAND_54 = {1{`RANDOM}};
  rsReg_1_1 = _RAND_54[31:0];
  _RAND_55 = {1{`RANDOM}};
  rsReg_1_2 = _RAND_55[31:0];
  _RAND_56 = {1{`RANDOM}};
  rsReg_1_3 = _RAND_56[31:0];
  _RAND_57 = {1{`RANDOM}};
  rsReg_2_0 = _RAND_57[31:0];
  _RAND_58 = {1{`RANDOM}};
  rsReg_2_1 = _RAND_58[31:0];
  _RAND_59 = {1{`RANDOM}};
  rsReg_2_2 = _RAND_59[31:0];
  _RAND_60 = {1{`RANDOM}};
  rsReg_2_3 = _RAND_60[31:0];
  _RAND_61 = {1{`RANDOM}};
  mask_0 = _RAND_61[0:0];
  _RAND_62 = {1{`RANDOM}};
  mask_1 = _RAND_62[0:0];
  _RAND_63 = {1{`RANDOM}};
  mask_2 = _RAND_63[0:0];
  _RAND_64 = {1{`RANDOM}};
  mask_3 = _RAND_64[0:0];
  _RAND_65 = {1{`RANDOM}};
  state = _RAND_65[1:0];
`endif // RANDOMIZE_REG_INIT
  `endif // RANDOMIZE
end // initial
`ifdef FIRRTL_AFTER_INITIAL
`FIRRTL_AFTER_INITIAL
`endif
`endif // SYNTHESIS
endmodule
module RRArbiter(
  input        clock,
  input        io_in_0_valid,
  input  [4:0] io_in_0_bits_rsAddr,
  input  [1:0] io_in_0_bits_rsType,
  input        io_in_1_valid,
  input  [4:0] io_in_1_bits_rsAddr,
  input  [1:0] io_in_1_bits_rsType,
  input        io_in_2_valid,
  input  [4:0] io_in_2_bits_rsAddr,
  input  [1:0] io_in_2_bits_rsType,
  input        io_in_3_valid,
  input  [4:0] io_in_3_bits_rsAddr,
  input        io_in_4_valid,
  input  [4:0] io_in_4_bits_rsAddr,
  input  [1:0] io_in_4_bits_rsType,
  input        io_in_5_valid,
  input  [4:0] io_in_5_bits_rsAddr,
  input  [1:0] io_in_5_bits_rsType,
  input        io_in_6_valid,
  input  [4:0] io_in_6_bits_rsAddr,
  input  [1:0] io_in_6_bits_rsType,
  input        io_in_7_valid,
  input  [4:0] io_in_7_bits_rsAddr,
  input        io_in_8_valid,
  input  [4:0] io_in_8_bits_rsAddr,
  input  [1:0] io_in_8_bits_rsType,
  input        io_in_9_valid,
  input  [4:0] io_in_9_bits_rsAddr,
  input  [1:0] io_in_9_bits_rsType,
  input        io_in_10_valid,
  input  [4:0] io_in_10_bits_rsAddr,
  input  [1:0] io_in_10_bits_rsType,
  input        io_in_11_valid,
  input  [4:0] io_in_11_bits_rsAddr,
  input        io_in_12_valid,
  input  [4:0] io_in_12_bits_rsAddr,
  input  [1:0] io_in_12_bits_rsType,
  input        io_in_13_valid,
  input  [4:0] io_in_13_bits_rsAddr,
  input  [1:0] io_in_13_bits_rsType,
  input        io_in_14_valid,
  input  [4:0] io_in_14_bits_rsAddr,
  input  [1:0] io_in_14_bits_rsType,
  input        io_in_15_valid,
  input  [4:0] io_in_15_bits_rsAddr,
  output       io_out_valid,
  output [4:0] io_out_bits_rsAddr,
  output [1:0] io_out_bits_rsType,
  output [3:0] io_chosen
);
`ifdef RANDOMIZE_REG_INIT
  reg [31:0] _RAND_0;
`endif // RANDOMIZE_REG_INIT
  wire  _GEN_1 = 4'h1 == io_chosen ? io_in_1_valid : io_in_0_valid; // @[Arbiter.scala 55:{16,16}]
  wire  _GEN_2 = 4'h2 == io_chosen ? io_in_2_valid : _GEN_1; // @[Arbiter.scala 55:{16,16}]
  wire  _GEN_3 = 4'h3 == io_chosen ? io_in_3_valid : _GEN_2; // @[Arbiter.scala 55:{16,16}]
  wire  _GEN_4 = 4'h4 == io_chosen ? io_in_4_valid : _GEN_3; // @[Arbiter.scala 55:{16,16}]
  wire  _GEN_5 = 4'h5 == io_chosen ? io_in_5_valid : _GEN_4; // @[Arbiter.scala 55:{16,16}]
  wire  _GEN_6 = 4'h6 == io_chosen ? io_in_6_valid : _GEN_5; // @[Arbiter.scala 55:{16,16}]
  wire  _GEN_7 = 4'h7 == io_chosen ? io_in_7_valid : _GEN_6; // @[Arbiter.scala 55:{16,16}]
  wire  _GEN_8 = 4'h8 == io_chosen ? io_in_8_valid : _GEN_7; // @[Arbiter.scala 55:{16,16}]
  wire  _GEN_9 = 4'h9 == io_chosen ? io_in_9_valid : _GEN_8; // @[Arbiter.scala 55:{16,16}]
  wire  _GEN_10 = 4'ha == io_chosen ? io_in_10_valid : _GEN_9; // @[Arbiter.scala 55:{16,16}]
  wire  _GEN_11 = 4'hb == io_chosen ? io_in_11_valid : _GEN_10; // @[Arbiter.scala 55:{16,16}]
  wire  _GEN_12 = 4'hc == io_chosen ? io_in_12_valid : _GEN_11; // @[Arbiter.scala 55:{16,16}]
  wire  _GEN_13 = 4'hd == io_chosen ? io_in_13_valid : _GEN_12; // @[Arbiter.scala 55:{16,16}]
  wire  _GEN_14 = 4'he == io_chosen ? io_in_14_valid : _GEN_13; // @[Arbiter.scala 55:{16,16}]
  wire [4:0] _GEN_17 = 4'h1 == io_chosen ? io_in_1_bits_rsAddr : io_in_0_bits_rsAddr; // @[Arbiter.scala 56:{15,15}]
  wire [4:0] _GEN_18 = 4'h2 == io_chosen ? io_in_2_bits_rsAddr : _GEN_17; // @[Arbiter.scala 56:{15,15}]
  wire [4:0] _GEN_19 = 4'h3 == io_chosen ? io_in_3_bits_rsAddr : _GEN_18; // @[Arbiter.scala 56:{15,15}]
  wire [4:0] _GEN_20 = 4'h4 == io_chosen ? io_in_4_bits_rsAddr : _GEN_19; // @[Arbiter.scala 56:{15,15}]
  wire [4:0] _GEN_21 = 4'h5 == io_chosen ? io_in_5_bits_rsAddr : _GEN_20; // @[Arbiter.scala 56:{15,15}]
  wire [4:0] _GEN_22 = 4'h6 == io_chosen ? io_in_6_bits_rsAddr : _GEN_21; // @[Arbiter.scala 56:{15,15}]
  wire [4:0] _GEN_23 = 4'h7 == io_chosen ? io_in_7_bits_rsAddr : _GEN_22; // @[Arbiter.scala 56:{15,15}]
  wire [4:0] _GEN_24 = 4'h8 == io_chosen ? io_in_8_bits_rsAddr : _GEN_23; // @[Arbiter.scala 56:{15,15}]
  wire [4:0] _GEN_25 = 4'h9 == io_chosen ? io_in_9_bits_rsAddr : _GEN_24; // @[Arbiter.scala 56:{15,15}]
  wire [4:0] _GEN_26 = 4'ha == io_chosen ? io_in_10_bits_rsAddr : _GEN_25; // @[Arbiter.scala 56:{15,15}]
  wire [4:0] _GEN_27 = 4'hb == io_chosen ? io_in_11_bits_rsAddr : _GEN_26; // @[Arbiter.scala 56:{15,15}]
  wire [4:0] _GEN_28 = 4'hc == io_chosen ? io_in_12_bits_rsAddr : _GEN_27; // @[Arbiter.scala 56:{15,15}]
  wire [4:0] _GEN_29 = 4'hd == io_chosen ? io_in_13_bits_rsAddr : _GEN_28; // @[Arbiter.scala 56:{15,15}]
  wire [4:0] _GEN_30 = 4'he == io_chosen ? io_in_14_bits_rsAddr : _GEN_29; // @[Arbiter.scala 56:{15,15}]
  wire [1:0] _GEN_49 = 4'h1 == io_chosen ? io_in_1_bits_rsType : io_in_0_bits_rsType; // @[Arbiter.scala 56:{15,15}]
  wire [1:0] _GEN_50 = 4'h2 == io_chosen ? io_in_2_bits_rsType : _GEN_49; // @[Arbiter.scala 56:{15,15}]
  wire [1:0] _GEN_51 = 4'h3 == io_chosen ? 2'h0 : _GEN_50; // @[Arbiter.scala 56:{15,15}]
  wire [1:0] _GEN_52 = 4'h4 == io_chosen ? io_in_4_bits_rsType : _GEN_51; // @[Arbiter.scala 56:{15,15}]
  wire [1:0] _GEN_53 = 4'h5 == io_chosen ? io_in_5_bits_rsType : _GEN_52; // @[Arbiter.scala 56:{15,15}]
  wire [1:0] _GEN_54 = 4'h6 == io_chosen ? io_in_6_bits_rsType : _GEN_53; // @[Arbiter.scala 56:{15,15}]
  wire [1:0] _GEN_55 = 4'h7 == io_chosen ? 2'h0 : _GEN_54; // @[Arbiter.scala 56:{15,15}]
  wire [1:0] _GEN_56 = 4'h8 == io_chosen ? io_in_8_bits_rsType : _GEN_55; // @[Arbiter.scala 56:{15,15}]
  wire [1:0] _GEN_57 = 4'h9 == io_chosen ? io_in_9_bits_rsType : _GEN_56; // @[Arbiter.scala 56:{15,15}]
  wire [1:0] _GEN_58 = 4'ha == io_chosen ? io_in_10_bits_rsType : _GEN_57; // @[Arbiter.scala 56:{15,15}]
  wire [1:0] _GEN_59 = 4'hb == io_chosen ? 2'h0 : _GEN_58; // @[Arbiter.scala 56:{15,15}]
  wire [1:0] _GEN_60 = 4'hc == io_chosen ? io_in_12_bits_rsType : _GEN_59; // @[Arbiter.scala 56:{15,15}]
  wire [1:0] _GEN_61 = 4'hd == io_chosen ? io_in_13_bits_rsType : _GEN_60; // @[Arbiter.scala 56:{15,15}]
  wire [1:0] _GEN_62 = 4'he == io_chosen ? io_in_14_bits_rsType : _GEN_61; // @[Arbiter.scala 56:{15,15}]
  reg [3:0] lastGrant; // @[Reg.scala 19:16]
  wire  grantMask_1 = 4'h1 > lastGrant; // @[Arbiter.scala 81:49]
  wire  grantMask_2 = 4'h2 > lastGrant; // @[Arbiter.scala 81:49]
  wire  grantMask_3 = 4'h3 > lastGrant; // @[Arbiter.scala 81:49]
  wire  grantMask_4 = 4'h4 > lastGrant; // @[Arbiter.scala 81:49]
  wire  grantMask_5 = 4'h5 > lastGrant; // @[Arbiter.scala 81:49]
  wire  grantMask_6 = 4'h6 > lastGrant; // @[Arbiter.scala 81:49]
  wire  grantMask_7 = 4'h7 > lastGrant; // @[Arbiter.scala 81:49]
  wire  grantMask_8 = 4'h8 > lastGrant; // @[Arbiter.scala 81:49]
  wire  grantMask_9 = 4'h9 > lastGrant; // @[Arbiter.scala 81:49]
  wire  grantMask_10 = 4'ha > lastGrant; // @[Arbiter.scala 81:49]
  wire  grantMask_11 = 4'hb > lastGrant; // @[Arbiter.scala 81:49]
  wire  grantMask_12 = 4'hc > lastGrant; // @[Arbiter.scala 81:49]
  wire  grantMask_13 = 4'hd > lastGrant; // @[Arbiter.scala 81:49]
  wire  grantMask_14 = 4'he > lastGrant; // @[Arbiter.scala 81:49]
  wire  grantMask_15 = 4'hf > lastGrant; // @[Arbiter.scala 81:49]
  wire  validMask_1 = io_in_1_valid & grantMask_1; // @[Arbiter.scala 82:76]
  wire  validMask_2 = io_in_2_valid & grantMask_2; // @[Arbiter.scala 82:76]
  wire  validMask_3 = io_in_3_valid & grantMask_3; // @[Arbiter.scala 82:76]
  wire  validMask_4 = io_in_4_valid & grantMask_4; // @[Arbiter.scala 82:76]
  wire  validMask_5 = io_in_5_valid & grantMask_5; // @[Arbiter.scala 82:76]
  wire  validMask_6 = io_in_6_valid & grantMask_6; // @[Arbiter.scala 82:76]
  wire  validMask_7 = io_in_7_valid & grantMask_7; // @[Arbiter.scala 82:76]
  wire  validMask_8 = io_in_8_valid & grantMask_8; // @[Arbiter.scala 82:76]
  wire  validMask_9 = io_in_9_valid & grantMask_9; // @[Arbiter.scala 82:76]
  wire  validMask_10 = io_in_10_valid & grantMask_10; // @[Arbiter.scala 82:76]
  wire  validMask_11 = io_in_11_valid & grantMask_11; // @[Arbiter.scala 82:76]
  wire  validMask_12 = io_in_12_valid & grantMask_12; // @[Arbiter.scala 82:76]
  wire  validMask_13 = io_in_13_valid & grantMask_13; // @[Arbiter.scala 82:76]
  wire  validMask_14 = io_in_14_valid & grantMask_14; // @[Arbiter.scala 82:76]
  wire  validMask_15 = io_in_15_valid & grantMask_15; // @[Arbiter.scala 82:76]
  wire [3:0] _GEN_65 = io_in_14_valid ? 4'he : 4'hf; // @[Arbiter.scala 91:{26,35}]
  wire [3:0] _GEN_66 = io_in_13_valid ? 4'hd : _GEN_65; // @[Arbiter.scala 91:{26,35}]
  wire [3:0] _GEN_67 = io_in_12_valid ? 4'hc : _GEN_66; // @[Arbiter.scala 91:{26,35}]
  wire [3:0] _GEN_68 = io_in_11_valid ? 4'hb : _GEN_67; // @[Arbiter.scala 91:{26,35}]
  wire [3:0] _GEN_69 = io_in_10_valid ? 4'ha : _GEN_68; // @[Arbiter.scala 91:{26,35}]
  wire [3:0] _GEN_70 = io_in_9_valid ? 4'h9 : _GEN_69; // @[Arbiter.scala 91:{26,35}]
  wire [3:0] _GEN_71 = io_in_8_valid ? 4'h8 : _GEN_70; // @[Arbiter.scala 91:{26,35}]
  wire [3:0] _GEN_72 = io_in_7_valid ? 4'h7 : _GEN_71; // @[Arbiter.scala 91:{26,35}]
  wire [3:0] _GEN_73 = io_in_6_valid ? 4'h6 : _GEN_72; // @[Arbiter.scala 91:{26,35}]
  wire [3:0] _GEN_74 = io_in_5_valid ? 4'h5 : _GEN_73; // @[Arbiter.scala 91:{26,35}]
  wire [3:0] _GEN_75 = io_in_4_valid ? 4'h4 : _GEN_74; // @[Arbiter.scala 91:{26,35}]
  wire [3:0] _GEN_76 = io_in_3_valid ? 4'h3 : _GEN_75; // @[Arbiter.scala 91:{26,35}]
  wire [3:0] _GEN_77 = io_in_2_valid ? 4'h2 : _GEN_76; // @[Arbiter.scala 91:{26,35}]
  wire [3:0] _GEN_78 = io_in_1_valid ? 4'h1 : _GEN_77; // @[Arbiter.scala 91:{26,35}]
  wire [3:0] _GEN_79 = io_in_0_valid ? 4'h0 : _GEN_78; // @[Arbiter.scala 91:{26,35}]
  wire [3:0] _GEN_80 = validMask_15 ? 4'hf : _GEN_79; // @[Arbiter.scala 93:{24,33}]
  wire [3:0] _GEN_81 = validMask_14 ? 4'he : _GEN_80; // @[Arbiter.scala 93:{24,33}]
  wire [3:0] _GEN_82 = validMask_13 ? 4'hd : _GEN_81; // @[Arbiter.scala 93:{24,33}]
  wire [3:0] _GEN_83 = validMask_12 ? 4'hc : _GEN_82; // @[Arbiter.scala 93:{24,33}]
  wire [3:0] _GEN_84 = validMask_11 ? 4'hb : _GEN_83; // @[Arbiter.scala 93:{24,33}]
  wire [3:0] _GEN_85 = validMask_10 ? 4'ha : _GEN_84; // @[Arbiter.scala 93:{24,33}]
  wire [3:0] _GEN_86 = validMask_9 ? 4'h9 : _GEN_85; // @[Arbiter.scala 93:{24,33}]
  wire [3:0] _GEN_87 = validMask_8 ? 4'h8 : _GEN_86; // @[Arbiter.scala 93:{24,33}]
  wire [3:0] _GEN_88 = validMask_7 ? 4'h7 : _GEN_87; // @[Arbiter.scala 93:{24,33}]
  wire [3:0] _GEN_89 = validMask_6 ? 4'h6 : _GEN_88; // @[Arbiter.scala 93:{24,33}]
  wire [3:0] _GEN_90 = validMask_5 ? 4'h5 : _GEN_89; // @[Arbiter.scala 93:{24,33}]
  wire [3:0] _GEN_91 = validMask_4 ? 4'h4 : _GEN_90; // @[Arbiter.scala 93:{24,33}]
  wire [3:0] _GEN_92 = validMask_3 ? 4'h3 : _GEN_91; // @[Arbiter.scala 93:{24,33}]
  wire [3:0] _GEN_93 = validMask_2 ? 4'h2 : _GEN_92; // @[Arbiter.scala 93:{24,33}]
  assign io_out_valid = 4'hf == io_chosen ? io_in_15_valid : _GEN_14; // @[Arbiter.scala 55:{16,16}]
  assign io_out_bits_rsAddr = 4'hf == io_chosen ? io_in_15_bits_rsAddr : _GEN_30; // @[Arbiter.scala 56:{15,15}]
  assign io_out_bits_rsType = 4'hf == io_chosen ? 2'h0 : _GEN_62; // @[Arbiter.scala 56:{15,15}]
  assign io_chosen = validMask_1 ? 4'h1 : _GEN_93; // @[Arbiter.scala 93:{24,33}]
  always @(posedge clock) begin
    if (io_out_valid) begin // @[Reg.scala 20:18]
      lastGrant <= io_chosen; // @[Reg.scala 20:22]
    end
  end
// Register and memory initialization
`ifdef RANDOMIZE_GARBAGE_ASSIGN
`define RANDOMIZE
`endif
`ifdef RANDOMIZE_INVALID_ASSIGN
`define RANDOMIZE
`endif
`ifdef RANDOMIZE_REG_INIT
`define RANDOMIZE
`endif
`ifdef RANDOMIZE_MEM_INIT
`define RANDOMIZE
`endif
`ifndef RANDOM
`define RANDOM $random
`endif
`ifdef RANDOMIZE_MEM_INIT
  integer initvar;
`endif
`ifndef SYNTHESIS
`ifdef FIRRTL_BEFORE_INITIAL
`FIRRTL_BEFORE_INITIAL
`endif
initial begin
  `ifdef RANDOMIZE
    `ifdef INIT_RANDOM
      `INIT_RANDOM
    `endif
    `ifndef VERILATOR
      `ifdef RANDOMIZE_DELAY
        #`RANDOMIZE_DELAY begin end
      `else
        #0.002 begin end
      `endif
    `endif
`ifdef RANDOMIZE_REG_INIT
  _RAND_0 = {1{`RANDOM}};
  lastGrant = _RAND_0[3:0];
`endif // RANDOMIZE_REG_INIT
  `endif // RANDOMIZE
end // initial
`ifdef FIRRTL_AFTER_INITIAL
`FIRRTL_AFTER_INITIAL
`endif
`endif // SYNTHESIS
endmodule
module operandArbiter(
  input        clock,
  input        io_readArbiterIO_0_0_valid,
  input  [4:0] io_readArbiterIO_0_0_bits_rsAddr,
  input  [1:0] io_readArbiterIO_0_0_bits_bankID,
  input  [1:0] io_readArbiterIO_0_0_bits_rsType,
  input        io_readArbiterIO_0_1_valid,
  input  [4:0] io_readArbiterIO_0_1_bits_rsAddr,
  input  [1:0] io_readArbiterIO_0_1_bits_bankID,
  input  [1:0] io_readArbiterIO_0_1_bits_rsType,
  input        io_readArbiterIO_0_2_valid,
  input  [4:0] io_readArbiterIO_0_2_bits_rsAddr,
  input  [1:0] io_readArbiterIO_0_2_bits_bankID,
  input  [1:0] io_readArbiterIO_0_2_bits_rsType,
  input        io_readArbiterIO_0_3_valid,
  input  [4:0] io_readArbiterIO_0_3_bits_rsAddr,
  input  [1:0] io_readArbiterIO_0_3_bits_bankID,
  input        io_readArbiterIO_1_0_valid,
  input  [4:0] io_readArbiterIO_1_0_bits_rsAddr,
  input  [1:0] io_readArbiterIO_1_0_bits_bankID,
  input  [1:0] io_readArbiterIO_1_0_bits_rsType,
  input        io_readArbiterIO_1_1_valid,
  input  [4:0] io_readArbiterIO_1_1_bits_rsAddr,
  input  [1:0] io_readArbiterIO_1_1_bits_bankID,
  input  [1:0] io_readArbiterIO_1_1_bits_rsType,
  input        io_readArbiterIO_1_2_valid,
  input  [4:0] io_readArbiterIO_1_2_bits_rsAddr,
  input  [1:0] io_readArbiterIO_1_2_bits_bankID,
  input  [1:0] io_readArbiterIO_1_2_bits_rsType,
  input        io_readArbiterIO_1_3_valid,
  input  [4:0] io_readArbiterIO_1_3_bits_rsAddr,
  input  [1:0] io_readArbiterIO_1_3_bits_bankID,
  input        io_readArbiterIO_2_0_valid,
  input  [4:0] io_readArbiterIO_2_0_bits_rsAddr,
  input  [1:0] io_readArbiterIO_2_0_bits_bankID,
  input  [1:0] io_readArbiterIO_2_0_bits_rsType,
  input        io_readArbiterIO_2_1_valid,
  input  [4:0] io_readArbiterIO_2_1_bits_rsAddr,
  input  [1:0] io_readArbiterIO_2_1_bits_bankID,
  input  [1:0] io_readArbiterIO_2_1_bits_rsType,
  input        io_readArbiterIO_2_2_valid,
  input  [4:0] io_readArbiterIO_2_2_bits_rsAddr,
  input  [1:0] io_readArbiterIO_2_2_bits_bankID,
  input  [1:0] io_readArbiterIO_2_2_bits_rsType,
  input        io_readArbiterIO_2_3_valid,
  input  [4:0] io_readArbiterIO_2_3_bits_rsAddr,
  input  [1:0] io_readArbiterIO_2_3_bits_bankID,
  input        io_readArbiterIO_3_0_valid,
  input  [4:0] io_readArbiterIO_3_0_bits_rsAddr,
  input  [1:0] io_readArbiterIO_3_0_bits_bankID,
  input  [1:0] io_readArbiterIO_3_0_bits_rsType,
  input        io_readArbiterIO_3_1_valid,
  input  [4:0] io_readArbiterIO_3_1_bits_rsAddr,
  input  [1:0] io_readArbiterIO_3_1_bits_bankID,
  input  [1:0] io_readArbiterIO_3_1_bits_rsType,
  input        io_readArbiterIO_3_2_valid,
  input  [4:0] io_readArbiterIO_3_2_bits_rsAddr,
  input  [1:0] io_readArbiterIO_3_2_bits_bankID,
  input  [1:0] io_readArbiterIO_3_2_bits_rsType,
  input        io_readArbiterIO_3_3_valid,
  input  [4:0] io_readArbiterIO_3_3_bits_rsAddr,
  input  [1:0] io_readArbiterIO_3_3_bits_bankID,
  output       io_readArbiterOut_0_valid,
  output [4:0] io_readArbiterOut_0_bits_rsAddr,
  output [1:0] io_readArbiterOut_0_bits_rsType,
  output       io_readArbiterOut_1_valid,
  output [4:0] io_readArbiterOut_1_bits_rsAddr,
  output [1:0] io_readArbiterOut_1_bits_rsType,
  output       io_readArbiterOut_2_valid,
  output [4:0] io_readArbiterOut_2_bits_rsAddr,
  output [1:0] io_readArbiterOut_2_bits_rsType,
  output       io_readArbiterOut_3_valid,
  output [4:0] io_readArbiterOut_3_bits_rsAddr,
  output [1:0] io_readArbiterOut_3_bits_rsType,
  output [3:0] io_readchosen_0,
  output [3:0] io_readchosen_1,
  output [3:0] io_readchosen_2,
  output [3:0] io_readchosen_3
);
  wire  bankArbiterScalar_0_clock; // @[operandCollector.scala 281:19]
  wire  bankArbiterScalar_0_io_in_0_valid; // @[operandCollector.scala 281:19]
  wire [4:0] bankArbiterScalar_0_io_in_0_bits_rsAddr; // @[operandCollector.scala 281:19]
  wire [1:0] bankArbiterScalar_0_io_in_0_bits_rsType; // @[operandCollector.scala 281:19]
  wire  bankArbiterScalar_0_io_in_1_valid; // @[operandCollector.scala 281:19]
  wire [4:0] bankArbiterScalar_0_io_in_1_bits_rsAddr; // @[operandCollector.scala 281:19]
  wire [1:0] bankArbiterScalar_0_io_in_1_bits_rsType; // @[operandCollector.scala 281:19]
  wire  bankArbiterScalar_0_io_in_2_valid; // @[operandCollector.scala 281:19]
  wire [4:0] bankArbiterScalar_0_io_in_2_bits_rsAddr; // @[operandCollector.scala 281:19]
  wire [1:0] bankArbiterScalar_0_io_in_2_bits_rsType; // @[operandCollector.scala 281:19]
  wire  bankArbiterScalar_0_io_in_3_valid; // @[operandCollector.scala 281:19]
  wire [4:0] bankArbiterScalar_0_io_in_3_bits_rsAddr; // @[operandCollector.scala 281:19]
  wire  bankArbiterScalar_0_io_in_4_valid; // @[operandCollector.scala 281:19]
  wire [4:0] bankArbiterScalar_0_io_in_4_bits_rsAddr; // @[operandCollector.scala 281:19]
  wire [1:0] bankArbiterScalar_0_io_in_4_bits_rsType; // @[operandCollector.scala 281:19]
  wire  bankArbiterScalar_0_io_in_5_valid; // @[operandCollector.scala 281:19]
  wire [4:0] bankArbiterScalar_0_io_in_5_bits_rsAddr; // @[operandCollector.scala 281:19]
  wire [1:0] bankArbiterScalar_0_io_in_5_bits_rsType; // @[operandCollector.scala 281:19]
  wire  bankArbiterScalar_0_io_in_6_valid; // @[operandCollector.scala 281:19]
  wire [4:0] bankArbiterScalar_0_io_in_6_bits_rsAddr; // @[operandCollector.scala 281:19]
  wire [1:0] bankArbiterScalar_0_io_in_6_bits_rsType; // @[operandCollector.scala 281:19]
  wire  bankArbiterScalar_0_io_in_7_valid; // @[operandCollector.scala 281:19]
  wire [4:0] bankArbiterScalar_0_io_in_7_bits_rsAddr; // @[operandCollector.scala 281:19]
  wire  bankArbiterScalar_0_io_in_8_valid; // @[operandCollector.scala 281:19]
  wire [4:0] bankArbiterScalar_0_io_in_8_bits_rsAddr; // @[operandCollector.scala 281:19]
  wire [1:0] bankArbiterScalar_0_io_in_8_bits_rsType; // @[operandCollector.scala 281:19]
  wire  bankArbiterScalar_0_io_in_9_valid; // @[operandCollector.scala 281:19]
  wire [4:0] bankArbiterScalar_0_io_in_9_bits_rsAddr; // @[operandCollector.scala 281:19]
  wire [1:0] bankArbiterScalar_0_io_in_9_bits_rsType; // @[operandCollector.scala 281:19]
  wire  bankArbiterScalar_0_io_in_10_valid; // @[operandCollector.scala 281:19]
  wire [4:0] bankArbiterScalar_0_io_in_10_bits_rsAddr; // @[operandCollector.scala 281:19]
  wire [1:0] bankArbiterScalar_0_io_in_10_bits_rsType; // @[operandCollector.scala 281:19]
  wire  bankArbiterScalar_0_io_in_11_valid; // @[operandCollector.scala 281:19]
  wire [4:0] bankArbiterScalar_0_io_in_11_bits_rsAddr; // @[operandCollector.scala 281:19]
  wire  bankArbiterScalar_0_io_in_12_valid; // @[operandCollector.scala 281:19]
  wire [4:0] bankArbiterScalar_0_io_in_12_bits_rsAddr; // @[operandCollector.scala 281:19]
  wire [1:0] bankArbiterScalar_0_io_in_12_bits_rsType; // @[operandCollector.scala 281:19]
  wire  bankArbiterScalar_0_io_in_13_valid; // @[operandCollector.scala 281:19]
  wire [4:0] bankArbiterScalar_0_io_in_13_bits_rsAddr; // @[operandCollector.scala 281:19]
  wire [1:0] bankArbiterScalar_0_io_in_13_bits_rsType; // @[operandCollector.scala 281:19]
  wire  bankArbiterScalar_0_io_in_14_valid; // @[operandCollector.scala 281:19]
  wire [4:0] bankArbiterScalar_0_io_in_14_bits_rsAddr; // @[operandCollector.scala 281:19]
  wire [1:0] bankArbiterScalar_0_io_in_14_bits_rsType; // @[operandCollector.scala 281:19]
  wire  bankArbiterScalar_0_io_in_15_valid; // @[operandCollector.scala 281:19]
  wire [4:0] bankArbiterScalar_0_io_in_15_bits_rsAddr; // @[operandCollector.scala 281:19]
  wire  bankArbiterScalar_0_io_out_valid; // @[operandCollector.scala 281:19]
  wire [4:0] bankArbiterScalar_0_io_out_bits_rsAddr; // @[operandCollector.scala 281:19]
  wire [1:0] bankArbiterScalar_0_io_out_bits_rsType; // @[operandCollector.scala 281:19]
  wire [3:0] bankArbiterScalar_0_io_chosen; // @[operandCollector.scala 281:19]
  wire  bankArbiterScalar_1_clock; // @[operandCollector.scala 281:19]
  wire  bankArbiterScalar_1_io_in_0_valid; // @[operandCollector.scala 281:19]
  wire [4:0] bankArbiterScalar_1_io_in_0_bits_rsAddr; // @[operandCollector.scala 281:19]
  wire [1:0] bankArbiterScalar_1_io_in_0_bits_rsType; // @[operandCollector.scala 281:19]
  wire  bankArbiterScalar_1_io_in_1_valid; // @[operandCollector.scala 281:19]
  wire [4:0] bankArbiterScalar_1_io_in_1_bits_rsAddr; // @[operandCollector.scala 281:19]
  wire [1:0] bankArbiterScalar_1_io_in_1_bits_rsType; // @[operandCollector.scala 281:19]
  wire  bankArbiterScalar_1_io_in_2_valid; // @[operandCollector.scala 281:19]
  wire [4:0] bankArbiterScalar_1_io_in_2_bits_rsAddr; // @[operandCollector.scala 281:19]
  wire [1:0] bankArbiterScalar_1_io_in_2_bits_rsType; // @[operandCollector.scala 281:19]
  wire  bankArbiterScalar_1_io_in_3_valid; // @[operandCollector.scala 281:19]
  wire [4:0] bankArbiterScalar_1_io_in_3_bits_rsAddr; // @[operandCollector.scala 281:19]
  wire  bankArbiterScalar_1_io_in_4_valid; // @[operandCollector.scala 281:19]
  wire [4:0] bankArbiterScalar_1_io_in_4_bits_rsAddr; // @[operandCollector.scala 281:19]
  wire [1:0] bankArbiterScalar_1_io_in_4_bits_rsType; // @[operandCollector.scala 281:19]
  wire  bankArbiterScalar_1_io_in_5_valid; // @[operandCollector.scala 281:19]
  wire [4:0] bankArbiterScalar_1_io_in_5_bits_rsAddr; // @[operandCollector.scala 281:19]
  wire [1:0] bankArbiterScalar_1_io_in_5_bits_rsType; // @[operandCollector.scala 281:19]
  wire  bankArbiterScalar_1_io_in_6_valid; // @[operandCollector.scala 281:19]
  wire [4:0] bankArbiterScalar_1_io_in_6_bits_rsAddr; // @[operandCollector.scala 281:19]
  wire [1:0] bankArbiterScalar_1_io_in_6_bits_rsType; // @[operandCollector.scala 281:19]
  wire  bankArbiterScalar_1_io_in_7_valid; // @[operandCollector.scala 281:19]
  wire [4:0] bankArbiterScalar_1_io_in_7_bits_rsAddr; // @[operandCollector.scala 281:19]
  wire  bankArbiterScalar_1_io_in_8_valid; // @[operandCollector.scala 281:19]
  wire [4:0] bankArbiterScalar_1_io_in_8_bits_rsAddr; // @[operandCollector.scala 281:19]
  wire [1:0] bankArbiterScalar_1_io_in_8_bits_rsType; // @[operandCollector.scala 281:19]
  wire  bankArbiterScalar_1_io_in_9_valid; // @[operandCollector.scala 281:19]
  wire [4:0] bankArbiterScalar_1_io_in_9_bits_rsAddr; // @[operandCollector.scala 281:19]
  wire [1:0] bankArbiterScalar_1_io_in_9_bits_rsType; // @[operandCollector.scala 281:19]
  wire  bankArbiterScalar_1_io_in_10_valid; // @[operandCollector.scala 281:19]
  wire [4:0] bankArbiterScalar_1_io_in_10_bits_rsAddr; // @[operandCollector.scala 281:19]
  wire [1:0] bankArbiterScalar_1_io_in_10_bits_rsType; // @[operandCollector.scala 281:19]
  wire  bankArbiterScalar_1_io_in_11_valid; // @[operandCollector.scala 281:19]
  wire [4:0] bankArbiterScalar_1_io_in_11_bits_rsAddr; // @[operandCollector.scala 281:19]
  wire  bankArbiterScalar_1_io_in_12_valid; // @[operandCollector.scala 281:19]
  wire [4:0] bankArbiterScalar_1_io_in_12_bits_rsAddr; // @[operandCollector.scala 281:19]
  wire [1:0] bankArbiterScalar_1_io_in_12_bits_rsType; // @[operandCollector.scala 281:19]
  wire  bankArbiterScalar_1_io_in_13_valid; // @[operandCollector.scala 281:19]
  wire [4:0] bankArbiterScalar_1_io_in_13_bits_rsAddr; // @[operandCollector.scala 281:19]
  wire [1:0] bankArbiterScalar_1_io_in_13_bits_rsType; // @[operandCollector.scala 281:19]
  wire  bankArbiterScalar_1_io_in_14_valid; // @[operandCollector.scala 281:19]
  wire [4:0] bankArbiterScalar_1_io_in_14_bits_rsAddr; // @[operandCollector.scala 281:19]
  wire [1:0] bankArbiterScalar_1_io_in_14_bits_rsType; // @[operandCollector.scala 281:19]
  wire  bankArbiterScalar_1_io_in_15_valid; // @[operandCollector.scala 281:19]
  wire [4:0] bankArbiterScalar_1_io_in_15_bits_rsAddr; // @[operandCollector.scala 281:19]
  wire  bankArbiterScalar_1_io_out_valid; // @[operandCollector.scala 281:19]
  wire [4:0] bankArbiterScalar_1_io_out_bits_rsAddr; // @[operandCollector.scala 281:19]
  wire [1:0] bankArbiterScalar_1_io_out_bits_rsType; // @[operandCollector.scala 281:19]
  wire [3:0] bankArbiterScalar_1_io_chosen; // @[operandCollector.scala 281:19]
  wire  bankArbiterScalar_2_clock; // @[operandCollector.scala 281:19]
  wire  bankArbiterScalar_2_io_in_0_valid; // @[operandCollector.scala 281:19]
  wire [4:0] bankArbiterScalar_2_io_in_0_bits_rsAddr; // @[operandCollector.scala 281:19]
  wire [1:0] bankArbiterScalar_2_io_in_0_bits_rsType; // @[operandCollector.scala 281:19]
  wire  bankArbiterScalar_2_io_in_1_valid; // @[operandCollector.scala 281:19]
  wire [4:0] bankArbiterScalar_2_io_in_1_bits_rsAddr; // @[operandCollector.scala 281:19]
  wire [1:0] bankArbiterScalar_2_io_in_1_bits_rsType; // @[operandCollector.scala 281:19]
  wire  bankArbiterScalar_2_io_in_2_valid; // @[operandCollector.scala 281:19]
  wire [4:0] bankArbiterScalar_2_io_in_2_bits_rsAddr; // @[operandCollector.scala 281:19]
  wire [1:0] bankArbiterScalar_2_io_in_2_bits_rsType; // @[operandCollector.scala 281:19]
  wire  bankArbiterScalar_2_io_in_3_valid; // @[operandCollector.scala 281:19]
  wire [4:0] bankArbiterScalar_2_io_in_3_bits_rsAddr; // @[operandCollector.scala 281:19]
  wire  bankArbiterScalar_2_io_in_4_valid; // @[operandCollector.scala 281:19]
  wire [4:0] bankArbiterScalar_2_io_in_4_bits_rsAddr; // @[operandCollector.scala 281:19]
  wire [1:0] bankArbiterScalar_2_io_in_4_bits_rsType; // @[operandCollector.scala 281:19]
  wire  bankArbiterScalar_2_io_in_5_valid; // @[operandCollector.scala 281:19]
  wire [4:0] bankArbiterScalar_2_io_in_5_bits_rsAddr; // @[operandCollector.scala 281:19]
  wire [1:0] bankArbiterScalar_2_io_in_5_bits_rsType; // @[operandCollector.scala 281:19]
  wire  bankArbiterScalar_2_io_in_6_valid; // @[operandCollector.scala 281:19]
  wire [4:0] bankArbiterScalar_2_io_in_6_bits_rsAddr; // @[operandCollector.scala 281:19]
  wire [1:0] bankArbiterScalar_2_io_in_6_bits_rsType; // @[operandCollector.scala 281:19]
  wire  bankArbiterScalar_2_io_in_7_valid; // @[operandCollector.scala 281:19]
  wire [4:0] bankArbiterScalar_2_io_in_7_bits_rsAddr; // @[operandCollector.scala 281:19]
  wire  bankArbiterScalar_2_io_in_8_valid; // @[operandCollector.scala 281:19]
  wire [4:0] bankArbiterScalar_2_io_in_8_bits_rsAddr; // @[operandCollector.scala 281:19]
  wire [1:0] bankArbiterScalar_2_io_in_8_bits_rsType; // @[operandCollector.scala 281:19]
  wire  bankArbiterScalar_2_io_in_9_valid; // @[operandCollector.scala 281:19]
  wire [4:0] bankArbiterScalar_2_io_in_9_bits_rsAddr; // @[operandCollector.scala 281:19]
  wire [1:0] bankArbiterScalar_2_io_in_9_bits_rsType; // @[operandCollector.scala 281:19]
  wire  bankArbiterScalar_2_io_in_10_valid; // @[operandCollector.scala 281:19]
  wire [4:0] bankArbiterScalar_2_io_in_10_bits_rsAddr; // @[operandCollector.scala 281:19]
  wire [1:0] bankArbiterScalar_2_io_in_10_bits_rsType; // @[operandCollector.scala 281:19]
  wire  bankArbiterScalar_2_io_in_11_valid; // @[operandCollector.scala 281:19]
  wire [4:0] bankArbiterScalar_2_io_in_11_bits_rsAddr; // @[operandCollector.scala 281:19]
  wire  bankArbiterScalar_2_io_in_12_valid; // @[operandCollector.scala 281:19]
  wire [4:0] bankArbiterScalar_2_io_in_12_bits_rsAddr; // @[operandCollector.scala 281:19]
  wire [1:0] bankArbiterScalar_2_io_in_12_bits_rsType; // @[operandCollector.scala 281:19]
  wire  bankArbiterScalar_2_io_in_13_valid; // @[operandCollector.scala 281:19]
  wire [4:0] bankArbiterScalar_2_io_in_13_bits_rsAddr; // @[operandCollector.scala 281:19]
  wire [1:0] bankArbiterScalar_2_io_in_13_bits_rsType; // @[operandCollector.scala 281:19]
  wire  bankArbiterScalar_2_io_in_14_valid; // @[operandCollector.scala 281:19]
  wire [4:0] bankArbiterScalar_2_io_in_14_bits_rsAddr; // @[operandCollector.scala 281:19]
  wire [1:0] bankArbiterScalar_2_io_in_14_bits_rsType; // @[operandCollector.scala 281:19]
  wire  bankArbiterScalar_2_io_in_15_valid; // @[operandCollector.scala 281:19]
  wire [4:0] bankArbiterScalar_2_io_in_15_bits_rsAddr; // @[operandCollector.scala 281:19]
  wire  bankArbiterScalar_2_io_out_valid; // @[operandCollector.scala 281:19]
  wire [4:0] bankArbiterScalar_2_io_out_bits_rsAddr; // @[operandCollector.scala 281:19]
  wire [1:0] bankArbiterScalar_2_io_out_bits_rsType; // @[operandCollector.scala 281:19]
  wire [3:0] bankArbiterScalar_2_io_chosen; // @[operandCollector.scala 281:19]
  wire  bankArbiterScalar_3_clock; // @[operandCollector.scala 281:19]
  wire  bankArbiterScalar_3_io_in_0_valid; // @[operandCollector.scala 281:19]
  wire [4:0] bankArbiterScalar_3_io_in_0_bits_rsAddr; // @[operandCollector.scala 281:19]
  wire [1:0] bankArbiterScalar_3_io_in_0_bits_rsType; // @[operandCollector.scala 281:19]
  wire  bankArbiterScalar_3_io_in_1_valid; // @[operandCollector.scala 281:19]
  wire [4:0] bankArbiterScalar_3_io_in_1_bits_rsAddr; // @[operandCollector.scala 281:19]
  wire [1:0] bankArbiterScalar_3_io_in_1_bits_rsType; // @[operandCollector.scala 281:19]
  wire  bankArbiterScalar_3_io_in_2_valid; // @[operandCollector.scala 281:19]
  wire [4:0] bankArbiterScalar_3_io_in_2_bits_rsAddr; // @[operandCollector.scala 281:19]
  wire [1:0] bankArbiterScalar_3_io_in_2_bits_rsType; // @[operandCollector.scala 281:19]
  wire  bankArbiterScalar_3_io_in_3_valid; // @[operandCollector.scala 281:19]
  wire [4:0] bankArbiterScalar_3_io_in_3_bits_rsAddr; // @[operandCollector.scala 281:19]
  wire  bankArbiterScalar_3_io_in_4_valid; // @[operandCollector.scala 281:19]
  wire [4:0] bankArbiterScalar_3_io_in_4_bits_rsAddr; // @[operandCollector.scala 281:19]
  wire [1:0] bankArbiterScalar_3_io_in_4_bits_rsType; // @[operandCollector.scala 281:19]
  wire  bankArbiterScalar_3_io_in_5_valid; // @[operandCollector.scala 281:19]
  wire [4:0] bankArbiterScalar_3_io_in_5_bits_rsAddr; // @[operandCollector.scala 281:19]
  wire [1:0] bankArbiterScalar_3_io_in_5_bits_rsType; // @[operandCollector.scala 281:19]
  wire  bankArbiterScalar_3_io_in_6_valid; // @[operandCollector.scala 281:19]
  wire [4:0] bankArbiterScalar_3_io_in_6_bits_rsAddr; // @[operandCollector.scala 281:19]
  wire [1:0] bankArbiterScalar_3_io_in_6_bits_rsType; // @[operandCollector.scala 281:19]
  wire  bankArbiterScalar_3_io_in_7_valid; // @[operandCollector.scala 281:19]
  wire [4:0] bankArbiterScalar_3_io_in_7_bits_rsAddr; // @[operandCollector.scala 281:19]
  wire  bankArbiterScalar_3_io_in_8_valid; // @[operandCollector.scala 281:19]
  wire [4:0] bankArbiterScalar_3_io_in_8_bits_rsAddr; // @[operandCollector.scala 281:19]
  wire [1:0] bankArbiterScalar_3_io_in_8_bits_rsType; // @[operandCollector.scala 281:19]
  wire  bankArbiterScalar_3_io_in_9_valid; // @[operandCollector.scala 281:19]
  wire [4:0] bankArbiterScalar_3_io_in_9_bits_rsAddr; // @[operandCollector.scala 281:19]
  wire [1:0] bankArbiterScalar_3_io_in_9_bits_rsType; // @[operandCollector.scala 281:19]
  wire  bankArbiterScalar_3_io_in_10_valid; // @[operandCollector.scala 281:19]
  wire [4:0] bankArbiterScalar_3_io_in_10_bits_rsAddr; // @[operandCollector.scala 281:19]
  wire [1:0] bankArbiterScalar_3_io_in_10_bits_rsType; // @[operandCollector.scala 281:19]
  wire  bankArbiterScalar_3_io_in_11_valid; // @[operandCollector.scala 281:19]
  wire [4:0] bankArbiterScalar_3_io_in_11_bits_rsAddr; // @[operandCollector.scala 281:19]
  wire  bankArbiterScalar_3_io_in_12_valid; // @[operandCollector.scala 281:19]
  wire [4:0] bankArbiterScalar_3_io_in_12_bits_rsAddr; // @[operandCollector.scala 281:19]
  wire [1:0] bankArbiterScalar_3_io_in_12_bits_rsType; // @[operandCollector.scala 281:19]
  wire  bankArbiterScalar_3_io_in_13_valid; // @[operandCollector.scala 281:19]
  wire [4:0] bankArbiterScalar_3_io_in_13_bits_rsAddr; // @[operandCollector.scala 281:19]
  wire [1:0] bankArbiterScalar_3_io_in_13_bits_rsType; // @[operandCollector.scala 281:19]
  wire  bankArbiterScalar_3_io_in_14_valid; // @[operandCollector.scala 281:19]
  wire [4:0] bankArbiterScalar_3_io_in_14_bits_rsAddr; // @[operandCollector.scala 281:19]
  wire [1:0] bankArbiterScalar_3_io_in_14_bits_rsType; // @[operandCollector.scala 281:19]
  wire  bankArbiterScalar_3_io_in_15_valid; // @[operandCollector.scala 281:19]
  wire [4:0] bankArbiterScalar_3_io_in_15_bits_rsAddr; // @[operandCollector.scala 281:19]
  wire  bankArbiterScalar_3_io_out_valid; // @[operandCollector.scala 281:19]
  wire [4:0] bankArbiterScalar_3_io_out_bits_rsAddr; // @[operandCollector.scala 281:19]
  wire [1:0] bankArbiterScalar_3_io_out_bits_rsType; // @[operandCollector.scala 281:19]
  wire [3:0] bankArbiterScalar_3_io_chosen; // @[operandCollector.scala 281:19]
  wire  _bankArbiterScalar_0_io_in_0_valid_T = io_readArbiterIO_0_0_bits_bankID == 2'h0; // @[operandCollector.scala 303:47]
  wire  _bankArbiterScalar_0_io_in_1_valid_T = io_readArbiterIO_0_1_bits_bankID == 2'h0; // @[operandCollector.scala 303:47]
  wire  _bankArbiterScalar_0_io_in_2_valid_T = io_readArbiterIO_0_2_bits_bankID == 2'h0; // @[operandCollector.scala 303:47]
  wire  _bankArbiterScalar_0_io_in_3_valid_T = io_readArbiterIO_0_3_bits_bankID == 2'h0; // @[operandCollector.scala 303:47]
  wire  _bankArbiterScalar_0_io_in_4_valid_T = io_readArbiterIO_1_0_bits_bankID == 2'h0; // @[operandCollector.scala 303:47]
  wire  _bankArbiterScalar_0_io_in_5_valid_T = io_readArbiterIO_1_1_bits_bankID == 2'h0; // @[operandCollector.scala 303:47]
  wire  _bankArbiterScalar_0_io_in_6_valid_T = io_readArbiterIO_1_2_bits_bankID == 2'h0; // @[operandCollector.scala 303:47]
  wire  _bankArbiterScalar_0_io_in_7_valid_T = io_readArbiterIO_1_3_bits_bankID == 2'h0; // @[operandCollector.scala 303:47]
  wire  _bankArbiterScalar_0_io_in_8_valid_T = io_readArbiterIO_2_0_bits_bankID == 2'h0; // @[operandCollector.scala 303:47]
  wire  _bankArbiterScalar_0_io_in_9_valid_T = io_readArbiterIO_2_1_bits_bankID == 2'h0; // @[operandCollector.scala 303:47]
  wire  _bankArbiterScalar_0_io_in_10_valid_T = io_readArbiterIO_2_2_bits_bankID == 2'h0; // @[operandCollector.scala 303:47]
  wire  _bankArbiterScalar_0_io_in_11_valid_T = io_readArbiterIO_2_3_bits_bankID == 2'h0; // @[operandCollector.scala 303:47]
  wire  _bankArbiterScalar_0_io_in_12_valid_T = io_readArbiterIO_3_0_bits_bankID == 2'h0; // @[operandCollector.scala 303:47]
  wire  _bankArbiterScalar_0_io_in_13_valid_T = io_readArbiterIO_3_1_bits_bankID == 2'h0; // @[operandCollector.scala 303:47]
  wire  _bankArbiterScalar_0_io_in_14_valid_T = io_readArbiterIO_3_2_bits_bankID == 2'h0; // @[operandCollector.scala 303:47]
  wire  _bankArbiterScalar_0_io_in_15_valid_T = io_readArbiterIO_3_3_bits_bankID == 2'h0; // @[operandCollector.scala 303:47]
  wire  _bankArbiterScalar_1_io_in_0_valid_T = io_readArbiterIO_0_0_bits_bankID == 2'h1; // @[operandCollector.scala 303:47]
  wire  _bankArbiterScalar_1_io_in_1_valid_T = io_readArbiterIO_0_1_bits_bankID == 2'h1; // @[operandCollector.scala 303:47]
  wire  _bankArbiterScalar_1_io_in_2_valid_T = io_readArbiterIO_0_2_bits_bankID == 2'h1; // @[operandCollector.scala 303:47]
  wire  _bankArbiterScalar_1_io_in_3_valid_T = io_readArbiterIO_0_3_bits_bankID == 2'h1; // @[operandCollector.scala 303:47]
  wire  _bankArbiterScalar_1_io_in_4_valid_T = io_readArbiterIO_1_0_bits_bankID == 2'h1; // @[operandCollector.scala 303:47]
  wire  _bankArbiterScalar_1_io_in_5_valid_T = io_readArbiterIO_1_1_bits_bankID == 2'h1; // @[operandCollector.scala 303:47]
  wire  _bankArbiterScalar_1_io_in_6_valid_T = io_readArbiterIO_1_2_bits_bankID == 2'h1; // @[operandCollector.scala 303:47]
  wire  _bankArbiterScalar_1_io_in_7_valid_T = io_readArbiterIO_1_3_bits_bankID == 2'h1; // @[operandCollector.scala 303:47]
  wire  _bankArbiterScalar_1_io_in_8_valid_T = io_readArbiterIO_2_0_bits_bankID == 2'h1; // @[operandCollector.scala 303:47]
  wire  _bankArbiterScalar_1_io_in_9_valid_T = io_readArbiterIO_2_1_bits_bankID == 2'h1; // @[operandCollector.scala 303:47]
  wire  _bankArbiterScalar_1_io_in_10_valid_T = io_readArbiterIO_2_2_bits_bankID == 2'h1; // @[operandCollector.scala 303:47]
  wire  _bankArbiterScalar_1_io_in_11_valid_T = io_readArbiterIO_2_3_bits_bankID == 2'h1; // @[operandCollector.scala 303:47]
  wire  _bankArbiterScalar_1_io_in_12_valid_T = io_readArbiterIO_3_0_bits_bankID == 2'h1; // @[operandCollector.scala 303:47]
  wire  _bankArbiterScalar_1_io_in_13_valid_T = io_readArbiterIO_3_1_bits_bankID == 2'h1; // @[operandCollector.scala 303:47]
  wire  _bankArbiterScalar_1_io_in_14_valid_T = io_readArbiterIO_3_2_bits_bankID == 2'h1; // @[operandCollector.scala 303:47]
  wire  _bankArbiterScalar_1_io_in_15_valid_T = io_readArbiterIO_3_3_bits_bankID == 2'h1; // @[operandCollector.scala 303:47]
  wire  _bankArbiterScalar_2_io_in_0_valid_T = io_readArbiterIO_0_0_bits_bankID == 2'h2; // @[operandCollector.scala 303:47]
  wire  _bankArbiterScalar_2_io_in_1_valid_T = io_readArbiterIO_0_1_bits_bankID == 2'h2; // @[operandCollector.scala 303:47]
  wire  _bankArbiterScalar_2_io_in_2_valid_T = io_readArbiterIO_0_2_bits_bankID == 2'h2; // @[operandCollector.scala 303:47]
  wire  _bankArbiterScalar_2_io_in_3_valid_T = io_readArbiterIO_0_3_bits_bankID == 2'h2; // @[operandCollector.scala 303:47]
  wire  _bankArbiterScalar_2_io_in_4_valid_T = io_readArbiterIO_1_0_bits_bankID == 2'h2; // @[operandCollector.scala 303:47]
  wire  _bankArbiterScalar_2_io_in_5_valid_T = io_readArbiterIO_1_1_bits_bankID == 2'h2; // @[operandCollector.scala 303:47]
  wire  _bankArbiterScalar_2_io_in_6_valid_T = io_readArbiterIO_1_2_bits_bankID == 2'h2; // @[operandCollector.scala 303:47]
  wire  _bankArbiterScalar_2_io_in_7_valid_T = io_readArbiterIO_1_3_bits_bankID == 2'h2; // @[operandCollector.scala 303:47]
  wire  _bankArbiterScalar_2_io_in_8_valid_T = io_readArbiterIO_2_0_bits_bankID == 2'h2; // @[operandCollector.scala 303:47]
  wire  _bankArbiterScalar_2_io_in_9_valid_T = io_readArbiterIO_2_1_bits_bankID == 2'h2; // @[operandCollector.scala 303:47]
  wire  _bankArbiterScalar_2_io_in_10_valid_T = io_readArbiterIO_2_2_bits_bankID == 2'h2; // @[operandCollector.scala 303:47]
  wire  _bankArbiterScalar_2_io_in_11_valid_T = io_readArbiterIO_2_3_bits_bankID == 2'h2; // @[operandCollector.scala 303:47]
  wire  _bankArbiterScalar_2_io_in_12_valid_T = io_readArbiterIO_3_0_bits_bankID == 2'h2; // @[operandCollector.scala 303:47]
  wire  _bankArbiterScalar_2_io_in_13_valid_T = io_readArbiterIO_3_1_bits_bankID == 2'h2; // @[operandCollector.scala 303:47]
  wire  _bankArbiterScalar_2_io_in_14_valid_T = io_readArbiterIO_3_2_bits_bankID == 2'h2; // @[operandCollector.scala 303:47]
  wire  _bankArbiterScalar_2_io_in_15_valid_T = io_readArbiterIO_3_3_bits_bankID == 2'h2; // @[operandCollector.scala 303:47]
  wire  _bankArbiterScalar_3_io_in_0_valid_T = io_readArbiterIO_0_0_bits_bankID == 2'h3; // @[operandCollector.scala 303:47]
  wire  _bankArbiterScalar_3_io_in_1_valid_T = io_readArbiterIO_0_1_bits_bankID == 2'h3; // @[operandCollector.scala 303:47]
  wire  _bankArbiterScalar_3_io_in_2_valid_T = io_readArbiterIO_0_2_bits_bankID == 2'h3; // @[operandCollector.scala 303:47]
  wire  _bankArbiterScalar_3_io_in_3_valid_T = io_readArbiterIO_0_3_bits_bankID == 2'h3; // @[operandCollector.scala 303:47]
  wire  _bankArbiterScalar_3_io_in_4_valid_T = io_readArbiterIO_1_0_bits_bankID == 2'h3; // @[operandCollector.scala 303:47]
  wire  _bankArbiterScalar_3_io_in_5_valid_T = io_readArbiterIO_1_1_bits_bankID == 2'h3; // @[operandCollector.scala 303:47]
  wire  _bankArbiterScalar_3_io_in_6_valid_T = io_readArbiterIO_1_2_bits_bankID == 2'h3; // @[operandCollector.scala 303:47]
  wire  _bankArbiterScalar_3_io_in_7_valid_T = io_readArbiterIO_1_3_bits_bankID == 2'h3; // @[operandCollector.scala 303:47]
  wire  _bankArbiterScalar_3_io_in_8_valid_T = io_readArbiterIO_2_0_bits_bankID == 2'h3; // @[operandCollector.scala 303:47]
  wire  _bankArbiterScalar_3_io_in_9_valid_T = io_readArbiterIO_2_1_bits_bankID == 2'h3; // @[operandCollector.scala 303:47]
  wire  _bankArbiterScalar_3_io_in_10_valid_T = io_readArbiterIO_2_2_bits_bankID == 2'h3; // @[operandCollector.scala 303:47]
  wire  _bankArbiterScalar_3_io_in_11_valid_T = io_readArbiterIO_2_3_bits_bankID == 2'h3; // @[operandCollector.scala 303:47]
  wire  _bankArbiterScalar_3_io_in_12_valid_T = io_readArbiterIO_3_0_bits_bankID == 2'h3; // @[operandCollector.scala 303:47]
  wire  _bankArbiterScalar_3_io_in_13_valid_T = io_readArbiterIO_3_1_bits_bankID == 2'h3; // @[operandCollector.scala 303:47]
  wire  _bankArbiterScalar_3_io_in_14_valid_T = io_readArbiterIO_3_2_bits_bankID == 2'h3; // @[operandCollector.scala 303:47]
  wire  _bankArbiterScalar_3_io_in_15_valid_T = io_readArbiterIO_3_3_bits_bankID == 2'h3; // @[operandCollector.scala 303:47]
  RRArbiter bankArbiterScalar_0 ( // @[operandCollector.scala 281:19]
    .clock(bankArbiterScalar_0_clock),
    .io_in_0_valid(bankArbiterScalar_0_io_in_0_valid),
    .io_in_0_bits_rsAddr(bankArbiterScalar_0_io_in_0_bits_rsAddr),
    .io_in_0_bits_rsType(bankArbiterScalar_0_io_in_0_bits_rsType),
    .io_in_1_valid(bankArbiterScalar_0_io_in_1_valid),
    .io_in_1_bits_rsAddr(bankArbiterScalar_0_io_in_1_bits_rsAddr),
    .io_in_1_bits_rsType(bankArbiterScalar_0_io_in_1_bits_rsType),
    .io_in_2_valid(bankArbiterScalar_0_io_in_2_valid),
    .io_in_2_bits_rsAddr(bankArbiterScalar_0_io_in_2_bits_rsAddr),
    .io_in_2_bits_rsType(bankArbiterScalar_0_io_in_2_bits_rsType),
    .io_in_3_valid(bankArbiterScalar_0_io_in_3_valid),
    .io_in_3_bits_rsAddr(bankArbiterScalar_0_io_in_3_bits_rsAddr),
    .io_in_4_valid(bankArbiterScalar_0_io_in_4_valid),
    .io_in_4_bits_rsAddr(bankArbiterScalar_0_io_in_4_bits_rsAddr),
    .io_in_4_bits_rsType(bankArbiterScalar_0_io_in_4_bits_rsType),
    .io_in_5_valid(bankArbiterScalar_0_io_in_5_valid),
    .io_in_5_bits_rsAddr(bankArbiterScalar_0_io_in_5_bits_rsAddr),
    .io_in_5_bits_rsType(bankArbiterScalar_0_io_in_5_bits_rsType),
    .io_in_6_valid(bankArbiterScalar_0_io_in_6_valid),
    .io_in_6_bits_rsAddr(bankArbiterScalar_0_io_in_6_bits_rsAddr),
    .io_in_6_bits_rsType(bankArbiterScalar_0_io_in_6_bits_rsType),
    .io_in_7_valid(bankArbiterScalar_0_io_in_7_valid),
    .io_in_7_bits_rsAddr(bankArbiterScalar_0_io_in_7_bits_rsAddr),
    .io_in_8_valid(bankArbiterScalar_0_io_in_8_valid),
    .io_in_8_bits_rsAddr(bankArbiterScalar_0_io_in_8_bits_rsAddr),
    .io_in_8_bits_rsType(bankArbiterScalar_0_io_in_8_bits_rsType),
    .io_in_9_valid(bankArbiterScalar_0_io_in_9_valid),
    .io_in_9_bits_rsAddr(bankArbiterScalar_0_io_in_9_bits_rsAddr),
    .io_in_9_bits_rsType(bankArbiterScalar_0_io_in_9_bits_rsType),
    .io_in_10_valid(bankArbiterScalar_0_io_in_10_valid),
    .io_in_10_bits_rsAddr(bankArbiterScalar_0_io_in_10_bits_rsAddr),
    .io_in_10_bits_rsType(bankArbiterScalar_0_io_in_10_bits_rsType),
    .io_in_11_valid(bankArbiterScalar_0_io_in_11_valid),
    .io_in_11_bits_rsAddr(bankArbiterScalar_0_io_in_11_bits_rsAddr),
    .io_in_12_valid(bankArbiterScalar_0_io_in_12_valid),
    .io_in_12_bits_rsAddr(bankArbiterScalar_0_io_in_12_bits_rsAddr),
    .io_in_12_bits_rsType(bankArbiterScalar_0_io_in_12_bits_rsType),
    .io_in_13_valid(bankArbiterScalar_0_io_in_13_valid),
    .io_in_13_bits_rsAddr(bankArbiterScalar_0_io_in_13_bits_rsAddr),
    .io_in_13_bits_rsType(bankArbiterScalar_0_io_in_13_bits_rsType),
    .io_in_14_valid(bankArbiterScalar_0_io_in_14_valid),
    .io_in_14_bits_rsAddr(bankArbiterScalar_0_io_in_14_bits_rsAddr),
    .io_in_14_bits_rsType(bankArbiterScalar_0_io_in_14_bits_rsType),
    .io_in_15_valid(bankArbiterScalar_0_io_in_15_valid),
    .io_in_15_bits_rsAddr(bankArbiterScalar_0_io_in_15_bits_rsAddr),
    .io_out_valid(bankArbiterScalar_0_io_out_valid),
    .io_out_bits_rsAddr(bankArbiterScalar_0_io_out_bits_rsAddr),
    .io_out_bits_rsType(bankArbiterScalar_0_io_out_bits_rsType),
    .io_chosen(bankArbiterScalar_0_io_chosen)
  );
  RRArbiter bankArbiterScalar_1 ( // @[operandCollector.scala 281:19]
    .clock(bankArbiterScalar_1_clock),
    .io_in_0_valid(bankArbiterScalar_1_io_in_0_valid),
    .io_in_0_bits_rsAddr(bankArbiterScalar_1_io_in_0_bits_rsAddr),
    .io_in_0_bits_rsType(bankArbiterScalar_1_io_in_0_bits_rsType),
    .io_in_1_valid(bankArbiterScalar_1_io_in_1_valid),
    .io_in_1_bits_rsAddr(bankArbiterScalar_1_io_in_1_bits_rsAddr),
    .io_in_1_bits_rsType(bankArbiterScalar_1_io_in_1_bits_rsType),
    .io_in_2_valid(bankArbiterScalar_1_io_in_2_valid),
    .io_in_2_bits_rsAddr(bankArbiterScalar_1_io_in_2_bits_rsAddr),
    .io_in_2_bits_rsType(bankArbiterScalar_1_io_in_2_bits_rsType),
    .io_in_3_valid(bankArbiterScalar_1_io_in_3_valid),
    .io_in_3_bits_rsAddr(bankArbiterScalar_1_io_in_3_bits_rsAddr),
    .io_in_4_valid(bankArbiterScalar_1_io_in_4_valid),
    .io_in_4_bits_rsAddr(bankArbiterScalar_1_io_in_4_bits_rsAddr),
    .io_in_4_bits_rsType(bankArbiterScalar_1_io_in_4_bits_rsType),
    .io_in_5_valid(bankArbiterScalar_1_io_in_5_valid),
    .io_in_5_bits_rsAddr(bankArbiterScalar_1_io_in_5_bits_rsAddr),
    .io_in_5_bits_rsType(bankArbiterScalar_1_io_in_5_bits_rsType),
    .io_in_6_valid(bankArbiterScalar_1_io_in_6_valid),
    .io_in_6_bits_rsAddr(bankArbiterScalar_1_io_in_6_bits_rsAddr),
    .io_in_6_bits_rsType(bankArbiterScalar_1_io_in_6_bits_rsType),
    .io_in_7_valid(bankArbiterScalar_1_io_in_7_valid),
    .io_in_7_bits_rsAddr(bankArbiterScalar_1_io_in_7_bits_rsAddr),
    .io_in_8_valid(bankArbiterScalar_1_io_in_8_valid),
    .io_in_8_bits_rsAddr(bankArbiterScalar_1_io_in_8_bits_rsAddr),
    .io_in_8_bits_rsType(bankArbiterScalar_1_io_in_8_bits_rsType),
    .io_in_9_valid(bankArbiterScalar_1_io_in_9_valid),
    .io_in_9_bits_rsAddr(bankArbiterScalar_1_io_in_9_bits_rsAddr),
    .io_in_9_bits_rsType(bankArbiterScalar_1_io_in_9_bits_rsType),
    .io_in_10_valid(bankArbiterScalar_1_io_in_10_valid),
    .io_in_10_bits_rsAddr(bankArbiterScalar_1_io_in_10_bits_rsAddr),
    .io_in_10_bits_rsType(bankArbiterScalar_1_io_in_10_bits_rsType),
    .io_in_11_valid(bankArbiterScalar_1_io_in_11_valid),
    .io_in_11_bits_rsAddr(bankArbiterScalar_1_io_in_11_bits_rsAddr),
    .io_in_12_valid(bankArbiterScalar_1_io_in_12_valid),
    .io_in_12_bits_rsAddr(bankArbiterScalar_1_io_in_12_bits_rsAddr),
    .io_in_12_bits_rsType(bankArbiterScalar_1_io_in_12_bits_rsType),
    .io_in_13_valid(bankArbiterScalar_1_io_in_13_valid),
    .io_in_13_bits_rsAddr(bankArbiterScalar_1_io_in_13_bits_rsAddr),
    .io_in_13_bits_rsType(bankArbiterScalar_1_io_in_13_bits_rsType),
    .io_in_14_valid(bankArbiterScalar_1_io_in_14_valid),
    .io_in_14_bits_rsAddr(bankArbiterScalar_1_io_in_14_bits_rsAddr),
    .io_in_14_bits_rsType(bankArbiterScalar_1_io_in_14_bits_rsType),
    .io_in_15_valid(bankArbiterScalar_1_io_in_15_valid),
    .io_in_15_bits_rsAddr(bankArbiterScalar_1_io_in_15_bits_rsAddr),
    .io_out_valid(bankArbiterScalar_1_io_out_valid),
    .io_out_bits_rsAddr(bankArbiterScalar_1_io_out_bits_rsAddr),
    .io_out_bits_rsType(bankArbiterScalar_1_io_out_bits_rsType),
    .io_chosen(bankArbiterScalar_1_io_chosen)
  );
  RRArbiter bankArbiterScalar_2 ( // @[operandCollector.scala 281:19]
    .clock(bankArbiterScalar_2_clock),
    .io_in_0_valid(bankArbiterScalar_2_io_in_0_valid),
    .io_in_0_bits_rsAddr(bankArbiterScalar_2_io_in_0_bits_rsAddr),
    .io_in_0_bits_rsType(bankArbiterScalar_2_io_in_0_bits_rsType),
    .io_in_1_valid(bankArbiterScalar_2_io_in_1_valid),
    .io_in_1_bits_rsAddr(bankArbiterScalar_2_io_in_1_bits_rsAddr),
    .io_in_1_bits_rsType(bankArbiterScalar_2_io_in_1_bits_rsType),
    .io_in_2_valid(bankArbiterScalar_2_io_in_2_valid),
    .io_in_2_bits_rsAddr(bankArbiterScalar_2_io_in_2_bits_rsAddr),
    .io_in_2_bits_rsType(bankArbiterScalar_2_io_in_2_bits_rsType),
    .io_in_3_valid(bankArbiterScalar_2_io_in_3_valid),
    .io_in_3_bits_rsAddr(bankArbiterScalar_2_io_in_3_bits_rsAddr),
    .io_in_4_valid(bankArbiterScalar_2_io_in_4_valid),
    .io_in_4_bits_rsAddr(bankArbiterScalar_2_io_in_4_bits_rsAddr),
    .io_in_4_bits_rsType(bankArbiterScalar_2_io_in_4_bits_rsType),
    .io_in_5_valid(bankArbiterScalar_2_io_in_5_valid),
    .io_in_5_bits_rsAddr(bankArbiterScalar_2_io_in_5_bits_rsAddr),
    .io_in_5_bits_rsType(bankArbiterScalar_2_io_in_5_bits_rsType),
    .io_in_6_valid(bankArbiterScalar_2_io_in_6_valid),
    .io_in_6_bits_rsAddr(bankArbiterScalar_2_io_in_6_bits_rsAddr),
    .io_in_6_bits_rsType(bankArbiterScalar_2_io_in_6_bits_rsType),
    .io_in_7_valid(bankArbiterScalar_2_io_in_7_valid),
    .io_in_7_bits_rsAddr(bankArbiterScalar_2_io_in_7_bits_rsAddr),
    .io_in_8_valid(bankArbiterScalar_2_io_in_8_valid),
    .io_in_8_bits_rsAddr(bankArbiterScalar_2_io_in_8_bits_rsAddr),
    .io_in_8_bits_rsType(bankArbiterScalar_2_io_in_8_bits_rsType),
    .io_in_9_valid(bankArbiterScalar_2_io_in_9_valid),
    .io_in_9_bits_rsAddr(bankArbiterScalar_2_io_in_9_bits_rsAddr),
    .io_in_9_bits_rsType(bankArbiterScalar_2_io_in_9_bits_rsType),
    .io_in_10_valid(bankArbiterScalar_2_io_in_10_valid),
    .io_in_10_bits_rsAddr(bankArbiterScalar_2_io_in_10_bits_rsAddr),
    .io_in_10_bits_rsType(bankArbiterScalar_2_io_in_10_bits_rsType),
    .io_in_11_valid(bankArbiterScalar_2_io_in_11_valid),
    .io_in_11_bits_rsAddr(bankArbiterScalar_2_io_in_11_bits_rsAddr),
    .io_in_12_valid(bankArbiterScalar_2_io_in_12_valid),
    .io_in_12_bits_rsAddr(bankArbiterScalar_2_io_in_12_bits_rsAddr),
    .io_in_12_bits_rsType(bankArbiterScalar_2_io_in_12_bits_rsType),
    .io_in_13_valid(bankArbiterScalar_2_io_in_13_valid),
    .io_in_13_bits_rsAddr(bankArbiterScalar_2_io_in_13_bits_rsAddr),
    .io_in_13_bits_rsType(bankArbiterScalar_2_io_in_13_bits_rsType),
    .io_in_14_valid(bankArbiterScalar_2_io_in_14_valid),
    .io_in_14_bits_rsAddr(bankArbiterScalar_2_io_in_14_bits_rsAddr),
    .io_in_14_bits_rsType(bankArbiterScalar_2_io_in_14_bits_rsType),
    .io_in_15_valid(bankArbiterScalar_2_io_in_15_valid),
    .io_in_15_bits_rsAddr(bankArbiterScalar_2_io_in_15_bits_rsAddr),
    .io_out_valid(bankArbiterScalar_2_io_out_valid),
    .io_out_bits_rsAddr(bankArbiterScalar_2_io_out_bits_rsAddr),
    .io_out_bits_rsType(bankArbiterScalar_2_io_out_bits_rsType),
    .io_chosen(bankArbiterScalar_2_io_chosen)
  );
  RRArbiter bankArbiterScalar_3 ( // @[operandCollector.scala 281:19]
    .clock(bankArbiterScalar_3_clock),
    .io_in_0_valid(bankArbiterScalar_3_io_in_0_valid),
    .io_in_0_bits_rsAddr(bankArbiterScalar_3_io_in_0_bits_rsAddr),
    .io_in_0_bits_rsType(bankArbiterScalar_3_io_in_0_bits_rsType),
    .io_in_1_valid(bankArbiterScalar_3_io_in_1_valid),
    .io_in_1_bits_rsAddr(bankArbiterScalar_3_io_in_1_bits_rsAddr),
    .io_in_1_bits_rsType(bankArbiterScalar_3_io_in_1_bits_rsType),
    .io_in_2_valid(bankArbiterScalar_3_io_in_2_valid),
    .io_in_2_bits_rsAddr(bankArbiterScalar_3_io_in_2_bits_rsAddr),
    .io_in_2_bits_rsType(bankArbiterScalar_3_io_in_2_bits_rsType),
    .io_in_3_valid(bankArbiterScalar_3_io_in_3_valid),
    .io_in_3_bits_rsAddr(bankArbiterScalar_3_io_in_3_bits_rsAddr),
    .io_in_4_valid(bankArbiterScalar_3_io_in_4_valid),
    .io_in_4_bits_rsAddr(bankArbiterScalar_3_io_in_4_bits_rsAddr),
    .io_in_4_bits_rsType(bankArbiterScalar_3_io_in_4_bits_rsType),
    .io_in_5_valid(bankArbiterScalar_3_io_in_5_valid),
    .io_in_5_bits_rsAddr(bankArbiterScalar_3_io_in_5_bits_rsAddr),
    .io_in_5_bits_rsType(bankArbiterScalar_3_io_in_5_bits_rsType),
    .io_in_6_valid(bankArbiterScalar_3_io_in_6_valid),
    .io_in_6_bits_rsAddr(bankArbiterScalar_3_io_in_6_bits_rsAddr),
    .io_in_6_bits_rsType(bankArbiterScalar_3_io_in_6_bits_rsType),
    .io_in_7_valid(bankArbiterScalar_3_io_in_7_valid),
    .io_in_7_bits_rsAddr(bankArbiterScalar_3_io_in_7_bits_rsAddr),
    .io_in_8_valid(bankArbiterScalar_3_io_in_8_valid),
    .io_in_8_bits_rsAddr(bankArbiterScalar_3_io_in_8_bits_rsAddr),
    .io_in_8_bits_rsType(bankArbiterScalar_3_io_in_8_bits_rsType),
    .io_in_9_valid(bankArbiterScalar_3_io_in_9_valid),
    .io_in_9_bits_rsAddr(bankArbiterScalar_3_io_in_9_bits_rsAddr),
    .io_in_9_bits_rsType(bankArbiterScalar_3_io_in_9_bits_rsType),
    .io_in_10_valid(bankArbiterScalar_3_io_in_10_valid),
    .io_in_10_bits_rsAddr(bankArbiterScalar_3_io_in_10_bits_rsAddr),
    .io_in_10_bits_rsType(bankArbiterScalar_3_io_in_10_bits_rsType),
    .io_in_11_valid(bankArbiterScalar_3_io_in_11_valid),
    .io_in_11_bits_rsAddr(bankArbiterScalar_3_io_in_11_bits_rsAddr),
    .io_in_12_valid(bankArbiterScalar_3_io_in_12_valid),
    .io_in_12_bits_rsAddr(bankArbiterScalar_3_io_in_12_bits_rsAddr),
    .io_in_12_bits_rsType(bankArbiterScalar_3_io_in_12_bits_rsType),
    .io_in_13_valid(bankArbiterScalar_3_io_in_13_valid),
    .io_in_13_bits_rsAddr(bankArbiterScalar_3_io_in_13_bits_rsAddr),
    .io_in_13_bits_rsType(bankArbiterScalar_3_io_in_13_bits_rsType),
    .io_in_14_valid(bankArbiterScalar_3_io_in_14_valid),
    .io_in_14_bits_rsAddr(bankArbiterScalar_3_io_in_14_bits_rsAddr),
    .io_in_14_bits_rsType(bankArbiterScalar_3_io_in_14_bits_rsType),
    .io_in_15_valid(bankArbiterScalar_3_io_in_15_valid),
    .io_in_15_bits_rsAddr(bankArbiterScalar_3_io_in_15_bits_rsAddr),
    .io_out_valid(bankArbiterScalar_3_io_out_valid),
    .io_out_bits_rsAddr(bankArbiterScalar_3_io_out_bits_rsAddr),
    .io_out_bits_rsType(bankArbiterScalar_3_io_out_bits_rsType),
    .io_chosen(bankArbiterScalar_3_io_chosen)
  );
  assign io_readArbiterOut_0_valid = bankArbiterScalar_0_io_out_valid; // @[operandCollector.scala 310:26]
  assign io_readArbiterOut_0_bits_rsAddr = bankArbiterScalar_0_io_out_bits_rsAddr; // @[operandCollector.scala 310:26]
  assign io_readArbiterOut_0_bits_rsType = bankArbiterScalar_0_io_out_bits_rsType; // @[operandCollector.scala 310:26]
  assign io_readArbiterOut_1_valid = bankArbiterScalar_1_io_out_valid; // @[operandCollector.scala 310:26]
  assign io_readArbiterOut_1_bits_rsAddr = bankArbiterScalar_1_io_out_bits_rsAddr; // @[operandCollector.scala 310:26]
  assign io_readArbiterOut_1_bits_rsType = bankArbiterScalar_1_io_out_bits_rsType; // @[operandCollector.scala 310:26]
  assign io_readArbiterOut_2_valid = bankArbiterScalar_2_io_out_valid; // @[operandCollector.scala 310:26]
  assign io_readArbiterOut_2_bits_rsAddr = bankArbiterScalar_2_io_out_bits_rsAddr; // @[operandCollector.scala 310:26]
  assign io_readArbiterOut_2_bits_rsType = bankArbiterScalar_2_io_out_bits_rsType; // @[operandCollector.scala 310:26]
  assign io_readArbiterOut_3_valid = bankArbiterScalar_3_io_out_valid; // @[operandCollector.scala 310:26]
  assign io_readArbiterOut_3_bits_rsAddr = bankArbiterScalar_3_io_out_bits_rsAddr; // @[operandCollector.scala 310:26]
  assign io_readArbiterOut_3_bits_rsType = bankArbiterScalar_3_io_out_bits_rsType; // @[operandCollector.scala 310:26]
  assign io_readchosen_0 = bankArbiterScalar_0_io_chosen; // @[operandCollector.scala 311:22]
  assign io_readchosen_1 = bankArbiterScalar_1_io_chosen; // @[operandCollector.scala 311:22]
  assign io_readchosen_2 = bankArbiterScalar_2_io_chosen; // @[operandCollector.scala 311:22]
  assign io_readchosen_3 = bankArbiterScalar_3_io_chosen; // @[operandCollector.scala 311:22]
  assign bankArbiterScalar_0_clock = clock;
  assign bankArbiterScalar_0_io_in_0_valid = io_readArbiterIO_0_0_valid & _bankArbiterScalar_0_io_in_0_valid_T; // @[operandCollector.scala 302:81]
  assign bankArbiterScalar_0_io_in_0_bits_rsAddr = io_readArbiterIO_0_0_bits_rsAddr; // @[operandCollector.scala 293:43]
  assign bankArbiterScalar_0_io_in_0_bits_rsType = io_readArbiterIO_0_0_bits_rsType; // @[operandCollector.scala 293:43]
  assign bankArbiterScalar_0_io_in_1_valid = io_readArbiterIO_0_1_valid & _bankArbiterScalar_0_io_in_1_valid_T; // @[operandCollector.scala 302:81]
  assign bankArbiterScalar_0_io_in_1_bits_rsAddr = io_readArbiterIO_0_1_bits_rsAddr; // @[operandCollector.scala 293:43]
  assign bankArbiterScalar_0_io_in_1_bits_rsType = io_readArbiterIO_0_1_bits_rsType; // @[operandCollector.scala 293:43]
  assign bankArbiterScalar_0_io_in_2_valid = io_readArbiterIO_0_2_valid & _bankArbiterScalar_0_io_in_2_valid_T; // @[operandCollector.scala 302:81]
  assign bankArbiterScalar_0_io_in_2_bits_rsAddr = io_readArbiterIO_0_2_bits_rsAddr; // @[operandCollector.scala 293:43]
  assign bankArbiterScalar_0_io_in_2_bits_rsType = io_readArbiterIO_0_2_bits_rsType; // @[operandCollector.scala 293:43]
  assign bankArbiterScalar_0_io_in_3_valid = io_readArbiterIO_0_3_valid & _bankArbiterScalar_0_io_in_3_valid_T; // @[operandCollector.scala 302:81]
  assign bankArbiterScalar_0_io_in_3_bits_rsAddr = io_readArbiterIO_0_3_bits_rsAddr; // @[operandCollector.scala 293:43]
  assign bankArbiterScalar_0_io_in_4_valid = io_readArbiterIO_1_0_valid & _bankArbiterScalar_0_io_in_4_valid_T; // @[operandCollector.scala 302:81]
  assign bankArbiterScalar_0_io_in_4_bits_rsAddr = io_readArbiterIO_1_0_bits_rsAddr; // @[operandCollector.scala 293:43]
  assign bankArbiterScalar_0_io_in_4_bits_rsType = io_readArbiterIO_1_0_bits_rsType; // @[operandCollector.scala 293:43]
  assign bankArbiterScalar_0_io_in_5_valid = io_readArbiterIO_1_1_valid & _bankArbiterScalar_0_io_in_5_valid_T; // @[operandCollector.scala 302:81]
  assign bankArbiterScalar_0_io_in_5_bits_rsAddr = io_readArbiterIO_1_1_bits_rsAddr; // @[operandCollector.scala 293:43]
  assign bankArbiterScalar_0_io_in_5_bits_rsType = io_readArbiterIO_1_1_bits_rsType; // @[operandCollector.scala 293:43]
  assign bankArbiterScalar_0_io_in_6_valid = io_readArbiterIO_1_2_valid & _bankArbiterScalar_0_io_in_6_valid_T; // @[operandCollector.scala 302:81]
  assign bankArbiterScalar_0_io_in_6_bits_rsAddr = io_readArbiterIO_1_2_bits_rsAddr; // @[operandCollector.scala 293:43]
  assign bankArbiterScalar_0_io_in_6_bits_rsType = io_readArbiterIO_1_2_bits_rsType; // @[operandCollector.scala 293:43]
  assign bankArbiterScalar_0_io_in_7_valid = io_readArbiterIO_1_3_valid & _bankArbiterScalar_0_io_in_7_valid_T; // @[operandCollector.scala 302:81]
  assign bankArbiterScalar_0_io_in_7_bits_rsAddr = io_readArbiterIO_1_3_bits_rsAddr; // @[operandCollector.scala 293:43]
  assign bankArbiterScalar_0_io_in_8_valid = io_readArbiterIO_2_0_valid & _bankArbiterScalar_0_io_in_8_valid_T; // @[operandCollector.scala 302:81]
  assign bankArbiterScalar_0_io_in_8_bits_rsAddr = io_readArbiterIO_2_0_bits_rsAddr; // @[operandCollector.scala 293:43]
  assign bankArbiterScalar_0_io_in_8_bits_rsType = io_readArbiterIO_2_0_bits_rsType; // @[operandCollector.scala 293:43]
  assign bankArbiterScalar_0_io_in_9_valid = io_readArbiterIO_2_1_valid & _bankArbiterScalar_0_io_in_9_valid_T; // @[operandCollector.scala 302:81]
  assign bankArbiterScalar_0_io_in_9_bits_rsAddr = io_readArbiterIO_2_1_bits_rsAddr; // @[operandCollector.scala 293:43]
  assign bankArbiterScalar_0_io_in_9_bits_rsType = io_readArbiterIO_2_1_bits_rsType; // @[operandCollector.scala 293:43]
  assign bankArbiterScalar_0_io_in_10_valid = io_readArbiterIO_2_2_valid & _bankArbiterScalar_0_io_in_10_valid_T; // @[operandCollector.scala 302:81]
  assign bankArbiterScalar_0_io_in_10_bits_rsAddr = io_readArbiterIO_2_2_bits_rsAddr; // @[operandCollector.scala 293:43]
  assign bankArbiterScalar_0_io_in_10_bits_rsType = io_readArbiterIO_2_2_bits_rsType; // @[operandCollector.scala 293:43]
  assign bankArbiterScalar_0_io_in_11_valid = io_readArbiterIO_2_3_valid & _bankArbiterScalar_0_io_in_11_valid_T; // @[operandCollector.scala 302:81]
  assign bankArbiterScalar_0_io_in_11_bits_rsAddr = io_readArbiterIO_2_3_bits_rsAddr; // @[operandCollector.scala 293:43]
  assign bankArbiterScalar_0_io_in_12_valid = io_readArbiterIO_3_0_valid & _bankArbiterScalar_0_io_in_12_valid_T; // @[operandCollector.scala 302:81]
  assign bankArbiterScalar_0_io_in_12_bits_rsAddr = io_readArbiterIO_3_0_bits_rsAddr; // @[operandCollector.scala 293:43]
  assign bankArbiterScalar_0_io_in_12_bits_rsType = io_readArbiterIO_3_0_bits_rsType; // @[operandCollector.scala 293:43]
  assign bankArbiterScalar_0_io_in_13_valid = io_readArbiterIO_3_1_valid & _bankArbiterScalar_0_io_in_13_valid_T; // @[operandCollector.scala 302:81]
  assign bankArbiterScalar_0_io_in_13_bits_rsAddr = io_readArbiterIO_3_1_bits_rsAddr; // @[operandCollector.scala 293:43]
  assign bankArbiterScalar_0_io_in_13_bits_rsType = io_readArbiterIO_3_1_bits_rsType; // @[operandCollector.scala 293:43]
  assign bankArbiterScalar_0_io_in_14_valid = io_readArbiterIO_3_2_valid & _bankArbiterScalar_0_io_in_14_valid_T; // @[operandCollector.scala 302:81]
  assign bankArbiterScalar_0_io_in_14_bits_rsAddr = io_readArbiterIO_3_2_bits_rsAddr; // @[operandCollector.scala 293:43]
  assign bankArbiterScalar_0_io_in_14_bits_rsType = io_readArbiterIO_3_2_bits_rsType; // @[operandCollector.scala 293:43]
  assign bankArbiterScalar_0_io_in_15_valid = io_readArbiterIO_3_3_valid & _bankArbiterScalar_0_io_in_15_valid_T; // @[operandCollector.scala 302:81]
  assign bankArbiterScalar_0_io_in_15_bits_rsAddr = io_readArbiterIO_3_3_bits_rsAddr; // @[operandCollector.scala 293:43]
  assign bankArbiterScalar_1_clock = clock;
  assign bankArbiterScalar_1_io_in_0_valid = io_readArbiterIO_0_0_valid & _bankArbiterScalar_1_io_in_0_valid_T; // @[operandCollector.scala 302:81]
  assign bankArbiterScalar_1_io_in_0_bits_rsAddr = io_readArbiterIO_0_0_bits_rsAddr; // @[operandCollector.scala 293:43]
  assign bankArbiterScalar_1_io_in_0_bits_rsType = io_readArbiterIO_0_0_bits_rsType; // @[operandCollector.scala 293:43]
  assign bankArbiterScalar_1_io_in_1_valid = io_readArbiterIO_0_1_valid & _bankArbiterScalar_1_io_in_1_valid_T; // @[operandCollector.scala 302:81]
  assign bankArbiterScalar_1_io_in_1_bits_rsAddr = io_readArbiterIO_0_1_bits_rsAddr; // @[operandCollector.scala 293:43]
  assign bankArbiterScalar_1_io_in_1_bits_rsType = io_readArbiterIO_0_1_bits_rsType; // @[operandCollector.scala 293:43]
  assign bankArbiterScalar_1_io_in_2_valid = io_readArbiterIO_0_2_valid & _bankArbiterScalar_1_io_in_2_valid_T; // @[operandCollector.scala 302:81]
  assign bankArbiterScalar_1_io_in_2_bits_rsAddr = io_readArbiterIO_0_2_bits_rsAddr; // @[operandCollector.scala 293:43]
  assign bankArbiterScalar_1_io_in_2_bits_rsType = io_readArbiterIO_0_2_bits_rsType; // @[operandCollector.scala 293:43]
  assign bankArbiterScalar_1_io_in_3_valid = io_readArbiterIO_0_3_valid & _bankArbiterScalar_1_io_in_3_valid_T; // @[operandCollector.scala 302:81]
  assign bankArbiterScalar_1_io_in_3_bits_rsAddr = io_readArbiterIO_0_3_bits_rsAddr; // @[operandCollector.scala 293:43]
  assign bankArbiterScalar_1_io_in_4_valid = io_readArbiterIO_1_0_valid & _bankArbiterScalar_1_io_in_4_valid_T; // @[operandCollector.scala 302:81]
  assign bankArbiterScalar_1_io_in_4_bits_rsAddr = io_readArbiterIO_1_0_bits_rsAddr; // @[operandCollector.scala 293:43]
  assign bankArbiterScalar_1_io_in_4_bits_rsType = io_readArbiterIO_1_0_bits_rsType; // @[operandCollector.scala 293:43]
  assign bankArbiterScalar_1_io_in_5_valid = io_readArbiterIO_1_1_valid & _bankArbiterScalar_1_io_in_5_valid_T; // @[operandCollector.scala 302:81]
  assign bankArbiterScalar_1_io_in_5_bits_rsAddr = io_readArbiterIO_1_1_bits_rsAddr; // @[operandCollector.scala 293:43]
  assign bankArbiterScalar_1_io_in_5_bits_rsType = io_readArbiterIO_1_1_bits_rsType; // @[operandCollector.scala 293:43]
  assign bankArbiterScalar_1_io_in_6_valid = io_readArbiterIO_1_2_valid & _bankArbiterScalar_1_io_in_6_valid_T; // @[operandCollector.scala 302:81]
  assign bankArbiterScalar_1_io_in_6_bits_rsAddr = io_readArbiterIO_1_2_bits_rsAddr; // @[operandCollector.scala 293:43]
  assign bankArbiterScalar_1_io_in_6_bits_rsType = io_readArbiterIO_1_2_bits_rsType; // @[operandCollector.scala 293:43]
  assign bankArbiterScalar_1_io_in_7_valid = io_readArbiterIO_1_3_valid & _bankArbiterScalar_1_io_in_7_valid_T; // @[operandCollector.scala 302:81]
  assign bankArbiterScalar_1_io_in_7_bits_rsAddr = io_readArbiterIO_1_3_bits_rsAddr; // @[operandCollector.scala 293:43]
  assign bankArbiterScalar_1_io_in_8_valid = io_readArbiterIO_2_0_valid & _bankArbiterScalar_1_io_in_8_valid_T; // @[operandCollector.scala 302:81]
  assign bankArbiterScalar_1_io_in_8_bits_rsAddr = io_readArbiterIO_2_0_bits_rsAddr; // @[operandCollector.scala 293:43]
  assign bankArbiterScalar_1_io_in_8_bits_rsType = io_readArbiterIO_2_0_bits_rsType; // @[operandCollector.scala 293:43]
  assign bankArbiterScalar_1_io_in_9_valid = io_readArbiterIO_2_1_valid & _bankArbiterScalar_1_io_in_9_valid_T; // @[operandCollector.scala 302:81]
  assign bankArbiterScalar_1_io_in_9_bits_rsAddr = io_readArbiterIO_2_1_bits_rsAddr; // @[operandCollector.scala 293:43]
  assign bankArbiterScalar_1_io_in_9_bits_rsType = io_readArbiterIO_2_1_bits_rsType; // @[operandCollector.scala 293:43]
  assign bankArbiterScalar_1_io_in_10_valid = io_readArbiterIO_2_2_valid & _bankArbiterScalar_1_io_in_10_valid_T; // @[operandCollector.scala 302:81]
  assign bankArbiterScalar_1_io_in_10_bits_rsAddr = io_readArbiterIO_2_2_bits_rsAddr; // @[operandCollector.scala 293:43]
  assign bankArbiterScalar_1_io_in_10_bits_rsType = io_readArbiterIO_2_2_bits_rsType; // @[operandCollector.scala 293:43]
  assign bankArbiterScalar_1_io_in_11_valid = io_readArbiterIO_2_3_valid & _bankArbiterScalar_1_io_in_11_valid_T; // @[operandCollector.scala 302:81]
  assign bankArbiterScalar_1_io_in_11_bits_rsAddr = io_readArbiterIO_2_3_bits_rsAddr; // @[operandCollector.scala 293:43]
  assign bankArbiterScalar_1_io_in_12_valid = io_readArbiterIO_3_0_valid & _bankArbiterScalar_1_io_in_12_valid_T; // @[operandCollector.scala 302:81]
  assign bankArbiterScalar_1_io_in_12_bits_rsAddr = io_readArbiterIO_3_0_bits_rsAddr; // @[operandCollector.scala 293:43]
  assign bankArbiterScalar_1_io_in_12_bits_rsType = io_readArbiterIO_3_0_bits_rsType; // @[operandCollector.scala 293:43]
  assign bankArbiterScalar_1_io_in_13_valid = io_readArbiterIO_3_1_valid & _bankArbiterScalar_1_io_in_13_valid_T; // @[operandCollector.scala 302:81]
  assign bankArbiterScalar_1_io_in_13_bits_rsAddr = io_readArbiterIO_3_1_bits_rsAddr; // @[operandCollector.scala 293:43]
  assign bankArbiterScalar_1_io_in_13_bits_rsType = io_readArbiterIO_3_1_bits_rsType; // @[operandCollector.scala 293:43]
  assign bankArbiterScalar_1_io_in_14_valid = io_readArbiterIO_3_2_valid & _bankArbiterScalar_1_io_in_14_valid_T; // @[operandCollector.scala 302:81]
  assign bankArbiterScalar_1_io_in_14_bits_rsAddr = io_readArbiterIO_3_2_bits_rsAddr; // @[operandCollector.scala 293:43]
  assign bankArbiterScalar_1_io_in_14_bits_rsType = io_readArbiterIO_3_2_bits_rsType; // @[operandCollector.scala 293:43]
  assign bankArbiterScalar_1_io_in_15_valid = io_readArbiterIO_3_3_valid & _bankArbiterScalar_1_io_in_15_valid_T; // @[operandCollector.scala 302:81]
  assign bankArbiterScalar_1_io_in_15_bits_rsAddr = io_readArbiterIO_3_3_bits_rsAddr; // @[operandCollector.scala 293:43]
  assign bankArbiterScalar_2_clock = clock;
  assign bankArbiterScalar_2_io_in_0_valid = io_readArbiterIO_0_0_valid & _bankArbiterScalar_2_io_in_0_valid_T; // @[operandCollector.scala 302:81]
  assign bankArbiterScalar_2_io_in_0_bits_rsAddr = io_readArbiterIO_0_0_bits_rsAddr; // @[operandCollector.scala 293:43]
  assign bankArbiterScalar_2_io_in_0_bits_rsType = io_readArbiterIO_0_0_bits_rsType; // @[operandCollector.scala 293:43]
  assign bankArbiterScalar_2_io_in_1_valid = io_readArbiterIO_0_1_valid & _bankArbiterScalar_2_io_in_1_valid_T; // @[operandCollector.scala 302:81]
  assign bankArbiterScalar_2_io_in_1_bits_rsAddr = io_readArbiterIO_0_1_bits_rsAddr; // @[operandCollector.scala 293:43]
  assign bankArbiterScalar_2_io_in_1_bits_rsType = io_readArbiterIO_0_1_bits_rsType; // @[operandCollector.scala 293:43]
  assign bankArbiterScalar_2_io_in_2_valid = io_readArbiterIO_0_2_valid & _bankArbiterScalar_2_io_in_2_valid_T; // @[operandCollector.scala 302:81]
  assign bankArbiterScalar_2_io_in_2_bits_rsAddr = io_readArbiterIO_0_2_bits_rsAddr; // @[operandCollector.scala 293:43]
  assign bankArbiterScalar_2_io_in_2_bits_rsType = io_readArbiterIO_0_2_bits_rsType; // @[operandCollector.scala 293:43]
  assign bankArbiterScalar_2_io_in_3_valid = io_readArbiterIO_0_3_valid & _bankArbiterScalar_2_io_in_3_valid_T; // @[operandCollector.scala 302:81]
  assign bankArbiterScalar_2_io_in_3_bits_rsAddr = io_readArbiterIO_0_3_bits_rsAddr; // @[operandCollector.scala 293:43]
  assign bankArbiterScalar_2_io_in_4_valid = io_readArbiterIO_1_0_valid & _bankArbiterScalar_2_io_in_4_valid_T; // @[operandCollector.scala 302:81]
  assign bankArbiterScalar_2_io_in_4_bits_rsAddr = io_readArbiterIO_1_0_bits_rsAddr; // @[operandCollector.scala 293:43]
  assign bankArbiterScalar_2_io_in_4_bits_rsType = io_readArbiterIO_1_0_bits_rsType; // @[operandCollector.scala 293:43]
  assign bankArbiterScalar_2_io_in_5_valid = io_readArbiterIO_1_1_valid & _bankArbiterScalar_2_io_in_5_valid_T; // @[operandCollector.scala 302:81]
  assign bankArbiterScalar_2_io_in_5_bits_rsAddr = io_readArbiterIO_1_1_bits_rsAddr; // @[operandCollector.scala 293:43]
  assign bankArbiterScalar_2_io_in_5_bits_rsType = io_readArbiterIO_1_1_bits_rsType; // @[operandCollector.scala 293:43]
  assign bankArbiterScalar_2_io_in_6_valid = io_readArbiterIO_1_2_valid & _bankArbiterScalar_2_io_in_6_valid_T; // @[operandCollector.scala 302:81]
  assign bankArbiterScalar_2_io_in_6_bits_rsAddr = io_readArbiterIO_1_2_bits_rsAddr; // @[operandCollector.scala 293:43]
  assign bankArbiterScalar_2_io_in_6_bits_rsType = io_readArbiterIO_1_2_bits_rsType; // @[operandCollector.scala 293:43]
  assign bankArbiterScalar_2_io_in_7_valid = io_readArbiterIO_1_3_valid & _bankArbiterScalar_2_io_in_7_valid_T; // @[operandCollector.scala 302:81]
  assign bankArbiterScalar_2_io_in_7_bits_rsAddr = io_readArbiterIO_1_3_bits_rsAddr; // @[operandCollector.scala 293:43]
  assign bankArbiterScalar_2_io_in_8_valid = io_readArbiterIO_2_0_valid & _bankArbiterScalar_2_io_in_8_valid_T; // @[operandCollector.scala 302:81]
  assign bankArbiterScalar_2_io_in_8_bits_rsAddr = io_readArbiterIO_2_0_bits_rsAddr; // @[operandCollector.scala 293:43]
  assign bankArbiterScalar_2_io_in_8_bits_rsType = io_readArbiterIO_2_0_bits_rsType; // @[operandCollector.scala 293:43]
  assign bankArbiterScalar_2_io_in_9_valid = io_readArbiterIO_2_1_valid & _bankArbiterScalar_2_io_in_9_valid_T; // @[operandCollector.scala 302:81]
  assign bankArbiterScalar_2_io_in_9_bits_rsAddr = io_readArbiterIO_2_1_bits_rsAddr; // @[operandCollector.scala 293:43]
  assign bankArbiterScalar_2_io_in_9_bits_rsType = io_readArbiterIO_2_1_bits_rsType; // @[operandCollector.scala 293:43]
  assign bankArbiterScalar_2_io_in_10_valid = io_readArbiterIO_2_2_valid & _bankArbiterScalar_2_io_in_10_valid_T; // @[operandCollector.scala 302:81]
  assign bankArbiterScalar_2_io_in_10_bits_rsAddr = io_readArbiterIO_2_2_bits_rsAddr; // @[operandCollector.scala 293:43]
  assign bankArbiterScalar_2_io_in_10_bits_rsType = io_readArbiterIO_2_2_bits_rsType; // @[operandCollector.scala 293:43]
  assign bankArbiterScalar_2_io_in_11_valid = io_readArbiterIO_2_3_valid & _bankArbiterScalar_2_io_in_11_valid_T; // @[operandCollector.scala 302:81]
  assign bankArbiterScalar_2_io_in_11_bits_rsAddr = io_readArbiterIO_2_3_bits_rsAddr; // @[operandCollector.scala 293:43]
  assign bankArbiterScalar_2_io_in_12_valid = io_readArbiterIO_3_0_valid & _bankArbiterScalar_2_io_in_12_valid_T; // @[operandCollector.scala 302:81]
  assign bankArbiterScalar_2_io_in_12_bits_rsAddr = io_readArbiterIO_3_0_bits_rsAddr; // @[operandCollector.scala 293:43]
  assign bankArbiterScalar_2_io_in_12_bits_rsType = io_readArbiterIO_3_0_bits_rsType; // @[operandCollector.scala 293:43]
  assign bankArbiterScalar_2_io_in_13_valid = io_readArbiterIO_3_1_valid & _bankArbiterScalar_2_io_in_13_valid_T; // @[operandCollector.scala 302:81]
  assign bankArbiterScalar_2_io_in_13_bits_rsAddr = io_readArbiterIO_3_1_bits_rsAddr; // @[operandCollector.scala 293:43]
  assign bankArbiterScalar_2_io_in_13_bits_rsType = io_readArbiterIO_3_1_bits_rsType; // @[operandCollector.scala 293:43]
  assign bankArbiterScalar_2_io_in_14_valid = io_readArbiterIO_3_2_valid & _bankArbiterScalar_2_io_in_14_valid_T; // @[operandCollector.scala 302:81]
  assign bankArbiterScalar_2_io_in_14_bits_rsAddr = io_readArbiterIO_3_2_bits_rsAddr; // @[operandCollector.scala 293:43]
  assign bankArbiterScalar_2_io_in_14_bits_rsType = io_readArbiterIO_3_2_bits_rsType; // @[operandCollector.scala 293:43]
  assign bankArbiterScalar_2_io_in_15_valid = io_readArbiterIO_3_3_valid & _bankArbiterScalar_2_io_in_15_valid_T; // @[operandCollector.scala 302:81]
  assign bankArbiterScalar_2_io_in_15_bits_rsAddr = io_readArbiterIO_3_3_bits_rsAddr; // @[operandCollector.scala 293:43]
  assign bankArbiterScalar_3_clock = clock;
  assign bankArbiterScalar_3_io_in_0_valid = io_readArbiterIO_0_0_valid & _bankArbiterScalar_3_io_in_0_valid_T; // @[operandCollector.scala 302:81]
  assign bankArbiterScalar_3_io_in_0_bits_rsAddr = io_readArbiterIO_0_0_bits_rsAddr; // @[operandCollector.scala 293:43]
  assign bankArbiterScalar_3_io_in_0_bits_rsType = io_readArbiterIO_0_0_bits_rsType; // @[operandCollector.scala 293:43]
  assign bankArbiterScalar_3_io_in_1_valid = io_readArbiterIO_0_1_valid & _bankArbiterScalar_3_io_in_1_valid_T; // @[operandCollector.scala 302:81]
  assign bankArbiterScalar_3_io_in_1_bits_rsAddr = io_readArbiterIO_0_1_bits_rsAddr; // @[operandCollector.scala 293:43]
  assign bankArbiterScalar_3_io_in_1_bits_rsType = io_readArbiterIO_0_1_bits_rsType; // @[operandCollector.scala 293:43]
  assign bankArbiterScalar_3_io_in_2_valid = io_readArbiterIO_0_2_valid & _bankArbiterScalar_3_io_in_2_valid_T; // @[operandCollector.scala 302:81]
  assign bankArbiterScalar_3_io_in_2_bits_rsAddr = io_readArbiterIO_0_2_bits_rsAddr; // @[operandCollector.scala 293:43]
  assign bankArbiterScalar_3_io_in_2_bits_rsType = io_readArbiterIO_0_2_bits_rsType; // @[operandCollector.scala 293:43]
  assign bankArbiterScalar_3_io_in_3_valid = io_readArbiterIO_0_3_valid & _bankArbiterScalar_3_io_in_3_valid_T; // @[operandCollector.scala 302:81]
  assign bankArbiterScalar_3_io_in_3_bits_rsAddr = io_readArbiterIO_0_3_bits_rsAddr; // @[operandCollector.scala 293:43]
  assign bankArbiterScalar_3_io_in_4_valid = io_readArbiterIO_1_0_valid & _bankArbiterScalar_3_io_in_4_valid_T; // @[operandCollector.scala 302:81]
  assign bankArbiterScalar_3_io_in_4_bits_rsAddr = io_readArbiterIO_1_0_bits_rsAddr; // @[operandCollector.scala 293:43]
  assign bankArbiterScalar_3_io_in_4_bits_rsType = io_readArbiterIO_1_0_bits_rsType; // @[operandCollector.scala 293:43]
  assign bankArbiterScalar_3_io_in_5_valid = io_readArbiterIO_1_1_valid & _bankArbiterScalar_3_io_in_5_valid_T; // @[operandCollector.scala 302:81]
  assign bankArbiterScalar_3_io_in_5_bits_rsAddr = io_readArbiterIO_1_1_bits_rsAddr; // @[operandCollector.scala 293:43]
  assign bankArbiterScalar_3_io_in_5_bits_rsType = io_readArbiterIO_1_1_bits_rsType; // @[operandCollector.scala 293:43]
  assign bankArbiterScalar_3_io_in_6_valid = io_readArbiterIO_1_2_valid & _bankArbiterScalar_3_io_in_6_valid_T; // @[operandCollector.scala 302:81]
  assign bankArbiterScalar_3_io_in_6_bits_rsAddr = io_readArbiterIO_1_2_bits_rsAddr; // @[operandCollector.scala 293:43]
  assign bankArbiterScalar_3_io_in_6_bits_rsType = io_readArbiterIO_1_2_bits_rsType; // @[operandCollector.scala 293:43]
  assign bankArbiterScalar_3_io_in_7_valid = io_readArbiterIO_1_3_valid & _bankArbiterScalar_3_io_in_7_valid_T; // @[operandCollector.scala 302:81]
  assign bankArbiterScalar_3_io_in_7_bits_rsAddr = io_readArbiterIO_1_3_bits_rsAddr; // @[operandCollector.scala 293:43]
  assign bankArbiterScalar_3_io_in_8_valid = io_readArbiterIO_2_0_valid & _bankArbiterScalar_3_io_in_8_valid_T; // @[operandCollector.scala 302:81]
  assign bankArbiterScalar_3_io_in_8_bits_rsAddr = io_readArbiterIO_2_0_bits_rsAddr; // @[operandCollector.scala 293:43]
  assign bankArbiterScalar_3_io_in_8_bits_rsType = io_readArbiterIO_2_0_bits_rsType; // @[operandCollector.scala 293:43]
  assign bankArbiterScalar_3_io_in_9_valid = io_readArbiterIO_2_1_valid & _bankArbiterScalar_3_io_in_9_valid_T; // @[operandCollector.scala 302:81]
  assign bankArbiterScalar_3_io_in_9_bits_rsAddr = io_readArbiterIO_2_1_bits_rsAddr; // @[operandCollector.scala 293:43]
  assign bankArbiterScalar_3_io_in_9_bits_rsType = io_readArbiterIO_2_1_bits_rsType; // @[operandCollector.scala 293:43]
  assign bankArbiterScalar_3_io_in_10_valid = io_readArbiterIO_2_2_valid & _bankArbiterScalar_3_io_in_10_valid_T; // @[operandCollector.scala 302:81]
  assign bankArbiterScalar_3_io_in_10_bits_rsAddr = io_readArbiterIO_2_2_bits_rsAddr; // @[operandCollector.scala 293:43]
  assign bankArbiterScalar_3_io_in_10_bits_rsType = io_readArbiterIO_2_2_bits_rsType; // @[operandCollector.scala 293:43]
  assign bankArbiterScalar_3_io_in_11_valid = io_readArbiterIO_2_3_valid & _bankArbiterScalar_3_io_in_11_valid_T; // @[operandCollector.scala 302:81]
  assign bankArbiterScalar_3_io_in_11_bits_rsAddr = io_readArbiterIO_2_3_bits_rsAddr; // @[operandCollector.scala 293:43]
  assign bankArbiterScalar_3_io_in_12_valid = io_readArbiterIO_3_0_valid & _bankArbiterScalar_3_io_in_12_valid_T; // @[operandCollector.scala 302:81]
  assign bankArbiterScalar_3_io_in_12_bits_rsAddr = io_readArbiterIO_3_0_bits_rsAddr; // @[operandCollector.scala 293:43]
  assign bankArbiterScalar_3_io_in_12_bits_rsType = io_readArbiterIO_3_0_bits_rsType; // @[operandCollector.scala 293:43]
  assign bankArbiterScalar_3_io_in_13_valid = io_readArbiterIO_3_1_valid & _bankArbiterScalar_3_io_in_13_valid_T; // @[operandCollector.scala 302:81]
  assign bankArbiterScalar_3_io_in_13_bits_rsAddr = io_readArbiterIO_3_1_bits_rsAddr; // @[operandCollector.scala 293:43]
  assign bankArbiterScalar_3_io_in_13_bits_rsType = io_readArbiterIO_3_1_bits_rsType; // @[operandCollector.scala 293:43]
  assign bankArbiterScalar_3_io_in_14_valid = io_readArbiterIO_3_2_valid & _bankArbiterScalar_3_io_in_14_valid_T; // @[operandCollector.scala 302:81]
  assign bankArbiterScalar_3_io_in_14_bits_rsAddr = io_readArbiterIO_3_2_bits_rsAddr; // @[operandCollector.scala 293:43]
  assign bankArbiterScalar_3_io_in_14_bits_rsType = io_readArbiterIO_3_2_bits_rsType; // @[operandCollector.scala 293:43]
  assign bankArbiterScalar_3_io_in_15_valid = io_readArbiterIO_3_3_valid & _bankArbiterScalar_3_io_in_15_valid_T; // @[operandCollector.scala 302:81]
  assign bankArbiterScalar_3_io_in_15_bits_rsAddr = io_readArbiterIO_3_3_bits_rsAddr; // @[operandCollector.scala 293:43]
endmodule
module FloatRegFileBank(
  input         clock,
  output [31:0] io_v0_0,
  output [31:0] io_rs_0,
  output [31:0] io_rs_1,
  output [31:0] io_rs_2,
  output [31:0] io_rs_3,
  input  [4:0]  io_rsidx,
  input  [31:0] io_rd_0,
  input  [31:0] io_rd_1,
  input  [31:0] io_rd_2,
  input  [31:0] io_rd_3,
  input  [4:0]  io_rdidx,
  input         io_rdwen,
  input         io_rdwmask_0,
  input         io_rdwmask_1,
  input         io_rdwmask_2,
  input         io_rdwmask_3
);
`ifdef RANDOMIZE_MEM_INIT
  reg [31:0] _RAND_0;
  reg [31:0] _RAND_5;
  reg [31:0] _RAND_10;
  reg [31:0] _RAND_15;
`endif // RANDOMIZE_MEM_INIT
`ifdef RANDOMIZE_REG_INIT
  reg [31:0] _RAND_1;
  reg [31:0] _RAND_2;
  reg [31:0] _RAND_3;
  reg [31:0] _RAND_4;
  reg [31:0] _RAND_6;
  reg [31:0] _RAND_7;
  reg [31:0] _RAND_8;
  reg [31:0] _RAND_9;
  reg [31:0] _RAND_11;
  reg [31:0] _RAND_12;
  reg [31:0] _RAND_13;
  reg [31:0] _RAND_14;
  reg [31:0] _RAND_16;
  reg [31:0] _RAND_17;
  reg [31:0] _RAND_18;
  reg [31:0] _RAND_19;
`endif // RANDOMIZE_REG_INIT
  reg [31:0] regs_0 [0:31]; // @[regfile.scala 39:25]
  wire  regs_0_MPORT_en; // @[regfile.scala 39:25]
  wire [4:0] regs_0_MPORT_addr; // @[regfile.scala 39:25]
  wire [31:0] regs_0_MPORT_data; // @[regfile.scala 39:25]
  wire  regs_0_MPORT_1_en; // @[regfile.scala 39:25]
  wire [4:0] regs_0_MPORT_1_addr; // @[regfile.scala 39:25]
  wire [31:0] regs_0_MPORT_1_data; // @[regfile.scala 39:25]
  wire [31:0] regs_0_MPORT_2_data; // @[regfile.scala 39:25]
  wire [4:0] regs_0_MPORT_2_addr; // @[regfile.scala 39:25]
  wire  regs_0_MPORT_2_mask; // @[regfile.scala 39:25]
  wire  regs_0_MPORT_2_en; // @[regfile.scala 39:25]
  reg  regs_0_MPORT_en_pipe_0;
  reg [4:0] regs_0_MPORT_addr_pipe_0;
  reg  regs_0_MPORT_1_en_pipe_0;
  reg [4:0] regs_0_MPORT_1_addr_pipe_0;
  reg [31:0] regs_1 [0:31]; // @[regfile.scala 39:25]
  wire  regs_1_MPORT_en; // @[regfile.scala 39:25]
  wire [4:0] regs_1_MPORT_addr; // @[regfile.scala 39:25]
  wire [31:0] regs_1_MPORT_data; // @[regfile.scala 39:25]
  wire  regs_1_MPORT_1_en; // @[regfile.scala 39:25]
  wire [4:0] regs_1_MPORT_1_addr; // @[regfile.scala 39:25]
  wire [31:0] regs_1_MPORT_1_data; // @[regfile.scala 39:25]
  wire [31:0] regs_1_MPORT_2_data; // @[regfile.scala 39:25]
  wire [4:0] regs_1_MPORT_2_addr; // @[regfile.scala 39:25]
  wire  regs_1_MPORT_2_mask; // @[regfile.scala 39:25]
  wire  regs_1_MPORT_2_en; // @[regfile.scala 39:25]
  reg  regs_1_MPORT_en_pipe_0;
  reg [4:0] regs_1_MPORT_addr_pipe_0;
  reg  regs_1_MPORT_1_en_pipe_0;
  reg [4:0] regs_1_MPORT_1_addr_pipe_0;
  reg [31:0] regs_2 [0:31]; // @[regfile.scala 39:25]
  wire  regs_2_MPORT_en; // @[regfile.scala 39:25]
  wire [4:0] regs_2_MPORT_addr; // @[regfile.scala 39:25]
  wire [31:0] regs_2_MPORT_data; // @[regfile.scala 39:25]
  wire  regs_2_MPORT_1_en; // @[regfile.scala 39:25]
  wire [4:0] regs_2_MPORT_1_addr; // @[regfile.scala 39:25]
  wire [31:0] regs_2_MPORT_1_data; // @[regfile.scala 39:25]
  wire [31:0] regs_2_MPORT_2_data; // @[regfile.scala 39:25]
  wire [4:0] regs_2_MPORT_2_addr; // @[regfile.scala 39:25]
  wire  regs_2_MPORT_2_mask; // @[regfile.scala 39:25]
  wire  regs_2_MPORT_2_en; // @[regfile.scala 39:25]
  reg  regs_2_MPORT_en_pipe_0;
  reg [4:0] regs_2_MPORT_addr_pipe_0;
  reg  regs_2_MPORT_1_en_pipe_0;
  reg [4:0] regs_2_MPORT_1_addr_pipe_0;
  reg [31:0] regs_3 [0:31]; // @[regfile.scala 39:25]
  wire  regs_3_MPORT_en; // @[regfile.scala 39:25]
  wire [4:0] regs_3_MPORT_addr; // @[regfile.scala 39:25]
  wire [31:0] regs_3_MPORT_data; // @[regfile.scala 39:25]
  wire  regs_3_MPORT_1_en; // @[regfile.scala 39:25]
  wire [4:0] regs_3_MPORT_1_addr; // @[regfile.scala 39:25]
  wire [31:0] regs_3_MPORT_1_data; // @[regfile.scala 39:25]
  wire [31:0] regs_3_MPORT_2_data; // @[regfile.scala 39:25]
  wire [4:0] regs_3_MPORT_2_addr; // @[regfile.scala 39:25]
  wire  regs_3_MPORT_2_mask; // @[regfile.scala 39:25]
  wire  regs_3_MPORT_2_en; // @[regfile.scala 39:25]
  reg  regs_3_MPORT_en_pipe_0;
  reg [4:0] regs_3_MPORT_addr_pipe_0;
  reg  regs_3_MPORT_1_en_pipe_0;
  reg [4:0] regs_3_MPORT_1_addr_pipe_0;
  assign regs_0_MPORT_en = regs_0_MPORT_en_pipe_0;
  assign regs_0_MPORT_addr = regs_0_MPORT_addr_pipe_0;
  assign regs_0_MPORT_data = regs_0[regs_0_MPORT_addr]; // @[regfile.scala 39:25]
  assign regs_0_MPORT_1_en = regs_0_MPORT_1_en_pipe_0;
  assign regs_0_MPORT_1_addr = regs_0_MPORT_1_addr_pipe_0;
  assign regs_0_MPORT_1_data = regs_0[regs_0_MPORT_1_addr]; // @[regfile.scala 39:25]
  assign regs_0_MPORT_2_data = io_rd_0;
  assign regs_0_MPORT_2_addr = io_rdidx;
  assign regs_0_MPORT_2_mask = io_rdwmask_0;
  assign regs_0_MPORT_2_en = io_rdwen;
  assign regs_1_MPORT_en = regs_1_MPORT_en_pipe_0;
  assign regs_1_MPORT_addr = regs_1_MPORT_addr_pipe_0;
  assign regs_1_MPORT_data = regs_1[regs_1_MPORT_addr]; // @[regfile.scala 39:25]
  assign regs_1_MPORT_1_en = regs_1_MPORT_1_en_pipe_0;
  assign regs_1_MPORT_1_addr = regs_1_MPORT_1_addr_pipe_0;
  assign regs_1_MPORT_1_data = regs_1[regs_1_MPORT_1_addr]; // @[regfile.scala 39:25]
  assign regs_1_MPORT_2_data = io_rd_1;
  assign regs_1_MPORT_2_addr = io_rdidx;
  assign regs_1_MPORT_2_mask = io_rdwmask_1;
  assign regs_1_MPORT_2_en = io_rdwen;
  assign regs_2_MPORT_en = regs_2_MPORT_en_pipe_0;
  assign regs_2_MPORT_addr = regs_2_MPORT_addr_pipe_0;
  assign regs_2_MPORT_data = regs_2[regs_2_MPORT_addr]; // @[regfile.scala 39:25]
  assign regs_2_MPORT_1_en = regs_2_MPORT_1_en_pipe_0;
  assign regs_2_MPORT_1_addr = regs_2_MPORT_1_addr_pipe_0;
  assign regs_2_MPORT_1_data = regs_2[regs_2_MPORT_1_addr]; // @[regfile.scala 39:25]
  assign regs_2_MPORT_2_data = io_rd_2;
  assign regs_2_MPORT_2_addr = io_rdidx;
  assign regs_2_MPORT_2_mask = io_rdwmask_2;
  assign regs_2_MPORT_2_en = io_rdwen;
  assign regs_3_MPORT_en = regs_3_MPORT_en_pipe_0;
  assign regs_3_MPORT_addr = regs_3_MPORT_addr_pipe_0;
  assign regs_3_MPORT_data = regs_3[regs_3_MPORT_addr]; // @[regfile.scala 39:25]
  assign regs_3_MPORT_1_en = regs_3_MPORT_1_en_pipe_0;
  assign regs_3_MPORT_1_addr = regs_3_MPORT_1_addr_pipe_0;
  assign regs_3_MPORT_1_data = regs_3[regs_3_MPORT_1_addr]; // @[regfile.scala 39:25]
  assign regs_3_MPORT_2_data = io_rd_3;
  assign regs_3_MPORT_2_addr = io_rdidx;
  assign regs_3_MPORT_2_mask = io_rdwmask_3;
  assign regs_3_MPORT_2_en = io_rdwen;
  assign io_v0_0 = regs_0_MPORT_1_data; // @[regfile.scala 44:9]
  assign io_rs_0 = io_rsidx == io_rdidx & io_rdwen ? io_rd_0 : regs_0_MPORT_data; // @[regfile.scala 43:15]
  assign io_rs_1 = io_rsidx == io_rdidx & io_rdwen ? io_rd_1 : regs_1_MPORT_data; // @[regfile.scala 43:15]
  assign io_rs_2 = io_rsidx == io_rdidx & io_rdwen ? io_rd_2 : regs_2_MPORT_data; // @[regfile.scala 43:15]
  assign io_rs_3 = io_rsidx == io_rdidx & io_rdwen ? io_rd_3 : regs_3_MPORT_data; // @[regfile.scala 43:15]
  always @(posedge clock) begin
    if (regs_0_MPORT_2_en & regs_0_MPORT_2_mask) begin
      regs_0[regs_0_MPORT_2_addr] <= regs_0_MPORT_2_data; // @[regfile.scala 39:25]
    end
    regs_0_MPORT_en_pipe_0 <= 1'h1;
    if (1'h1) begin
      regs_0_MPORT_addr_pipe_0 <= io_rsidx;
    end
    regs_0_MPORT_1_en_pipe_0 <= 1'h1;
    if (1'h1) begin
      regs_0_MPORT_1_addr_pipe_0 <= 5'h0;
    end
    if (regs_1_MPORT_2_en & regs_1_MPORT_2_mask) begin
      regs_1[regs_1_MPORT_2_addr] <= regs_1_MPORT_2_data; // @[regfile.scala 39:25]
    end
    regs_1_MPORT_en_pipe_0 <= 1'h1;
    if (1'h1) begin
      regs_1_MPORT_addr_pipe_0 <= io_rsidx;
    end
    regs_1_MPORT_1_en_pipe_0 <= 1'h1;
    if (1'h1) begin
      regs_1_MPORT_1_addr_pipe_0 <= 5'h0;
    end
    if (regs_2_MPORT_2_en & regs_2_MPORT_2_mask) begin
      regs_2[regs_2_MPORT_2_addr] <= regs_2_MPORT_2_data; // @[regfile.scala 39:25]
    end
    regs_2_MPORT_en_pipe_0 <= 1'h1;
    if (1'h1) begin
      regs_2_MPORT_addr_pipe_0 <= io_rsidx;
    end
    regs_2_MPORT_1_en_pipe_0 <= 1'h1;
    if (1'h1) begin
      regs_2_MPORT_1_addr_pipe_0 <= 5'h0;
    end
    if (regs_3_MPORT_2_en & regs_3_MPORT_2_mask) begin
      regs_3[regs_3_MPORT_2_addr] <= regs_3_MPORT_2_data; // @[regfile.scala 39:25]
    end
    regs_3_MPORT_en_pipe_0 <= 1'h1;
    if (1'h1) begin
      regs_3_MPORT_addr_pipe_0 <= io_rsidx;
    end
    regs_3_MPORT_1_en_pipe_0 <= 1'h1;
    if (1'h1) begin
      regs_3_MPORT_1_addr_pipe_0 <= 5'h0;
    end
  end
// Register and memory initialization
`ifdef RANDOMIZE_GARBAGE_ASSIGN
`define RANDOMIZE
`endif
`ifdef RANDOMIZE_INVALID_ASSIGN
`define RANDOMIZE
`endif
`ifdef RANDOMIZE_REG_INIT
`define RANDOMIZE
`endif
`ifdef RANDOMIZE_MEM_INIT
`define RANDOMIZE
`endif
`ifndef RANDOM
`define RANDOM $random
`endif
`ifdef RANDOMIZE_MEM_INIT
  integer initvar;
`endif
`ifndef SYNTHESIS
`ifdef FIRRTL_BEFORE_INITIAL
`FIRRTL_BEFORE_INITIAL
`endif
initial begin
  `ifdef RANDOMIZE
    `ifdef INIT_RANDOM
      `INIT_RANDOM
    `endif
    `ifndef VERILATOR
      `ifdef RANDOMIZE_DELAY
        #`RANDOMIZE_DELAY begin end
      `else
        #0.002 begin end
      `endif
    `endif
`ifdef RANDOMIZE_MEM_INIT
  _RAND_0 = {1{`RANDOM}};
  for (initvar = 0; initvar < 32; initvar = initvar+1)
    regs_0[initvar] = _RAND_0[31:0];
  _RAND_5 = {1{`RANDOM}};
  for (initvar = 0; initvar < 32; initvar = initvar+1)
    regs_1[initvar] = _RAND_5[31:0];
  _RAND_10 = {1{`RANDOM}};
  for (initvar = 0; initvar < 32; initvar = initvar+1)
    regs_2[initvar] = _RAND_10[31:0];
  _RAND_15 = {1{`RANDOM}};
  for (initvar = 0; initvar < 32; initvar = initvar+1)
    regs_3[initvar] = _RAND_15[31:0];
`endif // RANDOMIZE_MEM_INIT
`ifdef RANDOMIZE_REG_INIT
  _RAND_1 = {1{`RANDOM}};
  regs_0_MPORT_en_pipe_0 = _RAND_1[0:0];
  _RAND_2 = {1{`RANDOM}};
  regs_0_MPORT_addr_pipe_0 = _RAND_2[4:0];
  _RAND_3 = {1{`RANDOM}};
  regs_0_MPORT_1_en_pipe_0 = _RAND_3[0:0];
  _RAND_4 = {1{`RANDOM}};
  regs_0_MPORT_1_addr_pipe_0 = _RAND_4[4:0];
  _RAND_6 = {1{`RANDOM}};
  regs_1_MPORT_en_pipe_0 = _RAND_6[0:0];
  _RAND_7 = {1{`RANDOM}};
  regs_1_MPORT_addr_pipe_0 = _RAND_7[4:0];
  _RAND_8 = {1{`RANDOM}};
  regs_1_MPORT_1_en_pipe_0 = _RAND_8[0:0];
  _RAND_9 = {1{`RANDOM}};
  regs_1_MPORT_1_addr_pipe_0 = _RAND_9[4:0];
  _RAND_11 = {1{`RANDOM}};
  regs_2_MPORT_en_pipe_0 = _RAND_11[0:0];
  _RAND_12 = {1{`RANDOM}};
  regs_2_MPORT_addr_pipe_0 = _RAND_12[4:0];
  _RAND_13 = {1{`RANDOM}};
  regs_2_MPORT_1_en_pipe_0 = _RAND_13[0:0];
  _RAND_14 = {1{`RANDOM}};
  regs_2_MPORT_1_addr_pipe_0 = _RAND_14[4:0];
  _RAND_16 = {1{`RANDOM}};
  regs_3_MPORT_en_pipe_0 = _RAND_16[0:0];
  _RAND_17 = {1{`RANDOM}};
  regs_3_MPORT_addr_pipe_0 = _RAND_17[4:0];
  _RAND_18 = {1{`RANDOM}};
  regs_3_MPORT_1_en_pipe_0 = _RAND_18[0:0];
  _RAND_19 = {1{`RANDOM}};
  regs_3_MPORT_1_addr_pipe_0 = _RAND_19[4:0];
`endif // RANDOMIZE_REG_INIT
  `endif // RANDOMIZE
end // initial
`ifdef FIRRTL_AFTER_INITIAL
`FIRRTL_AFTER_INITIAL
`endif
`endif // SYNTHESIS
endmodule
module RegFileBank(
  input         clock,
  output [31:0] io_rs,
  input  [4:0]  io_rsidx,
  input  [31:0] io_rd,
  input  [4:0]  io_rdidx,
  input         io_rdwen
);
`ifdef RANDOMIZE_MEM_INIT
  reg [31:0] _RAND_0;
`endif // RANDOMIZE_MEM_INIT
`ifdef RANDOMIZE_REG_INIT
  reg [31:0] _RAND_1;
  reg [31:0] _RAND_2;
`endif // RANDOMIZE_REG_INIT
  reg [31:0] regs [0:31]; // @[regfile.scala 19:25]
  wire  regs_io_rs_MPORT_en; // @[regfile.scala 19:25]
  wire [4:0] regs_io_rs_MPORT_addr; // @[regfile.scala 19:25]
  wire [31:0] regs_io_rs_MPORT_data; // @[regfile.scala 19:25]
  wire [31:0] regs_MPORT_data; // @[regfile.scala 19:25]
  wire [4:0] regs_MPORT_addr; // @[regfile.scala 19:25]
  wire  regs_MPORT_mask; // @[regfile.scala 19:25]
  wire  regs_MPORT_en; // @[regfile.scala 19:25]
  reg  regs_io_rs_MPORT_en_pipe_0;
  reg [4:0] regs_io_rs_MPORT_addr_pipe_0;
  wire [31:0] _io_rs_T_5 = |io_rsidx ? regs_io_rs_MPORT_data : 32'h0; // @[regfile.scala 20:58]
  wire  _T = |io_rdidx; // @[regfile.scala 22:29]
  assign regs_io_rs_MPORT_en = regs_io_rs_MPORT_en_pipe_0;
  assign regs_io_rs_MPORT_addr = regs_io_rs_MPORT_addr_pipe_0;
  assign regs_io_rs_MPORT_data = regs[regs_io_rs_MPORT_addr]; // @[regfile.scala 19:25]
  assign regs_MPORT_data = io_rd;
  assign regs_MPORT_addr = io_rdidx;
  assign regs_MPORT_mask = 1'h1;
  assign regs_MPORT_en = io_rdwen & _T;
  assign io_rs = io_rsidx == io_rdidx & io_rdwen ? io_rd : _io_rs_T_5; // @[regfile.scala 20:15]
  always @(posedge clock) begin
    if (regs_MPORT_en & regs_MPORT_mask) begin
      regs[regs_MPORT_addr] <= regs_MPORT_data; // @[regfile.scala 19:25]
    end
    regs_io_rs_MPORT_en_pipe_0 <= 1'h1;
    if (1'h1) begin
      regs_io_rs_MPORT_addr_pipe_0 <= io_rsidx;
    end
  end
// Register and memory initialization
`ifdef RANDOMIZE_GARBAGE_ASSIGN
`define RANDOMIZE
`endif
`ifdef RANDOMIZE_INVALID_ASSIGN
`define RANDOMIZE
`endif
`ifdef RANDOMIZE_REG_INIT
`define RANDOMIZE
`endif
`ifdef RANDOMIZE_MEM_INIT
`define RANDOMIZE
`endif
`ifndef RANDOM
`define RANDOM $random
`endif
`ifdef RANDOMIZE_MEM_INIT
  integer initvar;
`endif
`ifndef SYNTHESIS
`ifdef FIRRTL_BEFORE_INITIAL
`FIRRTL_BEFORE_INITIAL
`endif
initial begin
  `ifdef RANDOMIZE
    `ifdef INIT_RANDOM
      `INIT_RANDOM
    `endif
    `ifndef VERILATOR
      `ifdef RANDOMIZE_DELAY
        #`RANDOMIZE_DELAY begin end
      `else
        #0.002 begin end
      `endif
    `endif
`ifdef RANDOMIZE_MEM_INIT
  _RAND_0 = {1{`RANDOM}};
  for (initvar = 0; initvar < 32; initvar = initvar+1)
    regs[initvar] = _RAND_0[31:0];
`endif // RANDOMIZE_MEM_INIT
`ifdef RANDOMIZE_REG_INIT
  _RAND_1 = {1{`RANDOM}};
  regs_io_rs_MPORT_en_pipe_0 = _RAND_1[0:0];
  _RAND_2 = {1{`RANDOM}};
  regs_io_rs_MPORT_addr_pipe_0 = _RAND_2[4:0];
`endif // RANDOMIZE_REG_INIT
  `endif // RANDOMIZE
end // initial
`ifdef FIRRTL_AFTER_INITIAL
`FIRRTL_AFTER_INITIAL
`endif
`endif // SYNTHESIS
endmodule
module crossBar(
  input  [3:0]  io_chosen_0,
  input  [3:0]  io_chosen_1,
  input  [3:0]  io_chosen_2,
  input  [3:0]  io_chosen_3,
  input         io_validArbiter_0,
  input         io_validArbiter_1,
  input         io_validArbiter_2,
  input         io_validArbiter_3,
  input  [31:0] io_dataIn_rs_0_0,
  input  [31:0] io_dataIn_rs_0_1,
  input  [31:0] io_dataIn_rs_0_2,
  input  [31:0] io_dataIn_rs_0_3,
  input  [31:0] io_dataIn_rs_1_0,
  input  [31:0] io_dataIn_rs_1_1,
  input  [31:0] io_dataIn_rs_1_2,
  input  [31:0] io_dataIn_rs_1_3,
  input  [31:0] io_dataIn_rs_2_0,
  input  [31:0] io_dataIn_rs_2_1,
  input  [31:0] io_dataIn_rs_2_2,
  input  [31:0] io_dataIn_rs_2_3,
  input  [31:0] io_dataIn_rs_3_0,
  input  [31:0] io_dataIn_rs_3_1,
  input  [31:0] io_dataIn_rs_3_2,
  input  [31:0] io_dataIn_rs_3_3,
  input  [31:0] io_dataIn_v0_0_0,
  input  [31:0] io_dataIn_v0_1_0,
  input  [31:0] io_dataIn_v0_2_0,
  input  [31:0] io_dataIn_v0_3_0,
  output        io_out_0_0_valid,
  output [1:0]  io_out_0_0_bits_regOrder,
  output [31:0] io_out_0_0_bits_data_0,
  output [31:0] io_out_0_0_bits_data_1,
  output [31:0] io_out_0_0_bits_data_2,
  output [31:0] io_out_0_0_bits_data_3,
  output [31:0] io_out_0_0_bits_v0_0,
  output        io_out_0_1_valid,
  output [1:0]  io_out_0_1_bits_regOrder,
  output [31:0] io_out_0_1_bits_data_0,
  output [31:0] io_out_0_1_bits_data_1,
  output [31:0] io_out_0_1_bits_data_2,
  output [31:0] io_out_0_1_bits_data_3,
  output [31:0] io_out_0_1_bits_v0_0,
  output        io_out_0_2_valid,
  output [1:0]  io_out_0_2_bits_regOrder,
  output [31:0] io_out_0_2_bits_data_0,
  output [31:0] io_out_0_2_bits_data_1,
  output [31:0] io_out_0_2_bits_data_2,
  output [31:0] io_out_0_2_bits_data_3,
  output [31:0] io_out_0_2_bits_v0_0,
  output        io_out_0_3_valid,
  output [1:0]  io_out_0_3_bits_regOrder,
  output [31:0] io_out_0_3_bits_data_0,
  output [31:0] io_out_0_3_bits_data_1,
  output [31:0] io_out_0_3_bits_data_2,
  output [31:0] io_out_0_3_bits_data_3,
  output [31:0] io_out_0_3_bits_v0_0,
  output        io_out_1_0_valid,
  output [1:0]  io_out_1_0_bits_regOrder,
  output [31:0] io_out_1_0_bits_data_0,
  output [31:0] io_out_1_0_bits_data_1,
  output [31:0] io_out_1_0_bits_data_2,
  output [31:0] io_out_1_0_bits_data_3,
  output [31:0] io_out_1_0_bits_v0_0,
  output        io_out_1_1_valid,
  output [1:0]  io_out_1_1_bits_regOrder,
  output [31:0] io_out_1_1_bits_data_0,
  output [31:0] io_out_1_1_bits_data_1,
  output [31:0] io_out_1_1_bits_data_2,
  output [31:0] io_out_1_1_bits_data_3,
  output [31:0] io_out_1_1_bits_v0_0,
  output        io_out_1_2_valid,
  output [1:0]  io_out_1_2_bits_regOrder,
  output [31:0] io_out_1_2_bits_data_0,
  output [31:0] io_out_1_2_bits_data_1,
  output [31:0] io_out_1_2_bits_data_2,
  output [31:0] io_out_1_2_bits_data_3,
  output [31:0] io_out_1_2_bits_v0_0,
  output        io_out_1_3_valid,
  output [1:0]  io_out_1_3_bits_regOrder,
  output [31:0] io_out_1_3_bits_data_0,
  output [31:0] io_out_1_3_bits_data_1,
  output [31:0] io_out_1_3_bits_data_2,
  output [31:0] io_out_1_3_bits_data_3,
  output [31:0] io_out_1_3_bits_v0_0,
  output        io_out_2_0_valid,
  output [1:0]  io_out_2_0_bits_regOrder,
  output [31:0] io_out_2_0_bits_data_0,
  output [31:0] io_out_2_0_bits_data_1,
  output [31:0] io_out_2_0_bits_data_2,
  output [31:0] io_out_2_0_bits_data_3,
  output [31:0] io_out_2_0_bits_v0_0,
  output        io_out_2_1_valid,
  output [1:0]  io_out_2_1_bits_regOrder,
  output [31:0] io_out_2_1_bits_data_0,
  output [31:0] io_out_2_1_bits_data_1,
  output [31:0] io_out_2_1_bits_data_2,
  output [31:0] io_out_2_1_bits_data_3,
  output [31:0] io_out_2_1_bits_v0_0,
  output        io_out_2_2_valid,
  output [1:0]  io_out_2_2_bits_regOrder,
  output [31:0] io_out_2_2_bits_data_0,
  output [31:0] io_out_2_2_bits_data_1,
  output [31:0] io_out_2_2_bits_data_2,
  output [31:0] io_out_2_2_bits_data_3,
  output [31:0] io_out_2_2_bits_v0_0,
  output        io_out_2_3_valid,
  output [1:0]  io_out_2_3_bits_regOrder,
  output [31:0] io_out_2_3_bits_data_0,
  output [31:0] io_out_2_3_bits_data_1,
  output [31:0] io_out_2_3_bits_data_2,
  output [31:0] io_out_2_3_bits_data_3,
  output [31:0] io_out_2_3_bits_v0_0,
  output        io_out_3_0_valid,
  output [1:0]  io_out_3_0_bits_regOrder,
  output [31:0] io_out_3_0_bits_data_0,
  output [31:0] io_out_3_0_bits_data_1,
  output [31:0] io_out_3_0_bits_data_2,
  output [31:0] io_out_3_0_bits_data_3,
  output [31:0] io_out_3_0_bits_v0_0,
  output        io_out_3_1_valid,
  output [1:0]  io_out_3_1_bits_regOrder,
  output [31:0] io_out_3_1_bits_data_0,
  output [31:0] io_out_3_1_bits_data_1,
  output [31:0] io_out_3_1_bits_data_2,
  output [31:0] io_out_3_1_bits_data_3,
  output [31:0] io_out_3_1_bits_v0_0,
  output        io_out_3_2_valid,
  output [1:0]  io_out_3_2_bits_regOrder,
  output [31:0] io_out_3_2_bits_data_0,
  output [31:0] io_out_3_2_bits_data_1,
  output [31:0] io_out_3_2_bits_data_2,
  output [31:0] io_out_3_2_bits_data_3,
  output [31:0] io_out_3_2_bits_v0_0,
  output        io_out_3_3_valid,
  output [1:0]  io_out_3_3_bits_regOrder,
  output [31:0] io_out_3_3_bits_data_0,
  output [31:0] io_out_3_3_bits_data_1,
  output [31:0] io_out_3_3_bits_data_2,
  output [31:0] io_out_3_3_bits_data_3,
  output [31:0] io_out_3_3_bits_v0_0
);
  wire [3:0] _CUId_0_T = {{2'd0}, io_chosen_0[3:2]}; // @[operandCollector.scala 333:29]
  wire [3:0] _GEN_5 = io_chosen_0 % 4'h4; // @[operandCollector.scala 334:33]
  wire [3:0] _CUId_1_T = {{2'd0}, io_chosen_1[3:2]}; // @[operandCollector.scala 333:29]
  wire [3:0] _GEN_6 = io_chosen_1 % 4'h4; // @[operandCollector.scala 334:33]
  wire [3:0] _CUId_2_T = {{2'd0}, io_chosen_2[3:2]}; // @[operandCollector.scala 333:29]
  wire [3:0] _GEN_7 = io_chosen_2 % 4'h4; // @[operandCollector.scala 334:33]
  wire [3:0] _CUId_3_T = {{2'd0}, io_chosen_3[3:2]}; // @[operandCollector.scala 333:29]
  wire [3:0] _GEN_8 = io_chosen_3 % 4'h4; // @[operandCollector.scala 334:33]
  wire [1:0] CUId_0 = _CUId_0_T[1:0]; // @[operandCollector.scala 328:18 333:13]
  wire [1:0] regOrder_0 = _GEN_5[1:0]; // @[operandCollector.scala 329:22 334:17]
  wire [31:0] _GEN_0 = CUId_0 == 2'h0 & io_validArbiter_0 & regOrder_0 == 2'h0 ? io_dataIn_rs_0_0 : 32'h0; // @[operandCollector.scala 351:74 352:34 344:40]
  wire [31:0] _GEN_1 = CUId_0 == 2'h0 & io_validArbiter_0 & regOrder_0 == 2'h0 ? io_dataIn_rs_0_1 : 32'h0; // @[operandCollector.scala 351:74 352:34 344:40]
  wire [31:0] _GEN_2 = CUId_0 == 2'h0 & io_validArbiter_0 & regOrder_0 == 2'h0 ? io_dataIn_rs_0_2 : 32'h0; // @[operandCollector.scala 351:74 352:34 344:40]
  wire [31:0] _GEN_3 = CUId_0 == 2'h0 & io_validArbiter_0 & regOrder_0 == 2'h0 ? io_dataIn_rs_0_3 : 32'h0; // @[operandCollector.scala 351:74 352:34 344:40]
  wire [31:0] _GEN_4 = CUId_0 == 2'h0 & io_validArbiter_0 & regOrder_0 == 2'h0 ? io_dataIn_v0_0_0 : 32'h0; // @[operandCollector.scala 351:74 353:32 345:38]
  wire [1:0] _GEN_9 = CUId_0 == 2'h0 & io_validArbiter_0 & regOrder_0 == 2'h0 ? regOrder_0 : 2'h0; // @[operandCollector.scala 351:74 355:38 347:44]
  wire [31:0] _GEN_10 = CUId_0 == 2'h0 & io_validArbiter_0 & regOrder_0 == 2'h1 ? io_dataIn_rs_0_0 : 32'h0; // @[operandCollector.scala 351:74 352:34 344:40]
  wire [31:0] _GEN_11 = CUId_0 == 2'h0 & io_validArbiter_0 & regOrder_0 == 2'h1 ? io_dataIn_rs_0_1 : 32'h0; // @[operandCollector.scala 351:74 352:34 344:40]
  wire [31:0] _GEN_12 = CUId_0 == 2'h0 & io_validArbiter_0 & regOrder_0 == 2'h1 ? io_dataIn_rs_0_2 : 32'h0; // @[operandCollector.scala 351:74 352:34 344:40]
  wire [31:0] _GEN_13 = CUId_0 == 2'h0 & io_validArbiter_0 & regOrder_0 == 2'h1 ? io_dataIn_rs_0_3 : 32'h0; // @[operandCollector.scala 351:74 352:34 344:40]
  wire [31:0] _GEN_14 = CUId_0 == 2'h0 & io_validArbiter_0 & regOrder_0 == 2'h1 ? io_dataIn_v0_0_0 : 32'h0; // @[operandCollector.scala 351:74 353:32 345:38]
  wire [1:0] _GEN_19 = CUId_0 == 2'h0 & io_validArbiter_0 & regOrder_0 == 2'h1 ? regOrder_0 : 2'h0; // @[operandCollector.scala 351:74 355:38 347:44]
  wire [31:0] _GEN_20 = CUId_0 == 2'h0 & io_validArbiter_0 & regOrder_0 == 2'h2 ? io_dataIn_rs_0_0 : 32'h0; // @[operandCollector.scala 351:74 352:34 344:40]
  wire [31:0] _GEN_21 = CUId_0 == 2'h0 & io_validArbiter_0 & regOrder_0 == 2'h2 ? io_dataIn_rs_0_1 : 32'h0; // @[operandCollector.scala 351:74 352:34 344:40]
  wire [31:0] _GEN_22 = CUId_0 == 2'h0 & io_validArbiter_0 & regOrder_0 == 2'h2 ? io_dataIn_rs_0_2 : 32'h0; // @[operandCollector.scala 351:74 352:34 344:40]
  wire [31:0] _GEN_23 = CUId_0 == 2'h0 & io_validArbiter_0 & regOrder_0 == 2'h2 ? io_dataIn_rs_0_3 : 32'h0; // @[operandCollector.scala 351:74 352:34 344:40]
  wire [31:0] _GEN_24 = CUId_0 == 2'h0 & io_validArbiter_0 & regOrder_0 == 2'h2 ? io_dataIn_v0_0_0 : 32'h0; // @[operandCollector.scala 351:74 353:32 345:38]
  wire [1:0] _GEN_29 = CUId_0 == 2'h0 & io_validArbiter_0 & regOrder_0 == 2'h2 ? regOrder_0 : 2'h0; // @[operandCollector.scala 351:74 355:38 347:44]
  wire [31:0] _GEN_30 = CUId_0 == 2'h0 & io_validArbiter_0 & regOrder_0 == 2'h3 ? io_dataIn_rs_0_0 : 32'h0; // @[operandCollector.scala 351:74 352:34 344:40]
  wire [31:0] _GEN_31 = CUId_0 == 2'h0 & io_validArbiter_0 & regOrder_0 == 2'h3 ? io_dataIn_rs_0_1 : 32'h0; // @[operandCollector.scala 351:74 352:34 344:40]
  wire [31:0] _GEN_32 = CUId_0 == 2'h0 & io_validArbiter_0 & regOrder_0 == 2'h3 ? io_dataIn_rs_0_2 : 32'h0; // @[operandCollector.scala 351:74 352:34 344:40]
  wire [31:0] _GEN_33 = CUId_0 == 2'h0 & io_validArbiter_0 & regOrder_0 == 2'h3 ? io_dataIn_rs_0_3 : 32'h0; // @[operandCollector.scala 351:74 352:34 344:40]
  wire [31:0] _GEN_34 = CUId_0 == 2'h0 & io_validArbiter_0 & regOrder_0 == 2'h3 ? io_dataIn_v0_0_0 : 32'h0; // @[operandCollector.scala 351:74 353:32 345:38]
  wire [1:0] _GEN_39 = CUId_0 == 2'h0 & io_validArbiter_0 & regOrder_0 == 2'h3 ? regOrder_0 : 2'h0; // @[operandCollector.scala 351:74 355:38 347:44]
  wire [31:0] _GEN_40 = CUId_0 == 2'h1 & io_validArbiter_0 & regOrder_0 == 2'h0 ? io_dataIn_rs_0_0 : 32'h0; // @[operandCollector.scala 351:74 352:34 344:40]
  wire [31:0] _GEN_41 = CUId_0 == 2'h1 & io_validArbiter_0 & regOrder_0 == 2'h0 ? io_dataIn_rs_0_1 : 32'h0; // @[operandCollector.scala 351:74 352:34 344:40]
  wire [31:0] _GEN_42 = CUId_0 == 2'h1 & io_validArbiter_0 & regOrder_0 == 2'h0 ? io_dataIn_rs_0_2 : 32'h0; // @[operandCollector.scala 351:74 352:34 344:40]
  wire [31:0] _GEN_43 = CUId_0 == 2'h1 & io_validArbiter_0 & regOrder_0 == 2'h0 ? io_dataIn_rs_0_3 : 32'h0; // @[operandCollector.scala 351:74 352:34 344:40]
  wire [31:0] _GEN_44 = CUId_0 == 2'h1 & io_validArbiter_0 & regOrder_0 == 2'h0 ? io_dataIn_v0_0_0 : 32'h0; // @[operandCollector.scala 351:74 353:32 345:38]
  wire [1:0] _GEN_49 = CUId_0 == 2'h1 & io_validArbiter_0 & regOrder_0 == 2'h0 ? regOrder_0 : 2'h0; // @[operandCollector.scala 351:74 355:38 347:44]
  wire [31:0] _GEN_50 = CUId_0 == 2'h1 & io_validArbiter_0 & regOrder_0 == 2'h1 ? io_dataIn_rs_0_0 : 32'h0; // @[operandCollector.scala 351:74 352:34 344:40]
  wire [31:0] _GEN_51 = CUId_0 == 2'h1 & io_validArbiter_0 & regOrder_0 == 2'h1 ? io_dataIn_rs_0_1 : 32'h0; // @[operandCollector.scala 351:74 352:34 344:40]
  wire [31:0] _GEN_52 = CUId_0 == 2'h1 & io_validArbiter_0 & regOrder_0 == 2'h1 ? io_dataIn_rs_0_2 : 32'h0; // @[operandCollector.scala 351:74 352:34 344:40]
  wire [31:0] _GEN_53 = CUId_0 == 2'h1 & io_validArbiter_0 & regOrder_0 == 2'h1 ? io_dataIn_rs_0_3 : 32'h0; // @[operandCollector.scala 351:74 352:34 344:40]
  wire [31:0] _GEN_54 = CUId_0 == 2'h1 & io_validArbiter_0 & regOrder_0 == 2'h1 ? io_dataIn_v0_0_0 : 32'h0; // @[operandCollector.scala 351:74 353:32 345:38]
  wire [1:0] _GEN_59 = CUId_0 == 2'h1 & io_validArbiter_0 & regOrder_0 == 2'h1 ? regOrder_0 : 2'h0; // @[operandCollector.scala 351:74 355:38 347:44]
  wire [31:0] _GEN_60 = CUId_0 == 2'h1 & io_validArbiter_0 & regOrder_0 == 2'h2 ? io_dataIn_rs_0_0 : 32'h0; // @[operandCollector.scala 351:74 352:34 344:40]
  wire [31:0] _GEN_61 = CUId_0 == 2'h1 & io_validArbiter_0 & regOrder_0 == 2'h2 ? io_dataIn_rs_0_1 : 32'h0; // @[operandCollector.scala 351:74 352:34 344:40]
  wire [31:0] _GEN_62 = CUId_0 == 2'h1 & io_validArbiter_0 & regOrder_0 == 2'h2 ? io_dataIn_rs_0_2 : 32'h0; // @[operandCollector.scala 351:74 352:34 344:40]
  wire [31:0] _GEN_63 = CUId_0 == 2'h1 & io_validArbiter_0 & regOrder_0 == 2'h2 ? io_dataIn_rs_0_3 : 32'h0; // @[operandCollector.scala 351:74 352:34 344:40]
  wire [31:0] _GEN_64 = CUId_0 == 2'h1 & io_validArbiter_0 & regOrder_0 == 2'h2 ? io_dataIn_v0_0_0 : 32'h0; // @[operandCollector.scala 351:74 353:32 345:38]
  wire [1:0] _GEN_69 = CUId_0 == 2'h1 & io_validArbiter_0 & regOrder_0 == 2'h2 ? regOrder_0 : 2'h0; // @[operandCollector.scala 351:74 355:38 347:44]
  wire [31:0] _GEN_70 = CUId_0 == 2'h1 & io_validArbiter_0 & regOrder_0 == 2'h3 ? io_dataIn_rs_0_0 : 32'h0; // @[operandCollector.scala 351:74 352:34 344:40]
  wire [31:0] _GEN_71 = CUId_0 == 2'h1 & io_validArbiter_0 & regOrder_0 == 2'h3 ? io_dataIn_rs_0_1 : 32'h0; // @[operandCollector.scala 351:74 352:34 344:40]
  wire [31:0] _GEN_72 = CUId_0 == 2'h1 & io_validArbiter_0 & regOrder_0 == 2'h3 ? io_dataIn_rs_0_2 : 32'h0; // @[operandCollector.scala 351:74 352:34 344:40]
  wire [31:0] _GEN_73 = CUId_0 == 2'h1 & io_validArbiter_0 & regOrder_0 == 2'h3 ? io_dataIn_rs_0_3 : 32'h0; // @[operandCollector.scala 351:74 352:34 344:40]
  wire [31:0] _GEN_74 = CUId_0 == 2'h1 & io_validArbiter_0 & regOrder_0 == 2'h3 ? io_dataIn_v0_0_0 : 32'h0; // @[operandCollector.scala 351:74 353:32 345:38]
  wire [1:0] _GEN_79 = CUId_0 == 2'h1 & io_validArbiter_0 & regOrder_0 == 2'h3 ? regOrder_0 : 2'h0; // @[operandCollector.scala 351:74 355:38 347:44]
  wire [31:0] _GEN_80 = CUId_0 == 2'h2 & io_validArbiter_0 & regOrder_0 == 2'h0 ? io_dataIn_rs_0_0 : 32'h0; // @[operandCollector.scala 351:74 352:34 344:40]
  wire [31:0] _GEN_81 = CUId_0 == 2'h2 & io_validArbiter_0 & regOrder_0 == 2'h0 ? io_dataIn_rs_0_1 : 32'h0; // @[operandCollector.scala 351:74 352:34 344:40]
  wire [31:0] _GEN_82 = CUId_0 == 2'h2 & io_validArbiter_0 & regOrder_0 == 2'h0 ? io_dataIn_rs_0_2 : 32'h0; // @[operandCollector.scala 351:74 352:34 344:40]
  wire [31:0] _GEN_83 = CUId_0 == 2'h2 & io_validArbiter_0 & regOrder_0 == 2'h0 ? io_dataIn_rs_0_3 : 32'h0; // @[operandCollector.scala 351:74 352:34 344:40]
  wire [31:0] _GEN_84 = CUId_0 == 2'h2 & io_validArbiter_0 & regOrder_0 == 2'h0 ? io_dataIn_v0_0_0 : 32'h0; // @[operandCollector.scala 351:74 353:32 345:38]
  wire [1:0] _GEN_89 = CUId_0 == 2'h2 & io_validArbiter_0 & regOrder_0 == 2'h0 ? regOrder_0 : 2'h0; // @[operandCollector.scala 351:74 355:38 347:44]
  wire [31:0] _GEN_90 = CUId_0 == 2'h2 & io_validArbiter_0 & regOrder_0 == 2'h1 ? io_dataIn_rs_0_0 : 32'h0; // @[operandCollector.scala 351:74 352:34 344:40]
  wire [31:0] _GEN_91 = CUId_0 == 2'h2 & io_validArbiter_0 & regOrder_0 == 2'h1 ? io_dataIn_rs_0_1 : 32'h0; // @[operandCollector.scala 351:74 352:34 344:40]
  wire [31:0] _GEN_92 = CUId_0 == 2'h2 & io_validArbiter_0 & regOrder_0 == 2'h1 ? io_dataIn_rs_0_2 : 32'h0; // @[operandCollector.scala 351:74 352:34 344:40]
  wire [31:0] _GEN_93 = CUId_0 == 2'h2 & io_validArbiter_0 & regOrder_0 == 2'h1 ? io_dataIn_rs_0_3 : 32'h0; // @[operandCollector.scala 351:74 352:34 344:40]
  wire [31:0] _GEN_94 = CUId_0 == 2'h2 & io_validArbiter_0 & regOrder_0 == 2'h1 ? io_dataIn_v0_0_0 : 32'h0; // @[operandCollector.scala 351:74 353:32 345:38]
  wire [1:0] _GEN_99 = CUId_0 == 2'h2 & io_validArbiter_0 & regOrder_0 == 2'h1 ? regOrder_0 : 2'h0; // @[operandCollector.scala 351:74 355:38 347:44]
  wire [31:0] _GEN_100 = CUId_0 == 2'h2 & io_validArbiter_0 & regOrder_0 == 2'h2 ? io_dataIn_rs_0_0 : 32'h0; // @[operandCollector.scala 351:74 352:34 344:40]
  wire [31:0] _GEN_101 = CUId_0 == 2'h2 & io_validArbiter_0 & regOrder_0 == 2'h2 ? io_dataIn_rs_0_1 : 32'h0; // @[operandCollector.scala 351:74 352:34 344:40]
  wire [31:0] _GEN_102 = CUId_0 == 2'h2 & io_validArbiter_0 & regOrder_0 == 2'h2 ? io_dataIn_rs_0_2 : 32'h0; // @[operandCollector.scala 351:74 352:34 344:40]
  wire [31:0] _GEN_103 = CUId_0 == 2'h2 & io_validArbiter_0 & regOrder_0 == 2'h2 ? io_dataIn_rs_0_3 : 32'h0; // @[operandCollector.scala 351:74 352:34 344:40]
  wire [31:0] _GEN_104 = CUId_0 == 2'h2 & io_validArbiter_0 & regOrder_0 == 2'h2 ? io_dataIn_v0_0_0 : 32'h0; // @[operandCollector.scala 351:74 353:32 345:38]
  wire [1:0] _GEN_109 = CUId_0 == 2'h2 & io_validArbiter_0 & regOrder_0 == 2'h2 ? regOrder_0 : 2'h0; // @[operandCollector.scala 351:74 355:38 347:44]
  wire [31:0] _GEN_110 = CUId_0 == 2'h2 & io_validArbiter_0 & regOrder_0 == 2'h3 ? io_dataIn_rs_0_0 : 32'h0; // @[operandCollector.scala 351:74 352:34 344:40]
  wire [31:0] _GEN_111 = CUId_0 == 2'h2 & io_validArbiter_0 & regOrder_0 == 2'h3 ? io_dataIn_rs_0_1 : 32'h0; // @[operandCollector.scala 351:74 352:34 344:40]
  wire [31:0] _GEN_112 = CUId_0 == 2'h2 & io_validArbiter_0 & regOrder_0 == 2'h3 ? io_dataIn_rs_0_2 : 32'h0; // @[operandCollector.scala 351:74 352:34 344:40]
  wire [31:0] _GEN_113 = CUId_0 == 2'h2 & io_validArbiter_0 & regOrder_0 == 2'h3 ? io_dataIn_rs_0_3 : 32'h0; // @[operandCollector.scala 351:74 352:34 344:40]
  wire [31:0] _GEN_114 = CUId_0 == 2'h2 & io_validArbiter_0 & regOrder_0 == 2'h3 ? io_dataIn_v0_0_0 : 32'h0; // @[operandCollector.scala 351:74 353:32 345:38]
  wire [1:0] _GEN_119 = CUId_0 == 2'h2 & io_validArbiter_0 & regOrder_0 == 2'h3 ? regOrder_0 : 2'h0; // @[operandCollector.scala 351:74 355:38 347:44]
  wire [31:0] _GEN_120 = CUId_0 == 2'h3 & io_validArbiter_0 & regOrder_0 == 2'h0 ? io_dataIn_rs_0_0 : 32'h0; // @[operandCollector.scala 351:74 352:34 344:40]
  wire [31:0] _GEN_121 = CUId_0 == 2'h3 & io_validArbiter_0 & regOrder_0 == 2'h0 ? io_dataIn_rs_0_1 : 32'h0; // @[operandCollector.scala 351:74 352:34 344:40]
  wire [31:0] _GEN_122 = CUId_0 == 2'h3 & io_validArbiter_0 & regOrder_0 == 2'h0 ? io_dataIn_rs_0_2 : 32'h0; // @[operandCollector.scala 351:74 352:34 344:40]
  wire [31:0] _GEN_123 = CUId_0 == 2'h3 & io_validArbiter_0 & regOrder_0 == 2'h0 ? io_dataIn_rs_0_3 : 32'h0; // @[operandCollector.scala 351:74 352:34 344:40]
  wire [31:0] _GEN_124 = CUId_0 == 2'h3 & io_validArbiter_0 & regOrder_0 == 2'h0 ? io_dataIn_v0_0_0 : 32'h0; // @[operandCollector.scala 351:74 353:32 345:38]
  wire [1:0] _GEN_129 = CUId_0 == 2'h3 & io_validArbiter_0 & regOrder_0 == 2'h0 ? regOrder_0 : 2'h0; // @[operandCollector.scala 351:74 355:38 347:44]
  wire [31:0] _GEN_130 = CUId_0 == 2'h3 & io_validArbiter_0 & regOrder_0 == 2'h1 ? io_dataIn_rs_0_0 : 32'h0; // @[operandCollector.scala 351:74 352:34 344:40]
  wire [31:0] _GEN_131 = CUId_0 == 2'h3 & io_validArbiter_0 & regOrder_0 == 2'h1 ? io_dataIn_rs_0_1 : 32'h0; // @[operandCollector.scala 351:74 352:34 344:40]
  wire [31:0] _GEN_132 = CUId_0 == 2'h3 & io_validArbiter_0 & regOrder_0 == 2'h1 ? io_dataIn_rs_0_2 : 32'h0; // @[operandCollector.scala 351:74 352:34 344:40]
  wire [31:0] _GEN_133 = CUId_0 == 2'h3 & io_validArbiter_0 & regOrder_0 == 2'h1 ? io_dataIn_rs_0_3 : 32'h0; // @[operandCollector.scala 351:74 352:34 344:40]
  wire [31:0] _GEN_134 = CUId_0 == 2'h3 & io_validArbiter_0 & regOrder_0 == 2'h1 ? io_dataIn_v0_0_0 : 32'h0; // @[operandCollector.scala 351:74 353:32 345:38]
  wire [1:0] _GEN_139 = CUId_0 == 2'h3 & io_validArbiter_0 & regOrder_0 == 2'h1 ? regOrder_0 : 2'h0; // @[operandCollector.scala 351:74 355:38 347:44]
  wire [31:0] _GEN_140 = CUId_0 == 2'h3 & io_validArbiter_0 & regOrder_0 == 2'h2 ? io_dataIn_rs_0_0 : 32'h0; // @[operandCollector.scala 351:74 352:34 344:40]
  wire [31:0] _GEN_141 = CUId_0 == 2'h3 & io_validArbiter_0 & regOrder_0 == 2'h2 ? io_dataIn_rs_0_1 : 32'h0; // @[operandCollector.scala 351:74 352:34 344:40]
  wire [31:0] _GEN_142 = CUId_0 == 2'h3 & io_validArbiter_0 & regOrder_0 == 2'h2 ? io_dataIn_rs_0_2 : 32'h0; // @[operandCollector.scala 351:74 352:34 344:40]
  wire [31:0] _GEN_143 = CUId_0 == 2'h3 & io_validArbiter_0 & regOrder_0 == 2'h2 ? io_dataIn_rs_0_3 : 32'h0; // @[operandCollector.scala 351:74 352:34 344:40]
  wire [31:0] _GEN_144 = CUId_0 == 2'h3 & io_validArbiter_0 & regOrder_0 == 2'h2 ? io_dataIn_v0_0_0 : 32'h0; // @[operandCollector.scala 351:74 353:32 345:38]
  wire [1:0] _GEN_149 = CUId_0 == 2'h3 & io_validArbiter_0 & regOrder_0 == 2'h2 ? regOrder_0 : 2'h0; // @[operandCollector.scala 351:74 355:38 347:44]
  wire [31:0] _GEN_150 = CUId_0 == 2'h3 & io_validArbiter_0 & regOrder_0 == 2'h3 ? io_dataIn_rs_0_0 : 32'h0; // @[operandCollector.scala 351:74 352:34 344:40]
  wire [31:0] _GEN_151 = CUId_0 == 2'h3 & io_validArbiter_0 & regOrder_0 == 2'h3 ? io_dataIn_rs_0_1 : 32'h0; // @[operandCollector.scala 351:74 352:34 344:40]
  wire [31:0] _GEN_152 = CUId_0 == 2'h3 & io_validArbiter_0 & regOrder_0 == 2'h3 ? io_dataIn_rs_0_2 : 32'h0; // @[operandCollector.scala 351:74 352:34 344:40]
  wire [31:0] _GEN_153 = CUId_0 == 2'h3 & io_validArbiter_0 & regOrder_0 == 2'h3 ? io_dataIn_rs_0_3 : 32'h0; // @[operandCollector.scala 351:74 352:34 344:40]
  wire [31:0] _GEN_154 = CUId_0 == 2'h3 & io_validArbiter_0 & regOrder_0 == 2'h3 ? io_dataIn_v0_0_0 : 32'h0; // @[operandCollector.scala 351:74 353:32 345:38]
  wire [1:0] _GEN_159 = CUId_0 == 2'h3 & io_validArbiter_0 & regOrder_0 == 2'h3 ? regOrder_0 : 2'h0; // @[operandCollector.scala 351:74 355:38 347:44]
  wire [1:0] CUId_1 = _CUId_1_T[1:0]; // @[operandCollector.scala 328:18 333:13]
  wire [1:0] regOrder_1 = _GEN_6[1:0]; // @[operandCollector.scala 329:22 334:17]
  wire [31:0] _GEN_160 = CUId_1 == 2'h0 & io_validArbiter_1 & regOrder_1 == 2'h0 ? io_dataIn_rs_1_0 : _GEN_0; // @[operandCollector.scala 351:74 352:34]
  wire [31:0] _GEN_161 = CUId_1 == 2'h0 & io_validArbiter_1 & regOrder_1 == 2'h0 ? io_dataIn_rs_1_1 : _GEN_1; // @[operandCollector.scala 351:74 352:34]
  wire [31:0] _GEN_162 = CUId_1 == 2'h0 & io_validArbiter_1 & regOrder_1 == 2'h0 ? io_dataIn_rs_1_2 : _GEN_2; // @[operandCollector.scala 351:74 352:34]
  wire [31:0] _GEN_163 = CUId_1 == 2'h0 & io_validArbiter_1 & regOrder_1 == 2'h0 ? io_dataIn_rs_1_3 : _GEN_3; // @[operandCollector.scala 351:74 352:34]
  wire [31:0] _GEN_164 = CUId_1 == 2'h0 & io_validArbiter_1 & regOrder_1 == 2'h0 ? io_dataIn_v0_1_0 : _GEN_4; // @[operandCollector.scala 351:74 353:32]
  wire [1:0] _GEN_169 = CUId_1 == 2'h0 & io_validArbiter_1 & regOrder_1 == 2'h0 ? regOrder_1 : _GEN_9; // @[operandCollector.scala 351:74 355:38]
  wire [31:0] _GEN_170 = CUId_1 == 2'h0 & io_validArbiter_1 & regOrder_1 == 2'h1 ? io_dataIn_rs_1_0 : _GEN_10; // @[operandCollector.scala 351:74 352:34]
  wire [31:0] _GEN_171 = CUId_1 == 2'h0 & io_validArbiter_1 & regOrder_1 == 2'h1 ? io_dataIn_rs_1_1 : _GEN_11; // @[operandCollector.scala 351:74 352:34]
  wire [31:0] _GEN_172 = CUId_1 == 2'h0 & io_validArbiter_1 & regOrder_1 == 2'h1 ? io_dataIn_rs_1_2 : _GEN_12; // @[operandCollector.scala 351:74 352:34]
  wire [31:0] _GEN_173 = CUId_1 == 2'h0 & io_validArbiter_1 & regOrder_1 == 2'h1 ? io_dataIn_rs_1_3 : _GEN_13; // @[operandCollector.scala 351:74 352:34]
  wire [31:0] _GEN_174 = CUId_1 == 2'h0 & io_validArbiter_1 & regOrder_1 == 2'h1 ? io_dataIn_v0_1_0 : _GEN_14; // @[operandCollector.scala 351:74 353:32]
  wire [1:0] _GEN_179 = CUId_1 == 2'h0 & io_validArbiter_1 & regOrder_1 == 2'h1 ? regOrder_1 : _GEN_19; // @[operandCollector.scala 351:74 355:38]
  wire [31:0] _GEN_180 = CUId_1 == 2'h0 & io_validArbiter_1 & regOrder_1 == 2'h2 ? io_dataIn_rs_1_0 : _GEN_20; // @[operandCollector.scala 351:74 352:34]
  wire [31:0] _GEN_181 = CUId_1 == 2'h0 & io_validArbiter_1 & regOrder_1 == 2'h2 ? io_dataIn_rs_1_1 : _GEN_21; // @[operandCollector.scala 351:74 352:34]
  wire [31:0] _GEN_182 = CUId_1 == 2'h0 & io_validArbiter_1 & regOrder_1 == 2'h2 ? io_dataIn_rs_1_2 : _GEN_22; // @[operandCollector.scala 351:74 352:34]
  wire [31:0] _GEN_183 = CUId_1 == 2'h0 & io_validArbiter_1 & regOrder_1 == 2'h2 ? io_dataIn_rs_1_3 : _GEN_23; // @[operandCollector.scala 351:74 352:34]
  wire [31:0] _GEN_184 = CUId_1 == 2'h0 & io_validArbiter_1 & regOrder_1 == 2'h2 ? io_dataIn_v0_1_0 : _GEN_24; // @[operandCollector.scala 351:74 353:32]
  wire [1:0] _GEN_189 = CUId_1 == 2'h0 & io_validArbiter_1 & regOrder_1 == 2'h2 ? regOrder_1 : _GEN_29; // @[operandCollector.scala 351:74 355:38]
  wire [31:0] _GEN_190 = CUId_1 == 2'h0 & io_validArbiter_1 & regOrder_1 == 2'h3 ? io_dataIn_rs_1_0 : _GEN_30; // @[operandCollector.scala 351:74 352:34]
  wire [31:0] _GEN_191 = CUId_1 == 2'h0 & io_validArbiter_1 & regOrder_1 == 2'h3 ? io_dataIn_rs_1_1 : _GEN_31; // @[operandCollector.scala 351:74 352:34]
  wire [31:0] _GEN_192 = CUId_1 == 2'h0 & io_validArbiter_1 & regOrder_1 == 2'h3 ? io_dataIn_rs_1_2 : _GEN_32; // @[operandCollector.scala 351:74 352:34]
  wire [31:0] _GEN_193 = CUId_1 == 2'h0 & io_validArbiter_1 & regOrder_1 == 2'h3 ? io_dataIn_rs_1_3 : _GEN_33; // @[operandCollector.scala 351:74 352:34]
  wire [31:0] _GEN_194 = CUId_1 == 2'h0 & io_validArbiter_1 & regOrder_1 == 2'h3 ? io_dataIn_v0_1_0 : _GEN_34; // @[operandCollector.scala 351:74 353:32]
  wire [1:0] _GEN_199 = CUId_1 == 2'h0 & io_validArbiter_1 & regOrder_1 == 2'h3 ? regOrder_1 : _GEN_39; // @[operandCollector.scala 351:74 355:38]
  wire [31:0] _GEN_200 = CUId_1 == 2'h1 & io_validArbiter_1 & regOrder_1 == 2'h0 ? io_dataIn_rs_1_0 : _GEN_40; // @[operandCollector.scala 351:74 352:34]
  wire [31:0] _GEN_201 = CUId_1 == 2'h1 & io_validArbiter_1 & regOrder_1 == 2'h0 ? io_dataIn_rs_1_1 : _GEN_41; // @[operandCollector.scala 351:74 352:34]
  wire [31:0] _GEN_202 = CUId_1 == 2'h1 & io_validArbiter_1 & regOrder_1 == 2'h0 ? io_dataIn_rs_1_2 : _GEN_42; // @[operandCollector.scala 351:74 352:34]
  wire [31:0] _GEN_203 = CUId_1 == 2'h1 & io_validArbiter_1 & regOrder_1 == 2'h0 ? io_dataIn_rs_1_3 : _GEN_43; // @[operandCollector.scala 351:74 352:34]
  wire [31:0] _GEN_204 = CUId_1 == 2'h1 & io_validArbiter_1 & regOrder_1 == 2'h0 ? io_dataIn_v0_1_0 : _GEN_44; // @[operandCollector.scala 351:74 353:32]
  wire [1:0] _GEN_209 = CUId_1 == 2'h1 & io_validArbiter_1 & regOrder_1 == 2'h0 ? regOrder_1 : _GEN_49; // @[operandCollector.scala 351:74 355:38]
  wire [31:0] _GEN_210 = CUId_1 == 2'h1 & io_validArbiter_1 & regOrder_1 == 2'h1 ? io_dataIn_rs_1_0 : _GEN_50; // @[operandCollector.scala 351:74 352:34]
  wire [31:0] _GEN_211 = CUId_1 == 2'h1 & io_validArbiter_1 & regOrder_1 == 2'h1 ? io_dataIn_rs_1_1 : _GEN_51; // @[operandCollector.scala 351:74 352:34]
  wire [31:0] _GEN_212 = CUId_1 == 2'h1 & io_validArbiter_1 & regOrder_1 == 2'h1 ? io_dataIn_rs_1_2 : _GEN_52; // @[operandCollector.scala 351:74 352:34]
  wire [31:0] _GEN_213 = CUId_1 == 2'h1 & io_validArbiter_1 & regOrder_1 == 2'h1 ? io_dataIn_rs_1_3 : _GEN_53; // @[operandCollector.scala 351:74 352:34]
  wire [31:0] _GEN_214 = CUId_1 == 2'h1 & io_validArbiter_1 & regOrder_1 == 2'h1 ? io_dataIn_v0_1_0 : _GEN_54; // @[operandCollector.scala 351:74 353:32]
  wire [1:0] _GEN_219 = CUId_1 == 2'h1 & io_validArbiter_1 & regOrder_1 == 2'h1 ? regOrder_1 : _GEN_59; // @[operandCollector.scala 351:74 355:38]
  wire [31:0] _GEN_220 = CUId_1 == 2'h1 & io_validArbiter_1 & regOrder_1 == 2'h2 ? io_dataIn_rs_1_0 : _GEN_60; // @[operandCollector.scala 351:74 352:34]
  wire [31:0] _GEN_221 = CUId_1 == 2'h1 & io_validArbiter_1 & regOrder_1 == 2'h2 ? io_dataIn_rs_1_1 : _GEN_61; // @[operandCollector.scala 351:74 352:34]
  wire [31:0] _GEN_222 = CUId_1 == 2'h1 & io_validArbiter_1 & regOrder_1 == 2'h2 ? io_dataIn_rs_1_2 : _GEN_62; // @[operandCollector.scala 351:74 352:34]
  wire [31:0] _GEN_223 = CUId_1 == 2'h1 & io_validArbiter_1 & regOrder_1 == 2'h2 ? io_dataIn_rs_1_3 : _GEN_63; // @[operandCollector.scala 351:74 352:34]
  wire [31:0] _GEN_224 = CUId_1 == 2'h1 & io_validArbiter_1 & regOrder_1 == 2'h2 ? io_dataIn_v0_1_0 : _GEN_64; // @[operandCollector.scala 351:74 353:32]
  wire [1:0] _GEN_229 = CUId_1 == 2'h1 & io_validArbiter_1 & regOrder_1 == 2'h2 ? regOrder_1 : _GEN_69; // @[operandCollector.scala 351:74 355:38]
  wire [31:0] _GEN_230 = CUId_1 == 2'h1 & io_validArbiter_1 & regOrder_1 == 2'h3 ? io_dataIn_rs_1_0 : _GEN_70; // @[operandCollector.scala 351:74 352:34]
  wire [31:0] _GEN_231 = CUId_1 == 2'h1 & io_validArbiter_1 & regOrder_1 == 2'h3 ? io_dataIn_rs_1_1 : _GEN_71; // @[operandCollector.scala 351:74 352:34]
  wire [31:0] _GEN_232 = CUId_1 == 2'h1 & io_validArbiter_1 & regOrder_1 == 2'h3 ? io_dataIn_rs_1_2 : _GEN_72; // @[operandCollector.scala 351:74 352:34]
  wire [31:0] _GEN_233 = CUId_1 == 2'h1 & io_validArbiter_1 & regOrder_1 == 2'h3 ? io_dataIn_rs_1_3 : _GEN_73; // @[operandCollector.scala 351:74 352:34]
  wire [31:0] _GEN_234 = CUId_1 == 2'h1 & io_validArbiter_1 & regOrder_1 == 2'h3 ? io_dataIn_v0_1_0 : _GEN_74; // @[operandCollector.scala 351:74 353:32]
  wire [1:0] _GEN_239 = CUId_1 == 2'h1 & io_validArbiter_1 & regOrder_1 == 2'h3 ? regOrder_1 : _GEN_79; // @[operandCollector.scala 351:74 355:38]
  wire [31:0] _GEN_240 = CUId_1 == 2'h2 & io_validArbiter_1 & regOrder_1 == 2'h0 ? io_dataIn_rs_1_0 : _GEN_80; // @[operandCollector.scala 351:74 352:34]
  wire [31:0] _GEN_241 = CUId_1 == 2'h2 & io_validArbiter_1 & regOrder_1 == 2'h0 ? io_dataIn_rs_1_1 : _GEN_81; // @[operandCollector.scala 351:74 352:34]
  wire [31:0] _GEN_242 = CUId_1 == 2'h2 & io_validArbiter_1 & regOrder_1 == 2'h0 ? io_dataIn_rs_1_2 : _GEN_82; // @[operandCollector.scala 351:74 352:34]
  wire [31:0] _GEN_243 = CUId_1 == 2'h2 & io_validArbiter_1 & regOrder_1 == 2'h0 ? io_dataIn_rs_1_3 : _GEN_83; // @[operandCollector.scala 351:74 352:34]
  wire [31:0] _GEN_244 = CUId_1 == 2'h2 & io_validArbiter_1 & regOrder_1 == 2'h0 ? io_dataIn_v0_1_0 : _GEN_84; // @[operandCollector.scala 351:74 353:32]
  wire [1:0] _GEN_249 = CUId_1 == 2'h2 & io_validArbiter_1 & regOrder_1 == 2'h0 ? regOrder_1 : _GEN_89; // @[operandCollector.scala 351:74 355:38]
  wire [31:0] _GEN_250 = CUId_1 == 2'h2 & io_validArbiter_1 & regOrder_1 == 2'h1 ? io_dataIn_rs_1_0 : _GEN_90; // @[operandCollector.scala 351:74 352:34]
  wire [31:0] _GEN_251 = CUId_1 == 2'h2 & io_validArbiter_1 & regOrder_1 == 2'h1 ? io_dataIn_rs_1_1 : _GEN_91; // @[operandCollector.scala 351:74 352:34]
  wire [31:0] _GEN_252 = CUId_1 == 2'h2 & io_validArbiter_1 & regOrder_1 == 2'h1 ? io_dataIn_rs_1_2 : _GEN_92; // @[operandCollector.scala 351:74 352:34]
  wire [31:0] _GEN_253 = CUId_1 == 2'h2 & io_validArbiter_1 & regOrder_1 == 2'h1 ? io_dataIn_rs_1_3 : _GEN_93; // @[operandCollector.scala 351:74 352:34]
  wire [31:0] _GEN_254 = CUId_1 == 2'h2 & io_validArbiter_1 & regOrder_1 == 2'h1 ? io_dataIn_v0_1_0 : _GEN_94; // @[operandCollector.scala 351:74 353:32]
  wire [1:0] _GEN_259 = CUId_1 == 2'h2 & io_validArbiter_1 & regOrder_1 == 2'h1 ? regOrder_1 : _GEN_99; // @[operandCollector.scala 351:74 355:38]
  wire [31:0] _GEN_260 = CUId_1 == 2'h2 & io_validArbiter_1 & regOrder_1 == 2'h2 ? io_dataIn_rs_1_0 : _GEN_100; // @[operandCollector.scala 351:74 352:34]
  wire [31:0] _GEN_261 = CUId_1 == 2'h2 & io_validArbiter_1 & regOrder_1 == 2'h2 ? io_dataIn_rs_1_1 : _GEN_101; // @[operandCollector.scala 351:74 352:34]
  wire [31:0] _GEN_262 = CUId_1 == 2'h2 & io_validArbiter_1 & regOrder_1 == 2'h2 ? io_dataIn_rs_1_2 : _GEN_102; // @[operandCollector.scala 351:74 352:34]
  wire [31:0] _GEN_263 = CUId_1 == 2'h2 & io_validArbiter_1 & regOrder_1 == 2'h2 ? io_dataIn_rs_1_3 : _GEN_103; // @[operandCollector.scala 351:74 352:34]
  wire [31:0] _GEN_264 = CUId_1 == 2'h2 & io_validArbiter_1 & regOrder_1 == 2'h2 ? io_dataIn_v0_1_0 : _GEN_104; // @[operandCollector.scala 351:74 353:32]
  wire [1:0] _GEN_269 = CUId_1 == 2'h2 & io_validArbiter_1 & regOrder_1 == 2'h2 ? regOrder_1 : _GEN_109; // @[operandCollector.scala 351:74 355:38]
  wire [31:0] _GEN_270 = CUId_1 == 2'h2 & io_validArbiter_1 & regOrder_1 == 2'h3 ? io_dataIn_rs_1_0 : _GEN_110; // @[operandCollector.scala 351:74 352:34]
  wire [31:0] _GEN_271 = CUId_1 == 2'h2 & io_validArbiter_1 & regOrder_1 == 2'h3 ? io_dataIn_rs_1_1 : _GEN_111; // @[operandCollector.scala 351:74 352:34]
  wire [31:0] _GEN_272 = CUId_1 == 2'h2 & io_validArbiter_1 & regOrder_1 == 2'h3 ? io_dataIn_rs_1_2 : _GEN_112; // @[operandCollector.scala 351:74 352:34]
  wire [31:0] _GEN_273 = CUId_1 == 2'h2 & io_validArbiter_1 & regOrder_1 == 2'h3 ? io_dataIn_rs_1_3 : _GEN_113; // @[operandCollector.scala 351:74 352:34]
  wire [31:0] _GEN_274 = CUId_1 == 2'h2 & io_validArbiter_1 & regOrder_1 == 2'h3 ? io_dataIn_v0_1_0 : _GEN_114; // @[operandCollector.scala 351:74 353:32]
  wire [1:0] _GEN_279 = CUId_1 == 2'h2 & io_validArbiter_1 & regOrder_1 == 2'h3 ? regOrder_1 : _GEN_119; // @[operandCollector.scala 351:74 355:38]
  wire [31:0] _GEN_280 = CUId_1 == 2'h3 & io_validArbiter_1 & regOrder_1 == 2'h0 ? io_dataIn_rs_1_0 : _GEN_120; // @[operandCollector.scala 351:74 352:34]
  wire [31:0] _GEN_281 = CUId_1 == 2'h3 & io_validArbiter_1 & regOrder_1 == 2'h0 ? io_dataIn_rs_1_1 : _GEN_121; // @[operandCollector.scala 351:74 352:34]
  wire [31:0] _GEN_282 = CUId_1 == 2'h3 & io_validArbiter_1 & regOrder_1 == 2'h0 ? io_dataIn_rs_1_2 : _GEN_122; // @[operandCollector.scala 351:74 352:34]
  wire [31:0] _GEN_283 = CUId_1 == 2'h3 & io_validArbiter_1 & regOrder_1 == 2'h0 ? io_dataIn_rs_1_3 : _GEN_123; // @[operandCollector.scala 351:74 352:34]
  wire [31:0] _GEN_284 = CUId_1 == 2'h3 & io_validArbiter_1 & regOrder_1 == 2'h0 ? io_dataIn_v0_1_0 : _GEN_124; // @[operandCollector.scala 351:74 353:32]
  wire [1:0] _GEN_289 = CUId_1 == 2'h3 & io_validArbiter_1 & regOrder_1 == 2'h0 ? regOrder_1 : _GEN_129; // @[operandCollector.scala 351:74 355:38]
  wire [31:0] _GEN_290 = CUId_1 == 2'h3 & io_validArbiter_1 & regOrder_1 == 2'h1 ? io_dataIn_rs_1_0 : _GEN_130; // @[operandCollector.scala 351:74 352:34]
  wire [31:0] _GEN_291 = CUId_1 == 2'h3 & io_validArbiter_1 & regOrder_1 == 2'h1 ? io_dataIn_rs_1_1 : _GEN_131; // @[operandCollector.scala 351:74 352:34]
  wire [31:0] _GEN_292 = CUId_1 == 2'h3 & io_validArbiter_1 & regOrder_1 == 2'h1 ? io_dataIn_rs_1_2 : _GEN_132; // @[operandCollector.scala 351:74 352:34]
  wire [31:0] _GEN_293 = CUId_1 == 2'h3 & io_validArbiter_1 & regOrder_1 == 2'h1 ? io_dataIn_rs_1_3 : _GEN_133; // @[operandCollector.scala 351:74 352:34]
  wire [31:0] _GEN_294 = CUId_1 == 2'h3 & io_validArbiter_1 & regOrder_1 == 2'h1 ? io_dataIn_v0_1_0 : _GEN_134; // @[operandCollector.scala 351:74 353:32]
  wire [1:0] _GEN_299 = CUId_1 == 2'h3 & io_validArbiter_1 & regOrder_1 == 2'h1 ? regOrder_1 : _GEN_139; // @[operandCollector.scala 351:74 355:38]
  wire [31:0] _GEN_300 = CUId_1 == 2'h3 & io_validArbiter_1 & regOrder_1 == 2'h2 ? io_dataIn_rs_1_0 : _GEN_140; // @[operandCollector.scala 351:74 352:34]
  wire [31:0] _GEN_301 = CUId_1 == 2'h3 & io_validArbiter_1 & regOrder_1 == 2'h2 ? io_dataIn_rs_1_1 : _GEN_141; // @[operandCollector.scala 351:74 352:34]
  wire [31:0] _GEN_302 = CUId_1 == 2'h3 & io_validArbiter_1 & regOrder_1 == 2'h2 ? io_dataIn_rs_1_2 : _GEN_142; // @[operandCollector.scala 351:74 352:34]
  wire [31:0] _GEN_303 = CUId_1 == 2'h3 & io_validArbiter_1 & regOrder_1 == 2'h2 ? io_dataIn_rs_1_3 : _GEN_143; // @[operandCollector.scala 351:74 352:34]
  wire [31:0] _GEN_304 = CUId_1 == 2'h3 & io_validArbiter_1 & regOrder_1 == 2'h2 ? io_dataIn_v0_1_0 : _GEN_144; // @[operandCollector.scala 351:74 353:32]
  wire [1:0] _GEN_309 = CUId_1 == 2'h3 & io_validArbiter_1 & regOrder_1 == 2'h2 ? regOrder_1 : _GEN_149; // @[operandCollector.scala 351:74 355:38]
  wire [31:0] _GEN_310 = CUId_1 == 2'h3 & io_validArbiter_1 & regOrder_1 == 2'h3 ? io_dataIn_rs_1_0 : _GEN_150; // @[operandCollector.scala 351:74 352:34]
  wire [31:0] _GEN_311 = CUId_1 == 2'h3 & io_validArbiter_1 & regOrder_1 == 2'h3 ? io_dataIn_rs_1_1 : _GEN_151; // @[operandCollector.scala 351:74 352:34]
  wire [31:0] _GEN_312 = CUId_1 == 2'h3 & io_validArbiter_1 & regOrder_1 == 2'h3 ? io_dataIn_rs_1_2 : _GEN_152; // @[operandCollector.scala 351:74 352:34]
  wire [31:0] _GEN_313 = CUId_1 == 2'h3 & io_validArbiter_1 & regOrder_1 == 2'h3 ? io_dataIn_rs_1_3 : _GEN_153; // @[operandCollector.scala 351:74 352:34]
  wire [31:0] _GEN_314 = CUId_1 == 2'h3 & io_validArbiter_1 & regOrder_1 == 2'h3 ? io_dataIn_v0_1_0 : _GEN_154; // @[operandCollector.scala 351:74 353:32]
  wire [1:0] _GEN_319 = CUId_1 == 2'h3 & io_validArbiter_1 & regOrder_1 == 2'h3 ? regOrder_1 : _GEN_159; // @[operandCollector.scala 351:74 355:38]
  wire [1:0] CUId_2 = _CUId_2_T[1:0]; // @[operandCollector.scala 328:18 333:13]
  wire [1:0] regOrder_2 = _GEN_7[1:0]; // @[operandCollector.scala 329:22 334:17]
  wire [31:0] _GEN_320 = CUId_2 == 2'h0 & io_validArbiter_2 & regOrder_2 == 2'h0 ? io_dataIn_rs_2_0 : _GEN_160; // @[operandCollector.scala 351:74 352:34]
  wire [31:0] _GEN_321 = CUId_2 == 2'h0 & io_validArbiter_2 & regOrder_2 == 2'h0 ? io_dataIn_rs_2_1 : _GEN_161; // @[operandCollector.scala 351:74 352:34]
  wire [31:0] _GEN_322 = CUId_2 == 2'h0 & io_validArbiter_2 & regOrder_2 == 2'h0 ? io_dataIn_rs_2_2 : _GEN_162; // @[operandCollector.scala 351:74 352:34]
  wire [31:0] _GEN_323 = CUId_2 == 2'h0 & io_validArbiter_2 & regOrder_2 == 2'h0 ? io_dataIn_rs_2_3 : _GEN_163; // @[operandCollector.scala 351:74 352:34]
  wire [31:0] _GEN_324 = CUId_2 == 2'h0 & io_validArbiter_2 & regOrder_2 == 2'h0 ? io_dataIn_v0_2_0 : _GEN_164; // @[operandCollector.scala 351:74 353:32]
  wire [1:0] _GEN_329 = CUId_2 == 2'h0 & io_validArbiter_2 & regOrder_2 == 2'h0 ? regOrder_2 : _GEN_169; // @[operandCollector.scala 351:74 355:38]
  wire [31:0] _GEN_330 = CUId_2 == 2'h0 & io_validArbiter_2 & regOrder_2 == 2'h1 ? io_dataIn_rs_2_0 : _GEN_170; // @[operandCollector.scala 351:74 352:34]
  wire [31:0] _GEN_331 = CUId_2 == 2'h0 & io_validArbiter_2 & regOrder_2 == 2'h1 ? io_dataIn_rs_2_1 : _GEN_171; // @[operandCollector.scala 351:74 352:34]
  wire [31:0] _GEN_332 = CUId_2 == 2'h0 & io_validArbiter_2 & regOrder_2 == 2'h1 ? io_dataIn_rs_2_2 : _GEN_172; // @[operandCollector.scala 351:74 352:34]
  wire [31:0] _GEN_333 = CUId_2 == 2'h0 & io_validArbiter_2 & regOrder_2 == 2'h1 ? io_dataIn_rs_2_3 : _GEN_173; // @[operandCollector.scala 351:74 352:34]
  wire [31:0] _GEN_334 = CUId_2 == 2'h0 & io_validArbiter_2 & regOrder_2 == 2'h1 ? io_dataIn_v0_2_0 : _GEN_174; // @[operandCollector.scala 351:74 353:32]
  wire [1:0] _GEN_339 = CUId_2 == 2'h0 & io_validArbiter_2 & regOrder_2 == 2'h1 ? regOrder_2 : _GEN_179; // @[operandCollector.scala 351:74 355:38]
  wire [31:0] _GEN_340 = CUId_2 == 2'h0 & io_validArbiter_2 & regOrder_2 == 2'h2 ? io_dataIn_rs_2_0 : _GEN_180; // @[operandCollector.scala 351:74 352:34]
  wire [31:0] _GEN_341 = CUId_2 == 2'h0 & io_validArbiter_2 & regOrder_2 == 2'h2 ? io_dataIn_rs_2_1 : _GEN_181; // @[operandCollector.scala 351:74 352:34]
  wire [31:0] _GEN_342 = CUId_2 == 2'h0 & io_validArbiter_2 & regOrder_2 == 2'h2 ? io_dataIn_rs_2_2 : _GEN_182; // @[operandCollector.scala 351:74 352:34]
  wire [31:0] _GEN_343 = CUId_2 == 2'h0 & io_validArbiter_2 & regOrder_2 == 2'h2 ? io_dataIn_rs_2_3 : _GEN_183; // @[operandCollector.scala 351:74 352:34]
  wire [31:0] _GEN_344 = CUId_2 == 2'h0 & io_validArbiter_2 & regOrder_2 == 2'h2 ? io_dataIn_v0_2_0 : _GEN_184; // @[operandCollector.scala 351:74 353:32]
  wire [1:0] _GEN_349 = CUId_2 == 2'h0 & io_validArbiter_2 & regOrder_2 == 2'h2 ? regOrder_2 : _GEN_189; // @[operandCollector.scala 351:74 355:38]
  wire [31:0] _GEN_350 = CUId_2 == 2'h0 & io_validArbiter_2 & regOrder_2 == 2'h3 ? io_dataIn_rs_2_0 : _GEN_190; // @[operandCollector.scala 351:74 352:34]
  wire [31:0] _GEN_351 = CUId_2 == 2'h0 & io_validArbiter_2 & regOrder_2 == 2'h3 ? io_dataIn_rs_2_1 : _GEN_191; // @[operandCollector.scala 351:74 352:34]
  wire [31:0] _GEN_352 = CUId_2 == 2'h0 & io_validArbiter_2 & regOrder_2 == 2'h3 ? io_dataIn_rs_2_2 : _GEN_192; // @[operandCollector.scala 351:74 352:34]
  wire [31:0] _GEN_353 = CUId_2 == 2'h0 & io_validArbiter_2 & regOrder_2 == 2'h3 ? io_dataIn_rs_2_3 : _GEN_193; // @[operandCollector.scala 351:74 352:34]
  wire [31:0] _GEN_354 = CUId_2 == 2'h0 & io_validArbiter_2 & regOrder_2 == 2'h3 ? io_dataIn_v0_2_0 : _GEN_194; // @[operandCollector.scala 351:74 353:32]
  wire [1:0] _GEN_359 = CUId_2 == 2'h0 & io_validArbiter_2 & regOrder_2 == 2'h3 ? regOrder_2 : _GEN_199; // @[operandCollector.scala 351:74 355:38]
  wire [31:0] _GEN_360 = CUId_2 == 2'h1 & io_validArbiter_2 & regOrder_2 == 2'h0 ? io_dataIn_rs_2_0 : _GEN_200; // @[operandCollector.scala 351:74 352:34]
  wire [31:0] _GEN_361 = CUId_2 == 2'h1 & io_validArbiter_2 & regOrder_2 == 2'h0 ? io_dataIn_rs_2_1 : _GEN_201; // @[operandCollector.scala 351:74 352:34]
  wire [31:0] _GEN_362 = CUId_2 == 2'h1 & io_validArbiter_2 & regOrder_2 == 2'h0 ? io_dataIn_rs_2_2 : _GEN_202; // @[operandCollector.scala 351:74 352:34]
  wire [31:0] _GEN_363 = CUId_2 == 2'h1 & io_validArbiter_2 & regOrder_2 == 2'h0 ? io_dataIn_rs_2_3 : _GEN_203; // @[operandCollector.scala 351:74 352:34]
  wire [31:0] _GEN_364 = CUId_2 == 2'h1 & io_validArbiter_2 & regOrder_2 == 2'h0 ? io_dataIn_v0_2_0 : _GEN_204; // @[operandCollector.scala 351:74 353:32]
  wire [1:0] _GEN_369 = CUId_2 == 2'h1 & io_validArbiter_2 & regOrder_2 == 2'h0 ? regOrder_2 : _GEN_209; // @[operandCollector.scala 351:74 355:38]
  wire [31:0] _GEN_370 = CUId_2 == 2'h1 & io_validArbiter_2 & regOrder_2 == 2'h1 ? io_dataIn_rs_2_0 : _GEN_210; // @[operandCollector.scala 351:74 352:34]
  wire [31:0] _GEN_371 = CUId_2 == 2'h1 & io_validArbiter_2 & regOrder_2 == 2'h1 ? io_dataIn_rs_2_1 : _GEN_211; // @[operandCollector.scala 351:74 352:34]
  wire [31:0] _GEN_372 = CUId_2 == 2'h1 & io_validArbiter_2 & regOrder_2 == 2'h1 ? io_dataIn_rs_2_2 : _GEN_212; // @[operandCollector.scala 351:74 352:34]
  wire [31:0] _GEN_373 = CUId_2 == 2'h1 & io_validArbiter_2 & regOrder_2 == 2'h1 ? io_dataIn_rs_2_3 : _GEN_213; // @[operandCollector.scala 351:74 352:34]
  wire [31:0] _GEN_374 = CUId_2 == 2'h1 & io_validArbiter_2 & regOrder_2 == 2'h1 ? io_dataIn_v0_2_0 : _GEN_214; // @[operandCollector.scala 351:74 353:32]
  wire [1:0] _GEN_379 = CUId_2 == 2'h1 & io_validArbiter_2 & regOrder_2 == 2'h1 ? regOrder_2 : _GEN_219; // @[operandCollector.scala 351:74 355:38]
  wire [31:0] _GEN_380 = CUId_2 == 2'h1 & io_validArbiter_2 & regOrder_2 == 2'h2 ? io_dataIn_rs_2_0 : _GEN_220; // @[operandCollector.scala 351:74 352:34]
  wire [31:0] _GEN_381 = CUId_2 == 2'h1 & io_validArbiter_2 & regOrder_2 == 2'h2 ? io_dataIn_rs_2_1 : _GEN_221; // @[operandCollector.scala 351:74 352:34]
  wire [31:0] _GEN_382 = CUId_2 == 2'h1 & io_validArbiter_2 & regOrder_2 == 2'h2 ? io_dataIn_rs_2_2 : _GEN_222; // @[operandCollector.scala 351:74 352:34]
  wire [31:0] _GEN_383 = CUId_2 == 2'h1 & io_validArbiter_2 & regOrder_2 == 2'h2 ? io_dataIn_rs_2_3 : _GEN_223; // @[operandCollector.scala 351:74 352:34]
  wire [31:0] _GEN_384 = CUId_2 == 2'h1 & io_validArbiter_2 & regOrder_2 == 2'h2 ? io_dataIn_v0_2_0 : _GEN_224; // @[operandCollector.scala 351:74 353:32]
  wire [1:0] _GEN_389 = CUId_2 == 2'h1 & io_validArbiter_2 & regOrder_2 == 2'h2 ? regOrder_2 : _GEN_229; // @[operandCollector.scala 351:74 355:38]
  wire [31:0] _GEN_390 = CUId_2 == 2'h1 & io_validArbiter_2 & regOrder_2 == 2'h3 ? io_dataIn_rs_2_0 : _GEN_230; // @[operandCollector.scala 351:74 352:34]
  wire [31:0] _GEN_391 = CUId_2 == 2'h1 & io_validArbiter_2 & regOrder_2 == 2'h3 ? io_dataIn_rs_2_1 : _GEN_231; // @[operandCollector.scala 351:74 352:34]
  wire [31:0] _GEN_392 = CUId_2 == 2'h1 & io_validArbiter_2 & regOrder_2 == 2'h3 ? io_dataIn_rs_2_2 : _GEN_232; // @[operandCollector.scala 351:74 352:34]
  wire [31:0] _GEN_393 = CUId_2 == 2'h1 & io_validArbiter_2 & regOrder_2 == 2'h3 ? io_dataIn_rs_2_3 : _GEN_233; // @[operandCollector.scala 351:74 352:34]
  wire [31:0] _GEN_394 = CUId_2 == 2'h1 & io_validArbiter_2 & regOrder_2 == 2'h3 ? io_dataIn_v0_2_0 : _GEN_234; // @[operandCollector.scala 351:74 353:32]
  wire [1:0] _GEN_399 = CUId_2 == 2'h1 & io_validArbiter_2 & regOrder_2 == 2'h3 ? regOrder_2 : _GEN_239; // @[operandCollector.scala 351:74 355:38]
  wire [31:0] _GEN_400 = CUId_2 == 2'h2 & io_validArbiter_2 & regOrder_2 == 2'h0 ? io_dataIn_rs_2_0 : _GEN_240; // @[operandCollector.scala 351:74 352:34]
  wire [31:0] _GEN_401 = CUId_2 == 2'h2 & io_validArbiter_2 & regOrder_2 == 2'h0 ? io_dataIn_rs_2_1 : _GEN_241; // @[operandCollector.scala 351:74 352:34]
  wire [31:0] _GEN_402 = CUId_2 == 2'h2 & io_validArbiter_2 & regOrder_2 == 2'h0 ? io_dataIn_rs_2_2 : _GEN_242; // @[operandCollector.scala 351:74 352:34]
  wire [31:0] _GEN_403 = CUId_2 == 2'h2 & io_validArbiter_2 & regOrder_2 == 2'h0 ? io_dataIn_rs_2_3 : _GEN_243; // @[operandCollector.scala 351:74 352:34]
  wire [31:0] _GEN_404 = CUId_2 == 2'h2 & io_validArbiter_2 & regOrder_2 == 2'h0 ? io_dataIn_v0_2_0 : _GEN_244; // @[operandCollector.scala 351:74 353:32]
  wire [1:0] _GEN_409 = CUId_2 == 2'h2 & io_validArbiter_2 & regOrder_2 == 2'h0 ? regOrder_2 : _GEN_249; // @[operandCollector.scala 351:74 355:38]
  wire [31:0] _GEN_410 = CUId_2 == 2'h2 & io_validArbiter_2 & regOrder_2 == 2'h1 ? io_dataIn_rs_2_0 : _GEN_250; // @[operandCollector.scala 351:74 352:34]
  wire [31:0] _GEN_411 = CUId_2 == 2'h2 & io_validArbiter_2 & regOrder_2 == 2'h1 ? io_dataIn_rs_2_1 : _GEN_251; // @[operandCollector.scala 351:74 352:34]
  wire [31:0] _GEN_412 = CUId_2 == 2'h2 & io_validArbiter_2 & regOrder_2 == 2'h1 ? io_dataIn_rs_2_2 : _GEN_252; // @[operandCollector.scala 351:74 352:34]
  wire [31:0] _GEN_413 = CUId_2 == 2'h2 & io_validArbiter_2 & regOrder_2 == 2'h1 ? io_dataIn_rs_2_3 : _GEN_253; // @[operandCollector.scala 351:74 352:34]
  wire [31:0] _GEN_414 = CUId_2 == 2'h2 & io_validArbiter_2 & regOrder_2 == 2'h1 ? io_dataIn_v0_2_0 : _GEN_254; // @[operandCollector.scala 351:74 353:32]
  wire [1:0] _GEN_419 = CUId_2 == 2'h2 & io_validArbiter_2 & regOrder_2 == 2'h1 ? regOrder_2 : _GEN_259; // @[operandCollector.scala 351:74 355:38]
  wire [31:0] _GEN_420 = CUId_2 == 2'h2 & io_validArbiter_2 & regOrder_2 == 2'h2 ? io_dataIn_rs_2_0 : _GEN_260; // @[operandCollector.scala 351:74 352:34]
  wire [31:0] _GEN_421 = CUId_2 == 2'h2 & io_validArbiter_2 & regOrder_2 == 2'h2 ? io_dataIn_rs_2_1 : _GEN_261; // @[operandCollector.scala 351:74 352:34]
  wire [31:0] _GEN_422 = CUId_2 == 2'h2 & io_validArbiter_2 & regOrder_2 == 2'h2 ? io_dataIn_rs_2_2 : _GEN_262; // @[operandCollector.scala 351:74 352:34]
  wire [31:0] _GEN_423 = CUId_2 == 2'h2 & io_validArbiter_2 & regOrder_2 == 2'h2 ? io_dataIn_rs_2_3 : _GEN_263; // @[operandCollector.scala 351:74 352:34]
  wire [31:0] _GEN_424 = CUId_2 == 2'h2 & io_validArbiter_2 & regOrder_2 == 2'h2 ? io_dataIn_v0_2_0 : _GEN_264; // @[operandCollector.scala 351:74 353:32]
  wire [1:0] _GEN_429 = CUId_2 == 2'h2 & io_validArbiter_2 & regOrder_2 == 2'h2 ? regOrder_2 : _GEN_269; // @[operandCollector.scala 351:74 355:38]
  wire [31:0] _GEN_430 = CUId_2 == 2'h2 & io_validArbiter_2 & regOrder_2 == 2'h3 ? io_dataIn_rs_2_0 : _GEN_270; // @[operandCollector.scala 351:74 352:34]
  wire [31:0] _GEN_431 = CUId_2 == 2'h2 & io_validArbiter_2 & regOrder_2 == 2'h3 ? io_dataIn_rs_2_1 : _GEN_271; // @[operandCollector.scala 351:74 352:34]
  wire [31:0] _GEN_432 = CUId_2 == 2'h2 & io_validArbiter_2 & regOrder_2 == 2'h3 ? io_dataIn_rs_2_2 : _GEN_272; // @[operandCollector.scala 351:74 352:34]
  wire [31:0] _GEN_433 = CUId_2 == 2'h2 & io_validArbiter_2 & regOrder_2 == 2'h3 ? io_dataIn_rs_2_3 : _GEN_273; // @[operandCollector.scala 351:74 352:34]
  wire [31:0] _GEN_434 = CUId_2 == 2'h2 & io_validArbiter_2 & regOrder_2 == 2'h3 ? io_dataIn_v0_2_0 : _GEN_274; // @[operandCollector.scala 351:74 353:32]
  wire [1:0] _GEN_439 = CUId_2 == 2'h2 & io_validArbiter_2 & regOrder_2 == 2'h3 ? regOrder_2 : _GEN_279; // @[operandCollector.scala 351:74 355:38]
  wire [31:0] _GEN_440 = CUId_2 == 2'h3 & io_validArbiter_2 & regOrder_2 == 2'h0 ? io_dataIn_rs_2_0 : _GEN_280; // @[operandCollector.scala 351:74 352:34]
  wire [31:0] _GEN_441 = CUId_2 == 2'h3 & io_validArbiter_2 & regOrder_2 == 2'h0 ? io_dataIn_rs_2_1 : _GEN_281; // @[operandCollector.scala 351:74 352:34]
  wire [31:0] _GEN_442 = CUId_2 == 2'h3 & io_validArbiter_2 & regOrder_2 == 2'h0 ? io_dataIn_rs_2_2 : _GEN_282; // @[operandCollector.scala 351:74 352:34]
  wire [31:0] _GEN_443 = CUId_2 == 2'h3 & io_validArbiter_2 & regOrder_2 == 2'h0 ? io_dataIn_rs_2_3 : _GEN_283; // @[operandCollector.scala 351:74 352:34]
  wire [31:0] _GEN_444 = CUId_2 == 2'h3 & io_validArbiter_2 & regOrder_2 == 2'h0 ? io_dataIn_v0_2_0 : _GEN_284; // @[operandCollector.scala 351:74 353:32]
  wire [1:0] _GEN_449 = CUId_2 == 2'h3 & io_validArbiter_2 & regOrder_2 == 2'h0 ? regOrder_2 : _GEN_289; // @[operandCollector.scala 351:74 355:38]
  wire [31:0] _GEN_450 = CUId_2 == 2'h3 & io_validArbiter_2 & regOrder_2 == 2'h1 ? io_dataIn_rs_2_0 : _GEN_290; // @[operandCollector.scala 351:74 352:34]
  wire [31:0] _GEN_451 = CUId_2 == 2'h3 & io_validArbiter_2 & regOrder_2 == 2'h1 ? io_dataIn_rs_2_1 : _GEN_291; // @[operandCollector.scala 351:74 352:34]
  wire [31:0] _GEN_452 = CUId_2 == 2'h3 & io_validArbiter_2 & regOrder_2 == 2'h1 ? io_dataIn_rs_2_2 : _GEN_292; // @[operandCollector.scala 351:74 352:34]
  wire [31:0] _GEN_453 = CUId_2 == 2'h3 & io_validArbiter_2 & regOrder_2 == 2'h1 ? io_dataIn_rs_2_3 : _GEN_293; // @[operandCollector.scala 351:74 352:34]
  wire [31:0] _GEN_454 = CUId_2 == 2'h3 & io_validArbiter_2 & regOrder_2 == 2'h1 ? io_dataIn_v0_2_0 : _GEN_294; // @[operandCollector.scala 351:74 353:32]
  wire [1:0] _GEN_459 = CUId_2 == 2'h3 & io_validArbiter_2 & regOrder_2 == 2'h1 ? regOrder_2 : _GEN_299; // @[operandCollector.scala 351:74 355:38]
  wire [31:0] _GEN_460 = CUId_2 == 2'h3 & io_validArbiter_2 & regOrder_2 == 2'h2 ? io_dataIn_rs_2_0 : _GEN_300; // @[operandCollector.scala 351:74 352:34]
  wire [31:0] _GEN_461 = CUId_2 == 2'h3 & io_validArbiter_2 & regOrder_2 == 2'h2 ? io_dataIn_rs_2_1 : _GEN_301; // @[operandCollector.scala 351:74 352:34]
  wire [31:0] _GEN_462 = CUId_2 == 2'h3 & io_validArbiter_2 & regOrder_2 == 2'h2 ? io_dataIn_rs_2_2 : _GEN_302; // @[operandCollector.scala 351:74 352:34]
  wire [31:0] _GEN_463 = CUId_2 == 2'h3 & io_validArbiter_2 & regOrder_2 == 2'h2 ? io_dataIn_rs_2_3 : _GEN_303; // @[operandCollector.scala 351:74 352:34]
  wire [31:0] _GEN_464 = CUId_2 == 2'h3 & io_validArbiter_2 & regOrder_2 == 2'h2 ? io_dataIn_v0_2_0 : _GEN_304; // @[operandCollector.scala 351:74 353:32]
  wire [1:0] _GEN_469 = CUId_2 == 2'h3 & io_validArbiter_2 & regOrder_2 == 2'h2 ? regOrder_2 : _GEN_309; // @[operandCollector.scala 351:74 355:38]
  wire [31:0] _GEN_470 = CUId_2 == 2'h3 & io_validArbiter_2 & regOrder_2 == 2'h3 ? io_dataIn_rs_2_0 : _GEN_310; // @[operandCollector.scala 351:74 352:34]
  wire [31:0] _GEN_471 = CUId_2 == 2'h3 & io_validArbiter_2 & regOrder_2 == 2'h3 ? io_dataIn_rs_2_1 : _GEN_311; // @[operandCollector.scala 351:74 352:34]
  wire [31:0] _GEN_472 = CUId_2 == 2'h3 & io_validArbiter_2 & regOrder_2 == 2'h3 ? io_dataIn_rs_2_2 : _GEN_312; // @[operandCollector.scala 351:74 352:34]
  wire [31:0] _GEN_473 = CUId_2 == 2'h3 & io_validArbiter_2 & regOrder_2 == 2'h3 ? io_dataIn_rs_2_3 : _GEN_313; // @[operandCollector.scala 351:74 352:34]
  wire [31:0] _GEN_474 = CUId_2 == 2'h3 & io_validArbiter_2 & regOrder_2 == 2'h3 ? io_dataIn_v0_2_0 : _GEN_314; // @[operandCollector.scala 351:74 353:32]
  wire [1:0] _GEN_479 = CUId_2 == 2'h3 & io_validArbiter_2 & regOrder_2 == 2'h3 ? regOrder_2 : _GEN_319; // @[operandCollector.scala 351:74 355:38]
  wire [1:0] CUId_3 = _CUId_3_T[1:0]; // @[operandCollector.scala 328:18 333:13]
  wire [1:0] regOrder_3 = _GEN_8[1:0]; // @[operandCollector.scala 329:22 334:17]
  assign io_out_0_0_valid = CUId_3 == 2'h0 & io_validArbiter_3 & regOrder_3 == 2'h0 | (CUId_2 == 2'h0 &
    io_validArbiter_2 & regOrder_2 == 2'h0 | (CUId_1 == 2'h0 & io_validArbiter_1 & regOrder_1 == 2'h0 | CUId_0 == 2'h0
     & io_validArbiter_0 & regOrder_0 == 2'h0)); // @[operandCollector.scala 351:74 354:30]
  assign io_out_0_0_bits_regOrder = CUId_3 == 2'h0 & io_validArbiter_3 & regOrder_3 == 2'h0 ? regOrder_3 : _GEN_329; // @[operandCollector.scala 351:74 355:38]
  assign io_out_0_0_bits_data_0 = CUId_3 == 2'h0 & io_validArbiter_3 & regOrder_3 == 2'h0 ? io_dataIn_rs_3_0 : _GEN_320; // @[operandCollector.scala 351:74 352:34]
  assign io_out_0_0_bits_data_1 = CUId_3 == 2'h0 & io_validArbiter_3 & regOrder_3 == 2'h0 ? io_dataIn_rs_3_1 : _GEN_321; // @[operandCollector.scala 351:74 352:34]
  assign io_out_0_0_bits_data_2 = CUId_3 == 2'h0 & io_validArbiter_3 & regOrder_3 == 2'h0 ? io_dataIn_rs_3_2 : _GEN_322; // @[operandCollector.scala 351:74 352:34]
  assign io_out_0_0_bits_data_3 = CUId_3 == 2'h0 & io_validArbiter_3 & regOrder_3 == 2'h0 ? io_dataIn_rs_3_3 : _GEN_323; // @[operandCollector.scala 351:74 352:34]
  assign io_out_0_0_bits_v0_0 = CUId_3 == 2'h0 & io_validArbiter_3 & regOrder_3 == 2'h0 ? io_dataIn_v0_3_0 : _GEN_324; // @[operandCollector.scala 351:74 353:32]
  assign io_out_0_1_valid = CUId_3 == 2'h0 & io_validArbiter_3 & regOrder_3 == 2'h1 | (CUId_2 == 2'h0 &
    io_validArbiter_2 & regOrder_2 == 2'h1 | (CUId_1 == 2'h0 & io_validArbiter_1 & regOrder_1 == 2'h1 | CUId_0 == 2'h0
     & io_validArbiter_0 & regOrder_0 == 2'h1)); // @[operandCollector.scala 351:74 354:30]
  assign io_out_0_1_bits_regOrder = CUId_3 == 2'h0 & io_validArbiter_3 & regOrder_3 == 2'h1 ? regOrder_3 : _GEN_339; // @[operandCollector.scala 351:74 355:38]
  assign io_out_0_1_bits_data_0 = CUId_3 == 2'h0 & io_validArbiter_3 & regOrder_3 == 2'h1 ? io_dataIn_rs_3_0 : _GEN_330; // @[operandCollector.scala 351:74 352:34]
  assign io_out_0_1_bits_data_1 = CUId_3 == 2'h0 & io_validArbiter_3 & regOrder_3 == 2'h1 ? io_dataIn_rs_3_1 : _GEN_331; // @[operandCollector.scala 351:74 352:34]
  assign io_out_0_1_bits_data_2 = CUId_3 == 2'h0 & io_validArbiter_3 & regOrder_3 == 2'h1 ? io_dataIn_rs_3_2 : _GEN_332; // @[operandCollector.scala 351:74 352:34]
  assign io_out_0_1_bits_data_3 = CUId_3 == 2'h0 & io_validArbiter_3 & regOrder_3 == 2'h1 ? io_dataIn_rs_3_3 : _GEN_333; // @[operandCollector.scala 351:74 352:34]
  assign io_out_0_1_bits_v0_0 = CUId_3 == 2'h0 & io_validArbiter_3 & regOrder_3 == 2'h1 ? io_dataIn_v0_3_0 : _GEN_334; // @[operandCollector.scala 351:74 353:32]
  assign io_out_0_2_valid = CUId_3 == 2'h0 & io_validArbiter_3 & regOrder_3 == 2'h2 | (CUId_2 == 2'h0 &
    io_validArbiter_2 & regOrder_2 == 2'h2 | (CUId_1 == 2'h0 & io_validArbiter_1 & regOrder_1 == 2'h2 | CUId_0 == 2'h0
     & io_validArbiter_0 & regOrder_0 == 2'h2)); // @[operandCollector.scala 351:74 354:30]
  assign io_out_0_2_bits_regOrder = CUId_3 == 2'h0 & io_validArbiter_3 & regOrder_3 == 2'h2 ? regOrder_3 : _GEN_349; // @[operandCollector.scala 351:74 355:38]
  assign io_out_0_2_bits_data_0 = CUId_3 == 2'h0 & io_validArbiter_3 & regOrder_3 == 2'h2 ? io_dataIn_rs_3_0 : _GEN_340; // @[operandCollector.scala 351:74 352:34]
  assign io_out_0_2_bits_data_1 = CUId_3 == 2'h0 & io_validArbiter_3 & regOrder_3 == 2'h2 ? io_dataIn_rs_3_1 : _GEN_341; // @[operandCollector.scala 351:74 352:34]
  assign io_out_0_2_bits_data_2 = CUId_3 == 2'h0 & io_validArbiter_3 & regOrder_3 == 2'h2 ? io_dataIn_rs_3_2 : _GEN_342; // @[operandCollector.scala 351:74 352:34]
  assign io_out_0_2_bits_data_3 = CUId_3 == 2'h0 & io_validArbiter_3 & regOrder_3 == 2'h2 ? io_dataIn_rs_3_3 : _GEN_343; // @[operandCollector.scala 351:74 352:34]
  assign io_out_0_2_bits_v0_0 = CUId_3 == 2'h0 & io_validArbiter_3 & regOrder_3 == 2'h2 ? io_dataIn_v0_3_0 : _GEN_344; // @[operandCollector.scala 351:74 353:32]
  assign io_out_0_3_valid = CUId_3 == 2'h0 & io_validArbiter_3 & regOrder_3 == 2'h3 | (CUId_2 == 2'h0 &
    io_validArbiter_2 & regOrder_2 == 2'h3 | (CUId_1 == 2'h0 & io_validArbiter_1 & regOrder_1 == 2'h3 | CUId_0 == 2'h0
     & io_validArbiter_0 & regOrder_0 == 2'h3)); // @[operandCollector.scala 351:74 354:30]
  assign io_out_0_3_bits_regOrder = CUId_3 == 2'h0 & io_validArbiter_3 & regOrder_3 == 2'h3 ? regOrder_3 : _GEN_359; // @[operandCollector.scala 351:74 355:38]
  assign io_out_0_3_bits_data_0 = CUId_3 == 2'h0 & io_validArbiter_3 & regOrder_3 == 2'h3 ? io_dataIn_rs_3_0 : _GEN_350; // @[operandCollector.scala 351:74 352:34]
  assign io_out_0_3_bits_data_1 = CUId_3 == 2'h0 & io_validArbiter_3 & regOrder_3 == 2'h3 ? io_dataIn_rs_3_1 : _GEN_351; // @[operandCollector.scala 351:74 352:34]
  assign io_out_0_3_bits_data_2 = CUId_3 == 2'h0 & io_validArbiter_3 & regOrder_3 == 2'h3 ? io_dataIn_rs_3_2 : _GEN_352; // @[operandCollector.scala 351:74 352:34]
  assign io_out_0_3_bits_data_3 = CUId_3 == 2'h0 & io_validArbiter_3 & regOrder_3 == 2'h3 ? io_dataIn_rs_3_3 : _GEN_353; // @[operandCollector.scala 351:74 352:34]
  assign io_out_0_3_bits_v0_0 = CUId_3 == 2'h0 & io_validArbiter_3 & regOrder_3 == 2'h3 ? io_dataIn_v0_3_0 : _GEN_354; // @[operandCollector.scala 351:74 353:32]
  assign io_out_1_0_valid = CUId_3 == 2'h1 & io_validArbiter_3 & regOrder_3 == 2'h0 | (CUId_2 == 2'h1 &
    io_validArbiter_2 & regOrder_2 == 2'h0 | (CUId_1 == 2'h1 & io_validArbiter_1 & regOrder_1 == 2'h0 | CUId_0 == 2'h1
     & io_validArbiter_0 & regOrder_0 == 2'h0)); // @[operandCollector.scala 351:74 354:30]
  assign io_out_1_0_bits_regOrder = CUId_3 == 2'h1 & io_validArbiter_3 & regOrder_3 == 2'h0 ? regOrder_3 : _GEN_369; // @[operandCollector.scala 351:74 355:38]
  assign io_out_1_0_bits_data_0 = CUId_3 == 2'h1 & io_validArbiter_3 & regOrder_3 == 2'h0 ? io_dataIn_rs_3_0 : _GEN_360; // @[operandCollector.scala 351:74 352:34]
  assign io_out_1_0_bits_data_1 = CUId_3 == 2'h1 & io_validArbiter_3 & regOrder_3 == 2'h0 ? io_dataIn_rs_3_1 : _GEN_361; // @[operandCollector.scala 351:74 352:34]
  assign io_out_1_0_bits_data_2 = CUId_3 == 2'h1 & io_validArbiter_3 & regOrder_3 == 2'h0 ? io_dataIn_rs_3_2 : _GEN_362; // @[operandCollector.scala 351:74 352:34]
  assign io_out_1_0_bits_data_3 = CUId_3 == 2'h1 & io_validArbiter_3 & regOrder_3 == 2'h0 ? io_dataIn_rs_3_3 : _GEN_363; // @[operandCollector.scala 351:74 352:34]
  assign io_out_1_0_bits_v0_0 = CUId_3 == 2'h1 & io_validArbiter_3 & regOrder_3 == 2'h0 ? io_dataIn_v0_3_0 : _GEN_364; // @[operandCollector.scala 351:74 353:32]
  assign io_out_1_1_valid = CUId_3 == 2'h1 & io_validArbiter_3 & regOrder_3 == 2'h1 | (CUId_2 == 2'h1 &
    io_validArbiter_2 & regOrder_2 == 2'h1 | (CUId_1 == 2'h1 & io_validArbiter_1 & regOrder_1 == 2'h1 | CUId_0 == 2'h1
     & io_validArbiter_0 & regOrder_0 == 2'h1)); // @[operandCollector.scala 351:74 354:30]
  assign io_out_1_1_bits_regOrder = CUId_3 == 2'h1 & io_validArbiter_3 & regOrder_3 == 2'h1 ? regOrder_3 : _GEN_379; // @[operandCollector.scala 351:74 355:38]
  assign io_out_1_1_bits_data_0 = CUId_3 == 2'h1 & io_validArbiter_3 & regOrder_3 == 2'h1 ? io_dataIn_rs_3_0 : _GEN_370; // @[operandCollector.scala 351:74 352:34]
  assign io_out_1_1_bits_data_1 = CUId_3 == 2'h1 & io_validArbiter_3 & regOrder_3 == 2'h1 ? io_dataIn_rs_3_1 : _GEN_371; // @[operandCollector.scala 351:74 352:34]
  assign io_out_1_1_bits_data_2 = CUId_3 == 2'h1 & io_validArbiter_3 & regOrder_3 == 2'h1 ? io_dataIn_rs_3_2 : _GEN_372; // @[operandCollector.scala 351:74 352:34]
  assign io_out_1_1_bits_data_3 = CUId_3 == 2'h1 & io_validArbiter_3 & regOrder_3 == 2'h1 ? io_dataIn_rs_3_3 : _GEN_373; // @[operandCollector.scala 351:74 352:34]
  assign io_out_1_1_bits_v0_0 = CUId_3 == 2'h1 & io_validArbiter_3 & regOrder_3 == 2'h1 ? io_dataIn_v0_3_0 : _GEN_374; // @[operandCollector.scala 351:74 353:32]
  assign io_out_1_2_valid = CUId_3 == 2'h1 & io_validArbiter_3 & regOrder_3 == 2'h2 | (CUId_2 == 2'h1 &
    io_validArbiter_2 & regOrder_2 == 2'h2 | (CUId_1 == 2'h1 & io_validArbiter_1 & regOrder_1 == 2'h2 | CUId_0 == 2'h1
     & io_validArbiter_0 & regOrder_0 == 2'h2)); // @[operandCollector.scala 351:74 354:30]
  assign io_out_1_2_bits_regOrder = CUId_3 == 2'h1 & io_validArbiter_3 & regOrder_3 == 2'h2 ? regOrder_3 : _GEN_389; // @[operandCollector.scala 351:74 355:38]
  assign io_out_1_2_bits_data_0 = CUId_3 == 2'h1 & io_validArbiter_3 & regOrder_3 == 2'h2 ? io_dataIn_rs_3_0 : _GEN_380; // @[operandCollector.scala 351:74 352:34]
  assign io_out_1_2_bits_data_1 = CUId_3 == 2'h1 & io_validArbiter_3 & regOrder_3 == 2'h2 ? io_dataIn_rs_3_1 : _GEN_381; // @[operandCollector.scala 351:74 352:34]
  assign io_out_1_2_bits_data_2 = CUId_3 == 2'h1 & io_validArbiter_3 & regOrder_3 == 2'h2 ? io_dataIn_rs_3_2 : _GEN_382; // @[operandCollector.scala 351:74 352:34]
  assign io_out_1_2_bits_data_3 = CUId_3 == 2'h1 & io_validArbiter_3 & regOrder_3 == 2'h2 ? io_dataIn_rs_3_3 : _GEN_383; // @[operandCollector.scala 351:74 352:34]
  assign io_out_1_2_bits_v0_0 = CUId_3 == 2'h1 & io_validArbiter_3 & regOrder_3 == 2'h2 ? io_dataIn_v0_3_0 : _GEN_384; // @[operandCollector.scala 351:74 353:32]
  assign io_out_1_3_valid = CUId_3 == 2'h1 & io_validArbiter_3 & regOrder_3 == 2'h3 | (CUId_2 == 2'h1 &
    io_validArbiter_2 & regOrder_2 == 2'h3 | (CUId_1 == 2'h1 & io_validArbiter_1 & regOrder_1 == 2'h3 | CUId_0 == 2'h1
     & io_validArbiter_0 & regOrder_0 == 2'h3)); // @[operandCollector.scala 351:74 354:30]
  assign io_out_1_3_bits_regOrder = CUId_3 == 2'h1 & io_validArbiter_3 & regOrder_3 == 2'h3 ? regOrder_3 : _GEN_399; // @[operandCollector.scala 351:74 355:38]
  assign io_out_1_3_bits_data_0 = CUId_3 == 2'h1 & io_validArbiter_3 & regOrder_3 == 2'h3 ? io_dataIn_rs_3_0 : _GEN_390; // @[operandCollector.scala 351:74 352:34]
  assign io_out_1_3_bits_data_1 = CUId_3 == 2'h1 & io_validArbiter_3 & regOrder_3 == 2'h3 ? io_dataIn_rs_3_1 : _GEN_391; // @[operandCollector.scala 351:74 352:34]
  assign io_out_1_3_bits_data_2 = CUId_3 == 2'h1 & io_validArbiter_3 & regOrder_3 == 2'h3 ? io_dataIn_rs_3_2 : _GEN_392; // @[operandCollector.scala 351:74 352:34]
  assign io_out_1_3_bits_data_3 = CUId_3 == 2'h1 & io_validArbiter_3 & regOrder_3 == 2'h3 ? io_dataIn_rs_3_3 : _GEN_393; // @[operandCollector.scala 351:74 352:34]
  assign io_out_1_3_bits_v0_0 = CUId_3 == 2'h1 & io_validArbiter_3 & regOrder_3 == 2'h3 ? io_dataIn_v0_3_0 : _GEN_394; // @[operandCollector.scala 351:74 353:32]
  assign io_out_2_0_valid = CUId_3 == 2'h2 & io_validArbiter_3 & regOrder_3 == 2'h0 | (CUId_2 == 2'h2 &
    io_validArbiter_2 & regOrder_2 == 2'h0 | (CUId_1 == 2'h2 & io_validArbiter_1 & regOrder_1 == 2'h0 | CUId_0 == 2'h2
     & io_validArbiter_0 & regOrder_0 == 2'h0)); // @[operandCollector.scala 351:74 354:30]
  assign io_out_2_0_bits_regOrder = CUId_3 == 2'h2 & io_validArbiter_3 & regOrder_3 == 2'h0 ? regOrder_3 : _GEN_409; // @[operandCollector.scala 351:74 355:38]
  assign io_out_2_0_bits_data_0 = CUId_3 == 2'h2 & io_validArbiter_3 & regOrder_3 == 2'h0 ? io_dataIn_rs_3_0 : _GEN_400; // @[operandCollector.scala 351:74 352:34]
  assign io_out_2_0_bits_data_1 = CUId_3 == 2'h2 & io_validArbiter_3 & regOrder_3 == 2'h0 ? io_dataIn_rs_3_1 : _GEN_401; // @[operandCollector.scala 351:74 352:34]
  assign io_out_2_0_bits_data_2 = CUId_3 == 2'h2 & io_validArbiter_3 & regOrder_3 == 2'h0 ? io_dataIn_rs_3_2 : _GEN_402; // @[operandCollector.scala 351:74 352:34]
  assign io_out_2_0_bits_data_3 = CUId_3 == 2'h2 & io_validArbiter_3 & regOrder_3 == 2'h0 ? io_dataIn_rs_3_3 : _GEN_403; // @[operandCollector.scala 351:74 352:34]
  assign io_out_2_0_bits_v0_0 = CUId_3 == 2'h2 & io_validArbiter_3 & regOrder_3 == 2'h0 ? io_dataIn_v0_3_0 : _GEN_404; // @[operandCollector.scala 351:74 353:32]
  assign io_out_2_1_valid = CUId_3 == 2'h2 & io_validArbiter_3 & regOrder_3 == 2'h1 | (CUId_2 == 2'h2 &
    io_validArbiter_2 & regOrder_2 == 2'h1 | (CUId_1 == 2'h2 & io_validArbiter_1 & regOrder_1 == 2'h1 | CUId_0 == 2'h2
     & io_validArbiter_0 & regOrder_0 == 2'h1)); // @[operandCollector.scala 351:74 354:30]
  assign io_out_2_1_bits_regOrder = CUId_3 == 2'h2 & io_validArbiter_3 & regOrder_3 == 2'h1 ? regOrder_3 : _GEN_419; // @[operandCollector.scala 351:74 355:38]
  assign io_out_2_1_bits_data_0 = CUId_3 == 2'h2 & io_validArbiter_3 & regOrder_3 == 2'h1 ? io_dataIn_rs_3_0 : _GEN_410; // @[operandCollector.scala 351:74 352:34]
  assign io_out_2_1_bits_data_1 = CUId_3 == 2'h2 & io_validArbiter_3 & regOrder_3 == 2'h1 ? io_dataIn_rs_3_1 : _GEN_411; // @[operandCollector.scala 351:74 352:34]
  assign io_out_2_1_bits_data_2 = CUId_3 == 2'h2 & io_validArbiter_3 & regOrder_3 == 2'h1 ? io_dataIn_rs_3_2 : _GEN_412; // @[operandCollector.scala 351:74 352:34]
  assign io_out_2_1_bits_data_3 = CUId_3 == 2'h2 & io_validArbiter_3 & regOrder_3 == 2'h1 ? io_dataIn_rs_3_3 : _GEN_413; // @[operandCollector.scala 351:74 352:34]
  assign io_out_2_1_bits_v0_0 = CUId_3 == 2'h2 & io_validArbiter_3 & regOrder_3 == 2'h1 ? io_dataIn_v0_3_0 : _GEN_414; // @[operandCollector.scala 351:74 353:32]
  assign io_out_2_2_valid = CUId_3 == 2'h2 & io_validArbiter_3 & regOrder_3 == 2'h2 | (CUId_2 == 2'h2 &
    io_validArbiter_2 & regOrder_2 == 2'h2 | (CUId_1 == 2'h2 & io_validArbiter_1 & regOrder_1 == 2'h2 | CUId_0 == 2'h2
     & io_validArbiter_0 & regOrder_0 == 2'h2)); // @[operandCollector.scala 351:74 354:30]
  assign io_out_2_2_bits_regOrder = CUId_3 == 2'h2 & io_validArbiter_3 & regOrder_3 == 2'h2 ? regOrder_3 : _GEN_429; // @[operandCollector.scala 351:74 355:38]
  assign io_out_2_2_bits_data_0 = CUId_3 == 2'h2 & io_validArbiter_3 & regOrder_3 == 2'h2 ? io_dataIn_rs_3_0 : _GEN_420; // @[operandCollector.scala 351:74 352:34]
  assign io_out_2_2_bits_data_1 = CUId_3 == 2'h2 & io_validArbiter_3 & regOrder_3 == 2'h2 ? io_dataIn_rs_3_1 : _GEN_421; // @[operandCollector.scala 351:74 352:34]
  assign io_out_2_2_bits_data_2 = CUId_3 == 2'h2 & io_validArbiter_3 & regOrder_3 == 2'h2 ? io_dataIn_rs_3_2 : _GEN_422; // @[operandCollector.scala 351:74 352:34]
  assign io_out_2_2_bits_data_3 = CUId_3 == 2'h2 & io_validArbiter_3 & regOrder_3 == 2'h2 ? io_dataIn_rs_3_3 : _GEN_423; // @[operandCollector.scala 351:74 352:34]
  assign io_out_2_2_bits_v0_0 = CUId_3 == 2'h2 & io_validArbiter_3 & regOrder_3 == 2'h2 ? io_dataIn_v0_3_0 : _GEN_424; // @[operandCollector.scala 351:74 353:32]
  assign io_out_2_3_valid = CUId_3 == 2'h2 & io_validArbiter_3 & regOrder_3 == 2'h3 | (CUId_2 == 2'h2 &
    io_validArbiter_2 & regOrder_2 == 2'h3 | (CUId_1 == 2'h2 & io_validArbiter_1 & regOrder_1 == 2'h3 | CUId_0 == 2'h2
     & io_validArbiter_0 & regOrder_0 == 2'h3)); // @[operandCollector.scala 351:74 354:30]
  assign io_out_2_3_bits_regOrder = CUId_3 == 2'h2 & io_validArbiter_3 & regOrder_3 == 2'h3 ? regOrder_3 : _GEN_439; // @[operandCollector.scala 351:74 355:38]
  assign io_out_2_3_bits_data_0 = CUId_3 == 2'h2 & io_validArbiter_3 & regOrder_3 == 2'h3 ? io_dataIn_rs_3_0 : _GEN_430; // @[operandCollector.scala 351:74 352:34]
  assign io_out_2_3_bits_data_1 = CUId_3 == 2'h2 & io_validArbiter_3 & regOrder_3 == 2'h3 ? io_dataIn_rs_3_1 : _GEN_431; // @[operandCollector.scala 351:74 352:34]
  assign io_out_2_3_bits_data_2 = CUId_3 == 2'h2 & io_validArbiter_3 & regOrder_3 == 2'h3 ? io_dataIn_rs_3_2 : _GEN_432; // @[operandCollector.scala 351:74 352:34]
  assign io_out_2_3_bits_data_3 = CUId_3 == 2'h2 & io_validArbiter_3 & regOrder_3 == 2'h3 ? io_dataIn_rs_3_3 : _GEN_433; // @[operandCollector.scala 351:74 352:34]
  assign io_out_2_3_bits_v0_0 = CUId_3 == 2'h2 & io_validArbiter_3 & regOrder_3 == 2'h3 ? io_dataIn_v0_3_0 : _GEN_434; // @[operandCollector.scala 351:74 353:32]
  assign io_out_3_0_valid = CUId_3 == 2'h3 & io_validArbiter_3 & regOrder_3 == 2'h0 | (CUId_2 == 2'h3 &
    io_validArbiter_2 & regOrder_2 == 2'h0 | (CUId_1 == 2'h3 & io_validArbiter_1 & regOrder_1 == 2'h0 | CUId_0 == 2'h3
     & io_validArbiter_0 & regOrder_0 == 2'h0)); // @[operandCollector.scala 351:74 354:30]
  assign io_out_3_0_bits_regOrder = CUId_3 == 2'h3 & io_validArbiter_3 & regOrder_3 == 2'h0 ? regOrder_3 : _GEN_449; // @[operandCollector.scala 351:74 355:38]
  assign io_out_3_0_bits_data_0 = CUId_3 == 2'h3 & io_validArbiter_3 & regOrder_3 == 2'h0 ? io_dataIn_rs_3_0 : _GEN_440; // @[operandCollector.scala 351:74 352:34]
  assign io_out_3_0_bits_data_1 = CUId_3 == 2'h3 & io_validArbiter_3 & regOrder_3 == 2'h0 ? io_dataIn_rs_3_1 : _GEN_441; // @[operandCollector.scala 351:74 352:34]
  assign io_out_3_0_bits_data_2 = CUId_3 == 2'h3 & io_validArbiter_3 & regOrder_3 == 2'h0 ? io_dataIn_rs_3_2 : _GEN_442; // @[operandCollector.scala 351:74 352:34]
  assign io_out_3_0_bits_data_3 = CUId_3 == 2'h3 & io_validArbiter_3 & regOrder_3 == 2'h0 ? io_dataIn_rs_3_3 : _GEN_443; // @[operandCollector.scala 351:74 352:34]
  assign io_out_3_0_bits_v0_0 = CUId_3 == 2'h3 & io_validArbiter_3 & regOrder_3 == 2'h0 ? io_dataIn_v0_3_0 : _GEN_444; // @[operandCollector.scala 351:74 353:32]
  assign io_out_3_1_valid = CUId_3 == 2'h3 & io_validArbiter_3 & regOrder_3 == 2'h1 | (CUId_2 == 2'h3 &
    io_validArbiter_2 & regOrder_2 == 2'h1 | (CUId_1 == 2'h3 & io_validArbiter_1 & regOrder_1 == 2'h1 | CUId_0 == 2'h3
     & io_validArbiter_0 & regOrder_0 == 2'h1)); // @[operandCollector.scala 351:74 354:30]
  assign io_out_3_1_bits_regOrder = CUId_3 == 2'h3 & io_validArbiter_3 & regOrder_3 == 2'h1 ? regOrder_3 : _GEN_459; // @[operandCollector.scala 351:74 355:38]
  assign io_out_3_1_bits_data_0 = CUId_3 == 2'h3 & io_validArbiter_3 & regOrder_3 == 2'h1 ? io_dataIn_rs_3_0 : _GEN_450; // @[operandCollector.scala 351:74 352:34]
  assign io_out_3_1_bits_data_1 = CUId_3 == 2'h3 & io_validArbiter_3 & regOrder_3 == 2'h1 ? io_dataIn_rs_3_1 : _GEN_451; // @[operandCollector.scala 351:74 352:34]
  assign io_out_3_1_bits_data_2 = CUId_3 == 2'h3 & io_validArbiter_3 & regOrder_3 == 2'h1 ? io_dataIn_rs_3_2 : _GEN_452; // @[operandCollector.scala 351:74 352:34]
  assign io_out_3_1_bits_data_3 = CUId_3 == 2'h3 & io_validArbiter_3 & regOrder_3 == 2'h1 ? io_dataIn_rs_3_3 : _GEN_453; // @[operandCollector.scala 351:74 352:34]
  assign io_out_3_1_bits_v0_0 = CUId_3 == 2'h3 & io_validArbiter_3 & regOrder_3 == 2'h1 ? io_dataIn_v0_3_0 : _GEN_454; // @[operandCollector.scala 351:74 353:32]
  assign io_out_3_2_valid = CUId_3 == 2'h3 & io_validArbiter_3 & regOrder_3 == 2'h2 | (CUId_2 == 2'h3 &
    io_validArbiter_2 & regOrder_2 == 2'h2 | (CUId_1 == 2'h3 & io_validArbiter_1 & regOrder_1 == 2'h2 | CUId_0 == 2'h3
     & io_validArbiter_0 & regOrder_0 == 2'h2)); // @[operandCollector.scala 351:74 354:30]
  assign io_out_3_2_bits_regOrder = CUId_3 == 2'h3 & io_validArbiter_3 & regOrder_3 == 2'h2 ? regOrder_3 : _GEN_469; // @[operandCollector.scala 351:74 355:38]
  assign io_out_3_2_bits_data_0 = CUId_3 == 2'h3 & io_validArbiter_3 & regOrder_3 == 2'h2 ? io_dataIn_rs_3_0 : _GEN_460; // @[operandCollector.scala 351:74 352:34]
  assign io_out_3_2_bits_data_1 = CUId_3 == 2'h3 & io_validArbiter_3 & regOrder_3 == 2'h2 ? io_dataIn_rs_3_1 : _GEN_461; // @[operandCollector.scala 351:74 352:34]
  assign io_out_3_2_bits_data_2 = CUId_3 == 2'h3 & io_validArbiter_3 & regOrder_3 == 2'h2 ? io_dataIn_rs_3_2 : _GEN_462; // @[operandCollector.scala 351:74 352:34]
  assign io_out_3_2_bits_data_3 = CUId_3 == 2'h3 & io_validArbiter_3 & regOrder_3 == 2'h2 ? io_dataIn_rs_3_3 : _GEN_463; // @[operandCollector.scala 351:74 352:34]
  assign io_out_3_2_bits_v0_0 = CUId_3 == 2'h3 & io_validArbiter_3 & regOrder_3 == 2'h2 ? io_dataIn_v0_3_0 : _GEN_464; // @[operandCollector.scala 351:74 353:32]
  assign io_out_3_3_valid = CUId_3 == 2'h3 & io_validArbiter_3 & regOrder_3 == 2'h3 | (CUId_2 == 2'h3 &
    io_validArbiter_2 & regOrder_2 == 2'h3 | (CUId_1 == 2'h3 & io_validArbiter_1 & regOrder_1 == 2'h3 | CUId_0 == 2'h3
     & io_validArbiter_0 & regOrder_0 == 2'h3)); // @[operandCollector.scala 351:74 354:30]
  assign io_out_3_3_bits_regOrder = CUId_3 == 2'h3 & io_validArbiter_3 & regOrder_3 == 2'h3 ? regOrder_3 : _GEN_479; // @[operandCollector.scala 351:74 355:38]
  assign io_out_3_3_bits_data_0 = CUId_3 == 2'h3 & io_validArbiter_3 & regOrder_3 == 2'h3 ? io_dataIn_rs_3_0 : _GEN_470; // @[operandCollector.scala 351:74 352:34]
  assign io_out_3_3_bits_data_1 = CUId_3 == 2'h3 & io_validArbiter_3 & regOrder_3 == 2'h3 ? io_dataIn_rs_3_1 : _GEN_471; // @[operandCollector.scala 351:74 352:34]
  assign io_out_3_3_bits_data_2 = CUId_3 == 2'h3 & io_validArbiter_3 & regOrder_3 == 2'h3 ? io_dataIn_rs_3_2 : _GEN_472; // @[operandCollector.scala 351:74 352:34]
  assign io_out_3_3_bits_data_3 = CUId_3 == 2'h3 & io_validArbiter_3 & regOrder_3 == 2'h3 ? io_dataIn_rs_3_3 : _GEN_473; // @[operandCollector.scala 351:74 352:34]
  assign io_out_3_3_bits_v0_0 = CUId_3 == 2'h3 & io_validArbiter_3 & regOrder_3 == 2'h3 ? io_dataIn_v0_3_0 : _GEN_474; // @[operandCollector.scala 351:74 353:32]
endmodule
module instDemux(
  output        io_in_ready,
  input         io_in_valid,
  input  [31:0] io_in_bits_inst,
  input  [1:0]  io_in_bits_wid,
  input         io_in_bits_fp,
  input  [1:0]  io_in_bits_branch,
  input         io_in_bits_simt_stack,
  input         io_in_bits_simt_stack_op,
  input         io_in_bits_barrier,
  input  [1:0]  io_in_bits_csr,
  input         io_in_bits_reverse,
  input  [1:0]  io_in_bits_sel_alu2,
  input  [1:0]  io_in_bits_sel_alu1,
  input         io_in_bits_isvec,
  input  [1:0]  io_in_bits_sel_alu3,
  input         io_in_bits_mask,
  input  [2:0]  io_in_bits_sel_imm,
  input  [1:0]  io_in_bits_mem_whb,
  input         io_in_bits_mem_unsigned,
  input  [5:0]  io_in_bits_alu_fn,
  input         io_in_bits_mem,
  input         io_in_bits_mul,
  input  [1:0]  io_in_bits_mem_cmd,
  input  [1:0]  io_in_bits_mop,
  input  [4:0]  io_in_bits_reg_idx1,
  input  [4:0]  io_in_bits_reg_idx2,
  input  [4:0]  io_in_bits_reg_idx3,
  input  [4:0]  io_in_bits_reg_idxw,
  input         io_in_bits_wfd,
  input         io_in_bits_fence,
  input         io_in_bits_sfu,
  input         io_in_bits_readmask,
  input         io_in_bits_writemask,
  input         io_in_bits_wxd,
  input  [31:0] io_in_bits_pc,
  input  [31:0] io_in_bits_spike_info_pc,
  input  [31:0] io_in_bits_spike_info_inst,
  input         io_out_0_ready,
  output        io_out_0_valid,
  output [31:0] io_out_0_bits_inst,
  output [1:0]  io_out_0_bits_wid,
  output        io_out_0_bits_fp,
  output [1:0]  io_out_0_bits_branch,
  output        io_out_0_bits_simt_stack,
  output        io_out_0_bits_simt_stack_op,
  output        io_out_0_bits_barrier,
  output [1:0]  io_out_0_bits_csr,
  output        io_out_0_bits_reverse,
  output [1:0]  io_out_0_bits_sel_alu2,
  output [1:0]  io_out_0_bits_sel_alu1,
  output        io_out_0_bits_isvec,
  output [1:0]  io_out_0_bits_sel_alu3,
  output        io_out_0_bits_mask,
  output [2:0]  io_out_0_bits_sel_imm,
  output [1:0]  io_out_0_bits_mem_whb,
  output        io_out_0_bits_mem_unsigned,
  output [5:0]  io_out_0_bits_alu_fn,
  output        io_out_0_bits_mem,
  output        io_out_0_bits_mul,
  output [1:0]  io_out_0_bits_mem_cmd,
  output [1:0]  io_out_0_bits_mop,
  output [4:0]  io_out_0_bits_reg_idx1,
  output [4:0]  io_out_0_bits_reg_idx2,
  output [4:0]  io_out_0_bits_reg_idx3,
  output [4:0]  io_out_0_bits_reg_idxw,
  output        io_out_0_bits_wfd,
  output        io_out_0_bits_fence,
  output        io_out_0_bits_sfu,
  output        io_out_0_bits_readmask,
  output        io_out_0_bits_writemask,
  output        io_out_0_bits_wxd,
  output [31:0] io_out_0_bits_pc,
  output [31:0] io_out_0_bits_spike_info_pc,
  output [31:0] io_out_0_bits_spike_info_inst,
  input         io_out_1_ready,
  output        io_out_1_valid,
  output [31:0] io_out_1_bits_inst,
  output [1:0]  io_out_1_bits_wid,
  output        io_out_1_bits_fp,
  output [1:0]  io_out_1_bits_branch,
  output        io_out_1_bits_simt_stack,
  output        io_out_1_bits_simt_stack_op,
  output        io_out_1_bits_barrier,
  output [1:0]  io_out_1_bits_csr,
  output        io_out_1_bits_reverse,
  output [1:0]  io_out_1_bits_sel_alu2,
  output [1:0]  io_out_1_bits_sel_alu1,
  output        io_out_1_bits_isvec,
  output [1:0]  io_out_1_bits_sel_alu3,
  output        io_out_1_bits_mask,
  output [2:0]  io_out_1_bits_sel_imm,
  output [1:0]  io_out_1_bits_mem_whb,
  output        io_out_1_bits_mem_unsigned,
  output [5:0]  io_out_1_bits_alu_fn,
  output        io_out_1_bits_mem,
  output        io_out_1_bits_mul,
  output [1:0]  io_out_1_bits_mem_cmd,
  output [1:0]  io_out_1_bits_mop,
  output [4:0]  io_out_1_bits_reg_idx1,
  output [4:0]  io_out_1_bits_reg_idx2,
  output [4:0]  io_out_1_bits_reg_idx3,
  output [4:0]  io_out_1_bits_reg_idxw,
  output        io_out_1_bits_wfd,
  output        io_out_1_bits_fence,
  output        io_out_1_bits_sfu,
  output        io_out_1_bits_readmask,
  output        io_out_1_bits_writemask,
  output        io_out_1_bits_wxd,
  output [31:0] io_out_1_bits_pc,
  output [31:0] io_out_1_bits_spike_info_pc,
  output [31:0] io_out_1_bits_spike_info_inst,
  input         io_out_2_ready,
  output        io_out_2_valid,
  output [31:0] io_out_2_bits_inst,
  output [1:0]  io_out_2_bits_wid,
  output        io_out_2_bits_fp,
  output [1:0]  io_out_2_bits_branch,
  output        io_out_2_bits_simt_stack,
  output        io_out_2_bits_simt_stack_op,
  output        io_out_2_bits_barrier,
  output [1:0]  io_out_2_bits_csr,
  output        io_out_2_bits_reverse,
  output [1:0]  io_out_2_bits_sel_alu2,
  output [1:0]  io_out_2_bits_sel_alu1,
  output        io_out_2_bits_isvec,
  output [1:0]  io_out_2_bits_sel_alu3,
  output        io_out_2_bits_mask,
  output [2:0]  io_out_2_bits_sel_imm,
  output [1:0]  io_out_2_bits_mem_whb,
  output        io_out_2_bits_mem_unsigned,
  output [5:0]  io_out_2_bits_alu_fn,
  output        io_out_2_bits_mem,
  output        io_out_2_bits_mul,
  output [1:0]  io_out_2_bits_mem_cmd,
  output [1:0]  io_out_2_bits_mop,
  output [4:0]  io_out_2_bits_reg_idx1,
  output [4:0]  io_out_2_bits_reg_idx2,
  output [4:0]  io_out_2_bits_reg_idx3,
  output [4:0]  io_out_2_bits_reg_idxw,
  output        io_out_2_bits_wfd,
  output        io_out_2_bits_fence,
  output        io_out_2_bits_sfu,
  output        io_out_2_bits_readmask,
  output        io_out_2_bits_writemask,
  output        io_out_2_bits_wxd,
  output [31:0] io_out_2_bits_pc,
  output [31:0] io_out_2_bits_spike_info_pc,
  output [31:0] io_out_2_bits_spike_info_inst,
  input         io_out_3_ready,
  output        io_out_3_valid,
  output [31:0] io_out_3_bits_inst,
  output [1:0]  io_out_3_bits_wid,
  output        io_out_3_bits_fp,
  output [1:0]  io_out_3_bits_branch,
  output        io_out_3_bits_simt_stack,
  output        io_out_3_bits_simt_stack_op,
  output        io_out_3_bits_barrier,
  output [1:0]  io_out_3_bits_csr,
  output        io_out_3_bits_reverse,
  output [1:0]  io_out_3_bits_sel_alu2,
  output [1:0]  io_out_3_bits_sel_alu1,
  output        io_out_3_bits_isvec,
  output [1:0]  io_out_3_bits_sel_alu3,
  output        io_out_3_bits_mask,
  output [2:0]  io_out_3_bits_sel_imm,
  output [1:0]  io_out_3_bits_mem_whb,
  output        io_out_3_bits_mem_unsigned,
  output [5:0]  io_out_3_bits_alu_fn,
  output        io_out_3_bits_mem,
  output        io_out_3_bits_mul,
  output [1:0]  io_out_3_bits_mem_cmd,
  output [1:0]  io_out_3_bits_mop,
  output [4:0]  io_out_3_bits_reg_idx1,
  output [4:0]  io_out_3_bits_reg_idx2,
  output [4:0]  io_out_3_bits_reg_idx3,
  output [4:0]  io_out_3_bits_reg_idxw,
  output        io_out_3_bits_wfd,
  output        io_out_3_bits_fence,
  output        io_out_3_bits_sfu,
  output        io_out_3_bits_readmask,
  output        io_out_3_bits_writemask,
  output        io_out_3_bits_wxd,
  output [31:0] io_out_3_bits_pc,
  output [31:0] io_out_3_bits_spike_info_pc,
  output [31:0] io_out_3_bits_spike_info_inst
);
  wire [1:0] _io_out_0_valid_T = io_out_2_ready ? 2'h2 : 2'h3; // @[Mux.scala 47:70]
  wire [1:0] _io_out_0_valid_T_1 = io_out_1_ready ? 2'h1 : _io_out_0_valid_T; // @[Mux.scala 47:70]
  wire [1:0] _io_out_0_valid_T_2 = io_out_0_ready ? 2'h0 : _io_out_0_valid_T_1; // @[Mux.scala 47:70]
  wire [3:0] _io_in_ready_T_4 = {io_out_3_ready,io_out_2_ready,io_out_1_ready,io_out_0_ready}; // @[operandCollector.scala 379:55]
  assign io_in_ready = |_io_in_ready_T_4; // @[operandCollector.scala 379:62]
  assign io_out_0_valid = _io_out_0_valid_T_2 == 2'h0 & io_in_valid; // @[operandCollector.scala 376:70]
  assign io_out_0_bits_inst = io_in_bits_inst; // @[operandCollector.scala 372:27]
  assign io_out_0_bits_wid = io_in_bits_wid; // @[operandCollector.scala 372:27]
  assign io_out_0_bits_fp = io_in_bits_fp; // @[operandCollector.scala 372:27]
  assign io_out_0_bits_branch = io_in_bits_branch; // @[operandCollector.scala 372:27]
  assign io_out_0_bits_simt_stack = io_in_bits_simt_stack; // @[operandCollector.scala 372:27]
  assign io_out_0_bits_simt_stack_op = io_in_bits_simt_stack_op; // @[operandCollector.scala 372:27]
  assign io_out_0_bits_barrier = io_in_bits_barrier; // @[operandCollector.scala 372:27]
  assign io_out_0_bits_csr = io_in_bits_csr; // @[operandCollector.scala 372:27]
  assign io_out_0_bits_reverse = io_in_bits_reverse; // @[operandCollector.scala 372:27]
  assign io_out_0_bits_sel_alu2 = io_in_bits_sel_alu2; // @[operandCollector.scala 372:27]
  assign io_out_0_bits_sel_alu1 = io_in_bits_sel_alu1; // @[operandCollector.scala 372:27]
  assign io_out_0_bits_isvec = io_in_bits_isvec; // @[operandCollector.scala 372:27]
  assign io_out_0_bits_sel_alu3 = io_in_bits_sel_alu3; // @[operandCollector.scala 372:27]
  assign io_out_0_bits_mask = io_in_bits_mask; // @[operandCollector.scala 372:27]
  assign io_out_0_bits_sel_imm = io_in_bits_sel_imm; // @[operandCollector.scala 372:27]
  assign io_out_0_bits_mem_whb = io_in_bits_mem_whb; // @[operandCollector.scala 372:27]
  assign io_out_0_bits_mem_unsigned = io_in_bits_mem_unsigned; // @[operandCollector.scala 372:27]
  assign io_out_0_bits_alu_fn = io_in_bits_alu_fn; // @[operandCollector.scala 372:27]
  assign io_out_0_bits_mem = io_in_bits_mem; // @[operandCollector.scala 372:27]
  assign io_out_0_bits_mul = io_in_bits_mul; // @[operandCollector.scala 372:27]
  assign io_out_0_bits_mem_cmd = io_in_bits_mem_cmd; // @[operandCollector.scala 372:27]
  assign io_out_0_bits_mop = io_in_bits_mop; // @[operandCollector.scala 372:27]
  assign io_out_0_bits_reg_idx1 = io_in_bits_reg_idx1; // @[operandCollector.scala 372:27]
  assign io_out_0_bits_reg_idx2 = io_in_bits_reg_idx2; // @[operandCollector.scala 372:27]
  assign io_out_0_bits_reg_idx3 = io_in_bits_reg_idx3; // @[operandCollector.scala 372:27]
  assign io_out_0_bits_reg_idxw = io_in_bits_reg_idxw; // @[operandCollector.scala 372:27]
  assign io_out_0_bits_wfd = io_in_bits_wfd; // @[operandCollector.scala 372:27]
  assign io_out_0_bits_fence = io_in_bits_fence; // @[operandCollector.scala 372:27]
  assign io_out_0_bits_sfu = io_in_bits_sfu; // @[operandCollector.scala 372:27]
  assign io_out_0_bits_readmask = io_in_bits_readmask; // @[operandCollector.scala 372:27]
  assign io_out_0_bits_writemask = io_in_bits_writemask; // @[operandCollector.scala 372:27]
  assign io_out_0_bits_wxd = io_in_bits_wxd; // @[operandCollector.scala 372:27]
  assign io_out_0_bits_pc = io_in_bits_pc; // @[operandCollector.scala 372:27]
  assign io_out_0_bits_spike_info_pc = io_in_bits_spike_info_pc; // @[operandCollector.scala 372:27]
  assign io_out_0_bits_spike_info_inst = io_in_bits_spike_info_inst; // @[operandCollector.scala 372:27]
  assign io_out_1_valid = _io_out_0_valid_T_2 == 2'h1 & io_in_valid; // @[operandCollector.scala 376:70]
  assign io_out_1_bits_inst = io_in_bits_inst; // @[operandCollector.scala 372:27]
  assign io_out_1_bits_wid = io_in_bits_wid; // @[operandCollector.scala 372:27]
  assign io_out_1_bits_fp = io_in_bits_fp; // @[operandCollector.scala 372:27]
  assign io_out_1_bits_branch = io_in_bits_branch; // @[operandCollector.scala 372:27]
  assign io_out_1_bits_simt_stack = io_in_bits_simt_stack; // @[operandCollector.scala 372:27]
  assign io_out_1_bits_simt_stack_op = io_in_bits_simt_stack_op; // @[operandCollector.scala 372:27]
  assign io_out_1_bits_barrier = io_in_bits_barrier; // @[operandCollector.scala 372:27]
  assign io_out_1_bits_csr = io_in_bits_csr; // @[operandCollector.scala 372:27]
  assign io_out_1_bits_reverse = io_in_bits_reverse; // @[operandCollector.scala 372:27]
  assign io_out_1_bits_sel_alu2 = io_in_bits_sel_alu2; // @[operandCollector.scala 372:27]
  assign io_out_1_bits_sel_alu1 = io_in_bits_sel_alu1; // @[operandCollector.scala 372:27]
  assign io_out_1_bits_isvec = io_in_bits_isvec; // @[operandCollector.scala 372:27]
  assign io_out_1_bits_sel_alu3 = io_in_bits_sel_alu3; // @[operandCollector.scala 372:27]
  assign io_out_1_bits_mask = io_in_bits_mask; // @[operandCollector.scala 372:27]
  assign io_out_1_bits_sel_imm = io_in_bits_sel_imm; // @[operandCollector.scala 372:27]
  assign io_out_1_bits_mem_whb = io_in_bits_mem_whb; // @[operandCollector.scala 372:27]
  assign io_out_1_bits_mem_unsigned = io_in_bits_mem_unsigned; // @[operandCollector.scala 372:27]
  assign io_out_1_bits_alu_fn = io_in_bits_alu_fn; // @[operandCollector.scala 372:27]
  assign io_out_1_bits_mem = io_in_bits_mem; // @[operandCollector.scala 372:27]
  assign io_out_1_bits_mul = io_in_bits_mul; // @[operandCollector.scala 372:27]
  assign io_out_1_bits_mem_cmd = io_in_bits_mem_cmd; // @[operandCollector.scala 372:27]
  assign io_out_1_bits_mop = io_in_bits_mop; // @[operandCollector.scala 372:27]
  assign io_out_1_bits_reg_idx1 = io_in_bits_reg_idx1; // @[operandCollector.scala 372:27]
  assign io_out_1_bits_reg_idx2 = io_in_bits_reg_idx2; // @[operandCollector.scala 372:27]
  assign io_out_1_bits_reg_idx3 = io_in_bits_reg_idx3; // @[operandCollector.scala 372:27]
  assign io_out_1_bits_reg_idxw = io_in_bits_reg_idxw; // @[operandCollector.scala 372:27]
  assign io_out_1_bits_wfd = io_in_bits_wfd; // @[operandCollector.scala 372:27]
  assign io_out_1_bits_fence = io_in_bits_fence; // @[operandCollector.scala 372:27]
  assign io_out_1_bits_sfu = io_in_bits_sfu; // @[operandCollector.scala 372:27]
  assign io_out_1_bits_readmask = io_in_bits_readmask; // @[operandCollector.scala 372:27]
  assign io_out_1_bits_writemask = io_in_bits_writemask; // @[operandCollector.scala 372:27]
  assign io_out_1_bits_wxd = io_in_bits_wxd; // @[operandCollector.scala 372:27]
  assign io_out_1_bits_pc = io_in_bits_pc; // @[operandCollector.scala 372:27]
  assign io_out_1_bits_spike_info_pc = io_in_bits_spike_info_pc; // @[operandCollector.scala 372:27]
  assign io_out_1_bits_spike_info_inst = io_in_bits_spike_info_inst; // @[operandCollector.scala 372:27]
  assign io_out_2_valid = _io_out_0_valid_T_2 == 2'h2 & io_in_valid; // @[operandCollector.scala 376:70]
  assign io_out_2_bits_inst = io_in_bits_inst; // @[operandCollector.scala 372:27]
  assign io_out_2_bits_wid = io_in_bits_wid; // @[operandCollector.scala 372:27]
  assign io_out_2_bits_fp = io_in_bits_fp; // @[operandCollector.scala 372:27]
  assign io_out_2_bits_branch = io_in_bits_branch; // @[operandCollector.scala 372:27]
  assign io_out_2_bits_simt_stack = io_in_bits_simt_stack; // @[operandCollector.scala 372:27]
  assign io_out_2_bits_simt_stack_op = io_in_bits_simt_stack_op; // @[operandCollector.scala 372:27]
  assign io_out_2_bits_barrier = io_in_bits_barrier; // @[operandCollector.scala 372:27]
  assign io_out_2_bits_csr = io_in_bits_csr; // @[operandCollector.scala 372:27]
  assign io_out_2_bits_reverse = io_in_bits_reverse; // @[operandCollector.scala 372:27]
  assign io_out_2_bits_sel_alu2 = io_in_bits_sel_alu2; // @[operandCollector.scala 372:27]
  assign io_out_2_bits_sel_alu1 = io_in_bits_sel_alu1; // @[operandCollector.scala 372:27]
  assign io_out_2_bits_isvec = io_in_bits_isvec; // @[operandCollector.scala 372:27]
  assign io_out_2_bits_sel_alu3 = io_in_bits_sel_alu3; // @[operandCollector.scala 372:27]
  assign io_out_2_bits_mask = io_in_bits_mask; // @[operandCollector.scala 372:27]
  assign io_out_2_bits_sel_imm = io_in_bits_sel_imm; // @[operandCollector.scala 372:27]
  assign io_out_2_bits_mem_whb = io_in_bits_mem_whb; // @[operandCollector.scala 372:27]
  assign io_out_2_bits_mem_unsigned = io_in_bits_mem_unsigned; // @[operandCollector.scala 372:27]
  assign io_out_2_bits_alu_fn = io_in_bits_alu_fn; // @[operandCollector.scala 372:27]
  assign io_out_2_bits_mem = io_in_bits_mem; // @[operandCollector.scala 372:27]
  assign io_out_2_bits_mul = io_in_bits_mul; // @[operandCollector.scala 372:27]
  assign io_out_2_bits_mem_cmd = io_in_bits_mem_cmd; // @[operandCollector.scala 372:27]
  assign io_out_2_bits_mop = io_in_bits_mop; // @[operandCollector.scala 372:27]
  assign io_out_2_bits_reg_idx1 = io_in_bits_reg_idx1; // @[operandCollector.scala 372:27]
  assign io_out_2_bits_reg_idx2 = io_in_bits_reg_idx2; // @[operandCollector.scala 372:27]
  assign io_out_2_bits_reg_idx3 = io_in_bits_reg_idx3; // @[operandCollector.scala 372:27]
  assign io_out_2_bits_reg_idxw = io_in_bits_reg_idxw; // @[operandCollector.scala 372:27]
  assign io_out_2_bits_wfd = io_in_bits_wfd; // @[operandCollector.scala 372:27]
  assign io_out_2_bits_fence = io_in_bits_fence; // @[operandCollector.scala 372:27]
  assign io_out_2_bits_sfu = io_in_bits_sfu; // @[operandCollector.scala 372:27]
  assign io_out_2_bits_readmask = io_in_bits_readmask; // @[operandCollector.scala 372:27]
  assign io_out_2_bits_writemask = io_in_bits_writemask; // @[operandCollector.scala 372:27]
  assign io_out_2_bits_wxd = io_in_bits_wxd; // @[operandCollector.scala 372:27]
  assign io_out_2_bits_pc = io_in_bits_pc; // @[operandCollector.scala 372:27]
  assign io_out_2_bits_spike_info_pc = io_in_bits_spike_info_pc; // @[operandCollector.scala 372:27]
  assign io_out_2_bits_spike_info_inst = io_in_bits_spike_info_inst; // @[operandCollector.scala 372:27]
  assign io_out_3_valid = _io_out_0_valid_T_2 == 2'h3 & io_in_valid; // @[operandCollector.scala 376:70]
  assign io_out_3_bits_inst = io_in_bits_inst; // @[operandCollector.scala 372:27]
  assign io_out_3_bits_wid = io_in_bits_wid; // @[operandCollector.scala 372:27]
  assign io_out_3_bits_fp = io_in_bits_fp; // @[operandCollector.scala 372:27]
  assign io_out_3_bits_branch = io_in_bits_branch; // @[operandCollector.scala 372:27]
  assign io_out_3_bits_simt_stack = io_in_bits_simt_stack; // @[operandCollector.scala 372:27]
  assign io_out_3_bits_simt_stack_op = io_in_bits_simt_stack_op; // @[operandCollector.scala 372:27]
  assign io_out_3_bits_barrier = io_in_bits_barrier; // @[operandCollector.scala 372:27]
  assign io_out_3_bits_csr = io_in_bits_csr; // @[operandCollector.scala 372:27]
  assign io_out_3_bits_reverse = io_in_bits_reverse; // @[operandCollector.scala 372:27]
  assign io_out_3_bits_sel_alu2 = io_in_bits_sel_alu2; // @[operandCollector.scala 372:27]
  assign io_out_3_bits_sel_alu1 = io_in_bits_sel_alu1; // @[operandCollector.scala 372:27]
  assign io_out_3_bits_isvec = io_in_bits_isvec; // @[operandCollector.scala 372:27]
  assign io_out_3_bits_sel_alu3 = io_in_bits_sel_alu3; // @[operandCollector.scala 372:27]
  assign io_out_3_bits_mask = io_in_bits_mask; // @[operandCollector.scala 372:27]
  assign io_out_3_bits_sel_imm = io_in_bits_sel_imm; // @[operandCollector.scala 372:27]
  assign io_out_3_bits_mem_whb = io_in_bits_mem_whb; // @[operandCollector.scala 372:27]
  assign io_out_3_bits_mem_unsigned = io_in_bits_mem_unsigned; // @[operandCollector.scala 372:27]
  assign io_out_3_bits_alu_fn = io_in_bits_alu_fn; // @[operandCollector.scala 372:27]
  assign io_out_3_bits_mem = io_in_bits_mem; // @[operandCollector.scala 372:27]
  assign io_out_3_bits_mul = io_in_bits_mul; // @[operandCollector.scala 372:27]
  assign io_out_3_bits_mem_cmd = io_in_bits_mem_cmd; // @[operandCollector.scala 372:27]
  assign io_out_3_bits_mop = io_in_bits_mop; // @[operandCollector.scala 372:27]
  assign io_out_3_bits_reg_idx1 = io_in_bits_reg_idx1; // @[operandCollector.scala 372:27]
  assign io_out_3_bits_reg_idx2 = io_in_bits_reg_idx2; // @[operandCollector.scala 372:27]
  assign io_out_3_bits_reg_idx3 = io_in_bits_reg_idx3; // @[operandCollector.scala 372:27]
  assign io_out_3_bits_reg_idxw = io_in_bits_reg_idxw; // @[operandCollector.scala 372:27]
  assign io_out_3_bits_wfd = io_in_bits_wfd; // @[operandCollector.scala 372:27]
  assign io_out_3_bits_fence = io_in_bits_fence; // @[operandCollector.scala 372:27]
  assign io_out_3_bits_sfu = io_in_bits_sfu; // @[operandCollector.scala 372:27]
  assign io_out_3_bits_readmask = io_in_bits_readmask; // @[operandCollector.scala 372:27]
  assign io_out_3_bits_writemask = io_in_bits_writemask; // @[operandCollector.scala 372:27]
  assign io_out_3_bits_wxd = io_in_bits_wxd; // @[operandCollector.scala 372:27]
  assign io_out_3_bits_pc = io_in_bits_pc; // @[operandCollector.scala 372:27]
  assign io_out_3_bits_spike_info_pc = io_in_bits_spike_info_pc; // @[operandCollector.scala 372:27]
  assign io_out_3_bits_spike_info_inst = io_in_bits_spike_info_inst; // @[operandCollector.scala 372:27]
endmodule
module Arbiter(
  output        io_in_0_ready,
  input         io_in_0_valid,
  input  [31:0] io_in_0_bits_alu_src1_0,
  input  [31:0] io_in_0_bits_alu_src1_1,
  input  [31:0] io_in_0_bits_alu_src1_2,
  input  [31:0] io_in_0_bits_alu_src1_3,
  input  [31:0] io_in_0_bits_alu_src2_0,
  input  [31:0] io_in_0_bits_alu_src2_1,
  input  [31:0] io_in_0_bits_alu_src2_2,
  input  [31:0] io_in_0_bits_alu_src2_3,
  input  [31:0] io_in_0_bits_alu_src3_0,
  input  [31:0] io_in_0_bits_alu_src3_1,
  input  [31:0] io_in_0_bits_alu_src3_2,
  input  [31:0] io_in_0_bits_alu_src3_3,
  input         io_in_0_bits_mask_0,
  input         io_in_0_bits_mask_1,
  input         io_in_0_bits_mask_2,
  input         io_in_0_bits_mask_3,
  input  [31:0] io_in_0_bits_control_inst,
  input  [1:0]  io_in_0_bits_control_wid,
  input         io_in_0_bits_control_fp,
  input  [1:0]  io_in_0_bits_control_branch,
  input         io_in_0_bits_control_simt_stack,
  input         io_in_0_bits_control_simt_stack_op,
  input         io_in_0_bits_control_barrier,
  input  [1:0]  io_in_0_bits_control_csr,
  input         io_in_0_bits_control_reverse,
  input  [1:0]  io_in_0_bits_control_sel_alu2,
  input  [1:0]  io_in_0_bits_control_sel_alu1,
  input         io_in_0_bits_control_isvec,
  input  [1:0]  io_in_0_bits_control_sel_alu3,
  input         io_in_0_bits_control_mask,
  input  [2:0]  io_in_0_bits_control_sel_imm,
  input  [1:0]  io_in_0_bits_control_mem_whb,
  input         io_in_0_bits_control_mem_unsigned,
  input  [5:0]  io_in_0_bits_control_alu_fn,
  input         io_in_0_bits_control_mem,
  input         io_in_0_bits_control_mul,
  input  [1:0]  io_in_0_bits_control_mem_cmd,
  input  [1:0]  io_in_0_bits_control_mop,
  input  [4:0]  io_in_0_bits_control_reg_idx1,
  input  [4:0]  io_in_0_bits_control_reg_idx2,
  input  [4:0]  io_in_0_bits_control_reg_idx3,
  input  [4:0]  io_in_0_bits_control_reg_idxw,
  input         io_in_0_bits_control_wfd,
  input         io_in_0_bits_control_fence,
  input         io_in_0_bits_control_sfu,
  input         io_in_0_bits_control_readmask,
  input         io_in_0_bits_control_writemask,
  input         io_in_0_bits_control_wxd,
  input  [31:0] io_in_0_bits_control_pc,
  input  [31:0] io_in_0_bits_control_spike_info_pc,
  input  [31:0] io_in_0_bits_control_spike_info_inst,
  output        io_in_1_ready,
  input         io_in_1_valid,
  input  [31:0] io_in_1_bits_alu_src1_0,
  input  [31:0] io_in_1_bits_alu_src1_1,
  input  [31:0] io_in_1_bits_alu_src1_2,
  input  [31:0] io_in_1_bits_alu_src1_3,
  input  [31:0] io_in_1_bits_alu_src2_0,
  input  [31:0] io_in_1_bits_alu_src2_1,
  input  [31:0] io_in_1_bits_alu_src2_2,
  input  [31:0] io_in_1_bits_alu_src2_3,
  input  [31:0] io_in_1_bits_alu_src3_0,
  input  [31:0] io_in_1_bits_alu_src3_1,
  input  [31:0] io_in_1_bits_alu_src3_2,
  input  [31:0] io_in_1_bits_alu_src3_3,
  input         io_in_1_bits_mask_0,
  input         io_in_1_bits_mask_1,
  input         io_in_1_bits_mask_2,
  input         io_in_1_bits_mask_3,
  input  [31:0] io_in_1_bits_control_inst,
  input  [1:0]  io_in_1_bits_control_wid,
  input         io_in_1_bits_control_fp,
  input  [1:0]  io_in_1_bits_control_branch,
  input         io_in_1_bits_control_simt_stack,
  input         io_in_1_bits_control_simt_stack_op,
  input         io_in_1_bits_control_barrier,
  input  [1:0]  io_in_1_bits_control_csr,
  input         io_in_1_bits_control_reverse,
  input  [1:0]  io_in_1_bits_control_sel_alu2,
  input  [1:0]  io_in_1_bits_control_sel_alu1,
  input         io_in_1_bits_control_isvec,
  input  [1:0]  io_in_1_bits_control_sel_alu3,
  input         io_in_1_bits_control_mask,
  input  [2:0]  io_in_1_bits_control_sel_imm,
  input  [1:0]  io_in_1_bits_control_mem_whb,
  input         io_in_1_bits_control_mem_unsigned,
  input  [5:0]  io_in_1_bits_control_alu_fn,
  input         io_in_1_bits_control_mem,
  input         io_in_1_bits_control_mul,
  input  [1:0]  io_in_1_bits_control_mem_cmd,
  input  [1:0]  io_in_1_bits_control_mop,
  input  [4:0]  io_in_1_bits_control_reg_idx1,
  input  [4:0]  io_in_1_bits_control_reg_idx2,
  input  [4:0]  io_in_1_bits_control_reg_idx3,
  input  [4:0]  io_in_1_bits_control_reg_idxw,
  input         io_in_1_bits_control_wfd,
  input         io_in_1_bits_control_fence,
  input         io_in_1_bits_control_sfu,
  input         io_in_1_bits_control_readmask,
  input         io_in_1_bits_control_writemask,
  input         io_in_1_bits_control_wxd,
  input  [31:0] io_in_1_bits_control_pc,
  input  [31:0] io_in_1_bits_control_spike_info_pc,
  input  [31:0] io_in_1_bits_control_spike_info_inst,
  output        io_in_2_ready,
  input         io_in_2_valid,
  input  [31:0] io_in_2_bits_alu_src1_0,
  input  [31:0] io_in_2_bits_alu_src1_1,
  input  [31:0] io_in_2_bits_alu_src1_2,
  input  [31:0] io_in_2_bits_alu_src1_3,
  input  [31:0] io_in_2_bits_alu_src2_0,
  input  [31:0] io_in_2_bits_alu_src2_1,
  input  [31:0] io_in_2_bits_alu_src2_2,
  input  [31:0] io_in_2_bits_alu_src2_3,
  input  [31:0] io_in_2_bits_alu_src3_0,
  input  [31:0] io_in_2_bits_alu_src3_1,
  input  [31:0] io_in_2_bits_alu_src3_2,
  input  [31:0] io_in_2_bits_alu_src3_3,
  input         io_in_2_bits_mask_0,
  input         io_in_2_bits_mask_1,
  input         io_in_2_bits_mask_2,
  input         io_in_2_bits_mask_3,
  input  [31:0] io_in_2_bits_control_inst,
  input  [1:0]  io_in_2_bits_control_wid,
  input         io_in_2_bits_control_fp,
  input  [1:0]  io_in_2_bits_control_branch,
  input         io_in_2_bits_control_simt_stack,
  input         io_in_2_bits_control_simt_stack_op,
  input         io_in_2_bits_control_barrier,
  input  [1:0]  io_in_2_bits_control_csr,
  input         io_in_2_bits_control_reverse,
  input  [1:0]  io_in_2_bits_control_sel_alu2,
  input  [1:0]  io_in_2_bits_control_sel_alu1,
  input         io_in_2_bits_control_isvec,
  input  [1:0]  io_in_2_bits_control_sel_alu3,
  input         io_in_2_bits_control_mask,
  input  [2:0]  io_in_2_bits_control_sel_imm,
  input  [1:0]  io_in_2_bits_control_mem_whb,
  input         io_in_2_bits_control_mem_unsigned,
  input  [5:0]  io_in_2_bits_control_alu_fn,
  input         io_in_2_bits_control_mem,
  input         io_in_2_bits_control_mul,
  input  [1:0]  io_in_2_bits_control_mem_cmd,
  input  [1:0]  io_in_2_bits_control_mop,
  input  [4:0]  io_in_2_bits_control_reg_idx1,
  input  [4:0]  io_in_2_bits_control_reg_idx2,
  input  [4:0]  io_in_2_bits_control_reg_idx3,
  input  [4:0]  io_in_2_bits_control_reg_idxw,
  input         io_in_2_bits_control_wfd,
  input         io_in_2_bits_control_fence,
  input         io_in_2_bits_control_sfu,
  input         io_in_2_bits_control_readmask,
  input         io_in_2_bits_control_writemask,
  input         io_in_2_bits_control_wxd,
  input  [31:0] io_in_2_bits_control_pc,
  input  [31:0] io_in_2_bits_control_spike_info_pc,
  input  [31:0] io_in_2_bits_control_spike_info_inst,
  output        io_in_3_ready,
  input         io_in_3_valid,
  input  [31:0] io_in_3_bits_alu_src1_0,
  input  [31:0] io_in_3_bits_alu_src1_1,
  input  [31:0] io_in_3_bits_alu_src1_2,
  input  [31:0] io_in_3_bits_alu_src1_3,
  input  [31:0] io_in_3_bits_alu_src2_0,
  input  [31:0] io_in_3_bits_alu_src2_1,
  input  [31:0] io_in_3_bits_alu_src2_2,
  input  [31:0] io_in_3_bits_alu_src2_3,
  input  [31:0] io_in_3_bits_alu_src3_0,
  input  [31:0] io_in_3_bits_alu_src3_1,
  input  [31:0] io_in_3_bits_alu_src3_2,
  input  [31:0] io_in_3_bits_alu_src3_3,
  input         io_in_3_bits_mask_0,
  input         io_in_3_bits_mask_1,
  input         io_in_3_bits_mask_2,
  input         io_in_3_bits_mask_3,
  input  [31:0] io_in_3_bits_control_inst,
  input  [1:0]  io_in_3_bits_control_wid,
  input         io_in_3_bits_control_fp,
  input  [1:0]  io_in_3_bits_control_branch,
  input         io_in_3_bits_control_simt_stack,
  input         io_in_3_bits_control_simt_stack_op,
  input         io_in_3_bits_control_barrier,
  input  [1:0]  io_in_3_bits_control_csr,
  input         io_in_3_bits_control_reverse,
  input  [1:0]  io_in_3_bits_control_sel_alu2,
  input  [1:0]  io_in_3_bits_control_sel_alu1,
  input         io_in_3_bits_control_isvec,
  input  [1:0]  io_in_3_bits_control_sel_alu3,
  input         io_in_3_bits_control_mask,
  input  [2:0]  io_in_3_bits_control_sel_imm,
  input  [1:0]  io_in_3_bits_control_mem_whb,
  input         io_in_3_bits_control_mem_unsigned,
  input  [5:0]  io_in_3_bits_control_alu_fn,
  input         io_in_3_bits_control_mem,
  input         io_in_3_bits_control_mul,
  input  [1:0]  io_in_3_bits_control_mem_cmd,
  input  [1:0]  io_in_3_bits_control_mop,
  input  [4:0]  io_in_3_bits_control_reg_idx1,
  input  [4:0]  io_in_3_bits_control_reg_idx2,
  input  [4:0]  io_in_3_bits_control_reg_idx3,
  input  [4:0]  io_in_3_bits_control_reg_idxw,
  input         io_in_3_bits_control_wfd,
  input         io_in_3_bits_control_fence,
  input         io_in_3_bits_control_sfu,
  input         io_in_3_bits_control_readmask,
  input         io_in_3_bits_control_writemask,
  input         io_in_3_bits_control_wxd,
  input  [31:0] io_in_3_bits_control_pc,
  input  [31:0] io_in_3_bits_control_spike_info_pc,
  input  [31:0] io_in_3_bits_control_spike_info_inst,
  input         io_out_ready,
  output        io_out_valid,
  output [31:0] io_out_bits_alu_src1_0,
  output [31:0] io_out_bits_alu_src1_1,
  output [31:0] io_out_bits_alu_src1_2,
  output [31:0] io_out_bits_alu_src1_3,
  output [31:0] io_out_bits_alu_src2_0,
  output [31:0] io_out_bits_alu_src2_1,
  output [31:0] io_out_bits_alu_src2_2,
  output [31:0] io_out_bits_alu_src2_3,
  output [31:0] io_out_bits_alu_src3_0,
  output [31:0] io_out_bits_alu_src3_1,
  output [31:0] io_out_bits_alu_src3_2,
  output [31:0] io_out_bits_alu_src3_3,
  output        io_out_bits_mask_0,
  output        io_out_bits_mask_1,
  output        io_out_bits_mask_2,
  output        io_out_bits_mask_3,
  output [31:0] io_out_bits_control_inst,
  output [1:0]  io_out_bits_control_wid,
  output        io_out_bits_control_fp,
  output [1:0]  io_out_bits_control_branch,
  output        io_out_bits_control_simt_stack,
  output        io_out_bits_control_simt_stack_op,
  output        io_out_bits_control_barrier,
  output [1:0]  io_out_bits_control_csr,
  output        io_out_bits_control_reverse,
  output [1:0]  io_out_bits_control_sel_alu2,
  output [1:0]  io_out_bits_control_sel_alu1,
  output        io_out_bits_control_isvec,
  output [1:0]  io_out_bits_control_sel_alu3,
  output        io_out_bits_control_mask,
  output [2:0]  io_out_bits_control_sel_imm,
  output [1:0]  io_out_bits_control_mem_whb,
  output        io_out_bits_control_mem_unsigned,
  output [5:0]  io_out_bits_control_alu_fn,
  output        io_out_bits_control_mem,
  output        io_out_bits_control_mul,
  output [1:0]  io_out_bits_control_mem_cmd,
  output [1:0]  io_out_bits_control_mop,
  output [4:0]  io_out_bits_control_reg_idx1,
  output [4:0]  io_out_bits_control_reg_idx2,
  output [4:0]  io_out_bits_control_reg_idx3,
  output [4:0]  io_out_bits_control_reg_idxw,
  output        io_out_bits_control_wfd,
  output        io_out_bits_control_fence,
  output        io_out_bits_control_sfu,
  output        io_out_bits_control_readmask,
  output        io_out_bits_control_writemask,
  output        io_out_bits_control_wxd,
  output [31:0] io_out_bits_control_pc,
  output [31:0] io_out_bits_control_spike_info_pc,
  output [31:0] io_out_bits_control_spike_info_inst
);
  wire [31:0] _GEN_1 = io_in_2_valid ? io_in_2_bits_alu_src1_0 : io_in_3_bits_alu_src1_0; // @[Arbiter.scala 136:15 138:26 140:19]
  wire [31:0] _GEN_2 = io_in_2_valid ? io_in_2_bits_alu_src1_1 : io_in_3_bits_alu_src1_1; // @[Arbiter.scala 136:15 138:26 140:19]
  wire [31:0] _GEN_3 = io_in_2_valid ? io_in_2_bits_alu_src1_2 : io_in_3_bits_alu_src1_2; // @[Arbiter.scala 136:15 138:26 140:19]
  wire [31:0] _GEN_4 = io_in_2_valid ? io_in_2_bits_alu_src1_3 : io_in_3_bits_alu_src1_3; // @[Arbiter.scala 136:15 138:26 140:19]
  wire [31:0] _GEN_5 = io_in_2_valid ? io_in_2_bits_alu_src2_0 : io_in_3_bits_alu_src2_0; // @[Arbiter.scala 136:15 138:26 140:19]
  wire [31:0] _GEN_6 = io_in_2_valid ? io_in_2_bits_alu_src2_1 : io_in_3_bits_alu_src2_1; // @[Arbiter.scala 136:15 138:26 140:19]
  wire [31:0] _GEN_7 = io_in_2_valid ? io_in_2_bits_alu_src2_2 : io_in_3_bits_alu_src2_2; // @[Arbiter.scala 136:15 138:26 140:19]
  wire [31:0] _GEN_8 = io_in_2_valid ? io_in_2_bits_alu_src2_3 : io_in_3_bits_alu_src2_3; // @[Arbiter.scala 136:15 138:26 140:19]
  wire [31:0] _GEN_9 = io_in_2_valid ? io_in_2_bits_alu_src3_0 : io_in_3_bits_alu_src3_0; // @[Arbiter.scala 136:15 138:26 140:19]
  wire [31:0] _GEN_10 = io_in_2_valid ? io_in_2_bits_alu_src3_1 : io_in_3_bits_alu_src3_1; // @[Arbiter.scala 136:15 138:26 140:19]
  wire [31:0] _GEN_11 = io_in_2_valid ? io_in_2_bits_alu_src3_2 : io_in_3_bits_alu_src3_2; // @[Arbiter.scala 136:15 138:26 140:19]
  wire [31:0] _GEN_12 = io_in_2_valid ? io_in_2_bits_alu_src3_3 : io_in_3_bits_alu_src3_3; // @[Arbiter.scala 136:15 138:26 140:19]
  wire  _GEN_13 = io_in_2_valid ? io_in_2_bits_mask_0 : io_in_3_bits_mask_0; // @[Arbiter.scala 136:15 138:26 140:19]
  wire  _GEN_14 = io_in_2_valid ? io_in_2_bits_mask_1 : io_in_3_bits_mask_1; // @[Arbiter.scala 136:15 138:26 140:19]
  wire  _GEN_15 = io_in_2_valid ? io_in_2_bits_mask_2 : io_in_3_bits_mask_2; // @[Arbiter.scala 136:15 138:26 140:19]
  wire  _GEN_16 = io_in_2_valid ? io_in_2_bits_mask_3 : io_in_3_bits_mask_3; // @[Arbiter.scala 136:15 138:26 140:19]
  wire [31:0] _GEN_17 = io_in_2_valid ? io_in_2_bits_control_inst : io_in_3_bits_control_inst; // @[Arbiter.scala 136:15 138:26 140:19]
  wire [1:0] _GEN_18 = io_in_2_valid ? io_in_2_bits_control_wid : io_in_3_bits_control_wid; // @[Arbiter.scala 136:15 138:26 140:19]
  wire  _GEN_19 = io_in_2_valid ? io_in_2_bits_control_fp : io_in_3_bits_control_fp; // @[Arbiter.scala 136:15 138:26 140:19]
  wire [1:0] _GEN_20 = io_in_2_valid ? io_in_2_bits_control_branch : io_in_3_bits_control_branch; // @[Arbiter.scala 136:15 138:26 140:19]
  wire  _GEN_21 = io_in_2_valid ? io_in_2_bits_control_simt_stack : io_in_3_bits_control_simt_stack; // @[Arbiter.scala 136:15 138:26 140:19]
  wire  _GEN_22 = io_in_2_valid ? io_in_2_bits_control_simt_stack_op : io_in_3_bits_control_simt_stack_op; // @[Arbiter.scala 136:15 138:26 140:19]
  wire  _GEN_23 = io_in_2_valid ? io_in_2_bits_control_barrier : io_in_3_bits_control_barrier; // @[Arbiter.scala 136:15 138:26 140:19]
  wire [1:0] _GEN_24 = io_in_2_valid ? io_in_2_bits_control_csr : io_in_3_bits_control_csr; // @[Arbiter.scala 136:15 138:26 140:19]
  wire  _GEN_25 = io_in_2_valid ? io_in_2_bits_control_reverse : io_in_3_bits_control_reverse; // @[Arbiter.scala 136:15 138:26 140:19]
  wire [1:0] _GEN_26 = io_in_2_valid ? io_in_2_bits_control_sel_alu2 : io_in_3_bits_control_sel_alu2; // @[Arbiter.scala 136:15 138:26 140:19]
  wire [1:0] _GEN_27 = io_in_2_valid ? io_in_2_bits_control_sel_alu1 : io_in_3_bits_control_sel_alu1; // @[Arbiter.scala 136:15 138:26 140:19]
  wire  _GEN_28 = io_in_2_valid ? io_in_2_bits_control_isvec : io_in_3_bits_control_isvec; // @[Arbiter.scala 136:15 138:26 140:19]
  wire [1:0] _GEN_29 = io_in_2_valid ? io_in_2_bits_control_sel_alu3 : io_in_3_bits_control_sel_alu3; // @[Arbiter.scala 136:15 138:26 140:19]
  wire  _GEN_30 = io_in_2_valid ? io_in_2_bits_control_mask : io_in_3_bits_control_mask; // @[Arbiter.scala 136:15 138:26 140:19]
  wire [2:0] _GEN_31 = io_in_2_valid ? io_in_2_bits_control_sel_imm : io_in_3_bits_control_sel_imm; // @[Arbiter.scala 136:15 138:26 140:19]
  wire [1:0] _GEN_32 = io_in_2_valid ? io_in_2_bits_control_mem_whb : io_in_3_bits_control_mem_whb; // @[Arbiter.scala 136:15 138:26 140:19]
  wire  _GEN_33 = io_in_2_valid ? io_in_2_bits_control_mem_unsigned : io_in_3_bits_control_mem_unsigned; // @[Arbiter.scala 136:15 138:26 140:19]
  wire [5:0] _GEN_34 = io_in_2_valid ? io_in_2_bits_control_alu_fn : io_in_3_bits_control_alu_fn; // @[Arbiter.scala 136:15 138:26 140:19]
  wire  _GEN_35 = io_in_2_valid ? io_in_2_bits_control_mem : io_in_3_bits_control_mem; // @[Arbiter.scala 136:15 138:26 140:19]
  wire  _GEN_36 = io_in_2_valid ? io_in_2_bits_control_mul : io_in_3_bits_control_mul; // @[Arbiter.scala 136:15 138:26 140:19]
  wire [1:0] _GEN_37 = io_in_2_valid ? io_in_2_bits_control_mem_cmd : io_in_3_bits_control_mem_cmd; // @[Arbiter.scala 136:15 138:26 140:19]
  wire [1:0] _GEN_38 = io_in_2_valid ? io_in_2_bits_control_mop : io_in_3_bits_control_mop; // @[Arbiter.scala 136:15 138:26 140:19]
  wire [4:0] _GEN_39 = io_in_2_valid ? io_in_2_bits_control_reg_idx1 : io_in_3_bits_control_reg_idx1; // @[Arbiter.scala 136:15 138:26 140:19]
  wire [4:0] _GEN_40 = io_in_2_valid ? io_in_2_bits_control_reg_idx2 : io_in_3_bits_control_reg_idx2; // @[Arbiter.scala 136:15 138:26 140:19]
  wire [4:0] _GEN_41 = io_in_2_valid ? io_in_2_bits_control_reg_idx3 : io_in_3_bits_control_reg_idx3; // @[Arbiter.scala 136:15 138:26 140:19]
  wire [4:0] _GEN_42 = io_in_2_valid ? io_in_2_bits_control_reg_idxw : io_in_3_bits_control_reg_idxw; // @[Arbiter.scala 136:15 138:26 140:19]
  wire  _GEN_43 = io_in_2_valid ? io_in_2_bits_control_wfd : io_in_3_bits_control_wfd; // @[Arbiter.scala 136:15 138:26 140:19]
  wire  _GEN_44 = io_in_2_valid ? io_in_2_bits_control_fence : io_in_3_bits_control_fence; // @[Arbiter.scala 136:15 138:26 140:19]
  wire  _GEN_45 = io_in_2_valid ? io_in_2_bits_control_sfu : io_in_3_bits_control_sfu; // @[Arbiter.scala 136:15 138:26 140:19]
  wire  _GEN_46 = io_in_2_valid ? io_in_2_bits_control_readmask : io_in_3_bits_control_readmask; // @[Arbiter.scala 136:15 138:26 140:19]
  wire  _GEN_47 = io_in_2_valid ? io_in_2_bits_control_writemask : io_in_3_bits_control_writemask; // @[Arbiter.scala 136:15 138:26 140:19]
  wire  _GEN_48 = io_in_2_valid ? io_in_2_bits_control_wxd : io_in_3_bits_control_wxd; // @[Arbiter.scala 136:15 138:26 140:19]
  wire [31:0] _GEN_49 = io_in_2_valid ? io_in_2_bits_control_pc : io_in_3_bits_control_pc; // @[Arbiter.scala 136:15 138:26 140:19]
  wire [31:0] _GEN_50 = io_in_2_valid ? io_in_2_bits_control_spike_info_pc : io_in_3_bits_control_spike_info_pc; // @[Arbiter.scala 136:15 138:26 140:19]
  wire [31:0] _GEN_51 = io_in_2_valid ? io_in_2_bits_control_spike_info_inst : io_in_3_bits_control_spike_info_inst; // @[Arbiter.scala 136:15 138:26 140:19]
  wire [31:0] _GEN_53 = io_in_1_valid ? io_in_1_bits_alu_src1_0 : _GEN_1; // @[Arbiter.scala 138:26 140:19]
  wire [31:0] _GEN_54 = io_in_1_valid ? io_in_1_bits_alu_src1_1 : _GEN_2; // @[Arbiter.scala 138:26 140:19]
  wire [31:0] _GEN_55 = io_in_1_valid ? io_in_1_bits_alu_src1_2 : _GEN_3; // @[Arbiter.scala 138:26 140:19]
  wire [31:0] _GEN_56 = io_in_1_valid ? io_in_1_bits_alu_src1_3 : _GEN_4; // @[Arbiter.scala 138:26 140:19]
  wire [31:0] _GEN_57 = io_in_1_valid ? io_in_1_bits_alu_src2_0 : _GEN_5; // @[Arbiter.scala 138:26 140:19]
  wire [31:0] _GEN_58 = io_in_1_valid ? io_in_1_bits_alu_src2_1 : _GEN_6; // @[Arbiter.scala 138:26 140:19]
  wire [31:0] _GEN_59 = io_in_1_valid ? io_in_1_bits_alu_src2_2 : _GEN_7; // @[Arbiter.scala 138:26 140:19]
  wire [31:0] _GEN_60 = io_in_1_valid ? io_in_1_bits_alu_src2_3 : _GEN_8; // @[Arbiter.scala 138:26 140:19]
  wire [31:0] _GEN_61 = io_in_1_valid ? io_in_1_bits_alu_src3_0 : _GEN_9; // @[Arbiter.scala 138:26 140:19]
  wire [31:0] _GEN_62 = io_in_1_valid ? io_in_1_bits_alu_src3_1 : _GEN_10; // @[Arbiter.scala 138:26 140:19]
  wire [31:0] _GEN_63 = io_in_1_valid ? io_in_1_bits_alu_src3_2 : _GEN_11; // @[Arbiter.scala 138:26 140:19]
  wire [31:0] _GEN_64 = io_in_1_valid ? io_in_1_bits_alu_src3_3 : _GEN_12; // @[Arbiter.scala 138:26 140:19]
  wire  _GEN_65 = io_in_1_valid ? io_in_1_bits_mask_0 : _GEN_13; // @[Arbiter.scala 138:26 140:19]
  wire  _GEN_66 = io_in_1_valid ? io_in_1_bits_mask_1 : _GEN_14; // @[Arbiter.scala 138:26 140:19]
  wire  _GEN_67 = io_in_1_valid ? io_in_1_bits_mask_2 : _GEN_15; // @[Arbiter.scala 138:26 140:19]
  wire  _GEN_68 = io_in_1_valid ? io_in_1_bits_mask_3 : _GEN_16; // @[Arbiter.scala 138:26 140:19]
  wire [31:0] _GEN_69 = io_in_1_valid ? io_in_1_bits_control_inst : _GEN_17; // @[Arbiter.scala 138:26 140:19]
  wire [1:0] _GEN_70 = io_in_1_valid ? io_in_1_bits_control_wid : _GEN_18; // @[Arbiter.scala 138:26 140:19]
  wire  _GEN_71 = io_in_1_valid ? io_in_1_bits_control_fp : _GEN_19; // @[Arbiter.scala 138:26 140:19]
  wire [1:0] _GEN_72 = io_in_1_valid ? io_in_1_bits_control_branch : _GEN_20; // @[Arbiter.scala 138:26 140:19]
  wire  _GEN_73 = io_in_1_valid ? io_in_1_bits_control_simt_stack : _GEN_21; // @[Arbiter.scala 138:26 140:19]
  wire  _GEN_74 = io_in_1_valid ? io_in_1_bits_control_simt_stack_op : _GEN_22; // @[Arbiter.scala 138:26 140:19]
  wire  _GEN_75 = io_in_1_valid ? io_in_1_bits_control_barrier : _GEN_23; // @[Arbiter.scala 138:26 140:19]
  wire [1:0] _GEN_76 = io_in_1_valid ? io_in_1_bits_control_csr : _GEN_24; // @[Arbiter.scala 138:26 140:19]
  wire  _GEN_77 = io_in_1_valid ? io_in_1_bits_control_reverse : _GEN_25; // @[Arbiter.scala 138:26 140:19]
  wire [1:0] _GEN_78 = io_in_1_valid ? io_in_1_bits_control_sel_alu2 : _GEN_26; // @[Arbiter.scala 138:26 140:19]
  wire [1:0] _GEN_79 = io_in_1_valid ? io_in_1_bits_control_sel_alu1 : _GEN_27; // @[Arbiter.scala 138:26 140:19]
  wire  _GEN_80 = io_in_1_valid ? io_in_1_bits_control_isvec : _GEN_28; // @[Arbiter.scala 138:26 140:19]
  wire [1:0] _GEN_81 = io_in_1_valid ? io_in_1_bits_control_sel_alu3 : _GEN_29; // @[Arbiter.scala 138:26 140:19]
  wire  _GEN_82 = io_in_1_valid ? io_in_1_bits_control_mask : _GEN_30; // @[Arbiter.scala 138:26 140:19]
  wire [2:0] _GEN_83 = io_in_1_valid ? io_in_1_bits_control_sel_imm : _GEN_31; // @[Arbiter.scala 138:26 140:19]
  wire [1:0] _GEN_84 = io_in_1_valid ? io_in_1_bits_control_mem_whb : _GEN_32; // @[Arbiter.scala 138:26 140:19]
  wire  _GEN_85 = io_in_1_valid ? io_in_1_bits_control_mem_unsigned : _GEN_33; // @[Arbiter.scala 138:26 140:19]
  wire [5:0] _GEN_86 = io_in_1_valid ? io_in_1_bits_control_alu_fn : _GEN_34; // @[Arbiter.scala 138:26 140:19]
  wire  _GEN_87 = io_in_1_valid ? io_in_1_bits_control_mem : _GEN_35; // @[Arbiter.scala 138:26 140:19]
  wire  _GEN_88 = io_in_1_valid ? io_in_1_bits_control_mul : _GEN_36; // @[Arbiter.scala 138:26 140:19]
  wire [1:0] _GEN_89 = io_in_1_valid ? io_in_1_bits_control_mem_cmd : _GEN_37; // @[Arbiter.scala 138:26 140:19]
  wire [1:0] _GEN_90 = io_in_1_valid ? io_in_1_bits_control_mop : _GEN_38; // @[Arbiter.scala 138:26 140:19]
  wire [4:0] _GEN_91 = io_in_1_valid ? io_in_1_bits_control_reg_idx1 : _GEN_39; // @[Arbiter.scala 138:26 140:19]
  wire [4:0] _GEN_92 = io_in_1_valid ? io_in_1_bits_control_reg_idx2 : _GEN_40; // @[Arbiter.scala 138:26 140:19]
  wire [4:0] _GEN_93 = io_in_1_valid ? io_in_1_bits_control_reg_idx3 : _GEN_41; // @[Arbiter.scala 138:26 140:19]
  wire [4:0] _GEN_94 = io_in_1_valid ? io_in_1_bits_control_reg_idxw : _GEN_42; // @[Arbiter.scala 138:26 140:19]
  wire  _GEN_95 = io_in_1_valid ? io_in_1_bits_control_wfd : _GEN_43; // @[Arbiter.scala 138:26 140:19]
  wire  _GEN_96 = io_in_1_valid ? io_in_1_bits_control_fence : _GEN_44; // @[Arbiter.scala 138:26 140:19]
  wire  _GEN_97 = io_in_1_valid ? io_in_1_bits_control_sfu : _GEN_45; // @[Arbiter.scala 138:26 140:19]
  wire  _GEN_98 = io_in_1_valid ? io_in_1_bits_control_readmask : _GEN_46; // @[Arbiter.scala 138:26 140:19]
  wire  _GEN_99 = io_in_1_valid ? io_in_1_bits_control_writemask : _GEN_47; // @[Arbiter.scala 138:26 140:19]
  wire  _GEN_100 = io_in_1_valid ? io_in_1_bits_control_wxd : _GEN_48; // @[Arbiter.scala 138:26 140:19]
  wire [31:0] _GEN_101 = io_in_1_valid ? io_in_1_bits_control_pc : _GEN_49; // @[Arbiter.scala 138:26 140:19]
  wire [31:0] _GEN_102 = io_in_1_valid ? io_in_1_bits_control_spike_info_pc : _GEN_50; // @[Arbiter.scala 138:26 140:19]
  wire [31:0] _GEN_103 = io_in_1_valid ? io_in_1_bits_control_spike_info_inst : _GEN_51; // @[Arbiter.scala 138:26 140:19]
  wire  grant_1 = ~io_in_0_valid; // @[Arbiter.scala 45:78]
  wire  grant_2 = ~(io_in_0_valid | io_in_1_valid); // @[Arbiter.scala 45:78]
  wire  grant_3 = ~(io_in_0_valid | io_in_1_valid | io_in_2_valid); // @[Arbiter.scala 45:78]
  assign io_in_0_ready = io_out_ready; // @[Arbiter.scala 146:19]
  assign io_in_1_ready = grant_1 & io_out_ready; // @[Arbiter.scala 146:19]
  assign io_in_2_ready = grant_2 & io_out_ready; // @[Arbiter.scala 146:19]
  assign io_in_3_ready = grant_3 & io_out_ready; // @[Arbiter.scala 146:19]
  assign io_out_valid = ~grant_3 | io_in_3_valid; // @[Arbiter.scala 147:31]
  assign io_out_bits_alu_src1_0 = io_in_0_valid ? io_in_0_bits_alu_src1_0 : _GEN_53; // @[Arbiter.scala 138:26 140:19]
  assign io_out_bits_alu_src1_1 = io_in_0_valid ? io_in_0_bits_alu_src1_1 : _GEN_54; // @[Arbiter.scala 138:26 140:19]
  assign io_out_bits_alu_src1_2 = io_in_0_valid ? io_in_0_bits_alu_src1_2 : _GEN_55; // @[Arbiter.scala 138:26 140:19]
  assign io_out_bits_alu_src1_3 = io_in_0_valid ? io_in_0_bits_alu_src1_3 : _GEN_56; // @[Arbiter.scala 138:26 140:19]
  assign io_out_bits_alu_src2_0 = io_in_0_valid ? io_in_0_bits_alu_src2_0 : _GEN_57; // @[Arbiter.scala 138:26 140:19]
  assign io_out_bits_alu_src2_1 = io_in_0_valid ? io_in_0_bits_alu_src2_1 : _GEN_58; // @[Arbiter.scala 138:26 140:19]
  assign io_out_bits_alu_src2_2 = io_in_0_valid ? io_in_0_bits_alu_src2_2 : _GEN_59; // @[Arbiter.scala 138:26 140:19]
  assign io_out_bits_alu_src2_3 = io_in_0_valid ? io_in_0_bits_alu_src2_3 : _GEN_60; // @[Arbiter.scala 138:26 140:19]
  assign io_out_bits_alu_src3_0 = io_in_0_valid ? io_in_0_bits_alu_src3_0 : _GEN_61; // @[Arbiter.scala 138:26 140:19]
  assign io_out_bits_alu_src3_1 = io_in_0_valid ? io_in_0_bits_alu_src3_1 : _GEN_62; // @[Arbiter.scala 138:26 140:19]
  assign io_out_bits_alu_src3_2 = io_in_0_valid ? io_in_0_bits_alu_src3_2 : _GEN_63; // @[Arbiter.scala 138:26 140:19]
  assign io_out_bits_alu_src3_3 = io_in_0_valid ? io_in_0_bits_alu_src3_3 : _GEN_64; // @[Arbiter.scala 138:26 140:19]
  assign io_out_bits_mask_0 = io_in_0_valid ? io_in_0_bits_mask_0 : _GEN_65; // @[Arbiter.scala 138:26 140:19]
  assign io_out_bits_mask_1 = io_in_0_valid ? io_in_0_bits_mask_1 : _GEN_66; // @[Arbiter.scala 138:26 140:19]
  assign io_out_bits_mask_2 = io_in_0_valid ? io_in_0_bits_mask_2 : _GEN_67; // @[Arbiter.scala 138:26 140:19]
  assign io_out_bits_mask_3 = io_in_0_valid ? io_in_0_bits_mask_3 : _GEN_68; // @[Arbiter.scala 138:26 140:19]
  assign io_out_bits_control_inst = io_in_0_valid ? io_in_0_bits_control_inst : _GEN_69; // @[Arbiter.scala 138:26 140:19]
  assign io_out_bits_control_wid = io_in_0_valid ? io_in_0_bits_control_wid : _GEN_70; // @[Arbiter.scala 138:26 140:19]
  assign io_out_bits_control_fp = io_in_0_valid ? io_in_0_bits_control_fp : _GEN_71; // @[Arbiter.scala 138:26 140:19]
  assign io_out_bits_control_branch = io_in_0_valid ? io_in_0_bits_control_branch : _GEN_72; // @[Arbiter.scala 138:26 140:19]
  assign io_out_bits_control_simt_stack = io_in_0_valid ? io_in_0_bits_control_simt_stack : _GEN_73; // @[Arbiter.scala 138:26 140:19]
  assign io_out_bits_control_simt_stack_op = io_in_0_valid ? io_in_0_bits_control_simt_stack_op : _GEN_74; // @[Arbiter.scala 138:26 140:19]
  assign io_out_bits_control_barrier = io_in_0_valid ? io_in_0_bits_control_barrier : _GEN_75; // @[Arbiter.scala 138:26 140:19]
  assign io_out_bits_control_csr = io_in_0_valid ? io_in_0_bits_control_csr : _GEN_76; // @[Arbiter.scala 138:26 140:19]
  assign io_out_bits_control_reverse = io_in_0_valid ? io_in_0_bits_control_reverse : _GEN_77; // @[Arbiter.scala 138:26 140:19]
  assign io_out_bits_control_sel_alu2 = io_in_0_valid ? io_in_0_bits_control_sel_alu2 : _GEN_78; // @[Arbiter.scala 138:26 140:19]
  assign io_out_bits_control_sel_alu1 = io_in_0_valid ? io_in_0_bits_control_sel_alu1 : _GEN_79; // @[Arbiter.scala 138:26 140:19]
  assign io_out_bits_control_isvec = io_in_0_valid ? io_in_0_bits_control_isvec : _GEN_80; // @[Arbiter.scala 138:26 140:19]
  assign io_out_bits_control_sel_alu3 = io_in_0_valid ? io_in_0_bits_control_sel_alu3 : _GEN_81; // @[Arbiter.scala 138:26 140:19]
  assign io_out_bits_control_mask = io_in_0_valid ? io_in_0_bits_control_mask : _GEN_82; // @[Arbiter.scala 138:26 140:19]
  assign io_out_bits_control_sel_imm = io_in_0_valid ? io_in_0_bits_control_sel_imm : _GEN_83; // @[Arbiter.scala 138:26 140:19]
  assign io_out_bits_control_mem_whb = io_in_0_valid ? io_in_0_bits_control_mem_whb : _GEN_84; // @[Arbiter.scala 138:26 140:19]
  assign io_out_bits_control_mem_unsigned = io_in_0_valid ? io_in_0_bits_control_mem_unsigned : _GEN_85; // @[Arbiter.scala 138:26 140:19]
  assign io_out_bits_control_alu_fn = io_in_0_valid ? io_in_0_bits_control_alu_fn : _GEN_86; // @[Arbiter.scala 138:26 140:19]
  assign io_out_bits_control_mem = io_in_0_valid ? io_in_0_bits_control_mem : _GEN_87; // @[Arbiter.scala 138:26 140:19]
  assign io_out_bits_control_mul = io_in_0_valid ? io_in_0_bits_control_mul : _GEN_88; // @[Arbiter.scala 138:26 140:19]
  assign io_out_bits_control_mem_cmd = io_in_0_valid ? io_in_0_bits_control_mem_cmd : _GEN_89; // @[Arbiter.scala 138:26 140:19]
  assign io_out_bits_control_mop = io_in_0_valid ? io_in_0_bits_control_mop : _GEN_90; // @[Arbiter.scala 138:26 140:19]
  assign io_out_bits_control_reg_idx1 = io_in_0_valid ? io_in_0_bits_control_reg_idx1 : _GEN_91; // @[Arbiter.scala 138:26 140:19]
  assign io_out_bits_control_reg_idx2 = io_in_0_valid ? io_in_0_bits_control_reg_idx2 : _GEN_92; // @[Arbiter.scala 138:26 140:19]
  assign io_out_bits_control_reg_idx3 = io_in_0_valid ? io_in_0_bits_control_reg_idx3 : _GEN_93; // @[Arbiter.scala 138:26 140:19]
  assign io_out_bits_control_reg_idxw = io_in_0_valid ? io_in_0_bits_control_reg_idxw : _GEN_94; // @[Arbiter.scala 138:26 140:19]
  assign io_out_bits_control_wfd = io_in_0_valid ? io_in_0_bits_control_wfd : _GEN_95; // @[Arbiter.scala 138:26 140:19]
  assign io_out_bits_control_fence = io_in_0_valid ? io_in_0_bits_control_fence : _GEN_96; // @[Arbiter.scala 138:26 140:19]
  assign io_out_bits_control_sfu = io_in_0_valid ? io_in_0_bits_control_sfu : _GEN_97; // @[Arbiter.scala 138:26 140:19]
  assign io_out_bits_control_readmask = io_in_0_valid ? io_in_0_bits_control_readmask : _GEN_98; // @[Arbiter.scala 138:26 140:19]
  assign io_out_bits_control_writemask = io_in_0_valid ? io_in_0_bits_control_writemask : _GEN_99; // @[Arbiter.scala 138:26 140:19]
  assign io_out_bits_control_wxd = io_in_0_valid ? io_in_0_bits_control_wxd : _GEN_100; // @[Arbiter.scala 138:26 140:19]
  assign io_out_bits_control_pc = io_in_0_valid ? io_in_0_bits_control_pc : _GEN_101; // @[Arbiter.scala 138:26 140:19]
  assign io_out_bits_control_spike_info_pc = io_in_0_valid ? io_in_0_bits_control_spike_info_pc : _GEN_102; // @[Arbiter.scala 138:26 140:19]
  assign io_out_bits_control_spike_info_inst = io_in_0_valid ? io_in_0_bits_control_spike_info_inst : _GEN_103; // @[Arbiter.scala 138:26 140:19]
endmodule
module operandCollector(
  input         clock,
  input         reset,
  output        io_control_ready,
  input         io_control_valid,
  input  [31:0] io_control_bits_inst,
  input  [1:0]  io_control_bits_wid,
  input         io_control_bits_fp,
  input  [1:0]  io_control_bits_branch,
  input         io_control_bits_simt_stack,
  input         io_control_bits_simt_stack_op,
  input         io_control_bits_barrier,
  input  [1:0]  io_control_bits_csr,
  input         io_control_bits_reverse,
  input  [1:0]  io_control_bits_sel_alu2,
  input  [1:0]  io_control_bits_sel_alu1,
  input         io_control_bits_isvec,
  input  [1:0]  io_control_bits_sel_alu3,
  input         io_control_bits_mask,
  input  [2:0]  io_control_bits_sel_imm,
  input  [1:0]  io_control_bits_mem_whb,
  input         io_control_bits_mem_unsigned,
  input  [5:0]  io_control_bits_alu_fn,
  input         io_control_bits_mem,
  input         io_control_bits_mul,
  input  [1:0]  io_control_bits_mem_cmd,
  input  [1:0]  io_control_bits_mop,
  input  [4:0]  io_control_bits_reg_idx1,
  input  [4:0]  io_control_bits_reg_idx2,
  input  [4:0]  io_control_bits_reg_idx3,
  input  [4:0]  io_control_bits_reg_idxw,
  input         io_control_bits_wfd,
  input         io_control_bits_fence,
  input         io_control_bits_sfu,
  input         io_control_bits_readmask,
  input         io_control_bits_writemask,
  input         io_control_bits_wxd,
  input  [31:0] io_control_bits_pc,
  input  [31:0] io_control_bits_spike_info_pc,
  input  [31:0] io_control_bits_spike_info_inst,
  input         io_out_ready,
  output        io_out_valid,
  output [31:0] io_out_bits_alu_src1_0,
  output [31:0] io_out_bits_alu_src1_1,
  output [31:0] io_out_bits_alu_src1_2,
  output [31:0] io_out_bits_alu_src1_3,
  output [31:0] io_out_bits_alu_src2_0,
  output [31:0] io_out_bits_alu_src2_1,
  output [31:0] io_out_bits_alu_src2_2,
  output [31:0] io_out_bits_alu_src2_3,
  output [31:0] io_out_bits_alu_src3_0,
  output [31:0] io_out_bits_alu_src3_1,
  output [31:0] io_out_bits_alu_src3_2,
  output [31:0] io_out_bits_alu_src3_3,
  output        io_out_bits_mask_0,
  output        io_out_bits_mask_1,
  output        io_out_bits_mask_2,
  output        io_out_bits_mask_3,
  output [31:0] io_out_bits_control_inst,
  output [1:0]  io_out_bits_control_wid,
  output        io_out_bits_control_fp,
  output [1:0]  io_out_bits_control_branch,
  output        io_out_bits_control_simt_stack,
  output        io_out_bits_control_simt_stack_op,
  output        io_out_bits_control_barrier,
  output [1:0]  io_out_bits_control_csr,
  output        io_out_bits_control_reverse,
  output [1:0]  io_out_bits_control_sel_alu2,
  output [1:0]  io_out_bits_control_sel_alu1,
  output        io_out_bits_control_isvec,
  output [1:0]  io_out_bits_control_sel_alu3,
  output        io_out_bits_control_mask,
  output [2:0]  io_out_bits_control_sel_imm,
  output [1:0]  io_out_bits_control_mem_whb,
  output        io_out_bits_control_mem_unsigned,
  output [5:0]  io_out_bits_control_alu_fn,
  output        io_out_bits_control_mem,
  output        io_out_bits_control_mul,
  output [1:0]  io_out_bits_control_mem_cmd,
  output [1:0]  io_out_bits_control_mop,
  output [4:0]  io_out_bits_control_reg_idx1,
  output [4:0]  io_out_bits_control_reg_idx2,
  output [4:0]  io_out_bits_control_reg_idx3,
  output [4:0]  io_out_bits_control_reg_idxw,
  output        io_out_bits_control_wfd,
  output        io_out_bits_control_fence,
  output        io_out_bits_control_sfu,
  output        io_out_bits_control_readmask,
  output        io_out_bits_control_writemask,
  output        io_out_bits_control_wxd,
  output [31:0] io_out_bits_control_pc,
  output [31:0] io_out_bits_control_spike_info_pc,
  output [31:0] io_out_bits_control_spike_info_inst,
  output        io_writeScalarCtrl_ready,
  input         io_writeScalarCtrl_valid,
  input  [31:0] io_writeScalarCtrl_bits_wb_wxd_rd,
  input         io_writeScalarCtrl_bits_wxd,
  input  [4:0]  io_writeScalarCtrl_bits_reg_idxw,
  input  [1:0]  io_writeScalarCtrl_bits_warp_id,
  input  [31:0] io_writeScalarCtrl_bits_spike_info_pc,
  input  [31:0] io_writeScalarCtrl_bits_spike_info_inst,
  output        io_writeVecCtrl_ready,
  input         io_writeVecCtrl_valid,
  input  [31:0] io_writeVecCtrl_bits_wb_wvd_rd_0,
  input  [31:0] io_writeVecCtrl_bits_wb_wvd_rd_1,
  input  [31:0] io_writeVecCtrl_bits_wb_wvd_rd_2,
  input  [31:0] io_writeVecCtrl_bits_wb_wvd_rd_3,
  input         io_writeVecCtrl_bits_wvd_mask_0,
  input         io_writeVecCtrl_bits_wvd_mask_1,
  input         io_writeVecCtrl_bits_wvd_mask_2,
  input         io_writeVecCtrl_bits_wvd_mask_3,
  input         io_writeVecCtrl_bits_wvd,
  input  [4:0]  io_writeVecCtrl_bits_reg_idxw,
  input  [1:0]  io_writeVecCtrl_bits_warp_id,
  input  [31:0] io_writeVecCtrl_bits_spike_info_pc,
  input  [31:0] io_writeVecCtrl_bits_spike_info_inst
);
`ifdef RANDOMIZE_REG_INIT
  reg [31:0] _RAND_0;
  reg [31:0] _RAND_1;
  reg [31:0] _RAND_2;
  reg [31:0] _RAND_3;
  reg [31:0] _RAND_4;
  reg [31:0] _RAND_5;
  reg [31:0] _RAND_6;
  reg [31:0] _RAND_7;
  reg [31:0] _RAND_8;
  reg [31:0] _RAND_9;
  reg [31:0] _RAND_10;
  reg [31:0] _RAND_11;
  reg [31:0] _RAND_12;
  reg [31:0] _RAND_13;
  reg [31:0] _RAND_14;
  reg [31:0] _RAND_15;
`endif // RANDOMIZE_REG_INIT
  wire  collectorUnit_clock; // @[operandCollector.scala 388:66]
  wire  collectorUnit_reset; // @[operandCollector.scala 388:66]
  wire  collectorUnit_io_control_ready; // @[operandCollector.scala 388:66]
  wire  collectorUnit_io_control_valid; // @[operandCollector.scala 388:66]
  wire [31:0] collectorUnit_io_control_bits_inst; // @[operandCollector.scala 388:66]
  wire [1:0] collectorUnit_io_control_bits_wid; // @[operandCollector.scala 388:66]
  wire  collectorUnit_io_control_bits_fp; // @[operandCollector.scala 388:66]
  wire [1:0] collectorUnit_io_control_bits_branch; // @[operandCollector.scala 388:66]
  wire  collectorUnit_io_control_bits_simt_stack; // @[operandCollector.scala 388:66]
  wire  collectorUnit_io_control_bits_simt_stack_op; // @[operandCollector.scala 388:66]
  wire  collectorUnit_io_control_bits_barrier; // @[operandCollector.scala 388:66]
  wire [1:0] collectorUnit_io_control_bits_csr; // @[operandCollector.scala 388:66]
  wire  collectorUnit_io_control_bits_reverse; // @[operandCollector.scala 388:66]
  wire [1:0] collectorUnit_io_control_bits_sel_alu2; // @[operandCollector.scala 388:66]
  wire [1:0] collectorUnit_io_control_bits_sel_alu1; // @[operandCollector.scala 388:66]
  wire  collectorUnit_io_control_bits_isvec; // @[operandCollector.scala 388:66]
  wire [1:0] collectorUnit_io_control_bits_sel_alu3; // @[operandCollector.scala 388:66]
  wire  collectorUnit_io_control_bits_mask; // @[operandCollector.scala 388:66]
  wire [2:0] collectorUnit_io_control_bits_sel_imm; // @[operandCollector.scala 388:66]
  wire [1:0] collectorUnit_io_control_bits_mem_whb; // @[operandCollector.scala 388:66]
  wire  collectorUnit_io_control_bits_mem_unsigned; // @[operandCollector.scala 388:66]
  wire [5:0] collectorUnit_io_control_bits_alu_fn; // @[operandCollector.scala 388:66]
  wire  collectorUnit_io_control_bits_mem; // @[operandCollector.scala 388:66]
  wire  collectorUnit_io_control_bits_mul; // @[operandCollector.scala 388:66]
  wire [1:0] collectorUnit_io_control_bits_mem_cmd; // @[operandCollector.scala 388:66]
  wire [1:0] collectorUnit_io_control_bits_mop; // @[operandCollector.scala 388:66]
  wire [4:0] collectorUnit_io_control_bits_reg_idx1; // @[operandCollector.scala 388:66]
  wire [4:0] collectorUnit_io_control_bits_reg_idx2; // @[operandCollector.scala 388:66]
  wire [4:0] collectorUnit_io_control_bits_reg_idx3; // @[operandCollector.scala 388:66]
  wire [4:0] collectorUnit_io_control_bits_reg_idxw; // @[operandCollector.scala 388:66]
  wire  collectorUnit_io_control_bits_wfd; // @[operandCollector.scala 388:66]
  wire  collectorUnit_io_control_bits_fence; // @[operandCollector.scala 388:66]
  wire  collectorUnit_io_control_bits_sfu; // @[operandCollector.scala 388:66]
  wire  collectorUnit_io_control_bits_readmask; // @[operandCollector.scala 388:66]
  wire  collectorUnit_io_control_bits_writemask; // @[operandCollector.scala 388:66]
  wire  collectorUnit_io_control_bits_wxd; // @[operandCollector.scala 388:66]
  wire [31:0] collectorUnit_io_control_bits_pc; // @[operandCollector.scala 388:66]
  wire [31:0] collectorUnit_io_control_bits_spike_info_pc; // @[operandCollector.scala 388:66]
  wire [31:0] collectorUnit_io_control_bits_spike_info_inst; // @[operandCollector.scala 388:66]
  wire  collectorUnit_io_bankIn_0_ready; // @[operandCollector.scala 388:66]
  wire  collectorUnit_io_bankIn_0_valid; // @[operandCollector.scala 388:66]
  wire [1:0] collectorUnit_io_bankIn_0_bits_regOrder; // @[operandCollector.scala 388:66]
  wire [31:0] collectorUnit_io_bankIn_0_bits_data_0; // @[operandCollector.scala 388:66]
  wire [31:0] collectorUnit_io_bankIn_0_bits_data_1; // @[operandCollector.scala 388:66]
  wire [31:0] collectorUnit_io_bankIn_0_bits_data_2; // @[operandCollector.scala 388:66]
  wire [31:0] collectorUnit_io_bankIn_0_bits_data_3; // @[operandCollector.scala 388:66]
  wire [31:0] collectorUnit_io_bankIn_0_bits_v0_0; // @[operandCollector.scala 388:66]
  wire  collectorUnit_io_bankIn_1_ready; // @[operandCollector.scala 388:66]
  wire  collectorUnit_io_bankIn_1_valid; // @[operandCollector.scala 388:66]
  wire [1:0] collectorUnit_io_bankIn_1_bits_regOrder; // @[operandCollector.scala 388:66]
  wire [31:0] collectorUnit_io_bankIn_1_bits_data_0; // @[operandCollector.scala 388:66]
  wire [31:0] collectorUnit_io_bankIn_1_bits_data_1; // @[operandCollector.scala 388:66]
  wire [31:0] collectorUnit_io_bankIn_1_bits_data_2; // @[operandCollector.scala 388:66]
  wire [31:0] collectorUnit_io_bankIn_1_bits_data_3; // @[operandCollector.scala 388:66]
  wire [31:0] collectorUnit_io_bankIn_1_bits_v0_0; // @[operandCollector.scala 388:66]
  wire  collectorUnit_io_bankIn_2_ready; // @[operandCollector.scala 388:66]
  wire  collectorUnit_io_bankIn_2_valid; // @[operandCollector.scala 388:66]
  wire [1:0] collectorUnit_io_bankIn_2_bits_regOrder; // @[operandCollector.scala 388:66]
  wire [31:0] collectorUnit_io_bankIn_2_bits_data_0; // @[operandCollector.scala 388:66]
  wire [31:0] collectorUnit_io_bankIn_2_bits_data_1; // @[operandCollector.scala 388:66]
  wire [31:0] collectorUnit_io_bankIn_2_bits_data_2; // @[operandCollector.scala 388:66]
  wire [31:0] collectorUnit_io_bankIn_2_bits_data_3; // @[operandCollector.scala 388:66]
  wire [31:0] collectorUnit_io_bankIn_2_bits_v0_0; // @[operandCollector.scala 388:66]
  wire  collectorUnit_io_bankIn_3_ready; // @[operandCollector.scala 388:66]
  wire  collectorUnit_io_bankIn_3_valid; // @[operandCollector.scala 388:66]
  wire [1:0] collectorUnit_io_bankIn_3_bits_regOrder; // @[operandCollector.scala 388:66]
  wire [31:0] collectorUnit_io_bankIn_3_bits_data_0; // @[operandCollector.scala 388:66]
  wire [31:0] collectorUnit_io_bankIn_3_bits_data_1; // @[operandCollector.scala 388:66]
  wire [31:0] collectorUnit_io_bankIn_3_bits_data_2; // @[operandCollector.scala 388:66]
  wire [31:0] collectorUnit_io_bankIn_3_bits_data_3; // @[operandCollector.scala 388:66]
  wire [31:0] collectorUnit_io_bankIn_3_bits_v0_0; // @[operandCollector.scala 388:66]
  wire  collectorUnit_io_issue_ready; // @[operandCollector.scala 388:66]
  wire  collectorUnit_io_issue_valid; // @[operandCollector.scala 388:66]
  wire [31:0] collectorUnit_io_issue_bits_alu_src1_0; // @[operandCollector.scala 388:66]
  wire [31:0] collectorUnit_io_issue_bits_alu_src1_1; // @[operandCollector.scala 388:66]
  wire [31:0] collectorUnit_io_issue_bits_alu_src1_2; // @[operandCollector.scala 388:66]
  wire [31:0] collectorUnit_io_issue_bits_alu_src1_3; // @[operandCollector.scala 388:66]
  wire [31:0] collectorUnit_io_issue_bits_alu_src2_0; // @[operandCollector.scala 388:66]
  wire [31:0] collectorUnit_io_issue_bits_alu_src2_1; // @[operandCollector.scala 388:66]
  wire [31:0] collectorUnit_io_issue_bits_alu_src2_2; // @[operandCollector.scala 388:66]
  wire [31:0] collectorUnit_io_issue_bits_alu_src2_3; // @[operandCollector.scala 388:66]
  wire [31:0] collectorUnit_io_issue_bits_alu_src3_0; // @[operandCollector.scala 388:66]
  wire [31:0] collectorUnit_io_issue_bits_alu_src3_1; // @[operandCollector.scala 388:66]
  wire [31:0] collectorUnit_io_issue_bits_alu_src3_2; // @[operandCollector.scala 388:66]
  wire [31:0] collectorUnit_io_issue_bits_alu_src3_3; // @[operandCollector.scala 388:66]
  wire  collectorUnit_io_issue_bits_mask_0; // @[operandCollector.scala 388:66]
  wire  collectorUnit_io_issue_bits_mask_1; // @[operandCollector.scala 388:66]
  wire  collectorUnit_io_issue_bits_mask_2; // @[operandCollector.scala 388:66]
  wire  collectorUnit_io_issue_bits_mask_3; // @[operandCollector.scala 388:66]
  wire [31:0] collectorUnit_io_issue_bits_control_inst; // @[operandCollector.scala 388:66]
  wire [1:0] collectorUnit_io_issue_bits_control_wid; // @[operandCollector.scala 388:66]
  wire  collectorUnit_io_issue_bits_control_fp; // @[operandCollector.scala 388:66]
  wire [1:0] collectorUnit_io_issue_bits_control_branch; // @[operandCollector.scala 388:66]
  wire  collectorUnit_io_issue_bits_control_simt_stack; // @[operandCollector.scala 388:66]
  wire  collectorUnit_io_issue_bits_control_simt_stack_op; // @[operandCollector.scala 388:66]
  wire  collectorUnit_io_issue_bits_control_barrier; // @[operandCollector.scala 388:66]
  wire [1:0] collectorUnit_io_issue_bits_control_csr; // @[operandCollector.scala 388:66]
  wire  collectorUnit_io_issue_bits_control_reverse; // @[operandCollector.scala 388:66]
  wire [1:0] collectorUnit_io_issue_bits_control_sel_alu2; // @[operandCollector.scala 388:66]
  wire [1:0] collectorUnit_io_issue_bits_control_sel_alu1; // @[operandCollector.scala 388:66]
  wire  collectorUnit_io_issue_bits_control_isvec; // @[operandCollector.scala 388:66]
  wire [1:0] collectorUnit_io_issue_bits_control_sel_alu3; // @[operandCollector.scala 388:66]
  wire  collectorUnit_io_issue_bits_control_mask; // @[operandCollector.scala 388:66]
  wire [2:0] collectorUnit_io_issue_bits_control_sel_imm; // @[operandCollector.scala 388:66]
  wire [1:0] collectorUnit_io_issue_bits_control_mem_whb; // @[operandCollector.scala 388:66]
  wire  collectorUnit_io_issue_bits_control_mem_unsigned; // @[operandCollector.scala 388:66]
  wire [5:0] collectorUnit_io_issue_bits_control_alu_fn; // @[operandCollector.scala 388:66]
  wire  collectorUnit_io_issue_bits_control_mem; // @[operandCollector.scala 388:66]
  wire  collectorUnit_io_issue_bits_control_mul; // @[operandCollector.scala 388:66]
  wire [1:0] collectorUnit_io_issue_bits_control_mem_cmd; // @[operandCollector.scala 388:66]
  wire [1:0] collectorUnit_io_issue_bits_control_mop; // @[operandCollector.scala 388:66]
  wire [4:0] collectorUnit_io_issue_bits_control_reg_idx1; // @[operandCollector.scala 388:66]
  wire [4:0] collectorUnit_io_issue_bits_control_reg_idx2; // @[operandCollector.scala 388:66]
  wire [4:0] collectorUnit_io_issue_bits_control_reg_idx3; // @[operandCollector.scala 388:66]
  wire [4:0] collectorUnit_io_issue_bits_control_reg_idxw; // @[operandCollector.scala 388:66]
  wire  collectorUnit_io_issue_bits_control_wfd; // @[operandCollector.scala 388:66]
  wire  collectorUnit_io_issue_bits_control_fence; // @[operandCollector.scala 388:66]
  wire  collectorUnit_io_issue_bits_control_sfu; // @[operandCollector.scala 388:66]
  wire  collectorUnit_io_issue_bits_control_readmask; // @[operandCollector.scala 388:66]
  wire  collectorUnit_io_issue_bits_control_writemask; // @[operandCollector.scala 388:66]
  wire  collectorUnit_io_issue_bits_control_wxd; // @[operandCollector.scala 388:66]
  wire [31:0] collectorUnit_io_issue_bits_control_pc; // @[operandCollector.scala 388:66]
  wire [31:0] collectorUnit_io_issue_bits_control_spike_info_pc; // @[operandCollector.scala 388:66]
  wire [31:0] collectorUnit_io_issue_bits_control_spike_info_inst; // @[operandCollector.scala 388:66]
  wire  collectorUnit_io_outArbiterIO_0_valid; // @[operandCollector.scala 388:66]
  wire [4:0] collectorUnit_io_outArbiterIO_0_bits_rsAddr; // @[operandCollector.scala 388:66]
  wire [1:0] collectorUnit_io_outArbiterIO_0_bits_bankID; // @[operandCollector.scala 388:66]
  wire [1:0] collectorUnit_io_outArbiterIO_0_bits_rsType; // @[operandCollector.scala 388:66]
  wire  collectorUnit_io_outArbiterIO_1_valid; // @[operandCollector.scala 388:66]
  wire [4:0] collectorUnit_io_outArbiterIO_1_bits_rsAddr; // @[operandCollector.scala 388:66]
  wire [1:0] collectorUnit_io_outArbiterIO_1_bits_bankID; // @[operandCollector.scala 388:66]
  wire [1:0] collectorUnit_io_outArbiterIO_1_bits_rsType; // @[operandCollector.scala 388:66]
  wire  collectorUnit_io_outArbiterIO_2_valid; // @[operandCollector.scala 388:66]
  wire [4:0] collectorUnit_io_outArbiterIO_2_bits_rsAddr; // @[operandCollector.scala 388:66]
  wire [1:0] collectorUnit_io_outArbiterIO_2_bits_bankID; // @[operandCollector.scala 388:66]
  wire [1:0] collectorUnit_io_outArbiterIO_2_bits_rsType; // @[operandCollector.scala 388:66]
  wire  collectorUnit_io_outArbiterIO_3_valid; // @[operandCollector.scala 388:66]
  wire [4:0] collectorUnit_io_outArbiterIO_3_bits_rsAddr; // @[operandCollector.scala 388:66]
  wire [1:0] collectorUnit_io_outArbiterIO_3_bits_bankID; // @[operandCollector.scala 388:66]
  wire  collectorUnit_1_clock; // @[operandCollector.scala 388:66]
  wire  collectorUnit_1_reset; // @[operandCollector.scala 388:66]
  wire  collectorUnit_1_io_control_ready; // @[operandCollector.scala 388:66]
  wire  collectorUnit_1_io_control_valid; // @[operandCollector.scala 388:66]
  wire [31:0] collectorUnit_1_io_control_bits_inst; // @[operandCollector.scala 388:66]
  wire [1:0] collectorUnit_1_io_control_bits_wid; // @[operandCollector.scala 388:66]
  wire  collectorUnit_1_io_control_bits_fp; // @[operandCollector.scala 388:66]
  wire [1:0] collectorUnit_1_io_control_bits_branch; // @[operandCollector.scala 388:66]
  wire  collectorUnit_1_io_control_bits_simt_stack; // @[operandCollector.scala 388:66]
  wire  collectorUnit_1_io_control_bits_simt_stack_op; // @[operandCollector.scala 388:66]
  wire  collectorUnit_1_io_control_bits_barrier; // @[operandCollector.scala 388:66]
  wire [1:0] collectorUnit_1_io_control_bits_csr; // @[operandCollector.scala 388:66]
  wire  collectorUnit_1_io_control_bits_reverse; // @[operandCollector.scala 388:66]
  wire [1:0] collectorUnit_1_io_control_bits_sel_alu2; // @[operandCollector.scala 388:66]
  wire [1:0] collectorUnit_1_io_control_bits_sel_alu1; // @[operandCollector.scala 388:66]
  wire  collectorUnit_1_io_control_bits_isvec; // @[operandCollector.scala 388:66]
  wire [1:0] collectorUnit_1_io_control_bits_sel_alu3; // @[operandCollector.scala 388:66]
  wire  collectorUnit_1_io_control_bits_mask; // @[operandCollector.scala 388:66]
  wire [2:0] collectorUnit_1_io_control_bits_sel_imm; // @[operandCollector.scala 388:66]
  wire [1:0] collectorUnit_1_io_control_bits_mem_whb; // @[operandCollector.scala 388:66]
  wire  collectorUnit_1_io_control_bits_mem_unsigned; // @[operandCollector.scala 388:66]
  wire [5:0] collectorUnit_1_io_control_bits_alu_fn; // @[operandCollector.scala 388:66]
  wire  collectorUnit_1_io_control_bits_mem; // @[operandCollector.scala 388:66]
  wire  collectorUnit_1_io_control_bits_mul; // @[operandCollector.scala 388:66]
  wire [1:0] collectorUnit_1_io_control_bits_mem_cmd; // @[operandCollector.scala 388:66]
  wire [1:0] collectorUnit_1_io_control_bits_mop; // @[operandCollector.scala 388:66]
  wire [4:0] collectorUnit_1_io_control_bits_reg_idx1; // @[operandCollector.scala 388:66]
  wire [4:0] collectorUnit_1_io_control_bits_reg_idx2; // @[operandCollector.scala 388:66]
  wire [4:0] collectorUnit_1_io_control_bits_reg_idx3; // @[operandCollector.scala 388:66]
  wire [4:0] collectorUnit_1_io_control_bits_reg_idxw; // @[operandCollector.scala 388:66]
  wire  collectorUnit_1_io_control_bits_wfd; // @[operandCollector.scala 388:66]
  wire  collectorUnit_1_io_control_bits_fence; // @[operandCollector.scala 388:66]
  wire  collectorUnit_1_io_control_bits_sfu; // @[operandCollector.scala 388:66]
  wire  collectorUnit_1_io_control_bits_readmask; // @[operandCollector.scala 388:66]
  wire  collectorUnit_1_io_control_bits_writemask; // @[operandCollector.scala 388:66]
  wire  collectorUnit_1_io_control_bits_wxd; // @[operandCollector.scala 388:66]
  wire [31:0] collectorUnit_1_io_control_bits_pc; // @[operandCollector.scala 388:66]
  wire [31:0] collectorUnit_1_io_control_bits_spike_info_pc; // @[operandCollector.scala 388:66]
  wire [31:0] collectorUnit_1_io_control_bits_spike_info_inst; // @[operandCollector.scala 388:66]
  wire  collectorUnit_1_io_bankIn_0_ready; // @[operandCollector.scala 388:66]
  wire  collectorUnit_1_io_bankIn_0_valid; // @[operandCollector.scala 388:66]
  wire [1:0] collectorUnit_1_io_bankIn_0_bits_regOrder; // @[operandCollector.scala 388:66]
  wire [31:0] collectorUnit_1_io_bankIn_0_bits_data_0; // @[operandCollector.scala 388:66]
  wire [31:0] collectorUnit_1_io_bankIn_0_bits_data_1; // @[operandCollector.scala 388:66]
  wire [31:0] collectorUnit_1_io_bankIn_0_bits_data_2; // @[operandCollector.scala 388:66]
  wire [31:0] collectorUnit_1_io_bankIn_0_bits_data_3; // @[operandCollector.scala 388:66]
  wire [31:0] collectorUnit_1_io_bankIn_0_bits_v0_0; // @[operandCollector.scala 388:66]
  wire  collectorUnit_1_io_bankIn_1_ready; // @[operandCollector.scala 388:66]
  wire  collectorUnit_1_io_bankIn_1_valid; // @[operandCollector.scala 388:66]
  wire [1:0] collectorUnit_1_io_bankIn_1_bits_regOrder; // @[operandCollector.scala 388:66]
  wire [31:0] collectorUnit_1_io_bankIn_1_bits_data_0; // @[operandCollector.scala 388:66]
  wire [31:0] collectorUnit_1_io_bankIn_1_bits_data_1; // @[operandCollector.scala 388:66]
  wire [31:0] collectorUnit_1_io_bankIn_1_bits_data_2; // @[operandCollector.scala 388:66]
  wire [31:0] collectorUnit_1_io_bankIn_1_bits_data_3; // @[operandCollector.scala 388:66]
  wire [31:0] collectorUnit_1_io_bankIn_1_bits_v0_0; // @[operandCollector.scala 388:66]
  wire  collectorUnit_1_io_bankIn_2_ready; // @[operandCollector.scala 388:66]
  wire  collectorUnit_1_io_bankIn_2_valid; // @[operandCollector.scala 388:66]
  wire [1:0] collectorUnit_1_io_bankIn_2_bits_regOrder; // @[operandCollector.scala 388:66]
  wire [31:0] collectorUnit_1_io_bankIn_2_bits_data_0; // @[operandCollector.scala 388:66]
  wire [31:0] collectorUnit_1_io_bankIn_2_bits_data_1; // @[operandCollector.scala 388:66]
  wire [31:0] collectorUnit_1_io_bankIn_2_bits_data_2; // @[operandCollector.scala 388:66]
  wire [31:0] collectorUnit_1_io_bankIn_2_bits_data_3; // @[operandCollector.scala 388:66]
  wire [31:0] collectorUnit_1_io_bankIn_2_bits_v0_0; // @[operandCollector.scala 388:66]
  wire  collectorUnit_1_io_bankIn_3_ready; // @[operandCollector.scala 388:66]
  wire  collectorUnit_1_io_bankIn_3_valid; // @[operandCollector.scala 388:66]
  wire [1:0] collectorUnit_1_io_bankIn_3_bits_regOrder; // @[operandCollector.scala 388:66]
  wire [31:0] collectorUnit_1_io_bankIn_3_bits_data_0; // @[operandCollector.scala 388:66]
  wire [31:0] collectorUnit_1_io_bankIn_3_bits_data_1; // @[operandCollector.scala 388:66]
  wire [31:0] collectorUnit_1_io_bankIn_3_bits_data_2; // @[operandCollector.scala 388:66]
  wire [31:0] collectorUnit_1_io_bankIn_3_bits_data_3; // @[operandCollector.scala 388:66]
  wire [31:0] collectorUnit_1_io_bankIn_3_bits_v0_0; // @[operandCollector.scala 388:66]
  wire  collectorUnit_1_io_issue_ready; // @[operandCollector.scala 388:66]
  wire  collectorUnit_1_io_issue_valid; // @[operandCollector.scala 388:66]
  wire [31:0] collectorUnit_1_io_issue_bits_alu_src1_0; // @[operandCollector.scala 388:66]
  wire [31:0] collectorUnit_1_io_issue_bits_alu_src1_1; // @[operandCollector.scala 388:66]
  wire [31:0] collectorUnit_1_io_issue_bits_alu_src1_2; // @[operandCollector.scala 388:66]
  wire [31:0] collectorUnit_1_io_issue_bits_alu_src1_3; // @[operandCollector.scala 388:66]
  wire [31:0] collectorUnit_1_io_issue_bits_alu_src2_0; // @[operandCollector.scala 388:66]
  wire [31:0] collectorUnit_1_io_issue_bits_alu_src2_1; // @[operandCollector.scala 388:66]
  wire [31:0] collectorUnit_1_io_issue_bits_alu_src2_2; // @[operandCollector.scala 388:66]
  wire [31:0] collectorUnit_1_io_issue_bits_alu_src2_3; // @[operandCollector.scala 388:66]
  wire [31:0] collectorUnit_1_io_issue_bits_alu_src3_0; // @[operandCollector.scala 388:66]
  wire [31:0] collectorUnit_1_io_issue_bits_alu_src3_1; // @[operandCollector.scala 388:66]
  wire [31:0] collectorUnit_1_io_issue_bits_alu_src3_2; // @[operandCollector.scala 388:66]
  wire [31:0] collectorUnit_1_io_issue_bits_alu_src3_3; // @[operandCollector.scala 388:66]
  wire  collectorUnit_1_io_issue_bits_mask_0; // @[operandCollector.scala 388:66]
  wire  collectorUnit_1_io_issue_bits_mask_1; // @[operandCollector.scala 388:66]
  wire  collectorUnit_1_io_issue_bits_mask_2; // @[operandCollector.scala 388:66]
  wire  collectorUnit_1_io_issue_bits_mask_3; // @[operandCollector.scala 388:66]
  wire [31:0] collectorUnit_1_io_issue_bits_control_inst; // @[operandCollector.scala 388:66]
  wire [1:0] collectorUnit_1_io_issue_bits_control_wid; // @[operandCollector.scala 388:66]
  wire  collectorUnit_1_io_issue_bits_control_fp; // @[operandCollector.scala 388:66]
  wire [1:0] collectorUnit_1_io_issue_bits_control_branch; // @[operandCollector.scala 388:66]
  wire  collectorUnit_1_io_issue_bits_control_simt_stack; // @[operandCollector.scala 388:66]
  wire  collectorUnit_1_io_issue_bits_control_simt_stack_op; // @[operandCollector.scala 388:66]
  wire  collectorUnit_1_io_issue_bits_control_barrier; // @[operandCollector.scala 388:66]
  wire [1:0] collectorUnit_1_io_issue_bits_control_csr; // @[operandCollector.scala 388:66]
  wire  collectorUnit_1_io_issue_bits_control_reverse; // @[operandCollector.scala 388:66]
  wire [1:0] collectorUnit_1_io_issue_bits_control_sel_alu2; // @[operandCollector.scala 388:66]
  wire [1:0] collectorUnit_1_io_issue_bits_control_sel_alu1; // @[operandCollector.scala 388:66]
  wire  collectorUnit_1_io_issue_bits_control_isvec; // @[operandCollector.scala 388:66]
  wire [1:0] collectorUnit_1_io_issue_bits_control_sel_alu3; // @[operandCollector.scala 388:66]
  wire  collectorUnit_1_io_issue_bits_control_mask; // @[operandCollector.scala 388:66]
  wire [2:0] collectorUnit_1_io_issue_bits_control_sel_imm; // @[operandCollector.scala 388:66]
  wire [1:0] collectorUnit_1_io_issue_bits_control_mem_whb; // @[operandCollector.scala 388:66]
  wire  collectorUnit_1_io_issue_bits_control_mem_unsigned; // @[operandCollector.scala 388:66]
  wire [5:0] collectorUnit_1_io_issue_bits_control_alu_fn; // @[operandCollector.scala 388:66]
  wire  collectorUnit_1_io_issue_bits_control_mem; // @[operandCollector.scala 388:66]
  wire  collectorUnit_1_io_issue_bits_control_mul; // @[operandCollector.scala 388:66]
  wire [1:0] collectorUnit_1_io_issue_bits_control_mem_cmd; // @[operandCollector.scala 388:66]
  wire [1:0] collectorUnit_1_io_issue_bits_control_mop; // @[operandCollector.scala 388:66]
  wire [4:0] collectorUnit_1_io_issue_bits_control_reg_idx1; // @[operandCollector.scala 388:66]
  wire [4:0] collectorUnit_1_io_issue_bits_control_reg_idx2; // @[operandCollector.scala 388:66]
  wire [4:0] collectorUnit_1_io_issue_bits_control_reg_idx3; // @[operandCollector.scala 388:66]
  wire [4:0] collectorUnit_1_io_issue_bits_control_reg_idxw; // @[operandCollector.scala 388:66]
  wire  collectorUnit_1_io_issue_bits_control_wfd; // @[operandCollector.scala 388:66]
  wire  collectorUnit_1_io_issue_bits_control_fence; // @[operandCollector.scala 388:66]
  wire  collectorUnit_1_io_issue_bits_control_sfu; // @[operandCollector.scala 388:66]
  wire  collectorUnit_1_io_issue_bits_control_readmask; // @[operandCollector.scala 388:66]
  wire  collectorUnit_1_io_issue_bits_control_writemask; // @[operandCollector.scala 388:66]
  wire  collectorUnit_1_io_issue_bits_control_wxd; // @[operandCollector.scala 388:66]
  wire [31:0] collectorUnit_1_io_issue_bits_control_pc; // @[operandCollector.scala 388:66]
  wire [31:0] collectorUnit_1_io_issue_bits_control_spike_info_pc; // @[operandCollector.scala 388:66]
  wire [31:0] collectorUnit_1_io_issue_bits_control_spike_info_inst; // @[operandCollector.scala 388:66]
  wire  collectorUnit_1_io_outArbiterIO_0_valid; // @[operandCollector.scala 388:66]
  wire [4:0] collectorUnit_1_io_outArbiterIO_0_bits_rsAddr; // @[operandCollector.scala 388:66]
  wire [1:0] collectorUnit_1_io_outArbiterIO_0_bits_bankID; // @[operandCollector.scala 388:66]
  wire [1:0] collectorUnit_1_io_outArbiterIO_0_bits_rsType; // @[operandCollector.scala 388:66]
  wire  collectorUnit_1_io_outArbiterIO_1_valid; // @[operandCollector.scala 388:66]
  wire [4:0] collectorUnit_1_io_outArbiterIO_1_bits_rsAddr; // @[operandCollector.scala 388:66]
  wire [1:0] collectorUnit_1_io_outArbiterIO_1_bits_bankID; // @[operandCollector.scala 388:66]
  wire [1:0] collectorUnit_1_io_outArbiterIO_1_bits_rsType; // @[operandCollector.scala 388:66]
  wire  collectorUnit_1_io_outArbiterIO_2_valid; // @[operandCollector.scala 388:66]
  wire [4:0] collectorUnit_1_io_outArbiterIO_2_bits_rsAddr; // @[operandCollector.scala 388:66]
  wire [1:0] collectorUnit_1_io_outArbiterIO_2_bits_bankID; // @[operandCollector.scala 388:66]
  wire [1:0] collectorUnit_1_io_outArbiterIO_2_bits_rsType; // @[operandCollector.scala 388:66]
  wire  collectorUnit_1_io_outArbiterIO_3_valid; // @[operandCollector.scala 388:66]
  wire [4:0] collectorUnit_1_io_outArbiterIO_3_bits_rsAddr; // @[operandCollector.scala 388:66]
  wire [1:0] collectorUnit_1_io_outArbiterIO_3_bits_bankID; // @[operandCollector.scala 388:66]
  wire  collectorUnit_2_clock; // @[operandCollector.scala 388:66]
  wire  collectorUnit_2_reset; // @[operandCollector.scala 388:66]
  wire  collectorUnit_2_io_control_ready; // @[operandCollector.scala 388:66]
  wire  collectorUnit_2_io_control_valid; // @[operandCollector.scala 388:66]
  wire [31:0] collectorUnit_2_io_control_bits_inst; // @[operandCollector.scala 388:66]
  wire [1:0] collectorUnit_2_io_control_bits_wid; // @[operandCollector.scala 388:66]
  wire  collectorUnit_2_io_control_bits_fp; // @[operandCollector.scala 388:66]
  wire [1:0] collectorUnit_2_io_control_bits_branch; // @[operandCollector.scala 388:66]
  wire  collectorUnit_2_io_control_bits_simt_stack; // @[operandCollector.scala 388:66]
  wire  collectorUnit_2_io_control_bits_simt_stack_op; // @[operandCollector.scala 388:66]
  wire  collectorUnit_2_io_control_bits_barrier; // @[operandCollector.scala 388:66]
  wire [1:0] collectorUnit_2_io_control_bits_csr; // @[operandCollector.scala 388:66]
  wire  collectorUnit_2_io_control_bits_reverse; // @[operandCollector.scala 388:66]
  wire [1:0] collectorUnit_2_io_control_bits_sel_alu2; // @[operandCollector.scala 388:66]
  wire [1:0] collectorUnit_2_io_control_bits_sel_alu1; // @[operandCollector.scala 388:66]
  wire  collectorUnit_2_io_control_bits_isvec; // @[operandCollector.scala 388:66]
  wire [1:0] collectorUnit_2_io_control_bits_sel_alu3; // @[operandCollector.scala 388:66]
  wire  collectorUnit_2_io_control_bits_mask; // @[operandCollector.scala 388:66]
  wire [2:0] collectorUnit_2_io_control_bits_sel_imm; // @[operandCollector.scala 388:66]
  wire [1:0] collectorUnit_2_io_control_bits_mem_whb; // @[operandCollector.scala 388:66]
  wire  collectorUnit_2_io_control_bits_mem_unsigned; // @[operandCollector.scala 388:66]
  wire [5:0] collectorUnit_2_io_control_bits_alu_fn; // @[operandCollector.scala 388:66]
  wire  collectorUnit_2_io_control_bits_mem; // @[operandCollector.scala 388:66]
  wire  collectorUnit_2_io_control_bits_mul; // @[operandCollector.scala 388:66]
  wire [1:0] collectorUnit_2_io_control_bits_mem_cmd; // @[operandCollector.scala 388:66]
  wire [1:0] collectorUnit_2_io_control_bits_mop; // @[operandCollector.scala 388:66]
  wire [4:0] collectorUnit_2_io_control_bits_reg_idx1; // @[operandCollector.scala 388:66]
  wire [4:0] collectorUnit_2_io_control_bits_reg_idx2; // @[operandCollector.scala 388:66]
  wire [4:0] collectorUnit_2_io_control_bits_reg_idx3; // @[operandCollector.scala 388:66]
  wire [4:0] collectorUnit_2_io_control_bits_reg_idxw; // @[operandCollector.scala 388:66]
  wire  collectorUnit_2_io_control_bits_wfd; // @[operandCollector.scala 388:66]
  wire  collectorUnit_2_io_control_bits_fence; // @[operandCollector.scala 388:66]
  wire  collectorUnit_2_io_control_bits_sfu; // @[operandCollector.scala 388:66]
  wire  collectorUnit_2_io_control_bits_readmask; // @[operandCollector.scala 388:66]
  wire  collectorUnit_2_io_control_bits_writemask; // @[operandCollector.scala 388:66]
  wire  collectorUnit_2_io_control_bits_wxd; // @[operandCollector.scala 388:66]
  wire [31:0] collectorUnit_2_io_control_bits_pc; // @[operandCollector.scala 388:66]
  wire [31:0] collectorUnit_2_io_control_bits_spike_info_pc; // @[operandCollector.scala 388:66]
  wire [31:0] collectorUnit_2_io_control_bits_spike_info_inst; // @[operandCollector.scala 388:66]
  wire  collectorUnit_2_io_bankIn_0_ready; // @[operandCollector.scala 388:66]
  wire  collectorUnit_2_io_bankIn_0_valid; // @[operandCollector.scala 388:66]
  wire [1:0] collectorUnit_2_io_bankIn_0_bits_regOrder; // @[operandCollector.scala 388:66]
  wire [31:0] collectorUnit_2_io_bankIn_0_bits_data_0; // @[operandCollector.scala 388:66]
  wire [31:0] collectorUnit_2_io_bankIn_0_bits_data_1; // @[operandCollector.scala 388:66]
  wire [31:0] collectorUnit_2_io_bankIn_0_bits_data_2; // @[operandCollector.scala 388:66]
  wire [31:0] collectorUnit_2_io_bankIn_0_bits_data_3; // @[operandCollector.scala 388:66]
  wire [31:0] collectorUnit_2_io_bankIn_0_bits_v0_0; // @[operandCollector.scala 388:66]
  wire  collectorUnit_2_io_bankIn_1_ready; // @[operandCollector.scala 388:66]
  wire  collectorUnit_2_io_bankIn_1_valid; // @[operandCollector.scala 388:66]
  wire [1:0] collectorUnit_2_io_bankIn_1_bits_regOrder; // @[operandCollector.scala 388:66]
  wire [31:0] collectorUnit_2_io_bankIn_1_bits_data_0; // @[operandCollector.scala 388:66]
  wire [31:0] collectorUnit_2_io_bankIn_1_bits_data_1; // @[operandCollector.scala 388:66]
  wire [31:0] collectorUnit_2_io_bankIn_1_bits_data_2; // @[operandCollector.scala 388:66]
  wire [31:0] collectorUnit_2_io_bankIn_1_bits_data_3; // @[operandCollector.scala 388:66]
  wire [31:0] collectorUnit_2_io_bankIn_1_bits_v0_0; // @[operandCollector.scala 388:66]
  wire  collectorUnit_2_io_bankIn_2_ready; // @[operandCollector.scala 388:66]
  wire  collectorUnit_2_io_bankIn_2_valid; // @[operandCollector.scala 388:66]
  wire [1:0] collectorUnit_2_io_bankIn_2_bits_regOrder; // @[operandCollector.scala 388:66]
  wire [31:0] collectorUnit_2_io_bankIn_2_bits_data_0; // @[operandCollector.scala 388:66]
  wire [31:0] collectorUnit_2_io_bankIn_2_bits_data_1; // @[operandCollector.scala 388:66]
  wire [31:0] collectorUnit_2_io_bankIn_2_bits_data_2; // @[operandCollector.scala 388:66]
  wire [31:0] collectorUnit_2_io_bankIn_2_bits_data_3; // @[operandCollector.scala 388:66]
  wire [31:0] collectorUnit_2_io_bankIn_2_bits_v0_0; // @[operandCollector.scala 388:66]
  wire  collectorUnit_2_io_bankIn_3_ready; // @[operandCollector.scala 388:66]
  wire  collectorUnit_2_io_bankIn_3_valid; // @[operandCollector.scala 388:66]
  wire [1:0] collectorUnit_2_io_bankIn_3_bits_regOrder; // @[operandCollector.scala 388:66]
  wire [31:0] collectorUnit_2_io_bankIn_3_bits_data_0; // @[operandCollector.scala 388:66]
  wire [31:0] collectorUnit_2_io_bankIn_3_bits_data_1; // @[operandCollector.scala 388:66]
  wire [31:0] collectorUnit_2_io_bankIn_3_bits_data_2; // @[operandCollector.scala 388:66]
  wire [31:0] collectorUnit_2_io_bankIn_3_bits_data_3; // @[operandCollector.scala 388:66]
  wire [31:0] collectorUnit_2_io_bankIn_3_bits_v0_0; // @[operandCollector.scala 388:66]
  wire  collectorUnit_2_io_issue_ready; // @[operandCollector.scala 388:66]
  wire  collectorUnit_2_io_issue_valid; // @[operandCollector.scala 388:66]
  wire [31:0] collectorUnit_2_io_issue_bits_alu_src1_0; // @[operandCollector.scala 388:66]
  wire [31:0] collectorUnit_2_io_issue_bits_alu_src1_1; // @[operandCollector.scala 388:66]
  wire [31:0] collectorUnit_2_io_issue_bits_alu_src1_2; // @[operandCollector.scala 388:66]
  wire [31:0] collectorUnit_2_io_issue_bits_alu_src1_3; // @[operandCollector.scala 388:66]
  wire [31:0] collectorUnit_2_io_issue_bits_alu_src2_0; // @[operandCollector.scala 388:66]
  wire [31:0] collectorUnit_2_io_issue_bits_alu_src2_1; // @[operandCollector.scala 388:66]
  wire [31:0] collectorUnit_2_io_issue_bits_alu_src2_2; // @[operandCollector.scala 388:66]
  wire [31:0] collectorUnit_2_io_issue_bits_alu_src2_3; // @[operandCollector.scala 388:66]
  wire [31:0] collectorUnit_2_io_issue_bits_alu_src3_0; // @[operandCollector.scala 388:66]
  wire [31:0] collectorUnit_2_io_issue_bits_alu_src3_1; // @[operandCollector.scala 388:66]
  wire [31:0] collectorUnit_2_io_issue_bits_alu_src3_2; // @[operandCollector.scala 388:66]
  wire [31:0] collectorUnit_2_io_issue_bits_alu_src3_3; // @[operandCollector.scala 388:66]
  wire  collectorUnit_2_io_issue_bits_mask_0; // @[operandCollector.scala 388:66]
  wire  collectorUnit_2_io_issue_bits_mask_1; // @[operandCollector.scala 388:66]
  wire  collectorUnit_2_io_issue_bits_mask_2; // @[operandCollector.scala 388:66]
  wire  collectorUnit_2_io_issue_bits_mask_3; // @[operandCollector.scala 388:66]
  wire [31:0] collectorUnit_2_io_issue_bits_control_inst; // @[operandCollector.scala 388:66]
  wire [1:0] collectorUnit_2_io_issue_bits_control_wid; // @[operandCollector.scala 388:66]
  wire  collectorUnit_2_io_issue_bits_control_fp; // @[operandCollector.scala 388:66]
  wire [1:0] collectorUnit_2_io_issue_bits_control_branch; // @[operandCollector.scala 388:66]
  wire  collectorUnit_2_io_issue_bits_control_simt_stack; // @[operandCollector.scala 388:66]
  wire  collectorUnit_2_io_issue_bits_control_simt_stack_op; // @[operandCollector.scala 388:66]
  wire  collectorUnit_2_io_issue_bits_control_barrier; // @[operandCollector.scala 388:66]
  wire [1:0] collectorUnit_2_io_issue_bits_control_csr; // @[operandCollector.scala 388:66]
  wire  collectorUnit_2_io_issue_bits_control_reverse; // @[operandCollector.scala 388:66]
  wire [1:0] collectorUnit_2_io_issue_bits_control_sel_alu2; // @[operandCollector.scala 388:66]
  wire [1:0] collectorUnit_2_io_issue_bits_control_sel_alu1; // @[operandCollector.scala 388:66]
  wire  collectorUnit_2_io_issue_bits_control_isvec; // @[operandCollector.scala 388:66]
  wire [1:0] collectorUnit_2_io_issue_bits_control_sel_alu3; // @[operandCollector.scala 388:66]
  wire  collectorUnit_2_io_issue_bits_control_mask; // @[operandCollector.scala 388:66]
  wire [2:0] collectorUnit_2_io_issue_bits_control_sel_imm; // @[operandCollector.scala 388:66]
  wire [1:0] collectorUnit_2_io_issue_bits_control_mem_whb; // @[operandCollector.scala 388:66]
  wire  collectorUnit_2_io_issue_bits_control_mem_unsigned; // @[operandCollector.scala 388:66]
  wire [5:0] collectorUnit_2_io_issue_bits_control_alu_fn; // @[operandCollector.scala 388:66]
  wire  collectorUnit_2_io_issue_bits_control_mem; // @[operandCollector.scala 388:66]
  wire  collectorUnit_2_io_issue_bits_control_mul; // @[operandCollector.scala 388:66]
  wire [1:0] collectorUnit_2_io_issue_bits_control_mem_cmd; // @[operandCollector.scala 388:66]
  wire [1:0] collectorUnit_2_io_issue_bits_control_mop; // @[operandCollector.scala 388:66]
  wire [4:0] collectorUnit_2_io_issue_bits_control_reg_idx1; // @[operandCollector.scala 388:66]
  wire [4:0] collectorUnit_2_io_issue_bits_control_reg_idx2; // @[operandCollector.scala 388:66]
  wire [4:0] collectorUnit_2_io_issue_bits_control_reg_idx3; // @[operandCollector.scala 388:66]
  wire [4:0] collectorUnit_2_io_issue_bits_control_reg_idxw; // @[operandCollector.scala 388:66]
  wire  collectorUnit_2_io_issue_bits_control_wfd; // @[operandCollector.scala 388:66]
  wire  collectorUnit_2_io_issue_bits_control_fence; // @[operandCollector.scala 388:66]
  wire  collectorUnit_2_io_issue_bits_control_sfu; // @[operandCollector.scala 388:66]
  wire  collectorUnit_2_io_issue_bits_control_readmask; // @[operandCollector.scala 388:66]
  wire  collectorUnit_2_io_issue_bits_control_writemask; // @[operandCollector.scala 388:66]
  wire  collectorUnit_2_io_issue_bits_control_wxd; // @[operandCollector.scala 388:66]
  wire [31:0] collectorUnit_2_io_issue_bits_control_pc; // @[operandCollector.scala 388:66]
  wire [31:0] collectorUnit_2_io_issue_bits_control_spike_info_pc; // @[operandCollector.scala 388:66]
  wire [31:0] collectorUnit_2_io_issue_bits_control_spike_info_inst; // @[operandCollector.scala 388:66]
  wire  collectorUnit_2_io_outArbiterIO_0_valid; // @[operandCollector.scala 388:66]
  wire [4:0] collectorUnit_2_io_outArbiterIO_0_bits_rsAddr; // @[operandCollector.scala 388:66]
  wire [1:0] collectorUnit_2_io_outArbiterIO_0_bits_bankID; // @[operandCollector.scala 388:66]
  wire [1:0] collectorUnit_2_io_outArbiterIO_0_bits_rsType; // @[operandCollector.scala 388:66]
  wire  collectorUnit_2_io_outArbiterIO_1_valid; // @[operandCollector.scala 388:66]
  wire [4:0] collectorUnit_2_io_outArbiterIO_1_bits_rsAddr; // @[operandCollector.scala 388:66]
  wire [1:0] collectorUnit_2_io_outArbiterIO_1_bits_bankID; // @[operandCollector.scala 388:66]
  wire [1:0] collectorUnit_2_io_outArbiterIO_1_bits_rsType; // @[operandCollector.scala 388:66]
  wire  collectorUnit_2_io_outArbiterIO_2_valid; // @[operandCollector.scala 388:66]
  wire [4:0] collectorUnit_2_io_outArbiterIO_2_bits_rsAddr; // @[operandCollector.scala 388:66]
  wire [1:0] collectorUnit_2_io_outArbiterIO_2_bits_bankID; // @[operandCollector.scala 388:66]
  wire [1:0] collectorUnit_2_io_outArbiterIO_2_bits_rsType; // @[operandCollector.scala 388:66]
  wire  collectorUnit_2_io_outArbiterIO_3_valid; // @[operandCollector.scala 388:66]
  wire [4:0] collectorUnit_2_io_outArbiterIO_3_bits_rsAddr; // @[operandCollector.scala 388:66]
  wire [1:0] collectorUnit_2_io_outArbiterIO_3_bits_bankID; // @[operandCollector.scala 388:66]
  wire  collectorUnit_3_clock; // @[operandCollector.scala 388:66]
  wire  collectorUnit_3_reset; // @[operandCollector.scala 388:66]
  wire  collectorUnit_3_io_control_ready; // @[operandCollector.scala 388:66]
  wire  collectorUnit_3_io_control_valid; // @[operandCollector.scala 388:66]
  wire [31:0] collectorUnit_3_io_control_bits_inst; // @[operandCollector.scala 388:66]
  wire [1:0] collectorUnit_3_io_control_bits_wid; // @[operandCollector.scala 388:66]
  wire  collectorUnit_3_io_control_bits_fp; // @[operandCollector.scala 388:66]
  wire [1:0] collectorUnit_3_io_control_bits_branch; // @[operandCollector.scala 388:66]
  wire  collectorUnit_3_io_control_bits_simt_stack; // @[operandCollector.scala 388:66]
  wire  collectorUnit_3_io_control_bits_simt_stack_op; // @[operandCollector.scala 388:66]
  wire  collectorUnit_3_io_control_bits_barrier; // @[operandCollector.scala 388:66]
  wire [1:0] collectorUnit_3_io_control_bits_csr; // @[operandCollector.scala 388:66]
  wire  collectorUnit_3_io_control_bits_reverse; // @[operandCollector.scala 388:66]
  wire [1:0] collectorUnit_3_io_control_bits_sel_alu2; // @[operandCollector.scala 388:66]
  wire [1:0] collectorUnit_3_io_control_bits_sel_alu1; // @[operandCollector.scala 388:66]
  wire  collectorUnit_3_io_control_bits_isvec; // @[operandCollector.scala 388:66]
  wire [1:0] collectorUnit_3_io_control_bits_sel_alu3; // @[operandCollector.scala 388:66]
  wire  collectorUnit_3_io_control_bits_mask; // @[operandCollector.scala 388:66]
  wire [2:0] collectorUnit_3_io_control_bits_sel_imm; // @[operandCollector.scala 388:66]
  wire [1:0] collectorUnit_3_io_control_bits_mem_whb; // @[operandCollector.scala 388:66]
  wire  collectorUnit_3_io_control_bits_mem_unsigned; // @[operandCollector.scala 388:66]
  wire [5:0] collectorUnit_3_io_control_bits_alu_fn; // @[operandCollector.scala 388:66]
  wire  collectorUnit_3_io_control_bits_mem; // @[operandCollector.scala 388:66]
  wire  collectorUnit_3_io_control_bits_mul; // @[operandCollector.scala 388:66]
  wire [1:0] collectorUnit_3_io_control_bits_mem_cmd; // @[operandCollector.scala 388:66]
  wire [1:0] collectorUnit_3_io_control_bits_mop; // @[operandCollector.scala 388:66]
  wire [4:0] collectorUnit_3_io_control_bits_reg_idx1; // @[operandCollector.scala 388:66]
  wire [4:0] collectorUnit_3_io_control_bits_reg_idx2; // @[operandCollector.scala 388:66]
  wire [4:0] collectorUnit_3_io_control_bits_reg_idx3; // @[operandCollector.scala 388:66]
  wire [4:0] collectorUnit_3_io_control_bits_reg_idxw; // @[operandCollector.scala 388:66]
  wire  collectorUnit_3_io_control_bits_wfd; // @[operandCollector.scala 388:66]
  wire  collectorUnit_3_io_control_bits_fence; // @[operandCollector.scala 388:66]
  wire  collectorUnit_3_io_control_bits_sfu; // @[operandCollector.scala 388:66]
  wire  collectorUnit_3_io_control_bits_readmask; // @[operandCollector.scala 388:66]
  wire  collectorUnit_3_io_control_bits_writemask; // @[operandCollector.scala 388:66]
  wire  collectorUnit_3_io_control_bits_wxd; // @[operandCollector.scala 388:66]
  wire [31:0] collectorUnit_3_io_control_bits_pc; // @[operandCollector.scala 388:66]
  wire [31:0] collectorUnit_3_io_control_bits_spike_info_pc; // @[operandCollector.scala 388:66]
  wire [31:0] collectorUnit_3_io_control_bits_spike_info_inst; // @[operandCollector.scala 388:66]
  wire  collectorUnit_3_io_bankIn_0_ready; // @[operandCollector.scala 388:66]
  wire  collectorUnit_3_io_bankIn_0_valid; // @[operandCollector.scala 388:66]
  wire [1:0] collectorUnit_3_io_bankIn_0_bits_regOrder; // @[operandCollector.scala 388:66]
  wire [31:0] collectorUnit_3_io_bankIn_0_bits_data_0; // @[operandCollector.scala 388:66]
  wire [31:0] collectorUnit_3_io_bankIn_0_bits_data_1; // @[operandCollector.scala 388:66]
  wire [31:0] collectorUnit_3_io_bankIn_0_bits_data_2; // @[operandCollector.scala 388:66]
  wire [31:0] collectorUnit_3_io_bankIn_0_bits_data_3; // @[operandCollector.scala 388:66]
  wire [31:0] collectorUnit_3_io_bankIn_0_bits_v0_0; // @[operandCollector.scala 388:66]
  wire  collectorUnit_3_io_bankIn_1_ready; // @[operandCollector.scala 388:66]
  wire  collectorUnit_3_io_bankIn_1_valid; // @[operandCollector.scala 388:66]
  wire [1:0] collectorUnit_3_io_bankIn_1_bits_regOrder; // @[operandCollector.scala 388:66]
  wire [31:0] collectorUnit_3_io_bankIn_1_bits_data_0; // @[operandCollector.scala 388:66]
  wire [31:0] collectorUnit_3_io_bankIn_1_bits_data_1; // @[operandCollector.scala 388:66]
  wire [31:0] collectorUnit_3_io_bankIn_1_bits_data_2; // @[operandCollector.scala 388:66]
  wire [31:0] collectorUnit_3_io_bankIn_1_bits_data_3; // @[operandCollector.scala 388:66]
  wire [31:0] collectorUnit_3_io_bankIn_1_bits_v0_0; // @[operandCollector.scala 388:66]
  wire  collectorUnit_3_io_bankIn_2_ready; // @[operandCollector.scala 388:66]
  wire  collectorUnit_3_io_bankIn_2_valid; // @[operandCollector.scala 388:66]
  wire [1:0] collectorUnit_3_io_bankIn_2_bits_regOrder; // @[operandCollector.scala 388:66]
  wire [31:0] collectorUnit_3_io_bankIn_2_bits_data_0; // @[operandCollector.scala 388:66]
  wire [31:0] collectorUnit_3_io_bankIn_2_bits_data_1; // @[operandCollector.scala 388:66]
  wire [31:0] collectorUnit_3_io_bankIn_2_bits_data_2; // @[operandCollector.scala 388:66]
  wire [31:0] collectorUnit_3_io_bankIn_2_bits_data_3; // @[operandCollector.scala 388:66]
  wire [31:0] collectorUnit_3_io_bankIn_2_bits_v0_0; // @[operandCollector.scala 388:66]
  wire  collectorUnit_3_io_bankIn_3_ready; // @[operandCollector.scala 388:66]
  wire  collectorUnit_3_io_bankIn_3_valid; // @[operandCollector.scala 388:66]
  wire [1:0] collectorUnit_3_io_bankIn_3_bits_regOrder; // @[operandCollector.scala 388:66]
  wire [31:0] collectorUnit_3_io_bankIn_3_bits_data_0; // @[operandCollector.scala 388:66]
  wire [31:0] collectorUnit_3_io_bankIn_3_bits_data_1; // @[operandCollector.scala 388:66]
  wire [31:0] collectorUnit_3_io_bankIn_3_bits_data_2; // @[operandCollector.scala 388:66]
  wire [31:0] collectorUnit_3_io_bankIn_3_bits_data_3; // @[operandCollector.scala 388:66]
  wire [31:0] collectorUnit_3_io_bankIn_3_bits_v0_0; // @[operandCollector.scala 388:66]
  wire  collectorUnit_3_io_issue_ready; // @[operandCollector.scala 388:66]
  wire  collectorUnit_3_io_issue_valid; // @[operandCollector.scala 388:66]
  wire [31:0] collectorUnit_3_io_issue_bits_alu_src1_0; // @[operandCollector.scala 388:66]
  wire [31:0] collectorUnit_3_io_issue_bits_alu_src1_1; // @[operandCollector.scala 388:66]
  wire [31:0] collectorUnit_3_io_issue_bits_alu_src1_2; // @[operandCollector.scala 388:66]
  wire [31:0] collectorUnit_3_io_issue_bits_alu_src1_3; // @[operandCollector.scala 388:66]
  wire [31:0] collectorUnit_3_io_issue_bits_alu_src2_0; // @[operandCollector.scala 388:66]
  wire [31:0] collectorUnit_3_io_issue_bits_alu_src2_1; // @[operandCollector.scala 388:66]
  wire [31:0] collectorUnit_3_io_issue_bits_alu_src2_2; // @[operandCollector.scala 388:66]
  wire [31:0] collectorUnit_3_io_issue_bits_alu_src2_3; // @[operandCollector.scala 388:66]
  wire [31:0] collectorUnit_3_io_issue_bits_alu_src3_0; // @[operandCollector.scala 388:66]
  wire [31:0] collectorUnit_3_io_issue_bits_alu_src3_1; // @[operandCollector.scala 388:66]
  wire [31:0] collectorUnit_3_io_issue_bits_alu_src3_2; // @[operandCollector.scala 388:66]
  wire [31:0] collectorUnit_3_io_issue_bits_alu_src3_3; // @[operandCollector.scala 388:66]
  wire  collectorUnit_3_io_issue_bits_mask_0; // @[operandCollector.scala 388:66]
  wire  collectorUnit_3_io_issue_bits_mask_1; // @[operandCollector.scala 388:66]
  wire  collectorUnit_3_io_issue_bits_mask_2; // @[operandCollector.scala 388:66]
  wire  collectorUnit_3_io_issue_bits_mask_3; // @[operandCollector.scala 388:66]
  wire [31:0] collectorUnit_3_io_issue_bits_control_inst; // @[operandCollector.scala 388:66]
  wire [1:0] collectorUnit_3_io_issue_bits_control_wid; // @[operandCollector.scala 388:66]
  wire  collectorUnit_3_io_issue_bits_control_fp; // @[operandCollector.scala 388:66]
  wire [1:0] collectorUnit_3_io_issue_bits_control_branch; // @[operandCollector.scala 388:66]
  wire  collectorUnit_3_io_issue_bits_control_simt_stack; // @[operandCollector.scala 388:66]
  wire  collectorUnit_3_io_issue_bits_control_simt_stack_op; // @[operandCollector.scala 388:66]
  wire  collectorUnit_3_io_issue_bits_control_barrier; // @[operandCollector.scala 388:66]
  wire [1:0] collectorUnit_3_io_issue_bits_control_csr; // @[operandCollector.scala 388:66]
  wire  collectorUnit_3_io_issue_bits_control_reverse; // @[operandCollector.scala 388:66]
  wire [1:0] collectorUnit_3_io_issue_bits_control_sel_alu2; // @[operandCollector.scala 388:66]
  wire [1:0] collectorUnit_3_io_issue_bits_control_sel_alu1; // @[operandCollector.scala 388:66]
  wire  collectorUnit_3_io_issue_bits_control_isvec; // @[operandCollector.scala 388:66]
  wire [1:0] collectorUnit_3_io_issue_bits_control_sel_alu3; // @[operandCollector.scala 388:66]
  wire  collectorUnit_3_io_issue_bits_control_mask; // @[operandCollector.scala 388:66]
  wire [2:0] collectorUnit_3_io_issue_bits_control_sel_imm; // @[operandCollector.scala 388:66]
  wire [1:0] collectorUnit_3_io_issue_bits_control_mem_whb; // @[operandCollector.scala 388:66]
  wire  collectorUnit_3_io_issue_bits_control_mem_unsigned; // @[operandCollector.scala 388:66]
  wire [5:0] collectorUnit_3_io_issue_bits_control_alu_fn; // @[operandCollector.scala 388:66]
  wire  collectorUnit_3_io_issue_bits_control_mem; // @[operandCollector.scala 388:66]
  wire  collectorUnit_3_io_issue_bits_control_mul; // @[operandCollector.scala 388:66]
  wire [1:0] collectorUnit_3_io_issue_bits_control_mem_cmd; // @[operandCollector.scala 388:66]
  wire [1:0] collectorUnit_3_io_issue_bits_control_mop; // @[operandCollector.scala 388:66]
  wire [4:0] collectorUnit_3_io_issue_bits_control_reg_idx1; // @[operandCollector.scala 388:66]
  wire [4:0] collectorUnit_3_io_issue_bits_control_reg_idx2; // @[operandCollector.scala 388:66]
  wire [4:0] collectorUnit_3_io_issue_bits_control_reg_idx3; // @[operandCollector.scala 388:66]
  wire [4:0] collectorUnit_3_io_issue_bits_control_reg_idxw; // @[operandCollector.scala 388:66]
  wire  collectorUnit_3_io_issue_bits_control_wfd; // @[operandCollector.scala 388:66]
  wire  collectorUnit_3_io_issue_bits_control_fence; // @[operandCollector.scala 388:66]
  wire  collectorUnit_3_io_issue_bits_control_sfu; // @[operandCollector.scala 388:66]
  wire  collectorUnit_3_io_issue_bits_control_readmask; // @[operandCollector.scala 388:66]
  wire  collectorUnit_3_io_issue_bits_control_writemask; // @[operandCollector.scala 388:66]
  wire  collectorUnit_3_io_issue_bits_control_wxd; // @[operandCollector.scala 388:66]
  wire [31:0] collectorUnit_3_io_issue_bits_control_pc; // @[operandCollector.scala 388:66]
  wire [31:0] collectorUnit_3_io_issue_bits_control_spike_info_pc; // @[operandCollector.scala 388:66]
  wire [31:0] collectorUnit_3_io_issue_bits_control_spike_info_inst; // @[operandCollector.scala 388:66]
  wire  collectorUnit_3_io_outArbiterIO_0_valid; // @[operandCollector.scala 388:66]
  wire [4:0] collectorUnit_3_io_outArbiterIO_0_bits_rsAddr; // @[operandCollector.scala 388:66]
  wire [1:0] collectorUnit_3_io_outArbiterIO_0_bits_bankID; // @[operandCollector.scala 388:66]
  wire [1:0] collectorUnit_3_io_outArbiterIO_0_bits_rsType; // @[operandCollector.scala 388:66]
  wire  collectorUnit_3_io_outArbiterIO_1_valid; // @[operandCollector.scala 388:66]
  wire [4:0] collectorUnit_3_io_outArbiterIO_1_bits_rsAddr; // @[operandCollector.scala 388:66]
  wire [1:0] collectorUnit_3_io_outArbiterIO_1_bits_bankID; // @[operandCollector.scala 388:66]
  wire [1:0] collectorUnit_3_io_outArbiterIO_1_bits_rsType; // @[operandCollector.scala 388:66]
  wire  collectorUnit_3_io_outArbiterIO_2_valid; // @[operandCollector.scala 388:66]
  wire [4:0] collectorUnit_3_io_outArbiterIO_2_bits_rsAddr; // @[operandCollector.scala 388:66]
  wire [1:0] collectorUnit_3_io_outArbiterIO_2_bits_bankID; // @[operandCollector.scala 388:66]
  wire [1:0] collectorUnit_3_io_outArbiterIO_2_bits_rsType; // @[operandCollector.scala 388:66]
  wire  collectorUnit_3_io_outArbiterIO_3_valid; // @[operandCollector.scala 388:66]
  wire [4:0] collectorUnit_3_io_outArbiterIO_3_bits_rsAddr; // @[operandCollector.scala 388:66]
  wire [1:0] collectorUnit_3_io_outArbiterIO_3_bits_bankID; // @[operandCollector.scala 388:66]
  wire  Arbiter_clock; // @[operandCollector.scala 389:23]
  wire  Arbiter_io_readArbiterIO_0_0_valid; // @[operandCollector.scala 389:23]
  wire [4:0] Arbiter_io_readArbiterIO_0_0_bits_rsAddr; // @[operandCollector.scala 389:23]
  wire [1:0] Arbiter_io_readArbiterIO_0_0_bits_bankID; // @[operandCollector.scala 389:23]
  wire [1:0] Arbiter_io_readArbiterIO_0_0_bits_rsType; // @[operandCollector.scala 389:23]
  wire  Arbiter_io_readArbiterIO_0_1_valid; // @[operandCollector.scala 389:23]
  wire [4:0] Arbiter_io_readArbiterIO_0_1_bits_rsAddr; // @[operandCollector.scala 389:23]
  wire [1:0] Arbiter_io_readArbiterIO_0_1_bits_bankID; // @[operandCollector.scala 389:23]
  wire [1:0] Arbiter_io_readArbiterIO_0_1_bits_rsType; // @[operandCollector.scala 389:23]
  wire  Arbiter_io_readArbiterIO_0_2_valid; // @[operandCollector.scala 389:23]
  wire [4:0] Arbiter_io_readArbiterIO_0_2_bits_rsAddr; // @[operandCollector.scala 389:23]
  wire [1:0] Arbiter_io_readArbiterIO_0_2_bits_bankID; // @[operandCollector.scala 389:23]
  wire [1:0] Arbiter_io_readArbiterIO_0_2_bits_rsType; // @[operandCollector.scala 389:23]
  wire  Arbiter_io_readArbiterIO_0_3_valid; // @[operandCollector.scala 389:23]
  wire [4:0] Arbiter_io_readArbiterIO_0_3_bits_rsAddr; // @[operandCollector.scala 389:23]
  wire [1:0] Arbiter_io_readArbiterIO_0_3_bits_bankID; // @[operandCollector.scala 389:23]
  wire  Arbiter_io_readArbiterIO_1_0_valid; // @[operandCollector.scala 389:23]
  wire [4:0] Arbiter_io_readArbiterIO_1_0_bits_rsAddr; // @[operandCollector.scala 389:23]
  wire [1:0] Arbiter_io_readArbiterIO_1_0_bits_bankID; // @[operandCollector.scala 389:23]
  wire [1:0] Arbiter_io_readArbiterIO_1_0_bits_rsType; // @[operandCollector.scala 389:23]
  wire  Arbiter_io_readArbiterIO_1_1_valid; // @[operandCollector.scala 389:23]
  wire [4:0] Arbiter_io_readArbiterIO_1_1_bits_rsAddr; // @[operandCollector.scala 389:23]
  wire [1:0] Arbiter_io_readArbiterIO_1_1_bits_bankID; // @[operandCollector.scala 389:23]
  wire [1:0] Arbiter_io_readArbiterIO_1_1_bits_rsType; // @[operandCollector.scala 389:23]
  wire  Arbiter_io_readArbiterIO_1_2_valid; // @[operandCollector.scala 389:23]
  wire [4:0] Arbiter_io_readArbiterIO_1_2_bits_rsAddr; // @[operandCollector.scala 389:23]
  wire [1:0] Arbiter_io_readArbiterIO_1_2_bits_bankID; // @[operandCollector.scala 389:23]
  wire [1:0] Arbiter_io_readArbiterIO_1_2_bits_rsType; // @[operandCollector.scala 389:23]
  wire  Arbiter_io_readArbiterIO_1_3_valid; // @[operandCollector.scala 389:23]
  wire [4:0] Arbiter_io_readArbiterIO_1_3_bits_rsAddr; // @[operandCollector.scala 389:23]
  wire [1:0] Arbiter_io_readArbiterIO_1_3_bits_bankID; // @[operandCollector.scala 389:23]
  wire  Arbiter_io_readArbiterIO_2_0_valid; // @[operandCollector.scala 389:23]
  wire [4:0] Arbiter_io_readArbiterIO_2_0_bits_rsAddr; // @[operandCollector.scala 389:23]
  wire [1:0] Arbiter_io_readArbiterIO_2_0_bits_bankID; // @[operandCollector.scala 389:23]
  wire [1:0] Arbiter_io_readArbiterIO_2_0_bits_rsType; // @[operandCollector.scala 389:23]
  wire  Arbiter_io_readArbiterIO_2_1_valid; // @[operandCollector.scala 389:23]
  wire [4:0] Arbiter_io_readArbiterIO_2_1_bits_rsAddr; // @[operandCollector.scala 389:23]
  wire [1:0] Arbiter_io_readArbiterIO_2_1_bits_bankID; // @[operandCollector.scala 389:23]
  wire [1:0] Arbiter_io_readArbiterIO_2_1_bits_rsType; // @[operandCollector.scala 389:23]
  wire  Arbiter_io_readArbiterIO_2_2_valid; // @[operandCollector.scala 389:23]
  wire [4:0] Arbiter_io_readArbiterIO_2_2_bits_rsAddr; // @[operandCollector.scala 389:23]
  wire [1:0] Arbiter_io_readArbiterIO_2_2_bits_bankID; // @[operandCollector.scala 389:23]
  wire [1:0] Arbiter_io_readArbiterIO_2_2_bits_rsType; // @[operandCollector.scala 389:23]
  wire  Arbiter_io_readArbiterIO_2_3_valid; // @[operandCollector.scala 389:23]
  wire [4:0] Arbiter_io_readArbiterIO_2_3_bits_rsAddr; // @[operandCollector.scala 389:23]
  wire [1:0] Arbiter_io_readArbiterIO_2_3_bits_bankID; // @[operandCollector.scala 389:23]
  wire  Arbiter_io_readArbiterIO_3_0_valid; // @[operandCollector.scala 389:23]
  wire [4:0] Arbiter_io_readArbiterIO_3_0_bits_rsAddr; // @[operandCollector.scala 389:23]
  wire [1:0] Arbiter_io_readArbiterIO_3_0_bits_bankID; // @[operandCollector.scala 389:23]
  wire [1:0] Arbiter_io_readArbiterIO_3_0_bits_rsType; // @[operandCollector.scala 389:23]
  wire  Arbiter_io_readArbiterIO_3_1_valid; // @[operandCollector.scala 389:23]
  wire [4:0] Arbiter_io_readArbiterIO_3_1_bits_rsAddr; // @[operandCollector.scala 389:23]
  wire [1:0] Arbiter_io_readArbiterIO_3_1_bits_bankID; // @[operandCollector.scala 389:23]
  wire [1:0] Arbiter_io_readArbiterIO_3_1_bits_rsType; // @[operandCollector.scala 389:23]
  wire  Arbiter_io_readArbiterIO_3_2_valid; // @[operandCollector.scala 389:23]
  wire [4:0] Arbiter_io_readArbiterIO_3_2_bits_rsAddr; // @[operandCollector.scala 389:23]
  wire [1:0] Arbiter_io_readArbiterIO_3_2_bits_bankID; // @[operandCollector.scala 389:23]
  wire [1:0] Arbiter_io_readArbiterIO_3_2_bits_rsType; // @[operandCollector.scala 389:23]
  wire  Arbiter_io_readArbiterIO_3_3_valid; // @[operandCollector.scala 389:23]
  wire [4:0] Arbiter_io_readArbiterIO_3_3_bits_rsAddr; // @[operandCollector.scala 389:23]
  wire [1:0] Arbiter_io_readArbiterIO_3_3_bits_bankID; // @[operandCollector.scala 389:23]
  wire  Arbiter_io_readArbiterOut_0_valid; // @[operandCollector.scala 389:23]
  wire [4:0] Arbiter_io_readArbiterOut_0_bits_rsAddr; // @[operandCollector.scala 389:23]
  wire [1:0] Arbiter_io_readArbiterOut_0_bits_rsType; // @[operandCollector.scala 389:23]
  wire  Arbiter_io_readArbiterOut_1_valid; // @[operandCollector.scala 389:23]
  wire [4:0] Arbiter_io_readArbiterOut_1_bits_rsAddr; // @[operandCollector.scala 389:23]
  wire [1:0] Arbiter_io_readArbiterOut_1_bits_rsType; // @[operandCollector.scala 389:23]
  wire  Arbiter_io_readArbiterOut_2_valid; // @[operandCollector.scala 389:23]
  wire [4:0] Arbiter_io_readArbiterOut_2_bits_rsAddr; // @[operandCollector.scala 389:23]
  wire [1:0] Arbiter_io_readArbiterOut_2_bits_rsType; // @[operandCollector.scala 389:23]
  wire  Arbiter_io_readArbiterOut_3_valid; // @[operandCollector.scala 389:23]
  wire [4:0] Arbiter_io_readArbiterOut_3_bits_rsAddr; // @[operandCollector.scala 389:23]
  wire [1:0] Arbiter_io_readArbiterOut_3_bits_rsType; // @[operandCollector.scala 389:23]
  wire [3:0] Arbiter_io_readchosen_0; // @[operandCollector.scala 389:23]
  wire [3:0] Arbiter_io_readchosen_1; // @[operandCollector.scala 389:23]
  wire [3:0] Arbiter_io_readchosen_2; // @[operandCollector.scala 389:23]
  wire [3:0] Arbiter_io_readchosen_3; // @[operandCollector.scala 389:23]
  wire  FloatRegFileBank_clock; // @[operandCollector.scala 390:53]
  wire [31:0] FloatRegFileBank_io_v0_0; // @[operandCollector.scala 390:53]
  wire [31:0] FloatRegFileBank_io_rs_0; // @[operandCollector.scala 390:53]
  wire [31:0] FloatRegFileBank_io_rs_1; // @[operandCollector.scala 390:53]
  wire [31:0] FloatRegFileBank_io_rs_2; // @[operandCollector.scala 390:53]
  wire [31:0] FloatRegFileBank_io_rs_3; // @[operandCollector.scala 390:53]
  wire [4:0] FloatRegFileBank_io_rsidx; // @[operandCollector.scala 390:53]
  wire [31:0] FloatRegFileBank_io_rd_0; // @[operandCollector.scala 390:53]
  wire [31:0] FloatRegFileBank_io_rd_1; // @[operandCollector.scala 390:53]
  wire [31:0] FloatRegFileBank_io_rd_2; // @[operandCollector.scala 390:53]
  wire [31:0] FloatRegFileBank_io_rd_3; // @[operandCollector.scala 390:53]
  wire [4:0] FloatRegFileBank_io_rdidx; // @[operandCollector.scala 390:53]
  wire  FloatRegFileBank_io_rdwen; // @[operandCollector.scala 390:53]
  wire  FloatRegFileBank_io_rdwmask_0; // @[operandCollector.scala 390:53]
  wire  FloatRegFileBank_io_rdwmask_1; // @[operandCollector.scala 390:53]
  wire  FloatRegFileBank_io_rdwmask_2; // @[operandCollector.scala 390:53]
  wire  FloatRegFileBank_io_rdwmask_3; // @[operandCollector.scala 390:53]
  wire  FloatRegFileBank_1_clock; // @[operandCollector.scala 390:53]
  wire [31:0] FloatRegFileBank_1_io_v0_0; // @[operandCollector.scala 390:53]
  wire [31:0] FloatRegFileBank_1_io_rs_0; // @[operandCollector.scala 390:53]
  wire [31:0] FloatRegFileBank_1_io_rs_1; // @[operandCollector.scala 390:53]
  wire [31:0] FloatRegFileBank_1_io_rs_2; // @[operandCollector.scala 390:53]
  wire [31:0] FloatRegFileBank_1_io_rs_3; // @[operandCollector.scala 390:53]
  wire [4:0] FloatRegFileBank_1_io_rsidx; // @[operandCollector.scala 390:53]
  wire [31:0] FloatRegFileBank_1_io_rd_0; // @[operandCollector.scala 390:53]
  wire [31:0] FloatRegFileBank_1_io_rd_1; // @[operandCollector.scala 390:53]
  wire [31:0] FloatRegFileBank_1_io_rd_2; // @[operandCollector.scala 390:53]
  wire [31:0] FloatRegFileBank_1_io_rd_3; // @[operandCollector.scala 390:53]
  wire [4:0] FloatRegFileBank_1_io_rdidx; // @[operandCollector.scala 390:53]
  wire  FloatRegFileBank_1_io_rdwen; // @[operandCollector.scala 390:53]
  wire  FloatRegFileBank_1_io_rdwmask_0; // @[operandCollector.scala 390:53]
  wire  FloatRegFileBank_1_io_rdwmask_1; // @[operandCollector.scala 390:53]
  wire  FloatRegFileBank_1_io_rdwmask_2; // @[operandCollector.scala 390:53]
  wire  FloatRegFileBank_1_io_rdwmask_3; // @[operandCollector.scala 390:53]
  wire  FloatRegFileBank_2_clock; // @[operandCollector.scala 390:53]
  wire [31:0] FloatRegFileBank_2_io_v0_0; // @[operandCollector.scala 390:53]
  wire [31:0] FloatRegFileBank_2_io_rs_0; // @[operandCollector.scala 390:53]
  wire [31:0] FloatRegFileBank_2_io_rs_1; // @[operandCollector.scala 390:53]
  wire [31:0] FloatRegFileBank_2_io_rs_2; // @[operandCollector.scala 390:53]
  wire [31:0] FloatRegFileBank_2_io_rs_3; // @[operandCollector.scala 390:53]
  wire [4:0] FloatRegFileBank_2_io_rsidx; // @[operandCollector.scala 390:53]
  wire [31:0] FloatRegFileBank_2_io_rd_0; // @[operandCollector.scala 390:53]
  wire [31:0] FloatRegFileBank_2_io_rd_1; // @[operandCollector.scala 390:53]
  wire [31:0] FloatRegFileBank_2_io_rd_2; // @[operandCollector.scala 390:53]
  wire [31:0] FloatRegFileBank_2_io_rd_3; // @[operandCollector.scala 390:53]
  wire [4:0] FloatRegFileBank_2_io_rdidx; // @[operandCollector.scala 390:53]
  wire  FloatRegFileBank_2_io_rdwen; // @[operandCollector.scala 390:53]
  wire  FloatRegFileBank_2_io_rdwmask_0; // @[operandCollector.scala 390:53]
  wire  FloatRegFileBank_2_io_rdwmask_1; // @[operandCollector.scala 390:53]
  wire  FloatRegFileBank_2_io_rdwmask_2; // @[operandCollector.scala 390:53]
  wire  FloatRegFileBank_2_io_rdwmask_3; // @[operandCollector.scala 390:53]
  wire  FloatRegFileBank_3_clock; // @[operandCollector.scala 390:53]
  wire [31:0] FloatRegFileBank_3_io_v0_0; // @[operandCollector.scala 390:53]
  wire [31:0] FloatRegFileBank_3_io_rs_0; // @[operandCollector.scala 390:53]
  wire [31:0] FloatRegFileBank_3_io_rs_1; // @[operandCollector.scala 390:53]
  wire [31:0] FloatRegFileBank_3_io_rs_2; // @[operandCollector.scala 390:53]
  wire [31:0] FloatRegFileBank_3_io_rs_3; // @[operandCollector.scala 390:53]
  wire [4:0] FloatRegFileBank_3_io_rsidx; // @[operandCollector.scala 390:53]
  wire [31:0] FloatRegFileBank_3_io_rd_0; // @[operandCollector.scala 390:53]
  wire [31:0] FloatRegFileBank_3_io_rd_1; // @[operandCollector.scala 390:53]
  wire [31:0] FloatRegFileBank_3_io_rd_2; // @[operandCollector.scala 390:53]
  wire [31:0] FloatRegFileBank_3_io_rd_3; // @[operandCollector.scala 390:53]
  wire [4:0] FloatRegFileBank_3_io_rdidx; // @[operandCollector.scala 390:53]
  wire  FloatRegFileBank_3_io_rdwen; // @[operandCollector.scala 390:53]
  wire  FloatRegFileBank_3_io_rdwmask_0; // @[operandCollector.scala 390:53]
  wire  FloatRegFileBank_3_io_rdwmask_1; // @[operandCollector.scala 390:53]
  wire  FloatRegFileBank_3_io_rdwmask_2; // @[operandCollector.scala 390:53]
  wire  FloatRegFileBank_3_io_rdwmask_3; // @[operandCollector.scala 390:53]
  wire  RegFileBank_clock; // @[operandCollector.scala 391:53]
  wire [31:0] RegFileBank_io_rs; // @[operandCollector.scala 391:53]
  wire [4:0] RegFileBank_io_rsidx; // @[operandCollector.scala 391:53]
  wire [31:0] RegFileBank_io_rd; // @[operandCollector.scala 391:53]
  wire [4:0] RegFileBank_io_rdidx; // @[operandCollector.scala 391:53]
  wire  RegFileBank_io_rdwen; // @[operandCollector.scala 391:53]
  wire  RegFileBank_1_clock; // @[operandCollector.scala 391:53]
  wire [31:0] RegFileBank_1_io_rs; // @[operandCollector.scala 391:53]
  wire [4:0] RegFileBank_1_io_rsidx; // @[operandCollector.scala 391:53]
  wire [31:0] RegFileBank_1_io_rd; // @[operandCollector.scala 391:53]
  wire [4:0] RegFileBank_1_io_rdidx; // @[operandCollector.scala 391:53]
  wire  RegFileBank_1_io_rdwen; // @[operandCollector.scala 391:53]
  wire  RegFileBank_2_clock; // @[operandCollector.scala 391:53]
  wire [31:0] RegFileBank_2_io_rs; // @[operandCollector.scala 391:53]
  wire [4:0] RegFileBank_2_io_rsidx; // @[operandCollector.scala 391:53]
  wire [31:0] RegFileBank_2_io_rd; // @[operandCollector.scala 391:53]
  wire [4:0] RegFileBank_2_io_rdidx; // @[operandCollector.scala 391:53]
  wire  RegFileBank_2_io_rdwen; // @[operandCollector.scala 391:53]
  wire  RegFileBank_3_clock; // @[operandCollector.scala 391:53]
  wire [31:0] RegFileBank_3_io_rs; // @[operandCollector.scala 391:53]
  wire [4:0] RegFileBank_3_io_rsidx; // @[operandCollector.scala 391:53]
  wire [31:0] RegFileBank_3_io_rd; // @[operandCollector.scala 391:53]
  wire [4:0] RegFileBank_3_io_rdidx; // @[operandCollector.scala 391:53]
  wire  RegFileBank_3_io_rdwen; // @[operandCollector.scala 391:53]
  wire [3:0] crossBar_io_chosen_0; // @[operandCollector.scala 392:24]
  wire [3:0] crossBar_io_chosen_1; // @[operandCollector.scala 392:24]
  wire [3:0] crossBar_io_chosen_2; // @[operandCollector.scala 392:24]
  wire [3:0] crossBar_io_chosen_3; // @[operandCollector.scala 392:24]
  wire  crossBar_io_validArbiter_0; // @[operandCollector.scala 392:24]
  wire  crossBar_io_validArbiter_1; // @[operandCollector.scala 392:24]
  wire  crossBar_io_validArbiter_2; // @[operandCollector.scala 392:24]
  wire  crossBar_io_validArbiter_3; // @[operandCollector.scala 392:24]
  wire [31:0] crossBar_io_dataIn_rs_0_0; // @[operandCollector.scala 392:24]
  wire [31:0] crossBar_io_dataIn_rs_0_1; // @[operandCollector.scala 392:24]
  wire [31:0] crossBar_io_dataIn_rs_0_2; // @[operandCollector.scala 392:24]
  wire [31:0] crossBar_io_dataIn_rs_0_3; // @[operandCollector.scala 392:24]
  wire [31:0] crossBar_io_dataIn_rs_1_0; // @[operandCollector.scala 392:24]
  wire [31:0] crossBar_io_dataIn_rs_1_1; // @[operandCollector.scala 392:24]
  wire [31:0] crossBar_io_dataIn_rs_1_2; // @[operandCollector.scala 392:24]
  wire [31:0] crossBar_io_dataIn_rs_1_3; // @[operandCollector.scala 392:24]
  wire [31:0] crossBar_io_dataIn_rs_2_0; // @[operandCollector.scala 392:24]
  wire [31:0] crossBar_io_dataIn_rs_2_1; // @[operandCollector.scala 392:24]
  wire [31:0] crossBar_io_dataIn_rs_2_2; // @[operandCollector.scala 392:24]
  wire [31:0] crossBar_io_dataIn_rs_2_3; // @[operandCollector.scala 392:24]
  wire [31:0] crossBar_io_dataIn_rs_3_0; // @[operandCollector.scala 392:24]
  wire [31:0] crossBar_io_dataIn_rs_3_1; // @[operandCollector.scala 392:24]
  wire [31:0] crossBar_io_dataIn_rs_3_2; // @[operandCollector.scala 392:24]
  wire [31:0] crossBar_io_dataIn_rs_3_3; // @[operandCollector.scala 392:24]
  wire [31:0] crossBar_io_dataIn_v0_0_0; // @[operandCollector.scala 392:24]
  wire [31:0] crossBar_io_dataIn_v0_1_0; // @[operandCollector.scala 392:24]
  wire [31:0] crossBar_io_dataIn_v0_2_0; // @[operandCollector.scala 392:24]
  wire [31:0] crossBar_io_dataIn_v0_3_0; // @[operandCollector.scala 392:24]
  wire  crossBar_io_out_0_0_valid; // @[operandCollector.scala 392:24]
  wire [1:0] crossBar_io_out_0_0_bits_regOrder; // @[operandCollector.scala 392:24]
  wire [31:0] crossBar_io_out_0_0_bits_data_0; // @[operandCollector.scala 392:24]
  wire [31:0] crossBar_io_out_0_0_bits_data_1; // @[operandCollector.scala 392:24]
  wire [31:0] crossBar_io_out_0_0_bits_data_2; // @[operandCollector.scala 392:24]
  wire [31:0] crossBar_io_out_0_0_bits_data_3; // @[operandCollector.scala 392:24]
  wire [31:0] crossBar_io_out_0_0_bits_v0_0; // @[operandCollector.scala 392:24]
  wire  crossBar_io_out_0_1_valid; // @[operandCollector.scala 392:24]
  wire [1:0] crossBar_io_out_0_1_bits_regOrder; // @[operandCollector.scala 392:24]
  wire [31:0] crossBar_io_out_0_1_bits_data_0; // @[operandCollector.scala 392:24]
  wire [31:0] crossBar_io_out_0_1_bits_data_1; // @[operandCollector.scala 392:24]
  wire [31:0] crossBar_io_out_0_1_bits_data_2; // @[operandCollector.scala 392:24]
  wire [31:0] crossBar_io_out_0_1_bits_data_3; // @[operandCollector.scala 392:24]
  wire [31:0] crossBar_io_out_0_1_bits_v0_0; // @[operandCollector.scala 392:24]
  wire  crossBar_io_out_0_2_valid; // @[operandCollector.scala 392:24]
  wire [1:0] crossBar_io_out_0_2_bits_regOrder; // @[operandCollector.scala 392:24]
  wire [31:0] crossBar_io_out_0_2_bits_data_0; // @[operandCollector.scala 392:24]
  wire [31:0] crossBar_io_out_0_2_bits_data_1; // @[operandCollector.scala 392:24]
  wire [31:0] crossBar_io_out_0_2_bits_data_2; // @[operandCollector.scala 392:24]
  wire [31:0] crossBar_io_out_0_2_bits_data_3; // @[operandCollector.scala 392:24]
  wire [31:0] crossBar_io_out_0_2_bits_v0_0; // @[operandCollector.scala 392:24]
  wire  crossBar_io_out_0_3_valid; // @[operandCollector.scala 392:24]
  wire [1:0] crossBar_io_out_0_3_bits_regOrder; // @[operandCollector.scala 392:24]
  wire [31:0] crossBar_io_out_0_3_bits_data_0; // @[operandCollector.scala 392:24]
  wire [31:0] crossBar_io_out_0_3_bits_data_1; // @[operandCollector.scala 392:24]
  wire [31:0] crossBar_io_out_0_3_bits_data_2; // @[operandCollector.scala 392:24]
  wire [31:0] crossBar_io_out_0_3_bits_data_3; // @[operandCollector.scala 392:24]
  wire [31:0] crossBar_io_out_0_3_bits_v0_0; // @[operandCollector.scala 392:24]
  wire  crossBar_io_out_1_0_valid; // @[operandCollector.scala 392:24]
  wire [1:0] crossBar_io_out_1_0_bits_regOrder; // @[operandCollector.scala 392:24]
  wire [31:0] crossBar_io_out_1_0_bits_data_0; // @[operandCollector.scala 392:24]
  wire [31:0] crossBar_io_out_1_0_bits_data_1; // @[operandCollector.scala 392:24]
  wire [31:0] crossBar_io_out_1_0_bits_data_2; // @[operandCollector.scala 392:24]
  wire [31:0] crossBar_io_out_1_0_bits_data_3; // @[operandCollector.scala 392:24]
  wire [31:0] crossBar_io_out_1_0_bits_v0_0; // @[operandCollector.scala 392:24]
  wire  crossBar_io_out_1_1_valid; // @[operandCollector.scala 392:24]
  wire [1:0] crossBar_io_out_1_1_bits_regOrder; // @[operandCollector.scala 392:24]
  wire [31:0] crossBar_io_out_1_1_bits_data_0; // @[operandCollector.scala 392:24]
  wire [31:0] crossBar_io_out_1_1_bits_data_1; // @[operandCollector.scala 392:24]
  wire [31:0] crossBar_io_out_1_1_bits_data_2; // @[operandCollector.scala 392:24]
  wire [31:0] crossBar_io_out_1_1_bits_data_3; // @[operandCollector.scala 392:24]
  wire [31:0] crossBar_io_out_1_1_bits_v0_0; // @[operandCollector.scala 392:24]
  wire  crossBar_io_out_1_2_valid; // @[operandCollector.scala 392:24]
  wire [1:0] crossBar_io_out_1_2_bits_regOrder; // @[operandCollector.scala 392:24]
  wire [31:0] crossBar_io_out_1_2_bits_data_0; // @[operandCollector.scala 392:24]
  wire [31:0] crossBar_io_out_1_2_bits_data_1; // @[operandCollector.scala 392:24]
  wire [31:0] crossBar_io_out_1_2_bits_data_2; // @[operandCollector.scala 392:24]
  wire [31:0] crossBar_io_out_1_2_bits_data_3; // @[operandCollector.scala 392:24]
  wire [31:0] crossBar_io_out_1_2_bits_v0_0; // @[operandCollector.scala 392:24]
  wire  crossBar_io_out_1_3_valid; // @[operandCollector.scala 392:24]
  wire [1:0] crossBar_io_out_1_3_bits_regOrder; // @[operandCollector.scala 392:24]
  wire [31:0] crossBar_io_out_1_3_bits_data_0; // @[operandCollector.scala 392:24]
  wire [31:0] crossBar_io_out_1_3_bits_data_1; // @[operandCollector.scala 392:24]
  wire [31:0] crossBar_io_out_1_3_bits_data_2; // @[operandCollector.scala 392:24]
  wire [31:0] crossBar_io_out_1_3_bits_data_3; // @[operandCollector.scala 392:24]
  wire [31:0] crossBar_io_out_1_3_bits_v0_0; // @[operandCollector.scala 392:24]
  wire  crossBar_io_out_2_0_valid; // @[operandCollector.scala 392:24]
  wire [1:0] crossBar_io_out_2_0_bits_regOrder; // @[operandCollector.scala 392:24]
  wire [31:0] crossBar_io_out_2_0_bits_data_0; // @[operandCollector.scala 392:24]
  wire [31:0] crossBar_io_out_2_0_bits_data_1; // @[operandCollector.scala 392:24]
  wire [31:0] crossBar_io_out_2_0_bits_data_2; // @[operandCollector.scala 392:24]
  wire [31:0] crossBar_io_out_2_0_bits_data_3; // @[operandCollector.scala 392:24]
  wire [31:0] crossBar_io_out_2_0_bits_v0_0; // @[operandCollector.scala 392:24]
  wire  crossBar_io_out_2_1_valid; // @[operandCollector.scala 392:24]
  wire [1:0] crossBar_io_out_2_1_bits_regOrder; // @[operandCollector.scala 392:24]
  wire [31:0] crossBar_io_out_2_1_bits_data_0; // @[operandCollector.scala 392:24]
  wire [31:0] crossBar_io_out_2_1_bits_data_1; // @[operandCollector.scala 392:24]
  wire [31:0] crossBar_io_out_2_1_bits_data_2; // @[operandCollector.scala 392:24]
  wire [31:0] crossBar_io_out_2_1_bits_data_3; // @[operandCollector.scala 392:24]
  wire [31:0] crossBar_io_out_2_1_bits_v0_0; // @[operandCollector.scala 392:24]
  wire  crossBar_io_out_2_2_valid; // @[operandCollector.scala 392:24]
  wire [1:0] crossBar_io_out_2_2_bits_regOrder; // @[operandCollector.scala 392:24]
  wire [31:0] crossBar_io_out_2_2_bits_data_0; // @[operandCollector.scala 392:24]
  wire [31:0] crossBar_io_out_2_2_bits_data_1; // @[operandCollector.scala 392:24]
  wire [31:0] crossBar_io_out_2_2_bits_data_2; // @[operandCollector.scala 392:24]
  wire [31:0] crossBar_io_out_2_2_bits_data_3; // @[operandCollector.scala 392:24]
  wire [31:0] crossBar_io_out_2_2_bits_v0_0; // @[operandCollector.scala 392:24]
  wire  crossBar_io_out_2_3_valid; // @[operandCollector.scala 392:24]
  wire [1:0] crossBar_io_out_2_3_bits_regOrder; // @[operandCollector.scala 392:24]
  wire [31:0] crossBar_io_out_2_3_bits_data_0; // @[operandCollector.scala 392:24]
  wire [31:0] crossBar_io_out_2_3_bits_data_1; // @[operandCollector.scala 392:24]
  wire [31:0] crossBar_io_out_2_3_bits_data_2; // @[operandCollector.scala 392:24]
  wire [31:0] crossBar_io_out_2_3_bits_data_3; // @[operandCollector.scala 392:24]
  wire [31:0] crossBar_io_out_2_3_bits_v0_0; // @[operandCollector.scala 392:24]
  wire  crossBar_io_out_3_0_valid; // @[operandCollector.scala 392:24]
  wire [1:0] crossBar_io_out_3_0_bits_regOrder; // @[operandCollector.scala 392:24]
  wire [31:0] crossBar_io_out_3_0_bits_data_0; // @[operandCollector.scala 392:24]
  wire [31:0] crossBar_io_out_3_0_bits_data_1; // @[operandCollector.scala 392:24]
  wire [31:0] crossBar_io_out_3_0_bits_data_2; // @[operandCollector.scala 392:24]
  wire [31:0] crossBar_io_out_3_0_bits_data_3; // @[operandCollector.scala 392:24]
  wire [31:0] crossBar_io_out_3_0_bits_v0_0; // @[operandCollector.scala 392:24]
  wire  crossBar_io_out_3_1_valid; // @[operandCollector.scala 392:24]
  wire [1:0] crossBar_io_out_3_1_bits_regOrder; // @[operandCollector.scala 392:24]
  wire [31:0] crossBar_io_out_3_1_bits_data_0; // @[operandCollector.scala 392:24]
  wire [31:0] crossBar_io_out_3_1_bits_data_1; // @[operandCollector.scala 392:24]
  wire [31:0] crossBar_io_out_3_1_bits_data_2; // @[operandCollector.scala 392:24]
  wire [31:0] crossBar_io_out_3_1_bits_data_3; // @[operandCollector.scala 392:24]
  wire [31:0] crossBar_io_out_3_1_bits_v0_0; // @[operandCollector.scala 392:24]
  wire  crossBar_io_out_3_2_valid; // @[operandCollector.scala 392:24]
  wire [1:0] crossBar_io_out_3_2_bits_regOrder; // @[operandCollector.scala 392:24]
  wire [31:0] crossBar_io_out_3_2_bits_data_0; // @[operandCollector.scala 392:24]
  wire [31:0] crossBar_io_out_3_2_bits_data_1; // @[operandCollector.scala 392:24]
  wire [31:0] crossBar_io_out_3_2_bits_data_2; // @[operandCollector.scala 392:24]
  wire [31:0] crossBar_io_out_3_2_bits_data_3; // @[operandCollector.scala 392:24]
  wire [31:0] crossBar_io_out_3_2_bits_v0_0; // @[operandCollector.scala 392:24]
  wire  crossBar_io_out_3_3_valid; // @[operandCollector.scala 392:24]
  wire [1:0] crossBar_io_out_3_3_bits_regOrder; // @[operandCollector.scala 392:24]
  wire [31:0] crossBar_io_out_3_3_bits_data_0; // @[operandCollector.scala 392:24]
  wire [31:0] crossBar_io_out_3_3_bits_data_1; // @[operandCollector.scala 392:24]
  wire [31:0] crossBar_io_out_3_3_bits_data_2; // @[operandCollector.scala 392:24]
  wire [31:0] crossBar_io_out_3_3_bits_data_3; // @[operandCollector.scala 392:24]
  wire [31:0] crossBar_io_out_3_3_bits_v0_0; // @[operandCollector.scala 392:24]
  wire  Demux_io_in_ready; // @[operandCollector.scala 393:21]
  wire  Demux_io_in_valid; // @[operandCollector.scala 393:21]
  wire [31:0] Demux_io_in_bits_inst; // @[operandCollector.scala 393:21]
  wire [1:0] Demux_io_in_bits_wid; // @[operandCollector.scala 393:21]
  wire  Demux_io_in_bits_fp; // @[operandCollector.scala 393:21]
  wire [1:0] Demux_io_in_bits_branch; // @[operandCollector.scala 393:21]
  wire  Demux_io_in_bits_simt_stack; // @[operandCollector.scala 393:21]
  wire  Demux_io_in_bits_simt_stack_op; // @[operandCollector.scala 393:21]
  wire  Demux_io_in_bits_barrier; // @[operandCollector.scala 393:21]
  wire [1:0] Demux_io_in_bits_csr; // @[operandCollector.scala 393:21]
  wire  Demux_io_in_bits_reverse; // @[operandCollector.scala 393:21]
  wire [1:0] Demux_io_in_bits_sel_alu2; // @[operandCollector.scala 393:21]
  wire [1:0] Demux_io_in_bits_sel_alu1; // @[operandCollector.scala 393:21]
  wire  Demux_io_in_bits_isvec; // @[operandCollector.scala 393:21]
  wire [1:0] Demux_io_in_bits_sel_alu3; // @[operandCollector.scala 393:21]
  wire  Demux_io_in_bits_mask; // @[operandCollector.scala 393:21]
  wire [2:0] Demux_io_in_bits_sel_imm; // @[operandCollector.scala 393:21]
  wire [1:0] Demux_io_in_bits_mem_whb; // @[operandCollector.scala 393:21]
  wire  Demux_io_in_bits_mem_unsigned; // @[operandCollector.scala 393:21]
  wire [5:0] Demux_io_in_bits_alu_fn; // @[operandCollector.scala 393:21]
  wire  Demux_io_in_bits_mem; // @[operandCollector.scala 393:21]
  wire  Demux_io_in_bits_mul; // @[operandCollector.scala 393:21]
  wire [1:0] Demux_io_in_bits_mem_cmd; // @[operandCollector.scala 393:21]
  wire [1:0] Demux_io_in_bits_mop; // @[operandCollector.scala 393:21]
  wire [4:0] Demux_io_in_bits_reg_idx1; // @[operandCollector.scala 393:21]
  wire [4:0] Demux_io_in_bits_reg_idx2; // @[operandCollector.scala 393:21]
  wire [4:0] Demux_io_in_bits_reg_idx3; // @[operandCollector.scala 393:21]
  wire [4:0] Demux_io_in_bits_reg_idxw; // @[operandCollector.scala 393:21]
  wire  Demux_io_in_bits_wfd; // @[operandCollector.scala 393:21]
  wire  Demux_io_in_bits_fence; // @[operandCollector.scala 393:21]
  wire  Demux_io_in_bits_sfu; // @[operandCollector.scala 393:21]
  wire  Demux_io_in_bits_readmask; // @[operandCollector.scala 393:21]
  wire  Demux_io_in_bits_writemask; // @[operandCollector.scala 393:21]
  wire  Demux_io_in_bits_wxd; // @[operandCollector.scala 393:21]
  wire [31:0] Demux_io_in_bits_pc; // @[operandCollector.scala 393:21]
  wire [31:0] Demux_io_in_bits_spike_info_pc; // @[operandCollector.scala 393:21]
  wire [31:0] Demux_io_in_bits_spike_info_inst; // @[operandCollector.scala 393:21]
  wire  Demux_io_out_0_ready; // @[operandCollector.scala 393:21]
  wire  Demux_io_out_0_valid; // @[operandCollector.scala 393:21]
  wire [31:0] Demux_io_out_0_bits_inst; // @[operandCollector.scala 393:21]
  wire [1:0] Demux_io_out_0_bits_wid; // @[operandCollector.scala 393:21]
  wire  Demux_io_out_0_bits_fp; // @[operandCollector.scala 393:21]
  wire [1:0] Demux_io_out_0_bits_branch; // @[operandCollector.scala 393:21]
  wire  Demux_io_out_0_bits_simt_stack; // @[operandCollector.scala 393:21]
  wire  Demux_io_out_0_bits_simt_stack_op; // @[operandCollector.scala 393:21]
  wire  Demux_io_out_0_bits_barrier; // @[operandCollector.scala 393:21]
  wire [1:0] Demux_io_out_0_bits_csr; // @[operandCollector.scala 393:21]
  wire  Demux_io_out_0_bits_reverse; // @[operandCollector.scala 393:21]
  wire [1:0] Demux_io_out_0_bits_sel_alu2; // @[operandCollector.scala 393:21]
  wire [1:0] Demux_io_out_0_bits_sel_alu1; // @[operandCollector.scala 393:21]
  wire  Demux_io_out_0_bits_isvec; // @[operandCollector.scala 393:21]
  wire [1:0] Demux_io_out_0_bits_sel_alu3; // @[operandCollector.scala 393:21]
  wire  Demux_io_out_0_bits_mask; // @[operandCollector.scala 393:21]
  wire [2:0] Demux_io_out_0_bits_sel_imm; // @[operandCollector.scala 393:21]
  wire [1:0] Demux_io_out_0_bits_mem_whb; // @[operandCollector.scala 393:21]
  wire  Demux_io_out_0_bits_mem_unsigned; // @[operandCollector.scala 393:21]
  wire [5:0] Demux_io_out_0_bits_alu_fn; // @[operandCollector.scala 393:21]
  wire  Demux_io_out_0_bits_mem; // @[operandCollector.scala 393:21]
  wire  Demux_io_out_0_bits_mul; // @[operandCollector.scala 393:21]
  wire [1:0] Demux_io_out_0_bits_mem_cmd; // @[operandCollector.scala 393:21]
  wire [1:0] Demux_io_out_0_bits_mop; // @[operandCollector.scala 393:21]
  wire [4:0] Demux_io_out_0_bits_reg_idx1; // @[operandCollector.scala 393:21]
  wire [4:0] Demux_io_out_0_bits_reg_idx2; // @[operandCollector.scala 393:21]
  wire [4:0] Demux_io_out_0_bits_reg_idx3; // @[operandCollector.scala 393:21]
  wire [4:0] Demux_io_out_0_bits_reg_idxw; // @[operandCollector.scala 393:21]
  wire  Demux_io_out_0_bits_wfd; // @[operandCollector.scala 393:21]
  wire  Demux_io_out_0_bits_fence; // @[operandCollector.scala 393:21]
  wire  Demux_io_out_0_bits_sfu; // @[operandCollector.scala 393:21]
  wire  Demux_io_out_0_bits_readmask; // @[operandCollector.scala 393:21]
  wire  Demux_io_out_0_bits_writemask; // @[operandCollector.scala 393:21]
  wire  Demux_io_out_0_bits_wxd; // @[operandCollector.scala 393:21]
  wire [31:0] Demux_io_out_0_bits_pc; // @[operandCollector.scala 393:21]
  wire [31:0] Demux_io_out_0_bits_spike_info_pc; // @[operandCollector.scala 393:21]
  wire [31:0] Demux_io_out_0_bits_spike_info_inst; // @[operandCollector.scala 393:21]
  wire  Demux_io_out_1_ready; // @[operandCollector.scala 393:21]
  wire  Demux_io_out_1_valid; // @[operandCollector.scala 393:21]
  wire [31:0] Demux_io_out_1_bits_inst; // @[operandCollector.scala 393:21]
  wire [1:0] Demux_io_out_1_bits_wid; // @[operandCollector.scala 393:21]
  wire  Demux_io_out_1_bits_fp; // @[operandCollector.scala 393:21]
  wire [1:0] Demux_io_out_1_bits_branch; // @[operandCollector.scala 393:21]
  wire  Demux_io_out_1_bits_simt_stack; // @[operandCollector.scala 393:21]
  wire  Demux_io_out_1_bits_simt_stack_op; // @[operandCollector.scala 393:21]
  wire  Demux_io_out_1_bits_barrier; // @[operandCollector.scala 393:21]
  wire [1:0] Demux_io_out_1_bits_csr; // @[operandCollector.scala 393:21]
  wire  Demux_io_out_1_bits_reverse; // @[operandCollector.scala 393:21]
  wire [1:0] Demux_io_out_1_bits_sel_alu2; // @[operandCollector.scala 393:21]
  wire [1:0] Demux_io_out_1_bits_sel_alu1; // @[operandCollector.scala 393:21]
  wire  Demux_io_out_1_bits_isvec; // @[operandCollector.scala 393:21]
  wire [1:0] Demux_io_out_1_bits_sel_alu3; // @[operandCollector.scala 393:21]
  wire  Demux_io_out_1_bits_mask; // @[operandCollector.scala 393:21]
  wire [2:0] Demux_io_out_1_bits_sel_imm; // @[operandCollector.scala 393:21]
  wire [1:0] Demux_io_out_1_bits_mem_whb; // @[operandCollector.scala 393:21]
  wire  Demux_io_out_1_bits_mem_unsigned; // @[operandCollector.scala 393:21]
  wire [5:0] Demux_io_out_1_bits_alu_fn; // @[operandCollector.scala 393:21]
  wire  Demux_io_out_1_bits_mem; // @[operandCollector.scala 393:21]
  wire  Demux_io_out_1_bits_mul; // @[operandCollector.scala 393:21]
  wire [1:0] Demux_io_out_1_bits_mem_cmd; // @[operandCollector.scala 393:21]
  wire [1:0] Demux_io_out_1_bits_mop; // @[operandCollector.scala 393:21]
  wire [4:0] Demux_io_out_1_bits_reg_idx1; // @[operandCollector.scala 393:21]
  wire [4:0] Demux_io_out_1_bits_reg_idx2; // @[operandCollector.scala 393:21]
  wire [4:0] Demux_io_out_1_bits_reg_idx3; // @[operandCollector.scala 393:21]
  wire [4:0] Demux_io_out_1_bits_reg_idxw; // @[operandCollector.scala 393:21]
  wire  Demux_io_out_1_bits_wfd; // @[operandCollector.scala 393:21]
  wire  Demux_io_out_1_bits_fence; // @[operandCollector.scala 393:21]
  wire  Demux_io_out_1_bits_sfu; // @[operandCollector.scala 393:21]
  wire  Demux_io_out_1_bits_readmask; // @[operandCollector.scala 393:21]
  wire  Demux_io_out_1_bits_writemask; // @[operandCollector.scala 393:21]
  wire  Demux_io_out_1_bits_wxd; // @[operandCollector.scala 393:21]
  wire [31:0] Demux_io_out_1_bits_pc; // @[operandCollector.scala 393:21]
  wire [31:0] Demux_io_out_1_bits_spike_info_pc; // @[operandCollector.scala 393:21]
  wire [31:0] Demux_io_out_1_bits_spike_info_inst; // @[operandCollector.scala 393:21]
  wire  Demux_io_out_2_ready; // @[operandCollector.scala 393:21]
  wire  Demux_io_out_2_valid; // @[operandCollector.scala 393:21]
  wire [31:0] Demux_io_out_2_bits_inst; // @[operandCollector.scala 393:21]
  wire [1:0] Demux_io_out_2_bits_wid; // @[operandCollector.scala 393:21]
  wire  Demux_io_out_2_bits_fp; // @[operandCollector.scala 393:21]
  wire [1:0] Demux_io_out_2_bits_branch; // @[operandCollector.scala 393:21]
  wire  Demux_io_out_2_bits_simt_stack; // @[operandCollector.scala 393:21]
  wire  Demux_io_out_2_bits_simt_stack_op; // @[operandCollector.scala 393:21]
  wire  Demux_io_out_2_bits_barrier; // @[operandCollector.scala 393:21]
  wire [1:0] Demux_io_out_2_bits_csr; // @[operandCollector.scala 393:21]
  wire  Demux_io_out_2_bits_reverse; // @[operandCollector.scala 393:21]
  wire [1:0] Demux_io_out_2_bits_sel_alu2; // @[operandCollector.scala 393:21]
  wire [1:0] Demux_io_out_2_bits_sel_alu1; // @[operandCollector.scala 393:21]
  wire  Demux_io_out_2_bits_isvec; // @[operandCollector.scala 393:21]
  wire [1:0] Demux_io_out_2_bits_sel_alu3; // @[operandCollector.scala 393:21]
  wire  Demux_io_out_2_bits_mask; // @[operandCollector.scala 393:21]
  wire [2:0] Demux_io_out_2_bits_sel_imm; // @[operandCollector.scala 393:21]
  wire [1:0] Demux_io_out_2_bits_mem_whb; // @[operandCollector.scala 393:21]
  wire  Demux_io_out_2_bits_mem_unsigned; // @[operandCollector.scala 393:21]
  wire [5:0] Demux_io_out_2_bits_alu_fn; // @[operandCollector.scala 393:21]
  wire  Demux_io_out_2_bits_mem; // @[operandCollector.scala 393:21]
  wire  Demux_io_out_2_bits_mul; // @[operandCollector.scala 393:21]
  wire [1:0] Demux_io_out_2_bits_mem_cmd; // @[operandCollector.scala 393:21]
  wire [1:0] Demux_io_out_2_bits_mop; // @[operandCollector.scala 393:21]
  wire [4:0] Demux_io_out_2_bits_reg_idx1; // @[operandCollector.scala 393:21]
  wire [4:0] Demux_io_out_2_bits_reg_idx2; // @[operandCollector.scala 393:21]
  wire [4:0] Demux_io_out_2_bits_reg_idx3; // @[operandCollector.scala 393:21]
  wire [4:0] Demux_io_out_2_bits_reg_idxw; // @[operandCollector.scala 393:21]
  wire  Demux_io_out_2_bits_wfd; // @[operandCollector.scala 393:21]
  wire  Demux_io_out_2_bits_fence; // @[operandCollector.scala 393:21]
  wire  Demux_io_out_2_bits_sfu; // @[operandCollector.scala 393:21]
  wire  Demux_io_out_2_bits_readmask; // @[operandCollector.scala 393:21]
  wire  Demux_io_out_2_bits_writemask; // @[operandCollector.scala 393:21]
  wire  Demux_io_out_2_bits_wxd; // @[operandCollector.scala 393:21]
  wire [31:0] Demux_io_out_2_bits_pc; // @[operandCollector.scala 393:21]
  wire [31:0] Demux_io_out_2_bits_spike_info_pc; // @[operandCollector.scala 393:21]
  wire [31:0] Demux_io_out_2_bits_spike_info_inst; // @[operandCollector.scala 393:21]
  wire  Demux_io_out_3_ready; // @[operandCollector.scala 393:21]
  wire  Demux_io_out_3_valid; // @[operandCollector.scala 393:21]
  wire [31:0] Demux_io_out_3_bits_inst; // @[operandCollector.scala 393:21]
  wire [1:0] Demux_io_out_3_bits_wid; // @[operandCollector.scala 393:21]
  wire  Demux_io_out_3_bits_fp; // @[operandCollector.scala 393:21]
  wire [1:0] Demux_io_out_3_bits_branch; // @[operandCollector.scala 393:21]
  wire  Demux_io_out_3_bits_simt_stack; // @[operandCollector.scala 393:21]
  wire  Demux_io_out_3_bits_simt_stack_op; // @[operandCollector.scala 393:21]
  wire  Demux_io_out_3_bits_barrier; // @[operandCollector.scala 393:21]
  wire [1:0] Demux_io_out_3_bits_csr; // @[operandCollector.scala 393:21]
  wire  Demux_io_out_3_bits_reverse; // @[operandCollector.scala 393:21]
  wire [1:0] Demux_io_out_3_bits_sel_alu2; // @[operandCollector.scala 393:21]
  wire [1:0] Demux_io_out_3_bits_sel_alu1; // @[operandCollector.scala 393:21]
  wire  Demux_io_out_3_bits_isvec; // @[operandCollector.scala 393:21]
  wire [1:0] Demux_io_out_3_bits_sel_alu3; // @[operandCollector.scala 393:21]
  wire  Demux_io_out_3_bits_mask; // @[operandCollector.scala 393:21]
  wire [2:0] Demux_io_out_3_bits_sel_imm; // @[operandCollector.scala 393:21]
  wire [1:0] Demux_io_out_3_bits_mem_whb; // @[operandCollector.scala 393:21]
  wire  Demux_io_out_3_bits_mem_unsigned; // @[operandCollector.scala 393:21]
  wire [5:0] Demux_io_out_3_bits_alu_fn; // @[operandCollector.scala 393:21]
  wire  Demux_io_out_3_bits_mem; // @[operandCollector.scala 393:21]
  wire  Demux_io_out_3_bits_mul; // @[operandCollector.scala 393:21]
  wire [1:0] Demux_io_out_3_bits_mem_cmd; // @[operandCollector.scala 393:21]
  wire [1:0] Demux_io_out_3_bits_mop; // @[operandCollector.scala 393:21]
  wire [4:0] Demux_io_out_3_bits_reg_idx1; // @[operandCollector.scala 393:21]
  wire [4:0] Demux_io_out_3_bits_reg_idx2; // @[operandCollector.scala 393:21]
  wire [4:0] Demux_io_out_3_bits_reg_idx3; // @[operandCollector.scala 393:21]
  wire [4:0] Demux_io_out_3_bits_reg_idxw; // @[operandCollector.scala 393:21]
  wire  Demux_io_out_3_bits_wfd; // @[operandCollector.scala 393:21]
  wire  Demux_io_out_3_bits_fence; // @[operandCollector.scala 393:21]
  wire  Demux_io_out_3_bits_sfu; // @[operandCollector.scala 393:21]
  wire  Demux_io_out_3_bits_readmask; // @[operandCollector.scala 393:21]
  wire  Demux_io_out_3_bits_writemask; // @[operandCollector.scala 393:21]
  wire  Demux_io_out_3_bits_wxd; // @[operandCollector.scala 393:21]
  wire [31:0] Demux_io_out_3_bits_pc; // @[operandCollector.scala 393:21]
  wire [31:0] Demux_io_out_3_bits_spike_info_pc; // @[operandCollector.scala 393:21]
  wire [31:0] Demux_io_out_3_bits_spike_info_inst; // @[operandCollector.scala 393:21]
  wire  issueArbiter_io_in_0_ready; // @[operandCollector.scala 466:28]
  wire  issueArbiter_io_in_0_valid; // @[operandCollector.scala 466:28]
  wire [31:0] issueArbiter_io_in_0_bits_alu_src1_0; // @[operandCollector.scala 466:28]
  wire [31:0] issueArbiter_io_in_0_bits_alu_src1_1; // @[operandCollector.scala 466:28]
  wire [31:0] issueArbiter_io_in_0_bits_alu_src1_2; // @[operandCollector.scala 466:28]
  wire [31:0] issueArbiter_io_in_0_bits_alu_src1_3; // @[operandCollector.scala 466:28]
  wire [31:0] issueArbiter_io_in_0_bits_alu_src2_0; // @[operandCollector.scala 466:28]
  wire [31:0] issueArbiter_io_in_0_bits_alu_src2_1; // @[operandCollector.scala 466:28]
  wire [31:0] issueArbiter_io_in_0_bits_alu_src2_2; // @[operandCollector.scala 466:28]
  wire [31:0] issueArbiter_io_in_0_bits_alu_src2_3; // @[operandCollector.scala 466:28]
  wire [31:0] issueArbiter_io_in_0_bits_alu_src3_0; // @[operandCollector.scala 466:28]
  wire [31:0] issueArbiter_io_in_0_bits_alu_src3_1; // @[operandCollector.scala 466:28]
  wire [31:0] issueArbiter_io_in_0_bits_alu_src3_2; // @[operandCollector.scala 466:28]
  wire [31:0] issueArbiter_io_in_0_bits_alu_src3_3; // @[operandCollector.scala 466:28]
  wire  issueArbiter_io_in_0_bits_mask_0; // @[operandCollector.scala 466:28]
  wire  issueArbiter_io_in_0_bits_mask_1; // @[operandCollector.scala 466:28]
  wire  issueArbiter_io_in_0_bits_mask_2; // @[operandCollector.scala 466:28]
  wire  issueArbiter_io_in_0_bits_mask_3; // @[operandCollector.scala 466:28]
  wire [31:0] issueArbiter_io_in_0_bits_control_inst; // @[operandCollector.scala 466:28]
  wire [1:0] issueArbiter_io_in_0_bits_control_wid; // @[operandCollector.scala 466:28]
  wire  issueArbiter_io_in_0_bits_control_fp; // @[operandCollector.scala 466:28]
  wire [1:0] issueArbiter_io_in_0_bits_control_branch; // @[operandCollector.scala 466:28]
  wire  issueArbiter_io_in_0_bits_control_simt_stack; // @[operandCollector.scala 466:28]
  wire  issueArbiter_io_in_0_bits_control_simt_stack_op; // @[operandCollector.scala 466:28]
  wire  issueArbiter_io_in_0_bits_control_barrier; // @[operandCollector.scala 466:28]
  wire [1:0] issueArbiter_io_in_0_bits_control_csr; // @[operandCollector.scala 466:28]
  wire  issueArbiter_io_in_0_bits_control_reverse; // @[operandCollector.scala 466:28]
  wire [1:0] issueArbiter_io_in_0_bits_control_sel_alu2; // @[operandCollector.scala 466:28]
  wire [1:0] issueArbiter_io_in_0_bits_control_sel_alu1; // @[operandCollector.scala 466:28]
  wire  issueArbiter_io_in_0_bits_control_isvec; // @[operandCollector.scala 466:28]
  wire [1:0] issueArbiter_io_in_0_bits_control_sel_alu3; // @[operandCollector.scala 466:28]
  wire  issueArbiter_io_in_0_bits_control_mask; // @[operandCollector.scala 466:28]
  wire [2:0] issueArbiter_io_in_0_bits_control_sel_imm; // @[operandCollector.scala 466:28]
  wire [1:0] issueArbiter_io_in_0_bits_control_mem_whb; // @[operandCollector.scala 466:28]
  wire  issueArbiter_io_in_0_bits_control_mem_unsigned; // @[operandCollector.scala 466:28]
  wire [5:0] issueArbiter_io_in_0_bits_control_alu_fn; // @[operandCollector.scala 466:28]
  wire  issueArbiter_io_in_0_bits_control_mem; // @[operandCollector.scala 466:28]
  wire  issueArbiter_io_in_0_bits_control_mul; // @[operandCollector.scala 466:28]
  wire [1:0] issueArbiter_io_in_0_bits_control_mem_cmd; // @[operandCollector.scala 466:28]
  wire [1:0] issueArbiter_io_in_0_bits_control_mop; // @[operandCollector.scala 466:28]
  wire [4:0] issueArbiter_io_in_0_bits_control_reg_idx1; // @[operandCollector.scala 466:28]
  wire [4:0] issueArbiter_io_in_0_bits_control_reg_idx2; // @[operandCollector.scala 466:28]
  wire [4:0] issueArbiter_io_in_0_bits_control_reg_idx3; // @[operandCollector.scala 466:28]
  wire [4:0] issueArbiter_io_in_0_bits_control_reg_idxw; // @[operandCollector.scala 466:28]
  wire  issueArbiter_io_in_0_bits_control_wfd; // @[operandCollector.scala 466:28]
  wire  issueArbiter_io_in_0_bits_control_fence; // @[operandCollector.scala 466:28]
  wire  issueArbiter_io_in_0_bits_control_sfu; // @[operandCollector.scala 466:28]
  wire  issueArbiter_io_in_0_bits_control_readmask; // @[operandCollector.scala 466:28]
  wire  issueArbiter_io_in_0_bits_control_writemask; // @[operandCollector.scala 466:28]
  wire  issueArbiter_io_in_0_bits_control_wxd; // @[operandCollector.scala 466:28]
  wire [31:0] issueArbiter_io_in_0_bits_control_pc; // @[operandCollector.scala 466:28]
  wire [31:0] issueArbiter_io_in_0_bits_control_spike_info_pc; // @[operandCollector.scala 466:28]
  wire [31:0] issueArbiter_io_in_0_bits_control_spike_info_inst; // @[operandCollector.scala 466:28]
  wire  issueArbiter_io_in_1_ready; // @[operandCollector.scala 466:28]
  wire  issueArbiter_io_in_1_valid; // @[operandCollector.scala 466:28]
  wire [31:0] issueArbiter_io_in_1_bits_alu_src1_0; // @[operandCollector.scala 466:28]
  wire [31:0] issueArbiter_io_in_1_bits_alu_src1_1; // @[operandCollector.scala 466:28]
  wire [31:0] issueArbiter_io_in_1_bits_alu_src1_2; // @[operandCollector.scala 466:28]
  wire [31:0] issueArbiter_io_in_1_bits_alu_src1_3; // @[operandCollector.scala 466:28]
  wire [31:0] issueArbiter_io_in_1_bits_alu_src2_0; // @[operandCollector.scala 466:28]
  wire [31:0] issueArbiter_io_in_1_bits_alu_src2_1; // @[operandCollector.scala 466:28]
  wire [31:0] issueArbiter_io_in_1_bits_alu_src2_2; // @[operandCollector.scala 466:28]
  wire [31:0] issueArbiter_io_in_1_bits_alu_src2_3; // @[operandCollector.scala 466:28]
  wire [31:0] issueArbiter_io_in_1_bits_alu_src3_0; // @[operandCollector.scala 466:28]
  wire [31:0] issueArbiter_io_in_1_bits_alu_src3_1; // @[operandCollector.scala 466:28]
  wire [31:0] issueArbiter_io_in_1_bits_alu_src3_2; // @[operandCollector.scala 466:28]
  wire [31:0] issueArbiter_io_in_1_bits_alu_src3_3; // @[operandCollector.scala 466:28]
  wire  issueArbiter_io_in_1_bits_mask_0; // @[operandCollector.scala 466:28]
  wire  issueArbiter_io_in_1_bits_mask_1; // @[operandCollector.scala 466:28]
  wire  issueArbiter_io_in_1_bits_mask_2; // @[operandCollector.scala 466:28]
  wire  issueArbiter_io_in_1_bits_mask_3; // @[operandCollector.scala 466:28]
  wire [31:0] issueArbiter_io_in_1_bits_control_inst; // @[operandCollector.scala 466:28]
  wire [1:0] issueArbiter_io_in_1_bits_control_wid; // @[operandCollector.scala 466:28]
  wire  issueArbiter_io_in_1_bits_control_fp; // @[operandCollector.scala 466:28]
  wire [1:0] issueArbiter_io_in_1_bits_control_branch; // @[operandCollector.scala 466:28]
  wire  issueArbiter_io_in_1_bits_control_simt_stack; // @[operandCollector.scala 466:28]
  wire  issueArbiter_io_in_1_bits_control_simt_stack_op; // @[operandCollector.scala 466:28]
  wire  issueArbiter_io_in_1_bits_control_barrier; // @[operandCollector.scala 466:28]
  wire [1:0] issueArbiter_io_in_1_bits_control_csr; // @[operandCollector.scala 466:28]
  wire  issueArbiter_io_in_1_bits_control_reverse; // @[operandCollector.scala 466:28]
  wire [1:0] issueArbiter_io_in_1_bits_control_sel_alu2; // @[operandCollector.scala 466:28]
  wire [1:0] issueArbiter_io_in_1_bits_control_sel_alu1; // @[operandCollector.scala 466:28]
  wire  issueArbiter_io_in_1_bits_control_isvec; // @[operandCollector.scala 466:28]
  wire [1:0] issueArbiter_io_in_1_bits_control_sel_alu3; // @[operandCollector.scala 466:28]
  wire  issueArbiter_io_in_1_bits_control_mask; // @[operandCollector.scala 466:28]
  wire [2:0] issueArbiter_io_in_1_bits_control_sel_imm; // @[operandCollector.scala 466:28]
  wire [1:0] issueArbiter_io_in_1_bits_control_mem_whb; // @[operandCollector.scala 466:28]
  wire  issueArbiter_io_in_1_bits_control_mem_unsigned; // @[operandCollector.scala 466:28]
  wire [5:0] issueArbiter_io_in_1_bits_control_alu_fn; // @[operandCollector.scala 466:28]
  wire  issueArbiter_io_in_1_bits_control_mem; // @[operandCollector.scala 466:28]
  wire  issueArbiter_io_in_1_bits_control_mul; // @[operandCollector.scala 466:28]
  wire [1:0] issueArbiter_io_in_1_bits_control_mem_cmd; // @[operandCollector.scala 466:28]
  wire [1:0] issueArbiter_io_in_1_bits_control_mop; // @[operandCollector.scala 466:28]
  wire [4:0] issueArbiter_io_in_1_bits_control_reg_idx1; // @[operandCollector.scala 466:28]
  wire [4:0] issueArbiter_io_in_1_bits_control_reg_idx2; // @[operandCollector.scala 466:28]
  wire [4:0] issueArbiter_io_in_1_bits_control_reg_idx3; // @[operandCollector.scala 466:28]
  wire [4:0] issueArbiter_io_in_1_bits_control_reg_idxw; // @[operandCollector.scala 466:28]
  wire  issueArbiter_io_in_1_bits_control_wfd; // @[operandCollector.scala 466:28]
  wire  issueArbiter_io_in_1_bits_control_fence; // @[operandCollector.scala 466:28]
  wire  issueArbiter_io_in_1_bits_control_sfu; // @[operandCollector.scala 466:28]
  wire  issueArbiter_io_in_1_bits_control_readmask; // @[operandCollector.scala 466:28]
  wire  issueArbiter_io_in_1_bits_control_writemask; // @[operandCollector.scala 466:28]
  wire  issueArbiter_io_in_1_bits_control_wxd; // @[operandCollector.scala 466:28]
  wire [31:0] issueArbiter_io_in_1_bits_control_pc; // @[operandCollector.scala 466:28]
  wire [31:0] issueArbiter_io_in_1_bits_control_spike_info_pc; // @[operandCollector.scala 466:28]
  wire [31:0] issueArbiter_io_in_1_bits_control_spike_info_inst; // @[operandCollector.scala 466:28]
  wire  issueArbiter_io_in_2_ready; // @[operandCollector.scala 466:28]
  wire  issueArbiter_io_in_2_valid; // @[operandCollector.scala 466:28]
  wire [31:0] issueArbiter_io_in_2_bits_alu_src1_0; // @[operandCollector.scala 466:28]
  wire [31:0] issueArbiter_io_in_2_bits_alu_src1_1; // @[operandCollector.scala 466:28]
  wire [31:0] issueArbiter_io_in_2_bits_alu_src1_2; // @[operandCollector.scala 466:28]
  wire [31:0] issueArbiter_io_in_2_bits_alu_src1_3; // @[operandCollector.scala 466:28]
  wire [31:0] issueArbiter_io_in_2_bits_alu_src2_0; // @[operandCollector.scala 466:28]
  wire [31:0] issueArbiter_io_in_2_bits_alu_src2_1; // @[operandCollector.scala 466:28]
  wire [31:0] issueArbiter_io_in_2_bits_alu_src2_2; // @[operandCollector.scala 466:28]
  wire [31:0] issueArbiter_io_in_2_bits_alu_src2_3; // @[operandCollector.scala 466:28]
  wire [31:0] issueArbiter_io_in_2_bits_alu_src3_0; // @[operandCollector.scala 466:28]
  wire [31:0] issueArbiter_io_in_2_bits_alu_src3_1; // @[operandCollector.scala 466:28]
  wire [31:0] issueArbiter_io_in_2_bits_alu_src3_2; // @[operandCollector.scala 466:28]
  wire [31:0] issueArbiter_io_in_2_bits_alu_src3_3; // @[operandCollector.scala 466:28]
  wire  issueArbiter_io_in_2_bits_mask_0; // @[operandCollector.scala 466:28]
  wire  issueArbiter_io_in_2_bits_mask_1; // @[operandCollector.scala 466:28]
  wire  issueArbiter_io_in_2_bits_mask_2; // @[operandCollector.scala 466:28]
  wire  issueArbiter_io_in_2_bits_mask_3; // @[operandCollector.scala 466:28]
  wire [31:0] issueArbiter_io_in_2_bits_control_inst; // @[operandCollector.scala 466:28]
  wire [1:0] issueArbiter_io_in_2_bits_control_wid; // @[operandCollector.scala 466:28]
  wire  issueArbiter_io_in_2_bits_control_fp; // @[operandCollector.scala 466:28]
  wire [1:0] issueArbiter_io_in_2_bits_control_branch; // @[operandCollector.scala 466:28]
  wire  issueArbiter_io_in_2_bits_control_simt_stack; // @[operandCollector.scala 466:28]
  wire  issueArbiter_io_in_2_bits_control_simt_stack_op; // @[operandCollector.scala 466:28]
  wire  issueArbiter_io_in_2_bits_control_barrier; // @[operandCollector.scala 466:28]
  wire [1:0] issueArbiter_io_in_2_bits_control_csr; // @[operandCollector.scala 466:28]
  wire  issueArbiter_io_in_2_bits_control_reverse; // @[operandCollector.scala 466:28]
  wire [1:0] issueArbiter_io_in_2_bits_control_sel_alu2; // @[operandCollector.scala 466:28]
  wire [1:0] issueArbiter_io_in_2_bits_control_sel_alu1; // @[operandCollector.scala 466:28]
  wire  issueArbiter_io_in_2_bits_control_isvec; // @[operandCollector.scala 466:28]
  wire [1:0] issueArbiter_io_in_2_bits_control_sel_alu3; // @[operandCollector.scala 466:28]
  wire  issueArbiter_io_in_2_bits_control_mask; // @[operandCollector.scala 466:28]
  wire [2:0] issueArbiter_io_in_2_bits_control_sel_imm; // @[operandCollector.scala 466:28]
  wire [1:0] issueArbiter_io_in_2_bits_control_mem_whb; // @[operandCollector.scala 466:28]
  wire  issueArbiter_io_in_2_bits_control_mem_unsigned; // @[operandCollector.scala 466:28]
  wire [5:0] issueArbiter_io_in_2_bits_control_alu_fn; // @[operandCollector.scala 466:28]
  wire  issueArbiter_io_in_2_bits_control_mem; // @[operandCollector.scala 466:28]
  wire  issueArbiter_io_in_2_bits_control_mul; // @[operandCollector.scala 466:28]
  wire [1:0] issueArbiter_io_in_2_bits_control_mem_cmd; // @[operandCollector.scala 466:28]
  wire [1:0] issueArbiter_io_in_2_bits_control_mop; // @[operandCollector.scala 466:28]
  wire [4:0] issueArbiter_io_in_2_bits_control_reg_idx1; // @[operandCollector.scala 466:28]
  wire [4:0] issueArbiter_io_in_2_bits_control_reg_idx2; // @[operandCollector.scala 466:28]
  wire [4:0] issueArbiter_io_in_2_bits_control_reg_idx3; // @[operandCollector.scala 466:28]
  wire [4:0] issueArbiter_io_in_2_bits_control_reg_idxw; // @[operandCollector.scala 466:28]
  wire  issueArbiter_io_in_2_bits_control_wfd; // @[operandCollector.scala 466:28]
  wire  issueArbiter_io_in_2_bits_control_fence; // @[operandCollector.scala 466:28]
  wire  issueArbiter_io_in_2_bits_control_sfu; // @[operandCollector.scala 466:28]
  wire  issueArbiter_io_in_2_bits_control_readmask; // @[operandCollector.scala 466:28]
  wire  issueArbiter_io_in_2_bits_control_writemask; // @[operandCollector.scala 466:28]
  wire  issueArbiter_io_in_2_bits_control_wxd; // @[operandCollector.scala 466:28]
  wire [31:0] issueArbiter_io_in_2_bits_control_pc; // @[operandCollector.scala 466:28]
  wire [31:0] issueArbiter_io_in_2_bits_control_spike_info_pc; // @[operandCollector.scala 466:28]
  wire [31:0] issueArbiter_io_in_2_bits_control_spike_info_inst; // @[operandCollector.scala 466:28]
  wire  issueArbiter_io_in_3_ready; // @[operandCollector.scala 466:28]
  wire  issueArbiter_io_in_3_valid; // @[operandCollector.scala 466:28]
  wire [31:0] issueArbiter_io_in_3_bits_alu_src1_0; // @[operandCollector.scala 466:28]
  wire [31:0] issueArbiter_io_in_3_bits_alu_src1_1; // @[operandCollector.scala 466:28]
  wire [31:0] issueArbiter_io_in_3_bits_alu_src1_2; // @[operandCollector.scala 466:28]
  wire [31:0] issueArbiter_io_in_3_bits_alu_src1_3; // @[operandCollector.scala 466:28]
  wire [31:0] issueArbiter_io_in_3_bits_alu_src2_0; // @[operandCollector.scala 466:28]
  wire [31:0] issueArbiter_io_in_3_bits_alu_src2_1; // @[operandCollector.scala 466:28]
  wire [31:0] issueArbiter_io_in_3_bits_alu_src2_2; // @[operandCollector.scala 466:28]
  wire [31:0] issueArbiter_io_in_3_bits_alu_src2_3; // @[operandCollector.scala 466:28]
  wire [31:0] issueArbiter_io_in_3_bits_alu_src3_0; // @[operandCollector.scala 466:28]
  wire [31:0] issueArbiter_io_in_3_bits_alu_src3_1; // @[operandCollector.scala 466:28]
  wire [31:0] issueArbiter_io_in_3_bits_alu_src3_2; // @[operandCollector.scala 466:28]
  wire [31:0] issueArbiter_io_in_3_bits_alu_src3_3; // @[operandCollector.scala 466:28]
  wire  issueArbiter_io_in_3_bits_mask_0; // @[operandCollector.scala 466:28]
  wire  issueArbiter_io_in_3_bits_mask_1; // @[operandCollector.scala 466:28]
  wire  issueArbiter_io_in_3_bits_mask_2; // @[operandCollector.scala 466:28]
  wire  issueArbiter_io_in_3_bits_mask_3; // @[operandCollector.scala 466:28]
  wire [31:0] issueArbiter_io_in_3_bits_control_inst; // @[operandCollector.scala 466:28]
  wire [1:0] issueArbiter_io_in_3_bits_control_wid; // @[operandCollector.scala 466:28]
  wire  issueArbiter_io_in_3_bits_control_fp; // @[operandCollector.scala 466:28]
  wire [1:0] issueArbiter_io_in_3_bits_control_branch; // @[operandCollector.scala 466:28]
  wire  issueArbiter_io_in_3_bits_control_simt_stack; // @[operandCollector.scala 466:28]
  wire  issueArbiter_io_in_3_bits_control_simt_stack_op; // @[operandCollector.scala 466:28]
  wire  issueArbiter_io_in_3_bits_control_barrier; // @[operandCollector.scala 466:28]
  wire [1:0] issueArbiter_io_in_3_bits_control_csr; // @[operandCollector.scala 466:28]
  wire  issueArbiter_io_in_3_bits_control_reverse; // @[operandCollector.scala 466:28]
  wire [1:0] issueArbiter_io_in_3_bits_control_sel_alu2; // @[operandCollector.scala 466:28]
  wire [1:0] issueArbiter_io_in_3_bits_control_sel_alu1; // @[operandCollector.scala 466:28]
  wire  issueArbiter_io_in_3_bits_control_isvec; // @[operandCollector.scala 466:28]
  wire [1:0] issueArbiter_io_in_3_bits_control_sel_alu3; // @[operandCollector.scala 466:28]
  wire  issueArbiter_io_in_3_bits_control_mask; // @[operandCollector.scala 466:28]
  wire [2:0] issueArbiter_io_in_3_bits_control_sel_imm; // @[operandCollector.scala 466:28]
  wire [1:0] issueArbiter_io_in_3_bits_control_mem_whb; // @[operandCollector.scala 466:28]
  wire  issueArbiter_io_in_3_bits_control_mem_unsigned; // @[operandCollector.scala 466:28]
  wire [5:0] issueArbiter_io_in_3_bits_control_alu_fn; // @[operandCollector.scala 466:28]
  wire  issueArbiter_io_in_3_bits_control_mem; // @[operandCollector.scala 466:28]
  wire  issueArbiter_io_in_3_bits_control_mul; // @[operandCollector.scala 466:28]
  wire [1:0] issueArbiter_io_in_3_bits_control_mem_cmd; // @[operandCollector.scala 466:28]
  wire [1:0] issueArbiter_io_in_3_bits_control_mop; // @[operandCollector.scala 466:28]
  wire [4:0] issueArbiter_io_in_3_bits_control_reg_idx1; // @[operandCollector.scala 466:28]
  wire [4:0] issueArbiter_io_in_3_bits_control_reg_idx2; // @[operandCollector.scala 466:28]
  wire [4:0] issueArbiter_io_in_3_bits_control_reg_idx3; // @[operandCollector.scala 466:28]
  wire [4:0] issueArbiter_io_in_3_bits_control_reg_idxw; // @[operandCollector.scala 466:28]
  wire  issueArbiter_io_in_3_bits_control_wfd; // @[operandCollector.scala 466:28]
  wire  issueArbiter_io_in_3_bits_control_fence; // @[operandCollector.scala 466:28]
  wire  issueArbiter_io_in_3_bits_control_sfu; // @[operandCollector.scala 466:28]
  wire  issueArbiter_io_in_3_bits_control_readmask; // @[operandCollector.scala 466:28]
  wire  issueArbiter_io_in_3_bits_control_writemask; // @[operandCollector.scala 466:28]
  wire  issueArbiter_io_in_3_bits_control_wxd; // @[operandCollector.scala 466:28]
  wire [31:0] issueArbiter_io_in_3_bits_control_pc; // @[operandCollector.scala 466:28]
  wire [31:0] issueArbiter_io_in_3_bits_control_spike_info_pc; // @[operandCollector.scala 466:28]
  wire [31:0] issueArbiter_io_in_3_bits_control_spike_info_inst; // @[operandCollector.scala 466:28]
  wire  issueArbiter_io_out_ready; // @[operandCollector.scala 466:28]
  wire  issueArbiter_io_out_valid; // @[operandCollector.scala 466:28]
  wire [31:0] issueArbiter_io_out_bits_alu_src1_0; // @[operandCollector.scala 466:28]
  wire [31:0] issueArbiter_io_out_bits_alu_src1_1; // @[operandCollector.scala 466:28]
  wire [31:0] issueArbiter_io_out_bits_alu_src1_2; // @[operandCollector.scala 466:28]
  wire [31:0] issueArbiter_io_out_bits_alu_src1_3; // @[operandCollector.scala 466:28]
  wire [31:0] issueArbiter_io_out_bits_alu_src2_0; // @[operandCollector.scala 466:28]
  wire [31:0] issueArbiter_io_out_bits_alu_src2_1; // @[operandCollector.scala 466:28]
  wire [31:0] issueArbiter_io_out_bits_alu_src2_2; // @[operandCollector.scala 466:28]
  wire [31:0] issueArbiter_io_out_bits_alu_src2_3; // @[operandCollector.scala 466:28]
  wire [31:0] issueArbiter_io_out_bits_alu_src3_0; // @[operandCollector.scala 466:28]
  wire [31:0] issueArbiter_io_out_bits_alu_src3_1; // @[operandCollector.scala 466:28]
  wire [31:0] issueArbiter_io_out_bits_alu_src3_2; // @[operandCollector.scala 466:28]
  wire [31:0] issueArbiter_io_out_bits_alu_src3_3; // @[operandCollector.scala 466:28]
  wire  issueArbiter_io_out_bits_mask_0; // @[operandCollector.scala 466:28]
  wire  issueArbiter_io_out_bits_mask_1; // @[operandCollector.scala 466:28]
  wire  issueArbiter_io_out_bits_mask_2; // @[operandCollector.scala 466:28]
  wire  issueArbiter_io_out_bits_mask_3; // @[operandCollector.scala 466:28]
  wire [31:0] issueArbiter_io_out_bits_control_inst; // @[operandCollector.scala 466:28]
  wire [1:0] issueArbiter_io_out_bits_control_wid; // @[operandCollector.scala 466:28]
  wire  issueArbiter_io_out_bits_control_fp; // @[operandCollector.scala 466:28]
  wire [1:0] issueArbiter_io_out_bits_control_branch; // @[operandCollector.scala 466:28]
  wire  issueArbiter_io_out_bits_control_simt_stack; // @[operandCollector.scala 466:28]
  wire  issueArbiter_io_out_bits_control_simt_stack_op; // @[operandCollector.scala 466:28]
  wire  issueArbiter_io_out_bits_control_barrier; // @[operandCollector.scala 466:28]
  wire [1:0] issueArbiter_io_out_bits_control_csr; // @[operandCollector.scala 466:28]
  wire  issueArbiter_io_out_bits_control_reverse; // @[operandCollector.scala 466:28]
  wire [1:0] issueArbiter_io_out_bits_control_sel_alu2; // @[operandCollector.scala 466:28]
  wire [1:0] issueArbiter_io_out_bits_control_sel_alu1; // @[operandCollector.scala 466:28]
  wire  issueArbiter_io_out_bits_control_isvec; // @[operandCollector.scala 466:28]
  wire [1:0] issueArbiter_io_out_bits_control_sel_alu3; // @[operandCollector.scala 466:28]
  wire  issueArbiter_io_out_bits_control_mask; // @[operandCollector.scala 466:28]
  wire [2:0] issueArbiter_io_out_bits_control_sel_imm; // @[operandCollector.scala 466:28]
  wire [1:0] issueArbiter_io_out_bits_control_mem_whb; // @[operandCollector.scala 466:28]
  wire  issueArbiter_io_out_bits_control_mem_unsigned; // @[operandCollector.scala 466:28]
  wire [5:0] issueArbiter_io_out_bits_control_alu_fn; // @[operandCollector.scala 466:28]
  wire  issueArbiter_io_out_bits_control_mem; // @[operandCollector.scala 466:28]
  wire  issueArbiter_io_out_bits_control_mul; // @[operandCollector.scala 466:28]
  wire [1:0] issueArbiter_io_out_bits_control_mem_cmd; // @[operandCollector.scala 466:28]
  wire [1:0] issueArbiter_io_out_bits_control_mop; // @[operandCollector.scala 466:28]
  wire [4:0] issueArbiter_io_out_bits_control_reg_idx1; // @[operandCollector.scala 466:28]
  wire [4:0] issueArbiter_io_out_bits_control_reg_idx2; // @[operandCollector.scala 466:28]
  wire [4:0] issueArbiter_io_out_bits_control_reg_idx3; // @[operandCollector.scala 466:28]
  wire [4:0] issueArbiter_io_out_bits_control_reg_idxw; // @[operandCollector.scala 466:28]
  wire  issueArbiter_io_out_bits_control_wfd; // @[operandCollector.scala 466:28]
  wire  issueArbiter_io_out_bits_control_fence; // @[operandCollector.scala 466:28]
  wire  issueArbiter_io_out_bits_control_sfu; // @[operandCollector.scala 466:28]
  wire  issueArbiter_io_out_bits_control_readmask; // @[operandCollector.scala 466:28]
  wire  issueArbiter_io_out_bits_control_writemask; // @[operandCollector.scala 466:28]
  wire  issueArbiter_io_out_bits_control_wxd; // @[operandCollector.scala 466:28]
  wire [31:0] issueArbiter_io_out_bits_control_pc; // @[operandCollector.scala 466:28]
  wire [31:0] issueArbiter_io_out_bits_control_spike_info_pc; // @[operandCollector.scala 466:28]
  wire [31:0] issueArbiter_io_out_bits_control_spike_info_inst; // @[operandCollector.scala 466:28]
  reg [3:0] REG__0; // @[operandCollector.scala 402:32]
  reg [3:0] REG__1; // @[operandCollector.scala 402:32]
  reg [3:0] REG__2; // @[operandCollector.scala 402:32]
  reg [3:0] REG__3; // @[operandCollector.scala 402:32]
  reg  REG_1_0; // @[operandCollector.scala 403:38]
  reg  REG_1_1; // @[operandCollector.scala 403:38]
  reg  REG_1_2; // @[operandCollector.scala 403:38]
  reg  REG_1_3; // @[operandCollector.scala 403:38]
  reg  REG_2; // @[operandCollector.scala 405:17]
  reg  REG_3; // @[operandCollector.scala 408:23]
  wire [31:0] vectorBank_0_rs_0 = FloatRegFileBank_io_rs_0; // @[operandCollector.scala 390:{27,27}]
  wire [31:0] _GEN_0 = REG_3 ? vectorBank_0_rs_0 : 32'h0; // @[operandCollector.scala 408:72 409:32 412:32]
  wire [31:0] vectorBank_0_rs_1 = FloatRegFileBank_io_rs_1; // @[operandCollector.scala 390:{27,27}]
  wire [31:0] _GEN_1 = REG_3 ? vectorBank_0_rs_1 : 32'h0; // @[operandCollector.scala 408:72 409:32 412:32]
  wire [31:0] vectorBank_0_rs_2 = FloatRegFileBank_io_rs_2; // @[operandCollector.scala 390:{27,27}]
  wire [31:0] _GEN_2 = REG_3 ? vectorBank_0_rs_2 : 32'h0; // @[operandCollector.scala 408:72 409:32 412:32]
  wire [31:0] vectorBank_0_rs_3 = FloatRegFileBank_io_rs_3; // @[operandCollector.scala 390:{27,27}]
  wire [31:0] _GEN_3 = REG_3 ? vectorBank_0_rs_3 : 32'h0; // @[operandCollector.scala 408:72 409:32 412:32]
  wire [31:0] vectorBank_0_v0_0 = FloatRegFileBank_io_v0_0; // @[operandCollector.scala 390:{27,27}]
  wire [31:0] _GEN_4 = REG_3 ? vectorBank_0_v0_0 : 32'h0; // @[operandCollector.scala 408:72 410:32 413:32]
  wire [31:0] scalarBank_0_rs = RegFileBank_io_rs; // @[operandCollector.scala 391:{27,27}]
  reg  REG_4; // @[operandCollector.scala 405:17]
  reg  REG_5; // @[operandCollector.scala 408:23]
  wire [31:0] vectorBank_1_rs_0 = FloatRegFileBank_1_io_rs_0; // @[operandCollector.scala 390:{27,27}]
  wire [31:0] _GEN_16 = REG_5 ? vectorBank_1_rs_0 : 32'h0; // @[operandCollector.scala 408:72 409:32 412:32]
  wire [31:0] vectorBank_1_rs_1 = FloatRegFileBank_1_io_rs_1; // @[operandCollector.scala 390:{27,27}]
  wire [31:0] _GEN_17 = REG_5 ? vectorBank_1_rs_1 : 32'h0; // @[operandCollector.scala 408:72 409:32 412:32]
  wire [31:0] vectorBank_1_rs_2 = FloatRegFileBank_1_io_rs_2; // @[operandCollector.scala 390:{27,27}]
  wire [31:0] _GEN_18 = REG_5 ? vectorBank_1_rs_2 : 32'h0; // @[operandCollector.scala 408:72 409:32 412:32]
  wire [31:0] vectorBank_1_rs_3 = FloatRegFileBank_1_io_rs_3; // @[operandCollector.scala 390:{27,27}]
  wire [31:0] _GEN_19 = REG_5 ? vectorBank_1_rs_3 : 32'h0; // @[operandCollector.scala 408:72 409:32 412:32]
  wire [31:0] vectorBank_1_v0_0 = FloatRegFileBank_1_io_v0_0; // @[operandCollector.scala 390:{27,27}]
  wire [31:0] _GEN_20 = REG_5 ? vectorBank_1_v0_0 : 32'h0; // @[operandCollector.scala 408:72 410:32 413:32]
  wire [31:0] scalarBank_1_rs = RegFileBank_1_io_rs; // @[operandCollector.scala 391:{27,27}]
  reg  REG_6; // @[operandCollector.scala 405:17]
  reg  REG_7; // @[operandCollector.scala 408:23]
  wire [31:0] vectorBank_2_rs_0 = FloatRegFileBank_2_io_rs_0; // @[operandCollector.scala 390:{27,27}]
  wire [31:0] _GEN_32 = REG_7 ? vectorBank_2_rs_0 : 32'h0; // @[operandCollector.scala 408:72 409:32 412:32]
  wire [31:0] vectorBank_2_rs_1 = FloatRegFileBank_2_io_rs_1; // @[operandCollector.scala 390:{27,27}]
  wire [31:0] _GEN_33 = REG_7 ? vectorBank_2_rs_1 : 32'h0; // @[operandCollector.scala 408:72 409:32 412:32]
  wire [31:0] vectorBank_2_rs_2 = FloatRegFileBank_2_io_rs_2; // @[operandCollector.scala 390:{27,27}]
  wire [31:0] _GEN_34 = REG_7 ? vectorBank_2_rs_2 : 32'h0; // @[operandCollector.scala 408:72 409:32 412:32]
  wire [31:0] vectorBank_2_rs_3 = FloatRegFileBank_2_io_rs_3; // @[operandCollector.scala 390:{27,27}]
  wire [31:0] _GEN_35 = REG_7 ? vectorBank_2_rs_3 : 32'h0; // @[operandCollector.scala 408:72 409:32 412:32]
  wire [31:0] vectorBank_2_v0_0 = FloatRegFileBank_2_io_v0_0; // @[operandCollector.scala 390:{27,27}]
  wire [31:0] _GEN_36 = REG_7 ? vectorBank_2_v0_0 : 32'h0; // @[operandCollector.scala 408:72 410:32 413:32]
  wire [31:0] scalarBank_2_rs = RegFileBank_2_io_rs; // @[operandCollector.scala 391:{27,27}]
  reg  REG_8; // @[operandCollector.scala 405:17]
  reg  REG_9; // @[operandCollector.scala 408:23]
  wire [31:0] vectorBank_3_rs_0 = FloatRegFileBank_3_io_rs_0; // @[operandCollector.scala 390:{27,27}]
  wire [31:0] _GEN_48 = REG_9 ? vectorBank_3_rs_0 : 32'h0; // @[operandCollector.scala 408:72 409:32 412:32]
  wire [31:0] vectorBank_3_rs_1 = FloatRegFileBank_3_io_rs_1; // @[operandCollector.scala 390:{27,27}]
  wire [31:0] _GEN_49 = REG_9 ? vectorBank_3_rs_1 : 32'h0; // @[operandCollector.scala 408:72 409:32 412:32]
  wire [31:0] vectorBank_3_rs_2 = FloatRegFileBank_3_io_rs_2; // @[operandCollector.scala 390:{27,27}]
  wire [31:0] _GEN_50 = REG_9 ? vectorBank_3_rs_2 : 32'h0; // @[operandCollector.scala 408:72 409:32 412:32]
  wire [31:0] vectorBank_3_rs_3 = FloatRegFileBank_3_io_rs_3; // @[operandCollector.scala 390:{27,27}]
  wire [31:0] _GEN_51 = REG_9 ? vectorBank_3_rs_3 : 32'h0; // @[operandCollector.scala 408:72 409:32 412:32]
  wire [31:0] vectorBank_3_v0_0 = FloatRegFileBank_3_io_v0_0; // @[operandCollector.scala 390:{27,27}]
  wire [31:0] _GEN_52 = REG_9 ? vectorBank_3_v0_0 : 32'h0; // @[operandCollector.scala 408:72 410:32 413:32]
  wire [31:0] scalarBank_3_rs = RegFileBank_3_io_rs; // @[operandCollector.scala 391:{27,27}]
  wire [4:0] _GEN_72 = {{3'd0}, io_writeVecCtrl_bits_warp_id}; // @[operandCollector.scala 444:57]
  wire [4:0] _wbVecBankId_T_1 = io_writeVecCtrl_bits_reg_idxw + _GEN_72; // @[operandCollector.scala 444:57]
  wire [1:0] _wbVecBankId_T_5 = 5'h2 == _wbVecBankId_T_1 ? 2'h2 : {{1'd0}, 5'h1 == _wbVecBankId_T_1}; // @[Mux.scala 81:58]
  wire [1:0] _wbVecBankId_T_7 = 5'h3 == _wbVecBankId_T_1 ? 2'h3 : _wbVecBankId_T_5; // @[Mux.scala 81:58]
  wire [1:0] _wbVecBankId_T_9 = 5'h4 == _wbVecBankId_T_1 ? 2'h0 : _wbVecBankId_T_7; // @[Mux.scala 81:58]
  wire [1:0] _wbVecBankId_T_11 = 5'h5 == _wbVecBankId_T_1 ? 2'h1 : _wbVecBankId_T_9; // @[Mux.scala 81:58]
  wire [1:0] _wbVecBankId_T_13 = 5'h6 == _wbVecBankId_T_1 ? 2'h2 : _wbVecBankId_T_11; // @[Mux.scala 81:58]
  wire [1:0] _wbVecBankId_T_15 = 5'h7 == _wbVecBankId_T_1 ? 2'h3 : _wbVecBankId_T_13; // @[Mux.scala 81:58]
  wire [1:0] _wbVecBankId_T_17 = 5'h8 == _wbVecBankId_T_1 ? 2'h0 : _wbVecBankId_T_15; // @[Mux.scala 81:58]
  wire [1:0] _wbVecBankId_T_19 = 5'h9 == _wbVecBankId_T_1 ? 2'h1 : _wbVecBankId_T_17; // @[Mux.scala 81:58]
  wire [1:0] _wbVecBankId_T_21 = 5'ha == _wbVecBankId_T_1 ? 2'h2 : _wbVecBankId_T_19; // @[Mux.scala 81:58]
  wire [1:0] _wbVecBankId_T_23 = 5'hb == _wbVecBankId_T_1 ? 2'h3 : _wbVecBankId_T_21; // @[Mux.scala 81:58]
  wire [1:0] _wbVecBankId_T_25 = 5'hc == _wbVecBankId_T_1 ? 2'h0 : _wbVecBankId_T_23; // @[Mux.scala 81:58]
  wire [1:0] _wbVecBankId_T_27 = 5'hd == _wbVecBankId_T_1 ? 2'h1 : _wbVecBankId_T_25; // @[Mux.scala 81:58]
  wire [1:0] _wbVecBankId_T_29 = 5'he == _wbVecBankId_T_1 ? 2'h2 : _wbVecBankId_T_27; // @[Mux.scala 81:58]
  wire [1:0] _wbVecBankId_T_31 = 5'hf == _wbVecBankId_T_1 ? 2'h3 : _wbVecBankId_T_29; // @[Mux.scala 81:58]
  wire [1:0] _wbVecBankId_T_33 = 5'h10 == _wbVecBankId_T_1 ? 2'h0 : _wbVecBankId_T_31; // @[Mux.scala 81:58]
  wire [1:0] _wbVecBankId_T_35 = 5'h11 == _wbVecBankId_T_1 ? 2'h1 : _wbVecBankId_T_33; // @[Mux.scala 81:58]
  wire [1:0] _wbVecBankId_T_37 = 5'h12 == _wbVecBankId_T_1 ? 2'h2 : _wbVecBankId_T_35; // @[Mux.scala 81:58]
  wire [1:0] _wbVecBankId_T_39 = 5'h13 == _wbVecBankId_T_1 ? 2'h3 : _wbVecBankId_T_37; // @[Mux.scala 81:58]
  wire [1:0] _wbVecBankId_T_41 = 5'h14 == _wbVecBankId_T_1 ? 2'h0 : _wbVecBankId_T_39; // @[Mux.scala 81:58]
  wire [1:0] _wbVecBankId_T_43 = 5'h15 == _wbVecBankId_T_1 ? 2'h1 : _wbVecBankId_T_41; // @[Mux.scala 81:58]
  wire [1:0] _wbVecBankId_T_45 = 5'h16 == _wbVecBankId_T_1 ? 2'h2 : _wbVecBankId_T_43; // @[Mux.scala 81:58]
  wire [1:0] _wbVecBankId_T_47 = 5'h17 == _wbVecBankId_T_1 ? 2'h3 : _wbVecBankId_T_45; // @[Mux.scala 81:58]
  wire [1:0] _wbVecBankId_T_49 = 5'h18 == _wbVecBankId_T_1 ? 2'h0 : _wbVecBankId_T_47; // @[Mux.scala 81:58]
  wire [1:0] _wbVecBankId_T_51 = 5'h19 == _wbVecBankId_T_1 ? 2'h1 : _wbVecBankId_T_49; // @[Mux.scala 81:58]
  wire [1:0] _wbVecBankId_T_53 = 5'h1a == _wbVecBankId_T_1 ? 2'h2 : _wbVecBankId_T_51; // @[Mux.scala 81:58]
  wire [1:0] _wbVecBankId_T_55 = 5'h1b == _wbVecBankId_T_1 ? 2'h3 : _wbVecBankId_T_53; // @[Mux.scala 81:58]
  wire [1:0] _wbVecBankId_T_57 = 5'h1c == _wbVecBankId_T_1 ? 2'h0 : _wbVecBankId_T_55; // @[Mux.scala 81:58]
  wire [1:0] _wbVecBankId_T_59 = 5'h1d == _wbVecBankId_T_1 ? 2'h1 : _wbVecBankId_T_57; // @[Mux.scala 81:58]
  wire [1:0] _wbVecBankId_T_61 = 5'h1e == _wbVecBankId_T_1 ? 2'h2 : _wbVecBankId_T_59; // @[Mux.scala 81:58]
  wire [1:0] _wbVecBankId_T_63 = 5'h1f == _wbVecBankId_T_1 ? 2'h3 : _wbVecBankId_T_61; // @[Mux.scala 81:58]
  wire [5:0] _GEN_73 = {{1'd0}, _wbVecBankId_T_1}; // @[Mux.scala 81:61]
  wire [1:0] _wbVecBankId_T_65 = 6'h20 == _GEN_73 ? 2'h0 : _wbVecBankId_T_63; // @[Mux.scala 81:58]
  wire [1:0] _wbVecBankId_T_67 = 6'h21 == _GEN_73 ? 2'h1 : _wbVecBankId_T_65; // @[Mux.scala 81:58]
  wire [1:0] _wbVecBankId_T_69 = 6'h22 == _GEN_73 ? 2'h2 : _wbVecBankId_T_67; // @[Mux.scala 81:58]
  wire [1:0] wbVecBankId = 6'h23 == _GEN_73 ? 2'h3 : _wbVecBankId_T_69; // @[Mux.scala 81:58]
  wire  _wbVecBankAddr_T_13 = 5'h7 == io_writeVecCtrl_bits_reg_idxw | (5'h6 == io_writeVecCtrl_bits_reg_idxw | (5'h5 ==
    io_writeVecCtrl_bits_reg_idxw | 5'h4 == io_writeVecCtrl_bits_reg_idxw)); // @[Mux.scala 81:58]
  wire [1:0] _wbVecBankAddr_T_15 = 5'h8 == io_writeVecCtrl_bits_reg_idxw ? 2'h2 : {{1'd0}, _wbVecBankAddr_T_13}; // @[Mux.scala 81:58]
  wire [1:0] _wbVecBankAddr_T_17 = 5'h9 == io_writeVecCtrl_bits_reg_idxw ? 2'h2 : _wbVecBankAddr_T_15; // @[Mux.scala 81:58]
  wire [1:0] _wbVecBankAddr_T_19 = 5'ha == io_writeVecCtrl_bits_reg_idxw ? 2'h2 : _wbVecBankAddr_T_17; // @[Mux.scala 81:58]
  wire [1:0] _wbVecBankAddr_T_21 = 5'hb == io_writeVecCtrl_bits_reg_idxw ? 2'h2 : _wbVecBankAddr_T_19; // @[Mux.scala 81:58]
  wire [1:0] _wbVecBankAddr_T_23 = 5'hc == io_writeVecCtrl_bits_reg_idxw ? 2'h3 : _wbVecBankAddr_T_21; // @[Mux.scala 81:58]
  wire [1:0] _wbVecBankAddr_T_25 = 5'hd == io_writeVecCtrl_bits_reg_idxw ? 2'h3 : _wbVecBankAddr_T_23; // @[Mux.scala 81:58]
  wire [1:0] _wbVecBankAddr_T_27 = 5'he == io_writeVecCtrl_bits_reg_idxw ? 2'h3 : _wbVecBankAddr_T_25; // @[Mux.scala 81:58]
  wire [1:0] _wbVecBankAddr_T_29 = 5'hf == io_writeVecCtrl_bits_reg_idxw ? 2'h3 : _wbVecBankAddr_T_27; // @[Mux.scala 81:58]
  wire [2:0] _wbVecBankAddr_T_31 = 5'h10 == io_writeVecCtrl_bits_reg_idxw ? 3'h4 : {{1'd0}, _wbVecBankAddr_T_29}; // @[Mux.scala 81:58]
  wire [2:0] _wbVecBankAddr_T_33 = 5'h11 == io_writeVecCtrl_bits_reg_idxw ? 3'h4 : _wbVecBankAddr_T_31; // @[Mux.scala 81:58]
  wire [2:0] _wbVecBankAddr_T_35 = 5'h12 == io_writeVecCtrl_bits_reg_idxw ? 3'h4 : _wbVecBankAddr_T_33; // @[Mux.scala 81:58]
  wire [2:0] _wbVecBankAddr_T_37 = 5'h13 == io_writeVecCtrl_bits_reg_idxw ? 3'h4 : _wbVecBankAddr_T_35; // @[Mux.scala 81:58]
  wire [2:0] _wbVecBankAddr_T_39 = 5'h14 == io_writeVecCtrl_bits_reg_idxw ? 3'h5 : _wbVecBankAddr_T_37; // @[Mux.scala 81:58]
  wire [2:0] _wbVecBankAddr_T_41 = 5'h15 == io_writeVecCtrl_bits_reg_idxw ? 3'h5 : _wbVecBankAddr_T_39; // @[Mux.scala 81:58]
  wire [2:0] _wbVecBankAddr_T_43 = 5'h16 == io_writeVecCtrl_bits_reg_idxw ? 3'h5 : _wbVecBankAddr_T_41; // @[Mux.scala 81:58]
  wire [2:0] _wbVecBankAddr_T_45 = 5'h17 == io_writeVecCtrl_bits_reg_idxw ? 3'h5 : _wbVecBankAddr_T_43; // @[Mux.scala 81:58]
  wire [2:0] _wbVecBankAddr_T_47 = 5'h18 == io_writeVecCtrl_bits_reg_idxw ? 3'h6 : _wbVecBankAddr_T_45; // @[Mux.scala 81:58]
  wire [2:0] _wbVecBankAddr_T_49 = 5'h19 == io_writeVecCtrl_bits_reg_idxw ? 3'h6 : _wbVecBankAddr_T_47; // @[Mux.scala 81:58]
  wire [2:0] _wbVecBankAddr_T_51 = 5'h1a == io_writeVecCtrl_bits_reg_idxw ? 3'h6 : _wbVecBankAddr_T_49; // @[Mux.scala 81:58]
  wire [2:0] _wbVecBankAddr_T_53 = 5'h1b == io_writeVecCtrl_bits_reg_idxw ? 3'h6 : _wbVecBankAddr_T_51; // @[Mux.scala 81:58]
  wire [2:0] _wbVecBankAddr_T_55 = 5'h1c == io_writeVecCtrl_bits_reg_idxw ? 3'h7 : _wbVecBankAddr_T_53; // @[Mux.scala 81:58]
  wire [2:0] _wbVecBankAddr_T_57 = 5'h1d == io_writeVecCtrl_bits_reg_idxw ? 3'h7 : _wbVecBankAddr_T_55; // @[Mux.scala 81:58]
  wire [2:0] _wbVecBankAddr_T_59 = 5'h1e == io_writeVecCtrl_bits_reg_idxw ? 3'h7 : _wbVecBankAddr_T_57; // @[Mux.scala 81:58]
  wire [2:0] _wbVecBankAddr_T_61 = 5'h1f == io_writeVecCtrl_bits_reg_idxw ? 3'h7 : _wbVecBankAddr_T_59; // @[Mux.scala 81:58]
  wire [5:0] _wbVecBankAddr_T_62 = io_writeVecCtrl_bits_warp_id * 4'h8; // @[operandCollector.scala 445:108]
  wire [5:0] _GEN_77 = {{3'd0}, _wbVecBankAddr_T_61}; // @[operandCollector.scala 445:78]
  wire [5:0] _wbVecBankAddr_T_64 = _GEN_77 + _wbVecBankAddr_T_62; // @[operandCollector.scala 445:78]
  wire [4:0] _GEN_78 = {{3'd0}, io_writeScalarCtrl_bits_warp_id}; // @[operandCollector.scala 446:60]
  wire [4:0] _wbScaBankId_T_1 = io_writeScalarCtrl_bits_reg_idxw + _GEN_78; // @[operandCollector.scala 446:60]
  wire [1:0] _wbScaBankId_T_5 = 5'h2 == _wbScaBankId_T_1 ? 2'h2 : {{1'd0}, 5'h1 == _wbScaBankId_T_1}; // @[Mux.scala 81:58]
  wire [1:0] _wbScaBankId_T_7 = 5'h3 == _wbScaBankId_T_1 ? 2'h3 : _wbScaBankId_T_5; // @[Mux.scala 81:58]
  wire [1:0] _wbScaBankId_T_9 = 5'h4 == _wbScaBankId_T_1 ? 2'h0 : _wbScaBankId_T_7; // @[Mux.scala 81:58]
  wire [1:0] _wbScaBankId_T_11 = 5'h5 == _wbScaBankId_T_1 ? 2'h1 : _wbScaBankId_T_9; // @[Mux.scala 81:58]
  wire [1:0] _wbScaBankId_T_13 = 5'h6 == _wbScaBankId_T_1 ? 2'h2 : _wbScaBankId_T_11; // @[Mux.scala 81:58]
  wire [1:0] _wbScaBankId_T_15 = 5'h7 == _wbScaBankId_T_1 ? 2'h3 : _wbScaBankId_T_13; // @[Mux.scala 81:58]
  wire [1:0] _wbScaBankId_T_17 = 5'h8 == _wbScaBankId_T_1 ? 2'h0 : _wbScaBankId_T_15; // @[Mux.scala 81:58]
  wire [1:0] _wbScaBankId_T_19 = 5'h9 == _wbScaBankId_T_1 ? 2'h1 : _wbScaBankId_T_17; // @[Mux.scala 81:58]
  wire [1:0] _wbScaBankId_T_21 = 5'ha == _wbScaBankId_T_1 ? 2'h2 : _wbScaBankId_T_19; // @[Mux.scala 81:58]
  wire [1:0] _wbScaBankId_T_23 = 5'hb == _wbScaBankId_T_1 ? 2'h3 : _wbScaBankId_T_21; // @[Mux.scala 81:58]
  wire [1:0] _wbScaBankId_T_25 = 5'hc == _wbScaBankId_T_1 ? 2'h0 : _wbScaBankId_T_23; // @[Mux.scala 81:58]
  wire [1:0] _wbScaBankId_T_27 = 5'hd == _wbScaBankId_T_1 ? 2'h1 : _wbScaBankId_T_25; // @[Mux.scala 81:58]
  wire [1:0] _wbScaBankId_T_29 = 5'he == _wbScaBankId_T_1 ? 2'h2 : _wbScaBankId_T_27; // @[Mux.scala 81:58]
  wire [1:0] _wbScaBankId_T_31 = 5'hf == _wbScaBankId_T_1 ? 2'h3 : _wbScaBankId_T_29; // @[Mux.scala 81:58]
  wire [1:0] _wbScaBankId_T_33 = 5'h10 == _wbScaBankId_T_1 ? 2'h0 : _wbScaBankId_T_31; // @[Mux.scala 81:58]
  wire [1:0] _wbScaBankId_T_35 = 5'h11 == _wbScaBankId_T_1 ? 2'h1 : _wbScaBankId_T_33; // @[Mux.scala 81:58]
  wire [1:0] _wbScaBankId_T_37 = 5'h12 == _wbScaBankId_T_1 ? 2'h2 : _wbScaBankId_T_35; // @[Mux.scala 81:58]
  wire [1:0] _wbScaBankId_T_39 = 5'h13 == _wbScaBankId_T_1 ? 2'h3 : _wbScaBankId_T_37; // @[Mux.scala 81:58]
  wire [1:0] _wbScaBankId_T_41 = 5'h14 == _wbScaBankId_T_1 ? 2'h0 : _wbScaBankId_T_39; // @[Mux.scala 81:58]
  wire [1:0] _wbScaBankId_T_43 = 5'h15 == _wbScaBankId_T_1 ? 2'h1 : _wbScaBankId_T_41; // @[Mux.scala 81:58]
  wire [1:0] _wbScaBankId_T_45 = 5'h16 == _wbScaBankId_T_1 ? 2'h2 : _wbScaBankId_T_43; // @[Mux.scala 81:58]
  wire [1:0] _wbScaBankId_T_47 = 5'h17 == _wbScaBankId_T_1 ? 2'h3 : _wbScaBankId_T_45; // @[Mux.scala 81:58]
  wire [1:0] _wbScaBankId_T_49 = 5'h18 == _wbScaBankId_T_1 ? 2'h0 : _wbScaBankId_T_47; // @[Mux.scala 81:58]
  wire [1:0] _wbScaBankId_T_51 = 5'h19 == _wbScaBankId_T_1 ? 2'h1 : _wbScaBankId_T_49; // @[Mux.scala 81:58]
  wire [1:0] _wbScaBankId_T_53 = 5'h1a == _wbScaBankId_T_1 ? 2'h2 : _wbScaBankId_T_51; // @[Mux.scala 81:58]
  wire [1:0] _wbScaBankId_T_55 = 5'h1b == _wbScaBankId_T_1 ? 2'h3 : _wbScaBankId_T_53; // @[Mux.scala 81:58]
  wire [1:0] _wbScaBankId_T_57 = 5'h1c == _wbScaBankId_T_1 ? 2'h0 : _wbScaBankId_T_55; // @[Mux.scala 81:58]
  wire [1:0] _wbScaBankId_T_59 = 5'h1d == _wbScaBankId_T_1 ? 2'h1 : _wbScaBankId_T_57; // @[Mux.scala 81:58]
  wire [1:0] _wbScaBankId_T_61 = 5'h1e == _wbScaBankId_T_1 ? 2'h2 : _wbScaBankId_T_59; // @[Mux.scala 81:58]
  wire [1:0] _wbScaBankId_T_63 = 5'h1f == _wbScaBankId_T_1 ? 2'h3 : _wbScaBankId_T_61; // @[Mux.scala 81:58]
  wire [5:0] _GEN_79 = {{1'd0}, _wbScaBankId_T_1}; // @[Mux.scala 81:61]
  wire [1:0] _wbScaBankId_T_65 = 6'h20 == _GEN_79 ? 2'h0 : _wbScaBankId_T_63; // @[Mux.scala 81:58]
  wire [1:0] _wbScaBankId_T_67 = 6'h21 == _GEN_79 ? 2'h1 : _wbScaBankId_T_65; // @[Mux.scala 81:58]
  wire [1:0] _wbScaBankId_T_69 = 6'h22 == _GEN_79 ? 2'h2 : _wbScaBankId_T_67; // @[Mux.scala 81:58]
  wire [1:0] wbScaBankId = 6'h23 == _GEN_79 ? 2'h3 : _wbScaBankId_T_69; // @[Mux.scala 81:58]
  wire  _wbScaBankAddr_T_13 = 5'h7 == io_writeScalarCtrl_bits_reg_idxw | (5'h6 == io_writeScalarCtrl_bits_reg_idxw | (5'h5
     == io_writeScalarCtrl_bits_reg_idxw | 5'h4 == io_writeScalarCtrl_bits_reg_idxw)); // @[Mux.scala 81:58]
  wire [1:0] _wbScaBankAddr_T_15 = 5'h8 == io_writeScalarCtrl_bits_reg_idxw ? 2'h2 : {{1'd0}, _wbScaBankAddr_T_13}; // @[Mux.scala 81:58]
  wire [1:0] _wbScaBankAddr_T_17 = 5'h9 == io_writeScalarCtrl_bits_reg_idxw ? 2'h2 : _wbScaBankAddr_T_15; // @[Mux.scala 81:58]
  wire [1:0] _wbScaBankAddr_T_19 = 5'ha == io_writeScalarCtrl_bits_reg_idxw ? 2'h2 : _wbScaBankAddr_T_17; // @[Mux.scala 81:58]
  wire [1:0] _wbScaBankAddr_T_21 = 5'hb == io_writeScalarCtrl_bits_reg_idxw ? 2'h2 : _wbScaBankAddr_T_19; // @[Mux.scala 81:58]
  wire [1:0] _wbScaBankAddr_T_23 = 5'hc == io_writeScalarCtrl_bits_reg_idxw ? 2'h3 : _wbScaBankAddr_T_21; // @[Mux.scala 81:58]
  wire [1:0] _wbScaBankAddr_T_25 = 5'hd == io_writeScalarCtrl_bits_reg_idxw ? 2'h3 : _wbScaBankAddr_T_23; // @[Mux.scala 81:58]
  wire [1:0] _wbScaBankAddr_T_27 = 5'he == io_writeScalarCtrl_bits_reg_idxw ? 2'h3 : _wbScaBankAddr_T_25; // @[Mux.scala 81:58]
  wire [1:0] _wbScaBankAddr_T_29 = 5'hf == io_writeScalarCtrl_bits_reg_idxw ? 2'h3 : _wbScaBankAddr_T_27; // @[Mux.scala 81:58]
  wire [2:0] _wbScaBankAddr_T_31 = 5'h10 == io_writeScalarCtrl_bits_reg_idxw ? 3'h4 : {{1'd0}, _wbScaBankAddr_T_29}; // @[Mux.scala 81:58]
  wire [2:0] _wbScaBankAddr_T_33 = 5'h11 == io_writeScalarCtrl_bits_reg_idxw ? 3'h4 : _wbScaBankAddr_T_31; // @[Mux.scala 81:58]
  wire [2:0] _wbScaBankAddr_T_35 = 5'h12 == io_writeScalarCtrl_bits_reg_idxw ? 3'h4 : _wbScaBankAddr_T_33; // @[Mux.scala 81:58]
  wire [2:0] _wbScaBankAddr_T_37 = 5'h13 == io_writeScalarCtrl_bits_reg_idxw ? 3'h4 : _wbScaBankAddr_T_35; // @[Mux.scala 81:58]
  wire [2:0] _wbScaBankAddr_T_39 = 5'h14 == io_writeScalarCtrl_bits_reg_idxw ? 3'h5 : _wbScaBankAddr_T_37; // @[Mux.scala 81:58]
  wire [2:0] _wbScaBankAddr_T_41 = 5'h15 == io_writeScalarCtrl_bits_reg_idxw ? 3'h5 : _wbScaBankAddr_T_39; // @[Mux.scala 81:58]
  wire [2:0] _wbScaBankAddr_T_43 = 5'h16 == io_writeScalarCtrl_bits_reg_idxw ? 3'h5 : _wbScaBankAddr_T_41; // @[Mux.scala 81:58]
  wire [2:0] _wbScaBankAddr_T_45 = 5'h17 == io_writeScalarCtrl_bits_reg_idxw ? 3'h5 : _wbScaBankAddr_T_43; // @[Mux.scala 81:58]
  wire [2:0] _wbScaBankAddr_T_47 = 5'h18 == io_writeScalarCtrl_bits_reg_idxw ? 3'h6 : _wbScaBankAddr_T_45; // @[Mux.scala 81:58]
  wire [2:0] _wbScaBankAddr_T_49 = 5'h19 == io_writeScalarCtrl_bits_reg_idxw ? 3'h6 : _wbScaBankAddr_T_47; // @[Mux.scala 81:58]
  wire [2:0] _wbScaBankAddr_T_51 = 5'h1a == io_writeScalarCtrl_bits_reg_idxw ? 3'h6 : _wbScaBankAddr_T_49; // @[Mux.scala 81:58]
  wire [2:0] _wbScaBankAddr_T_53 = 5'h1b == io_writeScalarCtrl_bits_reg_idxw ? 3'h6 : _wbScaBankAddr_T_51; // @[Mux.scala 81:58]
  wire [2:0] _wbScaBankAddr_T_55 = 5'h1c == io_writeScalarCtrl_bits_reg_idxw ? 3'h7 : _wbScaBankAddr_T_53; // @[Mux.scala 81:58]
  wire [2:0] _wbScaBankAddr_T_57 = 5'h1d == io_writeScalarCtrl_bits_reg_idxw ? 3'h7 : _wbScaBankAddr_T_55; // @[Mux.scala 81:58]
  wire [2:0] _wbScaBankAddr_T_59 = 5'h1e == io_writeScalarCtrl_bits_reg_idxw ? 3'h7 : _wbScaBankAddr_T_57; // @[Mux.scala 81:58]
  wire [2:0] _wbScaBankAddr_T_61 = 5'h1f == io_writeScalarCtrl_bits_reg_idxw ? 3'h7 : _wbScaBankAddr_T_59; // @[Mux.scala 81:58]
  wire [5:0] _wbScaBankAddr_T_62 = io_writeScalarCtrl_bits_warp_id * 4'h8; // @[operandCollector.scala 447:114]
  wire [5:0] _GEN_83 = {{3'd0}, _wbScaBankAddr_T_61}; // @[operandCollector.scala 447:81]
  wire [5:0] _wbScaBankAddr_T_64 = _GEN_83 + _wbScaBankAddr_T_62; // @[operandCollector.scala 447:81]
  collectorUnit collectorUnit ( // @[operandCollector.scala 388:66]
    .clock(collectorUnit_clock),
    .reset(collectorUnit_reset),
    .io_control_ready(collectorUnit_io_control_ready),
    .io_control_valid(collectorUnit_io_control_valid),
    .io_control_bits_inst(collectorUnit_io_control_bits_inst),
    .io_control_bits_wid(collectorUnit_io_control_bits_wid),
    .io_control_bits_fp(collectorUnit_io_control_bits_fp),
    .io_control_bits_branch(collectorUnit_io_control_bits_branch),
    .io_control_bits_simt_stack(collectorUnit_io_control_bits_simt_stack),
    .io_control_bits_simt_stack_op(collectorUnit_io_control_bits_simt_stack_op),
    .io_control_bits_barrier(collectorUnit_io_control_bits_barrier),
    .io_control_bits_csr(collectorUnit_io_control_bits_csr),
    .io_control_bits_reverse(collectorUnit_io_control_bits_reverse),
    .io_control_bits_sel_alu2(collectorUnit_io_control_bits_sel_alu2),
    .io_control_bits_sel_alu1(collectorUnit_io_control_bits_sel_alu1),
    .io_control_bits_isvec(collectorUnit_io_control_bits_isvec),
    .io_control_bits_sel_alu3(collectorUnit_io_control_bits_sel_alu3),
    .io_control_bits_mask(collectorUnit_io_control_bits_mask),
    .io_control_bits_sel_imm(collectorUnit_io_control_bits_sel_imm),
    .io_control_bits_mem_whb(collectorUnit_io_control_bits_mem_whb),
    .io_control_bits_mem_unsigned(collectorUnit_io_control_bits_mem_unsigned),
    .io_control_bits_alu_fn(collectorUnit_io_control_bits_alu_fn),
    .io_control_bits_mem(collectorUnit_io_control_bits_mem),
    .io_control_bits_mul(collectorUnit_io_control_bits_mul),
    .io_control_bits_mem_cmd(collectorUnit_io_control_bits_mem_cmd),
    .io_control_bits_mop(collectorUnit_io_control_bits_mop),
    .io_control_bits_reg_idx1(collectorUnit_io_control_bits_reg_idx1),
    .io_control_bits_reg_idx2(collectorUnit_io_control_bits_reg_idx2),
    .io_control_bits_reg_idx3(collectorUnit_io_control_bits_reg_idx3),
    .io_control_bits_reg_idxw(collectorUnit_io_control_bits_reg_idxw),
    .io_control_bits_wfd(collectorUnit_io_control_bits_wfd),
    .io_control_bits_fence(collectorUnit_io_control_bits_fence),
    .io_control_bits_sfu(collectorUnit_io_control_bits_sfu),
    .io_control_bits_readmask(collectorUnit_io_control_bits_readmask),
    .io_control_bits_writemask(collectorUnit_io_control_bits_writemask),
    .io_control_bits_wxd(collectorUnit_io_control_bits_wxd),
    .io_control_bits_pc(collectorUnit_io_control_bits_pc),
    .io_control_bits_spike_info_pc(collectorUnit_io_control_bits_spike_info_pc),
    .io_control_bits_spike_info_inst(collectorUnit_io_control_bits_spike_info_inst),
    .io_bankIn_0_ready(collectorUnit_io_bankIn_0_ready),
    .io_bankIn_0_valid(collectorUnit_io_bankIn_0_valid),
    .io_bankIn_0_bits_regOrder(collectorUnit_io_bankIn_0_bits_regOrder),
    .io_bankIn_0_bits_data_0(collectorUnit_io_bankIn_0_bits_data_0),
    .io_bankIn_0_bits_data_1(collectorUnit_io_bankIn_0_bits_data_1),
    .io_bankIn_0_bits_data_2(collectorUnit_io_bankIn_0_bits_data_2),
    .io_bankIn_0_bits_data_3(collectorUnit_io_bankIn_0_bits_data_3),
    .io_bankIn_0_bits_v0_0(collectorUnit_io_bankIn_0_bits_v0_0),
    .io_bankIn_1_ready(collectorUnit_io_bankIn_1_ready),
    .io_bankIn_1_valid(collectorUnit_io_bankIn_1_valid),
    .io_bankIn_1_bits_regOrder(collectorUnit_io_bankIn_1_bits_regOrder),
    .io_bankIn_1_bits_data_0(collectorUnit_io_bankIn_1_bits_data_0),
    .io_bankIn_1_bits_data_1(collectorUnit_io_bankIn_1_bits_data_1),
    .io_bankIn_1_bits_data_2(collectorUnit_io_bankIn_1_bits_data_2),
    .io_bankIn_1_bits_data_3(collectorUnit_io_bankIn_1_bits_data_3),
    .io_bankIn_1_bits_v0_0(collectorUnit_io_bankIn_1_bits_v0_0),
    .io_bankIn_2_ready(collectorUnit_io_bankIn_2_ready),
    .io_bankIn_2_valid(collectorUnit_io_bankIn_2_valid),
    .io_bankIn_2_bits_regOrder(collectorUnit_io_bankIn_2_bits_regOrder),
    .io_bankIn_2_bits_data_0(collectorUnit_io_bankIn_2_bits_data_0),
    .io_bankIn_2_bits_data_1(collectorUnit_io_bankIn_2_bits_data_1),
    .io_bankIn_2_bits_data_2(collectorUnit_io_bankIn_2_bits_data_2),
    .io_bankIn_2_bits_data_3(collectorUnit_io_bankIn_2_bits_data_3),
    .io_bankIn_2_bits_v0_0(collectorUnit_io_bankIn_2_bits_v0_0),
    .io_bankIn_3_ready(collectorUnit_io_bankIn_3_ready),
    .io_bankIn_3_valid(collectorUnit_io_bankIn_3_valid),
    .io_bankIn_3_bits_regOrder(collectorUnit_io_bankIn_3_bits_regOrder),
    .io_bankIn_3_bits_data_0(collectorUnit_io_bankIn_3_bits_data_0),
    .io_bankIn_3_bits_data_1(collectorUnit_io_bankIn_3_bits_data_1),
    .io_bankIn_3_bits_data_2(collectorUnit_io_bankIn_3_bits_data_2),
    .io_bankIn_3_bits_data_3(collectorUnit_io_bankIn_3_bits_data_3),
    .io_bankIn_3_bits_v0_0(collectorUnit_io_bankIn_3_bits_v0_0),
    .io_issue_ready(collectorUnit_io_issue_ready),
    .io_issue_valid(collectorUnit_io_issue_valid),
    .io_issue_bits_alu_src1_0(collectorUnit_io_issue_bits_alu_src1_0),
    .io_issue_bits_alu_src1_1(collectorUnit_io_issue_bits_alu_src1_1),
    .io_issue_bits_alu_src1_2(collectorUnit_io_issue_bits_alu_src1_2),
    .io_issue_bits_alu_src1_3(collectorUnit_io_issue_bits_alu_src1_3),
    .io_issue_bits_alu_src2_0(collectorUnit_io_issue_bits_alu_src2_0),
    .io_issue_bits_alu_src2_1(collectorUnit_io_issue_bits_alu_src2_1),
    .io_issue_bits_alu_src2_2(collectorUnit_io_issue_bits_alu_src2_2),
    .io_issue_bits_alu_src2_3(collectorUnit_io_issue_bits_alu_src2_3),
    .io_issue_bits_alu_src3_0(collectorUnit_io_issue_bits_alu_src3_0),
    .io_issue_bits_alu_src3_1(collectorUnit_io_issue_bits_alu_src3_1),
    .io_issue_bits_alu_src3_2(collectorUnit_io_issue_bits_alu_src3_2),
    .io_issue_bits_alu_src3_3(collectorUnit_io_issue_bits_alu_src3_3),
    .io_issue_bits_mask_0(collectorUnit_io_issue_bits_mask_0),
    .io_issue_bits_mask_1(collectorUnit_io_issue_bits_mask_1),
    .io_issue_bits_mask_2(collectorUnit_io_issue_bits_mask_2),
    .io_issue_bits_mask_3(collectorUnit_io_issue_bits_mask_3),
    .io_issue_bits_control_inst(collectorUnit_io_issue_bits_control_inst),
    .io_issue_bits_control_wid(collectorUnit_io_issue_bits_control_wid),
    .io_issue_bits_control_fp(collectorUnit_io_issue_bits_control_fp),
    .io_issue_bits_control_branch(collectorUnit_io_issue_bits_control_branch),
    .io_issue_bits_control_simt_stack(collectorUnit_io_issue_bits_control_simt_stack),
    .io_issue_bits_control_simt_stack_op(collectorUnit_io_issue_bits_control_simt_stack_op),
    .io_issue_bits_control_barrier(collectorUnit_io_issue_bits_control_barrier),
    .io_issue_bits_control_csr(collectorUnit_io_issue_bits_control_csr),
    .io_issue_bits_control_reverse(collectorUnit_io_issue_bits_control_reverse),
    .io_issue_bits_control_sel_alu2(collectorUnit_io_issue_bits_control_sel_alu2),
    .io_issue_bits_control_sel_alu1(collectorUnit_io_issue_bits_control_sel_alu1),
    .io_issue_bits_control_isvec(collectorUnit_io_issue_bits_control_isvec),
    .io_issue_bits_control_sel_alu3(collectorUnit_io_issue_bits_control_sel_alu3),
    .io_issue_bits_control_mask(collectorUnit_io_issue_bits_control_mask),
    .io_issue_bits_control_sel_imm(collectorUnit_io_issue_bits_control_sel_imm),
    .io_issue_bits_control_mem_whb(collectorUnit_io_issue_bits_control_mem_whb),
    .io_issue_bits_control_mem_unsigned(collectorUnit_io_issue_bits_control_mem_unsigned),
    .io_issue_bits_control_alu_fn(collectorUnit_io_issue_bits_control_alu_fn),
    .io_issue_bits_control_mem(collectorUnit_io_issue_bits_control_mem),
    .io_issue_bits_control_mul(collectorUnit_io_issue_bits_control_mul),
    .io_issue_bits_control_mem_cmd(collectorUnit_io_issue_bits_control_mem_cmd),
    .io_issue_bits_control_mop(collectorUnit_io_issue_bits_control_mop),
    .io_issue_bits_control_reg_idx1(collectorUnit_io_issue_bits_control_reg_idx1),
    .io_issue_bits_control_reg_idx2(collectorUnit_io_issue_bits_control_reg_idx2),
    .io_issue_bits_control_reg_idx3(collectorUnit_io_issue_bits_control_reg_idx3),
    .io_issue_bits_control_reg_idxw(collectorUnit_io_issue_bits_control_reg_idxw),
    .io_issue_bits_control_wfd(collectorUnit_io_issue_bits_control_wfd),
    .io_issue_bits_control_fence(collectorUnit_io_issue_bits_control_fence),
    .io_issue_bits_control_sfu(collectorUnit_io_issue_bits_control_sfu),
    .io_issue_bits_control_readmask(collectorUnit_io_issue_bits_control_readmask),
    .io_issue_bits_control_writemask(collectorUnit_io_issue_bits_control_writemask),
    .io_issue_bits_control_wxd(collectorUnit_io_issue_bits_control_wxd),
    .io_issue_bits_control_pc(collectorUnit_io_issue_bits_control_pc),
    .io_issue_bits_control_spike_info_pc(collectorUnit_io_issue_bits_control_spike_info_pc),
    .io_issue_bits_control_spike_info_inst(collectorUnit_io_issue_bits_control_spike_info_inst),
    .io_outArbiterIO_0_valid(collectorUnit_io_outArbiterIO_0_valid),
    .io_outArbiterIO_0_bits_rsAddr(collectorUnit_io_outArbiterIO_0_bits_rsAddr),
    .io_outArbiterIO_0_bits_bankID(collectorUnit_io_outArbiterIO_0_bits_bankID),
    .io_outArbiterIO_0_bits_rsType(collectorUnit_io_outArbiterIO_0_bits_rsType),
    .io_outArbiterIO_1_valid(collectorUnit_io_outArbiterIO_1_valid),
    .io_outArbiterIO_1_bits_rsAddr(collectorUnit_io_outArbiterIO_1_bits_rsAddr),
    .io_outArbiterIO_1_bits_bankID(collectorUnit_io_outArbiterIO_1_bits_bankID),
    .io_outArbiterIO_1_bits_rsType(collectorUnit_io_outArbiterIO_1_bits_rsType),
    .io_outArbiterIO_2_valid(collectorUnit_io_outArbiterIO_2_valid),
    .io_outArbiterIO_2_bits_rsAddr(collectorUnit_io_outArbiterIO_2_bits_rsAddr),
    .io_outArbiterIO_2_bits_bankID(collectorUnit_io_outArbiterIO_2_bits_bankID),
    .io_outArbiterIO_2_bits_rsType(collectorUnit_io_outArbiterIO_2_bits_rsType),
    .io_outArbiterIO_3_valid(collectorUnit_io_outArbiterIO_3_valid),
    .io_outArbiterIO_3_bits_rsAddr(collectorUnit_io_outArbiterIO_3_bits_rsAddr),
    .io_outArbiterIO_3_bits_bankID(collectorUnit_io_outArbiterIO_3_bits_bankID)
  );
  collectorUnit collectorUnit_1 ( // @[operandCollector.scala 388:66]
    .clock(collectorUnit_1_clock),
    .reset(collectorUnit_1_reset),
    .io_control_ready(collectorUnit_1_io_control_ready),
    .io_control_valid(collectorUnit_1_io_control_valid),
    .io_control_bits_inst(collectorUnit_1_io_control_bits_inst),
    .io_control_bits_wid(collectorUnit_1_io_control_bits_wid),
    .io_control_bits_fp(collectorUnit_1_io_control_bits_fp),
    .io_control_bits_branch(collectorUnit_1_io_control_bits_branch),
    .io_control_bits_simt_stack(collectorUnit_1_io_control_bits_simt_stack),
    .io_control_bits_simt_stack_op(collectorUnit_1_io_control_bits_simt_stack_op),
    .io_control_bits_barrier(collectorUnit_1_io_control_bits_barrier),
    .io_control_bits_csr(collectorUnit_1_io_control_bits_csr),
    .io_control_bits_reverse(collectorUnit_1_io_control_bits_reverse),
    .io_control_bits_sel_alu2(collectorUnit_1_io_control_bits_sel_alu2),
    .io_control_bits_sel_alu1(collectorUnit_1_io_control_bits_sel_alu1),
    .io_control_bits_isvec(collectorUnit_1_io_control_bits_isvec),
    .io_control_bits_sel_alu3(collectorUnit_1_io_control_bits_sel_alu3),
    .io_control_bits_mask(collectorUnit_1_io_control_bits_mask),
    .io_control_bits_sel_imm(collectorUnit_1_io_control_bits_sel_imm),
    .io_control_bits_mem_whb(collectorUnit_1_io_control_bits_mem_whb),
    .io_control_bits_mem_unsigned(collectorUnit_1_io_control_bits_mem_unsigned),
    .io_control_bits_alu_fn(collectorUnit_1_io_control_bits_alu_fn),
    .io_control_bits_mem(collectorUnit_1_io_control_bits_mem),
    .io_control_bits_mul(collectorUnit_1_io_control_bits_mul),
    .io_control_bits_mem_cmd(collectorUnit_1_io_control_bits_mem_cmd),
    .io_control_bits_mop(collectorUnit_1_io_control_bits_mop),
    .io_control_bits_reg_idx1(collectorUnit_1_io_control_bits_reg_idx1),
    .io_control_bits_reg_idx2(collectorUnit_1_io_control_bits_reg_idx2),
    .io_control_bits_reg_idx3(collectorUnit_1_io_control_bits_reg_idx3),
    .io_control_bits_reg_idxw(collectorUnit_1_io_control_bits_reg_idxw),
    .io_control_bits_wfd(collectorUnit_1_io_control_bits_wfd),
    .io_control_bits_fence(collectorUnit_1_io_control_bits_fence),
    .io_control_bits_sfu(collectorUnit_1_io_control_bits_sfu),
    .io_control_bits_readmask(collectorUnit_1_io_control_bits_readmask),
    .io_control_bits_writemask(collectorUnit_1_io_control_bits_writemask),
    .io_control_bits_wxd(collectorUnit_1_io_control_bits_wxd),
    .io_control_bits_pc(collectorUnit_1_io_control_bits_pc),
    .io_control_bits_spike_info_pc(collectorUnit_1_io_control_bits_spike_info_pc),
    .io_control_bits_spike_info_inst(collectorUnit_1_io_control_bits_spike_info_inst),
    .io_bankIn_0_ready(collectorUnit_1_io_bankIn_0_ready),
    .io_bankIn_0_valid(collectorUnit_1_io_bankIn_0_valid),
    .io_bankIn_0_bits_regOrder(collectorUnit_1_io_bankIn_0_bits_regOrder),
    .io_bankIn_0_bits_data_0(collectorUnit_1_io_bankIn_0_bits_data_0),
    .io_bankIn_0_bits_data_1(collectorUnit_1_io_bankIn_0_bits_data_1),
    .io_bankIn_0_bits_data_2(collectorUnit_1_io_bankIn_0_bits_data_2),
    .io_bankIn_0_bits_data_3(collectorUnit_1_io_bankIn_0_bits_data_3),
    .io_bankIn_0_bits_v0_0(collectorUnit_1_io_bankIn_0_bits_v0_0),
    .io_bankIn_1_ready(collectorUnit_1_io_bankIn_1_ready),
    .io_bankIn_1_valid(collectorUnit_1_io_bankIn_1_valid),
    .io_bankIn_1_bits_regOrder(collectorUnit_1_io_bankIn_1_bits_regOrder),
    .io_bankIn_1_bits_data_0(collectorUnit_1_io_bankIn_1_bits_data_0),
    .io_bankIn_1_bits_data_1(collectorUnit_1_io_bankIn_1_bits_data_1),
    .io_bankIn_1_bits_data_2(collectorUnit_1_io_bankIn_1_bits_data_2),
    .io_bankIn_1_bits_data_3(collectorUnit_1_io_bankIn_1_bits_data_3),
    .io_bankIn_1_bits_v0_0(collectorUnit_1_io_bankIn_1_bits_v0_0),
    .io_bankIn_2_ready(collectorUnit_1_io_bankIn_2_ready),
    .io_bankIn_2_valid(collectorUnit_1_io_bankIn_2_valid),
    .io_bankIn_2_bits_regOrder(collectorUnit_1_io_bankIn_2_bits_regOrder),
    .io_bankIn_2_bits_data_0(collectorUnit_1_io_bankIn_2_bits_data_0),
    .io_bankIn_2_bits_data_1(collectorUnit_1_io_bankIn_2_bits_data_1),
    .io_bankIn_2_bits_data_2(collectorUnit_1_io_bankIn_2_bits_data_2),
    .io_bankIn_2_bits_data_3(collectorUnit_1_io_bankIn_2_bits_data_3),
    .io_bankIn_2_bits_v0_0(collectorUnit_1_io_bankIn_2_bits_v0_0),
    .io_bankIn_3_ready(collectorUnit_1_io_bankIn_3_ready),
    .io_bankIn_3_valid(collectorUnit_1_io_bankIn_3_valid),
    .io_bankIn_3_bits_regOrder(collectorUnit_1_io_bankIn_3_bits_regOrder),
    .io_bankIn_3_bits_data_0(collectorUnit_1_io_bankIn_3_bits_data_0),
    .io_bankIn_3_bits_data_1(collectorUnit_1_io_bankIn_3_bits_data_1),
    .io_bankIn_3_bits_data_2(collectorUnit_1_io_bankIn_3_bits_data_2),
    .io_bankIn_3_bits_data_3(collectorUnit_1_io_bankIn_3_bits_data_3),
    .io_bankIn_3_bits_v0_0(collectorUnit_1_io_bankIn_3_bits_v0_0),
    .io_issue_ready(collectorUnit_1_io_issue_ready),
    .io_issue_valid(collectorUnit_1_io_issue_valid),
    .io_issue_bits_alu_src1_0(collectorUnit_1_io_issue_bits_alu_src1_0),
    .io_issue_bits_alu_src1_1(collectorUnit_1_io_issue_bits_alu_src1_1),
    .io_issue_bits_alu_src1_2(collectorUnit_1_io_issue_bits_alu_src1_2),
    .io_issue_bits_alu_src1_3(collectorUnit_1_io_issue_bits_alu_src1_3),
    .io_issue_bits_alu_src2_0(collectorUnit_1_io_issue_bits_alu_src2_0),
    .io_issue_bits_alu_src2_1(collectorUnit_1_io_issue_bits_alu_src2_1),
    .io_issue_bits_alu_src2_2(collectorUnit_1_io_issue_bits_alu_src2_2),
    .io_issue_bits_alu_src2_3(collectorUnit_1_io_issue_bits_alu_src2_3),
    .io_issue_bits_alu_src3_0(collectorUnit_1_io_issue_bits_alu_src3_0),
    .io_issue_bits_alu_src3_1(collectorUnit_1_io_issue_bits_alu_src3_1),
    .io_issue_bits_alu_src3_2(collectorUnit_1_io_issue_bits_alu_src3_2),
    .io_issue_bits_alu_src3_3(collectorUnit_1_io_issue_bits_alu_src3_3),
    .io_issue_bits_mask_0(collectorUnit_1_io_issue_bits_mask_0),
    .io_issue_bits_mask_1(collectorUnit_1_io_issue_bits_mask_1),
    .io_issue_bits_mask_2(collectorUnit_1_io_issue_bits_mask_2),
    .io_issue_bits_mask_3(collectorUnit_1_io_issue_bits_mask_3),
    .io_issue_bits_control_inst(collectorUnit_1_io_issue_bits_control_inst),
    .io_issue_bits_control_wid(collectorUnit_1_io_issue_bits_control_wid),
    .io_issue_bits_control_fp(collectorUnit_1_io_issue_bits_control_fp),
    .io_issue_bits_control_branch(collectorUnit_1_io_issue_bits_control_branch),
    .io_issue_bits_control_simt_stack(collectorUnit_1_io_issue_bits_control_simt_stack),
    .io_issue_bits_control_simt_stack_op(collectorUnit_1_io_issue_bits_control_simt_stack_op),
    .io_issue_bits_control_barrier(collectorUnit_1_io_issue_bits_control_barrier),
    .io_issue_bits_control_csr(collectorUnit_1_io_issue_bits_control_csr),
    .io_issue_bits_control_reverse(collectorUnit_1_io_issue_bits_control_reverse),
    .io_issue_bits_control_sel_alu2(collectorUnit_1_io_issue_bits_control_sel_alu2),
    .io_issue_bits_control_sel_alu1(collectorUnit_1_io_issue_bits_control_sel_alu1),
    .io_issue_bits_control_isvec(collectorUnit_1_io_issue_bits_control_isvec),
    .io_issue_bits_control_sel_alu3(collectorUnit_1_io_issue_bits_control_sel_alu3),
    .io_issue_bits_control_mask(collectorUnit_1_io_issue_bits_control_mask),
    .io_issue_bits_control_sel_imm(collectorUnit_1_io_issue_bits_control_sel_imm),
    .io_issue_bits_control_mem_whb(collectorUnit_1_io_issue_bits_control_mem_whb),
    .io_issue_bits_control_mem_unsigned(collectorUnit_1_io_issue_bits_control_mem_unsigned),
    .io_issue_bits_control_alu_fn(collectorUnit_1_io_issue_bits_control_alu_fn),
    .io_issue_bits_control_mem(collectorUnit_1_io_issue_bits_control_mem),
    .io_issue_bits_control_mul(collectorUnit_1_io_issue_bits_control_mul),
    .io_issue_bits_control_mem_cmd(collectorUnit_1_io_issue_bits_control_mem_cmd),
    .io_issue_bits_control_mop(collectorUnit_1_io_issue_bits_control_mop),
    .io_issue_bits_control_reg_idx1(collectorUnit_1_io_issue_bits_control_reg_idx1),
    .io_issue_bits_control_reg_idx2(collectorUnit_1_io_issue_bits_control_reg_idx2),
    .io_issue_bits_control_reg_idx3(collectorUnit_1_io_issue_bits_control_reg_idx3),
    .io_issue_bits_control_reg_idxw(collectorUnit_1_io_issue_bits_control_reg_idxw),
    .io_issue_bits_control_wfd(collectorUnit_1_io_issue_bits_control_wfd),
    .io_issue_bits_control_fence(collectorUnit_1_io_issue_bits_control_fence),
    .io_issue_bits_control_sfu(collectorUnit_1_io_issue_bits_control_sfu),
    .io_issue_bits_control_readmask(collectorUnit_1_io_issue_bits_control_readmask),
    .io_issue_bits_control_writemask(collectorUnit_1_io_issue_bits_control_writemask),
    .io_issue_bits_control_wxd(collectorUnit_1_io_issue_bits_control_wxd),
    .io_issue_bits_control_pc(collectorUnit_1_io_issue_bits_control_pc),
    .io_issue_bits_control_spike_info_pc(collectorUnit_1_io_issue_bits_control_spike_info_pc),
    .io_issue_bits_control_spike_info_inst(collectorUnit_1_io_issue_bits_control_spike_info_inst),
    .io_outArbiterIO_0_valid(collectorUnit_1_io_outArbiterIO_0_valid),
    .io_outArbiterIO_0_bits_rsAddr(collectorUnit_1_io_outArbiterIO_0_bits_rsAddr),
    .io_outArbiterIO_0_bits_bankID(collectorUnit_1_io_outArbiterIO_0_bits_bankID),
    .io_outArbiterIO_0_bits_rsType(collectorUnit_1_io_outArbiterIO_0_bits_rsType),
    .io_outArbiterIO_1_valid(collectorUnit_1_io_outArbiterIO_1_valid),
    .io_outArbiterIO_1_bits_rsAddr(collectorUnit_1_io_outArbiterIO_1_bits_rsAddr),
    .io_outArbiterIO_1_bits_bankID(collectorUnit_1_io_outArbiterIO_1_bits_bankID),
    .io_outArbiterIO_1_bits_rsType(collectorUnit_1_io_outArbiterIO_1_bits_rsType),
    .io_outArbiterIO_2_valid(collectorUnit_1_io_outArbiterIO_2_valid),
    .io_outArbiterIO_2_bits_rsAddr(collectorUnit_1_io_outArbiterIO_2_bits_rsAddr),
    .io_outArbiterIO_2_bits_bankID(collectorUnit_1_io_outArbiterIO_2_bits_bankID),
    .io_outArbiterIO_2_bits_rsType(collectorUnit_1_io_outArbiterIO_2_bits_rsType),
    .io_outArbiterIO_3_valid(collectorUnit_1_io_outArbiterIO_3_valid),
    .io_outArbiterIO_3_bits_rsAddr(collectorUnit_1_io_outArbiterIO_3_bits_rsAddr),
    .io_outArbiterIO_3_bits_bankID(collectorUnit_1_io_outArbiterIO_3_bits_bankID)
  );
  collectorUnit collectorUnit_2 ( // @[operandCollector.scala 388:66]
    .clock(collectorUnit_2_clock),
    .reset(collectorUnit_2_reset),
    .io_control_ready(collectorUnit_2_io_control_ready),
    .io_control_valid(collectorUnit_2_io_control_valid),
    .io_control_bits_inst(collectorUnit_2_io_control_bits_inst),
    .io_control_bits_wid(collectorUnit_2_io_control_bits_wid),
    .io_control_bits_fp(collectorUnit_2_io_control_bits_fp),
    .io_control_bits_branch(collectorUnit_2_io_control_bits_branch),
    .io_control_bits_simt_stack(collectorUnit_2_io_control_bits_simt_stack),
    .io_control_bits_simt_stack_op(collectorUnit_2_io_control_bits_simt_stack_op),
    .io_control_bits_barrier(collectorUnit_2_io_control_bits_barrier),
    .io_control_bits_csr(collectorUnit_2_io_control_bits_csr),
    .io_control_bits_reverse(collectorUnit_2_io_control_bits_reverse),
    .io_control_bits_sel_alu2(collectorUnit_2_io_control_bits_sel_alu2),
    .io_control_bits_sel_alu1(collectorUnit_2_io_control_bits_sel_alu1),
    .io_control_bits_isvec(collectorUnit_2_io_control_bits_isvec),
    .io_control_bits_sel_alu3(collectorUnit_2_io_control_bits_sel_alu3),
    .io_control_bits_mask(collectorUnit_2_io_control_bits_mask),
    .io_control_bits_sel_imm(collectorUnit_2_io_control_bits_sel_imm),
    .io_control_bits_mem_whb(collectorUnit_2_io_control_bits_mem_whb),
    .io_control_bits_mem_unsigned(collectorUnit_2_io_control_bits_mem_unsigned),
    .io_control_bits_alu_fn(collectorUnit_2_io_control_bits_alu_fn),
    .io_control_bits_mem(collectorUnit_2_io_control_bits_mem),
    .io_control_bits_mul(collectorUnit_2_io_control_bits_mul),
    .io_control_bits_mem_cmd(collectorUnit_2_io_control_bits_mem_cmd),
    .io_control_bits_mop(collectorUnit_2_io_control_bits_mop),
    .io_control_bits_reg_idx1(collectorUnit_2_io_control_bits_reg_idx1),
    .io_control_bits_reg_idx2(collectorUnit_2_io_control_bits_reg_idx2),
    .io_control_bits_reg_idx3(collectorUnit_2_io_control_bits_reg_idx3),
    .io_control_bits_reg_idxw(collectorUnit_2_io_control_bits_reg_idxw),
    .io_control_bits_wfd(collectorUnit_2_io_control_bits_wfd),
    .io_control_bits_fence(collectorUnit_2_io_control_bits_fence),
    .io_control_bits_sfu(collectorUnit_2_io_control_bits_sfu),
    .io_control_bits_readmask(collectorUnit_2_io_control_bits_readmask),
    .io_control_bits_writemask(collectorUnit_2_io_control_bits_writemask),
    .io_control_bits_wxd(collectorUnit_2_io_control_bits_wxd),
    .io_control_bits_pc(collectorUnit_2_io_control_bits_pc),
    .io_control_bits_spike_info_pc(collectorUnit_2_io_control_bits_spike_info_pc),
    .io_control_bits_spike_info_inst(collectorUnit_2_io_control_bits_spike_info_inst),
    .io_bankIn_0_ready(collectorUnit_2_io_bankIn_0_ready),
    .io_bankIn_0_valid(collectorUnit_2_io_bankIn_0_valid),
    .io_bankIn_0_bits_regOrder(collectorUnit_2_io_bankIn_0_bits_regOrder),
    .io_bankIn_0_bits_data_0(collectorUnit_2_io_bankIn_0_bits_data_0),
    .io_bankIn_0_bits_data_1(collectorUnit_2_io_bankIn_0_bits_data_1),
    .io_bankIn_0_bits_data_2(collectorUnit_2_io_bankIn_0_bits_data_2),
    .io_bankIn_0_bits_data_3(collectorUnit_2_io_bankIn_0_bits_data_3),
    .io_bankIn_0_bits_v0_0(collectorUnit_2_io_bankIn_0_bits_v0_0),
    .io_bankIn_1_ready(collectorUnit_2_io_bankIn_1_ready),
    .io_bankIn_1_valid(collectorUnit_2_io_bankIn_1_valid),
    .io_bankIn_1_bits_regOrder(collectorUnit_2_io_bankIn_1_bits_regOrder),
    .io_bankIn_1_bits_data_0(collectorUnit_2_io_bankIn_1_bits_data_0),
    .io_bankIn_1_bits_data_1(collectorUnit_2_io_bankIn_1_bits_data_1),
    .io_bankIn_1_bits_data_2(collectorUnit_2_io_bankIn_1_bits_data_2),
    .io_bankIn_1_bits_data_3(collectorUnit_2_io_bankIn_1_bits_data_3),
    .io_bankIn_1_bits_v0_0(collectorUnit_2_io_bankIn_1_bits_v0_0),
    .io_bankIn_2_ready(collectorUnit_2_io_bankIn_2_ready),
    .io_bankIn_2_valid(collectorUnit_2_io_bankIn_2_valid),
    .io_bankIn_2_bits_regOrder(collectorUnit_2_io_bankIn_2_bits_regOrder),
    .io_bankIn_2_bits_data_0(collectorUnit_2_io_bankIn_2_bits_data_0),
    .io_bankIn_2_bits_data_1(collectorUnit_2_io_bankIn_2_bits_data_1),
    .io_bankIn_2_bits_data_2(collectorUnit_2_io_bankIn_2_bits_data_2),
    .io_bankIn_2_bits_data_3(collectorUnit_2_io_bankIn_2_bits_data_3),
    .io_bankIn_2_bits_v0_0(collectorUnit_2_io_bankIn_2_bits_v0_0),
    .io_bankIn_3_ready(collectorUnit_2_io_bankIn_3_ready),
    .io_bankIn_3_valid(collectorUnit_2_io_bankIn_3_valid),
    .io_bankIn_3_bits_regOrder(collectorUnit_2_io_bankIn_3_bits_regOrder),
    .io_bankIn_3_bits_data_0(collectorUnit_2_io_bankIn_3_bits_data_0),
    .io_bankIn_3_bits_data_1(collectorUnit_2_io_bankIn_3_bits_data_1),
    .io_bankIn_3_bits_data_2(collectorUnit_2_io_bankIn_3_bits_data_2),
    .io_bankIn_3_bits_data_3(collectorUnit_2_io_bankIn_3_bits_data_3),
    .io_bankIn_3_bits_v0_0(collectorUnit_2_io_bankIn_3_bits_v0_0),
    .io_issue_ready(collectorUnit_2_io_issue_ready),
    .io_issue_valid(collectorUnit_2_io_issue_valid),
    .io_issue_bits_alu_src1_0(collectorUnit_2_io_issue_bits_alu_src1_0),
    .io_issue_bits_alu_src1_1(collectorUnit_2_io_issue_bits_alu_src1_1),
    .io_issue_bits_alu_src1_2(collectorUnit_2_io_issue_bits_alu_src1_2),
    .io_issue_bits_alu_src1_3(collectorUnit_2_io_issue_bits_alu_src1_3),
    .io_issue_bits_alu_src2_0(collectorUnit_2_io_issue_bits_alu_src2_0),
    .io_issue_bits_alu_src2_1(collectorUnit_2_io_issue_bits_alu_src2_1),
    .io_issue_bits_alu_src2_2(collectorUnit_2_io_issue_bits_alu_src2_2),
    .io_issue_bits_alu_src2_3(collectorUnit_2_io_issue_bits_alu_src2_3),
    .io_issue_bits_alu_src3_0(collectorUnit_2_io_issue_bits_alu_src3_0),
    .io_issue_bits_alu_src3_1(collectorUnit_2_io_issue_bits_alu_src3_1),
    .io_issue_bits_alu_src3_2(collectorUnit_2_io_issue_bits_alu_src3_2),
    .io_issue_bits_alu_src3_3(collectorUnit_2_io_issue_bits_alu_src3_3),
    .io_issue_bits_mask_0(collectorUnit_2_io_issue_bits_mask_0),
    .io_issue_bits_mask_1(collectorUnit_2_io_issue_bits_mask_1),
    .io_issue_bits_mask_2(collectorUnit_2_io_issue_bits_mask_2),
    .io_issue_bits_mask_3(collectorUnit_2_io_issue_bits_mask_3),
    .io_issue_bits_control_inst(collectorUnit_2_io_issue_bits_control_inst),
    .io_issue_bits_control_wid(collectorUnit_2_io_issue_bits_control_wid),
    .io_issue_bits_control_fp(collectorUnit_2_io_issue_bits_control_fp),
    .io_issue_bits_control_branch(collectorUnit_2_io_issue_bits_control_branch),
    .io_issue_bits_control_simt_stack(collectorUnit_2_io_issue_bits_control_simt_stack),
    .io_issue_bits_control_simt_stack_op(collectorUnit_2_io_issue_bits_control_simt_stack_op),
    .io_issue_bits_control_barrier(collectorUnit_2_io_issue_bits_control_barrier),
    .io_issue_bits_control_csr(collectorUnit_2_io_issue_bits_control_csr),
    .io_issue_bits_control_reverse(collectorUnit_2_io_issue_bits_control_reverse),
    .io_issue_bits_control_sel_alu2(collectorUnit_2_io_issue_bits_control_sel_alu2),
    .io_issue_bits_control_sel_alu1(collectorUnit_2_io_issue_bits_control_sel_alu1),
    .io_issue_bits_control_isvec(collectorUnit_2_io_issue_bits_control_isvec),
    .io_issue_bits_control_sel_alu3(collectorUnit_2_io_issue_bits_control_sel_alu3),
    .io_issue_bits_control_mask(collectorUnit_2_io_issue_bits_control_mask),
    .io_issue_bits_control_sel_imm(collectorUnit_2_io_issue_bits_control_sel_imm),
    .io_issue_bits_control_mem_whb(collectorUnit_2_io_issue_bits_control_mem_whb),
    .io_issue_bits_control_mem_unsigned(collectorUnit_2_io_issue_bits_control_mem_unsigned),
    .io_issue_bits_control_alu_fn(collectorUnit_2_io_issue_bits_control_alu_fn),
    .io_issue_bits_control_mem(collectorUnit_2_io_issue_bits_control_mem),
    .io_issue_bits_control_mul(collectorUnit_2_io_issue_bits_control_mul),
    .io_issue_bits_control_mem_cmd(collectorUnit_2_io_issue_bits_control_mem_cmd),
    .io_issue_bits_control_mop(collectorUnit_2_io_issue_bits_control_mop),
    .io_issue_bits_control_reg_idx1(collectorUnit_2_io_issue_bits_control_reg_idx1),
    .io_issue_bits_control_reg_idx2(collectorUnit_2_io_issue_bits_control_reg_idx2),
    .io_issue_bits_control_reg_idx3(collectorUnit_2_io_issue_bits_control_reg_idx3),
    .io_issue_bits_control_reg_idxw(collectorUnit_2_io_issue_bits_control_reg_idxw),
    .io_issue_bits_control_wfd(collectorUnit_2_io_issue_bits_control_wfd),
    .io_issue_bits_control_fence(collectorUnit_2_io_issue_bits_control_fence),
    .io_issue_bits_control_sfu(collectorUnit_2_io_issue_bits_control_sfu),
    .io_issue_bits_control_readmask(collectorUnit_2_io_issue_bits_control_readmask),
    .io_issue_bits_control_writemask(collectorUnit_2_io_issue_bits_control_writemask),
    .io_issue_bits_control_wxd(collectorUnit_2_io_issue_bits_control_wxd),
    .io_issue_bits_control_pc(collectorUnit_2_io_issue_bits_control_pc),
    .io_issue_bits_control_spike_info_pc(collectorUnit_2_io_issue_bits_control_spike_info_pc),
    .io_issue_bits_control_spike_info_inst(collectorUnit_2_io_issue_bits_control_spike_info_inst),
    .io_outArbiterIO_0_valid(collectorUnit_2_io_outArbiterIO_0_valid),
    .io_outArbiterIO_0_bits_rsAddr(collectorUnit_2_io_outArbiterIO_0_bits_rsAddr),
    .io_outArbiterIO_0_bits_bankID(collectorUnit_2_io_outArbiterIO_0_bits_bankID),
    .io_outArbiterIO_0_bits_rsType(collectorUnit_2_io_outArbiterIO_0_bits_rsType),
    .io_outArbiterIO_1_valid(collectorUnit_2_io_outArbiterIO_1_valid),
    .io_outArbiterIO_1_bits_rsAddr(collectorUnit_2_io_outArbiterIO_1_bits_rsAddr),
    .io_outArbiterIO_1_bits_bankID(collectorUnit_2_io_outArbiterIO_1_bits_bankID),
    .io_outArbiterIO_1_bits_rsType(collectorUnit_2_io_outArbiterIO_1_bits_rsType),
    .io_outArbiterIO_2_valid(collectorUnit_2_io_outArbiterIO_2_valid),
    .io_outArbiterIO_2_bits_rsAddr(collectorUnit_2_io_outArbiterIO_2_bits_rsAddr),
    .io_outArbiterIO_2_bits_bankID(collectorUnit_2_io_outArbiterIO_2_bits_bankID),
    .io_outArbiterIO_2_bits_rsType(collectorUnit_2_io_outArbiterIO_2_bits_rsType),
    .io_outArbiterIO_3_valid(collectorUnit_2_io_outArbiterIO_3_valid),
    .io_outArbiterIO_3_bits_rsAddr(collectorUnit_2_io_outArbiterIO_3_bits_rsAddr),
    .io_outArbiterIO_3_bits_bankID(collectorUnit_2_io_outArbiterIO_3_bits_bankID)
  );
  collectorUnit collectorUnit_3 ( // @[operandCollector.scala 388:66]
    .clock(collectorUnit_3_clock),
    .reset(collectorUnit_3_reset),
    .io_control_ready(collectorUnit_3_io_control_ready),
    .io_control_valid(collectorUnit_3_io_control_valid),
    .io_control_bits_inst(collectorUnit_3_io_control_bits_inst),
    .io_control_bits_wid(collectorUnit_3_io_control_bits_wid),
    .io_control_bits_fp(collectorUnit_3_io_control_bits_fp),
    .io_control_bits_branch(collectorUnit_3_io_control_bits_branch),
    .io_control_bits_simt_stack(collectorUnit_3_io_control_bits_simt_stack),
    .io_control_bits_simt_stack_op(collectorUnit_3_io_control_bits_simt_stack_op),
    .io_control_bits_barrier(collectorUnit_3_io_control_bits_barrier),
    .io_control_bits_csr(collectorUnit_3_io_control_bits_csr),
    .io_control_bits_reverse(collectorUnit_3_io_control_bits_reverse),
    .io_control_bits_sel_alu2(collectorUnit_3_io_control_bits_sel_alu2),
    .io_control_bits_sel_alu1(collectorUnit_3_io_control_bits_sel_alu1),
    .io_control_bits_isvec(collectorUnit_3_io_control_bits_isvec),
    .io_control_bits_sel_alu3(collectorUnit_3_io_control_bits_sel_alu3),
    .io_control_bits_mask(collectorUnit_3_io_control_bits_mask),
    .io_control_bits_sel_imm(collectorUnit_3_io_control_bits_sel_imm),
    .io_control_bits_mem_whb(collectorUnit_3_io_control_bits_mem_whb),
    .io_control_bits_mem_unsigned(collectorUnit_3_io_control_bits_mem_unsigned),
    .io_control_bits_alu_fn(collectorUnit_3_io_control_bits_alu_fn),
    .io_control_bits_mem(collectorUnit_3_io_control_bits_mem),
    .io_control_bits_mul(collectorUnit_3_io_control_bits_mul),
    .io_control_bits_mem_cmd(collectorUnit_3_io_control_bits_mem_cmd),
    .io_control_bits_mop(collectorUnit_3_io_control_bits_mop),
    .io_control_bits_reg_idx1(collectorUnit_3_io_control_bits_reg_idx1),
    .io_control_bits_reg_idx2(collectorUnit_3_io_control_bits_reg_idx2),
    .io_control_bits_reg_idx3(collectorUnit_3_io_control_bits_reg_idx3),
    .io_control_bits_reg_idxw(collectorUnit_3_io_control_bits_reg_idxw),
    .io_control_bits_wfd(collectorUnit_3_io_control_bits_wfd),
    .io_control_bits_fence(collectorUnit_3_io_control_bits_fence),
    .io_control_bits_sfu(collectorUnit_3_io_control_bits_sfu),
    .io_control_bits_readmask(collectorUnit_3_io_control_bits_readmask),
    .io_control_bits_writemask(collectorUnit_3_io_control_bits_writemask),
    .io_control_bits_wxd(collectorUnit_3_io_control_bits_wxd),
    .io_control_bits_pc(collectorUnit_3_io_control_bits_pc),
    .io_control_bits_spike_info_pc(collectorUnit_3_io_control_bits_spike_info_pc),
    .io_control_bits_spike_info_inst(collectorUnit_3_io_control_bits_spike_info_inst),
    .io_bankIn_0_ready(collectorUnit_3_io_bankIn_0_ready),
    .io_bankIn_0_valid(collectorUnit_3_io_bankIn_0_valid),
    .io_bankIn_0_bits_regOrder(collectorUnit_3_io_bankIn_0_bits_regOrder),
    .io_bankIn_0_bits_data_0(collectorUnit_3_io_bankIn_0_bits_data_0),
    .io_bankIn_0_bits_data_1(collectorUnit_3_io_bankIn_0_bits_data_1),
    .io_bankIn_0_bits_data_2(collectorUnit_3_io_bankIn_0_bits_data_2),
    .io_bankIn_0_bits_data_3(collectorUnit_3_io_bankIn_0_bits_data_3),
    .io_bankIn_0_bits_v0_0(collectorUnit_3_io_bankIn_0_bits_v0_0),
    .io_bankIn_1_ready(collectorUnit_3_io_bankIn_1_ready),
    .io_bankIn_1_valid(collectorUnit_3_io_bankIn_1_valid),
    .io_bankIn_1_bits_regOrder(collectorUnit_3_io_bankIn_1_bits_regOrder),
    .io_bankIn_1_bits_data_0(collectorUnit_3_io_bankIn_1_bits_data_0),
    .io_bankIn_1_bits_data_1(collectorUnit_3_io_bankIn_1_bits_data_1),
    .io_bankIn_1_bits_data_2(collectorUnit_3_io_bankIn_1_bits_data_2),
    .io_bankIn_1_bits_data_3(collectorUnit_3_io_bankIn_1_bits_data_3),
    .io_bankIn_1_bits_v0_0(collectorUnit_3_io_bankIn_1_bits_v0_0),
    .io_bankIn_2_ready(collectorUnit_3_io_bankIn_2_ready),
    .io_bankIn_2_valid(collectorUnit_3_io_bankIn_2_valid),
    .io_bankIn_2_bits_regOrder(collectorUnit_3_io_bankIn_2_bits_regOrder),
    .io_bankIn_2_bits_data_0(collectorUnit_3_io_bankIn_2_bits_data_0),
    .io_bankIn_2_bits_data_1(collectorUnit_3_io_bankIn_2_bits_data_1),
    .io_bankIn_2_bits_data_2(collectorUnit_3_io_bankIn_2_bits_data_2),
    .io_bankIn_2_bits_data_3(collectorUnit_3_io_bankIn_2_bits_data_3),
    .io_bankIn_2_bits_v0_0(collectorUnit_3_io_bankIn_2_bits_v0_0),
    .io_bankIn_3_ready(collectorUnit_3_io_bankIn_3_ready),
    .io_bankIn_3_valid(collectorUnit_3_io_bankIn_3_valid),
    .io_bankIn_3_bits_regOrder(collectorUnit_3_io_bankIn_3_bits_regOrder),
    .io_bankIn_3_bits_data_0(collectorUnit_3_io_bankIn_3_bits_data_0),
    .io_bankIn_3_bits_data_1(collectorUnit_3_io_bankIn_3_bits_data_1),
    .io_bankIn_3_bits_data_2(collectorUnit_3_io_bankIn_3_bits_data_2),
    .io_bankIn_3_bits_data_3(collectorUnit_3_io_bankIn_3_bits_data_3),
    .io_bankIn_3_bits_v0_0(collectorUnit_3_io_bankIn_3_bits_v0_0),
    .io_issue_ready(collectorUnit_3_io_issue_ready),
    .io_issue_valid(collectorUnit_3_io_issue_valid),
    .io_issue_bits_alu_src1_0(collectorUnit_3_io_issue_bits_alu_src1_0),
    .io_issue_bits_alu_src1_1(collectorUnit_3_io_issue_bits_alu_src1_1),
    .io_issue_bits_alu_src1_2(collectorUnit_3_io_issue_bits_alu_src1_2),
    .io_issue_bits_alu_src1_3(collectorUnit_3_io_issue_bits_alu_src1_3),
    .io_issue_bits_alu_src2_0(collectorUnit_3_io_issue_bits_alu_src2_0),
    .io_issue_bits_alu_src2_1(collectorUnit_3_io_issue_bits_alu_src2_1),
    .io_issue_bits_alu_src2_2(collectorUnit_3_io_issue_bits_alu_src2_2),
    .io_issue_bits_alu_src2_3(collectorUnit_3_io_issue_bits_alu_src2_3),
    .io_issue_bits_alu_src3_0(collectorUnit_3_io_issue_bits_alu_src3_0),
    .io_issue_bits_alu_src3_1(collectorUnit_3_io_issue_bits_alu_src3_1),
    .io_issue_bits_alu_src3_2(collectorUnit_3_io_issue_bits_alu_src3_2),
    .io_issue_bits_alu_src3_3(collectorUnit_3_io_issue_bits_alu_src3_3),
    .io_issue_bits_mask_0(collectorUnit_3_io_issue_bits_mask_0),
    .io_issue_bits_mask_1(collectorUnit_3_io_issue_bits_mask_1),
    .io_issue_bits_mask_2(collectorUnit_3_io_issue_bits_mask_2),
    .io_issue_bits_mask_3(collectorUnit_3_io_issue_bits_mask_3),
    .io_issue_bits_control_inst(collectorUnit_3_io_issue_bits_control_inst),
    .io_issue_bits_control_wid(collectorUnit_3_io_issue_bits_control_wid),
    .io_issue_bits_control_fp(collectorUnit_3_io_issue_bits_control_fp),
    .io_issue_bits_control_branch(collectorUnit_3_io_issue_bits_control_branch),
    .io_issue_bits_control_simt_stack(collectorUnit_3_io_issue_bits_control_simt_stack),
    .io_issue_bits_control_simt_stack_op(collectorUnit_3_io_issue_bits_control_simt_stack_op),
    .io_issue_bits_control_barrier(collectorUnit_3_io_issue_bits_control_barrier),
    .io_issue_bits_control_csr(collectorUnit_3_io_issue_bits_control_csr),
    .io_issue_bits_control_reverse(collectorUnit_3_io_issue_bits_control_reverse),
    .io_issue_bits_control_sel_alu2(collectorUnit_3_io_issue_bits_control_sel_alu2),
    .io_issue_bits_control_sel_alu1(collectorUnit_3_io_issue_bits_control_sel_alu1),
    .io_issue_bits_control_isvec(collectorUnit_3_io_issue_bits_control_isvec),
    .io_issue_bits_control_sel_alu3(collectorUnit_3_io_issue_bits_control_sel_alu3),
    .io_issue_bits_control_mask(collectorUnit_3_io_issue_bits_control_mask),
    .io_issue_bits_control_sel_imm(collectorUnit_3_io_issue_bits_control_sel_imm),
    .io_issue_bits_control_mem_whb(collectorUnit_3_io_issue_bits_control_mem_whb),
    .io_issue_bits_control_mem_unsigned(collectorUnit_3_io_issue_bits_control_mem_unsigned),
    .io_issue_bits_control_alu_fn(collectorUnit_3_io_issue_bits_control_alu_fn),
    .io_issue_bits_control_mem(collectorUnit_3_io_issue_bits_control_mem),
    .io_issue_bits_control_mul(collectorUnit_3_io_issue_bits_control_mul),
    .io_issue_bits_control_mem_cmd(collectorUnit_3_io_issue_bits_control_mem_cmd),
    .io_issue_bits_control_mop(collectorUnit_3_io_issue_bits_control_mop),
    .io_issue_bits_control_reg_idx1(collectorUnit_3_io_issue_bits_control_reg_idx1),
    .io_issue_bits_control_reg_idx2(collectorUnit_3_io_issue_bits_control_reg_idx2),
    .io_issue_bits_control_reg_idx3(collectorUnit_3_io_issue_bits_control_reg_idx3),
    .io_issue_bits_control_reg_idxw(collectorUnit_3_io_issue_bits_control_reg_idxw),
    .io_issue_bits_control_wfd(collectorUnit_3_io_issue_bits_control_wfd),
    .io_issue_bits_control_fence(collectorUnit_3_io_issue_bits_control_fence),
    .io_issue_bits_control_sfu(collectorUnit_3_io_issue_bits_control_sfu),
    .io_issue_bits_control_readmask(collectorUnit_3_io_issue_bits_control_readmask),
    .io_issue_bits_control_writemask(collectorUnit_3_io_issue_bits_control_writemask),
    .io_issue_bits_control_wxd(collectorUnit_3_io_issue_bits_control_wxd),
    .io_issue_bits_control_pc(collectorUnit_3_io_issue_bits_control_pc),
    .io_issue_bits_control_spike_info_pc(collectorUnit_3_io_issue_bits_control_spike_info_pc),
    .io_issue_bits_control_spike_info_inst(collectorUnit_3_io_issue_bits_control_spike_info_inst),
    .io_outArbiterIO_0_valid(collectorUnit_3_io_outArbiterIO_0_valid),
    .io_outArbiterIO_0_bits_rsAddr(collectorUnit_3_io_outArbiterIO_0_bits_rsAddr),
    .io_outArbiterIO_0_bits_bankID(collectorUnit_3_io_outArbiterIO_0_bits_bankID),
    .io_outArbiterIO_0_bits_rsType(collectorUnit_3_io_outArbiterIO_0_bits_rsType),
    .io_outArbiterIO_1_valid(collectorUnit_3_io_outArbiterIO_1_valid),
    .io_outArbiterIO_1_bits_rsAddr(collectorUnit_3_io_outArbiterIO_1_bits_rsAddr),
    .io_outArbiterIO_1_bits_bankID(collectorUnit_3_io_outArbiterIO_1_bits_bankID),
    .io_outArbiterIO_1_bits_rsType(collectorUnit_3_io_outArbiterIO_1_bits_rsType),
    .io_outArbiterIO_2_valid(collectorUnit_3_io_outArbiterIO_2_valid),
    .io_outArbiterIO_2_bits_rsAddr(collectorUnit_3_io_outArbiterIO_2_bits_rsAddr),
    .io_outArbiterIO_2_bits_bankID(collectorUnit_3_io_outArbiterIO_2_bits_bankID),
    .io_outArbiterIO_2_bits_rsType(collectorUnit_3_io_outArbiterIO_2_bits_rsType),
    .io_outArbiterIO_3_valid(collectorUnit_3_io_outArbiterIO_3_valid),
    .io_outArbiterIO_3_bits_rsAddr(collectorUnit_3_io_outArbiterIO_3_bits_rsAddr),
    .io_outArbiterIO_3_bits_bankID(collectorUnit_3_io_outArbiterIO_3_bits_bankID)
  );
  operandArbiter Arbiter ( // @[operandCollector.scala 389:23]
    .clock(Arbiter_clock),
    .io_readArbiterIO_0_0_valid(Arbiter_io_readArbiterIO_0_0_valid),
    .io_readArbiterIO_0_0_bits_rsAddr(Arbiter_io_readArbiterIO_0_0_bits_rsAddr),
    .io_readArbiterIO_0_0_bits_bankID(Arbiter_io_readArbiterIO_0_0_bits_bankID),
    .io_readArbiterIO_0_0_bits_rsType(Arbiter_io_readArbiterIO_0_0_bits_rsType),
    .io_readArbiterIO_0_1_valid(Arbiter_io_readArbiterIO_0_1_valid),
    .io_readArbiterIO_0_1_bits_rsAddr(Arbiter_io_readArbiterIO_0_1_bits_rsAddr),
    .io_readArbiterIO_0_1_bits_bankID(Arbiter_io_readArbiterIO_0_1_bits_bankID),
    .io_readArbiterIO_0_1_bits_rsType(Arbiter_io_readArbiterIO_0_1_bits_rsType),
    .io_readArbiterIO_0_2_valid(Arbiter_io_readArbiterIO_0_2_valid),
    .io_readArbiterIO_0_2_bits_rsAddr(Arbiter_io_readArbiterIO_0_2_bits_rsAddr),
    .io_readArbiterIO_0_2_bits_bankID(Arbiter_io_readArbiterIO_0_2_bits_bankID),
    .io_readArbiterIO_0_2_bits_rsType(Arbiter_io_readArbiterIO_0_2_bits_rsType),
    .io_readArbiterIO_0_3_valid(Arbiter_io_readArbiterIO_0_3_valid),
    .io_readArbiterIO_0_3_bits_rsAddr(Arbiter_io_readArbiterIO_0_3_bits_rsAddr),
    .io_readArbiterIO_0_3_bits_bankID(Arbiter_io_readArbiterIO_0_3_bits_bankID),
    .io_readArbiterIO_1_0_valid(Arbiter_io_readArbiterIO_1_0_valid),
    .io_readArbiterIO_1_0_bits_rsAddr(Arbiter_io_readArbiterIO_1_0_bits_rsAddr),
    .io_readArbiterIO_1_0_bits_bankID(Arbiter_io_readArbiterIO_1_0_bits_bankID),
    .io_readArbiterIO_1_0_bits_rsType(Arbiter_io_readArbiterIO_1_0_bits_rsType),
    .io_readArbiterIO_1_1_valid(Arbiter_io_readArbiterIO_1_1_valid),
    .io_readArbiterIO_1_1_bits_rsAddr(Arbiter_io_readArbiterIO_1_1_bits_rsAddr),
    .io_readArbiterIO_1_1_bits_bankID(Arbiter_io_readArbiterIO_1_1_bits_bankID),
    .io_readArbiterIO_1_1_bits_rsType(Arbiter_io_readArbiterIO_1_1_bits_rsType),
    .io_readArbiterIO_1_2_valid(Arbiter_io_readArbiterIO_1_2_valid),
    .io_readArbiterIO_1_2_bits_rsAddr(Arbiter_io_readArbiterIO_1_2_bits_rsAddr),
    .io_readArbiterIO_1_2_bits_bankID(Arbiter_io_readArbiterIO_1_2_bits_bankID),
    .io_readArbiterIO_1_2_bits_rsType(Arbiter_io_readArbiterIO_1_2_bits_rsType),
    .io_readArbiterIO_1_3_valid(Arbiter_io_readArbiterIO_1_3_valid),
    .io_readArbiterIO_1_3_bits_rsAddr(Arbiter_io_readArbiterIO_1_3_bits_rsAddr),
    .io_readArbiterIO_1_3_bits_bankID(Arbiter_io_readArbiterIO_1_3_bits_bankID),
    .io_readArbiterIO_2_0_valid(Arbiter_io_readArbiterIO_2_0_valid),
    .io_readArbiterIO_2_0_bits_rsAddr(Arbiter_io_readArbiterIO_2_0_bits_rsAddr),
    .io_readArbiterIO_2_0_bits_bankID(Arbiter_io_readArbiterIO_2_0_bits_bankID),
    .io_readArbiterIO_2_0_bits_rsType(Arbiter_io_readArbiterIO_2_0_bits_rsType),
    .io_readArbiterIO_2_1_valid(Arbiter_io_readArbiterIO_2_1_valid),
    .io_readArbiterIO_2_1_bits_rsAddr(Arbiter_io_readArbiterIO_2_1_bits_rsAddr),
    .io_readArbiterIO_2_1_bits_bankID(Arbiter_io_readArbiterIO_2_1_bits_bankID),
    .io_readArbiterIO_2_1_bits_rsType(Arbiter_io_readArbiterIO_2_1_bits_rsType),
    .io_readArbiterIO_2_2_valid(Arbiter_io_readArbiterIO_2_2_valid),
    .io_readArbiterIO_2_2_bits_rsAddr(Arbiter_io_readArbiterIO_2_2_bits_rsAddr),
    .io_readArbiterIO_2_2_bits_bankID(Arbiter_io_readArbiterIO_2_2_bits_bankID),
    .io_readArbiterIO_2_2_bits_rsType(Arbiter_io_readArbiterIO_2_2_bits_rsType),
    .io_readArbiterIO_2_3_valid(Arbiter_io_readArbiterIO_2_3_valid),
    .io_readArbiterIO_2_3_bits_rsAddr(Arbiter_io_readArbiterIO_2_3_bits_rsAddr),
    .io_readArbiterIO_2_3_bits_bankID(Arbiter_io_readArbiterIO_2_3_bits_bankID),
    .io_readArbiterIO_3_0_valid(Arbiter_io_readArbiterIO_3_0_valid),
    .io_readArbiterIO_3_0_bits_rsAddr(Arbiter_io_readArbiterIO_3_0_bits_rsAddr),
    .io_readArbiterIO_3_0_bits_bankID(Arbiter_io_readArbiterIO_3_0_bits_bankID),
    .io_readArbiterIO_3_0_bits_rsType(Arbiter_io_readArbiterIO_3_0_bits_rsType),
    .io_readArbiterIO_3_1_valid(Arbiter_io_readArbiterIO_3_1_valid),
    .io_readArbiterIO_3_1_bits_rsAddr(Arbiter_io_readArbiterIO_3_1_bits_rsAddr),
    .io_readArbiterIO_3_1_bits_bankID(Arbiter_io_readArbiterIO_3_1_bits_bankID),
    .io_readArbiterIO_3_1_bits_rsType(Arbiter_io_readArbiterIO_3_1_bits_rsType),
    .io_readArbiterIO_3_2_valid(Arbiter_io_readArbiterIO_3_2_valid),
    .io_readArbiterIO_3_2_bits_rsAddr(Arbiter_io_readArbiterIO_3_2_bits_rsAddr),
    .io_readArbiterIO_3_2_bits_bankID(Arbiter_io_readArbiterIO_3_2_bits_bankID),
    .io_readArbiterIO_3_2_bits_rsType(Arbiter_io_readArbiterIO_3_2_bits_rsType),
    .io_readArbiterIO_3_3_valid(Arbiter_io_readArbiterIO_3_3_valid),
    .io_readArbiterIO_3_3_bits_rsAddr(Arbiter_io_readArbiterIO_3_3_bits_rsAddr),
    .io_readArbiterIO_3_3_bits_bankID(Arbiter_io_readArbiterIO_3_3_bits_bankID),
    .io_readArbiterOut_0_valid(Arbiter_io_readArbiterOut_0_valid),
    .io_readArbiterOut_0_bits_rsAddr(Arbiter_io_readArbiterOut_0_bits_rsAddr),
    .io_readArbiterOut_0_bits_rsType(Arbiter_io_readArbiterOut_0_bits_rsType),
    .io_readArbiterOut_1_valid(Arbiter_io_readArbiterOut_1_valid),
    .io_readArbiterOut_1_bits_rsAddr(Arbiter_io_readArbiterOut_1_bits_rsAddr),
    .io_readArbiterOut_1_bits_rsType(Arbiter_io_readArbiterOut_1_bits_rsType),
    .io_readArbiterOut_2_valid(Arbiter_io_readArbiterOut_2_valid),
    .io_readArbiterOut_2_bits_rsAddr(Arbiter_io_readArbiterOut_2_bits_rsAddr),
    .io_readArbiterOut_2_bits_rsType(Arbiter_io_readArbiterOut_2_bits_rsType),
    .io_readArbiterOut_3_valid(Arbiter_io_readArbiterOut_3_valid),
    .io_readArbiterOut_3_bits_rsAddr(Arbiter_io_readArbiterOut_3_bits_rsAddr),
    .io_readArbiterOut_3_bits_rsType(Arbiter_io_readArbiterOut_3_bits_rsType),
    .io_readchosen_0(Arbiter_io_readchosen_0),
    .io_readchosen_1(Arbiter_io_readchosen_1),
    .io_readchosen_2(Arbiter_io_readchosen_2),
    .io_readchosen_3(Arbiter_io_readchosen_3)
  );
  FloatRegFileBank FloatRegFileBank ( // @[operandCollector.scala 390:53]
    .clock(FloatRegFileBank_clock),
    .io_v0_0(FloatRegFileBank_io_v0_0),
    .io_rs_0(FloatRegFileBank_io_rs_0),
    .io_rs_1(FloatRegFileBank_io_rs_1),
    .io_rs_2(FloatRegFileBank_io_rs_2),
    .io_rs_3(FloatRegFileBank_io_rs_3),
    .io_rsidx(FloatRegFileBank_io_rsidx),
    .io_rd_0(FloatRegFileBank_io_rd_0),
    .io_rd_1(FloatRegFileBank_io_rd_1),
    .io_rd_2(FloatRegFileBank_io_rd_2),
    .io_rd_3(FloatRegFileBank_io_rd_3),
    .io_rdidx(FloatRegFileBank_io_rdidx),
    .io_rdwen(FloatRegFileBank_io_rdwen),
    .io_rdwmask_0(FloatRegFileBank_io_rdwmask_0),
    .io_rdwmask_1(FloatRegFileBank_io_rdwmask_1),
    .io_rdwmask_2(FloatRegFileBank_io_rdwmask_2),
    .io_rdwmask_3(FloatRegFileBank_io_rdwmask_3)
  );
  FloatRegFileBank FloatRegFileBank_1 ( // @[operandCollector.scala 390:53]
    .clock(FloatRegFileBank_1_clock),
    .io_v0_0(FloatRegFileBank_1_io_v0_0),
    .io_rs_0(FloatRegFileBank_1_io_rs_0),
    .io_rs_1(FloatRegFileBank_1_io_rs_1),
    .io_rs_2(FloatRegFileBank_1_io_rs_2),
    .io_rs_3(FloatRegFileBank_1_io_rs_3),
    .io_rsidx(FloatRegFileBank_1_io_rsidx),
    .io_rd_0(FloatRegFileBank_1_io_rd_0),
    .io_rd_1(FloatRegFileBank_1_io_rd_1),
    .io_rd_2(FloatRegFileBank_1_io_rd_2),
    .io_rd_3(FloatRegFileBank_1_io_rd_3),
    .io_rdidx(FloatRegFileBank_1_io_rdidx),
    .io_rdwen(FloatRegFileBank_1_io_rdwen),
    .io_rdwmask_0(FloatRegFileBank_1_io_rdwmask_0),
    .io_rdwmask_1(FloatRegFileBank_1_io_rdwmask_1),
    .io_rdwmask_2(FloatRegFileBank_1_io_rdwmask_2),
    .io_rdwmask_3(FloatRegFileBank_1_io_rdwmask_3)
  );
  FloatRegFileBank FloatRegFileBank_2 ( // @[operandCollector.scala 390:53]
    .clock(FloatRegFileBank_2_clock),
    .io_v0_0(FloatRegFileBank_2_io_v0_0),
    .io_rs_0(FloatRegFileBank_2_io_rs_0),
    .io_rs_1(FloatRegFileBank_2_io_rs_1),
    .io_rs_2(FloatRegFileBank_2_io_rs_2),
    .io_rs_3(FloatRegFileBank_2_io_rs_3),
    .io_rsidx(FloatRegFileBank_2_io_rsidx),
    .io_rd_0(FloatRegFileBank_2_io_rd_0),
    .io_rd_1(FloatRegFileBank_2_io_rd_1),
    .io_rd_2(FloatRegFileBank_2_io_rd_2),
    .io_rd_3(FloatRegFileBank_2_io_rd_3),
    .io_rdidx(FloatRegFileBank_2_io_rdidx),
    .io_rdwen(FloatRegFileBank_2_io_rdwen),
    .io_rdwmask_0(FloatRegFileBank_2_io_rdwmask_0),
    .io_rdwmask_1(FloatRegFileBank_2_io_rdwmask_1),
    .io_rdwmask_2(FloatRegFileBank_2_io_rdwmask_2),
    .io_rdwmask_3(FloatRegFileBank_2_io_rdwmask_3)
  );
  FloatRegFileBank FloatRegFileBank_3 ( // @[operandCollector.scala 390:53]
    .clock(FloatRegFileBank_3_clock),
    .io_v0_0(FloatRegFileBank_3_io_v0_0),
    .io_rs_0(FloatRegFileBank_3_io_rs_0),
    .io_rs_1(FloatRegFileBank_3_io_rs_1),
    .io_rs_2(FloatRegFileBank_3_io_rs_2),
    .io_rs_3(FloatRegFileBank_3_io_rs_3),
    .io_rsidx(FloatRegFileBank_3_io_rsidx),
    .io_rd_0(FloatRegFileBank_3_io_rd_0),
    .io_rd_1(FloatRegFileBank_3_io_rd_1),
    .io_rd_2(FloatRegFileBank_3_io_rd_2),
    .io_rd_3(FloatRegFileBank_3_io_rd_3),
    .io_rdidx(FloatRegFileBank_3_io_rdidx),
    .io_rdwen(FloatRegFileBank_3_io_rdwen),
    .io_rdwmask_0(FloatRegFileBank_3_io_rdwmask_0),
    .io_rdwmask_1(FloatRegFileBank_3_io_rdwmask_1),
    .io_rdwmask_2(FloatRegFileBank_3_io_rdwmask_2),
    .io_rdwmask_3(FloatRegFileBank_3_io_rdwmask_3)
  );
  RegFileBank RegFileBank ( // @[operandCollector.scala 391:53]
    .clock(RegFileBank_clock),
    .io_rs(RegFileBank_io_rs),
    .io_rsidx(RegFileBank_io_rsidx),
    .io_rd(RegFileBank_io_rd),
    .io_rdidx(RegFileBank_io_rdidx),
    .io_rdwen(RegFileBank_io_rdwen)
  );
  RegFileBank RegFileBank_1 ( // @[operandCollector.scala 391:53]
    .clock(RegFileBank_1_clock),
    .io_rs(RegFileBank_1_io_rs),
    .io_rsidx(RegFileBank_1_io_rsidx),
    .io_rd(RegFileBank_1_io_rd),
    .io_rdidx(RegFileBank_1_io_rdidx),
    .io_rdwen(RegFileBank_1_io_rdwen)
  );
  RegFileBank RegFileBank_2 ( // @[operandCollector.scala 391:53]
    .clock(RegFileBank_2_clock),
    .io_rs(RegFileBank_2_io_rs),
    .io_rsidx(RegFileBank_2_io_rsidx),
    .io_rd(RegFileBank_2_io_rd),
    .io_rdidx(RegFileBank_2_io_rdidx),
    .io_rdwen(RegFileBank_2_io_rdwen)
  );
  RegFileBank RegFileBank_3 ( // @[operandCollector.scala 391:53]
    .clock(RegFileBank_3_clock),
    .io_rs(RegFileBank_3_io_rs),
    .io_rsidx(RegFileBank_3_io_rsidx),
    .io_rd(RegFileBank_3_io_rd),
    .io_rdidx(RegFileBank_3_io_rdidx),
    .io_rdwen(RegFileBank_3_io_rdwen)
  );
  crossBar crossBar ( // @[operandCollector.scala 392:24]
    .io_chosen_0(crossBar_io_chosen_0),
    .io_chosen_1(crossBar_io_chosen_1),
    .io_chosen_2(crossBar_io_chosen_2),
    .io_chosen_3(crossBar_io_chosen_3),
    .io_validArbiter_0(crossBar_io_validArbiter_0),
    .io_validArbiter_1(crossBar_io_validArbiter_1),
    .io_validArbiter_2(crossBar_io_validArbiter_2),
    .io_validArbiter_3(crossBar_io_validArbiter_3),
    .io_dataIn_rs_0_0(crossBar_io_dataIn_rs_0_0),
    .io_dataIn_rs_0_1(crossBar_io_dataIn_rs_0_1),
    .io_dataIn_rs_0_2(crossBar_io_dataIn_rs_0_2),
    .io_dataIn_rs_0_3(crossBar_io_dataIn_rs_0_3),
    .io_dataIn_rs_1_0(crossBar_io_dataIn_rs_1_0),
    .io_dataIn_rs_1_1(crossBar_io_dataIn_rs_1_1),
    .io_dataIn_rs_1_2(crossBar_io_dataIn_rs_1_2),
    .io_dataIn_rs_1_3(crossBar_io_dataIn_rs_1_3),
    .io_dataIn_rs_2_0(crossBar_io_dataIn_rs_2_0),
    .io_dataIn_rs_2_1(crossBar_io_dataIn_rs_2_1),
    .io_dataIn_rs_2_2(crossBar_io_dataIn_rs_2_2),
    .io_dataIn_rs_2_3(crossBar_io_dataIn_rs_2_3),
    .io_dataIn_rs_3_0(crossBar_io_dataIn_rs_3_0),
    .io_dataIn_rs_3_1(crossBar_io_dataIn_rs_3_1),
    .io_dataIn_rs_3_2(crossBar_io_dataIn_rs_3_2),
    .io_dataIn_rs_3_3(crossBar_io_dataIn_rs_3_3),
    .io_dataIn_v0_0_0(crossBar_io_dataIn_v0_0_0),
    .io_dataIn_v0_1_0(crossBar_io_dataIn_v0_1_0),
    .io_dataIn_v0_2_0(crossBar_io_dataIn_v0_2_0),
    .io_dataIn_v0_3_0(crossBar_io_dataIn_v0_3_0),
    .io_out_0_0_valid(crossBar_io_out_0_0_valid),
    .io_out_0_0_bits_regOrder(crossBar_io_out_0_0_bits_regOrder),
    .io_out_0_0_bits_data_0(crossBar_io_out_0_0_bits_data_0),
    .io_out_0_0_bits_data_1(crossBar_io_out_0_0_bits_data_1),
    .io_out_0_0_bits_data_2(crossBar_io_out_0_0_bits_data_2),
    .io_out_0_0_bits_data_3(crossBar_io_out_0_0_bits_data_3),
    .io_out_0_0_bits_v0_0(crossBar_io_out_0_0_bits_v0_0),
    .io_out_0_1_valid(crossBar_io_out_0_1_valid),
    .io_out_0_1_bits_regOrder(crossBar_io_out_0_1_bits_regOrder),
    .io_out_0_1_bits_data_0(crossBar_io_out_0_1_bits_data_0),
    .io_out_0_1_bits_data_1(crossBar_io_out_0_1_bits_data_1),
    .io_out_0_1_bits_data_2(crossBar_io_out_0_1_bits_data_2),
    .io_out_0_1_bits_data_3(crossBar_io_out_0_1_bits_data_3),
    .io_out_0_1_bits_v0_0(crossBar_io_out_0_1_bits_v0_0),
    .io_out_0_2_valid(crossBar_io_out_0_2_valid),
    .io_out_0_2_bits_regOrder(crossBar_io_out_0_2_bits_regOrder),
    .io_out_0_2_bits_data_0(crossBar_io_out_0_2_bits_data_0),
    .io_out_0_2_bits_data_1(crossBar_io_out_0_2_bits_data_1),
    .io_out_0_2_bits_data_2(crossBar_io_out_0_2_bits_data_2),
    .io_out_0_2_bits_data_3(crossBar_io_out_0_2_bits_data_3),
    .io_out_0_2_bits_v0_0(crossBar_io_out_0_2_bits_v0_0),
    .io_out_0_3_valid(crossBar_io_out_0_3_valid),
    .io_out_0_3_bits_regOrder(crossBar_io_out_0_3_bits_regOrder),
    .io_out_0_3_bits_data_0(crossBar_io_out_0_3_bits_data_0),
    .io_out_0_3_bits_data_1(crossBar_io_out_0_3_bits_data_1),
    .io_out_0_3_bits_data_2(crossBar_io_out_0_3_bits_data_2),
    .io_out_0_3_bits_data_3(crossBar_io_out_0_3_bits_data_3),
    .io_out_0_3_bits_v0_0(crossBar_io_out_0_3_bits_v0_0),
    .io_out_1_0_valid(crossBar_io_out_1_0_valid),
    .io_out_1_0_bits_regOrder(crossBar_io_out_1_0_bits_regOrder),
    .io_out_1_0_bits_data_0(crossBar_io_out_1_0_bits_data_0),
    .io_out_1_0_bits_data_1(crossBar_io_out_1_0_bits_data_1),
    .io_out_1_0_bits_data_2(crossBar_io_out_1_0_bits_data_2),
    .io_out_1_0_bits_data_3(crossBar_io_out_1_0_bits_data_3),
    .io_out_1_0_bits_v0_0(crossBar_io_out_1_0_bits_v0_0),
    .io_out_1_1_valid(crossBar_io_out_1_1_valid),
    .io_out_1_1_bits_regOrder(crossBar_io_out_1_1_bits_regOrder),
    .io_out_1_1_bits_data_0(crossBar_io_out_1_1_bits_data_0),
    .io_out_1_1_bits_data_1(crossBar_io_out_1_1_bits_data_1),
    .io_out_1_1_bits_data_2(crossBar_io_out_1_1_bits_data_2),
    .io_out_1_1_bits_data_3(crossBar_io_out_1_1_bits_data_3),
    .io_out_1_1_bits_v0_0(crossBar_io_out_1_1_bits_v0_0),
    .io_out_1_2_valid(crossBar_io_out_1_2_valid),
    .io_out_1_2_bits_regOrder(crossBar_io_out_1_2_bits_regOrder),
    .io_out_1_2_bits_data_0(crossBar_io_out_1_2_bits_data_0),
    .io_out_1_2_bits_data_1(crossBar_io_out_1_2_bits_data_1),
    .io_out_1_2_bits_data_2(crossBar_io_out_1_2_bits_data_2),
    .io_out_1_2_bits_data_3(crossBar_io_out_1_2_bits_data_3),
    .io_out_1_2_bits_v0_0(crossBar_io_out_1_2_bits_v0_0),
    .io_out_1_3_valid(crossBar_io_out_1_3_valid),
    .io_out_1_3_bits_regOrder(crossBar_io_out_1_3_bits_regOrder),
    .io_out_1_3_bits_data_0(crossBar_io_out_1_3_bits_data_0),
    .io_out_1_3_bits_data_1(crossBar_io_out_1_3_bits_data_1),
    .io_out_1_3_bits_data_2(crossBar_io_out_1_3_bits_data_2),
    .io_out_1_3_bits_data_3(crossBar_io_out_1_3_bits_data_3),
    .io_out_1_3_bits_v0_0(crossBar_io_out_1_3_bits_v0_0),
    .io_out_2_0_valid(crossBar_io_out_2_0_valid),
    .io_out_2_0_bits_regOrder(crossBar_io_out_2_0_bits_regOrder),
    .io_out_2_0_bits_data_0(crossBar_io_out_2_0_bits_data_0),
    .io_out_2_0_bits_data_1(crossBar_io_out_2_0_bits_data_1),
    .io_out_2_0_bits_data_2(crossBar_io_out_2_0_bits_data_2),
    .io_out_2_0_bits_data_3(crossBar_io_out_2_0_bits_data_3),
    .io_out_2_0_bits_v0_0(crossBar_io_out_2_0_bits_v0_0),
    .io_out_2_1_valid(crossBar_io_out_2_1_valid),
    .io_out_2_1_bits_regOrder(crossBar_io_out_2_1_bits_regOrder),
    .io_out_2_1_bits_data_0(crossBar_io_out_2_1_bits_data_0),
    .io_out_2_1_bits_data_1(crossBar_io_out_2_1_bits_data_1),
    .io_out_2_1_bits_data_2(crossBar_io_out_2_1_bits_data_2),
    .io_out_2_1_bits_data_3(crossBar_io_out_2_1_bits_data_3),
    .io_out_2_1_bits_v0_0(crossBar_io_out_2_1_bits_v0_0),
    .io_out_2_2_valid(crossBar_io_out_2_2_valid),
    .io_out_2_2_bits_regOrder(crossBar_io_out_2_2_bits_regOrder),
    .io_out_2_2_bits_data_0(crossBar_io_out_2_2_bits_data_0),
    .io_out_2_2_bits_data_1(crossBar_io_out_2_2_bits_data_1),
    .io_out_2_2_bits_data_2(crossBar_io_out_2_2_bits_data_2),
    .io_out_2_2_bits_data_3(crossBar_io_out_2_2_bits_data_3),
    .io_out_2_2_bits_v0_0(crossBar_io_out_2_2_bits_v0_0),
    .io_out_2_3_valid(crossBar_io_out_2_3_valid),
    .io_out_2_3_bits_regOrder(crossBar_io_out_2_3_bits_regOrder),
    .io_out_2_3_bits_data_0(crossBar_io_out_2_3_bits_data_0),
    .io_out_2_3_bits_data_1(crossBar_io_out_2_3_bits_data_1),
    .io_out_2_3_bits_data_2(crossBar_io_out_2_3_bits_data_2),
    .io_out_2_3_bits_data_3(crossBar_io_out_2_3_bits_data_3),
    .io_out_2_3_bits_v0_0(crossBar_io_out_2_3_bits_v0_0),
    .io_out_3_0_valid(crossBar_io_out_3_0_valid),
    .io_out_3_0_bits_regOrder(crossBar_io_out_3_0_bits_regOrder),
    .io_out_3_0_bits_data_0(crossBar_io_out_3_0_bits_data_0),
    .io_out_3_0_bits_data_1(crossBar_io_out_3_0_bits_data_1),
    .io_out_3_0_bits_data_2(crossBar_io_out_3_0_bits_data_2),
    .io_out_3_0_bits_data_3(crossBar_io_out_3_0_bits_data_3),
    .io_out_3_0_bits_v0_0(crossBar_io_out_3_0_bits_v0_0),
    .io_out_3_1_valid(crossBar_io_out_3_1_valid),
    .io_out_3_1_bits_regOrder(crossBar_io_out_3_1_bits_regOrder),
    .io_out_3_1_bits_data_0(crossBar_io_out_3_1_bits_data_0),
    .io_out_3_1_bits_data_1(crossBar_io_out_3_1_bits_data_1),
    .io_out_3_1_bits_data_2(crossBar_io_out_3_1_bits_data_2),
    .io_out_3_1_bits_data_3(crossBar_io_out_3_1_bits_data_3),
    .io_out_3_1_bits_v0_0(crossBar_io_out_3_1_bits_v0_0),
    .io_out_3_2_valid(crossBar_io_out_3_2_valid),
    .io_out_3_2_bits_regOrder(crossBar_io_out_3_2_bits_regOrder),
    .io_out_3_2_bits_data_0(crossBar_io_out_3_2_bits_data_0),
    .io_out_3_2_bits_data_1(crossBar_io_out_3_2_bits_data_1),
    .io_out_3_2_bits_data_2(crossBar_io_out_3_2_bits_data_2),
    .io_out_3_2_bits_data_3(crossBar_io_out_3_2_bits_data_3),
    .io_out_3_2_bits_v0_0(crossBar_io_out_3_2_bits_v0_0),
    .io_out_3_3_valid(crossBar_io_out_3_3_valid),
    .io_out_3_3_bits_regOrder(crossBar_io_out_3_3_bits_regOrder),
    .io_out_3_3_bits_data_0(crossBar_io_out_3_3_bits_data_0),
    .io_out_3_3_bits_data_1(crossBar_io_out_3_3_bits_data_1),
    .io_out_3_3_bits_data_2(crossBar_io_out_3_3_bits_data_2),
    .io_out_3_3_bits_data_3(crossBar_io_out_3_3_bits_data_3),
    .io_out_3_3_bits_v0_0(crossBar_io_out_3_3_bits_v0_0)
  );
  instDemux Demux ( // @[operandCollector.scala 393:21]
    .io_in_ready(Demux_io_in_ready),
    .io_in_valid(Demux_io_in_valid),
    .io_in_bits_inst(Demux_io_in_bits_inst),
    .io_in_bits_wid(Demux_io_in_bits_wid),
    .io_in_bits_fp(Demux_io_in_bits_fp),
    .io_in_bits_branch(Demux_io_in_bits_branch),
    .io_in_bits_simt_stack(Demux_io_in_bits_simt_stack),
    .io_in_bits_simt_stack_op(Demux_io_in_bits_simt_stack_op),
    .io_in_bits_barrier(Demux_io_in_bits_barrier),
    .io_in_bits_csr(Demux_io_in_bits_csr),
    .io_in_bits_reverse(Demux_io_in_bits_reverse),
    .io_in_bits_sel_alu2(Demux_io_in_bits_sel_alu2),
    .io_in_bits_sel_alu1(Demux_io_in_bits_sel_alu1),
    .io_in_bits_isvec(Demux_io_in_bits_isvec),
    .io_in_bits_sel_alu3(Demux_io_in_bits_sel_alu3),
    .io_in_bits_mask(Demux_io_in_bits_mask),
    .io_in_bits_sel_imm(Demux_io_in_bits_sel_imm),
    .io_in_bits_mem_whb(Demux_io_in_bits_mem_whb),
    .io_in_bits_mem_unsigned(Demux_io_in_bits_mem_unsigned),
    .io_in_bits_alu_fn(Demux_io_in_bits_alu_fn),
    .io_in_bits_mem(Demux_io_in_bits_mem),
    .io_in_bits_mul(Demux_io_in_bits_mul),
    .io_in_bits_mem_cmd(Demux_io_in_bits_mem_cmd),
    .io_in_bits_mop(Demux_io_in_bits_mop),
    .io_in_bits_reg_idx1(Demux_io_in_bits_reg_idx1),
    .io_in_bits_reg_idx2(Demux_io_in_bits_reg_idx2),
    .io_in_bits_reg_idx3(Demux_io_in_bits_reg_idx3),
    .io_in_bits_reg_idxw(Demux_io_in_bits_reg_idxw),
    .io_in_bits_wfd(Demux_io_in_bits_wfd),
    .io_in_bits_fence(Demux_io_in_bits_fence),
    .io_in_bits_sfu(Demux_io_in_bits_sfu),
    .io_in_bits_readmask(Demux_io_in_bits_readmask),
    .io_in_bits_writemask(Demux_io_in_bits_writemask),
    .io_in_bits_wxd(Demux_io_in_bits_wxd),
    .io_in_bits_pc(Demux_io_in_bits_pc),
    .io_in_bits_spike_info_pc(Demux_io_in_bits_spike_info_pc),
    .io_in_bits_spike_info_inst(Demux_io_in_bits_spike_info_inst),
    .io_out_0_ready(Demux_io_out_0_ready),
    .io_out_0_valid(Demux_io_out_0_valid),
    .io_out_0_bits_inst(Demux_io_out_0_bits_inst),
    .io_out_0_bits_wid(Demux_io_out_0_bits_wid),
    .io_out_0_bits_fp(Demux_io_out_0_bits_fp),
    .io_out_0_bits_branch(Demux_io_out_0_bits_branch),
    .io_out_0_bits_simt_stack(Demux_io_out_0_bits_simt_stack),
    .io_out_0_bits_simt_stack_op(Demux_io_out_0_bits_simt_stack_op),
    .io_out_0_bits_barrier(Demux_io_out_0_bits_barrier),
    .io_out_0_bits_csr(Demux_io_out_0_bits_csr),
    .io_out_0_bits_reverse(Demux_io_out_0_bits_reverse),
    .io_out_0_bits_sel_alu2(Demux_io_out_0_bits_sel_alu2),
    .io_out_0_bits_sel_alu1(Demux_io_out_0_bits_sel_alu1),
    .io_out_0_bits_isvec(Demux_io_out_0_bits_isvec),
    .io_out_0_bits_sel_alu3(Demux_io_out_0_bits_sel_alu3),
    .io_out_0_bits_mask(Demux_io_out_0_bits_mask),
    .io_out_0_bits_sel_imm(Demux_io_out_0_bits_sel_imm),
    .io_out_0_bits_mem_whb(Demux_io_out_0_bits_mem_whb),
    .io_out_0_bits_mem_unsigned(Demux_io_out_0_bits_mem_unsigned),
    .io_out_0_bits_alu_fn(Demux_io_out_0_bits_alu_fn),
    .io_out_0_bits_mem(Demux_io_out_0_bits_mem),
    .io_out_0_bits_mul(Demux_io_out_0_bits_mul),
    .io_out_0_bits_mem_cmd(Demux_io_out_0_bits_mem_cmd),
    .io_out_0_bits_mop(Demux_io_out_0_bits_mop),
    .io_out_0_bits_reg_idx1(Demux_io_out_0_bits_reg_idx1),
    .io_out_0_bits_reg_idx2(Demux_io_out_0_bits_reg_idx2),
    .io_out_0_bits_reg_idx3(Demux_io_out_0_bits_reg_idx3),
    .io_out_0_bits_reg_idxw(Demux_io_out_0_bits_reg_idxw),
    .io_out_0_bits_wfd(Demux_io_out_0_bits_wfd),
    .io_out_0_bits_fence(Demux_io_out_0_bits_fence),
    .io_out_0_bits_sfu(Demux_io_out_0_bits_sfu),
    .io_out_0_bits_readmask(Demux_io_out_0_bits_readmask),
    .io_out_0_bits_writemask(Demux_io_out_0_bits_writemask),
    .io_out_0_bits_wxd(Demux_io_out_0_bits_wxd),
    .io_out_0_bits_pc(Demux_io_out_0_bits_pc),
    .io_out_0_bits_spike_info_pc(Demux_io_out_0_bits_spike_info_pc),
    .io_out_0_bits_spike_info_inst(Demux_io_out_0_bits_spike_info_inst),
    .io_out_1_ready(Demux_io_out_1_ready),
    .io_out_1_valid(Demux_io_out_1_valid),
    .io_out_1_bits_inst(Demux_io_out_1_bits_inst),
    .io_out_1_bits_wid(Demux_io_out_1_bits_wid),
    .io_out_1_bits_fp(Demux_io_out_1_bits_fp),
    .io_out_1_bits_branch(Demux_io_out_1_bits_branch),
    .io_out_1_bits_simt_stack(Demux_io_out_1_bits_simt_stack),
    .io_out_1_bits_simt_stack_op(Demux_io_out_1_bits_simt_stack_op),
    .io_out_1_bits_barrier(Demux_io_out_1_bits_barrier),
    .io_out_1_bits_csr(Demux_io_out_1_bits_csr),
    .io_out_1_bits_reverse(Demux_io_out_1_bits_reverse),
    .io_out_1_bits_sel_alu2(Demux_io_out_1_bits_sel_alu2),
    .io_out_1_bits_sel_alu1(Demux_io_out_1_bits_sel_alu1),
    .io_out_1_bits_isvec(Demux_io_out_1_bits_isvec),
    .io_out_1_bits_sel_alu3(Demux_io_out_1_bits_sel_alu3),
    .io_out_1_bits_mask(Demux_io_out_1_bits_mask),
    .io_out_1_bits_sel_imm(Demux_io_out_1_bits_sel_imm),
    .io_out_1_bits_mem_whb(Demux_io_out_1_bits_mem_whb),
    .io_out_1_bits_mem_unsigned(Demux_io_out_1_bits_mem_unsigned),
    .io_out_1_bits_alu_fn(Demux_io_out_1_bits_alu_fn),
    .io_out_1_bits_mem(Demux_io_out_1_bits_mem),
    .io_out_1_bits_mul(Demux_io_out_1_bits_mul),
    .io_out_1_bits_mem_cmd(Demux_io_out_1_bits_mem_cmd),
    .io_out_1_bits_mop(Demux_io_out_1_bits_mop),
    .io_out_1_bits_reg_idx1(Demux_io_out_1_bits_reg_idx1),
    .io_out_1_bits_reg_idx2(Demux_io_out_1_bits_reg_idx2),
    .io_out_1_bits_reg_idx3(Demux_io_out_1_bits_reg_idx3),
    .io_out_1_bits_reg_idxw(Demux_io_out_1_bits_reg_idxw),
    .io_out_1_bits_wfd(Demux_io_out_1_bits_wfd),
    .io_out_1_bits_fence(Demux_io_out_1_bits_fence),
    .io_out_1_bits_sfu(Demux_io_out_1_bits_sfu),
    .io_out_1_bits_readmask(Demux_io_out_1_bits_readmask),
    .io_out_1_bits_writemask(Demux_io_out_1_bits_writemask),
    .io_out_1_bits_wxd(Demux_io_out_1_bits_wxd),
    .io_out_1_bits_pc(Demux_io_out_1_bits_pc),
    .io_out_1_bits_spike_info_pc(Demux_io_out_1_bits_spike_info_pc),
    .io_out_1_bits_spike_info_inst(Demux_io_out_1_bits_spike_info_inst),
    .io_out_2_ready(Demux_io_out_2_ready),
    .io_out_2_valid(Demux_io_out_2_valid),
    .io_out_2_bits_inst(Demux_io_out_2_bits_inst),
    .io_out_2_bits_wid(Demux_io_out_2_bits_wid),
    .io_out_2_bits_fp(Demux_io_out_2_bits_fp),
    .io_out_2_bits_branch(Demux_io_out_2_bits_branch),
    .io_out_2_bits_simt_stack(Demux_io_out_2_bits_simt_stack),
    .io_out_2_bits_simt_stack_op(Demux_io_out_2_bits_simt_stack_op),
    .io_out_2_bits_barrier(Demux_io_out_2_bits_barrier),
    .io_out_2_bits_csr(Demux_io_out_2_bits_csr),
    .io_out_2_bits_reverse(Demux_io_out_2_bits_reverse),
    .io_out_2_bits_sel_alu2(Demux_io_out_2_bits_sel_alu2),
    .io_out_2_bits_sel_alu1(Demux_io_out_2_bits_sel_alu1),
    .io_out_2_bits_isvec(Demux_io_out_2_bits_isvec),
    .io_out_2_bits_sel_alu3(Demux_io_out_2_bits_sel_alu3),
    .io_out_2_bits_mask(Demux_io_out_2_bits_mask),
    .io_out_2_bits_sel_imm(Demux_io_out_2_bits_sel_imm),
    .io_out_2_bits_mem_whb(Demux_io_out_2_bits_mem_whb),
    .io_out_2_bits_mem_unsigned(Demux_io_out_2_bits_mem_unsigned),
    .io_out_2_bits_alu_fn(Demux_io_out_2_bits_alu_fn),
    .io_out_2_bits_mem(Demux_io_out_2_bits_mem),
    .io_out_2_bits_mul(Demux_io_out_2_bits_mul),
    .io_out_2_bits_mem_cmd(Demux_io_out_2_bits_mem_cmd),
    .io_out_2_bits_mop(Demux_io_out_2_bits_mop),
    .io_out_2_bits_reg_idx1(Demux_io_out_2_bits_reg_idx1),
    .io_out_2_bits_reg_idx2(Demux_io_out_2_bits_reg_idx2),
    .io_out_2_bits_reg_idx3(Demux_io_out_2_bits_reg_idx3),
    .io_out_2_bits_reg_idxw(Demux_io_out_2_bits_reg_idxw),
    .io_out_2_bits_wfd(Demux_io_out_2_bits_wfd),
    .io_out_2_bits_fence(Demux_io_out_2_bits_fence),
    .io_out_2_bits_sfu(Demux_io_out_2_bits_sfu),
    .io_out_2_bits_readmask(Demux_io_out_2_bits_readmask),
    .io_out_2_bits_writemask(Demux_io_out_2_bits_writemask),
    .io_out_2_bits_wxd(Demux_io_out_2_bits_wxd),
    .io_out_2_bits_pc(Demux_io_out_2_bits_pc),
    .io_out_2_bits_spike_info_pc(Demux_io_out_2_bits_spike_info_pc),
    .io_out_2_bits_spike_info_inst(Demux_io_out_2_bits_spike_info_inst),
    .io_out_3_ready(Demux_io_out_3_ready),
    .io_out_3_valid(Demux_io_out_3_valid),
    .io_out_3_bits_inst(Demux_io_out_3_bits_inst),
    .io_out_3_bits_wid(Demux_io_out_3_bits_wid),
    .io_out_3_bits_fp(Demux_io_out_3_bits_fp),
    .io_out_3_bits_branch(Demux_io_out_3_bits_branch),
    .io_out_3_bits_simt_stack(Demux_io_out_3_bits_simt_stack),
    .io_out_3_bits_simt_stack_op(Demux_io_out_3_bits_simt_stack_op),
    .io_out_3_bits_barrier(Demux_io_out_3_bits_barrier),
    .io_out_3_bits_csr(Demux_io_out_3_bits_csr),
    .io_out_3_bits_reverse(Demux_io_out_3_bits_reverse),
    .io_out_3_bits_sel_alu2(Demux_io_out_3_bits_sel_alu2),
    .io_out_3_bits_sel_alu1(Demux_io_out_3_bits_sel_alu1),
    .io_out_3_bits_isvec(Demux_io_out_3_bits_isvec),
    .io_out_3_bits_sel_alu3(Demux_io_out_3_bits_sel_alu3),
    .io_out_3_bits_mask(Demux_io_out_3_bits_mask),
    .io_out_3_bits_sel_imm(Demux_io_out_3_bits_sel_imm),
    .io_out_3_bits_mem_whb(Demux_io_out_3_bits_mem_whb),
    .io_out_3_bits_mem_unsigned(Demux_io_out_3_bits_mem_unsigned),
    .io_out_3_bits_alu_fn(Demux_io_out_3_bits_alu_fn),
    .io_out_3_bits_mem(Demux_io_out_3_bits_mem),
    .io_out_3_bits_mul(Demux_io_out_3_bits_mul),
    .io_out_3_bits_mem_cmd(Demux_io_out_3_bits_mem_cmd),
    .io_out_3_bits_mop(Demux_io_out_3_bits_mop),
    .io_out_3_bits_reg_idx1(Demux_io_out_3_bits_reg_idx1),
    .io_out_3_bits_reg_idx2(Demux_io_out_3_bits_reg_idx2),
    .io_out_3_bits_reg_idx3(Demux_io_out_3_bits_reg_idx3),
    .io_out_3_bits_reg_idxw(Demux_io_out_3_bits_reg_idxw),
    .io_out_3_bits_wfd(Demux_io_out_3_bits_wfd),
    .io_out_3_bits_fence(Demux_io_out_3_bits_fence),
    .io_out_3_bits_sfu(Demux_io_out_3_bits_sfu),
    .io_out_3_bits_readmask(Demux_io_out_3_bits_readmask),
    .io_out_3_bits_writemask(Demux_io_out_3_bits_writemask),
    .io_out_3_bits_wxd(Demux_io_out_3_bits_wxd),
    .io_out_3_bits_pc(Demux_io_out_3_bits_pc),
    .io_out_3_bits_spike_info_pc(Demux_io_out_3_bits_spike_info_pc),
    .io_out_3_bits_spike_info_inst(Demux_io_out_3_bits_spike_info_inst)
  );
  Arbiter issueArbiter ( // @[operandCollector.scala 466:28]
    .io_in_0_ready(issueArbiter_io_in_0_ready),
    .io_in_0_valid(issueArbiter_io_in_0_valid),
    .io_in_0_bits_alu_src1_0(issueArbiter_io_in_0_bits_alu_src1_0),
    .io_in_0_bits_alu_src1_1(issueArbiter_io_in_0_bits_alu_src1_1),
    .io_in_0_bits_alu_src1_2(issueArbiter_io_in_0_bits_alu_src1_2),
    .io_in_0_bits_alu_src1_3(issueArbiter_io_in_0_bits_alu_src1_3),
    .io_in_0_bits_alu_src2_0(issueArbiter_io_in_0_bits_alu_src2_0),
    .io_in_0_bits_alu_src2_1(issueArbiter_io_in_0_bits_alu_src2_1),
    .io_in_0_bits_alu_src2_2(issueArbiter_io_in_0_bits_alu_src2_2),
    .io_in_0_bits_alu_src2_3(issueArbiter_io_in_0_bits_alu_src2_3),
    .io_in_0_bits_alu_src3_0(issueArbiter_io_in_0_bits_alu_src3_0),
    .io_in_0_bits_alu_src3_1(issueArbiter_io_in_0_bits_alu_src3_1),
    .io_in_0_bits_alu_src3_2(issueArbiter_io_in_0_bits_alu_src3_2),
    .io_in_0_bits_alu_src3_3(issueArbiter_io_in_0_bits_alu_src3_3),
    .io_in_0_bits_mask_0(issueArbiter_io_in_0_bits_mask_0),
    .io_in_0_bits_mask_1(issueArbiter_io_in_0_bits_mask_1),
    .io_in_0_bits_mask_2(issueArbiter_io_in_0_bits_mask_2),
    .io_in_0_bits_mask_3(issueArbiter_io_in_0_bits_mask_3),
    .io_in_0_bits_control_inst(issueArbiter_io_in_0_bits_control_inst),
    .io_in_0_bits_control_wid(issueArbiter_io_in_0_bits_control_wid),
    .io_in_0_bits_control_fp(issueArbiter_io_in_0_bits_control_fp),
    .io_in_0_bits_control_branch(issueArbiter_io_in_0_bits_control_branch),
    .io_in_0_bits_control_simt_stack(issueArbiter_io_in_0_bits_control_simt_stack),
    .io_in_0_bits_control_simt_stack_op(issueArbiter_io_in_0_bits_control_simt_stack_op),
    .io_in_0_bits_control_barrier(issueArbiter_io_in_0_bits_control_barrier),
    .io_in_0_bits_control_csr(issueArbiter_io_in_0_bits_control_csr),
    .io_in_0_bits_control_reverse(issueArbiter_io_in_0_bits_control_reverse),
    .io_in_0_bits_control_sel_alu2(issueArbiter_io_in_0_bits_control_sel_alu2),
    .io_in_0_bits_control_sel_alu1(issueArbiter_io_in_0_bits_control_sel_alu1),
    .io_in_0_bits_control_isvec(issueArbiter_io_in_0_bits_control_isvec),
    .io_in_0_bits_control_sel_alu3(issueArbiter_io_in_0_bits_control_sel_alu3),
    .io_in_0_bits_control_mask(issueArbiter_io_in_0_bits_control_mask),
    .io_in_0_bits_control_sel_imm(issueArbiter_io_in_0_bits_control_sel_imm),
    .io_in_0_bits_control_mem_whb(issueArbiter_io_in_0_bits_control_mem_whb),
    .io_in_0_bits_control_mem_unsigned(issueArbiter_io_in_0_bits_control_mem_unsigned),
    .io_in_0_bits_control_alu_fn(issueArbiter_io_in_0_bits_control_alu_fn),
    .io_in_0_bits_control_mem(issueArbiter_io_in_0_bits_control_mem),
    .io_in_0_bits_control_mul(issueArbiter_io_in_0_bits_control_mul),
    .io_in_0_bits_control_mem_cmd(issueArbiter_io_in_0_bits_control_mem_cmd),
    .io_in_0_bits_control_mop(issueArbiter_io_in_0_bits_control_mop),
    .io_in_0_bits_control_reg_idx1(issueArbiter_io_in_0_bits_control_reg_idx1),
    .io_in_0_bits_control_reg_idx2(issueArbiter_io_in_0_bits_control_reg_idx2),
    .io_in_0_bits_control_reg_idx3(issueArbiter_io_in_0_bits_control_reg_idx3),
    .io_in_0_bits_control_reg_idxw(issueArbiter_io_in_0_bits_control_reg_idxw),
    .io_in_0_bits_control_wfd(issueArbiter_io_in_0_bits_control_wfd),
    .io_in_0_bits_control_fence(issueArbiter_io_in_0_bits_control_fence),
    .io_in_0_bits_control_sfu(issueArbiter_io_in_0_bits_control_sfu),
    .io_in_0_bits_control_readmask(issueArbiter_io_in_0_bits_control_readmask),
    .io_in_0_bits_control_writemask(issueArbiter_io_in_0_bits_control_writemask),
    .io_in_0_bits_control_wxd(issueArbiter_io_in_0_bits_control_wxd),
    .io_in_0_bits_control_pc(issueArbiter_io_in_0_bits_control_pc),
    .io_in_0_bits_control_spike_info_pc(issueArbiter_io_in_0_bits_control_spike_info_pc),
    .io_in_0_bits_control_spike_info_inst(issueArbiter_io_in_0_bits_control_spike_info_inst),
    .io_in_1_ready(issueArbiter_io_in_1_ready),
    .io_in_1_valid(issueArbiter_io_in_1_valid),
    .io_in_1_bits_alu_src1_0(issueArbiter_io_in_1_bits_alu_src1_0),
    .io_in_1_bits_alu_src1_1(issueArbiter_io_in_1_bits_alu_src1_1),
    .io_in_1_bits_alu_src1_2(issueArbiter_io_in_1_bits_alu_src1_2),
    .io_in_1_bits_alu_src1_3(issueArbiter_io_in_1_bits_alu_src1_3),
    .io_in_1_bits_alu_src2_0(issueArbiter_io_in_1_bits_alu_src2_0),
    .io_in_1_bits_alu_src2_1(issueArbiter_io_in_1_bits_alu_src2_1),
    .io_in_1_bits_alu_src2_2(issueArbiter_io_in_1_bits_alu_src2_2),
    .io_in_1_bits_alu_src2_3(issueArbiter_io_in_1_bits_alu_src2_3),
    .io_in_1_bits_alu_src3_0(issueArbiter_io_in_1_bits_alu_src3_0),
    .io_in_1_bits_alu_src3_1(issueArbiter_io_in_1_bits_alu_src3_1),
    .io_in_1_bits_alu_src3_2(issueArbiter_io_in_1_bits_alu_src3_2),
    .io_in_1_bits_alu_src3_3(issueArbiter_io_in_1_bits_alu_src3_3),
    .io_in_1_bits_mask_0(issueArbiter_io_in_1_bits_mask_0),
    .io_in_1_bits_mask_1(issueArbiter_io_in_1_bits_mask_1),
    .io_in_1_bits_mask_2(issueArbiter_io_in_1_bits_mask_2),
    .io_in_1_bits_mask_3(issueArbiter_io_in_1_bits_mask_3),
    .io_in_1_bits_control_inst(issueArbiter_io_in_1_bits_control_inst),
    .io_in_1_bits_control_wid(issueArbiter_io_in_1_bits_control_wid),
    .io_in_1_bits_control_fp(issueArbiter_io_in_1_bits_control_fp),
    .io_in_1_bits_control_branch(issueArbiter_io_in_1_bits_control_branch),
    .io_in_1_bits_control_simt_stack(issueArbiter_io_in_1_bits_control_simt_stack),
    .io_in_1_bits_control_simt_stack_op(issueArbiter_io_in_1_bits_control_simt_stack_op),
    .io_in_1_bits_control_barrier(issueArbiter_io_in_1_bits_control_barrier),
    .io_in_1_bits_control_csr(issueArbiter_io_in_1_bits_control_csr),
    .io_in_1_bits_control_reverse(issueArbiter_io_in_1_bits_control_reverse),
    .io_in_1_bits_control_sel_alu2(issueArbiter_io_in_1_bits_control_sel_alu2),
    .io_in_1_bits_control_sel_alu1(issueArbiter_io_in_1_bits_control_sel_alu1),
    .io_in_1_bits_control_isvec(issueArbiter_io_in_1_bits_control_isvec),
    .io_in_1_bits_control_sel_alu3(issueArbiter_io_in_1_bits_control_sel_alu3),
    .io_in_1_bits_control_mask(issueArbiter_io_in_1_bits_control_mask),
    .io_in_1_bits_control_sel_imm(issueArbiter_io_in_1_bits_control_sel_imm),
    .io_in_1_bits_control_mem_whb(issueArbiter_io_in_1_bits_control_mem_whb),
    .io_in_1_bits_control_mem_unsigned(issueArbiter_io_in_1_bits_control_mem_unsigned),
    .io_in_1_bits_control_alu_fn(issueArbiter_io_in_1_bits_control_alu_fn),
    .io_in_1_bits_control_mem(issueArbiter_io_in_1_bits_control_mem),
    .io_in_1_bits_control_mul(issueArbiter_io_in_1_bits_control_mul),
    .io_in_1_bits_control_mem_cmd(issueArbiter_io_in_1_bits_control_mem_cmd),
    .io_in_1_bits_control_mop(issueArbiter_io_in_1_bits_control_mop),
    .io_in_1_bits_control_reg_idx1(issueArbiter_io_in_1_bits_control_reg_idx1),
    .io_in_1_bits_control_reg_idx2(issueArbiter_io_in_1_bits_control_reg_idx2),
    .io_in_1_bits_control_reg_idx3(issueArbiter_io_in_1_bits_control_reg_idx3),
    .io_in_1_bits_control_reg_idxw(issueArbiter_io_in_1_bits_control_reg_idxw),
    .io_in_1_bits_control_wfd(issueArbiter_io_in_1_bits_control_wfd),
    .io_in_1_bits_control_fence(issueArbiter_io_in_1_bits_control_fence),
    .io_in_1_bits_control_sfu(issueArbiter_io_in_1_bits_control_sfu),
    .io_in_1_bits_control_readmask(issueArbiter_io_in_1_bits_control_readmask),
    .io_in_1_bits_control_writemask(issueArbiter_io_in_1_bits_control_writemask),
    .io_in_1_bits_control_wxd(issueArbiter_io_in_1_bits_control_wxd),
    .io_in_1_bits_control_pc(issueArbiter_io_in_1_bits_control_pc),
    .io_in_1_bits_control_spike_info_pc(issueArbiter_io_in_1_bits_control_spike_info_pc),
    .io_in_1_bits_control_spike_info_inst(issueArbiter_io_in_1_bits_control_spike_info_inst),
    .io_in_2_ready(issueArbiter_io_in_2_ready),
    .io_in_2_valid(issueArbiter_io_in_2_valid),
    .io_in_2_bits_alu_src1_0(issueArbiter_io_in_2_bits_alu_src1_0),
    .io_in_2_bits_alu_src1_1(issueArbiter_io_in_2_bits_alu_src1_1),
    .io_in_2_bits_alu_src1_2(issueArbiter_io_in_2_bits_alu_src1_2),
    .io_in_2_bits_alu_src1_3(issueArbiter_io_in_2_bits_alu_src1_3),
    .io_in_2_bits_alu_src2_0(issueArbiter_io_in_2_bits_alu_src2_0),
    .io_in_2_bits_alu_src2_1(issueArbiter_io_in_2_bits_alu_src2_1),
    .io_in_2_bits_alu_src2_2(issueArbiter_io_in_2_bits_alu_src2_2),
    .io_in_2_bits_alu_src2_3(issueArbiter_io_in_2_bits_alu_src2_3),
    .io_in_2_bits_alu_src3_0(issueArbiter_io_in_2_bits_alu_src3_0),
    .io_in_2_bits_alu_src3_1(issueArbiter_io_in_2_bits_alu_src3_1),
    .io_in_2_bits_alu_src3_2(issueArbiter_io_in_2_bits_alu_src3_2),
    .io_in_2_bits_alu_src3_3(issueArbiter_io_in_2_bits_alu_src3_3),
    .io_in_2_bits_mask_0(issueArbiter_io_in_2_bits_mask_0),
    .io_in_2_bits_mask_1(issueArbiter_io_in_2_bits_mask_1),
    .io_in_2_bits_mask_2(issueArbiter_io_in_2_bits_mask_2),
    .io_in_2_bits_mask_3(issueArbiter_io_in_2_bits_mask_3),
    .io_in_2_bits_control_inst(issueArbiter_io_in_2_bits_control_inst),
    .io_in_2_bits_control_wid(issueArbiter_io_in_2_bits_control_wid),
    .io_in_2_bits_control_fp(issueArbiter_io_in_2_bits_control_fp),
    .io_in_2_bits_control_branch(issueArbiter_io_in_2_bits_control_branch),
    .io_in_2_bits_control_simt_stack(issueArbiter_io_in_2_bits_control_simt_stack),
    .io_in_2_bits_control_simt_stack_op(issueArbiter_io_in_2_bits_control_simt_stack_op),
    .io_in_2_bits_control_barrier(issueArbiter_io_in_2_bits_control_barrier),
    .io_in_2_bits_control_csr(issueArbiter_io_in_2_bits_control_csr),
    .io_in_2_bits_control_reverse(issueArbiter_io_in_2_bits_control_reverse),
    .io_in_2_bits_control_sel_alu2(issueArbiter_io_in_2_bits_control_sel_alu2),
    .io_in_2_bits_control_sel_alu1(issueArbiter_io_in_2_bits_control_sel_alu1),
    .io_in_2_bits_control_isvec(issueArbiter_io_in_2_bits_control_isvec),
    .io_in_2_bits_control_sel_alu3(issueArbiter_io_in_2_bits_control_sel_alu3),
    .io_in_2_bits_control_mask(issueArbiter_io_in_2_bits_control_mask),
    .io_in_2_bits_control_sel_imm(issueArbiter_io_in_2_bits_control_sel_imm),
    .io_in_2_bits_control_mem_whb(issueArbiter_io_in_2_bits_control_mem_whb),
    .io_in_2_bits_control_mem_unsigned(issueArbiter_io_in_2_bits_control_mem_unsigned),
    .io_in_2_bits_control_alu_fn(issueArbiter_io_in_2_bits_control_alu_fn),
    .io_in_2_bits_control_mem(issueArbiter_io_in_2_bits_control_mem),
    .io_in_2_bits_control_mul(issueArbiter_io_in_2_bits_control_mul),
    .io_in_2_bits_control_mem_cmd(issueArbiter_io_in_2_bits_control_mem_cmd),
    .io_in_2_bits_control_mop(issueArbiter_io_in_2_bits_control_mop),
    .io_in_2_bits_control_reg_idx1(issueArbiter_io_in_2_bits_control_reg_idx1),
    .io_in_2_bits_control_reg_idx2(issueArbiter_io_in_2_bits_control_reg_idx2),
    .io_in_2_bits_control_reg_idx3(issueArbiter_io_in_2_bits_control_reg_idx3),
    .io_in_2_bits_control_reg_idxw(issueArbiter_io_in_2_bits_control_reg_idxw),
    .io_in_2_bits_control_wfd(issueArbiter_io_in_2_bits_control_wfd),
    .io_in_2_bits_control_fence(issueArbiter_io_in_2_bits_control_fence),
    .io_in_2_bits_control_sfu(issueArbiter_io_in_2_bits_control_sfu),
    .io_in_2_bits_control_readmask(issueArbiter_io_in_2_bits_control_readmask),
    .io_in_2_bits_control_writemask(issueArbiter_io_in_2_bits_control_writemask),
    .io_in_2_bits_control_wxd(issueArbiter_io_in_2_bits_control_wxd),
    .io_in_2_bits_control_pc(issueArbiter_io_in_2_bits_control_pc),
    .io_in_2_bits_control_spike_info_pc(issueArbiter_io_in_2_bits_control_spike_info_pc),
    .io_in_2_bits_control_spike_info_inst(issueArbiter_io_in_2_bits_control_spike_info_inst),
    .io_in_3_ready(issueArbiter_io_in_3_ready),
    .io_in_3_valid(issueArbiter_io_in_3_valid),
    .io_in_3_bits_alu_src1_0(issueArbiter_io_in_3_bits_alu_src1_0),
    .io_in_3_bits_alu_src1_1(issueArbiter_io_in_3_bits_alu_src1_1),
    .io_in_3_bits_alu_src1_2(issueArbiter_io_in_3_bits_alu_src1_2),
    .io_in_3_bits_alu_src1_3(issueArbiter_io_in_3_bits_alu_src1_3),
    .io_in_3_bits_alu_src2_0(issueArbiter_io_in_3_bits_alu_src2_0),
    .io_in_3_bits_alu_src2_1(issueArbiter_io_in_3_bits_alu_src2_1),
    .io_in_3_bits_alu_src2_2(issueArbiter_io_in_3_bits_alu_src2_2),
    .io_in_3_bits_alu_src2_3(issueArbiter_io_in_3_bits_alu_src2_3),
    .io_in_3_bits_alu_src3_0(issueArbiter_io_in_3_bits_alu_src3_0),
    .io_in_3_bits_alu_src3_1(issueArbiter_io_in_3_bits_alu_src3_1),
    .io_in_3_bits_alu_src3_2(issueArbiter_io_in_3_bits_alu_src3_2),
    .io_in_3_bits_alu_src3_3(issueArbiter_io_in_3_bits_alu_src3_3),
    .io_in_3_bits_mask_0(issueArbiter_io_in_3_bits_mask_0),
    .io_in_3_bits_mask_1(issueArbiter_io_in_3_bits_mask_1),
    .io_in_3_bits_mask_2(issueArbiter_io_in_3_bits_mask_2),
    .io_in_3_bits_mask_3(issueArbiter_io_in_3_bits_mask_3),
    .io_in_3_bits_control_inst(issueArbiter_io_in_3_bits_control_inst),
    .io_in_3_bits_control_wid(issueArbiter_io_in_3_bits_control_wid),
    .io_in_3_bits_control_fp(issueArbiter_io_in_3_bits_control_fp),
    .io_in_3_bits_control_branch(issueArbiter_io_in_3_bits_control_branch),
    .io_in_3_bits_control_simt_stack(issueArbiter_io_in_3_bits_control_simt_stack),
    .io_in_3_bits_control_simt_stack_op(issueArbiter_io_in_3_bits_control_simt_stack_op),
    .io_in_3_bits_control_barrier(issueArbiter_io_in_3_bits_control_barrier),
    .io_in_3_bits_control_csr(issueArbiter_io_in_3_bits_control_csr),
    .io_in_3_bits_control_reverse(issueArbiter_io_in_3_bits_control_reverse),
    .io_in_3_bits_control_sel_alu2(issueArbiter_io_in_3_bits_control_sel_alu2),
    .io_in_3_bits_control_sel_alu1(issueArbiter_io_in_3_bits_control_sel_alu1),
    .io_in_3_bits_control_isvec(issueArbiter_io_in_3_bits_control_isvec),
    .io_in_3_bits_control_sel_alu3(issueArbiter_io_in_3_bits_control_sel_alu3),
    .io_in_3_bits_control_mask(issueArbiter_io_in_3_bits_control_mask),
    .io_in_3_bits_control_sel_imm(issueArbiter_io_in_3_bits_control_sel_imm),
    .io_in_3_bits_control_mem_whb(issueArbiter_io_in_3_bits_control_mem_whb),
    .io_in_3_bits_control_mem_unsigned(issueArbiter_io_in_3_bits_control_mem_unsigned),
    .io_in_3_bits_control_alu_fn(issueArbiter_io_in_3_bits_control_alu_fn),
    .io_in_3_bits_control_mem(issueArbiter_io_in_3_bits_control_mem),
    .io_in_3_bits_control_mul(issueArbiter_io_in_3_bits_control_mul),
    .io_in_3_bits_control_mem_cmd(issueArbiter_io_in_3_bits_control_mem_cmd),
    .io_in_3_bits_control_mop(issueArbiter_io_in_3_bits_control_mop),
    .io_in_3_bits_control_reg_idx1(issueArbiter_io_in_3_bits_control_reg_idx1),
    .io_in_3_bits_control_reg_idx2(issueArbiter_io_in_3_bits_control_reg_idx2),
    .io_in_3_bits_control_reg_idx3(issueArbiter_io_in_3_bits_control_reg_idx3),
    .io_in_3_bits_control_reg_idxw(issueArbiter_io_in_3_bits_control_reg_idxw),
    .io_in_3_bits_control_wfd(issueArbiter_io_in_3_bits_control_wfd),
    .io_in_3_bits_control_fence(issueArbiter_io_in_3_bits_control_fence),
    .io_in_3_bits_control_sfu(issueArbiter_io_in_3_bits_control_sfu),
    .io_in_3_bits_control_readmask(issueArbiter_io_in_3_bits_control_readmask),
    .io_in_3_bits_control_writemask(issueArbiter_io_in_3_bits_control_writemask),
    .io_in_3_bits_control_wxd(issueArbiter_io_in_3_bits_control_wxd),
    .io_in_3_bits_control_pc(issueArbiter_io_in_3_bits_control_pc),
    .io_in_3_bits_control_spike_info_pc(issueArbiter_io_in_3_bits_control_spike_info_pc),
    .io_in_3_bits_control_spike_info_inst(issueArbiter_io_in_3_bits_control_spike_info_inst),
    .io_out_ready(issueArbiter_io_out_ready),
    .io_out_valid(issueArbiter_io_out_valid),
    .io_out_bits_alu_src1_0(issueArbiter_io_out_bits_alu_src1_0),
    .io_out_bits_alu_src1_1(issueArbiter_io_out_bits_alu_src1_1),
    .io_out_bits_alu_src1_2(issueArbiter_io_out_bits_alu_src1_2),
    .io_out_bits_alu_src1_3(issueArbiter_io_out_bits_alu_src1_3),
    .io_out_bits_alu_src2_0(issueArbiter_io_out_bits_alu_src2_0),
    .io_out_bits_alu_src2_1(issueArbiter_io_out_bits_alu_src2_1),
    .io_out_bits_alu_src2_2(issueArbiter_io_out_bits_alu_src2_2),
    .io_out_bits_alu_src2_3(issueArbiter_io_out_bits_alu_src2_3),
    .io_out_bits_alu_src3_0(issueArbiter_io_out_bits_alu_src3_0),
    .io_out_bits_alu_src3_1(issueArbiter_io_out_bits_alu_src3_1),
    .io_out_bits_alu_src3_2(issueArbiter_io_out_bits_alu_src3_2),
    .io_out_bits_alu_src3_3(issueArbiter_io_out_bits_alu_src3_3),
    .io_out_bits_mask_0(issueArbiter_io_out_bits_mask_0),
    .io_out_bits_mask_1(issueArbiter_io_out_bits_mask_1),
    .io_out_bits_mask_2(issueArbiter_io_out_bits_mask_2),
    .io_out_bits_mask_3(issueArbiter_io_out_bits_mask_3),
    .io_out_bits_control_inst(issueArbiter_io_out_bits_control_inst),
    .io_out_bits_control_wid(issueArbiter_io_out_bits_control_wid),
    .io_out_bits_control_fp(issueArbiter_io_out_bits_control_fp),
    .io_out_bits_control_branch(issueArbiter_io_out_bits_control_branch),
    .io_out_bits_control_simt_stack(issueArbiter_io_out_bits_control_simt_stack),
    .io_out_bits_control_simt_stack_op(issueArbiter_io_out_bits_control_simt_stack_op),
    .io_out_bits_control_barrier(issueArbiter_io_out_bits_control_barrier),
    .io_out_bits_control_csr(issueArbiter_io_out_bits_control_csr),
    .io_out_bits_control_reverse(issueArbiter_io_out_bits_control_reverse),
    .io_out_bits_control_sel_alu2(issueArbiter_io_out_bits_control_sel_alu2),
    .io_out_bits_control_sel_alu1(issueArbiter_io_out_bits_control_sel_alu1),
    .io_out_bits_control_isvec(issueArbiter_io_out_bits_control_isvec),
    .io_out_bits_control_sel_alu3(issueArbiter_io_out_bits_control_sel_alu3),
    .io_out_bits_control_mask(issueArbiter_io_out_bits_control_mask),
    .io_out_bits_control_sel_imm(issueArbiter_io_out_bits_control_sel_imm),
    .io_out_bits_control_mem_whb(issueArbiter_io_out_bits_control_mem_whb),
    .io_out_bits_control_mem_unsigned(issueArbiter_io_out_bits_control_mem_unsigned),
    .io_out_bits_control_alu_fn(issueArbiter_io_out_bits_control_alu_fn),
    .io_out_bits_control_mem(issueArbiter_io_out_bits_control_mem),
    .io_out_bits_control_mul(issueArbiter_io_out_bits_control_mul),
    .io_out_bits_control_mem_cmd(issueArbiter_io_out_bits_control_mem_cmd),
    .io_out_bits_control_mop(issueArbiter_io_out_bits_control_mop),
    .io_out_bits_control_reg_idx1(issueArbiter_io_out_bits_control_reg_idx1),
    .io_out_bits_control_reg_idx2(issueArbiter_io_out_bits_control_reg_idx2),
    .io_out_bits_control_reg_idx3(issueArbiter_io_out_bits_control_reg_idx3),
    .io_out_bits_control_reg_idxw(issueArbiter_io_out_bits_control_reg_idxw),
    .io_out_bits_control_wfd(issueArbiter_io_out_bits_control_wfd),
    .io_out_bits_control_fence(issueArbiter_io_out_bits_control_fence),
    .io_out_bits_control_sfu(issueArbiter_io_out_bits_control_sfu),
    .io_out_bits_control_readmask(issueArbiter_io_out_bits_control_readmask),
    .io_out_bits_control_writemask(issueArbiter_io_out_bits_control_writemask),
    .io_out_bits_control_wxd(issueArbiter_io_out_bits_control_wxd),
    .io_out_bits_control_pc(issueArbiter_io_out_bits_control_pc),
    .io_out_bits_control_spike_info_pc(issueArbiter_io_out_bits_control_spike_info_pc),
    .io_out_bits_control_spike_info_inst(issueArbiter_io_out_bits_control_spike_info_inst)
  );
  assign io_control_ready = Demux_io_in_ready; // @[operandCollector.scala 428:15]
  assign io_out_valid = issueArbiter_io_out_valid; // @[operandCollector.scala 468:10]
  assign io_out_bits_alu_src1_0 = issueArbiter_io_out_bits_alu_src1_0; // @[operandCollector.scala 468:10]
  assign io_out_bits_alu_src1_1 = issueArbiter_io_out_bits_alu_src1_1; // @[operandCollector.scala 468:10]
  assign io_out_bits_alu_src1_2 = issueArbiter_io_out_bits_alu_src1_2; // @[operandCollector.scala 468:10]
  assign io_out_bits_alu_src1_3 = issueArbiter_io_out_bits_alu_src1_3; // @[operandCollector.scala 468:10]
  assign io_out_bits_alu_src2_0 = issueArbiter_io_out_bits_alu_src2_0; // @[operandCollector.scala 468:10]
  assign io_out_bits_alu_src2_1 = issueArbiter_io_out_bits_alu_src2_1; // @[operandCollector.scala 468:10]
  assign io_out_bits_alu_src2_2 = issueArbiter_io_out_bits_alu_src2_2; // @[operandCollector.scala 468:10]
  assign io_out_bits_alu_src2_3 = issueArbiter_io_out_bits_alu_src2_3; // @[operandCollector.scala 468:10]
  assign io_out_bits_alu_src3_0 = issueArbiter_io_out_bits_alu_src3_0; // @[operandCollector.scala 468:10]
  assign io_out_bits_alu_src3_1 = issueArbiter_io_out_bits_alu_src3_1; // @[operandCollector.scala 468:10]
  assign io_out_bits_alu_src3_2 = issueArbiter_io_out_bits_alu_src3_2; // @[operandCollector.scala 468:10]
  assign io_out_bits_alu_src3_3 = issueArbiter_io_out_bits_alu_src3_3; // @[operandCollector.scala 468:10]
  assign io_out_bits_mask_0 = issueArbiter_io_out_bits_mask_0; // @[operandCollector.scala 468:10]
  assign io_out_bits_mask_1 = issueArbiter_io_out_bits_mask_1; // @[operandCollector.scala 468:10]
  assign io_out_bits_mask_2 = issueArbiter_io_out_bits_mask_2; // @[operandCollector.scala 468:10]
  assign io_out_bits_mask_3 = issueArbiter_io_out_bits_mask_3; // @[operandCollector.scala 468:10]
  assign io_out_bits_control_inst = issueArbiter_io_out_bits_control_inst; // @[operandCollector.scala 468:10]
  assign io_out_bits_control_wid = issueArbiter_io_out_bits_control_wid; // @[operandCollector.scala 468:10]
  assign io_out_bits_control_fp = issueArbiter_io_out_bits_control_fp; // @[operandCollector.scala 468:10]
  assign io_out_bits_control_branch = issueArbiter_io_out_bits_control_branch; // @[operandCollector.scala 468:10]
  assign io_out_bits_control_simt_stack = issueArbiter_io_out_bits_control_simt_stack; // @[operandCollector.scala 468:10]
  assign io_out_bits_control_simt_stack_op = issueArbiter_io_out_bits_control_simt_stack_op; // @[operandCollector.scala 468:10]
  assign io_out_bits_control_barrier = issueArbiter_io_out_bits_control_barrier; // @[operandCollector.scala 468:10]
  assign io_out_bits_control_csr = issueArbiter_io_out_bits_control_csr; // @[operandCollector.scala 468:10]
  assign io_out_bits_control_reverse = issueArbiter_io_out_bits_control_reverse; // @[operandCollector.scala 468:10]
  assign io_out_bits_control_sel_alu2 = issueArbiter_io_out_bits_control_sel_alu2; // @[operandCollector.scala 468:10]
  assign io_out_bits_control_sel_alu1 = issueArbiter_io_out_bits_control_sel_alu1; // @[operandCollector.scala 468:10]
  assign io_out_bits_control_isvec = issueArbiter_io_out_bits_control_isvec; // @[operandCollector.scala 468:10]
  assign io_out_bits_control_sel_alu3 = issueArbiter_io_out_bits_control_sel_alu3; // @[operandCollector.scala 468:10]
  assign io_out_bits_control_mask = issueArbiter_io_out_bits_control_mask; // @[operandCollector.scala 468:10]
  assign io_out_bits_control_sel_imm = issueArbiter_io_out_bits_control_sel_imm; // @[operandCollector.scala 468:10]
  assign io_out_bits_control_mem_whb = issueArbiter_io_out_bits_control_mem_whb; // @[operandCollector.scala 468:10]
  assign io_out_bits_control_mem_unsigned = issueArbiter_io_out_bits_control_mem_unsigned; // @[operandCollector.scala 468:10]
  assign io_out_bits_control_alu_fn = issueArbiter_io_out_bits_control_alu_fn; // @[operandCollector.scala 468:10]
  assign io_out_bits_control_mem = issueArbiter_io_out_bits_control_mem; // @[operandCollector.scala 468:10]
  assign io_out_bits_control_mul = issueArbiter_io_out_bits_control_mul; // @[operandCollector.scala 468:10]
  assign io_out_bits_control_mem_cmd = issueArbiter_io_out_bits_control_mem_cmd; // @[operandCollector.scala 468:10]
  assign io_out_bits_control_mop = issueArbiter_io_out_bits_control_mop; // @[operandCollector.scala 468:10]
  assign io_out_bits_control_reg_idx1 = issueArbiter_io_out_bits_control_reg_idx1; // @[operandCollector.scala 468:10]
  assign io_out_bits_control_reg_idx2 = issueArbiter_io_out_bits_control_reg_idx2; // @[operandCollector.scala 468:10]
  assign io_out_bits_control_reg_idx3 = issueArbiter_io_out_bits_control_reg_idx3; // @[operandCollector.scala 468:10]
  assign io_out_bits_control_reg_idxw = issueArbiter_io_out_bits_control_reg_idxw; // @[operandCollector.scala 468:10]
  assign io_out_bits_control_wfd = issueArbiter_io_out_bits_control_wfd; // @[operandCollector.scala 468:10]
  assign io_out_bits_control_fence = issueArbiter_io_out_bits_control_fence; // @[operandCollector.scala 468:10]
  assign io_out_bits_control_sfu = issueArbiter_io_out_bits_control_sfu; // @[operandCollector.scala 468:10]
  assign io_out_bits_control_readmask = issueArbiter_io_out_bits_control_readmask; // @[operandCollector.scala 468:10]
  assign io_out_bits_control_writemask = issueArbiter_io_out_bits_control_writemask; // @[operandCollector.scala 468:10]
  assign io_out_bits_control_wxd = issueArbiter_io_out_bits_control_wxd; // @[operandCollector.scala 468:10]
  assign io_out_bits_control_pc = issueArbiter_io_out_bits_control_pc; // @[operandCollector.scala 468:10]
  assign io_out_bits_control_spike_info_pc = issueArbiter_io_out_bits_control_spike_info_pc; // @[operandCollector.scala 468:10]
  assign io_out_bits_control_spike_info_inst = issueArbiter_io_out_bits_control_spike_info_inst; // @[operandCollector.scala 468:10]
  assign io_writeScalarCtrl_ready = 1'h1; // @[operandCollector.scala 462:28]
  assign io_writeVecCtrl_ready = 1'h1; // @[operandCollector.scala 463:25]
  assign collectorUnit_clock = clock;
  assign collectorUnit_reset = reset;
  assign collectorUnit_io_control_valid = Demux_io_out_0_valid; // @[operandCollector.scala 388:31 429:65]
  assign collectorUnit_io_control_bits_inst = Demux_io_out_0_bits_inst; // @[operandCollector.scala 388:31 429:65]
  assign collectorUnit_io_control_bits_wid = Demux_io_out_0_bits_wid; // @[operandCollector.scala 388:31 429:65]
  assign collectorUnit_io_control_bits_fp = Demux_io_out_0_bits_fp; // @[operandCollector.scala 388:31 429:65]
  assign collectorUnit_io_control_bits_branch = Demux_io_out_0_bits_branch; // @[operandCollector.scala 388:31 429:65]
  assign collectorUnit_io_control_bits_simt_stack = Demux_io_out_0_bits_simt_stack; // @[operandCollector.scala 388:31 429:65]
  assign collectorUnit_io_control_bits_simt_stack_op = Demux_io_out_0_bits_simt_stack_op; // @[operandCollector.scala 388:31 429:65]
  assign collectorUnit_io_control_bits_barrier = Demux_io_out_0_bits_barrier; // @[operandCollector.scala 388:31 429:65]
  assign collectorUnit_io_control_bits_csr = Demux_io_out_0_bits_csr; // @[operandCollector.scala 388:31 429:65]
  assign collectorUnit_io_control_bits_reverse = Demux_io_out_0_bits_reverse; // @[operandCollector.scala 388:31 429:65]
  assign collectorUnit_io_control_bits_sel_alu2 = Demux_io_out_0_bits_sel_alu2; // @[operandCollector.scala 388:31 429:65]
  assign collectorUnit_io_control_bits_sel_alu1 = Demux_io_out_0_bits_sel_alu1; // @[operandCollector.scala 388:31 429:65]
  assign collectorUnit_io_control_bits_isvec = Demux_io_out_0_bits_isvec; // @[operandCollector.scala 388:31 429:65]
  assign collectorUnit_io_control_bits_sel_alu3 = Demux_io_out_0_bits_sel_alu3; // @[operandCollector.scala 388:31 429:65]
  assign collectorUnit_io_control_bits_mask = Demux_io_out_0_bits_mask; // @[operandCollector.scala 388:31 429:65]
  assign collectorUnit_io_control_bits_sel_imm = Demux_io_out_0_bits_sel_imm; // @[operandCollector.scala 388:31 429:65]
  assign collectorUnit_io_control_bits_mem_whb = Demux_io_out_0_bits_mem_whb; // @[operandCollector.scala 388:31 429:65]
  assign collectorUnit_io_control_bits_mem_unsigned = Demux_io_out_0_bits_mem_unsigned; // @[operandCollector.scala 388:31 429:65]
  assign collectorUnit_io_control_bits_alu_fn = Demux_io_out_0_bits_alu_fn; // @[operandCollector.scala 388:31 429:65]
  assign collectorUnit_io_control_bits_mem = Demux_io_out_0_bits_mem; // @[operandCollector.scala 388:31 429:65]
  assign collectorUnit_io_control_bits_mul = Demux_io_out_0_bits_mul; // @[operandCollector.scala 388:31 429:65]
  assign collectorUnit_io_control_bits_mem_cmd = Demux_io_out_0_bits_mem_cmd; // @[operandCollector.scala 388:31 429:65]
  assign collectorUnit_io_control_bits_mop = Demux_io_out_0_bits_mop; // @[operandCollector.scala 388:31 429:65]
  assign collectorUnit_io_control_bits_reg_idx1 = Demux_io_out_0_bits_reg_idx1; // @[operandCollector.scala 388:31 429:65]
  assign collectorUnit_io_control_bits_reg_idx2 = Demux_io_out_0_bits_reg_idx2; // @[operandCollector.scala 388:31 429:65]
  assign collectorUnit_io_control_bits_reg_idx3 = Demux_io_out_0_bits_reg_idx3; // @[operandCollector.scala 388:31 429:65]
  assign collectorUnit_io_control_bits_reg_idxw = Demux_io_out_0_bits_reg_idxw; // @[operandCollector.scala 388:31 429:65]
  assign collectorUnit_io_control_bits_wfd = Demux_io_out_0_bits_wfd; // @[operandCollector.scala 388:31 429:65]
  assign collectorUnit_io_control_bits_fence = Demux_io_out_0_bits_fence; // @[operandCollector.scala 388:31 429:65]
  assign collectorUnit_io_control_bits_sfu = Demux_io_out_0_bits_sfu; // @[operandCollector.scala 388:31 429:65]
  assign collectorUnit_io_control_bits_readmask = Demux_io_out_0_bits_readmask; // @[operandCollector.scala 388:31 429:65]
  assign collectorUnit_io_control_bits_writemask = Demux_io_out_0_bits_writemask; // @[operandCollector.scala 388:31 429:65]
  assign collectorUnit_io_control_bits_wxd = Demux_io_out_0_bits_wxd; // @[operandCollector.scala 388:31 429:65]
  assign collectorUnit_io_control_bits_pc = Demux_io_out_0_bits_pc; // @[operandCollector.scala 388:31 429:65]
  assign collectorUnit_io_control_bits_spike_info_pc = Demux_io_out_0_bits_spike_info_pc; // @[operandCollector.scala 388:31 429:65]
  assign collectorUnit_io_control_bits_spike_info_inst = Demux_io_out_0_bits_spike_info_inst; // @[operandCollector.scala 388:31 429:65]
  assign collectorUnit_io_bankIn_0_valid = crossBar_io_out_0_0_valid; // @[operandCollector.scala 388:31 417:70]
  assign collectorUnit_io_bankIn_0_bits_regOrder = crossBar_io_out_0_0_bits_regOrder; // @[operandCollector.scala 388:31 417:70]
  assign collectorUnit_io_bankIn_0_bits_data_0 = crossBar_io_out_0_0_bits_data_0; // @[operandCollector.scala 388:31 417:70]
  assign collectorUnit_io_bankIn_0_bits_data_1 = crossBar_io_out_0_0_bits_data_1; // @[operandCollector.scala 388:31 417:70]
  assign collectorUnit_io_bankIn_0_bits_data_2 = crossBar_io_out_0_0_bits_data_2; // @[operandCollector.scala 388:31 417:70]
  assign collectorUnit_io_bankIn_0_bits_data_3 = crossBar_io_out_0_0_bits_data_3; // @[operandCollector.scala 388:31 417:70]
  assign collectorUnit_io_bankIn_0_bits_v0_0 = crossBar_io_out_0_0_bits_v0_0; // @[operandCollector.scala 388:31 417:70]
  assign collectorUnit_io_bankIn_1_valid = crossBar_io_out_0_1_valid; // @[operandCollector.scala 388:31 417:70]
  assign collectorUnit_io_bankIn_1_bits_regOrder = crossBar_io_out_0_1_bits_regOrder; // @[operandCollector.scala 388:31 417:70]
  assign collectorUnit_io_bankIn_1_bits_data_0 = crossBar_io_out_0_1_bits_data_0; // @[operandCollector.scala 388:31 417:70]
  assign collectorUnit_io_bankIn_1_bits_data_1 = crossBar_io_out_0_1_bits_data_1; // @[operandCollector.scala 388:31 417:70]
  assign collectorUnit_io_bankIn_1_bits_data_2 = crossBar_io_out_0_1_bits_data_2; // @[operandCollector.scala 388:31 417:70]
  assign collectorUnit_io_bankIn_1_bits_data_3 = crossBar_io_out_0_1_bits_data_3; // @[operandCollector.scala 388:31 417:70]
  assign collectorUnit_io_bankIn_1_bits_v0_0 = crossBar_io_out_0_1_bits_v0_0; // @[operandCollector.scala 388:31 417:70]
  assign collectorUnit_io_bankIn_2_valid = crossBar_io_out_0_2_valid; // @[operandCollector.scala 388:31 417:70]
  assign collectorUnit_io_bankIn_2_bits_regOrder = crossBar_io_out_0_2_bits_regOrder; // @[operandCollector.scala 388:31 417:70]
  assign collectorUnit_io_bankIn_2_bits_data_0 = crossBar_io_out_0_2_bits_data_0; // @[operandCollector.scala 388:31 417:70]
  assign collectorUnit_io_bankIn_2_bits_data_1 = crossBar_io_out_0_2_bits_data_1; // @[operandCollector.scala 388:31 417:70]
  assign collectorUnit_io_bankIn_2_bits_data_2 = crossBar_io_out_0_2_bits_data_2; // @[operandCollector.scala 388:31 417:70]
  assign collectorUnit_io_bankIn_2_bits_data_3 = crossBar_io_out_0_2_bits_data_3; // @[operandCollector.scala 388:31 417:70]
  assign collectorUnit_io_bankIn_2_bits_v0_0 = crossBar_io_out_0_2_bits_v0_0; // @[operandCollector.scala 388:31 417:70]
  assign collectorUnit_io_bankIn_3_valid = crossBar_io_out_0_3_valid; // @[operandCollector.scala 388:31 417:70]
  assign collectorUnit_io_bankIn_3_bits_regOrder = crossBar_io_out_0_3_bits_regOrder; // @[operandCollector.scala 388:31 417:70]
  assign collectorUnit_io_bankIn_3_bits_data_0 = crossBar_io_out_0_3_bits_data_0; // @[operandCollector.scala 388:31 417:70]
  assign collectorUnit_io_bankIn_3_bits_data_1 = crossBar_io_out_0_3_bits_data_1; // @[operandCollector.scala 388:31 417:70]
  assign collectorUnit_io_bankIn_3_bits_data_2 = crossBar_io_out_0_3_bits_data_2; // @[operandCollector.scala 388:31 417:70]
  assign collectorUnit_io_bankIn_3_bits_data_3 = crossBar_io_out_0_3_bits_data_3; // @[operandCollector.scala 388:31 417:70]
  assign collectorUnit_io_bankIn_3_bits_v0_0 = crossBar_io_out_0_3_bits_v0_0; // @[operandCollector.scala 388:31 417:70]
  assign collectorUnit_io_issue_ready = issueArbiter_io_in_0_ready; // @[operandCollector.scala 467:{22,32}]
  assign collectorUnit_1_clock = clock;
  assign collectorUnit_1_reset = reset;
  assign collectorUnit_1_io_control_valid = Demux_io_out_1_valid; // @[operandCollector.scala 388:31 429:65]
  assign collectorUnit_1_io_control_bits_inst = Demux_io_out_1_bits_inst; // @[operandCollector.scala 388:31 429:65]
  assign collectorUnit_1_io_control_bits_wid = Demux_io_out_1_bits_wid; // @[operandCollector.scala 388:31 429:65]
  assign collectorUnit_1_io_control_bits_fp = Demux_io_out_1_bits_fp; // @[operandCollector.scala 388:31 429:65]
  assign collectorUnit_1_io_control_bits_branch = Demux_io_out_1_bits_branch; // @[operandCollector.scala 388:31 429:65]
  assign collectorUnit_1_io_control_bits_simt_stack = Demux_io_out_1_bits_simt_stack; // @[operandCollector.scala 388:31 429:65]
  assign collectorUnit_1_io_control_bits_simt_stack_op = Demux_io_out_1_bits_simt_stack_op; // @[operandCollector.scala 388:31 429:65]
  assign collectorUnit_1_io_control_bits_barrier = Demux_io_out_1_bits_barrier; // @[operandCollector.scala 388:31 429:65]
  assign collectorUnit_1_io_control_bits_csr = Demux_io_out_1_bits_csr; // @[operandCollector.scala 388:31 429:65]
  assign collectorUnit_1_io_control_bits_reverse = Demux_io_out_1_bits_reverse; // @[operandCollector.scala 388:31 429:65]
  assign collectorUnit_1_io_control_bits_sel_alu2 = Demux_io_out_1_bits_sel_alu2; // @[operandCollector.scala 388:31 429:65]
  assign collectorUnit_1_io_control_bits_sel_alu1 = Demux_io_out_1_bits_sel_alu1; // @[operandCollector.scala 388:31 429:65]
  assign collectorUnit_1_io_control_bits_isvec = Demux_io_out_1_bits_isvec; // @[operandCollector.scala 388:31 429:65]
  assign collectorUnit_1_io_control_bits_sel_alu3 = Demux_io_out_1_bits_sel_alu3; // @[operandCollector.scala 388:31 429:65]
  assign collectorUnit_1_io_control_bits_mask = Demux_io_out_1_bits_mask; // @[operandCollector.scala 388:31 429:65]
  assign collectorUnit_1_io_control_bits_sel_imm = Demux_io_out_1_bits_sel_imm; // @[operandCollector.scala 388:31 429:65]
  assign collectorUnit_1_io_control_bits_mem_whb = Demux_io_out_1_bits_mem_whb; // @[operandCollector.scala 388:31 429:65]
  assign collectorUnit_1_io_control_bits_mem_unsigned = Demux_io_out_1_bits_mem_unsigned; // @[operandCollector.scala 388:31 429:65]
  assign collectorUnit_1_io_control_bits_alu_fn = Demux_io_out_1_bits_alu_fn; // @[operandCollector.scala 388:31 429:65]
  assign collectorUnit_1_io_control_bits_mem = Demux_io_out_1_bits_mem; // @[operandCollector.scala 388:31 429:65]
  assign collectorUnit_1_io_control_bits_mul = Demux_io_out_1_bits_mul; // @[operandCollector.scala 388:31 429:65]
  assign collectorUnit_1_io_control_bits_mem_cmd = Demux_io_out_1_bits_mem_cmd; // @[operandCollector.scala 388:31 429:65]
  assign collectorUnit_1_io_control_bits_mop = Demux_io_out_1_bits_mop; // @[operandCollector.scala 388:31 429:65]
  assign collectorUnit_1_io_control_bits_reg_idx1 = Demux_io_out_1_bits_reg_idx1; // @[operandCollector.scala 388:31 429:65]
  assign collectorUnit_1_io_control_bits_reg_idx2 = Demux_io_out_1_bits_reg_idx2; // @[operandCollector.scala 388:31 429:65]
  assign collectorUnit_1_io_control_bits_reg_idx3 = Demux_io_out_1_bits_reg_idx3; // @[operandCollector.scala 388:31 429:65]
  assign collectorUnit_1_io_control_bits_reg_idxw = Demux_io_out_1_bits_reg_idxw; // @[operandCollector.scala 388:31 429:65]
  assign collectorUnit_1_io_control_bits_wfd = Demux_io_out_1_bits_wfd; // @[operandCollector.scala 388:31 429:65]
  assign collectorUnit_1_io_control_bits_fence = Demux_io_out_1_bits_fence; // @[operandCollector.scala 388:31 429:65]
  assign collectorUnit_1_io_control_bits_sfu = Demux_io_out_1_bits_sfu; // @[operandCollector.scala 388:31 429:65]
  assign collectorUnit_1_io_control_bits_readmask = Demux_io_out_1_bits_readmask; // @[operandCollector.scala 388:31 429:65]
  assign collectorUnit_1_io_control_bits_writemask = Demux_io_out_1_bits_writemask; // @[operandCollector.scala 388:31 429:65]
  assign collectorUnit_1_io_control_bits_wxd = Demux_io_out_1_bits_wxd; // @[operandCollector.scala 388:31 429:65]
  assign collectorUnit_1_io_control_bits_pc = Demux_io_out_1_bits_pc; // @[operandCollector.scala 388:31 429:65]
  assign collectorUnit_1_io_control_bits_spike_info_pc = Demux_io_out_1_bits_spike_info_pc; // @[operandCollector.scala 388:31 429:65]
  assign collectorUnit_1_io_control_bits_spike_info_inst = Demux_io_out_1_bits_spike_info_inst; // @[operandCollector.scala 388:31 429:65]
  assign collectorUnit_1_io_bankIn_0_valid = crossBar_io_out_1_0_valid; // @[operandCollector.scala 388:31 417:70]
  assign collectorUnit_1_io_bankIn_0_bits_regOrder = crossBar_io_out_1_0_bits_regOrder; // @[operandCollector.scala 388:31 417:70]
  assign collectorUnit_1_io_bankIn_0_bits_data_0 = crossBar_io_out_1_0_bits_data_0; // @[operandCollector.scala 388:31 417:70]
  assign collectorUnit_1_io_bankIn_0_bits_data_1 = crossBar_io_out_1_0_bits_data_1; // @[operandCollector.scala 388:31 417:70]
  assign collectorUnit_1_io_bankIn_0_bits_data_2 = crossBar_io_out_1_0_bits_data_2; // @[operandCollector.scala 388:31 417:70]
  assign collectorUnit_1_io_bankIn_0_bits_data_3 = crossBar_io_out_1_0_bits_data_3; // @[operandCollector.scala 388:31 417:70]
  assign collectorUnit_1_io_bankIn_0_bits_v0_0 = crossBar_io_out_1_0_bits_v0_0; // @[operandCollector.scala 388:31 417:70]
  assign collectorUnit_1_io_bankIn_1_valid = crossBar_io_out_1_1_valid; // @[operandCollector.scala 388:31 417:70]
  assign collectorUnit_1_io_bankIn_1_bits_regOrder = crossBar_io_out_1_1_bits_regOrder; // @[operandCollector.scala 388:31 417:70]
  assign collectorUnit_1_io_bankIn_1_bits_data_0 = crossBar_io_out_1_1_bits_data_0; // @[operandCollector.scala 388:31 417:70]
  assign collectorUnit_1_io_bankIn_1_bits_data_1 = crossBar_io_out_1_1_bits_data_1; // @[operandCollector.scala 388:31 417:70]
  assign collectorUnit_1_io_bankIn_1_bits_data_2 = crossBar_io_out_1_1_bits_data_2; // @[operandCollector.scala 388:31 417:70]
  assign collectorUnit_1_io_bankIn_1_bits_data_3 = crossBar_io_out_1_1_bits_data_3; // @[operandCollector.scala 388:31 417:70]
  assign collectorUnit_1_io_bankIn_1_bits_v0_0 = crossBar_io_out_1_1_bits_v0_0; // @[operandCollector.scala 388:31 417:70]
  assign collectorUnit_1_io_bankIn_2_valid = crossBar_io_out_1_2_valid; // @[operandCollector.scala 388:31 417:70]
  assign collectorUnit_1_io_bankIn_2_bits_regOrder = crossBar_io_out_1_2_bits_regOrder; // @[operandCollector.scala 388:31 417:70]
  assign collectorUnit_1_io_bankIn_2_bits_data_0 = crossBar_io_out_1_2_bits_data_0; // @[operandCollector.scala 388:31 417:70]
  assign collectorUnit_1_io_bankIn_2_bits_data_1 = crossBar_io_out_1_2_bits_data_1; // @[operandCollector.scala 388:31 417:70]
  assign collectorUnit_1_io_bankIn_2_bits_data_2 = crossBar_io_out_1_2_bits_data_2; // @[operandCollector.scala 388:31 417:70]
  assign collectorUnit_1_io_bankIn_2_bits_data_3 = crossBar_io_out_1_2_bits_data_3; // @[operandCollector.scala 388:31 417:70]
  assign collectorUnit_1_io_bankIn_2_bits_v0_0 = crossBar_io_out_1_2_bits_v0_0; // @[operandCollector.scala 388:31 417:70]
  assign collectorUnit_1_io_bankIn_3_valid = crossBar_io_out_1_3_valid; // @[operandCollector.scala 388:31 417:70]
  assign collectorUnit_1_io_bankIn_3_bits_regOrder = crossBar_io_out_1_3_bits_regOrder; // @[operandCollector.scala 388:31 417:70]
  assign collectorUnit_1_io_bankIn_3_bits_data_0 = crossBar_io_out_1_3_bits_data_0; // @[operandCollector.scala 388:31 417:70]
  assign collectorUnit_1_io_bankIn_3_bits_data_1 = crossBar_io_out_1_3_bits_data_1; // @[operandCollector.scala 388:31 417:70]
  assign collectorUnit_1_io_bankIn_3_bits_data_2 = crossBar_io_out_1_3_bits_data_2; // @[operandCollector.scala 388:31 417:70]
  assign collectorUnit_1_io_bankIn_3_bits_data_3 = crossBar_io_out_1_3_bits_data_3; // @[operandCollector.scala 388:31 417:70]
  assign collectorUnit_1_io_bankIn_3_bits_v0_0 = crossBar_io_out_1_3_bits_v0_0; // @[operandCollector.scala 388:31 417:70]
  assign collectorUnit_1_io_issue_ready = issueArbiter_io_in_1_ready; // @[operandCollector.scala 467:{22,32}]
  assign collectorUnit_2_clock = clock;
  assign collectorUnit_2_reset = reset;
  assign collectorUnit_2_io_control_valid = Demux_io_out_2_valid; // @[operandCollector.scala 388:31 429:65]
  assign collectorUnit_2_io_control_bits_inst = Demux_io_out_2_bits_inst; // @[operandCollector.scala 388:31 429:65]
  assign collectorUnit_2_io_control_bits_wid = Demux_io_out_2_bits_wid; // @[operandCollector.scala 388:31 429:65]
  assign collectorUnit_2_io_control_bits_fp = Demux_io_out_2_bits_fp; // @[operandCollector.scala 388:31 429:65]
  assign collectorUnit_2_io_control_bits_branch = Demux_io_out_2_bits_branch; // @[operandCollector.scala 388:31 429:65]
  assign collectorUnit_2_io_control_bits_simt_stack = Demux_io_out_2_bits_simt_stack; // @[operandCollector.scala 388:31 429:65]
  assign collectorUnit_2_io_control_bits_simt_stack_op = Demux_io_out_2_bits_simt_stack_op; // @[operandCollector.scala 388:31 429:65]
  assign collectorUnit_2_io_control_bits_barrier = Demux_io_out_2_bits_barrier; // @[operandCollector.scala 388:31 429:65]
  assign collectorUnit_2_io_control_bits_csr = Demux_io_out_2_bits_csr; // @[operandCollector.scala 388:31 429:65]
  assign collectorUnit_2_io_control_bits_reverse = Demux_io_out_2_bits_reverse; // @[operandCollector.scala 388:31 429:65]
  assign collectorUnit_2_io_control_bits_sel_alu2 = Demux_io_out_2_bits_sel_alu2; // @[operandCollector.scala 388:31 429:65]
  assign collectorUnit_2_io_control_bits_sel_alu1 = Demux_io_out_2_bits_sel_alu1; // @[operandCollector.scala 388:31 429:65]
  assign collectorUnit_2_io_control_bits_isvec = Demux_io_out_2_bits_isvec; // @[operandCollector.scala 388:31 429:65]
  assign collectorUnit_2_io_control_bits_sel_alu3 = Demux_io_out_2_bits_sel_alu3; // @[operandCollector.scala 388:31 429:65]
  assign collectorUnit_2_io_control_bits_mask = Demux_io_out_2_bits_mask; // @[operandCollector.scala 388:31 429:65]
  assign collectorUnit_2_io_control_bits_sel_imm = Demux_io_out_2_bits_sel_imm; // @[operandCollector.scala 388:31 429:65]
  assign collectorUnit_2_io_control_bits_mem_whb = Demux_io_out_2_bits_mem_whb; // @[operandCollector.scala 388:31 429:65]
  assign collectorUnit_2_io_control_bits_mem_unsigned = Demux_io_out_2_bits_mem_unsigned; // @[operandCollector.scala 388:31 429:65]
  assign collectorUnit_2_io_control_bits_alu_fn = Demux_io_out_2_bits_alu_fn; // @[operandCollector.scala 388:31 429:65]
  assign collectorUnit_2_io_control_bits_mem = Demux_io_out_2_bits_mem; // @[operandCollector.scala 388:31 429:65]
  assign collectorUnit_2_io_control_bits_mul = Demux_io_out_2_bits_mul; // @[operandCollector.scala 388:31 429:65]
  assign collectorUnit_2_io_control_bits_mem_cmd = Demux_io_out_2_bits_mem_cmd; // @[operandCollector.scala 388:31 429:65]
  assign collectorUnit_2_io_control_bits_mop = Demux_io_out_2_bits_mop; // @[operandCollector.scala 388:31 429:65]
  assign collectorUnit_2_io_control_bits_reg_idx1 = Demux_io_out_2_bits_reg_idx1; // @[operandCollector.scala 388:31 429:65]
  assign collectorUnit_2_io_control_bits_reg_idx2 = Demux_io_out_2_bits_reg_idx2; // @[operandCollector.scala 388:31 429:65]
  assign collectorUnit_2_io_control_bits_reg_idx3 = Demux_io_out_2_bits_reg_idx3; // @[operandCollector.scala 388:31 429:65]
  assign collectorUnit_2_io_control_bits_reg_idxw = Demux_io_out_2_bits_reg_idxw; // @[operandCollector.scala 388:31 429:65]
  assign collectorUnit_2_io_control_bits_wfd = Demux_io_out_2_bits_wfd; // @[operandCollector.scala 388:31 429:65]
  assign collectorUnit_2_io_control_bits_fence = Demux_io_out_2_bits_fence; // @[operandCollector.scala 388:31 429:65]
  assign collectorUnit_2_io_control_bits_sfu = Demux_io_out_2_bits_sfu; // @[operandCollector.scala 388:31 429:65]
  assign collectorUnit_2_io_control_bits_readmask = Demux_io_out_2_bits_readmask; // @[operandCollector.scala 388:31 429:65]
  assign collectorUnit_2_io_control_bits_writemask = Demux_io_out_2_bits_writemask; // @[operandCollector.scala 388:31 429:65]
  assign collectorUnit_2_io_control_bits_wxd = Demux_io_out_2_bits_wxd; // @[operandCollector.scala 388:31 429:65]
  assign collectorUnit_2_io_control_bits_pc = Demux_io_out_2_bits_pc; // @[operandCollector.scala 388:31 429:65]
  assign collectorUnit_2_io_control_bits_spike_info_pc = Demux_io_out_2_bits_spike_info_pc; // @[operandCollector.scala 388:31 429:65]
  assign collectorUnit_2_io_control_bits_spike_info_inst = Demux_io_out_2_bits_spike_info_inst; // @[operandCollector.scala 388:31 429:65]
  assign collectorUnit_2_io_bankIn_0_valid = crossBar_io_out_2_0_valid; // @[operandCollector.scala 388:31 417:70]
  assign collectorUnit_2_io_bankIn_0_bits_regOrder = crossBar_io_out_2_0_bits_regOrder; // @[operandCollector.scala 388:31 417:70]
  assign collectorUnit_2_io_bankIn_0_bits_data_0 = crossBar_io_out_2_0_bits_data_0; // @[operandCollector.scala 388:31 417:70]
  assign collectorUnit_2_io_bankIn_0_bits_data_1 = crossBar_io_out_2_0_bits_data_1; // @[operandCollector.scala 388:31 417:70]
  assign collectorUnit_2_io_bankIn_0_bits_data_2 = crossBar_io_out_2_0_bits_data_2; // @[operandCollector.scala 388:31 417:70]
  assign collectorUnit_2_io_bankIn_0_bits_data_3 = crossBar_io_out_2_0_bits_data_3; // @[operandCollector.scala 388:31 417:70]
  assign collectorUnit_2_io_bankIn_0_bits_v0_0 = crossBar_io_out_2_0_bits_v0_0; // @[operandCollector.scala 388:31 417:70]
  assign collectorUnit_2_io_bankIn_1_valid = crossBar_io_out_2_1_valid; // @[operandCollector.scala 388:31 417:70]
  assign collectorUnit_2_io_bankIn_1_bits_regOrder = crossBar_io_out_2_1_bits_regOrder; // @[operandCollector.scala 388:31 417:70]
  assign collectorUnit_2_io_bankIn_1_bits_data_0 = crossBar_io_out_2_1_bits_data_0; // @[operandCollector.scala 388:31 417:70]
  assign collectorUnit_2_io_bankIn_1_bits_data_1 = crossBar_io_out_2_1_bits_data_1; // @[operandCollector.scala 388:31 417:70]
  assign collectorUnit_2_io_bankIn_1_bits_data_2 = crossBar_io_out_2_1_bits_data_2; // @[operandCollector.scala 388:31 417:70]
  assign collectorUnit_2_io_bankIn_1_bits_data_3 = crossBar_io_out_2_1_bits_data_3; // @[operandCollector.scala 388:31 417:70]
  assign collectorUnit_2_io_bankIn_1_bits_v0_0 = crossBar_io_out_2_1_bits_v0_0; // @[operandCollector.scala 388:31 417:70]
  assign collectorUnit_2_io_bankIn_2_valid = crossBar_io_out_2_2_valid; // @[operandCollector.scala 388:31 417:70]
  assign collectorUnit_2_io_bankIn_2_bits_regOrder = crossBar_io_out_2_2_bits_regOrder; // @[operandCollector.scala 388:31 417:70]
  assign collectorUnit_2_io_bankIn_2_bits_data_0 = crossBar_io_out_2_2_bits_data_0; // @[operandCollector.scala 388:31 417:70]
  assign collectorUnit_2_io_bankIn_2_bits_data_1 = crossBar_io_out_2_2_bits_data_1; // @[operandCollector.scala 388:31 417:70]
  assign collectorUnit_2_io_bankIn_2_bits_data_2 = crossBar_io_out_2_2_bits_data_2; // @[operandCollector.scala 388:31 417:70]
  assign collectorUnit_2_io_bankIn_2_bits_data_3 = crossBar_io_out_2_2_bits_data_3; // @[operandCollector.scala 388:31 417:70]
  assign collectorUnit_2_io_bankIn_2_bits_v0_0 = crossBar_io_out_2_2_bits_v0_0; // @[operandCollector.scala 388:31 417:70]
  assign collectorUnit_2_io_bankIn_3_valid = crossBar_io_out_2_3_valid; // @[operandCollector.scala 388:31 417:70]
  assign collectorUnit_2_io_bankIn_3_bits_regOrder = crossBar_io_out_2_3_bits_regOrder; // @[operandCollector.scala 388:31 417:70]
  assign collectorUnit_2_io_bankIn_3_bits_data_0 = crossBar_io_out_2_3_bits_data_0; // @[operandCollector.scala 388:31 417:70]
  assign collectorUnit_2_io_bankIn_3_bits_data_1 = crossBar_io_out_2_3_bits_data_1; // @[operandCollector.scala 388:31 417:70]
  assign collectorUnit_2_io_bankIn_3_bits_data_2 = crossBar_io_out_2_3_bits_data_2; // @[operandCollector.scala 388:31 417:70]
  assign collectorUnit_2_io_bankIn_3_bits_data_3 = crossBar_io_out_2_3_bits_data_3; // @[operandCollector.scala 388:31 417:70]
  assign collectorUnit_2_io_bankIn_3_bits_v0_0 = crossBar_io_out_2_3_bits_v0_0; // @[operandCollector.scala 388:31 417:70]
  assign collectorUnit_2_io_issue_ready = issueArbiter_io_in_2_ready; // @[operandCollector.scala 467:{22,32}]
  assign collectorUnit_3_clock = clock;
  assign collectorUnit_3_reset = reset;
  assign collectorUnit_3_io_control_valid = Demux_io_out_3_valid; // @[operandCollector.scala 388:31 429:65]
  assign collectorUnit_3_io_control_bits_inst = Demux_io_out_3_bits_inst; // @[operandCollector.scala 388:31 429:65]
  assign collectorUnit_3_io_control_bits_wid = Demux_io_out_3_bits_wid; // @[operandCollector.scala 388:31 429:65]
  assign collectorUnit_3_io_control_bits_fp = Demux_io_out_3_bits_fp; // @[operandCollector.scala 388:31 429:65]
  assign collectorUnit_3_io_control_bits_branch = Demux_io_out_3_bits_branch; // @[operandCollector.scala 388:31 429:65]
  assign collectorUnit_3_io_control_bits_simt_stack = Demux_io_out_3_bits_simt_stack; // @[operandCollector.scala 388:31 429:65]
  assign collectorUnit_3_io_control_bits_simt_stack_op = Demux_io_out_3_bits_simt_stack_op; // @[operandCollector.scala 388:31 429:65]
  assign collectorUnit_3_io_control_bits_barrier = Demux_io_out_3_bits_barrier; // @[operandCollector.scala 388:31 429:65]
  assign collectorUnit_3_io_control_bits_csr = Demux_io_out_3_bits_csr; // @[operandCollector.scala 388:31 429:65]
  assign collectorUnit_3_io_control_bits_reverse = Demux_io_out_3_bits_reverse; // @[operandCollector.scala 388:31 429:65]
  assign collectorUnit_3_io_control_bits_sel_alu2 = Demux_io_out_3_bits_sel_alu2; // @[operandCollector.scala 388:31 429:65]
  assign collectorUnit_3_io_control_bits_sel_alu1 = Demux_io_out_3_bits_sel_alu1; // @[operandCollector.scala 388:31 429:65]
  assign collectorUnit_3_io_control_bits_isvec = Demux_io_out_3_bits_isvec; // @[operandCollector.scala 388:31 429:65]
  assign collectorUnit_3_io_control_bits_sel_alu3 = Demux_io_out_3_bits_sel_alu3; // @[operandCollector.scala 388:31 429:65]
  assign collectorUnit_3_io_control_bits_mask = Demux_io_out_3_bits_mask; // @[operandCollector.scala 388:31 429:65]
  assign collectorUnit_3_io_control_bits_sel_imm = Demux_io_out_3_bits_sel_imm; // @[operandCollector.scala 388:31 429:65]
  assign collectorUnit_3_io_control_bits_mem_whb = Demux_io_out_3_bits_mem_whb; // @[operandCollector.scala 388:31 429:65]
  assign collectorUnit_3_io_control_bits_mem_unsigned = Demux_io_out_3_bits_mem_unsigned; // @[operandCollector.scala 388:31 429:65]
  assign collectorUnit_3_io_control_bits_alu_fn = Demux_io_out_3_bits_alu_fn; // @[operandCollector.scala 388:31 429:65]
  assign collectorUnit_3_io_control_bits_mem = Demux_io_out_3_bits_mem; // @[operandCollector.scala 388:31 429:65]
  assign collectorUnit_3_io_control_bits_mul = Demux_io_out_3_bits_mul; // @[operandCollector.scala 388:31 429:65]
  assign collectorUnit_3_io_control_bits_mem_cmd = Demux_io_out_3_bits_mem_cmd; // @[operandCollector.scala 388:31 429:65]
  assign collectorUnit_3_io_control_bits_mop = Demux_io_out_3_bits_mop; // @[operandCollector.scala 388:31 429:65]
  assign collectorUnit_3_io_control_bits_reg_idx1 = Demux_io_out_3_bits_reg_idx1; // @[operandCollector.scala 388:31 429:65]
  assign collectorUnit_3_io_control_bits_reg_idx2 = Demux_io_out_3_bits_reg_idx2; // @[operandCollector.scala 388:31 429:65]
  assign collectorUnit_3_io_control_bits_reg_idx3 = Demux_io_out_3_bits_reg_idx3; // @[operandCollector.scala 388:31 429:65]
  assign collectorUnit_3_io_control_bits_reg_idxw = Demux_io_out_3_bits_reg_idxw; // @[operandCollector.scala 388:31 429:65]
  assign collectorUnit_3_io_control_bits_wfd = Demux_io_out_3_bits_wfd; // @[operandCollector.scala 388:31 429:65]
  assign collectorUnit_3_io_control_bits_fence = Demux_io_out_3_bits_fence; // @[operandCollector.scala 388:31 429:65]
  assign collectorUnit_3_io_control_bits_sfu = Demux_io_out_3_bits_sfu; // @[operandCollector.scala 388:31 429:65]
  assign collectorUnit_3_io_control_bits_readmask = Demux_io_out_3_bits_readmask; // @[operandCollector.scala 388:31 429:65]
  assign collectorUnit_3_io_control_bits_writemask = Demux_io_out_3_bits_writemask; // @[operandCollector.scala 388:31 429:65]
  assign collectorUnit_3_io_control_bits_wxd = Demux_io_out_3_bits_wxd; // @[operandCollector.scala 388:31 429:65]
  assign collectorUnit_3_io_control_bits_pc = Demux_io_out_3_bits_pc; // @[operandCollector.scala 388:31 429:65]
  assign collectorUnit_3_io_control_bits_spike_info_pc = Demux_io_out_3_bits_spike_info_pc; // @[operandCollector.scala 388:31 429:65]
  assign collectorUnit_3_io_control_bits_spike_info_inst = Demux_io_out_3_bits_spike_info_inst; // @[operandCollector.scala 388:31 429:65]
  assign collectorUnit_3_io_bankIn_0_valid = crossBar_io_out_3_0_valid; // @[operandCollector.scala 388:31 417:70]
  assign collectorUnit_3_io_bankIn_0_bits_regOrder = crossBar_io_out_3_0_bits_regOrder; // @[operandCollector.scala 388:31 417:70]
  assign collectorUnit_3_io_bankIn_0_bits_data_0 = crossBar_io_out_3_0_bits_data_0; // @[operandCollector.scala 388:31 417:70]
  assign collectorUnit_3_io_bankIn_0_bits_data_1 = crossBar_io_out_3_0_bits_data_1; // @[operandCollector.scala 388:31 417:70]
  assign collectorUnit_3_io_bankIn_0_bits_data_2 = crossBar_io_out_3_0_bits_data_2; // @[operandCollector.scala 388:31 417:70]
  assign collectorUnit_3_io_bankIn_0_bits_data_3 = crossBar_io_out_3_0_bits_data_3; // @[operandCollector.scala 388:31 417:70]
  assign collectorUnit_3_io_bankIn_0_bits_v0_0 = crossBar_io_out_3_0_bits_v0_0; // @[operandCollector.scala 388:31 417:70]
  assign collectorUnit_3_io_bankIn_1_valid = crossBar_io_out_3_1_valid; // @[operandCollector.scala 388:31 417:70]
  assign collectorUnit_3_io_bankIn_1_bits_regOrder = crossBar_io_out_3_1_bits_regOrder; // @[operandCollector.scala 388:31 417:70]
  assign collectorUnit_3_io_bankIn_1_bits_data_0 = crossBar_io_out_3_1_bits_data_0; // @[operandCollector.scala 388:31 417:70]
  assign collectorUnit_3_io_bankIn_1_bits_data_1 = crossBar_io_out_3_1_bits_data_1; // @[operandCollector.scala 388:31 417:70]
  assign collectorUnit_3_io_bankIn_1_bits_data_2 = crossBar_io_out_3_1_bits_data_2; // @[operandCollector.scala 388:31 417:70]
  assign collectorUnit_3_io_bankIn_1_bits_data_3 = crossBar_io_out_3_1_bits_data_3; // @[operandCollector.scala 388:31 417:70]
  assign collectorUnit_3_io_bankIn_1_bits_v0_0 = crossBar_io_out_3_1_bits_v0_0; // @[operandCollector.scala 388:31 417:70]
  assign collectorUnit_3_io_bankIn_2_valid = crossBar_io_out_3_2_valid; // @[operandCollector.scala 388:31 417:70]
  assign collectorUnit_3_io_bankIn_2_bits_regOrder = crossBar_io_out_3_2_bits_regOrder; // @[operandCollector.scala 388:31 417:70]
  assign collectorUnit_3_io_bankIn_2_bits_data_0 = crossBar_io_out_3_2_bits_data_0; // @[operandCollector.scala 388:31 417:70]
  assign collectorUnit_3_io_bankIn_2_bits_data_1 = crossBar_io_out_3_2_bits_data_1; // @[operandCollector.scala 388:31 417:70]
  assign collectorUnit_3_io_bankIn_2_bits_data_2 = crossBar_io_out_3_2_bits_data_2; // @[operandCollector.scala 388:31 417:70]
  assign collectorUnit_3_io_bankIn_2_bits_data_3 = crossBar_io_out_3_2_bits_data_3; // @[operandCollector.scala 388:31 417:70]
  assign collectorUnit_3_io_bankIn_2_bits_v0_0 = crossBar_io_out_3_2_bits_v0_0; // @[operandCollector.scala 388:31 417:70]
  assign collectorUnit_3_io_bankIn_3_valid = crossBar_io_out_3_3_valid; // @[operandCollector.scala 388:31 417:70]
  assign collectorUnit_3_io_bankIn_3_bits_regOrder = crossBar_io_out_3_3_bits_regOrder; // @[operandCollector.scala 388:31 417:70]
  assign collectorUnit_3_io_bankIn_3_bits_data_0 = crossBar_io_out_3_3_bits_data_0; // @[operandCollector.scala 388:31 417:70]
  assign collectorUnit_3_io_bankIn_3_bits_data_1 = crossBar_io_out_3_3_bits_data_1; // @[operandCollector.scala 388:31 417:70]
  assign collectorUnit_3_io_bankIn_3_bits_data_2 = crossBar_io_out_3_3_bits_data_2; // @[operandCollector.scala 388:31 417:70]
  assign collectorUnit_3_io_bankIn_3_bits_data_3 = crossBar_io_out_3_3_bits_data_3; // @[operandCollector.scala 388:31 417:70]
  assign collectorUnit_3_io_bankIn_3_bits_v0_0 = crossBar_io_out_3_3_bits_v0_0; // @[operandCollector.scala 388:31 417:70]
  assign collectorUnit_3_io_issue_ready = issueArbiter_io_in_3_ready; // @[operandCollector.scala 467:{22,32}]
  assign Arbiter_clock = clock;
  assign Arbiter_io_readArbiterIO_0_0_valid = collectorUnit_io_outArbiterIO_0_valid; // @[operandCollector.scala 388:{31,31}]
  assign Arbiter_io_readArbiterIO_0_0_bits_rsAddr = collectorUnit_io_outArbiterIO_0_bits_rsAddr; // @[operandCollector.scala 388:{31,31}]
  assign Arbiter_io_readArbiterIO_0_0_bits_bankID = collectorUnit_io_outArbiterIO_0_bits_bankID; // @[operandCollector.scala 388:{31,31}]
  assign Arbiter_io_readArbiterIO_0_0_bits_rsType = collectorUnit_io_outArbiterIO_0_bits_rsType; // @[operandCollector.scala 388:{31,31}]
  assign Arbiter_io_readArbiterIO_0_1_valid = collectorUnit_io_outArbiterIO_1_valid; // @[operandCollector.scala 388:{31,31}]
  assign Arbiter_io_readArbiterIO_0_1_bits_rsAddr = collectorUnit_io_outArbiterIO_1_bits_rsAddr; // @[operandCollector.scala 388:{31,31}]
  assign Arbiter_io_readArbiterIO_0_1_bits_bankID = collectorUnit_io_outArbiterIO_1_bits_bankID; // @[operandCollector.scala 388:{31,31}]
  assign Arbiter_io_readArbiterIO_0_1_bits_rsType = collectorUnit_io_outArbiterIO_1_bits_rsType; // @[operandCollector.scala 388:{31,31}]
  assign Arbiter_io_readArbiterIO_0_2_valid = collectorUnit_io_outArbiterIO_2_valid; // @[operandCollector.scala 388:{31,31}]
  assign Arbiter_io_readArbiterIO_0_2_bits_rsAddr = collectorUnit_io_outArbiterIO_2_bits_rsAddr; // @[operandCollector.scala 388:{31,31}]
  assign Arbiter_io_readArbiterIO_0_2_bits_bankID = collectorUnit_io_outArbiterIO_2_bits_bankID; // @[operandCollector.scala 388:{31,31}]
  assign Arbiter_io_readArbiterIO_0_2_bits_rsType = collectorUnit_io_outArbiterIO_2_bits_rsType; // @[operandCollector.scala 388:{31,31}]
  assign Arbiter_io_readArbiterIO_0_3_valid = collectorUnit_io_outArbiterIO_3_valid; // @[operandCollector.scala 388:{31,31}]
  assign Arbiter_io_readArbiterIO_0_3_bits_rsAddr = collectorUnit_io_outArbiterIO_3_bits_rsAddr; // @[operandCollector.scala 388:{31,31}]
  assign Arbiter_io_readArbiterIO_0_3_bits_bankID = collectorUnit_io_outArbiterIO_3_bits_bankID; // @[operandCollector.scala 388:{31,31}]
  assign Arbiter_io_readArbiterIO_1_0_valid = collectorUnit_1_io_outArbiterIO_0_valid; // @[operandCollector.scala 388:{31,31}]
  assign Arbiter_io_readArbiterIO_1_0_bits_rsAddr = collectorUnit_1_io_outArbiterIO_0_bits_rsAddr; // @[operandCollector.scala 388:{31,31}]
  assign Arbiter_io_readArbiterIO_1_0_bits_bankID = collectorUnit_1_io_outArbiterIO_0_bits_bankID; // @[operandCollector.scala 388:{31,31}]
  assign Arbiter_io_readArbiterIO_1_0_bits_rsType = collectorUnit_1_io_outArbiterIO_0_bits_rsType; // @[operandCollector.scala 388:{31,31}]
  assign Arbiter_io_readArbiterIO_1_1_valid = collectorUnit_1_io_outArbiterIO_1_valid; // @[operandCollector.scala 388:{31,31}]
  assign Arbiter_io_readArbiterIO_1_1_bits_rsAddr = collectorUnit_1_io_outArbiterIO_1_bits_rsAddr; // @[operandCollector.scala 388:{31,31}]
  assign Arbiter_io_readArbiterIO_1_1_bits_bankID = collectorUnit_1_io_outArbiterIO_1_bits_bankID; // @[operandCollector.scala 388:{31,31}]
  assign Arbiter_io_readArbiterIO_1_1_bits_rsType = collectorUnit_1_io_outArbiterIO_1_bits_rsType; // @[operandCollector.scala 388:{31,31}]
  assign Arbiter_io_readArbiterIO_1_2_valid = collectorUnit_1_io_outArbiterIO_2_valid; // @[operandCollector.scala 388:{31,31}]
  assign Arbiter_io_readArbiterIO_1_2_bits_rsAddr = collectorUnit_1_io_outArbiterIO_2_bits_rsAddr; // @[operandCollector.scala 388:{31,31}]
  assign Arbiter_io_readArbiterIO_1_2_bits_bankID = collectorUnit_1_io_outArbiterIO_2_bits_bankID; // @[operandCollector.scala 388:{31,31}]
  assign Arbiter_io_readArbiterIO_1_2_bits_rsType = collectorUnit_1_io_outArbiterIO_2_bits_rsType; // @[operandCollector.scala 388:{31,31}]
  assign Arbiter_io_readArbiterIO_1_3_valid = collectorUnit_1_io_outArbiterIO_3_valid; // @[operandCollector.scala 388:{31,31}]
  assign Arbiter_io_readArbiterIO_1_3_bits_rsAddr = collectorUnit_1_io_outArbiterIO_3_bits_rsAddr; // @[operandCollector.scala 388:{31,31}]
  assign Arbiter_io_readArbiterIO_1_3_bits_bankID = collectorUnit_1_io_outArbiterIO_3_bits_bankID; // @[operandCollector.scala 388:{31,31}]
  assign Arbiter_io_readArbiterIO_2_0_valid = collectorUnit_2_io_outArbiterIO_0_valid; // @[operandCollector.scala 388:{31,31}]
  assign Arbiter_io_readArbiterIO_2_0_bits_rsAddr = collectorUnit_2_io_outArbiterIO_0_bits_rsAddr; // @[operandCollector.scala 388:{31,31}]
  assign Arbiter_io_readArbiterIO_2_0_bits_bankID = collectorUnit_2_io_outArbiterIO_0_bits_bankID; // @[operandCollector.scala 388:{31,31}]
  assign Arbiter_io_readArbiterIO_2_0_bits_rsType = collectorUnit_2_io_outArbiterIO_0_bits_rsType; // @[operandCollector.scala 388:{31,31}]
  assign Arbiter_io_readArbiterIO_2_1_valid = collectorUnit_2_io_outArbiterIO_1_valid; // @[operandCollector.scala 388:{31,31}]
  assign Arbiter_io_readArbiterIO_2_1_bits_rsAddr = collectorUnit_2_io_outArbiterIO_1_bits_rsAddr; // @[operandCollector.scala 388:{31,31}]
  assign Arbiter_io_readArbiterIO_2_1_bits_bankID = collectorUnit_2_io_outArbiterIO_1_bits_bankID; // @[operandCollector.scala 388:{31,31}]
  assign Arbiter_io_readArbiterIO_2_1_bits_rsType = collectorUnit_2_io_outArbiterIO_1_bits_rsType; // @[operandCollector.scala 388:{31,31}]
  assign Arbiter_io_readArbiterIO_2_2_valid = collectorUnit_2_io_outArbiterIO_2_valid; // @[operandCollector.scala 388:{31,31}]
  assign Arbiter_io_readArbiterIO_2_2_bits_rsAddr = collectorUnit_2_io_outArbiterIO_2_bits_rsAddr; // @[operandCollector.scala 388:{31,31}]
  assign Arbiter_io_readArbiterIO_2_2_bits_bankID = collectorUnit_2_io_outArbiterIO_2_bits_bankID; // @[operandCollector.scala 388:{31,31}]
  assign Arbiter_io_readArbiterIO_2_2_bits_rsType = collectorUnit_2_io_outArbiterIO_2_bits_rsType; // @[operandCollector.scala 388:{31,31}]
  assign Arbiter_io_readArbiterIO_2_3_valid = collectorUnit_2_io_outArbiterIO_3_valid; // @[operandCollector.scala 388:{31,31}]
  assign Arbiter_io_readArbiterIO_2_3_bits_rsAddr = collectorUnit_2_io_outArbiterIO_3_bits_rsAddr; // @[operandCollector.scala 388:{31,31}]
  assign Arbiter_io_readArbiterIO_2_3_bits_bankID = collectorUnit_2_io_outArbiterIO_3_bits_bankID; // @[operandCollector.scala 388:{31,31}]
  assign Arbiter_io_readArbiterIO_3_0_valid = collectorUnit_3_io_outArbiterIO_0_valid; // @[operandCollector.scala 388:{31,31}]
  assign Arbiter_io_readArbiterIO_3_0_bits_rsAddr = collectorUnit_3_io_outArbiterIO_0_bits_rsAddr; // @[operandCollector.scala 388:{31,31}]
  assign Arbiter_io_readArbiterIO_3_0_bits_bankID = collectorUnit_3_io_outArbiterIO_0_bits_bankID; // @[operandCollector.scala 388:{31,31}]
  assign Arbiter_io_readArbiterIO_3_0_bits_rsType = collectorUnit_3_io_outArbiterIO_0_bits_rsType; // @[operandCollector.scala 388:{31,31}]
  assign Arbiter_io_readArbiterIO_3_1_valid = collectorUnit_3_io_outArbiterIO_1_valid; // @[operandCollector.scala 388:{31,31}]
  assign Arbiter_io_readArbiterIO_3_1_bits_rsAddr = collectorUnit_3_io_outArbiterIO_1_bits_rsAddr; // @[operandCollector.scala 388:{31,31}]
  assign Arbiter_io_readArbiterIO_3_1_bits_bankID = collectorUnit_3_io_outArbiterIO_1_bits_bankID; // @[operandCollector.scala 388:{31,31}]
  assign Arbiter_io_readArbiterIO_3_1_bits_rsType = collectorUnit_3_io_outArbiterIO_1_bits_rsType; // @[operandCollector.scala 388:{31,31}]
  assign Arbiter_io_readArbiterIO_3_2_valid = collectorUnit_3_io_outArbiterIO_2_valid; // @[operandCollector.scala 388:{31,31}]
  assign Arbiter_io_readArbiterIO_3_2_bits_rsAddr = collectorUnit_3_io_outArbiterIO_2_bits_rsAddr; // @[operandCollector.scala 388:{31,31}]
  assign Arbiter_io_readArbiterIO_3_2_bits_bankID = collectorUnit_3_io_outArbiterIO_2_bits_bankID; // @[operandCollector.scala 388:{31,31}]
  assign Arbiter_io_readArbiterIO_3_2_bits_rsType = collectorUnit_3_io_outArbiterIO_2_bits_rsType; // @[operandCollector.scala 388:{31,31}]
  assign Arbiter_io_readArbiterIO_3_3_valid = collectorUnit_3_io_outArbiterIO_3_valid; // @[operandCollector.scala 388:{31,31}]
  assign Arbiter_io_readArbiterIO_3_3_bits_rsAddr = collectorUnit_3_io_outArbiterIO_3_bits_rsAddr; // @[operandCollector.scala 388:{31,31}]
  assign Arbiter_io_readArbiterIO_3_3_bits_bankID = collectorUnit_3_io_outArbiterIO_3_bits_bankID; // @[operandCollector.scala 388:{31,31}]
  assign FloatRegFileBank_clock = clock;
  assign FloatRegFileBank_io_rsidx = Arbiter_io_readArbiterOut_0_bits_rsAddr; // @[operandCollector.scala 390:27 397:25]
  assign FloatRegFileBank_io_rd_0 = io_writeVecCtrl_bits_wb_wvd_rd_0; // @[operandCollector.scala 390:27 451:10]
  assign FloatRegFileBank_io_rd_1 = io_writeVecCtrl_bits_wb_wvd_rd_1; // @[operandCollector.scala 390:27 451:10]
  assign FloatRegFileBank_io_rd_2 = io_writeVecCtrl_bits_wb_wvd_rd_2; // @[operandCollector.scala 390:27 451:10]
  assign FloatRegFileBank_io_rd_3 = io_writeVecCtrl_bits_wb_wvd_rd_3; // @[operandCollector.scala 390:27 451:10]
  assign FloatRegFileBank_io_rdidx = _wbVecBankAddr_T_64[4:0]; // @[operandCollector.scala 441:27 445:17]
  assign FloatRegFileBank_io_rdwen = 2'h0 == wbVecBankId & (io_writeVecCtrl_bits_wvd & io_writeVecCtrl_valid); // @[operandCollector.scala 452:13 460:{33,33}]
  assign FloatRegFileBank_io_rdwmask_0 = io_writeVecCtrl_bits_wvd_mask_0; // @[operandCollector.scala 390:27 453:15]
  assign FloatRegFileBank_io_rdwmask_1 = io_writeVecCtrl_bits_wvd_mask_1; // @[operandCollector.scala 390:27 453:15]
  assign FloatRegFileBank_io_rdwmask_2 = io_writeVecCtrl_bits_wvd_mask_2; // @[operandCollector.scala 390:27 453:15]
  assign FloatRegFileBank_io_rdwmask_3 = io_writeVecCtrl_bits_wvd_mask_3; // @[operandCollector.scala 390:27 453:15]
  assign FloatRegFileBank_1_clock = clock;
  assign FloatRegFileBank_1_io_rsidx = Arbiter_io_readArbiterOut_1_bits_rsAddr; // @[operandCollector.scala 390:27 397:25]
  assign FloatRegFileBank_1_io_rd_0 = io_writeVecCtrl_bits_wb_wvd_rd_0; // @[operandCollector.scala 390:27 451:10]
  assign FloatRegFileBank_1_io_rd_1 = io_writeVecCtrl_bits_wb_wvd_rd_1; // @[operandCollector.scala 390:27 451:10]
  assign FloatRegFileBank_1_io_rd_2 = io_writeVecCtrl_bits_wb_wvd_rd_2; // @[operandCollector.scala 390:27 451:10]
  assign FloatRegFileBank_1_io_rd_3 = io_writeVecCtrl_bits_wb_wvd_rd_3; // @[operandCollector.scala 390:27 451:10]
  assign FloatRegFileBank_1_io_rdidx = _wbVecBankAddr_T_64[4:0]; // @[operandCollector.scala 441:27 445:17]
  assign FloatRegFileBank_1_io_rdwen = 2'h1 == wbVecBankId & (io_writeVecCtrl_bits_wvd & io_writeVecCtrl_valid); // @[operandCollector.scala 452:13 460:{33,33}]
  assign FloatRegFileBank_1_io_rdwmask_0 = io_writeVecCtrl_bits_wvd_mask_0; // @[operandCollector.scala 390:27 453:15]
  assign FloatRegFileBank_1_io_rdwmask_1 = io_writeVecCtrl_bits_wvd_mask_1; // @[operandCollector.scala 390:27 453:15]
  assign FloatRegFileBank_1_io_rdwmask_2 = io_writeVecCtrl_bits_wvd_mask_2; // @[operandCollector.scala 390:27 453:15]
  assign FloatRegFileBank_1_io_rdwmask_3 = io_writeVecCtrl_bits_wvd_mask_3; // @[operandCollector.scala 390:27 453:15]
  assign FloatRegFileBank_2_clock = clock;
  assign FloatRegFileBank_2_io_rsidx = Arbiter_io_readArbiterOut_2_bits_rsAddr; // @[operandCollector.scala 390:27 397:25]
  assign FloatRegFileBank_2_io_rd_0 = io_writeVecCtrl_bits_wb_wvd_rd_0; // @[operandCollector.scala 390:27 451:10]
  assign FloatRegFileBank_2_io_rd_1 = io_writeVecCtrl_bits_wb_wvd_rd_1; // @[operandCollector.scala 390:27 451:10]
  assign FloatRegFileBank_2_io_rd_2 = io_writeVecCtrl_bits_wb_wvd_rd_2; // @[operandCollector.scala 390:27 451:10]
  assign FloatRegFileBank_2_io_rd_3 = io_writeVecCtrl_bits_wb_wvd_rd_3; // @[operandCollector.scala 390:27 451:10]
  assign FloatRegFileBank_2_io_rdidx = _wbVecBankAddr_T_64[4:0]; // @[operandCollector.scala 441:27 445:17]
  assign FloatRegFileBank_2_io_rdwen = 2'h2 == wbVecBankId & (io_writeVecCtrl_bits_wvd & io_writeVecCtrl_valid); // @[operandCollector.scala 452:13 460:{33,33}]
  assign FloatRegFileBank_2_io_rdwmask_0 = io_writeVecCtrl_bits_wvd_mask_0; // @[operandCollector.scala 390:27 453:15]
  assign FloatRegFileBank_2_io_rdwmask_1 = io_writeVecCtrl_bits_wvd_mask_1; // @[operandCollector.scala 390:27 453:15]
  assign FloatRegFileBank_2_io_rdwmask_2 = io_writeVecCtrl_bits_wvd_mask_2; // @[operandCollector.scala 390:27 453:15]
  assign FloatRegFileBank_2_io_rdwmask_3 = io_writeVecCtrl_bits_wvd_mask_3; // @[operandCollector.scala 390:27 453:15]
  assign FloatRegFileBank_3_clock = clock;
  assign FloatRegFileBank_3_io_rsidx = Arbiter_io_readArbiterOut_3_bits_rsAddr; // @[operandCollector.scala 390:27 397:25]
  assign FloatRegFileBank_3_io_rd_0 = io_writeVecCtrl_bits_wb_wvd_rd_0; // @[operandCollector.scala 390:27 451:10]
  assign FloatRegFileBank_3_io_rd_1 = io_writeVecCtrl_bits_wb_wvd_rd_1; // @[operandCollector.scala 390:27 451:10]
  assign FloatRegFileBank_3_io_rd_2 = io_writeVecCtrl_bits_wb_wvd_rd_2; // @[operandCollector.scala 390:27 451:10]
  assign FloatRegFileBank_3_io_rd_3 = io_writeVecCtrl_bits_wb_wvd_rd_3; // @[operandCollector.scala 390:27 451:10]
  assign FloatRegFileBank_3_io_rdidx = _wbVecBankAddr_T_64[4:0]; // @[operandCollector.scala 441:27 445:17]
  assign FloatRegFileBank_3_io_rdwen = 2'h3 == wbVecBankId & (io_writeVecCtrl_bits_wvd & io_writeVecCtrl_valid); // @[operandCollector.scala 452:13 460:{33,33}]
  assign FloatRegFileBank_3_io_rdwmask_0 = io_writeVecCtrl_bits_wvd_mask_0; // @[operandCollector.scala 390:27 453:15]
  assign FloatRegFileBank_3_io_rdwmask_1 = io_writeVecCtrl_bits_wvd_mask_1; // @[operandCollector.scala 390:27 453:15]
  assign FloatRegFileBank_3_io_rdwmask_2 = io_writeVecCtrl_bits_wvd_mask_2; // @[operandCollector.scala 390:27 453:15]
  assign FloatRegFileBank_3_io_rdwmask_3 = io_writeVecCtrl_bits_wvd_mask_3; // @[operandCollector.scala 390:27 453:15]
  assign RegFileBank_clock = clock;
  assign RegFileBank_io_rsidx = Arbiter_io_readArbiterOut_0_bits_rsAddr; // @[operandCollector.scala 391:27 398:25]
  assign RegFileBank_io_rd = io_writeScalarCtrl_bits_wb_wxd_rd; // @[operandCollector.scala 391:27 457:10]
  assign RegFileBank_io_rdidx = _wbScaBankAddr_T_64[4:0]; // @[operandCollector.scala 442:27 447:17]
  assign RegFileBank_io_rdwen = 2'h0 == wbScaBankId & (io_writeScalarCtrl_bits_wxd & io_writeScalarCtrl_valid); // @[operandCollector.scala 458:13 461:{33,33}]
  assign RegFileBank_1_clock = clock;
  assign RegFileBank_1_io_rsidx = Arbiter_io_readArbiterOut_1_bits_rsAddr; // @[operandCollector.scala 391:27 398:25]
  assign RegFileBank_1_io_rd = io_writeScalarCtrl_bits_wb_wxd_rd; // @[operandCollector.scala 391:27 457:10]
  assign RegFileBank_1_io_rdidx = _wbScaBankAddr_T_64[4:0]; // @[operandCollector.scala 442:27 447:17]
  assign RegFileBank_1_io_rdwen = 2'h1 == wbScaBankId & (io_writeScalarCtrl_bits_wxd & io_writeScalarCtrl_valid); // @[operandCollector.scala 458:13 461:{33,33}]
  assign RegFileBank_2_clock = clock;
  assign RegFileBank_2_io_rsidx = Arbiter_io_readArbiterOut_2_bits_rsAddr; // @[operandCollector.scala 391:27 398:25]
  assign RegFileBank_2_io_rd = io_writeScalarCtrl_bits_wb_wxd_rd; // @[operandCollector.scala 391:27 457:10]
  assign RegFileBank_2_io_rdidx = _wbScaBankAddr_T_64[4:0]; // @[operandCollector.scala 442:27 447:17]
  assign RegFileBank_2_io_rdwen = 2'h2 == wbScaBankId & (io_writeScalarCtrl_bits_wxd & io_writeScalarCtrl_valid); // @[operandCollector.scala 458:13 461:{33,33}]
  assign RegFileBank_3_clock = clock;
  assign RegFileBank_3_io_rsidx = Arbiter_io_readArbiterOut_3_bits_rsAddr; // @[operandCollector.scala 391:27 398:25]
  assign RegFileBank_3_io_rd = io_writeScalarCtrl_bits_wb_wxd_rd; // @[operandCollector.scala 391:27 457:10]
  assign RegFileBank_3_io_rdidx = _wbScaBankAddr_T_64[4:0]; // @[operandCollector.scala 442:27 447:17]
  assign RegFileBank_3_io_rdwen = 2'h3 == wbScaBankId & (io_writeScalarCtrl_bits_wxd & io_writeScalarCtrl_valid); // @[operandCollector.scala 458:13 461:{33,33}]
  assign crossBar_io_chosen_0 = REG__0; // @[operandCollector.scala 402:22]
  assign crossBar_io_chosen_1 = REG__1; // @[operandCollector.scala 402:22]
  assign crossBar_io_chosen_2 = REG__2; // @[operandCollector.scala 402:22]
  assign crossBar_io_chosen_3 = REG__3; // @[operandCollector.scala 402:22]
  assign crossBar_io_validArbiter_0 = REG_1_0; // @[operandCollector.scala 403:28]
  assign crossBar_io_validArbiter_1 = REG_1_1; // @[operandCollector.scala 403:28]
  assign crossBar_io_validArbiter_2 = REG_1_2; // @[operandCollector.scala 403:28]
  assign crossBar_io_validArbiter_3 = REG_1_3; // @[operandCollector.scala 403:28]
  assign crossBar_io_dataIn_rs_0_0 = REG_2 ? scalarBank_0_rs : _GEN_0; // @[operandCollector.scala 405:66 406:32]
  assign crossBar_io_dataIn_rs_0_1 = REG_2 ? scalarBank_0_rs : _GEN_1; // @[operandCollector.scala 405:66 406:32]
  assign crossBar_io_dataIn_rs_0_2 = REG_2 ? scalarBank_0_rs : _GEN_2; // @[operandCollector.scala 405:66 406:32]
  assign crossBar_io_dataIn_rs_0_3 = REG_2 ? scalarBank_0_rs : _GEN_3; // @[operandCollector.scala 405:66 406:32]
  assign crossBar_io_dataIn_rs_1_0 = REG_4 ? scalarBank_1_rs : _GEN_16; // @[operandCollector.scala 405:66 406:32]
  assign crossBar_io_dataIn_rs_1_1 = REG_4 ? scalarBank_1_rs : _GEN_17; // @[operandCollector.scala 405:66 406:32]
  assign crossBar_io_dataIn_rs_1_2 = REG_4 ? scalarBank_1_rs : _GEN_18; // @[operandCollector.scala 405:66 406:32]
  assign crossBar_io_dataIn_rs_1_3 = REG_4 ? scalarBank_1_rs : _GEN_19; // @[operandCollector.scala 405:66 406:32]
  assign crossBar_io_dataIn_rs_2_0 = REG_6 ? scalarBank_2_rs : _GEN_32; // @[operandCollector.scala 405:66 406:32]
  assign crossBar_io_dataIn_rs_2_1 = REG_6 ? scalarBank_2_rs : _GEN_33; // @[operandCollector.scala 405:66 406:32]
  assign crossBar_io_dataIn_rs_2_2 = REG_6 ? scalarBank_2_rs : _GEN_34; // @[operandCollector.scala 405:66 406:32]
  assign crossBar_io_dataIn_rs_2_3 = REG_6 ? scalarBank_2_rs : _GEN_35; // @[operandCollector.scala 405:66 406:32]
  assign crossBar_io_dataIn_rs_3_0 = REG_8 ? scalarBank_3_rs : _GEN_48; // @[operandCollector.scala 405:66 406:32]
  assign crossBar_io_dataIn_rs_3_1 = REG_8 ? scalarBank_3_rs : _GEN_49; // @[operandCollector.scala 405:66 406:32]
  assign crossBar_io_dataIn_rs_3_2 = REG_8 ? scalarBank_3_rs : _GEN_50; // @[operandCollector.scala 405:66 406:32]
  assign crossBar_io_dataIn_rs_3_3 = REG_8 ? scalarBank_3_rs : _GEN_51; // @[operandCollector.scala 405:66 406:32]
  assign crossBar_io_dataIn_v0_0_0 = REG_2 ? 32'h0 : _GEN_4; // @[operandCollector.scala 405:66 407:32]
  assign crossBar_io_dataIn_v0_1_0 = REG_4 ? 32'h0 : _GEN_20; // @[operandCollector.scala 405:66 407:32]
  assign crossBar_io_dataIn_v0_2_0 = REG_6 ? 32'h0 : _GEN_36; // @[operandCollector.scala 405:66 407:32]
  assign crossBar_io_dataIn_v0_3_0 = REG_8 ? 32'h0 : _GEN_52; // @[operandCollector.scala 405:66 407:32]
  assign Demux_io_in_valid = io_control_valid; // @[operandCollector.scala 428:15]
  assign Demux_io_in_bits_inst = io_control_bits_inst; // @[operandCollector.scala 428:15]
  assign Demux_io_in_bits_wid = io_control_bits_wid; // @[operandCollector.scala 428:15]
  assign Demux_io_in_bits_fp = io_control_bits_fp; // @[operandCollector.scala 428:15]
  assign Demux_io_in_bits_branch = io_control_bits_branch; // @[operandCollector.scala 428:15]
  assign Demux_io_in_bits_simt_stack = io_control_bits_simt_stack; // @[operandCollector.scala 428:15]
  assign Demux_io_in_bits_simt_stack_op = io_control_bits_simt_stack_op; // @[operandCollector.scala 428:15]
  assign Demux_io_in_bits_barrier = io_control_bits_barrier; // @[operandCollector.scala 428:15]
  assign Demux_io_in_bits_csr = io_control_bits_csr; // @[operandCollector.scala 428:15]
  assign Demux_io_in_bits_reverse = io_control_bits_reverse; // @[operandCollector.scala 428:15]
  assign Demux_io_in_bits_sel_alu2 = io_control_bits_sel_alu2; // @[operandCollector.scala 428:15]
  assign Demux_io_in_bits_sel_alu1 = io_control_bits_sel_alu1; // @[operandCollector.scala 428:15]
  assign Demux_io_in_bits_isvec = io_control_bits_isvec; // @[operandCollector.scala 428:15]
  assign Demux_io_in_bits_sel_alu3 = io_control_bits_sel_alu3; // @[operandCollector.scala 428:15]
  assign Demux_io_in_bits_mask = io_control_bits_mask; // @[operandCollector.scala 428:15]
  assign Demux_io_in_bits_sel_imm = io_control_bits_sel_imm; // @[operandCollector.scala 428:15]
  assign Demux_io_in_bits_mem_whb = io_control_bits_mem_whb; // @[operandCollector.scala 428:15]
  assign Demux_io_in_bits_mem_unsigned = io_control_bits_mem_unsigned; // @[operandCollector.scala 428:15]
  assign Demux_io_in_bits_alu_fn = io_control_bits_alu_fn; // @[operandCollector.scala 428:15]
  assign Demux_io_in_bits_mem = io_control_bits_mem; // @[operandCollector.scala 428:15]
  assign Demux_io_in_bits_mul = io_control_bits_mul; // @[operandCollector.scala 428:15]
  assign Demux_io_in_bits_mem_cmd = io_control_bits_mem_cmd; // @[operandCollector.scala 428:15]
  assign Demux_io_in_bits_mop = io_control_bits_mop; // @[operandCollector.scala 428:15]
  assign Demux_io_in_bits_reg_idx1 = io_control_bits_reg_idx1; // @[operandCollector.scala 428:15]
  assign Demux_io_in_bits_reg_idx2 = io_control_bits_reg_idx2; // @[operandCollector.scala 428:15]
  assign Demux_io_in_bits_reg_idx3 = io_control_bits_reg_idx3; // @[operandCollector.scala 428:15]
  assign Demux_io_in_bits_reg_idxw = io_control_bits_reg_idxw; // @[operandCollector.scala 428:15]
  assign Demux_io_in_bits_wfd = io_control_bits_wfd; // @[operandCollector.scala 428:15]
  assign Demux_io_in_bits_fence = io_control_bits_fence; // @[operandCollector.scala 428:15]
  assign Demux_io_in_bits_sfu = io_control_bits_sfu; // @[operandCollector.scala 428:15]
  assign Demux_io_in_bits_readmask = io_control_bits_readmask; // @[operandCollector.scala 428:15]
  assign Demux_io_in_bits_writemask = io_control_bits_writemask; // @[operandCollector.scala 428:15]
  assign Demux_io_in_bits_wxd = io_control_bits_wxd; // @[operandCollector.scala 428:15]
  assign Demux_io_in_bits_pc = io_control_bits_pc; // @[operandCollector.scala 428:15]
  assign Demux_io_in_bits_spike_info_pc = io_control_bits_spike_info_pc; // @[operandCollector.scala 428:15]
  assign Demux_io_in_bits_spike_info_inst = io_control_bits_spike_info_inst; // @[operandCollector.scala 428:15]
  assign Demux_io_out_0_ready = collectorUnit_io_control_ready; // @[operandCollector.scala 388:{31,31}]
  assign Demux_io_out_1_ready = collectorUnit_1_io_control_ready; // @[operandCollector.scala 388:{31,31}]
  assign Demux_io_out_2_ready = collectorUnit_2_io_control_ready; // @[operandCollector.scala 388:{31,31}]
  assign Demux_io_out_3_ready = collectorUnit_3_io_control_ready; // @[operandCollector.scala 388:{31,31}]
  assign issueArbiter_io_in_0_valid = collectorUnit_io_issue_valid; // @[operandCollector.scala 388:{31,31}]
  assign issueArbiter_io_in_0_bits_alu_src1_0 = collectorUnit_io_issue_bits_alu_src1_0; // @[operandCollector.scala 388:{31,31}]
  assign issueArbiter_io_in_0_bits_alu_src1_1 = collectorUnit_io_issue_bits_alu_src1_1; // @[operandCollector.scala 388:{31,31}]
  assign issueArbiter_io_in_0_bits_alu_src1_2 = collectorUnit_io_issue_bits_alu_src1_2; // @[operandCollector.scala 388:{31,31}]
  assign issueArbiter_io_in_0_bits_alu_src1_3 = collectorUnit_io_issue_bits_alu_src1_3; // @[operandCollector.scala 388:{31,31}]
  assign issueArbiter_io_in_0_bits_alu_src2_0 = collectorUnit_io_issue_bits_alu_src2_0; // @[operandCollector.scala 388:{31,31}]
  assign issueArbiter_io_in_0_bits_alu_src2_1 = collectorUnit_io_issue_bits_alu_src2_1; // @[operandCollector.scala 388:{31,31}]
  assign issueArbiter_io_in_0_bits_alu_src2_2 = collectorUnit_io_issue_bits_alu_src2_2; // @[operandCollector.scala 388:{31,31}]
  assign issueArbiter_io_in_0_bits_alu_src2_3 = collectorUnit_io_issue_bits_alu_src2_3; // @[operandCollector.scala 388:{31,31}]
  assign issueArbiter_io_in_0_bits_alu_src3_0 = collectorUnit_io_issue_bits_alu_src3_0; // @[operandCollector.scala 388:{31,31}]
  assign issueArbiter_io_in_0_bits_alu_src3_1 = collectorUnit_io_issue_bits_alu_src3_1; // @[operandCollector.scala 388:{31,31}]
  assign issueArbiter_io_in_0_bits_alu_src3_2 = collectorUnit_io_issue_bits_alu_src3_2; // @[operandCollector.scala 388:{31,31}]
  assign issueArbiter_io_in_0_bits_alu_src3_3 = collectorUnit_io_issue_bits_alu_src3_3; // @[operandCollector.scala 388:{31,31}]
  assign issueArbiter_io_in_0_bits_mask_0 = collectorUnit_io_issue_bits_mask_0; // @[operandCollector.scala 388:{31,31}]
  assign issueArbiter_io_in_0_bits_mask_1 = collectorUnit_io_issue_bits_mask_1; // @[operandCollector.scala 388:{31,31}]
  assign issueArbiter_io_in_0_bits_mask_2 = collectorUnit_io_issue_bits_mask_2; // @[operandCollector.scala 388:{31,31}]
  assign issueArbiter_io_in_0_bits_mask_3 = collectorUnit_io_issue_bits_mask_3; // @[operandCollector.scala 388:{31,31}]
  assign issueArbiter_io_in_0_bits_control_inst = collectorUnit_io_issue_bits_control_inst; // @[operandCollector.scala 388:{31,31}]
  assign issueArbiter_io_in_0_bits_control_wid = collectorUnit_io_issue_bits_control_wid; // @[operandCollector.scala 388:{31,31}]
  assign issueArbiter_io_in_0_bits_control_fp = collectorUnit_io_issue_bits_control_fp; // @[operandCollector.scala 388:{31,31}]
  assign issueArbiter_io_in_0_bits_control_branch = collectorUnit_io_issue_bits_control_branch; // @[operandCollector.scala 388:{31,31}]
  assign issueArbiter_io_in_0_bits_control_simt_stack = collectorUnit_io_issue_bits_control_simt_stack; // @[operandCollector.scala 388:{31,31}]
  assign issueArbiter_io_in_0_bits_control_simt_stack_op = collectorUnit_io_issue_bits_control_simt_stack_op; // @[operandCollector.scala 388:{31,31}]
  assign issueArbiter_io_in_0_bits_control_barrier = collectorUnit_io_issue_bits_control_barrier; // @[operandCollector.scala 388:{31,31}]
  assign issueArbiter_io_in_0_bits_control_csr = collectorUnit_io_issue_bits_control_csr; // @[operandCollector.scala 388:{31,31}]
  assign issueArbiter_io_in_0_bits_control_reverse = collectorUnit_io_issue_bits_control_reverse; // @[operandCollector.scala 388:{31,31}]
  assign issueArbiter_io_in_0_bits_control_sel_alu2 = collectorUnit_io_issue_bits_control_sel_alu2; // @[operandCollector.scala 388:{31,31}]
  assign issueArbiter_io_in_0_bits_control_sel_alu1 = collectorUnit_io_issue_bits_control_sel_alu1; // @[operandCollector.scala 388:{31,31}]
  assign issueArbiter_io_in_0_bits_control_isvec = collectorUnit_io_issue_bits_control_isvec; // @[operandCollector.scala 388:{31,31}]
  assign issueArbiter_io_in_0_bits_control_sel_alu3 = collectorUnit_io_issue_bits_control_sel_alu3; // @[operandCollector.scala 388:{31,31}]
  assign issueArbiter_io_in_0_bits_control_mask = collectorUnit_io_issue_bits_control_mask; // @[operandCollector.scala 388:{31,31}]
  assign issueArbiter_io_in_0_bits_control_sel_imm = collectorUnit_io_issue_bits_control_sel_imm; // @[operandCollector.scala 388:{31,31}]
  assign issueArbiter_io_in_0_bits_control_mem_whb = collectorUnit_io_issue_bits_control_mem_whb; // @[operandCollector.scala 388:{31,31}]
  assign issueArbiter_io_in_0_bits_control_mem_unsigned = collectorUnit_io_issue_bits_control_mem_unsigned; // @[operandCollector.scala 388:{31,31}]
  assign issueArbiter_io_in_0_bits_control_alu_fn = collectorUnit_io_issue_bits_control_alu_fn; // @[operandCollector.scala 388:{31,31}]
  assign issueArbiter_io_in_0_bits_control_mem = collectorUnit_io_issue_bits_control_mem; // @[operandCollector.scala 388:{31,31}]
  assign issueArbiter_io_in_0_bits_control_mul = collectorUnit_io_issue_bits_control_mul; // @[operandCollector.scala 388:{31,31}]
  assign issueArbiter_io_in_0_bits_control_mem_cmd = collectorUnit_io_issue_bits_control_mem_cmd; // @[operandCollector.scala 388:{31,31}]
  assign issueArbiter_io_in_0_bits_control_mop = collectorUnit_io_issue_bits_control_mop; // @[operandCollector.scala 388:{31,31}]
  assign issueArbiter_io_in_0_bits_control_reg_idx1 = collectorUnit_io_issue_bits_control_reg_idx1; // @[operandCollector.scala 388:{31,31}]
  assign issueArbiter_io_in_0_bits_control_reg_idx2 = collectorUnit_io_issue_bits_control_reg_idx2; // @[operandCollector.scala 388:{31,31}]
  assign issueArbiter_io_in_0_bits_control_reg_idx3 = collectorUnit_io_issue_bits_control_reg_idx3; // @[operandCollector.scala 388:{31,31}]
  assign issueArbiter_io_in_0_bits_control_reg_idxw = collectorUnit_io_issue_bits_control_reg_idxw; // @[operandCollector.scala 388:{31,31}]
  assign issueArbiter_io_in_0_bits_control_wfd = collectorUnit_io_issue_bits_control_wfd; // @[operandCollector.scala 388:{31,31}]
  assign issueArbiter_io_in_0_bits_control_fence = collectorUnit_io_issue_bits_control_fence; // @[operandCollector.scala 388:{31,31}]
  assign issueArbiter_io_in_0_bits_control_sfu = collectorUnit_io_issue_bits_control_sfu; // @[operandCollector.scala 388:{31,31}]
  assign issueArbiter_io_in_0_bits_control_readmask = collectorUnit_io_issue_bits_control_readmask; // @[operandCollector.scala 388:{31,31}]
  assign issueArbiter_io_in_0_bits_control_writemask = collectorUnit_io_issue_bits_control_writemask; // @[operandCollector.scala 388:{31,31}]
  assign issueArbiter_io_in_0_bits_control_wxd = collectorUnit_io_issue_bits_control_wxd; // @[operandCollector.scala 388:{31,31}]
  assign issueArbiter_io_in_0_bits_control_pc = collectorUnit_io_issue_bits_control_pc; // @[operandCollector.scala 388:{31,31}]
  assign issueArbiter_io_in_0_bits_control_spike_info_pc = collectorUnit_io_issue_bits_control_spike_info_pc; // @[operandCollector.scala 388:{31,31}]
  assign issueArbiter_io_in_0_bits_control_spike_info_inst = collectorUnit_io_issue_bits_control_spike_info_inst; // @[operandCollector.scala 388:{31,31}]
  assign issueArbiter_io_in_1_valid = collectorUnit_1_io_issue_valid; // @[operandCollector.scala 388:{31,31}]
  assign issueArbiter_io_in_1_bits_alu_src1_0 = collectorUnit_1_io_issue_bits_alu_src1_0; // @[operandCollector.scala 388:{31,31}]
  assign issueArbiter_io_in_1_bits_alu_src1_1 = collectorUnit_1_io_issue_bits_alu_src1_1; // @[operandCollector.scala 388:{31,31}]
  assign issueArbiter_io_in_1_bits_alu_src1_2 = collectorUnit_1_io_issue_bits_alu_src1_2; // @[operandCollector.scala 388:{31,31}]
  assign issueArbiter_io_in_1_bits_alu_src1_3 = collectorUnit_1_io_issue_bits_alu_src1_3; // @[operandCollector.scala 388:{31,31}]
  assign issueArbiter_io_in_1_bits_alu_src2_0 = collectorUnit_1_io_issue_bits_alu_src2_0; // @[operandCollector.scala 388:{31,31}]
  assign issueArbiter_io_in_1_bits_alu_src2_1 = collectorUnit_1_io_issue_bits_alu_src2_1; // @[operandCollector.scala 388:{31,31}]
  assign issueArbiter_io_in_1_bits_alu_src2_2 = collectorUnit_1_io_issue_bits_alu_src2_2; // @[operandCollector.scala 388:{31,31}]
  assign issueArbiter_io_in_1_bits_alu_src2_3 = collectorUnit_1_io_issue_bits_alu_src2_3; // @[operandCollector.scala 388:{31,31}]
  assign issueArbiter_io_in_1_bits_alu_src3_0 = collectorUnit_1_io_issue_bits_alu_src3_0; // @[operandCollector.scala 388:{31,31}]
  assign issueArbiter_io_in_1_bits_alu_src3_1 = collectorUnit_1_io_issue_bits_alu_src3_1; // @[operandCollector.scala 388:{31,31}]
  assign issueArbiter_io_in_1_bits_alu_src3_2 = collectorUnit_1_io_issue_bits_alu_src3_2; // @[operandCollector.scala 388:{31,31}]
  assign issueArbiter_io_in_1_bits_alu_src3_3 = collectorUnit_1_io_issue_bits_alu_src3_3; // @[operandCollector.scala 388:{31,31}]
  assign issueArbiter_io_in_1_bits_mask_0 = collectorUnit_1_io_issue_bits_mask_0; // @[operandCollector.scala 388:{31,31}]
  assign issueArbiter_io_in_1_bits_mask_1 = collectorUnit_1_io_issue_bits_mask_1; // @[operandCollector.scala 388:{31,31}]
  assign issueArbiter_io_in_1_bits_mask_2 = collectorUnit_1_io_issue_bits_mask_2; // @[operandCollector.scala 388:{31,31}]
  assign issueArbiter_io_in_1_bits_mask_3 = collectorUnit_1_io_issue_bits_mask_3; // @[operandCollector.scala 388:{31,31}]
  assign issueArbiter_io_in_1_bits_control_inst = collectorUnit_1_io_issue_bits_control_inst; // @[operandCollector.scala 388:{31,31}]
  assign issueArbiter_io_in_1_bits_control_wid = collectorUnit_1_io_issue_bits_control_wid; // @[operandCollector.scala 388:{31,31}]
  assign issueArbiter_io_in_1_bits_control_fp = collectorUnit_1_io_issue_bits_control_fp; // @[operandCollector.scala 388:{31,31}]
  assign issueArbiter_io_in_1_bits_control_branch = collectorUnit_1_io_issue_bits_control_branch; // @[operandCollector.scala 388:{31,31}]
  assign issueArbiter_io_in_1_bits_control_simt_stack = collectorUnit_1_io_issue_bits_control_simt_stack; // @[operandCollector.scala 388:{31,31}]
  assign issueArbiter_io_in_1_bits_control_simt_stack_op = collectorUnit_1_io_issue_bits_control_simt_stack_op; // @[operandCollector.scala 388:{31,31}]
  assign issueArbiter_io_in_1_bits_control_barrier = collectorUnit_1_io_issue_bits_control_barrier; // @[operandCollector.scala 388:{31,31}]
  assign issueArbiter_io_in_1_bits_control_csr = collectorUnit_1_io_issue_bits_control_csr; // @[operandCollector.scala 388:{31,31}]
  assign issueArbiter_io_in_1_bits_control_reverse = collectorUnit_1_io_issue_bits_control_reverse; // @[operandCollector.scala 388:{31,31}]
  assign issueArbiter_io_in_1_bits_control_sel_alu2 = collectorUnit_1_io_issue_bits_control_sel_alu2; // @[operandCollector.scala 388:{31,31}]
  assign issueArbiter_io_in_1_bits_control_sel_alu1 = collectorUnit_1_io_issue_bits_control_sel_alu1; // @[operandCollector.scala 388:{31,31}]
  assign issueArbiter_io_in_1_bits_control_isvec = collectorUnit_1_io_issue_bits_control_isvec; // @[operandCollector.scala 388:{31,31}]
  assign issueArbiter_io_in_1_bits_control_sel_alu3 = collectorUnit_1_io_issue_bits_control_sel_alu3; // @[operandCollector.scala 388:{31,31}]
  assign issueArbiter_io_in_1_bits_control_mask = collectorUnit_1_io_issue_bits_control_mask; // @[operandCollector.scala 388:{31,31}]
  assign issueArbiter_io_in_1_bits_control_sel_imm = collectorUnit_1_io_issue_bits_control_sel_imm; // @[operandCollector.scala 388:{31,31}]
  assign issueArbiter_io_in_1_bits_control_mem_whb = collectorUnit_1_io_issue_bits_control_mem_whb; // @[operandCollector.scala 388:{31,31}]
  assign issueArbiter_io_in_1_bits_control_mem_unsigned = collectorUnit_1_io_issue_bits_control_mem_unsigned; // @[operandCollector.scala 388:{31,31}]
  assign issueArbiter_io_in_1_bits_control_alu_fn = collectorUnit_1_io_issue_bits_control_alu_fn; // @[operandCollector.scala 388:{31,31}]
  assign issueArbiter_io_in_1_bits_control_mem = collectorUnit_1_io_issue_bits_control_mem; // @[operandCollector.scala 388:{31,31}]
  assign issueArbiter_io_in_1_bits_control_mul = collectorUnit_1_io_issue_bits_control_mul; // @[operandCollector.scala 388:{31,31}]
  assign issueArbiter_io_in_1_bits_control_mem_cmd = collectorUnit_1_io_issue_bits_control_mem_cmd; // @[operandCollector.scala 388:{31,31}]
  assign issueArbiter_io_in_1_bits_control_mop = collectorUnit_1_io_issue_bits_control_mop; // @[operandCollector.scala 388:{31,31}]
  assign issueArbiter_io_in_1_bits_control_reg_idx1 = collectorUnit_1_io_issue_bits_control_reg_idx1; // @[operandCollector.scala 388:{31,31}]
  assign issueArbiter_io_in_1_bits_control_reg_idx2 = collectorUnit_1_io_issue_bits_control_reg_idx2; // @[operandCollector.scala 388:{31,31}]
  assign issueArbiter_io_in_1_bits_control_reg_idx3 = collectorUnit_1_io_issue_bits_control_reg_idx3; // @[operandCollector.scala 388:{31,31}]
  assign issueArbiter_io_in_1_bits_control_reg_idxw = collectorUnit_1_io_issue_bits_control_reg_idxw; // @[operandCollector.scala 388:{31,31}]
  assign issueArbiter_io_in_1_bits_control_wfd = collectorUnit_1_io_issue_bits_control_wfd; // @[operandCollector.scala 388:{31,31}]
  assign issueArbiter_io_in_1_bits_control_fence = collectorUnit_1_io_issue_bits_control_fence; // @[operandCollector.scala 388:{31,31}]
  assign issueArbiter_io_in_1_bits_control_sfu = collectorUnit_1_io_issue_bits_control_sfu; // @[operandCollector.scala 388:{31,31}]
  assign issueArbiter_io_in_1_bits_control_readmask = collectorUnit_1_io_issue_bits_control_readmask; // @[operandCollector.scala 388:{31,31}]
  assign issueArbiter_io_in_1_bits_control_writemask = collectorUnit_1_io_issue_bits_control_writemask; // @[operandCollector.scala 388:{31,31}]
  assign issueArbiter_io_in_1_bits_control_wxd = collectorUnit_1_io_issue_bits_control_wxd; // @[operandCollector.scala 388:{31,31}]
  assign issueArbiter_io_in_1_bits_control_pc = collectorUnit_1_io_issue_bits_control_pc; // @[operandCollector.scala 388:{31,31}]
  assign issueArbiter_io_in_1_bits_control_spike_info_pc = collectorUnit_1_io_issue_bits_control_spike_info_pc; // @[operandCollector.scala 388:{31,31}]
  assign issueArbiter_io_in_1_bits_control_spike_info_inst = collectorUnit_1_io_issue_bits_control_spike_info_inst; // @[operandCollector.scala 388:{31,31}]
  assign issueArbiter_io_in_2_valid = collectorUnit_2_io_issue_valid; // @[operandCollector.scala 388:{31,31}]
  assign issueArbiter_io_in_2_bits_alu_src1_0 = collectorUnit_2_io_issue_bits_alu_src1_0; // @[operandCollector.scala 388:{31,31}]
  assign issueArbiter_io_in_2_bits_alu_src1_1 = collectorUnit_2_io_issue_bits_alu_src1_1; // @[operandCollector.scala 388:{31,31}]
  assign issueArbiter_io_in_2_bits_alu_src1_2 = collectorUnit_2_io_issue_bits_alu_src1_2; // @[operandCollector.scala 388:{31,31}]
  assign issueArbiter_io_in_2_bits_alu_src1_3 = collectorUnit_2_io_issue_bits_alu_src1_3; // @[operandCollector.scala 388:{31,31}]
  assign issueArbiter_io_in_2_bits_alu_src2_0 = collectorUnit_2_io_issue_bits_alu_src2_0; // @[operandCollector.scala 388:{31,31}]
  assign issueArbiter_io_in_2_bits_alu_src2_1 = collectorUnit_2_io_issue_bits_alu_src2_1; // @[operandCollector.scala 388:{31,31}]
  assign issueArbiter_io_in_2_bits_alu_src2_2 = collectorUnit_2_io_issue_bits_alu_src2_2; // @[operandCollector.scala 388:{31,31}]
  assign issueArbiter_io_in_2_bits_alu_src2_3 = collectorUnit_2_io_issue_bits_alu_src2_3; // @[operandCollector.scala 388:{31,31}]
  assign issueArbiter_io_in_2_bits_alu_src3_0 = collectorUnit_2_io_issue_bits_alu_src3_0; // @[operandCollector.scala 388:{31,31}]
  assign issueArbiter_io_in_2_bits_alu_src3_1 = collectorUnit_2_io_issue_bits_alu_src3_1; // @[operandCollector.scala 388:{31,31}]
  assign issueArbiter_io_in_2_bits_alu_src3_2 = collectorUnit_2_io_issue_bits_alu_src3_2; // @[operandCollector.scala 388:{31,31}]
  assign issueArbiter_io_in_2_bits_alu_src3_3 = collectorUnit_2_io_issue_bits_alu_src3_3; // @[operandCollector.scala 388:{31,31}]
  assign issueArbiter_io_in_2_bits_mask_0 = collectorUnit_2_io_issue_bits_mask_0; // @[operandCollector.scala 388:{31,31}]
  assign issueArbiter_io_in_2_bits_mask_1 = collectorUnit_2_io_issue_bits_mask_1; // @[operandCollector.scala 388:{31,31}]
  assign issueArbiter_io_in_2_bits_mask_2 = collectorUnit_2_io_issue_bits_mask_2; // @[operandCollector.scala 388:{31,31}]
  assign issueArbiter_io_in_2_bits_mask_3 = collectorUnit_2_io_issue_bits_mask_3; // @[operandCollector.scala 388:{31,31}]
  assign issueArbiter_io_in_2_bits_control_inst = collectorUnit_2_io_issue_bits_control_inst; // @[operandCollector.scala 388:{31,31}]
  assign issueArbiter_io_in_2_bits_control_wid = collectorUnit_2_io_issue_bits_control_wid; // @[operandCollector.scala 388:{31,31}]
  assign issueArbiter_io_in_2_bits_control_fp = collectorUnit_2_io_issue_bits_control_fp; // @[operandCollector.scala 388:{31,31}]
  assign issueArbiter_io_in_2_bits_control_branch = collectorUnit_2_io_issue_bits_control_branch; // @[operandCollector.scala 388:{31,31}]
  assign issueArbiter_io_in_2_bits_control_simt_stack = collectorUnit_2_io_issue_bits_control_simt_stack; // @[operandCollector.scala 388:{31,31}]
  assign issueArbiter_io_in_2_bits_control_simt_stack_op = collectorUnit_2_io_issue_bits_control_simt_stack_op; // @[operandCollector.scala 388:{31,31}]
  assign issueArbiter_io_in_2_bits_control_barrier = collectorUnit_2_io_issue_bits_control_barrier; // @[operandCollector.scala 388:{31,31}]
  assign issueArbiter_io_in_2_bits_control_csr = collectorUnit_2_io_issue_bits_control_csr; // @[operandCollector.scala 388:{31,31}]
  assign issueArbiter_io_in_2_bits_control_reverse = collectorUnit_2_io_issue_bits_control_reverse; // @[operandCollector.scala 388:{31,31}]
  assign issueArbiter_io_in_2_bits_control_sel_alu2 = collectorUnit_2_io_issue_bits_control_sel_alu2; // @[operandCollector.scala 388:{31,31}]
  assign issueArbiter_io_in_2_bits_control_sel_alu1 = collectorUnit_2_io_issue_bits_control_sel_alu1; // @[operandCollector.scala 388:{31,31}]
  assign issueArbiter_io_in_2_bits_control_isvec = collectorUnit_2_io_issue_bits_control_isvec; // @[operandCollector.scala 388:{31,31}]
  assign issueArbiter_io_in_2_bits_control_sel_alu3 = collectorUnit_2_io_issue_bits_control_sel_alu3; // @[operandCollector.scala 388:{31,31}]
  assign issueArbiter_io_in_2_bits_control_mask = collectorUnit_2_io_issue_bits_control_mask; // @[operandCollector.scala 388:{31,31}]
  assign issueArbiter_io_in_2_bits_control_sel_imm = collectorUnit_2_io_issue_bits_control_sel_imm; // @[operandCollector.scala 388:{31,31}]
  assign issueArbiter_io_in_2_bits_control_mem_whb = collectorUnit_2_io_issue_bits_control_mem_whb; // @[operandCollector.scala 388:{31,31}]
  assign issueArbiter_io_in_2_bits_control_mem_unsigned = collectorUnit_2_io_issue_bits_control_mem_unsigned; // @[operandCollector.scala 388:{31,31}]
  assign issueArbiter_io_in_2_bits_control_alu_fn = collectorUnit_2_io_issue_bits_control_alu_fn; // @[operandCollector.scala 388:{31,31}]
  assign issueArbiter_io_in_2_bits_control_mem = collectorUnit_2_io_issue_bits_control_mem; // @[operandCollector.scala 388:{31,31}]
  assign issueArbiter_io_in_2_bits_control_mul = collectorUnit_2_io_issue_bits_control_mul; // @[operandCollector.scala 388:{31,31}]
  assign issueArbiter_io_in_2_bits_control_mem_cmd = collectorUnit_2_io_issue_bits_control_mem_cmd; // @[operandCollector.scala 388:{31,31}]
  assign issueArbiter_io_in_2_bits_control_mop = collectorUnit_2_io_issue_bits_control_mop; // @[operandCollector.scala 388:{31,31}]
  assign issueArbiter_io_in_2_bits_control_reg_idx1 = collectorUnit_2_io_issue_bits_control_reg_idx1; // @[operandCollector.scala 388:{31,31}]
  assign issueArbiter_io_in_2_bits_control_reg_idx2 = collectorUnit_2_io_issue_bits_control_reg_idx2; // @[operandCollector.scala 388:{31,31}]
  assign issueArbiter_io_in_2_bits_control_reg_idx3 = collectorUnit_2_io_issue_bits_control_reg_idx3; // @[operandCollector.scala 388:{31,31}]
  assign issueArbiter_io_in_2_bits_control_reg_idxw = collectorUnit_2_io_issue_bits_control_reg_idxw; // @[operandCollector.scala 388:{31,31}]
  assign issueArbiter_io_in_2_bits_control_wfd = collectorUnit_2_io_issue_bits_control_wfd; // @[operandCollector.scala 388:{31,31}]
  assign issueArbiter_io_in_2_bits_control_fence = collectorUnit_2_io_issue_bits_control_fence; // @[operandCollector.scala 388:{31,31}]
  assign issueArbiter_io_in_2_bits_control_sfu = collectorUnit_2_io_issue_bits_control_sfu; // @[operandCollector.scala 388:{31,31}]
  assign issueArbiter_io_in_2_bits_control_readmask = collectorUnit_2_io_issue_bits_control_readmask; // @[operandCollector.scala 388:{31,31}]
  assign issueArbiter_io_in_2_bits_control_writemask = collectorUnit_2_io_issue_bits_control_writemask; // @[operandCollector.scala 388:{31,31}]
  assign issueArbiter_io_in_2_bits_control_wxd = collectorUnit_2_io_issue_bits_control_wxd; // @[operandCollector.scala 388:{31,31}]
  assign issueArbiter_io_in_2_bits_control_pc = collectorUnit_2_io_issue_bits_control_pc; // @[operandCollector.scala 388:{31,31}]
  assign issueArbiter_io_in_2_bits_control_spike_info_pc = collectorUnit_2_io_issue_bits_control_spike_info_pc; // @[operandCollector.scala 388:{31,31}]
  assign issueArbiter_io_in_2_bits_control_spike_info_inst = collectorUnit_2_io_issue_bits_control_spike_info_inst; // @[operandCollector.scala 388:{31,31}]
  assign issueArbiter_io_in_3_valid = collectorUnit_3_io_issue_valid; // @[operandCollector.scala 388:{31,31}]
  assign issueArbiter_io_in_3_bits_alu_src1_0 = collectorUnit_3_io_issue_bits_alu_src1_0; // @[operandCollector.scala 388:{31,31}]
  assign issueArbiter_io_in_3_bits_alu_src1_1 = collectorUnit_3_io_issue_bits_alu_src1_1; // @[operandCollector.scala 388:{31,31}]
  assign issueArbiter_io_in_3_bits_alu_src1_2 = collectorUnit_3_io_issue_bits_alu_src1_2; // @[operandCollector.scala 388:{31,31}]
  assign issueArbiter_io_in_3_bits_alu_src1_3 = collectorUnit_3_io_issue_bits_alu_src1_3; // @[operandCollector.scala 388:{31,31}]
  assign issueArbiter_io_in_3_bits_alu_src2_0 = collectorUnit_3_io_issue_bits_alu_src2_0; // @[operandCollector.scala 388:{31,31}]
  assign issueArbiter_io_in_3_bits_alu_src2_1 = collectorUnit_3_io_issue_bits_alu_src2_1; // @[operandCollector.scala 388:{31,31}]
  assign issueArbiter_io_in_3_bits_alu_src2_2 = collectorUnit_3_io_issue_bits_alu_src2_2; // @[operandCollector.scala 388:{31,31}]
  assign issueArbiter_io_in_3_bits_alu_src2_3 = collectorUnit_3_io_issue_bits_alu_src2_3; // @[operandCollector.scala 388:{31,31}]
  assign issueArbiter_io_in_3_bits_alu_src3_0 = collectorUnit_3_io_issue_bits_alu_src3_0; // @[operandCollector.scala 388:{31,31}]
  assign issueArbiter_io_in_3_bits_alu_src3_1 = collectorUnit_3_io_issue_bits_alu_src3_1; // @[operandCollector.scala 388:{31,31}]
  assign issueArbiter_io_in_3_bits_alu_src3_2 = collectorUnit_3_io_issue_bits_alu_src3_2; // @[operandCollector.scala 388:{31,31}]
  assign issueArbiter_io_in_3_bits_alu_src3_3 = collectorUnit_3_io_issue_bits_alu_src3_3; // @[operandCollector.scala 388:{31,31}]
  assign issueArbiter_io_in_3_bits_mask_0 = collectorUnit_3_io_issue_bits_mask_0; // @[operandCollector.scala 388:{31,31}]
  assign issueArbiter_io_in_3_bits_mask_1 = collectorUnit_3_io_issue_bits_mask_1; // @[operandCollector.scala 388:{31,31}]
  assign issueArbiter_io_in_3_bits_mask_2 = collectorUnit_3_io_issue_bits_mask_2; // @[operandCollector.scala 388:{31,31}]
  assign issueArbiter_io_in_3_bits_mask_3 = collectorUnit_3_io_issue_bits_mask_3; // @[operandCollector.scala 388:{31,31}]
  assign issueArbiter_io_in_3_bits_control_inst = collectorUnit_3_io_issue_bits_control_inst; // @[operandCollector.scala 388:{31,31}]
  assign issueArbiter_io_in_3_bits_control_wid = collectorUnit_3_io_issue_bits_control_wid; // @[operandCollector.scala 388:{31,31}]
  assign issueArbiter_io_in_3_bits_control_fp = collectorUnit_3_io_issue_bits_control_fp; // @[operandCollector.scala 388:{31,31}]
  assign issueArbiter_io_in_3_bits_control_branch = collectorUnit_3_io_issue_bits_control_branch; // @[operandCollector.scala 388:{31,31}]
  assign issueArbiter_io_in_3_bits_control_simt_stack = collectorUnit_3_io_issue_bits_control_simt_stack; // @[operandCollector.scala 388:{31,31}]
  assign issueArbiter_io_in_3_bits_control_simt_stack_op = collectorUnit_3_io_issue_bits_control_simt_stack_op; // @[operandCollector.scala 388:{31,31}]
  assign issueArbiter_io_in_3_bits_control_barrier = collectorUnit_3_io_issue_bits_control_barrier; // @[operandCollector.scala 388:{31,31}]
  assign issueArbiter_io_in_3_bits_control_csr = collectorUnit_3_io_issue_bits_control_csr; // @[operandCollector.scala 388:{31,31}]
  assign issueArbiter_io_in_3_bits_control_reverse = collectorUnit_3_io_issue_bits_control_reverse; // @[operandCollector.scala 388:{31,31}]
  assign issueArbiter_io_in_3_bits_control_sel_alu2 = collectorUnit_3_io_issue_bits_control_sel_alu2; // @[operandCollector.scala 388:{31,31}]
  assign issueArbiter_io_in_3_bits_control_sel_alu1 = collectorUnit_3_io_issue_bits_control_sel_alu1; // @[operandCollector.scala 388:{31,31}]
  assign issueArbiter_io_in_3_bits_control_isvec = collectorUnit_3_io_issue_bits_control_isvec; // @[operandCollector.scala 388:{31,31}]
  assign issueArbiter_io_in_3_bits_control_sel_alu3 = collectorUnit_3_io_issue_bits_control_sel_alu3; // @[operandCollector.scala 388:{31,31}]
  assign issueArbiter_io_in_3_bits_control_mask = collectorUnit_3_io_issue_bits_control_mask; // @[operandCollector.scala 388:{31,31}]
  assign issueArbiter_io_in_3_bits_control_sel_imm = collectorUnit_3_io_issue_bits_control_sel_imm; // @[operandCollector.scala 388:{31,31}]
  assign issueArbiter_io_in_3_bits_control_mem_whb = collectorUnit_3_io_issue_bits_control_mem_whb; // @[operandCollector.scala 388:{31,31}]
  assign issueArbiter_io_in_3_bits_control_mem_unsigned = collectorUnit_3_io_issue_bits_control_mem_unsigned; // @[operandCollector.scala 388:{31,31}]
  assign issueArbiter_io_in_3_bits_control_alu_fn = collectorUnit_3_io_issue_bits_control_alu_fn; // @[operandCollector.scala 388:{31,31}]
  assign issueArbiter_io_in_3_bits_control_mem = collectorUnit_3_io_issue_bits_control_mem; // @[operandCollector.scala 388:{31,31}]
  assign issueArbiter_io_in_3_bits_control_mul = collectorUnit_3_io_issue_bits_control_mul; // @[operandCollector.scala 388:{31,31}]
  assign issueArbiter_io_in_3_bits_control_mem_cmd = collectorUnit_3_io_issue_bits_control_mem_cmd; // @[operandCollector.scala 388:{31,31}]
  assign issueArbiter_io_in_3_bits_control_mop = collectorUnit_3_io_issue_bits_control_mop; // @[operandCollector.scala 388:{31,31}]
  assign issueArbiter_io_in_3_bits_control_reg_idx1 = collectorUnit_3_io_issue_bits_control_reg_idx1; // @[operandCollector.scala 388:{31,31}]
  assign issueArbiter_io_in_3_bits_control_reg_idx2 = collectorUnit_3_io_issue_bits_control_reg_idx2; // @[operandCollector.scala 388:{31,31}]
  assign issueArbiter_io_in_3_bits_control_reg_idx3 = collectorUnit_3_io_issue_bits_control_reg_idx3; // @[operandCollector.scala 388:{31,31}]
  assign issueArbiter_io_in_3_bits_control_reg_idxw = collectorUnit_3_io_issue_bits_control_reg_idxw; // @[operandCollector.scala 388:{31,31}]
  assign issueArbiter_io_in_3_bits_control_wfd = collectorUnit_3_io_issue_bits_control_wfd; // @[operandCollector.scala 388:{31,31}]
  assign issueArbiter_io_in_3_bits_control_fence = collectorUnit_3_io_issue_bits_control_fence; // @[operandCollector.scala 388:{31,31}]
  assign issueArbiter_io_in_3_bits_control_sfu = collectorUnit_3_io_issue_bits_control_sfu; // @[operandCollector.scala 388:{31,31}]
  assign issueArbiter_io_in_3_bits_control_readmask = collectorUnit_3_io_issue_bits_control_readmask; // @[operandCollector.scala 388:{31,31}]
  assign issueArbiter_io_in_3_bits_control_writemask = collectorUnit_3_io_issue_bits_control_writemask; // @[operandCollector.scala 388:{31,31}]
  assign issueArbiter_io_in_3_bits_control_wxd = collectorUnit_3_io_issue_bits_control_wxd; // @[operandCollector.scala 388:{31,31}]
  assign issueArbiter_io_in_3_bits_control_pc = collectorUnit_3_io_issue_bits_control_pc; // @[operandCollector.scala 388:{31,31}]
  assign issueArbiter_io_in_3_bits_control_spike_info_pc = collectorUnit_3_io_issue_bits_control_spike_info_pc; // @[operandCollector.scala 388:{31,31}]
  assign issueArbiter_io_in_3_bits_control_spike_info_inst = collectorUnit_3_io_issue_bits_control_spike_info_inst; // @[operandCollector.scala 388:{31,31}]
  assign issueArbiter_io_out_ready = io_out_ready; // @[operandCollector.scala 468:10]
  always @(posedge clock) begin
    REG__0 <= Arbiter_io_readchosen_0; // @[operandCollector.scala 402:32]
    REG__1 <= Arbiter_io_readchosen_1; // @[operandCollector.scala 402:32]
    REG__2 <= Arbiter_io_readchosen_2; // @[operandCollector.scala 402:32]
    REG__3 <= Arbiter_io_readchosen_3; // @[operandCollector.scala 402:32]
    REG_1_0 <= Arbiter_io_readArbiterOut_0_valid; // @[operandCollector.scala 403:{46,46}]
    REG_1_1 <= Arbiter_io_readArbiterOut_1_valid; // @[operandCollector.scala 403:{46,46}]
    REG_1_2 <= Arbiter_io_readArbiterOut_2_valid; // @[operandCollector.scala 403:{46,46}]
    REG_1_3 <= Arbiter_io_readArbiterOut_3_valid; // @[operandCollector.scala 403:{46,46}]
    REG_2 <= Arbiter_io_readArbiterOut_0_bits_rsType == 2'h1; // @[operandCollector.scala 405:58]
    REG_3 <= Arbiter_io_readArbiterOut_0_bits_rsType == 2'h2; // @[operandCollector.scala 408:64]
    REG_4 <= Arbiter_io_readArbiterOut_1_bits_rsType == 2'h1; // @[operandCollector.scala 405:58]
    REG_5 <= Arbiter_io_readArbiterOut_1_bits_rsType == 2'h2; // @[operandCollector.scala 408:64]
    REG_6 <= Arbiter_io_readArbiterOut_2_bits_rsType == 2'h1; // @[operandCollector.scala 405:58]
    REG_7 <= Arbiter_io_readArbiterOut_2_bits_rsType == 2'h2; // @[operandCollector.scala 408:64]
    REG_8 <= Arbiter_io_readArbiterOut_3_bits_rsType == 2'h1; // @[operandCollector.scala 405:58]
    REG_9 <= Arbiter_io_readArbiterOut_3_bits_rsType == 2'h2; // @[operandCollector.scala 408:64]
  end
// Register and memory initialization
`ifdef RANDOMIZE_GARBAGE_ASSIGN
`define RANDOMIZE
`endif
`ifdef RANDOMIZE_INVALID_ASSIGN
`define RANDOMIZE
`endif
`ifdef RANDOMIZE_REG_INIT
`define RANDOMIZE
`endif
`ifdef RANDOMIZE_MEM_INIT
`define RANDOMIZE
`endif
`ifndef RANDOM
`define RANDOM $random
`endif
`ifdef RANDOMIZE_MEM_INIT
  integer initvar;
`endif
`ifndef SYNTHESIS
`ifdef FIRRTL_BEFORE_INITIAL
`FIRRTL_BEFORE_INITIAL
`endif
initial begin
  `ifdef RANDOMIZE
    `ifdef INIT_RANDOM
      `INIT_RANDOM
    `endif
    `ifndef VERILATOR
      `ifdef RANDOMIZE_DELAY
        #`RANDOMIZE_DELAY begin end
      `else
        #0.002 begin end
      `endif
    `endif
`ifdef RANDOMIZE_REG_INIT
  _RAND_0 = {1{`RANDOM}};
  REG__0 = _RAND_0[3:0];
  _RAND_1 = {1{`RANDOM}};
  REG__1 = _RAND_1[3:0];
  _RAND_2 = {1{`RANDOM}};
  REG__2 = _RAND_2[3:0];
  _RAND_3 = {1{`RANDOM}};
  REG__3 = _RAND_3[3:0];
  _RAND_4 = {1{`RANDOM}};
  REG_1_0 = _RAND_4[0:0];
  _RAND_5 = {1{`RANDOM}};
  REG_1_1 = _RAND_5[0:0];
  _RAND_6 = {1{`RANDOM}};
  REG_1_2 = _RAND_6[0:0];
  _RAND_7 = {1{`RANDOM}};
  REG_1_3 = _RAND_7[0:0];
  _RAND_8 = {1{`RANDOM}};
  REG_2 = _RAND_8[0:0];
  _RAND_9 = {1{`RANDOM}};
  REG_3 = _RAND_9[0:0];
  _RAND_10 = {1{`RANDOM}};
  REG_4 = _RAND_10[0:0];
  _RAND_11 = {1{`RANDOM}};
  REG_5 = _RAND_11[0:0];
  _RAND_12 = {1{`RANDOM}};
  REG_6 = _RAND_12[0:0];
  _RAND_13 = {1{`RANDOM}};
  REG_7 = _RAND_13[0:0];
  _RAND_14 = {1{`RANDOM}};
  REG_8 = _RAND_14[0:0];
  _RAND_15 = {1{`RANDOM}};
  REG_9 = _RAND_15[0:0];
`endif // RANDOMIZE_REG_INIT
  `endif // RANDOMIZE
end // initial
`ifdef FIRRTL_AFTER_INITIAL
`FIRRTL_AFTER_INITIAL
`endif
`endif // SYNTHESIS
endmodule
