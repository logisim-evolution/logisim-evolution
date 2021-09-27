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
import com.cburch.logisim.fpga.designrulecheck.netlistComponent;
import com.cburch.logisim.fpga.hdlgenerator.Hdl;
import com.cburch.logisim.fpga.hdlgenerator.InlinedHdlGeneratorFactory;
import com.cburch.logisim.util.LineBuffer;
import java.util.ArrayList;

public class AbstractConstantHdlGeneratorFactory extends InlinedHdlGeneratorFactory {

  public long getConstant(AttributeSet attrs) {
    return 0;
  }

  @Override
  public ArrayList<String> getInlinedCode(
      Netlist nets,
      Long componentId,
      netlistComponent componentInfo,
      String circuitName) {
    final var Contents = LineBuffer.getHdlBuffer();
    int NrOfBits = componentInfo.getComponent().getEnd(0).getWidth().getWidth();
    if (componentInfo.isEndConnected(0)) {
      long ConstantValue = getConstant(componentInfo.getComponent().getAttributeSet());
      if (componentInfo.getComponent().getEnd(0).getWidth().getWidth() == 1) {
        /* Single Port net */
        Contents.add("{{assign}} {{1}} {{=}} {{2}};", Hdl.getNetName(componentInfo, 0, true, nets), Hdl.getConstantVector(ConstantValue, 1))
            .add("");
      } else {
        if (nets.isContinuesBus(componentInfo, 0)) {
          /* easy case */
          Contents.add("{{assign}} {{1}} {{=}} {{2}};", Hdl.getBusNameContinues(componentInfo, 0, nets), Hdl.getConstantVector(ConstantValue, NrOfBits));
          Contents.add("");
        } else {
          /* we have to enumerate all bits */
          long mask = 1;
          String ConstValue = Hdl.zeroBit();
          for (byte bit = 0; bit < NrOfBits; bit++) {
            if ((mask & ConstantValue) != 0) ConstValue = Hdl.oneBit();
            else ConstValue = Hdl.zeroBit();
            mask <<= 1;
            Contents.add("{{assign}} {{1}} {{=}} {{2}};", Hdl.getBusEntryName(componentInfo, 0, true, bit, nets), ConstValue);
          }
          Contents.add("");
        }
      }
    }
    return Contents.getWithIndent();
  }

}
