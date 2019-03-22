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

package com.cburch.logisim.analyze.data;

import com.cburch.logisim.analyze.model.Expression;
import com.cburch.logisim.analyze.model.ExpressionVisitor;
import com.cburch.logisim.analyze.model.Var;

public class ExpressionLatex {

  private String LatexMathString;

  public ExpressionLatex(Expression expr) {
    if (expr == null) LatexMathString = "";
    else buildLatex(expr, "");
  }

  public ExpressionLatex(Expression expr, Var Function, int bitindex) {
    LatexMathString = "";
    if (expr == null) return;
    String FName;
    if (Function.width == 1) {
      FName = Function.name;
    } else {
      if (bitindex < 0 || bitindex >= Function.width) return;
      FName = Function.name + "_{" + bitindex + "}";
    }
    buildLatex(expr, FName);
  }

  public String get() {
    return LatexMathString;
  }

  private void buildLatex(Expression expr, String name) {
    final StringBuilder text = new StringBuilder();
    text.append('$' + name + (name.length() != 0 ? "=" : ""));
    expr.visit(
        new ExpressionVisitor<Object>() {
          private Object binary(Expression a, Expression b, int level, String op) {
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
            return binary(a, b, Expression.AND_LEVEL, " \\cdot ");
          }

          public Object visitConstant(int value) {
            text.append("" + Integer.toString(value, 16));
            return null;
          }

          public Object visitNot(Expression a) {
            text.append(" \\overline{");
            a.visit(this);
            text.append("} ");
            return null;
          }

          public Object visitOr(Expression a, Expression b) {
            return binary(a, b, Expression.OR_LEVEL, " + ");
          }

          public Object visitVariable(String name) {
            int i = name.indexOf(':');
            if (i >= 0) {
              String sub = name.substring(i + 1);
              name = name.substring(0, i);
              text.append(name + "_{" + sub + "}");
            } else {
              text.append(name);
            }
            return null;
          }

          public Object visitXor(Expression a, Expression b) {
            return binary(a, b, Expression.XOR_LEVEL, " \\oplus ");
          }
        });
    text.append("$\n");
    LatexMathString = text.toString();
  }
}
