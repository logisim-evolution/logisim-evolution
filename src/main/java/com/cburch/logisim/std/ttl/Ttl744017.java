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
import java.awt.Font;
import java.awt.Graphics;

/**
 * TTL 74x4017: Johnson decade counter with 10 decoded outputs.
 *
 * <p>Simulation follows the
 * <a href="https://assets.nexperia.com/documents/data-sheet/74HC_HCT4017.pdf">Nexperia 74HC4017</a>
 * and <a href="https://www.ti.com/lit/ds/symlink/cd74hc4017.pdf">TI CD74HC4017</a> function tables.
 * The counter advances on the low-to-high transition of {@code CP0} while {@code CP1} is low, or on
 * the high-to-low transition of {@code CP1} while {@code CP0} is high. A high {@code MR}
 * asynchronously selects decoded output 0 ({@code Q0} and {@code Q5-9} high, {@code Q1} to
 * {@code Q9} low). {@code Q5-9} is high for counts 0 to 4 and low for counts 5 to 9. Nexperia calls
 * that pin {@code Q5-9}; TI calls the same pin terminal count. Nanosecond delays are not modeled.
 * Illegal Johnson codes cannot be entered from the pins, so the internal correction circuit is not
 * simulated.
 */
public class Ttl744017 extends AbstractTtlGate {
  /**
   * Unique identifier of the tool, used as reference in project files. Do NOT change as it will
   * prevent project files from loading.
   *
   * <p>Identifier value must MUST be unique string among all tools.
   */
  public static final String _ID = "744017";

  public static final int PORT_INDEX_Q5 = 0;
  public static final int PORT_INDEX_Q1 = 1;
  public static final int PORT_INDEX_Q0 = 2;
  public static final int PORT_INDEX_Q2 = 3;
  public static final int PORT_INDEX_Q6 = 4;
  public static final int PORT_INDEX_Q7 = 5;
  public static final int PORT_INDEX_Q3 = 6;
  public static final int PORT_INDEX_Q8 = 7;
  public static final int PORT_INDEX_Q4 = 8;
  public static final int PORT_INDEX_Q9 = 9;
  public static final int PORT_INDEX_Q5_9 = 10;
  public static final int PORT_INDEX_CP1 = 11;
  public static final int PORT_INDEX_CP0 = 12;
  public static final int PORT_INDEX_MR = 13;

  private static final int DELAY = 4;
  private static final int DECADE = 10;
  private static final BitWidth WIDTH = BitWidth.create(4);
  private static final byte[] OUTPUT_PORTS = {1, 2, 3, 4, 5, 6, 7, 9, 10, 11, 12};
  private static final String[] PORT_NAMES = {
    "Q5",
    "Q1",
    "Q0",
    "Q2",
    "Q6",
    "Q7",
    "Q3",
    "Q8",
    "Q4",
    "Q9",
    "Q5-9 (carry, active low)",
    "CP1 (clock, high-to-low)",
    "CP0 (clock, low-to-high)",
    "MR (master reset, active high)"
  };
  /** Decoded outputs Q0 to Q9, in count order. */
  private static final int[] DECODED_PORTS = {
    PORT_INDEX_Q0,
    PORT_INDEX_Q1,
    PORT_INDEX_Q2,
    PORT_INDEX_Q3,
    PORT_INDEX_Q4,
    PORT_INDEX_Q5,
    PORT_INDEX_Q6,
    PORT_INDEX_Q7,
    PORT_INDEX_Q8,
    PORT_INDEX_Q9
  };

  /** Creates a 744017 Johnson decade counter. */
  public Ttl744017() {
    super(_ID, (byte) 16, OUTPUT_PORTS, PORT_NAMES, null);
  }

  @Override
  public void paintInternal(InstancePainter painter, int x, int y, int height, boolean up) {
    final var g = painter.getGraphics();
    super.paintBase(painter, false, false);
    g.setColor(Color.BLACK);
    final var previousFont = g.getFont();
    g.setFont(new Font(Font.DIALOG, Font.PLAIN, 8));
    g.drawRect(x + 5, y + 28, 10, 10);
    g.drawRect(x + 25, y + 28, 10, 10);
    g.drawRect(x + 45, y + 28, 10, 10);
    g.drawRect(x + 65, y + 28, 10, 10);
    g.drawRect(x + 85, y + 28, 10, 10);
    g.drawRect(x + 105, y + 28, 10, 10);
    g.drawRect(x + 125, y + 28, 10, 10);
    g.drawRect(x + 145, y + 12, 10, 10);
    g.drawRect(x + 125, y + 12, 10, 10);
    g.drawRect(x + 105, y + 12, 10, 10);
    g.drawRect(x + 85, y + 12, 10, 10);
    g.drawLine(x + 10, y + height - 22, x + 10, y + height - 7);
    g.drawLine(x + 30, y + height - 22, x + 30, y + height - 7);
    g.drawLine(x + 50, y + height - 22, x + 50, y + height - 7);
    g.drawLine(x + 70, y + height - 22, x + 70, y + height - 7);
    g.drawLine(x + 90, y + height - 22, x + 90, y + height - 7);
    g.drawLine(x + 110, y + height - 22, x + 110, y + height - 7);
    g.drawLine(x + 130, y + height - 22, x + 130, y + height - 7);
    g.drawLine(x + 150, y + 12, x + 150, y + 7);
    g.drawLine(x + 130, y + 12, x + 130, y + 7);
    g.drawLine(x + 110, y + 12, x + 110, y + 7);
    g.drawLine(x + 90, y + 12, x + 90, y + 7);
    g.drawLine(x + 5, y + 24, x + 155, y + 24);
    g.drawLine(x + 70, y + 7, x + 70, y + 9);
    g.drawLine(x + 70, y + 9, x + 1, y + 9);
    g.drawLine(x + 1, y + 9, x + 1, y + 24);
    g.drawLine(x + 1, y + 24, x + 5, y + 24);
    g.drawLine(x + 7, y + 24, x + 7, y + 28);
    g.drawLine(x + 27, y + 24, x + 27, y + 28);
    g.drawLine(x + 47, y + 24, x + 47, y + 28);
    g.drawLine(x + 67, y + 24, x + 67, y + 28);
    g.drawLine(x + 87, y + 24, x + 87, y + 28);
    g.drawLine(x + 107, y + 24, x + 107, y + 28);
    g.drawLine(x + 127, y + 24, x + 127, y + 28);
    g.drawLine(x + 147, y + 24, x + 147, y + 22);
    g.drawLine(x + 127, y + 24, x + 127, y + 22);
    g.drawLine(x + 107, y + 24, x + 107, y + 22);
    g.drawLine(x + 87, y + 24, x + 87, y + 22);
    g.drawLine(x + 5, y + 25, x + 155, y + 25);
    g.drawLine(x + 50, y + 7, x + 50, y + 9);
    g.drawLine(x + 50, y + 9, x + 1, y + 9);
    g.drawLine(x + 1, y + 9, x + 1, y + 25);
    g.drawLine(x + 1, y + 25, x + 5, y + 25);
    g.drawLine(x + 10, y + 25, x + 10, y + 28);
    g.drawLine(x + 30, y + 25, x + 30, y + 28);
    g.drawLine(x + 50, y + 25, x + 50, y + 28);
    g.drawLine(x + 70, y + 25, x + 70, y + 28);
    g.drawLine(x + 90, y + 25, x + 90, y + 28);
    g.drawLine(x + 110, y + 25, x + 110, y + 28);
    g.drawLine(x + 130, y + 25, x + 130, y + 28);
    g.drawLine(x + 150, y + 25, x + 150, y + 22);
    g.drawLine(x + 130, y + 25, x + 130, y + 22);
    g.drawLine(x + 110, y + 25, x + 110, y + 22);
    g.drawLine(x + 90, y + 25, x + 90, y + 22);
    g.drawLine(x + 5, y + 25, x + 155, y + 25);
    g.drawLine(x + 30, y + 7, x + 30, y + 9);
    g.drawLine(x + 30, y + 9, x + 1, y + 9);
    g.drawLine(x + 1, y + 9, x + 1, y + 25);
    g.drawLine(x + 1, y + 25, x + 5, y + 25);
    g.drawLine(x + 13, y + 25, x + 13, y + 28);
    g.drawLine(x + 33, y + 25, x + 33, y + 28);
    g.drawLine(x + 53, y + 25, x + 53, y + 28);
    g.drawLine(x + 73, y + 25, x + 73, y + 28);
    g.drawLine(x + 93, y + 25, x + 93, y + 28);
    g.drawLine(x + 113, y + 25, x + 113, y + 28);
    g.drawLine(x + 133, y + 25, x + 133, y + 28);
    g.drawLine(x + 153, y + 25, x + 153, y + 22);
    g.drawLine(x + 133, y + 25, x + 133, y + 22);
    g.drawLine(x + 113, y + 25, x + 113, y + 22);
    g.drawLine(x + 93, y + 25, x + 93, y + 22);
    g.drawOval(x + 88, y + 8, 4, 4);
    GraphicsUtil.drawCenteredText(g, "5", x + 10, y + height - 27);
    GraphicsUtil.drawCenteredText(g, "1", x + 30, y + height - 27);
    GraphicsUtil.drawCenteredText(g, "0", x + 50, y + height - 27);
    GraphicsUtil.drawCenteredText(g, "2", x + 70, y + height - 27);
    GraphicsUtil.drawCenteredText(g, "6", x + 90, y + height - 27);
    GraphicsUtil.drawCenteredText(g, "7", x + 110, y + height - 27);
    GraphicsUtil.drawCenteredText(g, "3", x + 130, y + height - 27);
    GraphicsUtil.drawCenteredText(g, "8", x + 150, y + 17);
    GraphicsUtil.drawCenteredText(g, "4", x + 130, y + 17);
    GraphicsUtil.drawCenteredText(g, "9", x + 110, y + 17);
    GraphicsUtil.drawCenteredText(g, "9", x + 90, y + 17);
    g.setFont(previousFont);
  }



  private static Color stateColor(Value value) {
    if (containsBit(value, Value.ERROR)) return Value.ERROR.getColor();
    if (!value.isFullyDefined()) return Value.UNKNOWN.getColor();
    return Value.TRUE.getColor();
  }

  private static TtlRegisterData getStateData(InstanceState state) {
    var data = (TtlRegisterData) state.getData();
    if (data == null) {
      data = new TtlRegisterData(WIDTH);
      state.setData(data);
    }
    return data;
  }

  @Override
  public void propagateTtl(InstanceState state) {
    final var data = getStateData(state);
    final var cp0 = state.getPortValue(PORT_INDEX_CP0);
    final var cp1 = state.getPortValue(PORT_INDEX_CP1);
    final var cp0Rising = data.updateClock(cp0, 0, StdAttr.TRIG_RISING);
    final var cp1Falling = data.updateClock(cp1, 1, StdAttr.TRIG_FALLING);
    if (state.getPortValue(PORT_INDEX_MR) == Value.TRUE) {
      data.setValue(Value.createKnown(WIDTH, 0));
    } else if (cp0Rising && cp1 == Value.FALSE) {
      data.setValue(nextCount(data.getValue()));
    } else if (cp1Falling && cp0 == Value.TRUE) {
      data.setValue(nextCount(data.getValue()));
    }
    publish(state, data.getValue());
  }

  /**
   * Advances one decade count. An error anywhere makes the next count entirely error. A count that
   * is only unknown, or a defined value outside 0 to 9, stays unknown. A legal count increments
   * modulo 10.
   */
  private static Value nextCount(Value current) {
    if (containsBit(current, Value.ERROR)) return Value.createError(WIDTH);
    if (!current.isFullyDefined()) return Value.createUnknown(WIDTH);
    final var count = current.toLongValue();
    if (count > 9) return Value.createUnknown(WIDTH);
    return Value.createKnown(WIDTH, (count + 1) % DECADE);
  }

  private void publish(InstanceState state, Value count) {
    if (containsBit(count, Value.ERROR) || !count.isFullyDefined() || count.toLongValue() > 9) {
      final var bit = containsBit(count, Value.ERROR) ? Value.ERROR : Value.UNKNOWN;
      for (final var port : DECODED_PORTS) {
        state.setPort(port, bit, DELAY);
      }
      state.setPort(PORT_INDEX_Q5_9, bit, DELAY);
      return;
    }
    final var step = (int) count.toLongValue();
    for (var output = 0; output < DECODED_PORTS.length; output++) {
      state.setPort(DECODED_PORTS[output], output == step ? Value.TRUE : Value.FALSE, DELAY);
    }
    state.setPort(PORT_INDEX_Q5_9, step < 5 ? Value.TRUE : Value.FALSE, DELAY);
  }

  private static boolean containsBit(Value word, Value bit) {
    for (var i = 0; i < word.getWidth(); i++) {
      if (word.get(i) == bit) return true;
    }
    return false;
  }

  @Override
  public boolean checkForGatedClocks(netlistComponent comp) {
    return true;
  }

  @Override
  public int[] clockPinIndex(netlistComponent comp) {
    return new int[] {PORT_INDEX_CP0, PORT_INDEX_CP1};
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
