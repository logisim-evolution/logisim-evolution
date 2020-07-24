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

package com.cburch.logisim.fpga.hdlgenerator;

import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.fpga.designrulecheck.Netlist;
import com.cburch.logisim.fpga.designrulecheck.NetlistComponent;
import com.cburch.logisim.fpga.gui.FPGAReport;

import java.util.ArrayList;
import java.util.SortedMap;
import java.util.TreeMap;

public class TickComponentHDLGeneratorFactory extends AbstractHDLGeneratorFactory {

  private long FpgaClockFrequency;
  private double TickFrequency;
  // private boolean useFPGAClock;
  private static final String ReloadValueStr = "ReloadValue";
  private static final Integer ReloadValueId = -1;
  private static final String NrOfCounterBitsStr = "NrOfBits";
  private static final Integer NrOfCounterBitsId = -2;

  public static final String FPGAClock = "FPGA_GlobalClock";
  public static final String FPGATick = "s_FPGA_Tick";

  public TickComponentHDLGeneratorFactory(long fpga_clock_frequency, double tick_frequency /*
								 * , boolean useFPGAClock
								 */) {
    FpgaClockFrequency = fpga_clock_frequency;
    TickFrequency = tick_frequency;
    // this.useFPGAClock = useFPGAClock;
  }

  @Override
  public String getComponentStringIdentifier() {
    return "LogisimTickGenerator";
  }

  @Override
  public SortedMap<String, Integer> GetInputList(Netlist TheNetlist, AttributeSet attrs) {
    SortedMap<String, Integer> Inputs = new TreeMap<String, Integer>();
    Inputs.put("FPGAClock", 1);
    return Inputs;
  }

  @Override
  public ArrayList<String> GetModuleFunctionality(
      Netlist TheNetlist, AttributeSet attrs, FPGAReport Reporter, String HDLType) {
    ArrayList<String> Contents = new ArrayList<String>();
    String Preamble = (HDLType.equals(VHDL)) ? "" : "assign ";
    String AssignOperator = (HDLType.equals(VHDL)) ? "<=" : "=";
    Contents.add("");
    Contents.addAll(MakeRemarkBlock("Here the Output is defined", 3, HDLType));
    if (TheNetlist.RequiresGlobalClockConnection()) {
      Contents.add("   " + Preamble + "FPGATick " + AssignOperator + " '1';");
    } else {
      Contents.add("   " + Preamble + "FPGATick " + AssignOperator + " s_tick_reg;");
    }
    Contents.add("");
    Contents.addAll(MakeRemarkBlock("Here the update logic is defined", 3, HDLType));
    if (HDLType.equals(VHDL)) {
      Contents.add(
          "   s_tick_next   <= '1' WHEN s_count_reg = std_logic_vector(to_unsigned(0,"
              + NrOfCounterBitsStr
              + ")) ELSE '0';");
      Contents.add(
          "   s_count_next  <= (OTHERS => '0') WHEN s_tick_reg /= '0' AND s_tick_reg /= '1' ELSE -- For simulation only!");
      Contents.add(
          "                    std_logic_vector(to_unsigned((ReloadValue-1),"
              + NrOfCounterBitsStr
              + ")) WHEN s_tick_next = '1' ELSE");
      Contents.add("                    std_logic_vector(unsigned(s_count_reg)-1);");
      Contents.add("");
    } else {
      Contents.add("   assign s_tick_next  = (s_count_reg == 0) ? 1'b1 : 1'b0;");
      Contents.add("   assign s_count_next = (s_count_reg == 0) ? ReloadValue-1 : s_count_reg-1;");
      Contents.add("");
      Contents.addAll(MakeRemarkBlock("Here the simulation only initial is defined", 3, HDLType));
      Contents.add("   initial");
      Contents.add("   begin");
      Contents.add("      s_count_reg = 0;");
      Contents.add("      s_tick_reg  = 1'b0;");
      Contents.add("   end");
      Contents.add("");
    }
    Contents.addAll(MakeRemarkBlock("Here the flipflops are defined", 3, HDLType));
    if (HDLType.equals(VHDL)) {
      Contents.add("   make_tick : PROCESS( FPGAClock , s_tick_next )");
      Contents.add("   BEGIN");
      Contents.add("      IF (FPGAClock'event AND (FPGAClock = '1')) THEN");
      Contents.add("         s_tick_reg <= s_tick_next;");
      Contents.add("      END IF;");
      Contents.add("   END PROCESS make_tick;");
      Contents.add("");
      Contents.add("   make_counter : PROCESS( FPGAClock , s_count_next )");
      Contents.add("   BEGIN");
      Contents.add("      IF (FPGAClock'event AND (FPGAClock = '1')) THEN");
      Contents.add("         s_count_reg <= s_count_next;");
      Contents.add("      END IF;");
      Contents.add("   END PROCESS make_counter;");
    } else {
      Contents.add("   always @(posedge FPGAClock)");
      Contents.add("   begin");
      Contents.add("       s_count_reg <= s_count_next;");
      Contents.add("       s_tick_reg  <= s_tick_next;");
      Contents.add("   end");
    }
    return Contents;
  }

  @Override
  public SortedMap<String, Integer> GetOutputList(Netlist TheNetlist, AttributeSet attrs) {
    SortedMap<String, Integer> Outputs = new TreeMap<String, Integer>();
    Outputs.put("FPGATick", 1);
    return Outputs;
  }

  @Override
  public SortedMap<Integer, String> GetParameterList(AttributeSet attrs) {
    SortedMap<Integer, String> Parameters = new TreeMap<Integer, String>();
    Parameters.put(ReloadValueId, ReloadValueStr);
    Parameters.put(NrOfCounterBitsId, NrOfCounterBitsStr);
    return Parameters;
  }

  @Override
  public SortedMap<String, Integer> GetParameterMap(
      Netlist Nets, NetlistComponent ComponentInfo, FPGAReport Reporter) {
    SortedMap<String, Integer> ParameterMap = new TreeMap<String, Integer>();
    double ReloadValueAcc = ((double) FpgaClockFrequency) / TickFrequency;
    long ReloadValue = (long) ReloadValueAcc;
    int nr_of_bits = 0;
    if ((ReloadValue > (long) 0x7FFFFFFF) | (ReloadValue < 0)) ReloadValue = (long) 0x7FFFFFFF;
    ParameterMap.put(ReloadValueStr, (int) ReloadValue);
    while (ReloadValue != 0) {
      nr_of_bits++;
      ReloadValue /= 2;
    }
    ParameterMap.put(NrOfCounterBitsStr, nr_of_bits);
    return ParameterMap;
  }

  @Override
  public SortedMap<String, String> GetPortMap(
      Netlist Nets, Object MapInfo, FPGAReport Reporter, String HDLType) {
    SortedMap<String, String> PortMap = new TreeMap<String, String>();
    PortMap.put("FPGAClock", TickComponentHDLGeneratorFactory.FPGAClock);
    PortMap.put("FPGATick", TickComponentHDLGeneratorFactory.FPGATick);
    return PortMap;
  }

  @Override
  public SortedMap<String, Integer> GetRegList(AttributeSet attrs, String HDLType) {
    SortedMap<String, Integer> Regs = new TreeMap<String, Integer>();
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
    SortedMap<String, Integer> Wires = new TreeMap<String, Integer>();
    Wires.put("s_tick_next", 1);
    Wires.put("s_count_next", NrOfCounterBitsId);
    return Wires;
  }

  @Override
  public boolean HDLTargetSupported(String HDLType, AttributeSet attrs) {
    return true;
  }
}
