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

public class Ttl7410HDLGenerator extends AbstractHDLGeneratorFactory {

  private final boolean Inverted;
  private final boolean andgate;

  public Ttl7410HDLGenerator() {
    this(true, true);
  }

  public Ttl7410HDLGenerator(boolean invert, boolean IsAnd) {
    super();
    Inverted = invert;
    andgate = IsAnd;
    myPorts
        .add(Port.INPUT, "A0", 1, 0)
        .add(Port.INPUT, "B0", 1, 1)
        .add(Port.INPUT, "C0", 1, 11)
        .add(Port.INPUT, "A1", 1, 2)
        .add(Port.INPUT, "B1", 1, 3)
        .add(Port.INPUT, "C1", 1, 4)
        .add(Port.INPUT, "A2", 1, 9)
        .add(Port.INPUT, "B2", 1, 8)
        .add(Port.INPUT, "C2", 1, 7)
        .add(Port.OUTPUT, "Y0", 1, 10)
        .add(Port.OUTPUT, "Y1", 1, 5)
        .add(Port.OUTPUT, "Y2", 1, 6);
  }

  @Override
  public ArrayList<String> getModuleFunctionality(Netlist TheNetlist, AttributeSet attrs) {
    final var contents = new ArrayList<String>();
    final var Inv = Inverted ? HDL.notOperator() : "";
    final var Func = andgate ? HDL.andOperator() : HDL.orOperator();
    contents.add("   " + HDL.assignPreamble() + "Y0" + HDL.assignOperator() + Inv + " (A0 " + Func + " B0 " + Func + " C0);");
    contents.add("   " + HDL.assignPreamble() + "Y1" + HDL.assignOperator() + Inv + " (A1 " + Func + " B1 " + Func + " C1);");
    contents.add("   " + HDL.assignPreamble() + "Y2" + HDL.assignOperator() + Inv + " (A2 " + Func + " B2 " + Func + " C2);");
    return contents;
  }

  @Override
  public boolean isHdlSupportedTarget(AttributeSet attrs) {
    /* TODO: Add support for the ones with VCC and Ground Pin */
    if (attrs == null) return false;
    return (!attrs.getValue(TtlLibrary.VCC_GND));
  }
}
