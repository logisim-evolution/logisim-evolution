module nibble_register(input clk, reset, enable, input [3:0] d, output reg [3:0] q);
    always @(posedge clk)
        if (reset) q <= 0;
        else if (enable) q <= d;
endmodule

module hierarchy(input clk, reset, enable, input [3:0] a, b, output [3:0] qa, qb);
    nibble_register first(clk, reset, enable, a, qa);
    nibble_register second(clk, reset, enable, b, qb);
endmodule
