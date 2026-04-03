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

public final class Value {

  /**
   * Creates a new wire value or retrieves it from the cache if it already exists.
   * Handles values up to MAX_WIDTH bits by using long arrays.
   *
   * @param width the number of bits in this value (0 to MAX_WIDTH)
   * @param error bitmask indicating which bits are in error state
   * @param unknown bitmask indicating which bits are unknown
   * @param value the actual bit values
   * @return a cached Value instance or a new Value if not in cache
   */
  private static Value create(int width, long error, long unknown, long value) {
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

      final var hashCode = Value.hashcode(width, error, unknown, value);
      Object cached = cache.get(hashCode);
      if (cached != null) {
        Value val = (Value) cached;
        if (val.width == width
            && val.value[0] == value
            && val.error[0] == error
            && val.unknown[0] == unknown) return val;
      }
      final var ret = new Value(width, error, unknown, value);
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

      final var hashCode = Value.hashcode(width, errorArray, unknownArray, valueArray);
      Object cached = cache.get(hashCode);
      if (cached != null) {
        Value val = (Value) cached;
        if (val.width == width
            && Arrays.equals(val.value, valueArray)
            && Arrays.equals(val.error, errorArray)
            && Arrays.equals(val.unknown, unknownArray)) return val;
      }
      final var ret = new Value(width, errorArray, unknownArray, valueArray);
      cache.put(hashCode, ret);
      return ret;
    }
  }

  /**
   * Creates a new wire value or retrieves it from the cache if it already exists.
   * Handles values up to MAX_WIDTH bits by using long arrays.<br>
   * If any of the input arrays have less bits in total than the width, they will
   * be padded with zeroes to the desired length, and if the array size exceeds
   * the width, it will be truncated.
   *
   * @param width the number of bits in this value (0 to MAX_WIDTH)
   * @param error bitmask indicating which bits are in error state
   * @param unknown bitmask indicating which bits are unknown
   * @param value the actual bit values
   * @return a cached Value instance or a new Value if not in cache
   */
  private static Value create(int width, long[] error, long[] unknown, long[] value) {
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

      final var hashCode = Value.hashcode(width, error, unknown, value);
      Object cached = cache.get(hashCode);
      if (cached != null) {
        Value val = (Value) cached;
        if (Arrays.equals(val.value, value)
            && val.width == width
            && Arrays.equals(val.error, error)
            && Arrays.equals(val.unknown, unknown)) return val;
      }
      final var ret = new Value(width, error, unknown, value);
      cache.put(hashCode, ret);
      return ret;
    }
  }

  public static Value create_unsafe(int width, long error, long unknown, long value) {
    int hashCode = Value.hashcode(width, error, unknown, value);
    Object obj = cache.get(hashCode);
    if (obj != null) {
      Value val = (Value) obj;
      if (val.value[0] == value && val.width == width && val.error[0] == error && val.unknown[0] == unknown) {
        return val;
      }
    }
    Value ret = new Value(width, error, unknown, value);
    cache.put(hashCode, ret);
    return ret;
  }

  public static Value create_unsafe(int width, long[] error, long[] unknown, long[] value) {
    int hashCode = Value.hashcode(width, error, unknown, value);
    Object obj = cache.get(hashCode);
    if (obj != null) {
      Value val = (Value) obj;
      if (Arrays.equals(val.value, value)
        && val.width == width
        && Arrays.equals(val.error, error)
        && Arrays.equals(val.unknown, unknown)) {
        return val;
      }
    }
    Value ret = new Value(width, error, unknown, value);
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

  /**
   * Code taken from Cornell's version of Logisim: http://www.cs.cornell.edu/courses/cs3410/2015sp/
   */
  public static Value fromLogString(BitWidth width, String t) throws Exception {
    // Strip underscores from the string for readability (e.g., 0x0000_1111 -> 0x00001111)
    // This must be done before radix detection since radixOfLogString uses length
    final var sb = new StringBuilder(t.length());
    for (int i = 0; i < t.length(); i++) {
      final var c = t.charAt(i);
      if (c != '_') {
        sb.append(c);
      }
    }
    final var cleaned = sb.toString();

    final var radix = radixOfLogString(width, cleaned);
    int offset;

    if (radix == 16 && cleaned.startsWith("0x")) offset = 2;
    else if (radix == 8 && cleaned.startsWith("0o")) offset = 2;
    else if (radix == 2 && cleaned.startsWith("0b")) offset = 2;
    else if (radix == 10 && cleaned.startsWith("-")) offset = 1;
    else offset = 0;

    int n = cleaned.length();

    if (n <= offset) throw new Exception("expected digits");

    int w = width.getWidth();
    long value = 0;
    long unknown = 0;

    for (var i = offset; i < n; i++) {
      final var c = cleaned.charAt(i);
      int d;

      if (c == 'x' && radix != 10) d = -1;
      else if ('0' <= c && c <= '9') d = c - '0';
      else if ('a' <= c && c <= 'f') d = 0xa + (c - 'a');
      else if ('A' <= c && c <= 'F') d = 0xA + (c - 'A');
      else
        throw new Exception(
            "Unexpected character '" + cleaned.charAt(i) + "' in \"" + t + "\"");

      if (d >= radix)
        throw new Exception("Unexpected character '" + cleaned.charAt(i) + "' in \"" + t + "\"");

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
    if (radix == 10 && cleaned.charAt(0) == '-') {
      value = -value;
    }

    // Check bit width - for signed values, check the range instead of bit shift
    if (w == 64) {
      if (((value & 0x7FFFFFFFFFFFFFFFL) >> (w - 1)) != 0) {
        int actualBits = 64 - Long.numberOfLeadingZeros(value & 0x7FFFFFFFFFFFFFFFL);
        throw new Exception("Too many bits in \"" + t + "\" expected " + w + " bit" + (w != 1 ? "s" : "")
            + (actualBits > 0 ? " did you mean [" + actualBits + "]?" : ""));
      }
    } else {
      // For signed decimal, check if value fits in w-bit signed range
      if (radix == 10) {
        long maxPositive = (1L << (w - 1)) - 1;
        long minNegative = -(1L << (w - 1));
        if (value > maxPositive || value < minNegative) {
          // Calculate actual bits needed (for absolute value)
          long absValue = value < 0 ? -value : value;
          int actualBits = absValue == 0 ? 1 : 64 - Long.numberOfLeadingZeros(absValue) + 1; // +1 for sign bit
          throw new Exception("Too many bits in \"" + t + "\" expected " + w + " bit" + (w != 1 ? "s" : "")
              + (actualBits > 0 ? " did you mean [" + actualBits + "]?" : ""));
        }
        // Mask to width for signed values (two's complement representation)
        long mask = (1L << w) - 1;
        value &= mask;
      } else {
        // For unsigned (hex, octal, binary), use bit shift check
        if ((value >> w) != 0) {
          // Calculate actual bits needed
          int actualBits = value == 0 ? 1 : 64 - Long.numberOfLeadingZeros(value);
          String reminder = "";

          // For hex values, suggest based on number of hex digits * 4 (each hex digit = 4 bits)
          if (radix == 16 && cleaned.length() > 2) {
            int hexDigits = cleaned.length() - 2; // Subtract "0x" prefix
            // Use hex digits * 4 as the suggested bit width (each hex digit = 4 bits)
            actualBits = hexDigits * 4;
            reminder = " Remember that 0x means hex and each hex digit is 4 bits";
          } else if (radix == 2 && cleaned.length() > 0) {
            // For binary values, suggest based on number of binary digits (each binary digit = 1 bit)
            int binaryDigits = cleaned.length() - (cleaned.startsWith("0b") ? 2 : 0); // Subtract "0b" prefix if present
            // Use binary digits as the suggested bit width (each binary digit = 1 bit)
            actualBits = binaryDigits;
            reminder = " Remember that 0b means binary and each binary digit is 1 bit";
          } else if (radix == 8 && cleaned.length() > 2) {
            // For octal values, suggest based on number of octal digits * 3 (each octal digit = 3 bits)
            int octalDigits = cleaned.length() - 2; // Subtract "0o" prefix
            // Use octal digits * 3 as the suggested bit width (each octal digit = 3 bits)
            actualBits = octalDigits * 3;
            reminder = " Remember that 0o means octal and each octal digit is 3 bits";
          }

          throw new Exception("Too many bits in \"" + t + "\" expected " + w + " bit" + (w != 1 ? "s" : "")
              + (actualBits > 0 ? " did you mean [" + actualBits + "]?" : "") + reminder);
        }
      }
    }

    unknown &= ((1L << w) - 1);
    return create(w, 0, unknown, value);
  }

  public static int radixOfLogString(BitWidth width, String t) {
    if (t.startsWith("0x")) return 16;
    if (t.startsWith("0o")) return 8;
    if (t.startsWith("0b")) return 2;
    if (t.length() == width.getWidth()) return 2;

    return 10;
  }

  public static Value repeat(Value base, BitWidth width) {
    return repeat(base, width.getWidth());
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

  private static long generateMask(int bitWidth) {
    return ((bitWidth % 64) == 0 ? -1L : ~(-1L << bitWidth));
  }

  private static int hashcode(int width, long error, long unknown, long value) {
    var hashCode = width;
    hashCode = 31 * hashCode + (int) (error ^ (error >>> 32));
    hashCode = 31 * hashCode + (int) (unknown ^ (unknown >>> 32));
    hashCode = 31 * hashCode + (int) (value ^ (value >>> 32));
    return hashCode;
  }

  private static int hashcode(int width, long[] error, long[] unknown, long[] value) {
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

  private final int width;

  private final long[] error;
  private final long[] unknown;
  private final long[] value;

  private Value(int width, long error, long unknown, long value) {
    // To ensure that the one-bit values are unique, this should be called
    // only for the one-bit values and by the private create method
    this.width = width;
    this.error = new long[]{error};
    this.unknown = new long[]{unknown};
    this.value = new long[]{value};
  }
  private Value(int width, long[] error, long[] unknown, long[] value) {
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
    } else if (this.width <= 64 && other.width <= 64){
      long false0 = ~this.value[0] & ~this.error[0] & ~this.unknown[0];
      long false1 = ~other.value[0] & ~other.error[0] & ~other.unknown[0];
      long falses = false0 | false1;
      return Value.create(
          Math.max(this.width, other.width),
          (this.error[0] | other.error[0] | this.unknown[0] | other.unknown[0]) & ~falses,
          0,
          this.value[0] & other.value[0]);
    }

    int maxWidth = Math.max(this.width, other.width);
    int len = (maxWidth + 63) >>> 6;

    long[] resultValue = new long[len];
    long[] resultError = new long[len];
    long[] resultUnknown = new long[len];

    for (int i = 0; i < len; i++) {
        long thisValue = (i < this.value.length) ? this.value[i] : 0L;
        long thisError = (i < this.error.length) ? this.error[i] : 0L;
        long thisUnknown = (i < this.unknown.length) ? this.unknown[i] : 0L;

        long otherValue = (i < other.value.length) ? other.value[i] : 0L;
        long otherError = (i < other.error.length) ? other.error[i] : 0L;
        long otherUnknown = (i < other.unknown.length) ? other.unknown[i] : 0L;

        long false0 = ~thisValue & ~thisError & ~thisUnknown;
        long false1 = ~otherValue & ~otherError & ~otherUnknown;
        long falses = false0 | false1;

        resultValue[i] = thisValue & otherValue;

        resultError[i] = (thisError | otherError | thisUnknown | otherUnknown) & ~falses;

        resultUnknown[i] = 0L;
    }

    return Value.create(maxWidth, resultError, resultUnknown, resultValue);
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
      long enabled = (this.value[0] | this.unknown[0] ) & ~this.error[0] ;
      long disabled = ~this.value[0] & ~this.unknown[0] & ~this.error[0] ;
      return Value.create(other.width,
          (this.error[0] | (other.error[0] & ~disabled)),
          (disabled | other.unknown[0]),
          (enabled & other.value[0]));
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
    } else if (this.width == other.width) {
      var newError = new long[error.length];
      var newUnknown = new long[unknown.length];
      var newValue = new long[value.length];
      for (int i = 0; i < this.value.length; i++) {
        long disagree = (this.value[i] ^ other.value[i]) & ~(this.unknown[i] | other.unknown[i]);
        newError[i] = this.error[i] | other.error[i] | disagree;
        newUnknown[i] = this.unknown[i] & other.unknown[i];
        newValue[i] = this.value[i] | other.value[i];
      }
      return Value.create(width, newError, newUnknown, newValue);
    } else {
      final int maxLen = Math.max(this.value.length, other.value.length);
      var newError = new long[maxLen];
      var newUnknown = new long[maxLen];
      var newValue = new long[maxLen];

      int i;
      int minLen = Math.min(error.length, other.error.length);
      for (i = 0; i < minLen - 1; i++) {
        long thisKnown = ~this.unknown[i];
        long otherKnown = ~other.unknown[i];
        long disagree = (this.value[i] ^ other.value[i]) & thisKnown & otherKnown;

        newError[i] = this.error[i] | other.error[i] | disagree;
        newUnknown[i] = ~thisKnown & ~otherKnown;
        newValue[i] = this.value[i] | other.value[i];
      }

      long mask = generateMask(width);
      long thisKnown = ~this.unknown[i] & mask;
      long otherKnown = ~other.unknown[i] & mask;
      long disagree = (this.value[i] ^ other.value[i]) & thisKnown & otherKnown;

      newError[i] = (this.error[i] | other.error[i] | disagree) & mask;
      newUnknown[i] = (~thisKnown & ~otherKnown) & mask;
      newValue[i] = (this.value[i] | other.value[i]) & mask;

      i++;

      if(this.error.length > other.error.length){
        for (; i < this.error.length; i++) {
          newError[i] = this.error[i];
          newUnknown[i] = this.unknown[i];
          newValue[i] = this.value[i];
        }
      } else {
        for (; i < other.error.length; i++) {
          newError[i] = other.error[i];
          newUnknown[i] = other.unknown[i];
          newValue[i] = other.value[i];
        }
      }
      return Value.create(width, newError, newUnknown, newValue);
    }
  }

  public static Value combineLikeWidths(int width, BusConnection[] connections) { // all widths must match
    int n = connections.length;
    for (int i = 0; i < n; i++) {
      Value drivenValue = connections[i].drivenValue;
      if (drivenValue != null && drivenValue != NIL) {
        long[] error = Arrays.copyOf(drivenValue.error, drivenValue.error.length);
        long[] unknown = Arrays.copyOf(drivenValue.unknown, drivenValue.unknown.length);
        long[] value = Arrays.copyOf(drivenValue.value, drivenValue.value.length);
        for (int j = i + 1; j < n; j++) {
          drivenValue = connections[j].drivenValue;
          if (drivenValue == null || drivenValue == NIL) continue;
          if (drivenValue.width != width) {
            throw new IllegalArgumentException("INTERNAL ERROR: mismatched widths in Value.combineLikeWidths");
          }
          for(int k = 0; k < value.length; k++){
            long disagree = (value[k] ^ drivenValue.value[k]) & ~(unknown[k] | drivenValue.unknown[k]);
            error[k] |= drivenValue.error[k] | disagree;
            unknown[k] &= drivenValue.unknown[k];
            value[k] |= drivenValue.value[k];
          }
        }
        return Value.create(width, error, unknown, value);
      }
    }
    return Value.createUnknown(BitWidth.create(width));
  }

  /**
   * Determines whether this Value is compatible with another Value.
   *
   * Compatibility requirements:<br>
   * - widths must be equal.<br>
   * - error state must be equal.<br>
   * - for every known bit, this bit must match the corresponding bit in other.value.<br>
   * - for every unknown bit in this value, other may have unknown or any concrete value.<br><br>
   *
   * Original version from Cornell's version of Logisim: http://www.cs.cornell.edu/courses/cs3410/2015sp/
   *
   * @param other the Value to compare against
   * @return true if the values are compatible, false otherwise
   *
   */
  public boolean compatible(Value other) {
    if(this.width != other.width) return false;

    var valueToTest = new long[value.length];
    var unknownToTest = new long[unknown.length];

   for (int i = 0; i < value.length; i++) {
    valueToTest[i] = other.value[i] & ~this.unknown[i];
    unknownToTest[i] = other.unknown[i] | this.unknown[i];
   }

    return Arrays.equals(this.error, other.error)
        && Arrays.equals(this.value, valueToTest)
        && Arrays.equals(this.unknown, unknownToTest);
  }

  @Override
  public boolean equals(Object otherObj) {
    return (otherObj instanceof Value other)
           ? this.width == other.width
              && Arrays.equals(this.error, other.error)
              && Arrays.equals(this.unknown, other.unknown)
              && Arrays.equals(this.value, other.value)
           : false;
  }

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

  private long[] extendWithOnes(long[] array, int newWidth){
      var newLength = (newWidth + 63) / 64;
      var arrayExtended = Arrays.copyOf(array, newLength);
      var maskInverse = ~generateMask(width);
      arrayExtended[array.length - 1] |= maskInverse;
      for (int i = array.length; i < newLength; i++) {
        arrayExtended[i] = -1L;
      }
      return arrayExtended;
  }

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
    for (int i = 0; i < error.length; i++) {
      if(error[i] != 0) return errorColor;
    }
    if (width == 0) return nilColor;
    if (width == 1) {
      if (this == UNKNOWN) return unknownColor;
      if (this == TRUE) return trueColor;
      return falseColor;
    }
    return multiColor;
  }

  public int getWidth() {
    return width;
  }

  @Override
  public int hashCode() {
    if (width <= 64) {
      return Value.hashcode(width, error[0], unknown[0], value[0]);
    }
    return Value.hashcode(width, error, unknown, value);
  }

  /**
   * @return true if the value contains any error bits.
   */
  public boolean isErrorValue() {
    if(width <= 64) return error[0] != 0;
    long errors = 0;
    for(int i = 0; i < error.length; i++){
      errors |= error[i];
    }
    return errors != 0;
  }

  public boolean isFullyDefined() {
    if(width <= 0) return false;
    if(width <= 64) return error[0] == 0 && unknown[0] == 0;

    long errors = 0;
    long unknowns = 0;
    for(int i = 0; i < error.length; i++){
      errors |= error[i];
    }
    for(int i = 0; i < unknown.length; i++){
      unknowns |= unknown[i];
    }
    return errors == 0 && unknowns == 0;
  }

  /**
   * @return true if the value has no errors and is fully unknown.
   */
  public boolean isUnknown() {
    if(width < 64){
      return error[0] == 0 && unknown[0] == ((1L << width) - 1);
    } else if (width == 64) {
      return error[0] == 0 && unknown[0] == -1L;
    }
    int i;
    for(i = 0; i < unknown.length - 1; i++){
      if(error[i] != 0 || unknown[i] != -1L) return false;
    }
    return error[i] == 0 && unknown[i] == generateMask(width);
  }

  public Value not() {
    if (width <= 1) {
      if (this == TRUE) return FALSE;
      if (this == FALSE) return TRUE;
      return ERROR;
    } else if (width <= 64) {
      return Value.create(width, error[0] | unknown[0], 0, ~value[0]);
    }
    var newError = new long[error.length];
    var newValue = new long[value.length];
    for (int i = 0; i < newError.length; i++) {
      newError[i] = error[i] | unknown[i];
      newValue[i] = ~value[i];
    }
    return Value.create(width, newError, new long[0], newValue);
  }

  public Value or(Value other) {
    if (other == null) return this;
    if (this.width == 1 && other.width == 1) {
      if (this == TRUE || other == TRUE) return TRUE;
      if (this == FALSE && other == FALSE) return FALSE;
      return ERROR;
    } else if(this.width <= 64 && other.width  <= 64) {
      long true0 = this.value[0] & ~this.error[0] & ~this.unknown[0];
      long true1 = other.value[0] & ~other.error[0] & ~other.unknown[0];
      long trues = true0 | true1;
      return Value.create(
          Math.max(this.width, other.width),
          (this.error[0] | other.error[0] | this.unknown[0] | other.unknown[0]) & ~trues,
          0,
          this.value[0] | other.value[0]);
    }

    var newError = new long[Math.max(error.length, other.error.length)];
    var newValue = new long[Math.max(value.length, other.value.length)];

    int i;
    for (i = 0; i < Math.min(error.length, other.error.length); i++) {
      long true0 = this.value[i] & ~this.error[i] & ~this.unknown[i];
      long true1 = other.value[i] & ~other.error[i] & ~other.unknown[i];
      long trues = true0 | true1;
      newError[i] = (this.error[i] | other.error[i] | this.unknown[i] | other.unknown[i]) & ~trues;
      newValue[i] = this.value[i] | other.value[i];
    }
    if(this.error.length > other.error.length){
      for (; i < this.error.length; i++) {
        newError[i] = this.error[i] | this.unknown[i];
        newValue[i] = this.value[i];
      }
    } else {
      for (; i < other.error.length; i++) {
        newError[i] = other.error[i] | other.unknown[i];
        newValue[i] = other.value[i];
      }
    }

    return Value.create(Math.max(this.width, other.width), newError, new long[0], newValue);
  }

  public Value set(int which, Value val) {
    if (val.width != 1) {
      throw new RuntimeException("Cannot set multiple values");
    } else if (which < 0 || which >= width) {
      throw new RuntimeException("Attempt to set outside value's width");
    } else if (width == 1) {
      return val;
    } else if (which < 64){
      long mask = ~(1L << which);
      return Value.create(
          this.width,
          (this.error[0] & mask) | (val.error[0] << which),
          (this.unknown[0] & mask) | (val.unknown[0] << which),
          (this.value[0] & mask) | (val.value[0] << which));
    } else {
      int index = which / 64;
      int bit = which % 64;
      long mask = ~(1L << bit);

      long[] newError = Arrays.copyOf(this.error, this.error.length);
      long[] newUnknown = Arrays.copyOf(this.unknown, this.unknown.length);
      long[] newValue = Arrays.copyOf(this.value, this.value.length);

      newError[index] = (newError[index] & mask) | (val.error[0] << bit);
      newUnknown[index] = (newUnknown[index] & mask) | (val.unknown[0] << bit);
      newValue[index] = (newValue[index] & mask) | (val.value[0] << bit);

      return Value.create(this.width, newError, newUnknown, newValue);
    }
  }

  public String toBinaryString() {
    switch (width) {
      case 0:
        return Character.toString(DONTCARECHAR);
      case 1:
        if (error[0] != 0) return Character.toString(ERRORCHAR);
        else if (unknown[0] != 0) return Character.toString(UNKNOWNCHAR);
        else if (value[0] != 0) return Character.toString(TRUECHAR);
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
        if (error[0] != 0) return Character.toString(ERRORCHAR);
        else if (unknown[0] != 0) return Character.toString(UNKNOWNCHAR);
        else if (value[0] != 0) return Character.toString(TRUECHAR);
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
    if (error[0] != 0) return -1L;
    if (unknown[0] != 0) return -1L;
    return value[0];
  }

  public long toSignExtendedLongValue() {
    if (error[0] != 0) return -1L;
    if (unknown[0] != 0) return -1L;
    final var shift = 64 - width;
    return value[0] << shift >> shift;
  }

  public BigInteger toBigInteger(boolean unsigned) {
    if (width == 0) return BigInteger.ZERO;
    final var expectedLength = (width + 7) / 8;
    byte[] magnitude = new byte[expectedLength];
    int byteIndex = 0;
    for (int longIndex = this.value.length - 1; longIndex >= 0; longIndex--) {
      long longValue = this.value[longIndex];
      int bytesInThisLong = (longIndex == this.value.length - 1) ? ((width % 64 + 7) / 8) : 8;
      for (int b = bytesInThisLong - 1; b >= 0; b--) {
        int shift = b * 8;
        magnitude[byteIndex++] = (byte) ((longValue >> shift) & 0xFF);
      }
    }
    if (unsigned) return new BigInteger(1, magnitude);
    return new BigInteger(magnitude);
  }

  public float toFloatValue() {
    if (error[0] != 0 || unknown[0] != 0 || width != 32) return Float.NaN;
    return Float.intBitsToFloat((int) value[0]);
  }

  public double toDoubleValue() {
    if (error[0] != 0 || unknown[0] != 0 || width != 64) return Double.NaN;
    return Double.longBitsToDouble(value[0]);
  }

  public float toFloatValueFromFP16() {
    if (error[0] != 0 || unknown[0] != 0 || width != 16) return Float.NaN;
    return Float.float16ToFloat((short) value[0]);
  }

  public float toFloatValueFromFP8() {
    if (error[0] != 0 || unknown[0] != 0 || width != 8) return Float.NaN;
    return MiniFloat.miniFloat143ToFloat((byte) value[0]);
  }

  public double toDoubleValueFromAnyFloat() {
    return switch (width) {
      case 8 -> toFloatValueFromFP8();
      case 16 -> toFloatValueFromFP16();
      case 32 -> toFloatValue();
      case 64 -> toDoubleValue();
      default -> Double.NaN;
    };
  }

  public String toStringFromFloatValue() {
    return switch (getWidth()) {
      case 8 -> Float.toString(toFloatValueFromFP8());
      case 16 -> Float.toString(toFloatValueFromFP16());
      case 32 -> Float.toString(toFloatValue());
      case 64 -> Double.toString(toDoubleValue());
      default -> "NaN";
    };
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
        if (error[0] != 0) return Character.toString(ERRORCHAR);
        else if (unknown[0] != 0) return Character.toString(UNKNOWNCHAR);
        else if (value[0] != 0) return Character.toString(TRUECHAR);
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
    }
    if(this.width <= 64 && other.width <= 64) {
      return Value.create(
          Math.max(this.width, other.width),
          this.error[0] | other.error[0] | this.unknown[0] | other.unknown[0],
          0,
          this.value[0] ^ other.value[0]);
    }

    var newError = new long[Math.max(error.length, other.error.length)];
    var newValue = new long[Math.max(value.length, other.value.length)];

    int i;
    for (i = 0; i < Math.min(error.length, other.error.length); i++) {
      newError[i] = this.error[i] | other.error[i] | this.unknown[i] | other.unknown[i];
      newValue[i] = this.value[i] ^ other.value[i];
    }
    if(this.error.length > other.error.length){
      for (; i < this.error.length; i++) {
        newError[i] = this.error[i] | this.unknown[i];
        newValue[i] = this.value[i];
      }
    } else {
      for (; i < other.error.length; i++) {
        newError[i] = other.error[i] | other.unknown[i];
        newValue[i] = other.value[i];
      }
    }

    return Value.create(Math.max(this.width, other.width), newError, new long[0], newValue);
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

  public Value pullTowardsBits(Value other) {
    // wherever this is unknown, use other's value for that bit instead
    if (width <= 0 || Arrays.equals(unknown, new long[unknown.length]) || other.width <= 0) return this;
    var e = new long[error.length];
    var v = new long[error.length];
    var u = new long[error.length];

    int i;
    for (i = 0; i < error.length - 1; i++) {
      e[i] = error[i] | (unknown[i] & other.error[i]);
      v[i] = value[i] | (unknown[i] & other.value[i]);
      u[i] = unknown[i] & (other.unknown[i]);
    }

    e[i] = error[i] | (unknown[i] & other.error[i]);
    v[i] = value[i] | (unknown[i] & other.value[i]);
    u[i] = unknown[i] & (other.unknown[i] | ~generateMask(other.width));

    return Value.create(width, e, u, v);
  }

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
}
