/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.gui.opts;

import com.cburch.logisim.file.Options;
import com.cburch.logisim.proj.Project;
import java.awt.LayoutManager;
import javax.swing.JPanel;

abstract class OptionsPanel extends JPanel {
  private static final long serialVersionUID = 1L;
  private final OptionsFrame optionsFrame;

  public OptionsPanel(OptionsFrame frame) {
    super();
    this.optionsFrame = frame;
  }

  public OptionsPanel(OptionsFrame frame, LayoutManager manager) {
    super(manager);
    this.optionsFrame = frame;
  }

  public abstract String getHelpText();

  Options getOptions() {
    return optionsFrame.getOptions();
  }

  OptionsFrame getOptionsFrame() {
    return optionsFrame;
  }

  Project getProject() {
    return optionsFrame.getProject();
  }

  public abstract String getTitle();

  public abstract void localeChanged();
}
