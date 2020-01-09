/**
 * This file is part of logisim-evolution.
 *
 * Logisim-evolution is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 *
 * Logisim-evolution is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License along 
 * with logisim-evolution. If not, see <http://www.gnu.org/licenses/>.
 *
 * Original code by Carl Burch (http://www.cburch.com), 2011.
 * Subsequent modifications by:
 *   + College of the Holy Cross
 *     http://www.holycross.edu
 *   + Haute École Spécialisée Bernoise/Berner Fachhochschule
 *     http://www.bfh.ch
 *   + Haute École du paysage, d'ingénierie et d'architecture de Genève
 *     http://hepia.hesge.ch/
 *   + Haute École d'Ingénierie et de Gestion du Canton de Vaud
 *     http://www.heig-vd.ch/
 */

package com.cburch.logisim.util;

import java.util.Iterator;
import java.util.NoSuchElementException;

public class IteratorUtil {

  private static class EmptyIterator<E> implements Iterator<E> {
    private EmptyIterator() {}

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

    private IteratorUnion(Iterator<? extends E> cur, Iterator<? extends E> next) {
      this.cur = cur;
      this.next = next;
    }

    public boolean hasNext() {
      return cur.hasNext() || (next != null && next.hasNext());
    }

    public E next() {
      if (!cur.hasNext()) {
        if (next == null) throw new NoSuchElementException();
        cur = next;
        if (!cur.hasNext()) throw new NoSuchElementException();
      }
      return cur.next();
    }

    public void remove() {
      cur.remove();
    }
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

  public static <E> Iterator<E> emptyIterator() {
    return new EmptyIterator<E>();
  }

  public static Iterator<?> EMPTY_ITERATOR = new EmptyIterator<Object>();
}
