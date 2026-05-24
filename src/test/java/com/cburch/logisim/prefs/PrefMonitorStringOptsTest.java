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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class PrefMonitorStringOptsTest {

  private String preferenceName;

  @AfterEach
  void removeTestPreference() {
    if (preferenceName != null) {
      AppPreferences.getPrefs().remove(preferenceName);
    }
  }

  @Test
  void setUpdatesInMemoryValueSynchronously() {
    preferenceName = "testStringOption-" + System.nanoTime();
    final var monitor = new PrefMonitorStringOpts(preferenceName, new String[] {"first", "second"}, "first");

    monitor.set("second");

    assertEquals("second", monitor.get());
  }
}
