/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.std.memory;

import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.fpga.designrulecheck.Netlist;
import com.cburch.logisim.fpga.designrulecheck.netlistComponent;
import com.cburch.logisim.fpga.hdlgenerator.AbstractHdlGeneratorFactory;
import com.cburch.logisim.fpga.hdlgenerator.Hdl;
import com.cburch.logisim.fpga.hdlgenerator.HdlParameters;
import com.cburch.logisim.fpga.hdlgenerator.HdlPorts;
import com.cburch.logisim.instance.Port;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.util.LineBuffer;
import java.util.SortedMap;
import java.util.TreeMap;

public class RandomHdlGeneratorFactory extends AbstractHdlGeneratorFactory {

  private static final String NR_OF_BITS_STR = "nrOfBits";
  private static final int NR_OF_BITS_ID = -1;
  private static final String SEED_STR = "seed";
  private static final int SEED_ID = -2;

  public RandomHdlGeneratorFactory() {
    super();
    myParametersList
        .add(NR_OF_BITS_STR, NR_OF_BITS_ID)
        // The seed parameter has 32 bits fixed
        .addVector(SEED_STR, SEED_ID, HdlParameters.MAP_INT_ATTRIBUTE, Random.ATTR_SEED, 32);
    myWires
        .addWire("s_initSeed", 48)
        .addWire("s_reset", 1)
        .addWire("s_resetNext", 3)
        .addWire("s_multShiftNext", 36)
        .addWire("s_seedShiftNext", 48)
        .addWire("s_multBusy", 1)
        .addWire("s_start", 1)
        .addWire("s_macLowIn1", 25)
        .addWire("s_macLowIn2", 25)
        .addWire("s_macHigh1Next", 24)
        .addWire("s_macHighIn2", 24)
        .addWire("s_busyPipeNext", 2)
        .addRegister("s_currentSeed", 48)
        .addRegister("s_resetReg", 3)
        .addRegister("s_multShiftReg", 36)
        .addRegister("s_seedShiftReg", 48)
        .addRegister("s_startReg", 1)
        .addRegister("s_macLowReg", 25)
        .addRegister("s_macHighReg", 24)
        .addRegister("s_macHighReg1", 24)
        .addRegister("s_busyPipeReg", 2)
        .addRegister("s_outputReg", NR_OF_BITS_ID);
    myPorts
        .add(Port.CLOCK, HdlPorts.getClockName(1), 1, Random.CK)
        .add(Port.INPUT, "clear", 1, Random.RST)
        .add(Port.INPUT, "enable", 1, Random.NXT, false)
        .add(Port.OUTPUT, "q", NR_OF_BITS_ID, Random.OUT);
  }

  @Override
  public SortedMap<String, String> getPortMap(Netlist Nets, Object MapInfo) {
    final var map = new TreeMap<String, String>();
    map.putAll(super.getPortMap(Nets, MapInfo));
    if (MapInfo instanceof netlistComponent && Hdl.isVhdl()) {
      final var comp = (netlistComponent) MapInfo;
      final var nrOfBits = comp.getComponent().getAttributeSet().getValue(StdAttr.WIDTH).getWidth();
      if (nrOfBits == 1) {
        final var outMap = map.get("q");
        map.remove("q");
        map.put("q(0)", outMap);
      }
    }
    return map;
  }

  @Override
  public LineBuffer getModuleFunctionality(Netlist nets, AttributeSet attrs) {
    final var contents =
        LineBuffer.getBuffer()
            .pair("seed", SEED_STR)
            .pair("nrOfBits", NR_OF_BITS_STR)
            .pair("GlobalClock", HdlPorts.getClockName(1))
            .pair("ClockEnable", HdlPorts.getTickName(1))
            .addRemarkBlock("This is a multicycle implementation of the Random Component")
            .empty();

    if (Hdl.isVhdl()) {
      contents.empty().addVhdlKeywords().add("""
          q               <= s_outputReg;
          s_initSeed      <= x"0005DEECE66D" {{when}} {{seed}} = x"00000000" {{else}}
                             x"0000"&seed;
          s_reset         <= '1' {{when}} s_resetReg /= "010" {{else}} '0';
          s_resetNext     <= "010" {{when}} (s_resetReg = "101" {{or}}
                                         s_resetReg = "010") {{and}}
                                         clear = '0' {{else}}
                             "101" {{when}} s_resetReg = "001" {{else}}
                             "001";
          s_start         <= '1' {{when}} ({{ClockEnable}} = '1' {{and}} enable = '1') {{or}}
                                   (s_resetReg = "101" {{and}} clear = '0') {{else}} '0';
          s_multShiftNext <= ({{others}} => '0') {{when}} s_reset = '1' {{else}}
                             X"5DEECE66D" {{when}} s_startReg = '1' {{else}}
                             '0'&s_multShiftReg(35 {{downto}} 1);
          s_seedShiftNext <= ({{others}} => '0') {{when}} s_reset = '1' {{else}}
                             s_currentSeed {{when}} s_startReg = '1' {{else}}
                             s_seedShiftReg(46 {{downto}} 0)&'0';
          s_multBusy      <= '0' {{when}} s_multShiftReg = X"000000000" {{else}} '1';

          s_macLowIn1     <= ({{others}} => '0') {{when}} s_startReg = '1' {{or}}
                                                    s_reset = '1' {{else}}
                             '0'&s_macLowReg(23 {{downto}} 0);
          s_macLowIn2     <= '0'&X"00000B"
                                  {{when}} s_startReg = '1' {{else}}
                             '0'&s_seedShiftReg(23 {{downto}} 0)
                                  {{when}} s_multShiftReg(0) = '1' {{else}}
                             ({{others}} => '0');
          s_macHighIn2    <= ({{others}} => '0') {{when}} s_startReg = '1' {{else}}
                             s_macHighReg;
          s_macHigh1Next  <= s_seedShiftReg(47 {{downto}} 24)
                                {{when}} s_multShiftReg(0) = '1' {{else}}
                             ({{others}} => '0');
          s_busyPipeNext  <= "00" {{when}} s_reset = '1' {{else}}
                             s_busyPipeReg(0)&s_multBusy;

          makeCurrentSeed : {{process}}({{GlobalClock}}, s_busyPipeReg, s_reset) {{is}}
          {{begin}}
             {{if}} (rising_edge({{GlobalClock}})) {{then}}
                {{if}} (s_reset = '1') {{then}} s_currentSeed <= s_initSeed;
                {{elsif}} (s_busyPipeReg = "10") {{then}}
                   s_currentSeed <= s_macHighReg&s_macLowReg(23 {{downto}} 0);
                {{end}} {{if}};
             {{end}} {{if}};
          {{end}} {{process}} makeCurrentSeed;

          makeShiftRegs : {{process}}({{GlobalClock}}, s_multShiftNext, s_seedShiftNext,
                                  s_macLowIn1, s_macLowIn2) {{is}}
          {{begin}}
             {{if}} (rising_edge({{GlobalClock}})) {{then}}
                s_multShiftReg <= s_multShiftNext;
                s_seedShiftReg <= s_seedShiftNext;
                s_macLowReg    <= std_logic_vector( unsigned(s_macLowIn1) + unsigned(s_macLowIn2) );
                s_macHighReg1  <= s_macHigh1Next;
                s_macHighReg   <= std_logic_vector( unsigned(s_macHighReg1) + unsigned(s_macHighIn2) +
                                  unsigned(s_macLowReg(24 {{downto}} 24)) );
                s_busyPipeReg  <= s_busyPipeNext;
             {{end}} {{if}};
          {{end}} {{process}} makeShiftRegs;

          makeStartReg : {{process}}({{GlobalClock}}, s_start) {{is}}
          {{begin}}
             {{if}} (rising_edge({{GlobalClock}})) {{then}}
                s_startReg <= s_start;
             {{end}} {{if}};
          {{end}} {{process}} makeStartReg;

          makeResetReg : {{process}}({{GlobalClock}}, s_resetNext) {{is}}
          {{begin}}
             {{if}} (rising_edge({{GlobalClock}})) {{then}}
                s_resetReg <= s_resetNext;
             {{end}} {{if}};
          {{end}} {{process}} makeResetReg;

          makeOutput : {{process}}({{GlobalClock}}, s_reset, s_initSeed) {{is}}
          {{begin}}
             {{if}} (rising_edge({{GlobalClock}})) {{then}}
                {{if}} (s_reset = '1') {{then}} s_outputReg <= s_initSeed( ({{nrOfBits}}-1) {{downto}} 0 );
                {{elsif}} ({{ClockEnable}} = '1' {{and}} enable = '1') {{then}}
                   s_outputReg <= s_currentSeed(({{nrOfBits}}+11) {{downto}} 12);
                {{end}} {{if}};
             {{end}} {{if}};
          {{end}} {{process}} makeOutput;
          """);
    } else {
      contents.add("""
          assign q = s_outputReg;
          assign s_initSeed      = ({{seed}} == 0) ? 48'h5DEECE66D : {{seed}};
          assign s_reset         = (s_resetReg==3'b010) ? 1'b1 : 1'b0;
          assign s_resetNext     = (( (s_resetReg == 3'b101) | (s_resetReg == 3'b010)) & clear)
                                      ? 3'b010
                                      : (s_resetReg==3'b001) ? 3'b101 : 3'b001;
          assign s_start         = (({{ClockEnable}}&enable)|((s_resetReg == 3'b101)&clear)) ? 1'b1 : 1'b0;
          assign s_multShiftNext = (s_reset)
                                      ? 36'd0
                                      : (s_startReg) ? 36'h5DEECE66D : {1'b0,s_multShiftReg[35:1]};
          assign s_seedShiftNext = (s_reset)
                                      ? 48'd0
                                      : (s_startReg) ? s_currentSeed : {s_seedShiftReg[46:0],1'b0};
          assign s_multBusy      = (s_multShiftReg == 0) ? 1'b0 : 1'b1;
          assign s_macLowIn1     = (s_startReg|s_reset) ? 25'd0 : {1'b0,s_macLowReg[23:0]};
          assign s_macLowIn2     = (s_startReg) ? 25'hB
                                      : (s_multShiftReg[0])
                                      ? {1'b0,s_seedShiftReg[23:0]} : 25'd0;
          assign s_macHighIn2    = (s_startReg) ? 0 : s_macHighReg;
          assign s_macHigh1Next  = (s_multShiftReg[0]) ? s_seedShiftReg[47:24] : 0;
          assign s_busyPipeNext  = (s_reset) ? 2'd0 : {s_busyPipeReg[0],s_multBusy};

          always @(posedge {{GlobalClock}})
          begin
             if (s_reset) s_currentSeed <= s_initSeed;
             else if (s_busyPipeReg == 2'b10) s_currentSeed <= {s_macHighReg,s_macLowReg[23:0]};
          end

          always @(posedge {{GlobalClock}})
          begin
                s_multShiftReg <= s_multShiftNext;
                s_seedShiftReg <= s_seedShiftNext;
                s_macLowReg    <= s_macLowIn1+s_macLowIn2;
                s_macHighReg1  <= s_macHigh1Next;
                s_macHighReg   <= s_macHighReg1+s_macHighIn2+s_macLowReg[24];
                s_busyPipeReg  <= s_busyPipeNext;
                s_startReg     <= s_start;
                s_resetReg     <= s_resetNext;
          end

          always @(posedge {{GlobalClock}})
          begin
             if (s_reset) s_outputReg <= s_initSeed[({{nrOfBits}}-1):0];
             else if ({{ClockEnable}}&enable) s_outputReg <= s_currentSeed[({{nrOfBits}}+11):12];
          end
          """);
    }
    return contents.empty();
  }
}
