/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.gui.generic;

import com.cburch.logisim.file.LibraryEvent;
import com.cburch.logisim.file.LibraryListener;
import com.cburch.logisim.file.LogisimFile;
import com.cburch.logisim.std.base.BaseLibrary;
import com.cburch.logisim.tools.Library;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.JTree;

/**
 * Code taken from Cornell's version of Logisim: http://www.cs.cornell.edu/courses/cs3410/2015sp/
 */
public class ProjectExplorerLibraryNode extends ProjectExplorerModel.Node<Library>
    implements LibraryListener {

  private static final long serialVersionUID = 1L;
  private LogisimFile file;
  private JTree guiElement = null;
  private final boolean showMouseTools;

  ProjectExplorerLibraryNode(ProjectExplorerModel model, Library lib, JTree gui, boolean showMouseTools) {
    super(model, lib);
    guiElement = gui;
    if (lib instanceof LogisimFile) {
      file = (LogisimFile) lib;
      file.addLibraryListener(this);
    }
    this.showMouseTools = showMouseTools;
    buildChildren();
  }

  private void buildChildren() {
    final var lib = getValue();
    if (lib != null) {
      final var showLib = (showMouseTools & lib instanceof BaseLibrary) || !lib.isHidden();
      if (showLib) {
        buildChildren(new ProjectExplorerToolNode(getModel(), null), lib.getTools(), 0);
        buildChildren(
            new ProjectExplorerLibraryNode(getModel(), null, guiElement, showMouseTools),
            lib.getLibraries(),
            lib.getTools().size());
      }
    }
  }

  private <T> void buildChildren(ProjectExplorerModel.Node<T> factory, List<? extends T> items, int startIndex) {
    // go through previously built children
    Map<T, ProjectExplorerModel.Node<T>> nodeMap = new HashMap<>();
    List<ProjectExplorerModel.Node<T>> nodeList = new ArrayList<>();
    var oldPos = startIndex;

    for (Enumeration<?> en = children(); en.hasMoreElements(); ) {
      final var baseNode = en.nextElement();
      if (baseNode.getClass() == factory.getClass()) {
        @SuppressWarnings("unchecked")
        final var node = (ProjectExplorerModel.Node<T>) baseNode;
        nodeMap.put(node.getValue(), node);
        nodeList.add(node);
        node.oldIndex = oldPos;
        node.newIndex = -1;
        oldPos++;
      }
    }

    var oldCount = oldPos;

    // go through what should be the children
    var actualPos = startIndex;
    var insertionCount = 0;
    oldPos = startIndex;

    for (T tool : items) {
      if (tool instanceof Library && ((Library) tool).isHidden()) {
        if (!showMouseTools || !(tool instanceof BaseLibrary)) continue;
      }
      ProjectExplorerModel.Node<T> node = nodeMap.get(tool);

      if (node == null) {
        node = factory.create(tool);
        node.oldIndex = -1;
        node.newIndex = actualPos;
        nodeList.add(node);
        insertionCount++;
      } else {
        node.newIndex = oldPos;
        oldPos++;
      }
      actualPos++;
    }

    // identify removals first
    if (oldPos != oldCount) {
      final var delIndex = new int[oldCount - oldPos];
      final var delNodes = new ProjectExplorerModel.Node<?>[delIndex.length];
      var delPos = 0;

      for (var i = nodeList.size() - 1; i >= 0; i--) {
        final var node = nodeList.get(i);
        if (node.newIndex < 0) {
          node.decommission();
          remove(node.oldIndex);
          nodeList.remove(node.oldIndex - startIndex);

          for (final var other : nodeList) {
            if (other.oldIndex > node.oldIndex) {
              other.oldIndex--;
            }
          }
          delIndex[delPos] = node.oldIndex;
          delNodes[delPos] = node;
          delPos++;
        }
      }
      this.fireNodesRemoved(delIndex, delNodes);
    }

    // identify moved nodes
    var minChange = Integer.MAX_VALUE >> 3;
    var maxChange = Integer.MIN_VALUE >> 3;

    for (ProjectExplorerModel.Node<T> node : nodeList) {
      if (node.newIndex != node.oldIndex && node.oldIndex >= 0) {
        minChange = Math.min(minChange, node.oldIndex);
        maxChange = Math.max(maxChange, node.oldIndex);
      }
    }
    if (minChange <= maxChange) {
      final var moveIndex = new int[maxChange - minChange + 1];
      final var moveNodes = new ProjectExplorerModel.Node<?>[moveIndex.length];

      for (var i = maxChange; i >= minChange; i--) {
        final var node = nodeList.get(i);
        moveIndex[node.newIndex - minChange] = node.newIndex;
        moveNodes[node.newIndex - minChange] = node;
        remove(i);
      }

      for (var i = 0; i < moveIndex.length; i++) {
        insert(moveNodes[i], moveIndex[i]);
      }

      this.fireNodesChanged(moveIndex, moveNodes);
    }

    // identify inserted nodes
    if (insertionCount > 0) {
      final var insIndex = new int[insertionCount];
      final var insNodes = new ProjectExplorerModel.Node<?>[insertionCount];
      var insertionsPos = 0;

      for (final var node : nodeList) {
        if (node.oldIndex < 0) {
          insert(node, node.newIndex);
          insIndex[insertionsPos] = node.newIndex;
          insNodes[insertionsPos] = node;
          insertionsPos++;
        }
      }
      this.fireNodesInserted(insIndex, insNodes);
    }
  }

  @Override
  ProjectExplorerLibraryNode create(Library userObject) {
    return new ProjectExplorerLibraryNode(getModel(), userObject, guiElement, showMouseTools);
  }

  @Override
  void decommission() {
    if (file != null) {
      file.removeLibraryListener(this);
    }
    for (final var en = children(); en.hasMoreElements(); ) {
      final var n = en.nextElement();
      if (n instanceof ProjectExplorerModel.Node<?> node) {
        node.decommission();
      }
    }
  }

  @Override
  public void libraryChanged(LibraryEvent event) {
    switch (event.getAction()) {
      case LibraryEvent.DIRTY_STATE:
      case LibraryEvent.SET_NAME:
        this.fireNodeChanged();
        break;
      case LibraryEvent.SET_MAIN:
        break;
      case LibraryEvent.ADD_TOOL:
      case LibraryEvent.REMOVE_TOOL:
      case LibraryEvent.MOVE_TOOL:
      case LibraryEvent.ADD_LIBRARY:
      case LibraryEvent.REMOVE_LIBRARY:
        buildChildren();
        break;
      default:
        fireStructureChanged();
    }
  }
}
