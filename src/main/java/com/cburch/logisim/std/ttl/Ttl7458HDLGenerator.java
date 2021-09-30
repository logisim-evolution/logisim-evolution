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

import java.util.ArrayList;

public class Ttl7458HDLGenerator extends AbstractHdlGeneratorFactory {

  public Ttl7458HDLGenerator() {
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
  public ArrayList<String> getModuleFunctionality(Netlist TheNetlist, AttributeSet attrs) {
    final var contents = new ArrayList<String>();
    contents.add("   " + Hdl.assignPreamble() + "Y0" + Hdl.assignOperator() + "(A0" + Hdl.andOperator() + "B0)"
            + Hdl.orOperator() + "(C0" + Hdl.andOperator() + "D0);");
    contents.add("   " + Hdl.assignPreamble() + "Y1" + Hdl.assignOperator() + "(A1" + Hdl.andOperator() + "B1"
            + Hdl.andOperator() + "C1)" + Hdl.orOperator() + "(D1" + Hdl.andOperator() + "E1" + Hdl.andOperator() + "F1);");
    return contents;
  }

  @Override
  public boolean isHdlSupportedTarget(AttributeSet attrs) {
    /* TODO: Add support for the ones with VCC and Ground Pin */
    if (attrs == null) return false;
    return (!attrs.getValue(TtlLibrary.VCC_GND));
  }
}
