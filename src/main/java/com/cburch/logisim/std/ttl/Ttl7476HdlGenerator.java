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

public class Ttl7476HdlGenerator extends AbstractHdlGeneratorFactory {

   public Ttl7476HdlGenerator() {
      super();
      myWires
            .addRegister("state1", 1)
            .addRegister("state2", 1)
            .addWire("next1", 1)
            .addWire("next2", 1);

      // Physical pin indices here match the `Ttl7476` logic port order array index
      // Note: This maps to the logic indices (0-13), avoiding the VCC/GND physics
      // mapping
      myPorts
            .add(Port.CLOCK, HdlPorts.getClockName(1), 1, 0)
            .add(Port.INPUT, "nPRE1", 1, 1)
            .add(Port.INPUT, "nCLR1", 1, 2)
            .add(Port.INPUT, "J1", 1, 3)
            .add(Port.CLOCK, HdlPorts.getClockName(2), 2, 4)
            .add(Port.INPUT, "nPRE2", 1, 5)
            .add(Port.INPUT, "nCLR2", 1, 6)
            .add(Port.INPUT, "J2", 1, 7)
            .add(Port.OUTPUT, "nQ2", 1, 8)
            .add(Port.OUTPUT, "Q2", 1, 9)
            .add(Port.INPUT, "K2", 1, 10)
            .add(Port.OUTPUT, "nQ1", 1, 11)
            .add(Port.OUTPUT, "Q1", 1, 12)
            .add(Port.INPUT, "K1", 1, 13);
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
                Q2  <= state2;
                nQ2 <= {{not}}(state2);

                next1 <= (J1 {{and}} {{not}}(state1)) {{or}} ({{not}}(K1) {{and}} state1);
                next2 <= (J2 {{and}} {{not}}(state2)) {{or}} ({{not}}(K2) {{and}} state2);

                ff1 : {{process}} ( {{CLK1}} , nCLR1 , nPRE1 ) {{is}}
                   BEGIN
                      {{if}} (nCLR1 = '0') {{then}} state1 <= '0';
                      {{elsif}} (nPRE1 = '0') {{then}} state1 <= '1';
                      {{elsif}} (falling_edge({{CLK1}})) {{then}}
                         {{if}} ({{tick1}}='1') {{then}} state1 <= next1; {{end}} {{if}};
                      {{end}} {{if}};
                   {{end}} {{process}} ff1;

                ff2 : {{process}} ( {{CLK2}} , nCLR2 , nPRE2 ) {{is}}
                   BEGIN
                      {{if}} (nCLR2 = '0') {{then}} state2 <= '0';
                      {{elsif}} (nPRE2 = '0') {{then}} state2 <= '1';
                      {{elsif}} (falling_edge({{CLK2}})) {{then}}
                         {{if}} ({{tick2}}='1') {{then}} state2 <= next2; {{end}} {{if}};
                      {{end}} {{if}};
                   {{end}} {{process}} ff2;
               """);
      } else {
         contents.add("""
               assign Q1    = state1;
               assign nQ1   = ~state1;
               assign Q2    = state2;
               assign nQ2   = ~state2;

               assign next1 = (J1 & ~state1) | (~K1 & state1);
               assign next2 = (J2 & ~state2) | (~K2 & state2);

               always @(negedge {{CLK1}} or negedge nCLR1 or negedge nPRE1)
               begin
                  if (nCLR1 == 0) state1 <= 0;
                  else if (nPRE1 == 0) state1 <= 1;
                  else if ({{tick1}} == 1) state1 <= next1;
               end

               always @(negedge {{CLK2}} or negedge nCLR2 or negedge nPRE2)
               begin
                  if (nCLR2 == 0) state2 <= 0;
                  else if (nPRE2 == 0) state2 <= 1;
                  else if ({{tick2}} == 1) state2 <= next2;
               end
               """);
      }
      return contents.empty();
   }

   @Override
   public boolean isHdlSupportedTarget(AttributeSet attrs) {
      if (attrs == null)
         return false;
      return (!attrs.getValue(TtlLibrary.VCC_GND));
   }
}