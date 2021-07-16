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
