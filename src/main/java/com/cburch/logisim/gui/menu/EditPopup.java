/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.gui.menu;

import static com.cburch.logisim.gui.Strings.S;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.Map;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

public abstract class EditPopup extends JPopupMenu {
  private static final long serialVersionUID = 1L;
  private final Listener listener;
  private final Map<LogisimMenuItem, JMenuItem> items;

  public EditPopup() {
    this(false);
  }

  public EditPopup(boolean waitForInitialize) {
    listener = new Listener();
    items = new HashMap<>();
    if (!waitForInitialize) initialize();
  }

  private boolean add(LogisimMenuItem item, String display) {
    if (shouldShow(item)) {
      final var menu = new JMenuItem(display);
      items.put(item, menu);
      menu.setEnabled(isEnabled(item));
      menu.addActionListener(listener);
      add(menu);
      return true;
    } else {
      return false;
    }
  }

  protected abstract void fire(LogisimMenuItem item);

  protected void initialize() {
    var x = false;
    x |= add(LogisimMenuBar.CUT, S.get("editCutItem"));
    x |= add(LogisimMenuBar.COPY, S.get("editCopyItem"));
    if (x) {
      addSeparator();
      x = false;
    }
    x |= add(LogisimMenuBar.DELETE, S.get("editClearItem"));
    x |= add(LogisimMenuBar.DUPLICATE, S.get("editDuplicateItem"));
    if (x) {
      addSeparator();
      x = false;
    }
    x |= add(LogisimMenuBar.RAISE, S.get("editRaiseItem"));
    x |= add(LogisimMenuBar.LOWER, S.get("editLowerItem"));
    x |= add(LogisimMenuBar.RAISE_TOP, S.get("editRaiseTopItem"));
    x |= add(LogisimMenuBar.LOWER_BOTTOM, S.get("editLowerBottomItem"));
    if (x) {
      addSeparator();
      x = false;
    }
    x |= add(LogisimMenuBar.ADD_CONTROL, S.get("editAddControlItem"));
    x |= add(LogisimMenuBar.REMOVE_CONTROL, S.get("editRemoveControlItem"));
    if (!x && getComponentCount() > 0) {
      remove(getComponentCount() - 1);
    }
  }

  protected abstract boolean isEnabled(LogisimMenuItem item);

  protected abstract boolean shouldShow(LogisimMenuItem item);

  private class Listener implements ActionListener {
    @Override
    public void actionPerformed(ActionEvent e) {
      final var source = e.getSource();
      for (final var entry : items.entrySet()) {
        if (entry.getValue() == source) {
          fire(entry.getKey());
          return;
        }
      }
    }
  }
}
