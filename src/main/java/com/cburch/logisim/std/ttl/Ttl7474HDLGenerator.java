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

public class Ttl7474HDLGenerator extends AbstractHDLGeneratorFactory {

  @Override
  public String getComponentStringIdentifier() {
    return "TTL7474";
  }

  @Override
  public SortedMap<String, Integer> GetInputList(Netlist TheNetlist, AttributeSet attrs) {
    SortedMap<String, Integer> MyInputs = new TreeMap<String, Integer>();
    MyInputs.put("nCLR1", 1);
    MyInputs.put("D1", 1);
    MyInputs.put("CLK1", 1);
    MyInputs.put("tick1", 1);
    MyInputs.put("nPRE1", 1);
    MyInputs.put("nCLR2", 1);
    MyInputs.put("D2", 1);
    MyInputs.put("CLK2", 1);
    MyInputs.put("tick2", 1);
    MyInputs.put("nPRE2", 1);
    return MyInputs;
  }

  @Override
  public SortedMap<String, Integer> GetOutputList(Netlist TheNetlist, AttributeSet attrs) {
    SortedMap<String, Integer> MyOutputs = new TreeMap<String, Integer>();
    MyOutputs.put("Q1", 1);
    MyOutputs.put("nQ1", 1);
    MyOutputs.put("Q2", 1);
    MyOutputs.put("nQ2", 1);
    return MyOutputs;
  }

  @Override
  public SortedMap<String, Integer> GetWireList(AttributeSet attrs, Netlist Nets) {
    SortedMap<String, Integer> Wires = new TreeMap<String, Integer>();
    Wires.put("state1", 1);
    Wires.put("state2", 1);
    Wires.put("next1", 1);
    Wires.put("next2", 1);
    return Wires;
  }

  @Override
  public ArrayList<String> GetModuleFunctionality(
      Netlist TheNetlist, AttributeSet attrs, FPGAReport Reporter, String HDLType) {
    ArrayList<String> Contents = new ArrayList<String>();
    Contents.add("   Q1  <= state1;");
    Contents.add("   nQ1 <= NOT(state1);");
    Contents.add("   Q2  <= state1;");
    Contents.add("   nQ2 <= NOT(state1);");
    Contents.add(" ");
    Contents.add("   next1 <= D1 WHEN tick1='1' ELSE state1;");
    Contents.add("   next2 <= D2 WHEN tick2='1' ELSE state2;");
    Contents.add(" ");
    Contents.add("   ff1 : PROCESS ( CLK1 , nCLR1 , nPRE1 ) IS");
    Contents.add("      BEGIN");
    Contents.add("         IF (nCLR1 = '0') THEN state1 <= '0';");
    Contents.add("         ELSIF (nPRE1 = '1') THEN state1 <= '1';");
    Contents.add("         ELSIF (rising_edge(CLK1)) THEN state1 <= next1;");
    Contents.add("         END IF;");
    Contents.add("      END PROCESS ff1;");
    Contents.add(" ");
    Contents.add("   ff2 : PROCESS ( CLK2 , nCLR2 , nPRE2 ) IS");
    Contents.add("      BEGIN");
    Contents.add("         IF (nCLR2 = '0') THEN state2 <= '0';");
    Contents.add("         ELSIF (nPRE2 = '1') THEN state2 <= '1';");
    Contents.add("         ELSIF (rising_edge(CLK2)) THEN state2 <= next2;");
    Contents.add("         END IF;");
    Contents.add("      END PROCESS ff2;");
    return Contents;
  }

  @Override
  public SortedMap<String, String> GetPortMap(
      Netlist Nets, Object MapInfo, FPGAReport Reporter, String HDLType) {
    SortedMap<String, String> PortMap = new TreeMap<String, String>();
    if (!(MapInfo instanceof NetlistComponent)) return PortMap;
    NetlistComponent ComponentInfo = (NetlistComponent) MapInfo;
    for (int i = 0; i < 2; i++) {
      Boolean GatedClock = false;
      Boolean HasClock = true;
      int ClockPinIndex = ComponentInfo.GetComponent().getFactory().ClockPinIndex(null)[i];
      if (!ComponentInfo.EndIsConnected(ClockPinIndex)) {
        Reporter.AddSevereWarning(
            "Component \"TTL7474\" in circuit \""
                + Nets.getCircuitName()
                + "\" has no clock connection for dff"
                + Integer.toString(i + 1));
        HasClock = false;
      }
      String ClockNetName = GetClockNetName(ComponentInfo, ClockPinIndex, Nets);
      if (ClockNetName.isEmpty()) {
        GatedClock = true;
      }
      if (!HasClock) {
        PortMap.put("CLK" + Integer.toString(i + 1), "'0'");
        PortMap.put("tick" + Integer.toString(i + 1), "'0'");
      } else if (GatedClock) {
        PortMap.put("tick" + Integer.toString(i + 1), "'1'");
        PortMap.put(
            "CLK" + Integer.toString(i + 1),
            GetNetName(ComponentInfo, ClockPinIndex, true, HDLType, Nets));
      } else {
        if (Nets.RequiresGlobalClockConnection()) {
          PortMap.put("tick" + Integer.toString(i + 1), "'1'");
        } else {
          PortMap.put(
              "tick" + Integer.toString(i + 1),
              ClockNetName
                  + "("
                  + Integer.toString(ClockHDLGeneratorFactory.PositiveEdgeTickIndex)
                  + ")");
        }
        PortMap.put(
            "CLK" + Integer.toString(i + 1),
            ClockNetName + "(" + Integer.toString(ClockHDLGeneratorFactory.GlobalClockIndex) + ")");
      }
    }
    PortMap.putAll(GetNetMap("nCLR1", false, ComponentInfo, 0, Reporter, HDLType, Nets));
    PortMap.putAll(GetNetMap("D1", true, ComponentInfo, 1, Reporter, HDLType, Nets));
    PortMap.putAll(GetNetMap("nPRE1", false, ComponentInfo, 3, Reporter, HDLType, Nets));
    PortMap.putAll(GetNetMap("Q1", true, ComponentInfo, 4, Reporter, HDLType, Nets));
    PortMap.putAll(GetNetMap("nQ1", true, ComponentInfo, 5, Reporter, HDLType, Nets));
    PortMap.putAll(GetNetMap("nCLR2", false, ComponentInfo, 11, Reporter, HDLType, Nets));
    PortMap.putAll(GetNetMap("D2", true, ComponentInfo, 10, Reporter, HDLType, Nets));
    PortMap.putAll(GetNetMap("nPRE2", false, ComponentInfo, 8, Reporter, HDLType, Nets));
    PortMap.putAll(GetNetMap("Q2", true, ComponentInfo, 7, Reporter, HDLType, Nets));
    PortMap.putAll(GetNetMap("nQ2", true, ComponentInfo, 6, Reporter, HDLType, Nets));
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
