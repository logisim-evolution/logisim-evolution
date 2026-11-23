module word_pipeline (
    input clk, reset, enable, subtract,
    input [13:0] a, b,
    output reg [13:0] q
);
    wire [13:0] sum = a + b;
    wire [13:0] difference = a - b;
    wire [13:0] selected = subtract ? difference : sum;
    always @(posedge clk)
        if (reset) q <= 14'b0;
        else if (enable) q <= selected;
endmodule
