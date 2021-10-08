/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.gui.appear;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;

import javax.swing.Icon;
import javax.swing.JOptionPane;

import com.cburch.draw.canvas.Canvas;
import com.cburch.draw.toolbar.ToolbarClickableItem;
import com.cburch.logisim.gui.generic.OptionPane;
import com.cburch.logisim.gui.icons.ArithmeticIcon;
import com.cburch.logisim.gui.icons.CircuitIcon;

public class ResetAppearanceTool implements ToolbarClickableItem {

  private final AppearanceCanvas canvas;
  private final boolean isClear;
  private final Icon icon;

  public ResetAppearanceTool(Canvas canvas, boolean isClear) {
    this.canvas = (canvas instanceof AppearanceCanvas appearanceCanvas) ? appearanceCanvas : null;
    this.isClear = isClear;
    icon = isClear ? new ArithmeticIcon("RST", 3) : new CircuitIcon();
  }

  @Override
  public Dimension getDimension(Object orientation) {
    return new Dimension(icon.getIconWidth() + 8, icon.getIconHeight() + 8);
  }

  @Override
  public String getToolTip() {
    // FIXME: hardcoded string
    return isClear ? "Restore default custom appearance" : "Clear appearance and load logisim default";
  }

  @Override
  public boolean isSelectable() {
    return false;
  }

  @Override
  public void paintIcon(Component destination, Graphics gfx) {
    icon.paintIcon(destination, gfx, 4, 4);
  }

  @Override
  public void clicked() {
    if (canvas == null || canvas.getCircuit() == null) return;
    final var appearance = canvas.getCircuit().getAppearance();
    if (appearance == null) return;
    // FIXME: hardcoded string
    if (OptionPane.showConfirmDialog(canvas, 
        "Are you sure you want to remove the current custom appearance and replace it?", 
        isClear ? "Restore default custom appearance" : "Clear appearance and load logisim default", 
        JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
      if (isClear) appearance.resetDefaultCustomAppearance();
      else appearance.loadDefaultLogisimAppearance();
      canvas.repaint(canvas.getBounds(null));
    }
  }

  @Override
  public void paintPressedIcon(Component destination, Graphics gfx) {
    icon.paintIcon(destination, gfx, 4, 4);
  }

}
