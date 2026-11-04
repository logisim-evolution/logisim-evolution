/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.std.ttl;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.circuit.CircuitState;
import com.cburch.logisim.comp.EndData;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.instance.Instance;
import com.cburch.logisim.instance.InstanceData;
import com.cburch.logisim.instance.InstanceFactory;
import com.cburch.logisim.instance.InstanceState;
import com.cburch.logisim.instance.Port;
import com.cburch.logisim.proj.Project;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Functional tests for the 74HC393 dual 4-bit binary ripple counter. */
class Ttl74393Test {
  private static final int GND_PORT = 12;
  private static final int VCC_PORT = 13;
  private static final BitWidth WIDTH = BitWidth.create(4);
  private static final int[] COUNTER1_PORTS = {
    Ttl74393.PORT_INDEX_1Q0,
    Ttl74393.PORT_INDEX_1Q1,
    Ttl74393.PORT_INDEX_1Q2,
    Ttl74393.PORT_INDEX_1Q3
  };
  private static final int[] COUNTER2_PORTS = {
    Ttl74393.PORT_INDEX_2Q0,
    Ttl74393.PORT_INDEX_2Q1,
    Ttl74393.PORT_INDEX_2Q2,
    Ttl74393.PORT_INDEX_2Q3
  };

  @Test
  void logicalPortsFollowTheDatasheetPinout() {
    final var gate = new Ttl74393();
    final var hiddenPower = createInstance(gate, false);

    assertEquals(12, hiddenPower.getPorts().size());
    assertPort(hiddenPower, Ttl74393.PORT_INDEX_1CP, 10, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74393.PORT_INDEX_1MR, 30, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74393.PORT_INDEX_1Q0, 50, 30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74393.PORT_INDEX_1Q1, 70, 30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74393.PORT_INDEX_1Q2, 90, 30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74393.PORT_INDEX_1Q3, 110, 30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74393.PORT_INDEX_2Q3, 130, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74393.PORT_INDEX_2Q2, 110, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74393.PORT_INDEX_2Q1, 90, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74393.PORT_INDEX_2Q0, 70, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74393.PORT_INDEX_2MR, 50, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74393.PORT_INDEX_2CP, 30, -30, EndData.INPUT_ONLY);

    final var shownPower = createInstance(gate, true);
    assertEquals(14, shownPower.getPorts().size());
    assertPort(shownPower, GND_PORT, 130, 30, EndData.INPUT_ONLY);
    assertPort(shownPower, VCC_PORT, 10, -30, EndData.INPUT_ONLY);
    assertTrue(gate.checkForGatedClocks(null));
    assertArrayEquals(
        new int[] {Ttl74393.PORT_INDEX_1CP, Ttl74393.PORT_INDEX_2CP}, gate.clockPinIndex(null));
  }

  @Test
  void eachCounterCountsSixteenStatesAndWraps() {
    final var gate = new Ttl74393();
    final var state = new TestInstanceState(gate, false);
    reset(gate, state);

    for (var count = 0; count <= 16; count++) {
      assertCount(state, COUNTER1_PORTS, count & 0xF);
      assertCount(state, COUNTER2_PORTS, 0);
      if (count < 16) pulse(gate, state, Ttl74393.PORT_INDEX_1CP);
    }

    for (var count = 0; count <= 16; count++) {
      assertCount(state, COUNTER1_PORTS, 0);
      assertCount(state, COUNTER2_PORTS, count & 0xF);
      if (count < 16) pulse(gate, state, Ttl74393.PORT_INDEX_2CP);
    }
  }

  @Test
  void countersAdvanceIndependentlyOnTheSameStep() {
    final var gate = new Ttl74393();
    final var state = new TestInstanceState(gate, false);
    reset(gate, state);
    clock(gate, state, Ttl74393.PORT_INDEX_1CP, 3);
    clock(gate, state, Ttl74393.PORT_INDEX_2CP, 5);
    assertCount(state, COUNTER1_PORTS, 3);
    assertCount(state, COUNTER2_PORTS, 5);

    state.setPortValue(Ttl74393.PORT_INDEX_1CP, Value.TRUE);
    state.setPortValue(Ttl74393.PORT_INDEX_2CP, Value.TRUE);
    gate.propagate(state);
    state.setPortValue(Ttl74393.PORT_INDEX_1CP, Value.FALSE);
    state.setPortValue(Ttl74393.PORT_INDEX_2CP, Value.FALSE);
    gate.propagate(state);
    assertCount(state, COUNTER1_PORTS, 4);
    assertCount(state, COUNTER2_PORTS, 6);
  }

  @Test
  void masterResetClearsAndOverridesTheClock() {
    final var gate = new Ttl74393();
    final var state = new TestInstanceState(gate, false);
    reset(gate, state);
    clock(gate, state, Ttl74393.PORT_INDEX_1CP, 4);
    clock(gate, state, Ttl74393.PORT_INDEX_2CP, 2);

    state.setPortValue(Ttl74393.PORT_INDEX_1CP, Value.TRUE);
    gate.propagate(state);
    state.setPortValue(Ttl74393.PORT_INDEX_1MR, Value.TRUE);
    state.setPortValue(Ttl74393.PORT_INDEX_1CP, Value.FALSE);
    gate.propagate(state);
    assertCount(state, COUNTER1_PORTS, 0);
    assertCount(state, COUNTER2_PORTS, 2);

    pulse(gate, state, Ttl74393.PORT_INDEX_1CP);
    assertCount(state, COUNTER1_PORTS, 0);
    state.setPortValue(Ttl74393.PORT_INDEX_1MR, Value.FALSE);
    gate.propagate(state);
    assertCount(state, COUNTER1_PORTS, 0);
    assertCount(state, COUNTER2_PORTS, 2);
    pulse(gate, state, Ttl74393.PORT_INDEX_1CP);
    assertCount(state, COUNTER1_PORTS, 1);

    state.setPortValue(Ttl74393.PORT_INDEX_2MR, Value.TRUE);
    gate.propagate(state);
    assertCount(state, COUNTER1_PORTS, 1);
    assertCount(state, COUNTER2_PORTS, 0);
  }

  @Test
  void risingAndUnknownClocksDoNotCount() {
    final var gate = new Ttl74393();
    final var state = new TestInstanceState(gate, false);
    reset(gate, state);

    state.setPortValue(Ttl74393.PORT_INDEX_1CP, Value.TRUE);
    gate.propagate(state);
    assertCount(state, COUNTER1_PORTS, 0);

    state.setPortValue(Ttl74393.PORT_INDEX_1CP, Value.UNKNOWN);
    gate.propagate(state);
    state.setPortValue(Ttl74393.PORT_INDEX_1CP, Value.FALSE);
    gate.propagate(state);
    assertCount(state, COUNTER1_PORTS, 0);

    state.setPortValue(Ttl74393.PORT_INDEX_1CP, Value.TRUE);
    gate.propagate(state);
    state.setPortValue(Ttl74393.PORT_INDEX_1CP, Value.ERROR);
    gate.propagate(state);
    state.setPortValue(Ttl74393.PORT_INDEX_1CP, Value.FALSE);
    gate.propagate(state);
    assertCount(state, COUNTER1_PORTS, 0);
    assertCount(state, COUNTER2_PORTS, 0);

    clock(gate, state, Ttl74393.PORT_INDEX_1CP, 4);
    state.setPortValue(Ttl74393.PORT_INDEX_1CP, Value.TRUE);
    gate.propagate(state);
    state.setPortValue(Ttl74393.PORT_INDEX_1MR, Value.TRUE);
    gate.propagate(state);
    assertCount(state, COUNTER1_PORTS, 0);
    state.setPortValue(Ttl74393.PORT_INDEX_1MR, Value.FALSE);
    gate.propagate(state);
    assertCount(state, COUNTER1_PORTS, 0);
    state.setPortValue(Ttl74393.PORT_INDEX_1CP, Value.FALSE);
    gate.propagate(state);
    assertCount(state, COUNTER1_PORTS, 1);
    assertCount(state, COUNTER2_PORTS, 0);
  }

  @Test
  void unknownResetDoesNotClearAndAFallingEdgeStillCounts() {
    final var gate = new Ttl74393();
    final var state = new TestInstanceState(gate, false);
    reset(gate, state);
    clock(gate, state, Ttl74393.PORT_INDEX_1CP, 8);

    state.setPortValue(Ttl74393.PORT_INDEX_1MR, Value.UNKNOWN);
    gate.propagate(state);
    assertCount(state, COUNTER1_PORTS, 8);

    pulse(gate, state, Ttl74393.PORT_INDEX_1CP);
    assertCount(state, COUNTER1_PORTS, 9);
  }

  @Test
  void undefinedCountBecomesUnknownOnTheNextClock() {
    final var gate = new Ttl74393();
    final var state = new TestInstanceState(gate, false);
    reset(gate, state);
    final var data = new TtlRegisterData(WIDTH, 2);
    data.setValue(0, Value.createUnknown(WIDTH));
    data.setValue(1, Value.createKnown(WIDTH, 3));
    state.setData(data);

    pulse(gate, state, Ttl74393.PORT_INDEX_1CP);
    assertLevel(state, COUNTER1_PORTS, Value.UNKNOWN);
    assertCount(state, COUNTER2_PORTS, 3);

    state.setPortValue(Ttl74393.PORT_INDEX_1MR, Value.TRUE);
    gate.propagate(state);
    assertCount(state, COUNTER1_PORTS, 0);
    assertCount(state, COUNTER2_PORTS, 3);
  }

  @Test
  void errorInTheCountBecomesErrorOnTheNextClock() {
    final var gate = new Ttl74393();
    final var state = new TestInstanceState(gate, false);
    reset(gate, state);
    final var data = new TtlRegisterData(WIDTH, 2);
    final var bits = Value.createKnown(WIDTH, 0).getAll();
    bits[0] = Value.ERROR;
    data.setValue(0, Value.create(bits));
    data.setValue(1, Value.createKnown(WIDTH, 6));
    state.setData(data);

    pulse(gate, state, Ttl74393.PORT_INDEX_1CP);
    assertLevel(state, COUNTER1_PORTS, Value.ERROR);
    assertCount(state, COUNTER2_PORTS, 6);

    state.setPortValue(Ttl74393.PORT_INDEX_1MR, Value.TRUE);
    gate.propagate(state);
    assertCount(state, COUNTER1_PORTS, 0);
  }

  @Test
  void invalidExposedPowerInputsMakeOutputsUnknown() {
    final var gate = new Ttl74393();
    final var state = new TestInstanceState(gate, true);
    state.setPortValue(GND_PORT, Value.FALSE);
    state.setPortValue(VCC_PORT, Value.TRUE);
    reset(gate, state);
    clock(gate, state, Ttl74393.PORT_INDEX_1CP, 8);
    clock(gate, state, Ttl74393.PORT_INDEX_2CP, 3);
    assertCount(state, COUNTER1_PORTS, 8);
    assertCount(state, COUNTER2_PORTS, 3);

    state.setPortValue(VCC_PORT, Value.FALSE);
    gate.propagate(state);
    assertLevel(state, COUNTER1_PORTS, Value.UNKNOWN);
    assertLevel(state, COUNTER2_PORTS, Value.UNKNOWN);

    state.setPortValue(VCC_PORT, Value.TRUE);
    gate.propagate(state);
    assertCount(state, COUNTER1_PORTS, 8);
    assertCount(state, COUNTER2_PORTS, 3);

    state.setPortValue(GND_PORT, Value.TRUE);
    gate.propagate(state);
    assertLevel(state, COUNTER1_PORTS, Value.UNKNOWN);
    assertLevel(state, COUNTER2_PORTS, Value.UNKNOWN);
  }

  private static Instance createInstance(InstanceFactory factory, boolean showPowerPins) {
    final var attrs = factory.createAttributeSet();
    attrs.setValue(TtlLibrary.VCC_GND, showPowerPins);
    return Instance.getInstanceFor(
        factory.createComponent(Location.create(0, 0, false), attrs));
  }

  private static void assertPort(Instance instance, int index, int x, int y, int type) {
    assertEquals(Location.create(x, y, false), instance.getPortLocation(index));
    assertEquals(type, instance.getPorts().get(index).getType());
  }

  private static void reset(Ttl74393 gate, TestInstanceState state) {
    state.setPortValue(Ttl74393.PORT_INDEX_1CP, Value.FALSE);
    state.setPortValue(Ttl74393.PORT_INDEX_2CP, Value.FALSE);
    state.setPortValue(Ttl74393.PORT_INDEX_1MR, Value.TRUE);
    state.setPortValue(Ttl74393.PORT_INDEX_2MR, Value.TRUE);
    gate.propagate(state);
    state.setPortValue(Ttl74393.PORT_INDEX_1MR, Value.FALSE);
    state.setPortValue(Ttl74393.PORT_INDEX_2MR, Value.FALSE);
    gate.propagate(state);
  }

  private static void pulse(Ttl74393 gate, TestInstanceState state, int clockPort) {
    state.setPortValue(clockPort, Value.TRUE);
    gate.propagate(state);
    state.setPortValue(clockPort, Value.FALSE);
    gate.propagate(state);
  }

  private static void clock(Ttl74393 gate, TestInstanceState state, int clockPort, int pulses) {
    for (var i = 0; i < pulses; i++) {
      pulse(gate, state, clockPort);
    }
  }

  private static void assertCount(TestInstanceState state, int[] ports, int count) {
    for (var bit = 0; bit < ports.length; bit++) {
      final var expected = ((count >> bit) & 1) == 0 ? Value.FALSE : Value.TRUE;
      assertEquals(expected, state.getPortValue(ports[bit]), "count " + count);
    }
  }

  private static void assertLevel(TestInstanceState state, int[] ports, Value level) {
    for (final var port : ports) {
      assertEquals(level, state.getPortValue(port));
    }
  }

  private static final class TestInstanceState implements InstanceState {
    private final AttributeSet attrs;
    private final Instance instance;
    private final Map<Integer, Value> portValues = new HashMap<>();
    private InstanceData data;

    private TestInstanceState(InstanceFactory factory, boolean showPowerPins) {
      attrs = factory.createAttributeSet();
      attrs.setValue(TtlLibrary.VCC_GND, showPowerPins);
      instance =
          Instance.getInstanceFor(
              factory.createComponent(Location.create(0, 0, false), attrs));
    }

    @Override
    public void fireInvalidated() {}

    @Override
    public AttributeSet getAttributeSet() {
      return attrs;
    }

    @Override
    public <E> E getAttributeValue(Attribute<E> attr) {
      return attrs.getValue(attr);
    }

    @Override
    public InstanceData getData() {
      return data;
    }

    @Override
    public InstanceFactory getFactory() {
      return instance.getFactory();
    }

    @Override
    public Instance getInstance() {
      return instance;
    }

    @Override
    public int getPortIndex(Port port) {
      return instance.getPorts().indexOf(port);
    }

    @Override
    public Value getPortValue(int portIndex) {
      return portValues.getOrDefault(portIndex, Value.UNKNOWN);
    }

    @Override
    public Project getProject() {
      return null;
    }

    @Override
    public int getTickCount() {
      return 0;
    }

    @Override
    public boolean isCircuitRoot() {
      return true;
    }

    @Override
    public boolean isPortConnected(int portIndex) {
      return false;
    }

    @Override
    public CircuitState createCircuitSubstateFor(Circuit circ) {
      return null;
    }

    @Override
    public void setData(InstanceData value) {
      data = value;
    }

    @Override
    public void setPort(int portIndex, Value value, int delay) {
      portValues.put(portIndex, value);
    }

    private void setPortValue(int portIndex, Value value) {
      portValues.put(portIndex, value);
    }
  }
}
