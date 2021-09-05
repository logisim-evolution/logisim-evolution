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
import com.cburch.logisim.util.LineBuffer;
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
    final var map = new TreeMap<String, Integer>();
    map.put("nCLR1", 1);
    map.put("D1", 1);
    map.put("CLK1", 1);
    map.put("tick1", 1);
    map.put("nPRE1", 1);
    map.put("nCLR2", 1);
    map.put("D2", 1);
    map.put("CLK2", 1);
    map.put("tick2", 1);
    map.put("nPRE2", 1);
    return map;
  }

  @Override
  public SortedMap<String, Integer> GetOutputList(Netlist TheNetlist, AttributeSet attrs) {
    final var map = new TreeMap<String, Integer>();
    map.put("Q1", 1);
    map.put("nQ1", 1);
    map.put("Q2", 1);
    map.put("nQ2", 1);
    return map;
  }

  @Override
  public SortedMap<String, Integer> GetWireList(AttributeSet attrs, Netlist Nets) {
    final var map = new TreeMap<String, Integer>();
    map.put("state1", 1);
    map.put("state2", 1);
    map.put("next1", 1);
    map.put("next2", 1);
    return map;
  }

  @Override
  public ArrayList<String> GetModuleFunctionality(Netlist theNetlist, AttributeSet attrs) {
    final var contents = new LineBuffer();
    return contents
        .addLines(
            "Q1  <= state1;",
            "nQ1 <= NOT(state1,",
            "Q2  <= state1;",
            "nQ2 <= NOT(state1,",
            "",
            "next1 <= D1 WHEN tick1='1' ELSE state1;",
            "next2 <= D2 WHEN tick2='1' ELSE state2;",
            "",
            "ff1 : PROCESS ( CLK1 , nCLR1 , nPRE1 ) IS",
            "   BEGIN",
            "      IF (nCLR1 = '0') THEN state1 <= '0';",
            "      ELSIF (nPRE1 = '1') THEN state1 <= '1';",
            "      ELSIF (rising_edge(CLK1)) THEN state1 <= next1;",
            "      END IF;",
            "   END PROCESS ff1;",
            "",
            "ff2 : PROCESS ( CLK2 , nCLR2 , nPRE2 ) IS",
            "   BEGIN",
            "      IF (nCLR2 = '0') THEN state2 <= '0';",
            "      ELSIF (nPRE2 = '1') THEN state2 <= '1';",
            "      ELSIF (rising_edge(CLK2)) THEN state2 <= next2;",
            "      END IF;",
            "   END PROCESS ff2;")
        .getWithIndent();
  }

  @Override
  public SortedMap<String, String> GetPortMap(Netlist Nets, Object MapInfo) {
    final var map = new TreeMap<String, String>();
    if (!(MapInfo instanceof NetlistComponent)) return map;
    final var componentinfo = (NetlistComponent) MapInfo;
    for (var i = 0; i < 2; i++) {
      var gatedClock = false;
      var hasClock = true;
      int clockPinIndex = componentinfo.getComponent().getFactory().ClockPinIndex(null)[i];
      if (!componentinfo.isEndConnected(clockPinIndex)) {
        Reporter.Report.AddSevereWarning(
            "Component \"TTL7474\" in circuit \""
                + Nets.getCircuitName()
                + "\" has no clock connection for dff"
                + (i + 1));
        hasClock = false;
      }
      final var clockNetName = GetClockNetName(componentinfo, clockPinIndex, Nets);
      if (clockNetName.isEmpty()) {
        gatedClock = true;
      }
      if (!hasClock) {
        map.put("CLK" + (i + 1), "'0'");
        map.put("tick" + (i + 1), "'0'");
      } else if (gatedClock) {
        map.put("tick" + (i + 1), "'1'");
        map.put(
            "CLK" + (i + 1),
            GetNetName(componentinfo, clockPinIndex, true, Nets));
      } else {
        if (Nets.requiresGlobalClockConnection()) {
          map.put("tick" + (i + 1), "'1'");
        } else {
          map.put(
              "tick" + (i + 1),
              clockNetName
                  + "("
                  + ClockHDLGeneratorFactory.POSITIVE_EDGE_TICK_INDEX
                  + ")");
        }
        map.put(
            "CLK" + (i + 1),
            clockNetName + "(" + ClockHDLGeneratorFactory.GLOBAL_CLOCK_INDEX + ")");
      }
    }
    map.putAll(GetNetMap("nCLR1", false, componentinfo, 0, Nets));
    map.putAll(GetNetMap("D1", true, componentinfo, 1, Nets));
    map.putAll(GetNetMap("nPRE1", false, componentinfo, 3, Nets));
    map.putAll(GetNetMap("Q1", true, componentinfo, 4, Nets));
    map.putAll(GetNetMap("nQ1", true, componentinfo, 5, Nets));
    map.putAll(GetNetMap("nCLR2", false, componentinfo, 11, Nets));
    map.putAll(GetNetMap("D2", true, componentinfo, 10, Nets));
    map.putAll(GetNetMap("nPRE2", false, componentinfo, 8, Nets));
    map.putAll(GetNetMap("Q2", true, componentinfo, 7, Nets));
    map.putAll(GetNetMap("nQ2", true, componentinfo, 6, Nets));
    return map;
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
    return (!attrs.getValue(TtlLibrary.VCC_GND) && HDL.isVHDL());
  }
}
