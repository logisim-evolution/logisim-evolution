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

package com.cburch.logisim.fpga.hdlgenerator;

import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.fpga.designrulecheck.Netlist;
import com.cburch.logisim.fpga.designrulecheck.NetlistComponent;
import com.cburch.logisim.util.LineBuffer;
import java.util.ArrayList;
import java.util.SortedMap;
import java.util.TreeMap;

public class TickComponentHDLGeneratorFactory extends AbstractHDLGeneratorFactory {

  private final long fpgaClockFrequency;
  private final double tickFrequency;
  private static final String ReloadValueStr = "ReloadValue";
  private static final Integer ReloadValueId = -1;
  private static final String NrOfCounterBitsStr = "NrOfBits";
  private static final Integer NrOfCounterBitsId = -2;

  public static final String FPGA_CLOCK = "FPGA_GlobalClock";
  public static final String FPGA_TICK = "s_FPGA_Tick";

  public TickComponentHDLGeneratorFactory(
      long fpga_clock_frequency, double tick_frequency /* boolean useFPGAClock */) {
    fpgaClockFrequency = fpga_clock_frequency;
    tickFrequency = tick_frequency;
    // this.useFPGAClock = useFPGAClock;
  }

  @Override
  public String getComponentStringIdentifier() {
    return "LogisimTickGenerator";
  }

  @Override
  public SortedMap<String, Integer> GetInputList(Netlist TheNetlist, AttributeSet attrs) {
    SortedMap<String, Integer> Inputs = new TreeMap<>();
    Inputs.put("FPGAClock", 1);
    return Inputs;
  }

  @Override
  public ArrayList<String> GetModuleFunctionality(Netlist TheNetlist, AttributeSet attrs) {
    final var Contents =
        (new LineBuffer())
            .addHdlPairs()
            .pair("nrOfCounterBits", NrOfCounterBitsStr)
            .add("")
            .addRemarkBlock("Here the Output is defined")
            .add(
                TheNetlist.requiresGlobalClockConnection()
                    ? "   {{assign}} FPGATick {{=}} '1';"
                    : "   {{assign}} FPGATick {{=}} s_tick_reg;")
            .add("")
            .addRemarkBlock("Here the update logic is defined");

    if (HDL.isVHDL()) {
      Contents.addLines(
          "s_tick_next   <= '1' WHEN s_count_reg = std_logic_vector(to_unsigned(0, {{nrOfCounterBits}})) ELSE '0';",
          "s_count_next  <= (OTHERS => '0') WHEN s_tick_reg /= '0' AND s_tick_reg /= '1' ELSE -- For simulation only!",
          "                 std_logic_vector(to_unsigned((ReloadValue-1), {{nrOfCounterBits}})) WHEN s_tick_next = '1' ELSE",
          "                 std_logic_vector(unsigned(s_count_reg)-1);",
          "");
    } else {
      Contents.addLines(
              "assign s_tick_next  = (s_count_reg == 0) ? 1'b1 : 1'b0;",
              "assign s_count_next = (s_count_reg == 0) ? ReloadValue-1 : s_count_reg-1;",
              "")
          .addRemarkBlock("Here the simulation only initial is defined")
          .addLines(
              "initial",
              "begin",
              "   s_count_reg = 0;",
              "   s_tick_reg  = 1'b0;",
              "end",
              "");
    }
    Contents.addRemarkBlock("Here the flipflops are defined");
    if (HDL.isVHDL()) {
      Contents.addLines(
          "make_tick : PROCESS( FPGAClock , s_tick_next )",
          "BEGIN",
          "   IF (FPGAClock'event AND (FPGAClock = '1')) THEN",
          "      s_tick_reg <= s_tick_next;",
          "   END IF;",
          "END PROCESS make_tick;",
          "",
          "make_counter : PROCESS( FPGAClock , s_count_next )",
          "BEGIN",
          "   IF (FPGAClock'event AND (FPGAClock = '1')) THEN",
          "      s_count_reg <= s_count_next;",
          "   END IF;",
          "END PROCESS make_counter;");
    } else {
      Contents.addLines(
          "always @(posedge FPGAClock)",
          "begin",
          "    s_count_reg <= s_count_next;",
          "    s_tick_reg  <= s_tick_next;",
          "end");
    }
    return Contents.getWithIndent();
  }

  @Override
  public SortedMap<String, Integer> GetOutputList(Netlist TheNetlist, AttributeSet attrs) {
    SortedMap<String, Integer> Outputs = new TreeMap<>();
    Outputs.put("FPGATick", 1);
    return Outputs;
  }

  @Override
  public SortedMap<Integer, String> GetParameterList(AttributeSet attrs) {
    final var map = new TreeMap<Integer, String>();
    map.put(ReloadValueId, ReloadValueStr);
    map.put(NrOfCounterBitsId, NrOfCounterBitsStr);
    return map;
  }

  @Override
  public SortedMap<String, Integer> GetParameterMap(Netlist Nets, NetlistComponent ComponentInfo) {
    SortedMap<String, Integer> ParameterMap = new TreeMap<>();
    double ReloadValueAcc = ((double) fpgaClockFrequency) / tickFrequency;
    long ReloadValue = (long) ReloadValueAcc;
    int nr_of_bits = 0;
    if ((ReloadValue > (long) 0x7FFFFFFF) | (ReloadValue < 0)) ReloadValue = 0x7FFFFFFF;
    ParameterMap.put(ReloadValueStr, (int) ReloadValue);
    while (ReloadValue != 0) {
      nr_of_bits++;
      ReloadValue /= 2;
    }
    ParameterMap.put(NrOfCounterBitsStr, nr_of_bits);
    return ParameterMap;
  }

  @Override
  public SortedMap<String, String> GetPortMap(Netlist Nets, Object MapInfo) {
    SortedMap<String, String> PortMap = new TreeMap<>();
    PortMap.put("FPGAClock", TickComponentHDLGeneratorFactory.FPGA_CLOCK);
    PortMap.put("FPGATick", TickComponentHDLGeneratorFactory.FPGA_TICK);
    return PortMap;
  }

  @Override
  public SortedMap<String, Integer> GetRegList(AttributeSet attrs) {
    SortedMap<String, Integer> Regs = new TreeMap<>();
    Regs.put("s_tick_reg", 1);
    Regs.put("s_count_reg", NrOfCounterBitsId);
    return Regs;
  }

  @Override
  public String GetSubDir() {
    return "base";
  }

  @Override
  public SortedMap<String, Integer> GetWireList(AttributeSet attrs, Netlist Nets) {
    SortedMap<String, Integer> Wires = new TreeMap<>();
    Wires.put("s_tick_next", 1);
    Wires.put("s_count_next", NrOfCounterBitsId);
    return Wires;
  }

  @Override
  public boolean HDLTargetSupported(AttributeSet attrs) {
    return true;
  }
}
