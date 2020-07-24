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

public class RandomHDLGeneratorFactory extends AbstractHDLGeneratorFactory {

  private static final String NrOfBitsStr = "NrOfBits";
  private static final int NrOfBitsId = -1;
  private static final String SeedStr = "Seed";
  private static final int SeedId = -2;

  @Override
  public String getComponentStringIdentifier() {
    return "RNG";
  }

  @Override
  public SortedMap<String, Integer> GetInputList(Netlist TheNetlist, AttributeSet attrs) {
    SortedMap<String, Integer> Inputs = new TreeMap<String, Integer>();
    Inputs.put("GlobalClock", 1);
    Inputs.put("ClockEnable", 1);
    Inputs.put("clear", 1);
    Inputs.put("enable", 1);
    return Inputs;
  }

  @Override
  public ArrayList<String> GetModuleFunctionality(
      Netlist TheNetlist, AttributeSet attrs, FPGAReport Reporter, String HDLType) {
    ArrayList<String> Contents = new ArrayList<String>();
    Contents.addAll(
        MakeRemarkBlock("This is a multicycle implementation of the Random Component", 3, HDLType));
    Contents.add("");
    if (HDLType.equals(VHDL)) {
      Contents.add("   Q            <= s_output_reg;");
      Contents.add("   s_InitSeed   <= X\"0005DEECE66D\" WHEN " + SeedStr + " = 0 ELSE");
      Contents.add(
          "                   X\"0000\"&std_logic_vector(to_unsigned(" + SeedStr + ",32));");
      Contents.add("   s_reset      <= '1' WHEN s_reset_reg /= \"010\" ELSE '0';");
      Contents.add("   s_reset_next <= \"010\" WHEN (s_reset_reg = \"101\" OR");
      Contents.add("                               s_reset_reg = \"010\") AND");
      Contents.add("                               clear = '0' ELSE");
      Contents.add("                   \"101\" WHEN s_reset_reg = \"001\" ELSE");
      Contents.add("                   \"001\";");
      Contents.add("   s_start      <= '1' WHEN (ClockEnable = '1' AND enable = '1') OR");
      Contents.add("                            (s_reset_reg = \"101\" AND clear = '0') ELSE '0';");
      Contents.add("   s_mult_shift_next <= (OTHERS => '0') WHEN s_reset = '1' ELSE");
      Contents.add("                        X\"5DEECE66D\" WHEN s_start_reg = '1' ELSE");
      Contents.add("                        '0'&s_mult_shift_reg(35 DOWNTO 1);");
      Contents.add("   s_seed_shift_next <= (OTHERS => '0') WHEN s_reset = '1' ELSE");
      Contents.add("                        s_current_seed WHEN s_start_reg = '1' ELSE");
      Contents.add("                        s_seed_shift_reg(46 DOWNTO 0)&'0';");
      Contents.add("   s_mult_busy       <= '0' WHEN s_mult_shift_reg = X\"000000000\" ELSE '1';");
      Contents.add("");
      Contents.add("   s_mac_lo_in_1     <= (OTHERS => '0') WHEN s_start_reg = '1' OR");
      Contents.add("                                             s_reset = '1' ELSE");
      Contents.add("                        '0'&s_mac_lo_reg(23 DOWNTO 0);");
      Contents.add("   s_mac_lo_in_2     <= '0'&X\"00000B\"");
      Contents.add("                           WHEN s_start_reg = '1' ELSE");
      Contents.add("                        '0'&s_seed_shift_reg(23 DOWNTO 0) ");
      Contents.add("                           WHEN s_mult_shift_reg(0) = '1' ELSE");
      Contents.add("                        (OTHERS => '0');");
      Contents.add("   s_mac_hi_in_2     <= (OTHERS => '0') WHEN s_start_reg = '1' ELSE");
      Contents.add("                        s_mac_hi_reg;");
      Contents.add("   s_mac_hi_1_next   <= s_seed_shift_reg(47 DOWNTO 24) ");
      Contents.add("                           WHEN s_mult_shift_reg(0) = '1' ELSE");
      Contents.add("                        (OTHERS => '0');");
      Contents.add("   s_busy_pipe_next  <= \"00\" WHEN s_reset = '1' ELSE");
      Contents.add("                        s_busy_pipe_reg(0)&s_mult_busy;");
      Contents.add("");
      Contents.add("   make_current_seed : PROCESS( GlobalClock , s_busy_pipe_reg , s_reset )");
      Contents.add("   BEGIN");
      Contents.add("      IF (GlobalClock'event AND (GlobalClock = '1')) THEN");
      Contents.add("         IF (s_reset = '1') THEN s_current_seed <= s_InitSeed;");
      Contents.add("         ELSIF (s_busy_pipe_reg = \"10\") THEN");
      Contents.add("            s_current_seed <= s_mac_hi_reg&s_mac_lo_reg(23 DOWNTO 0 );");
      Contents.add("         END IF;");
      Contents.add("      END IF;");
      Contents.add("   END PROCESS make_current_seed;");
      Contents.add("   ");
      Contents.add("   make_shift_regs : PROCESS(GlobalClock,s_mult_shift_next,s_seed_shift_next,");
      Contents.add("                             s_mac_lo_in_1,s_mac_lo_in_2)");
      Contents.add("   BEGIN");
      Contents.add("      IF (GlobalClock'event AND (GlobalClock = '1')) THEN");
      Contents.add("         s_mult_shift_reg <= s_mult_shift_next;");
      Contents.add("         s_seed_shift_reg <= s_seed_shift_next;");
      Contents.add(
          "         s_mac_lo_reg     <= std_logic_vector(unsigned(s_mac_lo_in_1)+unsigned(s_mac_lo_in_2));");
      Contents.add("         s_mac_hi_1_reg   <= s_mac_hi_1_next;");
      Contents.add(
          "         s_mac_hi_reg     <= std_logic_vector(unsigned(s_mac_hi_1_reg)+unsigned(s_mac_hi_in_2)+");
      Contents.add("                             unsigned(s_mac_lo_reg(24 DOWNTO 24)));");
      Contents.add("         s_busy_pipe_reg  <= s_busy_pipe_next;");
      Contents.add("      END IF;");
      Contents.add("   END PROCESS make_shift_regs;");
      Contents.add("");
      Contents.add("   make_start_reg : PROCESS(GlobalClock,s_start)");
      Contents.add("   BEGIN");
      Contents.add("      IF (GlobalClock'event AND (GlobalClock = '1')) THEN");
      Contents.add("         s_start_reg <= s_start;");
      Contents.add("      END IF;");
      Contents.add("   END PROCESS make_start_reg;");
      Contents.add("");
      Contents.add("   make_reset_reg : PROCESS(GlobalClock,s_reset_next)");
      Contents.add("   BEGIN");
      Contents.add("      IF (GlobalClock'event AND (GlobalClock = '1')) THEN");
      Contents.add("         s_reset_reg <= s_reset_next;");
      Contents.add("      END IF;");
      Contents.add("   END PROCESS make_reset_reg;");
      Contents.add("");
      Contents.add("   make_output : PROCESS( GlobalClock , s_reset , s_InitSeed )");
      Contents.add("   BEGIN");
      Contents.add("      IF (GlobalClock'event AND (GlobalClock = '1')) THEN");
      Contents.add(
          "         IF (s_reset = '1') THEN s_output_reg <= s_InitSeed( ("
              + NrOfBitsStr
              + "-1) DOWNTO 0 );");
      Contents.add("         ELSIF (ClockEnable = '1' AND enable = '1') THEN");
      Contents.add(
          "            s_output_reg <= s_current_seed((" + NrOfBitsStr + "+11) DOWNTO 12);");
      Contents.add("         END IF;");
      Contents.add("      END IF;");
      Contents.add("   END PROCESS make_output;");
    } else {
      Contents.add("   assign Q = s_output_reg;");
      Contents.add("   assign s_InitSeed = (" + SeedStr + ") ? " + SeedStr + " : 48'h5DEECE66D;");
      Contents.add("   assign s_reset = (s_reset_reg==3'b010) ? 1'b1 : 1'b0;");
      Contents.add("   assign s_reset_next = (((s_reset_reg == 3'b101)|");
      Contents.add("                           (s_reset_reg == 3'b010))&clear) ? 3'b010 :");
      Contents.add("                         (s_reset_reg==3'b001) ? 3'b101 : 3'b001;");
      Contents.add(
          "   assign s_start = ((ClockEnable&enable)|((s_reset_reg == 3'b101)&clear)) ? 1'b1 : 1'b0;");
      Contents.add("   assign s_mult_shift_next = (s_reset) ? 36'd0 :");
      Contents.add(
          "                              (s_start_reg) ? 36'h5DEECE66D : {1'b0,s_mult_shift_reg[35:1]};");
      Contents.add("   assign s_seed_shift_next = (s_reset) ? 48'd0 :");
      Contents.add(
          "                              (s_start_reg) ? s_current_seed : {s_seed_shift_reg[46:0],1'b0};");
      Contents.add("   assign s_mult_busy = (s_mult_shift_reg == 0) ? 1'b0 : 1'b1;");
      Contents.add(
          "   assign s_mac_lo_in_1 = (s_start_reg|s_reset) ? 25'd0 : {1'b0,s_mac_lo_reg[23:0]};");
      Contents.add("   assign s_mac_lo_in_2 = (s_start_reg) ? 25'hB :");
      Contents.add(
          "                          (s_mult_shift_reg[0]) ? {1'b0,s_seed_shift_reg[23:0]} : 25'd0;");
      Contents.add("   assign s_mac_hi_in_2 = (s_start_reg) ? 0 : s_mac_hi_reg;");
      Contents.add(
          "   assign s_mac_hi_1_next = (s_mult_shift_reg[0]) ? s_seed_shift_reg[47:24] : 0;");
      Contents.add(
          "   assign s_busy_pipe_next = (s_reset) ? 2'd0 : {s_busy_pipe_reg[0],s_mult_busy};");
      Contents.add("");
      Contents.add("   always @(posedge GlobalClock)");
      Contents.add("   begin");
      Contents.add("      if (s_reset) s_current_seed <= s_InitSeed;");
      Contents.add(
          "      else if (s_busy_pipe_reg == 2'b10) s_current_seed <= {s_mac_hi_reg,s_mac_lo_reg[23:0]};");
      Contents.add("   end");
      Contents.add("");
      Contents.add("   always @(posedge GlobalClock)");
      Contents.add("   begin");
      Contents.add("         s_mult_shift_reg <= s_mult_shift_next;");
      Contents.add("         s_seed_shift_reg <= s_seed_shift_next;");
      Contents.add("         s_mac_lo_reg     <= s_mac_lo_in_1+s_mac_lo_in_2;");
      Contents.add("         s_mac_hi_1_reg   <= s_mac_hi_1_next;");
      Contents.add("         s_mac_hi_reg     <= s_mac_hi_1_reg+s_mac_hi_in_2+s_mac_lo_reg[24];");
      Contents.add("         s_busy_pipe_reg  <= s_busy_pipe_next;");
      Contents.add("         s_start_reg      <= s_start;");
      Contents.add("         s_reset_reg      <= s_reset_next;");
      Contents.add("   end");
      Contents.add("");
      Contents.add("   always @(posedge GlobalClock)");
      Contents.add("   begin");
      Contents.add("      if (s_reset) s_output_reg <= s_InitSeed[(" + NrOfBitsStr + "-1):0];");
      Contents.add(
          "      else if (ClockEnable&enable) s_output_reg <= s_current_seed[("
              + NrOfBitsStr
              + "+11):12];");
      Contents.add("   end");
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
    Parameters.put(NrOfBitsId, NrOfBitsStr);
    Parameters.put(SeedId, SeedStr);
    return Parameters;
  }

  @Override
  public SortedMap<String, Integer> GetParameterMap(
      Netlist Nets, NetlistComponent ComponentInfo, FPGAReport Reporter) {
    SortedMap<String, Integer> ParameterMap = new TreeMap<String, Integer>();
    int seed = ComponentInfo.GetComponent().getAttributeSet().getValue(Random.ATTR_SEED);
    if (seed == 0) seed = (int) System.currentTimeMillis();
    ParameterMap.put(
        NrOfBitsStr,
        ComponentInfo.GetComponent().getAttributeSet().getValue(StdAttr.WIDTH).getWidth());
    ParameterMap.put(SeedStr, seed);
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
    String BracketOpen = (HDLType.equals(VHDL)) ? "(" : "[";
    String BracketClose = (HDLType.equals(VHDL)) ? ")" : "]";
    if (!ComponentInfo.EndIsConnected(Random.CK)) {
      Reporter.AddSevereWarning(
          "Component \"Random\" in circuit \""
              + Nets.getCircuitName()
              + "\" has no clock connection");
      HasClock = false;
    }
    String ClockNetName = GetClockNetName(ComponentInfo, Random.CK, Nets);
    if (ClockNetName.isEmpty()) {
      GatedClock = true;
      Reporter.AddError(
          "Found a gated clock for component \"Random\" in circuit \""
              + Nets.getCircuitName()
              + "\"");
      Reporter.AddError("This RNG will not work!");
    }
    if (ComponentInfo.GetComponent().getAttributeSet().containsAttribute(StdAttr.EDGE_TRIGGER)) {
      ActiveLow =
          ComponentInfo.GetComponent().getAttributeSet().getValue(StdAttr.EDGE_TRIGGER)
              == StdAttr.TRIG_FALLING;
    }
    if (!HasClock || GatedClock) {
      PortMap.put("GlobalClock", ZeroBit);
      PortMap.put("ClockEnable", ZeroBit);
    } else {
      PortMap.put(
          "GlobalClock",
          ClockNetName
              + BracketOpen
              + Integer.toString(ClockHDLGeneratorFactory.GlobalClockIndex)
              + BracketClose);
      if (Nets.RequiresGlobalClockConnection()) {
        PortMap.put(
            "ClockEnable",
            ClockNetName
                + BracketOpen
                + Integer.toString(ClockHDLGeneratorFactory.GlobalClockIndex)
                + BracketClose);
      } else {
        if (ActiveLow)
          PortMap.put(
              "ClockEnable",
              ClockNetName
                  + BracketOpen
                  + Integer.toString(ClockHDLGeneratorFactory.NegativeEdgeTickIndex)
                  + BracketClose);
        else
          PortMap.put(
              "ClockEnable",
              ClockNetName
                  + BracketOpen
                  + Integer.toString(ClockHDLGeneratorFactory.PositiveEdgeTickIndex)
                  + BracketClose);
      }
    }
    PortMap.putAll(GetNetMap("clear", true, ComponentInfo, Random.RST, Reporter, HDLType, Nets));
    PortMap.putAll(GetNetMap("enable", false, ComponentInfo, Random.NXT, Reporter, HDLType, Nets));
    String output = "Q";
    if (HDLType.equals(VHDL)
        & (ComponentInfo.GetComponent().getAttributeSet().getValue(StdAttr.WIDTH).getWidth() == 1))
      output += "(0)";
    PortMap.putAll(GetNetMap(output, true, ComponentInfo, Random.OUT, Reporter, HDLType, Nets));
    return PortMap;
  }

  @Override
  public SortedMap<String, Integer> GetRegList(AttributeSet attrs, String HDLType) {
    SortedMap<String, Integer> Regs = new TreeMap<String, Integer>();
    Regs.put("s_current_seed", 48);
    Regs.put("s_reset_reg", 3);
    Regs.put("s_mult_shift_reg", 36);
    Regs.put("s_seed_shift_reg", 48);
    Regs.put("s_start_reg", 1);
    Regs.put("s_mac_lo_reg", 25);
    Regs.put("s_mac_hi_reg", 24);
    Regs.put("s_mac_hi_1_reg", 24);
    Regs.put("s_busy_pipe_reg", 2);
    Regs.put("s_output_reg", NrOfBitsId);
    return Regs;
  }

  @Override
  public String GetSubDir() {
    return "memory";
  }

  @Override
  public SortedMap<String, Integer> GetWireList(AttributeSet attrs, Netlist Nets) {
    SortedMap<String, Integer> Wires = new TreeMap<String, Integer>();
    Wires.put("s_InitSeed", 48);
    Wires.put("s_reset", 1);
    Wires.put("s_reset_next", 3);
    Wires.put("s_mult_shift_next", 36);
    Wires.put("s_seed_shift_next", 48);
    Wires.put("s_mult_busy", 1);
    Wires.put("s_start", 1);
    Wires.put("s_mac_lo_in_1", 25);
    Wires.put("s_mac_lo_in_2", 25);
    Wires.put("s_mac_hi_1_next", 24);
    Wires.put("s_mac_hi_in_2", 24);
    Wires.put("s_busy_pipe_next", 2);
    return Wires;
  }

  @Override
  public boolean HDLTargetSupported(String HDLType, AttributeSet attrs) {
    return true;
  }
}
