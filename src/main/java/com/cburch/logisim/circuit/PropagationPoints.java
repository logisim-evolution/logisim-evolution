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

import java.awt.Graphics;
import java.util.HashMap;
import java.util.HashSet;

import com.cburch.logisim.comp.Component;
import com.cburch.logisim.comp.ComponentDrawContext;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.util.GraphicsUtil;

class PropagationPoints {
	private static class Entry {
		private CircuitState state;
		private Location loc;

		private Entry(CircuitState state, Location loc) {
			this.state = state;
			this.loc = loc;
		}

		@Override
		public boolean equals(Object other) {
			if (!(other instanceof Entry))
				return false;
			Entry o = (Entry) other;
			return state.equals(o.state) && loc.equals(o.loc);
		}

		@Override
		public int hashCode() {
			return state.hashCode() * 31 + loc.hashCode();
		}
	}

	private HashSet<Entry> data;

	PropagationPoints() {
		this.data = new HashSet<Entry>();
	}

	void add(CircuitState state, Location loc) {
		data.add(new Entry(state, loc));
	}

	private void addSubstates(HashMap<CircuitState, CircuitState> map,
			CircuitState source, CircuitState value) {
		map.put(source, value);
		for (CircuitState s : source.getSubstates()) {
			addSubstates(map, s, value);
		}
	}

	void clear() {
		data.clear();
	}

	void draw(ComponentDrawContext context) {
		if (data.isEmpty())
			return;

		CircuitState state = context.getCircuitState();
		HashMap<CircuitState, CircuitState> stateMap = new HashMap<CircuitState, CircuitState>();
		for (CircuitState s : state.getSubstates()) {
			addSubstates(stateMap, s, s);
		}

		Graphics g = context.getGraphics();
		GraphicsUtil.switchToWidth(g, 2);
		for (Entry e : data) {
			if (e.state == state) {
				Location p = e.loc;
				g.drawOval(p.getX() - 4, p.getY() - 4, 8, 8);
			} else if (stateMap.containsKey(e.state)) {
				CircuitState substate = stateMap.get(e.state);
				Component subcirc = substate.getSubcircuit();
				Bounds b = subcirc.getBounds();
				g.drawRect(b.getX(), b.getY(), b.getWidth(), b.getHeight());
			}
		}
		GraphicsUtil.switchToWidth(g, 1);
	}

	boolean isEmpty() {
		return data.isEmpty();
	}
}
