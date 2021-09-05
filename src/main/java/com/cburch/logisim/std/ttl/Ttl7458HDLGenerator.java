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

public class Ttl7458HDLGenerator extends AbstractHDLGeneratorFactory {

  @Override
  public String getComponentStringIdentifier() {
    return "TTL";
  }

  @Override
  public SortedMap<String, Integer> GetInputList(Netlist TheNetlist, AttributeSet attrs) {
    final var map = new TreeMap<String, Integer>();
    map.put("A0", 1);
    map.put("B0", 1);
    map.put("C0", 1);
    map.put("D0", 1);
    map.put("A1", 1);
    map.put("B1", 1);
    map.put("C1", 1);
    map.put("D1", 1);
    map.put("E1", 1);
    map.put("F1", 1);
    return map;
  }

  @Override
  public SortedMap<String, Integer> GetOutputList(Netlist TheNetlist, AttributeSet attrs) {
    final var map = new TreeMap<String, Integer>();
    map.put("Y0", 1);
    map.put("Y1", 1);
    return map;
  }

  @Override
  public ArrayList<String> GetModuleFunctionality(Netlist TheNetlist, AttributeSet attrs) {
    final var contents = new ArrayList<String>();
    contents.add("   " + HDL.assignPreamble() + "Y0" + HDL.assignOperator() + "(A0" + HDL.andOperator() + "B0)"
            + HDL.orOperator() + "(C0" + HDL.andOperator() + "D0);");
    contents.add("   " + HDL.assignPreamble() + "Y1" + HDL.assignOperator() + "(A1" + HDL.andOperator() + "B1"
            + HDL.andOperator() + "C1)" + HDL.orOperator() + "(D1" + HDL.andOperator() + "E1" + HDL.andOperator() + "F1);");
    return contents;
  }

  @Override
  public SortedMap<String, String> GetPortMap(Netlist nets, Object mapInfo) {
    final var map = new TreeMap<String, String>();
    if (!(mapInfo instanceof NetlistComponent)) return map;
    NetlistComponent ComponentInfo = (NetlistComponent) mapInfo;
    map.putAll(GetNetMap("A0", true, ComponentInfo, 1, nets));
    map.putAll(GetNetMap("B0", true, ComponentInfo, 2, nets));
    map.putAll(GetNetMap("C0", true, ComponentInfo, 3, nets));
    map.putAll(GetNetMap("D0", true, ComponentInfo, 4, nets));
    map.putAll(GetNetMap("Y0", true, ComponentInfo, 5, nets));
    map.putAll(GetNetMap("A1", true, ComponentInfo, 0, nets));
    map.putAll(GetNetMap("B1", true, ComponentInfo, 11, nets));
    map.putAll(GetNetMap("C1", true, ComponentInfo, 10, nets));
    map.putAll(GetNetMap("D1", true, ComponentInfo, 9, nets));
    map.putAll(GetNetMap("E1", true, ComponentInfo, 8, nets));
    map.putAll(GetNetMap("F1", true, ComponentInfo, 7, nets));
    map.putAll(GetNetMap("Y1", true, ComponentInfo, 6, nets));
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
