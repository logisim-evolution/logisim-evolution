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

/** Functional tests for the 74HC595 shift register with output latch and three-state outputs. */
class Ttl74595Test {
  private static final byte[] STORAGE = {
    Ttl74595.QA, Ttl74595.QB, Ttl74595.QC, Ttl74595.QD,
    Ttl74595.QE, Ttl74595.QF, Ttl74595.QG, Ttl74595.QH
  };
  private static final int GND_PORT = 14;
  private static final int VCC_PORT = 15;

  @Test
  void logicalPortsFollowTheDatasheetPinout() {
    final var gate = new Ttl74595();
    final var hiddenPower = createInstance(gate, false);

    assertEquals(14, hiddenPower.getPorts().size());
    assertPort(hiddenPower, Ttl74595.QB, 10, 30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74595.QC, 30, 30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74595.QD, 50, 30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74595.QE, 70, 30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74595.QF, 90, 30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74595.QG, 110, 30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74595.QH, 130, 30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74595.QHP, 150, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74595.SRCLR, 130, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74595.SRCLK, 110, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74595.RCLK, 90, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74595.OE, 70, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74595.SER, 50, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74595.QA, 30, -30, EndData.OUTPUT_ONLY);

    final var shownPower = createInstance(gate, true);
    assertEquals(16, shownPower.getPorts().size());
    assertEquals(Location.create(150, 30, false), shownPower.getPortLocation(GND_PORT));
    assertEquals(Location.create(10, -30, false), shownPower.getPortLocation(VCC_PORT));
  }

  @Test
  void shiftDoesNotChangeStorageUntilTheLatchClockRises() {
    final var gate = new Ttl74595();
    final var state = cleared(gate);

    // 0x59 shifted LSB first lands as 0x9A with QA as bit 0. QH' rises on the eighth shift.
    for (var bit = 0; bit < 8; bit++) {
      setLevel(state, Ttl74595.SER, ((0x59 >> bit) & 1) == 0 ? Value.FALSE : Value.TRUE);
      pulse(gate, state, Ttl74595.SRCLK);
      assertEquals(bit == 7 ? Value.TRUE : Value.FALSE, level(state, Ttl74595.QHP));
      assertParallel(state, 0x00);
    }

    pulse(gate, state, Ttl74595.RCLK);
    assertParallel(state, 0x9A);
    assertEquals(Value.TRUE, level(state, Ttl74595.QHP));
  }

  @Test
  void simultaneousClocksLatchTheValueFromBeforeTheShift() {
    final var gate = new Ttl74595();
    final var state = cleared(gate);

    setLevel(state, Ttl74595.SER, Value.TRUE);
    riseBoth(gate, state);
    assertParallel(state, 0x00);
    assertEquals(Value.FALSE, level(state, Ttl74595.QHP));

    setLevel(state, Ttl74595.SER, Value.FALSE);
    riseBoth(gate, state);
    assertParallel(state, 0x01);
    assertEquals(Value.FALSE, level(state, Ttl74595.QHP));
  }

  @Test
  void clearEmptiesTheShiftRegisterAndLeavesStorageAlone() {
    final var gate = new Ttl74595();
    final var state = cleared(gate);
    shiftByte(gate, state, 0x59);
    pulse(gate, state, Ttl74595.RCLK);
    assertParallel(state, 0x9A);

    setLevel(state, Ttl74595.SRCLR, Value.FALSE);
    gate.propagate(state);
    assertEquals(Value.FALSE, level(state, Ttl74595.QHP));
    assertParallel(state, 0x9A);

    setLevel(state, Ttl74595.SER, Value.TRUE);
    pulse(gate, state, Ttl74595.SRCLK);
    assertEquals(Value.FALSE, level(state, Ttl74595.QHP));
    assertParallel(state, 0x9A);

    setLevel(state, Ttl74595.SRCLR, Value.TRUE);
    gate.propagate(state);
    pulse(gate, state, Ttl74595.SRCLK);
    pulse(gate, state, Ttl74595.RCLK);
    assertParallel(state, 0x01);
  }

  @Test
  void heldClockLevelAndUnknownEdgesDoNotTransfer() {
    final var gate = new Ttl74595();
    final var state = cleared(gate);
    shiftByte(gate, state, 0x59);
    pulse(gate, state, Ttl74595.RCLK);

    setLevel(state, Ttl74595.SER, Value.TRUE);
    setLevel(state, Ttl74595.SRCLK, Value.TRUE);
    gate.propagate(state);
    setLevel(state, Ttl74595.SRCLK, Value.TRUE);
    gate.propagate(state);
    setLevel(state, Ttl74595.SRCLK, Value.FALSE);
    gate.propagate(state);
    assertParallel(state, 0x9A);

    setLevel(state, Ttl74595.SRCLK, Value.UNKNOWN);
    gate.propagate(state);
    setLevel(state, Ttl74595.SRCLK, Value.FALSE);
    gate.propagate(state);
    setLevel(state, Ttl74595.RCLK, Value.UNKNOWN);
    gate.propagate(state);
    setLevel(state, Ttl74595.RCLK, Value.FALSE);
    gate.propagate(state);
    assertParallel(state, 0x9A);
    assertEquals(Value.FALSE, level(state, Ttl74595.QHP));
  }

  @Test
  void unknownClearDoesNotResetAndTheNextShiftStillMovesData() {
    final var gate = new Ttl74595();
    final var state = cleared(gate);
    shiftByte(gate, state, 0x01);
    assertEquals(Value.TRUE, level(state, Ttl74595.QHP));

    setLevel(state, Ttl74595.SRCLR, Value.UNKNOWN);
    gate.propagate(state);
    assertEquals(Value.TRUE, level(state, Ttl74595.QHP));

    setLevel(state, Ttl74595.SER, Value.TRUE);
    pulse(gate, state, Ttl74595.SRCLK);
    pulse(gate, state, Ttl74595.RCLK);
    assertEquals(Value.FALSE, level(state, Ttl74595.QHP));
    assertParallel(state, 0x01);
  }

  @Test
  void outputEnableReleasesOnlyTheStorageOutputs() {
    final var gate = new Ttl74595();
    final var state = cleared(gate);
    shiftByte(gate, state, 0x59);
    pulse(gate, state, Ttl74595.RCLK);
    assertEquals(Value.TRUE, level(state, Ttl74595.QHP));

    setLevel(state, Ttl74595.OE, Value.TRUE);
    gate.propagate(state);
    assertParallelUnknown(state);
    assertEquals(Value.TRUE, level(state, Ttl74595.QHP));

    setLevel(state, Ttl74595.OE, Value.FALSE);
    gate.propagate(state);
    assertParallel(state, 0x9A);

    setLevel(state, Ttl74595.OE, Value.UNKNOWN);
    gate.propagate(state);
    assertParallelUnknown(state);
    assertEquals(Value.TRUE, level(state, Ttl74595.QHP));

    setLevel(state, Ttl74595.OE, Value.ERROR);
    gate.propagate(state);
    assertParallelUnknown(state);
    assertEquals(Value.TRUE, level(state, Ttl74595.QHP));
  }

  @Test
  void unknownAndErrorSerialBitsShiftThroughToTheSerialOutput() {
    final var gate = new Ttl74595();
    final var state = cleared(gate);

    setLevel(state, Ttl74595.SER, Value.UNKNOWN);
    pulse(gate, state, Ttl74595.SRCLK);
    pulse(gate, state, Ttl74595.RCLK);
    assertEquals(Value.UNKNOWN, level(state, Ttl74595.QA));
    assertEquals(Value.FALSE, level(state, Ttl74595.QB));
    assertEquals(Value.FALSE, level(state, Ttl74595.QHP));

    setLevel(state, Ttl74595.SER, Value.ERROR);
    pulse(gate, state, Ttl74595.SRCLK);
    pulse(gate, state, Ttl74595.RCLK);
    assertEquals(Value.ERROR, level(state, Ttl74595.QA));
    assertEquals(Value.UNKNOWN, level(state, Ttl74595.QB));

    setLevel(state, Ttl74595.SER, Value.FALSE);
    for (var extra = 0; extra < 6; extra++) {
      pulse(gate, state, Ttl74595.SRCLK);
    }
    assertEquals(Value.UNKNOWN, level(state, Ttl74595.QHP));
    pulse(gate, state, Ttl74595.RCLK);
    assertEquals(Value.ERROR, level(state, Ttl74595.QG));
    assertEquals(Value.UNKNOWN, level(state, Ttl74595.QH));
  }

  @Test
  void heldClockAndFallingEdgeShiftOnlyOnTheNextRise() {
    final var gate = new Ttl74595();
    final var state = cleared(gate);

    setLevel(state, Ttl74595.SER, Value.TRUE);
    setLevel(state, Ttl74595.SRCLK, Value.TRUE);
    gate.propagate(state);
    assertParallel(state, 0x00);
    assertEquals(Value.FALSE, level(state, Ttl74595.QHP));

    setLevel(state, Ttl74595.SER, Value.FALSE);
    gate.propagate(state);
    setLevel(state, Ttl74595.SRCLK, Value.FALSE);
    gate.propagate(state);
    assertParallel(state, 0x00);
    assertEquals(Value.FALSE, level(state, Ttl74595.QHP));

    setLevel(state, Ttl74595.SRCLK, Value.TRUE);
    gate.propagate(state);
    setLevel(state, Ttl74595.SRCLK, Value.FALSE);
    gate.propagate(state);
    pulse(gate, state, Ttl74595.RCLK);
    assertParallel(state, 0x02);

    setLevel(state, Ttl74595.SER, Value.TRUE);
    pulse(gate, state, Ttl74595.SRCLK);
    setLevel(state, Ttl74595.RCLK, Value.TRUE);
    gate.propagate(state);
    assertParallel(state, 0x05);

    setLevel(state, Ttl74595.SER, Value.FALSE);
    pulse(gate, state, Ttl74595.SRCLK);
    assertParallel(state, 0x05);
    setLevel(state, Ttl74595.RCLK, Value.FALSE);
    gate.propagate(state);
    assertParallel(state, 0x05);

    setLevel(state, Ttl74595.RCLK, Value.TRUE);
    gate.propagate(state);
    setLevel(state, Ttl74595.RCLK, Value.FALSE);
    gate.propagate(state);
    assertParallel(state, 0x0A);
  }

  @Test
  void tiedClocksKeepStorageOneShiftBehindForAWholeByte() {
    final var gate = new Ttl74595();
    final var state = cleared(gate);
    var shift = 0;
    var store = 0;

    for (var bit = 0; bit < 8; bit++) {
      final var serial = ((0x59 >> bit) & 1) == 0 ? Value.FALSE : Value.TRUE;
      setLevel(state, Ttl74595.SER, serial);
      riseBoth(gate, state);
      store = shift;
      shift = ((shift << 1) | (serial == Value.TRUE ? 1 : 0)) & 0xFF;
      assertParallel(state, store);
      assertEquals((shift & 0x80) == 0 ? Value.FALSE : Value.TRUE, level(state, Ttl74595.QHP));
    }
    assertParallel(state, 0x4D);
    assertEquals(Value.TRUE, level(state, Ttl74595.QHP));
  }

  @Test
  void outputEnableReleasesWhileSerialOutIsLowAndClearCopiesZeros() {
    final var gate = new Ttl74595();
    final var state = cleared(gate);
    shiftByte(gate, state, 0x59);
    pulse(gate, state, Ttl74595.RCLK);
    assertParallel(state, 0x9A);

    setLevel(state, Ttl74595.SRCLR, Value.FALSE);
    gate.propagate(state);
    assertEquals(Value.FALSE, level(state, Ttl74595.QHP));
    assertParallel(state, 0x9A);

    setLevel(state, Ttl74595.OE, Value.TRUE);
    gate.propagate(state);
    assertParallelUnknown(state);
    assertEquals(Value.FALSE, level(state, Ttl74595.QHP));

    setLevel(state, Ttl74595.OE, Value.FALSE);
    gate.propagate(state);
    assertParallel(state, 0x9A);

    pulse(gate, state, Ttl74595.RCLK);
    assertParallel(state, 0x00);
    assertEquals(Value.FALSE, level(state, Ttl74595.QHP));
  }

  @Test
  void invalidExposedPowerInputsMakeOutputsUnknown() {
    final var gate = new Ttl74595();
    final var state = new TtlTestInstanceState(gate, true);
    state.setPortValue(GND_PORT, Value.FALSE);
    state.setPortValue(VCC_PORT, Value.TRUE);
    arm(state);
    gate.propagate(state);
    clearStorage(gate, state);
    shiftByte(gate, state, 0x59);
    pulse(gate, state, Ttl74595.RCLK);
    assertParallel(state, 0x9A);

    state.setPortValue(VCC_PORT, Value.FALSE);
    gate.propagate(state);
    assertParallelUnknown(state);
    assertEquals(Value.UNKNOWN, level(state, Ttl74595.QHP));

    state.setPortValue(VCC_PORT, Value.TRUE);
    gate.propagate(state);
    assertParallel(state, 0x9A);
    assertEquals(Value.TRUE, level(state, Ttl74595.QHP));

    state.setPortValue(GND_PORT, Value.TRUE);
    gate.propagate(state);
    assertParallelUnknown(state);
    assertEquals(Value.UNKNOWN, level(state, Ttl74595.QHP));
  }

  /** Clears both registers and leaves the controls idle with outputs enabled. */
  private static TtlTestInstanceState cleared(Ttl74595 gate) {
    final var state = new TtlTestInstanceState(gate, false);
    arm(state);
    gate.propagate(state);
    clearStorage(gate, state);
    return state;
  }

  private static void arm(TtlTestInstanceState state) {
    setLevel(state, Ttl74595.SRCLR, Value.TRUE);
    setLevel(state, Ttl74595.OE, Value.FALSE);
    setLevel(state, Ttl74595.SER, Value.FALSE);
    setLevel(state, Ttl74595.SRCLK, Value.FALSE);
    setLevel(state, Ttl74595.RCLK, Value.FALSE);
  }

  private static void clearStorage(Ttl74595 gate, TtlTestInstanceState state) {
    setLevel(state, Ttl74595.SRCLR, Value.FALSE);
    gate.propagate(state);
    pulse(gate, state, Ttl74595.RCLK);
    setLevel(state, Ttl74595.SRCLR, Value.TRUE);
    gate.propagate(state);
  }

  /** Shifts eight bits, least significant bit first, without pulsing the latch clock. */
  private static void shiftByte(Ttl74595 gate, TtlTestInstanceState state, int bits) {
    for (var bit = 0; bit < 8; bit++) {
      setLevel(state, Ttl74595.SER, ((bits >> bit) & 1) == 0 ? Value.FALSE : Value.TRUE);
      pulse(gate, state, Ttl74595.SRCLK);
    }
  }

  private static void pulse(Ttl74595 gate, TtlTestInstanceState state, byte clockPin) {
    setLevel(state, clockPin, Value.TRUE);
    gate.propagate(state);
    setLevel(state, clockPin, Value.FALSE);
    gate.propagate(state);
  }

  private static void riseBoth(Ttl74595 gate, TtlTestInstanceState state) {
    setLevel(state, Ttl74595.SRCLK, Value.FALSE);
    setLevel(state, Ttl74595.RCLK, Value.FALSE);
    gate.propagate(state);
    setLevel(state, Ttl74595.SRCLK, Value.TRUE);
    setLevel(state, Ttl74595.RCLK, Value.TRUE);
    gate.propagate(state);
    setLevel(state, Ttl74595.SRCLK, Value.FALSE);
    setLevel(state, Ttl74595.RCLK, Value.FALSE);
    gate.propagate(state);
  }

  private static void assertParallel(TtlTestInstanceState state, int bits) {
    for (var stage = 0; stage < STORAGE.length; stage++) {
      final var expected = ((bits >> stage) & 1) == 0 ? Value.FALSE : Value.TRUE;
      assertEquals(expected, level(state, STORAGE[stage]), "stage " + stage);
    }
  }

  private static void assertParallelUnknown(TtlTestInstanceState state) {
    for (final var pin : STORAGE) {
      assertEquals(Value.UNKNOWN, level(state, pin));
    }
  }

  private static void assertPort(Instance instance, byte dsPinNr, int x, int y, int type) {
    final var port = Ttl74595.pinNrToPortNr(dsPinNr);
    assertEquals(Location.create(x, y, false), instance.getPortLocation(port));
    assertEquals(type, instance.getPorts().get(port).getType());
  }

  private static void setLevel(TtlTestInstanceState state, byte dsPinNr, Value value) {
    state.setPortValue(Ttl74595.pinNrToPortNr(dsPinNr), value);
  }

  private static Value level(TtlTestInstanceState state, byte dsPinNr) {
    return state.getPortValue(Ttl74595.pinNrToPortNr(dsPinNr));
  }
}
