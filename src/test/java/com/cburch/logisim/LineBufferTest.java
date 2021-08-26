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

import com.cburch.logisim.std.memory.Random;
import com.cburch.logisim.util.LineBuffer;
import java.util.HashMap;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;

/** Tests LogisimVersion class. */
public class LineBufferTest extends TestBase {

  private LineBuffer lb;

  @Before
  public void SetUp() {
    lb = new LineBuffer();
    assertEquals(0, lb.size());
  }

  @Test
  public void testClear() {
    final var val = getRandomString();
    this.lb.add(val);
    assertEquals(1, lb.size());

    this.lb.clear();
    assertEquals(0, lb.size());
  }

  @Test
  public void testSize() {
    final var itemCount = getRandomInt(10, 20);
    for (var i = 0; i < itemCount; i++) {
      this.lb.add(getRandomString());
    }
    assertEquals(itemCount, this.lb.size());
  }

  /* ********************************************************************************************* */

  @Test
  public void testFormatter() {
    final Map<String, String> tests =
        Map.of(
            " {{foo}} ", " FOO ",
            " {{bar}} ", " BAR ",
            " {{foo}} {{bar}} ", " FOO BAR ",
            " {{bar  }} ", " BAR ",
            " {{   bar  }} ", " BAR ",
            " {{   bar}} ", " BAR ");

    final var pairs =
        new LineBuffer.Pairs() {
          {
            add("foo", "FOO");
            add("bar", "BAR");
          }
        };

    for (final var test : tests.entrySet()) {
      final var lb = new LineBuffer();
      lb.add(test.getKey(), pairs);
      final var expected = new LineBuffer(test.getValue());
      assertEquals(expected, lb);
    }
  }

  @Test
  public void testFormatterWithGlobalPairs() {
    final Map<String, String> tests =
        Map.of(
            " {{foo}} ", " FOO ",
            " {{bar}} ", " BAR ",
            " {{foo}} {{bar}} ", " FOO BAR ",
            " {{bar  }} ", " BAR ",
            " {{   bar  }} ", " BAR ",
            " {{   bar}} ", " BAR ");

    final var pairs =
        new LineBuffer.Pairs() {
          {
            add("foo", "FOO");
            add("bar", "BAR");
          }
        };

    for (final var test : tests.entrySet()) {
      final var lb = new LineBuffer(pairs);
      lb.add(test.getKey());
      final var expected = new LineBuffer(test.getValue(), pairs);
      assertEquals(expected, lb);
    }
  }

  @Test
  public void testFormatterMixedPairs() {
    final Map<String, String> tests =
        Map.of(
            " {{foo}} ", " FOO ",
            " {{bar}} ", " BAR ",
            " {{foo}} {{bar}} ", " FOO BAR ",
            " {{bar  }} ", " BAR ",
            " {{   bar  }} ", " BAR ",
            " {{   bar}} ", " BAR ",
            " {{bang}} ", " BANG ");

    final var pairs =
        new LineBuffer.Pairs() {
          {
            add("foo", "FOO");
            add("bar", "BAR");
          }
        };

    for (final var test : tests.entrySet()) {
      final var lb = new LineBuffer(pairs);
      lb.add(test.getKey(), new LineBuffer.Pairs("bang", "BANG"));

      final var expPairs =
          new LineBuffer.Pairs() {
            {
              add("foo", "FOO");
              add("bar", "BAR");
              add("bang", "BANG");
            }
          };

      final var expected = new LineBuffer(test.getValue(), expPairs);
      assertEquals(expected, lb);
    }
  }

  /* ********************************************************************************************* */

  @Test
  public void testGetWithIndent() {
    final var indentSize = getRandomInt(2, 6);
    final var indent = getRandomString(indentSize);

    final var line = getRandomString();
    this.lb.add(line);
    final var result = this.lb.getWithIndent(indent).get(0);
    assertEquals(indentSize + line.length(), result.length());
    assertEquals(indent + line, result);
  }
}
