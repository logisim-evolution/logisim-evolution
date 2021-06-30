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

import java.util.ArrayList;
import java.util.SortedMap;
import java.util.TreeMap;

public class MultiplierHDLGeneratorFactory extends AbstractHDLGeneratorFactory {
  private static final String NrOfBitsStr = "NrOfBits";
  private static final int NrOfBitsId = -1;
  private static final String CalcBitsStr = "NrOfCalcBits";
  private static final int CalcBitsId = -2;
  private static final String UnsignedStr = "UnsignedMultiplier";
  private static final int UnsignedId = -3;

  @Override
  public String getComponentStringIdentifier() {
    return "MULT";
  }

  @Override
  public SortedMap<String, Integer> GetInputList(Netlist TheNetlist, AttributeSet attrs) {
    final var inputs = new TreeMap<String, Integer>();
    inputs.put("INP_A", NrOfBitsId);
    inputs.put("INP_B", NrOfBitsId);
    inputs.put("Cin", NrOfBitsId);
    return inputs;
  }

  @Override
  public ArrayList<String> GetModuleFunctionality(Netlist TheNetlist, AttributeSet attrs) {
    ArrayList<String> Contents = new ArrayList<>();
    if (HDL.isVHDL()) {
      Contents.add("   s_mult_result <= std_logic_vector(unsigned(INP_A)*unsigned(INP_B))");
      Contents.add("                       WHEN " + UnsignedStr + "= 1 ELSE");
      Contents.add("                    std_logic_vector(signed(INP_A)*signed(INP_B));");
      Contents.add(
          "   s_extended_Cin("
              + CalcBitsStr
              + "-1 DOWNTO "
              + NrOfBitsStr
              + ") <= (OTHERS => '0') WHEN "
              + UnsignedStr
              + "= 1 ELSE (OTHERS => Cin("
              + NrOfBitsStr
              + "-1));");
      Contents.add("   s_extended_Cin(" + NrOfBitsStr + "-1 DOWNTO 0) <= Cin;");
      Contents.add(
          "   s_new_result  <= std_logic_vector(unsigned(s_mult_result) + unsigned(s_extended_Cin))");
      Contents.add("                       WHEN " + UnsignedStr + "= 1 ELSE");
      Contents.add(
          "                    std_logic_vector(signed(s_mult_result) + signed(s_extended_Cin));");
      Contents.add(
          "   Mult_hi       <= s_new_result(" + CalcBitsStr + "-1 DOWNTO " + NrOfBitsStr + ");");
      Contents.add("   Mult_lo       <= s_new_result(" + NrOfBitsStr + "-1 DOWNTO 0);");
    } else {
      Contents.add("   reg[" + CalcBitsStr + "-1:0] s_Cin;");
      Contents.add("   reg[" + CalcBitsStr + "-1:0] s_mult_unsigned;");
      Contents.add("   reg[" + CalcBitsStr + "-1:0] s_interm_result;");
      Contents.add("   reg signed[" + CalcBitsStr + "-1:0] s_mult_signed;");
      Contents.add("   always @(*)");
      Contents.add("   begin");
      Contents.add("      s_Cin[" + NrOfBitsStr + "-1:0] = Cin;");
      Contents.add("      if (" + UnsignedStr + "== 1)");
      Contents.add("         begin");
      Contents.add("            s_Cin[" + CalcBitsStr + "-1:" + NrOfBitsStr + "] = 0;");
      Contents.add("            s_mult_unsigned = $unsigned(INP_A) * $unsigned(INP_B);");
      Contents.add("            s_interm_result = $unsigned(s_mult_unsigned) + $unsigned(s_Cin);");
      Contents.add("          end");
      Contents.add("       else");
      Contents.add("         begin");
      Contents.add("            if (Cin[" + NrOfBitsStr + "-1] == 1)");
      Contents.add("               s_Cin[" + CalcBitsStr + "-1:" + NrOfBitsStr + "] = -1;");
      Contents.add("            else");
      Contents.add("               s_Cin[" + CalcBitsStr + "-1:" + NrOfBitsStr + "] = 0;");
      Contents.add("            s_mult_signed = $signed(INP_A) * $signed(INP_B);");
      Contents.add("            s_interm_result = $signed(s_mult_signed) + $signed(s_Cin);");
      Contents.add("          end");
      Contents.add("   end");
      Contents.add("   ");
      Contents.add("   assign Mult_hi = s_interm_result[" + CalcBitsStr + "-1:" + NrOfBitsStr + "];");
      Contents.add("   assign Mult_lo = s_interm_result[" + NrOfBitsStr + "-1:0];");
    }
    return Contents;
  }

  @Override
  public SortedMap<String, Integer> GetOutputList(Netlist TheNetlist, AttributeSet attrs) {
    final var outputs = new TreeMap<String, Integer>();
    outputs.put("Mult_lo", NrOfBitsId);
    outputs.put("Mult_hi", NrOfBitsId);
    return outputs;
  }

  @Override
  public SortedMap<Integer, String> GetParameterList(AttributeSet attrs) {
    final var parameters = new TreeMap<Integer, String>();
    parameters.put(NrOfBitsId, NrOfBitsStr);
    parameters.put(CalcBitsId, CalcBitsStr);
    parameters.put(UnsignedId, UnsignedStr);
    return parameters;
  }

  @Override
  public SortedMap<String, Integer> GetParameterMap(Netlist Nets, NetlistComponent ComponentInfo) {
    final var parameterMap = new TreeMap<String, Integer>();
    final var nrOfBits = ComponentInfo.GetComponent().getEnd(0).getWidth().getWidth();
    boolean isUnsigned =
        ComponentInfo.GetComponent()
            .getAttributeSet()
            .getValue(Multiplier.MODE_ATTR)
            .equals(Multiplier.UNSIGNED_OPTION);
    parameterMap.put(NrOfBitsStr, nrOfBits);
    parameterMap.put(CalcBitsStr, 2 * nrOfBits);
    parameterMap.put(UnsignedStr, isUnsigned ? 1 : 0);
    return parameterMap;
  }

  @Override
  public SortedMap<String, String> GetPortMap(Netlist Nets, Object MapInfo) {
    final var portMap = new TreeMap<String, String>();
    if (!(MapInfo instanceof NetlistComponent)) return portMap;
    NetlistComponent ComponentInfo = (NetlistComponent) MapInfo;
    portMap.putAll(GetNetMap("INP_A", true, ComponentInfo, Multiplier.IN0, Nets));
    portMap.putAll(GetNetMap("INP_B", true, ComponentInfo, Multiplier.IN1, Nets));
    portMap.putAll(GetNetMap("Cin", true, ComponentInfo, Multiplier.C_IN, Nets));
    portMap.putAll(GetNetMap("Mult_lo", true, ComponentInfo, Multiplier.OUT, Nets));
    portMap.putAll(GetNetMap("Mult_hi", true, ComponentInfo, Multiplier.C_OUT, Nets));
    return portMap;
  }

  @Override
  public String GetSubDir() {
    return "arithmetic";
  }

  @Override
  public SortedMap<String, Integer> GetWireList(AttributeSet attrs, Netlist Nets) {
    final var wires = new TreeMap<String, Integer>();
    wires.put("s_mult_result", CalcBitsId);
    wires.put("s_extended_Cin", CalcBitsId);
    wires.put("s_new_result", CalcBitsId);
    return wires;
  }

  @Override
  public boolean HDLTargetSupported(AttributeSet attrs) {
    return true;
  }
}
