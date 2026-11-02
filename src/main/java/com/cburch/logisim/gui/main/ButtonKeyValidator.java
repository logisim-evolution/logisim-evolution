/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.gui.main;

import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.comp.Component;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeOption;
import com.cburch.logisim.gui.generic.OptionPane;
import com.cburch.logisim.std.io.Button;
import java.util.ArrayList;
import java.util.Collection;

final class ButtonKeyValidator {
  private ButtonKeyValidator() {}

  static boolean shouldApply(
      Circuit circuit, Collection<Component> selection, Attribute<?> attribute, Object value) {
    if (!Button.ATTR_KEY.equals(attribute)) return true;
    if (!(value instanceof AttributeOption option) || isNone(option)) return true;
    if (circuit == null || selection == null) return false;

    final var buttons = new ArrayList<Component>();
    for (final var comp : selection) {
      if (comp.getFactory() instanceof Button) buttons.add(comp);
    }
    if (buttons.isEmpty()) return true;
    if (buttons.size() == 1 && !Button.isKeyUsedByOther(circuit, buttons.get(0), option)) {
      return true;
    }
    OptionPane.showMessageDialog(
        null, com.cburch.logisim.std.Strings.S.get("buttonKeyDuplicate"));
    return false;
  }

  private static boolean isNone(AttributeOption option) {
    return Button.KEY_NONE.toString().equals(option.toString());
  }
}
