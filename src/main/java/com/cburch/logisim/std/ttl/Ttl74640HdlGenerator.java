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

/**
 * VHDL and Verilog for one 74HC640. Each channel is a three-state inverter. The two directions
 * cannot both be active, so the buses are not driven against each other.
 */
public class Ttl74640HdlGenerator extends AbstractHdlGeneratorFactory {

  /** Creates a generator for the inverting octal transceiver. */
  public Ttl74640HdlGenerator() {
    super();
    myPorts
        .add(Port.INPUT, "DIR", 1, Ttl74640.PORT_INDEX_DIR)
        .add(Port.INPUT, "nOE", 1, Ttl74640.PORT_INDEX_nOE)
        .add(Port.INOUT, "A1", 1, Ttl74640.PORT_INDEX_A1)
        .add(Port.INOUT, "A2", 1, Ttl74640.PORT_INDEX_A2)
        .add(Port.INOUT, "A3", 1, Ttl74640.PORT_INDEX_A3)
        .add(Port.INOUT, "A4", 1, Ttl74640.PORT_INDEX_A4)
        .add(Port.INOUT, "A5", 1, Ttl74640.PORT_INDEX_A5)
        .add(Port.INOUT, "A6", 1, Ttl74640.PORT_INDEX_A6)
        .add(Port.INOUT, "A7", 1, Ttl74640.PORT_INDEX_A7)
        .add(Port.INOUT, "A8", 1, Ttl74640.PORT_INDEX_A8)
        .add(Port.INOUT, "B1", 1, Ttl74640.PORT_INDEX_B1)
        .add(Port.INOUT, "B2", 1, Ttl74640.PORT_INDEX_B2)
        .add(Port.INOUT, "B3", 1, Ttl74640.PORT_INDEX_B3)
        .add(Port.INOUT, "B4", 1, Ttl74640.PORT_INDEX_B4)
        .add(Port.INOUT, "B5", 1, Ttl74640.PORT_INDEX_B5)
        .add(Port.INOUT, "B6", 1, Ttl74640.PORT_INDEX_B6)
        .add(Port.INOUT, "B7", 1, Ttl74640.PORT_INDEX_B7)
        .add(Port.INOUT, "B8", 1, Ttl74640.PORT_INDEX_B8);
  }

  @Override
  public LineBuffer getModuleFunctionality(Netlist theNetlist, AttributeSet attrs) {
    final var contents = LineBuffer.getHdlBuffer();
    if (Hdl.isVhdl()) {
      contents.empty().addVhdlKeywords().add("""
          B1 <= {{not}} A1 {{when}} nOE = '0' {{and}} DIR = '1' {{else}} 'Z';
          B2 <= {{not}} A2 {{when}} nOE = '0' {{and}} DIR = '1' {{else}} 'Z';
          B3 <= {{not}} A3 {{when}} nOE = '0' {{and}} DIR = '1' {{else}} 'Z';
          B4 <= {{not}} A4 {{when}} nOE = '0' {{and}} DIR = '1' {{else}} 'Z';
          B5 <= {{not}} A5 {{when}} nOE = '0' {{and}} DIR = '1' {{else}} 'Z';
          B6 <= {{not}} A6 {{when}} nOE = '0' {{and}} DIR = '1' {{else}} 'Z';
          B7 <= {{not}} A7 {{when}} nOE = '0' {{and}} DIR = '1' {{else}} 'Z';
          B8 <= {{not}} A8 {{when}} nOE = '0' {{and}} DIR = '1' {{else}} 'Z';
          A1 <= {{not}} B1 {{when}} nOE = '0' {{and}} DIR = '0' {{else}} 'Z';
          A2 <= {{not}} B2 {{when}} nOE = '0' {{and}} DIR = '0' {{else}} 'Z';
          A3 <= {{not}} B3 {{when}} nOE = '0' {{and}} DIR = '0' {{else}} 'Z';
          A4 <= {{not}} B4 {{when}} nOE = '0' {{and}} DIR = '0' {{else}} 'Z';
          A5 <= {{not}} B5 {{when}} nOE = '0' {{and}} DIR = '0' {{else}} 'Z';
          A6 <= {{not}} B6 {{when}} nOE = '0' {{and}} DIR = '0' {{else}} 'Z';
          A7 <= {{not}} B7 {{when}} nOE = '0' {{and}} DIR = '0' {{else}} 'Z';
          A8 <= {{not}} B8 {{when}} nOE = '0' {{and}} DIR = '0' {{else}} 'Z';
          """);
    } else {
      contents.add("""
          assign B1 = (nOE == 0 && DIR == 1) ? ~A1 : 1'bz;
          assign B2 = (nOE == 0 && DIR == 1) ? ~A2 : 1'bz;
          assign B3 = (nOE == 0 && DIR == 1) ? ~A3 : 1'bz;
          assign B4 = (nOE == 0 && DIR == 1) ? ~A4 : 1'bz;
          assign B5 = (nOE == 0 && DIR == 1) ? ~A5 : 1'bz;
          assign B6 = (nOE == 0 && DIR == 1) ? ~A6 : 1'bz;
          assign B7 = (nOE == 0 && DIR == 1) ? ~A7 : 1'bz;
          assign B8 = (nOE == 0 && DIR == 1) ? ~A8 : 1'bz;
          assign A1 = (nOE == 0 && DIR == 0) ? ~B1 : 1'bz;
          assign A2 = (nOE == 0 && DIR == 0) ? ~B2 : 1'bz;
          assign A3 = (nOE == 0 && DIR == 0) ? ~B3 : 1'bz;
          assign A4 = (nOE == 0 && DIR == 0) ? ~B4 : 1'bz;
          assign A5 = (nOE == 0 && DIR == 0) ? ~B5 : 1'bz;
          assign A6 = (nOE == 0 && DIR == 0) ? ~B6 : 1'bz;
          assign A7 = (nOE == 0 && DIR == 0) ? ~B7 : 1'bz;
          assign A8 = (nOE == 0 && DIR == 0) ? ~B8 : 1'bz;
          """);
    }
    return contents;
  }

  @Override
  public boolean isHdlSupportedTarget(AttributeSet attrs) {
    /* TODO: Add support for the ones with VCC and Ground Pin */
    if (attrs == null) return false;
    return (!attrs.getValue(TtlLibrary.VCC_GND));
  }
}
