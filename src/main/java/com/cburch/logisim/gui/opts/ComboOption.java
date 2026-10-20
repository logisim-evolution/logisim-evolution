/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.gui.opts;

import com.cburch.logisim.data.AttributeOption;
import com.cburch.logisim.util.StringGetter;
import javax.swing.JComboBox;

class ComboOption {
  private final Object value;
  private final StringGetter getter;

  ComboOption(AttributeOption value) {
    this.value = value;
    this.getter = null;
  }

  ComboOption(String value, StringGetter getter) {
    this.value = value;
    this.getter = getter;
  }

  @SuppressWarnings("rawtypes")
  static void setSelected(JComboBox combo, Object value) {
    for (int i = combo.getItemCount() - 1; i >= 0; i--) {
      ComboOption opt = (ComboOption) combo.getItemAt(i);
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
    if (getter != null) return getter.toString();
    if (value instanceof AttributeOption) return ((AttributeOption) value).toDisplayString();
    return "???";
  }
}
