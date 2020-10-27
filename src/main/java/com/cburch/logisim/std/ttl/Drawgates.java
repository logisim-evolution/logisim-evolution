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

package com.cburch.logisim.std.ttl;

import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.prefs.AppPreferences;
import com.cburch.logisim.util.GraphicsUtil;
import java.awt.Graphics;

public class Drawgates {
  // x and y correspond to output port coordinates
  public static void paintAnd(Graphics g, int x, int y, int width, int height, boolean negated) {
    if (negated) paintNegatedOutput(g, x, y);
    if (AppPreferences.GATE_SHAPE.get().equals(AppPreferences.SHAPE_RECTANGULAR)) {
      g.drawRect(x - width, y - height / 2, width, height);
      GraphicsUtil.drawCenteredText(g, "&", x - width / 2, y);
    } else {
      int[] xp = new int[] {x - width / 2, x - width, x - width, x - width / 2};
      int[] yp = new int[] {y - width / 2, y - width / 2, y + width / 2, y + width / 2};
      GraphicsUtil.drawCenteredArc(g, x - width / 2, y, width / 2, -90, 180);

      g.drawPolyline(xp, yp, 4);
      if (height > width) {
        g.drawLine(x - width, y - height / 2, x - width, y + height / 2);
      }
    }
  }

  static void paintBuffer(Graphics g, int x, int y, int width, int height) {
    int[] xp = new int[4];
    int[] yp = new int[4];
    xp[0] = x - 4;
    yp[0] = y;
    xp[1] = x - width;
    yp[1] = y - height / 2;
    xp[2] = x - width;
    yp[2] = y + height / 2;
    xp[3] = x - 4;
    yp[3] = y;
    g.drawPolyline(xp, yp, 4);
  }

  static void paintDoubleInputgate(
      Graphics g,
      int xrightpin,
      int y,
      int xinput,
      int youtput,
      int portheight,
      boolean up,
      boolean rightToLeft,
      int height) {
    int[] yPoints, xPoints;
    // rightmost input
    if (!rightToLeft)
      xPoints = new int[] { xrightpin, xrightpin, xrightpin - 10, xrightpin - 10, xinput };
    else // leftmost input if !rightToLeft
      xPoints = new int[] { xrightpin - 20, xrightpin - 20, xrightpin - 10, xrightpin - 10, xinput };   
    if (!up)
      yPoints =
          new int[] {
            y + height - AbstractTtlGate.pinheight,
            y + height - AbstractTtlGate.pinheight - (10 - AbstractTtlGate.pinheight),
            y + height - AbstractTtlGate.pinheight - (10 - AbstractTtlGate.pinheight),
            youtput + portheight / 3,
            youtput + portheight / 3
          };
    else
      yPoints =
          new int[] {
            y + AbstractTtlGate.pinheight,
            y + AbstractTtlGate.pinheight + (10 - AbstractTtlGate.pinheight),
            y + AbstractTtlGate.pinheight + (10 - AbstractTtlGate.pinheight),
            youtput - portheight / 3,
            youtput - portheight / 3
          };
    g.drawPolyline(xPoints, yPoints, 5);
    // leftmost input
    if (!rightToLeft)
      xPoints = new int[] { xrightpin - 20, xrightpin - 20, xinput };
    else // rightmost input if rightToLeft
      xPoints = new int[] { xrightpin, xrightpin, xinput };
    if (!up)
      yPoints =
          new int[] {
            y + height - AbstractTtlGate.pinheight,
            youtput - portheight / 3,
            youtput - portheight / 3
          };
    else
      yPoints =
          new int[] {
            y + AbstractTtlGate.pinheight, youtput + portheight / 3, youtput + portheight / 3
          };
    g.drawPolyline(xPoints, yPoints, 3);
  }

  private static void paintNegatedOutput(Graphics g, int x, int y) {
    g.drawOval(x, y - 2, 4, 4);
  }

  static void paintNot(Graphics g, int x, int y, int width, int height) {
    paintNegatedOutput(g, x - 4, y);
    if (AppPreferences.GATE_SHAPE.get().equals(AppPreferences.SHAPE_RECTANGULAR)) {
      g.drawRect(x - width, y - (width - 4) / 2, width - 4, width - 4);
      GraphicsUtil.drawCenteredText(g, "1", x - 4 - (width - 4) / 2, y);
    } else {
      int[] xp = new int[4];
      int[] yp = new int[4];
      xp[0] = x - 4;
      yp[0] = y;
      xp[1] = x - width;
      yp[1] = y - height / 2;
      xp[2] = x - width;
      yp[2] = y + height / 2;
      xp[3] = x - 4;
      yp[3] = y;
      g.drawPolyline(xp, yp, 4);
    }
  }

  static void paintOr(Graphics g, int x, int y, int width, int height, boolean negated, boolean rightToLeft) {
    int offset = rightToLeft ? -4 : 0;
    if (negated) paintNegatedOutput(g, x+offset, y);
    if (AppPreferences.GATE_SHAPE.get().equals(AppPreferences.SHAPE_RECTANGULAR)) {
      if (!rightToLeft) {
        g.drawRect(x - width, y - height / 2, width, height);
        GraphicsUtil.drawCenteredText(g, "\u2265" + "1", x - width / 2, y);
      } else {
        g.drawRect(x, y - height / 2, width, height);
        GraphicsUtil.drawCenteredText(g, "\u2265" + "1", x + width / 2, y);
      }
    } else {
      if (!rightToLeft) {
        GraphicsUtil.drawCenteredArc(g, x - 14, y - 10, 17, -90, 54);
        GraphicsUtil.drawCenteredArc(g, x - 14, y + 10, 17, 90, -54);
        GraphicsUtil.drawCenteredArc(g, x - 28, y, 15, -27, 54);
      } else {
        GraphicsUtil.drawCenteredArc(g, x + 14, y - 10, 17, -90, -54);
        GraphicsUtil.drawCenteredArc(g, x + 14, y + 10, 17, 90, 54);
        GraphicsUtil.drawCenteredArc(g, x + 28, y, 15, 153, 54);
      }
    }
  }

  static void paintOutputgate(
      Graphics g, int xpin, int y, int xoutput, int youtput, boolean up, int height) {
    int[] yPoints, xPoints;
    xPoints = new int[] {xoutput, xpin, xpin};
    if (!up) yPoints = new int[] {youtput, youtput, y + height - AbstractTtlGate.pinheight};
    else yPoints = new int[] {youtput, youtput, y + AbstractTtlGate.pinheight};
    g.drawPolyline(xPoints, yPoints, 3);
  }

  static void paintPortNames(
      InstancePainter painter, int x, int y, int height, String[] portnames) {
    Graphics g = painter.getGraphics();
    g.drawRect(
        x + 10,
        y + AbstractTtlGate.pinheight + 10,
        portnames.length * 10,
        height - 2 * AbstractTtlGate.pinheight - 20);
    for (int i = 0; i < 2; i++) {
      for (int j = 0; j < portnames.length / 2; j++) {
        GraphicsUtil.drawCenteredText(
            g,
            portnames[j + (i * 7)],
            i == 0 ? x + 10 + j * 20 : x + 160 - j * 20 - 10,
            y
                + height
                - AbstractTtlGate.pinheight
                - 7
                - i * (height - 2 * AbstractTtlGate.pinheight - 11));
      }
    }
  }

  static void paintSingleInputgate(
      Graphics g, int xpin, int y, int xinput, int youtput, boolean up, int height) {
    int[] yPoints, xPoints;
    xPoints = new int[] {xpin, xpin, xinput};
    if (!up) yPoints = new int[] {y + height - AbstractTtlGate.pinheight, youtput, youtput};
    else yPoints = new int[] {y + AbstractTtlGate.pinheight, youtput, youtput};
    g.drawPolyline(xPoints, yPoints, 3);
  }

  static void paintXor(Graphics g, int x, int y, int width, int height, boolean negated) {
    if (AppPreferences.GATE_SHAPE.get().equals(AppPreferences.SHAPE_RECTANGULAR)) {
      if (negated) paintNegatedOutput(g, x, y);
      g.drawRect(x - width, y - height / 2, width, height);
      GraphicsUtil.drawCenteredText(g, "=1", x - width / 2, y);
    } else {
      paintOr(g, x, y, width, height, negated, false);
      GraphicsUtil.drawCenteredArc(g, x - 32, y, 15, -27, 54);
    }
  }
}
