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

public class AdderHDLGeneratorFactory extends AbstractHdlGeneratorFactory {

  private static final String NR_OF_BITS_STRING = "NrOfBits";
  private static final int NR_OF_BITS_ID = -1;
  private static final String EXTENDED_BITS_STRING = "ExtendedBits";
  private static final int EXTENDED_BITS_ID = -2;

  public AdderHDLGeneratorFactory() {
    super();
    myParametersList
        .add(EXTENDED_BITS_STRING, EXTENDED_BITS_ID, HdlParameters.MAP_OFFSET, 1)
        .addBusOnly(NR_OF_BITS_STRING, NR_OF_BITS_ID);
    myWires
        .addWire("s_extended_dataA", EXTENDED_BITS_ID)
        .addWire("s_extended_dataB", EXTENDED_BITS_ID)
        .addWire("s_sum_result", EXTENDED_BITS_ID);
    myPorts
        .add(Port.INPUT, "DataA", NR_OF_BITS_ID, Adder.IN0, StdAttr.WIDTH)
        .add(Port.INPUT, "DataB", NR_OF_BITS_ID, Adder.IN1, StdAttr.WIDTH)
        .add(Port.INPUT, "CarryIn", 1, Adder.C_IN)
        .add(Port.OUTPUT, "Result", NR_OF_BITS_ID, Adder.OUT, StdAttr.WIDTH)
        .add(Port.OUTPUT, "CarryOut", 1, Adder.C_OUT);
  }

  @Override
  public LineBuffer getModuleFunctionality(Netlist TheNetlist, AttributeSet attrs) {
    final var contents = LineBuffer.getBuffer();
    final var nrOfBits = attrs.getValue(StdAttr.WIDTH).getWidth();
    if (Hdl.isVhdl()) {
      contents.add("""
          s_extended_dataA <= "0"&DataA;
          s_extended_dataB <= "0"&DataB;
          s_sum_result     <= std_logic_vector(unsigned(s_extended_dataA) +
                                               unsigned(s_extended_dataB) +
                                               (""&CarryIn));

          """);
      if (nrOfBits == 1) {
        contents.add("Result <= s_sum_result(0);");
      } else {
        contents.add("Result <= s_sum_result( ({{1}}-1) DOWNTO 0 )", NR_OF_BITS_STRING);
      }
      contents.add("CarryOut <= s_sum_result({{1}}-1);", EXTENDED_BITS_STRING);
    } else {
      contents.add("assign   {CarryOut,Result} = DataA + DataB + CarryIn;");
    }
    return contents;
  }
}
