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

/**
 * Code taken from Cornell's version of Logisim:
 * http://www.cs.cornell.edu/courses/cs3410/2015sp/
 */

package com.cburch.logisim.util;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;

public class UnionFind<E extends UnionFindElement<E>> implements Iterable<E> {

	private HashMap<E, Integer> sizes;

	public UnionFind(Collection<E> values) {
		this.sizes = new HashMap<E, Integer>();
		Integer one = Integer.valueOf(1);
		for (E elt : values) {
			elt.setUnionFindParent(elt);
			sizes.put(elt, one);
		}
	}

	public E findRepresentative(E value) {
		E parent = value.getUnionFindParent();
		if (parent == value) {
			return value;
		} else {
			parent = findRepresentative(parent);
			value.setUnionFindParent(parent);
			return parent;
		}
	}

	public int getRepresentativeCount() {
		return sizes.size();
	}

	public Collection<E> getRepresentatives() {
		return Collections.unmodifiableSet(sizes.keySet());
	}

	public int getSetSize(E value) {
		E repr = findRepresentative(value);
		return sizes.get(repr);
	}

	public Iterator<E> iterator() {
		return sizes.keySet().iterator();
	}

	public void union(E value0, E value1) {
		E repr0 = findRepresentative(value0);
		E repr1 = findRepresentative(value1);
		if (repr0 != repr1) {
			int size0 = sizes.get(repr0);
			int size1 = sizes.get(repr1);
			if (size0 < size1) {
				sizes.remove(repr0);
				repr0.setUnionFindParent(repr1);
				value0.setUnionFindParent(repr1);
				sizes.put(repr1, size0 + size1);
			} else {
				sizes.remove(repr1);
				repr1.setUnionFindParent(repr0);
				value1.setUnionFindParent(repr0);
				sizes.put(repr0, size0 + size1);
			}
		}
	}

}