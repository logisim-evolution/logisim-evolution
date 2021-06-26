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
  public SortedMap<String, Integer> GetInputList(Netlist theNetList, AttributeSet attrs) {
    final var map = new TreeMap<String, Integer>();
    final var nrOfSelectBits = attrs.getValue(Plexers.ATTR_SELECT).getWidth();
    final var nrOfBits = (attrs.getValue(StdAttr.WIDTH).getWidth() == 1) ? 1 : NrOfBitsId;
    for (var i = 0; i < (1 << nrOfSelectBits); i++)
      map.put("MuxIn_" + i, nrOfBits);
    map.put("Enable", 1);
    map.put("Sel", nrOfSelectBits);
    return map;
  }

  @Override
  public ArrayList<String> GetModuleFunctionality(Netlist theNetList, AttributeSet attrs) {
    final var contents = new ArrayList<String>();
    int nrOfSelectBits = attrs.getValue(Plexers.ATTR_SELECT).getWidth();
    if (HDL.isVHDL()) {
      contents.add("   make_mux : PROCESS( Enable,");
      for (var i = 0; i < (1 << nrOfSelectBits); i++)
        contents.add("                       MuxIn_" + i + ",");
      contents.add("                       Sel )");
      contents.add("   BEGIN");
      contents.add("      IF (Enable = '0') THEN");
      if (attrs.getValue(StdAttr.WIDTH).getWidth() > 1)
        contents.add("         MuxOut <= (OTHERS => '0');");
      else contents.add("         MuxOut <= '0';");
      contents.add("                        ELSE");
      contents.add("         CASE (Sel) IS");
      for (var i = 0; i < (1 << nrOfSelectBits) - 1; i++)
        contents.add(
            "            WHEN "
                + IntToBin(i, nrOfSelectBits)
                + " => MuxOut <= MuxIn_"
                + i
                + ";");
      contents.add(
          "            WHEN OTHERS  => MuxOut <= MuxIn_"
              + ((1 << nrOfSelectBits) - 1)
              + ";");
      contents.add("         END CASE;");
      contents.add("      END IF;");
      contents.add("   END PROCESS make_mux;");
    } else {
      contents.add("   assign MuxOut = s_selected_vector;");
      contents.add("");
      contents.add("   always @(*)");
      contents.add("   begin");
      contents.add("      if (~Enable) s_selected_vector <= 0;");
      contents.add("      else case (Sel)");
      for (var i = 0; i < (1 << nrOfSelectBits) - 1; i++) {
        contents.add("         " + IntToBin(i, nrOfSelectBits) + ":");
        contents.add("            s_selected_vector <= MuxIn_" + i + ";");
      }
      contents.add("         default:");
      contents.add(
          "            s_selected_vector <= MuxIn_"
              + ((1 << nrOfSelectBits) - 1)
              + ";");
      contents.add("      endcase");
      contents.add("   end");
    }
    return contents;
  }

  @Override
  public SortedMap<String, Integer> GetOutputList(Netlist nets, AttributeSet attrs) {
    final var map = new TreeMap<String, Integer>();
    int NrOfBits = (attrs.getValue(StdAttr.WIDTH).getWidth() == 1) ? 1 : NrOfBitsId;
    map.put("MuxOut", NrOfBits);
    return map;
  }

  @Override
  public SortedMap<Integer, String> GetParameterList(AttributeSet attrs) {
    final var map = new TreeMap<Integer, String>();
    final var nrOfBits = attrs.getValue(StdAttr.WIDTH).getWidth();
    if (nrOfBits > 1) map.put(NrOfBitsId, NrOfBitsStr);
    return map;
  }

  @Override
  public SortedMap<String, Integer> GetParameterMap(Netlist nets, NetlistComponent componentInfo) {
    final var map = new TreeMap<String, Integer>();
    final var nrOfBits =
        componentInfo.GetComponent().getAttributeSet().getValue(StdAttr.WIDTH).getWidth();
    if (nrOfBits > 1) map.put(NrOfBitsStr, nrOfBits);
    return map;
  }

  @Override
  public SortedMap<String, String> GetPortMap(Netlist nets, Object mapInfo) {
    final var map = new TreeMap<String, String>();
    if (!(mapInfo instanceof NetlistComponent)) return map;
    final var comp = (NetlistComponent) mapInfo;
    final var nrOfSelectBits = comp.GetComponent().getAttributeSet().getValue(Plexers.ATTR_SELECT).getWidth();
    var selectInputIndex = (1 << nrOfSelectBits);
    // begin with connecting all inputs of multiplexer
    for (var i = 0; i < selectInputIndex; i++)
      map.putAll(GetNetMap("MuxIn_" + i, true, comp, i, nets));
    // now select..
    map.putAll(GetNetMap("Sel", true, comp, selectInputIndex, nets));
    // now connect enable input...
    if (comp.GetComponent()
        .getAttributeSet()
        .getValue(Plexers.ATTR_ENABLE)) {
      map.putAll(
          GetNetMap(
              "Enable", false, comp, selectInputIndex + 1, nets));
    } else {
      map.put("Enable", HDL.oneBit());
      selectInputIndex--; // decrement pin index because enable doesn't exist...
    }
    // finally output
    map.putAll(GetNetMap("MuxOut", true, comp, selectInputIndex + 2, nets));
    return map;
  }

  @Override
  public SortedMap<String, Integer> GetRegList(AttributeSet attrs) {
    final var map = new TreeMap<String, Integer>();
    final var nrOfBits = (attrs.getValue(StdAttr.WIDTH).getWidth() == 1) ? 1 : NrOfBitsId;
    if (HDL.isVerilog()) map.put("s_selected_vector", nrOfBits);
    return map;
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
