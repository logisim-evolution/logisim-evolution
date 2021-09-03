/*
 * This file is part of logisim-evolution.
 *
 * Logisim-evolution is free software: you can redistribute it "+AND+"/or modify
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
 * with logisim-evolution. If "+NOT+", see <http://www.gnu.org/licenses/>.
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
import com.cburch.logisim.util.LineBuffer;
import java.util.ArrayList;
import java.util.SortedMap;
import java.util.TreeMap;

public class Ttl7442HDLGenerator extends AbstractHDLGeneratorFactory {

  private boolean IsExes3 = false;
  private boolean IsGray = false;

  public Ttl7442HDLGenerator() {
    super();
  }

  public Ttl7442HDLGenerator(boolean Exess3, boolean Gray) {
    super();
    IsExes3 = Exess3;
    IsGray = Gray;
  }

  @Override
  public String getComponentStringIdentifier() {
    return "TTL";
  }

  @Override
  public SortedMap<String, Integer> GetInputList(Netlist TheNetlist, AttributeSet attrs) {
    final var map = new TreeMap<String, Integer>();
    map.put("A", 1);
    map.put("B", 1);
    map.put("C", 1);
    map.put("D", 1);
    return map;
  }

  @Override
  public SortedMap<String, Integer> GetOutputList(Netlist TheNetlist, AttributeSet attrs) {
    final var map = new TreeMap<String, Integer>();
    map.put("O0", 1);
    map.put("O1", 1);
    map.put("O2", 1);
    map.put("O3", 1);
    map.put("O4", 1);
    map.put("O5", 1);
    map.put("O6", 1);
    map.put("O7", 1);
    map.put("O8", 1);
    map.put("O9", 1);
    return map;
  }

  @Override
  public ArrayList<String> GetModuleFunctionality(Netlist nets, AttributeSet attrs) {
    final var contents = (new LineBuffer()).addHdlPairs();

    if (IsExes3) {
      contents.addLines(
          "{{assign}} O0 {{=}} {{not}}( {{not}}(D) {{and}} {{not}}(C) {{and}} B {{and}} A );",
          "{{assign}} O1 {{=}} {{not}}( {{not}}(D) {{and}} C {{and}} {{not}}(B) {{and}} {{not}}(A) );",
          "{{assign}} O2 {{=}} {{not}}( {{not}}(D) {{and}} C {{and}} {{not}}(B) {{and}} A );",
          "{{assign}} O3 {{=}} {{not}}( {{not}}(D) {{and}} C {{and}} B {{and}} {{not}}(A) );",
          "{{assign}} O4 {{=}} {{not}}( {{not}}(D) {{and}} C {{and}} B {{and}} A );",
          "{{assign}} O5 {{=}} {{not}}( D {{and}} {{not}}(C) {{and}} {{not}}(B) {{and}} {{not}}(A) );",
          "{{assign}} O6 {{=}} {{not}}( D {{and}} {{not}}(C) {{and}} {{not}}(B) {{and}} A );",
          "{{assign}} O7 {{=}} {{not}}( D {{and}} {{not}}(C) {{and}} B {{and}} {{not}}(A) );",
          "{{assign}} O8 {{=}} {{not}}( D {{and}} {{not}}(C) {{and}} B {{and}} A );",
          "{{assign}} O9 {{=}} {{not}}( D {{and}} C {{and}} {{not}}(B) {{and}} {{not}}(A) );");
    } else if (IsGray) {
      contents.addLines(
          "{{assign}} O0 {{=}} {{not}}( {{not}}(D) {{and}} {{not}}(C) {{and}} B {{and}} {{not}}(A) );",
          "{{assign}} O1 {{=}} {{not}}( {{not}}(D) {{and}} C {{and}} B {{and}} {{not}}(A) );",
          "{{assign}} O2 {{=}} {{not}}( {{not}}(D) {{and}} C {{and}} B {{and}} A );",
          "{{assign}} O3 {{=}} {{not}}( {{not}}(D) {{and}} C {{and}} {{not}}(B) {{and}} A );",
          "{{assign}} O4 {{=}} {{not}}( {{not}}(D) {{and}} C {{and}} {{not}}(B) {{and}} {{not}(A) );",
          "{{assign}} O5 {{=}} {{not}}( D {{and}} C {{and}} {{not}}(B) {{and}} {{not}}(A) );",
          "{{assign}} O6 {{=}} {{not}}( D {{and}} C {{and}} {{not}}(B) {{and}} A );",
          "{{assign}} O7 {{=}} {{not}}( D {{and}} C {{and}} B {{and}} A );",
          "{{assign}} O8 {{=}} {{not}}( D {{and}} C {{and}} B {{and}} {{not}}(A) );",
          "{{assign}} O9 {{=}} {{not}}( D {{and}} {{not}}(C) {{and}} B {{and}} {not}}(A) );");
    } else {
      contents.addLines(
          "{{assign}} O0 {{=}} {{not}}( {{not}}(D) {{and}} {{not}}(C) {{and}} {{not}}(B) {{and}} {{not}}(A) );",
          "{{assign}} O1 {{=}} {{not}}( {{not}}(D) {{and}} {{not}}(C) {{and}} {{not}}(B) {{and}} A );",
          "{{assign}} O2 {{=}} {{not}}( {{not}}(D) {{and}} {{not}}(C) {{and}} B {{and}} {{not}}(A) );",
          "{{assign}} O3 {{=}} {{not}}( {{not}}(D) {{and}} {{not}}(C) {{and}} B {{and}} A );",
          "{{assign}} O4 {{=}} {{not}}( {{not}}(D) {{and}} C {{and}} {{not}}(B) {{and}} {{not}}(A) );",
          "{{assign}} O5 {{=}} {{not}}( {{not}}(D) {{and}} C {{and}} {{not}}(B) {{and}} A );",
          "{{assign}} O6 {{=}} {{not}}( {{not}}(D) {{and}} C {{and}} B {{and}} {{not}}(A) );",
          "{{assign}} O7 {{=}} {{not}}( {{not}}(D) {{and}} C {{and}} B {{and}} A );",
          "{{assign}} O8 {{=}} {{not}}( D {{and}} {{not}}(C) {{and}} {{not}}(B) {{and}} {{not}}(A) );",
          "{{assign}} O9 {{=}} {{not}}( D {{and}} {{not}}(C) {{and}} {{not}}(B) {{and}} A );");
    }
    return contents.get();
  }

  @Override
  public SortedMap<String, String> GetPortMap(Netlist nets, Object mapInfo) {
    final var map = new TreeMap<String, String>();
    if (!(mapInfo instanceof NetlistComponent)) return map;
    final var comp = (NetlistComponent) mapInfo;
    map.putAll(GetNetMap("A", true, comp, 13, nets));
    map.putAll(GetNetMap("B", true, comp, 12, nets));
    map.putAll(GetNetMap("C", true, comp, 11, nets));
    map.putAll(GetNetMap("D", true, comp, 10, nets));
    map.putAll(GetNetMap("O0", true, comp, 0, nets));
    map.putAll(GetNetMap("O1", true, comp, 1, nets));
    map.putAll(GetNetMap("O2", true, comp, 2, nets));
    map.putAll(GetNetMap("O3", true, comp, 3, nets));
    map.putAll(GetNetMap("O4", true, comp, 4, nets));
    map.putAll(GetNetMap("O5", true, comp, 5, nets));
    map.putAll(GetNetMap("O6", true, comp, 6, nets));
    map.putAll(GetNetMap("O7", true, comp, 7, nets));
    map.putAll(GetNetMap("O8", true, comp, 8, nets));
    map.putAll(GetNetMap("O9", true, comp, 9, nets));
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
    return (!attrs.getValue(TtlLibrary.VCC_GND));
  }
}
