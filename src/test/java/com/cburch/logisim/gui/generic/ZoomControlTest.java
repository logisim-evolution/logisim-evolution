/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.gui.generic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.cburch.logisim.data.Bounds;
import java.awt.Dimension;
import java.util.List;
import org.junit.jupiter.api.Test;

class ZoomControlTest {
  private static final double ZOOM_DELTA = 1.0e-6;
  private static final List<Double> ZOOM_OPTIONS = List.of(5.0, 100.0, 1000.0);

  @Test
  void computesFitZoomFromPaddedCircuitBounds() {
    assertEquals(
        0.5,
        ZoomControl.computeFitZoomFactor(
            Bounds.create(40, 30, 1900, 900), new Dimension(1000, 500), ZOOM_OPTIONS),
        ZOOM_DELTA);
  }

  @Test
  void usesMostRestrictiveViewportDimension() {
    assertEquals(
        0.75,
        ZoomControl.computeFitZoomFactor(
            Bounds.create(0, 0, 300, 700), new Dimension(1000, 600), ZOOM_OPTIONS),
        ZOOM_DELTA);
  }

  @Test
  void allowsAutoZoomButtonToEnlargeSmallCircuits() {
    assertEquals(
        2.5,
        ZoomControl.computeFitZoomFactor(
            Bounds.create(0, 0, 300, 200), new Dimension(1000, 800), ZOOM_OPTIONS),
        ZOOM_DELTA);
  }

  @Test
  void initialViewUsesOneHundredPercentInsteadOfEnlargingSmallCircuits() {
    assertEquals(
        1.0,
        ZoomControl.computeInitialZoomFactor(
            Bounds.create(0, 0, 300, 200), new Dimension(1000, 800), ZOOM_OPTIONS),
        ZOOM_DELTA);
  }

  @Test
  void initialViewShrinksLargeCircuitsToFit() {
    assertEquals(
        0.5,
        ZoomControl.computeInitialZoomFactor(
            Bounds.create(40, 30, 1900, 900), new Dimension(1000, 500), ZOOM_OPTIONS),
        ZOOM_DELTA);
  }

  @Test
  void initialViewUsesOneHundredPercentForEmptyCircuits() {
    assertEquals(
        1.0,
        ZoomControl.computeInitialZoomFactor(
            Bounds.EMPTY_BOUNDS, new Dimension(1000, 800), ZOOM_OPTIONS),
        ZOOM_DELTA);
  }

  @Test
  void clampsFitZoomToConfiguredRange() {
    assertEquals(
        0.05,
        ZoomControl.computeFitZoomFactor(
            Bounds.create(0, 0, 50000, 50000), new Dimension(100, 100), ZOOM_OPTIONS),
        ZOOM_DELTA);
    assertEquals(
        10.0,
        ZoomControl.computeFitZoomFactor(
            Bounds.create(0, 0, 1, 1), new Dimension(5000, 5000), ZOOM_OPTIONS),
        ZOOM_DELTA);
  }

  @Test
  void rejectsEmptyContentOrUnavailableViewport() {
    assertTrue(
        Double.isNaN(
            ZoomControl.computeFitZoomFactor(
                Bounds.EMPTY_BOUNDS, new Dimension(1000, 800), ZOOM_OPTIONS)));
    assertTrue(
        Double.isNaN(
            ZoomControl.computeFitZoomFactor(
                Bounds.create(0, 0, 300, 200), new Dimension(), ZOOM_OPTIONS)));
  }
}
