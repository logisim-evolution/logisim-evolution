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

/**
 * This class is intended to simplify building any HDL content, which usually contains of fixed text
 * lines and parametrized strings. This class offers wrapper methods that can format the strings
 * under the hood, reducing the number of explicit calls made to build a text line. See usage
 * examples.
 */
public class ContentBuilder {
  private static final int DEFAULT_INDENT = 3;

  private ArrayList<String> contents;

  public static final int maxLineLength = 80;

  public ContentBuilder() {
    contents = new java.util.ArrayList<String>();
  }

  public ContentBuilder clear() {
    contents.clear();
    return this;
  }

  /**
   * Adds single line to the content buffer.
   *
   * @param line
   * @return
   */
  public ContentBuilder add(String line) {
    contents.add(line);
    return this;
  }

  /**
   * Formats string using @String.format() and adds to the buffer.
   *
   * @param fmt Formatting string as accepted by String.format()
   * @param args Optional arguments
   * @return
   */
  public ContentBuilder add(String fmt, Object... args) {
    return add(String.format(fmt, args));
  }

  public ContentBuilder repeat(int count, String line) {
    for (var i = 0; i < count; i++) contents.add(line);
    return this;
  }

  /**
   * Appends single empty line to the content buffer.
   */
  public ContentBuilder empty() {
    return repeat(1, "");
  }

  /**
   * Appends `lines` count of empty line to the content buffer.
   */
  public ContentBuilder empty(int lines) {
    return repeat(lines, "");
  }

  /**
   * Adds
   * @param lines
   * @return
   */
  public ContentBuilder add(String... lines) {
    return add(Arrays.asList(lines));
  }

  /**
   * Adds all lines from given collection to content buffer.
   *
   * @param lines
   * @return
   */
  public ContentBuilder add(Collection<String> lines) {
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

  public ContentBuilder addRemarkBlock(String remarkText) {
    return addRemarkBlock(remarkText, DEFAULT_INDENT);
  }

  public ContentBuilder addRemarkBlock(String remarkText, Integer nrOfIndentSpaces) {
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
