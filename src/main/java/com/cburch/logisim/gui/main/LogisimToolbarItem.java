/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.gui.main;

import com.cburch.draw.toolbar.ToolbarItem;
import com.cburch.logisim.gui.menu.LogisimMenuItem;
import com.cburch.logisim.gui.menu.MenuListener;
import com.cburch.logisim.prefs.AppPreferences;
import com.cburch.logisim.util.IconsUtil;
import com.cburch.logisim.util.StringGetter;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import javax.swing.Icon;

public class LogisimToolbarItem implements ToolbarItem {
  private final MenuListener menu;
  private final LogisimMenuItem action;
  private Icon icon;
  private StringGetter toolTip;

  public LogisimToolbarItem(
      final MenuListener menu, final String iconName,
      final LogisimMenuItem action, final StringGetter toolTip) {
    this.menu = menu;
    this.icon = IconsUtil.getIcon(iconName);
    this.action = action;
    this.toolTip = toolTip;
  }

  public LogisimToolbarItem(
      final MenuListener menu, final Icon icon, final LogisimMenuItem action,
      final StringGetter toolTip) {
    this.menu = menu;
    this.icon = icon;
    this.action = action;
    this.toolTip = toolTip;
  }

  public void doAction() {
    if (menu != null && menu.isEnabled(action)) {
      menu.doAction(action);
    }
  }

  @Override
  public Dimension getDimension(final Object orientation) {
    if (icon == null) {
      return new Dimension(
          AppPreferences.getScaled(AppPreferences.IconSize),
          AppPreferences.getScaled(AppPreferences.IconSize));
    } else {
      int w = icon.getIconWidth();
      int h = icon.getIconHeight();
      return new Dimension(w, h + 2);
    }
  }

  @Override
  public String getToolTip() {
    return toolTip != null
      ? toolTip.toString()
      : null;
  }

  public void setToolTip(final StringGetter toolTip) {
    this.toolTip = toolTip;
  }

  @Override
  public boolean isSelectable() {
    return menu != null && menu.isEnabled(action);
  }

  @Override
  public void paintIcon(final Component destination, final Graphics gfx) {
    if (!isSelectable() && gfx instanceof Graphics2D g2d) {
      g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.3f));
    }

    if (icon == null) {
      final var simple = AppPreferences.getScaled(AppPreferences.IconSize) >> 2;
      gfx.setColor(new Color(255, 128, 128));
      gfx.fillRect(simple, simple, 2 * simple, 2 * simple);
      gfx.setColor(Color.BLACK);
      gfx.drawLine(simple, simple, 3 * simple, 3 * simple);
      gfx.drawLine(simple, 3 * simple, 3 * simple, simple);
      gfx.drawRect(simple, simple, 2 * simple, 2 * simple);
    } else {
      icon.paintIcon(destination, gfx, 0, 1);
    }
  }

  public void setIcon(String iconName) {
    this.icon = IconsUtil.getIcon(iconName);
  }
}
