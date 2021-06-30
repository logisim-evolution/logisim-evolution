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

package com.cburch.logisim.std.ttl;

import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.fpga.designrulecheck.Netlist;
import com.cburch.logisim.fpga.designrulecheck.NetlistComponent;
import com.cburch.logisim.fpga.hdlgenerator.AbstractHDLGeneratorFactory;
import com.cburch.logisim.fpga.hdlgenerator.HDL;
import java.util.ArrayList;
import java.util.SortedMap;
import java.util.TreeMap;

public class Ttl7485HDLGenerator extends AbstractHDLGeneratorFactory {

  @Override
  public String getComponentStringIdentifier() {
    return "TTL7485";
  }

  @Override
  public SortedMap<String, Integer> GetInputList(Netlist TheNetlist, AttributeSet attrs) {
    final var map = new TreeMap<String, Integer>();
    map.put("A0", 1);
    map.put("A1", 1);
    map.put("A2", 1);
    map.put("A3", 1);
    map.put("B0", 1);
    map.put("B1", 1);
    map.put("B2", 1);
    map.put("B3", 1);
    map.put("AltBin", 1);
    map.put("AeqBin", 1);
    map.put("AgtBin", 1);
    return map;
  }

  @Override
  public SortedMap<String, Integer> GetOutputList(Netlist TheNetlist, AttributeSet attrs) {
    final var map = new TreeMap<String, Integer>();
    map.put("AltBout", 1);
    map.put("AeqBout", 1);
    map.put("AgtBout", 1);
    return map;
  }

  @Override
  public SortedMap<String, Integer> GetWireList(AttributeSet attrs, Netlist Nets) {
    final var map = new TreeMap<String, Integer>();
    map.put("oppA", 4);
    map.put("oppB", 4);
    map.put("gt", 1);
    map.put("eq", 1);
    map.put("lt", 1);
    map.put("CompIn", 3);
    map.put("CompOut", 3);
    return map;
  }

  @Override
  public ArrayList<String> GetModuleFunctionality(Netlist TheNetlist, AttributeSet attrs) {
    final var contents = new ArrayList<String>();
    contents.add("   oppA   <= A3&A2&A1&A0;");
    contents.add("   oppB   <= B3&B2&B1&B0;");
    contents.add("   gt     <= '1' WHEN unsigned(oppA) > unsigned(oppB) ELSE '0';");
    contents.add("   eq     <= '1' WHEN unsigned(oppA) = unsigned(oppB) ELSE '0';");
    contents.add("   lt     <= '1' WHEN unsigned(oppA) < unsigned(oppB) ELSE '0';");
    contents.add(" ");
    contents.add("   CompIn <= AgtBin&AltBin&AeqBin;");
    contents.add("   WITH (CompIn) SELECT CompOut <= ");
    contents.add("      \"100\" WHEN \"100\",");
    contents.add("      \"010\" WHEN \"010\",");
    contents.add("      \"000\" WHEN \"110\",");
    contents.add("      \"110\" WHEN \"000\",");
    contents.add("      \"001\" WHEN OTHERS;");
    contents.add(" ");
    contents.add("   AgtBout <= '1' WHEN gt = '1' ELSE '0' WHEN lt = '1' ELSE CompOut(2);");
    contents.add("   AltBout <= '0' WHEN gt = '1' ELSE '1' WHEN lt = '1' ELSE CompOut(1);");
    contents.add("   AeqBout <= '0' WHEN (gt = '1') OR (lt = '1') ELSE CompOut(0);");
    return contents;
  }

  @Override
  public SortedMap<String, String> GetPortMap(Netlist Nets, Object MapInfo) {
    final var map = new TreeMap<String, String>();
    if (!(MapInfo instanceof NetlistComponent)) return map;
    final var ComponentInfo = (NetlistComponent) MapInfo;
    map.putAll(GetNetMap("A0", true, ComponentInfo, 8, Nets));
    map.putAll(GetNetMap("A1", true, ComponentInfo, 10, Nets));
    map.putAll(GetNetMap("A2", true, ComponentInfo, 11, Nets));
    map.putAll(GetNetMap("A3", true, ComponentInfo, 13, Nets));
    map.putAll(GetNetMap("B0", true, ComponentInfo, 7, Nets));
    map.putAll(GetNetMap("B1", true, ComponentInfo, 9, Nets));
    map.putAll(GetNetMap("B2", true, ComponentInfo, 12, Nets));
    map.putAll(GetNetMap("B3", true, ComponentInfo, 0, Nets));
    map.putAll(GetNetMap("AltBin", true, ComponentInfo, 1, Nets));
    map.putAll(GetNetMap("AeqBin", true, ComponentInfo, 2, Nets));
    map.putAll(GetNetMap("AgtBin", true, ComponentInfo, 3, Nets));
    map.putAll(GetNetMap("AltBout", true, ComponentInfo, 6, Nets));
    map.putAll(GetNetMap("AeqBout", true, ComponentInfo, 5, Nets));
    map.putAll(GetNetMap("AgtBout", true, ComponentInfo, 4, Nets));
    return map;
  }

  @Override
  public String GetSubDir() {
    /*
     * this method returns the module directory where the HDL code needs to
     * be placed
     */
    return "ttl";
  }

  @Override
  public boolean HDLTargetSupported(AttributeSet attrs) {
    /* TODO: Add support for the ones with VCC and Ground Pin */
    if (attrs == null) return false;
    return (!attrs.getValue(TtlLibrary.VCC_GND) && HDL.isVHDL());
  }
}
