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
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import com.cburch.logisim.comp.Component;
import com.cburch.logisim.data.Location;

class WireRepair extends CircuitTransaction {

	private static class MergeSets {
		private final HashMap<Wire, ArrayList<Wire>> map;

		MergeSets() {
			map = new HashMap<Wire, ArrayList<Wire>>();
		}

		Collection<ArrayList<Wire>> getMergeSets() {
			IdentityHashMap<ArrayList<Wire>, Boolean> lists;
			lists = new IdentityHashMap<ArrayList<Wire>, Boolean>();
			for (ArrayList<Wire> list : map.values()) {
				lists.put(list, Boolean.TRUE);
			}
			return lists.keySet();
		}

		void merge(Wire a, Wire b) {
			ArrayList<Wire> set0 = map.get(a);
			ArrayList<Wire> set1 = map.get(b);
			if (set0 == null && set1 == null) {
				set0 = new ArrayList<Wire>(2);
				set0.add(a);
				set0.add(b);
				map.put(a, set0);
				map.put(b, set0);
			} else if (set0 == null && set1 != null) {
				set1.add(a);
				map.put(a, set1);
			} else if (set0 != null && set1 == null) {
				set0.add(b);
				map.put(b, set0);
			} else if (set0 != set1) { // neither is null, and they are
										// different
				if (set0.size() > set1.size()) { // ensure set1 is the larger
					ArrayList<Wire> temp = set0;
					set0 = set1;
					set1 = temp;
				}
				set1.addAll(set0);
				for (Wire w : set0) {
					map.put(w, set1);
				}
			}
		}
	}

	private Circuit circuit;

	public WireRepair(Circuit circuit) {
		this.circuit = circuit;
	}

	private void doMerges(CircuitMutator mutator) {
		MergeSets sets = new MergeSets();
		for (Location loc : circuit.wires.points.getSplitLocations()) {
			Collection<?> at = circuit.getComponents(loc);
			if (at.size() == 2) {
				Iterator<?> atit = at.iterator();
				Object at0 = atit.next();
				Object at1 = atit.next();
				if (at0 instanceof Wire && at1 instanceof Wire) {
					Wire w0 = (Wire) at0;
					Wire w1 = (Wire) at1;
					if (w0.isParallel(w1)) {
						sets.merge(w0, w1);
					}
				}
			}
		}

		ReplacementMap repl = new ReplacementMap();
		for (ArrayList<Wire> mergeSet : sets.getMergeSets()) {
			if (mergeSet.size() > 1) {
				ArrayList<Location> locs = new ArrayList<Location>(
						2 * mergeSet.size());
				for (Wire w : mergeSet) {
					locs.add(w.getEnd0());
					locs.add(w.getEnd1());
				}
				Collections.sort(locs);
				Location e0 = locs.get(0);
				Location e1 = locs.get(locs.size() - 1);
				Wire wnew = Wire.create(e0, e1);
				Collection<Wire> wset = Collections.singleton(wnew);

				for (Wire w : mergeSet) {
					if (!w.equals(wset)) {
						repl.put(w, wset);
					}
				}
			}
		}
		mutator.replace(circuit, repl);
	}

	private void doMergeSet(ArrayList<Wire> mergeSet,
			ReplacementMap replacements, Set<Location> splitLocs) {
		TreeSet<Location> ends = new TreeSet<Location>();
		for (Wire w : mergeSet) {
			ends.add(w.getEnd0());
			ends.add(w.getEnd1());
		}
		Wire whole = Wire.create(ends.first(), ends.last());

		TreeSet<Location> mids = new TreeSet<Location>();
		mids.add(whole.getEnd0());
		mids.add(whole.getEnd1());
		for (Location loc : whole) {
			if (splitLocs.contains(loc)) {
				for (Component comp : circuit.getComponents(loc)) {
					if (!mergeSet.contains(comp)) {
						mids.add(loc);
						break;
					}
				}
			}
		}

		ArrayList<Wire> mergeResult = new ArrayList<Wire>();
		if (mids.size() == 2) {
			mergeResult.add(whole);
		} else {
			Location e0 = mids.first();
			for (Location e1 : mids) {
				mergeResult.add(Wire.create(e0, e1));
				e0 = e1;
			}
		}

		for (Wire w : mergeSet) {
			ArrayList<Component> wRepl = new ArrayList<Component>(2);
			for (Wire w2 : mergeResult) {
				if (w2.overlaps(w, false)) {
					wRepl.add(w2);
				}
			}
			replacements.put(w, wRepl);
		}
	}

	/*
	 * for debugging: private void printWires(String prefix, PrintStream out) {
	 * boolean first = true; for (Wire w : circuit.getWires()) { if (first) {
	 * out.println(prefix + ": " + w); first = false; } else {
	 * out.println("      " + w); } } out.println(prefix + ": none"); }
	 */

	private void doOverlaps(CircuitMutator mutator) {
		HashMap<Location, ArrayList<Wire>> wirePoints;
		wirePoints = new HashMap<Location, ArrayList<Wire>>();
		for (Wire w : circuit.getWires()) {
			for (Location loc : w) {
				ArrayList<Wire> locWires = wirePoints.get(loc);
				if (locWires == null) {
					locWires = new ArrayList<Wire>(3);
					wirePoints.put(loc, locWires);
				}
				locWires.add(w);
			}
		}

		MergeSets mergeSets = new MergeSets();
		for (ArrayList<Wire> locWires : wirePoints.values()) {
			if (locWires.size() > 1) {
				for (int i = 0, n = locWires.size(); i < n; i++) {
					Wire w0 = locWires.get(i);
					for (int j = i + 1; j < n; j++) {
						Wire w1 = locWires.get(j);
						if (w0.overlaps(w1, false)) {
							mergeSets.merge(w0, w1);
						}
					}
				}
			}
		}

		ReplacementMap replacements = new ReplacementMap();
		Set<Location> splitLocs = circuit.wires.points.getSplitLocations();
		for (ArrayList<Wire> mergeSet : mergeSets.getMergeSets()) {
			if (mergeSet.size() > 1) {
				doMergeSet(mergeSet, replacements, splitLocs);
			}
		}
		mutator.replace(circuit, replacements);
	}

	private void doSplits(CircuitMutator mutator) {
		Set<Location> splitLocs = circuit.wires.points.getSplitLocations();
		ReplacementMap repl = new ReplacementMap();
		for (Wire w : circuit.getWires()) {
			Location w0 = w.getEnd0();
			Location w1 = w.getEnd1();
			ArrayList<Location> splits = null;
			for (Location loc : splitLocs) {
				if (w.contains(loc) && !loc.equals(w0) && !loc.equals(w1)) {
					if (splits == null)
						splits = new ArrayList<Location>();
					splits.add(loc);
				}
			}
			if (splits != null) {
				splits.add(w1);
				Collections.sort(splits);
				Location e0 = w0;
				ArrayList<Wire> subs = new ArrayList<Wire>(splits.size());
				for (Location e1 : splits) {
					subs.add(Wire.create(e0, e1));
					e0 = e1;
				}
				repl.put(w, subs);
			}
		}
		mutator.replace(circuit, repl);
	}

	@Override
	protected Map<Circuit, Integer> getAccessedCircuits() {
		return Collections.singletonMap(circuit, READ_WRITE);
	}

	@Override
	protected void run(CircuitMutator mutator) {
		doMerges(mutator);
		doOverlaps(mutator);
		doSplits(mutator);
	}
}
