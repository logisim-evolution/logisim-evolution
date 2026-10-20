/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.gui.menu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.cburch.logisim.file.Loader;
import com.cburch.logisim.file.LogisimFile;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.tools.Library;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import org.junit.jupiter.api.Test;

class ProjectLibraryActionsTest {
  @Test
  void availableBuiltinLibrariesExcludeAlreadyLoadedBuiltins() {
    final var file = newFile();
    final var builtins = file.getLoader().getBuiltin().getLibraries();
    file.addLibrary(builtins.get(0));
    file.addLibrary(builtins.get(1));

    final var available = ProjectLibraryActions.availableBuiltinLibraries(file);

    assertFalse(available.contains(builtins.get(0)));
    assertFalse(available.contains(builtins.get(1)));
    assertTrue(available.contains(builtins.get(2)));
  }

  @Test
  void populateBuiltinLibraryMenuListsLoadableLibrariesDirectly() {
    final var file = newFile();
    final var project = new Project(file);
    final var menu = new JMenu();

    ProjectLibraryActions.populateBuiltinLibraryMenu(menu, project);

    assertEquals(file.getLoader().getBuiltin().getLibraries().size(), menu.getItemCount());
    assertEquals(
        file.getLoader().getBuiltin().getLibraries().get(0).getDisplayName(),
        menu.getItem(0).getText());
  }

  @Test
  void clickingBuiltinLibraryMenuItemLoadsThatLibrary() {
    final var file = newFile();
    final var project = new Project(file);
    final var library = file.getLoader().getBuiltin().getLibraries().get(0);
    final var menu = new JMenu();

    ProjectLibraryActions.populateBuiltinLibraryMenu(menu, project);
    menu.getItem(0).doClick();

    assertTrue(file.getLibraries().contains(library));
  }

  @Test
  void populateBuiltinLibraryMenuShowsDisabledMessageWhenNoBuiltinsCanBeLoaded() {
    final var file = newFile();
    for (final Library lib : file.getLoader().getBuiltin().getLibraries()) {
      file.addLibrary(lib);
    }
    final var project = new Project(file);
    final var menu = new JMenu();

    ProjectLibraryActions.populateBuiltinLibraryMenu(menu, project);

    assertEquals(1, menu.getItemCount());
    final JMenuItem item = menu.getItem(0);
    assertFalse(item.isEnabled());
  }

  private static LogisimFile newFile() {
    return LogisimFile.createNew(new Loader(null), null);
  }
}
