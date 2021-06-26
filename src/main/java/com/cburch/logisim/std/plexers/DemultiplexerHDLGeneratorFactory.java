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

public class DemultiplexerHDLGeneratorFactory extends AbstractHDLGeneratorFactory {

  private static final String NrOfBitsStr = "NrOfBits";
  private static final int NrOfBitsId = -1;

  @Override
  public String getComponentStringIdentifier() {
    return "DEMUX";
  }

  @Override
  public SortedMap<String, Integer> GetInputList(Netlist TheNetlist, AttributeSet attrs) {
    SortedMap<String, Integer> Inputs = new TreeMap<>();
    int NrOfBits = (attrs.getValue(StdAttr.WIDTH).getWidth() == 1) ? 1 : NrOfBitsId;
    int nr_of_select_bits = attrs.getValue(Plexers.ATTR_SELECT).getWidth();
    Inputs.put("DemuxIn", NrOfBits);
    Inputs.put("Enable", 1);
    Inputs.put("Sel", nr_of_select_bits);
    return Inputs;
  }

  @Override
  public ArrayList<String> GetModuleFunctionality(Netlist theNetList, AttributeSet attrs) {
    final var contents = new ArrayList<String>();
    var space = "  ";
    final var nrOfSelectBits = attrs.getValue(Plexers.ATTR_SELECT).getWidth();
    var numOutputs = (1 << nrOfSelectBits);
    for (var i = 0; i < numOutputs; i++) {
      if (i == 10) {
        space = " ";
      }
      final var binValue = IntToBin(i, nrOfSelectBits);
      if (HDL.isVHDL()) {
        contents.add("   DemuxOut_" + i + space + "<= DemuxIn WHEN sel = " + binValue + " AND");
        if (attrs.getValue(StdAttr.WIDTH).getWidth() > 1) {
          contents.add("                               Enable = '1' ELSE (OTHERS => '0');");
        } else {
          contents.add("                               Enable = '1' ELSE '0';");
        }
      } else {
        contents.add(
            "   assign DemuxOut_"
                + i
                + space
                + " = (Enable&(sel == "
                + binValue
                + " )) ? DemuxIn : 0;");
      }
    }
    return contents;
  }

  @Override
  public SortedMap<String, Integer> GetOutputList(Netlist theNetList, AttributeSet attrs) {
    SortedMap<String, Integer> outputs = new TreeMap<>();
    final var nrOfBits = (attrs.getValue(StdAttr.WIDTH).getWidth() == 1) ? 1 : NrOfBitsId;
    final var nrOfSelectBits = attrs.getValue(Plexers.ATTR_SELECT).getWidth();
    for (var i = 0; i < (1 << nrOfSelectBits); i++) {
      outputs.put("DemuxOut_" + i, nrOfBits);
    }
    return outputs;
  }

  @Override
  public SortedMap<Integer, String> GetParameterList(AttributeSet attrs) {
    SortedMap<Integer, String> params = new TreeMap<>();
    final var nrOfBits = attrs.getValue(StdAttr.WIDTH).getWidth();
    if (nrOfBits > 1) params.put(NrOfBitsId, NrOfBitsStr);
    return params;
  }

  @Override
  public SortedMap<String, Integer> GetParameterMap(Netlist nets, NetlistComponent componentInfo) {
    SortedMap<String, Integer> parameterMap = new TreeMap<>();
    final var nrOfBits =
        componentInfo.GetComponent().getAttributeSet().getValue(StdAttr.WIDTH).getWidth();
    if (nrOfBits > 1) parameterMap.put(NrOfBitsStr, nrOfBits);
    return parameterMap;
  }

  @Override
  public SortedMap<String, String> GetPortMap(Netlist nets, Object mapInfo) {
    SortedMap<String, String> portMap = new TreeMap<>();
    if (!(mapInfo instanceof NetlistComponent)) return portMap;
    final var ComponentInfo = (NetlistComponent) mapInfo;
    final var nrOfSelectBits =
        ComponentInfo.GetComponent().getAttributeSet().getValue(Plexers.ATTR_SELECT).getWidth();
    var selectInputIndex = (1 << nrOfSelectBits);
    // begin with connecting all outputs of demultiplexer
    for (var i = 0; i < selectInputIndex; i++)
      portMap.putAll(GetNetMap("DemuxOut_" + i, true, ComponentInfo, i, nets));
    // now select..
    portMap.putAll(GetNetMap("Sel", true, ComponentInfo, selectInputIndex, nets));
    // now connect enable input...
    if (ComponentInfo.GetComponent().getAttributeSet().getValue(Plexers.ATTR_ENABLE)) {
      portMap.putAll(GetNetMap("Enable", false, ComponentInfo, selectInputIndex + 1, nets));
    } else {
      portMap.put("Enable", HDL.oneBit());
      selectInputIndex--; // decrement pin index because enable doesn't exist...
    }
    // finally input
    portMap.putAll(GetNetMap("DemuxIn", true, ComponentInfo, selectInputIndex + 2, nets));
    return portMap;
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
