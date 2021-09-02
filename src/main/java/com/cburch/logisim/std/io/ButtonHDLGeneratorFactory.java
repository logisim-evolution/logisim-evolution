/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by Logisim-evolution developers
 * 
 * https://github.com/logisim-evolution/
 * 
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.std.io;

import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.fpga.designrulecheck.Netlist;
import com.cburch.logisim.fpga.designrulecheck.NetlistComponent;
import com.cburch.logisim.fpga.hdlgenerator.AbstractHDLGeneratorFactory;
import com.cburch.logisim.fpga.hdlgenerator.HDL;
import com.cburch.logisim.fpga.hdlgenerator.HDLGeneratorFactory;
import java.util.ArrayList;

public class ButtonHDLGeneratorFactory extends AbstractHDLGeneratorFactory {

  @Override
  public ArrayList<String> GetInlinedCode(Netlist nets, Long componentId, NetlistComponent componentInfo, String circuitName) {
    final var contents = new ArrayList<String>();
    for (int i = 0; i < componentInfo.nrOfEnds(); i++) {
      if (componentInfo.isEndConnected(i)) {
        final var pressPassive = componentInfo.getComponent().getAttributeSet().getValue(Button.ATTR_PRESS) ==  Button.BUTTON_PRESS_PASSIVE;
        contents.add(
            "   "
                + HDL.assignPreamble()
                + GetNetName(componentInfo, i, true, nets)
                + HDL.assignOperator()
                + (pressPassive ? HDL.notOperator() : "")
                + HDLGeneratorFactory.LocalInputBubbleBusname
                + HDL.BracketOpen()
                + (componentInfo.getLocalBubbleInputStartId() + i)
                + HDL.BracketClose()
                + ";");
      }
    }
    return contents;
  }

  @Override
  public boolean HDLTargetSupported(AttributeSet attrs) {
    return true;
  }

  @Override
  public boolean IsOnlyInlined() {
    return true;
  }
}
