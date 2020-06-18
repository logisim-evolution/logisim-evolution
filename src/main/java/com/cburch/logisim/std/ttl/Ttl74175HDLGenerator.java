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
import com.cburch.logisim.std.wiring.ClockHDLGeneratorFactory;
import java.util.ArrayList;
import java.util.SortedMap;
import java.util.TreeMap;

public class Ttl74175HDLGenerator extends AbstractHDLGeneratorFactory {

  @Override
  public String getComponentStringIdentifier() {
    return "TTL74175";
  }

  @Override
  public SortedMap<String, Integer> GetInputList(Netlist TheNetlist, AttributeSet attrs) {
    SortedMap<String, Integer> MyInputs = new TreeMap<String, Integer>();
    MyInputs.put("nCLR", 1);
    MyInputs.put("CLK", 1);
    MyInputs.put("Tick", 1);
    MyInputs.put("D1", 1);
    MyInputs.put("D2", 1);
    MyInputs.put("D3", 1);
    MyInputs.put("D4", 1);
    return MyInputs;
  }

  @Override
  public SortedMap<String, Integer> GetOutputList(Netlist TheNetlist, AttributeSet attrs) {
    SortedMap<String, Integer> MyOutputs = new TreeMap<String, Integer>();
    MyOutputs.put("nQ1", 1);
    MyOutputs.put("Q1", 1);
    MyOutputs.put("nQ2", 1);
    MyOutputs.put("Q2", 1);
    MyOutputs.put("nQ3", 1);
    MyOutputs.put("Q3", 1);
    MyOutputs.put("nQ4", 1);
    MyOutputs.put("Q4", 1);
    return MyOutputs;
  }

  @Override
  public SortedMap<String, Integer> GetWireList(AttributeSet attrs, Netlist Nets) {
    SortedMap<String, Integer> Wires = new TreeMap<String, Integer>();
    Wires.put("CurState", 4);
    Wires.put("NextState", 4);
    return Wires;
  }

  @Override
  public ArrayList<String> GetModuleFunctionality(
      Netlist TheNetlist, AttributeSet attrs, FPGAReport Reporter, String HDLType) {
    ArrayList<String> Contents = new ArrayList<String>();
    Contents.add("   NextState <= CurState WHEN tick = '0' ELSE");
    Contents.add("                D4&D3&D2&D1;");
    Contents.add(" ");
    Contents.add("   dffs : PROCESS( CLK , nCLR ) IS");
    Contents.add("      BEGIN");
    Contents.add("         IF (nCLR = '0') THEN CurState <= \"0000\";");
    Contents.add("         ELSIF (rising_edge(CLK)) THEN");
    Contents.add("            CurState <= NextState;");
    Contents.add("         END IF;");
    Contents.add("      END PROCESS dffs;");
    Contents.add(" ");
    Contents.add("   nQ1 <= NOT(CurState(0));");
    Contents.add("   Q1  <= CurState(0);");
    Contents.add("   nQ2 <= NOT(CurState(1));");
    Contents.add("   Q2  <= CurState(1);");
    Contents.add("   nQ3 <= NOT(CurState(2));");
    Contents.add("   Q3  <= CurState(2);");
    Contents.add("   nQ4 <= NOT(CurState(3));");
    Contents.add("   Q4  <= CurState(3);");
    return Contents;
  }

  public SortedMap<String, String> GetPortMap(
      Netlist Nets, Object MapInfo, FPGAReport Reporter, String HDLType) {
    SortedMap<String, String> PortMap = new TreeMap<String, String>();
    if (!(MapInfo instanceof NetlistComponent)) return PortMap;
    NetlistComponent ComponentInfo = (NetlistComponent) MapInfo;
    Boolean GatedClock = false;
    Boolean HasClock = true;
    int ClockPinIndex = ComponentInfo.GetComponent().getFactory().ClockPinIndex(null)[0];
    if (!ComponentInfo.EndIsConnected(ClockPinIndex)) {
      Reporter.AddSevereWarning(
          "Component \"TTL74165\" in circuit \""
              + Nets.getCircuitName()
              + "\" has no clock connection");
      HasClock = false;
    }
    String ClockNetName = GetClockNetName(ComponentInfo, ClockPinIndex, Nets);
    if (ClockNetName.isEmpty()) {
      GatedClock = true;
    }
    if (!HasClock) {
      PortMap.put("CLK", "'0'");
      PortMap.put("Tick", "'0'");
    } else if (GatedClock) {
      PortMap.put("Tick", "'1'");
      PortMap.put("CLK", GetNetName(ComponentInfo, ClockPinIndex, true, HDLType, Nets));
    } else {
      if (Nets.RequiresGlobalClockConnection()) {
        PortMap.put("Tick", "'1'");
      } else {
        PortMap.put(
            "Tick",
            ClockNetName
                + "("
                + Integer.toString(ClockHDLGeneratorFactory.PositiveEdgeTickIndex)
                + ")");
      }
      PortMap.put(
          "CLK",
          ClockNetName + "(" + Integer.toString(ClockHDLGeneratorFactory.GlobalClockIndex) + ")");
    }
    PortMap.putAll(GetNetMap("nCLR", true, ComponentInfo, 0, Reporter, HDLType, Nets));
    PortMap.putAll(GetNetMap("Q1", true, ComponentInfo, 1, Reporter, HDLType, Nets));
    PortMap.putAll(GetNetMap("nQ1", true, ComponentInfo, 2, Reporter, HDLType, Nets));
    PortMap.putAll(GetNetMap("D1", true, ComponentInfo, 3, Reporter, HDLType, Nets));
    PortMap.putAll(GetNetMap("D2", true, ComponentInfo, 4, Reporter, HDLType, Nets));
    PortMap.putAll(GetNetMap("nQ2", true, ComponentInfo, 5, Reporter, HDLType, Nets));
    PortMap.putAll(GetNetMap("Q2", true, ComponentInfo, 6, Reporter, HDLType, Nets));
    PortMap.putAll(GetNetMap("Q3", true, ComponentInfo, 8, Reporter, HDLType, Nets));
    PortMap.putAll(GetNetMap("nQ3", true, ComponentInfo, 9, Reporter, HDLType, Nets));
    PortMap.putAll(GetNetMap("D3", true, ComponentInfo, 10, Reporter, HDLType, Nets));
    PortMap.putAll(GetNetMap("D4", true, ComponentInfo, 11, Reporter, HDLType, Nets));
    PortMap.putAll(GetNetMap("nQ4", true, ComponentInfo, 12, Reporter, HDLType, Nets));
    PortMap.putAll(GetNetMap("Q4", true, ComponentInfo, 13, Reporter, HDLType, Nets));
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
