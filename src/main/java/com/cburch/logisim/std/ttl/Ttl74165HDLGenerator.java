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

public class Ttl74165HDLGenerator extends AbstractHDLGeneratorFactory {

  @Override
  public String getComponentStringIdentifier() {
    return "TTL74165";
  }

  @Override
  public SortedMap<String, Integer> GetInputList(Netlist TheNetlist, AttributeSet attrs) {
    SortedMap<String, Integer> myInputs = new TreeMap<>();
    myInputs.put("SHnLD", 1);
    myInputs.put("CK", 1);
    myInputs.put("CKIh", 1);
    myInputs.put("SER", 1);
    myInputs.put("P0", 1);
    myInputs.put("P1", 1);
    myInputs.put("P2", 1);
    myInputs.put("P3", 1);
    myInputs.put("P4", 1);
    myInputs.put("P5", 1);
    myInputs.put("P6", 1);
    myInputs.put("P7", 1);
    myInputs.put("Tick", 1);
    return myInputs;
  }

  @Override
  public SortedMap<String, Integer> GetOutputList(Netlist TheNetlist, AttributeSet attrs) {
    SortedMap<String, Integer> myOutputs = new TreeMap<>();
    myOutputs.put("Q7", 1);
    myOutputs.put("Q7n", 1);
    return myOutputs;
  }

  @Override
  public SortedMap<String, Integer> GetWireList(AttributeSet attrs, Netlist Nets) {
    SortedMap<String, Integer> wires = new TreeMap<>();
    wires.put("CurState", 8);
    wires.put("NextState", 8);
    wires.put("ParData", 8);
    wires.put("Enable", 1);
    return wires;
  }

  @Override
  public ArrayList<String> GetModuleFunctionality(Netlist TheNetlist, AttributeSet attrs) {
    final var contents = new ArrayList<String>();
    contents.add("   Q7  <= CurState(0);\n");
    contents.add("   Q7n <= NOT(CurState(0));\n");
    contents.add("\n");
    contents.add("   Enable  <= NOT(CKIh) AND Tick;");
    contents.add("   ParData <= P7&P6&P5&P4&P3&P2&P1&P0;");
    contents.add("\n");
    contents.add("   NextState <= CurState WHEN Enable = '0' ELSE");
    contents.add("                ParData WHEN SHnLD = '0' ELSE");
    contents.add("                SER&CurState(7 DOWNTO 1);");
    contents.add("\n");
    contents.add("   dffs : PROCESS( CK ) IS");
    contents.add("      BEGIN");
    contents.add("         IF (rising_edge(CK)) THEN CurState <= NextState;");
    contents.add("         END IF;");
    contents.add("      END PROCESS dffs;");
    return contents;
  }

  @Override
  public SortedMap<String, String> GetPortMap(Netlist nets, Object mapInfo) {
    SortedMap<String, String> portMap = new TreeMap<>();
    if (!(mapInfo instanceof NetlistComponent)) return portMap;
    final var componentInfo = (NetlistComponent) mapInfo;
    var gatedClock = false;
    var hasClock = true;
    final var ClockPinIndex = componentInfo.GetComponent().getFactory().ClockPinIndex(null)[0];
    if (!componentInfo.EndIsConnected(ClockPinIndex)) {
      Reporter.Report.AddSevereWarning(
          "Component \"TTL74165\" in circuit \""
              + nets.getCircuitName()
              + "\" has no clock connection");
      hasClock = false;
    }
    final var clockNetName = GetClockNetName(componentInfo, ClockPinIndex, nets);
    if (clockNetName.isEmpty()) {
      gatedClock = true;
    }
    if (!hasClock) {
      portMap.put("CK", "'0'");
      portMap.put("Tick", "'0'");
    } else if (gatedClock) {
      portMap.put("Tick", "'1'");
      portMap.put("CK", GetNetName(componentInfo, ClockPinIndex, true, nets));
    } else {
      if (nets.RequiresGlobalClockConnection()) {
        portMap.put("Tick", "'1'");
      } else {
        portMap.put(
            "Tick",
            clockNetName
                + "("
                + ClockHDLGeneratorFactory.PositiveEdgeTickIndex
                + ")");
      }
      portMap.put(
          "CK",
          clockNetName + "(" + ClockHDLGeneratorFactory.GlobalClockIndex + ")");
    }
    portMap.putAll(GetNetMap("SHnLD", true, componentInfo, 0, nets));
    portMap.putAll(GetNetMap("CKIh", true, componentInfo, 13, nets));
    portMap.putAll(GetNetMap("SER", true, componentInfo, 8, nets));
    portMap.putAll(GetNetMap("P0", true, componentInfo, 9, nets));
    portMap.putAll(GetNetMap("P1", true, componentInfo, 10, nets));
    portMap.putAll(GetNetMap("P2", true, componentInfo, 11, nets));
    portMap.putAll(GetNetMap("P3", true, componentInfo, 12, nets));
    portMap.putAll(GetNetMap("P4", true, componentInfo, 2, nets));
    portMap.putAll(GetNetMap("P5", true, componentInfo, 3, nets));
    portMap.putAll(GetNetMap("P6", true, componentInfo, 4, nets));
    portMap.putAll(GetNetMap("P7", true, componentInfo, 5, nets));
    portMap.putAll(GetNetMap("Q7n", true, componentInfo, 6, nets));
    portMap.putAll(GetNetMap("Q7", true, componentInfo, 7, nets));
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
