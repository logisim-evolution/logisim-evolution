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

import com.cburch.logisim.gui.menu.Menu;
import com.cburch.logisim.prefs.AppPreferences;
import com.cburch.logisim.prefs.PrefMonitorKeyStroke;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import javax.swing.ButtonGroup;
import javax.swing.JFrame;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JRadioButtonMenuItem;

import com.cburch.logisim.Main;
import com.cburch.logisim.data.Direction;
import com.cburch.logisim.prefs.AppPreferences;

public class WindowMenu extends Menu {
  private class MyListener implements LocaleListener, ActionListener, PropertyChangeListener{
    @Override
    public void actionPerformed(ActionEvent e) {
      final var src = e.getSource();
      if (src == minimize) {
        doMinimize();
      } else if (src == zoom) {
        doZoom();
      } else if (src == close) {
        doClose();
      } else if (src == toolbar) {
        doToolbar();
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
        if (i.getJFrame() == owner) {
          return i;
        }
      }
      for (WindowMenuItem i : transientItems) {
        if (i.getJFrame() == owner) {
          return i;
        }
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
      toolbar.setText(S.get("windowShowToolbarItem"));
    }

    @Override
    public void propertyChange(PropertyChangeEvent event) {
      if (AppPreferences.TOOLBAR_PLACEMENT.isSource(event)) {
        toolbar.setState(isToolbarVisible());
      }
    }
  }

  private static final long serialVersionUID = 1L;

  private final JFrame owner;
  private final MyListener myListener = new MyListener();
  private final JMenuItem minimize = new JMenuItem();
  private final JMenuItem zoom = new JMenuItem();
  private final JCheckBoxMenuItem toolbar = new JCheckBoxMenuItem();
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
    minimize.setAccelerator(((PrefMonitorKeyStroke)
        AppPreferences.HOTKEY_WINDOW_MINIMIZE).getWithMask(0));
    close.setAccelerator(((PrefMonitorKeyStroke)
        AppPreferences.HOTKEY_WINDOW_CLOSE).getWithMask(0));

    /* add myself to hotkey sync */
    AppPreferences.gui_sync_objects.add(this);

    if (owner == null) {
      minimize.setEnabled(false);
      zoom.setEnabled(false);
      close.setEnabled(false);
    } else {
      minimize.addActionListener(myListener);
      zoom.addActionListener(myListener);
      close.addActionListener(myListener);
    }

    toolbar.setEnabled(true);
    toolbar.setState(isToolbarVisible());
    toolbar.addActionListener(myListener);
    AppPreferences.TOOLBAR_PLACEMENT.addPropertyChangeListener(myListener);

    computeEnabled();
    computeContents();

    LocaleManager.addLocaleListener(myListener);
    myListener.localeChanged();
  }

  @Override
  public void hotkeyUpdate() {
    minimize.setAccelerator(((PrefMonitorKeyStroke)
        AppPreferences.HOTKEY_WINDOW_MINIMIZE).getWithMask(0));
    close.setAccelerator(((PrefMonitorKeyStroke)
        AppPreferences.HOTKEY_WINDOW_CLOSE).getWithMask(0));
  }

  void addMenuItem(Object source, WindowMenuItem item, boolean isPersistent) {
    if (isPersistent) {
      persistentItems.add(item);
    } else {
      transientItems.add(item);
    }
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
    addSeparator();
    add(toolbar);

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

  @Override
  protected void computeEnabled() {
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
    if (owner == null) {
      return;
    }

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

    if (locChanged) {
      owner.setLocation(windowLoc);
    }
    if (sizeChanged) {
      owner.setSize(windowSize);
    }
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

  static boolean isToolbarVisible() {
    String loc = AppPreferences.TOOLBAR_PLACEMENT.get();
    return loc == null || !loc.equals(AppPreferences.TOOLBAR_HIDDEN);
  }

  static void setToolbarVisibility(boolean show) {
    if (show)
      AppPreferences.TOOLBAR_PLACEMENT.set(Direction.NORTH.toString());
    else
      AppPreferences.TOOLBAR_PLACEMENT.set(AppPreferences.TOOLBAR_HIDDEN);
  }

  void doToolbar() {
    setToolbarVisibility(toolbar.getState());
  }
}
