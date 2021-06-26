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
import java.util.ArrayList;
import java.util.SortedMap;
import java.util.TreeMap;

public class AbstractGateHDLGenerator extends AbstractHDLGeneratorFactory {

  public boolean IsInverter() {
    return false;
  }

  @Override
  public String getComponentStringIdentifier() {
    return "TTL";
  }

  @Override
  public SortedMap<String, Integer> GetInputList(Netlist TheNetlist, AttributeSet attrs) {
    SortedMap<String, Integer> MyInputs = new TreeMap<>();
    final var NrOfGates = (IsInverter()) ? 6 : 4;
    for (var i = 0; i < NrOfGates; i++) {
      MyInputs.put("gate_" + i + "_A", 1);
      if (!IsInverter()) MyInputs.put("gate_" + i + "_B", 1);
    }
    return MyInputs;
  }

  @Override
  public SortedMap<String, Integer> GetOutputList(Netlist TheNetlist, AttributeSet attrs) {
    SortedMap<String, Integer> MyOutputs = new TreeMap<>();
    final var NrOfGates = (IsInverter()) ? 6 : 4;
    for (var i = 0; i < NrOfGates; i++) {
      MyOutputs.put("gate_" + i + "_O", 1);
    }
    return MyOutputs;
  }

  public ArrayList<String> GetLogicFunction(int index) {
    return new ArrayList<>();
  }

  @Override
  public ArrayList<String> GetModuleFunctionality(Netlist TheNetlist, AttributeSet attrs) {
    final var Contents = new ArrayList<String>();
    final var NrOfGates = (IsInverter()) ? 6 : 4;
    for (var i = 0; i < NrOfGates; i++) {
      Contents.addAll(MakeRemarkBlock("Here gate " + i + " is described", 3));
      Contents.addAll(GetLogicFunction(i));
    }
    return Contents;
  }

  @Override
  public SortedMap<String, String> GetPortMap(Netlist Nets, Object MapInfo) {
    SortedMap<String, String> PortMap = new TreeMap<>();
    if (!(MapInfo instanceof NetlistComponent)) return PortMap;
    final var ComponentInfo = (NetlistComponent) MapInfo;
    final var NrOfGates = (IsInverter()) ? 6 : 4;
    for (var i = 0; i < NrOfGates; i++) {
      if (IsInverter()) {
        final var inindex = (i < 3) ? i * 2 : i * 2 + 1;
        final var outindex = (i < 3) ? i * 2 + 1 : i * 2;
        PortMap.putAll(GetNetMap("gate_" + i + "_A", true, ComponentInfo, inindex, Nets));
        PortMap.putAll(GetNetMap("gate_" + i + "_O", true, ComponentInfo, outindex, Nets));
      } else {
        final var inindex1 = (i < 2) ? i * 3 : i * 3 + 1;
        final var inindex2 = inindex1 + 1;
        final var outindex = (i < 2) ? i * 3 + 2 : i * 3;
        PortMap.putAll(GetNetMap("gate_" + i + "_A", true, ComponentInfo, inindex1, Nets));
        PortMap.putAll(GetNetMap("gate_" + i + "_B", true, ComponentInfo, inindex2, Nets));
        PortMap.putAll(GetNetMap("gate_" + i + "_O", true, ComponentInfo, outindex, Nets));
      }
    }
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
    return !attrs.getValue(TTL.VCC_GND);
  }
}
