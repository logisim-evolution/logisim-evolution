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
import com.cburch.logisim.fpga.hdlgenerator.AbstractHDLGeneratorFactory;
import com.cburch.logisim.fpga.hdlgenerator.HDL;
import com.cburch.logisim.fpga.hdlgenerator.HDLParameters;
import com.cburch.logisim.instance.Port;
import com.cburch.logisim.util.LineBuffer;
import java.util.ArrayList;

public class DividerHDLGeneratorFactory extends AbstractHDLGeneratorFactory {
  private static final String NR_OF_BITS_STRING = "NrOfBits";
  private static final int NR_OF_BITS_ID = -1;
  private static final String CALC_BITS_STRING = "CalcBits";
  private static final int CALC_BITS_ID = -2;
  private static final String UNSIGNED_STRING = "UnsignedDivider";
  private static final int UNSIGNED_ID = -3;

  public DividerHDLGeneratorFactory() {
    super();
    myParametersList
        .add(NR_OF_BITS_STRING, NR_OF_BITS_ID)
        .add(CALC_BITS_STRING, CALC_BITS_ID, HDLParameters.MAP_MULTIPLY, 2)
        .add(UNSIGNED_STRING, UNSIGNED_ID, HDLParameters.MAP_ATTRIBUTE_OPTION, Comparator.MODE_ATTR, ComparatorHDLGeneratorFactory.SIGNED_MAP);
    myWires
        .addWire("s_div_result", CALC_BITS_ID)
        .addWire("s_mod_result", NR_OF_BITS_ID)
        .addWire("s_extended_dividend", CALC_BITS_ID);
    myPorts
        .add(Port.INPUT, "INP_A", NR_OF_BITS_ID, Divider.IN0)
        .add(Port.INPUT, "INP_B", NR_OF_BITS_ID, Divider.IN1)
        .add(Port.INPUT, "Upper", NR_OF_BITS_ID, Divider.UPPER)
        .add(Port.OUTPUT, "Quotient", NR_OF_BITS_ID, Divider.OUT)
        .add(Port.OUTPUT, "Remainder", NR_OF_BITS_ID, Divider.REM);
  }


  @Override
  public ArrayList<String> getModuleFunctionality(Netlist TheNetlist, AttributeSet attrs) {
    final var Contents = LineBuffer.getBuffer()
            .pair("nrOfBits", NR_OF_BITS_STRING)
            .pair("unsigned", UNSIGNED_STRING)
            .pair("calcBits", CALC_BITS_STRING);

    if (HDL.isVhdl()) {
      Contents.add("""
          s_extended_dividend({{calcBits}}-1 DOWNTO {{nrOfBits}}) <= Upper;
          s_extended_dividend({{nrOfBits}}-1 DOWNTO 0) <= INP_A;
          s_div_result <= std_logic_vector(unsigned(s_extended_dividend) / unsigned(INP_B))
                             WHEN {{unsigned}} = 1 ELSE
                          std_logic_vector(signed(s_extended_dividend) / signed(INP_B));
          s_mod_result <= std_logic_vector(unsigned(s_extended_dividend) mod unsigned(INP_B))
                             WHEN {{unsigned}} = 1 ELSE
                          std_logic_vector(signed(s_extended_dividend) mod signed(INP_B));
          Quotient  <= s_div_result({{nrOfBits}}-1 DOWNTO 0);
          Remainder <= s_mod_result({{nrOfBits}}-1 DOWNTO 0);
          """);
    }
    return Contents.getWithIndent();
  }

  @Override
  public boolean isHdlSupportedTarget(AttributeSet attrs) {
    return HDL.isVhdl();
  }
}
