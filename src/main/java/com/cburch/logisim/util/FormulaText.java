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
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.gui.generic.TikZWriter;

import java.awt.BasicStroke;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/**
 * Draws a label line wrapped in {@code $...$} as a small logic formula. Any other text, including
 * an unrecognized command, stays ordinary text. TikZ export keeps the source string so LaTeX can
 * typeset it; SVG export draws the same formula as the canvas.
 */
public final class FormulaText {

  private static final String CDOT = "⋅";
  private static final String OPLUS = "⊕";
  private static final float SUBSCRIPT_SCALE = 0.7f;
  private static final int SUBSCRIPT_DROP_DIVISOR = 3;
  private static final int OVERLINE_GAP_DIVISOR = 8;
  private static final int OVERLINE_RULE = 1;

  private FormulaText() {
    throw new IllegalStateException("Utility class. No instantiation allowed.");
  }

  /** True when at least one line is a complete {@code $...$} formula. */
  public static boolean containsFormula(String text) {
    if (text == null || text.isEmpty()) return false;
    for (final var line : text.split("\\n", -1)) {
      if (parseMath(line) != null) return true;
    }
    return false;
  }

  public static void draw(Graphics g, String text, int x, int y, int halign, int valign) {
    final var block = layout(g, text);
    if (block == null) return;
    final var top = textTop(y, block.firstAscent, block.height, block.lines.length, valign);
    final var emitSource = emitLatexSource(g);
    var cursor = top;
    for (final var line : block.lines) {
      final var baseline = cursor + line.ascent;
      final var left = textLeft(x, line.width, halign);
      if (line.mathSource != null && emitSource) {
        g.drawString(line.mathSource, left, baseline);
      } else if (line.box != null) {
        line.box.draw(g, left, baseline);
      } else if (!line.raw.isEmpty()) {
        g.drawString(line.raw, left, baseline);
      }
      cursor += line.height;
    }
  }

  public static Rectangle getBounds(Graphics g, String text, int x, int y, int halign, int valign) {
    final var block = layout(g, text);
    if (block == null) return new Rectangle(x, y, 0, 0);
    final var top = textTop(y, block.firstAscent, block.height, block.lines.length, valign);
    return new Rectangle(textLeft(x, block.width, halign), top, block.width, block.height);
  }

  /** Measures a formula label without a component graphics context. */
  public static Bounds estimateBounds(String text, Font font, int halign, int valign) {
    final var image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
    final var graphics = image.createGraphics();
    if (font != null) graphics.setFont(font);
    final var bounds = getBounds(graphics, text, 0, 0, halign, valign);
    graphics.dispose();
    return Bounds.create(bounds);
  }

  private static boolean emitLatexSource(Graphics g) {
    return g instanceof TikZWriter writer && !writer.isSvgMode();
  }

  private static BlockLayout layout(Graphics g, String text) {
    if (g == null || text == null) return null;
    final var font = g.getFont();
    final var rawLines = text.split("\\n", -1);
    final var lines = new LineLayout[rawLines.length];
    final var leading = new TextMetrics(g, font, "Ay").leading;
    var formulaFound = false;
    var width = 0;
    var height = 0;
    for (var i = 0; i < rawLines.length; i++) {
      final var math = parseMath(rawLines[i]);
      if (math != null) {
        formulaFound = true;
        final var box = measure(math.node, g, font);
        final var lineHeight = box.ascent + box.descent + leading;
        lines[i] =
            new LineLayout(rawLines[i], math.source, box, box.width, box.ascent, box.descent, lineHeight);
      } else {
        final var metrics = new TextMetrics(g, font, rawLines[i]);
        lines[i] =
            new LineLayout(
                rawLines[i], null, null, metrics.width, metrics.ascent, metrics.descent, metrics.height);
      }
      width = Math.max(width, lines[i].width);
      height += lines[i].height;
    }
    if (!formulaFound || lines.length == 0) return null;
    return new BlockLayout(lines, width, height, lines[0].ascent);
  }

  private static Parsed parseMath(String line) {
    final var trimmed = line.trim();
    if (trimmed.length() < 3 || trimmed.charAt(0) != '$' || trimmed.charAt(trimmed.length() - 1) != '$') {
      return null;
    }
    final var cursor = new Cursor(trimmed.substring(1, trimmed.length() - 1));
    final var node = parseSequence(cursor, false);
    if (node == null || cursor.failed || cursor.hasNext()) return null;
    return new Parsed(trimmed, node);
  }

  private static Node parseSequence(Cursor cursor, boolean untilBrace) {
    final var children = new ArrayList<Node>();
    while (cursor.hasNext()) {
      final var ch = cursor.peek();
      if (ch == '}') return closeGroup(cursor, untilBrace, children);
      if (Character.isWhitespace(ch)) {
        cursor.next();
        continue;
      }
      if (ch == '_') {
        if (!appendSubscript(cursor, children)) return null;
        continue;
      }
      if (ch == '\\') {
        final var command = parseCommand(cursor);
        if (command == null) return null;
        children.add(command);
        continue;
      }
      if (isLiteral(ch)) {
        children.add(new TextNode(readLiteral(cursor)));
        continue;
      }
      cursor.fail();
      return null;
    }
    if (untilBrace || children.isEmpty()) {
      if (untilBrace) cursor.fail();
      return null;
    }
    return new RowNode(List.copyOf(children));
  }

  private static Node closeGroup(Cursor cursor, boolean untilBrace, List<Node> children) {
    if (!untilBrace || children.isEmpty()) {
      cursor.fail();
      return null;
    }
    cursor.next();
    return new RowNode(List.copyOf(children));
  }

  private static boolean appendSubscript(Cursor cursor, List<Node> children) {
    if (children.isEmpty()) {
      cursor.fail();
      return false;
    }
    cursor.next();
    if (!consumeOpeningBrace(cursor)) return false;
    final var sub = parseSequence(cursor, true);
    if (sub == null) return false;
    final var base = children.remove(children.size() - 1);
    children.add(new SubscriptNode(base, sub));
    return true;
  }

  private static Node parseCommand(Cursor cursor) {
    cursor.next();
    return switch (readCommandName(cursor)) {
      case "text" -> parseTextCommand(cursor);
      case "overline" -> parseOverlineCommand(cursor);
      case "cdot" -> new TextNode(CDOT);
      case "oplus" -> new TextNode(OPLUS);
      default -> {
        cursor.fail();
        yield null;
      }
    };
  }

  private static Node parseTextCommand(Cursor cursor) {
    if (!consumeOpeningBrace(cursor)) return null;
    final var raw = readRawText(cursor);
    if (raw == null) return null;
    return new TextNode(raw);
  }

  private static Node parseOverlineCommand(Cursor cursor) {
    if (!consumeOpeningBrace(cursor)) return null;
    final var inner = parseSequence(cursor, true);
    if (inner == null) return null;
    return new OverlineNode(inner);
  }

  private static boolean consumeOpeningBrace(Cursor cursor) {
    skipWhitespace(cursor);
    if (!cursor.hasNext() || cursor.peek() != '{') {
      cursor.fail();
      return false;
    }
    cursor.next();
    return true;
  }

  private static void skipWhitespace(Cursor cursor) {
    while (cursor.hasNext() && Character.isWhitespace(cursor.peek())) {
      cursor.next();
    }
  }

  private static String readCommandName(Cursor cursor) {
    final var name = new StringBuilder();
    while (cursor.hasNext() && Character.isLetter(cursor.peek())) {
      name.append(cursor.next());
    }
    return name.toString();
  }

  private static String readRawText(Cursor cursor) {
    final var raw = new StringBuilder();
    while (cursor.hasNext()) {
      final var ch = cursor.next();
      if (ch == '}') return raw.toString();
      raw.append(ch);
    }
    cursor.fail();
    return null;
  }

  private static String readLiteral(Cursor cursor) {
    final var raw = new StringBuilder();
    while (cursor.hasNext() && isLiteral(cursor.peek())) {
      raw.append(cursor.next());
    }
    return raw.toString();
  }

  private static boolean isLiteral(char ch) {
    return Character.isLetterOrDigit(ch) || ch == '+' || ch == '=' || ch == '(' || ch == ')';
  }

  private static Box measure(Node node, Graphics g, Font font) {
    if (node instanceof TextNode text) return measureText(text.value, g, font);
    if (node instanceof OverlineNode overline) return measureOverline(overline.inner, g, font);
    if (node instanceof SubscriptNode subscript) return measureSubscript(subscript, g, font);
    if (node instanceof RowNode row) return measureRow(row.children, g, font);
    throw new IllegalStateException("Unknown formula node");
  }

  private static Box measureText(String text, Graphics g, Font font) {
    final var metrics = new TextMetrics(g, font, text.isEmpty() ? null : text);
    final var width = text.isEmpty() ? 0 : metrics.width;
    return new TextBox(text, font, width, metrics.ascent, metrics.descent);
  }

  private static Box measureOverline(Node inner, Graphics g, Font font) {
    final var box = measure(inner, g, font);
    final var gap = Math.max(1, font.getSize() / OVERLINE_GAP_DIVISOR);
    final var ascent = box.ascent + gap + OVERLINE_RULE;
    return new OverlineBox(box, gap, box.width, ascent, box.descent);
  }

  private static Box measureSubscript(SubscriptNode node, Graphics g, Font font) {
    final var base = measure(node.base, g, font);
    final var sub = measure(node.sub, g, subscriptFont(font));
    final var drop = Math.max(1, base.ascent / SUBSCRIPT_DROP_DIVISOR);
    final var ascent = Math.max(base.ascent, sub.ascent - drop);
    final var descent = Math.max(base.descent, drop + sub.descent);
    return new SubscriptBox(base, sub, drop, base.width + sub.width, ascent, descent);
  }

  private static Box measureRow(List<Node> children, Graphics g, Font font) {
    final var boxes = new ArrayList<Box>(children.size());
    var width = 0;
    var ascent = 0;
    var descent = 0;
    for (final var child : children) {
      final var box = measure(child, g, font);
      boxes.add(box);
      width += box.width;
      ascent = Math.max(ascent, box.ascent);
      descent = Math.max(descent, box.descent);
    }
    return new RowBox(boxes, width, ascent, descent);
  }

  private static Font subscriptFont(Font font) {
    final var size = Math.max(1f, font.getSize2D() * SUBSCRIPT_SCALE);
    return font.deriveFont(size);
  }

  private static int textLeft(int x, int width, int halign) {
    return switch (halign) {
      case GraphicsUtil.H_CENTER -> x - width / 2;
      case GraphicsUtil.H_RIGHT -> x - width;
      default -> x;
    };
  }

  private static int textTop(int y, int ascent, int height, int lineCount, int valign) {
    return switch (valign) {
      case GraphicsUtil.V_CENTER -> y - (lineCount == 1 ? ascent / 2 : height / 2);
      case GraphicsUtil.V_CENTER_OVERALL -> y - height / 2;
      case GraphicsUtil.V_BASELINE -> y - ascent;
      case GraphicsUtil.V_BOTTOM -> y - height;
      default -> y;
    };
  }

  private sealed interface Node permits TextNode, OverlineNode, SubscriptNode, RowNode {}

  private record Parsed(String source, Node node) {}

  private record TextNode(String value) implements Node {}

  private record OverlineNode(Node inner) implements Node {}

  private record SubscriptNode(Node base, Node sub) implements Node {}

  private record RowNode(List<Node> children) implements Node {}

  private record LineLayout(
      String raw, String mathSource, Box box, int width, int ascent, int descent, int height) {}

  private record BlockLayout(LineLayout[] lines, int width, int height, int firstAscent) {}

  private static final class Cursor {
    private final String text;
    private int index;
    private boolean failed;

    private Cursor(String text) {
      this.text = text;
    }

    private boolean hasNext() {
      return index < text.length();
    }

    private char peek() {
      return text.charAt(index);
    }

    private char next() {
      return text.charAt(index++);
    }

    private void fail() {
      failed = true;
    }
  }

  private abstract static class Box {
    final int width;
    final int ascent;
    final int descent;

    private Box(int width, int ascent, int descent) {
      this.width = width;
      this.ascent = ascent;
      this.descent = descent;
    }

    abstract void draw(Graphics g, int x, int baseline);
  }

  private static final class TextBox extends Box {
    private final String text;
    private final Font font;

    private TextBox(String text, Font font, int width, int ascent, int descent) {
      super(width, ascent, descent);
      this.text = text;
      this.font = font;
    }

    @Override
    void draw(Graphics g, int x, int baseline) {
      final var old = g.getFont();
      g.setFont(font);
      try {
        if (!text.isEmpty()) g.drawString(text, x, baseline);
      } finally {
        g.setFont(old);
      }
    }
  }

  private static final class OverlineBox extends Box {
    private final Box inner;
    private final int gap;

    private OverlineBox(Box inner, int gap, int width, int ascent, int descent) {
      super(width, ascent, descent);
      this.inner = inner;
      this.gap = gap;
    }

    @Override
    void draw(Graphics g, int x, int baseline) {
      inner.draw(g, x, baseline);
      if (width <= 0) return;
      final var yLine = baseline - inner.ascent - gap;
      if (g instanceof Graphics2D g2) {
        final var old = g2.getStroke();
        g2.setStroke(new BasicStroke(OVERLINE_RULE));
        try {
          g2.drawLine(x, yLine, x + width - 1, yLine);
        } finally {
          g2.setStroke(old);
        }
      } else {
        g.drawLine(x, yLine, x + width - 1, yLine);
      }
    }
  }

  private static final class SubscriptBox extends Box {
    private final Box base;
    private final Box sub;
    private final int drop;

    private SubscriptBox(Box base, Box sub, int drop, int width, int ascent, int descent) {
      super(width, ascent, descent);
      this.base = base;
      this.sub = sub;
      this.drop = drop;
    }

    @Override
    void draw(Graphics g, int x, int baseline) {
      base.draw(g, x, baseline);
      sub.draw(g, x + base.width, baseline + drop);
    }
  }

  private static final class RowBox extends Box {
    private final List<Box> children;

    private RowBox(List<Box> children, int width, int ascent, int descent) {
      super(width, ascent, descent);
      this.children = children;
    }

    @Override
    void draw(Graphics g, int x, int baseline) {
      var cursor = x;
      for (final var child : children) {
        child.draw(g, cursor, baseline);
        cursor += child.width;
      }
    }
  }
}
