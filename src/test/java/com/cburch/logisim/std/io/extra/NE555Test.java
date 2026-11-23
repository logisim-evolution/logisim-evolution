/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.std.io.extra;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

/** Saved ohms and microfarads for the NE555, including engineering notation. */
class NE555Test {
  @Test
  void resistorAcceptsEngineeringNotationAndKeepsOhmsInTheFile() {
    final var resistor = NE555.ATTR_R1;
    assertEquals(NE555.DEFAULT_R1_OHM, resistor.parse("71660"));
    assertEquals(NE555.DEFAULT_R1_OHM, resistor.parse("71.66k"));
    assertEquals(NE555.DEFAULT_R1_OHM, resistor.parse("71.66 kΩ"));
    assertEquals(NE555.MIN_R1_OHM, resistor.parse("1k"));
    assertEquals(NE555.MAX_R1_OHM, resistor.parse("10M"));
    assertEquals("71660", resistor.toStandardString(NE555.DEFAULT_R1_OHM));
    assertEquals(
        NE555.DEFAULT_R1_OHM, resistor.parse(resistor.toDisplayString(NE555.DEFAULT_R1_OHM)));
    assertThrows(NumberFormatException.class, () -> resistor.parse("999"));
    assertThrows(NumberFormatException.class, () -> resistor.parse("10.1M"));
  }

  @Test
  void capacitorStaysAWholeNumberOfMicrofarads() {
    final var capacitor = NE555.ATTR_C1;
    assertEquals(10, capacitor.parse("10"));
    assertEquals(10, capacitor.parse("10u"));
    assertEquals(10, capacitor.parse("10 µF"));
    assertEquals(NE555.MAX_C1_UF, capacitor.parse("1000u"));
    assertEquals("10", capacitor.toStandardString(10));
    assertEquals("10 µF", capacitor.toDisplayString(10));
    assertEquals(10, capacitor.parse(capacitor.toDisplayString(10)));
    assertThrows(NumberFormatException.class, () -> capacitor.parse("10.5µF"));
    assertThrows(NumberFormatException.class, () -> capacitor.parse("9"));
    assertThrows(NumberFormatException.class, () -> capacitor.parse("1001"));
  }
}
