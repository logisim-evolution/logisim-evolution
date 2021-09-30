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

public class Ttl7413HDLGenerator extends AbstractHdlGeneratorFactory {

  private final boolean inverted;

  public Ttl7413HDLGenerator() {
    this(true);
  }

  public Ttl7413HDLGenerator(boolean inv) {
    super();
    inverted = inv;
    myPorts
        .add(Port.INPUT, "A0", 1, 0)
        .add(Port.INPUT, "B0", 1, 1)
        .add(Port.INPUT, "C0", 1, 2)
        .add(Port.INPUT, "D0", 1, 3)
        .add(Port.INPUT, "A1", 1, 9)
        .add(Port.INPUT, "B1", 1, 8)
        .add(Port.INPUT, "C1", 1, 7)
        .add(Port.INPUT, "D1", 1, 6)
        .add(Port.OUTPUT, "Y0", 1, 4)
        .add(Port.OUTPUT, "Y1", 1, 5);
  }

  @Override
  public ArrayList<String> getModuleFunctionality(Netlist theNetlist, AttributeSet attrs) {
    final var contents = new ArrayList<String>();
    final var Inv = inverted ? Hdl.notOperator() : "";
    contents.add("   " + Hdl.assignPreamble() + "Y0" + Hdl.assignOperator() + Inv
            + " (A0" + Hdl.andOperator() + "B0" + Hdl.andOperator() + "C0" + Hdl.andOperator() + "D0);");
    contents.add("   " + Hdl.assignPreamble() + "Y1" + Hdl.assignOperator() + Inv
            + " (A1" + Hdl.andOperator() + "B1" + Hdl.andOperator() + "C1" + Hdl.andOperator() + "D1);");
    return contents;
  }

  @Override
  public boolean isHdlSupportedTarget(AttributeSet attrs) {
    /* TODO: Add support for the ones with VCC and Ground Pin */
    if (attrs == null) return false;
    return (!attrs.getValue(TtlLibrary.VCC_GND));
  }
}
