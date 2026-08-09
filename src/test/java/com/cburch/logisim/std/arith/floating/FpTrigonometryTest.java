/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.std.arith.floating;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.cburch.logisim.comp.EndData;
import org.junit.jupiter.api.Test;

class FpTrigonometryTest {
  @Test
  void functionResultPortsAreOutputs() {
    final var ports = new FpTrigonometry().getPorts();

    assertEquals(EndData.INPUT_ONLY, ports.get(0).getType());
    assertEquals(EndData.OUTPUT_ONLY, ports.get(1).getType());
    assertEquals(EndData.OUTPUT_ONLY, ports.get(2).getType());
    assertEquals(EndData.OUTPUT_ONLY, ports.get(3).getType());
    assertEquals(EndData.OUTPUT_ONLY, ports.get(4).getType());
  }
}
