/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.std.ttl;

import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.instance.InstanceState;
import com.cburch.logisim.util.LineBuffer;

/** TTL 74x00: quad 2-input NAND gate */
public class Ttl7400 extends AbstractTtlGate {
  /**
   * Unique identifier of the tool, used as reference in project files. Do NOT change as it will
   * prevent project files from loading.
   *
   * <p>Identifier value must MUST be unique string among all tools.
   */
  public static final String _ID = "7400";

  private static class NandGateHdlGeneratorFactory extends AbstractGateHdlGenerator {
    @Override
    public LineBuffer getLogicFunction(int index) {
      return LineBuffer.getHdlBuffer()
          .add("{{assign}}gateO{{1}}{{=}}{{not}}(gateA{{1}}{{and}}gateB{{1}});", index);
    }
  }

  private static final byte pinCount = 14;
  private static final byte[] outPins = {3, 6, 8, 11};

  public Ttl7400() {
    super(_ID, pinCount, outPins, true, new NandGateHdlGeneratorFactory());
  }

  public Ttl7400(String name) {
    super(name, pinCount, outPins, true, new NandGateHdlGeneratorFactory());
  }

  @Override
  public void paintInternal(InstancePainter painter, int x, int y, int height, boolean up) {
    final var g = painter.getGraphics();
    final var portwidth = 19;
    final var portheight = 15;
    final var youtput = y + (up ? 20 : 40);
    Drawgates.paintAnd(g, x + 40, youtput, portwidth - 4, portheight, true);
    // output line
    Drawgates.paintOutputgate(g, x + 50, y, x + 44, youtput, up, height);
    // input lines
    Drawgates.paintDoubleInputgate(
        g, x + 30, y, x + 44 - portwidth, youtput, portheight, up, false, height);
  }

  @Override
  public void propagateTtl(InstanceState state) {
    for (var i = 2; i < 6; i += 3) {
      state.setPort(i, (state.getPortValue(i - 1).and(state.getPortValue(i - 2)).not()), 1);
    }
    for (var i = 6; i < 12; i += 3) {
      state.setPort(i, (state.getPortValue(i + 1).and(state.getPortValue(i + 2)).not()), 1);
    }
  }
}
