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
    SortedMap<String, Integer> MyInputs = new TreeMap<>();
    MyInputs.put("A1", 1);
    MyInputs.put("A2", 1);
    MyInputs.put("A3", 1);
    MyInputs.put("A4", 1);
    MyInputs.put("B1", 1);
    MyInputs.put("B2", 1);
    MyInputs.put("B3", 1);
    MyInputs.put("B4", 1);
    MyInputs.put("Cin", 1);
    return MyInputs;
  }

  @Override
  public SortedMap<String, Integer> GetOutputList(Netlist TheNetlist, AttributeSet attrs) {
    SortedMap<String, Integer> MyOutputs = new TreeMap<>();
    MyOutputs.put("S1", 1);
    MyOutputs.put("S2", 1);
    MyOutputs.put("S3", 1);
    MyOutputs.put("S4", 1);
    MyOutputs.put("Cout", 1);
    return MyOutputs;
  }

  @Override
  public SortedMap<String, Integer> GetWireList(AttributeSet attrs, Netlist Nets) {
    SortedMap<String, Integer> Wires = new TreeMap<>();
    Wires.put("oppA", 5);
    Wires.put("oppB", 5);
    Wires.put("oppC", 5);
    Wires.put("Result", 5);
    return Wires;
  }

  @Override
  public ArrayList<String> GetModuleFunctionality(Netlist TheNetlist, AttributeSet attrs) {
    ArrayList<String> Contents = new ArrayList<>();
    Contents.add("   oppA   <= \"0\"&A4&A3&A2&A1;");
    Contents.add("   oppB   <= \"0\"&B4&B3&B2&B1;");
    Contents.add("   oppC   <= \"0000\"&Cin;");
    Contents.add("   Result <= std_logic_vector(unsigned(oppA)+unsigned(oppB)+unsigned(oppC));");
    Contents.add("   S1     <= Result(0);");
    Contents.add("   S2     <= Result(1);");
    Contents.add("   S3     <= Result(2);");
    Contents.add("   S4     <= Result(3);");
    Contents.add("   Cout   <= Result(4);");
    return Contents;
  }

  @Override
  public SortedMap<String, String> GetPortMap(Netlist Nets, Object MapInfo) {
    SortedMap<String, String> PortMap = new TreeMap<>();
    if (!(MapInfo instanceof NetlistComponent)) return PortMap;
    NetlistComponent ComponentInfo = (NetlistComponent) MapInfo;
    PortMap.putAll(GetNetMap("A1", true, ComponentInfo, 4, Nets));
    PortMap.putAll(GetNetMap("A2", true, ComponentInfo, 2, Nets));
    PortMap.putAll(GetNetMap("A3", true, ComponentInfo, 12, Nets));
    PortMap.putAll(GetNetMap("A4", true, ComponentInfo, 10, Nets));
    PortMap.putAll(GetNetMap("B1", true, ComponentInfo, 5, Nets));
    PortMap.putAll(GetNetMap("B2", true, ComponentInfo, 1, Nets));
    PortMap.putAll(GetNetMap("B3", true, ComponentInfo, 13, Nets));
    PortMap.putAll(GetNetMap("B4", true, ComponentInfo, 9, Nets));
    PortMap.putAll(GetNetMap("Cin", true, ComponentInfo, 6, Nets));
    PortMap.putAll(GetNetMap("S1", true, ComponentInfo, 3, Nets));
    PortMap.putAll(GetNetMap("S2", true, ComponentInfo, 0, Nets));
    PortMap.putAll(GetNetMap("S3", true, ComponentInfo, 11, Nets));
    PortMap.putAll(GetNetMap("S4", true, ComponentInfo, 8, Nets));
    PortMap.putAll(GetNetMap("Cout", true, ComponentInfo, 7, Nets));
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
