`timescale 1ns / 1ps

module SimpleModuleTest;

  // Inputs
  reg clock;
  reg reset;
  reg [3:0] io_current_mask;

  // Outputs
  wire [1:0] io_current_mask_index_0;
  wire [1:0] io_current_mask_index_1;
  wire [1:0] io_current_mask_index_2;
  wire [1:0] io_current_mask_index_3;

  // Instantiate the Unit Under Test (UUT)
  SimpleModule uut (
                 .clock(clock),
                 .reset(reset),
                 .io_current_mask(io_current_mask),
                 .io_current_mask_index_0(io_current_mask_index_0),
                 .io_current_mask_index_1(io_current_mask_index_1),
                 .io_current_mask_index_2(io_current_mask_index_2),
                 .io_current_mask_index_3(io_current_mask_index_3)
               );

  initial
  begin
    // Initialize Inputs
    clock = 0;
    reset = 1;
    io_current_mask = 0;

    // Wait 100 ns for global reset to finish
    #100;
    reset = 0;

    // Add stimulus here
    test_masks;
    #10 $finish;
  end

  // Clock generation
  always #5 clock = ~clock;

  task test_masks;
    integer i;
    for (i = 0; i < 16; i = i + 1)
    begin
      io_current_mask = i;
      #10; // Wait for the change to propagate
      $display("Mask: %b, Index: %d %d %d %d", io_current_mask,
               io_current_mask_index_3, io_current_mask_index_2,
               io_current_mask_index_1, io_current_mask_index_0);
    end
  endtask
endmodule
