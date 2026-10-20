/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.std.io;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

class MatrixKeypadTest {

  @Test
  void testAttributes() {
    final var keypad = new MatrixKeypad();
    assertNotNull(MatrixKeypad.ATTR_MODE);
    assertEquals("raw", MatrixKeypad.MODE_RAW.getValue());
    assertEquals("pmod_kypd", MatrixKeypad.MODE_PMOD_KYPD.getValue());
  }

  @Test
  void testHdlSupport() {
    final var keypad = new MatrixKeypad();
    final var attrs = keypad.createAttributeSet();

    attrs.setValue(MatrixKeypad.ATTR_MODE, MatrixKeypad.MODE_RAW);
    org.junit.jupiter.api.Assertions.assertFalse(keypad.isHDLSupportedComponent(attrs));

    attrs.setValue(MatrixKeypad.ATTR_MODE, MatrixKeypad.MODE_PMOD_KYPD);
    org.junit.jupiter.api.Assertions.assertTrue(keypad.isHDLSupportedComponent(attrs));
    org.junit.jupiter.api.Assertions.assertNotNull(keypad.getHDLGenerator(attrs));
  }
}
