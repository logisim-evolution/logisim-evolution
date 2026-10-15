/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.std.io.extra;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.circuit.CircuitMutation;
import com.cburch.logisim.circuit.CircuitState;
import com.cburch.logisim.circuit.Simulator;
import com.cburch.logisim.circuit.Wire;
import com.cburch.logisim.comp.Component;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.file.Loader;
import com.cburch.logisim.file.LogisimFile;
import com.cburch.logisim.instance.InstanceDataSingleton;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.std.wiring.Constant;
import com.cburch.logisim.std.wiring.Pin;
import com.cburch.logisim.std.wiring.PullResistor;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class SwitchContactTest {
  private Simulator simulator;

  @AfterEach
  void shutDown() {
    if (simulator != null) simulator.shutDown();
  }

  @Test
  void closedSwitchOverridesPullUpAndOpenSwitchLetsTheBusFloatHigh() throws InterruptedException {
    final var harness = harness();
    final var contact = eastSwitch(Location.create(80, 100, true));
    final var input = contact.getEnd(0).getLocation();
    final var output = contact.getEnd(1).getLocation();
    final var low = constant(input, 0L);
    final var pull = pullUp(output);
    harness.add(contact, low, pull);

    harness.close(contact, true);
    harness.settle();
    assertEquals(Value.FALSE, harness.value(output));
    assertEquals(Value.FALSE, harness.value(input));

    harness.close(contact, false);
    harness.settle();
    assertEquals(Value.TRUE, harness.value(output));
    assertEquals(Value.FALSE, harness.value(input));
  }

  @Test
  void valuePassesOnlyTowardTheFacingEnd() throws InterruptedException {
    final var harness = harness();
    final var contact = eastSwitch(Location.create(100, 100, true));
    final var input = contact.getEnd(0).getLocation();
    final var output = contact.getEnd(1).getLocation();
    final var source = sourcePin("In", input);
    final var opposite = sourcePin("Out", output);
    harness.add(contact, source, opposite);
    harness.close(contact, true);

    harness.drive(source, Value.FALSE);
    harness.drive(opposite, Value.UNKNOWN);
    harness.settle();
    assertEquals(Value.FALSE, harness.value(input));
    assertEquals(Value.FALSE, harness.value(output));

    harness.drive(source, Value.UNKNOWN);
    harness.drive(opposite, Value.TRUE);
    harness.settle();
    assertEquals(Value.UNKNOWN, harness.value(input));
    assertEquals(Value.TRUE, harness.value(output));
  }

  @Test
  void switchesInSeriesFollowAChangingSource() throws InterruptedException {
    final var harness = harness();
    final var first = eastSwitch(Location.create(100, 100, true));
    final var second = eastSwitch(Location.create(140, 100, true));
    final var source = sourcePin("In", first.getEnd(0).getLocation());
    final var link = Wire.create(first.getEnd(1).getLocation(), second.getEnd(0).getLocation());
    harness.add(first, second, source, link);
    harness.close(first, true);
    harness.close(second, true);

    harness.drive(source, Value.FALSE);
    harness.settle();
    assertEquals(Value.FALSE, harness.value(second.getEnd(1).getLocation()));

    harness.drive(source, Value.TRUE);
    harness.settle();
    assertEquals(Value.TRUE, harness.value(first.getEnd(1).getLocation()));
    assertEquals(Value.TRUE, harness.value(second.getEnd(1).getLocation()));
  }

  private Harness harness() {
    final var file = LogisimFile.createNew(new Loader(null), null);
    final var project = new Project(file);
    final var circuit = file.getMainCircuit();
    circuit.setProject(project);
    simulator = project.getSimulator();
    return new Harness(circuit, simulator);
  }

  private static Component eastSwitch(Location location) {
    final var factory = new Switch();
    return factory.createComponent(location, factory.createAttributeSet());
  }

  private static Component constant(Location location, long value) {
    final var attrs = Constant.FACTORY.createAttributeSet();
    attrs.setValue(Constant.ATTR_VALUE, value);
    return Constant.FACTORY.createComponent(location, attrs);
  }

  private static Component pullUp(Location location) {
    final var attrs = PullResistor.FACTORY.createAttributeSet();
    attrs.setValue(PullResistor.ATTR_PULL_TYPE, PullResistor.ATTR_PULL_TYPE.parse("1"));
    return PullResistor.FACTORY.createComponent(location, attrs);
  }

  private static Component sourcePin(String label, Location location) {
    final var attrs = Pin.FACTORY.createAttributeSet();
    attrs.setValue(Pin.ATTR_TYPE, Pin.INPUT);
    attrs.setValue(Pin.ATTR_BEHAVIOR, Pin.TRISTATE);
    attrs.setValue(StdAttr.LABEL, label);
    return Pin.FACTORY.createComponent(location, attrs);
  }

  private static final class Harness {
    private final Circuit circuit;
    private final Simulator simulator;
    private CircuitState state;

    private Harness(Circuit circuit, Simulator simulator) {
      this.circuit = circuit;
      this.simulator = simulator;
    }

    private void add(Component... components) throws InterruptedException {
      final var mutation = new CircuitMutation(circuit);
      for (final var component : components) {
        mutation.add(component);
      }
      mutation.execute();
      state = CircuitState.createRootState(circuit.getProject(), circuit);
      simulator.setCircuitState(state);
      settle();
    }

    private void close(Component contact, boolean closed) {
      state.setData(contact, new InstanceDataSingleton(closed));
      state.markComponentAsDirty(contact);
    }

    private void drive(Component pin, Value value) {
      Pin.FACTORY.driveInputPin(state.getInstanceState(pin), value);
      state.markComponentAsDirty(pin);
    }

    private Value value(Location location) {
      return state.getValue(location);
    }

    private void settle() throws InterruptedException {
      final var done = new CountDownLatch(1);
      final Simulator.Listener listener =
          new Simulator.Listener() {
            @Override
            public void propagationCompleted(Simulator.Event event) {
              done.countDown();
            }

            @Override
            public void simulatorReset(Simulator.Event event) {}

            @Override
            public void simulatorStateChanged(Simulator.Event event) {}
          };
      simulator.addSimulatorListener(listener);
      try {
        assertTrue(simulator.nudge());
        assertTrue(done.await(2, TimeUnit.SECONDS));
      } finally {
        simulator.removeSimulatorListener(listener);
      }
    }
  }
}
