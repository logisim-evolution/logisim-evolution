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

import java.util.HashSet;

public abstract class Expression {
	static interface IntVisitor {
		public int visitAnd(Expression a, Expression b);

		public int visitConstant(int value);

		public int visitNot(Expression a);

		public int visitOr(Expression a, Expression b);

		public int visitVariable(String name);

		public int visitXor(Expression a, Expression b);
	}

	static interface Visitor {
		public void visitAnd(Expression a, Expression b);

		public void visitConstant(int value);

		public void visitNot(Expression a);

		public void visitOr(Expression a, Expression b);

		public void visitVariable(String name);

		public void visitXor(Expression a, Expression b);
	}

	public static final int OR_LEVEL = 0;
	public static final int XOR_LEVEL = 1;

	public static final int AND_LEVEL = 2;

	public static final int NOT_LEVEL = 3;

	public boolean containsXor() {
		return 1 == visit(new IntVisitor() {
			public int visitAnd(Expression a, Expression b) {
				return a.visit(this) == 1 || b.visit(this) == 1 ? 1 : 0;
			}

			public int visitConstant(int value) {
				return 0;
			}

			public int visitNot(Expression a) {
				return a.visit(this);
			}

			public int visitOr(Expression a, Expression b) {
				return a.visit(this) == 1 || b.visit(this) == 1 ? 1 : 0;
			}

			public int visitVariable(String name) {
				return 0;
			}

			public int visitXor(Expression a, Expression b) {
				return 1;
			}
		});
	}

	public boolean evaluate(final Assignments assignments) {
		int ret = visit(new IntVisitor() {
			public int visitAnd(Expression a, Expression b) {
				return a.visit(this) & b.visit(this);
			}

			public int visitConstant(int value) {
				return value;
			}

			public int visitNot(Expression a) {
				return ~a.visit(this);
			}

			public int visitOr(Expression a, Expression b) {
				return a.visit(this) | b.visit(this);
			}

			public int visitVariable(String name) {
				return assignments.get(name) ? 1 : 0;
			}

			public int visitXor(Expression a, Expression b) {
				return a.visit(this) ^ b.visit(this);
			}
		});
		return (ret & 1) != 0;
	}

	public abstract int getPrecedence();

	public boolean isCircular() {
		final HashSet<Expression> visited = new HashSet<Expression>();
		visited.add(this);
		return 1 == visit(new IntVisitor() {
			private int binary(Expression a, Expression b) {
				if (!visited.add(a))
					return 1;
				if (a.visit(this) == 1)
					return 1;
				visited.remove(a);

				if (!visited.add(b))
					return 1;
				if (b.visit(this) == 1)
					return 1;
				visited.remove(b);

				return 0;
			}

			public int visitAnd(Expression a, Expression b) {
				return binary(a, b);
			}

			public int visitConstant(int value) {
				return 0;
			}

			public int visitNot(Expression a) {
				if (!visited.add(a))
					return 1;
				if (a.visit(this) == 1)
					return 1;
				visited.remove(a);
				return 0;
			}

			public int visitOr(Expression a, Expression b) {
				return binary(a, b);
			}

			public int visitVariable(String name) {
				return 0;
			}

			public int visitXor(Expression a, Expression b) {
				return binary(a, b);
			}
		});
	}

	public boolean isCnf() {
		return 1 == visit(new IntVisitor() {
			int level = 0;

			public int visitAnd(Expression a, Expression b) {
				if (level > 1)
					return 0;
				int oldLevel = level;
				level = 1;
				int ret = a.visit(this) == 1 && b.visit(this) == 1 ? 1 : 0;
				level = oldLevel;
				return ret;
			}

			public int visitConstant(int value) {
				return 1;
			}

			public int visitNot(Expression a) {
				if (level == 2)
					return 0;
				int oldLevel = level;
				level = 2;
				int ret = a.visit(this);
				level = oldLevel;
				return ret;
			}

			public int visitOr(Expression a, Expression b) {
				if (level > 0)
					return 0;
				return a.visit(this) == 1 && b.visit(this) == 1 ? 1 : 0;
			}

			public int visitVariable(String name) {
				return 1;
			}

			public int visitXor(Expression a, Expression b) {
				return 0;
			}
		});
	}

	Expression removeVariable(final String input) {
		return visit(new ExpressionVisitor<Expression>() {
			public Expression visitAnd(Expression a, Expression b) {
				Expression l = a.visit(this);
				Expression r = b.visit(this);
				if (l == null)
					return r;
				if (r == null)
					return l;
				return Expressions.and(l, r);
			}

			public Expression visitConstant(int value) {
				return Expressions.constant(value);
			}

			public Expression visitNot(Expression a) {
				Expression l = a.visit(this);
				if (l == null)
					return null;
				return Expressions.not(l);
			}

			public Expression visitOr(Expression a, Expression b) {
				Expression l = a.visit(this);
				Expression r = b.visit(this);
				if (l == null)
					return r;
				if (r == null)
					return l;
				return Expressions.or(l, r);
			}

			public Expression visitVariable(String name) {
				return name.equals(input) ? null : Expressions.variable(name);
			}

			public Expression visitXor(Expression a, Expression b) {
				Expression l = a.visit(this);
				Expression r = b.visit(this);
				if (l == null)
					return r;
				if (r == null)
					return l;
				return Expressions.xor(l, r);
			}
		});
	}

	Expression replaceVariable(final String oldName, final String newName) {
		return visit(new ExpressionVisitor<Expression>() {
			public Expression visitAnd(Expression a, Expression b) {
				Expression l = a.visit(this);
				Expression r = b.visit(this);
				return Expressions.and(l, r);
			}

			public Expression visitConstant(int value) {
				return Expressions.constant(value);
			}

			public Expression visitNot(Expression a) {
				Expression l = a.visit(this);
				return Expressions.not(l);
			}

			public Expression visitOr(Expression a, Expression b) {
				Expression l = a.visit(this);
				Expression r = b.visit(this);
				return Expressions.or(l, r);
			}

			public Expression visitVariable(String name) {
				return Expressions.variable(name.equals(oldName) ? newName
						: name);
			}

			public Expression visitXor(Expression a, Expression b) {
				Expression l = a.visit(this);
				Expression r = b.visit(this);
				return Expressions.xor(l, r);
			}
		});
	}

	@Override
	public String toString() {
		final StringBuilder text = new StringBuilder();
		visit(new Visitor() {
			private void binary(Expression a, Expression b, int level, String op) {
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
			}

			public void visitAnd(Expression a, Expression b) {
				binary(a, b, AND_LEVEL, " ");
			}

			public void visitConstant(int value) {
				text.append("" + Integer.toString(value, 16));
			}

			public void visitNot(Expression a) {
				text.append("~");
				if (a.getPrecedence() < NOT_LEVEL) {
					text.append("(");
					a.visit(this);
					text.append(")");
				} else {
					a.visit(this);
				}
			}

			public void visitOr(Expression a, Expression b) {
				binary(a, b, OR_LEVEL, " + ");
			}

			public void visitVariable(String name) {
				text.append(name);
			}

			public void visitXor(Expression a, Expression b) {
				binary(a, b, XOR_LEVEL, " ^ ");
			}
		});
		return text.toString();
	}

	public abstract <T> T visit(ExpressionVisitor<T> visitor);

	abstract int visit(IntVisitor visitor);

	abstract void visit(Visitor visitor);
}
