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

package com.cburch.logisim.std.plexers;

import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.fpga.designrulecheck.Netlist;
import com.cburch.logisim.fpga.designrulecheck.NetlistComponent;
import com.cburch.logisim.fpga.hdlgenerator.AbstractHDLGeneratorFactory;
import com.cburch.logisim.fpga.hdlgenerator.HDL;
import com.cburch.logisim.instance.StdAttr;
import java.util.ArrayList;
import java.util.SortedMap;
import java.util.TreeMap;

public class MultiplexerHDLGeneratorFactory extends AbstractHDLGeneratorFactory {

  private static final String NrOfBitsStr = "NrOfBits";
  private static final int NrOfBitsId = -1;

  @Override
  public String getComponentStringIdentifier() {
    return "MUX";
  }

  @Override
  public SortedMap<String, Integer> GetInputList(Netlist TheNetlist, AttributeSet attrs) {
    SortedMap<String, Integer> Inputs = new TreeMap<>();
    int nr_of_select_bits = attrs.getValue(Plexers.ATTR_SELECT).getWidth();
    int NrOfBits = (attrs.getValue(StdAttr.WIDTH).getWidth() == 1) ? 1 : NrOfBitsId;
    for (int i = 0; i < (1 << nr_of_select_bits); i++)
      Inputs.put("MuxIn_" + i, NrOfBits);
    Inputs.put("Enable", 1);
    Inputs.put("Sel", nr_of_select_bits);
    return Inputs;
  }

  @Override
  public ArrayList<String> GetModuleFunctionality(Netlist TheNetlist, AttributeSet attrs) {
    ArrayList<String> Contents = new ArrayList<>();
    int nr_of_select_bits = attrs.getValue(Plexers.ATTR_SELECT).getWidth();
    if (HDL.isVHDL()) {
      Contents.add("   make_mux : PROCESS( Enable,");
      for (int i = 0; i < (1 << nr_of_select_bits); i++)
        Contents.add("                       MuxIn_" + i + ",");
      Contents.add("                       Sel )");
      Contents.add("   BEGIN");
      Contents.add("      IF (Enable = '0') THEN");
      if (attrs.getValue(StdAttr.WIDTH).getWidth() > 1)
        Contents.add("         MuxOut <= (OTHERS => '0');");
      else Contents.add("         MuxOut <= '0';");
      Contents.add("                        ELSE");
      Contents.add("         CASE (Sel) IS");
      for (int i = 0; i < (1 << nr_of_select_bits) - 1; i++)
        Contents.add(
            "            WHEN "
                + IntToBin(i, nr_of_select_bits)
                + " => MuxOut <= MuxIn_"
                + i
                + ";");
      Contents.add(
          "            WHEN OTHERS  => MuxOut <= MuxIn_"
              + ((1 << nr_of_select_bits) - 1)
              + ";");
      Contents.add("         END CASE;");
      Contents.add("      END IF;");
      Contents.add("   END PROCESS make_mux;");
    } else {
      Contents.add("   assign MuxOut = s_selected_vector;");
      Contents.add("");
      Contents.add("   always @(*)");
      Contents.add("   begin");
      Contents.add("      if (~Enable) s_selected_vector <= 0;");
      Contents.add("      else case (Sel)");
      for (int i = 0; i < (1 << nr_of_select_bits) - 1; i++) {
        Contents.add("         " + IntToBin(i, nr_of_select_bits) + ":");
        Contents.add("            s_selected_vector <= MuxIn_" + i + ";");
      }
      Contents.add("         default:");
      Contents.add(
          "            s_selected_vector <= MuxIn_"
              + ((1 << nr_of_select_bits) - 1)
              + ";");
      Contents.add("      endcase");
      Contents.add("   end");
    }
    return Contents;
  }

  @Override
  public SortedMap<String, Integer> GetOutputList(Netlist TheNetlist, AttributeSet attrs) {
    SortedMap<String, Integer> Outputs = new TreeMap<>();
    int NrOfBits = (attrs.getValue(StdAttr.WIDTH).getWidth() == 1) ? 1 : NrOfBitsId;
    Outputs.put("MuxOut", NrOfBits);
    return Outputs;
  }

  @Override
  public SortedMap<Integer, String> GetParameterList(AttributeSet attrs) {
    SortedMap<Integer, String> Parameters = new TreeMap<>();
    int NrOfBits = attrs.getValue(StdAttr.WIDTH).getWidth();
    if (NrOfBits > 1) Parameters.put(NrOfBitsId, NrOfBitsStr);
    return Parameters;
  }

  @Override
  public SortedMap<String, Integer> GetParameterMap(Netlist Nets, NetlistComponent ComponentInfo) {
    SortedMap<String, Integer> ParameterMap = new TreeMap<>();
    int NrOfBits =
        ComponentInfo.GetComponent().getAttributeSet().getValue(StdAttr.WIDTH).getWidth();
    if (NrOfBits > 1) ParameterMap.put(NrOfBitsStr, NrOfBits);
    return ParameterMap;
  }

  @Override
  public SortedMap<String, String> GetPortMap(Netlist Nets, Object MapInfo) {
    SortedMap<String, String> PortMap = new TreeMap<>();
    if (!(MapInfo instanceof NetlistComponent)) return PortMap;
    NetlistComponent ComponentInfo = (NetlistComponent) MapInfo;
    int nr_of_select_bits =
        ComponentInfo.GetComponent().getAttributeSet().getValue(Plexers.ATTR_SELECT).getWidth();
    int select_input_index = (1 << nr_of_select_bits);
    // begin with connecting all inputs of multiplexer
    for (int i = 0; i < select_input_index; i++)
      PortMap.putAll(
          GetNetMap(
              "MuxIn_" + i, true, ComponentInfo, i, Nets));
    // now select..
    PortMap.putAll(
        GetNetMap("Sel", true, ComponentInfo, select_input_index, Nets));
    // now connect enable input...
    if (ComponentInfo.GetComponent()
        .getAttributeSet()
        .getValue(Plexers.ATTR_ENABLE)
        .booleanValue()) {
      PortMap.putAll(
          GetNetMap(
              "Enable", false, ComponentInfo, select_input_index + 1, Nets));
    } else {
      PortMap.put("Enable", HDL.oneBit());
      select_input_index--; // decrement pin index because enable doesn't exist...
    }
    // finally output
    PortMap.putAll(GetNetMap("MuxOut", true, ComponentInfo, select_input_index + 2, Nets));
    return PortMap;
  }

  @Override
  public SortedMap<String, Integer> GetRegList(AttributeSet attrs) {
    SortedMap<String, Integer> Regs = new TreeMap<>();
    int NrOfBits = (attrs.getValue(StdAttr.WIDTH).getWidth() == 1) ? 1 : NrOfBitsId;
    if (HDL.isVerilog()) Regs.put("s_selected_vector", NrOfBits);
    return Regs;
  }

  @Override
  public String GetSubDir() {
    return "plexers";
  }

  @Override
  public boolean HDLTargetSupported(AttributeSet attrs) {
    return true;
  }
}
