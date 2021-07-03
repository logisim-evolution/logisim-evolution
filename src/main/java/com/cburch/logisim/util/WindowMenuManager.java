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

package com.cburch.logisim.util;

import java.util.ArrayList;
import java.util.List;

class WindowMenuManager {
  public static void addManager(WindowMenuItemManager manager) {
    for (final var menu : menus) {
      manager.createMenuItem(menu);
    }
    managers.add(manager);
  }

  public static void addMenu(WindowMenu menu) {
    for (final var manager : managers) {
      manager.createMenuItem(menu);
    }
    menus.add(menu);
  }

  private static void enableAll() {
    for (final var menu : menus) {
      menu.computeEnabled();
    }
  }

  static WindowMenuItemManager getCurrentManager() {
    return currentManager;
  }

  static List<WindowMenu> getMenus() {
    return menus;
  }

  // TODO frames should call removeMenu when they're destroyed

  public static void removeManager(WindowMenuItemManager manager) {
    for (final var menu : menus) {
      manager.removeMenuItem(menu);
    }
    managers.remove(manager);
  }

  static void setCurrentManager(WindowMenuItemManager value) {
    if (value == currentManager) return;

    final var doEnable = (currentManager == null) != (value == null);
    if (currentManager == null)
      setNullItems(false);
    else
      currentManager.setSelected(false);
    currentManager = value;
    if (currentManager == null)
      setNullItems(true);
    else
      currentManager.setSelected(true);
    if (doEnable) enableAll();
  }

  private static void setNullItems(boolean value) {
    for (final var menu : menus) {
      menu.setNullItemSelected(value);
    }
  }

  static void unsetCurrentManager(WindowMenuItemManager value) {
    if (value != currentManager) return;
    setCurrentManager(null);
  }

  private static final ArrayList<WindowMenu> menus = new ArrayList<>();

  private static final ArrayList<WindowMenuItemManager> managers = new ArrayList<>();

  private static WindowMenuItemManager currentManager = null;

  private WindowMenuManager() {}
}
