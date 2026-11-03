/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class MainTest {
  // A scratch node, so the real Logisim preferences are never touched.
  private final Preferences node = Preferences.userRoot().node("logisim-test-" + System.nanoTime());

  @AfterEach
  void removeNode() throws BackingStoreException {
    node.removeNode();
  }

  @Test
  void emptyNodeIsNewInstall() {
    assertTrue(Main.isNewInstall(node));
  }

  @Test
  void nodeWithOneKeyIsExistingInstall() {
    node.put("someKey", "value");
    assertFalse(Main.isNewInstall(node));
  }

  @Test
  void checkDoesNotWrite() throws BackingStoreException {
    Main.isNewInstall(node);
    assertEquals(0, node.keys().length);
  }

  @Test
  void unreadableNodeIsExistingInstall() throws BackingStoreException {
    final var broken = mock(Preferences.class);
    when(broken.keys()).thenThrow(new BackingStoreException("test"));
    assertFalse(Main.isNewInstall(broken));
  }
}
