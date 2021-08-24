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
import java.util.Map;

/**
 * This class is intended to simplify building any HDL content, which usually contains of fixed text
 * lines and parametrized strings. This class offers wrapper methods that can format the strings
 * under the hood, reducing the number of explicit calls made to build a text line. See usage
 * examples.
 */
public class LineBuffer {
  private static final int DEFAULT_INDENT = 3;

  private ArrayList<String> contents;

  public static final int maxLineLength = 80;

  public LineBuffer() {
    contents = new java.util.ArrayList<String>();
  }

  public LineBuffer clear() {
    contents.clear();
    return this;
  }

  /**
   * Adds single line to the content buffer.
   *
   * @param line String to be added to the content buffer.
   */
  public LineBuffer add(String line) {
    contents.add(line);
    return this;
  }

  /**
   * Formats string using @String.format() and adds to the buffer.
   *
   * @param fmt Formatting string as accepted by String.format()
   * @param args Optional arguments
   */
  public LineBuffer add(String fmt, Object... args) {
    return add(String.format(fmt, args));
  }

  /**
   * Key-Value formatter. Will look for all occurences of keys from provided map and replace with
   * values. To enforce key uniquess and avoid too-greedy replacing, the fmt string must wrap key in
   * double curly braces, i.e. in map `("foo", "bar")`, in formatting string `This {{key}} will be
   * replaced.`. Processed string is then added to content buffer.
   *
   * @param fmt Formatting string. Wrap keys in `{{` and `}}`.
   * @param map Search-Replace map.
   */
  public LineBuffer add(String fmt, HashMap<String, String> map) {
    for (Map.Entry<String, String> set : map.entrySet()) {
      final var search = String.format("{{%s}}", set.getKey());
      fmt = fmt.replace(search, set.getValue());
    }
    add(fmt);

    return this;
  }

  public LineBuffer repeat(int count, String line) {
    for (var i = 0; i < count; i++) contents.add(line);
    return this;
  }

  /** Appends single empty line to the content buffer. */
  public LineBuffer empty() {
    return repeat(1, "");
  }

  /** Appends `lines` count of empty line to the content buffer. */
  public LineBuffer empty(int lines) {
    return repeat(lines, "");
  }

  public LineBuffer add(String... lines) {
    return add(Arrays.asList(lines));
  }

  /**
   * Adds all lines from given collection to content buffer.
   *
   * @param lines
   * @return
   */
  public LineBuffer add(Collection<String> lines) {
    contents.addAll(lines);
    return this;
  }

  /**
   * Returns content buffer as ArrayList()
   *
   * @return
   */
  public ArrayList<String> get() {
    return contents;
  }

  public LineBuffer addRemarkBlock(String remarkText) {
    return addRemarkBlock(remarkText, DEFAULT_INDENT);
  }

  public LineBuffer addRemarkBlock(String remarkText, Integer nrOfIndentSpaces) {
    add(buildRemarkBlock(remarkText, nrOfIndentSpaces));
    return this;
  }

  /* Here all global helper methods are defined */
  protected ArrayList<String> buildRemarkBlock(String remarkText, Integer nrOfIndentSpaces) {
    final var maxRemarkLength = maxLineLength - 2 * HDL.remarkOverhead() - nrOfIndentSpaces;
    var remarkWords = remarkText.split(" ");
    var oneLine = new StringBuilder();
    var contents = new ArrayList<String>();
    var maxWordLength = 0;
    for (var word : remarkWords) if (word.length() > maxWordLength) maxWordLength = word.length();
    if (maxRemarkLength < maxWordLength) return contents;
    /* we start with generating the first remark line */
    while (oneLine.length() < nrOfIndentSpaces) oneLine.append(" ");
    for (var i = 0; i < maxLineLength - nrOfIndentSpaces; i++) {
      oneLine.append(HDL.getRemakrChar(i == 0, i == maxLineLength - nrOfIndentSpaces - 1));
    }
    contents.add(oneLine.toString());
    oneLine.setLength(0);
    /* Next we put the remark text block in 1 or multiple lines */
    for (var remarkWord : remarkWords) {
      if ((oneLine.length() + remarkWord.length() + HDL.remarkOverhead()) > (maxLineLength - 1)) {
        /* Next word does not fit, we end this line and create a new one */
        while (oneLine.length() < (maxLineLength - HDL.remarkOverhead())) {
          oneLine.append(" ");
        }
        oneLine
            .append(" ")
            .append(HDL.getRemakrChar(false, false))
            .append(HDL.getRemakrChar(false, false));
        contents.add(oneLine.toString());
        oneLine.setLength(0);
      }
      while (oneLine.length() < nrOfIndentSpaces) oneLine.append(" ");
      if (oneLine.length() == nrOfIndentSpaces) {
        /* we put the preamble */
        oneLine.append(HDL.getRemarkStart());
      }
      if (remarkWord.endsWith("\\")) {
        /* Forced new line */
        oneLine.append(remarkWord, 0, remarkWord.length() - 1);
        while (oneLine.length() < (maxLineLength - HDL.remarkOverhead())) oneLine.append(" ");
      } else {
        oneLine.append(remarkWord).append(" ");
      }
    }
    if (oneLine.length() > (nrOfIndentSpaces + HDL.remarkOverhead())) {
      /* we have an unfinished remark line */
      while (oneLine.length() < (maxLineLength - HDL.remarkOverhead())) oneLine.append(" ");
      oneLine
          .append(" ")
          .append(HDL.getRemakrChar(false, false))
          .append(HDL.getRemakrChar(false, false));
      contents.add(oneLine.toString());
      oneLine.setLength(0);
    }
    /* we end with generating the last remark line */
    while (oneLine.length() < nrOfIndentSpaces) oneLine.append(" ");
    for (var i = 0; i < maxLineLength - nrOfIndentSpaces; i++)
      oneLine.append(HDL.getRemakrChar(i == maxLineLength - nrOfIndentSpaces - 1, i == 0));
    contents.add(oneLine.toString());
    return contents;
  }
}
