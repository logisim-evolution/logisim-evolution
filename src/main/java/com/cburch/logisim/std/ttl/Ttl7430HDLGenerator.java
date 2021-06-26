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

public class Ttl7430HDLGenerator extends AbstractHDLGeneratorFactory {

  @Override
  public String getComponentStringIdentifier() {
    return "TTL";
  }

  @Override
  public SortedMap<String, Integer> GetInputList(Netlist TheNetlist, AttributeSet attrs) {
    SortedMap<String, Integer> myInputs = new TreeMap<>();
    myInputs.put("A", 1);
    myInputs.put("B", 1);
    myInputs.put("C", 1);
    myInputs.put("D", 1);
    myInputs.put("E", 1);
    myInputs.put("F", 1);
    myInputs.put("G", 1);
    myInputs.put("H", 1);
    return myInputs;
  }

  @Override
  public SortedMap<String, Integer> GetOutputList(Netlist TheNetlist, AttributeSet attrs) {
    SortedMap<String, Integer> myOutputs = new TreeMap<>();
    myOutputs.put("Y", 1);
    return myOutputs;
  }

  @Override
  public ArrayList<String> GetModuleFunctionality(Netlist TheNetlist, AttributeSet attrs) {
    final var contents = new ArrayList<String>();
    contents.add("   " + HDL.assignPreamble() + "Y1" + HDL.assignOperator() + HDL.notOperator() + "(A"
            + HDL.andOperator() + "B" + HDL.andOperator() + "C" + HDL.andOperator() + "D" + HDL.andOperator() + "E"
            + HDL.andOperator() + "F" + HDL.andOperator() + "G" + HDL.andOperator() + "H);");
    return contents;
  }

  @Override
  public SortedMap<String, String> GetPortMap(Netlist Nets, Object MapInfo) {
    SortedMap<String, String> portMap = new TreeMap<>();
    if (!(MapInfo instanceof NetlistComponent)) return portMap;
    final var ComponentInfo = (NetlistComponent) MapInfo;
    portMap.putAll(GetNetMap("A", true, ComponentInfo, 0, Nets));
    portMap.putAll(GetNetMap("B", true, ComponentInfo, 1, Nets));
    portMap.putAll(GetNetMap("C", true, ComponentInfo, 2, Nets));
    portMap.putAll(GetNetMap("D", true, ComponentInfo, 3, Nets));
    portMap.putAll(GetNetMap("E", true, ComponentInfo, 4, Nets));
    portMap.putAll(GetNetMap("F", true, ComponentInfo, 5, Nets));
    portMap.putAll(GetNetMap("G", true, ComponentInfo, 7, Nets));
    portMap.putAll(GetNetMap("H", true, ComponentInfo, 8, Nets));
    portMap.putAll(GetNetMap("Y", true, ComponentInfo, 6, Nets));
    return portMap;
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
