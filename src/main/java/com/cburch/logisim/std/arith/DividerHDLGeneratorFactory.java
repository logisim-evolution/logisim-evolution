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
import com.cburch.logisim.util.LineBuffer;
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
    final var map = new TreeMap<String, Integer>();
    map.put("INP_A", NrOfBitsId);
    map.put("INP_B", NrOfBitsId);
    map.put("Upper", NrOfBitsId);
    return map;
  }

  @Override
  public ArrayList<String> GetModuleFunctionality(Netlist TheNetlist, AttributeSet attrs) {
    final var Contents = (new LineBuffer())
            .pair("nrOfBits", NrOfBitsStr)
            .pair("unsigned", UnsignedStr)
            .pair("calcBits", CalcBitsStr);

    if (HDL.isVHDL()) {
      Contents.addLines(
          "s_extended_dividend({{calcBits}}-1 DOWNTO {{nrOfBits}}) <= Upper;",
          "s_extended_dividend({{nrOfBits}}-1 DOWNTO 0) <= INP_A;",
          "s_div_result <= std_logic_vector(unsigned(s_extended_dividend) / unsigned(INP_B))",
          "                   WHEN {{unsigned}} = 1 ELSE",
          "                std_logic_vector(signed(s_extended_dividend) / signed(INP_B));",
          "s_mod_result <= std_logic_vector(unsigned(s_extended_dividend) mod unsigned(INP_B))",
          "                   WHEN {{unsigned}} = 1 ELSE",
          "                std_logic_vector(signed(s_extended_dividend) mod signed(INP_B));",
          "Quotient  <= s_div_result({{nrOfBits}}-1 DOWNTO 0);",
          "Remainder <= s_mod_result({{nrOfBits}}-1 DOWNTO 0);");
    }
    return Contents.getWithIndent();
  }

  @Override
  public SortedMap<String, Integer> GetOutputList(Netlist TheNetlist, AttributeSet attrs) {
    final var map = new TreeMap<String, Integer>();
    map.put("Quotient", NrOfBitsId);
    map.put("Remainder", NrOfBitsId);
    return map;
  }

  @Override
  public SortedMap<Integer, String> GetParameterList(AttributeSet attrs) {
    final var map = new TreeMap<Integer, String>();
    map.put(NrOfBitsId, NrOfBitsStr);
    map.put(CalcBitsId, CalcBitsStr);
    map.put(UnsignedId, UnsignedStr);
    return map;
  }

  @Override
  public SortedMap<String, Integer> GetParameterMap(Netlist Nets, NetlistComponent ComponentInfo) {
    final var map = new TreeMap<String, Integer>();
    final var nrOfBits =
        ComponentInfo.getComponent().getAttributeSet().getValue(StdAttr.WIDTH).getWidth();
    final var isUnsigned = ComponentInfo.getComponent()
            .getAttributeSet()
            .getValue(Multiplier.MODE_ATTR)
            .equals(Multiplier.UNSIGNED_OPTION);
    // TODO(kwalsh) - null the upper if not connected, or add a parameter
    final var CalcBits = 2 * nrOfBits;
    map.put(NrOfBitsStr, nrOfBits);
    map.put(CalcBitsStr, CalcBits);
    map.put(UnsignedStr, isUnsigned ? 1 : 0);
    return map;
  }

  @Override
  public SortedMap<String, String> GetPortMap(Netlist Nets, Object MapInfo) {
    final var portMap = new TreeMap<String, String>();
    if (!(MapInfo instanceof NetlistComponent)) return portMap;
    NetlistComponent ComponentInfo = (NetlistComponent) MapInfo;
    portMap.putAll(GetNetMap("INP_A", true, ComponentInfo, Divider.IN0, Nets));
    portMap.putAll(GetNetMap("INP_B", true, ComponentInfo, Divider.IN1, Nets));
    portMap.putAll(GetNetMap("Upper", true, ComponentInfo, Divider.UPPER, Nets));
    portMap.putAll(GetNetMap("Quotient", true, ComponentInfo, Divider.OUT, Nets));
    portMap.putAll(GetNetMap("Remainder", true, ComponentInfo, Divider.REM, Nets));
    return portMap;
  }

  @Override
  public String GetSubDir() {
    return "arithmetic";
  }

  @Override
  public SortedMap<String, Integer> GetWireList(AttributeSet attrs, Netlist Nets) {
    final var wires = new TreeMap<String, Integer>();
    wires.put("s_div_result", CalcBitsId);
    wires.put("s_mod_result", NrOfBitsId);
    wires.put("s_extended_dividend", CalcBitsId);
    return wires;
  }

  @Override
  public boolean HDLTargetSupported(AttributeSet attrs) {
    return HDL.isVHDL();
  }
}
