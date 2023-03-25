//Copyright 1986-2019 Xilinx, Inc. All Rights Reserved.
//--------------------------------------------------------------------------------
//Tool Version: Vivado v.2019.1 (win64) Build 2552052 Fri May 24 14:49:42 MDT 2019
//Date        : Fri Aug  5 17:21:39 2022
//Host        : DSPLABYZX running 64-bit major release  (build 9200)
//Command     : generate_target config_mb_wrapper.bd
//Design      : config_mb_wrapper
//Purpose     : IP block netlist
//--------------------------------------------------------------------------------
`timescale 1 ps / 1 ps

module config_mb_wrapper
   (ddr4_sdram_act_n,
    ddr4_sdram_adr,
    ddr4_sdram_ba,
    ddr4_sdram_bg,
    ddr4_sdram_ck_c,
    ddr4_sdram_ck_t,
    ddr4_sdram_cke,
    ddr4_sdram_cs_n,
    ddr4_sdram_dm_n,
    ddr4_sdram_dq,
    ddr4_sdram_dqs_c,
    ddr4_sdram_dqs_t,
    ddr4_sdram_odt,
    ddr4_sdram_reset_n,
    default_100mhz_clk_clk_n,
    default_100mhz_clk_clk_p,
    led_8bits_tri_o,
    reset_0,
    rs232_uart_0_rxd,
    rs232_uart_0_txd);
  output ddr4_sdram_act_n;
  output [16:0]ddr4_sdram_adr;
  output [1:0]ddr4_sdram_ba;
  output ddr4_sdram_bg;
  output ddr4_sdram_ck_c;
  output ddr4_sdram_ck_t;
  output ddr4_sdram_cke;
  output [1:0]ddr4_sdram_cs_n;
  inout [8:0]ddr4_sdram_dm_n;
  inout [71:0]ddr4_sdram_dq;
  inout [8:0]ddr4_sdram_dqs_c;
  inout [8:0]ddr4_sdram_dqs_t;
  output ddr4_sdram_odt;
  output ddr4_sdram_reset_n;
  input default_100mhz_clk_clk_n;
  input default_100mhz_clk_clk_p;
  output [7:0]led_8bits_tri_o;
  input reset_0;
  input rs232_uart_0_rxd;
  output rs232_uart_0_txd;

  wire ddr4_sdram_act_n;
  wire [16:0]ddr4_sdram_adr;
  wire [1:0]ddr4_sdram_ba;
  wire ddr4_sdram_bg;
  wire ddr4_sdram_ck_c;
  wire ddr4_sdram_ck_t;
  wire ddr4_sdram_cke;
  wire [1:0]ddr4_sdram_cs_n;
  wire [8:0]ddr4_sdram_dm_n;
  wire [71:0]ddr4_sdram_dq;
  wire [8:0]ddr4_sdram_dqs_c;
  wire [8:0]ddr4_sdram_dqs_t;
  wire ddr4_sdram_odt;
  wire ddr4_sdram_reset_n;
  wire default_100mhz_clk_clk_n;
  wire default_100mhz_clk_clk_p;
  wire [7:0]led_8bits_tri_o;
  wire reset_0;
  wire rs232_uart_0_rxd;
  wire rs232_uart_0_txd;

  config_mb config_mb_i
       (.ddr4_sdram_act_n(ddr4_sdram_act_n),
        .ddr4_sdram_adr(ddr4_sdram_adr),
        .ddr4_sdram_ba(ddr4_sdram_ba),
        .ddr4_sdram_bg(ddr4_sdram_bg),
        .ddr4_sdram_ck_c(ddr4_sdram_ck_c),
        .ddr4_sdram_ck_t(ddr4_sdram_ck_t),
        .ddr4_sdram_cke(ddr4_sdram_cke),
        .ddr4_sdram_cs_n(ddr4_sdram_cs_n),
        .ddr4_sdram_dm_n(ddr4_sdram_dm_n),
        .ddr4_sdram_dq(ddr4_sdram_dq),
        .ddr4_sdram_dqs_c(ddr4_sdram_dqs_c),
        .ddr4_sdram_dqs_t(ddr4_sdram_dqs_t),
        .ddr4_sdram_odt(ddr4_sdram_odt),
        .ddr4_sdram_reset_n(ddr4_sdram_reset_n),
        .default_100mhz_clk_clk_n(default_100mhz_clk_clk_n),
        .default_100mhz_clk_clk_p(default_100mhz_clk_clk_p),
        .led_8bits_tri_o(led_8bits_tri_o),
        .reset_0(reset_0),
        .rs232_uart_0_rxd(rs232_uart_0_rxd),
        .rs232_uart_0_txd(rs232_uart_0_txd));
endmodule
