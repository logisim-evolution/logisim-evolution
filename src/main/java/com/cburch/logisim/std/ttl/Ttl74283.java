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
import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.instance.InstanceState;
import com.cburch.logisim.util.GraphicsUtil;
import java.awt.BasicStroke;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.Path2D;

public class Ttl74283 extends AbstractTtlGate {
  /**
   * Unique identifier of the tool, used as reference in project files. Do NOT change as it will
   * prevent project files from loading.
   *
   * <p>Identifier value must MUST be unique string among all tools.
   */
  public static final String _ID = "74283";

  public Ttl74283() {
    super(
        _ID,
        (byte) 16,
        new byte[] {1, 4, 9, 10, 13},
        new String[] {
          "∑2", "B2", "A2", "∑1", "A1", "B1", "CIN", "C4", "∑4", "B4", "A4", "∑3", "A3", "B3"
        },
        new Ttl74283HdlGenerator());
  }

  @Override
  public void paintInternal(InstancePainter painter, int x, int y, int height, boolean up) {
    final var g = painter.getGraphics();
    super.paintBase(painter, false, false);
    final var top = y + PIN_HEIGHT;
    final var bottom = y + height - PIN_HEIGHT;
    // Inputs A and B use the face toward the pins. Sums leave the bus face and
    // run to the pins, so the cells can sit closer to the package edge.
    g.drawRect(x + 52, y + 17, 16, 7);
    g.drawRect(x + 92, y + 17, 16, 7);
    g.drawRect(x + 32, y + 36, 16, 7);
    g.drawRect(x + 72, y + 36, 16, 7);
    g.drawRect(x + 112, y + 28, 16, 12);
    final var previous = g.getFont();
    g.setFont(new Font(Font.DIALOG, Font.PLAIN, 3));
    label(g, "Σ3", x + 60, y + 20);
    label(g, "Σ4", x + 100, y + 20);
    label(g, "Σ2", x + 40, y + 39);
    label(g, "Σ1", x + 80, y + 39);
    label(g, "CLA", x + 120, y + 34);
    g.setFont(previous);

    bus(g, x + 46, y + 36, x + 46, y + 30, x + 112, y + 30);
    wire(g, x + 54, y + 24, x + 54, y + 30);
    wire(g, x + 94, y + 24, x + 94, y + 30);
    wire(g, x + 86, y + 36, x + 86, y + 30);

    wire(g, x + 30, top, x + 30, y + 15, x + 56, y + 15, x + 56, y + 17);
    wire(g, x + 50, top, x + 50, y + 11, x + 64, y + 11, x + 64, y + 17);
    wire(g, x + 70, top, x + 70, y + 26, x + 66, y + 26, x + 66, y + 24);

    wire(g, x + 90, top, x + 90, y + 11, x + 96, y + 11, x + 96, y + 17);
    wire(g, x + 110, top, x + 110, y + 11, x + 104, y + 11, x + 104, y + 17);
    wire(g, x + 130, top, x + 130, y + 26, x + 106, y + 26, x + 106, y + 24);

    wire(g, x + 10, bottom, x + 10, y + 34, x + 34, y + 34, x + 34, y + 36);
    wire(g, x + 30, bottom, x + 30, y + 49, x + 36, y + 49, x + 36, y + 43);
    wire(g, x + 50, bottom, x + 50, y + 49, x + 44, y + 49, x + 44, y + 43);

    wire(g, x + 70, bottom, x + 70, y + 34, x + 74, y + 34, x + 74, y + 36);
    wire(g, x + 90, bottom, x + 90, y + 49, x + 76, y + 49, x + 76, y + 43);
    wire(g, x + 110, bottom, x + 110, y + 45, x + 84, y + 45, x + 84, y + 43);

    wire(g, x + 150, top, x + 150, y + 34, x + 128, y + 34);
    wire(g, x + 130, bottom, x + 130, y + 42, x + 108, y + 42, x + 108, y + 38, x + 112, y + 38);
  }

  /** Block caption whose bounds are centered on the block, not merely the anchor. */
  private static void label(Graphics g, String text, int cx, int cy) {
    GraphicsUtil.drawText(g, text, cx, cy, GraphicsUtil.H_CENTER, GraphicsUtil.V_CENTER_OVERALL);
  }

  /**
   * Carry bus, including the corner at Σ2. One polyline so the thick stroke joins; separate butt
   * ends would leave a gap.
   */
  private static void bus(Graphics g, int... points) {
    final var g2 = (Graphics2D) g;
    final var previous = g2.getStroke();
    g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER));
    final var path = new Path2D.Float();
    path.moveTo(points[0], points[1]);
    for (var i = 2; i < points.length; i += 2) {
      path.lineTo(points[i], points[i + 1]);
    }
    g2.draw(path);
    g2.setStroke(previous);
  }

  /** Orthogonal wire through the given points. One call is one net. */
  private static void wire(Graphics g, int... points) {
    for (var i = 2; i < points.length; i += 2) {
      g.drawLine(points[i - 2], points[i - 1], points[i], points[i + 1]);
    }
  }

  @Override
  public void propagateTtl(InstanceState state) {
    final var A1 = state.getPortValue(4) == Value.TRUE ? (byte) 1 : 0;
    final var A2 = state.getPortValue(2) == Value.TRUE ? (byte) 2 : 0;
    final var A3 = state.getPortValue(12) == Value.TRUE ? (byte) 4 : 0;
    final var A4 = state.getPortValue(10) == Value.TRUE ? (byte) 8 : 0;
    final var B1 = state.getPortValue(5) == Value.TRUE ? (byte) 1 : 0;
    final var B2 = state.getPortValue(1) == Value.TRUE ? (byte) 2 : 0;
    final var B3 = state.getPortValue(13) == Value.TRUE ? (byte) 4 : 0;
    final var B4 = state.getPortValue(9) == Value.TRUE ? (byte) 8 : 0;
    final var CIN = state.getPortValue(6) == Value.TRUE ? (byte) 1 : 0;
    final var sum = (byte) (A1 + A2 + A3 + A4 + B1 + B2 + B3 + B4 + CIN);
    final var output = Value.createKnown(BitWidth.create(5), sum);
    state.setPort(3, output.get(0), 1);
    state.setPort(0, output.get(1), 1);
    state.setPort(11, output.get(2), 1);
    state.setPort(8, output.get(3), 1);
    state.setPort(7, output.get(4), 1);
  }
}
