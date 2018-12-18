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

package com.cburch.logisim.gui.log;

import java.awt.Color;
import java.awt.Graphics;

import javax.swing.Icon;

import com.cburch.logisim.comp.Component;
import com.cburch.logisim.comp.ComponentDrawContext;

class ComponentIcon implements Icon {
	public static final int TRIANGLE_NONE = 0;
	public static final int TRIANGLE_CLOSED = 1;
	public static final int TRIANGLE_OPEN = 2;

	private Component comp;
	private int triangleState = TRIANGLE_NONE;

	ComponentIcon(Component comp) {
		this.comp = comp;
	}

	public int getIconHeight() {
		return 20;
	}

	public int getIconWidth() {
		return 20;
	}

	public void paintIcon(java.awt.Component c, Graphics g, int x, int y) {
		// draw tool icon
		Graphics gIcon = g.create();
		ComponentDrawContext context = new ComponentDrawContext(c, null, null,
				g, gIcon);
		comp.getFactory().paintIcon(context, x, y, comp.getAttributeSet());
		gIcon.dispose();

		if (triangleState != TRIANGLE_NONE) {
			int[] xp;
			int[] yp;
			if (triangleState == TRIANGLE_CLOSED) {
				xp = new int[] { x + 13, x + 13, x + 17 };
				yp = new int[] { y + 11, y + 19, y + 15 };
			} else {
				xp = new int[] { x + 11, x + 19, x + 15 };
				yp = new int[] { y + 13, y + 13, y + 17 };
			}
			g.setColor(Color.LIGHT_GRAY);
			g.fillPolygon(xp, yp, 3);
			g.setColor(Color.DARK_GRAY);
			g.drawPolygon(xp, yp, 3);
		}
	}

	public void setTriangleState(int value) {
		triangleState = value;
	}
}
