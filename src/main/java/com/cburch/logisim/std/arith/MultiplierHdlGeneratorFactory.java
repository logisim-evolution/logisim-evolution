/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.std.arith;

import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.fpga.designrulecheck.Netlist;
import com.cburch.logisim.fpga.hdlgenerator.AbstractHdlGeneratorFactory;
import com.cburch.logisim.fpga.hdlgenerator.Hdl;
import com.cburch.logisim.fpga.hdlgenerator.HdlParameters;
import com.cburch.logisim.instance.Port;
import com.cburch.logisim.util.LineBuffer;

public class MultiplierHdlGeneratorFactory extends AbstractHdlGeneratorFactory {

  private static final String NR_OF_BITS_STRING = "NrOfBits";
  private static final int NR_OF_BITS_ID = -1;
  private static final String CALC_BITS_STRING = "CalcBits";
  private static final int CALC_BITS_ID = -2;
  private static final String UNSIGNED_STRING = "UnsignedMultiplier";
  private static final int UNSIGNED_ID = -3;

  public MultiplierHdlGeneratorFactory() {
    super();
    myParametersList
        .add(NR_OF_BITS_STRING, NR_OF_BITS_ID)
        .add(CALC_BITS_STRING, CALC_BITS_ID, HdlParameters.MAP_MULTIPLY, 2)
        .add(UNSIGNED_STRING, UNSIGNED_ID, HdlParameters.MAP_ATTRIBUTE_OPTION, Comparator.MODE_ATTR, ComparatorHdlGeneratorFactory.SIGNED_MAP);
    myWires
        .addWire("s_mult_result", CALC_BITS_ID)
        .addWire("s_extended_Cin", CALC_BITS_ID)
        .addWire("s_new_result", CALC_BITS_ID);
    myPorts
        .add(Port.INPUT, "INP_A", NR_OF_BITS_ID, Multiplier.IN0)
        .add(Port.INPUT, "INP_B", NR_OF_BITS_ID, Multiplier.IN1)
        .add(Port.INPUT, "Cin", NR_OF_BITS_ID, Multiplier.C_IN)
        .add(Port.OUTPUT, "Mult_lo", NR_OF_BITS_ID, Multiplier.OUT)
        .add(Port.OUTPUT, "Mult_hi", NR_OF_BITS_ID, Multiplier.C_OUT);
  }

  @Override
  public LineBuffer getModuleFunctionality(Netlist TheNetlist, AttributeSet attrs) {
    final var contents =
          LineBuffer.getBuffer()
            .pair("nrOfBits", NR_OF_BITS_STRING)
            .pair("unsigned", UNSIGNED_STRING)
            .pair("calcBits", CALC_BITS_STRING);

    if (Hdl.isVhdl()) {
      contents.add("""
          s_mult_result <= std_logic_vector(unsigned(INP_A)*unsigned(INP_B))
                              WHEN {{unsigned}}= 1 ELSE
                           std_logic_vector(signed(INP_A)*signed(INP_B));
          s_extended_Cin({{calcBits}}-1 DOWNTO {{nrOfBits}}) <= (OTHERS => '0') WHEN {{unsigned}} = 1 ELSE (OTHERS => Cin({{nrOfBits}}-1));
          s_extended_Cin({{nrOfBits}}-1 DOWNTO 0) <= Cin;
          s_new_result  <= std_logic_vector(unsigned(s_mult_result) + unsigned(s_extended_Cin))
                              WHEN {{unsigned}}= 1 ELSE
                           std_logic_vector(signed(s_mult_result) + signed(s_extended_Cin));
          Mult_hi       <= s_new_result({{calcBits}}-1 DOWNTO {{nrOfBits}});
          Mult_lo       <= s_new_result({{nrOfBits}}-1 DOWNTO 0);
          """);
    } else {
      contents.add("""
          reg[{{calcBits}}-1:0] s_Cin;
          reg[{{calcBits}}-1:0] s_mult_unsigned;
          reg[{{calcBits}}-1:0] s_interm_result;
          reg signed[{{calcBits}}-1:0] s_mult_signed;
          always @(*)
          begin
             s_Cin[{{nrOfBits}}-1:0] = Cin;
             if ({{unsigned}}== 1)
                begin
                   s_Cin[{{calcBits}}-1:{{nrOfBits}}] = 0;
                   s_mult_unsigned = $unsigned(INP_A) * $unsigned(INP_B);
                   s_interm_result = $unsigned(s_mult_unsigned) + $unsigned(s_Cin);
                 end
              else
                begin
                   if (Cin[{{nrOfBits}}-1] == 1)
                      s_Cin[{{calcBits}}-1:{{nrOfBits}}] = -1;
                   else
                      s_Cin[{{calcBits}}-1:{{nrOfBits}}] = 0;
                   s_mult_signed = $signed(INP_A) * $signed(INP_B);
                   s_interm_result = $signed(s_mult_signed) + $signed(s_Cin);
                 end
          end

          assign Mult_hi = s_interm_result[{{calcBits}}-1:{{nrOfBits}}];
          assign Mult_lo = s_interm_result[{{nrOfBits}}-1:0];
          """);
    }
    return contents;
  }
}
