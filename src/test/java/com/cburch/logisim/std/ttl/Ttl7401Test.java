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

import com.cburch.logisim.comp.EndData;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.instance.Instance;
import org.junit.jupiter.api.Test;

/** Tests for the TTL 7401 quad 2-input open-collector NAND gate. */
class Ttl7401Test {
  /** {input A, input B, output Y} port indexes for each gate. */
  private static final int[][] GATES = {
    {Ttl7401.PORT_INDEX_1A, Ttl7401.PORT_INDEX_1B, Ttl7401.PORT_INDEX_1Y},
    {Ttl7401.PORT_INDEX_2A, Ttl7401.PORT_INDEX_2B, Ttl7401.PORT_INDEX_2Y},
    {Ttl7401.PORT_INDEX_3A, Ttl7401.PORT_INDEX_3B, Ttl7401.PORT_INDEX_3Y},
    {Ttl7401.PORT_INDEX_4A, Ttl7401.PORT_INDEX_4B, Ttl7401.PORT_INDEX_4Y}
  };

  @Test
  void usesPhysicalPinoutAndKeepsPowerPortsLast() {
    final var gate = new Ttl7401();
    final var hiddenPower = TtlTestInstanceState.createInstance(gate, false);

    assertEquals(12, hiddenPower.getPorts().size());
    // Bottom row, pins 1-6: the output is the first pin of each gate.
    assertPort(hiddenPower, Ttl7401.PORT_INDEX_1Y, 10, 30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl7401.PORT_INDEX_1A, 30, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl7401.PORT_INDEX_1B, 50, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl7401.PORT_INDEX_2Y, 70, 30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl7401.PORT_INDEX_2A, 90, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl7401.PORT_INDEX_2B, 110, 30, EndData.INPUT_ONLY);
    // Top row, pins 8-13.
    assertPort(hiddenPower, Ttl7401.PORT_INDEX_3A, 130, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl7401.PORT_INDEX_3B, 110, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl7401.PORT_INDEX_3Y, 90, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl7401.PORT_INDEX_4A, 70, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl7401.PORT_INDEX_4B, 50, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl7401.PORT_INDEX_4Y, 30, -30, EndData.OUTPUT_ONLY);

    final var shownPower = TtlTestInstanceState.createInstance(gate, true);
    assertEquals(14, shownPower.getPorts().size());
    assertPort(shownPower, 12, 130, 30, EndData.INPUT_ONLY); // Physical pin 7: GND
    assertPort(shownPower, 13, 10, -30, EndData.INPUT_ONLY); // Physical pin 14: VCC
  }

  @Test
  void eachGateDrivesLowOnlyWhenBothInputsAreHigh() {
    final var gate = new Ttl7401();
    final var state = new TtlTestInstanceState(gate, false);
    final var levels = new Value[] {Value.FALSE, Value.TRUE};

    for (final var pins : GATES) {
      for (final var a : levels) {
        for (final var b : levels) {
          driveAllLow(state);
          state.setPortValue(pins[0], a);
          state.setPortValue(pins[1], b);
          gate.propagate(state);
          final var expected = (a == Value.TRUE && b == Value.TRUE) ? Value.FALSE : Value.UNKNOWN;
          assertEquals(expected, state.getPortValue(pins[2]));
          assertOtherOutputs(state, pins[2], Value.UNKNOWN);
        }
      }
    }
  }

  @Test
  void aLowInputReleasesOutputEvenIfOtherInputIsUnsettled() {
    final var gate = new Ttl7401();
    final var state = new TtlTestInstanceState(gate, false);

    state.setPortValue(Ttl7401.PORT_INDEX_1A, Value.FALSE);
    state.setPortValue(Ttl7401.PORT_INDEX_1B, Value.ERROR);
    gate.propagate(state);
    assertEquals(Value.UNKNOWN, state.getPortValue(Ttl7401.PORT_INDEX_1Y));
  }

  @Test
  void unsettledInputThatDecidesTheOutputMakesItError() {
    final var gate = new Ttl7401();
    final var state = new TtlTestInstanceState(gate, false);

    for (final var unsettled : new Value[] {Value.UNKNOWN, Value.ERROR}) {
      driveAllLow(state);
      state.setPortValue(Ttl7401.PORT_INDEX_1A, Value.TRUE);
      state.setPortValue(Ttl7401.PORT_INDEX_1B, unsettled);
      gate.propagate(state);
      assertEquals(Value.ERROR, state.getPortValue(Ttl7401.PORT_INDEX_1Y));
      assertOtherOutputs(state, Ttl7401.PORT_INDEX_1Y, Value.UNKNOWN);
    }
  }

  @Test
  void invalidExposedPowerInputsMakeOutputsUnknown() {
    final var gate = new Ttl7401();
    final var state = new TtlTestInstanceState(gate, true);
    state.setPortValue(12, Value.FALSE);
    state.setPortValue(13, Value.TRUE);
    driveFirstGateHigh(state);
    gate.propagate(state);
    assertEquals(Value.FALSE, state.getPortValue(Ttl7401.PORT_INDEX_1Y));

    state.setPortValue(13, Value.FALSE);
    gate.propagate(state);
    assertUnknownOutputs(state);

    state.setPortValue(13, Value.TRUE);
    driveFirstGateHigh(state);
    gate.propagate(state);
    assertEquals(Value.FALSE, state.getPortValue(Ttl7401.PORT_INDEX_1Y));

    state.setPortValue(12, Value.TRUE);
    gate.propagate(state);
    assertUnknownOutputs(state);
  }

  private static void assertPort(Instance instance, int index, int x, int y, int type) {
    assertEquals(Location.create(x, y, false), instance.getPortLocation(index));
    assertEquals(type, instance.getPorts().get(index).getType());
  }

  private static void driveAllLow(TtlTestInstanceState state) {
    for (final var pins : GATES) {
      state.setPortValue(pins[0], Value.FALSE);
      state.setPortValue(pins[1], Value.FALSE);
    }
  }

  private static void driveFirstGateHigh(TtlTestInstanceState state) {
    driveAllLow(state);
    state.setPortValue(Ttl7401.PORT_INDEX_1A, Value.TRUE);
    state.setPortValue(Ttl7401.PORT_INDEX_1B, Value.TRUE);
  }

  private static void assertOtherOutputs(TtlTestInstanceState state, int driven, Value expected) {
    for (final var pins : GATES) {
      if (pins[2] != driven) {
        assertEquals(expected, state.getPortValue(pins[2]));
      }
    }
  }

  private static void assertUnknownOutputs(TtlTestInstanceState state) {
    for (final var pins : GATES) {
      assertEquals(Value.UNKNOWN, state.getPortValue(pins[2]));
    }
  }
}
