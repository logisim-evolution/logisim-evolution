/*
 * Logisim-evolution - digital logic design tool and simulator
 * © 2001 Logisim-evolution contributors
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.draw.toolbar;

public interface ToolbarModelListener {
  void toolbarAppearanceChanged(ToolbarModelEvent event);

  void toolbarContentsChanged(ToolbarModelEvent event);
}
