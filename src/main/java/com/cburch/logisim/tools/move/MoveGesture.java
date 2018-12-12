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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.circuit.Wire;
import com.cburch.logisim.comp.Component;
import com.cburch.logisim.comp.EndData;
import com.cburch.logisim.data.Direction;
import com.cburch.logisim.data.Location;

public class MoveGesture {
	private static Set<ConnectionData> computeConnections(Circuit circuit,
			Set<Component> selected) {
		if (selected == null || selected.isEmpty())
			return Collections.emptySet();

		// first identify locations that might be connected
		Set<Location> locs = new HashSet<Location>();
		for (Component comp : selected) {
			for (EndData end : comp.getEnds()) {
				locs.add(end.getLocation());
			}
		}

		// now see which of them require connection
		Set<ConnectionData> conns = new HashSet<ConnectionData>();
		for (Location loc : locs) {
			boolean found = false;
			for (Component comp : circuit.getComponents(loc)) {
				if (!selected.contains(comp)) {
					found = true;
					break;
				}
			}
			if (found) {
				List<Wire> wirePath;
				Location wirePathStart;
				Wire lastOnPath = findWire(circuit, loc, selected, null);
				if (lastOnPath == null) {
					wirePath = Collections.emptyList();
					wirePathStart = loc;
				} else {
					wirePath = new ArrayList<Wire>();
					Location cur = loc;
					for (Wire w = lastOnPath; w != null; w = findWire(circuit,
							cur, selected, w)) {
						wirePath.add(w);
						cur = w.getOtherEnd(cur);
					}
					Collections.reverse(wirePath);
					wirePathStart = cur;
				}

				Direction dir = null;
				if (lastOnPath != null) {
					Location other = lastOnPath.getOtherEnd(loc);
					int dx = loc.getX() - other.getX();
					int dy = loc.getY() - other.getY();
					if (Math.abs(dx) > Math.abs(dy)) {
						dir = dx > 0 ? Direction.EAST : Direction.WEST;
					} else {
						dir = dy > 0 ? Direction.SOUTH : Direction.NORTH;
					}
				}
				conns.add(new ConnectionData(loc, dir, wirePath, wirePathStart));
			}
		}
		return conns;
	}

	private static Wire findWire(Circuit circ, Location loc,
			Set<Component> ignore, Wire ignoreW) {
		Wire ret = null;
		for (Component comp : circ.getComponents(loc)) {
			if (!ignore.contains(comp) && comp != ignoreW) {
				if (ret == null && comp instanceof Wire) {
					ret = (Wire) comp;
				} else {
					return null;
				}
			}
		}
		return ret;
	}

	private MoveRequestListener listener;

	private Circuit circuit;
	private HashSet<Component> selected;
	private transient Set<ConnectionData> connections;

	private transient AvoidanceMap initAvoid;

	private HashMap<MoveRequest, MoveResult> cachedResults;

	public MoveGesture(MoveRequestListener listener, Circuit circuit,
			Collection<Component> selected) {
		this.listener = listener;
		this.circuit = circuit;
		this.selected = new HashSet<Component>(selected);
		this.connections = null;
		this.initAvoid = null;
		this.cachedResults = new HashMap<MoveRequest, MoveResult>();
	}

	public boolean enqueueRequest(int dx, int dy) {
		MoveRequest request = new MoveRequest(this, dx, dy);
		synchronized (cachedResults) {
			Object result = cachedResults.get(request);
			if (result == null) {
				ConnectorThread.enqueueRequest(request, false);
				return true;
			} else {
				return false;
			}
		}
	}

	public MoveResult findResult(int dx, int dy) {
		MoveRequest request = new MoveRequest(this, dx, dy);
		synchronized (cachedResults) {
			return cachedResults.get(request);
		}
	}

	public MoveResult forceRequest(int dx, int dy) {
		MoveRequest request = new MoveRequest(this, dx, dy);
		ConnectorThread.enqueueRequest(request, true);
		synchronized (cachedResults) {
			Object result = cachedResults.get(request);
			while (result == null) {
				try {
					cachedResults.wait();
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					return null;
				}
				result = cachedResults.get(request);
			}
			return (MoveResult) result;
		}
	}

	Set<ConnectionData> getConnections() {
		Set<ConnectionData> ret = connections;
		if (ret == null) {
			ret = computeConnections(circuit, selected);
			connections = ret;
		}
		return ret;
	}

	AvoidanceMap getFixedAvoidanceMap() {
		AvoidanceMap ret = initAvoid;
		if (ret == null) {
			HashSet<Component> comps = new HashSet<Component>(
					circuit.getNonWires());
			comps.addAll(circuit.getWires());
			comps.removeAll(selected);
			ret = AvoidanceMap.create(comps, 0, 0);
			initAvoid = ret;
		}
		return ret;
	}

	HashSet<Component> getSelected() {
		return selected;
	}

	void notifyResult(MoveRequest request, MoveResult result) {
		synchronized (cachedResults) {
			cachedResults.put(request, result);
			cachedResults.notifyAll();
		}
		if (listener != null) {
			listener.requestSatisfied(this, request.getDeltaX(),
					request.getDeltaY());
		}
	}

}
