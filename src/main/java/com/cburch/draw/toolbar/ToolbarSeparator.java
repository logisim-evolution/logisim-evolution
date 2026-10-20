/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.draw.toolbar;

import com.cburch.logisim.prefs.AppPreferences;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;

public class ToolbarSeparator implements ToolbarItem {
  private final int size;

  public ToolbarSeparator(int size) {
    this.size = size;
  }

  @Override
  public Dimension getDimension(Object orientation) {
    return new Dimension(size, size);
  }

  @Override
  public String getToolTip() {
    return null;
  }

  @Override
  public boolean isSelectable() {
    return false;
  }

  @Override
  public void paintIcon(Component destination, Graphics gfx) {
    final var dim = destination.getSize();
    var x = 0;
    var y = 0;
    var w = dim.width;
    var h = dim.height;
    final var width = AppPreferences.getScaled(2);
    if (h >= w) { // separator is a vertical line in horizontal toolbar
      x = (w - width - 2) / 2;
      w = width;
    } else { // separator is a horizontal line in vertical toolbar
      y = (h - width - 2) / 2;
      h = width;
    }
    gfx.setColor(Color.GRAY);
    gfx.fillRect(x, y, w, h);
  }
}
