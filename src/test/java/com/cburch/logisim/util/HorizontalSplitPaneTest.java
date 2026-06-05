/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import javax.swing.JPanel;
import org.junit.jupiter.api.Test;

class HorizontalSplitPaneTest {
  private static final double FRACTION_DELTA = 1.0e-4;

  @Test
  void defaultFractionBoundsAllowCollapsedPanes() {
    final var pane = new HorizontalSplitPane(new JPanel(), new JPanel(), 0.5);

    pane.setFraction(0.0);
    assertEquals(0.0, pane.getFraction(), FRACTION_DELTA);

    pane.setFraction(1.0);
    assertEquals(1.0, pane.getFraction(), FRACTION_DELTA);
  }

  @Test
  void fractionBoundsClampFractionChanges() {
    final var pane = new HorizontalSplitPane(new JPanel(), new JPanel(), 0.5);
    pane.setFractionBounds(0.05, 0.95);

    pane.setFraction(0.0);
    assertEquals(0.05, pane.getFraction(), FRACTION_DELTA);

    pane.setFraction(1.0);
    assertEquals(0.95, pane.getFraction(), FRACTION_DELTA);
  }

  @Test
  void fractionIgnoresNan() {
    final var pane = new HorizontalSplitPane(new JPanel(), new JPanel(), 0.5);

    pane.setFraction(Double.NaN);

    assertEquals(0.5, pane.getFraction(), FRACTION_DELTA);
  }

  @Test
  void fractionBoundsCanBeRelaxedForProgrammaticHiding() {
    final var pane = new HorizontalSplitPane(new JPanel(), new JPanel(), 0.5);
    pane.setFractionBounds(0.05, 0.95);
    pane.setFractionBounds(0.0, 1.0);

    pane.setFraction(1.0);
    assertEquals(1.0, pane.getFraction(), FRACTION_DELTA);
  }

  @Test
  void fractionBoundsRejectInvalidRanges() {
    final var pane = new HorizontalSplitPane(new JPanel(), new JPanel(), 0.5);

    assertThrows(IllegalArgumentException.class, () -> pane.setFractionBounds(-0.1, 0.9));
    assertThrows(IllegalArgumentException.class, () -> pane.setFractionBounds(0.1, 1.1));
    assertThrows(IllegalArgumentException.class, () -> pane.setFractionBounds(0.9, 0.1));
    assertThrows(IllegalArgumentException.class, () -> pane.setFractionBounds(Double.NaN, 0.9));
    assertThrows(
        IllegalArgumentException.class,
        () -> pane.setFractionBounds(0.1, Double.POSITIVE_INFINITY));
  }
}
