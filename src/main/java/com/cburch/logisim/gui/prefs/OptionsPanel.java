/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.gui.prefs;

import java.awt.LayoutManager;
import javax.swing.JPanel;

public abstract class OptionsPanel extends JPanel {
  private static final long serialVersionUID = 1L;
  private final PreferencesFrame optionsFrame;

  public OptionsPanel(PreferencesFrame frame) {
    super();
    this.optionsFrame = frame;
  }

  public OptionsPanel(PreferencesFrame frame, LayoutManager manager) {
    super(manager);
    this.optionsFrame = frame;
  }

  public abstract String getHelpText();

  public PreferencesFrame getPreferencesFrame() {
    return optionsFrame;
  }

  public abstract String getTitle();

  public abstract void localeChanged();
}
