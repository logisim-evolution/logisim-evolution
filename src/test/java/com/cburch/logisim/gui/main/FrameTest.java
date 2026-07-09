/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.gui.main;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class FrameTest {
  private static final double FRACTION_DELTA = 1.0e-4;

  @Test
  void unfinishedFrameDoesNotReportExplorerVisibleDuringMenuConstruction() throws Exception {
    final var frame = (Frame) allocateWithoutConstructor(Frame.class);

    assertFalse(frame.isExplorerVisible());
  }

  @Test
  void restoresDefaultWhenVhdlConsoleSplitWouldCollapseEditor() {
    assertEquals(0.75, Frame.sanitizeVhdlConsoleSplitFraction(null), FRACTION_DELTA);
    assertEquals(0.75, Frame.sanitizeVhdlConsoleSplitFraction(Double.NaN), FRACTION_DELTA);
    assertEquals(
        0.75, Frame.sanitizeVhdlConsoleSplitFraction(Double.NEGATIVE_INFINITY), FRACTION_DELTA);
    assertEquals(0.75, Frame.sanitizeVhdlConsoleSplitFraction(0.0), FRACTION_DELTA);
    assertEquals(0.75, Frame.sanitizeVhdlConsoleSplitFraction(0.04), FRACTION_DELTA);
    assertEquals(0.05, Frame.sanitizeVhdlConsoleSplitFraction(0.05), FRACTION_DELTA);
  }

  @Test
  void restoresDefaultWhenVhdlConsoleSplitWouldHideConsole() {
    assertEquals(0.75, Frame.sanitizeVhdlConsoleSplitFraction(1.0), FRACTION_DELTA);
    assertEquals(0.75, Frame.sanitizeVhdlConsoleSplitFraction(0.99), FRACTION_DELTA);
    assertEquals(
        0.75, Frame.sanitizeVhdlConsoleSplitFraction(Double.POSITIVE_INFINITY), FRACTION_DELTA);
  }

  @Test
  void preservesUsableVhdlConsoleSplit() {
    assertEquals(0.25, Frame.sanitizeVhdlConsoleSplitFraction(0.25), FRACTION_DELTA);
    assertEquals(0.75, Frame.sanitizeVhdlConsoleSplitFraction(0.75), FRACTION_DELTA);
    assertEquals(0.95, Frame.sanitizeVhdlConsoleSplitFraction(0.95), FRACTION_DELTA);
  }

  @Test
  void rejectsUnusableVhdlConsoleSplitForPersistence() {
    assertFalse(Frame.isUsableVhdlConsoleSplitFraction(Double.NaN));
    assertFalse(Frame.isUsableVhdlConsoleSplitFraction(Double.NEGATIVE_INFINITY));
    assertFalse(Frame.isUsableVhdlConsoleSplitFraction(0.0));
    assertFalse(Frame.isUsableVhdlConsoleSplitFraction(0.04));
    assertTrue(Frame.isUsableVhdlConsoleSplitFraction(0.05));
    assertTrue(Frame.isUsableVhdlConsoleSplitFraction(0.25));
    assertTrue(Frame.isUsableVhdlConsoleSplitFraction(0.75));
    assertTrue(Frame.isUsableVhdlConsoleSplitFraction(0.95));
    assertFalse(Frame.isUsableVhdlConsoleSplitFraction(0.99));
    assertFalse(Frame.isUsableVhdlConsoleSplitFraction(1.0));
    assertFalse(Frame.isUsableVhdlConsoleSplitFraction(Double.POSITIVE_INFINITY));
  }

  @Test
  void restoresDefaultWhenExplorerSplitWouldHideNavigationPane() {
    assertEquals(0.25, Frame.sanitizeExplorerSplitFraction(null), FRACTION_DELTA);
    assertEquals(0.25, Frame.sanitizeExplorerSplitFraction(Double.NaN), FRACTION_DELTA);
    assertEquals(
        0.25, Frame.sanitizeExplorerSplitFraction(Double.NEGATIVE_INFINITY), FRACTION_DELTA);
    assertEquals(0.25, Frame.sanitizeExplorerSplitFraction(0.0), FRACTION_DELTA);
    assertEquals(0.25, Frame.sanitizeExplorerSplitFraction(0.04), FRACTION_DELTA);
    assertEquals(0.05, Frame.sanitizeExplorerSplitFraction(0.05), FRACTION_DELTA);
  }

  @Test
  void restoresDefaultWhenExplorerSplitWouldHideCanvas() {
    assertEquals(0.25, Frame.sanitizeExplorerSplitFraction(1.0), FRACTION_DELTA);
    assertEquals(0.25, Frame.sanitizeExplorerSplitFraction(0.99), FRACTION_DELTA);
    assertEquals(
        0.25, Frame.sanitizeExplorerSplitFraction(Double.POSITIVE_INFINITY), FRACTION_DELTA);
  }

  @Test
  void preservesUsableExplorerSplit() {
    assertEquals(0.05, Frame.sanitizeExplorerSplitFraction(0.05), FRACTION_DELTA);
    assertEquals(0.25, Frame.sanitizeExplorerSplitFraction(0.25), FRACTION_DELTA);
    assertEquals(0.75, Frame.sanitizeExplorerSplitFraction(0.75), FRACTION_DELTA);
    assertEquals(0.95, Frame.sanitizeExplorerSplitFraction(0.95), FRACTION_DELTA);
  }

  @Test
  void rejectsUnusableExplorerSplitForPersistence() {
    assertFalse(Frame.isUsableExplorerSplitFraction(Double.NaN));
    assertFalse(Frame.isUsableExplorerSplitFraction(Double.NEGATIVE_INFINITY));
    assertFalse(Frame.isUsableExplorerSplitFraction(0.0));
    assertFalse(Frame.isUsableExplorerSplitFraction(0.04));
    assertTrue(Frame.isUsableExplorerSplitFraction(0.05));
    assertTrue(Frame.isUsableExplorerSplitFraction(0.25));
    assertTrue(Frame.isUsableExplorerSplitFraction(0.75));
    assertTrue(Frame.isUsableExplorerSplitFraction(0.95));
    assertFalse(Frame.isUsableExplorerSplitFraction(0.99));
    assertFalse(Frame.isUsableExplorerSplitFraction(1.0));
    assertFalse(Frame.isUsableExplorerSplitFraction(Double.POSITIVE_INFINITY));
  }

  private static Object allocateWithoutConstructor(Class<?> type) throws Exception {
    final var unsafeClass = Class.forName("sun.misc.Unsafe");
    final var unsafeField = unsafeClass.getDeclaredField("theUnsafe");
    unsafeField.setAccessible(true);
    final var unsafe = unsafeField.get(null);
    return unsafeClass.getMethod("allocateInstance", Class.class).invoke(unsafe, type);
  }
}
