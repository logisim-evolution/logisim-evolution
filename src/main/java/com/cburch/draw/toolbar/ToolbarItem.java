/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.draw.toolbar;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;

public interface ToolbarItem {
  Dimension getDimension(Object orientation);

  String getToolTip();

  boolean isSelectable();

  void paintIcon(Component destination, Graphics gfx);
}
