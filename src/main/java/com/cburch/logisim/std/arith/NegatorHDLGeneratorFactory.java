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
import com.cburch.logisim.instance.Port;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.util.LineBuffer;
import java.util.ArrayList;

public class NegatorHDLGeneratorFactory extends AbstractHDLGeneratorFactory {

  private static final String NR_OF_BITS_STRING = "NrOfBits";
  private static final int NR_OF_BITS_ID = -1;

  public NegatorHDLGeneratorFactory() {
    super();
    myParametersList
        .addBusOnly(NR_OF_BITS_STRING, NR_OF_BITS_ID);
    myPorts
        .add(Port.INPUT, "DataX", NR_OF_BITS_ID, Negator.IN, StdAttr.WIDTH)
        .add(Port.OUTPUT, "MinDataX", NR_OF_BITS_ID, Negator.OUT, StdAttr.WIDTH);
  }

  @Override
  public ArrayList<String> GetModuleFunctionality(Netlist TheNetlist, AttributeSet attrs) {
    final var Contents = LineBuffer.getBuffer();
    if (HDL.isVHDL()) {
      int nrOfBits = attrs.getValue(StdAttr.WIDTH).getWidth();
      Contents.add(
          (nrOfBits == 1)
              ? "MinDataX <= DataX;"
              : "MinDataX <= std_logic_vector(unsigned(NOT(DataX)) + 1);");
    } else {
      Contents.add("assign   MinDataX = -DataX;");
    }
    return Contents.getWithIndent();
  }
}
