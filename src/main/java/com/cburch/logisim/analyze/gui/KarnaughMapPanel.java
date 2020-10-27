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

package com.cburch.logisim.analyze.gui;

import static com.cburch.logisim.analyze.Strings.S;

import com.cburch.logisim.analyze.data.ExpressionRenderData;
import com.cburch.logisim.analyze.data.KMapGroups;
import com.cburch.logisim.analyze.model.AnalyzerModel;
import com.cburch.logisim.analyze.model.Entry;
import com.cburch.logisim.analyze.model.Expression;
import com.cburch.logisim.analyze.model.Expression.Notation;
import com.cburch.logisim.analyze.model.OutputExpressionsEvent;
import com.cburch.logisim.analyze.model.OutputExpressionsListener;
import com.cburch.logisim.analyze.model.TruthTable;
import com.cburch.logisim.analyze.model.TruthTableEvent;
import com.cburch.logisim.analyze.model.TruthTableListener;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.prefs.AppPreferences;
import com.cburch.logisim.util.GraphicsUtil;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.font.FontRenderContext;
import java.awt.font.TextAttribute;
import java.awt.font.TextLayout;
import java.text.AttributedString;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JPanel;

public class KarnaughMapPanel extends JPanel
    implements MouseMotionListener, MouseListener, Entry.EntryChangedListener {
  private class MyListener implements OutputExpressionsListener, TruthTableListener {

    public void rowsChanged(TruthTableEvent event) {}

    public void cellsChanged(TruthTableEvent event) {
      kMapGroups.update();
      repaint();
    }

    public void expressionChanged(OutputExpressionsEvent event) {
      if (event.getType() == OutputExpressionsEvent.OUTPUT_MINIMAL
          && event.getVariable().equals(output)) {
        kMapGroups.update();
        repaint();
      }
    }

    public void structureChanged(TruthTableEvent event) {
      kMapGroups.update();
      computePreferredSize();
    }
  }

  private static final long serialVersionUID = 1L;

  private class KMapInfo {
    private int headWidth, headHeight;
    private int Width, Height;
    private int xOff, yOff;

    public KMapInfo(int headWidth, int headHeight, int tableWidth, int tableHeight) {
      xOff = 0;
      yOff = 0;
      this.headWidth = headWidth;
      this.headHeight = headHeight;
      this.Width = tableWidth;
      this.Height = tableHeight;
    }

    public void calculateOffsets(int boxWidth, int boxHeight) {
      xOff = (boxWidth - Width) / 2;
      yOff = (boxHeight - Height) / 2;
    }

    public int getXOffset() {
      return xOff;
    }

    public int getYOffset() {
      return yOff;
    }

    public int getWidth() {
      return Width;
    }

    public int getHeight() {
      return Height;
    }

    public int getHeaderWidth() {
      return headWidth;
    }

    public int getHeaderHeight() {
      return headHeight;
    }
  }

  public static final int MAX_VARS = 6;
  public static final int[] ROW_VARS = {0, 0, 1, 1, 2, 2, 3};
  public static final int[] COL_VARS = {0, 1, 1, 2, 2, 3, 3};
  private static final int[] BigCOL_Index = {0, 1, 3, 2, 6, 7, 5, 4};
  private static final int[] BigCOL_Place = {0, 1, 3, 2, 7, 6, 4, 5};
  private static final int CELL_HORZ_SEP = 10;
  private static final int CELL_VERT_SEP = 10;

  private MyListener myListener = new MyListener();
  private ExpressionView completeExpression;
  private AnalyzerModel model;
  private String output;
  private int cellWidth = 1;
  private int cellHeight = 1;
  private int provisionalX;
  private int provisionalY;
  private Entry provisionalValue = null;
  private Font HeaderFont;
  private Font EntryFont;
  private boolean KMapLined;
  private Bounds KMapArea;
  private KMapInfo KLinedInfo;
  private KMapInfo KNumberedInfo;
  private KMapGroups kMapGroups;
  private Bounds SelInfo;
  private Point hover;
  private Notation notation = Notation.MATHEMATICAL;
  private boolean selected;
  private Dimension kMapDim;
  
  boolean isSelected() {
	  return selected;
  }

  public KarnaughMapPanel(AnalyzerModel model, ExpressionView expr) {
    super(new GridLayout(1, 1));
    completeExpression = expr;
    this.model = model;
    EntryFont = AppPreferences.getScaledFont(getFont());
    HeaderFont = EntryFont.deriveFont(Font.BOLD);
    model.getOutputExpressions().addOutputExpressionsListener(myListener);
    model.getTruthTable().addTruthTableListener(myListener);
    setToolTipText(" ");
    KMapLined = AppPreferences.KMAP_LINED_STYLE.get();
    kMapGroups = new KMapGroups(model);
    addMouseMotionListener(this);
    addMouseListener(this);
    hover = new Point(-1, -1);
    FocusListener f = new FocusListener() {
      public void focusGained(FocusEvent e) {
        if (e.isTemporary()) return;
        selected = true;
        repaint();
      }
      public void focusLost(FocusEvent e) {
        if (e.isTemporary()) return;
        selected = false;
        repaint();
      }
    };
    addFocusListener(f);
    Entry.ZERO.addListener(this);
    Entry.ONE.addListener(this);
    Entry.DONT_CARE.addListener(this);
  }

  private void computePreferredSize() {
    SelInfo = null;
    Graphics2D g = (Graphics2D) getGraphics();
    TruthTable table = model.getTruthTable();

    String message = null;
    if (output == null) {
      message = S.get("karnaughNoOutputError");
    } else if (table.getInputColumnCount() > MAX_VARS) {
      message = S.get("karnaughTooManyInputsError");
    } else if (table.getInputColumnCount() == 0) message = S.get("karnaughNoInputsError");

    if (message != null) {
      if (g == null) {
        setPreferredSize(
            new Dimension(
                AppPreferences.getScaled(20 * message.length()),
                AppPreferences.getScaled(AppPreferences.BoxSize)));
      } else {
        FontRenderContext ctx = g.getFontRenderContext();
        TextLayout mLayout = new TextLayout(message, HeaderFont, ctx);
        setPreferredSize(
            new Dimension(
                (int) mLayout.getBounds().getWidth(), (int) mLayout.getBounds().getHeight()));
      }
    } else {
      computePreferredLinedSize(g, table);
      computePreferredNumberedSize(g, table);
      int boxWidth = Math.max(KLinedInfo.getWidth(), KNumberedInfo.getWidth());
      boxWidth = Math.max(boxWidth, AppPreferences.getScaled(300));
      int boxHeight = Math.max(KLinedInfo.getHeight(), KNumberedInfo.getHeight());
      KLinedInfo.calculateOffsets(boxWidth, boxHeight);
      KNumberedInfo.calculateOffsets(boxWidth, boxHeight);
      FontRenderContext ctx = g.getFontRenderContext();
      int selectedHeight;
      TextLayout t1 = new TextLayout(S.get("SelectedKmapGroup"), HeaderFont, ctx);
      selectedHeight = 3 * (int) t1.getBounds().getHeight();
      SelInfo = Bounds.create(0, boxHeight, boxWidth, selectedHeight);
      setPreferredSize(new Dimension(boxWidth, boxHeight + selectedHeight));
      kMapDim = new Dimension(boxWidth,boxHeight); 
    }

    invalidate();
    if (g != null) repaint();
  }
  
  public Dimension getKMapDim() {
    return kMapDim;
  }

  private List<TextLayout> header(
      List<String> inputs,
      int start,
      int end,
      boolean rowLabel,
      boolean addComma,
      FontRenderContext ctx) {
    List<TextLayout> lines = new ArrayList<TextLayout>();
    if (start >= end) return lines;
    StringBuilder ret = new StringBuilder(inputs.get(start));
    for (int i = start + 1; i < end; i++) {
      ret.append(", ");
      ret.append(inputs.get(i));
    }
    if (addComma) ret.append(",");
    int maxSize = rowLabel ? (1 << (end - start - 1)) * cellWidth : 100;
    TextLayout myLayout = Styled(ret.toString(), HeaderFont, ctx);
    if (((end - start) <= 1) || (myLayout.getBounds().getWidth() <= maxSize)) {
      lines.add(myLayout);
      return lines;
    }
    int NrOfEntries = end - start;
    if (NrOfEntries > 1) {
      int half = NrOfEntries >> 1;
      lines.addAll(header(inputs, start, end - half, rowLabel, true, ctx));
      lines.addAll(header(inputs, end - half, end, rowLabel, addComma, ctx));
    } else lines.add(myLayout);
    return lines;
  }

  private void computePreferredNumberedSize(Graphics2D g, TruthTable table) {
    List<String> inputs = model.getInputs().bits;
    int inputCount = table.getInputColumnCount();
    int rowVars = ROW_VARS[inputCount];
    int colVars = COL_VARS[inputCount];
    int headHeight, headWidth, tableWidth, tableHeight;
    if (g == null) {
      cellHeight = 16;
      cellWidth = 24;
    } else {
      FontMetrics fm = g.getFontMetrics(EntryFont);
      cellHeight = fm.getAscent() + CELL_VERT_SEP;
      cellWidth = fm.stringWidth("00") + CELL_HORZ_SEP;
    }
    int rows = 1 << rowVars;
    int cols = 1 << colVars;
    int bodyWidth = cellWidth * (cols + 1);
    int bodyHeight = cellHeight * (rows + 1);

    int colLabelWidth;
    if (g == null) {
      headHeight = 16;
      headWidth = 80;
      colLabelWidth = 80;
    } else {
      FontRenderContext ctx = g.getFontRenderContext();
      List<TextLayout> rowHeader = header(inputs, 0, rowVars, true, false, ctx);
      List<TextLayout> colHeader = header(inputs, rowVars, rowVars + colVars, false, false, ctx);
      headWidth = 0;
      int height = 0;
      for (TextLayout l : rowHeader) {
        int w = (int) l.getBounds().getWidth();
        if (w > headWidth) headWidth = w;
      }
      colLabelWidth = 0;
      for (TextLayout l : colHeader) {
        int w = (int) l.getBounds().getWidth();
        int h = (int) l.getBounds().getHeight();
        if (w > colLabelWidth) colLabelWidth = w;
        if (h > height) height = h;
      }
      headHeight = colHeader.size() * height;
    }
    tableHeight = headHeight + bodyHeight + 5;
    tableWidth = headWidth + Math.max(bodyWidth, colLabelWidth + cellWidth) + 5;
    KNumberedInfo = new KMapInfo(headWidth, headHeight, tableWidth, tableHeight);
  }

  private void computePreferredLinedSize(Graphics2D g, TruthTable table) {
    int headHeight, headWidth, tableWidth, tableHeight;
    if (g == null) {
      headHeight = 16;
      cellHeight = 16;
      cellWidth = 24;
    } else {
      FontRenderContext ctx = ((Graphics2D) g).getFontRenderContext();
      FontMetrics fm = g.getFontMetrics(HeaderFont);
      int singleheight = StyledHeight(Styled("E", HeaderFont), ctx);
      headHeight = StyledHeight(Styled("E:2", HeaderFont), ctx) + (fm.getAscent() - singleheight);

      fm = g.getFontMetrics(EntryFont);
      cellHeight = fm.getAscent() + CELL_VERT_SEP;
      cellWidth = fm.stringWidth("00") + CELL_HORZ_SEP;
    }

    int rows = 1 << ROW_VARS[table.getInputColumnCount()];
    int cols = 1 << COL_VARS[table.getInputColumnCount()];
    tableWidth = headHeight + cellWidth * (cols) + 15;
    tableHeight = headHeight + cellHeight * (rows) + 15;
    if ((cols >= 4) && (rows >= 4)) {
      tableWidth += headHeight + 11;
    }
    if (cols >= 4) {
      tableHeight += headHeight + 11;
    }
    if (cols > 4) {
      tableHeight += headHeight + (headHeight >> 2) + 11;
    }
    if (rows > 4) {
      tableWidth += headHeight + (headHeight >> 2) + 11;
    }
    headWidth = 0;
    KLinedInfo = new KMapInfo(headWidth, headHeight, tableWidth, tableHeight);
  }

  public static int getCol(int tableRow, int rows, int cols) {
    int ret = tableRow % cols;
    if (cols > 4) {
      return BigCOL_Place[ret];
    }
    switch (ret) {
      case 2:
        return 3;
      case 3:
        return 2;
      default:
        return ret;
    }
  }

  public void setStyleLined() {
    KMapLined = true;
    AppPreferences.KMAP_LINED_STYLE.set(true);
    repaint();
  }

  public void setStyleNumbered() {
    KMapLined = false;
    AppPreferences.KMAP_LINED_STYLE.set(false);
    repaint();
  }

  public int getOutputColumn(MouseEvent event) {
    return model.getOutputs().bits.indexOf(output);
  }

  public static int getRow(int tableRow, int rows, int cols) {
    int ret = tableRow / cols;
    if (rows > 4) {
      return BigCOL_Place[ret];
    }
    switch (ret) {
      case 2:
        return 3;
      case 3:
        return 2;
      default:
        return ret;
    }
  }

  public int getRow(MouseEvent event) {
    TruthTable table = model.getTruthTable();
    int inputs = table.getInputColumnCount();
    if (inputs >= ROW_VARS.length) return -1;
    int x = event.getX() - KMapArea.getX();
    int y = event.getY() - KMapArea.getY();
    if (x < 0 || y < 0) return -1;
    int row = y / cellHeight;
    int col = x / cellWidth;
    int rows = 1 << ROW_VARS[inputs];
    int cols = 1 << COL_VARS[inputs];
    if (row >= rows || col >= cols) return -1;
    return getTableRow(row, col, rows, cols);
  }

  private int getTableRow(int row, int col, int rows, int cols) {
    return toRow(row, rows) * cols + toCol(col, cols);
  }

  @Override
  public String getToolTipText(MouseEvent event) {
    TruthTable table = model.getTruthTable();
    int row = getRow(event);
    if (row < 0) return null;
    int col = getOutputColumn(event);
    Entry entry = table.getOutputEntry(row, col);
    String s = entry.getErrorMessage();
    if (s == null) s = "";
    else s += "<br>";
    s += output + " = " + entry.getDescription();
    List<String> inputs = model.getInputs().bits;
    if (inputs.size() == 0) return "<html>" + s + "</html>";
    s += "<br>When:";
    int n = inputs.size();
    for (int i = 0; i < MAX_VARS && i < inputs.size(); i++) {
      s += "<br>&nbsp;&nbsp;&nbsp;&nbsp;" + inputs.get(i) + " = " + ((row >> (n - i - 1)) & 1);
    }
    return "<html>" + s + "</html>";
  }

  public TruthTable getTruthTable() {
    return model.getTruthTable();
  }

  void localeChanged() {
    computePreferredSize();
    repaint();
  }

  @Override
  public void paintComponent(Graphics g) {
	  paintKmap(g,true);
  }
  
  public void paintKmap(Graphics g , boolean selectionBlock) {
    if (!(g instanceof Graphics2D)) return;
    Graphics2D g2 = (Graphics2D) g;
    if (AppPreferences.AntiAliassing.getBoolean()) {
      g2.setRenderingHint(
          RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
      g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    }
    Color col = g2.getColor();
    if (selectionBlock) {
      g2.setColor(getBackground());
      g2.fillRect(0, 0, getBounds().width, getBounds().height);
      g2.setColor(col);
    }

    TruthTable table = model.getTruthTable();
    int inputCount = table.getInputColumnCount();
    Dimension sz = getSize();
    String message = null;
    if (output == null) {
      message = S.get("karnaughNoOutputError");
    } else if (inputCount > MAX_VARS) {
      message = S.get("karnaughTooManyInputsError");
    } else if (inputCount == 0) {
      message = S.get("karnaughNoInputsError");
    }
    if (message != null) {
      g.setFont(HeaderFont);
      GraphicsUtil.drawCenteredText(g2, message, sz.width / 2, sz.height / 2);
      return;
    }

    if (KMapLined) {
      int x = KLinedInfo.getXOffset();
      int y = KLinedInfo.getYOffset();
      drawLinedHeader(g2, x, y);
      x += KLinedInfo.getHeaderHeight() + 11;
      y += KLinedInfo.getHeaderHeight() + 11;
      PaintKMap(g2, x, y, table);
    } else {
      int x = KNumberedInfo.getXOffset();
      int y = KNumberedInfo.getYOffset();
      drawNumberedHeader(g2, x, y);
      x += KNumberedInfo.getHeaderWidth() + cellWidth;
      y += KNumberedInfo.getHeaderHeight() + cellHeight;
      PaintKMap(g2, x, y, table);
    }
    if (!selectionBlock)
      return;
    Expression expr = kMapGroups.GetHighlightedExpression();
    Color bcol = kMapGroups.GetBackgroundColor();
    FontRenderContext ctx = g2.getFontRenderContext();
    Color ccol = g2.getColor();
    if (bcol != null) g2.setColor(bcol);
    else g2.setColor(this.getBackground());
    g2.fillRect(SelInfo.getX(), SelInfo.getY(), SelInfo.getWidth() - 1, SelInfo.getHeight() - 1);
    g2.setColor(ccol);
    g2.drawRect(SelInfo.getX(), SelInfo.getY(), SelInfo.getWidth() - 1, SelInfo.getHeight() - 1);
    if (expr == null) {
      TextLayout t1 = new TextLayout(S.get("NoSelectedKmapGroup"), HeaderFont, ctx);
      int xoff = (SelInfo.getWidth() - (int) t1.getBounds().getWidth()) / 2;
      int yoff = (SelInfo.getHeight() - (int) t1.getBounds().getHeight()) / 2;
      t1.draw(g2, xoff + SelInfo.getX(), yoff + SelInfo.getY() + t1.getAscent());
    } else {
      TextLayout t1 = new TextLayout(S.get("SelectedKmapGroup"), HeaderFont, ctx);
      int xoff = (SelInfo.getWidth() - (int) t1.getBounds().getWidth()) / 2;
      t1.draw(g2, xoff + SelInfo.getX(), SelInfo.getY() + t1.getAscent());
      ExpressionRenderData t2 = new ExpressionRenderData(expr, SelInfo.getWidth(), notation);
      xoff = (SelInfo.getWidth() - t2.getWidth()) / 2;
      t2.paint(g, xoff + SelInfo.getX(), (int) (SelInfo.getY() + t1.getAscent() + t1.getDescent()));
    }
  }
  
  public void setNotation(Notation notation) {
	  if (notation == this.notation)
		  return;
	  this.notation = notation;
  }

  private String label(int row, int rows) {
    switch (rows) {
      case 2:
        return "" + row;
      case 4:
        switch (row) {
          case 0:
            return "00";
          case 1:
            return "01";
          case 2:
            return "11";
          case 3:
            return "10";
        }
      case 8:
        switch (row) {
          case 0:
            return "000";
          case 1:
            return "001";
          case 2:
            return "011";
          case 3:
            return "010";
          case 4:
            return "110";
          case 5:
            return "111";
          case 6:
            return "101";
          case 7:
            return "100";
        }
      default:
        return "";
    }
  }

  private void drawNumberedHeader(Graphics2D g, int x, int y) {
    TruthTable table = model.getTruthTable();
    int inputCount = table.getInputColumnCount();
    int tableXstart = x + KNumberedInfo.getHeaderWidth() + cellWidth;
    int tableYstart = y + KNumberedInfo.getHeaderHeight() + cellHeight;
    int rowVars = ROW_VARS[inputCount];
    int colVars = COL_VARS[inputCount];
    int rows = 1 << rowVars;
    int cols = 1 << colVars;
    FontMetrics headFm = g.getFontMetrics(HeaderFont);
    FontRenderContext ctx = g.getFontRenderContext();
    Font NumberFont = HeaderFont;
    int width2 = headFm.stringWidth("00");
    int width3 = headFm.stringWidth("000");
    float scale = (float) width2 / (float) width3;
    NumberFont = HeaderFont.deriveFont(scale * HeaderFont.getSize2D());
    for (int c = 0; c < cols; c++) {
      String label = label(c, cols);
      TextLayout Slabel = Styled(label, NumberFont, ctx);
      int xoff = (cellWidth - (int) Slabel.getBounds().getWidth()) >> 1;
      Slabel.draw(
          g, tableXstart + xoff + c * cellWidth, tableYstart - 3 - (int) Slabel.getDescent());
    }
    for (int r = 0; r < rows; r++) {
      String label = label(r, rows);
      TextLayout Slabel = Styled(label, NumberFont, ctx);
      Slabel.draw(
          g,
          (float) (tableXstart - Slabel.getBounds().getWidth() - Slabel.getDescent() - 3),
          (float)
              (tableYstart
                  + (cellHeight - Slabel.getAscent()) / 2
                  + Slabel.getAscent()
                  + r * cellHeight));
    }
    List<TextLayout> rowHeader = header(model.getInputs().bits, 0, rowVars, true, false, ctx);
    List<TextLayout> colHeader =
        header(model.getInputs().bits, rowVars, rowVars + colVars, false, false, ctx);
    int rx = x + 3;
    int ry = y + KNumberedInfo.getHeaderHeight() + cellHeight / 2;
    for (TextLayout l : rowHeader) {
      l.draw(g, rx, ry + l.getAscent());
      ry += (int) l.getBounds().getHeight();
    }
    rx = x + KNumberedInfo.getHeaderWidth() + cellWidth / 2;
    ry = y + 3;
    for (TextLayout l : colHeader) {
      l.draw(g, rx, ry + l.getAscent());
      ry += (int) l.getBounds().getHeight();
    }
  }

  private AttributedString Styled(String header, Font font) {
    ArrayList<Integer> starts = new ArrayList<Integer>();
    ArrayList<Integer> stops = new ArrayList<Integer>();
    StringBuffer str = new StringBuffer();
    int idx = 0;
    while (header != null && idx < header.length()) {
      if (header.charAt(idx) == ':' || header.charAt(idx) == '[') {
        idx++;
        starts.add(str.length());
        while (idx < header.length() && "0123456789".indexOf(header.charAt(idx)) >= 0) {
          str.append(header.charAt(idx++));
        }
        stops.add(str.length());
        if ((idx < header.length()) && header.charAt(idx) == ']')
          idx++;
      } else str.append(header.charAt(idx++));
    }
    AttributedString styled = new AttributedString(str.toString());
    styled.addAttribute(TextAttribute.FAMILY, font.getFamily());
    styled.addAttribute(TextAttribute.SIZE, font.getSize());
    for (int i = 0; i < starts.size(); i++)
      styled.addAttribute(
          TextAttribute.SUPERSCRIPT, TextAttribute.SUPERSCRIPT_SUB, starts.get(i), stops.get(i));
    return styled;
  }

  private TextLayout Styled(String header, Font font, FontRenderContext ctx) {
    return new TextLayout(Styled(header, font).getIterator(), ctx);
  }

  private int StyledWidth(AttributedString header, FontRenderContext ctx) {
    TextLayout layout = new TextLayout(header.getIterator(), ctx);
    return (int) layout.getBounds().getWidth();
  }

  private int StyledHeight(AttributedString header, FontRenderContext ctx) {
    TextLayout layout = new TextLayout(header.getIterator(), ctx);
    return (int) layout.getBounds().getHeight();
  }

  private void drawKmapLine(Graphics2D g, Point P1, Point P2) {
    Stroke oldstroke = g.getStroke();
    g.setStroke(new BasicStroke(2));
    g.drawLine(P1.x, P1.y, P2.x, P2.y);
    if (P1.y == P2.y) {
      // we have a horizontal line
      g.drawLine(P1.x, P1.y - 4, P1.x, P1.y + 4);
      g.drawLine(P2.x, P2.y - 4, P2.x, P2.y + 4);
    } else {
      // we have a vertical line
      g.drawLine(P1.x - 4, P1.y, P1.x + 4, P1.y);
      g.drawLine(P2.x - 4, P2.y, P2.x + 4, P2.y);
    }
    g.setStroke(oldstroke);
  }

  private void drawLinedHeader(Graphics2D g, int x, int y) {
    TruthTable table = model.getTruthTable();
    int inputCount = table.getInputColumnCount();
    FontMetrics headFm = g.getFontMetrics(HeaderFont);
    FontRenderContext ctx = g.getFontRenderContext();
    int rowVars = ROW_VARS[inputCount];
    int colVars = COL_VARS[inputCount];
    int rows = 1 << rowVars;
    int cols = 1 << colVars;
    int headHeight = KLinedInfo.getHeaderHeight();
    for (int i = 0; i < inputCount; i++) {
      AttributedString header = Styled(model.getInputs().bits.get(i), HeaderFont);
      Boolean rotated = false;
      int middleOffset = StyledWidth(header, ctx) >> 1;
      int xoffset = headHeight + 11;
      int yoffset = headHeight + 11;
      switch (i) {
        case 0:
          if (inputCount == 1) {
            rotated = false;
            xoffset += cellWidth + cellWidth / 2;
            yoffset = headFm.getAscent();
          } else {
            rotated = true;
            yoffset += (rows - 1) * cellHeight;
            if (inputCount < 4) yoffset += cellHeight / 2;
            if (inputCount > 5) yoffset -= cellHeight;
            xoffset = headFm.getAscent();
          }
          break;
        case 1:
          if (inputCount == 2) {
            rotated = false;
            xoffset += cellWidth + cellWidth / 2;
            yoffset = headFm.getAscent();
          } else if (inputCount == 3) {
            rotated = false;
            xoffset += 3 * cellWidth;
            yoffset = headFm.getAscent();
          } else {
            rotated = true;
            xoffset += 4 * cellWidth + 11 + headFm.getAscent();
            yoffset += 2 * cellHeight;
            if (inputCount > 4) xoffset += 4 * cellWidth;
            if (inputCount > 5) yoffset += 2 * cellHeight;
          }
          break;
        case 2:
          rotated = false;
          if (inputCount == 3) {
            xoffset += 2 * cellWidth;
            yoffset += 11 + 2 * cellHeight + headFm.getAscent();
          } else if (inputCount == 4) {
            xoffset += 3 * cellWidth;
            yoffset = headFm.getAscent();
          } else if (inputCount == 6) {
            xoffset += 11 + 8 * cellWidth + headFm.getAscent() + headHeight + (headHeight >> 2);
            yoffset += 2 * cellHeight;
            rotated = true;
          } else {
            xoffset += 6 * cellWidth;
            yoffset += 11 + 4 * cellHeight + headFm.getAscent();
          }
          break;
        case 3:
          rotated = false;
          if (inputCount == 4) {
            xoffset += 2 * cellWidth;
            yoffset += 11 + 4 * cellHeight + headFm.getAscent();
          } else if (inputCount == 6) {
            xoffset += 6 * cellWidth;
            yoffset += 11 + 8 * cellHeight + headFm.getAscent();
          } else {
            xoffset += 4 * cellWidth;
            yoffset = headFm.getAscent();
          }
          break;
        case 4:
          rotated = false;
          if (inputCount == 6) {
            xoffset += 4 * cellWidth;
            yoffset = headFm.getAscent();
          } else {
            xoffset += 2 * cellWidth;
            yoffset += 11 + 4 * cellHeight + headFm.getAscent() + headHeight + (headHeight >> 2);
          }
          break;
        case 5:
          rotated = false;
          xoffset += 2 * cellWidth;
          yoffset += 11 + 8 * cellHeight + headFm.getAscent() + headHeight + (headHeight >> 2);
          break;
        default:
          break;
      }
      if (rotated) {
        g.translate(xoffset + x, yoffset + y);
        g.rotate(-Math.PI / 2.0);
        g.drawString(header.getIterator(), -middleOffset, 0);
        g.rotate(Math.PI / 2.0);
        g.translate(-(xoffset + x), -(yoffset + y));
        if (i == 2 && inputCount == 6) {
          yoffset += 4 * cellHeight;
          g.translate(xoffset + x, yoffset + y);
          g.rotate(-Math.PI / 2.0);
          g.drawString(header.getIterator(), -middleOffset, 0);
          g.rotate(Math.PI / 2.0);
          g.translate(-(xoffset + x), -(yoffset + y));
        }
      } else g.drawString(header.getIterator(), xoffset + x - middleOffset, yoffset + y);
      if ((i == 4 && inputCount == 5) || (i == 5))
        g.drawString(header.getIterator(), 4 * cellWidth + xoffset + x - middleOffset, yoffset + y);
    }

    x += headHeight + 11;
    y += headHeight + 11;
    /* Here the lines are placed */
    switch (cols) {
      case 2:
        drawKmapLine(g, new Point(x + cellWidth, y - 8), new Point(x + 2 * cellWidth, y - 8));
        break;
      case 4:
        drawKmapLine(g, new Point(x + 2 * cellWidth, y - 8), new Point(x + 4 * cellWidth, y - 8));
        drawKmapLine(
            g,
            new Point(x + cellWidth, y + 9 + rows * cellHeight),
            new Point(x + 3 * cellWidth, y + 9 + rows * cellHeight));
        break;
      case 8:
        drawKmapLine(
            g,
            new Point(x + cellWidth, y + 8 + rows * cellHeight + headHeight + (headHeight >> 2)),
            new Point(
                x + 3 * cellWidth, y + 8 + rows * cellHeight + headHeight + (headHeight >> 2)));
        drawKmapLine(
            g,
            new Point(
                x + 5 * cellWidth, y + 8 + rows * cellHeight + headHeight + (headHeight >> 2)),
            new Point(
                x + 7 * cellWidth, y + 8 + rows * cellHeight + headHeight + (headHeight >> 2)));
        drawKmapLine(g, new Point(x + 2 * cellWidth, y - 8), new Point(x + 6 * cellWidth, y - 8));
        drawKmapLine(
            g,
            new Point(x + 4 * cellWidth, y + 8 + rows * cellHeight),
            new Point(x + 8 * cellWidth, y + 8 + rows * cellHeight));
        break;
    }
    switch (rows) {
      case 2:
        drawKmapLine(g, new Point(x - 8, y + cellHeight), new Point(x - 8, y + 2 * cellHeight));
        break;
      case 4:
        drawKmapLine(g, new Point(x - 8, y + 2 * cellHeight), new Point(x - 8, y + 4 * cellHeight));
        drawKmapLine(
            g,
            new Point(x + cols * cellWidth + 8, y + cellHeight),
            new Point(x + cols * cellWidth + 8, y + 3 * cellHeight));
        break;
      case 8:
        drawKmapLine(g, new Point(x - 8, y + 4 * cellHeight), new Point(x - 8, y + 8 * cellHeight));
        drawKmapLine(
            g,
            new Point(x + cols * cellWidth + 8, y + 2 * cellHeight),
            new Point(x + cols * cellWidth + 8, y + 6 * cellHeight));
        drawKmapLine(
            g,
            new Point(
                x + cols * cellWidth + 8 + headHeight + (headHeight >> 2), y + 1 * cellHeight),
            new Point(
                x + cols * cellWidth + 8 + headHeight + (headHeight >> 2), y + 3 * cellHeight));
        drawKmapLine(
            g,
            new Point(
                x + cols * cellWidth + 8 + headHeight + (headHeight >> 2), y + 5 * cellHeight),
            new Point(
                x + cols * cellWidth + 8 + headHeight + (headHeight >> 2), y + 7 * cellHeight));
        break;
    }
  }

  private void PaintKMap(Graphics2D g, int x, int y, TruthTable table) {
    int inputCount = table.getInputColumnCount();
    int rowVars = ROW_VARS[inputCount];
    int colVars = COL_VARS[inputCount];
    int rows = 1 << rowVars;
    int cols = 1 << colVars;
    g.setFont(EntryFont);
    FontMetrics fm = g.getFontMetrics();
    int dy = (cellHeight + fm.getAscent()) / 2;

    KMapArea = Bounds.create(x, y, cols * cellWidth, rows * cellHeight);
    Stroke oldstroke = g.getStroke();
    g.setStroke(new BasicStroke(2));
    g.drawLine(x - cellHeight, y - cellHeight, x, y);
    g.setStroke(oldstroke);
    int outputColumn = table.getOutputIndex(output);
    for (int i = 0; i < rows; i++) {
      for (int j = 0; j < cols; j++) {
        int row = getTableRow(i, j, rows, cols);
        Entry entry = table.getOutputEntry(row, outputColumn);
        if (provisionalValue != null && row == provisionalY && outputColumn == provisionalX)
          entry = provisionalValue;
        if (entry.isError()) {
          g.setColor(Value.ERROR_COLOR);
          g.fillRect(x + j * cellWidth, y + i * cellHeight, cellWidth, cellHeight);
          g.setColor(Color.BLACK);
        } else if (hover.x == j && hover.y == i) {
          g.fillRect(x + j * cellWidth, y + i * cellHeight, cellWidth, cellHeight);
        }
        g.setStroke(new BasicStroke(2));
        g.drawRect(x + j * cellWidth, y + i * cellHeight, cellWidth, cellHeight);
        g.setStroke(oldstroke);
      }
    }

    if (outputColumn < 0) return;

    kMapGroups.paint(g, x, y, cellWidth, cellHeight);
    g.setColor(Color.BLUE);
    for (int i = 0; i < rows; i++) {
      for (int j = 0; j < cols; j++) {
        int row = getTableRow(i, j, rows, cols);
        if (provisionalValue != null && row == provisionalY && outputColumn == provisionalX) {
          String text = provisionalValue.getDescription();
          g.setColor(Color.BLACK);
          g.drawString(
              text,
              x + j * cellWidth + (cellWidth - fm.stringWidth(text)) / 2,
              y + i * cellHeight + dy);
          g.setColor(Color.BLUE);
        } else {
          Entry entry = table.getOutputEntry(row, outputColumn);
          String text = entry.getDescription();
          g.drawString(
              text,
              x + j * cellWidth + (cellWidth - fm.stringWidth(text)) / 2,
              y + i * cellHeight + dy);
        }
      }
    }
    g.setColor(Color.BLACK);
  }

  public void setEntryProvisional(int y, int x, Entry value) {
    provisionalY = y;
    provisionalX = x;
    provisionalValue = value;
    repaint();
  }

  public void setOutput(String value) {
    boolean recompute = (output == null || value == null) && output != value;
    output = value;
    kMapGroups.setOutput(value);
    if (recompute) computePreferredSize();
    else repaint();
  }

  public void setFormat(int format) {
    kMapGroups.setformat(format);
  }

  private int toRow(int row, int rows) {
    if (rows > 4) {
      return BigCOL_Index[row];
    }
    if (rows == 4) {
      switch (row) {
        case 2:
          return 3;
        case 3:
          return 2;
        default:
          return row;
      }
    } else {
      return row;
    }
  }

  private int toCol(int col, int cols) {
    if (cols > 4) {
      return BigCOL_Index[col];
    }
    if (cols == 4) {
      switch (col) {
        case 2:
          return 3;
        case 3:
          return 2;
        default:
          return col;
      }
    } else {
      return col;
    }
  }

  public void mouseDragged(MouseEvent e) {}

  public void mouseMoved(MouseEvent e) {
    if (KMapArea == null) return;
    int posX = e.getX();
    int posY = e.getY();
    if ((posX >= KMapArea.getX())
        && (posX <= KMapArea.getX() + KMapArea.getWidth())
        && (posY >= KMapArea.getY())
        && (posY <= KMapArea.getY() + KMapArea.getHeight())) {
      int x = posX - KMapArea.getX();
      int y = posY - KMapArea.getY();
      int col = x / cellWidth;
      int row = y / cellHeight;
      if (kMapGroups.highlight(col, row)) {
        Expression expr = kMapGroups.GetHighlightedExpression();
        completeExpression.getRenderData().setSubExpression(expr);
        completeExpression.repaint();
        repaint();
      }
      if (col != hover.x || row != hover.y) {
        hover.x = col;
        hover.y = row;
        repaint();
      }
    } else {
      if (!kMapGroups.clearHighlight()) {
        completeExpression.getRenderData().setSubExpression(null);
        completeExpression.repaint();
        repaint();
      }
      if (hover.x >= 0 || hover.y >= 0) {
        hover.x = -1;
        hover.y = -1;
        repaint();
      }
    }
  }

  public void mouseClicked(MouseEvent e) {
    if (KMapArea == null) return;
    int row = getRow(e);
    if (row < 0) return;
    int col = getOutputColumn(e);
    TruthTable tt = model.getTruthTable();
    tt.expandVisibleRows();
    Entry entry = tt.getOutputEntry(row, col);
    if (entry.equals(Entry.DONT_CARE)) {
      tt.setOutputEntry(row, col, Entry.ZERO);
    } else if (entry.equals(Entry.ZERO)) {
      tt.setOutputEntry(row, col, Entry.ONE);
    } else if (entry.equals(Entry.ONE)) {
      tt.setOutputEntry(row, col, Entry.DONT_CARE);
    }
  }

  public void mousePressed(MouseEvent e) {}

  public void mouseReleased(MouseEvent e) {}

  public void mouseEntered(MouseEvent e) {}

  public void mouseExited(MouseEvent e) {}

  @Override
  public void EntryDesriptionChanged() { repaint(); }
}
