/*
 * This file is part of logisim-evolution.
 *
 * Logisim-evolution is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 *
 * Logisim-evolution is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with logisim-evolution. If not, see <http://www.gnu.org/licenses/>.
 *
 * Original code by Carl Burch (http://www.cburch.com), 2011.
 * Subsequent modifications by:
 *   + College of the Holy Cross
 *     http://www.holycross.edu
 *   + Haute École Spécialisée Bernoise/Berner Fachhochschule
 *     http://www.bfh.ch
 *   + Haute École du paysage, d'ingénierie et d'architecture de Genève
 *     http://hepia.hesge.ch/
 *   + Haute École d'Ingénierie et de Gestion du Canton de Vaud
 *     http://www.heig-vd.ch/
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

  public void addTreeModelListener(TreeModelListener l) {
    listeners.add(l);
  }

  private TreePath findPath(Object node) {
    ArrayList<Object> path = new ArrayList<>();
    Object current = node;
    while (current instanceof TreeNode) {
      path.add(0, current);
      current = ((TreeNode) current).getParent();
    }
    if (current != null) {
      path.add(0, current);
    }
    return new TreePath(path.toArray());
  }

  protected void fireNodeChanged(Object node) {
    TreeModelEvent e = new TreeModelEvent(this, findPath(node));
    for (TreeModelListener l : listeners) {
      l.treeNodesChanged(e);
    }
  }

  protected void fireStructureChanged(Object node) {
    TreeModelEvent e = new TreeModelEvent(this, findPath(node));
    for (TreeModelListener l : listeners) {
      l.treeStructureChanged(e);
    }
  }

  public Object getChild(Object parent, int index) {
    if (parent instanceof TreeNode) {
      return ((TreeNode) parent).getChildAt(index);
    } else {
      return null;
    }
  }

  public int getChildCount(Object parent) {
    if (parent instanceof TreeNode) {
      return ((TreeNode) parent).getChildCount();
    } else {
      return 0;
    }
  }

  public CircuitState getCurrentView() {
    return currentView;
  }

  public void setCurrentView(CircuitState value) {
    CircuitState oldView = currentView;
    if (oldView != value) {
      currentView = value;

      SimulationTreeCircuitNode node1 = mapToNode(oldView);
      if (node1 != null) fireNodeChanged(node1);

      SimulationTreeCircuitNode node2 = mapToNode(value);
      if (node2 != null) fireNodeChanged(node2);
    }
  }

  public int getIndexOfChild(Object parent, Object child) {
    if (parent instanceof TreeNode && child instanceof TreeNode) {
      return ((TreeNode) parent).getIndex((TreeNode) child);
    } else {
      return -1;
    }
  }

  public Object getRoot() {
    return root;
  }

  public boolean isLeaf(Object node) {
    if (node instanceof TreeNode) {
      return ((TreeNode) node).getChildCount() == 0;
    } else {
      return true;
    }
  }

  public void updateSimulationList(List<CircuitState> allRootStates) {
    root.updateSimulationList(allRootStates);
  }

  protected SimulationTreeNode mapComponentToNode(Component comp) {
    return null;
  }

  private SimulationTreeCircuitNode mapToNode(CircuitState state) {
    TreePath path = mapToPath(state);
    if (path == null) {
      return null;
    } else {
      return (SimulationTreeCircuitNode) path.getLastPathComponent();
    }
  }

  public TreePath mapToPath(CircuitState state) {
    if (state == null) return null;
    ArrayList<CircuitState> path = new ArrayList<>();
    CircuitState current = state;
    CircuitState parent = current.getParentState();
    while (parent != null && parent != state) {
      path.add(current);
      current = parent;
      parent = current.getParentState();
    }
    path.add(current);

    Object[] pathNodes = new Object[path.size() + 1];
    pathNodes[0] = root;
    int pathPos = 1;
    SimulationTreeNode node = root;
    for (int i = path.size() - 1; i >= 0; i--) {
      current = path.get(i);
      SimulationTreeNode oldNode = node;
      for (TreeNode child : Collections.list(node.children())) {
        if (child instanceof SimulationTreeCircuitNode) {
          SimulationTreeCircuitNode circNode = (SimulationTreeCircuitNode) child;
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

  public void removeTreeModelListener(TreeModelListener l) {
    listeners.remove(l);
  }

  public void valueForPathChanged(TreePath path, Object newValue) {
    throw new UnsupportedOperationException();
  }
}
