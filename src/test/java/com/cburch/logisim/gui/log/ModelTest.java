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
import com.cburch.logisim.data.Value;
import com.cburch.logisim.file.Loader;
import com.cburch.logisim.file.LogisimFile;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.std.wiring.Clock;
import com.cburch.logisim.std.wiring.Pin;
import java.util.concurrent.atomic.AtomicLong;
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

  @Test
  void realTimeModeKeepsInitialValueUntilFirstObservedChange() throws Exception {
    final var fixture = new Fixture();
    final var pin = Pin.FACTORY.createComponent(Location.create(100, 100, true), Pin.FACTORY.createAttributeSet());
    add(fixture.circuit, pin);
    Pin.FACTORY.driveInputPin(fixture.state.getInstanceState(pin), Value.FALSE);

    final var now = new AtomicLong(0);
    final var model = new Model(fixture.state, now::get);
    model.setRealMode(1_000_000_000, false);

    now.set(20_000_000);
    Pin.FACTORY.driveInputPin(fixture.state.getInstanceState(pin), Value.TRUE);
    model.propagationCompleted(false, false, true);

    final var signal = model.getSignal(0);
    assertEquals(Value.FALSE, signal.getValue(model.getEndTime() / 2));
    assertEquals(Value.TRUE, signal.getValue(model.getEndTime() - 1));
  }

  @Test
  void realTimeModeInitialDurationDoesNotExceedTimeScale() {
    final var fixture = new Fixture();
    final var model = new Model(fixture.state);

    model.setRealMode(5, false);

    assertEquals(5, model.getEndTime());
  }

  @Test
  void realTimeModeChangeDurationDoesNotExceedTimeScale() throws Exception {
    final var fixture = new Fixture();
    final var pin = Pin.FACTORY.createComponent(Location.create(100, 100, true), Pin.FACTORY.createAttributeSet());
    add(fixture.circuit, pin);
    Pin.FACTORY.driveInputPin(fixture.state.getInstanceState(pin), Value.FALSE);

    final var now = new AtomicLong(0);
    final var model = new Model(fixture.state, now::get);
    model.setRealMode(5, false);

    now.set(1_000_000_000);
    Pin.FACTORY.driveInputPin(fixture.state.getInstanceState(pin), Value.TRUE);
    model.propagationCompleted(false, false, true);

    assertEquals(15, model.getEndTime());
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
