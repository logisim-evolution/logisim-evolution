/*
 * Logisim-evolution - digital logic design tool and simulator
 * © 2001 Logisim-evolution contributors
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.util;

import java.util.AbstractList;
import java.util.Collections;
import java.util.List;

public class UnmodifiableList<E> extends AbstractList<E> {

  private final E[] data;

  public UnmodifiableList(E[] data) {
    this.data = data;
  }

  public static <E> List<E> create(E[] data) {
    if (data.length == 0) {
      return Collections.emptyList();
    } else {
      return new UnmodifiableList<>(data);
    }
  }

  @Override
  public E get(int index) {
    return data[index];
  }

  @Override
  public int size() {
    return data.length;
  }
}
