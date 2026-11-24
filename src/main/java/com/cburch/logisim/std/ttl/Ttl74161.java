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
import com.cburch.logisim.instance.InstancePoker;
import com.cburch.logisim.instance.InstanceState;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.prefs.AppPreferences;
import com.cburch.logisim.util.GraphicsUtil;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseEvent;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;

public class Ttl74161 extends AbstractTtlGate {
  /**
   * Unique identifier of the tool, used as reference in project files. Do NOT change as it will
   * prevent project files from loading.
   *
   * <p>Identifier value must MUST be unique string among all tools.
   */
  public static final String _ID = "74161";

  public static final int PORT_INDEX_nCLR = 0;
  public static final int PORT_INDEX_CLK = 1;
  public static final int PORT_INDEX_A = 2;
  public static final int PORT_INDEX_B = 3;
  public static final int PORT_INDEX_C = 4;
  public static final int PORT_INDEX_D = 5;
  public static final int PORT_INDEX_EnP = 6;
  public static final int PORT_INDEX_nLOAD = 7;
  public static final int PORT_INDEX_EnT = 8;
  public static final int PORT_INDEX_QD = 9;
  public static final int PORT_INDEX_QC = 10;
  public static final int PORT_INDEX_QB = 11;
  public static final int PORT_INDEX_QA = 12;
  public static final int PORT_INDEX_RC0 = 13;
  private static final String[] PORT_NAMES = {
    "MR/CLR (Reset, active LOW)",
    "CP/CLK (Clock)",
    "D0/A",
    "D1/B",
    "D2/C",
    "D3/D",
    "CE/ENP (Count Enable)",
    "PE/LOAD (Parallel Enable, active LOW)",
    "CET/ENT (Count Enable Carry)",
    "Q3/QD",
    "Q2/QC",
    "A1/QB",
    "A0/QA",
    "TC/RC0 (Terminal Count)"
  };
  private static final byte[] OUTPUT_PORTS = {11, 12, 13, 14, 15};

  /** Counter block. A click on a vertical slice toggles Q0..Q3 from left to right. */
  private static final int COUNTER_LEFT = 13;

  private static final int COUNTER_TOP = 28;
  private static final int COUNTER_RIGHT = 137;
  private static final int COUNTER_BOTTOM = 38;
  private static final int[] BIT_SPLIT = {13, 44, 75, 106, 137};

  public Ttl74161() {
    super(_ID, (byte) 16, OUTPUT_PORTS, PORT_NAMES, null);
    super.setInstancePoker(Poker.class);
  }

  public Ttl74161(String name) {
    super(name, (byte) 16, OUTPUT_PORTS, PORT_NAMES, null);
    super.setInstancePoker(Poker.class);
  }

  public static class Poker extends InstancePoker {
    boolean isPressed = true;

    private boolean isInside(InstanceState state, MouseEvent e) {
      final var p = getTranslatedTtlXY(state, e);
      return bitAt(p.x, p.y) >= 0;
    }

    private int getIndex(InstanceState state, MouseEvent e) {
      final var p = getTranslatedTtlXY(state, e);
      final var bit = bitAt(p.x, p.y);
      return bit < 0 ? 0 : bit;
    }

    /** Four vertical slices of the counter, one per Q, so a click still toggles that bit. */
    private static int bitAt(int px, int py) {
      if (py < COUNTER_TOP || py > COUNTER_BOTTOM) return -1;
      if (px < COUNTER_LEFT || px > COUNTER_RIGHT) return -1;
      for (var bit = 0; bit < BIT_SPLIT.length - 1; bit++) {
        final var right = bit == BIT_SPLIT.length - 2 ? COUNTER_RIGHT : BIT_SPLIT[bit + 1] - 1;
        if (px <= right) return bit;
      }
      return -1;
    }

    @Override
    public void mousePressed(InstanceState state, MouseEvent e) {
      isPressed = isInside(state, e);
    }

    @Override
    public void mouseReleased(InstanceState state, MouseEvent e) {
      if (!state.getAttributeValue(TtlLibrary.DRAW_INTERNAL_STRUCTURE)) return;
      if (isPressed && isInside(state, e)) {
        var index = getIndex(state, e);
        final var data = (TtlRegisterData) state.getData();
        if (data == null) return;
        var current = data.getValue().toLongValue();
        final var bitValue = 1L << index;
        current ^= bitValue;
        updateState(state, current);
      }
      isPressed = false;
    }
  }

  @Override
  public void paintInternal(InstancePainter painter, int x, int y, int height, boolean up) {
    final var g = painter.getGraphics();
    super.paintBase(painter, false, false);
    g.setColor(Color.BLACK);
    drawCounter(g, x, y, height);
  }

  /**
   * One counter. Inputs sit on the bottom, outputs on the top. D and Q are buses. CEP and CET meet
   * in the enable gate; the carry output and CET meet in the terminal-count gate.
   */
  private static void drawCounter(Graphics g, int x, int y, int height) {
    final var pinTop = y + AbstractTtlGate.PIN_HEIGHT;
    final var pinBottom = y + height - AbstractTtlGate.PIN_HEIGHT;
    final var counterLeft = x + COUNTER_LEFT;
    final var counterTop = y + COUNTER_TOP;
    final var counterBottom = y + COUNTER_BOTTOM;

    final var previousFont = g.getFont();
    g.setFont(new Font(Font.DIALOG, Font.PLAIN, 5));
    g.drawRect(counterLeft, counterTop, COUNTER_RIGHT - COUNTER_LEFT, COUNTER_BOTTOM - COUNTER_TOP);
    final var counterMidY = (counterTop + counterBottom) / 2;
    GraphicsUtil.drawText(
        g, "COUNTER", x + (COUNTER_LEFT + COUNTER_RIGHT) / 2, counterMidY,
        GraphicsUtil.H_CENTER, GraphicsUtil.V_CENTER_OVERALL);
    g.drawLine(x + 28, counterBottom, x + 30, counterMidY);
    g.drawLine(x + 32, counterBottom, x + 30, counterMidY);
    paintAndUp(g, x + 30, y + 11, 9, 10);
    g.drawRect(x + 114, y + 40, 8, 11);
    drawGateMark(g, x + 118, y + 45);
    g.setFont(previousFont);

    g.drawLine(x + 30, y + 11, x + 30, pinTop);
    g.drawLine(x + 28, y + 20, x + 28, counterTop);

    g.drawLine(x + 32, y + 24, x + 32, y + 20);
    g.drawLine(x + 32, y + 24, x + 141, y + 24);
    g.drawLine(x + 130, pinTop, x + 130, y + 24);
    drawNode(g, x + 130, y + 24);
    g.drawLine(x + 141, y + 24, x + 141, y + 44);
    g.drawLine(x + 141, y + 44, x + 122, y + 44);

    // Straight bus out to the far face of the thick end stub. The stub shares that
    // centre, so the bar covers the joint; a turn cuts the upper half short.
    bus(g, x + 49.5f, y + 16, x + 111f, y + 16);
    bus(g, x + 110, y + 16, x + 110, counterTop);
    g.drawLine(x + 50, pinTop, x + 50, y + 16);
    g.drawLine(x + 70, pinTop, x + 70, y + 16);
    g.drawLine(x + 90, pinTop, x + 90, y + 16);
    g.drawLine(x + 110, pinTop, x + 110, y + 16);

    busPath(
        g,
        new float[] {x + 49.5f, x + 69, x + 69},
        new float[] {y + 48, y + 48, counterBottom});
    bus(g, x + 69, y + 48, x + 110.5f, y + 48);
    g.drawLine(x + 50, pinBottom, x + 50, y + 48);
    g.drawLine(x + 70, pinBottom, x + 70, y + 48);
    g.drawLine(x + 90, pinBottom, x + 90, y + 48);
    g.drawLine(x + 110, pinBottom, x + 110, y + 48);

    g.drawOval(x + 16, counterBottom, 2, 2);
    g.drawLine(x + 10, pinBottom, x + 10, y + 48);
    g.drawLine(x + 10, y + 48, x + 17, y + 48);
    g.drawLine(x + 17, y + 48, x + 17, y + 40);

    g.drawLine(x + 30, pinBottom, x + 30, counterBottom);

    g.drawOval(x + 131, counterBottom, 2, 2);
    g.drawLine(x + 150, pinTop, x + 150, y + 42);
    g.drawLine(x + 150, y + 42, x + 132, y + 42);
    g.drawLine(x + 132, y + 42, x + 132, y + 40);

    g.drawLine(x + 114, y + 45, x + 99, y + 45);
    g.drawLine(x + 99, y + 45, x + 99, counterBottom);
    g.drawLine(x + 130, pinBottom, x + 130, y + 48);
    g.drawLine(x + 130, y + 48, x + 122, y + 48);
  }

  /** Thick bus. The butt end sits half a pixel outside the outermost tap. */
  private static void busPath(Graphics g, float[] xs, float[] ys) {
    if (!(g instanceof Graphics2D g2) || xs.length == 0) {
      for (var i = 1; i < xs.length; i++) {
        g.drawLine(Math.round(xs[i - 1]), Math.round(ys[i - 1]), Math.round(xs[i]), Math.round(ys[i]));
      }
      return;
    }
    final var previous = g2.getStroke();
    g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER));
    final var path = new Path2D.Float();
    path.moveTo(xs[0], ys[0]);
    for (var i = 1; i < xs.length; i++) {
      path.lineTo(xs[i], ys[i]);
    }
    g2.draw(path);
    g2.setStroke(previous);
  }

  /** Thick bus. The butt end sits half a pixel outside the outermost tap. */
  private static void bus(Graphics g, float x1, float y1, float x2, float y2) {
    if (!(g instanceof Graphics2D g2)) {
      g.drawLine(Math.round(x1), Math.round(y1), Math.round(x2), Math.round(y2));
      return;
    }
    final var previous = g2.getStroke();
    g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER));
    g2.draw(new Line2D.Float(x1, y1, x2, y2));
    g2.setStroke(previous);
  }

  /** AND with the output on top. A label that does not fit becomes the arc form. */
  private static void paintAndUp(Graphics g, int outX, int outY, int depth, int span) {
    final var left = outX - span / 2;
    if (rectangularGates() && gateMarkFits(g, span, depth)) {
      g.drawRect(left, outY, span, depth);
      drawGateMark(g, outX, outY + depth / 2);
      return;
    }
    final var radius = depth / 2;
    GraphicsUtil.drawCenteredArc(g, outX, outY + radius, radius, 0, 180);
    final var back = outY + depth;
    g.drawLine(outX - radius, outY + radius, left, back);
    g.drawLine(outX + radius, outY + radius, left + span, back);
    g.drawLine(left, back, left + span, back);
  }

  private static boolean rectangularGates() {
    return AppPreferences.SHAPE_RECTANGULAR.equals(AppPreferences.GATE_SHAPE.get());
  }

  private static boolean gateMarkFits(Graphics g, int width, int height) {
    final var bounds =
        GraphicsUtil.getTextBounds(
            g, "&", 0, 0, GraphicsUtil.H_CENTER, GraphicsUtil.V_CENTER_OVERALL);
    return bounds.width < width && bounds.height < height;
  }

  private static void drawGateMark(Graphics g, int cx, int cy) {
    GraphicsUtil.drawText(g, "&", cx, cy, GraphicsUtil.H_CENTER, GraphicsUtil.V_CENTER_OVERALL);
  }

  private static void drawNode(Graphics g, int x, int y) {
    g.fillOval(x - 1, y - 1, 2, 2);
  }

  public static void updateState(InstanceState state, Long value) {
    var data = getStateData(state);

    data.setValue(Value.createKnown(BitWidth.create(4), value));
    final var vA = data.getValue().get(0);
    final var vB = data.getValue().get(1);
    final var vC = data.getValue().get(2);
    final var vD = data.getValue().get(3);

    state.setPort(PORT_INDEX_QA, vA, 1);
    state.setPort(PORT_INDEX_QB, vB, 1);
    state.setPort(PORT_INDEX_QC, vC, 1);
    state.setPort(PORT_INDEX_QD, vD, 1);

    // RC0 = QA AND QB AND QC AND QD AND ENT
    state.setPort(
        PORT_INDEX_RC0, state.getPortValue(PORT_INDEX_EnT).and(vA).and(vB).and(vC).and(vD), 1);
  }

  public static TtlRegisterData getStateData(InstanceState state) {
    var data = (TtlRegisterData) state.getData();
    if (data == null) {
      data = new TtlRegisterData(BitWidth.create(4));
      state.setData(data);
    }
    return data;
  }

  @Override
  public void propagateTtl(InstanceState state) {
    var data = getStateData(state);
    final var triggered = data.updateClock(state.getPortValue(PORT_INDEX_CLK), StdAttr.TRIG_RISING);
    final var nClear = state.getPortValue(PORT_INDEX_nCLR).toLongValue();
    var counter = data.getValue().toLongValue();

    if (nClear == 0) {
      counter = 0;
    } else if (triggered) {
      final var nLoad = state.getPortValue(PORT_INDEX_nLOAD);
      if (nLoad.toLongValue() == 0) {
        counter = state.getPortValue(PORT_INDEX_A).toLongValue();
        counter += state.getPortValue(PORT_INDEX_B).toLongValue() << 1;
        counter += state.getPortValue(PORT_INDEX_C).toLongValue() << 2;
        counter += state.getPortValue(PORT_INDEX_D).toLongValue() << 3;
      } else {
        final var enpAndEnt =
            state
                .getPortValue(PORT_INDEX_EnP)
                .and(state.getPortValue(PORT_INDEX_EnT))
                .toLongValue();
        if (enpAndEnt == 1) {
          counter++;
          if (counter > 15) {
            counter = 0;
          }
        }
      }
    }
    updateState(state, counter);
  }

  @Override
  public boolean checkForGatedClocks(netlistComponent comp) {
    return true;
  }

  @Override
  public int[] clockPinIndex(netlistComponent comp) {
    return new int[] {1};
  }
}
