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

package com.cburch.logisim.tools;

import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.Set;

import com.cburch.logisim.LogisimVersion;
import com.cburch.logisim.comp.Component;
import com.cburch.logisim.comp.ComponentDrawContext;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeDefaultProvider;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.gui.main.Canvas;

//
// DRAWING TOOLS
//
public abstract class Tool implements AttributeDefaultProvider {
	private static Cursor dflt_cursor = Cursor
			.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR);

	public Tool cloneTool() {
		return this;
	}

	public void deselect(Canvas canvas) {
	}

	public void draw(Canvas canvas, ComponentDrawContext context) {
		draw(context);
	}

	// This was the draw method until 2.0.4 - As of 2.0.5, you should
	// use the other draw method.
	public void draw(ComponentDrawContext context) {
	}

	public AttributeSet getAttributeSet() {
		return null;
	}

	public AttributeSet getAttributeSet(Canvas canvas) {
		return getAttributeSet();
	}

	public Cursor getCursor() {
		return dflt_cursor;
	}

	public Object getDefaultAttributeValue(Attribute<?> attr, LogisimVersion ver) {
		return null;
	}

	public abstract String getDescription();

	public abstract String getDisplayName();

	public Set<Component> getHiddenComponents(Canvas canvas) {
		return null;
	}

	public abstract String getName();

	public boolean isAllDefaultValues(AttributeSet attrs, LogisimVersion ver) {
		return false;
	}

	public void keyPressed(Canvas canvas, KeyEvent e) {
	}

	public void keyReleased(Canvas canvas, KeyEvent e) {
	}

	public void keyTyped(Canvas canvas, KeyEvent e) {
	}

	public void mouseDragged(Canvas canvas, Graphics g, MouseEvent e) {
	}

	public void mouseEntered(Canvas canvas, Graphics g, MouseEvent e) {
	}

	public void mouseExited(Canvas canvas, Graphics g, MouseEvent e) {
	}

	public void mouseMoved(Canvas canvas, Graphics g, MouseEvent e) {
	}

	public void mousePressed(Canvas canvas, Graphics g, MouseEvent e) {
	}

	public void mouseReleased(Canvas canvas, Graphics g, MouseEvent e) {
	}

	public void paintIcon(ComponentDrawContext c, int x, int y) {
	}

	public void select(Canvas canvas) {
	}

	public void setAttributeSet(AttributeSet attrs) {
	}

	public boolean sharesSource(Tool other) {
		return this == other;
	}

	@Override
	public String toString() {
		return getName();
	}

}
