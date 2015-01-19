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

import java.io.PrintStream;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cburch.logisim.comp.Component;

public class ReplacementMap {

	final static Logger logger = LoggerFactory.getLogger(ReplacementMap.class);

	private boolean frozen;
	private HashMap<Component, HashSet<Component>> map;
	private HashMap<Component, HashSet<Component>> inverse;

	public ReplacementMap() {
		this(new HashMap<Component, HashSet<Component>>(),
				new HashMap<Component, HashSet<Component>>());
	}

	public ReplacementMap(Component oldComp, Component newComp) {
		this(new HashMap<Component, HashSet<Component>>(),
				new HashMap<Component, HashSet<Component>>());
		HashSet<Component> oldSet = new HashSet<Component>(3);
		oldSet.add(oldComp);
		HashSet<Component> newSet = new HashSet<Component>(3);
		newSet.add(newComp);
		map.put(oldComp, newSet);
		inverse.put(newComp, oldSet);
	}

	private ReplacementMap(HashMap<Component, HashSet<Component>> map,
			HashMap<Component, HashSet<Component>> inverse) {
		this.map = map;
		this.inverse = inverse;
	}

	public void add(Component comp) {
		if (frozen) {
			throw new IllegalStateException("cannot change map after frozen");
		}
		inverse.put(comp, new HashSet<Component>(3));
	}

	void append(ReplacementMap next) {
		for (Map.Entry<Component, HashSet<Component>> e : next.map.entrySet()) {
			Component b = e.getKey();
			HashSet<Component> cs = e.getValue(); // what b is replaced by
			HashSet<Component> as = this.inverse.remove(b); // what was replaced
															// to get b
			if (as == null) { // b pre-existed replacements so
				as = new HashSet<Component>(3); // we say it replaces itself.
				as.add(b);
			}

			for (Component a : as) {
				HashSet<Component> aDst = this.map.get(a);
				if (aDst == null) { // should happen when b pre-existed only
					aDst = new HashSet<Component>(cs.size());
					this.map.put(a, aDst);
				}
				aDst.remove(b);
				aDst.addAll(cs);
			}

			for (Component c : cs) {
				HashSet<Component> cSrc = this.inverse.get(c); // should always
																// be null
				if (cSrc == null) {
					cSrc = new HashSet<Component>(as.size());
					this.inverse.put(c, cSrc);
				}
				cSrc.addAll(as);
			}
		}

		for (Map.Entry<Component, HashSet<Component>> e : next.inverse
				.entrySet()) {
			Component c = e.getKey();
			if (!inverse.containsKey(c)) {
				HashSet<Component> bs = e.getValue();
				if (!bs.isEmpty()) {
					logger.error("Internal error: component replaced but not represented");
				}
				inverse.put(c, new HashSet<Component>(3));
			}
		}
	}

	void freeze() {
		frozen = true;
	}

	public Collection<Component> get(Component prev) {
		return map.get(prev);
	}

	public Collection<? extends Component> getAdditions() {
		return inverse.keySet();
	}

	public Collection<Component> getComponentsReplacing(Component comp) {
		return map.get(comp);
	}

	ReplacementMap getInverseMap() {
		return new ReplacementMap(inverse, map);
	}

	public Collection<? extends Component> getRemovals() {
		return map.keySet();
	}

	public Collection<Component> getReplacedComponents() {
		return map.keySet();
	}

	public boolean isEmpty() {
		return map.isEmpty() && inverse.isEmpty();
	}

	public void print(PrintStream out) {
		boolean found = false;
		for (Component c : getRemovals()) {
			if (!found)
				out.println("  removals:");
			found = true;
			out.println("    " + c.toString());
		}
		if (!found)
			out.println("  removals: none");

		found = false;
		for (Component c : getAdditions()) {
			if (!found)
				out.println("  additions:");
			found = true;
			out.println("    " + c.toString());
		}
		if (!found)
			out.println("  additions: none");
	}

	public void put(Component prev, Collection<? extends Component> next) {
		if (frozen) {
			throw new IllegalStateException("cannot change map after frozen");
		}

		HashSet<Component> repl = map.get(prev);
		if (repl == null) {
			repl = new HashSet<Component>(next.size());
			map.put(prev, repl);
		}
		repl.addAll(next);

		for (Component n : next) {
			repl = inverse.get(n);
			if (repl == null) {
				repl = new HashSet<Component>(3);
				inverse.put(n, repl);
			}
			repl.add(prev);
		}
	}

	public void remove(Component comp) {
		if (frozen) {
			throw new IllegalStateException("cannot change map after frozen");
		}
		map.put(comp, new HashSet<Component>(3));
	}

	public void replace(Component prev, Component next) {
		put(prev, Collections.singleton(next));
	}

	public void reset() {
		map.clear();
		inverse.clear();
	}
}
