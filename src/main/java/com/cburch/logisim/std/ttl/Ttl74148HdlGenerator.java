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

public class Ttl74148HdlGenerator extends AbstractHdlGeneratorFactory {

  public Ttl74148HdlGenerator() {
    super();
    myWires
        .addWire("s_priority", 3)
        .addWire("s_code", 3)
        .addWire("s_noInput", 1);
    // Unconnected inputs are pulled to one, as every input of this device is active low.
    myPorts
        .add(Port.INPUT, "nI0", 1, 8, false)
        .add(Port.INPUT, "nI1", 1, 9, false)
        .add(Port.INPUT, "nI2", 1, 10, false)
        .add(Port.INPUT, "nI3", 1, 11, false)
        .add(Port.INPUT, "nI4", 1, 0, false)
        .add(Port.INPUT, "nI5", 1, 1, false)
        .add(Port.INPUT, "nI6", 1, 2, false)
        .add(Port.INPUT, "nI7", 1, 3, false)
        .add(Port.INPUT, "nEI", 1, 4, false)
        .add(Port.OUTPUT, "nA0", 1, 7)
        .add(Port.OUTPUT, "nA1", 1, 6)
        .add(Port.OUTPUT, "nA2", 1, 5)
        .add(Port.OUTPUT, "nGS", 1, 12)
        .add(Port.OUTPUT, "nEO", 1, 13);
  }

  @Override
  public LineBuffer getModuleFunctionality(Netlist nets, AttributeSet attrs) {
    final var contents = LineBuffer.getHdlBuffer();
    if (Hdl.isVhdl()) {
      contents.addVhdlKeywords().add("""
          nA2 <= s_code(2);
          nA1 <= s_code(1);
          nA0 <= s_code(0);
          nGS <= nEI {{or}} s_noInput;
          nEO <= nEI {{or}} ({{not}} s_noInput);

          s_noInput <= nI0 {{and}} nI1 {{and}} nI2 {{and}} nI3 {{and}} nI4 {{and}} nI5 {{and}} nI6 {{and}} nI7;
          s_code    <= s_priority {{when}} nEI = '0' {{else}} "111";

          s_priority <= "000" {{when}} nI7 = '0' {{else}}
                        "001" {{when}} nI6 = '0' {{else}}
                        "010" {{when}} nI5 = '0' {{else}}
                        "011" {{when}} nI4 = '0' {{else}}
                        "100" {{when}} nI3 = '0' {{else}}
                        "101" {{when}} nI2 = '0' {{else}}
                        "110" {{when}} nI1 = '0' {{else}}
                        "111";
          """);
    } else {
      contents.add("""
          assign nA2 = s_code[2];
          assign nA1 = s_code[1];
          assign nA0 = s_code[0];
          assign nGS = nEI | s_noInput;
          assign nEO = nEI | ~s_noInput;

          assign s_noInput = nI0 & nI1 & nI2 & nI3 & nI4 & nI5 & nI6 & nI7;
          assign s_code    = (nEI == 0) ? s_priority : 3'b111;

          assign s_priority = (nI7 == 0) ? 3'b000 :
                              (nI6 == 0) ? 3'b001 :
                              (nI5 == 0) ? 3'b010 :
                              (nI4 == 0) ? 3'b011 :
                              (nI3 == 0) ? 3'b100 :
                              (nI2 == 0) ? 3'b101 :
                              (nI1 == 0) ? 3'b110 :
                                           3'b111;
          """);
    }
    return contents.empty();
  }

  @Override
  public boolean isHdlSupportedTarget(AttributeSet attrs) {
    /* TODO: Add support for the ones with VCC and Ground Pin */
    if (attrs == null) return false;
    return (!attrs.getValue(TtlLibrary.VCC_GND));
  }
}
