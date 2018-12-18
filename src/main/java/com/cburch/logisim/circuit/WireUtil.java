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

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;

import com.cburch.logisim.comp.Component;
import com.cburch.logisim.data.Location;

public class WireUtil {
	static CircuitPoints computeCircuitPoints(
			Collection<? extends Component> components) {
		CircuitPoints points = new CircuitPoints();
		for (Component comp : components) {
			points.add(comp);
		}
		return points;
	}

	// Merge all parallel endpoint-to-endpoint wires within the given set.
	public static Collection<? extends Component> mergeExclusive(
			Collection<? extends Component> toMerge) {
		if (toMerge.size() <= 1)
			return toMerge;

		HashSet<Component> ret = new HashSet<Component>(toMerge);
		CircuitPoints points = computeCircuitPoints(toMerge);

		HashSet<Wire> wires = new HashSet<Wire>();
		for (Location loc : points.getSplitLocations()) {
			Collection<? extends Component> at = points.getComponents(loc);
			if (at.size() == 2) {
				Iterator<? extends Component> atIt = at.iterator();
				Component o0 = atIt.next();
				Component o1 = atIt.next();
				if (o0 instanceof Wire && o1 instanceof Wire) {
					Wire w0 = (Wire) o0;
					Wire w1 = (Wire) o1;
					if (w0.is_x_equal == w1.is_x_equal) {
						wires.add(w0);
						wires.add(w1);
					}
				}
			}
		}
		points = null;

		ret.removeAll(wires);
		while (!wires.isEmpty()) {
			Iterator<Wire> it = wires.iterator();
			Wire w = it.next();
			Location e0 = w.e0;
			Location e1 = w.e1;
			it.remove();
			boolean found;
			do {
				found = false;
				for (it = wires.iterator(); it.hasNext();) {
					Wire cand = it.next();
					if (cand.e0.equals(e1)) {
						e1 = cand.e1;
						found = true;
						it.remove();
					} else if (cand.e1.equals(e0)) {
						e0 = cand.e0;
						found = true;
						it.remove();
					}
				}
			} while (found);
			ret.add(Wire.create(e0, e1));
		}

		return ret;
	}

	private WireUtil() {
	}
}
