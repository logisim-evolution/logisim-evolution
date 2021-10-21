/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.util;

import java.util.AbstractList;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public final class CollectionUtil {
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

  private CollectionUtil() {}

  public static <E> List<E> createUnmodifiableListUnion(List<? extends E> a, List<? extends E> b) {
    return new UnionList<>(a, b);
  }

  public static <E> Set<E> createUnmodifiableSetUnion(Set<? extends E> a, Set<? extends E> b) {
    return new UnionSet<>(a, b);
  }

  /**
   * Checks if given collection is either null or empty.
   */
  public static boolean isNullOrEmpty(Collection collection) {
    return collection == null || collection.isEmpty();
  }

  /**
   * Checks if given collection is not empty and not null.
   */
  public static boolean isNotEmpty(Collection collection) {
    return collection != null && !collection.isEmpty();
  }
}
