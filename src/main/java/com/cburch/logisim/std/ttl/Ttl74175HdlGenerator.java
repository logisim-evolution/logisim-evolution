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

public class Ttl74175HdlGenerator extends AbstractHdlGeneratorFactory {

  public Ttl74175HdlGenerator() {
    super();
    myWires
        .addWire("curState", 4)
        .addWire("nextState", 4);
    myPorts
        .add(Port.CLOCK, HdlPorts.CLOCK, 1, 7)
        .add(Port.INPUT, "nCLR", 1, 0)
        .add(Port.INPUT, "D1", 1, 3)
        .add(Port.INPUT, "D2", 1, 4)
        .add(Port.INPUT, "D3", 1, 10)
        .add(Port.INPUT, "D4", 1, 11)
        .add(Port.OUTPUT, "nQ1", 1, 2)
        .add(Port.OUTPUT, "Q1", 1, 1)
        .add(Port.OUTPUT, "nQ2", 1, 5)
        .add(Port.OUTPUT, "Q2", 1, 6)
        .add(Port.OUTPUT, "nQ3", 1, 9)
        .add(Port.OUTPUT, "Q3", 1, 8)
        .add(Port.OUTPUT, "nQ4", 1, 12)
        .add(Port.OUTPUT, "Q4", 1, 13);
  }

  @Override
  public LineBuffer getModuleFunctionality(Netlist TheNetlist, AttributeSet attrs) {
    final var contents = LineBuffer.getHdlBuffer()
        .pair("CLK", HdlPorts.CLOCK)
        .pair("tick", HdlPorts.TICK);
    if (Hdl.isVhdl()) {
      contents.empty().addVhdlKeywords().add("""
            nextState <= curState {{when}} {{tick}} = '0' {{else}}
                         D4&D3&D2&D1;

            dffs : {{process}}({{CLK}}, nCLR) {{is}}
               {{begin}}
                  {{if}} (nCLR = '0') {{then}} curState <= "0000";
                  {{elsif}} (rising_edge({{CLK}})) {{then}}
                     curState <= nextState;
                  {{end}} {{if}};
               {{end}} {{process}} dffs;

            nQ1 <= {{not}}(curState(0));
            Q1  <= curState(0);
            nQ2 <= {{not}}(curState(1));
            Q2  <= curState(1);
            nQ3 <= {{not}}(curState(2));
            Q3  <= curState(2);
            nQ4 <= {{not}}(curState(3));
            Q4  <= curState(3);")
            """);
    } else {
      contents.add("""
          assign nextState = tick == 0 ? curState : {D4, D3, D2, D1};
          assign nQ1       = ~curState[0];
          assign Q1        = curState[0];
          assign nQ2       = ~curState[1];
          assign Q2        = curState[1];
          assign nQ3       = ~curState[2];
          assign Q3        = curState[2];
          assign nQ4       = ~curState[3];
          assign Q4        = curState[3];

          always @(posedge {{CLK}} or negedge nCLR)
          begin
             if (~nCLR) curState <= 0;
             else curState <= nextState;
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
