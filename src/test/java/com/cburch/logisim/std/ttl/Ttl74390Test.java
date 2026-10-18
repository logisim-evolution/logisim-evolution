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

/** Functional tests for the 74HC390 dual decade ripple counter. */
class Ttl74390Test {
  private static final int GND_PORT = 14;
  private static final int VCC_PORT = 15;

  /** Nexperia table 4, Q0 + 2*Q1 + 4*Q2 + 8*Q3, after each clock from a cleared counter. */
  private static final int[] BIQUINARY = {2, 4, 6, 8, 1, 3, 5, 7, 9, 0};
  private static final int[] OUTPUT_PORTS = {
    Ttl74390.PORT_INDEX_1Q0,
    Ttl74390.PORT_INDEX_1Q1,
    Ttl74390.PORT_INDEX_1Q2,
    Ttl74390.PORT_INDEX_1Q3,
    Ttl74390.PORT_INDEX_2Q0,
    Ttl74390.PORT_INDEX_2Q1,
    Ttl74390.PORT_INDEX_2Q2,
    Ttl74390.PORT_INDEX_2Q3
  };

  @Test
  void logicalPortsFollowTheDatasheetPinout() {
    final var gate = new Ttl74390();
    final var hiddenPower = createInstance(gate, false);

    assertEquals(14, hiddenPower.getPorts().size());
    assertPort(hiddenPower, Ttl74390.PORT_INDEX_1CP0, 10, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74390.PORT_INDEX_1MR, 30, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74390.PORT_INDEX_1Q0, 50, 30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74390.PORT_INDEX_1CP1, 70, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74390.PORT_INDEX_1Q1, 90, 30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74390.PORT_INDEX_1Q2, 110, 30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74390.PORT_INDEX_1Q3, 130, 30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74390.PORT_INDEX_2Q3, 150, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74390.PORT_INDEX_2Q2, 130, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74390.PORT_INDEX_2Q1, 110, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74390.PORT_INDEX_2CP1, 90, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74390.PORT_INDEX_2Q0, 70, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74390.PORT_INDEX_2MR, 50, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74390.PORT_INDEX_2CP0, 30, -30, EndData.INPUT_ONLY);

    final var shownPower = createInstance(gate, true);
    assertEquals(16, shownPower.getPorts().size());
    assertPort(shownPower, GND_PORT, 150, 30, EndData.INPUT_ONLY);
    assertPort(shownPower, VCC_PORT, 10, -30, EndData.INPUT_ONLY);
  }

  @Test
  void divideByTwoAndDivideByFiveCountIndependently() {
    final var gate = new Ttl74390();
    final var state = new TestInstanceState(gate, false);
    reset(gate, state);

    pulse(gate, state, Ttl74390.PORT_INDEX_1CP0);
    assertEquals(1, counterValue(state, 1));
    pulse(gate, state, Ttl74390.PORT_INDEX_1CP0);
    assertEquals(0, counterValue(state, 1));
    pulse(gate, state, Ttl74390.PORT_INDEX_1CP0);
    assertEquals(1, counterValue(state, 1));
    assertEquals(0, counterValue(state, 2));

    reset(gate, state);
    for (final var expected : new int[] {2, 4, 6, 8, 0}) {
      pulse(gate, state, Ttl74390.PORT_INDEX_1CP1);
      assertEquals(expected, counterValue(state, 1));
    }
    assertEquals(0, counterValue(state, 2));
  }

  @Test
  void bcdCascadeCountsFromZeroThroughNine() {
    final var gate = new Ttl74390();
    final var state = new TestInstanceState(gate, false);
    reset(gate, state);

    for (var step = 1; step <= 10; step++) {
      bcdClock(gate, state);
      assertEquals(step % 10, counterValue(state, 1));
      assertEquals(0, counterValue(state, 2));
    }
  }

  @Test
  void biQuinaryCascadeFollowsTheDatasheetSequence() {
    final var gate = new Ttl74390();
    final var state = new TestInstanceState(gate, false);
    reset(gate, state);
    pulse(gate, state, Ttl74390.PORT_INDEX_1CP0);

    for (final var expected : BIQUINARY) {
      biQuinaryClock(gate, state);
      assertEquals(expected, counterValue(state, 2));
      assertEquals(1, counterValue(state, 1));
    }
  }

  @Test
  void masterResetClearsOnlyItsHalfAndOverridesTheClock() {
    final var gate = new Ttl74390();
    final var state = new TestInstanceState(gate, false);
    reset(gate, state);
    pulse(gate, state, Ttl74390.PORT_INDEX_1CP0);
    pulse(gate, state, Ttl74390.PORT_INDEX_2CP0);

    state.setPortValue(Ttl74390.PORT_INDEX_1CP0, Value.TRUE);
    gate.propagate(state);
    state.setPortValue(Ttl74390.PORT_INDEX_1MR, Value.TRUE);
    state.setPortValue(Ttl74390.PORT_INDEX_1CP0, Value.FALSE);
    gate.propagate(state);
    assertEquals(0, counterValue(state, 1));
    assertEquals(1, counterValue(state, 2));

    state.setPortValue(Ttl74390.PORT_INDEX_2MR, Value.UNKNOWN);
    gate.propagate(state);
    assertEquals(1, counterValue(state, 2));

    state.setPortValue(Ttl74390.PORT_INDEX_1MR, Value.FALSE);
    gate.propagate(state);
    pulse(gate, state, Ttl74390.PORT_INDEX_1CP0);
    assertEquals(1, counterValue(state, 1));
  }

  @Test
  void risingAndUnknownClocksDoNotCount() {
    final var gate = new Ttl74390();
    final var state = new TestInstanceState(gate, false);
    reset(gate, state);

    state.setPortValue(Ttl74390.PORT_INDEX_1CP0, Value.TRUE);
    gate.propagate(state);
    assertEquals(0, counterValue(state, 1));

    state.setPortValue(Ttl74390.PORT_INDEX_1CP0, Value.UNKNOWN);
    gate.propagate(state);
    state.setPortValue(Ttl74390.PORT_INDEX_1CP0, Value.FALSE);
    gate.propagate(state);
    assertEquals(0, counterValue(state, 1));
  }

  @Test
  void unknownResetDoesNotClearAndAFallingEdgeStillCounts() {
    final var gate = new Ttl74390();
    final var state = new TestInstanceState(gate, false);
    reset(gate, state);
    pulse(gate, state, Ttl74390.PORT_INDEX_1CP0);
    assertEquals(1, counterValue(state, 1));

    state.setPortValue(Ttl74390.PORT_INDEX_1MR, Value.UNKNOWN);
    gate.propagate(state);
    assertEquals(1, counterValue(state, 1));

    pulse(gate, state, Ttl74390.PORT_INDEX_1CP0);
    assertEquals(0, counterValue(state, 1));
  }

  @Test
  void undefinedCountBecomesUnknownOnTheNextClock() {
    final var gate = new Ttl74390();
    final var state = new TestInstanceState(gate, false);
    reset(gate, state);
    final var data = (TtlRegisterData) state.getData();
    data.setValue(0, Value.createUnknown(data.getWidth()));

    pulse(gate, state, Ttl74390.PORT_INDEX_1CP0);
    assertEquals(Value.UNKNOWN, state.getPortValue(Ttl74390.PORT_INDEX_1Q0));
    pulse(gate, state, Ttl74390.PORT_INDEX_1CP1);
    assertEquals(Value.UNKNOWN, state.getPortValue(Ttl74390.PORT_INDEX_1Q1));
    assertEquals(Value.UNKNOWN, state.getPortValue(Ttl74390.PORT_INDEX_1Q2));
    assertEquals(Value.UNKNOWN, state.getPortValue(Ttl74390.PORT_INDEX_1Q3));

    state.setPortValue(Ttl74390.PORT_INDEX_1MR, Value.TRUE);
    gate.propagate(state);
    assertEquals(0, counterValue(state, 1));
  }

  @Test
  void errorInTheCountBecomesErrorOnTheNextClock() {
    final var gate = new Ttl74390();
    final var state = new TestInstanceState(gate, false);
    reset(gate, state);
    final var data = (TtlRegisterData) state.getData();
    final var divideByTwo = data.getValue(0).getAll();
    divideByTwo[0] = Value.ERROR;
    data.setValue(0, Value.create(divideByTwo));

    pulse(gate, state, Ttl74390.PORT_INDEX_1CP0);
    assertEquals(Value.ERROR, state.getPortValue(Ttl74390.PORT_INDEX_1Q0));
    assertEquals(Value.FALSE, state.getPortValue(Ttl74390.PORT_INDEX_1Q1));

    final var divideByFive = Value.createKnown(data.getWidth(), 0).getAll();
    divideByFive[1] = Value.ERROR;
    data.setValue(0, Value.create(divideByFive));
    pulse(gate, state, Ttl74390.PORT_INDEX_1CP1);
    assertEquals(Value.FALSE, state.getPortValue(Ttl74390.PORT_INDEX_1Q0));
    assertEquals(Value.ERROR, state.getPortValue(Ttl74390.PORT_INDEX_1Q1));
    assertEquals(Value.ERROR, state.getPortValue(Ttl74390.PORT_INDEX_1Q2));
    assertEquals(Value.ERROR, state.getPortValue(Ttl74390.PORT_INDEX_1Q3));

    state.setPortValue(Ttl74390.PORT_INDEX_1MR, Value.TRUE);
    gate.propagate(state);
    assertEquals(0, counterValue(state, 1));
  }

  @Test
  void invalidExposedPowerInputsMakeOutputsUnknown() {
    final var gate = new Ttl74390();
    final var state = new TestInstanceState(gate, true);
    state.setPortValue(GND_PORT, Value.FALSE);
    state.setPortValue(VCC_PORT, Value.TRUE);
    reset(gate, state);
    pulse(gate, state, Ttl74390.PORT_INDEX_1CP0);
    assertEquals(1, counterValue(state, 1));

    state.setPortValue(VCC_PORT, Value.FALSE);
    gate.propagate(state);
    assertUnknownOutputs(state);

    state.setPortValue(VCC_PORT, Value.TRUE);
    gate.propagate(state);
    assertEquals(1, counterValue(state, 1));

    state.setPortValue(GND_PORT, Value.TRUE);
    gate.propagate(state);
    assertUnknownOutputs(state);
  }

  private static Instance createInstance(InstanceFactory factory, boolean showPowerPins) {
    return new TestInstanceState(factory, showPowerPins).getInstance();
  }

  private static void assertPort(Instance instance, int index, int x, int y, int type) {
    assertEquals(Location.create(x, y, false), instance.getPortLocation(index));
    assertEquals(type, instance.getPorts().get(index).getType());
  }

  private static void reset(Ttl74390 gate, TestInstanceState state) {
    state.setPortValue(Ttl74390.PORT_INDEX_1CP0, Value.FALSE);
    state.setPortValue(Ttl74390.PORT_INDEX_1CP1, Value.FALSE);
    state.setPortValue(Ttl74390.PORT_INDEX_2CP0, Value.FALSE);
    state.setPortValue(Ttl74390.PORT_INDEX_2CP1, Value.FALSE);
    state.setPortValue(Ttl74390.PORT_INDEX_1MR, Value.TRUE);
    state.setPortValue(Ttl74390.PORT_INDEX_2MR, Value.TRUE);
    gate.propagate(state);
    state.setPortValue(Ttl74390.PORT_INDEX_1MR, Value.FALSE);
    state.setPortValue(Ttl74390.PORT_INDEX_2MR, Value.FALSE);
    gate.propagate(state);
  }

  private static void pulse(Ttl74390 gate, TestInstanceState state, int clockPort) {
    state.setPortValue(clockPort, Value.TRUE);
    gate.propagate(state);
    state.setPortValue(clockPort, Value.FALSE);
    gate.propagate(state);
  }

  /** External BCD wiring: 1Q0 drives 1CP1, and the counter is clocked on 1CP0. */
  private static void bcdClock(Ttl74390 gate, TestInstanceState state) {
    state.setPortValue(Ttl74390.PORT_INDEX_1CP1, state.getPortValue(Ttl74390.PORT_INDEX_1Q0));
    state.setPortValue(Ttl74390.PORT_INDEX_1CP0, Value.TRUE);
    gate.propagate(state);
    state.setPortValue(Ttl74390.PORT_INDEX_1CP1, state.getPortValue(Ttl74390.PORT_INDEX_1Q0));
    state.setPortValue(Ttl74390.PORT_INDEX_1CP0, Value.FALSE);
    gate.propagate(state);
    state.setPortValue(Ttl74390.PORT_INDEX_1CP1, state.getPortValue(Ttl74390.PORT_INDEX_1Q0));
    gate.propagate(state);
  }

  /** External bi-quinary wiring: 2Q3 drives 2CP0, and the counter is clocked on 2CP1. */
  private static void biQuinaryClock(Ttl74390 gate, TestInstanceState state) {
    state.setPortValue(Ttl74390.PORT_INDEX_2CP0, state.getPortValue(Ttl74390.PORT_INDEX_2Q3));
    state.setPortValue(Ttl74390.PORT_INDEX_2CP1, Value.TRUE);
    gate.propagate(state);
    state.setPortValue(Ttl74390.PORT_INDEX_2CP0, state.getPortValue(Ttl74390.PORT_INDEX_2Q3));
    state.setPortValue(Ttl74390.PORT_INDEX_2CP1, Value.FALSE);
    gate.propagate(state);
    state.setPortValue(Ttl74390.PORT_INDEX_2CP0, state.getPortValue(Ttl74390.PORT_INDEX_2Q3));
    gate.propagate(state);
  }

  private static int counterValue(TestInstanceState state, int half) {
    final var q0 = half == 1 ? Ttl74390.PORT_INDEX_1Q0 : Ttl74390.PORT_INDEX_2Q0;
    final var q1 = half == 1 ? Ttl74390.PORT_INDEX_1Q1 : Ttl74390.PORT_INDEX_2Q1;
    final var q2 = half == 1 ? Ttl74390.PORT_INDEX_1Q2 : Ttl74390.PORT_INDEX_2Q2;
    final var q3 = half == 1 ? Ttl74390.PORT_INDEX_1Q3 : Ttl74390.PORT_INDEX_2Q3;
    return (int)
        (state.getPortValue(q0).toLongValue()
            | state.getPortValue(q1).toLongValue() << 1
            | state.getPortValue(q2).toLongValue() << 2
            | state.getPortValue(q3).toLongValue() << 3);
  }

  private static void assertUnknownOutputs(TestInstanceState state) {
    for (final var port : OUTPUT_PORTS) {
      assertEquals(Value.UNKNOWN, state.getPortValue(port));
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
