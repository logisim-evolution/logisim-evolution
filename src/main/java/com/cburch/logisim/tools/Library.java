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

  public abstract boolean removeLibrary(String name);

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
      if (obj instanceof AddTool) {
        AddTool tool = (AddTool) obj;
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
