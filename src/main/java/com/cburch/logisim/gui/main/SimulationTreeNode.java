/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.gui.main;

import com.cburch.logisim.comp.ComponentFactory;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import javax.swing.tree.TreeNode;

public class SimulationTreeNode implements TreeNode {
  protected final SimulationTreeModel model;
  protected final SimulationTreeNode parent;
  protected ArrayList<TreeNode> children;

  public SimulationTreeNode(SimulationTreeModel model, SimulationTreeNode parent) {
    this.model = model;
    this.parent = parent;
    this.children = new ArrayList<>();
  }

  @Override
  public Enumeration<TreeNode> children() {
    return Collections.enumeration(children);
  }

  @Override
  public boolean getAllowsChildren() {
    return true;
  }

  @Override
  public TreeNode getChildAt(int index) {
    return children.get(index);
  }

  @Override
  public int getChildCount() {
    return children.size();
  }

  public ComponentFactory getComponentFactory() {
    return null;
  }

  @Override
  public int getIndex(TreeNode node) {
    return children.indexOf(node);
  }

  @Override
  public TreeNode getParent() {
    return parent;
  }

  public boolean isCurrentView(SimulationTreeModel model) {
    return false;
  }

  @Override
  public boolean isLeaf() {
    return false;
  }
}
