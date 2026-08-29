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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.KeyStroke;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class PrefMonitorKeyStrokeTest {
  private final String preferenceName = "testHotkey" + UUID.randomUUID();

  @AfterEach
  void removeTestPreference() {
    AppPreferences.getPrefs().remove(preferenceName);
  }

  @Test
  void setImmediatelyUpdatesValueAndNotifiesListenersOnce() {
    final var monitor =
        new PrefMonitorKeyStroke(
            preferenceName, KeyEvent.VK_1, InputEvent.CTRL_DOWN_MASK, true, true);
    final var newValue =
        KeyStroke.getKeyStroke(KeyEvent.VK_F1, InputEvent.CTRL_DOWN_MASK);
    final var notifications = new AtomicInteger();
    final var notificationThread = new AtomicReference<Thread>();
    monitor.addPropertyChangeListener(
        event -> {
          notifications.incrementAndGet();
          notificationThread.compareAndSet(null, Thread.currentThread());
        });

    monitor.set(newValue);

    assertEquals(newValue, monitor.get());
    assertEquals(1, notifications.get());
    assertSame(Thread.currentThread(), notificationThread.get());

    monitor.preferenceChange(
        new java.util.prefs.PreferenceChangeEvent(
            AppPreferences.getPrefs(), preferenceName, null));
    assertEquals(1, notifications.get());
  }

  @Test
  void supportsAnUnassignedDefaultAndClearingAnAssignment() {
    final var monitor =
        new PrefMonitorKeyStroke(preferenceName, null, true, true);
    final var assigned =
        KeyStroke.getKeyStroke(KeyEvent.VK_F12, InputEvent.CTRL_DOWN_MASK);
    final var notifications = new AtomicInteger();
    monitor.addPropertyChangeListener(event -> notifications.incrementAndGet());

    assertNull(monitor.get());
    assertNull(monitor.getWithMask(InputEvent.SHIFT_DOWN_MASK));
    assertEquals("", monitor.getDisplayString());
    assertFalse(monitor.compare(KeyEvent.VK_F12, InputEvent.CTRL_DOWN_MASK));

    monitor.set(assigned);
    assertEquals(assigned, monitor.get());
    assertEquals(1, notifications.get());

    monitor.set((KeyStroke) null);
    assertNull(monitor.get());
    assertEquals("", monitor.getDisplayString());
    assertEquals(2, notifications.get());

    monitor.set((KeyStroke) null);
    assertEquals(2, notifications.get());
  }
}
