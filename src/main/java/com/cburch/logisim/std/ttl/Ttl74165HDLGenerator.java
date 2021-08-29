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

public class Ttl74165HDLGenerator extends AbstractHDLGeneratorFactory {

  @Override
  public String getComponentStringIdentifier() {
    return "TTL74165";
  }

  @Override
  public SortedMap<String, Integer> GetInputList(Netlist TheNetlist, AttributeSet attrs) {
    final var map = new TreeMap<String, Integer>();
    map.put("SHnLD", 1);
    map.put("CK", 1);
    map.put("CKIh", 1);
    map.put("SER", 1);
    map.put("P0", 1);
    map.put("P1", 1);
    map.put("P2", 1);
    map.put("P3", 1);
    map.put("P4", 1);
    map.put("P5", 1);
    map.put("P6", 1);
    map.put("P7", 1);
    map.put("Tick", 1);
    return map;
  }

  @Override
  public SortedMap<String, Integer> GetOutputList(Netlist TheNetlist, AttributeSet attrs) {
    final var map = new TreeMap<String, Integer>();
    map.put("Q7", 1);
    map.put("Q7n", 1);
    return map;
  }

  @Override
  public SortedMap<String, Integer> GetWireList(AttributeSet attrs, Netlist Nets) {
    final var map = new TreeMap<String, Integer>();
    map.put("CurState", 8);
    map.put("NextState", 8);
    map.put("ParData", 8);
    map.put("Enable", 1);
    return map;
  }

  @Override
  public ArrayList<String> GetModuleFunctionality(Netlist TheNetlist, AttributeSet attrs) {
    return (new LineBuffer())
        .addLines(
            "Q7  <= CurState(0);",
            "Q7n <= NOT(CurState(0));",
            "",
            "Enable  <= NOT(CKIh) AND Tick;",
            "ParData <= P7&P6&P5&P4&P3&P2&P1&P0;",
            "",
            "NextState <= CurState WHEN Enable = '0' ELSE",
            "             ParData WHEN SHnLD = '0' ELSE",
            "             SER&CurState(7 DOWNTO 1);",
            "",
            "dffs : PROCESS( CK ) IS",
            "   BEGIN",
            "      IF (rising_edge(CK)) THEN CurState <= NextState;",
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
    final var ClockPinIndex = comp.getComponent().getFactory().ClockPinIndex(null)[0];
    if (!comp.isEndConnected(ClockPinIndex)) {
      Reporter.Report.AddSevereWarning(
          "Component \"TTL74165\" in circuit \""
              + nets.getCircuitName()
              + "\" has no clock connection");
      hasClock = false;
    }
    final var clockNetName = GetClockNetName(comp, ClockPinIndex, nets);
    if (clockNetName.isEmpty()) {
      gatedClock = true;
    }
    if (!hasClock) {
      map.put("CK", "'0'");
      map.put("Tick", "'0'");
    } else if (gatedClock) {
      map.put("Tick", "'1'");
      map.put("CK", GetNetName(comp, ClockPinIndex, true, nets));
    } else {
      if (nets.requiresGlobalClockConnection()) {
        map.put("Tick", "'1'");
      } else {
        map.put(
            "Tick",
            clockNetName
                + "("
                + ClockHDLGeneratorFactory.POSITIVE_EDGE_TICK_INDEX
                + ")");
      }
      map.put(
          "CK",
          clockNetName + "(" + ClockHDLGeneratorFactory.GLOBAL_CLOCK_INDEX + ")");
    }
    map.putAll(GetNetMap("SHnLD", true, comp, 0, nets));
    map.putAll(GetNetMap("CKIh", true, comp, 13, nets));
    map.putAll(GetNetMap("SER", true, comp, 8, nets));
    map.putAll(GetNetMap("P0", true, comp, 9, nets));
    map.putAll(GetNetMap("P1", true, comp, 10, nets));
    map.putAll(GetNetMap("P2", true, comp, 11, nets));
    map.putAll(GetNetMap("P3", true, comp, 12, nets));
    map.putAll(GetNetMap("P4", true, comp, 2, nets));
    map.putAll(GetNetMap("P5", true, comp, 3, nets));
    map.putAll(GetNetMap("P6", true, comp, 4, nets));
    map.putAll(GetNetMap("P7", true, comp, 5, nets));
    map.putAll(GetNetMap("Q7n", true, comp, 6, nets));
    map.putAll(GetNetMap("Q7", true, comp, 7, nets));
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
