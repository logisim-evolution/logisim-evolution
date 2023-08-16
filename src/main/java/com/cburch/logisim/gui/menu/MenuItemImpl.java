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
import javax.swing.JMenuItem;

public class MenuItemImpl extends JMenuItem implements MenuItem {

  private static final long serialVersionUID = 1L;
  private final MenuItemHelper helper;

  public MenuItemImpl(Menu menu, LogisimMenuItem menuItem) {
    helper = new MenuItemHelper(this, menu, menuItem);
    super.addActionListener(helper);
    setEnabled(true);
  }

  @Override
  public void actionPerformed(ActionEvent event) {
    helper.actionPerformed(event);
  }

  @Override
  public void addActionListener(ActionListener l) {
    helper.addActionListener(l);
  }

  @Override
  public boolean hasListeners() {
    return helper.hasListeners();
  }

  @Override
  public void removeActionListener(ActionListener l) {
    helper.removeActionListener(l);
  }

  @Override
  public void setEnabled(boolean value) {
    helper.setEnabled(value);
    super.setEnabled(value && helper.hasListeners());
  }
}
