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
import java.util.SortedMap;
import java.util.TreeMap;

public class RandomHDLGeneratorFactory extends AbstractHDLGeneratorFactory {

  private static final String NR_OF_BITS_STR = "NrOfBits";
  private static final int NrOfBitsId = -1;
  private static final String SEED_STR = "Seed";
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
    final var contents =
        (new LineBuffer())
            .pair("seed", SEED_STR)
            .pair("nrOfBits", NR_OF_BITS_STR)
            .addRemarkBlock("This is a multicycle implementation of the Random Component")
            .empty();

    if (HDL.isVHDL()) {
      contents.addLines(
          "Q            <= s_output_reg;",
          "s_InitSeed   <= X\"0005DEECE66D\" WHEN {{seed}} = 0 ELSE",
          "                X\"0000\"&std_logic_vector(to_unsigned({{seed}}, 32));",
          "s_reset      <= '1' WHEN s_reset_reg /= \"010\" ELSE '0';",
          "s_reset_next <= \"010\" WHEN (s_reset_reg = \"101\" OR",
          "                            s_reset_reg = \"010\") AND",
          "                            clear = '0' ELSE",
          "                \"101\" WHEN s_reset_reg = \"001\" ELSE",
          "                \"001\";",
          "s_start      <= '1' WHEN (ClockEnable = '1' AND enable = '1') OR",
          "                         (s_reset_reg = \"101\" AND clear = '0') ELSE '0';",
          "s_mult_shift_next <= (OTHERS => '0') WHEN s_reset = '1' ELSE",
          "                     X\"5DEECE66D\" WHEN s_start_reg = '1' ELSE",
          "                     '0'&s_mult_shift_reg(35 DOWNTO 1);",
          "s_seed_shift_next <= (OTHERS => '0') WHEN s_reset = '1' ELSE",
          "                     s_current_seed WHEN s_start_reg = '1' ELSE",
          "                     s_seed_shift_reg(46 DOWNTO 0)&'0';",
          "s_mult_busy       <= '0' WHEN s_mult_shift_reg = X\"000000000\" ELSE '1';",
          "",
          "s_mac_lo_in_1     <= (OTHERS => '0') WHEN s_start_reg = '1' OR",
          "                                          s_reset = '1' ELSE",
          "                     '0'&s_mac_lo_reg(23 DOWNTO 0);",
          "s_mac_lo_in_2     <= '0'&X\"00000B\"",
          "                        WHEN s_start_reg = '1' ELSE",
          "                     '0'&s_seed_shift_reg(23 DOWNTO 0) ",
          "                        WHEN s_mult_shift_reg(0) = '1' ELSE",
          "                     (OTHERS => '0');",
          "s_mac_hi_in_2     <= (OTHERS => '0') WHEN s_start_reg = '1' ELSE",
          "                     s_mac_hi_reg;",
          "s_mac_hi_1_next   <= s_seed_shift_reg(47 DOWNTO 24) ",
          "                        WHEN s_mult_shift_reg(0) = '1' ELSE",
          "                     (OTHERS => '0');",
          "s_busy_pipe_next  <= \"00\" WHEN s_reset = '1' ELSE",
          "                     s_busy_pipe_reg(0)&s_mult_busy;",
          "",
          "make_current_seed : PROCESS( GlobalClock , s_busy_pipe_reg , s_reset )",
          "BEGIN",
          "   IF (GlobalClock'event AND (GlobalClock = '1')) THEN",
          "      IF (s_reset = '1') THEN s_current_seed <= s_InitSeed;",
          "      ELSIF (s_busy_pipe_reg = \"10\") THEN",
          "         s_current_seed <= s_mac_hi_reg&s_mac_lo_reg(23 DOWNTO 0);",
          "      END IF;",
          "   END IF;",
          "END PROCESS make_current_seed;",
          "",
          "make_shift_regs : PROCESS(GlobalClock,s_mult_shift_next,s_seed_shift_next,",
          "                          s_mac_lo_in_1,s_mac_lo_in_2)",
          "BEGIN",
          "   IF (GlobalClock'event AND (GlobalClock = '1')) THEN",
          "      s_mult_shift_reg <= s_mult_shift_next;",
          "      s_seed_shift_reg <= s_seed_shift_next;",
          "      s_mac_lo_reg     <= std_logic_vector(unsigned(s_mac_lo_in_1)+unsigned(s_mac_lo_in_2));",
          "      s_mac_hi_1_reg   <= s_mac_hi_1_next;",
          "      s_mac_hi_reg     <= std_logic_vector(unsigned(s_mac_hi_1_reg)+unsigned(s_mac_hi_in_2)+",
          "                          unsigned(s_mac_lo_reg(24 DOWNTO 24)));",
          "      s_busy_pipe_reg  <= s_busy_pipe_next;",
          "   END IF;",
          "END PROCESS make_shift_regs;",
          "",
          "make_start_reg : PROCESS(GlobalClock,s_start)",
          "BEGIN",
          "   IF (GlobalClock'event AND (GlobalClock = '1')) THEN",
          "      s_start_reg <= s_start;",
          "   END IF;",
          "END PROCESS make_start_reg;",
          "",
          "make_reset_reg : PROCESS(GlobalClock,s_reset_next)",
          "BEGIN",
          "   IF (GlobalClock'event AND (GlobalClock = '1')) THEN",
          "      s_reset_reg <= s_reset_next;",
          "   END IF;",
          "END PROCESS make_reset_reg;",
          "",
          "   make_output : PROCESS( GlobalClock , s_reset , s_InitSeed )",
          "   BEGIN",
          "      IF (GlobalClock'event AND (GlobalClock = '1')) THEN",
          "         IF (s_reset = '1') THEN s_output_reg <= s_InitSeed( ({{nrOfBits}}-1) DOWNTO 0 );",
          "         ELSIF (ClockEnable = '1' AND enable = '1') THEN",
          "            s_output_reg <= s_current_seed(({{nrOfBits}}+11) DOWNTO 12);",
          "         END IF;",
          "      END IF;",
          "   END PROCESS make_output;");
    } else {
      contents.addLines(
          "assign Q = s_output_reg;",
          "assign s_InitSeed = ({{seed}}) ? {{seed}} : 48'h5DEECE66D;",
          "assign s_reset = (s_reset_reg==3'b010) ? 1'b1 : 1'b0;",
          "assign s_reset_next = (((s_reset_reg == 3'b101)|",
          "                        (s_reset_reg == 3'b010))&clear) ? 3'b010 :",
          "                      (s_reset_reg==3'b001) ? 3'b101 : 3'b001;",
          "assign s_start = ((ClockEnable&enable)|((s_reset_reg == 3'b101)&clear)) ? 1'b1 : 1'b0;",
          "assign s_mult_shift_next = (s_reset) ? 36'd0 :",
          "                           (s_start_reg) ? 36'h5DEECE66D : {1'b0,s_mult_shift_reg[35:1]};",
          "assign s_seed_shift_next = (s_reset) ? 48'd0 :",
          "                           (s_start_reg) ? s_current_seed : {s_seed_shift_reg[46:0],1'b0};",
          "assign s_mult_busy = (s_mult_shift_reg == 0) ? 1'b0 : 1'b1;",
          "assign s_mac_lo_in_1 = (s_start_reg|s_reset) ? 25'd0 : {1'b0,s_mac_lo_reg[23:0]};",
          "assign s_mac_lo_in_2 = (s_start_reg) ? 25'hB :",
          "                       (s_mult_shift_reg[0]) ? {1'b0,s_seed_shift_reg[23:0]} : 25'd0;",
          "assign s_mac_hi_in_2 = (s_start_reg) ? 0 : s_mac_hi_reg;",
          "assign s_mac_hi_1_next = (s_mult_shift_reg[0]) ? s_seed_shift_reg[47:24] : 0;",
          "assign s_busy_pipe_next = (s_reset) ? 2'd0 : {s_busy_pipe_reg[0],s_mult_busy};",
          "",
          "always @(posedge GlobalClock)",
          "begin",
          "   if (s_reset) s_current_seed <= s_InitSeed;",
          "   else if (s_busy_pipe_reg == 2'b10) s_current_seed <= {s_mac_hi_reg,s_mac_lo_reg[23:0]};",
          "end",
          "",
          "always @(posedge GlobalClock)",
          "begin",
          "      s_mult_shift_reg <= s_mult_shift_next;",
          "      s_seed_shift_reg <= s_seed_shift_next;",
          "      s_mac_lo_reg     <= s_mac_lo_in_1+s_mac_lo_in_2;",
          "      s_mac_hi_1_reg   <= s_mac_hi_1_next;",
          "      s_mac_hi_reg     <= s_mac_hi_1_reg+s_mac_hi_in_2+s_mac_lo_reg[24];",
          "      s_busy_pipe_reg  <= s_busy_pipe_next;",
          "      s_start_reg      <= s_start;",
          "      s_reset_reg      <= s_reset_next;",
          "end",
          "",
          "   always @(posedge GlobalClock)",
          " begin",
          "   if (s_reset) s_output_reg <= s_InitSeed[({{nrOfBits}}-1):0];",
          "   else if (ClockEnable&enable) s_output_reg <= s_current_seed[({{nrOfBits}}+11):12];",
          "end");
    }
    return contents.getWithIndent();
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
    map.put(NrOfBitsId, NR_OF_BITS_STR);
    map.put(SeedId, SEED_STR);
    return map;
  }

  @Override
  public SortedMap<String, Integer> GetParameterMap(Netlist Nets, NetlistComponent ComponentInfo) {
    final var map = new TreeMap<String, Integer>();
    var seed = ComponentInfo.getComponent().getAttributeSet().getValue(Random.ATTR_SEED);
    if (seed == 0) seed = (int) System.currentTimeMillis();
    map.put(
            NR_OF_BITS_STR,
        ComponentInfo.getComponent().getAttributeSet().getValue(StdAttr.WIDTH).getWidth());
    map.put(SEED_STR, seed);
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
    if (!comp.isEndConnected(Random.CK)) {
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
    if (comp.getComponent().getAttributeSet().containsAttribute(StdAttr.EDGE_TRIGGER)) {
      activeLow =
          comp.getComponent().getAttributeSet().getValue(StdAttr.EDGE_TRIGGER)
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
              + ClockHDLGeneratorFactory.GLOBAL_CLOCK_INDEX
              + HDL.BracketClose());
      if (Nets.requiresGlobalClockConnection()) {
        map.put(
            "ClockEnable",
            clockNetName
                + HDL.BracketOpen()
                + ClockHDLGeneratorFactory.GLOBAL_CLOCK_INDEX
                + HDL.BracketClose());
      } else {
        if (activeLow)
          map.put(
              "ClockEnable",
              clockNetName
                  + HDL.BracketOpen()
                  + ClockHDLGeneratorFactory.NEGATIVE_EDGE_TICK_INDEX
                  + HDL.BracketClose());
        else
          map.put(
              "ClockEnable",
              clockNetName
                  + HDL.BracketOpen()
                  + ClockHDLGeneratorFactory.POSITIVE_EDGE_TICK_INDEX
                  + HDL.BracketClose());
      }
    }
    map.putAll(GetNetMap("clear", true, comp, Random.RST, Nets));
    map.putAll(GetNetMap("enable", false, comp, Random.NXT, Nets));
    var output = "Q";
    if (HDL.isVHDL()
        & (comp.getComponent().getAttributeSet().getValue(StdAttr.WIDTH).getWidth() == 1))
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
