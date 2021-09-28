/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.std.gates;

import java.util.ArrayList;

import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.fpga.designrulecheck.Netlist;
import com.cburch.logisim.fpga.designrulecheck.netlistComponent;
import com.cburch.logisim.fpga.hdlgenerator.Hdl;
import com.cburch.logisim.fpga.hdlgenerator.InlinedHdlGeneratorFactory;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.util.LineBuffer;

public class AbstractBufferHdlGenerator extends InlinedHdlGeneratorFactory {

  private boolean isInverter;

  public AbstractBufferHdlGenerator(boolean isInverter) {
    this.isInverter = isInverter;
  }

  @Override
  public ArrayList<String> getInlinedCode(Netlist nets, Long componentId, netlistComponent componentInfo,
      String circuitName) {
    final var nrOfBits = componentInfo.getComponent().getAttributeSet().getValue(StdAttr.WIDTH).getWidth();
    return new ArrayList<String>() {{
        add((nrOfBits == 1)
            ? LineBuffer.format("   {{1}}{{=}}{{2}}{{3}};",
                Hdl.getNetName(componentInfo, 0, false, nets),
                isInverter ? Hdl.notOperator() : "",
                Hdl.getNetName(componentInfo, 1, false, nets))
            : LineBuffer.format("   {{1}}{{=}}{{2}}{{3}};",
                Hdl.getBusName(componentInfo, 0, nets),
                isInverter ? Hdl.notOperator() : "",
                Hdl.getBusName(componentInfo, 1, nets)));
      }};
  }

  @Override
  public boolean isHdlSupportedTarget(AttributeSet attrs) {
    var supported = true;
    if (attrs.containsAttribute(GateAttributes.ATTR_OUTPUT))
      supported = attrs.getValue(GateAttributes.ATTR_OUTPUT).equals(GateAttributes.OUTPUT_01);
    return supported;
  }
}