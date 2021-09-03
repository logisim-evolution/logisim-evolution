/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 * 
 * https://github.com/logisim-evolution/
 * 
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.prefs;

import com.cburch.logisim.data.AttributeOption;

public class ConvertEvent {
  private final AttributeOption value;

  public ConvertEvent(AttributeOption value) {
    this.value = value;
  }

  public AttributeOption getValue() {
    return value;
  }
}
