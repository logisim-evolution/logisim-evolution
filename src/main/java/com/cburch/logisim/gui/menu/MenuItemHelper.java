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
import java.util.ArrayList;
import javax.swing.JMenuItem;

class MenuItemHelper implements ActionListener {
  private final JMenuItem source;
  private final LogisimMenuItem menuItem;
  private final Menu menu;
  private final ArrayList<ActionListener> listeners;
  private boolean enabled;
  private boolean inActionListener;

  public MenuItemHelper(JMenuItem source, Menu menu, LogisimMenuItem menuItem) {
    this.source = source;
    this.menu = menu;
    this.menuItem = menuItem;
    this.enabled = true;
    this.inActionListener = false;
    this.listeners = new ArrayList<>();
  }

  @Override
  public void actionPerformed(ActionEvent event) {
    if (!listeners.isEmpty()) {
      final var e =
          new ActionEvent(
              menuItem,
              event.getID(),
              event.getActionCommand(),
              event.getWhen(),
              event.getModifiers());
      for (ActionListener l : listeners) {
        l.actionPerformed(e);
      }
    }
  }

  public void addActionListener(ActionListener l) {
    listeners.add(l);
    computeEnabled();
  }

  private void computeEnabled() {
    inActionListener = true;
    try {
      source.setEnabled(enabled);
      menu.computeEnabled();
    } finally {
      inActionListener = false;
    }
  }

  public boolean hasListeners() {
    return !listeners.isEmpty();
  }

  public void removeActionListener(ActionListener l) {
    listeners.remove(l);
    computeEnabled();
  }

  public void setEnabled(boolean value) {
    if (!inActionListener) {
      enabled = value;
    }
  }
}
