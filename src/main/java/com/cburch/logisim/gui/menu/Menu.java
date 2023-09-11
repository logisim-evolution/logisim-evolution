/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.gui.menu;

import javax.swing.JMenu;

public abstract class Menu extends JMenu {
  private static final long serialVersionUID = 1L;

  protected abstract void computeEnabled();

  public abstract void hotkeyUpdate();
}
