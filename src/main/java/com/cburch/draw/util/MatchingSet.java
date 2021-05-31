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

package com.cburch.draw.util;

import com.cburch.draw.model.CanvasObject;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class MatchingSet<E extends CanvasObject> extends AbstractSet<E> {
  private final Set<Member<E>> set;

  public MatchingSet() {
    set = new HashSet<>();
  }

  public MatchingSet(Collection<E> initialContents) {
    set = new HashSet<>(initialContents.size());
    for (E value : initialContents) {
      set.add(new Member<>(value));
    }
  }

  @Override
  public boolean add(E value) {
    return set.add(new Member<>(value));
  }

  @Override
  public boolean contains(Object value) {
    @SuppressWarnings("unchecked")
    E eValue = (E) value;
    return set.contains(new Member<>(eValue));
  }

  @Override
  public Iterator<E> iterator() {
    return new MatchIterator<>(set.iterator());
  }

  @Override
  public boolean remove(Object value) {
    @SuppressWarnings("unchecked")
    E eValue = (E) value;
    return set.remove(new Member<>(eValue));
  }

  @Override
  public int size() {
    return set.size();
  }

  private static class MatchIterator<E extends CanvasObject> implements Iterator<E> {
    private final Iterator<Member<E>> it;

    MatchIterator(Iterator<Member<E>> it) {
      this.it = it;
    }

    public boolean hasNext() {
      return it.hasNext();
    }

    public E next() {
      return it.next().value;
    }

    public void remove() {
      it.remove();
    }
  }

  private static class Member<E extends CanvasObject> {
    final E value;

    public Member(E value) {
      this.value = value;
    }

    @Override
    public boolean equals(Object other) {
      @SuppressWarnings("unchecked")
      Member<E> that = (Member<E>) other;
      return this.value.matches(that.value);
    }

    @Override
    public int hashCode() {
      return value.matchesHashCode();
    }
  }
}
