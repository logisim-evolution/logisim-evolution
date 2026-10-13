/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.prefs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.prefs.Preferences;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class ThemeColorPreferenceTest {
  private final Preferences preferences = AppPreferences.getPrefs();
  private final String originalLookAndFeel = AppPreferences.LookAndFeel.get();
  private final String lightTheme = "TestLightTheme-" + System.nanoTime();
  private final String darkTheme = "TestDarkTheme-" + System.nanoTime();

  @AfterEach
  void restorePreferences() throws Exception {
    AppPreferences.LookAndFeel.set(originalLookAndFeel);
    AppPreferences.applyThemeColors();
    removeThemePreferences(lightTheme);
    removeThemePreferences(darkTheme);
    preferences.flush();
  }

  @Test
  void customColorsStayWithTheirTheme() throws Exception {
    final var lightCustomColor = 0x00112233;
    final var darkCustomColor = 0x00445566;

    AppPreferences.LookAndFeel.set(lightTheme);
    assertEquals(AppPreferences.DEFAULT_GRID_BG_COLOR, AppPreferences.GRID_BG_COLOR.get());
    AppPreferences.GRID_BG_COLOR.set(lightCustomColor);

    AppPreferences.LookAndFeel.set(darkTheme);
    assertEquals(AppPreferences.DARK_GRID_BG_COLOR, AppPreferences.GRID_BG_COLOR.get());
    AppPreferences.GRID_BG_COLOR.set(darkCustomColor);

    AppPreferences.LookAndFeel.set(lightTheme);
    assertEquals(lightCustomColor, AppPreferences.GRID_BG_COLOR.get());
    AppPreferences.LookAndFeel.set(darkTheme);
    assertEquals(darkCustomColor, AppPreferences.GRID_BG_COLOR.get());

    assertEquals(AppPreferences.DEFAULT_GRID_BG_COLOR,
        preferences.getInt(defaultKey(lightTheme), Integer.MIN_VALUE));
    assertEquals(AppPreferences.DARK_GRID_BG_COLOR,
        preferences.getInt(defaultKey(darkTheme), Integer.MIN_VALUE));
  }

  @Test
  void resetOnlyChangesTheCurrentTheme() throws Exception {
    final var lightCustomColor = 0x00112233;
    final var darkCustomColor = 0x00445566;

    AppPreferences.LookAndFeel.set(lightTheme);
    AppPreferences.GRID_BG_COLOR.set(lightCustomColor);
    AppPreferences.LookAndFeel.set(darkTheme);
    AppPreferences.GRID_BG_COLOR.set(darkCustomColor);

    AppPreferences.setDefaultGridColors();
    assertEquals(AppPreferences.DARK_GRID_BG_COLOR, AppPreferences.GRID_BG_COLOR.get());

    AppPreferences.LookAndFeel.set(lightTheme);
    assertEquals(lightCustomColor, AppPreferences.GRID_BG_COLOR.get());
  }

  @Test
  void themeDefaultsAreStoredInPreferences() throws Exception {
    AppPreferences.LookAndFeel.set(lightTheme);

    assertNotNull(preferences.get(defaultKey(lightTheme), null));
  }

  @ParameterizedTest
  @MethodSource("simulationAnnotationColors")
  void simulationAnnotationColorsStayWithTheirTheme(PrefMonitor<Integer> color, int defaultColor) {
    final var lightCustomColor = 0x00112233;
    final var darkCustomColor = 0x00445566;

    AppPreferences.LookAndFeel.set(lightTheme);
    assertEquals(defaultColor, color.get());
    color.set(lightCustomColor);
    AppPreferences.LookAndFeel.set(darkTheme);
    assertEquals(defaultColor, color.get());
    color.set(darkCustomColor);

    assertEquals(lightCustomColor, preferences.getInt(
        "themeColors." + lightTheme + "." + color.getIdentifier(), Integer.MIN_VALUE));
    assertEquals(darkCustomColor, preferences.getInt(
        "themeColors." + darkTheme + "." + color.getIdentifier(), Integer.MIN_VALUE));
    assertEquals(defaultColor, preferences.getInt(
        "themeColors." + darkTheme + ".defaults." + color.getIdentifier(), Integer.MIN_VALUE));

    AppPreferences.LookAndFeel.set(lightTheme);
    assertEquals(lightCustomColor, color.get());
    AppPreferences.LookAndFeel.set(darkTheme);
    assertEquals(darkCustomColor, color.get());
    AppPreferences.setDefaultGridColors();
    assertEquals(defaultColor, color.get());
    AppPreferences.LookAndFeel.set(lightTheme);
    assertEquals(lightCustomColor, color.get());
  }

  private static Stream<Arguments> simulationAnnotationColors() {
    return Stream.of(
        Arguments.of(AppPreferences.WIDTH_ERROR_COLOR, AppPreferences.DEFAULT_WIDTH_ERROR_COLOR),
        Arguments.of(AppPreferences.WIDTH_ERROR_CAPTION_COLOR,
            AppPreferences.DEFAULT_WIDTH_ERROR_CAPTION_COLOR),
        Arguments.of(AppPreferences.WIDTH_ERROR_HIGHLIGHT_COLOR,
            AppPreferences.DEFAULT_WIDTH_ERROR_HIGHLIGHT_COLOR),
        Arguments.of(AppPreferences.WIDTH_ERROR_BACKGROUND_COLOR,
            AppPreferences.DEFAULT_WIDTH_ERROR_BACKGROUND_COLOR),
        Arguments.of(AppPreferences.CLOCK_FREQUENCY_COLOR,
            AppPreferences.DEFAULT_CLOCK_FREQUENCY_COLOR));
  }

  private String defaultKey(String theme) {
    return "themeColors." + theme + ".defaults." + AppPreferences.GRID_BG_COLOR.getIdentifier();
  }

  private void removeThemePreferences(String theme) throws Exception {
    final var prefix = "themeColors." + theme + ".";
    for (final var key : preferences.keys()) {
      if (key.startsWith(prefix)) {
        preferences.remove(key);
      }
    }
  }
}
