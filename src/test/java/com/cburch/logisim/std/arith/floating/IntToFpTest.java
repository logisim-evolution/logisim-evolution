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
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.instance.InstanceState;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.std.arith.Comparator;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class IntToFpTest {
  private static Value convert(int fpWidth, boolean unsigned, long in) {
    final var width = BitWidth.create(64);
    final var state = mock(InstanceState.class);
    when(state.getAttributeValue(StdAttr.WIDTH)).thenReturn(width);
    when(state.getAttributeValue(StdAttr.FP_WIDTH)).thenReturn(BitWidth.create(fpWidth));
    when(state.getAttributeValue(Comparator.MODE_ATTR))
        .thenReturn(unsigned ? Comparator.UNSIGNED_OPTION : Comparator.SIGNED_OPTION);
    when(state.getPortValue(0)).thenReturn(Value.createKnown(width, in));

    new IntToFp().propagate(state);

    final var out = ArgumentCaptor.forClass(Value.class);
    verify(state).setPort(eq(1), out.capture(), anyInt());
    return out.getValue();
  }

  @Test
  void roundsWideIntegersToFloatOnce() {
    // 2^62 + 2^38 + 1 lies just above the midpoint between two floats, but as a double it
    // becomes the midpoint itself and then rounds down to even
    final var in = (1L << 62) + (1L << 38) + 1;
    assertEquals(0x5e800001, convert(32, false, in).toLongValue());
    assertEquals(0xde800001L, convert(32, false, -in).toLongValue());
    // The same for 2^64 - 3 * 2^39 + 1 as an unsigned input
    assertEquals(0x5f7fffff, convert(32, true, -(3L << 39) + 1).toLongValue());
  }

  @Test
  void keepsExactValues() {
    assertEquals(0x1p24, convert(32, false, 1 << 24).toDoubleValueFromAnyFloat());
    assertEquals(-3.0, convert(32, false, -3).toDoubleValueFromAnyFloat());
    assertEquals(0x1p62, convert(64, false, 1L << 62).toDoubleValueFromAnyFloat());
  }
}
