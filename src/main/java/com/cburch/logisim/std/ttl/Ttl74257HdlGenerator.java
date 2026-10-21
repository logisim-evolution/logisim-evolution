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

/** VHDL and Verilog generator for the 74x257 quad 2-line to 1-line data selector. */
public class Ttl74257HdlGenerator extends AbstractHdlGeneratorFactory {

  /** Creates an HDL generator for the four 3-state multiplexers. */
  public Ttl74257HdlGenerator() {
    super();
    myPorts
        .add(Port.INPUT, "S", 1, Ttl74257.pinNrToPortNr(Ttl74257.SELECT))
        .add(Port.INPUT, "I0_1", 1, Ttl74257.pinNrToPortNr(Ttl74257.L1_I0))
        .add(Port.INPUT, "I1_1", 1, Ttl74257.pinNrToPortNr(Ttl74257.L1_I1))
        .add(Port.OUTPUT, "Y1", 1, Ttl74257.pinNrToPortNr(Ttl74257.L1_Y))
        .add(Port.INPUT, "I0_2", 1, Ttl74257.pinNrToPortNr(Ttl74257.L2_I0))
        .add(Port.INPUT, "I1_2", 1, Ttl74257.pinNrToPortNr(Ttl74257.L2_I1))
        .add(Port.OUTPUT, "Y2", 1, Ttl74257.pinNrToPortNr(Ttl74257.L2_Y))
        .add(Port.OUTPUT, "Y3", 1, Ttl74257.pinNrToPortNr(Ttl74257.L3_Y))
        .add(Port.INPUT, "I1_3", 1, Ttl74257.pinNrToPortNr(Ttl74257.L3_I1))
        .add(Port.INPUT, "I0_3", 1, Ttl74257.pinNrToPortNr(Ttl74257.L3_I0))
        .add(Port.OUTPUT, "Y4", 1, Ttl74257.pinNrToPortNr(Ttl74257.L4_Y))
        .add(Port.INPUT, "I1_4", 1, Ttl74257.pinNrToPortNr(Ttl74257.L4_I1))
        .add(Port.INPUT, "I0_4", 1, Ttl74257.pinNrToPortNr(Ttl74257.L4_I0))
        .add(Port.INPUT, "nOE", 1, Ttl74257.pinNrToPortNr(Ttl74257.OE));
  }

  @Override
  public LineBuffer getModuleFunctionality(Netlist nets, AttributeSet attrs) {
    final var contents = LineBuffer.getHdlBuffer();
    if (Hdl.isVhdl()) {
      contents.addVhdlKeywords().add(
          """
          Y1 <= 'Z' {{when}} nOE = '1' {{else}} I1_1 {{when}} S = '1' {{else}} I0_1;
          Y2 <= 'Z' {{when}} nOE = '1' {{else}} I1_2 {{when}} S = '1' {{else}} I0_2;
          Y3 <= 'Z' {{when}} nOE = '1' {{else}} I1_3 {{when}} S = '1' {{else}} I0_3;
          Y4 <= 'Z' {{when}} nOE = '1' {{else}} I1_4 {{when}} S = '1' {{else}} I0_4;
          """);
    } else {
      contents.add(
          """
          assign Y1 = (nOE == 1) ? 1'bZ : ((S == 1) ? I1_1 : I0_1);
          assign Y2 = (nOE == 1) ? 1'bZ : ((S == 1) ? I1_2 : I0_2);
          assign Y3 = (nOE == 1) ? 1'bZ : ((S == 1) ? I1_3 : I0_3);
          assign Y4 = (nOE == 1) ? 1'bZ : ((S == 1) ? I1_4 : I0_4);
          """);
    }
    return contents.empty();
  }

  @Override
  public boolean isHdlSupportedTarget(AttributeSet attrs) {
    /* TODO: Add support for the ones with VCC and Ground Pin */
    if (attrs == null) {
      return false;
    }
    return (!attrs.getValue(TtlLibrary.VCC_GND));
  }
}
