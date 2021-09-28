/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.data;

public class AttributeEvent {
  private final AttributeSet source;
  private final Attribute<?> attr;
  private final Object value;
  private final Object oldvalue;

  public AttributeEvent(AttributeSet source) {
    this(source, null, null, null);
  }

  public AttributeEvent(AttributeSet source, Attribute<?> attr, Object value, Object oldvalue) {
    this.source = source;
    this.attr = attr;
    this.value = value;
    this.oldvalue = oldvalue;
  }

  public Attribute<?> getAttribute() {
    return attr;
  }

  public AttributeSet getSource() {
    return source;
  }

  public Object getValue() {
    return value;
  }

  public Object getOldValue() {
    return oldvalue;
  }
}
