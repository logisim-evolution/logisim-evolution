/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.gui.log;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.cburch.logisim.data.Value;
import org.junit.jupiter.api.Test;

class SignalTest {

  @Test
  void resetClearsSignalStartOffset() {
    final var info = mock(SignalInfo.class);
    when(info.getWidth()).thenReturn(1);
    final var signal = new Signal(0, info, Value.FALSE, 1, 99, 0);

    signal.reset(Value.TRUE, 10);

    assertEquals(Value.TRUE, signal.getValue(0));
    assertEquals(Value.TRUE, signal.getValue(9));
    assertNull(signal.getValue(10));
    assertEquals(10, signal.getEndTime());
  }
}
