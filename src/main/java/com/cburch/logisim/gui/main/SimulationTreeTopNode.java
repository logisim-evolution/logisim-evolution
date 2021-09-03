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
import java.util.HashMap;
import java.util.List;
import javax.swing.tree.TreeNode;

class SimulationTreeTopNode extends SimulationTreeNode {

  public SimulationTreeTopNode(SimulationTreeModel model, List<CircuitState> allRootStates) {
    super(model, null);
    for (CircuitState state : allRootStates)
      children.add(new SimulationTreeCircuitNode(model, null, state, null));
  }

  public void updateSimulationList(List<CircuitState> allRootStates) {
    boolean changed = false;
    HashMap<CircuitState, TreeNode> old = new HashMap<>();
    HashMap<CircuitState, Integer> oldPos = new HashMap<>();
    int i = 0;
    for (TreeNode node : children) {
      CircuitState state = ((SimulationTreeCircuitNode) node).getCircuitState();
      old.put(state, node);
      oldPos.put(state, i++);
    }
    children.clear();
    i = 0;
    for (CircuitState state : allRootStates) {
      TreeNode node = old.get(state);
      if (node == null) {
        changed = true;
        node = new SimulationTreeCircuitNode(model, null, state, null);
      } else if (oldPos.get(state) != i++) {
        changed = true;
      }
      children.add(node);
    }
    if (changed) model.fireStructureChanged(this);
  }

  @Override
  public String toString() {
    return "Active Simulations";
  }
}
