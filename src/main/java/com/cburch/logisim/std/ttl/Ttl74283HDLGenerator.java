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
import com.cburch.logisim.util.LineBuffer;
import java.util.ArrayList;
import java.util.SortedMap;
import java.util.TreeMap;

public class Ttl74283HDLGenerator extends AbstractHDLGeneratorFactory {

  @Override
  public String getComponentStringIdentifier() {
    return "TTL74283";
  }

  @Override
  public SortedMap<String, Integer> GetInputList(Netlist TheNetlist, AttributeSet attrs) {
    final var map = new TreeMap<String, Integer>();
    map.put("A1", 1);
    map.put("A2", 1);
    map.put("A3", 1);
    map.put("A4", 1);
    map.put("B1", 1);
    map.put("B2", 1);
    map.put("B3", 1);
    map.put("B4", 1);
    map.put("Cin", 1);
    return map;
  }

  @Override
  public SortedMap<String, Integer> GetOutputList(Netlist TheNetlist, AttributeSet attrs) {
    final var map = new TreeMap<String, Integer>();
    map.put("S1", 1);
    map.put("S2", 1);
    map.put("S3", 1);
    map.put("S4", 1);
    map.put("Cout", 1);
    return map;
  }

  @Override
  public SortedMap<String, Integer> GetWireList(AttributeSet attrs, Netlist Nets) {
    final var map = new TreeMap<String, Integer>();
    map.put("oppA", 5);
    map.put("oppB", 5);
    map.put("oppC", 5);
    map.put("Result", 5);
    return map;
  }

  @Override
  public ArrayList<String> GetModuleFunctionality(Netlist TheNetlist, AttributeSet attrs) {
    return (new LineBuffer())
        .addLines(
            "oppA   <= \"0\"&A4&A3&A2&A1;",
            "oppB   <= \"0\"&B4&B3&B2&B1;",
            "oppC   <= \"0000\"&Cin;",
            "Result <= std_logic_vector(unsigned(oppA)+unsigned(oppB)+unsigned(oppC));",
            "S1     <= Result(0);",
            "S2     <= Result(1);",
            "S3     <= Result(2);",
            "S4     <= Result(3);",
            "Cout   <= Result(4);")
        .getWithIndent();
  }

  @Override
  public SortedMap<String, String> GetPortMap(Netlist nets, Object mapInfo) {
    final var map = new TreeMap<String, String>();
    if (!(mapInfo instanceof NetlistComponent)) return map;
    final var comp = (NetlistComponent) mapInfo;
    map.putAll(GetNetMap("A1", true, comp, 4, nets));
    map.putAll(GetNetMap("A2", true, comp, 2, nets));
    map.putAll(GetNetMap("A3", true, comp, 12, nets));
    map.putAll(GetNetMap("A4", true, comp, 10, nets));
    map.putAll(GetNetMap("B1", true, comp, 5, nets));
    map.putAll(GetNetMap("B2", true, comp, 1, nets));
    map.putAll(GetNetMap("B3", true, comp, 13, nets));
    map.putAll(GetNetMap("B4", true, comp, 9, nets));
    map.putAll(GetNetMap("Cin", true, comp, 6, nets));
    map.putAll(GetNetMap("S1", true, comp, 3, nets));
    map.putAll(GetNetMap("S2", true, comp, 0, nets));
    map.putAll(GetNetMap("S3", true, comp, 11, nets));
    map.putAll(GetNetMap("S4", true, comp, 8, nets));
    map.putAll(GetNetMap("Cout", true, comp, 7, nets));
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
