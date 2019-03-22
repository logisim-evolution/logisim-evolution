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

import com.cburch.logisim.comp.ComponentFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import javax.swing.tree.TreeNode;

public class SimulationTreeNode implements TreeNode {
  protected SimulationTreeModel model;
  protected SimulationTreeNode parent;
  protected ArrayList<TreeNode> children;

  public SimulationTreeNode(SimulationTreeModel model, SimulationTreeNode parent) {
    this.model = model;
    this.parent = parent;
    this.children = new ArrayList<TreeNode>();
  }

  public Enumeration<TreeNode> children() {
    return Collections.enumeration(children);
  }

  public boolean getAllowsChildren() {
    return true;
  }

  public TreeNode getChildAt(int index) {
    return children.get(index);
  }

  public int getChildCount() {
    return children.size();
  }

  public ComponentFactory getComponentFactory() {
    return null;
  }

  public int getIndex(TreeNode node) {
    return children.indexOf(node);
  }

  public TreeNode getParent() {
    return parent;
  }

  public boolean isCurrentView(SimulationTreeModel model) {
    return false;
  }

  public boolean isLeaf() {
	  return false;
  }
}
