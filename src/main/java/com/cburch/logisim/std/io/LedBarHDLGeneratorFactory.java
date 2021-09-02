/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.std.io;

import java.util.ArrayList;

import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.fpga.designrulecheck.Netlist;
import com.cburch.logisim.fpga.designrulecheck.NetlistComponent;
import com.cburch.logisim.fpga.hdlgenerator.AbstractHDLGeneratorFactory;
import com.cburch.logisim.fpga.hdlgenerator.HDL;
import com.cburch.logisim.fpga.hdlgenerator.HDLGeneratorFactory;

public class LedBarHDLGeneratorFactory extends AbstractHDLGeneratorFactory {

  protected Attribute<BitWidth> getAttributeColumns() {
    return LedBar.ATTR_MATRIX_COLS;
  }

  @Override
  public ArrayList<String> GetInlinedCode(Netlist netlist, Long componentId, NetlistComponent componentInfo, String circuitName) {
    final var contents = new ArrayList<String>();
    final var isSingleBus = componentInfo.getComponent().getAttributeSet().getValue(LedBar.ATTR_INPUT_TYPE).equals(LedBar.INPUT_ONE_WIRE);
    final var nrOfSegments = componentInfo.getComponent().getAttributeSet().getValue(getAttributeColumns()).getWidth();
    for (var pin = 0; pin < nrOfSegments; pin++) {
      final var destPin = HDLGeneratorFactory.LocalOutputBubbleBusname
          + HDL.BracketOpen()
          + (componentInfo.getLocalBubbleOutputStartId() + pin)
          + HDL.BracketClose();
      final var sourcePin = isSingleBus ? GetBusEntryName(componentInfo, 0, true, pin, netlist)
          : GetNetName(componentInfo, pin, true, netlist);
      contents.add("   " + HDL.assignPreamble() + destPin + HDL.assignOperator() + sourcePin + ";");
    }
    return contents;
  }

  @Override
  public boolean HDLTargetSupported(AttributeSet attrs) {
    return attrs.getValue(DotMatrixBase.ATTR_PERSIST) == 0;
  }

  @Override
  public boolean IsOnlyInlined() {
    return true;
  }
}
