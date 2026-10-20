/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.gui.main;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import com.cburch.draw.toolbar.Toolbar;
import com.cburch.draw.toolbar.ToolbarItem;
import com.cburch.draw.toolbar.ToolbarModel;
import com.cburch.draw.toolbar.ToolbarModelListener;
import com.cburch.draw.toolbar.ToolbarSeparator;
import com.cburch.logisim.prefs.PrefMonitor;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.PreferenceChangeEvent;
import javax.swing.JComponent;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.Test;

class KeyboardToolSelectionTest {
  @Test
  void selectsToolbarSlotsWhileSkippingSeparators() {
    final var first = new TestToolbarItem();
    final var second = new TestToolbarItem();
    final var model = new TestToolbarModel(List.of(first, new ToolbarSeparator(4), second));
    final var toolbar = new Toolbar(model);
    final var hotkeys = createHotkeys();

    try (var ignored = KeyboardToolSelection.register(toolbar, hotkeys)) {
      performBoundAction(toolbar, hotkeys.get(1).get());

      assertSame(second, model.selected);
    }
  }

  @Test
  void lastShortcutSelectsLastDefaultToolbarTool() {
    final var items = new ArrayList<ToolbarItem>();
    final var selectableItems = new ArrayList<ToolbarItem>();
    for (var i = 0; i < KeyboardToolSelection.TOOL_SELECTION_COUNT; i++) {
      final var item = new TestToolbarItem();
      selectableItems.add(item);
      items.add(item);
      if (i == 4) {
        items.add(new ToolbarSeparator(4));
      }
    }
    final var model = new TestToolbarModel(items);
    final var toolbar = new Toolbar(model);
    final var hotkeys = createHotkeys();

    try (var ignored = KeyboardToolSelection.register(toolbar, hotkeys)) {
      final var last = KeyboardToolSelection.TOOL_SELECTION_COUNT - 1;
      performBoundAction(toolbar, hotkeys.get(last).get());

      assertSame(selectableItems.get(last), model.selected);
    }
  }

  @Test
  void leavesUnassignedSlotUnboundUntilPreferenceIsAssigned() throws Exception {
    final var model = new TestToolbarModel(List.of(new TestToolbarItem()));
    final var toolbar = new Toolbar(model);
    final var hotkeys = createHotkeys();
    final var index = 10;
    final var oldKey = hotkeys.get(index).get();
    final var assignedKey =
        KeyStroke.getKeyStroke(KeyEvent.VK_F12, InputEvent.SHIFT_DOWN_MASK);
    hotkeys.get(index).set(null);

    try (var ignored = KeyboardToolSelection.register(toolbar, hotkeys)) {
      final var inputMap = toolbar.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
      assertNull(inputMap.get(oldKey));

      hotkeys.get(index).set(assignedKey);
      SwingUtilities.invokeAndWait(() -> {});
      assertEquals("ToolSelect11", inputMap.get(assignedKey));

      hotkeys.get(index).set(null);
      SwingUtilities.invokeAndWait(() -> {});
      assertNull(inputMap.get(assignedKey));
    }
  }

  @Test
  void replacesOldBindingWhenPreferenceChanges() throws Exception {
    final var model = new TestToolbarModel(List.of(new TestToolbarItem()));
    final var toolbar = new Toolbar(model);
    final var hotkeys = createHotkeys();
    final var oldKey = hotkeys.get(0).get();
    final var newKey = KeyStroke.getKeyStroke(KeyEvent.VK_F12, InputEvent.SHIFT_DOWN_MASK);

    try (var registration = KeyboardToolSelection.register(toolbar, hotkeys)) {
      hotkeys.get(0).set(newKey);
      SwingUtilities.invokeAndWait(() -> {});

      final var inputMap = toolbar.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
      assertNull(inputMap.get(oldKey));
      assertEquals("ToolSelect1", inputMap.get(newKey));

      registration.close();
      final var keyAfterClose = KeyStroke.getKeyStroke(KeyEvent.VK_F11, 0);
      hotkeys.get(0).set(keyAfterClose);
      SwingUtilities.invokeAndWait(() -> {});
      assertEquals("ToolSelect1", inputMap.get(newKey));
      assertNull(inputMap.get(keyAfterClose));
    }
  }

  private static List<PrefMonitor<KeyStroke>> createHotkeys() {
    final var hotkeys = new ArrayList<PrefMonitor<KeyStroke>>();
    for (var i = 0; i < KeyboardToolSelection.TOOL_SELECTION_COUNT; i++) {
      hotkeys.add(
          new TestKeyStrokePreference(
              "testToolSelect" + i,
              KeyStroke.getKeyStroke(KeyEvent.VK_A + i, InputEvent.CTRL_DOWN_MASK)));
    }
    return hotkeys;
  }

  private static void performBoundAction(Toolbar toolbar, KeyStroke hotkey) {
    final var inputMap = toolbar.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
    final var actionKey = inputMap.get(hotkey);
    final var action = toolbar.getActionMap().get(actionKey);
    action.actionPerformed(new ActionEvent(toolbar, ActionEvent.ACTION_PERFORMED, "test"));
  }

  private static final class TestKeyStrokePreference implements PrefMonitor<KeyStroke> {
    private final String identifier;
    private final PropertyChangeSupport listeners = new PropertyChangeSupport(this);
    private KeyStroke value;

    private TestKeyStrokePreference(String identifier, KeyStroke value) {
      this.identifier = identifier;
      this.value = value;
    }

    @Override
    public void addPropertyChangeListener(PropertyChangeListener listener) {
      listeners.addPropertyChangeListener(identifier, listener);
    }

    @Override
    public KeyStroke get() {
      return value;
    }

    @Override
    public boolean getBoolean() {
      throw new UnsupportedOperationException();
    }

    @Override
    public String getIdentifier() {
      return identifier;
    }

    @Override
    public boolean isSource(java.beans.PropertyChangeEvent event) {
      return identifier.equals(event.getPropertyName());
    }

    @Override
    public void preferenceChange(PreferenceChangeEvent event) {
      throw new UnsupportedOperationException();
    }

    @Override
    public void removePropertyChangeListener(PropertyChangeListener listener) {
      listeners.removePropertyChangeListener(identifier, listener);
    }

    @Override
    public void set(KeyStroke newValue) {
      final var oldValue = value;
      value = newValue;
      listeners.firePropertyChange(identifier, oldValue, newValue);
    }

    @Override
    public void setBoolean(boolean value) {
      throw new UnsupportedOperationException();
    }
  }

  private static final class TestToolbarModel implements ToolbarModel {
    private final List<ToolbarItem> items;
    private ToolbarItem selected;

    private TestToolbarModel(List<ToolbarItem> items) {
      this.items = items;
    }

    @Override
    public void addToolbarModelListener(ToolbarModelListener listener) {
      // Not needed by this fixed test model.
    }

    @Override
    public List<ToolbarItem> getItems() {
      return items;
    }

    @Override
    public boolean isSelected(ToolbarItem item) {
      return selected == item;
    }

    @Override
    public void itemSelected(ToolbarItem item) {
      selected = item;
    }

    @Override
    public void removeToolbarModelListener(ToolbarModelListener listener) {
      // Not needed by this fixed test model.
    }
  }

  private static final class TestToolbarItem implements ToolbarItem {
    @Override
    public Dimension getDimension(Object orientation) {
      return new Dimension(16, 16);
    }

    @Override
    public String getToolTip() {
      return "";
    }

    @Override
    public boolean isSelectable() {
      return true;
    }

    @Override
    public void paintIcon(Component destination, Graphics graphics) {
      // No icon is needed for shortcut-selection tests.
    }
  }
}
