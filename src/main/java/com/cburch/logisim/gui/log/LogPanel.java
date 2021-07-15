/*
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
    EditHandler h = getEditHandler();
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
