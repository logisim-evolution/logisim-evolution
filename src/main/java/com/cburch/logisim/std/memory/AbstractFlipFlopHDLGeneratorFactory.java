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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

public class AbstractFlipFlopHDLGeneratorFactory extends AbstractHDLGeneratorFactory {

  private static final String ActivityLevelStr = "ActiveLevel";

  public String ComponentName() {
    return "";
  }

  @Override
  public String getComponentStringIdentifier() {
    return "FF_LATCH";
  }

  @Override
  public SortedMap<String, Integer> GetInputList(Netlist nets, AttributeSet attrs) {
    SortedMap<String, Integer> inputs = new TreeMap<>();
    inputs.put("Reset", 1);
    inputs.put("Preset", 1);
    inputs.put("Tick", 1);
    inputs.put("Clock", 1);
    inputs.putAll(GetInputPorts());
    return inputs;
  }

  public Map<String, String> GetInputMaps(NetlistComponent componentInfo, Netlist nets) {
    return new HashMap<>();
  }

  public Map<String, Integer> GetInputPorts() {
    return new HashMap<>();
  }

  @Override
  public ArrayList<String> GetModuleFunctionality(Netlist nets, AttributeSet attrs) {
    final var contents = new ArrayList<String>();
    final var SelectOperator = (HDL.isVHDL()) ? "" : "[" + ActivityLevelStr + "]";
    contents.addAll(MakeRemarkBlock("Here the output signals are defined", 3));
    contents.add(
        "   "
            + HDL.assignPreamble()
            + "Q    "
            + HDL.assignOperator()
            + "s_current_state_reg"
            + SelectOperator
            + ";");
    contents.add(
        "   "
            + HDL.assignPreamble()
            + "Q_bar"
            + HDL.assignOperator()
            + HDL.notOperator()
            + "(s_current_state_reg"
            + SelectOperator
            + ");");
    contents.add("");
    contents.addAll(MakeRemarkBlock("Here the update logic is defined", 3));
    contents.addAll(GetUpdateLogic());
    contents.add("");
    if (HDL.isVerilog()) {
      contents.addAll(MakeRemarkBlock("Here the initial register value is defined; for simulation only", 3));
      contents.add("   initial");
      contents.add("   begin");
      contents.add("      s_current_state_reg = 0;");
      contents.add("   end");
      contents.add("");
    }
    contents.addAll(MakeRemarkBlock("Here the actual state register is defined", 3));
    if (HDL.isVHDL()) {
      contents.add("   make_memory : PROCESS( clock , Reset , Preset , Tick , s_next_state )");
      contents.add("      VARIABLE temp : std_logic_vector(0 DOWNTO 0);");
      contents.add("   BEGIN");
      contents.add("      temp := std_logic_vector(to_unsigned(" + ActivityLevelStr + ",1));");
      contents.add("      IF (Reset = '1') THEN s_current_state_reg <= '0';");
      contents.add("      ELSIF (Preset = '1') THEN s_current_state_reg <= '1';");
      if (Netlist.IsFlipFlop(attrs)) {
        contents.add("      ELSIF (Clock'event AND (Clock = temp(0))) THEN");
      } else {
        contents.add("      ELSIF (Clock = temp(0)) THEN");
      }
      contents.add("         IF (Tick = '1') THEN");
      contents.add("            s_current_state_reg <= s_next_state;");
      contents.add("         END IF;");
      contents.add("      END IF;");
      contents.add("   END PROCESS make_memory;");
    } else {
      if (Netlist.IsFlipFlop(attrs)) {
        contents.add("   always @(posedge Reset or posedge Preset or negedge Clock)");
        contents.add("   begin");
        contents.add("      if (Reset) s_current_state_reg[0] <= 1'b0;");
        contents.add("      else if (Preset) s_current_state_reg[0] <= 1'b1;");
        contents.add("      else if (Tick) s_current_state_reg[0] <= s_next_state;");
        contents.add("   end");
        contents.add("");
        contents.add("   always @(posedge Reset or posedge Preset or posedge Clock)");
        contents.add("   begin");
        contents.add("      if (Reset) s_current_state_reg[1] <= 1'b0;");
        contents.add("      else if (Preset) s_current_state_reg[1] <= 1'b1;");
        contents.add("      else if (Tick) s_current_state_reg[1] <= s_next_state;");
        contents.add("   end");
      } else {
        contents.add("   always @(*)");
        contents.add("   begin");
        contents.add("      if (Reset) s_current_state_reg <= 2'b0;");
        contents.add("      else if (Preset) s_current_state_reg <= 2'b1;");
        contents.add(
            "      else if (Tick & (Clock == "
                + ActivityLevelStr
                + ")) s_current_state_reg <= {s_next_state,s_next_state};");
        contents.add("   end");
      }
    }
    contents.add("");
    return contents;
  }

  @Override
  public SortedMap<String, Integer> GetOutputList(Netlist TheNetlist, AttributeSet attrs) {
    SortedMap<String, Integer> outputs = new TreeMap<>();
    outputs.put("Q", 1);
    outputs.put("Q_bar", 1);
    return outputs;
  }

  @Override
  public SortedMap<Integer, String> GetParameterList(AttributeSet attrs) {
    SortedMap<Integer, String> parameters = new TreeMap<>();
    parameters.put(-1, ActivityLevelStr);
    return parameters;
  }

  @Override
  public SortedMap<String, Integer> GetParameterMap(Netlist Nets, NetlistComponent ComponentInfo) {
    SortedMap<String, Integer> parameterMap = new TreeMap<>();
    var activityLevel = 1;
    var gatedClock = false;
    var activeLow = false;
    final var attrs = ComponentInfo.GetComponent().getAttributeSet();
    final var clockNetName = GetClockNetName(ComponentInfo, ComponentInfo.NrOfEnds() - 5, Nets);
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
    parameterMap.put(ActivityLevelStr, activityLevel);
    return parameterMap;
  }

  @Override
  public SortedMap<String, String> GetPortMap(Netlist Nets, Object MapInfo) {
    SortedMap<String, String> portMap = new TreeMap<>();
    if (!(MapInfo instanceof NetlistComponent)) return portMap;
    final var ComponentInfo = (NetlistComponent) MapInfo;
    var gatedClock = false;
    var hasClock = true;
    var activeLow = false;
    final var nrOfPins = ComponentInfo.NrOfEnds();
    final var attrs = ComponentInfo.GetComponent().getAttributeSet();
    if (!ComponentInfo.EndIsConnected(ComponentInfo.NrOfEnds() - 5)) {
      Reporter.Report.AddSevereWarning(
          "Component \""
              + ComponentName()
              + "\" in circuit \""
              + Nets.getCircuitName()
              + "\" has no clock connection");
      hasClock = false;
    }
    final var clockNetName = GetClockNetName(ComponentInfo, ComponentInfo.NrOfEnds() - 5, Nets);
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
    portMap.putAll(GetNetMap("Reset", true, ComponentInfo, nrOfPins - 2, Nets));
    portMap.putAll(GetNetMap("Preset", true, ComponentInfo, nrOfPins - 1, Nets));
    if (hasClock && !gatedClock && Netlist.IsFlipFlop(attrs)) {
      if (Nets.RequiresGlobalClockConnection()) {
        portMap.put(
            "Tick",
            clockNetName
                + HDL.BracketOpen()
                + ClockHDLGeneratorFactory.GlobalClockIndex
                + HDL.BracketClose());
      } else {
        if (activeLow)
          portMap.put(
              "Tick",
              clockNetName
                  + HDL.BracketOpen()
                  + ClockHDLGeneratorFactory.NegativeEdgeTickIndex
                  + HDL.BracketClose());
        else
          portMap.put(
              "Tick",
              clockNetName
                  + HDL.BracketOpen()
                  + ClockHDLGeneratorFactory.PositiveEdgeTickIndex
                  + HDL.BracketClose());
      }
      portMap.put(
          "Clock",
          clockNetName
              + HDL.BracketOpen()
              + ClockHDLGeneratorFactory.GlobalClockIndex
              + HDL.BracketClose());
    } else if (!hasClock) {
      portMap.put("Tick", HDL.zeroBit());
      portMap.put("Clock", HDL.zeroBit());
    } else {
      portMap.put("Tick", HDL.oneBit());
      if (!gatedClock) {
        if (activeLow)
          portMap.put(
              "Clock",
              clockNetName
                  + HDL.BracketOpen()
                  + ClockHDLGeneratorFactory.InvertedDerivedClockIndex
                  + HDL.BracketClose());
        else
          portMap.put(
              "Clock",
              clockNetName
                  + HDL.BracketOpen()
                  + ClockHDLGeneratorFactory.DerivedClockIndex
                  + HDL.BracketClose());
      } else {
        portMap.put("Clock", GetNetName(ComponentInfo, ComponentInfo.NrOfEnds() - 5, true, Nets));
      }
    }
    portMap.putAll(GetInputMaps(ComponentInfo, Nets));
    portMap.putAll(GetNetMap("Q", true, ComponentInfo, nrOfPins - 4, Nets));
    portMap.putAll(GetNetMap("Q_bar", true, ComponentInfo, nrOfPins - 3, Nets));
    return portMap;
  }

  @Override
  public SortedMap<String, Integer> GetRegList(AttributeSet attrs) {
    SortedMap<String, Integer> regs = new TreeMap<>();
    regs.put("s_current_state_reg", (HDL.isVHDL()) ? 1 : 2);
    return regs;
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
    SortedMap<String, Integer> wires = new TreeMap<>();
    wires.put("s_next_state", 1);
    return wires;
  }

  @Override
  public boolean HDLTargetSupported(AttributeSet attrs) {
    return true;
  }
}
