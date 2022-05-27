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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mockStatic;

import com.cburch.logisim.fpga.hdlgenerator.Hdl;
import com.cburch.logisim.util.LineBuffer;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.commons.text.WordUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

/** Tests LogisimVersion class. */
@ExtendWith(MockitoExtension.class)
public class LineBufferTest extends TestBase {

  private LineBuffer lb;

  @BeforeEach
  public void setup() {
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

    final var lb = LineBuffer.getBuffer().pair("pair", pair).add("{{pair}}-{{1}}-{{2}}", foo, bar);
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
    final Map<String, String> tests =
        Map.of(
            " {{foo}} ", " FOO ",
            "{{bar}}", "BAR",
            " {{foo}} {{bar}} ", " FOO BAR ",
            " {{bar  }} ", " BAR ",
            " {{   bar  }} ", " BAR ",
            " {{   bar}} ", " BAR ");

    final var globalPairs = (new LineBuffer.Pairs()).pair("foo", "FOO").pair("bar", "BAR");

    for (final var test : tests.entrySet()) {
      final var lb = new LineBuffer(globalPairs);
      lb.add(test.getKey());
      final var expected = LineBuffer.getBuffer().add(test.getValue(), globalPairs);
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

    final var globalPairs = (new LineBuffer.Pairs()).pair("foo", "FOO").pair("bar", "BAR");

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
    buffer.pair("assign", assign).pair("=", eq).pair("<", ob).pair(">", cb);

    // do not merge with the above code.
    final var id = getRandomInt(1, 1023);
    final var pin = getRandomString();
    final var ins = getRandomString();

    buffer.pair("id", id).pair("pin", pin).pair("ins", ins);

    buffer.add(fmt, arg1);

    final var exp =
        LineBuffer.format(
            "{{1}}{{2}}{{3}}{{4}}{{5}}{{6}}{{7}}{{8}};", assign, ins, id, ob, pin, cb, eq, arg1);
    assertEquals(1, buffer.size());
    assertEquals(exp, buffer.get(0));
  }

  /**
   * Ensures that we properly deal with special chars in replacement string as backslashes (\) and
   * dollar signs ($) in the replacement string may cause the results to be different than if it
   * were being treated as a literal replacement string.
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

  /** Checks if providing less arguments than positional placeholders would be detected. */
  @Test
  public void testAddTooLittlePosArgs() {
    assertThrows(RuntimeException.class, () -> this.lb.add("This is {{1}} bar {{   2}} test", 666));
  }

  /**
   * Ensures we properly fail when formatting string uses positional placeholders, but there's none
   * provided.
   */
  @Test
  public void testGetUsedPlaceholders() {
    assertThrows(
        RuntimeException.class,
        () -> this.lb.validateLineNoPositionals("This is {{foo}} bar {{   2}} test"));
  }

  /* ********************************************************************************************* */

  /** Ensures getWithIndent() returns what it should. */
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

  /** Ensures addUnique() will not add non-unique content to the buffer. */
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
   * This tests ensures default constructor exists but is made protected to enforce users to call
   * getBuffer() and getHdlBuffer().
   */
  @Test
  public void testDefaultConstructorIsNotPublic() {
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

    assertTrue(found, "Default ctor not found!");
  }

  /* ********************************************************************************************* */

  /** Ensures format() can handle additional opening brackets. */
  @Test
  public void testPlaceholderSoroundedByThreeBrackets() {
    final var fmt = "{{{1}}{{2}},{{{3}}{testText}}};";

    final var buffer = LineBuffer.getBuffer();

    final var arg1 = getRandomString();
    final var arg2 = getRandomString();
    final var arg3 = getRandomString();

    buffer.add(fmt, arg1, arg2, arg3);

    final var exp = String.format("{%s%s,{%s{testText}}};", arg1, arg2, arg3);
    assertEquals(1, buffer.size());
    assertEquals(exp, buffer.get(0));
  }

  /* ********************************************************************************************* */

  @Test
  public void testRemarkLine() {
    final var remark = getRandomString();
    doRemarkLineTest(remark, false);
    doRemarkLineTest(remark, true);
  }

  private void doRemarkLineTest(String remark, boolean isVhdl) {
    try (final var mockedHdl = mockStatic(Hdl.class)) {
      setupMockedHdl(mockedHdl, isVhdl);
      final var expected = String.format("%s%s", Hdl.getLineCommentStart(), remark);
      final var lb = LineBuffer.getBuffer();
      lb.addRemarkLine(remark);
      assertEquals(1, lb.size());
      assertEquals(expected, lb.get(0));
    }
  }

  /* ********************************************************************************************* */

  /** Tests remark block generator for non-indented blocks. */
  @Test
  public void testBuildRemarkBlock() {
    doBuildRemarkBlockTest(getRandomString(), 0);
  }

  /** Ensures that indented remark blocks are correctly generated. */
  @Test
  public void testBuildRemarkBlockWithIndent() {
    doBuildRemarkBlockTest(getRandomString(), getRandomInt(1, 10));
  }

  /**
   * Ensures that remark with multiple words and total remark length longer than max line length are
   * correctly word wrapped.
   */
  @Test
  public void testMultilineRemarkBlock() {
    final var wordCnt = getRandomInt(20, 30);
    final var sb = new StringBuilder();
    for (var i = 0; i < wordCnt; i++) {
      sb.append(getRandomString()).append(" ");
    }
    doBuildRemarkBlockTest(sb.toString(), 0);
  }

  /** Ensures that remark with words longer than max line length are properly force-word wrapped. */
  @Test
  public void testForcedMultilineRemarkBlock() {
    final var cnt = getRandomInt(20, 30);
    final var remark = getRandomString().repeat(cnt);
    assertTrue(remark.length() > LineBuffer.MAX_LINE_LENGTH);
    doBuildRemarkBlockTest(remark, 0);
  }

  /**
   * Tests if remark containing multiple words, of which some are longer than max line length is
   * correctly wrapped.
   */
  @Test
  public void testMixedRemarkWrapping() {
    final var sb = new StringBuilder();

    final var cnt = getRandomInt(20, 30);
    final var tooLongWord = getRandomString().repeat(cnt);
    assertTrue(tooLongWord.length() > LineBuffer.MAX_LINE_LENGTH);
    sb.append(tooLongWord);

    final var wordCnt = getRandomInt(20, 30);
    for (var i = 0; i < wordCnt; i++) {
      sb.append(getRandomString()).append(" ");
    }

    doBuildRemarkBlockTest(sb.toString(), 0);
  }

  /** Ensures that indentation exceeding allowed range is handled correctly. */
  @Test
  public void testEdgeIndentWrappingOfRemarkBlock() {
    final var remark = getRandomString();
    assertThrows(
        IllegalArgumentException.class,
        () -> LineBuffer.getBuffer().addRemarkBlock(remark, LineBuffer.MAX_LINE_LENGTH));
  }

  /** Ensures that negative indentation is handled correctly. */
  @Test
  public void testNegativeIndentWrappingOfRemarkBlock() {
    final var remark = getRandomString();
    final var indent = getRandomInt(-100, -1);
    assertThrows(
        IllegalArgumentException.class,
        () -> LineBuffer.getBuffer().addRemarkBlock(remark, indent));
  }

  /** Test remark block builder for both Vhdl and non Vhdl modes. */
  private void doBuildRemarkBlockTest(String remarkText, int indentSpaces) {
    doBuildRemarkBlockTest(remarkText, indentSpaces, false);
    doBuildRemarkBlockTest(remarkText, indentSpaces, true);
  }

  // FIXME: this test do not cover breaking remark into multiple lines
  private void doBuildRemarkBlockTest(String remarkText, int indentSpaces, boolean isVhdl) {
    final var lb = LineBuffer.getBuffer();
    final var indent = " ".repeat(indentSpaces);

    try (final var mockedHdl = mockStatic(Hdl.class)) {
      setupMockedHdl(mockedHdl, isVhdl);

      final var maxLineLength =
          LineBuffer.MAX_LINE_LENGTH - (2 * Hdl.REMARK_MARKER_LENGTH) - indentSpaces;
      final var remarkLines =
          List.of(WordUtils.wrap(remarkText, maxLineLength, "\n", true).split("\n"));

      final var lineSep = indent + "-".repeat(LineBuffer.MAX_LINE_LENGTH - indentSpaces);
      final var expected = new ArrayList<String>();

      // Header separator line
      final var header = new StringBuilder();
      expected.add(
          header
              .append(indent)
              .append(Hdl.getRemarkBlockStart())
              .append(Hdl.getRemarkChar().repeat(LineBuffer.MAX_LINE_LENGTH - header.length()))
              .toString());

      // Build remark line
      final var tmpLine = new StringBuilder();
      for (final var remarkLine : remarkLines) {
        tmpLine.append(indent + Hdl.getRemarkBlockLineStart() + remarkLine);
        final var remaining =
            LineBuffer.MAX_LINE_LENGTH
                - indentSpaces
                - (2 * Hdl.REMARK_MARKER_LENGTH)
                - remarkLine.length();
        tmpLine.append(" ".repeat(remaining > 0 ? remaining : 0));
        tmpLine.append(Hdl.getRemarkBlockLineEnd());
        expected.add(tmpLine.toString());
        tmpLine.setLength(0);
      }

      // Footer separator line
      final var footer = new StringBuilder();
      expected.add(
          footer
              .append(indent)
              .append(
                  Hdl.getRemarkChar()
                      .repeat(
                          LineBuffer.MAX_LINE_LENGTH - footer.length() - Hdl.REMARK_MARKER_LENGTH))
              .append(Hdl.getRemarkBlockEnd())
              .toString());

      lb.addRemarkBlock(remarkText, indentSpaces);
      final var result = lb.get();

      assertEquals(expected, result);
    }
  }

  private void setupMockedHdl(MockedStatic<Hdl> mockedHdl, boolean isVhdl) {
    mockedHdl.when(Hdl::isVhdl).thenReturn(isVhdl);

    mockedHdl.when(Hdl::getRemarkChar).thenCallRealMethod();
    mockedHdl.when(Hdl::getRemarkBlockStart).thenCallRealMethod();
    mockedHdl.when(Hdl::getRemarkBlockEnd).thenCallRealMethod();
    mockedHdl.when(Hdl::getRemarkBlockLineStart).thenCallRealMethod();
    mockedHdl.when(Hdl::getRemarkBlockLineEnd).thenCallRealMethod();
    mockedHdl.when(Hdl::getLineCommentStart).thenCallRealMethod();
  }
}
