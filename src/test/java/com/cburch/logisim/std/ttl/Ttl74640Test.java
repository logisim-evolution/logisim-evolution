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
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.cburch.logisim.comp.EndData;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.instance.Instance;
import org.junit.jupiter.api.Test;

/** Functional tests for the 74HC640 octal inverting bus transceiver. */
class Ttl74640Test {
  private static final int[] BUS_A = {
    Ttl74640.PORT_INDEX_A1,
    Ttl74640.PORT_INDEX_A2,
    Ttl74640.PORT_INDEX_A3,
    Ttl74640.PORT_INDEX_A4,
    Ttl74640.PORT_INDEX_A5,
    Ttl74640.PORT_INDEX_A6,
    Ttl74640.PORT_INDEX_A7,
    Ttl74640.PORT_INDEX_A8
  };
  private static final int[] BUS_B = {
    Ttl74640.PORT_INDEX_B1,
    Ttl74640.PORT_INDEX_B2,
    Ttl74640.PORT_INDEX_B3,
    Ttl74640.PORT_INDEX_B4,
    Ttl74640.PORT_INDEX_B5,
    Ttl74640.PORT_INDEX_B6,
    Ttl74640.PORT_INDEX_B7,
    Ttl74640.PORT_INDEX_B8
  };

  @Test
  void logicalPortsFollowTheDatasheetPinout() {
    final var gate = new Ttl74640();
    final var hiddenPower = createInstance(gate, false);

    assertEquals(18, hiddenPower.getPorts().size());
    assertPort(hiddenPower, Ttl74640.PORT_INDEX_DIR, 10, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74640.PORT_INDEX_A1, 30, 30, EndData.INPUT_OUTPUT);
    assertPort(hiddenPower, Ttl74640.PORT_INDEX_A2, 50, 30, EndData.INPUT_OUTPUT);
    assertPort(hiddenPower, Ttl74640.PORT_INDEX_A3, 70, 30, EndData.INPUT_OUTPUT);
    assertPort(hiddenPower, Ttl74640.PORT_INDEX_A4, 90, 30, EndData.INPUT_OUTPUT);
    assertPort(hiddenPower, Ttl74640.PORT_INDEX_A5, 110, 30, EndData.INPUT_OUTPUT);
    assertPort(hiddenPower, Ttl74640.PORT_INDEX_A6, 130, 30, EndData.INPUT_OUTPUT);
    assertPort(hiddenPower, Ttl74640.PORT_INDEX_A7, 150, 30, EndData.INPUT_OUTPUT);
    assertPort(hiddenPower, Ttl74640.PORT_INDEX_A8, 170, 30, EndData.INPUT_OUTPUT);
    assertPort(hiddenPower, Ttl74640.PORT_INDEX_B8, 190, -30, EndData.INPUT_OUTPUT);
    assertPort(hiddenPower, Ttl74640.PORT_INDEX_B7, 170, -30, EndData.INPUT_OUTPUT);
    assertPort(hiddenPower, Ttl74640.PORT_INDEX_B6, 150, -30, EndData.INPUT_OUTPUT);
    assertPort(hiddenPower, Ttl74640.PORT_INDEX_B5, 130, -30, EndData.INPUT_OUTPUT);
    assertPort(hiddenPower, Ttl74640.PORT_INDEX_B4, 110, -30, EndData.INPUT_OUTPUT);
    assertPort(hiddenPower, Ttl74640.PORT_INDEX_B3, 90, -30, EndData.INPUT_OUTPUT);
    assertPort(hiddenPower, Ttl74640.PORT_INDEX_B2, 70, -30, EndData.INPUT_OUTPUT);
    assertPort(hiddenPower, Ttl74640.PORT_INDEX_B1, 50, -30, EndData.INPUT_OUTPUT);
    assertPort(hiddenPower, Ttl74640.PORT_INDEX_nOE, 30, -30, EndData.INPUT_ONLY);

    final var shownPower = createInstance(gate, true);
    assertEquals(20, shownPower.getPorts().size());
    assertPort(shownPower, Ttl74640.PORT_INDEX_GND, 190, 30, EndData.INPUT_ONLY);
    assertPort(shownPower, Ttl74640.PORT_INDEX_VCC, 10, -30, EndData.INPUT_ONLY);
  }

  @Test
  void directionIsNotAClock() {
    assertFalse(new Ttl74640().checkForGatedClocks(null));
  }

  @Test
  void highDirectionDrivesBWithTheComplementOfA() {
    final var gate = new Ttl74640();
    final var patterns = new int[] {0x00, 0xFF, 0x55, 0xAA, 0x01, 0x80, 0x0F, 0xF0};

    for (final var pattern : patterns) {
      final var state = new TtlTestInstanceState(gate, false);
      present(state, pattern, 0x00, Value.TRUE, Value.FALSE);
      gate.propagate(state);
      assertBus(state, BUS_B, pattern ^ 0xFF);
      assertBusUnknown(state, BUS_A);
    }
  }

  @Test
  void lowDirectionDrivesAWithTheComplementOfB() {
    final var gate = new Ttl74640();
    final var patterns = new int[] {0x00, 0xFF, 0x55, 0xAA, 0x01, 0x80, 0x0F, 0xF0};

    for (final var pattern : patterns) {
      final var state = new TtlTestInstanceState(gate, false);
      present(state, 0x00, pattern, Value.FALSE, Value.FALSE);
      gate.propagate(state);
      assertBus(state, BUS_A, pattern ^ 0xFF);
      assertBusUnknown(state, BUS_B);
    }
  }

  @Test
  void eachBitInvertsOntoItsOwnPair() {
    final var gate = new Ttl74640();

    for (var bit = 0; bit < 8; bit++) {
      final var towardB = new TtlTestInstanceState(gate, false);
      present(towardB, 1 << bit, 0xFF, Value.TRUE, Value.FALSE);
      gate.propagate(towardB);
      assertBus(towardB, BUS_B, (~(1 << bit)) & 0xFF);

      final var towardA = new TtlTestInstanceState(gate, false);
      present(towardA, 0xFF, 1 << bit, Value.FALSE, Value.FALSE);
      gate.propagate(towardA);
      assertBus(towardA, BUS_A, (~(1 << bit)) & 0xFF);
    }
  }

  @Test
  void outputEnableReleasesBothBuses() {
    final var gate = new Ttl74640();
    final var directions = new Value[] {Value.TRUE, Value.FALSE};

    for (final var direction : directions) {
      final var state = new TtlTestInstanceState(gate, false);
      present(state, 0x55, 0x0F, direction, Value.TRUE);
      gate.propagate(state);
      assertBusUnknown(state, BUS_A);
      assertBusUnknown(state, BUS_B);
    }
  }

  @Test
  void unknownControlsReleaseBothBuses() {
    final var gate = new Ttl74640();
    final var controls = new Value[] {Value.UNKNOWN, Value.ERROR};

    for (final var control : controls) {
      final var unknownEnable = new TtlTestInstanceState(gate, false);
      present(unknownEnable, 0x55, 0x0F, Value.TRUE, control);
      gate.propagate(unknownEnable);
      assertBusUnknown(unknownEnable, BUS_A);
      assertBusUnknown(unknownEnable, BUS_B);

      final var unknownDirection = new TtlTestInstanceState(gate, false);
      present(unknownDirection, 0x55, 0x0F, control, Value.FALSE);
      gate.propagate(unknownDirection);
      assertBusUnknown(unknownDirection, BUS_A);
      assertBusUnknown(unknownDirection, BUS_B);
    }
  }

  @Test
  void unknownSourceBitBecomesAnErrorOnTheDrivenBus() {
    final var gate = new Ttl74640();
    final var invalid = new Value[] {Value.UNKNOWN, Value.ERROR};

    for (final var source : invalid) {
      final var towardB = new TtlTestInstanceState(gate, false);
      present(towardB, 0x00, 0x00, Value.TRUE, Value.FALSE);
      towardB.setPortValue(Ttl74640.PORT_INDEX_A1, source);
      gate.propagate(towardB);
      assertEquals(Value.ERROR, towardB.getPortValue(Ttl74640.PORT_INDEX_B1));
      for (var bit = 1; bit < 8; bit++) {
        assertEquals(Value.TRUE, towardB.getPortValue(BUS_B[bit]), "B" + (bit + 1));
      }

      final var towardA = new TtlTestInstanceState(gate, false);
      present(towardA, 0x00, 0x00, Value.FALSE, Value.FALSE);
      towardA.setPortValue(Ttl74640.PORT_INDEX_B1, source);
      gate.propagate(towardA);
      assertEquals(Value.ERROR, towardA.getPortValue(Ttl74640.PORT_INDEX_A1));
      for (var bit = 1; bit < 8; bit++) {
        assertEquals(Value.TRUE, towardA.getPortValue(BUS_A[bit]), "A" + (bit + 1));
      }
    }
  }

  /**
   * Same vectors as the passing Arduino bench, on one instance: both directions, then high-Z, then
   * a new word after the buses were released. The part does not keep the previous word.
   */
  @Test
  void benchSequenceDrivesTheComplementAndThenReleasesTheBuses() {
    final var gate = new Ttl74640();
    final var state = new TtlTestInstanceState(gate, false);
    final var patterns = new int[] {0x00, 0xFF, 0x55, 0xAA};

    for (final var pattern : patterns) {
      present(state, pattern, 0x00, Value.TRUE, Value.FALSE);
      gate.propagate(state);
      assertBus(state, BUS_B, pattern ^ 0xFF);
      assertBusUnknown(state, BUS_A);
    }
    for (var bit = 0; bit < 8; bit++) {
      present(state, 1 << bit, 0x00, Value.TRUE, Value.FALSE);
      gate.propagate(state);
      assertBus(state, BUS_B, (~(1 << bit)) & 0xFF);
    }

    for (final var pattern : patterns) {
      present(state, 0x00, pattern, Value.FALSE, Value.FALSE);
      gate.propagate(state);
      assertBus(state, BUS_A, pattern ^ 0xFF);
      assertBusUnknown(state, BUS_B);
    }
    for (var bit = 0; bit < 8; bit++) {
      present(state, 0x00, 1 << bit, Value.FALSE, Value.FALSE);
      gate.propagate(state);
      assertBus(state, BUS_A, (~(1 << bit)) & 0xFF);
    }

    for (final var direction : new Value[] {Value.TRUE, Value.FALSE}) {
      present(state, 0x00, 0x00, direction, Value.TRUE);
      gate.propagate(state);
      assertBusUnknown(state, BUS_A);
      assertBusUnknown(state, BUS_B);
      present(state, 0xFF, 0xFF, direction, Value.TRUE);
      gate.propagate(state);
      assertBusUnknown(state, BUS_A);
      assertBusUnknown(state, BUS_B);
    }

    present(state, 0x0F, 0x00, Value.TRUE, Value.FALSE);
    gate.propagate(state);
    assertBus(state, BUS_B, 0xF0);
    present(state, 0x00, 0xF0, Value.FALSE, Value.FALSE);
    gate.propagate(state);
    assertBus(state, BUS_A, 0x0F);
  }

  @Test
  void invalidExposedPowerInputsReleaseBothBuses() {
    final var gate = new Ttl74640();
    final var state = new TtlTestInstanceState(gate, true);
    state.setPortValue(Ttl74640.PORT_INDEX_GND, Value.FALSE);
    state.setPortValue(Ttl74640.PORT_INDEX_VCC, Value.TRUE);
    present(state, 0x55, 0x00, Value.TRUE, Value.FALSE);
    gate.propagate(state);
    assertBus(state, BUS_B, 0xAA);

    state.setPortValue(Ttl74640.PORT_INDEX_VCC, Value.FALSE);
    gate.propagate(state);
    assertBusUnknown(state, BUS_A);
    assertBusUnknown(state, BUS_B);

    state.setPortValue(Ttl74640.PORT_INDEX_VCC, Value.TRUE);
    present(state, 0x55, 0x00, Value.TRUE, Value.FALSE);
    gate.propagate(state);
    assertBus(state, BUS_B, 0xAA);

    state.setPortValue(Ttl74640.PORT_INDEX_GND, Value.TRUE);
    gate.propagate(state);
    assertBusUnknown(state, BUS_A);
    assertBusUnknown(state, BUS_B);
  }

  private static void present(
      TtlTestInstanceState state, int busA, int busB, Value direction, Value outputEnable) {
    state.setPortValue(Ttl74640.PORT_INDEX_DIR, direction);
    state.setPortValue(Ttl74640.PORT_INDEX_nOE, outputEnable);
    for (var bit = 0; bit < 8; bit++) {
      state.setPortValue(BUS_A[bit], bitValue(busA, bit));
      state.setPortValue(BUS_B[bit], bitValue(busB, bit));
    }
  }

  private static Value bitValue(int word, int bit) {
    return ((word >> bit) & 1) == 1 ? Value.TRUE : Value.FALSE;
  }

  private static void assertBus(TtlTestInstanceState state, int[] ports, int expected) {
    for (var bit = 0; bit < ports.length; bit++) {
      assertEquals(bitValue(expected, bit), state.getPortValue(ports[bit]), "bit " + bit);
    }
  }

  private static void assertBusUnknown(TtlTestInstanceState state, int[] ports) {
    for (final var port : ports) {
      assertEquals(Value.UNKNOWN, state.getPortValue(port));
    }
  }

  private static void assertPort(Instance instance, int port, int x, int y, int type) {
    assertEquals(Location.create(x, y, false), instance.getPortLocation(port));
    assertEquals(type, instance.getPorts().get(port).getType());
  }
}
