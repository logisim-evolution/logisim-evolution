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
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.cburch.logisim.comp.Component;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.file.Loader;
import com.cburch.logisim.file.LogisimFile;
import com.cburch.logisim.instance.InstanceFactory;
import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.instance.InstanceState;
import com.cburch.logisim.proj.Project;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

class SimulatorTest {

  private static final String PROPAGATE_ERROR = "propagation stopped with useful context";

  @Test
  void recordsPropagationExceptionMessage() throws InterruptedException {
    final var file = LogisimFile.createNew(new Loader(null), null);
    final var project = new Project(file);
    final var circuit = file.getMainCircuit();
    circuit.setProject(project);
    final var throwingComponent =
        ThrowingFactory.INSTANCE.createComponent(
            Location.create(100, 100, true), ThrowingFactory.INSTANCE.createAttributeSet());
    add(circuit, throwingComponent);

    final var simulator = project.getSimulator();
    try {
      final var propagated = new CountDownLatch(1);
      simulator.addSimulatorListener(
          new Simulator.Listener() {
            @Override
            public void propagationCompleted(Simulator.Event e) {
              propagated.countDown();
            }

            @Override
            public void simulatorReset(Simulator.Event e) {}

            @Override
            public void simulatorStateChanged(Simulator.Event e) {}
          });
      simulator.setCircuitState(CircuitState.createRootState(project, circuit));

      assertTrue(simulator.nudge());
      assertTrue(propagated.await(2, TimeUnit.SECONDS));
      assertTrue(simulator.isExceptionEncountered());
      assertEquals(PROPAGATE_ERROR, simulator.getExceptionMessage());
    } finally {
      simulator.shutDown();
    }
  }

  private static void add(Circuit circuit, Component component) {
    final var mutation = new CircuitMutation(circuit);
    mutation.add(component);
    mutation.execute();
  }

  private static final class ThrowingFactory extends InstanceFactory {
    private static final ThrowingFactory INSTANCE = new ThrowingFactory();

    private ThrowingFactory() {
      super("Throwing");
    }

    @Override
    public void paintInstance(InstancePainter painter) {}

    @Override
    public void propagate(InstanceState state) {
      throw new UnsupportedOperationException(PROPAGATE_ERROR);
    }
  }
}
