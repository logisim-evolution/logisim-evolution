/**
 * This file is part of logisim-evolution.
 *
 * Logisim-evolution is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 *
 * Logisim-evolution is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License along 
 * with logisim-evolution. If not, see <http://www.gnu.org/licenses/>.
 *
 * Original code by Carl Burch (http://www.cburch.com), 2011.
 * Subsequent modifications by:
 *   + College of the Holy Cross
 *     http://www.holycross.edu
 *   + Haute École Spécialisée Bernoise/Berner Fachhochschule
 *     http://www.bfh.ch
 *   + Haute École du paysage, d'ingénierie et d'architecture de Genève
 *     http://hepia.hesge.ch/
 *   + Haute École d'Ingénierie et de Gestion du Canton de Vaud
 *     http://www.heig-vd.ch/
 */

package com.cburch.logisim.circuit;

import com.cburch.logisim.comp.ComponentDrawContext;
import com.cburch.logisim.data.Direction;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.util.GraphicsUtil;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;

class SplitterPainter {
  static void drawLabels(ComponentDrawContext context, SplitterAttributes attrs, Location origin) {
    // compute labels
    String[] ends = new String[attrs.fanout + 1];
    int curEnd = -1;
    int cur0 = 0;
    for (int i = 0, n = attrs.bit_end.length; i <= n; i++) {
      int bit = i == n ? -1 : attrs.bit_end[i];
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
          String old = ends[curEnd];
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

    Graphics g = context.getGraphics().create();
    Font font = g.getFont();
    g.setFont(font.deriveFont(7.0f));

    SplitterParameters parms = attrs.getParameters();
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
    int halign = parms.getTextHorzAlign();
    int valign = parms.getTextVertAlign();
    x += (halign == GraphicsUtil.H_RIGHT ? -1 : 1) * (SPINE_WIDTH / 2 + 1);
    y += valign == GraphicsUtil.V_TOP ? 0 : -3;
    for (int i = 0, n = attrs.fanout; i < n; i++) {
      String text = ends[i + 1];
      if (text != null) {
        GraphicsUtil.drawText(g, text, x, y, halign, valign);
      }
      x += dx;
      y += dy;
    }

    g.dispose();
  }

  static void drawLegacy(ComponentDrawContext context, SplitterAttributes attrs, Location origin) {
    Graphics g = context.getGraphics();
    CircuitState state = context.getCircuitState();
    Direction facing = attrs.facing;
    int fanout = attrs.fanout;
    SplitterParameters parms = attrs.getParameters();

    g.setColor(Value.MULTI_COLOR);
    int x0 = origin.getX();
    int y0 = origin.getY();
    int x1 = x0 + parms.getEnd0X();
    int y1 = y0 + parms.getEnd0Y();
    int dx = parms.getEndToEndDeltaX();
    int dy = parms.getEndToEndDeltaY();
    if (facing == Direction.NORTH || facing == Direction.SOUTH) {
      int ySpine = (y0 + y1) / 2;
      GraphicsUtil.switchToWidth(g, Wire.WIDTH);
      g.drawLine(x0, y0, x0, ySpine);
      int xi = x1;
      int yi = y1;
      for (int i = 1; i <= fanout; i++) {
        if (context.getShowState()) {
          g.setColor(state.getValue(Location.create(xi, yi)).getColor());
        }
        int xSpine = xi + (xi == x0 ? 0 : (xi < x0 ? 10 : -10));
        g.drawLine(xi, yi, xSpine, ySpine);
        xi += dx;
        yi += dy;
      }
      if (fanout > 3) {
        GraphicsUtil.switchToWidth(g, SPINE_WIDTH);
        g.setColor(Value.MULTI_COLOR);
        g.drawLine(
            x1 + (dx > 0 ? 10 : -10), ySpine, x1 + (fanout - 1) * dx + (dx > 0 ? 10 : -10), ySpine);
      } else {
        g.setColor(Value.MULTI_COLOR);
        g.fillOval(x0 - SPINE_DOT / 2, ySpine - SPINE_DOT / 2, SPINE_DOT, SPINE_DOT);
      }
    } else {
      int xSpine = (x0 + x1) / 2;
      GraphicsUtil.switchToWidth(g, Wire.WIDTH);
      g.drawLine(x0, y0, xSpine, y0);
      int xi = x1;
      int yi = y1;
      for (int i = 1; i <= fanout; i++) {
        if (context.getShowState()) {
          g.setColor(state.getValue(Location.create(xi, yi)).getColor());
        }
        int ySpine = yi + (yi == y0 ? 0 : (yi < y0 ? 10 : -10));
        g.drawLine(xi, yi, xSpine, ySpine);
        xi += dx;
        yi += dy;
      }
      if (fanout >= 3) {
        GraphicsUtil.switchToWidth(g, SPINE_WIDTH);
        g.setColor(Value.MULTI_COLOR);
        g.drawLine(
            xSpine, y1 + (dy > 0 ? 10 : -10), xSpine, y1 + (fanout - 1) * dy + (dy > 0 ? 10 : -10));
      } else {
        g.setColor(Value.MULTI_COLOR);
        g.fillOval(xSpine - SPINE_DOT / 2, y0 - SPINE_DOT / 2, SPINE_DOT, SPINE_DOT);
      }
    }
    GraphicsUtil.switchToWidth(g, 1);
  }

  static void drawLines(ComponentDrawContext context, SplitterAttributes attrs, Location origin) {
    boolean showState = context.getShowState();
    CircuitState state = showState ? context.getCircuitState() : null;
    if (state == null) showState = false;

    SplitterParameters parms = attrs.getParameters();
    int x0 = origin.getX();
    int y0 = origin.getY();
    int x = x0 + parms.getEnd0X();
    int y = y0 + parms.getEnd0Y();
    int dx = parms.getEndToEndDeltaX();
    int dy = parms.getEndToEndDeltaY();
    int dxEndSpine = parms.getEndToSpineDeltaX();
    int dyEndSpine = parms.getEndToSpineDeltaY();

    Graphics g = context.getGraphics();
    Color oldColor = g.getColor();
    GraphicsUtil.switchToWidth(g, Wire.WIDTH);
    for (int i = 0, n = attrs.fanout; i < n; i++) {
      if (showState) {
        Value val = state.getValue(Location.create(x, y));
        g.setColor(val.getColor());
      }
      g.drawLine(x, y, x + dxEndSpine, y + dyEndSpine);
      x += dx;
      y += dy;
    }
    GraphicsUtil.switchToWidth(g, SPINE_WIDTH);
    g.setColor(Value.MULTI_COLOR);
    int spine0x = x0 + parms.getSpine0X();
    int spine0y = y0 + parms.getSpine0Y();
    int spine1x = x0 + parms.getSpine1X();
    int spine1y = y0 + parms.getSpine1Y();
    if (spine0x == spine1x && spine0y == spine1y) { // centered
      int fanout = attrs.fanout;
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
