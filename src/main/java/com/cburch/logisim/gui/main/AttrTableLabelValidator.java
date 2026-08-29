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
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.prefs.AppPreferences;
import com.cburch.logisim.util.SyntaxChecker;
import java.util.Objects;

final class AttrTableLabelValidator {
  private AttrTableLabelValidator() {}

  static boolean shouldApply(
      Circuit circuit, Component component, Attribute<?> attribute, Object requestedValue) {
    if (!StdAttr.LABEL.equals(attribute)) return true;
    if (circuit == null || component == null || !(requestedValue instanceof String label)) {
      return false;
    }

    final var hdlType = AppPreferences.HdlType.get();
    final var labelIsUnique =
        Circuit.isCorrectLabelForCurrentHdl(
            circuit.getName(),
            label,
            circuit.getNonWires(),
            component.getAttributeSet(),
            component.getFactory(),
            true);
    return labelIsUnique
        && SyntaxChecker.isVariableNameAcceptable(label, hdlType, true)
        && !Objects.equals(component.getAttributeSet().getValue(StdAttr.LABEL), label);
  }
}
