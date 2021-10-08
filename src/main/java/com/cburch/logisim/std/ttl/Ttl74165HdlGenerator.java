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
import com.cburch.logisim.fpga.hdlgenerator.HdlPorts;
import com.cburch.logisim.instance.Port;
import com.cburch.logisim.util.LineBuffer;

public class Ttl74165HdlGenerator extends AbstractHdlGeneratorFactory {

  public Ttl74165HdlGenerator() {
    super();
    myWires
        .addWire("curState", 8)
        .addWire("nextState", 8)
        .addWire("parData", 8)
        .addWire("enable", 1);
    myPorts
        .add(Port.CLOCK, HdlPorts.CLOCK, 1, 1)
        .add(Port.INPUT, "SHnLD", 1, 0)
        .add(Port.INPUT, "CKIh", 1, 13)
        .add(Port.INPUT, "SER", 1, 8)
        .add(Port.INPUT, "P0", 1, 9)
        .add(Port.INPUT, "P1", 1, 10)
        .add(Port.INPUT, "P2", 1, 11)
        .add(Port.INPUT, "P3", 1, 12)
        .add(Port.INPUT, "P4", 1, 2)
        .add(Port.INPUT, "P5", 1, 3)
        .add(Port.INPUT, "P6", 1, 4)
        .add(Port.INPUT, "P7", 1, 5)
        .add(Port.OUTPUT, "Q7", 1, 6)
        .add(Port.OUTPUT, "Q7n", 1, 7);
  }

  @Override
  public LineBuffer getModuleFunctionality(Netlist TheNetlist, AttributeSet attrs) {
    final var contents = LineBuffer.getHdlBuffer()
        .pair("CK", HdlPorts.CLOCK)
        .pair("Tick", HdlPorts.TICK);
    if (Hdl.isVhdl()) {
      contents.empty().addVhdlKeywords().add("""
          Q7  <= curState(0);
          Q7n <= {{not}}(curState(0));

          enable  <= {{not}}(CKIh) {{and}} {{Tick}};
          parData <= P7&P6&P5&P4&P3&P2&P1&P0;

          nextState <= curState {{when}} enable = '0' {{else}}
                       parData {{when}} SHnLD = '0' {{else}}
                       SER&curState(7 DOWNTO 1);

          dffs : {{process}}({{CK}}) {{is}}
          {{begin}}
             {{if}} (rising_edge({{CK}})) {{then}} curState <= nextState;
             {{end}} {{if}};
          {{end}} {{process}} dffs;
          """);
    } else {
      contents.add("""
          assign Q7        = curState[0];
          assign Q7n       = ~curState[0];
          assign enable    = ~CKIh & {{tick}};
          assign parData   = {P7, P6, P5, P4, P3, P2, P1, P0};
          assign nextState = enable == 0 ? curState :
                             SHnLD == 0 ? parData : {SER, curState[7:1]};
          always @(posedge {{CLK}})
          begin
             curState = nextState;
          end
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
