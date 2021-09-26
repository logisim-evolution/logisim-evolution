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

public class Ttl7451HDLGenerator extends AbstractHDLGeneratorFactory {

  public Ttl7451HDLGenerator() {
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
  public ArrayList<String> GetModuleFunctionality(Netlist nets, AttributeSet attrs) {
    final var contents = new ArrayList<String>();
    contents.add("   " + HDL.assignPreamble() + "Y1" + HDL.assignOperator() + HDL.notOperator()
            + "((A1" + HDL.andOperator() + "B1)" + HDL.orOperator() + "(C1" + HDL.andOperator() + "D1));");
    contents.add("   " + HDL.assignPreamble() + "Y2" + HDL.assignOperator() + HDL.notOperator()
            + "((A2" + HDL.andOperator() + "B2)" + HDL.orOperator() + "(C2" + HDL.andOperator() + "D2));");
    return contents;
  }

  @Override
  public boolean isHDLSupportedTarget(AttributeSet attrs) {
    /* TODO: Add support for the ones with VCC and Ground Pin */
    if (attrs == null) return false;
    return (!attrs.getValue(TtlLibrary.VCC_GND));
  }
}
