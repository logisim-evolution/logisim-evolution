package com.cburch.contracts;

import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;

public interface BaseListDataListenerContract extends ListDataListener {
  @Override
  default void intervalAdded(ListDataEvent var1) {
    // no-op implementation
  }

  @Override
  default void intervalRemoved(ListDataEvent var1) {
    // no-op implementation
  }

  @Override
  default void contentsChanged(ListDataEvent var1) {
    // no-op implementation
  }
}
