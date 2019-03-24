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

package com.cburch.logisim.gui.menu;

import java.awt.event.ActionEvent;

public abstract class EditHandler {
  public void cut() { }
  public void copy() { }
  public void paste() { }
  public void delete() { }
  public void duplicate() { }
  public void selectAll() { }
  public void raise() { }
  public void lower() { }
  public void raiseTop() { }
  public void lowerBottom() { }
  public void addControlPoint() { }
  public void removeControlPoint() { }
  public abstract void computeEnabled();

  protected void setEnabled(LogisimMenuItem action, boolean value) {
    Listener l = listener;
    if (l != null) {
      l.enableChanged(this, action, value);
    }
  }

  public static interface Listener {
    void enableChanged(EditHandler handler, LogisimMenuItem action, boolean value);
  }

  private Listener listener;

  public void setListener(Listener listener) {
    this.listener = listener;
  }
  
  public void actionPerformed(ActionEvent e) {
    Object src = e.getSource();
    if (src == LogisimMenuBar.CUT)
      cut();
    else if (src == LogisimMenuBar.COPY)
      copy();
    else if (src == LogisimMenuBar.PASTE)
      paste();
    else if (src == LogisimMenuBar.DELETE)
      delete();
    else if (src == LogisimMenuBar.DUPLICATE)
      duplicate();
    else if (src == LogisimMenuBar.SELECT_ALL)
      selectAll();
    else if (src == LogisimMenuBar.RAISE)
      raise();
    else if (src == LogisimMenuBar.LOWER)
      lower();
    else if (src == LogisimMenuBar.RAISE_TOP)
      raiseTop();
    else if (src == LogisimMenuBar.LOWER_BOTTOM)
      lowerBottom();
    else if (src == LogisimMenuBar.ADD_CONTROL)
      addControlPoint();
    else if (src == LogisimMenuBar.REMOVE_CONTROL)
      removeControlPoint();
  }
}
