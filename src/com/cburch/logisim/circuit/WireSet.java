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

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import com.cburch.logisim.data.Location;

public class WireSet {
	private static final Set<Wire> NULL_WIRES = Collections.emptySet();
	public static final WireSet EMPTY = new WireSet(NULL_WIRES);

	private Set<Wire> wires;
	private Set<Location> points;

	WireSet(Set<Wire> wires) {
		if (wires.isEmpty()) {
			this.wires = NULL_WIRES;
			points = Collections.emptySet();
		} else {
			this.wires = wires;
			points = new HashSet<Location>();
			for (Wire w : wires) {
				points.add(w.e0);
				points.add(w.e1);
			}
		}
	}

	public boolean containsLocation(Location loc) {
		return points.contains(loc);
	}

	public boolean containsWire(Wire w) {
		return wires.contains(w);
	}
}
