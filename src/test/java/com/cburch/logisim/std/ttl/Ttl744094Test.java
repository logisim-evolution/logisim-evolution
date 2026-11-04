/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.std.ttl;

import static com.cburch.logisim.std.ttl.TtlTestInstanceState.createInstance;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.cburch.logisim.comp.EndData;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.instance.Instance;
import org.junit.jupiter.api.Test;

/** Functional tests for the 74HC4094 shift-and-store register with three-state outputs. */
class Ttl744094Test {
  private static final byte[] PARALLEL = {
    Ttl744094.QP0, Ttl744094.QP1, Ttl744094.QP2, Ttl744094.QP3,
    Ttl744094.QP4, Ttl744094.QP5, Ttl744094.QP6, Ttl744094.QP7
  };
  private static final int GND_PORT = 14;
  private static final int VCC_PORT = 15;

  @Test
  void logicalPortsFollowTheDatasheetPinout() {
    final var gate = new Ttl744094();
    final var hiddenPower = createInstance(gate, false);

    assertEquals(14, hiddenPower.getPorts().size());
    assertPort(hiddenPower, Ttl744094.STR, 10, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl744094.D, 30, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl744094.CP, 50, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl744094.QP0, 70, 30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl744094.QP1, 90, 30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl744094.QP2, 110, 30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl744094.QP3, 130, 30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl744094.QS1, 150, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl744094.QS2, 130, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl744094.QP7, 110, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl744094.QP6, 90, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl744094.QP5, 70, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl744094.QP4, 50, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl744094.OE, 30, -30, EndData.INPUT_ONLY);

    final var shownPower = createInstance(gate, true);
    assertEquals(16, shownPower.getPorts().size());
    assertEquals(Location.create(150, 30, false), shownPower.getPortLocation(GND_PORT));
    assertEquals(Location.create(10, -30, false), shownPower.getPortLocation(VCC_PORT));
  }

  @Test
  void transparentStrobeUpdatesOnTheRisingClockAndDelaysQs2() {
    final var gate = new Ttl744094();
    final var state = flushed(gate);

    setLevel(state, Ttl744094.D, Value.TRUE);
    rise(gate, state);
    assertParallel(state, 0x01);
    assertEquals(Value.FALSE, level(state, Ttl744094.QS1));
    assertEquals(Value.FALSE, level(state, Ttl744094.QS2));
    fall(gate, state);
    assertEquals(Value.FALSE, level(state, Ttl744094.QS2));

    setLevel(state, Ttl744094.D, Value.FALSE);
    for (var step = 0; step < 6; step++) {
      pulse(gate, state);
    }
    assertEquals(Value.FALSE, level(state, Ttl744094.QS1));
    assertEquals(Value.FALSE, level(state, Ttl744094.QS2));

    rise(gate, state);
    assertEquals(Value.TRUE, level(state, Ttl744094.QS1));
    assertEquals(Value.FALSE, level(state, Ttl744094.QS2));
    assertParallel(state, 0x80);
    fall(gate, state);
    assertEquals(Value.TRUE, level(state, Ttl744094.QS1));
    assertEquals(Value.TRUE, level(state, Ttl744094.QS2));
  }

  @Test
  void lowStrobeHoldsParallelUntilStrobeRisesWithoutAClock() {
    final var gate = new Ttl744094();
    final var state = flushed(gate);

    shiftByte(gate, state, 0xA5);
    assertParallel(state, 0xA5);
    assertEquals(Value.TRUE, level(state, Ttl744094.QS1));
    assertEquals(Value.TRUE, level(state, Ttl744094.QS2));

    setLevel(state, Ttl744094.STR, Value.FALSE);
    gate.propagate(state);
    shiftByte(gate, state, 0x5A);
    assertParallel(state, 0xA5);
    assertEquals(Value.FALSE, level(state, Ttl744094.QS1));
    assertEquals(Value.FALSE, level(state, Ttl744094.QS2));

    setLevel(state, Ttl744094.STR, Value.TRUE);
    gate.propagate(state);
    assertParallel(state, 0x5A);
    setLevel(state, Ttl744094.STR, Value.FALSE);
    gate.propagate(state);
    setLevel(state, Ttl744094.D, Value.TRUE);
    pulse(gate, state);
    assertParallel(state, 0x5A);
    assertEquals(Value.TRUE, level(state, Ttl744094.QS1));
    assertEquals(Value.TRUE, level(state, Ttl744094.QS2));
  }

  @Test
  void unknownStrobeDoesNotOpenStorage() {
    final var gate = new Ttl744094();
    final var state = flushed(gate);
    shiftByte(gate, state, 0xA5);

    setLevel(state, Ttl744094.STR, Value.UNKNOWN);
    gate.propagate(state);
    setLevel(state, Ttl744094.D, Value.TRUE);
    pulse(gate, state);
    assertParallel(state, 0xA5);
    assertEquals(Value.FALSE, level(state, Ttl744094.QS1));
  }

  @Test
  void outputEnableReleasesOnlyTheParallelOutputs() {
    final var gate = new Ttl744094();
    final var state = flushed(gate);
    shiftByte(gate, state, 0xA5);
    assertEquals(Value.TRUE, level(state, Ttl744094.QS1));
    assertEquals(Value.TRUE, level(state, Ttl744094.QS2));

    setLevel(state, Ttl744094.OE, Value.FALSE);
    gate.propagate(state);
    assertParallelUnknown(state);
    assertEquals(Value.TRUE, level(state, Ttl744094.QS1));
    assertEquals(Value.TRUE, level(state, Ttl744094.QS2));

    setLevel(state, Ttl744094.OE, Value.TRUE);
    gate.propagate(state);
    assertParallel(state, 0xA5);

    setLevel(state, Ttl744094.OE, Value.UNKNOWN);
    gate.propagate(state);
    assertParallelUnknown(state);
    assertEquals(Value.TRUE, level(state, Ttl744094.QS1));

    setLevel(state, Ttl744094.OE, Value.ERROR);
    gate.propagate(state);
    assertParallelUnknown(state);
    assertEquals(Value.TRUE, level(state, Ttl744094.QS2));

    setLevel(state, Ttl744094.OE, Value.TRUE);
    gate.propagate(state);
    assertParallel(state, 0xA5);

    setLevel(state, Ttl744094.OE, Value.FALSE);
    setLevel(state, Ttl744094.D, Value.FALSE);
    pulse(gate, state);
    assertParallelUnknown(state);
    assertEquals(Value.FALSE, level(state, Ttl744094.QS1));
    assertEquals(Value.FALSE, level(state, Ttl744094.QS2));

    setLevel(state, Ttl744094.OE, Value.TRUE);
    gate.propagate(state);
    assertParallel(state, 0x4A);
  }

  @Test
  void heldClockLevelAndUnknownEdgesDoNotShift() {
    final var gate = new Ttl744094();
    final var state = flushed(gate);
    shiftByte(gate, state, 0xA5);

    setLevel(state, Ttl744094.D, Value.TRUE);
    rise(gate, state);
    setLevel(state, Ttl744094.CP, Value.TRUE);
    gate.propagate(state);
    assertParallel(state, 0x4B);
    assertEquals(Value.FALSE, level(state, Ttl744094.QS1));
    assertEquals(Value.TRUE, level(state, Ttl744094.QS2));
    fall(gate, state);
    assertEquals(Value.FALSE, level(state, Ttl744094.QS2));

    setLevel(state, Ttl744094.CP, Value.UNKNOWN);
    gate.propagate(state);
    setLevel(state, Ttl744094.CP, Value.FALSE);
    gate.propagate(state);
    assertParallel(state, 0x4B);
    assertEquals(Value.FALSE, level(state, Ttl744094.QS1));
    assertEquals(Value.FALSE, level(state, Ttl744094.QS2));
  }

  @Test
  void unknownAndErrorDataShiftThroughToTheSerialOutputs() {
    final var gate = new Ttl744094();
    final var state = flushed(gate);

    setLevel(state, Ttl744094.D, Value.UNKNOWN);
    pulse(gate, state);
    assertEquals(Value.UNKNOWN, level(state, Ttl744094.QP0));
    assertEquals(Value.FALSE, level(state, Ttl744094.QP1));
    assertEquals(Value.FALSE, level(state, Ttl744094.QS1));

    setLevel(state, Ttl744094.D, Value.ERROR);
    pulse(gate, state);
    assertEquals(Value.ERROR, level(state, Ttl744094.QP0));
    assertEquals(Value.UNKNOWN, level(state, Ttl744094.QP1));

    setLevel(state, Ttl744094.D, Value.FALSE);
    for (var extra = 0; extra < 5; extra++) {
      pulse(gate, state);
    }
    rise(gate, state);
    assertEquals(Value.UNKNOWN, level(state, Ttl744094.QS1));
    assertEquals(Value.FALSE, level(state, Ttl744094.QS2));
    fall(gate, state);
    assertEquals(Value.UNKNOWN, level(state, Ttl744094.QS2));

    pulse(gate, state);
    assertEquals(Value.ERROR, level(state, Ttl744094.QS1));
    assertEquals(Value.ERROR, level(state, Ttl744094.QS2));
    assertEquals(Value.FALSE, level(state, Ttl744094.QP6));
    assertEquals(Value.ERROR, level(state, Ttl744094.QP7));
  }

  @Test
  void invalidExposedPowerInputsMakeOutputsUnknown() {
    final var gate = new Ttl744094();
    final var state = new TtlTestInstanceState(gate, true);
    state.setPortValue(GND_PORT, Value.FALSE);
    state.setPortValue(VCC_PORT, Value.TRUE);
    arm(state);
    gate.propagate(state);
    flushZeros(gate, state);
    shiftByte(gate, state, 0xA5);
    assertParallel(state, 0xA5);

    state.setPortValue(VCC_PORT, Value.FALSE);
    gate.propagate(state);
    assertParallelUnknown(state);
    assertEquals(Value.UNKNOWN, level(state, Ttl744094.QS1));
    assertEquals(Value.UNKNOWN, level(state, Ttl744094.QS2));

    state.setPortValue(VCC_PORT, Value.TRUE);
    gate.propagate(state);
    assertParallel(state, 0xA5);
    assertEquals(Value.TRUE, level(state, Ttl744094.QS1));
    assertEquals(Value.TRUE, level(state, Ttl744094.QS2));

    state.setPortValue(GND_PORT, Value.TRUE);
    gate.propagate(state);
    assertParallelUnknown(state);
    assertEquals(Value.UNKNOWN, level(state, Ttl744094.QS1));
  }

  /** Leaves the controls idle, enables the outputs and fills both registers with zeros. */
  private static TtlTestInstanceState flushed(Ttl744094 gate) {
    final var state = new TtlTestInstanceState(gate, false);
    arm(state);
    gate.propagate(state);
    flushZeros(gate, state);
    return state;
  }

  private static void arm(TtlTestInstanceState state) {
    setLevel(state, Ttl744094.STR, Value.TRUE);
    setLevel(state, Ttl744094.OE, Value.TRUE);
    setLevel(state, Ttl744094.D, Value.FALSE);
    setLevel(state, Ttl744094.CP, Value.FALSE);
  }

  private static void flushZeros(Ttl744094 gate, TtlTestInstanceState state) {
    setLevel(state, Ttl744094.STR, Value.TRUE);
    setLevel(state, Ttl744094.D, Value.FALSE);
    for (var step = 0; step < 8; step++) {
      pulse(gate, state);
    }
  }

  /** Shifts eight bits, most significant bit first, with the strobe left as it is. */
  private static void shiftByte(Ttl744094 gate, TtlTestInstanceState state, int bits) {
    for (var bit = 7; bit >= 0; bit--) {
      setLevel(state, Ttl744094.D, ((bits >> bit) & 1) == 0 ? Value.FALSE : Value.TRUE);
      pulse(gate, state);
    }
  }

  private static void pulse(Ttl744094 gate, TtlTestInstanceState state) {
    rise(gate, state);
    fall(gate, state);
  }

  private static void rise(Ttl744094 gate, TtlTestInstanceState state) {
    setLevel(state, Ttl744094.CP, Value.TRUE);
    gate.propagate(state);
  }

  private static void fall(Ttl744094 gate, TtlTestInstanceState state) {
    setLevel(state, Ttl744094.CP, Value.FALSE);
    gate.propagate(state);
  }

  private static void assertParallel(TtlTestInstanceState state, int bits) {
    for (var stage = 0; stage < PARALLEL.length; stage++) {
      final var expected = ((bits >> stage) & 1) == 0 ? Value.FALSE : Value.TRUE;
      assertEquals(expected, level(state, PARALLEL[stage]), "stage " + stage);
    }
  }

  private static void assertParallelUnknown(TtlTestInstanceState state) {
    for (final var pin : PARALLEL) {
      assertEquals(Value.UNKNOWN, level(state, pin));
    }
  }

  private static void assertPort(Instance instance, byte dsPinNr, int x, int y, int type) {
    final var port = Ttl744094.pinNrToPortNr(dsPinNr);
    assertEquals(Location.create(x, y, false), instance.getPortLocation(port));
    assertEquals(type, instance.getPorts().get(port).getType());
  }

  private static void setLevel(TtlTestInstanceState state, byte dsPinNr, Value value) {
    state.setPortValue(Ttl744094.pinNrToPortNr(dsPinNr), value);
  }

  private static Value level(TtlTestInstanceState state, byte dsPinNr) {
    return state.getPortValue(Ttl744094.pinNrToPortNr(dsPinNr));
  }
}
