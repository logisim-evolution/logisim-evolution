/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.gui.generic;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.fail;

import com.cburch.logisim.file.Loader;
import com.cburch.logisim.file.LogisimFile;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.std.wiring.Pin;
import com.cburch.logisim.tools.AddTool;
import com.cburch.logisim.tools.Library;
import com.cburch.logisim.tools.Tool;
import java.util.Enumeration;
import java.util.List;
import javax.swing.tree.TreePath;
import org.junit.jupiter.api.Test;

class ProjectExplorerTest {

  @Test
  void selectsToolsFromNestedLibraries() {
    final var leafTool = new AddTool(Pin.FACTORY);
    final var leafLibrary = new TestLibrary("leaf", List.of(leafTool), List.of());
    final var parentLibrary = new TestLibrary("parent", List.of(), List.of(leafLibrary));
    final var file = LogisimFile.createNew(new Loader(null), null);
    file.addLibrary(parentLibrary);

    try {
      final var project = new Project(file);
      final var explorer = new ProjectExplorer(project, false);
      final var root = (ProjectExplorerLibraryNode) explorer.getModel().getRoot();
      final var parentNode = findLibraryNode(root, parentLibrary);
      final var leafNode = findLibraryNode(parentNode, leafLibrary);
      final var toolNode = findToolNode(leafNode, leafTool);

      explorer.setSelectionPath(new TreePath(new Object[] {root, parentNode, leafNode, toolNode}));

      assertSame(leafTool, explorer.getSelectedTool());
    } finally {
      file.stopAutosaveThread(false);
    }
  }

  private static ProjectExplorerLibraryNode findLibraryNode(
      ProjectExplorerLibraryNode parent, Library library) {
    for (final var children = parent.children(); children.hasMoreElements(); ) {
      final var child = children.nextElement();
      if (child instanceof ProjectExplorerLibraryNode node && node.getValue() == library) {
        return node;
      }
    }
    return fail("library node not found: " + library.getName());
  }

  private static ProjectExplorerToolNode findToolNode(ProjectExplorerLibraryNode parent, Tool tool) {
    for (final Enumeration<?> children = parent.children(); children.hasMoreElements(); ) {
      final var child = children.nextElement();
      if (child instanceof ProjectExplorerToolNode node && node.getValue() == tool) {
        return node;
      }
    }
    return fail("tool node not found: " + tool.getName());
  }

  private static class TestLibrary extends Library {
    private final String name;
    private final List<Tool> tools;
    private final List<Library> libraries;

    TestLibrary(String name, List<Tool> tools, List<Library> libraries) {
      this.name = name;
      this.tools = tools;
      this.libraries = libraries;
    }

    @Override
    public String getName() {
      return name;
    }

    @Override
    public List<Library> getLibraries() {
      return libraries;
    }

    @Override
    public List<? extends Tool> getTools() {
      return tools;
    }
  }
}
