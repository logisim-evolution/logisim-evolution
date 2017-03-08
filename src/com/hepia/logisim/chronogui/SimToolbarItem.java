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
/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.hepia.logisim.chronogui;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Component;
import java.awt.Composite;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;

import javax.swing.Icon;

import com.cburch.draw.toolbar.ToolbarItem;
import com.cburch.logisim.prefs.AppPreferences;
import com.cburch.logisim.util.Icons;
import com.cburch.logisim.util.StringGetter;

/**
 *
 * @author saugyy
 */
public class SimToolbarItem implements ToolbarItem {
	private Icon icon;
	private StringGetter toolTip;

	public SimToolbarItem(String iconName, StringGetter toolTip) {
		this.icon = Icons.getIcon(iconName);
		this.toolTip = toolTip;
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
		return true;
	}

	public void paintIcon(Component destination, Graphics g) {
		if (!isSelectable() && g instanceof Graphics2D) {
			Composite c = AlphaComposite.getInstance(AlphaComposite.SRC_OVER,
					0.3f);
			((Graphics2D) g).setComposite(c);
		}

		if (icon == null) {
			g.setColor(new Color(255, 128, 128));
			g.fillRect(4, 4, 8, 8);
			g.setColor(Color.BLACK);
			g.drawLine(4, 4, 12, 12);
			g.drawLine(4, 12, 12, 4);
			g.drawRect(4, 4, 8, 8);
		} else {
			icon.paintIcon(destination, g, 0, 1);
		}
	}

}
