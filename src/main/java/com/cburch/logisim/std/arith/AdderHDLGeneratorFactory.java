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

public class AdderHDLGeneratorFactory extends AbstractHDLGeneratorFactory {

  private static final String NrOfBitsStr = "NrOfBits";
  private static final int NrOfBitsId = -1;
  private static final String ExtendedBitsStr = "ExtendedBits";
  private static final int ExtendedBitsId = -2;

  @Override
  public String getComponentStringIdentifier() {
    return "ADDER2C";
  }

  @Override
  public SortedMap<String, Integer> GetInputList(Netlist TheNetlist, AttributeSet attrs) {
    SortedMap<String, Integer> Inputs = new TreeMap<>();
    int inputbits = (attrs.getValue(StdAttr.WIDTH).getWidth() == 1) ? 1 : NrOfBitsId;
    Inputs.put("DataA", inputbits);
    Inputs.put("DataB", inputbits);
    Inputs.put("CarryIn", 1);
    return Inputs;
  }

  @Override
  public ArrayList<String> GetModuleFunctionality(Netlist TheNetlist, AttributeSet attrs) {
    ArrayList<String> Contents = new ArrayList<>();
    int nrOfBits = attrs.getValue(StdAttr.WIDTH).getWidth();
    if (HDL.isVHDL()) {
      Contents.add("   s_extended_dataA <= \"0\"&DataA;");
      Contents.add("   s_extended_dataB <= \"0\"&DataB;");
      Contents.add("   s_sum_result     <= std_logic_vector(unsigned(s_extended_dataA)+");
      Contents.add("                                        unsigned(s_extended_dataB)+");
      Contents.add("                                        (\"\"&CarryIn));");
      Contents.add("");
      if (nrOfBits == 1) {
        Contents.add("   Result <= s_sum_result(0);");
      } else {
        Contents.add("   Result <= s_sum_result( (" + NrOfBitsStr + "-1) DOWNTO 0 );");
      }
      Contents.add("   CarryOut <= s_sum_result(" + ExtendedBitsStr + "-1);");
    } else {
      Contents.add("    assign   {CarryOut,Result} = DataA + DataB + CarryIn;");
    }
    return Contents;
  }

  @Override
  public SortedMap<String, Integer> GetOutputList(Netlist TheNetlist, AttributeSet attrs) {
    SortedMap<String, Integer> Outputs = new TreeMap<>();
    int outputbits = (attrs.getValue(StdAttr.WIDTH).getWidth() == 1) ? 1 : NrOfBitsId;
    Outputs.put("Result", outputbits);
    Outputs.put("CarryOut", 1);
    return Outputs;
  }

  @Override
  public SortedMap<Integer, String> GetParameterList(AttributeSet attrs) {
    SortedMap<Integer, String> Parameters = new TreeMap<>();
    int outputbits = attrs.getValue(StdAttr.WIDTH).getWidth();
    if (outputbits > 1) Parameters.put(NrOfBitsId, NrOfBitsStr);
    Parameters.put(ExtendedBitsId, ExtendedBitsStr);
    return Parameters;
  }

  @Override
  public SortedMap<String, Integer> GetParameterMap(
      Netlist Nets, NetlistComponent ComponentInfo) {
    SortedMap<String, Integer> ParameterMap = new TreeMap<>();
    int nrOfBits = ComponentInfo.GetComponent().getEnd(0).getWidth().getWidth();
    ParameterMap.put(ExtendedBitsStr, nrOfBits + 1);
    if (nrOfBits > 1) ParameterMap.put(NrOfBitsStr, nrOfBits);
    return ParameterMap;
  }

  @Override
  public SortedMap<String, String> GetPortMap(Netlist Nets, Object MapInfo) {
    SortedMap<String, String> PortMap = new TreeMap<>();
    if (!(MapInfo instanceof NetlistComponent)) return PortMap;
    NetlistComponent ComponentInfo = (NetlistComponent) MapInfo;
    PortMap.putAll(GetNetMap("DataA", true, ComponentInfo, 0, Nets));
    PortMap.putAll(GetNetMap("DataB", true, ComponentInfo, 1, Nets));
    PortMap.putAll(GetNetMap("Result", true, ComponentInfo, 2, Nets));
    PortMap.putAll(GetNetMap("CarryIn", true, ComponentInfo, 3, Nets));
    PortMap.putAll(GetNetMap("CarryOut", true, ComponentInfo, 4, Nets));
    return PortMap;
  }

  @Override
  public String GetSubDir() {
    return "arithmetic";
  }

  @Override
  public SortedMap<String, Integer> GetWireList(AttributeSet attrs, Netlist Nets) {
    SortedMap<String, Integer> Wires = new TreeMap<>();
    Wires.put("s_extended_dataA", ExtendedBitsId);
    Wires.put("s_extended_dataB", ExtendedBitsId);
    Wires.put("s_sum_result", ExtendedBitsId);
    return Wires;
  }

  @Override
  public boolean HDLTargetSupported(AttributeSet attrs) {
    return true;
  }
}
