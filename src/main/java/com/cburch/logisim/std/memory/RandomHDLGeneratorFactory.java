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
  public SortedMap<String, Integer> GetInputList(Netlist nets, AttributeSet attrs) {
    final var map = new TreeMap<String, Integer>();
    map.put("GlobalClock", 1);
    map.put("ClockEnable", 1);
    map.put("clear", 1);
    map.put("enable", 1);
    return map;
  }

  @Override
  public ArrayList<String> GetModuleFunctionality(Netlist nets, AttributeSet attrs) {
    final var contents = new ArrayList<String>();
    contents.addAll(
        MakeRemarkBlock("This is a multicycle implementation of the Random Component", 3));
    contents.add("");
    if (HDL.isVHDL()) {
      contents.add("   Q            <= s_output_reg;");
      contents.add("   s_InitSeed   <= X\"0005DEECE66D\" WHEN " + SeedStr + " = 0 ELSE");
      contents.add(
          "                   X\"0000\"&std_logic_vector(to_unsigned(" + SeedStr + ",32));");
      contents.add("   s_reset      <= '1' WHEN s_reset_reg /= \"010\" ELSE '0';");
      contents.add("   s_reset_next <= \"010\" WHEN (s_reset_reg = \"101\" OR");
      contents.add("                               s_reset_reg = \"010\") AND");
      contents.add("                               clear = '0' ELSE");
      contents.add("                   \"101\" WHEN s_reset_reg = \"001\" ELSE");
      contents.add("                   \"001\";");
      contents.add("   s_start      <= '1' WHEN (ClockEnable = '1' AND enable = '1') OR");
      contents.add("                            (s_reset_reg = \"101\" AND clear = '0') ELSE '0';");
      contents.add("   s_mult_shift_next <= (OTHERS => '0') WHEN s_reset = '1' ELSE");
      contents.add("                        X\"5DEECE66D\" WHEN s_start_reg = '1' ELSE");
      contents.add("                        '0'&s_mult_shift_reg(35 DOWNTO 1);");
      contents.add("   s_seed_shift_next <= (OTHERS => '0') WHEN s_reset = '1' ELSE");
      contents.add("                        s_current_seed WHEN s_start_reg = '1' ELSE");
      contents.add("                        s_seed_shift_reg(46 DOWNTO 0)&'0';");
      contents.add("   s_mult_busy       <= '0' WHEN s_mult_shift_reg = X\"000000000\" ELSE '1';");
      contents.add("");
      contents.add("   s_mac_lo_in_1     <= (OTHERS => '0') WHEN s_start_reg = '1' OR");
      contents.add("                                             s_reset = '1' ELSE");
      contents.add("                        '0'&s_mac_lo_reg(23 DOWNTO 0);");
      contents.add("   s_mac_lo_in_2     <= '0'&X\"00000B\"");
      contents.add("                           WHEN s_start_reg = '1' ELSE");
      contents.add("                        '0'&s_seed_shift_reg(23 DOWNTO 0) ");
      contents.add("                           WHEN s_mult_shift_reg(0) = '1' ELSE");
      contents.add("                        (OTHERS => '0');");
      contents.add("   s_mac_hi_in_2     <= (OTHERS => '0') WHEN s_start_reg = '1' ELSE");
      contents.add("                        s_mac_hi_reg;");
      contents.add("   s_mac_hi_1_next   <= s_seed_shift_reg(47 DOWNTO 24) ");
      contents.add("                           WHEN s_mult_shift_reg(0) = '1' ELSE");
      contents.add("                        (OTHERS => '0');");
      contents.add("   s_busy_pipe_next  <= \"00\" WHEN s_reset = '1' ELSE");
      contents.add("                        s_busy_pipe_reg(0)&s_mult_busy;");
      contents.add("");
      contents.add("   make_current_seed : PROCESS( GlobalClock , s_busy_pipe_reg , s_reset )");
      contents.add("   BEGIN");
      contents.add("      IF (GlobalClock'event AND (GlobalClock = '1')) THEN");
      contents.add("         IF (s_reset = '1') THEN s_current_seed <= s_InitSeed;");
      contents.add("         ELSIF (s_busy_pipe_reg = \"10\") THEN");
      contents.add("            s_current_seed <= s_mac_hi_reg&s_mac_lo_reg(23 DOWNTO 0 );");
      contents.add("         END IF;");
      contents.add("      END IF;");
      contents.add("   END PROCESS make_current_seed;");
      contents.add("   ");
      contents.add("   make_shift_regs : PROCESS(GlobalClock,s_mult_shift_next,s_seed_shift_next,");
      contents.add("                             s_mac_lo_in_1,s_mac_lo_in_2)");
      contents.add("   BEGIN");
      contents.add("      IF (GlobalClock'event AND (GlobalClock = '1')) THEN");
      contents.add("         s_mult_shift_reg <= s_mult_shift_next;");
      contents.add("         s_seed_shift_reg <= s_seed_shift_next;");
      contents.add(
          "         s_mac_lo_reg     <= std_logic_vector(unsigned(s_mac_lo_in_1)+unsigned(s_mac_lo_in_2));");
      contents.add("         s_mac_hi_1_reg   <= s_mac_hi_1_next;");
      contents.add(
          "         s_mac_hi_reg     <= std_logic_vector(unsigned(s_mac_hi_1_reg)+unsigned(s_mac_hi_in_2)+");
      contents.add("                             unsigned(s_mac_lo_reg(24 DOWNTO 24)));");
      contents.add("         s_busy_pipe_reg  <= s_busy_pipe_next;");
      contents.add("      END IF;");
      contents.add("   END PROCESS make_shift_regs;");
      contents.add("");
      contents.add("   make_start_reg : PROCESS(GlobalClock,s_start)");
      contents.add("   BEGIN");
      contents.add("      IF (GlobalClock'event AND (GlobalClock = '1')) THEN");
      contents.add("         s_start_reg <= s_start;");
      contents.add("      END IF;");
      contents.add("   END PROCESS make_start_reg;");
      contents.add("");
      contents.add("   make_reset_reg : PROCESS(GlobalClock,s_reset_next)");
      contents.add("   BEGIN");
      contents.add("      IF (GlobalClock'event AND (GlobalClock = '1')) THEN");
      contents.add("         s_reset_reg <= s_reset_next;");
      contents.add("      END IF;");
      contents.add("   END PROCESS make_reset_reg;");
      contents.add("");
      contents.add("   make_output : PROCESS( GlobalClock , s_reset , s_InitSeed )");
      contents.add("   BEGIN");
      contents.add("      IF (GlobalClock'event AND (GlobalClock = '1')) THEN");
      contents.add(
          "         IF (s_reset = '1') THEN s_output_reg <= s_InitSeed( ("
              + NrOfBitsStr
              + "-1) DOWNTO 0 );");
      contents.add("         ELSIF (ClockEnable = '1' AND enable = '1') THEN");
      contents.add(
          "            s_output_reg <= s_current_seed((" + NrOfBitsStr + "+11) DOWNTO 12);");
      contents.add("         END IF;");
      contents.add("      END IF;");
      contents.add("   END PROCESS make_output;");
    } else {
      contents.add("   assign Q = s_output_reg;");
      contents.add("   assign s_InitSeed = (" + SeedStr + ") ? " + SeedStr + " : 48'h5DEECE66D;");
      contents.add("   assign s_reset = (s_reset_reg==3'b010) ? 1'b1 : 1'b0;");
      contents.add("   assign s_reset_next = (((s_reset_reg == 3'b101)|");
      contents.add("                           (s_reset_reg == 3'b010))&clear) ? 3'b010 :");
      contents.add("                         (s_reset_reg==3'b001) ? 3'b101 : 3'b001;");
      contents.add(
          "   assign s_start = ((ClockEnable&enable)|((s_reset_reg == 3'b101)&clear)) ? 1'b1 : 1'b0;");
      contents.add("   assign s_mult_shift_next = (s_reset) ? 36'd0 :");
      contents.add(
          "                              (s_start_reg) ? 36'h5DEECE66D : {1'b0,s_mult_shift_reg[35:1]};");
      contents.add("   assign s_seed_shift_next = (s_reset) ? 48'd0 :");
      contents.add(
          "                              (s_start_reg) ? s_current_seed : {s_seed_shift_reg[46:0],1'b0};");
      contents.add("   assign s_mult_busy = (s_mult_shift_reg == 0) ? 1'b0 : 1'b1;");
      contents.add(
          "   assign s_mac_lo_in_1 = (s_start_reg|s_reset) ? 25'd0 : {1'b0,s_mac_lo_reg[23:0]};");
      contents.add("   assign s_mac_lo_in_2 = (s_start_reg) ? 25'hB :");
      contents.add(
          "                          (s_mult_shift_reg[0]) ? {1'b0,s_seed_shift_reg[23:0]} : 25'd0;");
      contents.add("   assign s_mac_hi_in_2 = (s_start_reg) ? 0 : s_mac_hi_reg;");
      contents.add(
          "   assign s_mac_hi_1_next = (s_mult_shift_reg[0]) ? s_seed_shift_reg[47:24] : 0;");
      contents.add(
          "   assign s_busy_pipe_next = (s_reset) ? 2'd0 : {s_busy_pipe_reg[0],s_mult_busy};");
      contents.add("");
      contents.add("   always @(posedge GlobalClock)");
      contents.add("   begin");
      contents.add("      if (s_reset) s_current_seed <= s_InitSeed;");
      contents.add(
          "      else if (s_busy_pipe_reg == 2'b10) s_current_seed <= {s_mac_hi_reg,s_mac_lo_reg[23:0]};");
      contents.add("   end");
      contents.add("");
      contents.add("   always @(posedge GlobalClock)");
      contents.add("   begin");
      contents.add("         s_mult_shift_reg <= s_mult_shift_next;");
      contents.add("         s_seed_shift_reg <= s_seed_shift_next;");
      contents.add("         s_mac_lo_reg     <= s_mac_lo_in_1+s_mac_lo_in_2;");
      contents.add("         s_mac_hi_1_reg   <= s_mac_hi_1_next;");
      contents.add("         s_mac_hi_reg     <= s_mac_hi_1_reg+s_mac_hi_in_2+s_mac_lo_reg[24];");
      contents.add("         s_busy_pipe_reg  <= s_busy_pipe_next;");
      contents.add("         s_start_reg      <= s_start;");
      contents.add("         s_reset_reg      <= s_reset_next;");
      contents.add("   end");
      contents.add("");
      contents.add("   always @(posedge GlobalClock)");
      contents.add("   begin");
      contents.add("      if (s_reset) s_output_reg <= s_InitSeed[(" + NrOfBitsStr + "-1):0];");
      contents.add(
          "      else if (ClockEnable&enable) s_output_reg <= s_current_seed[("
              + NrOfBitsStr
              + "+11):12];");
      contents.add("   end");
    }
    return contents;
  }

  @Override
  public SortedMap<String, Integer> GetOutputList(Netlist TheNetlist, AttributeSet attrs) {
    final var map = new TreeMap<String, Integer>();
    map.put("Q", NrOfBitsId);
    return map;
  }

  @Override
  public SortedMap<Integer, String> GetParameterList(AttributeSet attrs) {
    final var map = new TreeMap<Integer, String>();
    map.put(NrOfBitsId, NrOfBitsStr);
    map.put(SeedId, SeedStr);
    return map;
  }

  @Override
  public SortedMap<String, Integer> GetParameterMap(Netlist Nets, NetlistComponent ComponentInfo) {
    final var map = new TreeMap<String, Integer>();
    var seed = ComponentInfo.GetComponent().getAttributeSet().getValue(Random.ATTR_SEED);
    if (seed == 0) seed = (int) System.currentTimeMillis();
    map.put(
        NrOfBitsStr,
        ComponentInfo.GetComponent().getAttributeSet().getValue(StdAttr.WIDTH).getWidth());
    map.put(SeedStr, seed);
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
    if (!comp.EndIsConnected(Random.CK)) {
      Reporter.Report.AddSevereWarning(
          "Component \"Random\" in circuit \""
              + Nets.getCircuitName()
              + "\" has no clock connection");
      hasClock = false;
    }
    final var clockNetName = GetClockNetName(comp, Random.CK, Nets);
    if (clockNetName.isEmpty()) {
      gatedClock = true;
      Reporter.Report.AddError(
          "Found a gated clock for component \"Random\" in circuit \""
              + Nets.getCircuitName()
              + "\"");
      Reporter.Report.AddError("This RNG will not work!");
    }
    if (comp.GetComponent().getAttributeSet().containsAttribute(StdAttr.EDGE_TRIGGER)) {
      activeLow =
          comp.GetComponent().getAttributeSet().getValue(StdAttr.EDGE_TRIGGER)
              == StdAttr.TRIG_FALLING;
    }
    if (!hasClock || gatedClock) {
      map.put("GlobalClock", HDL.zeroBit());
      map.put("ClockEnable", HDL.zeroBit());
    } else {
      map.put(
          "GlobalClock",
          clockNetName
              + HDL.BracketOpen()
              + ClockHDLGeneratorFactory.GlobalClockIndex
              + HDL.BracketClose());
      if (Nets.RequiresGlobalClockConnection()) {
        map.put(
            "ClockEnable",
            clockNetName
                + HDL.BracketOpen()
                + ClockHDLGeneratorFactory.GlobalClockIndex
                + HDL.BracketClose());
      } else {
        if (activeLow)
          map.put(
              "ClockEnable",
              clockNetName
                  + HDL.BracketOpen()
                  + ClockHDLGeneratorFactory.NegativeEdgeTickIndex
                  + HDL.BracketClose());
        else
          map.put(
              "ClockEnable",
              clockNetName
                  + HDL.BracketOpen()
                  + ClockHDLGeneratorFactory.PositiveEdgeTickIndex
                  + HDL.BracketClose());
      }
    }
    map.putAll(GetNetMap("clear", true, comp, Random.RST, Nets));
    map.putAll(GetNetMap("enable", false, comp, Random.NXT, Nets));
    var output = "Q";
    if (HDL.isVHDL()
        & (comp.GetComponent().getAttributeSet().getValue(StdAttr.WIDTH).getWidth() == 1))
      output += "(0)";
    map.putAll(GetNetMap(output, true, comp, Random.OUT, Nets));
    return map;
  }

  @Override
  public SortedMap<String, Integer> GetRegList(AttributeSet attrs) {
    final var map = new TreeMap<String, Integer>();
    map.put("s_current_seed", 48);
    map.put("s_reset_reg", 3);
    map.put("s_mult_shift_reg", 36);
    map.put("s_seed_shift_reg", 48);
    map.put("s_start_reg", 1);
    map.put("s_mac_lo_reg", 25);
    map.put("s_mac_hi_reg", 24);
    map.put("s_mac_hi_1_reg", 24);
    map.put("s_busy_pipe_reg", 2);
    map.put("s_output_reg", NrOfBitsId);
    return map;
  }

  @Override
  public String GetSubDir() {
    return "memory";
  }

  @Override
  public SortedMap<String, Integer> GetWireList(AttributeSet attrs, Netlist Nets) {
    final var map = new TreeMap<String, Integer>();
    map.put("s_InitSeed", 48);
    map.put("s_reset", 1);
    map.put("s_reset_next", 3);
    map.put("s_mult_shift_next", 36);
    map.put("s_seed_shift_next", 48);
    map.put("s_mult_busy", 1);
    map.put("s_start", 1);
    map.put("s_mac_lo_in_1", 25);
    map.put("s_mac_lo_in_2", 25);
    map.put("s_mac_hi_1_next", 24);
    map.put("s_mac_hi_in_2", 24);
    map.put("s_busy_pipe_next", 2);
    return map;
  }

  @Override
  public boolean HDLTargetSupported(AttributeSet attrs) {
    return true;
  }
}
