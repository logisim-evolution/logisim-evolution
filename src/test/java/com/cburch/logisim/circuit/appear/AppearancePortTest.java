/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.circuit.appear;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.cburch.logisim.data.Location;
import com.cburch.logisim.instance.Instance;
import com.cburch.logisim.std.wiring.Pin;
import org.junit.jupiter.api.Test;

class AppearancePortTest {
  @Test
  void matchesRequiresSamePositionAndPin() {
    final var component = Pin.FACTORY.createComponent(
        Location.create(100, 100, true), Pin.FACTORY.createAttributeSet());
    final var original = new AppearancePort(
        Location.create(40, 50, true), Instance.getInstanceFor(component));
    final var clone = (AppearancePort) original.clone();
    assertTrue(original.matches(clone));
    assertEquals(original.matchesHashCode(), clone.matchesHashCode());

    clone.translate(10, 0);
    assertFalse(original.matches(clone));
    final var otherComponent = Pin.FACTORY.createComponent(
        Location.create(100, 100, true), Pin.FACTORY.createAttributeSet());
    assertFalse(original.matches(new AppearancePort(
        original.getLocation(), Instance.getInstanceFor(otherComponent))));
    assertFalse(original.matches(new AppearanceAnchor(original.getLocation())));
  }
}
