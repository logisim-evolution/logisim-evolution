/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.gui.theme;

import com.cburch.logisim.prefs.AppPreferences;
import java.awt.Color;
import java.util.prefs.Preferences;

public class ColorSchemeRegistry {

  public static final String SCHEME_MODERN = "MODERN";
  public static final String SCHEME_CLASSIC = "CLASSIC";
  public static final String SCHEME_COLORBLIND = "COLORBLIND";
  public static final String SCHEME_CUSTOM = "CUSTOM";

  public static final String DISPLAY_MODERN = "Logisim Modern (default)";
  public static final String DISPLAY_CLASSIC = "Logisim Classic";
  public static final String DISPLAY_COLORBLIND = "Color-blind";
  public static final String DISPLAY_CUSTOM = "Custom";

  // --- 1. MODERN SCHEME ---
  public static final ColorScheme MODERN_LIGHT =
      new ColorScheme(
          "Modern Light",
          ColorScheme.fromHex("#16A05A"), // True (1)
          ColorScheme.fromHex("#0B4A2C"), // False (0)
          ColorScheme.fromHex("#1C71D8"), // Unknown
          ColorScheme.fromHex("#C01C28"), // Error
          ColorScheme.fromHex("#B35C00"), // Mismatch
          ColorScheme.fromHex("#4A5361"), // Bus
          ColorScheme.fromHex("#8A9099"), // Unconnected
          ColorScheme.fromHex("#26282C"), // Wire/component stroke
          ColorScheme.fromHex("#550000"), // Caption
          ColorScheme.fromHex("#FFFF00"), // Highlight
          ColorScheme.fromHex("#FFE6D2"), // Background
          ColorScheme.fromHex("#D81B60")  // Clock frequency
      );

  public static final ColorScheme MODERN_DARK =
      new ColorScheme(
          "Modern Dark",
          ColorScheme.fromHex("#55F08A"), // True (1)
          ColorScheme.fromHex("#1B8458"), // False (0)
          ColorScheme.fromHex("#5B9BFF"), // Unknown
          ColorScheme.fromHex("#FF6B5E"), // Error
          ColorScheme.fromHex("#F0A030"), // Mismatch
          ColorScheme.fromHex("#9FA9B8"), // Bus
          ColorScheme.fromHex("#7C828C"), // Unconnected
          ColorScheme.fromHex("#E6E6E6"), // Wire/component stroke
          ColorScheme.fromHex("#FFB4A9"), // Caption
          ColorScheme.fromHex("#FFE082"), // Highlight
          ColorScheme.fromHex("#3C2A20"), // Background
          ColorScheme.fromHex("#FF70C0")  // Clock frequency
      );

  // --- 2. CLASSIC UPSTREAM SCHEME ---
  public static final ColorScheme CLASSIC =
      new ColorScheme(
          "Classic",
          new Color(0x0000D200),
          new Color(0x00006400),
          new Color(0x002828FF),
          new Color(0x00C00000),
          new Color(0x00FF7B00),
          new Color(0x00000000), // Black bus
          new Color(0x00808080),
          new Color(0x00FE00FF),
          new Color(0x00550000),
          new Color(0x00FFFF00),
          new Color(0x00FFE6D2),
          new Color(0x00FF00B4)
      );

  // --- 3. COLOR-BLIND SCHEME ---
  public static final ColorScheme COLORBLIND =
      new ColorScheme(
          "Color-blind",
          new Color(0x00F4EB42),
          new Color(0x00203BE8),
          new Color(0x0001BC9D),
          new Color(0x00C10000),
          new Color(0x00C413DB),
          new Color(0x00000001), // Custom bus
          new Color(0x00818181),
          new Color(0x00BBBBBB),
          new Color(0x00560000),
          new Color(0x00FFFE00),
          new Color(0x00FFE6D2),
          new Color(0x00FF00B4)
      );

  public static ColorScheme getCustomScheme() {
    return new ColorScheme(
        "Custom",
        new Color(AppPreferences.TRUE_COLOR.get()),
        new Color(AppPreferences.FALSE_COLOR.get()),
        new Color(AppPreferences.UNKNOWN_COLOR.get()),
        new Color(AppPreferences.ERROR_COLOR.get()),
        new Color(AppPreferences.WIDTH_ERROR_COLOR.get()),
        new Color(AppPreferences.BUS_COLOR.get()),
        new Color(AppPreferences.NIL_COLOR.get()),
        new Color(AppPreferences.STROKE_COLOR.get()),
        new Color(AppPreferences.WIDTH_ERROR_CAPTION_COLOR.get()),
        new Color(AppPreferences.WIDTH_ERROR_HIGHLIGHT_COLOR.get()),
        new Color(AppPreferences.WIDTH_ERROR_BACKGROUND_COLOR.get()),
        new Color(AppPreferences.CLOCK_FREQUENCY_COLOR.get())
    );
  }

  public static ColorScheme getColorScheme(String schemeId, boolean dark) {
    if (SCHEME_CLASSIC.equalsIgnoreCase(schemeId)) {
      return CLASSIC;
    }
    if (SCHEME_COLORBLIND.equalsIgnoreCase(schemeId)) {
      return COLORBLIND;
    }
    if (SCHEME_CUSTOM.equalsIgnoreCase(schemeId)) {
      return getCustomScheme();
    }
    // Default to MODERN
    return dark ? MODERN_DARK : MODERN_LIGHT;
  }

  public static ColorScheme getActiveScheme() {
    String schemeId = AppPreferences.COLOR_SCHEME.get();
    boolean dark = AppPreferences.isDarkTheme(AppPreferences.LookAndFeel.get());
    return getColorScheme(schemeId, dark);
  }

  /**
   * Idempotent migration:
   * If COLOR_SCHEME is unset:
   * - If stored colors match classic defaults or are unset -> MODERN
   * - If they match colorblind set -> COLORBLIND
   * - Otherwise -> CUSTOM (preserves user custom colors)
   */
  public static synchronized void performMigrationIfNeeded() {
    Preferences prefs = AppPreferences.getPrefs();
    String existing = prefs.get("colorScheme", null);
    if (existing != null && !existing.isBlank()) {
      return; // Already migrated
    }

    boolean isColorBlind =
        (prefs.getInt("SimTrueColor", 0) == 0xF4EB42)
            && (prefs.getInt("SimFalseColor", 0) == 0x203BE8);

    boolean isClassicOrUnset =
        (prefs.getInt("SimTrueColor", 0x0000D200) == 0x0000D200
            || prefs.getInt("SimTrueColor", 0x0000D300) == 0x0000D300)
            && (prefs.getInt("SimFalseColor", 0x00006400) == 0x00006400
            || prefs.getInt("SimFalseColor", 0x00006500) == 0x00006500)
            && (prefs.getInt("SimUnknownColor", 0x002828FF) == 0x002828FF
            || prefs.getInt("SimUnknownColor", 0x002827FF) == 0x002827FF);

    if (isColorBlind) {
      AppPreferences.COLOR_SCHEME.set(SCHEME_COLORBLIND);
    } else if (isClassicOrUnset) {
      AppPreferences.COLOR_SCHEME.set(SCHEME_MODERN);
    } else {
      AppPreferences.COLOR_SCHEME.set(SCHEME_CUSTOM);
    }
  }

  public static void copySchemeToCustom(ColorScheme scheme) {
    AppPreferences.TRUE_COLOR.set(scheme.trueColor().getRGB() & 0xFFFFFF);
    AppPreferences.FALSE_COLOR.set(scheme.falseColor().getRGB() & 0xFFFFFF);
    AppPreferences.UNKNOWN_COLOR.set(scheme.unknownColor().getRGB() & 0xFFFFFF);
    AppPreferences.ERROR_COLOR.set(scheme.errorColor().getRGB() & 0xFFFFFF);
    AppPreferences.WIDTH_ERROR_COLOR.set(scheme.mismatchColor().getRGB() & 0xFFFFFF);
    AppPreferences.BUS_COLOR.set(scheme.busColor().getRGB() & 0xFFFFFF);
    AppPreferences.NIL_COLOR.set(scheme.unconnectedColor().getRGB() & 0xFFFFFF);
    AppPreferences.STROKE_COLOR.set(scheme.strokeColor().getRGB() & 0xFFFFFF);
    AppPreferences.WIDTH_ERROR_CAPTION_COLOR.set(scheme.widthErrorCaptionColor().getRGB() & 0xFFFFFF);
    AppPreferences.WIDTH_ERROR_HIGHLIGHT_COLOR.set(scheme.widthErrorHighlightColor().getRGB() & 0xFFFFFF);
    AppPreferences.WIDTH_ERROR_BACKGROUND_COLOR.set(scheme.widthErrorBackgroundColor().getRGB() & 0xFFFFFF);
    AppPreferences.CLOCK_FREQUENCY_COLOR.set(scheme.clockFrequencyColor().getRGB() & 0xFFFFFF);
  }
}
