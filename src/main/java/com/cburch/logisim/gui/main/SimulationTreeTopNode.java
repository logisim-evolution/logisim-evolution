/**
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

import java.util.List;
import java.util.HashMap;
import javax.swing.tree.TreeNode;

import com.cburch.logisim.circuit.CircuitState;

class SimulationTreeTopNode extends SimulationTreeNode {

  public SimulationTreeTopNode(SimulationTreeModel model,
      List<CircuitState> allRootStates) {
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
      CircuitState state = ((SimulationTreeCircuitNode)node).getCircuitState();
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
    if (changed)
      model.fireStructureChanged(this);
  }

  @Override
  public String toString() {
    return "Active Simulations";
 
  }
}
