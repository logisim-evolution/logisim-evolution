/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.gui.appear;

import com.cburch.draw.toolbar.ToolbarClickableItem;
import com.cburch.draw.tools.DrawingAttributeSet;
import com.cburch.logisim.gui.icons.ShowStateIcon;
import java.awt.Dimension;
import java.awt.Graphics;
import javax.swing.Icon;

public class ShowStateTool implements ToolbarClickableItem {

  private final AppearanceView view;
  private final AppearanceCanvas canvas;
  private final Icon icon;
  private final Icon pressed;

  public ShowStateTool(AppearanceView view, AppearanceCanvas canvas, DrawingAttributeSet attrs) {
    this.view = view;
    this.canvas = canvas;
    icon = new ShowStateIcon(false);
    pressed = new ShowStateIcon(true);
  }

  @Override
  public Dimension getDimension(Object orientation) {
    return new Dimension(icon.getIconWidth() + 8, icon.getIconHeight() + 8);
  }

  @Override
  public String getToolTip() {
    // FIXME: hardcoded string
    return "Select state to be shown";
  }

  @Override
  public boolean isSelectable() {
    return false;
  }

  @Override
  public void clicked() {
    final var w = new ShowStateDialog(view.getFrame(), canvas);
    final var p = view.getFrame().getLocation();
    p.translate(80, 50);
    w.setLocation(p);
    w.setVisible(true);
  }

  @Override
  public void paintIcon(java.awt.Component destination, Graphics gfx) {
    icon.paintIcon(destination, gfx, 4, 4);
  }

  @Override
  public void paintPressedIcon(java.awt.Component destination, Graphics gfx) {
    pressed.paintIcon(destination, gfx, 4, 4);
  }
}
