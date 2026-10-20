/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.gui.main;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Font;
import java.util.List;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.Test;

class SimulationTreeRendererTest {

  @Test
  void inactiveNodeReservesWidthForCurrentViewDecoration() throws Exception {
    SwingUtilities.invokeAndWait(
        () -> {
          final var model = new SimulationTreeModel(List.of());
          final var node = new TestNode(model, (SimulationTreeNode) model.getRoot(), "R00");
          final var tree = new JTree(model);
          tree.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
          final var renderer = new SimulationTreeRenderer();

          node.currentView = false;
          final var inactiveWidth = render(renderer, tree, node).getPreferredSize().width;

          node.currentView = true;
          final var currentViewWidth = render(renderer, tree, node).getPreferredSize().width;

          assertTrue(
              inactiveWidth >= currentViewWidth,
              () ->
                  "Inactive row width "
                      + inactiveWidth
                      + " does not reserve space for current-view width "
                      + currentViewWidth);
        });
  }

  @Test
  void currentViewBoldFontDoesNotLeakIntoFollowingRows() throws Exception {
    SwingUtilities.invokeAndWait(
        () -> {
          final var model = new SimulationTreeModel(List.of());
          final var root = (SimulationTreeNode) model.getRoot();
          final var current = new TestNode(model, root, "current");
          final var inactive = new TestNode(model, root, "inactive");
          final var tree = new JTree(model);
          tree.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
          final var renderer = new SimulationTreeRenderer();

          current.currentView = true;
          assertTrue(render(renderer, tree, current).getFont().isBold());

          inactive.currentView = false;
          assertFalse(render(renderer, tree, inactive).getFont().isBold());
        });
  }

  private static java.awt.Component render(
      SimulationTreeRenderer renderer, JTree tree, SimulationTreeNode node) {
    return renderer.getTreeCellRendererComponent(tree, node, false, false, true, 0, false);
  }

  private static class TestNode extends SimulationTreeNode {
    private boolean currentView;
    private final String text;

    TestNode(SimulationTreeModel model, SimulationTreeNode parent, String text) {
      super(model, parent);
      this.text = text;
    }

    @Override
    public boolean isCurrentView(SimulationTreeModel model) {
      return currentView;
    }

    @Override
    public boolean isLeaf() {
      return true;
    }

    @Override
    public String toString() {
      return text;
    }
  }
}
