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
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;

/**
 * TTL 74HC374: octal positive-edge D flip-flop with three-state outputs.
 *
 * <p>Simulation follows the
 * <a href="https://www.ti.com/lit/ds/symlink/sn74hc374.pdf">TI SN74HC374</a> and
 * <a href="https://assets.nexperia.com/documents/data-sheet/74HC_HCT374.pdf">Nexperia 74HC374</a>
 * data sheets. Each flip-flop stores its D input on the rising edge of CLK. A low nOE drives the
 * stored bit onto Q; a high nOE, or any level that is not a solid low, releases every Q. Output
 * enable does not change the register, and the register still loads while the outputs are
 * released. High impedance is reported as unknown. Pin names follow the data sheet: D0–D7 and
 * Q0–Q7.
 */
public class Ttl74374 extends AbstractTtlGate {
  /**
   * Unique identifier of the tool, used as reference in project files. Do NOT change as it will
   * prevent project files from loading.
   *
   * <p>Identifier value MUST be unique string among all tools.
   */
  public static final String _ID = "74374";

  public static final int PORT_INDEX_nOE = 0;
  public static final int PORT_INDEX_Q0 = 1;
  public static final int PORT_INDEX_D0 = 2;
  public static final int PORT_INDEX_D1 = 3;
  public static final int PORT_INDEX_Q1 = 4;
  public static final int PORT_INDEX_Q2 = 5;
  public static final int PORT_INDEX_D2 = 6;
  public static final int PORT_INDEX_D3 = 7;
  public static final int PORT_INDEX_Q3 = 8;
  public static final int PORT_INDEX_CLK = 9;
  public static final int PORT_INDEX_Q4 = 10;
  public static final int PORT_INDEX_D4 = 11;
  public static final int PORT_INDEX_D5 = 12;
  public static final int PORT_INDEX_Q5 = 13;
  public static final int PORT_INDEX_Q6 = 14;
  public static final int PORT_INDEX_D6 = 15;
  public static final int PORT_INDEX_D7 = 16;
  public static final int PORT_INDEX_Q7 = 17;
  /** Port index of GND when the explicit power pins are shown. */
  public static final int PORT_INDEX_GND = 18;
  /** Port index of VCC when the explicit power pins are shown. */
  public static final int PORT_INDEX_VCC = 19;

  private static final int DELAY = 1;
  private static final int BITS = 8;
  /** Data port of each bit. Index 0 is D0. */
  private static final int[] DATA = {
    PORT_INDEX_D0,
    PORT_INDEX_D1,
    PORT_INDEX_D2,
    PORT_INDEX_D3,
    PORT_INDEX_D4,
    PORT_INDEX_D5,
    PORT_INDEX_D6,
    PORT_INDEX_D7
  };
  /** Output port of each bit. Index 0 is Q0. */
  private static final int[] OUTPUTS = {
    PORT_INDEX_Q0,
    PORT_INDEX_Q1,
    PORT_INDEX_Q2,
    PORT_INDEX_Q3,
    PORT_INDEX_Q4,
    PORT_INDEX_Q5,
    PORT_INDEX_Q6,
    PORT_INDEX_Q7
  };
  private static final byte[] OUTPUT_PINS = {2, 5, 6, 9, 12, 15, 16, 19};
  private static final String[] PORT_NAMES = {
    "nOE (output enable, active low)",
    "Q0",
    "D0",
    "D1",
    "Q1",
    "Q2",
    "D2",
    "D3",
    "Q3",
    "CLK",
    "Q4",
    "D4",
    "D5",
    "Q5",
    "Q6",
    "D6",
    "D7",
    "Q7"
  };
  /** Column pairs that share one D pin and one Q pin on opposite sides of the package. */
  private static final Slice[] SLICES = {
    new Slice(30, 50),
    new Slice(90, 70),
    new Slice(110, 130),
    new Slice(170, 150)
  };
  private static final int TOP_TIP = 11;
  private static final int TOP_BASE = 16;
  private static final int TOP_SIDE = 13;
  private static final int TOP_BUS = 18;
  private static final int TOP_FLOP = 20;
  private static final int FLOP_HEIGHT = 4;
  private static final int FLOP_WIDTH = 12;
  private static final int TOP_FLOP_EDGE = TOP_FLOP + FLOP_HEIGHT;
  private static final int DATA_TOP = 26;
  private static final int CLOCK_Y = 30;
  private static final int DATA_BOTTOM = 34;
  private static final int BOT_FLOP = 36;
  private static final int BOT_FLOP_EDGE = BOT_FLOP + FLOP_HEIGHT;
  private static final int BOT_BUS = 42;
  private static final int BOT_BASE = 44;
  private static final int BOT_SIDE = 47;
  private static final int BOT_TIP = 49;
  private static final int WEDGE_DEPTH = 2;
  private static final int CLOCK_COLUMN = 190;
  private static final int ENABLE_PIN = 10;
  private static final int ENABLE_SPINE = 16;

  /** Creates a 74374 octal positive-edge D flip-flop with three-state outputs. */
  public Ttl74374() {
    super(_ID, (byte) 20, OUTPUT_PINS, PORT_NAMES, new Ttl74374HdlGenerator());
  }

  /**
   * Eight rising-edge D flip-flops and eight isosceles buffers. The buffer apex points at its Q
   * pin and the flat side faces the flip-flop. nOE leaves pin 1 and ends on a side of each
   * triangle. Each D and each Q keeps its own wire.
   */
  @Override
  public void paintInternal(InstancePainter painter, int x, int y, int height, boolean up) {
    final var g = painter.getGraphics();
    super.paintBase(painter, false, false);
    g.setColor(Color.BLACK);
    drawSymbols(g, x, y);
    drawDataWires(g, x, y, height);
    drawStoredBits(g, x, y);
    drawBufferOutputs(g, x, y, height);
    drawClock(g, x, y);
    drawOutputEnable(g, x, y, height);
  }

  /**
   * One column pair holds the upper and lower bit. The flip-flop is shifted toward its Q pin so
   * the buffer can stay isosceles. Contacts on a long edge are inset equally, and Q is not on the
   * clock or D column.
   */
  private static void drawSymbols(Graphics g, int x, int y) {
    for (var slice : SLICES) {
      drawFlop(g, x, y + TOP_FLOP, slice, true);
      drawFlop(g, x, y + BOT_FLOP, slice, false);
      drawBuffer(g, x, y + TOP_TIP, y + TOP_BASE, slice);
      drawBuffer(g, x, y + BOT_TIP, y + BOT_BASE, slice);
    }
  }

  /** Horizontal flop. The wedge base sits on the inner edge and the tip stays inside the body. */
  private static void drawFlop(Graphics g, int x, int flopY, Slice slice, boolean outputAbove) {
    g.drawRect(x + slice.flopLeft(), flopY, FLOP_WIDTH, FLOP_HEIGHT);
    final var edgeY = outputAbove ? flopY + FLOP_HEIGHT : flopY;
    final var tipY = outputAbove ? edgeY - WEDGE_DEPTH : edgeY + WEDGE_DEPTH;
    final var clockX = x + slice.clockX();
    g.drawLine(clockX - 1, edgeY, clockX, tipY);
    g.drawLine(clockX + 1, edgeY, clockX, tipY);
    g.drawLine(clockX - 1, edgeY, clockX + 1, edgeY);
  }

  /** Isosceles triangle. The apex is the output toward the pin; the base is horizontal. */
  private static void drawBuffer(Graphics g, int x, int tipY, int baseY, Slice slice) {
    final var tipX = x + slice.qColumn();
    g.drawPolygon(
        new int[] {tipX, x + slice.baseLeft(), x + slice.baseRight()},
        new int[] {tipY, baseY, baseY},
        3);
  }

  /**
   * Each D stays on its pin column until the lane two pixels outside the flip-flop, then steps to
   * its contact. The upper and lower runs of one column keep a gap of eight around the clock.
   */
  private static void drawDataWires(Graphics g, int x, int y, int height) {
    final var pinTop = y + PIN_HEIGHT;
    final var pinBottom = y + height - PIN_HEIGHT;
    for (var slice : SLICES) {
      final var column = x + slice.dColumn();
      final var contact = x + slice.dX();
      g.drawLine(column, pinTop, column, y + DATA_TOP);
      g.drawLine(column, y + DATA_TOP, contact, y + DATA_TOP);
      g.drawLine(contact, y + DATA_TOP, contact, y + TOP_FLOP_EDGE);
      g.drawLine(column, pinBottom, column, y + DATA_BOTTOM);
      g.drawLine(column, y + DATA_BOTTOM, contact, y + DATA_BOTTOM);
      g.drawLine(contact, y + DATA_BOTTOM, contact, y + BOT_FLOP);
    }
  }

  /** Straight run from the centered flop output to the matching base contact. */
  private static void drawStoredBits(Graphics g, int x, int y) {
    for (var slice : SLICES) {
      final var contact = x + slice.dataX();
      g.drawLine(contact, y + TOP_BASE, contact, y + TOP_FLOP);
      g.drawLine(contact, y + BOT_FLOP_EDGE, contact, y + BOT_BASE);
    }
  }

  /** From the buffer tip straight along its own Q column to the pin. */
  private static void drawBufferOutputs(Graphics g, int x, int y, int height) {
    final var pinTop = y + PIN_HEIGHT;
    final var pinBottom = y + height - PIN_HEIGHT;
    for (var slice : SLICES) {
      final var column = x + slice.qColumn();
      g.drawLine(column, pinTop, column, y + TOP_TIP);
      g.drawLine(column, y + BOT_TIP, column, pinBottom);
    }
  }

  /**
   * Rising-edge clock. One vertical feeds both wedges of a column pair and meets the lane from
   * pin 11. A dot marks that three-way joint only.
   */
  private static void drawClock(Graphics g, int x, int y) {
    final var pinTop = y + PIN_HEIGHT;
    final var leftClock = x + SLICES[0].clockX();
    g.drawLine(x + CLOCK_COLUMN, pinTop, x + CLOCK_COLUMN, y + CLOCK_Y);
    g.drawLine(x + CLOCK_COLUMN, y + CLOCK_Y, leftClock, y + CLOCK_Y);
    for (var slice : SLICES) {
      final var clockX = x + slice.clockX();
      g.drawLine(clockX, y + TOP_FLOP_EDGE, clockX, y + BOT_FLOP);
      drawDot(g, clockX, y + CLOCK_Y);
    }
  }

  /**
   * Output enable leaves pin 1 and ends on a side edge of every triangle. The trunk crosses each
   * stored-bit wire at its midpoint, and a short run finishes on the slant.
   */
  private static void drawOutputEnable(Graphics g, int x, int y, int height) {
    final var pinBottom = y + height - PIN_HEIGHT;
    final var lastRiser = x + rightmostRiser();
    final var spine = x + ENABLE_SPINE;
    drawPinInverter(g, x, y, pinBottom);
    g.drawLine(x + ENABLE_PIN, y + BOT_BUS, lastRiser, y + BOT_BUS);
    g.drawLine(spine, y + BOT_BUS, spine, y + TOP_BUS);
    g.drawLine(spine, y + TOP_BUS, lastRiser, y + TOP_BUS);
    drawDot(g, spine, y + BOT_BUS);
    for (var slice : SLICES) {
      final var riser = x + slice.riserX();
      final var side = x + slice.sideX();
      g.drawLine(riser, y + TOP_BUS, riser, y + TOP_SIDE);
      g.drawLine(riser, y + TOP_SIDE, side, y + TOP_SIDE);
      g.drawLine(riser, y + BOT_BUS, riser, y + BOT_SIDE);
      g.drawLine(riser, y + BOT_SIDE, side, y + BOT_SIDE);
      if (slice.riserX() != rightmostRiser()) {
        drawDot(g, riser, y + TOP_BUS);
        drawDot(g, riser, y + BOT_BUS);
      }
    }
  }

  /**
   * Small inverter on the wire from pin 1. The apex points away from the pin and lies on the
   * bubble; the output leaves the far side of that bubble.
   */
  private static void drawPinInverter(Graphics g, int x, int y, int pinBottom) {
    final var centerX = x + ENABLE_PIN;
    final var baseY = y + 49;
    final var tipY = y + 46;
    final var half = 3;
    g.drawLine(centerX, pinBottom, centerX, baseY);
    g.drawLine(centerX - half, baseY, centerX + half, baseY);
    final var graphics = (Graphics2D) g;
    final var previous = graphics.getStroke();
    graphics.setStroke(new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL));
    g.drawLine(centerX - half, baseY, centerX, tipY);
    g.drawLine(centerX + half, baseY, centerX, tipY);
    graphics.setStroke(previous);
    g.drawOval(centerX - 1, tipY - 2, 2, 2);
    g.drawLine(centerX, tipY - 2, centerX, y + BOT_BUS);
  }

  private static int rightmostRiser() {
    var max = 0;
    for (var slice : SLICES) {
      max = Math.max(max, slice.riserX());
    }
    return max;
  }

  /** Joint of three or more segments. The oval center is the common point. */
  private static void drawDot(Graphics g, int centerX, int centerY) {
    g.fillOval(centerX - 1, centerY - 1, 2, 2);
  }

  @Override
  public void propagateTtl(InstanceState state) {
    var data = (TtlRegisterData) state.getData();
    if (data == null) {
      data = new TtlRegisterData(BitWidth.ONE, BITS);
      state.setData(data);
    }
    if (data.updateClock(state.getPortValue(PORT_INDEX_CLK), StdAttr.TRIG_RISING)) {
      for (var bit = 0; bit < BITS; bit++) {
        data.setValue(bit, state.getPortValue(DATA[bit]));
      }
    }
    final var enabled = state.getPortValue(PORT_INDEX_nOE) == Value.FALSE;
    for (var bit = 0; bit < BITS; bit++) {
      state.setPort(OUTPUTS[bit], enabled ? data.getValue(bit) : Value.UNKNOWN, DELAY);
    }
  }

  @Override
  public boolean checkForGatedClocks(netlistComponent comp) {
    return true;
  }

  @Override
  public int[] clockPinIndex(netlistComponent comp) {
    return new int[] {PORT_INDEX_CLK};
  }

  /**
   * One Q/D column pair. The flip-flop is shifted toward Q. The buffer is an isosceles triangle
   * centered on Q, with D on the base and output enable on the side that faces away from D.
   */
  private record Slice(int qColumn, int dColumn) {
    private static final int HALF = 5;

    private boolean qOnLeft() {
      return qColumn < dColumn;
    }

    private int baseLeft() {
      return qColumn - HALF;
    }

    private int baseRight() {
      return qColumn + HALF;
    }

    /** Center of the base, on the same axis as the apex. The flip-flop output enters there. */
    private int dataX() {
      return qColumn;
    }

    /** Point on the slant. */
    private int sideX() {
      return qOnLeft() ? qColumn - 2 : qColumn + 2;
    }

    /** Vertical run just outside the base corner. */
    private int riserX() {
      return qOnLeft() ? qColumn - 7 : qColumn + 7;
    }

    private int clockX() {
      return qOnLeft() ? dataX() - 2 : dataX() + 2;
    }

    private int dX() {
      return qOnLeft() ? dataX() + 2 : dataX() - 2;
    }

    private int flopLeft() {
      return dataX() - 6;
    }
  }

}
