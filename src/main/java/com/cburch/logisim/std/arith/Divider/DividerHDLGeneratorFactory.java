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

package com.cburch.logisim.std.arith;

import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.fpga.designrulecheck.Netlist;
import com.cburch.logisim.fpga.designrulecheck.NetlistComponent;
import com.cburch.logisim.fpga.hdlgenerator.AbstractHDLGeneratorFactory;
import com.cburch.logisim.fpga.hdlgenerator.HDL;
import com.cburch.logisim.instance.StdAttr;
import java.util.ArrayList;
import java.util.SortedMap;
import java.util.TreeMap;

public class DividerHDLGeneratorFactory extends AbstractHDLGeneratorFactory {
  private static final String NrOfBitsStr = "NrOfBits";
  private static final int NrOfBitsId = -1;
  private static final String CalcBitsStr = "CalcBits";
  private static final int CalcBitsId = -2;
  private static final String UnsignedStr = "UnsignedDivider";
  private static final int UnsignedId = -3;

  @Override
  public String getComponentStringIdentifier() {
    return "DIV";
  }

  @Override
  public SortedMap<String, Integer> GetInputList(Netlist TheNetlist, AttributeSet attrs) {
    SortedMap<String, Integer> Inputs = new TreeMap<>();
    Inputs.put("INP_A", NrOfBitsId);
    Inputs.put("INP_B", NrOfBitsId);
    Inputs.put("Upper", NrOfBitsId);
    return Inputs;
  }

  @Override
  public ArrayList<String> GetModuleFunctionality(Netlist TheNetlist, AttributeSet attrs) {
    ArrayList<String> Contents = new ArrayList<>();
    if (HDL.isVHDL()) {
      Contents.add(
          "   s_extended_dividend(" + CalcBitsStr + "-1 DOWNTO " + NrOfBitsStr + ") <= Upper;");
      Contents.add("   s_extended_dividend(" + NrOfBitsStr + "-1 DOWNTO 0) <= INP_A;");
      Contents.add(
          "   s_div_result <= std_logic_vector(unsigned(s_extended_dividend) / unsigned(INP_B))");
      Contents.add("                      WHEN " + UnsignedStr + " = 1 ELSE");
      Contents.add(
          "                   std_logic_vector(signed(s_extended_dividend) / signed(INP_B));");
      Contents.add(
          "   s_mod_result <= std_logic_vector(unsigned(s_extended_dividend) mod unsigned(INP_B))");
      Contents.add("                      WHEN " + UnsignedStr + " = 1 ELSE");
      Contents.add(
          "                   std_logic_vector(signed(s_extended_dividend) mod signed(INP_B));");
      Contents.add("   Quotient  <= s_div_result(" + NrOfBitsStr + "-1 DOWNTO 0);");
      Contents.add("   Remainder <= s_mod_result(" + NrOfBitsStr + "-1 DOWNTO 0);");
    }
    return Contents;
  }

  @Override
  public SortedMap<String, Integer> GetOutputList(Netlist TheNetlist, AttributeSet attrs) {
    SortedMap<String, Integer> Outputs = new TreeMap<>();
    Outputs.put("Quotient", NrOfBitsId);
    Outputs.put("Remainder", NrOfBitsId);
    return Outputs;
  }

  @Override
  public SortedMap<Integer, String> GetParameterList(AttributeSet attrs) {
    SortedMap<Integer, String> Parameters = new TreeMap<>();
    Parameters.put(NrOfBitsId, NrOfBitsStr);
    Parameters.put(CalcBitsId, CalcBitsStr);
    Parameters.put(UnsignedId, UnsignedStr);
    return Parameters;
  }

  @Override
  public SortedMap<String, Integer> GetParameterMap(Netlist Nets, NetlistComponent ComponentInfo) {
    SortedMap<String, Integer> ParameterMap = new TreeMap<>();
    int NrOfBits =
        ComponentInfo.GetComponent().getAttributeSet().getValue(StdAttr.WIDTH).getWidth();
    boolean isUnsigned =
        ComponentInfo.GetComponent()
            .getAttributeSet()
            .getValue(Multiplier.MODE_ATTR)
            .equals(Multiplier.UNSIGNED_OPTION);
    // TODO(kwalsh) - null the upper if not connected, or add a parameter
    int CalcBits = 2 * NrOfBits;
    ParameterMap.put(NrOfBitsStr, NrOfBits);
    ParameterMap.put(CalcBitsStr, CalcBits);
    ParameterMap.put(UnsignedStr, isUnsigned ? 1 : 0);
    return ParameterMap;
  }

  @Override
  public SortedMap<String, String> GetPortMap(Netlist Nets, Object MapInfo) {
    SortedMap<String, String> PortMap = new TreeMap<>();
	if (!(MapInfo instanceof NetlistComponent)) return PortMap;
	NetlistComponent ComponentInfo = (NetlistComponent) MapInfo;
    PortMap.putAll(GetNetMap("INP_A", true, ComponentInfo, Divider.IN0, Nets));
    PortMap.putAll(GetNetMap("INP_B", true, ComponentInfo, Divider.IN1, Nets));
    PortMap.putAll(GetNetMap("Upper", true, ComponentInfo, Divider.UPPER, Nets));
    PortMap.putAll(GetNetMap("Quotient", true, ComponentInfo, Divider.OUT, Nets));
    PortMap.putAll(GetNetMap("Remainder", true, ComponentInfo, Divider.REM, Nets));
    return PortMap;
  }

  @Override
  public String GetSubDir() {
    return "arithmetic";
  }

  @Override
  public SortedMap<String, Integer> GetWireList(AttributeSet attrs, Netlist Nets) {
    SortedMap<String, Integer> Wires = new TreeMap<>();
    Wires.put("s_div_result", CalcBitsId);
    Wires.put("s_mod_result", NrOfBitsId);
    Wires.put("s_extended_dividend", CalcBitsId);
    return Wires;
  }

  @Override
  public boolean HDLTargetSupported(AttributeSet attrs) {
    return HDL.isVHDL();
  }
}
