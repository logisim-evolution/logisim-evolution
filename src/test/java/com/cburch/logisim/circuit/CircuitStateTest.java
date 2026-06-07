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
import com.cburch.logisim.comp.EndData;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.file.Loader;
import com.cburch.logisim.file.LogisimFile;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.std.memory.Ram;
import com.cburch.logisim.std.wiring.Pin;
import org.junit.jupiter.api.Test;

class CircuitStateTest {

  private static final class Fixture {
    private final LogisimFile file;
    private final Project project;
    private final Circuit circuit;
    private final CircuitState state;

    private Fixture() {
      file = LogisimFile.createNew(new Loader(null), null);
      project = new Project(file);
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

  @Test
  void disconnectedSubcircuitInputPropagatesUnknownInsideSubcircuit() {
    final var fixture = new Fixture();
    final var child = new Circuit("child", fixture.file, fixture.project);
    fixture.file.addCircuit(child);
    final var input = Pin.FACTORY.createComponent(Location.create(100, 100, true), Pin.FACTORY.createAttributeSet());
    final var outputAttrs = Pin.FACTORY.createAttributeSet();
    outputAttrs.setValue(Pin.ATTR_TYPE, Pin.OUTPUT);
    final var output = Pin.FACTORY.createComponent(Location.create(140, 100, true), outputAttrs);
    add(child, input);
    add(child, output);
    add(child, Wire.create(input.getLocation(), output.getLocation()));

    final var childInstance =
        child
            .getSubcircuitFactory()
            .createComponent(Location.create(200, 100, true), child.getSubcircuitFactory().createAttributeSet());
    add(fixture.circuit, childInstance);

    final var state = CircuitState.createRootState(fixture.project, fixture.circuit, Thread.currentThread());
    state.getPropagator().propagate();
    state.getPropagator().propagate();
    state.getPropagator().propagate();
    state.getPropagator().propagate();

    final var subState = child.getSubcircuitFactory().getSubstate(state, childInstance);
    assertEquals(Value.UNKNOWN, subState.getValue(input.getLocation()));
    assertEquals(Value.UNKNOWN, subState.getValue(output.getLocation()));
    final var outputEnd =
        childInstance.getEnds().stream().filter(EndData::isOutput).findFirst().orElseThrow();
    assertEquals(Value.UNKNOWN, state.getValue(outputEnd.getLocation()));
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
