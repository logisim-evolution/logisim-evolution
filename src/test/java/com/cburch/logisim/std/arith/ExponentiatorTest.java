/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.std.arith;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Value;
import java.math.BigInteger;
import org.junit.jupiter.api.Test;

public class ExponentiatorTest {

  private static void check(int width, long base, long exp, boolean unsigned) {
    final var w = BitWidth.create(width);
    final var res =
        Exponentiator.computePower(
            w, Value.createKnown(w, base), Value.createKnown(w, exp), unsigned);
    final var mask = BigInteger.ONE.shiftLeft(width).subtract(BigInteger.ONE);
    final var full =
        BigInteger.valueOf(base)
            .and(mask)
            .modPow(BigInteger.valueOf(exp).and(mask), BigInteger.ONE.shiftLeft(2 * width));
    assertEquals(full.and(mask).longValue(), res[0].toLongValue());
    assertEquals(full.shiftRight(width).and(mask).longValue(), res[1].toLongValue());
  }

  @Test
  public void testLargeUnsignedExponent() {
    // The exponent used to be cast to int, so these gave 1 or did not fit in a BigInteger
    check(32, 3, 0x80000000L, true);
    check(32, 3, 0x7fffffffL, true);
    check(64, 5, -1L, true);
    check(40, 7, (1L << 39) + 1, true);
  }

  @Test
  public void testSmallExponent() {
    check(32, 3, 13, true);
    check(48, 3, 13, false);
    check(64, 12345, 7, true);
  }

  @Test
  public void testNegativeExponentIsZero() {
    final var w40 = BitWidth.create(40);
    final var res =
        Exponentiator.computePower(
            w40, Value.createKnown(w40, 3), Value.createKnown(w40, -1), false);
    assertEquals(0, res[0].toLongValue());
    assertEquals(0, res[1].toLongValue());
  }
}
