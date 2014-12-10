/*******************************************************************************
 * This file is part of logisim-evolution.
 *
 *   logisim-evolution is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   logisim-evolution is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with logisim-evolution.  If not, see <http://www.gnu.org/licenses/>.
 *
 *   Original code by Carl Burch (http://www.cburch.com), 2011.
 *   Subsequent modifications by :
 *     + Haute École Spécialisée Bernoise
 *       http://www.bfh.ch
 *     + Haute École du paysage, d'ingénierie et d'architecture de Genève
 *       http://hepia.hesge.ch/
 *     + Haute École d'Ingénierie et de Gestion du Canton de Vaud
 *       http://www.heig-vd.ch/
 *   The project is currently maintained by :
 *     + REDS Institute - HEIG-VD
 *       Yverdon-les-Bains, Switzerland
 *       http://reds.heig-vd.ch
 *******************************************************************************/

package com.cburch.logisim.analyze.model;

import java.util.ArrayList;

import com.cburch.logisim.util.StringGetter;

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

		ParserException error(StringGetter message) {
			return new ParserException(message, offset, length);
		}
	}

	private static boolean okCharacter(char c) {
		return Character.isWhitespace(c) || Character.isJavaIdentifierStart(c)
				|| "()01~^+!&|".indexOf(c) >= 0;
	}

	private static Expression parse(ArrayList<Token> tokens)
			throws ParserException {
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
				while (i + 1 < tokens.size()
						&& tokens.get(i + 1).type == TOKEN_NOT_POSTFIX) {
					here = Expressions.not(here);
					i++;
				}
				while (peekLevel(stack) == Expression.NOT_LEVEL) {
					here = Expressions.not(here);
					pop(stack);
				}
				current = Expressions.and(current, here);
				if (peekLevel(stack) == Expression.AND_LEVEL) {
					Context top = pop(stack);
					current = Expressions.and(top.current, current);
				}
			} else if (t.type == TOKEN_NOT) {
				if (current != null) {
					push(stack,
							current,
							Expression.AND_LEVEL,
							new Token(TOKEN_AND, t.offset, Strings
									.get("implicitAndOperator")));
				}
				push(stack, null, Expression.NOT_LEVEL, t);
				current = null;
			} else if (t.type == TOKEN_NOT_POSTFIX) {
				throw t.error(Strings.getter("unexpectedApostrophe"));
			} else if (t.type == TOKEN_LPAREN) {
				if (current != null) {
					push(stack,
							current,
							Expression.AND_LEVEL,
							new Token(TOKEN_AND, t.offset, 0, Strings
									.get("implicitAndOperator")));
				}
				push(stack, null, -2, t);
				current = null;
			} else if (t.type == TOKEN_RPAREN) {
				current = popTo(stack, -1, current);
				// there had better be a LPAREN atop the stack now.
				if (stack.isEmpty()) {
					throw t.error(Strings.getter("lparenMissingError"));
				}
				pop(stack);
				while (i + 1 < tokens.size()
						&& tokens.get(i + 1).type == TOKEN_NOT_POSTFIX) {
					current = Expressions.not(current);
					i++;
				}
				current = popTo(stack, Expression.AND_LEVEL, current);
			} else {
				if (current == null) {
					throw t.error(Strings.getter("missingLeftOperandError",
							t.text));
				}
				int level = 0;
				switch (t.type) {
				case TOKEN_AND:
					level = Expression.AND_LEVEL;
					break;
				case TOKEN_OR:
					level = Expression.OR_LEVEL;
					break;
				case TOKEN_XOR:
					level = Expression.XOR_LEVEL;
					break;
				}
				push(stack, popTo(stack, level, current), level, t);
				current = null;
			}
		}
		current = popTo(stack, -1, current);
		if (!stack.isEmpty()) {
			Context top = pop(stack);
			throw top.cause.error(Strings.getter("rparenMissingError"));
		}
		return current;
	}

	public static Expression parse(String in, AnalyzerModel model)
			throws ParserException {
		ArrayList<Token> tokens = toTokens(in, false);

		if (tokens.size() == 0)
			return null;

		for (Token token : tokens) {
			if (token.type == TOKEN_ERROR) {
				throw token.error(Strings.getter("invalidCharacterError",
						token.text));
			} else if (token.type == TOKEN_IDENT) {
				int index = model.getInputs().indexOf(token.text);
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
					} else {
						throw token.error(Strings.getter("badVariableName",
								token.text));
					}
				}
			}
		}

		return parse(tokens);
	}

	private static int peekLevel(ArrayList<Context> stack) {
		if (stack.isEmpty())
			return -3;
		Context context = stack.get(stack.size() - 1);
		return context.level;
	}

	private static Context pop(ArrayList<Context> stack) {
		return stack.remove(stack.size() - 1);
	}

	private static Expression popTo(ArrayList<Context> stack, int level,
			Expression current) throws ParserException {
		while (!stack.isEmpty() && peekLevel(stack) >= level) {
			Context top = pop(stack);
			if (current == null)
				throw top.cause.error(Strings.getter(
						"missingRightOperandError", top.cause.text));
			switch (top.level) {
			case Expression.AND_LEVEL:
				current = Expressions.and(top.current, current);
				break;
			case Expression.OR_LEVEL:
				current = Expressions.or(top.current, current);
				break;
			case Expression.XOR_LEVEL:
				current = Expressions.xor(top.current, current);
				break;
			case Expression.NOT_LEVEL:
				current = Expressions.not(current);
				break;
			}
		}
		return current;
	}

	private static void push(ArrayList<Context> stack, Expression expr,
			int level, Token cause) {
		stack.add(new Context(expr, level, cause));
	}

	/**
	 * I wrote this without thinking, and then realized that this is quite
	 * complicated because of removing operators. I haven't bothered to do it
	 * correctly; instead, it just regenerates a string from the raw expression.
	 * static String removeVariable(String in, String variable) { StringBuilder
	 * ret = new StringBuilder(); ArrayList tokens = toTokens(in, true); Token
	 * lastWhite = null; for (int i = 0, n = tokens.size(); i < n; i++) { Token
	 * token = (Token) tokens.get(i); if (token.type == TOKEN_IDENT &&
	 * token.text.equals(variable)) { ; // just ignore it } else if (token.type
	 * == TOKEN_WHITE) { if (lastWhite != null) { if (lastWhite.text.length() >=
	 * token.text.length()) { ; // don't repeat shorter whitespace } else {
	 * ret.replace(ret.length() - lastWhite.text.length(), ret.length(),
	 * token.text); lastWhite = token; } } else { lastWhite = token;
	 * ret.append(token.text); } } else { lastWhite = null;
	 * ret.append(token.text); } } return ret.toString(); }
	 */

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
		int pos = 0;
		while (true) {
			int whiteStart = pos;
			while (pos < in.length() && Character.isWhitespace(in.charAt(pos)))
				pos++;
			if (includeWhite && pos != whiteStart) {
				tokens.add(new Token(TOKEN_WHITE, whiteStart, in.substring(
						whiteStart, pos)));
			}
			if (pos == in.length())
				return tokens;

			int start = pos;
			char startChar = in.charAt(pos);
			pos++;
			if (Character.isJavaIdentifierStart(startChar)) {
				while (Character.isJavaIdentifierPart(in.charAt(pos)))
					pos++;
				tokens.add(new Token(TOKEN_IDENT, start, in.substring(start,
						pos)));
			} else {
				switch (startChar) {
				case '(':
					tokens.add(new Token(TOKEN_LPAREN, start, "("));
					break;
				case ')':
					tokens.add(new Token(TOKEN_RPAREN, start, ")"));
					break;
				case '0':
				case '1':
					tokens.add(new Token(TOKEN_CONST, start, "" + startChar));
					break;
				case '~':
					tokens.add(new Token(TOKEN_NOT, start, "~"));
					break;
				case '\'':
					tokens.add(new Token(TOKEN_NOT_POSTFIX, start, "'"));
					break;
				case '^':
					tokens.add(new Token(TOKEN_XOR, start, "^"));
					break;
				case '+':
					tokens.add(new Token(TOKEN_OR, start, "+"));
					break;
				case '!':
					tokens.add(new Token(TOKEN_NOT, start, "!"));
					break;
				case '&':
					if (in.charAt(pos) == '&')
						pos++;
					tokens.add(new Token(TOKEN_AND, start, in.substring(start,
							pos)));
					break;
				case '|':
					if (in.charAt(pos) == '|')
						pos++;
					tokens.add(new Token(TOKEN_OR, start, in.substring(start,
							pos)));
					break;
				default:
					while (!okCharacter(in.charAt(pos)))
						pos++;
					String errorText = in.substring(start, pos);
					tokens.add(new Token(TOKEN_ERROR, start, errorText));
				}
			}
		}
	}

	//
	// tokenizing code
	//
	private static final int TOKEN_AND = 0;
	private static final int TOKEN_OR = 1;
	private static final int TOKEN_XOR = 2;

	private static final int TOKEN_NOT = 3;

	private static final int TOKEN_NOT_POSTFIX = 4;

	private static final int TOKEN_LPAREN = 5;

	private static final int TOKEN_RPAREN = 6;

	private static final int TOKEN_IDENT = 7;

	private static final int TOKEN_CONST = 8;
	private static final int TOKEN_WHITE = 9;
	private static final int TOKEN_ERROR = 10;

	private Parser() {
	}
}
