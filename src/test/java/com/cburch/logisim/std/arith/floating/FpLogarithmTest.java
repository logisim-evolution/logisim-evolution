/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.std.arith.floating;

import static com.cburch.logisim.std.Strings.S;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class FpLogarithmTest {
  @Test
  void modeAttributeUsesLogarithmLabel() {
    final var displayName = FpLogarithm.LOG_MODE.getDisplayName();

    assertEquals(S.get("fpLogarithmMode"), displayName);
  }
}
