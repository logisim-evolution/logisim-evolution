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

import java.util.ArrayList;
import java.util.HashSet;

import com.cburch.logisim.analyze.data.Range;
import com.cburch.logisim.analyze.model.Var.Bit;

public abstract class Expression {
  public static interface Visitor<T> {
    public default T visitVariable(String name) { return null; }
    public default T visitConstant(int value) { return null; }
    public default T visitNot(Expression a) { return null; }
    public default T visitBinary(Expression a, Expression b, Op op) {
      a.visit(this);
      b.visit(this);
      return null;
    }
    public default T visitAnd(Expression a, Expression b) { return visitBinary(a, b, Op.AND); }
    public default T visitOr(Expression a, Expression b) { return visitBinary(a, b, Op.OR); }
    public default T visitXor(Expression a, Expression b) { return visitBinary(a, b, Op.XOR); }
    public default T visitXnor(Expression a, Expression b) { return visitBinary(a, b, Op.XNOR); }
    public default T visitEq(Expression a, Expression b) { return visitBinary(a, b, Op.EQ); }  
  }
  
  protected static interface IntVisitor {
    public int visitVariable(String name);
    public int visitConstant(int value);
    public int visitNot(Expression a);
    public int visitAnd(Expression a, Expression b);
    public int visitOr(Expression a, Expression b);
    public int visitXor(Expression a, Expression b);
    public int visitXnor(Expression a, Expression b);
    public int visitEq(Expression a, Expression b);
  }

  public boolean contains(Op o) {
    return o == visit(new Visitor<Op>() {
      @Override
      public Op visitBinary(Expression a, Expression b, Op op) {
        return (op == o || a.visit(this) == o || b.visit(this) == o) ? o : null;
      }
      @Override
      public Op visitNot(Expression a) { return a.visit(this); }
    });
  }

  public boolean evaluate(final Assignments assignments) {
    int ret =
        visit(
            new IntVisitor() {
              @Override
              public int visitAnd(Expression a, Expression b) {
                return a.visit(this) & b.visit(this);
              }

              @Override
              public int visitConstant(int value) {
                return value;
              }

              @Override
              public int visitNot(Expression a) {
                return ~a.visit(this);
              }

              @Override
              public int visitOr(Expression a, Expression b) {
                return a.visit(this) | b.visit(this);
              }

              @Override
              public int visitVariable(String name) {
                return assignments.get(name) ? 1 : 0;
              }

              @Override
              public int visitXor(Expression a, Expression b) {
                return a.visit(this) ^ b.visit(this);
              }
              
              @Override
              public int visitXnor(Expression a, Expression b) {
            	  return ~(a.visit(this) ^ b.visit(this));
              }
              
              @Override
              public int visitEq(Expression a, Expression b) {
                return ~(a.visit(this) ^ b.visit(this)&1);
              }
            });
    return (ret & 1) != 0;
  }
  
  public static enum Notation { MATHEMATICAL(0), LOGIC(1), ALTLOGIC(2), PROGBOOLS(3), PROGBITS(4), LaTeX(5);

  public final int Id;

  public final int[] opLvl;
  public final String[] opSym;

  // Notes on precedence:
  // all forms of NOT are the highest precedence level
  public static final int NOT_PRECEDENCE = 14;
  // times and implicit and are next
  public static final int IMPLICIT_AND_PRECEDENCE = 13;
  public static final int TIMES_PRECEDENCE = 13;
  // oplus is next
  public static final int OPLUS_PRECEDENCE = 12;
  // plus is next
  public static final int PLUS_PRECEDENCE = 11;
  // otimes is next
  public static final int OTIMES_PRECEDENCE = 10;
  // not-equals, not-equiv, equiv, vee, vee-underbar, and cap are next
  public static final int LOGIC_PRECEDENCE = 9;
  // & is next
  public static final int BITAND_PRECEDENCE = 8;
  // ^ is next
  public static final int BITXOR_PRECEDENCE = 7;
  // | is next
  public static final int BITOR_PRECEDENCE = 6;
  // && is next
  public static final int AND_PRECEDENCE = 5;
  // || is next
  public static final int OR_PRECEDENCE = 4;
  // "and" is next
  public static final int PYTHON_AND_PRECEDENCE = 3;
  // "xor" is next
  public static final int PYTHON_XOR_PRECEDENCE = 2;
  // "or" is next
  public static final int PYTHON_OR_PRECEDENCE = 1;
  // all forms of equals are level 0
  public static final int EQ_PRECEDENCE = 0;

  private Notation(int id) { 
    Id = id;
    // Precendence level and symbol for each of { EQ, XNOR, OR, XOR, AND, NOT }
    switch (id) {
      case 1 : // Logic notation: equiv, vee, vee-underbar, cap, tilde
          opLvl = new int[] { 0, 9, 9, 9, 9, 14, };
          opSym = new String[] { " = ", "\u2261", "\u2228", "\u22BB", "\u2227", "\u00AC", };
          break;
      case 2 :  // Alternative Logic notation: equiv, vee, not-equiv, cap, ell
          opLvl = new int[] { 0, 9, 9, 9, 9, 14, };
          opSym = new String[] { " = ", "\u2261", "\u2228", "\u2262", "\u2227", "~", };
          break;
      case 3 : // Programming with booleans notation: ==, ||, !=, &&, !
          opLvl = new int[] { 0, 9, 4, 9, 5, 14, };
          opSym = new String[] { " = ", "==", "||", "!=", "&&", "!", };
          break;
      case 4 : // Programming with bits notation: ^ ~, |, ^, &, ~
          opLvl = new int[] { 0, 9, 6, 7, 8, 14, };
          opSym = new String[] { " = ", "^~", "|", "^", "&", "~", };
          break;
      case 5 : // LaTeX
    	  opLvl = new int[] { 0, 10, 11, 12, 13, 14, };
    	  opSym = new String[] {" = " , " \\oplus " , "+" , " \\oplus " , " \\cdot " , " \\overline{", };
    	  break;
      default :  // Mathematical notation: otimes, plus, oplus, times, and overbar
    	  opLvl = new int[] { 0, 10, 11, 12, 13, 14, };
    	  opSym = new String[] {" = " , "\u2299" , "+" , "\u2295" , "\u22C5" , "~", };
    	  break;
    }
  }

    public String toString() {
      String key = name().toLowerCase() + "Notation";
      return S.get(key);
    }
  }

  public static enum Op {
    EQ(0,2), XNOR(1,2) , OR(2,2), XOR(3,2), AND(4,2), NOT(5,1);

    public final int Id, Arity;

    private Op(int id, int arity) {
      Id = id;
      Arity = arity;
    }
  }

//  // Notation choices:
//  public static final String[][] OPSYM = {
//    {"=" , "+" , "\u2295" , "\u22C5" , "~", }, // Mathematic
//    {"=", "\u2228", "\u2295", "\u2227", "\u00AC", }, // Logic
//    {"==", "||", "^", "&&", "!",}, // programming
//    {" = ", " + ", " \\oplus ", " \\cdot ", " \\overline{",}, // LaTeX
//  };


  public final ArrayList<Range> nots = new ArrayList<Range>();
  public final ArrayList<Range> subscripts = new ArrayList<Range>();
  public final ArrayList<Range> marks = new ArrayList<Range>();
  private Integer[] badness;

  public abstract int getPrecedence(Notation notation);
  public abstract Op getOp();

  public boolean isCircular() {
    final HashSet<Expression> visited = new HashSet<Expression>();
    visited.add(this);
    Object loop = new Object();
    return loop == visit(new Visitor<Object>() {
      @Override
      public Object visitBinary(Expression a, Expression b, Op op) {
        if (!visited.add(a)) 
          return loop;
        if (a.visit(this) == loop)
          return loop;
        visited.remove(a);

        if (!visited.add(b))
          return loop;
        if (b.visit(this) == loop)
          return loop;
        visited.remove(b);
        return null;
      }

      @Override
      public Object visitNot(Expression a) {
        if (!visited.add(a))
          return loop;
        if (a.visit(this) == loop)
          return loop;
        visited.remove(a);
        return null;
      }
    });
  }

  public boolean isCnf() {
    Object cnf = new Object();
    return cnf == visit(new Visitor<Object>() {
      int level = 0;

      @Override
      public Object visitAnd(Expression a, Expression b) {
         if (level > 1) return null;
         int oldLevel = level;
         level = 1;
         Object ret = a.visit(this) == cnf && b.visit(this) == cnf ? cnf : null;
         level = oldLevel;
         return ret;
      }

      @Override
      public Object visitConstant(int value) {
        return cnf;
      }

      @Override
      public Object visitNot(Expression a) {
        if (level == 2) return null;
        int oldLevel = level;
        level = 2;
        Object ret = a.visit(this);
        level = oldLevel;
        return ret;
      }

      @Override
      public Object visitOr(Expression a, Expression b) {
        if (level > 0) return null;
        return a.visit(this) == cnf && b.visit(this) == cnf ? cnf : null;
      }

      @Override
      public Object visitVariable(String name) {
         return cnf;
      }

      @Override
      public Object visitXor(Expression a, Expression b) {
        return null;
      }
              
      @Override
      public Object visitXnor(Expression a, Expression b) {
        return null;
      }
              
      @Override
      public Object visitEq(Expression a, Expression b) {
        return null;
      }
    });
  }

  Expression removeVariable(final String input) {
    return visit(new Visitor<Expression>() {
      @Override
      public Expression visitAnd(Expression a, Expression b) {
        Expression l = a.visit(this);
        Expression r = b.visit(this);
        if (l == null) return r;
        if (r == null) return l;
        return Expressions.and(l, r);
      }

      @Override
      public Expression visitConstant(int value) {
        return Expressions.constant(value);
      }

      @Override
      public Expression visitNot(Expression a) {
        Expression l = a.visit(this);
        if (l == null) return null;
        return Expressions.not(l);
      }

      @Override
      public Expression visitOr(Expression a, Expression b) {
        Expression l = a.visit(this);
        Expression r = b.visit(this);
        if (l == null) return r;
        if (r == null) return l;
        return Expressions.or(l, r);
      }

      @Override
      public Expression visitVariable(String name) {
        return name.equals(input) ? null : Expressions.variable(name);
      }

      @Override
      public Expression visitXor(Expression a, Expression b) {
        Expression l = a.visit(this);
        Expression r = b.visit(this);
        if (l == null) return r;
        if (r == null) return l;
        return Expressions.xor(l, r);
      }
      
      @Override
      public Expression visitXnor(Expression a, Expression b) {
        Expression l = a.visit(this);
        Expression r = b.visit(this);
        if (l == null)
          return r;
        if (r == null)
          return l;
        return Expressions.xnor(l, r);
      }
      
      @Override
      public Expression visitEq(Expression a, Expression b) {
        Expression l = a.visit(this);
        Expression r = b.visit(this);
        if (l == null)
           return r;
        if (r == null)
          return l;
        return Expressions.eq(l, r);
      }
    });
  }

  Expression replaceVariable(final String oldName, final String newName) {
    return visit(new Visitor<Expression>() {
      @Override
      public Expression visitAnd(Expression a, Expression b) {
        Expression l = a.visit(this);
        Expression r = b.visit(this);
        return Expressions.and(l, r);
      }

      @Override
      public Expression visitConstant(int value) {
        return Expressions.constant(value);
      }

      @Override
      public Expression visitNot(Expression a) {
        Expression l = a.visit(this);
        return Expressions.not(l);
      }

      @Override
      public Expression visitOr(Expression a, Expression b) {
        Expression l = a.visit(this);
        Expression r = b.visit(this);
        return Expressions.or(l, r);
      }

      @Override
      public Expression visitVariable(String name) {
        return Expressions.variable(name.equals(oldName) ? newName : name);
      }

      @Override
      public Expression visitXor(Expression a, Expression b) {
        Expression l = a.visit(this);
        Expression r = b.visit(this);
        return Expressions.xor(l, r);
      }
      
      @Override
      public Expression visitXnor(Expression a, Expression b) {
        Expression l = a.visit(this);
        Expression r = b.visit(this);
        return Expressions.xnor(l, r);
      }

      @Override
      public Expression visitEq(Expression a, Expression b) {
        Expression l = a.visit(this);
        Expression r = b.visit(this);
        return Expressions.eq(l, r);
      }
    });
  }

  @Override
  public String toString() {
    return toString(Notation.MATHEMATICAL);
  }
  
  public String toString(Notation notation) {
    return toString(notation,false);
  }

  public String toString(Notation notation, boolean reduce) {
    return toString(notation , reduce , null);
  }
  
  
  private static final int BADNESS_NOT_BREAK = 15;
  private static final int BADNESS_PARENTESIS_BREAK = 10;
  private static final int BADNESS_CONST_BREAK = 100;
  private static final int BADNESS_VAR_BREAK = 200;
  private static final int BADNESS_AND_BREAK = 5;
  
  public String toString(Notation notation, boolean reduce, Expression other) {
    final StringBuilder text = new StringBuilder();
    final ArrayList<Integer> badnessList = new ArrayList<Integer>();
    if (reduce) {
      nots.clear();
      subscripts.clear();
      marks.clear();
    }
    visit(new Visitor<Void>() {
      int curBadness = 0;
      boolean AndOp = false;
      boolean inXnor = false;
      
      private void add(String txt) {
    	  text.append(txt);
    	  for (int i = 0 ; i < txt.length() ; i++)
    		  badnessList.add(curBadness);
      }
      
      @Override
      public Void visitBinary(Expression a, Expression b, Op op) {
        Range mark = null;
        if (a.equals(other)) {
          mark = new Range();
          mark.startIndex = text.length();
          marks.add(mark);
        }
        int opLvl = notation.opLvl[op.Id];
        int aLvl = a.getPrecedence(notation);
        int bLvl = b.getPrecedence(notation);
        if (aLvl < opLvl || (aLvl == opLvl && a.getOp() != op)) {
          curBadness += BADNESS_PARENTESIS_BREAK;
          add("(");
          a.visit(this);
          add(")");
          curBadness -= BADNESS_PARENTESIS_BREAK;
        } else {
          a.visit(this);
        }
        if (mark != null) {
          mark.stopIndex = text.length();
          mark = null;
        }
        add(notation.opSym[op.Id]);
        if (b.equals(other)) {
          mark = new Range();
          mark.startIndex = text.length();
          marks.add(mark);
        }
        if (bLvl < opLvl || (bLvl == opLvl && b.getOp() != op)) {
          curBadness += BADNESS_PARENTESIS_BREAK;
          add("(");
          b.visit(this);
          add(")");
          curBadness -= BADNESS_PARENTESIS_BREAK;
        } else {
          b.visit(this);
        }
        if (mark != null) {
          mark.stopIndex = text.length();
        }
        return null;
      }
      
      @Override
      public Void visitConstant(int value) {
        curBadness += BADNESS_CONST_BREAK;
        add(Integer.toString(value, 16));
        curBadness -= BADNESS_CONST_BREAK;
        return null;
      }
      
      @Override
      public Void visitNot(Expression a) {
        curBadness += BADNESS_NOT_BREAK;
        int opLvl = notation.opLvl[Op.NOT.Id];
        int aLvl = a.getPrecedence(notation);
        if (reduce && notation.equals(Notation.MATHEMATICAL)) {
          Range notData = new Range();
          notData.startIndex = text.length();
          nots.add(notData);
          a.visit(this);
          notData.stopIndex = text.length();
        } else {
          add(notation.opSym[Op.NOT.Id]);
    	  if (notation.equals(Notation.LaTeX)) {
    	    a.visit(this);
    	    add("} ");
    	  } else if (aLvl < opLvl || (aLvl == opLvl && a.getOp() != Op.NOT)) {
            curBadness += BADNESS_PARENTESIS_BREAK;
            add("(");
            a.visit(this);
            add(")");
            curBadness -= BADNESS_PARENTESIS_BREAK;
          } else {
            a.visit(this);
          }
        }
        curBadness -= BADNESS_NOT_BREAK;
    	return null;
      }
      
      @Override
      public Void visitXnor(Expression a, Expression b) {
        if (inXnor || !notation.equals(Notation.LaTeX))
          visitBinary(a, b, notation.equals(Notation.LaTeX) ? Op.XOR : Op.XNOR);
        else {
          inXnor = true;
          text.append(" \\overline{");
          visitBinary(a, b, Op.XOR);
          text.append("}");
          inXnor = false;
        }
        return null;
      }

      @Override
      public Void visitVariable(String name) {
        String baseName = name;
        String index = null;
        try {
            Bit b = Bit.parse(name);
        	baseName = b.name;
        	if (b.b >= 0)
        	  index = Integer.toString(b.b);
          } catch (ParserException except) {
            /* TODO: catch exception */
          }
        curBadness += BADNESS_VAR_BREAK;
        if (reduce && index != null) {
          add(baseName);
          Range subscript = new Range();
          subscript.startIndex = text.length();
          add(index);
          subscript.stopIndex = text.length();
          subscripts.add(subscript);
        } else if (notation.equals(Notation.LaTeX)) {
          add(baseName);
          if (index != null)
        	  add("_{"+index+"}");	
        } else
    	  add(name);
        curBadness -= BADNESS_VAR_BREAK;
        return null;
      }
      
      @Override
      public Void visitAnd(Expression a, Expression b) {
        if (AndOp)
          visitBinary(a, b, Op.AND);
        else {
          AndOp = true;
          curBadness += BADNESS_AND_BREAK;
          visitBinary(a, b, Op.AND);
    	  curBadness -= BADNESS_AND_BREAK;
    	  AndOp = false;
        }
    	return null;
      }
    });
    badness = badnessList.toArray(new Integer[badnessList.size()]);
    String result = notation.equals(Notation.LaTeX) ?"$"+text.toString()+"$" : text.toString();
    return result;
  }
  
  public Integer[] getBadness() {
    return badness;  
  }
 
  public static boolean isAssignment(Expression expr) {
    if (expr == null || !(expr instanceof Expressions.Eq))
      return false;
    Expressions.Eq eq = (Expressions.Eq)expr;
    return (eq.a != null && (eq.a instanceof Expressions.Variable));
  }

  public static String getAssignmentVariable(Expression expr) {
    if (expr == null || !(expr instanceof Expressions.Eq))
      return null;
    Expressions.Eq eq = (Expressions.Eq)expr;
    return (eq.a != null && (eq.a instanceof Expressions.Variable)) ? eq.a.toString() : null;
  }

  public static Expression getAssignmentExpression(Expression expr) {
    if (expr == null || !(expr instanceof Expressions.Eq))
      return null;
    Expressions.Eq eq = (Expressions.Eq)expr;
    return (eq.a != null && (eq.a instanceof Expressions.Variable)) ? eq.b : null;
  }

  public abstract <T> T visit(Visitor<T> visitor);

  abstract int visit(IntVisitor visitor);

}
