/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import com.cburch.draw.shapes.Line;
import com.cburch.logisim.util.LineBuffer;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;

/** Tests LogisimVersion class. */
public class LineBufferTest extends TestBase {

  private LineBuffer lb;

  @Before
  public void SetUp() {
    lb = LineBuffer.getBuffer();
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
    final var lb = LineBuffer.getBuffer();
    final var test = getRandomString();
    lb.add(test);

    assertEquals(1, lb.size());
    assertEquals(test, lb.get(0));
  }

  /** Tests if appending other LB works. */
  @Test
  public void testAddContentFromAnotherLineBuffer() {
    final var lb1 = LineBuffer.getBuffer();
    final var lb1Strings = new ArrayList<String>();
    final var lb1Cnt = getRandomInt(1, 10);
    for (var i = 0; i < lb1Cnt; i++) {
      final var str = getRandomString();
      lb1Strings.add(str);
      lb1.add(str);
    }

    final var lb2 = LineBuffer.getBuffer();
    final var lb2Strings = new ArrayList<String>();
    final var lb2Cnt = getRandomInt(1, 10);
    for (var i = 0; i < lb2Cnt; i++) {
      final var str = getRandomString();
      lb2Strings.add(str);
      lb2.add(str);
    }

    lb1.add(lb2);

    assertEquals(lb1Cnt + lb2Cnt, lb1.size());
    var idx = 0;
    for (final var line : lb1Strings) {
      assertEquals(line, lb1.get(idx++));
    }
    for (final var line : lb2Strings) {
      assertEquals(line, lb1.get(idx++));
    }
  }

  /** Tests is add(String, Object...) works as expected. */
  @Test
  public void testAddVarArgs() {
    final var lb = LineBuffer.getBuffer();
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

    final var lb = LineBuffer.getBuffer()
            .pair("pair", pair)
            .add("{{pair}}-{{1}}-{{2}}", foo, bar);
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
            pair("foo", "FOO");
            pair("bar", "BAR");
          }
        };

    for (final var test : tests.entrySet()) {
      final var lb = LineBuffer.getBuffer();
      lb.add(test.getKey(), pairs);
      final var expected = new LineBuffer(test.getValue());
      assertEquals(expected, lb);
    }
  }

  @Test
  public void testFormatterWithGlobalPairs() {
    final Map<String, String> tests = Map.of(
        " {{foo}} ", " FOO ",
        "{{bar}}", "BAR",
        " {{foo}} {{bar}} ", " FOO BAR ",
        " {{bar  }} ", " BAR ",
        " {{   bar  }} ", " BAR ",
        " {{   bar}} ", " BAR ");

    final var globalPairs = (new LineBuffer.Pairs())
        .pair("foo", "FOO")
        .pair("bar", "BAR");

    for (final var test : tests.entrySet()) {
      final var lb = new LineBuffer(globalPairs);
      lb.add(test.getKey());
      final var expected = LineBuffer.getBuffer().add(test.getValue(), globalPairs);
      assertEquals(expected, lb);
    }
  }

  @Test
  public void testFormatterMixedPairs() {
    final Map<String, String> tests = Map.of(
        " {{foo}} ", " FOO ",
        " {{bar}} ", " BAR ",
        " {{foo}} {{bar}} ", " FOO BAR ",
        " {{bar  }} ", " BAR ",
        " {{   bar  }} ", " BAR ",
        " {{   bar}} ", " BAR ",
        " {{bang}} ", " BANG ");

    final var globalPairs = (new LineBuffer.Pairs())
        .pair("foo", "FOO")
        .pair("bar", "BAR");

    for (final var test : tests.entrySet()) {
      final var lb = new LineBuffer(globalPairs);
      lb.add(test.getKey(), new LineBuffer.Pairs("bang", "BANG"));

      final var expPairs = new LineBuffer.Pairs();
      expPairs.pair("foo", "FOO");
      expPairs.pair("bar", "BAR");
      expPairs.pair("bang", "BANG");

      final var expected = new LineBuffer(test.getValue(), expPairs);
      assertEquals(expected, lb);
    }
  }

  @Test
  public void testMultiplePlaceholdersInLine() {
    final var arg1 = "ARG_1";
    final var fmt = "{{assign}}{{ins}}{{id}}{{<}}{{pin}}{{>}}{{=}}{{1}};";

    final var buffer = LineBuffer.getBuffer();
    // final var buffer = LineBuffer.getHdlBuffer();  // FIXME: mock isVHDL() first!

    final var assign = getRandomString();
    final var eq = getRandomString();
    final var ob = getRandomString();
    final var cb = getRandomString();
    buffer
        .pair("assign", assign)
        .pair("=", eq)
        .pair("<", ob)
        .pair(">", cb);

    // do not merge with the above code.
    final var id = getRandomInt(1, 1023);
    final var pin = getRandomString();
    final var ins = getRandomString();

    buffer
        .pair("id", id)
        .pair("pin", pin)
        .pair("ins", ins);

    buffer.add(fmt, arg1);

    final var exp = LineBuffer.format("{{1}}{{2}}{{3}}{{4}}{{5}}{{6}}{{7}}{{8}};", assign, ins, id, ob, pin, cb, eq, arg1);
    assertEquals(1, buffer.size());
    assertEquals(exp, buffer.get(0));
  }

  /**
   * Ensures that we properly deal with special chars in replacement string as backslashes (\)
   * and dollar signs ($) in the replacement string may cause the results to be different
   * than if it were being treated as a literal replacement string.
   */
  @Test
  public void testFormatWithSpecialCharsInReplacementString() {
    final var tests = Map.of("$", "\\");
    for (final var test : tests.entrySet()) {
      final var result = LineBuffer.format("{{1}}", test.getValue());
      assertEquals(test.getValue(), result);
    }
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

  /**
   * Ensures we properly fail when formatting string uses positional placeholders, but there's none provided.
   */
  @Test
  public void testGetUsedPlaceholders() {
    assertThrows(RuntimeException.class, () -> {
      this.lb.validateLineNoPositionals("This is {{foo}} bar {{   2}} test");
    });
  }

  /* ********************************************************************************************* */

  /**
   * Ensures getWithIndent() returns what it should.
   */
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

  /**
   * Ensures addUnique() will not add non-unique content to the buffer.
   */
  @Test
  public void testAddUnique() {
    final var line = getRandomString();

    final var lb = LineBuffer.getBuffer();
    lb.addUnique(line);
    assertEquals(1, lb.size());
    lb.addUnique(line);
    assertEquals(1, lb.size());

    assertEquals(line, lb.get(0));
  }

  /**
   * This tests ensures default constructor exists but is made protected to enforce users to
   * call getBuffer() and getHdlBuffer().
   */
  @Test
  public void testDefaultConstructorIsNotPublic()  {
    final var lb = LineBuffer.getBuffer();

    final var ctors = LineBuffer.class.getDeclaredConstructors();
    assertTrue(ctors.length > 0);

    var found = false;
    for (final var ctor : ctors) {
      // we care default, argumentless ctor only.
      if (!ctor.getDeclaringClass().equals(LineBuffer.class)) continue;
      if (ctor.getParameterCount() != 0) continue;
      assertEquals(0, ctor.getParameterCount());
      // FIXME: temporary change!
      // assertTrue(Modifier.isPublic(ctor.getModifiers()));
      assertFalse(Modifier.isPublic(ctor.getModifiers()));
      found = true;
      break;
    }

    assertTrue("Default ctor not found!", found);
  }

}
