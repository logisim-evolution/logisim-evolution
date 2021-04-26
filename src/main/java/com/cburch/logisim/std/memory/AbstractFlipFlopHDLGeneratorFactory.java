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
import com.cburch.logisim.fpga.gui.FPGAReport;
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
  public SortedMap<String, Integer> GetInputList(Netlist TheNetlist, AttributeSet attrs) {
    SortedMap<String, Integer> Inputs = new TreeMap<>();
    Inputs.put("Reset", 1);
    Inputs.put("Preset", 1);
    Inputs.put("Tick", 1);
    Inputs.put("Clock", 1);
    Inputs.putAll(GetInputPorts());
    return Inputs;
  }

  public Map<String, String> GetInputMaps(NetlistComponent ComponentInfo, Netlist Nets, FPGAReport Reporter) {
    return new HashMap<>();
  }

  public Map<String, Integer> GetInputPorts() {
    return new HashMap<>();
  }

  @Override
  public ArrayList<String> GetModuleFunctionality(Netlist TheNetlist, AttributeSet attrs, FPGAReport Reporter) {
    ArrayList<String> Contents = new ArrayList<>();
    String SelectOperator = (HDL.isVHDL()) ? "" : "[" + ActivityLevelStr + "]";
    Contents.addAll(MakeRemarkBlock("Here the output signals are defined", 3));
    Contents.add(
        "   "
            + HDL.assignPreamble()
            + "Q    "
            + HDL.assignOperator()
            + "s_current_state_reg"
            + SelectOperator
            + ";");
    Contents.add(
        "   "
            + HDL.assignPreamble()
            + "Q_bar"
            + HDL.assignOperator()
            + HDL.notOperator()
            + "(s_current_state_reg"
            + SelectOperator
            + ");");
    Contents.add("");
    Contents.addAll(MakeRemarkBlock("Here the update logic is defined", 3));
    Contents.addAll(GetUpdateLogic());
    Contents.add("");
    if (HDL.isVerilog()) {
      Contents.addAll(MakeRemarkBlock("Here the initial register value is defined; for simulation only", 3));
      Contents.add("   initial");
      Contents.add("   begin");
      Contents.add("      s_current_state_reg = 0;");
      Contents.add("   end");
      Contents.add("");
    }
    Contents.addAll(MakeRemarkBlock("Here the actual state register is defined", 3));
    if (HDL.isVHDL()) {
      Contents.add("   make_memory : PROCESS( clock , Reset , Preset , Tick , s_next_state )");
      Contents.add("      VARIABLE temp : std_logic_vector(0 DOWNTO 0);");
      Contents.add("   BEGIN");
      Contents.add("      temp := std_logic_vector(to_unsigned(" + ActivityLevelStr + ",1));");
      Contents.add("      IF (Reset = '1') THEN s_current_state_reg <= '0';");
      Contents.add("      ELSIF (Preset = '1') THEN s_current_state_reg <= '1';");
      if (Netlist.IsFlipFlop(attrs)) {
        Contents.add("      ELSIF (Clock'event AND (Clock = temp(0))) THEN");
      } else {
        Contents.add("      ELSIF (Clock = temp(0)) THEN");
      }
      Contents.add("         IF (Tick = '1') THEN");
      Contents.add("            s_current_state_reg <= s_next_state;");
      Contents.add("         END IF;");
      Contents.add("      END IF;");
      Contents.add("   END PROCESS make_memory;");
    } else {
      if (Netlist.IsFlipFlop(attrs)) {
        Contents.add("   always @(posedge Reset or posedge Preset or negedge Clock)");
        Contents.add("   begin");
        Contents.add("      if (Reset) s_current_state_reg[0] <= 1'b0;");
        Contents.add("      else if (Preset) s_current_state_reg[0] <= 1'b1;");
        Contents.add("      else if (Tick) s_current_state_reg[0] <= s_next_state;");
        Contents.add("   end");
        Contents.add("");
        Contents.add("   always @(posedge Reset or posedge Preset or posedge Clock)");
        Contents.add("   begin");
        Contents.add("      if (Reset) s_current_state_reg[1] <= 1'b0;");
        Contents.add("      else if (Preset) s_current_state_reg[1] <= 1'b1;");
        Contents.add("      else if (Tick) s_current_state_reg[1] <= s_next_state;");
        Contents.add("   end");
      } else {
        Contents.add("   always @(*)");
        Contents.add("   begin");
        Contents.add("      if (Reset) s_current_state_reg <= 2'b0;");
        Contents.add("      else if (Preset) s_current_state_reg <= 2'b1;");
        Contents.add(
            "      else if (Tick & (Clock == "
                + ActivityLevelStr
                + ")) s_current_state_reg <= {s_next_state,s_next_state};");
        Contents.add("   end");
      }
    }
    Contents.add("");
    return Contents;
  }

  @Override
  public SortedMap<String, Integer> GetOutputList(Netlist TheNetlist, AttributeSet attrs) {
    SortedMap<String, Integer> Outputs = new TreeMap<>();
    Outputs.put("Q", 1);
    Outputs.put("Q_bar", 1);
    return Outputs;
  }

  @Override
  public SortedMap<Integer, String> GetParameterList(AttributeSet attrs) {
    SortedMap<Integer, String> Parameters = new TreeMap<>();
    Parameters.put(-1, ActivityLevelStr);
    return Parameters;
  }

  @Override
  public SortedMap<String, Integer> GetParameterMap(
      Netlist Nets, NetlistComponent ComponentInfo, FPGAReport Reporter) {
    SortedMap<String, Integer> ParameterMap = new TreeMap<>();
    int ActivityLevel = 1;
    Boolean GatedClock = false;
    Boolean ActiveLow = false;
    AttributeSet attrs = ComponentInfo.GetComponent().getAttributeSet();
    String ClockNetName = GetClockNetName(ComponentInfo, ComponentInfo.NrOfEnds() - 5, Nets);
    if (ClockNetName.isEmpty()) {
      GatedClock = true;
    }
    if (attrs.containsAttribute(StdAttr.EDGE_TRIGGER)) {
      if (attrs.getValue(StdAttr.EDGE_TRIGGER) == StdAttr.TRIG_FALLING) ActiveLow = true;
    } else {
      if (attrs.containsAttribute(StdAttr.TRIGGER)) {
        if (attrs.getValue(StdAttr.TRIGGER) == StdAttr.TRIG_FALLING
            || attrs.getValue(StdAttr.TRIGGER) == StdAttr.TRIG_LOW) ActiveLow = true;
      }
    }
    if (GatedClock && ActiveLow) {
      ActivityLevel = 0;
    }
    ParameterMap.put(ActivityLevelStr, ActivityLevel);
    return ParameterMap;
  }

  @Override
  public SortedMap<String, String> GetPortMap(Netlist Nets, Object MapInfo, FPGAReport Reporter) {
    SortedMap<String, String> PortMap = new TreeMap<>();
    if (!(MapInfo instanceof NetlistComponent)) return PortMap;
    NetlistComponent ComponentInfo = (NetlistComponent) MapInfo;
    Boolean GatedClock = false;
    Boolean HasClock = true;
    Boolean ActiveLow = false;
    int nr_of_pins = ComponentInfo.NrOfEnds();
    AttributeSet attrs = ComponentInfo.GetComponent().getAttributeSet();
    if (!ComponentInfo.EndIsConnected(ComponentInfo.NrOfEnds() - 5)) {
      Reporter.AddSevereWarning(
          "Component \""
              + ComponentName()
              + "\" in circuit \""
              + Nets.getCircuitName()
              + "\" has no clock connection");
      HasClock = false;
    }
    String ClockNetName = GetClockNetName(ComponentInfo, ComponentInfo.NrOfEnds() - 5, Nets);
    if (ClockNetName.isEmpty()) {
      GatedClock = true;
    }
    if (attrs.containsAttribute(StdAttr.EDGE_TRIGGER)) {
      if (attrs.getValue(StdAttr.EDGE_TRIGGER) == StdAttr.TRIG_FALLING) ActiveLow = true;
    } else {
      if (attrs.containsAttribute(StdAttr.TRIGGER)) {
        if (attrs.getValue(StdAttr.TRIGGER) == StdAttr.TRIG_FALLING
            || attrs.getValue(StdAttr.TRIGGER) == StdAttr.TRIG_LOW) ActiveLow = true;
      }
    }
    PortMap.putAll(
        GetNetMap("Reset", true, ComponentInfo, nr_of_pins - 2, Reporter, Nets));
    PortMap.putAll(
        GetNetMap("Preset", true, ComponentInfo, nr_of_pins - 1, Reporter, Nets));
    if (HasClock && !GatedClock && Netlist.IsFlipFlop(attrs)) {
      if (Nets.RequiresGlobalClockConnection()) {
        PortMap.put(
            "Tick",
            ClockNetName
                + HDL.BracketOpen()
                + ClockHDLGeneratorFactory.GlobalClockIndex
                + HDL.BracketClose());
      } else {
        if (ActiveLow)
          PortMap.put(
              "Tick",
              ClockNetName
                  + HDL.BracketOpen()
                  + ClockHDLGeneratorFactory.NegativeEdgeTickIndex
                  + HDL.BracketClose());
        else
          PortMap.put(
              "Tick",
              ClockNetName
                  + HDL.BracketOpen()
                  + ClockHDLGeneratorFactory.PositiveEdgeTickIndex
                  + HDL.BracketClose());
      }
      PortMap.put(
          "Clock",
          ClockNetName
              + HDL.BracketOpen()
              + ClockHDLGeneratorFactory.GlobalClockIndex
              + HDL.BracketClose());
    } else if (!HasClock) {
      PortMap.put("Tick", HDL.zeroBit());
      PortMap.put("Clock", HDL.zeroBit());
    } else {
      PortMap.put("Tick", HDL.oneBit());
      if (!GatedClock) {
        if (ActiveLow)
          PortMap.put(
              "Clock",
              ClockNetName
                  + HDL.BracketOpen()
                  + ClockHDLGeneratorFactory.InvertedDerivedClockIndex
                  + HDL.BracketClose());
        else
          PortMap.put(
              "Clock",
              ClockNetName
                  + HDL.BracketOpen()
                  + ClockHDLGeneratorFactory.DerivedClockIndex
                  + HDL.BracketClose());
      } else {
        PortMap.put("Clock", GetNetName(ComponentInfo, ComponentInfo.NrOfEnds() - 5, true, Nets));
      }
    }
    PortMap.putAll(GetInputMaps(ComponentInfo, Nets, Reporter));
    PortMap.putAll(GetNetMap("Q", true, ComponentInfo, nr_of_pins - 4, Reporter, Nets));
    PortMap.putAll(GetNetMap("Q_bar", true, ComponentInfo, nr_of_pins - 3, Reporter, Nets));
    return PortMap;
  }

  @Override
  public SortedMap<String, Integer> GetRegList(AttributeSet attrs) {
    SortedMap<String, Integer> Regs = new TreeMap<>();
    Regs.put("s_current_state_reg", (HDL.isVHDL()) ? 1 : 2);
    return Regs;
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
    SortedMap<String, Integer> Wires = new TreeMap<>();
    Wires.put("s_next_state", 1);
    return Wires;
  }

  @Override
  public boolean HDLTargetSupported(AttributeSet attrs) {
    return true;
  }
}
