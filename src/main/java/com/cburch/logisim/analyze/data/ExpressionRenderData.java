/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.analyze.data;

import static com.cburch.logisim.analyze.Strings.S;

import com.cburch.logisim.analyze.model.Expression;
import com.cburch.logisim.analyze.model.Expression.Notation;
import com.cburch.logisim.prefs.AppPreferences;
import com.cburch.logisim.util.GraphicsUtil;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.font.FontRenderContext;
import java.awt.font.TextAttribute;
import java.awt.font.TextLayout;
import java.awt.image.BufferedImage;
import java.text.AttributedString;
import java.util.ArrayList;
import java.util.List;

public class ExpressionRenderData {

  private final Expression expr;
  private final Notation notation;
  private final int prefWidth;
  private final int parentWidth;
  private int height;
  private String[] lineText;
  private final ArrayList<ArrayList<Range>> lineNots;
  private final ArrayList<ArrayList<Range>> lineSubscripts;
  private ArrayList<ArrayList<Range>> lineMarks;
  private int[] lineY;
  private AttributedString[] lineStyled;
  private int[][] notStarts;
  private int[][] notStops;
  private static final Color MARKCOLOR = Color.BLACK;

  private final Font expressionBaseFont;
  private final FontMetrics expressionBaseFontMetrics;

  private final int notSep;
  private final int extraLeading;
  private final int minimumHeight;

  public ExpressionRenderData(Expression expr, int width, Notation notation) {
    this.expr = expr;
    this.parentWidth = width;
    this.notation = notation;
    notSep = AppPreferences.getScaled(3);
    extraLeading = AppPreferences.getScaled(4);
    expressionBaseFont = AppPreferences.getScaledFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));
    final var img = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
    final Graphics2D g = (Graphics2D) img.getGraphics().create();
    if (AppPreferences.AntiAliassing.getBoolean()) {
      g.setRenderingHint(
          RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
      g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    }
    g.setFont(expressionBaseFont);
    final var fm = expressionBaseFontMetrics = g.getFontMetrics();
    minimumHeight = fm.getHeight() + fm.getHeight() >> 1;
    g.dispose();
    if (expr == null || expr.toString(notation, true).length() == 0) {
      lineStyled = null;
      lineText = new String[] {S.get("expressionEmpty")};
      lineSubscripts = new ArrayList<>();
      lineSubscripts.add(new ArrayList<>());
      lineNots = new ArrayList<>();
      lineNots.add(new ArrayList<>());
      lineMarks = new ArrayList<>();
      lineMarks.add(new ArrayList<>());
    } else {
      computeLineText();
      lineSubscripts = computeLineAttribs(expr.subscripts);
      lineNots = computeLineAttribs(expr.nots);
      lineMarks = computeLineAttribs(expr.marks);
      computeNotDepths();
    }
    computeLineY();
    prefWidth = lineText.length > 1 ? width : fm.stringWidth(lineText[0]);
  }

  public void setSubExpression(Expression subExpr) {
    if (expr == null || subExpr == null) return;
    expr.toString(notation, true, subExpr);
    lineMarks = computeLineAttribs(expr.marks);
    lineStyled = null;
  }

  private ArrayList<ArrayList<Range>> computeLineAttribs(List<Range> attribs) {
    final var attrs = new ArrayList<ArrayList<Range>>();
    for (int i = 0; i < lineText.length; i++) {
      attrs.add(new ArrayList<>());
    }
    for (Range nd : attribs) {
      var pos = 0;
      for (int j = 0; j < attrs.size() && pos < nd.stopIndex; j++) {
        final var line = lineText[j];
        var nextPos = pos + line.length();
        if (nextPos > nd.startIndex) {
          Range toAdd = new Range();
          toAdd.startIndex = Math.max(pos, nd.startIndex) - pos;
          toAdd.stopIndex = Math.min(nextPos, nd.stopIndex) - pos;
          attrs.get(j).add(toAdd);
        }
        pos = nextPos;
      }
    }
    return attrs;
  }

  public int getParentWidth() {
    return parentWidth;
  }

  private void computeLineText() {
    var text = expr.toString(notation, true);
    final var badness = expr.getBadness();
    final var bestBreakPositions = new ArrayList<Integer>();
    final var secondBestBreakPositions = new ArrayList<Integer>();
    var minimal1 = Integer.MAX_VALUE;
    var minimal2 = Integer.MAX_VALUE;
    lineStyled = null;
    for (int i = 0; i < text.length(); i++) {
      if (badness[i] < minimal1) {
        minimal1 = badness[i];
      } else if (badness[i] < minimal2 && badness[i] > minimal1) {
        minimal2 = badness[i];
      }
    }
    for (int i = 0; i < text.length(); i++) {
      if (badness[i] == minimal1) {
        bestBreakPositions.add(i + 1);
        secondBestBreakPositions.add(i + 1);
      } else if (badness[i] == minimal2) secondBestBreakPositions.add(i + 1);
    }
    bestBreakPositions.add(text.length());
    secondBestBreakPositions.add(text.length());
    final var lines = new ArrayList<String>();
    final var img = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
    final var g = (Graphics2D) img.getGraphics().create();
    if (AppPreferences.AntiAliassing.getBoolean()) {
      g.setRenderingHint(
          RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
      g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    }
    g.setFont(expressionBaseFont);
    final var ctx = g.getFontRenderContext();
    /* first pass, we are going to break on the best positions if required */
    var i = bestBreakPositions.size() - 1;
    var breakPosition = 0;
    while (i >= 0 && text.length() > 0 && (bestBreakPositions.get(i) - breakPosition) > 0) {
      if (getWidth(ctx, text, bestBreakPositions.get(i) - breakPosition, expr.subscripts, expr.marks)
          <= parentWidth) {
        String addedLine = text.substring(0, bestBreakPositions.get(i) - breakPosition);
        lines.add(addedLine);
        text = text.substring(bestBreakPositions.get(i) - breakPosition);
        breakPosition += addedLine.length();
        i = bestBreakPositions.size() - 1;
      } else i--;
    }
    /* second pass, we are going to break on the second best positions if required */
    i = secondBestBreakPositions.size() - 1;
    while (i >= 0 && text.length() > 0 && (secondBestBreakPositions.get(i) - breakPosition) > 0) {
      if (getWidth(
                  ctx,
                  text,
                  secondBestBreakPositions.get(i) - breakPosition,
                  expr.subscripts,
                  expr.marks)
              <= parentWidth
          || (i == 0)
          || (secondBestBreakPositions.get(i - 1) - breakPosition <= 0)) {
        var addedLine = text.substring(0, secondBestBreakPositions.get(i) - breakPosition);
        lines.add(addedLine);
        text = text.substring(secondBestBreakPositions.get(i) - breakPosition);
        breakPosition += addedLine.length();
        i = secondBestBreakPositions.size() - 1;
      } else {
        i--;
      }
    }
    g.dispose();
    lineText = lines.toArray(new String[0]);
  }

  private void computeLineY() {
    lineY = new int[lineNots.size()];
    var curY = 0;
    for (int i = 0; i < lineY.length; i++) {
      int maxDepth = -1;
      final var nots = lineNots.get(i);
      for (Range nd : nots) {
        if (nd.depth > maxDepth) maxDepth = nd.depth;
      }
      lineY[i] = curY + (maxDepth + 1) * AppPreferences.getScaled(notSep);
      curY = lineY[i] + expressionBaseFontMetrics.getHeight() + extraLeading;
    }
    height = Math.max(minimumHeight, curY);
  }

  private void computeNotDepths() {
    for (ArrayList<Range> nots : lineNots) {
      final var n = nots.size();
      final var stack = new int[n];
      for (int i = 0; i < nots.size(); i++) {
        final var nd = nots.get(i);
        var depth = 0;
        var top = 0;
        stack[0] = nd.stopIndex;
        for (int j = i + 1; j < nots.size(); j++) {
          final var nd2 = nots.get(j);
          if (nd2.startIndex >= nd.stopIndex) break;
          while (nd2.startIndex >= stack[top]) top--;
          ++top;
          stack[top] = nd2.stopIndex;
          if (top > depth) depth = top;
        }
        nd.depth = depth;
      }
    }
  }

  public Dimension getPreferredSize() {
    return new Dimension(prefWidth, height);
  }

  private AttributedString style(String s, int end, List<Range> subs, List<Range> marks, boolean replaceSpaces) {
    /* This is a hack to get TextLayout to correctly format and calculate the width
     * of this substring (see remark in getWidth(...) below. As we have a mono spaced
     * font the size of all chars is equal.
     */
    var sub = s.substring(0, end);
    if (replaceSpaces) {
      sub = sub.replace(" ", "_");
    }
    AttributedString as = new AttributedString(sub);
    as.addAttribute(TextAttribute.FAMILY, expressionBaseFont.getFamily());
    as.addAttribute(TextAttribute.SIZE, expressionBaseFont.getSize());
    for (Range r : subs) {
      if (r.stopIndex <= end)
        as.addAttribute(TextAttribute.SUPERSCRIPT, TextAttribute.SUPERSCRIPT_SUB, r.startIndex, r.stopIndex);
    }
    for (Range m : marks) {
      if (m.stopIndex <= end)
        as.addAttribute(TextAttribute.FOREGROUND, MARKCOLOR, m.startIndex, m.stopIndex);
    }
    return as;
  }

  public int getWidth() {
    final var img = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
    final var g = (Graphics2D) img.getGraphics().create();
    g.setFont(expressionBaseFont);
    if (AppPreferences.AntiAliassing.getBoolean()) {
      g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
      g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    }
    final var ctx = g.getFontRenderContext();
    if (lineStyled == null) {
      lineStyled = new AttributedString[lineText.length];
      notStarts = new int[lineText.length][];
      notStops = new int[lineText.length][];
      for (int i = 0; i < lineText.length; i++) {
        final var line = lineText[i];
        final var nots = lineNots.get(i);
        final var subs = lineSubscripts.get(i);
        final var marks = lineMarks.get(i);
        notStarts[i] = new int[nots.size()];
        notStops[i] = new int[nots.size()];
        for (int j = 0; j < nots.size(); j++) {
          final var not = nots.get(j);
          notStarts[i][j] = getWidth(ctx, line, not.startIndex, subs, marks);
          notStops[i][j] = getWidth(ctx, line, not.stopIndex, subs, marks);
        }
        lineStyled[i] = style(line, line.length(), subs, marks, false);
      }
    }
    var width = 0;
    for (AttributedString attributedString : lineStyled) {
      final var test = new TextLayout(attributedString.getIterator(), ctx);
      if (test.getBounds().getWidth() > width)
        width = (int) test.getBounds().getWidth();
    }
    g.dispose();
    return width;
  }

  private int getWidth(FontRenderContext ctx, String s, int end, List<Range> subs, List<Range> marks) {
    if (end == 0) return 0;
    final var as = style(s, end, subs, marks, true);
    /* The TextLayout class will omit trailing spaces,
     * hence the width is incorrectly calculated. Therefore in the previous method we can
     * replace the spaces by underscores to prevent this problem; maybe
     * there is a more intelligent way.
     */
    final var layout = new TextLayout(as.getIterator(), ctx);
    return (int) layout.getBounds().getWidth();
  }

  public void paint(Graphics g, int x, int y) {
    g.setFont(expressionBaseFont);
    if (AppPreferences.AntiAliassing.getBoolean()) {
      final var g2 = (Graphics2D) g;
      g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
      g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    }
    final var fm = g.getFontMetrics();
    if (lineStyled == null) {
      final var ctx = ((Graphics2D) g).getFontRenderContext();
      lineStyled = new AttributedString[lineText.length];
      notStarts = new int[lineText.length][];
      notStops = new int[lineText.length][];
      for (int i = 0; i < lineText.length; i++) {
        final var line = lineText[i];
        final var nots = lineNots.get(i);
        final var subs = lineSubscripts.get(i);
        final var marks = lineMarks.get(i);
        notStarts[i] = new int[nots.size()];
        notStops[i] = new int[nots.size()];
        for (int j = 0; j < nots.size(); j++) {
          Range not = nots.get(j);
          notStarts[i][j] = getWidth(ctx, line, not.startIndex, subs, marks);
          notStops[i][j] = getWidth(ctx, line, not.stopIndex, subs, marks);
        }
        lineStyled[i] = style(line, line.length(), subs, marks, false);
      }
    }
    final var col = g.getColor();
    var curCol = col;
    for (int i = 0; i < lineStyled.length; i++) {
      final var as = lineStyled[i];
      final var nots = lineNots.get(i);
      final var marks = lineMarks.get(i);
      Range md;
      if (marks.isEmpty()) {
        md = new Range();
        md.startIndex = -1;
        md.stopIndex = -1;
        curCol = col;
      } else {
        md = marks.get(0);
        curCol = Color.GRAY;
      }
      g.setColor(curCol);
      g.drawString(as.getIterator(), x, y + lineY[i] + fm.getAscent());

      for (int j = 0; j < nots.size(); j++) {
        final var nd = nots.get(j);
        final var notY = y + lineY[i] - nd.depth * AppPreferences.getScaled(notSep);
        final var startX = x + notStarts[i][j];
        final var stopX = x + notStops[i][j];
        if (nd.startIndex >= md.startIndex && nd.stopIndex <= md.stopIndex) g.setColor(MARKCOLOR);
        GraphicsUtil.switchToWidth(g, 2);
        g.drawLine(startX, notY, stopX, notY);
        GraphicsUtil.switchToWidth(g, 1);
        if (nd.startIndex >= md.startIndex && nd.stopIndex <= md.stopIndex) g.setColor(curCol);
      }
    }
  }

}
