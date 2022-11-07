/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.util;

import java.awt.Color;

/**
 * Some color management helper methods.
 *
 * For Swing's UIManager color keys see:
 */
public final class ColorUtil {

  private ColorUtil() {
    throw new IllegalStateException("Utility class. No instantiation allowed.");
  }

  /**
   * Returns complementary color to provided one (i.e. white for black etc).
   */
  public static Color getComplementaryColor(Color color) {
    return new Color(255 - color.getRed(), 255 - color.getGreen(), 255 - color.getBlue());
  }

  /**
   * Returns either BLACK or WHITE color, depending on which one would be "more complementary" (better
   * readable) to provided color.
   */
  public static Color getComplementaryBlackWhite(Color color) {
    final var Y = (getLuminance(color) > 128) ? 0 : 255;
    return new Color(Y, Y, Y);
  }

  /**
   * Calculates luminance for given color.
   *
   * https://en.wikipedia.org/wiki/Grayscale
   */
  public static int getLuminance(Color color) {
    return (int) (0.2126 * color.getRed() + 0.7152 * color.getGreen() + 0.0722 * color.getBlue());
  }

  /**
   * Returns grayscale "equivalent" of provided color.
   */
  public static Color toGrayscale(Color color) {
    final int Y = getLuminance(color);
    return new Color(Y, Y, Y);
  }

}
