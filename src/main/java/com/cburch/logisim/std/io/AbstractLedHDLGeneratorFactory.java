/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
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

public class AbstractLedHDLGeneratorFactory extends AbstractHDLGeneratorFactory {

  @Override
  public ArrayList<String> GetInlinedCode(Netlist nets, Long componentId, NetlistComponent componentInfo, String circuitName) {
    final var contents = new ArrayList<String>();
    for (var i = 0; i < componentInfo.nrOfEnds(); i++) {
      contents.add(
          "   "
              + HDL.assignPreamble()
              + HDLGeneratorFactory.LocalOutputBubbleBusname
              + HDL.BracketOpen()
              + (componentInfo.getLocalBubbleOutputStartId() + i)
              + HDL.BracketClose()
              + HDL.assignOperator()
              + GetNetName(componentInfo, i, true, nets)
              + ";");
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
