/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.util;

import com.cburch.draw.util.TextMetrics;
import com.cburch.logisim.circuit.RadixOption;
import com.cburch.logisim.data.Value;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;

public final class GraphicsUtil {

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

  private GraphicsUtil() {
    throw new IllegalStateException("Utility class. No instantiation allowed.");
  }

  public static void drawArrow(Graphics g, int x0, int y0, int x1, int y1, int headLength, int headAngle) {
    final var offs = headAngle * Math.PI / 180.0;
    final var angle = Math.atan2(y0 - y1, x0 - x1);
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
    final var oldColor = g.getColor();
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

  public static void drawCenteredText(Graphics g, Font font, String text, int x, int y, Color fg, Color bg) {
    drawText(g, text, x, y, H_CENTER, V_CENTER);
  }

  public static void drawCenteredValue(Graphics2D gfx, Value value, RadixOption radix, int x, int y) {
    final var valueString = radix.toString(value);
    final var radixIdentifier = radix.getIndexChar();
    final var fontMetrics = gfx.getFontMetrics();
    final var valueBounds = fontMetrics.getStringBounds(valueString, gfx);
    gfx.drawString(valueString, x - (int) (valueBounds.getWidth() / 2), y + (int) (valueBounds.getHeight() / 2));
    gfx.scale(0.7, 0.7);
    final var radixBounds = fontMetrics.getStringBounds(radixIdentifier, gfx);
    final var currentColor = gfx.getColor();
    gfx.setColor(Color.BLUE);
    final var radixXpos = x + (valueBounds.getWidth() / 2) + 1;
    final var radixYpos = y + valueBounds.getHeight() - (radixBounds.getHeight() / 3);
    gfx.drawString(radixIdentifier, (int) (radixXpos / 0.7), (int) (radixYpos / 0.7));
    gfx.scale(1 / 0.7, 1 / 0.7);
    gfx.setColor(currentColor);
  }

  public static void drawCenteredColoredText(Graphics g, String text, Color fg, Color bg, int x, int y) {
    drawText(g, text, x, y, H_CENTER, V_CENTER, fg, bg);
  }

  public static Rectangle getTextCursor(Graphics g, String text, int x, int y, int pos, int halign, int valign) {
    final var layout = textLayout(g, text);
    final var offset = Math.max(0, Math.min(pos, text.length()));
    final var line = lineAtOffset(text, offset);
    final var top = textTop(y, layout.ascent(), layout.height(), layout.lines().length, valign);
    final var left = textLeft(x, layout.widths()[line.index()], halign);
    final var prefix = text.substring(line.start(), offset);
    return new Rectangle(
        left + new TextMetrics(g, prefix).width,
        top + line.index() * layout.lineHeight(),
        1,
        layout.lineHeight());
  }

  public static int getTextPosition(Graphics g, String text, int x, int y, int halign, int valign) {
    final var layout = textLayout(g, text);
    final var top = textTop(0, layout.ascent(), layout.height(), layout.lines().length, valign);
    final var lineIndex =
        Math.max(0, Math.min((y - top) / layout.lineHeight(), layout.lines().length - 1));
    final var line = layout.lines()[lineIndex];
    x -= textLeft(0, layout.widths()[lineIndex], halign);
    var last = 0;
    final var font = g.getFont();
    final var fr = ((Graphics2D) g).getFontRenderContext();
    for (var i = 0; i < line.length(); i++) {
      final var cur = (int) font.getStringBounds(line.substring(0, i + 1), fr).getWidth();
      if (x <= (last + cur) / 2) {
        return lineStart(text, lineIndex) + i;
      }
      last = cur;
    }
    return lineStart(text, lineIndex) + line.length();
  }

  public static List<Rectangle> getTextSelectionBounds(
      Graphics g, String text, int x, int y, int start, int end, int halign, int valign) {
    final var result = new ArrayList<Rectangle>();
    final var first = Math.max(0, Math.min(Math.min(start, end), text.length()));
    final var last = Math.max(0, Math.min(Math.max(start, end), text.length()));
    if (first == last) return result;

    final var layout = textLayout(g, text);
    var lineStart = 0;
    for (var i = 0; i < layout.lines().length; i++) {
      final var lineEnd = lineStart + layout.lines()[i].length();
      final var selectionStart = Math.max(first, lineStart);
      final var selectionEnd = Math.min(last, lineEnd);
      if (selectionStart < selectionEnd) {
        final var from = getTextCursor(g, text, x, y, selectionStart, halign, valign);
        final var to = getTextCursor(g, text, x, y, selectionEnd, halign, valign);
        result.add(new Rectangle(from.x, from.y, Math.max(1, to.x - from.x), from.height));
      }
      lineStart = lineEnd + 1;
    }
    return result;
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
    final var oldfont = g.getFont();
    if (font != null) g.setFont(font);
    drawText(g, text, x, y, halign, valign, fg, bg);
    if (font != null) g.setFont(oldfont);
  }

  public static void drawText(
      Graphics g, Font font, String text, int x, int y, int halign, int valign) {
    final var oldfont = g.getFont();
    if (font != null) g.setFont(font);
    drawText(g, text, x, y, halign, valign);
    if (font != null) g.setFont(oldfont);
  }

  public static void drawText(Graphics g, String text, int x, int y, int halign, int valign) {
    if (text.length() == 0) return;
    final var layout = textLayout(g, text);
    final var top = textTop(y, layout.ascent(), layout.height(), layout.lines().length, valign);
    for (var i = 0; i < layout.lines().length; i++) {
      g.drawString(
          layout.lines()[i],
          textLeft(x, layout.widths()[i], halign),
          top + layout.ascent() + i * layout.lineHeight());
    }
  }

  public static void drawText(Graphics g, String text, int x, int y, int halign, int valign, Color fg, Color bg) {
    if (text.length() == 0) return;
    final var bd = getTextBounds(g, text, x, y, halign, valign);
    if (g instanceof Graphics2D g2d) {
      g2d.setPaint(bg);
      g.fillRect(bd.x, bd.y, bd.width, bd.height);
      g2d.setPaint(fg);
    }
    drawText(g, text, x, y, halign, valign);
  }

  public static void outlineText(Graphics g, String text, int x, int y, Color fg, Color bg) {
    final var g2 = (Graphics2D) g;
    final var glyphVector = g2.getFont().createGlyphVector(g2.getFontRenderContext(), text);
    final var textShape = glyphVector.getOutline();
    final var transform = g2.getTransform();
    g2.translate(x, y);
    g2.setColor(bg);
    g2.draw(textShape);
    g2.setColor(fg);
    g2.fill(textShape);
    g2.setTransform(transform);
  }

  public static Rectangle getTextBounds(Graphics g, Font font, String text, int x, int y, int halign, int valign) {
    if (g == null) return new Rectangle(x, y, 0, 0);
    final var oldfont = g.getFont();
    if (font != null) g.setFont(font);
    final var ret = getTextBounds(g, text, x, y, halign, valign);
    if (font != null) g.setFont(oldfont);
    return ret;
  }

  public static Rectangle getTextBounds(Graphics g, String text, int x, int y, int halign, int valign) {
    if (g == null) return new Rectangle(x, y, 0, 0);

    final var layout = textLayout(g, text);
    final var width = layout.width();
    final var height = layout.height();

    return new Rectangle(
        textLeft(x, width, halign),
        textTop(y, layout.ascent(), height, layout.lines().length, valign),
        width,
        height);
  }

  private record LineOffset(int index, int start) {}

  private record TextLayout(
      String[] lines, int[] widths, int width, int ascent, int lineHeight, int height) {}

  private static LineOffset lineAtOffset(String text, int offset) {
    var start = 0;
    var index = 0;
    while (true) {
      final var newline = text.indexOf('\n', start);
      if (newline < 0 || offset <= newline) return new LineOffset(index, start);
      start = newline + 1;
      index++;
    }
  }

  private static int lineStart(String text, int lineIndex) {
    var start = 0;
    for (var i = 0; i < lineIndex; i++) start = text.indexOf('\n', start) + 1;
    return start;
  }

  private static int textLeft(int x, int width, int halign) {
    return switch (halign) {
      case H_CENTER -> x - width / 2;
      case H_RIGHT -> x - width;
      default -> x;
    };
  }

  private static TextLayout textLayout(Graphics g, String text) {
    final var lines = text.split("\\n", -1);
    final var widths = new int[lines.length];
    var width = 0;
    for (var i = 0; i < lines.length; i++) {
      widths[i] = new TextMetrics(g, lines[i]).width;
      width = Math.max(width, widths[i]);
    }
    final var metrics = new TextMetrics(g, lines[0]);
    return new TextLayout(
        lines, widths, width, metrics.ascent, metrics.height, metrics.height * lines.length);
  }

  private static int textTop(int y, int ascent, int height, int lineCount, int valign) {
    return switch (valign) {
      case V_CENTER -> y - (lineCount == 1 ? ascent / 2 : height / 2);
      case V_CENTER_OVERALL -> y - height / 2;
      case V_BASELINE -> y - ascent;
      case V_BOTTOM -> y - height;
      default -> y;
    };
  }

  public static void switchToWidth(Graphics gfx, int width) {
    if (gfx instanceof Graphics2D g2d) {
      g2d.setStroke(new BasicStroke(width));
    }
  }

}
