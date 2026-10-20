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

public class Ttl7410HdlGenerator extends AbstractHdlGeneratorFactory {

  private final boolean Inverted;
  private final boolean andgate;

  public Ttl7410HdlGenerator() {
    this(true, true);
  }

  public Ttl7410HdlGenerator(boolean invert, boolean IsAnd) {
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
  public LineBuffer getModuleFunctionality(Netlist TheNetlist, AttributeSet attrs) {
    final var Inv = Inverted ? Hdl.notOperator() : "";
    final var Func = andgate ? Hdl.andOperator() : Hdl.orOperator();
    return LineBuffer.getHdlBuffer()
        .add("{{assign}}Y0{{=}}{{1}}(A0{{2}}B0{{2}}C0);", Inv, Func)
        .add("{{assign}}Y1{{=}}{{1}}(A1{{2}}B1{{2}}C1);", Inv, Func)
        .add("{{assign}}Y2{{=}}{{1}}(A2{{2}}B2{{2}}C2);", Inv, Func);
  }

  @Override
  public boolean isHdlSupportedTarget(AttributeSet attrs) {
    /* TODO: Add support for the ones with VCC and Ground Pin */
    if (attrs == null) return false;
    return (!attrs.getValue(TtlLibrary.VCC_GND));
  }
}
