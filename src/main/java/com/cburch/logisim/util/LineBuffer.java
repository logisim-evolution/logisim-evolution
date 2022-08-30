/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.util;

import com.cburch.logisim.fpga.hdlgenerator.Hdl;
import com.cburch.logisim.fpga.hdlgenerator.Vhdl;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.RandomAccess;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.text.WordUtils;

/**
 * This class is intended to simplify building any HDL content, which usually contains of fixed text
 * lines and parametrized strings. This class offers wrapper methods that can format the strings
 * under the hood, reducing the number of explicit calls made to build a text line. See usage
 * examples.
 */
public class LineBuffer implements RandomAccess {
  public static final int MAX_LINE_LENGTH = 80;
  // Default indentation string
  public static final String DEFAULT_INDENT_STR = "   "; // three spaces
  public static final int DEFAULT_INDENT = 1;
  public static final int MAX_ALLOWED_INDENT = MAX_LINE_LENGTH - (2 * Hdl.REMARK_MARKER_LENGTH);

  // Internal buffer holding separate lines.
  private final ArrayList<String> contents = new java.util.ArrayList<>();

  // Paired placeholders.
  private final Pairs pairs = new Pairs();

  private final String SPACE = " ";

  /* ********************************************************************************************* */

  protected LineBuffer() {
    super();
    addDefaultPairs();
  }

  /**
   * Construct LineBuffer with line added to the container.
   *
   * @param line text line to be added to buffer
   */
  public LineBuffer(String line) {
    this();
    add(line);
  }

  /**
   * Constructs LineBuffer instance, then sets placeholder pairs and adds given text line to the
   * container. Note, that because placeholder pairs are set first, you can instantly use these
   * placeholders in your line. Also note that
   *
   * @param line Text line to be added.
   * @param pairsToAdd Placeholder pairs to be used.
   */
  public LineBuffer(String line, Pairs pairsToAdd) {
    this();
    pairs.addPairs(pairsToAdd);
    add(line);
  }

  /**
   * Constructs LineBuffer instance, then adds provided placeholders pairs to be used with the
   * instance.
   *
   * @param pairsToAdd Placeholder pairs to be used.
   */
  public LineBuffer(Pairs pairsToAdd) {
    this();
    pairs.addPairs(pairsToAdd);
  }
  /* ********************************************************************************************* */

  /**
   * Returns instance of LineBuffer with default settings.
   *
   * @return instance of LineBuffer
   */
  public static LineBuffer getBuffer() {
    return new LineBuffer();
  }

  /**
   * Returns instance of LineBuffer preconfigured with HdlPairs.
   *
   * @return instance of LineBuffer
   */
  public static LineBuffer getHdlBuffer() {
    return getBuffer().addHdlPairs();
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

  /**
   * Returns true in case the buffer is empty otherwise false
   *
   * @return the buffer is empty
   */
  public boolean isEmpty() {
    return contents.isEmpty();
  }

  /**
   * Checks if given entry exists in content buffer.
   *
   * @param line Line to look for.
   */
  public boolean contains(String line) {
    return contents.contains(line);
  }

  /**
   * Copies pairs from provided container to internal pair buffer.
   *
   * @param pairsToAdd Pairs to copy.
   */
  public LineBuffer addPairs(Pairs pairsToAdd) {
    pairs.addPairs(pairsToAdd);
    return this;
  }

  /* ********************************************************************************************* */

  /**
   * Adds pairs that are always available.
   *
   * @return Instance of self for easy chaining.
   */
  protected LineBuffer addDefaultPairs() {
    return pair("1u", getDefaultIndent()).pair("2u", getIndent(2)).pair("3u", getIndent(3));
  }

  /**
   * Injects commonly used HDL pairs making them enabled for placeholders.
   *
   * @return Instance of self for easy chaining.
   */
  public LineBuffer addHdlPairs() {
    return pair("assign", Hdl.assignPreamble())
        .pair("=", Hdl.assignOperator())
        .pair("==", Hdl.equalOperator())
        .pair("!=", Hdl.notEqualOperator())
        .pair("or", Hdl.orOperator())
        .pair("and", Hdl.andOperator())
        .pair("xor", Hdl.xorOperator())
        .pair("not", Hdl.notOperator())
        .pair("<", Hdl.bracketOpen())
        .pair(">", Hdl.bracketClose())
        .pair("else", Hdl.elseStatement())
        .pair("endif", Hdl.endIf())
        .pair("0b", Hdl.zeroBit())
        .pair("1b", Hdl.oneBit());
  }

  /**
   * Injects the VHDL keywords making them enabled for placeholders
   *
   * @return Instance of self for easy chaining.
   */
  public LineBuffer addVhdlKeywords() {
    for (final var keyword : Vhdl.getVhdlKeywords()) pair(keyword.toLowerCase(), keyword);
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
   * @return Instance of self for easy chaining.
   */
  public LineBuffer addUnique(String fmt, Object... args) {
    // Resolve positional arguments then apply paired ones.     WE need to do this first (instead of
    // letting add() do that) otherwise `contains` would be looking for non-final version of the
    // string.
    final var line = applyPairs(format(fmt, args));
    if (!contents.contains(line)) add(line, true);
    return this;
  }

  /**
   * Adds line to the buffer only if line is not present already. Please note this implementation of
   * `addUnique()` does not resolve positional arguments. If you use them, you musts pass final form
   * of the string (pass thru `format()` if needed) or use other implementations.
   *
   * @param line Line to add if not present in buffer.
   * @return Instance of self for easy chaining.
   */
  public LineBuffer addUnique(String line) {
    // Resolve positional arguments then apply paired ones.     WE need to do this first (instead of
    // letting add() do that) otherwise `contains` would be looking for non-final version of the
    // string.
    line = applyPairs(line);
    if (!contents.contains(line)) add(line, true);
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
   * Adds single line to the content buffer. Will resolve paried placeholders first, if applyMap is
   * `true`, but won't resolve positionals).
   *
   * @param line line to be added
   * @param applyMap `true` if line shall be processed for placeholders (default), `false` if you
   *     want it to be added "raw".
   * @return Instance of self for easy chaining.
   */
  public LineBuffer add(String line, boolean applyMap) {
    if (applyMap) line = applyPairs(line, pairs);

    // Ensure no placeholders left unprocessed.
    validateLineNoPositionals(line);

    contents.add(line);
    return this;
  }

  /**
   * Adds content of provided StringBuilder to the buffer.
   *
   * @param stringBuilder StringBuilder which contents is to be added.
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
   * @return Instance of self for easy chaining.
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
   * @return Instance of self for easy chaining.
   */
  public LineBuffer addLines(String... lines) {
    return add(Arrays.asList(lines));
  }

  /**
   * Appends content of another buffer
   *
   * @param otherBuffer Another LineBuffer to append content from.
   * @return Instance of self for easy chaining.
   */
  public LineBuffer add(LineBuffer otherBuffer) {
    return add(otherBuffer.get());
  }

  /* ********************************************************************************************* */

  /**
   * Formats provided fmt string using global pairs.
   *
   * @param fnt Formatting string.
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
   * @return Formatted string.
   */
  public static String applyPairs(String format, Pairs pairs) {
    if (pairs != null) {
      for (final var set : pairs.getContainer().entrySet()) {
        final var searchRegExp = String.format("\\{\\{\\s*%s\\s*\\}\\}", set.getKey());
        // Both backslashes (\) and dollar signs ($) in the replacement string may cause the
        // results to be different than if it were being treated as a literal replacement string
        // so as we do not need to support i.e. group references etc, we just need to escape it.
        final var replacement = Matcher.quoteReplacement(set.getValue().toString());
        format = format.replaceAll(searchRegExp, replacement);
      }
    }
    return format;
  }

  /**
   * Adds `line` string to the contents buffer `count` times.
   *
   * @param count Number of times line should be added.
   * @param line String to be added to the buffer.
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
  public List<String> get() {
    return contents;
  }

  /**
   * Returns content buffer as ArrayList(), with each line indented by `DEFAULT_INDENT` using
   * `DEFAULT_INDENT_STR` as a indentation character.
   *
   * @return indented content of the buffer.
   */
  public List<String> getWithIndent() {
    return getWithIndent(getDefaultIndent());
  }

  /**
   * Returns content buffer as ArrayList() with every single entry prefixed by `howMany` characters
   * of `DEFAULT_INDENT_STR`.
   *
   * @param howMany Number of spaces to prefix each line with.
   * @return indented content of the buffer.
   */
  public List<String> getWithIndent(int howMany) {
    return getWithIndent(getIndent(howMany));
  }

  /**
   * Returns content buffer as ArrayList() with every single entry prefixed by `indent` string
   * `howMany` times.
   *
   * @param howMany Number of times `indent` string should be repeated to form the final indent
   *     string.
   * @param indent Indent string.
   * @return indented content of the buffer.
   */
  public List<String> getWithIndent(int howMany, String indent) {
    return getWithIndent(indent.repeat(howMany));
  }

  /**
   * Returns content buffer as ArrayList() with every single entry prefixed by `indent` string.
   *
   * @param indent Indent string.
   * @return indented content of the buffer.
   */
  public List<String> getWithIndent(String indent) {
    final var result = new ArrayList<String>();
    for (final var content : contents) {
      final var lines = content.split("\n");
      for (final var line : lines) {
        // We do not indent empty lines, just ones with content.
        result.add((line.length() == 0) ? line : indent + line);
      }
    }
    return result;
  }

  /** Returns **copy** of internal pair buffer. */
  public Pairs getPairCopy() {
    final var clone = (Pairs) pairs.clone();
    return clone;
  }

  /* ********************************************************************************************* */

  /** Returns default unit of indentation string. */
  public static String getDefaultIndent() {
    return getIndent(DEFAULT_INDENT, DEFAULT_INDENT_STR);
  }

  /**
   * Returns 2 units of default indentation string.
   *
   * @param indentUnits Number of indentation units to return.
   */
  public static String getIndent(int indentUnits) {
    return getIndent(indentUnits, DEFAULT_INDENT_STR);
  }

  /**
   * Returns 3 units of default indentation string.
   *
   * @param indentUnits Number of indentation units to return.
   * @param indentString Indentation string.
   */
  public static String getIndent(int indentUnits, String indentString) {
    return indentString.repeat(indentUnits);
  }

  /* ********************************************************************************************* */

  /**
   * Builds and adds remark block to the contents buffer.
   *
   * @param remarkText Remark text.
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
   * @return Instance of self for easy chaining.
   */
  public LineBuffer addRemarkBlock(String remarkText, int nrOfIndentSpaces) {
    add(buildRemarkBlock(remarkText, nrOfIndentSpaces));
    return this;
  }

  /**
   * Builds remark block.
   *
   * @param remarkText Remark text.
   * @param indentSpaces Number of extra indentation spaces.
   * @return Constructed lines of remark block.
   */
  protected ArrayList<String> buildRemarkBlock(String remarkText, int indentSpaces) {
    if (indentSpaces < 0) {
      throw new IllegalArgumentException("Negative indentation is not allowed.");
    }
    if (indentSpaces > MAX_ALLOWED_INDENT) {
      throw new IllegalArgumentException(
          format(
              "Max allowed indentation is {{1}}, {{2}} given.", MAX_ALLOWED_INDENT, indentSpaces));
    }

    final var maxRemarkLineLength = MAX_LINE_LENGTH - indentSpaces - (2 * Hdl.REMARK_MARKER_LENGTH);
    final var indent = SPACE.repeat(indentSpaces);
    final var contents = new ArrayList<String>();

    final var oneLine = new StringBuilder();
    final var remarkLines =
        List.of(WordUtils.wrap(remarkText, maxRemarkLineLength, "\n", true).split("\n"));

    // Generate header line
    oneLine
        .append(indent)
        .append(Hdl.getRemarkBlockStart())
        .append(Hdl.getRemarkChar().repeat(MAX_LINE_LENGTH - oneLine.length()));
    contents.add(oneLine.toString());
    oneLine.setLength(0);

    for (final var remarkLine : remarkLines) {
      oneLine.append(indent).append(Hdl.getRemarkBlockLineStart()).append(remarkLine);
      if (remarkLine.length() < maxRemarkLineLength) {
        oneLine.append(SPACE.repeat(maxRemarkLineLength - remarkLine.length()));
      }
      oneLine.append(Hdl.getRemarkBlockLineEnd());
      contents.add(oneLine.toString());
      oneLine.setLength(0);
    }

    // We end with generating the last remark line.
    oneLine
        .append(indent)
        .append(
            Hdl.getRemarkChar()
                .repeat(MAX_LINE_LENGTH - oneLine.length() - Hdl.REMARK_MARKER_LENGTH))
        .append(Hdl.getRemarkBlockEnd());

    contents.add(oneLine.toString());

    return contents;
  }

  /**
   * Builds a single remark line
   *
   * @param remarkText text to put in the line
   */
  public LineBuffer addRemarkLine(String remarkText) {
    add("{{1}}{{2}}", Hdl.getLineCommentStart(), remarkText);
    return this;
  }

  /* ********************************************************************************************* */

  /**
   * Formats provided fmt string using provided arguments for positional placeholders.
   *
   * @param fmt Formattting string.
   * @param args Positional placeholders.
   * @return Formatted string.
   */
  public static String format(String fmt, Object... args) {
    return applyPairs(fmt, Pairs.fromArgs(args));
  }

  /**
   * Formats provided fmt string using given arguments for positional placeholders but also includes
   * HDL placeholders.
   *
   * @param fmt Formattting string.
   * @param args Positional placeholders.
   * @return Formatted string.
   */
  public static String formatHdl(String fmt, Object... args) {
    return getHdlBuffer().add(fmt, args).get(0);
  }

  /**
   * Formats provided fmt string using given arguments for positional placeholders but also includes
   * HDL placeholders and VHDL keywords.
   *
   * @param fmt Formattting string.
   * @param args Positional placeholders.
   * @return Formatted string.
   */
  public static String formatVhdl(String fmt, Object... args) {
    return getHdlBuffer().addVhdlKeywords().add(fmt, args).get(0);
  }

  /* ********************************************************************************************* */

  /**
   * Emits warning string to stdout.
   *
   * @param fmt Formatting string.
   * @param args Positional arguments.
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

  private List<String> placeholders = new ArrayList<>();
  private final List<String> positionalPlaceholders = new ArrayList<>();
  private final List<String> pairedPlaceholders = new ArrayList<>();

  // check if we have positional args and/or paired
  // if positional: check if we have args given, then check all the rest
  // for paired check mapping vs global map and one-time pairs

  /**
   * Parses formatting string looking for positional placeholders and then nitializes internal
   * validator data.
   *
   * @param fmt Formatting string to analize.
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
    validateLine(fmt, null);
  }

  /**
   * Ensures all positional placeholders match provided arguments, there's no "too much" positional
   * arguments etc.
   *
   * @param fmt Formatting string to analize.
   * @param args Positional placeholders.
   */
  protected void validateLineWithPositionalArgs(String fmt, Object... args) {
    initValidator(fmt);

    final var posArgsCnt = positionalPlaceholders.size();

    // Do we have positional placeholders in fmt?
    if (positionalPlaceholders.isEmpty()) {
      // Warn if we have no positional placeholders used, but still receive positional arguments.
      if (args.length > 0)
        warn(
            "#E004: Useless positional arguments. Expected nothing, but received {{2}} for '{{1}}'.",
            fmt, posArgsCnt);
    } else {
      if (posArgsCnt < args.length)
        // We had too many positional args given compared to positional placeholders. But that
        // difference can be OK, so just warn.
        abort(
            "#E001: Too many positional arguments, Expected {{2}}, but received {{3}} for '{{1}}'.",
            fmt, posArgsCnt, args.length);

      if (posArgsCnt > args.length)
        // Too little arguments provided vs. awaiting placeholders. That's life threatening
        // condition.
        abort(
            "#E002: Insufficient positional arguments. Expected {{2}}, but received {{3}} for '{{1}}'.",
            fmt, posArgsCnt, args.length);

      // count matches, let's see if contents too.
      for (final var posKey : positionalPlaceholders) {
        if (Integer.parseInt(posKey) > posArgsCnt) {
          // Reference to non-existing position found. Warn about all detected issues. We fail
          // later.
          warn(
              "#E003: Invalid positional argument. '{{1}}' used, but max value is {{2}} for '{{3}}'.",
              posKey, posArgsCnt, fmt);
        }
      }
    }
  }

  /**
   * Ensures formatting string is not using undefined placeholder.
   *
   * @param fmt Formatting string to analize.
   */
  protected void validateLineWithPairedPlaceholders(String fmt) {
    initValidator(fmt);

    // check if we use any non mapped placeholder
    for (final var key : pairedPlaceholders) {
      if (!placeholders.contains(key)) {
        abort("#E005: Placeholder '{{1}}' has no mapping while processing '{{2}}'.", key, fmt);
      }
    }
  }

  /**
   * Ensures profided formatting strings can be succesfuly handled as all placeholders, positional
   * or not are present.
   *
   * @param fmt Formatting string to analize.
   * @param argPairs Positional placeholders.
   */
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
    for (final var key : pairedPlaceholders) {
      if (!(pairs.getContainer().containsKey(key)
          || (argPairs != null && argPairs.getContainer().containsKey(key)))) {
        abort("#E006: No mapping for '{{1}}' placeholder in '{{2}}'.", key, fmt);
      }
    }
  }

  /* ********************************************************************************************* */

  /**
   * Extract names of valid placeholders found in provided string.
   *
   * @param fmt String to analyze.
   * @return Returns list of found placeholders. If no placeholder is found, returnes empty list.
   */
  public List<String> extractPlaceholders(String fmt) {
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

  /** Returns string representation of the internal container. */
  @Override
  public String toString() {
    return contents.toString();
  }

  /* ********************************************************************************************* */

  /**
   * Create new pair and adds it to internal pair contaier.
   *
   * @param key Pair's key.
   * @param value Pair's value.
   * @return Instance of self for easy chaining.
   */
  public LineBuffer pair(String key, Object value) {
    pairs.pair(key, value);
    return this;
  }

  /* ********************************************************************************************* */

  /** Container holding all the key-value pairs used by LineBuffer. */
  public static class Pairs implements Cloneable {
    /** Internal pair container. */
    private final HashMap<String, Object> pairContainer = new HashMap<>();

    /** Default constructor. */
    public Pairs() {
      // default constructor does nothing special.
    }

    /**
     * Creates pairs container and adds new pair to it.
     *
     * @param key Pair's key.
     * @param value Pair's value.
     */
    public Pairs(String key, Object value) {
      pair(key, value);
    }

    /**
     * Constructs Pairs map from positional arguments auto-assigning numerical placeholders `{{x}}`
     * where `x` is integer starting from `1`.
     *
     * @param args Arguments to use to build the map.
     */
    public static Pairs fromArgs(Object... args) {
      final var map = new Pairs();
      var idx = 1;
      for (final var arg : args) {
        map.addPositionalPair(String.valueOf(idx++), arg.toString());
      }
      return map;
    }

    /**
     * Creates new, non-positional pair and adds to internal buffer.
     *
     * @param key Pair's key.
     * @param value Pair's value.
     * @return Returns instance of container for easy chaining.
     */
    public Pairs pair(String key, Object value) {
      return addNonPositionalPair(key, value);
    }

    /**
     * Adds new, non-positional pair to the container.
     *
     * @param key Pair's key.
     * @param value Pair's value.
     * @return Returns instance of container for easy chaining.
     * @throws RuntimeException for invalid keys.
     */
    public Pairs addNonPositionalPair(String key, Object value) {
      // Numeric only keys are not allowed because these are reserved for positional placeholders.
      if (key.matches("^\\d+$")) {
        throw new RuntimeException(
            format("Invalid pair key '{{1}}'. You cannot add positional arguments as pairs.", key));
      }
      pairContainer.put(key, value);
      return this;
    }

    /**
     * Adds pairs form another container.
     *
     * @param pairs Pair container to copy pairs from.
     * @return Returns instance of container for easy chaining.
     */
    public Pairs addPairs(Pairs pairs) {
      for (final var pair : pairs.entrySet()) {
        addNonPositionalPair(pair.getKey(), pair.getValue());
      }
      return this;
    }

    /**
     * Adds given pair, ensuring it's for postional placeholders only.
     *
     * @param key Pair's key.
     * @param value Pair's value.
     * @return Returns instance of container for easy chaining.
     */
    public Pairs addPositionalPair(String key, Object value) {
      if (!key.matches("^\\d+$")) {
        throw new RuntimeException(
            format("Invalid pair key '{{1}}'. Positional arguments' keys must be numeric.", key));
      }
      pairContainer.put(key, value);
      return this;
    }

    /**
     * Clears pair container.
     *
     * @return Returns instance of container for easy chaining.
     */
    public Pairs clear() {
      pairContainer.clear();
      return this;
    }

    /** Returns instance of internal HashMap holding all the pairs. */
    protected HashMap<String, Object> getContainer() {
      return pairContainer;
    }

    /** Returns entrySet for internal pair container for easier iteration over it. */
    public Set<Map.Entry<String, Object>> entrySet() {
      return pairContainer.entrySet();
    }

    /** Clones current instance of Pairs. */
    @Override
    protected Pairs clone() {
      final var clone = new Pairs();
      for (final var pair : pairContainer.entrySet()) {
        clone.pair(pair.getKey(), pair.getValue());
      }
      return clone;
    }

    /** Returns string representation of the internal container. */
    @Override
    public String toString() {
      return pairContainer.toString();
    }
  } // end of Pairs
} // end of LineBuffer
