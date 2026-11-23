/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.util;

import static com.cburch.logisim.util.SystemTheme.Result.DARK;
import static com.cburch.logisim.util.SystemTheme.Result.LIGHT;
import static com.cburch.logisim.util.SystemTheme.Result.UNKNOWN;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.Test;

class SystemThemeTest {
  @Test
  void macDark() {
    assertEquals(DARK, SystemTheme.parseMac("Dark\n"));
    assertEquals(DARK, SystemTheme.parseMac("dark"));
  }

  @Test
  void macMissingKeyIsLight() {
    // The command ran and exited with an error, because light mode has no key.
    assertEquals(LIGHT, SystemTheme.parseMac(""));
    assertEquals(LIGHT, SystemTheme.parseMac("\n"));
  }

  @Test
  void macUnknown() {
    assertEquals(UNKNOWN, SystemTheme.parseMac(null));
    assertEquals(UNKNOWN, SystemTheme.parseMac("Light\n"));
    assertEquals(UNKNOWN, SystemTheme.parseMac("not a theme"));
  }

  @Test
  void windowsDark() {
    final var output = "\r\nHKEY_CURRENT_USER\\Software\\Microsoft\\Windows\\CurrentVersion"
        + "\\Themes\\Personalize\r\n    AppsUseLightTheme    REG_DWORD    0x0\r\n\r\n";
    assertEquals(DARK, SystemTheme.parseWindows(output));
  }

  @Test
  void windowsLight() {
    assertEquals(LIGHT,
        SystemTheme.parseWindows("    AppsUseLightTheme    REG_DWORD    0x1\r\n"));
    // The command ran but printed nothing, as on Windows versions without the value.
    assertEquals(LIGHT, SystemTheme.parseWindows(""));
  }

  @Test
  void windowsUnknown() {
    assertEquals(UNKNOWN, SystemTheme.parseWindows(null));
    assertEquals(UNKNOWN,
        SystemTheme.parseWindows("ERROR: The system was unable to find the key."));
    assertEquals(UNKNOWN, SystemTheme.parseWindows("not a theme"));
  }

  @Test
  void windowsValueTooLongDoesNotThrow() {
    assertEquals(UNKNOWN,
        SystemTheme.parseWindows("AppsUseLightTheme REG_DWORD 0xFFFFFFFFFFFFFFFFF"));
    assertEquals(UNKNOWN,
        SystemTheme.parseWindows("AppsUseLightTheme REG_DWORD 0x000000000"));
  }

  @Test
  void linuxPortalValues() {
    assertEquals(DARK, SystemTheme.parseLinux("(<<uint32 1>>,)\n"));
    assertEquals(LIGHT, SystemTheme.parseLinux("(<<uint32 2>>,)\n"));
    assertEquals(LIGHT, SystemTheme.parseLinux("(<<uint32 0>>,)\n"));
  }

  @Test
  void linuxPortalUnknown() {
    assertEquals(UNKNOWN, SystemTheme.parseLinux(null));
    assertEquals(UNKNOWN, SystemTheme.parseLinux(""));
    assertEquals(UNKNOWN, SystemTheme.parseLinux("not a theme"));
    assertEquals(UNKNOWN, SystemTheme.parseLinux("(<<uint32 7>>,)\n"));
    assertEquals(UNKNOWN, SystemTheme.parseLinux("(<<uint32 10>>,)\n"));
    assertEquals(UNKNOWN, SystemTheme.parseLinux("(<<uint32 99999999999999999999>>,)\n"));
  }

  @Test
  void gsettingsValues() {
    assertEquals(DARK, SystemTheme.parseGsettings("'prefer-dark'\n"));
    assertEquals(DARK, SystemTheme.parseGsettings("prefer-dark"));
    assertEquals(LIGHT, SystemTheme.parseGsettings("'prefer-light'\n"));
    assertEquals(LIGHT, SystemTheme.parseGsettings("'default'\n"));
  }

  @Test
  void gsettingsUnknown() {
    assertEquals(UNKNOWN, SystemTheme.parseGsettings(null));
    assertEquals(UNKNOWN, SystemTheme.parseGsettings(""));
    assertEquals(UNKNOWN, SystemTheme.parseGsettings("''"));
    assertEquals(UNKNOWN, SystemTheme.parseGsettings("not a theme"));
    assertEquals(UNKNOWN, SystemTheme.parseGsettings("'PREFER-DARK'"));
  }

  @Test
  void portalAnswerSkipsGsettings() {
    assertEquals(DARK, SystemTheme.linux("(<<uint32 1>>,)", () -> fail("should not be called")));
    assertEquals(LIGHT, SystemTheme.linux("(<<uint32 2>>,)", () -> fail("should not be called")));
  }

  @Test
  void gsettingsIsUsedWhenPortalFails() {
    assertEquals(DARK, SystemTheme.linux(null, () -> "'prefer-dark'\n"));
    assertEquals(LIGHT, SystemTheme.linux("", () -> "'default'\n"));
    assertEquals(DARK, SystemTheme.linux("(<<uint32 7>>,)", () -> "'prefer-dark'\n"));
  }

  @Test
  void unknownWhenBothFail() {
    assertEquals(UNKNOWN, SystemTheme.linux(null, () -> null));
    assertEquals(UNKNOWN, SystemTheme.linux("not a theme", () -> ""));
  }
}
