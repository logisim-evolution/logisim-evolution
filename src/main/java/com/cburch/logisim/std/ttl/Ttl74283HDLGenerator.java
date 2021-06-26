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

public class Ttl74283HDLGenerator extends AbstractHDLGeneratorFactory {

  @Override
  public String getComponentStringIdentifier() {
    return "TTL74283";
  }

  @Override
  public SortedMap<String, Integer> GetInputList(Netlist TheNetlist, AttributeSet attrs) {
    SortedMap<String, Integer> myInputs = new TreeMap<>();
    myInputs.put("A1", 1);
    myInputs.put("A2", 1);
    myInputs.put("A3", 1);
    myInputs.put("A4", 1);
    myInputs.put("B1", 1);
    myInputs.put("B2", 1);
    myInputs.put("B3", 1);
    myInputs.put("B4", 1);
    myInputs.put("Cin", 1);
    return myInputs;
  }

  @Override
  public SortedMap<String, Integer> GetOutputList(Netlist TheNetlist, AttributeSet attrs) {
    SortedMap<String, Integer> myOutputs = new TreeMap<>();
    myOutputs.put("S1", 1);
    myOutputs.put("S2", 1);
    myOutputs.put("S3", 1);
    myOutputs.put("S4", 1);
    myOutputs.put("Cout", 1);
    return myOutputs;
  }

  @Override
  public SortedMap<String, Integer> GetWireList(AttributeSet attrs, Netlist Nets) {
    SortedMap<String, Integer> wires = new TreeMap<>();
    wires.put("oppA", 5);
    wires.put("oppB", 5);
    wires.put("oppC", 5);
    wires.put("Result", 5);
    return wires;
  }

  @Override
  public ArrayList<String> GetModuleFunctionality(Netlist TheNetlist, AttributeSet attrs) {
    final var contents = new ArrayList<String>();
    contents.add("   oppA   <= \"0\"&A4&A3&A2&A1;");
    contents.add("   oppB   <= \"0\"&B4&B3&B2&B1;");
    contents.add("   oppC   <= \"0000\"&Cin;");
    contents.add("   Result <= std_logic_vector(unsigned(oppA)+unsigned(oppB)+unsigned(oppC));");
    contents.add("   S1     <= Result(0);");
    contents.add("   S2     <= Result(1);");
    contents.add("   S3     <= Result(2);");
    contents.add("   S4     <= Result(3);");
    contents.add("   Cout   <= Result(4);");
    return contents;
  }

  @Override
  public SortedMap<String, String> GetPortMap(Netlist Nets, Object MapInfo) {
    SortedMap<String, String> PortMap = new TreeMap<>();
    if (!(MapInfo instanceof NetlistComponent)) return PortMap;
    final var componentInfo = (NetlistComponent) MapInfo;
    PortMap.putAll(GetNetMap("A1", true, componentInfo, 4, Nets));
    PortMap.putAll(GetNetMap("A2", true, componentInfo, 2, Nets));
    PortMap.putAll(GetNetMap("A3", true, componentInfo, 12, Nets));
    PortMap.putAll(GetNetMap("A4", true, componentInfo, 10, Nets));
    PortMap.putAll(GetNetMap("B1", true, componentInfo, 5, Nets));
    PortMap.putAll(GetNetMap("B2", true, componentInfo, 1, Nets));
    PortMap.putAll(GetNetMap("B3", true, componentInfo, 13, Nets));
    PortMap.putAll(GetNetMap("B4", true, componentInfo, 9, Nets));
    PortMap.putAll(GetNetMap("Cin", true, componentInfo, 6, Nets));
    PortMap.putAll(GetNetMap("S1", true, componentInfo, 3, Nets));
    PortMap.putAll(GetNetMap("S2", true, componentInfo, 0, Nets));
    PortMap.putAll(GetNetMap("S3", true, componentInfo, 11, Nets));
    PortMap.putAll(GetNetMap("S4", true, componentInfo, 8, Nets));
    PortMap.putAll(GetNetMap("Cout", true, componentInfo, 7, Nets));
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
    return (!attrs.getValue(TTL.VCC_GND) && HDL.isVHDL());
  }
}
