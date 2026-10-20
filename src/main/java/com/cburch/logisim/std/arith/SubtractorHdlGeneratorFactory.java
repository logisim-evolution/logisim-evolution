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
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.util.LineBuffer;

public class SubtractorHdlGeneratorFactory extends AbstractHdlGeneratorFactory {

  private static final String NR_OF_BITS_STRING = "nrOfBits";
  private static final int NR_OF_BITS_ID = -1;
  private static final String EXTENDED_BITS_STRING = "extendedBits";
  private static final int EXTENDED_BITS_ID = -2;

  public SubtractorHdlGeneratorFactory() {
    super();
    myParametersList
        .addBusOnly(NR_OF_BITS_STRING, NR_OF_BITS_ID)
        .add(EXTENDED_BITS_STRING, EXTENDED_BITS_ID, HdlParameters.MAP_OFFSET, 1);
    myWires
        .addWire("s_extendeddataA", EXTENDED_BITS_ID)
        .addWire("s_extendeddataB", EXTENDED_BITS_ID)
        .addWire("s_sumresult", EXTENDED_BITS_ID)
        .addWire("n_bIn", 1)
        .addWire("s_carry", 1);
    myPorts
        .add(Port.INPUT, "dataA", NR_OF_BITS_ID, Subtractor.IN0, StdAttr.WIDTH)
        .add(Port.INPUT, "dataB", NR_OF_BITS_ID, Subtractor.IN1, StdAttr.WIDTH)
        .add(Port.INPUT, "borrowIn", 1, Subtractor.B_IN)
        .add(Port.OUTPUT, "result", NR_OF_BITS_ID, Subtractor.OUT, StdAttr.WIDTH)
        .add(Port.OUTPUT, "borrowOut", 1, Subtractor.B_OUT);
  }

  @Override
  public LineBuffer getModuleFunctionality(Netlist TheNetlist, AttributeSet attrs) {
    final var contents = LineBuffer.getBuffer();
    final var nrOfBits = attrs.getValue(StdAttr.WIDTH).getWidth();
    if (Hdl.isVhdl()) {
      contents.empty().addVhdlKeywords().add("""
          s_extendeddataA <= "0"&dataA;
          s_extendeddataB <= "0"&({{not}}(dataB));
          s_carry         <= {{not}}(borrowIn);
          s_sumresult     <= std_logic_vector(unsigned(s_extendeddataA) +
                             unsigned(s_extendeddataB) +
                             (""&s_carry));
          """);
      contents.add(
          (nrOfBits == 1)
              ? "result    <= s_sumresult(0);"
              : "result    <= s_sumresult( (" + NR_OF_BITS_STRING + "-1) {{downto}} 0 );");
      contents.add("borrowOut <= {{not}}(s_sumresult(" + EXTENDED_BITS_STRING + "-1));");
    } else {
      contents.add("""
          assign n_bIn = ~borrowIn;
          assign {s_carry,result} = dataA + ~(dataB) + n_bIn;
          assign borrowOut        = ~s_carry;
          """);
    }
    return contents.empty();
  }
}
