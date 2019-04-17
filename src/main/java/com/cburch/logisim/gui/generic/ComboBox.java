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

package com.cburch.logisim.gui.generic;

import javax.swing.ComboBoxModel;
import javax.swing.JComboBox;

public class ComboBox<T> extends JComboBox<T> {
  public ComboBox(T[] choices) {
    super(choices);
    setMaximumRowCount(Math.min(choices.length, 33));
    setKeySelectionManager(new MultiCharSelectionManager());
  }

  public static class MultiCharSelectionManager implements JComboBox.KeySelectionManager {
    String prefix = "";
    long last;

    static int currentIndex(ComboBoxModel model) {
      Object item = model.getSelectedItem();
      for (int i = 0; item != null && i < model.getSize(); i++)
        if (item.equals(model.getElementAt(i))) return i;
      return -1;
    }

    public int selectionForKey(char ch, ComboBoxModel model) {
      int idx = currentIndex(model);
      long now = System.currentTimeMillis();
      if (now > last + 500) {
        prefix = "";
        idx = 0;
      }
      last = now;

      prefix += Character.toLowerCase(ch);

      int n = model.getSize();
      for (int offset = 0; offset < n; offset++) {
        int i = (idx + offset) % n;
        Object item = model.getElementAt(i);
        if (item != null && item.toString().toLowerCase().startsWith(prefix)) return i;
      }

      return -1;
    }
  }
}
