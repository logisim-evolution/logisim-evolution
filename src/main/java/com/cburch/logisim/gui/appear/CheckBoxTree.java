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

package com.cburch.logisim.gui.appear;

import java.util.ArrayList;
import java.util.Enumeration;

import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeNode;
import javax.swing.event.TreeModelListener;
import javax.swing.event.TreeModelEvent;

import org.scijava.swing.checkboxtree.CheckBoxNodeData;
import org.scijava.swing.checkboxtree.CheckBoxNodeEditor;
import org.scijava.swing.checkboxtree.CheckBoxNodeRenderer;

public class CheckBoxTree extends JTree {

  public CheckBoxTree(DefaultMutableTreeNode root) {
    super(root);
    final var renderer = new CheckBoxNodeRenderer();
    this.setCellRenderer(renderer);
    this.setCellEditor(new CheckBoxNodeEditor(this));
    this.setEditable(true);

    getModel().addTreeModelListener(new TreeModelListener() {
      @Override
      public void treeNodesChanged(TreeModelEvent e) {
        nodeModified(e.getTreePath(), e.getChildIndices());
      }
      @Override
      public void treeStructureChanged(TreeModelEvent e) {
      }
      @Override
      public void treeNodesRemoved(TreeModelEvent e) {
      }
      @Override
      public void treeNodesInserted(TreeModelEvent e) {
      }
    });
  }

  private void nodeModified(TreePath parentPath, int[] changedChildren) {
    var parent = (DefaultMutableTreeNode) parentPath.getLastPathComponent();
    DefaultMutableTreeNode node;
    if (changedChildren == null) {
      node = parent;
      parent = null;
    } else {
      node = (DefaultMutableTreeNode) parent.getChildAt(changedChildren[0]);
    }
    var checked = ((CheckBoxNodeData) node.getUserObject()).isChecked();
    markDescendents(node, checked);
    for (DefaultMutableTreeNode p = parent; p != null; p = (DefaultMutableTreeNode) p.getParent()) {
      adjustParentForChildrenValues(p);
    }
  }

  private void markDescendents(DefaultMutableTreeNode node, boolean checked) {
    for (Enumeration<TreeNode> e = node.depthFirstEnumeration(); e.hasMoreElements();) {
      DefaultMutableTreeNode n = (DefaultMutableTreeNode) (e.nextElement());
      ((CheckBoxNodeData) (n.getUserObject())).setChecked(checked);
    }
  }

  private void adjustParentForChildrenValues(DefaultMutableTreeNode parent) {
    boolean foundCheck = false;
    for (Enumeration<TreeNode> e = parent.children(); !foundCheck && e.hasMoreElements(); ) {
      DefaultMutableTreeNode child = (DefaultMutableTreeNode) (e.nextElement());
      foundCheck = (((CheckBoxNodeData) (child.getUserObject())).isChecked());
    }
    ((CheckBoxNodeData) parent.getUserObject()).setChecked(foundCheck);
  }

  private void adjustParentsInTree(DefaultMutableTreeNode root) {
    for (Enumeration<TreeNode> e = root.postorderEnumeration(); e.hasMoreElements();) {
      final var node = (DefaultMutableTreeNode) (e.nextElement());
      if (!node.isLeaf()) {
        adjustParentForChildrenValues(node);
      }
    }
  }

  public void setCheckingPaths(TreePath[] paths) {
    for (TreePath p : paths) {
      DefaultMutableTreeNode treeNode = (DefaultMutableTreeNode) (p.getLastPathComponent());
      ((CheckBoxNodeData) (treeNode.getUserObject())).setChecked(true);
    }
    adjustParentsInTree((DefaultMutableTreeNode) getModel().getRoot());
  }

  public TreePath[] getCheckingPaths() {
    ArrayList<TreePath> paths = new ArrayList<>();
    DefaultMutableTreeNode root = (DefaultMutableTreeNode) getModel().getRoot();
    for (final var e = root.preorderEnumeration(); e.hasMoreElements();) {
      final var n = (DefaultMutableTreeNode) (e.nextElement());
      if (((CheckBoxNodeData) (n.getUserObject())).isChecked()) {
        paths.add(new TreePath(n.getPath()));
      }
    }
    return paths.toArray(new TreePath[0]);
  }

  public boolean isPathChecked(TreePath path) {
    final var n = (DefaultMutableTreeNode) (path.getLastPathComponent());
    return ((CheckBoxNodeData) (n.getUserObject())).isChecked();
  }
}
