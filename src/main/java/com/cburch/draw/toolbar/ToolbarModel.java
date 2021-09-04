/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.draw.toolbar;

import java.util.List;

public interface ToolbarModel {
  void addToolbarModelListener(ToolbarModelListener listener);

  List<ToolbarItem> getItems();

  boolean isSelected(ToolbarItem item);

  void itemSelected(ToolbarItem item);

  void removeToolbarModelListener(ToolbarModelListener listener);
}
