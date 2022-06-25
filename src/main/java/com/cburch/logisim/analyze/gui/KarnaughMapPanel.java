/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.analyze.gui;

import static com.cburch.logisim.analyze.Strings.S;

import com.cburch.contracts.BaseMouseListenerContract;
import com.cburch.contracts.BaseMouseMotionListenerContract;
import com.cburch.logisim.analyze.data.ExpressionRenderData;
import com.cburch.logisim.analyze.data.KarnaughMapGroups;
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
import com.cburch.logisim.util.LineBuffer;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseEvent;
import java.awt.font.FontRenderContext;
import java.awt.font.TextAttribute;
import java.awt.font.TextLayout;
import java.text.AttributedString;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javax.swing.JPanel;

public class KarnaughMapPanel extends JPanel implements BaseMouseMotionListenerContract, BaseMouseListenerContract, Entry.EntryChangedListener {
  private class MyListener implements OutputExpressionsListener, TruthTableListener {

    @Override
    public void rowsChanged(TruthTableEvent event) {
      // dummy
    }

    @Override
    public void cellsChanged(TruthTableEvent event) {
      karnaughMapGroups.update();
      repaint();
    }

    @Override
    public void expressionChanged(OutputExpressionsEvent event) {
      if (event.getType() == OutputExpressionsEvent.OUTPUT_MINIMAL && event.getVariable().equals(output)) {
        karnaughMapGroups.update();
        repaint();
      }
    }

    @Override
    public void structureChanged(TruthTableEvent event) {
      karnaughMapGroups.update();
      computePreferredSize();
    }
  }

  private static final long serialVersionUID = 1L;

  private static class KMapInfo {
    private final int headWidth;
    private final int headHeight;
    private final int width;
    private final int height;
    private int xOff;
    private int yOff;

    public KMapInfo(int headWidth, int headHeight, int tableWidth, int tableHeight) {
      xOff = 0;
      yOff = 0;
      this.headWidth = headWidth;
      this.headHeight = headHeight;
      this.width = tableWidth;
      this.height = tableHeight;
    }

    public void calculateOffsets(int boxWidth, int boxHeight) {
      xOff = (boxWidth - width) / 2;
      yOff = (boxHeight - height) / 2;
    }

    public int getXOffset() {
      return xOff;
    }

    public int getYOffset() {
      return yOff;
    }

    public int getWidth() {
      return width;
    }

    public int getHeight() {
      return height;
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
  private static final int[] bigColIndex = {0, 1, 3, 2, 6, 7, 5, 4};
  private static final int[] bigColPlace = {0, 1, 3, 2, 7, 6, 4, 5};
  private static final int cellHorizontalSeparator = 10;
  private static final int cellVerticalSeparator = 10;

  private final MyListener myListener = new MyListener();
  private final ExpressionView completeExpression;
  private final AnalyzerModel model;
  private String output;
  private int cellWidth = 1;
  private int cellHeight = 1;
  private int provisionalX;
  private int provisionalY;
  private Entry provisionalValue = null;
  private final Font headerFont;
  private final Font entryFont;
  private boolean isKMapLined;
  private Bounds kMapArea;
  private KMapInfo linedKMapInfo;
  private KMapInfo numberedKMapInfo;
  private final KarnaughMapGroups karnaughMapGroups;
  private Bounds selInfo;
  private final Point hover;
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
    entryFont = AppPreferences.getScaledFont(getFont());
    headerFont = entryFont.deriveFont(Font.BOLD);
    model.getOutputExpressions().addOutputExpressionsListener(myListener);
    model.getTruthTable().addTruthTableListener(myListener);
    setToolTipText(" ");
    isKMapLined = AppPreferences.KMAP_LINED_STYLE.get();
    karnaughMapGroups = new KarnaughMapGroups(model);
    addMouseMotionListener(this);
    addMouseListener(this);
    hover = new Point(-1, -1);
    final var f =
        new FocusListener() {
          @Override
          public void focusGained(FocusEvent e) {
            if (e.isTemporary()) return;
            selected = true;
            repaint();
          }

          @Override
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
    selInfo = null;
    final var g = (Graphics2D) getGraphics();
    final var table = model.getTruthTable();

    String message = null;
    if (output == null) {
      message = S.get("karnaughNoOutputError");
    } else if (table.getInputColumnCount() > MAX_VARS) {
      message = S.get("karnaughTooManyInputsError");
    } else if (table.getInputColumnCount() == 0) {
      message = S.get("karnaughNoInputsError");
    } else if (table.getInputColumnCount() == 1) {
      message = S.get("karnaughTooFewInputsError");
    }

    if (message != null) {
      if (g == null) {
        setPreferredSize(
            new Dimension(
                AppPreferences.getScaled(20 * message.length()),
                AppPreferences.getScaled(AppPreferences.BOX_SIZE)));
      } else {
        final var ctx = g.getFontRenderContext();
        final var msgLayout = new TextLayout(message, headerFont, ctx);
        setPreferredSize(
            new Dimension(
                (int) msgLayout.getBounds().getWidth(), (int) msgLayout.getBounds().getHeight()));
      }
    } else {
      computePreferredLinedSize(g, table);
      computePreferredNumberedSize(g, table);
      var boxWidth = Math.max(linedKMapInfo.getWidth(), numberedKMapInfo.getWidth());
      boxWidth = Math.max(boxWidth, AppPreferences.getScaled(300));
      var boxHeight = Math.max(linedKMapInfo.getHeight(), numberedKMapInfo.getHeight());
      linedKMapInfo.calculateOffsets(boxWidth, boxHeight);
      numberedKMapInfo.calculateOffsets(boxWidth, boxHeight);
      int selectedHeight = 0;
      if (g != null) {
        final var ctx = g.getFontRenderContext();
        final var t1 = new TextLayout(S.get("SelectedKmapGroup"), headerFont, ctx);
        selectedHeight = 3 * (int) t1.getBounds().getHeight();
      }
      selInfo = Bounds.create(0, boxHeight, boxWidth, selectedHeight);
      setPreferredSize(new Dimension(boxWidth, boxHeight + selectedHeight));
      kMapDim = new Dimension(boxWidth, boxHeight);
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
    final var lines = new ArrayList<TextLayout>();
    if (start >= end) return lines;
    final var ret = new StringBuilder(inputs.get(start));
    for (var i = start + 1; i < end; i++) {
      ret.append(", ");
      ret.append(inputs.get(i));
    }
    if (addComma) ret.append(",");
    final var maxSize = rowLabel ? (1 << (end - start - 1)) * cellWidth : 100;
    final var myLayout = styled(ret.toString(), headerFont, ctx);
    if (((end - start) <= 1) || (myLayout.getBounds().getWidth() <= maxSize)) {
      lines.add(myLayout);
      return lines;
    }
    final var nrOfEntries = end - start;
    if (nrOfEntries > 1) {
      final var half = nrOfEntries >> 1;
      lines.addAll(header(inputs, start, end - half, rowLabel, true, ctx));
      lines.addAll(header(inputs, end - half, end, rowLabel, addComma, ctx));
    } else {
      lines.add(myLayout);
    }
    return lines;
  }

  private void computePreferredNumberedSize(Graphics2D gfx, TruthTable table) {
    final var inputs = model.getInputs().bits;
    final var inputCount = table.getInputColumnCount();
    final var rowVars = ROW_VARS[inputCount];
    final var colVars = COL_VARS[inputCount];
    int headHeight;
    int headWidth;
    int tableHeight;

    if (gfx == null) {
      cellHeight = 16;
      cellWidth = 24;
    } else {
      final var fm = gfx.getFontMetrics(entryFont);
      cellHeight = fm.getAscent() + cellVerticalSeparator;
      cellWidth = fm.stringWidth("00") + cellHorizontalSeparator;
    }
    final var rows = 1 << rowVars;
    final var cols = 1 << colVars;
    final var bodyWidth = cellWidth * (cols + 1);
    final var bodyHeight = cellHeight * (rows + 1);

    int colLabelWidth;
    if (gfx == null) {
      headHeight = 16;
      headWidth = 80;
      colLabelWidth = 80;
    } else {
      final var ctx = gfx.getFontRenderContext();
      final var rowHeader = header(inputs, 0, rowVars, true, false, ctx);
      final var colHeader = header(inputs, rowVars, rowVars + colVars, false, false, ctx);
      headWidth = 0;
      var height = 0;
      for (TextLayout l : rowHeader) {
        final var w = (int) l.getBounds().getWidth();
        if (w > headWidth) headWidth = w;
      }
      colLabelWidth = 0;
      for (final var l : colHeader) {
        final var w = (int) l.getBounds().getWidth();
        final var h = (int) l.getBounds().getHeight();
        if (w > colLabelWidth) colLabelWidth = w;
        if (h > height) height = h;
      }
      headHeight = colHeader.size() * height;
    }
    tableHeight = headHeight + bodyHeight + 5;
    final var tableWidth = headWidth + Math.max(bodyWidth, colLabelWidth + cellWidth) + 5;
    numberedKMapInfo = new KMapInfo(headWidth, headHeight, tableWidth, tableHeight);
  }

  private void computePreferredLinedSize(Graphics2D gfx, TruthTable table) {
    int headHeight;
    final int headWidth;
    int tableWidth;
    int tableHeight;

    if (gfx == null) {
      headHeight = 16;
      cellHeight = 16;
      cellWidth = 24;
    } else {
      final var ctx = gfx.getFontRenderContext();
      var fm = gfx.getFontMetrics(headerFont);
      final var singleheight = styledHeight(styled("E", headerFont), ctx);
      headHeight = styledHeight(styled("E:2", headerFont), ctx) + (fm.getAscent() - singleheight);

      fm = gfx.getFontMetrics(entryFont);
      cellHeight = fm.getAscent() + cellVerticalSeparator;
      cellWidth = fm.stringWidth("00") + cellHorizontalSeparator;
    }

    final var rows = 1 << ROW_VARS[table.getInputColumnCount()];
    final var cols = 1 << COL_VARS[table.getInputColumnCount()];
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
    linedKMapInfo = new KMapInfo(headWidth, headHeight, tableWidth, tableHeight);
  }

  public static int getCol(int tableRow, int rows, int cols) {
    int ret = tableRow % cols;
    if (cols > 4) {
      return bigColPlace[ret];
    }
    return switch (ret) {
      case 2 -> 3;
      case 3 -> 2;
      default -> ret;
    };
  }

  public void setStyleLined() {
    isKMapLined = true;
    AppPreferences.KMAP_LINED_STYLE.set(true);
    repaint();
  }

  public void setStyleNumbered() {
    isKMapLined = false;
    AppPreferences.KMAP_LINED_STYLE.set(false);
    repaint();
  }

  public int getOutputColumn(MouseEvent event) {
    return model.getOutputs().bits.indexOf(output);
  }

  public static int getRow(int tableRow, int rows, int cols) {
    int ret = tableRow / cols;
    if (rows > 4) {
      return bigColPlace[ret];
    }
    return switch (ret) {
      case 2 -> 3;
      case 3 -> 2;
      default -> ret;
    };
  }

  public int getRow(MouseEvent event) {
    final var table = model.getTruthTable();
    final var inputs = table.getInputColumnCount();
    if (inputs >= ROW_VARS.length) return -1;
    final var x = event.getX() - kMapArea.getX();
    final var y = event.getY() - kMapArea.getY();
    if (x < 0 || y < 0) return -1;
    final var row = y / cellHeight;
    final var col = x / cellWidth;
    final var rows = 1 << ROW_VARS[inputs];
    final var cols = 1 << COL_VARS[inputs];
    if (row >= rows || col >= cols) return -1;
    return getTableRow(row, col, rows, cols);
  }

  private int getTableRow(int row, int col, int rows, int cols) {
    return toRow(row, rows) * cols + toCol(col, cols);
  }

  @Override
  public String getToolTipText(MouseEvent event) {
    if (kMapArea == null) return null;
    final var table = model.getTruthTable();
    final var row = getRow(event);
    if (row < 0) return null;
    final var col = getOutputColumn(event);
    final var entry = table.getOutputEntry(row, col);
    final var s = new StringBuilder(
        entry.getErrorMessage() == null
            ? ""
            : entry.getErrorMessage() + "<br>");
    s.append(output).append(" = ").append(entry.getDescription());
    final var inputs = model.getInputs().bits;
    if (inputs.isEmpty()) return "<html>" + s + "</html>";
    s.append("<br>When:");
    final var n = inputs.size();
    for (var i = 0; i < MAX_VARS && i < inputs.size(); i++) {
      s.append("<br/>&nbsp;&nbsp;&nbsp;&nbsp;").append(inputs.get(i)).append(" = ").append((row >> (n - i - 1)) & 1);
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
  public void paintComponent(Graphics gfx) {
    paintKmap(gfx, true);
  }

  public void paintKmap(Graphics gfx, boolean selectionBlock) {
    if (!(gfx instanceof Graphics2D g2)) return;
    if (AppPreferences.AntiAliassing.getBoolean()) {
      g2.setRenderingHint(
          RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
      g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    }
    final var col = g2.getColor();
    if (selectionBlock) {
      g2.setColor(getBackground());
      g2.fillRect(0, 0, getBounds().width, getBounds().height);
      g2.setColor(col);
    }

    final var table = model.getTruthTable();
    final var inputCount = table.getInputColumnCount();
    final var sz = getSize();
    String message = null;
    if (output == null) {
      message = S.get("karnaughNoOutputError");
    } else if (inputCount > MAX_VARS) {
      message = S.get("karnaughTooManyInputsError");
    } else if (inputCount == 0) {
      message = S.get("karnaughNoInputsError");
    } else if (inputCount == 1) {
      message = S.get("karnaughTooFewInputsError");
    }
    if (message != null) {
      gfx.setFont(headerFont);
      GraphicsUtil.drawCenteredText(g2, message, sz.width / 2, sz.height / 2);
      return;
    }

    int x;
    int y;
    if (isKMapLined) {
      x = linedKMapInfo.getXOffset();
      y = linedKMapInfo.getYOffset();
      drawLinedHeader(g2, x, y);
      x += linedKMapInfo.getHeaderHeight() + 11;
      y += linedKMapInfo.getHeaderHeight() + 11;
    } else {
      x = numberedKMapInfo.getXOffset();
      y = numberedKMapInfo.getYOffset();
      drawNumberedHeader(g2, x, y);
      x += numberedKMapInfo.getHeaderWidth() + cellWidth;
      y += numberedKMapInfo.getHeaderHeight() + cellHeight;
    }
    doPaintKMap(g2, x, y, table);
    if (!selectionBlock) return;

    final var expr = karnaughMapGroups.getHighlightedExpression();
    final var ctx = g2.getFontRenderContext();
    final var ccol = g2.getColor();
    final var bcol = karnaughMapGroups.getBackgroundColor();
    if (bcol != null) g2.setColor(bcol);
    else g2.setColor(this.getBackground());
    g2.fillRect(selInfo.getX(), selInfo.getY(), selInfo.getWidth() - 1, selInfo.getHeight() - 1);
    g2.setColor(ccol);
    g2.drawRect(selInfo.getX(), selInfo.getY(), selInfo.getWidth() - 1, selInfo.getHeight() - 1);
    if (expr == null) {
      final var t1 = new TextLayout(S.get("NoSelectedKmapGroup"), headerFont, ctx);
      final var xoff = (selInfo.getWidth() - (int) t1.getBounds().getWidth()) / 2;
      final var yoff = (selInfo.getHeight() - (int) t1.getBounds().getHeight()) / 2;
      t1.draw(g2, xoff + selInfo.getX(), yoff + selInfo.getY() + t1.getAscent());
    } else {
      final var t1 = new TextLayout(S.get("SelectedKmapGroup"), headerFont, ctx);
      var xoff = (selInfo.getWidth() - (int) t1.getBounds().getWidth()) / 2;
      t1.draw(g2, xoff + selInfo.getX(), selInfo.getY() + t1.getAscent());
      final var t2 = new ExpressionRenderData(expr, selInfo.getWidth(), notation);
      xoff = (selInfo.getWidth() - t2.getWidth()) / 2;
      t2.paint(gfx, xoff + selInfo.getX(), (int) (selInfo.getY() + t1.getAscent() + t1.getDescent()));
    }
  }

  public void setNotation(Notation notation) {
    if (notation == this.notation) return;
    this.notation = notation;
  }

  private String label(int row, int rows) {
    if (row < 0 || row >= rows) {
      throw new RuntimeException(LineBuffer.format("Row {{1}} is outside range of {{2}} rows.", row, rows));
    }
    return switch (rows) {
      case 2 -> String.valueOf(row);
      case 4 -> switch (row) {
        case 0 -> "00";
        case 1 -> "01";
        case 2 -> "11";
        case 3 -> "10";
        default -> throw new IllegalStateException(LineBuffer.format("Unexpected value: {{1}} for rows={{2}}", row, rows));
      };
      case 8 -> switch (row) {
        case 0 -> "000";
        case 1 -> "001";
        case 2 -> "011";
        case 3 -> "010";
        case 4 -> "110";
        case 5 -> "111";
        case 6 -> "101";
        case 7 -> "100";
        default -> throw new IllegalStateException(LineBuffer.format("Unexpected value: {{1}} for rows={{2}}", row, rows));
      };
      default -> throw new IllegalStateException(LineBuffer.format("Unhandled number of rows: {{1}}", rows));
    };
  }

  private void drawNumberedHeader(Graphics2D gfx, int x, int y) {
    final var table = model.getTruthTable();
    final var inputCount = table.getInputColumnCount();
    final var tableXstart = x + numberedKMapInfo.getHeaderWidth() + cellWidth;
    final var tableYstart = y + numberedKMapInfo.getHeaderHeight() + cellHeight;
    final var rowVars = ROW_VARS[inputCount];
    final var colVars = COL_VARS[inputCount];
    final var rows = 1 << rowVars;
    final var cols = 1 << colVars;
    final var headFm = gfx.getFontMetrics(headerFont);
    final var ctx = gfx.getFontRenderContext();
    var numberFont = headerFont;
    final var width2 = headFm.stringWidth("00");
    final var width3 = headFm.stringWidth("000");
    final var scale = (float) width2 / (float) width3;
    numberFont = headerFont.deriveFont(scale * headerFont.getSize2D());
    for (var c = 0; c < cols; c++) {
      final var label = label(c, cols);
      final var styledLabel = styled(label, numberFont, ctx);
      final var xoff = (cellWidth - (int) styledLabel.getBounds().getWidth()) >> 1;
      styledLabel.draw(gfx, tableXstart + xoff + c * cellWidth, tableYstart - 3 - (int) styledLabel.getDescent());
    }
    for (var r = 0; r < rows; r++) {
      final var label = label(r, rows);
      final var styledLabel = styled(label, numberFont, ctx);
      styledLabel.draw(
          gfx,
          (float) (tableXstart - styledLabel.getBounds().getWidth() - styledLabel.getDescent() - 3),
          tableYstart
              + (cellHeight - styledLabel.getAscent()) / 2
              + styledLabel.getAscent()
              + r * cellHeight);
    }
    final var rowHeader = header(model.getInputs().bits, 0, rowVars, true, false, ctx);
    final var colHeader =
        header(model.getInputs().bits, rowVars, rowVars + colVars, false, false, ctx);
    var rx = x + 3;
    var ry = y + numberedKMapInfo.getHeaderHeight() + cellHeight / 2;
    for (final var l : rowHeader) {
      l.draw(gfx, rx, ry + l.getAscent());
      ry += (int) l.getBounds().getHeight();
    }
    rx = x + numberedKMapInfo.getHeaderWidth() + cellWidth / 2;
    ry = y + 3;
    for (TextLayout l : colHeader) {
      l.draw(gfx, rx, ry + l.getAscent());
      ry += (int) l.getBounds().getHeight();
    }
  }

  private AttributedString styled(String header, Font font) {
    ArrayList<Integer> starts = new ArrayList<>();
    ArrayList<Integer> stops = new ArrayList<>();
    StringBuilder str = new StringBuilder();
    var idx = 0;
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
    final var styled = new AttributedString(str.toString());
    styled.addAttribute(TextAttribute.FAMILY, font.getFamily());
    styled.addAttribute(TextAttribute.SIZE, font.getSize());
    for (var i = 0; i < starts.size(); i++) {
      styled.addAttribute(TextAttribute.SUPERSCRIPT, TextAttribute.SUPERSCRIPT_SUB, starts.get(i), stops.get(i));
    }
    return styled;
  }

  private TextLayout styled(String header, Font font, FontRenderContext ctx) {
    return new TextLayout(styled(header, font).getIterator(), ctx);
  }

  private int styledWidth(AttributedString header, FontRenderContext ctx) {
    final var layout = new TextLayout(header.getIterator(), ctx);
    return (int) layout.getBounds().getWidth();
  }

  private int styledHeight(AttributedString header, FontRenderContext ctx) {
    final var layout = new TextLayout(header.getIterator(), ctx);
    return (int) layout.getBounds().getHeight();
  }

  private void drawKmapLine(Graphics2D gfx, Point p1, Point p2) {
    final var oldStroke = gfx.getStroke();
    gfx.setStroke(new BasicStroke(2));
    gfx.drawLine(p1.x, p1.y, p2.x, p2.y);
    if (p1.y == p2.y) {
      // we have a horizontal line
      gfx.drawLine(p1.x, p1.y - 4, p1.x, p1.y + 4);
      gfx.drawLine(p2.x, p2.y - 4, p2.x, p2.y + 4);
    } else {
      // we have a vertical line
      gfx.drawLine(p1.x - 4, p1.y, p1.x + 4, p1.y);
      gfx.drawLine(p2.x - 4, p2.y, p2.x + 4, p2.y);
    }
    gfx.setStroke(oldStroke);
  }

  private void drawLinedHeader(Graphics2D gfx, int x, int y) {
    final var table = model.getTruthTable();
    final var inputCount = table.getInputColumnCount();
    final var headFm = gfx.getFontMetrics(headerFont);
    final var ctx = gfx.getFontRenderContext();
    final var rowVars = ROW_VARS[inputCount];
    final var colVars = COL_VARS[inputCount];
    final var rows = 1 << rowVars;
    final int cols = 1 << colVars;
    final var headHeight = linedKMapInfo.getHeaderHeight();
    for (var i = 0; i < inputCount; i++) {
      final var header = styled(model.getInputs().bits.get(i), headerFont);
      var rotated = false;
      final var middleOffset = styledWidth(header, ctx) >> 1;
      var offsetX = headHeight + 11;
      var offsetY = headHeight + 11;
      switch (i) {
        case 0:
          if (inputCount == 1) {
            rotated = false;
            offsetX += cellWidth + cellWidth / 2;
            offsetY = headFm.getAscent();
          } else {
            rotated = true;
            offsetY += (rows - 1) * cellHeight;
            if (inputCount < 4) offsetY += cellHeight / 2;
            if (inputCount > 5) offsetY -= cellHeight;
            offsetX = headFm.getAscent();
          }
          break;
        case 1:
          if (inputCount == 2) {
            rotated = false;
            offsetX += cellWidth + cellWidth / 2;
            offsetY = headFm.getAscent();
          } else if (inputCount == 3) {
            rotated = false;
            offsetX += 3 * cellWidth;
            offsetY = headFm.getAscent();
          } else {
            rotated = true;
            offsetX += 4 * cellWidth + 11 + headFm.getAscent();
            offsetY += 2 * cellHeight;
            if (inputCount > 4) offsetX += 4 * cellWidth;
            if (inputCount > 5) offsetY += 2 * cellHeight;
          }
          break;
        case 2:
          rotated = false;
          if (inputCount == 3) {
            offsetX += 2 * cellWidth;
            offsetY += 11 + 2 * cellHeight + headFm.getAscent();
          } else if (inputCount == 4) {
            offsetX += 3 * cellWidth;
            offsetY = headFm.getAscent();
          } else if (inputCount == 6) {
            offsetX += 11 + 8 * cellWidth + headFm.getAscent() + headHeight + (headHeight >> 2);
            offsetY += 2 * cellHeight;
            rotated = true;
          } else {
            offsetX += 6 * cellWidth;
            offsetY += 11 + 4 * cellHeight + headFm.getAscent();
          }
          break;
        case 3:
          rotated = false;
          if (inputCount == 4) {
            offsetX += 2 * cellWidth;
            offsetY += 11 + 4 * cellHeight + headFm.getAscent();
          } else if (inputCount == 6) {
            offsetX += 6 * cellWidth;
            offsetY += 11 + 8 * cellHeight + headFm.getAscent();
          } else {
            offsetX += 4 * cellWidth;
            offsetY = headFm.getAscent();
          }
          break;
        case 4:
          rotated = false;
          if (inputCount == 6) {
            offsetX += 4 * cellWidth;
            offsetY = headFm.getAscent();
          } else {
            offsetX += 2 * cellWidth;
            offsetY += 11 + 4 * cellHeight + headFm.getAscent() + headHeight + (headHeight >> 2);
          }
          break;
        case 5:
          rotated = false;
          offsetX += 2 * cellWidth;
          offsetY += 11 + 8 * cellHeight + headFm.getAscent() + headHeight + (headHeight >> 2);
          break;
        default:
          break;
      }
      if (rotated) {
        gfx.translate(offsetX + x, offsetY + y);
        gfx.rotate(-Math.PI / 2.0);
        gfx.drawString(header.getIterator(), -middleOffset, 0);
        gfx.rotate(Math.PI / 2.0);
        gfx.translate(-(offsetX + x), -(offsetY + y));
        if (i == 2 && inputCount == 6) {
          offsetY += 4 * cellHeight;
          gfx.translate(offsetX + x, offsetY + y);
          gfx.rotate(-Math.PI / 2.0);
          gfx.drawString(header.getIterator(), -middleOffset, 0);
          gfx.rotate(Math.PI / 2.0);
          gfx.translate(-(offsetX + x), -(offsetY + y));
        }
      } else {
        gfx.drawString(header.getIterator(), offsetX + x - middleOffset, offsetY + y);
      }
      if ((i == 4 && inputCount == 5) || (i == 5)) {
        gfx.drawString(header.getIterator(), 4 * cellWidth + offsetX + x - middleOffset, offsetY + y);
      }
    }

    x += headHeight + 11;
    y += headHeight + 11;
    /* Here the lines are placed */
    switch (cols) {
      case 2 ->
        drawKmapLine(gfx, new Point(x + cellWidth, y - 8), new Point(x + 2 * cellWidth, y - 8));
      case 4 -> {
        drawKmapLine(gfx, new Point(x + 2 * cellWidth, y - 8), new Point(x + 4 * cellWidth, y - 8));
        drawKmapLine(
            gfx,
            new Point(x + cellWidth, y + 9 + rows * cellHeight),
            new Point(x + 3 * cellWidth, y + 9 + rows * cellHeight));
      }
      case 8 -> {
        drawKmapLine(
            gfx,
            new Point(x + cellWidth, y + 8 + rows * cellHeight + headHeight + (headHeight >> 2)),
            new Point(
                x + 3 * cellWidth, y + 8 + rows * cellHeight + headHeight + (headHeight >> 2)));
        drawKmapLine(
            gfx,
            new Point(
                x + 5 * cellWidth, y + 8 + rows * cellHeight + headHeight + (headHeight >> 2)),
            new Point(
                x + 7 * cellWidth, y + 8 + rows * cellHeight + headHeight + (headHeight >> 2)));
        drawKmapLine(gfx, new Point(x + 2 * cellWidth, y - 8), new Point(x + 6 * cellWidth, y - 8));
        drawKmapLine(
            gfx,
            new Point(x + 4 * cellWidth, y + 8 + rows * cellHeight),
            new Point(x + 8 * cellWidth, y + 8 + rows * cellHeight));
      }
      default -> {
        // none
      }
    }
    switch (rows) {
      case 2 ->
          drawKmapLine(gfx, new Point(x - 8, y + cellHeight), new Point(x - 8, y + 2 * cellHeight));
      case 4 -> {
        drawKmapLine(gfx, new Point(x - 8, y + 2 * cellHeight),
            new Point(x - 8, y + 4 * cellHeight));
        drawKmapLine(
            gfx,
            new Point(x + cols * cellWidth + 8, y + cellHeight),
            new Point(x + cols * cellWidth + 8, y + 3 * cellHeight));
      }
      case 8 -> {
        drawKmapLine(gfx, new Point(x - 8, y + 4 * cellHeight),
            new Point(x - 8, y + 8 * cellHeight));
        drawKmapLine(
            gfx,
            new Point(x + cols * cellWidth + 8, y + 2 * cellHeight),
            new Point(x + cols * cellWidth + 8, y + 6 * cellHeight));
        drawKmapLine(
            gfx,
            new Point(
                x + cols * cellWidth + 8 + headHeight + (headHeight >> 2), y + 1 * cellHeight),
            new Point(
                x + cols * cellWidth + 8 + headHeight + (headHeight >> 2), y + 3 * cellHeight));
        drawKmapLine(
            gfx,
            new Point(
                x + cols * cellWidth + 8 + headHeight + (headHeight >> 2), y + 5 * cellHeight),
            new Point(
                x + cols * cellWidth + 8 + headHeight + (headHeight >> 2), y + 7 * cellHeight));
      }
      default -> {
        // none
      }
    }
  }

  private void doPaintKMap(Graphics2D gfx, int x, int y, TruthTable table) {
    final var inputCount = table.getInputColumnCount();
    final var rowVars = ROW_VARS[inputCount];
    final var colVars = COL_VARS[inputCount];
    final var rows = 1 << rowVars;
    final var cols = 1 << colVars;
    gfx.setFont(entryFont);
    final var fm = gfx.getFontMetrics();
    final var dy = (cellHeight + fm.getAscent()) / 2;

    kMapArea = Bounds.create(x, y, cols * cellWidth, rows * cellHeight);
    final var oldstroke = gfx.getStroke();
    gfx.setStroke(new BasicStroke(2));
    gfx.drawLine(x - cellHeight, y - cellHeight, x, y);
    gfx.setStroke(oldstroke);
    final var outputColumn = table.getOutputIndex(output);
    for (var i = 0; i < rows; i++) {
      for (var j = 0; j < cols; j++) {
        final var row = getTableRow(i, j, rows, cols);
        var entry = table.getOutputEntry(row, outputColumn);
        if (provisionalValue != null && row == provisionalY && outputColumn == provisionalX)
          entry = provisionalValue;
        if (entry.isError()) {
          gfx.setColor(Value.errorColor);
          gfx.fillRect(x + j * cellWidth, y + i * cellHeight, cellWidth, cellHeight);
          gfx.setColor(Color.BLACK);
        } else if (hover.x == j && hover.y == i) {
          gfx.fillRect(x + j * cellWidth, y + i * cellHeight, cellWidth, cellHeight);
        }
        gfx.setStroke(new BasicStroke(2));
        gfx.drawRect(x + j * cellWidth, y + i * cellHeight, cellWidth, cellHeight);
        gfx.setStroke(oldstroke);
      }
    }

    if (outputColumn < 0) return;

    karnaughMapGroups.paint(gfx, x, y, cellWidth, cellHeight);
    gfx.setColor(Color.BLUE);
    for (var i = 0; i < rows; i++) {
      for (var j = 0; j < cols; j++) {
        final var row = getTableRow(i, j, rows, cols);
        if (provisionalValue != null && row == provisionalY && outputColumn == provisionalX) {
          final var text = provisionalValue.getDescription();
          gfx.setColor(Color.BLACK);
          gfx.drawString(
              text,
              x + j * cellWidth + (cellWidth - fm.stringWidth(text)) / 2,
              y + i * cellHeight + dy);
          gfx.setColor(Color.BLUE);
        } else {
          final var entry = table.getOutputEntry(row, outputColumn);
          final var text = entry.getDescription();
          gfx.drawString(
              text,
              x + j * cellWidth + (cellWidth - fm.stringWidth(text)) / 2,
              y + i * cellHeight + dy);
        }
      }
    }
    gfx.setColor(Color.BLACK);
  }

  public void setEntryProvisional(int y, int x, Entry value) {
    provisionalY = y;
    provisionalX = x;
    provisionalValue = value;
    repaint();
  }

  public void setOutput(String value) {
    final var recompute = (output == null || value == null) && !Objects.equals(output, value);
    output = value;
    karnaughMapGroups.setOutput(value);
    if (recompute) {
      computePreferredSize();
    } else {
      repaint();
    }
  }

  public void setFormat(int format) {
    karnaughMapGroups.setformat(format);
  }

  private int toRow(int row, int rows) {
    if (rows > 4) {
      return bigColIndex[row];
    }
    if (rows == 4) {
      return switch (row) {
        case 2 -> 3;
        case 3 -> 2;
        default -> row;
      };
    } else {
      return row;
    }
  }

  private int toCol(int col, int cols) {
    if (cols > 4) {
      return bigColIndex[col];
    }
    if (cols == 4) {
      return switch (col) {
        case 2 -> 3;
        case 3 -> 2;
        default -> col;
      };
    } else {
      return col;
    }
  }

  @Override
  public void mouseMoved(MouseEvent e) {
    if (kMapArea == null) return;
    final var posX = e.getX();
    final var posY = e.getY();
    if ((posX >= kMapArea.getX())
        && (posX <= kMapArea.getX() + kMapArea.getWidth())
        && (posY >= kMapArea.getY())
        && (posY <= kMapArea.getY() + kMapArea.getHeight())) {
      final var x = posX - kMapArea.getX();
      final var y = posY - kMapArea.getY();
      final var col = x / cellWidth;
      final var row = y / cellHeight;
      if (karnaughMapGroups.highlight(col, row)) {
        Expression expr = karnaughMapGroups.getHighlightedExpression();
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
      if (!karnaughMapGroups.clearHighlight()) {
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

  @Override
  public void mouseClicked(MouseEvent e) {
    if (kMapArea == null) return;
    final var row = getRow(e);
    if (row < 0) return;
    final var col = getOutputColumn(e);
    final var tt = model.getTruthTable();
    tt.expandVisibleRows();
    final var entry = tt.getOutputEntry(row, col);
    if (entry.equals(Entry.DONT_CARE)) {
      tt.setOutputEntry(row, col, Entry.ZERO);
    } else if (entry.equals(Entry.ZERO)) {
      tt.setOutputEntry(row, col, Entry.ONE);
    } else if (entry.equals(Entry.ONE)) {
      tt.setOutputEntry(row, col, Entry.DONT_CARE);
    }
  }

  @Override
  public void entryDesriptionChanged() {
    repaint();
  }
}
