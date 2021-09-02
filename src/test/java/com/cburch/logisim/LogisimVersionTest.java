/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import org.junit.Before;
import org.junit.Test;

/** Tests LogisimVersion class. */
public class LogisimVersionTest {

  private LogisimVersion older;
  private LogisimVersion newer;
  private LogisimVersion newerToo;

  /** Test method for {@link com.cburch.logisim.LogisimVersion#fromString(java.lang.String)}. */
  @Test
  public void testFromString() {
    String[] tests = {"1.2.3", "1.2.3-beta1"};
    for (var test : tests) {
      assertNotNull(LogisimVersion.fromString(test));
      assertEquals(test, LogisimVersion.fromString(test).toString());
      // Should return a new object
      assertNotSame(LogisimVersion.fromString(test), LogisimVersion.fromString(test));
    }
  }

  /**
   * Test method for {@link
   * com.cburch.logisim.LogisimVersion#compareTo(com.cburch.logisim.LogisimVersion)} .
   */
  @Test
  public void testCompareTo() {
    older = new LogisimVersion(1, 2, 3);
    newer = new LogisimVersion(1, 2, 4);
    newerToo = new LogisimVersion(1, 2, 4);

    assertTrue(older.compareTo(newer) < 0);
    assertEquals(0, newer.compareTo(newer));
    assertEquals(0, newer.compareTo(newerToo));
    assertTrue(newer.compareTo(older) > 0);
  }

  @Test
  public void testCompareToWithSuffix() {
    older = new LogisimVersion(1, 2, 3, "beta1");
    newer = new LogisimVersion(1, 2, 3);

    assertTrue(older.compareTo(newer) < 0);
    assertEquals(0, newer.compareTo(newer));
    assertTrue(newer.compareTo(older) > 0);
  }

  @Test
  public void testCompareToObjectWithSuffix() {
    older = new LogisimVersion(1, 2, 3, "beta1");
    newer = new LogisimVersion(1, 2, 3, "RC1");
    assertEquals(0, older.compareTo(newer));
  }

  /** Test method for {@link com.cburch.logisim.LogisimVersion#equals(java.lang.Object)}. */
  @Test
  public void testEqualsObject() {
    older = new LogisimVersion(1, 2, 3);
    newer = new LogisimVersion(1, 2, 4);
    assertEquals(older, older);
    assertNotEquals(older, newer);
  }

  @Test
  public void testIsStable() {
    final var tests =
        new HashMap<String, Boolean>() {
          {
            put("1.2.3", true);
            put("1.2.3-rc1", false);
          }
        };
    for (final var test : tests.entrySet()) {
      final var version = LogisimVersion.fromString(test.getKey());
      assertEquals(test.getValue(), version.isStable());
    }
  }
}
