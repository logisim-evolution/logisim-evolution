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

class Ttl7493Test {
  @Test
  void usesPhysicalPinoutAndKeepsPowerPortsLast() {
    final var gate = new Ttl7493();
    final var hiddenPower = createInstance(gate, false);

    assertEquals(8, hiddenPower.getPorts().size());
    assertPort(hiddenPower, Ttl7493.PORT_INDEX_CKB, 10, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl7493.PORT_INDEX_R0_1, 30, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl7493.PORT_INDEX_R0_2, 50, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl7493.PORT_INDEX_QC, 130, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl7493.PORT_INDEX_QB, 110, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl7493.PORT_INDEX_QD, 70, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl7493.PORT_INDEX_QA, 50, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl7493.PORT_INDEX_CKA, 10, -30, EndData.INPUT_ONLY);

    final var shownPower = createInstance(gate, true);
    assertEquals(10, shownPower.getPorts().size());
    assertPort(shownPower, 8, 90, -30, EndData.INPUT_ONLY); // Physical pin 10: GND
    assertPort(shownPower, 9, 90, 30, EndData.INPUT_ONLY); // Physical pin 5: VCC
  }

  @Test
  void configurablePowerPinsPreserveConventionalTtlLayout() {
    final var counter = new Ttl74161();

    assertEquals(14, createInstance(counter, false).getPorts().size());
    final var shownPower = createInstance(counter, true);
    assertEquals(16, shownPower.getPorts().size());
    assertPort(shownPower, 14, 150, 30, EndData.INPUT_ONLY); // Physical pin 8: GND
    assertPort(shownPower, 15, 10, -30, EndData.INPUT_ONLY); // Physical pin 16: VCC
  }

  @Test
  void configurablePowerPinsPreserveUnusedAndInoutPortHandling() {
    final var gateWithUnusedPins = new Ttl7413();
    assertEquals(10, createInstance(gateWithUnusedPins, false).getPorts().size());
    final var shownGatePower = createInstance(gateWithUnusedPins, true);
    assertEquals(12, shownGatePower.getPorts().size());
    assertPort(shownGatePower, 10, 130, 30, EndData.INPUT_ONLY); // Physical pin 7: GND
    assertPort(shownGatePower, 11, 10, -30, EndData.INPUT_ONLY); // Physical pin 14: VCC

    final var transceiver = createInstance(new Ttl74245(), true);
    assertEquals(20, transceiver.getPorts().size());
    assertPort(transceiver, 1, 30, 30, EndData.INPUT_OUTPUT); // Physical pin 2: A1
    assertPort(transceiver, 18, 190, 30, EndData.INPUT_ONLY); // Physical pin 10: GND
    assertPort(transceiver, 19, 10, -30, EndData.INPUT_ONLY); // Physical pin 20: VCC
  }

  @Test
  void independentFallingEdgeClocksAndAsynchronousResetWork() {
    final var gate = new Ttl7493();
    final var state = new TestInstanceState(gate, false);
    reset(gate, state);

    pulse(gate, state, Ttl7493.PORT_INDEX_CKA);
    assertEquals(1, outputValue(state));

    pulse(gate, state, Ttl7493.PORT_INDEX_CKB);
    assertEquals(3, outputValue(state));

    state.setPortValue(Ttl7493.PORT_INDEX_R0_1, Value.TRUE);
    state.setPortValue(Ttl7493.PORT_INDEX_R0_2, Value.TRUE);
    gate.propagate(state);
    assertEquals(0, outputValue(state));
  }

  @Test
  void qaToCkbCascadeCountsFromZeroThroughFifteen() {
    final var gate = new Ttl7493();
    final var state = new TestInstanceState(gate, false);
    reset(gate, state);

    for (var expected = 1; expected <= 16; expected++) {
      cascadeClockA(gate, state);
      assertEquals(expected & 0xF, outputValue(state));
    }
  }

  @Test
  void invalidExposedPowerInputsMakeOutputsUnknown() {
    final var gate = new Ttl7493();
    final var state = new TestInstanceState(gate, true);
    state.setPortValue(8, Value.FALSE);
    state.setPortValue(9, Value.TRUE);
    reset(gate, state);
    pulse(gate, state, Ttl7493.PORT_INDEX_CKA);
    assertEquals(1, outputValue(state));

    state.setPortValue(9, Value.FALSE);
    gate.propagate(state);
    assertUnknownOutputs(state);

    state.setPortValue(9, Value.TRUE);
    gate.propagate(state);
    assertEquals(1, outputValue(state));

    state.setPortValue(8, Value.TRUE);
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

  private static void reset(Ttl7493 gate, TestInstanceState state) {
    state.setPortValue(Ttl7493.PORT_INDEX_CKA, Value.FALSE);
    state.setPortValue(Ttl7493.PORT_INDEX_CKB, Value.FALSE);
    state.setPortValue(Ttl7493.PORT_INDEX_R0_1, Value.TRUE);
    state.setPortValue(Ttl7493.PORT_INDEX_R0_2, Value.TRUE);
    gate.propagate(state);
    state.setPortValue(Ttl7493.PORT_INDEX_R0_1, Value.FALSE);
    state.setPortValue(Ttl7493.PORT_INDEX_R0_2, Value.FALSE);
    gate.propagate(state);
  }

  private static void pulse(Ttl7493 gate, TestInstanceState state, int clockPort) {
    state.setPortValue(clockPort, Value.TRUE);
    gate.propagate(state);
    state.setPortValue(clockPort, Value.FALSE);
    gate.propagate(state);
  }

  private static void cascadeClockA(Ttl7493 gate, TestInstanceState state) {
    state.setPortValue(Ttl7493.PORT_INDEX_CKB, state.getPortValue(Ttl7493.PORT_INDEX_QA));
    state.setPortValue(Ttl7493.PORT_INDEX_CKA, Value.TRUE);
    gate.propagate(state);
    state.setPortValue(Ttl7493.PORT_INDEX_CKB, state.getPortValue(Ttl7493.PORT_INDEX_QA));
    state.setPortValue(Ttl7493.PORT_INDEX_CKA, Value.FALSE);
    gate.propagate(state);
    state.setPortValue(Ttl7493.PORT_INDEX_CKB, state.getPortValue(Ttl7493.PORT_INDEX_QA));
    gate.propagate(state);
  }

  private static int outputValue(TestInstanceState state) {
    return (int)
        (state.getPortValue(Ttl7493.PORT_INDEX_QA).toLongValue()
            | state.getPortValue(Ttl7493.PORT_INDEX_QB).toLongValue() << 1
            | state.getPortValue(Ttl7493.PORT_INDEX_QC).toLongValue() << 2
            | state.getPortValue(Ttl7493.PORT_INDEX_QD).toLongValue() << 3);
  }

  private static void assertUnknownOutputs(TestInstanceState state) {
    assertEquals(Value.UNKNOWN, state.getPortValue(Ttl7493.PORT_INDEX_QA));
    assertEquals(Value.UNKNOWN, state.getPortValue(Ttl7493.PORT_INDEX_QB));
    assertEquals(Value.UNKNOWN, state.getPortValue(Ttl7493.PORT_INDEX_QC));
    assertEquals(Value.UNKNOWN, state.getPortValue(Ttl7493.PORT_INDEX_QD));
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
