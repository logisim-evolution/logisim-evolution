module cache (
  input  wire       clk,
  input  wire [7:0] addr_r_i,
  input  wire [7:0] addr_w_i,
  input  wire [7:0] w_data,
  input  wire       w_en,
  output reg  [7:0] d_out
);
  reg [7:0] cache[0:255];
  always @(posedge clk) begin
    d_out <= cache[addr_r_i];
    if (w_en) begin
      cache[addr_w_i] <= w_data;
    end
  end
endmodule

module memory (
  input  wire       clk,
  input  wire [7:0] addr_r_i,
  input  wire [7:0] addr_w_i,
  input  wire [7:0] w_data,
  input  wire       w_en,
  output reg  [7:0] d_out
);

  cache cache1 (
    clk,
    addr_r_i,
    addr_w_i,
    w_data,
    w_en,
    d_out
  );
endmodule
