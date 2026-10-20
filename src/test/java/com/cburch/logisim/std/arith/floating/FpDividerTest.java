/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.std.arith.floating;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.instance.InstanceState;
import com.cburch.logisim.instance.StdAttr;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class FpDividerTest {
  @ParameterizedTest
  @ValueSource(ints = {8, 16, 32, 64})
  void leavesErrorClearWhenBothOutputsAreNumbers(int bitWidth) {
    final var width = BitWidth.create(bitWidth);
    final var state = mock(InstanceState.class);
    when(state.getAttributeValue(StdAttr.FP_WIDTH)).thenReturn(width);
    when(state.getPortValue(0)).thenReturn(Value.createKnown(width, 6.0));
    when(state.getPortValue(1)).thenReturn(Value.createKnown(width, 4.0));

    new FpDivider().propagate(state);

    verify(state)
        .setPort(4, Value.FALSE, (width.getWidth() + 2) * FpDivider.PER_DELAY);
  }

  @ParameterizedTest
  @ValueSource(ints = {8, 16, 32, 64})
  void reportsErrorWhenRemainderIsNaN(int bitWidth) {
    final var width = BitWidth.create(bitWidth);
    final var state = mock(InstanceState.class);
    when(state.getAttributeValue(StdAttr.FP_WIDTH)).thenReturn(width);
    when(state.getPortValue(0)).thenReturn(Value.createKnown(width, 1.0));
    when(state.getPortValue(1)).thenReturn(Value.createKnown(width, 0.0));

    new FpDivider().propagate(state);

    verify(state)
        .setPort(4, Value.TRUE, (width.getWidth() + 2) * FpDivider.PER_DELAY);
  }
}
