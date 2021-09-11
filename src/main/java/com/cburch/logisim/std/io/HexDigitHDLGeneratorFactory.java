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
import com.cburch.logisim.fpga.designrulecheck.NetlistComponent;
import com.cburch.logisim.fpga.hdlgenerator.HDL;
import com.cburch.logisim.fpga.hdlgenerator.HDLGeneratorFactory;
import com.cburch.logisim.fpga.hdlgenerator.InlinedHdlGeneratorFactory;
import com.cburch.logisim.fpga.hdlgenerator.WithSelectHDLGenerator;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.util.LineBuffer;
import java.util.ArrayList;

public class HexDigitHDLGeneratorFactory extends InlinedHdlGeneratorFactory {

  @Override
  public ArrayList<String> GetInlinedCode(Netlist nets, Long componentId, NetlistComponent componentInfo, String circuitName) {
    final var startId = componentInfo.getLocalBubbleOutputStartId();
    final var signalName = LineBuffer.format("{{1}}{{<}}{{2}}{{3}}{{4}}{{>}}", LOCAL_OUTPUT_BUBBLE_BUS_NAME, (startId + 6), HDL.vectorLoopId(), startId); 
    final var contents =
        (new LineBuffer()).withHdlPairs()
            .pair("bubbleBusName", LOCAL_OUTPUT_BUBBLE_BUS_NAME)
            .pair("sigName", signalName) 
            .pair("dpName", HDL.getNetName(componentInfo, HexDigit.DP, true, nets));
    contents.add("");
    if (componentInfo.isEndConnected(HexDigit.HEX)) {
      final var generator = (new WithSelectHDLGenerator(componentInfo.getComponent().getAttributeSet().getValue(StdAttr.LABEL),
          HDL.getBusName(componentInfo, HexDigit.HEX, nets), 4, signalName, 7))
          .add(0L, "0111111")
          .add(1L, "0000110")
          .add(2L, "1011011")
          .add(3L, "1001111")
          .add(4L, "1100110")
          .add(5L, "1101101")
          .add(6L, "1111101")
          .add(7L, "0000111")
          .add(8L, "1111111")
          .add(9L, "1100111")
          .add(10L, "1110111")
          .add(11L, "1111100")
          .add(12L, "0111001")
          .add(13L, "1011110")
          .add(14L, "1111001")
          .setDefault("1110001");
      contents.add(generator.getHdlCode());
    } else {
      contents.add("{{assign}} {{sigName}} {{=}} {{1}};", HDL.GetZeroVector(7, true));
    }
    if (componentInfo.getComponent().getAttributeSet().getValue(SevenSegment.ATTR_DP))
      contents.add("{{assign}} {{bubbleBusName}}{{<}}{{1}}{{>}} {{=}} {{dpName}};", (startId + 7));
    return contents.getWithIndent();
  }

}
