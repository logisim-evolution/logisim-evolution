/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.std.io;

import java.util.ArrayList;

import com.cburch.logisim.fpga.designrulecheck.Netlist;
import com.cburch.logisim.fpga.designrulecheck.NetlistComponent;
import com.cburch.logisim.fpga.hdlgenerator.HDL;
import com.cburch.logisim.fpga.hdlgenerator.InlinedHdlGeneratorFactory;
import com.cburch.logisim.util.LineBuffer;

public class AbstractSimpleIOHDLGeneratorFactory extends InlinedHdlGeneratorFactory {
  
  private final boolean isInputComponent;
  
  public AbstractSimpleIOHDLGeneratorFactory(boolean isInputComponent) {
    this.isInputComponent = isInputComponent;
  }

  @Override
  public ArrayList<String> GetInlinedCode(Netlist nets, Long componentId, NetlistComponent componentInfo, String circuitName) {
    final var contents = (new LineBuffer()).addHdlPairs();
    for (int i = 0; i < componentInfo.nrOfEnds(); i++) {
      if (componentInfo.isEndConnected(i) && isInputComponent) {
        final var pressPassive = componentInfo.getComponent().getAttributeSet().getValue(Button.ATTR_PRESS) ==  Button.BUTTON_PRESS_PASSIVE;
        contents.add("{{assign}} {{1}} {{=}} {{2}}{{3}}{{<}}{{4}}{{>}};",
            HDL.getNetName(componentInfo, i, true, nets),
            (pressPassive ? HDL.notOperator() : ""),
            LOCAL_INPUT_BUBBLE_BUS_NAME,
            componentInfo.getLocalBubbleInputStartId() + i);
      }
      if (!isInputComponent) {
        contents.add("{{assign}} {{1}}{{<}}{{2}}{{>}} {{=}} {{3}};",
            LOCAL_OUTPUT_BUBBLE_BUS_NAME,
            (componentInfo.getLocalBubbleOutputStartId() + i),
            HDL.getNetName(componentInfo, i, true, nets));
      }
    }
    return contents.getWithIndent(3);
  }

}
