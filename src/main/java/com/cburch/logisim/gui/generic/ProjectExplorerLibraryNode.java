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
import com.cburch.logisim.tools.AddTool;
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

  ProjectExplorerLibraryNode(
      ProjectExplorerModel model, Library lib, JTree gui, boolean showMouseTools) {
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
    Library lib = getValue();
    if (lib != null) {
      boolean showLib = (showMouseTools & lib instanceof BaseLibrary) || !lib.isHidden();
      if (showLib) {
        buildChildren(new ProjectExplorerToolNode(getModel(), null), lib.getTools(), 0);
        buildChildren(
            new ProjectExplorerLibraryNode(getModel(), null, guiElement, showMouseTools),
            lib.getLibraries(),
            lib.getTools().size());
      }
    }
  }

  private <T> void buildChildren(
      ProjectExplorerModel.Node<T> factory, List<? extends T> items, int startIndex) {
    // go through previously built children
    Map<T, ProjectExplorerModel.Node<T>> nodeMap = new HashMap<>();
    List<ProjectExplorerModel.Node<T>> nodeList = new ArrayList<>();
    int oldPos = startIndex;

    for (Enumeration<?> en = children(); en.hasMoreElements(); ) {
      Object baseNode = en.nextElement();
      if (baseNode.getClass() == factory.getClass()) {
        @SuppressWarnings("unchecked")
        ProjectExplorerModel.Node<T> node = (ProjectExplorerModel.Node<T>) baseNode;
        nodeMap.put(node.getValue(), node);
        nodeList.add(node);
        node.oldIndex = oldPos;
        node.newIndex = -1;
        oldPos++;
      }
    }

    int oldCount = oldPos;

    // go through what should be the children
    int actualPos = startIndex;
    int insertionCount = 0;
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
      int[] delIndex = new int[oldCount - oldPos];
      ProjectExplorerModel.Node<?>[] delNodes = new ProjectExplorerModel.Node<?>[delIndex.length];
      int delPos = 0;

      for (int i = nodeList.size() - 1; i >= 0; i--) {
        ProjectExplorerModel.Node<T> node = nodeList.get(i);

        if (node.newIndex < 0) {
          node.decommission();
          remove(node.oldIndex);
          nodeList.remove(node.oldIndex - startIndex);

          for (ProjectExplorerModel.Node<T> other : nodeList) {
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
    int minChange = Integer.MAX_VALUE >> 3;
    int maxChange = Integer.MIN_VALUE >> 3;

    for (ProjectExplorerModel.Node<T> node : nodeList) {
      if (node.newIndex != node.oldIndex && node.oldIndex >= 0) {
        minChange = Math.min(minChange, node.oldIndex);
        maxChange = Math.max(maxChange, node.oldIndex);
      }
    }
    if (minChange <= maxChange) {
      int[] moveIndex = new int[maxChange - minChange + 1];
      ProjectExplorerModel.Node<?>[] moveNodes = new ProjectExplorerModel.Node<?>[moveIndex.length];

      for (int i = maxChange; i >= minChange; i--) {
        ProjectExplorerModel.Node<T> node = nodeList.get(i);
        moveIndex[node.newIndex - minChange] = node.newIndex;
        moveNodes[node.newIndex - minChange] = node;
        remove(i);
      }

      for (int i = 0; i < moveIndex.length; i++) {
        insert(moveNodes[i], moveIndex[i]);
      }

      this.fireNodesChanged(moveIndex, moveNodes);
    }

    // identify inserted nodes
    if (insertionCount > 0) {
      int[] insIndex = new int[insertionCount];
      ProjectExplorerModel.Node<?>[] insNodes = new ProjectExplorerModel.Node<?>[insertionCount];
      int insertionsPos = 0;

      for (ProjectExplorerModel.Node<T> node : nodeList) {
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
    for (Enumeration<?> en = children(); en.hasMoreElements(); ) {
      Object n = en.nextElement();
      if (n instanceof ProjectExplorerModel.Node<?>) {
        ((ProjectExplorerModel.Node<?>) n).decommission();
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
