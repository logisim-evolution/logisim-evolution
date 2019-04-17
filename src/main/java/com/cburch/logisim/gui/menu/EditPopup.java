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

import static com.cburch.logisim.gui.Strings.S;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.Map;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

public abstract class EditPopup extends JPopupMenu {
  private class Listener implements ActionListener {
    public void actionPerformed(ActionEvent e) {
      Object source = e.getSource();
      for (Map.Entry<LogisimMenuItem, JMenuItem> entry : items.entrySet()) {
        if (entry.getValue() == source) {
          fire(entry.getKey());
          return;
        }
      }
    }
  }

  private static final long serialVersionUID = 1L;

  private Listener listener;
  private Map<LogisimMenuItem, JMenuItem> items;

  public EditPopup() {
    this(false);
  }

  public EditPopup(boolean waitForInitialize) {
    listener = new Listener();
    items = new HashMap<LogisimMenuItem, JMenuItem>();
    if (!waitForInitialize) initialize();
  }

  private boolean add(LogisimMenuItem item, String display) {
    if (shouldShow(item)) {
      JMenuItem menu = new JMenuItem(display);
      items.put(item, menu);
      menu.setEnabled(isEnabled(item));
      menu.addActionListener(listener);
      add(menu);
      return true;
    } else {
      return false;
    }
  }

  protected abstract void fire(LogisimMenuItem item);

  protected void initialize() {
    boolean x = false;
    x |= add(LogisimMenuBar.CUT, S.get("editCutItem"));
    x |= add(LogisimMenuBar.COPY, S.get("editCopyItem"));
    if (x) {
      addSeparator();
      x = false;
    }
    x |= add(LogisimMenuBar.DELETE, S.get("editClearItem"));
    x |= add(LogisimMenuBar.DUPLICATE, S.get("editDuplicateItem"));
    if (x) {
      addSeparator();
      x = false;
    }
    x |= add(LogisimMenuBar.RAISE, S.get("editRaiseItem"));
    x |= add(LogisimMenuBar.LOWER, S.get("editLowerItem"));
    x |= add(LogisimMenuBar.RAISE_TOP, S.get("editRaiseTopItem"));
    x |= add(LogisimMenuBar.LOWER_BOTTOM, S.get("editLowerBottomItem"));
    if (x) {
      addSeparator();
      x = false;
    }
    x |= add(LogisimMenuBar.ADD_CONTROL, S.get("editAddControlItem"));
    x |= add(LogisimMenuBar.REMOVE_CONTROL, S.get("editRemoveControlItem"));
    if (!x && getComponentCount() > 0) {
      remove(getComponentCount() - 1);
    }
  }

  protected abstract boolean isEnabled(LogisimMenuItem item);

  protected abstract boolean shouldShow(LogisimMenuItem item);
}
