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
import lombok.Getter;

public class MenuListener {
  @Getter protected final LogisimMenuBar menuBar;
  protected final ArrayList<EnabledListener> listeners;
  protected final EditListener editListener = new EditListener();

  public MenuListener(LogisimMenuBar menuBar) {
    this.menuBar = menuBar;
    this.listeners = new ArrayList<>();
  }

  public void addEnabledListener(EnabledListener listener) {
    listeners.add(listener);
  }

  public void removeEnabledListener(EnabledListener listener) {
    listeners.remove(listener);
  }

  protected void fireEnableChanged() {
    for (EnabledListener listener : listeners) {
      listener.menuEnableChanged(this);
    }
  }

  public void setEditHandler(EditHandler handler) {
    editListener.setHandler(handler);
  }

  public void doAction(LogisimMenuItem item) {
    menuBar.doAction(item);
  }

  public boolean isEnabled(LogisimMenuItem item) {
    return menuBar.isEnabled(item);
  }

  public interface EnabledListener {
    void menuEnableChanged(MenuListener source);
  }

  protected class EditListener implements ActionListener, EditHandler.Listener {
    private EditHandler handler = null;

    @Override
    public void actionPerformed(ActionEvent e) {
      if (handler != null) handler.actionPerformed(e);
    }

    @Override
    public void enableChanged(EditHandler handler, LogisimMenuItem action, boolean value) {
      if (handler == this.handler) {
        menuBar.setEnabled(action, value);
        fireEnableChanged();
      }
    }

    public void register() {
      for (final var item : LogisimMenuBar.EDIT_ITEMS) {
        menuBar.addActionListener(item, this);
      }
      computeEnabled();
    }

    public void computeEnabled() {
      if (handler != null) {
        handler.computeEnabled();
      } else {
        for (final var item : LogisimMenuBar.EDIT_ITEMS) {
          menuBar.setEnabled(item, false);
        }
      }
    }

    private void setHandler(EditHandler value) {
      handler = value;
      if (handler != null) handler.setListener(this);
      computeEnabled();
    }
  }
}
