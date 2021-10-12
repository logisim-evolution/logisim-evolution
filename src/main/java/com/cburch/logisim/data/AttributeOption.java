/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.data;

import com.cburch.logisim.util.StringGetter;

public class AttributeOption implements AttributeOptionInterface {
  private final Object value;
  private final String name;
  private final StringGetter desc;

  public AttributeOption(Object value, String name, StringGetter desc) {
    this.value = value;
    this.name = name;
    this.desc = desc;
  }

  public AttributeOption(Object value, StringGetter desc) {
    this.value = value;
    this.name = value.toString();
    this.desc = desc;
  }

  public Object getValue() {
    return value;
  }

  public String toDisplayString() {
    return desc.toString();
  }

  public StringGetter getDisplayGetter() {
    return desc;
  }

  @Override
  public String toString() {
    return name;
  }
}
