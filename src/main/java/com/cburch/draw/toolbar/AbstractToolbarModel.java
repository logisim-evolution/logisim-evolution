/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.draw.toolbar;

import java.util.ArrayList;
import java.util.List;
import lombok.val;

public abstract class AbstractToolbarModel implements ToolbarModel {
  private final List<ToolbarModelListener> listeners;

  public AbstractToolbarModel() {
    listeners = new ArrayList<>();
  }

  @Override
  public void addToolbarModelListener(ToolbarModelListener listener) {
    listeners.add(listener);
  }

  protected void fireToolbarAppearanceChanged() {
    val event = new ToolbarModelEvent(this);
    for (val listener : listeners) {
      listener.toolbarAppearanceChanged(event);
    }
  }

  protected void fireToolbarContentsChanged() {
    val event = new ToolbarModelEvent(this);
    for (val listener : listeners) {
      listener.toolbarContentsChanged(event);
    }
  }

  @Override
  public abstract List<ToolbarItem> getItems();

  @Override
  public abstract boolean isSelected(ToolbarItem item);

  @Override
  public abstract void itemSelected(ToolbarItem item);

  @Override
  public void removeToolbarModelListener(ToolbarModelListener listener) {
    listeners.remove(listener);
  }
}
