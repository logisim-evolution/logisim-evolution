/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.circuit.appear;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.circuit.CircuitMutation;
import com.cburch.logisim.comp.Component;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.file.Loader;
import com.cburch.logisim.file.LogisimFile;
import com.cburch.logisim.instance.InstanceComponent;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.std.memory.Register;
import com.cburch.logisim.std.memory.RegisterShape;
import java.util.List;
import org.junit.jupiter.api.Test;

class DynamicElementTransactionTest {

  @Test
  void componentReplacementUpdatesDynamicElementPath() {
    final var fixture = new Fixture();
    final var registerFactory = new Register();
    final var register =
        registerFactory.createComponent(
            Location.create(100, 100, true), registerFactory.createAttributeSet());
    add(fixture.circuit, register);

    final var shape =
        new RegisterShape(
            10, 10, new DynamicElement.Path(new InstanceComponent[] {(InstanceComponent) register}));
    fixture.circuit.getAppearance().addObjects(0, List.of(shape));

    final var movedRegister =
        registerFactory.createComponent(Location.create(140, 100, true), register.getAttributeSet());
    replace(fixture.circuit, register, movedRegister);

    assertTrue(fixture.circuit.getAppearance().getCustomObjectsFromBottom().contains(shape));
    assertSame(movedRegister, shape.getPath().leaf());
  }

  private static final class Fixture {
    private final Circuit circuit;

    private Fixture() {
      final var file = LogisimFile.createNew(new Loader(null), null);
      final var project = new Project(file);
      circuit = file.getMainCircuit();
      circuit.setProject(project);
      project.setCurrentCircuit(circuit);
    }
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
