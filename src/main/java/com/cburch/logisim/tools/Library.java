/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.tools;

import com.cburch.logisim.comp.ComponentFactory;
import com.cburch.logisim.util.LibraryUtil;
import java.util.Collections;
import java.util.List;

public abstract class Library {
  private boolean hidden = false;

  public boolean contains(ComponentFactory query) {
    return indexOf(query) >= 0;
  }

  public boolean containsFromSource(Tool query) {
    for (final var tool : getTools()) {
      if (tool.sharesSource(query)) {
        return true;
      }
    }
    return false;
  }

  public String getDisplayName() {
    return getName();
  }

  /**
   * Returns unique library identifier as specified in library static const "_ID" attribute.
   */
  public String getName() {
    return LibraryUtil.getName(getClass());
  }

  public List<Library> getLibraries() {
    return Collections.emptyList();
  }

  public Library getLibrary(String name) {
    for (final var lib : getLibraries()) {
      if (lib.getName().equals(name)) {
        return lib;
      }
    }
    return null;
  }

  public boolean removeLibrary(String name) {
    return false;
  }

  public Tool getTool(String name) {
    for (final var tool : getTools()) {
      if (tool.getName().equals(name)) {
        return tool;
      }
    }
    return null;
  }

  public abstract List<? extends Tool> getTools();

  public int indexOf(ComponentFactory query) {
    int index = -1;
    for (final var obj : getTools()) {
      index++;
      if (obj instanceof AddTool tool) {
        if (tool.getFactory() == query) return index;
      }
    }
    return -1;
  }

  public boolean isDirty() {
    return false;
  }

  public boolean isHidden() {
    return hidden;
  }

  public void setHidden() {
    hidden = true;
  }

  @Override
  public String toString() {
    return getName();
  }
}
