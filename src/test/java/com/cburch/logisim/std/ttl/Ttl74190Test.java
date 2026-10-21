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

/** Functional tests for the 74HC190 synchronous presettable BCD up/down counter. */
class Ttl74190Test {
  /** Figure 3 count-up arrows, including the illegal codes. */
  private static final int[] COUNT_UP = {1, 2, 3, 4, 5, 6, 7, 8, 9, 0, 9, 4, 9, 0, 9, 0};

  /** Figure 3 count-down arrows. Code 11 reaches a decade state on the second clock. */
  private static final int[] COUNT_DOWN = {9, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 3, 4, 5, 6};

  private static final int GND_PORT = 14;
  private static final int VCC_PORT = 15;

  @Test
  void logicalPortsFollowTheDatasheetPinout() {
    final var gate = new Ttl74190();
    final var hiddenPower = createInstance(gate, false);

    assertEquals(14, hiddenPower.getPorts().size());
    assertPort(hiddenPower, Ttl74190.D1, 10, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74190.Q1, 30, 30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74190.Q0, 50, 30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74190.CE, 70, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74190.DU, 90, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74190.Q2, 110, 30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74190.Q3, 130, 30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74190.D3, 150, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74190.D2, 130, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74190.PL, 110, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74190.TC, 90, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74190.RC, 70, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74190.CP, 50, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74190.D0, 30, -30, EndData.INPUT_ONLY);

    final var shownPower = createInstance(gate, true);
    assertEquals(16, shownPower.getPorts().size());
    assertEquals(Location.create(150, 30, false), shownPower.getPortLocation(GND_PORT));
    assertEquals(Location.create(10, -30, false), shownPower.getPortLocation(VCC_PORT));
  }

  @Test
  void parallelLoadIsTransparentAndOverridesTheClock() {
    final var gate = new Ttl74190();
    final var state = hold(gate);

    for (var code = 0; code < 16; code++) {
      load(gate, state, code);
      assertCount(state, code);
    }

    load(gate, state, 4);
    set(state, Ttl74190.DU, Value.FALSE);
    set(state, Ttl74190.CE, Value.FALSE);
    set(state, Ttl74190.CP, Value.FALSE);
    set(state, Ttl74190.PL, Value.FALSE);
    setData(state, 9);
    gate.propagate(state);
    assertCount(state, 9);
    assertFlags(state, true, false);

    set(state, Ttl74190.CP, Value.TRUE);
    setData(state, 12);
    gate.propagate(state);
    assertCount(state, 12);
    assertFlags(state, false, true);
  }

  @Test
  void countingFollowsBothStateDiagrams() {
    final var gate = new Ttl74190();
    final var state = hold(gate);

    for (var code = 0; code < 16; code++) {
      load(gate, state, code);
      arm(gate, state, Value.FALSE);
      assertCount(state, code);
      assertFlags(state, countsUpToTerminal(code), !countsUpToTerminal(code));
      set(state, Ttl74190.CP, Value.TRUE);
      gate.propagate(state);
      assertCount(state, COUNT_UP[code]);
      assertFlags(state, countsUpToTerminal(COUNT_UP[code]), true);
      fall(gate, state);
    }

    for (var code = 0; code < 16; code++) {
      load(gate, state, code);
      arm(gate, state, Value.TRUE);
      assertCount(state, code);
      assertFlags(state, code == 0, code != 0);
      set(state, Ttl74190.CP, Value.TRUE);
      gate.propagate(state);
      assertCount(state, COUNT_DOWN[code]);
      assertFlags(state, COUNT_DOWN[code] == 0, true);
      fall(gate, state);
    }
  }

  @Test
  void decadeCycleWrapsAndTheSecondDownClockLeavesEleven() {
    final var gate = new Ttl74190();
    final var state = hold(gate);
    load(gate, state, 0);
    set(state, Ttl74190.DU, Value.FALSE);
    set(state, Ttl74190.CE, Value.FALSE);
    for (var code = 0; code < 10; code++) {
      assertCount(state, code);
      rise(gate, state);
      fall(gate, state);
    }
    assertCount(state, 0);

    load(gate, state, 0);
    set(state, Ttl74190.DU, Value.TRUE);
    set(state, Ttl74190.CE, Value.FALSE);
    rise(gate, state);
    fall(gate, state);
    assertCount(state, 9);
    rise(gate, state);
    fall(gate, state);
    assertCount(state, 8);

    load(gate, state, 11);
    set(state, Ttl74190.DU, Value.TRUE);
    set(state, Ttl74190.CE, Value.FALSE);
    rise(gate, state);
    fall(gate, state);
    assertCount(state, 10);
    rise(gate, state);
    fall(gate, state);
    assertCount(state, 9);
  }

  @Test
  void terminalCountAndRippleClockFollowTheLevelOfTheClock() {
    final var gate = new Ttl74190();
    final var state = hold(gate);

    load(gate, state, 9);
    set(state, Ttl74190.DU, Value.FALSE);
    set(state, Ttl74190.CE, Value.FALSE);
    gate.propagate(state);
    assertFlags(state, true, false);

    set(state, Ttl74190.CP, Value.TRUE);
    gate.propagate(state);
    assertCount(state, 0);
    assertFlags(state, false, true);

    load(gate, state, 0);
    set(state, Ttl74190.DU, Value.TRUE);
    set(state, Ttl74190.CE, Value.FALSE);
    set(state, Ttl74190.CP, Value.TRUE);
    set(state, Ttl74190.PL, Value.FALSE);
    gate.propagate(state);
    set(state, Ttl74190.PL, Value.TRUE);
    gate.propagate(state);
    assertCount(state, 0);
    assertFlags(state, true, true);

    set(state, Ttl74190.CP, Value.FALSE);
    gate.propagate(state);
    assertCount(state, 0);
    assertFlags(state, true, false);

    set(state, Ttl74190.CE, Value.TRUE);
    gate.propagate(state);
    assertCount(state, 0);
    assertFlags(state, true, true);
  }

  @Test
  void terminalCountFollowsQaAndQdUpAndZeroDownWithoutTheEnable() {
    final var gate = new Ttl74190();
    final var state = hold(gate);
    set(state, Ttl74190.DU, Value.FALSE);
    set(state, Ttl74190.CE, Value.TRUE);
    set(state, Ttl74190.CP, Value.FALSE);

    for (var code = 0; code < 16; code++) {
      load(gate, state, code);
      set(state, Ttl74190.CE, Value.TRUE);
      gate.propagate(state);
      final var terminal = (code & 0x9) == 0x9;
      assertFlags(state, terminal, true);
    }

    for (var code : new int[] {9, 11, 13, 15}) {
      load(gate, state, code);
      set(state, Ttl74190.CE, Value.FALSE);
      gate.propagate(state);
      assertFlags(state, true, false);
    }

    load(gate, state, 0);
    set(state, Ttl74190.DU, Value.TRUE);
    set(state, Ttl74190.CE, Value.TRUE);
    gate.propagate(state);
    assertFlags(state, true, true);
    set(state, Ttl74190.CE, Value.FALSE);
    gate.propagate(state);
    assertFlags(state, true, false);
  }

  @Test
  void countEnableHoldsAndDirectionChangesTheTerminalCount() {
    final var gate = new Ttl74190();
    final var state = hold(gate);
    load(gate, state, 3);
    set(state, Ttl74190.CE, Value.TRUE);
    set(state, Ttl74190.DU, Value.FALSE);
    rise(gate, state);
    fall(gate, state);
    assertCount(state, 3);

    load(gate, state, 0);
    set(state, Ttl74190.CE, Value.FALSE);
    set(state, Ttl74190.DU, Value.FALSE);
    gate.propagate(state);
    assertFlags(state, false, true);
    set(state, Ttl74190.DU, Value.TRUE);
    gate.propagate(state);
    assertCount(state, 0);
    assertFlags(state, true, false);
  }

  @Test
  void fallingAndUnknownClocksDoNotCount() {
    final var gate = new Ttl74190();
    final var state = hold(gate);
    load(gate, state, 2);
    set(state, Ttl74190.CE, Value.FALSE);
    set(state, Ttl74190.CP, Value.TRUE);
    gate.propagate(state);
    assertCount(state, 3);
    set(state, Ttl74190.CP, Value.FALSE);
    gate.propagate(state);
    assertCount(state, 3);

    set(state, Ttl74190.CP, Value.UNKNOWN);
    gate.propagate(state);
    set(state, Ttl74190.CP, Value.TRUE);
    gate.propagate(state);
    assertCount(state, 3);
  }

  @Test
  void unknownLoadConflictsOnlyOnBitsThatDiffer() {
    final var gate = new Ttl74190();
    final var state = hold(gate);
    load(gate, state, 5);
    set(state, Ttl74190.PL, Value.UNKNOWN);
    setData(state, 5);
    gate.propagate(state);
    assertCount(state, 5);

    setData(state, 7);
    gate.propagate(state);
    assertBit(state, Ttl74190.Q0, Value.TRUE);
    assertBit(state, Ttl74190.Q1, Value.UNKNOWN);
    assertBit(state, Ttl74190.Q2, Value.TRUE);
    assertBit(state, Ttl74190.Q3, Value.FALSE);
  }

  @Test
  void unknownDirectionBecomesErrorOnlyWhenTheTwoNextStatesDiffer() {
    final var gate = new Ttl74190();
    final var state = hold(gate);
    load(gate, state, 10);
    set(state, Ttl74190.CE, Value.FALSE);
    set(state, Ttl74190.DU, Value.ERROR);
    rise(gate, state);
    assertCount(state, 9);

    load(gate, state, 0);
    set(state, Ttl74190.CE, Value.FALSE);
    set(state, Ttl74190.DU, Value.ERROR);
    rise(gate, state);
    assertBit(state, Ttl74190.Q0, Value.TRUE);
    assertBit(state, Ttl74190.Q1, Value.FALSE);
    assertBit(state, Ttl74190.Q2, Value.FALSE);
    assertBit(state, Ttl74190.Q3, Value.ERROR);
  }

  @Test
  void invalidExposedPowerInputsMakeOutputsUnknown() {
    final var gate = new Ttl74190();
    final var state = new TtlTestInstanceState(gate, true);
    state.setPortValue(GND_PORT, Value.FALSE);
    state.setPortValue(VCC_PORT, Value.TRUE);
    set(state, Ttl74190.CP, Value.FALSE);
    set(state, Ttl74190.PL, Value.TRUE);
    set(state, Ttl74190.CE, Value.TRUE);
    set(state, Ttl74190.DU, Value.FALSE);
    gate.propagate(state);
    load(gate, state, 4);

    state.setPortValue(VCC_PORT, Value.FALSE);
    gate.propagate(state);
    assertBit(state, Ttl74190.Q0, Value.UNKNOWN);
    assertBit(state, Ttl74190.TC, Value.UNKNOWN);

    state.setPortValue(VCC_PORT, Value.TRUE);
    gate.propagate(state);
    assertCount(state, 4);
  }

  private static TtlTestInstanceState hold(Ttl74190 gate) {
    final var state = new TtlTestInstanceState(gate, false);
    set(state, Ttl74190.CP, Value.FALSE);
    set(state, Ttl74190.PL, Value.TRUE);
    set(state, Ttl74190.CE, Value.TRUE);
    set(state, Ttl74190.DU, Value.FALSE);
    setData(state, 0);
    gate.propagate(state);
    return state;
  }

  private static void load(Ttl74190 gate, TtlTestInstanceState state, int code) {
    set(state, Ttl74190.CP, Value.FALSE);
    set(state, Ttl74190.CE, Value.TRUE);
    set(state, Ttl74190.PL, Value.FALSE);
    setData(state, code);
    gate.propagate(state);
    set(state, Ttl74190.PL, Value.TRUE);
    gate.propagate(state);
  }

  private static boolean countsUpToTerminal(int code) {
    return (code & 0x9) == 0x9;
  }

  /** Count enable low and the clock low, so a terminal state drives the ripple clock low. */
  private static void arm(Ttl74190 gate, TtlTestInstanceState state, Value direction) {
    set(state, Ttl74190.DU, direction);
    set(state, Ttl74190.CE, Value.FALSE);
    set(state, Ttl74190.CP, Value.FALSE);
    gate.propagate(state);
  }

  private static void rise(Ttl74190 gate, TtlTestInstanceState state) {
    set(state, Ttl74190.CP, Value.FALSE);
    gate.propagate(state);
    set(state, Ttl74190.CP, Value.TRUE);
    gate.propagate(state);
  }

  private static void fall(Ttl74190 gate, TtlTestInstanceState state) {
    set(state, Ttl74190.CP, Value.FALSE);
    gate.propagate(state);
  }

  private static void setData(TtlTestInstanceState state, int code) {
    set(state, Ttl74190.D0, bit(code, 0));
    set(state, Ttl74190.D1, bit(code, 1));
    set(state, Ttl74190.D2, bit(code, 2));
    set(state, Ttl74190.D3, bit(code, 3));
  }

  private static Value bit(int code, int index) {
    return ((code >> index) & 1) == 0 ? Value.FALSE : Value.TRUE;
  }

  private static void set(TtlTestInstanceState state, byte pin, Value value) {
    state.setPortValue(Ttl74190.pinNrToPortNr(pin), value);
  }

  private static void assertCount(TtlTestInstanceState state, int code) {
    assertBit(state, Ttl74190.Q0, bit(code, 0));
    assertBit(state, Ttl74190.Q1, bit(code, 1));
    assertBit(state, Ttl74190.Q2, bit(code, 2));
    assertBit(state, Ttl74190.Q3, bit(code, 3));
  }

  private static void assertFlags(
      TtlTestInstanceState state, boolean terminal, boolean rippleHigh) {
    assertBit(state, Ttl74190.TC, terminal ? Value.TRUE : Value.FALSE);
    assertBit(state, Ttl74190.RC, rippleHigh ? Value.TRUE : Value.FALSE);
  }

  private static void assertBit(TtlTestInstanceState state, byte pin, Value expected) {
    assertEquals(expected, state.getPortValue(Ttl74190.pinNrToPortNr(pin)), "pin " + pin);
  }

  private static void assertPort(Instance instance, byte pin, int x, int y, int type) {
    final var port = Ttl74190.pinNrToPortNr(pin);
    assertEquals(Location.create(x, y, false), instance.getPortLocation(port));
    assertEquals(type, instance.getPorts().get(port).getType());
  }
}
