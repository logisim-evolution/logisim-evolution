/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.util;

import com.cburch.logisim.prefs.AppPreferences;
import java.awt.Color;
import java.awt.Component;
import javax.swing.UIManager;

/**
 * Some color management helper methods.
 *
 * For Swing's UIManager color keys see:
 */
public final class ColorUtil {

  public static final Color MAGNIFYING_INTERIOR = new Color(255, 230, 230, 220);

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

  /**
   * Return active theme accent color for active circuit highlight.
   */
  public static Color getThemeAccentColor() {
    final var isDark = AppPreferences.isDarkTheme(AppPreferences.LookAndFeel.get());
    return isDark ? new Color(255, 107, 107) : new Color(185, 28, 28);
  }

  /**
   * Return background color for magnifying glass icon interior.
   */
  public static Color getMagnifyingInterior(Component c) {
    var bg = (c != null && c.getBackground() != null) ? c.getBackground() : UIManager.getColor("Tree.background");
    if (bg == null) {
      bg = UIManager.getColor("Panel.background");
    }
    return (bg != null) ? new Color(bg.getRed(), bg.getGreen(), bg.getBlue(), 180) : MAGNIFYING_INTERIOR;
  }

}
