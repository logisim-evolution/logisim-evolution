/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.std.gates;

import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.util.GraphicsUtil;
import java.util.HashMap;

class PainterDin {
  private static void paint(
      InstancePainter painter, int width, int height, boolean drawBubble, int dinType) {
    final var g = painter.getGraphics();
    var xMid = -width;
    final var y0 = -height / 2;
    if (drawBubble) {
      width -= 8;
    }
    final var diam = Math.min(height, 2 * width);
    if (dinType == AND) {
      // nothing to do
    } else if (dinType == OR) {
      paintOrLines(painter, width, height, drawBubble);
    } else if (dinType == XOR || dinType == XNOR) {
      int elen = Math.min(diam / 2 - 10, 20);
      int ex0 = xMid + (diam / 2 - elen) / 2;
      int ex1 = ex0 + elen;
      g.drawLine(ex0, -5, ex1, -5);
      g.drawLine(ex0, 0, ex1, 0);
      g.drawLine(ex0, 5, ex1, 5);
      if (dinType == XOR) {
        final var exMid = ex0 + elen / 2;
        g.drawLine(exMid, -8, exMid, 8);
      }
    } else {
      throw new IllegalArgumentException("unrecognized shape");
    }

    GraphicsUtil.switchToWidth(g, 2);
    final var x0 = xMid - diam / 2;
    final var oldColor = g.getColor();
    if (painter.getShowState()) {
      final var val = painter.getPortValue(0);
      g.setColor(val.getColor());
    }
    g.drawLine(x0 + diam, 0, 0, 0);
    g.setColor(oldColor);
    if (height <= diam) {
      g.drawArc(x0, y0, diam, diam, -90, 180);
    } else {
      final var x1 = x0 + diam;
      final var yy0 = -(height - diam) / 2;
      final var yy1 = (height - diam) / 2;
      g.drawArc(x0, y0, diam, diam, 0, 90);
      g.drawLine(x1, yy0, x1, yy1);
      g.drawArc(x0, y0 + height - diam, diam, diam, -90, 90);
    }
    g.drawLine(xMid, y0, xMid, y0 + height);
    if (drawBubble) {
      g.fillOval(x0 + diam - 4, -4, 8, 8);
      xMid += 4;
    }
  }

  static void paintAnd(InstancePainter painter, int width, int height, boolean drawBubble) {
    paint(painter, width, height, drawBubble, AND);
  }

  static void paintOr(InstancePainter painter, int width, int height, boolean drawBubble) {
    paint(painter, width, height, drawBubble, OR);
  }

  private static void paintOrLines(InstancePainter painter, int width, int height, boolean hasBubble) {
    final var baseAttrs = (GateAttributes) painter.getAttributeSet();
    final var inputs = baseAttrs.inputs;
    final var attrs = (GateAttributes) OrGate.FACTORY.createAttributeSet();
    attrs.inputs = inputs;
    attrs.size = baseAttrs.size;

    final var g = painter.getGraphics();
    // draw state if appropriate
    // ignore lines if in print view
    final var r = Math.min(height / 2, width);
    final var hash = r << 4 | inputs;
    var lens = orLenArrays.get(hash);
    if (lens == null) {
      lens = new int[inputs];
      orLenArrays.put(hash, lens);
      final var yCurveStart = height / 2 - r;
      for (var i = 0; i < inputs; i++) {
        var y = OrGate.FACTORY.getInputOffset(attrs, i).getY();
        if (y < 0) y = -y;
        if (y <= yCurveStart) {
          lens[i] = r;
        } else {
          int dy = y - yCurveStart;
          lens[i] = (int) (Math.sqrt(r * r - dy * dy) + 0.5);
        }
      }
    }

    final var factory = hasBubble ? NorGate.FACTORY : OrGate.FACTORY;
    final var printView = painter.isPrintView() && painter.getInstance() != null;
    GraphicsUtil.switchToWidth(g, 2);
    for (var i = 0; i < inputs; i++) {
      if (!printView || painter.isPortConnected(i)) {
        final var loc = factory.getInputOffset(attrs, i);
        int x = loc.getX();
        int y = loc.getY();
        g.drawLine(x, y, x + lens[i], y);
      }
    }
  }

  static void paintXnor(InstancePainter painter, int width, int height, boolean drawBubble) {
    paint(painter, width, height, drawBubble, XNOR);
  }

  static void paintXor(InstancePainter painter, int width, int height, boolean drawBubble) {
    paint(painter, width, height, drawBubble, XOR);
  }

  static final int AND = 0;

  static final int OR = 1;

  static final int XOR = 2;

  static final int XNOR = 3;

  private static final HashMap<Integer, int[]> orLenArrays = new HashMap<>();

  private PainterDin() {}
}
