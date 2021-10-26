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

import com.cburch.logisim.util.CollectionUtil;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

public class CollectionUtilTest extends TestBase {

  /**
   * Checks if isNotEmpty() correctly handles non-null and non-empty collections.
   */
  @Test
  public void testIsNotEmptyPositive() {
    final var collection = List.of(getRandomString());
    assertTrue(CollectionUtil.isNotEmpty(collection));
  }

  /**
   * Checks if isNotEmpty() correctly handles null or empty collections.
   */
  @Test
  public void testIsNotEmptyNegative() {
    assertFalse(CollectionUtil.isNotEmpty(null));

    final var emptyCollection = new ArrayList<String>();
    assertFalse(CollectionUtil.isNotEmpty(emptyCollection));
  }

  /**
   * Checks if isNullOrEmpty() correctly handles null or empty collections.
   */
  @Test
  public void testIsNullOrEmptyPositive() {
    assertTrue(CollectionUtil.isNullOrEmpty(null));
    assertTrue(CollectionUtil.isNullOrEmpty(new ArrayList()));
  }

  /**
   * Checks if isNullOrEmpty() correctly handles non-empty collections.
   */
  @Test
  public void testIsNullOrEmptyNegative() {
    final var nonEmptyCollection = List.of(getRandomString());
    assertFalse(CollectionUtil.isNullOrEmpty(nonEmptyCollection));
  }
}
