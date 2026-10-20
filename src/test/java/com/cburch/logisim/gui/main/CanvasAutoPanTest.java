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
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class CanvasAutoPanTest {
  @Test
  void pointerInCenterDoesNotPan() {
    assertEquals(0, Canvas.autoPanDelta(150, 100, 200));
  }

  @Test
  void pointerNearEdgesPansOutward() {
    assertTrue(Canvas.autoPanDelta(105, 100, 200) < 0);
    assertTrue(Canvas.autoPanDelta(295, 100, 200) > 0);
  }

  @Test
  void pointerOutsideHotZonesDoesNotPan() {
    assertEquals(0, Canvas.autoPanDelta(132, 100, 200));
    assertEquals(0, Canvas.autoPanDelta(267, 100, 200));
  }

  @Test
  void pointerAtHotZoneBoundariesPansOutward() {
    assertTrue(Canvas.autoPanDelta(131, 100, 200) < 0);
    assertTrue(Canvas.autoPanDelta(268, 100, 200) > 0);
  }

  @Test
  void panningAcceleratesTowardEdge() {
    final var nearHotZoneStart = Canvas.autoPanDelta(270, 100, 200);
    final var atEdge = Canvas.autoPanDelta(299, 100, 200);

    assertTrue(atEdge > nearHotZoneStart);
  }

  @Test
  void panningSpeedIsCappedOutsideView() {
    assertEquals(
        Canvas.autoPanDelta(100, 100, 200), Canvas.autoPanDelta(50, 100, 200));
    assertEquals(
        Canvas.autoPanDelta(299, 100, 200), Canvas.autoPanDelta(350, 100, 200));
  }
}
