/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.util;

import java.awt.Frame;
import java.awt.event.ActionEvent;
import javax.swing.JFrame;
import javax.swing.JRadioButtonMenuItem;

class WindowMenuItem extends JRadioButtonMenuItem {
  private static final long serialVersionUID = 1L;
  private final WindowMenuItemManager manager;

  WindowMenuItem(WindowMenuItemManager manager) {
    this.manager = manager;
    setText(manager.getText());
    setSelected(WindowMenuManager.getCurrentManager() == manager);
  }

  public void actionPerformed(ActionEvent event) {
    final var frame = getJFrame();
    frame.setExtendedState(Frame.NORMAL);
    frame.setVisible(true);
    frame.toFront();
  }

  public JFrame getJFrame() {
    return manager.getJFrame(true, null);
  }
}
