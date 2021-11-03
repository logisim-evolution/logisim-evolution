/*
 * Logisim-evolution - digital logic design tool and simulator
 * © 2001 Logisim-evolution contributors
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.gui.menu;

public class LogisimMenuItem {
  private final String name;

  LogisimMenuItem(String name) {
    this.name = name;
  }

  @Override
  public String toString() {
    return name;
  }
}
