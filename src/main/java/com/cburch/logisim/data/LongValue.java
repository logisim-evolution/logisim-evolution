/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.data;

import java.math.BigInteger;
import com.cburch.logisim.util.MiniFloat;

/**
 * This class is used exclusivly as implentation for Value instances with bitwidth <= 64
 */
public final class LongValue extends Value {

  public static final Value FALSE = new LongValue(1, 0, 0, 0);
  public static final Value TRUE = new LongValue(1, 0, 0, 1);
  public static final Value UNKNOWN = new LongValue(1, 0, 1, 0);
  public static final Value ERROR = new LongValue(1, 1, 0, 0);
  public static final Value NIL = new LongValue(0, 0, 0, 0);

  final long error;
  final long unknown;
  final long value;

  /** To ensure that the one-bit values are unique, this should be called
   * only for the one-bit values and by the protected create method
   */
  LongValue(int width, long error, long unknown, long value) {
    super(width);
    this.error = error;
    this.unknown = unknown;
    this.value = value;
  }

  public static int hashcode(int width, long error, long unknown, long value) {
    var hashCode = width;
    hashCode = 31 * hashCode + (int) (error ^ (error >>> 32));
    hashCode = 31 * hashCode + (int) (unknown ^ (unknown >>> 32));
    hashCode = 31 * hashCode + (int) (value ^ (value >>> 32));
    return hashCode;
  }

  @Override
  public int hashCode() {
    return LongValue.hashcode(width, error, unknown, value);
  }

  @Override
  public boolean equals(Object otherObj) {
    return (otherObj instanceof LongValue other)
           ? this.width == other.width
              && this.error == other.error
              && this.unknown == other.unknown
              && this.value == other.value
           : false;
  }

  public boolean equals(int width, long error, long unknown, long value) {
    return this.value == value
        && this.width == width
        && this.error == error
        && this.unknown == unknown;
  }

  @Override
  public boolean isErrorValue() {
    return error != 0;
  }

  @Override
  public boolean isFullyDefined() {
    return width > 0 && error == 0 && unknown == 0;
  }

  @Override
  public boolean isUnknown() {
    if (width == 64) {
      return error == 0 && unknown == -1L;
    } else {
      return error == 0 && unknown == ((1L << width) - 1);
    }
  }

  /** {@inheritDoc} */
  @Override
  public boolean compatible(Value other) {
    if (this.width != other.width) {
      return false;
    }
    if (other instanceof LongValue otherLong) {
      return this.error == otherLong.error
          && this.value == (otherLong.value & ~this.unknown)
          && this.unknown == (otherLong.unknown | this.unknown);
    }
    return false;
  }

  @Override
  public Value get(int which) {
    if (which < 0 || which >= width) return ERROR;
    long mask = 1L << which;
    if ((error & mask) != 0) return ERROR;
    if ((unknown & mask) != 0) return UNKNOWN;
    if ((value & mask) != 0) return TRUE;
    return FALSE;
  }

  @Override
  public Value[] getAll() {
    final var ret = new Value[width];
    for (var i = 0; i < ret.length; i++) {
      ret[i] = get(i);
    }
    return ret;
  }


  @Override
  public Value not() {
    if (width <= 1) {
      if (this == TRUE) return FALSE;
      if (this == FALSE) return TRUE;
      return ERROR;
    } else {
      return Value.create(this.width, this.error | this.unknown, 0, ~this.value);
    }
  }

  @Override
  public Value and(Value other) {
    if (other == null) return this;
    if (this.width == 1 && other.width == 1) {
      if (this == FALSE || other == FALSE) return FALSE;
      if (this == TRUE && other == TRUE) return TRUE;
      return ERROR;
    } else if (other instanceof LongValue otherL) {
      long false0 = ~this.value & ~this.error & ~this.unknown;
      long false1 = ~otherL.value & ~otherL.error & ~otherL.unknown;
      long falses = false0 | false1;
      return Value.create(
          Math.max(this.width, otherL.width),
          (this.error | otherL.error | this.unknown | otherL.unknown) & ~falses,
          0,
          this.value & otherL.value);
    }
    return other.and(this);
  }

  @Override
  public Value or(Value other) {
    if (other == null) return this;
    if (this.width == 1 && other.width == 1) {
      if (this == TRUE || other == TRUE) return TRUE;
      if (this == FALSE && other == FALSE) return FALSE;
      return ERROR;
    } else if (other instanceof LongValue otherL) {
      long true0 = this.value & ~this.error & ~this.unknown;
      long true1 = otherL.value & ~otherL.error & ~otherL.unknown;
      long trues = true0 | true1;
      return Value.create(
          Math.max(this.width, otherL.width),
          (this.error | otherL.error | this.unknown | otherL.unknown) & ~trues,
          0,
          this.value | otherL.value);
    }
    return other.or(this);
  }

  @Override
  public Value xor(Value other) {
    if (other == null) return this;
    if (this.width <= 1 && other.width <= 1) {
      if (this == ERROR || other == ERROR) return ERROR;
      if (this == UNKNOWN || other == UNKNOWN) return ERROR;
      if (this == NIL || other == NIL) return ERROR;
      if ((this == TRUE) == (other == TRUE)) return FALSE;
      return TRUE;
    } else if (other instanceof LongValue otherL) {
      return Value.create(
          Math.max(this.width, otherL.width),
          this.error | otherL.error | this.unknown | otherL.unknown,
          0,
          this.value ^ otherL.value);
    }
    return other.xor(this);
  }

  @Override
  public Value set(int which, Value val) {
    if (val.width != 1) {
      throw new RuntimeException("Cannot set multiple values");
    } else if (which < 0 || which >= width) {
      throw new RuntimeException("Attempt to set outside value's width");
    } else if (width == 1) {
      return val;
    } else {
      long mask = ~(1L << which);
      LongValue longVal = (LongValue) val;
      return Value.create(
          this.width,
          (this.error & mask) | (longVal.error << which),
          (this.unknown & mask) | (longVal.unknown << which),
          (this.value & mask) | (longVal.value << which));
    }
  }

  @Override
  public Value combine(Value other) {
    if (other == null) return this;
    if (this == NIL) return other;
    if (other == NIL) return this;
    if (this.width == 1 && other.width == 1) {
      if (this == other) return this;
      if (this == UNKNOWN) return other;
      if (other == UNKNOWN) return this;
      return ERROR;
    } else if (this.width == other.width) {
      LongValue otherL = (LongValue) other;
      long disagree = (this.value ^ otherL.value) & ~(this.unknown | otherL.unknown);
      return Value.create(
          width,
          this.error | otherL.error | disagree,
          this.unknown & otherL.unknown,
          this.value | otherL.value);
    } else if (other instanceof LongValue otherL) {
      long thisKnown = ~this.unknown & (this.width == 64 ? -1 : ~(-1 << this.width));
      long otherKnown = ~otherL.unknown & (otherL.width == 64 ? -1 : ~(-1 << otherL.width));
      long disagree = (this.value ^ otherL.value) & thisKnown & otherKnown;
      return Value.create(
          Math.max(this.width, otherL.width),
          this.error | otherL.error | disagree,
          ~thisKnown & ~otherKnown,
          this.value | otherL.value);
    }
    return other.combine(this);
  }

  @Override
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
      LongValue otherL = (LongValue) other;
      long enabled = (this.value | this.unknown) & ~this.error;
      long disabled = ~this.value & ~this.unknown & ~this.error;
      return Value.create(other.width,
          (this.error | (otherL.error & ~disabled)),
          (disabled | otherL.unknown),
          (enabled & otherL.value));
    }
  }

  @Override
  public Value pullTowardsBits(Value other) {
    // wherever this is unknown, use other's value for that bit instead
    if (width <= 0 || unknown == 0 || other.width <= 0) return this;

    if (other.width > 64) {
      Value[] otherBits = new Value[64];
      for (int i = 0; i < 64; i++) {
        otherBits[i] = other.get(i);
      }
      other = create(otherBits);
    }

    LongValue otherL = (LongValue) other;
    long e = error | (unknown & otherL.error);
    long v = value | (unknown & otherL.value);
    long u = unknown & (otherL.unknown | ~generateMask(other.width));
    return Value.create(width, e, u, v);
  }

  @Override
  public Value pullEachBitTowards(Value bit) {
    // wherever this is unknown, use bit instead
    if (width <= 0 || unknown == 0 || bit.width <= 0) return this;
    if (bit == ERROR) {
      return Value.create(width, error | unknown, 0, value);
    } else if (bit == TRUE) {
      return Value.create(width, error, 0, value | unknown);
    } else if (bit == FALSE) {
      return Value.create(width, error, 0, value | 0);
    } else if (bit == UNKNOWN) {
      return this;
    } else {
      throw new IllegalArgumentException("pull value must be 1, 0, X, or E");
    }
  }

  @Override
  public Value extendWidth(int newWidth, Value others) {
    if (width == newWidth) return this;
    if (newWidth < width) return Value.create(newWidth, error, unknown, value);

    if (newWidth <= 64) {
      long maskInverse = ~generateMask(width);
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

    long[] errorArr = new long[]{error};
    long[] valueArr = new long[]{value};
    long[] unknownArr = new long[]{unknown};

    if (others == Value.ERROR) {
      return Value.create(newWidth, extendWithOnes(errorArr, newWidth), unknownArr, valueArr);
    } else if (others == Value.FALSE) {
      return Value.create(newWidth, errorArr, unknownArr, valueArr);
    } else if (others == Value.TRUE) {
      return Value.create(newWidth, errorArr, unknownArr, extendWithOnes(valueArr, newWidth));
    } else {
      return Value.create(newWidth, errorArr, extendWithOnes(unknownArr, newWidth), valueArr);
    }
  }

  @Override
  public String toString() {
    return toDisplayString();
  }

  @Override
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

  @Override
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

  @Override
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

  @Override
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

  @Override
  public String toDecimalString(boolean signed) {
    if (width == 0) return Character.toString(DONTCARECHAR);
    if (isErrorValue()) return Character.toString(ERRORCHAR);
    if (!isFullyDefined()) return Character.toString(UNKNOWNCHAR);

    if (signed) {
      return Long.toString(toSignExtendedLongValue());
    } else {
      return Long.toUnsignedString(toLongValue());
    }
  }

  @Override
  public String toFloatString() {
    return switch (getWidth()) {
      case 8 -> Float.toString(toFloatValueFromFP8());
      case 16 -> Float.toString(toFloatValueFromFP16());
      case 32 -> Float.toString(toFloatValue());
      case 64 -> Double.toString(toDoubleValue());
      default -> "NaN";
    };
  }


  @Override
  public long toLongValue() {
    if (error != 0) return -1L;
    if (unknown != 0) return -1L;
    return value;
  }

  @Override
  public long toSignExtendedLongValue() {
    if (error != 0) return -1L;
    if (unknown != 0) return -1L;
    final var shift = 64 - width;
    return value << shift >> shift;
  }

  @Override
  public BigInteger toBigInteger(boolean unsigned) {
    var mask = (width == 64 ? -1L : ~(-1L << width));
    long value = this.value & mask;
    if (unsigned) {
      return new BigInteger(
        1,
          new byte[] {
            (byte) ((value >> 56) & 0xFFL),
            (byte) ((value >> 48) & 0xFFL),
            (byte) ((value >> 40) & 0xFFL),
            (byte) ((value >> 32) & 0xFFL),
            (byte) ((value >> 24) & 0xFFL),
            (byte) ((value >> 16) & 0xFFL),
            (byte) ((value >> 8) & 0xFFL),
            (byte) ((value) & 0xFFL)
          }
      );
    }
    if ((value >> (width - 1)) != 0) value |= ~mask;
    return BigInteger.valueOf(value);
  }

  @Override
  public float toFloatValueFromFP8() {
    if (error != 0 || unknown != 0 || width != 8) return Float.NaN;
    return MiniFloat.miniFloat143ToFloat((byte) value);
  }

  @Override
  public float toFloatValueFromFP16() {
    if (error != 0 || unknown != 0 || width != 16) return Float.NaN;
    return Float.float16ToFloat((short) value);
  }

  @Override
  public float toFloatValue() {
    if (error != 0 || unknown != 0 || width != 32) return Float.NaN;
    return Float.intBitsToFloat((int) value);
  }

  @Override
  public double toDoubleValue() {
    if (error != 0 || unknown != 0 || width != 64) return Double.NaN;
    return Double.longBitsToDouble(value);
  }

  @Override
  public double toDoubleValueFromAnyFloat() {
    return switch (width) {
      case 8 -> toFloatValueFromFP8();
      case 16 -> toFloatValueFromFP16();
      case 32 -> toFloatValue();
      case 64 -> toDoubleValue();
      default -> Double.NaN;
    };
  }
}
