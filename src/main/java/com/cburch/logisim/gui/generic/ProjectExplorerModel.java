/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

/*
 * Code taken from Cornell's version of Logisim: http://www.cs.cornell.edu/courses/cs3410/2015sp/
 */

package com.cburch.logisim.gui.generic;

import com.cburch.logisim.file.LogisimFile;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.proj.ProjectEvent;
import com.cburch.logisim.proj.ProjectListener;
import com.cburch.logisim.tools.Tool;

import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;

class ProjectExplorerModel extends DefaultTreeModel implements ProjectListener {

  private static final long serialVersionUID = 1L;
  private final JTree uiElement;
  private Project proj;
  private final boolean showMouseTools;

  ProjectExplorerModel(Project proj, JTree gui, boolean showMouseTools) {
    super(null);
    this.proj = proj;
    this.showMouseTools = showMouseTools;
    setRoot(new ProjectExplorerLibraryNode(this, proj.getLogisimFile(), gui, showMouseTools));
    proj.addProjectListener(this);
    uiElement = gui;
  }

  Node<Tool> findTool(Tool tool) {
    final var root = (Node<?>) getRoot();
    if (root == null || tool == null) return null;
    final var en = root.depthFirstEnumeration();
    while (en.hasMoreElements()) {
      final var node = (Node<?>) en.nextElement();
      if (node.getValue() == tool) {
        @SuppressWarnings("unchecked")
        final Node<Tool> nodeTool = (Node<Tool>) node;
        return nodeTool;
      }
    }
    return null;
  }

  void fireStructureChanged() {
    final var model = this;
    final var root = (Node<?>) getRoot();
    SwingUtilities.invokeLater(
        () -> {
          if (root != null) {
            model.fireTreeNodesChanged(model, root.getUserObjectPath(), null, null);
            model.fireTreeStructureChanged(model, root.getUserObjectPath(), null, null);
          } else {
            model.fireTreeNodesChanged(model, null, null, null);
            model.fireTreeStructureChanged(model, null, null, null);
          }
        });
  }

  // ProjectListener methods
  @Override
  public void projectChanged(ProjectEvent event) {
    final var act = event.getAction();
    if (act == ProjectEvent.ACTION_SET_FILE) {
      setLogisimFile(proj.getLogisimFile());
      fireStructureChanged();
    }
  }

  private void setLogisimFile(LogisimFile file) {
    final var oldRoot = (Node<?>) getRoot();
    oldRoot.decommission();
    setRoot(
        (file == null)
            ? null
            : new ProjectExplorerLibraryNode(this, file, uiElement, showMouseTools));
    fireStructureChanged();
  }

  public void setProject(Project value) {
    final var old = proj;

    if (old != null) {
      old.removeProjectListener(this);
    }

    setLogisimFile(null);
    proj = value;

    if (value != null) {
      value.addProjectListener(this);
      setLogisimFile(value.getLogisimFile());
    }
    fireStructureChanged();
  }

  public void updateStructure() {
    fireStructureChanged();
  }

  abstract static class Node<T> extends DefaultMutableTreeNode {

    private static final long serialVersionUID = 1L;
    final ProjectExplorerModel model;
    int oldIndex;
    int newIndex;

    Node(ProjectExplorerModel model, T userObject) {
      super(userObject);
      this.model = model;
    }

    abstract Node<T> create(T userObject);

    abstract void decommission();

    public void fireNodeChanged() {
      final var parent = (Node<?>) this.getParent();

      if (parent == null) {
        model.fireTreeNodesChanged(this, null, null, null);
      } else {
        final var indices = new int[] {parent.getIndex(this)};
        final var items = new Object[] {this.getUserObject()};
        model.fireTreeNodesChanged(this, parent.getPath(), indices, items);
      }
    }

    void fireNodesChanged(int[] indices, Node<?>[] children) {
      model.fireTreeNodesChanged(model, this.getPath(), indices, children);
    }

    void fireNodesInserted(int[] indices, Node<?>[] children) {
      model.fireTreeNodesInserted(model, this.getPath(), indices, children);
    }

    void fireNodesRemoved(int[] indices, Node<?>[] children) {
      model.fireTreeNodesRemoved(model, this.getPath(), indices, children);
    }

    void fireStructureChanged() {
      model.fireStructureChanged();
    }

    ProjectExplorerModel getModel() {
      return model;
    }

    public T getValue() {
      @SuppressWarnings("unchecked")
      T val = (T) getUserObject();
      return val;
    }
  }
}
