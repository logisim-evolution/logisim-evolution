/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.soc.data;

import java.util.HashSet;
import javax.swing.text.Segment;
import org.fife.ui.rsyntaxtextarea.AbstractTokenMaker;
import org.fife.ui.rsyntaxtextarea.RSyntaxUtilities;
import org.fife.ui.rsyntaxtextarea.Token;
import org.fife.ui.rsyntaxtextarea.TokenMap;

public class AssemblerHighlighter extends AbstractTokenMaker {
  public static final int REPEAT_LAST = -1;
  public static final int DOUBLE_QUOTE_END = -2;
  public static final int MAYBE_SHIFT_LEFT = -3;
  public static final int MAYBE_SHIFT_RIGHT = -4;
  public static final int SHIFT_END = -5;
  private boolean escape = false;

  private static final String[] directives = {
      ".ascii", ".align", ".file", ".globl", ".local", ".comm", ".common", ".ident",
      ".section", ".size", ".text", ".data", ".rodata", ".bss", ".string", ".p2align",
      ".asciz", ".equ", ".macro", ".endm", ".type", ".option", ".byte", ".2byte", ".half",
      ".short", ".4byte", ".word", ".long", ".8byte", ".dword", ".quad", ".balign",
      ".zero", ".org"};

  @SuppressWarnings("serial")
  public static final HashSet<String> BYTES = new HashSet<>() {{
      add(".byte");
  }};
  @SuppressWarnings("serial")
  public static final HashSet<String> SHORTS = new HashSet<>() {{
      add(".half");
      add(".2byte");
      add(".short");
  }};
  @SuppressWarnings("serial")
  public static final HashSet<String> INTS = new HashSet<>() {{
      add(".word");
      add(".4byte");
      add(".long");
  }};
  @SuppressWarnings("serial")
  public static final HashSet<String> LONGS = new HashSet<>() {{
      add(".dword");
      add(".8byte");
      add(".quad");
  }};
  @SuppressWarnings("serial")
  public static final HashSet<String> STRINGS = new HashSet<>() {{
      add(".ascii");
      add(".asciz");
      add(".string");
  }};


  @Override
  public TokenMap getWordsToHighlight() {
    TokenMap map = new TokenMap();
    for (String directive : directives)
      map.put(directive, Token.FUNCTION);
    return map;
  }

  @Override
  public void addToken(Segment segment, int start, int end, int tokenType, int startOffset) {
    // This assumes all keywords, etc. were parsed as "identifiers."
    if (tokenType == Token.IDENTIFIER) {
      int value = wordsToHighlight.get(segment, start, end);
      if (value != -1) {
        tokenType = value;
      }
    }
    super.addToken(segment, start, end, tokenType, startOffset);
  }

  private int check(Segment text, char kar, int currentToken, int start, int index, int newStart) {
    int currentTokenType = currentToken >= 0 ? currentToken : Token.LITERAL_CHAR;
    if (currentTokenType == Token.COMMENT_EOL) return Token.COMMENT_EOL;
    if (currentTokenType == Token.LITERAL_STRING_DOUBLE_QUOTE && (kar != '"' || escape)) {
      escape = kar == '\\';
      return Token.LITERAL_STRING_DOUBLE_QUOTE;
    }
    switch (kar) {
      case ' ':
      case '\t':
        if (currentTokenType != Token.NULL && currentTokenType != Token.WHITESPACE)
          addToken(text, start, index - 1, currentTokenType, newStart);
        return Token.WHITESPACE;
      case '"':
        if (currentTokenType == Token.LITERAL_STRING_DOUBLE_QUOTE) {
          addToken(text, start, index, currentTokenType, newStart);
          return DOUBLE_QUOTE_END;
        }
        if (currentTokenType != Token.NULL)
          addToken(text, start, index - 1, currentTokenType, newStart);
        escape = false;
        return Token.LITERAL_STRING_DOUBLE_QUOTE;
      case '#':
        if (currentTokenType != Token.NULL)
          addToken(text, start, index - 1, currentTokenType, newStart);
        return Token.COMMENT_EOL;
      case '<':
        if (currentToken != MAYBE_SHIFT_LEFT) {
          if (currentTokenType != Token.NULL)
            addToken(text, start, index - 1, currentTokenType, newStart);
          return MAYBE_SHIFT_LEFT;
        } else {
          addToken(text, start, index, currentTokenType, newStart);
          return SHIFT_END;
        }
      case '>':
        if (currentToken != MAYBE_SHIFT_RIGHT) {
          if (currentTokenType != Token.NULL)
            addToken(text, start, index - 1, currentTokenType, newStart);
          return MAYBE_SHIFT_RIGHT;
        } else {
          addToken(text, start, index, currentTokenType, newStart);
          return SHIFT_END;
        }
      case '@':
        if (currentTokenType != Token.NULL)
          addToken(text, start, index - 1, currentTokenType, newStart);
        return currentTokenType == Token.PREPROCESSOR ? REPEAT_LAST : Token.PREPROCESSOR;
      case '(':
      case ')':
      case '{':
      case '}':
      case '[':
      case ',':
      case ':':
      case '+':
      case '-':
      case '*':
      case '/':
      case '%':
      case ']':
        if (currentTokenType != Token.NULL)
          addToken(text, start, index - 1, currentTokenType, newStart);
        return currentTokenType == Token.LITERAL_CHAR ? REPEAT_LAST : Token.LITERAL_CHAR;
      case 'x':
      case 'X':
        if (currentTokenType == Token.LITERAL_NUMBER_DECIMAL_INT) {
          return Token.LITERAL_NUMBER_HEXADECIMAL;
        }
    }
    if (currentTokenType == Token.IDENTIFIER) return Token.IDENTIFIER;
    if (RSyntaxUtilities.isDigit(kar)) {
      if (currentTokenType == Token.PREPROCESSOR) return Token.PREPROCESSOR;
      if (currentTokenType != Token.NULL
          && currentTokenType != Token.LITERAL_NUMBER_DECIMAL_INT
          && currentTokenType != Token.LITERAL_NUMBER_HEXADECIMAL)
        addToken(text, start, index - 1, currentTokenType, newStart);
      return currentTokenType != Token.LITERAL_NUMBER_HEXADECIMAL
          ? Token.LITERAL_NUMBER_DECIMAL_INT
          : currentTokenType;
    }
    if (RSyntaxUtilities.isHexCharacter(kar)
        && currentTokenType == Token.LITERAL_NUMBER_HEXADECIMAL) return currentTokenType;
    if (currentTokenType != Token.NULL)
      addToken(text, start, index - 1, currentTokenType, newStart);
    return Token.IDENTIFIER;
  }

  @Override
  public Token getTokenList(Segment arg0, int arg1, int arg2) {
    resetTokenList();

    char[] array = arg0.array;
    int offset = arg0.offset;
    int count = arg0.count;
    int end = offset + count;
    int newStartOffset = arg2 - offset;

    int currentTokenStart = offset;
    int currentTokenType = arg1;

    escape = false;
    for (int i = offset; i < end; i++) {
      char c = array[i];
      int newTokenType =
          check(
              arg0, c, currentTokenType, currentTokenStart, i, newStartOffset + currentTokenStart);
      if (newTokenType != currentTokenType
          && !(newTokenType == Token.LITERAL_NUMBER_HEXADECIMAL
              && currentTokenType == Token.LITERAL_NUMBER_DECIMAL_INT)) currentTokenStart = i;
      if (newTokenType == DOUBLE_QUOTE_END || newTokenType == SHIFT_END) {
        currentTokenStart = i + 1;
        currentTokenType = Token.NULL;
      } else if (newTokenType == REPEAT_LAST) currentTokenStart = i;
      else currentTokenType = newTokenType;
    }
    switch (currentTokenType) {
      case Token.LITERAL_STRING_DOUBLE_QUOTE:
        addToken(
            arg0, currentTokenStart, end - 1, currentTokenType, newStartOffset + currentTokenStart);
        break;
      case Token.NULL:
        addNullToken();
        break;
      default:
        addToken(
            arg0, currentTokenStart, end - 1, currentTokenType, newStartOffset + currentTokenStart);
        addNullToken();
    }
    return firstToken;
  }
}
