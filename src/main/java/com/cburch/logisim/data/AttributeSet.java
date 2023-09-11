/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.data;

import java.util.List;

public interface AttributeSet {
  default void addAttributeListener(AttributeListener l) {
    // no-op implementation
  }

  Object clone();

  boolean containsAttribute(Attribute<?> attr);

  default Attribute<?> getAttribute(String name) {
    return null;
  }

  default List<Attribute<?>> getAttributes() {
    return null;
  }

  <V> V getValue(Attribute<V> attr);

  boolean isReadOnly(Attribute<?> attr);

  boolean isToSave(Attribute<?> attr);

  default void removeAttributeListener(AttributeListener l) {
    // no-op implementation
  }

  void setReadOnly(Attribute<?> attr, boolean value); // optional

  <V> void setValue(Attribute<V> attr, V value);

  /**
   * Returns attributes that may also be changed as a side effect of changing attr to value or
   * changing attr back to its current value from value. This method does not change attr.
   *
   * @param attr The attribute whose change is being considered
   * @param value The new value for attr that is being considered
   * @return a List of attributes that may also be changed, or null if there are no such attributes.
   */
  default <V> List<Attribute<?>> attributesMayAlsoBeChanged(Attribute<V> attr, V value) {
    return null;
  }
}
