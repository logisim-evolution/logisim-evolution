/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.util;

import static com.cburch.logisim.util.Strings.S;

import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import javax.swing.ButtonGroup;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.KeyStroke;

public class WindowMenu extends JMenu {
  private class MyListener implements LocaleListener, ActionListener {
    @Override
    public void actionPerformed(ActionEvent e) {
      final var src = e.getSource();
      if (src == minimize) {
        doMinimize();
      } else if (src == zoom) {
        doZoom();
      } else if (src == close) {
        doClose();
      } else if (src instanceof WindowMenuItem choice) {
        if (choice.isSelected()) {
          final var item = findOwnerItem();
          if (item != null) {
            item.setSelected(true);
          }
          choice.actionPerformed(e);
        }
      }
    }

    private WindowMenuItem findOwnerItem() {
      for (WindowMenuItem i : persistentItems) {
        if (i.getJFrame() == owner) return i;
      }
      for (WindowMenuItem i : transientItems) {
        if (i.getJFrame() == owner) return i;
      }
      return null;
    }

    @Override
    public void localeChanged() {
      WindowMenu.this.setText(S.get("windowMenu"));
      minimize.setText(S.get("windowMinimizeItem"));
      close.setText(S.get("windowCloseItem"));
      zoom.setText(
          MacCompatibility.isQuitAutomaticallyPresent()
              ? S.get("windowZoomItemMac")
              : S.get("windowZoomItem"));
    }
  }

  private static final long serialVersionUID = 1L;

  private final JFrame owner;
  private final MyListener myListener = new MyListener();
  private final JMenuItem minimize = new JMenuItem();
  private final JMenuItem zoom = new JMenuItem();
  private final JMenuItem close = new JMenuItem();
  private final JRadioButtonMenuItem nullItem = new JRadioButtonMenuItem();
  private final ArrayList<WindowMenuItem> persistentItems = new ArrayList<>();
  private final ArrayList<WindowMenuItem> transientItems = new ArrayList<>();

  /**
   * Constructor for "Window" menu.
   *
   * @param owner Parent frame.
   */
  public WindowMenu(JFrame owner) {
    this.owner = owner;
    WindowMenuManager.addMenu(this);

    final var menuMask = getToolkit().getMenuShortcutKeyMaskEx();
    minimize.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_M, menuMask));
    close.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_W, menuMask));

    if (owner == null) {
      minimize.setEnabled(false);
      zoom.setEnabled(false);
      close.setEnabled(false);
    } else {
      minimize.addActionListener(myListener);
      zoom.addActionListener(myListener);
      close.addActionListener(myListener);
    }

    computeEnabled();
    computeContents();

    LocaleManager.addLocaleListener(myListener);
    myListener.localeChanged();
  }

  void addMenuItem(Object source, WindowMenuItem item, boolean isPersistent) {
    if (isPersistent) persistentItems.add(item);
    else transientItems.add(item);
    item.addActionListener(myListener);
    computeContents();
  }

  private void computeContents() {
    final var bgroup = new ButtonGroup();
    bgroup.add(nullItem);

    removeAll();
    add(minimize);
    add(zoom);
    add(close);

    if (!persistentItems.isEmpty()) {
      addSeparator();
      for (JRadioButtonMenuItem item : persistentItems) {
        bgroup.add(item);
        add(item);
      }
    }

    if (!transientItems.isEmpty()) {
      addSeparator();
      for (JRadioButtonMenuItem item : transientItems) {
        bgroup.add(item);
        add(item);
      }
    }

    WindowMenuItemManager currentManager = WindowMenuManager.getCurrentManager();
    if (currentManager != null) {
      JRadioButtonMenuItem item = currentManager.getMenuItem(this);
      if (item != null) {
        item.setSelected(true);
      }
    }
  }

  void computeEnabled() {
    WindowMenuItemManager currentManager = WindowMenuManager.getCurrentManager();
    minimize.setEnabled(currentManager != null);
    zoom.setEnabled(currentManager != null);
    close.setEnabled(currentManager != null);
  }

  void doClose() {
    if (owner instanceof WindowClosable windowClosable) {
      windowClosable.requestClose();
    } else if (owner != null) {
      int action = owner.getDefaultCloseOperation();
      if (action == JFrame.EXIT_ON_CLOSE) {
        System.exit(0);
      } else if (action == JFrame.HIDE_ON_CLOSE) {
        owner.setVisible(false);
      } else if (action == JFrame.DISPOSE_ON_CLOSE) {
        owner.dispose();
      }
    }
  }

  void doMinimize() {
    if (owner != null) {
      owner.setExtendedState(Frame.ICONIFIED);
    }
  }

  void doZoom() {
    if (owner == null) return;

    owner.pack();
    final var screenSize = owner.getToolkit().getScreenSize();
    final var windowSize = owner.getPreferredSize();
    final var windowLoc = owner.getLocation();

    var locChanged = false;
    var sizeChanged = false;
    if (windowLoc.x + windowSize.width > screenSize.width) {
      windowLoc.x = Math.max(0, screenSize.width - windowSize.width);
      locChanged = true;
      if (windowLoc.x + windowSize.width > screenSize.width) {
        windowSize.width = screenSize.width - windowLoc.x;
        sizeChanged = true;
      }
    }
    if (windowLoc.y + windowSize.height > screenSize.height) {
      windowLoc.y = Math.max(0, screenSize.height - windowSize.height);
      locChanged = true;
      if (windowLoc.y + windowSize.height > screenSize.height) {
        windowSize.height = screenSize.height - windowLoc.y;
        sizeChanged = true;
      }
    }

    if (locChanged) owner.setLocation(windowLoc);
    if (sizeChanged) owner.setSize(windowSize);
  }

  void removeMenuItem(Object source, JRadioButtonMenuItem item) {
    if (transientItems.remove(item)) {
      item.removeActionListener(myListener);
    }
    computeContents();
  }

  void setNullItemSelected(boolean value) {
    nullItem.setSelected(value);
  }
}
