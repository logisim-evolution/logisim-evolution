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
import java.awt.event.ActionListener;
import java.util.ArrayList;

import com.cburch.logisim.gui.menu.LogisimMenuBar;
import com.cburch.logisim.gui.menu.LogisimMenuItem;

public class MenuListener {
  protected class EditListener implements ActionListener, EditHandler.Listener {
    private EditHandler handler = null;

    public void actionPerformed(ActionEvent e) {
      if (handler != null)
        handler.actionPerformed(e);
    }

    public void enableChanged(EditHandler handler, LogisimMenuItem action, boolean value) {
      if (handler == this.handler) {
        menubar.setEnabled(action, value);
        fireEnableChanged();
      }
    }

    public void register() {
      for (LogisimMenuItem item: LogisimMenuBar.EDIT_ITEMS)
        menubar.addActionListener(item, this);
      computeEnabled();
    }

    public void computeEnabled() {
      if (handler != null) {
        handler.computeEnabled();
      } else {
        for (LogisimMenuItem item: LogisimMenuBar.EDIT_ITEMS)
          menubar.setEnabled(item, false);
      }
    }

    private void setHandler(EditHandler value) {
      handler = value;
      if (handler != null)
        handler.setListener(this);
      computeEnabled();
    }
  }

  public interface EnabledListener {
    public void menuEnableChanged(MenuListener source);
  }

  protected LogisimMenuBar menubar;
  protected ArrayList<EnabledListener> listeners;
  protected EditListener editListener = new EditListener();

  public MenuListener(LogisimMenuBar menubar) {
    this.menubar = menubar;
    this.listeners = new ArrayList<EnabledListener>();
  }

  public void addEnabledListener(EnabledListener listener) {
    listeners.add(listener);
  }

  public void removeEnabledListener(EnabledListener listener) {
    listeners.remove(listener);
  }

  protected void fireEnableChanged() {
    for (EnabledListener listener : listeners) {
      listener.menuEnableChanged(this);
    }
  }

  public void setEditHandler(EditHandler handler) {
    editListener.setHandler(handler);
  }

  public void doAction(LogisimMenuItem item) {
    menubar.doAction(item);
  }

  public LogisimMenuBar getMenuBar() {
    return menubar;
  }

  public boolean isEnabled(LogisimMenuItem item) {
    return menubar.isEnabled(item);
  }
}