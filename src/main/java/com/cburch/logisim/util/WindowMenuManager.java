/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.util;

import java.util.ArrayList;
import java.util.List;

public final class WindowMenuManager {

  private static final ArrayList<WindowMenu> menus = new ArrayList<>();

  private static final ArrayList<WindowMenuItemManager> managers = new ArrayList<>();

  private static WindowMenuItemManager currentManager = null;

  private WindowMenuManager() {
    throw new IllegalStateException("Utility class. No instantiation allowed.");
  }

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
    if (currentManager == null) setNullItems(false);
    else currentManager.setSelected(false);
    currentManager = value;
    if (currentManager == null) setNullItems(true);
    else currentManager.setSelected(true);
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
}
