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

public class Ttl7458HdlGenerator extends AbstractHdlGeneratorFactory {

  public Ttl7458HdlGenerator() {
    super();
    myPorts
        .add(Port.INPUT, "A0", 1, 1)
        .add(Port.INPUT, "B0", 1, 2)
        .add(Port.INPUT, "C0", 1, 3)
        .add(Port.INPUT, "D0", 1, 4)
        .add(Port.INPUT, "A1", 1, 0)
        .add(Port.INPUT, "B1", 1, 11)
        .add(Port.INPUT, "C1", 1, 10)
        .add(Port.INPUT, "D1", 1, 9)
        .add(Port.INPUT, "E1", 1, 8)
        .add(Port.INPUT, "F1", 1, 7)
        .add(Port.OUTPUT, "Y0", 1, 5)
        .add(Port.OUTPUT, "Y1", 1, 6);
  }

  @Override
  public LineBuffer getModuleFunctionality(Netlist TheNetlist, AttributeSet attrs) {
    return LineBuffer.getHdlBuffer()
        .add("{{assign}}Y0{{=}}(A0{{and}}B0){{or}}(C0{{and}}D0);")
        .add("{{assign}}Y1{{=}}(A1{{and}}B1{{and}}C1){{or}}(D1{{and}}E1{{and}}F1);");
  }

  @Override
  public boolean isHdlSupportedTarget(AttributeSet attrs) {
    /* TODO: Add support for the ones with VCC and Ground Pin */
    if (attrs == null) return false;
    return (!attrs.getValue(TtlLibrary.VCC_GND));
  }
}
