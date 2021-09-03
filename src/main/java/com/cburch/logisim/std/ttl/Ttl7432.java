/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.std.ttl;

import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.fpga.hdlgenerator.HDL;
import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.instance.InstanceState;
import java.awt.Graphics;
import java.util.ArrayList;

public class Ttl7432 extends AbstractTtlGate {
  /**
   * Unique identifier of the tool, used as reference in project files.
   * Do NOT change as it will prevent project files from loading.
   *
   * Identifier value must MUST be unique string among all tools.
   */
  public static final String _ID = "7432";

  private static class OrGateHDLGeneratorFactory extends AbstractGateHDLGenerator {

    @Override
    public String getComponentStringIdentifier() {
      return "TTL7400";
    }

    @Override
    public ArrayList<String> GetLogicFunction(int index) {
      final var contents = new ArrayList<String>();
      contents.add("   " + HDL.assignPreamble() + "gate_" + index + "_O" + HDL.assignOperator()
              + "gate_" + index + "_A" + HDL.orOperator() + "gate_" + index + "B;");
      contents.add("");
      return contents;
    }
  }

  public Ttl7432() {
    super(_ID, (byte) 14, new byte[] {3, 6, 8, 11}, true);
  }

  @Override
  public void paintInternal(InstancePainter painter, int x, int y, int height, boolean up) {
    final var g = painter.getGraphics();
    final var portwidth = 14;
    final var portheight = 15;
    final var youtput = y + (up ? 20 : 40);
    Drawgates.paintOr(g, x + 40, youtput, portwidth, portheight, false, false);
    // output line
    Drawgates.paintOutputgate(g, x + 50, y, x + 40, youtput, up, height);
    // input lines
    Drawgates.paintDoubleInputgate(
        g, x + 30, y, x + 40 - portwidth, youtput, portheight, up, false, height);
  }

  @Override
  public void ttlpropagate(InstanceState state) {
    for (byte i = 2; i < 6; i += 3) {
      state.setPort(i, state.getPortValue(i - 1).or(state.getPortValue(i - 2)), 1);
    }
    for (byte i = 6; i < 12; i += 3) {
      state.setPort(i, state.getPortValue(i + 1).or(state.getPortValue(i + 2)), 1);
    }
  }

  @Override
  public boolean HDLSupportedComponent(AttributeSet attrs) {
    if (MyHDLGenerator == null) MyHDLGenerator = new OrGateHDLGeneratorFactory();
    return MyHDLGenerator.HDLTargetSupported(attrs);
  }
}
