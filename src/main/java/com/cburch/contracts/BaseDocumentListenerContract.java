/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.contracts;

import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

public interface BaseDocumentListenerContract extends DocumentListener {

  @Override
  default void insertUpdate(DocumentEvent event) {
    // no-op implementation
  }

  @Override
  default void removeUpdate(DocumentEvent event) {
    // no-op implementation
  }

  @Override
  default void changedUpdate(DocumentEvent event) {
    // no-op implementation
  }
}
