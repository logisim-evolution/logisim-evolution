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
import com.cburch.logisim.fpga.hdlgenerator.AbstractHDLGeneratorFactory;
import com.cburch.logisim.instance.Port;
import com.cburch.logisim.util.LineBuffer;
import java.util.ArrayList;

public class AbstractGateHDLGenerator extends AbstractHDLGeneratorFactory {

  private final boolean isInverter;

  public AbstractGateHDLGenerator() {
    this(false);
  }

  public AbstractGateHDLGenerator(boolean isInverter) {
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
          .add(Port.INPUT, String.format("gate_%d_A", gate), 1, inindex1)
          .add(Port.OUTPUT, String.format("gate_%d_O", gate), 1, outindex);
      if (!isInverter)
        myPorts.add(Port.INPUT, String.format("gate_%d_B", gate), 1, inindex2);
    }
  }

  public ArrayList<String> getLogicFunction(int index) {
    return new ArrayList<>();
  }

  @Override
  public ArrayList<String> getModuleFunctionality(Netlist TheNetlist, AttributeSet attrs) {
    final var contents = LineBuffer.getBuffer();
    final var nrOfGates = isInverter ? 6 : 4;
    for (var i = 0; i < nrOfGates; i++) {
      contents.addRemarkBlock("Here gate %d is described", i).add(getLogicFunction(i));
    }
    return contents.get();
  }

  @Override
  public boolean isHdlSupportedTarget(AttributeSet attrs) {
    /* TODO: Add support for the ones with VCC and Ground Pin */
    if (attrs == null) return false;
    return !attrs.getValue(TtlLibrary.VCC_GND);
  }
}
