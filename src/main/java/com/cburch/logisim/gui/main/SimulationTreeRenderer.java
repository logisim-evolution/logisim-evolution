/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.gui.main;

import com.cburch.logisim.comp.ComponentDrawContext;
import com.cburch.logisim.comp.ComponentFactory;
import com.cburch.logisim.gui.generic.ProjectExplorer;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JTree;
import javax.swing.tree.DefaultTreeCellRenderer;

public class SimulationTreeRenderer extends DefaultTreeCellRenderer {
  private static final long serialVersionUID = 1L;

  @Override
  public Component getTreeCellRendererComponent(
      JTree tree,
      Object value,
      boolean selected,
      boolean expanded,
      boolean leaf,
      int row,
      boolean hasFocus) {
    Component ret =
        super.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);
    final var model = (SimulationTreeModel) tree.getModel();
    if (ret instanceof JLabel label) {
      if (value instanceof SimulationTreeNode node) {
        final var factory = node.getComponentFactory();
        if (factory != null) {
          label.setIcon(new RendererIcon(factory, node.isCurrentView(model)));
        }
      }
    }
    return ret;
  }

  private static class RendererIcon implements Icon {
    private final ComponentFactory factory;
    private final boolean isCurrentView;

    RendererIcon(ComponentFactory factory, boolean isCurrentView) {
      this.factory = factory;
      this.isCurrentView = isCurrentView;
    }

    @Override
    public int getIconHeight() {
      return 20;
    }

    @Override
    public int getIconWidth() {
      return 20;
    }

    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
      final var context = new ComponentDrawContext(c, null, null, g, g);
      factory.paintIcon(context, x, y, factory.createAttributeSet());

      // draw magnifying glass if appropriate
      if (isCurrentView) {
        final var tx = x + 13;
        final var ty = y + 13;
        int[] xp = {tx - 1, x + 18, x + 20, tx + 1};
        int[] yp = {ty + 1, y + 20, y + 18, ty - 1};
        g.setColor(ProjectExplorer.MAGNIFYING_INTERIOR);
        g.fillOval(x + 5, y + 5, 10, 10);
        g.setColor(Color.BLACK);
        g.drawOval(x + 5, y + 5, 10, 10);
        g.fillPolygon(xp, yp, xp.length);
      }
    }
  }
}
