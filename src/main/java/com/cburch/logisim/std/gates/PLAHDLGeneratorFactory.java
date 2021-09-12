/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
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
  public boolean isHDLSupportedTarget(AttributeSet attrs) {
    return HDL.isVHDL();
  }
}
