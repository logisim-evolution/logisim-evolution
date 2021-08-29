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

package com.cburch.logisim.util;

import com.cburch.logisim.fpga.hdlgenerator.HDL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.RandomAccess;

/**
 * This class is intended to simplify building any HDL content, which usually contains of fixed text
 * lines and parametrized strings. This class offers wrapper methods that can format the strings
 * under the hood, reducing the number of explicit calls made to build a text line. See usage
 * examples.
 */
public class LineBuffer implements RandomAccess {
  public static final int MAX_LINE_LENGTH = 80;
  public static final int DEFAULT_INDENT = 3;
  public static final String DEFAULT_INDENT_STR = " ";

  private ArrayList<String> contents = new java.util.ArrayList<String>();

  /* ********************************************************************************************* */

  /**
   * Default constructor.
   */
  public LineBuffer() {
    super();
  }

  /**
   * Construct LineBuffer with line added to the container.
   * @param line text line to be added to buffer
   */
  public LineBuffer(String line) {
    add(line);
  }

  /**
   * Constructs LineBuffer instance, then sets placeholder pairs and adds given text line to the
   * container. Note, that because placeholder pairs are set first, you can instantly use these
   * placeholders in your line. Also note that
   *
   * @param line Text line to be added.
   * @param pairs Placeholder pairs to be used.
   */
//  public LineBuffer(String line, Pairs pairs) {
//    withPairs(pairs);
//    add(line);
//  }


  /**
   * Constructs LineBuffer instance, then adds provided placeholders pairs to be used with the instance.
   *
   * @param pairs Placeholder pairs to be used.
   */
  public LineBuffer(Pairs pairs) {
    super();
    addPairs(pairs);
  }

  /**
   * Returns number of unique pairs stored already.
   * @return number of pairs.
   */
  public int size() {
    return contents.size();
  }

  protected Pairs pairs = new Pairs();

  public LineBuffer addHdlPairs() {
    return pair("assign", HDL.assignPreamble())
        .pair("=", HDL.assignOperator())
        .pair("or", HDL.orOperator())
        .pair("and", HDL.andOperator())
        .pair("not", HDL.notOperator())
        .pair("bracketOpen", HDL.BracketOpen())
        .pair("bracketClose", HDL.BracketClose())
        .pair("<", HDL.BracketOpen())
        .pair(">", HDL.BracketClose())
        .pair("0b", HDL.zeroBit())
        .pair("1b", HDL.oneBit());
  }

  public LineBuffer addPairs(Pairs pairs) {
    this.pairs = pairs;
    return this;
  }

  /**
   * Clears internal buffer.
   *
   * @return Instance of self for easy chaining.
   */
  public LineBuffer clear() {
    clearBuffer();
    clearPairs();
    return this;
  }

  /**
   * Clears pair map.
   *
   * @return Instance of self for easy chaining.
   */
  public LineBuffer clearPairs() {
    pairs.clear();
    return this;
  }

  /**
   * Clears content buffer.
   *
   * @return Instance of self for easy chaining.
   */
  public LineBuffer clearBuffer() {
    contents.clear();
    return this;
  }

  /* ********************************************************************************************* */

  public boolean isEmpty() {
    return contents.isEmpty();
  }

  public boolean contains(Object obj) {
    return contents.contains(obj);
  }

  /* ********************************************************************************************* */

  public static String format(String fmt, Object... args) {
    return applyPairs(fmt, Pairs.fromArgs(args));
  }

  /* ********************************************************************************************* */

  /**
   * Adds line to the buffer only if line is not present already.
   *
   * @param line Line to optionally add.
   */
  public LineBuffer addUnique(String line) {
    add(line, true);
    return this;
  }

  /* ********************************************************************************************* */

  /**
   * Adds single line to the content buffer.
   *
   * @param line String to be added to the content buffer.
   *
   * @return Instance of self for easy chaining.
   */
  public LineBuffer add(String line) {
    return add(line, true);
  }

  /**
   * Adds single line to the content buffer. If applyMap is `true`, it will try to format line with
   * known pairs (this is default behavior).
   *
   * @param line line to be added
   * @param applyMap `true` if line shall be processed for placeholders (default), `false` if you
   *     want it to be added "raw".
   *
   * @return Instance of self for easy chaining.
   */
  public LineBuffer add(String line, boolean applyMap) {
    if (applyMap) {
      line = applyPairs(line, pairs);
    }
    contents.add(line);
    return this;
  }

  /**
   * Adds content of provided StringBuilder to the buffer.
   *
   * @param stringBuilder StringBuilder which contents is to be added.
   *
   * @return Instance of self for easy chaining.
   */
  public LineBuffer add(StringBuilder stringBuilder) {
    return add(stringBuilder.toString());
  }

  /**
   * Formats string using @String.format() and adds to the buffer via standard pipeline (so
   * placeholders will be handled too).
   *
   * @param fmt Formatting string as accepted by String.format()
   * @param args Optional arguments
   *
   * @return Instance of self for easy chaining.
   */
  public LineBuffer add(String fmt, Object... args) {
    return add(fmt, Pairs.fromArgs(args));
  }

  /**
   * Key-Value formatter. Will look for all occurences of keys from provided map and replace with
   * values. To enforce key uniquess and avoid too-greedy replacing, the fmt string must wrap key in
   * double curly braces, i.e. in map `("foo", "bar")`, in formatting string `This {{key}} will be
   * replaced.`. The `key` can be any string HashMap accepts, with the only difference leading and
   * trailing spaces are ignored, which lets i.e. aligning placeholders. All these `{{foo}}`, `{{
   * foo}}` and `{{foo }}` are equivalent. Processed string is then added to content buffer.
   *
   * @param format Formatting string. Wrap keys in `{{` and `}}`.
   * @param pairs Search-Replace map.
   * @return Instance of self for easy chaining.
   */
  public LineBuffer add(String format, Pairs pairs) {
    return add(applyPairs(format, pairs));
  }

  /**
   * Adds all lines from given collection to content buffer using defult pipeline, so placeholders
   * are processed too.
   *
   * @param lines
   */
  public LineBuffer add(Collection<String> lines) {
    for (final var line : lines) add(line);
    return this;
  }

  /**
   *
   * Adds each argument to be added as separate line to the buffer.
   *
   * Note: I had to use different name than add() for this particular method,
   * as its signature takes over the `add(String fmt, Obj... args)` which in turn
   * ends up placeholders keys being left unhandled.
   *
   * @param lines lines to be added to the buffer.
   *
   * @return Instance of self for easy chaining.
   */
  public LineBuffer addLines(String... lines) {
    return add(Arrays.asList(lines));
  }

  /* ********************************************************************************************* */

  public String applyPairs(String format) {
    return applyPairs(format, pairs);
  }

  /**
   * Applies search-replace var to provided string.
   *
   * @param format String to format, with (optional) `{{placeholders}}`.
   * @param pairs Instance of `Pairs` holdinhg replacements for placeholders.
   */
  public static String applyPairs(String format, Pairs pairs) {
    if (pairs != null) {
      for (final var set : pairs.entrySet()) {
        final var searchRegExp = String.format("\\{\\{\\s*%s\\s*\\}\\}", set.getKey());
        format = format.replaceAll(searchRegExp, set.getValue().toString());
      }
    }
    return format;
  }

  /**
   * Adds `line` string to the contents buffer `count` times.
   *
   * @param count Number of times line should be added.
   * @param line String to be added to the buffer.
   * @return
   */
  public LineBuffer repeat(int count, String line) {
    for (var i = 0; i < count; i++) add(line);
    return this;
  }

  /* ********************************************************************************************* */

  /** Appends single empty line to the content buffer. */
  public LineBuffer empty() {
    return repeat(1, "");
  }

  /**
   * Appends `count` number for empty lines to the content buffer.
   *
   * @param count number of empty lines to be addeed.
   */
  public LineBuffer empty(int count) {
    return repeat(count, "");
  }

  /* ********************************************************************************************* */

  /**
   * Returns specified line of the content buffer present at index position.
   *
   * @return elment
   *
   * @throws IndexOutOfBoundsException
   */
  public String get(int index) {
    return contents.get(index);
  }

  /**
   * Returns whole content buffer as ArrayList(). Content is returned as-is, no additional
   * processing happens.
   *
   * @return unindented content of the buffer.
   */
  public ArrayList<String> get() {
    return contents;
  }

  /**
   * Returns content buffer as ArrayList(), with each line indented by `DEFAULT_INDENT` using
   * `DEFAULT_INDENT_STR` as a indentation character.
   *
   * @return indented content of the buffer.
   */
  public ArrayList<String> getWithIndent() {
    return getWithIndent(DEFAULT_INDENT, DEFAULT_INDENT_STR);
  }

  /**
   * Returns content buffer as ArrayList() with every single entry prefixed by `howMany` characters
   * of `DEFAULT_INDENT_STR`.
   *
   * @param howMany Number of spaces to prefix each line with.
   *
   * @return indented content of the buffer.
   */
  public ArrayList<String> getWithIndent(int howMany) {
    return getWithIndent(howMany, DEFAULT_INDENT_STR);
  }

  /**
   * Returns content buffer as ArrayList() with every single entry prefixed by `indent` string
   * `howMany` times.
   *
   * @param howMany Number of times `indent` string should be repeated to form the final indent
   *     string.
   * @param indent Indent string.
   *
   * @return indented content of the buffer.
   */
  public ArrayList<String> getWithIndent(int howMany, String indent) {
    return getWithIndent(indent.repeat(howMany));
  }

  public ArrayList<String> getWithIndent(String indent) {
    final var result = new ArrayList<String>();
    for (final var line : contents) {
      // We do not indent empty lines, just ones with content.
      result.add((line.length() == 0) ? line : indent + line);
    }
    return result;
  }

  /* ********************************************************************************************* */

  /**
   * Builds and adds remark block to the contents buffer.
   *
   * @param remarkText Remark text.
   */
  public LineBuffer addRemarkBlock(String remarkText) {
    return addRemarkBlock(remarkText, 0);
  }

  /**
   * Builds and adds remark block to the contents buffer.
   *
   * @param remarkText Remark text.
   * @param nrOfIndentSpaces Number of extra indentation spaces.
   * @return
   */
  public LineBuffer addRemarkBlock(String remarkText, Integer nrOfIndentSpaces) {
    add(buildRemarkBlock(remarkText, nrOfIndentSpaces));
    return this;
  }

  /**
   * Builds remark block.
   *
   * @param remarkText Remark text.
   * @param nrOfIndentSpaces Number of extra indentation spaces.
   */
  protected ArrayList<String> buildRemarkBlock(String remarkText, Integer nrOfIndentSpaces) {
    final var maxRemarkLength = MAX_LINE_LENGTH - 2 * HDL.remarkOverhead() - nrOfIndentSpaces;
    final var remarkWords = remarkText.split(" ");
    final var oneLine = new StringBuilder();
    final var contents = new ArrayList<String>();
    var maxWordLength = 0;
    for (final var word : remarkWords) {
      if (word.length() > maxWordLength) maxWordLength = word.length();
    }
    if (maxRemarkLength < maxWordLength) return contents;
    /* we start with generating the first remark line */
    while (oneLine.length() < nrOfIndentSpaces) oneLine.append(" ");
    for (var i = 0; i < MAX_LINE_LENGTH - nrOfIndentSpaces; i++) {
      oneLine.append(HDL.getRemakrChar(i == 0, i == MAX_LINE_LENGTH - nrOfIndentSpaces - 1));
    }
    contents.add(oneLine.toString());
    oneLine.setLength(0);
    /* Next we put the remark text block in 1 or multiple lines */
    for (final var remarkWord : remarkWords) {
      if ((oneLine.length() + remarkWord.length() + HDL.remarkOverhead()) > (MAX_LINE_LENGTH - 1)) {
        /* Next word does not fit, we end this line and create a new one */
        while (oneLine.length() < (MAX_LINE_LENGTH - HDL.remarkOverhead())) oneLine.append(" ");
        oneLine
            .append(" ")
            .append(HDL.getRemakrChar(false, false))
            .append(HDL.getRemakrChar(false, false));
        contents.add(oneLine.toString());
        oneLine.setLength(0);
      }
      while (oneLine.length() < nrOfIndentSpaces) oneLine.append(" ");
      if (oneLine.length() == nrOfIndentSpaces)
        oneLine.append(HDL.getRemarkStart()); // we put the preamble
      if (remarkWord.endsWith("\\")) {
        // Forced new line
        oneLine.append(remarkWord, 0, remarkWord.length() - 1);
        while (oneLine.length() < (MAX_LINE_LENGTH - HDL.remarkOverhead())) oneLine.append(" ");
      } else {
        oneLine.append(remarkWord).append(" ");
      }
    }
    if (oneLine.length() > (nrOfIndentSpaces + HDL.remarkOverhead())) {
      // We have an unfinished remark line
      while (oneLine.length() < (MAX_LINE_LENGTH - HDL.remarkOverhead())) oneLine.append(" ");
      oneLine
          .append(" ")
          .append(HDL.getRemakrChar(false, false))
          .append(HDL.getRemakrChar(false, false));
      contents.add(oneLine.toString());
      oneLine.setLength(0);
    }
    // We end with generating the last remark line.
    while (oneLine.length() < nrOfIndentSpaces) oneLine.append(" ");
    for (var i = 0; i < MAX_LINE_LENGTH - nrOfIndentSpaces; i++)
      oneLine.append(HDL.getRemakrChar(i == MAX_LINE_LENGTH - nrOfIndentSpaces - 1, i == 0));
    contents.add(oneLine.toString());

    return contents;
  }

  /* ********************************************************************************************* */

  /**
   * Both objects are equal if their content (and its order) is exatcly the same.
   *
   * @param other Other instance of LineBuffer
   */
  @Override
  public boolean equals(Object other) {
    if (!(other instanceof LineBuffer && size() == ((LineBuffer) other).size())) {
      return false;
    }
    for (var i = 0; i < size(); i++) {
      final var thisLine = get().get(i);
      final var otherLine = ((LineBuffer) other).get().get(i);
      if (!(thisLine.equals(otherLine))) return false;
    }
    return true;
  }

  @Override
  public String toString() {
    return contents.toString();
  }

  /* ********************************************************************************************* */

  public LineBuffer pair(String key, Object value) {
    pairs.add(key, value);
    return this;
  }

  /* ********************************************************************************************* */

  public static class Pairs extends HashMap<String, Object> {
    public Pairs() {
      // empty
    }


    /**
     * Constructs Pairs map from positional arguments auto-assigning numerical
     * placeholders `{{x}}` where `x` is integer starting from `1`.
     *
     * @param args Arguments to use to build the map.
     */
    public static Pairs fromArgs(Object... args) {
      final var map = new Pairs();
      var idx = 1;
      for (final var arg : args) {
        map.add(String.valueOf(idx++), "" + arg);
      }
      return map;
    }


    public Pairs(String key, Object value) {
      add(key, value);
    }

    public Pairs add(String key, Object value) {
      put(key, value);
      return this;
    }
  }

  /* ********************************************************************************************* */

} // end of LineBuffer
