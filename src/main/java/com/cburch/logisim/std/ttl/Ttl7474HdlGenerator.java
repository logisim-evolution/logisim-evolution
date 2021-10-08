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

public class Ttl7474HdlGenerator extends AbstractHdlGeneratorFactory {

  public Ttl7474HdlGenerator() {
    super();
    myWires
        .addWire("state1", 1)
        .addWire("state2", 1)
        .addWire("next1", 1)
        .addWire("next2", 1);
    myPorts
        .add(Port.CLOCK, HdlPorts.getClockName(1), 1, 2)
        .add(Port.CLOCK, HdlPorts.getClockName(2), 2, 9)
        .add(Port.INPUT, "nCLR1", 1, 0)
        .add(Port.INPUT, "D1", 1, 1)
        .add(Port.INPUT, "nPRE1", 1, 3)
        .add(Port.INPUT, "nCLR2", 1, 11)
        .add(Port.INPUT, "D2", 1, 10)
        .add(Port.INPUT, "nPRE2", 1, 8)
        .add(Port.OUTPUT, "Q1", 1, 4)
        .add(Port.OUTPUT, "nQ1", 1, 5)
        .add(Port.OUTPUT, "Q2", 1, 7)
        .add(Port.OUTPUT, "nQ2", 1, 6);
  }

  @Override
  public LineBuffer getModuleFunctionality(Netlist theNetlist, AttributeSet attrs) {
    final var contents = LineBuffer.getHdlBuffer()
        .pair("CLK1", HdlPorts.getClockName(1))
        .pair("CLK2", HdlPorts.getClockName(2))
        .pair("tick1", HdlPorts.getTickName(1))
        .pair("tick2", HdlPorts.getTickName(2));
    if (Hdl.isVhdl()) {
      contents.empty().addVhdlKeywords().add("""
            Q1  <= state1;
            nQ1 <= {{not}}(state1);
            Q2  <= state1;
            nQ2 <= {{not}}(state1);

            next1 <= D1 {{when}} {{tick1}}='1' {{else}} state1;
            next2 <= D2 {{when}} {{tick2}}='1' {{else}} state2;

            ff1 : {{process}} ( {{CLK1}} , nCLR1 , nPRE1 ) {{is}}
               BEGIN
                  {{if}} (nCLR1 = '0') {{then}} state1 <= '0';
                  {{elsif}} (nPRE1 = '0') {{then}} state1 <= '1';
                  {{elsif}} (rising_edge({{CLK1}})) {{then}} state1 <= next1;
                  {{end}} {{if}};
               {{end}} {{process}} ff1;

            ff2 : {{process}} ( {{CLK2}} , nCLR2 , nPRE2 ) {{is}}
               BEGIN
                  {{if}} (nCLR2 = '0') {{then}} state2 <= '0';
                  {{elsif}} (nPRE2 = '0') {{then}} state2 <= '1';
                  {{elsif}} (rising_edge({{CLK2}})) {{then}} state2 <= next2;
                  {{end}} {{if}};
               {{end}} {{process}} ff2;
           """);
    } else {
      contents.add("""
          assign Q1    = state1;
          assign nQ1   = ~state1;
          assign Q2    = state2;
          assign nQ2   = ~state2;
          assign next1 = tick1 == 1 ? D1 : state1;
          assign next2 = tick2 == 1 ? D2 : state2;

          always @(posedge {{CLK1}} or negedge nCLR1 or negedge nPRE1)
          begin
             if (nCLR1 == 0) state1 <= 0;
             else if (nPRE1 == 0) state1 <= 1;
             else state1 <= next1;
          end

          always @(posedge {{CLK2}} or negedge nCLR2 or negedge nPRE2)
          begin
             if (nCLR2 == 0) state2 <= 0;
             else if (nPRE2 == 0) state2 <= 1;
             else state2 <= next2;
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
