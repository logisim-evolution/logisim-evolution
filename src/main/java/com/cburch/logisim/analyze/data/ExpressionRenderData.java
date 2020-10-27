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

package com.cburch.logisim.analyze.data;

import static com.cburch.logisim.analyze.Strings.S;

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

import com.cburch.logisim.analyze.model.Expression;
import com.cburch.logisim.analyze.model.Expression.Notation;
import com.cburch.logisim.prefs.AppPreferences;
import com.cburch.logisim.util.GraphicsUtil;

public class ExpressionRenderData {

  private Expression expr;
  private Notation notation;
  private int prefWidth;
  private int parrentWidth;
  private int height;
  private String[] lineText;
  private ArrayList<ArrayList<Range>> lineNots;
  private ArrayList<ArrayList<Range>> lineSubscripts;
  private ArrayList<ArrayList<Range>> lineMarks;
  private int[] lineY;
  private AttributedString[] lineStyled;
  private int[][] notStarts;
  private int[][] notStops;
  private static Color MARKCOLOR = Color.BLACK; 

  private Font EXPRESSION_BASE_FONT;
  private FontMetrics EXPRESSION_BASE_FONTMETRICS;
  
  private int NOT_SEP;
  private int EXTRA_LEADING;
  private int MINIMUM_HEIGHT;

  public ExpressionRenderData(Expression expr, int width, Notation notation) {
    this.expr = expr;
    this.parrentWidth = width;
    this.notation = notation;
    NOT_SEP = AppPreferences.getScaled(3);
    EXTRA_LEADING = AppPreferences.getScaled(4);
    EXPRESSION_BASE_FONT = AppPreferences.getScaledFont(new Font("Monospaced", Font.PLAIN, 14));
    BufferedImage img = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
    Graphics2D g = (Graphics2D)img.getGraphics().create();
    if (AppPreferences.AntiAliassing.getBoolean()) {
      g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
      g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
    }
    g.setFont(EXPRESSION_BASE_FONT);
    FontMetrics fm = EXPRESSION_BASE_FONTMETRICS = g.getFontMetrics();
    MINIMUM_HEIGHT = fm.getHeight()+fm.getHeight()>>1;
    g.dispose();
    if (expr == null || expr.toString(notation, true).length() == 0) {
      lineStyled = null;
      lineText = new String[] {S.get("expressionEmpty")};
      lineSubscripts = new ArrayList<ArrayList<Range>>();
      lineSubscripts.add(new ArrayList<Range>());
      lineNots = new ArrayList<ArrayList<Range>>();
      lineNots.add(new ArrayList<Range>());
      lineMarks = new ArrayList<ArrayList<Range>>();
      lineMarks.add(new ArrayList<Range>());
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
	if (expr == null) return;
	if (subExpr == null) return;
    expr.toString(notation, true, subExpr);
    lineMarks = computeLineAttribs(expr.marks);
    lineStyled = null;
  }
  
  private ArrayList<ArrayList<Range>> computeLineAttribs(ArrayList<Range> attribs) {
    ArrayList<ArrayList<Range>> attrs = new ArrayList<ArrayList<Range>>();
    for (int i = 0; i < lineText.length; i++) {
      attrs.add(new ArrayList<Range>());
    }
    for (Range nd : attribs) {
      int pos = 0;
      for (int j = 0; j < attrs.size() && pos < nd.stopIndex; j++) {
        String line = lineText[j];
        int nextPos = pos + line.length();
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
	  return parrentWidth;
  }

  private void computeLineText() {
    String text = expr.toString(notation, true);
    Integer[] badness = expr.getBadness();
    ArrayList<Integer> bestBreakPositions = new ArrayList<Integer>();
    ArrayList<Integer> secondBestBreakPositions = new ArrayList<Integer>();
    Integer minimal1=Integer.MAX_VALUE,minimal2=Integer.MAX_VALUE;
    lineStyled = null;
    for (int i = 0 ; i < text.length() ; i++) {
    	if (badness[i]<minimal1) {
    		minimal1 = badness[i];
    	} else if (badness[i]< minimal2 && badness[i] > minimal1) {
    		minimal2 = badness[i];
    	}
    }
    for (int i = 0 ; i < text.length() ; i++) {
      if (badness[i] == minimal1) {
        bestBreakPositions.add(i+1);
        secondBestBreakPositions.add(i+1);
      } else if (badness[i] == minimal2)
        secondBestBreakPositions.add(i+1);
    }
    bestBreakPositions.add(text.length());
    secondBestBreakPositions.add(text.length());
    ArrayList<String> lines = new ArrayList<String>();
    BufferedImage img = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
    Graphics2D g = (Graphics2D)img.getGraphics().create();
    if (AppPreferences.AntiAliassing.getBoolean()) {
      g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
      g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
    }
    g.setFont(EXPRESSION_BASE_FONT);
    FontRenderContext ctx = g.getFontRenderContext();
    /* first pass, we are going to break on the best positions if required */
    int i = bestBreakPositions.size()-1;
    int breakPosition = 0;
    while (i >= 0 && text.length()> 0 && (bestBreakPositions.get(i)-breakPosition) > 0) {
    	if (getWidth(ctx,text,bestBreakPositions.get(i)-breakPosition,expr.subscripts,expr.marks)<=parrentWidth) {
    		String addedLine = text.substring(0, bestBreakPositions.get(i)-breakPosition);
    		lines.add(addedLine);
    		text = text.substring(bestBreakPositions.get(i)-breakPosition);
    		breakPosition += addedLine.length();
    		i = bestBreakPositions.size()-1;
    	} else i--;
    }
    /* second pass, we are going to break on the second best positions if required */
    i = secondBestBreakPositions.size()-1;
    while (i >= 0 && text.length()> 0 && (secondBestBreakPositions.get(i)-breakPosition) > 0) {
    	if (getWidth(ctx,text,secondBestBreakPositions.get(i)-breakPosition,expr.subscripts,expr.marks)<=parrentWidth ||
              i==0 || secondBestBreakPositions.get(i-1)-breakPosition <= 0) {
    		String addedLine = text.substring(0, secondBestBreakPositions.get(i)-breakPosition);
    		lines.add(addedLine);
    		text = text.substring(secondBestBreakPositions.get(i)-breakPosition);
    		breakPosition += addedLine.length();
    		i = secondBestBreakPositions.size()-1;
    	} else i--;
    }
    g.dispose();
    lineText = lines.toArray(new String[lines.size()]);
  }

  private void computeLineY() {
    lineY = new int[lineNots.size()];
    int curY = 0;
    for (int i = 0; i < lineY.length; i++) {
      int maxDepth = -1;
      ArrayList<Range> nots = lineNots.get(i);
      for (Range nd : nots) {
        if (nd.depth > maxDepth) maxDepth = nd.depth;
      }
      lineY[i] = curY + (maxDepth + 1) * AppPreferences.getScaled(NOT_SEP);
      curY = lineY[i] + EXPRESSION_BASE_FONTMETRICS.getHeight() + EXTRA_LEADING;
    }
    height = Math.max(MINIMUM_HEIGHT, curY);
  }

  private void computeNotDepths() {
    for (ArrayList<Range> nots : lineNots) {
      int n = nots.size();
      int[] stack = new int[n];
      for (int i = 0; i < nots.size(); i++) {
        Range nd = nots.get(i);
        int depth = 0;
        int top = 0;
        stack[0] = nd.stopIndex;
        for (int j = i + 1; j < nots.size(); j++) {
          Range nd2 = nots.get(j);
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

  private AttributedString style(String s, int end, ArrayList<Range> subs, ArrayList<Range> marks, boolean replaceSpaces) {
    /* This is a hack to get TextLayout to correctly format and calculate the width
     * of this substring (see remark in getWidth(...) below. As we have a mono spaced
     * font the size of all chars is equal.
     */
    String sub = s.substring(0, end);
    if (replaceSpaces) {
     sub = sub.replaceAll("[ ()\\u22C5]","_");
    }
    AttributedString as = new AttributedString(sub);
    as.addAttribute(TextAttribute.FAMILY, EXPRESSION_BASE_FONT.getFamily());
    as.addAttribute(TextAttribute.SIZE, EXPRESSION_BASE_FONT.getSize());
    for (Range r : subs) {
      if (r.stopIndex <= end)
        as.addAttribute(TextAttribute.SUPERSCRIPT, TextAttribute.SUPERSCRIPT_SUB, r.startIndex, r.stopIndex);
    }
    for (Range m : marks) {
      if (m.stopIndex <= end)
        as.addAttribute(TextAttribute.FOREGROUND, MARKCOLOR, m.startIndex , m.stopIndex);
    }
    return as;
  }

  public int getWidth() {
    BufferedImage img = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
    Graphics2D g = (Graphics2D)img.getGraphics().create();
    g.setFont(EXPRESSION_BASE_FONT);
    if (AppPreferences.AntiAliassing.getBoolean()) {
      g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
      g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
    }
    FontRenderContext ctx = g.getFontRenderContext();
    if (lineStyled == null) {
      lineStyled = new AttributedString[lineText.length];
      notStarts = new int[lineText.length][];
      notStops = new int[lineText.length][];
      for (int i = 0; i < lineText.length; i++) {
        String line = lineText[i];
        ArrayList<Range> nots = lineNots.get(i);
        ArrayList<Range> subs = lineSubscripts.get(i);
        ArrayList<Range> marks = lineMarks.get(i);
        notStarts[i] = new int[nots.size()];
        notStops[i] = new int[nots.size()];
        for (int j = 0; j < nots.size(); j++) {
          Range not = nots.get(j);
          notStarts[i][j] = getWidth(ctx, line, not.startIndex, subs,marks);
          notStops[i][j] = getWidth(ctx, line, not.stopIndex, subs,marks);
        }
        lineStyled[i] = style(line, line.length(), subs, marks, false);
      }
    }
    int width = 0;
    for (int i = 0; i < lineStyled.length; i++) {
      TextLayout test = new TextLayout(lineStyled[i].getIterator(), ctx);
      if (test.getBounds().getWidth() > width) width = (int) test.getBounds().getWidth();
    }
    g.dispose();
    return width;
  }

  private int getWidth(FontRenderContext ctx, String s, int end, ArrayList<Range> subs, ArrayList<Range> marks) {
    if (end == 0) return 0;
    AttributedString as = style(s, end, subs, marks, true);
    /* The TextLayout class will omit trailing spaces, incorrectly format parenthesis/cdot,
     * hence the width is incorrectly calculated. Therefore in the previous method we can
     * replace the spaces and parenthesis by underscores to prevent this problem; maybe
     * there is a more intelligent way.
     */
    TextLayout layout = new TextLayout(as.getIterator(), ctx);
    return (int) layout.getBounds().getWidth();
  }

  public void paint(Graphics g, int x, int y) {
    g.setFont(EXPRESSION_BASE_FONT);
    if (AppPreferences.AntiAliassing.getBoolean()) {
      Graphics2D g2 = (Graphics2D) g;
      g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
      g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    }
    FontMetrics fm = g.getFontMetrics();
    if (lineStyled == null) {
      FontRenderContext ctx = ((Graphics2D) g).getFontRenderContext();
      lineStyled = new AttributedString[lineText.length];
      notStarts = new int[lineText.length][];
      notStops = new int[lineText.length][];
      for (int i = 0; i < lineText.length; i++) {
        String line = lineText[i];
        ArrayList<Range> nots = lineNots.get(i);
        ArrayList<Range> subs = lineSubscripts.get(i);
        ArrayList<Range> marks = lineMarks.get(i);
        notStarts[i] = new int[nots.size()];
        notStops[i] = new int[nots.size()];
        for (int j = 0; j < nots.size(); j++) {
          Range not = nots.get(j);
          notStarts[i][j] = getWidth(ctx, line, not.startIndex, subs,marks);
          notStops[i][j] = getWidth(ctx, line, not.stopIndex, subs,marks);
        }
        lineStyled[i] = style(line, line.length(), subs, marks, false);
      }
    }
    Color col = g.getColor();
    Color curCol = col;
    for (int i = 0; i < lineStyled.length; i++) {
      AttributedString as = lineStyled[i];
      ArrayList<Range> nots = lineNots.get(i);
      ArrayList<Range> marks = lineMarks.get(i);
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
        Range nd = nots.get(j);
        int notY = y + lineY[i] - nd.depth * AppPreferences.getScaled(NOT_SEP);
        int startX = x + notStarts[i][j];
        int stopX = x + notStops[i][j];
        if (nd.startIndex >= md.startIndex && nd.stopIndex <= md.stopIndex)
        	g.setColor(MARKCOLOR);
        GraphicsUtil.switchToWidth(g, 2);
        g.drawLine(startX, notY, stopX, notY);
        GraphicsUtil.switchToWidth(g, 1);
        if (nd.startIndex >= md.startIndex && nd.stopIndex <= md.stopIndex)
        	g.setColor(curCol);
      }
    }
  }
  
}
