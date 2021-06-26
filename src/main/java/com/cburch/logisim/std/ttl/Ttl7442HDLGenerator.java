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
    SortedMap<String, Integer> MyInputs = new TreeMap<>();
    MyInputs.put("A", 1);
    MyInputs.put("B", 1);
    MyInputs.put("C", 1);
    MyInputs.put("D", 1);
    return MyInputs;
  }

  @Override
  public SortedMap<String, Integer> GetOutputList(Netlist TheNetlist, AttributeSet attrs) {
    SortedMap<String, Integer> MyOutputs = new TreeMap<>();
    MyOutputs.put("O0", 1);
    MyOutputs.put("O1", 1);
    MyOutputs.put("O2", 1);
    MyOutputs.put("O3", 1);
    MyOutputs.put("O4", 1);
    MyOutputs.put("O5", 1);
    MyOutputs.put("O6", 1);
    MyOutputs.put("O7", 1);
    MyOutputs.put("O8", 1);
    MyOutputs.put("O9", 1);
    return MyOutputs;
  }

  @Override
  public ArrayList<String> GetModuleFunctionality(Netlist TheNetlist, AttributeSet attrs) {
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
  public SortedMap<String, String> GetPortMap(Netlist Nets, Object MapInfo) {
    SortedMap<String, String> PortMap = new TreeMap<>();
    if (!(MapInfo instanceof NetlistComponent)) return PortMap;
    NetlistComponent ComponentInfo = (NetlistComponent) MapInfo;
    PortMap.putAll(GetNetMap("A", true, ComponentInfo, 13, Nets));
    PortMap.putAll(GetNetMap("B", true, ComponentInfo, 12, Nets));
    PortMap.putAll(GetNetMap("C", true, ComponentInfo, 11, Nets));
    PortMap.putAll(GetNetMap("D", true, ComponentInfo, 10, Nets));
    PortMap.putAll(GetNetMap("O0", true, ComponentInfo, 0, Nets));
    PortMap.putAll(GetNetMap("O1", true, ComponentInfo, 1, Nets));
    PortMap.putAll(GetNetMap("O2", true, ComponentInfo, 2, Nets));
    PortMap.putAll(GetNetMap("O3", true, ComponentInfo, 3, Nets));
    PortMap.putAll(GetNetMap("O4", true, ComponentInfo, 4, Nets));
    PortMap.putAll(GetNetMap("O5", true, ComponentInfo, 5, Nets));
    PortMap.putAll(GetNetMap("O6", true, ComponentInfo, 6, Nets));
    PortMap.putAll(GetNetMap("O7", true, ComponentInfo, 7, Nets));
    PortMap.putAll(GetNetMap("O8", true, ComponentInfo, 8, Nets));
    PortMap.putAll(GetNetMap("O9", true, ComponentInfo, 9, Nets));
    return PortMap;
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
    return (!attrs.getValue(TTL.VCC_GND));
  }
}
