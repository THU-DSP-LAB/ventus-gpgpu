module ScalarALU(
  input  [4:0]  io_func,
  input  [31:0] io_in2,
  input  [31:0] io_in1,
  output [31:0] io_out,
  output        io_cmp_out
);
  wire  _in2_inv_T_1 = io_func <= 5'hf; // @[ALU.scala 56:49]
  wire  _in2_inv_T_2 = io_func >= 5'ha & io_func <= 5'hf; // @[ALU.scala 56:42]
  wire [31:0] _in2_inv_T_3 = ~io_in2; // @[ALU.scala 80:38]
  wire [31:0] in2_inv = _in2_inv_T_2 ? _in2_inv_T_3 : io_in2; // @[ALU.scala 80:20]
  wire [31:0] _adder_out_T_1 = io_in1 + in2_inv; // @[ALU.scala 81:26]
  wire [31:0] _GEN_0 = {{31'd0}, _in2_inv_T_2}; // @[ALU.scala 81:36]
  wire [31:0] adder_out = _adder_out_T_1 + _GEN_0; // @[ALU.scala 81:36]
  wire [31:0] in1_xor_in2 = io_in1 ^ in2_inv; // @[ALU.scala 82:28]
  wire  _slt_T_7 = io_func[1] ? io_in2[31] : io_in1[31]; // @[ALU.scala 87:10]
  wire  slt = io_in1[31] == io_in2[31] ? adder_out[31] : _slt_T_7; // @[ALU.scala 86:8]
  wire  _io_cmp_out_T_2 = ~io_func[3]; // @[ALU.scala 60:26]
  wire  _io_cmp_out_T_4 = _io_cmp_out_T_2 ? in1_xor_in2 == 32'h0 : slt; // @[ALU.scala 88:43]
  wire [4:0] shamt = io_in2[4:0]; // @[ALU.scala 91:32]
  wire  _shin_T_2 = io_func == 5'h5 | io_func == 5'hb; // @[ALU.scala 92:36]
  wire [31:0] _GEN_1 = {{16'd0}, io_in1[31:16]}; // @[Bitwise.scala 105:31]
  wire [31:0] _shin_T_6 = _GEN_1 & 32'hffff; // @[Bitwise.scala 105:31]
  wire [31:0] _shin_T_8 = {io_in1[15:0], 16'h0}; // @[Bitwise.scala 105:70]
  wire [31:0] _shin_T_10 = _shin_T_8 & 32'hffff0000; // @[Bitwise.scala 105:80]
  wire [31:0] _shin_T_11 = _shin_T_6 | _shin_T_10; // @[Bitwise.scala 105:39]
  wire [31:0] _GEN_2 = {{8'd0}, _shin_T_11[31:8]}; // @[Bitwise.scala 105:31]
  wire [31:0] _shin_T_16 = _GEN_2 & 32'hff00ff; // @[Bitwise.scala 105:31]
  wire [31:0] _shin_T_18 = {_shin_T_11[23:0], 8'h0}; // @[Bitwise.scala 105:70]
  wire [31:0] _shin_T_20 = _shin_T_18 & 32'hff00ff00; // @[Bitwise.scala 105:80]
  wire [31:0] _shin_T_21 = _shin_T_16 | _shin_T_20; // @[Bitwise.scala 105:39]
  wire [31:0] _GEN_3 = {{4'd0}, _shin_T_21[31:4]}; // @[Bitwise.scala 105:31]
  wire [31:0] _shin_T_26 = _GEN_3 & 32'hf0f0f0f; // @[Bitwise.scala 105:31]
  wire [31:0] _shin_T_28 = {_shin_T_21[27:0], 4'h0}; // @[Bitwise.scala 105:70]
  wire [31:0] _shin_T_30 = _shin_T_28 & 32'hf0f0f0f0; // @[Bitwise.scala 105:80]
  wire [31:0] _shin_T_31 = _shin_T_26 | _shin_T_30; // @[Bitwise.scala 105:39]
  wire [31:0] _GEN_4 = {{2'd0}, _shin_T_31[31:2]}; // @[Bitwise.scala 105:31]
  wire [31:0] _shin_T_36 = _GEN_4 & 32'h33333333; // @[Bitwise.scala 105:31]
  wire [31:0] _shin_T_38 = {_shin_T_31[29:0], 2'h0}; // @[Bitwise.scala 105:70]
  wire [31:0] _shin_T_40 = _shin_T_38 & 32'hcccccccc; // @[Bitwise.scala 105:80]
  wire [31:0] _shin_T_41 = _shin_T_36 | _shin_T_40; // @[Bitwise.scala 105:39]
  wire [31:0] _GEN_5 = {{1'd0}, _shin_T_41[31:1]}; // @[Bitwise.scala 105:31]
  wire [31:0] _shin_T_46 = _GEN_5 & 32'h55555555; // @[Bitwise.scala 105:31]
  wire [31:0] _shin_T_48 = {_shin_T_41[30:0], 1'h0}; // @[Bitwise.scala 105:70]
  wire [31:0] _shin_T_50 = _shin_T_48 & 32'haaaaaaaa; // @[Bitwise.scala 105:80]
  wire [31:0] _shin_T_51 = _shin_T_46 | _shin_T_50; // @[Bitwise.scala 105:39]
  wire [31:0] shin = io_func == 5'h5 | io_func == 5'hb ? io_in1 : _shin_T_51; // @[ALU.scala 92:17]
  wire  _shout_r_T_4 = _in2_inv_T_2 & shin[31]; // @[ALU.scala 93:36]
  wire [32:0] _shout_r_T_6 = {_shout_r_T_4,shin}; // @[ALU.scala 93:57]
  wire [32:0] _shout_r_T_7 = $signed(_shout_r_T_6) >>> shamt; // @[ALU.scala 93:64]
  wire [31:0] shout_r = _shout_r_T_7[31:0]; // @[ALU.scala 93:73]
  wire [31:0] _GEN_6 = {{16'd0}, shout_r[31:16]}; // @[Bitwise.scala 105:31]
  wire [31:0] _shout_l_T_3 = _GEN_6 & 32'hffff; // @[Bitwise.scala 105:31]
  wire [31:0] _shout_l_T_5 = {shout_r[15:0], 16'h0}; // @[Bitwise.scala 105:70]
  wire [31:0] _shout_l_T_7 = _shout_l_T_5 & 32'hffff0000; // @[Bitwise.scala 105:80]
  wire [31:0] _shout_l_T_8 = _shout_l_T_3 | _shout_l_T_7; // @[Bitwise.scala 105:39]
  wire [31:0] _GEN_7 = {{8'd0}, _shout_l_T_8[31:8]}; // @[Bitwise.scala 105:31]
  wire [31:0] _shout_l_T_13 = _GEN_7 & 32'hff00ff; // @[Bitwise.scala 105:31]
  wire [31:0] _shout_l_T_15 = {_shout_l_T_8[23:0], 8'h0}; // @[Bitwise.scala 105:70]
  wire [31:0] _shout_l_T_17 = _shout_l_T_15 & 32'hff00ff00; // @[Bitwise.scala 105:80]
  wire [31:0] _shout_l_T_18 = _shout_l_T_13 | _shout_l_T_17; // @[Bitwise.scala 105:39]
  wire [31:0] _GEN_8 = {{4'd0}, _shout_l_T_18[31:4]}; // @[Bitwise.scala 105:31]
  wire [31:0] _shout_l_T_23 = _GEN_8 & 32'hf0f0f0f; // @[Bitwise.scala 105:31]
  wire [31:0] _shout_l_T_25 = {_shout_l_T_18[27:0], 4'h0}; // @[Bitwise.scala 105:70]
  wire [31:0] _shout_l_T_27 = _shout_l_T_25 & 32'hf0f0f0f0; // @[Bitwise.scala 105:80]
  wire [31:0] _shout_l_T_28 = _shout_l_T_23 | _shout_l_T_27; // @[Bitwise.scala 105:39]
  wire [31:0] _GEN_9 = {{2'd0}, _shout_l_T_28[31:2]}; // @[Bitwise.scala 105:31]
  wire [31:0] _shout_l_T_33 = _GEN_9 & 32'h33333333; // @[Bitwise.scala 105:31]
  wire [31:0] _shout_l_T_35 = {_shout_l_T_28[29:0], 2'h0}; // @[Bitwise.scala 105:70]
  wire [31:0] _shout_l_T_37 = _shout_l_T_35 & 32'hcccccccc; // @[Bitwise.scala 105:80]
  wire [31:0] _shout_l_T_38 = _shout_l_T_33 | _shout_l_T_37; // @[Bitwise.scala 105:39]
  wire [31:0] _GEN_10 = {{1'd0}, _shout_l_T_38[31:1]}; // @[Bitwise.scala 105:31]
  wire [31:0] _shout_l_T_43 = _GEN_10 & 32'h55555555; // @[Bitwise.scala 105:31]
  wire [31:0] _shout_l_T_45 = {_shout_l_T_38[30:0], 1'h0}; // @[Bitwise.scala 105:70]
  wire [31:0] _shout_l_T_47 = _shout_l_T_45 & 32'haaaaaaaa; // @[Bitwise.scala 105:80]
  wire [31:0] shout_l = _shout_l_T_43 | _shout_l_T_47; // @[Bitwise.scala 105:39]
  wire [31:0] _shout_T_3 = _shin_T_2 ? shout_r : 32'h0; // @[ALU.scala 95:18]
  wire [31:0] _shout_T_5 = io_func == 5'h1 ? shout_l : 32'h0; // @[ALU.scala 96:8]
  wire [31:0] shout = _shout_T_3 | _shout_T_5; // @[ALU.scala 95:82]
  wire [31:0] _logic_T_1 = io_in1 ^ io_in2; // @[ALU.scala 99:46]
  wire [31:0] _logic_T_3 = io_in1 | io_in2; // @[ALU.scala 100:35]
  wire [31:0] _logic_T_5 = io_in1 & io_in2; // @[ALU.scala 101:38]
  wire [31:0] _logic_T_6 = io_func == 5'h7 ? _logic_T_5 : 32'h0; // @[ALU.scala 101:10]
  wire [31:0] _logic_T_7 = io_func == 5'h6 ? _logic_T_3 : _logic_T_6; // @[ALU.scala 100:8]
  wire [31:0] logic_ = io_func == 5'h4 ? _logic_T_1 : _logic_T_7; // @[ALU.scala 99:18]
  wire  _shift_logic_cmp_T_2 = io_func >= 5'hc & _in2_inv_T_1; // @[ALU.scala 57:42]
  wire  _shift_logic_cmp_T_3 = _shift_logic_cmp_T_2 & slt; // @[ALU.scala 103:40]
  wire [31:0] _GEN_11 = {{31'd0}, _shift_logic_cmp_T_3}; // @[ALU.scala 103:47]
  wire [31:0] _shift_logic_cmp_T_4 = _GEN_11 | logic_; // @[ALU.scala 103:47]
  wire [31:0] shift_logic_cmp = _shift_logic_cmp_T_4 | shout; // @[ALU.scala 103:55]
  wire [31:0] out = io_func == 5'h0 | io_func == 5'ha ? adder_out : shift_logic_cmp; // @[ALU.scala 104:16]
  wire  _minu_T = io_in1 > io_in2; // @[ALU.scala 107:22]
  wire [31:0] minu = io_in1 > io_in2 ? io_in2 : io_in1; // @[ALU.scala 107:15]
  wire [31:0] maxu = _minu_T ? io_in1 : io_in2; // @[ALU.scala 108:15]
  wire  _mins_T = $signed(io_in1) > $signed(io_in2); // @[ALU.scala 111:20]
  wire [31:0] mins = $signed(io_in1) > $signed(io_in2) ? $signed(io_in2) : $signed(io_in1); // @[ALU.scala 111:37]
  wire [31:0] maxs = _mins_T ? $signed(io_in1) : $signed(io_in2); // @[ALU.scala 112:37]
  wire [31:0] _minmaxout_T_3 = io_func == 5'h13 ? minu : maxu; // @[ALU.scala 115:22]
  wire [31:0] _minmaxout_T_4 = io_func == 5'h10 ? maxs : _minmaxout_T_3; // @[ALU.scala 114:22]
  wire [31:0] minmaxout = io_func == 5'h11 ? mins : _minmaxout_T_4; // @[ALU.scala 113:22]
  wire  _io_out_T_2 = io_func[4:2] == 3'h4; // @[ALU.scala 61:32]
  wire [31:0] _io_out_T_3 = _io_out_T_2 ? minmaxout : out; // @[ALU.scala 118:16]
  assign io_out = io_func == 5'h8 ? io_in2 : _io_out_T_3; // @[ALU.scala 117:16]
  assign io_cmp_out = io_func[0] ^ _io_cmp_out_T_4; // @[ALU.scala 88:38]
endmodule
module Queue(
  input         clock,
  input         reset,
  output        io_enq_ready,
  input         io_enq_valid,
  input  [31:0] io_enq_bits_wb_wxd_rd,
  input         io_enq_bits_wxd,
  input  [4:0]  io_enq_bits_reg_idxw,
  input  [1:0]  io_enq_bits_warp_id,
  input         io_deq_ready,
  output        io_deq_valid,
  output [31:0] io_deq_bits_wb_wxd_rd,
  output        io_deq_bits_wxd,
  output [4:0]  io_deq_bits_reg_idxw,
  output [1:0]  io_deq_bits_warp_id
);
`ifdef RANDOMIZE_MEM_INIT
  reg [31:0] _RAND_0;
  reg [31:0] _RAND_1;
  reg [31:0] _RAND_2;
  reg [31:0] _RAND_3;
`endif // RANDOMIZE_MEM_INIT
`ifdef RANDOMIZE_REG_INIT
  reg [31:0] _RAND_4;
`endif // RANDOMIZE_REG_INIT
  reg [31:0] ram_wb_wxd_rd [0:0]; // @[Decoupled.scala 259:95]
  wire  ram_wb_wxd_rd_io_deq_bits_MPORT_en; // @[Decoupled.scala 259:95]
  wire  ram_wb_wxd_rd_io_deq_bits_MPORT_addr; // @[Decoupled.scala 259:95]
  wire [31:0] ram_wb_wxd_rd_io_deq_bits_MPORT_data; // @[Decoupled.scala 259:95]
  wire [31:0] ram_wb_wxd_rd_MPORT_data; // @[Decoupled.scala 259:95]
  wire  ram_wb_wxd_rd_MPORT_addr; // @[Decoupled.scala 259:95]
  wire  ram_wb_wxd_rd_MPORT_mask; // @[Decoupled.scala 259:95]
  wire  ram_wb_wxd_rd_MPORT_en; // @[Decoupled.scala 259:95]
  reg  ram_wxd [0:0]; // @[Decoupled.scala 259:95]
  wire  ram_wxd_io_deq_bits_MPORT_en; // @[Decoupled.scala 259:95]
  wire  ram_wxd_io_deq_bits_MPORT_addr; // @[Decoupled.scala 259:95]
  wire  ram_wxd_io_deq_bits_MPORT_data; // @[Decoupled.scala 259:95]
  wire  ram_wxd_MPORT_data; // @[Decoupled.scala 259:95]
  wire  ram_wxd_MPORT_addr; // @[Decoupled.scala 259:95]
  wire  ram_wxd_MPORT_mask; // @[Decoupled.scala 259:95]
  wire  ram_wxd_MPORT_en; // @[Decoupled.scala 259:95]
  reg [4:0] ram_reg_idxw [0:0]; // @[Decoupled.scala 259:95]
  wire  ram_reg_idxw_io_deq_bits_MPORT_en; // @[Decoupled.scala 259:95]
  wire  ram_reg_idxw_io_deq_bits_MPORT_addr; // @[Decoupled.scala 259:95]
  wire [4:0] ram_reg_idxw_io_deq_bits_MPORT_data; // @[Decoupled.scala 259:95]
  wire [4:0] ram_reg_idxw_MPORT_data; // @[Decoupled.scala 259:95]
  wire  ram_reg_idxw_MPORT_addr; // @[Decoupled.scala 259:95]
  wire  ram_reg_idxw_MPORT_mask; // @[Decoupled.scala 259:95]
  wire  ram_reg_idxw_MPORT_en; // @[Decoupled.scala 259:95]
  reg [1:0] ram_warp_id [0:0]; // @[Decoupled.scala 259:95]
  wire  ram_warp_id_io_deq_bits_MPORT_en; // @[Decoupled.scala 259:95]
  wire  ram_warp_id_io_deq_bits_MPORT_addr; // @[Decoupled.scala 259:95]
  wire [1:0] ram_warp_id_io_deq_bits_MPORT_data; // @[Decoupled.scala 259:95]
  wire [1:0] ram_warp_id_MPORT_data; // @[Decoupled.scala 259:95]
  wire  ram_warp_id_MPORT_addr; // @[Decoupled.scala 259:95]
  wire  ram_warp_id_MPORT_mask; // @[Decoupled.scala 259:95]
  wire  ram_warp_id_MPORT_en; // @[Decoupled.scala 259:95]
  reg  maybe_full; // @[Decoupled.scala 262:27]
  wire  empty = ~maybe_full; // @[Decoupled.scala 264:28]
  wire  do_enq = io_enq_ready & io_enq_valid; // @[Decoupled.scala 50:35]
  wire  do_deq = io_deq_ready & io_deq_valid; // @[Decoupled.scala 50:35]
  assign ram_wb_wxd_rd_io_deq_bits_MPORT_en = 1'h1;
  assign ram_wb_wxd_rd_io_deq_bits_MPORT_addr = 1'h0;
  assign ram_wb_wxd_rd_io_deq_bits_MPORT_data = ram_wb_wxd_rd[ram_wb_wxd_rd_io_deq_bits_MPORT_addr]; // @[Decoupled.scala 259:95]
  assign ram_wb_wxd_rd_MPORT_data = io_enq_bits_wb_wxd_rd;
  assign ram_wb_wxd_rd_MPORT_addr = 1'h0;
  assign ram_wb_wxd_rd_MPORT_mask = 1'h1;
  assign ram_wb_wxd_rd_MPORT_en = io_enq_ready & io_enq_valid;
  assign ram_wxd_io_deq_bits_MPORT_en = 1'h1;
  assign ram_wxd_io_deq_bits_MPORT_addr = 1'h0;
  assign ram_wxd_io_deq_bits_MPORT_data = ram_wxd[ram_wxd_io_deq_bits_MPORT_addr]; // @[Decoupled.scala 259:95]
  assign ram_wxd_MPORT_data = io_enq_bits_wxd;
  assign ram_wxd_MPORT_addr = 1'h0;
  assign ram_wxd_MPORT_mask = 1'h1;
  assign ram_wxd_MPORT_en = io_enq_ready & io_enq_valid;
  assign ram_reg_idxw_io_deq_bits_MPORT_en = 1'h1;
  assign ram_reg_idxw_io_deq_bits_MPORT_addr = 1'h0;
  assign ram_reg_idxw_io_deq_bits_MPORT_data = ram_reg_idxw[ram_reg_idxw_io_deq_bits_MPORT_addr]; // @[Decoupled.scala 259:95]
  assign ram_reg_idxw_MPORT_data = io_enq_bits_reg_idxw;
  assign ram_reg_idxw_MPORT_addr = 1'h0;
  assign ram_reg_idxw_MPORT_mask = 1'h1;
  assign ram_reg_idxw_MPORT_en = io_enq_ready & io_enq_valid;
  assign ram_warp_id_io_deq_bits_MPORT_en = 1'h1;
  assign ram_warp_id_io_deq_bits_MPORT_addr = 1'h0;
  assign ram_warp_id_io_deq_bits_MPORT_data = ram_warp_id[ram_warp_id_io_deq_bits_MPORT_addr]; // @[Decoupled.scala 259:95]
  assign ram_warp_id_MPORT_data = io_enq_bits_warp_id;
  assign ram_warp_id_MPORT_addr = 1'h0;
  assign ram_warp_id_MPORT_mask = 1'h1;
  assign ram_warp_id_MPORT_en = io_enq_ready & io_enq_valid;
  assign io_enq_ready = io_deq_ready | empty; // @[Decoupled.scala 289:16 309:{24,39}]
  assign io_deq_valid = ~empty; // @[Decoupled.scala 288:19]
  assign io_deq_bits_wb_wxd_rd = ram_wb_wxd_rd_io_deq_bits_MPORT_data; // @[Decoupled.scala 296:17]
  assign io_deq_bits_wxd = ram_wxd_io_deq_bits_MPORT_data; // @[Decoupled.scala 296:17]
  assign io_deq_bits_reg_idxw = ram_reg_idxw_io_deq_bits_MPORT_data; // @[Decoupled.scala 296:17]
  assign io_deq_bits_warp_id = ram_warp_id_io_deq_bits_MPORT_data; // @[Decoupled.scala 296:17]
  always @(posedge clock) begin
    if (ram_wb_wxd_rd_MPORT_en & ram_wb_wxd_rd_MPORT_mask) begin
      ram_wb_wxd_rd[ram_wb_wxd_rd_MPORT_addr] <= ram_wb_wxd_rd_MPORT_data; // @[Decoupled.scala 259:95]
    end
    if (ram_wxd_MPORT_en & ram_wxd_MPORT_mask) begin
      ram_wxd[ram_wxd_MPORT_addr] <= ram_wxd_MPORT_data; // @[Decoupled.scala 259:95]
    end
    if (ram_reg_idxw_MPORT_en & ram_reg_idxw_MPORT_mask) begin
      ram_reg_idxw[ram_reg_idxw_MPORT_addr] <= ram_reg_idxw_MPORT_data; // @[Decoupled.scala 259:95]
    end
    if (ram_warp_id_MPORT_en & ram_warp_id_MPORT_mask) begin
      ram_warp_id[ram_warp_id_MPORT_addr] <= ram_warp_id_MPORT_data; // @[Decoupled.scala 259:95]
    end
    if (reset) begin // @[Decoupled.scala 262:27]
      maybe_full <= 1'h0; // @[Decoupled.scala 262:27]
    end else if (do_enq != do_deq) begin // @[Decoupled.scala 279:27]
      maybe_full <= do_enq; // @[Decoupled.scala 280:16]
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
  for (initvar = 0; initvar < 1; initvar = initvar+1)
    ram_wb_wxd_rd[initvar] = _RAND_0[31:0];
  _RAND_1 = {1{`RANDOM}};
  for (initvar = 0; initvar < 1; initvar = initvar+1)
    ram_wxd[initvar] = _RAND_1[0:0];
  _RAND_2 = {1{`RANDOM}};
  for (initvar = 0; initvar < 1; initvar = initvar+1)
    ram_reg_idxw[initvar] = _RAND_2[4:0];
  _RAND_3 = {1{`RANDOM}};
  for (initvar = 0; initvar < 1; initvar = initvar+1)
    ram_warp_id[initvar] = _RAND_3[1:0];
`endif // RANDOMIZE_MEM_INIT
`ifdef RANDOMIZE_REG_INIT
  _RAND_4 = {1{`RANDOM}};
  maybe_full = _RAND_4[0:0];
`endif // RANDOMIZE_REG_INIT
  `endif // RANDOMIZE
end // initial
`ifdef FIRRTL_AFTER_INITIAL
`FIRRTL_AFTER_INITIAL
`endif
`endif // SYNTHESIS
endmodule
module Queue_1(
  input         clock,
  input         reset,
  output        io_enq_ready,
  input         io_enq_valid,
  input  [1:0]  io_enq_bits_wid,
  input         io_enq_bits_jump,
  input  [31:0] io_enq_bits_new_pc,
  input         io_deq_ready,
  output        io_deq_valid,
  output [1:0]  io_deq_bits_wid,
  output        io_deq_bits_jump,
  output [31:0] io_deq_bits_new_pc
);
`ifdef RANDOMIZE_MEM_INIT
  reg [31:0] _RAND_0;
  reg [31:0] _RAND_1;
  reg [31:0] _RAND_2;
`endif // RANDOMIZE_MEM_INIT
`ifdef RANDOMIZE_REG_INIT
  reg [31:0] _RAND_3;
`endif // RANDOMIZE_REG_INIT
  reg [1:0] ram_wid [0:0]; // @[Decoupled.scala 259:95]
  wire  ram_wid_io_deq_bits_MPORT_en; // @[Decoupled.scala 259:95]
  wire  ram_wid_io_deq_bits_MPORT_addr; // @[Decoupled.scala 259:95]
  wire [1:0] ram_wid_io_deq_bits_MPORT_data; // @[Decoupled.scala 259:95]
  wire [1:0] ram_wid_MPORT_data; // @[Decoupled.scala 259:95]
  wire  ram_wid_MPORT_addr; // @[Decoupled.scala 259:95]
  wire  ram_wid_MPORT_mask; // @[Decoupled.scala 259:95]
  wire  ram_wid_MPORT_en; // @[Decoupled.scala 259:95]
  reg  ram_jump [0:0]; // @[Decoupled.scala 259:95]
  wire  ram_jump_io_deq_bits_MPORT_en; // @[Decoupled.scala 259:95]
  wire  ram_jump_io_deq_bits_MPORT_addr; // @[Decoupled.scala 259:95]
  wire  ram_jump_io_deq_bits_MPORT_data; // @[Decoupled.scala 259:95]
  wire  ram_jump_MPORT_data; // @[Decoupled.scala 259:95]
  wire  ram_jump_MPORT_addr; // @[Decoupled.scala 259:95]
  wire  ram_jump_MPORT_mask; // @[Decoupled.scala 259:95]
  wire  ram_jump_MPORT_en; // @[Decoupled.scala 259:95]
  reg [31:0] ram_new_pc [0:0]; // @[Decoupled.scala 259:95]
  wire  ram_new_pc_io_deq_bits_MPORT_en; // @[Decoupled.scala 259:95]
  wire  ram_new_pc_io_deq_bits_MPORT_addr; // @[Decoupled.scala 259:95]
  wire [31:0] ram_new_pc_io_deq_bits_MPORT_data; // @[Decoupled.scala 259:95]
  wire [31:0] ram_new_pc_MPORT_data; // @[Decoupled.scala 259:95]
  wire  ram_new_pc_MPORT_addr; // @[Decoupled.scala 259:95]
  wire  ram_new_pc_MPORT_mask; // @[Decoupled.scala 259:95]
  wire  ram_new_pc_MPORT_en; // @[Decoupled.scala 259:95]
  reg  maybe_full; // @[Decoupled.scala 262:27]
  wire  empty = ~maybe_full; // @[Decoupled.scala 264:28]
  wire  do_enq = io_enq_ready & io_enq_valid; // @[Decoupled.scala 50:35]
  wire  do_deq = io_deq_ready & io_deq_valid; // @[Decoupled.scala 50:35]
  assign ram_wid_io_deq_bits_MPORT_en = 1'h1;
  assign ram_wid_io_deq_bits_MPORT_addr = 1'h0;
  assign ram_wid_io_deq_bits_MPORT_data = ram_wid[ram_wid_io_deq_bits_MPORT_addr]; // @[Decoupled.scala 259:95]
  assign ram_wid_MPORT_data = io_enq_bits_wid;
  assign ram_wid_MPORT_addr = 1'h0;
  assign ram_wid_MPORT_mask = 1'h1;
  assign ram_wid_MPORT_en = io_enq_ready & io_enq_valid;
  assign ram_jump_io_deq_bits_MPORT_en = 1'h1;
  assign ram_jump_io_deq_bits_MPORT_addr = 1'h0;
  assign ram_jump_io_deq_bits_MPORT_data = ram_jump[ram_jump_io_deq_bits_MPORT_addr]; // @[Decoupled.scala 259:95]
  assign ram_jump_MPORT_data = io_enq_bits_jump;
  assign ram_jump_MPORT_addr = 1'h0;
  assign ram_jump_MPORT_mask = 1'h1;
  assign ram_jump_MPORT_en = io_enq_ready & io_enq_valid;
  assign ram_new_pc_io_deq_bits_MPORT_en = 1'h1;
  assign ram_new_pc_io_deq_bits_MPORT_addr = 1'h0;
  assign ram_new_pc_io_deq_bits_MPORT_data = ram_new_pc[ram_new_pc_io_deq_bits_MPORT_addr]; // @[Decoupled.scala 259:95]
  assign ram_new_pc_MPORT_data = io_enq_bits_new_pc;
  assign ram_new_pc_MPORT_addr = 1'h0;
  assign ram_new_pc_MPORT_mask = 1'h1;
  assign ram_new_pc_MPORT_en = io_enq_ready & io_enq_valid;
  assign io_enq_ready = io_deq_ready | empty; // @[Decoupled.scala 289:16 309:{24,39}]
  assign io_deq_valid = ~empty; // @[Decoupled.scala 288:19]
  assign io_deq_bits_wid = ram_wid_io_deq_bits_MPORT_data; // @[Decoupled.scala 296:17]
  assign io_deq_bits_jump = ram_jump_io_deq_bits_MPORT_data; // @[Decoupled.scala 296:17]
  assign io_deq_bits_new_pc = ram_new_pc_io_deq_bits_MPORT_data; // @[Decoupled.scala 296:17]
  always @(posedge clock) begin
    if (ram_wid_MPORT_en & ram_wid_MPORT_mask) begin
      ram_wid[ram_wid_MPORT_addr] <= ram_wid_MPORT_data; // @[Decoupled.scala 259:95]
    end
    if (ram_jump_MPORT_en & ram_jump_MPORT_mask) begin
      ram_jump[ram_jump_MPORT_addr] <= ram_jump_MPORT_data; // @[Decoupled.scala 259:95]
    end
    if (ram_new_pc_MPORT_en & ram_new_pc_MPORT_mask) begin
      ram_new_pc[ram_new_pc_MPORT_addr] <= ram_new_pc_MPORT_data; // @[Decoupled.scala 259:95]
    end
    if (reset) begin // @[Decoupled.scala 262:27]
      maybe_full <= 1'h0; // @[Decoupled.scala 262:27]
    end else if (do_enq != do_deq) begin // @[Decoupled.scala 279:27]
      maybe_full <= do_enq; // @[Decoupled.scala 280:16]
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
  for (initvar = 0; initvar < 1; initvar = initvar+1)
    ram_wid[initvar] = _RAND_0[1:0];
  _RAND_1 = {1{`RANDOM}};
  for (initvar = 0; initvar < 1; initvar = initvar+1)
    ram_jump[initvar] = _RAND_1[0:0];
  _RAND_2 = {1{`RANDOM}};
  for (initvar = 0; initvar < 1; initvar = initvar+1)
    ram_new_pc[initvar] = _RAND_2[31:0];
`endif // RANDOMIZE_MEM_INIT
`ifdef RANDOMIZE_REG_INIT
  _RAND_3 = {1{`RANDOM}};
  maybe_full = _RAND_3[0:0];
`endif // RANDOMIZE_REG_INIT
  `endif // RANDOMIZE
end // initial
`ifdef FIRRTL_AFTER_INITIAL
`FIRRTL_AFTER_INITIAL
`endif
`endif // SYNTHESIS
endmodule
module ALUexe(
  input         clock,
  input         reset,
  output        io_in_ready,
  input         io_in_valid,
  input  [31:0] io_in_bits_in1,
  input  [31:0] io_in_bits_in2,
  input  [31:0] io_in_bits_in3,
  input  [31:0] io_in_bits_ctrl_inst,
  input  [1:0]  io_in_bits_ctrl_wid,
  input         io_in_bits_ctrl_fp,
  input  [1:0]  io_in_bits_ctrl_branch,
  input         io_in_bits_ctrl_simt_stack,
  input         io_in_bits_ctrl_simt_stack_op,
  input         io_in_bits_ctrl_barrier,
  input  [1:0]  io_in_bits_ctrl_csr,
  input         io_in_bits_ctrl_reverse,
  input  [1:0]  io_in_bits_ctrl_sel_alu2,
  input  [1:0]  io_in_bits_ctrl_sel_alu1,
  input         io_in_bits_ctrl_isvec,
  input  [1:0]  io_in_bits_ctrl_sel_alu3,
  input         io_in_bits_ctrl_mask,
  input  [2:0]  io_in_bits_ctrl_sel_imm,
  input  [1:0]  io_in_bits_ctrl_mem_whb,
  input         io_in_bits_ctrl_mem_unsigned,
  input  [5:0]  io_in_bits_ctrl_alu_fn,
  input         io_in_bits_ctrl_mem,
  input         io_in_bits_ctrl_mul,
  input  [1:0]  io_in_bits_ctrl_mem_cmd,
  input  [1:0]  io_in_bits_ctrl_mop,
  input  [4:0]  io_in_bits_ctrl_reg_idx1,
  input  [4:0]  io_in_bits_ctrl_reg_idx2,
  input  [4:0]  io_in_bits_ctrl_reg_idx3,
  input  [4:0]  io_in_bits_ctrl_reg_idxw,
  input         io_in_bits_ctrl_wfd,
  input         io_in_bits_ctrl_fence,
  input         io_in_bits_ctrl_sfu,
  input         io_in_bits_ctrl_readmask,
  input         io_in_bits_ctrl_writemask,
  input         io_in_bits_ctrl_wxd,
  input  [31:0] io_in_bits_ctrl_pc,
  input         io_out_ready,
  output        io_out_valid,
  output [31:0] io_out_bits_wb_wxd_rd,
  output        io_out_bits_wxd,
  output [4:0]  io_out_bits_reg_idxw,
  output [1:0]  io_out_bits_warp_id,
  input         io_out2br_ready,
  output        io_out2br_valid,
  output [1:0]  io_out2br_bits_wid,
  output        io_out2br_bits_jump,
  output [31:0] io_out2br_bits_new_pc
);
  wire [4:0] alu_io_func; // @[execution.scala 29:17]
  wire [31:0] alu_io_in2; // @[execution.scala 29:17]
  wire [31:0] alu_io_in1; // @[execution.scala 29:17]
  wire [31:0] alu_io_out; // @[execution.scala 29:17]
  wire  alu_io_cmp_out; // @[execution.scala 29:17]
  wire  result_clock; // @[execution.scala 34:20]
  wire  result_reset; // @[execution.scala 34:20]
  wire  result_io_enq_ready; // @[execution.scala 34:20]
  wire  result_io_enq_valid; // @[execution.scala 34:20]
  wire [31:0] result_io_enq_bits_wb_wxd_rd; // @[execution.scala 34:20]
  wire  result_io_enq_bits_wxd; // @[execution.scala 34:20]
  wire [4:0] result_io_enq_bits_reg_idxw; // @[execution.scala 34:20]
  wire [1:0] result_io_enq_bits_warp_id; // @[execution.scala 34:20]
  wire  result_io_deq_ready; // @[execution.scala 34:20]
  wire  result_io_deq_valid; // @[execution.scala 34:20]
  wire [31:0] result_io_deq_bits_wb_wxd_rd; // @[execution.scala 34:20]
  wire  result_io_deq_bits_wxd; // @[execution.scala 34:20]
  wire [4:0] result_io_deq_bits_reg_idxw; // @[execution.scala 34:20]
  wire [1:0] result_io_deq_bits_warp_id; // @[execution.scala 34:20]
  wire  result_br_clock; // @[execution.scala 35:23]
  wire  result_br_reset; // @[execution.scala 35:23]
  wire  result_br_io_enq_ready; // @[execution.scala 35:23]
  wire  result_br_io_enq_valid; // @[execution.scala 35:23]
  wire [1:0] result_br_io_enq_bits_wid; // @[execution.scala 35:23]
  wire  result_br_io_enq_bits_jump; // @[execution.scala 35:23]
  wire [31:0] result_br_io_enq_bits_new_pc; // @[execution.scala 35:23]
  wire  result_br_io_deq_ready; // @[execution.scala 35:23]
  wire  result_br_io_deq_valid; // @[execution.scala 35:23]
  wire [1:0] result_br_io_deq_bits_wid; // @[execution.scala 35:23]
  wire  result_br_io_deq_bits_jump; // @[execution.scala 35:23]
  wire [31:0] result_br_io_deq_bits_new_pc; // @[execution.scala 35:23]
  wire  _io_in_ready_T = result_br_io_enq_ready & result_io_enq_ready; // @[execution.scala 44:71]
  wire  _io_in_ready_T_2 = 2'h1 == io_in_bits_ctrl_branch ? result_br_io_enq_ready : _io_in_ready_T; // @[Mux.scala 81:58]
  ScalarALU alu ( // @[execution.scala 29:17]
    .io_func(alu_io_func),
    .io_in2(alu_io_in2),
    .io_in1(alu_io_in1),
    .io_out(alu_io_out),
    .io_cmp_out(alu_io_cmp_out)
  );
  Queue result ( // @[execution.scala 34:20]
    .clock(result_clock),
    .reset(result_reset),
    .io_enq_ready(result_io_enq_ready),
    .io_enq_valid(result_io_enq_valid),
    .io_enq_bits_wb_wxd_rd(result_io_enq_bits_wb_wxd_rd),
    .io_enq_bits_wxd(result_io_enq_bits_wxd),
    .io_enq_bits_reg_idxw(result_io_enq_bits_reg_idxw),
    .io_enq_bits_warp_id(result_io_enq_bits_warp_id),
    .io_deq_ready(result_io_deq_ready),
    .io_deq_valid(result_io_deq_valid),
    .io_deq_bits_wb_wxd_rd(result_io_deq_bits_wb_wxd_rd),
    .io_deq_bits_wxd(result_io_deq_bits_wxd),
    .io_deq_bits_reg_idxw(result_io_deq_bits_reg_idxw),
    .io_deq_bits_warp_id(result_io_deq_bits_warp_id)
  );
  Queue_1 result_br ( // @[execution.scala 35:23]
    .clock(result_br_clock),
    .reset(result_br_reset),
    .io_enq_ready(result_br_io_enq_ready),
    .io_enq_valid(result_br_io_enq_valid),
    .io_enq_bits_wid(result_br_io_enq_bits_wid),
    .io_enq_bits_jump(result_br_io_enq_bits_jump),
    .io_enq_bits_new_pc(result_br_io_enq_bits_new_pc),
    .io_deq_ready(result_br_io_deq_ready),
    .io_deq_valid(result_br_io_deq_valid),
    .io_deq_bits_wid(result_br_io_deq_bits_wid),
    .io_deq_bits_jump(result_br_io_deq_bits_jump),
    .io_deq_bits_new_pc(result_br_io_deq_bits_new_pc)
  );
  assign io_in_ready = 2'h0 == io_in_bits_ctrl_branch ? result_io_enq_ready : _io_in_ready_T_2; // @[Mux.scala 81:58]
  assign io_out_valid = result_io_deq_valid; // @[execution.scala 36:16]
  assign io_out_bits_wb_wxd_rd = result_io_deq_bits_wb_wxd_rd; // @[execution.scala 36:16]
  assign io_out_bits_wxd = result_io_deq_bits_wxd; // @[execution.scala 36:16]
  assign io_out_bits_reg_idxw = result_io_deq_bits_reg_idxw; // @[execution.scala 36:16]
  assign io_out_bits_warp_id = result_io_deq_bits_warp_id; // @[execution.scala 36:16]
  assign io_out2br_valid = result_br_io_deq_valid; // @[execution.scala 37:19]
  assign io_out2br_bits_wid = result_br_io_deq_bits_wid; // @[execution.scala 37:19]
  assign io_out2br_bits_jump = result_br_io_deq_bits_jump; // @[execution.scala 37:19]
  assign io_out2br_bits_new_pc = result_br_io_deq_bits_new_pc; // @[execution.scala 37:19]
  assign alu_io_func = io_in_bits_ctrl_alu_fn[4:0]; // @[execution.scala 33:38]
  assign alu_io_in2 = io_in_bits_in2; // @[execution.scala 31:13]
  assign alu_io_in1 = io_in_bits_in1; // @[execution.scala 30:13]
  assign result_clock = clock;
  assign result_reset = reset;
  assign result_io_enq_valid = io_in_valid & io_in_bits_ctrl_wxd; // @[execution.scala 51:35]
  assign result_io_enq_bits_wb_wxd_rd = alu_io_out; // @[execution.scala 40:31]
  assign result_io_enq_bits_wxd = io_in_bits_ctrl_wxd; // @[execution.scala 42:25]
  assign result_io_enq_bits_reg_idxw = io_in_bits_ctrl_reg_idxw; // @[execution.scala 41:30]
  assign result_io_enq_bits_warp_id = io_in_bits_ctrl_wid; // @[execution.scala 39:29]
  assign result_io_deq_ready = io_out_ready; // @[execution.scala 36:16]
  assign result_br_clock = clock;
  assign result_br_reset = reset;
  assign result_br_io_enq_valid = io_in_valid & io_in_bits_ctrl_branch != 2'h0; // @[execution.scala 50:38]
  assign result_br_io_enq_bits_wid = io_in_bits_ctrl_wid; // @[execution.scala 46:28]
  assign result_br_io_enq_bits_jump = 2'h3 == io_in_bits_ctrl_branch | (2'h2 == io_in_bits_ctrl_branch | 2'h1 ==
    io_in_bits_ctrl_branch & alu_io_cmp_out); // @[Mux.scala 81:58]
  assign result_br_io_enq_bits_new_pc = io_in_bits_in3; // @[execution.scala 47:31]
  assign result_br_io_deq_ready = io_out2br_ready; // @[execution.scala 37:19]
endmodule
