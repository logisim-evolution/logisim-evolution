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
import java.util.regex.Pattern;

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

  // Paired placeholders
  protected Pairs pairs = new Pairs();

  /* ********************************************************************************************* */

  /**
   * Default constructor.
   */
  public LineBuffer() {
    super();
  }

  /**
   * Construct LineBuffer with line added to the container.
   *
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
  public LineBuffer(String line, Pairs pairs) {
    addPairs(pairs);
    add(line);
  }

  /**
   * Constructs LineBuffer instance, then adds provided placeholders pairs to be used with the instance.
   *
   * @param pairs Placeholder pairs to be used.
   */
  public LineBuffer(Pairs pairs) {
    super();
    addPairs(pairs);
  }

  /* ********************************************************************************************* */

  /**
   * Returns number of content entries stored in buffer.
   *
   * @return number of entries.
   */
  public int size() {
    return contents.size();
  }

  public boolean isEmpty() {
    return contents.isEmpty();
  }

  public boolean contains(Object obj) {
    return contents.contains(obj);
  }

  /* ********************************************************************************************* */

  /**
   * Injects commonly used HDL pairs making them enabled for placeholders.
   *
   * @return Instance of self for easy chaining.
   */
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

  /**
   * Adds paired placeholders to internal map.
   *
   * @param pairs
   *
   * @return Instance of self for easy chaining.
   */
  public LineBuffer addPairs(Pairs pairs) {
    // FIXME we appedn placeholders, not replace existing map! Also shall detect positionals
    this.pairs = pairs;
    return this;
  }

  /**
   * Clears content buffer and pairs.
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

  /**
   * Adds line to the buffer only if line is not present already, formatting it first.
   *
   * @param fmt Line to add if not present in buffer.
   *
   * @return Instance of self for easy chaining.
   */
  public LineBuffer addUnique(String fmt, Object... args) {
    // Resolve positional arguments then apply paired ones.     WE need to do this first (instead of
    // letting add() do that) otherwise `contains` would be looking for non-final version of the
    // string.
    final var line = applyPairs(format(fmt, args));
    if (!contents.contains(line))
      add(line, true);
    return this;
  }

  /**
   * Adds line to the buffer only if line is not present already. Please note this implementation
   * of `addUnique()` does not resolve positional arguments. If you use them, you musts pass final
   * form of the string (pass thru `format()` if needed) or use other implementations.
   *
   * @param line Line to add if not present in buffer.
   *
   * @return Instance of self for easy chaining.
   */
  public LineBuffer addUnique(String line) {
    // Resolve positional arguments then apply paired ones.     WE need to do this first (instead of
    // letting add() do that) otherwise `contains` would be looking for non-final version of the
    // string.
    line = applyPairs(line);
    if (!contents.contains(line))
      add(line, true);
    return this;
  }

  /* ********************************************************************************************* */

  /**
   * Adds single line to the content buffer. Will resolve paried placeholders first (but not
   * positionals).
   *
   * @param line String to be added to the content buffer.
   * @return Instance of self for easy chaining.
   */
  public LineBuffer add(String line) {
    return add(line, true);
  }

  /**
   * Adds single line to the content buffer. Will resolve paried placeholders first, if applyMap is `true`,
   * but won't resolve positionals).
   *
   * @param line line to be added
   * @param applyMap `true` if line shall be processed for placeholders (default), `false` if you
   *     want it to be added "raw".
   * @return Instance of self for easy chaining.
   */
  public LineBuffer add(String line, boolean applyMap) {
    if (applyMap) {
      line = applyPairs(line, pairs);
    }

    // Ensure no placeholders left unprocessed.
    validateLineNoPositionals(line);

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
   * Formats string using `format()` method and adds to the buffer via standard pipeline (so
   * placeholders will be handled too).
   *
   * @param fmt Formatting string as accepted by String.format()
   * @param args Optional values for positional placeholders.
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
   * @param fmt Formatting string. Wrap keys in `{{` and `}}`.
   * @param pairs Search-Replace map.
   *
   * @return Instance of self for easy chaining.
   */
  public LineBuffer add(String fmt, Pairs pairs) {
    fmt = applyPairs(fmt, pairs);

    // Ensure no placeholders left unprocessed.
    validateLineNoPositionals(fmt);

    return add(fmt);
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
   * Adds each argument to be added as separate line to the buffer.
   *
   * <p>Note: I had to use different name than add() for this particular method, as its signature
   * takes over the `add(String fmt, Obj... args)` which in turn ends up placeholders keys being
   * left unhandled.
   *
   * @param lines lines to be added to the buffer.
   *
   * @return Instance of self for easy chaining.
   */
  public LineBuffer addLines(String... lines) {
    return add(Arrays.asList(lines));
  }

  /* ********************************************************************************************* */

  /**
   * Formats provided fmt string using global pairs.
   *
   * @param fnt Formatting string.
   *
   * @return Instance of self for easy chaining.
   */
  public String applyPairs(String fnt) {
    return applyPairs(fnt, pairs);
  }

  /**
   * Applies search-replace var to provided string.
   *
   * @param format String to format, with (optional) `{{placeholders}}`.
   * @param pairs Instance of `Pairs` holdinhg replacements for placeholders.
   *
   * @return Formatted string.
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
   *
   * @return Instance of self for easy chaining.
   */
  public LineBuffer repeat(int count, String line) {
    for (var i = 0; i < count; i++) add(line);
    return this;
  }

  /* ********************************************************************************************* */

  /**
   * Appends single empty line to the content buffer.
   *
   * @return Instance of self for easy chaining.
   */
  public LineBuffer empty() {
    return repeat(1, "");
  }

  /**
   * Appends `count` number for empty lines to the content buffer.
   *
   * @param count number of empty lines to be addeed.
   *
   * @return Instance of self for easy chaining.
   */
  public LineBuffer empty(int count) {
    return repeat(count, "");
  }

  /* ********************************************************************************************* */

  /**
   * Returns specified line of the content buffer present at index position.
   *
   * @return elment at given position or exception for invalid index.
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
   * @param howMany Number of times `indent` string should be repeated to form the final indent string.
   * @param indent Indent string.
   *
   * @return indented content of the buffer.
   */
  public ArrayList<String> getWithIndent(int howMany, String indent) {
    return getWithIndent(indent.repeat(howMany));
  }

  /**
   * Returns content buffer as ArrayList() with every single entry prefixed by `indent` string.
   *
   * @param indent Indent string.
   *
   * @return indented content of the buffer.
   */
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
   *
   * @return Instance of self for easy chaining.
   */
  public LineBuffer addRemarkBlock(String remarkText) {
    return addRemarkBlock(remarkText, 0);
  }

  /**
   * Builds and adds remark block to the contents buffer.
   *
   * @param remarkText Remark text.
   * @param nrOfIndentSpaces Number of extra indentation spaces.
   *
   * @return Instance of self for easy chaining.
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
   *
   * @return Constructed lines of remark block.
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
   * Formats provided fmt string using provided arguments for positional placeholders.
   *
   * @param fmt Formattting string.
   * @param args Positional placeholders.
   *
   * @return Formatted string.
   */
  public static String format(String fmt, Object... args) {
    return applyPairs(fmt, Pairs.fromArgs(args));
  }

  /* ********************************************************************************************* */

  /**
   * Emits warning string to stdout.
   *
   * @param fmt Formatting string.
   * @param args Positional arguments.
   *
   * @return Instance of self for easy chaining.
   */
  private LineBuffer warn(String fmt, Object... args) {
    System.out.println(format("WARNING: " + fmt, args));
    return this;
  }

  /**
   * Throws error as RuntimeException
   *
   * @param msg Exception message.
   */
  private void abort(String msg) {
    throw new RuntimeException(msg);
  }

  /**
   * Formats error message and then throws abort()
   *
   * @param fmt Exception message formatting string.
   * @param args Positional placeholders.
   */
  private void abort(String fmt, Object... args) {
    abort(format(fmt, args));
  }

  /* ********************************************************************************************* */

  private ArrayList<String> placeholders = new ArrayList<>();
  private ArrayList<String> positionalPlaceholders = new ArrayList<>();
  private ArrayList<String> pairedPlaceholders = new ArrayList<>();

  // check if we have positional args and/or paired
  // if positional: check if we have args given, then check all the rest
  // for paired check mapping vs global map and one-time pairs

  /**
   * Parses formatting string looking for positional placeholders and then
   * nitializes internal validator data.
   *
   * @param fmt Formatting string to analize.
   *
   * @return Instance of self for easy chaining.
   */
  protected LineBuffer initValidator(String fmt) {
    placeholders = extractPlaceholders(fmt);
    positionalPlaceholders.clear();
    pairedPlaceholders.clear();

    // Separate positionals and other placeholders
    final var pattern = Pattern.compile("^\\d+$");
    for (final var phKey : placeholders) {
      if (pattern.matcher(phKey).find()) positionalPlaceholders.add(phKey);
      else pairedPlaceholders.add(phKey);
    }

    return this;
  }

  public void validateLineNoPositionals(String fmt) {
    final var empty = new Object[] {};
    validateLine(fmt, null);
  }

  protected void validateLineWithPositionalArgs(String fmt, Object... args) {
    initValidator(fmt);

    final var positionalsCnt = positionalPlaceholders.size();

    // Do we have positional placeholders in fmt?
    if (positionalPlaceholders.isEmpty()) {
      // Warn if we have no positional placeholders used, but still receive positional arguments.
      if (args.length > 0)
        warn("#E004: Useless positional arguments. Expected nothing, but received {{2}} for '{{1}}'.",
                fmt, positionalsCnt);
    } else {
      if (positionalsCnt < args.length)
        // We had too many positional args given compared to positional placeholders. But that
        // difference can be OK, so just warn.
        abort("#E001: Too many positional arguments, Expected {{2}}, but received {{3}} for '{{1}}'.",
                fmt, positionalsCnt, args.length);

      if (positionalsCnt > args.length)
        // Too little arguments provided vs. awaiting placeholders. That's life threatening condition.
        abort("#E002: Insufficient positional arguments. Expected {{2}}, but received {{3}} for '{{1}}'.",
                fmt, positionalsCnt, args.length);

      // count matches, let's see if contents too.
      var errorCnt = 0;
      for (final var posKey : positionalPlaceholders) {
        if (Integer.valueOf(posKey) > positionalsCnt) {
          // Reference to non-existing position found. Warn about all detected issues. We fail later.
          warn("#E003: Invalid positional argument. '{{1}}' used, but max value is {{2}}.", posKey, positionalsCnt);
          errorCnt++;
        }
      }
      if (errorCnt > 0)
        abort("#E003: Non-existing positional arguments found in '{{1}}'. See console output for details.", fmt);
    }
  }

  protected void validateLineWithPairedPlaceholders(String fmt) {
    initValidator(fmt);

    // check if we use any non mapped placeholder
    var errorCnt = 0;
    for (final var key : pairedPlaceholders) {
      if (!placeholders.contains(key)) {
        warn("#E005: Placeholder '{{2}}' has no mapping while processing '{{1}}'.", fmt, key);
        errorCnt++;
      }
    }
    if (errorCnt > 0) abort("#E005: Unmapped placeholders detected. See console output for details.");
  }

  protected void validateLine(String fmt, Pairs argPairs) {
    initValidator(fmt);

    if (argPairs != null) {
      validateLineWithPositionalArgs(fmt, argPairs);
    } else {
      if (positionalPlaceholders.size() > 0)
        abort(
            "#E004: No positional arguments, but expected {{2}} for '{{1}}'.",
            fmt, positionalPlaceholders.size());
    }

    // Check if paired placeholders used in formatting string are known at this point.
    var errorCount = 0;
    for (final var key : pairedPlaceholders) {
      if (!(pairs.containsKey(key) || (argPairs != null && argPairs.containsKey(key)))) {
        warn("#E006: No mapping for placeholder: '{{1}}'", key);
        errorCount++;
      }
    }
    if (errorCount > 0)
      abort("#E006: {{1}} unmapped placeholders detected in '{{2}}'. See console output for details.", errorCount, fmt);
  }

  /* ********************************************************************************************* */

  /**
   * Extract names of valid placeholders found in provided string.
   *
   * @param fmt String to analyze.
   *
   * @return Returns list of found placeholders. If no placeholder is found, returnes empty list.
   */
  public ArrayList<String> extractPlaceholders(String fmt) {
    final var keys = new ArrayList<String>();

    final var regex = "(\\{\\{.+?\\}\\})+";
    final var pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
    final var matcher = pattern.matcher(fmt);
    while (matcher.find()) {
      // Extract key from between the brackets:
      final var bracketsCharCount = 2;
      for (var i = 1; i <= matcher.groupCount(); i++) {
        var keyStr = matcher.group(i);
        keyStr = keyStr.substring(bracketsCharCount, keyStr.length() - bracketsCharCount).strip();

        // FIXME: we shall overwrite old ph or fail?
        if (!keys.contains(keyStr)) keys.add(keyStr);
      }
    }

    return keys;
  }

  /* ********************************************************************************************* */

  /**
   * Both objects are equal if their content (and its order) is exatcly the same.
   *
   * @param other Other instance of LineBuffer
   */
  @Override
  public boolean equals(Object other) {
    if (!(other instanceof LineBuffer && size() == ((LineBuffer) other).size())) return false;
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
