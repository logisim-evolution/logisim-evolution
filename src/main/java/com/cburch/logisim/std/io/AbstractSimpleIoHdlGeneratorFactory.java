/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.std.io;

import java.util.HashMap;

import com.cburch.logisim.fpga.designrulecheck.Netlist;
import com.cburch.logisim.fpga.designrulecheck.netlistComponent;
import com.cburch.logisim.fpga.hdlgenerator.Hdl;
import com.cburch.logisim.fpga.hdlgenerator.InlinedHdlGeneratorFactory;
import com.cburch.logisim.util.LineBuffer;

public class AbstractSimpleIoHdlGeneratorFactory extends InlinedHdlGeneratorFactory {

  private final boolean isInputComponent;

  public AbstractSimpleIoHdlGeneratorFactory(boolean isInputComponent) {
    this.isInputComponent = isInputComponent;
  }

  @Override
  public LineBuffer getInlinedCode(Netlist nets, Long componentId, netlistComponent componentInfo, String circuitName) {
    final var contents = LineBuffer.getHdlBuffer();
    final var wires = new HashMap<String, String>();
    for (int i = 0; i < componentInfo.nrOfEnds(); i++) {
      if (componentInfo.isEndConnected(i) && isInputComponent) {
        final var pressPassive = componentInfo.getComponent().getAttributeSet().getValue(Button.ATTR_PRESS) ==  Button.BUTTON_PRESS_PASSIVE;
        final var destination = Hdl.getNetName(componentInfo, i, true, nets); 
        final var source = LineBuffer.formatHdl("{{1}}{{2}}{{<}}{{3}}{{>}}",
            (pressPassive ? Hdl.notOperator() : ""),
            LOCAL_INPUT_BUBBLE_BUS_NAME,
            componentInfo.getLocalBubbleInputStartId() + i);
        wires.put(destination, source);
      }
      if (!isInputComponent) {
        wires.put(LineBuffer.formatHdl("{{1}}{{<}}{{2}}{{>}}",
            LOCAL_OUTPUT_BUBBLE_BUS_NAME,
            (componentInfo.getLocalBubbleOutputStartId() + i)),
            Hdl.getNetName(componentInfo, i, true, nets));
      }
    }
    Hdl.addAllWiresSorted(contents, wires);
    return contents;
  }

}
