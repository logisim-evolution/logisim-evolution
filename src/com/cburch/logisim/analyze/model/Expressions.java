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

public class Expressions {
	private static class And extends Binary {
		And(Expression a, Expression b) {
			super(a, b);
		}

		@Override
		public int getPrecedence() {
			return Expression.AND_LEVEL;
		}

		@Override
		public <T> T visit(ExpressionVisitor<T> visitor) {
			return visitor.visitAnd(a, b);
		}

		@Override
		int visit(IntVisitor visitor) {
			return visitor.visitAnd(a, b);
		}

		@Override
		void visit(Visitor visitor) {
			visitor.visitAnd(a, b);
		}
	}

	private static abstract class Binary extends Expression {
		protected final Expression a;
		protected final Expression b;

		Binary(Expression a, Expression b) {
			this.a = a;
			this.b = b;
		}

		@Override
		public boolean equals(Object other) {
			if (other == null)
				return false;
			if (this.getClass() != other.getClass())
				return false;
			Binary o = (Binary) other;
			return this.a.equals(o.a) && this.b.equals(o.b);
		}

		@Override
		public int hashCode() {
			return 31 * (31 * getClass().hashCode() + a.hashCode())
					+ b.hashCode();
		}
	}

	private static class Constant extends Expression {
		private int value;

		Constant(int value) {
			this.value = value;
		}

		@Override
		public boolean equals(Object other) {
			if (!(other instanceof Constant))
				return false;
			Constant o = (Constant) other;
			return this.value == o.value;
		}

		@Override
		public int getPrecedence() {
			return Integer.MAX_VALUE;
		}

		@Override
		public int hashCode() {
			return value;
		}

		@Override
		public <T> T visit(ExpressionVisitor<T> visitor) {
			return visitor.visitConstant(value);
		}

		@Override
		int visit(IntVisitor visitor) {
			return visitor.visitConstant(value);
		}

		@Override
		void visit(Visitor visitor) {
			visitor.visitConstant(value);
		}
	}

	private static class Not extends Expression {
		private Expression a;

		Not(Expression a) {
			this.a = a;
		}

		@Override
		public boolean equals(Object other) {
			if (!(other instanceof Not))
				return false;
			Not o = (Not) other;
			return this.a.equals(o.a);
		}

		@Override
		public int getPrecedence() {
			return Expression.NOT_LEVEL;
		}

		@Override
		public int hashCode() {
			return 31 * a.hashCode();
		}

		@Override
		public <T> T visit(ExpressionVisitor<T> visitor) {
			return visitor.visitNot(a);
		}

		@Override
		int visit(IntVisitor visitor) {
			return visitor.visitNot(a);
		}

		@Override
		void visit(Visitor visitor) {
			visitor.visitNot(a);
		}
	}

	private static class Or extends Binary {
		Or(Expression a, Expression b) {
			super(a, b);
		}

		@Override
		public int getPrecedence() {
			return Expression.OR_LEVEL;
		}

		@Override
		public <T> T visit(ExpressionVisitor<T> visitor) {
			return visitor.visitOr(a, b);
		}

		@Override
		int visit(IntVisitor visitor) {
			return visitor.visitOr(a, b);
		}

		@Override
		void visit(Visitor visitor) {
			visitor.visitOr(a, b);
		}
	}

	private static class Variable extends Expression {
		private String name;

		Variable(String name) {
			this.name = name;
		}

		@Override
		public boolean equals(Object other) {
			if (!(other instanceof Variable))
				return false;
			Variable o = (Variable) other;
			return this.name.equals(o.name);
		}

		@Override
		public int getPrecedence() {
			return Integer.MAX_VALUE;
		}

		@Override
		public int hashCode() {
			return name.hashCode();
		}

		@Override
		public <T> T visit(ExpressionVisitor<T> visitor) {
			return visitor.visitVariable(name);
		}

		@Override
		int visit(IntVisitor visitor) {
			return visitor.visitVariable(name);
		}

		@Override
		void visit(Visitor visitor) {
			visitor.visitVariable(name);
		}
	}

	private static class Xor extends Binary {
		Xor(Expression a, Expression b) {
			super(a, b);
		}

		@Override
		public int getPrecedence() {
			return Expression.XOR_LEVEL;
		}

		@Override
		public <T> T visit(ExpressionVisitor<T> visitor) {
			return visitor.visitXor(a, b);
		}

		@Override
		int visit(IntVisitor visitor) {
			return visitor.visitXor(a, b);
		}

		@Override
		void visit(Visitor visitor) {
			visitor.visitXor(a, b);
		}
	}

	public static Expression and(Expression a, Expression b) {
		if (a == null)
			return b;
		if (b == null)
			return a;
		return new And(a, b);
	}

	public static Expression constant(int value) {
		return new Constant(value);
	}

	public static Expression not(Expression a) {
		if (a == null)
			return null;
		return new Not(a);
	}

	public static Expression or(Expression a, Expression b) {
		if (a == null)
			return b;
		if (b == null)
			return a;
		return new Or(a, b);
	}

	public static Expression variable(String name) {
		return new Variable(name);
	}

	public static Expression xor(Expression a, Expression b) {
		if (a == null)
			return b;
		if (b == null)
			return a;
		return new Xor(a, b);
	}

	private Expressions() {
	}
}
