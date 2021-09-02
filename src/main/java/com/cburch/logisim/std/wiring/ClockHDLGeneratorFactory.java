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

package com.cburch.logisim.std.wiring;

import com.cburch.logisim.comp.Component;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.fpga.designrulecheck.Netlist;
import com.cburch.logisim.fpga.designrulecheck.NetlistComponent;
import com.cburch.logisim.fpga.hdlgenerator.AbstractHDLGeneratorFactory;
import com.cburch.logisim.fpga.hdlgenerator.HDL;
import com.cburch.logisim.fpga.hdlgenerator.TickComponentHDLGeneratorFactory;
import com.cburch.logisim.util.LineBuffer;
import java.util.ArrayList;
import java.util.SortedMap;
import java.util.TreeMap;

public class ClockHDLGeneratorFactory extends AbstractHDLGeneratorFactory {

  public static final int NR_OF_CLOCK_BITS = 5;
  public static final int DERIVED_CLOCK_INDEX = 0;
  public static final int INVERTED_DERIVED_CLOCK_INDEX = 1;
  public static final int POSITIVE_EDGE_TICK_INDEX = 2;
  public static final int NEGATIVE_EDGE_TICK_INDEX = 3;
  public static final int GLOBAL_CLOCK_INDEX = 4;
  private static final String HIGH_TICK_STR = "HighTicks";
  private static final int HIGH_TICK_ID = -1;
  private static final String LOW_TICK_STR = "LowTicks";
  private static final int LOW_TICK_ID = -2;
  private static final String PHASE_STR = "Phase";
  private static final int PHASE_ID = -3;
  private static final String NR_OF_BITS_STR = "NrOfBits";
  private static final int NR_OF_BITS_ID = -4;

  private String GetClockNetName(Component comp, Netlist TheNets) {
    StringBuilder Contents = new StringBuilder();
    int ClockNetId = TheNets.getClockSourceId(comp);
    if (ClockNetId >= 0) {
      Contents.append(ClockTreeName).append(ClockNetId);
    }
    return Contents.toString();
  }

  @Override
  public String getComponentStringIdentifier() {
    return "CLOCKGEN";
  }

  @Override
  public SortedMap<String, Integer> GetInputList(Netlist TheNetlist, AttributeSet attrs) {
    final var map = new TreeMap<String, Integer>();
    map.put("GlobalClock", 1);
    map.put("ClockTick", 1);
    return map;
  }

  @Override
  public ArrayList<String> GetModuleFunctionality(Netlist TheNetlist, AttributeSet attrs) {
    final var Contents =
        (new LineBuffer())
            .pair("phase", PHASE_STR)
            .pair("nrOfBits", NR_OF_BITS_STR)
            .pair("lowTick", LOW_TICK_STR)
            .pair("highTick", HIGH_TICK_STR)
            .addRemarkBlock("Here the output signals are defines; we synchronize them all on the main clock");

    if (HDL.isVHDL()) {
      Contents.addLines(
          "ClockBus <= GlobalClock&s_output_regs;",
          "makeOutputs : PROCESS( GlobalClock )",
          "BEGIN",
          "   IF (GlobalClock'event AND (GlobalClock = '1')) THEN",
          "      s_buf_regs(0)     <= s_derived_clock_reg({{phase}}-1);",
          "      s_buf_regs(1)     <= NOT(s_derived_clock_reg({{phase}}-1));",
          "      s_output_regs(0)  <= s_buf_regs(0);",
          "      s_output_regs(1)  <= s_buf_regs(1);",
          "      s_output_regs(2)  <= NOT(s_buf_regs(0)) AND s_derived_clock_reg({{phase}}-1);",
          "      s_output_regs(3)  <= s_buf_regs(0) AND NOT(s_derived_clock_reg({{phase}}-1));",
          "   END IF;",
          "END PROCESS makeOutputs;");
    } else {
      Contents.addLines(
          "assign ClockBus = {GlobalClock,s_output_regs};",
          "always @(posedge GlobalClock)",
          "begin",
          "   s_buf_regs[0]    <= s_derived_clock_reg[{{phase}}-1];",
          "   s_buf_regs[1]    <= ~s_derived_clock_reg[{{phase}}-1];",
          "   s_output_regs[0] <= s_buf_regs[0];",
          "   s_output_regs[1] <= s_output_regs[1];",
          "   s_output_regs[2] <= ~s_buf_regs[0] & s_derived_clock_reg[{{phase}}-1];",
          "   s_output_regs[3] <= ~s_derived_clock_reg[{{phase}}-1] & s_buf_regs[0];",
          "end");
    }
    Contents.add("").addRemarkBlock("Here the control signals are defined");
    if (HDL.isVHDL()) {
      Contents.addLines(
          "s_counter_is_zero <= '1' WHEN s_counter_reg = std_logic_vector(to_unsigned(0,{{nrOfBits}})) ELSE '0';",
          "s_counter_next    <= std_logic_vector(unsigned(s_counter_reg) - 1)",
          "                       WHEN s_counter_is_zero = '0' ELSE",
          "                    std_logic_vector(to_unsigned(({{lowTick}}-1), {{nrOfBits}}))",
          "                       WHEN s_derived_clock_reg(0) = '1' ELSE",
          "                    std_logic_vector(to_unsigned(({{highTick}}-1), {{nrOfBits}}));"
      );
    } else {
      Contents.addLines(
              "assign s_counter_is_zero = (s_counter_reg == 0) ? 1'b1 : 1'b0;",
              "assign s_counter_next = (s_counter_is_zero == 1'b0)",
              "                           ? s_counter_reg - 1",
              "                           : (s_derived_clock_reg[0] == 1'b1)",
              "                              ? {{lowTick}} - 1",
              "                              : {{highTick}} - 1;",
              "")
          .addRemarkBlock("Here the initial values are defined (for simulation only)")
          .addLines(
              "initial",
              "begin",
              "   s_output_regs = 0;",
              "   s_derived_clock_reg = 0;",
              "   s_counter_reg = 0;",
              "end");
    }
    Contents.add("").addRemarkBlock("Here the state registers are defined");
    if (HDL.isVHDL()) {
      Contents.addLines(
          "makeDerivedClock : PROCESS( GlobalClock , ClockTick , s_counter_is_zero ,",
          "                            s_derived_clock_reg)",
          "BEGIN",
          "   IF (GlobalClock'event AND (GlobalClock = '1')) THEN",
          "      IF (s_derived_clock_reg(0) /= '0' AND s_derived_clock_reg(0) /= '1') THEN --For simulation only",
          "         s_derived_clock_reg <= (OTHERS => '1');",
          "      ELSIF (ClockTick = '1') THEN",
          "         FOR n IN {{phase}}-1 DOWNTO 1 LOOP",
          "           s_derived_clock_reg(n) <= s_derived_clock_reg(n-1);",
          "         END LOOP;",
          "         s_derived_clock_reg(0) <= s_derived_clock_reg(0) XOR s_counter_is_zero;",
          "      END IF;",
          "   END IF;",
          "END PROCESS makeDerivedClock;",
          "",
          "makeCounter : PROCESS( GlobalClock , ClockTick , s_counter_next ,",
          "                       s_derived_clock_reg )",
          "BEGIN",
          "   IF (GlobalClock'event AND (GlobalClock = '1')) THEN",
          "      IF (s_derived_clock_reg(0) /= '0' AND s_derived_clock_reg(0) /= '1') THEN --For simulation only",
          "         s_counter_reg <= (OTHERS => '0');",
          "      ELSIF (ClockTick = '1') THEN",
          "         s_counter_reg <= s_counter_next;",
          "      END IF;",
          "   END IF;",
          "END PROCESS makeCounter;");
    } else {
      Contents.addLines(
          "integer n;",
          "always @(posedge GlobalClock)",
          "begin",
          "   if (ClockTick)",
          "   begin",
          "      s_derived_clock_reg[0] <= s_derived_clock_reg[0] ^ s_counter_is_zero;",
          "      for (n = 1; n < {{phase}}; n = n+1) begin",
          "         s_derived_clock_reg[n] <= s_derived_clock_reg[n-1];",
          "      end",
          "   end",
          "end",
          "",
          "always @(posedge GlobalClock)",
          "begin",
          "   if (ClockTick)",
          "   begin",
          "      s_counter_reg <= s_counter_next;",
          "   end",
          "end");
    }
    Contents.add("");
    return Contents.getWithIndent();
  }

  @Override
  public SortedMap<String, Integer> GetOutputList(Netlist TheNetlist, AttributeSet attrs) {
    final var map = new TreeMap<String, Integer>();
    map.put("ClockBus", NR_OF_CLOCK_BITS);
    return map;
  }

  @Override
  public SortedMap<Integer, String> GetParameterList(AttributeSet attrs) {
    final var map = new TreeMap<Integer, String>();
    map.put(HIGH_TICK_ID, HIGH_TICK_STR);
    map.put(LOW_TICK_ID, LOW_TICK_STR);
    map.put(PHASE_ID, PHASE_STR);
    map.put(NR_OF_BITS_ID, NR_OF_BITS_STR);
    return map;
  }

  @Override
  public SortedMap<String, Integer> GetParameterMap(Netlist Nets, NetlistComponent ComponentInfo) {
    final var map = new TreeMap<String, Integer>();
    int HighTicks = ComponentInfo.getComponent().getAttributeSet().getValue(Clock.ATTR_HIGH);
    int LowTicks = ComponentInfo.getComponent().getAttributeSet().getValue(Clock.ATTR_LOW);
    int Phase = ComponentInfo.getComponent().getAttributeSet().getValue(Clock.ATTR_PHASE);
    Phase = Phase % (HighTicks + LowTicks);
    int MaxValue = Math.max(HighTicks, LowTicks);
    int nr_of_bits = 0;
    while (MaxValue != 0) {
      nr_of_bits++;
      MaxValue /= 2;
    }
    map.put(HIGH_TICK_STR, HighTicks);
    map.put(LOW_TICK_STR, LowTicks);
    map.put(PHASE_STR, (HighTicks + LowTicks) - Phase);
    map.put(NR_OF_BITS_STR, nr_of_bits);
    return map;
  }

  @Override
  public SortedMap<String, String> GetPortMap(Netlist Nets, Object MapInfo) {
    final var map = new TreeMap<String, String>();
    if (!(MapInfo instanceof NetlistComponent)) return map;
    NetlistComponent ComponentInfo = (NetlistComponent) MapInfo;
    map.put("GlobalClock", TickComponentHDLGeneratorFactory.FPGA_CLOCK);
    map.put("ClockTick", TickComponentHDLGeneratorFactory.FPGA_TICK);
    map.put("ClockBus", "s_" + GetClockNetName(ComponentInfo.getComponent(), Nets));
    return map;
  }

  @Override
  public SortedMap<String, Integer> GetRegList(AttributeSet attrs) {
    final var map = new TreeMap<String, Integer>();
    map.put("s_output_regs", NR_OF_CLOCK_BITS - 1);
    map.put("s_buf_regs", 2);
    map.put("s_counter_reg", NR_OF_BITS_ID);
    map.put("s_derived_clock_reg", PHASE_ID);
    return map;
  }

  @Override
  public String GetSubDir() {
    /*
     * this method returns the module directory where the HDL code needs to
     * be placed
     */
    return "base";
  }

  @Override
  public SortedMap<String, Integer> GetWireList(AttributeSet attrs, Netlist Nets) {
    final var map = new TreeMap<String, Integer>();
    map.put("s_counter_next", NR_OF_BITS_ID);
    map.put("s_counter_is_zero", 1);
    return map;
  }

  @Override
  public boolean HDLTargetSupported(AttributeSet attrs) {
    return true;
  }
}
