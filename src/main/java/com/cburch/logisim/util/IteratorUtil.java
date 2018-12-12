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

import java.util.Iterator;
import java.util.NoSuchElementException;

public class IteratorUtil {
	private static class ArrayIterator<E> implements Iterator<E> {
		private E[] data;
		private int i = -1;

		private ArrayIterator(E[] data) {
			this.data = data;
		}

		public boolean hasNext() {
			return i + 1 < data.length;
		}

		public E next() {
			if (!hasNext())
				throw new NoSuchElementException();
			i++;
			return data[i];
		}

		public void remove() {
			throw new UnsupportedOperationException("ArrayIterator.remove");
		}
	}

	private static class EmptyIterator<E> implements Iterator<E> {
		private EmptyIterator() {
		}

		public boolean hasNext() {
			return false;
		}

		public E next() {
			throw new NoSuchElementException();
		}

		public void remove() {
			throw new UnsupportedOperationException("EmptyIterator.remove");
		}
	}

	private static class IteratorUnion<E> implements Iterator<E> {
		Iterator<? extends E> cur;
		Iterator<? extends E> next;

		private IteratorUnion(Iterator<? extends E> cur,
				Iterator<? extends E> next) {
			this.cur = cur;
			this.next = next;
		}

		public boolean hasNext() {
			return cur.hasNext() || (next != null && next.hasNext());
		}

		public E next() {
			if (!cur.hasNext()) {
				if (next == null)
					throw new NoSuchElementException();
				cur = next;
				if (!cur.hasNext())
					throw new NoSuchElementException();
			}
			return cur.next();
		}

		public void remove() {
			cur.remove();
		}
	}

	private static class UnitIterator<E> implements Iterator<E> {
		private E data;
		private boolean taken = false;

		private UnitIterator(E data) {
			this.data = data;
		}

		public boolean hasNext() {
			return !taken;
		}

		public E next() {
			if (taken)
				throw new NoSuchElementException();
			taken = true;
			return data;
		}

		public void remove() {
			throw new UnsupportedOperationException("UnitIterator.remove");
		}
	}

	public static <E> Iterator<E> createArrayIterator(E[] data) {
		return new ArrayIterator<E>(data);
	}

	public static <E> Iterator<E> createJoinedIterator(
			Iterator<? extends E> i0, Iterator<? extends E> i1) {
		if (!i0.hasNext()) {
			@SuppressWarnings("unchecked")
			Iterator<E> ret = (Iterator<E>) i1;
			return ret;
		} else if (!i1.hasNext()) {
			@SuppressWarnings("unchecked")
			Iterator<E> ret = (Iterator<E>) i0;
			return ret;
		} else {
			return new IteratorUnion<E>(i0, i1);
		}
	}

	public static <E> Iterator<E> createUnitIterator(E data) {
		return new UnitIterator<E>(data);
	}

	public static <E> Iterator<E> emptyIterator() {
		return new EmptyIterator<E>();
	}

	public static Iterator<?> EMPTY_ITERATOR = new EmptyIterator<Object>();

}
