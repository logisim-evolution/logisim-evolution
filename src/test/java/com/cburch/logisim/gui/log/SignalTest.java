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

  @Test
  void testReplaceRecentBoundaryChunkDeallocation() {
    final var info = mock(SignalInfo.class);
    when(info.getWidth()).thenReturn(1);
    final var signal = new Signal(0, info, Value.FALSE, 10, 0, 0);

    // Fill buffer with alternating values up to chunk boundary (512 elements)
    for (int i = 0; i < 512; i++) {
      signal.extend((i % 2 == 0) ? Value.TRUE : Value.FALSE, 10);
    }

    // Coalesce / deduplicate on the boundary
    org.junit.jupiter.api.Assertions.assertDoesNotThrow(() -> signal.replaceRecent(Value.FALSE, 10));

    // Next extend must not throw ArrayIndexOutOfBoundsException
    org.junit.jupiter.api.Assertions.assertDoesNotThrow(() -> signal.extend(Value.TRUE, 10));
    org.junit.jupiter.api.Assertions.assertNotNull(signal.getValue(signal.getEndTime() - 5));
  }

  @Test
  void testInitialSignalIteratorSafety() {
    final var info = mock(SignalInfo.class);
    when(info.getWidth()).thenReturn(1);
    final var signal = new Signal(0, info, Value.FALSE, 10, 0, 0);

    final var it = signal.new Iterator();
    org.junit.jupiter.api.Assertions.assertNotNull(it);
    org.junit.jupiter.api.Assertions.assertEquals(Value.FALSE, it.value);
  }

  @Test
  void testReplaceRecentUnwindingTimeInvariant() {
    final var info = mock(SignalInfo.class);
    when(info.getWidth()).thenReturn(1);
    final var signal = new Signal(0, info, Value.FALSE, 100, 50, 0); // starts at 50, dur 100 -> end 150
    signal.extend(Value.TRUE, 100); // end 250

    // Replace recent 300 ns (exceeds existing 200 ns total data, fully unwinds and rewinds start)
    signal.replaceRecent(Value.UNKNOWN, 300);

    // End time must be preserved at 250, start time shifted back to -50
    org.junit.jupiter.api.Assertions.assertEquals(250, signal.getEndTime());
    org.junit.jupiter.api.Assertions.assertEquals(Value.UNKNOWN, signal.getValue(-50));
    org.junit.jupiter.api.Assertions.assertEquals(Value.UNKNOWN, signal.getValue(249));
    org.junit.jupiter.api.Assertions.assertNull(signal.getValue(250));
  }

  @Test
  void testReplaceRecentMultipleChunksUnwinding() {
    final var info = mock(SignalInfo.class);
    when(info.getWidth()).thenReturn(1);
    final var signal = new Signal(0, info, Value.FALSE, 10, 0, 0);

    // Add multiple entries
    signal.extend(Value.TRUE, 20);
    signal.extend(Value.FALSE, 30);
    signal.extend(Value.TRUE, 40); // total 100 ns (0..100)

    // Unwind 60 ns (removes 40 ns of TRUE, and 20 ns of FALSE leaving 10 ns of FALSE)
    signal.replaceRecent(Value.UNKNOWN, 60);

    assertEquals(100, signal.getEndTime());
    assertEquals(Value.FALSE, signal.getValue(0));
    assertEquals(Value.TRUE, signal.getValue(15));
    assertEquals(Value.FALSE, signal.getValue(35));
    assertEquals(Value.UNKNOWN, signal.getValue(40));
    assertEquals(Value.UNKNOWN, signal.getValue(99));
    assertNull(signal.getValue(100));
  }

  @Test
  void testEmptySignalIteratorAndAdvance() {
    final var info = mock(SignalInfo.class);
    when(info.getWidth()).thenReturn(1);
    // Initialized with 0 duration and initialValue null
    final var signal = new Signal(0, info, null, 0, 0, 0);

    final var it = signal.new Iterator();
    assertNull(it.value);
    assertEquals(0, it.duration);
    org.junit.jupiter.api.Assertions.assertFalse(it.advance());
    org.junit.jupiter.api.Assertions.assertFalse(it.advance(10));
    assertNull(signal.getValue(0));
  }
}
