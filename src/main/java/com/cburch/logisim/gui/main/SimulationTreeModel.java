/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.gui.main;

import com.cburch.logisim.circuit.CircuitState;
import com.cburch.logisim.comp.Component;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

public class SimulationTreeModel implements TreeModel {
  private final ArrayList<TreeModelListener> listeners;
  private final SimulationTreeTopNode root;
  private CircuitState currentView;

  public SimulationTreeModel(List<CircuitState> allRootStates) {
    this.listeners = new ArrayList<>();
    this.root = new SimulationTreeTopNode(this, allRootStates);
    this.currentView = null;
  }

  @Override
  public void addTreeModelListener(TreeModelListener l) {
    listeners.add(l);
  }

  private TreePath findPath(Object node) {
    final var path = new ArrayList<Object>();
    var current = node;
    while (current instanceof TreeNode treeNode) {
      path.add(0, current);
      current = treeNode.getParent();
    }
    if (current != null) {
      path.add(0, current);
    }
    return new TreePath(path.toArray());
  }

  protected void fireNodeChanged(Object node) {
    if (node != null) {
      final var e = new TreeModelEvent(this, findPath(node));
      for (final var l : listeners) {
        l.treeNodesChanged(e);
      }
    }
  }

  protected void fireStructureChanged(Object node) {
    final var e = new TreeModelEvent(this, findPath(node));
    for (final var l : listeners) {
      l.treeStructureChanged(e);
    }
  }

  @Override
  public Object getChild(Object parent, int index) {
    return (parent instanceof TreeNode node)
           ? node.getChildAt(index)
           : null;
  }

  @Override
  public int getChildCount(Object parent) {
    return (parent instanceof TreeNode node)
           ? node.getChildCount()
           : 0;
  }

  public CircuitState getCurrentView() {
    return currentView;
  }

  public void setCurrentView(CircuitState value) {
    final var oldView = currentView;
    if (oldView != value) {
      currentView = value;
      fireNodeChanged(mapToNode(oldView));
      fireNodeChanged(mapToNode(value));
    }
  }

  @Override
  public int getIndexOfChild(Object parent, Object child) {
    return (parent instanceof TreeNode parentNode && child instanceof TreeNode childNode)
            ? parentNode.getIndex(childNode)
            : -1;
  }

  @Override
  public Object getRoot() {
    return root;
  }

  @Override
  public boolean isLeaf(Object node) {
    return (node instanceof TreeNode treeNode)
           ? treeNode.getChildCount() == 0
           : true;
  }

  public void updateSimulationList(List<CircuitState> allRootStates) {
    root.updateSimulationList(allRootStates);
  }

  protected SimulationTreeNode mapComponentToNode(Component comp) {
    return null;
  }

  private SimulationTreeCircuitNode mapToNode(CircuitState state) {
    final var path = mapToPath(state);
    return (path != null)
        ? (SimulationTreeCircuitNode) path.getLastPathComponent()
        : null;
  }

  public TreePath mapToPath(CircuitState state) {
    if (state == null) return null;
    final var path = new ArrayList<CircuitState>();
    var current = state;
    var parent = current.getParentState();
    while (parent != null && parent != state) {
      path.add(current);
      current = parent;
      parent = current.getParentState();
    }
    path.add(current);

    final var pathNodes = new Object[path.size() + 1];
    pathNodes[0] = root;
    int pathPos = 1;
    SimulationTreeNode node = root;
    for (int i = path.size() - 1; i >= 0; i--) {
      current = path.get(i);
      SimulationTreeNode oldNode = node;
      for (final var child : Collections.list(node.children())) {
        if (child instanceof SimulationTreeCircuitNode circNode) {
          if (circNode.getCircuitState() == current) {
            node = circNode;
            break;
          }
        }
      }
      if (node == oldNode) {
        return null;
      }
      pathNodes[pathPos] = node;
      pathPos++;
    }
    return new TreePath(pathNodes);
  }

  @Override
  public void removeTreeModelListener(TreeModelListener l) {
    listeners.remove(l);
  }

  @Override
  public void valueForPathChanged(TreePath path, Object newValue) {
    throw new UnsupportedOperationException();
  }
}
