/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.std.wiring;

import com.cburch.logisim.fpga.designrulecheck.Netlist;
import com.cburch.logisim.fpga.designrulecheck.netlistComponent;
import com.cburch.logisim.fpga.gui.Reporter;
import com.cburch.logisim.fpga.hdlgenerator.Hdl;
import com.cburch.logisim.fpga.hdlgenerator.InlinedHdlGeneratorFactory;
import com.cburch.logisim.util.LineBuffer;

public class BitExtenderHdlGeneratorFactory extends InlinedHdlGeneratorFactory {

  @Override
  public LineBuffer getInlinedCode(Netlist nets, Long componentId, netlistComponent componentInfo, String circuitName) {
    final var contents = LineBuffer.getBuffer();
    int nrOfPins = componentInfo.nrOfEnds();
    for (int i = 1; i < nrOfPins; i++) {
      if (!componentInfo.isEndConnected(i)) {
        // FIXME: hardcoded string
        Reporter.report.addError(String.format(
            "Bit Extender component has floating input connection in circuit: %s", circuitName));
        // return empty buffer.
        return contents;
      }
    }
    if (componentInfo.getComponent().getEnd(0).getWidth().getWidth() == 1) {
      /* Special case: Single bit output */
      contents.add("{{assign}} {{1}} {{=}} {{2}};", Hdl.getNetName(componentInfo, 0, true, nets), Hdl.getNetName(componentInfo, 1, true, nets));
      contents.add("");
    } else {
      /*
       * We make ourselves life easy, we just enumerate through all the
       * bits
       */
      final var replacement = new StringBuilder();
      final var type = (String) componentInfo.getComponent().getAttributeSet().getValue(BitExtender.ATTR_TYPE)
                  .getValue();
      if (type.equals("zero")) replacement.append(Hdl.zeroBit());
      if (type.equals("one")) replacement.append(Hdl.oneBit());
      if (type.equals("sign")) {
        if (componentInfo.getEnd(1).getNrOfBits() > 1) {
          replacement.append(Hdl.getBusEntryName(componentInfo, 1, true,
                  componentInfo.getComponent().getEnd(1).getWidth().getWidth() - 1, nets));
        } else {
          replacement.append(Hdl.getNetName(componentInfo, 1, true, nets));
        }
      }
      if (type.equals("input"))
        replacement.append(Hdl.getNetName(componentInfo, 2, true, nets));
      for (int bit = 0; bit < componentInfo.getComponent().getEnd(0).getWidth().getWidth(); bit++) {
        if (bit < componentInfo.getComponent().getEnd(1).getWidth().getWidth()) {
          if (componentInfo.getEnd(1).getNrOfBits() > 1) {
            contents.add("{{assign}} {{1}} {{=}} {{2}};", Hdl.getBusEntryName(componentInfo, 0, true, bit, nets), Hdl.getBusEntryName(componentInfo, 1, true, bit, nets));
          } else {
            contents.add("{{assign}} {{1}} {{=}} {{2}};", Hdl.getBusEntryName(componentInfo, 0, true, bit, nets) + Hdl.getNetName(componentInfo, 1, true, nets));
          }
        } else {
          contents.add("{{assign}} {{1}} {{=}} {{2}};", Hdl.getBusEntryName(componentInfo, 0, true, bit, nets), replacement);
        }
      }
      contents.empty();
    }
    return contents;
  }
}
