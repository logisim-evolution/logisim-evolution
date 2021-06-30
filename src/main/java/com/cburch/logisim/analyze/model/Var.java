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

import static com.cburch.logisim.analyze.Strings.S;

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
    final var other = (Var) o;
    return (other.name.equals(this.name) && other.width == this.width);
  }

  @Override
  public int hashCode() {
    return name.hashCode() + width;
  }

  @Override
  public String toString() {
    return (width > 1) ? name + "[" + (width - 1) + "..0]" : name;
  }

  public static Var parse(String s) throws ParserException {
    s = s.trim();
    final var i = s.indexOf('[');
    final var j = s.lastIndexOf(']');
    var w = 1;
    if (0 < i && i < j && j == s.length() - 1) {
      final var braces = s.substring(i + 1, j);
      if (!braces.endsWith("..0")) throw new ParserException(S.getter("variableFormat"), i);
      try {
        w = 1 + Integer.parseInt(braces.substring(0, braces.length() - 3));
      } catch (NumberFormatException e) {
        throw new ParserException(S.getter("variableFormat"), i);
      }
      if (w < 1) throw new ParserException(S.getter("variableFormat"), i);
      else if (w > 32) throw new ParserException(S.getter("variableTooMuchBits"), i);
      s = s.substring(0, i).trim();
    } else if (i >= 0 || j >= 0) {
      throw new ParserException(S.getter("variableFormat"), i >= 0 ? i : j);
    } else {
      s = s.trim();
    }
    return new Var(s, w);
  }

  public static class Bit {
    public final String name;
    public final int bitIndex; // -1 means no index

    public Bit(String name, int b) {
      this.name = name;
      this.bitIndex = b;
    }

    public String toString() {
      return (bitIndex == -1) ? name : name + "[" + bitIndex + "]";
    }

    public static Bit parse(String s) throws ParserException {
      s = s.trim();
      var i = s.indexOf(':');
      if (i > 0) {
        try {
          final var name = s.substring(0, i);
          final var sub = Integer.parseInt(s.substring(i + 1));
          return new Bit(name, sub);
        } catch (NumberFormatException e) {
          throw new ParserException(S.getter("badVariableIndexError"), i);
        }
      } else if (i == 0) {
        throw new ParserException(S.getter("badVariableColonError"), i);
      }
      i = s.indexOf('[');
      int j = s.lastIndexOf(']');
      if (0 < i && i < j && j == s.length() - 1) {
        try {
          final var name = s.substring(0, i).trim();
          final var sub = Integer.parseInt(s.substring(i + 1, j));
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
    return (width > 1) ? name + "[" + b + "]" : name;
  }

  public Iterator<String> iterator() {
    return new Iterator<>() {
      int bitIndex = width - 1;

      @Override
      public boolean hasNext() {
        return (bitIndex >= 0);
      }

      @Override
      public String next() {
        return bitName(bitIndex--);
      }

      @Override
      public void remove() {
        throw new UnsupportedOperationException();
      }
    };
  }
}
