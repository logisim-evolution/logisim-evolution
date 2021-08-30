/*
 * This file is part of logisim-evolution.
 *
 * Logisim-evolution is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 *
 * Logisim-evolution is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with logisim-evolution. If not, see <http://www.gnu.org/licenses/>.
 *
 * Original code by Carl Burch (http://www.cburch.com), 2011.
 * Subsequent modifications by:
 *   + College of the Holy Cross
 *     http://www.holycross.edu
 *   + Haute École Spécialisée Bernoise/Berner Fachhochschule
 *     http://www.bfh.ch
 *   + Haute École du paysage, d'ingénierie et d'architecture de Genève
 *     http://hepia.hesge.ch/
 *   + Haute École d'Ingénierie et de Gestion du Canton de Vaud
 *     http://www.heig-vd.ch/
 */

package com.cburch.logisim.std.memory;

import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.fpga.designrulecheck.Netlist;
import com.cburch.logisim.fpga.designrulecheck.NetlistComponent;
import com.cburch.logisim.fpga.gui.Reporter;
import com.cburch.logisim.fpga.hdlgenerator.AbstractHDLGeneratorFactory;
import com.cburch.logisim.fpga.hdlgenerator.HDL;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.std.wiring.ClockHDLGeneratorFactory;
import com.cburch.logisim.util.LineBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

public class AbstractFlipFlopHDLGeneratorFactory extends AbstractHDLGeneratorFactory {

  private static final String ACTIVITY_LEVEL_STR = "ActiveLevel";

  public String ComponentName() {
    return "";
  }

  @Override
  public String getComponentStringIdentifier() {
    return "FF_LATCH";
  }

  @Override
  public SortedMap<String, Integer> GetInputList(Netlist nets, AttributeSet attrs) {
    final var map = new TreeMap<String, Integer>();
    map.put("Reset", 1);
    map.put("Preset", 1);
    map.put("Tick", 1);
    map.put("Clock", 1);
    map.putAll(GetInputPorts());
    return map;
  }

  public Map<String, String> GetInputMaps(NetlistComponent componentInfo, Netlist nets) {
    return new HashMap<>();
  }

  public Map<String, Integer> GetInputPorts() {
    return new HashMap<>();
  }

  @Override
  public ArrayList<String> GetModuleFunctionality(Netlist nets, AttributeSet attrs) {
    final var contents = new LineBuffer();
    final var SelectOperator = (HDL.isVHDL()) ? "" : "[" + ACTIVITY_LEVEL_STR + "]";
    contents
        .addRemarkBlock("Here the output signals are defined")
        .add("   {{1}}Q    {{2}}s_current_state_reg{{3}};", HDL.assignPreamble(), HDL.assignOperator(), SelectOperator)
        .add("   {{1}}Q_bar{{2}}{{3}}(s_current_state_reg{{4}});", HDL.assignPreamble(), HDL.assignOperator(), HDL.notOperator(), SelectOperator)
        .add("")
        .addRemarkBlock("Here the update logic is defined")
        .add(GetUpdateLogic())
        .add("");
    if (HDL.isVerilog()) {
      contents
          .addRemarkBlock("Here the initial register value is defined; for simulation only")
          .add("   initial")
          .add("   begin")
          .add("      s_current_state_reg = 0;")
          .add("   end")
          .add("");
    }

    contents.addRemarkBlock("Here the actual state register is defined");
    if (HDL.isVHDL()) {
      contents
          .add("make_memory : PROCESS( clock , Reset , Preset , Tick , s_next_state )")
          .add("   VARIABLE temp : std_logic_vector(0 DOWNTO 0);")
          .add("BEGIN")
          .add("   temp := std_logic_vector(to_unsigned({{1}}, 1));", ACTIVITY_LEVEL_STR)
          .add("   IF (Reset = '1') THEN s_current_state_reg <= '0';")
          .add("   ELSIF (Preset = '1') THEN s_current_state_reg <= '1';");
      if (Netlist.isFlipFlop(attrs)) {
        contents.add("   ELSIF (Clock'event AND (Clock = temp(0))) THEN");
      } else {
        contents.add("   ELSIF (Clock = temp(0)) THEN");
      }
      contents
          .add("       IF (Tick = '1') THEN")
          .add("         s_current_state_reg <= s_next_state;")
          .add("      END IF;")
          .add("   END IF;")
          .add("END PROCESS make_memory;");
    } else {
      if (Netlist.isFlipFlop(attrs)) {
        contents.addLines(
            "always @(posedge Reset or posedge Preset or negedge Clock)",
            "begin",
            "   if (Reset) s_current_state_reg[0] <= 1'b0;",
            "   else if (Preset) s_current_state_reg[0] <= 1'b1;",
            "   else if (Tick) s_current_state_reg[0] <= s_next_state;",
            "end",
            "",
            "always @(posedge Reset or posedge Preset or posedge Clock)",
            "begin",
            "   if (Reset) s_current_state_reg[1] <= 1'b0;",
            "   else if (Preset) s_current_state_reg[1] <= 1'b1;",
            "   else if (Tick) s_current_state_reg[1] <= s_next_state;",
            "end");
      } else {
        contents
            .pair("activityLevel", ACTIVITY_LEVEL_STR)
            .addLines(
                "always @(*)",
                "begin",
                "   if (Reset) s_current_state_reg <= 2'b0;",
                "   else if (Preset) s_current_state_reg <= 2'b1;",
                "   else if (Tick & (Clock == {{activityLevel}})) s_current_state_reg <= {s_next_state,s_next_state};",
                "end");
      }
    }
    contents.empty();
    return contents.getWithIndent();
  }

  @Override
  public SortedMap<String, Integer> GetOutputList(Netlist TheNetlist, AttributeSet attrs) {
    final var map = new TreeMap<String, Integer>();
    map.put("Q", 1);
    map.put("Q_bar", 1);
    return map;
  }

  @Override
  public SortedMap<Integer, String> GetParameterList(AttributeSet attrs) {
    final var map = new TreeMap<Integer, String>();
    map.put(-1, ACTIVITY_LEVEL_STR);
    return map;
  }

  @Override
  public SortedMap<String, Integer> GetParameterMap(Netlist Nets, NetlistComponent ComponentInfo) {
    final var map = new TreeMap<String, Integer>();
    var activityLevel = 1;
    var gatedClock = false;
    var activeLow = false;
    final var attrs = ComponentInfo.getComponent().getAttributeSet();
    final var clockNetName = GetClockNetName(ComponentInfo, ComponentInfo.nrOfEnds() - 5, Nets);
    if (clockNetName.isEmpty()) {
      gatedClock = true;
    }
    if (attrs.containsAttribute(StdAttr.EDGE_TRIGGER)) {
      if (attrs.getValue(StdAttr.EDGE_TRIGGER) == StdAttr.TRIG_FALLING) activeLow = true;
    } else {
      if (attrs.containsAttribute(StdAttr.TRIGGER)) {
        if (attrs.getValue(StdAttr.TRIGGER) == StdAttr.TRIG_FALLING
            || attrs.getValue(StdAttr.TRIGGER) == StdAttr.TRIG_LOW) activeLow = true;
      }
    }
    if (gatedClock && activeLow) {
      activityLevel = 0;
    }
    map.put(ACTIVITY_LEVEL_STR, activityLevel);
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
    final var nrOfPins = comp.nrOfEnds();
    final var attrs = comp.getComponent().getAttributeSet();
    if (!comp.isEndConnected(comp.nrOfEnds() - 5)) {
      Reporter.Report.AddSevereWarning(
          "Component \""
              + ComponentName()
              + "\" in circuit \""
              + Nets.getCircuitName()
              + "\" has no clock connection");
      hasClock = false;
    }
    final var clockNetName = GetClockNetName(comp, comp.nrOfEnds() - 5, Nets);
    if (clockNetName.isEmpty()) {
      gatedClock = true;
    }
    if (attrs.containsAttribute(StdAttr.EDGE_TRIGGER)) {
      if (attrs.getValue(StdAttr.EDGE_TRIGGER) == StdAttr.TRIG_FALLING) activeLow = true;
    } else {
      if (attrs.containsAttribute(StdAttr.TRIGGER)) {
        if (attrs.getValue(StdAttr.TRIGGER) == StdAttr.TRIG_FALLING
            || attrs.getValue(StdAttr.TRIGGER) == StdAttr.TRIG_LOW) activeLow = true;
      }
    }
    map.putAll(GetNetMap("Reset", true, comp, nrOfPins - 2, Nets));
    map.putAll(GetNetMap("Preset", true, comp, nrOfPins - 1, Nets));
    if (hasClock && !gatedClock && Netlist.isFlipFlop(attrs)) {
      if (Nets.requiresGlobalClockConnection()) {
        map.put(
            "Tick",
            clockNetName
                + HDL.BracketOpen()
                + ClockHDLGeneratorFactory.GLOBAL_CLOCK_INDEX
                + HDL.BracketClose());
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
        map.put("Clock", GetNetName(comp, comp.nrOfEnds() - 5, true, Nets));
      }
    }
    map.putAll(GetInputMaps(comp, Nets));
    map.putAll(GetNetMap("Q", true, comp, nrOfPins - 4, Nets));
    map.putAll(GetNetMap("Q_bar", true, comp, nrOfPins - 3, Nets));
    return map;
  }

  @Override
  public SortedMap<String, Integer> GetRegList(AttributeSet attrs) {
    final var map = new TreeMap<String, Integer>();
    map.put("s_current_state_reg", (HDL.isVHDL()) ? 1 : 2);
    return map;
  }

  @Override
  public String GetSubDir() {
    return "memory";
  }

  public ArrayList<String> GetUpdateLogic() {
    return new ArrayList<>();
  }

  @Override
  public SortedMap<String, Integer> GetWireList(AttributeSet attrs, Netlist Nets) {
    final var map = new TreeMap<String, Integer>();
    map.put("s_next_state", 1);
    return map;
  }

  @Override
  public boolean HDLTargetSupported(AttributeSet attrs) {
    return true;
  }
}
