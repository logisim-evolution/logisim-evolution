/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.std.memory;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class Xoshiro256PlusPlusTest {

  @Test
  void splitMix64InitialStateMatchesReferenceVector() {
    assertArrayEquals(
        new long[] {
          0x910A2DEC89025CC1L,
          0xBEEB8DA1658EEC67L,
          0xF893A2EEFB32555EL,
          0x71C18690EE42C90BL
        },
        Xoshiro256PlusPlus.initialState(1));
  }

  @Test
  void xoshiro256PlusPlusSequenceMatchesReferenceVector() {
    final var expected =
        new long[] {
          0xCFC5D07F6F03C29BL,
          0xBF424132963FE08DL,
          0x19A37D5757AAF520L,
          0xBF08119F05CD56D6L,
          0x2F47184B86186FA4L,
          0x97299FCAE7202345L
        };
    final var generator = new Xoshiro256PlusPlus(1);

    for (final var value : expected) {
      assertEquals(value, generator.currentValue());
      generator.step();
    }
  }
}
