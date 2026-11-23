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
import com.cburch.logisim.fpga.hdlgenerator.Hdl;
import com.cburch.logisim.instance.Port;
import com.cburch.logisim.util.LineBuffer;

/** HDL generator for the 744075 triple 3-input OR gate. */
public class Ttl744075HdlGenerator extends AbstractHdlGeneratorFactory {

  public Ttl744075HdlGenerator() {
    super();
    myPorts
        .add(Port.INPUT, "A1", 1, Ttl744075.PORT_INDEX_1A)
        .add(Port.INPUT, "B1", 1, Ttl744075.PORT_INDEX_1B)
        .add(Port.INPUT, "C1", 1, Ttl744075.PORT_INDEX_1C)
        .add(Port.OUTPUT, "Y1", 1, Ttl744075.PORT_INDEX_1Y)
        .add(Port.INPUT, "A2", 1, Ttl744075.PORT_INDEX_2A)
        .add(Port.INPUT, "B2", 1, Ttl744075.PORT_INDEX_2B)
        .add(Port.INPUT, "C2", 1, Ttl744075.PORT_INDEX_2C)
        .add(Port.OUTPUT, "Y2", 1, Ttl744075.PORT_INDEX_2Y)
        .add(Port.INPUT, "A3", 1, Ttl744075.PORT_INDEX_3A)
        .add(Port.INPUT, "B3", 1, Ttl744075.PORT_INDEX_3B)
        .add(Port.INPUT, "C3", 1, Ttl744075.PORT_INDEX_3C)
        .add(Port.OUTPUT, "Y3", 1, Ttl744075.PORT_INDEX_3Y);
  }

  @Override
  public LineBuffer getModuleFunctionality(Netlist theNetlist, AttributeSet attrs) {
    final var or = Hdl.orOperator();
    return LineBuffer.getHdlBuffer()
        .add("{{assign}}Y1{{=}}(A1{{1}}B1{{1}}C1);", or)
        .add("{{assign}}Y2{{=}}(A2{{1}}B2{{1}}C2);", or)
        .add("{{assign}}Y3{{=}}(A3{{1}}B3{{1}}C3);", or);
  }

  @Override
  public boolean isHdlSupportedTarget(AttributeSet attrs) {
    if (attrs == null) return false;
    return !attrs.getValue(TtlLibrary.VCC_GND);
  }
}
