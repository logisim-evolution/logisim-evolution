module add_sub #(
  parameter WIDTH = 4
) (
  input  wire             clk,
  input  wire             rst,
  input  wire             cmd_i,
  input  wire [WIDTH-1:0] a_i,
  input  wire [WIDTH-1:0] b_i,
  output reg  [WIDTH-1:0] result_o
);
  always @(posedge clk) begin
    if (rst) begin
      result_o <= 0;
    end
    else begin
      result_o <= cmd_i ? a_i + b_i : a_i - b_i;
    end
  end
endmodule
