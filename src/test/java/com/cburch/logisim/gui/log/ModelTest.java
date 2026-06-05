/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.gui.log;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.circuit.CircuitMutation;
import com.cburch.logisim.circuit.CircuitState;
import com.cburch.logisim.comp.Component;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.file.Loader;
import com.cburch.logisim.file.LogisimFile;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.std.wiring.Clock;
import org.junit.jupiter.api.Test;

class ModelTest {

  @Test
  void clockModeInitializesToCurrentClockPhaseDuration() {
    final var fixture = new Fixture();
    final var attrs = Clock.FACTORY.createAttributeSet();
    attrs.setValue(Clock.ATTR_LOW, 2);
    final var clock = Clock.FACTORY.createComponent(Location.create(100, 100, true), attrs);
    add(fixture.circuit, clock);

    final var model = new Model(fixture.state);

    assertEquals(10_000, model.getEndTime());
    assertDoesNotThrow(() -> model.propagationCompleted(false, false, true));
  }

  private static void add(Circuit circuit, Component component) {
    final var mutation = new CircuitMutation(circuit);
    mutation.add(component);
    mutation.execute();
  }

  private static final class Fixture {
    private final Circuit circuit;
    private final CircuitState state;

    private Fixture() {
      final var file = LogisimFile.createNew(new Loader(null), null);
      final var project = new Project(file);
      circuit = file.getMainCircuit();
      circuit.setProject(project);
      project.setCurrentCircuit(circuit);
      state = project.getCircuitState();
    }
  }
}
