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

/**
 * VHDL and Verilog for one 74HC393. Each half counts on the falling edge of its own clock and
 * clears asynchronously while its master reset is high.
 */
public class Ttl74393HdlGenerator extends AbstractHdlGeneratorFactory {

  /** Creates a generator for the dual 4-bit ripple counter. */
  public Ttl74393HdlGenerator() {
    super();
    myWires
        .addRegister("state1", 4)
        .addRegister("state2", 4);
    // The width of a clock port selects its tick name, as in Ttl7476: 1 is tick, 2 is tick2.
    myPorts
        .add(Port.CLOCK, HdlPorts.getClockName(1), 1, Ttl74393.PORT_INDEX_1CP)
        .add(Port.INPUT, "MR1", 1, Ttl74393.PORT_INDEX_1MR)
        .add(Port.OUTPUT, "Q1_0", 1, Ttl74393.PORT_INDEX_1Q0)
        .add(Port.OUTPUT, "Q1_1", 1, Ttl74393.PORT_INDEX_1Q1)
        .add(Port.OUTPUT, "Q1_2", 1, Ttl74393.PORT_INDEX_1Q2)
        .add(Port.OUTPUT, "Q1_3", 1, Ttl74393.PORT_INDEX_1Q3)
        .add(Port.OUTPUT, "Q2_3", 1, Ttl74393.PORT_INDEX_2Q3)
        .add(Port.OUTPUT, "Q2_2", 1, Ttl74393.PORT_INDEX_2Q2)
        .add(Port.OUTPUT, "Q2_1", 1, Ttl74393.PORT_INDEX_2Q1)
        .add(Port.OUTPUT, "Q2_0", 1, Ttl74393.PORT_INDEX_2Q0)
        .add(Port.INPUT, "MR2", 1, Ttl74393.PORT_INDEX_2MR)
        .add(Port.CLOCK, HdlPorts.getClockName(2), 2, Ttl74393.PORT_INDEX_2CP);
  }

  @Override
  public LineBuffer getModuleFunctionality(Netlist theNetlist, AttributeSet attrs) {
    final var contents =
        LineBuffer.getHdlBuffer()
            .pair("CLK1", HdlPorts.getClockName(1))
            .pair("CLK2", HdlPorts.getClockName(2))
            .pair("tick1", HdlPorts.getTickName(1))
            .pair("tick2", HdlPorts.getTickName(2));
    if (Hdl.isVhdl()) {
      contents.addVhdlKeywords().add("""
          Q1_0 <= state1(0);
          Q1_1 <= state1(1);
          Q1_2 <= state1(2);
          Q1_3 <= state1(3);
          Q2_0 <= state2(0);
          Q2_1 <= state2(1);
          Q2_2 <= state2(2);
          Q2_3 <= state2(3);

          counter1 : {{process}}({{CLK1}}, MR1) {{is}}
             {{begin}}
                {{if}} (MR1 = '1') {{then}} state1 <= "0000";
                {{elsif}} (falling_edge({{CLK1}})) {{then}}
                   {{if}} ({{tick1}} = '1') {{then}} state1 <= std_logic_vector(unsigned(state1) + 1);
                   {{end}} {{if}};
                {{end}} {{if}};
             {{end}} {{process}} counter1;

          counter2 : {{process}}({{CLK2}}, MR2) {{is}}
             {{begin}}
                {{if}} (MR2 = '1') {{then}} state2 <= "0000";
                {{elsif}} (falling_edge({{CLK2}})) {{then}}
                   {{if}} ({{tick2}} = '1') {{then}} state2 <= std_logic_vector(unsigned(state2) + 1);
                   {{end}} {{if}};
                {{end}} {{if}};
             {{end}} {{process}} counter2;
          """);
    } else {
      contents.add("""
          assign Q1_0 = state1[0];
          assign Q1_1 = state1[1];
          assign Q1_2 = state1[2];
          assign Q1_3 = state1[3];
          assign Q2_0 = state2[0];
          assign Q2_1 = state2[1];
          assign Q2_2 = state2[2];
          assign Q2_3 = state2[3];

          always @(negedge {{CLK1}} or posedge MR1)
          begin
             if (MR1 == 1) state1 <= 0;
             else if ({{tick1}} == 1) state1 <= state1 + 1;
          end

          always @(negedge {{CLK2}} or posedge MR2)
          begin
             if (MR2 == 1) state2 <= 0;
             else if ({{tick2}} == 1) state2 <= state2 + 1;
          end
          """);
    }
    return contents;
  }

  @Override
  public boolean isHdlSupportedTarget(AttributeSet attrs) {
    if (attrs == null) return false;
    return (!attrs.getValue(TtlLibrary.VCC_GND));
  }
}
