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

import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.fpga.designrulecheck.Netlist;
import com.cburch.logisim.fpga.designrulecheck.NetlistComponent;
import com.cburch.logisim.fpga.hdlgenerator.HDL;
import com.cburch.logisim.fpga.hdlgenerator.InlinedHDLGeneratorFactory;
import com.cburch.logisim.util.LineBuffer;

public class LedBarHDLGeneratorFactory extends InlinedHDLGeneratorFactory {

  protected Attribute<BitWidth> getAttributeColumns() {
    return LedBar.ATTR_MATRIX_COLS;
  }

  @Override
  public ArrayList<String> getInlinedCode(Netlist netlist, Long componentId, NetlistComponent componentInfo, String circuitName) {
    final var contents = LineBuffer.getBuffer().addHdlPairs();
    final var isSingleBus = componentInfo.getComponent().getAttributeSet().getValue(LedBar.ATTR_INPUT_TYPE).equals(LedBar.INPUT_ONE_WIRE);
    final var nrOfSegments = componentInfo.getComponent().getAttributeSet().getValue(getAttributeColumns()).getWidth();
    for (var pin = 0; pin < nrOfSegments; pin++) {
      final var destPin = LineBuffer.format("{{1}}{{<}}{{2}}{{>}}", LOCAL_OUTPUT_BUBBLE_BUS_NAME,
          componentInfo.getLocalBubbleOutputStartId() + pin);
      final var sourcePin = isSingleBus ? HDL.getBusEntryName(componentInfo, 0, true, pin, netlist)
          : HDL.getNetName(componentInfo, pin, true, netlist);
      contents.add("{{assign}} {{1}} {{=}} {{2}};", destPin, sourcePin);
    }
    return contents.getWithIndent(3);
  }

  @Override
  public boolean isHDLSupportedTarget(AttributeSet attrs) {
    return attrs.getValue(DotMatrixBase.ATTR_PERSIST) == 0;
  }
}
