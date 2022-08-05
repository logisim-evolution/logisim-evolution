/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.std.gates;

import com.cburch.logisim.fpga.designrulecheck.Netlist;
import com.cburch.logisim.fpga.designrulecheck.netlistComponent;
import com.cburch.logisim.fpga.hdlgenerator.Hdl;
import com.cburch.logisim.fpga.hdlgenerator.InlinedHdlGeneratorFactory;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.util.LineBuffer;

public class ControlledBufferHdlGenerator extends InlinedHdlGeneratorFactory {

  @Override
  public LineBuffer getInlinedCode(
      Netlist nets, Long componentId, netlistComponent componentInfo, String circuitName) {
    final var contents = LineBuffer.getBuffer();
    final var triName = Hdl.getNetName(componentInfo, 2, true, nets);
    var inpName = "";
    var outpName = "";
    var triState = "";
    final var nrBits =
        componentInfo.getComponent().getAttributeSet().getValue(StdAttr.WIDTH).getWidth();
    if (nrBits > 1) {
      inpName = Hdl.getBusName(componentInfo, 1, nets);
      outpName = Hdl.getBusName(componentInfo, 0, nets);
      triState = Hdl.isVhdl() ? "({{others}} => 'Z')" : nrBits + "'bZ";
    } else {
      inpName = Hdl.getNetName(componentInfo, 1, true, nets);
      outpName = Hdl.getNetName(componentInfo, 0, true, nets);
      triState = Hdl.isVhdl() ? "'Z'" : "1'bZ";
    }
    if (componentInfo.isEndConnected(2) && componentInfo.isEndConnected(0)) {
      final var invert =
          ((ControlledBuffer) componentInfo.getComponent().getFactory()).isInverter()
              ? Hdl.notOperator()
              : "";
      if (Hdl.isVhdl()) {
        contents
            .addVhdlKeywords()
            .add(
                "{{1}}<= {{2}}{{3}} {{when}} {{4}} = '1' {{else}} {{5}};",
                outpName, invert, inpName, triName, triState);
      } else {
        contents.add(
            "assign {{1}} = ({{2}}) ? {{3}}{{4}} : {{5}};",
            outpName, triName, invert, inpName, triState);
      }
    }
    return contents;
  }
}
