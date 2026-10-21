module buffer #(
  parameter WIDTH = 3
) (
  input  wire [WIDTH-1:0] in,
  output wire [WIDTH-1:0] out
);
  assign out = in;
endmodule
