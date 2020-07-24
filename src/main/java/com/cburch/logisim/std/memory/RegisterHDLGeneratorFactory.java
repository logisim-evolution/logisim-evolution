/**
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
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.std.wiring.ClockHDLGeneratorFactory;
import java.util.ArrayList;
import java.util.SortedMap;
import java.util.TreeMap;

public class RegisterHDLGeneratorFactory extends AbstractHDLGeneratorFactory {

  private static final String NrOfBitsStr = "NrOfBits";
  private static final int NrOfBitsId = -1;
  private static final String ActiveLevelStr = "ActiveLevel";
  private static final int ActiveLevelId = -2;

  @Override
  public String getComponentStringIdentifier() {
    return "REGISTER_FILE";
  }

  @Override
  public SortedMap<String, Integer> GetInputList(Netlist TheNetlist, AttributeSet attrs) {
    SortedMap<String, Integer> Inputs = new TreeMap<String, Integer>();
    Inputs.put("Reset", 1);
    Inputs.put("ClockEnable", 1);
    Inputs.put("Tick", 1);
    Inputs.put("Clock", 1);
    Inputs.put("D", NrOfBitsId);
    return Inputs;
  }

  @Override
  public ArrayList<String> GetModuleFunctionality(
      Netlist TheNetlist, AttributeSet attrs, FPGAReport Reporter, String HDLType) {
    ArrayList<String> Contents = new ArrayList<String>();
    if (HDLType.equals(VHDL)) {
      Contents.add("   Q <= s_state_reg;");
      Contents.add("");
      Contents.add("   make_memory : PROCESS( clock , Reset , ClockEnable , Tick , D )");
      Contents.add("   BEGIN");
      Contents.add("      IF (Reset = '1') THEN s_state_reg <= (OTHERS => '0');");
      if (Netlist.IsFlipFlop(attrs)) {
        Contents.add("      ELSIF (" + ActiveLevelStr + " = 1) THEN");
        Contents.add("         IF (Clock'event AND (Clock = '1')) THEN");
        Contents.add("            IF (ClockEnable = '1' AND Tick = '1') THEN");
        Contents.add("               s_state_reg <= D;");
        Contents.add("            END IF;");
        Contents.add("         END IF;");
        Contents.add("      ELSIF (" + ActiveLevelStr + " = 0) THEN");
        Contents.add("         IF (Clock'event AND (Clock = '0')) THEN");
        Contents.add("         IF (ClockEnable = '1' AND Tick = '1') THEN");
        Contents.add("               s_state_reg <= D;");
        Contents.add("            END IF;");
        Contents.add("         END IF;");

        /////
        // Contents.add("      ELSIF (Clock'event AND (Clock = std_logic_vector(to_unsigned("
        //		+ ActiveLevelStr + ",1)) )) THEN");
      } else {
        Contents.add("      ELSIF (" + ActiveLevelStr + " = 1) THEN");
        Contents.add("         IF (Clock = '1') THEN");
        Contents.add("            IF (ClockEnable = '1' AND Tick = '1') THEN");
        Contents.add("               s_state_reg <= D;");
        Contents.add("            END IF;");
        Contents.add("         END IF;");
        Contents.add("      ELSIF (" + ActiveLevelStr + " = 0) THEN");
        Contents.add("         IF (Clock = '0') THEN");
        Contents.add("            IF (ClockEnable = '1' AND Tick = '1') THEN");
        Contents.add("               s_state_reg <= D;");
        Contents.add("            END IF;");
        Contents.add("         END IF;");
        // Contents.add("      ELSIF (Clock = std_logic_vector(to_unsigned("
        //		+ ActiveLevelStr + ",1)) ) THEN");
      }
      // Contents.add("         IF (ClockEnable = '1' AND Tick = '1') THEN");
      // Contents.add("            s_state_reg <= D;");
      // Contents.add("         END IF;");
      Contents.add("      END IF;");
      Contents.add("   END PROCESS make_memory;");
    } else {
      if (!Netlist.IsFlipFlop(attrs)) {
        Contents.add("   assign Q = s_state_reg;");
        Contents.add("");
        Contents.add("   always @(*)");
        Contents.add("   begin");
        Contents.add("      if (Reset) s_state_reg <= 0;");
        Contents.add(
            "      else if ((Clock==" + ActiveLevelStr + ")&ClockEnable&Tick) s_state_reg <= D;");
        Contents.add("   end");
      } else {
        Contents.add(
            "   assign Q = (" + ActiveLevelStr + ") ? s_state_reg : s_state_reg_neg_edge;");
        Contents.add("");
        Contents.add("   always @(posedge Clock or posedge Reset)");
        Contents.add("   begin");
        Contents.add("      if (Reset) s_state_reg <= 0;");
        Contents.add("      else if (ClockEnable&Tick) s_state_reg <= D;");
        Contents.add("   end");
        Contents.add("");
        Contents.add("   always @(negedge Clock or posedge Reset)");
        Contents.add("   begin");
        Contents.add("      if (Reset) s_state_reg_neg_edge <= 0;");
        Contents.add("      else if (ClockEnable&Tick) s_state_reg_neg_edge <= D;");
        Contents.add("   end");
      }
    }
    return Contents;
  }

  @Override
  public SortedMap<String, Integer> GetOutputList(Netlist TheNetlist, AttributeSet attrs) {
    SortedMap<String, Integer> Outputs = new TreeMap<String, Integer>();
    Outputs.put("Q", NrOfBitsId);
    return Outputs;
  }

  @Override
  public SortedMap<Integer, String> GetParameterList(AttributeSet attrs) {
    SortedMap<Integer, String> Parameters = new TreeMap<Integer, String>();
    Parameters.put(ActiveLevelId, ActiveLevelStr);
    Parameters.put(NrOfBitsId, NrOfBitsStr);
    return Parameters;
  }

  @Override
  public SortedMap<String, Integer> GetParameterMap(
      Netlist Nets, NetlistComponent ComponentInfo, FPGAReport Reporter) {
    SortedMap<String, Integer> ParameterMap = new TreeMap<String, Integer>();
    int ActiveLevel = 1;
    Boolean GatedClock = false;
    Boolean ActiveLow = false;
    AttributeSet attrs = ComponentInfo.GetComponent().getAttributeSet();
    String ClockNetName = GetClockNetName(ComponentInfo, Register.CK, Nets);
    if (ClockNetName.isEmpty()) {
      GatedClock = true;
      if (Netlist.IsFlipFlop(attrs))
        Reporter.AddWarning(
            "Found a gated clock for component \"Register\" in circuit \""
                + Nets.getCircuitName()
                + "\"");
    }
    if (attrs.getValue(StdAttr.TRIGGER) == StdAttr.TRIG_FALLING
        || attrs.getValue(StdAttr.TRIGGER) == StdAttr.TRIG_LOW) ActiveLow = true;

    if (GatedClock && ActiveLow) {
      ActiveLevel = 0;
    }
    ParameterMap.put(ActiveLevelStr, ActiveLevel);
    ParameterMap.put(
        NrOfBitsStr, ComponentInfo.GetComponent().getEnd(Register.IN).getWidth().getWidth());
    return ParameterMap;
  }

  @Override
  public SortedMap<String, String> GetPortMap(
	      Netlist Nets, Object MapInfo, FPGAReport Reporter, String HDLType) {
    SortedMap<String, String> PortMap = new TreeMap<String, String>();
    if (!(MapInfo instanceof NetlistComponent)) return PortMap;
    NetlistComponent ComponentInfo = (NetlistComponent) MapInfo;
    Boolean GatedClock = false;
    Boolean HasClock = true;
    Boolean ActiveLow = false;
    String ZeroBit = (HDLType.equals(VHDL)) ? "'0'" : "1'b0";
    String SetBit = (HDLType.equals(VHDL)) ? "'1'" : "1'b1";
    String BracketOpen = (HDLType.equals(VHDL)) ? "(" : "[";
    String BracketClose = (HDLType.equals(VHDL)) ? ")" : "]";
    AttributeSet attrs = ComponentInfo.GetComponent().getAttributeSet();
    if (!ComponentInfo.EndIsConnected(Register.CK)) {
      Reporter.AddSevereWarning(
          "Component \"Register\" in circuit \""
              + Nets.getCircuitName()
              + "\" has no clock connection");
      HasClock = false;
    }
    String ClockNetName = GetClockNetName(ComponentInfo, Register.CK, Nets);
    if (ClockNetName.isEmpty()) {
      GatedClock = true;
    }
    if (attrs.getValue(StdAttr.TRIGGER) == StdAttr.TRIG_FALLING
        || attrs.getValue(StdAttr.TRIGGER) == StdAttr.TRIG_LOW) ActiveLow = true;
    PortMap.putAll(GetNetMap("Reset", true, ComponentInfo, Register.CLR, Reporter, HDLType, Nets));
    PortMap.putAll(
        GetNetMap("ClockEnable", false, ComponentInfo, Register.EN, Reporter, HDLType, Nets));

    if (HasClock && !GatedClock && Netlist.IsFlipFlop(attrs)) {
      if (Nets.RequiresGlobalClockConnection()) {
        PortMap.put("Tick", SetBit);
      } else {
        if (ActiveLow)
          PortMap.put(
              "Tick",
              ClockNetName
                  + BracketOpen
                  + Integer.toString(ClockHDLGeneratorFactory.NegativeEdgeTickIndex)
                  + BracketClose);
        else
          PortMap.put(
              "Tick",
              ClockNetName
                  + BracketOpen
                  + Integer.toString(ClockHDLGeneratorFactory.PositiveEdgeTickIndex)
                  + BracketClose);
      }
      PortMap.put(
          "Clock",
          ClockNetName
              + BracketOpen
              + Integer.toString(ClockHDLGeneratorFactory.GlobalClockIndex)
              + BracketClose);
    } else if (!HasClock) {
      PortMap.put("Tick", ZeroBit);
      PortMap.put("Clock", ZeroBit);
    } else {
      PortMap.put("Tick", SetBit);
      if (!GatedClock) {
        if (ActiveLow)
          PortMap.put(
              "Clock",
              ClockNetName
                  + BracketOpen
                  + Integer.toString(ClockHDLGeneratorFactory.InvertedDerivedClockIndex)
                  + BracketClose);
        else
          PortMap.put(
              "Clock",
              ClockNetName
                  + BracketOpen
                  + Integer.toString(ClockHDLGeneratorFactory.DerivedClockIndex)
                  + BracketClose);
      } else {
        PortMap.put("Clock", GetNetName(ComponentInfo, Register.CK, true, HDLType, Nets));
      }
    }
    String Input = "D";
    String Output = "Q";
    if (HDLType.equals(VHDL)
        & (ComponentInfo.GetComponent().getAttributeSet().getValue(StdAttr.WIDTH).getWidth()
            == 1)) {
      Input += "(0)";
      Output += "(0)";
    }
    PortMap.putAll(GetNetMap(Input, true, ComponentInfo, Register.IN, Reporter, HDLType, Nets));
    PortMap.putAll(GetNetMap(Output, true, ComponentInfo, Register.OUT, Reporter, HDLType, Nets));
    return PortMap;
  }

  @Override
  public SortedMap<String, Integer> GetRegList(AttributeSet attrs, String HDLType) {
    SortedMap<String, Integer> Regs = new TreeMap<String, Integer>();
    Regs.put("s_state_reg", NrOfBitsId);
    if (HDLType.equals(VERILOG) & Netlist.IsFlipFlop(attrs))
      Regs.put("s_state_reg_neg_edge", NrOfBitsId);
    return Regs;
  }

  @Override
  public String GetSubDir() {
    return "memory";
  }

  @Override
  public boolean HDLTargetSupported(String HDLType, AttributeSet attrs) {
    return true;
  }
}
