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

package com.cburch.logisim.gui.main;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Component;
import java.awt.Composite;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;

import javax.swing.Icon;

import com.cburch.draw.toolbar.ToolbarItem;
import com.cburch.logisim.gui.menu.LogisimMenuItem;
import com.cburch.logisim.prefs.AppPreferences;
import com.cburch.logisim.util.Icons;
import com.cburch.logisim.util.StringGetter;

class LogisimToolbarItem implements ToolbarItem {
	private MenuListener menu;
	private Icon icon;
	private LogisimMenuItem action;
	private StringGetter toolTip;

	public LogisimToolbarItem(MenuListener menu, String iconName,
			LogisimMenuItem action, StringGetter toolTip) {
		this.menu = menu;
		this.icon = Icons.getIcon(iconName);
		this.action = action;
		this.toolTip = toolTip;
	}

	public void doAction() {
		if (menu != null && menu.isEnabled(action)) {
			menu.doAction(action);
		}
	}

	public Dimension getDimension(Object orientation) {
		if (icon == null) {
			return new Dimension(AppPreferences.getScaled(AppPreferences.IconSize), 
					AppPreferences.getScaled(AppPreferences.IconSize));
		} else {
			int w = icon.getIconWidth();
			int h = icon.getIconHeight();
			return new Dimension(w, h + 2);
		}
	}

	public String getToolTip() {
		if (toolTip != null) {
			return toolTip.toString();
		} else {
			return null;
		}
	}

	public boolean isSelectable() {
		return menu != null && menu.isEnabled(action);
	}

	public void paintIcon(Component destination, Graphics g) {
		if (!isSelectable() && g instanceof Graphics2D) {
			Composite c = AlphaComposite.getInstance(AlphaComposite.SRC_OVER,
					0.3f);
			((Graphics2D) g).setComposite(c);
		}

		if (icon == null) {
			int simple = AppPreferences.getScaled(AppPreferences.IconSize)>>2;
			g.setColor(new Color(255, 128, 128));
			g.fillRect(simple, simple, 2*simple, 2*simple);
			g.setColor(Color.BLACK);
			g.drawLine(simple, simple, 3*simple, 3*simple);
			g.drawLine(simple, 3*simple, 3*simple, simple);
			g.drawRect(simple, simple, 2*simple, 2*simple);
		} else {
			icon.paintIcon(destination, g, 0, 1);
		}
	}

	public void setIcon(String iconName) {
		this.icon = Icons.getIcon(iconName);
	}

	public void setToolTip(StringGetter toolTip) {
		this.toolTip = toolTip;
	}
}
