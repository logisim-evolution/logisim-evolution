/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.std.ttl;

import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.fpga.designrulecheck.Netlist;
import com.cburch.logisim.fpga.hdlgenerator.AbstractHdlGeneratorFactory;
import com.cburch.logisim.instance.Port;
import com.cburch.logisim.util.LineBuffer;

public class Ttl7451HdlGenerator extends AbstractHdlGeneratorFactory {

  public Ttl7451HdlGenerator() {
    super();
    myPorts
        .add(Port.INPUT, "A1", 1, 0)
        .add(Port.INPUT, "B1", 1, 9)
        .add(Port.INPUT, "C1", 1, 7)
        .add(Port.INPUT, "D1", 1, 8)
        .add(Port.INPUT, "A2", 1, 1)
        .add(Port.INPUT, "B2", 1, 2)
        .add(Port.INPUT, "C2", 1, 3)
        .add(Port.INPUT, "D2", 1, 4)
        .add(Port.OUTPUT, "Y1", 1, 6)
        .add(Port.OUTPUT, "Y2", 1, 5);
  }

  @Override
  public LineBuffer getModuleFunctionality(Netlist nets, AttributeSet attrs) {
    return LineBuffer.getHdlBuffer()
        .add("{{assign}}Y1{{=}}{{not}}((A1{{and}}B1){{or}}(C1{{and}}D1));")
        .add("{{assign}}Y2{{=}}{{not}}((A2{{and}}B2){{or}}(C2{{and}}D2));");
  }

  @Override
  public boolean isHdlSupportedTarget(AttributeSet attrs) {
    /* TODO: Add support for the ones with VCC and Ground Pin */
    if (attrs == null) return false;
    return (!attrs.getValue(TtlLibrary.VCC_GND));
  }
}
