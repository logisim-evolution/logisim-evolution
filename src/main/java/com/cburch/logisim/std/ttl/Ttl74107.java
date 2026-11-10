/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.std.ttl;

import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.fpga.designrulecheck.netlistComponent;
import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.instance.InstanceState;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.util.GraphicsUtil;
import java.awt.Color;
import java.awt.Graphics;

/**
 * TTL 74x107: dual negative-edge J-K flip-flop with reset.
 *
 * <p>Simulation follows the
 * <a href="https://assets.nexperia.com/documents/data-sheet/74HC_HCT107.pdf">74HC107</a> and
 * <a href="https://www.ti.com/lit/ds/symlink/cd74hc107.pdf">CD74HC107</a> data sheets. Each half
 * has J, K, a high-to-low clock and an asynchronous active-low reset. There is no preset. Reset
 * forces Q low and its complement high. While reset is high, a falling clock toggles when J and K
 * are high, sets when only J is high, clears when only K is high, and holds when both are low. A
 * rising edge does not change the state. The two halves are independent. Nanosecond delays are
 * not modeled.
 */
public class Ttl74107 extends AbstractTtlGate {
  /**
   * Unique identifier of the tool, used as reference in project files. Do NOT change as it will
   * prevent project files from loading.
   *
   * <p>Identifier value must MUST be unique string among all tools.
   */
  public static final String _ID = "74107";

  public static final int PORT_1J = 0;
  public static final int PORT_N1Q = 1;
  public static final int PORT_1Q = 2;
  public static final int PORT_1K = 3;
  public static final int PORT_2Q = 4;
  public static final int PORT_N2Q = 5;
  public static final int PORT_2J = 6;
  public static final int PORT_2CP = 7;
  public static final int PORT_N2R = 8;
  public static final int PORT_2K = 9;
  public static final int PORT_1CP = 10;
  public static final int PORT_N1R = 11;

  private static final int DELAY = 8;
  private static final BitWidth WIDTH = BitWidth.create(2);
  private static final byte[] OUTPUT_PINS = {2, 3, 5, 6};
  private static final String[] PORT_NAMES = {
    "1J", "n1Q", "1Q", "1K", "2Q", "n2Q", "2J", "2CP", "n2R", "2K", "1CP", "n1R"
  };
  private static final String[] PIN_NAMES = {
    "1J", "n1Q", "1Q", "1K", "2Q", "n2Q", null, "2J", "2CP", "n2R", "2K", "1CP", "n1R", null
  };

  /** Creates a 74107 dual negative-edge J-K flip-flop with reset. */
  public Ttl74107() {
    super(_ID, (byte) 14, OUTPUT_PINS, PORT_NAMES, new Ttl74107HdlGenerator());
  }

  @Override
  public void paintInternal(InstancePainter painter, int x, int y, int height, boolean up) {
    final var g = painter.getGraphics();
    super.paintBase(painter, false, false);
    g.setColor(Color.BLACK);
    final var rawState = painter.getData();
    final TtlRegisterData stored = rawState instanceof TtlRegisterData ? (TtlRegisterData) rawState : null;
    g.drawRect((x + 14), (y + 18), 48, 24);
    GraphicsUtil.drawCenteredText(g, "J", (x + 22), (y + 26));
    GraphicsUtil.drawCenteredText(g, "K", (x + 22), (y + 36));
    GraphicsUtil.drawCenteredText(g, "Q", (x + 54), (y + 26));
    GraphicsUtil.drawCenteredText(g, "Q", (x + 54), (y + 36));
    structureBubble(g, (x + 54), (y + 32));
    structureDot(g, (x + 38), (y + 30), stored, 0, 0);
    g.drawLine((x + 50), (y + 7), (x + 50), (y + 18));
    g.drawLine((x + 50) - 3, (y + 18), (x + 50), ((y + 18)) + 4);
    g.drawLine((x + 50) + 3, (y + 18), (x + 50), ((y + 18)) + 4);
    structureBubble(g, (x + 50), ((y + 18)) - 6);
    g.drawLine((x + 10), (y + height - 7), (x + 10), (y + height - 13));
    g.drawLine((x + 10), (y + height - 13), (x + 24), (y + height - 13));
    g.drawLine((x + 24), (y + height - 13), (x + 24), (y + 42));
    g.drawLine((x + 70), (y + height - 7), (x + 70), (y + height - 13));
    g.drawLine((x + 70), (y + height - 13), (x + 31), (y + height - 13));
    g.drawLine((x + 31), (y + height - 13), (x + 31), (y + 42));
    g.drawLine((x + 30), (y + 7), (x + 30), (y + 18));
    structureBubble(g, (x + 30), ((y + 18)) - 5);
    g.drawLine((x + 50), (y + height - 7), (x + 50), (y + 42));
    g.drawLine((x + 30), (y + height - 7), (x + 30), (y + 42));
    structureBubble(g, (x + 30), ((y + 42)) + 5);
    g.drawRect((x + 70), (y + 18), 48, 24);
    GraphicsUtil.drawCenteredText(g, "J", (x + 78), (y + 26));
    GraphicsUtil.drawCenteredText(g, "K", (x + 78), (y + 36));
    GraphicsUtil.drawCenteredText(g, "Q", (x + 110), (y + 26));
    GraphicsUtil.drawCenteredText(g, "Q", (x + 110), (y + 36));
    structureBubble(g, (x + 110), (y + 32));
    structureDot(g, (x + 94), (y + 30), stored, 0, 1);
    g.drawLine((x + 110), (y + 7), (x + 110), (y + 18));
    g.drawLine((x + 110) - 3, (y + 18), (x + 110), ((y + 18)) + 4);
    g.drawLine((x + 110) + 3, (y + 18), (x + 110), ((y + 18)) + 4);
    structureBubble(g, (x + 110), ((y + 18)) - 6);
    g.drawLine((x + 130), (y + 7), (x + 130), (y + 13));
    g.drawLine((x + 130), (y + 13), (x + 87), (y + 13));
    g.drawLine((x + 87), (y + 13), (x + 87), (y + 18));
    g.drawLine((x + 70), (y + 7), (x + 70), (y + 13));
    g.drawLine((x + 70), (y + 13), (x + 94), (y + 13));
    g.drawLine((x + 94), (y + 13), (x + 94), (y + 18));
    g.drawLine((x + 90), (y + 7), (x + 90), (y + 18));
    structureBubble(g, (x + 90), ((y + 18)) - 5);
    g.drawLine((x + 90), (y + height - 7), (x + 90), (y + 42));
    g.drawLine((x + 110), (y + height - 7), (x + 110), (y + 42));
    structureBubble(g, (x + 110), ((y + 42)) + 5);
  }

  private static TtlRegisterData getState(InstanceState state) {
    var data = (TtlRegisterData) state.getData();
    if (data == null) {
      data = new TtlRegisterData(WIDTH);
      state.setData(data);
    }
    return data;
  }

  @Override
  public void propagateTtl(InstanceState state) {
    final var data = getState(state);
    final var edge1 = data.updateClock(state.getPortValue(PORT_1CP), 0, StdAttr.TRIG_FALLING);
    final var edge2 = data.updateClock(state.getPortValue(PORT_2CP), 1, StdAttr.TRIG_FALLING);
    final var current = data.getValue();
    final var q1 =
        nextState(
            current.get(0),
            state.getPortValue(PORT_N1R),
            edge1,
            state.getPortValue(PORT_1J),
            state.getPortValue(PORT_1K));
    final var q2 =
        nextState(
            current.get(1),
            state.getPortValue(PORT_N2R),
            edge2,
            state.getPortValue(PORT_2J),
            state.getPortValue(PORT_2K));
    data.setValue(Value.create(new Value[] {q1, q2}));
    state.setPort(PORT_1Q, q1, DELAY);
    state.setPort(PORT_N1Q, complement(q1), DELAY);
    state.setPort(PORT_2Q, q2, DELAY);
    state.setPort(PORT_N2Q, complement(q2), DELAY);
  }

  /**
   * Applies asynchronous reset, then a falling-edge J-K transition. Reset wins over the clock.
   * An error on an input that can change the result makes the state error; an unknown input does
   * the same only when the two substitutions disagree.
   */
  private static Value nextState(Value current, Value reset, boolean edge, Value j, Value k) {
    if (reset == Value.FALSE) return Value.FALSE;
    if (reset == Value.ERROR) return Value.ERROR;
    if (reset != Value.TRUE) return Value.UNKNOWN;
    if (!edge) return current;
    return nextBit(current, j, k);
  }

  private static Value nextBit(Value current, Value j, Value k) {
    final var jLevels = levels(j);
    final var kLevels = levels(k);
    final var agreed = definedNext(current, jLevels[0], kLevels[0]);
    for (var jHigh : jLevels) {
      for (var kHigh : kLevels) {
        if (definedNext(current, jHigh, kHigh) != agreed) {
          return (j == Value.ERROR || k == Value.ERROR) ? Value.ERROR : Value.UNKNOWN;
        }
      }
    }
    return agreed;
  }

  private static Value definedNext(Value current, boolean jHigh, boolean kHigh) {
    if (jHigh && kHigh) return complement(current);
    if (jHigh) return Value.TRUE;
    if (kHigh) return Value.FALSE;
    return current;
  }

  /** Inverse that keeps unknown distinct from error. {@link Value#not()} maps both to error. */
  private static Value complement(Value bit) {
    if (bit == Value.TRUE) return Value.FALSE;
    if (bit == Value.FALSE) return Value.TRUE;
    if (bit == Value.ERROR) return Value.ERROR;
    return Value.UNKNOWN;
  }

  private static boolean[] levels(Value bit) {
    if (bit == Value.TRUE) return new boolean[] {true};
    if (bit == Value.FALSE) return new boolean[] {false};
    return new boolean[] {false, true};
  }

  @Override
  public boolean checkForGatedClocks(netlistComponent comp) {
    return true;
  }

  @Override
  public int[] clockPinIndex(netlistComponent comp) {
    return new int[] {PORT_1CP, PORT_2CP};
  }

  private static int structureX(int originX, int pins, int pin) {
    if (pin <= pins / 2) {
      return originX + (pin - 1) * 20 + 10;
    }
    return originX + (pins - pin) * 20 + 10;
  }

  private static int structureY(int originY, int bodyHeight, int pins, int pin) {
    if (pin <= pins / 2) {
      return originY + bodyHeight - AbstractTtlGate.PIN_HEIGHT;
    }
    return originY + AbstractTtlGate.PIN_HEIGHT;
  }

  private static void structureBubble(Graphics g, int centerX, int centerY) {
    g.drawOval(centerX - 2, centerY - 2, 4, 4);
  }

  private static void structureDot(
      Graphics g, int centerX, int centerY, TtlRegisterData data, int word, int bit) {
    if (data == null) {
      return;
    }
    final var value = data.getValue(word);
    if (bit < 0 || bit >= value.getWidth()) {
      return;
    }
    final var bitValue = value.get(bit);
    g.setColor(bitValue.getColor());
    g.fillOval(centerX - 3, centerY - 3, 6, 6);
    g.setColor(Color.WHITE);
    GraphicsUtil.drawCenteredText(g, bitValue.toDisplayString(), centerX, centerY);
    g.setColor(Color.BLACK);
  }

}
