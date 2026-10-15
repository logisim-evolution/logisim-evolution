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

import com.cburch.logisim.circuit.CircuitMutation;
import com.cburch.logisim.circuit.CircuitState;
import com.cburch.logisim.circuit.Simulator;
import com.cburch.logisim.comp.Component;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.file.Loader;
import com.cburch.logisim.file.LogisimFile;
import com.cburch.logisim.instance.InstanceDataSingleton;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.std.wiring.Pin;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SwitchContactTest {
  private Project project;
  private CircuitState state;
  private Simulator simulator;
  private Component contact;
  private Component left;
  private Component right;
  private Location leftPoint;
  private Location rightPoint;

  @BeforeEach
  void buildCircuit() throws InterruptedException {
    final var file = LogisimFile.createNew(new Loader(null), null);
    project = new Project(file);
    final var circuit = file.getMainCircuit();
    circuit.setProject(project);
    simulator = project.getSimulator();

    final var factory = new Switch();
    contact = factory.createComponent(Location.create(100, 100, true), factory.createAttributeSet());
    leftPoint = contact.getEnd(0).getLocation();
    rightPoint = contact.getEnd(1).getLocation();
    left = sourcePin("L", leftPoint);
    right = sourcePin("R", rightPoint);

    final var mutation = new CircuitMutation(circuit);
    mutation.add(contact);
    mutation.add(left);
    mutation.add(right);
    mutation.execute();

    state = CircuitState.createRootState(project, circuit);
    simulator.setCircuitState(state);
    settle();
  }

  @AfterEach
  void shutDown() {
    if (simulator != null) simulator.shutDown();
  }

  @Test
  void closedContactPassesEitherWayAndFollowsAChangingSource() throws InterruptedException {
    close(true);
    drive(left, Value.TRUE);
    drive(right, Value.UNKNOWN);
    settle();
    assertEquals(Value.TRUE, state.getValue(leftPoint));
    assertEquals(Value.TRUE, state.getValue(rightPoint));

    drive(left, Value.FALSE);
    settle();
    assertEquals(Value.FALSE, state.getValue(leftPoint));
    assertEquals(Value.FALSE, state.getValue(rightPoint));

    drive(left, Value.UNKNOWN);
    drive(right, Value.TRUE);
    settle();
    assertEquals(Value.TRUE, state.getValue(leftPoint));
    assertEquals(Value.TRUE, state.getValue(rightPoint));
  }

  @Test
  void openContactKeepsTheEndsIndependent() throws InterruptedException {
    close(false);
    drive(left, Value.TRUE);
    drive(right, Value.FALSE);
    settle();
    assertEquals(Value.TRUE, state.getValue(leftPoint));
    assertEquals(Value.FALSE, state.getValue(rightPoint));
  }

  @Test
  void closedContactRecoversAfterOneDriverReleases() throws InterruptedException {
    close(true);
    drive(left, Value.FALSE);
    drive(right, Value.UNKNOWN);
    settle();
    assertEquals(Value.FALSE, state.getValue(leftPoint));
    assertEquals(Value.FALSE, state.getValue(rightPoint));

    drive(right, Value.TRUE);
    settle();
    assertEquals(Value.ERROR, state.getValue(leftPoint));
    assertEquals(Value.ERROR, state.getValue(rightPoint));

    drive(left, Value.UNKNOWN);
    settle();
    assertEquals(Value.TRUE, state.getValue(leftPoint));
    assertEquals(Value.TRUE, state.getValue(rightPoint));

    drive(left, Value.FALSE);
    settle();
    assertEquals(Value.ERROR, state.getValue(leftPoint));
    assertEquals(Value.ERROR, state.getValue(rightPoint));

    drive(right, Value.UNKNOWN);
    settle();
    assertEquals(Value.FALSE, state.getValue(leftPoint));
    assertEquals(Value.FALSE, state.getValue(rightPoint));
  }

  @Test
  void opposingDriversConflictWhileClosed() throws InterruptedException {
    close(true);
    drive(left, Value.TRUE);
    drive(right, Value.FALSE);
    settle();
    assertEquals(Value.ERROR, state.getValue(leftPoint));
    assertEquals(Value.ERROR, state.getValue(rightPoint));
  }

  private static Component sourcePin(String label, Location location) {
    final var attrs = Pin.FACTORY.createAttributeSet();
    attrs.setValue(Pin.ATTR_TYPE, Pin.INPUT);
    attrs.setValue(Pin.ATTR_BEHAVIOR, Pin.TRISTATE);
    attrs.setValue(StdAttr.LABEL, label);
    return Pin.FACTORY.createComponent(location, attrs);
  }

  private void close(boolean closed) {
    state.setData(contact, new InstanceDataSingleton(closed));
    state.markComponentAsDirty(contact);
  }

  private void drive(Component pin, Value value) {
    Pin.FACTORY.driveInputPin(state.getInstanceState(pin), value);
    state.markComponentAsDirty(pin);
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
