/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.analyze.gui;

import static com.cburch.logisim.analyze.Strings.S;

import com.cburch.logisim.util.LocaleListener;
import com.cburch.logisim.util.LocaleManager;
import com.cburch.logisim.util.WindowMenuItemManager;
import javax.swing.JFrame;

public class AnalyzerManager extends WindowMenuItemManager implements LocaleListener {
  public static Analyzer getAnalyzer(java.awt.Component parent) {
    if (analysisWindow == null) {
      analysisWindow = new Analyzer();
      analysisWindow.pack();
      analysisWindow.setLocationRelativeTo(parent);
      if (analysisManager != null) analysisManager.frameOpened(analysisWindow);
    }
    return analysisWindow;
  }

  public static void initialize() {
    analysisManager = new AnalyzerManager();
  }

  private static Analyzer analysisWindow = null;
  private static AnalyzerManager analysisManager = null;

  private AnalyzerManager() {
    super(S.get("analyzerWindowTitle"), true);
    LocaleManager.addLocaleListener(this);
  }

  @Override
  public JFrame getJFrame(boolean create, java.awt.Component parent) {
    return (create) ? getAnalyzer(parent) : analysisWindow;
  }

  @Override
  public void localeChanged() {
    setText(S.get("analyzerWindowTitle"));
  }
}
