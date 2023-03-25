module GPGPU_axi_adapter_top(
  input         clock,
  input         reset,
  input  [31:0] s_axi_lite_awaddr,
  input  [2:0]  s_axi_lite_awprot,
  input         s_axi_lite_awvalid,
  output        s_axi_lite_awready,
  input  [11:0] s_axi_lite_awid,
  input  [31:0] s_axi_lite_wdata,
  input  [3:0]  s_axi_lite_wstrb,
  input         s_axi_lite_wvalid,
  output        s_axi_lite_wready,
  input  [11:0] s_axi_lite_wid,
  output [1:0]  s_axi_lite_bresp,
  output        s_axi_lite_bvalid,
  input         s_axi_lite_bready,
  output [11:0] s_axi_lite_bid,
  input  [31:0] s_axi_lite_araddr,
  input  [2:0]  s_axi_lite_arprot,
  input         s_axi_lite_arvalid,
  output        s_axi_lite_arready,
  input  [11:0] s_axi_lite_arid,
  output [31:0] s_axi_lite_rdata,
  output [1:0]  s_axi_lite_rresp,
  output        s_axi_lite_rvalid,
  input         s_axi_lite_rready,
  output [11:0] s_axi_lite_rid,
  input         m_axi_awready,
  output        m_axi_awvalid,
  output [3:0]  m_axi_awid,
  output [31:0] m_axi_awaddr,
  output [7:0]  m_axi_awlen,
  output [2:0]  m_axi_awsize,
  output [1:0]  m_axi_awburst,
  output        m_axi_awlock,
  output [3:0]  m_axi_awcache,
  output [2:0]  m_axi_awprot,
  output [3:0]  m_axi_awqos,
  input         m_axi_wready,
  output        m_axi_wvalid,
  output [63:0] m_axi_wdata,
  output [7:0]  m_axi_wstrb,
  output        m_axi_wlast,
  output        m_axi_bready,
  input         m_axi_bvalid,
  input  [3:0]  m_axi_bid,
  input  [1:0]  m_axi_bresp,
  input         m_axi_arready,
  output        m_axi_arvalid,
  output [3:0]  m_axi_arid,
  output [31:0] m_axi_araddr,
  output [7:0]  m_axi_arlen,
  output [2:0]  m_axi_arsize,
  output [1:0]  m_axi_arburst,
  output        m_axi_arlock,
  output [3:0]  m_axi_arcache,
  output [2:0]  m_axi_arprot,
  output [3:0]  m_axi_arqos,
  output        m_axi_rready,
  input         m_axi_rvalid,
  input  [3:0]  m_axi_rid,
  input  [63:0] m_axi_rdata,
  input  [1:0]  m_axi_rresp,
  input         m_axi_rlast
);
  wire  gpgpu_axi_top_clock; // @[GPGPU_top.scala 119:27]
  wire  gpgpu_axi_top_resetn; // @[GPGPU_top.scala 119:27]
  wire [31:0] gpgpu_axi_top_io_s_aw_awaddr; // @[GPGPU_top.scala 119:27]
  wire  gpgpu_axi_top_io_s_aw_awvalid; // @[GPGPU_top.scala 119:27]
  wire  gpgpu_axi_top_io_s_aw_awready; // @[GPGPU_top.scala 119:27]
  wire [11:0] gpgpu_axi_top_io_s_aw_awid; // @[GPGPU_top.scala 119:27]
  wire [31:0] gpgpu_axi_top_io_s_w_wdata; // @[GPGPU_top.scala 119:27]
  wire  gpgpu_axi_top_io_s_w_wvalid; // @[GPGPU_top.scala 119:27]
  wire  gpgpu_axi_top_io_s_w_wready; // @[GPGPU_top.scala 119:27]
  wire  gpgpu_axi_top_io_s_b_bvalid; // @[GPGPU_top.scala 119:27]
  wire  gpgpu_axi_top_io_s_b_bready; // @[GPGPU_top.scala 119:27]
  wire [11:0] gpgpu_axi_top_io_s_b_bid; // @[GPGPU_top.scala 119:27]
  wire [31:0] gpgpu_axi_top_io_s_ar_araddr; // @[GPGPU_top.scala 119:27]
  wire  gpgpu_axi_top_io_s_ar_arvalid; // @[GPGPU_top.scala 119:27]
  wire  gpgpu_axi_top_io_s_ar_arready; // @[GPGPU_top.scala 119:27]
  wire [11:0] gpgpu_axi_top_io_s_ar_arid; // @[GPGPU_top.scala 119:27]
  wire [31:0] gpgpu_axi_top_io_s_r_rdata; // @[GPGPU_top.scala 119:27]
  wire  gpgpu_axi_top_io_s_r_rvalid; // @[GPGPU_top.scala 119:27]
  wire  gpgpu_axi_top_io_s_r_rready; // @[GPGPU_top.scala 119:27]
  wire [11:0] gpgpu_axi_top_io_s_r_rid; // @[GPGPU_top.scala 119:27]
  wire  gpgpu_axi_top_io_m_aw_ready; // @[GPGPU_top.scala 119:27]
  wire  gpgpu_axi_top_io_m_aw_valid; // @[GPGPU_top.scala 119:27]
  wire [3:0] gpgpu_axi_top_io_m_aw_bits_id; // @[GPGPU_top.scala 119:27]
  wire [31:0] gpgpu_axi_top_io_m_aw_bits_addr; // @[GPGPU_top.scala 119:27]
  wire  gpgpu_axi_top_io_m_w_ready; // @[GPGPU_top.scala 119:27]
  wire  gpgpu_axi_top_io_m_w_valid; // @[GPGPU_top.scala 119:27]
  wire [63:0] gpgpu_axi_top_io_m_w_bits_data; // @[GPGPU_top.scala 119:27]
  wire [7:0] gpgpu_axi_top_io_m_w_bits_strb; // @[GPGPU_top.scala 119:27]
  wire  gpgpu_axi_top_io_m_w_bits_last; // @[GPGPU_top.scala 119:27]
  wire  gpgpu_axi_top_io_m_b_valid; // @[GPGPU_top.scala 119:27]
  wire [3:0] gpgpu_axi_top_io_m_b_bits_id; // @[GPGPU_top.scala 119:27]
  wire  gpgpu_axi_top_io_m_ar_ready; // @[GPGPU_top.scala 119:27]
  wire  gpgpu_axi_top_io_m_ar_valid; // @[GPGPU_top.scala 119:27]
  wire [3:0] gpgpu_axi_top_io_m_ar_bits_id; // @[GPGPU_top.scala 119:27]
  wire [31:0] gpgpu_axi_top_io_m_ar_bits_addr; // @[GPGPU_top.scala 119:27]
  wire  gpgpu_axi_top_io_m_r_ready; // @[GPGPU_top.scala 119:27]
  wire  gpgpu_axi_top_io_m_r_valid; // @[GPGPU_top.scala 119:27]
  wire [3:0] gpgpu_axi_top_io_m_r_bits_id; // @[GPGPU_top.scala 119:27]
  wire [63:0] gpgpu_axi_top_io_m_r_bits_data; // @[GPGPU_top.scala 119:27]
  wire  gpgpu_axi_top_io_m_r_bits_last; // @[GPGPU_top.scala 119:27]
  GPGPU_axi_top gpgpu_axi_top ( // @[GPGPU_top.scala 119:27]
    .clock(gpgpu_axi_top_clock),
    .reset(gpgpu_axi_top_resetn),
    .io_s_aw_awaddr(gpgpu_axi_top_io_s_aw_awaddr),
    .io_s_aw_awvalid(gpgpu_axi_top_io_s_aw_awvalid),
    .io_s_aw_awready(gpgpu_axi_top_io_s_aw_awready),
    .io_s_aw_awid(gpgpu_axi_top_io_s_aw_awid),
    .io_s_w_wdata(gpgpu_axi_top_io_s_w_wdata),
    .io_s_w_wvalid(gpgpu_axi_top_io_s_w_wvalid),
    .io_s_w_wready(gpgpu_axi_top_io_s_w_wready),
    .io_s_b_bvalid(gpgpu_axi_top_io_s_b_bvalid),
    .io_s_b_bready(gpgpu_axi_top_io_s_b_bready),
    .io_s_b_bid(gpgpu_axi_top_io_s_b_bid),
    .io_s_ar_araddr(gpgpu_axi_top_io_s_ar_araddr),
    .io_s_ar_arvalid(gpgpu_axi_top_io_s_ar_arvalid),
    .io_s_ar_arready(gpgpu_axi_top_io_s_ar_arready),
    .io_s_ar_arid(gpgpu_axi_top_io_s_ar_arid),
    .io_s_r_rdata(gpgpu_axi_top_io_s_r_rdata),
    .io_s_r_rvalid(gpgpu_axi_top_io_s_r_rvalid),
    .io_s_r_rready(gpgpu_axi_top_io_s_r_rready),
    .io_s_r_rid(gpgpu_axi_top_io_s_r_rid),
    .io_m_aw_ready(gpgpu_axi_top_io_m_aw_ready),
    .io_m_aw_valid(gpgpu_axi_top_io_m_aw_valid),
    .io_m_aw_bits_id(gpgpu_axi_top_io_m_aw_bits_id),
    .io_m_aw_bits_addr(gpgpu_axi_top_io_m_aw_bits_addr),
    .io_m_w_ready(gpgpu_axi_top_io_m_w_ready),
    .io_m_w_valid(gpgpu_axi_top_io_m_w_valid),
    .io_m_w_bits_data(gpgpu_axi_top_io_m_w_bits_data),
    .io_m_w_bits_strb(gpgpu_axi_top_io_m_w_bits_strb),
    .io_m_w_bits_last(gpgpu_axi_top_io_m_w_bits_last),
    .io_m_b_valid(gpgpu_axi_top_io_m_b_valid),
    .io_m_b_bits_id(gpgpu_axi_top_io_m_b_bits_id),
    .io_m_ar_ready(gpgpu_axi_top_io_m_ar_ready),
    .io_m_ar_valid(gpgpu_axi_top_io_m_ar_valid),
    .io_m_ar_bits_id(gpgpu_axi_top_io_m_ar_bits_id),
    .io_m_ar_bits_addr(gpgpu_axi_top_io_m_ar_bits_addr),
    .io_m_r_ready(gpgpu_axi_top_io_m_r_ready),
    .io_m_r_valid(gpgpu_axi_top_io_m_r_valid),
    .io_m_r_bits_id(gpgpu_axi_top_io_m_r_bits_id),
    .io_m_r_bits_data(gpgpu_axi_top_io_m_r_bits_data),
    .io_m_r_bits_last(gpgpu_axi_top_io_m_r_bits_last)
  );
  assign s_axi_lite_awready = gpgpu_axi_top_io_s_aw_awready; // @[GPGPU_top.scala 117:7]
  assign s_axi_lite_wready = gpgpu_axi_top_io_s_w_wready; // @[GPGPU_top.scala 117:7]
  assign s_axi_lite_bresp = 2'h0; // @[GPGPU_top.scala 117:7]
  assign s_axi_lite_bvalid = gpgpu_axi_top_io_s_b_bvalid; // @[GPGPU_top.scala 117:7]
  assign s_axi_lite_bid = gpgpu_axi_top_io_s_b_bid; // @[GPGPU_top.scala 117:7]
  assign s_axi_lite_arready = gpgpu_axi_top_io_s_ar_arready; // @[GPGPU_top.scala 117:7]
  assign s_axi_lite_rdata = gpgpu_axi_top_io_s_r_rdata; // @[GPGPU_top.scala 117:7]
  assign s_axi_lite_rresp = 2'h0; // @[GPGPU_top.scala 117:7]
  assign s_axi_lite_rvalid = gpgpu_axi_top_io_s_r_rvalid; // @[GPGPU_top.scala 117:7]
  assign s_axi_lite_rid = gpgpu_axi_top_io_s_r_rid; // @[GPGPU_top.scala 117:7]
  assign m_axi_awvalid = gpgpu_axi_top_io_m_aw_valid; // @[GPGPU_top.scala 118:7]
  assign m_axi_awid = gpgpu_axi_top_io_m_aw_bits_id; // @[GPGPU_top.scala 118:7]
  assign m_axi_awaddr = gpgpu_axi_top_io_m_aw_bits_addr; // @[GPGPU_top.scala 118:7]
  assign m_axi_awlen = 8'h3; // @[GPGPU_top.scala 118:7]
  assign m_axi_awsize = 3'h3; // @[GPGPU_top.scala 118:7]
  assign m_axi_awburst = 2'h1; // @[GPGPU_top.scala 118:7]
  assign m_axi_awlock = 1'h0; // @[GPGPU_top.scala 118:7]
  assign m_axi_awcache = 4'h6; // @[GPGPU_top.scala 118:7]
  assign m_axi_awprot = 3'h0; // @[GPGPU_top.scala 118:7]
  assign m_axi_awqos = 4'h0; // @[GPGPU_top.scala 118:7]
  assign m_axi_wvalid = gpgpu_axi_top_io_m_w_valid; // @[GPGPU_top.scala 118:7]
  assign m_axi_wdata = gpgpu_axi_top_io_m_w_bits_data; // @[GPGPU_top.scala 118:7]
  assign m_axi_wstrb = gpgpu_axi_top_io_m_w_bits_strb; // @[GPGPU_top.scala 118:7]
  assign m_axi_wlast = gpgpu_axi_top_io_m_w_bits_last; // @[GPGPU_top.scala 118:7]
  assign m_axi_bready = 1'h1; // @[GPGPU_top.scala 118:7]
  assign m_axi_arvalid = gpgpu_axi_top_io_m_ar_valid; // @[GPGPU_top.scala 118:7]
  assign m_axi_arid = gpgpu_axi_top_io_m_ar_bits_id; // @[GPGPU_top.scala 118:7]
  assign m_axi_araddr = gpgpu_axi_top_io_m_ar_bits_addr; // @[GPGPU_top.scala 118:7]
  assign m_axi_arlen = 8'h3; // @[GPGPU_top.scala 118:7]
  assign m_axi_arsize = 3'h3; // @[GPGPU_top.scala 118:7]
  assign m_axi_arburst = 2'h1; // @[GPGPU_top.scala 118:7]
  assign m_axi_arlock = 1'h0; // @[GPGPU_top.scala 118:7]
  assign m_axi_arcache = 4'h6; // @[GPGPU_top.scala 118:7]
  assign m_axi_arprot = 3'h0; // @[GPGPU_top.scala 118:7]
  assign m_axi_arqos = 4'h0; // @[GPGPU_top.scala 118:7]
  assign m_axi_rready = gpgpu_axi_top_io_m_r_ready; // @[GPGPU_top.scala 118:7]
  assign gpgpu_axi_top_clock = clock;
  assign gpgpu_axi_top_resetn = ~reset;
  assign gpgpu_axi_top_io_s_aw_awaddr = s_axi_lite_awaddr; // @[GPGPU_top.scala 117:7]
  assign gpgpu_axi_top_io_s_aw_awvalid = s_axi_lite_awvalid; // @[GPGPU_top.scala 117:7]
  assign gpgpu_axi_top_io_s_aw_awid = s_axi_lite_awid; // @[GPGPU_top.scala 117:7]
  assign gpgpu_axi_top_io_s_w_wdata = s_axi_lite_wdata; // @[GPGPU_top.scala 117:7]
  assign gpgpu_axi_top_io_s_w_wvalid = s_axi_lite_wvalid; // @[GPGPU_top.scala 117:7]
  assign gpgpu_axi_top_io_s_b_bready = s_axi_lite_bready; // @[GPGPU_top.scala 117:7]
  assign gpgpu_axi_top_io_s_ar_araddr = s_axi_lite_araddr; // @[GPGPU_top.scala 117:7]
  assign gpgpu_axi_top_io_s_ar_arvalid = s_axi_lite_arvalid; // @[GPGPU_top.scala 117:7]
  assign gpgpu_axi_top_io_s_ar_arid = s_axi_lite_arid; // @[GPGPU_top.scala 117:7]
  assign gpgpu_axi_top_io_s_r_rready = s_axi_lite_rready; // @[GPGPU_top.scala 117:7]
  assign gpgpu_axi_top_io_m_aw_ready = m_axi_awready; // @[GPGPU_top.scala 118:7]
  assign gpgpu_axi_top_io_m_w_ready = m_axi_wready; // @[GPGPU_top.scala 118:7]
  assign gpgpu_axi_top_io_m_b_valid = m_axi_bvalid; // @[GPGPU_top.scala 118:7]
  assign gpgpu_axi_top_io_m_b_bits_id = m_axi_bid; // @[GPGPU_top.scala 118:7]
  assign gpgpu_axi_top_io_m_ar_ready = m_axi_arready; // @[GPGPU_top.scala 118:7]
  assign gpgpu_axi_top_io_m_r_valid = m_axi_rvalid; // @[GPGPU_top.scala 118:7]
  assign gpgpu_axi_top_io_m_r_bits_id = m_axi_rid; // @[GPGPU_top.scala 118:7]
  assign gpgpu_axi_top_io_m_r_bits_data = m_axi_rdata; // @[GPGPU_top.scala 118:7]
  assign gpgpu_axi_top_io_m_r_bits_last = m_axi_rlast; // @[GPGPU_top.scala 118:7]
endmodule