package com.cburch.logisim.gui.appear;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseEvent;
import java.util.EventObject;
import java.util.ArrayList;

import javax.swing.AbstractCellEditor;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.UIManager;
import javax.swing.event.ChangeEvent;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreeCellEditor;
import javax.swing.tree.TreeCellRenderer;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeNode;
import java.util.Enumeration;

import org.scijava.swing.checkboxtree.CheckBoxNodeData;
import org.scijava.swing.checkboxtree.CheckBoxNodeEditor;
import org.scijava.swing.checkboxtree.CheckBoxNodePanel;
import org.scijava.swing.checkboxtree.CheckBoxNodeRenderer;

public class CheckBoxTree extends JTree {

  public CheckBoxTree(DefaultMutableTreeNode root) {
    super(root);

    CheckBoxNodeRenderer renderer = new CheckBoxNodeRenderer();
    this.setCellRenderer(renderer);

    this.setCellEditor(new CheckBoxNodeEditor(this));
    this.setEditable(true);

    // printTree(root);

  }

  void setCheckingPaths(TreePath[] paths) {
    for (TreePath p : paths) {
      DefaultMutableTreeNode treeNode = (DefaultMutableTreeNode)(p.getLastPathComponent());
      CheckBoxNodeData d = (CheckBoxNodeData)(treeNode.getUserObject());
      d.setChecked(true);
    }
  }

  TreePath[] getCheckingPaths() {
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

  boolean isPathChecked(TreePath path) {
    DefaultMutableTreeNode n = (DefaultMutableTreeNode)(path.getLastPathComponent());
    CheckBoxNodeData d = (CheckBoxNodeData)(n.getUserObject());
    return d.isChecked();
  }

  void printTree(DefaultMutableTreeNode node) {
    for (Enumeration<TreeNode> e = node.preorderEnumeration(); e.hasMoreElements();) {
      DefaultMutableTreeNode n = (DefaultMutableTreeNode)(e.nextElement());
      int level = n.getLevel();
      for (int i=0; i<level; i++) {
        System.out.print(" ");
      }
      System.out.println(n);
    }
  }
}

