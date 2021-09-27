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
import com.cburch.logisim.fpga.hdlgenerator.AbstractHdlGeneratorFactory;
import com.cburch.logisim.fpga.hdlgenerator.Hdl;
import com.cburch.logisim.instance.Port;
import com.cburch.logisim.util.LineBuffer;
import java.util.ArrayList;

public class PLAHDLGeneratorFactory extends AbstractHdlGeneratorFactory {

  public PLAHDLGeneratorFactory() {
    super();
    myPorts
        .add(Port.INPUT, "index", 0, PLA.IN_PORT, PLA.ATTR_IN_WIDTH)
        .add(Port.OUTPUT, "Result", 0, PLA.OUT_PORT, PLA.ATTR_OUT_WIDTH);
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
  public ArrayList<String> getModuleFunctionality(Netlist nets, AttributeSet attrs) {
    final var contents = LineBuffer.getHdlBuffer();
    final var tt = attrs.getValue(PLA.ATTR_TABLE);
    final var outSz = attrs.getValue(PLA.ATTR_OUT_WIDTH).getWidth();
    if (Hdl.isVhdl()) {
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
  public boolean isHdlSupportedTarget(AttributeSet attrs) {
    return Hdl.isVhdl();
  }
}
