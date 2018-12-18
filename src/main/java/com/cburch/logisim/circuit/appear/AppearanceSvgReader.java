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

package com.cburch.logisim.circuit.appear;

import java.util.Map;

import org.w3c.dom.Element;

import com.cburch.draw.model.AbstractCanvasObject;
import com.cburch.draw.shapes.SvgReader;
import com.cburch.logisim.data.Direction;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.instance.Instance;

public class AppearanceSvgReader {
	public static AbstractCanvasObject createShape(Element elt,
			Map<Location, Instance> pins) {
		String name = elt.getTagName();
		if (name.equals("circ-anchor") || name.equals("circ-origin")) {
			Location loc = getLocation(elt);
			AbstractCanvasObject ret = new AppearanceAnchor(loc);
			if (elt.hasAttribute("facing")) {
				Direction facing = Direction.parse(elt.getAttribute("facing"));
				ret.setValue(AppearanceAnchor.FACING, facing);
			}
			return ret;
		} else if (name.equals("circ-port")) {
			Location loc = getLocation(elt);
			String[] pinStr = elt.getAttribute("pin").split(",");
			Location pinLoc = Location.create(
					Integer.parseInt(pinStr[0].trim()),
					Integer.parseInt(pinStr[1].trim()));
			Instance pin = pins.get(pinLoc);
			if (pin == null) {
				return null;
			} else {
				return new AppearancePort(loc, pin);
			}
		} else {
			return SvgReader.createShape(elt);
		}
	}

	private static Location getLocation(Element elt) {
		double x = Double.parseDouble(elt.getAttribute("x"));
		double y = Double.parseDouble(elt.getAttribute("y"));
		double w = Double.parseDouble(elt.getAttribute("width"));
		double h = Double.parseDouble(elt.getAttribute("height"));
		int px = (int) Math.round(x + w / 2);
		int py = (int) Math.round(y + h / 2);
		return Location.create(px, py);
	}
}
