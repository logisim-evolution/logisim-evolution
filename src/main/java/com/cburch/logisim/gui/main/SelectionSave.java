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

package com.cburch.logisim.gui.main;

import com.cburch.logisim.comp.Component;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import lombok.Getter;
import lombok.val;

class SelectionSave {
  @Getter private Component[] floatingComponents;
  @Getter private Component[] anchoredComponents;

  private SelectionSave() {}

  public static SelectionSave create(Selection sel) {
    val save = new SelectionSave();

    val lifted = sel.getFloatingComponents();
    if (!lifted.isEmpty()) {
      save.floatingComponents = lifted.toArray(new Component[0]);
    }

    val selected = sel.getAnchoredComponents();
    if (!selected.isEmpty()) {
      save.anchoredComponents = selected.toArray(new Component[0]);
    }

    return save;
  }

  private static boolean isSame(Component[] save, Collection<Component> sel) {
    return (save == null) ? sel.isEmpty() : toSet(save).equals(sel);
  }

  private static boolean isSame(Component[] a, Component[] b) {
    if (a == null || a.length == 0) {
      return b == null || b.length == 0;
    } else if (b == null || b.length == 0) {
      return false;
    } else if (a.length != b.length) {
      return false;
    } else {
      return toSet(a).equals(toSet(b));
    }
  }

  public boolean isSame(Selection sel) {
    return isSame(floatingComponents, sel.getFloatingComponents())
        && isSame(anchoredComponents, sel.getAnchoredComponents());
  }

  private static HashSet<Component> toSet(Component[] comps) {
    val ret = new HashSet<Component>(comps.length);
    ret.addAll(Arrays.asList(comps));
    return ret;
  }

  @Override
  public boolean equals(Object other) {
    if (other instanceof SelectionSave) {
      val o = (SelectionSave) other;
      return isSame(this.floatingComponents, o.floatingComponents) && isSame(this.anchoredComponents, o.anchoredComponents);
    } else {
      return false;
    }
  }

  @Override
  public int hashCode() {
    var ret = 0;
    if (floatingComponents != null) {
      for (val component : floatingComponents) ret += component.hashCode();
    }
    if (anchoredComponents != null) {
      for (val component : anchoredComponents) ret += component.hashCode();
    }
    return ret;
  }
}
