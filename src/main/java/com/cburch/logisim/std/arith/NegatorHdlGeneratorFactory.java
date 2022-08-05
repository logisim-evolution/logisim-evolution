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
import com.cburch.logisim.instance.Port;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.util.LineBuffer;

public class NegatorHdlGeneratorFactory extends AbstractHdlGeneratorFactory {

  private static final String NR_OF_BITS_STRING = "nrOfBits";
  private static final int NR_OF_BITS_ID = -1;

  public NegatorHdlGeneratorFactory() {
    super();
    myParametersList.addBusOnly(NR_OF_BITS_STRING, NR_OF_BITS_ID);
    myPorts
        .add(Port.INPUT, "dataX", NR_OF_BITS_ID, Negator.IN, StdAttr.WIDTH)
        .add(Port.OUTPUT, "minDataX", NR_OF_BITS_ID, Negator.OUT, StdAttr.WIDTH);
  }

  @Override
  public LineBuffer getModuleFunctionality(Netlist TheNetlist, AttributeSet attrs) {
    final var contents = LineBuffer.getBuffer();
    if (Hdl.isVhdl()) {
      int nrOfBits = attrs.getValue(StdAttr.WIDTH).getWidth();
      contents
          .empty()
          .addVhdlKeywords()
          .add(
              (nrOfBits == 1)
                  ? "minDataX <= dataX;"
                  : "minDataX <= std_logic_vector(unsigned({{not}}(dataX)) + 1);");
    } else {
      contents.add("assign minDataX = -dataX;");
    }
    return contents.empty();
  }
}
