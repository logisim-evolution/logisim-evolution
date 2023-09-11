/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.std.gates;

import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.fpga.designrulecheck.Netlist;
import com.cburch.logisim.fpga.designrulecheck.netlistComponent;
import com.cburch.logisim.fpga.hdlgenerator.Hdl;
import com.cburch.logisim.fpga.hdlgenerator.InlinedHdlGeneratorFactory;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.util.LineBuffer;

public class AbstractBufferHdlGenerator extends InlinedHdlGeneratorFactory {

  private final boolean isInverter;

  public AbstractBufferHdlGenerator(boolean isInverter) {
    this.isInverter = isInverter;
  }

  @Override
  public LineBuffer getInlinedCode(
      Netlist nets, Long componentId, netlistComponent componentInfo, String circuitName) {
    final var nrOfBits =
        componentInfo.getComponent().getAttributeSet().getValue(StdAttr.WIDTH).getWidth();
    final var dest =
        (nrOfBits == 1)
            ? Hdl.getNetName(componentInfo, 0, false, nets)
            : Hdl.getBusName(componentInfo, 0, nets);
    final var source =
        (nrOfBits == 1)
            ? Hdl.getNetName(componentInfo, 1, false, nets)
            : Hdl.getBusName(componentInfo, 1, nets);
    return !componentInfo.isEndConnected(0) 
            ? LineBuffer.getBuffer().add("")
            : LineBuffer.getHdlBuffer()
                .add("{{assign}}{{1}}{{=}}{{2}}{{3}};", dest, isInverter ? Hdl.notOperator() : "", source);
  }

  @Override
  public boolean isHdlSupportedTarget(AttributeSet attrs) {
    var supported = true;
    if (attrs.containsAttribute(GateAttributes.ATTR_OUTPUT))
      supported = attrs.getValue(GateAttributes.ATTR_OUTPUT).equals(GateAttributes.OUTPUT_01);
    return supported;
  }
}
