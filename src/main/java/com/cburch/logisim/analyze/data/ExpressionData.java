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
 *******************************************************************************/

package com.cburch.logisim.analyze.data;

import java.util.ArrayList;

import com.cburch.logisim.analyze.model.Expression;
import com.cburch.logisim.analyze.model.ExpressionVisitor;

public class ExpressionData {

	private static final int BADNESS_IDENT_BREAK = 10000;
	private static final int BADNESS_BEFORE_SPACE = 500;
	private static final int BADNESS_BEFORE_AND = 50;
	private static final int BADNESS_BEFORE_XOR = 30;

	private static final int BADNESS_BEFORE_OR = 0;
	private static final int BADNESS_NOT_BREAK = 100;
	private static final int BADNESS_PER_NOT_BREAK = 30;

	String text;
	public final ArrayList<Range> nots = new ArrayList<Range>();
	public final ArrayList<Range> subscripts = new ArrayList<Range>();
	int[] badness;

	public ExpressionData(Expression expr) {
		if (expr == null) {
			text = "";
			badness = new int[0];
		} else {
			computeText(expr);
			computeBadnesses();
		}
	}
	
	public String getText() {
		return text;
	}
	
	public int[] getBadness() {
		return badness;
	}
	
	private void computeBadnesses() {
		badness = new int[text.length() + 1];
		badness[text.length()] = 0;
		if (text.length() == 0)
			return;

		badness[0] = Integer.MAX_VALUE;
		Range curNot = nots.isEmpty() ? null : (Range) nots.get(0);
		int curNotIndex = 0;
		char prev = text.charAt(0);
		for (int i = 1; i < text.length(); i++) {
			// invariant: curNot.stopIndex >= i (and is first such),
			// or curNot == null if none such exists
			char cur = text.charAt(i);
			if (cur == ' ') {
				badness[i] = BADNESS_BEFORE_SPACE;
				;
			} else if (Character.isJavaIdentifierPart(cur)) {
				if (Character.isJavaIdentifierPart(prev)) {
					badness[i] = BADNESS_IDENT_BREAK;
				} else {
					badness[i] = BADNESS_BEFORE_AND;
				}
			} else if (cur == '+') {
				badness[i] = BADNESS_BEFORE_OR;
			} else if (cur == '^') {
				badness[i] = BADNESS_BEFORE_XOR;
			} else if (cur == ')') {
				badness[i] = BADNESS_BEFORE_SPACE;
			} else { // cur == '('
				badness[i] = BADNESS_BEFORE_AND;
			}

			while (curNot != null && curNot.stopIndex <= i) {
				++curNotIndex;
				curNot = (curNotIndex >= nots.size() ? null
						: (Range) nots.get(curNotIndex));
			}

			if (curNot != null && badness[i] < BADNESS_IDENT_BREAK) {
				int depth = 0;
				Range nd = curNot;
				int ndi = curNotIndex;
				while (nd != null && nd.startIndex < i) {
					if (nd.stopIndex > i)
						++depth;
					++ndi;
					nd = ndi < nots.size() ? (Range) nots.get(ndi) : null;
				}
				if (depth > 0) {
					badness[i] += BADNESS_NOT_BREAK + (depth - 1)
							* BADNESS_PER_NOT_BREAK;
				}
			}

			prev = cur;
		}
	}

	private void computeText(Expression expr) {
		final StringBuilder text = new StringBuilder();
		expr.visit(new ExpressionVisitor<Object>() {
			private Object binary(Expression a, Expression b, int level,
					String op) {
				if (a.getPrecedence() < level) {
					text.append("(");
					a.visit(this);
					text.append(")");
				} else {
					a.visit(this);
				}
				text.append(op);
				if (b.getPrecedence() < level) {
					text.append("(");
					b.visit(this);
					text.append(")");
				} else {
					b.visit(this);
				}
				return null;
			}

			public Object visitAnd(Expression a, Expression b) {
				return binary(a, b, Expression.AND_LEVEL, " ");
			}

			public Object visitConstant(int value) {
				text.append("" + Integer.toString(value, 16));
				return null;
			}

			public Object visitNot(Expression a) {
				Range notData = new Range();
				notData.startIndex = text.length();
				nots.add(notData);
				a.visit(this);
				notData.stopIndex = text.length();
				return null;
			}

			public Object visitOr(Expression a, Expression b) {
				return binary(a, b, Expression.OR_LEVEL, " + ");
			}

			public Object visitVariable(String name) {
				 int i = name.indexOf(':');
                 if (i >= 0) {
                         String sub = name.substring(i+1);
                         name = name.substring(0, i);
                         text.append(name);
                         Range subscript = new Range();
                         subscript.startIndex = text.length();
                         text.append(sub);
                         subscript.stopIndex = text.length();
                         subscripts.add(subscript);
                 } else {
                         text.append(name);
                 }
				return null;
			}

			public Object visitXor(Expression a, Expression b) {
				return binary(a, b, Expression.XOR_LEVEL, " ^ ");
			}
		});
		this.text = text.toString();
	}
}
