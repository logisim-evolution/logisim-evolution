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
import com.cburch.logisim.fpga.designrulecheck.NetlistComponent;
import com.cburch.logisim.fpga.gui.Reporter;
import com.cburch.logisim.fpga.hdlgenerator.AbstractHDLGeneratorFactory;
import com.cburch.logisim.fpga.hdlgenerator.HDL;
import com.cburch.logisim.fpga.hdlgenerator.HDLParameters;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.std.wiring.ClockHDLGeneratorFactory;
import com.cburch.logisim.util.LineBuffer;
import java.util.ArrayList;
import java.util.SortedMap;
import java.util.TreeMap;

public class RegisterHDLGeneratorFactory extends AbstractHDLGeneratorFactory {

  private static final String NR_OF_BITS_STRING = "NrOfBits";
  private static final int NR_OF_BITS_ID = -1;
  private static final String INVERT_CLOCK_STRING = "InvertClock";
  private static final int INVERT_CLOCK_ID = -2;

  public RegisterHDLGeneratorFactory() {
    super();
    myParametersList
        .add(NR_OF_BITS_STRING, NR_OF_BITS_ID)
        .add(INVERT_CLOCK_STRING, INVERT_CLOCK_ID, HDLParameters.MAP_ATTRIBUTE_OPTION, StdAttr.TRIGGER, AbstractFlipFlopHDLGeneratorFactory.TRIGGER_MAP);
    myWires
        .addWire("s_clock", 1)
        .addRegister("s_state_reg", NR_OF_BITS_ID);
  }

  @Override
  public SortedMap<String, Integer> GetInputList(Netlist nets, AttributeSet attrs) {
    final var map = new TreeMap<String, Integer>();
    map.put("Reset", 1);
    map.put("ClockEnable", 1);
    map.put("Tick", 1);
    map.put("Clock", 1);
    map.put("D", NR_OF_BITS_ID);
    return map;
  }

  @Override
  public ArrayList<String> GetModuleFunctionality(Netlist nets, AttributeSet attrs) {
    final var contents = LineBuffer.getBuffer()
            .pair("invertClock", INVERT_CLOCK_STRING);
    if (HDL.isVHDL()) {
      contents.add("""
          Q       <= s_state_reg;
          s_clock <= clock WHEN {{invertClock}} = 0 ELSE NOT(clock);

          make_memory : PROCESS( s_clock , Reset , ClockEnable , Tick , D )
          BEGIN
             IF (Reset = '1') THEN s_state_reg <= (OTHERS => '0');
          """);
      if (Netlist.isFlipFlop(attrs)) {
        contents.add("""
               ELSIF (rising_Edge(s_clock)) THEN
                  IF (ClockEnable = '1' AND Tick = '1') THEN
                     s_state_reg <= D;
                  END IF;
               """);
      } else {
        contents.add("""
              ELSIF (s_clock = '1') THEN
                 IF (ClockEnable = '1' AND Tick = '1') THEN
                    s_state_reg <= D;
                 END IF;
              """);
      }
      contents.add("""
                 END IF;
              END PROCESS make_memory;
              """);
    } else {
      if (!Netlist.isFlipFlop(attrs)) {
        contents.add("""
            assign Q = s_state_reg;
            assign s_clock = {{invertClock}} == 0 ? clock : ~clock;

            always @(*)
            begin
               if (Reset) s_state_reg <= 0;
               else if (s_Clock&ClockEnable&Tick) s_state_reg <= D;
            end
            """);
      } else {
        contents.add("""
            assign Q = s_state_reg;
            assign s_clock = {{invertClock}} == 0 ? clock : ~clock;

            always @(posedge s_clock or posedge Reset)
            begin
               if (Reset) s_state_reg <= 0;
               else if (ClockEnable&Tick) s_state_reg <= D;
            end
            """);
      }
    }
    return contents.getWithIndent();
  }

  @Override
  public SortedMap<String, Integer> GetOutputList(Netlist nets, AttributeSet attrs) {
    final var map = new TreeMap<String, Integer>();
    map.put("Q", NR_OF_BITS_ID);
    return map;
  }

  @Override
  public SortedMap<String, String> GetPortMap(Netlist Nets, Object MapInfo) {
    final var map = new TreeMap<String, String>();
    if (!(MapInfo instanceof NetlistComponent)) return map;
    final var comp = (NetlistComponent) MapInfo;
    var gatedClock = false;
    var hasClock = true;
    var activeLow = false;
    final var attrs = comp.getComponent().getAttributeSet();
    if (!comp.isEndConnected(Register.CK)) {
      Reporter.Report.AddSevereWarning(
          "Component \"Register\" in circuit \""
              + Nets.getCircuitName()
              + "\" has no clock connection");
      hasClock = false;
    }
    final var clockNetName = HDL.getClockNetName(comp, Register.CK, Nets);
    if (clockNetName.isEmpty()) {
      gatedClock = true;
    }
    if (attrs.getValue(StdAttr.TRIGGER) == StdAttr.TRIG_FALLING
        || attrs.getValue(StdAttr.TRIGGER) == StdAttr.TRIG_LOW) activeLow = true;
    map.putAll(GetNetMap("Reset", true, comp, Register.CLR, Nets));
    map.putAll(
        GetNetMap("ClockEnable", false, comp, Register.EN, Nets));

    if (hasClock && !gatedClock && Netlist.isFlipFlop(attrs)) {
      if (Nets.requiresGlobalClockConnection()) {
        map.put("Tick", HDL.oneBit());
      } else {
        if (activeLow)
          map.put(
              "Tick",
              clockNetName
                  + HDL.BracketOpen()
                  + ClockHDLGeneratorFactory.NEGATIVE_EDGE_TICK_INDEX
                  + HDL.BracketClose());
        else
          map.put(
              "Tick",
              clockNetName
                  + HDL.BracketOpen()
                  + ClockHDLGeneratorFactory.POSITIVE_EDGE_TICK_INDEX
                  + HDL.BracketClose());
      }
      map.put(
          "Clock",
          clockNetName
              + HDL.BracketOpen()
              + ClockHDLGeneratorFactory.GLOBAL_CLOCK_INDEX
              + HDL.BracketClose());
    } else if (!hasClock) {
      map.put("Tick", HDL.zeroBit());
      map.put("Clock", HDL.zeroBit());
    } else {
      map.put("Tick", HDL.oneBit());
      if (!gatedClock) {
        if (activeLow)
          map.put(
              "Clock",
              clockNetName
                  + HDL.BracketOpen()
                  + ClockHDLGeneratorFactory.INVERTED_DERIVED_CLOCK_INDEX
                  + HDL.BracketClose());
        else
          map.put(
              "Clock",
              clockNetName
                  + HDL.BracketOpen()
                  + ClockHDLGeneratorFactory.DERIVED_CLOCK_INDEX
                  + HDL.BracketClose());
      } else {
        map.put("Clock", HDL.getNetName(comp, Register.CK, true, Nets));
      }
    }
    var input = "D";
    var output = "Q";
    if (HDL.isVHDL()
        & (comp.getComponent().getAttributeSet().getValue(StdAttr.WIDTH).getWidth()
            == 1)) {
      input += "(0)";
      output += "(0)";
    }
    map.putAll(GetNetMap(input, true, comp, Register.IN, Nets));
    map.putAll(GetNetMap(output, true, comp, Register.OUT, Nets));
    return map;
  }
}
