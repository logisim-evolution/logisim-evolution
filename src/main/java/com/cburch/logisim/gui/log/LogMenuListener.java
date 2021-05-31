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

import com.cburch.logisim.gui.menu.LogisimMenuBar;
import com.cburch.logisim.gui.menu.MenuListener;
import com.cburch.logisim.gui.menu.PrintHandler;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class LogMenuListener extends MenuListener {

  protected class FileListener implements ActionListener {
    public void actionPerformed(ActionEvent event) {
      if (printer != null)
        printer.actionPerformed(event);
    }
    boolean registered;
    public void register(boolean en) {
      if (registered == en)
        return;
      registered = en;
      if (en) {
        menubar.addActionListener(LogisimMenuBar.EXPORT_IMAGE, this);
        menubar.addActionListener(LogisimMenuBar.PRINT, this);
      } else {
        menubar.removeActionListener(LogisimMenuBar.EXPORT_IMAGE, this);
        menubar.removeActionListener(LogisimMenuBar.PRINT, this);
      }
    }
  }

  private final FileListener fileListener = new FileListener();
  private PrintHandler printer;

  public LogMenuListener(LogisimMenuBar menubar) {
    super(menubar);
    fileListener.register(false);
    editListener.register();
  }

  public void setPrintHandler(PrintHandler printer) {
    this.printer = printer;
    fileListener.register(printer != null);
  }

}
