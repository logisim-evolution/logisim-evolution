/*
 * This file is part of logisim-evolution.
 *
 * Logisim-evolution is free software: you can redistribute it "+AND+"/or modify
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
 * with logisim-evolution. If "+NOT+", see <http://www.gnu.org/licenses/>.
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

public class Ttl7442HDLGenerator extends AbstractHDLGeneratorFactory {

  private boolean IsExes3 = false;
  private boolean IsGray = false;

  public Ttl7442HDLGenerator() {
    super();
  }

  public Ttl7442HDLGenerator(boolean Exess3, boolean Gray) {
    super();
    IsExes3 = Exess3;
    IsGray = Gray;
  }

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
    return map;
  }

  @Override
  public SortedMap<String, Integer> GetOutputList(Netlist TheNetlist, AttributeSet attrs) {
    final var map = new TreeMap<String, Integer>();
    map.put("O0", 1);
    map.put("O1", 1);
    map.put("O2", 1);
    map.put("O3", 1);
    map.put("O4", 1);
    map.put("O5", 1);
    map.put("O6", 1);
    map.put("O7", 1);
    map.put("O8", 1);
    map.put("O9", 1);
    return map;
  }

  @Override
  public ArrayList<String> GetModuleFunctionality(Netlist nets, AttributeSet attrs) {
    final var contents = new ArrayList<String>();
    final var NOT = HDL.notOperator();
    final var AND = HDL.andOperator();
    if (IsExes3) {
      contents.add("   " + HDL.assignPreamble() + "O0" + HDL.assignOperator() + NOT + "(" + NOT + "(D)" + AND + NOT + "(C)" + AND + "B" + AND + "A);");
      contents.add("   " + HDL.assignPreamble() + "O1" + HDL.assignOperator() + NOT + "(" + NOT + "(D)" + AND + "C" + AND + NOT + "(B)" + AND + NOT + "(A));");
      contents.add("   " + HDL.assignPreamble() + "O2" + HDL.assignOperator() + NOT + "(" + NOT + "(D)" + AND + "C" + AND + NOT + "(B)" + AND + "A);");
      contents.add("   " + HDL.assignPreamble() + "O3" + HDL.assignOperator() + NOT + "(" + NOT + "(D)" + AND + "C" + AND + "B" + AND + NOT + "(A));");
      contents.add("   " + HDL.assignPreamble() + "O4" + HDL.assignOperator() + NOT + "(" + NOT + "(D)" + AND + "C" + AND + "B" + AND + "A);");
      contents.add("   " + HDL.assignPreamble() + "O5" + HDL.assignOperator() + NOT + "(D" + AND + NOT + "(C)" + AND + NOT + "(B)" + AND + NOT + "(A));");
      contents.add("   " + HDL.assignPreamble() + "O6" + HDL.assignOperator() + NOT + "(D" + AND + NOT + "(C)" + AND + NOT + "(B)" + AND + "A);");
      contents.add("   " + HDL.assignPreamble() + "O7" + HDL.assignOperator() + NOT + "(D" + AND + NOT + "(C)" + AND + "B" + AND + NOT + "(A));");
      contents.add("   " + HDL.assignPreamble() + "O8" + HDL.assignOperator() + NOT + "(D" + AND + NOT + "(C)" + AND + "B" + AND + "A);");
      contents.add("   " + HDL.assignPreamble() + "O9" + HDL.assignOperator() + NOT + "(D" + AND + "C" + AND + NOT + "(B)" + AND + NOT + "(A));");
    } else if (IsGray) {
      contents.add("   " + HDL.assignPreamble() + "O0" + HDL.assignOperator() + NOT + "(" + NOT + "(D)" + AND + NOT + "(C)" + AND + "B" + AND + NOT + "(A));");
      contents.add("   " + HDL.assignPreamble() + "O1" + HDL.assignOperator() + NOT + "(" + NOT + "(D)" + AND + "C" + AND + "B" + AND + NOT + "(A));");
      contents.add("   " + HDL.assignPreamble() + "O2" + HDL.assignOperator() + NOT + "(" + NOT + "(D)" + AND + "C" + AND + "B" + AND + "A);");
      contents.add("   " + HDL.assignPreamble() + "O3" + HDL.assignOperator() + NOT + "(" + NOT + "(D)" + AND + "C" + AND + NOT + "(B)" + AND + "A);");
      contents.add("   " + HDL.assignPreamble() + "O4" + HDL.assignOperator() + NOT + "(" + NOT + "(D)" + AND + "C" + AND + NOT + "(B)" + AND + NOT + "(A));");
      contents.add("   " + HDL.assignPreamble() + "O5" + HDL.assignOperator() + NOT + "(D" + AND + "C" + AND + NOT + "(B)" + AND + NOT + "(A));");
      contents.add("   " + HDL.assignPreamble() + "O6" + HDL.assignOperator() + NOT + "(D" + AND + "C" + AND + NOT + "(B)" + AND + "A);");
      contents.add("   " + HDL.assignPreamble() + "O7" + HDL.assignOperator() + NOT + "(D" + AND + "C" + AND + "B" + AND + "A);");
      contents.add("   " + HDL.assignPreamble() + "O8" + HDL.assignOperator() + NOT + "(D" + AND + "C" + AND + "B" + AND + NOT + "(A));");
      contents.add("   " + HDL.assignPreamble() + "O9" + HDL.assignOperator() + NOT + "(D" + AND + NOT + "(C)" + AND + "B" + AND + NOT + "(A));");
    } else {
      contents.add("   " + HDL.assignPreamble() + "O0" + HDL.assignOperator() + NOT + "(" + NOT + "(D)" + AND + NOT + "(C)" + AND + NOT + "(B)" + AND + NOT + "(A));");
      contents.add("   " + HDL.assignPreamble() + "O1" + HDL.assignOperator() + NOT + "(" + NOT + "(D)" + AND + NOT + "(C)" + AND + NOT + "(B)" + AND + "A);");
      contents.add("   " + HDL.assignPreamble() + "O2" + HDL.assignOperator() + NOT + "(" + NOT + "(D)" + AND + NOT + "(C)" + AND + "B" + AND + NOT + "(A));");
      contents.add("   " + HDL.assignPreamble() + "O3" + HDL.assignOperator() + NOT + "(" + NOT + "(D)" + AND + NOT + "(C)" + AND + "B" + AND + "A);");
      contents.add("   " + HDL.assignPreamble() + "O4" + HDL.assignOperator() + NOT + "(" + NOT + "(D)" + AND + "C" + AND + NOT + "(B)" + AND + NOT + "(A));");
      contents.add("   " + HDL.assignPreamble() + "O5" + HDL.assignOperator() + NOT + "(" + NOT + "(D)" + AND + "C" + AND + NOT + "(B)" + AND + "A);");
      contents.add("   " + HDL.assignPreamble() + "O6" + HDL.assignOperator() + NOT + "(" + NOT + "(D)" + AND + "C" + AND + "B" + AND + NOT + "(A));");
      contents.add("   " + HDL.assignPreamble() + "O7" + HDL.assignOperator() + NOT + "(" + NOT + "(D)" + AND + "C" + AND + "B" + AND + "A);");
      contents.add("   " + HDL.assignPreamble() + "O8" + HDL.assignOperator() + NOT + "(D" + AND + NOT + "(C)" + AND + NOT + "(B)" + AND + NOT + "(A));");
      contents.add("   " + HDL.assignPreamble() + "O9" + HDL.assignOperator() + NOT + "(D" + AND + NOT + "(C)" + AND + NOT + "(B)" + AND + "A);");
    }
    return contents;
  }

  @Override
  public SortedMap<String, String> GetPortMap(Netlist nets, Object mapInfo) {
    final var map = new TreeMap<String, String>();
    if (!(mapInfo instanceof NetlistComponent)) return map;
    final var comp = (NetlistComponent) mapInfo;
    map.putAll(GetNetMap("A", true, comp, 13, nets));
    map.putAll(GetNetMap("B", true, comp, 12, nets));
    map.putAll(GetNetMap("C", true, comp, 11, nets));
    map.putAll(GetNetMap("D", true, comp, 10, nets));
    map.putAll(GetNetMap("O0", true, comp, 0, nets));
    map.putAll(GetNetMap("O1", true, comp, 1, nets));
    map.putAll(GetNetMap("O2", true, comp, 2, nets));
    map.putAll(GetNetMap("O3", true, comp, 3, nets));
    map.putAll(GetNetMap("O4", true, comp, 4, nets));
    map.putAll(GetNetMap("O5", true, comp, 5, nets));
    map.putAll(GetNetMap("O6", true, comp, 6, nets));
    map.putAll(GetNetMap("O7", true, comp, 7, nets));
    map.putAll(GetNetMap("O8", true, comp, 8, nets));
    map.putAll(GetNetMap("O9", true, comp, 9, nets));
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
