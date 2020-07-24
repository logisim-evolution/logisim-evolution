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

import java.text.ParseException;
import java.util.Iterator;

public class Var implements Iterable<String> {

  public final int width;
  public final String name;

  public Var(String n, int w) {
    name = n;
    width = w;
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof Var)) return false;
    Var other = (Var) o;
    return (other.name.equals(this.name) && other.width == this.width);
  }

  @Override
  public int hashCode() {
    return name.hashCode() + width;
  }

  @Override
  public String toString() {
    if (width > 1) return name + "[" + (width - 1) + "..0]";
    else return name;
  }

  public static Var parse(String s) throws ParserException {
    s = s.trim();
    int i = s.indexOf('[');
    int j = s.lastIndexOf(']');
    int w = 1;
    if (0 < i && i < j && j == s.length()-1) {
      String braces = s.substring(i+1, j);
      if (!braces.endsWith("..0"))
        throw new ParserException(S.getter("variableFormat"), i);
      try {
        w = 1+Integer.parseInt(braces.substring(0, braces.length()-3));
      } catch (NumberFormatException e) {
        throw new ParserException(S.getter("variableFormat"), i);
      }
      if (w < 1)
        throw new ParserException(S.getter("variableFormat"), i);
      else if (w > 32)
        throw new ParserException(S.getter("variableTooMuchBits"), i);
      s = s.substring(0, i).trim();
    } else if (i >= 0 || j >= 0) {
      throw new ParserException(S.getter("variableFormat"), i >= 0 ? i : j);
    } else {
      s = s.trim();
    }
    return new Var(s, w);
  }
  
  public static class Bit {
    public String name;
    public int b; // -1 means no index
    public Bit(String name, int b) {
      this.name = name;
      this.b = b;
    }
    public String toString() {
      if (b == -1)
        return name;
      else
        return name + "[" + b + "]";
    }
    public static Bit parse(String s) throws ParserException {
      s = s.trim();
      int i = s.indexOf(':');
      if (i > 0) {
        try {
          String name = s.substring(0, i);
          int sub = Integer.parseInt(s.substring(i+1));
          return new Bit(name, sub);
        } catch (NumberFormatException e) {
          throw new ParserException(S.getter("badVariableIndexError"), i);
        }
      } else if (i == 0) {
        throw new ParserException(S.getter("badVariableColonError"), i);
      }
      i = s.indexOf('[');
      int j = s.lastIndexOf(']');
      if (0 < i && i < j && j == s.length()-1) {
        try {
          String name = s.substring(0, i).trim();
          int sub = Integer.parseInt(s.substring(i+1, j));
          return new Bit(name, sub);
        } catch (NumberFormatException e) {
          throw new ParserException(S.getter("badVariableIndexError"), i);
        }
      } else if (i >= 0 || j >= 0) {
        throw new ParserException(S.getter("badVariableBitFormError"), i >= 0 ? i : j);
      }
      return new Bit(s, -1);
    }
  }


  public String bitName(int b) {
    if (b >= width) {
      throw new IllegalArgumentException("Can't access bit " + b + " of " + width);
    }
    if (width > 1) return name + "[" + b + "]";
    else return name;
  }

  public Iterator<String> iterator() {
    return new Iterator<String>() {
      int b = width - 1;

      @Override
      public boolean hasNext() {
        return (b >= 0);
      }

      @Override
      public String next() {
        return bitName(b--);
      }

      @Override
      public void remove() {
        throw new UnsupportedOperationException();
      }
    };
  }
}
