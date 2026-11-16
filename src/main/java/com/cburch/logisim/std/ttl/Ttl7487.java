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

import java.awt.BasicStroke;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.GeneralPath;

/*
 * Class simulates a TTL7487 IC
 * It takes a 4 bit input (A1-A4) and performs one of the following oppertations based on inputs B and c to produce outputs Y1-Y4
 *   B=0, C=0 output complement of A
 *   B=0, C=1 output A unchanged
 *   B=1, C=0 output all High
 *   B=1, C=1 output all Low
 * Likely use is before an adder to give control for addition/subtraction along with increment/decrement/transparent in conjunction with the adder's carry in bit
 * Datasheet: https://archive.org/details/bitsavers_tidataBookVol2_45945352/page/n404/mode/1up?view=theater
 * Alternative source (page 3-354, pdf page 405): http://bitsavers.org/components/ti/_dataBooks/1985_TI_The_TTL_Data_Book_Vol_2.pdf
 */
public class Ttl7487 extends AbstractTtlGate {

  /**
   * Unique identifier of the tool, used as reference in project files. Do NOT change as it will
   * prevent project files from loading.
   *
   * <p>Identifier value must MUST be unique string among all tools.
   */
  public static final String _ID = "7487";

  // input data (port number)
  public static final byte A1 = 1;
  public static final byte A2 = 3;
  public static final byte A3 = 7;
  public static final byte A4 = 9;
  // input control (port number)
  public static final byte B = 5;
  public static final byte C = 0;

  // output (port number)
  public static final byte Y1 = 2;
  public static final byte Y2 = 4;
  public static final byte Y3 = 6;
  public static final byte Y4 = 8;

  // No Connect (datasheet pins)
  public static final byte NC1 = 4;
  public static final byte NC2 = 11;

  public static final byte DELAY = 1;

  public Ttl7487() {
    super(
        _ID,
        (byte) 14,
        new byte[] {3, 6, 9, 12},
        new byte[] {NC1, NC2},
        new String[] {"C", "A1", "Y1", "A2", "Y2", "B", "Y3", "A3", "Y4", "A4"},
        null);
  }

  /**
   * TI logic diagram: B and C each go through one inverter. Every channel NANDs its A input with
   * inverted B, inverts inverted C again, and XORs those two results onto Y.
   */
  @Override
  public void paintInternal(InstancePainter painter, int x, int y, int height, boolean up) {
    final var g = painter.getGraphics();
    super.paintBase(painter, false, false);
    drawSchematic(g, x, y);
  }

  /**
   * Two rows of channels. Shared inverters on pins 1 and 8 feed every channel: /B into the NAND,
   * /C into the second inverter. Each NAND output and that inverter meet in an XOR.
   */
  private static void drawSchematic(Graphics g, int x, int y) {
    drawNand(g, x, y, 36, 11, 10, 8);
    drawNot(g, x, y, 36, 21, 10, 5);
    drawXor(g, x, y, 56, 13, 10, 12);
    drawNand(g, x, y, 36, 35, 10, 8);
    drawNot(g, x, y, 36, 45, 10, 4);
    drawXor(g, x, y, 56, 37, 10, 12);
    drawNand(g, x, y, 92, 11, 10, 8);
    drawNot(g, x, y, 92, 21, 10, 5);
    drawXor(g, x, y, 108, 13, 10, 12);
    drawNand(g, x, y, 92, 35, 10, 8);
    drawNot(g, x, y, 92, 45, 10, 4);
    drawXor(g, x, y, 108, 37, 10, 12);
    drawNotDown(g, x, y, 126, 11, 8, 10);
    drawNotUp(g, x, y, 6, 39, 8, 10);

    wire(g, x, y, 30, 7, 30, 13, 36, 13);
    wire(g, x, y, 90, 7, 90, 13, 92, 13);
    wire(g, x, y, 30, 53, 30, 37, 36, 37);
    wire(g, x, y, 90, 53, 90, 37, 92, 37);
    wire(g, x, y, 66, 19, 68, 19, 68, 11, 50, 11, 50, 7);
    wire(g, x, y, 66, 43, 68, 43, 68, 51, 50, 51, 50, 53);
    wire(g, x, y, 118, 19, 120, 19, 120, 11, 110, 11, 110, 7);
    wire(g, x, y, 118, 43, 120, 43, 120, 51, 110, 51, 110, 53);
    wire(g, x, y, 48, 15, 56, 15);
    wire(g, x, y, 46, 23, 56, 23);
    wire(g, x, y, 48, 39, 56, 39);
    wire(g, x, y, 46, 47, 56, 47);
    wire(g, x, y, 104, 15, 108, 15);
    wire(g, x, y, 102, 23, 108, 23);
    wire(g, x, y, 104, 39, 108, 39);
    wire(g, x, y, 102, 47, 108, 47);
    wire(g, x, y, 130, 7, 130, 11);
    wire(g, x, y, 130, 23, 130, 28, 24, 28);
    wire(g, x, y, 24, 17, 24, 41);
    wire(g, x, y, 24, 17, 36, 17);
    wire(g, x, y, 24, 41, 36, 41);
    wire(g, x, y, 84, 17, 84, 41);
    wire(g, x, y, 84, 17, 92, 17);
    wire(g, x, y, 84, 41, 92, 41);
    wire(g, x, y, 10, 53, 10, 49);
    wire(g, x, y, 10, 37, 10, 33, 78, 33);
    wire(g, x, y, 20, 23, 36, 23);
    wire(g, x, y, 20, 47, 36, 47);
    wire(g, x, y, 20, 23, 20, 47);
    wire(g, x, y, 78, 23, 78, 47);
    wire(g, x, y, 78, 23, 92, 23);
    wire(g, x, y, 78, 47, 92, 47);

    join(g, x, y, 24, 28);
    join(g, x, y, 84, 28);
    join(g, x, y, 20, 33);
    join(g, x, y, 78, 33);
  }

  private static void drawNand(Graphics g, int x, int y, int left, int top, int width, int height) {
    final var originX = x + left;
    final var originY = y + top;
    drawAndBody(g, originX, originY, width, height);
    bubble(g, originX + width, originY + height / 2, true);
  }

  private static void drawNot(Graphics g, int x, int y, int left, int top, int width, int height) {
    final var originX = x + left;
    final var originY = y + top;
    final var midY = originY + height / 2;
    final var face = originX + width;
    final var tip = face - 2;
    drawBevelPolygon(
        g,
        new int[] {originX, tip, originX},
        new int[] {originY, midY, originY + height});
    bubble(g, tip, midY, true);
  }

  /** Inverter on a bottom pin: flat base below, sharp tip and bubble upward. */
  private static void drawNotUp(Graphics g, int x, int y, int left, int top, int width, int height) {
    final var originX = x + left;
    final var originY = y + top;
    final var midX = originX + width / 2;
    final var base = originY + height;
    drawBevelPolygon(
        g,
        new int[] {originX, midX, originX + width},
        new int[] {base, originY, base});
    g.drawOval(midX - 1, originY - 2, 2, 2);
  }

  /** Inverter under a top pin: flat base on top, sharp tip and bubble downward. */
  private static void drawNotDown(Graphics g, int x, int y, int left, int top, int width, int height) {
    final var originX = x + left;
    final var originY = y + top;
    final var midX = originX + width / 2;
    final var face = originY + height;
    drawBevelPolygon(
        g,
        new int[] {originX, midX, originX + width},
        new int[] {originY, face, originY});
    g.drawOval(midX - 1, face, 2, 2);
  }

  /** OR body plus the extra input curve. The tip is the right edge of the box. */
  private static void drawXor(Graphics g, int x, int y, int left, int top, int width, int height) {
    final var x0 = x + left;
    final var y0 = y + top;
    final var x2 = x0 + width;
    final var y1 = y0 + height;
    final var mid = y0 + height / 2;
    final var shield = Math.min(3, Math.max(2, width / 4));
    final var x1 = x0 + shield;
    final var span = Math.max(1, x2 - x1);
    final var path = new GeneralPath();
    path.moveTo(x2, mid);
    path.quadTo(x1 + 2 * span / 3, y0, x1, y0);
    path.quadTo(x1 + span / 3, mid, x1, y1);
    path.quadTo(x1 + 2 * span / 3, y1, x2, mid);
    path.closePath();
    path.moveTo(x0, y0);
    path.quadTo(x0 + Math.max(2, span / 3), mid, x0, y1);
    ((Graphics2D) g).draw(path);
  }

  private static void drawAndBody(Graphics g, int left, int top, int width, int height) {
    final var face = left + width;
    final var nose = face - 1;
    final var bottom = top + height;
    final var radius = Math.min(height / 2, Math.max(1, nose - left));
    g.drawLine(left, top, left, bottom);
    if (radius == height / 2) {
      final var chord = nose - radius;
      g.drawLine(left, top, chord, top);
      g.drawLine(left, bottom, chord, bottom);
      g.drawArc(nose - 2 * radius, top, 2 * radius, height, -90, 180);
      return;
    }
    final var arcTop = top + (height - 2 * radius) / 2;
    g.drawLine(left, top, nose, top);
    g.drawLine(nose, top, nose, arcTop);
    g.drawArc(nose - 2 * radius, arcTop, 2 * radius, 2 * radius, -90, 180);
    g.drawLine(nose, arcTop + 2 * radius, nose, bottom);
    g.drawLine(left, bottom, nose, bottom);
  }

  /** Triangle without a miter spike: the default join extends an acute tip into the bubble. */
  private static void drawBevelPolygon(Graphics g, int[] xs, int[] ys) {
    final var g2 = (Graphics2D) g;
    final var previous = g2.getStroke();
    g2.setStroke(new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL));
    g2.drawPolygon(xs, ys, xs.length);
    g2.setStroke(previous);
  }

  /** Even 2×2 bubble outside a vertical face, centered on {@code axisY} and touching the face. */
  private static void bubble(Graphics g, int faceX, int axisY, boolean toTheRight) {
    g.drawOval(toTheRight ? faceX : faceX - 2, axisY - 1, 2, 2);
  }

  private static void wire(Graphics g, int x, int y, int... pts) {
    for (var i = 0; i < pts.length - 2; i += 2) {
      g.drawLine(x + pts[i], y + pts[i + 1], x + pts[i + 2], y + pts[i + 3]);
    }
  }

  private static void join(Graphics g, int x, int y, int px, int py) {
    g.fillOval(x + px - 1, y + py - 1, 2, 2);
  }

  @Override
  public void propagateTtl(InstanceState state) {
    if (state.getPortValue(B) == Value.TRUE) {
      // High or low output
      state.setPort(Y1, state.getPortValue(C).not(), DELAY);
      state.setPort(Y2, state.getPortValue(C).not(), DELAY);
      state.setPort(Y3, state.getPortValue(C).not(), DELAY);
      state.setPort(Y4, state.getPortValue(C).not(), DELAY);
    } else if (state.getPortValue(B) == Value.FALSE) {
      if (state.getPortValue(C) == Value.TRUE) {
        // not inverted
        state.setPort(Y1, state.getPortValue(A1), DELAY);
        state.setPort(Y2, state.getPortValue(A2), DELAY);
        state.setPort(Y3, state.getPortValue(A3), DELAY);
        state.setPort(Y4, state.getPortValue(A4), DELAY);
      } else if (state.getPortValue(C) == Value.FALSE) {
        // inverted
        state.setPort(Y1, state.getPortValue(A1).not(), DELAY);
        state.setPort(Y2, state.getPortValue(A2).not(), DELAY);
        state.setPort(Y3, state.getPortValue(A3).not(), DELAY);
        state.setPort(Y4, state.getPortValue(A4).not(), DELAY);
      } else {
        state.setPort(Y1, Value.ERROR, DELAY);
        state.setPort(Y2, Value.ERROR, DELAY);
        state.setPort(Y3, Value.ERROR, DELAY);
        state.setPort(Y4, Value.ERROR, DELAY);
      }
    } else {
      state.setPort(Y1, Value.ERROR, DELAY);
      state.setPort(Y2, Value.ERROR, DELAY);
      state.setPort(Y3, Value.ERROR, DELAY);
      state.setPort(Y4, Value.ERROR, DELAY);
    }

  }
}
