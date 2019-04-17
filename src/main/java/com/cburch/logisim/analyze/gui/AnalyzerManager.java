/**
 * This file is part of logisim-evolution.
 *
 * Logisim-evolution is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 *
 * Logisim-evolution is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License along 
 * with logisim-evolution. If not, see <http://www.gnu.org/licenses/>.
 *
 * Original code by Carl Burch (http://www.cburch.com), 2011.
 * Subsequent modifications by:
 *   + College of the Holy Cross
 *     http://www.holycross.edu
 *   + Haute École Spécialisée Bernoise/Berner Fachhochschule
 *     http://www.bfh.ch
 *   + Haute École du paysage, d'ingénierie et d'architecture de Genève
 *     http://hepia.hesge.ch/
 *   + Haute École d'Ingénierie et de Gestion du Canton de Vaud
 *     http://www.heig-vd.ch/
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
    if (create) {
      return getAnalyzer(parent);
    } else {
      return analysisWindow;
    }
  }

  public void localeChanged() {
    setText(S.get("analyzerWindowTitle"));
  }
}
