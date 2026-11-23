/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.gui.prefs;

import static com.cburch.logisim.util.SystemTheme.Result.DARK;
import static com.cburch.logisim.util.SystemTheme.Result.LIGHT;
import static com.cburch.logisim.util.SystemTheme.Result.UNKNOWN;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.cburch.logisim.util.SystemTheme;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class SystemThemeWatcherTest {
  private static final String DEFAULT_LIGHT = "com.formdev.flatlaf.FlatIntelliJLaf";
  private static final String DEFAULT_DARK = "com.formdev.flatlaf.FlatDarculaLaf";
  private static final String OWN_LIGHT = "com.formdev.flatlaf.FlatLightLaf";
  private static final String OWN_DARK = "com.formdev.flatlaf.themes.FlatMacDarkLaf";

  // A scratch node, so the real Logisim preferences are never touched.
  private final Preferences node = Preferences.userRoot().node("logisim-test-" + System.nanoTime());

  @AfterEach
  void removeNode() throws BackingStoreException {
    node.removeNode();
  }

  // What happens when the user ticks the box: the current theme is kept for its mode, then
  // the theme for the system mode is chosen.
  private String tick(String current, boolean currentIsDark, SystemTheme.Result system) {
    SystemThemeWatcher.remember(node, current, currentIsDark);
    return SystemThemeWatcher.lookAndFeelFor(node, system, current);
  }

  @Test
  void defaultsWhenNothingIsStored() {
    assertEquals(DEFAULT_LIGHT, SystemThemeWatcher.rememberedLookAndFeel(node, false));
    assertEquals(DEFAULT_DARK, SystemThemeWatcher.rememberedLookAndFeel(node, true));
  }

  @Test
  void darkThemeGoesToDarkSlot() {
    SystemThemeWatcher.remember(node, OWN_DARK, true);
    assertEquals(OWN_DARK, SystemThemeWatcher.rememberedLookAndFeel(node, true));
    assertEquals(DEFAULT_LIGHT, SystemThemeWatcher.rememberedLookAndFeel(node, false));
  }

  @Test
  void lightThemeGoesToLightSlot() {
    SystemThemeWatcher.remember(node, OWN_LIGHT, false);
    assertEquals(OWN_LIGHT, SystemThemeWatcher.rememberedLookAndFeel(node, false));
    assertEquals(DEFAULT_DARK, SystemThemeWatcher.rememberedLookAndFeel(node, true));
  }

  @Test
  void recordingOneSlotLeavesTheOtherAlone() {
    SystemThemeWatcher.remember(node, OWN_LIGHT, false);
    SystemThemeWatcher.remember(node, OWN_DARK, true);
    SystemThemeWatcher.remember(node, DEFAULT_LIGHT, false);
    assertEquals(DEFAULT_LIGHT, SystemThemeWatcher.rememberedLookAndFeel(node, false));
    assertEquals(OWN_DARK, SystemThemeWatcher.rememberedLookAndFeel(node, true));
  }

  @Test
  void systemModeSelectsTheRememberedSlot() {
    SystemThemeWatcher.remember(node, OWN_LIGHT, false);
    SystemThemeWatcher.remember(node, OWN_DARK, true);
    assertEquals(OWN_DARK, SystemThemeWatcher.lookAndFeelFor(node, DARK, OWN_LIGHT));
    assertEquals(OWN_LIGHT, SystemThemeWatcher.lookAndFeelFor(node, LIGHT, OWN_DARK));
  }

  @Test
  void unknownSystemThemeKeepsTheCurrentOne() {
    assertEquals(OWN_DARK, SystemThemeWatcher.lookAndFeelFor(node, UNKNOWN, OWN_DARK));
  }

  @Test
  void automaticSwitchDoesNotWriteToTheNode() throws BackingStoreException {
    SystemThemeWatcher.lookAndFeelFor(node, DARK, OWN_LIGHT);
    SystemThemeWatcher.lookAndFeelFor(node, LIGHT, OWN_DARK);
    SystemThemeWatcher.lookAndFeelFor(node, UNKNOWN, OWN_DARK);
    assertEquals(0, node.keys().length);
  }

  @Test
  void ownThemesSurviveTickSwitchUntickAndTickAgain() {
    // The user picked their own themes in the combo box.
    SystemThemeWatcher.remember(node, OWN_LIGHT, false);
    SystemThemeWatcher.remember(node, OWN_DARK, true);

    var current = tick(OWN_LIGHT, false, DARK);
    assertEquals(OWN_DARK, current);

    // Automatic switches while the box is ticked.
    current = SystemThemeWatcher.lookAndFeelFor(node, LIGHT, current);
    assertEquals(OWN_LIGHT, current);
    current = SystemThemeWatcher.lookAndFeelFor(node, DARK, current);
    assertEquals(OWN_DARK, current);
    current = SystemThemeWatcher.lookAndFeelFor(node, LIGHT, current);

    // Unticking records nothing, and the theme stays as it is. Ticking again follows the system.
    assertEquals(OWN_LIGHT, current);
    current = tick(current, false, DARK);
    assertEquals(OWN_DARK, current);
    assertEquals(OWN_LIGHT, SystemThemeWatcher.rememberedLookAndFeel(node, false));
    assertEquals(OWN_DARK, SystemThemeWatcher.rememberedLookAndFeel(node, true));
  }

  @Test
  void firstTickKeepsTheCurrentThemeOfAnExistingUser() {
    // The user never used the combo box in this version and is on a dark theme.
    var current = tick(OWN_DARK, true, LIGHT);
    assertEquals(DEFAULT_LIGHT, current);
    current = SystemThemeWatcher.lookAndFeelFor(node, DARK, current);
    assertEquals(OWN_DARK, current);
  }
}
