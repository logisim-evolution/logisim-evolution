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
import static org.junit.jupiter.api.Assertions.assertThrows;
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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Functional tests for the 74HC123 dual retriggerable monostable. */
class Ttl74123Test {
  /** Datasheet point Rext = 10 kΩ, Cext = 100 nF, which is also the component default. */
  private static final long PULSE_NANOS = 450_000L;

  /** Clock read by the component while a test propagates or pauses. */
  private long now;
  private static final int GND_PORT = 10;
  private static final int VCC_PORT = 11;
  private static final int[] OUTPUT_PORTS = {
    Ttl74123.PORT_INDEX_1Q,
    Ttl74123.PORT_INDEX_1QBAR,
    Ttl74123.PORT_INDEX_2Q,
    Ttl74123.PORT_INDEX_2QBAR
  };

  @BeforeEach
  void installClock() {
    now = 0;
    Ttl74123.setClock(() -> now);
  }

  @AfterEach
  void restoreClock() {
    Ttl74123.setClock(null);
  }

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
  void widthNanosMatchesTheDatasheetExample() {
    assertEquals(PULSE_NANOS, Ttl74123.widthNanos(10, 100_000));
    assertEquals(9_000_000_000L, Ttl74123.widthNanos(1000, 20_000_000));
    assertEquals(1L, Ttl74123.widthNanos(0, 0));
    assertEquals(450_000_000_000L, Ttl74123.widthNanos(1000, 1_000_000_000));
    assertEquals("450 µs", Ttl74123.formatWidth(PULSE_NANOS));
    assertEquals("9 µs", Ttl74123.formatWidth(9_000L));
    assertEquals("1.5 ms", Ttl74123.formatWidth(1_500_000L));
    assertEquals("9 s", Ttl74123.formatWidth(9_000_000_000L));
    assertEquals("500 ns", Ttl74123.formatWidth(500L));
  }

  @Test
  void timingValuesAcceptPrefixesAndSavedNumbers() {
    final var resistor = Ttl74123RcAttribute.Kind.RESISTOR;
    final var capacitor = Ttl74123RcAttribute.Kind.CAPACITOR;
    assertEquals(10, Ttl74123RcAttribute.parse(resistor, "10"));
    assertEquals(10, Ttl74123RcAttribute.parse(resistor, "10k"));
    assertEquals(10, Ttl74123RcAttribute.parse(resistor, "10 kΩ"));
    assertEquals(10, Ttl74123RcAttribute.parse(resistor, "1.0e4"));
    assertEquals(1000, Ttl74123RcAttribute.parse(resistor, "1M"));
    assertEquals(1000, Ttl74123RcAttribute.parse(resistor, "1meg"));
    assertEquals(100_000, Ttl74123RcAttribute.parse(capacitor, "100000"));
    assertEquals(100_000, Ttl74123RcAttribute.parse(capacitor, "100n"));
    assertEquals(100_000, Ttl74123RcAttribute.parse(capacitor, "100 nF"));
    assertEquals(1_000_000, Ttl74123RcAttribute.parse(capacitor, "1u"));
    assertEquals(1_000_000, Ttl74123RcAttribute.parse(capacitor, "1 u"));
    assertEquals(1_000_000, Ttl74123RcAttribute.parse(capacitor, "1µF"));
    assertEquals(1_000_000, Ttl74123RcAttribute.parse(capacitor, "1.0e-6"));
    assertEquals(6_666_667, Ttl74123RcAttribute.parse(capacitor, "6.666667u"));
    assertEquals("10 kΩ", Ttl74123RcAttribute.format(resistor, 10));
    assertEquals("1 MΩ", Ttl74123RcAttribute.format(resistor, 1000));
    assertEquals("100 nF", Ttl74123RcAttribute.format(capacitor, 100_000));
    assertEquals("1000 µF", Ttl74123RcAttribute.format(capacitor, 1_000_000_000));
    assertEquals(10, Ttl74123.REXT_1.parse(Ttl74123.REXT_1.toDisplayString(10)));
    assertThrows(NumberFormatException.class, () -> Ttl74123RcAttribute.parse(resistor, "1"));
    assertThrows(NumberFormatException.class, () -> Ttl74123RcAttribute.parse(capacitor, "1n"));
    assertThrows(NumberFormatException.class, () -> Ttl74123RcAttribute.parse(capacitor, "abc"));
  }

  /** The attribute table writes the shown text back, so the display must parse to the same value. */
  @Test
  void timingValuesSurviveADisplayRoundTrip() {
    final var resistor = Ttl74123RcAttribute.Kind.RESISTOR;
    final var capacitor = Ttl74123RcAttribute.Kind.CAPACITOR;
    assertEquals("10.001 nF", Ttl74123RcAttribute.format(capacitor, 10_001));
    assertEquals("999.999 nF", Ttl74123RcAttribute.format(capacitor, 999_999));
    assertEquals("1.5 µF", Ttl74123RcAttribute.format(capacitor, 1_500_000));
    assertEquals("6.666667 µF", Ttl74123RcAttribute.format(capacitor, 6_666_667));

    final int[] resistorValues = {2, 10, 100, 999, 1000};
    for (final var stored : resistorValues) {
      final var shown = Ttl74123RcAttribute.format(resistor, stored);
      assertEquals(stored, Ttl74123RcAttribute.parse(resistor, shown));
    }
    final int[] capacitorValues = {
      10_000, 10_001, 100_000, 999_999, 1_000_000, 1_500_000, 6_666_667, 1_000_000_000
    };
    for (final var stored : capacitorValues) {
      final var shown = Ttl74123RcAttribute.format(capacitor, stored);
      assertEquals(stored, Ttl74123RcAttribute.parse(capacitor, shown));
    }

    assertEquals("", Ttl74123.CEXT_1.toStandardString(null));
    assertEquals("100000", Ttl74123.CEXT_1.toStandardString(100_000));
  }

  /** The words ohm, ohms, farad, and farads name the unit in any letter case. */
  @Test
  void timingValuesAcceptUnitWords() {
    final var resistor = Ttl74123RcAttribute.Kind.RESISTOR;
    final var capacitor = Ttl74123RcAttribute.Kind.CAPACITOR;
    assertEquals(10, Ttl74123RcAttribute.parse(resistor, "10kohm"));
    assertEquals(10, Ttl74123RcAttribute.parse(resistor, "10 kOhms"));
    assertEquals(10, Ttl74123RcAttribute.parse(resistor, "10000 ohm"));
    assertEquals(10, Ttl74123RcAttribute.parse(resistor, "10000 OHMS"));
    assertEquals(100_000, Ttl74123RcAttribute.parse(capacitor, "100nfarad"));
    assertEquals(100_000, Ttl74123RcAttribute.parse(capacitor, "100 nFarads"));
    assertEquals(1_000_000, Ttl74123RcAttribute.parse(capacitor, "1ufarad"));
    assertEquals(1_000_000, Ttl74123RcAttribute.parse(capacitor, "1 uFARADS"));
  }

  /** The farad symbol is a capital F, so {@code fF} is femtofarad and {@code ff} is not. */
  @Test
  void timingValuesRequireCapitalFaradSymbol() {
    final var capacitor = Ttl74123RcAttribute.Kind.CAPACITOR;
    assertEquals(10_000, Ttl74123RcAttribute.parse(capacitor, "10000000fF"));
    assertEquals(100_000, Ttl74123RcAttribute.parse(capacitor, "100nF"));
    assertThrows(NumberFormatException.class, () -> Ttl74123RcAttribute.parse(capacitor, "100nf"));
    assertThrows(NumberFormatException.class, () -> Ttl74123RcAttribute.parse(capacitor, "10000000ff"));
  }

  /** Upper-case F, P, N, U, and K are not SI prefixes. */
  @Test
  void timingValuesRejectUppercaseSiPrefixes() {
    final var resistor = Ttl74123RcAttribute.Kind.RESISTOR;
    final var capacitor = Ttl74123RcAttribute.Kind.CAPACITOR;
    assertEquals(10_000, Ttl74123RcAttribute.parse(capacitor, "10000000f"));
    assertEquals(10_000_000, Ttl74123RcAttribute.parse(capacitor, "10000000p"));
    assertThrows(NumberFormatException.class, () -> Ttl74123RcAttribute.parse(resistor, "10K"));
    assertThrows(NumberFormatException.class, () -> Ttl74123RcAttribute.parse(capacitor, "1U"));
    assertThrows(NumberFormatException.class, () -> Ttl74123RcAttribute.parse(capacitor, "100N"));
    assertThrows(NumberFormatException.class, () -> Ttl74123RcAttribute.parse(capacitor, "10000000P"));
    assertThrows(NumberFormatException.class, () -> Ttl74123RcAttribute.parse(capacitor, "10000000F"));
  }

  /** A hex float or a d/f literal suffix is not a resistor value, although Java would read it. */
  @Test
  void timingValuesRejectJavaNumberSyntax() {
    final var resistor = Ttl74123RcAttribute.Kind.RESISTOR;
    assertThrows(NumberFormatException.class, () -> Ttl74123RcAttribute.parse(resistor, "0x1p3"));
    assertThrows(NumberFormatException.class, () -> Ttl74123RcAttribute.parse(resistor, "10d"));
    assertThrows(NumberFormatException.class, () -> Ttl74123RcAttribute.parse(resistor, "10D"));
  }

  @Test
  void risingBTriggersWhenAIsLowAndResetIsHigh() {
    final var gate = gate();
    final var state = arm(gate, false, false);
    now = 1_000;

    state.setPortValue(Ttl74123.PORT_INDEX_1B, Value.TRUE);
    gate.propagate(state);

    assertPulse(state, 1, Value.TRUE);
    assertPulse(state, 2, Value.FALSE);
    assertFalse(gate.expire(state, now + PULSE_NANOS - 1));
    gate.propagate(state);
    assertPulse(state, 1, Value.TRUE);
    assertTrue(gate.expire(state, now + PULSE_NANOS));
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
    now = 1_000;
    state.setPortValue(Ttl74123.PORT_INDEX_1B, Value.TRUE);
    gate.propagate(state);

    now = 5_000;
    state.setPortValue(Ttl74123.PORT_INDEX_1B, Value.FALSE);
    gate.propagate(state);
    assertPulse(state, 1, Value.TRUE);
    state.setPortValue(Ttl74123.PORT_INDEX_1B, Value.TRUE);
    gate.propagate(state);

    assertFalse(gate.expire(state, now + PULSE_NANOS - 1));
    gate.propagate(state);
    assertPulse(state, 1, Value.TRUE);
    assertTrue(gate.expire(state, now + PULSE_NANOS));
    gate.propagate(state);
    assertPulse(state, 1, Value.FALSE);
  }

  @Test
  void stableInputLevelsDoNotAbortAnActivePulse() {
    final var gate = gate();
    final var state = arm(gate, false, false);
    now = 1_000;
    state.setPortValue(Ttl74123.PORT_INDEX_1B, Value.TRUE);
    gate.propagate(state);

    state.setPortValue(Ttl74123.PORT_INDEX_1A, Value.TRUE);
    gate.propagate(state);
    state.setPortValue(Ttl74123.PORT_INDEX_1B, Value.FALSE);
    gate.propagate(state);
    assertPulse(state, 1, Value.TRUE);

    assertFalse(gate.expire(state, now + PULSE_NANOS - 1));
    assertTrue(gate.expire(state, now + PULSE_NANOS));
    gate.propagate(state);
    assertPulse(state, 1, Value.FALSE);
  }

  @Test
  void timingAttributesApplyOnTheNextTrigger() {
    final var gate = gate();
    final var state = arm(gate, false, false);
    now = 1_000;
    state.setPortValue(Ttl74123.PORT_INDEX_1B, Value.TRUE);
    gate.propagate(state);

    state.getAttributeSet().setValue(Ttl74123.CEXT_1, 10_000);
    assertFalse(gate.expire(state, now + PULSE_NANOS - 1));
    gate.propagate(state);
    assertPulse(state, 1, Value.TRUE);

    assertTrue(gate.expire(state, now + PULSE_NANOS));
    gate.propagate(state);
    now = 2_000;
    state.setPortValue(Ttl74123.PORT_INDEX_1B, Value.FALSE);
    gate.propagate(state);
    state.setPortValue(Ttl74123.PORT_INDEX_1B, Value.TRUE);
    gate.propagate(state);
    final var shortened = Ttl74123.widthNanos(10, 10_000);
    assertFalse(gate.expire(state, now + shortened - 1));
    assertTrue(gate.expire(state, now + shortened));
  }

  @Test
  void stoppedSimulationFreezesTheRemainingPulse() {
    final var gate = gate();
    final var state = arm(gate, false, false);
    now = 1_000;
    state.setPortValue(Ttl74123.PORT_INDEX_1B, Value.TRUE);
    gate.propagate(state);

    now = 1_000 + 100_000;
    gate.setSimulationRunning(state, false);
    assertFalse(gate.expire(state, now + PULSE_NANOS));
    gate.propagate(state);
    assertPulse(state, 1, Value.TRUE);

    final var resumeAt = 50_000_000L;
    now = resumeAt;
    gate.setSimulationRunning(state, true);
    final var remaining = PULSE_NANOS - 100_000;
    assertFalse(gate.expire(state, resumeAt + remaining - 1));
    gate.propagate(state);
    assertPulse(state, 1, Value.TRUE);
    assertTrue(gate.expire(state, resumeAt + remaining));
    gate.propagate(state);
    assertPulse(state, 1, Value.FALSE);
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

  /** Records a stable input level so the next transition can be recognized as an edge. */
  private static TestInstanceState arm(Ttl74123 gate, boolean inputA, boolean inputB) {
    final var state = new TestInstanceState(gate, false);
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
