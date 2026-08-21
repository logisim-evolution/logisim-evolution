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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cburch.logisim.comp.EndData;
import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.instance.InstanceState;
import com.cburch.logisim.instance.StdAttr;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

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

  @ParameterizedTest
  @ValueSource(ints = {8, 16, 32, 64})
  void leavesErrorClearWhenInverseFunctionOutputsAreNumbers(int bitWidth) {
    final var width = BitWidth.create(bitWidth);
    final var state = mock(InstanceState.class);
    when(state.getAttributeValue(StdAttr.FP_WIDTH)).thenReturn(width);
    when(state.getAttributeValue(FpTrigonometry.TRIG_MODE)).thenReturn(FpTrigonometry.ARC);
    when(state.getPortValue(0)).thenReturn(Value.createKnown(width, 0.5));

    new FpTrigonometry().propagate(state);

    verify(state)
        .setPort(4, Value.FALSE, (width.getWidth() + 2) * FpTrigonometry.PER_DELAY);
  }

  @ParameterizedTest
  @ValueSource(ints = {8, 16, 32, 64})
  void reportsErrorWhenInverseFunctionOutputIsNaN(int bitWidth) {
    final var width = BitWidth.create(bitWidth);
    final var state = mock(InstanceState.class);
    when(state.getAttributeValue(StdAttr.FP_WIDTH)).thenReturn(width);
    when(state.getAttributeValue(FpTrigonometry.TRIG_MODE)).thenReturn(FpTrigonometry.ARC);
    when(state.getPortValue(0)).thenReturn(Value.createKnown(width, 2.0));

    new FpTrigonometry().propagate(state);

    verify(state)
        .setPort(4, Value.TRUE, (width.getWidth() + 2) * FpTrigonometry.PER_DELAY);
  }
}
