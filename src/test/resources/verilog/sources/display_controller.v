`define disp_width 128
`define disp_height 128
module display_controller (
  input  wire                            clk,
  input  wire                            rst,
  input  wire                            bgn,
  output reg                             disp_we_o,
  output reg  [ $clog2(`disp_width)-1:0] x_o,
  output reg  [$clog2(`disp_height)-1:0] y_o,
  output reg  [                    23:0] clr_o,
  output reg                             disp_rst_o
);
  localparam IDLE = 0;
  localparam CLR_SET = 1;
  localparam COORD_UPDATE = 2;
  reg [                     1:0] state;
  reg [ $clog2(`disp_width)-1:0] cx;
  reg [$clog2(`disp_height)-1:0] cy;
  parameter [23:0] GREEN = 24'h00FF00;
  parameter [23:0] RED = 24'hFF0000;
  parameter [23:0] BLUE = 24'h0000FF;
  reg [1:0] clr_index;
  assign disp_rst_o = rst;
  assign x_o        = cx;
  assign y_o        = cy;
  always @(posedge clk) begin
    disp_we_o <= 0;
    if (rst) begin
      state <= IDLE;
    end
    else begin
      case (state)
        IDLE: begin
          cx        <= 0;
          cy        <= 0;
          clr_index <= 0;
          clr_o     <= 0;
          if (bgn) begin
            state <= CLR_SET;
          end
        end
        CLR_SET: begin
          case (clr_index)
            2'd0: begin
              clr_o <= RED;
            end
            2'd1: begin
              clr_o <= BLUE;
            end
            2'd2: begin
              clr_o <= GREEN;
            end
          endcase
          disp_we_o <= 1;
          state     <= COORD_UPDATE;
        end
        COORD_UPDATE: begin
          cx <= cx + 1;
          if (cx == `disp_width - 1) begin
            if (cy == `disp_height - 1) begin
              if (clr_index == 2) begin
                clr_index <= 0;
              end
              else begin
                clr_index <= clr_index + 1;
              end
            end
            cy <= cy + 1;
          end
          state <= CLR_SET;
        end
      endcase
    end
  end
endmodule
