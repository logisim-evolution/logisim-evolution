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

package com.cburch.logisim.tools.move;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Set;

import com.cburch.logisim.circuit.Wire;
import com.cburch.logisim.comp.Component;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Location;

class AvoidanceMap {
	static AvoidanceMap create(Collection<Component> elements, int dx, int dy) {
		AvoidanceMap ret = new AvoidanceMap(new HashMap<Location, String>());
		ret.markAll(elements, dx, dy);
		return ret;
	}

	private final HashMap<Location, String> avoid;

	private AvoidanceMap(HashMap<Location, String> map) {
		avoid = map;
	}

	public AvoidanceMap cloneMap() {
		return new AvoidanceMap(new HashMap<Location, String>(avoid));
	}

	public Object get(Location loc) {
		return avoid.get(loc);
	}

	public void markAll(Collection<Component> elements, int dx, int dy) {
		// first we go through the components, saying that we should not
		// intersect with any point that lies within a component
		for (Component el : elements) {
			if (el instanceof Wire) {
				markWire((Wire) el, dx, dy);
			} else {
				markComponent(el, dx, dy);
			}
		}
	}

	public void markComponent(Component comp, int dx, int dy) {
		HashMap<Location, String> avoid = this.avoid;
		boolean translated = dx != 0 || dy != 0;
		Bounds bds = comp.getBounds();
		int x0 = bds.getX() + dx;
		int y0 = bds.getY() + dy;
		int x1 = x0 + bds.getWidth();
		int y1 = y0 + bds.getHeight();
		x0 += 9 - (x0 + 9) % 10;
		y0 += 9 - (y0 + 9) % 10;
		for (int x = x0; x <= x1; x += 10) {
			for (int y = y0; y <= y1; y += 10) {
				Location loc = Location.create(x, y);
				// loc is most likely in the component, so go ahead and
				// put it into the map as if it is - and in the rare event
				// that loc isn't in the component, we can remove it.
				String prev = avoid.put(loc, Connector.ALLOW_NEITHER);
				if (prev != Connector.ALLOW_NEITHER) {
					Location baseLoc = translated ? loc.translate(-dx, -dy)
							: loc;
					if (!comp.contains(baseLoc)) {
						if (prev == null) {
							avoid.remove(loc);
						} else {
							avoid.put(loc, prev);
						}
					}
				}
			}
		}
	}

	public void markWire(Wire w, int dx, int dy) {
		HashMap<Location, String> avoid = this.avoid;
		boolean translated = dx != 0 || dy != 0;
		Location loc0 = w.getEnd0();
		Location loc1 = w.getEnd1();
		if (translated) {
			loc0 = loc0.translate(dx, dy);
			loc1 = loc1.translate(dx, dy);
		}
		avoid.put(loc0, Connector.ALLOW_NEITHER);
		avoid.put(loc1, Connector.ALLOW_NEITHER);
		int x0 = loc0.getX();
		int y0 = loc0.getY();
		int x1 = loc1.getX();
		int y1 = loc1.getY();
		if (x0 == x1) { // vertical wire
			for (Location loc : Wire.create(loc0, loc1)) {
				Object prev = avoid.put(loc, Connector.ALLOW_HORIZONTAL);
				if (prev == Connector.ALLOW_NEITHER
						|| prev == Connector.ALLOW_VERTICAL) {
					avoid.put(loc, Connector.ALLOW_NEITHER);
				}
			}
		} else if (y0 == y1) { // horizontal wire
			for (Location loc : Wire.create(loc0, loc1)) {
				Object prev = avoid.put(loc, Connector.ALLOW_VERTICAL);
				if (prev == Connector.ALLOW_NEITHER
						|| prev == Connector.ALLOW_HORIZONTAL) {
					avoid.put(loc, Connector.ALLOW_NEITHER);
				}
			}
		} else { // diagonal - shouldn't happen
			throw new RuntimeException("diagonal wires not supported");
		}
	}

	public void print(PrintStream stream) {
		ArrayList<Location> list = new ArrayList<Location>(avoid.keySet());
		Collections.sort(list);
		for (int i = 0, n = list.size(); i < n; i++) {
			stream.println(list.get(i) + ": " + avoid.get(list.get(i)));
		}
	}

	public void unmarkLocation(Location loc) {
		avoid.remove(loc);
	}

	public void unmarkWire(Wire w, Location deletedEnd, Set<Location> unmarkable) {
		Location loc0 = w.getEnd0();
		Location loc1 = w.getEnd1();
		if (unmarkable == null || unmarkable.contains(deletedEnd)) {
			avoid.remove(deletedEnd);
		}
		int x0 = loc0.getX();
		int y0 = loc0.getY();
		int x1 = loc1.getX();
		int y1 = loc1.getY();
		if (x0 == x1) { // vertical wire
			for (Location loc : w) {
				if (unmarkable == null || unmarkable.contains(deletedEnd)) {
					Object prev = avoid.remove(loc);
					if (prev != Connector.ALLOW_HORIZONTAL && prev != null) {
						avoid.put(loc, Connector.ALLOW_VERTICAL);
					}
				}
			}
		} else if (y0 == y1) { // horizontal wire
			for (Location loc : w) {
				if (unmarkable == null || unmarkable.contains(deletedEnd)) {
					Object prev = avoid.remove(loc);
					if (prev != Connector.ALLOW_VERTICAL && prev != null) {
						avoid.put(loc, Connector.ALLOW_HORIZONTAL);
					}
				}
			}
		} else { // diagonal - shouldn't happen
			throw new RuntimeException("diagonal wires not supported");
		}
	}
}
