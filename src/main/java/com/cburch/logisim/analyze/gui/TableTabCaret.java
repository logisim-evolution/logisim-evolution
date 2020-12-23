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

import com.cburch.logisim.analyze.model.Entry;
import com.cburch.logisim.analyze.model.TruthTable;
import com.cburch.logisim.analyze.model.TruthTableEvent;
import com.cburch.logisim.analyze.model.TruthTableListener;
import com.cburch.logisim.util.GraphicsUtil;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.KeyStroke;

class TableTabCaret {
  private class Listener
      implements MouseListener,
          MouseMotionListener,
          KeyListener,
          FocusListener,
          TruthTableListener,
          ActionListener {

    public void cellsChanged(TruthTableEvent event) {}

    public void focusGained(FocusEvent e) {
      repaint(cursor);
    }

    public void focusLost(FocusEvent e) {
      repaint(cursor);
    }

    public void actionPerformed(ActionEvent event) {
      String action = event.getActionCommand();
      if (action.equals("1")) {
        doKey('1');
      } else if (action.equals("0")) {
        doKey('0');
      } else if (action.equals("x")) {
        doKey('-');
      } else if (action.equals("compact")) {
        final TruthTable tt = table.getTruthTable();
        if (tt.getRowCount() > 4096) {
          (new Analyzer.PleaseWait<Void>(S.get("tabcaretCompactRows"), table) {
                private static final long serialVersionUID = 1L;
				@Override
                public Void doInBackground() throws Exception {
                  tt.compactVisibleRows();
                  return null;
                }
              })
              .get();
        } else {
          tt.compactVisibleRows();
        }
      } else if (action.equals("expand")) {
        TruthTable model = table.getTruthTable();
        model.expandVisibleRows();
      }
    }

    public void keyPressed(KeyEvent e) {
      int rows = table.getRowCount();
      int inputs = table.getInputColumnCount();
      int outputs = table.getOutputColumnCount();
      int cols = inputs + outputs;
      boolean shift = (e.getModifiers() & InputEvent.SHIFT_MASK) != 0;
      Pt p = (shift ? markB.isValid() ? markB : markA : cursor);
      switch (e.getKeyCode()) {
        case KeyEvent.VK_UP:
          move(p.row - 1, p.col, shift);
          break;
        case KeyEvent.VK_LEFT:
          move(p.row, p.col - 1, shift);
          break;
        case KeyEvent.VK_DOWN:
          move(p.row + 1, p.col, shift);
          break;
        case KeyEvent.VK_RIGHT:
          move(p.row, p.col + 1, shift);
          break;
        case KeyEvent.VK_HOME:
          if (p.col == 0) move(0, 0, shift);
          else move(p.row, 0, shift);
          break;
        case KeyEvent.VK_END:
          if (p.col == cols - 1) move(rows - 1, cols - 1, shift);
          else move(p.row, cols - 1, shift);
          break;
        case KeyEvent.VK_PAGE_DOWN:
          rows = table.getBody().getVisibleRect().height / table.getCellHeight();
          if (rows > 2) rows--;
          move(p.row + rows, p.col, shift);
          break;
        case KeyEvent.VK_PAGE_UP:
          rows = table.getBody().getVisibleRect().height / table.getCellHeight();
          if (rows > 2) rows--;
          move(cursor.row - rows, cursor.col, shift);
          break;
      }
    }

    public void keyReleased(KeyEvent e) {}

    public void keyTyped(KeyEvent e) {
      int mask = e.getModifiers();
      if ((mask & ~InputEvent.SHIFT_MASK) != 0) return;
      char c = e.getKeyChar();
      doKey(c);
    }

    private int[] allRowsContaining(List<Integer> indexes) {
      TruthTable model = table.getTruthTable();
      int n = (indexes == null ? 0 : indexes.size());
      if (n == 0) return null;
      int rows[] = new int[n];
      for (int i = 0; i < n; i++) rows[i] = model.findVisibleRowContaining(indexes.get(i));
      Arrays.sort(rows);
      return rows;
    }

    private List<Integer> allIndexesForRowRange(int r1, int r2) {
      TruthTable model = table.getTruthTable();
      if (r1 < 0 || r2 < 0) return null;
      if (r1 > r2) {
        int t = r1;
        r1 = r2;
        r2 = t;
      }
      ArrayList<Integer> indexes = new ArrayList<>();
      for (int r = r1; r <= r2; r++) {
        for (Integer idx : model.getVisibleRowIndexes(r)) indexes.add(idx);
      }
      Collections.sort(indexes);
      return indexes;
    }

    void doKey(char c) {
      clearHilight();
      table.requestFocus();
      if (!cursor.isValid()) {
        if (!marked()) return;
        Rectangle s = getSelection();
        cursor = new Pt(s.y, s.x);
        repaint(cursor);
        scrollTo(cursor);
      }
      TruthTable model = table.getTruthTable();
      int inputs = table.getInputColumnCount();
      Entry newEntry = null;
      int dx = 1, dy = 0;
      switch (c) {
        case ' ':
          if (cursor.col < inputs) {
            Entry cur = model.getVisibleInputEntry(cursor.row, cursor.col);
            newEntry = (cur == Entry.DONT_CARE ? Entry.ZERO : Entry.ONE);
          } else {
            Entry cur = model.getVisibleOutputEntry(cursor.row, cursor.col - inputs);
            if (cur == Entry.ZERO) newEntry = Entry.ONE;
            else if (cur == Entry.ONE) newEntry = Entry.DONT_CARE;
            else newEntry = Entry.ZERO;
          }
          break;
        case '0':
        case 'f':
        case 'F':
          newEntry = Entry.ZERO;
          break;
        case '1':
        case 't':
        case 'T':
          newEntry = Entry.ONE;
          break;
        case '-':
        case 'x':
        case 'X':
          newEntry = Entry.DONT_CARE;
          break;
        case '\n':
          dy = 1;
          break;
        case '\u0008': // backspace
          newEntry = Entry.DONT_CARE;
          dx = -1;
          break;
        default:
          return;
      }
      if (newEntry != null && cursor.col < inputs) {
        // Nearly all of this is just trying to do a sensible
        // cursor/selection update.
        // FIXME: This is very inefficient for large tables. It
        // makes a round trip from row numbers to indexes and
        // back, for the cursor and the marks. But there is no
        // obvious way to get from an index to a row number
        // except for scanning all existing rows.
        // First: save the old state
        Pt oldCursor = cursor, oldMarkA = markA, oldMarkB = markB;
        List<Integer> oldCursorIdx, oldMarkIdx;
        oldCursorIdx = allIndexesForRowRange(cursor.row, cursor.row);
        oldMarkIdx = allIndexesForRowRange(markA.row, markB.row);
        // Second: do the actual update
        boolean updated = model.setVisibleInputEntry(cursor.row, cursor.col, newEntry, true);
        // Third: try to update the cursor and selection.
        if (updated) {
          // Update the cursor position
          cursor = invalid;
          int rows[] = allRowsContaining(oldCursorIdx);
          if (rows != null) {
            if (newEntry != Entry.ONE) cursor = new Pt(rows[0], oldCursor.col);
            else cursor = new Pt(rows[rows.length - 1], oldCursor.col);
            hilightRows = rows;
          }
          // Update the selection
          markA = cursor;
          markB = invalid;
          int marks[] = allRowsContaining(oldMarkIdx);
          if (marks != null) {
            int n = marks.length;
            if (isContiguous(marks)) {
              boolean fwd = oldMarkA.row <= oldMarkB.row;
              markA = new Pt(marks[fwd ? 0 : n - 1], oldMarkA.col);
              markB = new Pt(marks[fwd ? n - 1 : 0], oldMarkB.col);
            }
            hilightRows = marks;
          }
          table.repaint();
        }
      } else if (newEntry != null) {
        model.setVisibleOutputEntry(cursor.row, cursor.col - inputs, newEntry);
      }
      if (!markA.isValid() || !markB.isValid()) return;
      Rectangle s = getSelection();
      int row = cursor.row;
      int col = cursor.col;
      if (dy > 0) { // advance down
        col = s.x;
        if (++row >= s.y + s.height) row = s.y;
      } else if (dx > 0) { // advance right
        if (++col >= s.x + s.width) {
          col = s.x;
          if (++row >= s.y + s.height) {
            row = s.y;
          }
        }
      } else if (dx < 0) { // advance left
        if (--col < s.x) {
          col = s.x + s.width - 1;
          if (--row < s.y) row = s.y + s.height - 1;
        }
      }
      Pt oldCursor = cursor;
      cursor = new Pt(row, col);
      repaint(oldCursor, cursor, markA, markB);
      scrollTo(cursor);
    }

    public void mouseClicked(MouseEvent e) {
      if (cursor.isValid() && cursor.col >= table.getInputColumnCount()) {
        /* We clicked inside the output region; we mark the complete
         * region below the cursor.
         * markA is already set, so we set markB to the end of the table
         */
        markB = new Pt(table.getRowCount() - 1, markA.col);
        repaint(markA, markB);
      }
    }

    public void mouseDragged(MouseEvent e) {
      Pt oldMarkB = markB;
      markB = pointNear(e);
      repaint(oldMarkB, cursor, markA, markB);
    }

    public void mouseEntered(MouseEvent e) {
      Pt oldHover = hover;
      hover = pointAt(e);
      repaint(oldHover, hover);
    }

    public void mouseMoved(MouseEvent e) {
      Pt oldHover = hover;
      hover = pointAt(e);
      repaint(oldHover, hover);
    }

    public void mouseExited(MouseEvent e) {
      Pt oldHover = hover;
      hover = invalid;
      repaint(oldHover, hover);
    }

    public void mousePressed(MouseEvent e) {
      table.requestFocus();
      if ((e.getModifiers() & InputEvent.SHIFT_MASK) != 0) mouseDragged(e);
      else {
        setCursor(pointAt(e), pointNear(e));
      }
    }

    public void mouseReleased(MouseEvent e) {
      mouseDragged(e);
    }

    public void rowsChanged(TruthTableEvent event) {
      structureChanged(event);
    }

    public void structureChanged(TruthTableEvent event) {
      cursor = invalid;
      markA = invalid;
      markB = invalid;
      hover = invalid;
      clearHilight();
      repaint();
    }

    Pt pointAt(MouseEvent e) {
      return new Pt(table.getRow(e), table.getColumn(e));
    }

    Pt pointNear(MouseEvent e) {
      return new Pt(table.getNearestRow(e), table.getNearestColumn(e));
    }
  }

  private class Pt implements Comparable<Pt> {
    final int row, col;

    Pt() {
      row = -1;
      col = -1;
    }

    Pt(int r, int c) {
      row = r;
      col = c;
    }

    boolean isValid() {
      return row >= 0 && col >= 0 && row < table.getRowCount() && col < table.getColumnCount();
    }

    @Override
    public boolean equals(Object o) {
      if (!(o instanceof Pt)) return false;
      Pt other = (Pt) o;
      return (other.row == this.row && other.col == this.col)
          || (!other.isValid() && !this.isValid());
    }

    @Override
    public int compareTo(Pt other) {
      if (!other.isValid()) return (!this.isValid()) ? 0 : 1;
      else if (!this.isValid()) return -1;
      else if (other.row != this.row) return this.row - other.row;
      else return this.col - other.col;
    }

    @Override
    public String toString() {
      if (isValid()) return "Pt(" + row + ", " + col + ")";
      else return "Pt(?, ?)";
    }
  }

  private static Color SELECT_COLOR = new Color(192, 192, 255);
  private static Color HIGHLIGHT_COLOR = new Color(255, 255, 192);
  private Listener listener = new Listener();
  private TableTab table;
  private Pt cursor, markA, markB, hover, invalid, home;
  private int hilightRows[];
  private boolean cleanHilight;

  private void clearHilight() {
    if (hilightRows == null) return;
    cleanHilight = true;
    hilightRows = null;
  }

  public ActionListener getListener() {
    return listener;
  }

  TableTabCaret(TableTab table) {
    this.table = table;
    invalid = new Pt();
    home = new Pt(0, 0);
    cursor = home;
    markA = cursor;
    markB = invalid;
    hover = invalid;
    table.getTruthTable().addTruthTableListener(listener);
    table.getBody().addMouseListener(listener);
    table.getBody().addMouseMotionListener(listener);
    table.addKeyListener(listener);
    table.addFocusListener(listener);

    InputMap imap = table.getInputMap();
    ActionMap amap = table.getActionMap();
    AbstractAction nullAction =
        new AbstractAction() {
          private static final long serialVersionUID = 1L;

          public void actionPerformed(ActionEvent e) {}
        };
    String nullKey = "null";
    amap.put(nullKey, nullAction);
    imap.put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0), nullKey);
    imap.put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0), nullKey);
    imap.put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0), nullKey);
    imap.put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0), nullKey);
    imap.put(KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_DOWN, 0), nullKey);
    imap.put(KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_UP, 0), nullKey);
    imap.put(KeyStroke.getKeyStroke(KeyEvent.VK_HOME, 0), nullKey);
    imap.put(KeyStroke.getKeyStroke(KeyEvent.VK_END, 0), nullKey);
    imap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), nullKey);
  }

  boolean marked() {
    return (markA.isValid() && markB.isValid());
  }

  Rectangle getSelection() {
    if (marked()) {
      int r0 = Math.min(markA.row, markB.row);
      int c0 = Math.min(markA.col, markB.col);
      int r1 = Math.max(markA.row, markB.row);
      int c1 = Math.max(markA.col, markB.col);
      return new Rectangle(c0, r0, (c1 - c0) + 1, (r1 - r0) + 1);
    } else if (cursor.isValid()) {
      return new Rectangle(cursor.col, cursor.row, 1, 1);
    } else {
      return new Rectangle(0, 0, -1, -1);
    }
  }

  boolean hasSelection() {
    return marked() || cursor.isValid();
  }

  boolean hadSelection = false;
  void updateMenus() {
    boolean sel = hasSelection();
    if (hadSelection != sel) {
      hadSelection = sel;
      table.updateTab();
    }
  }

  void paintBackground(Graphics g) {
    if (hilightRows != null) {
      g.setColor(HIGHLIGHT_COLOR);
      int inputs = table.getInputColumnCount();
      int outputs = table.getOutputColumnCount();
      int x0 = table.getXLeft(0);
      int x1 = table.getXRight(inputs + outputs - 1);
      for (int r : hilightRows) {
        int y = table.getY(r);
        int h = table.getCellHeight();
        g.fillRect(x0, y, x1 - x0, h);
      }
    }
    if (marked() && !markA.equals(markB)) {
      Rectangle r = region(markA, markB);
      g.setColor(SELECT_COLOR);
      g.fillRect(r.x, r.y, r.width, r.height);
    }
    if (table.isFocusOwner() && cursor.isValid()) {
      Rectangle r = region(cursor);
      g.setColor(Color.WHITE);
      g.fillRect(r.x, r.y + 1, r.width - 1, r.height - 3);
    }
  }

  void paintForeground(Graphics g) {
    if (!table.isFocusOwner()) return;
    Pt p;
    if (cursor.isValid()) {
      p = cursor;
      g.setColor(Color.BLACK);
    } else if (hover.isValid()) {
      p = hover;
      g.setColor(Color.GRAY);
    } else {
      return;
    }
    int x = table.getXLeft(p.col);
    int y = table.getY(p.row);
    int w = table.getCellWidth(p.row);
    int h = table.getCellHeight();
    GraphicsUtil.switchToWidth(g, 2);
    g.drawRect(x - 1, y, w + 1, h - 2);
    GraphicsUtil.switchToWidth(g, 1);
  }

  void selectAll() {
    table.requestFocus();
    clearHilight();
    cursor = invalid;
    markA = new Pt(0, 0);
    markB = new Pt(table.getRowCount() - 1, table.getColumnCount() - 1);
    repaint(markA, markB);
  }

  private Pt pointNear(int row, int col) {
    int inputs = table.getInputColumnCount();
    int outputs = table.getOutputColumnCount();
    int rows = table.getRowCount();
    int cols = inputs + outputs;
    row = row < 0 ? 0 : row >= rows ? rows - 1 : row;
    col = col < 0 ? 0 : col >= cols ? cols - 1 : col;
    return new Pt(row, col);
  }

  private void move(int row, int col, boolean shift) {
    Pt p = pointNear(row, col);
    if (shift) {
      Pt oldMarkB = markB;
      markB = p;
      repaint(oldMarkB, cursor, markA, markB);
      scrollTo(markB);
    } else {
      setCursor(p, p);
    }
  }

  private void setCursor(Pt p, Pt m) {
    Pt oldCursor = cursor;
    Pt oldMarkA = markA;
    Pt oldMarkB = markB;
    clearHilight();
    cursor = p;
    markA = m;
    markB = invalid;
    repaint(oldCursor, oldMarkA, oldMarkB, cursor, markA, markB);
    if (cursor.isValid()) scrollTo(cursor);
  }

  private void scrollTo(Pt p) {
    if (!p.isValid()) return;
    int cx = table.getXLeft(p.col);
    int cy = table.getY(p.row);
    int cw = table.getCellWidth(p.col);
    int ch = table.getCellHeight();
    Rectangle r = new Rectangle(cx, cy, cw, ch);
    table.getBody().scrollRectToVisible(r);
  }

  private void repaint(Pt... pts) {
    updateMenus();
    if (cleanHilight) {
      cleanHilight = false;
      table.repaint();
      return;
    }
    Rectangle r = region(pts);
    if (r.isEmpty()) return;
    r.grow(2, 2);
    table.getBody().repaint(r);
  }

  private Rectangle region(Pt... pts) {
    int r0 = -1, r1 = -1, c0 = -1, c1 = -1;
    for (Pt p : pts) {
      if (p == null || !p.isValid()) continue;
      if (r0 == -1) {
        r0 = p.row;
        c0 = p.col;
        r1 = r0;
        c1 = c0;
      } else {
        r0 = Math.min(r0, p.row);
        c0 = Math.min(c0, p.col);
        r1 = Math.max(r1, p.row);
        c1 = Math.max(c1, p.col);
      }
    }
    if (r0 < 0) return new Rectangle(0, 0, -1, -1);
    int x0 = table.getXLeft(c0);
    int x1 = table.getXRight(c1);
    int y0 = table.getY(r0);
    int y1 = table.getY(r1) + table.getCellHeight();
    return new Rectangle(x0 - 2, y0 - 2, (x1 - x0) + 4, (y1 - y0) + 4);
  }

  private boolean isContiguous(int rows[]) {
    if (rows.length <= 1) return true;
    for (int i = 1; i < rows.length; i++) {
      if (Math.abs(rows[i] - rows[i]) > 1) return false;
    }
    return true;
  }
}
