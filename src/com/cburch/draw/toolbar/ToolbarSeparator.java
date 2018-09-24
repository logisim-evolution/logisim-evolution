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

package com.cburch.draw.toolbar;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;

public class ToolbarSeparator implements ToolbarItem {
	private int size;

	public ToolbarSeparator(int size) {
		this.size = size;
	}

	public Dimension getDimension(Object orientation) {
		return new Dimension(size, size);
	}

	public String getToolTip() {
		return null;
	}

	public boolean isSelectable() {
		return false;
	}

	public void paintIcon(Component destination, Graphics g) {
		Dimension dim = destination.getSize();
		g.setColor(Color.GRAY);
		int x = 0;
		int y = 0;
		int w = dim.width;
		int h = dim.height;
		if (h >= w) { // separator is a vertical line in horizontal toolbar
			h -= 8;
			y = 2;
			x = (w - 2) / 2;
			w = 2;
		} else { // separator is a horizontal line in vertical toolbar
			w -= 8;
			x = 2;
			y = (h - 2) / 2;
			h = 2;
		}
		g.fillRect(x, y, w, h);
	}
}
