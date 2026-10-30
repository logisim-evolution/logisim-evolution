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
import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.instance.Instance;
import org.junit.jupiter.api.Test;

/** Functional tests for the 74HC4017 Johnson decade counter. */
class Ttl744017Test {
  private static final int GND_PORT = 14;
  private static final int VCC_PORT = 15;
  private static final BitWidth WIDTH = BitWidth.create(4);
  private static final int[] DECODED_PORTS = {
    Ttl744017.PORT_INDEX_Q0,
    Ttl744017.PORT_INDEX_Q1,
    Ttl744017.PORT_INDEX_Q2,
    Ttl744017.PORT_INDEX_Q3,
    Ttl744017.PORT_INDEX_Q4,
    Ttl744017.PORT_INDEX_Q5,
    Ttl744017.PORT_INDEX_Q6,
    Ttl744017.PORT_INDEX_Q7,
    Ttl744017.PORT_INDEX_Q8,
    Ttl744017.PORT_INDEX_Q9
  };

  @Test
  void logicalPortsFollowTheDatasheetPinout() {
    final var gate = new Ttl744017();
    final var hiddenPower = createInstance(gate, false);

    assertEquals(14, hiddenPower.getPorts().size());
    assertPort(hiddenPower, Ttl744017.PORT_INDEX_Q5, 10, 30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl744017.PORT_INDEX_Q1, 30, 30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl744017.PORT_INDEX_Q0, 50, 30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl744017.PORT_INDEX_Q2, 70, 30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl744017.PORT_INDEX_Q6, 90, 30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl744017.PORT_INDEX_Q7, 110, 30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl744017.PORT_INDEX_Q3, 130, 30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl744017.PORT_INDEX_Q8, 150, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl744017.PORT_INDEX_Q4, 130, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl744017.PORT_INDEX_Q9, 110, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl744017.PORT_INDEX_Q5_9, 90, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl744017.PORT_INDEX_CP1, 70, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl744017.PORT_INDEX_CP0, 50, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl744017.PORT_INDEX_MR, 30, -30, EndData.INPUT_ONLY);

    final var shownPower = createInstance(gate, true);
    assertEquals(16, shownPower.getPorts().size());
    assertPort(shownPower, GND_PORT, 150, 30, EndData.INPUT_ONLY);
    assertPort(shownPower, VCC_PORT, 10, -30, EndData.INPUT_ONLY);
  }

  @Test
  void cp0RisingWalksTheDecadeAndWraps() {
    final var gate = new Ttl744017();
    final var state = new TtlTestInstanceState(gate, false);
    reset(gate, state);
    assertCount(state, 0);

    for (var count = 1; count <= 10; count++) {
      pulseCp0(gate, state);
      assertCount(state, count % 10);
    }
  }

  @Test
  void cp1FallingAdvancesOnlyWhileCp0IsHigh() {
    final var gate = new Ttl744017();
    final var state = new TtlTestInstanceState(gate, false);
    reset(gate, state);

    state.setPortValue(Ttl744017.PORT_INDEX_CP1, Value.TRUE);
    gate.propagate(state);
    state.setPortValue(Ttl744017.PORT_INDEX_CP0, Value.TRUE);
    gate.propagate(state);
    assertCount(state, 0);

    state.setPortValue(Ttl744017.PORT_INDEX_CP1, Value.FALSE);
    gate.propagate(state);
    assertCount(state, 1);

    state.setPortValue(Ttl744017.PORT_INDEX_CP1, Value.TRUE);
    gate.propagate(state);
    assertCount(state, 1);

    state.setPortValue(Ttl744017.PORT_INDEX_CP0, Value.FALSE);
    gate.propagate(state);
    state.setPortValue(Ttl744017.PORT_INDEX_CP1, Value.FALSE);
    gate.propagate(state);
    assertCount(state, 1);
  }

  @Test
  void bothQualifyingEdgesInOneStepAdvanceOnce() {
    final var gate = new Ttl744017();
    final var state = new TtlTestInstanceState(gate, false);
    reset(gate, state);
    state.setPortValue(Ttl744017.PORT_INDEX_CP1, Value.TRUE);
    gate.propagate(state);

    state.setPortValue(Ttl744017.PORT_INDEX_CP0, Value.TRUE);
    state.setPortValue(Ttl744017.PORT_INDEX_CP1, Value.FALSE);
    gate.propagate(state);
    assertCount(state, 1);
  }

  @Test
  void masterResetClearsAndConsumesAClockEdge() {
    final var gate = new Ttl744017();
    final var state = new TtlTestInstanceState(gate, false);
    reset(gate, state);
    pulseCp0(gate, state);
    pulseCp0(gate, state);
    assertCount(state, 2);

    state.setPortValue(Ttl744017.PORT_INDEX_MR, Value.TRUE);
    state.setPortValue(Ttl744017.PORT_INDEX_CP0, Value.TRUE);
    gate.propagate(state);
    assertCount(state, 0);

    state.setPortValue(Ttl744017.PORT_INDEX_MR, Value.FALSE);
    gate.propagate(state);
    assertCount(state, 0);

    state.setPortValue(Ttl744017.PORT_INDEX_CP0, Value.FALSE);
    gate.propagate(state);
    pulseCp0(gate, state);
    assertCount(state, 1);
  }

  @Test
  void unknownClockLevelsDoNotCount() {
    final var gate = new Ttl744017();
    final var state = new TtlTestInstanceState(gate, false);
    reset(gate, state);

    state.setPortValue(Ttl744017.PORT_INDEX_CP1, Value.UNKNOWN);
    gate.propagate(state);
    state.setPortValue(Ttl744017.PORT_INDEX_CP0, Value.TRUE);
    gate.propagate(state);
    assertCount(state, 0);

    state.setPortValue(Ttl744017.PORT_INDEX_CP0, Value.FALSE);
    state.setPortValue(Ttl744017.PORT_INDEX_CP1, Value.FALSE);
    gate.propagate(state);
    state.setPortValue(Ttl744017.PORT_INDEX_CP1, Value.TRUE);
    gate.propagate(state);
    state.setPortValue(Ttl744017.PORT_INDEX_CP0, Value.UNKNOWN);
    gate.propagate(state);
    state.setPortValue(Ttl744017.PORT_INDEX_CP1, Value.FALSE);
    gate.propagate(state);
    assertCount(state, 0);

    state.setPortValue(Ttl744017.PORT_INDEX_CP0, Value.FALSE);
    gate.propagate(state);
    pulseCp0(gate, state);
    assertCount(state, 1);
  }

  @Test
  void unknownResetDoesNotClearAndARisingEdgeStillCounts() {
    final var gate = new Ttl744017();
    final var state = new TtlTestInstanceState(gate, false);
    reset(gate, state);
    pulseCp0(gate, state);
    pulseCp0(gate, state);
    pulseCp0(gate, state);

    state.setPortValue(Ttl744017.PORT_INDEX_MR, Value.UNKNOWN);
    gate.propagate(state);
    assertCount(state, 3);

    pulseCp0(gate, state);
    assertCount(state, 4);
  }

  @Test
  void undefinedCountBecomesUnknownOnTheNextClock() {
    final var gate = new Ttl744017();
    final var state = new TtlTestInstanceState(gate, false);
    reset(gate, state);
    final var data = new TtlRegisterData(WIDTH);
    data.setValue(Value.createUnknown(WIDTH));
    state.setData(data);

    pulseCp0(gate, state);
    assertOutputs(state, Value.UNKNOWN);

    state.setPortValue(Ttl744017.PORT_INDEX_MR, Value.TRUE);
    gate.propagate(state);
    assertCount(state, 0);
  }

  @Test
  void errorInTheCountBecomesErrorOnTheNextClock() {
    final var gate = new Ttl744017();
    final var state = new TtlTestInstanceState(gate, false);
    reset(gate, state);
    final var data = new TtlRegisterData(WIDTH);
    final var bits = Value.createKnown(WIDTH, 0).getAll();
    bits[0] = Value.ERROR;
    data.setValue(Value.create(bits));
    state.setData(data);

    pulseCp0(gate, state);
    assertOutputs(state, Value.ERROR);

    state.setPortValue(Ttl744017.PORT_INDEX_MR, Value.TRUE);
    gate.propagate(state);
    assertCount(state, 0);
  }

  @Test
  void countOutsideTheDecadeStaysUnknown() {
    final var gate = new Ttl744017();
    final var state = new TtlTestInstanceState(gate, false);
    reset(gate, state);
    final var data = new TtlRegisterData(WIDTH);
    data.setValue(Value.createKnown(WIDTH, 10));
    state.setData(data);

    gate.propagate(state);
    assertOutputs(state, Value.UNKNOWN);

    pulseCp0(gate, state);
    assertOutputs(state, Value.UNKNOWN);

    state.setPortValue(Ttl744017.PORT_INDEX_MR, Value.TRUE);
    gate.propagate(state);
    assertCount(state, 0);
  }

  @Test
  void invalidExposedPowerInputsMakeOutputsUnknown() {
    final var gate = new Ttl744017();
    final var state = new TtlTestInstanceState(gate, true);
    state.setPortValue(GND_PORT, Value.FALSE);
    state.setPortValue(VCC_PORT, Value.TRUE);
    reset(gate, state);
    pulseCp0(gate, state);
    pulseCp0(gate, state);
    pulseCp0(gate, state);
    assertCount(state, 3);

    state.setPortValue(VCC_PORT, Value.FALSE);
    gate.propagate(state);
    assertOutputs(state, Value.UNKNOWN);

    state.setPortValue(VCC_PORT, Value.TRUE);
    gate.propagate(state);
    assertCount(state, 3);

    state.setPortValue(GND_PORT, Value.TRUE);
    gate.propagate(state);
    assertOutputs(state, Value.UNKNOWN);
  }

  private static void assertPort(Instance instance, int index, int x, int y, int type) {
    assertEquals(Location.create(x, y, false), instance.getPortLocation(index));
    assertEquals(type, instance.getPorts().get(index).getType());
  }

  private static void reset(Ttl744017 gate, TtlTestInstanceState state) {
    state.setPortValue(Ttl744017.PORT_INDEX_CP0, Value.FALSE);
    state.setPortValue(Ttl744017.PORT_INDEX_CP1, Value.FALSE);
    state.setPortValue(Ttl744017.PORT_INDEX_MR, Value.TRUE);
    gate.propagate(state);
    state.setPortValue(Ttl744017.PORT_INDEX_MR, Value.FALSE);
    gate.propagate(state);
  }

  private static void pulseCp0(Ttl744017 gate, TtlTestInstanceState state) {
    state.setPortValue(Ttl744017.PORT_INDEX_CP0, Value.TRUE);
    gate.propagate(state);
    state.setPortValue(Ttl744017.PORT_INDEX_CP0, Value.FALSE);
    gate.propagate(state);
  }

  private static void assertCount(TtlTestInstanceState state, int count) {
    for (var output = 0; output < DECODED_PORTS.length; output++) {
      final var expected = output == count ? Value.TRUE : Value.FALSE;
      assertEquals(expected, state.getPortValue(DECODED_PORTS[output]), "Q" + output);
    }
    assertEquals(
        count < 5 ? Value.TRUE : Value.FALSE,
        state.getPortValue(Ttl744017.PORT_INDEX_Q5_9),
        "Q5-9");
  }

  private static void assertOutputs(TtlTestInstanceState state, Value expected) {
    for (final var port : DECODED_PORTS) {
      assertEquals(expected, state.getPortValue(port));
    }
    assertEquals(expected, state.getPortValue(Ttl744017.PORT_INDEX_Q5_9));
  }
}
