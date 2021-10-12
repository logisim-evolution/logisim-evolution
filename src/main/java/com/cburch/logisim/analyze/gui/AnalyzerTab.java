/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.analyze.gui;

import com.cburch.logisim.gui.menu.EditHandler;
import com.cburch.logisim.gui.menu.PrintHandler;
import javax.swing.JPanel;

abstract class AnalyzerTab extends JPanel {
  private static final long serialVersionUID = 1L;

  abstract void localeChanged();

  abstract void updateTab();

  EditHandler getEditHandler() {
    return null;
  }

  PrintHandler getPrintHandler() {
    return null;
  }
}
