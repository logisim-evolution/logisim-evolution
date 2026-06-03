/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.circuit;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.cburch.logisim.comp.Component;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.std.wiring.Pin;
import com.cburch.logisim.std.wiring.Tunnel;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class CircuitLabelValidationTest {
  @Test
  void rejectsLabelsThatOnlyDifferByCaseFromAnotherComponent() {
    final var existingPin = pinWithLabel("A");
    final var components = componentSet(existingPin);

    assertFalse(
        Circuit.isCorrectLabel(
            "main", "a", components, Pin.FACTORY.createAttributeSet(), Pin.FACTORY, false));
  }

  @Test
  void allowsChangingOnlyTheCaseOfTheSameComponentLabel() {
    final var existingPin = pinWithLabel("A");
    final var components = componentSet(existingPin);

    assertTrue(
        Circuit.isCorrectLabel(
            "main", "a", components, existingPin.getAttributeSet(), Pin.FACTORY, false));
  }

  @Test
  void rejectsPinLabelsThatOnlyDifferByCaseFromCircuitName() {
    assertFalse(
        Circuit.isCorrectLabel(
            "Display", "display", Set.of(), Pin.FACTORY.createAttributeSet(), Pin.FACTORY, false));
  }

  @Test
  void allowsTunnelLabelsToReuseComponentLabels() {
    final var existingPin = pinWithLabel("A");
    final var tunnelAttrs = Tunnel.FACTORY.createAttributeSet();

    assertTrue(
        Circuit.isCorrectLabel(
            "main", "a", componentSet(existingPin), tunnelAttrs, Tunnel.FACTORY, false));
  }

  private static Component pinWithLabel(String label) {
    final var attrs = Pin.FACTORY.createAttributeSet();
    attrs.setValue(StdAttr.LABEL, label);
    return Pin.FACTORY.createComponent(Location.create(0, 0, true), attrs);
  }

  private static Set<Component> componentSet(Component component) {
    final var components = new HashSet<Component>();
    components.add(component);
    return components;
  }
}
