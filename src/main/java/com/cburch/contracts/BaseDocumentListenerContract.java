package com.cburch.contracts;

import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

public interface BaseDocumentListenerContract extends DocumentListener {

  @Override
  default void insertUpdate(DocumentEvent event) {
    // dummy implementation
  }

  @Override
  default void removeUpdate(DocumentEvent event) {
    // dummy implementation
  }

  @Override
  default void changedUpdate(DocumentEvent event) {
    // dummy implementation
  }
}
