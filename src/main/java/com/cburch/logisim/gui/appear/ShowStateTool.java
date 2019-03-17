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

package com.cburch.logisim.gui.appear;

import java.awt.Graphics;
import java.awt.Dimension;
import java.awt.Point;
import javax.swing.Icon;

import com.cburch.draw.tools.DrawingAttributeSet;
import com.cburch.draw.toolbar.ToolbarClickableItem;
import com.cburch.logisim.util.Icons;

public class ShowStateTool implements ToolbarClickableItem {

	private AppearanceView view;
	private AppearanceCanvas canvas;
	private DrawingAttributeSet attrs;
	private Icon icon, pressed;

	public ShowStateTool(AppearanceView view, AppearanceCanvas canvas, DrawingAttributeSet attrs) {
		this.view = view;
		this.canvas = canvas;
		this.attrs = attrs;
		icon = Icons.getIcon("showstate.gif");
		pressed = Icons.getIcon("showstate_pressed.gif");
	}

	public Dimension getDimension(Object orientation) {
		return new Dimension(icon.getIconWidth() + 8, icon.getIconHeight() + 8);
	}

	public String getToolTip() {
		return "Select state to be shown";
	}

	public boolean isSelectable() {
		return false;
	}

	public void clicked() {
		ShowStateDialog w = new ShowStateDialog(view.getFrame(), canvas);
		Point p = view.getFrame().getLocation();
		p.translate(80, 50);
		w.setLocation(p);
		w.setVisible(true);
	}

	public void paintIcon(java.awt.Component destination, Graphics g) {
		icon.paintIcon(destination, g, 4, 4);
	}

	public void paintPressedIcon(java.awt.Component destination, Graphics g) {
		pressed.paintIcon(destination, g, 4, 4);
	}

}