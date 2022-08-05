/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import org.junit.jupiter.api.Test;

/** Tests LogisimVersion class. */
public class LogisimVersionTest extends TestBase {

  private LogisimVersion newA;
  private LogisimVersion newerB;
  private LogisimVersion newerToo;

  /** Test method for {@link com.cburch.logisim.LogisimVersion#fromString(java.lang.String)}. */
  @Test
  public void testFromString() {
    final String[] tests = {
      "1.2.3-beta1", "1.2.3beta1", "1.2.3",
    };
    for (final var test : tests) {
      var vfs = LogisimVersion.fromString(test);
      assertNotNull(vfs);
      assertEquals(test, vfs.toString());
      // Should return a new object
      assertNotSame(vfs, LogisimVersion.fromString(test));
    }
  }

  /** Ensures default implementation for toString() uses no dash separator even if suffix is set. */
  @Test
  public void testToStringReturnsNoSeparator() {
    var major = getRandomInt(0, 10);
    var minor = getRandomInt(0, 10);
    var patch = getRandomInt(0, 10);
    var lsv = new LogisimVersion(major, minor, patch);
    var exp = String.format("%d.%d.%d", major, minor, patch);
    assertEquals(exp, lsv.toString());

    major = getRandomInt(0, 10);
    minor = getRandomInt(0, 10);
    patch = getRandomInt(0, 10);
    var suffix = getRandomString(false);
    lsv = new LogisimVersion(major, minor, patch, suffix);
    exp = String.format("%d.%d.%d%s", major, minor, patch, suffix);
    assertEquals(exp, lsv.toString());
  }

  /** Ensures toString() preserves dash separator if original source version string had one. */
  @Test
  public void testToStringPreservesGivenSeparator() {
    for (var i = 0; i < 10; i++) {
      var major = getRandomInt(0, 10);
      var minor = getRandomInt(0, 10);
      var patch = getRandomInt(0, 10);
      var sep = (getRandomInt(0, 1) == 1) ? "-" : "";
      var suffix = getRandomString(false);
      var exp = String.format("%d.%d.%d%s%s", major, minor, patch, sep, suffix);
      var lsv = LogisimVersion.fromString(exp);
      assertEquals(exp, lsv.toString());
    }
  }

  /** Tests if one letter suffix (i.e. 1.2.3a) is handled properly */
  @Test
  public void testFromStringHandlesOneLetterSuffix() {
    final var major = getRandomInt(0, 10);
    final var minor = getRandomInt(0, 10);
    final var patch = getRandomInt(0, 10);
    final var suffix = getRandomString(1, false);

    var exp = String.format("%d.%d.%d%s", major, minor, patch, suffix);
    var lsv = LogisimVersion.fromString(exp);
    assertEquals(exp, lsv.toString());
  }

  /**
   * Ensures version string is properly validated and invalid format causes exception to be thrown.
   */
  @Test
  public void testToStringInvalidVersionStrings() {
    // Given the set of invalid version stsrings
    final String[] tests = {
      "1.1.2_tvTpQEMCNVUAVI", // "_" is not a valid separator
      "1.1.2-8GtvTpQEMCNVUAVI", // "-" is valid separator, but suffix must start with a letter.
      "1.1.2-", // Too short
    };

    for (final var test : tests) {
      assertThrows(
          IllegalArgumentException.class,
          () -> {
            var vfs = LogisimVersion.fromString(test);
          });
    }
  }

  /**
   * Test method for {@link
   * com.cburch.logisim.LogisimVersion#compareTo(com.cburch.logisim.LogisimVersion)} .
   */
  @Test
  public void testCompareTo() {
    newA = new LogisimVersion(1, 2, 3);
    newerB = new LogisimVersion(1, 2, 4);
    newerToo = new LogisimVersion(1, 2, 4);

    assertTrue(newA.compareTo(newerB) < 0);
    assertEquals(0, newerB.compareTo(newerB));
    assertEquals(0, newerB.compareTo(newerToo));
    assertTrue(newerB.compareTo(newA) > 0);
  }

  /** Tests if compareTo() properly compares versions with suffix. */
  @Test
  public void testCompareToWithSuffix() {
    newA = new LogisimVersion(1, 2, 3, "beta1");
    // "newer" is stable version, so "older" than any unbstable build with sufifx
    newerB = new LogisimVersion(1, 2, 3);

    assertTrue(newA.compareTo(newerB) < 0);
    assertEquals(0, newerB.compareTo(newerB));
    assertTrue(newerB.compareTo(newA) > 0);
  }

  @Test
  public void testCompareToObjectWithSuffix() {
    newA = new LogisimVersion(1, 2, 3, "beta1");
    newerB = new LogisimVersion(1, 2, 3, "RC1");
    // Both objects' versions are equally weighted.
    assertEquals(0, newA.compareTo(newerB));
  }

  /** Test method for {@link com.cburch.logisim.LogisimVersion#equals(java.lang.Object)}. */
  @Test
  public void testEqualsObject() {
    newA = new LogisimVersion(1, 2, 3);
    newerB = new LogisimVersion(1, 2, 4);
    assertEquals(newA, newA);
    assertNotEquals(newA, newerB);
  }

  /** Checks if result of isStable() matche expectations. */
  @Test
  public void testIsStable() {
    final var tests =
        new HashMap<String, Boolean>() {
          {
            put("1.2.3", true);
            put("1.2.3a", false);
            put("1.2.3-rc1", false);
          }
        };
    for (final var test : tests.entrySet()) {
      final var version = LogisimVersion.fromString(test.getKey());
      assertEquals(test.getValue(), version.isStable());
    }
  }
}
