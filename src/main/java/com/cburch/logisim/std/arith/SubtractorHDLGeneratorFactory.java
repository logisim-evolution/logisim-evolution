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

public class SubtractorHDLGeneratorFactory extends AbstractHdlGeneratorFactory {

  private static final String NR_OF_BITS_STRING = "NrOfBits";
  private static final int NR_OF_BITS_ID = -1;
  private static final String EXTENDED_BITS_STRING = "ExtendedBits";
  private static final int EXTENDED_BITS_ID = -2;

  public SubtractorHDLGeneratorFactory() {
    super();
    myParametersList
        .addBusOnly(NR_OF_BITS_STRING, NR_OF_BITS_ID)
        .add(EXTENDED_BITS_STRING, EXTENDED_BITS_ID, HdlParameters.MAP_OFFSET, 1);
    myWires
        .addWire("s_extended_dataA", EXTENDED_BITS_ID)
        .addWire("s_extended_dataB", EXTENDED_BITS_ID)
        .addWire("s_sum_result", EXTENDED_BITS_ID)
        .addWire("s_carry", 1);
    myPorts
        .add(Port.INPUT, "DataA", NR_OF_BITS_ID, Subtractor.IN0, StdAttr.WIDTH)
        .add(Port.INPUT, "DataB", NR_OF_BITS_ID, Subtractor.IN1, StdAttr.WIDTH)
        .add(Port.INPUT, "BorrowIn", 1, Subtractor.B_IN)
        .add(Port.OUTPUT, "Result", NR_OF_BITS_ID, Subtractor.OUT, StdAttr.WIDTH)
        .add(Port.OUTPUT, "BorrowOut", 1, Subtractor.B_OUT);
  }

  @Override
  public LineBuffer getModuleFunctionality(Netlist TheNetlist, AttributeSet attrs) {
    final var contents = LineBuffer.getBuffer();
    final var nrOfBits = attrs.getValue(StdAttr.WIDTH).getWidth();
    if (Hdl.isVhdl()) {
      contents.add("""
          s_extended_dataA <= "0"&DataA;
          s_extended_dataB <= "0"&(NOT(DataB));
          s_carry          <= NOT(BorrowIn);
          s_sum_result     <= std_logic_vector(unsigned(s_extended_dataA)+
                              unsigned(s_extended_dataB)+
                              (""&s_carry));
          
          """);
      contents.add(
          (nrOfBits == 1)
              ? "Result <= s_sum_result(0);"
              : "Result <= s_sum_result( (" + NR_OF_BITS_STRING + "-1) DOWNTO 0 );");
      contents.add("BorrowOut <= NOT(s_sum_result(" + EXTENDED_BITS_STRING + "-1));");
    } else {
      contents.add("""
          assign   {s_carry,Result} = DataA + ~(DataB) + ~(BorrowIn);
          assign   BorrowOut = ~s_carry;
          """);
    }
    return contents;
  }
}
