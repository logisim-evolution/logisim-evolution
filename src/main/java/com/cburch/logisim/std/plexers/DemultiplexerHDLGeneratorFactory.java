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
import com.cburch.logisim.fpga.fpgagui.FPGAReport;
import com.cburch.logisim.fpga.hdlgenerator.AbstractHDLGeneratorFactory;
import com.cburch.logisim.instance.StdAttr;
import java.util.ArrayList;
import java.util.SortedMap;
import java.util.TreeMap;

public class DemultiplexerHDLGeneratorFactory extends AbstractHDLGeneratorFactory {

  private static final String NrOfBitsStr = "NrOfBits";
  private static final int NrOfBitsId = -1;

  @Override
  public String getComponentStringIdentifier() {
    return "DEMUX";
  }

  @Override
  public SortedMap<String, Integer> GetInputList(Netlist TheNetlist, AttributeSet attrs) {
    SortedMap<String, Integer> Inputs = new TreeMap<String, Integer>();
    int NrOfBits = (attrs.getValue(StdAttr.WIDTH).getWidth() == 1) ? 1 : NrOfBitsId;
    int nr_of_select_bits = attrs.getValue(Plexers.ATTR_SELECT).getWidth();
    Inputs.put("DemuxIn", NrOfBits);
    Inputs.put("Enable", 1);
    Inputs.put("Sel", nr_of_select_bits);
    return Inputs;
  }

  @Override
  public ArrayList<String> GetModuleFunctionality(
      Netlist TheNetlist, AttributeSet attrs, FPGAReport Reporter, String HDLType) {
    ArrayList<String> Contents = new ArrayList<String>();
    String Space = "  ";
    int nr_of_select_bits = attrs.getValue(Plexers.ATTR_SELECT).getWidth();
    int num_outputs = (1 << nr_of_select_bits);
    for (int i = 0; i < num_outputs; i++) {
      if (i == 10) {
        Space = " ";
      }
      String binValue = IntToBin(i, nr_of_select_bits, HDLType);
      if (HDLType.equals(VHDL)) {
        Contents.add("   DemuxOut_" + i + Space + "<= DemuxIn WHEN Sel = " + binValue + " AND");
        if (attrs.getValue(StdAttr.WIDTH).getWidth() > 1) {
          Contents.add("                               Enable = '1' ELSE (OTHERS => '0');");
        } else {
          Contents.add("                               Enable = '1' ELSE '0';");
        }
      } else {
        Contents.add(
            "   assign DemuxOut_"
                + Integer.toString(i)
                + Space
                + " = (Enable&(Sel == "
                + binValue
                + " )) ? DemuxIn : 0;");
      }
    }
    return Contents;
  }

  @Override
  public SortedMap<String, Integer> GetOutputList(Netlist TheNetlist, AttributeSet attrs) {
    SortedMap<String, Integer> Outputs = new TreeMap<String, Integer>();
    int NrOfBits = (attrs.getValue(StdAttr.WIDTH).getWidth() == 1) ? 1 : NrOfBitsId;
    int nr_of_select_bits = attrs.getValue(Plexers.ATTR_SELECT).getWidth();
    for (int i = 0; i < (1 << nr_of_select_bits); i++) {
      Outputs.put("DemuxOut_" + Integer.toString(i), NrOfBits);
    }
    return Outputs;
  }

  @Override
  public SortedMap<Integer, String> GetParameterList(AttributeSet attrs) {
    SortedMap<Integer, String> Parameters = new TreeMap<Integer, String>();
    int NrOfBits = attrs.getValue(StdAttr.WIDTH).getWidth();
    if (NrOfBits > 1) Parameters.put(NrOfBitsId, NrOfBitsStr);
    return Parameters;
  }

  @Override
  public SortedMap<String, Integer> GetParameterMap(
      Netlist Nets, NetlistComponent ComponentInfo, FPGAReport Reporter) {
    SortedMap<String, Integer> ParameterMap = new TreeMap<String, Integer>();
    int NrOfBits =
        ComponentInfo.GetComponent().getAttributeSet().getValue(StdAttr.WIDTH).getWidth();
    if (NrOfBits > 1) ParameterMap.put(NrOfBitsStr, NrOfBits);
    return ParameterMap;
  }

  @Override
  public SortedMap<String, String> GetPortMap(
      Netlist Nets, NetlistComponent ComponentInfo, FPGAReport Reporter, String HDLType) {
    SortedMap<String, String> PortMap = new TreeMap<String, String>();
    int nr_of_select_bits =
        ComponentInfo.GetComponent().getAttributeSet().getValue(Plexers.ATTR_SELECT).getWidth();
    int select_input_index = (1 << nr_of_select_bits);
    // begin with connecting all outputs of demultiplexer
    for (int i = 0; i < select_input_index; i++)
      PortMap.putAll(
          GetNetMap(
              "DemuxOut_" + Integer.toString(i), true, ComponentInfo, i, Reporter, HDLType, Nets));
    // now select..
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
      select_input_index--; // decrement pin index because enable doesn't
      // exist...
    }
    // finally input
    PortMap.putAll(
        GetNetMap("DemuxIn", true, ComponentInfo, select_input_index + 2, Reporter, HDLType, Nets));
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
