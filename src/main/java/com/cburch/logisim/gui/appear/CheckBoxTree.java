package com.cburch.logisim.gui.appear;

import java.util.ArrayList;
import java.util.Enumeration;

import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreeSelectionModel;
import javax.swing.event.TreeModelListener;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.ChangeEvent;

import org.scijava.swing.checkboxtree.CheckBoxNodeData;
import org.scijava.swing.checkboxtree.CheckBoxNodeEditor;
import org.scijava.swing.checkboxtree.CheckBoxNodeRenderer;

public class CheckBoxTree extends JTree {

  public CheckBoxTree(DefaultMutableTreeNode root) {
    super(root);
    CheckBoxNodeRenderer renderer = new CheckBoxNodeRenderer();
    this.setCellRenderer(renderer);
    this.setCellEditor(new CheckBoxNodeEditor(this));
    this.setEditable(true);
    getSelectionModel().setSelectionMode (TreeSelectionModel.SINGLE_TREE_SELECTION);

    getModel().addTreeModelListener(new TreeModelListener() {
      public void treeNodesChanged(TreeModelEvent e) {
        nodeModified(e.getTreePath(), e.getChildIndices());
      }
      public void treeStructureChanged(TreeModelEvent e) {
      }
      public void treeNodesRemoved(TreeModelEvent e) {
      }
      public void treeNodesInserted(TreeModelEvent e) {
      }
    });

  }

  private void nodeModified(TreePath parentPath, int[] changedChildren) {
    DefaultMutableTreeNode parent = (DefaultMutableTreeNode)parentPath.getLastPathComponent();
    DefaultMutableTreeNode node;
    if (changedChildren == null) {
      node = parent;
      parent = null;
    } else {
      node = (DefaultMutableTreeNode)parent.getChildAt(changedChildren[0]);
    }
    boolean checked = ((CheckBoxNodeData)node.getUserObject()).isChecked();
    markDescendents(node, checked);
    adjustAncestorsForChildrenValues(parent);
  }

  private void markDescendents(DefaultMutableTreeNode node, boolean checked) {
    for (Enumeration<TreeNode> e = node.depthFirstEnumeration(); e.hasMoreElements();) {
      ((CheckBoxNodeData)((DefaultMutableTreeNode)(e.nextElement())).getUserObject()).setChecked(checked);
    }
  }

  private void adjustAncestorsForChildrenValues(DefaultMutableTreeNode parent) {
    for (DefaultMutableTreeNode p=parent ; p != null; p = (DefaultMutableTreeNode)p.getParent()) {
      adjustParentForChildrenValues(p);
    }
  }

  private void adjustParentForChildrenValues(DefaultMutableTreeNode parent) {
      boolean foundCheck = false;
      for (Enumeration<TreeNode> e = parent.children(); !foundCheck && e.hasMoreElements();) {
        DefaultMutableTreeNode child = (DefaultMutableTreeNode)(e.nextElement());
        foundCheck = (((CheckBoxNodeData)(child.getUserObject())).isChecked());
      }
      ((CheckBoxNodeData)parent.getUserObject()).setChecked(foundCheck);
  }

  private void adjustParentsInTree(DefaultMutableTreeNode root) {
    for (Enumeration<TreeNode> e = root.postorderEnumeration(); e.hasMoreElements();) {
      DefaultMutableTreeNode node = (DefaultMutableTreeNode)(e.nextElement());
      if ( ! node.isLeaf()) {
        adjustParentForChildrenValues(node);
      }
    }
  }

  public void setCheckingPaths(TreePath[] paths) {
    for (TreePath p : paths) {
      DefaultMutableTreeNode treeNode = (DefaultMutableTreeNode)(p.getLastPathComponent());
      ((CheckBoxNodeData)(treeNode.getUserObject())).setChecked(true);
    }
    adjustParentsInTree((DefaultMutableTreeNode)getModel().getRoot());
  }

  public TreePath[] getCheckingPaths() {
    ArrayList<TreePath> paths = new ArrayList<>();
    DefaultMutableTreeNode root = (DefaultMutableTreeNode) getModel().getRoot();
    for (Enumeration<TreeNode> e = root.preorderEnumeration(); e.hasMoreElements();) {
      DefaultMutableTreeNode n = (DefaultMutableTreeNode)(e.nextElement());
      CheckBoxNodeData d = (CheckBoxNodeData)(n.getUserObject());
      if (d.isChecked()) {
        paths.add(new TreePath(n.getPath()));
      }
    }
    return paths.toArray(new TreePath[0]);
  }

  public boolean isPathChecked(TreePath path) {
    DefaultMutableTreeNode n = (DefaultMutableTreeNode)(path.getLastPathComponent());
    return ((CheckBoxNodeData)(n.getUserObject())).isChecked();
  }
}
