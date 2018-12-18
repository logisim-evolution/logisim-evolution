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

package com.cburch.logisim.util;

import java.util.AbstractList;
import java.util.AbstractSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class CollectionUtil {
	private static class UnionList<E> extends AbstractList<E> {
		private List<? extends E> a;
		private List<? extends E> b;

		UnionList(List<? extends E> a, List<? extends E> b) {
			this.a = a;
			this.b = b;
		}

		@Override
		public E get(int index) {
			E ret;
			if (index < a.size()) {
				ret = a.get(index);
			} else {
				ret = a.get(index - a.size());
			}
			return ret;
		}

		@Override
		public int size() {
			return a.size() + b.size();
		}
	}

	private static class UnionSet<E> extends AbstractSet<E> {
		private Set<? extends E> a;
		private Set<? extends E> b;

		UnionSet(Set<? extends E> a, Set<? extends E> b) {
			this.a = a;
			this.b = b;
		}

		@Override
		public Iterator<E> iterator() {
			return IteratorUtil
					.createJoinedIterator(a.iterator(), b.iterator());
		}

		@Override
		public int size() {
			return a.size() + b.size();
		}
	}

	public static <E> List<E> createUnmodifiableListUnion(List<? extends E> a,
			List<? extends E> b) {
		return new UnionList<E>(a, b);
	}

	public static <E> Set<E> createUnmodifiableSetUnion(Set<? extends E> a,
			Set<? extends E> b) {
		return new UnionSet<E>(a, b);
	}

	private CollectionUtil() {
	}
}
