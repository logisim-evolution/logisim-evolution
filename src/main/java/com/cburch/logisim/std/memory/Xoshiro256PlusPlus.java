/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.std.memory;

/**
 * A deterministic xoshiro256++ state initialized with SplitMix64.
 *
 * @see <a href="https://prng.di.unimi.it/xoshiro256plusplus.c">xoshiro256++ reference</a>
 * @see <a href="https://prng.di.unimi.it/splitmix64.c">SplitMix64 reference</a>
 */
final class Xoshiro256PlusPlus {
  private static final long SPLIT_MIX_GAMMA = 0x9E3779B97F4A7C15L;
  private final long[] state;

  Xoshiro256PlusPlus(long seed) {
    state = initialState(seed);
  }

  static long[] initialState(long seed) {
    final var result = new long[4];
    var splitMixState = seed;
    for (var i = 0; i < result.length; i++) {
      splitMixState += SPLIT_MIX_GAMMA;
      var value = splitMixState;
      value = (value ^ (value >>> 30)) * 0xBF58476D1CE4E5B9L;
      value = (value ^ (value >>> 27)) * 0x94D049BB133111EBL;
      result[i] = value ^ (value >>> 31);
    }
    return result;
  }

  long currentValue() {
    return Long.rotateLeft(state[0] + state[3], 23) + state[0];
  }

  void step() {
    final var shiftedState1 = state[1] << 17;

    state[2] ^= state[0];
    state[3] ^= state[1];
    state[1] ^= state[2];
    state[0] ^= state[3];

    state[2] ^= shiftedState1;
    state[3] = Long.rotateLeft(state[3], 45);
  }
}
