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

public class AdderHdlGeneratorFactory extends AbstractHdlGeneratorFactory {

  private static final String NR_OF_BITS_STRING = "nrOfBits";
  private static final int NR_OF_BITS_ID = -1;
  private static final String EXTENDED_BITS_STRING = "extendedBits";
  private static final int EXTENDED_BITS_ID = -2;

  public AdderHdlGeneratorFactory() {
    super();
    myParametersList
        .add(EXTENDED_BITS_STRING, EXTENDED_BITS_ID, HdlParameters.MAP_OFFSET, 1)
        .addBusOnly(NR_OF_BITS_STRING, NR_OF_BITS_ID);
    myWires
        .addWire("s_extendedDataA", EXTENDED_BITS_ID)
        .addWire("s_extendedDataB", EXTENDED_BITS_ID)
        .addWire("s_sumResult", EXTENDED_BITS_ID);
    myPorts
        .add(Port.INPUT, "dataA", NR_OF_BITS_ID, Adder.IN0, StdAttr.WIDTH)
        .add(Port.INPUT, "dataB", NR_OF_BITS_ID, Adder.IN1, StdAttr.WIDTH)
        .add(Port.INPUT, "carryIn", 1, Adder.C_IN)
        .add(Port.OUTPUT, "result", NR_OF_BITS_ID, Adder.OUT, StdAttr.WIDTH)
        .add(Port.OUTPUT, "carryOut", 1, Adder.C_OUT);
  }

  @Override
  public LineBuffer getModuleFunctionality(Netlist theNetlist, AttributeSet attrs) {
    final var contents = LineBuffer.getBuffer();
    final var nrOfBits = attrs.getValue(StdAttr.WIDTH).getWidth();
    if (Hdl.isVhdl()) {
      contents.empty().add("""
          s_extendedDataA <= "0"&dataA;
          s_extendedDataB <= "0"&dataB;
          s_sumResult     <= std_logic_vector(unsigned(s_extendedDataA) +
                                               unsigned(s_extendedDataB) +
                                               (""&carryIn));
          """);
      if (nrOfBits == 1) {
        contents.add("result   <= s_sumResult(0);");
      } else {
        contents.add("result   <= s_sumResult( ({{1}}-1) DOWNTO 0 );", NR_OF_BITS_STRING);
      }
      contents.add("carryOut <= s_sumResult({{1}}-1);", EXTENDED_BITS_STRING);
    } else {
      contents.add("assign   {carryOut, result} = dataA + dataB + carryIn;");
    }
    return contents.empty();
  }
}
