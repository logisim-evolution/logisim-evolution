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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;

class ProjectExplorerModel extends DefaultTreeModel implements ProjectListener {

  private static final long serialVersionUID = 1L;
  private final JTree uiElement;
  private Project proj;
  private final boolean showMouseTools;
  /**
   * Lowercased words the tree is narrowed down to, all of which have to be found in a name for it
   * to match. Empty when no filtering takes place.
   */
  private List<String> filterWords = List.of();
  /**
   * Children to be shown for each node while filtering, {@code null} when no filter is set. The
   * node tree itself is never touched by the filter: only what the model reports gets narrowed.
   */
  private Map<TreeNode, List<TreeNode>> visibleChildren = null;

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

  /**
   * Narrows the tree down to the nodes matching filter, i.e. the ones whose name contains it,
   * together with the libraries leading to them. Text is split into words and a name has to
   * contain all of them, in any order, to be considered a match: "foo bar" finds both
   * "foo bar", "bar foo", "foo else bar" and "rabarbar foobar". A library matching by its own
   * name keeps all of its content visible. Passing empty string turns the filter off.
   */
  void setFilter(String text) {
    final var newWords = splitIntoWords(text);
    if (newWords.equals(filterWords)) return;
    filterWords = newWords;
    rebuildVisibleNodes();

    final var root = (TreeNode) getRoot();
    if (root == null) {
      fireStructureChanged();
    } else {
      nodeStructureChanged(root);
    }
  }

  boolean isFiltering() {
    return visibleChildren != null;
  }

  /** Returns the lowercased words currently filtered by, empty list when no filter is set. */
  List<String> getFilterWords() {
    return filterWords;
  }

  private static List<String> splitIntoWords(String text) {
    final var words = new ArrayList<String>();
    for (final var word : text.toLowerCase().split("\\s+")) {
      if (!word.isEmpty()) words.add(word);
    }
    return words;
  }

  private void rebuildVisibleNodes() {
    if (filterWords.isEmpty()) {
      visibleChildren = null;
      return;
    }
    visibleChildren = new HashMap<>();
    final var root = (TreeNode) getRoot();
    if (root != null) collectVisibleNodes(root, true);
  }

  /**
   * Fills {@link #visibleChildren} for given node and tells whether the node is to be shown, which
   * is the case when its own name matches the filter or when any of its descendants does.
   */
  private boolean collectVisibleNodes(TreeNode node, boolean isRoot) {
    // Root node stands for the project file itself, so matching its
    // name must not unfold all of it.
    final var selfMatch = !isRoot && nameMatchesFilter(node);
    final var visible = new ArrayList<TreeNode>();
    for (var i = 0; i < node.getChildCount(); i++) {
      final var child = node.getChildAt(i);
      if (collectVisibleNodes(child, false) || selfMatch) visible.add(child);
    }
    visibleChildren.put(node, visible);

    return selfMatch || !visible.isEmpty();
  }

  /**
   * Tells whether given name matches the filter on its own, i.e. contains all of its words. Note
   * that an element can well be shown while its name does not match, when what matched is its
   * parent or any of its children (i.e. library name because its content matches).
   */
  boolean matchesFilter(String name) {
    if (name == null) return false;

    final var lowercased = name.toLowerCase();
    for (final var word : filterWords) {
      if (!lowercased.contains(word)) return false;
    }
    return true;
  }

  private boolean nameMatchesFilter(TreeNode node) {
    return matchesFilter(getDisplayName(node));
  }

  private static String getDisplayName(TreeNode node) {
    if (node instanceof ProjectExplorerToolNode toolNode) {
      final var tool = toolNode.getValue();
      return (tool == null) ? null : tool.getDisplayName();
    }
    if (node instanceof ProjectExplorerLibraryNode libNode) {
      final var lib = libNode.getValue();
      return (lib == null) ? null : lib.getDisplayName();
    }
    return null;
  }

  private List<TreeNode> getVisibleChildren(Object parent) {
    return (visibleChildren == null) ? null : visibleChildren.get(parent);
  }

  @Override
  public Object getChild(Object parent, int index) {
    final var visible = getVisibleChildren(parent);
    return (visible == null) ? super.getChild(parent, index) : visible.get(index);
  }

  @Override
  public int getChildCount(Object parent) {
    final var visible = getVisibleChildren(parent);
    return (visible == null) ? super.getChildCount(parent) : visible.size();
  }

  @Override
  public int getIndexOfChild(Object parent, Object child) {
    final var visible = getVisibleChildren(parent);
    return (visible == null) ? super.getIndexOfChild(parent, child) : visible.indexOf(child);
  }

  @Override
  public boolean isLeaf(Object node) {
    // While filtering, a library with no matching content must not offer an empty expand handle.
    return isFiltering() ? (getChildCount(node) == 0) : super.isLeaf(node);
  }

  void fireStructureChanged() {
    final var model = this;
    // Nodes may have come or gone since the filter was built, so it has to be refreshed as well.
    rebuildVisibleNodes();
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
        // Reading the model, not the parent node, as filtering shifts the indices.
        final var index = model.getIndexOfChild(parent, this);
        if (index < 0) {
          // filtered out so nothing to repaint
          return;
        }
        final var indices = new int[] {index};
        final var items = new Object[] {this.getUserObject()};
        model.fireTreeNodesChanged(this, parent.getPath(), indices, items);
      }
    }

    void fireNodesChanged(int[] indices, Node<?>[] children) {
      // Indices are computed against the unfiltered children, so the whole structure
      // has to be refreshed instead while filtering.
      if (model.isFiltering()) {
        model.fireStructureChanged();
        return;
      }
      model.fireTreeNodesChanged(model, this.getPath(), indices, children);
    }

    void fireNodesInserted(int[] indices, Node<?>[] children) {
      if (model.isFiltering()) {
        model.fireStructureChanged();
        return;
      }
      model.fireTreeNodesInserted(model, this.getPath(), indices, children);
    }

    void fireNodesRemoved(int[] indices, Node<?>[] children) {
      if (model.isFiltering()) {
        model.fireStructureChanged();
        return;
      }
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
