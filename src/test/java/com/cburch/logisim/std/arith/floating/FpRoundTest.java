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

import com.cburch.logisim.data.AttributeOption;
import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.instance.InstanceState;
import com.cburch.logisim.instance.StdAttr;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;

class FpRoundTest {
  private static double round(int bitWidth, AttributeOption mode, double in) {
    final var width = BitWidth.create(bitWidth);
    final var state = mock(InstanceState.class);
    when(state.getAttributeValue(StdAttr.FP_WIDTH)).thenReturn(width);
    when(state.getAttributeValue(FpToInt.MODE_ATTRIBUTE)).thenReturn(mode);
    when(state.getPortValue(0)).thenReturn(Value.createKnown(width, in));

    new FpRound().propagate(state);

    final var out = ArgumentCaptor.forClass(Value.class);
    verify(state).setPort(eq(1), out.capture(), anyInt());
    return out.getValue().toDoubleValueFromAnyFloat();
  }

  @ParameterizedTest
  @ValueSource(ints = {32, 64})
  void keepsValuesBeyondLongRange(int bitWidth) {
    // These used to go through a long and came out as 9.223372036854776E18
    final var big = bitWidth == 32 ? (double) 1e20f : 1e20;
    assertEquals(big, round(bitWidth, FpToInt.TRUNCATE_OPTION, big));
    assertEquals(big, round(bitWidth, FpToInt.ROUND_OPTION, big));
    assertEquals(-big, round(bitWidth, FpToInt.TRUNCATE_OPTION, -big));
    assertEquals(-big, round(bitWidth, FpToInt.ROUND_OPTION, -big));
  }

  @ParameterizedTest
  @ValueSource(ints = {16, 32, 64})
  void keepsInfinityAndNaN(int bitWidth) {
    for (final var mode : new AttributeOption[] {FpToInt.TRUNCATE_OPTION, FpToInt.ROUND_OPTION}) {
      assertEquals(Double.POSITIVE_INFINITY, round(bitWidth, mode, Double.POSITIVE_INFINITY));
      assertEquals(Double.NEGATIVE_INFINITY, round(bitWidth, mode, Double.NEGATIVE_INFINITY));
      assertEquals(Double.NaN, round(bitWidth, mode, Double.NaN));
    }
  }

  @Test
  void keepsTheSignOfZeroResults() {
    assertEquals(-0.0, round(32, FpToInt.TRUNCATE_OPTION, -0.75));
    assertEquals(-0.0, round(32, FpToInt.ROUND_OPTION, -0.25));
    assertEquals(-0.0, round(32, FpToInt.ROUND_OPTION, -0.5));
  }

  @Test
  void roundsOrdinaryValues() {
    assertEquals(2.0, round(32, FpToInt.TRUNCATE_OPTION, 2.75));
    assertEquals(-2.0, round(32, FpToInt.TRUNCATE_OPTION, -2.75));
    assertEquals(3.0, round(32, FpToInt.ROUND_OPTION, 2.5));
    assertEquals(-2.0, round(32, FpToInt.ROUND_OPTION, -2.5));
    assertEquals(-3.0, round(32, FpToInt.ROUND_OPTION, -2.75));
  }
}
