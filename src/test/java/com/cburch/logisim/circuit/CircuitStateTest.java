/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.circuit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import com.cburch.logisim.comp.Component;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.file.Loader;
import com.cburch.logisim.file.LogisimFile;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.std.memory.Ram;
import com.cburch.logisim.std.wiring.Pin;
import org.junit.jupiter.api.Test;

class CircuitStateTest {

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

  @Test
  void replacementWithSameFactoryKeepsComponentState() {
    final var fixture = new Fixture();
    final var ramFactory = new Ram();
    final var ram = ramFactory.createComponent(Location.create(100, 100, true), ramFactory.createAttributeSet());
    add(fixture.circuit, ram);

    final var contents = ramFactory.getContents(fixture.state.getInstanceState(ram));
    contents.set(3, 0x5a);
    final var ramState = fixture.state.getData(ram);

    final var movedRam = ramFactory.createComponent(Location.create(140, 100, true), ram.getAttributeSet());
    replace(fixture.circuit, ram, movedRam);

    assertNull(fixture.state.getData(ram));
    assertSame(ramState, fixture.state.getData(movedRam));
    assertEquals(0x5a, ramFactory.getContents(fixture.state.getInstanceState(movedRam)).get(3));
  }

  @Test
  void replacementWithDifferentFactoryDropsComponentState() {
    final var fixture = new Fixture();
    final var ramFactory = new Ram();
    final var ram = ramFactory.createComponent(Location.create(100, 100, true), ramFactory.createAttributeSet());
    add(fixture.circuit, ram);

    ramFactory.getContents(fixture.state.getInstanceState(ram)).set(3, 0x5a);
    final var pin = Pin.FACTORY.createComponent(Location.create(140, 100, true), Pin.FACTORY.createAttributeSet());

    replace(fixture.circuit, ram, pin);

    assertNull(fixture.state.getData(ram));
    assertNull(fixture.state.getData(pin));
  }

  private static void add(Circuit circuit, Component component) {
    final var mutation = new CircuitMutation(circuit);
    mutation.add(component);
    mutation.execute();
  }

  private static void replace(Circuit circuit, Component oldComponent, Component newComponent) {
    final var mutation = new CircuitMutation(circuit);
    mutation.replace(oldComponent, newComponent);
    mutation.execute();
  }
}
