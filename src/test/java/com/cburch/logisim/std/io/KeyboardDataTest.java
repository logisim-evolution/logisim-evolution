/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.std.io;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class KeyboardDataTest {

  @Test
  void insertHandlesBeginningMiddleEndAndCapacityBoundary() {
    final var data = keyboardData("ac", 3);

    assertTrue(data.setCursor(1));
    assertTrue(data.insert('b'));
    assertEquals("abc", data.toString());
    assertEquals(2, data.getCursorPosition());
    assertFalse(data.insert('x'));
    assertEquals("abc", data.toString());

    data.clear();
    assertTrue(data.insert('b'));
    assertTrue(data.setCursor(0));
    assertTrue(data.insert('a'));
    assertTrue(data.setCursor(2));
    assertTrue(data.insert('c'));
    assertEquals("abc", data.toString());
  }

  @Test
  void deleteHandlesBeginningMiddleEndAndEmptyTailCopy() {
    final var data = keyboardData("abcd", 4);

    assertTrue(data.setCursor(1));
    assertTrue(data.delete());
    assertEquals("acd", data.toString());

    assertTrue(data.setCursor(0));
    assertTrue(data.delete());
    assertEquals("cd", data.toString());

    assertTrue(data.setCursor(1));
    assertTrue(data.delete());
    assertEquals("c", data.toString());
    assertFalse(data.delete());
    assertEquals("c", data.toString());
  }

  @Test
  void dequeueHandlesEmptySingleAndMultipleCharacters() {
    final var data = keyboardData("abc", 3);
    assertTrue(data.setCursor(2));

    assertEquals('a', data.dequeue());
    assertEquals("bc", data.toString());
    assertEquals(1, data.getCursorPosition());

    assertEquals('b', data.dequeue());
    assertEquals('c', data.dequeue());
    assertEquals("", data.toString());
    assertEquals(0, data.getCursorPosition());
    assertEquals('\0', data.dequeue());
  }

  private static KeyboardData keyboardData(String value, int capacity) {
    final var data = new KeyboardData(capacity);
    for (final var character : value.toCharArray()) assertTrue(data.insert(character));
    return data;
  }
}
