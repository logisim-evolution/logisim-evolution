/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.cburch.logisim.util.StringUtil;
import org.junit.jupiter.api.Test;

public class StringUtilTest extends TestBase {

  /** Checks if isNotEmpty() correctly handles non-null and non-empty strings. */
  @Test
  public void testIsNotEmptyPositive() {
    assertTrue(StringUtil.isNotEmpty(getRandomString()));
    assertTrue(StringUtil.isNotEmpty("     "));
  }

  /** Checks if isNotEmpty() correctly handles null or empty strings. */
  @Test
  public void testIsNotEmptyNegative() {
    assertFalse(StringUtil.isNotEmpty(null));
    assertFalse(StringUtil.isNotEmpty(""));
  }

  /** Checks if isNullOrEmpty() correctly handles null or empty strings. */
  @Test
  public void testIsNullOrEmptyPositive() {
    assertTrue(StringUtil.isNullOrEmpty(null));
    assertTrue(StringUtil.isNullOrEmpty(""));
  }

  /** Checks if isNullOrEmpty() correctly handles non-empty strings. */
  @Test
  public void testIsNullOrEmptyNegative() {
    assertFalse(StringUtil.isNullOrEmpty(getRandomString()));
  }

  /** Ensures startWith() correctly deals null string to search. */
  @Test
  public void testStartWithForNull() {
    assertFalse(StringUtil.startsWith(null, ""));
  }

  @Test
  public void testStartWithFor() {
    final var haystick = getRandomString();
    final var offset = getRandomInt(3, 6);
    final var needle = haystick.substring(0, getRandomInt(offset, haystick.length() - offset));

    assertTrue(StringUtil.startsWith(haystick, needle));
    assertFalse(StringUtil.startsWith(haystick, needle.substring(1)));
  }
}
