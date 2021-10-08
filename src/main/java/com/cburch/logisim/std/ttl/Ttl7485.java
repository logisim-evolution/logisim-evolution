/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.std.ttl;

import com.cburch.logisim.data.Value;
import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.instance.InstanceState;

public class Ttl7485 extends AbstractTtlGate {
  /**
   * Unique identifier of the tool, used as reference in project files.
   * Do NOT change as it will prevent project files from loading.
   *
   * Identifier value must MUST be unique string among all tools.
   */
  public static final String _ID = "7485";

  public Ttl7485() {
    super(
        _ID,
        (byte) 16,
        new byte[] {5, 6, 7},
        new String[] {
          "B3", "A<B", "A=B", "A>B", "A>B", "A=B", "A<B", "B0", "A0", "B1", "A1", "A2", "B2", "A3"
        },
        new Ttl7485HdlGenerator());
  }

  @Override
  public void paintInternal(InstancePainter painter, int x, int y, int height, boolean up) {
    super.paintBase(painter, true, false);
    Drawgates.paintPortNames(painter, x, y, height, super.portNames);
  }

  @Override
  public void propagateTtl(InstanceState state) {
    final var A0 = state.getPortValue(8) == Value.TRUE ? (byte) 1 : 0;
    final var A1 = state.getPortValue(10) == Value.TRUE ? (byte) 2 : 0;
    final var A2 = state.getPortValue(11) == Value.TRUE ? (byte) 4 : 0;
    final var A3 = state.getPortValue(13) == Value.TRUE ? (byte) 8 : 0;
    final var B0 = state.getPortValue(7) == Value.TRUE ? (byte) 1 : 0;
    final var B1 = state.getPortValue(9) == Value.TRUE ? (byte) 2 : 0;
    final var B2 = state.getPortValue(12) == Value.TRUE ? (byte) 4 : 0;
    final var B3 = state.getPortValue(0) == Value.TRUE ? (byte) 8 : 0;
    final var A = (byte) (A3 + A2 + A1 + A0);
    final var B = (byte) (B3 + B2 + B1 + B0);
    if (A > B) {
      state.setPort(4, Value.TRUE, 1);
      state.setPort(5, Value.FALSE, 1);
      state.setPort(6, Value.FALSE, 1);
    } else if (A < B) {
      state.setPort(4, Value.FALSE, 1);
      state.setPort(5, Value.FALSE, 1);
      state.setPort(6, Value.TRUE, 1);
    } else {
      if (state.getPortValue(2) == Value.TRUE) {
        state.setPort(4, Value.FALSE, 1);
        state.setPort(5, Value.TRUE, 1);
        state.setPort(6, Value.FALSE, 1);
      } else if (state.getPortValue(1) == Value.TRUE && state.getPortValue(3) == Value.TRUE) {
        state.setPort(4, Value.FALSE, 1);
        state.setPort(5, Value.FALSE, 1);
        state.setPort(6, Value.FALSE, 1);
      } else if (state.getPortValue(1) == Value.TRUE) {
        state.setPort(4, Value.FALSE, 1);
        state.setPort(5, Value.FALSE, 1);
        state.setPort(6, Value.TRUE, 1);
      } else if (state.getPortValue(3) == Value.TRUE) {
        state.setPort(4, Value.TRUE, 1);
        state.setPort(5, Value.FALSE, 1);
        state.setPort(6, Value.FALSE, 1);
      } else {
        state.setPort(4, Value.TRUE, 1);
        state.setPort(5, Value.FALSE, 1);
        state.setPort(6, Value.TRUE, 1);
      }
    }
  }
}
