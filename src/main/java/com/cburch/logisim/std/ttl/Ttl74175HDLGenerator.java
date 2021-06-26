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
import com.cburch.logisim.fpga.gui.Reporter;
import com.cburch.logisim.fpga.hdlgenerator.AbstractHDLGeneratorFactory;
import com.cburch.logisim.fpga.hdlgenerator.HDL;
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
    SortedMap<String, Integer> myInputs = new TreeMap<>();
    myInputs.put("nCLR", 1);
    myInputs.put("CLK", 1);
    myInputs.put("Tick", 1);
    myInputs.put("D1", 1);
    myInputs.put("D2", 1);
    myInputs.put("D3", 1);
    myInputs.put("D4", 1);
    return myInputs;
  }

  @Override
  public SortedMap<String, Integer> GetOutputList(Netlist TheNetlist, AttributeSet attrs) {
    SortedMap<String, Integer> myOutputs = new TreeMap<>();
    myOutputs.put("nQ1", 1);
    myOutputs.put("Q1", 1);
    myOutputs.put("nQ2", 1);
    myOutputs.put("Q2", 1);
    myOutputs.put("nQ3", 1);
    myOutputs.put("Q3", 1);
    myOutputs.put("nQ4", 1);
    myOutputs.put("Q4", 1);
    return myOutputs;
  }

  @Override
  public SortedMap<String, Integer> GetWireList(AttributeSet attrs, Netlist Nets) {
    SortedMap<String, Integer> wires = new TreeMap<>();
    wires.put("CurState", 4);
    wires.put("NextState", 4);
    return wires;
  }

  @Override
  public ArrayList<String> GetModuleFunctionality(Netlist TheNetlist, AttributeSet attrs) {
    final var contents = new ArrayList<String>();
    contents.add("   NextState <= CurState WHEN tick = '0' ELSE");
    contents.add("                D4&D3&D2&D1;");
    contents.add(" ");
    contents.add("   dffs : PROCESS( CLK , nCLR ) IS");
    contents.add("      BEGIN");
    contents.add("         IF (nCLR = '0') THEN CurState <= \"0000\";");
    contents.add("         ELSIF (rising_edge(CLK)) THEN");
    contents.add("            CurState <= NextState;");
    contents.add("         END IF;");
    contents.add("      END PROCESS dffs;");
    contents.add(" ");
    contents.add("   nQ1 <= NOT(CurState(0));");
    contents.add("   Q1  <= CurState(0);");
    contents.add("   nQ2 <= NOT(CurState(1));");
    contents.add("   Q2  <= CurState(1);");
    contents.add("   nQ3 <= NOT(CurState(2));");
    contents.add("   Q3  <= CurState(2);");
    contents.add("   nQ4 <= NOT(CurState(3));");
    contents.add("   Q4  <= CurState(3);");
    return contents;
  }

  public SortedMap<String, String> GetPortMap(Netlist nets, Object mapInfo) {
    SortedMap<String, String> portMap = new TreeMap<>();
    if (!(mapInfo instanceof NetlistComponent)) return portMap;
    final var componentInfo = (NetlistComponent) mapInfo;
    var gatedClock = false;
    var hasClock = true;
    int clockPinIndex = componentInfo.GetComponent().getFactory().ClockPinIndex(null)[0];
    if (!componentInfo.EndIsConnected(clockPinIndex)) {
      Reporter.Report.AddSevereWarning("Component \"TTL74165\" in circuit \"" + nets.getCircuitName()
              + "\" has no clock connection");
      hasClock = false;
    }
    final var ClockNetName = GetClockNetName(componentInfo, clockPinIndex, nets);
    if (ClockNetName.isEmpty()) {
      gatedClock = true;
    }
    if (!hasClock) {
      portMap.put("CLK", "'0'");
      portMap.put("Tick", "'0'");
    } else if (gatedClock) {
      portMap.put("Tick", "'1'");
      portMap.put("CLK", GetNetName(componentInfo, clockPinIndex, true, nets));
    } else {
      if (nets.RequiresGlobalClockConnection()) {
        portMap.put("Tick", "'1'");
      } else {
        portMap.put(
            "Tick",
            ClockNetName
                + "("
                + ClockHDLGeneratorFactory.PositiveEdgeTickIndex
                + ")");
      }
      portMap.put(
          "CLK",
          ClockNetName + "(" + ClockHDLGeneratorFactory.GlobalClockIndex + ")");
    }
    portMap.putAll(GetNetMap("nCLR", true, componentInfo, 0, nets));
    portMap.putAll(GetNetMap("Q1", true, componentInfo, 1, nets));
    portMap.putAll(GetNetMap("nQ1", true, componentInfo, 2, nets));
    portMap.putAll(GetNetMap("D1", true, componentInfo, 3, nets));
    portMap.putAll(GetNetMap("D2", true, componentInfo, 4, nets));
    portMap.putAll(GetNetMap("nQ2", true, componentInfo, 5, nets));
    portMap.putAll(GetNetMap("Q2", true, componentInfo, 6, nets));
    portMap.putAll(GetNetMap("Q3", true, componentInfo, 8, nets));
    portMap.putAll(GetNetMap("nQ3", true, componentInfo, 9, nets));
    portMap.putAll(GetNetMap("D3", true, componentInfo, 10, nets));
    portMap.putAll(GetNetMap("D4", true, componentInfo, 11, nets));
    portMap.putAll(GetNetMap("nQ4", true, componentInfo, 12, nets));
    portMap.putAll(GetNetMap("Q4", true, componentInfo, 13, nets));
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
    return (!attrs.getValue(TTL.VCC_GND) && HDL.isVHDL());
  }
}
