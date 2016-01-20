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

package com.cburch.logisim.circuit;

import java.awt.Color;
import java.awt.Graphics;

import com.cburch.logisim.comp.AbstractComponentFactory;
import com.cburch.logisim.comp.Component;
import com.cburch.logisim.comp.ComponentDrawContext;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.util.GraphicsUtil;
import com.cburch.logisim.util.StringGetter;

class WireFactory extends AbstractComponentFactory {
	public static final WireFactory instance = new WireFactory();

	private WireFactory() {
	}

	@Override
	public AttributeSet createAttributeSet() {
		return Wire.create(Location.create(0, 0), Location.create(100, 0));
	}

	@Override
	public Component createComponent(Location loc, AttributeSet attrs) {
		Object dir = attrs.getValue(Wire.dir_attr);
		int len = attrs.getValue(Wire.len_attr).intValue();

		if (dir == Wire.VALUE_HORZ) {
			return Wire.create(loc, loc.translate(len, 0));
		} else {
			return Wire.create(loc, loc.translate(0, len));
		}
	}

	//
	// user interface methods
	//
	@Override
	public void drawGhost(ComponentDrawContext context, Color color, int x,
			int y, AttributeSet attrs) {
		Graphics g = context.getGraphics();
		Object dir = attrs.getValue(Wire.dir_attr);
		int len = attrs.getValue(Wire.len_attr).intValue();

		g.setColor(color);
		GraphicsUtil.switchToWidth(g, 3);
		if (dir == Wire.VALUE_HORZ) {
			g.drawLine(x, y, x + len, y);
		} else {
			g.drawLine(x, y, x, y + len);
		}
	}

	@Override
	public StringGetter getDisplayGetter() {
		return Strings.getter("wireComponent");
	}

	@Override
	public String getName() {
		return "Wire";
	}

	@Override
	public Bounds getOffsetBounds(AttributeSet attrs) {
		Object dir = attrs.getValue(Wire.dir_attr);
		int len = attrs.getValue(Wire.len_attr).intValue();

		if (dir == Wire.VALUE_HORZ) {
			return Bounds.create(0, -2, len, 5);
		} else {
			return Bounds.create(-2, 0, 5, len);
		}
	}
	
	@Override
	public boolean HDLSupportedComponent(String HDLIdentifier, AttributeSet attrs) {
		return true;
	}
}
