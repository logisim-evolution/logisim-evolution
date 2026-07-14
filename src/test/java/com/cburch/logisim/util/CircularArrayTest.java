/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

public class CircularArrayTest {

  /** Tests if CircularArray works as a queue adding at end. */
  @Test
  public void testCircularArrayAddLastRemoveFirst() {
    final var ca = new CircularArray<Integer>(8);
    assertTrue(ca.isEmpty());
    for (int i = 0; i < 16; i++) {
      ca.addLast(i);
    }
    assertFalse(ca.isEmpty());
    assertEquals(16, ca.size());
    assertEquals(16, ca.capacity());
    for (int i = 0; i < ca.size(); i++) {
      assertEquals(i, ca.get(i));
    }
    for (int i = 0; i < 4; i++) {
      int result = ca.removeFirst();
      assertEquals(i, result);
    }
    assertEquals(12, ca.size());
    for (int i = 0; i < ca.size(); i++) {
      assertEquals(i + 4, ca.get(i));
    }
    for (int i = 16; i < 32; i++) {
      ca.addLast(i);
    }
    assertEquals(28, ca.size());
    for (int i = 0; i < ca.size(); i++) {
      assertEquals(i + 4, ca.get(i));
    }
    ca.set(3, 300);
    ca.set(5, 500);
    ca.set(6, 600);
    assertEquals(300, ca.get(3));
    assertEquals(500, ca.get(5));
    assertEquals(600, ca.get(6));
    ca.clear();
    assertEquals(0, ca.size());
    ca.addLast(1);
    assertEquals(1, ca.size());
    int val = ca.get(0);
    assertEquals(1, val);
    val = ca.removeFirst();
    assertEquals(1, val);
  }

  /** Tests if CircularArray works as a queue adding at beginning. */
  @Test
  public void testCircularArrayAddFirstRemoveLast() {
    final var ca = new CircularArray<Integer>(8);
    assertTrue(ca.isEmpty());
    for (int i = 0; i < 16; i++) {
      ca.addFirst(i);
    }
    assertFalse(ca.isEmpty());
    assertEquals(16, ca.size());
    assertEquals(16, ca.capacity());
    for (int i = 0; i < ca.size(); i++) {
      assertEquals(15 - i, ca.get(i));
    }
    for (int i = 0; i < 4; i++) {
      int result = ca.removeLast();
      assertEquals(i, result);
    }
    assertEquals(12, ca.size());
    for (int i = 0; i < ca.size(); i++) {
      assertEquals(15 - i, ca.get(i));
    }
    for (int i = 16; i < 32; i++) {
      ca.addFirst(i);
    }
    assertEquals(28, ca.size());
    for (int i = 0; i < ca.size(); i++) {
      assertEquals(31 - i, ca.get(i));
    }
    ca.set(3, 300);
    ca.set(5, 500);
    ca.set(6, 600);
    assertEquals(300, ca.get(3));
    assertEquals(500, ca.get(5));
    assertEquals(600, ca.get(6));
    ca.clear();
    assertEquals(0, ca.size());
    ca.addFirst(1);
    assertEquals(1, ca.size());
    int val = ca.get(0);
    assertEquals(1, val);
    val = ca.removeLast();
    assertEquals(1, val);
  }
}
