/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.gui.main;

import com.cburch.draw.toolbar.Toolbar;
import com.cburch.logisim.prefs.AppPreferences;
import com.cburch.logisim.prefs.PrefMonitor;
import com.cburch.logisim.prefs.PrefMonitorKeyStroke;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.List;
import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;

public class KeyboardToolSelection extends AbstractAction {
  private static final long serialVersionUID = 1L;
  static final int TOOL_SELECTION_COUNT = 14;
  private static final String ACTION_PREFIX = "ToolSelect";
  private static final List<PrefMonitor<KeyStroke>> TOOL_SELECTION_HOTKEYS =
      List.of(
          AppPreferences.HOTKEY_TOOL_SELECT_1,
          AppPreferences.HOTKEY_TOOL_SELECT_2,
          AppPreferences.HOTKEY_TOOL_SELECT_3,
          AppPreferences.HOTKEY_TOOL_SELECT_4,
          AppPreferences.HOTKEY_TOOL_SELECT_5,
          AppPreferences.HOTKEY_TOOL_SELECT_6,
          AppPreferences.HOTKEY_TOOL_SELECT_7,
          AppPreferences.HOTKEY_TOOL_SELECT_8,
          AppPreferences.HOTKEY_TOOL_SELECT_9,
          AppPreferences.HOTKEY_TOOL_SELECT_10,
          AppPreferences.HOTKEY_TOOL_SELECT_11,
          AppPreferences.HOTKEY_TOOL_SELECT_12,
          AppPreferences.HOTKEY_TOOL_SELECT_13,
          AppPreferences.HOTKEY_TOOL_SELECT_14);
  private final Toolbar toolbar;
  private final int index;

  public KeyboardToolSelection(Toolbar toolbar, int index) {
    this.toolbar = toolbar;
    this.index = index;
  }

  public static Registration register(Toolbar toolbar) {
    return register(toolbar, TOOL_SELECTION_HOTKEYS);
  }

  static Registration register(Toolbar toolbar, List<PrefMonitor<KeyStroke>> hotkeys) {
    return new Registration(toolbar, hotkeys);
  }

  static String getHotkeyDisplayString(int index) {
    return ((PrefMonitorKeyStroke) TOOL_SELECTION_HOTKEYS.get(index)).getDisplayString();
  }

  static final class Registration implements AutoCloseable, PropertyChangeListener {
    private final Toolbar toolbar;
    private final List<PrefMonitor<KeyStroke>> hotkeys;
    private volatile boolean closed;

    private Registration(Toolbar toolbar, List<PrefMonitor<KeyStroke>> hotkeys) {
      if (hotkeys.size() != TOOL_SELECTION_COUNT) {
        throw new IllegalArgumentException("Expected " + TOOL_SELECTION_COUNT + " toolbar hotkeys");
      }
      this.toolbar = toolbar;
      this.hotkeys = List.copyOf(hotkeys);

      final var actionMap = toolbar.getActionMap();
      for (var i = 0; i < TOOL_SELECTION_COUNT; i++) {
        actionMap.put(actionKey(i), new KeyboardToolSelection(toolbar, i));
      }
      updateBindings();
      for (final var hotkey : this.hotkeys) {
        hotkey.addPropertyChangeListener(this);
      }
    }

    private static String actionKey(int index) {
      return ACTION_PREFIX + (index + 1);
    }

    private static boolean isToolbarSelectionAction(Object value) {
      if (!(value instanceof String action)) {
        return false;
      }
      for (var i = 0; i < TOOL_SELECTION_COUNT; i++) {
        if (actionKey(i).equals(action)) {
          return true;
        }
      }
      return false;
    }

    private void updateBindings() {
      if (closed) {
        return;
      }
      final var inputMap = toolbar.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
      final var oldKeys = inputMap.keys();
      if (oldKeys != null) {
        for (final var oldKey : oldKeys) {
          if (isToolbarSelectionAction(inputMap.get(oldKey))) {
            inputMap.remove(oldKey);
          }
        }
      }
      for (var i = 0; i < TOOL_SELECTION_COUNT; i++) {
        final var hotkey = hotkeys.get(i).get();
        if (hotkey != null) {
          inputMap.put(hotkey, actionKey(i));
        }
      }
    }

    @Override
    public void propertyChange(PropertyChangeEvent event) {
      if (closed) {
        return;
      }
      if (SwingUtilities.isEventDispatchThread()) {
        updateBindings();
      } else {
        SwingUtilities.invokeLater(this::updateBindings);
      }
    }

    @Override
    public void close() {
      if (closed) {
        return;
      }
      closed = true;
      for (final var hotkey : hotkeys) {
        hotkey.removePropertyChangeListener(this);
      }
    }
  }

  @Override
  public void actionPerformed(ActionEvent event) {
    final var model = toolbar.getToolbarModel();
    var i = -1;
    for (final var item : model.getItems()) {
      if (item.isSelectable()) {
        i++;
        if (i == index) {
          model.itemSelected(item);
        }
      }
    }
  }
}
