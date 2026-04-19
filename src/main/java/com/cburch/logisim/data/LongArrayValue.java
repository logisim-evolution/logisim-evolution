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
import java.util.Arrays;

/**
 * This class is used exclusivly as implentation for Value instances with bitwidth > 64
 */

public final class LongArrayValue extends Value {

  final long[] error;
  final long[] unknown;
  final long[] value;


  /** Should only be instantiated from Value.
   */
  LongArrayValue(int width, long[] error, long[] unknown, long[] value) {
    // To ensure that the one-bit values are unique, this should be called
    // only by the protected create methods
    super(width);
    this.error = error;
    this.unknown = unknown;
    this.value = value;
  }

  public static int hashcode(int width, long[] error, long[] unknown, long[] value) {
    var hashCode = width;
    for (int i = 0; i < error.length; i++) {
      hashCode = 31 * hashCode + (int) (error[i] ^ (error[i] >>> 32));
    }
    for (int i = 0; i < unknown.length; i++) {
      hashCode = 31 * hashCode + (int) (unknown[i] ^ (unknown[i] >>> 32));
    }
    for (int i = 0; i < value.length; i++) {
      hashCode = 31 * hashCode + (int) (value[i] ^ (value[i] >>> 32));
    }
    return hashCode;
  }

  @Override
  public int hashCode() {
    return LongArrayValue.hashcode(width, error, unknown, value);
  }

  @Override
  public boolean equals(Object otherObj) {
    return (otherObj instanceof LongArrayValue other)
           ? this.width == other.width
              && Arrays.equals(this.error, other.error)
              && Arrays.equals(this.unknown, other.unknown)
              && Arrays.equals(this.value, other.value)
           : false;
  }

  public boolean equals(int width, long[] error, long[] unknown, long[] value) {
    return this.width == width
        && Arrays.equals(this.value, value)
        && Arrays.equals(this.error, error)
        && Arrays.equals(this.unknown, unknown);
  }

  @Override
  public boolean isErrorValue() {
    if (width <= 64) return error[0] != 0;
    long errors = 0;
    for (int i = 0; i < error.length; i++) {
      errors |= error[i];
    }
    return errors != 0;
  }

  @Override
  public boolean isFullyDefined() {
    if (width <= 0) return false;
    if (width <= 64) return error[0] == 0 && unknown[0] == 0;

    long errors = 0;
    long unknowns = 0;
    for (int i = 0; i < error.length; i++) {
      errors |= error[i];
    }
    for (int i = 0; i < unknown.length; i++) {
      unknowns |= unknown[i];
    }
    return errors == 0 && unknowns == 0;
  }

  @Override
  public boolean isUnknown() {
    if (width < 64) {
      return error[0] == 0 && unknown[0] == ((1L << width) - 1);
    } else if (width == 64) {
      return error[0] == 0 && unknown[0] == -1L;
    }
    int i;
    for (i = 0; i < unknown.length - 1; i++) {
      if (error[i] != 0 || unknown[i] != -1L) return false;
    }
    return error[i] == 0 && unknown[i] == generateMask(width);
  }

  @Override
  public boolean compatible(Value other) {
    if (this.width != other.width) return false;

    var valueToTest = new long[value.length];
    var unknownToTest = new long[unknown.length];

    LongArrayValue otherL = (LongArrayValue) other;
    for (int i = 0; i < value.length; i++) {
      valueToTest[i] = otherL.value[i] & ~this.unknown[i];
      unknownToTest[i] = otherL.unknown[i] | this.unknown[i];
    }

    return Arrays.equals(this.error, otherL.error)
        && Arrays.equals(this.value, valueToTest)
        && Arrays.equals(this.unknown, unknownToTest);
  }

  @Override
  public Value get(int which) {
    try {
      if (which < 0 || which >= width) return ERROR;
      int whichBit = which % 64;
      int whichIndex = which / 64;
      long mask = 1L << whichBit;
      if ((error[whichIndex] & mask) != 0) return ERROR;
      else if ((unknown[whichIndex] & mask) != 0) return UNKNOWN;
      else if ((value[whichIndex] & mask) != 0) return TRUE;
      else return FALSE;
    } catch (Exception e) {
      return ERROR;
    }
  }

  @Override
  public Value[] getAll() {
    final var ret = new Value[width];

    int bit = 0;

    for (int i = 0; i < value.length; i++) {
      long mask = 1L;
      for (int j = 0; j < 64 & bit < width; j++) {
        if ((error[i] & mask) != 0) ret[bit] = ERROR;
        else if ((unknown[i] & mask) != 0) ret[bit] = UNKNOWN;
        else if ((value[i] & mask) != 0) ret[bit] = TRUE;
        else ret[bit] = FALSE;
        mask <<= 1L;
        bit++;
      }
    }
    return ret;
  }

  @Override
  public Value not() {
    var newError = new long[error.length];
    var newValue = new long[value.length];
    for (int i = 0; i < newError.length; i++) {
      newError[i] = error[i] | unknown[i];
      newValue[i] = ~value[i];
    }
    return Value.create(width, newError, new long[0], newValue);
  }

  @Override
  public Value and(Value other) {
    if (other == null) return this;

    int maxWidth = Math.max(this.width, other.width);
    int len = (maxWidth + 63) / 64;

    long[] resultValue = new long[len];
    long[] resultError = new long[len];

    if (other instanceof LongValue otherL) {
      long false0 = ~this.value[0] & ~this.error[0] & ~this.unknown[0];
      long false1 = ~otherL.value & ~otherL.error & ~otherL.unknown;
      long falses = false0 | false1;

      resultValue[0] = this.value[0] & otherL.value;
      resultError[0] = (this.error[0] | otherL.error | this.unknown[0] | otherL.unknown) & ~falses;
    } else {
      LongArrayValue otherL = (LongArrayValue) other;
      for (int i = 0; i < len; i++) {
        long thisValue = (i < this.value.length) ? this.value[i] : 0L;
        long thisError = (i < this.error.length) ? this.error[i] : 0L;
        long thisUnknown = (i < this.unknown.length) ? this.unknown[i] : 0L;

        long otherValue = (i < otherL.value.length) ? otherL.value[i] : 0L;
        long otherError = (i < otherL.error.length) ? otherL.error[i] : 0L;
        long otherUnknown = (i < otherL.unknown.length) ? otherL.unknown[i] : 0L;

        long false0 = ~thisValue & ~thisError & ~thisUnknown;
        long false1 = ~otherValue & ~otherError & ~otherUnknown;
        long falses = false0 | false1;

        resultValue[i] = thisValue & otherValue;
        resultError[i] = (thisError | otherError | thisUnknown | otherUnknown) & ~falses;
      }
    }

    return Value.create(maxWidth, resultError, new long[0], resultValue);
  }

  @Override
  public Value or(Value other) {
    int maxWidth = Math.max(this.width, other.width);
    int len = (maxWidth + 63) / 64;

    var resultError = new long[len];
    var resultValue = new long[len];

    if (other instanceof LongValue otherL) {
      resultValue = Arrays.copyOf(this.value, len);
      resultError =  Arrays.copyOf(this.error, len);

      long true0 = this.value[0] & ~this.error[0] & ~this.unknown[0];
      long true1 = otherL.value & ~otherL.error & ~otherL.unknown;
      long trues = true0 | true1;

      resultValue[0] = this.value[0] | otherL.value;
      resultError[0] = (this.error[0] | otherL.error | this.unknown[0] | otherL.unknown) & ~trues;
    } else {
      LongArrayValue otherL = (LongArrayValue) other;
      for (int i = 0; i < len; i++) {
        long thisValue = (i < this.value.length) ? this.value[i] : 0L;
        long thisError = (i < this.error.length) ? this.error[i] : 0L;
        long thisUnknown = (i < this.unknown.length) ? this.unknown[i] : 0L;

        long otherValue = (i < otherL.value.length) ? otherL.value[i] : 0L;
        long otherError = (i < otherL.error.length) ? otherL.error[i] : 0L;
        long otherUnknown = (i < otherL.unknown.length) ? otherL.unknown[i] : 0L;

        long true0 = thisValue & ~thisError & ~thisUnknown;
        long true1 = otherValue & ~otherError & ~otherUnknown;
        long trues = true0 | true1;

        resultValue[i] = thisValue | otherValue;
        resultError[i] = (thisError | otherError | thisUnknown | otherUnknown) & ~trues;
      }
    }

    return Value.create(maxWidth, resultError, new long[0], resultValue);
  }

  @Override
  public Value xor(Value other) {
    int maxWidth = Math.max(this.width, other.width);
    int len = (maxWidth + 63) / 64;

    var resultError = new long[len];
    var resultValue = new long[len];

    if (other instanceof LongValue otherL) {
      resultValue = Arrays.copyOf(this.value, len);
      resultError =  Arrays.copyOf(this.error, len);

      long true0 = this.value[0] & ~this.error[0] & ~this.unknown[0];
      long true1 = otherL.value & ~otherL.error & ~otherL.unknown;
      long trues = true0 | true1;

      resultValue[0] = this.value[0] | otherL.value;
      resultError[0] = (this.error[0] | otherL.error | this.unknown[0] | otherL.unknown) & ~trues;
    } else {
      LongArrayValue otherL = (LongArrayValue) other;
      for (int i = 0; i < len; i++) {
        long thisValue = (i < this.value.length) ? this.value[i] : 0L;
        long thisError = (i < this.error.length) ? this.error[i] : 0L;
        long thisUnknown = (i < this.unknown.length) ? this.unknown[i] : 0L;

        long otherValue = (i < otherL.value.length) ? otherL.value[i] : 0L;
        long otherError = (i < otherL.error.length) ? otherL.error[i] : 0L;
        long otherUnknown = (i < otherL.unknown.length) ? otherL.unknown[i] : 0L;

        resultValue[i] = thisValue ^ otherValue;
        resultError[i] = (thisError | otherError | thisUnknown | otherUnknown);
      }
    }

    return Value.create(maxWidth, resultError, new long[0], resultValue);
  }

  @Override
  public Value set(int which, Value val) {
    if (val.width != 1) {
      throw new RuntimeException("Cannot set multiple values");
    } else if (which < 0 || which >= width) {
      throw new RuntimeException("Attempt to set outside value's width");
    }

    int index = which / 64;
    int bit = which % 64;
    long mask = ~(1L << bit);

    long[] newError = Arrays.copyOf(this.error, this.error.length);
    long[] newUnknown = Arrays.copyOf(this.unknown, this.unknown.length);
    long[] newValue = Arrays.copyOf(this.value, this.value.length);

    LongValue valL = (LongValue) val;
    newError[index] = (newError[index] & mask) | (valL.error << bit);
    newUnknown[index] = (newUnknown[index] & mask) | (valL.unknown << bit);
    newValue[index] = (newValue[index] & mask) | (valL.value << bit);

    return Value.create(this.width, newError, newUnknown, newValue);
  }

  @Override
  public Value combine(Value other) {
    if (other == null || other == NIL) return this;
    else if (this.width == other.width) {
      var newError = new long[error.length];
      var newUnknown = new long[unknown.length];
      var newValue = new long[value.length];
      LongArrayValue otherL = (LongArrayValue) other;
      for (int i = 0; i < this.value.length; i++) {
        long disagree = (this.value[i] ^ otherL.value[i]) & ~(this.unknown[i] | otherL.unknown[i]);
        newError[i] = this.error[i] | otherL.error[i] | disagree;
        newUnknown[i] = this.unknown[i] & otherL.unknown[i];
        newValue[i] = this.value[i] | otherL.value[i];
      }
      return Value.create(width, newError, newUnknown, newValue);
    } else if (other instanceof LongValue) {
      other = other.extendWidth(this.width, UNKNOWN);
    }

    LongArrayValue otherL = (LongArrayValue) other;
    final int maxLen = Math.max(this.value.length, otherL.value.length);
    var newError = new long[maxLen];
    var newUnknown = new long[maxLen];
    var newValue = new long[maxLen];

    int i;
    int minLen = Math.min(this.error.length, otherL.error.length);
    for (i = 0; i < minLen - 1; i++) {
      long thisKnown = ~this.unknown[i];
      long otherKnown = ~otherL.unknown[i];
      long disagree = (this.value[i] ^ otherL.value[i]) & thisKnown & otherKnown;

      newError[i] = this.error[i] | otherL.error[i] | disagree;
      newUnknown[i] = ~thisKnown & ~otherKnown;
      newValue[i] = this.value[i] | otherL.value[i];
    }

    long mask = generateMask(width);
    long thisKnown = ~this.unknown[i] & mask;
    long otherKnown = ~otherL.unknown[i] & mask;
    long disagree = (this.value[i] ^ otherL.value[i]) & thisKnown & otherKnown;

    newError[i] = (this.error[i] | otherL.error[i] | disagree) & mask;
    newUnknown[i] = (~thisKnown & ~otherKnown) & mask;
    newValue[i] = (this.value[i] | otherL.value[i]) & mask;

    i++;

    if (this.error.length > otherL.error.length) {
      for (; i < this.error.length; i++) {
        newError[i] = this.error[i];
        newUnknown[i] = this.unknown[i];
        newValue[i] = this.value[i];
      }
    } else {
      for (; i < otherL.error.length; i++) {
        newError[i] = otherL.error[i];
        newUnknown[i] = otherL.unknown[i];
        newValue[i] = otherL.value[i];
      }
    }
    return Value.create(width, newError, newUnknown, newValue);
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
      //TODO implement support for bitwidths > 64
      LongArrayValue otherL = (LongArrayValue) other;
      long enabled = (this.value[0] | this.unknown[0]) & ~this.error[0];
      long disabled = ~this.value[0] & ~this.unknown[0] & ~this.error[0];
      return Value.create(other.width,
          (this.error[0] | (otherL.error[0] & ~disabled)),
          (disabled | otherL.unknown[0]),
          (enabled & otherL.value[0]));
    }
  }

  @Override
  public Value pullTowardsBits(Value other) {
    // wherever this is unknown, use other's value for that bit instead
    if (Arrays.equals(unknown, new long[unknown.length]) || other.width <= 0) return this;
    var e = new long[error.length];
    var v = new long[error.length];
    var u = new long[error.length];

    int i;
    if (other instanceof LongValue otherL) {
      for (i = 0; i < error.length; i++) {
        long otherError = (i == 0) ? otherL.error : 0L;
        long otherValue = (i == 0) ? otherL.value : 0L;
        long otherUnknown = (i == 0) ? otherL.unknown : 0L;

        e[i] = error[i] | (unknown[i] & otherError);
        v[i] = value[i] | (unknown[i] & otherValue);
        u[i] = unknown[i] & otherUnknown;
      }
      return Value.create(width, e, u, v);
    }

    LongArrayValue otherL = (LongArrayValue) other;
    for (i = 0; i < error.length - 1; i++) {
      e[i] = error[i] | (unknown[i] & otherL.error[i]);
      v[i] = value[i] | (unknown[i] & otherL.value[i]);
      u[i] = unknown[i] & (otherL.unknown[i]);
    }

    e[i] = error[i] | (unknown[i] & otherL.error[i]);
    v[i] = value[i] | (unknown[i] & otherL.value[i]);
    u[i] = unknown[i] & (otherL.unknown[i] | ~generateMask(otherL.width));

    return Value.create(width, e, u, v);
  }

  @Override
  public Value pullEachBitTowards(Value bit) {
    // wherever this is unknown, use bit instead
    if (width <= 0 || Arrays.equals(unknown, new long[unknown.length]) || bit.width <= 0) return this;
    if (bit == ERROR) {
      var newError = new long[unknown.length];
      for (int i = 0; i < newError.length; i++) {
        newError[i] = error[i] | unknown[i];
      }
      return Value.create(width, newError, new long[unknown.length], value);
    } else if (bit == TRUE) {
      var newValue = new long[unknown.length];
      for (int i = 0; i < newValue.length; i++) {
        newValue[i] = value[i] | unknown[i];
      }
      return Value.create(width, error, new long[unknown.length], newValue);
    } else if (bit == FALSE) {
      var newValue = new long[unknown.length];
      for (int i = 0; i < newValue.length; i++) {
        newValue[i] = value[i] & ~unknown[i];
      }
      return Value.create(width, error, new long[unknown.length], newValue);
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

    if (others == Value.ERROR) {
      return Value.create(newWidth, extendWithOnes(error, newWidth), unknown, value);
    } else if (others == Value.FALSE) {
      return Value.create(newWidth, error, unknown, value);
    } else if (others == Value.TRUE) {
      return Value.create(newWidth, error, unknown, extendWithOnes(value, newWidth));
    } else {
      return Value.create(newWidth, error, extendWithOnes(unknown, newWidth), value);
    }
  }

  @Override
  public String toString() {
    final var ret = new StringBuilder();
    for (var i = width - 1; i >= 0; i--) {
      ret.append(get(i).toString());
      if (i % 4 == 0 && i != 0) ret.append(" ");
    }
    return ret.toString();
  }

  @Override
  public String toDisplayString() {
    final var ret = new StringBuilder();
    for (var i = width - 1; i >= 0; i--) {
      ret.append(get(i).toString());
      if (i % 4 == 0 && i != 0) ret.append(" ");
    }
    return ret.toString();
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
        if (isErrorValue()) return Character.toString(ERRORCHAR);
        if (!isFullyDefined()) return Character.toString(UNKNOWNCHAR);
        //TODO implement custom radix sizes properly for bitwidths > 64
        return Long.toString(toLongValue(), radix);
    }
  }

  @Override
  public String toBinaryString() {
    final var ret = new StringBuilder();
    for (int i = width - 1; i >= 0; i--) {
      ret.append(get(i).toString());
    }
    return ret.toString();
  }

  @Override
  public String toOctalString() {
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

  @Override
  public String toHexString() {
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

  @Override
  public String toDecimalString(boolean signed) {
    return toBigInteger(!signed).toString();
  }

  @Override
  public String toFloatString() {
    return "NaN";
  }


  @Override
  public long toLongValue() {
    if (error[0] != 0) return -1L;
    if (unknown[0] != 0) return -1L;
    return value[0];
  }

  @Override
  public long toSignExtendedLongValue() {
    //Value will always be truncated since we will always have > 64 bits
    //in LongArrayValues
    return toLongValue();
  }

  @Override
  public BigInteger toBigInteger(boolean unsigned) {
    int byteLength = (width + 7) / 8;
    byte[] magnitude = new byte[byteLength];

    int byteIndex = byteLength;
    for (int longIndex = 0; longIndex < value.length; longIndex++) {
      long word = value[longIndex];
      int bytesInWord = (longIndex == value.length - 1) ? ((width + 7) / 8) - (longIndex * 8) : 8;
      for (int b = 0; b < bytesInWord; b++) {
        magnitude[--byteIndex] = (byte) (word >>> (8 * b));
      }
    }

    if (!unsigned) {
      boolean negative = (value[value.length - 1] < 0);
      if (negative) {
        for (int i = 0; i < byteIndex; i++) {
          magnitude[i] = (byte) 0xFF;
        }
      }
    }

    return unsigned ? new BigInteger(1, magnitude) : new BigInteger(magnitude);
  }

  @Override
  public float toFloatValueFromFP8() {
    return Float.NaN;
  }

  @Override
  public float toFloatValueFromFP16() {
    return Float.NaN;
  }

  @Override
  public float toFloatValue() {
    return Float.NaN;
  }

  @Override
  public double toDoubleValue() {
    return Double.NaN;
  }

  @Override
  public double toDoubleValueFromAnyFloat() {
    return Double.NaN;
  }
}
