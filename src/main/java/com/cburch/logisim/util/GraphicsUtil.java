/*
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

package com.cburch.logisim.util;

import com.cburch.draw.util.TextMetrics;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.geom.AffineTransform;

public class GraphicsUtil {
  public static void drawArrow(
      Graphics g, int x0, int y0, int x1, int y1, int headLength, int headAngle) {
    double offs = headAngle * Math.PI / 180.0;
    double angle = Math.atan2(y0 - y1, x0 - x1);
    int[] xs = {
      x1 + (int) (headLength * Math.cos(angle + offs)),
      x1,
      x1 + (int) (headLength * Math.cos(angle - offs))
    };
    int[] ys = {
      y1 + (int) (headLength * Math.sin(angle + offs)),
      y1,
      y1 + (int) (headLength * Math.sin(angle - offs))
    };
    g.drawLine(x0, y0, x1, y1);
    g.drawPolyline(xs, ys, 3);
  }

  public static void drawArrow2(Graphics g, int x0, int y0, int x1, int y1, int x2, int y2) {
    int[] xs = {x0, x1, x2};
    int[] ys = {y0, y1, y2};
    GraphicsUtil.switchToWidth(g, 7);
    g.drawPolyline(xs, ys, 3);
    Color oldColor = g.getColor();
    g.setColor(Color.WHITE);
    GraphicsUtil.switchToWidth(g, 3);
    g.drawPolyline(xs, ys, 3);
    g.setColor(oldColor);
    GraphicsUtil.switchToWidth(g, 1);
  }

  public static void drawCenteredArc(Graphics g, int x, int y, int r, int start, int dist) {
    g.drawArc(x - r, y - r, 2 * r, 2 * r, start, dist);
  }

  public static void drawCenteredText(Graphics g, String text, int x, int y) {
    drawText(g, text, x, y, H_CENTER, V_CENTER);
  }

  public static void drawCenteredText(
      Graphics g, Font font, String text, int x, int y, Color fg, Color bg) {
    drawText(g, text, x, y, H_CENTER, V_CENTER);
  }

  public static void drawCenteredColoredText(
      Graphics g, String text, Color fg, Color bg, int x, int y) {
    drawText(g, text, x, y, H_CENTER, V_CENTER, fg, bg);
  }

  public static Rectangle getTextCursor(
      Graphics g, String text, int x, int y, int pos, int halign, int valign) {
    Rectangle r = getTextBounds(g, text, x, y, halign, valign);
    if (pos > 0) r.x += new TextMetrics(g, text.substring(0, pos)).width;
    r.width = 1;
    return r;
  }

  public static int getTextPosition(Graphics g, String text, int x, int y, int halign, int valign) {
    Rectangle r = getTextBounds(g, text, 0, 0, halign, valign);
    x -= r.x;
    int last = 0;
    Font font = g.getFont();
    FontRenderContext fr = ((Graphics2D) g).getFontRenderContext();
    for (int i = 0; i < text.length(); i++) {
      int cur = (int) font.getStringBounds(text.substring(0, i + 1), fr).getWidth();
      if (x <= (last + cur) / 2) {
        return i;
      }
      last = cur;
    }
    return text.length();
  }

  public static void drawText(
      Graphics g,
      Font font,
      String text,
      int x,
      int y,
      int halign,
      int valign,
      Color fg,
      Color bg) {
    Font oldfont = g.getFont();
    if (font != null) g.setFont(font);
    drawText(g, text, x, y, halign, valign, fg, bg);
    if (font != null) g.setFont(oldfont);
  }

  public static void drawText(
      Graphics g, Font font, String text, int x, int y, int halign, int valign) {
    Font oldfont = g.getFont();
    if (font != null) g.setFont(font);
    drawText(g, text, x, y, halign, valign);
    if (font != null) g.setFont(oldfont);
  }

  public static void drawText(Graphics g, String text, int x, int y, int halign, int valign) {
    if (text.length() == 0) return;
    Rectangle bd = getTextBounds(g, text, x, y, halign, valign);
    TextMetrics tm = new TextMetrics(g, text);
    g.drawString(text, bd.x, bd.y + tm.ascent);
  }

  public static void drawText(
      Graphics g, String text, int x, int y, int halign, int valign, Color fg, Color bg) {
    if (text.length() == 0) return;
    Rectangle bd = getTextBounds(g, text, x, y, halign, valign);
    TextMetrics tm = new TextMetrics(g, text);
    if (g instanceof Graphics2D) {
      ((Graphics2D) g).setPaint(bg);
      g.fillRect(bd.x, bd.y, bd.width, bd.height);
      ((Graphics2D) g).setPaint(fg);
    }
    g.drawString(text, bd.x, bd.y + tm.ascent);
  }

  public static void outlineText(Graphics g, String text, int x, int y, Color fg, Color bg) {
    Graphics2D g2 = (Graphics2D) g;
    GlyphVector glyphVector = g2.getFont().createGlyphVector(g2.getFontRenderContext(), text);
    Shape textShape = glyphVector.getOutline();
    AffineTransform transform = g2.getTransform();
    g2.translate(x, y);
    g2.setColor(bg);
    g2.draw(textShape);
    g2.setColor(fg);
    g2.fill(textShape);
    g2.setTransform(transform);
  }

  public static Rectangle getTextBounds(
      Graphics g, Font font, String text, int x, int y, int halign, int valign) {
    if (g == null) return new Rectangle(x, y, 0, 0);
    Font oldfont = g.getFont();
    if (font != null) g.setFont(font);
    Rectangle ret = getTextBounds(g, text, x, y, halign, valign);
    if (font != null) g.setFont(oldfont);
    return ret;
  }

  public static Rectangle getTextBounds(
      Graphics g, String text, int x, int y, int halign, int valign) {
    if (g == null) return new Rectangle(x, y, 0, 0);
    TextMetrics tm = new TextMetrics(g, text);
    int width = tm.width;
    int ascent = tm.ascent;
    int height = tm.height;

    Rectangle ret = new Rectangle(x, y, width, height);
    switch (halign) {
      case H_CENTER:
        ret.translate(-(width / 2), 0);
        break;
      case H_RIGHT:
        ret.translate(-width, 0);
        break;
      default:
    }
    switch (valign) {
      case V_TOP:
        break;
      case V_CENTER:
        ret.translate(0, -(ascent / 2));
        break;
      case V_CENTER_OVERALL:
        ret.translate(0, -(height / 2));
        break;
      case V_BASELINE:
        ret.translate(0, -ascent);
        break;
      case V_BOTTOM:
        ret.translate(0, -height);
        break;
      default:
    }
    return ret;
  }

  public static void switchToWidth(Graphics g, int width) {
    if (g instanceof Graphics2D) {
      Graphics2D g2 = (Graphics2D) g;
      g2.setStroke(new BasicStroke((float) width));
    }
  }

  public static final int H_LEFT = -1;

  public static final int H_CENTER = 0;

  public static final int H_RIGHT = 1;
  public static final int V_TOP = -1;

  public static final int V_CENTER = 0;
  public static final int V_BASELINE = 1;
  public static final int V_BOTTOM = 2;

  public static final int V_CENTER_OVERALL = 3;

  public static final int CONTROL_WIDTH = 2;
  public static final int NEGATED_WIDTH = 2;
  public static final int DATA_SINGLE_WIDTH = 3;
  public static final int DATA_MULTI_WIDTH = 4;
}
