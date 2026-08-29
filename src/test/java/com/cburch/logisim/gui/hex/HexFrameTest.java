/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.gui.hex;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.cburch.hex.HexModel;
import com.cburch.hex.HexModelListener;
import org.junit.jupiter.api.Test;

class HexFrameTest {
  @Test
  void parseAddressAcceptsHexWithOptionalPrefix() {
    final var model = new RangeModel(0, 0x2f);

    assertEquals(0x1a, HexFrame.parseAddress("1a", model));
    assertEquals(0x1a, HexFrame.parseAddress("1A", model));
    assertEquals(0x1a, HexFrame.parseAddress("0x1a", model));
    assertEquals(0x1a, HexFrame.parseAddress("  0X1A  ", model));
  }

  @Test
  void parseAddressRequiresAddressWithinModelRange() {
    final var model = new RangeModel(0x10, 0x2f);

    assertEquals(0x10, HexFrame.parseAddress("10", model));
    assertEquals(0x2f, HexFrame.parseAddress("2f", model));
    assertThrows(NumberFormatException.class, () -> HexFrame.parseAddress("f", model));
    assertThrows(NumberFormatException.class, () -> HexFrame.parseAddress("30", model));
  }

  @Test
  void parseAddressRejectsBlankOrInvalidInput() {
    final var model = new RangeModel(0, 0x2f);

    assertThrows(NumberFormatException.class, () -> HexFrame.parseAddress("", model));
    assertThrows(NumberFormatException.class, () -> HexFrame.parseAddress("0x", model));
    assertThrows(NumberFormatException.class, () -> HexFrame.parseAddress("not-hex", model));
  }

  private static class RangeModel implements HexModel {
    private final long firstOffset;
    private final long lastOffset;

    RangeModel(long firstOffset, long lastOffset) {
      this.firstOffset = firstOffset;
      this.lastOffset = lastOffset;
    }

    @Override
    public void addHexModelListener(HexModelListener l) {}

    @Override
    public void fill(long start, long length, long value) {}

    @Override
    public long get(long address) {
      return 0;
    }

    @Override
    public long getFirstOffset() {
      return firstOffset;
    }

    @Override
    public long getLastOffset() {
      return lastOffset;
    }

    @Override
    public int getValueWidth() {
      return 8;
    }

    @Override
    public void removeHexModelListener(HexModelListener l) {}

    @Override
    public void set(long address, long value) {}

    @Override
    public void set(long start, long[] values) {}
  }
}
