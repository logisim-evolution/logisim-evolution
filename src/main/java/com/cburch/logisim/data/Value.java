/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.data;

import java.awt.Color;
import java.math.BigInteger;
import java.util.Arrays;
import com.cburch.logisim.circuit.CircuitWires.BusConnection;
import com.cburch.logisim.prefs.AppPreferences;
import com.cburch.logisim.util.Cache;
import com.cburch.logisim.util.MiniFloat;

public abstract class Value {

  /**
   * Creates a new wire value or retrieves it from the cache if it already exists.
   * Returns either a LongValue or LongArrayValue instance depending on the size of the
   * width.
   *
   * @param width the number of bits in this value
   * @param error bitmask indicating which bits are in error state
   * @param unknown bitmask indicating which bits are unknown
   * @param value the actual bit values
   * @return a cached Value instance or a new Value if not in cache
   */
  protected static Value create(int width, long error, long unknown, long value) {
    if (width == 0) {
      return Value.NIL;
    } else if (width == 1) {
      if ((error & 1) != 0) return Value.ERROR;
      else if ((unknown & 1) != 0) return Value.UNKNOWN;
      else if ((value & 1) != 0) return Value.TRUE;
      else return Value.FALSE;
    } else if (width <= 64) {
      final var mask = generateMask(width);
      error = error & mask;
      unknown = unknown & mask & ~error;
      value = value & mask & ~unknown & ~error;

      final var hashCode = LongValue.hashcode(width, error, unknown, value);
      Object cached = cache.get(hashCode);
      if (cached != null && cached instanceof LongValue val) {
        if(val.equals(width, error, unknown, value)) return val;
      }
      final var ret = new LongValue(width, error, unknown, value);
      cache.put(hashCode, ret);
      return ret;
    } else {
      final var arraySize = (width + 63) / 64;
      final var remainingBits = width % 64;
      final var mask = generateMask(remainingBits);

      var errorArray = new long[arraySize];
      var unknownArray = new long[arraySize];
      var valueArray = new long[arraySize];

      Arrays.fill(errorArray, error < 0 ? -1 : 0);
      Arrays.fill(unknownArray, unknown < 0 ? -1 : 0);
      Arrays.fill(valueArray, value < 0 ? -1 : 0);

      error = error & mask;
      unknown = unknown & mask & ~error;
      value = value & mask & ~unknown & ~error;

      errorArray[arraySize - 1] = error;
      unknownArray[arraySize - 1] = unknown;
      valueArray[arraySize - 1] = value;

      final var hashCode = LongArrayValue.hashcode(width, errorArray, unknownArray, valueArray);
      Object cached = cache.get(hashCode);
      if (cached != null && cached instanceof LongArrayValue val) {
        if(val.equals(width, errorArray, unknownArray, valueArray)) return val;
      }
      final var ret = new LongArrayValue(width, errorArray, unknownArray, valueArray);
      cache.put(hashCode, ret);
      return ret;
    }
  }

  /**
   * Creates a new wire value or retrieves it from the cache if it already exists.
   * Returns either a LongValue or LongArrayValue instance depending on the size of the
   * width.<br>
   * If any of the input arrays have less bits in total than the width, they will
   * be padded with zeroes to the desired length, and if the array size exceeds
   * the width, it will be truncated.
   *
   * @param width the number of bits in this value
   * @param error bitmask indicating which bits are in error state
   * @param unknown bitmask indicating which bits are unknown
   * @param value the actual bit values
   * @return a cached Value instance or a new Value if not in cache
   */
  protected static Value create(int width, long[] error, long[] unknown, long[] value) {
    if (width <= 64){
      return Value.create(width, error[0], unknown[0], value[0]);
    } else {
      final int expectedLength = (width + 63) / 64;
      if(error.length < expectedLength) {
        error = Arrays.copyOf(error, expectedLength);
      }
      if(unknown.length < expectedLength) {
        unknown = Arrays.copyOf(unknown, expectedLength);
      }
      if(value.length < expectedLength) {
        value = Arrays.copyOf(value, expectedLength);
      }

      final var mask = generateMask(width);
      error[error.length - 1] = error[error.length - 1] & mask;
      unknown[unknown.length - 1] = unknown[unknown.length - 1] & mask & ~error[error.length - 1];
      value[value.length - 1] = value[value.length - 1] & mask & ~unknown[unknown.length - 1] & ~error[error.length - 1];

      final var hashCode = LongArrayValue.hashcode(width, error, unknown, value);
      Object cached = cache.get(hashCode);
      if (cached != null && cached instanceof LongArrayValue val) {
        if(val.equals(width, error, unknown, value)) return val;
      }
      final var ret = new LongArrayValue(width, error, unknown, value);
      cache.put(hashCode, ret);
      return ret;
    }
  }

  /**
   * Creates a new wire value or retrieves it from the cache if it already exists.
   *
   * This method should only be used for bitwidths <= 64 and > 1
   * Otherwise it will return invalid or non-unique 1 bit LongValues.
   *
   * @param width the number of bits in this value
   * @param error bitmask indicating which bits are in error state
   * @param unknown bitmask indicating which bits are unknown
   * @param value the actual bit values
   * @return a cached LongValue instance or a new LongValue if not in cache
   */
  public static Value create_unsafe(int width, long error, long unknown, long value) {
    final var hashCode = LongValue.hashcode(width, error, unknown, value);
    Object cached = cache.get(hashCode);
    if (cached != null && cached instanceof LongValue val) {
      if(val.equals(width, error, unknown, value)) return val;
    }
    Value ret = new LongValue(width, error, unknown, value);
    cache.put(hashCode, ret);
    return ret;
  }

  /**
   * Creates a new wire value or retrieves it from the cache if it already exists.
   *
   * This method should only be used for bitwidths > 64
   * Otherwise it will return invalid LongArrayValues.
   *
   * @param width the number of bits in this value
   * @param error bitmask indicating which bits are in error state
   * @param unknown bitmask indicating which bits are unknown
   * @param value the actual bit values
   * @return a cached LongValue instance or a new LongValue if not in cache
   */
  public static Value create_unsafe(int width, long[] error, long[] unknown, long[] value) {
    final var hashCode = LongArrayValue.hashcode(width, error, unknown, value);
    Object cached = cache.get(hashCode);
    if (cached != null && cached instanceof LongArrayValue val) {
      if(val.equals(width, error, unknown, value)) return val;
    }
    Value ret = new LongArrayValue(width, error, unknown, value);
    cache.put(hashCode, ret);
    return ret;
  }

  public static Value create(Value[] values) {
    if (values.length == 0) return NIL;
    if (values.length == 1) return values[0];
    if (values.length > MAX_WIDTH) {
      throw new RuntimeException("Cannot have more than " + MAX_WIDTH + " bits in a value");
    }
    final var width = values.length;
    final var arraySize = (width + 63) / 64;
    final var remainingBits = width % 64;

    long[] value = new long[arraySize];
    long[] unknown = new long[arraySize];
    long[] error = new long[arraySize];

    for(int j = 0; j < arraySize; j++) {
      int bitsInThisChunk = (j == arraySize - 1 && remainingBits != 0) ? remainingBits : 64;
      for (var i = 0; i < bitsInThisChunk; i++) {
        var index = j * 64 + i;
        long mask = 1L << i;
        if (values[index] == TRUE) value[j] |= mask;
        else if (values[index] == FALSE) /* do nothing */ ;
        else if (values[index] == UNKNOWN) unknown[j] |= mask;
        else if (values[index] == ERROR) error[j] |= mask;
        else {
          throw new RuntimeException("unrecognized value " + values[index]);
        }
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

  public static Value createKnown(BitWidth bits, BigInteger value) {
    int width = bits.getWidth();
    if (width <= 64) {
      return Value.create(width, 0, 0, value.longValue());
    }

    int arraySize = (width + 63) / 64;
    long[] longArray = new long[arraySize];

    BigInteger mask = BigInteger.ONE.shiftLeft(64).subtract(BigInteger.ONE);
    for (int i = 0; i < arraySize; i++) {
        longArray[i] = value.and(mask).longValue();
        value = value.shiftRight(64);
    }

    return Value.create(width, new long[longArray.length], new long[longArray.length], longArray);
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

  public static Value createKnown(BitWidth bits, double value) {
    return createKnown(bits.getWidth(), value);
  }

  public static Value createKnown(int bits, double value) {
    return switch (bits) {
      case 8 -> Value.createKnown(8, MiniFloat.floatToMiniFloat143((float) value));
      case 16 -> Value.createKnown(16, Float.floatToFloat16((float) value));
      case 32 -> Value.createKnown((float) value);
      case 64 -> Value.createKnown(value);
      default -> Value.ERROR;
    };
  }

  public static Value combineLikeWidths(int width, BusConnection[] connections) { // all widths must match
    int n = connections.length;
    for (int i = 0; i < n; i++) {
      Value drivenValue = connections[i].drivenValue;
      if (drivenValue != null && drivenValue != NIL) {
        if(drivenValue instanceof LongArrayValue v) {
          long[] error = Arrays.copyOf(v.error, v.error.length);
          long[] unknown = Arrays.copyOf(v.unknown, v.unknown.length);
          long[] value = Arrays.copyOf(v.value, v.value.length);
          for (int j = i + 1; j < n; j++) {
            drivenValue = connections[j].drivenValue;
            if (drivenValue == null || drivenValue == NIL) continue;
            if (drivenValue.width != width) {
              throw new IllegalArgumentException("INTERNAL ERROR: mismatched widths in Value.combineLikeWidths");
            }
            v = (LongArrayValue) drivenValue;
            for(int k = 0; k < value.length; k++){
              long disagree = (value[k] ^ v.value[k]) & ~(unknown[k] | v.unknown[k]);
              error[k] |= v.error[k] | disagree;
              unknown[k] &= v.unknown[k];
              value[k] |= v.value[k];
            }
          }
          return Value.create(width, error, unknown, value);
        } else {
          LongValue v = (LongValue) drivenValue;
          long error = v.error;
          long unknown = v.unknown;
          long value = v.value;
          for (int j = i + 1; j < n; j++) {
            drivenValue = connections[j].drivenValue;
            if (drivenValue == null || drivenValue == NIL) continue;
            if (drivenValue.width != width) {
              throw new IllegalArgumentException("INTERNAL ERROR: mismatched widths in LongValue.combineLikeWidths_unsafe");
            }
            v = (LongValue) drivenValue;
            long disagree = (value ^ v.value) & ~(unknown | v.unknown);
            error |= v.error | disagree;
            unknown &= v.unknown;
            value |= v.value;
          }
          return Value.create(width, error, unknown, value);
        }
      }
    }
    return Value.createUnknown(BitWidth.create(width));
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

  final protected static long generateMask(int bitWidth) {
    return ((bitWidth % 64) == 0 ? -1L : ~(-1L << bitWidth));
  }
  final protected long[] extendWithOnes(long[] array, int newWidth){
    var newLength = (newWidth + 63) / 64;
    var arrayExtended = Arrays.copyOf(array, newLength);
    var maskInverse = ~generateMask(width);
    arrayExtended[array.length - 1] |= maskInverse;
    for (int i = array.length; i < newLength; i++) {
      arrayExtended[i] = -1L;
    }
    return arrayExtended;
  }

  public static char TRUECHAR = AppPreferences.TRUE_CHAR.get().charAt(0);
  public static char FALSECHAR = AppPreferences.FALSE_CHAR.get().charAt(0);
  public static char UNKNOWNCHAR = AppPreferences.UNKNOWN_CHAR.get().charAt(0);
  public static char ERRORCHAR = AppPreferences.ERROR_CHAR.get().charAt(0);
  public static char DONTCARECHAR = AppPreferences.DONTCARE_CHAR.get().charAt(0);
  public static final Value FALSE = LongValue.FALSE;
  public static final Value TRUE = LongValue.TRUE;
  public static final Value UNKNOWN = LongValue.UNKNOWN;
  public static final Value ERROR = LongValue.ERROR;
  public static final Value NIL = LongValue.NIL;

  public static final int MAX_WIDTH = 2048;

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

  protected final int width;

  protected Value(int width){
    this.width = width;
  }

  public static boolean equal(Value a, Value b) {
    if ((a == null || a == Value.NIL) && (b == null || b == Value.NIL)) {
      return true; // both are effectively NIL
    }
    if (a != null && b != null && a.equals(b)) {
      return true; // both are same non-NIL value
    }
    return false;
  }

  final public BitWidth getBitWidth() {
    return BitWidth.create(width);
  }

  final public int getWidth() {
    return width;
  }

  public Color getColor() {
    if (width == 0) return nilColor;
    if(isErrorValue()) return errorColor;
    if (width == 1) {
      if (this == UNKNOWN) return unknownColor;
      if (this == TRUE) return trueColor;
      return falseColor;
    }
    return multiColor;
  }


  @Override
  public abstract int hashCode();

  @Override
  public abstract boolean equals(Object otherObj);

  /**
   * @return true if the value contains any error bits.
   */
  public abstract boolean isErrorValue();

  /**
   * @return true if the value contains no error or unknown bits.
   */
  public abstract boolean isFullyDefined();

  /**
   * @return true if the value has no errors and is fully unknown.
   */
  public abstract boolean isUnknown();

  /**
   * Determines whether this Value is compatible with another Value.
   *
   * Compatibility requirements:<br>
   * - widths must be equal.<br>
   * - error state must be equal.<br>
   * - for every known bit, this bit must match the corresponding bit in other.value.<br>
   * - for every unknown bit in this value, other may have unknown or any concrete value.<br><br>
   *
   * @param other the Value to compare against
   * @return true if the values are compatible, false otherwise
   *
   */
  public abstract boolean compatible(Value other);

  public abstract Value get(int which);

  public abstract Value[] getAll();


  public abstract Value not();

  public abstract Value and(Value other);

  public abstract Value or(Value other);

  public abstract Value xor(Value other);

  public abstract Value set(int which, Value val);

  public abstract Value combine(Value other);

  /**
   * Determines the output of a tri-state buffer controlled by this Value.
   *
   * This method simulates tri-state logic where this Value acts as a control signal
   * (enable) and the other Value is the data being buffered. The output depends on
   * the control state:
   * <ul>
   *   <li>If control is FALSE: output is fully unknown (high-impedance state)</li>
   *   <li>If control is TRUE or UNKNOWN: output passes through the other value</li>
   *   <li>If control is ERROR: output is error state</li>
   *   <li>If control width doesn't match other width: output is error state</li>
   *   <li>If control is multi-bit: each bit enables/disables the corresponding output bit</li>
   * </ul>
   *
   * @param other the data Value to be controlled/buffered
   * @return a Value representing the tri-state buffer output, or null if other is null
   */
  public abstract Value controls(Value other);

  public abstract Value pullTowardsBits(Value other);

  public abstract Value pullEachBitTowards(Value bit);

  public abstract Value extendWidth(int newWidth, Value others);

  @Override
  public abstract String toString();

  public abstract String toDisplayString();

  public abstract String toDisplayString(int radix);

  public abstract String toBinaryString();

  public abstract String toOctalString();

  public abstract String toHexString();

  public abstract String toDecimalString(boolean signed);

  public abstract String toFloatString();


  public abstract long toLongValue();

  public abstract long toSignExtendedLongValue();

  public abstract BigInteger toBigInteger(boolean unsigned);

  public abstract float toFloatValueFromFP8();

  public abstract float toFloatValueFromFP16();

  public abstract float toFloatValue();

  public abstract double toDoubleValue();

  public abstract double toDoubleValueFromAnyFloat();
}
