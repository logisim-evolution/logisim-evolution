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
import lombok.Getter;

public class AttributeOption implements AttributeOptionInterface {
  @Getter private final Object value;
  private final String name;
  @Getter private final StringGetter displayGetter;

  public AttributeOption(Object value, String name, StringGetter displayGetter) {
    this.value = value;
    this.name = name;
    this.displayGetter = displayGetter;
  }

  public AttributeOption(Object value, StringGetter displayGetter) {
    this.value = value;
    this.name = value.toString();
    this.displayGetter = displayGetter;
  }

  @Override
  public String toDisplayString() {
    return displayGetter.toString();
  }

  @Override
  public String toString() {
    return name;
  }
}
