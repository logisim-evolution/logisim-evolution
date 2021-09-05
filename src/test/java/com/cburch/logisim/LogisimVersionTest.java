/*
 * This file is part of logisim-evolution.
 *
 * Logisim-evolution is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 *
 * Logisim-evolution is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with logisim-evolution. If not, see <http://www.gnu.org/licenses/>.
 *
 * Original code by Carl Burch (http://www.cburch.com), 2011.
 * Subsequent modifications by:
 *   + College of the Holy Cross
 *     http://www.holycross.edu
 *   + Haute École Spécialisée Bernoise/Berner Fachhochschule
 *     http://www.bfh.ch
 *   + Haute École du paysage, d'ingénierie et d'architecture de Genève
 *     http://hepia.hesge.ch/
 *   + Haute École d'Ingénierie et de Gestion du Canton de Vaud
 *     http://www.heig-vd.ch/
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
