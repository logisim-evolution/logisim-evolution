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

public class Ttl7464HDLGenerator extends AbstractHDLGeneratorFactory {

  @Override
  public String getComponentStringIdentifier() {
    return "TTL";
  }

  @Override
  public SortedMap<String, Integer> GetInputList(Netlist TheNetlist, AttributeSet attrs) {
    final var map = new TreeMap<String, Integer>();
    map.put("A", 1);
    map.put("B", 1);
    map.put("C", 1);
    map.put("D", 1);
    map.put("E", 1);
    map.put("F", 1);
    map.put("G", 1);
    map.put("H", 1);
    map.put("I", 1);
    map.put("J", 1);
    map.put("K", 1);
    return map;
  }

  @Override
  public SortedMap<String, Integer> GetOutputList(Netlist TheNetlist, AttributeSet attrs) {
    final var map = new TreeMap<String, Integer>();
    map.put("Y", 1);
    return map;
  }

  @Override
  public ArrayList<String> GetModuleFunctionality(Netlist nets, AttributeSet attrs) {
    final var contents = new ArrayList<String>();
    contents.add("   " + HDL.assignPreamble() + "Y" + HDL.assignOperator() + HDL.notOperator() + "((A"
            + HDL.andOperator() + "B" + HDL.andOperator() + "C" + HDL.andOperator() + "D)" + HDL.orOperator()
            + "(E" + HDL.andOperator() + "F)" + HDL.orOperator() + "(G" + HDL.andOperator()
            + "H" + HDL.andOperator() + "I)" + HDL.orOperator() + "(J" + HDL.andOperator() + "K));");
    return contents;
  }

  @Override
  public SortedMap<String, String> GetPortMap(Netlist nets, Object mapInfo) {
    final var map = new TreeMap<String, String>();
    if (!(mapInfo instanceof NetlistComponent)) return map;
    final var netlistComponent = (NetlistComponent) mapInfo;
    map.putAll(GetNetMap("A", true, netlistComponent, 0, nets));
    map.putAll(GetNetMap("B", true, netlistComponent, 11, nets));
    map.putAll(GetNetMap("C", true, netlistComponent, 10, nets));
    map.putAll(GetNetMap("D", true, netlistComponent, 9, nets));
    map.putAll(GetNetMap("E", true, netlistComponent, 1, nets));
    map.putAll(GetNetMap("F", true, netlistComponent, 2, nets));
    map.putAll(GetNetMap("G", true, netlistComponent, 3, nets));
    map.putAll(GetNetMap("H", true, netlistComponent, 4, nets));
    map.putAll(GetNetMap("I", true, netlistComponent, 5, nets));
    map.putAll(GetNetMap("J", true, netlistComponent, 7, nets));
    map.putAll(GetNetMap("K", true, netlistComponent, 8, nets));
    map.putAll(GetNetMap("Y", true, netlistComponent, 6, nets));
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
    return (!attrs.getValue(TtlLibrary.VCC_GND));
  }
}
