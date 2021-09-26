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
import com.cburch.logisim.fpga.hdlgenerator.AbstractHDLGeneratorFactory;
import com.cburch.logisim.fpga.hdlgenerator.HDL;
import com.cburch.logisim.instance.Port;

import java.util.ArrayList;

public class Ttl7413HDLGenerator extends AbstractHDLGeneratorFactory {

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
  public ArrayList<String> GetModuleFunctionality(Netlist theNetlist, AttributeSet attrs) {
    final var contents = new ArrayList<String>();
    final var Inv = inverted ? HDL.notOperator() : "";
    contents.add("   " + HDL.assignPreamble() + "Y0" + HDL.assignOperator() + Inv
            + " (A0" + HDL.andOperator() + "B0" + HDL.andOperator() + "C0" + HDL.andOperator() + "D0);");
    contents.add("   " + HDL.assignPreamble() + "Y1" + HDL.assignOperator() + Inv
            + " (A1" + HDL.andOperator() + "B1" + HDL.andOperator() + "C1" + HDL.andOperator() + "D1);");
    return contents;
  }

  @Override
  public boolean isHDLSupportedTarget(AttributeSet attrs) {
    /* TODO: Add support for the ones with VCC and Ground Pin */
    if (attrs == null) return false;
    return (!attrs.getValue(TtlLibrary.VCC_GND));
  }
}
