/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.gui.menu;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.KeyStroke;

interface MenuItem {
  void actionPerformed(ActionEvent event);

  void addActionListener(ActionListener l);

  boolean hasListeners();

  boolean isEnabled();

  void setEnabled(boolean value);

  void removeActionListener(ActionListener l);

  KeyStroke getAccelerator();
}
