/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.util;

import com.cburch.contracts.BaseWindowListenerContract;
import java.awt.event.WindowEvent;
import java.util.HashMap;
import javax.swing.JFrame;
import javax.swing.JRadioButtonMenuItem;

public abstract class WindowMenuItemManager {
  private class MyListener implements BaseWindowListenerContract {
    @Override
    public void windowActivated(WindowEvent event) {
      addToManager();
      WindowMenuManager.setCurrentManager(WindowMenuItemManager.this);
    }

    @Override
    public void windowClosed(WindowEvent event) {
      removeFromManager();
    }

    @Override
    public void windowClosing(WindowEvent event) {
      final var frame = getJFrame(false, null);
      if (frame.getDefaultCloseOperation() == JFrame.HIDE_ON_CLOSE) {
        removeFromManager();
      }
    }

    @Override
    public void windowDeactivated(WindowEvent event) {
      WindowMenuManager.unsetCurrentManager(WindowMenuItemManager.this);
    }

    @Override
    public void windowIconified(WindowEvent event) {
      addToManager();
      WindowMenuManager.setCurrentManager(WindowMenuItemManager.this);
    }
  }

  private final MyListener myListener = new MyListener();
  private String text;
  private final boolean persistent;
  private boolean listenerAdded = false;
  private boolean inManager = false;
  private final HashMap<WindowMenu, JRadioButtonMenuItem> menuItems = new HashMap<>();

  public WindowMenuItemManager(String text, boolean persistent) {
    this.text = text;
    this.persistent = persistent;
    if (persistent) {
      WindowMenuManager.addManager(this);
    }
  }

  private void addToManager() {
    if (!persistent && !inManager) {
      WindowMenuManager.addManager(this);
      inManager = true;
    }
  }

  void createMenuItem(WindowMenu menu) {
    final var ret = new WindowMenuItem(this);
    menuItems.put(menu, ret);
    menu.addMenuItem(this, ret, persistent);
  }

  public void frameClosed(JFrame frame) {
    if (!persistent) {
      if (listenerAdded) {
        frame.removeWindowListener(myListener);
        listenerAdded = false;
      }
      removeFromManager();
    }
  }

  public void frameOpened(JFrame frame) {
    if (!listenerAdded) {
      frame.addWindowListener(myListener);
      listenerAdded = true;
    }
    addToManager();
    WindowMenuManager.setCurrentManager(this);
  }

  public abstract JFrame getJFrame(boolean create, java.awt.Component parent);

  JRadioButtonMenuItem getMenuItem(WindowMenu key) {
    return menuItems.get(key);
  }

  public String getText() {
    return text;
  }

  private void removeFromManager() {
    if (!persistent && inManager) {
      inManager = false;
      for (final var menu : WindowMenuManager.getMenus()) {
        final var menuItem = menuItems.get(menu);
        menu.removeMenuItem(this, menuItem);
      }
      WindowMenuManager.removeManager(this);
    }
  }

  void removeMenuItem(WindowMenu menu) {
    final var item = menuItems.remove(menu);
    if (item != null) menu.removeMenuItem(this, item);
  }

  void setSelected(boolean selected) {
    for (final var item : menuItems.values()) {
      item.setSelected(selected);
    }
  }

  public void setText(String value) {
    text = value;
    for (final var menuItem : menuItems.values()) {
      menuItem.setText(text);
    }
  }
}
