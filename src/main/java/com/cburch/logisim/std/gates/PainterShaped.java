/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.std.gates;

import com.cburch.logisim.data.Direction;
import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.util.GraphicsUtil;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.util.HashMap;

public class PainterShaped {
  private static GeneralPath computeShield(int width, int height) {
    GeneralPath base;
    if (width < 40) {
      base = SHIELD_NARROW;
    } else if (width < 60) {
      base = SHIELD_MEDIUM;
    } else {
      base = SHIELD_WIDE;
    }

    if (height <= width) { // no wings
      return base;
    } else { // we need to add wings
      int wingHeight = (height - width) / 2;
      int dx = Math.min(20, wingHeight / 4);

      final var path = new GeneralPath();
      path.moveTo(-width, -height / 2);
      path.quadTo(-width + dx, -(width + height) / 4, -width, -width / 2);
      path.append(base, true);
      path.quadTo(-width + dx, (width + height) / 4, -width, height / 2);
      return path;
    }
  }

  private static int[] getInputLineLengths(GateAttributes attrs, AbstractGate factory) {
    final var inputs = attrs.inputs;
    final var mainHeight = (Integer) attrs.size.getValue();
    Integer key = inputs * 31 + mainHeight;
    Object ret = INPUT_LENGTHS.get(key);
    if (ret != null) {
      return (int[]) ret;
    }

    final var facing = attrs.facing;
    if (facing != Direction.EAST) {
      attrs = (GateAttributes) attrs.clone();
      attrs.facing = Direction.EAST;
    }

    final var lengths = new int[inputs];
    INPUT_LENGTHS.put(key, lengths);
    final var loc0 = OrGate.FACTORY.getInputOffset(attrs, 0);
    final var locn = OrGate.FACTORY.getInputOffset(attrs, inputs - 1);
    int totalHeight = 10 + loc0.manhattanDistanceTo(locn);
    if (totalHeight < mainHeight) totalHeight = mainHeight;

    final var path = computeShield(mainHeight, totalHeight);
    for (int i = 0; i < inputs; i++) {
      final var loci = OrGate.FACTORY.getInputOffset(attrs, i);
      final var p = new Point2D.Float(loci.getX() + 1, loci.getY());
      int iters = 0;
      while (path.contains(p) && iters < 15) {
        iters++;
        p.setLocation(p.getX() + 1, p.getY());
      }
      if (iters >= 15) iters = 0;
      lengths[i] = iters;
    }
    return lengths;
  }

  static void paintAnd(InstancePainter painter, int width, int height) {
    final var g = painter.getGraphics();
    GraphicsUtil.switchToWidth(g, 2);
    int[] xp = new int[] {-width / 2, -width + 1, -width + 1, -width / 2};
    int[] yp = new int[] {-width / 2, -width / 2, width / 2, width / 2};
    GraphicsUtil.drawCenteredArc(g, -width / 2, 0, width / 2, -90, 180);

    g.drawPolyline(xp, yp, 4);
    if (height > width) {
      g.drawLine(-width + 1, -height / 2, -width + 1, height / 2);
    }
  }

  static void paintInputLines(InstancePainter painter, AbstractGate factory) {
    final var loc = painter.getLocation();
    final var printView = painter.isPrintView();
    final var attrs = (GateAttributes) painter.getAttributeSet();
    final var facing = attrs.facing;
    final var inputs = attrs.inputs;
    final var negated = attrs.negated;

    final var lengths = getInputLineLengths(attrs, factory);
    if (painter.getInstance() == null) { // drawing ghost - negation bubbles
      // only
      for (var i = 0; i < inputs; i++) {
        final var iNegated = ((negated >> i) & 1) == 1;
        if (iNegated) {
          final var offs = factory.getInputOffset(attrs, i);
          final var loci = loc.translate(offs.getX(), offs.getY());
          final var cent = loci.translate(facing, lengths[i] + 5);
          painter.drawDongle(cent.getX(), cent.getY());
        }
      }
    } else {
      final var g = painter.getGraphics();
      final var baseColor = g.getColor();
      GraphicsUtil.switchToWidth(g, 3);
      for (int i = 0; i < inputs; i++) {
        final var offs = factory.getInputOffset(attrs, i);
        final var src = loc.translate(offs.getX(), offs.getY());
        final var len = lengths[i];
        if (len != 0 && (!printView || painter.isPortConnected(i + 1))) {
          if (painter.getShowState()) {
            final var val = painter.getPortValue(i + 1);
            g.setColor(val.getColor());
          } else {
            g.setColor(baseColor);
          }
          final var dst = src.translate(facing, len);
          g.drawLine(src.getX(), src.getY(), dst.getX(), dst.getY());
        }
        if (((negated >> i) & 1) == 1) {
          final var cent = src.translate(facing, lengths[i] + 5);
          g.setColor(baseColor);
          painter.drawDongle(cent.getX(), cent.getY());
          GraphicsUtil.switchToWidth(g, 3);
        }
      }
    }
  }

  static void paintNot(InstancePainter painter) {
    final var g = painter.getGraphics();
    GraphicsUtil.switchToWidth(g, 2);
    if (painter.getAttributeValue(NotGate.ATTR_SIZE) == NotGate.SIZE_NARROW) {
      GraphicsUtil.switchToWidth(g, 2);
      int[] xp = new int[4];
      int[] yp = new int[4];
      xp[0] = -7;
      yp[0] = -1;
      xp[1] = -19;
      yp[1] = -6;
      xp[2] = -19;
      yp[2] = 6;
      xp[3] = -7;
      yp[3] = 1;
      g.drawPolyline(xp, yp, 4);
      g.drawOval(-6, -3, 6, 6);
    } else {
      int[] xp = new int[4];
      int[] yp = new int[4];
      xp[0] = -10;
      yp[0] = 0;
      xp[1] = -29;
      yp[1] = -7;
      xp[2] = -29;
      yp[2] = 7;
      xp[3] = -10;
      yp[3] = 0;
      g.drawPolyline(xp, yp, 4);
      g.drawOval(-9, -4, 9, 9);
    }
  }

  static void paintOr(InstancePainter painter, int width, int height) {
    final var g = painter.getGraphics();
    GraphicsUtil.switchToWidth(g, 2);
    GeneralPath path;
    if (width < 40) {
      path = PATH_NARROW;
    } else if (width < 60) {
      path = PATH_MEDIUM;
    } else {
      path = PATH_WIDE;
    }
    ((Graphics2D) g).draw(path);
    if (height > width) {
      paintShield(g, 0, width, height);
    }
  }

  private static void paintShield(Graphics g, int xlate, int width, int height) {
    GraphicsUtil.switchToWidth(g, 2);
    g.translate(xlate, 0);
    ((Graphics2D) g).draw(computeShield(width, height));
    g.translate(-xlate, 0);
  }

  static void paintXor(InstancePainter painter, int width, int height) {
    final var g = painter.getGraphics();
    paintOr(painter, width - 10, width - 10);
    paintShield(g, -10, width - 10, height);
  }

  private static final GeneralPath PATH_NARROW;

  private static final GeneralPath PATH_MEDIUM;

  private static final GeneralPath PATH_WIDE;

  private static final GeneralPath SHIELD_NARROW;

  private static final GeneralPath SHIELD_MEDIUM;

  private static final GeneralPath SHIELD_WIDE;

  static {
    PATH_NARROW = new GeneralPath();
    PATH_NARROW.moveTo(0, 0);
    PATH_NARROW.quadTo(-10, -15, -30, -15);
    PATH_NARROW.quadTo(-22, 0, -30, 15);
    PATH_NARROW.quadTo(-10, 15, 0, 0);
    PATH_NARROW.closePath();

    PATH_MEDIUM = new GeneralPath();
    PATH_MEDIUM.moveTo(0, 0);
    PATH_MEDIUM.quadTo(-20, -25, -50, -25);
    PATH_MEDIUM.quadTo(-37, 0, -50, 25);
    PATH_MEDIUM.quadTo(-20, 25, 0, 0);
    PATH_MEDIUM.closePath();

    PATH_WIDE = new GeneralPath();
    PATH_WIDE.moveTo(0, 0);
    PATH_WIDE.quadTo(-25, -35, -70, -35);
    PATH_WIDE.quadTo(-50, 0, -70, 35);
    PATH_WIDE.quadTo(-25, 35, 0, 0);
    PATH_WIDE.closePath();

    SHIELD_NARROW = new GeneralPath();
    SHIELD_NARROW.moveTo(-30, -15);
    SHIELD_NARROW.quadTo(-22, 0, -30, 15);

    SHIELD_MEDIUM = new GeneralPath();
    SHIELD_MEDIUM.moveTo(-50, -25);
    SHIELD_MEDIUM.quadTo(-37, 0, -50, 25);

    SHIELD_WIDE = new GeneralPath();
    SHIELD_WIDE.moveTo(-70, -35);
    SHIELD_WIDE.quadTo(-50, 0, -70, 35);
  }

  private static final HashMap<Integer, int[]> INPUT_LENGTHS = new HashMap<>();

  private PainterShaped() {}
}
