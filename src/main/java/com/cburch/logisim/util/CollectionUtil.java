/*
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

import java.util.AbstractList;
import java.util.AbstractSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class CollectionUtil {
  private static class UnionList<E> extends AbstractList<E> {
    private final List<? extends E> listA;
    private final List<? extends E> listB;

    UnionList(List<? extends E> a, List<? extends E> b) {
      this.listA = a;
      this.listB = b;
    }

    @Override
    public E get(int index) {
      E ret;
      if (index < listA.size()) {
        ret = listA.get(index);
      } else {
        ret = listA.get(index - listA.size());
      }
      return ret;
    }

    @Override
    public int size() {
      return listA.size() + listB.size();
    }
  }

  private static class UnionSet<E> extends AbstractSet<E> {
    private final Set<? extends E> setA;
    private final Set<? extends E> setB;

    UnionSet(Set<? extends E> a, Set<? extends E> b) {
      this.setA = a;
      this.setB = b;
    }

    @Override
    public Iterator<E> iterator() {
      return IteratorUtil.createJoinedIterator(setA.iterator(), setB.iterator());
    }

    @Override
    public int size() {
      return setA.size() + setB.size();
    }
  }

  public static <E> List<E> createUnmodifiableListUnion(List<? extends E> a, List<? extends E> b) {
    return new UnionList<>(a, b);
  }

  public static <E> Set<E> createUnmodifiableSetUnion(Set<? extends E> a, Set<? extends E> b) {
    return new UnionSet<>(a, b);
  }

  private CollectionUtil() {}
}
