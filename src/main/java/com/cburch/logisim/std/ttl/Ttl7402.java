/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.std.ttl;

import com.cburch.logisim.fpga.hdlgenerator.HDL;
import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.instance.InstanceState;
import java.util.ArrayList;

public class Ttl7402 extends AbstractTtlGate {
  /**
   * Unique identifier of the tool, used as reference in project files.
   * Do NOT change as it will prevent project files from loading.
   *
   * Identifier value must MUST be unique string among all tools.
   */
  public static final String _ID = "7402";

  private static class NorGateHDLGeneratorFactory extends AbstractGateHDLGenerator {

    public NorGateHDLGeneratorFactory() {
      super("TTL7402");
    }

    @Override
    public ArrayList<String> GetLogicFunction(int index) {
      final var contents = new ArrayList<String>();
      contents.add("   " + HDL.assignPreamble() + "gate_" + index + "_O" + HDL.assignOperator()
              + HDL.notOperator() + "(gate_" + index + "_A" + HDL.orOperator() + "gate_" + index + "B);");
      contents.add("");
      return contents;
    }
  }

  private static final byte portCount = 14;
  private static final byte[] outPorts = {1, 4, 10, 13};

  public Ttl7402() {
    super(_ID, portCount, outPorts, true, new NorGateHDLGeneratorFactory());
  }

  public Ttl7402(String name) {
    super(name, portCount, outPorts, true, new NorGateHDLGeneratorFactory());
  }

  @Override
  public void paintInternal(InstancePainter painter, int x, int y, int height, boolean up) {
    final var g = painter.getGraphics();
    final var portwidth = 18;
    final var portheight = 15;
    final var youtput = y + (up ? 20 : 40);
    Drawgates.paintOr(g, x + 20, youtput, portwidth - 4, portheight, true, true);
    // output line
    Drawgates.paintOutputgate(g, x + 10, y, x + 16, youtput, up, height);
    // input lines
    Drawgates.paintDoubleInputgate(
        g, x + 50, y, x + 16 + portwidth, youtput, portheight, up, true, height);
  }

  @Override
  public void ttlpropagate(InstanceState state) {
    for (byte i = 0; i < 6; i += 3) {
      state.setPort(i, (state.getPortValue(i + 1).or(state.getPortValue(i + 2)).not()), 1);
    }
    for (byte i = 8; i < 12; i += 3) {
      state.setPort(i, (state.getPortValue(i - 1).or(state.getPortValue(i - 2)).not()), 1);
    }
  }
}
