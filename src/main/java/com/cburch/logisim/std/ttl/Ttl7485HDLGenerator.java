/**
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
import com.cburch.logisim.fpga.gui.FPGAReport;
import com.cburch.logisim.fpga.hdlgenerator.AbstractHDLGeneratorFactory;
import com.cburch.logisim.fpga.hdlgenerator.HDLGeneratorFactory;
import java.util.ArrayList;
import java.util.SortedMap;
import java.util.TreeMap;

public class Ttl7485HDLGenerator extends AbstractHDLGeneratorFactory {

  @Override
  public String getComponentStringIdentifier() {
    return "TTL7485";
  }

  @Override
  public SortedMap<String, Integer> GetInputList(Netlist TheNetlist, AttributeSet attrs) {
    SortedMap<String, Integer> MyInputs = new TreeMap<String, Integer>();
    MyInputs.put("A0", 1);
    MyInputs.put("A1", 1);
    MyInputs.put("A2", 1);
    MyInputs.put("A3", 1);
    MyInputs.put("B0", 1);
    MyInputs.put("B1", 1);
    MyInputs.put("B2", 1);
    MyInputs.put("B3", 1);
    MyInputs.put("AltBin", 1);
    MyInputs.put("AeqBin", 1);
    MyInputs.put("AgtBin", 1);
    return MyInputs;
  }

  @Override
  public SortedMap<String, Integer> GetOutputList(Netlist TheNetlist, AttributeSet attrs) {
    SortedMap<String, Integer> MyOutputs = new TreeMap<String, Integer>();
    MyOutputs.put("AltBout", 1);
    MyOutputs.put("AeqBout", 1);
    MyOutputs.put("AgtBout", 1);
    return MyOutputs;
  }

  @Override
  public SortedMap<String, Integer> GetWireList(AttributeSet attrs, Netlist Nets) {
    SortedMap<String, Integer> Wires = new TreeMap<String, Integer>();
    Wires.put("oppA", 4);
    Wires.put("oppB", 4);
    Wires.put("gt", 1);
    Wires.put("eq", 1);
    Wires.put("lt", 1);
    Wires.put("CompIn", 3);
    Wires.put("CompOut", 3);
    return Wires;
  }

  @Override
  public ArrayList<String> GetModuleFunctionality(
      Netlist TheNetlist, AttributeSet attrs, FPGAReport Reporter, String HDLType) {
    ArrayList<String> Contents = new ArrayList<String>();
    Contents.add("   oppA   <= A3&A2&A1&A0;");
    Contents.add("   oppB   <= B3&B2&B1&B0;");
    Contents.add("   gt     <= '1' WHEN unsigned(oppA) > unsigned(oppB) ELSE '0';");
    Contents.add("   eq     <= '1' WHEN unsigned(oppA) = unsigned(oppB) ELSE '0';");
    Contents.add("   lt     <= '1' WHEN unsigned(oppA) < unsigned(oppB) ELSE '0';");
    Contents.add(" ");
    Contents.add("   CompIn <= AgtBin&AltBin&AeqBin;");
    Contents.add("   WITH (CompIn) SELECT CompOut <= ");
    Contents.add("      \"100\" WHEN \"100\",");
    Contents.add("      \"010\" WHEN \"010\",");
    Contents.add("      \"000\" WHEN \"110\",");
    Contents.add("      \"110\" WHEN \"000\",");
    Contents.add("      \"001\" WHEN OTHERS;");
    Contents.add(" ");
    Contents.add("   AgtBout <= '1' WHEN gt = '1' ELSE '0' WHEN lt = '1' ELSE CompOut(2);");
    Contents.add("   AltBout <= '0' WHEN gt = '1' ELSE '1' WHEN lt = '1' ELSE CompOut(1);");
    Contents.add("   AeqBout <= '0' WHEN (gt = '1') OR (lt = '1') ELSE CompOut(0);");
    return Contents;
  }

  @Override
  public SortedMap<String, String> GetPortMap(
      Netlist Nets, Object MapInfo, FPGAReport Reporter, String HDLType) {
    SortedMap<String, String> PortMap = new TreeMap<String, String>();
    if (!(MapInfo instanceof NetlistComponent)) return PortMap;
    NetlistComponent ComponentInfo = (NetlistComponent) MapInfo;
    PortMap.putAll(GetNetMap("A0", true, ComponentInfo, 8, Reporter, HDLType, Nets));
    PortMap.putAll(GetNetMap("A1", true, ComponentInfo, 10, Reporter, HDLType, Nets));
    PortMap.putAll(GetNetMap("A2", true, ComponentInfo, 11, Reporter, HDLType, Nets));
    PortMap.putAll(GetNetMap("A3", true, ComponentInfo, 13, Reporter, HDLType, Nets));
    PortMap.putAll(GetNetMap("B0", true, ComponentInfo, 7, Reporter, HDLType, Nets));
    PortMap.putAll(GetNetMap("B1", true, ComponentInfo, 9, Reporter, HDLType, Nets));
    PortMap.putAll(GetNetMap("B2", true, ComponentInfo, 12, Reporter, HDLType, Nets));
    PortMap.putAll(GetNetMap("B3", true, ComponentInfo, 0, Reporter, HDLType, Nets));
    PortMap.putAll(GetNetMap("AltBin", true, ComponentInfo, 1, Reporter, HDLType, Nets));
    PortMap.putAll(GetNetMap("AeqBin", true, ComponentInfo, 2, Reporter, HDLType, Nets));
    PortMap.putAll(GetNetMap("AgtBin", true, ComponentInfo, 3, Reporter, HDLType, Nets));
    PortMap.putAll(GetNetMap("AltBout", true, ComponentInfo, 6, Reporter, HDLType, Nets));
    PortMap.putAll(GetNetMap("AeqBout", true, ComponentInfo, 5, Reporter, HDLType, Nets));
    PortMap.putAll(GetNetMap("AgtBout", true, ComponentInfo, 4, Reporter, HDLType, Nets));
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
  public boolean HDLTargetSupported(String HDLType, AttributeSet attrs) {
    /* TODO: Add support for the ones with VCC and Ground Pin */
    if (attrs == null) return false;
    return (!attrs.getValue(TTL.VCC_GND) && (HDLType.equals(HDLGeneratorFactory.VHDL)));
  }
}
