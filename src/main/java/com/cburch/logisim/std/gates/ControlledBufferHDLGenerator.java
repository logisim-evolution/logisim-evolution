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
import com.cburch.logisim.fpga.designrulecheck.NetlistComponent;
import com.cburch.logisim.fpga.hdlgenerator.HDL;
import com.cburch.logisim.fpga.hdlgenerator.InlinedHDLGeneratorFactory;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.util.LineBuffer;
import java.util.ArrayList;

public class ControlledBufferHDLGenerator extends InlinedHDLGeneratorFactory {

  @Override
  public ArrayList<String> getInlinedCode(Netlist nets, Long componentId, NetlistComponent componentInfo, String circuitName) {
    final var contents = LineBuffer.getBuffer();
    final var triName = HDL.getNetName(componentInfo, 2, true, nets);
    var inpName = "";
    var outpName = "";
    var triState = "";
    final var nrBits = componentInfo.getComponent().getAttributeSet().getValue(StdAttr.WIDTH).getWidth();
    if (nrBits > 1) {
      inpName = HDL.getBusName(componentInfo, 1, nets);
      outpName = HDL.getBusName(componentInfo, 0, nets);
      triState = HDL.isVHDL() ? "(OTHERS => 'Z')" : nrBits + "'bZ";
    } else {
      inpName = HDL.getNetName(componentInfo, 1, true, nets);
      outpName = HDL.getNetName(componentInfo, 0, true, nets);
      triState = HDL.isVHDL() ? "'Z'" : "1'bZ";
    }
    if (componentInfo.isEndConnected(2) && componentInfo.isEndConnected(0)) {
      final var invert = ((ControlledBuffer) componentInfo.getComponent().getFactory()).isInverter()
              ? HDL.notOperator()
              : "";
      if (HDL.isVHDL()) {
        contents.add("   {{1}}<= {{2}}{{3}} WHEN {{4}} = '1' ELSE {{5}};", outpName, invert, inpName, triName, triState);
      } else {
        contents.add("   assign {{1}} = ({{2}}) ? {{3}}{{4}} : {{5}};", outpName, triName, invert, inpName, triState);
      }
    }
    return contents.get();
  }
}
