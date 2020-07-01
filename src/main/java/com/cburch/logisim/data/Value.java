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

package com.cburch.logisim.data;

import com.cburch.logisim.prefs.AppPreferences;
import com.cburch.logisim.util.Cache;
import java.awt.Color;
import java.util.Arrays;

public class Value {

  private static Value create(int width, long error, long unknown, long value) {
    if (width == 0) {
      return Value.NIL;
    } else if (width == 1) {
      if ((error & 1) != 0) return Value.ERROR;
      else if ((unknown & 1) != 0) return Value.UNKNOWN;
      else if ((value & 1) != 0) return Value.TRUE;
      else return Value.FALSE;
    } else {
      long mask = (width == 64 ? -1L : ~(-1L << width));
      error = error & mask;
      unknown = unknown & mask & ~error;
      value = value & mask & ~unknown & ~error;

      int hashCode = width;
      hashCode = 31 * hashCode + (int) (error ^ (error >>> 32));
      hashCode = 31 * hashCode + (int) (unknown ^ (unknown >>> 32));
      hashCode = 31 * hashCode + (int) (value ^ (value >>> 32));
      Object cached = cache.get(hashCode);
      if (cached != null) {
        Value val = (Value) cached;
        if (val.value == value
            && val.width == width
            && val.error == error
            && val.unknown == unknown) return val;
      }
      Value ret = new Value(width, error, unknown, value);
      cache.put(hashCode, ret);
      return ret;
    }
  }

  public static Value create(Value[] values) {
    if (values.length == 0) return NIL;
    if (values.length == 1) return values[0];
    if (values.length > MAX_WIDTH)
      throw new RuntimeException("Cannot have more than " + MAX_WIDTH + " bits in a value");

    int width = values.length;
    long value = 0;
    long unknown = 0;
    long error = 0;
    for (int i = 0; i < values.length; i++) {
      long mask = 1L << i;
      if (values[i] == TRUE) value |= mask;
      else if (values[i] == FALSE) /* do nothing */ ;
      else if (values[i] == UNKNOWN) unknown |= mask;
      else if (values[i] == ERROR) error |= mask;
      else {
        throw new RuntimeException("unrecognized value " + values[i]);
      }
    }
    return Value.create(width, error, unknown, value);
  }

  public static Value createError(BitWidth bits) {
    return Value.create(bits.getWidth(), -1, 0, 0);
  }

  public static Value createKnown(BitWidth bits, long value) {
    return Value.create(bits.getWidth(), 0, 0, value);
  }

  public static Value createUnknown(BitWidth bits) {
    return Value.create(bits.getWidth(), 0, -1, 0);
  }

  /* Added to test */
  public static Value createKnown(int bits, long value) {
    return Value.create(bits, 0, 0, value);
  }

  /**
   * Code taken from Cornell's version of Logisim: http://www.cs.cornell.edu/courses/cs3410/2015sp/
   */
  public static Value fromLogString(BitWidth width, String t) throws Exception {
    int radix = radixOfLogString(width, t);
    int offset;

    if (radix == 16 || radix == 8) offset = 2;
    else if (radix == 10 && t.startsWith("-")) offset = 1;
    else offset = 0;

    int n = t.length();

    if (n <= offset) throw new Exception("expected digits");

    int w = width.getWidth();
    long value = 0, unknown = 0;

    for (int i = offset; i < n; i++) {
      char c = t.charAt(i);
      int d;

      if (c == 'x' && radix != 10) d = -1;
      else if ('0' <= c && c <= '9') d = c - '0';
      else if ('a' <= c && c <= 'f') d = 0xa + (c - 'a');
      else if ('A' <= c && c <= 'F') d = 0xA + (c - 'A');
      else
        throw new Exception(
            "unexpected character '" + t.substring(i, i + 1) + "' in \"" + t + "\"");

      if (d >= radix)
        throw new Exception(
            "unexpected character '" + t.substring(i, i + 1) + "' in \"" + t + "\"");

      value *= radix;
      unknown *= radix;
      if ((value >> (radix == 10 ? 33 : w)) != 0 || (unknown >> 36) != 0)
        throw new Exception("too many bits in \"" + t + "\"");

      if (radix != 10) {
        if (d == -1) unknown |= (radix - 1);
        else value |= d;
      } else {
        if (d == -1) unknown += (radix - 1);
        else value += d;
      }
    }
    if (radix == 10 && t.charAt(0) == '-') value = -value;

    if (w == 64) {
      if (((value & 0x7FFFFFFFFFFFFFFFL) >> (w - 1)) != 0)
        throw new Exception("too many bits in \"" + t + "\"");
    } else {
      if ((value >> w) != 0) throw new Exception("too many bits in \"" + t + "\"");
    }

    unknown &= ((1L << w) - 1);
    return create(w, 0, unknown, value);
  }

  /**
   * Code taken from Cornell's version of Logisim: http://www.cs.cornell.edu/courses/cs3410/2015sp/
   */
  public static int radixOfLogString(BitWidth width, String t) {
    if (t.startsWith("0x")) return 16;
    if (t.startsWith("0o")) return 8;
    if (t.length() == width.getWidth()) return 2;

    return 10;
  }

  public static Value repeat(Value base, int bits) {
    if (base.getWidth() != 1) {
      throw new IllegalArgumentException("first parameter must be one bit");
    }
    if (bits == 1) {
      return base;
    } else {
      Value[] ret = new Value[bits];
      Arrays.fill(ret, base);
      return create(ret);
    }
  }

  public static char TRUECHAR = AppPreferences.TRUE_CHAR.get().charAt(0);
  public static char FALSECHAR = AppPreferences.FALSE_CHAR.get().charAt(0);
  public static char UNKNOWNCHAR = AppPreferences.UNKNOWN_CHAR.get().charAt(0);
  public static char ERRORCHAR = AppPreferences.ERROR_CHAR.get().charAt(0);
  public static char DONTCARECHAR = AppPreferences.DONTCARE_CHAR.get().charAt(0);
  public static final Value FALSE = new Value(1, 0, 0, 0);
  public static final Value TRUE = new Value(1, 0, 0, 1);
  public static final Value UNKNOWN = new Value(1, 0, 1, 0);
  public static final Value ERROR = new Value(1, 1, 0, 0);
  public static final Value NIL = new Value(0, 0, 0, 0);
  public static final int MAX_WIDTH = 64;

  public static Color FALSE_COLOR = new Color(AppPreferences.FALSE_COLOR.get());
  public static Color TRUE_COLOR = new Color(AppPreferences.TRUE_COLOR.get());
  public static Color UNKNOWN_COLOR = new Color(AppPreferences.UNKNOWN_COLOR.get());
  public static Color ERROR_COLOR = new Color(AppPreferences.ERROR_COLOR.get());
  public static Color NIL_COLOR = new Color(AppPreferences.NIL_COLOR.get());
  public static Color STROKE_COLOR = new Color(AppPreferences.STROKE_COLOR.get());
  public static Color MULTI_COLOR = new Color(AppPreferences.BUS_COLOR.get());
  public static Color WIDTH_ERROR_COLOR = new Color(AppPreferences.WIDTH_ERROR_COLOR.get());
  public static Color WIDTH_ERROR_CAPTION_COLOR = new Color(AppPreferences.WIDTH_ERROR_CAPTION_COLOR.get());
  public static Color WIDTH_ERROR_HIGHLIGHT_COLOR = new Color(AppPreferences.WIDTH_ERROR_HIGHLIGHT_COLOR.get());
  public static Color WIDTH_ERROR_CAPTION_BGCOLOR = new Color(AppPreferences.WIDTH_ERROR_BACKGROUND_COLOR.get());


  private static final Cache cache = new Cache();

  private final int width;

  private final long error;
  private final long unknown;
  private final long value;

  private Value(int width, long error, long unknown, long value) {
    // To ensure that the one-bit values are unique, this should be called
    // only
    // for the one-bit values and by the private create method
    this.width = width;
    this.error = error;
    this.unknown = unknown;
    this.value = value;
  }

  public Value and(Value other) {
    if (other == null) return this;
    if (this.width == 1 && other.width == 1) {
      if (this == FALSE || other == FALSE) return FALSE;
      if (this == TRUE && other == TRUE) return TRUE;
      return ERROR;
    } else {
      long false0 = ~this.value & ~this.error & ~this.unknown;
      long false1 = ~other.value & ~other.error & ~other.unknown;
      long falses = false0 | false1;
      return Value.create(
          Math.max(this.width, other.width),
          (this.error | other.error | this.unknown | other.unknown) & ~falses,
          0,
          this.value & other.value);
    }
  }

  public Value controls(Value other) { // e.g. tristate buffer
    if (other == null)
      return null;
    if (this.width == 1) {
      if (this == FALSE)
        return Value.create(other.width, 0, -1, 0);
      if (this == TRUE || this == UNKNOWN)
        return other;
      return Value.create(other.width, -1, 0, 0);
    } else if (this.width != other.width) {
      return Value.create(other.width, -1, 0, 0);
    } else {
      long enabled = (this.value | this.unknown) & ~this.error;
      long disabled = ~this.value & ~this.unknown & ~this.error;
      return Value.create(other.width,
          (this.error | (other.error & ~disabled)),
          (disabled | other.unknown),
          (enabled & other.value));
    }
  }

  public Value combine(Value other) {
    if (other == null) return this;
    if (this == NIL) return other;
    if (other == NIL) return this;
    if (this.width == 1 && other.width == 1) {
      if (this == other) return this;
      if (this == UNKNOWN) return other;
      if (other == UNKNOWN) return this;
      return ERROR;
    } else {
      long disagree = (this.value ^ other.value) & ~(this.unknown | other.unknown);
      return Value.create(
          Math.max(this.width, other.width),
          this.error | other.error | disagree,
          this.unknown & other.unknown,
          (this.value & ~this.unknown) | (other.value & ~other.unknown));
    }
  }

  /**
   * Code taken from Cornell's version of Logisim: http://www.cs.cornell.edu/courses/cs3410/2015sp/
   */
  public boolean compatible(Value other) {
    // where this has a value, other must have same value
    // where this has unknown, other can have unknown or any value
    // where this has error, other must have error
    return (this.width == other.width
        && this.error == other.error
        && this.value == (other.value & ~this.unknown)
        && this.unknown == (other.unknown | this.unknown));
  }

  @Override
  public boolean equals(Object other_obj) {
    if (!(other_obj instanceof Value)) return false;
    Value other = (Value) other_obj;
    boolean ret =
        this.width == other.width
            && this.error == other.error
            && this.unknown == other.unknown
            && this.value == other.value;
    return ret;
  }

  public Value extendWidth(int newWidth, Value others) {
    if (width == newWidth) return this;
    long maskInverse = (width == 64 ? 0 : (-1L << width));
    if (others == Value.ERROR) {
      return Value.create(newWidth, error | maskInverse, unknown, value);
    } else if (others == Value.FALSE) {
      return Value.create(newWidth, error, unknown, value);
    } else if (others == Value.TRUE) {
      return Value.create(newWidth, error, unknown, value | maskInverse);
    } else {
      return Value.create(newWidth, error, unknown | maskInverse, value);
    }
  }

  public Value get(int which) {
    if (which < 0 || which >= width) return ERROR;
    long mask = 1L << which;
    if ((error & mask) != 0) return ERROR;
    else if ((unknown & mask) != 0) return UNKNOWN;
    else if ((value & mask) != 0) return TRUE;
    else return FALSE;
  }

  public Value[] getAll() {
    Value[] ret = new Value[width];
    for (int i = 0; i < ret.length; i++) {
      ret[i] = get(i);
    }
    return ret;
  }

  public BitWidth getBitWidth() {
    return BitWidth.create(width);
  }

  public Color getColor() {
    if (error != 0) {
      return ERROR_COLOR;
    } else if (width == 0) {
      return NIL_COLOR;
    } else if (width == 1) {
      if (this == UNKNOWN) return UNKNOWN_COLOR;
      else if (this == TRUE) return TRUE_COLOR;
      else return FALSE_COLOR;
    } else {
      return MULTI_COLOR;
    }
  }

  public int getWidth() {
    return width;
  }

  @Override
  public int hashCode() {
    int ret = width;
    ret = 31 * ret + (int) (error ^ (error >>> 32));
    ret = 31 * ret + (int) (unknown ^ (unknown >>> 32));
    ret = 31 * ret + (int) (value ^ (value >>> 32));
    return ret;
  }

  public boolean isErrorValue() {
    return error != 0;
  }

  public boolean isFullyDefined() {
    return width > 0 && error == 0 && unknown == 0;
  }

  public boolean isUnknown() {
    if (width == 64) {
      return error == 0 && unknown == -1L;
    } else {
      return error == 0 && unknown == ((1L << width) - 1);
    }
  }

  public Value not() {
    if (width <= 1) {
      if (this == TRUE) return FALSE;
      if (this == FALSE) return TRUE;
      return ERROR;
    } else {
      return Value.create(this.width, this.error | this.unknown, 0, ~this.value);
    }
  }

  public Value or(Value other) {
    if (other == null) return this;
    if (this.width == 1 && other.width == 1) {
      if (this == TRUE || other == TRUE) return TRUE;
      if (this == FALSE && other == FALSE) return FALSE;
      return ERROR;
    } else {
      long true0 = this.value & ~this.error & ~this.unknown;
      long true1 = other.value & ~other.error & ~other.unknown;
      long trues = true0 | true1;
      return Value.create(
          Math.max(this.width, other.width),
          (this.error | other.error | this.unknown | other.unknown) & ~trues,
          0,
          this.value | other.value);
    }
  }

  public Value set(int which, Value val) {
    if (val.width != 1) {
      throw new RuntimeException("Cannot set multiple values");
    } else if (which < 0 || which >= width) {
      throw new RuntimeException("Attempt to set outside value's width");
    } else if (width == 1) {
      return val;
    } else {
      long mask = ~(1L << which);
      return Value.create(
          this.width,
          (this.error & mask) | (val.error << which),
          (this.unknown & mask) | (val.unknown << which),
          (this.value & mask) | (val.value << which));
    }
  }

  public String toBinaryString() {
    switch (width) {
      case 0:
        return Character.toString(DONTCARECHAR);
      case 1:
        if (error != 0) return Character.toString(ERRORCHAR);
        else if (unknown != 0) return Character.toString(UNKNOWNCHAR);
        else if (value != 0) return Character.toString(TRUECHAR);
        else return Character.toString(FALSECHAR);
      default:
        StringBuilder ret = new StringBuilder();
        for (int i = width - 1; i >= 0; i--) {
          ret.append(get(i).toString());
        }
        return ret.toString();
    }
  }

  public String toDecimalString(boolean signed) {
    if (width == 0) return Character.toString(DONTCARECHAR);
    if (isErrorValue()) return Character.toString(ERRORCHAR);
    if (!isFullyDefined()) return Character.toString(UNKNOWNCHAR);

    long value = toLongValue();
    if (signed) {
      if (width < 64 && (value >> (width - 1)) != 0) {
        value |= (-1) << width;
      }
      return Long.toString(value);
    } else {
      if (width < 64) {
        long mask = (-1 << width)^0xFFFFFFFFFFFFFFFFL;
        value &= mask;
      }
      return Long.toUnsignedString(value);
    }
  }

  public String toDisplayString() {
    switch (width) {
      case 0:
        return Character.toString(DONTCARECHAR);
      case 1:
        if (error != 0) return Character.toString(ERRORCHAR);
        else if (unknown != 0) return Character.toString(UNKNOWNCHAR);
        else if (value != 0) return Character.toString(TRUECHAR);
        else return Character.toString(FALSECHAR);
      default:
        StringBuilder ret = new StringBuilder();
        for (int i = width - 1; i >= 0; i--) {
          ret.append(get(i).toString());
          if (i % 4 == 0 && i != 0) ret.append(" ");
        }
        return ret.toString();
    }
  }

  public String toDisplayString(int radix) {
    switch (radix) {
      case 2:
        return toDisplayString();
      case 8:
        return toOctalString();
      case 16:
        return toHexString();
      default:
        if (width == 0) return Character.toString(DONTCARECHAR);
        if (isErrorValue()) return Character.toString(ERRORCHAR);
        if (!isFullyDefined()) return Character.toString(UNKNOWNCHAR);
        return Long.toString(toLongValue(), radix);
    }
  }

  public String toHexString() {
    if (width <= 1) {
      return toString();
    } else {
      Value[] vals = getAll();
      char[] c = new char[(vals.length + 3) / 4];
      for (int i = 0; i < c.length; i++) {
        int k = c.length - 1 - i;
        int frst = 4 * k;
        int last = Math.min(vals.length, 4 * (k + 1));
        int v = 0;
        c[i] = ' ';
        for (int j = last - 1; j >= frst; j--) {
          if (vals[j] == Value.ERROR) {
            c[i] = ERRORCHAR;
            break;
          }
          if (vals[j] == Value.UNKNOWN) {
            c[i] = UNKNOWNCHAR;
            break;
          }
          v = 2 * v;
          if (vals[j] == Value.TRUE) v++;
        }
        if (c[i] == ' ') c[i] = Character.forDigit(v, 16);
      }
      return new String(c);
    }
  }

  public long toLongValue() {
    if (error != 0) return -1L;
    if (unknown != 0) return -1L;
    return value;
  }

  public String toOctalString() {
    if (width <= 1) {
      return toString();
    } else {
      Value[] vals = getAll();
      char[] c = new char[(vals.length + 2) / 3];
      for (int i = 0; i < c.length; i++) {
        int k = c.length - 1 - i;
        int frst = 3 * k;
        int last = Math.min(vals.length, 3 * (k + 1));
        int v = 0;
        c[i] = ' ';
        for (int j = last - 1; j >= frst; j--) {
          if (vals[j] == Value.ERROR) {
            c[i] = ERRORCHAR;
            break;
          }
          if (vals[j] == Value.UNKNOWN) {
            c[i] = UNKNOWNCHAR;
            break;
          }
          v = 2 * v;
          if (vals[j] == Value.TRUE) v++;
        }
        if (c[i] == ' ') c[i] = Character.forDigit(v, 8);
      }
      return new String(c);
    }
  }

  @Override
  public String toString() {
    switch (width) {
      case 0:
        return Character.toString(DONTCARECHAR);
      case 1:
        if (error != 0) return Character.toString(ERRORCHAR);
        else if (unknown != 0) return Character.toString(UNKNOWNCHAR);
        else if (value != 0) return Character.toString(TRUECHAR);
        else return Character.toString(FALSECHAR);
      default:
        StringBuilder ret = new StringBuilder();
        for (int i = width - 1; i >= 0; i--) {
          ret.append(get(i).toString());
          if (i % 4 == 0 && i != 0) ret.append(" ");
        }
        return ret.toString();
    }
  }

  public Value xor(Value other) {
    if (other == null) return this;
    if (this.width <= 1 && other.width <= 1) {
      if (this == ERROR || other == ERROR) return ERROR;
      if (this == UNKNOWN || other == UNKNOWN) return ERROR;
      if (this == NIL || other == NIL) return ERROR;
      if ((this == TRUE) == (other == TRUE)) return FALSE;
      return TRUE;
    } else {
      return Value.create(
          Math.max(this.width, other.width),
          this.error | other.error | this.unknown | other.unknown,
          0,
          this.value ^ other.value);
    }
  }
}
