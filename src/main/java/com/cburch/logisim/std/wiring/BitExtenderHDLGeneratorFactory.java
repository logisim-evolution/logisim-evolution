/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.std.wiring;

import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.fpga.designrulecheck.Netlist;
import com.cburch.logisim.fpga.designrulecheck.NetlistComponent;
import com.cburch.logisim.fpga.gui.Reporter;
import com.cburch.logisim.fpga.hdlgenerator.AbstractHDLGeneratorFactory;
import com.cburch.logisim.fpga.hdlgenerator.HDL;

import com.cburch.logisim.util.LineBuffer;
import java.util.ArrayList;

public class BitExtenderHDLGeneratorFactory extends AbstractHDLGeneratorFactory {

  @Override
  public ArrayList<String> GetInlinedCode(
      Netlist Nets,
      Long ComponentId,
      NetlistComponent ComponentInfo,
      String CircuitName) {
    final var Contents = new LineBuffer();
    int NrOfPins = ComponentInfo.nrOfEnds();
    for (int i = 1; i < NrOfPins; i++) {
      if (!ComponentInfo.isEndConnected(i)) {
        Reporter.Report.AddError(
            "Bit Extender component has floating input connection in circuit \""
                + CircuitName
                + "\"!");
        // return empty buffer.
        return Contents.get();
      }
    }
    if (ComponentInfo.getComponent().getEnd(0).getWidth().getWidth() == 1) {
      /* Special case: Single bit output */
      Contents.add("{{assign}} {{1}} {{=}} {{2}};", GetNetName(ComponentInfo, 0, true, Nets), GetNetName(ComponentInfo, 1, true, Nets));
      Contents.add("");
    } else {
      /*
       * We make ourselves life easy, we just enumerate through all the
       * bits
       */
      StringBuilder Replacement = new StringBuilder();
      String type =
          (String)
              ComponentInfo.getComponent()
                  .getAttributeSet()
                  .getValue(BitExtender.ATTR_TYPE)
                  .getValue();
      if (type.equals("zero")) Replacement.append(HDL.zeroBit());
      if (type.equals("one")) Replacement.append(HDL.oneBit());
      if (type.equals("sign")) {
        if (ComponentInfo.getEnd(1).getNrOfBits() > 1) {
          Replacement.append(
              GetBusEntryName(
                  ComponentInfo,
                  1,
                  true,
                  ComponentInfo.getComponent().getEnd(1).getWidth().getWidth() - 1,
                  Nets));
        } else {
          Replacement.append(GetNetName(ComponentInfo, 1, true, Nets));
        }
      }
      if (type.equals("input"))
        Replacement.append(GetNetName(ComponentInfo, 2, true, Nets));
      for (int bit = 0; bit < ComponentInfo.getComponent().getEnd(0).getWidth().getWidth(); bit++) {
        if (bit < ComponentInfo.getComponent().getEnd(1).getWidth().getWidth()) {
          if (ComponentInfo.getEnd(1).getNrOfBits() > 1) {
            Contents.add("{{assign}} {{1}} {{=}} {{2}};", GetBusEntryName(ComponentInfo, 0, true, bit, Nets), GetBusEntryName(ComponentInfo, 1, true, bit, Nets));
          } else {
            Contents.add("{{assign}} {{1}} {{=}} {{2}};", GetBusEntryName(ComponentInfo, 0, true, bit, Nets) + GetNetName(ComponentInfo, 1, true, Nets));
          }
        } else {
          Contents.add("{{assign}} {{1}} {{=}} {{2}};", GetBusEntryName(ComponentInfo, 0, true, bit, Nets), Replacement);
        }
      }
      Contents.add("");
    }
    return Contents.getWithIndent();
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
