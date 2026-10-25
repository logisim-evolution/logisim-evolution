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

/** Tests for the TTL 7405 hex open-drain inverter. */
class Ttl7405Test {
  private static final int[][] INVERTERS = {
    {Ttl7405.PORT_INDEX_1A, Ttl7405.PORT_INDEX_1Y},
    {Ttl7405.PORT_INDEX_2A, Ttl7405.PORT_INDEX_2Y},
    {Ttl7405.PORT_INDEX_3A, Ttl7405.PORT_INDEX_3Y},
    {Ttl7405.PORT_INDEX_4A, Ttl7405.PORT_INDEX_4Y},
    {Ttl7405.PORT_INDEX_5A, Ttl7405.PORT_INDEX_5Y},
    {Ttl7405.PORT_INDEX_6A, Ttl7405.PORT_INDEX_6Y}
  };

  private static final int[] OUTPUTS = {
    Ttl7405.PORT_INDEX_1Y,
    Ttl7405.PORT_INDEX_2Y,
    Ttl7405.PORT_INDEX_3Y,
    Ttl7405.PORT_INDEX_4Y,
    Ttl7405.PORT_INDEX_5Y,
    Ttl7405.PORT_INDEX_6Y
  };

  @Test
  void usesPhysicalPinoutAndKeepsPowerPortsLast() {
    final var gate = new Ttl7405();
    final var hiddenPower = TtlTestInstanceState.createInstance(gate, false);

    assertEquals(12, hiddenPower.getPorts().size());
    assertPort(hiddenPower, Ttl7405.PORT_INDEX_1A, 10, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl7405.PORT_INDEX_1Y, 30, 30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl7405.PORT_INDEX_2A, 50, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl7405.PORT_INDEX_2Y, 70, 30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl7405.PORT_INDEX_3A, 90, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl7405.PORT_INDEX_3Y, 110, 30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl7405.PORT_INDEX_4Y, 130, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl7405.PORT_INDEX_4A, 110, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl7405.PORT_INDEX_5Y, 90, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl7405.PORT_INDEX_5A, 70, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl7405.PORT_INDEX_6Y, 50, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl7405.PORT_INDEX_6A, 30, -30, EndData.INPUT_ONLY);

    final var shownPower = TtlTestInstanceState.createInstance(gate, true);
    assertEquals(14, shownPower.getPorts().size());
    assertPort(shownPower, 12, 130, 30, EndData.INPUT_ONLY); // Physical pin 7: GND
    assertPort(shownPower, 13, 10, -30, EndData.INPUT_ONLY); // Physical pin 14: VCC
  }

  @Test
  void eachInverterDrivesLowOnlyWhenInputIsHigh() {
    final var gate = new Ttl7405();
    final var state = new TtlTestInstanceState(gate, false);

    for (final var pins : INVERTERS) {
      driveAllLow(state);
      state.setPortValue(pins[0], Value.TRUE);
      gate.propagate(state);
      assertEquals(Value.FALSE, state.getPortValue(pins[1]));
      assertOtherOutputs(state, pins[1], Value.UNKNOWN);

      state.setPortValue(pins[0], Value.FALSE);
      gate.propagate(state);
      assertEquals(Value.UNKNOWN, state.getPortValue(pins[1]));
      assertOtherOutputs(state, pins[1], Value.UNKNOWN);
    }
  }

  @Test
  void unsettledInputMakesOutputError() {
    final var gate = new Ttl7405();
    final var state = new TtlTestInstanceState(gate, false);

    for (final var unsettled : new Value[] {Value.UNKNOWN, Value.ERROR}) {
      driveAllLow(state);
      state.setPortValue(Ttl7405.PORT_INDEX_1A, unsettled);
      gate.propagate(state);
      assertEquals(Value.ERROR, state.getPortValue(Ttl7405.PORT_INDEX_1Y));
      assertOtherOutputs(state, Ttl7405.PORT_INDEX_1Y, Value.UNKNOWN);
    }
  }

  @Test
  void invalidExposedPowerInputsMakeOutputsUnknown() {
    final var gate = new Ttl7405();
    final var state = new TtlTestInstanceState(gate, true);
    state.setPortValue(12, Value.FALSE);
    state.setPortValue(13, Value.TRUE);
    driveFirstInputHigh(state);
    gate.propagate(state);
    assertEquals(Value.FALSE, state.getPortValue(Ttl7405.PORT_INDEX_1Y));

    state.setPortValue(13, Value.FALSE);
    gate.propagate(state);
    assertUnknownOutputs(state);

    state.setPortValue(13, Value.TRUE);
    driveFirstInputHigh(state);
    gate.propagate(state);
    assertEquals(Value.FALSE, state.getPortValue(Ttl7405.PORT_INDEX_1Y));

    state.setPortValue(12, Value.TRUE);
    gate.propagate(state);
    assertUnknownOutputs(state);
  }

  private static void assertPort(Instance instance, int index, int x, int y, int type) {
    assertEquals(Location.create(x, y, false), instance.getPortLocation(index));
    assertEquals(type, instance.getPorts().get(index).getType());
  }

  private static void driveAllLow(TtlTestInstanceState state) {
    for (final var pins : INVERTERS) {
      state.setPortValue(pins[0], Value.FALSE);
    }
  }

  private static void driveFirstInputHigh(TtlTestInstanceState state) {
    driveAllLow(state);
    state.setPortValue(Ttl7405.PORT_INDEX_1A, Value.TRUE);
  }

  private static void assertOtherOutputs(TtlTestInstanceState state, int driven, Value expected) {
    for (final var output : OUTPUTS) {
      if (output != driven) {
        assertEquals(expected, state.getPortValue(output));
      }
    }
  }

  private static void assertUnknownOutputs(TtlTestInstanceState state) {
    for (final var output : OUTPUTS) {
      assertEquals(Value.UNKNOWN, state.getPortValue(output));
    }
  }
}
