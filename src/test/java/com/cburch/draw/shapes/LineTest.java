/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.draw.shapes;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class LineTest {

  @Test
  void matchesReturnsTrueForIdenticalLines() {
    final var line = new Line(0, 1, 2, 3);
    final var copy = new Line(0, 1, 2, 3);
    assertTrue(line.matches(copy));
  }

  @Test
  void matchesReturnsFalseForTransposedCoordinates() {
    final var line = new Line(0, 1, 2, 3);
    final var transposed = new Line(0, 2, 1, 3);
    assertFalse(line.matches(transposed));
  }
}
