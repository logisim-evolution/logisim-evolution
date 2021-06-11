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

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.List;

public abstract class Library {

  /**
   * Unique identifier of the library, used as reference in project files.
   * Do NOT change as it will prevent project files from loading.
   *
   * Identifier value must MUST be unique string among all libraries.
   */
  public static final String _ID = null;

  private boolean hidden = false;

  public boolean contains(ComponentFactory query) {
    return indexOf(query) >= 0;
  }

  public boolean containsFromSource(Tool query) {
    for (Tool tool : getTools()) {
      if (tool.sharesSource(query)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Returns the name of the library that the user will see.
   */
  public String getDisplayName() {
    return getName();
  }

  /**
   * Returns unique library identifier.
   *
   * As we want to have static _ID per library, generic
   * implementation must look for it in the current instance
   */
  public String getName() {
    try {
      Field[] fields = getClass().getDeclaredFields();
      for (int i = 0; i < fields.length; i++) {
        if (fields[i].getName().equals("_ID")) {
          return (String)fields[i].get(this);
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
    return null;
  }

  public List<Library> getLibraries() {
    return Collections.emptyList();
  }

  public Library getLibrary(String name) {
    for (Library lib : getLibraries()) {
      if (name.equals(lib.getName())) {
        return lib;
      }
    }
    return null;
  }

  public boolean removeLibrary(String Name) {
    return false;
  }

  public Tool getTool(String name) {
    for (Tool tool : getTools()) {
      if (tool.getName().equals(name)) {
        return tool;
      }
    }
    return null;
  }

  /**
   * Returns a list of all the tools available in this library.
   */
  public abstract List<? extends Tool> getTools();

  public int indexOf(ComponentFactory query) {
    int index = -1;
    for (Tool obj : getTools()) {
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
