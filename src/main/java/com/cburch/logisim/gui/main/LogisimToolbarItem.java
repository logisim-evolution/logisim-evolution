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
import com.cburch.logisim.util.Icons;
import com.cburch.logisim.util.StringGetter;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import javax.swing.Icon;
import lombok.Setter;
import lombok.val;

public class LogisimToolbarItem implements ToolbarItem {
  private final MenuListener menu;
  private final LogisimMenuItem action;
  private Icon icon;
  @Setter private StringGetter toolTip;

  public LogisimToolbarItem(
      MenuListener menu, String iconName, LogisimMenuItem action, StringGetter toolTip) {
    this.menu = menu;
    this.icon = Icons.getIcon(iconName);
    this.action = action;
    this.toolTip = toolTip;
  }

  public LogisimToolbarItem(
      MenuListener menu, Icon icon, LogisimMenuItem action, StringGetter toolTip) {
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
  public Dimension getDimension(Object orientation) {
    if (icon == null) {
      return new Dimension(
          AppPreferences.getScaled(AppPreferences.IconSize),
          AppPreferences.getScaled(AppPreferences.IconSize));
    } else {
      val w = icon.getIconWidth();
      val h = icon.getIconHeight();
      return new Dimension(w, h + 2);
    }
  }

  @Override
  public String getToolTip() {
    return (toolTip != null) ? toolTip.toString() : null;
  }

  @Override
  public boolean isSelectable() {
    return menu != null && menu.isEnabled(action);
  }

  @Override
  public void paintIcon(Component destination, Graphics gfx) {
    if (!isSelectable() && gfx instanceof Graphics2D) {
      val c = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.3f);
      ((Graphics2D) gfx).setComposite(c);
    }

    if (icon == null) {
      val simple = AppPreferences.getScaled(AppPreferences.IconSize) >> 2;
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

  // FIXME: This looks like unused method. Remove?
  public void setIcon(String iconName) {
    this.icon = Icons.getIcon(iconName);
  }
}
