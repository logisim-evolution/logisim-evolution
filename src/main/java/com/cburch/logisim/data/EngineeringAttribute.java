/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.data;

import static com.cburch.logisim.data.Strings.S;

import com.cburch.logisim.gui.generic.ComboBox;
import com.cburch.logisim.util.StringGetter;
import java.math.BigDecimal;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Resistance or capacitance stored as an integer number of one fixed unit.
 *
 * <p>A circuit file keeps that integer, so a bare number means the stored unit. A prefixed value
 * is an absolute ohm or farad quantity and is accepted only when it lands on a whole stored unit.
 * The text shown in the attribute table parses back to the same integer. SI prefixes keep their
 * case, the farad symbol is a capital {@code F}, and {@code meg} is accepted in any case.
 */
public final class EngineeringAttribute extends Attribute<Integer> {
  /** Unit of the integer written to the circuit file. */
  public enum Unit {
    /** One stored unit is one ohm. */
    OHM(Dimension.RESISTANCE, 0),
    /** One stored unit is one kiloohm. */
    KILOOHM(Dimension.RESISTANCE, 3),
    /** One stored unit is one megaohm. */
    MEGOHM(Dimension.RESISTANCE, 6),
    /** One stored unit is one picofarad. */
    PICOFARAD(Dimension.CAPACITANCE, -12),
    /** One stored unit is one nanofarad. */
    NANOFARAD(Dimension.CAPACITANCE, -9),
    /** One stored unit is one microfarad. */
    MICROFARAD(Dimension.CAPACITANCE, -6);

    private final Dimension dimension;
    /** Power of ten of one stored unit, relative to an ohm or a farad. */
    private final int exponent;

    Unit(Dimension dimension, int exponent) {
      this.dimension = dimension;
      this.exponent = exponent;
    }
  }

  private enum Dimension {
    RESISTANCE,
    CAPACITANCE
  }

  private static final int[] RESISTANCE_DISPLAY = {0, 3, 6};
  private static final int[] CAPACITANCE_DISPLAY = {-12, -9, -6};
  private static final int MAX_TEXT_LENGTH = 80;

  /** {@link Double#parseDouble} also takes hex floats and a d/f literal suffix. These do not. */
  private static final Pattern DECIMAL =
      Pattern.compile("[+-]?(\\d+\\.?\\d*|\\.\\d+)([eE][+-]?\\d+)?");

  private final Unit unit;
  private final int minimum;
  private final int maximum;
  private final int[] presets;

  private EngineeringAttribute(
      String name,
      StringGetter display,
      Unit unit,
      int minimum,
      int maximum,
      int[] presets) {
    super(name, display);
    if (unit == null) {
      throw new IllegalArgumentException("unit is required");
    }
    if (minimum > maximum) {
      throw new IllegalArgumentException("minimum " + minimum + " exceeds maximum " + maximum);
    }
    this.unit = unit;
    this.minimum = minimum;
    this.maximum = maximum;
    this.presets = presets == null ? new int[0] : presets.clone();
    for (final var preset : this.presets) {
      if (preset < minimum || preset > maximum) {
        throw new IllegalArgumentException("preset " + preset + " is outside the accepted range");
      }
    }
  }

  static EngineeringAttribute create(
      String name,
      StringGetter display,
      Unit unit,
      int minimum,
      int maximum,
      int[] presets) {
    return new EngineeringAttribute(name, display, unit, minimum, maximum, presets);
  }

  @Override
  public java.awt.Component getCellEditor(Integer value) {
    if (presets.length == 0) {
      return super.getCellEditor(value);
    }
    final var choices = new String[presets.length];
    for (var i = 0; i < presets.length; i++) {
      choices[i] = format(presets[i]);
    }
    final var combo = new ComboBox<>(choices);
    combo.setEditable(true);
    combo.setSelectedItem(value == null ? null : format(value));
    return combo;
  }

  @Override
  public Integer parse(String value) {
    if (value == null || value.isBlank() || value.length() > MAX_TEXT_LENGTH) {
      throw invalid(value);
    }
    final var text = normalize(value);
    if (text.length() > MAX_TEXT_LENGTH) {
      throw invalid(value);
    }
    final var token = token(value, text);
    final var magnitude = decimal(token.number, value);
    final var bare = !token.explicitUnit && !token.prefixed && !hasExponent(token.number);
    final var scaled =
        bare ? magnitude : magnitude.scaleByPowerOfTen(token.prefixExponent - unit.exponent);
    final long stored;
    try {
      stored = scaled.longValueExact();
    } catch (ArithmeticException ignored) {
      if (hasFraction(scaled)) {
        throw invalid(value);
      }
      throw outOfRange();
    }
    if (stored < minimum || stored > maximum) {
      throw outOfRange();
    }
    return (int) stored;
  }

  @Override
  public String toDisplayString(Integer value) {
    return value == null ? "" : format(value);
  }

  @Override
  public String toStandardString(Integer value) {
    return value == null ? "" : Integer.toString(value);
  }

  private String format(long stored) {
    final var exponent = displayExponent(stored);
    final var decimals = exponent - unit.exponent;
    return formatMagnitude(stored, decimals) + " " + symbol(unit.dimension, exponent);
  }

  /**
   * Largest prefix whose value is at least one, and never a unit above megaohm or microfarad, so
   * 1000 µF stays {@code 1000 µF}.
   */
  private int displayExponent(long stored) {
    final var options =
        unit.dimension == Dimension.RESISTANCE ? RESISTANCE_DISPLAY : CAPACITANCE_DISPLAY;
    var chosen = unit.exponent;
    var found = false;
    for (final var exponent : options) {
      final var delta = exponent - unit.exponent;
      final var qualifies = delta <= 0 ? stored >= 1 : stored >= pow10(delta);
      if (qualifies && (!found || exponent > chosen)) {
        chosen = exponent;
        found = true;
      }
    }
    return chosen;
  }

  private NumberFormatException invalid(String raw) {
    return new NumberFormatException(S.get("engineeringInvalid", raw, hint()));
  }

  private NumberFormatException outOfRange() {
    return new NumberFormatException(
        S.get("engineeringRange", format(minimum), format(maximum)));
  }

  private String hint() {
    final var key =
        switch (unit) {
          case OHM -> "engineeringOhmHint";
          case KILOOHM -> "engineeringKiloohmHint";
          case MEGOHM -> "engineeringMegohmHint";
          case PICOFARAD -> "engineeringPicofaradHint";
          case NANOFARAD -> "engineeringNanofaradHint";
          case MICROFARAD -> "engineeringMicrofaradHint";
        };
    return S.get(key);
  }

  private String normalize(String raw) {
    final var text =
        raw.trim()
            .replace(" ", "")
            .replace("\u00a0", "")
            .replace("µ", "u")
            .replace("μ", "u");
    if (unit.dimension == Dimension.CAPACITANCE && text.indexOf('Ω') >= 0) {
      throw invalid(raw);
    }
    if (unit.dimension == Dimension.RESISTANCE) {
      return text.replace("Ω", "");
    }
    return text;
  }

  private Token token(String raw, String original) {
    var text = original;
    var explicitUnit = false;
    final var lower = text.toLowerCase(Locale.ROOT);
    if (unit.dimension == Dimension.RESISTANCE) {
      if (lower.endsWith("farads") || lower.endsWith("farad")) {
        throw invalid(raw);
      } else if (lower.endsWith("ohms")) {
        text = text.substring(0, text.length() - 4);
        explicitUnit = true;
      } else if (lower.endsWith("ohm")) {
        text = text.substring(0, text.length() - 3);
        explicitUnit = true;
      }
    } else if (lower.endsWith("ohms") || lower.endsWith("ohm")) {
      throw invalid(raw);
    } else if (lower.endsWith("farads")) {
      text = text.substring(0, text.length() - 6);
      explicitUnit = true;
    } else if (lower.endsWith("farad")) {
      text = text.substring(0, text.length() - 5);
      explicitUnit = true;
    } else if (endsWithFaradSymbol(text)) {
      text = text.substring(0, text.length() - 1);
      explicitUnit = true;
    }
    var prefixExponent = 0;
    var prefixed = false;
    if (text.toLowerCase(Locale.ROOT).endsWith("meg")) {
      prefixExponent = 6;
      text = text.substring(0, text.length() - 3);
      prefixed = true;
    } else if (!text.isEmpty()) {
      final var scale = prefixExponent(text.charAt(text.length() - 1));
      // A value other than the sentinel is a prefix. Otherwise the character stays, and the
      // decimal check rejects it.
      if (scale != Integer.MIN_VALUE) {
        prefixExponent = scale;
        text = text.substring(0, text.length() - 1);
        prefixed = true;
      }
    }
    if (!DECIMAL.matcher(text).matches()) {
      throw invalid(raw);
    }
    return new Token(text, prefixExponent, explicitUnit, prefixed);
  }

  /** A capital {@code F} is the farad symbol, so {@code fF} is femtofarad and {@code ff} is not. */
  private static boolean endsWithFaradSymbol(String text) {
    return text.length() >= 2 && text.charAt(text.length() - 1) == 'F';
  }

  /** SI prefixes are case-sensitive, so {@code F}, {@code P}, {@code N}, {@code U}, and {@code K} are not accepted. */
  private static int prefixExponent(char prefix) {
    return switch (prefix) {
      case 'f' -> -15;
      case 'p' -> -12;
      case 'n' -> -9;
      case 'u' -> -6;
      case 'm' -> -3;
      case 'k' -> 3;
      case 'M' -> 6;
      default -> Integer.MIN_VALUE;
    };
  }

  private static boolean hasExponent(String number) {
    return number.indexOf('e') >= 0 || number.indexOf('E') >= 0;
  }

  /** {@link BigDecimal} rejects a trailing dot and a dot directly before an exponent. */
  private BigDecimal decimal(String number, String raw) {
    final var normalized = number.endsWith(".") ? number.substring(0, number.length() - 1) : number;
    final var parsed = parseDecimal(normalized);
    if (parsed != null) {
      return parsed;
    }
    final var fixed = parseDecimal(normalized.replace(".e", "e").replace(".E", "E"));
    if (fixed != null) {
      return fixed;
    }
    throw invalid(raw);
  }

  private static BigDecimal parseDecimal(String number) {
    try {
      return new BigDecimal(number);
    } catch (NumberFormatException ignored) {
      return null;
    }
  }

  private static boolean hasFraction(BigDecimal value) {
    return value.stripTrailingZeros().scale() > 0;
  }

  private static String formatMagnitude(long magnitude, int decimals) {
    if (decimals <= 0) {
      return Long.toString(magnitude);
    }
    final var negative = magnitude < 0;
    var digits = Long.toString(Math.abs(magnitude));
    if (digits.length() <= decimals) {
      digits = "0".repeat(decimals - digits.length() + 1) + digits;
    }
    final var point = digits.length() - decimals;
    final var body = trimZeros(digits.substring(0, point) + "." + digits.substring(point));
    return negative ? "-" + body : body;
  }

  private static String trimZeros(String text) {
    if (text.indexOf('.') < 0) {
      return text;
    }
    var end = text.length();
    while (end > 0 && text.charAt(end - 1) == '0') {
      end--;
    }
    if (end > 0 && text.charAt(end - 1) == '.') {
      end--;
    }
    return text.substring(0, end);
  }

  private static String symbol(Dimension dimension, int exponent) {
    if (dimension == Dimension.RESISTANCE) {
      return switch (exponent) {
        case 0 -> "Ω";
        case 3 -> "kΩ";
        case 6 -> "MΩ";
        default -> "Ω";
      };
    }
    return switch (exponent) {
      case -12 -> "pF";
      case -9 -> "nF";
      case -6 -> "µF";
      default -> "F";
    };
  }

  private static long pow10(int exponent) {
    var value = 1L;
    for (var i = 0; i < exponent; i++) {
      value *= 10L;
    }
    return value;
  }

  private record Token(String number, int prefixExponent, boolean explicitUnit, boolean prefixed) {}
}
