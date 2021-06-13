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
import com.cburch.logisim.fpga.hdlgenerator.HDLGeneratorFactory;
import java.util.ArrayList;
import java.util.SortedMap;
import java.util.TreeMap;

public class Ttl7410HDLGenerator extends AbstractHDLGeneratorFactory {

  private boolean Inverted = true;
  private boolean andgate = true;

  public Ttl7410HDLGenerator() {
    super();
  }

  public Ttl7410HDLGenerator(boolean invert, boolean IsAnd) {
    super();
    Inverted = invert;
    andgate = IsAnd;
  }

  @Override
  public String getComponentStringIdentifier() {
    return "TTL";
  }

  @Override
  public SortedMap<String, Integer> GetInputList(Netlist TheNetlist, AttributeSet attrs) {
    SortedMap<String, Integer> MyInputs = new TreeMap<>();
    MyInputs.put("A0", 1);
    MyInputs.put("B0", 1);
    MyInputs.put("C0", 1);
    MyInputs.put("A1", 1);
    MyInputs.put("B1", 1);
    MyInputs.put("C1", 1);
    MyInputs.put("A2", 1);
    MyInputs.put("B2", 1);
    MyInputs.put("C2", 1);
    return MyInputs;
  }

  @Override
  public SortedMap<String, Integer> GetOutputList(Netlist TheNetlist, AttributeSet attrs) {
    SortedMap<String, Integer> MyOutputs = new TreeMap<>();
    MyOutputs.put("Y0", 1);
    MyOutputs.put("Y1", 1);
    MyOutputs.put("Y2", 1);
    return MyOutputs;
  }

  @Override
  public ArrayList<String> GetModuleFunctionality(Netlist TheNetlist, AttributeSet attrs) {
    ArrayList<String> Contents = new ArrayList<>();
    String Inv = Inverted ? HDL.notOperator() : "";
    String Func = andgate ? HDL.andOperator() : HDL.orOperator();
    Contents.add("   "+HDL.assignPreamble()+"Y0"+HDL.assignOperator()+ Inv + " (A0 " + Func + " B0 " + Func + " C0);");
    Contents.add("   "+HDL.assignPreamble()+"Y1"+HDL.assignOperator()+ Inv + " (A1 " + Func + " B1 " + Func + " C1);");
    Contents.add("   "+HDL.assignPreamble()+"Y2"+HDL.assignOperator()+ Inv + " (A2 " + Func + " B2 " + Func + " C2);");
    return Contents;
  }

  @Override
  public SortedMap<String, String> GetPortMap(Netlist Nets, Object MapInfo) {
    SortedMap<String, String> PortMap = new TreeMap<>();
    if (!(MapInfo instanceof NetlistComponent)) return PortMap;
    NetlistComponent ComponentInfo = (NetlistComponent) MapInfo;
    PortMap.putAll(GetNetMap("A0", true, ComponentInfo, 0, Nets));
    PortMap.putAll(GetNetMap("B0", true, ComponentInfo, 1, Nets));
    PortMap.putAll(GetNetMap("C0", true, ComponentInfo, 11, Nets));
    PortMap.putAll(GetNetMap("Y0", true, ComponentInfo, 10, Nets));
    PortMap.putAll(GetNetMap("A1", true, ComponentInfo, 2, Nets));
    PortMap.putAll(GetNetMap("B1", true, ComponentInfo, 3, Nets));
    PortMap.putAll(GetNetMap("C1", true, ComponentInfo, 4, Nets));
    PortMap.putAll(GetNetMap("Y1", true, ComponentInfo, 5, Nets));
    PortMap.putAll(GetNetMap("A2", true, ComponentInfo, 9, Nets));
    PortMap.putAll(GetNetMap("B2", true, ComponentInfo, 8, Nets));
    PortMap.putAll(GetNetMap("C2", true, ComponentInfo, 7, Nets));
    PortMap.putAll(GetNetMap("Y2", true, ComponentInfo, 6, Nets));
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
