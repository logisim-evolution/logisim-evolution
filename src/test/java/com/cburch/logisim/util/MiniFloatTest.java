/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class MiniFloatTest {

  @Test
  void roundingCarriesIntoTheExponent() {
    // 1.111 + 1 in the dropped bits rounds up to the next power of two
    assertEquals((byte) 0x40, MiniFloat.floatToMiniFloat143(1.9375f));
    assertEquals((byte) 0x58, MiniFloat.floatToMiniFloat143(15.5f));
    assertEquals((byte) 0xd8, MiniFloat.floatToMiniFloat143(-15.5f));
    // 248 is halfway between 240 and 256, so it rounds to infinity
    assertEquals((byte) 0x78, MiniFloat.floatToMiniFloat143(248f));
  }

  @Test
  void nanStaysNan() {
    // a payload only in the low significand bits must not turn into infinity
    assertTrue(Float.isNaN(MiniFloat.miniFloat143ToFloat(
        MiniFloat.floatToMiniFloat143(Float.intBitsToFloat(0x7f800001)))));
    assertTrue(Float.isNaN(MiniFloat.miniFloat143ToFloat(
        MiniFloat.floatToMiniFloat143(Float.intBitsToFloat(0xff8fffff)))));
    assertTrue(Float.isNaN(MiniFloat.miniFloat143ToFloat(MiniFloat.floatToMiniFloat143(Float.NaN))));
  }

  @Test
  void everyEncodingRoundTrips() {
    for (int b = 0; b < 256; b++) {
      final var f = MiniFloat.miniFloat143ToFloat((byte) b);
      final var back = MiniFloat.floatToMiniFloat143(f);
      if (Float.isNaN(f)) {
        assertTrue(Float.isNaN(MiniFloat.miniFloat143ToFloat(back)), "encoding " + b);
      } else {
        assertEquals((byte) b, back, "encoding " + b);
      }
    }
  }
}
