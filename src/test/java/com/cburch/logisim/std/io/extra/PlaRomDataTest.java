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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.cburch.logisim.data.Value;
import org.junit.jupiter.api.Test;

class PlaRomDataTest {
  private static final String LEGACY_CONTENTS = "1 2 0 1 1 0 0 1 ";

  @Test
  void legacyContentsUseTheComponentDimensions() {
    final var data = new PlaRomData((byte) 2, (byte) 2, (byte) 2);

    data.decodeSavedData(LEGACY_CONTENTS);

    assertEquals(LEGACY_CONTENTS, data.getSavedData());
    assertTrue(data.getInputAndValue(0, 0));
    assertTrue(data.getInputAndValue(0, 3));
    assertTrue(data.getInputAndValue(1, 2));
    assertTrue(data.getAndOutputValue(0, 0));
    assertTrue(data.getAndOutputValue(1, 1));
    assertFalse(data.getAndOutputValue(0, 1));

    data.decodeSavedData("0*8 ");
    assertFalse(data.getInputAndValue(0, 0));
    assertFalse(data.getAndOutputValue(1, 1));
  }

  @Test
  void clonedSimulationStateDoesNotShareMatricesOrInputValues() {
    final var original = new PlaRomData((byte) 2, (byte) 2, (byte) 2);
    original.decodeSavedData(LEGACY_CONTENTS);

    final var copy = original.clone();
    copy.setInputAndValue(0, 0, false);
    copy.setAndOutputValue(1, 1, false);
    copy.setInputsValue(new Value[] {Value.TRUE, Value.FALSE});

    assertTrue(original.getInputAndValue(0, 0));
    assertTrue(original.getAndOutputValue(1, 1));
    assertEquals(Value.UNKNOWN, original.getInputValue((byte) 0));
  }
}
