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

public class AbstractConstantHdlGeneratorFactory extends InlinedHdlGeneratorFactory {

  public long getConstant(AttributeSet attrs) {
    return 0;
  }

  @Override
  public LineBuffer getInlinedCode(
      Netlist nets, Long componentId, netlistComponent componentInfo, String circuitName) {
    final var contents = LineBuffer.getHdlBuffer();
    int nrOfBits = componentInfo.getComponent().getEnd(0).getWidth().getWidth();
    if (componentInfo.isEndConnected(0)) {
      final var constantValue = getConstant(componentInfo.getComponent().getAttributeSet());
      if (componentInfo.getComponent().getEnd(0).getWidth().getWidth() == 1) {
        /* Single Port net */
        contents
            .add(
                "{{assign}} {{1}} {{=}} {{2}};",
                Hdl.getNetName(componentInfo, 0, true, nets),
                Hdl.getConstantVector(constantValue, 1))
            .add("");
      } else {
        if (nets.isContinuesBus(componentInfo, 0)) {
          /* easy case */
          contents.add(
              "{{assign}} {{1}} {{=}} {{2}};",
              Hdl.getBusNameContinues(componentInfo, 0, nets),
              Hdl.getConstantVector(constantValue, nrOfBits));
          contents.add("");
        } else {
          /* we have to enumerate all bits */
          var mask = 1L;
          var constValue = Hdl.zeroBit();
          for (var bit = 0; bit < nrOfBits; bit++) {
            if ((mask & constantValue) != 0) constValue = Hdl.oneBit();
            else constValue = Hdl.zeroBit();
            mask <<= 1;
            contents.add(
                "{{assign}} {{1}} {{=}} {{2}};",
                Hdl.getBusEntryName(componentInfo, 0, true, bit, nets), constValue);
          }
          contents.add("");
        }
      }
    }
    return contents;
  }
}
