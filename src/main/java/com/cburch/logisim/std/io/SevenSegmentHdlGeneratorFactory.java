/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.std.io;

import com.cburch.logisim.fpga.designrulecheck.Netlist;
import com.cburch.logisim.fpga.designrulecheck.netlistComponent;
import com.cburch.logisim.fpga.hdlgenerator.Hdl;
import com.cburch.logisim.fpga.hdlgenerator.InlinedHdlGeneratorFactory;
import com.cburch.logisim.util.LineBuffer;
import java.util.HashMap;

/**
 * Maps each segment bubble to either its own pin or one bit of the segment bus. The FPGA pin names
 * stay {@code Segment_A} … {@code DecimalPoint} in both modes.
 */
public class SevenSegmentHdlGeneratorFactory extends InlinedHdlGeneratorFactory {

  @Override
  public LineBuffer getInlinedCode(
      Netlist netlist, Long componentId, netlistComponent componentInfo, String circuitName) {
    final var contents = LineBuffer.getHdlBuffer();
    final var attrs = componentInfo.getComponent().getAttributeSet();
    final var bus = SevenSegment.isBusInput(attrs);
    final var nrOfSegments = SevenSegment.segmentCount(attrs);
    final var wires = new HashMap<String, String>();
    for (var pin = 0; pin < nrOfSegments; pin++) {
      final var destPin =
          LineBuffer.format(
              "{{1}}{{<}}{{2}}{{>}}",
              LOCAL_OUTPUT_BUBBLE_BUS_NAME, componentInfo.getLocalBubbleOutputStartId() + pin);
      final var sourcePin =
          bus
              ? Hdl.getBusEntryName(componentInfo, 0, true, pin, netlist)
              : Hdl.getNetName(componentInfo, pin, true, netlist);
      wires.put(destPin, sourcePin);
    }
    Hdl.addAllWiresSorted(contents, wires);
    return contents;
  }
}
