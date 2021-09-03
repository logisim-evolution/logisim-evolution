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

public class MenuListener {
  protected final LogisimMenuBar menubar;
  protected final ArrayList<EnabledListener> listeners;
  protected final EditListener editListener = new EditListener();

  public MenuListener(LogisimMenuBar menubar) {
    this.menubar = menubar;
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
    menubar.doAction(item);
  }

  public LogisimMenuBar getMenuBar() {
    return menubar;
  }

  public boolean isEnabled(LogisimMenuItem item) {
    return menubar.isEnabled(item);
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
        menubar.setEnabled(action, value);
        fireEnableChanged();
      }
    }

    public void register() {
      for (final var item : LogisimMenuBar.EDIT_ITEMS) {
        menubar.addActionListener(item, this);
      }
      computeEnabled();
    }

    public void computeEnabled() {
      if (handler != null) {
        handler.computeEnabled();
      } else {
        for (final var item : LogisimMenuBar.EDIT_ITEMS) {
          menubar.setEnabled(item, false);
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
