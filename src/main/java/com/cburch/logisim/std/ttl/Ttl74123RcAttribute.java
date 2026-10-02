/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.std.ttl;

import static com.cburch.logisim.std.Strings.S;

import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.gui.generic.ComboBox;
import com.cburch.logisim.util.StringGetter;
import java.util.Locale;

/**
 * Timing resistor or capacitor for a 74123, stored as kiloohms or picofarads.
 *
 * <p>The attribute table shows a few typical values and also accepts a typed value. A prefixed
 * value is an absolute ohm or farad quantity, as in {@code 10k} or {@code 100n}. A bare number
 * keeps the unit used in saved circuits: kiloohms or picofarads.
 */
final class Ttl74123RcAttribute extends Attribute<Integer> {
  enum Kind {
    RESISTOR,
    CAPACITOR
  }

  static final int RESISTOR_MIN_KOHM = 2;
  static final int RESISTOR_MAX_KOHM = 1000;
  static final int CAPACITOR_MIN_PF = 10_000;
  static final int CAPACITOR_MAX_PF = 1_000_000_000;

  private static final int[] RESISTOR_PRESETS = {2, 10, 100, 1000};
  private static final int[] CAPACITOR_PRESETS = {
    10_000, 100_000, 1_000_000, 10_000_000, 100_000_000, 1_000_000_000
  };

  private final Kind kind;

  private Ttl74123RcAttribute(String name, StringGetter displayName, Kind kind) {
    super(name, displayName);
    this.kind = kind;
  }

  static Ttl74123RcAttribute resistor(String name, StringGetter displayName) {
    return new Ttl74123RcAttribute(name, displayName, Kind.RESISTOR);
  }

  static Ttl74123RcAttribute capacitor(String name, StringGetter displayName) {
    return new Ttl74123RcAttribute(name, displayName, Kind.CAPACITOR);
  }

  @Override
  public java.awt.Component getCellEditor(Integer value) {
    final var presets = kind == Kind.RESISTOR ? RESISTOR_PRESETS : CAPACITOR_PRESETS;
    final var choices = new String[presets.length];
    for (var i = 0; i < presets.length; i++) {
      choices[i] = format(kind, presets[i]);
    }
    final var combo = new ComboBox<>(choices);
    combo.setEditable(true);
    combo.setSelectedItem(value == null ? null : format(kind, value));
    return combo;
  }

  @Override
  public Integer parse(String value) {
    return parse(kind, value);
  }

  @Override
  public String toDisplayString(Integer value) {
    return value == null ? "" : format(kind, value);
  }

  @Override
  public String toStandardString(Integer value) {
    return Integer.toString(value);
  }

  static int parse(Kind kind, String raw) {
    if (raw == null || raw.isBlank()) {
      throw invalid(kind, raw);
    }
    final Token token;
    try {
      token = new Token(raw);
    } catch (SyntaxException e) {
      throw invalid(kind, raw);
    }
    final double quantity = token.absoluteQuantity();
    final long stored =
        token.legacyBareNumber
            ? Math.round(quantity)
            : Math.round(kind == Kind.RESISTOR ? quantity / 1_000.0 : quantity / 1e-12);
    final var min = kind == Kind.RESISTOR ? RESISTOR_MIN_KOHM : CAPACITOR_MIN_PF;
    final var max = kind == Kind.RESISTOR ? RESISTOR_MAX_KOHM : CAPACITOR_MAX_PF;
    if (stored < min || stored > max) {
      throw new NumberFormatException(
          kind == Kind.RESISTOR ? S.get("ttl74123RextRange") : S.get("ttl74123CextRange"));
    }
    return (int) stored;
  }

  static String format(Kind kind, int stored) {
    if (kind == Kind.RESISTOR) {
      return stored == 1000 ? "1 MΩ" : stored + " kΩ";
    }
    if (stored % 1_000_000 == 0) {
      return (stored / 1_000_000) + " µF";
    }
    if (stored % 1_000 == 0) {
      return (stored / 1_000) + " nF";
    }
    return trim(stored / 1_000_000.0) + " µF";
  }

  private static NumberFormatException invalid(Kind kind, String raw) {
    return new NumberFormatException(
        S.get(kind == Kind.RESISTOR ? "ttl74123RextInvalid" : "ttl74123CextInvalid", raw));
  }

  private static String trim(double value) {
    final var text = String.format(Locale.ROOT, "%.3f", value);
    var end = text.length();
    while (end > 0 && text.charAt(end - 1) == '0') {
      end--;
    }
    if (end > 0 && text.charAt(end - 1) == '.') {
      end--;
    }
    return text.substring(0, end);
  }

  private static final class Token {
    private final double magnitude;
    private final double multiplier;
    private final boolean legacyBareNumber;

    private Token(String raw) {
      var text =
          raw.trim()
              .replace(" ", "")
              .replace("\u00a0", "")
              .replace("µ", "u")
              .replace("μ", "u")
              .replace("Ω", "");
      var explicitUnit = false;
      final var lower = text.toLowerCase(Locale.ROOT);
      if (lower.endsWith("ohms")) {
        text = text.substring(0, text.length() - 4);
        explicitUnit = true;
      } else if (lower.endsWith("ohm")) {
        text = text.substring(0, text.length() - 3);
        explicitUnit = true;
      } else if (lower.endsWith("farads")) {
        text = text.substring(0, text.length() - 6);
        explicitUnit = true;
      } else if (lower.endsWith("farad")) {
        text = text.substring(0, text.length() - 5);
        explicitUnit = true;
      } else if (endsWithFaradUnit(text, lower)) {
        text = text.substring(0, text.length() - 1);
        explicitUnit = true;
      }
      var factor = 1.0;
      var prefixed = false;
      final var prefixSource = text.toLowerCase(Locale.ROOT);
      if (prefixSource.endsWith("meg")) {
        factor = 1e6;
        text = text.substring(0, text.length() - 3);
        prefixed = true;
      } else if (!text.isEmpty()) {
        final var prefix = text.charAt(text.length() - 1);
        final var scale = scale(prefix);
        if (scale > 0) {
          factor = scale;
          text = text.substring(0, text.length() - 1);
          prefixed = true;
        }
      }
      try {
        magnitude = Double.parseDouble(text);
      } catch (NumberFormatException e) {
        throw new SyntaxException();
      }
      if (!Double.isFinite(magnitude)) {
        throw new SyntaxException();
      }
      multiplier = factor;
      legacyBareNumber = !explicitUnit && !prefixed && !text.contains("e") && !text.contains("E");
    }

    private double absoluteQuantity() {
      return magnitude * multiplier;
    }
  }

  /** A trailing f/F is the farad unit after a prefix. A capital F after a digit is one farad. */
  private static boolean endsWithFaradUnit(String text, String lower) {
    if (lower.length() < 2 || lower.charAt(lower.length() - 1) != 'f') {
      return false;
    }
    if (!Character.isDigit(lower.charAt(lower.length() - 2))) {
      return true;
    }
    return text.charAt(text.length() - 1) == 'F';
  }

  private static final class SyntaxException extends NumberFormatException {
    private static final long serialVersionUID = 1L;
  }

  private static double scale(char prefix) {
    return switch (prefix) {
      case 'f', 'F' -> 1e-15;
      case 'p', 'P' -> 1e-12;
      case 'n', 'N' -> 1e-9;
      case 'u', 'U' -> 1e-6;
      case 'm' -> 1e-3;
      case 'k', 'K' -> 1e3;
      case 'M' -> 1e6;
      default -> 0;
    };
  }
}
