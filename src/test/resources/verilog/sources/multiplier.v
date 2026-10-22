module multiplier (
	clk,
	i_multiplicand,
	i_multiplier,
	i_mul_type,
	i_begin,
	i_reset,
	o_full_product,
	o_result,
	o_done
);
	reg _sv2v_0;
	parameter IN_WIDTH = 32;
	input wire clk;
	input wire [IN_WIDTH - 1:0] i_multiplicand;
	input wire [IN_WIDTH - 1:0] i_multiplier;
	input wire [1:0] i_mul_type;
	input wire i_begin;
	input wire i_reset;
	output reg [(IN_WIDTH * 2) - 1:0] o_full_product;
	output reg [IN_WIDTH - 1:0] o_result;
	output reg o_done;
	reg [2:0] mul_type;
	reg [2:0] mul_kind;
	reg [1:0] mul_state;
	reg [$clog2(IN_WIDTH):0] iterator;
	reg [IN_WIDTH - 1:0] multiplicand;
	reg [(2 * IN_WIDTH) - 1:0] product;
	reg booth_bit;
	reg [IN_WIDTH:0] sum_sub;
	reg [IN_WIDTH:0] unsigned_sum;
	reg [IN_WIDTH:0] hsu_sum;
	always @(*) begin
		if (_sv2v_0)
			;
		case (i_mul_type)
			2'b00: mul_type = 3'b100;
			2'b01: mul_type = 3'b000;
			2'b10: mul_type = 3'b001;
			2'b11: mul_type = 3'b101;
			default: mul_type = 3'b010;
		endcase
		case ({product[0], booth_bit})
			2'b10: sum_sub = {product[(2 * IN_WIDTH) - 1], product[(2 * IN_WIDTH) - 1:IN_WIDTH]} - {multiplicand[IN_WIDTH - 1], multiplicand};
			2'b01: sum_sub = {product[(2 * IN_WIDTH) - 1], product[(2 * IN_WIDTH) - 1:IN_WIDTH]} + {multiplicand[IN_WIDTH - 1], multiplicand};
			default: sum_sub = {product[(2 * IN_WIDTH) - 1], product[(2 * IN_WIDTH) - 1:IN_WIDTH]};
		endcase
		unsigned_sum = product[(2 * IN_WIDTH) - 1:IN_WIDTH] + multiplicand;
		hsu_sum = {product[(2 * IN_WIDTH) - 1], product[(2 * IN_WIDTH) - 1:IN_WIDTH]} + {multiplicand[IN_WIDTH - 1], multiplicand};
	end
	always @(posedge clk)
		if (i_reset) begin
			mul_kind <= 3'b010;
			mul_state <= 2'd0;
			o_done <= 0;
			iterator <= 0;
			multiplicand <= 0;
			booth_bit <= 0;
			product <= 0;
			o_done <= 0;
			o_result <= 0;
		end
		else
			case (mul_state)
				2'd0: begin
					o_done <= 0;
					iterator <= 0;
					product <= 0;
					if (o_done)
						;
					else if (i_begin) begin
						multiplicand <= i_multiplicand;
						mul_kind <= mul_type;
						mul_state <= 2'd1;
						product <= {{IN_WIDTH {1'b0}}, i_multiplier};
						booth_bit <= 1'b0;
					end
				end
				2'd1: begin
					iterator <= iterator + 1;
					if (mul_kind[2]) begin
						if (product[0])
							product <= {unsigned_sum, product[IN_WIDTH - 1:1]};
						else
							product <= {1'b0, product[(2 * IN_WIDTH) - 1:1]};
					end
					else if (~mul_kind[0]) begin
						product <= {sum_sub, product[IN_WIDTH - 1:1]};
						booth_bit <= product[0];
					end
					else if (product[0])
						product <= {hsu_sum, product[IN_WIDTH - 1:1]};
					else
						product <= {product[(2 * IN_WIDTH) - 1], product[(2 * IN_WIDTH) - 1:1]};
					if (iterator == (IN_WIDTH - 1)) begin
						iterator <= 0;
						mul_state <= 2'd2;
					end
				end
				2'd2: begin
					o_done <= 1;
					o_full_product <= product;
					if (mul_kind[2] & ~mul_kind[0])
						o_result <= product[IN_WIDTH - 1:0];
					else
						o_result <= product[(2 * IN_WIDTH) - 1:IN_WIDTH];
					mul_state <= 2'd0;
				end
				default: mul_state <= 2'd0;
			endcase
	initial _sv2v_0 = 0;
endmodule
