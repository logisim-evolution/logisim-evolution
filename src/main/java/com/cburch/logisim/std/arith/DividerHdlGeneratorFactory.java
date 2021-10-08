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

public class DividerHdlGeneratorFactory extends AbstractHdlGeneratorFactory {
  private static final String NR_OF_BITS_STRING = "nrOfBits";
  private static final int NR_OF_BITS_ID = -1;
  private static final String CALC_BITS_STRING = "calcBits";
  private static final int CALC_BITS_ID = -2;
  private static final String UNSIGNED_STRING = "unsignedDivider";
  private static final int UNSIGNED_ID = -3;

  public DividerHdlGeneratorFactory() {
    super();
    myParametersList
        .add(NR_OF_BITS_STRING, NR_OF_BITS_ID)
        .add(CALC_BITS_STRING, CALC_BITS_ID, HdlParameters.MAP_MULTIPLY, 2)
        .add(UNSIGNED_STRING, UNSIGNED_ID, HdlParameters.MAP_ATTRIBUTE_OPTION, Comparator.MODE_ATTR, ComparatorHdlGeneratorFactory.SIGNED_MAP);
    myWires
        .addWire("s_divResult", CALC_BITS_ID)
        .addWire("s_modResult", NR_OF_BITS_ID)
        .addWire("s_extendedDividend", CALC_BITS_ID);
    myPorts
        .add(Port.INPUT, "inputA", NR_OF_BITS_ID, Divider.IN0)
        .add(Port.INPUT, "inputB", NR_OF_BITS_ID, Divider.IN1)
        .add(Port.INPUT, "upper", NR_OF_BITS_ID, Divider.UPPER)
        .add(Port.OUTPUT, "quotient", NR_OF_BITS_ID, Divider.OUT)
        .add(Port.OUTPUT, "remainder", NR_OF_BITS_ID, Divider.REM);
  }


  @Override
  public LineBuffer getModuleFunctionality(Netlist TheNetlist, AttributeSet attrs) {
    final var contents = LineBuffer.getBuffer()
            .pair("nrOfBits", NR_OF_BITS_STRING)
            .pair("unsigned", UNSIGNED_STRING)
            .pair("calcBits", CALC_BITS_STRING);

    if (Hdl.isVhdl()) {
      contents.empty().addVhdlKeywords().add("""
          s_extendedDividend({{calcBits}}-1 {{downto}} {{nrOfBits}}) <= upper;
          s_extendedDividend({{nrOfBits}}-1 {{downto}} 0) <= inputA;
          s_divResult <= std_logic_vector(unsigned(s_extendedDividend) / unsigned(inputB))
                             {{when}} {{unsigned}} = 1 {{else}}
                          std_logic_vector(signed(s_extendedDividend) / signed(inputB));
          s_modResult <= std_logic_vector(unsigned(s_extendedDividend) {{mod}} unsigned(inputB))
                             {{when}} {{unsigned}} = 1 {{else}}
                          std_logic_vector(signed(s_extendedDividend) {{mod}} signed(inputB));
          quotient  <= s_divResult({{nrOfBits}}-1 {{downto}} 0);
          remainder <= s_modResult({{nrOfBits}}-1 {{downto}} 0);
          """);
    }
    return contents.empty();
  }

  @Override
  public boolean isHdlSupportedTarget(AttributeSet attrs) {
    return Hdl.isVhdl();
  }
}
