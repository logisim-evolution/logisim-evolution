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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.gui.generic.ZoomModel;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeListener;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class CircuitViewMemoryTest {
  private static final double ZOOM_DELTA = 1.0e-6;

  @Test
  void restoresSavedViewForCircuit() {
    final var circuit = new Circuit("main", null, null);
    final var memory = new CircuitViewMemory();
    final var zoom = new TestZoomModel();
    final var savedPosition = new Point(120, 80);

    zoom.setZoomFactor(1.5);
    memory.remember(circuit, zoom, savedPosition);
    savedPosition.setLocation(0, 0);

    zoom.setZoomFactor(0.75);
    final var restoredPosition = new AtomicReference<Point>();

    assertTrue(memory.restore(circuit, zoom, restoredPosition::set));
    assertEquals(1.5, zoom.getZoomFactor(), ZOOM_DELTA);
    assertEquals(new Point(120, 80), restoredPosition.get());
  }

  @Test
  void usesCircuitIdentityInsteadOfName() {
    final var first = new Circuit("same", null, null);
    final var second = new Circuit("same", null, null);
    final var memory = new CircuitViewMemory();
    final var zoom = new TestZoomModel();

    zoom.setZoomFactor(2.0);
    memory.remember(first, zoom, new Point(30, 40));

    zoom.setZoomFactor(1.0);
    final var restoredPosition = new AtomicReference<Point>();

    assertFalse(memory.restore(second, zoom, restoredPosition::set));
    assertEquals(1.0, zoom.getZoomFactor(), ZOOM_DELTA);
    assertNull(restoredPosition.get());
  }

  @Test
  void forgetRemovesSavedViewForCircuit() {
    final var circuit = new Circuit("main", null, null);
    final var memory = new CircuitViewMemory();
    final var zoom = new TestZoomModel();
    final var restoredPosition = new AtomicReference<Point>();

    memory.remember(circuit, zoom, new Point(10, 20));
    memory.forget(circuit);

    assertFalse(memory.restore(circuit, zoom, restoredPosition::set));
    assertNull(restoredPosition.get());
  }

  @Test
  void forgetUsesCircuitIdentityInsteadOfName() {
    final var first = new Circuit("same", null, null);
    final var second = new Circuit("same", null, null);
    final var memory = new CircuitViewMemory();
    final var zoom = new TestZoomModel();
    final var restoredPosition = new AtomicReference<Point>();

    memory.remember(first, zoom, new Point(30, 40));
    memory.forget(second);

    assertTrue(memory.restore(first, zoom, restoredPosition::set));
    assertEquals(new Point(30, 40), restoredPosition.get());
  }

  @Test
  void restorePassesCopyOfSavedPosition() {
    final var circuit = new Circuit("main", null, null);
    final var memory = new CircuitViewMemory();
    final var zoom = new TestZoomModel();
    final var restoredPosition = new AtomicReference<Point>();

    memory.remember(circuit, zoom, new Point(10, 20));
    assertTrue(memory.restore(circuit, zoom, restoredPosition::set));
    restoredPosition.get().setLocation(99, 99);

    assertTrue(memory.restore(circuit, zoom, restoredPosition::set));
    assertEquals(new Point(10, 20), restoredPosition.get());
  }

  private static class TestZoomModel implements ZoomModel {
    private double zoomFactor = 1.0;

    @Override
    public boolean getShowGrid() {
      return false;
    }

    @Override
    public void setShowGrid(boolean value) {}

    @Override
    public double getZoomFactor() {
      return zoomFactor;
    }

    @Override
    public List<Double> getZoomOptions() {
      return List.of();
    }

    @Override
    public void setZoomFactor(double value) {
      zoomFactor = value;
    }

    @Override
    public void setZoomFactor(double value, MouseEvent e) {
      zoomFactor = value;
    }

    @Override
    public void setZoomFactorCenter(double value) {
      zoomFactor = value;
    }

    @Override
    public void addPropertyChangeListener(String prop, PropertyChangeListener l) {}

    @Override
    public void removePropertyChangeListener(String prop, PropertyChangeListener l) {}
  }
}
