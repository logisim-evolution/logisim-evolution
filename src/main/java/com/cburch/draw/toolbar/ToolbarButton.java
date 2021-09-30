/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.draw.toolbar;

import com.cburch.contracts.BaseMouseListenerContract;
import com.cburch.logisim.util.GraphicsUtil;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.MouseEvent;
import javax.swing.JComponent;

class ToolbarButton extends JComponent implements BaseMouseListenerContract {
  private static final long serialVersionUID = 1L;

  private static final int BORDER = 2;

  private final Toolbar toolbar;
  private final ToolbarItem item;

  ToolbarButton(Toolbar toolbar, ToolbarItem item) {
    this.toolbar = toolbar;
    this.item = item;
    addMouseListener(this);
    setFocusable(true);
    setToolTipText("");
  }

  public ToolbarItem getItem() {
    return item;
  }

  @Override
  public Dimension getMinimumSize() {
    return getPreferredSize();
  }

  @Override
  public Dimension getPreferredSize() {
    final var dim = item.getDimension(toolbar.getOrientation());
    dim.width += 2 * BORDER;
    dim.height += 2 * BORDER;
    return dim;
  }

  @Override
  public String getToolTipText(MouseEvent e) {
    return item.getToolTip();
  }

  @Override
  public void mouseClicked(MouseEvent e) {
    // dummy
  }

  @Override
  public void mouseEntered(MouseEvent e) {
    // dummy
  }

  @Override
  public void mouseExited(MouseEvent e) {
    toolbar.setPressed(null);
  }

  @Override
  public void mousePressed(MouseEvent e) {
    if (item != null && (item.isSelectable() || (item instanceof ToolbarClickableItem))) {
      toolbar.setPressed(this);
    }
  }

  @Override
  public void mouseReleased(MouseEvent e) {
    if (toolbar.getPressed() == this) {
      toolbar.setPressed(null);
      if (item != null && item.isSelectable()) {
        toolbar.getToolbarModel().itemSelected(item);
      } else if (item != null && item instanceof ToolbarClickableItem clickableItem) {
        clickableItem.clicked();
      }
    }
  }

  @Override
  public void paintComponent(Graphics g) {
    if (toolbar.getPressed() == this) {
      if (item instanceof ToolbarClickableItem clickableItem) {
        final var g2 = g.create();
        g2.translate(BORDER, BORDER);
        clickableItem.paintPressedIcon(ToolbarButton.this, g2);
        g2.dispose();
        return;
      }
      final var dim = item.getDimension(toolbar.getOrientation());
      final var defaultColor = g.getColor();
      GraphicsUtil.switchToWidth(g, 2);
      g.setColor(Color.GRAY);
      g.fillRect(BORDER, BORDER, dim.width, dim.height);
      GraphicsUtil.switchToWidth(g, 1);
      g.setColor(defaultColor);
    }

    final var g2 = g.create();
    g2.translate(BORDER, BORDER);
    item.paintIcon(ToolbarButton.this, g2);
    g2.dispose();

    // draw selection indicator
    if (toolbar.getToolbarModel().isSelected(item)) {
      final var dim = item.getDimension(toolbar.getOrientation());
      GraphicsUtil.switchToWidth(g, 2);
      g.setColor(Color.BLACK);
      g.drawRect(BORDER, BORDER, dim.width, dim.height);
      GraphicsUtil.switchToWidth(g, 1);
    }
  }
}
