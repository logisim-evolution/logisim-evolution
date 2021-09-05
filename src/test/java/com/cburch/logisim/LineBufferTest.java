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
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import com.cburch.logisim.fpga.hdlgenerator.HDL;
import com.cburch.logisim.util.LineBuffer;
import java.util.ArrayList;
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

  /** Tests is plain add(String) works as expected. */
  @Test
  public void testAdd() {
    final var lb = new LineBuffer();
    final var test = getRandomString();
    lb.add(test);

    assertEquals(1, lb.size());
    assertEquals(test, lb.get(0));
  }

  /** Tests is add(String, Object...) works as expected. */
  @Test
  public void testAddVarArgs() {
    final var lb = new LineBuffer();
    final var foo = getRandomString();
    final var bar = getRandomInt(0, 100);

    lb.add("{{1}}{{2}}", foo, bar);
    assertEquals(1, lb.size());
    assertEquals(foo + bar, lb.get(0));
    lb.clear();

    lb.add("{{2}}{{1}}", foo, bar);
    assertEquals(1, lb.size());
    assertEquals(bar + foo, lb.get(0));
  }

  @Test
  public void testAddWordsAndWithPair() {
    final var pair = getRandomString();
    final var foo = getRandomString();
    final var bar = getRandomInt(0, 100);

    final var lb = (new LineBuffer())
            .pair("pair", pair)
            .add("{{pair}}-{{1}}-{{2}}", foo, bar);
    System.out.println(lb.toString());
    assertEquals(1, lb.size());
    final var expected = String.format("%s-%s-%d", pair, foo, bar);
    assertEquals(expected, lb.get(0));
  }

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
      final var expected = (new LineBuffer()).add(test.getValue(), pairs);
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

    final var globalPairs =
        new LineBuffer.Pairs() {
          {
            add("foo", "FOO");
            add("bar", "BAR");
          }
        };

    for (final var test : tests.entrySet()) {
      final var lb = new LineBuffer(globalPairs);
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

  @Test
  public void testMultiplePlaceholdersInLine() {
    final var arg1 = "ARG_1";
    final var line = "{{assign}} s_{{ins}}{{id}}{{<}}{{pin}}{{>}} {{=}} {{1}};";

    final var buffer = new LineBuffer();
    // lb.addHdlPairs();  // FIXME: mock isVHDL()
    buffer
        .pair("assign", "assign")
        .pair("=", "=")
        .pair("<", "<")
        .pair(">", ">");

    buffer
        .pair("id", "ID")
        .pair("pin", "PIN")
        .pair("ins", "INS");

    buffer.add(line, arg1);

    final var exp = "assign s_INSID<PIN> = " + arg1 + ";";
    assertEquals(1, buffer.size());
    assertEquals(exp, buffer.get(0));
  }

  /* ********************************************************************************************* */

  /**
   * Checks if providing less arguments than positional placeholders would be detected.
   */
  @Test
  public void testAddTooLittlePosArgs() {
    assertThrows(RuntimeException.class, () -> {
      this.lb.add("This is {{1}} bar {{   2}} test", 666);
    });
  }

  @Test
  public void testGetUsedPlaceholders() {
    assertThrows(RuntimeException.class, () -> {
      this.lb.validateLineNoPositionals("This is {{foo}} bar {{   2}} test");
    });
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
