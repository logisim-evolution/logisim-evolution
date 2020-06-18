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

public class DecoderHDLGeneratorFactory extends AbstractHDLGeneratorFactory {

  @Override
  public String getComponentStringIdentifier() {
    return "BINDECODER";
  }

  @Override
  public SortedMap<String, Integer> GetInputList(Netlist TheNetlist, AttributeSet attrs) {
    SortedMap<String, Integer> Inputs = new TreeMap<String, Integer>();
    Inputs.put("Enable", 1);
    Inputs.put("Sel", attrs.getValue(Plexers.ATTR_SELECT).getWidth());
    return Inputs;
  }

  @Override
  public ArrayList<String> GetModuleFunctionality(
      Netlist TheNetlist, AttributeSet attrs, FPGAReport Reporter, String HDLType) {
    ArrayList<String> Contents = new ArrayList<String>();
    int nr_of_select_bits = attrs.getValue(Plexers.ATTR_SELECT).getWidth();
    int num_outputs = (1 << nr_of_select_bits);
    String Space = " ";
    for (int i = 0; i < num_outputs; i++) {
      String binValue = IntToBin(i, nr_of_select_bits, HDLType);
      if (i == 10) Space = "";
      if (HDLType.equals(VHDL)) {
        Contents.add("   DecoderOut_" + i + Space + "<= '1' WHEN sel = " + binValue + " AND");
        Contents.add(Space + "                             Enable = '1' ELSE '0';");
      } else {
        Contents.add(
            "   assign DecoderOut_"
                + Integer.toString(i)
                + Space
                + " = (Enable&(sel == "
                + binValue
                + ")) ? 1'b1 : 1'b0;");
      }
    }
    return Contents;
  }

  @Override
  public SortedMap<String, Integer> GetOutputList(Netlist TheNetlist, AttributeSet attrs) {
    SortedMap<String, Integer> Outputs = new TreeMap<String, Integer>();
    for (int i = 0; i < (1 << attrs.getValue(Plexers.ATTR_SELECT).getWidth()); i++) {
      Outputs.put("DecoderOut_" + Integer.toString(i), 1);
    }
    return Outputs;
  }

  @Override
  public SortedMap<String, String> GetPortMap(
      Netlist Nets, Object MapInfo, FPGAReport Reporter, String HDLType) {
    SortedMap<String, String> PortMap = new TreeMap<String, String>();
    if (!(MapInfo instanceof NetlistComponent)) return PortMap;
    NetlistComponent ComponentInfo = (NetlistComponent) MapInfo;
    int nr_of_select_bits =
        ComponentInfo.GetComponent().getAttributeSet().getValue(Plexers.ATTR_SELECT).getWidth();
    int select_input_index = (1 << nr_of_select_bits);
    // first outputs
    for (int i = 0; i < select_input_index; i++)
      PortMap.putAll(
          GetNetMap(
              "DecoderOut_" + Integer.toString(i),
              true,
              ComponentInfo,
              i,
              Reporter,
              HDLType,
              Nets));
    // select..
    PortMap.putAll(
        GetNetMap("Sel", true, ComponentInfo, select_input_index, Reporter, HDLType, Nets));

    // now connect enable input...
    if (ComponentInfo.GetComponent()
        .getAttributeSet()
        .getValue(Plexers.ATTR_ENABLE)
        .booleanValue()) {
      PortMap.putAll(
          GetNetMap(
              "Enable", false, ComponentInfo, select_input_index + 1, Reporter, HDLType, Nets));
    } else {
      String SetBit = (HDLType.equals(VHDL)) ? "'1'" : "1'b1";
      PortMap.put("Enable", SetBit);
    }
    return PortMap;
  }

  @Override
  public String GetSubDir() {
    return "plexers";
  }

  @Override
  public boolean HDLTargetSupported(String HDLType, AttributeSet attrs) {
    return true;
  }
}
