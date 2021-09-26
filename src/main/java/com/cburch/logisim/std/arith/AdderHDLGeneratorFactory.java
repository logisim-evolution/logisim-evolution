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
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.util.LineBuffer;
import java.util.ArrayList;

public class AdderHDLGeneratorFactory extends AbstractHDLGeneratorFactory {

  private static final String NR_OF_BITS_STRING = "NrOfBits";
  private static final int NR_OF_BITS_ID = -1;
  private static final String EXTENDED_BITS_STRING = "ExtendedBits";
  private static final int EXTENDED_BITS_ID = -2;
  
  public AdderHDLGeneratorFactory() {
    super();
    myParametersList
        .add(EXTENDED_BITS_STRING, EXTENDED_BITS_ID, HDLParameters.MAP_OFFSET, 1)
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
  public ArrayList<String> GetModuleFunctionality(Netlist TheNetlist, AttributeSet attrs) {
    final var Contents = LineBuffer.getBuffer();
    int nrOfBits = attrs.getValue(StdAttr.WIDTH).getWidth();
    if (HDL.isVHDL()) {
      Contents.add("""
          s_extended_dataA <= "0"&DataA;
          s_extended_dataB <= "0"&DataB;
          s_sum_result     <= std_logic_vector(unsigned(s_extended_dataA) +
                                               unsigned(s_extended_dataB) +
                                               (""&CarryIn));
          
          """);
      if (nrOfBits == 1) {
        Contents.add("Result <= s_sum_result(0);");
      } else {
        Contents.add("Result <= s_sum_result( ({{1}}-1) DOWNTO 0 )", NR_OF_BITS_STRING);
      }
      Contents.add("CarryOut <= s_sum_result({{1}}-1);", EXTENDED_BITS_STRING);
    } else {
      Contents.add("assign   {CarryOut,Result} = DataA + DataB + CarryIn;");
    }
    return Contents.getWithIndent();
  }
}
