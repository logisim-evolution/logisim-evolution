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
	private static final Font HEAD_FONT = new Font("Serif", Font.BOLD, 14);
	private static final Font BODY_FONT = new Font("Serif", Font.PLAIN, 14);
	private static final int COLUMN_SEP = 8;

	private static final int HEADER_SEP = 4;

	private MyListener myListener = new MyListener();
	private TruthTable table;
	private int cellWidth = 25; // reasonable start values
	private int cellHeight = 15;
	private int tableWidth;
	private int tableHeight;
	private int provisionalX;
	private int provisionalY;
	private Entry provisionalValue = null;
	private TableTabCaret caret;
	private TableTabClip clip;

	public TableTab(TruthTable table) {
		this.table = table;
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
			FontMetrics fm = g.getFontMetrics(HEAD_FONT);
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
		tableWidth = (cellWidth + COLUMN_SEP) * (inputs + outputs) - COLUMN_SEP;
		tableHeight = cellHeight * (1 + table.getRowCount()) + HEADER_SEP;
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
		int ret = (x + COLUMN_SEP / 2) / (cellWidth + COLUMN_SEP);
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
		if (y < cellHeight + HEADER_SEP)
			return -1;
		int ret = (y - cellHeight - HEADER_SEP) / cellHeight;
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
							* cellHeight + HEADER_SEP;
				} else {
					return curY > cellHeight + HEADER_SEP ? numCells
							* cellHeight : numCells * cellHeight + HEADER_SEP;
				}
			}

			@Override
			public int getUnitIncrement(int direction) {
				int curY = getValue();
				if (direction > 0) {
					return curY > 0 ? cellHeight : cellHeight + HEADER_SEP;
				} else {
					return curY > cellHeight + HEADER_SEP ? cellHeight
							: cellHeight + HEADER_SEP;
				}
			}
		};
	}

	int getX(int col) {
		Dimension sz = getSize();
		int left = Math.max(0, (sz.width - tableWidth) / 2);
		int inputs = table.getInputColumnCount();
		if (inputs == 0)
			left += cellWidth + COLUMN_SEP;
		return left + col * (cellWidth + COLUMN_SEP);
	}

	int getY(int row) {
		Dimension sz = getSize();
		int top = Math.max(0, (sz.height - tableHeight) / 2);
		return top + cellHeight + HEADER_SEP + row * cellHeight;
	}

	void localeChanged() {
		computePreferredSize();
		repaint();
	}

	@Override
	public void paintComponent(Graphics g) {
		/* Anti-aliasing changes from https://github.com/hausen/logisim-evolution */
		Graphics2D g2 = (Graphics2D)g;
		g2.setRenderingHint(
				RenderingHints.KEY_TEXT_ANTIALIASING,
				RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		g2.setRenderingHint(
				RenderingHints.KEY_ANTIALIASING,
				RenderingHints.VALUE_ANTIALIAS_ON);
		
		super.paintComponent(g);

		caret.paintBackground(g);

		Dimension sz = getSize();
		int top = Math.max(0, (sz.height - tableHeight) / 2);
		int left = Math.max(0, (sz.width - tableWidth) / 2);
		int inputs = table.getInputColumnCount();
		int outputs = table.getOutputColumnCount();
		if (inputs == 0 && outputs == 0) {
			g.setFont(BODY_FONT);
			GraphicsUtil.drawCenteredText(g, Strings.get("tableEmptyMessage"),
					sz.width / 2, sz.height / 2);
			return;
		}

		g.setColor(Color.GRAY);
		int lineX = left + (cellWidth + COLUMN_SEP) * inputs - COLUMN_SEP / 2;
		if (inputs == 0)
			lineX = left + cellWidth + COLUMN_SEP / 2;
		int lineY = top + cellHeight + HEADER_SEP / 2;
		g.drawLine(left, lineY, left + tableWidth, lineY);
		g.drawLine(lineX, top, lineX, top + tableHeight);

		g.setColor(Color.BLACK);
		g.setFont(HEAD_FONT);
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

		g.setFont(BODY_FONT);
		FontMetrics bodyMetric = g.getFontMetrics();
		y = top + cellHeight + HEADER_SEP;
		Rectangle clip = g.getClipBounds();
		int firstRow = Math.max(0, (clip.y - y) / cellHeight);
		int lastRow = Math.min(table.getRowCount(), 2
				+ (clip.y + clip.height - y) / cellHeight);
		y += firstRow * cellHeight;
		if (inputs == 0)
			left += cellWidth + COLUMN_SEP;
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
					g.setColor(Color.BLACK);
				}
				String label = entry.getDescription();
				int width = bodyMetric.stringWidth(label);
				if (provisional) {
					provisional = false;
					g.setColor(Color.GREEN);
					g.drawString(label, x + (cellWidth - width) / 2, y
							+ bodyMetric.getAscent());
					g.setColor(Color.BLACK);
				} else {
					g.drawString(label, x + (cellWidth - width) / 2, y
							+ bodyMetric.getAscent());
				}
				x += cellWidth + COLUMN_SEP;
			}
			y += cellHeight;
		}

		caret.paintForeground(g);
	}

	private int paintHeader(String header, int x, int y, Graphics g,
			FontMetrics fm) {
		int width = fm.stringWidth(header);
		g.drawString(header, x + (cellWidth - width) / 2, y);
		return x + cellWidth + COLUMN_SEP;
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

		int top = (getHeight() - tableHeight) / 2 + cellHeight + HEADER_SEP + y
				* cellHeight;
		repaint(0, top, getWidth(), cellHeight);
	}
}
