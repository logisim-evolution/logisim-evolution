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
import com.cburch.logisim.prefs.AppPreferences;
import org.junit.jupiter.api.Test;

/** Functional tests for the 74HC259 8-bit addressable latch. */
class Ttl74259Test {
  private static final byte[] OUTPUTS = {
    Ttl74259.Q0,
    Ttl74259.Q1,
    Ttl74259.Q2,
    Ttl74259.Q3,
    Ttl74259.Q4,
    Ttl74259.Q5,
    Ttl74259.Q6,
    Ttl74259.Q7
  };
  private static final int GND_PORT = 14;
  private static final int VCC_PORT = 15;

  @Test
  void logicalPortsFollowTheDatasheetPinout() {
    final var gate = new Ttl74259();
    final var hiddenPower = createInstance(gate, false);

    assertEquals(14, hiddenPower.getPorts().size());
    assertPort(hiddenPower, Ttl74259.A0, 10, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74259.A1, 30, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74259.A2, 50, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74259.Q0, 70, 30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74259.Q1, 90, 30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74259.Q2, 110, 30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74259.Q3, 130, 30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74259.Q4, 150, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74259.Q5, 130, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74259.Q6, 110, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74259.Q7, 90, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74259.D, 70, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74259.LE, 50, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74259.MR, 30, -30, EndData.INPUT_ONLY);

    final var shownPower = createInstance(gate, true);
    assertEquals(16, shownPower.getPorts().size());
    assertEquals(Location.create(150, 30, false), shownPower.getPortLocation(GND_PORT));
    assertEquals(Location.create(10, -30, false), shownPower.getPortLocation(VCC_PORT));
    assertEquals(EndData.INPUT_ONLY, shownPower.getPorts().get(GND_PORT).getType());
    assertEquals(EndData.INPUT_ONLY, shownPower.getPorts().get(VCC_PORT).getType());
  }

  @Test
  void latchEnableIsNotAnEdgeClock() {
    assertFalse(new Ttl74259().checkForGatedClocks(null));
  }

  @Test
  void resetClearsEveryLatch() {
    final var gate = new Ttl74259();
    final var state = new TtlTestInstanceState(gate, false);
    latchBits(gate, state, 0xFF);

    drive(state, 7, Value.TRUE, Value.TRUE, Value.FALSE);
    gate.propagate(state);

    assertMask(state, 0x00);
  }

  @Test
  void demultiplexerFollowsDataAndClearsTheOthers() {
    final var gate = new Ttl74259();
    final var state = new TtlTestInstanceState(gate, false);
    latchBits(gate, state, 0xFF);

    for (var address = 0; address < OUTPUTS.length; address++) {
      drive(state, address, Value.TRUE, Value.FALSE, Value.FALSE);
      gate.propagate(state);
      assertMask(state, 1 << address);

      drive(state, address, Value.FALSE, Value.FALSE, Value.FALSE);
      gate.propagate(state);
      assertMask(state, 0x00);
    }
  }

  @Test
  void addressableLatchKeepsUnselectedBits() {
    final var gate = new Ttl74259();
    final var state = new TtlTestInstanceState(gate, false);
    reset(gate, state);

    drive(state, 0, Value.TRUE, Value.FALSE, Value.TRUE);
    gate.propagate(state);
    assertMask(state, 0x01);

    drive(state, 1, Value.TRUE, Value.FALSE, Value.TRUE);
    gate.propagate(state);
    assertMask(state, 0x03);

    drive(state, 1, Value.FALSE, Value.FALSE, Value.TRUE);
    gate.propagate(state);
    assertMask(state, 0x01);
  }

  @Test
  void addressableLatchAccumulatesAOneAtEveryAddress() {
    final var gate = new Ttl74259();
    final var state = new TtlTestInstanceState(gate, false);
    reset(gate, state);

    var mask = 0;
    for (var address = 0; address < OUTPUTS.length; address++) {
      drive(state, address, Value.TRUE, Value.FALSE, Value.TRUE);
      gate.propagate(state);
      mask |= 1 << address;
      assertMask(state, mask);
    }
    assertMask(state, 0xFF);

    drive(state, 3, Value.FALSE, Value.FALSE, Value.TRUE);
    gate.propagate(state);
    assertMask(state, 0xF7);
  }

  @Test
  void demultiplexerDropsThePreviousOutputWhenTheAddressChanges() {
    final var gate = new Ttl74259();
    final var state = new TtlTestInstanceState(gate, false);
    reset(gate, state);

    drive(state, 0, Value.TRUE, Value.FALSE, Value.FALSE);
    gate.propagate(state);
    assertMask(state, 0x01);

    drive(state, 1, Value.TRUE, Value.FALSE, Value.FALSE);
    gate.propagate(state);
    assertMask(state, 0x02);
  }

  @Test
  void memoryHoldsWhileDataAndAddressChange() {
    final var gate = new Ttl74259();
    final var state = new TtlTestInstanceState(gate, false);
    latchBits(gate, state, 0xA5);

    drive(state, 0, Value.FALSE, Value.TRUE, Value.TRUE);
    gate.propagate(state);
    drive(state, 3, Value.TRUE, Value.TRUE, Value.TRUE);
    gate.propagate(state);

    assertMask(state, 0xA5);

    drive(state, 3, Value.TRUE, Value.FALSE, Value.TRUE);
    gate.propagate(state);
    assertMask(state, 0xAD);
  }

  @Test
  void demultiplexerClearsBitsBeforeTheLatchHoldsThem() {
    final var gate = new Ttl74259();
    final var state = new TtlTestInstanceState(gate, false);
    latchBits(gate, state, 0xFF);

    drive(state, 3, Value.TRUE, Value.FALSE, Value.FALSE);
    gate.propagate(state);
    assertMask(state, 0x08);

    drive(state, 3, Value.FALSE, Value.TRUE, Value.TRUE);
    gate.propagate(state);
    drive(state, 0, Value.TRUE, Value.TRUE, Value.TRUE);
    gate.propagate(state);
    assertMask(state, 0x08);

    latchBits(gate, state, 0xFF);
    drive(state, 1, Value.TRUE, Value.FALSE, Value.FALSE);
    gate.propagate(state);
    drive(state, 1, Value.FALSE, Value.FALSE, Value.TRUE);
    gate.propagate(state);
    assertMask(state, 0x00);
  }

  @Test
  void unknownControlsDoNotChangeStoredBits() {
    final var gate = new Ttl74259();
    final var state = new TtlTestInstanceState(gate, false);
    latchBits(gate, state, 0xA5);

    drive(state, 1, Value.FALSE, Value.UNKNOWN, Value.TRUE);
    gate.propagate(state);
    set(state, Ttl74259.MR, Value.ERROR);
    gate.propagate(state);

    assertMask(state, 0xA5);
  }

  @Test
  void unknownDataIsStoredInTheAddressedLatch() {
    final var gate = new Ttl74259();
    final var state = new TtlTestInstanceState(gate, false);
    reset(gate, state);

    drive(state, 3, Value.UNKNOWN, Value.FALSE, Value.TRUE);
    gate.propagate(state);
    assertOutput(state, 3, Value.UNKNOWN);
    assertIdleExcept(state, 3);

    drive(state, 3, Value.ERROR, Value.FALSE, Value.TRUE);
    gate.propagate(state);
    drive(state, 3, Value.FALSE, Value.TRUE, Value.TRUE);
    gate.propagate(state);

    assertOutput(state, 3, Value.ERROR);
    assertIdleExcept(state, 3);
  }

  @Test
  void partialAddressCombinesSelectedAndUnselectedResults() {
    final var gate = new Ttl74259();
    final var state = new TtlTestInstanceState(gate, false);
    final var low = Value.FALSE;
    final var error = Value.ERROR;
    reset(gate, state);

    drive(state, 0, Value.TRUE, low, Value.TRUE);
    set(state, Ttl74259.A0, Value.UNKNOWN);
    gate.propagate(state);
    assertOutputs(state, error, error, low, low, low, low, low, low);

    reset(gate, state);
    drive(state, 0, Value.TRUE, low, Value.TRUE);
    gate.propagate(state);
    drive(state, 0, low, low, Value.TRUE);
    set(state, Ttl74259.A0, Value.UNKNOWN);
    gate.propagate(state);
    assertOutputs(state, error, low, low, low, low, low, low, low);

    reset(gate, state);
    drive(state, 0, low, low, low);
    set(state, Ttl74259.A0, Value.UNKNOWN);
    gate.propagate(state);
    assertMask(state, 0x00);

    drive(state, 0, Value.TRUE, low, low);
    set(state, Ttl74259.A0, Value.UNKNOWN);
    gate.propagate(state);
    assertOutputs(state, error, error, low, low, low, low, low, low);
  }

  @Test
  void startupFollowsTheMemoryPreference() throws InterruptedException {
    final var previous = AppPreferences.Memory_Startup_Unknown.get();
    try {
      setStartupUnknown(false);
      final var cleared = new Ttl74259();
      final var clearedState = new TtlTestInstanceState(cleared, false);
      drive(clearedState, 0, Value.TRUE, Value.TRUE, Value.TRUE);
      cleared.propagate(clearedState);
      assertMask(clearedState, 0x00);

      setStartupUnknown(true);
      final var unknown = new Ttl74259();
      final var unknownState = new TtlTestInstanceState(unknown, false);
      drive(unknownState, 0, Value.FALSE, Value.TRUE, Value.TRUE);
      unknown.propagate(unknownState);
      for (var index = 0; index < OUTPUTS.length; index++) {
        assertOutput(unknownState, index, Value.UNKNOWN);
      }
    } finally {
      AppPreferences.Memory_Startup_Unknown.set(previous);
    }
  }

  @Test
  void invalidExposedPowerInputsMakeOutputsUnknown() {
    final var gate = new Ttl74259();
    final var state = new TtlTestInstanceState(gate, true);
    power(state, Value.FALSE, Value.TRUE);
    latchBits(gate, state, 0xA5);
    drive(state, 0, Value.FALSE, Value.TRUE, Value.TRUE);
    gate.propagate(state);
    assertMask(state, 0xA5);

    power(state, Value.FALSE, Value.FALSE);
    gate.propagate(state);
    assertAll(state, Value.UNKNOWN);

    power(state, Value.FALSE, Value.TRUE);
    gate.propagate(state);
    assertMask(state, 0xA5);

    power(state, Value.TRUE, Value.TRUE);
    gate.propagate(state);
    assertAll(state, Value.UNKNOWN);
  }

  /**
   * Preference updates reach the cached value on the preferences listener thread, so a write is not
   * visible to the next component until that cache matches.
   */
  private static void setStartupUnknown(boolean unknown) throws InterruptedException {
    AppPreferences.Memory_Startup_Unknown.set(unknown);
    final var deadline = System.nanoTime() + 1_000_000_000L;
    while (AppPreferences.Memory_Startup_Unknown.get() != unknown) {
      if (System.nanoTime() > deadline) {
        assertEquals(unknown, AppPreferences.Memory_Startup_Unknown.get());
      }
      Thread.sleep(10);
    }
  }

  private static void reset(Ttl74259 gate, TtlTestInstanceState state) {
    drive(state, 0, Value.FALSE, Value.TRUE, Value.FALSE);
    gate.propagate(state);
  }

  private static void latchBits(Ttl74259 gate, TtlTestInstanceState state, int mask) {
    for (var index = 0; index < OUTPUTS.length; index++) {
      final var data = ((mask >> index) & 1) == 1 ? Value.TRUE : Value.FALSE;
      drive(state, index, data, Value.FALSE, Value.TRUE);
      gate.propagate(state);
    }
  }

  private static void drive(
      TtlTestInstanceState state, int address, Value data, Value latchEnable, Value masterReset) {
    set(state, Ttl74259.A0, bit(address, 0));
    set(state, Ttl74259.A1, bit(address, 1));
    set(state, Ttl74259.A2, bit(address, 2));
    set(state, Ttl74259.D, data);
    set(state, Ttl74259.LE, latchEnable);
    set(state, Ttl74259.MR, masterReset);
  }

  private static Value bit(int address, int place) {
    return ((address >> place) & 1) == 1 ? Value.TRUE : Value.FALSE;
  }

  private static void power(TtlTestInstanceState state, Value ground, Value supply) {
    state.setPortValue(GND_PORT, ground);
    state.setPortValue(VCC_PORT, supply);
  }

  private static void assertMask(TtlTestInstanceState state, int mask) {
    for (var index = 0; index < OUTPUTS.length; index++) {
      assertOutput(state, index, bit(mask, index));
    }
  }

  private static void assertIdleExcept(TtlTestInstanceState state, int skipped) {
    for (var index = 0; index < OUTPUTS.length; index++) {
      if (index == skipped) continue;
      assertOutput(state, index, Value.FALSE);
    }
  }

  private static void assertOutputs(TtlTestInstanceState state, Value... expected) {
    assertEquals(OUTPUTS.length, expected.length);
    for (var index = 0; index < OUTPUTS.length; index++) {
      assertOutput(state, index, expected[index]);
    }
  }

  private static void assertAll(TtlTestInstanceState state, Value expected) {
    for (var index = 0; index < OUTPUTS.length; index++) {
      assertOutput(state, index, expected);
    }
  }

  private static void assertOutput(TtlTestInstanceState state, int index, Value expected) {
    assertEquals(expected, state.getPortValue(Ttl74259.pinNrToPortNr(OUTPUTS[index])), "Q" + index);
  }

  private static void assertPort(Instance instance, byte dsPinNr, int x, int y, int type) {
    final var port = Ttl74259.pinNrToPortNr(dsPinNr);
    assertEquals(Location.create(x, y, false), instance.getPortLocation(port));
    assertEquals(type, instance.getPorts().get(port).getType());
  }

  private static void set(TtlTestInstanceState state, byte dsPinNr, Value value) {
    state.setPortValue(Ttl74259.pinNrToPortNr(dsPinNr), value);
  }
}
