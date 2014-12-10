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

package com.cburch.hex;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;

class Measures {
	private HexEditor hex;
	private int headerChars;
	private int cellChars;
	private int headerWidth;
	private int spacerWidth;
	private int cellWidth;
	private int cellHeight;
	private int cols;
	private int baseX;
	private boolean guessed;

	public Measures(HexEditor hex) {
		this.hex = hex;
		this.guessed = true;
		this.cols = 1;
		this.cellWidth = -1;
		this.cellHeight = -1;
		this.cellChars = 2;
		this.headerChars = 4;

		computeCellSize(null);
	}

	private void computeCellSize(Graphics g) {
		HexModel model = hex.getModel();

		// compute number of characters in headers and cells
		if (model == null) {
			headerChars = 4;
			cellChars = 2;
		} else {
			int logSize = 0;
			long addrEnd = model.getLastOffset();
			while (addrEnd > (1L << logSize)) {
				logSize++;
			}
			headerChars = (logSize + 3) / 4;
			cellChars = (model.getValueWidth() + 3) / 4;
		}

		// compute character sizes
		FontMetrics fm = g == null ? null : g.getFontMetrics(hex.getFont());
		int charWidth;
		int spaceWidth;
		int lineHeight;
		if (fm == null) {
			charWidth = 8;
			spaceWidth = 6;
			Font font = hex.getFont();
			if (font == null) {
				lineHeight = 16;
			} else {
				lineHeight = font.getSize();
			}
		} else {
			guessed = false;
			charWidth = 0;
			for (int i = 0; i < 16; i++) {
				int width = fm.stringWidth(Integer.toHexString(i));
				if (width > charWidth)
					charWidth = width;
			}
			spaceWidth = fm.stringWidth(" ");
			lineHeight = fm.getHeight();
		}

		// update header and cell dimensions
		headerWidth = headerChars * charWidth + spaceWidth;
		spacerWidth = spaceWidth;
		cellWidth = cellChars * charWidth + spaceWidth;
		cellHeight = lineHeight;

		// compute preferred size
		int width = headerWidth + cols * cellWidth + (cols / 4) * spacerWidth;
		long height;
		if (model == null) {
			height = 16 * cellHeight;
		} else {
			long addr0 = getBaseAddress(model);
			long addr1 = model.getLastOffset();
			long rows = (int) (((addr1 - addr0 + 1) + cols - 1) / cols);
			height = rows * cellHeight;
			if (height > Integer.MAX_VALUE)
				height = Integer.MAX_VALUE;
		}

		// update preferred size
		Dimension pref = hex.getPreferredSize();
		if (pref.width != width || pref.height != height) {
			pref.width = width;
			pref.height = (int) height;
			hex.setPreferredSize(pref);
			hex.revalidate();
		}

		widthChanged();
	}

	void ensureComputed(Graphics g) {
		if (guessed || cellWidth < 0)
			computeCellSize(g);
	}

	public long getBaseAddress(HexModel model) {
		if (model == null) {
			return 0;
		} else {
			long addr0 = model.getFirstOffset();
			return addr0 - addr0 % cols;
		}
	}

	public int getBaseX() {
		return baseX;
	}

	public int getCellChars() {
		return cellChars;
	}

	public int getCellHeight() {
		return cellHeight;
	}

	public int getCellWidth() {
		return cellWidth;
	}

	public int getColumnCount() {
		return cols;
	}

	public int getLabelChars() {
		return headerChars;
	}

	public int getLabelWidth() {
		return headerWidth;
	}

	public int getValuesWidth() {
		return ((cols - 1) / 4) * spacerWidth + cols * cellWidth;
	}

	public int getValuesX() {
		return baseX + spacerWidth;
	}

	void recompute() {
		computeCellSize(hex.getGraphics());
	}

	public long toAddress(int x, int y) {
		HexModel model = hex.getModel();
		if (model == null)
			return Integer.MIN_VALUE;
		long addr0 = model.getFirstOffset();
		long addr1 = model.getLastOffset();

		long base = getBaseAddress(model) + ((long) y / cellHeight) * cols;
		int offs = (x - baseX) / (cellWidth + (spacerWidth + 2) / 4);
		if (offs < 0)
			offs = 0;
		if (offs >= cols)
			offs = cols - 1;

		long ret = base + offs;
		if (ret > addr1)
			ret = addr1;
		if (ret < addr0)
			ret = addr0;
		return ret;
	}

	public int toX(long addr) {
		int col = (int) (addr % cols);
		return baseX + (1 + (col / 4)) * spacerWidth + col * cellWidth;
	}

	public int toY(long addr) {
		long row = (addr - getBaseAddress(hex.getModel())) / cols;
		long ret = row * cellHeight;
		return ret < Integer.MAX_VALUE ? (int) ret : Integer.MAX_VALUE;
	}

	void widthChanged() {
		int oldCols = cols;
		int width;
		if (guessed || cellWidth < 0) {
			cols = 16;
			width = hex.getPreferredSize().width;
		} else {
			width = hex.getWidth();
			int ret = (width - headerWidth)
					/ (cellWidth + (spacerWidth + 3) / 4);
			if (ret >= 16)
				cols = 16;
			else if (ret >= 8)
				cols = 8;
			else
				cols = 4;
		}
		int lineWidth = headerWidth + cols * cellWidth + ((cols / 4) - 1)
				* spacerWidth;
		int newBase = headerWidth + Math.max(0, (width - lineWidth) / 2);
		if (baseX != newBase) {
			baseX = newBase;
			hex.repaint();
		}
		if (cols != oldCols)
			recompute();
	}
}
