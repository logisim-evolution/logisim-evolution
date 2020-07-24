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

public class CounterHDLGeneratorFactory extends AbstractHDLGeneratorFactory {

  private static final String NrOfBitsStr = "width";
  private static final int NrOfBitsId = -1;
  private static final String MaxValStr = "max_val";
  private static final int MaxValId = -2;
  private static final String ActiveEdgeStr = "ClkEdge";
  private static final int ActiveEdgeId = -3;
  private static final String ModeStr = "mode";
  private static final int ModeId = -4;

  @Override
  public String getComponentStringIdentifier() {
    return "COUNTER";
  }

  @Override
  public SortedMap<String, Integer> GetInputList(Netlist TheNetlist, AttributeSet attrs) {
    SortedMap<String, Integer> Inputs = new TreeMap<String, Integer>();
    Inputs.put("GlobalClock", 1);
    Inputs.put("ClockEnable", 1);
    Inputs.put("LoadData", NrOfBitsId);
    Inputs.put("clear", 1);
    Inputs.put("load", 1);
    Inputs.put("Up_n_Down", 1);
    Inputs.put("Enable", 1);
    return Inputs;
  }

  @Override
  public ArrayList<String> GetModuleFunctionality(
      Netlist TheNetlist, AttributeSet attrs, FPGAReport Reporter, String HDLType) {
    ArrayList<String> Contents = new ArrayList<String>();
    Contents.addAll(
        MakeRemarkBlock(
            "Functionality of the counter:\\ __Load_Count_|_mode\\ ____0____0___|_halt\\ "
                + "____0____1___|_count_up_(default)\\ ____1____0___|load\\ ____1____1___|_count_down",
            3,
            HDLType));
    if (HDLType.equals(VHDL)) {
      Contents.add("");
      Contents.add("   CompareOut   <= s_carry;");
      Contents.add("   CountValue   <= s_counter_value;");
      Contents.add("");
      Contents.add("   make_carry : PROCESS( Up_n_Down ,");
      Contents.add("                         s_counter_value )");
      Contents.add("   BEGIN");
      Contents.add("      IF (Up_n_Down = '0') THEN");
      Contents.add("         IF (s_counter_value = std_logic_vector(to_unsigned(0,width))) THEN");
      Contents.add("            s_carry <= '1';");
      Contents.add("                                                               ELSE");
      Contents.add("            s_carry <= '0';");
      Contents.add("         END IF; -- Down counting");
      Contents.add("                           ELSE");
      Contents.add(
          "         IF (s_counter_value = std_logic_vector(to_unsigned(max_val,width))) THEN");
      Contents.add("            s_carry <= '1';");
      Contents.add("                                                                     ELSE");
      Contents.add("            s_carry <= '0';");
      Contents.add("         END IF; -- Up counting");
      Contents.add("      END IF;");
      Contents.add("   END PROCESS make_carry;");
      Contents.add("");
      Contents.add(
          "   s_real_enable <= '0' WHEN (load = '0' AND enable = '0') -- Counter disabled");
      Contents.add("                             OR");
      Contents.add(
          "                             (mode = 1 AND s_carry = '1' AND load = '0') -- Stay at value situation");
      Contents.add("                        ELSE ClockEnable;");
      Contents.add("");
      Contents.add("   make_next_value : PROCESS( load , Up_n_Down , s_counter_value ,");
      Contents.add("                              LoadData , s_carry )");
      Contents.add("      VARIABLE v_downcount : std_logic;         ");
      Contents.add("   BEGIN");
      Contents.add("      v_downcount := NOT(Up_n_Down);");
      Contents.add("      IF ((load = '1') OR -- load condition");
      Contents.add("          (mode = 3 AND s_carry = '1')    -- Wrap load condition");
      Contents.add("         ) THEN s_next_counter_value <= LoadData;");
      Contents.add("           ELSE");
      Contents.add("         CASE (mode) IS");
      Contents.add("            WHEN  0     => IF (s_carry = '1') THEN");
      Contents.add("                              IF (v_downcount = '1') THEN ");
      Contents.add(
          "                                 s_next_counter_value <= std_logic_vector(to_unsigned(max_val,width));");
      Contents.add("                                                     ELSE ");
      Contents.add("                                 s_next_counter_value <= (OTHERS => '0');");
      Contents.add("                              END IF;");
      Contents.add("                                              ELSE");
      Contents.add("                              IF (v_downcount = '1') THEN ");
      Contents.add(
          "                                 s_next_counter_value <= std_logic_vector(unsigned(s_counter_value) - 1);");
      Contents.add("                                                     ELSE ");
      Contents.add(
          "                                 s_next_counter_value <= std_logic_vector(unsigned(s_counter_value) + 1);");
      Contents.add("                              END IF;");
      Contents.add("                           END IF;");
      Contents.add("            WHEN OTHERS => IF (v_downcount = '1') THEN ");
      Contents.add(
          "                              s_next_counter_value <= std_logic_vector(unsigned(s_counter_value) - 1);");
      Contents.add("                                                  ELSE ");
      Contents.add(
          "                              s_next_counter_value <= std_logic_vector(unsigned(s_counter_value) + 1);");
      Contents.add("                           END IF;");
      Contents.add("         END CASE;");
      Contents.add("      END IF;");
      Contents.add("   END PROCESS make_next_value;");
      Contents.add("");
      Contents.add(
          "   make_flops : PROCESS( GlobalClock , s_real_enable , clear , s_next_counter_value )");
      Contents.add("      VARIABLE temp : std_logic_vector(0 DOWNTO 0);");
      Contents.add("   BEGIN");
      Contents.add("      temp := std_logic_vector(to_unsigned(" + ActiveEdgeStr + ",1));");
      Contents.add("      IF (clear = '1') THEN s_counter_value <= (OTHERS => '0');");
      Contents.add("      ELSIF (GlobalClock'event AND (GlobalClock = temp(0))) THEN");
      Contents.add(
          "         IF (s_real_enable = '1') THEN s_counter_value <= s_next_counter_value;");
      Contents.add("         END IF;");
      Contents.add("      END IF;");
      Contents.add("   END PROCESS make_flops;");
    } else {
      Contents.add("");
      Contents.add("   assign CompareOut = s_carry;");
      Contents.add(
          "   assign CountValue = ("
              + ActiveEdgeStr
              + ") ? s_counter_value : s_counter_value_neg_edge;");
      Contents.add("");
      Contents.add("   always@(*)");
      Contents.add("   begin");
      Contents.add("      if (Up_n_Down)");
      Contents.add("         begin");
      Contents.add("            if (" + ActiveEdgeStr + ")");
      Contents.add("               s_carry = (s_counter_value == max_val) ? 1'b1 : 1'b0;");
      Contents.add("            else");
      Contents.add("               s_carry = (s_counter_value_neg_edge == max_val) ? 1'b1 : 1'b0;");
      Contents.add("         end");
      Contents.add("      else");
      Contents.add("         begin");
      Contents.add("            if (" + ActiveEdgeStr + ")");
      Contents.add("               s_carry = (s_counter_value == 0) ? 1'b1 : 1'b0;");
      Contents.add("            else");
      Contents.add("               s_carry = (s_counter_value_neg_edge == 0) ? 1'b1 : 1'b0;");
      Contents.add("         end");
      Contents.add("   end");
      Contents.add("");
      Contents.add("   assign s_real_enable = ((~(load)&~(Enable))|");
      Contents.add("                           ((mode==1)&s_carry&~(load))) ? 1'b0 : ClockEnable;");
      Contents.add("");
      Contents.add("   always @(*)");
      Contents.add("   begin");
      Contents.add("      if ((load)|((mode==3)&s_carry))");
      Contents.add("         s_next_counter_value = LoadData;");
      Contents.add("      else if ((mode==0)&s_carry&Up_n_Down)");
      Contents.add("         s_next_counter_value = 0;");
      Contents.add("      else if ((mode==0)&s_carry)");
      Contents.add("         s_next_counter_value = max_val;");
      Contents.add("      else if (Up_n_Down)");
      Contents.add("         begin");
      Contents.add("            if (" + ActiveEdgeStr + ")");
      Contents.add("               s_next_counter_value = s_counter_value + 1;");
      Contents.add("            else");
      Contents.add("               s_next_counter_value = s_counter_value_neg_edge + 1;");
      Contents.add("         end");
      Contents.add("      else");
      Contents.add("         begin");
      Contents.add("            if (" + ActiveEdgeStr + ")");
      Contents.add("               s_next_counter_value = s_counter_value - 1;");
      Contents.add("            else");
      Contents.add("               s_next_counter_value = s_counter_value_neg_edge - 1;");
      Contents.add("         end");
      Contents.add("   end");
      Contents.add("");
      Contents.add("   always @(posedge GlobalClock or posedge clear)");
      Contents.add("   begin");
      Contents.add("       if (clear) s_counter_value <= 0;");
      Contents.add("       else if (s_real_enable) s_counter_value <= s_next_counter_value;");
      Contents.add("   end");
      Contents.add("");
      Contents.add("   always @(negedge GlobalClock or posedge clear)");
      Contents.add("   begin");
      Contents.add("       if (clear) s_counter_value_neg_edge <= 0;");
      Contents.add(
          "       else if (s_real_enable) s_counter_value_neg_edge <= s_next_counter_value;");
      Contents.add("   end");
      Contents.add("");
    }
    return Contents;
  }

  @Override
  public SortedMap<String, Integer> GetOutputList(Netlist TheNetlist, AttributeSet attrs) {
    SortedMap<String, Integer> Outputs = new TreeMap<String, Integer>();
    Outputs.put("CountValue", NrOfBitsId);
    Outputs.put("CompareOut", 1);
    return Outputs;
  }

  @Override
  public SortedMap<Integer, String> GetParameterList(AttributeSet attrs) {
    SortedMap<Integer, String> Parameters = new TreeMap<Integer, String>();
    Parameters.put(NrOfBitsId, NrOfBitsStr);
    Parameters.put(MaxValId, MaxValStr);
    Parameters.put(ActiveEdgeId, ActiveEdgeStr);
    Parameters.put(ModeId, ModeStr);
    return Parameters;
  }

  @Override
  public SortedMap<String, Integer> GetParameterMap(
      Netlist Nets, NetlistComponent ComponentInfo, FPGAReport Reporter) {
    SortedMap<String, Integer> ParameterMap = new TreeMap<String, Integer>();
    AttributeSet attrs = ComponentInfo.GetComponent().getAttributeSet();
    int mode = 0;
    if (attrs.containsAttribute(Counter.ATTR_ON_GOAL)) {
      if (attrs.getValue(Counter.ATTR_ON_GOAL) == Counter.ON_GOAL_STAY) mode = 1;
      else if (attrs.getValue(Counter.ATTR_ON_GOAL) == Counter.ON_GOAL_CONT) mode = 2;
      else if (attrs.getValue(Counter.ATTR_ON_GOAL) == Counter.ON_GOAL_LOAD) mode = 3;
    } else {
      mode = 1;
    }
    ParameterMap.put(NrOfBitsStr, attrs.getValue(StdAttr.WIDTH).getWidth());
    ParameterMap.put(MaxValStr, attrs.getValue(Counter.ATTR_MAX).intValue());
    int ClkEdge = 1;
    if (GetClockNetName(ComponentInfo, Counter.CK, Nets).isEmpty()
        && attrs.getValue(StdAttr.EDGE_TRIGGER) == StdAttr.TRIG_FALLING) ClkEdge = 0;
    ParameterMap.put(ActiveEdgeStr, ClkEdge);
    ParameterMap.put(ModeStr, mode);
    return ParameterMap;
  }

  @Override
  public SortedMap<String, String> GetPortMap(
	      Netlist Nets, Object MapInfo, FPGAReport Reporter, String HDLType) {
    SortedMap<String, String> PortMap = new TreeMap<String, String>();
    if (!(MapInfo instanceof NetlistComponent)) return PortMap;
    NetlistComponent ComponentInfo = (NetlistComponent) MapInfo;
    String ZeroBit = (HDLType.equals(VHDL)) ? "'0'" : "1'b0";
    String SetBit = (HDLType.equals(VHDL)) ? "'1'" : "1'b1";
    String BracketOpen = (HDLType.equals(VHDL)) ? "(" : "[";
    String BracketClose = (HDLType.equals(VHDL)) ? ")" : "]";
    AttributeSet attrs = ComponentInfo.GetComponent().getAttributeSet();
    if (!ComponentInfo.EndIsConnected(Counter.CK)) {
      Reporter.AddSevereWarning(
          "Component \"Counter\" in circuit \""
              + Nets.getCircuitName()
              + "\" has no clock connection");
      PortMap.put("GlobalClock", ZeroBit);
      PortMap.put("ClockEnable", ZeroBit);
    } else {
      String ClockNetName = GetClockNetName(ComponentInfo, Counter.CK, Nets);
      if (ClockNetName.isEmpty()) {
        PortMap.putAll(
            GetNetMap("GlobalClock", true, ComponentInfo, Counter.CK, Reporter, HDLType, Nets));
        PortMap.put("ClockEnable", SetBit);
      } else {
        int ClockBusIndex = ClockHDLGeneratorFactory.DerivedClockIndex;
        if (Nets.RequiresGlobalClockConnection()) {
          ClockBusIndex = ClockHDLGeneratorFactory.GlobalClockIndex;
        } else {
          if (attrs.getValue(StdAttr.EDGE_TRIGGER) == StdAttr.TRIG_LOW)
            ClockBusIndex = ClockHDLGeneratorFactory.InvertedDerivedClockIndex;
          else if (attrs.getValue(StdAttr.EDGE_TRIGGER) == StdAttr.TRIG_RISING)
            ClockBusIndex = ClockHDLGeneratorFactory.PositiveEdgeTickIndex;
          else if (attrs.getValue(StdAttr.EDGE_TRIGGER) == StdAttr.TRIG_FALLING)
            ClockBusIndex = ClockHDLGeneratorFactory.InvertedDerivedClockIndex;
        }
        PortMap.put(
            "GlobalClock",
            ClockNetName
                + BracketOpen
                + Integer.toString(ClockHDLGeneratorFactory.GlobalClockIndex)
                + BracketClose);
        PortMap.put(
            "ClockEnable",
            ClockNetName + BracketOpen + Integer.toString(ClockBusIndex) + BracketClose);
      }
    }
    String Input = "LoadData";
    if (HDLType.equals(VHDL)
        & (ComponentInfo.GetComponent().getAttributeSet().getValue(StdAttr.WIDTH).getWidth() == 1))
      Input += "(0)";
    PortMap.putAll(GetNetMap(Input, true, ComponentInfo, Counter.IN, Reporter, HDLType, Nets));
    PortMap.putAll(GetNetMap("clear", true, ComponentInfo, Counter.CLR, Reporter, HDLType, Nets));
    PortMap.putAll(GetNetMap("load", true, ComponentInfo, Counter.LD, Reporter, HDLType, Nets));
    PortMap.putAll(GetNetMap("Enable", false, ComponentInfo, Counter.EN, Reporter, HDLType, Nets));
    PortMap.putAll(
        GetNetMap("Up_n_Down", false, ComponentInfo, Counter.UD, Reporter, HDLType, Nets));
    String Output = "CountValue";
    if (HDLType.equals(VHDL)
        & (ComponentInfo.GetComponent().getAttributeSet().getValue(StdAttr.WIDTH).getWidth() == 1))
      Output += "(0)";
    PortMap.putAll(GetNetMap(Output, true, ComponentInfo, Counter.OUT, Reporter, HDLType, Nets));
    PortMap.putAll(
        GetNetMap("CompareOut", true, ComponentInfo, Counter.CARRY, Reporter, HDLType, Nets));
    return PortMap;
  }

  @Override
  public SortedMap<String, Integer> GetRegList(AttributeSet attrs, String HDLType) {
    SortedMap<String, Integer> Regs = new TreeMap<String, Integer>();
    Regs.put("s_next_counter_value", NrOfBitsId); // for verilog generation
    // in explicite process
    Regs.put("s_carry", 1); // for verilog generation in explicite process
    Regs.put("s_counter_value", NrOfBitsId);
    if (HDLType.equals(VERILOG)) Regs.put("s_counter_value_neg_edge", NrOfBitsId);
    return Regs;
  }

  @Override
  public String GetSubDir() {
    /*
     * this method returns the module directory where the HDL code needs to
     * be placed
     */
    return "memory";
  }

  @Override
  public SortedMap<String, Integer> GetWireList(AttributeSet attrs, Netlist Nets) {
    SortedMap<String, Integer> Wires = new TreeMap<String, Integer>();
    Wires.put("s_real_enable", 1);
    return Wires;
  }

  @Override
  public boolean HDLTargetSupported(String HDLType, AttributeSet attrs) {
    return true;
  }
}
