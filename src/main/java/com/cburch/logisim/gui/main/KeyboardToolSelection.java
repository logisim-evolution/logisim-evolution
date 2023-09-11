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
import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.KeyStroke;

public class KeyboardToolSelection extends AbstractAction {
  private static final long serialVersionUID = 1L;
  private final Toolbar toolbar;
  private final int index;

  public KeyboardToolSelection(Toolbar toolbar, int index) {
    this.toolbar = toolbar;
    this.index = index;
  }

  public static void register(Toolbar toolbar) {
    final var amap = toolbar.getActionMap();
    final var imap = toolbar.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
    final var mask = toolbar.getToolkit().getMenuShortcutKeyMaskEx();
    for (var i = 0; i < 10; i++) {
      final var keyStroke = KeyStroke.getKeyStroke((char) ('0' + i), mask);
      final var j = (i == 0 ? 10 - 1 : i - 1);
      final var action = new KeyboardToolSelection(toolbar, j);
      final var key = "ToolSelect" + i;
      amap.put(key, action);
      imap.put(keyStroke, key);
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
