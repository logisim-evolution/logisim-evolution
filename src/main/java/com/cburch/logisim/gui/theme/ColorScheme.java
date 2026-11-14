/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.gui.theme;

import java.awt.Color;
import java.util.Objects;

/**
 * Immutable container for simulation wire and indicator colors for a specific theme variant (Light or Dark).
 */
public record ColorScheme(
    String name,
    Color trueColor,
    Color falseColor,
    Color unknownColor,
    Color errorColor,
    Color mismatchColor,
    Color busColor,
    Color unconnectedColor,
    Color strokeColor,
    Color widthErrorCaptionColor,
    Color widthErrorHighlightColor,
    Color widthErrorBackgroundColor,
    Color clockFrequencyColor) {

  public ColorScheme {
    Objects.requireNonNull(trueColor, "trueColor");
    Objects.requireNonNull(falseColor, "falseColor");
    Objects.requireNonNull(unknownColor, "unknownColor");
    Objects.requireNonNull(errorColor, "errorColor");
    Objects.requireNonNull(mismatchColor, "mismatchColor");
    Objects.requireNonNull(busColor, "busColor");
    Objects.requireNonNull(unconnectedColor, "unconnectedColor");
    Objects.requireNonNull(strokeColor, "strokeColor");
    Objects.requireNonNull(widthErrorCaptionColor, "widthErrorCaptionColor");
    Objects.requireNonNull(widthErrorHighlightColor, "widthErrorHighlightColor");
    Objects.requireNonNull(widthErrorBackgroundColor, "widthErrorBackgroundColor");
    Objects.requireNonNull(clockFrequencyColor, "clockFrequencyColor");
  }

  public static Color fromHex(String hex) {
    if (hex.startsWith("#")) {
      hex = hex.substring(1);
    }
    return new Color(Integer.parseInt(hex, 16));
  }
}
