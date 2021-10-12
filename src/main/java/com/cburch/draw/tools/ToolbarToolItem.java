/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.draw.tools;

import com.cburch.draw.toolbar.ToolbarItem;
import com.cburch.logisim.prefs.AppPreferences;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import javax.swing.Icon;

public class ToolbarToolItem implements ToolbarItem {
  private final AbstractTool tool;
  private final Icon icon;

  public ToolbarToolItem(AbstractTool tool) {
    this.tool = tool;
    this.icon = tool.getIcon();
  }

  @Override
  public Dimension getDimension(Object orientation) {
    if (icon == null) {
      return new Dimension(
          AppPreferences.getScaled(AppPreferences.IconSize),
          AppPreferences.getScaled(AppPreferences.IconSize));
    } else {
      return new Dimension(
          icon.getIconWidth() + 4 * AppPreferences.ICON_BORDER,
          icon.getIconHeight() + 4 * AppPreferences.ICON_BORDER);
    }
  }

  public AbstractTool getTool() {
    return tool;
  }

  @Override
  public String getToolTip() {
    return tool.getDescription();
  }

  @Override
  public boolean isSelectable() {
    return true;
  }

  @Override
  public void paintIcon(Component destination, Graphics gfx) {
    if (icon == null) {
      gfx.setColor(new Color(255, 128, 128));
      gfx.fillRect(4, 4, 8, 8);
      gfx.setColor(Color.BLACK);
      gfx.drawLine(4, 4, 12, 12);
      gfx.drawLine(4, 12, 12, 4);
      gfx.drawRect(4, 4, 8, 8);
    } else {
      icon.paintIcon(destination, gfx, 4, 4);
    }
  }
}
