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

public class AbstractOctalFlopsHDLGenerator extends AbstractHDLGeneratorFactory {

  @Override
  public SortedMap<String, Integer> GetInputList(Netlist TheNetlist, AttributeSet attrs) {
    final var map = new TreeMap<String, Integer>();
    map.put("nCLR", 1);
    map.put("nCLKen", 1);
    map.put("CLK", 1);
    map.put("tick", 1);
    map.put("D0", 1);
    map.put("D1", 1);
    map.put("D2", 1);
    map.put("D3", 1);
    map.put("D4", 1);
    map.put("D5", 1);
    map.put("D6", 1);
    map.put("D7", 1);
    return map;
  }

  @Override
  public SortedMap<String, Integer> GetOutputList(Netlist TheNetlist, AttributeSet attrs) {
    final var map = new TreeMap<String, Integer>();
    map.put("Q0", 1);
    map.put("Q1", 1);
    map.put("Q2", 1);
    map.put("Q3", 1);
    map.put("Q4", 1);
    map.put("Q5", 1);
    map.put("Q6", 1);
    map.put("Q7", 1);
    return map;
  }

  @Override
  public SortedMap<String, Integer> GetWireList(AttributeSet attrs, Netlist Nets) {
    final var map = new TreeMap<String, Integer>();
    map.put("state", 8);
    map.put("enable", 1);
    map.put("nexts", 8);
    return map;
  }

  @Override
  public ArrayList<String> GetModuleFunctionality(Netlist theNetlist, AttributeSet attrs) {
    return (new LineBuffer())
        .addLines(
            "enable <= tick and NOT(nCLKen);",
            "nexts  <= D7&D6&D5&D4&D3&D2&D1&D0 WHEN enable = '1' ELSE state;",
            "Q0     <= state(0);",
            "Q1     <= state(1);",
            "Q2     <= state(2);",
            "Q3     <= state(3);",
            "Q4     <= state(4);",
            "Q5     <= state(5);",
            "Q6     <= state(6);",
            "Q7     <= state(7);",
            "",
            "dffs : PROCESS( CLK , nCLR ) IS",
            "   BEGIN",
            "      IF (nCLR = '1') THEN state <= (OTHERS => '0');",
            "      ELSIF (rising_edge(CLK)) THEN state <= nexts;",
            "      END IF;",
            "   END PROCESS dffs;")
        .getWithIndent();
  }

  @Override
  public SortedMap<String, String> GetPortMap(Netlist nets, Object mapInfo) {
    final var map = new TreeMap<String, String>();
    if (!(mapInfo instanceof NetlistComponent)) return map;
    final var comp = (NetlistComponent) mapInfo;
    var gatedClock = false;
    var hasClock = true;
    final var clockPinIndex = comp.getComponent().getFactory().ClockPinIndex(null)[0];
    if (!comp.isEndConnected(clockPinIndex)) {
      Reporter.Report.AddSevereWarning(
          "Component \""
              + getComponentStringIdentifier()
              + "\" in circuit \""
              + nets.getCircuitName()
              + "\" has no clock connection");
      hasClock = false;
    }
    String ClockNetName = GetClockNetName(comp, clockPinIndex, nets);
    if (ClockNetName.isEmpty()) {
      gatedClock = true;
    }
    if (!hasClock) {
      map.put("CLK", "'0'");
      map.put("tick", "'0'");
    } else if (gatedClock) {
      map.put("tick", "'1'");
      map.put("CLK", GetNetName(comp, clockPinIndex, true, nets));
    } else {
      if (nets.requiresGlobalClockConnection()) {
        map.put("tick", "'1'");
      } else {
        map.put(
            "tick",
            ClockNetName
                + "("
                + ClockHDLGeneratorFactory.POSITIVE_EDGE_TICK_INDEX
                + ")");
      }
      map.put(
          "CLK",
          ClockNetName + "(" + ClockHDLGeneratorFactory.GLOBAL_CLOCK_INDEX + ")");
    }
    map.putAll(GetNetMap("D0", true, comp, 2, nets));
    map.putAll(GetNetMap("D1", true, comp, 3, nets));
    map.putAll(GetNetMap("D2", true, comp, 6, nets));
    map.putAll(GetNetMap("D3", true, comp, 7, nets));
    map.putAll(GetNetMap("D4", true, comp, 11, nets));
    map.putAll(GetNetMap("D5", true, comp, 12, nets));
    map.putAll(GetNetMap("D6", true, comp, 15, nets));
    map.putAll(GetNetMap("D7", true, comp, 16, nets));
    map.putAll(GetNetMap("Q0", true, comp, 1, nets));
    map.putAll(GetNetMap("Q1", true, comp, 4, nets));
    map.putAll(GetNetMap("Q2", true, comp, 5, nets));
    map.putAll(GetNetMap("Q3", true, comp, 8, nets));
    map.putAll(GetNetMap("Q4", true, comp, 10, nets));
    map.putAll(GetNetMap("Q5", true, comp, 13, nets));
    map.putAll(GetNetMap("Q6", true, comp, 14, nets));
    map.putAll(GetNetMap("Q7", true, comp, 17, nets));
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
    return (!attrs.getValue(TtlLibrary.VCC_GND) && (HDL.isVHDL()));
  }
}
