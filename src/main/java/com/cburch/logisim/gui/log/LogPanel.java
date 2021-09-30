/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.gui.log;

import com.cburch.logisim.gui.menu.EditHandler;
import com.cburch.logisim.gui.menu.LogisimMenuBar;
import com.cburch.logisim.gui.menu.PrintHandler;
import com.cburch.logisim.proj.Project;
import java.awt.LayoutManager;
import javax.swing.JPanel;

public abstract class LogPanel extends JPanel {
  private static final long serialVersionUID = 1L;
  private final LogFrame logFrame;

  public LogPanel(LogFrame frame) {
    super();
    this.logFrame = frame;
  }

  public LogPanel(LogFrame frame, LayoutManager manager) {
    super(manager);
    this.logFrame = frame;
  }

  public abstract String getHelpText();

  void updateTab() {
    final var h = getEditHandler();
    if (h != null) h.computeEnabled();
  }

  public EditHandler getEditHandler() {
    return null;
  }

  public PrintHandler getPrintHandler() {
    return null;
  }

  public LogFrame getLogFrame() {
    return logFrame;
  }

  protected LogisimMenuBar getLogisimMenuBar() {
    return logFrame.getLogisimMenuBar();
  }

  protected Model getModel() {
    return logFrame.getModel();
  }

  protected Project getProject() {
    return logFrame.getProject();
  }

  public abstract String getTitle();

  public void localeChanged() {
    // no-op implementation
  }

  public void modelChanged(Model oldModel, Model newModel) {
    // no-op implementation
  }
}
