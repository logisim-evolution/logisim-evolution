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

import java.awt.Color;
import java.awt.Graphics;
import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.cburch.draw.model.CanvasObject;
import com.cburch.draw.model.Handle;
import com.cburch.draw.model.HandleGesture;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.instance.Instance;
import com.cburch.logisim.std.wiring.Pin;
import com.cburch.logisim.util.UnmodifiableList;

public class AppearancePort extends AppearanceElement {
	private static final int INPUT_RADIUS = 4;
	private static final int OUTPUT_RADIUS = 5;
	private static final int MINOR_RADIUS = 2;
	public static final Color COLOR = Color.BLUE;

	private Instance pin;

	public AppearancePort(Location location, Instance pin) {
		super(location);
		this.pin = pin;
	}

	@Override
	public boolean contains(Location loc, boolean assumeFilled) {
		if (isInput()) {
			return getBounds().contains(loc);
		} else {
			return super.isInCircle(loc, OUTPUT_RADIUS);
		}
	}

	@Override
	public Bounds getBounds() {
		int r = isInput() ? INPUT_RADIUS : OUTPUT_RADIUS;
		return super.getBounds(r);
	}

	@Override
	public String getDisplayName() {
		return Strings.get("circuitPort");
	}

	@Override
	public List<Handle> getHandles(HandleGesture gesture) {
		Location loc = getLocation();

		int r = isInput() ? INPUT_RADIUS : OUTPUT_RADIUS;
		return UnmodifiableList.create(new Handle[] {
				new Handle(this, loc.translate(-r, -r)),
				new Handle(this, loc.translate(r, -r)),
				new Handle(this, loc.translate(r, r)),
				new Handle(this, loc.translate(-r, r)) });
	}

	public Instance getPin() {
		return pin;
	}

	private boolean isInput() {
		Instance p = pin;
		return p == null || Pin.FACTORY.isInputPin(p);
	}

	@Override
	public boolean matches(CanvasObject other) {
		if (other instanceof AppearancePort) {
			AppearancePort that = (AppearancePort) other;
			return this.matches(that) && this.pin == that.pin;
		} else {
			return false;
		}
	}

	@Override
	public int matchesHashCode() {
		return super.matchesHashCode() + pin.hashCode();
	}

	@Override
	public void paint(Graphics g, HandleGesture gesture) {
		Location location = getLocation();
		int x = location.getX();
		int y = location.getY();
		g.setColor(COLOR);
		if (isInput()) {
			int r = INPUT_RADIUS;
			g.drawRect(x - r, y - r, 2 * r, 2 * r);
		} else {
			int r = OUTPUT_RADIUS;
			g.drawOval(x - r, y - r, 2 * r, 2 * r);
		}
		g.fillOval(x - MINOR_RADIUS, y - MINOR_RADIUS, 2 * MINOR_RADIUS,
				2 * MINOR_RADIUS);
	}

	void setPin(Instance value) {
		pin = value;
	}

	@Override
	public Element toSvgElement(Document doc) {
		Location loc = getLocation();
		Location pinLoc = pin.getLocation();
		Element ret = doc.createElement("circ-port");
		int r = isInput() ? INPUT_RADIUS : OUTPUT_RADIUS;
		ret.setAttribute("x", "" + (loc.getX() - r));
		ret.setAttribute("y", "" + (loc.getY() - r));
		ret.setAttribute("width", "" + 2 * r);
		ret.setAttribute("height", "" + 2 * r);
		ret.setAttribute("pin", "" + pinLoc.getX() + "," + pinLoc.getY());
		return ret;
	}
}
