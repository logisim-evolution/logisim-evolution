/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.std.base;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.prefs.AppPreferences;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class TextColorAttributeTest {
  private final String originalTheme = AppPreferences.LookAndFeel.get();

  @AfterEach
  void restoreTheme() throws Exception {
    SwingUtilities.invokeAndWait(() -> AppPreferences.LookAndFeel.set(originalTheme));
  }

  @Test
  void themeChangesDisplayButNotFileRepresentation() {
    final var stored = Text.ATTR_COLOR.parse("#0000ff80");
    AppPreferences.LookAndFeel.set("TestLight");
    assertEquals("#0000ff80", Text.ATTR_COLOR.toDisplayString(stored));

    AppPreferences.LookAndFeel.set("TestDark");
    assertEquals("#dadaff80", Text.ATTR_COLOR.toDisplayString(stored));
    assertEquals("#0000ff80", Text.ATTR_COLOR.toStandardString(stored));
    assertEquals(stored, Text.ATTR_COLOR.parse("#0000ff80"));
    assertEquals("#0000ff80", StdAttr.LABEL_COLOR.toDisplayString(stored));
  }
}
