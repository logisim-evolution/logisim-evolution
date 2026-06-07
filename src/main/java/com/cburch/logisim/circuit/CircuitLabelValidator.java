/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.circuit;

import java.util.Locale;

final class CircuitLabelValidator {
  private CircuitLabelValidator() {}

  static String labelKey(String label) {
    return label.toUpperCase(Locale.ROOT);
  }

  static boolean labelsMatch(String first, String second) {
    return first.equalsIgnoreCase(second);
  }
}
