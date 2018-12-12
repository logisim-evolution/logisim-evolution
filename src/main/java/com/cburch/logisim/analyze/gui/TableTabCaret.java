/*******************************************************************************
 * This file is part of logisim-evolution.
 *
 *   logisim-evolution is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   logisim-evolution is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with logisim-evolution.  If not, see <http://www.gnu.org/licenses/>.
 *
 *   Original code by Carl Burch (http://www.cburch.com), 2011.
 *   Subsequent modifications by :
 *     + Haute École Spécialisée Bernoise
 *       http://www.bfh.ch
 *     + Haute École du paysage, d'ingénierie et d'architecture de Genève
 *       http://hepia.hesge.ch/
 *     + Haute École d'Ingénierie et de Gestion du Canton de Vaud
 *       http://www.heig-vd.ch/
 *   The project is currently maintained by :
 *     + REDS Institute - HEIG-VD
 *       Yverdon-les-Bains, Switzerland
 *       http://reds.heig-vd.ch
 *******************************************************************************/

package com.cburch.logisim.analyze.gui;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.KeyStroke;

import com.cburch.logisim.analyze.model.Entry;
import com.cburch.logisim.analyze.model.TruthTable;
import com.cburch.logisim.analyze.model.TruthTableEvent;
import com.cburch.logisim.analyze.model.TruthTableListener;
import com.cburch.logisim.util.GraphicsUtil;

class TableTabCaret {
	private class Listener implements MouseListener, MouseMotionListener,
			KeyListener, FocusListener, TruthTableListener {

		public void cellsChanged(TruthTableEvent event) {
		}

		public void focusGained(FocusEvent e) {
			if (cursorRow >= 0)
				expose(cursorRow, cursorCol);
		}

		public void focusLost(FocusEvent e) {
			if (cursorRow >= 0)
				expose(cursorRow, cursorCol);
		}

		public void keyPressed(KeyEvent e) {
			if (cursorRow < 0)
				return;
			TruthTable model = table.getTruthTable();
			int rows = model.getRowCount();
			int inputs = model.getInputColumnCount();
			int outputs = model.getOutputColumnCount();
			int cols = inputs + outputs;
			boolean shift = (e.getModifiers() & InputEvent.SHIFT_MASK) != 0;
			switch (e.getKeyCode()) {
			case KeyEvent.VK_UP:
				setCursor(cursorRow - 1, cursorCol, shift);
				break;
			case KeyEvent.VK_LEFT:
				setCursor(cursorRow, cursorCol - 1, shift);
				break;
			case KeyEvent.VK_DOWN:
				setCursor(cursorRow + 1, cursorCol, shift);
				break;
			case KeyEvent.VK_RIGHT:
				setCursor(cursorRow, cursorCol + 1, shift);
				break;
			case KeyEvent.VK_HOME:
				if (cursorCol == 0)
					setCursor(0, 0, shift);
				else
					setCursor(cursorRow, 0, shift);
				break;
			case KeyEvent.VK_END:
				if (cursorCol == cols - 1)
					setCursor(rows - 1, cols - 1, shift);
				else
					setCursor(cursorRow, cols - 1, shift);
				break;
			case KeyEvent.VK_PAGE_DOWN:
				rows = table.getVisibleRect().height / table.getCellHeight();
				if (rows > 2)
					rows--;
				setCursor(cursorRow + rows, cursorCol, shift);
				break;
			case KeyEvent.VK_PAGE_UP:
				rows = table.getVisibleRect().height / table.getCellHeight();
				if (rows > 2)
					rows--;
				setCursor(cursorRow - rows, cursorCol, shift);
				break;
			}
		}

		public void keyReleased(KeyEvent e) {
		}

		public void keyTyped(KeyEvent e) {
			int mask = e.getModifiers();
			if ((mask & ~InputEvent.SHIFT_MASK) != 0)
				return;

			char c = e.getKeyChar();
			Entry newEntry = null;
			switch (c) {
			case ' ':
				if (cursorRow >= 0) {
					TruthTable model = table.getTruthTable();
					int inputs = model.getInputColumnCount();
					if (cursorCol >= inputs) {
						Entry cur = model.getOutputEntry(cursorRow, cursorCol
								- inputs);
						if (cur == Entry.ZERO)
							cur = Entry.ONE;
						else if (cur == Entry.ONE)
							cur = Entry.DONT_CARE;
						else
							cur = Entry.ZERO;
						model.setOutputEntry(cursorRow, cursorCol - inputs, cur);
					}
				}
				break;
			case '0':
				newEntry = Entry.ZERO;
				break;
			case '1':
				newEntry = Entry.ONE;
				break;
			case '-':
				newEntry = Entry.DONT_CARE;
				break;
			case '\n':
				setCursor(cursorRow + 1, table.getTruthTable()
						.getInputColumnCount(),
						(mask & InputEvent.SHIFT_MASK) != 0);
				break;
			case '\u0008':
			case '\u007f':
				setCursor(cursorRow, cursorCol - 1,
						(mask & InputEvent.SHIFT_MASK) != 0);
				break;
			default:
			}
			if (newEntry != null) {
				TruthTable model = table.getTruthTable();
				int inputs = model.getInputColumnCount();
				int outputs = model.getOutputColumnCount();
				if (cursorCol >= inputs) {
					model.setOutputEntry(cursorRow, cursorCol - inputs,
							newEntry);
					if (cursorCol >= inputs + outputs - 1) {
						setCursor(cursorRow + 1, inputs, false);
					} else {
						setCursor(cursorRow, cursorCol + 1, false);
					}
				}
			}
		}

		public void mouseClicked(MouseEvent e) {
		}

		public void mouseDragged(MouseEvent e) {
			int row = table.getRow(e);
			int col = table.getColumn(e);
			setCursor(row, col, true);
		}

		public void mouseEntered(MouseEvent e) {
		}

		public void mouseExited(MouseEvent e) {
		}

		public void mouseMoved(MouseEvent e) {
		}

		public void mousePressed(MouseEvent e) {
			table.requestFocus();
			int row = table.getRow(e);
			int col = table.getColumn(e);
			setCursor(row, col, (e.getModifiers() & InputEvent.SHIFT_MASK) != 0);
		}

		public void mouseReleased(MouseEvent e) {
			mouseDragged(e);
		}

		public void structureChanged(TruthTableEvent event) {
			TruthTable model = event.getSource();
			int inputs = model.getInputColumnCount();
			int outputs = model.getOutputColumnCount();
			int rows = model.getRowCount();
			int cols = inputs + outputs;
			boolean changed = false;
			if (cursorRow >= rows) {
				cursorRow = rows - 1;
				changed = true;
			}
			if (cursorCol >= cols) {
				cursorCol = cols - 1;
				changed = true;
			}
			if (markRow >= rows) {
				markRow = rows - 1;
				changed = true;
			}
			if (markCol >= cols) {
				markCol = cols - 1;
				changed = true;
			}
			if (changed)
				table.repaint();
		}
	}

	private static Color SELECT_COLOR = new Color(192, 192, 255);
	private Listener listener = new Listener();
	private TableTab table;
	private int cursorRow;
	private int cursorCol;
	private int markRow;

	private int markCol;

	TableTabCaret(TableTab table) {
		this.table = table;
		cursorRow = 0;
		cursorCol = 0;
		markRow = 0;
		markCol = 0;
		table.getTruthTable().addTruthTableListener(listener);
		table.addMouseListener(listener);
		table.addMouseMotionListener(listener);
		table.addKeyListener(listener);
		table.addFocusListener(listener);

		InputMap imap = table.getInputMap();
		ActionMap amap = table.getActionMap();
		AbstractAction nullAction = new AbstractAction() {
			private static final long serialVersionUID = 1L;

			public void actionPerformed(ActionEvent e) {
			}
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

	private void expose(int row, int col) {
		if (row >= 0) {
			int x0 = table.getX(0);
			int x1 = table.getX(table.getColumnCount() - 1)
					+ table.getCellWidth();
			table.repaint(x0 - 2, table.getY(row) - 2, (x1 - x0) + 4,
					table.getCellHeight() + 4);
		}
	}

	int getCursorCol() {
		return cursorCol;
	}

	int getCursorRow() {
		return cursorRow;
	}

	int getMarkCol() {
		return markCol;
	}

	int getMarkRow() {
		return markRow;
	}

	void paintBackground(Graphics g) {
		if (cursorRow >= 0 && cursorCol >= 0
				&& (cursorRow != markRow || cursorCol != markCol)) {
			g.setColor(SELECT_COLOR);

			int r0 = cursorRow;
			int c0 = cursorCol;
			int r1 = markRow;
			int c1 = markCol;
			if (r1 < r0) {
				int t = r1;
				r1 = r0;
				r0 = t;
			}
			if (c1 < c0) {
				int t = c1;
				c1 = c0;
				c0 = t;
			}
			int x0 = table.getX(c0);
			int y0 = table.getY(r0);
			int x1 = table.getX(c1) + table.getCellWidth();
			int y1 = table.getY(r1) + table.getCellHeight();
			g.fillRect(x0, y0, x1 - x0, y1 - y0);
		}
	}

	void paintForeground(Graphics g) {
		if (!table.isFocusOwner())
			return;
		if (cursorRow >= 0 && cursorCol >= 0) {
			int x = table.getX(cursorCol);
			int y = table.getY(cursorRow);
			GraphicsUtil.switchToWidth(g, 2);
			g.drawRect(x, y, table.getCellWidth(), table.getCellHeight());
			GraphicsUtil.switchToWidth(g, 2);
		}
	}

	void selectAll() {
		table.requestFocus();
		TruthTable model = table.getTruthTable();
		setCursor(model.getRowCount(),
				model.getInputColumnCount() + model.getOutputColumnCount(),
				false);
		setCursor(0, 0, true);
	}

	private void setCursor(int row, int col, boolean keepMark) {
		TruthTable model = table.getTruthTable();
		int rows = model.getRowCount();
		int cols = model.getInputColumnCount() + model.getOutputColumnCount();
		if (row < 0)
			row = 0;
		if (col < 0)
			col = 0;
		if (row >= rows)
			row = rows - 1;
		if (col >= cols)
			col = cols - 1;

		if (row == cursorRow && col == cursorCol
				&& (keepMark || (row == markRow && col == markCol))) {
			; // nothing is changing, so do nothing
		} else if (!keepMark && markRow == cursorRow && markCol == cursorCol) {
			int oldRow = cursorRow;
			int oldCol = cursorCol;
			cursorRow = row;
			cursorCol = col;
			markRow = row;
			markCol = col;
			expose(oldRow, oldCol);
			expose(cursorRow, cursorCol);
		} else {
			int r0 = Math.min(row, Math.min(cursorRow, markRow));
			int r1 = Math.max(row, Math.max(cursorRow, markRow));
			int c0 = Math.min(col, Math.min(cursorCol, markCol));
			int c1 = Math.max(col, Math.max(cursorCol, markCol));
			cursorRow = row;
			cursorCol = col;
			if (!keepMark) {
				markRow = row;
				markCol = col;
			}

			int x0 = table.getX(c0);
			int x1 = table.getX(c1) + table.getCellWidth();
			int y0 = table.getY(r0);
			int y1 = table.getY(r1) + table.getCellHeight();
			table.repaint(x0 - 2, y0 - 2, (x1 - x0) + 4, (y1 - y0) + 4);
		}
		int cx = table.getX(cursorCol);
		int cy = table.getY(cursorRow);
		int cw = table.getCellWidth();
		int ch = table.getCellHeight();
		if (cursorRow == 0) {
			ch += cy;
			cy = 0;
		}
		table.scrollRectToVisible(new Rectangle(cx, cy, cw, ch));
	}
}
