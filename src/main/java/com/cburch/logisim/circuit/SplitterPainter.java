/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.circuit;

import com.cburch.logisim.comp.ComponentDrawContext;
import com.cburch.logisim.data.Direction;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.util.GraphicsUtil;
import java.awt.Graphics2D;

class SplitterPainter {
  static void drawLabels(ComponentDrawContext context, SplitterAttributes attrs, Location origin) {
    // compute labels
    final var ends = new String[attrs.fanout + 1];
    var curEnd = -1;
    var cur0 = 0;
    for (int i = 0, n = attrs.bitEnd.length; i <= n; i++) {
      final var bit = i == n ? -1 : attrs.bitEnd[i];
      if (bit != curEnd) {
        int cur1 = i - 1;
        String toAdd;
        if (curEnd <= 0) {
          toAdd = null;
        } else if (cur0 == cur1) {
          toAdd = "" + cur0;
        } else {
          toAdd = cur1 + "-" + cur0;
        }
        if (toAdd != null) {
          final var old = ends[curEnd];
          if (old == null) {
            ends[curEnd] = toAdd;
          } else {
            ends[curEnd] = toAdd + "," + old;
          }
        }
        curEnd = bit;
        cur0 = i;
      }
    }

    final var g = context.getGraphics().create();
    final var font = g.getFont();
    g.setFont(font.deriveFont(7.0f));

    final var parms = attrs.getParameters();
    int x = origin.getX() + parms.getEnd0X() + parms.getEndToSpineDeltaX();
    int y = origin.getY() + parms.getEnd0Y() + parms.getEndToSpineDeltaY();
    int dx = parms.getEndToEndDeltaX();
    int dy = parms.getEndToEndDeltaY();
    if (parms.getTextAngle() != 0) {
      ((Graphics2D) g).rotate(Math.PI / 2.0);
      int t;
      t = -x;
      x = y;
      y = t;
      t = -dx;
      dx = dy;
      dy = t;
    }
    final var halign = parms.getTextHorzAlign();
    final var valign = parms.getTextVertAlign();
    x += (halign == GraphicsUtil.H_RIGHT ? -1 : 1) * (SPINE_WIDTH / 2 + 1);
    y += valign == GraphicsUtil.V_TOP ? 0 : -3;
    for (int i = 0, n = attrs.fanout; i < n; i++) {
      final var text = ends[i + 1];
      if (text != null) {
        GraphicsUtil.drawText(g, text, x, y, halign, valign);
      }
      x += dx;
      y += dy;
    }

    g.dispose();
  }

  static void drawLegacy(ComponentDrawContext context, SplitterAttributes attrs, Location origin) {
    final var g = context.getGraphics();
    final var state = context.getCircuitState();
    final var facing = attrs.facing;
    final var fanout = attrs.fanout;
    final var parms = attrs.getParameters();

    g.setColor(Value.multiColor);
    final var x0 = origin.getX();
    final var y0 = origin.getY();
    final var x1 = x0 + parms.getEnd0X();
    final var y1 = y0 + parms.getEnd0Y();
    final var dx = parms.getEndToEndDeltaX();
    final var dy = parms.getEndToEndDeltaY();
    if (facing == Direction.NORTH || facing == Direction.SOUTH) {
      final var ySpine = (y0 + y1) / 2;
      GraphicsUtil.switchToWidth(g, Wire.WIDTH);
      g.drawLine(x0, y0, x0, ySpine);
      var xi = x1;
      var yi = y1;
      for (int i = 1; i <= fanout; i++) {
        if (context.getShowState()) {
          g.setColor(state.getValue(Location.create(xi, yi, true)).getColor());
        }
        final var xSpine = xi + (xi == x0 ? 0 : (xi < x0 ? 10 : -10));
        g.drawLine(xi, yi, xSpine, ySpine);
        xi += dx;
        yi += dy;
      }
      if (fanout > 3) {
        GraphicsUtil.switchToWidth(g, SPINE_WIDTH);
        g.setColor(Value.multiColor);
        g.drawLine(
            x1 + (dx > 0 ? 10 : -10), ySpine, x1 + (fanout - 1) * dx + (dx > 0 ? 10 : -10), ySpine);
      } else {
        g.setColor(Value.multiColor);
        g.fillOval(x0 - SPINE_DOT / 2, ySpine - SPINE_DOT / 2, SPINE_DOT, SPINE_DOT);
      }
    } else {
      final var xSpine = (x0 + x1) / 2;
      GraphicsUtil.switchToWidth(g, Wire.WIDTH);
      g.drawLine(x0, y0, xSpine, y0);
      var xi = x1;
      var yi = y1;
      for (int i = 1; i <= fanout; i++) {
        if (context.getShowState()) {
          g.setColor(state.getValue(Location.create(xi, yi, true)).getColor());
        }
        final var ySpine = yi + (yi == y0 ? 0 : (yi < y0 ? 10 : -10));
        g.drawLine(xi, yi, xSpine, ySpine);
        xi += dx;
        yi += dy;
      }
      if (fanout >= 3) {
        GraphicsUtil.switchToWidth(g, SPINE_WIDTH);
        g.setColor(Value.multiColor);
        g.drawLine(
            xSpine, y1 + (dy > 0 ? 10 : -10), xSpine, y1 + (fanout - 1) * dy + (dy > 0 ? 10 : -10));
      } else {
        g.setColor(Value.multiColor);
        g.fillOval(xSpine - SPINE_DOT / 2, y0 - SPINE_DOT / 2, SPINE_DOT, SPINE_DOT);
      }
    }
    GraphicsUtil.switchToWidth(g, 1);
  }

  static void drawLines(ComponentDrawContext context, SplitterAttributes attrs, Location origin) {
    var showState = context.getShowState();
    final var state = showState ? context.getCircuitState() : null;
    if (state == null) showState = false;

    final var parms = attrs.getParameters();
    final var x0 = origin.getX();
    final var y0 = origin.getY();
    var x = x0 + parms.getEnd0X();
    var y = y0 + parms.getEnd0Y();
    var dx = parms.getEndToEndDeltaX();
    var dy = parms.getEndToEndDeltaY();
    final var dxEndSpine = parms.getEndToSpineDeltaX();
    final var dyEndSpine = parms.getEndToSpineDeltaY();

    final var g = context.getGraphics();
    final var oldColor = g.getColor();
    GraphicsUtil.switchToWidth(g, Wire.WIDTH);
    for (int i = 0, n = attrs.fanout; i < n; i++) {
      if (showState) {
        final var val = state.getValue(Location.create(x, y, true));
        g.setColor(val.getColor());
      }
      g.drawLine(x, y, x + dxEndSpine, y + dyEndSpine);
      x += dx;
      y += dy;
    }
    GraphicsUtil.switchToWidth(g, SPINE_WIDTH);
    g.setColor(Value.multiColor);
    var spine0x = x0 + parms.getSpine0X();
    var spine0y = y0 + parms.getSpine0Y();
    var spine1x = x0 + parms.getSpine1X();
    var spine1y = y0 + parms.getSpine1Y();
    if (spine0x == spine1x && spine0y == spine1y) { // centered
      final var fanout = attrs.fanout;
      spine0x = x0 + parms.getEnd0X() + parms.getEndToSpineDeltaX();
      spine0y = y0 + parms.getEnd0Y() + parms.getEndToSpineDeltaY();
      spine1x = spine0x + (fanout - 1) * parms.getEndToEndDeltaX();
      spine1y = spine0y + (fanout - 1) * parms.getEndToEndDeltaY();
      if (parms.getEndToEndDeltaX() == 0) { // vertical spine
        if (spine0y < spine1y) {
          spine0y++;
          spine1y--;
        } else {
          spine0y--;
          spine1y++;
        }
        g.drawLine(x0 + parms.getSpine1X() / 4, y0, spine0x, y0);
      } else {
        if (spine0x < spine1x) {
          spine0x++;
          spine1x--;
        } else {
          spine0x--;
          spine1x++;
        }
        g.drawLine(x0, y0 + parms.getSpine1Y() / 4, x0, spine0y);
      }
      if (fanout <= 1) { // spine is empty
        int diam = SPINE_DOT;
        g.fillOval(spine0x - diam / 2, spine0y - diam / 2, diam, diam);
      } else {
        g.drawLine(spine0x, spine0y, spine1x, spine1y);
      }
    } else {
      int[] xSpine = {spine0x, spine1x, x0 + parms.getSpine1X() / 4};
      int[] ySpine = {spine0y, spine1y, y0 + parms.getSpine1Y() / 4};
      g.drawPolyline(xSpine, ySpine, 3);
    }
    g.setColor(oldColor);
  }

  private static final int SPINE_WIDTH = Wire.WIDTH + 2;

  private static final int SPINE_DOT = Wire.WIDTH + 4;
}
