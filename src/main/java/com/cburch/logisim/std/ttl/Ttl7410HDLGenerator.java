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
    final var contents = new ArrayList<String>();
    final var Inv = Inverted ? HDL.notOperator() : "";
    final var Func = andgate ? HDL.andOperator() : HDL.orOperator();
    contents.add("   " + HDL.assignPreamble() + "Y0" + HDL.assignOperator() + Inv + " (A0 " + Func + " B0 " + Func + " C0);");
    contents.add("   " + HDL.assignPreamble() + "Y1" + HDL.assignOperator() + Inv + " (A1 " + Func + " B1 " + Func + " C1);");
    contents.add("   " + HDL.assignPreamble() + "Y2" + HDL.assignOperator() + Inv + " (A2 " + Func + " B2 " + Func + " C2);");
    return contents;
  }

  @Override
  public SortedMap<String, String> GetPortMap(Netlist Nets, Object MapInfo) {
    SortedMap<String, String> portMap = new TreeMap<>();
    if (!(MapInfo instanceof NetlistComponent)) return portMap;
    final var componentInfo = (NetlistComponent) MapInfo;
    portMap.putAll(GetNetMap("A0", true, componentInfo, 0, Nets));
    portMap.putAll(GetNetMap("B0", true, componentInfo, 1, Nets));
    portMap.putAll(GetNetMap("C0", true, componentInfo, 11, Nets));
    portMap.putAll(GetNetMap("Y0", true, componentInfo, 10, Nets));
    portMap.putAll(GetNetMap("A1", true, componentInfo, 2, Nets));
    portMap.putAll(GetNetMap("B1", true, componentInfo, 3, Nets));
    portMap.putAll(GetNetMap("C1", true, componentInfo, 4, Nets));
    portMap.putAll(GetNetMap("Y1", true, componentInfo, 5, Nets));
    portMap.putAll(GetNetMap("A2", true, componentInfo, 9, Nets));
    portMap.putAll(GetNetMap("B2", true, componentInfo, 8, Nets));
    portMap.putAll(GetNetMap("C2", true, componentInfo, 7, Nets));
    portMap.putAll(GetNetMap("Y2", true, componentInfo, 6, Nets));
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
