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

public class Ttl7442 extends AbstractTtlGate {
  /**
   * Unique identifier of the tool, used as reference in project files.
   * Do NOT change as it will prevent project files from loading.
   *
   * Identifier value must MUST be unique string among all tools.
   */
  public static final String _ID = "7442";

  private boolean isExec3 = false;
  private boolean isGray = false;

  private static final byte pinCount = 14;
  private static final byte[] outPins = {1, 2, 3, 4, 5, 6, 7, 9, 10, 11};
  private static final String[] pinNames = {"O0", "O1", "O2", "O3", "O4", "O5", "O6", "O7", "O8", "O9", "D", "C", "B", "A"};

  public Ttl7442() {
    super(_ID, pinCount, outPins, pinNames, new Ttl7442HdlGenerator(false, false));
  }

  public Ttl7442(String name, int encoding) {
    super(name, pinCount, outPins, pinNames, new Ttl7442HdlGenerator(encoding == 1, encoding == 2));
    isExec3 = encoding == 1;
    isGray = encoding == 2;
  }

  @Override
  public void paintInternal(InstancePainter painter, int x, int y, int height, boolean up) {
    super.paintBase(painter, false, false);
    final var g = painter.getGraphics();
    g.drawRect(x + 18, y + 10, 84, 18);
    var mask = 1;
    for (int i = 0; i < 10; i++) {
      g.drawOval(x + 22 + i * 8, y + 28, 4, 4);
      g.drawLine(
          x + 24 + i * 8,
          y + 32,
          x + 24 + i * 8,
          y + height - AbstractTtlGate.PIN_HEIGHT - (i + 1) * 2);
      g.drawString(Integer.toString(i), x + 22 + i * 8, y + 26);
      if (i < 4) {
        g.drawString(Integer.toString(mask), x + 27 + i * 20, y + 16);
        mask <<= 1;
        g.drawLine(x + 30 + i * 20, y + AbstractTtlGate.PIN_HEIGHT, x + 30 + i * 20, y + 10);
      }
      if (i < 7) {
        g.drawLine(
            x + 10 + i * 20,
            y + height - AbstractTtlGate.PIN_HEIGHT,
            x + 10 + i * 20,
            y + height - AbstractTtlGate.PIN_HEIGHT - (i + 1) * 2);
        g.drawLine(
            x + 10 + i * 20,
            y + height - AbstractTtlGate.PIN_HEIGHT - (i + 1) * 2,
            x + 24 + i * 8,
            y + height - AbstractTtlGate.PIN_HEIGHT - (i + 1) * 2);
      } else {
        int j = i == 7 ? 9 : i == 9 ? 7 : 8;
        g.drawLine(
            x + i * 20 - 30,
            y + AbstractTtlGate.PIN_HEIGHT,
            x + i * 20 - 30,
            y + height - AbstractTtlGate.PIN_HEIGHT - (j + 1) * 2);
        g.drawLine(
            x + i * 20 - 30,
            y + height - AbstractTtlGate.PIN_HEIGHT - (j + 1) * 2,
            x + 24 + j * 8,
            y + height - AbstractTtlGate.PIN_HEIGHT - (j + 1) * 2);
      }
    }
  }

  @Override
  public void propagateTtl(InstanceState state) {
    int decode = -1;
    if (!(state.getPortValue(13).isErrorValue() | state.getPortValue(13).isUnknown())) {
      decode = state.getPortValue(13) == Value.TRUE ? 1 : 0;
      if (!(state.getPortValue(12).isErrorValue() | state.getPortValue(12).isUnknown())) {
        decode |= state.getPortValue(12) == Value.TRUE ? 2 : 0;
        if (!(state.getPortValue(11).isErrorValue() | state.getPortValue(11).isUnknown())) {
          decode |= state.getPortValue(11) == Value.TRUE ? 4 : 0;
          if (!(state.getPortValue(10).isErrorValue() | state.getPortValue(10).isUnknown())) {
            decode |= state.getPortValue(10) == Value.TRUE ? 8 : 0;
          } else decode = -1;
        } else decode = -1;
      } else decode = -1;
    }
    if (decode < 0) {
      state.setPort(0, Value.UNKNOWN, 1);
      state.setPort(1, Value.UNKNOWN, 1);
      state.setPort(2, Value.UNKNOWN, 1);
      state.setPort(3, Value.UNKNOWN, 1);
      state.setPort(4, Value.UNKNOWN, 1);
      state.setPort(5, Value.UNKNOWN, 1);
      state.setPort(6, Value.UNKNOWN, 1);
      state.setPort(7, Value.UNKNOWN, 1);
      state.setPort(8, Value.UNKNOWN, 1);
      state.setPort(9, Value.UNKNOWN, 1);
    } else if (isGray) {
      state.setPort(0, decode == 2 ? Value.FALSE : Value.TRUE, 1);
      state.setPort(1, decode == 6 ? Value.FALSE : Value.TRUE, 1);
      state.setPort(2, decode == 7 ? Value.FALSE : Value.TRUE, 1);
      state.setPort(3, decode == 5 ? Value.FALSE : Value.TRUE, 1);
      state.setPort(4, decode == 4 ? Value.FALSE : Value.TRUE, 1);
      state.setPort(5, decode == 12 ? Value.FALSE : Value.TRUE, 1);
      state.setPort(6, decode == 13 ? Value.FALSE : Value.TRUE, 1);
      state.setPort(7, decode == 15 ? Value.FALSE : Value.TRUE, 1);
      state.setPort(8, decode == 14 ? Value.FALSE : Value.TRUE, 1);
      state.setPort(9, decode == 10 ? Value.FALSE : Value.TRUE, 1);
    } else {
      if (isExec3) decode -= 3;
      state.setPort(0, decode == 0 ? Value.FALSE : Value.TRUE, 1);
      state.setPort(1, decode == 1 ? Value.FALSE : Value.TRUE, 1);
      state.setPort(2, decode == 2 ? Value.FALSE : Value.TRUE, 1);
      state.setPort(3, decode == 3 ? Value.FALSE : Value.TRUE, 1);
      state.setPort(4, decode == 4 ? Value.FALSE : Value.TRUE, 1);
      state.setPort(5, decode == 5 ? Value.FALSE : Value.TRUE, 1);
      state.setPort(6, decode == 6 ? Value.FALSE : Value.TRUE, 1);
      state.setPort(7, decode == 7 ? Value.FALSE : Value.TRUE, 1);
      state.setPort(8, decode == 8 ? Value.FALSE : Value.TRUE, 1);
      state.setPort(9, decode == 9 ? Value.FALSE : Value.TRUE, 1);
    }
  }
}
