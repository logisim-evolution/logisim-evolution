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

package com.cburch.logisim.std.plexers;

import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.fpga.designrulecheck.Netlist;
import com.cburch.logisim.fpga.designrulecheck.NetlistComponent;
import com.cburch.logisim.fpga.gui.FPGAReport;
import com.cburch.logisim.fpga.hdlgenerator.AbstractHDLGeneratorFactory;
import java.util.ArrayList;
import java.util.SortedMap;
import java.util.TreeMap;

public class BitSelectorHDLGeneratorFactory extends AbstractHDLGeneratorFactory {

  private static final String InputBitsStr = "NrOfInputBits";
  private static final int InputBitsId = -1;
  private static final String OutputsBitsStr = "NrOfOutputBits";
  private static final int OutputsBitsId = -2;
  private static final String SelectBitsStr = "NrOfSelBits";
  private static final int SelectBitsId = -3;
  private static final String ExtendedBitsStr = "NrOfExtendedBits";
  private static final int ExtendedBitsId = -4;

  @Override
  public String getComponentStringIdentifier() {
    return "BITSELECTOR";
  }

  @Override
  public SortedMap<String, Integer> GetInputList(Netlist TheNetlist, AttributeSet attrs) {
    SortedMap<String, Integer> Inputs = new TreeMap<String, Integer>();
    Inputs.put("DataIn", InputBitsId);
    Inputs.put("Sel", SelectBitsId);
    return Inputs;
  }

  @Override
  public ArrayList<String> GetModuleFunctionality(
      Netlist TheNetlist, AttributeSet attrs, FPGAReport Reporter, String HDLType) {
    ArrayList<String> Contents = new ArrayList<String>();
    int output_bits = attrs.getValue(BitSelector.GROUP_ATTR).getWidth();
    if (HDLType.equals(VHDL)) {
      Contents.add(
          "   s_extended_vector(("
              + ExtendedBitsStr
              + "-1) DOWNTO "
              + InputBitsStr
              + ") <= (OTHERS => '0');");
      Contents.add("   s_extended_vector((" + InputBitsStr + "-1) DOWNTO 0) <= DataIn;");
      if (output_bits > 1)
        Contents.add(
            "   DataOut <= s_extended_vector(((to_integer(unsigned(Sel))+1)*"
                + OutputsBitsStr
                + ")-1 DOWNTO to_integer(unsigned(Sel))*"
                + OutputsBitsStr
                + ");");
      else Contents.add("   DataOut <= s_extended_vector(to_integer(unsigned(Sel)));");
    } else {
      Contents.add(
          "   assign s_extended_vector[" + ExtendedBitsStr + "-1:" + InputBitsStr + "] = 0;");
      Contents.add("   assign s_extended_vector[" + InputBitsStr + "-1:0] = DataIn;");
      if (output_bits > 1) {
        Contents.add("   wire[513:0] s_select_vector;");
        Contents.add("   reg[" + OutputsBitsStr + "-1:0] s_selected_slice;");
        Contents.add("   assign s_select_vector[513:" + ExtendedBitsStr + "] = 0;");
        Contents.add("   assign s_select_vector[" + ExtendedBitsStr + "-1:0] = s_extended_vector;");
        Contents.add("   assign DataOut = s_selected_slice;");
        Contents.add("");
        Contents.add("   always @(*)");
        Contents.add("   begin");
        Contents.add("      case (Sel)");
        for (int i = 15; i > 0; i--) {
          Contents.add(
              "         "
                  + i
                  + " : s_selected_slice <= s_select_vector[("
                  + Integer.toString(i + 1)
                  + "*"
                  + OutputsBitsStr
                  + ")-1:"
                  + i
                  + "*"
                  + OutputsBitsStr
                  + "];");
        }
        Contents.add(
            "         default : s_selected_slice <= s_select_vector[" + OutputsBitsStr + "-1:0];");
        Contents.add("      endcase");
        Contents.add("   end");
      } else Contents.add("   assign DataOut = s_extended_vector[Sel];");
    }
    return Contents;
  }

  @Override
  public SortedMap<String, Integer> GetOutputList(Netlist TheNetlist, AttributeSet attrs) {
    SortedMap<String, Integer> Outputs = new TreeMap<String, Integer>();
    int output_bits = (attrs.getValue(BitSelector.GROUP_ATTR).getWidth() == 1) ? 1 : OutputsBitsId;
    Outputs.put("DataOut", output_bits);
    return Outputs;
  }

  @Override
  public SortedMap<Integer, String> GetParameterList(AttributeSet attrs) {
    SortedMap<Integer, String> Parameters = new TreeMap<Integer, String>();
    int output_bits = attrs.getValue(BitSelector.GROUP_ATTR).getWidth();
    Parameters.put(InputBitsId, InputBitsStr);
    if (output_bits > 1) Parameters.put(OutputsBitsId, OutputsBitsStr);
    Parameters.put(SelectBitsId, SelectBitsStr);
    Parameters.put(ExtendedBitsId, ExtendedBitsStr);
    return Parameters;
  }

  @Override
  public SortedMap<String, Integer> GetParameterMap(
      Netlist Nets, NetlistComponent ComponentInfo, FPGAReport Reporter) {
    SortedMap<String, Integer> ParameterMap = new TreeMap<String, Integer>();
    int sel_bits = ComponentInfo.GetComponent().getEnd(2).getWidth().getWidth();
    int input_bits = ComponentInfo.GetComponent().getEnd(1).getWidth().getWidth();
    int output_bits = ComponentInfo.GetComponent().getEnd(0).getWidth().getWidth();
    ParameterMap.put(InputBitsStr, input_bits);
    ParameterMap.put(SelectBitsStr, sel_bits);
    if (output_bits > 1) ParameterMap.put(OutputsBitsStr, output_bits);
    int nr_of_slices = 1;
    for (int i = 0; i < sel_bits; i++) {
      nr_of_slices <<= 1;
    }
    ParameterMap.put(ExtendedBitsStr, nr_of_slices * output_bits + 1);
    return ParameterMap;
  }

  @Override
  public SortedMap<String, String> GetPortMap(
      Netlist Nets, Object MapInfo, FPGAReport Reporter, String HDLType) {
    SortedMap<String, String> PortMap = new TreeMap<String, String>();
    if (!(MapInfo instanceof NetlistComponent)) return PortMap;
    NetlistComponent ComponentInfo = (NetlistComponent) MapInfo;
    PortMap.putAll(GetNetMap("DataIn", true, ComponentInfo, 1, Reporter, HDLType, Nets));
    PortMap.putAll(GetNetMap("Sel", true, ComponentInfo, 2, Reporter, HDLType, Nets));
    PortMap.putAll(GetNetMap("DataOut", true, ComponentInfo, 0, Reporter, HDLType, Nets));
    return PortMap;
  }

  @Override
  public String GetSubDir() {
    return "plexers";
  }

  @Override
  public SortedMap<String, Integer> GetWireList(AttributeSet attrs, Netlist Nets) {
    SortedMap<String, Integer> Wires = new TreeMap<String, Integer>();
    Wires.put("s_extended_vector", ExtendedBitsId);
    return Wires;
  }

  @Override
  public boolean HDLTargetSupported(String HDLType, AttributeSet attrs) {
    return true;
  }
}
