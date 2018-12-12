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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Set;

import com.cburch.logisim.comp.Component;
import com.cburch.logisim.comp.EndData;
import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Location;

class CircuitPoints {
	private static class LocationData {
		BitWidth width = BitWidth.UNKNOWN;
		ArrayList<Component> components = new ArrayList<Component>(4);
		ArrayList<EndData> ends = new ArrayList<EndData>(4);
		// these lists are parallel - ends corresponding to wires are null
	}

	private HashMap<Location, LocationData> map = new HashMap<Location, LocationData>();
	private HashMap<Location, WidthIncompatibilityData> incompatibilityData = new HashMap<Location, WidthIncompatibilityData>();

	public CircuitPoints() {
	}

	//
	// update methods
	//
	void add(Component comp) {
		if (comp instanceof Wire) {
			Wire w = (Wire) comp;
			addSub(w.getEnd0(), w, null);
			addSub(w.getEnd1(), w, null);
		} else {
			for (EndData endData : comp.getEnds()) {
				if (endData != null) {
					addSub(endData.getLocation(), comp, endData);
				}
			}
		}
	}

	void add(Component comp, EndData endData) {
		if (endData != null)
			addSub(endData.getLocation(), comp, endData);
	}

	private void addSub(Location loc, Component comp, EndData endData) {
		LocationData locData = map.get(loc);
		if (locData == null) {
			locData = new LocationData();
			map.put(loc, locData);
		}
		locData.components.add(comp);
		locData.ends.add(endData);
		computeIncompatibilityData(loc, locData);
	}

	private void computeIncompatibilityData(Location loc, LocationData locData) {
		WidthIncompatibilityData error = null;
		if (locData != null) {
			BitWidth width = BitWidth.UNKNOWN;
			for (EndData endData : locData.ends) {
				if (endData != null) {
					BitWidth endWidth = endData.getWidth();
					if (width == BitWidth.UNKNOWN) {
						width = endWidth;
					} else if (width != endWidth
							&& endWidth != BitWidth.UNKNOWN) {
						if (error == null) {
							error = new WidthIncompatibilityData();
							error.add(loc, width);
						}
						error.add(loc, endWidth);
					}
				}
			}
			locData.width = width;
		}

		if (error == null) {
			incompatibilityData.remove(loc);
		} else {
			incompatibilityData.put(loc, error);
		}
	}

	private Collection<? extends Component> find(Location loc, boolean isWire) {
		LocationData locData = map.get(loc);
		if (locData == null)
			return Collections.emptySet();

		// first see how many elements we have; we can handle some simple
		// cases without creating any new lists
		ArrayList<Component> list = locData.components;
		int retSize = 0;
		Component retValue = null;
		for (Component o : list) {
			if ((o instanceof Wire) == isWire) {
				retValue = o;
				retSize++;
			}
		}
		if (retSize == list.size())
			return list;
		if (retSize == 0)
			return Collections.emptySet();
		if (retSize == 1)
			return Collections.singleton(retValue);

		// otherwise we have to create our own list
		Component[] ret = new Component[retSize];
		int retPos = 0;
		for (Component o : list) {
			if ((o instanceof Wire) == isWire) {
				ret[retPos] = o;
				retPos++;
			}
		}
		return Arrays.asList(ret);
	}

	int getComponentCount(Location loc) {
		LocationData locData = map.get(loc);
		return locData == null ? 0 : locData.components.size();
	}

	Collection<? extends Component> getComponents(Location loc) {
		LocationData locData = map.get(loc);
		if (locData == null)
			return Collections.emptySet();
		else
			return locData.components;
	}

	Component getExclusive(Location loc) {
		LocationData locData = map.get(loc);
		if (locData == null)
			return null;
		int i = -1;
		for (EndData endData : locData.ends) {
			i++;
			if (endData != null && endData.isExclusive()) {
				return locData.components.get(i);
			}
		}
		return null;
	}

	Collection<? extends Component> getNonWires(Location loc) {
		return find(loc, false);
	}

	Collection<? extends Component> getSplitCauses(Location loc) {
		return getComponents(loc);
	}

	//
	// access methods
	//
	Set<Location> getSplitLocations() {
		return map.keySet();
	}

	BitWidth getWidth(Location loc) {
		LocationData locData = map.get(loc);
		return locData == null ? BitWidth.UNKNOWN : locData.width;
	}

	Collection<WidthIncompatibilityData> getWidthIncompatibilityData() {
		return incompatibilityData.values();
	}

	Collection<Wire> getWires(Location loc) {
		@SuppressWarnings("unchecked")
		Collection<Wire> ret = (Collection<Wire>) find(loc, true);
		return ret;
	}

	boolean hasConflict(Component comp) {
		if (comp instanceof Wire) {
			return false;
		} else {
			for (EndData endData : comp.getEnds()) {
				if (endData != null && endData.isExclusive()
						&& getExclusive(endData.getLocation()) != null) {
					return true;
				}
			}
			return false;
		}
	}

	void remove(Component comp) {
		if (comp instanceof Wire) {
			Wire w = (Wire) comp;
			removeSub(w.getEnd0(), w);
			removeSub(w.getEnd1(), w);
		} else {
			for (EndData endData : comp.getEnds()) {
				if (endData != null) {
					removeSub(endData.getLocation(), comp);
				}
			}
		}
	}

	void remove(Component comp, EndData endData) {
		if (endData != null)
			removeSub(endData.getLocation(), comp);
	}

	private void removeSub(Location loc, Component comp) {
		LocationData locData = map.get(loc);
		if (locData == null)
			return;

		int index = locData.components.indexOf(comp);
		if (index < 0)
			return;

		if (locData.components.size() == 1) {
			map.remove(loc);
			incompatibilityData.remove(loc);
		} else {
			locData.components.remove(index);
			locData.ends.remove(index);
			computeIncompatibilityData(loc, locData);
		}
	}

}
