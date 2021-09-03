/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.gui.main;

import com.cburch.logisim.comp.Component;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;

class SelectionSave {
  private Component[] floating;
  private Component[] anchored;

  private SelectionSave() {}

  public static SelectionSave create(Selection sel) {
    SelectionSave save = new SelectionSave();

    Collection<Component> lifted = sel.getFloatingComponents();
    if (!lifted.isEmpty()) {
      save.floating = lifted.toArray(new Component[0]);
    }

    Collection<Component> selected = sel.getAnchoredComponents();
    if (!selected.isEmpty()) {
      save.anchored = selected.toArray(new Component[0]);
    }

    return save;
  }

  private static boolean isSame(Component[] save, Collection<Component> sel) {
    if (save == null) {
      return sel.isEmpty();
    } else {
      return toSet(save).equals(sel);
    }
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
    return isSame(floating, sel.getFloatingComponents())
        && isSame(anchored, sel.getAnchoredComponents());
  }

  private static HashSet<Component> toSet(Component[] comps) {
    HashSet<Component> ret = new HashSet<>(comps.length);
    ret.addAll(Arrays.asList(comps));
    return ret;
  }

  @Override
  public boolean equals(Object other) {
    if (other instanceof SelectionSave) {
      SelectionSave o = (SelectionSave) other;
      return isSame(this.floating, o.floating) && isSame(this.anchored, o.anchored);
    } else {
      return false;
    }
  }

  public Component[] getAnchoredComponents() {
    return anchored;
  }

  public Component[] getFloatingComponents() {
    return floating;
  }

  @Override
  public int hashCode() {
    int ret = 0;
    if (floating != null) {
      for (Component c : floating) ret += c.hashCode();
    }
    if (anchored != null) {
      for (Component c : anchored) ret += c.hashCode();
    }
    return ret;
  }
}
