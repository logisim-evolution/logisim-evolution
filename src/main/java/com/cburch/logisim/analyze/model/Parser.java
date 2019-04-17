/**
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

package com.cburch.logisim.analyze.model;

import static com.cburch.logisim.analyze.Strings.S;

import com.cburch.logisim.util.StringGetter;
import java.util.ArrayList;

public class Parser {
  //
  // parsing code
  //
  private static class Context {
    int level;
    Expression current;
    Token cause;

    Context(Expression current, int level, Token cause) {
      this.level = level;
      this.current = current;
      this.cause = cause;
    }
  }

  private static class Token {
    int type;
    int offset;
    int length;
    String text;

    Token(int type, int offset, int length, String text) {
      this.type = type;
      this.offset = offset;
      this.length = length;
      this.text = text;
    }

    Token(int type, int offset, String text) {
      this(type, offset, text.length(), text);
    }

    @SuppressWarnings("unused")
    ParserException error(StringGetter message) {
      return new ParserException(message, offset, length);
    }
  }

  private static boolean okCharacter(char c) {
    return Character.isWhitespace(c)
        || Character.isJavaIdentifierStart(c)
        || "()01~-^+*!&|=\\':[]".indexOf(c) >= 0
        || "\u22C0\u22C1\u2227\u2228\u2295\u22C5\u00AC\u2219".indexOf(c) >= 0
        || "\u21D4\u2261\u2194\u02DC\u00B7\u2225\u22BB\u22A4\u22A5".indexOf(c) >= 0;
  }

  private static Expression parse(ArrayList<Token> tokens) throws ParserException {
    ArrayList<Context> stack = new ArrayList<Context>();
    Expression current = null;
    for (int i = 0; i < tokens.size(); i++) {
      Token t = tokens.get(i);
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
        while (peekLevel(stack) == Expression.Op.NOT.Level) {
          here = Expressions.not(here);
          pop(stack);
        }
        current = Expressions.and(current, here);
        if (peekLevel(stack) == Expression.Op.AND.Level) {
          Context top = pop(stack);
          current = Expressions.and(top.current, current);
        }
      } else if (t.type == TOKEN_NOT) {
        if (current != null) {
          push(
              stack,
              current,
              Expression.Op.AND.Level,
              new Token(TOKEN_AND, t.offset, S.get("implicitAndOperator")));
        }
        push(stack, null, Expression.Op.NOT.Level, t);
        current = null;
      } else if (t.type == TOKEN_NOT_POSTFIX) {
        throw t.error(S.getter("unexpectedApostrophe"));
      } else if (t.type == TOKEN_LPAREN) {
        if (current != null) {
          push(stack,current,Expression.Op.AND.Level,new Token(TOKEN_AND, t.offset, 0, S.get("implicitAndOperator")));
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
        current = popTo(stack, Expression.Op.AND.Level, current);
      } else {
        if (current == null) {
          throw t.error(S.getter("missingLeftOperandError", t.text));
        }
        int level = 0;
        switch (t.type) {
          case TOKEN_AND:
            level = Expression.Op.AND.Level;
            break;
          case TOKEN_OR:
            level = Expression.Op.OR.Level;
            break;
          case TOKEN_XOR:
            level = Expression.Op.XOR.Level;
          case TOKEN_EQ:
        	level = Expression.Op.EQ.Level;
            break;
        }
        push(stack, popTo(stack, level, current), level, t);
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
  
  public static Expression parseMaybeAssignment(String in, AnalyzerModel model) throws ParserException {
    return parse(in, model, true);
  }
  
  private static Expression parse(String in, AnalyzerModel model, boolean allowOutputAssignment) throws ParserException {
    ArrayList<Token> tokens = toTokens(in, false);

    if (tokens.size() == 0) return null;

    int i = -1;
    for (Token token : tokens) {
      i++;
      if (token.type == TOKEN_ERROR_BADCHAR) {
        throw token.error(S.getter("invalidCharacterError", token.text));
      } else if (token.type == TOKEN_ERROR_BRACE) {
        throw token.error(S.getter("missingBraceError", token.text));
      } else if (token.type == TOKEN_ERROR_SUBSCRIPT) {
        throw token.error(S.getter("missingSubscriptError", token.text));
      } else if (token.type == TOKEN_ERROR_IDENT) {
       throw token.error(S.getter("missingIdentifierError", token.text));
      } else if (token.type == TOKEN_IDENT) {
        int index = model.getInputs().bits.indexOf(token.text);
        if (index < 0) {
          // ok; but maybe this is an operator
          String opText = token.text.toUpperCase();
          if (opText.equals("NOT")) {
            token.type = TOKEN_NOT;
          } else if (opText.equals("AND")) {
            token.type = TOKEN_AND;
          } else if (opText.equals("XOR")) {
            token.type = TOKEN_XOR;
          } else if (opText.equals("OR")) {
            token.type = TOKEN_OR;
          } else if (opText.contentEquals("EQUALS")) {
            token.type = TOKEN_EQ;
          } else {
            if (i == 0 && allowOutputAssignment) {
              index = model.getOutputs().bits.indexOf(token.text);
              if (index <= 0 && tokens.size() >= 2 && tokens.get(1).type == TOKEN_EQ)
              continue;
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
    Context context = stack.get(stack.size() - 1);
    return context.level;
  }

  private static Context pop(ArrayList<Context> stack) {
    return stack.remove(stack.size() - 1);
  }

  private static Expression popTo(ArrayList<Context> stack, int level, Expression current)
      throws ParserException {
    while (!stack.isEmpty() && peekLevel(stack) >= level) {
      Context top = pop(stack);
      if (current == null)
        throw top.cause.error(S.getter("missingRightOperandError", top.cause.text));
      else if (top.level == Expression.Op.AND.Level)
          current = Expressions.and(top.current, current);
      else if (top.level == Expression.Op.OR.Level)
          current = Expressions.or(top.current, current);
      else if (top.level == Expression.Op.XOR.Level)
          current = Expressions.xor(top.current, current);
      else if (top.level == Expression.Op.EQ.Level)
          current = Expressions.eq(top.current, current);
      else if (top.level == Expression.Op.NOT.Level)
          current = Expressions.not(current);
    }
    return current;
  }

  private static void push(ArrayList<Context> stack, Expression expr, int level, Token cause) {
    stack.add(new Context(expr, level, cause));
  }

  //Note: Doing this without "tokenizing then re-stringify" is tricky.
  static String replaceVariable(String in, String oldName, String newName) {
    StringBuilder ret = new StringBuilder();
    ArrayList<Token> tokens = toTokens(in, true);
    for (Token token : tokens) {
      if (token.type == TOKEN_IDENT && token.text.equals(oldName)) {
        ret.append(newName);
      } else {
        ret.append(token.text);
      }
    }
    return ret.toString();
  }

  private static ArrayList<Token> toTokens(String in, boolean includeWhite) {
    ArrayList<Token> tokens = new ArrayList<Token>();

    // Guarantee that we will stop just after reading whitespace,
    // not in the middle of a token.
    in = in + " ";
    int len = in.length();
    int pos = 0;
  outerloop:
    while (true) {
      int whiteStart = pos;
      while (pos < len && Character.isWhitespace(in.charAt(pos))) pos++;

      if (includeWhite && pos != whiteStart)
        tokens.add(new Token(TOKEN_WHITE, whiteStart, in.substring(whiteStart, pos)));
      if (pos == len)
        return tokens;
      
      int start = pos;
      char startChar = in.charAt(pos);
      pos++;
      if (Character.isJavaIdentifierStart(startChar)) {
        while (Character.isJavaIdentifierPart(in.charAt(pos))) pos++;
        String name = in.substring(start, pos);
        String subscript = null;
        if (in.charAt(pos) == ':') {
          pos++;
          int substart = pos;
          while ("0123456789".indexOf(in.charAt(pos)) >= 0) pos++;
          subscript = in.substring(substart, pos);
        } else if (in.charAt(pos) == '[') {
          int bracestart = pos;
          pos++;
          while (pos < len && Character.isWhitespace(in.charAt(pos)))
            pos++;
          if (pos == len) {
            tokens.add(new Token(TOKEN_ERROR_BRACE, start, in.substring(bracestart)));
            continue outerloop;
          }
          int substart = pos;
          while ("0123456789".indexOf(in.charAt(pos)) >= 0)
            pos++;
          subscript = in.substring(substart, pos);
          while (pos < len && Character.isWhitespace(in.charAt(pos)))
            pos++;
          if (pos == len) {
            tokens.add(new Token(TOKEN_ERROR_BRACE, start, in.substring(bracestart)));
            continue outerloop;
          }
          if (in.charAt(pos) != ']') {
            tokens.add(new Token(TOKEN_ERROR_BRACE, start, in.substring(bracestart)));
            continue outerloop;
          }
          pos++;
        }
        if (subscript != null) {
          subscript = subscript.trim();
          if (subscript.equals("")) {
            tokens.add(new Token(TOKEN_ERROR_SUBSCRIPT, start, in.substring(start, pos)));
            continue outerloop;
          }
          try {
            int s = Integer.parseInt(subscript);
            tokens.add(new Token(TOKEN_IDENT, start, name + "[" + s + "]"));
          } catch (NumberFormatException e) {
            // should not happen
            tokens.add(new Token(TOKEN_ERROR_SUBSCRIPT, start, in.substring(start, pos)));
          }
        } else {
          tokens.add(new Token(TOKEN_IDENT, start, name));
        }
      } else {
        switch (startChar) {
          case '(':
            tokens.add(new Token(TOKEN_LPAREN, start, "("));
            break;
          case ')':
            tokens.add(new Token(TOKEN_RPAREN, start, ")"));
            break;
          case '1':
          case '\u22A4':
            tokens.add(new Token(TOKEN_CONST, start, "1"));
            break;
          case '0':
          case '\u22A5':
            tokens.add(new Token(TOKEN_CONST,start,"0"));
            break;
          case '~':
          case '-':
          case '!':
          case '\u00AC': // logical not
          case '\u02DC': // tilde
            tokens.add(new Token(TOKEN_NOT, start, "~"));
            break;
          case '\'':
            tokens.add(new Token(TOKEN_NOT_POSTFIX, start, "'"));
            break;
          case '^':
          case '\u2295': // xor
          case '\u22BB': // underbar-vee (xor)
            tokens.add(new Token(TOKEN_XOR, start, "^"));
            break;
          case '+':
          case '\u22C1': // large disjunction
          case '\u2228': // small disjunction
          case '\u2225': // logical or
            tokens.add(new Token(TOKEN_OR, start, "+"));
            break;
          case '*':
          case '\u22C0': // large conjunction
          case '\u2227': // small conjunction
          case '\u22C5': // cdot
          case '\u2219': // bullet
          case '\u00B7': // middle-dot
            tokens.add(new Token(TOKEN_AND, start, "*"));
            break;
          case '\u21D4': // left-right-doublearrow
          case '\u2261': // equiv
          case '\u2194': // left-right-arrow
            tokens.add(new Token(TOKEN_EQ, start, "="));
            break;
          case '&':
            if (in.charAt(pos) == '&') pos++;
            tokens.add(new Token(TOKEN_AND, start, in.substring(start, pos)));
            break;
          case '|':
            if (in.charAt(pos) == '|') pos++;
            tokens.add(new Token(TOKEN_OR, start, in.substring(start, pos)));
            break;
          case '=':
              if (in.charAt(pos) == '=')
                pos++;
              tokens.add(new Token(TOKEN_EQ, start, in.substring(start, pos)));
              break;
            case ':':
            case '[':
            case ']':
              tokens.add(new Token(TOKEN_ERROR_IDENT, start, in.substring(start, start+1)));
              break;
          default:
            while (!okCharacter(in.charAt(pos))) pos++;
            String errorText = in.substring(start, pos);
            tokens.add(new Token(TOKEN_ERROR_BADCHAR, start, errorText));
        }
      }
    }
  }

  private static final int TOKEN_AND = 0;
  private static final int TOKEN_OR = 1;
  private static final int TOKEN_XOR = 2;
  private static final int TOKEN_EQ = 3;
  private static final int TOKEN_NOT = 4;
  private static final int TOKEN_NOT_POSTFIX = 5;
  private static final int TOKEN_LPAREN = 6;
  private static final int TOKEN_RPAREN = 7;
  private static final int TOKEN_IDENT = 8;
  private static final int TOKEN_CONST = 9;
  private static final int TOKEN_WHITE = 10;
  private static final int TOKEN_ERROR_BADCHAR = 11;
  private static final int TOKEN_ERROR_BRACE = 12;
  private static final int TOKEN_ERROR_SUBSCRIPT = 13;
  private static final int TOKEN_ERROR_IDENT = 14;

  private Parser() {}
}
