/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.std.memory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.instance.Instance;
import com.cburch.logisim.instance.InstanceData;
import com.cburch.logisim.instance.InstanceLogger;
import com.cburch.logisim.instance.InstanceState;
import com.cburch.logisim.instance.StdAttr;
import org.junit.jupiter.api.Test;

class RamLoggerTest {

  @Test
  void ramAddressLogOptionsReadMemoryValues() {
    final var contents = memoryContents();
    final var logger = new Ram.Logger();
    final var state = memoryState(new RamState(null, contents, listener()));

    assertAddressOptionReadsValue(logger, state);
  }

  @Test
  void dualRamAddressLogOptionsReadMemoryValues() {
    final var contents = memoryContents();
    final var logger = new DualRam.Logger();
    final var state = memoryState(new RamState(null, contents, listener()));

    assertAddressOptionReadsValue(logger, state);
  }

  private static void assertAddressOptionReadsValue(InstanceLogger logger, InstanceState state) {
    final var option = logger.getLogOptions(state)[3];

    assertEquals("Memory[3]", logger.getLogName(state, option));
    assertEquals(Value.createKnown(BitWidth.create(8), 0x5a), logger.getLogValue(state, option));
  }

  private static MemContents memoryContents() {
    final var contents = MemContents.create(4, 8, false);
    contents.set(3, 0x5a);
    return contents;
  }

  private static InstanceState memoryState(InstanceData memoryState) {
    final var state = mock(InstanceState.class);
    when(state.getAttributeValue(StdAttr.LABEL)).thenReturn("Memory");
    when(state.getAttributeValue(Mem.ADDR_ATTR)).thenReturn(BitWidth.create(4));
    when(state.getAttributeValue(Mem.DATA_ATTR)).thenReturn(BitWidth.create(8));
    when(state.getData()).thenReturn(memoryState);
    return state;
  }

  private static Mem.MemListener listener() {
    return new Mem.MemListener(mock(Instance.class));
  }
}
