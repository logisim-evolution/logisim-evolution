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

/** VHDL and Verilog for the two negative-edge J-K flip-flops of a 74107. */
public class Ttl74107HdlGenerator extends AbstractHdlGeneratorFactory {

  /** Creates an HDL generator for both halves of a 74107. */
  public Ttl74107HdlGenerator() {
    super();
    myWires
        .addRegister("state1", 1)
        .addRegister("state2", 1)
        .addWire("next1", 1)
        .addWire("next2", 1);
    // The bit count selects the tick name (tick versus tick2), as in Ttl7476HdlGenerator.
    myPorts
        .add(Port.INPUT, "J1", 1, Ttl74107.PORT_1J)
        .add(Port.OUTPUT, "nQ1", 1, Ttl74107.PORT_N1Q)
        .add(Port.OUTPUT, "Q1", 1, Ttl74107.PORT_1Q)
        .add(Port.INPUT, "K1", 1, Ttl74107.PORT_1K)
        .add(Port.OUTPUT, "Q2", 1, Ttl74107.PORT_2Q)
        .add(Port.OUTPUT, "nQ2", 1, Ttl74107.PORT_N2Q)
        .add(Port.INPUT, "J2", 1, Ttl74107.PORT_2J)
        .add(Port.CLOCK, HdlPorts.getClockName(2), 2, Ttl74107.PORT_2CP)
        .add(Port.INPUT, "nR2", 1, Ttl74107.PORT_N2R)
        .add(Port.INPUT, "K2", 1, Ttl74107.PORT_2K)
        .add(Port.CLOCK, HdlPorts.getClockName(1), 1, Ttl74107.PORT_1CP)
        .add(Port.INPUT, "nR1", 1, Ttl74107.PORT_N1R);
  }

  @Override
  public LineBuffer getModuleFunctionality(Netlist nets, AttributeSet attrs) {
    final var contents =
        LineBuffer.getHdlBuffer()
            .pair("CLK1", HdlPorts.getClockName(1))
            .pair("CLK2", HdlPorts.getClockName(2))
            .pair("tick1", HdlPorts.getTickName(1))
            .pair("tick2", HdlPorts.getTickName(2));
    if (Hdl.isVhdl()) {
      contents.addVhdlKeywords().add("""
          Q1  <= state1;
          nQ1 <= {{not}}(state1);
          Q2  <= state2;
          nQ2 <= {{not}}(state2);

          next1 <= (J1 {{and}} {{not}}(state1)) {{or}} ({{not}}(K1) {{and}} state1);
          next2 <= (J2 {{and}} {{not}}(state2)) {{or}} ({{not}}(K2) {{and}} state2);

          ff1 : {{process}}({{CLK1}}, nR1) {{is}}
          {{begin}}
             {{if}} (nR1 = '0') {{then}} state1 <= '0';
             {{elsif}} (falling_edge({{CLK1}})) {{then}}
                {{if}} ({{tick1}} = '1') {{then}} state1 <= next1; {{end}} {{if}};
             {{end}} {{if}};
          {{end}} {{process}} ff1;

          ff2 : {{process}}({{CLK2}}, nR2) {{is}}
          {{begin}}
             {{if}} (nR2 = '0') {{then}} state2 <= '0';
             {{elsif}} (falling_edge({{CLK2}})) {{then}}
                {{if}} ({{tick2}} = '1') {{then}} state2 <= next2; {{end}} {{if}};
             {{end}} {{if}};
          {{end}} {{process}} ff2;
          """);
    } else {
      contents.add("""
          assign Q1  = state1;
          assign nQ1 = ~state1;
          assign Q2  = state2;
          assign nQ2 = ~state2;

          assign next1 = (J1 & ~state1) | (~K1 & state1);
          assign next2 = (J2 & ~state2) | (~K2 & state2);

          always @(negedge {{CLK1}} or negedge nR1)
          begin
             if (nR1 == 0) state1 <= 0;
             else if ({{tick1}} == 1) state1 <= next1;
          end

          always @(negedge {{CLK2}} or negedge nR2)
          begin
             if (nR2 == 0) state2 <= 0;
             else if ({{tick2}} == 1) state2 <= next2;
          end
          """);
    }
    return contents.empty();
  }

  @Override
  public boolean isHdlSupportedTarget(AttributeSet attrs) {
    if (attrs == null) return false;
    return !attrs.getValue(TtlLibrary.VCC_GND);
  }
}
