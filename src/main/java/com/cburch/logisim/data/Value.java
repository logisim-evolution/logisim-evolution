/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
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
      final var mask = (width == 64 ? -1L : ~(-1L << width));
      error = error & mask;
      unknown = unknown & mask & ~error;
      value = value & mask & ~unknown & ~error;

      var hashCode = width;
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
      final var ret = new Value(width, error, unknown, value);
      cache.put(hashCode, ret);
      return ret;
    }
  }

  public static Value create(Value[] values) {
    if (values.length == 0) return NIL;
    if (values.length == 1) return values[0];
    if (values.length > MAX_WIDTH) {
      throw new RuntimeException("Cannot have more than " + MAX_WIDTH + " bits in a value");
    }

    final var width = values.length;
    long value = 0;
    long unknown = 0;
    long error = 0;
    for (var i = 0; i < values.length; i++) {
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

  public static Value createUnknown(BitWidth bits) {
    return Value.create(bits.getWidth(), 0, -1, 0);
  }

  public static Value createKnown(BitWidth bits, long value) {
    return Value.create(bits.getWidth(), 0, 0, value);
  }

  public static Value createKnown(float value) {
    return Value.create(32, 0, 0, Float.floatToIntBits(value));
  }

  public static Value createKnown(double value) {
    return Value.create(64, 0, 0, Double.doubleToLongBits(value));
  }

  /* Added to test */
  public static Value createKnown(int bits, long value) {
    return Value.create(bits, 0, 0, value);
  }

  /**
   * Code taken from Cornell's version of Logisim: http://www.cs.cornell.edu/courses/cs3410/2015sp/
   */
  public static Value fromLogString(BitWidth width, String t) throws Exception {
    final var radix = radixOfLogString(width, t);
    int offset;

    if (radix == 16 || radix == 8) offset = 2;
    else if (radix == 10 && t.startsWith("-")) offset = 1;
    else offset = 0;

    int n = t.length();

    if (n <= offset) throw new Exception("expected digits");

    int w = width.getWidth();
    long value = 0;
    long unknown = 0;

    for (var i = offset; i < n; i++) {
      final var c = t.charAt(i);
      int d;

      if (c == 'x' && radix != 10) d = -1;
      else if ('0' <= c && c <= '9') d = c - '0';
      else if ('a' <= c && c <= 'f') d = 0xa + (c - 'a');
      else if ('A' <= c && c <= 'F') d = 0xA + (c - 'A');
      else
        throw new Exception(
            "Unexpected character '" + t.charAt(i) + "' in \"" + t + "\"");

      if (d >= radix)
        throw new Exception("Unexpected character '" + t.charAt(i) + "' in \"" + t + "\"");

      value *= radix;
      unknown *= radix;

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
        throw new Exception("Too many bits in \"" + t + "\"");
    } else {
      if ((value >> w) != 0) throw new Exception("Too many bits in \"" + t + "\"");
    }

    unknown &= ((1L << w) - 1);
    return create(w, 0, unknown, value);
  }

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
      final var ret = new Value[bits];
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

  public static Color falseColor = new Color(AppPreferences.FALSE_COLOR.get());
  public static Color trueColor = new Color(AppPreferences.TRUE_COLOR.get());
  public static Color unknownColor = new Color(AppPreferences.UNKNOWN_COLOR.get());
  public static Color errorColor = new Color(AppPreferences.ERROR_COLOR.get());
  public static Color nilColor = new Color(AppPreferences.NIL_COLOR.get());
  public static Color strokeColor = new Color(AppPreferences.STROKE_COLOR.get());
  public static Color multiColor = new Color(AppPreferences.BUS_COLOR.get());
  public static Color widthErrorColor = new Color(AppPreferences.WIDTH_ERROR_COLOR.get());
  public static Color widthErrorCaptionColor = new Color(AppPreferences.WIDTH_ERROR_CAPTION_COLOR.get());
  public static Color widthErrorHighlightColor = new Color(AppPreferences.WIDTH_ERROR_HIGHLIGHT_COLOR.get());
  public static Color widthErrorCaptionBgcolor = new Color(AppPreferences.WIDTH_ERROR_BACKGROUND_COLOR.get());
  public static Color clockFrequencyColor = new Color(AppPreferences.CLOCK_FREQUENCY_COLOR.get());

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
  public boolean equals(Object otherObj) {
    return (otherObj instanceof Value other)
           ? this.width == other.width
              && this.error == other.error
              && this.unknown == other.unknown
              && this.value == other.value
           : false;
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
    final var ret = new Value[width];
    for (var i = 0; i < ret.length; i++) {
      ret[i] = get(i);
    }
    return ret;
  }

  public BitWidth getBitWidth() {
    return BitWidth.create(width);
  }

  public Color getColor() {
    if (error != 0) {
      return errorColor;
    } else if (width == 0) {
      return nilColor;
    } else if (width == 1) {
      if (this == UNKNOWN) return unknownColor;
      else if (this == TRUE) return trueColor;
      else return falseColor;
    } else {
      return multiColor;
    }
  }

  public int getWidth() {
    return width;
  }

  @Override
  public int hashCode() {
    var ret = width;
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
        final var ret = new StringBuilder();
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

    // Keep only valid bits, zeroing bits above value width.
    long mask = (-1L) >>> (Long.SIZE - width);
    long val = toLongValue() & mask;

    if (signed) {
      // Copy sign bit into upper bits.
      boolean isNegative = (val >> (width - 1)) != 0;
      if (isNegative) {
        val |= ~mask;
      }
      return Long.toString(val);
    } else {
      return Long.toUnsignedString(val);
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
        final var ret = new StringBuilder();
        for (var i = width - 1; i >= 0; i--) {
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
      final var vals = getAll();
      final var c = new char[(vals.length + 3) / 4];
      for (var i = 0; i < c.length; i++) {
        final var k = c.length - 1 - i;
        final var frst = 4 * k;
        final var last = Math.min(vals.length, 4 * (k + 1));
        var v = 0;
        c[i] = ' ';
        for (var j = last - 1; j >= frst; j--) {
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

  public float toFloatValue() {
    if (error != 0 || unknown != 0 || width != 32) return Float.NaN;
    return Float.intBitsToFloat((int) value);
  }

  public double toDoubleValue() {
    if (error != 0 || unknown != 0 || width != 64) return Double.NaN;
    return Double.longBitsToDouble(value);
  }

  public float toFloatValueFromFP16() {
    if (error != 0 || unknown != 0 || width != 16) return Float.NaN;
    return Float.intBitsToFloat(fp16Tofp32_raw((int) value));
  }

  public static int fp16Tofp32_raw(int val) {
    int oldExp = ((val >> 10) & 0b011111);
    int oldFraction = (val & 0b0000001111111111);
    boolean isEven = (val & 0b1000000000000000) == 0;
    //check for zeros
    if (oldFraction == 0 && oldExp == 0) return isEven ? 0x00000000 : 0x80000000;
    //figure out how much the exponent needs adjusted
    int expAdjustment = 0;
    //calculate adjustment if subnormal number
    if (oldExp == 0 && oldFraction != 0)expAdjustment = Integer.numberOfLeadingZeros(oldFraction) - 21;
    //get the new exponent while checking to see if is inf or NaN
    int newExp = (oldExp == 0b11111) ? 0b11111111 : (oldExp - 15 + 127 - ((expAdjustment == 0) ? 0 : (expAdjustment - 1)));
    //start putting together the float
    //add the sign first
    int out = isEven ? 0 : 0x80000000;
    //add the exponent
    out |= newExp << 23;
    //add the fractional part accounting for any subnormal corrections
    out |= (oldFraction << (13 + expAdjustment)) & 0x007FFFFF;
    //correct for NaNs
    if (oldExp == 0b11111 && oldFraction != 0)out |= 0x0400000;
    return out;
  }

  public static int fp32Tofp16_raw(int val) {
    //get the sign
    boolean isEven = (val & 0x80000000) == 0;
    //look at the exponent
    int oldExp = ((val >> 23) & 0x0FF) - 127;
    //get the fractional component
    int oldFraction = (val & 0x007FFFFF);
    //check for numbers that are too large or NaN
    if (oldExp > 15) {
      if (oldExp == 128 && oldFraction != 0) return (isEven ? 0x7E00 : 0xFE00) | (oldFraction >> 13); //NaN
      else return isEven ? 0x7C00 : 0xFC00; //Inf
    }
    //check for numbers that are too small and will be zero
    if (oldExp < -25) return isEven ? 0x0000 : 0x8000;
    //figure out wht the 16bit exponent will be
    boolean isSubnormalNumber = oldExp <= -15;
    int newExp;
    if (isSubnormalNumber) newExp = -15;
    else if (oldExp == 127) newExp = 15; //NaN
    else newExp = oldExp;
    //get the fraction with the hidden bit
    int fraction = oldFraction | 0x00800000;
    //handle numbers that will be subnormal at 16 bits
    if (isSubnormalNumber) {
      int fractionShift = -(oldExp + 14); //right shift amount fo fraction
      //checking for rounding (round to nearest, ties to even)
      int GRS = fraction & (0x1FFFFFFF >> (16 - fractionShift));
      if ((GRS > (0x1000 << fractionShift)) | (GRS == (0x1000 << fractionShift) & (fraction & (0x2000 << fractionShift)) != 0)) {
        //need to round up
        fraction = (fraction >> (13 + fractionShift)) + 1;
        if (fraction >= 0x0400) {
          //need to adjust exponent, but there is only one possibility
          return isEven ? 0x0400 : 0x8400;
        }
      } else {
        fraction = fraction >> (13 + fractionShift);
      }
    } else { //handle numbers that will be normal at 16 bits
      //check for rounding (round to nearest, ties to even)
      int GRS = fraction & 0x1FFF;
      if ((GRS > 0x1000) | (GRS == 0x1000 & (fraction & 0x2000) != 0)) {
        //need to round up
        fraction = (fraction >> 13) + 1;
        if (fraction >= 0x0800) {
          //need to adjust exponent
          if (newExp == 15) return isEven ? 0x7C00 : 0xFC00; //rounded to infinity
          else if (newExp <= 14) newExp++;
          //fix the fractional component
          fraction = fraction >> 1;
        }
      } else {
        fraction = fraction >> 13;
      }
    }
    //assemble
    int out = isEven ? 0x0000 : 0x8000;
    out |= (newExp + 15) << 10;
    out |= fraction & 0x03FF;
    return out & 0xFFFF;
  }

  public String toOctalString() {
    if (width <= 1) {
      return toString();
    } else {
      final var vals = getAll();
      final var c = new char[(vals.length + 2) / 3];
      for (var i = 0; i < c.length; i++) {
        final var k = c.length - 1 - i;
        final var frst = 3 * k;
        final var last = Math.min(vals.length, 3 * (k + 1));
        var v = 0;
        c[i] = ' ';
        for (var j = last - 1; j >= frst; j--) {
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
        final var ret = new StringBuilder();
        for (var i = width - 1; i >= 0; i--) {
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
