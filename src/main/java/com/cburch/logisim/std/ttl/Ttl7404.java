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

/**
 * TTL 74x04: hex inverter gate
 */
public class Ttl7404 extends AbstractTtlGate {
  /**
   * Unique identifier of the tool, used as reference in project files.
   * Do NOT change as it will prevent project files from loading.
   *
   * Identifier value must MUST be unique string among all tools.
   */
  public static final String _ID = "7404";

  private static class NotGateHdlGeneratorFactory extends AbstractGateHdlGenerator {

    public NotGateHdlGeneratorFactory() {
      super(true);
    }

    @Override
    public LineBuffer getLogicFunction(int index) {
      return LineBuffer.getHdlBuffer().add("{{assign}}gateO{{1}}{{assign}}{{not}}(gateA{{1}});", index);
    }
  }

  private static final byte portCount = 14;
  private static final byte[] outPorts = {2, 4, 6, 8, 10, 12};

  public Ttl7404() {
    super(_ID, portCount, outPorts, true, new NotGateHdlGeneratorFactory());
  }

  public Ttl7404(String name) {
    super(name, portCount, outPorts, true, new NotGateHdlGeneratorFactory());
  }

  @Override
  public void paintInternal(InstancePainter painter, int x, int y, int height, boolean up) {
    final var g = painter.getGraphics();
    final var portwidth = 12;
    final var portheight = 6;
    final var youtput = y + (up ? 20 : 40);
    Drawgates.paintNot(g, x + 26, youtput, portwidth, portheight);
    Drawgates.paintOutputgate(g, x + 30, y, x + 26, youtput, up, height);
    Drawgates.paintSingleInputgate(g, x + 10, y, x + 26 - portwidth, youtput, up, height);
  }

  @Override
  public void propagateTtl(InstanceState state) {
    for (byte i = 1; i < 6; i += 2) {
      state.setPort(i, state.getPortValue(i - 1).not(), 1);
    }
    for (byte i = 6; i < 12; i += 2) {
      state.setPort(i, state.getPortValue(i + 1).not(), 1);
    }
  }
}
