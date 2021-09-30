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
import java.util.ArrayList;

public class BitExtenderHdlGeneratorFactory extends InlinedHdlGeneratorFactory {

  @Override
  public ArrayList<String> getInlinedCode(
      Netlist nets,
      Long componentId,
      netlistComponent componentInfo,
      String circuitName) {
    final var Contents = LineBuffer.getBuffer();
    int NrOfPins = componentInfo.nrOfEnds();
    for (int i = 1; i < NrOfPins; i++) {
      if (!componentInfo.isEndConnected(i)) {
        Reporter.report.addError(
            "Bit Extender component has floating input connection in circuit \""
                + circuitName
                + "\"!");
        // return empty buffer.
        return Contents.get();
      }
    }
    if (componentInfo.getComponent().getEnd(0).getWidth().getWidth() == 1) {
      /* Special case: Single bit output */
      Contents.add("{{assign}} {{1}} {{=}} {{2}};", Hdl.getNetName(componentInfo, 0, true, nets), Hdl.getNetName(componentInfo, 1, true, nets));
      Contents.add("");
    } else {
      /*
       * We make ourselves life easy, we just enumerate through all the
       * bits
       */
      StringBuilder Replacement = new StringBuilder();
      String type =
          (String)
              componentInfo.getComponent()
                  .getAttributeSet()
                  .getValue(BitExtender.ATTR_TYPE)
                  .getValue();
      if (type.equals("zero")) Replacement.append(Hdl.zeroBit());
      if (type.equals("one")) Replacement.append(Hdl.oneBit());
      if (type.equals("sign")) {
        if (componentInfo.getEnd(1).getNrOfBits() > 1) {
          Replacement.append(
              Hdl.getBusEntryName(
                  componentInfo,
                  1,
                  true,
                  componentInfo.getComponent().getEnd(1).getWidth().getWidth() - 1,
                  nets));
        } else {
          Replacement.append(Hdl.getNetName(componentInfo, 1, true, nets));
        }
      }
      if (type.equals("input"))
        Replacement.append(Hdl.getNetName(componentInfo, 2, true, nets));
      for (int bit = 0; bit < componentInfo.getComponent().getEnd(0).getWidth().getWidth(); bit++) {
        if (bit < componentInfo.getComponent().getEnd(1).getWidth().getWidth()) {
          if (componentInfo.getEnd(1).getNrOfBits() > 1) {
            Contents.add("{{assign}} {{1}} {{=}} {{2}};", Hdl.getBusEntryName(componentInfo, 0, true, bit, nets), Hdl.getBusEntryName(componentInfo, 1, true, bit, nets));
          } else {
            Contents.add("{{assign}} {{1}} {{=}} {{2}};", Hdl.getBusEntryName(componentInfo, 0, true, bit, nets) + Hdl.getNetName(componentInfo, 1, true, nets));
          }
        } else {
          Contents.add("{{assign}} {{1}} {{=}} {{2}};", Hdl.getBusEntryName(componentInfo, 0, true, bit, nets), Replacement);
        }
      }
      Contents.add("");
    }
    return Contents.getWithIndent();
  }
}
