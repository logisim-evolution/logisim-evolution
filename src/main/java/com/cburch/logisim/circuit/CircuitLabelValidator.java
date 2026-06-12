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
  enum LabelIdentity {
    HDL_COMPATIBLE,
    CASE_SENSITIVE
  }

  private CircuitLabelValidator() {}

  static String labelKey(String label) {
    return labelKey(label, LabelIdentity.HDL_COMPATIBLE);
  }

  static String labelKey(String label, LabelIdentity identity) {
    if (identity == LabelIdentity.CASE_SENSITIVE) return label;
    return label.toUpperCase(Locale.ROOT);
  }

  static boolean labelsMatch(String first, String second) {
    return labelsMatch(first, second, LabelIdentity.HDL_COMPATIBLE);
  }

  static boolean labelsMatch(String first, String second, LabelIdentity identity) {
    if (identity == LabelIdentity.CASE_SENSITIVE) return first.equals(second);
    return first.equalsIgnoreCase(second);
  }
}
