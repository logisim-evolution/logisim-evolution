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

package com.cburch.logisim.analyze.model;

public class Expressions {
  private Expressions() {
    // dummy, private
  }

  private static class And extends Binary {
    And(Expression a, Expression b) {
      super(a, b);
    }

    @Override
    public int getPrecedence(Notation notation) {
      return notation.opLvl[Op.AND.id];
    }

    @Override
    public Op getOp() {
      return Expression.Op.AND;
    }

    @Override
    int visit(IntVisitor visitor) {
      return visitor.visitAnd(exprA, exprB);
    }

    @Override
    public <T> T visit(Visitor<T> visitor) {
      return visitor.visitAnd(exprA, exprB);
    }
  }

  private abstract static class Binary extends Expression {
    protected final Expression exprA;
    protected final Expression exprB;

    Binary(Expression a, Expression b) {
      this.exprA = a;
      this.exprB = b;
    }

    @Override
    public boolean equals(Object other) {
      if (other == null) return false;
      if (this.getClass() != other.getClass()) return false;
      final var o = (Binary) other;
      return this.exprA.equals(o.exprA) && this.exprB.equals(o.exprB);
    }

    @Override
    public int hashCode() {
      return 31 * (31 * getClass().hashCode() + exprA.hashCode()) + exprB.hashCode();
    }
  }

  private static class Constant extends Expression {
    private final int value;

    Constant(int value) {
      this.value = value;
    }

    @Override
    public boolean equals(Object other) {
      if (!(other instanceof Constant)) return false;
      final var o = (Constant) other;
      return this.value == o.value;
    }

    @Override
    public int getPrecedence(Notation notation) {
      return Integer.MAX_VALUE;
    }

    @Override
    public Op getOp() {
      return null;
    }

    @Override
    public int hashCode() {
      return value;
    }

    @Override
    public <T> T visit(Visitor<T> visitor) {
      return visitor.visitConstant(value);
    }

    @Override
    int visit(IntVisitor visitor) {
      return visitor.visitConstant(value);
    }
  }

  private static class Not extends Expression {
    private final Expression expr;

    Not(Expression a) {
      this.expr = a;
    }

    @Override
    public boolean equals(Object other) {
      if (!(other instanceof Not)) return false;
      final var o = (Not) other;
      return this.expr.equals(o.expr);
    }

    @Override
    public int getPrecedence(Notation notation) {
      return notation.opLvl[Op.NOT.id];
    }

    @Override
    public Op getOp() {
      return Expression.Op.NOT;
    }

    @Override
    public int hashCode() {
      return 31 * expr.hashCode();
    }

    @Override
    public <T> T visit(Visitor<T> visitor) {
      return visitor.visitNot(expr);
    }

    @Override
    int visit(IntVisitor visitor) {
      return visitor.visitNot(expr);
    }
  }

  private static class Or extends Binary {
    Or(Expression a, Expression b) {
      super(a, b);
    }

    @Override
    public int getPrecedence(Notation notation) {
      return notation.opLvl[Op.OR.id];
    }

    @Override
    public Op getOp() {
      return Expression.Op.OR;
    }

    @Override
    public <T> T visit(Visitor<T> visitor) {
      return visitor.visitOr(exprA, exprB);
    }

    @Override
    int visit(IntVisitor visitor) {
      return visitor.visitOr(exprA, exprB);
    }
  }

  protected static class Variable extends Expression {
    private final String name;

    Variable(String name) {
      this.name = name;
    }

    @Override
    public boolean equals(Object other) {
      if (!(other instanceof Variable)) return false;
      final var o = (Variable) other;
      return this.name.equals(o.name);
    }

    @Override
    public int getPrecedence(Notation notation) {
      return Integer.MAX_VALUE;
    }

    @Override
    public int hashCode() {
      return name.hashCode();
    }

    @Override
    public Op getOp() {
      return null;
    }

    @Override
    public <T> T visit(Visitor<T> visitor) {
      return visitor.visitVariable(name);
    }

    @Override
    int visit(IntVisitor visitor) {
      return visitor.visitVariable(name);
    }
  }

  private static class Xor extends Binary {
    Xor(Expression a, Expression b) {
      super(a, b);
    }

    @Override
    public int getPrecedence(Notation notation) {
      return notation.opLvl[Op.XOR.id];
    }

    @Override
    public Op getOp() {
      return Expression.Op.XOR;
    }

    @Override
    public <T> T visit(Visitor<T> visitor) {
      return visitor.visitXor(exprA, exprB);
    }

    @Override
    int visit(IntVisitor visitor) {
      return visitor.visitXor(exprA, exprB);
    }
  }

  private static class Xnor extends Binary {
    Xnor(Expression a, Expression b) {
      super(a, b);
    }

    @Override
    public int getPrecedence(Notation notation) {
      return notation.opLvl[Op.XNOR.id];
    }

    @Override
    public Op getOp() {
      return Expression.Op.XNOR;
    }

    @Override
    public <T> T visit(Visitor<T> visitor) {
      return visitor.visitXnor(exprA, exprB);
    }

    @Override
    int visit(IntVisitor visitor) {
      return visitor.visitXnor(exprA, exprB);
    }
  }

  protected static class Eq extends Binary {
    Eq(Expression a, Expression b) {
      super(a, b);
    }

    @Override
    public int getPrecedence(Notation notation) {
      return notation.opLvl[Op.EQ.id];
    }

    @Override
    public Op getOp() {
      return Expression.Op.EQ;
    }

    @Override
    public <T> T visit(Visitor<T> visitor) {
      return visitor.visitEq(exprA, exprB);
    }

    @Override
    int visit(IntVisitor visitor) {
      return visitor.visitEq(exprA, exprB);
    }
  }

  public static Expression and(Expression a, Expression b) {
    if (a == null) return b;
    if (b == null) return a;
    return new And(a, b);
  }

  public static Expression constant(int value) {
    return new Constant(value);
  }

  public static Expression not(Expression a) {
    if (a == null) return null;
    return new Not(a);
  }

  public static Expression or(Expression a, Expression b) {
    if (a == null) return b;
    if (b == null) return a;
    return new Or(a, b);
  }

  public static Expression xor(Expression a, Expression b) {
    if (a == null) return b;
    if (b == null) return a;
    return new Xor(a, b);
  }

  public static Expression xnor(Expression a, Expression b) {
    if (a == null) return b;
    if (b == null) return a;
    return new Xnor(a, b);
  }

  public static Expression eq(Expression a, Expression b) {
    if (a == null) return b;
    if (b == null) return a;
    return new Eq(a, b);
  }

  public static Expression variable(String name) {
    return new Variable(name);
  }

}
