/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.std.ttl;

import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.fpga.designrulecheck.Netlist;
import com.cburch.logisim.fpga.hdlgenerator.AbstractHdlGeneratorFactory;
import com.cburch.logisim.instance.Port;
import com.cburch.logisim.util.LineBuffer;

public class AbstractGateHdlGenerator extends AbstractHdlGeneratorFactory {

  private final boolean isInverter;

  public AbstractGateHdlGenerator() {
    this(false);
  }

  public AbstractGateHdlGenerator(boolean isInverter) {
    super();
    this.isInverter = isInverter;
    final var nrOfGates = (isInverter) ? 6 : 4;
    for (var gate = 0; gate < nrOfGates; gate++) {
      var inindex1 = (gate < 2) ? gate * 3 : gate * 3 + 1;
      final var inindex2 = inindex1 + 1;
      var outindex = (gate < 2) ? gate * 3 + 2 : gate * 3;
      if (isInverter) {
        inindex1 = (gate < 3) ? gate * 2 : gate * 2 + 1;
        outindex = (gate < 3) ? gate * 2 + 1 : gate * 2;
      }
      myPorts
          .add(Port.INPUT, String.format("gateA%d", gate), 1, inindex1)
          .add(Port.OUTPUT, String.format("gateO%d", gate), 1, outindex);
      if (!isInverter) myPorts.add(Port.INPUT, String.format("gateB%d", gate), 1, inindex2);
    }
  }

  public LineBuffer getLogicFunction(int index) {
    return LineBuffer.getHdlBuffer();
  }

  @Override
  public LineBuffer getModuleFunctionality(Netlist theNetlist, AttributeSet attrs) {
    final var contents = LineBuffer.getBuffer();
    final var nrOfGates = isInverter ? 6 : 4;
    for (var i = 0; i < nrOfGates; i++) {
      contents.addRemarkBlock("Here gate %d is described", i).add(getLogicFunction(i));
    }
    return contents;
  }

  @Override
  public boolean isHdlSupportedTarget(AttributeSet attrs) {
    /* TODO: Add support for the ones with VCC and Ground Pin */
    if (attrs == null) return false;
    return !attrs.getValue(TtlLibrary.VCC_GND);
  }
}
