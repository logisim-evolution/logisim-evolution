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

package com.cburch.logisim.gui.log;

import static com.cburch.logisim.gui.Strings.S;

import com.cburch.logisim.util.GraphicsUtil;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

class TablePanel extends LogPanel {
  private static final long serialVersionUID = 1L;
  private static final Font HEAD_FONT = new Font("Serif", Font.BOLD, 14);
  private static final Font BODY_FONT = new Font("Serif", Font.PLAIN, 14);
  private static final int COLUMN_SEP = 8;
  private static final int HEADER_SEP = 4;
  private final MyListener myListener = new MyListener();
  private final VerticalScrollBar vsb;
  private final TableView tableview;
  private int cellWidth = 25; // reasonable start values
  private int cellHeight = 15;
  private final int rowCount = 0;
  private int tableWidth;
  private int tableHeight;

  // FIXME: method is unused
  public TablePanel(LogFrame frame) {
    super(frame);
    vsb = new VerticalScrollBar();
    tableview = new TableView();
    JScrollPane pane =
        new JScrollPane(
            tableview,
            JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
            JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
    pane.setVerticalScrollBar(vsb);
    setLayout(new BorderLayout());
    add(pane);
    modelChanged(null, getModel());
  }

  public int getColumn(MouseEvent event) {
    Model model = getModel();
    int x = event.getX() - (getWidth() - tableWidth) / 2;
    if (x < 0) return -1;
    int ret = (x + COLUMN_SEP / 2) / (cellWidth + COLUMN_SEP);
    return ret >= 0 && ret < model.getSignalCount() ? ret : -1;
  }

  @Override
  public String getHelpText() {
    return S.get("tableHelp");
  }

  public int getRow(MouseEvent event) {
    int y = event.getY() - (getHeight() - tableHeight) / 2;
    if (y < cellHeight + HEADER_SEP) return -1;
    int ret = (y - cellHeight - HEADER_SEP) / cellHeight;
    return ret >= 0 && ret < rowCount ? ret : -1;
  }

  @Override
  public String getTitle() {
    return S.get("tableTab");
  }

  @Override
  public void localeChanged() {
    tableview.computePreferredSize();
    repaint();
  }

  @Override
  public void modelChanged(Model oldModel, Model newModel) {
    if (oldModel != null) oldModel.removeModelListener(myListener);
    if (newModel != null) newModel.addModelListener(myListener);
  }

  class TableView extends JPanel {
    private static final long serialVersionUID = 1L;

    private void computePreferredSize() {
      // todo: sizing is terrible
      Model model = getModel();
      int columns = model.getSignalCount();
      if (columns == 0) {
        setPreferredSize(new Dimension(0, 0));
        return;
      }

      Graphics g = getGraphics();
      if (g == null) {
        cellHeight = 16;
        cellWidth = 24;
      } else {
        FontMetrics fm = g.getFontMetrics(HEAD_FONT);
        cellHeight = fm.getHeight();
        cellWidth = 24;
        for (int i = 0; i < columns; i++) {
          String header = model.getItem(i).getShortName();
          cellWidth = Math.max(cellWidth, fm.stringWidth(header));
        }
      }
      tableWidth = (cellWidth + COLUMN_SEP) * columns - COLUMN_SEP;
      tableHeight = cellHeight * (1 + rowCount) + HEADER_SEP;
      setPreferredSize(new Dimension(tableWidth, tableHeight));
      revalidate();
      myListener.update();
      repaint();
    }

    @Override
    public void paintComponent(Graphics g) {
      super.paintComponent(g);

      Dimension sz = getSize();
      final int top = Math.max(0, (sz.height - tableHeight) / 2);
      final int left = Math.max(0, (sz.width - tableWidth) / 2);
      Model model = getModel();
      if (model == null) return;
      int columns = model.getSignalCount();
      if (columns == 0) {
        g.setFont(BODY_FONT);
        GraphicsUtil.drawCenteredText(g, S.get("tableEmptyMessage"), sz.width / 2, sz.height / 2);
        return;
      }

      g.setColor(Color.GRAY);
      int lineY = top + cellHeight + HEADER_SEP / 2;
      g.drawLine(left, lineY, left + tableWidth, lineY);

      g.setColor(Color.BLACK);
      g.setFont(HEAD_FONT);
      FontMetrics headerMetric = g.getFontMetrics();
      int x = left;
      int y = top + headerMetric.getAscent() + 1;
      for (int i = 0; i < columns; i++) {
        x = paintHeader(model.getItem(i).getShortName(), x, y, g, headerMetric);
      }
      g.setFont(BODY_FONT);
      FontMetrics bodyMetric = g.getFontMetrics();
      Rectangle clip = g.getClipBounds();
      int firstRow = Math.max(0, (clip.y - y) / cellHeight - 1);
      int lastRow = Math.min(rowCount, 2 + (clip.y + clip.height - y) / cellHeight);
      int y0 = top + cellHeight + HEADER_SEP;
      x = left;
      // for (int col = 0; col < columns; col++) {
      //   SignalInfo item = sel.get(col);
      //   ValueLog log = model.getValueLog(item);
      //   int offs = rowCount - log.size();
      //   y = y0 + Math.max(offs, firstRow) * cellHeight;
      //   for (int row = Math.max(offs, firstRow); row < lastRow; row++) {
      //     Value val = log.get(row - offs);
      //     String label = item.format(val);
      //     int width = bodyMetric.stringWidth(label);
      //     g.drawString(label, x + (cellWidth - width) / 2,
      //         y + bodyMetric.getAscent());
      //     y += cellHeight;
      //   }
      //   x += cellWidth + COLUMN_SEP;
      // }
    }

    private int paintHeader(String header, int x, int y, Graphics g, FontMetrics fm) {
      int width = fm.stringWidth(header);
      g.drawString(header, x + (cellWidth - width) / 2, y);
      return x + cellWidth + COLUMN_SEP;
    }
  }

  private static class MyListener implements Model.Listener {
    private void computeRowCount() {
      // dummy, private
    }

    void update() {
      // do nothing
    }

    @Override
    public void modeChanged(Model.Event event) {
      System.out.println("todo");
    }

    @Override
    public void historyLimitChanged(Model.Event event) {
      System.out.println("todo");
      // TODO: update(); maybe?
    }

    @Override
    public void signalsExtended(Model.Event event) {
      update();
    }

    @Override
    public void signalsReset(Model.Event event) {
      update();
    }

    @Override
    public void selectionChanged(Model.Event event) {
      computeRowCount();
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
      int curY = getValue();
      int curHeight = getVisibleAmount();
      int numCells = curHeight / cellHeight - 1;
      if (numCells <= 0) numCells = 1;
      if (direction > 0) {
        return curY > 0 ? numCells * cellHeight : numCells * cellHeight + HEADER_SEP;
      } else {
        return curY > cellHeight + HEADER_SEP
            ? numCells * cellHeight
            : numCells * cellHeight + HEADER_SEP;
      }
    }

    @Override
    public int getUnitIncrement(int direction) {
      int curY = getValue();
      if (direction > 0) {
        return curY > 0 ? cellHeight : cellHeight + HEADER_SEP;
      } else {
        return curY > cellHeight + HEADER_SEP ? cellHeight : cellHeight + HEADER_SEP;
      }
    }

    @Override
    public void stateChanged(ChangeEvent event) {
      int newMaximum = getMaximum();
      int newExtent = getVisibleAmount();
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
