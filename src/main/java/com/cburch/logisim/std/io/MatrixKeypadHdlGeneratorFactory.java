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
import com.cburch.logisim.fpga.designrulecheck.netlistComponent;
import com.cburch.logisim.fpga.hdlgenerator.Hdl;
import com.cburch.logisim.fpga.hdlgenerator.InlinedHdlGeneratorFactory;
import com.cburch.logisim.util.LineBuffer;
import java.util.HashMap;

public class MatrixKeypadHdlGeneratorFactory extends InlinedHdlGeneratorFactory {

  @Override
  public boolean isHdlSupportedTarget(AttributeSet attrs) {
    return attrs != null && attrs.getValue(MatrixKeypad.ATTR_MODE) == MatrixKeypad.MODE_PMOD_KYPD;
  }

  @Override
  public LineBuffer getInlinedCode(
      Netlist nets, Long componentId, netlistComponent componentInfo, String circuitName) {
    final var contents = LineBuffer.getHdlBuffer();
    final var wires = new HashMap<String, String>();

    // Rows (ends 0..3, INPUTs)
    for (int r = 0; r < MatrixKeypad.ROWS; r++) {
      final var end = MatrixKeypad.ROW_BASE + r;
      if (componentInfo.isEndConnected(end)) {
        final var source = Hdl.getNetName(componentInfo, end, true, nets);
        final var bubbleIdx = componentInfo.getLocalBubbleInputStartId() + r;
        wires.put(LineBuffer.formatHdl("{{1}}{{<}}{{2}}{{>}}", LOCAL_INPUT_BUBBLE_BUS_NAME, bubbleIdx), source);
      }
    }

    // Cols (ends 4..7, OUTPUTs)
    for (int c = 0; c < MatrixKeypad.COLS; c++) {
      final var end = MatrixKeypad.COL_BASE + c;
      if (componentInfo.isEndConnected(end)) {
        wires.put(
            LineBuffer.formatHdl(
                "{{1}}{{<}}{{2}}{{>}}", LOCAL_OUTPUT_BUBBLE_BUS_NAME, componentInfo.getLocalBubbleOutputStartId() + c),
            Hdl.getNetName(componentInfo, end, true, nets));
      }
    }

    Hdl.addAllWiresSorted(contents, wires);
    return contents;
  }
}
