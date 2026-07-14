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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
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

  @Test
  void setFiresPropertyChangeSynchronously() {
    preferenceName = "testStringOption-" + System.nanoTime();
    final var monitor =
        new PrefMonitorStringOpts(
            preferenceName, new String[] {"first", "second"}, "first");
    final var events = new ArrayList<PropertyChangeEvent>();
    final PropertyChangeListener listener = events::add;
    monitor.addPropertyChangeListener(listener);

    monitor.set("second");

    assertEquals("second", monitor.get());
    assertEquals(1, events.size());
    final var event = events.get(0);
    assertEquals(preferenceName, event.getPropertyName());
    assertEquals("first", event.getOldValue());
    assertEquals("second", event.getNewValue());
  }

  @Test
  void setDoesNotNotifyWhenNormalizedValueIsUnchanged() {
    preferenceName = "testStringOption-" + System.nanoTime();
    final var monitor =
        new PrefMonitorStringOpts(
            preferenceName, new String[] {"first", "second"}, "first");
    final var events = new ArrayList<PropertyChangeEvent>();
    final PropertyChangeListener listener = events::add;
    monitor.addPropertyChangeListener(listener);

    monitor.set("invalid");

    assertEquals("first", monitor.get());
    assertTrue(events.isEmpty());
  }
}
