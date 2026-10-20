/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.std.ttl;

import static org.junit.jupiter.api.Assertions.assertEquals;

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

/** Functional tests for the 74HC4060 14-stage binary ripple counter. */
class Ttl744060Test {
  private static final int GND_PORT = 12;
  private static final int VCC_PORT = 13;
  private static final BitWidth WIDTH = BitWidth.create(14);
  private static final int[] VISIBLE_BITS = {3, 4, 5, 6, 7, 8, 9, 11, 12, 13};
  private static final int[] VISIBLE_PORTS = {
    Ttl744060.PORT_INDEX_Q4,
    Ttl744060.PORT_INDEX_Q5,
    Ttl744060.PORT_INDEX_Q6,
    Ttl744060.PORT_INDEX_Q7,
    Ttl744060.PORT_INDEX_Q8,
    Ttl744060.PORT_INDEX_Q9,
    Ttl744060.PORT_INDEX_Q10,
    Ttl744060.PORT_INDEX_Q12,
    Ttl744060.PORT_INDEX_Q13,
    Ttl744060.PORT_INDEX_Q14
  };

  @Test
  void logicalPortsFollowTheDatasheetPinout() {
    final var gate = new Ttl744060();
    final var hiddenPower = createInstance(gate, false);

    assertEquals(12, hiddenPower.getPorts().size());
    assertPort(hiddenPower, Ttl744060.PORT_INDEX_Q12, 10, 30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl744060.PORT_INDEX_Q13, 30, 30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl744060.PORT_INDEX_Q14, 50, 30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl744060.PORT_INDEX_Q6, 70, 30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl744060.PORT_INDEX_Q5, 90, 30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl744060.PORT_INDEX_Q7, 110, 30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl744060.PORT_INDEX_Q4, 130, 30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl744060.PORT_INDEX_RS, 110, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl744060.PORT_INDEX_MR, 90, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl744060.PORT_INDEX_Q9, 70, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl744060.PORT_INDEX_Q8, 50, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl744060.PORT_INDEX_Q10, 30, -30, EndData.OUTPUT_ONLY);

    final var shownPower = createInstance(gate, true);
    assertEquals(14, shownPower.getPorts().size());
    assertPort(shownPower, GND_PORT, 150, 30, EndData.INPUT_ONLY);
    assertPort(shownPower, VCC_PORT, 10, -30, EndData.INPUT_ONLY);
  }

  @Test
  void visibleOutputsFollowBinaryStageWeights() {
    final var gate = new Ttl744060();
    final var state = new TestInstanceState(gate, false);
    reset(gate, state);
    assertCount(state, 0);

    for (var count = 1; count <= 64; count++) {
      pulse(gate, state);
      assertCount(state, count);
    }

    clock(gate, state, 1024 - 64);
    assertCount(state, 1024);
    clock(gate, state, 1024);
    assertCount(state, 2048);
    clock(gate, state, 2048);
    assertCount(state, 4096);
    clock(gate, state, 4096);
    assertCount(state, 8192);
    assertEquals(Value.TRUE, state.getPortValue(Ttl744060.PORT_INDEX_Q14));
    clock(gate, state, 8192);
    assertCount(state, 0);
  }

  @Test
  void masterResetClearsAndOverridesTheClock() {
    final var gate = new Ttl744060();
    final var state = new TestInstanceState(gate, false);
    reset(gate, state);
    clock(gate, state, 8);
    assertCount(state, 8);

    state.setPortValue(Ttl744060.PORT_INDEX_RS, Value.TRUE);
    gate.propagate(state);
    state.setPortValue(Ttl744060.PORT_INDEX_MR, Value.TRUE);
    state.setPortValue(Ttl744060.PORT_INDEX_RS, Value.FALSE);
    gate.propagate(state);
    assertCount(state, 0);

    pulse(gate, state);
    assertCount(state, 0);
    state.setPortValue(Ttl744060.PORT_INDEX_MR, Value.FALSE);
    gate.propagate(state);
    assertCount(state, 0);
    clock(gate, state, 8);
    assertCount(state, 8);
  }

  @Test
  void risingAndUnknownClocksDoNotCount() {
    final var gate = new Ttl744060();
    final var state = new TestInstanceState(gate, false);
    reset(gate, state);

    state.setPortValue(Ttl744060.PORT_INDEX_RS, Value.TRUE);
    gate.propagate(state);
    assertCount(state, 0);

    state.setPortValue(Ttl744060.PORT_INDEX_RS, Value.UNKNOWN);
    gate.propagate(state);
    state.setPortValue(Ttl744060.PORT_INDEX_RS, Value.FALSE);
    gate.propagate(state);
    assertCount(state, 0);
  }

  @Test
  void unknownResetDoesNotClearAndAFallingEdgeStillCounts() {
    final var gate = new Ttl744060();
    final var state = new TestInstanceState(gate, false);
    reset(gate, state);
    clock(gate, state, 8);

    state.setPortValue(Ttl744060.PORT_INDEX_MR, Value.UNKNOWN);
    gate.propagate(state);
    assertCount(state, 8);

    pulse(gate, state);
    assertCount(state, 9);
  }

  @Test
  void undefinedCountBecomesUnknownOnTheNextClock() {
    final var gate = new Ttl744060();
    final var state = new TestInstanceState(gate, false);
    reset(gate, state);
    final var data = new TtlRegisterData(WIDTH);
    data.setValue(Value.createUnknown(WIDTH));
    state.setData(data);

    pulse(gate, state);
    assertUnknownOutputs(state);

    state.setPortValue(Ttl744060.PORT_INDEX_MR, Value.TRUE);
    gate.propagate(state);
    assertCount(state, 0);
  }

  @Test
  void errorInTheCountBecomesErrorOnTheNextClock() {
    final var gate = new Ttl744060();
    final var state = new TestInstanceState(gate, false);
    reset(gate, state);
    final var data = new TtlRegisterData(WIDTH);
    final var bits = Value.createKnown(WIDTH, 0).getAll();
    bits[0] = Value.ERROR;
    data.setValue(Value.create(bits));
    state.setData(data);

    pulse(gate, state);
    assertErrorOutputs(state);

    state.setPortValue(Ttl744060.PORT_INDEX_MR, Value.TRUE);
    gate.propagate(state);
    assertCount(state, 0);
  }

  @Test
  void invalidExposedPowerInputsMakeOutputsUnknown() {
    final var gate = new Ttl744060();
    final var state = new TestInstanceState(gate, true);
    state.setPortValue(GND_PORT, Value.FALSE);
    state.setPortValue(VCC_PORT, Value.TRUE);
    reset(gate, state);
    clock(gate, state, 8);
    assertCount(state, 8);

    state.setPortValue(VCC_PORT, Value.FALSE);
    gate.propagate(state);
    assertUnknownOutputs(state);

    state.setPortValue(VCC_PORT, Value.TRUE);
    gate.propagate(state);
    assertCount(state, 8);

    state.setPortValue(GND_PORT, Value.TRUE);
    gate.propagate(state);
    assertUnknownOutputs(state);
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

  private static void reset(Ttl744060 gate, TestInstanceState state) {
    state.setPortValue(Ttl744060.PORT_INDEX_RS, Value.FALSE);
    state.setPortValue(Ttl744060.PORT_INDEX_MR, Value.TRUE);
    gate.propagate(state);
    state.setPortValue(Ttl744060.PORT_INDEX_MR, Value.FALSE);
    gate.propagate(state);
  }

  private static void pulse(Ttl744060 gate, TestInstanceState state) {
    state.setPortValue(Ttl744060.PORT_INDEX_RS, Value.TRUE);
    gate.propagate(state);
    state.setPortValue(Ttl744060.PORT_INDEX_RS, Value.FALSE);
    gate.propagate(state);
  }

  private static void clock(Ttl744060 gate, TestInstanceState state, int pulses) {
    for (var i = 0; i < pulses; i++) {
      pulse(gate, state);
    }
  }

  private static void assertCount(TestInstanceState state, int count) {
    for (var i = 0; i < VISIBLE_BITS.length; i++) {
      final var expected = ((count >> VISIBLE_BITS[i]) & 1) == 0 ? Value.FALSE : Value.TRUE;
      assertEquals(expected, state.getPortValue(VISIBLE_PORTS[i]), "count " + count);
    }
  }

  private static void assertUnknownOutputs(TestInstanceState state) {
    for (final var port : VISIBLE_PORTS) {
      assertEquals(Value.UNKNOWN, state.getPortValue(port));
    }
  }

  private static void assertErrorOutputs(TestInstanceState state) {
    for (final var port : VISIBLE_PORTS) {
      assertEquals(Value.ERROR, state.getPortValue(port));
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
