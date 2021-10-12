/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
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

    @Override
    public boolean hasNext() {
      return it.hasNext();
    }

    @Override
    public E next() {
      return it.next().value;
    }

    @Override
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
