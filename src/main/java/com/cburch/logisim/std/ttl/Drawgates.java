/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
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
      final var xp = new int[] {x - width / 2, x - width, x - width, x - width / 2};
      final var yp = new int[] {y - width / 2, y - width / 2, y + width / 2, y + width / 2};
      GraphicsUtil.drawCenteredArc(g, x - width / 2, y, width / 2, -90, 180);

      g.drawPolyline(xp, yp, 4);
      if (height > width) {
        g.drawLine(x - width, y - height / 2, x - width, y + height / 2);
      }
    }
  }

  static void paintBuffer(Graphics g, int x, int y, int width, int height) {
    final var xp = new int[4];
    final var yp = new int[4];
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
      Graphics gfx,
      int rightPinX,
      int y,
      int inputX,
      int outputY,
      int portHeight,
      boolean up,
      boolean rightToLeft,
      int height) {
    var xPoints =
        (!rightToLeft)
            ? new int[] {
              rightPinX, rightPinX, rightPinX - 10, rightPinX - 10, inputX
            } // rightmost input
            : new int[] {
              rightPinX - 20, rightPinX - 20, rightPinX - 10, rightPinX - 10, inputX
            }; // leftmost input if !rightToLeft

    var yPoints =
        (!up)
            ? new int[] {
              y + height - AbstractTtlGate.PIN_HEIGHT,
              y + height - AbstractTtlGate.PIN_HEIGHT - (10 - AbstractTtlGate.PIN_HEIGHT),
              y + height - AbstractTtlGate.PIN_HEIGHT - (10 - AbstractTtlGate.PIN_HEIGHT),
              outputY + portHeight / 3,
              outputY + portHeight / 3
            }
            : new int[] {
              y + AbstractTtlGate.PIN_HEIGHT,
              y + AbstractTtlGate.PIN_HEIGHT + (10 - AbstractTtlGate.PIN_HEIGHT),
              y + AbstractTtlGate.PIN_HEIGHT + (10 - AbstractTtlGate.PIN_HEIGHT),
              outputY - portHeight / 3,
              outputY - portHeight / 3
            };
    gfx.drawPolyline(xPoints, yPoints, 5);

    xPoints =
        (!rightToLeft)
            ? new int[] {rightPinX - 20, rightPinX - 20, inputX} // leftmost input
            : new int[] {rightPinX, rightPinX, inputX}; // rightmost input if rightToLeft
    yPoints =
        (!up)
            ? new int[] {
              y + height - AbstractTtlGate.PIN_HEIGHT,
              outputY - portHeight / 3,
              outputY - portHeight / 3
            }
            : new int[] {
              y + AbstractTtlGate.PIN_HEIGHT, outputY + portHeight / 3, outputY + portHeight / 3
            };
    gfx.drawPolyline(xPoints, yPoints, 3);
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
      final var xp = new int[4];
      final var yp = new int[4];
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

  static void paintOr(
      Graphics g, int x, int y, int width, int height, boolean negated, boolean rightToLeft) {
    final var offset = rightToLeft ? -4 : 0;
    if (negated) paintNegatedOutput(g, x + offset, y);
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
    final var xPoints = new int[] {xoutput, xpin, xpin};
    final var yPoints =
        !up
            ? new int[] {youtput, youtput, y + height - AbstractTtlGate.PIN_HEIGHT}
            : new int[] {youtput, youtput, y + AbstractTtlGate.PIN_HEIGHT};
    g.drawPolyline(xPoints, yPoints, 3);
  }

  static void paintPortNames(
      InstancePainter painter, int x, int y, int height, String[] portNames) {
    final var gfx = painter.getGraphics();
    final var portsPerRow = portNames.length / 2;
    gfx.drawRect(
        x + 10,
        y + AbstractTtlGate.PIN_HEIGHT + 10,
        portNames.length * 10,
        height - 2 * AbstractTtlGate.PIN_HEIGHT - 20);
    for (var i = 0; i < 2; i++) {
      for (var j = 0; j < portsPerRow; j++) {
        GraphicsUtil.drawCenteredText(
            gfx,
            portNames[j + (i * portsPerRow)],
            i == 0 ? x + 10 + j * 20 : x + 20 * portsPerRow - j * 20 + 10,
            y
                + height
                - AbstractTtlGate.PIN_HEIGHT
                - 7
                - i * (height - 2 * AbstractTtlGate.PIN_HEIGHT - 11));
      }
    }
  }

  static void paintSingleInputgate(
      Graphics g, int xpin, int y, int xinput, int youtput, boolean up, int height) {
    final var xPoints = new int[] {xpin, xpin, xinput};
    final var yPoints =
        !up
            ? new int[] {y + height - AbstractTtlGate.PIN_HEIGHT, youtput, youtput}
            : new int[] {y + AbstractTtlGate.PIN_HEIGHT, youtput, youtput};
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

  /**
   * Draws the schematic symbol for an open-collector/open-drain output
   *
   * @param g Graphics context to draw to
   * @param x x position of symbol
   * @param y y position of symbol
   */
  static void paintOpenCollector(Graphics g, int x, int y) {
    g.drawPolyline(new int[] {x, x + 3, x + 6, x + 3, x}, new int[] {y, y + 3, y, y - 3, y}, 5);
    g.drawLine(x, y + 3, x + 6, y + 3);
  }
}
