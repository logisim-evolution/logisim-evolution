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

package com.cburch.logisim.std.arith;

import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.fpga.designrulecheck.Netlist;
import com.cburch.logisim.fpga.designrulecheck.NetlistComponent;
import com.cburch.logisim.fpga.gui.FPGAReport;
import com.cburch.logisim.fpga.hdlgenerator.AbstractHDLGeneratorFactory;
import com.cburch.logisim.instance.StdAttr;
import java.util.ArrayList;
import java.util.SortedMap;
import java.util.TreeMap;

public class ComparatorHDLGeneratorFactory extends AbstractHDLGeneratorFactory {

  private static final String NrOfBitsStr = "NrOfBits";
  private static final int NrOfBitsId = -1;
  private static final String TwosComplementStr = "TwosComplement";
  private static final int TwosComplementId = -2;

  @Override
  public String getComponentStringIdentifier() {
    return "Comparator";
  }

  @Override
  public SortedMap<String, Integer> GetInputList(Netlist TheNetlist, AttributeSet attrs) {
    SortedMap<String, Integer> Inputs = new TreeMap<String, Integer>();
    int inputbits = (attrs.getValue(StdAttr.WIDTH).getWidth() == 1) ? 1 : NrOfBitsId;
    Inputs.put("DataA", inputbits);
    Inputs.put("DataB", inputbits);
    return Inputs;
  }

  @Override
  public ArrayList<String> GetModuleFunctionality(
      Netlist TheNetlist, AttributeSet attrs, FPGAReport Reporter, String HDLType) {
    ArrayList<String> Contents = new ArrayList<String>();
    int nrOfBits = attrs.getValue(StdAttr.WIDTH).getWidth();
    if (HDLType.equals(VHDL)) {
      if (nrOfBits == 1) {
        Contents.add("   A_EQ_B <= DataA XNOR DataB;");
        Contents.add(
            "   A_LT_B <= DataA AND NOT(DataB) WHEN "
                + TwosComplementStr
                + " = 1 ELSE NOT(DataA) AND DataB;");
        Contents.add(
            "   A_GT_B <= NOT(DataA) AND DataB WHEN "
                + TwosComplementStr
                + " = 1 ELSE DataA AND NOT(DataB);");
      } else {
        Contents.add("   s_signed_less      <= '1' WHEN signed(DataA) < signed(DataB) ELSE '0';");
        Contents.add(
            "   s_unsigned_less    <= '1' WHEN unsigned(DataA) < unsigned(DataB) ELSE '0';");
        Contents.add("   s_signed_greater   <= '1' WHEN signed(DataA) > signed(DataB) ELSE '0';");
        Contents.add(
            "   s_unsigned_greater <= '1' WHEN unsigned(DataA) > unsigned(DataB) ELSE '0';");
        Contents.add("");
        Contents.add("   A_EQ_B <= '1' WHEN DataA = DataB ELSE '0';");
        Contents.add(
            "   A_GT_B <= s_signed_greater WHEN "
                + TwosComplementStr
                + "=1 ELSE s_unsigned_greater;");
        Contents.add(
            "   A_LT_B <= s_signed_less    WHEN " + TwosComplementStr + "=1 ELSE s_unsigned_less;");
      }
    } else {
      if (nrOfBits == 1) {
        Contents.add("   assign A_EQ_B = (DataA == DataB);");
        Contents.add("   assign A_LT_B = (DataA < DataB);");
        Contents.add("   assign A_GT_B = (DataA > DataB);");
      } else {
        Contents.add("   assign s_signed_less      = ($signed(DataA) < $signed(DataB));");
        Contents.add("   assign s_unsigned_less    = (DataA < DataB);");
        Contents.add("   assign s_signed_greater   = ($signed(DataA) > $signed(DataB));");
        Contents.add("   assign s_unsigned_greater = (DataA > DataB);");
        Contents.add("");
        Contents.add("   assign A_EQ_B = (DataA == DataB);");
        Contents.add(
            "   assign A_GT_B = ("
                + TwosComplementStr
                + "==1) ? s_signed_greater : s_unsigned_greater;");
        Contents.add(
            "   assign A_LT_B = (" + TwosComplementStr + "==1) ? s_signed_less : s_unsigned_less;");
      }
    }
    return Contents;
  }

  @Override
  public SortedMap<String, Integer> GetOutputList(Netlist TheNetlist, AttributeSet attrs) {
    SortedMap<String, Integer> Outputs = new TreeMap<String, Integer>();
    Outputs.put("A_GT_B", 1);
    Outputs.put("A_EQ_B", 1);
    Outputs.put("A_LT_B", 1);
    return Outputs;
  }

  @Override
  public SortedMap<Integer, String> GetParameterList(AttributeSet attrs) {
    SortedMap<Integer, String> Parameters = new TreeMap<Integer, String>();
    int inputbits = attrs.getValue(StdAttr.WIDTH).getWidth();
    if (inputbits > 1) {
      Parameters.put(NrOfBitsId, NrOfBitsStr);
    }
    Parameters.put(TwosComplementId, TwosComplementStr);
    return Parameters;
  }

  @Override
  public SortedMap<String, Integer> GetParameterMap(
      Netlist Nets, NetlistComponent ComponentInfo, FPGAReport Reporter) {
    SortedMap<String, Integer> ParameterMap = new TreeMap<String, Integer>();
    int nrOfBits = ComponentInfo.GetComponent().getEnd(0).getWidth().getWidth();
    int IsSigned = 0;
    AttributeSet attrs = ComponentInfo.GetComponent().getAttributeSet();
    if (attrs.containsAttribute(Comparator.MODE_ATTRIBUTE)) {
      if (attrs.getValue(Comparator.MODE_ATTRIBUTE).equals(Comparator.SIGNED_OPTION)) IsSigned = 1;
    }
    if (nrOfBits > 1) {
      ParameterMap.put(NrOfBitsStr, nrOfBits);
    }
    ParameterMap.put(TwosComplementStr, IsSigned);
    return ParameterMap;
  }

  @Override
  public SortedMap<String, String> GetPortMap(
	      Netlist Nets, Object MapInfo, FPGAReport Reporter, String HDLType) {
    SortedMap<String, String> PortMap = new TreeMap<String, String>();
	if (!(MapInfo instanceof NetlistComponent)) return PortMap;
	NetlistComponent ComponentInfo = (NetlistComponent) MapInfo;
    PortMap.putAll(GetNetMap("DataA", true, ComponentInfo, 0, Reporter, HDLType, Nets));
    PortMap.putAll(GetNetMap("DataB", true, ComponentInfo, 1, Reporter, HDLType, Nets));
    PortMap.putAll(GetNetMap("A_GT_B", true, ComponentInfo, 2, Reporter, HDLType, Nets));
    PortMap.putAll(GetNetMap("A_EQ_B", true, ComponentInfo, 3, Reporter, HDLType, Nets));
    PortMap.putAll(GetNetMap("A_LT_B", true, ComponentInfo, 4, Reporter, HDLType, Nets));
    return PortMap;
  }

  @Override
  public String GetSubDir() {
    return "arithmetic";
  }

  @Override
  public SortedMap<String, Integer> GetWireList(AttributeSet attrs, Netlist Nets) {
    SortedMap<String, Integer> Wires = new TreeMap<String, Integer>();
    int inputbits = attrs.getValue(StdAttr.WIDTH).getWidth();
    if (inputbits > 1) {
      Wires.put("s_signed_less", 1);
      Wires.put("s_unsigned_less", 1);
      Wires.put("s_signed_greater", 1);
      Wires.put("s_unsigned_greater", 1);
    }
    return Wires;
  }

  @Override
  public boolean HDLTargetSupported(String HDLType, AttributeSet attrs) {
    return true;
  }
}
