/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 * 
 * https://github.com/logisim-evolution/
 * 
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.circuit;

public class WireUtil {
  /*
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
  */
}
