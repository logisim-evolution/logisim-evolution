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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

/** Functional tests for the 74HC123 dual retriggerable monostable. */
class Ttl74123Test {
  /** Datasheet point Rext = 10 kΩ, Cext = 100 nF is 450 µs, which is 9 ticks at 20 kHz. */
  private static final double DATASHEET_TICK_HZ = 20_000;
  /**
   * Unit tests have no simulator, so the component uses 1 Hz. These parts last 9 ticks at that
   * rate and stay inside the attribute limits.
   */
  private static final int NINE_TICK_REXT_KOHM = 1000;
  private static final int NINE_TICK_CEXT_PF = 20_000_000;
  private static final int PULSE_TICKS = 9;
  private static final int GND_PORT = 10;
  private static final int VCC_PORT = 11;
  private static final int[] OUTPUT_PORTS = {
    Ttl74123.PORT_INDEX_1Q,
    Ttl74123.PORT_INDEX_1QBAR,
    Ttl74123.PORT_INDEX_2Q,
    Ttl74123.PORT_INDEX_2QBAR
  };

  @Test
  void logicalPortsFollowTheDatasheetPinout() {
    final var gate = new Ttl74123();
    final var hiddenPower = createInstance(gate, false);

    assertEquals(10, hiddenPower.getPorts().size());
    assertPort(hiddenPower, Ttl74123.PORT_INDEX_1A, 10, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74123.PORT_INDEX_1B, 30, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74123.PORT_INDEX_1RD, 50, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74123.PORT_INDEX_1QBAR, 70, 30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74123.PORT_INDEX_2Q, 90, 30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74123.PORT_INDEX_2A, 150, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74123.PORT_INDEX_2B, 130, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74123.PORT_INDEX_2RD, 110, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74123.PORT_INDEX_2QBAR, 90, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74123.PORT_INDEX_1Q, 70, -30, EndData.OUTPUT_ONLY);

    final var shownPower = createInstance(gate, true);
    assertEquals(12, shownPower.getPorts().size());
    assertPort(shownPower, GND_PORT, 150, 30, EndData.INPUT_ONLY);
    assertPort(shownPower, VCC_PORT, 10, -30, EndData.INPUT_ONLY);
  }

  @Test
  void widthTicksMatchesTheDatasheetExample() {
    assertEquals(PULSE_TICKS, Ttl74123.widthTicks(10, 100_000, DATASHEET_TICK_HZ));
    assertEquals(
        PULSE_TICKS, Ttl74123.widthTicks(NINE_TICK_REXT_KOHM, NINE_TICK_CEXT_PF, 1));
    assertEquals(1, Ttl74123.widthTicks(10, 100_000, 1));
    assertEquals(1, Ttl74123.widthTicks(10, 100_000, 0));
    assertEquals(Integer.MAX_VALUE, Ttl74123.widthTicks(1000, 1_000_000_000, 1.0e12));
  }

  @Test
  void risingBTriggersWhenAIsLowAndResetIsHigh() {
    final var gate = gate();
    final var state = arm(gate, false, false);
    state.setTickCount(100);

    state.setPortValue(Ttl74123.PORT_INDEX_1B, Value.TRUE);
    gate.propagate(state);

    assertPulse(state, 1, Value.TRUE);
    assertPulse(state, 2, Value.FALSE);
    assertFalse(gate.expire(state, 108));
    gate.propagate(state);
    assertPulse(state, 1, Value.TRUE);
    assertTrue(gate.expire(state, 109));
    gate.propagate(state);
    assertPulse(state, 1, Value.FALSE);
  }

  @Test
  void fallingATriggersWhenBIsHighAndResetIsHigh() {
    final var gate = gate();
    final var state = arm(gate, true, true);
    assertPulse(state, 1, Value.FALSE);

    state.setPortValue(Ttl74123.PORT_INDEX_1A, Value.FALSE);
    gate.propagate(state);
    assertPulse(state, 1, Value.TRUE);
  }

  @Test
  void risingResetTriggersWhenAIsLowAndBIsHigh() {
    final var gate = gate();
    final var state = new TestInstanceState(gate, false);
    state.setPortValue(Ttl74123.PORT_INDEX_1A, Value.FALSE);
    state.setPortValue(Ttl74123.PORT_INDEX_1B, Value.TRUE);
    state.setPortValue(Ttl74123.PORT_INDEX_1RD, Value.FALSE);
    idleSecondHalf(state);
    gate.propagate(state);
    assertPulse(state, 1, Value.FALSE);

    state.setPortValue(Ttl74123.PORT_INDEX_1RD, Value.TRUE);
    gate.propagate(state);
    assertPulse(state, 1, Value.TRUE);
  }

  @Test
  void resetClearsThePulseImmediately() {
    final var gate = gate();
    final var state = arm(gate, false, false);
    state.setPortValue(Ttl74123.PORT_INDEX_1B, Value.TRUE);
    gate.propagate(state);
    assertPulse(state, 1, Value.TRUE);

    state.setPortValue(Ttl74123.PORT_INDEX_1RD, Value.FALSE);
    gate.propagate(state);
    assertPulse(state, 1, Value.FALSE);
  }

  @Test
  void retriggerRestartsTheCapturedWidth() {
    final var gate = gate();
    final var state = arm(gate, false, false);
    state.setTickCount(100);
    state.setPortValue(Ttl74123.PORT_INDEX_1B, Value.TRUE);
    gate.propagate(state);

    state.setTickCount(104);
    state.setPortValue(Ttl74123.PORT_INDEX_1B, Value.FALSE);
    gate.propagate(state);
    assertPulse(state, 1, Value.TRUE);
    state.setPortValue(Ttl74123.PORT_INDEX_1B, Value.TRUE);
    gate.propagate(state);

    assertFalse(gate.expire(state, 112));
    gate.propagate(state);
    assertPulse(state, 1, Value.TRUE);
    assertTrue(gate.expire(state, 113));
    gate.propagate(state);
    assertPulse(state, 1, Value.FALSE);
  }

  @Test
  void stableInputLevelsDoNotAbortAnActivePulse() {
    final var gate = gate();
    final var state = arm(gate, false, false);
    state.setTickCount(100);
    state.setPortValue(Ttl74123.PORT_INDEX_1B, Value.TRUE);
    gate.propagate(state);

    state.setPortValue(Ttl74123.PORT_INDEX_1A, Value.TRUE);
    gate.propagate(state);
    state.setPortValue(Ttl74123.PORT_INDEX_1B, Value.FALSE);
    gate.propagate(state);
    assertPulse(state, 1, Value.TRUE);

    assertFalse(gate.expire(state, 108));
    assertTrue(gate.expire(state, 109));
    gate.propagate(state);
    assertPulse(state, 1, Value.FALSE);
  }

  @Test
  void timingAttributesApplyOnTheNextTrigger() {
    final var gate = gate();
    final var state = arm(gate, false, false);
    state.setTickCount(100);
    state.setPortValue(Ttl74123.PORT_INDEX_1B, Value.TRUE);
    gate.propagate(state);

    state.getAttributeSet().setValue(Ttl74123.CEXT_1, 10_000);
    assertFalse(gate.expire(state, 101));
    gate.propagate(state);
    assertPulse(state, 1, Value.TRUE);

    assertTrue(gate.expire(state, 109));
    gate.propagate(state);
    state.setTickCount(200);
    state.setPortValue(Ttl74123.PORT_INDEX_1B, Value.FALSE);
    gate.propagate(state);
    state.setPortValue(Ttl74123.PORT_INDEX_1B, Value.TRUE);
    gate.propagate(state);
    assertFalse(gate.expire(state, 200));
    assertTrue(gate.expire(state, 201));
  }

  @Test
  void halvesTriggerIndependently() {
    final var gate = gate();
    final var state = arm(gate, false, false);
    state.setPortValue(Ttl74123.PORT_INDEX_1B, Value.TRUE);
    gate.propagate(state);
    assertPulse(state, 1, Value.TRUE);
    assertPulse(state, 2, Value.FALSE);

    state.setPortValue(Ttl74123.PORT_INDEX_2B, Value.TRUE);
    gate.propagate(state);
    assertPulse(state, 1, Value.TRUE);
    assertPulse(state, 2, Value.TRUE);
  }

  @Test
  void undefinedLevelsDoNotCreateEdgesOrClearAPulse() {
    final var gate = gate();
    final var state = arm(gate, false, false);

    state.setPortValue(Ttl74123.PORT_INDEX_1B, Value.UNKNOWN);
    gate.propagate(state);
    state.setPortValue(Ttl74123.PORT_INDEX_1B, Value.TRUE);
    gate.propagate(state);
    assertPulse(state, 1, Value.FALSE);

    state.setPortValue(Ttl74123.PORT_INDEX_1B, Value.FALSE);
    gate.propagate(state);
    state.setPortValue(Ttl74123.PORT_INDEX_1B, Value.TRUE);
    gate.propagate(state);
    assertPulse(state, 1, Value.TRUE);

    state.setPortValue(Ttl74123.PORT_INDEX_1RD, Value.UNKNOWN);
    gate.propagate(state);
    assertPulse(state, 1, Value.TRUE);
    state.setPortValue(Ttl74123.PORT_INDEX_1RD, Value.ERROR);
    gate.propagate(state);
    assertPulse(state, 1, Value.TRUE);
  }

  @Test
  void gatedLevelsDoNotTrigger() {
    final var gate = gate();
    final var blockedByA = arm(gate, true, false);
    blockedByA.setPortValue(Ttl74123.PORT_INDEX_1B, Value.TRUE);
    gate.propagate(blockedByA);
    assertPulse(blockedByA, 1, Value.FALSE);

    final var blockedByB = arm(gate, true, false);
    blockedByB.setPortValue(Ttl74123.PORT_INDEX_1A, Value.FALSE);
    gate.propagate(blockedByB);
    assertPulse(blockedByB, 1, Value.FALSE);

    final var heldInReset = arm(gate, false, false);
    heldInReset.setPortValue(Ttl74123.PORT_INDEX_1RD, Value.FALSE);
    heldInReset.setPortValue(Ttl74123.PORT_INDEX_1B, Value.TRUE);
    gate.propagate(heldInReset);
    assertPulse(heldInReset, 1, Value.FALSE);
  }

  @Test
  void wrongPowerPinsForceUnknownOutputs() {
    final var gate = gate();
    final var state = new TestInstanceState(gate, true);
    idleSecondHalf(state);
    state.setPortValue(Ttl74123.PORT_INDEX_1A, Value.FALSE);
    state.setPortValue(Ttl74123.PORT_INDEX_1B, Value.FALSE);
    state.setPortValue(Ttl74123.PORT_INDEX_1RD, Value.TRUE);
    gate.propagate(state);
    for (final var port : OUTPUT_PORTS) {
      assertEquals(Value.UNKNOWN, state.getPortValue(port));
    }

    state.setPortValue(GND_PORT, Value.FALSE);
    state.setPortValue(VCC_PORT, Value.TRUE);
    gate.propagate(state);
    state.setPortValue(Ttl74123.PORT_INDEX_1B, Value.TRUE);
    gate.propagate(state);
    assertPulse(state, 1, Value.TRUE);
  }

  private static Ttl74123 gate() {
    return new Ttl74123();
  }

  private static void useNineTickTiming(TestInstanceState state) {
    state.getAttributeSet().setValue(Ttl74123.REXT_1, NINE_TICK_REXT_KOHM);
    state.getAttributeSet().setValue(Ttl74123.CEXT_1, NINE_TICK_CEXT_PF);
    state.getAttributeSet().setValue(Ttl74123.REXT_2, NINE_TICK_REXT_KOHM);
    state.getAttributeSet().setValue(Ttl74123.CEXT_2, NINE_TICK_CEXT_PF);
  }

  /** Records a stable input level so the next transition can be recognized as an edge. */
  private static TestInstanceState arm(Ttl74123 gate, boolean inputA, boolean inputB) {
    final var state = new TestInstanceState(gate, false);
    useNineTickTiming(state);
    state.setPortValue(Ttl74123.PORT_INDEX_1A, inputA ? Value.TRUE : Value.FALSE);
    state.setPortValue(Ttl74123.PORT_INDEX_1B, inputB ? Value.TRUE : Value.FALSE);
    state.setPortValue(Ttl74123.PORT_INDEX_1RD, Value.TRUE);
    idleSecondHalf(state);
    gate.propagate(state);
    assertPulse(state, 1, Value.FALSE);
    return state;
  }

  private static void idleSecondHalf(TestInstanceState state) {
    state.setPortValue(Ttl74123.PORT_INDEX_2A, Value.FALSE);
    state.setPortValue(Ttl74123.PORT_INDEX_2B, Value.FALSE);
    state.setPortValue(Ttl74123.PORT_INDEX_2RD, Value.TRUE);
  }

  private static void assertPulse(TestInstanceState state, int half, Value output) {
    final var q = half == 1 ? Ttl74123.PORT_INDEX_1Q : Ttl74123.PORT_INDEX_2Q;
    final var qBar = half == 1 ? Ttl74123.PORT_INDEX_1QBAR : Ttl74123.PORT_INDEX_2QBAR;
    assertEquals(output, state.getPortValue(q));
    assertEquals(output.not(), state.getPortValue(qBar));
  }

  private static Instance createInstance(InstanceFactory factory, boolean showPowerPins) {
    return new TestInstanceState(factory, showPowerPins).getInstance();
  }

  private static void assertPort(Instance instance, int index, int x, int y, int type) {
    assertEquals(Location.create(x, y, false), instance.getPortLocation(index));
    assertEquals(type, instance.getPorts().get(index).getType());
  }

  private static final class TestInstanceState implements InstanceState {
    private final AttributeSet attrs;
    private final Instance instance;
    private final Map<Integer, Value> portValues = new HashMap<>();
    private InstanceData data;
    private int tickCount;

    private TestInstanceState(InstanceFactory factory, boolean showPowerPins) {
      attrs = factory.createAttributeSet();
      attrs.setValue(TtlLibrary.VCC_GND, showPowerPins);
      instance =
          Instance.getInstanceFor(
              factory.createComponent(Location.create(0, 0, false), attrs));
    }

    private void setPortValue(int portIndex, Value value) {
      portValues.put(portIndex, value);
    }

    private void setTickCount(int ticks) {
      tickCount = ticks;
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
      return tickCount;
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
  }
}
