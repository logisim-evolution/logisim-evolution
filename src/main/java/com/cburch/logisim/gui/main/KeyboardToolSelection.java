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
import com.cburch.draw.toolbar.ToolbarItem;
import com.cburch.draw.toolbar.ToolbarModel;
import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.InputMap;
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
    ActionMap amap = toolbar.getActionMap();
    InputMap imap = toolbar.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
    int mask = toolbar.getToolkit().getMenuShortcutKeyMaskEx();
    for (int i = 0; i < 10; i++) {
      KeyStroke keyStroke = KeyStroke.getKeyStroke((char) ('0' + i), mask);
      int j = (i == 0 ? 10 : i - 1);
      KeyboardToolSelection action = new KeyboardToolSelection(toolbar, j);
      String key = "ToolSelect" + i;
      amap.put(key, action);
      imap.put(keyStroke, key);
    }
  }

  @Override
  public void actionPerformed(ActionEvent event) {
    ToolbarModel model = toolbar.getToolbarModel();
    int i = -1;
    for (ToolbarItem item : model.getItems()) {
      if (item.isSelectable()) {
        i++;
        if (i == index) {
          model.itemSelected(item);
        }
      }
    }
  }
}
