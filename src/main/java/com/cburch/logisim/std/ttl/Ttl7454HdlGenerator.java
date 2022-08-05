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

public class Ttl7454HdlGenerator extends AbstractHdlGeneratorFactory {

  public Ttl7454HdlGenerator() {
    super();
    myPorts
        .add(Port.INPUT, "A", 1, 0)
        .add(Port.INPUT, "B", 1, 8)
        .add(Port.INPUT, "C", 1, 1)
        .add(Port.INPUT, "D", 1, 2)
        .add(Port.INPUT, "E", 1, 3)
        .add(Port.INPUT, "F", 1, 4)
        .add(Port.INPUT, "G", 1, 6)
        .add(Port.INPUT, "H", 1, 7)
        .add(Port.OUTPUT, "Y", 1, 5);
  }

  @Override
  public LineBuffer getModuleFunctionality(Netlist TheNetlist, AttributeSet attrs) {
    return LineBuffer.getHdlBuffer()
        .add(
            "{{assign}}Y{{=}}{{not}}((A{{and}}B){{or}}(C{{and}}D){{or}}(E{{and}}F){{or}}(G{{and}}H));");
  }

  @Override
  public boolean isHdlSupportedTarget(AttributeSet attrs) {
    /* TODO: Add support for the ones with VCC and Ground Pin */
    if (attrs == null) return false;
    return (!attrs.getValue(TtlLibrary.VCC_GND));
  }
}
