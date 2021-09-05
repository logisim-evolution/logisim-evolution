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

package com.cburch.logisim.std.gates;

import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.fpga.designrulecheck.Netlist;
import com.cburch.logisim.fpga.designrulecheck.NetlistComponent;
import com.cburch.logisim.fpga.hdlgenerator.AbstractHDLGeneratorFactory;
import com.cburch.logisim.fpga.hdlgenerator.HDL;
import com.cburch.logisim.util.LineBuffer;
import java.util.ArrayList;
import java.util.SortedMap;
import java.util.TreeMap;

public class PLAHDLGeneratorFactory extends AbstractHDLGeneratorFactory {

  @Override
  public String getComponentStringIdentifier() {
    return "PLA_COMPONENT";
  }

  @Override
  public SortedMap<String, Integer> GetInputList(Netlist nets, AttributeSet attrs) {
    final var inputs = new TreeMap<String, Integer>();
    inputs.put("Index", attrs.getValue(PLA.ATTR_IN_WIDTH).getWidth());
    return inputs;
  }

  private static String bits(char[] b) {
    final var s = new StringBuilder();
    for (final var c : b) s.insert(0, ((c == '0' || c == '1') ? c : '-'));
    if (b.length == 1) return "'" + s + "'";
    else return "\"" + s + "\"";
  }

  private static String zeros(int sz) {
    final var s = new StringBuilder();
    s.append("0".repeat(sz));
    if (sz == 1) return "'" + s + "'";
    else return "\"" + s + "\"";
  }

  @Override
  public ArrayList<String> GetModuleFunctionality(Netlist nets, AttributeSet attrs) {
    final var contents = new LineBuffer();
    final var tt = attrs.getValue(PLA.ATTR_TABLE);
    final var outSz = attrs.getValue(PLA.ATTR_OUT_WIDTH).getWidth();
    if (HDL.isVHDL()) {
      var leader = "    Result <= ";
      final var indent = "              ";
      if (tt.rows().isEmpty()) {
        contents.add("{{1}}{{2}};", leader, zeros(outSz));
      } else {
        for (PLATable.Row r : tt.rows()) {
          contents.add("{{1}}{{2}} WHEN std_match(Index, {{3}}) ELSE", leader, bits(r.outBits), bits(r.inBits));
          leader = indent;
        }
        contents.add("{{1}}{{2}};", leader, zeros(outSz));
      }
    } else {
      // TODO
    }
    return contents.get();
  }

  @Override
  public SortedMap<String, Integer> GetOutputList(Netlist nets, AttributeSet attrs) {
    final var outputs = new TreeMap<String, Integer>();
    outputs.put("Result", attrs.getValue(PLA.ATTR_OUT_WIDTH).getWidth());
    return outputs;
  }

  @Override
  public SortedMap<String, String> GetPortMap(Netlist nets, Object mapInfo) {
    final var portMap = new TreeMap<String, String>();
    if (!(mapInfo instanceof NetlistComponent)) return portMap;
    final var componentInfo = (NetlistComponent) mapInfo;
    portMap.putAll(GetNetMap("Index", true, componentInfo, PLA.IN_PORT, nets));
    portMap.putAll(GetNetMap("Result", true, componentInfo, PLA.OUT_PORT, nets));
    return portMap;
  }

  @Override
  public String GetSubDir() {
    return "gates";
  }

  @Override
  public boolean HDLTargetSupported(AttributeSet attrs) {
    return HDL.isVHDL();
  }
}
