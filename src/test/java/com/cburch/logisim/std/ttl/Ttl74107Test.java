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
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.cburch.logisim.comp.EndData;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.instance.Instance;
import org.junit.jupiter.api.Test;

/** Functional tests for the 74HC107 dual negative-edge J-K flip-flop with reset. */
class Ttl74107Test {
  private static final int J = 0;
  private static final int K = 1;
  private static final int CP = 2;
  private static final int R = 3;
  private static final int Q = 4;
  private static final int NQ = 5;
  private static final int[][] HALVES = {
    {
      Ttl74107.PORT_1J, Ttl74107.PORT_1K, Ttl74107.PORT_1CP,
      Ttl74107.PORT_N1R, Ttl74107.PORT_1Q, Ttl74107.PORT_N1Q
    },
    {
      Ttl74107.PORT_2J, Ttl74107.PORT_2K, Ttl74107.PORT_2CP,
      Ttl74107.PORT_N2R, Ttl74107.PORT_2Q, Ttl74107.PORT_N2Q
    }
  };
  private static final int GND_PORT = 12;
  private static final int VCC_PORT = 13;

  @Test
  void logicalPortsFollowTheDatasheetPinout() {
    final var gate = new Ttl74107();
    final var hiddenPower = createInstance(gate, false);

    assertEquals(12, hiddenPower.getPorts().size());
    assertPort(hiddenPower, Ttl74107.PORT_1J, 10, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74107.PORT_N1Q, 30, 30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74107.PORT_1Q, 50, 30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74107.PORT_1K, 70, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74107.PORT_2Q, 90, 30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74107.PORT_N2Q, 110, 30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74107.PORT_2J, 130, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74107.PORT_2CP, 110, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74107.PORT_N2R, 90, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74107.PORT_2K, 70, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74107.PORT_1CP, 50, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74107.PORT_N1R, 30, -30, EndData.INPUT_ONLY);

    final var shownPower = createInstance(gate, true);
    assertEquals(14, shownPower.getPorts().size());
    assertPort(shownPower, GND_PORT, 130, 30, EndData.INPUT_ONLY);
    assertPort(shownPower, VCC_PORT, 10, -30, EndData.INPUT_ONLY);
  }

  @Test
  void clockPinsAreTheTwoNegativeEdgeClocks() {
    final var gate = new Ttl74107();
    assertTrue(gate.checkForGatedClocks(null));
    assertArrayEquals(new int[] {Ttl74107.PORT_1CP, Ttl74107.PORT_2CP}, gate.clockPinIndex(null));
  }

  @Test
  void resetClearsBothHalvesWithoutAClock() {
    final var gate = new Ttl74107();
    final var state = resetBoth(gate);
    for (var half : HALVES) fall(gate, state, half, Value.TRUE, Value.FALSE);
    assertBoth(state, Value.TRUE, Value.FALSE);

    for (var half : HALVES) state.setPortValue(half[R], Value.FALSE);
    gate.propagate(state);
    assertBoth(state, Value.FALSE, Value.TRUE);

    for (var half : HALVES) state.setPortValue(half[R], Value.TRUE);
    gate.propagate(state);
    assertBoth(state, Value.FALSE, Value.TRUE);
  }

  @Test
  void fallingEdgeAppliesTheJkTableOnBothHalves() {
    final var gate = new Ttl74107();
    final var state = resetBoth(gate);

    for (var half : HALVES) {
      fall(gate, state, half, Value.FALSE, Value.FALSE);
      assertHalf(state, half, Value.FALSE, Value.TRUE);

      fall(gate, state, half, Value.TRUE, Value.FALSE);
      assertHalf(state, half, Value.TRUE, Value.FALSE);

      fall(gate, state, half, Value.TRUE, Value.FALSE);
      assertHalf(state, half, Value.TRUE, Value.FALSE);

      fall(gate, state, half, Value.FALSE, Value.TRUE);
      assertHalf(state, half, Value.FALSE, Value.TRUE);

      fall(gate, state, half, Value.TRUE, Value.TRUE);
      assertHalf(state, half, Value.TRUE, Value.FALSE);
      fall(gate, state, half, Value.TRUE, Value.TRUE);
      assertHalf(state, half, Value.FALSE, Value.TRUE);

      fall(gate, state, half, Value.FALSE, Value.FALSE);
      assertHalf(state, half, Value.FALSE, Value.TRUE);
    }
  }

  @Test
  void risingEdgeAndLevelChangesDoNotTransfer() {
    final var gate = new Ttl74107();
    final var state = resetBoth(gate);
    final var half = HALVES[0];

    state.setPortValue(half[J], Value.TRUE);
    state.setPortValue(half[K], Value.FALSE);
    state.setPortValue(half[CP], Value.TRUE);
    gate.propagate(state);
    assertHalf(state, half, Value.FALSE, Value.TRUE);

    state.setPortValue(half[CP], Value.TRUE);
    gate.propagate(state);
    assertHalf(state, half, Value.FALSE, Value.TRUE);

    state.setPortValue(half[J], Value.FALSE);
    state.setPortValue(half[K], Value.TRUE);
    gate.propagate(state);
    assertHalf(state, half, Value.FALSE, Value.TRUE);

    state.setPortValue(half[CP], Value.FALSE);
    gate.propagate(state);
    assertHalf(state, half, Value.FALSE, Value.TRUE);

    state.setPortValue(half[CP], Value.UNKNOWN);
    gate.propagate(state);
    state.setPortValue(half[CP], Value.FALSE);
    gate.propagate(state);
    assertHalf(state, half, Value.FALSE, Value.TRUE);
  }

  @Test
  void halvesIgnoreEachOthersClock() {
    final var gate = new Ttl74107();
    final var state = resetBoth(gate);

    fall(gate, state, HALVES[0], Value.TRUE, Value.FALSE);
    assertHalf(state, HALVES[0], Value.TRUE, Value.FALSE);
    assertHalf(state, HALVES[1], Value.FALSE, Value.TRUE);

    fall(gate, state, HALVES[1], Value.TRUE, Value.TRUE);
    assertHalf(state, HALVES[0], Value.TRUE, Value.FALSE);
    assertHalf(state, HALVES[1], Value.TRUE, Value.FALSE);
  }

  @Test
  void resetOverridesAFallingEdgeThatWouldHold() {
    final var gate = new Ttl74107();
    final var state = resetBoth(gate);
    final var half = HALVES[0];
    fall(gate, state, half, Value.TRUE, Value.FALSE);
    assertHalf(state, half, Value.TRUE, Value.FALSE);

    state.setPortValue(half[J], Value.FALSE);
    state.setPortValue(half[K], Value.FALSE);
    state.setPortValue(half[R], Value.FALSE);
    state.setPortValue(half[CP], Value.TRUE);
    gate.propagate(state);
    state.setPortValue(half[CP], Value.FALSE);
    gate.propagate(state);
    assertHalf(state, half, Value.FALSE, Value.TRUE);

    state.setPortValue(half[J], Value.TRUE);
    state.setPortValue(half[CP], Value.TRUE);
    gate.propagate(state);
    state.setPortValue(half[CP], Value.FALSE);
    gate.propagate(state);
    assertHalf(state, half, Value.FALSE, Value.TRUE);

    state.setPortValue(half[R], Value.TRUE);
    gate.propagate(state);
    assertHalf(state, half, Value.FALSE, Value.TRUE);
  }

  @Test
  void unknownAndErrorInputsAffectOnlyUndeterminedTransitions() {
    final var gate = new Ttl74107();
    final var state = resetBoth(gate);
    final var half = HALVES[1];

    fall(gate, state, half, Value.TRUE, Value.UNKNOWN);
    assertHalf(state, half, Value.TRUE, Value.FALSE);

    fall(gate, state, half, Value.UNKNOWN, Value.FALSE);
    assertHalf(state, half, Value.TRUE, Value.FALSE);

    fall(gate, state, half, Value.ERROR, Value.FALSE);
    assertHalf(state, half, Value.TRUE, Value.FALSE);

    state.setPortValue(half[R], Value.FALSE);
    gate.propagate(state);
    state.setPortValue(half[R], Value.TRUE);
    gate.propagate(state);
    fall(gate, state, half, Value.UNKNOWN, Value.FALSE);
    assertHalf(state, half, Value.UNKNOWN, Value.UNKNOWN);

    fall(gate, state, half, Value.ERROR, Value.FALSE);
    assertHalf(state, half, Value.ERROR, Value.ERROR);

    state.setPortValue(half[R], Value.UNKNOWN);
    gate.propagate(state);
    assertHalf(state, half, Value.UNKNOWN, Value.UNKNOWN);

    state.setPortValue(half[R], Value.ERROR);
    gate.propagate(state);
    assertHalf(state, half, Value.ERROR, Value.ERROR);

    state.setPortValue(half[R], Value.FALSE);
    gate.propagate(state);
    assertHalf(state, half, Value.FALSE, Value.TRUE);
  }

  @Test
  void resetOfOneHalfLeavesTheOtherHalfSet() {
    final var gate = new Ttl74107();
    final var state = resetBoth(gate);
    fall(gate, state, HALVES[0], Value.TRUE, Value.FALSE);
    fall(gate, state, HALVES[1], Value.TRUE, Value.FALSE);
    assertBoth(state, Value.TRUE, Value.FALSE);

    state.setPortValue(HALVES[0][R], Value.FALSE);
    gate.propagate(state);
    assertHalf(state, HALVES[0], Value.FALSE, Value.TRUE);
    assertHalf(state, HALVES[1], Value.TRUE, Value.FALSE);
  }

  @Test
  void releasingResetOnAFallingEdgeAppliesJk() {
    final var gate = new Ttl74107();
    final var state = resetBoth(gate);
    final var half = HALVES[0];
    state.setPortValue(half[J], Value.TRUE);
    state.setPortValue(half[K], Value.FALSE);
    state.setPortValue(half[R], Value.FALSE);
    state.setPortValue(half[CP], Value.TRUE);
    gate.propagate(state);
    assertHalf(state, half, Value.FALSE, Value.TRUE);

    state.setPortValue(half[R], Value.TRUE);
    state.setPortValue(half[CP], Value.FALSE);
    gate.propagate(state);
    assertHalf(state, half, Value.TRUE, Value.FALSE);
  }

  @Test
  void unknownKHoldsADeterminedZeroAndBothClocksStepTogether() {
    final var gate = new Ttl74107();
    final var state = resetBoth(gate);

    fall(gate, state, HALVES[0], Value.FALSE, Value.UNKNOWN);
    assertHalf(state, HALVES[0], Value.FALSE, Value.TRUE);

    state.setPortValue(HALVES[0][J], Value.TRUE);
    state.setPortValue(HALVES[0][K], Value.FALSE);
    state.setPortValue(HALVES[1][J], Value.TRUE);
    state.setPortValue(HALVES[1][K], Value.FALSE);
    state.setPortValue(HALVES[0][CP], Value.TRUE);
    state.setPortValue(HALVES[1][CP], Value.TRUE);
    gate.propagate(state);
    state.setPortValue(HALVES[0][CP], Value.FALSE);
    state.setPortValue(HALVES[1][CP], Value.FALSE);
    gate.propagate(state);
    assertBoth(state, Value.TRUE, Value.FALSE);
  }

  @Test
  void invalidExposedPowerInputsMakeOutputsUnknown() {
    final var gate = new Ttl74107();
    final var state = new TtlTestInstanceState(gate, true);
    state.setPortValue(GND_PORT, Value.FALSE);
    state.setPortValue(VCC_PORT, Value.TRUE);
    for (var half : HALVES) {
      state.setPortValue(half[J], Value.FALSE);
      state.setPortValue(half[K], Value.FALSE);
      state.setPortValue(half[CP], Value.FALSE);
      state.setPortValue(half[R], Value.FALSE);
    }
    gate.propagate(state);
    assertBoth(state, Value.FALSE, Value.TRUE);

    state.setPortValue(VCC_PORT, Value.FALSE);
    gate.propagate(state);
    assertBoth(state, Value.UNKNOWN, Value.UNKNOWN);

    state.setPortValue(VCC_PORT, Value.TRUE);
    gate.propagate(state);
    assertBoth(state, Value.FALSE, Value.TRUE);

    state.setPortValue(GND_PORT, Value.TRUE);
    gate.propagate(state);
    assertBoth(state, Value.UNKNOWN, Value.UNKNOWN);
  }

  private static void assertPort(Instance instance, int index, int x, int y, int type) {
    assertEquals(Location.create(x, y, false), instance.getPortLocation(index));
    assertEquals(type, instance.getPorts().get(index).getType());
  }

  private static TtlTestInstanceState idle(Ttl74107 gate) {
    final var state = new TtlTestInstanceState(gate, false);
    for (var half : HALVES) {
      state.setPortValue(half[J], Value.FALSE);
      state.setPortValue(half[K], Value.FALSE);
      state.setPortValue(half[CP], Value.FALSE);
      state.setPortValue(half[R], Value.TRUE);
    }
    return state;
  }

  private static TtlTestInstanceState resetBoth(Ttl74107 gate) {
    final var state = idle(gate);
    for (var half : HALVES) state.setPortValue(half[R], Value.FALSE);
    gate.propagate(state);
    for (var half : HALVES) state.setPortValue(half[R], Value.TRUE);
    gate.propagate(state);
    return state;
  }

  private static void fall(
      Ttl74107 gate, TtlTestInstanceState state, int[] half, Value j, Value k) {
    state.setPortValue(half[J], j);
    state.setPortValue(half[K], k);
    state.setPortValue(half[CP], Value.TRUE);
    gate.propagate(state);
    state.setPortValue(half[CP], Value.FALSE);
    gate.propagate(state);
  }

  private static void assertHalf(TtlTestInstanceState state, int[] half, Value q, Value nq) {
    assertEquals(q, state.getPortValue(half[Q]));
    assertEquals(nq, state.getPortValue(half[NQ]));
  }

  private static void assertBoth(TtlTestInstanceState state, Value q, Value nq) {
    for (var half : HALVES) assertHalf(state, half, q, nq);
  }
}
