/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.prefs;

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import org.junit.jupiter.api.Test;

class AppPreferencesHotkeyTest {
  @Test
  void rejectsSwingShowToolTipShortcut() {
    final var conflict =
        AppPreferences.hotkeyCheckConflict(
            "hotkeyToolSelect1", KeyEvent.VK_F1, InputEvent.CTRL_DOWN_MASK);

    assertFalse(conflict.isEmpty());
  }
}
