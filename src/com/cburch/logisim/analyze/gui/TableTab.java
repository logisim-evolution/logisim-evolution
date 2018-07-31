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
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseEvent;

import javax.swing.JPanel;
import javax.swing.JScrollBar;

import com.cburch.logisim.analyze.model.Entry;
import com.cburch.logisim.analyze.model.TruthTable;
import com.cburch.logisim.analyze.model.TruthTableEvent;
import com.cburch.logisim.analyze.model.TruthTableListener;
import com.cburch.logisim.prefs.AppPreferences;
import com.cburch.logisim.util.GraphicsUtil;

class TableTab extends JPanel implements TruthTablePanel, TabInterface {
	private class MyListener implements TruthTableListener {
		public void cellsChanged(TruthTableEvent event) {
			repaint();
		}

		public void structureChanged(TruthTableEvent event) {
			computePreferredSize();
		}
	}

	private static final long serialVersionUID = 1L;

	private MyListener myListener = new MyListener();
	private TruthTable table;
	private int cellWidth;
	private int cellHeight;
	private int tableWidth;
	private int tableHeight;
	private int provisionalX;
	private int provisionalY;
	private Entry provisionalValue = null;
	private TableTabCaret caret;
	private TableTabClip clip;
	private Font HeaderFont;
	private Font EntryFont;
	private int ColSeperate;
	private int HeadSeperate;

	public TableTab(TruthTable table) {
		this.table = table;
		HeaderFont = AppPreferences.getScaledFont(getFont()).deriveFont(Font.BOLD);
		EntryFont = AppPreferences.getScaledFont(getFont());
		cellWidth = AppPreferences.getScaled(AppPreferences.IconSize+AppPreferences.IconBorder*2);
		cellHeight = AppPreferences.getScaled(AppPreferences.IconSize);
		ColSeperate = AppPreferences.getScaled(AppPreferences.IconSize>>1);
		HeadSeperate = AppPreferences.getScaled(AppPreferences.IconSize>>2);
		table.addTruthTableListener(myListener);
		setToolTipText(" ");
		caret = new TableTabCaret(this);
		clip = new TableTabClip(this);
	}

	private void computePreferredSize() {
		int inputs = table.getInputColumnCount();
		int outputs = table.getOutputColumnCount();
		if (inputs == 0 && outputs == 0) {
			setPreferredSize(new Dimension(0, 0));
			return;
		}

		Graphics g = getGraphics();
		if (g == null) {
			cellHeight = 16;
			cellWidth = 24;
		} else {
			FontMetrics fm = g.getFontMetrics(HeaderFont);
			cellHeight = fm.getHeight();
			cellWidth = 24;
			if (inputs == 0 || outputs == 0) {
				cellWidth = Math.max(cellWidth,
						fm.stringWidth(Strings.get("tableNullHeader")));
			}
			for (int i = 0; i < inputs + outputs; i++) {
				String header = i < inputs ? table.getInputHeader(i) : table
						.getOutputHeader(i - inputs);
				cellWidth = Math.max(cellWidth, fm.stringWidth(header));
			}
		}

		if (inputs == 0)
			inputs = 1;
		if (outputs == 0)
			outputs = 1;
		tableWidth = (cellWidth + ColSeperate) * (inputs + outputs) - ColSeperate;
		tableHeight = cellHeight * (1 + table.getRowCount()) + HeadSeperate;
		setPreferredSize(new Dimension(tableWidth, tableHeight));
		revalidate();
		repaint();
	}

	public void copy() {
		requestFocus();
		clip.copy();
	}

	public void delete() {
		requestFocus();
		int r0 = caret.getCursorRow();
		int r1 = caret.getMarkRow();
		int c0 = caret.getCursorCol();
		int c1 = caret.getMarkCol();
		if (r0 < 0 || r1 < 0)
			return;
		if (r1 < r0) {
			int t = r0;
			r0 = r1;
			r1 = t;
		}
		if (c1 < c0) {
			int t = c0;
			c0 = c1;
			c1 = t;
		}
		int inputs = table.getInputColumnCount();
		for (int c = c0; c <= c1; c++) {
			if (c >= inputs) {
				for (int r = r0; r <= r1; r++) {
					table.setOutputEntry(r, c - inputs, Entry.DONT_CARE);
				}
			}
		}
	}

	TableTabCaret getCaret() {
		return caret;
	}

	int getCellHeight() {
		return cellHeight;
	}

	int getCellWidth() {
		return cellWidth;
	}

	public int getColumn(MouseEvent event) {
		int x = event.getX() - (getWidth() - tableWidth) / 2;
		if (x < 0)
			return -1;
		int inputs = table.getInputColumnCount();
		int cols = inputs + table.getOutputColumnCount();
		int ret = (x + ColSeperate / 2) / (cellWidth + ColSeperate);
		if (inputs == 0)
			ret--;
		return ret >= 0 ? ret < cols ? ret : cols : -1;
	}

	int getColumnCount() {
		int inputs = table.getInputColumnCount();
		int outputs = table.getOutputColumnCount();
		return inputs + outputs;
	}

	public int getOutputColumn(MouseEvent event) {
		int inputs = table.getInputColumnCount();
		if (inputs == 0)
			inputs = 1;
		int ret = getColumn(event);
		return ret >= inputs ? ret - inputs : -1;
	}

	public int getRow(MouseEvent event) {
		int y = event.getY() - (getHeight() - tableHeight) / 2;
		if (y < cellHeight + HeadSeperate)
			return -1;
		int ret = (y - cellHeight - HeadSeperate) / cellHeight;
		int rows = table.getRowCount();
		return ret >= 0 ? ret < rows ? ret : rows : -1;
	}

	@Override
	public String getToolTipText(MouseEvent event) {
		int row = getRow(event);
		int col = getOutputColumn(event);
		Entry entry = table.getOutputEntry(row, col);
		return entry.getErrorMessage();
	}

	public TruthTable getTruthTable() {
		return table;
	}

	JScrollBar getVerticalScrollBar() {
		return new JScrollBar() {
			private static final long serialVersionUID = 1L;

			@Override
			public int getBlockIncrement(int direction) {
				int curY = getValue();
				int curHeight = getVisibleAmount();
				int numCells = curHeight / cellHeight - 1;
				if (numCells <= 0)
					numCells = 1;
				if (direction > 0) {
					return curY > 0 ? numCells * cellHeight : numCells
							* cellHeight + HeadSeperate;
				} else {
					return curY > cellHeight + HeadSeperate ? numCells
							* cellHeight : numCells * cellHeight + HeadSeperate;
				}
			}

			@Override
			public int getUnitIncrement(int direction) {
				int curY = getValue();
				if (direction > 0) {
					return curY > 0 ? cellHeight : cellHeight + HeadSeperate;
				} else {
					return curY > cellHeight + HeadSeperate ? cellHeight
							: cellHeight + HeadSeperate;
				}
			}
		};
	}

	int getX(int col) {
		Dimension sz = getSize();
		int left = Math.max(0, (sz.width - tableWidth) / 2);
		int inputs = table.getInputColumnCount();
		if (inputs == 0)
			left += cellWidth + ColSeperate;
		return left + col * (cellWidth + ColSeperate);
	}

	int getY(int row) {
		Dimension sz = getSize();
		int top = Math.max(0, (sz.height - tableHeight) / 2);
		return top + cellHeight + HeadSeperate + row * cellHeight;
	}

	void localeChanged() {
		computePreferredSize();
		repaint();
	}

	@Override
	public void paintComponent(Graphics g) {
		if (AppPreferences.AntiAliassing.getBoolean()) {
			Graphics2D g2 = (Graphics2D) g;
			g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		}
		
		super.paintComponent(g);

		caret.paintBackground(g);

		Dimension sz = getSize();
		int top = Math.max(0, (sz.height - tableHeight) / 2);
		int left = Math.max(0, (sz.width - tableWidth) / 2);
		int inputs = table.getInputColumnCount();
		int outputs = table.getOutputColumnCount();
		if (inputs == 0 && outputs == 0) {
			g.setFont(HeaderFont);
			GraphicsUtil.drawCenteredText(g, Strings.get("tableEmptyMessage"),
					sz.width / 2, sz.height / 2);
			return;
		}

		g.setColor(Color.GRAY);
		int lineX = left + (cellWidth + ColSeperate) * inputs - ColSeperate / 2;
		if (inputs == 0)
			lineX = left + cellWidth + ColSeperate / 2;
		int lineY = top + cellHeight + HeadSeperate / 2;
		g.drawLine(left, lineY, left + tableWidth, lineY);
		g.drawLine(left, lineY-1, left + tableWidth, lineY-1);
		g.drawLine(left, lineY+1, left + tableWidth, lineY+1);
		g.drawLine(lineX, top, lineX, top + tableHeight);
		g.drawLine(lineX-1, top, lineX-1, top + tableHeight);

		g.setColor(Color.BLACK);
		g.setFont(HeaderFont);
		FontMetrics headerMetric = g.getFontMetrics();
		int x = left;
		int y = top + headerMetric.getAscent() + 1;
		if (inputs == 0) {
			x = paintHeader(Strings.get("tableNullHeader"), x, y, g,
					headerMetric);
		} else {
			for (int i = 0; i < inputs; i++) {
				x = paintHeader(table.getInputHeader(i), x, y, g, headerMetric);
			}
		}
		if (outputs == 0) {
			x = paintHeader(Strings.get("tableNullHeader"), x, y, g,
					headerMetric);
		} else {
			for (int i = 0; i < outputs; i++) {
				x = paintHeader(table.getOutputHeader(i), x, y, g, headerMetric);
			}
		}

		g.setColor(Color.BLUE);
		g.setFont(EntryFont);
		FontMetrics bodyMetric = g.getFontMetrics();
		y = top + cellHeight + HeadSeperate;
		Rectangle clip = g.getClipBounds();
		int firstRow = Math.max(0, (clip.y - y) / cellHeight);
		int lastRow = Math.min(table.getRowCount(), 2
				+ (clip.y + clip.height - y) / cellHeight);
		y += firstRow * cellHeight;
		if (inputs == 0)
			left += cellWidth + ColSeperate;
		boolean provisional = false;
		for (int i = firstRow; i < lastRow; i++) {
			x = left;
			for (int j = 0; j < inputs + outputs; j++) {
				Entry entry = j < inputs ? table.getInputEntry(i, j) : table
						.getOutputEntry(i, j - inputs);
				if (provisionalValue != null && i == provisionalY
						&& j - inputs == provisionalX) {
					provisional = true;
					entry = provisionalValue;
				}
				if (entry.isError()) {
					g.setColor(ERROR_COLOR);
					g.fillRect(x, y, cellWidth, cellHeight);
					g.setColor(Color.BLUE);
				}
				String label = entry.getDescription();
				int width = bodyMetric.stringWidth(label);
				if (provisional) {
					provisional = false;
					g.setColor(Color.ORANGE);
					g.drawString(label, x + (cellWidth - width) / 2, y
							+ bodyMetric.getAscent());
					g.setColor(Color.BLUE);
				} else {
					g.drawString(label, x + (cellWidth - width) / 2, y
							+ bodyMetric.getAscent());
				}
				x += cellWidth + ColSeperate;
			}
			y += cellHeight;
		}

		caret.paintForeground(g);
	}

	private int paintHeader(String header, int x, int y, Graphics g,
			FontMetrics fm) {
		int width = fm.stringWidth(header);
		g.drawString(header, x + (cellWidth - width) / 2, y);
		return x + cellWidth + ColSeperate;
	}

	public void paste() {
		requestFocus();
		clip.paste();
	}

	public void selectAll() {
		caret.selectAll();
	}

	public void setEntryProvisional(int y, int x, Entry value) {
		provisionalY = y;
		provisionalX = x;
		provisionalValue = value;

		int top = (getHeight() - tableHeight) / 2 + cellHeight + HeadSeperate + y
				* cellHeight;
		repaint(0, top, getWidth(), cellHeight);
	}
}
