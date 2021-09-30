/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.analyze.model;

import static com.cburch.logisim.analyze.Strings.S;

import com.cburch.logisim.analyze.model.Expression.Notation;
import com.cburch.logisim.util.StringGetter;
import java.util.ArrayList;
import java.util.function.Predicate;

public class Parser {
  private Parser() {
    // dummy, private
  }

  private static class Context {
    final int level;
    final Expression current;
    final Token cause;

    Context(Expression current, int level, Token cause) {
      this.level = level;
      this.current = current;
      this.cause = cause;
    }
  }

  private static class Token {
    int type;
    final int offset;
    final int length;
    int precedence;
    final String text;

    Token(int type, int offset, int length, String text, int precedence) {
      this.type = type;
      this.offset = offset;
      this.length = length;
      this.text = text;
      this.precedence = precedence;
    }

    Token(int type, int offset, String text, int precedence) {
      this(type, offset, text.length(), text, precedence);
    }

    ParserException error(StringGetter message) {
      return new ParserException(message, offset, length);
    }
  }

  private static boolean okCharacter(char c) {
    return Character.isWhitespace(c)
        || Character.isJavaIdentifierStart(c)
        || "()01~-^+*!&|=\\':[]".indexOf(c) >= 0
        || "\u2260\u2262\u22C0\u22C1\u2227\u2228\u2295\u22C5\u00AC\u2219".indexOf(c) >= 0
        || "\u21D4\u2261\u2194\u02DC\u00B7\u2225\u22BB\u22A4\u22A5".indexOf(c) >= 0;
  }

  public static Expression parseMaybeAssignment(String in, AnalyzerModel model)
          throws ParserException {
    return parse(in, model, true);
  }

  private static Expression parse(ArrayList<Token> tokens) throws ParserException {
    final var stack = new ArrayList<Context>();
    Expression current = null;
    for (var i = 0; i < tokens.size(); i++) {
      final var t = tokens.get(i);
      if (t.type == TOKEN_IDENT || t.type == TOKEN_CONST) {
        Expression here;
        if (t.type == TOKEN_IDENT) {
          here = Expressions.variable(t.text);
        } else {
          here = Expressions.constant(Integer.parseInt(t.text, 16));
        }
        while (i + 1 < tokens.size() && tokens.get(i + 1).type == TOKEN_NOT_POSTFIX) {
          here = Expressions.not(here);
          i++;
        }
        while (peekLevel(stack) == Expression.Notation.NOT_PRECEDENCE) {
          here = Expressions.not(here);
          pop(stack);
        }
        current = Expressions.and(current, here);
        if (peekLevel(stack) == Expression.Notation.IMPLICIT_AND_PRECEDENCE) {
          Context top = pop(stack);
          current = Expressions.and(top.current, current);
        }
      } else if (t.type == TOKEN_NOT) {
        if (current != null) {
          push(
              stack,
              current,
              Expression.Notation.IMPLICIT_AND_PRECEDENCE,
              new Token(TOKEN_AND, t.offset, S.get("implicitAndOperator"), Notation.IMPLICIT_AND_PRECEDENCE));
        }
        push(stack, null, Expression.Notation.NOT_PRECEDENCE, t);
        current = null;
      } else if (t.type == TOKEN_NOT_POSTFIX) {
        throw t.error(S.getter("unexpectedApostrophe"));
      } else if (t.type == TOKEN_LPAREN) {
        if (current != null) {
          push(stack, current, Notation.IMPLICIT_AND_PRECEDENCE,
              new Token(TOKEN_AND, t.offset, 0, S.get("implicitAndOperator"), Notation.IMPLICIT_AND_PRECEDENCE));
        }
        push(stack, null, -2, t);
        current = null;
      } else if (t.type == TOKEN_RPAREN) {
        current = popTo(stack, -1, current);
        // there had better be a LPAREN atop the stack now.
        if (stack.isEmpty()) {
          throw t.error(S.getter("lparenMissingError"));
        }
        pop(stack);
        while (i + 1 < tokens.size() && tokens.get(i + 1).type == TOKEN_NOT_POSTFIX) {
          current = Expressions.not(current);
          i++;
        }
        current = popTo(stack, Notation.IMPLICIT_AND_PRECEDENCE, current);
      } else {
        if (current == null) {
          throw t.error(S.getter("missingLeftOperandError", t.text));
        }
        push(stack, popTo(stack, t.precedence, current), t.precedence, t);
        current = null;
      }
    }
    current = popTo(stack, -1, current);
    if (!stack.isEmpty()) {
      Context top = pop(stack);
      throw top.cause.error(S.getter("rparenMissingError"));
    }
    return current;
  }

  public static Expression parse(String in, AnalyzerModel model) throws ParserException {
    return parse(in, model, false);
  }

  private static Expression parse(String in, AnalyzerModel model, boolean allowOutputAssignment)
      throws ParserException {
    final var tokens = toTokens(in, false);

    if (tokens.isEmpty()) return null;

    var i = -1;
    for (final var token : tokens) {
      i++;
      if (token.type == TOKEN_ERROR_BADCHAR) {
        throw token.error(S.getter("invalidCharacterError", token.text));
      } else if (token.type == TOKEN_ERROR_BRACE) {
        throw token.error(S.getter("missingBraceError", token.text));
      } else if (token.type == TOKEN_ERROR_SUBSCRIPT) {
        throw token.error(S.getter("missingSubscriptError", token.text));
      } else if (token.type == TOKEN_ERROR_IDENT) {
        throw token.error(S.getter("missingIdentifierError", token.text));
      } else if (token.type == TOKEN_EQ && (i != 1 || !allowOutputAssignment)) {
        throw token.error(S.getter("unexpectedAssignmentError", token.text));
      } else if (token.type == TOKEN_IDENT) {
        int index = model.getInputs().bits.indexOf(token.text);
        if (index < 0) {
          // ok; but maybe this is an  a python-like (spelled out) operator
          final var opText = token.text.toUpperCase();
          if (opText.equals("NOT")) {
            token.type = TOKEN_NOT;
            token.precedence = Expression.Notation.NOT_PRECEDENCE;
          } else if (opText.equals("AND")) {
            token.type = TOKEN_AND;
            token.precedence = Expression.Notation.PYTHON_AND_PRECEDENCE;
          } else if (opText.equals("XOR")) {
            token.type = TOKEN_XOR;
            token.precedence = Expression.Notation.PYTHON_XOR_PRECEDENCE;
          } else if (opText.equals("OR")) {
            token.type = TOKEN_OR;
            token.precedence = Expression.Notation.PYTHON_OR_PRECEDENCE;
          } else if (opText.contentEquals("EQUALS")) {
            token.type = TOKEN_XNOR;
            token.precedence = Expression.Notation.LOGIC_PRECEDENCE;
          } else {
            // or, maybe it is a top-level assignment like "foo: expr", "foo = expr", etc
            if (i == 0 && allowOutputAssignment) {
              index = model.getOutputs().bits.indexOf(token.text);
              if (index >= 0
                  && tokens.size() >= 2
                  && (tokens.get(1).type == TOKEN_XNOR || tokens.get(1).type == TOKEN_EQ)) {
                tokens.get(1).type = TOKEN_EQ;
                tokens.get(1).precedence = Expression.Notation.EQ_PRECEDENCE;
                continue;
              }
            }
            throw token.error(S.getter("badVariableName", token.text));
          }
        }
      }
    }

    return parse(tokens);
  }

  private static int peekLevel(ArrayList<Context> stack) {
    if (stack.isEmpty()) return -3;
    final var context = stack.get(stack.size() - 1);
    return context.level;
  }

  private static Context pop(ArrayList<Context> stack) {
    return stack.remove(stack.size() - 1);
  }

  private static Expression popTo(ArrayList<Context> stack, int level, Expression current)
      throws ParserException {
    while (!stack.isEmpty() && peekLevel(stack) >= level) {
      final var top = pop(stack);
      if (current == null)
        throw top.cause.error(S.getter("missingRightOperandError", top.cause.text));
      else if (top.cause.type == TOKEN_AND)
          current = Expressions.and(top.current, current);
      else if (top.cause.type == TOKEN_OR)
          current = Expressions.or(top.current, current);
      else if (top.cause.type == TOKEN_XOR)
          current = Expressions.xor(top.current, current);
      else if (top.cause.type == TOKEN_XNOR)
          current = Expressions.xnor(top.current, current);
      else if (top.cause.type == TOKEN_EQ)
          current = Expressions.eq(top.current, current);
      else if (top.cause.type == TOKEN_NOT)
          current = Expressions.not(current);
    }
    return current;
  }

  private static void push(ArrayList<Context> stack, Expression expr, int level, Token cause) {
    stack.add(new Context(expr, level, cause));
  }

  //Note: Doing this without "tokenizing then re-stringify" is tricky.
  static String replaceVariable(String in, String oldName, String newName) {
    final var ret = new StringBuilder();
    final var tokens = toTokens(in, true);
    for (Token token : tokens) {
      if (token.type == TOKEN_IDENT && token.text.equals(oldName)) {
        ret.append(newName);
      } else {
        ret.append(token.text);
      }
    }
    return ret.toString();
  }

  static class Tokenizer {
    private final String in;
    private final boolean includeWhite;
    private int pos;
    private final int len;

    public Tokenizer(String in, boolean includeWhite) {
      this.len = in.length();
      // Guarantee that we will stop just after reading whitespace,
      // not in the middle of a token.
      this.in = in + " ";
      this.includeWhite = includeWhite;
    }

    boolean skipWhile(Predicate<Character> pred) {
      while (pos < len && pred.test(peek())) pos++;
      return pos == len;
    }

    boolean skipUntil(Predicate<Character> pred) {
      return skipWhile(pred.negate());
    }

    boolean skipSpaces() {
      return skipWhile(Character::isWhitespace);
    }

    String readNumber() {
      final var substart = pos;
      skipWhile(this::isDigit);
      return in.substring(substart, pos);
    }

    boolean isDigit(char c) {
      return c >= '0' && c <= '9';
    }

    boolean accept(char c) {
      if (peek() == c) {
        pos++;
        return true;
      }

      return false;
    }

    char peek() {
      return in.charAt(pos);
    }

    char next() {
      return in.charAt(pos++);
    }

    Token readToken(char startChar, int start) {
      switch (startChar) {
        case '(':
          return new Token(TOKEN_LPAREN, start, "(", Integer.MAX_VALUE);
        case ')':
          return new Token(TOKEN_RPAREN, start, ")", Integer.MAX_VALUE);
        case '1':
        case '\u22A4': // down tack
          return new Token(TOKEN_CONST, start, "1", Integer.MAX_VALUE);
        case '0':
        case '\u22A5': // up tack
          return new Token(TOKEN_CONST, start, "0", Integer.MAX_VALUE);
        case '~':
        case '-':
        case '\u00AC': // logical not
        case '\u02DC': // tilde
          return new Token(TOKEN_NOT, start, "~", Notation.NOT_PRECEDENCE);
        case '!':
          if (accept('=')) {
            return new Token(TOKEN_XOR, start, in.substring(start, pos), Notation.LOGIC_PRECEDENCE);
          } else {
            return new Token(TOKEN_NOT, start, "~", Notation.NOT_PRECEDENCE);
          }
        case '\'':
          return new Token(TOKEN_NOT_POSTFIX, start, "'", Notation.NOT_PRECEDENCE);
        case '^':
        case '\u2295': // oplus
          return new Token(TOKEN_XOR, start, "^", Notation.OPLUS_PRECEDENCE);
        case '\u22BB': // vee-underbar
        case '\u2262': // not-equiv
        case '\u2260': // not-equals
          return new Token(TOKEN_XOR, start, "^", Notation.LOGIC_PRECEDENCE);
        case '+':
        case '\u22C1': // large disjunction
        case '\u2228': // small disjunction
          return new Token(TOKEN_OR, start, "+", Notation.LOGIC_PRECEDENCE);
        case '\u2225': // logical or
          return new Token(TOKEN_OR, start, "+", Notation.OR_PRECEDENCE);
        case '*':
        case '\u22C0': // large conjunction
        case '\u2227': // small conjunction
          return new Token(TOKEN_AND, start, "*", Notation.LOGIC_PRECEDENCE);
        case '\u22C5': // cdot
        case '\u2219': // bullet
        case '\u00B7': // middle-dot
          return new Token(TOKEN_AND, start, "*", Notation.TIMES_PRECEDENCE);
        case '\u2299': // otimes
          return new Token(TOKEN_XNOR, start, "^", Notation.OTIMES_PRECEDENCE);
        case '\u21D4': // left-right-doublearrow
        case '\u2261': // equiv
        case '\u2194': // left-right-arrow
          return new Token(TOKEN_XNOR, start, "=", Notation.LOGIC_PRECEDENCE);
        case '&':
          if (accept('&')) {
            return new Token(TOKEN_AND, start, "&&", Notation.AND_PRECEDENCE);
          } else {
            return new Token(TOKEN_AND, start, "&", Notation.BITAND_PRECEDENCE);
          }
        case '|':
          if (accept('|')) {
            return new Token(TOKEN_OR, start, "||", Notation.OR_PRECEDENCE);
          } else {
            return new Token(TOKEN_OR, start, "|", Notation.BITOR_PRECEDENCE);
          }
        case '=':
          accept('=');
          return new Token(TOKEN_XNOR, start, in.substring(start, pos), Notation.LOGIC_PRECEDENCE);
        case ':':
          accept('=');
          return new Token(TOKEN_EQ, start, in.substring(start, pos), Notation.EQ_PRECEDENCE);
        case '[':
        case ']':
          return new Token(TOKEN_ERROR_IDENT, start, in.substring(start, start + 1), 0);
        default:
          skipUntil(Parser::okCharacter);
          final var errorText = in.substring(start, pos);
          return new Token(TOKEN_ERROR_BADCHAR, start, errorText, 0);
      }
    }

    ArrayList<Token> tokenize() {
      ArrayList<Token> tokens = new ArrayList<>();

      pos = 0;
      while (true) {
        final var whiteStart = pos;
        skipSpaces();

        if (includeWhite && pos != whiteStart) {
          tokens.add(new Token(TOKEN_WHITE, whiteStart, in.substring(whiteStart, pos), 0));
        }
        if (pos == len) {
          return tokens;
        }

        final var start = pos;
        final var startChar = next();
        if (Character.isJavaIdentifierStart(startChar)) {
          skipWhile(Character::isJavaIdentifierPart);
          final var name = in.substring(start, pos);
          String subscript = null;
          if (in.charAt(pos) == ':' && isDigit(in.charAt(pos + 1))) {
            pos++;
            subscript = readNumber();
          } else if (in.charAt(pos) == '[') {
            int bracestart = pos;
            pos++;
            if (skipSpaces()) { // EOL
              tokens.add(new Token(TOKEN_ERROR_BRACE, start, in.substring(bracestart), 0));
              continue;
            }
            subscript = readNumber();
            if (skipSpaces() || !accept(']')) { // EOL or missing bracket
              tokens.add(new Token(TOKEN_ERROR_BRACE, start, in.substring(bracestart), 0));
              continue;
            }
            pos++;
          }
          if (subscript != null) {
            subscript = subscript.trim();
            if (subscript.isEmpty()) {
              tokens.add(new Token(TOKEN_ERROR_SUBSCRIPT, start, in.substring(start, pos), 0));
              continue;
            }
            try {
              int s = Integer.parseInt(subscript);
              tokens.add(new Token(TOKEN_IDENT, start, name + "[" + s + "]", Integer.MAX_VALUE));
            } catch (NumberFormatException e) {
              // should not happen
              tokens.add(new Token(TOKEN_ERROR_SUBSCRIPT, start, in.substring(start, pos), 0));
            }
          } else {
            tokens.add(new Token(TOKEN_IDENT, start, name, Integer.MAX_VALUE));
          }
        } else {
          tokens.add(readToken(startChar, start));
        }
      }
    }
  }

  private static ArrayList<Token> toTokens(String in, boolean includeWhite) {
    return new Tokenizer(in, includeWhite).tokenize();
  }

  private static final int TOKEN_AND = 0;
  private static final int TOKEN_OR = 1;
  private static final int TOKEN_XOR = 2;
  private static final int TOKEN_EQ = 3;
  private static final int TOKEN_XNOR = 4;
  private static final int TOKEN_NOT = 5;
  private static final int TOKEN_NOT_POSTFIX = 6;
  private static final int TOKEN_LPAREN = 7;
  private static final int TOKEN_RPAREN = 8;
  private static final int TOKEN_IDENT = 9;
  private static final int TOKEN_CONST = 10;
  private static final int TOKEN_WHITE = 11;
  private static final int TOKEN_ERROR_BADCHAR = 12;
  private static final int TOKEN_ERROR_BRACE = 13;
  private static final int TOKEN_ERROR_SUBSCRIPT = 14;
  private static final int TOKEN_ERROR_IDENT = 15;
}
