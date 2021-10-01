/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.gui.log;

import static com.cburch.logisim.gui.Strings.S;

import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.util.GraphicsUtil;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.event.MouseEvent;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.ToolTipManager;
import javax.swing.border.Border;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 * Code taken from Cornell's version of Logisim: http://www.cs.cornell.edu/courses/cs3410/2015sp/
 */
public class ValueTable extends JPanel {

  private static final long serialVersionUID = 1L;
  private static final Font HEAD_FONT = new Font("Serif", Font.BOLD, 14);
  private static final Font BODY_FONT = new Font("Monospaced", Font.PLAIN, 14);
  private static final int COLUMN_SEP = 8;
  private static final int HEADER_SEP = 4;
  private final TableHeader header;
  private final TableBody body;
  private final VerticalScrollBar vsb;
  private final JScrollPane scrollPane;
  // cached copy of rows that are visible
  private Cell[][] rowData;
  private int rowStart;
  private int rowCount;
  private int[] columnWidth;
  private int cellHeight;
  private int tableWidth;
  private int tableHeight;
  private Model model;

  public ValueTable(Model model) {
    header = new TableHeader();
    body = new TableBody();
    vsb = new VerticalScrollBar();

    scrollPane =
        new JScrollPane(
            body,
            JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
            JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
    scrollPane.setVerticalScrollBar(vsb);
    scrollPane.setColumnHeaderView(header);
    scrollPane.getViewport().setBorder(null);
    Border b = scrollPane.getViewportBorder();
    scrollPane.setViewportBorder(null);
    scrollPane.setBorder(b);
    setLayout(new BorderLayout());
    add(scrollPane);

    ToolTipManager.sharedInstance().registerComponent(header);
    ToolTipManager.sharedInstance().registerComponent(body);

    setModel(model);
  }

  private void computePreferredSize() {
    final var oldCellHeight = cellHeight;
    final var oldTableWidth = tableWidth;
    final var oldTableHeight = tableHeight;
    final var columns = model == null ? 0 : model.getColumnCount();

    if (columnWidth == null || columnWidth.length < columns) columnWidth = new int[columns];

    if (columns == 0) {
      cellHeight = 16;
      tableWidth = tableHeight = 0;
    } else {
      final var g = getGraphics();
      var cellsWidth = 0;

      if (g == null) {
        cellHeight = 16;
        cellsWidth = 24 * columns;
      } else {
        final var headerMetric = g.getFontMetrics(HEAD_FONT);
        final var bodyMetric = g.getFontMetrics(BODY_FONT);
        cellHeight = Math.max(headerMetric.getHeight(), bodyMetric.getHeight());
        for (var i = 0; i < columns; i++) {
          final var radix = model.getColumnValueRadix(i);
          // column should be at least as wide as 24, as header, and
          // as formatted value
          final var header = model.getColumnName(i);
          var cellWidth = Math.max(24, headerMetric.stringWidth(header));
          final var w = model.getColumnValueWidth(i);

          if (w != null) {
            final var val =
                Value.createKnown(
                    w, (radix == 2 ? 0 : (radix == 10 ? (1 << (w.getWidth() - 1)) : w.getMask())));
            final var label = val.toDisplayString(radix);
            cellWidth = Math.max(cellWidth, bodyMetric.stringWidth(label));
          }

          columnWidth[i] = cellWidth;
          cellsWidth += cellWidth;
        }
      }

      tableWidth = cellsWidth + COLUMN_SEP * (columns + 1);
      tableHeight = cellHeight * model.getRowCount();
    }

    if (cellHeight != oldCellHeight
        || tableWidth != oldTableWidth
        || tableHeight != oldTableHeight) {
      final var headSize = new Dimension(tableWidth, cellHeight + HEADER_SEP);
      final var bodySize = new Dimension(tableWidth, tableHeight);
      body.setPreferredSize(bodySize);
      header.setPreferredSize(headSize);
      body.revalidate();
      header.revalidate();
    }
  }

  public void dataChanged() {
    rowCount = 0;
    repaint();
  }

  int findColumn(int x, int width) {
    var left = Math.max(0, (width - tableWidth) / 2);
    if (x < left + COLUMN_SEP || x >= left + tableWidth) return -1;
    left += COLUMN_SEP;
    final var columns = model.getColumnCount();

    for (var i = 0; i < columns; i++) {
      final var cellWidth = columnWidth[i];
      if (x >= left && x < left + cellWidth) return i;
      left += cellWidth + COLUMN_SEP;
    }
    return -1;
  }

  int findRow(int y, int height) {
    if (y < 0) return -1;
    final var row = y / cellHeight;
    if (row >= rowCount) return -1;

    return row;
  }

  public void modelChanged() {
    computePreferredSize();
    dataChanged();
  }

  void refreshData(int top, int bottom) {
    final var columns = model == null ? 0 : model.getColumnCount();

    if (columns == 0) {
      rowCount = 0;
      return;
    }
    final var rows = model.getRowCount();
    if (rows == 0) {
      rowCount = 0;
      return;
    }

    var topRow = Math.min(rows - 1, Math.max(0, top / cellHeight));
    var bottomRow = Math.min(rows - 1, Math.max(0, bottom / cellHeight));

    if (rowData != null
        && rowStart <= topRow
        && topRow < rowStart + rowCount
        && rowStart <= bottomRow
        && bottomRow < rowStart + rowCount) return;

    // we pre-fetch a bit more than strictly visible
    final var rect = scrollPane.getViewport().getViewRect();
    top = rect.y - rect.height / 2;
    bottom = rect.y + rect.height * 2;
    topRow = Math.min(rows - 1, Math.max(0, top / cellHeight - 10));
    bottomRow = Math.min(rows - 1, Math.max(0, bottom / cellHeight + 10));

    rowStart = Math.min(topRow, bottomRow);
    rowCount = Math.max(topRow, bottomRow) - rowStart + 1;

    if (rowCount == 0) return;

    if (rowData == null || rowData.length < rowCount || rowData[0].length != columns)
      rowData = new Cell[rowCount + 1][columns];

    model.getRowData(rowStart, rowCount, rowData);
  }

  public void setModel(Model model) {
    this.model = model;
    modelChanged();
  }

  public interface Model {

    void changeColumnValueRadix(int i);

    int getColumnCount();

    String getColumnName(int i);

    int getColumnValueRadix(int i);

    BitWidth getColumnValueWidth(int i);

    int getRowCount();

    void getRowData(int firstRow, int rowCount, Cell[][] rowData);
  }

  public static class Cell {

    public final Object value;
    public final Color bg;
    public final Color fg;
    public final String tip;

    public Cell(Object v, Color b, Color f, String t) {
      value = v;
      bg = b;
      fg = f;
      tip = t;
    }
  }

  private class TableBody extends JPanel {

    private static final long serialVersionUID = 1L;

    @Override
    public String getToolTipText(MouseEvent event) {
      int col = model == null ? -1 : findColumn(event.getX(), getSize().width);

      if (col < 0) return null;

      int row = rowData == null ? -1 : findRow(event.getY(), getSize().height);

      if (!(rowStart <= row && row < rowStart + rowCount)) return null;

      Cell cell = rowData[row - rowStart][col];

      if (cell == null) return null;

      return cell.tip;
    }

    @Override
    public void paintComponent(Graphics g) {
      super.paintComponent(g);

      final var sz = getSize();

      g.setColor(Color.BLACK);
      g.setFont(BODY_FONT);

      final var columns = model == null ? 0 : model.getColumnCount();
      if (columns == 0) {
        rowCount = 0;
        GraphicsUtil.drawCenteredText(g, S.get("tableEmptyMessage"), sz.width / 2, sz.height / 2);
        return;
      }

      final var bodyMetric = g.getFontMetrics();
      final var clip = g.getClipBounds();
      refreshData(clip.y, clip.y + clip.height);

      if (rowCount == 0) return;

      final var firstRow = Math.max(0, clip.y / cellHeight);
      final var lastRow = Math.min(model.getRowCount() - 1, (clip.y + clip.height) / cellHeight);

      var top = 0;
      final var left = Math.max(0, (sz.width - tableWidth) / 2);
      var x = left + COLUMN_SEP;

      final var bg = getBackground();

      for (int col = 0; col < columns; col++) {
        int y = top + firstRow * cellHeight;
        g.setColor(Color.GRAY);
        g.drawLine(x - COLUMN_SEP / 2, clip.y, x - COLUMN_SEP / 2, clip.y + clip.height);
        g.setColor(Color.BLACK);
        final var cellWidth = columnWidth[col];
        final var radix = model.getColumnValueRadix(col);

        for (var row = firstRow; row <= lastRow; row++) {
          if (!(rowStart <= row && row < rowStart + rowCount)) continue;
          final var cell = rowData[row - rowStart][col];

          if (cell == null) continue;

          g.setColor(cell.bg == null ? bg : cell.bg);
          g.fillRect(x - COLUMN_SEP / 2 + 1, y, cellWidth + COLUMN_SEP - 1, cellHeight);
          g.setColor(Color.BLACK);

          if (cell.value != null) {
            final var label =
                (cell.value instanceof Value
                    ? ((Value) cell.value).toDisplayString(radix)
                    : (String) cell.value);
            final var width = bodyMetric.stringWidth(label);

            if (cell.fg != null) g.setColor(cell.fg);

            g.drawString(label, x + (cellWidth - width) / 2, y + bodyMetric.getAscent());

            if (cell.fg != null) g.setColor(Color.BLACK);
          }
          y += cellHeight;
        }
        x += cellWidth + COLUMN_SEP;
      }
      g.setColor(Color.GRAY);
      g.drawLine(x - COLUMN_SEP / 2, clip.y, x - COLUMN_SEP / 2, clip.y + clip.height);
    }
  }

  private class TableHeader extends JPanel {

    private static final long serialVersionUID = 1L;

    TableHeader() {
      addMouseListener(new MyListener());
    }

    @Override
    public String getToolTipText(MouseEvent event) {
      int col = model == null ? -1 : findColumn(event.getX(), getSize().width);
      if (col < 0) return null;

      int radix = model.getColumnValueRadix(col);

      if (radix == 0) return null;

      return S.get("tableHeaderHelp", Integer.toString(radix));
    }

    @Override
    public void paintComponent(Graphics g) {
      super.paintComponent(g);

      final var sz = getSize();
      g.setColor(Color.GRAY);

      final var columns = model == null ? 0 : model.getColumnCount();
      if (columns == 0) {
        g.drawLine(0, cellHeight + HEADER_SEP / 2, sz.width, cellHeight + HEADER_SEP / 2);
        return;
      }

      g.setFont(HEAD_FONT);
      final var headerMetric = g.getFontMetrics();
      final var top = 0;
      final var left = Math.max(0, (sz.width - tableWidth) / 2);

      g.drawLine(left, cellHeight + HEADER_SEP / 2, left + tableWidth, cellHeight + HEADER_SEP / 2);

      var x = left + COLUMN_SEP;
      final var y = top + headerMetric.getAscent() + 1;

      for (var i = 0; i < columns; i++) {
        g.setColor(Color.GRAY);
        g.drawLine(x - COLUMN_SEP / 2, 0, x - COLUMN_SEP / 2, cellHeight);
        g.setColor(Color.BLACK);
        final var label = model.getColumnName(i);
        final var cellWidth = columnWidth[i];
        final var width = headerMetric.stringWidth(label);
        g.drawString(label, x + (cellWidth - width) / 2, y);
        x += cellWidth + COLUMN_SEP;
      }

      g.setColor(Color.GRAY);
      g.drawLine(x - COLUMN_SEP / 2, 0, x - COLUMN_SEP / 2, cellHeight);
    }

    class MyListener extends java.awt.event.MouseAdapter {
      @Override
      public void mouseClicked(MouseEvent e) {
        final var col = model == null ? -1 : findColumn(e.getX(), getSize().width);
        if (col >= 0) model.changeColumnValueRadix(col);
      }
    }
  }

  private class VerticalScrollBar extends JScrollBar implements ChangeListener {

    private static final long serialVersionUID = 1L;
    private int oldMaximum = -1;
    private int oldExtent = -1;

    public VerticalScrollBar() {
      getModel().addChangeListener(this);
    }

    @Override
    public int getBlockIncrement(int direction) {
      final var curHeight = getVisibleAmount();
      var numCells = curHeight / cellHeight - 1;
      if (numCells <= 0) numCells = 1;
      return numCells * cellHeight;
    }

    @Override
    public int getUnitIncrement(int direction) {
      return cellHeight;
    }

    @Override
    public void stateChanged(ChangeEvent event) {
      final var newMaximum = getMaximum();
      final var newExtent = getVisibleAmount();
      if (oldMaximum != newMaximum || oldExtent != newExtent) {
        if (getValue() + oldExtent >= oldMaximum) {
          setValue(newMaximum - newExtent);
        }
        oldMaximum = newMaximum;
        oldExtent = newExtent;
      }
    }
  }
}
