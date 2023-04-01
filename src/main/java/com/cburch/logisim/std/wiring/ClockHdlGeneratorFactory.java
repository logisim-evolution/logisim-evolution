/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.std.wiring;

import com.cburch.logisim.comp.Component;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.fpga.designrulecheck.Netlist;
import com.cburch.logisim.fpga.designrulecheck.netlistComponent;
import com.cburch.logisim.fpga.hdlgenerator.AbstractHdlGeneratorFactory;
import com.cburch.logisim.fpga.hdlgenerator.Hdl;
import com.cburch.logisim.fpga.hdlgenerator.HdlGeneratorFactory;
import com.cburch.logisim.fpga.hdlgenerator.HdlParameters;
import com.cburch.logisim.fpga.hdlgenerator.SynthesizedClockHdlGeneratorFactory;
import com.cburch.logisim.fpga.hdlgenerator.TickComponentHdlGeneratorFactory;
import com.cburch.logisim.instance.Port;
import com.cburch.logisim.util.LineBuffer;
import java.util.SortedMap;
import java.util.TreeMap;

public class ClockHdlGeneratorFactory extends AbstractHdlGeneratorFactory {

  public static final int NR_OF_CLOCK_BITS = 5;
  public static final int DERIVED_CLOCK_INDEX = 0;
  public static final int INVERTED_DERIVED_CLOCK_INDEX = 1;
  public static final int POSITIVE_EDGE_TICK_INDEX = 2;
  public static final int NEGATIVE_EDGE_TICK_INDEX = 3;
  public static final int GLOBAL_CLOCK_INDEX = 4;
  private static final String HIGH_TICK_STR = "highTicks";
  private static final int HIGH_TICK_ID = -1;
  private static final String LOW_TICK_STR = "lowTicks";
  private static final int LOW_TICK_ID = -2;
  private static final String PHASE_STR = "phase";
  private static final int PHASE_ID = -3;
  private static final String NR_OF_BITS_STR = "nrOfBits";
  private static final int NR_OF_BITS_ID = -4;

  public ClockHdlGeneratorFactory() {
    super("base");
    myParametersList
        .add(HIGH_TICK_STR, HIGH_TICK_ID, HdlParameters.MAP_INT_ATTRIBUTE, Clock.ATTR_HIGH)
        .add(LOW_TICK_STR, LOW_TICK_ID, HdlParameters.MAP_INT_ATTRIBUTE, Clock.ATTR_LOW)
        .add(PHASE_STR, PHASE_ID, HdlParameters.MAP_INT_ATTRIBUTE, Clock.ATTR_PHASE, 1)
        .add(NR_OF_BITS_STR, NR_OF_BITS_ID, HdlParameters.MAP_LN2, Clock.ATTR_HIGH, Clock.ATTR_LOW);
    myWires
        .addWire("s_counterNext", NR_OF_BITS_ID)
        .addWire("s_counterIsZero", 1)
        .addRegister("s_outputRegs", NR_OF_CLOCK_BITS - 1)
        .addRegister("s_bufferRegs", 2)
        .addRegister("s_counterValue", NR_OF_BITS_ID)
        .addRegister("s_derivedClock", PHASE_ID);
    myPorts
        .add(Port.INPUT, "globalClock", 1, 0)
        .add(Port.INPUT, "clockTick", 1, 1)
        .add(Port.OUTPUT, "clockBus", NR_OF_CLOCK_BITS, 2);
  }

  @Override
  public SortedMap<String, String> getPortMap(Netlist nets, Object mapInfo) {
    final var map = new TreeMap<String, String>();
    if (!(mapInfo instanceof final netlistComponent componentInfo)) return map;
    map.put("globalClock", SynthesizedClockHdlGeneratorFactory.SYNTHESIZED_CLOCK);
    map.put("clockTick", TickComponentHdlGeneratorFactory.FPGA_TICK);
    map.put("clockBus", getClockNetName(componentInfo.getComponent(), nets));
    return map;
  }

  private static String getClockNetName(Component comp, Netlist theNets) {
    final var contents = new StringBuilder();
    int clockNetId = theNets.getClockSourceId(comp);
    if (clockNetId >= 0) {
      contents.append("s_").append(HdlGeneratorFactory.CLOCK_TREE_NAME).append(clockNetId);
    }
    return contents.toString();
  }

  @Override
  public LineBuffer getModuleFunctionality(Netlist theNetlist, AttributeSet attrs) {
    final var contents = LineBuffer.getHdlBuffer()
            .pair("phase", PHASE_STR)
            .pair("nrOfBits", NR_OF_BITS_STR)
            .pair("lowTick", LOW_TICK_STR)
            .pair("highTick", HIGH_TICK_STR)
            .addRemarkBlock("The output signals are defined here; we synchronize them all on the main clock")
            .empty();

    if (Hdl.isVhdl()) {
      contents.addVhdlKeywords().add("""
          clockBus <= globalClock&s_outputRegs;
          
          makeOutputs : {{process}}(globalClock) {{is}}
          {{begin}}
             {{if}} (rising_edge(globalClock)) {{then}}
                s_bufferRegs(0)  <= s_derivedClock({{phase}} - 1);
                s_bufferRegs(1)  <= {{not}}(s_derivedClock({{phase}} - 1));
                s_outputRegs(0)  <= s_bufferRegs(0);
                s_outputRegs(1)  <= s_bufferRegs(1);
                s_outputRegs(2)  <= {{not}}(s_bufferRegs(0)) {{and}} s_derivedClock({{phase}} - 1);
                s_outputRegs(3)  <= s_bufferRegs(0) {{and}} {{not}}(s_derivedClock({{phase}} - 1));
             {{end}} {{if}};
          {{end}} {{process}} makeOutputs;
          """);
    } else {
      contents.add("""
          assign clockBus = {globalClock,s_outputRegs};
          always @(posedge globalClock)
          begin
             s_bufferRegs[0] <= s_derivedClock[{{phase}} - 1];
             s_bufferRegs[1] <= ~s_derivedClock[{{phase}} - 1];
             s_outputRegs[0] <= s_bufferRegs[0];
             s_outputRegs[1] <= s_outputRegs[1];
             s_outputRegs[2] <= ~s_bufferRegs[0] & s_derivedClock[{{phase}} - 1];
             s_outputRegs[3] <= ~s_derivedClock[{{phase}} - 1] & s_bufferRegs[0];
          end
          """);
    }
    contents.empty().addRemarkBlock("The control signals are defined here");
    if (Hdl.isVhdl()) {
      contents.add("""
          s_counterIsZero <= '1' {{when}} s_counterValue = std_logic_vector(to_unsigned(0,{{nrOfBits}})) {{else}} '0';
          s_counterNext   <= std_logic_vector(unsigned(s_counterValue) - 1)
                                {{when}} s_counterIsZero = '0' {{else}}
                             std_logic_vector(to_unsigned(({{lowTick}}-1), {{nrOfBits}}))
                                {{when}} s_derivedClock(0) = '1' {{else}}
                             std_logic_vector(to_unsigned(({{highTick}}-1), {{nrOfBits}}));
          """);
    } else {
      contents.add("""
              assign s_counterIsZero = (s_counterValue == 0) ? 1'b1 : 1'b0;
              assign s_counterNext = (s_counterIsZero == 1'b0)
                                     ? s_counterValue - 1
                                     : (s_derivedClock[0] == 1'b1)
                                        ? {{lowTick}} - 1
                                        : {{highTick}} - 1;
              """)
          .empty()
          .addRemarkBlock("The initial values are defined here (for simulation only)")
          .add("""
              initial
              begin
                 s_outputRegs = 0;
                 s_derivedClock = 0;
                 s_counterValue = 0;
              end
              """);
    }
    contents.empty().addRemarkBlock("The state registers are defined here");
    if (Hdl.isVhdl()) {
      contents.add("""
          makeDerivedClock : {{process}}(globalClock, clockTick, s_counterIsZero, s_derivedClock) {{is}}
          {{begin}}
             {{if}} (rising_edge(globalClock)) {{then}}
                {{if}} (s_derivedClock(0) /= '0' {{and}} s_derivedClock(0) /= '1') {{then}} --For simulation only
                   s_derivedClock <= ({{others}} => '1');
                {{elsif}} (clockTick = '1') {{then}}
                   {{for}} n IN {{phase}}-1 {{downto}} 1 {{loop}}
                     s_derivedClock(n) <= s_derivedClock(n-1);
                   {{end}} {{loop}};
                   s_derivedClock(0) <= s_derivedClock(0) {{xor}} s_counterIsZero;
                {{end}} {{if}};
             {{end}} {{if}};
          {{end}} {{process}} makeDerivedClock;

          makeCounter : {{process}}(globalClock, clockTick, s_counterNext, s_derivedClock) {{is}}
          {{begin}}
             {{if}} (rising_edge(globalClock)) {{then}}
                {{if}} (s_derivedClock(0) /= '0' {{and}} s_derivedClock(0) /= '1') {{then}} --For simulation only
                   s_counterValue <= ({{others}} => '0');
                {{elsif}} (clockTick = '1') {{then}}
                   s_counterValue <= s_counterNext;
                {{end}} {{if}};
             {{end}} {{if}};
          {{end}} {{process}} makeCounter;
          """);
    } else {
      contents.add("""
          integer n;
          always @(posedge globalClock)
          begin
             if (clockTick)
             begin
                s_derivedClock[0] <= s_derivedClock[0] ^ s_counterIsZero;
                for (n = 1; n < {{phase}}; n = n+1) begin
                   s_derivedClock[n] <= s_derivedClock[n-1];
                end
             end
          end

          always @(posedge globalClock)
          begin
             if (clockTick)
             begin
                s_counterValue <= s_counterNext;
             end
          end
          """);
    }
    return contents.empty();
  }
}
