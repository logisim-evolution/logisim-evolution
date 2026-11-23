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
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;

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

  /**
   * Wide AND under CP0 and CP1, output downward. One counter block replaces the separate counter
   * and decoder: clock and MR enter its left face, and the output bus leaves the vertical center
   * of the right face into the half-ring.
   */
  @Override
  public void paintInternal(InstancePainter painter, int x, int y, int height, boolean up) {
    final var g = painter.getGraphics();
    super.paintBase(painter, false, false);
    g.setColor(Color.BLACK);
    g.drawRect(x + 44, y + 13, 32, 16);
    g.drawRect(x + 82, y + 30, 40, 14);
    drawClockWedgeLeft(g, x + 82, y + 33);
    final var previousFont = g.getFont();
    g.setFont(new Font(Font.DIALOG, Font.PLAIN, 6));
    GraphicsUtil.drawText(
        g, "&", x + 60, y + 21, GraphicsUtil.H_CENTER, GraphicsUtil.V_CENTER_OVERALL);
    GraphicsUtil.drawText(
        g, "COUNTER", x + 102, y + 37, GraphicsUtil.H_CENTER, GraphicsUtil.V_CENTER_OVERALL);
    g.setFont(previousFont);

    wire(g, x, y, 50, 7, 50, 13);
    wire(g, x, y, 70, 7, 70, 11);
    drawBubble(g, x + 70, y + 12);
    wire(g, x, y, 60, 29, 60, 33, 82, 33);
    wire(g, x, y, 30, 7, 30, 41, 82, 41);

    bus(g, x, y, 122, 37, 132, 37, 132, 48, 10, 48);
    bus(g, x, y, 132, 37, 132, 14, 90, 14);
    bus(g, x, y, 132, 14, 150, 14);

    wire(g, x, y, 10, 48, 10, 53);
    wire(g, x, y, 30, 48, 30, 53);
    wire(g, x, y, 50, 48, 50, 53);
    wire(g, x, y, 70, 48, 70, 53);
    wire(g, x, y, 90, 48, 90, 53);
    wire(g, x, y, 110, 48, 110, 53);
    wire(g, x, y, 130, 48, 130, 53);
    wire(g, x, y, 90, 7, 90, 14);
    wire(g, x, y, 110, 7, 110, 14);
    wire(g, x, y, 130, 7, 130, 14);
    wire(g, x, y, 150, 7, 150, 14);
  }

  /** Orthogonal polyline. Offsets are paired and measured from the package origin. */
  private static void wire(Graphics g, int originX, int originY, int... xy) {
    for (var i = 2; i < xy.length; i += 2) {
      g.drawLine(
          originX + xy[i - 2], originY + xy[i - 1], originX + xy[i], originY + xy[i + 1]);
    }
  }

  /** Width-2 bus. One polyline keeps a mitered corner instead of a white gap. */
  private static void bus(Graphics g, int originX, int originY, int... xy) {
    if (!(g instanceof Graphics2D g2) || xy.length < 4) {
      return;
    }
    final var previous = g2.getStroke();
    g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER));
    final var count = xy.length / 2;
    final var xs = new int[count];
    final var ys = new int[count];
    for (var i = 0; i < count; i++) {
      xs[i] = originX + xy[i * 2];
      ys[i] = originY + xy[i * 2 + 1];
    }
    g2.drawPolyline(xs, ys, count);
    g2.setStroke(previous);
  }

  /** Clock wedge: 2px base on the left edge, point inside the counter. */
  private static void drawClockWedgeLeft(Graphics g, int x, int y) {
    g.drawLine(x, y - 1, x, y + 1);
    g.drawLine(x, y - 1, x + 3, y);
    g.drawLine(x, y + 1, x + 3, y);
  }

  /** Even 2×2 bubble. The center is the point the caller put on the wire axis. */
  private static void drawBubble(Graphics g, int centerX, int centerY) {
    g.drawOval(centerX - 1, centerY - 1, 2, 2);
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

}
