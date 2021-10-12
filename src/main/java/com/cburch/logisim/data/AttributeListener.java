/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.data;

public interface AttributeListener {
  default void attributeListChanged(AttributeEvent e) {
    // no-op implementation
  }

  default void attributeValueChanged(AttributeEvent e) {
    // no-op implementation
  }
}
