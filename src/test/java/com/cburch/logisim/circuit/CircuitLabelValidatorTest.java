/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.circuit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class CircuitLabelValidatorTest {
  @Test
  void labelKeysRepresentCurrentCaseInsensitiveIdentity() {
    assertEquals(CircuitLabelValidator.labelKey("A"), CircuitLabelValidator.labelKey("a"));
  }

  @Test
  void labelsMatchIgnoringCase() {
    assertTrue(CircuitLabelValidator.labelsMatch("SegmentA", "segmenta"));
    assertFalse(CircuitLabelValidator.labelsMatch("SegmentA", "SegmentB"));
  }
}
