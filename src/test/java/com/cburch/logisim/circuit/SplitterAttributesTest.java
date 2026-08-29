/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.circuit;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.BitWidth;
import org.junit.jupiter.api.Test;

class SplitterAttributesTest {
  @Test
  void fanoutIncreasePreservesCustomBitMapping() {
    final var attrs = new SplitterAttributes();
    attrs.setValue(SplitterAttributes.ATTR_WIDTH, BitWidth.create(8));
    for (var i = 0; i < 7; i++) {
      setBitEnd(attrs, i, 1);
    }
    setBitEnd(attrs, 7, 2);

    attrs.setValue(SplitterAttributes.ATTR_FANOUT, 3);

    assertArrayEquals(new byte[] {1, 1, 1, 1, 1, 1, 1, 2}, attrs.bitEnd);
  }

  @Test
  void fanoutDecreaseMapsRemovedEndsToLastRemainingEnd() {
    final var attrs = new SplitterAttributes();
    attrs.setValue(SplitterAttributes.ATTR_WIDTH, BitWidth.create(5));
    attrs.setValue(SplitterAttributes.ATTR_FANOUT, 4);
    setBitEnd(attrs, 0, 1);
    setBitEnd(attrs, 1, 2);
    setBitEnd(attrs, 2, 3);
    setBitEnd(attrs, 3, 4);
    setBitEnd(attrs, 4, 4);

    attrs.setValue(SplitterAttributes.ATTR_FANOUT, 3);

    assertArrayEquals(new byte[] {1, 2, 3, 3, 3}, attrs.bitEnd);
  }

  @SuppressWarnings("unchecked")
  private static void setBitEnd(SplitterAttributes attrs, int bit, int end) {
    attrs.setValue((Attribute<Integer>) attrs.getBitOutAttribute(bit), end);
  }
}
