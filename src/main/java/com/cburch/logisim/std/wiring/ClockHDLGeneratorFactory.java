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
import java.util.ArrayList;
import java.util.SortedMap;
import java.util.TreeMap;

public class ClockHDLGeneratorFactory extends AbstractHDLGeneratorFactory {

  public static final int NrOfClockBits = 5;
  public static final int DerivedClockIndex = 0;
  public static final int InvertedDerivedClockIndex = 1;
  public static final int PositiveEdgeTickIndex = 2;
  public static final int NegativeEdgeTickIndex = 3;
  public static final int GlobalClockIndex = 4;
  private static final String HighTickStr = "HighTicks";
  private static final int HighTickId = -1;
  private static final String LowTickStr = "LowTicks";
  private static final int LowTickId = -2;
  private static final String PhaseStr = "Phase";
  private static final int PhaseId = -3;
  private static final String NrOfBitsStr = "NrOfBits";
  private static final int NrOfBitsId = -4;

  private String GetClockNetName(Component comp, Netlist TheNets) {
    StringBuilder Contents = new StringBuilder();
    int ClockNetId = TheNets.GetClockSourceId(comp);
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
    SortedMap<String, Integer> Inputs = new TreeMap<>();
    Inputs.put("GlobalClock", 1);
    Inputs.put("ClockTick", 1);
    return Inputs;
  }

  @Override
  public ArrayList<String> GetModuleFunctionality(Netlist TheNetlist, AttributeSet attrs) {
    ArrayList<String> Contents = new ArrayList<>();
    Contents.addAll(MakeRemarkBlock("Here the output signals are defines; we synchronize them all on the main clock", 3));
/*    if (TheNetlist.RawFPGAClock()) {
      if (HighTicks != LowTicks) {
        Reporter.AddFatalError("Clock component detected with " +HighTicks+":"+LowTicks+ " hi:lo duty cycle,"
            + " but maximum clock speed was selected. Only 1:1 duty cycle is supported with "
            + " maximum clock speed.");
      }
      if (HDLType.equals(VHDL)) {
        Contents.add("   ClockBus <= GlobalClock & '1' & '1' & NOT(GlobalClock) & GlobalClock;");
      } else {
        Contents.add("   assign ClockBus = {GlobalClock, 3'b1, 3'b1, ~GlobalClock, GlobalClock};");
      }
      Contents.add("");
      return Contents;
    }
*/    if (HDL.isVHDL()) {
      Contents.add("   ClockBus <= GlobalClock&s_output_regs;");
      Contents.add("   makeOutputs : PROCESS( GlobalClock )");
      Contents.add("   BEGIN");
      Contents.add("      IF (GlobalClock'event AND (GlobalClock = '1')) THEN");
      Contents.add("         s_buf_regs(0)     <= s_derived_clock_reg(" + PhaseStr + "-1);");
      Contents.add("         s_buf_regs(1)     <= NOT(s_derived_clock_reg(" + PhaseStr + "-1));");
      Contents.add("         s_output_regs(0)  <= s_buf_regs(0);");
      Contents.add("         s_output_regs(1)  <= s_buf_regs(1);");
      Contents.add("         s_output_regs(2)  <= NOT(s_buf_regs(0)) AND s_derived_clock_reg(" + PhaseStr + "-1);");
      Contents.add("         s_output_regs(3)  <= s_buf_regs(0) AND NOT(s_derived_clock_reg(" + PhaseStr + "-1));");
      Contents.add("      END IF;");
      Contents.add("   END PROCESS makeOutputs;");
    } else {
      Contents.add("   assign ClockBus = {GlobalClock,s_output_regs};");
      Contents.add("   always @(posedge GlobalClock)");
      Contents.add("   begin");
      Contents.add("      s_buf_regs[0]    <= s_derived_clock_reg[" + PhaseStr + "-1];");
      Contents.add("      s_buf_regs[1]    <= ~s_derived_clock_reg[" + PhaseStr + "-1];");
      Contents.add("      s_output_regs[0] <= s_buf_regs[0];");
      Contents.add("      s_output_regs[1] <= s_output_regs[1];");
      Contents.add("      s_output_regs[2] <= ~s_buf_regs[0] & s_derived_clock_reg[" + PhaseStr + "-1];");
      Contents.add("      s_output_regs[3] <= ~s_derived_clock_reg[" + PhaseStr + "-1] & s_buf_regs[0];");
      Contents.add("   end");
    }
    Contents.add("");
    Contents.addAll(MakeRemarkBlock("Here the control signals are defined", 3));
    if (HDL.isVHDL()) {
      Contents.add("   s_counter_is_zero    <= '1' WHEN s_counter_reg = std_logic_vector(to_unsigned(0,"
              + NrOfBitsStr
              + ")) ELSE '0';");
      Contents.add("   s_counter_next    <= std_logic_vector(unsigned(s_counter_reg) - 1)");
      Contents.add("                           WHEN s_counter_is_zero = '0' ELSE");
      Contents.add(
          "                        std_logic_vector(to_unsigned(("
              + LowTickStr
              + "-1),"
              + NrOfBitsStr
              + "))");
      Contents.add("                           WHEN s_derived_clock_reg(0) = '1' ELSE");
      Contents.add(
          "                        std_logic_vector(to_unsigned(("
              + HighTickStr
              + "-1),"
              + NrOfBitsStr
              + "));");
    } else {
      Contents.add("   assign s_counter_is_zero = (s_counter_reg == 0) ? 1'b1 : 1'b0;");
      Contents.add("   assign s_counter_next = (s_counter_is_zero == 1'b0) ? s_counter_reg - 1 :");
      Contents.add(
          "                           (s_derived_clock_reg[0] == 1'b1) ? " + LowTickStr + " - 1 :");
      Contents.add(
          "                                                           " + HighTickStr + " - 1;");
      Contents.add("");
      Contents.addAll(MakeRemarkBlock("Here the initial values are defined (for simulation only)", 3));
      Contents.add("   initial");
      Contents.add("   begin");
      Contents.add("      s_output_regs = 0;");
      Contents.add("      s_derived_clock_reg = 0;");
      Contents.add("      s_counter_reg = 0;");
      Contents.add("   end");
    }
    Contents.add("");
    Contents.addAll(MakeRemarkBlock("Here the state registers are defined", 3));
    if (HDL.isVHDL()) {
      Contents.add("   makeDerivedClock : PROCESS( GlobalClock , ClockTick , s_counter_is_zero ,");
      Contents.add("                               s_derived_clock_reg)");
      Contents.add("   BEGIN");
      Contents.add("      IF (GlobalClock'event AND (GlobalClock = '1')) THEN");
      Contents.add("         IF (s_derived_clock_reg(0) /= '0' AND s_derived_clock_reg(0) /= '1') THEN --For simulation only");
      Contents.add("            s_derived_clock_reg <= (OTHERS => '1');");
      Contents.add("         ELSIF (ClockTick = '1') THEN");
      Contents.add("            FOR n IN " + PhaseStr + "-1 DOWNTO 1 LOOP");
      Contents.add("              s_derived_clock_reg(n) <= s_derived_clock_reg(n-1);");
      Contents.add("            END LOOP;");
      Contents.add("            s_derived_clock_reg(0) <= s_derived_clock_reg(0) XOR s_counter_is_zero;");
      Contents.add("         END IF;");
      Contents.add("      END IF;");
      Contents.add("   END PROCESS makeDerivedClock;");
      Contents.add("");
      Contents.add("   makeCounter : PROCESS( GlobalClock , ClockTick , s_counter_next ,");
      Contents.add("                          s_derived_clock_reg )");
      Contents.add("   BEGIN");
      Contents.add("      IF (GlobalClock'event AND (GlobalClock = '1')) THEN");
      Contents.add("         IF (s_derived_clock_reg(0) /= '0' AND s_derived_clock_reg(0) /= '1') THEN --For simulation only");
      Contents.add("            s_counter_reg <= (OTHERS => '0');");
      Contents.add("         ELSIF (ClockTick = '1') THEN");
      Contents.add("            s_counter_reg <= s_counter_next;");
      Contents.add("         END IF;");
      Contents.add("      END IF;");
      Contents.add("   END PROCESS makeCounter;");
    } else {
      Contents.add("   integer n;");
      Contents.add("   always @(posedge GlobalClock)");
      Contents.add("   begin");
      Contents.add("      if (ClockTick)");
      Contents.add("      begin");
      Contents.add("         s_derived_clock_reg[0] <= s_derived_clock_reg[0] ^ s_counter_is_zero;");
      Contents.add("         for (n = 1; n < " + PhaseStr + "; n = n+1) begin");
      Contents.add("            s_derived_clock_reg[n] <= s_derived_clock_reg[n-1];");
      Contents.add("         end");
      Contents.add("      end");
      Contents.add("   end");
      Contents.add("");
      Contents.add("   always @(posedge GlobalClock)");
      Contents.add("   begin");
      Contents.add("      if (ClockTick)");
      Contents.add("      begin");
      Contents.add("         s_counter_reg <= s_counter_next;");
      Contents.add("      end");
      Contents.add("   end");
    }
    Contents.add("");
    return Contents;
  }

  @Override
  public SortedMap<String, Integer> GetOutputList(Netlist TheNetlist, AttributeSet attrs) {
    SortedMap<String, Integer> Outputs = new TreeMap<>();
    Outputs.put("ClockBus", NrOfClockBits);
    return Outputs;
  }

  @Override
  public SortedMap<Integer, String> GetParameterList(AttributeSet attrs) {
    SortedMap<Integer, String> Parameters = new TreeMap<>();
    Parameters.put(HighTickId, HighTickStr);
    Parameters.put(LowTickId, LowTickStr);
    Parameters.put(PhaseId, PhaseStr);
    Parameters.put(NrOfBitsId, NrOfBitsStr);
    return Parameters;
  }

  @Override
  public SortedMap<String, Integer> GetParameterMap(Netlist Nets, NetlistComponent ComponentInfo) {
    SortedMap<String, Integer> ParameterMap = new TreeMap<>();
    int HighTicks = ComponentInfo.GetComponent().getAttributeSet().getValue(Clock.ATTR_HIGH);
    int LowTicks = ComponentInfo.GetComponent().getAttributeSet().getValue(Clock.ATTR_LOW);
    int Phase = ComponentInfo.GetComponent().getAttributeSet().getValue(Clock.ATTR_PHASE);
    Phase = Phase % (HighTicks + LowTicks);
    int MaxValue = Math.max(HighTicks, LowTicks);
    int nr_of_bits = 0;
    while (MaxValue != 0) {
      nr_of_bits++;
      MaxValue /= 2;
    }
    ParameterMap.put(HighTickStr, HighTicks);
    ParameterMap.put(LowTickStr, LowTicks);
    ParameterMap.put(PhaseStr, (HighTicks + LowTicks) - Phase);
    ParameterMap.put(NrOfBitsStr, nr_of_bits);
    return ParameterMap;
  }

  @Override
  public SortedMap<String, String> GetPortMap(Netlist Nets, Object MapInfo) {
    SortedMap<String, String> PortMap = new TreeMap<>();
    if (!(MapInfo instanceof NetlistComponent)) return PortMap;
    NetlistComponent ComponentInfo = (NetlistComponent) MapInfo;
    PortMap.put("GlobalClock", TickComponentHDLGeneratorFactory.FPGAClock);
    PortMap.put("ClockTick", TickComponentHDLGeneratorFactory.FPGATick);
    PortMap.put("ClockBus", "s_" + GetClockNetName(ComponentInfo.GetComponent(), Nets));
    return PortMap;
  }

  @Override
  public SortedMap<String, Integer> GetRegList(AttributeSet attrs) {
    SortedMap<String, Integer> Regs = new TreeMap<>();
    Regs.put("s_output_regs", NrOfClockBits - 1);
    Regs.put("s_buf_regs", 2);
    Regs.put("s_counter_reg", NrOfBitsId);
    Regs.put("s_derived_clock_reg", PhaseId);
    return Regs;
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
    SortedMap<String, Integer> Wires = new TreeMap<>();
    Wires.put("s_counter_next", NrOfBitsId);
    Wires.put("s_counter_is_zero", 1);
    return Wires;
  }

  @Override
  public boolean HDLTargetSupported(AttributeSet attrs) {
    return true;
  }
}
