module native_memory (
    input clk,
    input we,
    input [13:0] addr,
    input [31:0] data,
    output [31:0] q,
    output [7:0] lookup
);
    // 64 KiB: one native RAM rather than 16,384 registers plus address logic.
    reg [31:0] words [0:16383];
    always @(posedge clk)
        if (we) words[addr] <= data;
    assign q = words[addr];

    reg [7:0] table_data [0:255];
    integer i;
    initial
        for (i = 0; i < 256; i = i + 1)
            table_data[i] = (i * 37) ^ 8'hA5;
    assign lookup = table_data[addr[7:0]];
endmodule
