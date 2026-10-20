/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.gui.prefs;

import com.cburch.logisim.util.StringGetter;
import javax.swing.JComboBox;

public class PrefOption {
  private final Object value;
  private final StringGetter getter;

  public PrefOption(String value, StringGetter getter) {
    this.value = value;
    this.getter = getter;
  }

  static void setSelected(JComboBox<PrefOption> combo, Object value) {
    for (var i = combo.getItemCount() - 1; i >= 0; i--) {
      final var opt = combo.getItemAt(i);
      if (opt.getValue().equals(value)) {
        combo.setSelectedItem(opt);
        return;
      }
    }
    combo.setSelectedItem(combo.getItemAt(0));
  }

  public Object getValue() {
    return value;
  }

  @Override
  public String toString() {
    return getter.toString();
  }
}
