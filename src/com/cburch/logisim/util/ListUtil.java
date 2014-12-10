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
import java.util.Iterator;
import java.util.List;

public class ListUtil {
	private static class JoinedList<E> extends AbstractList<E> {
		List<? extends E> a;
		List<? extends E> b;

		JoinedList(List<? extends E> a, List<? extends E> b) {
			this.a = a;
			this.b = b;
		}

		@Override
		public E get(int index) {
			if (index < a.size())
				return a.get(index);
			else
				return b.get(index - a.size());
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

	public static <E> List<E> joinImmutableLists(List<? extends E> a,
			List<? extends E> b) {
		return new JoinedList<E>(a, b);
	}

	private ListUtil() {
	}
}