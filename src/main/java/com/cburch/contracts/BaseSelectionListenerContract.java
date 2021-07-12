package com.cburch.contracts;

import com.cburch.draw.canvas.SelectionEvent;
import com.cburch.draw.canvas.SelectionListener;

public interface BaseSelectionListenerContract extends SelectionListener {
  @Override
  default void selectionChanged(SelectionEvent e) {
    // default implementation
  }
}
